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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.HostPluginWrapper;
import jd.JDInit;
import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.components.TwoTextFieldDialog;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.gui.skins.simple.config.ConfigPanelAddons;
import jd.gui.skins.simple.config.ConfigPanelPluginForHost;

import jd.gui.skins.simple.config.ConfigPanelReconnect;

import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
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

public class SimpleGUI implements UIInterface, ActionListener, UIListener, WindowListener, DropTargetListener {

    public static String WAITING_USER_IO = JDLocale.L("gui.linkgrabber.waitinguserio", "Waiting for user input");

    /**
     * Toggled das MenuItem fuer die Ansicht des Log Fensters
     * 
     * @author Tom
     */
    private final class LogDialogWindowAdapter extends WindowAdapter {

        public void windowClosed(WindowEvent e) {
            if (menViewLog != null) {
                menViewLog.setSelected(false);
            }
        }

        public void windowOpened(WindowEvent e) {
            if (menViewLog != null) {
                menViewLog.setSelected(true);
            }
        }
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

        private JCheckBox chbPremium;

        private JLabel lblMessage;

        private JLabel lblSimu;

        private JLabel lblSpeed;

        protected JSpinner spMax;

        protected JSpinner spMaxDls;

        public StatusBar() {
            setLayout(new BorderLayout());

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JPanel panel = new JPanel(new BorderLayout(0, 0));
            add(panel, BorderLayout.WEST);
            add(right, BorderLayout.EAST);

            // TODO: Please replace with proper Icon that catches the users eye.
            // Icon could even change in case of a warning or error.
            // gruener Haken - everything ok
            // oranges Warnschild - ohoh
            // roter Kreis - we are roally f#$%cked!
            ImageIcon statusIcon = JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.jd_logo"), 16, -1);

            lblMessage = new JLabel(JDLocale.L("sys.message.welcome", "Welcome to JDownloader"));
            lblMessage.setIcon(statusIcon);
            statusBarHandler = new LabelHandler(lblMessage, JDLocale.L("sys.message.welcome", "Welcome to JDownloader"));

            chbPremium = new JCheckBox(JDLocale.L("gui.statusbar.premium", "Premium"));
            chbPremium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            chbPremium.setToolTipText(JDLocale.L("gui.tooltip.statusbar.premium", "Aus/An schalten des Premiumdownloads"));
            chbPremium.addChangeListener(this);
            JDUtilities.getController().addControlListener(this);
            lblSpeed = new JLabel(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
            lblSimu = new JLabel(JDLocale.L("gui.statusbar.sim_ownloads", "Max.Dls."));

            spMax = new JSpinner();
            spMax.setModel(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0), 0, Integer.MAX_VALUE, 50));
            spMax.setPreferredSize(new Dimension(60, 20));
            spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
            spMax.addChangeListener(this);
            colorizeSpinnerSpeed();

            spMaxDls = new JSpinner();
            spMaxDls.setModel(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
            spMaxDls.setPreferredSize(new Dimension(60, 20));
            spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
            spMaxDls.addChangeListener(this);

            panel.add(lblMessage);
            right.add(chbPremium);
            addItem(true, right, bundle(lblSimu, spMaxDls));
            addItem(true, right, bundle(lblSpeed, spMax));
        }

        void addItem(boolean seperator, JComponent where, Component component) {
            int n = 10;
            Dimension d = new Dimension(n, 0);
            JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
            separator.setPreferredSize(new Dimension(n, n));
            where.add(new Box.Filler(d, d, d));
            if (seperator) where.add(separator);
            where.add(component);
        }

        private Component bundle(Component c1, Component c2) {
            JPanel panel = new JPanel(new BorderLayout(2, 0));
            panel.add(c1, BorderLayout.WEST);
            panel.add(c2, BorderLayout.EAST);
            return panel;
        }

