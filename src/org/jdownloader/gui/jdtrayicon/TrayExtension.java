//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.gui.jdtrayicon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.MainFrameClosingHandler;
import jd.gui.swing.jdgui.views.settings.sidebar.CheckBoxedEntry;
import jd.plugins.AddonPanel;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.DesktopSupportLinux;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.appwork.utils.swing.windowmanager.WindowManager.WindowExtendedState;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.translate.TrayiconTranslation;
import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class TrayExtension extends AbstractExtension<TrayConfig, TrayiconTranslation> implements MouseListener, MouseMotionListener, WindowStateListener, ActionListener, MainFrameClosingHandler, CheckBoxedEntry {
    @Override
    public boolean isHeadlessRunnable() {
        return false;
    }

    @Override
    protected void stop() throws StopException {
        removeTrayIcon();
        if (guiFrame != null) {
            JDGui.getInstance().setClosingHandler(null);
            guiFrame.removeWindowStateListener(this);
            JDGui.getInstance().setWindowToTray(false);
            guiFrame.setAlwaysOnTop(false);
            guiFrame = null;
        }
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_MINIMIZE;
    }

    public String getName() {
        return T.getName();
    }

    @Override
    protected void start() throws StartException {
        if (Application.getJavaVersion() < Application.JAVA16) {
            LogController.CL(TrayExtension.class).severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + Application.getJavaVersion());
            throw new StartException("Tray is supported since Java 1.6. your Version: " + Application.getJavaVersion());
        }
        if (!SystemTray.isSupported()) {
            LogController.CL(TrayExtension.class).severe("Error initializing SystemTray: Tray isn't supported jet");
            if (CrossSystem.isUnix()) {
                LogController.CL().severe("Make sure your Notification Area is enabled!");
            }
            throw new StartException("Tray isn't supported!");
        }

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        try {
                            if (JDGui.getInstance() != null) {
                                initGUI(true);
                                LogController.CL(TrayExtension.class).info("Systemtray OK");
                            }
                        } catch (Exception e) {
                            LogController.CL(TrayExtension.class).log(e);
                        }
                    }
                };
            }
        });
    }

    @Override
    public String getDescription() {
        return _TRAY.T.jd_plugins_optional_jdtrayicon_jdlighttray_description();
    }

    @Override
    public AddonPanel<TrayExtension> getGUI() {
        return null;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    private TrayIconPopup                       trayIconPopup;

    private TrayIcon                            trayIcon;

    private JFrame                              guiFrame;

    private TrayIconTooltip                     trayIconTooltip;

    private TrayMouseAdapter                    ma;

    private ExtensionConfigPanel<TrayExtension> configPanel;

    private long                                lastCloseRequest;

    private boolean                             asking;

    public ExtensionConfigPanel<TrayExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public TrayExtension() {
        setTitle(_TRAY.T.jd_plugins_optional_jdtrayicon_jdlighttray());
    }

    public static int readEnableBalloonTips() throws UnsupportedEncodingException, IOException {
        final String iconResult = IO.readInputStreamToString(Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced\" /v \"EnableBalloonTips\"").getInputStream());
        final Matcher matcher = Pattern.compile("EnableBalloonTips\\s+REG_DWORD\\s+0x(.*)").matcher(iconResult);
        matcher.find();
        final String value = matcher.group(1);
        return Integer.parseInt(value, 16);
    }

    public static void writeEnableBalloonTips(final int foregroundLockTimeout) {
        try {
            final Process p = Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced\" /v \"EnableBalloonTips\" /t REG_DWORD /d 0x" + Integer.toHexString(foregroundLockTimeout) + " /f");
            IO.readInputStreamToString(p.getInputStream());
            final int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new IOException("Reg add execution failed");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void initGUI(final boolean startup) {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        try {
                            removeTrayIcon();
                            final SystemTray systemTray = SystemTray.getSystemTray();
                            final int trayIconWidth = (int) systemTray.getTrayIconSize().getWidth();
                            final int trayIconHeight = (int) systemTray.getTrayIconSize().getHeight();
                            final BufferedImage img;
                            if (getSettings().isGreyIconEnabled()) {
                                img = ImageProvider.convertToGrayScale(IconIO.getScaledInstance(NewTheme.I().getImage("logo/jd_logo_128_128", -1), trayIconWidth, trayIconHeight));
                            } else {
                                img = IconIO.getScaledInstance(NewTheme.I().getImage("logo/jd_logo_128_128", -1), trayIconWidth, trayIconHeight);
                            }
                            LogController.CL(TrayExtension.class).info("TrayIconSize:" + trayIconWidth + "x" + trayIconHeight + "->IconSize:" + img.getWidth() + "x" + img.getHeight());
                            // workaround for gnome 3 transparency bug
                            if (getSettings().isGnomeTrayIconTransparentEnabled() && (CrossSystem.isUnix())) {
                                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6453521
                                final DesktopSupportLinux desktop = new DesktopSupportLinux();
                                try {
                                    if ((desktop.isGnomeDesktop() || desktop.isXFCEDesktop()) && !desktop.isWayland()) {
                                        getSettings().setGnomeTrayIconTransparentEnabled(false);
                                        getSettings()._getStorageHandler().write();// disable+write to avoid crash on next try
                                        // Wayland/java crashes onjava.awt.Robot.createScreenCapture
                                        LogController.CL(TrayExtension.class).info("Apply LinuxTrayIcon workaround");
                                        final Robot robo = new java.awt.Robot();
                                        final BufferedImage screenCapture = robo.createScreenCapture(new Rectangle(2, img.getHeight()));
                                        for (int y = 0; y < img.getHeight(); y++) {
                                            final Color pixel = new Color(screenCapture.getRGB(1, y));
                                            for (int x = 0; x < img.getWidth(); x++) {
                                                final Color tmp = new Color(img.getRGB(x, y));
                                                final float alpha = ((img.getRGB(x, y) >> 24) & 0xFF) / 255F;
                                                final int cr = (int) (alpha * tmp.getRed() + (1 - alpha) * pixel.getRed());
                                                final int cg = (int) (alpha * tmp.getGreen() + (1 - alpha) * pixel.getGreen());
                                                final int cb = (int) (alpha * tmp.getBlue() + (1 - alpha) * pixel.getBlue());
                                                img.setRGB(x, y, new Color(cr, cg, cb).getRGB());
                                            }
                                        }
                                    }
                                } catch (final Throwable e) {
                                    LogController.CL().log(e);
                                } finally {
                                    getSettings().setGnomeTrayIconTransparentEnabled(true);
                                }
                            }
                            /*
                             * trayicon message must be set, else windows cannot handle icon right (eg autohide feature)
                             */
                            trayIcon = new TrayIcon(img, "JDownloader");
                            trayIcon.setImageAutoSize(true);
                            trayIcon.addActionListener(TrayExtension.this);
                            ma = new TrayMouseAdapter(TrayExtension.this, trayIcon);
                            trayIconTooltip = new TrayIconTooltip();
                            LogController.CL(TrayExtension.class).info("JDLightTrayIcon Init complete");
                            if (guiFrame == null) {
                                guiFrame = JDGui.getInstance().getMainFrame();
                                if (guiFrame != null) {
                                    JDGui.getInstance().setClosingHandler(TrayExtension.this);
                                    guiFrame.addComponentListener(new ComponentListener() {
                                        @Override
                                        public void componentShown(ComponentEvent e) {
                                            if (getSettings().isTrayOnlyVisibleIfWindowIsHiddenEnabled()) {
                                                new EDTRunner() {

                                                    @Override
                                                    protected void runInEDT() {
                                                        removeTrayIcon();
                                                    }
                                                };
                                            }
                                        }

                                        @Override
                                        public void componentResized(ComponentEvent e) {
                                        }

                                        @Override
                                        public void componentMoved(ComponentEvent e) {
                                        }

                                        @Override
                                        public void componentHidden(ComponentEvent e) {
                                            if (getSettings().isTrayOnlyVisibleIfWindowIsHiddenEnabled()) {
                                                new EDTRunner() {
                                                    @Override
                                                    protected void runInEDT() {
                                                        removeTrayIcon();
                                                        initGUI(false);
                                                    }
                                                };
                                            }
                                        }
                                    });
                                    guiFrame.removeWindowStateListener(TrayExtension.this);
                                    guiFrame.addWindowStateListener(TrayExtension.this);
                                    if (startup && getSettings().isStartMinimizedEnabled()) {
                                        JDGui.getInstance().setWindowToTray(true);
                                    }
                                }
                            }
                            if (!getSettings().isTrayOnlyVisibleIfWindowIsHiddenEnabled() || !guiFrame.isVisible()) {
                                systemTray.add(trayIcon);
                                trayIcon.addMouseListener(ma);
                                trayIcon.addMouseMotionListener(ma);
                            }
                        } catch (Throwable e) {
                            /*
                             * on Gnome3, Unity, this can happen because icon might be blacklisted, see here
                             * http://www.webupd8.org/2011/04/how-to-re-enable -notification-area.html
                             * 
                             * dconf-editor", then navigate to desktop > unity > panel and whitelist JDownloader
                             * 
                             * also see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7103610
                             * 
                             * TODO: maybe add dialog to inform user
                             */
                            LogController.CL().log(e);
                            try {
                                setEnabled(false);
                            } catch (final Throwable e1) {
                            }
                            return;
                        }
                    }
                };
            }
        });
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        trayIconTooltip.hideTooltip();
    }

    public void mousePressed(MouseEvent e) {
        trayIconTooltip.hideTooltip();
        if (e.getSource() instanceof TrayIcon) {
            if (!CrossSystem.isMac()) {
                if (e.getClickCount() >= (getSettings().isToogleWindowStatusWithSingleClickEnabled() ? 1 : 2) && !SwingUtilities.isRightMouseButton(e)) {
                    JDGui.getInstance().setWindowToTray(guiFrame.isVisible());
                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        if (!checkPassword()) {
                            return;
                        }
                        trayIconPopup = new TrayIconPopup(this);
                        calcLocation(trayIconPopup, e.getPoint());
                        WindowManager.getInstance().setVisible(trayIconPopup, true, FrameState.OS_DEFAULT);
                        trayIconPopup.startAutoHide();
                    }
                }
            } else {
                if (e.getClickCount() >= (getSettings().isToogleWindowStatusWithSingleClickEnabled() ? 1 : 2) && !SwingUtilities.isLeftMouseButton(e)) {
                    JDGui.getInstance().setWindowToTray(guiFrame.isVisible() & guiFrame.getState() != Frame.ICONIFIED);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        if (!checkPassword()) {
                            return;
                        }
                        trayIconPopup = new TrayIconPopup(this);
                        Point pointOnScreen = e.getLocationOnScreen();
                        // if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                        calcLocation(trayIconPopup, pointOnScreen);
                        WindowManager.getInstance().setVisible(trayIconPopup, true, FrameState.OS_DEFAULT);
                        trayIconPopup.startAutoHide();
                    }
                }
            }
        }
    }

    private boolean checkPassword() {
        final boolean visible = JDGui.getInstance().getMainFrame().isVisible();
        if (visible) {
            return true;
        }
        final boolean pwEnabled = CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled();
        if (!pwEnabled) {
            return true;
        }
        final boolean pwEmpty = StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue());
        if (pwEmpty) {
            return true;
        }
        try {
            final String password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI.T.JDGui_setVisible_password_(), _GUI.T.JDGui_setVisible_password_msg(), null, new AbstractIcon(IconKey.ICON_LOCK, 32), null, null);
            if (!CFG_GUI.PASSWORD.getValue().equals(password)) {
                Dialog.getInstance().showMessageDialog(_GUI.T.JDGui_setVisible_password_wrong());
                return false;
            }
        } catch (DialogNoAnswerException e) {
            return false;
        }
        return true;
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    private static void calcLocation(final TrayIconPopup window, final Point p) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                if (!CrossSystem.isMac()) {
                    if (p.x <= limitX) {
                        if (p.y <= limitY) {
                            // top left
                            window.setLocation(p.x, p.y);
                        } else {
                            // bottom left
                            window.setLocation(p.x, p.y - window.getHeight());
                        }
                    } else {
                        if (p.y <= limitY) {
                            // top right
                            window.setLocation(p.x - window.getWidth(), p.y);
                        } else {
                            // bottom right
                            window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                        }
                    }
                } else {
                    if (p.getX() <= (screenSize.getWidth() - window.getWidth())) {
                        window.setLocation((int) p.getX(), 22);
                    } else {
                        window.setLocation(p.x - window.getWidth(), 22);
                    }
                }
                return null;
            }
        }.waitForEDT();
    }

    /**
     * gets called if mouse stays over the tray. Edit delay in {@link TrayMouseAdapter}
     */
    public void mouseStay(MouseEvent e) {
        if (!getSettings().isToolTipEnabled()) {
            return;
        }
        if (trayIconPopup != null && trayIconPopup.isVisible()) {
            return;
        }
        trayIconTooltip.showTooltip(((TrayMouseAdapter) e.getSource()).getEstimatedTopLeft());
    }

    private void removeTrayIcon() {
        try {
            if (trayIcon != null) {
                trayIcon.removeActionListener(this);
                if (ma != null) {
                    trayIcon.removeMouseListener(ma);
                    trayIcon.removeMouseMotionListener(ma);
                }
                SystemTray.getSystemTray().remove(trayIcon);
            }
        } catch (Throwable e) {
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
        /* workaround for : toFront() */
        if (guiFrame != null) {
            guiFrame.setAlwaysOnTop(false);
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowStateChanged(WindowEvent evt) {
        int oldState = evt.getOldState();
        int newState = evt.getNewState();
        if (System.currentTimeMillis() - lastCloseRequest < 1000 || asking) {
            return;
        }
        if ((oldState & JFrame.ICONIFIED) == 0 && (newState & JFrame.ICONIFIED) != 0) {
            switch (getSettings().getOnMinimizeAction()) {
            case TO_TASKBAR:
                return;
            case TO_TRAY:
                // let's hope that this does not flicker. works fine for win7
                JDGui.getInstance().setWindowToTray(true);
                // JDGui.getInstance().getMainFrame().setExtendedState(JFrame.NORMAL);
            }
            // Frame was not iconified
        } else if ((oldState & JFrame.ICONIFIED) != 0 && (newState & JFrame.ICONIFIED) == 0) {
            // Frame was iconified
        }
    }

    @Override
    protected void initExtension() throws StartException {
        configPanel = new TrayConfigPanel(this);
    }

    @Override
    public void handleCommand(String command, String... parameters) {
    }

    private OnCloseAction windowClosedTray(final WindowEvent e) {
        if (CrossSystem.isMac()) {
            return windowClosedTrayForMac(e);
        }
        final OnCloseAction[] ret = new OnCloseAction[1];
        ret[0] = null;
        final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK, T.JDGui_windowClosing_try_title(), T.JDGui_windowClosing_try_msg_2(), new AbstractIcon(IconKey.ICON_EXIT, 32), T.JDGui_windowClosing_try_asnwer_close(), null);
        try {
            d.setLeftActions(new AppAction() {
                {
                    setName(T.JDGui_windowClosing_try_answer_totaskbar());
                }

                @Override
                public void actionPerformed(ActionEvent e1) {
                    ret[0] = OnCloseAction.TO_TASKBAR;
                    d.dispose();
                }
            }, new AppAction() {
                {
                    setName(T.JDGui_windowClosing_try_answer_tray());
                    setEnabled(SystemTray.isSupported());
                }

                @Override
                public void actionPerformed(ActionEvent e1) {
                    ret[0] = OnCloseAction.TO_TRAY;
                    d.dispose();
                }
            });
            Dialog.I().showDialog(d);
            // to tray
            if (ret[0] == null) {
                ret[0] = OnCloseAction.EXIT;
            }
        } catch (DialogNoAnswerException e1) {
            // set source to null in order to avoid further actions in - for example the Tray extension listsners
            e.setSource(null);
            e1.printStackTrace();
            ret[0] = OnCloseAction.ASK;
        }
        if (d.isDontShowAgainSelected()) {
            getSettings().setOnCloseAction(ret[0]);
        }
        return ret[0];
    }

    private OnCloseAction windowClosedTrayForMac(WindowEvent e) {
        final OnCloseAction[] ret = new OnCloseAction[1];
        ret[0] = null;
        final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK, T.JDGui_windowClosing_try_title(), T.JDGui_windowClosing_try_msg_2(), new AbstractIcon(IconKey.ICON_EXIT, 32), T.JDGui_windowClosing_try_asnwer_close(), null);
        try {
            d.setLeftActions(new AppAction() {
                {
                    setName(T.JDGui_windowClosing_try_answer_totaskbar());
                }

                @Override
                public void actionPerformed(ActionEvent e1) {
                    ret[0] = OnCloseAction.TO_TASKBAR;
                    d.dispose();
                }
            }, new AppAction() {
                {
                    setName(T.JDGui_windowClosing_try_answer_tray());
                    setEnabled(SystemTray.isSupported());
                }

                @Override
                public void actionPerformed(ActionEvent e1) {
                    ret[0] = OnCloseAction.TO_TRAY;
                    d.dispose();
                }
            });
            Dialog.I().showDialog(d);
            // to tray
            if (ret[0] == null) {
                ret[0] = OnCloseAction.EXIT;
            }
        } catch (DialogNoAnswerException e1) {
            // set source to null in order to avoid further actions in - for example the Tray extension listsners
            e.setSource(null);
            e1.printStackTrace();
            ret[0] = OnCloseAction.ASK;
        }
        if (d.isDontShowAgainSelected()) {
            getSettings().setOnCloseAction(ret[0]);
        }
        return ret[0];
    }

    @Override
    public void windowClosing(WindowEvent e) {
        final AtomicBoolean asked = new AtomicBoolean(false);
        try {
            lastCloseRequest = System.currentTimeMillis();
            main: if (isEnabled()) {
                switch (getSettings().getOnCloseAction()) {
                case ASK:
                    asked.set(true);
                    switch (windowClosedTray(e)) {
                    case ASK:
                        // cancel clicked
                        return;
                    case EXIT:
                        // exit clicked
                        break main;
                    case TO_TASKBAR:
                        WindowManager.getInstance().setExtendedState(JDGui.getInstance().getMainFrame(), WindowExtendedState.ICONIFIED);
                        return;
                    case TO_TRAY:
                        if (SystemTray.isSupported()) {
                            JDGui.getInstance().setWindowToTray(true);
                            return;
                        }
                    }
                case EXIT:
                    break main;
                case TO_TASKBAR:
                    WindowManager.getInstance().setExtendedState(JDGui.getInstance().getMainFrame(), WindowExtendedState.ICONIFIED);
                    return;
                case TO_TRAY:
                    if (SystemTray.isSupported()) {
                        JDGui.getInstance().setWindowToTray(true);
                        return;
                    }
                }
            }
        } catch (final Throwable e1) {
            /* plugin not loaded yet */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e1);
        }
        // no special mac handling. the typical mac handling would be close to tray
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(asked.get()));
    }

    @Override
    public Icon _getIcon(int size) {
        return NewTheme.I().getIcon(getIconKey(), size);
    }

    @Override
    public boolean _isEnabled() {
        return isEnabled();
    }

    @Override
    public void _setEnabled(boolean b) throws StartException, StopException {
        setEnabled(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    public boolean isActive() {
        return trayIconPopup != null && trayIconPopup.hasBeenRecentlyActive();
    }

}