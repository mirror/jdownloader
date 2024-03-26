package jd.controlling.linkcrawler;

import java.util.Iterator;
import java.util.List;

import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcollector.LinknameCleaner;
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
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

public class CrawledLink implements AbstractPackageChildrenNode<CrawledPackage>, CheckableLink, AbstractNodeNotifier, Iterable<CrawledLink> {
    private static enum PROPERTY {
        ENABLED,
        CRAWL_DEEP,
        AUTO_CONFIRM,
        AUTO_START,
        FORCED_AUTO_START
    }

    private volatile byte properties = 1; // default ENABLED=true

    protected synchronized final boolean setProperty(final boolean b, final PROPERTY property) {
        final byte properties = this.properties;
        if (b) {
            this.properties |= 1 << property.ordinal();
        } else {
            this.properties &= ~(1 << property.ordinal());
        }
        return this.properties != properties;
    }

    protected final boolean getProperty(final PROPERTY property) {
        return (properties & 1 << property.ordinal()) != 0;
    }

    public boolean isCrawlDeep() {
        return getProperty(PROPERTY.CRAWL_DEEP);
    }

    public void setCrawlDeep(boolean crawlDeep) {
        setProperty(crawlDeep, PROPERTY.CRAWL_DEEP);
    }

    private volatile CrawledPackage parent = null;
    private LinkOriginDetails       origin;

    public boolean isAutoConfirmEnabled() {
        return getProperty(PROPERTY.AUTO_CONFIRM);
    }

    public void setOrigin(LinkOriginDetails source) {
        this.origin = source;
    }

    public LinkOriginDetails getOrigin() {
        return origin;
    }

    public void setAutoConfirmEnabled(boolean autoAddEnabled) {
        setProperty(autoAddEnabled, PROPERTY.AUTO_CONFIRM);
    }

    public boolean isAutoStartEnabled() {
        return getProperty(PROPERTY.AUTO_START);
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        setProperty(autoStartEnabled, PROPERTY.AUTO_START);
    }

    public boolean isForcedAutoStartEnabled() {
        return getProperty(PROPERTY.FORCED_AUTO_START);
    }

    public void setForcedAutoStartEnabled(boolean forcedAutoStartEnabled) {
        setProperty(forcedAutoStartEnabled, PROPERTY.FORCED_AUTO_START);
    }

