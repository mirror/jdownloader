//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.io.File;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.gui.skins.simple.AgbDialog;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 * 
 * @author astaldo/JD-Team
 */
public class SingleDownloadController extends Thread {
    public static final String WAIT_TIME_ON_CONNECTION_LOSS = "WAIT_TIME_ON_CONNECTION_LOSS";

    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost currentPlugin;

    /**
     * Wurde der Download abgebrochen?
     */
    // private boolean aborted = false;
    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

    /**
     * Das übergeordnete Fenster
     */
    private JDController controller;

    private DownloadLink downloadLink;

    private boolean aborted;

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param dlink
     *            Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(JDController controller, DownloadLink dlink) {
        super("JD-StartDownloads");
        this.downloadLink = dlink;
        this.controller = controller;
        this.setPriority(Thread.MIN_PRIORITY);

        this.downloadLink.setDownloadLinkController(this);
    }

    /**
     * Bricht den Downloadvorgang ab.
     */
    public SingleDownloadController abortDownload() {
        downloadLink.setStatusText(JDLocale.L("controller.status.termination", "termination..."));
        // aborted = true;

        // if (currentPlugin != null) currentPlugin.abort();
        this.aborted = true;
        this.interrupt();
        // System.out.println("IS interrupted?: "+this+" -
        // "+Thread.currentThread().isInterrupted()+" - "+isInterrupted());

        return this;
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
        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_ACTIVE, this));
        downloadLink.setInProgress(true);
        plugin.resetPlugin();
        this.handlePlugin();

        downloadLink.setInProgress(false);

        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
        plugin.clean();
        downloadLink.requestGuiUpdate();
    }

    private void fireControlEvent(ControlEvent controlEvent) {
        JDUtilities.getController().fireControlEvent(controlEvent);

    }

    private void handlePlugin() {
        try {
            if (downloadLink.getDownloadURL() == null) {

                downloadLink.setStatusText(JDLocale.L("controller.status.containererror", "Container Fehler"));

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_SECURITY);
                fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);

                return;

            }
            downloadLink.setStatusText(JDLocale.L("controller.status.active", "aktiv"));
            downloadLink.setInProgress(true);
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, currentPlugin);
            fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
            currentPlugin.init();

            PluginStep step = currentPlugin.doNextStep(downloadLink);

            // Hier werden alle einzelnen Schritte des Plugins durchgegangen,
            // bis entweder null zurückgegeben wird oder ein Fehler auftritt

            while (!this.isAborted()&&step != null && step.getStatus() != PluginStep.STATUS_ERROR) {

                // downloadLink.setStatusText(JDLocale.L("controller.status.running",
                // "running..."));
                if (step.getStatus() != PluginStep.STATUS_SKIP) {
                    switch (step.getStep()) {
                    case PluginStep.STEP_PENDING:
                        long wait = (Long) step.getParameter();
                        step.setParameter(null);
                        logger.info("Erzwungene Wartezeit: " + wait);
                        while (wait > 0 && !this.isAborted()) {
                            downloadLink.setStatusText(JDUtilities.sprintf(JDLocale.L("controller.status.mustWaittime", "Erzwungene Wartezeit: %s"), new String[] { JDUtilities.formatSeconds((int) (wait / 1000)) }));
                            downloadLink.requestGuiUpdate();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                            wait -= 1000;
                        }
                        break;
                    case PluginStep.STEP_GET_CAPTCHA_FILE:
                        downloadLink.setStatusText(JDLocale.L("controller.status.captcha", "Captcha"));
                        fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                        File captcha = null;
                        logger.severe("GOGO JAC!");
                        if (step.getParameter() != null && step.getParameter() instanceof File) {
                            captcha = (File) step.getParameter();
                            step.setParameter(null);
                        }
                        if (captcha == null) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            logger.info("Captcha == null");
                            break;
                        } else {
                            downloadLink.setLatestCaptchaFile(captcha);

                            if (currentPlugin.doBotCheck(captcha)) {
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                                step.setStatus(PluginStep.STATUS_ERROR);
                                step.setParameter(null);
                                break;
                            }

                            String captchaText = Plugin.getCaptchaCode(captcha, currentPlugin);
                            logger.info("CaptchaCode: " + captchaText + " set in " + step);
                            downloadLink.setStatusText(JDLocale.L("controller.status.captchacode", "Code: ") + captchaText);
                            fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                            step.setParameter(captchaText);
                            step.setStatus(PluginStep.STATUS_DONE);

                        }
                        break;
                    }

                }

                if (step != null && downloadLink != null && currentPlugin != null) {
                  
                    PluginStep nextStep = currentPlugin.getNextStep(step);
                    if (nextStep != null) {
                        downloadLink.setStatusText(nextStep.toString());
                        fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                    }

                }
                // Bricht ab wenn es Fehler gab
                if (step.getStatus() == PluginStep.STATUS_ERROR) {
                    logger.severe("Error detected");
                    break;
                }

                step = currentPlugin.doNextStep(downloadLink);
                logger.finer("got step: " + step + " Linkstatus: " + downloadLink.getStatus());

            }

          
            if(step==null){
                step=currentPlugin.getSteps().lastElement();
                
            }
            PluginStep resultStep = step;
            int resultPluginStatus = step != null ? step.getStatus() : -1;
            int resultLinkStatus = downloadLink.getStatus();
          
            if (this.isAborted()) {

               
                logger.warning("Thread aborted");
                downloadLink.setStatus(DownloadLink.STATUS_TODO);
         

                return;
            }
            logger.info("FINISHED " + resultStep + " / " + resultLinkStatus);
        
                switch (resultLinkStatus) {
                case DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT:
                    this.onErrorWaittime(downloadLink, currentPlugin, resultStep);
                    break;
//                case DownloadLink.STATUS_ERROR_WAITTIME:
//                    this.onErrorStaticWaittime(downloadLink, currentPlugin, resultStep);
//                    break;
                case DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE:
                    this.onErrorTemporarilyUnavailable(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_TO_MANY_USERS:
                    this.onErrorTooManyUsers(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR:
                    this.onErrorCaptchaImage(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_ABUSED:
                    this.onErrorAbused(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_NOT_UPLOADED:
                    this.onErrorNotUploaded(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_AGB_NOT_SIGNED:
                    this.onErrorAGBNotSigned(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_UNKNOWN_RETRY:
                    this.onErrorRetry(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_FILE_NOT_FOUND:
                    this.onErrorFileNotFound(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_CAPTCHA_WRONG:
                    this.onErrorCaptcha(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_PREMIUM:
                    this.onErrorPremium(downloadLink, currentPlugin, resultStep);
                    break;

                case DownloadLink.STATUS_ERROR_NO_FREE_SPACE:
                    this.onErrorNoFreeSpace(downloadLink, currentPlugin, resultStep);
                    break;

                case DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC:
                    this.onErrorPluginSpecific(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_BOT_DETECTED:
                    this.onErrorBotdetection(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_DOWNLOAD_INCOMPLETE:
                    this.onErrorIncomplete(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_ALREADYEXISTS:
                    this.onErrorFileExists(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK:
                    this.onErrorFileInProgress(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED:
                    this.onErrorChunkloadFailed(downloadLink, currentPlugin, resultStep);
                    break;
                case DownloadLink.STATUS_ERROR_NOCONNECTION:
                    this.onErrorNoConnection(downloadLink, currentPlugin, resultStep);
                    break;

//                default:
//                    logger.info("Uknown error id: " + resultLinkStatus);
//                    this.onErrorUnknown(downloadLink, currentPlugin, resultStep);
                }

                if (downloadLink.getStatus() != DownloadLink.STATUS_TODO && resultPluginStatus == PluginStep.STATUS_ERROR && currentPlugin.getRetryCount() < currentPlugin.getMaxRetries() && !downloadLink.isWaitingForReconnect()) {
//                    currentPlugin.setRetryOnErrorCount(currentPlugin.getRetryOnErrorCount() + 1);
//                    logger.info("Retry on Error: " + (currentPlugin.getRetryOnErrorCount() - 1));

                    onErrorRetry(downloadLink, currentPlugin, resultStep);
                }

           
                // downloadLink.setStatusText(JDLocale.L("controller.status.finished",
                // "Fertig"));
                if (resultPluginStatus != PluginStep.STATUS_ERROR &&resultLinkStatus != DownloadLink.STATUS_DONE) {
                    logger.severe("Pluginerror: resultStep returned null and Downloadlink status != STATUS_DONE.  retry Link");
                    this.onErrorRetry(downloadLink, currentPlugin, resultStep);
                    return;
                }
                if(resultPluginStatus != PluginStep.STATUS_ERROR ){
                onDownloadFinishedSuccessFull(downloadLink,resultStep,resultLinkStatus);
                }
             

            
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void onDownloadFinishedSuccessFull(DownloadLink downloadLink, PluginStep resultStep, int resultLinkStatus) {
        downloadLink.setStatusText(null);
        logger.finer("final resultStep: " + resultStep + " Linkstatus: " + resultLinkStatus);
    
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
            new PackageManager().onDownloadedPackage(downloadLink);
        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

        Interaction.handleInteraction((Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED), downloadLink);
        if (JDUtilities.getController().isContainerFile(new File(downloadLink.getFileOutput()))) {
            Interaction.handleInteraction((Interaction.INTERACTION_CONTAINER_DOWNLOAD), downloadLink);
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) controller.loadContainerFile(new File(downloadLink.getFileOutput()));
        }
        if (JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLED, true)) controller.getUnrarModule().interact(downloadLink);
        
    }

    private void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(controlID, param);

    }

    private void onErrorNoConnection(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        logger.severe("Error occurred: No Serverconnection");
        long milliSeconds = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(WAIT_TIME_ON_CONNECTION_LOSS, 5 * 60) * 1000;

        while (milliSeconds > 0 && !isInterrupted() && !this.downloadLink.isAborted()) {

            downloadLink.setStatusText(JDUtilities.formatSeconds((int) (milliSeconds / 1000)));
            downloadLink.requestGuiUpdate();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            milliSeconds -= 1000;
        }

       downloadLink.setStatus(DownloadLink.STATUS_TODO);
        // downloadLink.setEndOfWaittime(0);
        // downloadLink.reset();
        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        onErrorRetry(downloadLink2, plugin, step);
    }

    private void onErrorChunkloadFailed(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {

        logger.severe("Chunkload failed: ");
        downloadLink.setStatusText(JDLocale.L("controller.status.chunkloadfailed", "Multidownload fehlgeschlagen"));

        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    private void onErrorFileInProgress(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        downloadLink.setEnabled(false);
        downloadLink.setStatusText(JDLocale.L("controller.status.fileinprogress", "Datei wird schon geladen"));

        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    private void onErrorIncomplete(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {

        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);

    }

    private void onErrorFileExists(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {

        String todo = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_FILE_EXISTS, JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));

        if (todo.equals(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"))) {
            downloadLink.setEnabled(false);
            downloadLink.setStatusText(JDLocale.L("controller.status.fileexists.skip", "Datei schon vorhanden"));
            
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        } else {
            if (new File(downloadLink.getFileOutput()).delete()) {

                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                downloadLink.setEndOfWaittime(0);
            } else {
                downloadLink.setStatusText(JDLocale.L("controller.status.fileexists.overwritefailed", "Überschreiben fehlgeschlagen ") + downloadLink.getFileOutput());

            }

        }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

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
         * size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\"><br>
         * wurden nicht gelesen und akzeptiert.</font></font></p><p><font
         * size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\"><br>
         * Anbieter: </font></p>")+plugin.getHost(); String
         * url="http://www.the-lounge.org/viewtopic.php?f=222&t=8842";
         * JDUtilities.getGUI().showHelpMessage(title, message, url); }
         */

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    private void onErrorPluginSpecific(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        String message = (String) step.getParameter();
        logger.severe("Error occurred: " + message);
        step.setParameter(null);
        if (message != null) downloadLink.setStatusText(message);
       // downloadLink.setStatus(DownloadLink.STATUS_TODO);
        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    private void onErrorNoFreeSpace(DownloadLink downloadLink2, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.freespace", "Zu wenig Speicherplatz"));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

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
        } catch (InterruptedException e) {
            return;
        }
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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
        long milliSeconds = 2 * 60 * 1000;
        try {
            milliSeconds = (Long) step.getParameter();
            step.setParameter(null);
        } catch (Exception e) {
        }

        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText(JDLocale.L("controller.status.toManyUser", "ausgelastet") + " ");
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        // downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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
        logger.warning("disable PREMIUM for: " + plugin + " Reason: "+step.getParameter());
        String str = (String) step.getParameter();
        step.setParameter(null);
        if (str == null) {
            plugin.getProperties().setProperty(Plugin.PROPERTY_USE_PREMIUM, false);
        } else {
            plugin.getProperties().setProperty(str, false);
            downloadLink.setStatusText(str);
        }
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
    }

//    /**
//     * Wird aufgerufen wenn Das Plugin eine Immer gleiche Wartezeit meldet. z.B.
//     * bei unbekannter Wartezeit
//     * 
//     * @param downloadLink
//     * @param plugin
//     * @param step
//     */
//    private void onErrorStaticWaittime(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
//        logger.severe("Error occurred: Static Wait Time " + step);
//        long milliSeconds;
//        if (step.getParameter() != null) {
//            milliSeconds = (Long) step.getParameter();
//        } else {
//            milliSeconds = 10000;
//        }
//        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
//        downloadLink.setStatusText(JDLocale.L("controller.status.reconnect", "Reconnect "));
//        // downloadLink.setInProgress(true);
//        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
//        // Download Zeit. Versuch durch eine Interaction einen reconnect
//        // zu machen. wenn das klappt nochmal versuchen
//
//        // Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT),
//        // this);
//        // Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME),
//        // this);
//        Reconnecter.requestReconnect();
//        // if (Reconnecter.waitForNewIP(0)) {
//        // downloadLink.setStatus(DownloadLink.STATUS_TODO);
//        // downloadLink.setEndOfWaittime(0);
//        // }
//        // while (downloadLink.getRemainingWaittime() > 0 && !aborted) {
//        // fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED,
//        // downloadLink);
//        // try {
//        // Thread.sleep(5000);
//        // } catch (InterruptedException e) {
//        // }
//        //
//        // }
//        downloadLink.setStatus(DownloadLink.STATUS_TODO);
//
//        downloadLink.setStatusText("");
//    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin, PluginStep step) {
        downloadLink.setStatusText(JDLocale.L("controller.status.filenotfound", "File Not Found"));
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
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
        if (plugin.getRetryCount() >= plugin.getMaxRetries()) {
            onErrorUnknown(downloadLink, plugin, step);
            return;
        }
        logger.info("Retry " + plugin.getRetryCount());

        downloadLink.setStatusText(String.format(JDLocale.L("controller.status.retryonerror", "Neuer Versuch(%s/%s) in 3 Sekunden"), (1 + plugin.getRetryCount()) + "", plugin.getMaxRetries()));
        downloadLink.requestGuiUpdate();
        JDUtilities.sleep(3000);
        plugin.setRetryCount(plugin.getRetryCount() + 1);

//        if (step != null && step.getParameter() != null) {
//            try {
//                logger.info("step.getParameter() " + step.getParameter());
//                long milliSeconds = (Long) step.getParameter();
//                downloadLink.setStatusText(JDUtilities.sprintf(JDLocale.L("controller.status.wait", "Warten: %s sek."), new String[] { JDUtilities.formatSeconds((int) (milliSeconds / 1000)) }));
//                fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, null));
//                try {
//                    Thread.sleep(milliSeconds);
//                } catch (InterruptedException e) {
//                }
//            } catch (Exception e2) {
//
//            }
//        }

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
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        logger.severe("Error occurred while downloading file");

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
        downloadLink.setEndOfWaittime(0);
        downloadLink.setStatusText(JDLocale.L("controller.status.botDetected", "Bot erkannt/Reconnect"));
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        if (plugin.getBotWaittime() <= 0) {
            Reconnecter.requestReconnect();
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            downloadLink.setEndOfWaittime(System.currentTimeMillis() + 2 * 60 * 1000);
            downloadLink.setStatusText(JDLocale.L("controller.status.botWaitReconnect", "Bot. Warte auf Reconnect"));
        } else if (plugin.getBotWaittime() > 0) {

            long wait = plugin.getBotWaittime();

            while (wait > 0 && !isInterrupted()) {
                downloadLink.setStatusText(JDLocale.L("controller.status.botWait", "Botwait ") + JDUtilities.formatSeconds((int) wait / 1000));
                fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                wait -= 1000;

            }
            downloadLink.setStatus(DownloadLink.STATUS_TODO);

            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

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

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        downloadLink.setStatus(DownloadLink.STATUS_TODO);
        downloadLink.setEndOfWaittime(0);
        onErrorRetry(downloadLink, plugin, step);
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
        logger.finer("Error occurred: Wait Time " + step);
        long milliSeconds = (Long) step.getParameter();
        step.setParameter(null);
        downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
        downloadLink.setStatusText(" " + JDLocale.L("controller.status.reconnect", "Reconnect "));

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        // Download Zeit. Versuch durch eine Interaction einen reconnect
        // zu machen. wenn das klappt nochmal versuchen

        // if (Reconnecter.waitForNewIP(0)) {
        // downloadLink.setStatus(DownloadLink.STATUS_TODO);
        // downloadLink.setEndOfWaittime(0);
        // }
        Reconnecter.requestReconnect();
        // while (downloadLink.getRemainingWaittime() > 0 && !aborted) {
        // fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED,
        // downloadLink);
        // try {
        // Thread.sleep(5000);
        // } catch (InterruptedException e) {
        // }
        //
        // }
        // downloadLink.setStatus(DownloadLink.STATUS_TODO);
        // downloadLink.setStatusText("");
    }

    public DownloadLink getDownloadLink() {
        return this.downloadLink;
    }

    public boolean isAborted() {
        return aborted;
    }

    public PluginForHost getCurrentPlugin() {
        return currentPlugin;
    }

}