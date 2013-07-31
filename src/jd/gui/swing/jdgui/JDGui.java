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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
import jd.gui.swing.jdgui.components.StatusBarImpl;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.JDMenuBar;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.Screen;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.ActiveDialogException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.ExtJFrame;
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
import org.appwork.utils.swing.WindowManager;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.appwork.utils.swing.WindowManager.WindowExtendedState;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogHandler;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.InternDialog;
import org.appwork.utils.swing.dialog.OwnerFinder;
import org.appwork.utils.swing.dialog.WindowStack;
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
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class JDGui implements UpdaterListener, OwnerFinder {

    /**
     * An Abstract panelrepresentation used in requestPanel(Panels.*,Parameter
     * 
     * @author Coalado
     */
    public static enum Panels {
        CONFIG,

        /**
         * Represents a configview. Parameter is a {@link ConfigContainer} or the {@link Class} reference to an {@link ConfigPanel}.
         */
        CONFIGPANEL,

        /**
         * Represents the {@link DownloadView}.
         */
        DOWNLOADLIST,

        /**
         * Represents the {@link LinkgrabberView}.
         */
        LINKGRABBER,
        /**
         * Displays the {@link Premium}-ConfigPanel. The parameter is the account which should be selected.
         */
        PREMIUMCONFIG

    }

    private static JDGui INSTANCE;

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits existierende zurück
     * 
     * @return
     */
    public static JDGui getInstance() {

        return JDGui.INSTANCE;
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

    private MainFrameClosingHandler closingHandler;

    private volatile boolean        dialogShowing = false;

    private DownloadsView           downloadView;

    private Thread                  initThread    = null;

    private LinkGrabberView         linkgrabberView;
    private LogSource               logger;
    protected ExtJFrame             mainFrame;

    private MainTabbedPane          mainTabbedPane;
    private JDMenuBar               menuBar;
    private StatusBarImpl           statusBar;

    private MainToolBar             toolBar;

    private TrayExtension           tray;

    private Thread                  trayIconChecker;

    private JPanel                  waitingPane;

    private JDGui() {
        logger = LogController.getInstance().getLogger("Gui");
        initFrame("JDownloader");
        AbstractDialog.setDefaultRoot(getMainFrame());
        updateTitle();

        // Important for unittests
        this.mainFrame.setName("MAINFRAME");
        UpdateController.getInstance().getEventSender().addListener(this, true);
        initDialogLocators();

        this.setWindowTitle("JDownloader");

        AbstractDialog.setGlobalOwnerFinder(this);
        this.initDefaults();
        this.initComponents();
        this.setWindowIcon();
        this.layoutComponents();
        this.mainFrame.pack();

        initLocationAndDimension();
        //
        initToolTipSettings();

        initUpdateFrameListener();

        initCaptchaToFrontListener();

        initShiftControlSWindowResetKeyListener();
        // Launcher.INIT_COMPLETE
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                onGuiInitComplete();

            }

        });

        initSilentModeHooks();

        // init tray
        tray = new TrayExtension();
        try {
            tray.init();
        } catch (Exception e1) {
            logger.log(e1);
        }
    }

    public void disposeView(View view) {
        if (view == null) return;
        view = this.mainTabbedPane.getComponentEquals(view);
        if (view == null) return;
        this.mainTabbedPane.remove(view);
    }

    @Override
    public Window findDialogOwner(AbstractDialog<?> dialogModel, WindowStack windowStack) {

        return AbstractDialog.DEFAULT_OWNER_FINDER.findDialogOwner(dialogModel, windowStack);
    }

    public void flashTaskbar() {
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isTaskBarFlashEnabled() && CrossSystem.isWindows()) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    final int state = mainFrame.getExtendedState();
                    try {
                        if (state == ExtendedState.ICONIFIED.getId()) {
                            GuiUtils.flashWindow(mainFrame, true);
                        } else {
                            GuiUtils.flashWindow(mainFrame, true);
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            };
        }
    }

    public MainFrameClosingHandler getClosingHandler() {
        return closingHandler;
    }

    public Thread getInitThread() {
        return initThread;
    }

    public LogSource getLogger() {
        return logger;
    }

    /**
     * Returns the gui's mainjframe
     * 
     * @return
     */
    public JFrame getMainFrame() {
        return mainFrame;
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

    public StatusBarImpl getStatusBar() {
        return statusBar;
    }

    public TrayExtension getTray() {
        return tray;
    }

    public void initCaptchaToFrontListener() {

        mainFrame.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (e.getOppositeWindow() == null) {
                    for (Window w : Window.getWindows()) {
                        if (w instanceof InternDialog && !((InternDialog) w).getDialogModel().isDisposed()) {
                            Window owner = ((InternDialog) w).getOwner();

                            if (owner == null && w.isVisible()) {
                                WindowManager.getInstance().setZState(w, FrameState.TO_FRONT_FOCUSED);
                            }
                            // ((InternDialog)w).getDialogModel() instanceof AbstractCaptchaDialog)

                        }

                    }
                }

            }

            @Override
            public void windowLostFocus(WindowEvent e) {

            }
        });
        //
        // mainFrame.addWindowListener(new WindowListener() {
        //
        // @Override
        // public void windowOpened(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowIconified(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowDeiconified(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowDeactivated(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowClosing(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowClosed(WindowEvent windowevent) {
        // }
        //
        // @Override
        // public void windowActivated(WindowEvent e) {
        // if (e.getOppositeWindow() == null) {
        // // new activation
        // // if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isCaptchaDialogsRequestFocusEnabled()) {
        //
        // for (final Window w : mainFrame.getOwnedWindows()) {
        // if (w.isShowing()) {
        // if (w instanceof InternDialog) {
        // AbstractDialog dialogModel = ((InternDialog) w).getDialogModel();
        // if (dialogModel instanceof AbstractCaptchaDialog) {
        // WindowManager.getInstance().setZState(w, FrameState.TO_FRONT_FOCUSED);
        //
        // break;
        //
        // }
        // }
        // }
        // }
        // // }
        //
        // } else {
        // // from a different jd frame
        //
        // }
        // }
        // });
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
                            WindowManager.getInstance().setVisible(JDGui.this.getMainFrame(), false, FrameState.OS_DEFAULT);
                            return null;
                        }
                    }.start();
                }
            });
        }

    }

    private void initDefaults() {
        this.mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.mainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (e.getComponent() == getMainFrame()) {

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
                                WindowManager.getInstance().setVisible(JDGui.this.getMainFrame(), false, FrameState.OS_DEFAULT);
                                return null;
                            }
                        }.start();
                        return;
                    }

                    RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());
                }
            }
        });

        // Directly reshow another tooltip
        ToolTipManager.sharedInstance().setReshowDelay(0);
        // Increase time a tooltip is displayed (default is 4000)
        ToolTipManager.sharedInstance().setDismissDelay(6000);
    }

    public void initDialogLocators() {
        RememberRelativeDialogLocator locator;
        // set a default locator to remmber dialogs position
        AbstractDialog.setDefaultLocator(locator = new RememberRelativeDialogLocator("", mainFrame) {

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

            @Override
            public void onClose(AbstractDialog<?> abstractDialog) {
                if (!abstractDialog.hasBeenMoved()) return;
                super.onClose(abstractDialog);
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
    }

    private void initFrame(final String string) {

        mainFrame = new ExtJFrame(string) {

            /**
                 * 
                 */
            private static final long serialVersionUID = -4218493713632551975L;

            public void dispose() {

                super.dispose();
            }

            public void setVisible(boolean b) {
                if (b && !isVisible()) {
                    if (CFG_GUI.PASSWORD_PROTECTION_ENABLED.isEnabled() && !StringUtils.isEmpty(CFG_GUI.PASSWORD.getValue())) {
                        String password;
                        if (dialogShowing) return;
                        try {

                            dialogShowing = true;
                            password = Dialog.getInstance().showInputDialog(Dialog.STYLE_PASSWORD, _GUI._.JDGui_setVisible_password_(), _GUI._.JDGui_setVisible_password_msg(), null, NewTheme.I().getIcon("lock", 32), null, null);
                            String internPw = CFG_GUI.PASSWORD.getValue();
                            if (!internPw.equals(password)) {

                                Dialog.getInstance().showMessageDialog(_GUI._.JDGui_setVisible_password_wrong());

                                return;
                            }
                        } catch (DialogNoAnswerException e) {
                            return;
                        } finally {
                            dialogShowing = false;
                        }
                    }
                }
                // if we hide a frame which is locked by an active modal dialog,
                // we get in problems. avoid this!
                if (!b) {
                    for (Window w : getOwnedWindows()) {
                        if (w instanceof JDialog) {
                            boolean mod = ((JDialog) w).isModal();
                            boolean v = w.isVisible();

                            if (mod && v) {
                                Toolkit.getDefaultToolkit().beep();
                                logger.log(new ActiveDialogException(((JDialog) w)));
                                WindowManager.getInstance().setZState(w, FrameState.TO_FRONT_FOCUSED);

                                return;
                            }
                        }
                    }
                }
                super.setVisible(b);
            }

            public void toFront() {

                if (!isVisible()) return;
                super.toFront();
                //

            }
        };
    }

    public void initLocationAndDimension() {
        initThread = new Thread("initLocationAndDimension") {
            @Override
            public void run() {
                try {
                    internalInitLocationAndDimension();
                } finally {
                    initThread = null;
                }
            }
        };
        initThread.start();
    }

    public void initShiftControlSWindowResetKeyListener() {
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
    }

    public void initSilentModeHooks() {
        // Hook into dialog System

        Dialog.getInstance().setHandler(new DialogHandler() {

            @Override
            public <T> T showDialog(AbstractDialog<T> dialog) throws DialogClosedException, DialogCanceledException {
                // synchronized (this) {
                try {

                    dialog.forceDummyInit();
                    try {
                        if (dialog.evaluateDontShowAgainFlag()) {
                            final int mask = dialog.getReturnmask();
                            if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { throw new DialogClosedException(mask); }
                            if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { throw new DialogCanceledException(mask); }
                            if (!BinaryLogic.containsSome(mask, Dialog.RETURN_OK)) { throw new DialogCanceledException(mask | Dialog.RETURN_CLOSED); }
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

                    boolean silentModeActive = isSilentModeActive();
                    if (silentModeActive && CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION.getValue() == DialogDuringSilentModeAction.CANCEL_DIALOG) {
                        // Cancel dialog
                        throw new DialogClosedException(Dialog.RETURN_CLOSED);
                    }

                    // if this is the edt, we should not block it.. NEVER
                    if (!SwingUtilities.isEventDispatchThread()) {
                        // block dialog calls... the shall appear as soon as isSilentModeActive is false.
                        long countdown = -1;

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
                } finally {
                    dialog.resetDummyInit();
                }
                return Dialog.getInstance().getDefaultHandler().showDialog(dialog);
                // }

            }
        });
    }

    private void initToolTipSettings() {

        ToolTipController.getInstance().setClassicToolstipsEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());

        ToolTipManager.sharedInstance().setEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
        CFG_GUI.TOOLTIP_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                ToolTipController.getInstance().setClassicToolstipsEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
                ToolTipManager.sharedInstance().setEnabled(CFG_GUI.TOOLTIP_ENABLED.isEnabled());
            }
        });
    }

    public void initUpdateFrameListener() {
        mainFrame.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {

            }

            public void windowClosed(WindowEvent e) {

            }

            public void windowClosing(WindowEvent e) {

            }

            public void windowDeactivated(WindowEvent e) {

            }

            public void windowDeiconified(WindowEvent e) {

                UpdateController.getInstance().setGuiToFront(mainFrame);

            }

            public void windowIconified(WindowEvent e) {

            }

            public void windowOpened(WindowEvent e) {

                UpdateController.getInstance().setGuiToFront(mainFrame);

            }
        });
    }

    /**
     * under Linux EDT and XAWT can cause deadlock when we call getDefaultConfiguration() inside EDT, so I moved this to work outside EDT
     * and only put the mainframe stuff into EDT
     * 
     * restores the dimension and location to the window
     */
    private void internalInitLocationAndDimension() {

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

                WindowManager.getInstance().setVisible(mainFrame, true, FrameState.OS_DEFAULT);
                if (CrossSystem.isMac() && !mainFrame.isUndecorated()) {
                    mainFrame.addComponentListener(new ComponentAdapter() {

                        private String screenID;

                        @Override
                        public void componentMoved(ComponentEvent e) {

                            String newScreenID = mainFrame.getGraphicsConfiguration().getDevice().getIDstring();
                            if (!StringUtils.equals(newScreenID, screenID)) {
                                // 7. Why a JFrame hides the OS TaskBar when being displayed maximized via JFrame#setExtendedState()?
                                //
                                // The described problem is a Swing issue which appears only with non-native decorated frames by using
                                // JFrame#setExtendedState().
                                // The workaround below can be used to fix the issue - whereas this is the JFrame instance:
                                if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
                                    ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
                                    logger.info("Set Mainframe MaximizedBounds to: " + mainFrame.getMaximizedBounds());
                                }
                                screenID = newScreenID;
                            }

                        }

                    });
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

    public boolean isCurrentPanel(Panels panelID) {
        switch (panelID) {
        case DOWNLOADLIST:
            return JDGui.this.downloadView == mainTabbedPane.getSelectedComponent();
        case LINKGRABBER:
            return JDGui.this.linkgrabberView == mainTabbedPane.getSelectedComponent();
        }
        return false;
    }

    public boolean isSilentModeActive() {

        Boolean ret = new EDTHelper<Boolean>() {
            @Override
            public Boolean edtRun() {
                // don't block anthing if the frame is active anyway
                if ((getMainFrame().hasFocus() || getMainFrame().isActive()) && getMainFrame().isVisible()) { return false; }
                if (UpdateController.getInstance().getHandler() != null && GuiUtils.isActiveWindow(UpdateController.getInstance().getHandler().getGuiFrame())) return false;

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

    protected void onGuiInitComplete() {

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                new EDTHelper<Object>() {

                    @Override
                    public Object edtRun() {

                        JDGui.this.mainTabbedPane.onClose();

                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setLastFrameStatus(FrameStatus.create(mainFrame, JsonConfig.create(GraphicalUserInterfaceSettings.class).getLastFrameStatus()));

                        WindowManager.getInstance().setVisible(JDGui.this.getMainFrame(), false, FrameState.OS_DEFAULT);
                        JDGui.this.getMainFrame().dispose();

                        return null;

                    }
                }.getReturnValue();
            }
        });

        // new Thread("StatsDialog") {
        // public void run() {
        // try {
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // showStatsDialog();
        //
        // }
        // }.start();
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
                        Files.deleteRecursiv(Application.getResource("update/versioninfo/JDU"));
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

    @Override
    public void onUpdatesAvailable(final boolean selfupdate, final InstallLog installlog) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                updateTitle();

            }
        };
    }

    /**
     * requests a special view from the gui. example:
     * 
     * JDUtilities.getGUI().requestPanel(Panels.*,parameter);
     * 
     * asks the gui backend to display the downloadlist. IT depends on the guibackend which requests are fullfilled and which not.
     * 
     * @param panelID
     *            {@link Panels}
     * @param parameter
     *            TODO
     * @see Panels
     */

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

    public void setClosingHandler(MainFrameClosingHandler closingHandler) {
        this.closingHandler = closingHandler;
    }

    /**
     * Adds view to the main tabbedpane if setActive is true, the enw panel will be selected
     */

    public void setContent(final View view, boolean setActive) {

        if (!this.mainTabbedPane.contains(view)) {
            this.mainTabbedPane.addTab(view);
        }
        if (setActive) this.mainTabbedPane.setSelectedComponent(view);
    }

    public void setFrameState(FrameState toFrontFocused) {

        switch (toFrontFocused) {
        case OS_DEFAULT:
            if (isSilentModeActive()) {
                flashTaskbar();
            } else {

                if (WindowManager.getInstance().getExtendedState(mainFrame) == WindowExtendedState.ICONIFIED) {
                    WindowManager.getInstance().setExtendedState(mainFrame, WindowExtendedState.NORMAL);
                }
                WindowManager.getInstance().setVisible(mainFrame, true, toFrontFocused);
            }

            break;
        case TO_BACK:
            flashTaskbar();
            break;
        case TO_FRONT:
            if (isSilentModeActive()) {
                flashTaskbar();
            } else {
                if (WindowManager.getInstance().getExtendedState(mainFrame) == WindowExtendedState.ICONIFIED) {
                    WindowManager.getInstance().setExtendedState(mainFrame, WindowExtendedState.NORMAL);
                }
                WindowManager.getInstance().setVisible(mainFrame, true, toFrontFocused);

            }
            break;
        case TO_FRONT_FOCUSED:
            if (isSilentModeActive()) {
                flashTaskbar();
            } else {

                if (WindowManager.getInstance().getExtendedState(mainFrame) == WindowExtendedState.ICONIFIED) {
                    WindowManager.getInstance().setExtendedState(mainFrame, WindowExtendedState.NORMAL);
                }
                WindowManager.getInstance().setVisible(mainFrame, true, toFrontFocused);
            }
            break;
        }

    }

    public void setFrameStatus(final int id) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                switch (id) {
                case UIConstants.WINDOW_STATUS_MAXIMIZED:
                    mainFrame.setState(Frame.MAXIMIZED_BOTH);
                    WindowManager.getInstance().setVisible(mainFrame, true, FrameState.OS_DEFAULT);
                    break;
                case UIConstants.WINDOW_STATUS_MINIMIZED:
                    mainFrame.setState(Frame.ICONIFIED);
                    break;
                case UIConstants.WINDOW_STATUS_NORMAL:
                    if (!GuiUtils.isActiveWindow(getMainFrame())) {
                        mainFrame.setState(Frame.NORMAL);
                        WindowManager.getInstance().setVisible(mainFrame, true, FrameState.OS_DEFAULT);
                    }

                    break;
                case UIConstants.WINDOW_STATUS_FOREGROUND_NO_FOCUS:
                    if (!GuiUtils.isActiveWindow(getMainFrame())) {

                        if (isSilentModeActive()) {
                            flashTaskbar();
                            return;
                        }
                        {
                            // AbstractDialog captchaDialog = null;

                            // if (finalCaptchaDialog != null) {
                            // finalCaptchaDialog.getDialog().setFocusableWindowState(false);
                            // }

                            if (WindowManager.getInstance().getExtendedState(mainFrame) == WindowExtendedState.ICONIFIED) {
                                WindowManager.getInstance().setExtendedState(mainFrame, WindowExtendedState.NORMAL);
                            }
                            WindowManager.getInstance().setVisible(mainFrame, true, FrameState.TO_FRONT_FOCUSED);
                            //
                            if (tray.isEnabled()) {
                                setWindowToTray(false);
                            }

                        }
                    } else {

                        logger.info("No To Top. We already have focus");
                    }
                    break;
                case UIConstants.WINDOW_STATUS_FOREGROUND:
                    if (!GuiUtils.isActiveWindow(getMainFrame())) {

                        if (isSilentModeActive()) {
                            flashTaskbar();
                            return;
                        }
                        {
                            // AbstractDialog captchaDialog = null;

                            // if (finalCaptchaDialog != null) {
                            // finalCaptchaDialog.getDialog().setFocusableWindowState(false);
                            // }

                            if (WindowManager.getInstance().getExtendedState(mainFrame) == WindowExtendedState.ICONIFIED) {
                                WindowManager.getInstance().setExtendedState(mainFrame, WindowExtendedState.NORMAL);
                            }
                            WindowManager.getInstance().setVisible(mainFrame, true, FrameState.TO_FRONT);
                            //
                            if (tray.isEnabled()) {
                                setWindowToTray(false);
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

                    int estate = getMainFrame().getExtendedState();
                    if ((estate & JFrame.ICONIFIED) != 0) {
                        WindowManager.getInstance().setExtendedState(getMainFrame(), WindowExtendedState.NORMAL);
                    }
                    if (!getMainFrame().isVisible()) {
                        WindowManager.getInstance().setVisible(getMainFrame(), true, FrameState.TO_FRONT_FOCUSED);
                    }

                    if (trayIconChecker != null) {
                        trayIconChecker.interrupt();
                        trayIconChecker = null;
                    }

                } else {

                    WindowManager.getInstance().hide(getMainFrame());
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

    protected void updateTitle() {
        try {
            if (UpdateController.getInstance().hasPendingUpdates()) {
                getMainFrame().setTitle(_GUI._.JDGui_updateTitle_updates_available("JDownloader"));
            } else {
                getMainFrame().setTitle("JDownloader");
            }
        } catch (Exception e) {
            getMainFrame().setTitle("JDownloader");
        }
    }

    public static void init() {
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
    }

}