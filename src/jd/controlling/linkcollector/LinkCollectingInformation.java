package jd.controlling.linkcollector;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.storage.config.MinTimeWeakReference;

public class LinkCollectingInformation {

    private MinTimeWeakReference<LinkCrawler>              linkCrawler = null;
    private MinTimeWeakReference<LinkChecker<CrawledLink>> linkChecker = null;

    public LinkCollectingInformation(LinkCrawler lc, LinkChecker<CrawledLink> lch) {
        this.setLinkCrawler(lc);
        this.setLinkChecker(lch);
    }

    public LinkCrawler getLinkCrawler() {
        MinTimeWeakReference<LinkCrawler> lCopy = this.linkCrawler;
        if (lCopy == null) return null;
        LinkCrawler ret = lCopy.get();
        if (ret == null) {
            linkCrawler = null;
            return null;
        }
        return ret;
    }

    protected void setLinkCrawler(LinkCrawler linkCrawler) {
        if (linkCrawler == null) {
            this.linkCrawler = null;
            return;
        }
        this.linkCrawler = new MinTimeWeakReference<LinkCrawler>(linkCrawler, 10000, "LinkCrawler:" + this);
    }

    public LinkChecker<CrawledLink> getLinkChecker() {
        MinTimeWeakReference<LinkChecker<CrawledLink>> lCopy = this.linkChecker;
        if (lCopy == null) return null;
        LinkChecker<CrawledLink> ret = lCopy.get();
        if (ret == null) {
            linkChecker = null;
            return null;
        }
        return ret;
    }

    protected void setLinkChecker(LinkChecker<CrawledLink> linkChecker) {
        if (linkChecker == null) {
            this.linkChecker = null;
            return;
        }
        this.linkChecker = new MinTimeWeakReference<LinkChecker<CrawledLink>>(linkChecker, 10000, "LinkChecker:" + this);
    }

}
