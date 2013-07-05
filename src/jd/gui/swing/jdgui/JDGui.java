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

package jd.gui.swing.jdgui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;

import jd.SecondLevelLaunch;
import jd.config.ConfigContainer;
import jd.gui.UIConstants;
import jd.gui.swing.SwingGui;
import jd.gui.swing.dialog.AbstractCaptchaDialog;
import jd.gui.swing.jdgui.components.StatusBarImpl;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.JDMenuBar;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.Screen;
import net.miginfocom.swing.MigLayout;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.utils.Application;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogHandler;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.TimerDialog;
import org.appwork.utils.swing.dialog.TimerDialog.InternDialog;
import org.appwork.utils.swing.dialog.locator.DialogLocator;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.appwork.utils.swing.locator.AbstractLocator;
import org.jdownloader.gui.GuiUtils;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.jdtrayicon.TrayExtension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.FrameStatus;
import org.jdownloader.settings.FrameStatus.ExtendedState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.SilentModeSettings.DialogDuringSilentModeAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.UpdateController;

public class JDGui extends SwingGui {

    private static JDGui INSTANCE;

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits existierende zurück
     * 
     * @return
     */
    public static JDGui getInstance() {
        if (JDGui.INSTANCE == null) {
            JDGui.INSTANCE = new EDTHelper<JDGui>() {
                @Override
                public JDGui edtRun() {
                    try {
                        return new JDGui();
                    } finally {

                    }
                }

            }.getReturnValue();
        }
        return JDGui.INSTANCE;
    }

    private JDMenuBar               menuBar;

    private StatusBarImpl           statusBar;
    private MainTabbedPane          mainTabbedPane;
    private DownloadsView           downloadView;

    private LinkGrabberView         linkgrabberView;
    private MainToolBar             toolBar;
    private JPanel                  waitingPane;

    private MainFrameClosingHandler closingHandler;

    private LogSource               logger;

    private TrayExtension           tray;

    private Thread                  initThread = null;

    public Thread getInitThread() {
        return initThread;
    }

    public MainFrameClosingHandler getClosingHandler() {
        return closingHandler;
    }

    public void setClosingHandler(MainFrameClosingHandler closingHandler) {
        this.closingHandler = closingHandler;
    }

