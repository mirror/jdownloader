//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.controlling.reconnect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.gui.skins.simple.components.Linkgrabber.LinkCheck;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Reconnecter {

    private static String CURRENT_IP = "";
    /**
     * Set to true only if there is a reconnect running currently
     */
    private static boolean RECONNECT_IN_PROGRESS = false;
    /**
     * Timestampo of the latest IP CHange
     */
    private static long LAST_UP_UPDATE_TIME = 0;
    private static Logger logger = JDLogger.getLogger();
    /**
     * Only true if a reconect has been requestst.
     */
    private static boolean RECONNECT_REQUESTED = false;

    public static void toggleReconnect() {
        boolean newState = !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newState);
        JDUtilities.getConfiguration().save();
        if (!newState) JDUtilities.getGUI().displayMiniWarning(JDL.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDL.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."));
    }

    private static boolean checkExternalIPChange() {
        LAST_UP_UPDATE_TIME = System.currentTimeMillis();
        String tmp = CURRENT_IP;
        CURRENT_IP = JDUtilities.getIPAddress(null);
        if (CURRENT_IP != null && tmp.length() > 0 && !tmp.equals(CURRENT_IP)) {
            logger.info("Detected external IP Change.");
            return true;
        }
        return false;
    }

    /**
     * Führt einen Reconnect durch.
     * 
     * @return <code>true</code>, wenn der Reconnect erfolgreich war, sonst
     *         <code>false</code>
     */
    public static boolean doReconnect() {
        JDController controller = JDUtilities.getController();
        boolean ipChangeSuccess = false;
        if (System.currentTimeMillis() - LAST_UP_UPDATE_TIME > 1000 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60 * 10)) {
            /*
             * gab schon nen externen reconnect , checke nur falls wirklich
             * ipcheck aktiv ist!
             */
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                if (Reconnecter.checkExternalIPChange()) return true;
            }
        }

        Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, controller);
        int type = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
        logger.info("Try to reconnect...");
        /* laufende downloads stoppen */
        ArrayList<DownloadLink> disabled = DownloadWatchDog.getInstance().getRunningDownloads();
        if (disabled.size() != 0) logger.info("Stopping all running downloads!");
        for (DownloadLink link : disabled) {
            link.setEnabled(false);
        }
        /* warte bis alle gestoppt sind */
        for (int wait = 0; wait < 10; wait++) {
            if (DownloadWatchDog.getInstance().getActiveDownloads() == 0) break;
            try {
                Thread.sleep(1000);
                logger.info("Still waiting for all downloads to stop!");
            } catch (InterruptedException e) {
                break;
            }
        }
        if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
            logger.severe("Could not stop all running downloads!");
        }

        try {
            switch (type) {
            case ReconnectMethod.EXTERN:
                ipChangeSuccess = new ExternReconnect().doReconnect();
                break;
            case ReconnectMethod.BATCH:
                ipChangeSuccess = new BatchReconnect().doReconnect();
                break;
            default:
                ipChangeSuccess = new HTTPLiveHeader().doReconnect();

            }
        } catch (Exception e) {
            logger.severe("ReconnectMethod failed!");
        }
        /* gestoppte downloads wieder aufnehmen */
        for (DownloadLink link : disabled) {
            link.setEnabled(true);
        }
        LAST_UP_UPDATE_TIME = System.currentTimeMillis();
        CURRENT_IP = JDUtilities.getIPAddress(null);
        return ipChangeSuccess;
    }

    public static boolean isReconnecting() {
        return RECONNECT_IN_PROGRESS;
    }

    /**
     * Returns true, if there is a requested reconnect qaiting, and the user
     * selected not to start new downloads of reconnects are waiting
     * 
     * @return
     */
    public static boolean isReconnectPrefered() {

        return (isReconnectRequested() && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) && SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT", true) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true));
    }

    public static boolean doReconnectIfRequested(boolean bypassrcvalidation) {
        if (RECONNECT_IN_PROGRESS) return false;
        /* falls nen Linkcheck läuft, kein Reconnect */
        if (LinkCheck.getLinkChecker().isRunning()) {
            //JDLogger.getLogger().info("No Reconnect: Linkgrabber is active");
            return false;
        }
        boolean num;
        if (num = JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) {
            /* darf keinen reconnect machen */
            //JDLogger.getLogger().info("No Reconnect: " + num + " no resumable downloads are running");
            return false;
        }
        RECONNECT_IN_PROGRESS = true;
        boolean ret = false;
        try {
            ret = doReconnectIfRequestedInternal(bypassrcvalidation);
            if (ret) {
                Reconnecter.resetAllLinks();
                Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, JDUtilities.getController());
            }

        } catch (Exception e) {
        }
        RECONNECT_IN_PROGRESS = false;
        return ret;
    }

    public static boolean doReconnectIfRequestedInternal(boolean bypassrcvalidation) {
        boolean ret = false;
        /* überhaupt ein reconnect angefragt? */
        if (isReconnectRequested()) {
            if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                /*
                 * auto reconnect ist AUS, dann nur noch schaun ob sich ip
                 * geändert hat
                 */
                if (System.currentTimeMillis() - LAST_UP_UPDATE_TIME > 1000 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60 * 10)) {
                    /*
                     * hier nur ein ip check falls auch ip check wirklich aktiv,
                     * sonst gibts ne endlos reconnectschleife
                     */
                    if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) return Reconnecter.checkExternalIPChange();
                }
                return false;

            } else {
                /* auto reconnect ist AN */
                if (bypassrcvalidation || JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true)) {
                    try {
                        ret = Reconnecter.doReconnect();
                        if (ret) {
                            logger.info("Reconnect successfully!");
                        } else {
                            logger.info("Reconnect failed!");
                        }
                    } catch (Exception e) {
                        logger.finest("Reconnect failed.");
                    }
                    if (ret == false) {
                        ProgressController progress = new ProgressController(JDL.L("jd.controlling.reconnect.Reconnector.progress.failed", "Reconnect failed! Please check your reconnect Settings and try a Manual Reconnect!"), 100);
                        progress.finalize(10000l);
                    }
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, ret);
                    JDUtilities.getConfiguration().save();
                }
            }
        }
        return ret;
    }

    private static void resetAllLinks() {
        ArrayList<FilePackage> packages = JDUtilities.getController().getPackages();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                for (DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    if (nextDownloadLink.getPlugin() != null && nextDownloadLink.getPlugin().getRemainingHosterWaittime() > 0) {
                        if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                            nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                            nextDownloadLink.getLinkStatus().resetWaitTime();
                            nextDownloadLink.getPlugin().resetHosterWaitTime();
                            logger.finer("Reset GLOBALS: " + nextDownloadLink.getPlugin());
                            nextDownloadLink.getPlugin().resetPluginGlobals();
                        }
                    }
                }
            }
        }
    }

    public static boolean waitForNewIP(long i) {
        setReconnectRequested(true);
        final ProgressController progress = new ProgressController(JDL.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", "0:00s"), 2);
        if (i > 0) {
            i += System.currentTimeMillis();
        }
        progress.setStatus(1);
        final long startTime = System.currentTimeMillis();
        boolean ret;
        Thread timer = new Thread() {
            @Override
            public void run() {
                while (true) {
                    progress.setStatusText(JDL.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", Formatter.formatSeconds((System.currentTimeMillis() - startTime) / 1000)));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

            }
        };
        timer.start();
        while (!(ret = Reconnecter.doReconnectIfRequested(true)) && (System.currentTimeMillis() < i || i <= 0)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ret = false;
                break;
            }
        }
        timer.interrupt();

        if (!ret) {
            progress.setColor(Color.RED);
            progress.setStatusText(JDL.L("gui.reconnect.progress.status.failed", "Reconnect failed"));
        } else {
            progress.setStatusText(JDL.L("gui.reconnect.progress.status.success", "Reconnect successfull"));
        }
        setReconnectRequested(false);
        progress.finalize(4000);
        return ret;
    }

    public static boolean doManualReconnect() {
        boolean oldState = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);

        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, true);

        boolean restartDownloads = JDUtilities.getController().stopDownloads();

        boolean success = Reconnecter.waitForNewIP(1);

        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, oldState);

        if (restartDownloads) {
            JDUtilities.getController().startDownloads();
        }

        return success;
    }

    /**
     * @param RECONNECT_REQUESTED
     *            the RECONNECT_REQUESTED to set
     */
    public static void setReconnectRequested(boolean reconnectRequested) {
        Reconnecter.RECONNECT_REQUESTED = reconnectRequested;
    }

    /**
     * @return the RECONNECT_REQUESTED
     */
    public static boolean isReconnectRequested() {
        return RECONNECT_REQUESTED;
    }

}
