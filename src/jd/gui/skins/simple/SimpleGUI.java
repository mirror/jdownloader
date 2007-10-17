package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import jd.JDFileFilter;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI implements UIInterface, ActionListener, UIListener, WindowListener {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID         = 3966433144683787356L;

    /**
     * Das Hauptfenster
     */
    private JFrame            frame;

    /**
     * Die Menüleiste
     */
    private JMenuBar          menuBar;

    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar          toolBar;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private TabDownloadLinks  tabDownloadTable;                                ;

    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabPluginActivity tabPluginActivity;

    /**
     * TabbedPane
     */
    // private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar         statusBar;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener               = null;

    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    public JToggleButton      btnStartStop;

    private JDAction          actionStartStopDownload;

    private JDAction          actionLoadLinks;

    private JDAction          actionLoadContainer;

    private JDAction          actionSaveLinks;

    private JDAction          actionExit;

    private JDAction          actionLog;

    private JDAction          actionConfig;

    private JDAction          actionReconnect;

    private JDAction          actionUpdate;

    private JDAction          actionDnD;

    private JDAction          actionItemsTop;

    private JDAction          actionItemsUp;

    private JDAction          actionItemsDown;

    private JDAction          actionItemsBottom;

    private JDAction          actionItemsAdd;

    private JDAction          actionItemsDelete;

    private LogDialog         logDialog;

    private Logger            logger                   = Plugin.getLogger();

    Dropper                   dragNDrop;

    private JCheckBoxMenuItem menViewLog               = null;

    private JSplitPane        splitpane;

    private PluginEvent       hostPluginDataChanged    = null;

    private PluginEvent       decryptPluginDataChanged = null;

    private JCheckBox         reconnectBox;

    private LinkGrabber       linkGrabber;

    private JDAction          actionSearch;

    private JDAction          actionPause;

    private JToggleButton     btnPause;

//    private SimpleTrayIcon    tray;

    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI() {
        super();
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        }
        catch (UnsupportedLookAndFeelException e) {
        }
        uiListener = new Vector<UIListener>();
        frame = new JFrame();
        // tabbedPane = new JTabbedPane();
        menuBar = new JMenuBar();
        toolBar = new JToolBar();
        frame.addWindowListener(this);
        frame.setIconImage(JDUtilities.getImage("jd_logo"));
        frame.setTitle(getJDTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();
        frame.pack();
        frame.setLocation(JDUtilities.getCenterOfComponent(null, frame));
        frame.setVisible(true);
        // DND
        dragNDrop = new Dropper(new JFrame());
        dragNDrop.addUIListener(this);
        // Ruft jede sekunde ein UpdateEvent auf
        new Thread("GUI Interval") {
            public void run() {
                while (true) {
                    interval();
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }.start();
        loadTrayIcon();

    }

    private void loadTrayIcon() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_NO_TRAY, false)) {
            if (JDUtilities.getJavaVersion() >= 1.6d) {

//                tray = (SimpleTrayIcon) JDUtilities.getHomeDirInstance("jd/gui/skins/simple/SimpleTrayIcon.class", new Object[] { this });

            }
            else {
                logger.warning("Tray icon is supported with javaversions>=1.6. >Your Version: " + JDUtilities.getJavaVersion());
            }
        }

    }

    private String getJDTitle() {
        return JDUtilities.JD_TITLE + " " + JDUtilities.JD_VERSION + JDUtilities.getRevision() + " (" + JDUtilities.getLastChangeDate() + " " + JDUtilities.getLastChangeTime() + ")";
    }

    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions() {
        actionStartStopDownload = new JDAction(this, "start", "action.start", JDAction.APP_START_STOP_DOWNLOADS);
        actionPause = new JDAction(this, "pause", "action.pause", JDAction.APP_PAUSE_DOWNLOADS);
        actionItemsAdd = new JDAction(this, "add", "action.add", JDAction.ITEMS_ADD);
        actionDnD = new JDAction(this, "dnd", "action.dnd", JDAction.ITEMS_DND);
        actionLoadLinks = new JDAction(this, "load", "action.load", JDAction.APP_LOAD);
        actionLoadContainer = new JDAction(this, "loadContainer", "action.load_container", JDAction.APP_LOAD_CONTAINER);
        actionSaveLinks = new JDAction(this, "save", "action.save", JDAction.APP_SAVE);
        actionExit = new JDAction(this, "exit", "action.exit", JDAction.APP_EXIT);
        actionLog = new JDAction(this, "log", "action.viewlog", JDAction.APP_LOG);
        actionConfig = new JDAction(this, "configuration", "action.configuration", JDAction.APP_CONFIGURATION);
        actionReconnect = new JDAction(this, "reconnect", "action.reconnect", JDAction.APP_RECONNECT);
        actionUpdate = new JDAction(this, "update", "action.update", JDAction.APP_UPDATE);
        actionSearch = new JDAction(this, "search", "action.search", JDAction.APP_SEARCH);
        actionItemsDelete = new JDAction(this, "delete", "action.edit.items_remove", JDAction.ITEMS_REMOVE);
        actionItemsTop = new JDAction(this, "top", "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
        actionItemsUp = new JDAction(this, "up", "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
        actionItemsDown = new JDAction(this, "down", "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
        actionItemsBottom = new JDAction(this, "bottom", "action.edit.items_bottom", JDAction.ITEMS_MOVE_BOTTOM);
    }

    // Funktion wird jede Sekunde aufgerufen
    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren
     */
    private void interval() {
        if (JDUtilities.getController() != null) {
            statusBar.setSpeed(JDUtilities.getController().getSpeedMeter().getSpeed());
        }
        if (hostPluginDataChanged != null) {
            tabDownloadTable.pluginEvent(hostPluginDataChanged);
        }
        if (decryptPluginDataChanged != null) {
            tabPluginActivity.pluginEvent(decryptPluginDataChanged);
        }
        decryptPluginDataChanged = null;
        hostPluginDataChanged = null;
    }

    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar() {
        // file menu
        JMenu menFile = new JMenu(JDUtilities.getResourceString("menu.file"));
        menFile.setMnemonic(JDUtilities.getResourceChar("menu.file_mnem"));
        JMenuItem menFileLoadContainer = createMenuItem(actionLoadContainer);
        JMenuItem menFileLoad = createMenuItem(actionLoadLinks);
        JMenuItem menFileSave = createMenuItem(actionSaveLinks);
        JMenuItem menFileExit = createMenuItem(actionExit);
        // edit menu
        JMenu menEdit = new JMenu(JDUtilities.getResourceString("menu.edit"));
        menFile.setMnemonic(JDUtilities.getResourceChar("menu.edit_mnem"));
        JMenuItem menEditItemTop = createMenuItem(actionItemsTop);
        JMenuItem menEditItemUp = createMenuItem(actionItemsUp);
        JMenuItem menEditItemDown = createMenuItem(actionItemsDown);
        JMenuItem menEditItemBottom = createMenuItem(actionItemsBottom);
        JMenuItem menEditItemsDelete = createMenuItem(actionItemsDelete);
        menEdit.add(menEditItemsDelete);
        menEdit.addSeparator();
        menEdit.add(menEditItemTop);
        menEdit.add(menEditItemUp);
        menEdit.add(menEditItemDown);
        menEdit.add(menEditItemBottom);
        // action menu
        JMenu menAction = new JMenu(JDUtilities.getResourceString("menu.action"));
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.action_mnem"));
        JMenuItem menDownload = createMenuItem(actionStartStopDownload);
        JMenuItem menAddLinks = createMenuItem(actionItemsAdd);
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.action_mnem"));
        // extra
        JMenu menExtra = new JMenu(JDUtilities.getResourceString("menu.extra"));
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.extra_mnem"));
        menViewLog = new JCheckBoxMenuItem(actionLog);
        menViewLog.setIcon(null);
        if (actionLog.getAccelerator() != null) menViewLog.setAccelerator(actionLog.getAccelerator());
        JMenuItem menConfig = createMenuItem(actionConfig);
        // add menus to parents
        menFile.add(menFileLoadContainer);
        menFile.add(menFileLoad);
        menFile.add(menFileSave);
        menFile.addSeparator();
        menFile.add(menFileExit);
        menExtra.add(menViewLog);
        menExtra.add(menConfig);
        menAction.add(menDownload);
        menAction.add(menAddLinks);
        menuBar.add(menFile);
        menuBar.add(menEdit);
        menuBar.add(menAction);
        menuBar.add(menExtra);
        frame.setJMenuBar(menuBar);
    }

    /**
     * factory method for menu items
     * 
     * @param action action for the menu item
     * @return the new menu item
     */
    private static JMenuItem createMenuItem(JDAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setIcon(null);
        if (action.getAccelerator() != null) menuItem.setAccelerator(action.getAccelerator());
        return menuItem;
    }

    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI() {
        // tabbedPane = new JTabbedPane();
        tabDownloadTable = new TabDownloadLinks(this);
        tabPluginActivity = new TabPluginActivity();
        statusBar = new StatusBar();
        splitpane = new JSplitPane();
        splitpane.setBottomComponent(tabPluginActivity);
        // JPanel tmp=new JPanel(new BorderLayout());
        // tmp.add(tabPluginActivity,BorderLayout.NORTH);
        splitpane.setTopComponent(tabDownloadTable);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        // tmp.add(tabDownloadTable,BorderLayout.CENTER);
        // tabbedPane.addTab(JDUtilities.getResourceString("label.tab.download"),
        // tmp);
        // tabbedPane.addTab(JDUtilities.getResourceString("label.tab.plugin_activity"),
        // tabPluginActivity);
        btnStartStop = new JToggleButton(actionStartStopDownload);
        btnStartStop.setSelectedIcon(new ImageIcon(JDUtilities.getImage("stop")));
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setText(null);
        btnPause = new JToggleButton(actionPause);
        btnPause.setSelectedIcon(new ImageIcon(JDUtilities.getImage("pause_active")));
        btnPause.setFocusPainted(false);
        btnPause.setBorderPainted(false);
        btnPause.setText(null);
        btnPause.setEnabled(false);
        JButton btnAdd = new JButton(actionItemsAdd);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setText(null);
        JButton btnDelete = new JButton(actionItemsDelete);
        btnDelete.setFocusPainted(false);
        btnDelete.setBorderPainted(false);
        btnDelete.setText(null);
        JButton btnConfig = new JButton(this.actionConfig);
        btnConfig.setFocusPainted(false);
        btnConfig.setBorderPainted(false);
        btnConfig.setText(null);
        JButton btnReconnect = new JButton(this.actionReconnect);
        btnReconnect.setFocusPainted(false);
        btnReconnect.setBorderPainted(false);
        btnReconnect.setText(null);
        JButton btnUpdate = new JButton(this.actionUpdate);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setBorderPainted(false);
        btnUpdate.setText(null);
        JButton btnSave = new JButton(this.actionSaveLinks);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setText(null);
        JButton btnLoad = new JButton(this.actionLoadLinks);
        btnLoad.setFocusPainted(false);
        btnLoad.setBorderPainted(false);
        btnLoad.setText(null);
        JButton btnLog = new JButton(this.actionLog);
        btnLog.setFocusPainted(false);
        btnLog.setBorderPainted(false);
        btnLog.setText(null);
        JButton btnDnD = new JButton(this.actionDnD);
        btnDnD.setFocusPainted(false);
        btnDnD.setBorderPainted(false);
        btnDnD.setText(null);
        JButton btnSearch = new JButton(this.actionSearch);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setText(null);
        toolBar.setFloatable(false);
        toolBar.add(btnLoad);
        toolBar.add(btnSave);
        toolBar.addSeparator();
        toolBar.add(btnStartStop);
        toolBar.add(btnPause);
        toolBar.add(btnAdd);
        toolBar.add(btnDelete);
        toolBar.add(btnSearch);
        toolBar.addSeparator();
        toolBar.add(btnUpdate);
        toolBar.addSeparator();
        toolBar.add(btnConfig);
        toolBar.add(btnLog);
        toolBar.addSeparator();
        toolBar.add(btnReconnect);
        toolBar.add(btnDnD);
        reconnectBox = new JCheckBox("Reconnect durchführen");
        boolean rc = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
        reconnectBox.setSelected(rc);
        HTTPReconnect.setEnabled(rc);
        ExternReconnect.setEnabled(rc);
        reconnectBox.addActionListener(this);
        toolBar.add(reconnectBox);
        frame.setLayout(new GridBagLayout());
        JDUtilities.addToGridBag(frame, toolBar, 0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
        JDUtilities.addToGridBag(frame, splitpane, 0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(frame, statusBar, 0, 2, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        // Einbindung des Log Dialogs
        logDialog = new LogDialog(frame, logger);
        logDialog.addWindowListener(new LogDialogWindowAdapter());
    }

    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     * 
     * @param e Die erwünschte Aktion
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reconnectBox) {
            HTTPReconnect.setEnabled(reconnectBox.getSelectedObjects() != null);
            ExternReconnect.setEnabled(reconnectBox.getSelectedObjects() != null);
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, reconnectBox.getSelectedObjects() != null);
            fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_CONFIG));
            return;
        }
        switch (e.getID()) {
            case JDAction.ITEMS_MOVE_UP:
            case JDAction.ITEMS_MOVE_DOWN:
            case JDAction.ITEMS_MOVE_TOP:
            case JDAction.ITEMS_MOVE_BOTTOM:
                tabDownloadTable.moveSelectedItems(e.getID());
                break;
            case JDAction.APP_PAUSE_DOWNLOADS:
                fireUIEvent(new UIEvent(this, UIEvent.UI_PAUSE_DOWNLOADS, btnPause.isSelected()));
                break;
            case JDAction.APP_START_STOP_DOWNLOADS:
                this.startStopDownloads();
                break;
            case JDAction.APP_SAVE:
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new JDFileFilter(null, ".jdl", true));
                fc.showSaveDialog(frame);
                File ret = fc.getSelectedFile();
                if (ret != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_LINKS, ret));
                }
                break;
            case JDAction.APP_LOAD:
                fc = new JFileChooser();
                fc.setFileFilter(new JDFileFilter(null, "*.jdl", true));
                fc.showOpenDialog(frame);
                ret = fc.getSelectedFile();
                if (ret != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, ret));
                }
                break;
            case JDAction.APP_LOAD_CONTAINER:
                fc = new JFileChooser();
                fc.showOpenDialog(frame);
                File file = fc.getSelectedFile();
                if (file != null && file.exists()) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_CONTAINER, file));
                }
                break;
            case JDAction.APP_EXIT:
                frame.setVisible(false);
                frame.dispose();
                fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT));
                break;
            case JDAction.APP_LOG:
                logDialog.setVisible(!logDialog.isVisible());
                break;
            case JDAction.APP_RECONNECT:
                this.doReconnect();
                break;
            case JDAction.APP_UPDATE:
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_UPDATE));
                break;
            case JDAction.ITEMS_REMOVE:
                if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                    if (this.showConfirmDialog("Ausgewählte Links wirklich entfernen?")) {
                        tabDownloadTable.removeSelectedLinks();
                        fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_CHANGED, null));
                    }
                }
                else {
                    tabDownloadTable.removeSelectedLinks();
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_CHANGED, null));
                }
                break;
            case JDAction.ITEMS_DND:
                this.toggleDnD();
                break;
            case JDAction.ITEMS_ADD:
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String cb = "";
                try {
                    cb = (String) clipboard.getData(DataFlavor.stringFlavor);
                }
                catch (UnsupportedFlavorException e1) {
                }
                catch (IOException e1) {
                }
                String data = JOptionPane.showInputDialog(frame, "Bitte Link eingeben:", cb);
                if (data != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, data));
                }
                break;
            case JDAction.APP_SEARCH:
                SearchDialog s = new SearchDialog(this.getFrame());
                data = s.getText();
                logger.info(data);
                if (data != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, data));
                }
                break;
            case JDAction.APP_CONFIGURATION:
                boolean configChanged = ConfigurationDialog.showConfig(frame, this);
                if (configChanged) fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_CONFIG));
                break;
        }
    }

    public void toggleDnD() {
        if (dragNDrop.isVisible()) {
            dragNDrop.setVisible(false);
            fireUIEvent(new UIEvent(this, UIEvent.UI_SET_CLIPBOARD, false));
        }
        else {
            fireUIEvent(new UIEvent(this, UIEvent.UI_SET_CLIPBOARD, true));
            dragNDrop.setVisible(true);
            dragNDrop.setText("Ziehe Links auf mich!");
        }

    }

    public void doReconnect() {
        // statusBar.setText("Interaction: HTTPReconnect");
        // dragNDrop.setText("Reconnect....");
        // frame.setTitle(JDUtilities.JD_TITLE+" |Aktion:
        // HTTPReconnect");
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            int confirm = JOptionPane.showConfirmDialog(frame, "Wollen Sie sicher eine neue Verbindung aufbauen?");
            if (confirm == JOptionPane.OK_OPTION) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
            }
        }
        else {
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
        }
        // statusBar.setText(null);
        // frame.setTitle(JDUtilities.JD_TITLE);
        // dragNDrop.setText("");

    }

    public void startStopDownloads() {
        if (btnStartStop.isSelected() && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            btnPause.setEnabled(true);
            fireUIEvent(new UIEvent(this, UIEvent.UI_START_DOWNLOADS));
        }
        else if (!btnStartStop.isSelected() && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            final SimpleGUI _this = this;
            // Dieser Thread muss sein, weil die Funktionen zum anhalten
            // der Downloads blockiere und sonst die GUI einfriert bis
            // alle downloads angehalten wurden. Start/stop vorgänge
            // sind während dieser zeit nicht möglich da
            // getDownloadStatus() in der Ausführungszeit auf
            // JDController.DOWNLOAD_TERMINATION_IN_PROGRESS steht
            new Thread() {
                public void run() {
                    btnPause.setEnabled(false);
                    btnPause.setSelected(false);
                    fireUIEvent(new UIEvent(_this, UIEvent.UI_STOP_DOWNLOADS));
                }
            }.start();
        }

    }

    /**
     * Delligiert die Pluginevents weiter an das host/decryptpanel.
     * CHangedEvents werden abgefangen und im sekundeninterval weitergegeben.
     */
    public void delegatedPluginEvent(PluginEvent event) {
        if (event.getSource() instanceof PluginForHost && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {
            this.hostPluginDataChanged = event;
            return;
        }
        if (event.getSource() instanceof PluginForDecrypt && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {
            this.decryptPluginDataChanged = event;
            return;
        }
        if (event.getSource() instanceof PluginForSearch && event.getEventID() == PluginEvent.PLUGIN_DATA_CHANGED) {
            this.decryptPluginDataChanged = event;
            return;
        }
        if (event.getSource() instanceof PluginForHost) {
            tabDownloadTable.pluginEvent(event);
            return;
        }
        if (event.getSource() instanceof PluginForDecrypt || event.getSource() instanceof PluginForSearch) {
            tabPluginActivity.pluginEvent(event);
            splitpane.setDividerLocation(0.8);
            return;
        }
    }

    public void delegatedControlEvent(ControlEvent event) {
        switch (event.getID()) {
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE:
                logger.info("decrypt-active");
                setPluginActive((PluginForDecrypt) event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE:
                logger.info("decrypt-inactive");
                setPluginActive((PluginForDecrypt) event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE:
                logger.info("host-active");
                setPluginActive((PluginForHost) event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE:
                logger.info("host-inakcive");
                setPluginActive((PluginForHost) event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:

//                showTrayTip("Download", "" + ((DownloadLink) event.getParameter()).getStatusText() + ": " + event.getParameter());
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
//                showTrayTip("Downloads", "All downloads finished");
                btnStartStop.setSelected(false);
                btnPause.setEnabled(false);
                btnPause.setSelected(false);
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE:
                logger.info("Interaction start. ");
//                showTrayTip("Interaction", ((Interaction) event.getParameter()).getInteractionName());
                statusBar.setText("Interaction: " + ((Interaction) event.getParameter()).getInteractionName());
                frame.setTitle(JDUtilities.JD_TITLE + " |Aktion: " + ((Interaction) event.getParameter()).getInteractionName());
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                logger.info("Interaction zu ende. rest status");
//                switch (((Interaction) event.getParameter()).getCallCode()) {
//                    case Interaction.INTERACTION_CALL_ERROR:
//                        showTrayTip("Interaction", "Finished (ERROR): " + ((Interaction) event.getParameter()).getInteractionName());
//
//                        break;
//                    case Interaction.INTERACTION_CALL_RUNNING:
//                        showTrayTip("Interaction", "Finished (RUNNING): " + ((Interaction) event.getParameter()).getInteractionName());
//
//                        break;
//                    case Interaction.INTERACTION_CALL_SUCCESS:
//                        showTrayTip("Interaction", "Finished (SUCESSFULL): " + ((Interaction) event.getParameter()).getInteractionName());
//
//                        break;
//
//                }
                statusBar.setText(null);
                frame.setTitle(getJDTitle());
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
                tabDownloadTable.fireTableChanged();
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                break;
            case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
                frame.setTitle(getJDTitle() + " - Downloads werden abgebrochen");
                break;
            case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                frame.setTitle(getJDTitle());
                break;
        }
    }

//    private void showTrayTip(String header, String msg) {
//        if (tray == null) return;
//        this.tray.showTip(header, msg);
//    }

    public Vector<DownloadLink> getDownloadLinks() {
        if (tabDownloadTable != null) return tabDownloadTable.getLinks();
        return null;
    }

    public void setDownloadLinks(Vector<DownloadLink> links) {
        if (tabDownloadTable != null) {
            tabDownloadTable.setDownloadLinks(links.toArray(new DownloadLink[] {}));
        }
    }

    public void addDownloadLinks(Vector<DownloadLink> links) {
        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        if (tabDownloadTable != null) {
            tabDownloadTable.setDownloadLinks(linkList);
        }
    }

    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress) {
        CaptchaDialog captchaDialog = new CaptchaDialog(frame, plugin, captchaAddress);
        // frame.toFront();
        captchaDialog.setVisible(true);
        return captchaDialog.getCaptchaText();
    }

    public void setPluginActive(Plugin plugin, boolean isActive) {
        if (plugin instanceof PluginForDecrypt) {
            statusBar.setPluginForDecryptActive(isActive);
        }
        else {
            statusBar.setPluginForHostActive(isActive);
        }
    }

    public void addUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }

    public void removeUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }

    public void fireUIEvent(UIEvent uiEvent) {
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();
            while (recIt.hasNext()) {
                ((UIListener) recIt.next()).uiEvent(uiEvent);
            }
        }
    }

    /**
     * Toggled das MenuItem fuer die Ansicht des Log Fensters
     * 
     * @author Tom
     */
    private final class LogDialogWindowAdapter extends WindowAdapter {
        @Override
        public void windowOpened(WindowEvent e) {
            if (menViewLog != null) menViewLog.setSelected(true);
        }

        @Override
        public void windowClosed(WindowEvent e) {
            if (menViewLog != null) menViewLog.setSelected(false);
        }
    }

    /**
     * Diese Klasse realisiert eine StatusBar
     * 
     * @author astaldo
     */
    private class StatusBar extends JPanel {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 3676496738341246846L;

        private JLabel            lblMessage;

        private JLabel            lblSpeed;

        private JLabel            lblPluginHostActive;

        private JLabel            lblPluginDecryptActive;

        private ImageIcon         imgActive;

        private ImageIcon         imgInactive;

        public StatusBar() {
            imgActive = new ImageIcon(JDUtilities.getImage("led_green"));
            imgInactive = new ImageIcon(JDUtilities.getImage("led_empty"));
            setLayout(new GridBagLayout());
            lblMessage = new JLabel(JDUtilities.getResourceString("label.status.welcome"));
            lblSpeed = new JLabel();
            lblPluginHostActive = new JLabel(imgInactive);
            lblPluginDecryptActive = new JLabel(imgInactive);
            lblPluginDecryptActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_decrypt"));
            lblPluginHostActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_host"));
            JDUtilities.addToGridBag(this, lblMessage, 0, 0, 1, 1, 1, 1, new Insets(0, 5, 0, 0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblSpeed, 1, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 5), GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblPluginHostActive, 2, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 0), GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblPluginDecryptActive, 3, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 5), GridBagConstraints.NONE, GridBagConstraints.EAST);
        }

        public void setText(String text) {
            if (text == null) text = JDUtilities.getResourceString("label.status.welcome");
            lblMessage.setText(text);
        }

        /**
         * Setzt die Downloadgeschwindigkeit
         * 
         * @param speed bytes pro sekunde
         */
        public void setSpeed(Integer speed) {
            if (speed < 0) return;
            if (speed > 1024) {
                lblSpeed.setText((speed / 1024) + "kbytes/sec");
            }
            else {
                lblSpeed.setText(speed + "bytes/sec");
            }
        }

        /**
         * Zeigt, ob die Plugins zum Downloaden von einem Anbieter arbeiten
         * 
         * @param active wahr, wenn Downloads aktiv sind
         */
        public void setPluginForHostActive(boolean active) {
            setPluginActive(lblPluginHostActive, active);
        }

        /**
         * Zeigt an, ob die Plugins zum Entschlüsseln von Links arbeiten
         * 
         * @param active wahr, wenn soeben Links entschlüsselt werden
         */
        public void setPluginForDecryptActive(boolean active) {
            setPluginActive(lblPluginDecryptActive, active);
        }

        /**
         * Ändert das genutzte Bild eines Labels, um In/Aktivität anzuzeigen
         * 
         * @param lbl Das zu ändernde Label
         * @param active soll es als aktiv oder inaktiv gekennzeichnet werden
         */
        private void setPluginActive(JLabel lbl, boolean active) {
            if (active)
                lbl.setIcon(imgActive);
            else
                lbl.setIcon(imgInactive);
        }
    }

    /**
     * Zeigt einen Messagedialog an
     */
    public void showMessageDialog(String string) {
        JOptionPane.showMessageDialog(frame, string);
    }

    /**
     * Zeigt einen Confirm Dialog an
     * 
     * @param string
     */
    public boolean showConfirmDialog(String string) {
        return JOptionPane.showConfirmDialog(frame, string) == JOptionPane.OK_OPTION;
    }

    /**
     * Setzt den text im DropTargets
     * 
     * @param text
     */
    public void setDropTargetText(String text) {
        dragNDrop.setText(text);
    }

    public void uiEvent(UIEvent uiEvent) {
        switch (uiEvent.getID()) {
            case UIEvent.UI_DRAG_AND_DROP:
                fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, uiEvent.getParameter()));
                break;
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    public void addLinksToGrabber(Vector<DownloadLink> links) {
        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        if (linkGrabber != null && !linkGrabber.isVisible()) {
            linkGrabber.dispose();
            linkGrabber = null;
        }
        if (linkGrabber == null) {
            linkGrabber = new LinkGrabber(this, linkList);
            linkGrabber.setVisible(true);
        }
        else {
            linkGrabber.addLinks(linkList);
        }
        dragNDrop.setText("Grabbed: " + linkGrabber.getLinkList().size() + " (+" + ((Vector) links).size() + ")");
    }

    public String showUserInputDialog(String string) {
        return JOptionPane.showInputDialog(frame, string);
    }

    public void windowClosing(WindowEvent e) {
        fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT, null));
    }

    public void windowClosed(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {

    }

    public void windowIconified(WindowEvent e) {
//        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_NO_TRAY, false) && tray != null) {
//            frame.setVisible(false);
//            tray.showTip("Minimized", "jDownloader has been minimized to the tray. Doubleclick to show jDownloader!");
//
//        }

    }

    public void windowOpened(WindowEvent e) {}
}
