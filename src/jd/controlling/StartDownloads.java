package jd.controlling;

import java.io.File;
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
     * Das Plugin, das den aktuellen Download steuert
     */
 private  PluginForHost                         currentPlugin;
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
     * @param controller Controller
     * @param interactions Hier sind alle möglichen Interaktionen gespeichert
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
        if(currentPlugin!=null)currentPlugin.abort();
    }

    public void run() {
        /**
         * Der DownloadLink
         */
        DownloadLink downloadLink;
        /**
         * Das Plugin, das den aktuellen Download steuert
         */
        PluginForHost                         plugin;
        while ((downloadLink = controller.getNextDownloadLink()) != null) {
            logger.info("working on " + downloadLink.getName());
            currentPlugin=plugin = downloadLink.getPlugin();
            plugin.resetPlugin();
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
                            logger.severe("Captchaadresse = null");
                            step.setParameter("");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            break;
                        }
                        else {

                            File dest = JDUtilities.getResourceFile("captchas/" + plugin.getPluginName() + "/captcha_" + (new Date().getTime()) + ".jpg");
                            if(!JDUtilities.download(dest, captchaAdress)||!dest.exists()){
                                logger.severe("Captcha Download fehlgeschlagen: "+captchaAdress);
                                step.setParameter("");
                                step.setStatus(PluginStep.STATUS_ERROR);
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                                break;  
                            }
                            if (plugin.doBotCheck(dest)) {
                                downloadLink.setStatusText("Bot erkannt");
                                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));

                                step.setParameter(null);
                                step.setStatus(PluginStep.STATUS_DONE);
                            }
                            else {
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
                if (step != null && downloadLink != null && plugin != null && plugin.nextStep(step) != null) {
                    downloadLink.setStatusText(plugin.nextStep(step).toString());
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                }

                step = plugin.getNextStep(downloadLink);
            }
            // /Der Download ist an dieser Stelle entweder Beendet oder
            // Abgebrochen. Mögliche Ursachen können nun untersucht werden um
            // den download eventl neu zu starten
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE));
            if (aborted) {
                downloadLink.setStatusText("Abgebrochen");
                plugin.abort();
                logger.warning("Thread aborted");
                downloadLink.setStatus(DownloadLink.STATUS_TODO);                
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                }
              
                clearDownloadListStatus();
             
                return;

            }

            if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT && step.getStatus() == PluginStep.STATUS_ERROR) {

                this.onErrorWaittime(downloadLink, plugin, step);

            }
            else if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_CAPTCHA_WRONG && step.getStatus() == PluginStep.STATUS_ERROR) {
                this.onErrorCaptcha(downloadLink, plugin, step);
            }
            else if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_BOT_DETECTED && step.getStatus() == PluginStep.STATUS_ERROR) {
                this.onErrorBotdetection(downloadLink, plugin, step);

            }
           
            else if (step != null && downloadLink.getStatus() == DownloadLink.STATUS_ERROR_UNKNOWN_RETRY&& step.getStatus() == PluginStep.STATUS_ERROR) {
                this.onErrorRetry(downloadLink, plugin, step);
            }
            else if (step != null && step.getStatus() == PluginStep.STATUS_ERROR) {
                this.onErrorUnknown(downloadLink, plugin, step);
            }
            else {
                downloadLink.setStatusText("Fertig");
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
                handleInteraction(Interaction.INTERACTION_DOWNLOAD_FINISHED, this);

            }

        }

        this.waitForDownloadLinks();
    }
private void clearDownloadListStatus() {
    Vector<DownloadLink> links;
   
    logger.finer("Clear");
    links = controller.getDownloadLinks();   
    for (int i = 0; i < links.size(); i++) {
        if( links.elementAt(i).getStatus()!=DownloadLink.STATUS_DONE){
             
        links.elementAt(i).setStatusText("");
        links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
        }
      
    }
    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
    
}

