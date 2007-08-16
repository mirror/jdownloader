package jd.controlling;

import java.awt.Frame;
import java.util.logging.Logger;

import jd.controlling.event.ControlEvent;
import jd.gui.TabDownloadLinks;
import jd.gui.Utilities;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 *
 * @author astaldo
 */
public class StartDownloads extends ControlMulticaster{
    /**
     * Der DownloadLink
     */
    private DownloadLink downloadLink;
    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost plugin;
    /**
     * Wurde der Download abgebrochen?
     */
    private boolean aborted=false;
    /**
     * Der Logger
     */
    private Logger logger = Plugin.getLogger();
    /**
     * Das übergeordnete Fenster
     */
    private Frame owner;
    /**
     * Die Komponente mit den Downloadlinks
     */
    private TabDownloadLinks tabDownloadLinks;
    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param owner Das übergeordnete Fenster
     * @param tabDownloadLinks Die Komponente, mit den Downloadlinks
     */
    public StartDownloads(Frame owner, TabDownloadLinks tabDownloadLinks){
        super("JD-StartDownloads");
        this.tabDownloadLinks = tabDownloadLinks;
        this.owner = owner;
    }
    /**
     * Bricht den Downloadvorgang ab
     */
    public void abortDownload(){
        aborted=true;
        if(plugin != null)
            plugin.abort();
    }
    public void run(){

        while((downloadLink = tabDownloadLinks.getNextDownloadLink()) != null){
            downloadLink = tabDownloadLinks.getNextDownloadLink();
            logger.info("working on "+downloadLink.getName());
            plugin   = downloadLink.getPlugin();
            plugin.init();
            PluginStep step = plugin.getNextStep(downloadLink);
            // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
            // bis entwerder null zurückgegeben wird oder ein Fehler auftritt
            fireControlEvent(new ControlEvent(ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE));
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
                        String captchaText = Utilities.getCaptcha(owner,plugin, (String)step.getParameter());
                        step.setParameter(captchaText);
                        step.setStatus(PluginStep.STATUS_DONE);
                }
                step = plugin.getNextStep(downloadLink);
            }
            fireControlEvent(new ControlEvent(ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE));
            if(aborted){
                logger.warning("Thread aborted");
            }
            if(step != null && step.getStatus() == PluginStep.STATUS_ERROR){
                logger.severe("Error occurred while downloading file");
            }
            fireControlEvent(new ControlEvent(ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
        }
        fireControlEvent(new ControlEvent(ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED));
    }
}