package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
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
    private boolean                            invalidated  = false;
    private IPConnectionState                  latestConnectionState;
    private final ArrayList<IPConnectionState> ipLog        = new ArrayList<IPConnectionState>();
    /**
     * blacklist for not working ip check providers
     */
    private final ArrayList<IPCheckProvider>   badProviders = new ArrayList<IPCheckProvider>();

    private IPController() {

    }

    /**
     * forces the class to get a new IP
     * 
     * @return
     */
    public IP fetchIP() {
        IPConnectionState newIP = null;
        IPCheckProvider icp = null;
        while (true) {
            try {
                icp = this.getIPCheckProvider();
                newIP = new IPConnectionState(icp.getExternalIP());
                break;
            } catch (final InvalidProviderException e) {
                // IP check provider is bad.
                this.badProviders.add(icp);

            } catch (final IPCheckException e) {
                JDLogger.getLogger().info(e.getMessage());
                newIP = new IPConnectionState(e);
                break;
            }

        }
        if (this.ipLog.size() > 0) {

            final IPConnectionState entry = this.ipLog.get(this.ipLog.size() - 1);
            if (!entry.equalsLog(newIP)) {
                this.ipLog.add(newIP);
                this.latestConnectionState = newIP;
            }
        } else {
            this.ipLog.add(newIP);
            this.latestConnectionState = newIP;
        }
        return this.latestConnectionState.getExternalIp();
    }

    /**
     * returns the latest log ebtry, or fetches the ip to get one
     * 
     * @return
     */
    private synchronized IPConnectionState getCurrentLog() {
        if (this.latestConnectionState == null) {
            this.fetchIP();
        }
        return this.latestConnectionState;
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
        return this.getCurrentLog().getExternalIp();
    }

    /**
     * finds the best ipcheck provider
     * 
     * @return
     */
    private IPCheckProvider getIPCheckProvider() {
        IPCheckProvider p = ReconnectPluginController.getInstance().getActivePlugin().getIPCheckProvider();

        if (p == null || this.badProviders.contains(p)) {

            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {

                p = BalancedWebIPCheck.getInstance();
            } else {
                p = CustomWebIpCheck.getInstance();
            }
        }
        return p;
    }

    /**
     * Tells the IpController, that the current ip is "BAD". We need a new one<br>
     * 
     * @see #validate()
     * @see #validate(int, int)
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
     * gets the latest connection state and validates if we have a new ip.<br>
     * 
     * @see #validate(int, int) for more details.<br>
     * 
     *      This method only does one single Check.
     */
    public boolean validate() {
        if (!this.invalidated) { return true; }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            // IP check disabled. each validate request is successfull
            this.invalidated = false;

        }

        final IPConnectionState before = this.getCurrentLog();
        this.fetchIP();

        if (this.latestConnectionState != before && this.latestConnectionState.getExternalIp() != null) {

            this.invalidated = false;

        }
        return !this.isInvalidated();
    }

    /**
     * check for max waitForIPTime seconds in an interval of ipCheckInterval if
     * the ip is valid.<br>
     * Call {@link #invalidate()} to invalidate the current state. AFterwards a
     * reconnect can get a new ip.<br>
     * this method gets the new connectionstate and validates it
     * 
     * 
     * @param waitForIPTime
     * @param ipCheckInterval
     * @return
     * @throws InterruptedException
     */
    public boolean validate(final int waitForIPTime, final int ipCheckInterval) throws InterruptedException {
        if (!this.invalidated) { return true; }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            Thread.sleep(waitForIPTime);
            // IP check disabled. each validate request is successfull
            this.invalidated = false;

        }
        final long endTime = System.currentTimeMillis() + waitForIPTime * 1000;
        final IPConnectionState before = this.getCurrentLog();
        IPConnectionState offline = null;
        while (System.currentTimeMillis() < endTime) {
            this.fetchIP();

            if (!before.equalsLog(this.latestConnectionState) && this.latestConnectionState.getExternalIp() != null) {

                this.invalidated = false;

                break;
            } else if (offline != null && this.latestConnectionState.isOnline()) {
                // we have been offline, and online again, but have same ip
                this.invalidated = true;
            } else if (!before.equalsLog(this.latestConnectionState) && this.latestConnectionState.getExternalIp() == null) {
                // errorhandling
                if (this.latestConnectionState.getCause() != null) {
                    try {
                        throw this.latestConnectionState.getCause();
                    } catch (final ForbiddenIPException e) {
                        // forbidden IP.. no need to wait
                        this.invalidated = true;
                        break;

                    } catch (final Throwable e) {
                        // nothing
                    }
                }

            }
            if (before.isOnline() && this.latestConnectionState.isOffline()) {
                offline = this.latestConnectionState;
            }
            Thread.sleep(Math.max(250, ipCheckInterval));

        }

        return !this.isInvalidated();
    }
}