/**
 * Diese Funktion wird aufgerufen wenn der Download abgebrochen wurde und wiederholt werden soll
 * @param downloadLink
 * @param plugin2
 * @param step
 */
    private void onErrorRetry(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
       
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
        
    }

    /**
     * Diese Funktion wird aufgerufen wenn ein Download durch einen unbekannten
     * fehler abgebrochen wurde
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorUnknown(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText("Unbekannter Fehler");
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
        logger.severe("Error occurred while downloading file");
        handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);

    }

    /**
     * Diese Funktion wird aufgerufen sobald ein Download wegen einer
     * Botdetection abgebrochen wird
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorBotdetection(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        // Bot erkannt. Interaction!
        logger.severe("Error occurred: Bot detected");
        downloadLink.setStatusText("Bot erkannt");
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
        if (handleInteraction(Interaction.INTERACTION_DOWNLOAD_WAITTIME, this)) {
            
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
        }

    }

    /**
     * Diese Funktion wird aufgerufen wenn ein Download wegen eines
     * captchafehlersabgebrochen wird
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorCaptcha(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Captcha Wrong");
        // captcha Falsch. Download wiederholen
        downloadLink.setStatusText("Code falsch");
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
       
        downloadLink.setStatus(DownloadLink.STATUS_TODO);

    }

    /**
     * Diese Funktion wird aufgerufen wenn Ein Download mit einem Waittimefehler
     * abgebrochen wird
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorWaittime(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Wait Time " + step);
        long milliSeconds = (Long) step.getParameter();
        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText("");
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));
        // Download Zeit. Versuch durch eine INteraction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen
        if (handleInteraction(Interaction.INTERACTION_DOWNLOAD_WAITTIME, this)) {
          
            
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
        }

    }

    /**
     * Diese Methode prüft wiederholt die Downloadlinks solange welche dabei
     * sind die Wartezeit haben. Läuft die Wartezeit ab, oder findet ein
     * reconnect statt, wird wieder die Run methode aufgerifen
     */
    private void waitForDownloadLinks() {
        Vector<DownloadLink> links;
        DownloadLink link;
        boolean hasWaittimeLinks = false;
        logger.finer("Wait");
        boolean returnToRun = false;

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {

            e.printStackTrace();
        }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED));

        links = controller.getDownloadLinks();

        for (int i = 0; i < links.size(); i++) {
            link = links.elementAt(i);
            if (!link.isEnabled()) continue;
            // Link mit Wartezeit in der queue
            if (link.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT) {
                if (link.getRemainingWaittime() == 0) {
                   
                    link.setStatus(DownloadLink.STATUS_TODO);
                    link.setEndOfWaittime(0);
                    returnToRun = true;

                }
              
                hasWaittimeLinks = true;
                // Neuer Link hinzugefügt
            }
            else if (link.getStatus() == DownloadLink.STATUS_TODO) {
                returnToRun = true;
            }

        }
        if (aborted) {
            clearDownloadListStatus();
            logger.warning("Thread aborted");
            return;

        }
        else if (returnToRun) {
            run();
            return;
        }

        if (!hasWaittimeLinks) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED));
            handleInteraction(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL, this);
            logger.info("Alle Downloads beendet");
        }
        else {
            waitForDownloadLinks();
        }

    }

    /**
     * Hier werden die Interaktionen durchgeführt
     * 
     * @param interactionID InteraktionsID, die durchgeführt werden soll
     */
    private boolean handleInteraction(int interactionID, Object arg) {
//fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE, this));
        boolean ret = true;
        Vector<Interaction> localInteractions = interactions.get(interactionID);
        if (localInteractions != null && localInteractions.size() > 0) {
            Iterator<Interaction> iterator = localInteractions.iterator();

            while (iterator.hasNext()) {
                Interaction i = iterator.next();
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE,i));
                if (!i.interact(this)) {
                    ret = false;
                    logger.severe("interaction failed: " + i);
                }
                else {
                    logger.info("interaction successfull: " + i);
                }
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE,i));
           ;
            }
        }
        else {
            return false;
        }

        return ret;

    }
}