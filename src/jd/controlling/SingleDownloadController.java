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
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.gui.skins.simple.AgbDialog;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
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

    private boolean aborted;

    /**
     * Das übergeordnete Fenster
     */
    private JDController controller;

    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost currentPlugin;

    private DownloadLink downloadLink;

    private LinkStatus linkStatus;

    /**
     * Wurde der Download abgebrochen?
     */
    // private boolean aborted = false;
    /**
     * Der Logger
     */
    private Logger logger = JDUtilities.getLogger();

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
        downloadLink = dlink;
        linkStatus = downloadLink.getLinkStatus();
        this.controller = controller;
        setPriority(Thread.MIN_PRIORITY);

        downloadLink.setDownloadLinkController(this);
    }

    /**
     * Bricht den Downloadvorgang ab.
     */
    public SingleDownloadController abortDownload() {
        linkStatus.setStatusText(JDLocale.L("controller.status.termination", "termination..."));
        // aborted = true;

        // if (currentPlugin != null) currentPlugin.abort();
        aborted = true;
        interrupt();
        // System.out.println("IS interrupted?: "+this+" -
        // "+Thread.currentThread().isInterrupted()+" - "+isInterrupted());

        return this;
    }

    private void fireControlEvent(ControlEvent controlEvent) {
        JDUtilities.getController().fireControlEvent(controlEvent);

    }

    private void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(controlID, param);

    }

    public PluginForHost getCurrentPlugin() {
        return currentPlugin;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private void handlePlugin() {
        try {
            if (downloadLink.getDownloadURL() == null) {

                downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.containererror", "Container Fehler"));

                // linkStatus.addStatus(LinkStatus.ERROR_SECURITY);
                fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);

                return;

            }
            linkStatus.setStatusText(JDLocale.L("gui.download.create_connection", "Connecting..."));

            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, currentPlugin);
            fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink);
            currentPlugin.init();
            try {
                currentPlugin.handle(downloadLink);
            } catch (SocketTimeoutException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage("Hoster offline");
                linkStatus.setValue(20 * 60 * 1000l);

            } catch (InterruptedException e) {
                e.printStackTrace();

                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (NullPointerException e) {
                e.printStackTrace();

                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));

            } catch (Exception e) {

                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));

            }

            if (isAborted()) {

                logger.warning("Thread aborted");
                linkStatus.setStatus(LinkStatus.TODO);
                return;
            }
            if (linkStatus.isFailed()) {
                logger.severe("\r\nError occured- " + downloadLink.getLinkStatus());
            }

            switch (linkStatus.getLatestStatus()) {

            case LinkStatus.ERROR_IP_BLOCKED:
                onErrorWaittime(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                onErrorTemporarilyUnavailable(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_AGB_NOT_SIGNED:
                onErrorAGBNotSigned(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                onErrorFileNotFound(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_LINK_IN_PROGRESS:
                onErrorLinkBlock(downloadLink, currentPlugin);

            case LinkStatus.ERROR_FATAL:
                onErrorFatal(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_CAPTCHA:
                onErrorCaptcha(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_PREMIUM:
                onErrorPremium(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                onErrorIncomplete(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                onErrorFileExists(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                onErrorChunkloadFailed(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_PLUGIN_DEFEKT:
                onErrorPluginDefect(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_NO_CONNECTION:
            case LinkStatus.ERROR_TIMEOUT_REACHED:
                onErrorNoConnection(downloadLink, currentPlugin);
                break;

            default:
                if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    logger.severe("\r\nFinished- " + downloadLink.getLinkStatus());
                    onDownloadFinishedSuccessFull(downloadLink);
                } else {
                    retry(downloadLink, currentPlugin);
                }
            }

            // if (linkStatus.isStatus(LinkStatus.TODO) &&
            // currentPlugin.getRetryCount() < currentPlugin.getMaxRetries() &&
            // !downloadLink.isWaitingForReconnect()) {
            //
            // onErrorRetry(downloadLink, currentPlugin);
            // }

            // downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.finished",
            // "Fertig"));
            // if (resultPluginStatus != PluginStep.STATUS_ERROR &&
            // resultLinkStatus != LinkStatus.FINISHED) {
            // logger.severe("Pluginerror: resultStep returned null and
            // Downloadlink status != STATUS_DONE. retry Link");
            // this.onErrorRetry(downloadLink, currentPlugin);
            // return;
            // }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void onErrorLinkBlock(DownloadLink downloadLink, PluginForHost currentPlugin) {

        LinkStatus status = downloadLink.getLinkStatus();

        status.resetWaitTime();

        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    private void onErrorPluginDefect(DownloadLink downloadLink2, PluginForHost currentPlugin2) {
        logger.severe(" The PLugin for " + currentPlugin.getHost() + " seems to be out of date. Please inform the Support-team http://jdownloader.org/support.");
        logger.severe(downloadLink2.getLinkStatus().getErrorMesage());

        downloadLink2.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
        downloadLink2.getLinkStatus().setErrorMessage(JDLocale.L("controller.status.pluindefekt", "Plugin out of date"));

        downloadLink.requestGuiUpdate();

    }

    public boolean isAborted() {
        return aborted;
    }

    private void onDownloadFinishedSuccessFull(DownloadLink downloadLink) {

        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
            new PackageManager().onDownloadedPackage(downloadLink);
        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

        Interaction.handleInteraction(Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED, downloadLink);
        if (JDUtilities.getController().isContainerFile(new File(downloadLink.getFileOutput()))) {
            Interaction.handleInteraction(Interaction.INTERACTION_CONTAINER_DOWNLOAD, downloadLink);
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                controller.loadContainerFile(new File(downloadLink.getFileOutput()));
            }
        }
        if (JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLED, true)) {
            controller.getUnrarModule().interact(downloadLink);
        }

    }

    private void onErrorAGBNotSigned(DownloadLink downloadLink2, PluginForHost plugin) {

        downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.agb_tos", "AGB nicht akzeptiert"));

        new AgbDialog(downloadLink2, 30);

        /*
         * 
         * if(JDController.FLAGS.getIntegerProperty("AGBMESSAGESIGNED_"+plugin.getHost(),
         * 0)==0){
         * 
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

    /**
     * Diese Funktion wird aufgerufen wenn ein Download wegen eines
     * captchafehlersabgebrochen wird
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorCaptcha(DownloadLink downloadLink, PluginForHost plugin) {
        retry(downloadLink, plugin);
    }

    private void retry(DownloadLink downloadLink, PluginForHost plugin) {
        int r;
        if ((r = downloadLink.getLinkStatus().getRetryCount()) <= plugin.getMaxRetries()) {
            downloadLink.getLinkStatus().reset();
            downloadLink.getLinkStatus().setRetryCount(r + 1);
            try {
                plugin.sleep(2000, downloadLink);
            } catch (InterruptedException e) {
            }

        } else {

            // downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);

        }

    }

    private void onErrorChunkloadFailed(DownloadLink downloadLink, PluginForHost plugin) {
        if (linkStatus.getErrorMesage() == null) {
            linkStatus.setErrorMessage(JDLocale.L("plugins.error.downloadfailed", "Download failed"));
        }

    }

    private void onErrorFatal(DownloadLink downloadLink, PluginForHost currentPlugin) {

        downloadLink.requestGuiUpdate();
    }

    private void onErrorFileExists(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();

        String todo = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_FILE_EXISTS, JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));

        if (todo.equals(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"))) {
            downloadLink.setEnabled(false);
            status.setErrorMessage(JDLocale.L("controller.status.fileexists.skip", "Datei schon vorhanden"));

            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
        } else {
            if (new File(downloadLink.getFileOutput()).delete()) {
                status.reset();
            } else {
                status.addStatus(LinkStatus.ERROR_FATAL);
                status.setErrorMessage(JDLocale.L("controller.status.fileexists.overwritefailed", "Überschreiben fehlgeschlagen ") + downloadLink.getFileOutput());

            }
        }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));

    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin) {
        logger.severe("File not found :" + downloadLink);
    }

    // /**
    // * Wird aufgerufen wenn Das Plugin eine Immer gleiche Wartezeit meldet.
    // z.B.
    // * bei unbekannter Wartezeit
    // *
    // * @param downloadLink
    // * @param plugin
    // * @param step
    // */
    // private void onErrorStaticWaittime(DownloadLink downloadLink,
    // PluginForHost plugin) {
    // logger.severe("Error occurred: Static Wait Time " + step);
    // long milliSeconds;
    // if (step.getParameter() != null) {
    // milliSeconds = (Long) step.getParameter();
    // } else {
    // milliSeconds = 10000;
    // }
    // downloadLink.setEndOfWaittime(System.currentTimeMillis() + milliSeconds);
    // downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.reconnect",
    // "Reconnect "));
    // // downloadLink.setInProgress(true);
    // fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
    // // Download Zeit. Versuch durch eine Interaction einen reconnect
    // // zu machen. wenn das klappt nochmal versuchen
    //
    // //
    // Interaction.handleInteraction((Interaction.INTERACTION_NEED_RECONNECT),
    // // this);
    // //
    // Interaction.handleInteraction((Interaction.INTERACTION_DOWNLOAD_WAITTIME),
    // // this);
    // Reconnecter.requestReconnect();
    // // if (Reconnecter.waitForNewIP(0)) {
    // // linkStatus.addStatus(LinkStatus.TODO);
    // // downloadLink.setEndOfWaittime(0);
    // // }
    // // while (downloadLink.getRemainingWaittime() > 0 && !aborted) {
    // // fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED,
    // // downloadLink);
    // // try {
    // // Thread.sleep(5000);
    // // } catch (InterruptedException e) {
    // // }
    // //
    // // }
    // linkStatus.addStatus(LinkStatus.TODO);
    //
    // downloadLink.getLinkStatus().setStatusText("");
    // }

    private void onErrorIncomplete(DownloadLink downloadLink, PluginForHost plugin) {

        retry(downloadLink, plugin);

    }

    // /**
    // * Wird aufgerufen wenn eine Datei nicht fertig upgeloaded wurde
    // *
    // * @param downloadLink
    // * @param plugin
    // * @param step
    // */
    // private void onErrorNotUploaded(DownloadLink downloadLink, PluginForHost
    // plugin) {
    // downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.incompleteUpload",
    // "File not full uploaded"));
    // linkStatus.addStatus(LinkStatus.TODO);
    // fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
    // }

    // /**
    // * Wird aufgerufen wenn das Captchabild nicht geladen werden konnte
    // *
    // * @param downloadLink
    // * @param plugin
    // * @param step
    // */
    // private void onErrorCaptchaImage(DownloadLink downloadLink, PluginForHost
    // plugin) {
    // downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.captchaError",
    // "Captcha Fehler"));
    //
    // fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
    // }

    private void onErrorNoConnection(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        logger.severe("Error occurred: No Serverconnection");
        long milliSeconds = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(WAIT_TIME_ON_CONNECTION_LOSS, 5 * 60) * 1000;
        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        linkStatus.setWaitTime(milliSeconds);

        downloadLink.setEnabled(false);
        if (linkStatus.getErrorMesage() == null) {
            linkStatus.setErrorMessage(JDLocale.L("controller.status.connectionproblems", "Connection lost."));
        }
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
    private void onErrorPremium(DownloadLink downloadLink, PluginForHost plugin) {

        linkStatus.reset();

    }

    /**
     * Wird aufgerufen wenn ein Link kurzzeitig nicht verfügbar ist. ER wird
     * deaktiviert und kann zu einem späteren zeitpunkt wieder aktiviert werden
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorTemporarilyUnavailable(DownloadLink downloadLink, PluginForHost plugin) {
        logger.severe("Error occurred: Temporarily unavailably: " + downloadLink.getLinkStatus().getValue() + " ms");
        // long milliSeconds = (Long) step.getParameter();
        // downloadLink.setEndOfWaittime(System.currentTimeMillis() +
        // milliSeconds);
        LinkStatus status = downloadLink.getLinkStatus();
        status.setErrorMessage(JDLocale.L("controller.status.tempUnavailable", "kurzzeitig nicht verfügbar"));

        /*
         * Value<=0 bedeutet das der link dauerhauft deaktiviert bleiben soll.
         * value>0 gibt die zeit an die der link deaktiviert bleiben muss in ms.
         * Der DownloadWatchdoggibt den Link wieder frei ewnn es zeit ist.
         */
        if (status.getValue() > 0) {
            status.setWaitTime((int) status.getValue());
        } else {
            status.resetWaitTime();
        }

        downloadLink.setEnabled(false);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED, downloadLink));
    }

    /**
     * Diese Funktion wird aufgerufen wenn Ein Download mit einem Waittimefehler
     * abgebrochen wird
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorWaittime(DownloadLink downloadLink, PluginForHost plugin) {
        logger.finer("Error occurred: Wait Time ");
        LinkStatus status = downloadLink.getLinkStatus();
        long milliSeconds = downloadLink.getLinkStatus().getValue();

        if (milliSeconds <= 0) {
            logger.severe("Es wurde vom PLugin keine Wartezeit übergeben");
            status.addStatus(LinkStatus.ERROR_FATAL);
            status.setErrorMessage(JDLocale.L("plugins.errors.pluginerror", "Plugin error. Inform Support"));
            return;
        }
        status.setWaitTime((int) milliSeconds);
        plugin.setHosterWaittime((int) milliSeconds);

        // blockiert bis zu einem erfolgreichem recionnect
        // if (Reconnecter.waitForNewIP(0)) {
        // linkStatus.reset();
        // }
        Reconnecter.requestReconnect();
        // while (status.getRemainingWaittime() > 0) {
        //
        // fireControlEvent(ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED,
        // downloadLink);
        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // break;
        // }
        // }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, null));

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        /**
         * Das Plugin, das den aktuellen Download steuert
         */
        PluginForHost plugin;
        linkStatus.setStatusText(null);
        linkStatus.setErrorMessage(null);
        logger.info("working on " + downloadLink.getName());
        currentPlugin = plugin = (PluginForHost) downloadLink.getPlugin();
        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_ACTIVE, this));
        linkStatus.setInProgress(true);
        // plugin.resetPlugin();

        handlePlugin();

        linkStatus.setInProgress(false);

        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
        plugin.clean();
        downloadLink.requestGuiUpdate();
    }

}