package jd.controlling;

import java.io.File;
import java.util.logging.Logger;
import jd.config.Configuration;
import jd.controlling.interaction.CaptchaMethodLoader;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.gui.skins.simple.AgbDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 * 
 * @author astaldo/JD-Team
 */
public class SingleDownloadController extends ControlMulticaster {
    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost currentPlugin;

    /**
     * Wurde der Download abgebrochen?
     */
    private boolean       aborted = false;

    /**
     * Der Logger
     */
    private Logger        logger  = JDUtilities.getLogger();

    /**
     * Das übergeordnete Fenster
     */
    private JDController  controller;

    private DownloadLink  downloadLink;

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller Controller
     * @param dlink Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(JDController controller, DownloadLink dlink) {
        super("JD-StartDownloads");
        this.downloadLink = dlink;
        this.controller = controller;
    }

    /**
     * Bricht den Downloadvorgang ab.
     */
    public void abortDownload() {
        downloadLink.setStatusText(JDLocale.L("controller.status.termination", "termination..."));
        aborted = true;
        if (currentPlugin != null) currentPlugin.abort();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    public void run() {
        /**
         * Das Plugin, das den aktuellen Download steuert
         */
        PluginForHost plugin;
        logger.info("working on " + downloadLink.getName());
        currentPlugin = plugin = (PluginForHost) downloadLink.getPlugin();
        plugin.resetPlugin();

        if (downloadLink.getDownloadURL() == null) {

            downloadLink.setStatusText(JDLocale.L("controller.status.containererror", "Container Fehler"));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_SECURITY);
            downloadLink.setInProgress(false);
            Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED, downloadLink));

