package jd.controlling.reconnect.ipcheck;

public class IPLogEntry {

    private final IP               ip;
    private final IPCheckException cause;

    public IPLogEntry(final IP ip2) {
        this(ip2, null);
    }

    public IPLogEntry(final IP ip, final IPCheckException e) {
        this.ip = ip;
        this.cause = e;

    }

    /**
     * Checks in case of errors, if the cause is the same
     * 
     * @param currentIP
     * @return
     */
    private boolean equalsCause(final IPLogEntry currentIP) {
        if (this.cause == null && currentIP.cause != null) { return false; }
        if (currentIP.cause == null && this.cause != null) { return false; }
        if (this.cause == currentIP.cause) { return true; }
        if (this.cause.getMessage().equals(currentIP.cause.getMessage())) { return true; }
        return false;
    }

    /**
     * return true, if the ip, or the error cause are the same
     * 
     * @param currentIP
     * @return
     */
    public boolean equalsLog(final IPLogEntry currentIP) {
        if (this.ip == null && currentIP.getIp() == null) { return this.equalsCause(currentIP); }
        if (currentIP.ip == null && this.ip != null) { return false; }
        return this.ip.equals(currentIP.ip);

    }

    public IPCheckException getCause() {
        return this.cause;
    }

    public IP getIp() {
        return this.ip;
    }

}
