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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import jd.config.ConfigContainer;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberDistributeEvent;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
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
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.menu.AboutMenu;
import jd.gui.swing.jdgui.menu.AddLinksMenu;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.menu.CleanupMenu;
import jd.gui.swing.jdgui.menu.JStartMenu;
import jd.gui.swing.jdgui.menu.PremiumMenu;
import jd.gui.swing.jdgui.menu.SaveMenu;
import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons;
import jd.gui.swing.jdgui.settings.panels.premium.Premium;
import jd.gui.swing.jdgui.views.ConfigurationView;
import jd.gui.swing.jdgui.views.DownloadView;
import jd.gui.swing.jdgui.views.LinkgrabberView;
import jd.gui.swing.jdgui.views.TabbedPanelView;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.logview.LogView;
import jd.gui.swing.jdgui.views.sidebars.configuration.AddonConfig;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.update.WebUpdater;
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
    private TabProgress multiProgressBar;
    private DownloadView downloadView;
    private LinkgrabberView linkgrabberView;
    private ConfigurationView configurationView;

    private LogView logView;
    private MainToolBar toolBar;
    private JPanel waitingPane;
    private boolean exitRequested = false;

    private JDGui() {
        super("");
        // disable Clipboard while gui is loading
        ClipboardHandler.getClipboard().setTempDisabled(true);
        // Important for unittests
        mainFrame.setName("MAINFRAME");
        // GUIUtils.getConfig() =
        // SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER);

        initDefaults();
        initComponents();

        setWindowIcon();
        setWindowTitle();
        layoutComponents();

        mainFrame.pack();
        initLocationAndDimension();
        mainFrame.setVisible(true);
        if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
        }
        mainFrame.setExtendedState(GUIUtils.getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + mainFrame.getName(), JFrame.NORMAL));
        ClipboardHandler.getClipboard().setTempDisabled(false);
        LinkGrabberController.getInstance().setDistributer(this);
    }

    @Override
    public void displayMiniWarning(String shortWarn, String longWarn) {
        /*
         * TODO: mal durch ein einheitliches notificationo system ersetzen,
         * welches an das eventsystem gekoppelt ist
         */
        Balloon.show(shortWarn, JDTheme.II("gui.images.warning", 32, 32), longWarn);
    }

    /**
     * restores the dimension and location to the window
     */
    private void initLocationAndDimension() {
        Dimension dim = GUIUtils.getLastDimension(mainFrame, null);
        if (dim == null) dim = new Dimension(800, 600);
        mainFrame.setPreferredSize(dim);
        mainFrame.setSize(dim);
        mainFrame.setMinimumSize(new Dimension(400, 100));
        mainFrame.setLocation(GUIUtils.getLastLocation(null, null, mainFrame));
        mainFrame.setExtendedState(GUIUtils.getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + mainFrame.getName(), JFrame.NORMAL));

        if (mainFrame.getRootPane().getUI().toString().contains("SyntheticaRootPaneUI")) {
            ((de.javasoft.plaf.synthetica.SyntheticaRootPaneUI) mainFrame.getRootPane().getUI()).setMaximizedBounds(mainFrame);
        }

    }

    private void initComponents() {
        this.menuBar = createMenuBar();
        statusBar = new JDStatusBar();
        this.waitingPane = new JPanel();
        waitingPane.setOpaque(false);
        mainTabbedPane = MainTabbedPane.getInstance();

        multiProgressBar = new TabProgress();
        this.toolBar = MainToolBar.getInstance();
        toolBar.registerAccelerators(this);
        downloadView = new DownloadView();
        linkgrabberView = new LinkgrabberView();
        configurationView = new ConfigurationView();

        logView = new LogView();
        // mainTabbedPane.add());
        // mainTabbedPane.add(new JLabel("III2"));
        // mainTabbedPane.add(new JLabel("III3"));
        // mainTabbedPane.add(new JLabel("III4"));

        mainTabbedPane.addTab(downloadView);

        mainTabbedPane.addTab(linkgrabberView);
        mainTabbedPane.addTab(configurationView);

        mainTabbedPane.addTab(logView);
        // mainTabbedPane.addTab(new ClosableView());
        mainTabbedPane.setSelectedComponent(downloadView);

    }

    private void layoutComponents() {
        JPanel contentPane;
        mainFrame.setContentPane(contentPane = new JPanel());
        MigLayout mainLayout = new MigLayout("ins 0 0 0 0,wrap 1", "[grow,fill]", "[grow,fill]0[shrink]");
        contentPane.setLayout(mainLayout);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(toolBar, "dock NORTH");

        contentPane.add(mainTabbedPane);

        contentPane.add(multiProgressBar, "hidemode 3");
        contentPane.add(statusBar, "dock SOUTH");

    }

    private void initDefaults() {

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(this);

        ToolTipManager.sharedInstance().setReshowDelay(0);

    }

    /**
     * Sets the windowtitle depüending on the used branch
     */
    private void setWindowTitle() {
        String branch = WebUpdater.getConfig("WEBUPDATE").getStringProperty("BRANCHINUSE", null);
        if (branch != null) {
            mainFrame.setTitle("JDownloader -" + branch + "-");
        } else {
            mainFrame.setTitle("JDownloader");
        }

    }

    /**
     * Sets the Windows ICons. lot's of lafs have problems resizing the icon. so
     * we set different sizes. for 1.5 it is only possible to use
     * setIconImage(Icon icon)
     */
    private void setWindowIcon() {
        ArrayList<Image> list = new ArrayList<Image>();
        list.add(JDImage.getImage("logo/logo_14_14"));
        list.add(JDImage.getImage("logo/logo_15_15"));
        list.add(JDImage.getImage("logo/logo_16_16"));
        list.add(JDImage.getImage("logo/logo_17_17"));
        list.add(JDImage.getImage("logo/logo_18_18"));
        list.add(JDImage.getImage("logo/logo_19_19"));
        list.add(JDImage.getImage("logo/logo_20_20"));
        list.add(JDImage.getImage("logo/jd_logo_64_64"));
        if (JDUtilities.getJavaVersion() >= 1.6) {
            mainFrame.setIconImages(list);
        } else {
            mainFrame.setIconImage(list.get(3));
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar ret = new JMenuBar();

        JMenu file = new JMenu(JDL.L("jd.gui.skins.simple.simplegui.menubar.filemenu", "File"));

        file.add(new SaveMenu());
        file.addSeparator();
        file.add(new RestartAction());
        file.add(new ExitAction());

        JMenu edit = new JMenu(JDL.L("jd.gui.skins.simple.simplegui.menubar.linksmenu", "Links"));

        edit.add(new AddLinksMenu());
        edit.add(new CleanupMenu());
        ret.add(file);
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
            if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false)) {
                new Thread() {
                    public void run() {
                        this.setName("Autostart counter");
                        final ProgressController pc = new ProgressController(JDL.L("gui.autostart", "Autostart downloads in few secounds..."));
                        pc.getBroadcaster().addListener(new ProgressControllerListener() {
                            public void onProgressControllerEvent(ProgressControllerEvent event) {
                                pc.setStatusText("Autostart aborted!");
                            }
                        });
                        pc.finalize(10 * 1000l);
                        while (!pc.isFinished()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                        if (!pc.isAbort()) JDUtilities.getController().startDownloads();
                    }
                }.start();
            }
            break;

        case ControlEvent.CONTROL_SYSTEM_EXIT:
            this.exitRequested = true;
            final String id = JDController.requestDelayExit("JDGUI");
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    mainTabbedPane.onClose();
                    GUIUtils.saveLastLocation(getMainFrame(), null);
                    GUIUtils.saveLastDimension(getMainFrame(), null);
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
            Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.stopped", "Download stopped"));
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

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == getMainFrame()) closeWindow();
    }

    public void closeWindow() {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?")), UserIO.RETURN_OK)) {
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
    public void setContent(SwitchPanel tabbedPanel) {

        View view;
        if (tabbedPanel instanceof View) {
            view = (View) tabbedPanel;
        } else {
            view = new TabbedPanelView(tabbedPanel);
        }

        if (!mainTabbedPane.contains(view)) {
            mainTabbedPane.addTab(view);
        }
        mainTabbedPane.setSelectedComponent(view);
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

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
                    ((ConfigSidebar) JDGui.this.configurationView.getSidebar()).setSelectedTreeEntry(Premium.class);

                    if (param != null) {
                        Premium p = (Premium) configurationView.getContent();
                        p.setSelectedAccount((Account) param);
                    }

                    mainTabbedPane.setSelectedComponent(JDGui.this.configurationView);

                    // Premium.showAccountInformation(this, account);
                    break;

                case ADDON_MANAGER:
                    ((ConfigSidebar) JDGui.this.configurationView.getSidebar()).setSelectedTreeEntry(ConfigPanelAddons.class);

                    mainTabbedPane.setSelectedComponent(JDGui.this.configurationView);

                    // Premium.showAccountInformation(this, account);
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
                // TODO Auto-generated method stub
                return null;
            }
        }.waitForEDT();
    }

    /**
     * Converts a ConfigContainer to a Configpanel and displays it
     * 
     * @param container
     */
    protected void showConfigPanel(final ConfigContainer container) {

        this.mainTabbedPane.getSelectedView().setInfoPanel(JDCollapser.getInstance());

        String name = "";
        if (container.getTitle() != null) {
            name = container.getTitle();
        }
        JDCollapser.getInstance().setContentPanel(AddonConfig.getInstance(container, name), JDL.LF("jd.gui.swing.jdgui.JDGui.showConfigPanel.title", "Setting for %s", name), container.getGroup() == null ? null : container.getGroup().getIcon());

        // this.mainTabbedPane.getSelectedView().setContent(
        // JDCollapser.getInstance());
        this.mainTabbedPane.getSelectedView().setInfoPanel(JDCollapser.getInstance());

    }

    @Override
    public void disposeView(SwitchPanel view) {
        if (view instanceof View) {
            view = mainTabbedPane.getComponentEquals((View) view);
            mainTabbedPane.remove((View) view);
        }
    }

    public void addLinks(ArrayList<DownloadLink> links, boolean hidegrabber, boolean autostart) {
        if (links.size() == 0) return;
        if (hidegrabber || autostart) {
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
            if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                DownloadController.getInstance().addAllAt(fps, 0);
            } else {
                DownloadController.getInstance().addAll(fps);
            }
            if (autostart) JDController.getInstance().startDownloads();
        } else {
            LinkGrabberPanel.getLinkGrabber().addLinks(links);
            requestPanel(UserIF.Panels.LINKGRABBER, null);
        }
    }

}
