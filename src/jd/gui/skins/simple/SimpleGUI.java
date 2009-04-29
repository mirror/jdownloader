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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.Main;
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
import jd.controlling.interaction.Interaction;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.gui.JDLookAndFeelManager;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.components.TwoTextFieldDialog;
import jd.gui.skins.simple.components.DownloadView.DownloadLinksPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkAdder;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberTreeTableAction;
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
import jd.gui.skins.simple.listener.JDMouseListener;
import jd.gui.skins.simple.startmenu.JDStartMenu;
import jd.gui.skins.simple.tasks.AddonTaskPane;
import jd.gui.skins.simple.tasks.ConfigTaskPane;
import jd.gui.skins.simple.tasks.DownloadTaskPane;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.gui.skins.simple.tasks.LogTaskPane;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXLoginDialog;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.JXLoginPane.Status;
import org.jdesktop.swingx.image.StackBlurFilter;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.utils.LafConstants.AnimationKind;

public class SimpleGUI extends JXFrame implements UIInterface, ActionListener, WindowListener {

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

    private SpeedMeterPanel speedmeter;

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

    private boolean noTitlePane = false;

    private JXCollapsiblePane leftcolPane;

    private JDSeparator sep;

    private SingletonPanel logPanel;

    private SingletonPanel addonPanel;

    private ConfigTaskPane cfgTskPane;

    private AddonTaskPane addonTaskPanel;

    private JDSubstanceUI titleUI;

    private Image mainMenuIconRollOver;

    private Image mainMenuIcon;

    private boolean mainMenuRollOverStatus = false;

    private SwingWorker<Object, Object> cursorworker;

    /**
     * Das Hauptfenster wird erstellt. Singleton. Use SimpleGUI.createGUI
     */
    private SimpleGUI() {
        super("");
        SimpleGuiConstants.GUI_CONFIG = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        JDLookAndFeelManager.setUIManager();

        /**
         * Init panels
         */

        statusBar = new JDStatusBar();
        initWaitPane();
        this.setEnabled(false);
        this.setWaiting(true);

        if (isSubstance()) {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 54, 54);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 54, 54);
            this.getRootPane().setUI(titleUI = new JDSubstanceUI(mainMenuIcon));

