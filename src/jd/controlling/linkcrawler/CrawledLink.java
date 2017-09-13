package jd.controlling.linkcrawler;

import java.util.Iterator;
import java.util.List;

import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.LinkInfo;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

public class CrawledLink implements AbstractPackageChildrenNode<CrawledPackage>, CheckableLink, AbstractNodeNotifier, Iterable<CrawledLink> {
    private volatile boolean crawlDeep = false;

    public boolean isCrawlDeep() {
        return crawlDeep;
    }

    public void setCrawlDeep(boolean crawlDeep) {
        this.crawlDeep = crawlDeep;
    }

    private volatile CrawledPackage            parent               = null;
    private volatile UnknownCrawledLinkHandler unknownHandler       = null;
    private volatile CrawledLinkModifier       modifyHandler        = null;
    private volatile BrokenCrawlerHandler      brokenCrawlerHandler = null;
    private volatile boolean                   autoConfirmEnabled   = false;
    private volatile UniqueAlltimeID           uniqueID             = null;
    private LinkOriginDetails                  origin;

    public boolean isAutoConfirmEnabled() {
        return autoConfirmEnabled;
    }

    public void setOrigin(LinkOriginDetails source) {
        this.origin = source;
    }

    public LinkOriginDetails getOrigin() {
        return origin;
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

    private boolean forcedAutoStartEnabled = false;

    public boolean isForcedAutoStartEnabled() {
        return forcedAutoStartEnabled;
    }

    public void setForcedAutoStartEnabled(boolean forcedAutoStartEnabled) {
        this.forcedAutoStartEnabled = forcedAutoStartEnabled;
    }

    private boolean autoStartEnabled = false;

    public UnknownCrawledLinkHandler getUnknownHandler() {
        return unknownHandler;
    }

    public void setUnknownHandler(UnknownCrawledLinkHandler unknownHandler) {
        this.unknownHandler = unknownHandler;
    }

    private volatile LinkCollectingJob         sourceJob          = null;
    private volatile long                      created            = -1;
    private boolean                            enabledState       = true;
    private volatile PackageInfo               desiredPackageInfo = null;
    private volatile LinkCollectingInformation collectingInfo     = null;

    public PackageInfo getDesiredPackageInfo() {
        return desiredPackageInfo;
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        if (desiredPackageInfo == null || desiredPackageInfo.isEmpty()) {
            this.desiredPackageInfo = null;
        } else {
            this.desiredPackageInfo = desiredPackageInfo;
        }
    }

    /**
     * Linkid should be unique for a certain link. in most cases, this is the url itself, but somtimes (youtube e.g.) the id contains info
     * about how to prozess the file afterwards.
     *
     * example:<br>
     * 2 youtube links may have the same url, but the one will be converted into mp3, and the other stays flv. url is the same, but linkID
     * different.
     *
     * @return
     */
    public String getLinkID() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            final String linkID = dlLink.getLinkID();
            if (linkID != null) {
                return linkID;
            }
        }
        return getURL();
    }

    /**
     * @return the sourceJob
     */
    public LinkCollectingJob getSourceJob() {
        return sourceJob;
    }

    public long getJobID() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getJobID();
        }
        return -1;
    }

    /**
     * @param sourceJob
     *            the sourceJob to set
     */
    public void setSourceJob(LinkCollectingJob sourceJob) {
        this.sourceJob = sourceJob;
    }

    public long getSize() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getView().getBytesTotal();
        }
        return -1;
    }

    /**
     * @return the hPlugin
     */
    public PluginForHost gethPlugin() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getDefaultPlugin();
        } else {
            return null;
        }
    }

    /**
     * @return the dlLink
     */
    public DownloadLink getDownloadLink() {
        final Object llink = link;
        if (llink instanceof DownloadLink) {
            return (DownloadLink) llink;
        } else {
            return null;
        }
    }

    public String getUrlLink() {
        final Object llink = link;
        if (llink instanceof CharSequence) {
            return llink.toString();
        } else {
            return null;
        }
    }

    /**
     * @return the cLink
     */
    public CryptedLink getCryptedLink() {
        final Object llink = link;
        if (llink instanceof CryptedLink) {
            return (CryptedLink) llink;
        } else {
            return null;
        }
    }

    private volatile Object          link       = null;
    private volatile CrawledLink     sourceLink = null;
    private volatile String          name       = null;
    private volatile FilterRule      matchingFilter;
    private volatile LinkCrawlerRule matchingRule;

    public LinkCrawlerRule getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(LinkCrawlerRule matchingRule) {
        this.matchingRule = matchingRule;
    }

    private volatile ArchiveInfo     archiveInfo;
    private volatile UniqueAlltimeID previousParent = null;
    private volatile String[]        sourceUrls;
    private volatile LinkInfo        linkInfo       = null;

    public CrawledLink(DownloadLink dlLink) {
        link = dlLink;
        passwordForward(dlLink);
    }

    protected void passwordForward(DownloadLink dlLink) {
        if (dlLink == null) {
            return;
        }
        final List<String> lst = dlLink.getSourcePluginPasswordList();
        if (lst != null && lst.size() > 0) {
            getArchiveInfo().getExtractionPasswords().addAll(lst);
        }
    }

    public void setDownloadLink(DownloadLink dlLink) {
        link = dlLink;
        passwordForward(dlLink);
    }

    public CrawledLink(CryptedLink cLink) {
        link = cLink;
    }

    public CrawledLink(CharSequence url) {
        link = url;
    }

    public String getName() {
        final String lname = name;
        if (lname != null) {
            if (lname.contains("<jd:")) {
                final CrawledPackage lparent = this.getParentNode();
                String packageName = null;
                if (lparent != null) {
                    packageName = lparent.getName();
                }
                return CrossSystem.alleviatePathParts(PackagizerController.replaceDynamicTags(lname, packageName, this));
            } else {
                return lname;
            }
        }
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getView().getDisplayName();
        }
        final String url = getURL();
        final String name = Plugin.extractFileNameFromURL(url);
        if (name != null) {
            return name;
        }
        return "RAWURL:" + url;
    }

    public int getChunks() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getChunks();
        } else {
            return -1;
        }
    }

    public void setChunks(int chunks) {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            dlLink.setChunks(chunks);
        }
    }

    public void setName(String name) {
        if (StringUtils.equals(name, this.name)) {
            return;
        }
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            if (StringUtils.equals(name, link.getName())) {
                name = null;
            }
            if (StringUtils.equals(name, this.name)) {
                return;
            }
        }
        if (name != null) {
            if (!name.contains("<jd:")) {
                name = CrossSystem.alleviatePathParts(name);
            }
            if (StringUtils.equals(name, this.name)) {
                return;
            }
        }
        if (StringUtils.isEmpty(name)) {
            this.name = null;
        } else {
            this.name = name;
        }
        setLinkInfo(null);
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, getName()));
        }
    }

    /* returns unmodified name variable */
    public String _getName() {
        return name;
    }

    public boolean isNameSet() {
        return name != null;
    }

    public String getHost() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getHost();
        } else {
            return null;
        }
    }

    public String getURL() {
        final Object llink = link;
        if (llink != null) {
            if (llink instanceof DownloadLink) {
                return ((DownloadLink) llink).getPluginPatternMatcher();
            } else if (llink instanceof CryptedLink) {
                return ((CryptedLink) llink).getCryptedUrl();
            } else {
                return llink.toString();
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        final CrawledLink parentL = sourceLink;
        final StringBuilder sb = new StringBuilder();
        if (isNameSet()) {
            sb.append("NAME:");
            sb.append(getName());
        }
        final Object llink = link;
        if (llink != null) {
            if (llink instanceof DownloadLink) {
                final DownloadLink downloadLink = (DownloadLink) llink;
                final PluginForHost plugin = downloadLink.getDefaultPlugin();
                if (plugin != null) {
                    sb.append("DLink(" + plugin.getLazyP().getDisplayName() + "):" + downloadLink.getPluginPatternMatcher());
                } else {
                    sb.append("DLink:" + downloadLink.getPluginPatternMatcher());
                }
            } else if (llink instanceof CryptedLink) {
                final CryptedLink cryptedLink = (CryptedLink) llink;
                final LazyCrawlerPlugin plugin = cryptedLink.getLazyC();
                if (plugin != null) {
                    sb.append("CLink(" + plugin.getDisplayName() + "):" + cryptedLink.getCryptedUrl());
                } else {
                    sb.append("CLink:" + cryptedLink.getCryptedUrl());
                }
            } else {
                sb.append("URL:" + llink.toString());
            }
        }
        if (parentL != null) {
            sb.append("<--");
            sb.append(parentL.toString());
        }
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
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            dlLink.setArchiveID(id);
        }
    }

    public void setEnabled(boolean b) {
        if (b == enabledState) {
            return;
        }
        enabledState = b;
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.ENABLED, b));
        }
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
        final CrawledLink lsourceLink = getSourceLink();
        if (lsourceLink == null) {
            return this;
        }
        return lsourceLink.getOriginLink();
    }

    public void setSourceLink(CrawledLink parent) {
        sourceLink = parent;
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

    public AvailableLinkState getLinkState() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            switch (dlLink.getAvailableStatus()) {
            case FALSE:
                return AvailableLinkState.OFFLINE;
            case TRUE:
                return AvailableLinkState.ONLINE;
            case UNCHECKABLE:
                return AvailableLinkState.TEMP_UNKNOWN;
            case UNCHECKED:
                return AvailableLinkState.UNKNOWN;
            default:
                return AvailableLinkState.UNKNOWN;
            }
        }
        return AvailableLinkState.UNKNOWN;
    }

    public Priority getPriority() {
        try {
            final DownloadLink dlLink = getDownloadLink();
            if (dlLink == null) {
                return Priority.DEFAULT;
            }
            return dlLink.getPriorityEnum();
        } catch (Throwable e) {
            return Priority.DEFAULT;
        }
    }

    public void setPriority(Priority priority) {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            dlLink.setPriorityEnum(priority);
        }
    }

    /**
     * Returns if this link an be handled without manual user captcha input
     *
     * @return
     * @since JD2
     */
    public boolean hasAutoCaptcha() {
        final PluginForHost plugin = gethPlugin();
        return plugin != null && plugin.hasAutoCaptcha();
    }

    public boolean hasCaptcha(Account acc) {
        final PluginForHost plugin = gethPlugin();
        final DownloadLink dlLink = getDownloadLink();
        return plugin != null && dlLink != null && Boolean.TRUE.equals(plugin.expectCaptcha(dlLink, acc));
    }

    public boolean isDirectHTTP() {
        final PluginForHost plugin = gethPlugin();
        return plugin != null && plugin.getClass().getName().endsWith("r.DirectHTTP");
    }

    public boolean isFTP() {
        final PluginForHost plugin = gethPlugin();
        return plugin != null && plugin.getClass().getName().endsWith("r.Ftp");
    }

    public DomainInfo getDomainInfo() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getDomainInfo();
        } else {
            return null;
        }
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

    public boolean hasVariantSupport() {
        final DownloadLink dlLink = getDownloadLink();
        return dlLink != null && dlLink.hasVariantSupport();
    }

    public UniqueAlltimeID getUniqueID() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getUniqueID();
        }
        if (uniqueID != null) {
            return uniqueID;
        }
        synchronized (this) {
            if (uniqueID != null) {
                return uniqueID;
            }
            uniqueID = new UniqueAlltimeID();
        }
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
        final LinkCollectingInformation lcollectingInfo = collectingInfo;
        final CrawledLink lsourceLink = getSourceLink();
        if (lcollectingInfo != null || lsourceLink == null) {
            return lcollectingInfo;
        } else {
            return lsourceLink.getCollectingInfo();
        }
    }

    public ArchiveInfo getArchiveInfo() {
        if (archiveInfo != null) {
            return archiveInfo;
        }
        synchronized (this) {
            if (archiveInfo != null) {
                return archiveInfo;
            }
            archiveInfo = new ArchiveInfo();
        }
        return archiveInfo;
    }

    public boolean hasArchiveInfo() {
        final ArchiveInfo larchiveInfo = archiveInfo;
        if (larchiveInfo != null) {
            if (!BooleanStatus.UNSET.equals(larchiveInfo.getAutoExtract())) {
                return true;
            }
            if (larchiveInfo.getExtractionPasswords() != null && larchiveInfo.getExtractionPasswords().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public void setArchiveInfo(ArchiveInfo archiveInfo) {
        this.archiveInfo = archiveInfo;
    }

    public LinkInfo getLinkInfo() {
        final DownloadLink dlLink = getDownloadLink();
        if (!isNameSet() && dlLink != null) {
            return dlLink.getLinkInfo();
        }
        final LinkInfo linkInfo = this.linkInfo;
        if (linkInfo == null) {
            final LinkInfo newLinkInfo = LinkInfo.getLinkInfo(this);
            this.linkInfo = newLinkInfo;
            return newLinkInfo;
        }
        return linkInfo;
    }

    private void setLinkInfo(LinkInfo linkInfo) {
        this.linkInfo = linkInfo;
    }

    @Override
    public void nodeUpdated(AbstractNode source, NOTIFY notify, Object param) {
        final CrawledPackage lparent = parent;
        if (lparent == null) {
            return;
        }
        AbstractNode lsource = source;
        if (lsource != null && lsource instanceof DownloadLink) {
            if (param instanceof DownloadLinkProperty) {
                DownloadLinkProperty propertyEvent = (DownloadLinkProperty) param;
                switch (propertyEvent.getProperty()) {
                case AVAILABILITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.AVAILABILITY, propertyEvent.getValue()));
                    return;
                case ENABLED:
                    /* not needed to forward at the moment */
                    // nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this,
                    // CrawledLinkProperty.Property.ENABLED,
                    // propertyEvent.getValue()));
                    return;
                case NAME:
                    if (!isNameSet()) {
                        /* we use the name from downloadLink */
                        setLinkInfo(null);
                        nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, propertyEvent.getValue()));
                        return;
                    }
                case PRIORITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.PRIORITY, propertyEvent.getValue()));
                    return;
                }
            }
        }
        if (lsource == null) {
            lsource = this;
        }
        lparent.nodeUpdated(lsource, notify, param);
    }

    @Override
    public boolean hasNotificationListener() {
        final CrawledPackage lparent = parent;
        return lparent != null && lparent.hasNotificationListener();
    }

    @Override
    public UniqueAlltimeID getPreviousParentNodeID() {
        return previousParent;
    }

    public String getArchiveID() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getArchiveID();
        } else {
            return null;
        }
    }

    public void firePropertyChanged(CrawledLinkProperty.Property property, Object value) {
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, property, value));
        }
    }

    public void setSourceUrls(String[] sourceUrls) {
        this.sourceUrls = sourceUrls;
    }

    public String[] getSourceUrls() {
        return sourceUrls;
    }

    public void setComment(String comment) {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            link.setComment(comment);
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, getName()));
            }
        }
    }

    public String getComment() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getComment();
        } else {
            return null;
        }
    }

    @Override
    public Iterator<CrawledLink> iterator() {
        return new Iterator<CrawledLink>() {
            private CrawledLink current = CrawledLink.this;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public CrawledLink next() {
                final CrawledLink ret = current;
                if (current != null) {
                    current = current.getSourceLink();
                }
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