    private JDGui() {
        super("JDownloader");
        // Important for unittests
        this.mainFrame.setName("MAINFRAME");
        RememberRelativeDialogLocator locator;
        // set a default locator to remmber dialogs position
        AbstractDialog.setDefaultLocator(locator = new RememberRelativeDialogLocator("", mainFrame) {

            @Override
            public void onClose(AbstractDialog<?> abstractDialog) {
                if (!abstractDialog.hasBeenMoved()) return;
                super.onClose(abstractDialog);
            }

            @Override
            protected String getID(Window frame) {
                try {
                    if (frame instanceof InternDialog) {

                        AbstractDialog dialog = (AbstractDialog) ((InternDialog) frame).getDialogModel();

                        String key = dialog.getTitle();
                        if (StringUtils.isEmpty(key)) {
                            key = dialog.toString();
                        }
                        return Hash.getMD5(key);
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
                return super.getID(frame);
            }

        });
        locator.setFallbackLocator(new DialogLocator() {

            @Override
            public Point getLocationOnScreen(AbstractDialog<?> abstractDialog) {
                if (abstractDialog.getDialog().getParent() != null && abstractDialog.getDialog().getParent().isShowing()) { return AbstractDialog.LOCATE_CENTER_OF_SCREEN.getLocationOnScreen(abstractDialog); }
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
                /* WARNING: this can cause deadlock under linux EDT/XAWT */
                final Rectangle bounds = gd.getDefaultConfiguration().getBounds();
                bounds.y += insets.top;
                bounds.x += insets.left;
                bounds.width -= insets.left + insets.right;
                bounds.height -= insets.top + insets.bottom;

                Point ret = AbstractLocator.validate(new Point(bounds.width - abstractDialog.getDialog().getSize().width, bounds.height - abstractDialog.getDialog().getSize().height), abstractDialog.getDialog());
                return ret;
            }

            @Override
            public void onClose(AbstractDialog<?> abstractDialog) {
            }

        });

        logger = LogController.getInstance().getLogger("Gui");
        this.setWindowTitle("JDownloader");
        this.initDefaults();
        this.initComponents();
        this.setWindowIcon();
        this.layoutComponents();
        this.mainFrame.pack();
        Dialog.getInstance().setParentOwner(this.mainFrame);

        initThread = new Thread("initLocationAndDimension") {
            @Override
            public void run() {
                try {
                    initLocationAndDimension();
                } finally {
                    initThread = null;
                }
            }
        };
        initThread.start();

        initToolTipSettings();

        mainFrame.addWindowListener(new WindowListener() {

            public void windowOpened(WindowEvent e) {

                UpdateController.getInstance().setGuiToFront(mainFrame);

            }

            public void windowIconified(WindowEvent e) {

            }

            public void windowDeiconified(WindowEvent e) {

                UpdateController.getInstance().setGuiToFront(mainFrame);

            }

            public void windowDeactivated(WindowEvent e) {

            }

            public void windowClosing(WindowEvent e) {

            }

            public void windowClosed(WindowEvent e) {

            }

            public void windowActivated(WindowEvent e) {

                if (e.getOppositeWindow() == null) {
                    // new activation
                    // if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogsRequestFocusEnabled()) {

                    for (final Window w : mainFrame.getOwnedWindows()) {
                        if (w.isShowing()) {
                            if (w instanceof TimerDialog.InternDialog) {
                                TimerDialog dialogModel = ((TimerDialog.InternDialog) w).getDialogModel();
                                if (dialogModel instanceof AbstractCaptchaDialog) {

                                    w.toFront();
                                    break;

                                }
                            }
                        }
                    }
                    // }

                } else {
                    // from a different jd frame

                }
                Dialog.getInstance().setParentOwner(getMainFrame());

            }
        });
        mainFrame.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(WindowEvent e) {

            }

            @Override
            public void windowGainedFocus(WindowEvent e) {

            }
        });
        mainFrame.addWindowStateListener(new WindowStateListener() {

            @Override
            public void windowStateChanged(WindowEvent e) {

            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new KeyEventPostProcessor() {

            public boolean postProcessKeyEvent(final KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_RELEASED && e.isShiftDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                    try {
                        /*
                         * dirty little helper for mac os problem, unable to reach window header
                         */
                        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                        JDGui.this.mainFrame.setExtendedState(Frame.NORMAL);
                        JDGui.this.mainFrame.setSize(new Dimension(800, 600));
                        final Rectangle abounds = JDGui.this.mainFrame.getBounds();
                        JDGui.this.mainFrame.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
                        LogController.GL.info("Center MainFrame");
                        return true;
                    } catch (final Exception ee) {
                        LogController.GL.log(ee);
                    }
                }
                return false;
            }
        });
        // Launcher.INIT_COMPLETE
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {

                ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                    @Override
                    public void onShutdown(final ShutdownRequest shutdownRequest) {
                        new EDTHelper<Object>() {

                            @Override
                            public Object edtRun() {

                                JDGui.this.mainTabbedPane.onClose();

                                JsonConfig.create(GraphicalUserInterfaceSettings.class).setLastFrameStatus(FrameStatus.create(mainFrame, JsonConfig.create(GraphicalUserInterfaceSettings.class).getLastFrameStatus()));

                                JDGui.this.getMainFrame().setVisible(false);
                                JDGui.this.getMainFrame().dispose();
                                return null;

                            }
                        }.getReturnValue();
                    }
                });

            }

        });

        if (Application.getJavaVersion() >= 17000000) {
            CFG_GUI.WINDOWS_REQUEST_FOCUS_ON_ACTIVATION_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
                {
                    getMainFrame().setAutoRequestFocus(CFG_GUI.WINDOWS_REQUEST_FOCUS_ON_ACTIVATION_ENABLED.isEnabled());
                }

                @Override
                public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                }

                @Override
                public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                    getMainFrame().setAutoRequestFocus(CFG_GUI.WINDOWS_REQUEST_FOCUS_ON_ACTIVATION_ENABLED.isEnabled());
                }
            });
        } else {
            logger.warning("  CFG_GUI.WINDOWS_REQUEST_FOCUS_ON_ACTIVATION_ENABLED not working for java <1.7");
        }
        // Hook into dialog System

        Dialog.getInstance().setHandler(new DialogHandler() {

            @Override
            public <T> T showDialog(AbstractDialog<T> dialog) throws DialogClosedException, DialogCanceledException {
                // synchronized (this) {
                try {
                    boolean silentModeActive = isSilentModeActive();
                    if (silentModeActive && CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION.getValue() == DialogDuringSilentModeAction.CANCEL_DIALOG) {
                        // Cancel dialog
                        throw new DialogClosedException(Dialog.RETURN_CLOSED);
                    }
                    // if this is the edt, we should not block it.. NEVER
                    if (!SwingUtilities.isEventDispatchThread()) {
                        // block dialog calls... the shall appear as soon as isSilentModeActive is false.
                        long countdown = -1;
                        dialog.forceDummyInit();
                        try {
                            if (dialog.evaluateDontShowAgainFlag()) {
                                final int mask = dialog.getReturnmask();
                                if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { throw new DialogClosedException(mask); }
                                if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { throw new DialogCanceledException(mask); }
                                return dialog.getReturnValue();
                            }
                        } catch (DialogNoAnswerException e) {
                            throw e;
                        } catch (Exception e) {
                            // Dialogs are not initialized.... nullpointers are very likly
                            // These nullpointers should be fixed
                            // in this case, we should continue normal
                            logger.log(e);
                        }
                        if (dialog.isCountdownFlagEnabled()) {
                            long countdownDif = dialog.getCountdown() * 1000;
                            countdown = System.currentTimeMillis() + countdownDif;
                        }
                        if (countdown < 0 && CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION.getValue() == DialogDuringSilentModeAction.WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT) {
                            countdown = System.currentTimeMillis() + CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION_TIMEOUT.getValue();

                        }
                        flashTaskbar();
                        while (isSilentModeActive()) {
                            if (countdown > 0) {
                                Thread.sleep(Math.min(Math.max(1, countdown - System.currentTimeMillis()), 250));
                                if (System.currentTimeMillis() > countdown) {
                                    dialog.onTimeout();
                                    // clear interrupt
                                    Thread.interrupted();
                                    final int mask = dialog.getReturnmask();
                                    if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { throw new DialogClosedException(mask); }
                                    if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { throw new DialogCanceledException(mask); }
                                    try {
                                        return dialog.getReturnValue();

                                    } catch (Exception e) {
                                        // dialogs have not been initialized. so the getReturnValue might fail.
                                        logger.log(e);
                                        throw new DialogClosedException(Dialog.RETURN_CLOSED | Dialog.RETURN_TIMEOUT);
                                    }
                                }
                            } else {
                                Thread.sleep(250);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new DialogClosedException(Dialog.RETURN_INTERRUPT, e);
                } catch (DialogClosedException e) {
                    throw e;
                } catch (DialogCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    logger.log(e);
                }
                dialog.resetDummyInit();
                return Dialog.getInstance().getDefaultHandler().showDialog(dialog);
                // }

            }
        });
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread("StatsDialog") {
                    public void run() {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showStatsDialog();

                    }
                }.start();
                Application.getResource("/tmp/update/self/JDU").mkdirs();
                new Thread() {
                    public void run() {
                        logger.info("Update bug Finder");
                        int counter = -1;
                        try {
                            logger.info("Find Counter");
                            counter = org.appwork.Counter.VALUE;
                            logger.info(Application.getJarFile(org.appwork.Counter.class).getAbsolutePath());
                            logger.info("Done: " + counter);
                        } catch (Throwable e) {
                            logger.log(e);

                        }
                        long start = System.currentTimeMillis();
                        while (UpdateController.getInstance().getHandler() == null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (System.currentTimeMillis() - start > 30000) {
                                logger.info("Handler null");
                                return;
                            }
                        }
                        logger.info("Gogogo " + counter);
                        if (counter != 1) {
                            try {
                                logger.info("Delete jdu");
                                Files.deleteRecursiv(Application.getResource("cfg/versioninfo/JDU"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                logger.info("Delete extensioncache");
                                Files.deleteRecursiv(Application.getResource("tmp/extensioncache"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            File rev = Application.getResource("/tmp/update/self/JDU/rev");
                            logger.info("create dummy rev");
                            if (!rev.exists()) {
                                rev.getParentFile().mkdirs();
                                try {
                                    IO.writeStringToFile(rev, "0");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            logger.info("Show Message");
                            Dialog.getInstance().showMessageDialog("This is a very important Update. You should run this NOW!");
                            // runUpdateChecker is synchronized and may block
                            logger.info("Init update");
                            UpdateController.getInstance().setGuiVisible(true);
                            UpdateController.getInstance().runUpdateChecker(true);
                        }
                    }
                }.start();

            }
        });

        // init tray
        tray = new TrayExtension();
        try {
            tray.init();
        } catch (Exception e1) {
            logger.log(e1);
        }
    }

    protected void showStatsDialog() {

        ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.JDGui_showStatsDialog_title_(), _GUI._.JDGui_showStatsDialog_message_(), NewTheme.I().getIcon("bug", 32), _GUI._.JDGui_showStatsDialog_yes_(), _GUI._.JDGui_showStatsDialog_no_());
        d.setDoNotShowAgainSelected(true);
        try {
            Dialog.getInstance().showDialog(d);
            if (!d.isHiddenByDontShowAgain()) {
                StatsManager.I().setEnabled(true);
            }
            return;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    private void initToolTipSettings() {

        ToolTipController.getInstance().setClassicToolstipsEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());

        ToolTipManager.sharedInstance().setEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
        CFG_GUI.TOOLTIP_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                ToolTipController.getInstance().setClassicToolstipsEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
                ToolTipManager.sharedInstance().setEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
    }

    @Override
    public void disposeView(View view) {
        if (view == null) return;
        view = this.mainTabbedPane.getComponentEquals(view);
        if (view == null) return;
        this.mainTabbedPane.remove(view);
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

    private void initComponents() {
        this.menuBar = JDMenuBar.getInstance();
        this.statusBar = new StatusBarImpl();
        this.waitingPane = new JPanel();
        this.waitingPane.setOpaque(false);
        this.mainTabbedPane = MainTabbedPane.getInstance();
        this.toolBar = MainToolBar.getInstance();
        this.toolBar.registerAccelerators(this);
        this.downloadView = new DownloadsView();
        this.linkgrabberView = new LinkGrabberView();
        this.mainTabbedPane.addTab(downloadView);
        this.mainTabbedPane.addTab(this.linkgrabberView);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isConfigViewVisible()) {
                            mainTabbedPane.addTab(ConfigurationView.getInstance());
                        }
                        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isLogViewVisible()) {
                            // this.mainTabbedPane.addTab(LogView.getInstance());
                        }
                    }

                };
            }
        });

        this.mainTabbedPane.setSelectedComponent(this.downloadView);

        if (CrossSystem.isMac()) {
            // add handling for Command+W for closing window on Mac OS
            KeyStroke closeKey = KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            this.mainTabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(closeKey, "closeWindow");
            this.mainTabbedPane.getActionMap().put("closeWindow", new AbstractAction() {
                private static final long serialVersionUID = 9149139888018750308L;

                public void actionPerformed(ActionEvent e) {
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            JDGui.this.getMainFrame().setVisible(false);
                            return null;
                        }
                    }.start();
                }
            });
        }

    }

    public StatusBarImpl getStatusBar() {
        return statusBar;
    }

    private void initDefaults() {
        this.mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.mainFrame.addWindowListener(this);

        // Directly reshow another tooltip
        ToolTipManager.sharedInstance().setReshowDelay(0);
        // Increase time a tooltip is displayed (default is 4000)
        ToolTipManager.sharedInstance().setDismissDelay(6000);
    }

    /**
     * under Linux EDT and XAWT can cause deadlock when we call getDefaultConfiguration() inside EDT, so I moved this to work outside EDT
     * and only put the mainframe stuff into EDT
     * 
     * restores the dimension and location to the window
     */
    private void initLocationAndDimension() {

        FrameStatus stat = JsonConfig.create(GraphicalUserInterfaceSettings.class).getLastFrameStatus();

        if (stat == null) {
            stat = new FrameStatus();
        }
        final FrameStatus status = stat;
        Dimension dim = null;
        if (status.getWidth() > 0 && status.getHeight() > 0) {
            dim = new Dimension(status.getWidth(), status.getHeight());
        }
        if (dim == null) {
            dim = new Dimension(1024, 728);
        }

        Point loc = new Point(status.getX(), status.getY());
        GraphicsDevice lastScreen = null;
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        for (final GraphicsDevice screen : screens) {
            if (screen.getIDstring() != null && screen.getIDstring().equals(status.getScreenID())) {
                lastScreen = screen;
                break;
            }
        }
        if (lastScreen == null) {
            lastScreen = GUIUtils.getScreenDevice(status.getX(), status.getY());
        }
        switch (status.getExtendedState()) {
        case MAXIMIZED_BOTH:
            if (lastScreen != null) {
                loc = lastScreen.getDefaultConfiguration().getBounds().getLocation();
            }
            break;
        case MAXIMIZED_HORIZ:
            // TODO: no idea what this should do. seems do have no effect for win7
            // Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_HORIZ)==false
            // check on other OSs
            break;

        case MAXIMIZED_VERT:
            // TODO: no idea what this should do. seems do have no effect for win7
            // Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_VERT)==false
            // check on other OSs
            break;
        }

        if (!status.isLocationSet()) {
            loc = Screen.getCenterOfComponent(null, mainFrame);
        } else if (lastScreen == null) {
            loc = Screen.getCenterOfComponent(null, mainFrame);
        }

        // try to find offscreen
        logger.info("Check if Screen Location are ok " + loc + " - " + dim);

        Rectangle jdRectange = new Rectangle(loc, dim);
        boolean isok = false;
        for (final GraphicsDevice screen : screens) {
            logger.info("Screen: " + screen.getIDstring() + " " + screen.getDefaultConfiguration());
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();

            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());

            logger.info("Screenbounds: " + bounds);
            logger.info("Screen Insets: " + insets);
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= (insets.left + insets.right);
            bounds.height -= (insets.top + insets.bottom);
            logger.info("New Screenbounds: " + bounds);

            jdRectange.height = 30;
            Rectangle inter = jdRectange.intersection(bounds);

            logger.info("Intersection(actual): " + inter);
            if (inter.width > 50 && inter.height >= 30) {
                // ok. Titlebar is in screen.
                isok = true;
                logger.info("Screen OK");
                break;

            } else {
                logger.info("Screen BAD");
            }

        }
        final Dimension fDim2 = dim;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                mainFrame.setSize(fDim2);
            }
        };

        Point finalLocation;
        if (!isok) {

            final Dimension fDim = dim;
            final Point cuLoc = loc;
            finalLocation = new EDTHelper<Point>() {
                @Override
                public Point edtRun() {
                    mainFrame.setSize(fDim);
                    return AbstractLocator.correct(cuLoc, mainFrame);
                }
            }.getReturnValue();

            logger.info("Screen BAD: Reset the Location to: " + finalLocation + "@" + dim);

        } else {
            // Point correctedLocation = AbstractLocator.correct(loc, mainFrame);
            // if (!correctedLocation.equals(loc)) {
            // logger.info("Corrected Location from " + loc + " to " + correctedLocation);
            // loc = correctedLocation;
            // }
            finalLocation = loc;
        }

        Integer state = null;
        if (status.isSilentShutdown() && !status.isActive()) {
            // else frame would jump to the front
            state = Frame.ICONIFIED;
        } else {
            if (status.getExtendedState() == ExtendedState.ICONIFIED) {
                state = Frame.NORMAL;
            } else {
                state = status.getExtendedState().getId();
            }
        }

        final Dimension finalDim = dim;
        final Integer finalState = state;
        final Point finalLocation2 = finalLocation;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (finalLocation2 != null) mainFrame.setLocation(finalLocation2);
                mainFrame.setMinimumSize(new Dimension(400, 100));
                mainFrame.setSize(finalDim);
                mainFrame.setPreferredSize(finalDim);
                if (finalState != null) mainFrame.setExtendedState(finalState);

                mainFrame.setVisible(true);
                // 7. Why a JFrame hides the OS TaskBar when being displayed maximized via JFrame#setExtendedState()?
                //
                // The described problem is a Swing issue which appears only with non-native decorated frames by using
                // JFrame#setExtendedState().
                // The workaround below can be used to fix the issue - whereas this is the JFrame instance:
                if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
                    ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
                }
            }
        }.waitForEDT();

    }

    protected void internalSetWaiting(final boolean b) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                JDGui.this.getMainFrame().setGlassPane(JDGui.this.waitingPane);
                JDGui.this.waitingPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                JDGui.this.getMainFrame().getGlassPane().setVisible(b);
                return null;
            }
        }.waitForEDT();
    }

    private void layoutComponents() {
        final JPanel contentPane = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]0[shrink]"));
        contentPane.add(this.mainTabbedPane);

        // contentPane.add(new
        // org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel());

        // contentPane.add(tb);
        contentPane.add(this.statusBar, "dock SOUTH");

        this.mainFrame.setContentPane(contentPane);
        this.mainFrame.setJMenuBar(this.menuBar);
        this.mainFrame.add(this.toolBar, "dock NORTH,height 38!");

    }

    // private void openSettings() {
    // final ConfigurationView config = ConfigurationView.getInstance();
    // if (!this.mainTabbedPane.contains(config)) {
    // this.mainTabbedPane.addTab(config);
    // }
    // this.mainTabbedPane.setSelectedComponent(config);
    // }

    @Override
    public void requestPanel(final Panels panel, final Object param) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                switch (panel) {
                case DOWNLOADLIST:
                    JDGui.this.mainTabbedPane.setSelectedComponent(JDGui.this.downloadView);
                    break;
                case LINKGRABBER:
                    JDGui.this.mainTabbedPane.setSelectedComponent(JDGui.this.linkgrabberView);

                    break;
                case PREMIUMCONFIG:
                    // ConfigurationView.getInstance().getSidebar().setSelectedTreeEntry(Premium.class);
                    // JDGui.this.openSettings();
                    // if (param != null && param instanceof Account) {
                    // final Premium p = (Premium)
                    // ConfigurationView.getInstance().getContent();
                    // p.setSelectedAccount((Account) param);
                    // }
                    // break;
                case CONFIGPANEL:
                    // if (param instanceof ConfigContainer) {
                    // if (((ConfigContainer) param).getEntries().isEmpty()) {
                    // return null; }
                    // JDGui.this.showConfigPanel((ConfigContainer) param);
                    // } else if (param instanceof Class<?>) {
                    // ConfigurationView.getInstance().getSidebar().setSelectedTreeEntry((Class<?>)
                    // param);
                    // JDGui.this.openSettings();
                    // }
                    // break;
                default:
                    JDGui.this.mainTabbedPane.setSelectedComponent(JDGui.this.downloadView);
                }
                return null;
            }
        }.start();
    }

    /**
     * Adds view to the main tabbedpane if setActive is true, the enw panel will be selected
     */
    @Override
    public void setContent(final View view, boolean setActive) {

        if (!this.mainTabbedPane.contains(view)) {
            this.mainTabbedPane.addTab(view);
        }
        if (setActive) this.mainTabbedPane.setSelectedComponent(view);
    }

    @Override
    public void setFrameStatus(final int id) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                switch (id) {
                case UIConstants.WINDOW_STATUS_MAXIMIZED:
                    mainFrame.setState(Frame.MAXIMIZED_BOTH);
                    mainFrame.setVisible(true);
                    break;
                case UIConstants.WINDOW_STATUS_MINIMIZED:
                    mainFrame.setState(Frame.ICONIFIED);
                    break;
                case UIConstants.WINDOW_STATUS_NORMAL:
                    if (!GuiUtils.isActiveWindow(getMainFrame())) {
                        mainFrame.setState(Frame.NORMAL);
                        mainFrame.setVisible(true);
                    }

                    break;
                case UIConstants.WINDOW_STATUS_FOREGROUND:
                    if (!GuiUtils.isActiveWindow(getMainFrame())) {

                        if (isSilentModeActive()) {
                            flashTaskbar();
                            return;
                        }
                        if (!requestCaptchaDialogFocus()) {
                            TimerDialog captchaDialog = null;
                            // if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogsRequestFocusEnabled()) {
                            //
                            // for (final Window w : mainFrame.getOwnedWindows()) {
                            // if (w.isShowing()) {
                            // if (w instanceof TimerDialog.InternDialog) {
                            // TimerDialog dialogModel = ((TimerDialog.InternDialog) w).getDialogModel();
                            // if (dialogModel instanceof AbstractCaptchaDialog) {
                            //
                            // captchaDialog = dialogModel;
                            // break;
                            //
                            // }
                            // }
                            // }
                            // }
                            // }
                            // final TimerDialog finalCaptchaDialog = captchaDialog;

                            if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isWindowsRequestFocusOnActivationEnabled()) {
                                mainFrame.setFocusableWindowState(false);
                            }
                            // if (finalCaptchaDialog != null) {
                            // finalCaptchaDialog.getDialog().setFocusableWindowState(false);
                            // }
                            mainFrame.setState(Frame.NORMAL);

                            mainFrame.setVisible(true);
                            mainFrame.toFront();
                            //
                            if (tray.isEnabled()) {
                                setWindowToTray(false);
                            }
                            // if (finalCaptchaDialog != null) {
                            // // it seems that we need this workaround on some systems. if we set the state to true imediatielly, this
                            // // would
                            // // not
                            // // work on some systems
                            // // http://stackoverflow.com/questions/4788953/is-it-possible-to-bring-jframe-to-front-but-not-focus
                            // Timer timer = new Timer(500, new ActionListener() {
                            //
                            // @Override
                            // public void actionPerformed(ActionEvent e) {
                            // finalCaptchaDialog.getDialog().setFocusableWindowState(true);
                            // }
                            // });
                            //
                            // timer.setRepeats(false);
                            // timer.start();
                            // }
                            if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isWindowsRequestFocusOnActivationEnabled()) {
                                // it seems that we need this workaround on some systems. if we set the state to true imediatielly, this
                                // would
                                // not
                                // work on some systems
                                // http://stackoverflow.com/questions/4788953/is-it-possible-to-bring-jframe-to-front-but-not-focus
                                Timer timer = new Timer(500, new ActionListener() {

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        mainFrame.setFocusableWindowState(true);
                                    }
                                });

                                timer.setRepeats(false);
                                timer.start();
                            }
                        }
                    } else {

                        logger.info("No To Top. We already have focus");
                    }

                    break;
                }
            }

        };

    }

    public boolean isSilentModeActive() {

        Boolean ret = new EDTHelper<Boolean>() {
            @Override
            public Boolean edtRun() {
                // don't block anthing if the frame is active anyway
                if ((getMainFrame().hasFocus() || getMainFrame().isActive()) && getMainFrame().isVisible()) { return false; }
                // don't block anything if the tray is active
                if (tray.isEnabled() && tray.isActive()) return false;

                if (CFG_SILENTMODE.MANUAL_ENABLED.isEnabled()) { return true; }
                switch (CFG_SILENTMODE.CFG.getAutoTrigger()) {
                case JD_IN_TASKBAR:
                    if (getMainFrame().getState() == JFrame.ICONIFIED && getMainFrame().isVisible()) return true;
                    break;
                case JD_IN_TRAY:
                    if (!getMainFrame().isVisible()) return true;
                    break;
                default:
                    return false;
                }
                return false;
            }
        }.getReturnValue();
        // 75|Gui 18.06.13 16:32:43 - SEVERE [ Gui ] -> java.lang.NullPointerException
        // at jd.gui.swing.jdgui.JDGui.isSilentModeActive(JDGui.java:986)
        // at jd.gui.swing.jdgui.JDGui$9.showDialog(JDGui.java:398)
        // at org.appwork.utils.swing.dialog.Dialog.showDialog(Dialog.java:561)
        // at org.appwork.utils.logging2.sendlogs.AbstractLogAction.create(AbstractLogAction.java:132)
        // at org.appwork.utils.logging2.sendlogs.AbstractLogAction$1.run(AbstractLogAction.java:68)
        // at org.appwork.utils.swing.dialog.ProgressDialog$3.run(ProgressDialog.java:217)
        if (ret == Boolean.TRUE) return true;
        return false;
    }

    public boolean requestCaptchaDialogFocus() {

        switch (CFG_GUI.CFG.getFocusTriggerForCaptchaDialogs()) {
        case NEVER:

            break;
        case MAINFRAME_IS_MAXIMIZED_OR_ICONIFIED_OR_TOTRAY:
            return requestCaptchaDialogFocusInternal();

        case MAINFRAME_IS_MAXIMIZED:

            if (getMainFrame().getState() != JFrame.ICONIFIED && JDGui.getInstance().getMainFrame().isVisible()) { return requestCaptchaDialogFocusInternal(); }

            break;

        case MAINFRAME_IS_MAXIMIZED_OR_ICONIFIED:
            if (getMainFrame().isVisible()) { return requestCaptchaDialogFocusInternal(); }
            break;

        default:
            //
        }

        return false;
    }

    public boolean requestCaptchaDialogFocusInternal() {
        for (final Window w : mainFrame.getOwnedWindows()) {
            if (w.isShowing()) {
                if (w instanceof TimerDialog.InternDialog) {
                    TimerDialog dialogModel = ((TimerDialog.InternDialog) w).getDialogModel();
                    if (dialogModel instanceof AbstractCaptchaDialog) {

                        w.toFront();
                        // w.requestFocus();
                        logger.info("Request Focus: " + w);
                        return true;

                    }
                }
            }
        }
        return false;
    }

    @Override
    public void setWaiting(final boolean b) {
        this.internalSetWaiting(b);
    }

    /**
     * Sets the Windows Icons. lot's of lafs have problems resizing the icon. so we set different sizes. for 1.5 it is only possible to use
     * {@link JFrame#setIconImage(Image)}
     */
    private void setWindowIcon() {
        /*
         * NOTE: on linux setIconImage only works when the frame is set invisible and visible again
         */
        /*
         * we only load a single resolution icon here to show a jd icon instead of java icon and not having a great impact on startup time
         */
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                Image image = NewTheme.I().getImage("logo/jd_logo_64_64", -1);
                if (Application.getJavaVersion() >= 16000000) {
                    final java.util.List<Image> list = new ArrayList<Image>();
                    list.add(image);
                    mainFrame.setIconImages(list);
                } else {
                    mainFrame.setIconImage(image);
                }
                return null;
            }
        }.start();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                if (Application.getJavaVersion() >= 16000000) {
                    final java.util.List<Image> list = new ArrayList<Image>();
                    list.add(NewTheme.I().getImage("logo/logo_14_14", -1));
                    list.add(NewTheme.I().getImage("logo/logo_15_15", -1));
                    list.add(NewTheme.I().getImage("logo/logo_16_16", -1));
                    list.add(NewTheme.I().getImage("logo/logo_17_17", -1));
                    list.add(NewTheme.I().getImage("logo/logo_18_18", -1));
                    list.add(NewTheme.I().getImage("logo/logo_19_19", -1));
                    list.add(NewTheme.I().getImage("logo/logo_20_20", -1));
                    list.add(NewTheme.I().getImage("logo/jd_logo_64_64", -1));
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            mainFrame.setIconImages(list);
                            return null;
                        }
                    }.start();
                } else {
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            mainFrame.setIconImage(NewTheme.I().getImage("logo/logo_17_17", -1));
                            return null;
                        }
                    }.start();
                }
            }
        });
    }

    public void setWindowTitle(final String msg) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                JDGui.this.mainFrame.setTitle(msg);
                return null;
            }
        }.start();
    }

    /**
     * Converts a {@link ConfigContainer} to a {@link AddonConfig} and displays it
     * 
     * @param container
     */
    protected void showConfigPanel(final ConfigContainer container) {
        String name = "";
        if (container.getTitle() != null) {
            name = container.getTitle();
        } else if (container.getGroup() != null && container.getGroup().getName() != null) {
            name = container.getGroup().getName();
        }

        ImageIcon icon = null;
        if (container.getIcon() != null) {
            icon = container.getIcon();
        } else if (container.getGroup() != null && container.getGroup().getIcon() != null) {
            icon = container.getGroup().getIcon();
        }

        final AddonConfig addonConfig = AddonConfig.getInstance(container, "_2", false);

        final JScrollPane scrollPane = new JScrollPane(addonConfig.getPanel());
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // this.mainTabbedPane.getSelectedView().setInfoPanel(col);
    }

    @Override
    public void windowClosing(final WindowEvent e) {
        if (e.getComponent() == this.getMainFrame()) {

            if (closingHandler != null) {
                closingHandler.windowClosing(e);
                return;
            }

            /*
             * without trayicon also dont close/exit for macos
             */
            if (CrossSystem.isMac()) {
                new EDTHelper<Object>() {
                    @Override
                    public Object edtRun() {
                        /* set visible state */
                        JDGui.this.getMainFrame().setVisible(false);
                        return null;
                    }
                }.start();
                return;
            }

            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());
        }
    }

    public static void help(final String title, final String msg, final ImageIcon icon) {

        Timer timer = new Timer(200, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                HelpDialog.show(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, icon);

            }
        });
        timer.setRepeats(false);
        timer.start();

    }

    @Override
    public boolean isCurrentPanel(Panels panelID) {
        switch (panelID) {
        case DOWNLOADLIST:
            return JDGui.this.downloadView == mainTabbedPane.getSelectedComponent();
        case LINKGRABBER:
            return JDGui.this.linkgrabberView == mainTabbedPane.getSelectedComponent();
        }
        return false;
    }

    public TrayExtension getTray() {
        return tray;
    }

    private Thread trayIconChecker;

    /**
     * Sets the window to tray or restores it. This method contains a lot of workarounds for individual system problems... Take care to
     * avoid sideeffects when changing anything
     * 
     * @param minimize
     */
    public void setWindowToTray(final boolean minimize) {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                /* set visible state */

                if (!minimize) {
                    getMainFrame().setVisible(true);

                    /* workaround for : toFront() */
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            if (trayIconChecker != null) {
                                trayIconChecker.interrupt();
                                trayIconChecker = null;
                            }

                            getMainFrame().setAlwaysOnTop(true);
                            Timer timer = new Timer(2000, new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    // if (getMainFrame().isAlwaysOnTop()) getMainFrame().setAlwaysOnTop(false);
                                }
                            });
                            timer.setRepeats(false);
                            timer.restart();
                            getMainFrame().toFront();
                            return null;
                        }
                    }.start();

                } else {
                    getMainFrame().setVisible(false);

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
                                        logger.severe("TrayIcon gone?! WTF? We will try to restore as soon as possible");
                                    }
                                }
                                if (reInitTrayIcon) {
                                    tray.initGUI(false);
                                    try {
                                        if (SystemTray.getSystemTray().getTrayIcons().length > 0) {
                                            reInitNeeded = false;
                                            logger.severe("TrayIcon restored!");
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
                }
                return null;
            }
        }.start();
    }

    public void flashTaskbar() {
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isTaskBarFlashEnabled() && CrossSystem.isWindows()) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    final int state = mainFrame.getExtendedState();
                    try {
                        if (state == ExtendedState.ICONIFIED.getId()) {
                            GuiUtils.flashWindow(mainFrame, true, true);
                        } else {
                            GuiUtils.flashWindow(mainFrame, true, true);
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            };
        }
    }
}