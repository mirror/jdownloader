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
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;

import jd.config.ConfigContainer;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkCheck;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberDistributeEvent;
import jd.event.ControlEvent;
import jd.gui.UIConstants;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.components.JDCollapser;
import jd.gui.swing.jdgui.components.StatusBar;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.JDMenuBar;
import jd.gui.swing.jdgui.views.downloads.DownloadView;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.linkgrabber.LinkgrabberView;
import jd.gui.swing.jdgui.views.log.LogView;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.Screen;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadsView;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class JDGui extends SwingGui implements LinkGrabberDistributeEvent {

    private static final long serialVersionUID = 1048792964102830601L;
    private static JDGui      INSTANCE;

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits
     * existierende zurück
     * 
     * @return
     */
    public static JDGui getInstance() {
        if (JDGui.INSTANCE == null) {
            JDGui.INSTANCE = new GuiRunnable<JDGui>() {
                @Override
                public JDGui runSave() {
                    return new JDGui();
                }

            }.getReturnValue();
        }
        return JDGui.INSTANCE;
    }

    private JDMenuBar       menuBar;

    private StatusBar       statusBar;
    private MainTabbedPane  mainTabbedPane;
    private DownloadView    downloadView;

    private LinkgrabberView linkgrabberView;
    private MainToolBar     toolBar;
    private JPanel          waitingPane;

    private boolean         exitRequested = false;

    private JDGui() {
        super("");
        // disable Clipboard while gui is loading
        ClipboardHandler.getClipboard().setTempDisabled(true);
        // Important for unittests
        this.mainFrame.setName("MAINFRAME");

        this.initDefaults();
        this.initComponents();

        this.setWindowIcon();
        this.setWindowTitle(JDUtilities.getJDTitle());
        this.layoutComponents();

        this.mainFrame.pack();
        Dialog.getInstance().setParentOwner(this.mainFrame);
        this.initLocationAndDimension();
        this.mainFrame.setVisible(true);
        if (this.mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) this.mainFrame.getRootPane().getUI()).setMaximizedBounds(this.mainFrame);
        }
        ClipboardHandler.getClipboard().setTempDisabled(false);
        LinkGrabberController.getInstance().setDistributer(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new KeyEventPostProcessor() {

            public boolean postProcessKeyEvent(final KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_RELEASED && e.isShiftDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                    try {
                        /*
                         * dirty little helper for mac os problem, unable to
                         * reach window header
                         */
                        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                        JDGui.this.mainFrame.setExtendedState(Frame.NORMAL);
                        JDGui.this.mainFrame.setSize(new Dimension(800, 600));
                        final Rectangle abounds = JDGui.this.mainFrame.getBounds();
                        JDGui.this.mainFrame.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
                        JDLogger.getLogger().info("Center MainFrame");
                        return true;
                    } catch (final Exception ee) {
                        JDLogger.exception(ee);
                    }
                }
                return false;
            }
        });

    }

    public void addLinks(final ArrayList<DownloadLink> links, final boolean hidegrabber, final boolean autostart) {
        if (links.size() == 0) { return; }
        if (hidegrabber || autostart) {
            new Thread() {
                @Override
                public void run() {
                    /* TODO: hier autopackaging ? */
                    final ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName("Added " + System.currentTimeMillis());
                    for (final DownloadLink link : links) {
                        if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                            fp.add(link);
                            if (!fps.contains(fp)) {
                                fps.add(fp);
                            }
                        } else {
                            if (!fps.contains(link.getFilePackage())) {
                                fps.add(link.getFilePackage());
                            }
                        }
                    }
                    LinkCheck.getLinkChecker().checkLinksandWait(links);
                    if (JsonConfig.create(GeneralSettings.class).isAddNewLinksOnTop()) {

                        DownloadController.getInstance().addAllAt(fps, 0);
                    } else {
                        DownloadController.getInstance().addAll(fps);
                    }
                    if (autostart) {
                        DownloadWatchDog.getInstance().startDownloads();
                    }
                }
            }.start();
        } else {
            LinkGrabberPanel.getLinkGrabber().addLinks(links);
            this.requestPanel(UserIF.Panels.LINKGRABBER, null);
        }
    }

    @Override
    public void closeWindow() {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.sys_ask_rlyclose()), UserIO.RETURN_OK)) {
            JDUtilities.getController().exit();
        }
    }

    public void controlEvent(final ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            JDLogger.getLogger().info("Init complete");
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    JDGui.this.mainFrame.setEnabled(true);
                    return null;
                }
            }.start();
            if (JsonConfig.create(GeneralSettings.class).isAutoStartDownloadsOnStartupEnabled()) {
                /* autostart downloads when no autoupdate is enabled */
                JDController.getInstance().autostartDownloadsonStartup();
            }
            break;
        case ControlEvent.CONTROL_SYSTEM_EXIT:
            this.exitRequested = true;
            final String id = JDController.requestDelayExit("JDGUI");

            new EDTHelper<Object>() {

                @Override
                public Object edtRun() {
                    JDGui.this.mainTabbedPane.onClose();
                    GUIUtils.saveLastLocation(JDGui.this.getMainFrame());
                    GUIUtils.saveLastDimension(JDGui.this.getMainFrame());

                    JDController.releaseDelayExit(id);
                    JDGui.this.getMainFrame().setVisible(false);
                    JDGui.this.getMainFrame().dispose();
                    return null;

                }
            }.getReturnValue();
            break;
        case ControlEvent.CONTROL_DOWNLOAD_START:
            Balloon.showIfHidden(_GUI._.ballon_download_title(), NewTheme.I().getIcon("go-next", 32), _GUI._.ballon_download_finished_started());
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            JDLogger.getLogger().info("All downloads finished");
            Balloon.showIfHidden(_GUI._.ballon_download_title(), NewTheme.I().getIcon("media-playback-stop", 32), _GUI._.ballon_download_finished_stopped());
            break;
        }
    }

    /**
     * TODO: mal durch ein einheitliches notification system ersetzen, welches
     * an das eventsystem gekoppelt ist
     */
    @Override
    public void displayMiniWarning(final String shortWarn, final String longWarn) {
        Balloon.show(shortWarn, NewTheme.I().getIcon("warning", 32), longWarn);
    }

    @Override
    public void disposeView(View view) {
        view = this.mainTabbedPane.getComponentEquals(view);
        this.mainTabbedPane.remove(view);
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

    private void initComponents() {
        this.menuBar = new JDMenuBar();
        this.statusBar = new StatusBar();
        this.waitingPane = new JPanel();
        this.waitingPane.setOpaque(false);
        this.mainTabbedPane = MainTabbedPane.getInstance();
        this.toolBar = MainToolBar.getInstance();
        this.toolBar.registerAccelerators(this);
        this.downloadView = DownloadView.getInstance();
        this.linkgrabberView = LinkgrabberView.getInstance();

        this.mainTabbedPane.addTab(this.downloadView);
        this.mainTabbedPane.addTab(new DownloadsView());
        this.mainTabbedPane.addTab(this.linkgrabberView);

        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isConfigViewVisible()) {
            this.mainTabbedPane.addTab(ConfigurationView.getInstance());
        }
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isLogViewVisible()) {
            this.mainTabbedPane.addTab(LogView.getInstance());
        }
        this.mainTabbedPane.setSelectedComponent(this.downloadView);
        // TODO
        // this.toolBar.setList(GUIUtils.getConfig().getGenericProperty("TOOLBAR",
        // ToolBar.DEFAULT_LIST).toArray(new String[] {}));
        this.toolBar.setList(ToolBar.DEFAULT_LIST.toArray(new String[] {}));
        if (OSDetector.isMac()) {
            // add handling for Command+W for closing window on Mac OS
            KeyStroke closeKey = KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            this.mainTabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(closeKey, "closeWindow");
            this.mainTabbedPane.getActionMap().put("closeWindow", new AbstractAction() {
                private static final long serialVersionUID = 9149139888018750308L;

                public void actionPerformed(ActionEvent e) {
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            JDGui.this.getMainFrame().setVisible(false);
                            return null;
                        }
                    }.start();
                }
            });
        }
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
     * restores the dimension and location to the window
     */
    private void initLocationAndDimension() {
        Dimension dim = GUIUtils.getLastDimension(this.mainFrame);
        if (dim == null) {
            dim = new Dimension(800, 600);
        }
        this.mainFrame.setPreferredSize(dim);
        this.mainFrame.setSize(dim);
        this.mainFrame.setMinimumSize(new Dimension(400, 100));
        Point loc = GUIUtils.getLastLocation(null, this.mainFrame);
        this.mainFrame.setLocation(loc);
        // try to find offscreen
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();
        boolean isok = false;
        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            int xMin, xMax, yMin, yMax;
            if (CrossSystem.isWindows()) {
                xMin = Math.max(bounds.x, loc.x);
                xMax = Math.min(bounds.x + bounds.width, loc.x + dim.width);
                yMin = Math.max(bounds.y, loc.y);
                yMax = Math.min(bounds.y + bounds.height, loc.y + 30);
            } else if (CrossSystem.isLinux()) {
                xMin = Math.max(bounds.x, loc.x);
                xMax = Math.min(bounds.x + bounds.width, loc.x + dim.width);
                yMin = Math.max(bounds.y, loc.y);
                yMax = Math.min(bounds.y + bounds.height, loc.y + 30);

            } else {
                xMin = Math.max(bounds.x, loc.x);
                xMax = Math.min(bounds.x + bounds.width, loc.x + dim.width);
                yMin = Math.max(bounds.y + 30, loc.y);
                yMax = Math.min(bounds.y - 30 + bounds.height, loc.y + 30);
            }
            int intersectionWidth = xMax - xMin;
            int intersectionHeight = yMax - yMin;
            if (intersectionWidth > 50 && intersectionHeight >= 30) {
                // ok. Titlebar is in screen.
                isok = true;
                break;

            }

        }
        if (!isok) {
            this.mainFrame.setPreferredSize(new Dimension(800, 600));
            this.mainFrame.setSize(new Dimension(800, 600));
            loc = Screen.getCenterOfComponent(null, mainFrame);
            this.mainFrame.setLocation(loc);
        }

        if (GUIUtils.STORAGE.get("extendedstate." + mainFrame.getName(), JFrame.NORMAL) == Frame.ICONIFIED) {
            this.mainFrame.setExtendedState(Frame.NORMAL);
        } else {
            this.mainFrame.setExtendedState(GUIUtils.STORAGE.get("extendedstate." + mainFrame.getName(), JFrame.NORMAL));
        }

        if (this.mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) this.mainFrame.getRootPane().getUI()).setMaximizedBounds(this.mainFrame);
        }

    }

    protected void internalSetWaiting(final boolean b) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                JDGui.this.getMainFrame().setGlassPane(JDGui.this.waitingPane);
                JDGui.this.waitingPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                JDGui.this.getMainFrame().getGlassPane().setVisible(b);
                return null;
            }
        }.waitForEDT();
    }

    /**
     * returns true, if the user requested the app to close
     * 
     * @return
     */
    public boolean isExitRequested() {
        return this.exitRequested;
    }

    private void layoutComponents() {
        final JPanel contentPane = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]0[shrink]"));
        contentPane.add(this.mainTabbedPane);
        contentPane.add(this.statusBar, "dock SOUTH");

        this.mainFrame.setContentPane(contentPane);
        this.mainFrame.setJMenuBar(this.menuBar);
        this.mainFrame.add(this.toolBar, "dock NORTH");
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
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
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
     * Adds view to the main tabbedpane if setActive is true, the enw panel will
     * be selected
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
        switch (id) {
        case UIConstants.WINDOW_STATUS_MAXIMIZED:
            this.mainFrame.setState(Frame.MAXIMIZED_BOTH);
            break;
        case UIConstants.WINDOW_STATUS_MINIMIZED:
            this.mainFrame.setState(Frame.ICONIFIED);
            break;
        case UIConstants.WINDOW_STATUS_NORMAL:
            this.mainFrame.setState(Frame.NORMAL);
            this.mainFrame.setVisible(true);
            break;
        case UIConstants.WINDOW_STATUS_FOREGROUND:
            this.mainFrame.setState(Frame.NORMAL);
            this.mainFrame.setFocusableWindowState(false);
            this.mainFrame.setVisible(true);
            this.mainFrame.toFront();
            this.mainFrame.setFocusableWindowState(true);
            break;
        }
    }

    @Override
    public void setWaiting(final boolean b) {
        this.internalSetWaiting(b);
    }

    /**
     * Sets the Windows Icons. lot's of lafs have problems resizing the icon. so
     * we set different sizes. for 1.5 it is only possible to use
     * {@link JFrame#setIconImage(Image)}
     */
    private void setWindowIcon() {
        if (Application.getJavaVersion() >= 16000000) {
            final ArrayList<Image> list = new ArrayList<Image>();
            list.add(JDImage.getImage("logo/logo_14_14"));
            list.add(JDImage.getImage("logo/logo_15_15"));
            list.add(JDImage.getImage("logo/logo_16_16"));
            list.add(JDImage.getImage("logo/logo_17_17"));
            list.add(JDImage.getImage("logo/logo_18_18"));
            list.add(JDImage.getImage("logo/logo_19_19"));
            list.add(JDImage.getImage("logo/logo_20_20"));
            list.add(JDImage.getImage("logo/jd_logo_64_64"));
            this.mainFrame.setIconImages(list);
        } else {
            this.mainFrame.setIconImage(JDImage.getImage("logo/logo_17_17"));
        }
    }

    public void setWindowTitle(final String msg) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                JDGui.this.mainFrame.setTitle(msg);
                return null;
            }
        }.start();
    }

    /**
     * Converts a {@link ConfigContainer} to a {@link AddonConfig} and displays
     * it
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

        final SwitchPanel oldPanel = this.mainTabbedPane.getSelectedView().getInfoPanel();

        final AddonConfig addonConfig = AddonConfig.getInstance(container, "_2", false);

        final JDCollapser col = new JDCollapser() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClosed() {
                /*
                 * Show the old info panel if it isn't a closable JDCollapser
                 * (e.g. used for config panels)
                 */
                if (oldPanel != null && oldPanel instanceof JDCollapser) {
                    JDGui.this.mainTabbedPane.getSelectedView().setInfoPanel(null);
                } else {
                    JDGui.this.mainTabbedPane.getSelectedView().setInfoPanel(oldPanel);
                }
                addonConfig.onHide();
            }

            @Override
            protected void onHide() {
            }

            @Override
            protected void onShow() {
            }

        };

        final JScrollPane scrollPane = new JScrollPane(addonConfig.getPanel());
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        col.getContent().add(scrollPane, "h min(300, pref)!");
        col.setInfos(name, icon);

        this.mainTabbedPane.getSelectedView().setInfoPanel(col);
    }

    @Override
    public void windowClosing(final WindowEvent e) {
        if (e.getComponent() == this.getMainFrame()) {
            /* dont close/exit if trayicon minimizing is enabled */
            // final OptionalPluginWrapper addon =
            // JDUtilities.getOptionalPlugin("trayicon");
            // if (addon != null && addon.isEnabled() &&
            // addon.getPlugin().isRunning()) {
            // if ((Boolean) addon.getPlugin().interact("closetotray", null) ==
            // true) {
            // UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN
            // | UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION,
            // JDL.L("sys.warning.noclose",
            // "JDownloader will be minimized to tray!"));
            // return;
            // }
            // }
            /*
             * without trayicon also dont close/exit for macos
             */
            if (OSDetector.isMac()) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        /* set visible state */
                        JDGui.this.getMainFrame().setVisible(false);
                        return null;
                    }
                }.start();
                return;
            }
            this.closeWindow();
        }
    }

    public static void help(final String title, final String msg, final ImageIcon icon) {

        Timer timer = new Timer(200, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    SimpleTextBallon d = new SimpleTextBallon(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, icon) {

                        @Override
                        protected String getDontShowAgainKey() {
                            return title;
                        }

                    };

                    Dialog.getInstance().showDialog(d);
                } catch (OffScreenException e1) {
                    e1.printStackTrace();
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });
        timer.setRepeats(false);
        timer.start();

    }

}