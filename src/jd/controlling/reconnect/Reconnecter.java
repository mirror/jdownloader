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
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkCheck;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Reconnecter {
    /**
     * Addons may set this ley in a messagebox property. the reconnector then
     * checks the ip again after the
     * {@link ControlEvent#CONTROL_BEFORE_RECONNECT} this can be used for
     * reconnect addons
     */
    public static final String VERFIFY_IP_AGAIN = "VERIFY_IP_AGAIN";


    
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
        if (!newState) UserIF.getInstance().displayMiniWarning(JDL.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDL.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."));
    }

    public static void setCurrentIP(String ip) {
        if (ip == null) ip = "na";
        CURRENT_IP = ip;
        LAST_UP_UPDATE_TIME = System.currentTimeMillis();
    }

    private static boolean checkExternalIPChange() {
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) return false;
        LAST_UP_UPDATE_TIME = System.currentTimeMillis();
        String tmp = CURRENT_IP;
        CURRENT_IP = IPCheck.getIPAddress();
        if (tmp == null) tmp = CURRENT_IP;
        if (!CURRENT_IP.equals("na") && tmp.length() > 0 && !tmp.equals(CURRENT_IP)) {
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
        boolean ipChangeSuccess = false;
        if (System.currentTimeMillis() - LAST_UP_UPDATE_TIME > (1000 * 60) * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL2", 10)) {
            if (Reconnecter.checkExternalIPChange()) return true;
        }
        // use a messagebox to get messages of eventl reconnect addons
        Property messageBox = new Property();
        // direct eventsender call
        JDUtilities.getController().fireControlEvent(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_BEFORE_RECONNECT, messageBox));

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

        JDUtilities.getController().fireControlEventDirect(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_RECONNECT_REQUEST, messageBox));

        if (messageBox.getBooleanProperty(VERFIFY_IP_AGAIN, false)) {
            logger.severe("Use Reconnect Addon. disable the reconnect addons to use normal reconnect settings.!");
            try {
                ipChangeSuccess = checkExternalIPChange();
            } catch (Exception e) {
                logger.severe("No connection!");
                ipChangeSuccess = false;
            }
        } else {
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
        }
        /* gestoppte downloads wieder aufnehmen */
        for (DownloadLink link : disabled) {
            link.setEnabled(true);
        }
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
        return (isReconnectRequested() && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) && SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT", true));
    }

    public static boolean doReconnectIfRequested(boolean doit) {
        if (RECONNECT_IN_PROGRESS) return false;
        /* running linkgrabber will not allow a reconnect */
        if (LinkCheck.getLinkChecker().isRunning()) return false;
        /* not allowed to do a reconnect */
        if (JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) return false;
        RECONNECT_IN_PROGRESS = true;
        boolean ret = false;
        try {
            ret = doReconnectIfRequestedInternal(doit);
            if (ret) {
                Reconnecter.resetAllLinks();
                JDUtilities.getController().fireControlEvent(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_AFTER_RECONNECT, null));
            }
        } catch (Exception e) {
        }
        RECONNECT_IN_PROGRESS = false;
        return ret;
    }

    public static boolean doReconnectIfRequestedInternal(boolean doit) {
        boolean ret = false;
        /* überhaupt ein reconnect angefragt? */
        if (isReconnectRequested()) {
            if (!doit && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                /*
                 * auto reconnect ist AUS, dann nur noch schaun ob sich ip
                 * geändert hat
                 */
                if (System.currentTimeMillis() - LAST_UP_UPDATE_TIME > (1000 * 60) * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL2", 10)) return Reconnecter.checkExternalIPChange();
                return false;

            } else {
                /* auto reconnect ist AN */
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
                    /* reconnect failed, increase fail counter */
                    ProgressController progress = new ProgressController(JDL.L("jd.controlling.reconnect.Reconnector.progress.failed", "Reconnect failed! Please check your reconnect Settings and try a Manual Reconnect!"), 100, "gui.images.reconnect_warning");
                    progress.doFinalize(10000l);
                    int counter = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0) + 1;
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, counter);
                    if (counter > 5) {
                        /*
                         * more than 5 failed reconnects in row, disable
                         * autoreconnect and show message
                         */
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_OKAY, false);
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, false);
                        UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("jd.controlling.reconnect.Reconnector.progress.failed2", "Reconnect failed too often! Autoreconnect is disabled! Please check your reconnect Settings!"));
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0);
                    }
                    JDUtilities.getConfiguration().save();
                } else {
                    /* reconnect okay, reset fail counter */
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_OKAY, true);
                    JDUtilities.getConfiguration().save();
                }

            }
        }
        return ret;
    }

    /** reset ipblocked links */
    private static void resetAllLinks() {
        ArrayList<FilePackage> packages = JDUtilities.getController().getPackages();
        /* reset hoster ipblock waittimes */
        DownloadWatchDog.getInstance().resetIPBlockWaittime(null);
        synchronized (packages) {
            for (FilePackage fp : packages) {
                for (DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    if (nextDownloadLink.getPlugin() != null && nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                        nextDownloadLink.getLinkStatus().resetWaitTime();
                    }
                }
            }
        }
    }

    /**
     * do it will start reconnectrequest even if user disabled autoreconnect
     */
    public static boolean waitForNewIP(long i, boolean doit) {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) == false && doit == false) return false;
        setReconnectRequested(true);
        final ProgressController progress = new ProgressController(JDL.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", "0:00s"), 2, "gui.images.reconnect");
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
                    progress.setStatusText(JDL.LF("gui.reconnect.progress.status2", "Reconnect running: %s", Formatter.formatSeconds((System.currentTimeMillis() - startTime) / 1000)));
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
        progress.doFinalize(4000);
        return ret;
    }

    public static boolean doManualReconnect() {
        boolean restartDownloads = DownloadWatchDog.getInstance().stopDownloads();
        boolean success = Reconnecter.waitForNewIP(1, true);
        if (restartDownloads) DownloadWatchDog.getInstance().startDownloads();
        return success;
    }

    /**
     * @param reconnectRequested
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
