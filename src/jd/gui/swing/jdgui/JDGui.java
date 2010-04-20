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
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Configuration;
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
import jd.gui.swing.jdgui.components.JDStatusBar;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.AboutMenu;
import jd.gui.swing.jdgui.menu.AddLinksMenu;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.menu.CleanupMenu;
import jd.gui.swing.jdgui.menu.FileMenu;
import jd.gui.swing.jdgui.menu.JStartMenu;
import jd.gui.swing.jdgui.menu.PremiumMenu;
import jd.gui.swing.jdgui.views.downloads.DownloadView;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.linkgrabber.LinkgrabberView;
import jd.gui.swing.jdgui.views.log.LogView;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons;
import jd.gui.swing.jdgui.views.settings.panels.premium.Premium;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class JDGui extends SwingGui implements LinkGrabberDistributeEvent {

    private static final long serialVersionUID = 1048792964102830601L;
    private static JDGui INSTANCE;
    private JMenuBar menuBar;
    private JDStatusBar statusBar;

    private MainTabbedPane mainTabbedPane;
    private DownloadView downloadView;
    private LinkgrabberView linkgrabberView;
    // private ConfigurationView configurationView;

    private MainToolBar toolBar;
    private JPanel waitingPane;
    private boolean exitRequested = false;

    private JDGui() {
        super("");
        // disable Clipboard while gui is loading
        ClipboardHandler.getClipboard().setTempDisabled(true);
        // Important for unittests
        mainFrame.setName("MAINFRAME");

        initDefaults();
        initComponents();

        setWindowIcon();
        setWindowTitle(JDUtilities.getJDTitle());
        layoutComponents();

        mainFrame.pack();

        initLocationAndDimension();
        mainFrame.setVisible(true);
        if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
        }
        ClipboardHandler.getClipboard().setTempDisabled(false);
        LinkGrabberController.getInstance().setDistributer(this);
    }

    @Override
    public void displayMiniWarning(String shortWarn, String longWarn) {
        /*
         * TODO: mal durch ein einheitliches notification system ersetzen,
         * welches an das eventsystem gekoppelt ist
         */
        Balloon.show(shortWarn, JDTheme.II("gui.images.warning", 32, 32), longWarn);
    }

    /**
     * restores the dimension and location to the window
     */
    private void initLocationAndDimension() {
        Dimension dim = GUIUtils.getLastDimension(mainFrame);
        if (dim == null) dim = new Dimension(800, 600);
        mainFrame.setPreferredSize(dim);
        mainFrame.setSize(dim);
        mainFrame.setMinimumSize(new Dimension(400, 100));
        mainFrame.setLocation(GUIUtils.getLastLocation(null, mainFrame));
        mainFrame.setExtendedState(GUIUtils.getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + mainFrame.getName(), JFrame.NORMAL));

        if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
        }

    }

    private void initComponents() {
        menuBar = createMenuBar();
        statusBar = new JDStatusBar();
        waitingPane = new JPanel();
        waitingPane.setOpaque(false);
        mainTabbedPane = MainTabbedPane.getInstance();
        toolBar = MainToolBar.getInstance();
        toolBar.registerAccelerators(this);
        downloadView = DownloadView.getInstance();
        linkgrabberView = LinkgrabberView.getInstance();

        mainTabbedPane.addTab(downloadView);
        mainTabbedPane.addTab(linkgrabberView);

        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_CONFIG_SHOWN, true)) mainTabbedPane.addTab(ConfigurationView.getInstance());
        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_LOGVIEW_SHOWN, false)) mainTabbedPane.addTab(LogView.getInstance());
        mainTabbedPane.setSelectedComponent(downloadView);
        toolBar.setList(GUIUtils.getConfig().getGenericProperty("TOOLBAR", ToolBar.DEFAULT_LIST).toArray(new String[] {}));
    }

    private void layoutComponents() {
        JPanel contentPane;
        mainFrame.setContentPane(contentPane = new JPanel());
        MigLayout mainLayout = new MigLayout("ins 0 0 0 0,wrap 1", "[grow,fill]", "[grow,fill]0[shrink]");
        contentPane.setLayout(mainLayout);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(toolBar, "dock NORTH");

        contentPane.add(mainTabbedPane);
        contentPane.add(statusBar, "dock SOUTH");

    }

    private void initDefaults() {
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(this);

        ToolTipManager.sharedInstance().setReshowDelay(0);
    }

    public void setWindowTitle(final String msg) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                mainFrame.setTitle(msg);
                return null;
            }
        }.start();
    }

    /**
     * Sets the Windows Icons. lot's of lafs have problems resizing the icon. so
     * we set different sizes. for 1.5 it is only possible to use
     * {@link JFrame#setIconImage(Image)}
     */
    private void setWindowIcon() {
        if (JDUtilities.getJavaVersion() >= 1.6) {
            ArrayList<Image> list = new ArrayList<Image>();
            list.add(JDImage.getImage("logo/logo_14_14"));
            list.add(JDImage.getImage("logo/logo_15_15"));
            list.add(JDImage.getImage("logo/logo_16_16"));
            list.add(JDImage.getImage("logo/logo_17_17"));
            list.add(JDImage.getImage("logo/logo_18_18"));
            list.add(JDImage.getImage("logo/logo_19_19"));
            list.add(JDImage.getImage("logo/logo_20_20"));
            list.add(JDImage.getImage("logo/jd_logo_64_64"));
            mainFrame.setIconImages(list);
        } else {
            mainFrame.setIconImage(JDImage.getImage("logo/logo_17_17"));
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar ret = new JMenuBar();

        JMenu edit = new JMenu(JDL.L("jd.gui.skins.simple.simplegui.menubar.linksmenu", "Links"));

        edit.add(new AddLinksMenu());
        edit.add(new CleanupMenu());

        ret.add(new FileMenu());
        ret.add(edit);
        JStartMenu m;
        ret.add(m = PremiumMenu.getInstance());
        m.setIcon(null);
        ret.add(m = AddonsMenu.getInstance());
        m.setIcon(null);
        ret.add(m = new AboutMenu());
        m.setIcon(null);

        return ret;
    }

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits
     * existierende zurück
     * 
     * @return
     */
    public static JDGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GuiRunnable<JDGui>() {
                @Override
                public JDGui runSave() {
                    return new JDGui();
                }

            }.getReturnValue();
        }
        return INSTANCE;
    }

    @Override
    public void setFrameStatus(int id) {
        switch (id) {
        case UIConstants.WINDOW_STATUS_MAXIMIZED:
            mainFrame.setState(JFrame.MAXIMIZED_BOTH);
            break;
        case UIConstants.WINDOW_STATUS_MINIMIZED:
            mainFrame.setState(JFrame.ICONIFIED);
            break;
        case UIConstants.WINDOW_STATUS_NORMAL:
            mainFrame.setState(JFrame.NORMAL);
            mainFrame.setVisible(true);
            break;
        case UIConstants.WINDOW_STATUS_FOREGROUND:
            mainFrame.setState(JFrame.NORMAL);
            mainFrame.setFocusableWindowState(false);
            mainFrame.setVisible(true);
            mainFrame.toFront();
            mainFrame.setFocusableWindowState(true);
            break;
        }
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            JDLogger.getLogger().info("Init complete");
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    mainFrame.setEnabled(true);
                    return null;
                }
            }.start();
            if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false) && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                /* autostart downloads when no autoupdate is enabled */
                JDController.getInstance().autostartDownloadsonStartup();
            }
            break;
        case ControlEvent.CONTROL_SYSTEM_EXIT:
            this.exitRequested = true;
            final String id = JDController.requestDelayExit("JDGUI");
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    mainTabbedPane.onClose();
                    GUIUtils.saveLastLocation(getMainFrame());
                    GUIUtils.saveLastDimension(getMainFrame());
                    GUIUtils.getConfig().save();
                    JDController.releaseDelayExit(id);
                    getMainFrame().setVisible(false);
                    getMainFrame().dispose();
                    return null;
                }
            }.start();

            break;

        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            for (DownloadLink link : DownloadController.getInstance().getAllDownloadLinks()) {
                if (link.getLinkStatus().hasStatus(LinkStatus.TODO)) {
                    JDLogger.getLogger().info("Downloads stopped");
                    return;
                }
            }
            JDLogger.getLogger().info("All downloads finished");
            break;
        case ControlEvent.CONTROL_DOWNLOAD_START:
            Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.started", "Download started"));
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.stop", 32, 32), JDL.L("ballon.download.finished.stopped", "Download stopped"));
            break;
        }
    }

    /**
     * returns true, if the user requested the app to close
     * 
     * @return
     */
    public boolean isExitRequested() {
        return exitRequested;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == getMainFrame()) {
            /* dont close/exit if trayicon minimizing is enabled */
            OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("trayicon");
            if (addon != null && addon.isEnabled() && addon.getPlugin().isRunning()) {
                if ((Boolean) addon.getPlugin().interact("closetotray", null) == true) {
                    UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L("sys.warning.noclose", "JDownloader will be minimized to tray!"));
                    return;
                }
            }
            /*
             * without trayicon also dont close/exit for macos
             */
            if (OSDetector.isMac()) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        /* set visible state */
                        getMainFrame().setVisible(false);
                        return null;
                    }
                }.start();
                return;
            }
            closeWindow();
        }
    }

    @Override
    public void closeWindow() {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("sys.ask.rlyclose", "Are you sure that you want to exit JDownloader?")), UserIO.RETURN_OK)) {
            JDUtilities.getController().exit();
        }
    }

    @Override
    public void setWaiting(final boolean b) {
        internalSetWaiting(b);
    }

    protected void internalSetWaiting(final boolean b) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                getMainFrame().setGlassPane(waitingPane);
                waitingPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getMainFrame().getGlassPane().setVisible(b);
                return null;
            }
        }.waitForEDT();
    }

    @Override
    public void setContent(View view) {
        if (!mainTabbedPane.contains(view)) {
            mainTabbedPane.addTab(view);
        }
        mainTabbedPane.setSelectedComponent(view);
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

    @Override
    public void requestPanel(final Panels panel, final Object param) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                switch (panel) {
                case DOWNLOADLIST:
                    mainTabbedPane.setSelectedComponent(downloadView);
                    break;
                case LINKGRABBER:
                    mainTabbedPane.setSelectedComponent(linkgrabberView);
                    break;
                case PREMIUMCONFIG:
                    ConfigurationView.getInstance().getSidebar().setSelectedTreeEntry(Premium.class);
                    openSettings();
                    if (param != null && param instanceof Account) {
                        Premium p = (Premium) ConfigurationView.getInstance().getContent();
                        p.setSelectedAccount((Account) param);
                    }
                    break;
                case ADDON_MANAGER:
                    ConfigurationView.getInstance().getSidebar().setSelectedTreeEntry(ConfigPanelAddons.class);
                    openSettings();
                    break;
                case CONFIGPANEL:
                    if (param instanceof ConfigContainer) {
                        if (((ConfigContainer) param).getEntries().size() == 0) return null;
                        showConfigPanel((ConfigContainer) param);
                    }
                    break;
                default:
                    mainTabbedPane.setSelectedComponent(downloadView);
                }
                return null;
            }
        }.start();
    }

    private void openSettings() {
        ConfigurationView config = ConfigurationView.getInstance();
        if (!mainTabbedPane.contains(config)) mainTabbedPane.addTab(config);
        mainTabbedPane.setSelectedComponent(config);
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
        }
        if (name == null && container.getGroup() != null && container.getGroup().getName() != null) name = container.getGroup().getName();

        ImageIcon icon = null;
        if (container.getIcon() != null) {
            icon = container.getIcon();
        } else if (container.getGroup() != null && container.getGroup().getIcon() != null) {
            icon = container.getGroup().getIcon();
        }

        final SwitchPanel oldPanel = mainTabbedPane.getSelectedView().getInfoPanel();
        AddonConfig p = AddonConfig.getInstance(container, name, "_2");
        JDCollapser col = new JDCollapser() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClosed() {
                // Show the old info panel if it isn't a closable JDCollapser
                // (e.g. used for config panels)
                if (oldPanel != null && oldPanel instanceof JDCollapser) {
                    mainTabbedPane.getSelectedView().setInfoPanel(null);
                } else {
                    mainTabbedPane.getSelectedView().setInfoPanel(oldPanel);
                }
            }

            @Override
            protected void onHide() {
            }

            @Override
            protected void onShow() {
            }

        };
        col.getContent().add(p.getPanel());
        col.setInfos(name, icon);

        this.mainTabbedPane.getSelectedView().setInfoPanel(col);
    }

    @Override
    public void disposeView(View view) {
        view = mainTabbedPane.getComponentEquals(view);
        mainTabbedPane.remove(view);
    }

    public void addLinks(final ArrayList<DownloadLink> links, boolean hidegrabber, final boolean autostart) {
        if (links.size() == 0) return;
        if (hidegrabber || autostart) {
            new Thread() {
                @Override
                public void run() {
                    /* TODO: hier autopackaging ? */
                    ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName("Added " + System.currentTimeMillis());
                    for (DownloadLink link : links) {
                        if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                            fp.add(link);
                            if (!fps.contains(fp)) fps.add(fp);
                        } else {
                            if (!fps.contains(link.getFilePackage())) fps.add(link.getFilePackage());
                        }
                    }
                    LinkCheck.getLinkChecker().checkLinksandWait(links);
                    if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                        DownloadController.getInstance().addAllAt(fps, 0);
                    } else {
                        DownloadController.getInstance().addAll(fps);
                    }
                    if (autostart) DownloadWatchDog.getInstance().startDownloads();
                }
            }.start();
        } else {
            LinkGrabberPanel.getLinkGrabber().addLinks(links);
            requestPanel(UserIF.Panels.LINKGRABBER, null);
        }
    }

}
