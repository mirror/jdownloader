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

package jd.gui.skins.jdgui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

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
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.components.Balloon;
import jd.gui.skins.jdgui.components.JDStatusBar;
import jd.gui.skins.jdgui.components.toolbar.MainToolBar;
import jd.gui.skins.jdgui.events.EDTEventQueue;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.jdgui.interfaces.View;
import jd.gui.skins.jdgui.menu.AboutMenu;
import jd.gui.skins.jdgui.menu.AddLinksMenu;
import jd.gui.skins.jdgui.menu.AddonsMenu;
import jd.gui.skins.jdgui.menu.CleanupMenu;
import jd.gui.skins.jdgui.menu.JStartMenu;
import jd.gui.skins.jdgui.menu.PremiumMenu;
import jd.gui.skins.jdgui.menu.SaveMenu;
import jd.gui.skins.jdgui.menu.actions.ExitAction;
import jd.gui.skins.jdgui.menu.actions.RestartAction;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.gui.skins.jdgui.views.ConfigurationView;
import jd.gui.skins.jdgui.views.DownloadView;
import jd.gui.skins.jdgui.views.LinkgrabberView;
import jd.gui.skins.jdgui.views.LogView;
import jd.gui.skins.jdgui.views.TabbedPanelView;
import jd.gui.skins.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
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

    private JDGui() {
        super("");
        // disable Clipboard while gui is loading
        ClipboardHandler.getClipboard().setTempDisabled(true);
        // Important for unittests
        setName("MAINFRAME");
        // GUIUtils.getConfig() =
        // SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER);

        initDefaults();
        initComponents();

        setWindowIcon();
        setWindowTitle();
        layoutComponents();

        pack();
        initLocationAndDimension();
        setVisible(true);
        ClipboardHandler.getClipboard().setTempDisabled(false);
        LinkGrabberController.getInstance().setDistributer(this);
    }

    /**
     * restores the dimension and location to the window
     */
    private void initLocationAndDimension() {
        Dimension dim = GUIUtils.getLastDimension(this, null);
        if (dim == null) dim = new Dimension(800, 600);
        setPreferredSize(dim);
        this.setSize(dim);
        setMinimumSize(new Dimension(400, 100));
        setLocation(GUIUtils.getLastLocation(null, null, this));
        setExtendedState(GUIUtils.getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + this.getName(), JFrame.NORMAL));
    }

    private void initComponents() {
        this.menuBar = createMenuBar();
        statusBar = new JDStatusBar();

        mainTabbedPane = new MainTabbedPane();

        multiProgressBar = new TabProgress();
        this.toolBar = MainToolBar.getInstance();
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
        // mainTabbedPane.addTab(new TestView());
        mainTabbedPane.setSelectedComponent(downloadView);
    }

    private void layoutComponents() {
        JPanel contentPane;
        this.setContentPane(contentPane = new JPanel());
        MigLayout mainLayout = new MigLayout("ins 0 0 0 0,wrap 1", "[grow,fill]", "[grow,fill]0[shrink]");
        contentPane.setLayout(mainLayout);
        this.setJMenuBar(menuBar);
        add(toolBar, "dock NORTH");
        contentPane.add(mainTabbedPane);

        contentPane.add(multiProgressBar, "hidemode 3");
        contentPane.add(statusBar, "dock SOUTH");

    }

    private void initDefaults() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());
        setDefaultCloseOperation(SwingGui.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
    }

    /**
     * Sets the windowtitle depüending on the used branch
     */
    private void setWindowTitle() {
        String branch = WebUpdater.getConfig("WEBUPDATE").getStringProperty("BRANCHINUSE", null);
        if (branch != null) {
            setTitle("JDownloader -" + branch + "-");
        } else {
            setTitle("JDownloader");
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
            this.setIconImages(list);
        } else {
            this.setIconImage(list.get(3));
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
        ret.add(m = new PremiumMenu());
        m.setIcon(null);
        ret.add(m = new AddonsMenu());
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
            setState(JFrame.MAXIMIZED_BOTH);
            break;
        case UIConstants.WINDOW_STATUS_MINIMIZED:
            setState(JFrame.ICONIFIED);
            break;
        case UIConstants.WINDOW_STATUS_NORMAL:
            setState(JFrame.NORMAL);
            setVisible(true);
            break;
        case UIConstants.WINDOW_STATUS_FOREGROUND:
            setState(JFrame.NORMAL);
            setFocusableWindowState(false);
            setVisible(true);
            toFront();
            setFocusableWindowState(true);
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
                    setEnabled(true);
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
            JDController.requestDelayExit();
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    mainTabbedPane.onClose();
                    GUIUtils.saveLastLocation(JDGui.this, null);
                    GUIUtils.saveLastDimension(JDGui.this, null);
                    GUIUtils.getConfig().save();
                    JDController.releaseDelayExit();
                    setVisible(false);
                    dispose();
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

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == this) closeWindow();
    }

    public void closeWindow() {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN, JDL.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?")), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
            JDUtilities.getController().exit();
        }
    }

    @Override
    public void setWaiting(boolean b) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setContent(SwitchPanel tabbedPanel) {
        TabbedPanelView view = new TabbedPanelView(tabbedPanel);
        if (!mainTabbedPane.contains(view)) {
            mainTabbedPane.addTab(view);
        }
        mainTabbedPane.setSelectedComponent(view);
    }

    public MainTabbedPane getMainTabbedPane() {
        return this.mainTabbedPane;
    }

    public void requestPanel(final byte panelID) {

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                switch (panelID) {
                case UIConstants.PANEL_ID_DOWNLOADLIST:
                    mainTabbedPane.setSelectedComponent(downloadView);
                    break;
                case UIConstants.PANEL_ID_LINKGRABBER:
                    mainTabbedPane.setSelectedComponent(linkgrabberView);
                    break;
                default:
                    mainTabbedPane.setSelectedComponent(downloadView);
                }

                // TODO Auto-generated method stub
                return null;
            }

        }.waitForEDT();
    }

    @Override
    public void disposeView(SwitchPanel view) {
        if (view instanceof View) {
            view = mainTabbedPane.getComponentEquals((View) view);
            mainTabbedPane.remove(view);
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
            requestPanel(UIConstants.PANEL_ID_LINKGRABBER);
        }
    }

}