        private void colorizeSpinnerSpeed() {
            /* färbt den spinner ein, falls speedbegrenzung aktiv */
            JSpinner.DefaultEditor spMaxEditor = (JSpinner.DefaultEditor) spMax.getEditor();
            if ((Integer) spMax.getValue() > 0) {
                lblSpeed.setForeground(JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03"));
                spMaxEditor.getTextField().setForeground(Color.red);
            } else {
                lblSpeed.setForeground(Color.black);
                spMaxEditor.getTextField().setForeground(Color.black);
            }
        }

        public void controlEvent(ControlEvent event) {
            if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
                Property p = (Property) event.getSource();
                if (spMax != null && event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                    setSpinnerSpeed(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
                } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                    spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
                } else if (p == JDUtilities.getConfiguration() && event.getParameter().equals(Configuration.PARAM_USE_GLOBAL_PREMIUM)) {
                    chbPremium.setSelected(p.getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
                } else if (event.getID() == ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED) {
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                }
            }
        }

        /**
         * Setzt die Downloadgeschwindigkeit
         * 
         * @param speed
         *            bytes pro sekunde
         */
        public void setSpeed(int speed) {
            if (speed <= 0) {
                lblSpeed.setText(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
            } else {
                lblSpeed.setText("(" + JDUtilities.formatKbReadable(speed / 1024) + "/s)");
            }
        }

        public void setSpinnerSpeed(Integer speed) {
            spMax.setValue(speed);
            colorizeSpinnerSpeed();
        }

        public void stateChanged(ChangeEvent e) {

            if (e.getSource() == spMax) {
                colorizeSpinnerSpeed();
                SubConfiguration subConfig = JDUtilities.getSubConfig("DOWNLOAD");
                subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, (Integer) spMax.getValue());
                subConfig.save();

            } else if (e.getSource() == spMaxDls) {
                SubConfiguration subConfig = JDUtilities.getSubConfig("DOWNLOAD");
                subConfig.setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, (Integer) spMaxDls.getValue());
                subConfig.save();

            } else if (e.getSource() == chbPremium) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) != chbPremium.isSelected()) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, chbPremium.isSelected());
                    JDUtilities.getConfiguration().save();
                }
            }
        }
    }

    public static SimpleGUI CURRENTGUI = null;

    public static final String GUICONFIGNAME = "simpleGUI";

    private transient static SubConfiguration guiConfig = null;

    public static final String PARAM_BROWSER = "BROWSER";

    public static final String PARAM_BROWSER_VARS = "BROWSER_VARS";

    public static final String PARAM_DISABLE_CONFIRM_DIALOGS = "DISABLE_CONFIRM_DIALOGS";

    public static final String PARAM_DISABLE_TREETABLE_TOOLTIPPS = "DISABLE_TREETABLE_TOOLTIPPS";

    public static final String PARAM_JAC_LOG = "JAC_DOLOG";

    public static final String PARAM_SHOW_SPLASH = "SHOW_SPLASH";

    public static final String PARAM_SHOW_FENGSHUI = "SHOW_FENGSHUI";

    public static final String PARAM_START_DOWNLOADS_AFTER_START = "START_DOWNLOADS_AFTER_START";

    public static final String PARAM_THEME = "THEME";

    public static final String PARAM_DCLICKPACKAGE = "PARAM_DCLICKPACKAGE";

    public static final String PARAM_INPUTTIMEOUT = "PARAM_INPUTTIMEOUT";

    public static final String SELECTED_CONFIG_TAB = "SELECTED_CONFIG_TAB";

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    public static final String PARAM_CUSTOM_BROWSER = "CUSTOM_ROWSER";

    public static final String PARAM_CUSTOM_BROWSER_USE = "PARAM_CUSTOM_ROWSER_USE";

    public static final String PARAM_CUSTOM_BROWSER_PARAM = "PARAM_CUSTOM_ROWSER_PARAM";

    public static final String PARAM_NUM_PREMIUM_CONFIG_FIELDS = "PARAM_NUM_PREMIUM_CONFIG_FIELDS";

    public static final String PARAM_SHOW_SPEEDMETER = "PARAM_SHOW_SPEEDMETER";

    public static final String PARAM_SHOW_SPEEDMETER_WINDOWSIZE = "PARAM_SHOW_SPEEDMETER_WINDOWSIZE";

    /**
     * factory method for menu items
     * 
     * @param action
     *            action for the menu item
     * @return the new menu item
     */
    private static JMenuItem createMenuItem(JDAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        if (menuItem.getIcon() instanceof ImageIcon) {
            ImageIcon icon = (ImageIcon) menuItem.getIcon();
            menuItem.setIcon(JDUtilities.getScaledImageIcon(icon, 16, -1));
        }
        if (action.getAccelerator() != null) {
            menuItem.setAccelerator(action.getAccelerator());
        }
        return menuItem;
    }

    private static JMenu createMenu(String iconname, String ressourceName) {
        JMenu menu = new JMenu(iconname);
        ImageIcon icon = JDUtilities.getScaledImageIcon(JDTheme.V(ressourceName), 16, -1);
        if (icon != null) {
            menu.setIcon(icon);
        }
        return menu;
    }

    public static JMenuItem getJMenuItem(final MenuItem mi) {
        JMenuItem m;
        switch (mi.getID()) {
        case MenuItem.SEPARATOR:
            return null;
        case MenuItem.NORMAL:
            m = new JMenuItem(new JDMenuAction(mi));
            if (mi.getAccelerator() != null) m.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m;
        case MenuItem.TOGGLE:
            JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(new JDMenuAction(mi));
            if (mi.isSelected()) {
                m2.setIcon(JDTheme.II("gui.images.selected"));
            } else {
                m2.setIcon(JDTheme.II("gui.images.unselected"));
            }
            if (mi.getAccelerator() != null) m2.setAccelerator(KeyStroke.getKeyStroke(mi.getAccelerator()));
            return m2;
        case MenuItem.CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());
            JMenuItem c;
            if (mi.getSize() > 0) for (int i = 0; i < mi.getSize(); i++) {
                c = SimpleGUI.getJMenuItem(mi.get(i));
                if (c == null) {
                    m3.addSeparator();
                } else {
                    m3.add(c);
                }
            }
            m3.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    JMenu m = (JMenu) e.getSource();
                    m.removeAll();
                    JMenuItem c;
                    if (mi.getSize() == 0) m.setEnabled(false);
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

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) {
            key = child.getName();
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = guiConfig.getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            Dimension dim = (Dimension) loc;
            if (dim.width > width) dim.width = width;
            if (dim.height > height) dim.height = height;

            return dim;
        }

        return null;
    }

    public static Point getLastLocation(Component parent, String key, Component child) {
        if (key == null) {
            key = child.getName();
        }
        Object loc = guiConfig.getProperty("LOCATION_OF_" + key);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        if (loc != null && loc instanceof Point) {
            Point point = (Point) loc;
            if (point.x < 0) point.x = 0;
            if (point.y < 0) point.y = 0;
            if (point.x > width) point.x = width;
            if (point.y > height) point.y = height;

            return point;
        }
        return JDUtilities.getCenterOfComponent(parent, child);
    }

    public static void restoreWindow(JFrame parent, Component component) {
        if (parent == null) {
            parent = CURRENTGUI.getFrame();
        }

        component.setLocation(SimpleGUI.getLastLocation(parent, null, component));
        Dimension dim = SimpleGUI.getLastDimension(component, null);
        if (dim != null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);
            if (component instanceof JFrame) ((JFrame) component).setExtendedState(guiConfig.getIntegerProperty("MAXIMIZED_STATE_OF_" + component.getName(), JFrame.NORMAL));
        } else {
            component.validate();
        }

    }

    public static void saveLastDimension(Component child, String key) {
        if (key == null) {
            key = child.getName();
        }
        if (guiConfig == null) return;
        guiConfig.setProperty("DIMENSION_OF_" + key, child.getSize());
        if (child instanceof JFrame) guiConfig.setProperty("MAXIMIZED_STATE_OF_" + key, ((JFrame) child).getExtendedState());
        guiConfig.save();
    }

    public static void saveLastLocation(Component parent, String key) {
        if (guiConfig == null) return;
        if (key == null) {
            key = parent.getName();
        }

        if (parent.isShowing()) {
            guiConfig.setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());
            guiConfig.save();
        }

    }

    private JDAction actionAbout;

    private JDAction actionClipBoard;

    // private JDAction actionConfig;

    private JDAction actionDnD;

    private JDAction actionExit;

    private JDAction actionRestart;

    private JDAction actionHelp;

    private JDAction actionOptionalConfig;

    private JDAction actionHostConfig;

    private JDAction actionItemsAdd;

    private JDAction actionItemsBottom;

    private JDAction actionItemsDelete;

    private JDAction actionItemsDown;

    private JDAction actionItemsTop;

    private JDAction actionItemsUp;

    private JDAction actionAddLinks;

    private JDAction actionRemoveLinks;

    private JDAction actionRemovePackages;

    private JDAction actionLoadDLC;

    private JDAction actionLog;

    private JDAction actionBackup;

    private JDAction actionPause;

    private JDAction actionReconnect;

    private JDAction actionSaveDLC;

    private JDAction actionStartStopDownload;

    private JDAction actionUpdate;

    private JDAction actionWiki;

    private JDAction actionChanges;

    private JButton btnClipBoard;

    private JButton btnPause;

    private JButton btnReconnect;

    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    public JButton btnStartStop;

    private JDAction doReconnect;

    Dropper dragNDrop;

    /**
     * Das Hauptfenster
     */
    private JFrame frame;

    private LinkGrabber linkGrabber;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksTreeTablePanel linkListPane;

    private LogDialog logDialog;

    public LogDialog getLogDialog() {
        return logDialog;
    }

    public void setLogDialog(LogDialog logDialog) {
        this.logDialog = logDialog;
    }

    private Logger logger = JDUtilities.getLogger();

    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar;

    private JMenu menAddons;

    private JMenu menHosts;

    private JMenuItem menViewLog = null;

    private JMenuItem createBackup = null;

    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabProgress progressBar;

    private JSplitPane horizontalSplitPane;

    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar statusBar;

    public LabelHandler statusBarHandler;

    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar toolBar;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;

    private JLabel warning;

    private Thread warningWorker;

    private SpeedMeterPanel speedmeter;

    private TabbedPane taskPane;



    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI() {
        super();
        guiConfig = JDUtilities.getSubConfig(GUICONFIGNAME);
        JDLookAndFeelManager.setUIManager();
        uiListener = new Vector<UIListener>();
        frame = new JFrame();
        menuBar = new JMenuBar();
        toolBar = new JToolBar();

        frame.addWindowListener(this);
        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setTitle(JDUtilities.getJDTitle());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        initActions();
        initMenuBar();
        localize();
        buildUI();

        frame.setName("MAINFRAME");
        Dimension dim = SimpleGUI.getLastDimension(frame, null);
        if (dim == null) {
            dim = new Dimension(600, 600);
        }
        frame.setPreferredSize(dim);
        frame.setLocation(SimpleGUI.getLastLocation(null, null, frame));
        frame.pack();
        frame.setExtendedState(guiConfig.getIntegerProperty("MAXIMIZED_STATE_OF_" + frame.getName(), JFrame.NORMAL));
        frame.setVisible(true);
        // DND
        dragNDrop = new Dropper();
        dragNDrop.addUIListener(this);
        // Ruft jede sekunde ein UpdateEvent auf

        /*
         * jago: Fixed very bad style of coding which often is the cause of
         * fully frozen applications!!! After the JFrame is realized (e.g.
         * setVisible, see above) it is not allowed to access any Swing object
         * from outside of the EDT. Even worse in this case the code created a
         * new Thread that constantly executed interval() which accesses the EDT
         * from outside every second!
         * 
         * Look out for similar code in the rest of JD whenever you encounter
         * unreproducible, seemingly random crashes or an unresponsive/frozen
         * GUI.
         */
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

    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     * 
     * @param e
     *            Die erwünschte Aktion
     */

    public void actionPerformed(ActionEvent e) {
        JDSounds.PT("sound.gui.clickToolbar");
        switch (e.getID()) {
        case JDAction.ITEMS_MOVE_UP:
        case JDAction.ITEMS_MOVE_DOWN:
        case JDAction.ITEMS_MOVE_TOP:
        case JDAction.ITEMS_MOVE_BOTTOM:
            linkListPane.moveSelectedItems(e.getID());
            break;
        case JDAction.APP_ALLOW_RECONNECT:
            logger.finer("Allow Reconnect");
            boolean checked = !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
            if (checked) {
                displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."), 10000);
            }

            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, checked);
            JDUtilities.getConfiguration().save();

            /*
             * Steht hier, weil diese Funktion(toggleReconnect) direkt vom
             * Trayicon Addon aufgerufen wird und ich dennoch die Gui aktuell
             * halten will
             */

            // btnReconnect.setIcon(new
            // ImageIcon(JDUtilities.getImage(getDoReconnectImage())));
            break;
        case JDAction.APP_PAUSE_DOWNLOADS:
            btnPause.setSelected(!btnPause.isSelected());
            fireUIEvent(new UIEvent(this, UIEvent.UI_PAUSE_DOWNLOADS, btnPause.isSelected()));
            btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
            break;
        case JDAction.APP_CLIPBOARD:
            logger.finer("Clipboard");
            ClipboardHandler.getClipboard().toggleActivation();
            break;
        case JDAction.APP_START_STOP_DOWNLOADS:
            logger.finer("Start Stop Downloads");
            startStopDownloads();
            btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
            btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
            break;
        case JDAction.APP_SAVE_DLC:
            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.setDialogTitle(JDLocale.L("gui.filechooser.savelistasdlc", "Save list in DLC file"));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }
            if (ret != null) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_LINKS, ret));
            }
            break;
        case JDAction.APP_LOAD_DLC:
            fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setDialogTitle(JDLocale.L("gui.filechooser.loaddlc", "Load DLC file"));
            fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
            if (fc.showOpenDialog(frame) == JDFileChooser.APPROVE_OPTION) {
                ret = fc.getSelectedFile();
                if (ret != null) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, ret));
                }
            }
            break;

        case JDAction.APP_EXIT:
            frame.setVisible(false);
            frame.dispose();
            fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT));
            break;
        case JDAction.APP_RESTART:
            frame.setVisible(false);
            frame.dispose();
            fireUIEvent(new UIEvent(this, UIEvent.UI_RESTART));
            break;
        case JDAction.APP_LOG:
            logDialog.setVisible(!logDialog.isVisible());
            menViewLog.setSelected(!logDialog.isVisible());
            break;
        case JDAction.APP_BACKUP:
            JDInit.createQueueBackup();
            JOptionPane.showMessageDialog(null, JDLocale.L("gui.messagewindow.backupcreated", "Link list backup created successfuly."), JDLocale.L("gui.messagewindow.information", "Information"), JOptionPane.INFORMATION_MESSAGE);
            break;
        case JDAction.APP_RECONNECT:
            new Thread() {
                @Override
                public void run() {
                    doReconnect();
                }
            }.start();
            break;
        case JDAction.APP_UPDATE:
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_UPDATE));

            break;
        case JDAction.ITEMS_REMOVE:
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + linkListPane.countSelectedLinks() + " links in " + linkListPane.countSelectedPackages() + " packages)")) {
                    linkListPane.removeSelectedLinks();
                }
            } else {
                linkListPane.removeSelectedLinks();
            }
            break;
        case JDAction.APP_OPEN_OPT_CONFIG:
            SimpleGUI.showConfigDialog(frame, new ConfigPanelAddons(JDUtilities.getConfiguration()), false);
            JDUtilities.getConfiguration().save();
            break;
        case JDAction.APP_OPEN_HOST_CONFIG:
            SimpleGUI.showConfigDialog(frame, new ConfigPanelPluginForHost(JDUtilities.getConfiguration()), false);
            JDUtilities.getConfiguration().save();
            break;
        case JDAction.ITEMS_REMOVE_PACKAGES:
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (showConfirmDialog(JDLocale.L("gui.downloadlist.delete_packages", "Remove completed packages?"))) {
                    JDUtilities.getController().removeCompletedPackages();
                }
            } else {
                JDUtilities.getController().removeCompletedPackages();
            }
            break;
        case JDAction.ITEMS_REMOVE_LINKS:
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (showConfirmDialog(JDLocale.L("gui.downloadlist.delete_downloadlinks", "Remove completed downloads?"))) {
                    JDUtilities.getController().removeCompletedDownloadLinks();
                }
            } else {
                JDUtilities.getController().removeCompletedDownloadLinks();
            }
            break;
        case JDAction.ITEMS_DND:
            toggleDnD();
            break;
        case JDAction.ABOUT:
            JDAboutDialog.showDialog();
            break;
        case JDAction.CHANGES:
            showChangelogDialog();
            break;
        case JDAction.ITEMS_ADD:
            String cb = "";
            try {
                cb = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            } catch (Exception e1) {
            }
            String data = LinkInputDialog.showDialog(frame, cb.trim());
            if (data != null && data.length() > 0) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, data));
            }
            break;
        case JDAction.HELP:
            try {
                JLinkButton.openURL(JDLocale.L("gui.support.forumurl", "http://board.jdownloader.org"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            break;
        case JDAction.WIKI:
            try {
                JLinkButton.openURL(JDLocale.L("gui.support.wikiurl", "http://wiki.jdownloader.org"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            break;
        // case JDAction.APP_CONFIGURATION:
        // showConfig();
        // break;
        }

    }

    // public void showConfig() {
    // if (guiConfig.getBooleanProperty(PARAM_SHOW_FENGSHUI, true) == false) {
    // ConfigurationDialog.showConfig(frame);
    // } else {
    // if (SimpleGUI.CURRENTGUI.getFrame() != null) {
    // if (SimpleGUI.CURRENTGUI.getFrame().isVisible() == false) {
    // FengShuiConfigPanel.getInstance();
    // } else {
    // ConfigurationDialog.showConfig(frame);
    // }
    // } else {
    // FengShuiConfigPanel.getInstance();
    // }
    // }
    // }

    public synchronized void addLinksToGrabber(Vector<DownloadLink> links) {
        logger.info("GRAB");

        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        // if (linkGrabber != null) {
        // logger.info("Linkgrabber should be disposed");
        // linkGrabber.dispose();
        // linkGrabber = null;
        // }
        if (linkGrabber == null) {
            logger.info("new linkgrabber");

            linkGrabber = new LinkGrabber(this, linkList);
            taskPane.add(new GrabberTaskPane("Linkgrabber",JDTheme.II("gui.images.add"),linkGrabber));
     
            taskPane.displayPanel(linkGrabber);


        } else {
            logger.info("add to grabber");
            linkGrabber.addLinks(linkList);
            taskPane.displayPanel(linkGrabber);
        }
        dragNDrop.setText(JDLocale.L("gui.droptarget.grabbed", "Grabbed:") + " " + linkList.length + " (" + links.size() + ")");
    }

    public void addUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }

    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI() {
        CURRENTGUI = this;
        linkListPane = new DownloadLinksTreeTablePanel(this);

        taskPane = new TabbedPane(this);
        taskPane.add(new DownloadTaskPane("Download",JDTheme.II("gui.images.down"),linkListPane));


        taskPane.add(new ConfigTaskPane("Configuration",JDTheme.II("gui.images.configuration")));
        taskPane.displayPanel(linkListPane);
        // taskPane.display(0);
        progressBar = new TabProgress();
        statusBar = new StatusBar();
        horizontalSplitPane = new JSplitPane();
        horizontalSplitPane.setBottomComponent(progressBar);
        horizontalSplitPane.setTopComponent(taskPane);
        horizontalSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        btnStartStop = createMenuButton(actionStartStopDownload);
        btnPause = createMenuButton(actionPause);
        btnPause.setEnabled(false);
        btnPause.setSelected(false);
        btnReconnect = createMenuButton(doReconnect);
        btnReconnect.setSelected(false);
        btnClipBoard = createMenuButton(actionClipBoard);
        btnClipBoard.setSelected(false);
        new DropTarget(btnClipBoard, this);

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(btnStartStop);
        toolBar.add(btnPause);
        toolBar.add(createMenuButton(actionItemsAdd));
        toolBar.add(createMenuButton(actionLoadDLC));
        toolBar.add(createMenuButton(actionItemsDelete));
        toolBar.addSeparator();
        toolBar.add(createMenuButton(actionItemsBottom));
        toolBar.add(createMenuButton(actionItemsDown));
        toolBar.add(createMenuButton(actionItemsUp));
        toolBar.add(createMenuButton(actionItemsTop));
        // toolBar.addSeparator();
        // toolBar.add(createMenuButton(actionConfig));
        toolBar.addSeparator();
        toolBar.add(btnReconnect);
        toolBar.add(createMenuButton(actionReconnect));
        toolBar.add(btnClipBoard);
        toolBar.addSeparator();
        toolBar.add(createMenuButton(actionUpdate));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_SHOW_SPEEDMETER, true)) {
            speedmeter = new SpeedMeterPanel();
            if (!UIManager.getLookAndFeel().getName().equals("Nimbus") && !UIManager.getLookAndFeel().getName().equals("gtk")) {
                toolBar.add(Box.createGlue());
            }
            // toolBar.add(Box.createGlue());
            toolBar.add(speedmeter);
        }
        JPanel panel = new JPanel(new BorderLayout());
        int n = 2;
        toolBar.setBorder(new EmptyBorder(n, 0, n, 0));
        n = 5;
        panel.setBorder(new EmptyBorder(0, n, 0, n));
        n = 3;
        statusBar.setBorder(new EmptyBorder(n, 0, n, 0));
        frame.setContentPane(panel);

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(horizontalSplitPane, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);

        // Einbindung des Log Dialogs
        logDialog = new LogDialog(frame, logger);
        logDialog.addWindowListener(new LogDialogWindowAdapter());
    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:
                    frame.setTitle(JDUtilities.getJDTitle());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, false)) {
                        startStopDownloads();
                    }

                    break;
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Plugin Aktiviert: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction start. ");
                        statusBarHandler.changeTxt(JDLocale.L("gui.statusbar.interaction", "Interaction:") + " " + ((Interaction) event.getSource()).getInteractionName(), 10000, true);
                        frame.setTitle(JDUtilities.JD_TITLE + " | " + JDLocale.L("gui.titleaddaction", "Action: ") + " " + ((Interaction) event.getSource()).getInteractionName());
                    }
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.getFrame().setVisible(false);
                    SimpleGUI.this.getFrame().dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Plugin Deaktiviert: " + event.getSource());
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction zu Ende. Rest status");
                        if (Interaction.areInteractionsInProgress()) {
                            statusBarHandler.changeTxt(null, 0, false);
                            frame.setTitle(JDUtilities.getJDTitle());
                        }
                    }
                    break;
                case ControlEvent.CONTROL_ON_PROGRESS:
                    handleProgressController((ProgressController) event.getSource(), event.getParameter());
                    break;
                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                    logger.info("ALL FINISHED");
                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(false);
                    btnPause.setSelected(false);
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));

                    if (speedmeter != null) speedmeter.stop();

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
                    break;
                case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                    if ((Property) event.getSource() == JDUtilities.getConfiguration()) {
                        if (event.getParameter().equals(Configuration.PARAM_DISABLE_RECONNECT)) {
                            btnReconnect.setIcon(new ImageIcon(JDUtilities.getImage(getDoReconnectImage())));
                        } else if (event.getParameter().equals(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE)) {
                            btnClipBoard.setIcon(new ImageIcon(JDUtilities.getImage(getClipBoardImage())));
                        }
                    }
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    // only in this way the button state is correctly set
                    // controller.startDownloads() is called by button itself so
                    // it cannot handle this
                    if (speedmeter != null) speedmeter.start();
                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(true);
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    if (speedmeter != null) speedmeter.stop();
                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(true);
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;
                }
            }
        });
    }

    private JButton createMenuButton(JDAction action) {
        JButton bt = new JButton(action);
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setOpaque(false);
        bt.setText(null);
        return bt;
    }

    public void displayMiniWarning(final String shortWarn, final String toolTip, final int showtime) {
        if (shortWarn == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    warning.setVisible(false);
                    warning.setText("");
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    warning.setVisible(true);
                    warning.setText(shortWarn);
                    warning.setToolTipText(toolTip);
                }
            });
            if (warningWorker != null) {
                warningWorker.interrupt();
            }
            warningWorker = new Thread() {

                public void run() {
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(300);
                        } catch (Exception e) {
                        }
                        if (this.isInterrupted()) { return; }
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                warning.setEnabled(false);
                            }
                        });
                        if (this.isInterrupted()) { return; }
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                        if (this.isInterrupted()) { return; }
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                warning.setEnabled(true);
                            }
                        });
                        if (this.isInterrupted()) { return; }

                    }

                }
            };
            warningWorker.start();

            if (showtime > 0) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(showtime);
                        } catch (InterruptedException e) {

                            e.printStackTrace();
                        }
                        displayMiniWarning(null, null, 0);
                    }
                }.start();
            }
        }
    }

    public void doReconnect() {
        if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            int confirm = JOptionPane.showConfirmDialog(frame, JDLocale.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?"), "", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
            }
        } else {
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
        }
    }

    public void fireUIEvent(UIEvent uiEvent) {
        synchronized (uiListener) {
            for (UIListener lstn : uiListener) {
                lstn.uiEvent(uiEvent);
            }
        }
    }

    public String getCaptchaCodeFromUser(final Plugin plugin, final File captchaAddress, final String def) {
        CaptchaDialog captchaDialog = new CaptchaDialog(frame, plugin, captchaAddress, def);

        logger.info("Returned: " + captchaDialog.getCaptchaText());
        return captchaDialog.getCaptchaText();
    }

    public String getInputFromUser(final String message, final String def) {
        InputDialog inputDialog = new InputDialog(frame, message, def);
        return inputDialog.getInputText();
    }

    private String getClipBoardImage() {
        if (ClipboardHandler.getClipboard().isEnabled()) {
            return JDTheme.V("gui.images.clipboardon");
        } else {
            return JDTheme.V("gui.images.clipboardoff");
        }
    }

    private String getDoReconnectImage() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
            return JDTheme.V("gui.images.reconnect_ok");
        } else {
            return JDTheme.V("gui.images.reconnect_bad");
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    private String getPauseImage() {
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            if (btnPause.isSelected()) {
                return JDTheme.V("gui.images.stop_after_active");
            } else {
                return JDTheme.V("gui.images.stop_after");
            }
        } else {
            return JDTheme.V("gui.images.stop_after");
        }
    }

    private String getStartStopDownloadImage() {
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            return JDTheme.V("gui.images.next");
        } else {
            return JDTheme.V("gui.images.stop");
        }
    }

    private void handleProgressController(ProgressController source, Object parameter) {

        if (!progressBar.hasController(source) && !source.isFinished()) {
            progressBar.addController(source);
        } else if (source.isFinished()) {
            progressBar.removeController(source);
        } else {
            progressBar.updateController(source);
        }

        if (progressBar.getControllers().size() > 0) {
            horizontalSplitPane.setDividerLocation(0.8);
        }
    }

    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions() {
        actionStartStopDownload = new JDAction(this, getStartStopDownloadImage(), "action.start", JDAction.APP_START_STOP_DOWNLOADS);
        actionPause = new JDAction(this, getPauseImage(), "action.pause", JDAction.APP_PAUSE_DOWNLOADS);
        actionItemsAdd = new JDAction(this, JDTheme.V("gui.images.add"), "action.add", JDAction.ITEMS_ADD);
        actionAddLinks = new JDAction(this, JDTheme.V("gui.images.add"), "action.add", JDAction.ITEMS_ADD);
        actionRemoveLinks = new JDAction(this, JDTheme.V("gui.images.delete"), "action.remove.links", JDAction.ITEMS_REMOVE_LINKS);
        actionRemovePackages = new JDAction(this, JDTheme.V("gui.images.delete"), "action.remove.packages", JDAction.ITEMS_REMOVE_PACKAGES);
        actionDnD = new JDAction(this, JDTheme.V("gui.images.clipboard"), "action.dnd", JDAction.ITEMS_DND);
        actionOptionalConfig = new JDAction(this, JDTheme.V("gui.images.config.packagemanager"), "action.optconfig", JDAction.APP_OPEN_OPT_CONFIG);
        actionHostConfig = new JDAction(this, JDTheme.V("gui.images.config.host"), "action.hostconfig", JDAction.APP_OPEN_HOST_CONFIG);
        actionLoadDLC = new JDAction(this, JDTheme.V("gui.images.load"), "action.load", JDAction.APP_LOAD_DLC);
        actionSaveDLC = new JDAction(this, JDTheme.V("gui.images.save"), "action.save", JDAction.APP_SAVE_DLC);
        actionExit = new JDAction(this, JDTheme.V("gui.images.exit"), "action.exit", JDAction.APP_EXIT);
        actionRestart = new JDAction(this, JDTheme.V("gui.images.exit"), "action.restart", JDAction.APP_RESTART);
        actionLog = new JDAction(this, JDTheme.V("gui.images.terminal"), "action.viewlog", JDAction.APP_LOG);
        actionBackup = new JDAction(this, JDTheme.V("gui.images.save"), "action.backup", JDAction.APP_BACKUP);
        actionClipBoard = new JDAction(this, getClipBoardImage(), "action.clipboard", JDAction.APP_CLIPBOARD);
        // actionConfig = new JDAction(this,
        // JDTheme.V("gui.images.configuration"), "action.configuration",
        // JDAction.APP_CONFIGURATION);
        actionReconnect = new JDAction(this, JDTheme.V("gui.images.reconnect"), "action.reconnect", JDAction.APP_RECONNECT);
        actionUpdate = new JDAction(this, JDTheme.V("gui.images.update_manager"), "action.update", JDAction.APP_UPDATE);
        actionItemsDelete = new JDAction(this, JDTheme.V("gui.images.delete"), "action.edit.items_remove", JDAction.ITEMS_REMOVE);
        actionItemsTop = new JDAction(this, JDTheme.V("gui.images.go_top"), "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
        actionItemsUp = new JDAction(this, JDTheme.V("gui.images.top"), "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
        actionItemsDown = new JDAction(this, JDTheme.V("gui.images.down"), "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
        actionItemsBottom = new JDAction(this, JDTheme.V("gui.images.go_bottom"), "action.edit.items_bottom", JDAction.ITEMS_MOVE_BOTTOM);
        doReconnect = new JDAction(this, getDoReconnectImage(), "action.doReconnect", JDAction.APP_ALLOW_RECONNECT);
        actionHelp = new JDAction(this, JDTheme.V("gui.images.help"), "action.help", JDAction.HELP);
        actionWiki = new JDAction(this, JDTheme.V("gui.images.help"), "action.wiki", JDAction.WIKI);
        actionAbout = new JDAction(this, JDTheme.V("gui.images.jd_logo"), "action.about", JDAction.ABOUT);
        actionChanges = new JDAction(this, JDTheme.V("gui.images.update_manager"), "action.changes", JDAction.CHANGES);
    }

    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar() {
        JMenu menFile = new JMenu(JDLocale.L("gui.menu.file", "File"));
        JMenu menExtra = new JMenu(JDLocale.L("gui.menu.extra", "Extras"));
        menAddons = new JMenu(JDLocale.L("gui.menu.addons", "Addons"));
        menHosts = new JMenu(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"));
        JMenu menHelp = new JMenu(JDLocale.L("gui.menu.plugins.help", "?"));

        menViewLog = SimpleGUI.createMenuItem(actionLog);
        createBackup = SimpleGUI.createMenuItem(actionBackup);

        // Adds the menus from the Addons
        createOptionalPluginsMenuEntries();

        // Adds the menus from the Host Plugins
        createHostPluginsMenuEntries();

        JMenu menAdd = createMenu(JDLocale.L("gui.menu.add", "Add"), "gui.images.add");
        menAdd.add(SimpleGUI.createMenuItem(actionAddLinks));
        menAdd.add(SimpleGUI.createMenuItem(actionLoadDLC));
        menFile.add(menAdd);
        JMenu menRemove = createMenu(JDLocale.L("gui.menu.remove", "Remove"), "gui.images.delete");
        menRemove.add(SimpleGUI.createMenuItem(actionItemsDelete));
        menRemove.addSeparator();
        menRemove.add(SimpleGUI.createMenuItem(actionRemoveLinks));
        menRemove.add(SimpleGUI.createMenuItem(actionRemovePackages));
        menFile.add(menRemove);
        menFile.addSeparator();
        menFile.add(SimpleGUI.createMenuItem(actionSaveDLC));
        menFile.addSeparator();
        menFile.add(SimpleGUI.createMenuItem(actionRestart));
        menFile.add(SimpleGUI.createMenuItem(actionExit));

        // menExtra.add(SimpleGUI.createMenuItem(actionConfig));
        menExtra.addSeparator();
        menExtra.add(SimpleGUI.createMenuItem(actionDnD));

        menHelp.add(menViewLog);
        menHelp.add(createBackup);
        menHelp.addSeparator();
        menHelp.add(SimpleGUI.createMenuItem(actionHelp));
        menHelp.add(SimpleGUI.createMenuItem(actionWiki));
        menHelp.addSeparator();
        menHelp.add(SimpleGUI.createMenuItem(actionChanges));
        menHelp.add(SimpleGUI.createMenuItem(actionAbout));

        menuBar.setLayout(new GridBagLayout());
        Insets insets = new Insets(1, 1, 1, 1);

        int m = 0;
        JDUtilities.addToGridBag(menuBar, menFile, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menExtra, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menAddons, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menHosts, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menHelp, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, new JLabel(""), m++, 0, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        warning = new JLabel("", JDTheme.II("gui.images.warning", 16, 16), SwingConstants.RIGHT);
        warning.setVisible(false);
        warning.setIconTextGap(10);
        warning.setHorizontalTextPosition(SwingConstants.RIGHT);
        JDUtilities.addToGridBag(menuBar, warning, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

        try {
            ImageIcon icon = JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.update_manager"), 16, -1);
            JLinkButton linkButton = new JLinkButton(JDLocale.L("jdownloader.org", "jDownloader.org"), icon, new URL(JDLocale.L("jdownloader.localnewsurl", "http://jdownloader.org/news?lng=en")));
            linkButton.setHorizontalTextPosition(SwingConstants.LEFT);
            linkButton.setBorder(null);
            Dimension d = new Dimension(10, 0);
            JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(menuBar, linkButton, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            d = new Dimension(1, 0);
            JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        } catch (MalformedURLException e1) {
        }

        frame.setJMenuBar(menuBar);

    }

    public void createOptionalPluginsMenuEntries() {
        Component temp = (menAddons.getComponentCount() != 0) ? menAddons.getComponent(0) : SimpleGUI.createMenuItem(this.actionOptionalConfig);
        menAddons.removeAll();
        menAddons.add(temp);
        menAddons.addSeparator();

        ArrayList<JMenuItem> itemsWithSubmenu = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsToggle = new ArrayList<JMenuItem>();
        ArrayList<JMenuItem> itemsPress = new ArrayList<JMenuItem>();

        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded()) continue;
            if (plg.getPlugin().createMenuitems() != null && JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), false)) {
                if (plg.getPlugin().createMenuitems().size() > 1) {

                    MenuItem m = new MenuItem(MenuItem.CONTAINER, plg.getPlugin().getHost(), 0);
                    m.setItems(plg.getPlugin().createMenuitems());
                    JMenuItem mi = SimpleGUI.getJMenuItem(m);

                    if (mi != null) {
                        itemsWithSubmenu.add(mi);

                        ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                        ((JMenu) mi).addMenuListener(new MenuListener() {
                            public void menuCanceled(MenuEvent e) {
                            }

                            public void menuDeselected(MenuEvent e) {
                            }

                            public void menuSelected(MenuEvent e) {
                                JMenu m = (JMenu) e.getSource();

                                m.removeAll();
                                for (MenuItem menuItem : plg.getPlugin().createMenuitems()) {
                                    JMenuItem c = SimpleGUI.getJMenuItem(menuItem);

                                    if (c == null) {
                                        m.addSeparator();
                                    } else {
                                        m.add(c);
                                    }
                                }
                            }

                        });
                    } else {
                        menAddons.addSeparator();
                    }
                } else {
                    for (MenuItem mi : plg.getPlugin().createMenuitems()) {
                        JMenuItem c = SimpleGUI.getJMenuItem(mi);
                        c.setDisabledIcon(null);
                        c.setIcon(null);
                        c.setSelectedIcon(null);
                        c.setDisabledSelectedIcon(null);
                        if (mi.getID() == MenuItem.TOGGLE)
                            itemsToggle.add(c);
                        else
                            itemsPress.add(c);
                        break;
                    }
                }
                for (JMenuItem jmi : itemsWithSubmenu)
                    menAddons.add(jmi);
                for (JMenuItem jmi : itemsPress)
                    menAddons.add(jmi);
                for (JMenuItem jmi : itemsToggle)
                    menAddons.add(jmi);
            }
        }
        if (menAddons.getItem(menAddons.getItemCount() - 1) == null) {
            menAddons.remove(menAddons.getItemCount() - 1);
        }
    }

    public void createHostPluginsMenuEntries() {
        Component temp = null;
        if (menHosts.getComponentCount() != 0) {
            temp = menHosts.getComponent(0);
        } else {
            temp = SimpleGUI.createMenuItem(actionHostConfig);
        }
        menHosts.removeAll();
        menHosts.add(temp);

        menHosts.addSeparator();
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = wrapper.getPlugin();
                if (helpPlugin.createMenuitems() != null) {
                    JMenuItem mi = SimpleGUI.getJMenuItem(new MenuItem(MenuItem.CONTAINER, helpPlugin.getHost(), 0));
                    if (mi != null) {
                        menHosts.add(mi);
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
                                for (MenuItem menuItem : helpPlugin.createMenuitems()) {
                                    c = SimpleGUI.getJMenuItem(menuItem);
                                    if (c == null) {
                                        m.addSeparator();
                                    } else {
                                        m.add(c);
                                    }
                                }
                            }
                        });
                    } else {
                        menHosts.addSeparator();
                    }
                }
            }

        }
        if (menHosts.getItemCount() == 0) {
            menHosts.setEnabled(false);
        }
    }

    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren
     */
    private void interval() {

        if (JDUtilities.getController() != null) {
            statusBar.setSpeed(JDUtilities.getController().getSpeedMeter());
        }

        progressBar.updateController(null);
        frame.setTitle(JDUtilities.getJDTitle());
    }

    public void removeUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }

    /**
     * Setzt den text im DropTargets
     * 
     * @param text
     */
    public void setDropTargetText(String text) {
        dragNDrop.setText(text);
    }

    public void localize() {
        UIManager.put("FileChooser.upFolderToolTipText", JDLocale.L("gui.filechooser.upFolderToolTipText", "Up one level"));
        UIManager.put("FileChooser.filesOfTypeLabelText", JDLocale.L("gui.filechooser.filesOfTypeLabelText", "Files of type:"));
        UIManager.put("FileChooser.lookInLabelText", JDLocale.L("gui.filechooser.lookInLabelText", "Look in:"));
        UIManager.put("FileChooser.saveInLabelText", JDLocale.L("gui.filechooser.saveInLabelText", "Save in:"));
        UIManager.put("FileChooser.fileNameLabelText", JDLocale.L("gui.filechooser.fileNameLabelText", "File name:"));
        UIManager.put("FileChooser.homeFolderToolTipText", JDLocale.L("gui.filechooser.homeFolderToolTipText", "Home folder"));
        UIManager.put("FileChooser.newFolderToolTipText", JDLocale.L("gui.filechooser.newFolderToolTipText", "Make a new folder"));
        UIManager.put("FileChooser.listViewButtonToolTipText", JDLocale.L("gui.filechooser.listViewButtonToolTipText", "List view"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText", JDLocale.L("gui.filechooser.detailsViewButtonToolTipText", "Details"));
        UIManager.put("FileChooser.saveButtonText", JDLocale.L("gui.filechooser.saveButtonText", "Save"));
        UIManager.put("FileChooser.openButtonText", JDLocale.L("gui.filechooser.openButtonText", "Open"));
        UIManager.put("FileChooser.cancelButtonText", JDLocale.L("gui.filechooser.cancelButtonText", "Cancel"));
        UIManager.put("FileChooser.updateButtonText", JDLocale.L("gui.filechooser.updateButtonText", "Update"));
        UIManager.put("FileChooser.helpButtonText", JDLocale.L("gui.filechooser.helpButtonText", "Help"));
        UIManager.put("FileChooser.deleteButtonText", JDLocale.L("gui.filechooser.deleteButtonText", "Delete"));
        UIManager.put("FileChooser.saveButtonToolTipText", JDLocale.L("gui.filechooser.saveButtonToolTipText", "Save"));
        UIManager.put("FileChooser.openButtonToolTipText", JDLocale.L("gui.filechooser.openButtonToolTipText", "Open"));
        UIManager.put("FileChooser.cancelButtonToolTipText", JDLocale.L("gui.filechooser.cancelButtonToolTipText", "Cancel"));
        UIManager.put("FileChooser.updateButtonToolTipText", JDLocale.L("gui.filechooser.updateButtonToolTipText", "Update"));
        UIManager.put("FileChooser.helpButtonToolTipText", JDLocale.L("gui.filechooser.helpButtonToolTipText", "Help"));
        UIManager.put("FileChooser.deleteButtonToolTipText", JDLocale.L("gui.filechooser.deleteButtonToolTipText", "Delete"));
        UIManager.put("FileChooser.openDialogTitleText", JDLocale.L("gui.filechooser.openWindowTitleText", "Open"));
        UIManager.put("FileChooser.saveDialogTitleText", JDLocale.L("gui.filechooser.saveWindowTitleText", "Save"));
        UIManager.put("FileChooser.acceptAllFileFilterText", JDLocale.L("gui.filechooser.acceptAllFileFilterText", "All files"));
        UIManager.put("FileChooser.other.newFolder", JDLocale.L("gui.filechooser.other.newFoldert", "New folder"));
        UIManager.put("FileChooser.other.newFolder.subsequent", JDLocale.L("gui.filechooser.other.newFolder.subsequent", "New folder {0}"));
        UIManager.put("FileChooser.win32.newFolder", JDLocale.L("gui.filechooser.win32.newFolder", "New folder"));
        UIManager.put("FileChooser.win32.newFolder.subsequent", JDLocale.L("gui.filechooser.win32.newFolder.subsequent", "New folder {0}"));
        UIManager.put("FileChooser.pathLabelText", JDLocale.L("gui.filechooser.pathLabelText", "Path"));

        UIManager.put("JXTable.column.packSelected", JDLocale.L("gui.treetable.packSelected", "Pack selected column"));
        UIManager.put("JXTable.column.packAll", JDLocale.L("gui.treetable.packAll", "Pack all columns"));
        UIManager.put("JXTable.column.horizontalScroll", JDLocale.L("gui.treetable.horizontalScroll", "Horizontal scroll"));

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

    /**
     * Setzt die Geschwindigkeit in der Statusbar
     * 
     * @param speed
     */
    public void setSpeedStatusBar(Integer speed) {
        statusBar.setSpinnerSpeed(speed);
    }

    public boolean showConfirmDialog(String message) {
        // logger.info("ConfirmDialog");
        Object[] options = { JDLocale.L("gui.btn_yes", "Yes"), JDLocale.L("gui.btn_no", "No") };
        int n = JOptionPane.showOptionDialog(frame, message, "", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return (n == 0);
    }

    public boolean showConfirmDialog(String message, String title) {
        // logger.info("ConfirmDialog");
        Object[] options = { JDLocale.L("gui.btn_yes", "Yes"), JDLocale.L("gui.btn_no", "No") };
        int n = JOptionPane.showOptionDialog(frame, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        return (n == 0);
    }

    public boolean showCountdownConfirmDialog(String string, int sec) {
        // logger.info("CountdownConfirmDialog");
        return CountdownConfirmDialog.showCountdownConfirmDialog(frame, string, sec);
    }

    public int showHelpMessage(String title, String message, boolean toHTML, String url, String helpMsg, int sec) {
        // logger.info("HelpMessageDialog");
        if (toHTML) message = "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + message + "</font>";
        try {
            return JHelpDialog.showHelpMessage(frame, title, message, new URL(url), helpMsg, sec);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean showHTMLDialog(String title, String htmlQuestion) {
        // logger.info("HTMLDialog");
        return HTMLDialog.showDialog(getFrame(), title, htmlQuestion);
    }

    public void showMessageDialog(String string) {
        // logger.info("MessageDialog");
        JOptionPane.showMessageDialog(frame, string);
    }

    public String showTextAreaDialog(String title, String question, String def) {
        // logger.info("TextAreaDialog");
        return TextAreaDialog.showDialog(getFrame(), title, question, def);
    }

    public String showUserInputDialog(String string) {
        // logger.info("UserInputDialog");
        return JOptionPane.showInputDialog(frame, string);
    }

    public String showUserInputDialog(String string, String def) {
        // logger.info("UserInputDialog");
        return JOptionPane.showInputDialog(frame, string, def);
    }

    public String[] showTextAreaDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        // logger.info("TextAreaDialog");
        return TextAreaDialog.showDialog(getFrame(), title, questionOne, questionTwo, defaultOne, defaultTwo);
    }

    public String[] showTwoTextFieldDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        // logger.info("TextFieldDialog");
        return TwoTextFieldDialog.showDialog(getFrame(), title, questionOne, questionTwo, defaultOne, defaultTwo);
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

    public void startStopDownloads() {
        btnStartStop.setEnabled(false);
        btnPause.setEnabled(true);
        JDUtilities.getController().toggleStartStop();
    }

    public void toggleDnD() {
        if (dragNDrop.isVisible()) {
            dragNDrop.setVisible(false);
        } else {
            dragNDrop.setVisible(true);
            dragNDrop.setText(JDLocale.L("gui.droptarget.label", "Ziehe Links auf mich!"));
        }
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

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == getFrame()) {
            boolean doIt;
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                doIt = showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
            } else {
                doIt = true;
            }
            if (doIt) {
                SimpleGUI.saveLastLocation(e.getComponent(), null);
                SimpleGUI.saveLastDimension(e.getComponent(), null);
                guiConfig.save();
                fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT, null));
            }
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
        return guiConfig;
    }

    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        logger.info("Drag: DROP " + dtde.getDropAction() + " : " + dtde.getSourceActions() + " - " + dtde.getSource());
        try {
            Transferable tr = dtde.getTransferable();
            dtde.acceptDrop(dtde.getDropAction());
            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, files));
            } else if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List list = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (int t = 0; t < list.size(); t++) {
                    fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, list.get(t)));
                }
            } else {
                logger.info("Unsupported Drop-Type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

}
