package jd.controlling.proxy;

import jd.plugins.DownloadLink;

public class ProxyBlock {

    final private DownloadLink link;
    final private long         blockedUntil;
    final private REASON       reason;

    public static enum REASON {
        IP,
        UNAVAIL
    }

    public ProxyBlock(DownloadLink link, long until, REASON reason) {
        this.link = link;
        this.blockedUntil = until;
        this.reason = reason;
    }

    public DownloadLink getLink() {
        return link;
    }

    public long getBlockedUntil() {
        return blockedUntil;
    }

    public long getBlockedTimeout() {
        final long now = System.currentTimeMillis();
        final long ab = blockedUntil - now;
        return Math.max(0l, ab);
    }

    /**
     * @return the reason
     */
    public REASON getReason() {
        return reason;
    }

}
