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

package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.config.ConfigContainer;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.ClipboardHandler;
import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.gui.UIInterface;
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.InfoPanelHandler;
import jd.gui.skins.jdgui.components.JDCollapser;
import jd.gui.skins.jdgui.components.downloadview.DownloadLinksPanel;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkAdder;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberPanel;
import jd.gui.skins.jdgui.components.linkgrabberview.LinkGrabberTableAction;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.panels.ConfigPanelAddons;
import jd.gui.skins.simple.config.panels.ConfigPanelCaptcha;
import jd.gui.skins.simple.config.panels.ConfigPanelDownload;
import jd.gui.skins.simple.config.panels.ConfigPanelEventmanager;
import jd.gui.skins.simple.config.panels.ConfigPanelGUI;
import jd.gui.skins.simple.config.panels.ConfigPanelGeneral;
import jd.gui.skins.simple.config.panels.ConfigPanelPluginForHost;
import jd.gui.skins.simple.config.panels.ConfigPanelReconnect;
import jd.gui.skins.simple.startmenu.AboutMenu;
import jd.gui.skins.simple.startmenu.AddLinksMenu;
import jd.gui.skins.simple.startmenu.AddonsMenu;
import jd.gui.skins.simple.startmenu.CleanupMenu;
import jd.gui.skins.simple.startmenu.JDStartMenu;
import jd.gui.skins.simple.startmenu.JStartMenu;
import jd.gui.skins.simple.startmenu.PremiumMenu;
import jd.gui.skins.simple.startmenu.SaveMenu;
import jd.gui.skins.simple.startmenu.actions.ExitAction;
import jd.gui.skins.simple.startmenu.actions.RestartAction;
import jd.gui.skins.simple.tasks.AddonTaskPane;
import jd.gui.skins.simple.tasks.ConfigTaskPane;
import jd.gui.skins.simple.tasks.DownloadTaskPane;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.gui.swing.laf.LookAndFeelController;
import jd.gui.userio.dialog.ContainerDialog;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.utils.LafConstants.AnimationKind;

public class SimpleGUI extends SwingGui {

    public static SimpleGUI CURRENTGUI = null;

    private static SimpleGUI INSTANCE;

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    private LinkGrabberPanel linkGrabber;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksPanel linkListPane;

    public LogPane getLogDialog() {
        return (LogPane) logPanel.getPanel();
    }

    private Logger logger = JDLogger.getLogger();

    private TabProgress progressBar;

    private TaskPane taskPane;

    private ContentPanel contentPanel;

    private DownloadTaskPane dlTskPane;

    public DownloadTaskPane getDlTskPane() {
        return dlTskPane;
    }

    private LinkGrabberTaskPane lgTaskPane;

    private JDToolBar toolBar;

    // private JDMenuBar menuBar;

    private JDStatusBar statusBar;

    // private boolean noTitlePane = false;

    private JDSeparator sep;

    private SingletonPanel logPanel;

    // private SingletonPanel addonPanel;

    private ConfigTaskPane cfgTskPane;

    private AddonTaskPane addonTaskPanel;

    // private JDSubstanceUI titleUI;

    private Image mainMenuIconRollOver;

    private Image mainMenuIcon;

    // private boolean mainMenuRollOverStatus = false;

    private JViewport taskPaneView;

    private SwingWorker<Object, Object> cursorworker;

    private JPopupMenu startMenu;

    private JLabel startbutton;

    private JMenuBar menuBar;

