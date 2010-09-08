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
import jd.controlling.reconnect.plugins.ReconnectPluginController;
import jd.event.ControlEvent;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public final class Reconnecter {

    private static final String NA                    = "na";

    /**
     * Addons may set this ley in a messagebox property. the reconnector then
     * checks the ip again after the
     * {@link ControlEvent#CONTROL_BEFORE_RECONNECT} this can be used for
     * reconnect addons
     */
    public static final String  VERIFY_IP_AGAIN       = "VERIFY_IP_AGAIN";

    private static String       CURRENT_IP            = "";

    /**
     * Set to true only if there is a reconnect running currently
     */
    private static boolean      RECONNECT_IN_PROGRESS = false;
    /**
     * Timestampo of the latest IP CHange
     */
    private static long         LAST_UP_UPDATE_TIME   = 0;
    private static final Logger LOG                   = JDLogger.getLogger();
    /**
     * Only true if a reconect has been requestst.
     */
    private static boolean      RECONNECT_REQUESTED   = false;

    private static boolean checkExternalIPChange() {
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) { return false; }
        Reconnecter.LAST_UP_UPDATE_TIME = System.currentTimeMillis();
        String tmp = Reconnecter.CURRENT_IP;
        Reconnecter.CURRENT_IP = IPCheck.getIPAddress();
        if (tmp == null) {
            tmp = Reconnecter.CURRENT_IP;
        }
        if (!Reconnecter.CURRENT_IP.equals(Reconnecter.NA) && tmp.length() > 0 && !tmp.equals(Reconnecter.CURRENT_IP)) {
            Reconnecter.LOG.info("Detected external IP Change.");
            return true;
        }
        return false;
    }

    public static boolean doManualReconnect() {
        final boolean restartDownloads = DownloadWatchDog.getInstance().stopDownloads();
        final boolean success = Reconnecter.waitForNewIP(1, true);
        if (restartDownloads) {
            DownloadWatchDog.getInstance().startDownloads();
        }
        return success;
    }

    /**
     * Führt einen Reconnect durch.
     * 
     * @return <code>true</code>, wenn der Reconnect erfolgreich war, sonst
     *         <code>false</code>
     */
    public static boolean doReconnect() {
        boolean ipChangeSuccess = false;
        if (System.currentTimeMillis() - Reconnecter.LAST_UP_UPDATE_TIME > 1000 * 60 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL2", 10)) {
            if (Reconnecter.checkExternalIPChange()) { return true; }
        }
        // use a messagebox to get messages of eventl reconnect addons
        final Property messageBox = new Property();
        // direct eventsender call
        JDUtilities.getController().fireControlEvent(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_BEFORE_RECONNECT, messageBox));

        Reconnecter.LOG.info("Try to reconnect...");
        /* laufende downloads stoppen */
        final ArrayList<DownloadLink> disabled = DownloadWatchDog.getInstance().getRunningDownloads();
        if (!disabled.isEmpty()) {
            Reconnecter.LOG.info("Stopping all running downloads!");

            for (final DownloadLink link : disabled) {
                link.setEnabled(false);
            }
            /* warte bis alle gestoppt sind */
            for (int wait = 0; wait < 10; wait++) {
                if (DownloadWatchDog.getInstance().getActiveDownloads() == 0) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                    Reconnecter.LOG.info("Still waiting for all downloads to stop!");
                } catch (final InterruptedException e) {
                    break;
                }
            }
            if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
                Reconnecter.LOG.severe("Could not stop all running downloads!");
            }
        }
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_RECONNECT_REQUEST, messageBox));

        if (messageBox.getBooleanProperty(Reconnecter.VERIFY_IP_AGAIN, false)) {
            Reconnecter.LOG.severe("Using special reconnect addon");
            try {
                ipChangeSuccess = Reconnecter.checkExternalIPChange();
            } catch (final Exception e) {

                Reconnecter.LOG.severe("No connection!");
                ipChangeSuccess = false;
            }
        } else {
            try {

                ipChangeSuccess = ReconnectPluginController.getInstance().doReconnect();

            } catch (final Exception e) {
                Reconnecter.LOG.severe("ReconnectMethod failed!");
            }
        }
        /* gestoppte downloads wieder aufnehmen */
        for (final DownloadLink link : disabled) {
            link.setEnabled(true);
        }
        JDUtilities.getController().fireControlEvent(new ControlEvent(JDUtilities.getController(), ControlEvent.CONTROL_AFTER_RECONNECT, null));

        return ipChangeSuccess;
    }

    public static boolean doReconnectIfRequested(final boolean doit) {
        if (Reconnecter.RECONNECT_IN_PROGRESS) { return false; }
        /* running linkgrabber will not allow a reconnect */
        if (LinkCheck.getLinkChecker().isRunning()) { return false; }
        /* not allowed to do a reconnect */
        if (JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) { return false; }
        Reconnecter.RECONNECT_IN_PROGRESS = true;
        boolean ret = false;
        try {
            ret = Reconnecter.doReconnectIfRequestedInternal(doit);
            if (ret) {
                Reconnecter.resetAllLinks();
            }
        } catch (final Exception e) {
        }
        Reconnecter.RECONNECT_IN_PROGRESS = false;
        return ret;
    }

    public static boolean doReconnectIfRequestedInternal(final boolean doit) {
        boolean ret = false;
        final Configuration configuration = JDUtilities.getConfiguration();
        /* überhaupt ein reconnect angefragt? */
        if (Reconnecter.isReconnectRequested()) {
            if (!doit && !configuration.getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                /*
                 * auto reconnect ist AUS, dann nur noch schaun ob sich ip
                 * geändert hat
                 */
                if (System.currentTimeMillis() - Reconnecter.LAST_UP_UPDATE_TIME > 1000 * 60 * SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL2", 10)) { return Reconnecter.checkExternalIPChange(); }
                return false;

            } else {
                /* auto reconnect ist AN */
                try {
                    ret = Reconnecter.doReconnect();
                    if (ret) {
                        Reconnecter.LOG.info("Reconnect successful!");
                    } else {
                        Reconnecter.LOG.info("Reconnect failed!");
                    }
                } catch (final Exception e) {
                    Reconnecter.LOG.finest("Reconnect failed.");
                }
                if (ret == false) {
                    /* reconnect failed, increase fail counter */
                    final ProgressController progress = new ProgressController(JDL.L("jd.controlling.reconnect.Reconnector.progress.failed", "Reconnect failed! Please check your reconnect Settings and try a Manual Reconnect!"), 100, "gui.images.reconnect_warning");
                    progress.doFinalize(10000l);
                    final int counter = configuration.getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0) + 1;
                    configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, counter);
                    if (counter > 5) {
                        /*
                         * more than 5 failed reconnects in row, disable
                         * autoreconnect and show message
                         */
                        configuration.setProperty(Configuration.PARAM_RECONNECT_OKAY, false);
                        configuration.setProperty(Configuration.PARAM_ALLOW_RECONNECT, false);
                        UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("jd.controlling.reconnect.Reconnector.progress.failed2", "Reconnect failed too often! Autoreconnect is disabled! Please check your reconnect Settings!"));
                        configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0);
                    }
                    configuration.save();
                } else {
                    /* reconnect okay, reset fail counter */
                    configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER, 0);
                    configuration.setProperty(Configuration.PARAM_RECONNECT_OKAY, true);
                    configuration.save();
                }

            }
        }
        return ret;
    }

    public static boolean isReconnectAllowed() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
    }

    public static boolean isReconnecting() {
        return Reconnecter.RECONNECT_IN_PROGRESS;
    }

    /**
     * Returns true, if there is a requested reconnect qaiting, and the user
     * selected not to start new downloads of reconnects are waiting
     * 
     * @return
     */
    public static boolean isReconnectPrefered() {
        return Reconnecter.isReconnectRequested() && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) && SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_PREFER_RECONNECT", true);
    }

    /**
     * @return the RECONNECT_REQUESTED
     */
    public static boolean isReconnectRequested() {
        return Reconnecter.RECONNECT_REQUESTED;
    }

    /** reset ipblocked links */
    private static void resetAllLinks() {
        final ArrayList<FilePackage> packages = JDUtilities.getController().getPackages();
        /* reset hoster ipblock waittimes */
        DownloadWatchDog.getInstance().resetIPBlockWaittime(null);
        synchronized (packages) {
            for (final FilePackage fp : packages) {
                for (final DownloadLink nextDownloadLink : fp.getDownloadLinkList()) {
                    if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                        nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                        nextDownloadLink.getLinkStatus().resetWaitTime();
                    }
                }
            }
        }
    }

    public static void setCurrentIP(final String ip) {
        Reconnecter.CURRENT_IP = ip == null ? Reconnecter.NA : ip;
        Reconnecter.LAST_UP_UPDATE_TIME = System.currentTimeMillis();
    }

    /**
     * @param reconnectRequested
     *            the RECONNECT_REQUESTED to set
     */
    public static void setReconnectRequested(final boolean reconnectRequested) {
        Reconnecter.RECONNECT_REQUESTED = reconnectRequested;
    }

    public static void toggleReconnect() {
        final Configuration configuration = JDUtilities.getConfiguration();
        final boolean newState = !configuration.getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        configuration.setProperty(Configuration.PARAM_ALLOW_RECONNECT, newState);
        configuration.save();
        if (!newState) {
            UserIF.getInstance().displayMiniWarning(JDL.L("gui.warning.reconnect.hasbeendisabled", "Reconnection is disabled!"), JDL.L("gui.warning.reconnect.hasbeendisabled.tooltip", "To allow JDownloader to perform automated reconnections, you should enable this feature!"));
        }
    }

    /**
     * do it will start reconnectrequest even if user disabled autoreconnect
     */
    public static boolean waitForNewIP(long i, final boolean doit) {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true) == false && doit == false) { return false; }
        Reconnecter.setReconnectRequested(true);
        final ProgressController progress = new ProgressController(JDL.LF("gui.reconnect.progress.status", "Reconnect running: %s m:s", "0:00s"), 2, "gui.images.reconnect");
        if (i > 0) {
            i += System.currentTimeMillis();
        }
        progress.setStatus(1);
        final long startTime = System.currentTimeMillis();
        boolean ret;
        final Thread timer = new Thread() {
            @Override
            public void run() {
                while (true) {
                    progress.setStatusText(JDL.LF("gui.reconnect.progress.status2", "Reconnect running: %s", Formatter.formatSeconds((System.currentTimeMillis() - startTime) / 1000)));
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        return;
                    }
                }

            }
        };
        timer.start();
        while (!(ret = Reconnecter.doReconnectIfRequested(true)) && (System.currentTimeMillis() < i || i <= 0)) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
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
        Reconnecter.setReconnectRequested(false);
        progress.doFinalize(4000);
        return ret;
    }

    /**
     * Don't let anyone instantiate this class.
     */
    private Reconnecter() {
    }

}
