package jd.plugins;

public class MultiHostHost {
    /** How long shall we block this host if a limit gets reached? Until the next day/hour? */
    public enum LimitResetMode {
        DAILY;
    }

    /** Why was this host blocked? Because of too many errored out tries via JD or because of the multihost/multihost limits? */
    public enum DeactivatedReason {
        JDOWNLOADER,
        MULTIHOST;
    }

    private String domain                    = null;
    private int    linksLeft                 = -1;
    private int    linksLeftMax              = -1;
    private long   trafficLeft               = -1;
    private long   trafficMax                = -1;
    /* Timestamp when limits get reset. */
    private long   timestampLimitReset       = -1;
    /* How much traffic is credited when downloading from this host? */
    private short  trafficUsageFactorPercent = 100;

    public MultiHostHost(final String domain) {
        this.domain = domain;
    }

    protected String getDomain() {
        return this.domain;
    }
    // private void setDomain(String domain) {
    // this.domain = domain;
    // }

    protected int getLinksLeft() {
        return linksLeft;
    }

    protected void setLinksLeft(int linksLeft) {
        this.linksLeft = linksLeft;
    }

    protected int getLinksLeftMax() {
        return linksLeftMax;
    }

    protected void setLinksLeftMax(int linksLeftMax) {
        this.linksLeftMax = linksLeftMax;
    }

    protected long getTrafficLeft() {
        return trafficLeft;
    }

    protected void setTrafficLeft(long trafficLeft) {
        this.trafficLeft = trafficLeft;
    }

    protected long getTrafficMax() {
        return trafficMax;
    }

    protected void setTrafficMax(long trafficMax) {
        this.trafficMax = trafficMax;
    }

    protected long getTimestampTrafficReset() {
        return timestampLimitReset;
    }

    protected void setTimestampTrafficReset(long timestampTrafficReset) {
        this.timestampLimitReset = timestampTrafficReset;
    }

    protected short getTrafficUsageFactorPercent() {
        return trafficUsageFactorPercent;
    }

    protected void setTrafficUsageFactorPercent(short trafficUsageFactorPercent) {
        this.trafficUsageFactorPercent = trafficUsageFactorPercent;
    }

    protected boolean canDownload(final DownloadLink link) {
        if (this.linksLeft == -1 && this.trafficLeft == -1) {
            /* No limits -> Allow download */
            return true;
        } else if (this.trafficLeft == -1 && this.linksLeft > 0) {
            /* E.g. multihost only limits max. links per time but not traffic. */
            return true;
        } else if (this.linksLeft > 0 && (this.trafficLeft == -1 || link.getView().getBytesTotal() <= 0 || this.trafficLeft >= link.getView().getBytesTotal())) {
            /* Limits but enough traffic available for this link -> Allow download */
            return true;
        } else {
            /* One of the two limitations reached -> Download impossible */
            return false;
        }
    }
}