    /**
     * Das Hauptfenster wird erstellt. Singleton. Use SimpleGUI.createGUI
     */
    private SimpleGUI() {
        super("");

        // Avoid resize bug if decorated.. works only for windows

        SimpleGuiConstants.GUI_CONFIG = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        updateDecoration();
        LookAndFeelController.setUIManager();

        /**
         * Init panels
         */

        menuBar = createMenuBar();
        this.setJMenuBar(menuBar);
        statusBar = new JDStatusBar();
        initWaitPane();
        this.setEnabled(false);
        this.setWaiting(true);

        if (isSubstance() && SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.DECORATION_ENABLED, true)) {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 54, 54);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 54, 54);
            // noTitlePane = false;
        } else {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 32, 32);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 32, 32);
            // this.noTitlePane = true;
        }

        if (isSubstance()) this.getRootPane().setUI(new JDSubstanceUI());

        toolBar = new JDToolBar();

        // System.out.println(ui);
        addWindowListener(this);
        this.setAnimate();
        JDController.getInstance().addControlListener(new ConfigPropertyListener(SimpleGuiConstants.ANIMATION_ENABLED) {

            // @Override
            public void onPropertyChanged(Property source, String propertyName) {
                setAnimate();
            }

        });

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

        // this.setIconImage(JDImage.getImage("empty"));
        setTitle(JDUtilities.getJDTitle());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // initActions();
        // initMenuBar();
        JDL.initLocalisation();

        buildUI();

        setName("MAINFRAME");
        Dimension dim = SimpleGuiUtils.getLastDimension(this, null);
        if (dim == null) {
            dim = new Dimension(800, 600);
        }
        setPreferredSize(dim);
        setMinimumSize(new Dimension(400, 100));
        setLocation(SimpleGuiUtils.getLastLocation(null, null, this));
        pack();

        setExtendedState(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("MAXIMIZED_STATE_OF_" + this.getName(), JFrame.NORMAL));

        this.hideSideBar(SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_SIDEBAR_COLLAPSED, false));

        setVisible(true);

        // Why this?
        // Because we want to start clipboardwatcher first, when gui is finished
        // with init, not before!
        ClipboardHandler.getClipboard().setTempDisabled(false);

        new Thread("guiworker") {
            public void run() {
                while (true) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            interval();
                        }
                    });
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }.start();

        startbutton = new JLabel(new ImageIcon(mainMenuIcon));
        startbutton.setToolTipText(JDL.L("gui.menu.tooltip", "Click here to open main menu"));
        startbutton.addMouseListener(new JDMouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                startbutton.setIcon(new ImageIcon(mainMenuIconRollOver));
            }

            public void mouseExited(MouseEvent e) {
                startbutton.setIcon(new ImageIcon(mainMenuIcon));
            }

            public void mouseClicked(MouseEvent e) {
                setWaiting(true);

                startMenu = new JPopupMenu() {

                    private static final long serialVersionUID = 3510198302982639068L;

                    public void paint(Graphics g) {
                        super.paint(g);
                        setWaiting(false);
                    }

                    public void setVisible(boolean b) {
                        super.setVisible(b);
                        if (b) startMenu = null;
                    }

                };

                JDStartMenu.createMenu(startMenu);

                startMenu.show(e.getComponent(), startMenu.getLocation().x, startMenu.getLocation().y + mainMenuIcon.getHeight(null));
            }

        });

        // JPanel glass = new JPanel(new MigLayout("ins 0"));
        // if (JFrame.isDefaultLookAndFeelDecorated()) {
        // glass.add(startbutton, "gapleft 2,gaptop 25,alignx left,aligny top");
        // } else {
        // glass.add(startbutton, "gapleft 2,gaptop 2,alignx left,aligny top");
        // }
        //
        // glass.setOpaque(false);
        // this.setGlassPane(glass);
        // glass.setVisible(true);
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

    public void setWaiting(boolean b) {

        if (b) {
            this.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            this.getGlassPane().setCursor(null);
        }

        // super.setWaiting(b);
    }

    /**
     * Workaround the substance bug, that the resizecursor does not get resetted
     * if the movement is fast.
     */
    public void setCursor(Cursor c) {
        // System.out.println("set cursor " + c);
        if (this.getCursor() == c) return;
        if (isSubstance()) {
            switch (c.getType()) {
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
                final Cursor cc = c;
                if (cursorworker != null) {
                    cursorworker.cancel(true);
                    cursorworker = null;
                }
                this.cursorworker = new SwingWorker<Object, Object>() {

                    @Override
                    protected Object doInBackground() throws Exception {
                        Thread.sleep(2000);

                        return null;
                    }

                    public void done() {
                        if (cursorworker == this) {
                            if (getCursor() == cc) {
                                System.out.println("Reset cursor");
                                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            }
                            cursorworker = null;
                        }
                    }

                };
                cursorworker.execute();
            }
        }
        super.setCursor(c);

    }

    public JDToolBar getToolBar() {
        return toolBar;
    }

    private void initWaitPane() {

        // JXLabel lbl = new
        // JXLabel(JDImage.getScaledImageIcon(JDImage.getImage(
        // "logo/jd_logo_128_128"), 300, 300));
        // glass.add(lbl, "alignx center, aligny center");
        // JProgressBar prg = new JProgressBar();
        // glass.add(prg, "alignx center, aligny center,shrink");
        // prg.setStringPainted(false);
        // prg.setIndeterminate(true);
        // glass.setOpaque(false);
        // glass.setAlpha(0.5f);
        // AbstractPainter fgPainter = (AbstractPainter)
        // lbl.getForegroundPainter();
        // StackBlurFilter filter = new StackBlurFilter();
        // fgPainter.setFilters(filter);
        // glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // this.setWaitPane(glass);
    }

    public void onLAFChanged() {
        if (isSubstance()) {

            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 54, 54);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 54, 54);
            this.getRootPane().setUI(new JDSubstanceUI());

            // JDController.genew DownloadtInstance().addControlListener(new
            // ConfigPropertyListener(SimpleGuiConstants.ANIMATION_ENABLED) {
            //
            // // @Override
            // public void onPropertyChanged(Property source, String
            // propertyName) {
            //
            // }
            //
            // });
            // noTitlePane = false;
        }

    }

    private void setAnimate() {

        if (isSubstance()) {
            if (SimpleGuiConstants.isAnimated()) {
                UIManager.put(LafWidget.ANIMATION_KIND, AnimationKind.REGULAR);

            } else {
                UIManager.put(LafWidget.ANIMATION_KIND, AnimationKind.NONE);
            }

        }

    }

    public void updateDecoration() {
        // UIManager.getLookAndFeel().getName().toLowerCase().contains("substance")&&
        if (UIManager.getLookAndFeel().getSupportsWindowDecorations() && SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.DECORATION_ENABLED, true)) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        } else {
            setUndecorated(false);
            JFrame.setDefaultLookAndFeelDecorated(false);
            JDialog.setDefaultLookAndFeelDecorated(false);
        }
    }

    /**
     * Factorymethode. Erzeugt eine INstanc der Gui oder gibt eine bereits
     * existierende zurück
     * 
     * @return
     */
    public static SimpleGUI createGUI() {
        if (INSTANCE == null) INSTANCE = new SimpleGUI();
        return INSTANCE;
    }

    public synchronized void addLinksToGrabber(final ArrayList<DownloadLink> links, final boolean hideGrabber) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                logger.info("Add links to Linkgrabber: " + links.size());
                DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
                linkGrabber.addLinks(linkList);

                if (!hideGrabber) taskPane.switcher(lgTaskPane);
                return null;
            }

        }.start();

    }

    public TaskPane getTaskPane() {
        return taskPane;
    }

    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI() {
        CURRENTGUI = this;
        linkListPane = new DownloadLinksPanel();
        contentPanel = new ContentPanel();

        taskPane = new TaskPane();

        addDownloadTask();
        addLinkgrabberTask();
        addConfigTask();
        addAddonTask();

        progressBar = new TabProgress();

        contentPanel.display(linkListPane);

        taskPane.switcher(dlTskPane);

        JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]0[]0[]0[]"));

        setContentPane(panel);

        add(toolBar, "dock north");
        // panel.add(this.toolBar, "spanx");
        JPanel center = new JPanel(new MigLayout("ins 0,wrap 3", "[fill]0[shrink]0[fill,grow 100]", "[grow,fill]0[]"));

        // taskPaneView = new JViewport();
        // taskPaneView.setView(taskPane);

        center.add(taskPane, "hidemode 2,spany 2,aligny top,width 160:n:n");
        sep = new JDSeparator();

        center.add(sep, "width 6!,gapright 2,spany 2,growy, pushy,hidemode 1");
        sep.setVisible(false);
        center.add(contentPanel, "");
        // sp.setBorder(null);
        center.add(JDCollapser.getInstance(), "hidemode 3,gaptop 15,growx,pushx,growy,pushy");

        panel.add(center);
        // panel.add(generalPurposeTasks, "cell 0 2");
        // contentPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

        panel.add(progressBar, "spanx,hidemode 3");
        // panel.add(new PremiumStatus(), "spanx, cell 0 4");
        panel.add(this.statusBar, "spanx, dock south");
        // this.setStatusBar(statusBar);

    }

    private void addAddonTask() {
        addonTaskPanel = new AddonTaskPane(JDL.L("gui.taskpanes.addons", "Addons"), JDTheme.II("gui.images.taskpanes.addons", 16, 16));
        taskPane.add(addonTaskPanel);
    }

    public AddonTaskPane getAddonPanel() {
        return addonTaskPanel;
    }

    private void addConfigTask() {
        cfgTskPane = new ConfigTaskPane(JDL.L("gui.taskpanes.configuration", "Configuration"), JDTheme.II("gui.images.taskpanes.configuration", 16, 16));

        Object[] configConstructorObjects = new Object[] { JDUtilities.getConfiguration() };

        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_ADDONS, new SingletonPanel(ConfigPanelAddons.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_CAPTCHA, new SingletonPanel(ConfigPanelCaptcha.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_DOWNLOAD, new SingletonPanel(ConfigPanelDownload.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_EVENTMANAGER, new SingletonPanel(ConfigPanelEventmanager.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GENERAL, new SingletonPanel(ConfigPanelGeneral.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GUI, new SingletonPanel(ConfigPanelGUI.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_HOST, new SingletonPanel(ConfigPanelPluginForHost.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_RECONNECT, new SingletonPanel(ConfigPanelReconnect.class, configConstructorObjects));

        cfgTskPane.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {

                switch (e.getID()) {
                case DownloadTaskPane.ACTION_CLICK:

                    contentPanel.display(((TaskPanel) e.getSource()).getPanel(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_GENERAL)));

                    break;
                case ConfigTaskPane.ACTION_SAVE:
                    boolean restart = false;

                    for (SingletonPanel panel : ((ConfigTaskPane) e.getSource()).getPanels()) {

                        if (panel != null && panel.getPanel() != null && panel.getPanel() instanceof ConfigPanel) {
                            if (((ConfigPanel) panel.getPanel()).hasChanges() == PropertyType.NEEDS_RESTART) restart = true;
                            ((ConfigPanel) panel.getPanel()).save();
                        }
                    }

                    if (restart) {
                        if (JDUtilities.getGUI().showConfirmDialog(JDL.L("gui.config.save.restart", "Your changes need a restart of JDownloader to take effect.\r\nRestart now?"), JDL.L("gui.config.save.restart.title", "JDownloader restart requested"))) {
                            JDUtilities.restartJD();
                        }
                    }
                    break;

                case ConfigTaskPane.ACTION_ADDONS:
                case ConfigTaskPane.ACTION_CAPTCHA:
                case ConfigTaskPane.ACTION_DOWNLOAD:
                case ConfigTaskPane.ACTION_EVENTMANAGER:
                case ConfigTaskPane.ACTION_GENERAL:
                case ConfigTaskPane.ACTION_GUI:
                case ConfigTaskPane.ACTION_HOST:
                case ConfigTaskPane.ACTION_RECONNECT:
                    SimpleGuiConstants.GUI_CONFIG.setProperty("LAST_CONFIG_PANEL", e.getID());

                    contentPanel.display(((ConfigTaskPane) e.getSource()).getPanel(e.getID()));
                    SimpleGuiConstants.GUI_CONFIG.save();
                    break;
                }

            }
        });

        taskPane.add(cfgTskPane);

    }

    public ConfigTaskPane getCfgTskPane() {
        return cfgTskPane;
    }

    private void addLinkgrabberTask() {
        linkGrabber = LinkGrabberPanel.getLinkGrabber();
        lgTaskPane = new LinkGrabberTaskPane(JDL.L("gui.taskpanes.linkgrabber", "LinkGrabber"), JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));
        lgTaskPane.setName(JDL.L("quickhelp.linkgrabbertaskpane", "Linkgrabber Taskpane"));
        LinkAdder linkadder = new LinkAdder();

        lgTaskPane.addPanel(new SingletonPanel(linkadder));
        LinkGrabberController.getInstance().addListener(new LinkGrabberControllerListener() {
            public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
                switch (event.getID()) {
                case LinkGrabberControllerEvent.ADDED:
                    taskPane.switcher(dlTskPane);
                    break;
                case LinkGrabberControllerEvent.EMPTY:
                    // lgTaskPane.setPanelID(0);
                    break;
                }
            }

        });
        lgTaskPane.addPanel(new SingletonPanel(linkGrabber));

        lgTaskPane.addActionListener(linkGrabber);
        lgTaskPane.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switch (e.getID()) {
                case DownloadTaskPane.ACTION_CLICK:
                    if (linkGrabber.hasLinks()) {
                        lgTaskPane.setPanelID(1);
                    } else {
                        lgTaskPane.setPanelID(0);
                    }
                    break;
                case LinkGrabberTableAction.GUI_ADD:
                    lgTaskPane.setPanelID(0);
                    return;
                }
            }
        });
        taskPane.add(lgTaskPane);
    }

    public LinkGrabberTaskPane getLgTaskPane() {
        return lgTaskPane;
    }

    private void addDownloadTask() {

        dlTskPane = new DownloadTaskPane(JDL.L("gui.taskpanes.download", "Download"), JDTheme.II("gui.images.taskpanes.download", 16, 16));
        dlTskPane.setName(JDL.L("quickhelp.downloadtaskpane", "Download Taskpane"));
        // dlTskPane.add(toolBar);
        // // toolBar.setFocusable(false);
        // // toolBar.setBorderPainted(true);
        // toolBar.setOpaque(false);
        dlTskPane.addPanel(new SingletonPanel(linkListPane));
        dlTskPane.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                switch (e.getID()) {
                case DownloadTaskPane.ACTION_CLICK:
                    contentPanel.display(dlTskPane.getPanel(0));
                    break;
                }

            }

        });
        taskPane.add(dlTskPane);
    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:
                    logger.info("Init complete");

                    SimpleGUI.this.setWaiting(false);
                    SimpleGUI.this.setEnabled(true);
                    if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false)) {
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
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Module started: " + event.getSource());
                    setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.setVisible(false);
                    SimpleGUI.this.dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Module finished: " + event.getSource());
                    setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                    for (DownloadLink link : DownloadController.getInstance().getAllDownloadLinks()) {
                        if (link.getLinkStatus().hasStatus(LinkStatus.TODO)) {
                            logger.info("Downloads stopped");
                            return;
                        }
                    }
                    logger.info("All downloads finished");

                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.started", "Download started"));
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    Balloon.showIfHidden(JDL.L("ballon.download.title", "Download"), JDTheme.II("gui.images.next", 32, 32), JDL.L("ballon.download.finished.stopped", "Download stopped"));
                    break;
                }
            }
        });
    }

    public void displayMiniWarning(final String shortWarn, final String toolTip) {
        Balloon.show(shortWarn, JDTheme.II("gui.images.warning", 32, 32), toolTip);
    }

    public void doManualReconnect() {
        new GuiRunnable<Object>() {
            public Object runSave() {
                if (showConfirmDialog(JDL.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?"))) {
                    new Thread(new Runnable() {
                        public void run() {
                            Reconnecter.doManualReconnect();
                        }
                    }).start();
                }
                return null;
            }
        }.start();
    }

    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren TODO
     */
    private void interval() {
        setTitle(JDUtilities.getJDTitle());
    }

    public void setFrameStatus(int id) {
        switch (id) {
        case UIInterface.WINDOW_STATUS_MAXIMIZED:
            setState(JFrame.MAXIMIZED_BOTH);
            break;
        case UIInterface.WINDOW_STATUS_MINIMIZED:
            setState(JFrame.ICONIFIED);
            break;
        case UIInterface.WINDOW_STATUS_NORMAL:
            setState(JFrame.NORMAL);
            setVisible(true);
            break;
        case UIInterface.WINDOW_STATUS_FOREGROUND:
            setState(JFrame.NORMAL);
            setFocusableWindowState(false);
            setVisible(true);
            toFront();
            setFocusableWindowState(true);
            break;
        }

    }

    public boolean showConfirmDialog(String message) {
        return showConfirmDialog(message, JDL.L("userio.countdownconfirm", "Please confirm"));
    }

    public boolean showConfirmDialog(String string, String title) {
        int flags = UserIO.NO_COUNTDOWN;
        if (string.contains("<") && string.contains(">")) flags |= UserIO.STYLE_HTML;
        return JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(flags, title, string, null, null, null), UserIO.RETURN_OK);
    }

    public boolean showCountdownConfirmDialog(String string, int sec) {
        UserIO.setCountdownTime(sec);
        int flags = 0;
        if (string.contains("<") && string.contains(">")) flags |= UserIO.STYLE_HTML;
        try {
            return JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(flags, JDL.L("userio.countdownconfirm", "Please confirm"), string, JDTheme.II("gui.images.config.eventmanager", 32, 32), null, null), UserIO.RETURN_OK);
        } finally {
            UserIO.setCountdownTime(null);
        }
    }

    public boolean showHTMLDialog(String title, String htmlQuestion) {
        return JDFlags.hasAllFlags(UserIO.getInstance().requestHtmlDialog(UserIO.NO_COUNTDOWN, title, htmlQuestion), UserIO.RETURN_OK);
    }

    public void showMessageDialog(final String string) {
        UserIO.getInstance().requestMessageDialog(string);
    }

    public String showUserInputDialog(String string) {
        return showUserInputDialog(string, "");
    }

    public String showUserInputDialog(String string, String def) {
        return UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, JDL.L("gui.userio.input.title", "Please enter!"), string, def, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
    }

    public String showCountdownUserInputDialog(String message, String def) {
        return UserIO.getInstance().requestInputDialog(0, JDL.L("gui.userio.input.title", "Please enter!"), message, def, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
    }

    public String showTextAreaDialog(String title, String message, String def) {
        return UserIO.getInstance().requestTextAreaDialog(title, message, def);
    }

    public String[] showTwoTextFieldDialog(String title, String messageOne, String messageTwo, String defOne, String defTwo) {
        return UserIO.getInstance().requestTwoTextFieldDialog(title, messageOne, defOne, messageTwo, defTwo);
    }

    public static void displayConfig(final ConfigContainer container, final boolean toLastTab) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                ConfigEntriesPanel cep;

                JDCollapser.getInstance().setContentPanel(cep = new ConfigEntriesPanel(container));
                if (toLastTab) {
                    Component comp = cep.getComponent(0);
                    if (comp instanceof JTabbedPane) {
                        ((JTabbedPane) comp).setSelectedIndex(((JTabbedPane) comp).getTabCount() - 1);
                    }
                }
                if (container.getGroup() != null) {
                    JDCollapser.getInstance().setTitle(container.getGroup().getName());
                    // JDCollapser.getInstance().setIcon(container.getGroup().
                    // getIcon());
                } else {
                    JDCollapser.getInstance().setTitle(JDL.L("gui.panels.collapsibleconfig", "Settings"));
                    JDCollapser.getInstance().setIcon(JDTheme.II("gui.images.config.addons", 24, 24));
                }

                InfoPanelHandler.setPanel(JDCollapser.getInstance());

                return null;
            }

        }.start();
    }

    public void closeWindow() {
        if (showConfirmDialog(JDL.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"))) {
            contentPanel.getRightPanel().onHide();
            SimpleGuiUtils.saveLastLocation(this, null);
            SimpleGuiUtils.saveLastDimension(this, null);
            SimpleGuiConstants.GUI_CONFIG.save();
            JDUtilities.getController().exit();
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == this) closeWindow();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void showAccountInformation(final PluginForHost pluginForHost, final Account account) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                AccountInfo ai;
                try {
                    ai = pluginForHost.getAccountInformation(account);
                } catch (Exception e) {
                    account.setEnabled(false);
                    JDLogger.exception(e);
                    SimpleGUI.this.showMessageDialog(JDL.LF("gui.accountcheck.pluginerror", "Plugin %s may be defect. Inform support!", pluginForHost.getPluginID()));
                    return null;
                }
                if (ai == null) {
                    SimpleGUI.this.showMessageDialog(JDL.LF("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet.", pluginForHost.getHost()));
                    return null;
                }
                if (!ai.isValid()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDL.LF("plugins.host.premium.info.notValid", "The account for '%s' isn't valid! Please check username and password!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }
                if (ai.isExpired()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDL.LF("plugins.host.premium.info.expired", "The account for '%s' is expired! Please extend the account or buy a new one!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }

                String def = JDL.LF("plugins.host.premium.info.title", "Accountinformation from %s for %s", account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDL.L("plugins.host.premium.info.validUntil", "Valid until"), JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDL.L("plugins.host.premium.info.files", "Files"), JDL.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDL.L("plugins.host.premium.info.usedSpace", "Used Space"), JDL.L("plugins.host.premium.info.cash", "Cash"), JDL.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDL.L("plugins.host.premium.info.status", "Info") };

                DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String validUntil = (ai.isExpired() ? JDL.L("plugins.host.premium.info.expiredInfo", "[expired]") + " " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                if (ai.getValidUntil() == -1) validUntil = null;
                String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                String[] data = new String[] { validUntil, Formatter.formatReadable(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, Formatter.formatReadable(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", Formatter.formatReadable(ai.getTrafficShareLeft()), ai.getStatus() };

                JPanel panel = new JPanel(new MigLayout("ins 5", "[right]10[grow,fill]10[]"));
                panel.add(new JXTitledSeparator("<html><b>" + def + "</b></html>"), "spanx, pushx, growx, gapbottom 15");

                for (int j = 0; j < data.length; j++) {
                    if (data[j] != null && !data[j].equals("-1") && !data[j].equals("-1 B")) {
                        panel.add(new JLabel(label[j]), "gapleft 20");

                        JTextField tf = new JTextField(data[j]);
                        tf.setBorder(null);
                        tf.setBackground(null);
                        tf.setEditable(false);
                        tf.setOpaque(false);

                        if (label[j].equals(JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                            PieChartAPI freeTrafficChart = new PieChartAPI("", 150, 60);
                            freeTrafficChart.addEntity(new ChartAPIEntity(JDL.L("plugins.host.premium.info.freeTraffic", "Free"), ai.getTrafficLeft(), new Color(50, 200, 50)));
                            freeTrafficChart.addEntity(new ChartAPIEntity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                            freeTrafficChart.fetchImage();

                            panel.add(tf);
                            panel.add(freeTrafficChart, "spany, wrap");
                        } else {
                            panel.add(tf, "span 2, wrap");
                        }
                    }

                }
                new ContainerDialog(UserIO.NO_CANCEL_OPTION, def, panel, null, null);

                return null;
            }

        }.start();
    }

    public static void showChangelogDialog() {
        int status = UserIO.getInstance().requestHelpDialog(UserIO.NO_CANCEL_OPTION, JDL.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDL.L("system.update.message", "Update successfull"), JDL.L("system.update.showchangelogv2", "What's new?"), "http://jdownloader.org/changes/index");
        if (JDFlags.hasAllFlags(status, UserIO.RETURN_OK) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, true)) {
            try {
                JLinkButton.openURL("http://jdownloader.org/changes/index");
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    public SubConfiguration getGuiConfig() {
        return SimpleGuiConstants.GUI_CONFIG;
    }

    public Container getRealContentPane() {

        return super.getContentPane();

    }

    public ContentPanel getContentPane() {
        return this.contentPanel;

    }

    /**
     * Returns of Substance LAF is active
     * 
     * @return
     */
    public static boolean isSubstance() {
        return UIManager.getLookAndFeel().getName().contains("Substance");
    }

    public void hideSideBar(boolean b) {
        if (this.taskPaneView == null || taskPaneView.isVisible() == !b) return;
        if (b) {
            if (this.sep != null) this.sep.setMinimized(b);
            taskPaneView.setVisible(!b);
            // this.contentPanel.display(linkListPane);

        } else {
            if (this.sep != null) this.sep.setMinimized(b);
            taskPaneView.setVisible(!b);

        }

        SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_SIDEBAR_COLLAPSED, b);
        SimpleGuiConstants.GUI_CONFIG.save();

    }

    public SwitchPanel getDownloadPanel() {
        return this.linkListPane;
    }

    public boolean isJTattoo() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setContent(SwitchPanel tabbedPanel) {
        this.getContentPane().display(tabbedPanel);
    }

}