            return;

        }
        downloadLink.setStatusText(JDLocale.L("controller.status.active", "aktiv"));
        downloadLink.setInProgress(true);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        plugin.init();

        PluginStep step = plugin.doNextStep(downloadLink);

        // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
        // bis entweder null zurückgegeben wird oder ein Fehler auftritt
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE, plugin));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_STARTS, downloadLink));
        while (!aborted && step != null && step.getStatus() != PluginStep.STATUS_ERROR) {

            downloadLink.setStatusText(JDLocale.L("controller.status.running", "running..."));
            if (step.getStatus() != PluginStep.STATUS_SKIP) {
                switch (step.getStep()) {
                    case PluginStep.STEP_PENDING:
                        long wait = (Long) step.getParameter();
                        logger.info("Erzwungene Wartezeit: " + wait);
                        while (wait > 0 && !aborted) {
                            downloadLink.setStatusText(JDUtilities.sprintf(JDLocale.L("controller.status.mustWaittime", "Erzwungene Wartezeit: %s"), new String[] { JDUtilities.formatSeconds((int) (wait / 1000)) }));
                            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                            }
                            wait -= 1000;
                        }
                        break;
                    case PluginStep.STEP_GET_CAPTCHA_FILE:
                        downloadLink.setStatusText(JDLocale.L("controller.status.captcha", "Captcha"));
                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                        File captcha = null;
                        logger.severe("GOGO JAC!");
                        if (step.getParameter() != null && step.getParameter() instanceof File) {
                            captcha = (File) step.getParameter();
                        }
                        if (captcha == null) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            logger.info("Captcha == null");
                            break;
                        }
                        else {
                            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_CAPTCHA_LOADED, captcha));
                            downloadLink.setLatestCaptchaFile(captcha);

                            if (plugin.doBotCheck(captcha)) {
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                                step.setStatus(PluginStep.STATUS_ERROR);
                                step.setParameter(null);
                                break;
                            }

                            String captchaText = Plugin.getCaptchaCode(captcha, plugin);
                            logger.info("CaptchaCode: " + captchaText+" set in "+step);
                            downloadLink.setStatusText(JDLocale.L("controller.status.captchacode", "Code: ") + captchaText);
                            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                            step.setParameter(captchaText);
                            step.setStatus(PluginStep.STATUS_DONE);

                        }
                        break;
                }
              
            }
            if (aborted) {
                break;
            }
            if (step != null && downloadLink != null && plugin != null && plugin.nextStep(step) != null) {
                downloadLink.setStatusText(plugin.nextStep(step).toString());
                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
            }
            // Bricht ab wenn es Fehler gab
            if (step.getStatus() == PluginStep.STATUS_ERROR) {
                logger.severe("Error detected");
                break;
            }

            step = plugin.doNextStep(downloadLink);
        }
        // Der Download ist an dieser Stelle entweder Beendet oder
        // Abgebrochen. Mögliche Ursachen können nun untersucht werden um
        // den download eventl neu zu starten
        if (aborted) {
            downloadLink.setStatusText(JDLocale.L("controller.status.aborted", "Abgebrochen"));
            plugin.abort();
            logger.warning("Thread aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
            }
            downloadLink.setInProgress(false);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE, plugin));
            return;
        }
        if (step != null && step.getStatus() == PluginStep.STATUS_ERROR) {

            switch (downloadLink.getStatus()) {
                case DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT:
                    this.onErrorWaittime(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_STATIC_WAITTIME:
                    this.onErrorStaticWaittime(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE:
                    this.onErrorTemporarilyUnavailable(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_TO_MANY_USERS:
                    this.onErrorTooManyUsers(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR:
                    this.onErrorCaptchaImage(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_ABUSED:
                    this.onErrorAbused(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_NOT_UPLOADED:
                    this.onErrorNotUploaded(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_AGB_NOT_SIGNED:
                    this.onErrorAGBNotSigned(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_UNKNOWN_RETRY:
                    this.onErrorRetry(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_NOT_FOUND:
                    this.onErrorFileNotFound(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_CAPTCHA_WRONG:
                    this.onErrorCaptcha(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_PREMIUM:
                    this.onErrorPremium(downloadLink, plugin, step);
                    break;

                case DownloadLink.STATUS_ERROR_NO_FREE_SPACE:
                    this.onErrorNoFreeSpace(downloadLink, plugin, step);
                    break;

                case DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC:
                    this.onErrorPluginSpecific(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_BOT_DETECTED:
                    this.onErrorBotdetection(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_DOWNLOAD_INCOMPLETE:
                    this.onErrorIncomplete(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_ALREADYEXISTS:
                    this.onErrorFileExists(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK:
                    this.onErrorFileInProgress(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED:
                    this.onErrorChunkloadFailed(downloadLink, plugin, step);
                    break;   
                    
                default:
                    logger.info("Uknown error id: " + downloadLink.getStatus());
                    this.onErrorUnknown(downloadLink, plugin, step);
            }
            downloadLink.setInProgress(false);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE, plugin));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED, downloadLink));

        }
        else {
            downloadLink.setStatusText(JDLocale.L("controller.status.finished", "Fertig"));
            downloadLink.setInProgress(false);
           if(downloadLink.getStatus()!=DownloadLink.STATUS_DONE){
               logger.severe("Pluginerror: Step returned null and Downloadlink status != STATUS_DONE");
               downloadLink.setStatus(DownloadLink.STATUS_DONE);
           }
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE, plugin));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED, downloadLink));
            Interaction.handleInteraction((Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED), downloadLink);
            if (JDUtilities.getController().isContainerFile(new File(downloadLink.getFileOutput()))) {
                Interaction.handleInteraction((Interaction.INTERACTION_CONTAINER_DOWNLOAD), downloadLink);
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) controller.loadContainerFile(new File(downloadLink.getFileOutput()));
            }
            if (JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLED, true)) controller.getUnrarModule().interact(downloadLink);

        }

    }

    private void onErrorChunkloadFailed(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
       
        logger.severe("Chunkload failed: ");
       downloadLink.setStatusText(JDLocale.L("controller.status.chunkloadfailed", "Multidownload fehlgeschlagen"));

        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        
    }

    private void onErrorFileInProgress(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        downloadLink.setEnabled(false);
        downloadLink.setStatusText(JDLocale.L("controller.status.fileinprogress", "Datei wird schon geladen"));
        downloadLink.setInProgress(false);
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

    }

    private void onErrorIncomplete(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        downloadLink.setInProgress(false);
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);

    }

    private void onErrorFileExists(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {

        String todo = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_FILE_EXISTS, JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));

        if (todo.equals(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"))) {
            downloadLink.setEnabled(false);
            downloadLink.setStatusText(JDLocale.L("controller.status.fileexists.skip", "Datei schon vorhanden"));
            downloadLink.setInProgress(false);
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        }
        else {
            if (new File(downloadLink.getFileOutput()).delete()) {
                downloadLink.setInProgress(false);
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                downloadLink.setEndOfWaittime(0);
            }
            else {
                downloadLink.setStatusText(JDLocale.L("controller.status.fileexists.overwritefailed", "Überschreiben fehlgeschlagen ") + downloadLink.getFileOutput());

            }

        }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

    }

    private void onErrorAGBNotSigned(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {

        downloadLink.setStatusText(JDLocale.L("controller.status.agb_tos", "AGB nicht akzeptiert"));
        new AgbDialog(downloadLink2);

        /*
         * if(JDController.FLAGS.getIntegerProperty("AGBMESSAGESIGNED_"+plugin.getHost(),
         * 0)==0){
         * JDController.FLAGS.setProperty("AGBMESSAGESIGNED_"+plugin.getHost(),
         * 1); String title=JDLocale.L("gui.dialogs.agb_tos_warning_title",
         * "Allgemeinen Geschäftsbedingungen nicht aktzeptiert"); String
         * message=JDLocale.L("gui.dialogs.agb_tos_warning_text", "<p><font
         * size=\"3\"><strong><font size=\2\" face=\"Verdana, Arial,
         * Helvetica, sans-serif\">Die Allgemeinen Geschäftsbedingungen (AGB)</font></strong><font
         * size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\"><br/>
         * wurden nicht gelesen und akzeptiert.</font></font></p><p><font
         * size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\"><br/>
         * Anbieter: </font></p>")+plugin.getHost(); String
         * url="http://www.the-lounge.org/viewtopic.php?f=222&t=8842";
         * JDUtilities.getGUI().showHelpMessage(title, message, url); }
         */

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

    }

    private void onErrorPluginSpecific(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        String message = (String) step.getParameter();
        logger.severe("Error occurred: " + message);
        if (message != null) downloadLink.setStatusText(message);

        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

    }

    private void onErrorNoFreeSpace(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.freespace", "Zu wenig Speicherplatz"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

    }

    /**
     * Wird aufgerufen wenn ein Link kurzzeitig nicht verfügbar ist. ER wird
     * deaktiviert und kann zu einem späteren zeitpunkt wieder aktiviert werden
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorTemporarilyUnavailable(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Temporarily unavailably");
        // long milliSeconds = (Long) step.getParameter();
        // downloadLink.setEndOfWaittime(System.currentTimeMillis() +
        // milliSeconds);
        downloadLink.setStatusText(JDLocale.L("controller.status.tempUnavailable", "kurzzeitig nicht verfügbar"));
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
        }

        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Wiurd aufgerufen wenn ein Link grundsätzlich online ist, aber der Server
     * mommentan zu sehr ausgelastet ist
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorTooManyUsers(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Temporarily to many users");
        long milliSeconds=20*60*1000;
        try{
        milliSeconds= (Long) step.getParameter();
        }catch(Exception e){}
        
        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText(JDLocale.L("controller.status.toManyUser", "ausgelastet")+" ");

        //downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Fehlerfunktion für einen UNbekannten premiumfehler.
     * Plugin-premium-support wird deaktiviert und link wird erneut versucht
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorPremium(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.info("deaktivier PREMIUM für: " + plugin + " Grund: Unbekannt");
        plugin.getProperties().setProperty(Plugin.PROPERTY_USE_PREMIUM, false);

        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
    }

    /**
     * Wird aufgerufen wenn Das Plugin eine Immer gleiche Wartezeit meldet. z.B.
     * bei unbekannter Wartezeit
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorStaticWaittime(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Static Wait Time " + step);
        long milliSeconds;
        if (step.getParameter() != null) {
            milliSeconds = (Long) step.getParameter();
        }
        else {
            milliSeconds = 10000;
        }
        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText(JDLocale.L("controller.status.reconnect", "Reconnect "));
        // downloadLink.setInProgress(true);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        // Download Zeit. Versuch durch eine Interaction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen

        // Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT),
        // this);
        // Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME),
        // this);
        if (controller.reconnect()) {
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
        }

        downloadLink.setStatusText("");
    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.filenotfound", "File Not Found"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Wird bei einem File abused Fehler aufgerufen
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorAbused(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.abused", "File Abused"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Wird aufgerufen wenn eine Datei nicht fertig upgeloaded wurde
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorNotUploaded(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.incompleteUpload", "File not full uploaded"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Wird aufgerufen wenn das Captchabild nicht geladen werden konnte
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorCaptchaImage(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.captchaError", "Captcha Fehler"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }

    /**
     * Diese Funktion wird aufgerufen wenn der Download abgebrochen wurde und
     * wiederholt werden soll
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorRetry(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        if (step.getParameter() != null) {
            logger.info("step.getParameter() " + step.getParameter());
            long milliSeconds = (Long) step.getParameter();
            downloadLink.setStatusText(JDUtilities.sprintf(JDLocale.L("controller.status.wait", "Warten: %s sek."), new String[] { JDUtilities.formatSeconds((int) (milliSeconds / 1000)) }));
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));
            try {
                Thread.sleep(milliSeconds);
            }
            catch (InterruptedException e) {
            }
        }
        downloadLink.setInProgress(false);
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
        downloadLink.setStatusText(JDLocale.L("controller.status.unknownError", "Unbekannter Fehler"));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        logger.severe("Error occurred while downloading file");
        downloadLink.setInProgress(false);
        Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);
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

        downloadLink.setStatusText(JDLocale.L("controller.status.botDetected", "Bot erkannt/Reconnect"));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        new CaptchaMethodLoader().interact(plugin.getHost());
        if (plugin.getBotWaittime() < 0 && controller.reconnect()) {
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
        }
        else if (plugin.getBotWaittime() > 0) {

            downloadLink.setEndOfWaittime(System.currentTimeMillis() + plugin.getBotWaittime());
            downloadLink.setStatusText(JDLocale.L("controller.status.botWait", "Botwait "));

            downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);

            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));

        }

        logger.severe("Bot detected");
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
        downloadLink.setStatusText(JDLocale.L("controller.status.captchaFailed", "Code falsch"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
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
        downloadLink.setStatusText(JDLocale.L("controller.status.reconnect", "Reconnect "));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        // Download Zeit. Versuch durch eine Interaction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen

        if (controller.reconnect()) {
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
        }

        downloadLink.setStatusText("");
    }

    public DownloadLink getDownloadLink() {
        return this.downloadLink;
    }
}