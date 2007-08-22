package jd.gui;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JFrame;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.ClipboardHandler;
import jd.controlling.StartDownloads;
import jd.controlling.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginListener;

public abstract class GUIInterface implements PluginListener, ControlListener, ClipboardOwner{
    /**
     * Titel der Applikation
     */
    private final String JD_TITLE  = "jDownloader 0.0.1";
    /**
     * Icon der Applikation
     */
    private final Image JD_ICON = JDUtilities.getImage("mind");
    /**
     * Logger für Meldungen des Programmes
     */
    protected static Logger logger = Plugin.getLogger();
    /**
     * Der Thread, der das Downloaden realisiert
     */
    protected StartDownloads download = null;
    /**
     * Die Konfiguration
     */
    protected Configuration config = JDUtilities.getConfiguration();
    public GUIInterface(){
        logger.info("Version : "+Plugin.VERSION);

        JFrame frame = getFrame();
        if(frame!= null){
            frame.setIconImage(JD_ICON);
            frame.setTitle(JD_TITLE);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }
    /**
     * Methode, um eine Veränderung der Zwischenablage zu bemerken und zu verarbeiten
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        new ClipboardHandler(this).start();
    }
    /**
     * Verarbeitet Aktionen
     * 
     * @param actionID eine Aktion
     */
    public abstract void doAction(int actionID);
    /**
     * Liefert das Hauptfenster zurück oder null
     * @return Das Hauptfenster
     */
    public abstract JFrame getFrame();
    /**
     * Liefert den nächsten DownloadLink 
     * 
     * @return Der nächste DownloadLink oder null
     */
    public abstract DownloadLink getNextDownloadLink();
    /**
     * Liefert alle verfügbaren DownloadLinks zurück
     * 
     * @return Alle verfügbaren DownloadLinks
     */
    protected abstract Vector<DownloadLink> getDownloadLinks();
    /**
     * Setzt diese Links in die Tabelle zum Downloaded, und ersetzt alle vorherigen
     * 
     * @param links Herunterzuladende DownloadLinks
     */
    protected abstract void setDownloadLinks(Vector<DownloadLink> links);
    /**
     * Startet oder stoppt den Downloadvorgang
     */
    protected void startStopDownloads(){
        if(download == null){
            download = new StartDownloads(this, config.getInteractions());
            download.addControlListener(this);
            download.start();
        }
        else{
            download.interrupt();
            download=null;
        }
    }
    /**
     * Speichert alle Links in eine Datei
     */
    protected void saveLinks(){
        Vector<DownloadLink> links = getDownloadLinks();
        JDUtilities.saveObject(getFrame(),links,null,"jd",".links");
    }
    /**
     * Lädt alle Links aus einer Datei
     */
    protected void loadLinks(){
        PluginForHost neededPlugin;
        Object obj = JDUtilities.loadObject(getFrame(),null);
        if(obj instanceof Vector){
            Vector<DownloadLink> links = (Vector<DownloadLink>)obj;
            for(int i=0;i<links.size();i++){
                neededPlugin = JDUtilities.getPluginForHost(links.get(i).getHost());
                links.get(i).setPlugin(neededPlugin);
            }
            setDownloadLinks(links);
        }
    }
    protected void saveConfig(){
        JDUtilities.saveObject(getFrame(), config, new File(JDUtilities.CONFIG_PATH),"jd","config");
    }
}
