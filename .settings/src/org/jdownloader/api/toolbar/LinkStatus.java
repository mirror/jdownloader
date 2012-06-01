package org.jdownloader.api.toolbar;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;

import org.appwork.storage.Storable;

public class LinkStatus implements Storable {

    private CrawledLink link;

    @SuppressWarnings("unused")
    private LinkStatus(/* Storable */) {
        this.link = new CrawledLink("");
    }

    public LinkStatus(CrawledLink link) {
        this.link = link;
    }

    public String getName() {
        return link._getName();
    }

    public String getHost() {
        return link.getDomainInfo().getTld();
    }

    public long getSize() {
        return link.getSize();
    }

    public String getURL() {
        return link.getURL();
    }

    public LinkState getStatus() {
        return link.getLinkState();
    }

    public String getLinkCheckID() {
        CrawledLink parent = link.getSourceLink();
        if (parent != null && parent instanceof LinkCheckLink) { return ((LinkCheckLink) parent).getLinkCheckID(); }
        return null;
    }

}
