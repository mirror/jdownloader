package jd.plugins;

import java.util.ArrayList;

import org.appwork.utils.formatter.SizeFormatter;

public class MultiHostHost {
    /** How long shall we block this host if a limit gets reached? Until the next day/hour? */
    public enum LimitResetMode {
        DAILY;
    }

    /** Why was this host blocked? Because of too many errored out tries via JD or because of the multihost/multihost limits? */
    public enum MultihosterHostStatus {
        WORKING,
        WORKING_UNSTABLE,
        DEACTIVATED_JDOWNLOADER,
        DEACTIVATED_MULTIHOST,
        DEACTIVATED_MULTIHOST_NOT_FOR_THIS_ACCOUNT_TYPE,
        UNSUPPORTED_JDOWNLOADER,
        OFFLINE;
    }

    private String                name                      = null;
    private String                domain                    = null;
    private ArrayList<String>     domains                   = new ArrayList<String>();
    private boolean               isUnlimitedTraffic        = true;
    private boolean               isUnlimitedLinks          = true;
    private int                   linksLeft                 = -1;
    private int                   linksMax                  = -1;
    private long                  trafficLeft               = -1;
    private long                  trafficMax                = -1;
    /* Timestamp when limits get reset. */
    private long                  timestampLimitReset       = -1;
    /* How much traffic is credited when downloading from this host? */
    private short                 trafficUsageFactorPercent = 100;
    private int                   maxChunks                 = 0;
    private boolean               resume                    = true;
    private String                statusText                = null;
    private MultihosterHostStatus status                    = MultihosterHostStatus.WORKING;

    public MultiHostHost(final String domain) {
        this.domain = domain;
        this.addDomain(domain);
    }

    protected String getDomain() {
        return this.domain;
    }

    private void addDomain(String domain) {
        this.domain = domain;
        if (!this.domains.contains(domain)) {
            this.domains.add(domain);
        }
    }

    protected int getLinksLeft() {
        return linksLeft;
    }

    protected void setLinksLeft(int num) {
        this.linksLeft = num;
    }

    protected int getLinksMax() {
        return linksMax;
    }

    protected void setLinksMax(int num) {
        this.linksMax = num;
    }

    /** Only do this when linksMax is given. */
    protected void setLinksUsed(int num) {
        this.linksLeft = this.linksMax - num;
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

    protected void setTrafficMax(long bytes) {
        this.trafficMax = bytes;
    }

    protected void setTrafficUsed(long bytes) {
        this.trafficLeft = this.trafficMax - bytes;
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

    /** Traffic usage factor e.g. 3 -> 300%. */
    protected void setTrafficUsageFactor(short num) {
        this.trafficUsageFactorPercent = (short) (100 * num);
    }

    protected boolean canDownload(final DownloadLink link) {
        if (isUnlimitedTraffic || isUnlimitedLinks) {
            return true;
        } else if (this.linksLeft <= 0) {
            return false;
        } else if (this.trafficLeft <= 0) {
            return false;
        } else if (link.getView().getBytesTotal() != -1 && this.trafficLeft < link.getView().getBytesTotal()) {
            /* Not enough traffic to download this link */
            return false;
        } else {
            return true;
        }
    }

    public String getStatusText() {
        if (this.statusText != null) {
            return statusText;
        } else if (status != null) {
            return status.name();
        } else {
            return null;
        }
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public MultihosterHostStatus getStatus() {
        return status;
    }

    public void setStatus(MultihosterHostStatus status) {
        this.status = status;
    }

    public int getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(int maxChunks) {
        this.maxChunks = maxChunks;
    }

    public boolean isResume() {
        return resume;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public String toString() {
        return this.getDomain() + " | LinksAvailable: " + this.getLinksLeft() + "/" + this.getLinksMax() + " | Traffic: " + SizeFormatter.formatBytes(this.getTrafficLeft()) + "/" + SizeFormatter.formatBytes(this.getTrafficMax()) + " | Chunks: " + this.getMaxChunks() + " | Resume: " + this.isResume();
    }
}