            // JDController.getInstance().addControlListener(new
            // ConfigPropertyListener(SimpleGuiConstants.ANIMATION_ENABLED) {
            //
            // // @Override
            // public void onPropertyChanged(Property source, String
            // propertyName) {
            //
            // }
            //
            // });
            noTitlePane = false;

        } else {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54_trans"), 32, 32);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_54_54"), 32, 32);
            this.noTitlePane = true;

        }

        toolBar = new JDToolBar(noTitlePane, mainMenuIcon);

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
        JDLocale.initLocalisation();

        updateDecoration();
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
        this.getLeftcolPane().setAnimated(false);
        this.hideSideBar(SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_SIDEBAR_COLLAPSED, false));
        this.getLeftcolPane().setAnimated(true);
        setVisible(true);

        ClipboardHandler.getClipboard().setTempDisableD(false);
        this.contentPanel.addMouseListener(new JDMouseListener() {
            public void mouseEntered(MouseEvent e) {
                System.out.println("Entered");
            }
        });
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
                        jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                    }
                }
            }
        }.start();

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

    @SuppressWarnings("unchecked")
    private void initWaitPane() {
        JXPanel glass = new JXPanel(new MigLayout("ins 80,wrap 1", "[fill,grow]", "[fill,grow][]"));
        JXLabel lbl = new JXLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_128_128"), 300, 300));
        glass.add(lbl, "alignx center, aligny center");
        JProgressBar prg;
        glass.add(prg = new JProgressBar(), "alignx center, aligny center,shrink");
        prg.setStringPainted(false);

        prg.setIndeterminate(true);
        glass.setOpaque(false);
        glass.setAlpha(0.5f);
        AbstractPainter fgPainter = (AbstractPainter) lbl.getForegroundPainter();
        StackBlurFilter filter = new StackBlurFilter();
        fgPainter.setFilters(filter);
        glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.setWaitPane(glass);
        glass.addMouseListener(new JDMouseListener());

    }

    public void onLAFChanged() {
        if (isSubstance()) {
            mainMenuIcon = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_48_48_noShadow"), 48, 48);
            mainMenuIconRollOver = JDImage.getScaledImage(JDImage.getImage("logo/jd_logo_48_48"), 48, 48);
            this.getRootPane().setUI(titleUI = new JDSubstanceUI(mainMenuIcon));

            JDController.getInstance().addControlListener(new ConfigPropertyListener(SimpleGuiConstants.ANIMATION_ENABLED) {

                // @Override
                public void onPropertyChanged(Property source, String propertyName) {

                }

            });
            noTitlePane = false;
        }

    }

    public void onMainMenuMouseClick(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        // menu.setSize(50,50);

        // menu.setPreferredSize(mb.getMinimumSize());
        // menu.setMinimumSize(mb.getMinimumSize());

        JDStartMenu.createMenu(popup);

        popup.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
        // mb.add(menu);
    }

    public void onMainMenuMouseExit() {
        if (!mainMenuRollOverStatus) return;
        this.mainMenuRollOverStatus = false;
        toolBar.setMainMenuIcon(mainMenuIcon);
        toolBar.setToolTipText(null);
        if (titleUI != null) {
            titleUI.setMainMenuIcon(mainMenuIcon);
            titleUI.setToolTipText(null);
        }
    }

    public void onMainMenuMouseEnter() {
        if (mainMenuRollOverStatus) return;
        this.mainMenuRollOverStatus = true;
        toolBar.setMainMenuIcon(mainMenuIconRollOver);
        toolBar.setToolTipText(JDLocale.L("gui.menu.tooltip", "Click to open the main menu"));
        if (titleUI != null) {
            titleUI.setMainMenuIcon(mainMenuIconRollOver);
            titleUI.setToolTipText("Click to open the main menu");
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

        if (UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
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

    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     * 
     * @param e
     *            Die erwünschte Aktion
     */

    public void actionPerformed(ActionEvent e) {
        // JDSounds.PT("sound.gui.clickToolbar");
        // switch (e.getID()) {
        // // case JDAction.ITEMS_MOVE_UP:
        // // case JDAction.ITEMS_MOVE_DOWN:
        // // case JDAction.ITEMS_MOVE_TOP:
        // // case JDAction.ITEMS_MOVE_BOTTOM:
        // // linkListPane.moveSelectedItems(e.getID());
        // // break;
        // case JDAction.APP_ALLOW_RECONNECT:
        // logger.finer("Allow Reconnect");
        // boolean checked =
        // !JDUtilities.getConfiguration().getBooleanProperty(Configuration
        // .PARAM_DISABLE_RECONNECT, false);
        // if (checked) {
        //displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled",
        // "Reconnect deaktiviert!"),
        // JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip",
        // "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."
        // ), 10000);
        // }
        //
        // JDUtilities.getConfiguration().setProperty(Configuration.
        // PARAM_DISABLE_RECONNECT, checked);
        // JDUtilities.getConfiguration().save();
        //
        // /*
        // * Steht hier, weil diese Funktion(toggleReconnect) direkt vom
        // * Trayicon Addon aufgerufen wird und ich dennoch die Gui aktuell
        // * halten will
        // */
        //
        // // btnReconnect.setIcon(new
        // // ImageIcon(JDUtilities.getImage(getDoReconnectImage())));
        // break;
        // // case JDAction.APP_PAUSE_DOWNLOADS:
        // // btnPause.setSelected(!btnPause.isSelected());
        // // fireUIEvent(new UIEvent(this, UIEvent.UI_PAUSE_DOWNLOADS,
        // // btnPause.isSelected()));
        // // btnPause.setIcon(new
        // // ImageIcon(JDUtilities.getImage(getPauseImage())));
        // // break;
        // case JDAction.APP_CLIPBOARD:
        // logger.finer("Clipboard");
        // ClipboardHandler.getClipboard().toggleActivation();
        // break;
        //

        //
        // case JDAction.APP_EXIT:
        // frame.setVisible(false);
        // frame.dispose();
        // JDUtilities.getController().exit();
        //
        // break;
        // case JDAction.APP_RESTART:
        // frame.setVisible(false);
        // frame.dispose();
        // JDUtilities.getController().restart();
        // break;
        // case JDAction.APP_LOG:
        // logDialog.setVisible(!logDialog.isVisible());
        // menViewLog.setSelected(!logDialog.isVisible());
        // break;
        // case JDAction.APP_BACKUP:
        // JDInit.createQueueBackup();
        // JOptionPane.showMessageDialog(null,
        // JDLocale.L("gui.messagewindow.backupcreated",
        // "Link list backup created successfuly."),
        // JDLocale.L("gui.messagewindow.information", "Information"),
        // JOptionPane.INFORMATION_MESSAGE);
        // break;
        // // case JDAction.APP_RECONNECT:
        // // new Thread() {
        // // //@Override
        // // public void run() {
        // // doReconnect();
        // // }
        // // }.start();
        // // break;
        // // case JDAction.APP_UPDATE:
        // // fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_UPDATE));
        // //
        // // break;
        // // case JDAction.ITEMS_REMOVE:
        // // if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS,
        // // false)) {
        // // if (showConfirmDialog(JDLocale.L("gui.downloadlist.delete",
        // // "Ausgewählte Links wirklich entfernen?") + " (" +
        // // linkListPane.countSelectedLinks() + " links in " +
        // // linkListPane.countSelectedPackages() + " packages)")) {
        // // linkListPane.removeSelectedLinks();
        // // }
        // // } else {
        // // linkListPane.removeSelectedLinks();
        // // }
        // // break;
        // case JDAction.APP_OPEN_OPT_CONFIG:
        // SimpleGUI.showConfigDialog(frame, new
        // ConfigPanelAddons(JDUtilities.getConfiguration()), false);
        // JDUtilities.getConfiguration().save();
        // break;
        // // case JDAction.APP_OPEN_HOST_CONFIG:
        // // SimpleGUI.showConfigDialog(frame, new
        // // ConfigPanelPluginForHost(JDUtilities.getConfiguration()), false);
        // // JDUtilities.getConfiguration().save();
        // // break;
        // // case JDAction.ITEMS_REMOVE_PACKAGES:
        // // if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS,
        // // false)) {
        // // if
        // (showConfirmDialog(JDLocale.L("gui.downloadlist.delete_packages",
        // // "Remove completed packages?"))) {
        // // JDUtilities.getController().removeCompletedPackages();
        // // }
        // // } else {
        // // JDUtilities.getController().removeCompletedPackages();
        // // }
        // // break;
        // // case JDAction.ITEMS_REMOVE_LINKS:
        // // if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS,
        // // false)) {
        // // if
        // //
        // (showConfirmDialog(JDLocale.L("gui.downloadlist.delete_downloadlinks"
        // // , "Remove completed downloads?"))) {
        // // JDUtilities.getController().removeCompletedDownloadLinks();
        // // }
        // // } else {
        // // JDUtilities.getController().removeCompletedDownloadLinks();
        // // }
        // // break;
        //
        // case JDAction.ABOUT:
        // JDAboutDialog.showDialog();
        // break;
        // case JDAction.CHANGES:
        // showChangelogDialog();
        // break;
        // case JDAction.ITEMS_ADD:

        // }
        // break;
        // case JDAction.HELP:
        // try {
        // JLinkButton.openURL(JDLocale.L("gui.support.forumurl",
        // "http://board.jdownloader.org"));
        // } catch (Exception e1) {
        // JDLogger.exception(e1);
        // }
        // break;
        // case JDAction.WIKI:
        // try {
        // JLinkButton.openURL(JDLocale.L("gui.support.wikiurl",
        // "http://wiki.jdownloader.org"));
        // } catch (Exception e1) {
        // JDLogger.exception(e1);
        // }
        // break;
        // // case JDAction.APP_CONFIGURATION:
        // // showConfig();
        // // break;
    }

    // public void showConfig() {
    // if (guiConfig.getBooleanProperty(PARAM_SHOW_FENGSHUI, true) == false) {
    // ConfigurationDialog.showConfig(frame);
    // } else {
    // if (SimpleGUI.CURRENTGUI != null) {
    // if (SimpleGUI.CURRENTGUI.isVisible() == false) {
    // FengShuiConfigPanel.getInstance();
    // } else {
    // ConfigurationDialog.showConfig(frame);
    // }
    // } else {
    // FengShuiConfigPanel.getInstance();
    // }
    // }
    // }

    public synchronized void addLinksToGrabber(final Vector<DownloadLink> links, final boolean hideGrabber) {
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
        addLogTask();

        progressBar = new TabProgress();

        contentPanel.display(linkListPane);

        taskPane.switcher(dlTskPane);

        JPanel panel = new JPanel(new MigLayout("ins 0,wrap 3", "[fill]0[shrink]0[fill,grow 100]", "[]0[grow,fill]0[]0[]0[]0[]"));

        setContentPane(panel);
        panel.add(this.toolBar, "spanx");

        leftcolPane = new JDCollapsiblePane();
        leftcolPane.add(new JScrollPane(taskPane));

        panel.add(leftcolPane, "spany 2");
        sep = new JDSeparator();

        leftcolPane.addPropertyChangeListener(sep);
        panel.add(sep, "gapright 2,spany 2");

        panel.add(contentPanel);
        // sp.setBorder(null);

        panel.add(JDCollapser.getInstance(), "hidemode 3,gaptop 15,cell 2 2,growx,pushx");
        // panel.add(generalPurposeTasks, "cell 0 2");
        // contentPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        panel.add(progressBar, "spanx,hidemode 3,cell 0 3");

        // panel.add(new PremiumStatus(), "spanx, cell 0 4");
        panel.add(this.statusBar, "spanx, cell 0 5");

    }

    private void addAddonTask() {
        addonTaskPanel = new AddonTaskPane(JDLocale.L("gui.taskpanes.addons", "Addons"), JDTheme.II("gui.images.taskpanes.addons", 24, 24));

        addonPanel = new SingletonPanel(AddonPane.class);
        addonTaskPanel.addPanel(addonPanel);
        taskPane.add(addonTaskPanel);
    }

    public AddonTaskPane getAddonPanel() {
        return addonTaskPanel;
    }

    private void addLogTask() {

        LogTaskPane logTask = new LogTaskPane(JDLocale.L("gui.taskpanes.log", "Log"), JDTheme.II("gui.images.terminal", 24, 24));

        logPanel = new SingletonPanel(LogPane.class, new Object[] { logger });
        logTask.addPanel(logPanel);
        logTask.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                switch (e.getID()) {
                case DownloadTaskPane.ACTION_CLICK:

                    contentPanel.display(((TaskPanel) e.getSource()).getPanel(0));

                }
            }
        });
        taskPane.add(logTask);
    }

    private void addConfigTask() {
        cfgTskPane = new ConfigTaskPane(JDLocale.L("gui.taskpanes.configuration", "Configuration"), JDTheme.II("gui.images.taskpanes.configuration", 24, 24));

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
                        if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.config.save.restart", "Your changes need a restart of JDownloader to take effect.\r\nRestart now?"), JDLocale.L("gui.config.save.restart.title", "JDownloader restart requested"))) {
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
        lgTaskPane = new LinkGrabberTaskPane(JDLocale.L("gui.taskpanes.linkgrabber", "LinkGrabber"), JDTheme.II("gui.images.taskpanes.linkgrabber", 24, 24));
        lgTaskPane.addPanel(new SingletonPanel(LinkAdder.class));
        LinkGrabberController.getInstance().getBroadcaster().addListener(new LinkGrabberControllerListener() {
            public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
                switch (event.getID()) {
                case LinkGrabberControllerEvent.ADDED:
                    taskPane.switcher(dlTskPane);
                    break;
                case LinkGrabberControllerEvent.EMPTY:
                    lgTaskPane.setPanelID(0);
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
                case LinkGrabberTreeTableAction.GUI_ADD:
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

        dlTskPane = new DownloadTaskPane(JDLocale.L("gui.taskpanes.download", "Download"), JDTheme.II("gui.images.taskpanes.download", 24, 24));
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
                case DownloadTaskPane.ACTION_STARTSTOP:

                    if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
                        JDUtilities.getController().stopDownloads();
                    } else {
                        JDUtilities.getController().startDownloads();
                    }
                    break;
                }

            }

        });
        taskPane.add(dlTskPane);
    }

    public JXCollapsiblePane getLeftcolPane() {
        return leftcolPane;
    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:
                    setTitle(JDUtilities.getJDTitle());
                    SimpleGUI.this.setWaiting(false);
                    SimpleGUI.this.setEnabled(true);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false)) {
                        JDUtilities.getController().startDownloads();
                    }
                    

                    break;
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Module started: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {

                        setTitle(JDUtilities.JD_TITLE + " | " + JDLocale.L("gui.titleaddaction", "Action: ") + " " + ((Interaction) event.getSource()).getInteractionName());
                    }
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.setVisible(false);
                    SimpleGUI.this.dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Module finished: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {

                        if (Interaction.areInteractionsInProgress()) {

                            setTitle(JDUtilities.getJDTitle());
                        }
                    }
                    break;

                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                    if (speedmeter != null) speedmeter.stop();
                    for (DownloadLink link : DownloadController.getInstance().getAllDownloadLinks()) {
                        if (link.getLinkStatus().hasStatus(LinkStatus.TODO)) {
                            logger.info("Downloads stopped");
                            return;
                        }

                    }
                    logger.info("All downloads finished");

                    break;
                case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
                    setTitle(JDUtilities.getJDTitle() + " - terminate");
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                    setTitle(JDUtilities.getJDTitle());
                    break;

                case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                    if ((Property) event.getSource() == JDUtilities.getConfiguration()) {
                        if (event.getParameter().equals(Configuration.PARAM_DISABLE_RECONNECT)) {

                        } else if (event.getParameter().equals(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE)) {

                        }
                    }
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:

                    if (speedmeter != null) speedmeter.start();

                    break;
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    if (speedmeter != null) speedmeter.stop();

                    break;
                }
            }
        });
    }

    public void displayMiniWarning(final String shortWarn, final String toolTip, final int showtime) {
        // if (shortWarn == null) {
        // SwingUtilities.invokeLater(new Runnable() {
        // public void run() {
        // warning.setVisible(false);
        // warning.setText("");
        // }
        // });
        // } else {
        // SwingUtilities.invokeLater(new Runnable() {
        // public void run() {
        // warning.setVisible(true);
        // warning.setText(shortWarn);
        // warning.setToolTipText(toolTip);
        // }
        // });
        // if (warningWorker != null) {
        // warningWorker.interrupt();
        // }
        // warningWorker = new Thread() {
        //
        // public void run() {
        // for (int i = 0; i < 5; i++) {
        // try {
        // Thread.sleep(300);
        // } catch (Exception e) {
        // }
        // if (this.isInterrupted()) { return; }
        // EventQueue.invokeLater(new Runnable() {
        // public void run() {
        // warning.setEnabled(false);
        // }
        // });
        // if (this.isInterrupted()) { return; }
        // try {
        // Thread.sleep(100);
        // } catch (Exception e) {
        // }
        // if (this.isInterrupted()) { return; }
        // EventQueue.invokeLater(new Runnable() {
        // public void run() {
        // warning.setEnabled(true);
        // }
        // });
        // if (this.isInterrupted()) { return; }
        //
        // }
        //
        // }
        // };
        // warningWorker.start();
        //
        // if (showtime > 0) {
        // new Thread() {
        // public void run() {
        // try {
        // Thread.sleep(showtime);
        // } catch (InterruptedException e) {
        //
        //jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE
        // ,"Exception occured",e);
        // }
        // displayMiniWarning(null, null, 0);
        // }
        // }.start();
        // }
        // }

        /**
         * TODO
         */
    }

    public void doManualReconnect() {
        boolean restart = false;
        if (!SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            int confirm = JOptionPane.showConfirmDialog(this, JDLocale.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?"), "", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);

                restart = JDUtilities.getController().stopDownloads();
                if (Reconnecter.waitForNewIP(1)) {
                    showMessageDialog(JDLocale.L("gui.reconnect.success", "Reconnect erfolgreich"));
                } else {
                    showMessageDialog(JDLocale.L("gui.reconnect.failed", "Reconnect fehlgeschlagen"));
                }
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
            }
        } else {
            restart = JDUtilities.getController().stopDownloads();
            if (Reconnecter.waitForNewIP(1)) {
                showMessageDialog(JDLocale.L("gui.reconnect.success", "Reconnect erfolgreich"));
            } else {
                showMessageDialog(JDLocale.L("gui.reconnect.failed", "Reconnect fehlgeschlagen"));
            }
        }
        if (restart) {
            JDUtilities.getController().startDownloads();
        }
    }

    public String showCountdownUserInputDialog(final String message, final String def) {

        GuiRunnable<String> run = new GuiRunnable<String>() {

            public String runSave() {
                return new InputDialog(SimpleGUI.this, message, def).getInputText();
            }

        };
        return run.getReturnValue();

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
        case UIInterface.WINDOW_STATUS_TRAYED:
            break;
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
        return showConfirmDialog(message, "");

    }

    public boolean showConfirmDialog(final String message, final String title) {

        GuiRunnable<Boolean> run = new GuiRunnable<Boolean>() {

            // @Override
            public Boolean runSave() {
                Object[] options = { JDLocale.L("gui.btn_yes", "Yes"), JDLocale.L("gui.btn_no", "No") };
                int n = JOptionPane.showOptionDialog(SimpleGUI.this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                return n == 0;
            }
        };

        return run.getReturnValue();
    }

    public boolean showCountdownConfirmDialog(final String string, final int sec) {
        return new GuiRunnable<Boolean>() {
            // @Override
            public Boolean runSave() {
                return CountdownConfirmDialog.showCountdownConfirmDialog(SimpleGUI.this, string, sec);

            }

        }.getReturnValue();

    }

    public int showHelpMessage(final String title, String message, final boolean toHTML, final String url, final String helpMsg, final int sec) {

        final String msg = toHTML ? "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + message + "</font>" : message;

        return new GuiRunnable<Integer>() {
            // @Override
            public Integer runSave() {
                try {
                    return JHelpDialog.showHelpMessage(SimpleGUI.this, title, msg, new URL(url), helpMsg, sec);
                } catch (MalformedURLException e) {

                }
                return -1;

            }

        }.getReturnValue();

    }

    public boolean showHTMLDialog(final String title, final String htmlQuestion) {
        return new GuiRunnable<Boolean>() {
            // @Override
            public Boolean runSave() {

                return HTMLDialog.showDialog(SimpleGUI.this, title, htmlQuestion);

            }

        }.getReturnValue();

    }

    public void showMessageDialog(final String string) {
        // logger.info("MessageDialog");

        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {

                JOptionPane.showMessageDialog(SimpleGUI.this, string);
                return null;
            }

        }.start();

    }

    public String showTextAreaDialog(final String title, final String question, final String def) {

        GuiRunnable<String> run = new GuiRunnable<String>() {
            // @Override
            public String runSave() {
                return TextAreaDialog.showDialog(SimpleGUI.this, title, question, def);
            }
        };
        return run.getReturnValue();

    }

    public String showUserInputDialog(final String string) {

        return showUserInputDialog(string, "");
    }

    public String showUserInputDialog(final String string, final String def) {
        GuiRunnable<String> run = new GuiRunnable<String>() {
            // @Override
            public String runSave() {
                return JOptionPane.showInputDialog(SimpleGUI.this, string, def);
            }
        };
        return run.getReturnValue();

    }

    public String[] showTextAreaDialog(final String title, final String questionOne, final String questionTwo, final String defaultOne, final String defaultTwo) {

        GuiRunnable<String[]> run = new GuiRunnable<String[]>() {
            // @Override
            public String[] runSave() {
                return TextAreaDialog.showDialog(SimpleGUI.this, title, questionOne, questionTwo, defaultOne, defaultTwo);
            }
        };
        return run.getReturnValue();

    }

    public String[] showTwoTextFieldDialog(final String title, final String questionOne, final String questionTwo, final String defaultOne, final String defaultTwo) {

        GuiRunnable<String[]> run = new GuiRunnable<String[]>() {
            // @Override
            public String[] runSave() {
                return TwoTextFieldDialog.showDialog(SimpleGUI.this, title, questionOne, questionTwo, defaultOne, defaultTwo);
            }
        };
        return run.getReturnValue();

    }

    public static void displayConfig(final ConfigContainer container, final int i) {
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                ConfigEntriesPanel cep;
                JDCollapser.getInstance().setContentPanel(cep = new ConfigEntriesPanel(container));
                if (i > 0) {
                    Component comp = cep.getComponent(0);
                    if (comp instanceof JTabbedPane) {
                        ((JTabbedPane) comp).setSelectedIndex(((JTabbedPane) comp).getTabCount() - 1);
                    }
                }
                if (container.getGroup() != null) {
                    JDCollapser.getInstance().setTitle(container.getGroup().getName());
                    JDCollapser.getInstance().setIcon(container.getGroup().getIcon());
                } else {
                    JDCollapser.getInstance().setTitle(JDLocale.L("gui.panels.collapsibleconfig", "Settings"));
                    JDCollapser.getInstance().setIcon(JDTheme.II("gui.images.config.addons", 24, 24));
                }

                JDCollapser.getInstance().setVisible(true);
                JDCollapser.getInstance().setCollapsed(false);
                return null;
            }

        }.waitForEDT();
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (!OSDetector.isMac()) {
            if (e.getComponent() == this) {
                boolean doIt;
                if (!SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                    doIt = showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
                } else {
                    doIt = true;
                }
                if (doIt) {
                    SimpleGuiUtils.saveLastLocation(e.getComponent(), null);
                    SimpleGuiUtils.saveLastDimension(e.getComponent(), null);
                    SimpleGuiConstants.GUI_CONFIG.save();
                    JDUtilities.getController().exit();

                }
            }
        } else {
            e.getWindow().setVisible(false);
        }

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
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("gui.accountcheck.pluginerror", "Plugin %s may be defect. Inform support!", pluginForHost.getPluginID()));
                    return null;
                }
                if (ai == null) {
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet.", pluginForHost.getHost()));
                    return null;
                }
                if (!ai.isValid()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.notValid", "The account for '%s' isn't valid! Please check username and password!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }
                if (ai.isExpired()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.expired", "The account for '%s' is expired! Please extend the account or buy a new one!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }
                JPanel panel = new JPanel(new MigLayout("ins 22", "[right]10[grow,fill]40"));
                String def = JDLocale.LF("plugins.host.premium.info.title", "Accountinformation from %s for %s", account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDLocale.L("plugins.host.premium.info.validUntil", "Valid until"), JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDLocale.L("plugins.host.premium.info.files", "Files"), JDLocale.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDLocale.L("plugins.host.premium.info.usedSpace", "Used Space"), JDLocale.L("plugins.host.premium.info.cash", "Cash"), JDLocale.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDLocale.L("plugins.host.premium.info.status", "Info") };

                DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String validUntil = (ai.isExpired() ? "[expired] " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                if (ai.getValidUntil() == -1) validUntil = null;
                String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                String[] data = new String[] { validUntil, Formatter.formatReadable(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, Formatter.formatReadable(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", Formatter.formatReadable(ai.getTrafficShareLeft()), ai.getStatus() };
                panel.add(new JXTitledSeparator(def), "spanx, pushx, growx, gapbottom 15");
                PieChartAPI freeTrafficChart = new PieChartAPI("", 125, 60);
                freeTrafficChart.addEntity(new ChartAPIEntity("Free", ai.getTrafficLeft(), new Color(50, 200, 50)));
                freeTrafficChart.addEntity(new ChartAPIEntity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                freeTrafficChart.fetchImage();

                for (int j = 0; j < data.length; j++) {
                    if (data[j] != null && !data[j].equals("-1")) {
                        panel.add(new JLabel(label[j]), "gapleft 20");
                        if (label[j].equals(JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                            JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            JTextField tf;
                            panel2.add(tf = new JTextField(data[j]));
                            tf.setBorder(null);
                            tf.setBackground(null);
                            tf.setEditable(false);
                            tf.setOpaque(false);
                            panel2.add(freeTrafficChart);
                            panel.add(panel2, "wrap");
                        } else {
                            JTextField tf;
                            panel.add(tf = new JTextField(data[j]), "wrap");
                            tf.setBorder(null);
                            tf.setBackground(null);
                            tf.setEditable(false);
                            tf.setOpaque(false);

                        }
                    }

                }
                JOptionPane.showMessageDialog(null, panel, def, JOptionPane.INFORMATION_MESSAGE);

                return null;
            }

        }.start();
    }

    public static void showChangelogDialog() {
        int status = JDUtilities.getGUI().showHelpMessage(JDLocale.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDLocale.L("system.update.message", "Update successfull"), false, "http://jdownloader.org/changes/index", JDLocale.L("system.update.showchangelogv2", "What's new?"), 60);
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, true) && status != 0) {
            try {
                JLinkButton.openURL("http://jdownloader.org/changes/index");
            } catch (Exception e) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            }
        }
    }

    public SubConfiguration getGuiConfig() {
        return SimpleGuiConstants.GUI_CONFIG;
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
        if (getLeftcolPane() == null || getLeftcolPane().isCollapsed() == b) return;
        if (b) {
            getLeftcolPane().setCollapsed(true);
            this.contentPanel.display(linkListPane);

        } else {
            getLeftcolPane().setCollapsed(false);

        }

        SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_SIDEBAR_COLLAPSED, b);
        SimpleGuiConstants.GUI_CONFIG.save();

    }

    public JTabbedPanel getDownloadPanel() {
        return this.linkListPane;
    }

    public String[] showLoginDialog(String title, String defaultUser, String defaultPassword, String error) {
        JXLoginDialog d = new JXLoginDialog(this, JDLocale.L("gui.dialogs.login.title", "Login required"), true);
        if (defaultPassword != null) d.getPanel().setPassword(defaultPassword.toCharArray());
        d.getPanel().setUserName(defaultUser);
        d.getPanel().setErrorMessage(error);
        d.getPanel().setMessage(title);
        d.setVisible(true);

        if (d.getStatus() != Status.SUCCEEDED) return null;

        return new String[] { d.getPanel().getUserName(), new String(d.getPanel().getPassword()) };
    }

}
