package jd.controlling;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JFrame;

import jd.Configuration;
import jd.JDCrypt;
import jd.JDUtilities;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
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
    
    public JDController(){
        downloadLinks = new Vector<DownloadLink>();
        loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
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
            download.abortDownload();
//            download.interrupt();
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
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                download = null;
                uiInterface.controlEvent(event);
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                if(links != null && links instanceof Vector && ((Vector)links).size()>0){
                    downloadLinks.addAll((Vector<DownloadLink>)links);
                }
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                uiInterface.setDownloadLinks(downloadLinks);
                break;
            default:
                uiInterface.controlEvent(event);
                break;
        }
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
                break;
            case UIEvent.UI_SAVE_CONFIG:
                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), "jdownloader", ".config", true);
                break;
            case UIEvent.UI_SAVE_LINKS:
                File file = (File)uiEvent.getParameter();             
                saveDownloadLinks(file);
                break;
            case UIEvent.UI_LOAD_LINKS:               
                file = (File)uiEvent.getParameter();               
                loadDownloadLinks(file);
             
                break;
            case UIEvent.UI_LINKS_TO_REMOVE:
                DownloadLink link = (DownloadLink)uiEvent.getParameter();
                int index= downloadLinks.indexOf(link);
                downloadLinks.remove(index);
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                uiInterface.setDownloadLinks(downloadLinks);
                break;
        }
    }
  
    /**
     * @author coalado
     * Speichert die Linksliste  ab
     * @param file
     */
    public void saveDownloadLinks(File file){      
        String[] linkList=new String[downloadLinks.size()];
        for (int i=0;i<linkList.length;i++){
            linkList[i]=downloadLinks.elementAt(i).getEncryptedUrlDownload();
        }
        JDUtilities.saveObject(new JFrame(), linkList, file, "links", "dat", false);
    }
    /**
     * @author coalado
     * Lädt eine Linkliste
     * @param file
     */
    public void loadDownloadLinks(File file){
        if(!file.exists())return;
        String[] linkList=(String[])JDUtilities.loadObject(new JFrame(),file, false);
        if(linkList!=null) {
        for (int i=0;i<linkList.length;i++){
            distributeData = new DistributeData(JDCrypt.decrypt(linkList[i]));
            distributeData.addControlListener(this);
            distributeData.start();
            
        }
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
            if(nextDownloadLink.isEnabled() && nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO&&nextDownloadLink.getRemainingWaittime()==0)
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
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress){
        return uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress);
    }
    public void setUiInterface(UIInterface uiInterface) {
        if(this.uiInterface != null)
            this.uiInterface.removeUIListener(this);
        this.uiInterface = uiInterface;
        uiInterface.addUIListener(this);
    }
}
