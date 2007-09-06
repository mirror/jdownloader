package jd.controlling;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
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
public class StartDownloads extends ControlMulticaster {
    /**
     * Der DownloadLink
     */
    private DownloadLink                          downloadLink;
    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost                         plugin;
    /**
     * Wurde der Download abgebrochen?
     */
    private boolean                               aborted = false;
    /**
     * Der Logger
     */
    private Logger                                logger  = Plugin.getLogger();
    /**
     * Das übergeordnete Fenster
     */
    private JDController                          controller;
    /**
     * Hiermit werden Interaktionen zum laufenden DownloadThread umgesetzt (ZB
     * ein Reconnect)
     */
    private HashMap<Integer, Vector<Interaction>> interactions;

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param interactions
     *            Hier sind alle möglichen Interaktionen gespeichert
     */
    public StartDownloads(JDController controller, HashMap<Integer, Vector<Interaction>> interactions) {
        super("JD-StartDownloads");
        this.controller = controller;
        this.interactions = interactions;

    }

    /**
     * Bricht den Downloadvorgang ab
     */
    public void abortDownload() {
        aborted = true;
        if (plugin != null)
            plugin.abort();
    }

    public void run() {
        while ((downloadLink = controller.getNextDownloadLink()) != null) {
            logger.info("working on " + downloadLink.getName());
            plugin = downloadLink.getPlugin();
            downloadLink.setStatusText("aktiv");
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
            plugin.init();
            PluginStep step = plugin.getNextStep(downloadLink);
            // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
            // bis entweder null zurückgegeben wird oder ein Fehler auftritt

            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE));
            while (!aborted && step != null && step.getStatus() != PluginStep.STATUS_ERROR) {
                logger.info("Current Step:  " + step);
                switch (step.getStep()) {
             
                    case PluginStep.STEP_CAPTCHA:
                        downloadLink.setStatusText("Captcha");
                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                        String captchaAdress = (String) step.getParameter();
                        if (captchaAdress == null) {
                            step.setParameter("");
                            step.setStatus(PluginStep.STATUS_DONE);
                        } else {

                            File dest = JDUtilities.getResourceFile("captchas/" + plugin.getPluginName() + "/captcha_" + (new Date().getTime()) + ".jpg");
                            JDUtilities.download(dest, captchaAdress);
                            if (plugin.doBotCheck(dest)) {
                                downloadLink.setStatusText("Bot erkannt");
                                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));

                                step.setParameter(null);
                                step.setStatus(PluginStep.STATUS_DONE);
                            } else {
                                String captchaText = JDUtilities.getCaptcha(controller, plugin, dest);
                                logger.info("CaptchaCode: " + captchaText);
                                downloadLink.setStatusText("Code: " + captchaText);
                                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                                step.setParameter(captchaText);
                                step.setStatus(PluginStep.STATUS_DONE);
                            }
                          
                        }
                        break;
                 
                }

             
                downloadLink.setStatusText(plugin.nextStep(step).toString());
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                
                step = plugin.getNextStep(downloadLink);
            }
            ///Der Download ist an dieser Stelle entweder Beendet oder Abgebrochen. Mögliche Ursachen können nun untersucht werden um den download eventl neu zu starten
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE));
            if (aborted) {
                downloadLink.setStatusText("Abgebrochen");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                logger.warning("Thread aborted");
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                plugin.resetSteps();
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                return;

            }
           
            if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT && step.getStatus() == PluginStep.STATUS_ERROR) {
                logger.severe("Error occurred: Wait Time "+step);
                long  milliSeconds= (Long) step.getParameter();               
                downloadLink.setEndOfWaittime(System.currentTimeMillis()+milliSeconds);   
                downloadLink.setStatusText("Wartezeit");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                //Download Zeit. Versuch durch eine INteraction einen reconnect zu machen. wenn das klappt nochmal versuchen
                if (handleInteraction(Interaction.INTERACTION_DOWNLOAD_WAITTIME, downloadLink)) {
                    plugin.resetSteps();
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                    downloadLink.setEndOfWaittime(0);
                }
                
            } else if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_CAPTCHA_WRONG && step.getStatus() == PluginStep.STATUS_ERROR) {
                logger.severe("Error occurred: Captcha Wrong");
                //captcha Falsch. Download wiederholen
                downloadLink.setStatusText("Code falsch");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                plugin.resetSteps();
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
            } else if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_BOT_DETECTED && step.getStatus() == PluginStep.STATUS_ERROR) {
                //Bot erkannt. Interaction!
                logger.severe("Error occurred: Bot detected");
                downloadLink.setStatusText("Bot erkannt");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                if (handleInteraction(Interaction.INTERACTION_DOWNLOAD_WAITTIME, downloadLink)) {
                    plugin.resetSteps();
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                }
              
            } else if (step != null && step.getStatus() == PluginStep.STATUS_ERROR) {
                downloadLink.setStatusText("Unbekannter Fehler");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                logger.severe("Error occurred while downloading file");
                handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, downloadLink);
            }else{
            downloadLink.setStatusText("Fertig");
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
            handleInteraction(Interaction.INTERACTION_DOWNLOAD_FINISHED, downloadLink);
           
            }
           
          
              
        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED));
        handleInteraction(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL, null);
        logger.info("Alle Downloads beendet");
    }

    /**
     * Hier werden die Interaktionen durchgeführt
     * 
     * @param interactionID
     *            InteraktionsID, die durchgeführt werden soll
     */
    private boolean handleInteraction(int interactionID, Object arg) {

        boolean ret = true;
        Vector<Interaction> localInteractions = interactions.get(interactionID);
        if (localInteractions != null && localInteractions.size() > 0) {
            Iterator<Interaction> iterator = localInteractions.iterator();
            if(!iterator.hasNext())return false;
            while (iterator.hasNext()) {
                Interaction i = iterator.next();
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE, i));
                if (!i.interact(arg)) {
                    ret = false;
                    logger.severe("interaction failed: " + i);
                } else {
                    logger.info("interaction successfull: " + i);
                }
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE, i));
            }
        }

        return ret;

    }
}