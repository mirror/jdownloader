package jd.controlling;

import java.io.File;
import java.util.logging.Logger;

import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 * 
 * @author astaldo/coalado
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
    private Logger        logger  = Plugin.getLogger();
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
        downloadLink.setStatusText("termination...");
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
        downloadLink.setStatusText("aktiv");
        downloadLink.setInProgress(true);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        plugin.init();
        PluginStep step = plugin.doNextStep(downloadLink);
        // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
        // bis entweder null zurückgegeben wird oder ein Fehler auftritt
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_ACTIVE, plugin));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_STARTS, downloadLink));
        while (!aborted && step != null && step.getStatus() != PluginStep.STATUS_ERROR) {
            logger.info("Current Step:  " + step);
            downloadLink.setStatusText("running...");
            switch (step.getStep()) {
                case PluginStep.STEP_PENDING:
                    long wait = (Long) step.getParameter();
                    logger.info("Erzwungene Wartezeit: " + wait);
                    while (wait > 0 && !aborted) {
                        downloadLink.setStatusText("Erzwungene Wartezeit: " + JDUtilities.formatSeconds((int) (wait / 1000)));
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
                    downloadLink.setStatusText("Captcha");
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                    File captcha = null;
                    if (step.getParameter() != null && step.getParameter() instanceof File) {
                        captcha = (File) step.getParameter();
                    }
                    if (captcha == null) {
                        logger.severe("Captchaadresse = null");
                        step.setParameter("");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
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
                        // führt die erste Interaction zum Captcha
                        // decoden aus.
                        if (!Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_CAPTCHA), downloadLink, 0)) {
                            String captchaText = JDUtilities.getCaptcha(controller, plugin, captcha);
                            logger.info("CaptchaCode: " + captchaText);
                            downloadLink.setStatusText("Code: " + captchaText);
                            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                            step.setParameter(captchaText);
                            step.setStatus(PluginStep.STATUS_DONE);
                        }
                        else {
                            Interaction[] interacts = Interaction.getInteractions(Interaction.INTERACTION_DOWNLOAD_CAPTCHA);
                            if (interacts.length > 0) {
                                String captchaText = (String) interacts[0].getProperty("captchaCode");
                                if (captchaText == null) {
                                    // im NOtfall doch JAC nutzen
                                    captchaText = JDUtilities.getCaptcha(controller, plugin, captcha);
                                }
                                logger.info("CaptchaCode: " + captchaText);
                                downloadLink.setStatusText("Code: " + captchaText);
                                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
                                step.setParameter(captchaText);
                                step.setStatus(PluginStep.STATUS_DONE);
                            }
                        }
                    }
                    break;
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
                break;
            }
            step = plugin.doNextStep(downloadLink);
        }
        // Der Download ist an dieser Stelle entweder Beendet oder
        // Abgebrochen. Mögliche Ursachen können nun untersucht werden um
        // den download eventl neu zu starten
        if (aborted) {
            downloadLink.setStatusText("Abgebrochen");
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
                    this.onErrorToManyUsers(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR:
                    this.onErrorCaptchaImage(downloadLink, plugin, step);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_ABUSED:
                    this.onErrorAbused(downloadLink, plugin, step);
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
                case DownloadLink.STATUS_ERROR_PREMIUM_LOGIN:
                    this.onErrorPremiumLogin(downloadLink, plugin, step);
                    break;
                default:
                    this.onErrorUnknown(downloadLink, plugin, step);
            }
        }
        else {
            downloadLink.setStatusText("Fertig");
            downloadLink.setInProgress(false);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
            Interaction.handleInteraction((Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED), downloadLink);
        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_HOST_INACTIVE, plugin));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED, downloadLink));
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
        downloadLink.setStatusText("temp. Unavailable");
        downloadLink.setInProgress(false);
        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }
    /**
     * Wiurd aufgerufen wenn ein Link grundsätzlich online ist, aber der Server mommentan zu sehr ausgelastet ist
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorToManyUsers(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: Temporarily to many users");
        long milliSeconds = (Long) step.getParameter();
        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText("ausgelastet");
        downloadLink.setInProgress(false);
        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
    }
    /**
     * Wird aufgerufen wenn ein Loginfehler bei premiumaccounts auftritt. Die
     * premiumnutzung beim Plugin wird deaktiviert und der Link wird nochmals
     * versucht
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorPremiumLogin(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        // premium abschalten.
        logger.info("deaktivier PREMIUM für: " + plugin + " Grund: Logins falsch");
        plugin.getProperties().setProperty(Plugin.PROPERTY_USE_PREMIUM, false);
        downloadLink.setInProgress(false);
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
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
        downloadLink.setInProgress(false);
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
        downloadLink.setStatusText("Reconnect ");
        downloadLink.setInProgress(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        // Download Zeit. Versuch durch eine Interaction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen
        if (Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT), this) || Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME), this)) {
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
        downloadLink.setStatusText("File Not Found");
        downloadLink.setInProgress(false);
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
        downloadLink.setStatusText("File Abused");
        downloadLink.setInProgress(false);
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
        downloadLink.setStatusText("Captcha Fehler");
        downloadLink.setInProgress(false);
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
            downloadLink.setStatusText("Warten: " + JDUtilities.formatSeconds((int) (milliSeconds / 1000)) + " sek.");
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
        downloadLink.setStatusText("Unbekannter Fehler");
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
        downloadLink.setInProgress(false);
        downloadLink.setStatusText("Bot erkannt/Reconnect ");
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        if (Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT), this) || Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME), this)) {
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
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
        downloadLink.setStatusText("Code falsch");
        downloadLink.setInProgress(false);
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
        downloadLink.setStatusText("Reconnect ");
        downloadLink.setInProgress(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, downloadLink));
        // Download Zeit. Versuch durch eine Interaction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen
        boolean a=Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT), this);
        
        boolean b=Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME), this);
        if ( a||b ) {
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(0);
        }
        downloadLink.setStatusText("");
    }
    public DownloadLink getDownloadLink() {
        return this.downloadLink;
    }
}