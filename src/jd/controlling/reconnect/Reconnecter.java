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
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.gui.skins.simple.components.Linkgrabber.LinkCheck;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Reconnecter {

    private static String CURRENT_IP = "";
    private static boolean IS_RECONNECTREQUESTING = false;
    private static long lastIPUpdate = 0;
    private static Logger logger = JDLogger.getLogger();
    public static boolean hasWaittimeLinks = false;

    public static void toggleReconnect() {
        boolean newState = !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newState);
        JDUtilities.getConfiguration().save();
        if (!newState) JDUtilities.getGUI().displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."));
    }

    private static boolean checkExternalIPChange() {
        lastIPUpdate = System.currentTimeMillis();
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
        if (System.currentTimeMillis() - lastIPUpdate > 1000 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60 * 10)) {
            /* gab schon nen externen reconnect */
            if (Reconnecter.checkExternalIPChange()) return true;
        }
        ArrayList<DownloadLink> disabled = new ArrayList<DownloadLink>();

        Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, controller);
        int type = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
        logger.info("DO RECONNECT NOW");
        boolean interrupt = SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", true);
        if (interrupt) {
            for (FilePackage fp : controller.getPackages()) {
                for (DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                        nextDownloadLink.setEnabled(false);
                        logger.info("disabled " + nextDownloadLink);
                        disabled.add(nextDownloadLink);
                    }
                }
            }
        }
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
        if (interrupt) {
            for (DownloadLink link : disabled) {
                logger.info("enable " + link);
                link.setEnabled(true);
            }
        }
        logger.info("Reconnect success: " + ipChangeSuccess);
        lastIPUpdate = System.currentTimeMillis();
        CURRENT_IP = JDUtilities.getIPAddress(null);
        return ipChangeSuccess;
    }

    public static boolean isReconnecting() {
        return IS_RECONNECTREQUESTING;
    }

    public static boolean preferReconnect() {
        /*
         * falls reconnect gewünscht ist und dieser erfolgreich eingestellt und
         * reconnects vorrang gegenüber reconnectfreie downloads haben soll
         */
        return (hasWaittimeLinks && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) && SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT", true) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, true));
    }

    public static boolean doReconnectIfRequested() {
        if (IS_RECONNECTREQUESTING) return false;
        /* falls nen Linkcheck läuft, kein Reconnect */
        if (LinkCheck.getLinkChecker().isRunning()) return false;
        if (JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) {
            /* darf keinen reconnect machen */
            return false;
        }
        IS_RECONNECTREQUESTING = true;
        boolean ret = doReconnectIfRequestedInternal();
        if (ret) {
            Reconnecter.resetAllLinks();
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, JDUtilities.getController());
        }
        IS_RECONNECTREQUESTING = false;
        return ret;
    }

    public static boolean doReconnectIfRequestedInternal() {
        boolean ret = false;
        /* überhaupt ein reconnect angefragt? */
        if (hasWaittimeLinks) {
            /* erstma schau ob überhaupt eingeschalten */
            if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                if (System.currentTimeMillis() - lastIPUpdate > 1000 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60 * 10)) return Reconnecter.checkExternalIPChange();
                return false;
            }
            /* jetzt nen echten reconnect versuchen */
            try {
                ret = Reconnecter.doReconnect();
            } catch (Exception e) {
                logger.finest("Reconnect failed. Exception " + e.getMessage());
                JDLogger.exception(e);
            }
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_LATEST_RECONNECT_RESULT, ret);
            JDUtilities.getConfiguration().save();
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
        hasWaittimeLinks = true;
        final ProgressController progress = new ProgressController(JDLocale.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", "0:00s"), 2);
        if (i > 0) {
            i += System.currentTimeMillis();
        }
        progress.setStatus(1);
        final long startTime = System.currentTimeMillis();
        boolean ret;
        Thread timer = new Thread() {
            public void run() {
                while (true) {
                    progress.setStatusText(JDLocale.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", Formatter.formatSeconds((System.currentTimeMillis() - startTime) / 1000)));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

            }
        };
        timer.start();
        while (!(ret = Reconnecter.doReconnectIfRequested()) && (System.currentTimeMillis() < i || i <= 0)) {
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
            progress.setStatusText(JDLocale.L("gui.reconnect.progress.status.failed", "Reconnect failed"));
        } else {
            progress.setStatusText(JDLocale.L("gui.reconnect.progress.status.success", "Reconnect successfull"));
        }
        hasWaittimeLinks = false;
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

}
