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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.gui.JDLookAndFeelManager;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.components.TwoTextFieldDialog;
import jd.gui.skins.simple.components.DownloadView.DownloadLinksPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2Panel;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.ConfigPanelAddons;
import jd.gui.skins.simple.config.ConfigPanelCaptcha;
import jd.gui.skins.simple.config.ConfigPanelDownload;
import jd.gui.skins.simple.config.ConfigPanelEventmanager;
import jd.gui.skins.simple.config.ConfigPanelGUI;
import jd.gui.skins.simple.config.ConfigPanelGeneral;
import jd.gui.skins.simple.config.ConfigPanelPluginForHost;
import jd.gui.skins.simple.config.ConfigPanelReconnect;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.gui.skins.simple.config.FengShuiConfigPanel;
import jd.gui.skins.simple.premium.PremiumPane;
import jd.gui.skins.simple.tasks.ConfigTaskPane;
import jd.gui.skins.simple.tasks.DownloadTaskPane;
import jd.gui.skins.simple.tasks.GeneralPurposeTaskPanel;
import jd.gui.skins.simple.tasks.LinkGrabberTaskPane;
import jd.gui.skins.simple.tasks.PremiumTaskPane;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.nutils.OSDetector;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jvnet.substance.SubstanceLookAndFeel;

public class SimpleGUI extends JXFrame implements UIInterface, ActionListener, WindowListener {

    public static SimpleGUI CURRENTGUI = null;

    private static SimpleGUI INSTANCE;

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    private LinkGrabberV2Panel linkGrabber;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksPanel linkListPane;

    private LogDialog logDialog;

    public LogDialog getLogDialog() {
        return logDialog;
    }

    private Logger logger = JDUtilities.getLogger();

    private TabProgress progressBar;

    private SpeedMeterPanel speedmeter;

    private TaskPane taskPane;

    private ContentPanel contentPanel;

    private DownloadTaskPane dlTskPane;

    private LinkGrabberTaskPane lgTaskPane;

    private JDToolBar toolBar;

    private JDMenuBar menuBar;

    /**
     * Das Hauptfenster wird erstellt. Singleton. Use SimpleGUI.createGUI
     */
    private SimpleGUI() {
        super();
        SimpleGuiConstants.GUI_CONFIG = JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME);
        JDLookAndFeelManager.setUIManager();

        menuBar = new JDMenuBar();
        toolBar = new JDToolBar();

