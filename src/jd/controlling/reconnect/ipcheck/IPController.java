package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.reconnect.ReconnectPluginController;

import com.sun.istack.internal.Nullable;

public class IPController {
    private static final IPController INSTANCE = new IPController();

    public static IPController getInstance() {
        return IPController.INSTANCE;
    }

    /**
     * true, if the current ip has no use. we need a new one
     */
    private boolean                     invalidated = false;
    private IPLogEntry                  currentIP;
    private final ArrayList<IPLogEntry> ipLog       = new ArrayList<IPLogEntry>();

    private IPController() {

    }

    /**
     * forces the class to get a new IP
     * 
     * @return
     */
    public IP fetchIP() {
        IPLogEntry newIP;
        try {
            newIP = new IPLogEntry(this.getIPCheckProvider().getIP());
        } catch (final IPCheckException e) {
            e.printStackTrace();
            newIP = new IPLogEntry(null, e);

        }
        if (this.ipLog.size() > 0) {

            final IPLogEntry entry = this.ipLog.get(this.ipLog.size() - 1);
            if (!entry.equalsLog(newIP)) {
                this.ipLog.add(newIP);
                this.currentIP = newIP;
            }
        } else {
            this.ipLog.add(newIP);
            this.currentIP = newIP;
        }
        return this.currentIP.getIp();
    }

    /**
     * returns the latest log ebtry, or fetches the ip to get one
     * 
     * @return
     */
    private synchronized IPLogEntry getCurrentLog() {
        if (this.currentIP == null) {
            this.fetchIP();
        }
        return this.currentIP;
    }

    /**
     * Returns the current external IP. fetches new ip if if is marked as
     * invalid, or is null
     * 
     * 
     * @return
     */
    @Nullable
    public IP getIP() {
        return this.getCurrentLog().getIp();
    }

    /**
     * finds the best ipcheck provider
     * 
     * @return
     */
    private IPCheckProvider getIPCheckProvider() {
        final IPCheckProvider p = ReconnectPluginController.getInstance().getActivePlugin().getIPCheckProvider();

        if (p == null) {

            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {

                return BalancedWebIPCheck.getInstance();
            } else {
                return CustomWebIpCheck.getInstance();
            }
        }
        return null;
    }

    /**
     * Tells the IpController, that the current ip is "BAD". We need a new one
     */
    public void invalidate() {
        this.invalidated = true;
    }

    /**
     * returns true if the current ip is "Bad" and we need a new one
     * 
     * @return
     */
    public boolean isInvalidated() {
        return this.invalidated;
    }

    /**
     * gets new ip and validates it.
     */
    public boolean validate() {
        if (!this.invalidated) { return true; }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            // IP check disabled. each validate request is successfull
            this.invalidated = false;

        }
        return !this.isInvalidated();
    }

    public boolean validate(final int waitForIPTime, final int ipCheckInterval) throws InterruptedException {

        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            Thread.sleep(waitForIPTime);
            // IP check disabled. each validate request is successfull
            this.invalidated = false;

        }
        final long endTime = System.currentTimeMillis() + waitForIPTime * 1000;
        final IPLogEntry before = this.getCurrentLog();

        while (System.currentTimeMillis() < endTime) {
            this.fetchIP();
            if (this.currentIP != before && this.currentIP.getIp() != null) {

                this.invalidated = false;
                break;
            }

        }

        return !this.isInvalidated();
    }
}
