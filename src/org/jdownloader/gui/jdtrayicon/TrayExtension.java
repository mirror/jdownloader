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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jd.SecondLevelLaunch;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.MainFrameClosingHandler;
import jd.gui.swing.jdgui.views.settings.sidebar.CheckBoxedEntry;
import jd.plugins.AddonPanel;

import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.DesktopSupportLinux;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.jdtrayicon.translate.TrayiconTranslation;
import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class TrayExtension extends AbstractExtension<TrayConfig, TrayiconTranslation> implements MouseListener, MouseMotionListener, WindowStateListener, ActionListener, MainFrameClosingHandler, CheckBoxedEntry {

    // private LinkCollectorHighlightListener highListener = new LinkCollectorHighlightListener() {
    //
    // @Override
    // public void onHighLight(CrawledLink parameter) {
    // if (guiFrame != null && !guiFrame.isVisible()) {
    // /*
    // * dont try to restore jd if password required
    // */
    // if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
    // /**
    // * do nothing , because a password is set
    // */
    // return;
    // }
    // }
    // miniIt(false);
    // if (iconified) {
    // /*
    // * restore normale state,if windows was iconified
    // */
    // new EDTHelper<Object>() {
    // @Override
    // public Object edtRun() {
    // /*
    // * after this normal , its back to iconified
    // */
    // guiFrame.setState(JFrame.NORMAL);
    // return null;
    // }
    // }.start();
    // }
    // }
    //
    // @Override
    // public boolean isThisListenerEnabled() {
    // return true;
    // // LinkgrabberResultsOption option =
    // // getSettings().getShowLinkgrabbingResultsOption();
    // // if ((guiFrame != null && !guiFrame.isVisible() && option ==
    // // LinkgrabberResultsOption.ONLY_IF_MINIMIZED) || option ==
    // // LinkgrabberResultsOption.ALWAYS) { return true; }
    // // return false;
    // }
    // };

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
        return "minimize";
    }

    public String getName() {
        return _.getName();
    }

    @Override
    protected void start() throws StartException {
        if (Application.getJavaVersion() < Application.JAVA16) {
            LogController.CL(TrayExtension.class).severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + Application.getJavaVersion());
            throw new StartException("Tray is supported since Java 1.6. your Version: " + Application.getJavaVersion());
        }
        if (!SystemTray.isSupported()) {
            LogController.CL(TrayExtension.class).severe("Error initializing SystemTray: Tray isn't supported jet");
            if (CrossSystem.isLinux()) LogController.CL().severe("Make sure your Notification Area is enabled!");
            throw new StartException("Tray isn't supported!");
        }
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                // LinkCollector.getInstance().getEventsender().addListener(highListener);
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            if (SwingGui.getInstance() != null) {
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
        return _TRAY._.jd_plugins_optional_jdtrayicon_jdlighttray_description();
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

    private boolean                             iconified       = false;

    private Thread                              trayIconChecker = null;

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
        setTitle(_TRAY._.jd_plugins_optional_jdtrayicon_jdlighttray());

    }

    public void initGUI(final boolean startup) {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            removeTrayIcon();
                            SystemTray systemTray = SystemTray.getSystemTray();
                            BufferedImage img = IconIO.getScaledInstance(NewTheme.I().getImage("logo/jd_logo_64_64", -1), (int) systemTray.getTrayIconSize().getWidth(), (int) systemTray.getTrayIconSize().getHeight());

                            if (getSettings().isGreyIconEnabled()) {
                                img = ImageProvider.convertToGrayScale(img);
                            }

                            // workaround for gnome 3 transparency bug
                            if (getSettings().isGnomeTrayIconTransparentEnabled() && CrossSystem.isLinux() && new DesktopSupportLinux().isGnomeDesktop()) {
                                java.awt.Robot robo = new java.awt.Robot();
                                Color tmp, newColor;
                                int cr, cb, cg;
                                float alpha;
                                for (int y = 0; y < img.getHeight(); y++) {
                                    newColor = robo.getPixelColor(1, y);
                                    for (int x = 0; x < img.getWidth(); x++) {
                                        tmp = new Color(img.getRGB(x, y));
                                        alpha = ((img.getRGB(x, y) >> 24) & 0xFF) / 255F;
                                        cr = (int) (alpha * tmp.getRed() + (1 - alpha) * newColor.getRed());
                                        cg = (int) (alpha * tmp.getGreen() + (1 - alpha) * newColor.getGreen());
                                        cb = (int) (alpha * tmp.getBlue() + (1 - alpha) * newColor.getBlue());
                                        tmp = new Color(cr, cg, cb);
                                        img.setRGB(x, y, tmp.getRGB());
                                    }
                                }
                            }

                            /*
                             * trayicon message must be set, else windows cannot handle icon right (eg autohide feature)
                             */
                            trayIcon = new TrayIcon(img, "JDownloader");
                            trayIcon.setImageAutoSize(true);
                            trayIcon.addActionListener(TrayExtension.this);
                            ma = new TrayMouseAdapter(TrayExtension.this, trayIcon);
                            trayIcon.addMouseListener(ma);
                            trayIcon.addMouseMotionListener(ma);
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
                                                        // initGUI(false);
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
                        if (!checkPassword()) return;
                        trayIconPopup = new TrayIconPopup(this);
                        calcLocation(trayIconPopup, e.getPoint());
                        WindowManager.getInstance().setVisible(trayIconPopup, true,FrameState.OS_DEFAULT);
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
                        if (!checkPassword()) return;
                        trayIconPopup = new TrayIconPopup(this);
                        Point pointOnScreen = e.getLocationOnScreen();
                        if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                        calcLocation(trayIconPopup, pointOnScreen);
                        WindowManager.getInstance().setVisible(trayIconPopup, true,FrameState.OS_DEFAULT);
                        trayIconPopup.startAutoHide();
                    }
                }
            }
        }
    }

    private boolean checkPassword() {
        boolean visible = JDGui.getInstance().getMainFrame().isVisible();
        if (visible) return true;
        boolean pwEnabled = CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled();
        if (!pwEnabled) return true;
        boolean pwEmpty = StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue());
        if (pwEmpty) return true;

        String password;
        try {
            password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI._.SwingGui_setVisible_password_(), _GUI._.SwingGui_setVisible_password_msg(), null, NewTheme.I().getIcon("lock", 32), null, null);
            if (!CFG_GUI.PASSWORD.getValue().equals(password)) {
                Dialog.getInstance().showMessageDialog(_GUI._.SwingGui_setVisible_password_wrong());
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

        if (!getSettings().isToolTipEnabled()) return;
        if (trayIconPopup != null && trayIconPopup.isVisible()) return;
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
        if (guiFrame != null) guiFrame.setAlwaysOnTop(false);
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowStateChanged(WindowEvent evt) {
        int oldState = evt.getOldState();
        int newState = evt.getNewState();
        if (System.currentTimeMillis() - lastCloseRequest < 1000 || asking) return;
        if ((oldState & JFrame.ICONIFIED) == 0 && (newState & JFrame.ICONIFIED) != 0) {
            iconified = true;

            switch (getSettings().getOnMinimizeAction()) {

            case TO_TASKBAR:
                return;
            case TO_TRAY:
                // let's hope that this does not flicker. works fine for win7

                JDGui.getInstance().setWindowToTray(true);
                JDGui.getInstance().getMainFrame().setExtendedState(JFrame.NORMAL);

            }
            // Frame was not iconified
        } else if ((oldState & JFrame.ICONIFIED) != 0 && (newState & JFrame.ICONIFIED) == 0) {
            iconified = false;
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
        final OnCloseAction[] ret = new OnCloseAction[1];
        ret[0] = null;
        final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK, _.JDGui_windowClosing_try_title_(), _.JDGui_windowClosing_try_msg_2(), NewTheme.I().getIcon("exit", 32), _.JDGui_windowClosing_try_asnwer_close(), null);

        try {

            d.setLeftActions(new AppAction() {
                {
                    setName(_.JDGui_windowClosing_try_answer_totaskbar());
                }

                @Override
                public void actionPerformed(ActionEvent e1) {
                    ret[0] = OnCloseAction.TO_TASKBAR;
                    d.dispose();

                }
            }, new AppAction() {
                {
                    setName(_.JDGui_windowClosing_try_answer_tray());
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
            if (ret[0] == null) ret[0] = OnCloseAction.EXIT;

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
                        JDGui.getInstance().getMainFrame().setExtendedState(JFrame.ICONIFIED);
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
                    JDGui.getInstance().getMainFrame().setExtendedState(JFrame.ICONIFIED);
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
            Log.exception(e1);
        }
        /*
         * without trayicon also dont close/exit for macos
         */
        if (CrossSystem.isMac()) {
            new EDTHelper<Object>() {
                @Override
                public Object edtRun() {
                    /* set visible state */
                    WindowManager.getInstance().setVisible(JDGui.getInstance().getMainFrame(), false,FrameState.OS_DEFAULT);
                    return null;
                }
            }.start();
            return;
        }
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(asked.get()));

    }

    @Override
    public ImageIcon _getIcon(int size) {

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