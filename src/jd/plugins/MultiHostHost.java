package jd.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.storage.Storable;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;

public class MultiHostHost implements Storable {
    /** How long shall we block this host if a limit gets reached? Until the next day/hour? */
    // public enum LimitResetMode {
    // DAILY;
    // }
    /** Why was this host blocked? Because of too many errored out tries via JD or because of the multihost/multihost limits? */
    public enum MultihosterHostStatus {
        WORKING,
        WORKING_UNSTABLE,
        DEACTIVATED_JDOWNLOADER,
        DEACTIVATED_MULTIHOST,
        DEACTIVATED_MULTIHOST_NOT_FOR_THIS_ACCOUNT_TYPE,
        DEACTIVATED_MULTIHOST_LIMIT_REACHED,
        UNSUPPORTED_JDOWNLOADER;
    }

    private String                name                            = null;
    private List<String>          domains                         = new ArrayList<String>();
    private Boolean               isUnlimitedTraffic              = true;
    private Boolean               isUnlimitedLinks                = true;
    private int                   linksLeft                       = -1;
    private int                   linksMax                        = -1;
    private long                  trafficLeft                     = -1;
    private long                  trafficMax                      = -1;
    private long                  unavailableUntilTimestamp       = -1;
    private short                 trafficCalculationFactorPercent = 100;
    private int                   maxChunks                       = 0;
    private Boolean               resume                          = null;
    private String                statusText                      = null;
    private MultihosterHostStatus status                          = null;

    public MultiHostHost(final String domain) {
        this.addDomain(domain);
    }

    public void addDomain(final String domain) {
        if (domain == null) {
            throw new IllegalArgumentException();
        }
        if (!this.domains.contains(domain)) {
            this.domains.add(domain);
        }
    }

    public void addDomains(final ArrayList<String> domains) {
        for (final String domain : domains) {
            this.addDomain(domain);
        }
    }

    public int getLinksLeft() {
        return linksLeft;
    }

    public void setLinksLeft(int num) {
        this.linksLeft = num;
        this.isUnlimitedLinks = false;
    }

    public int getLinksMax() {
        return linksMax;
    }

    public void setLinksMax(int num) {
        this.linksMax = num;
        this.isUnlimitedLinks = false;
    }

    /** Only do this when linksMax is given. */
    public void setLinksUsed(int num) {
        this.linksLeft = this.linksMax - num;
        this.isUnlimitedLinks = false;
    }

    public long getTrafficLeft() {
        return trafficLeft;
    }

    public void setTrafficLeft(long trafficLeft) {
        this.trafficLeft = trafficLeft;
        this.isUnlimitedTraffic = false;
    }

    public long getTrafficMax() {
        return trafficMax;
    }

    public void setTrafficMax(long bytes) {
        this.trafficMax = bytes;
        this.isUnlimitedTraffic = false;
    }

    public void setTrafficUsed(long bytes) {
        this.trafficLeft = this.trafficMax - bytes;
        this.isUnlimitedTraffic = false;
    }

    /**
     * How much traffic is needed and credited when downloading from this host? </br>
     * 500 = 5 times the size of the downloaded file.
     */
    public short getTrafficCalculationFactorPercent() {
        return trafficCalculationFactorPercent;
    }

    public void setTrafficCalculationFactorPercent(short num) {
        this.trafficCalculationFactorPercent = num;
    }

    /** Traffic usage factor e.g. 3 -> 300%. */
    public void setTrafficCalculationFactor(short num) {
        this.trafficCalculationFactorPercent = (short) (100 * num);
    }

    public boolean isUnlimitedLinks() {
        if (this.isUnlimitedLinks == null) {
            return true;
        } else {
            return this.isUnlimitedLinks;
        }
    }

    public boolean isUnlimitedTraffic() {
        if (this.isUnlimitedTraffic == null) {
            return true;
        } else {
            return this.isUnlimitedTraffic;
        }
    }

    public boolean canDownload(final DownloadLink link) {
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
        if (this.unavailableUntilTimestamp > Time.systemIndependentCurrentJVMTimeMillis()) {
            return MultihosterHostStatus.DEACTIVATED_JDOWNLOADER;
        } else if (status == null) {
            return MultihosterHostStatus.WORKING;
        } else {
            return status;
        }
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
        if (resume == null) {
            return true;
        } else {
            return resume;
        }
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public boolean supportsDomain(String domain) {
        if (domain == null) {
            return false;
        }
        domain = domain.toLowerCase(Locale.ENGLISH);
        if (this.domains.contains(domain)) {
            return true;
        } else {
            return false;
        }
    }

    public List<String> getDomains() {
        return this.domains;
    }

    public long getUnavailableUntilTimestamp() {
        return unavailableUntilTimestamp;
    }

    public void setUnavailableTime(long milliseconds) {
        milliseconds = Math.min(milliseconds, 5 * 60 * 1000);
        this.unavailableUntilTimestamp = Time.systemIndependentCurrentJVMTimeMillis() + milliseconds;
    }

    @Override
    public String toString() {
        final String title;
        if (this.domains != null && this.domains.size() > 0) {
            title = this.domains.iterator().next();
        } else {
            title = this.name;
        }
        return title + " | LinksAvailable: " + this.getLinksLeft() + "/" + this.getLinksMax() + " | Traffic: " + SizeFormatter.formatBytes(this.getTrafficLeft()) + "/" + SizeFormatter.formatBytes(this.getTrafficMax()) + " | Chunks: " + this.getMaxChunks() + " | Resume: " + this.isResume();
    }
}