    public UnknownCrawledLinkHandler getUnknownHandler() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getUnknownHandler();
        } else {
            return null;
        }
    }

    public void setUnknownHandler(UnknownCrawledLinkHandler unknownHandler) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, unknownHandler != null);
        if (crawling != null) {
            crawling.setUnknownHandler(unknownHandler);
        }
    }

    private volatile long created = -1;

    public PackageInfo getDesiredPackageInfo() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getDesiredPackageInfo();
        } else {
            return null;
        }
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, desiredPackageInfo != null && !desiredPackageInfo.isEmpty());
        if (crawling != null) {
            if (desiredPackageInfo == null || desiredPackageInfo.isEmpty()) {
                crawling.setDesiredPackageInfo(null);
            } else {
                crawling.setDesiredPackageInfo(desiredPackageInfo);
            }
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
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getSourceJob();
        } else {
            return null;
        }
    }

    public long getJobID() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            return link.getJobID();
        } else {
            return -1;
        }
    }

    /**
     * @param sourceJob
     *            the sourceJob to set
     */
    public void setSourceJob(LinkCollectingJob sourceJob) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, sourceJob != null);
        if (crawling != null) {
            crawling.setSourceJob(sourceJob);
        }
    }

    public long getSize() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getView().getBytesTotal();
        } else {
            return -1;
        }
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

    private volatile Object link = null;
    private volatile String name = null;

    public LinkCrawlerRule getMatchingRule() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getMatchingRule();
        } else {
            return null;
        }
    }

    public void setMatchingRule(LinkCrawlerRule matchingRule) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, matchingRule != null);
        if (crawling != null) {
            crawling.setMatchingRule(matchingRule);
        }
    }

    private volatile ArchiveInfo     archiveInfo;
    private volatile UniqueAlltimeID previousParent = null;
    private volatile String[]        sourceUrls;
    private volatile LinkInfo        linkInfo       = null;

    public CrawledLink(DownloadLink dlLink) {
        setDownloadLink(dlLink);
    }

    public void setDownloadLink(DownloadLink dlLink) {
        link = dlLink;
        if (dlLink != null) {
            final List<String> lst = dlLink.getSourcePluginPasswordList();
            if (lst != null && lst.size() > 0) {
                getArchiveInfo().getExtractionPasswords().addAll(lst);
            }
        }
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
                return fixFilename((PackagizerController.replaceDynamicTags(lname, packageName, this)));
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
        } else {
            return "RAWURL:" + url;
        }
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
                name = fixFilename(name);
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

    protected String fixFilename(final String filename) {
        return LinknameCleaner.cleanFilename(filename);
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
        final CrawledLink parentL = getSourceLink();
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
        return getProperty(PROPERTY.ENABLED);
    }

    public void setArchiveID(String id) {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            dlLink.setArchiveID(id);
        }
    }

    public void setEnabled(boolean b) {
        if (setProperty(b, PROPERTY.ENABLED)) {
            if (hasNotificationListener()) {
                nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.ENABLED, b));
            }
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
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            final CrawledLink ret = crawling.getSourceLink();
            if (ret != null) {
                return ret;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public CrawledLink getOriginLink() {
        final CrawledLink lsourceLink = getSourceLink();
        if (lsourceLink == null) {
            return this;
        } else {
            return lsourceLink.getOriginLink();
        }
    }

    public void setSourceLink(CrawledLink parent) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, parent != null);
        if (crawling != null) {
            crawling.setSourceLink(parent);
        }
    }

    public void setMatchingFilter(FilterRule matchedFilter) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, matchedFilter != null);
        if (crawling != null) {
            crawling.setMatchingFilter(matchedFilter);
        }
    }

    /**
     * If this Link got filtered by {@link CaptchaHandler}, you can get the matching deny rule here.<br>
     * <br>
     *
     * @return
     */
    public FilterRule getMatchingFilter() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getMatchingFilter();
        } else {
            return null;
        }
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
        } else {
            return AvailableLinkState.UNKNOWN;
        }
    }

    public Priority getPriority() {
        try {
            final DownloadLink dlLink = getDownloadLink();
            if (dlLink == null) {
                return Priority.DEFAULT;
            } else {
                return dlLink.getPriorityEnum();
            }
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
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getModifyHandler();
        } else {
            return null;
        }
    }

    public void setCustomCrawledLinkModifier(CrawledLinkModifier modifier) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, modifier != null);
        if (crawling != null) {
            crawling.setModifyHandler(modifier);
        }
    }

    /**
     * @param brokenCrawlerHandler
     *            the brokenCrawlerHandler to set
     */
    public void setBrokenCrawlerHandler(BrokenCrawlerHandler brokenCrawlerHandler) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, brokenCrawlerHandler != null);
        if (crawling != null) {
            crawling.setBrokenCrawlerHandler(brokenCrawlerHandler);
        }
    }

    /**
     * @return the brokenCrawlerHandler
     */
    public BrokenCrawlerHandler getBrokenCrawlerHandler() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getBrokenCrawlerHandler();
        } else {
            return null;
        }
    }

    public boolean hasVariantSupport() {
        final DownloadLink dlLink = getDownloadLink();
        return dlLink != null && dlLink.hasVariantSupport();
    }

    public UniqueAlltimeID getUniqueID() {
        final DownloadLink dlLink = getDownloadLink();
        if (dlLink != null) {
            return dlLink.getUniqueID();
        } else {
            return null;
        }
    }

    /**
     * @param collectingInfo
     *            the collectingInfo to set
     */
    public void setCollectingInfo(LinkCollectingInformation collectingInfo) {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, collectingInfo != null);
        if (crawling != null) {
            crawling.setCollectingInfo(collectingInfo);
        }
    }

    public boolean hasCollectingInfo() {
        return getCollectingInfo() != null;
    }

    private LinkCollectingInformation _getCollectingInfo() {
        final CrawlingCrawledLink crawling = CrawlingCrawledLink.get(this, false);
        if (crawling != null) {
            return crawling.getCollectingInfo();
        } else {
            return null;
        }
    }

    /**
     * @return the collectingInfo
     */
    public LinkCollectingInformation getCollectingInfo() {
        final LinkCollectingInformation lcollectingInfo = _getCollectingInfo();
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
        } else {
            synchronized (this) {
                if (archiveInfo != null) {
                    return archiveInfo;
                } else {
                    archiveInfo = new ArchiveInfo();
                    return archiveInfo;
                }
            }
        }
    }

    public boolean hasArchiveInfo() {
        final ArchiveInfo larchiveInfo = archiveInfo;
        if (larchiveInfo != null) {
            if (!BooleanStatus.UNSET.equals(larchiveInfo.getAutoExtract())) {
                return true;
            } else if (larchiveInfo.getExtractionPasswords() != null && larchiveInfo.getExtractionPasswords().size() > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
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
        if (lparent == null || !lparent.hasNotificationListener()) {
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
                    }
                    return;
                case PRIORITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.PRIORITY, propertyEvent.getValue()));
                    return;
                case COMMENT:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, getName()));
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
