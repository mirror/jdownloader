package jd.controlling;

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.JDUtilities;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

public class JDController implements PluginListener, ControlListener, UIListener{
    /**
     * Der Thread, der das Downloaden realisiert
     */
    private StartDownloads download = null;
    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData distributeData = null;
    /**
     * Die Konfiguration
     */
    protected Configuration config = JDUtilities.getConfiguration();
    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface uiInterface;
    /**
     * Die DownloadLinks
     */
    private Vector<DownloadLink> downloadLinks;
    /**
     * Der Logger
     */
    private Logger logger = Plugin.getLogger();
    
    public JDController(UIInterface uiInterface){
        this.uiInterface = uiInterface;
        
        uiInterface.addUIListener(this);
        downloadLinks = new Vector<DownloadLink>();
    }
    /**
     * Startet den Downloadvorgang
     */
    private void startDownloads(){
        if(download == null){
            download = new StartDownloads(this, config.getInteractions());
            download.addControlListener(this);
            download.start();
        }
        else{
            logger.warning("download still active");
        }
    }
    private void stopDownloads(){
        if(download != null){
            download.interrupt();
            download=null;
        }
        else{
            logger.warning("no active download");
        }
    }
    public void pluginEvent(PluginEvent event) {
        uiInterface.pluginEvent(event);
    }
    /**
     * Hier werden ControlEvent ausgewertet
     */
    public void controlEvent(ControlEvent event) {
        switch(event.getID()){
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE:
                uiInterface.setPluginActive((PluginForDecrypt)event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE:
                uiInterface.setPluginActive((PluginForDecrypt)event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE:
                uiInterface.setPluginActive((PluginForHost)event.getParameter(), true);
                break;
            case ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE:
                uiInterface.setPluginActive((PluginForHost)event.getParameter(), false);
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                download = null;
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                if(links != null && links instanceof Vector && ((Vector)links).size()>0){
                    downloadLinks.addAll((Vector<DownloadLink>)links);
                }
                break;
        }
        uiInterface.controlEvent(event);

    }
    
    public void uiEvent(UIEvent uiEvent) {
        switch (uiEvent.getActionID()) {
            case UIEvent.UI_START_DOWNLOADS:
                startDownloads();
                break;
            case UIEvent.UI_STOP_DOWNLOADS:
                stopDownloads();
                break;
            case UIEvent.UI_LINKS_TO_PROCESS:
                String data = (String)uiEvent.getParameter();
                distributeData = new DistributeData(data);
                distributeData.addControlListener(this);
                distributeData.start();
        }
    }
    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    protected Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }
    /**
     * Setzt alle DownloadLinks neu
     * @param links Die neuen DownloadLinks
     */
    protected void setDownloadLinks(Vector<DownloadLink> links) {
        downloadLinks = links;
        
    }
    /**
     * Liefert den nächsten DownloadLink 
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while(iterator.hasNext()){
            nextDownloadLink = iterator.next();
            if(nextDownloadLink.isEnabled() && nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO)
                return nextDownloadLink;
        }
        return null;
    }
    /**
     * Der Benuter soll den Captcha Code erkennen
     * 
     * @param plugin Das Plugin, das den Code anfordert
     * @param captchaAddress Adresse des anzuzeigenden Bildes
     * @return Text des Captchas
     */
    public String getCaptchaCodeFromUser(Plugin plugin, String captchaAddress){
        return uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress);
    }
}
