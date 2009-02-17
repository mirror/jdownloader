package jd.controlling.reconnect;

import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public abstract class ReconnectMethod {

    protected static Logger logger = JDUtilities.getLogger();

    protected Property configuration;

    public static final String PARAM_RECONNECT_TYPE = "RECONNECT_TYPE";

    public static final String PARAM_IPCHECKWAITTIME = "RECONNECT_IPCHECKWAITTIME";

    public static final String PARAM_RETRIES = "RECONNECT_RETRIES";

    public static final String PARAM_WAITFORIPCHANGE = "RECONNECT_WAITFORIPCHANGE";

    protected transient ConfigContainer config;

    protected int retries = 0;

    public ReconnectMethod() {
        config = null;
    }

    public final boolean doReconnect() {
        retries++;
        ProgressController progress = new ProgressController(this.toString(), 10);

        progress.setStatusText(JDLocale.L("reconnect.progress.1_retries", "Reconnect #") + retries);

        int waittime = configuration.getIntegerProperty(PARAM_IPCHECKWAITTIME, 0);
        int maxretries = configuration.getIntegerProperty(PARAM_RETRIES, 0);
        int waitForIp = configuration.getIntegerProperty(PARAM_WAITFORIPCHANGE, 10);

        logger.info("Starting " + this.toString() + " #" + retries);
        String preIp = JDUtilities.getIPAddress(null);

        progress.increase(1);
        progress.setStatusText(JDLocale.L("reconnect.progress.2_oldIP", "Reconnect Old IP:") + preIp);
        logger.finer("IP before: " + preIp);
        if (!runCommands(progress)) {
            logger.info("An error occured while processing the reconnect ... Terminating");
            return false;
        }
        logger.finer("Wait " + waittime + " seconds ...");
        try {
            Thread.sleep(waittime * 1000);
        } catch (InterruptedException e) {
        }

        String afterIP = JDUtilities.getIPAddress(null);

        if (!JDUtilities.validateIP(afterIP)) {
            logger.warning("IP " + afterIP + " was filtered by mask: " + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));
            JDUtilities.getGUI().displayMiniWarning(String.format(JDLocale.L("reconnect.ipfiltered.warning.short", "Die IP %s wurde als nicht erlaubt identifiziert"), afterIP), null, 20);
            afterIP = "offline";
        }
        logger.finer("Ip after: " + afterIP);
        progress.setStatusText(JDLocale.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        logger.info("Wait " + waitForIp + " sek for new ip");

        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals("offline"))) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
            }
            afterIP = JDUtilities.getIPAddress(null);
            progress.setStatusText(JDLocale.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
            logger.finer("Ip Check: " + afterIP);
        }

        logger.finer("Ip after: " + afterIP);
        if (afterIP.equals("offline") && !afterIP.equals(preIp)) {
            logger.warning("JD could disconnect your router, but could not connect afterwards. Try to rise the option 'Wait until first IP Check'");
            endTime = System.currentTimeMillis() + 120 * 1000;
            while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP.equals("offline"))) {
                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException e) {
                }
                afterIP = JDUtilities.getIPAddress(null);
                progress.setStatusText(JDLocale.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", preIp, afterIP));

                logger.finer("Ip Check: " + afterIP);
            }
        }

        if (!afterIP.equals(preIp) && !afterIP.equals("offline")) {
            progress.finalize();
            logger.info("Reconnect successful: " + afterIP);
            return true;
        }

        if (maxretries == -1 || retries <= maxretries) {
            progress.finalize();
            return doReconnect();
        }

        progress.finalize();
        logger.info("Reconnect failed: " + afterIP);
        return false;

    }

    protected abstract boolean runCommands(ProgressController progress);

    public abstract void initConfig();

    public final ConfigContainer getConfig() {
        if (config == null) {
            config = new ConfigContainer(this);
            initConfig();
        }
        return config;
    }

    @Deprecated
    public void resetMethod() {
        retries = 0;
    }

}
