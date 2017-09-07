package org.jdownloader.api.toolbar;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;

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

    public AvailableLinkState getStatus() {
        return link.getLinkState();
    }

    public String getLinkCheckID() {
        return null;
    }
}
