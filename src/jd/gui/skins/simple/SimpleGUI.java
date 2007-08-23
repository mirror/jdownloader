package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import jd.JDUtilities;
import jd.controlling.JDAction;
import jd.controlling.event.ControlEvent;
import jd.gui.GUIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI extends GUIInterface implements ClipboardOwner{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    private static final String JDOWNLOADER_ID = "JDownloader active";
    /**
     * Das Hauptfenster
     */
    private JFrame frame;
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
    private TabDownloadLinks  tabDownloadTable;;
    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabPluginActivity tabPluginActivity;
    /**
     * TabbedPane
     */
    private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar statusBar;
    
    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    private JToggleButton btnStartStop;
    private JDAction actionStartStopDownload;
    private JDAction actionAdd;
    private JDAction actionDelete;
    private JDAction actionLoadLinks;
    private JDAction actionSaveLinks;
    private JDAction actionExit;
    private JDAction actionLog;
    
    private LogDialog logDialog;

   private JCheckBoxMenuItem menViewLog;

    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI(){
        super();
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        }
        catch (UnsupportedLookAndFeelException e) {}
        
        frame      = new JFrame();
        tabbedPane = new JTabbedPane();
        menuBar    = new JMenuBar();
        toolBar    = new JToolBar();
        frame.setIconImage(JD_ICON);
        frame.setTitle(JD_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(JDOWNLOADER_ID), this);

        frame.pack();
        frame.setVisible(true);
    }
    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions(){
        actionStartStopDownload = new JDAction(this,  "start", "action.start",  JDAction.APP_START_STOP_DOWNLOADS);
        actionAdd               = new JDAction(this,    "add", "action.add",    JDAction.ITEMS_ADD);
        actionDelete            = new JDAction(this, "delete", "action.delete", JDAction.ITEMS_REMOVE);
        actionLoadLinks         = new JDAction(this,   "load", "action.load",   JDAction.APP_LOAD);
        actionSaveLinks         = new JDAction(this,   "save", "action.save",   JDAction.APP_SAVE);
        actionExit              = new JDAction(this,   "exit", "action.exit",   JDAction.APP_EXIT);
        actionLog               = new JDAction(this,    "log", "action.viewlog",JDAction.VIEW_LOG);
    }
    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar(){
        // file menu
        JMenu menFile         = new JMenu(JDUtilities.getResourceString("menu.file"));
        menFile.setMnemonic(JDUtilities.getResourceChar("menu.file_mnem"));

        JMenuItem menFileLoad = createMenuItem(actionLoadLinks);
        JMenuItem menFileSave = createMenuItem(actionSaveLinks);
        JMenuItem menFileExit = createMenuItem(actionExit);
        
        // view menu
        JMenu menView         = new JMenu(JDUtilities.getResourceString("menu.view"));
        menView.setMnemonic(JDUtilities.getResourceChar("menu.view_mnem"));
        
        menViewLog = new JCheckBoxMenuItem(actionLog);
        menViewLog.setIcon(null);
        
        // action menu
        JMenu menAction       = new JMenu(JDUtilities.getResourceString("menu.action"));
        menAction.setMnemonic(JDUtilities.getResourceChar("menu.action_mnem"));
        
        JMenuItem menDownload = createMenuItem(actionStartStopDownload);
        
        // add menus to parents
        menFile.add(menFileLoad);
        menFile.add(menFileSave);
        menFile.addSeparator();
        menFile.add(menFileExit);
        
        menView.add(menViewLog);
        
        menAction.add(menDownload);
        
        menuBar.add(menFile);
        menuBar.add(menView);
        menuBar.add(menAction);
        frame.setJMenuBar(menuBar);
    }
     
    /**
     * factory method for menu items
     * @param action action for the menu item
     * @return the new menu item
     */
    private static JMenuItem createMenuItem(JDAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setIcon(null);
        if (action.getAccelerator()!=null)
           menuItem.setAccelerator(action.getAccelerator());
        return menuItem;
    }
    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI(){
        tabbedPane        = new JTabbedPane();
        tabDownloadTable  = new TabDownloadLinks(this);
        tabPluginActivity = new TabPluginActivity();
        statusBar         = new StatusBar();
        tabbedPane.addTab(JDUtilities.getResourceString("label.tab.download"),        tabDownloadTable);
        tabbedPane.addTab(JDUtilities.getResourceString("label.tab.plugin_activity"), tabPluginActivity);

        btnStartStop  = new JToggleButton(actionStartStopDownload);
        btnStartStop.setSelectedIcon(new ImageIcon(JDUtilities.getImage("stop")));
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setText(null);


        JButton btnAdd    = new JButton(actionAdd);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setText(null);

        JButton btnDelete = new JButton(actionDelete);
        btnDelete.setFocusPainted(false);
        btnDelete.setBorderPainted(false);
        btnDelete.setText(null);

        toolBar.setFloatable(false);
        toolBar.add(btnStartStop);
        toolBar.add(btnAdd);
        toolBar.add(btnDelete);

        frame.setLayout(new GridBagLayout());
        JDUtilities.addToGridBag(frame, toolBar,     0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
        JDUtilities.addToGridBag(frame, tabbedPane,  0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH,       GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(frame, statusBar,   0, 2, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
        
        // Einbindung des Log Dialogs
        logDialog = new LogDialog(getFrame(), logger);
        logDialog.setVisible(true);
        logDialog.addWindowListener(new LogDialogWindowAdapter());
    }
    
    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     *
     * @param actionID Die erwünschte Aktion
     */
    public synchronized void doAction(int actionID){
        switch(actionID){
            case JDAction.ITEMS_MOVE_UP:
            case JDAction.ITEMS_MOVE_DOWN:
            case JDAction.ITEMS_MOVE_TOP:
            case JDAction.ITEMS_MOVE_BOTTOM:
                if(tabbedPane.getSelectedComponent() == tabDownloadTable){
                    tabDownloadTable.moveItems(actionID);
                }
                break;
            case JDAction.APP_START_STOP_DOWNLOADS:
                startStopDownloads();
                break;
            case JDAction.APP_SAVE:
                saveLinks();
                break;
            case JDAction.APP_LOAD:
                loadLinks();
                break;
            case JDAction.APP_EXIT:
                frame.setVisible(false);
                saveConfig();
                break;
            case JDAction.VIEW_LOG:
                logDialog.setVisible(!logDialog.isVisible());
                break;
            case JDAction.ITEMS_ADD:
                break;
        }
    }
    /**
     * Hier werden ControlEvent ausgewertet
     */
    public void controlEvent(ControlEvent event) {
        switch(event.getID()){
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
                tabDownloadTable.fireTableChanged();
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                download = null;
                btnStartStop.setSelected(false);
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                if(links != null && links instanceof Vector && ((Vector)links).size()>0){
                    tabDownloadTable.addLinks((Vector)links);
                }
                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE:
                statusBar.setPluginForDecryptActive(true);
                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE:
                statusBar.setPluginForDecryptActive(false);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE:
                statusBar.setPluginForHostActive(true);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE:
                statusBar.setPluginForHostActive(false);
                break;
        }
    }
    public void pluginEvent(PluginEvent event) {
        if(event.getSource() instanceof PluginForHost)
            tabDownloadTable.pluginEvent(event);
        if(event.getSource() instanceof PluginForDecrypt)
            tabPluginActivity.pluginEvent(event);
    }
    @Override
    protected Vector<DownloadLink> getDownloadLinks() {
        if(tabDownloadTable != null)
            return tabDownloadTable.getLinks();
        return null;
    }
    
    @Override
    protected void setDownloadLinks(Vector<DownloadLink> links) {
        if (tabDownloadTable != null)
            tabDownloadTable.setDownloadLinks(links);
        
    }
    @Override
    public JFrame getFrame() {
        return frame;
    }
    @Override
    public DownloadLink getNextDownloadLink() {
        if(tabDownloadTable != null)
            return tabDownloadTable.getNextDownloadLink();
        return null;
    }

    /**
     * Toggled das MenuItem fuer die Ansicht des Log Fensters
     * 
     * @author Tom
     */
    private final class LogDialogWindowAdapter extends WindowAdapter {
       @Override
       public void windowOpened(WindowEvent e) {
          menViewLog.setSelected(true);
       }

       @Override
       public void windowClosed(WindowEvent e) {
          menViewLog.setSelected(false);
       }
    }
    /**
     * Diese Klasse realisiert eine StatusBar
     *
     * @author astaldo
     */
    private class StatusBar extends JPanel{
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 3676496738341246846L;
        private JLabel lblMessage;
        private JLabel lblSpeed;
        private JLabel lblPluginHostActive;
        private JLabel lblPluginDecryptActive;
        private ImageIcon imgActive;
        private ImageIcon imgInactive;

        public StatusBar(){
            imgActive   = new ImageIcon(JDUtilities.getImage("led_green"));
            imgInactive = new ImageIcon(JDUtilities.getImage("led_empty"));

            setLayout(new GridBagLayout());
            lblMessage             = new JLabel(JDUtilities.getResourceString("label.status.welcome"));
            lblSpeed               = new JLabel("450 kb/s");
            lblPluginHostActive    = new JLabel(imgInactive);
            lblPluginDecryptActive = new JLabel(imgInactive);
            lblPluginDecryptActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_decrypt"));
            lblPluginHostActive.setToolTipText(JDUtilities.getResourceString("tooltip.status.plugin_host"));

            JDUtilities.addToGridBag(this, lblMessage,             0, 0, 1, 1, 1, 1, new Insets(0,5,0,0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblSpeed,               1, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.WEST);
            JDUtilities.addToGridBag(this, lblPluginHostActive,    2, 0, 1, 1, 0, 0, new Insets(0,5,0,0), GridBagConstraints.NONE,       GridBagConstraints.EAST);
            JDUtilities.addToGridBag(this, lblPluginDecryptActive, 3, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.EAST);
        }
        public void setText(String text){
            lblMessage.setText(text);
        }
        /**
         * Zeigt, ob die Plugins zum Downloaden von einem Anbieter arbeiten
         *
         * @param active wahr, wenn Downloads aktiv sind
         */
        public void setPluginForHostActive(boolean active)    { setPluginActive(lblPluginHostActive,    active); }
        /**
         * Zeigt an, ob die Plugins zum Entschlüsseln von Links arbeiten
         *
         * @param active wahr, wenn soeben Links entschlüsselt werden
         */
        public void setPluginForDecryptActive(boolean active) { setPluginActive(lblPluginDecryptActive, active); }
        /**
         * Ändert das genutzte Bild eines Labels, um In/Aktivität anzuzeigen
         *
         * @param lbl Das zu ändernde Label
         * @param active soll es als aktiv oder inaktiv gekennzeichnet werden
         */
        private void setPluginActive(JLabel lbl,boolean active){
            if(active)
                lbl.setIcon(imgActive);
            else
                lbl.setIcon(imgInactive);
        }
    }
}
