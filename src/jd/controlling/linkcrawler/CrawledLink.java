package jd.controlling.linkcrawler;

import javax.swing.ImageIcon;

import jd.controlling.captcha.CaptchaController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;

import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.FilterRule;

public class CrawledLink implements AbstractPackageChildrenNode<CrawledPackage>, CheckableLink {

    public static enum LinkState {
        ONLINE,
        OFFLINE,
        UNKNOWN,
        TEMP_UNKNOWN
    }

    private CrawledPackage    parent             = null;
    private PluginForDecrypt  dPlugin            = null;
    private LinkCollectingJob sourceJob          = null;
    private long              created            = -1;
    private boolean           isDupeAllow        = false;
    private String            realHost           = null;
    boolean                   enabledState       = true;
    private PackageInfo       desiredPackageInfo = new PackageInfo();

    public PackageInfo getDesiredPackageInfo() {
        return desiredPackageInfo;
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        this.desiredPackageInfo = desiredPackageInfo;
    }

    /**
     * @return the isDupeAllow
     */
    public boolean isDupeAllow() {
        return isDupeAllow;
    }

    /**
     * @param isDupeAllow
     *            the isDupeAllow to set
     */
    public void setDupeAllow(boolean isDupeAllow) {
        this.isDupeAllow = isDupeAllow;
    }

    /**
     * @return the sourceJob
     */
    public LinkCollectingJob getSourceJob() {
        return sourceJob;
    }

    /**
     * @param sourceJob
     *            the sourceJob to set
     */
    public void setSourceJob(LinkCollectingJob sourceJob) {
        this.sourceJob = sourceJob;
    }

    /**
     * @return the dPlugin
     */
    public PluginForDecrypt getdPlugin() {
        return dPlugin;
    }

    /**
     * @param dPlugin
     *            the dPlugin to set
     */
    public void setdPlugin(PluginForDecrypt dPlugin) {
        this.dPlugin = dPlugin;
    }

    public long getSize() {
        if (dlLink != null) return dlLink.getDownloadSize();
        return -1;
    }

    /**
     * @return the hPlugin
     */
    public PluginForHost gethPlugin() {
        if (hPlugin != null) return hPlugin;
        if (dlLink != null) return dlLink.getDefaultPlugin();
        return null;
    }

    /**
     * @param hPlugin
     *            the hPlugin to set
     */
    public void sethPlugin(PluginForHost hPlugin) {
        this.hPlugin = hPlugin;
    }

    private PluginForHost hPlugin = null;
    private PluginsC      cPlugin = null;

    public PluginsC getcPlugin() {
        return cPlugin;
    }

    public void setcPlugin(PluginsC cPlugin) {
        this.cPlugin = cPlugin;
    }

    private DownloadLink dlLink = null;

    /**
     * @return the dlLink
     */
    public DownloadLink getDownloadLink() {
        return dlLink;
    }

    /**
     * @return the cLink
     */
    public CryptedLink getCryptedLink() {
        return cLink;
    }

    private CryptedLink cLink      = null;
    private String      url;
    private CrawledLink sourceLink = null;
    private FilterRule  matchingFilter;

    public CrawledLink(DownloadLink dlLink) {
        this.dlLink = dlLink;
    }

    public CrawledLink(CryptedLink cLink) {
        this.cLink = cLink;
    }

    public CrawledLink(String url) {
        if (url == null) return;
        this.url = new String(url);
    }

    public String getName() {
        if (dlLink != null) return dlLink.getName();
        return "DUMMY";
    }

    public int getChunks() {
        if (dlLink != null) return dlLink.getChunks();
        return -1;
    }

    public void setChunks(int chunks) {
        if (dlLink != null) dlLink.setChunks(chunks);

    }

    public void setForcedName(String name) {
        if (dlLink != null) dlLink.forceFileName(name);
    }

    public ImageIcon getHosterIcon() {
        if (dlLink != null) return dlLink.getHosterIcon();
        return null;
    }

    public String getHost() {
        if (dlLink != null) return dlLink.getHost();
        return null;
    }

    public String getRealHost() {
        if (realHost != null) return realHost;
        if (dlLink != null) {
            /* causes creation of iconHost in DownloadLink */
            dlLink.getHosterIcon();
            realHost = dlLink.getIconHost();
        }
        return realHost;
    }

    public ImageIcon getIcon() {
        if (dlLink != null) return dlLink.getIcon();
        return null;
    }

    public String getURL() {
        if (dlLink != null) return dlLink.getDownloadURL();
        if (cLink != null) return cLink.getCryptedUrl();
        if (url != null) return url;
        return null;
    }

    @Override
    public String toString() {
        CrawledLink parentL = sourceLink;
        StringBuilder sb = new StringBuilder();
        if (parentL != null) {
            sb.append(parentL.toString() + "-->");
        }
        if (url != null) sb.append("URL:" + getURL());
        if (dlLink != null) sb.append("DLLink:" + getURL());
        if (cLink != null) sb.append("CLink:" + getURL());
        return sb.toString();
    }

    public CrawledPackage getParentNode() {
        return parent;
    }

    public void setParentNode(CrawledPackage parent) {
        this.parent = parent;
    }

    public boolean isEnabled() {
        return enabledState;
    }

    public void setEnabled(boolean b) {
        if (b == enabledState) return;
        enabledState = b;
        CrawledPackage lparent = parent;
        if (lparent != null) lparent.notifyPropertyChanges();
    }

    public long getCreated() {
        return created;
    }

    protected void setCreated(long created) {
        this.created = created;
    }

    public long getFinishedDate() {
        return 0;
    }

    public CrawledLink getSourceLink() {
        return sourceLink;
    }

    public CrawledLink getOriginLink() {
        if (sourceLink == null) return this;
        return sourceLink.getOriginLink();
    }

    public void setSourceLink(CrawledLink parent) {
        this.sourceLink = parent;
    }

    public void setMatchingFilter(FilterRule matchedFilter) {
        this.matchingFilter = matchedFilter;
    }

    /**
     * If this Link got filtered by {@link CaptchaController}, you can get the
     * matching deny rule here.<br>
     * <br>
     * 
     * @return
     */
    public FilterRule getMatchingFilter() {
        return matchingFilter;
    }

    public LinkState getLinkState() {
        if (dlLink != null) {
            switch (dlLink.getAvailableStatusInfo()) {
            case FALSE:
                return LinkState.OFFLINE;
            case TRUE:
                return LinkState.ONLINE;
            case UNCHECKABLE:
                return LinkState.TEMP_UNKNOWN;
            case UNCHECKED:
                return LinkState.UNKNOWN;
            default:
                return LinkState.UNKNOWN;
            }
        }
        return LinkState.UNKNOWN;
    }

    public void setChunks() {
    }

    public Priority getPriority() {
        try {
            if (dlLink == null) return Priority.DEFAULT;
            return Priority.values()[dlLink.getPriority() + 1];
        } catch (Throwable e) {
            return Priority.DEFAULT;
        }
    }

    public void setPriority(Priority priority) {
        if (dlLink != null) dlLink.setPriority(priority.ordinal());
    }

}
