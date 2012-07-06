package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ipcheck.event.IPControllEvent;
import jd.controlling.reconnect.ipcheck.event.IPControllEventSender;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class IPController extends ArrayList<IPConnectionState> {
    /**
     * 
     */
    private static final long         serialVersionUID = -6856149094542337379L;
    private static final IPController INSTANCE         = new IPController();

    public static IPController getInstance() {
        return IPController.INSTANCE;
    }

    /**
     * true, if the current ip has no use. we need a new one
     */
    private boolean                          invalidated  = false;
    private IPConnectionState                latestConnectionState;
    private final Object                     LOCK         = new Object();
    /**
     * blacklist for not working ip check providers
     */
    private final ArrayList<IPCheckProvider> badProviders = new ArrayList<IPCheckProvider>();
    private IPConnectionState                invalidState = null;
    private IPControllEventSender            eventSender;

    public IPControllEventSender getEventSender() {
        return eventSender;
    }

    private IPController() {
        eventSender = new IPControllEventSender();
    }

    @Override
    public boolean add(final IPConnectionState state) {
        if (state == null) { return false; }
        synchronized (this.LOCK) {
            if (this.size() > 0) {
                final IPConnectionState entry = this.get(this.size() - 1);
                /* new and current state are equal */
                if (entry.equalsLog(state)) { return false; }
            }
            /* new IPConnectionState reached */
            IPConnectionState oldState = latestConnectionState;
            this.latestConnectionState = state;

            eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.STATECHANGED, oldState, state));

            return super.add(state);
        }
    }

    protected boolean changedIP() {
        if (this.invalidState == null) { return false; }
        /* we dont have any previous states, we cannot check if ip changed */
        if (this.size() == 0) { return false; }
        synchronized (this.LOCK) {
            /* fetch current ip */
            this.fetchIP();
            final IPConnectionState current = this.latestConnectionState;
            /* currently offline = no ip change */
            if (current.isOffline()) {

            return false; }

            // run back the statelog, until we reached the invalidState. Check
            // all states on the way for a new ip
            for (int index = this.size() - 1; index >= 0; index--) {
                if (this.get(index) == this.invalidState) {
                    /*
                     * we reached the element we began with, so check changed IP and then stop
                     */
                    if (!this.invalidState.equalsLog(current)) {

                        eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.IP_CHANGED, invalidState, current));
                        return true;
                    }
                    return false;
                }
                /*
                 * we found a state that was online and had different ip that new state, so ip changed
                 */
                if (this.get(index).isOnline() && !this.get(index).equalsLog(current)) {
                    eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.IP_CHANGED, invalidState, current));

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * forces the class to get a new IP
     * 
     * @return
     */
    protected IP fetchIP() {
        IPConnectionState newIP = null;
        IPCheckProvider icp = null;
        while (true) {
            try {
                icp = this.getIPCheckProvider();
                newIP = new IPConnectionState(icp.getExternalIP());
                System.out.println("IP: " + newIP.getExternalIp());
                break;
            } catch (final InvalidProviderException e) {
                // IP check provider is bad.
                this.badProviders.add(icp);
            } catch (final IPCheckException e) {
                // JDLogger.getLogger().info(e.getMessage());
                newIP = new IPConnectionState(e);
                break;
            }
        }

        IPConnectionState old = latestConnectionState;
        if (add(newIP)) {
            if (latestConnectionState.isOffline()) {

                eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.OFFLINE));

            }
            if ((old == null || old.isOffline()) && !newIP.isOffline()) {
                eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.ONLINE, newIP));

            }
        }
        return this.latestConnectionState.getExternalIp();
    }

    /**
     * returns the latest log entry, or fetches the ip to get one
     * 
     * @return
     */
    public synchronized IPConnectionState getIpState() {
        if (this.latestConnectionState == null) {
            fetchIP();
        }
        return this.latestConnectionState;
    }

    /**
     * Returns the current external IP. fetches new ip if if is marked as invalid, or is null
     * 
     * 
     * @return
     */

    public IP getIP() {
        return this.getIpState().getExternalIp();
    }

    /**
     * finds the best ipcheck provider
     * 
     * @return
     */
    private IPCheckProvider getIPCheckProvider() {
        IPCheckProvider p = ReconnectPluginController.getInstance().getActivePlugin().getIPCheckProvider();
        if (p == null || this.badProviders.contains(p)) {

            if (!JsonConfig.create(ReconnectConfig.class).isCustomIPCheckEnabled()) {
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
     * @see #validateAndWait(int, int, int)
     */
    public void invalidate() {
        if (this.invalidated == true) { return; }
        this.setInvalidated(true);
        this.invalidState = this.getIpState();
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
     * @see #validateAndWait(int, int, int) for more details.<br>
     * 
     *      This method only does one single Check.
     */
    public boolean validate() {
        if (!this.invalidated) { return true; }
        if (JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled()) {
            // IP check disabled. each validate request is successful

            setInvalidated(false);
            return true;
        }
        if (this.changedIP()) {
            setInvalidated(false);
            return true;
        } else {
            setInvalidated(true);
            return false;
        }

    }

    private void setInvalidated(boolean invalidated) {
        if (invalidated == this.invalidated) return;
        this.invalidated = invalidated;
        if (invalidated) {
            eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.INVALIDATED, getIpState()));
        } else {
            eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.VALIDATED, invalidState, getIpState()));
        }
    }

    /**
     * check for max waitForIPTime seconds in an interval of ipCheckInterval if the ip is valid.<br>
     * Call {@link #invalidate()} to invalidate the current state. AFterwards a reconnect can get a new ip.<br>
     * this method gets the new connectionstate and validates it
     * 
     * 
     * @param waitForIPTime
     * @param waitForOfflineTime
     *            TODO
     * @param ipCheckInterval
     * @return
     * @throws InterruptedException
     */
    public boolean validateAndWait(final int waitForIPTime, int waitForOfflineTime, final int ipCheckInterval) throws InterruptedException {
        if (!this.invalidated) { return true; }
        if (JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled()) {
            Thread.sleep(waitForIPTime);
            // IP check disabled. each validate request is successful
            this.setInvalidated(false);
            return true;
        }
        final long endTime = System.currentTimeMillis() + waitForIPTime * 1000;
        final long endOfflineTime = System.currentTimeMillis() + waitForOfflineTime * 1000;
        boolean hasBeenOffline = false;
        LogSource logger = LogController.CL();
        try {
            while (true) {
                /* ip change detected then we can stop */
                this.validate();
                if (!isInvalidated()) {
                    return true;
                } else if (latestConnectionState.isOffline()) {
                    hasBeenOffline = true;
                }

                if (this.latestConnectionState.getCause() != null) {
                    try {
                        throw this.latestConnectionState.getCause();
                    } catch (final ForbiddenIPException e) {
                        eventSender.fireEvent(new IPControllEvent(IPControllEvent.Type.FORBIDDEN_IP, getIpState()));
                        // forbidden IP.. no need to wait
                        this.setInvalidated(true);
                        return false;

                    } catch (final Throwable e) {
                        // nothing
                    }
                }
                if (!hasBeenOffline && System.currentTimeMillis() >= endOfflineTime) {
                    // break
                    logger.info("Not offline after " + waitForOfflineTime + " seconds");
                    break;

                }
                if (System.currentTimeMillis() >= endTime) {
                    logger.info("Not reconnected after " + waitForIPTime + " seconds");
                    break;
                }
                Thread.sleep(Math.max(250, ipCheckInterval * 1000));
            }
        } finally {
            logger.close();
        }
        return !this.isInvalidated();
    }

    public void waitUntilWeAreOnline() throws InterruptedException {
        // Make sure that we are online
        while (IPController.getInstance().getIpState().isOffline()) {
            IPController.getInstance().invalidate();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Thread.sleep(1000);
            IPController.getInstance().validate();
        }
    }

}
