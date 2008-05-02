//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.JDFileFilter;
import jd.captcha.CES;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.gui.skins.simple.config.jdUnrarPasswordListDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.CESClient;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI implements UIInterface, ActionListener, UIListener, WindowListener {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    public static final String PARAM_LOCALE = "LOCALE";

    public static final String PARAM_THEME = "THEME";

    public static final String PARAM_JAC_LOG = "JAC_DOLOG";

    public static final String PARAM_USE_EXPERT_VIEW = "USE_EXPERT_VIEW";

    public static final String PARAM_BROWSER_VARS = "BROWSER_VARS";

    public static final String PARAM_BROWSER = "BROWSER";

    public static final String PARAM_START_DOWNLOADS_AFTER_START = "START_DOWNLOADS_AFTER_START";

    public static final String GUICONFIGNAME = "simpleGUI";

    /**
     * Das Hauptfenster
     */
    private JFrame frame;
    public static SimpleGUI CURRENTGUI = null;
    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar;

    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar toolBar;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksTreeTablePanel linkListPane;;

    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabProgress progressBar;

    /**
     * TabbedPane
     */
    // private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar statusBar;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;

    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    public JButton btnStartStop;

    private JDAction actionStartStopDownload;

    // private JDAction removeFinished;

    private JDAction actionExit;

    private JDAction actionLog;

    private JDAction actionConfig;

    private JDAction actionReconnect;

    private JDAction actionUpdate;

    private JDAction actionDnD;

    // private JDAction actionItemsTop;

    // private JDAction actionItemsUp;

    // private JDAction actionItemsDown;

    // private JDAction actionItemsBottom;

    private JDAction actionItemsAdd;

    private JDAction actionItemsDelete;

    private LogDialog logDialog;

    private Logger logger = JDUtilities.getLogger();

    Dropper dragNDrop;

    private JCheckBoxMenuItem menViewLog = null;

    private JSplitPane splitpane;

    private LinkGrabber linkGrabber;

    // private JDAction actionSearch;

    private JDAction actionPause;

    private JButton btnPause;

    private JDAction actionLoadDLC;

    private JDAction actionSaveDLC;

    private JDAction actionTester;

    private JDAction actionUnrar;

    private JDAction actionClipBoard;

    private JDAction actionCes;

    private JDAction actionPasswordlist;

    private JDAction doReconnect;

    private JButton btnToggleReconnect;

    private JButton btnClipBoard;
    private JButton btnCes;
    private JDAction actionHelp;

    public static final String PARAM_DISABLE_CONFIRM_DIALOGS = "DISABLE_CONFIRM_DIALOGS";

    private static SubConfiguration guiConfig = JDUtilities.getSubConfig(GUICONFIGNAME);

    public static final String PARAM_PLAF = "PLAF";

    public static final String PARAM_SHOW_SPLASH = "SHOW_SPLASH";

    public static final String SELECTED_CONFIG_TAB = "SELECTED_CONFIG_TAB";

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
                } catch (UnsupportedLookAndFeelException e) {
                } catch (ClassNotFoundException e) {

                } catch (InstantiationException e) {

                } catch (IllegalAccessException e) {

                }
            }
        }
        if (!plafisSet) {
            try {
                UIManager.setLookAndFeel(new WindowsLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
            }
        }

        uiListener = new Vector<UIListener>();
        frame = new JFrame();
        // tabbedPane = new JTabbedPane();
        menuBar = new JMenuBar();
        toolBar = new JToolBar();

        frame.addWindowListener(this);

        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setTitle(JDUtilities.getJDTitle());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();
        frame.pack();

        // frame.getContentPane().add(desktop, BorderLayout.CENTER);
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.setName("MAINFRAME");
        if (SimpleGUI.getLastDimension(frame, null) != null) frame.setSize(getLastDimension(frame, null));
        // frame.setLocation(JDUtilities.getCenterOfComponent(null, frame));

        frame.setLocation(getLastLocation(null, null, frame));

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

                    } catch (InterruptedException e) {
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
        if (loc != null && loc instanceof Point && ((Point) loc).getX() < width && ((Point) loc).getY() < height) {
            if (((Point) loc).getX() < 0) ((Point) loc).x = 0;
            if (((Point) loc).getY() < 0) ((Point) loc).y = 0;
            return (Point) loc;
        }
        return JDUtilities.getCenterOfComponent(parent, child);
    }

    public static void saveLastLocation(Component parent, String key) {
        if (key == null) key = parent.getName();

        if (parent.isShowing()) {
            guiConfig.setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());
            guiConfig.save();
            // JDUtilities.getLogger().info("LOCATION_OF_ VOR: " +
            // "LOCATION_OF_" + key + " : " + parent.getLocationOnScreen());

        }

    }

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) key = child.getName();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = guiConfig.getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            if (((Dimension) loc).width > width) ((Dimension) loc).width = width;
            if (((Dimension) loc).height > height) ((Dimension) loc).height = height;
            return (Dimension) loc;
        }

        return null;
    }

    public static void saveLastDimension(Component child, String key) {
        if (key == null) key = child.getName();
        guiConfig.setProperty("DIMENSION_OF_" + key, child.getSize());
        guiConfig.save();
        // JDUtilities.getLogger().info("DIMEN VOR: " + "DIMENSION_OF_" + key +
        // " : " + child.getSize());

    }

    private String getClipBoardImage() {
        if (JDUtilities.getController().getClipboard().isEnabled())
            return JDTheme.V("gui.images.clipboardon");
        else
            return JDTheme.V("gui.images.clipboardoff");

    }

    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions() {
        actionStartStopDownload = new JDAction(this, JDTheme.V("gui.images.next"), "action.start", JDAction.APP_START_STOP_DOWNLOADS);
        actionPause = new JDAction(this, JDTheme.V("gui.images.stop_after"), "action.pause", JDAction.APP_PAUSE_DOWNLOADS);
        actionItemsAdd = new JDAction(this, JDTheme.V("gui.images.add"), "action.add", JDAction.ITEMS_ADD);
        actionDnD = new JDAction(this, JDTheme.V("gui.images.clipboard"), "action.dnd", JDAction.ITEMS_DND);

        actionLoadDLC = new JDAction(this, JDTheme.V("gui.images.load"), "action.load", JDAction.APP_LOAD_DLC);
        actionSaveDLC = new JDAction(this, JDTheme.V("gui.images.save"), "action.save", JDAction.APP_SAVE_DLC);

        actionExit = new JDAction(this, JDTheme.V("gui.images.exit"), "action.exit", JDAction.APP_EXIT);
        actionLog = new JDAction(this, JDTheme.V("gui.images.terminal"), "action.viewlog", JDAction.APP_LOG);
        actionTester = new JDAction(this, JDTheme.V("gui.images.jd_logo"), "action.tester", JDAction.APP_TESTER);
        actionUnrar = new JDAction(this, JDTheme.V("gui.images.jd_logo"), "action.unrar", JDAction.APP_UNRAR);
        actionClipBoard = new JDAction(this, getClipBoardImage(), "action.clipboard", JDAction.APP_CLIPBOARD);
        actionCes = new JDAction(this, CES.getCesImageString(), "action.ces", JDAction.APP_CES);
        actionPasswordlist = new JDAction(this, JDTheme.V("gui.images.jd_logo"), "action.passwordlist", JDAction.APP_PASSWORDLIST);
        actionConfig = new JDAction(this, JDTheme.V("gui.images.configuration"), "action.configuration", JDAction.APP_CONFIGURATION);
        actionReconnect = new JDAction(this, JDTheme.V("gui.images.reconnect"), "action.reconnect", JDAction.APP_RECONNECT);
        actionUpdate = new JDAction(this, JDTheme.V("gui.images.update_manager"), "action.update", JDAction.APP_UPDATE);
        // actionSearch = new JDAction(this, JDTheme.I("gui.images.find"),
        // "action.search", JDAction.APP_SEARCH);
        actionItemsDelete = new JDAction(this, JDTheme.V("gui.images.delete"), "action.edit.items_remove", JDAction.ITEMS_REMOVE);
        // actionItemsTop = new JDAction(this, JDTheme.I("gui.images.top"),
        // "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
        // actionItemsUp = new JDAction(this, JDTheme.I("gui.images.go_top"),
        // "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
        // actionItemsDown = new JDAction(this, JDTheme.I("gui.images.down"),
        // "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
        // actionItemsBottom = new JDAction(this,
        // JDTheme.I("gui.images.go_bottom"), "action.edit.items_bottom",
        // JDAction.ITEMS_MOVE_BOTTOM);
        doReconnect = new JDAction(this, JDTheme.V("gui.images.reconnect_ok"), "action.doReconnect", JDAction.ITEMS_MOVE_BOTTOM);
        actionHelp = new JDAction(this, JDTheme.V("gui.images.help"), "action.help", JDAction.HELP);

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

        // linkListPane.pluginEvent(new PluginEvent(new
        // Rapidshare(),PluginEvent.PLUGIN_DATA_CHANGED, null));
        // linkListPane.refresh();

        this.progressBar.updateController(null);

        // hostPluginDataChanged = null;
        this.frame.setTitle(JDUtilities.getJDTitle());
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
        // JMenuItem menEditItemTop = createMenuItem(actionItemsTop);
        // JMenuItem menEditItemUp = createMenuItem(actionItemsUp);
        // JMenuItem menEditItemDown = createMenuItem(actionItemsDown);
        // JMenuItem menEditItemBottom = createMenuItem(actionItemsBottom);
        JMenuItem menEditItemsDelete = createMenuItem(actionItemsDelete);
        menEdit.add(menEditItemsDelete);
        menEdit.addSeparator();
        // menEdit.add(menEditItemTop);
        // menEdit.add(menEditItemUp);
        // menEdit.add(menEditItemDown);
        // menEdit.add(menEditItemBottom);
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

        // Adds the menus form the Addons
        JMenu menAddons = new JMenu(JDLocale.L("gui.menu.addons", "Addons"));

        HashMap<String, PluginOptional> addons = JDUtilities.getPluginsOptional();
        Iterator<String> e = addons.keySet().iterator();
        JMenuItem mi;

        while (e.hasNext()) {
            final PluginOptional helpplugin = addons.get(e.next());

            if (helpplugin.createMenuitems() != null && JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + helpplugin.getPluginName(), false)) {

                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);
                m.setItems(helpplugin.createMenuitems());
                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    menAddons.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();

                            m.removeAll();
                            for (Iterator<MenuItem> it = helpplugin.createMenuitems().iterator(); it.hasNext();) {
                                m.add(SimpleGUI.getJMenuItem(it.next()));

                            }
                        }

                    });
                } else {
                    menAddons.addSeparator();
                }
            }
        }

        if (menAddons.getItemCount() == 0) {
            menAddons.setEnabled(false);
        }
        // Adds the menus form the plugins
        JMenu menPlugins = new JMenu(JDLocale.L("gui.menu.plugins", "Plugins"));
        JMenu helpHost = new JMenu(JDLocale.L("gui.menu.plugins.host", "Hoster"));
        JMenu helpDecrypt = new JMenu(JDLocale.L("gui.menu.plugins.decrypt", "Decrypter"));
        JMenu helpContainer = new JMenu(JDLocale.L("gui.menu.plugins.container", "Container"));
        menPlugins.add(helpHost);

        menPlugins.add(helpDecrypt);
        menPlugins.add(helpContainer);

        for (Iterator<PluginForHost> it = JDUtilities.getPluginsForHost().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);
                // m.setItems(helpplugin.createMenuitems());
                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpHost.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            JMenuItem c;
                            m.removeAll();
                            for (Iterator<MenuItem> it = helpplugin.createMenuitems().iterator(); it.hasNext();) {
                                c = SimpleGUI.getJMenuItem(it.next());
                                if (c == null) {
                                    m.addSeparator();
                                } else {
                                    m.add(c);
                                }

                            }

                        }

                    });
                } else {
                    helpHost.addSeparator();
                }
            }
        }
        if (helpHost.getItemCount() == 0) {
            helpHost.setEnabled(false);
        }

        for (Iterator<PluginForDecrypt> it = JDUtilities.getPluginsForDecrypt().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);

                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpDecrypt.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            m.removeAll();
                            for (Iterator<MenuItem> it = helpplugin.createMenuitems().iterator(); it.hasNext();) {
                                m.add(SimpleGUI.getJMenuItem(it.next()));

                            }

                        }

                    });
                } else {
                    helpDecrypt.addSeparator();
                }
            }
        }
        if (helpDecrypt.getItemCount() == 0) {
            helpDecrypt.setEnabled(false);
        }

        for (Iterator<PluginForContainer> it = JDUtilities.getPluginsForContainer().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);

                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpContainer.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            m.removeAll();
                            for (Iterator<MenuItem> it = helpplugin.createMenuitems().iterator(); it.hasNext();) {
                                m.add(SimpleGUI.getJMenuItem(it.next()));

                            }

                        }

                    });
                } else {
                    helpContainer.addSeparator();
                }
            }
        }
        if (helpContainer.getItemCount() == 0) {
            helpContainer.setEnabled(false);
        }

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
        menuBar.add(menAddons);
        menuBar.add(menPlugins);
        frame.setJMenuBar(menuBar);
    }

    /**
     * factory method for menu items
     * 
     * @param action
     *            action for the menu item
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
        CURRENTGUI = this;
        linkListPane = new DownloadLinksTreeTablePanel(this);
        progressBar = new TabProgress();
        statusBar = new StatusBar();
        splitpane = new JSplitPane();
        splitpane.setBottomComponent(progressBar);
        // JPanel tmp=new JPanel(new BorderLayout());
        splitpane.setTopComponent(linkListPane);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        // tmp.add(tabDownloadTable,BorderLayout.CENTER);
        // tabbedPane.addTab(JDLocale.L("gui.tab.download"),
        // tmp);
        // tabbedPane.addTab(JDLocale.L("gui.tab.plugin_activity"),
        // tabPluginActivity);
        btnStartStop = new JButton(actionStartStopDownload);
        if (JDUtilities.getImage(JDTheme.V("gui.images.stop")) != null) btnStartStop.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.stop"))));
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setText(null);
        btnPause = new JButton(actionPause);
        if (JDUtilities.getImage(JDTheme.V("gui.images.stop_after_active")) != null) btnPause.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.stop_after_active"))));
        btnPause.setFocusPainted(false);
        btnPause.setBorderPainted(false);
        btnPause.setText(null);
        btnPause.setEnabled(false);
        btnToggleReconnect = new JButton(doReconnect);
        if (JDUtilities.getImage(JDTheme.V("gui.images.reconnect_bad")) != null) btnToggleReconnect.setSelectedIcon(new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.reconnect_bad"))));
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
        btnCes = new JButton(this.actionCes);
        btnCes.setFocusPainted(false);
        btnCes.setBorderPainted(false);
        btnCes.setText(null);
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
        toolBar.add(btnCes);
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
     * @param e
     *            Die erwünschte Aktion
     */
    public void actionPerformed(ActionEvent e) {
        JDSounds.PT("sound.gui.clickToolbar");
        if (e.getSource() == btnToggleReconnect) {
            this.toggleReconnect(true);
            return;
        }
        switch (e.getID()) {
        case JDAction.ITEMS_MOVE_UP:
        case JDAction.ITEMS_MOVE_DOWN:
        case JDAction.ITEMS_MOVE_TOP:
        case JDAction.ITEMS_MOVE_BOTTOM:
            // linkListPane.moveSelectedItems(e.getID());
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
        case JDAction.APP_CES:
            logger.finer("CES");
            CES.toggleActivation();
            btnCes.setIcon(CES.getImage());
            break;
        case JDAction.APP_PASSWORDLIST:
            new jdUnrarPasswordListDialog(((SimpleGUI) JDUtilities.getController().getUiInterface()).getFrame()).setVisible(true);
            break;
        case JDAction.APP_START_STOP_DOWNLOADS:

            btnStartStop.setSelected(!btnStartStop.isSelected());
            if (!btnStartStop.isSelected()) btnStartStop.setEnabled(false);
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

                    linkListPane.removeSelectedLinks();

                    // fireUIEvent(new UIEvent(this,
                    // UIEvent.UI_UPDATED_LINKLIST, this.getDownloadLinks()));

                }
            } else {
                linkListPane.removeSelectedLinks();
                // fireUIEvent(new UIEvent(this, UIEvent.UI_UPDATED_LINKLIST,
                // this.getDownloadLinks()));
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
            LinkInputDialog inputDialog = new LinkInputDialog(frame, cb.trim());
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
                JLinkButton.openURL("http://jdownloadersupport.ath.cx");
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            break;
        case JDAction.APP_CONFIGURATION:
            ConfigurationDialog.showConfig(frame, this);

            break;
        }
    }

    public void toggleDnD() {
        if (dragNDrop.isVisible()) {
            dragNDrop.setVisible(false);
        } else {
            dragNDrop.setVisible(true);
            dragNDrop.setText("Ziehe Links auf mich!");
        }
    }

    public void toggleReconnect(boolean message) {
        btnToggleReconnect.setSelected(!btnToggleReconnect.isSelected());
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, btnToggleReconnect.isSelected());
        JDUtilities.saveConfig();

        if (btnToggleReconnect.isSelected() && message) this.showMessageDialog("Reconnect is now disabled! Do not forget to reactivate this feature!");
    }

    public void doReconnect() {
        // statusBar.setText("Interaction: HTTPReconnect");
        // dragNDrop.setText("Reconnect....");
        // frame.setTitle(JDUtilities.JD_TITLE+" |Aktion:
        // HTTPReconnect");
        boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
        if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            int confirm = JOptionPane.showConfirmDialog(frame, "Wollen Sie sicher eine neue Verbindung aufbauen?");
            if (confirm == JOptionPane.OK_OPTION) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
            }
        } else {
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);

        // statusBar.setText(null);
        // frame.setTitle(JDUtilities.JD_TITLE);
        // dragNDrop.setText("");
    }

    public void startStopDownloads() {

        if (btnStartStop.isSelected() && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            btnPause.setEnabled(true);

            fireUIEvent(new UIEvent(this, UIEvent.UI_START_DOWNLOADS));
        } else if (!btnStartStop.isSelected() && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
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

    //
    // /**
    // * Delligiert die Pluginevents weiter an das host/decryptpanel.
    // * CHangedEvents werden abgefangen und im sekundeninterval weitergegeben.
    // */
    // public void delegatedPluginEvent(PluginEvent event) {
    //     
    // if (event.getSource() instanceof PluginForHost) {
    // //linkListPane.pluginEvent(event);
    // return;
    // }
    // if (event.getSource() instanceof PluginForDecrypt) {
    // // progressBar.pluginEvent(event);
    // splitpane.setDividerLocation(0.8);
    // return;
    // }
    // if (event.getSource() instanceof PluginOptional) {
    // JDAction actionToDo = null;
    // switch (event.getEventID()) {
    // case PluginEvent.PLUGIN_CONTROL_DND:
    // actionToDo = actionDnD;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_EXIT:
    // actionToDo = actionExit;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_RECONNECT:
    // actionToDo = actionReconnect;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_SHOW_CONFIG:
    // actionToDo = actionConfig;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_SHOW_UI:
    // frame.setVisible((Boolean) event.getParameter1());
    // break;
    // case PluginEvent.PLUGIN_CONTROL_START_STOP:
    // actionToDo = actionStartStopDownload;
    // break;
    // }
    // if (actionToDo != null) actionPerformed(new ActionEvent(this,
    // actionToDo.getActionID(), ""));
    //
    // }
    // }

    private void handleProgressController(ProgressController source, Object parameter) {

        if (!this.progressBar.hasController(source) && !source.isFinished()) {
            // logger.info("addController " + source);
            progressBar.addController(source);
        } else if (source.isFinished()) {
            // logger.info("removeController " + source);
            progressBar.removeController(source);
        } else {
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
    // public Vector<DownloadLink> getDownloadLinks() {
    // if (linkListPane != null) return linkListPane.getLinks();
    // return null;
    // }

    // public void setDownloadLinks(Vector<DownloadLink> links) {
    // if (linkListPane != null) {
    // linkListPane.setDownloadLinks(links.toArray(new DownloadLink[] {}));
    // }
    // }

    // public void addDownloadLinks(Vector<DownloadLink> links) {
    // DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
    // if (linkListPane != null) {
    // linkListPane.setDownloadLinks(linkList);
    // }
    // }

    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress, String def) {
        CaptchaDialog captchaDialog = new CaptchaDialog(frame, plugin, captchaAddress, def);
        // frame.toFront();

        captchaDialog.setVisible(true);
        logger.info("Returned: " + captchaDialog.getCaptchaText());
        return captchaDialog.getCaptchaText();
    }

    // public void setPluginActive(Plugin plugin, boolean isActive) {
    // if (plugin instanceof PluginForDecrypt) {
    // statusBar.setPluginForDecryptActive(isActive);
    // }
    // else {
    // statusBar.setPluginForHostActive(isActive);
    // }
    // }

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
     * Setzt die Geschwindigkeit in der Statusbar
     * 
     * @param speed
     */
    public void setSpeedStatusBar(Integer speed) {
        statusBar.setSpinnerSpeed(speed);
    }

    /**
     * Diese Klasse realisiert eine StatusBar
     * 
     * @author astaldo
     */
    private class StatusBar extends JPanel implements ChangeListener, ControlListener {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 3676496738341246846L;

        private JLabel lblMessage;

        private JLabel lblSpeed;
        private JLabel lblSimu;
        // private JLabel lblPluginHostActive;
        //
        // private JLabel lblPluginDecryptActive;

        private ImageIcon imgActive;

        private ImageIcon imgInactive;

        protected JSpinner spMax;
        protected JSpinner spMaxDls;

        private JButton btnConfirm;

        private JCheckBox chbPremium;

        public StatusBar() {
            if (JDUtilities.getImage(JDTheme.V("gui.images.led_green")) != null) imgActive = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.led_green")));
            if (JDUtilities.getImage(JDTheme.V("gui.images.led_empty")) != null) imgInactive = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.led_empty")));
            setLayout(new GridBagLayout());
            lblMessage = new JLabel(JDLocale.L("sys.message.welcome"));
            chbPremium = new JCheckBox(JDLocale.L("gui.statusbar.premium", "Premium"));
            chbPremium.setToolTipText(JDLocale.L("gui.tooltip.statusbar.premium", "Aus/An schalten des Premiumdownloads"));
            chbPremium.addChangeListener(this);
            JDUtilities.getController().addControlListener(this);
            chbPremium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            lblSpeed = new JLabel(JDLocale.L("gui.statusbar.speed", "Geschw."));
            lblSimu = new JLabel(JDLocale.L("gui.statusbar.sim_ownloads", "Max.Dls."));
            int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

            spMax = new JSpinner();
            spMax.setModel(new SpinnerNumberModel(maxspeed, 0, Integer.MAX_VALUE, 50));
            spMax.setPreferredSize(new Dimension(60, 20));
            spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
            spMax.addChangeListener(this);

            spMaxDls = new JSpinner();
            spMaxDls.setModel(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 3), 1, 20, 1));
            spMaxDls.setPreferredSize(new Dimension(60, 20));
            spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
            spMaxDls.addChangeListener(this);

            // lblPluginHostActive = new JLabel(imgInactive);
            // lblPluginDecryptActive = new JLabel(imgInactive);
            // lblPluginDecryptActive.setToolTipText(JDLocale.L("gui.tooltip.plugin_decrypt"));
            // lblPluginHostActive.setToolTipText(JDLocale.L("gui.tooltip.plugin_host"));
            JDUtilities.addToGridBag(this, lblMessage, 0, 0, 1, 1, 1, 1, new Insets(0, 5, 0, 0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblSimu, 1, 0, 1, 1, 0, 0, new Insets(0, 2, 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, spMaxDls, 2, 0, 1, 1, 0, 0, new Insets(0, 2, 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(this, chbPremium, 3, 0, 1, 1, 0, 0, new Insets(0, 2, 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, new JSeparator(JSeparator.VERTICAL), 4, 0, 1, 1, 0, 0, new Insets(2, 2, 2, 2), GridBagConstraints.BOTH, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(this, lblSpeed, 5, 0, 1, 1, 0, 0, new Insets(0, 2, 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);

            JDUtilities.addToGridBag(this, spMax, 6, 0, 1, 1, 0, 0, new Insets(0, 2, 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);

            // JDUtilities.addToGridBag(this, lblPluginHostActive, 5, 0, 1, 1,
            // 0, 0, new Insets(0, 5, 0, 0), GridBagConstraints.NONE,
            // GridBagConstraints.EAST);
            // JDUtilities.addToGridBag(this, lblPluginDecryptActive, 6, 0, 1,
            // 1, 0, 0, new Insets(0, 0, 0, 0), GridBagConstraints.NONE,
            // GridBagConstraints.EAST);
        }

        public void setText(String text) {
            if (text == null) text = JDLocale.L("sys.message.welcome");
            lblMessage.setText(text);
        }

        /**
         * Setzt die Downloadgeschwindigkeit
         * 
         * @param speed
         *            bytes pro sekunde
         */
        public void setSpeed(Integer speed) {
            if (speed <= 0) {
                lblSpeed.setText("");
                return;
            }

            if (speed > 1024) {
                lblSpeed.setText((speed / 1024) + JDLocale.L("gui.download.kbps", "kb/s"));
            } else {
                lblSpeed.setText(speed + JDLocale.L("gui.download.bps", "bytes/s"));
            }
        }

        // /**
        // * Zeigt, ob die Plugins zum Downloaden von einem Anbieter arbeiten
        // *
        // * @param active wahr, wenn Downloads aktiv sind
        // */
        // public void setPluginForHostActive(boolean active) {
        // setPluginActive(lblPluginHostActive, active);
        // }
        //
        // /**
        // * Zeigt an, ob die Plugins zum Entschlüsseln von Links arbeiten
        // *
        // * @param active wahr, wenn soeben Links entschlüsselt werden
        // */
        // public void setPluginForDecryptActive(boolean active) {
        // setPluginActive(lblPluginDecryptActive, active);
        // }

        // /**
        // * Ändert das genutzte Bild eines Labels, um In/Aktivität anzuzeigen
        // *
        // * @param lbl Das zu ändernde Label
        // * @param active soll es als aktiv oder inaktiv gekennzeichnet werden
        // */
        // private void setPluginActive(JLabel lbl, boolean active) {
        // if (active)
        // lbl.setIcon(imgActive);
        // else
        // lbl.setIcon(imgInactive);
        // }

        public void setSpinnerSpeed(Integer speed) {
            spMax.setValue(speed);
        }

        public void stateChanged(ChangeEvent e) {
            int max = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

            if (e.getSource() == spMax) {
                int value = (Integer) spMax.getValue();

                if (max != value) {
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                    JDUtilities.getSubConfig("DOWNLOAD").save();
                }

            }
            if (e.getSource() == this.spMaxDls) {
                int value = (Integer) spMaxDls.getValue();

                if (max != value) {
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                    JDUtilities.getSubConfig("DOWNLOAD").save();
                }

            }
            if (e.getSource() == chbPremium) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) != chbPremium.isSelected()) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, chbPremium.isSelected());
                    JDUtilities.saveConfig();
                }
            }

        }

        public void controlEvent(ControlEvent event) {
            Property p;
            switch (event.getID()) {
            case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                p = (Property) event.getSource();
                if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {

                    spMax.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED));

                } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                    spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN));
                } else if (event.getParameter().equals(Configuration.PARAM_USE_GLOBAL_PREMIUM)) {
                    chbPremium.setSelected(p.getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));

                } else if (event.getParameter().equals(CESClient.UPDATE)) {
                    p = (Property) event.getSource();
                    CESClient ces = (CESClient) p.getProperty(CESClient.LASTEST_INSTANCE);
                    if (ces != null) {
                        setText(ces.getAccountInfoString());
                    }
                    
                    if(!CES.isEnabled()){
                        btnCes.setIcon(CES.getImage());
                        setText("");
                    }
                }
                break;
            }

        }
    }

    /**
     * Zeigt einen Messagedialog an
     */
    public void showMessageDialog(String string) {
        logger.info("messagedialog");
        JOptionPane.showMessageDialog(frame, string);
    }

    /**
     * Zeigt einen Confirm Dialog an
     * 
     * @param string
     */
    public boolean showConfirmDialog(String string) {
        logger.info("confirmdialog");
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
            } else {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, uiEvent.getParameter()));
            }
            break;
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    public synchronized void addLinksToGrabber(Vector<DownloadLink> links) {
        logger.info("GRAB");
        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        if (linkGrabber != null && (!linkGrabber.isDisplayable() || !linkGrabber.isVisible())) {
            logger.info("Linkgrabber should be disposed");
            linkGrabber.dispose();
            linkGrabber = null;
        }
        if (linkGrabber == null) {
            logger.info("new linkgrabber");
            linkGrabber = new LinkGrabber(this, linkList);

        } else {
            logger.info("add to grabber");
            linkGrabber.addLinks(linkList);
        }
        dragNDrop.setText("Grabbed: " + linkList.length + " (+" + ((Vector) links).size() + ")");
    }

    public String showUserInputDialog(String string) {
        logger.info("userinputdialog");
        return JOptionPane.showInputDialog(frame, string);
    }

    public String showTextAreaDialog(String title, String question, String def) {
        logger.info("Textareadialog");
        return TextAreaDialog.showDialog(this.getFrame(), title, question, def);

    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == this.getFrame()) {
            boolean doIt;
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                doIt = this.showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
            } else {
                doIt = true;
            }
            if (doIt) {
                saveLastLocation(e.getComponent(), null);
                saveLastDimension(e.getComponent(), null);
                JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
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
        logger.info("HTMLDIALOG");
        return HTMLDialog.showDialog(getFrame(), title, htmlQuestion);

    }

    public void onJDInitComplete() {
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            btnStartStop.setSelected(true);
            this.startStopDownloads();
        }
        this.frame.setTitle(JDUtilities.getJDTitle());

    }

    public int showHelpMessage(String title, String message, String url) {
        logger.info("helpmessagedialog");
        try {
            return JHelpDialog.showHelpMessage(frame, title, "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + message + "</font>", new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void restoreWindow(JFrame parent, Object object, Component component) {
        if (parent == null) parent = CURRENTGUI.getFrame();
        // JDUtilities.getLogger().info("Restore Position of "+component);
        Point point = SimpleGUI.getLastLocation(parent, null, component);
        if (point.y < 0) point.y = 0;
        if (point.x < 0) point.x = 0;
        component.setLocation(point);
        if (SimpleGUI.getLastDimension(component, null) != null) {
            Dimension dim = SimpleGUI.getLastDimension(component, null);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);

            // JDUtilities.getLogger().info("Default size:
            // "+SimpleGUI.getLastDimension(component, null));
        } else {
            // JDUtilities.getLogger().info("Default dim");
            component.validate();
        }

    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {

        case ControlEvent.CONTROL_PLUGIN_ACTIVE:
            logger.info("Plugin Aktiviert: " + event.getSource());
            // setPluginActive((PluginForDecrypt) event.getParameter(), true);
            if (event.getSource() instanceof Interaction) {
                logger.info("Interaction start. ");
                statusBar.setText("Interaction: " + ((Interaction) event.getSource()).getInteractionName());
                frame.setTitle(JDUtilities.JD_TITLE + " |Aktion: " + ((Interaction) event.getSource()).getInteractionName());

            }
            break;
        case ControlEvent.CONTROL_SYSTEM_EXIT:
            this.getFrame().setVisible(false);
            this.getFrame().dispose();
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            logger.info("Plugin Deaktiviert: " + event.getSource());
            // setPluginActive((PluginForDecrypt) event.getParameter(), false);
            if (event.getSource() instanceof Interaction) {
                logger.info("Interaction zu ende. rest status");

                statusBar.setText(null);
                frame.setTitle(JDUtilities.getJDTitle());
            }
            break;
        case ControlEvent.CONTROL_ON_PROGRESS:

            handleProgressController((ProgressController) event.getSource(), event.getParameter());

            break;

        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            // showTrayTip("Downloads", "All downloads finished");
            logger.info("ALL FINISHED");
            btnStartStop.setSelected(false);
            btnStartStop.setEnabled(true);
            btnPause.setEnabled(false);
            btnPause.setSelected(false);
            break;

        case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
            break;
        case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
            frame.setTitle(JDUtilities.getJDTitle() + " - Downloads werden abgebrochen");
            break;
        case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
            frame.setTitle(JDUtilities.getJDTitle());
            break;
        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            if (event.getSource().getClass() == JDController.class) {
            }
            break;
        case ControlEvent.CONTROL_STARTSTOP_DOWNLOAD:
            // only in this way the button state is correctly set
            // controller.startDownloads() is called by button itself so it
            // cannot handle this
            btnStartStop.setSelected(!btnStartStop.isSelected());
            if (!btnStartStop.isSelected()) btnStartStop.setEnabled(false);
            this.startStopDownloads();
            break;
        }

    }

    public void setGUIStatus(int id) {
        switch (id) {
        case UIInterface.WINDOW_STATUS_TRAYED:

            break;
        case UIInterface.WINDOW_STATUS_MAXIMIZED:
            frame.setState(JFrame.MAXIMIZED_BOTH);
            break;
        case UIInterface.WINDOW_STATUS_MINIMIZED:
            frame.setState(JFrame.ICONIFIED);
            break;

        case UIInterface.WINDOW_STATUS_NORMAL:
            frame.setState(JFrame.NORMAL);
            frame.setVisible(true);

            break;
        case UIInterface.WINDOW_STATUS_FOREGROUND:
            frame.setState(JFrame.NORMAL);
            frame.setFocusableWindowState(false);
            frame.setVisible(true);
            frame.toFront();
            frame.setFocusableWindowState(true);
            break;
        }

    }

    // public void updateStatusBar() {
    // int maxspeed =
    // JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
    // 0);
    //
    // this.statusBar.spMaxDls.setModel(new
    // SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
    // 3), 1, 20, 1));
    //
    // this.statusBar.spMax.setModel(new SpinnerNumberModel(maxspeed, 0,
    // Integer.MAX_VALUE, 50));
    //
    // this.statusBar.chbPremium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));
    //
    // return;
    // }

    public static JMenuItem getJMenuItem(final MenuItem mi) {
        switch (mi.getID()) {
        case MenuItem.SEPARATOR:
            return null;
        case MenuItem.NORMAL:
            JMenuItem m = new JMenuItem(new JDMenuAction(mi));

            return m;

        case MenuItem.TOGGLE:
            JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(new JDMenuAction(mi));
            JDUtilities.getLogger().info("SELECTED: " + mi.isSelected());
            if (mi.isSelected()) {
                m2.setIcon(JDTheme.II("gui.images.selected"));
            } else {
                m2.setIcon(JDTheme.II("gui.images.unselected"));
            }
            // m2.setIcon(JDTheme.I("gui.images.selected"));

            // m2.addActionListener(mi.getActionListener());
            // m2.setSelected(mi.isSelected());
            return m2;
        case MenuItem.CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());
            m3.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    JMenu m = (JMenu) e.getSource();
                    m.removeAll();
                    JMenuItem c;
                    for (int i = 0; i < mi.getSize(); i++) {
                        c = SimpleGUI.getJMenuItem(mi.get(i));
                        if (c == null) {
                            m.addSeparator();
                        } else {
                            m.add(c);
                        }

                    }
                }

            });

            return m3;
        }
        return null;
    }

}
