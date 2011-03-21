package jd.controlling.reconnect.ipcheck;

/**
 * This class stores a single "Connection State". States are, for example:<br>
 * 
 * Offline {@link #ip} ==null, {@link #cause} is the Exception that occured or <br>
 * Online {@link #ip} !=null and is valid
 * 
 * @author thomas
 * 
 */
public class IPConnectionState {

    private final IP               ip;
    private final IPCheckException cause;

    /**
     * CReates an Entry for a valid IP.
     * 
     * @throws NullPointerException
     *             if ip2 is null
     * @param ip2
     */
    public IPConnectionState(final IP ip2) {
        if (ip2 == null) { throw new NullPointerException(); }
        this.ip = ip2;
        this.cause = null;
    }

    /**
     * CReates an Entry for an invalid ip. For example offline, or wrong ip
     * syntax.
     * 
     * @throws NullPointerException
     *             if e is null
     * @param e
     *            is the Exception that occured while getting the ip
     */
    public IPConnectionState(final IPCheckException e) {
        if (e == null) { throw new NullPointerException(); }
        this.cause = e;
        this.ip = null;

    }

    /**
     * Checks in case of errors, if the cause is the same
     * 
     * @param currentIP
     * @return
     */
    private boolean equalsCause(final IPConnectionState currentIP) {
        if (this.cause == null && currentIP.cause != null) { return false; }
        if (currentIP.cause == null && this.cause != null) { return false; }
        if (this.cause == currentIP.cause) { return true; }
        if (this.cause.getMessage().equals(currentIP.cause.getMessage())) { return true; }
        return false;
    }

    /**
     * return true, if the ip, or the error cause are the same. returns falls,
     * only if the COnnectionstate is different.
     * 
     * @param currentIP
     * @return
     */
    public boolean equalsLog(final IPConnectionState currentIP) {
        if (this.ip == null && currentIP.ip == null) { return this.equalsCause(currentIP); }
        if (currentIP.ip == null && this.ip != null) { return false; }
        if (this.ip == null && currentIP.ip != null) { return false; }
        return this.ip.equals(currentIP.ip);

    }

    /**
     * 
     * @return the cause that caused this state
     */

    public IPCheckException getCause() {
        return this.cause;
    }

    /**
     * returns the IP behind this connection state
     * 
     * @return
     */

    public IP getExternalIp() {
        return this.ip;
    }

    /**
     * returns if this connection state is online and has a valid external IP
     * 
     * @return
     */
    public boolean isOffline() {
        return this.ip == null;
    }

    /**
     * returns if this connections state is offline, and has NO valid external
     * ip
     * 
     * @return
     */
    public boolean isOnline() {
        return this.ip != null;
    }

}
