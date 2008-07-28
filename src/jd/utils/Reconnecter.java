package jd.utils;

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
import jd.plugins.PluginForHost;

public class Reconnecter {


    private static String CURRENT_IP = "";
    private static long lastIPUpdate = 0;
    private static boolean LAST_RECONNECT_SUCCESS = false;
    private static boolean IS_RECONNECTING = false;
    private static int RECONNECT_REQUESTS = 0;
    private static Logger logger = JDUtilities.getLogger();

    /**
     * Führt über die in der cnfig gegebenen daten einen reconnect durch.
     * 
     * @return
     */
    public static void requestReconnect() {
        RECONNECT_REQUESTS++;

    }

    public static boolean waitForReconnect() {
        requestReconnect();
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

    public static boolean isGlobalDisabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
    }

    public static boolean doReconnect() {

        if (waitForRunningRequests() > 0 && LAST_RECONNECT_SUCCESS) return true;
        boolean ipChangeSuccess = false;
        IS_RECONNECTING = true;
        if (isGlobalDisabled()) {

            if ((System.currentTimeMillis() - lastIPUpdate) > (1000 * JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL", 60*10))) {
                ipChangeSuccess = checkExternalIPChange();
                JDUtilities.getGUI().displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."), 60000);

            }

            if (!ipChangeSuccess) {
                IS_RECONNECTING = false;
                return false;
            }
        }
        if (!ipChangeSuccess) {
            if (JDUtilities.getController().getForbiddenReconnectDownloadNum() > 0) {
                //logger.finer("Downloads are running. reconnect is disabled");
                IS_RECONNECTING = false;
                return false;
            }
            Interaction.handleInteraction(Interaction.INTERACTION_BEFORE_RECONNECT, JDUtilities.getController());
            String type = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, null);
            if (type == null) {
                IS_RECONNECTING = false;
                logger.severe("Reconnect is not configured. Config->Reconnect!");
                return false;
            }
            IS_RECONNECTING = true;

            if (type.equals(JDLocale.L("modules.reconnect.types.extern", "Extern"))) {
                ipChangeSuccess = new ExternReconnect().interact(null);
            } else if (type.equals(JDLocale.L("modules.reconnect.types.batch", "Batch"))) {
                ipChangeSuccess = new BatchReconnect().interact(null);
            } else {
                ipChangeSuccess = new HTTPLiveHeader().interact(null);
            }

            LAST_RECONNECT_SUCCESS = ipChangeSuccess;
            logger.info("Reconnect success: " + ipChangeSuccess);
        }
        if (ipChangeSuccess) {
            resetAllLinks();

        }
        if (ipChangeSuccess) {
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, JDUtilities.getController());
        }
        IS_RECONNECTING = false;
        lastIPUpdate = System.currentTimeMillis();
        CURRENT_IP = JDUtilities.getIPAddress();
        RECONNECT_REQUESTS = 0;
        return ipChangeSuccess;
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
                  if (nextDownloadLink.getRemainingWaittime() > 0) {
                      nextDownloadLink.setEndOfWaittime(0);
                      logger.finer("REset GLOBALS: " + ((PluginForHost) nextDownloadLink.getPlugin()));
                      ((PluginForHost) nextDownloadLink.getPlugin()).resetPluginGlobals();
                      nextDownloadLink.getLinkStatus().setStatus(LinkStatus.TODO);

                  }
              }
          }
      }
        
    }

    private static boolean checkExternalIPChange() {
        lastIPUpdate = System.currentTimeMillis();
        String tmp = CURRENT_IP;
        CURRENT_IP = JDUtilities.getIPAddress();
        if (CURRENT_IP != null &&tmp.length()>0&& !tmp.equals(CURRENT_IP)) {
            logger.info("Detected external IP Change.");
            return true;
        }
        return false;
    }

    public static boolean doReconnectIfRequested() {
        if (RECONNECT_REQUESTS > 0) { return doReconnect(); }
        return false;
    }

    public static boolean waitForNewIP(long i) {
        requestReconnect();
        if(i>0)
        i += System.currentTimeMillis();
        boolean ret;
        while (!(ret = doReconnectIfRequested()) &&( System.currentTimeMillis() < i||i<=0)) {

            try {
                Thread.sleep(300);

            } catch (InterruptedException e) {
            }

        }

        return ret;
    }

}
