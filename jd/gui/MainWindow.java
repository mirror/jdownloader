package jd.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
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
    
    private static final StringSelection JDOWNLOADER_ID = new StringSelection("JDownloader active");
    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar                    = new JMenuBar();
    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar          toolBar           = new JToolBar();
    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private Vector<PluginForDecrypt> pluginsForDecrypt = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle Plugins für die Anbieter gespeichert
     */
    private Vector<PluginForHost>    pluginsForHost    = new Vector<PluginForHost>();
    /**
     * Logger für Meldungen des Programmes
     */
    private Logger            logger = Plugin.getLogger();
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
    private JTabbedPane       tabbedPane               = new JTabbedPane();
    private JDAction actionStartDownload = new JDAction("start","start",JDAction.APP_START_NEXT_DOWNLOAD);
    private JDAction actionMoveUp = new JDAction("up","move_up",JDAction.ITEMS_MOVE_UP);
    /**
     * Das Hauptfenster wird erstellt
     */
    public MainWindow(){
        loadImages();
        buildUI();
        getPlugins();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(JDOWNLOADER_ID, this);

        setSize(500,300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt 
     */
    private void buildUI(){
        tabbedPane        = new JTabbedPane();
        tabDownloadTable  = new TabDownloadLinks();
        tabPluginActivity = new TabPluginActivity();
        tabbedPane.addTab(Utilities.getResourceString("label.tab.download"),        tabDownloadTable);
        tabbedPane.addTab(Utilities.getResourceString("label.tab.plugin_activity"), tabPluginActivity);

        JButton start = new JButton(actionStartDownload);
        start.setFocusPainted(false);start.setBorderPainted(false);
        
        toolBar.setFloatable(false);
        toolBar.add(start);

        setLayout(new GridBagLayout());
        Utilities.addToGridBag(this, toolBar,     0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH); 
        Utilities.addToGridBag(this, tabbedPane,  0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH,       GridBagConstraints.CENTER); 
        setJMenuBar(menuBar);
    }
    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages(){
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Utilities.addImage("start",   new ImageIcon(toolkit.getImage(cl.getResource("GIF/start.gif"))));
        Utilities.addImage("move_up", new ImageIcon(toolkit.getImage(cl.getResource("GIF/start.gif"))));
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
            logger.info("Decrypt-Plugin:"+p.getPluginName());
        }

        //Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while(iterator.hasNext())
        {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            p.addPluginListener(tabDownloadTable);
            logger.info("Host-Plugin:"+p.getPluginName());
        }
    }
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
            case JDAction.APP_START_NEXT_DOWNLOAD:
                DownloadLink downloadLink = tabDownloadTable.getNextDownloadLink();
                if (downloadLink != null)
                    new StartDownload(downloadLink).start();
                else
                    logger.severe("error.no_download");
                break;
        }
        
    }
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        new ClipboardHandler().start();
    }
    private class ClipboardHandler extends Thread{
        public void run(){
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            synchronized (clipboard) {
                try {
                    clipboard.wait(500);
                }
                catch (InterruptedException e) { }
                try {
                    String data = (String)clipboard.getData(DataFlavor.stringFlavor);
                    new DistributeData(data).start();
//                    System.out.println(data);
                }
                catch (UnsupportedFlavorException e1) {}
                catch (IOException e1)                {}

                clipboard.setContents(JDOWNLOADER_ID, MainWindow.this);
            }
        }
    }
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
        public static final int APP_START_NEXT_DOWNLOAD = 7;
        public static final int APP_SHOW_LOG            = 8;
        public static final int APP_STOP_DOWNLOADS      = 9;
        
        private int actionID;
        
        public JDAction(String actionName, String iconName, int actionID){
            super();
            ImageIcon icon = Utilities.getImage(iconName);
            putValue(Action.NAME, actionName);
            putValue(Action.SMALL_ICON, icon);
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
         * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll
         * 
         * @param data Daten, die verteilt werden sollen
         */
        public DistributeData (String data){
            this.data = data;
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
                    cryptedLinks.addAll(pDecrypt.getMatches(data));
                    data = pDecrypt.cutMatches(data);
                    decryptedLinks.addAll(pDecrypt.decryptLinks(cryptedLinks));
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
        DownloadLink downloadLink;
        
        public StartDownload(DownloadLink downloadLink){
            this.downloadLink = downloadLink;
        }
        public void run(){
            Plugin plugin   = downloadLink.getPlugin();
            PluginStep step = plugin.getNextStep(downloadLink);
            while(step != null){
                switch(step.getStep()){
                    case PluginStep.WAIT_TIME:
                        try {
                            Thread.sleep((Long)step.getParameter());
                        }
                        catch (InterruptedException e) { e.printStackTrace(); }
                        break;
                        
                }
                step = plugin.getNextStep(downloadLink);
            }
             
        }
    }
}
