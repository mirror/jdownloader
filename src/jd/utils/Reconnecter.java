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

package jd.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.BatchReconnect;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.router.RouterInfoCollector;

public class Reconnecter {

    private static String CURRENT_IP = "";
    private static boolean IS_RECONNECTING = false;
    private static boolean LAST_RECONNECT_SUCCESS = false;
    private static long lastIPUpdate = 0;
    private static Logger logger = JDUtilities.getLogger();
    private static int RECONNECT_REQUESTS = 0;

    private static boolean checkExternalIPChange() {
        lastIPUpdate = System.currentTimeMillis();
        String tmp = CURRENT_IP;
        CURRENT_IP = JDUtilities.getIPAddress();
        if (CURRENT_IP != null && tmp.length() > 0 && !tmp.equals(CURRENT_IP)) {
            logger.info("Detected external IP Change.");
            return true;
        }
        return false;
    }

    public static boolean doReconnect() {

        if (Reconnecter.waitForRunningRequests() > 0 && LAST_RECONNECT_SUCCESS) { return true; }
        boolean ipChangeSuccess = false;
        IS_RECONNECTING = true;
        if (Reconnecter.isGlobalDisabled()) {

            if (System.currentTimeMillis() - lastIPUpdate > 1000 * JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60 * 10)) {
                ipChangeSuccess = Reconnecter.checkExternalIPChange();
                JDUtilities.getGUI().displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."), 60000);

            }

            if (!ipChangeSuccess) {
                IS_RECONNECTING = false;

                return false;
            }
        }

        ArrayList<DownloadLink> disabled = new ArrayList<DownloadLink>();
        if (!ipChangeSuccess) {
            if (JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) {
                // logger.finer("Downloads are running. reconnect is disabled");
                IS_RECONNECTING = false;
                return false;
            }

            Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, JDUtilities.getController());
            String type = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));
            if (type == null) {
                IS_RECONNECTING = false;
                logger.severe("Reconnect is not configured. Config->Reconnect!");
                return false;
            }
            IS_RECONNECTING = true;
            logger.info("DO RECONNECT NOW");
            boolean interrupt = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", true);
            if (interrupt) {
                JDUtilities.getController().pauseDownloads(true);

                Iterator<FilePackage> iterator = JDUtilities.getController().getPackages().iterator();
                FilePackage fp = null;
                DownloadLink nextDownloadLink;

                while (iterator.hasNext()) {
                    fp = iterator.next();
                    Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                    while (it2.hasNext()) {
                        nextDownloadLink = it2.next();
                        if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                            nextDownloadLink.setEnabled(false);
                            logger.info("disbaled +" + nextDownloadLink);
                            disabled.add(nextDownloadLink);
                        }
                    }
                }
            }
            if (type.equals(JDLocale.L("modules.reconnect.types.extern", "Extern"))) {
                ipChangeSuccess = new ExternReconnect().interact(null);
            } else if (type.equals(JDLocale.L("modules.reconnect.types.batch", "Batch"))) {
                ipChangeSuccess = new BatchReconnect().interact(null);
            } else if (type.equals(JDLocale.L("modules.reconnect.types.clr", "CLR Script"))) {
                ipChangeSuccess = new HTTPLiveHeader().interact(null);
            } else {
                ipChangeSuccess = new HTTPLiveHeader().interact(null);
            }
            if (interrupt) {
                JDUtilities.getController().pauseDownloads(false);
                for (DownloadLink link : disabled) {
                    logger.info("enable +" + link);
                    link.setEnabled(true);
                }

            }

            LAST_RECONNECT_SUCCESS = ipChangeSuccess;
            logger.info("Reconnect success: " + ipChangeSuccess);
        }

        if (ipChangeSuccess) {
            Reconnecter.resetAllLinks();
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, JDUtilities.getController());
            RouterInfoCollector.showDialog();
        }
        IS_RECONNECTING = false;
        lastIPUpdate = System.currentTimeMillis();
        CURRENT_IP = JDUtilities.getIPAddress();
        RECONNECT_REQUESTS = 0;
        return ipChangeSuccess;
    }

    public static boolean doReconnectIfRequested() {

        if (RECONNECT_REQUESTS > 0) { return Reconnecter.doReconnect(); }
        return false;
    }

    public static boolean isGlobalDisabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
    }

    /**
     * Führt über die in der cnfig gegebenen daten einen reconnect durch.
     * 
     * @return
     */
    public static void requestReconnect() {
        RECONNECT_REQUESTS++;

    }

    private static void resetAllLinks() {
        Vector<FilePackage> packages = JDUtilities.getController().getPackages();
        synchronized (packages) {
            Iterator<FilePackage> iterator = packages.iterator();
            FilePackage fp = null;
            DownloadLink nextDownloadLink;
            while (iterator.hasNext()) {
                fp = iterator.next();
                Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator();
                while (it2.hasNext()) {
                    nextDownloadLink = it2.next();
                    if (nextDownloadLink.getPlugin().getRemainingHosterWaittime() > 0) {
                        if (nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED)) {
                            nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);

                            nextDownloadLink.getPlugin().resetHosterWaitTime();
                            logger.finer("REset GLOBALS: " + nextDownloadLink.getPlugin());
                            nextDownloadLink.getPlugin().resetPluginGlobals();
                        }

                    }
                }
            }
        }

    }

    public static boolean waitForNewIP(long i) {
        Reconnecter.requestReconnect();
        if (i > 0) {
            i += System.currentTimeMillis();
        }
        boolean ret;
        while (!(ret = Reconnecter.doReconnectIfRequested()) && (System.currentTimeMillis() < i || i <= 0)) {

            try {
                Thread.sleep(300);

            } catch (InterruptedException e) {
            }

        }
        // TODO if(ret) RouterInfoCollector.showDialog();

        return ret;
    }

    public static boolean waitForReconnect() {
        Reconnecter.requestReconnect();
        return true;
    }

    private static int waitForRunningRequests() {
        int wait = 0;
        while (IS_RECONNECTING) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            wait += 500;

        }
        return wait;

    }

}
