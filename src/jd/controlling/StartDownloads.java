package jd.controlling;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDUtilities;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
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
    private JDController controller;
    /**
     * Hiermit werden Interaktionen zum laufenden DownloadThread umgesetzt
     * (ZB ein Reconnect)
     */
    private HashMap<Integer, Vector<Interaction>> interactions;
    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller Controller
     * @param interactions Hier sind alle möglichen Interaktionen gespeichert
     */
    public StartDownloads(JDController controller, HashMap<Integer, Vector<Interaction>> interactions){
        super("JD-StartDownloads");
        this.controller = controller;
        this.interactions = interactions;

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
        while((downloadLink = controller.getNextDownloadLink()) != null){
            logger.info("working on "+downloadLink.getName());
            plugin   = downloadLink.getPlugin();
            plugin.init();
            PluginStep step = plugin.getNextStep(downloadLink);
            // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
            // bis entweder null zurückgegeben wird oder ein Fehler auftritt
            fireControlEvent(new ControlEvent(this,ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE));
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
                        String captchaText = JDUtilities.getCaptcha(controller,plugin, (String)step.getParameter());
                        step.setParameter(captchaText);
                        step.setStatus(PluginStep.STATUS_DONE);
                }
                step = plugin.getNextStep(downloadLink);
            }
            fireControlEvent(new ControlEvent(this,ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE));
            if(aborted){
                logger.warning("Thread aborted");
            }
            if(step != null && step.getStatus() == PluginStep.STATUS_ERROR){
                logger.severe("Error occurred while downloading file");
                handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED);
            }
            fireControlEvent(new ControlEvent(this,ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
            handleInteraction(Interaction.INTERACTION_DOWNLOAD_FINISHED);
        }
        fireControlEvent(new ControlEvent(this,ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED));
        handleInteraction(Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED);
    }
    /**
     * Hier werden die Interaktionen durchgeführt
     * 
     * @param interactionID InteraktionsID, die durchgeführt werden soll
     */
    private void handleInteraction(int interactionID){
        Vector<Interaction> localInteractions = interactions.get(interactionID);
        if(localInteractions != null && localInteractions.size()>0){
            Iterator<Interaction> iterator = localInteractions.iterator();
            while(iterator.hasNext()){
                Interaction i = iterator.next();
                if(!i.interact()){
                    logger.severe("interaction failed: "+i);
                }
            }
        }
        
    }
}