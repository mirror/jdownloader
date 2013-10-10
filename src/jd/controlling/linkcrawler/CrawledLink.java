package jd.controlling.linkcrawler;

import java.util.List;

import javax.swing.ImageIcon;

import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;

public class CrawledLink implements AbstractPackageChildrenNode<CrawledPackage>, CheckableLink, AbstractNodeNotifier {

    public static enum LinkState {
        ONLINE,
        OFFLINE,
        UNKNOWN,
        TEMP_UNKNOWN
    }

    private boolean crawlDeep = false;

    public boolean isCrawlDeep() {
        return crawlDeep;
    }

    public void setCrawlDeep(boolean crawlDeep) {
        this.crawlDeep = crawlDeep;
    }

    private CrawledPackage            parent               = null;
    private UnknownCrawledLinkHandler unknownHandler       = null;
    private CrawledLinkModifier       modifyHandler        = null;
    private BrokenCrawlerHandler      brokenCrawlerHandler = null;
    private boolean                   autoConfirmEnabled   = false;

    private transient UniqueAlltimeID uniqueID             = new UniqueAlltimeID();

    public boolean isAutoConfirmEnabled() {
        return autoConfirmEnabled;
    }

    public void setAutoConfirmEnabled(boolean autoAddEnabled) {
        this.autoConfirmEnabled = autoAddEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    private boolean autoStartEnabled = false;

    public UnknownCrawledLinkHandler getUnknownHandler() {
        return unknownHandler;
    }

    public void setUnknownHandler(UnknownCrawledLinkHandler unknownHandler) {
        this.unknownHandler = unknownHandler;
    }

    private LinkCollectingJob         sourceJob          = null;
    private long                      created            = -1;

    boolean                           enabledState       = true;
    private PackageInfo               desiredPackageInfo = null;
    private LinkCollectingInformation collectingInfo     = null;

    public PackageInfo getDesiredPackageInfo() {
        return desiredPackageInfo;
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        this.desiredPackageInfo = desiredPackageInfo;
    }

    /**
     * Linkid should be unique for a certain link. in most cases, this is the url itself, but somtimes (youtube e.g.) the id contains info about how to prozess
     * the file afterwards.
     * 
     * example:<br>
     * 2 youtube links may have the same url, but the one will be converted into mp3, and the other stays flv. url is the same, but linkID different.
     * 
     * @return
     */
    public String getLinkID() {
        String linkID = null;
        if (dlLink != null) linkID = dlLink.getLinkID();
        if (linkID != null) return linkID;
        return getURL();
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

    private DownloadLink  dlLink  = null;

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

    private CryptedLink          cLink          = null;
    private String               url;
    private CrawledLink          sourceLink     = null;
    private String               name           = null;
    private FilterRule           matchingFilter;

    private volatile ArchiveInfo archiveInfo;
    private UniqueAlltimeID      previousParent = null; ;

    public CrawledLink(DownloadLink dlLink) {
        this.dlLink = dlLink;
        passwordForward(dlLink);
    }

    private void passwordForward(DownloadLink dlLink) {
        if (dlLink == null) return;
        List<String> lst = dlLink.getSourcePluginPasswordList();
        if (lst != null && lst.size() > 0) {
            getArchiveInfo().getExtractionPasswords().addAll(lst);
        }
    }

    public void setDownloadLink(DownloadLink dlLink) {
        this.dlLink = dlLink;
        passwordForward(dlLink);
    }

    public CrawledLink(CryptedLink cLink) {
        this.cLink = cLink;
    }

    public CrawledLink(String url) {
        if (url == null) return;
        this.url = new String(url);
    }

    public String getName() {
        String lname = name;
        if (lname != null) {
            CrawledPackage lparent = this.getParentNode();
            String packageName = null;
            if (lparent != null) packageName = lparent.getName();
            return PackagizerController.replaceDynamicTags(lname, packageName);
        }
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

    public void setName(String name) {
        if (name != null && name.equals(this.name)) return;
        if (StringUtils.isEmpty(name)) {
            this.name = null;
        } else {
            this.name = name;
        }
        if (hasNotificationListener()) nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, getName()));
    }

    /* returns unmodified name variable */
    public String _getName() {
        return name;
    }

    public boolean isNameSet() {
        return name != null;
    }

    public String getHost() {
        if (dlLink != null) return dlLink.getHost();
        return null;
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
        sb.append("NAME:");
        sb.append(getName());
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

    public synchronized void setParentNode(CrawledPackage parent) {
        if (this.parent == parent) {
            this.previousParent = null;
            return;
        }
        if (this.parent != null) {
            this.previousParent = this.parent.getUniqueID();
        }
        this.parent = parent;
    }

    public boolean isEnabled() {
        return enabledState;
    }

    public void setArchiveID(String id) {
        if (dlLink != null) {
            dlLink.setArchiveID(id);
        }
    }

    public void setEnabled(boolean b) {
        if (b == enabledState) return;
        enabledState = b;
        if (hasNotificationListener()) nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.ENABLED, b));
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
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
     * If this Link got filtered by {@link CaptchaHandler}, you can get the matching deny rule here.<br>
     * <br>
     * 
     * @return
     */
    public FilterRule getMatchingFilter() {
        return matchingFilter;
    }

    public LinkState getLinkState() {
        if (dlLink != null) {
            switch (dlLink.getAvailableStatus()) {
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

    public Priority getPriority() {
        try {
            if (dlLink == null) return Priority.DEFAULT;
            return dlLink.getPriorityEnum();
        } catch (Throwable e) {
            return Priority.DEFAULT;
        }
    }

    public void setPriority(Priority priority) {
        if (dlLink != null) {
            dlLink.setPriorityEnum(priority);
        }
    }

    /**
     * Returns if this linkc an be handled without manual user captcha input
     * 
     * @return
     */
    public boolean hasAutoCaptcha() {
        if (gethPlugin() != null) { return gethPlugin().hasAutoCaptcha(); }
        return true;
    }

    public boolean hasCaptcha(Account acc) {
        if (gethPlugin() != null) { return gethPlugin().hasCaptcha(dlLink, acc); }
        return false;
    }

    public boolean isDirectHTTP() {
        if (gethPlugin() != null) { return gethPlugin().getClass().getName().equalsIgnoreCase("jd.plugins.hoster.DirectHTTP"); }
        return false;
    }

    public DomainInfo getDomainInfo() {
        if (dlLink != null) { return dlLink.getDomainInfo(); }
        return null;
    }

    public CrawledLinkModifier getCustomCrawledLinkModifier() {
        return modifyHandler;
    }

    public void setCustomCrawledLinkModifier(CrawledLinkModifier modifier) {
        this.modifyHandler = modifier;
    }

    /**
     * @param brokenCrawlerHandler
     *            the brokenCrawlerHandler to set
     */
    public void setBrokenCrawlerHandler(BrokenCrawlerHandler brokenCrawlerHandler) {
        this.brokenCrawlerHandler = brokenCrawlerHandler;
    }

    /**
     * @return the brokenCrawlerHandler
     */
    public BrokenCrawlerHandler getBrokenCrawlerHandler() {
        return brokenCrawlerHandler;
    }

    public UniqueAlltimeID getUniqueID() {
        if (dlLink != null) return dlLink.getUniqueID();
        return uniqueID;
    }

    /**
     * @param collectingInfo
     *            the collectingInfo to set
     */
    public void setCollectingInfo(LinkCollectingInformation collectingInfo) {
        this.collectingInfo = collectingInfo;
    }

    /**
     * @return the collectingInfo
     */
    public LinkCollectingInformation getCollectingInfo() {
        if (collectingInfo != null || sourceLink == null) return collectingInfo;
        return sourceLink.getCollectingInfo();
    }

    public ArchiveInfo getArchiveInfo() {
        if (archiveInfo != null) return archiveInfo;
        synchronized (this) {
            if (archiveInfo != null) return archiveInfo;
            archiveInfo = new ArchiveInfo();

        }
        return archiveInfo;
    }

    public boolean hasArchiveInfo() {
        ArchiveInfo larchiveInfo = archiveInfo;
        if (larchiveInfo != null) {
            if (!BooleanStatus.UNSET.equals(larchiveInfo.getAutoExtract())) return true;
            if (larchiveInfo.getExtractionPasswords() != null && larchiveInfo.getExtractionPasswords().size() > 0) return true;
        }
        return false;
    }

    public void setArchiveInfo(ArchiveInfo archiveInfo) {
        this.archiveInfo = archiveInfo;
    }

    @Override
    public void nodeUpdated(AbstractNode source, NOTIFY notify, Object param) {
        CrawledPackage lparent = parent;
        if (lparent == null) return;
        AbstractNode lsource = source;
        if (lsource != null && lsource instanceof DownloadLink) {
            if (param instanceof DownloadLinkProperty) {
                DownloadLinkProperty propertyEvent = (DownloadLinkProperty) param;
                switch (propertyEvent.getProperty()) {
                case AVAILABILITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.AVAILABILITY, propertyEvent.getValue()));
                    return;
                case ENABLED:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.ENABLED, propertyEvent.getValue()));
                    return;
                case NAME:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, propertyEvent.getValue()));
                    return;
                case PRIORITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.PRIORITY, propertyEvent.getValue()));
                    return;
                }
            }
        }
        if (lsource == null) lsource = this;
        lparent.nodeUpdated(lsource, notify, param);
    }

    @Override
    public boolean hasNotificationListener() {
        CrawledPackage lparent = parent;
        if (lparent != null && lparent.hasNotificationListener()) return true;
        return false;
    }

    @Override
    public UniqueAlltimeID getPreviousParentNodeID() {
        return previousParent;
    }

    public String getArchiveID() {
        if (dlLink != null) { return dlLink.getArchiveID(); }
        return null;
    }

}
