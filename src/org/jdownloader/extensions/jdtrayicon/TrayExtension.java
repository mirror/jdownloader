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

package org.jdownloader.extensions.jdtrayicon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.Launcher;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorHighlightListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.MainFrameClosingHandler;
import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.update.inapp.RlyExitListener;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.jdtrayicon.translate.T;
import org.jdownloader.extensions.jdtrayicon.translate.TrayiconTranslation;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class TrayExtension extends AbstractExtension<TrayConfig, TrayiconTranslation> implements MouseListener, MouseMotionListener, WindowStateListener, ActionListener, ShutdownVetoListener, MainFrameClosingHandler {

    private LinkCollectorHighlightListener highListener = new LinkCollectorHighlightListener() {

                                                            @Override
                                                            public void onHighLight(CrawledLink parameter) {
                                                                if (guiFrame != null && !guiFrame.isVisible()) {
                                                                    /*
                                                                     * dont try to restore jd if password required
                                                                     */
                                                                    if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
                                                                        /**
                                                                         * do nothing , because a password is set
                                                                         */
                                                                        return;
                                                                    }
                                                                }
                                                                miniIt(false);
                                                                if (iconified) {
                                                                    /*
                                                                     * restore normale state,if windows was iconified
                                                                     */
                                                                    new EDTHelper<Object>() {
                                                                        @Override
                                                                        public Object edtRun() {
                                                                            /*
                                                                             * after this normal , its back to iconified
                                                                             */
                                                                            guiFrame.setState(JFrame.NORMAL);
                                                                            return null;
                                                                        }
                                                                    }.start();
                                                                }
                                                            }

                                                            @Override
                                                            public boolean isThisListenerEnabled() {
                                                                LinkgrabberResultsOption option = getSettings().getShowLinkgrabbingResultsOption();
                                                                if ((guiFrame != null && !guiFrame.isVisible() && option == LinkgrabberResultsOption.ONLY_IF_MINIMIZED) || option == LinkgrabberResultsOption.ALWAYS) { return true; }
                                                                return false;
                                                            }
                                                        };

    @Override
    protected void stop() throws StopException {
        removeTrayIcon();
        if (guiFrame != null) {
            JDGui.getInstance().setClosingHandler(null);
            guiFrame.removeWindowStateListener(this);
            miniIt(false);
            guiFrame.setAlwaysOnTop(false);
            guiFrame = null;
        }
        LinkCollector.getInstance().getEventsender().removeListener(highListener);
        ShutdownController.getInstance().removeShutdownVetoListener(TrayExtension.this);
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
        Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            if (SwingGui.getInstance() != null) {
                                LinkCollector.getInstance().getEventsender().addListener(highListener);
                                ShutdownController.getInstance().addShutdownVetoListener(TrayExtension.this);
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
    public boolean isQuickToggleEnabled() {
        return false;
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_jdtrayicon_jdlighttray_description();
    }

    @Override
    public AddonPanel<TrayExtension> getGUI() {
        return null;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    public java.util.ArrayList<JMenuItem> getMenuAction() {
        return null;
    }

    private TrayIconPopup                       trayIconPopup;

    private TrayIcon                            trayIcon;

    private JFrame                              guiFrame;

    private TrayIconTooltip                     trayIconTooltip;

    private TrayMouseAdapter                    ma;

    private boolean                             iconified       = false;

    private Timer                               disableAlwaysonTop;
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

    public TrayExtension() throws StartException {
        setTitle(T._.jd_plugins_optional_jdtrayicon_jdlighttray());

        disableAlwaysonTop = new Timer(2000, this);
        disableAlwaysonTop.setInitialDelay(2000);
        disableAlwaysonTop.setRepeats(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == disableAlwaysonTop) {
            if (guiFrame != null) guiFrame.setAlwaysOnTop(false);
        }
        return;
    }

    private void initGUI(final boolean startup) {
        try {
            SystemTray systemTray = SystemTray.getSystemTray();
            BufferedImage img = IconIO.getScaledInstance(NewTheme.I().getImage("logo/jd_logo_64_64", -1), (int) systemTray.getTrayIconSize().getWidth(), (int) systemTray.getTrayIconSize().getHeight());
            // if (CrossSystem.isMac()) {
            // img = ImageProvider.convertToGrayScale(img);
            // }

            // workaround for gnome 3 transparency bug
            if (System.getProperty("sun.desktop").equals("gnome")) { // gnome desktop
                // img = ImageProvider.convertToGrayScale(img);
                java.awt.Robot robo = new java.awt.Robot();
                Color tmp, newColor;
                int cr, cb, cg, alpha;
                for (int y = 0; y < img.getHeight(); y++) {
                    newColor = robo.getPixelColor(0, y);
                    for (int x = 0; x < img.getWidth(); x++) {
                        tmp = new Color(img.getRGB(x, y));
                        alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                        // calculate new color values for each channel
                        cr = (alpha / 255) * tmp.getRed() + ((255 - alpha) / 255) * newColor.getRed();
                        cg = (alpha / 255) * tmp.getGreen() + ((255 - alpha) / 255) * newColor.getGreen();
                        cb = (alpha / 255) * tmp.getBlue() + ((255 - alpha) / 255) * newColor.getBlue();
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
            trayIcon.addActionListener(this);
            ma = new TrayMouseAdapter(this, trayIcon);
            trayIcon.addMouseListener(ma);
            trayIcon.addMouseMotionListener(ma);
            trayIconTooltip = new TrayIconTooltip();
            systemTray.add(trayIcon);
        } catch (Throwable e) {
            /*
             * on Gnome3, Unity, this can happen because icon might be blacklisted, see here http://www.webupd8.org/2011/04/how-to-re-enable
             * -notification-area.html
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
        Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                LogController.CL(TrayExtension.class).info("JDLightTrayIcon Init complete");
                guiFrame = JDGui.getInstance().getMainFrame();
                if (guiFrame != null) {

                    JDGui.getInstance().setClosingHandler(TrayExtension.this);
                    guiFrame.removeWindowStateListener(TrayExtension.this);
                    guiFrame.addWindowStateListener(TrayExtension.this);
                    if (startup && getSettings().isStartMinimizedEnabled()) {
                        miniIt(true);
                    }
                }
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
                    miniIt(guiFrame.isVisible());
                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        if (!checkPassword()) return;
                        trayIconPopup = new TrayIconPopup();
                        calcLocation(trayIconPopup, e.getPoint());
                        trayIconPopup.setVisible(true);
                        trayIconPopup.startAutoHide();
                    }
                }
            } else {
                if (e.getClickCount() >= (getSettings().isToogleWindowStatusWithSingleClickEnabled() ? 1 : 2) && !SwingUtilities.isLeftMouseButton(e)) {
                    miniIt(guiFrame.isVisible() & guiFrame.getState() != Frame.ICONIFIED);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        if (!checkPassword()) return;
                        trayIconPopup = new TrayIconPopup();
                        Point pointOnScreen = e.getLocationOnScreen();
                        if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                        calcLocation(trayIconPopup, pointOnScreen);
                        trayIconPopup.setVisible(true);
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

    public void miniIt(final boolean minimize) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                /* set visible state */
                guiFrame.setVisible(!minimize);
                if (minimize == false) {
                    if (guiFrame.isVisible()) {
                        /* workaround for : toFront() */
                        new EDTHelper<Object>() {
                            @Override
                            public Object edtRun() {
                                if (trayIconChecker != null) {
                                    trayIconChecker.interrupt();
                                    trayIconChecker = null;
                                }
                                guiFrame.setAlwaysOnTop(true);
                                disableAlwaysonTop.restart();
                                guiFrame.toFront();
                                return null;
                            }
                        }.start();
                    }
                } else {
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            trayIconChecker = new Thread() {

                                @Override
                                public void run() {
                                    boolean reInitNeeded = false;
                                    while (Thread.currentThread() == trayIconChecker) {
                                        boolean reInitTrayIcon = false;
                                        try {
                                            reInitTrayIcon = 0 == SystemTray.getSystemTray().getTrayIcons().length;
                                        } catch (UnsupportedOperationException e) {
                                            if (reInitNeeded == false) {
                                                reInitNeeded = true;
                                                LogController.CL(TrayExtension.class).severe("TrayIcon gone?! WTF? We will try to restore as soon as possible");
                                            }
                                        }
                                        if (reInitTrayIcon) {
                                            removeTrayIcon();
                                            initGUI(false);
                                            try {
                                                if (SystemTray.getSystemTray().getTrayIcons().length > 0) {
                                                    reInitNeeded = false;
                                                    LogController.CL(TrayExtension.class).severe("TrayIcon restored!");
                                                }
                                            } catch (UnsupportedOperationException e) {
                                            }
                                        }
                                        try {
                                            Thread.sleep(15000);
                                        } catch (InterruptedException e) {
                                            break;
                                        }
                                    }
                                }

                            };
                            trayIconChecker.setDaemon(true);
                            trayIconChecker.setName("TrayIconRestore");
                            trayIconChecker.start();
                            return null;
                        }
                    }.start();
                }
                return null;
            }
        }.start();
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

                miniIt(true);
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
    public void onShutdown(boolean silent) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {

    }

    @Override
    public void onShutdownVeto(ShutdownVetoException[] shutdownVetoExceptions) {
    }

    @Override
    public void onSilentShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
    }

    private OnCloseAction windowClosedTray(final WindowEvent e) {
        final OnCloseAction[] ret = new OnCloseAction[1];
        ret[0] = null;
        final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK, _.JDGui_windowClosing_try_title_(), _.JDGui_windowClosing_try_msg_2(), NewTheme.I().getIcon("exit", 32), _.JDGui_windowClosing_try_asnwer_close(), null);

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

        LazyExtension tray = null;
        try {
            lastCloseRequest = System.currentTimeMillis();
            main: if (isEnabled()) {

                switch (getSettings().getOnCloseAction()) {
                case ASK:
                    switch (windowClosedTray(e)) {
                    case ASK:
                        // cancel clicked

                        return;
                    case EXIT:
                        // exit clicked

                        // set source to null in order to avoid further actions in - for example the Tray extension listsners

                        if (!CrossSystem.isMac()) ShutdownController.getInstance().removeShutdownVetoListener(RlyExitListener.getInstance());

                        break main;

                    case TO_TASKBAR:
                        JDGui.getInstance().getMainFrame().setExtendedState(JFrame.ICONIFIED);
                        return;
                    case TO_TRAY:
                        if (SystemTray.isSupported()) {
                            miniIt(true);
                            return;

                        }
                    }
                case EXIT:
                    if (!CrossSystem.isMac()) ShutdownController.getInstance().removeShutdownVetoListener(RlyExitListener.getInstance());

                    break main;
                case TO_TASKBAR:
                    JDGui.getInstance().getMainFrame().setExtendedState(JFrame.ICONIFIED);
                    return;

                case TO_TRAY:
                    if (SystemTray.isSupported()) {
                        miniIt(true);
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
                    JDGui.getInstance().getMainFrame().setVisible(false);
                    return null;
                }
            }.start();
            return;
        }
        org.jdownloader.controlling.JDRestartController.getInstance().exit(true);
    }

}