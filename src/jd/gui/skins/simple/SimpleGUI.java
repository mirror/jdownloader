package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import jd.JDFileFilter;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.gui.skins.simple.config.jdUnrarPasswordListDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.event.PluginEvent;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI implements UIInterface, ActionListener, UIListener, WindowListener {
    /**
     * serialVersionUID
     */
    private static final long       serialVersionUID                  = 3966433144683787356L;

    public static final String      PARAM_LOCALE                      = "LOCALE";

    public static final String      PARAM_THEME                       = "THEME";

    public static final String      PARAM_JAC_LOG                     = "JAC_DOLOG";

    public static final String      PARAM_USE_EXPERT_VIEW             = "USE_EXPERT_VIEW";

    public static final String      PARAM_LANG_EDITMODE               = "LANG_EDITMODE";

    public static final String      PARAM_BROWSER_VARS                = "BROWSER_VARS";

    public static final String      PARAM_BROWSER                     = "BROWSER";

    public static final String      PARAM_START_DOWNLOADS_AFTER_START = "START_DOWNLOADS_AFTER_START";

    public static final String      GUICONFIGNAME                     = "simpleGUI";

    /**
     * Das Hauptfenster
     */
    private JFrame                  frame;

    /**
     * Die Menüleiste
     */
    private JMenuBar                menuBar;

    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar                toolBar;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private TabDownloadLinks        tabDownloadTable;                                                            ;

    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabProgress             progressBar;

    /**
     * TabbedPane
     */
    // private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar               statusBar;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener>       uiListener                        = null;

    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    public JButton            btnStartStop;

    private JDAction                actionStartStopDownload;

  //  private JDAction                removeFinished;

    private JDAction                actionExit;

    private JDAction                actionLog;

    private JDAction                actionConfig;

    private JDAction                actionReconnect;

    private JDAction                actionUpdate;

    private JDAction                actionDnD;

    private JDAction                actionItemsTop;

    private JDAction                actionItemsUp;

    private JDAction                actionItemsDown;

    private JDAction                actionItemsBottom;

    private JDAction                actionItemsAdd;

    private JDAction                actionItemsDelete;

    private LogDialog               logDialog;

    private Logger                  logger                            = JDUtilities.getLogger();

    Dropper                         dragNDrop;

    private JCheckBoxMenuItem       menViewLog                        = null;

    private JSplitPane              splitpane;

    private PluginEvent             hostPluginDataChanged             = null;

    private PluginEvent             decryptPluginDataChanged          = null;

    private LinkGrabber             linkGrabber;

    // private JDAction actionSearch;

    private JDAction                actionPause;

    private JButton           btnPause;

    private JDAction                actionLoadDLC;

    private JDAction                actionSaveDLC;

    private JDAction                actionTester;

    private JDAction                actionUnrar;

    private JDAction                actionClipBoard;

    private JDAction                actionPasswordlist;

    private JDAction                doReconnect;

    private JButton           btnToggleReconnect;

    private JButton                 btnClipBoard;

    private JDAction                actionHelp;

    private LocationListener        locationListener;

    public static final String      PARAM_DISABLE_CONFIRM_DIALOGS     = "DISABLE_CONFIRM_DIALOGS";

    private static SubConfiguration guiConfig                         = JDUtilities.getSubConfig(GUICONFIGNAME);

    public static final String      PARAM_PLAF                        = "PLAF";



    public static final String PARAM_SHOW_SPLASH = "SHOW_SPLASH";

    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI() {
        super();

        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();

        boolean plafisSet = false;
        for (int i = 0; i < info.length; i++) {
            if (info[i].getName().equals(guiConfig.getStringProperty(PARAM_PLAF))) {
                try {
                    UIManager.setLookAndFeel(info[i].getClassName());
                    plafisSet = true;
                }
                catch (UnsupportedLookAndFeelException e) {
                }
                catch (ClassNotFoundException e) {

                }
                catch (InstantiationException e) {

                }
                catch (IllegalAccessException e) {

                }
            }
        }
        if (!plafisSet) {
            try {
                UIManager.setLookAndFeel(new WindowsLookAndFeel());
            }
            catch (UnsupportedLookAndFeelException e) {
            }
        }

        uiListener = new Vector<UIListener>();
        frame = new JFrame();
        // tabbedPane = new JTabbedPane();
        menuBar = new JMenuBar();
        toolBar = new JToolBar();
        this.locationListener = new LocationListener();
        frame.addWindowListener(this);

        frame.setIconImage(JDUtilities.getImage(JDTheme.I("gui.images.jd_logo")));
        frame.setTitle(JDUtilities.getJDTitle());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();
        frame.pack();

        frame.setName("MAINFRAME");
        if (SimpleGUI.getLastDimension(frame, null) != null) frame.setSize(getLastDimension(frame, null));
        // frame.setLocation(JDUtilities.getCenterOfComponent(null, frame));

        frame.setLocation(getLastLocation(null, null, frame));

        frame.setVisible(true);
        // DND
        dragNDrop = new Dropper(new JFrame());
        dragNDrop.addUIListener(this);
        frame.addComponentListener(locationListener);
        frame.addWindowListener(locationListener);
        // Ruft jede sekunde ein UpdateEvent auf
        new Thread("GUI Interval") {
            public void run() {
                while (true) {
                    interval();
                    try {
                        Thread.sleep(1000);
                        if(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_TWEAK_GUI_INTERVAL,false)){
                            Thread.sleep(2000);
                        }
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }.start();

        // enableOptionalPlugins(true);
    }

    public static Point getLastLocation(Component parent, String key, Component child) {
        if (key == null) key = child.getName();
        Object loc = guiConfig.getProperty("LOCATION_OF_" + key);
        // JDUtilities.getLogger().info("Get dim of " + "LOCATION_OF_" + key + "
        // : " + loc);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        if (loc != null && loc instanceof Point && ((Point) loc).getX() > 0 && ((Point) loc).getX() < width && ((Point) loc).getY() > 0 && ((Point) loc).getY() < height) {
            return (Point) loc;
        }
        return JDUtilities.getCenterOfComponent(parent, child);
    }

    public static void saveLastLocation(Component parent, String key) {
        if (key == null) key = parent.getName();

        if (parent.isShowing()) {
            guiConfig.setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());

        }

    }

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) key = child.getName();

        Object loc = guiConfig.getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            return (Dimension) loc;
        }

        return null;
    }

    public static void saveLastDimension(Component child, String key) {
        if (key == null) key = child.getName();
        guiConfig.setProperty("DIMENSION_OF_" + key, child.getSize());
        // JDUtilities.getLogger().info("DIMEN VOR: " + "DIMENSION_OF_" + key +
        // " : " + child.getSize());

    }

    private String getClipBoardImage() {
        if (JDUtilities.getController().getClipboard().isEnabled())
            return JDTheme.I("gui.images.clipboardon");
        else
            return JDTheme.I("gui.images.clipboardoff");

    }

    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions() {
        actionStartStopDownload = new JDAction(this, JDTheme.I("gui.images.next"), "action.start", JDAction.APP_START_STOP_DOWNLOADS);
        actionPause = new JDAction(this, JDTheme.I("gui.images.stop_after"), "action.pause", JDAction.APP_PAUSE_DOWNLOADS);
        actionItemsAdd = new JDAction(this, JDTheme.I("gui.images.add"), "action.add", JDAction.ITEMS_ADD);
        actionDnD = new JDAction(this, JDTheme.I("gui.images.clipboard"), "action.dnd", JDAction.ITEMS_DND);

        actionLoadDLC = new JDAction(this, JDTheme.I("gui.images.load"), "action.load", JDAction.APP_LOAD_DLC);
        actionSaveDLC = new JDAction(this, JDTheme.I("gui.images.save"), "action.save", JDAction.APP_SAVE_DLC);

        actionExit = new JDAction(this, JDTheme.I("gui.images.exit"), "action.exit", JDAction.APP_EXIT);
        actionLog = new JDAction(this, JDTheme.I("gui.images.terminal"), "action.viewlog", JDAction.APP_LOG);
        actionTester = new JDAction(this, JDTheme.I("gui.images.jd_logo"), "action.tester", JDAction.APP_TESTER);
        actionUnrar = new JDAction(this, JDTheme.I("gui.images.jd_logo"), "action.unrar", JDAction.APP_UNRAR);
        actionClipBoard = new JDAction(this, getClipBoardImage(), "action.clipboard", JDAction.APP_CLIPBOARD);
        actionPasswordlist = new JDAction(this, JDTheme.I("gui.images.jd_logo"), "action.passwordlist", JDAction.APP_PASSWORDLIST);
        actionConfig = new JDAction(this, JDTheme.I("gui.images.configuration"), "action.configuration", JDAction.APP_CONFIGURATION);
        actionReconnect = new JDAction(this, JDTheme.I("gui.images.reconnect"), "action.reconnect", JDAction.APP_RECONNECT);
        actionUpdate = new JDAction(this, JDTheme.I("gui.images.update_manager"), "action.update", JDAction.APP_UPDATE);
        // actionSearch = new JDAction(this, JDTheme.I("gui.images.find"),
        // "action.search", JDAction.APP_SEARCH);
        actionItemsDelete = new JDAction(this, JDTheme.I("gui.images.delete"), "action.edit.items_remove", JDAction.ITEMS_REMOVE);
        actionItemsTop = new JDAction(this, JDTheme.I("gui.images.top"), "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
        actionItemsUp = new JDAction(this, JDTheme.I("gui.images.go_top"), "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
        actionItemsDown = new JDAction(this, JDTheme.I("gui.images.down"), "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
        actionItemsBottom = new JDAction(this, JDTheme.I("gui.images.go_bottom"), "action.edit.items_bottom", JDAction.ITEMS_MOVE_BOTTOM);
        doReconnect = new JDAction(this, JDTheme.I("gui.images.reconnect_ok"), "action.doReconnect", JDAction.ITEMS_MOVE_BOTTOM);
        actionHelp = new JDAction(this, JDTheme.I("gui.images.help"), "action.help", JDAction.HELP);

    }

    // Funktion wird jede Sekunde aufgerufen
    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren
     */
    private void interval() {
        
        if (JDUtilities.getController() != null) {
            statusBar.setSpeed(JDUtilities.getController().getSpeedMeter());
        }
        if (hostPluginDataChanged != null) {
            tabDownloadTable.pluginEvent(hostPluginDataChanged);
        }
        this.progressBar.updateController(null);
        decryptPluginDataChanged = null;
        hostPluginDataChanged = null;
    }

    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar() {
        // file menu
        JMenu menFile = new JMenu(JDLocale.L("gui.menu.file"));
        menFile.setMnemonic(JDLocale.L("gui.menu.file_mnem").charAt(0));

        JMenuItem menFileLoad = createMenuItem(actionLoadDLC);
        JMenuItem menFileSave = createMenuItem(actionSaveDLC);
        JMenuItem menFileExit = createMenuItem(actionExit);
        // edit menu
        JMenu menEdit = new JMenu(JDLocale.L("gui.menu.edit"));
        menFile.setMnemonic(JDLocale.L("gui.menu.edit_mnem").charAt(0));
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
        JMenu menAction = new JMenu(JDLocale.L("gui.menu.action"));
        menAction.setMnemonic(JDLocale.L("gui.menu.action_mnem").charAt(0));
        JMenuItem menDownload = createMenuItem(actionStartStopDownload);
        JMenuItem menAddLinks = createMenuItem(actionItemsAdd);
        menAction.setMnemonic(JDLocale.L("gui.menu.action_mnem").charAt(0));
        // extra
        JMenu menExtra = new JMenu(JDLocale.L("gui.menu.extra"));
        menAction.setMnemonic(JDLocale.L("gui.menu.extra_mnem").charAt(0));
        menViewLog = new JCheckBoxMenuItem(actionLog);
        menViewLog.setIcon(null);
        if (actionLog.getAccelerator() != null) menViewLog.setAccelerator(actionLog.getAccelerator());
        JMenuItem memDnD = createMenuItem(actionDnD);
        JMenuItem menConfig = createMenuItem(actionConfig);
        JMenuItem menTester = createMenuItem(actionTester);
        JMenuItem menUnrar = createMenuItem(actionUnrar);
        JMenuItem menPasswordlist = createMenuItem(actionPasswordlist);
        JMenuItem help = createMenuItem(actionHelp);
        // add menus to parents

        menFile.add(menFileLoad);
        menFile.add(menFileSave);
        menFile.addSeparator();
        menFile.add(menFileExit);
        menExtra.add(menViewLog);
        menExtra.add(menTester);
        menExtra.add(menConfig);
        menExtra.add(memDnD);
        menExtra.add(menUnrar);
        menExtra.add(menPasswordlist);
        menExtra.add(new JSeparator());
        menExtra.add(help);
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
        progressBar = new TabProgress();
        statusBar = new StatusBar();
        splitpane = new JSplitPane();
        splitpane.setBottomComponent(progressBar);
        // JPanel tmp=new JPanel(new BorderLayout());
        // tmp.add(tabPluginActivity,BorderLayout.NORTH);
        splitpane.setTopComponent(tabDownloadTable);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        // tmp.add(tabDownloadTable,BorderLayout.CENTER);
        // tabbedPane.addTab(JDLocale.L("gui.tab.download"),
        // tmp);
        // tabbedPane.addTab(JDLocale.L("gui.tab.plugin_activity"),
        // tabPluginActivity);
        btnStartStop = new JButton(actionStartStopDownload);
        if (JDUtilities.getImage(JDTheme.I("gui.images.stop")) != null) btnStartStop.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.stop"))));
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setText(null);
        btnPause = new JButton(actionPause);
        if (JDUtilities.getImage(JDTheme.I("gui.images.stop_after_active")) != null) btnPause.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.stop_after_active"))));
        btnPause.setFocusPainted(false);
        btnPause.setBorderPainted(false);
        btnPause.setText(null);
        btnPause.setEnabled(false);
        btnToggleReconnect = new JButton(doReconnect);
        if (JDUtilities.getImage(JDTheme.I("gui.images.reconnect_bad")) != null) btnToggleReconnect.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.reconnect_bad"))));
        btnToggleReconnect.setFocusPainted(false);
        btnToggleReconnect.setBorderPainted(false);
        btnToggleReconnect.setText(null);
        btnToggleReconnect.setEnabled(true);
        btnToggleReconnect.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));

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
        JButton btnSave = new JButton(this.actionSaveDLC);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setText(null);
        JButton btnLoad = new JButton(this.actionLoadDLC);
        btnLoad.setFocusPainted(false);
        btnLoad.setBorderPainted(false);
        btnLoad.setText(null);
        JButton btnLog = new JButton(this.actionLog);
        btnLog.setFocusPainted(false);
        btnLog.setBorderPainted(false);
        btnLog.setText(null);
        btnClipBoard = new JButton(this.actionClipBoard);
        btnClipBoard.setFocusPainted(false);
        btnClipBoard.setBorderPainted(false);
        btnClipBoard.setText(null);
        // JButton btnSearch = new JButton(this.actionSearch);
        // btnSearch.setFocusPainted(false);
        // btnSearch.setBorderPainted(false);
        // btnSearch.setText(null);
        JButton btnHelp = new JButton(this.actionHelp);
        btnHelp.setFocusPainted(false);
        btnHelp.setBorderPainted(false);
        btnHelp.setText(null);

        toolBar.setFloatable(false);
        toolBar.add(btnLoad);
        toolBar.add(btnSave);
        toolBar.addSeparator();
        toolBar.add(btnStartStop);
        toolBar.add(btnPause);
        toolBar.add(btnAdd);
        toolBar.add(btnDelete);
        // toolBar.add(btnSearch);
        toolBar.addSeparator();
        toolBar.add(btnUpdate);
        toolBar.addSeparator();
        toolBar.add(btnConfig);
        toolBar.add(btnLog);
        toolBar.addSeparator();
        toolBar.add(btnReconnect);
        toolBar.add(btnClipBoard);
        toolBar.add(btnToggleReconnect);
        toolBar.add(btnHelp);
        // reconnectBox = new JCheckBox("Reconnect durchführen");
        // boolean rc =
        // JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT,
        // true);
        //
        // reconnectBox.addActionListener(this);
        // toolBar.add(reconnectBox);
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
        if (e.getSource() == btnToggleReconnect) {
            btnToggleReconnect.setSelected(!btnToggleReconnect.isSelected());
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, btnToggleReconnect.isSelected());

            fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_CONFIG));
            if (btnToggleReconnect.isSelected()) this.showMessageDialog("Reconnect is now disabled! Do not forget to reactivate this feature!");
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
                this.btnPause.setSelected(!btnPause.isSelected());
                fireUIEvent(new UIEvent(this, UIEvent.UI_PAUSE_DOWNLOADS, btnPause.isSelected()));
                break;
            case JDAction.APP_TESTER:
                logger.finer("Test trigger pressed");
                Interaction.handleInteraction(Interaction.INTERACTION_TESTTRIGGER, false);
                break;
            case JDAction.APP_UNRAR:
                logger.finer("Unrar");
                JDUtilities.getController().getUnrarModule().interact(null);
                break;
            case JDAction.APP_CLIPBOARD:
                logger.finer("Clipboard");
                JDUtilities.getController().getClipboard().toggleActivation();
                btnClipBoard.setIcon(new ImageIcon(JDUtilities.getImage(getClipBoardImage())));
                break;
            case JDAction.APP_PASSWORDLIST:
                new jdUnrarPasswordListDialog(((SimpleGUI) JDUtilities.getController().getUiInterface()).getFrame()).setVisible(true);
                break;
            case JDAction.APP_START_STOP_DOWNLOADS:
                btnStartStop.setSelected(!btnStartStop.isSelected());
                this.startStopDownloads();
                
                break;
            case JDAction.APP_SAVE_DLC:
                JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
                fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
                fc.showSaveDialog(frame);
                File ret = fc.getSelectedFile();
                if (ret == null) return;
                if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                    ret = new File(ret.getAbsolutePath() + ".dlc");
                }
                if (ret != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_LINKS, ret));
                }
                break;
            case JDAction.APP_LOAD_DLC:
                fc = new JDFileChooser("_LOADSAVEDLC");
                fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
                fc.showOpenDialog(frame);
                ret = fc.getSelectedFile();
                if (ret != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, ret));
                }
                break;

            case JDAction.APP_EXIT:
                frame.setVisible(false);
                frame.dispose();
                fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT));
                break;
            case JDAction.APP_LOG:
                logDialog.setVisible(!logDialog.isVisible());
                menViewLog.setSelected(!logDialog.isVisible());
                break;
            case JDAction.APP_RECONNECT:
                this.doReconnect();
                break;
            case JDAction.APP_UPDATE:
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_UPDATE));
                break;
            case JDAction.ITEMS_REMOVE:
                if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                    if (this.showConfirmDialog("Ausgewählte Links wirklich entfernen?")) {

                        tabDownloadTable.removeSelectedLinks();

                        fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_CHANGED, this.getDownloadLinks()));

                    }
                }
                else {
                    tabDownloadTable.removeSelectedLinks();
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_CHANGED, this.getDownloadLinks()));
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
                } catch (UnsupportedFlavorException e1) {
                } catch (IOException e1) {
                }
                LinkInputDialog inputDialog = new LinkInputDialog(frame, cb);
                String data = inputDialog.getLinksString();
                if (data != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, data));
                }
                break;
            // case JDAction.APP_SEARCH:
            // SearchDialog s = new SearchDialog(this.getFrame());
            // data = s.getText();
            // if (!data.endsWith(":::")) {
            // logger.info(data);
            // if (data != null) {
            // fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS,
            // data));
            // }
            // }
            // break;
            //                
            case JDAction.HELP:
                try {
                    JLinkButton.OpenURL("http://jdownloadersupport.ath.cx");
                }
                catch (Exception e1) {
                    e1.printStackTrace();
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
        }
        else {
            dragNDrop.setVisible(true);
            dragNDrop.setText("Ziehe Links auf mich!");
        }
    }

    public void doReconnect() {
        // statusBar.setText("Interaction: HTTPReconnect");
        // dragNDrop.setText("Reconnect....");
        // frame.setTitle(JDUtilities.JD_TITLE+" |Aktion:
        // HTTPReconnect");
        if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
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
        if (event.getSource() instanceof PluginForHost) {
            tabDownloadTable.pluginEvent(event);
            return;
        }
        if (event.getSource() instanceof PluginForDecrypt) {
            // progressBar.pluginEvent(event);
            splitpane.setDividerLocation(0.8);
            return;
        }
        if (event.getSource() instanceof PluginOptional) {
            JDAction actionToDo = null;
            switch (event.getEventID()) {
                case PluginEvent.PLUGIN_CONTROL_DND:
                    actionToDo = actionDnD;
                    break;
                case PluginEvent.PLUGIN_CONTROL_EXIT:
                    actionToDo = actionExit;
                    break;
                case PluginEvent.PLUGIN_CONTROL_RECONNECT:
                    actionToDo = actionReconnect;
                    break;
                case PluginEvent.PLUGIN_CONTROL_SHOW_CONFIG:
                    actionToDo = actionConfig;
                    break;
                case PluginEvent.PLUGIN_CONTROL_SHOW_UI:
                    frame.setVisible((Boolean) event.getParameter1());
                    break;
                case PluginEvent.PLUGIN_CONTROL_START_STOP:
                    actionToDo = actionStartStopDownload;
                    break;
            }
            if (actionToDo != null) actionPerformed(new ActionEvent(this, actionToDo.getActionID(), ""));

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
            case ControlEvent.CONTROL_ON_PROGRESS:

                handleProgressController((ProgressController) event.getSource(), event.getParameter());

                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE:
                logger.info("host-active");
                setPluginActive((PluginForHost) event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE:
                logger.info("host-inactive");
                setPluginActive((PluginForHost) event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                // showTrayTip("Download", "" + ((DownloadLink)
                // event.getParameter()).getStatusText() + ": " +
                // event.getParameter());
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                // showTrayTip("Downloads", "All downloads finished");
                btnStartStop.setSelected(false);
                btnPause.setEnabled(false);
                btnPause.setSelected(false);
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE:
                logger.info("Interaction start. ");
                // showTrayTip("Interaction", ((Interaction)
                // event.getParameter()).getInteractionName());
                statusBar.setText("Interaction: " + ((Interaction) event.getParameter()).getInteractionName());
                frame.setTitle(JDUtilities.JD_TITLE + " |Aktion: " + ((Interaction) event.getParameter()).getInteractionName());
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                logger.info("Interaction zu ende. rest status");
                // switch (((Interaction) event.getParameter()).getCallCode()) {
                // case Interaction.INTERACTION_CALL_ERROR:
                // showTrayTip("Interaction", "Finished (ERROR): " +
                // ((Interaction) event.getParameter()).getInteractionName());
                //
                // break;
                // case Interaction.INTERACTION_CALL_RUNNING:
                // showTrayTip("Interaction", "Finished (RUNNING): " +
                // ((Interaction) event.getParameter()).getInteractionName());
                //
                // break;
                // case Interaction.INTERACTION_CALL_SUCCESS:
                // showTrayTip("Interaction", "Finished (SUCESSFULL): " +
                // ((Interaction) event.getParameter()).getInteractionName());
                //
                // break;
                //
                // }
                statusBar.setText(null);
                frame.setTitle(JDUtilities.getJDTitle());
                break;
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:

                tabDownloadTable.fireTableChanged();
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                break;
            case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
                frame.setTitle(JDUtilities.getJDTitle() + " - Downloads werden abgebrochen");
                break;
            case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                frame.setTitle(JDUtilities.getJDTitle());
                break;
        }
    }

    private void handleProgressController(ProgressController source, Object parameter) {

        if (!this.progressBar.hasController(source) && !source.isFinished()) {
            // logger.info("addController " + source);
            progressBar.addController(source);
        }
        else if (source.isFinished()) {
            // logger.info("removeController " + source);
            progressBar.removeController(source);
        }
        else {
            // logger.info("updateController: " + source);
            progressBar.updateController(source);
        }

        if (progressBar.getControllers().size() > 0) {
            splitpane.setDividerLocation(0.8);

        }
    }

    // private void showTrayTip(String header, String msg) {
    // if (tray == null) return;
    // this.tray.showTip(header, msg);
    // }
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
            if (JDUtilities.getImage(JDTheme.I("gui.images.led_green")) != null) imgActive = new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.led_green")));
            if (JDUtilities.getImage(JDTheme.I("gui.images.led_empty")) != null) imgInactive = new ImageIcon(JDUtilities.getImage(JDTheme.I("gui.images.led_empty")));
            setLayout(new GridBagLayout());
            lblMessage = new JLabel(JDLocale.L("sys.message.welcome"));
            lblSpeed = new JLabel();
            lblPluginHostActive = new JLabel(imgInactive);
            lblPluginDecryptActive = new JLabel(imgInactive);
            lblPluginDecryptActive.setToolTipText(JDLocale.L("gui.tooltip.plugin_decrypt"));
            lblPluginHostActive.setToolTipText(JDLocale.L("gui.tooltip.plugin_host"));
            JDUtilities.addToGridBag(this, lblMessage, 0, 0, 1, 1, 1, 1, new Insets(0, 5, 0, 0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblSpeed, 1, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 5), GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblPluginHostActive, 2, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 0), GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblPluginDecryptActive, 3, 0, 1, 1, 0, 0, new Insets(0, 5, 0, 5), GridBagConstraints.NONE, GridBagConstraints.EAST);
        }

        public void setText(String text) {
            if (text == null) text = JDLocale.L("sys.message.welcome");
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
                lblSpeed.setText((speed / 1024) + JDLocale.L("gui.download.kbps", "kbytes/sek"));
            }
            else {
                lblSpeed.setText(speed + JDLocale.L("gui.download.bps", "bytes/sek"));
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
                if (uiEvent.getParameter() instanceof String) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, uiEvent.getParameter()));
                }
                else {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, uiEvent.getParameter()));
                }
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

        }
        else {
            linkGrabber.addLinks(linkList);
        }
        dragNDrop.setText("Grabbed: " + linkList.length + " (+" + ((Vector) links).size() + ")");
    }

    public String showUserInputDialog(String string) {
        return JOptionPane.showInputDialog(frame, string);
    }

    public String showTextAreaDialog(String title, String question, String def) {
        return TextAreaDialog.showDialog(this.getFrame(), title, question, def);

    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == this.getFrame()) {
            boolean doIt;
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                doIt = this.showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
            }
            else {
                doIt = true;
            }
            if (doIt) {
                this.getFrame().setVisible(false);
                this.getFrame().dispose();
                this.fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT, null));
            }
        }
    }

    public void windowActivated(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public void windowClosed(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public void windowDeactivated(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public void windowDeiconified(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public void windowIconified(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public void windowOpened(WindowEvent e) {
    // TODO Auto-generated method stub

    }

    public boolean showHTMLDialog(String title, String htmlQuestion) {
        return HTMLDialog.showDialog(getFrame(), title, htmlQuestion);

    }

    public void onJDInitComplete() {
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            btnStartStop.setSelected(true);
            this.startStopDownloads();
        }

    }



}