        addWindowListener(this);
        setIconImage(JDTheme.I("gui.images.jd_logo"));
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
            dim = new Dimension(600, 600);
        }
        setPreferredSize(dim);
        setLocation(SimpleGuiUtils.getLastLocation(null, null, this));
        pack();
        setExtendedState(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("MAXIMIZED_STATE_OF_" + this.getName(), JFrame.NORMAL));
        setVisible(true);

        ClipboardHandler.getClipboard();

        new Thread("guiworker") {
            public void run() {
                while (true) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            interval();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void updateDecoration() {
        if (UIManager.getLookAndFeel().getSupportsWindowDecorations()) {
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        } else {
            setUndecorated(false);
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
        JDSounds.PT("sound.gui.clickToolbar");
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
        // displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled",
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
        // case JDAction.APP_SAVE_DLC:
        // JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
        // fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
        // fc.setDialogTitle(JDLocale.L("gui.filechooser.savelistasdlc",
        // "Save list in DLC file"));
        // if (fc.showSaveDialog(frame) == JDFileChooser.APPROVE_OPTION) {
        // File ret = fc.getSelectedFile();
        // if (ret == null) return;
        // if (JDIO.getFileExtension(ret) == null ||
        // !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
        // ret = new File(ret.getAbsolutePath() + ".dlc");
        // }
        // if (ret != null) {
        //
        // JDUtilities.getController().saveDLC(ret,
        // JDUtilities.getController().getDownloadLinks());
        // }
        // }
        // break;
        // case JDAction.APP_LOAD_DLC:
        // fc = new JDFileChooser("_LOADSAVEDLC");
        // fc.setDialogTitle(JDLocale.L("gui.filechooser.loaddlc",
        // "Load DLC file"));
        // fc.setFileFilter(new JDFileFilter(null,
        // ".dlc|.rsdf|.ccf|.linkbackup", true));
        // if (fc.showOpenDialog(frame) == JDFileChooser.APPROVE_OPTION) {
        // File ret2 = fc.getSelectedFile();
        // if (ret2 != null) {
        // JDUtilities.getController().loadContainerFile(ret2);
        //
        // }
        // }
        // break;
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
        // // @Override
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
        // String cb = "";
        // try {
        // cb = (String)
        // Toolkit.getDefaultToolkit().getSystemClipboard().getData
        // (DataFlavor.stringFlavor);
        // } catch (Exception e1) {
        // }
        // String data = LinkInputDialog.showDialog(frame, cb.trim());
        // if (data != null && data.length() > 0) {
        //
        // JDUtilities.getController().distributeLinks(data);
        // }
        // break;
        // case JDAction.HELP:
        // try {
        // JLinkButton.openURL(JDLocale.L("gui.support.forumurl",
        // "http://board.jdownloader.org"));
        // } catch (Exception e1) {
        // e1.printStackTrace();
        // }
        // break;
        // case JDAction.WIKI:
        // try {
        // JLinkButton.openURL(JDLocale.L("gui.support.wikiurl",
        // "http://wiki.jdownloader.org"));
        // } catch (Exception e1) {
        // e1.printStackTrace();
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

    public synchronized void addLinksToGrabber(Vector<DownloadLink> links, boolean hideGrabber) {
        logger.info("GRAB");
        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        logger.info("add to grabber");
        linkGrabber.addLinks(linkList);
        taskPane.switcher(lgTaskPane);

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
        // taskPane.setBackgroundPainter(null);

        dlTskPane = new DownloadTaskPane(JDLocale.L("gui.taskpanes.download", "Download"), JDTheme.II("gui.images.taskpanes.download", 24, 24));
        dlTskPane.addPanel(new SingletonPanel(linkListPane));
        dlTskPane.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println(e.getActionCommand());
                switch (e.getID()) {
                case DownloadTaskPane.ACTION_TOGGLE:

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

        linkGrabber = new LinkGrabberV2Panel();
        lgTaskPane = new LinkGrabberTaskPane(JDLocale.L("gui.taskpanes.linkgrabber", "LinkGrabber"), JDTheme.II("gui.images.taskpanes.linkgrabber", 24, 24));
        lgTaskPane.addPanel(new SingletonPanel(linkGrabber));
        linkGrabber.getJDBroadcaster().addJDListener(lgTaskPane);
        lgTaskPane.addActionListener(linkGrabber);
        lgTaskPane.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println(e.getActionCommand());
                switch (e.getID()) {
                case DownloadTaskPane.ACTION_TOGGLE:

                    contentPanel.display(((TaskPanel) e.getSource()).getPanel(0));
                    break;
                }
            }
        });
        taskPane.add(lgTaskPane);

        ConfigTaskPane cfgTskPane = new ConfigTaskPane(JDLocale.L("gui.taskpanes.configuration", "Configuration"), JDTheme.II("gui.images.taskpanes.configuration", 24, 24));
        cfgTskPane.addPanel(new SingletonPanel(FengShuiConfigPanel.class, new Object[] {}));
        Object[] configConstructorObjects = new Object[] { JDUtilities.getConfiguration() };

        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_ADDONS, new SingletonPanel(ConfigPanelAddons.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_CAPTCHA, new SingletonPanel(ConfigPanelCaptcha.class, configConstructorObjects));

        // cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_DECRYPT, new
        // SingletonPanel(ConfigPanelPluginForDecrypt.class,
        // configConstructorObjects));

        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_DOWNLOAD, new SingletonPanel(ConfigPanelDownload.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_EVENTMANAGER, new SingletonPanel(ConfigPanelEventmanager.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GENERAL, new SingletonPanel(ConfigPanelGeneral.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GUI, new SingletonPanel(ConfigPanelGUI.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_HOST, new SingletonPanel(ConfigPanelPluginForHost.class, configConstructorObjects));
        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_RECONNECT, new SingletonPanel(ConfigPanelReconnect.class, configConstructorObjects));

        cfgTskPane.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                switch (e.getID()) {
                case DownloadTaskPane.ACTION_TOGGLE:

                    contentPanel.display(((TaskPanel) e.getSource()).getPanel(0));
                    break;
                case ConfigTaskPane.ACTION_SAVE:
                    boolean restart = false;

                    for (SingletonPanel panel : ((ConfigTaskPane) e.getSource()).getPanels()) {

                        if (panel.getPanel() instanceof ConfigPanel) {
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
                case ConfigTaskPane.ACTION_DECRYPT:
                case ConfigTaskPane.ACTION_DOWNLOAD:
                case ConfigTaskPane.ACTION_EVENTMANAGER:
                case ConfigTaskPane.ACTION_GENERAL:
                case ConfigTaskPane.ACTION_GUI:
                case ConfigTaskPane.ACTION_HOST:
                case ConfigTaskPane.ACTION_RECONNECT:
                    contentPanel.display(((ConfigTaskPane) e.getSource()).getPanel(e.getID()));
                    break;
                }

            }
        });

        taskPane.add(cfgTskPane);

        PremiumTaskPane premTskPane = new PremiumTaskPane(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"), JDTheme.II("gui.images.taskpanes.premium", 24, 24));

        premTskPane.addPanel(new SingletonPanel(PremiumPane.class, new Object[] {}));
        premTskPane.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!(e.getSource() instanceof JButton)) return;
                contentPanel.display(new SingletonPanel(PremiumPane.class, new Object[] { ((JButton) e.getSource()).getText() }).getPanel());
            }
        });

        taskPane.add(premTskPane);
        progressBar = new TabProgress();

        contentPanel.display(linkListPane);

        taskPane.switcher(dlTskPane);

        JPanel panel = new JPanel(new MigLayout("ins 0,wrap 2", "[fill]0[fill,grow 100]", "[grow,fill]0[]0[]"));

        setContentPane(panel);

        panel.add(taskPane);
        GeneralPurposeTaskPanel generalPurposeTasks = new GeneralPurposeTaskPanel(JDLocale.L("gui.taskpanes.generalpurpose", "Quick Config"), JDTheme.II("gui.images.taskpanes.generalpurpose", 16, 16));

        // taskPane.setBorder(BorderFactory.createLineBorder(Color.RED));
        panel.add(contentPanel, "spany 2");
        panel.add(generalPurposeTasks);
        // contentPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        panel.add(progressBar, "span,hidemode 3");

        logDialog = new LogDialog(this, logger);

    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:
                    setTitle(JDUtilities.getJDTitle());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_DOWNLOADS_AFTER_START, false)) {
                        JDUtilities.getController().startDownloads();
                    }

                    break;
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Plugin Aktiviert: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction start. ");

                        setTitle(JDUtilities.JD_TITLE + " | " + JDLocale.L("gui.titleaddaction", "Action: ") + " " + ((Interaction) event.getSource()).getInteractionName());
                    }
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.setVisible(false);
                    SimpleGUI.this.dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Plugin Deaktiviert: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction zu Ende. Rest status");
                        if (Interaction.areInteractionsInProgress()) {

                            setTitle(JDUtilities.getJDTitle());
                        }
                    }
                    break;

                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                    logger.info("ALL FINISHED");

                    if (speedmeter != null) speedmeter.stop();

                    break;
                case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
                    setTitle(JDUtilities.getJDTitle() + " - Downloads werden abgebrochen");
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                    setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
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
        // e.printStackTrace();
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

    /**
     * Displays a Captchainput Dialog
     */
    public String showCountdownCaptchaDialog(final Plugin plugin, final File captchaAddress, final String def) {

        GuiRunnable<String> run = new GuiRunnable<String>() {
            @Override
            public String runSave() {
                return new CaptchaDialog(SimpleGUI.this, plugin, captchaAddress, def).getCaptchaText();
            }
        };
        return run.getReturnValue();
    }

    public String showCountdownUserInputDialog(final String message, final String def) {

        GuiRunnable<String> run = new GuiRunnable<String>() {

            public String runSave() {
                return new InputDialog(SimpleGUI.this, message, def).getInputText();
            }

        };
        return run.getReturnValue();

    }

    // /**
    // * Die Aktionen werden initialisiert
    // */
    // public void initActions() {
    // // actionStartStopDownload = new JDAction(this,
    // // getStartStopDownloadImage(), "action.start",
    // // JDAction.APP_START_STOP_DOWNLOADS);
    // // actionPause = new JDAction(this, getPauseImage(), "action.pause",
    // // JDAction.APP_PAUSE_DOWNLOADS);
    // // actionItemsAdd = new JDAction(this, JDTheme.V("gui.images.add"),
    // // "action.add", JDAction.ITEMS_ADD);
    // // actionRemoveLinks = new JDAction(this,
    // // JDTheme.V("gui.images.delete"), "action.remove.links",
    // // JDAction.ITEMS_REMOVE_LINKS);
    // // actionRemovePackages = new JDAction(this,
    // // JDTheme.V("gui.images.delete"), "action.remove.packages",
    // // JDAction.ITEMS_REMOVE_PACKAGES);
    // actionOptionalConfig = new JDAction(this,
    // JDTheme.V("gui.images.config.packagemanager"), "action.optconfig",
    // JDAction.APP_OPEN_OPT_CONFIG);
    // actionSaveDLC = new JDAction(this, JDTheme.V("gui.images.save"),
    // "action.save", JDAction.APP_SAVE_DLC);
    // actionExit = new JDAction(this, JDTheme.V("gui.images.exit"),
    // "action.exit", JDAction.APP_EXIT);
    // actionRestart = new JDAction(this, JDTheme.V("gui.images.exit"),
    // "action.restart", JDAction.APP_RESTART);
    // actionLog = new JDAction(this, JDTheme.V("gui.images.terminal"),
    // "action.viewlog", JDAction.APP_LOG);
    // actionBackup = new JDAction(this, JDTheme.V("gui.images.save"),
    // "action.backup", JDAction.APP_BACKUP);
    // // actionClipBoard = new JDAction(this, getClipBoardImage(),
    // // "action.clipboard", JDAction.APP_CLIPBOARD);
    // // actionConfig = new JDAction(this,
    // // JDTheme.V("gui.images.configuration"), "action.configuration",
    // // JDAction.APP_CONFIGURATION);
    // // actionReconnect = new JDAction(this,
    // // JDTheme.V("gui.images.reconnect"), "action.reconnect",
    // // JDAction.APP_RECONNECT);
    // // actionUpdate = new JDAction(this,
    // // JDTheme.V("gui.images.update_manager"), "action.update",
    // // JDAction.APP_UPDATE);
    // // actionItemsDelete = new JDAction(this,
    // // JDTheme.V("gui.images.delete"), "action.edit.items_remove",
    // // JDAction.ITEMS_REMOVE);
    // // actionItemsTop = new JDAction(this, JDTheme.V("gui.images.go_top"),
    // // "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
    // // actionItemsUp = new JDAction(this, JDTheme.V("gui.images.top"),
    // // "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
    // // actionItemsDown = new JDAction(this, JDTheme.V("gui.images.down"),
    // // "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
    // // actionItemsBottom = new JDAction(this,
    // // JDTheme.V("gui.images.go_bottom"), "action.edit.items_bottom",
    // // JDAction.ITEMS_MOVE_BOTTOM);
    // // doReconnect = new JDAction(this, getDoReconnectImage(),
    // // "action.doReconnect", JDAction.APP_ALLOW_RECONNECT);
    // actionHelp = new JDAction(this, JDTheme.V("gui.images.help"),
    // "action.help", JDAction.HELP);
    // actionWiki = new JDAction(this, JDTheme.V("gui.images.help"),
    // "action.wiki", JDAction.WIKI);
    // actionAbout = new JDAction(this, JDTheme.V("gui.images.jd_logo"),
    // "action.about", JDAction.ABOUT);
    // actionChanges = new JDAction(this,
    // JDTheme.V("gui.images.update_manager"), "action.changes",
    // JDAction.CHANGES);
    // }
    //
    // /**
    // * Das Menü wird hier initialisiert
    // */
    // public void initMenuBar() {
    // JMenu menFile = new JMenu(JDLocale.L("gui.menu.file", "File"));
    // // JMenu menExtra = new JMenu(JDLocale.L("gui.menu.extra", "Extras"));
    // menAddons = OptionalMenuMenu.getMenu(JDLocale.L("gui.menu.addons",
    // "Addons"), actionOptionalConfig);
    // JMenu menHelp = new JMenu(JDLocale.L("gui.menu.plugins.help", "?"));
    // // createOptionalPluginsMenuEntries();
    // menViewLog = JDMenu.createMenuItem(actionLog);
    // createBackup = JDMenu.createMenuItem(actionBackup);
    //
    // // Adds the menus from the Addons
    //
    // // JMenu menRemove = createMenu(JDLocale.L("gui.menu.remove", "Remove"),
    // // "gui.images.delete");
    // // menRemove.add(JDMenu.createMenuItem(actionItemsDelete));
    // // menRemove.addSeparator();
    // // menRemove.add(JDMenu.createMenuItem(actionRemoveLinks));
    // // menRemove.add(JDMenu.createMenuItem(actionRemovePackages));
    // // menFile.add(menRemove);
    // // menFile.addSeparator();
    // menFile.add(JDMenu.createMenuItem(actionSaveDLC));
    // menFile.addSeparator();
    // menFile.add(JDMenu.createMenuItem(actionRestart));
    // menFile.add(JDMenu.createMenuItem(actionExit));
    //
    // // menExtra.add(JDMenu.createMenuItem(actionConfig));
    // // menExtra.addSeparator();
    //
    // menHelp.add(menViewLog);
    // menHelp.add(createBackup);
    // menHelp.addSeparator();
    // menHelp.add(JDMenu.createMenuItem(actionHelp));
    // menHelp.add(JDMenu.createMenuItem(actionWiki));
    // menHelp.addSeparator();
    // menHelp.add(JDMenu.createMenuItem(actionChanges));
    // menHelp.add(JDMenu.createMenuItem(actionAbout));
    //
    // menuBar.setLayout(new GridBagLayout());
    // Insets insets = new Insets(1, 1, 1, 1);
    //
    // int m = 0;
    // JDUtilities.addToGridBag(menuBar, menFile, m++, 0, 1, 1, 0, 0, insets,
    // GridBagConstraints.NONE, GridBagConstraints.WEST);
    // // JDUtilities.addToGridBag(menuBar, menExtra, m++, 0, 1, 1, 0, 0,
    // // insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // JDUtilities.addToGridBag(menuBar, menAddons, m++, 0, 1, 1, 0, 0, insets,
    // GridBagConstraints.NONE, GridBagConstraints.WEST);
    // JDUtilities.addToGridBag(menuBar, menHelp, m++, 0, 1, 1, 0, 0, insets,
    // GridBagConstraints.NONE, GridBagConstraints.WEST);
    // JDUtilities.addToGridBag(menuBar, new JLabel(""), m++, 0, 1, 1, 1, 0,
    // insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    //
    // warning = new JLabel("", JDTheme.II("gui.images.warning", 16, 16),
    // SwingConstants.RIGHT);
    // warning.setVisible(false);
    // warning.setIconTextGap(10);
    // warning.setHorizontalTextPosition(SwingConstants.RIGHT);
    // JDUtilities.addToGridBag(menuBar, warning, m++, 0, 1, 1, 0, 0, insets,
    // GridBagConstraints.NONE, GridBagConstraints.EAST);
    //
    // try {
    // ImageIcon icon = JDTheme.II("gui.images.update_manager", 16, 16);
    // JLinkButton linkButton = new JLinkButton(JDLocale.L("jdownloader.org",
    // "jDownloader.org"), icon, new URL(JDLocale.L("jdownloader.localnewsurl",
    // "http://jdownloader.org/news?lng=en")));
    // linkButton.setHorizontalTextPosition(SwingConstants.LEFT);
    // linkButton.setBorder(null);
    // Dimension d = new Dimension(10, 0);
    // JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1,
    // 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // JDUtilities.addToGridBag(menuBar, linkButton, m++, 0, 1, 1, 0, 0, insets,
    // GridBagConstraints.NONE, GridBagConstraints.EAST);
    // d = new Dimension(1, 0);
    // JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1,
    // 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // } catch (MalformedURLException e1) {
    // }
    //
    // frame.setJMenuBar(menuBar);
    // }

    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren TODO
     */
    private void interval() {

        // if (JDUtilities.getController() != null) {
        // statusBar.setSpeed(JDUtilities.getController().getSpeedMeter());
        // }

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

            @Override
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
            @Override
            public Boolean runSave() {
                return CountdownConfirmDialog.showCountdownConfirmDialog(SimpleGUI.this, string, sec);

            }

        }.getReturnValue();

    }

    public int showHelpMessage(final String title, String message, final boolean toHTML, final String url, final String helpMsg, final int sec) {

        final String msg = toHTML ? "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + message + "</font>" : message;

        return new GuiRunnable<Integer>() {
            @Override
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
            @Override
            public Boolean runSave() {

                return HTMLDialog.showDialog(SimpleGUI.this, title, htmlQuestion);

            }

        }.getReturnValue();

    }

    public void showMessageDialog(final String string) {
        // logger.info("MessageDialog");

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                JOptionPane.showMessageDialog(SimpleGUI.this, string);
                return null;
            }

        }.start();

    }

    public String showTextAreaDialog(final String title, final String question, final String def) {

        GuiRunnable<String> run = new GuiRunnable<String>() {
            @Override
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
            @Override
            public String runSave() {
                return JOptionPane.showInputDialog(SimpleGUI.this, string, def);
            }
        };
        return run.getReturnValue();

    }

    public String[] showTextAreaDialog(final String title, final String questionOne, final String questionTwo, final String defaultOne, final String defaultTwo) {

        GuiRunnable<String[]> run = new GuiRunnable<String[]>() {
            @Override
            public String[] runSave() {
                return TextAreaDialog.showDialog(SimpleGUI.this, title, questionOne, questionTwo, defaultOne, defaultTwo);
            }
        };
        return run.getReturnValue();

    }

    public String[] showTwoTextFieldDialog(final String title, final String questionOne, final String questionTwo, final String defaultOne, final String defaultTwo) {

        GuiRunnable<String[]> run = new GuiRunnable<String[]>() {
            @Override
            public String[] runSave() {
                return TwoTextFieldDialog.showDialog(SimpleGUI.this, title, questionOne, questionTwo, defaultOne, defaultTwo);
            }
        };
        return run.getReturnValue();

    }

    public static void showConfigDialog(Frame parent, ConfigContainer container) {
        showConfigDialog(parent, container, false);
    }

    public static void showConfigDialog(Frame parent, ConfigContainer container, boolean alwaysOnTop) {
        showConfigDialog(parent, new ConfigEntriesPanel(container), alwaysOnTop);
    }

    public static void showConfigDialog(Frame parent, ConfigPanel config, boolean alwaysOnTop) {
        // logger.info("ConfigDialog");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JPanel(), BorderLayout.NORTH);
        panel.add(config, BorderLayout.CENTER);

        ConfigurationPopup pop = new ConfigurationPopup(parent, config, panel);
        pop.setModal(true);
        pop.setAlwaysOnTop(alwaysOnTop);
        pop.setLocation(JDUtilities.getCenterOfComponent(parent, pop));
        pop.setVisible(true);
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
        new Thread() {
            public void run() {
                AccountInfo ai;
                try {
                    ai = pluginForHost.getAccountInformation(account);
                } catch (Exception e) {
                    account.setEnabled(false);
                    e.printStackTrace();
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("gui.accountcheck.pluginerror", "Plugin %s may be defect. Inform support!", pluginForHost.getPluginID()));
                    return;
                }
                if (ai == null) {
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet.", pluginForHost.getHost()));
                    return;
                }
                if (!ai.isValid()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.notValid", "The account for '%s' isn't valid! Please check username and password!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return;
                }
                if (ai.isExpired()) {
                    account.setEnabled(false);
                    SimpleGUI.this.showMessageDialog(JDLocale.LF("plugins.host.premium.info.expired", "The account for '%s' is expired! Please extend the account or buy a new one!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return;
                }
                JPanel panel = new JPanel(new MigLayout("ins 22", "[right]10[grow,fill]40"));
                String def = String.format(JDLocale.L("plugins.host.premium.info.title", "Accountinformation from %s for %s"), account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDLocale.L("plugins.host.premium.info.validUntil", "Valid until"), JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDLocale.L("plugins.host.premium.info.files", "Files"), JDLocale.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDLocale.L("plugins.host.premium.info.usedSpace", "Used Space"), JDLocale.L("plugins.host.premium.info.cash", "Cash"), JDLocale.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDLocale.L("plugins.host.premium.info.status", "Info") };

                DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String validUntil = (ai.isExpired() ? "[expired] " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                if (ai.getValidUntil() == -1) validUntil = null;
                String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                String[] data = new String[] { validUntil, JDUtilities.formatBytesToMB(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, JDUtilities.formatBytesToMB(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", JDUtilities.formatBytesToMB(ai.getTrafficShareLeft()), ai.getStatus() };
                panel.add(new JXTitledSeparator(def), "spanx, pushx, growx, gapbottom 15");
                ChartAPI_PIE freeTrafficChart = new ChartAPI_PIE("", 125, 60, panel.getBackground());
                freeTrafficChart.addEntity(new ChartAPI_Entity("Free", ai.getTrafficLeft(), new Color(50, 200, 50)));
                freeTrafficChart.addEntity(new ChartAPI_Entity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                freeTrafficChart.fetchImage();

                for (int j = 0; j < data.length; j++) {
                    if (data[j] != null && !data[j].equals("-1")) {
                        panel.add(new JLabel(label[j]), "gapleft 20");
                        if (label[j].equals(JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                            JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            panel2.add(new JTextField(data[j]));
                            panel2.add(freeTrafficChart);
                            panel.add(panel2, "wrap");
                        } else {
                            panel.add(new JTextField(data[j]), "wrap");
                        }
                    }

                }
                JOptionPane.showMessageDialog(null, panel, def, JOptionPane.INFORMATION_MESSAGE);
            }
        }.start();
    }

    public static void showChangelogDialog() {
        int status = JDUtilities.getGUI().showHelpMessage(JDLocale.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDLocale.L("system.update.message", "Update successfull"), false, "http://jdownloader.org/changes/index", JDLocale.L("system.update.showchangelogv2", "What's new?"), 60);
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, true) && status != 0) {
            try {
                JLinkButton.openURL("http://jdownloader.org/changes/index");
            } catch (Exception e) {
                e.printStackTrace();
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

        return UIManager.getLookAndFeel() instanceof SubstanceLookAndFeel;
    }

}
