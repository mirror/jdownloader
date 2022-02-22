package jd.controlling.linkcrawler;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.config.Property;
import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollectingJob;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.controlling.filter.FilterRule;

public class CrawlingCrawledLink extends Property {
    private final static WeakHashMap<CrawledLink, CrawlingCrawledLink> MAP     = new WeakHashMap<CrawledLink, CrawlingCrawledLink>();
    private final static DelayedRunnable                               CLEANUP = new DelayedRunnable(10 * 1000, 30 * 3000) {
        @Override
        public void delayedrun() {
            synchronized (MAP) {
                if (MAP.size() > 0) {
                    CLEANUP.resetAndStart();
                }
            }
        }
    };

    protected static CrawlingCrawledLink get(final CrawledLink crawledLink, final boolean createIfNotExists) {
        synchronized (MAP) {
            CrawlingCrawledLink ret = MAP.get(crawledLink);
            if (ret == null && createIfNotExists) {
                ret = new CrawlingCrawledLink();
                MAP.put(crawledLink, ret);
                CLEANUP.resetAndStart();
            }
            return ret;
        }
    }

    public UnknownCrawledLinkHandler getUnknownHandler() {
        return (UnknownCrawledLinkHandler) getProperty(UnknownCrawledLinkHandler.class.getName());
    }

    public void setUnknownHandler(UnknownCrawledLinkHandler unknownHandler) {
        setProperty(UnknownCrawledLinkHandler.class.getName(), unknownHandler);
    }

    public CrawledLinkModifier getModifyHandler() {
        return (CrawledLinkModifier) getProperty(CrawledLinkModifier.class.getName());
    }

    public void setModifyHandler(CrawledLinkModifier modifyHandler) {
        setProperty(CrawledLinkModifier.class.getName(), modifyHandler);
    }

    @Override
    public boolean setProperty(String key, Object value) {
        synchronized (MAP) {
            final boolean ret = super.setProperty(key, value);
            if (getPropertiesSize() == 0) {
                final Iterator<Entry<CrawledLink, CrawlingCrawledLink>> it = MAP.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<CrawledLink, CrawlingCrawledLink> next = it.next();
                    if (next.getValue() == this) {
                        it.remove();
                        break;
                    }
                }
            }
            return ret;
        }
    }

    public BrokenCrawlerHandler getBrokenCrawlerHandler() {
        return (BrokenCrawlerHandler) getProperty(BrokenCrawlerHandler.class.getName());
    }

    public void setBrokenCrawlerHandler(BrokenCrawlerHandler brokenCrawlerHandler) {
        setProperty(BrokenCrawlerHandler.class.getName(), brokenCrawlerHandler);
    }

    public PackageInfo getDesiredPackageInfo() {
        return (PackageInfo) getProperty(PackageInfo.class.getName());
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        setProperty(PackageInfo.class.getName(), desiredPackageInfo);
    }

    public LinkCollectingInformation getCollectingInfo() {
        return (LinkCollectingInformation) getProperty(LinkCollectingInformation.class.getName());
    }

    public void setCollectingInfo(LinkCollectingInformation collectingInfo) {
        setProperty(LinkCollectingInformation.class.getName(), collectingInfo);
    }

    public FilterRule getMatchingFilter() {
        return (FilterRule) getProperty(FilterRule.class.getName());
    }

    public void setMatchingFilter(FilterRule matchingFilter) {
        setProperty(FilterRule.class.getName(), matchingFilter);
    }

    public CrawledLink getSourceLink() {
        return (CrawledLink) getProperty(CrawledLink.class.getName());
    }

    public void setSourceLink(CrawledLink sourceLink) {
        setProperty(CrawledLink.class.getName(), sourceLink);
    }

    public LinkCrawlerRule getMatchingRule() {
        return (LinkCrawlerRule) getProperty(LinkCrawlerRule.class.getName());
    }

    public void setMatchingRule(LinkCrawlerRule matchingRule) {
        setProperty(LinkCrawlerRule.class.getName(), matchingRule);
    }

    public static WeakHashMap<CrawledLink, CrawlingCrawledLink> getMap() {
        return MAP;
    }

    public LinkCollectingJob getSourceJob() {
        return (LinkCollectingJob) getProperty(LinkCollectingJob.class.getName());
    }

    public void setSourceJob(LinkCollectingJob sourceJob) {
        setProperty(LinkCollectingJob.class.getName(), sourceJob);
    }
}
