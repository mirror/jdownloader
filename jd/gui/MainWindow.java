package jd.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import sun.misc.Service;

public class MainWindow extends JFrame implements ClipboardOwner{
   
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;
    
    private static final String JDOWNLOADER_ID = "JDownloader active";
    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar = new JMenuBar();
    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar toolBar = new JToolBar();
    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private Vector<PluginForDecrypt> pluginsForDecrypt = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle Plugins für die Anbieter gespeichert
     */
    private Vector<PluginForHost> pluginsForHost = new Vector<PluginForHost>();
    /**
     * Logger für Meldungen des Programmes
     */
    private Logger logger = Plugin.getLogger();
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
    private JTabbedPane tabbedPane = new JTabbedPane();
    /**
     * Der Thread, der das Downloaden realisiert
     */
    private StartDownload download = null;
    private Speedometer speedoMeter = new Speedometer();
    private StatusBar statusBar;
    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    private JToggleButton btnStartStop;
    private JDAction actionStartStopDownload;
    private JDAction actionMoveUp;
    private JDAction actionMoveDown;
    private JDAction actionAdd;
    private JDAction actionDelete;
    
    /**
     * Das Hauptfenster wird erstellt
     */
    public MainWindow(){
        loadImages();
        initActions();
        buildUI();
        getPlugins();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(JDOWNLOADER_ID), this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(Utilities.getImage("mind"));
        setTitle("jDownloader 0.0.1");
        pack();
    }
    /**
     * Die Aktionen werden initialisiert
     *
     */
    public void initActions(){
        actionStartStopDownload = new JDAction("start",  "action.start",     JDAction.APP_START_STOP_DOWNLOADS);
        actionMoveUp            = new JDAction("up",     "action.move_up",   JDAction.ITEMS_MOVE_UP);
        actionMoveDown          = new JDAction("down",   "action.move_down", JDAction.ITEMS_MOVE_DOWN);
        actionAdd               = new JDAction("add",    "action.add",       JDAction.ITEMS_MOVE_DOWN);
        actionDelete            = new JDAction("delete", "action.delete",    JDAction.ITEMS_MOVE_DOWN);
    }
    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt 
     */
    private void buildUI(){
        tabbedPane        = new JTabbedPane();
        tabDownloadTable  = new TabDownloadLinks();
        tabPluginActivity = new TabPluginActivity();
        statusBar         = new StatusBar();
        tabbedPane.addTab(Utilities.getResourceString("label.tab.download"),        tabDownloadTable);
        tabbedPane.addTab(Utilities.getResourceString("label.tab.plugin_activity"), tabPluginActivity);

        btnStartStop  = new JToggleButton(actionStartStopDownload);
        btnStartStop.setSelectedIcon(new ImageIcon(Utilities.getImage("stop")));
        btnStartStop.setFocusPainted(false); 
        btnStartStop.setBorderPainted(false);
        
        JButton btnAdd    = new JButton(actionAdd);           btnAdd.setFocusPainted(false);   btnAdd.setBorderPainted(false);
        JButton btnDelete = new JButton(actionDelete);        btnDelete.setFocusPainted(false);btnDelete.setBorderPainted(false);
        
        toolBar.setFloatable(false);
        toolBar.add(btnStartStop);
        toolBar.add(btnAdd);
        toolBar.add(btnDelete);

        setLayout(new GridBagLayout());
        Utilities.addToGridBag(this, toolBar,     0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH); 
        Utilities.addToGridBag(this, tabbedPane,  0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH,       GridBagConstraints.CENTER); 
//        Utilities.addToGridBag(this, speedoMeter, 1, 1, 1, 1, 0, 0, null, GridBagConstraints.VERTICAL,   GridBagConstraints.WEST); 
        Utilities.addToGridBag(this, statusBar,   0, 2, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST); 
//        setJMenuBar(menuBar);
    }
    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages(){
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Utilities.addImage("start",    toolkit.getImage(cl.getResource("img/start.png")));
        Utilities.addImage("stop",     toolkit.getImage(cl.getResource("img/stop.png")));
        Utilities.addImage("add",      toolkit.getImage(cl.getResource("img/add.png")));
        Utilities.addImage("delete",   toolkit.getImage(cl.getResource("img/delete.png")));
        Utilities.addImage("up",       toolkit.getImage(cl.getResource("img/up.png")));
        Utilities.addImage("down",     toolkit.getImage(cl.getResource("img/down.png")));
        Utilities.addImage("mind",     toolkit.getImage(cl.getResource("img/mind.png")));
        Utilities.addImage("led_empty",toolkit.getImage(cl.getResource("img/led_empty.gif")));
        Utilities.addImage("led_green",toolkit.getImage(cl.getResource("img/led_green.gif")));
    }
    /**
     * Hier werden alle Plugins im aktuellen Verzeichnis geparsed (und im Classpath)
     */
    private void getPlugins(){
        try {
            // Alle JAR Dateien, die in diesem Verzeichnis liegen, werden dem 
            // Classloader hinzugefügt. 
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();        
            if(classLoader != null && (classLoader instanceof URLClassLoader)){            
                URLClassLoader urlClassLoader = (URLClassLoader)classLoader;           
                Method         addURL         = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});    
                File           files[]        = new File(".").listFiles(Utilities.filterJar);
                
                addURL.setAccessible(true);        
                for(int i=0;i<files.length;i++){
                    URL jarURL = files[i].toURL();                
                    addURL.invoke(urlClassLoader, new Object[]{jarURL});        
                }                      
            }
        }
        catch (Exception e) { }        
        
        Iterator iterator;
        
        //Zuerst Plugins zum Dekodieren verschlüsselter Links
        iterator = Service.providers(PluginForDecrypt.class);
        while(iterator.hasNext())
        {
            PluginForDecrypt p = (PluginForDecrypt) iterator.next();
            pluginsForDecrypt.add(p);
            p.addPluginListener(tabPluginActivity);
            logger.info("Decrypt-Plugin : "+p.getPluginName());
        }

        //Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while(iterator.hasNext())
        {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            p.addPluginListener(tabDownloadTable);
            logger.info("Host-Plugin : "+p.getPluginName());
        }
    }
    /**
     * Diese Methode erstellt einen neuen Captchadialog und liefert den eingegebenen Text zurück.
     * 
     * @param captchaAdress Adresse des anzuzeigenden Bildes
     * @return Der vom Benutzer eingegebene Text
     */
    private String getCaptcha(String captchaAdress){
        String captchaText=null;
        CaptchaDialog captchaDialog = new CaptchaDialog(this,captchaAdress);
        MainWindow.this.toFront();
        captchaDialog.setVisible(true);
        return captchaDialog.getCaptchaText();
    }
    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     * 
     * @param actionID Die erwünschte Aktion
     */
    public void doAction(int actionID){
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
                if(download == null){
                    download = new StartDownload();
                    download.start();
                }
                else{
                    download.interrupt();
                    download=null;
                }
                break;
        }
        
    }
    /**
     * Methode, um eine Veränderung der Zwischenablage zu bemerken und zu verarbeiten
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        new ClipboardHandler().start();
    }
    /**
     * Diese Klasse ist dafür da, zeitverzögert die Zwischenablage zu untersuchen 
     * 
     * @author astaldo
     */
    private class ClipboardHandler extends Thread{
        public ClipboardHandler(){
            super("JD-ClipboardHandler");
        }
        public void run(){
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            synchronized (clipboard) {
                try {
                    clipboard.wait(500);
                }
                catch (InterruptedException e) { }
                String data = JDOWNLOADER_ID;
                try {
                    data = (String)clipboard.getData(DataFlavor.stringFlavor);
                    new DistributeData(data).start();
                }
                catch (UnsupportedFlavorException e1) {}
                catch (IOException e1)                {}

                clipboard.setContents(new StringSelection(data), MainWindow.this);
            }
        }
    }
    /**
     * Alle Interaktionen (Knöpfe, Shortcuts) sollten über diese JDAction stattfinden
     * 
     * @author astaldo
     */
    public class JDAction extends AbstractAction{
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 7393495345332708426L;

        public static final int ITEMS_MOVE_UP           = 1;
        public static final int ITEMS_MOVE_DOWN         = 2;
        public static final int ITEMS_MOVE_TOP          = 3;
        public static final int ITEMS_MOVE_BOTTOM       = 4;
        public static final int ITEMS_DISABLE           = 5;
        public static final int ITEMS_ENABLE            = 6;
        public static final int APP_START_STOP_DOWNLOADS = 7;
        public static final int APP_SHOW_LOG            = 8;
        public static final int APP_STOP_DOWNLOADS      = 9;
        
        private int actionID;
        /**
         * Erstellt ein neues JDAction-Objekt
         * 
         * @param iconName 
         * @param descriptionResource Text für den Tooltip aus der Resourcedatei
         * @param actionID ID dieser Aktion
         */
        public JDAction(String iconName, String descriptionResource, int actionID){
            super();
            ImageIcon icon = new ImageIcon(Utilities.getImage(iconName));
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, Utilities.getResourceString(descriptionResource));
            this.actionID = actionID;
        }
        public void actionPerformed(ActionEvent e) {
            doAction(actionID);
        }
        public int getActionID(){
            return actionID;
        }
    }
    /**
     * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage an (unter Umständen auch mehrere) Plugins
     * Die gefundenen Treffer werden ausgeschnitten.
     * 
     * @author astaldo
     */
    private class DistributeData extends Thread{
        private String data;
        /**
         * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll.
         * Die übergebenen Daten werden durch einen URLDecoder geschickt.
         * 
         * @param data Daten, die verteilt werden sollen
         */
        public DistributeData (String data){
            super("JD-DistributeData");
            this.data = data;
            try {
                this.data = URLDecoder.decode(this.data,"US-ASCII");
            }
            catch (UnsupportedEncodingException e) { }
        }
        public void run(){
            Vector<DownloadLink> links    = new Vector<DownloadLink>();
            Vector<String> cryptedLinks   = new Vector<String>();
            Vector<String> decryptedLinks = new Vector<String>();
            PluginForDecrypt pDecrypt;
            PluginForHost    pHost;

            // Zuerst wird überprüft, ob ein Decrypt-Plugin einen Teil aus der
            // Zwischenablage entschlüsseln kann. Ist das der Fall, wird die entsprechende Stelle
            // verarbeitet und gelöscht, damit sie keinesfalls nochmal verarbeitet wird.
            for(int i=0; i<pluginsForDecrypt.size();i++){
                pDecrypt = pluginsForDecrypt.elementAt(i);
                if(pDecrypt.isClipboardEnabled() && pDecrypt.canHandle(data)){
                    statusBar.setPluginForDecryptActive(true);
                    cryptedLinks.addAll(pDecrypt.getMatches(data,pDecrypt.getSupportedLinks()));
                    data = pDecrypt.cutMatches(data);
                    decryptedLinks.addAll(pDecrypt.decryptLinks(cryptedLinks));
                    statusBar.setPluginForDecryptActive(false);
                }
            }
            // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die Plugins der Hoster geschickt.
            for(int i=0; i<pluginsForHost.size();i++){
                pHost = pluginsForHost.elementAt(i);
                if(pHost.isClipboardEnabled() && pHost.canHandle(data)){
                    links.addAll(pHost.getDownloadLinks(data));
                    data = pHost.cutMatches(data);
                }
            }
            // Als letztes werden die entschlüsselten Links (soweit überhaupt vorhanden)
            // an die HostPlugins geschickt, damit diese einen Downloadlink erstellen können
            Iterator<String> iterator = decryptedLinks.iterator();
            while(iterator.hasNext()){
                String decrypted = iterator.next();
                for(int i=0; i<pluginsForHost.size();i++){
                    pHost = pluginsForHost.elementAt(i);
                    if(pHost.isClipboardEnabled() && pHost.canHandle(decrypted)){
                        links.addAll(pHost.getDownloadLinks(decrypted));
                        iterator.remove();
                    }
                }
            }

            if(links!=null && links.size()>0){
                tabDownloadTable.addLinks(links);
            }
        }
    }
    /**
     * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
     * 
     * @author astaldo
     */
    private class StartDownload extends Thread{
        /**
         * Der DownloadLink
         */
        private DownloadLink downloadLink;
        private Plugin plugin;
        private boolean aborted=false;
        
        public StartDownload(){
            super("JD-StartDownload");
        }
        public void abortDownload(){
            aborted=true;
            if(plugin != null)
                plugin.abort();
        }
        public void run(){

//            while((downloadLink = tabDownloadTable.getNextDownloadLink()) != null){

                plugin   = downloadLink.getPlugin();
                PluginStep step = plugin.getNextStep(downloadLink);

                // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
                // bis entwerder null zurückgegeben wird oder ein Fehler auftritt
                statusBar.setPluginForHostActive(true);
                while(!aborted && step != null && step.getStatus()!=PluginStep.STATUS_ERROR){
                    switch(step.getStep()){
                        case PluginStep.STEP_WAIT_TIME:
                            try {
                                long milliSeconds = (Long)step.getParameter();
                                logger.info("wait "+ milliSeconds+" ms");
                                Thread.sleep(milliSeconds);
                                step.setStatus(PluginStep.STATUS_DONE);
                            }
                            catch (InterruptedException e) { e.printStackTrace(); }
                            break;
                        case PluginStep.STEP_CAPTCHA:
                            String captchaText = getCaptcha((String)step.getParameter());
                            step.setParameter(captchaText);
                            step.setStatus(PluginStep.STATUS_DONE);
                    }
                    step = plugin.getNextStep(downloadLink);
                }
                statusBar.setPluginForHostActive(false);
                if(aborted){
                    logger.warning("Thread aborted");
                }
                if(step != null && step.getStatus() == PluginStep.STATUS_ERROR){
                    logger.severe("Error occurred while downloading file");
                }
//            }
            btnStartStop.setSelected(false);
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
            imgActive   = new ImageIcon(Utilities.getImage("led_green"));
            imgInactive = new ImageIcon(Utilities.getImage("led_empty"));
            
            setLayout(new GridBagLayout());
            lblMessage             = new JLabel(Utilities.getResourceString("label.status.welcome"));
            lblSpeed               = new JLabel("450 kb/s");
            lblPluginHostActive    = new JLabel(imgInactive);
            lblPluginDecryptActive = new JLabel(imgInactive);
            lblPluginDecryptActive.setToolTipText(Utilities.getResourceString("tooltip.status.plugin_decrypt"));
            lblPluginHostActive.setToolTipText(Utilities.getResourceString("tooltip.status.plugin_host"));
            
            Utilities.addToGridBag(this, lblMessage,             0, 0, 1, 1, 1, 1, new Insets(0,5,0,0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            Utilities.addToGridBag(this, lblSpeed,               1, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.WEST);
            Utilities.addToGridBag(this, lblPluginHostActive,    2, 0, 1, 1, 0, 0, new Insets(0,5,0,0), GridBagConstraints.NONE,       GridBagConstraints.EAST);
            Utilities.addToGridBag(this, lblPluginDecryptActive, 3, 0, 1, 1, 0, 0, new Insets(0,5,0,5), GridBagConstraints.NONE,       GridBagConstraints.EAST);
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
