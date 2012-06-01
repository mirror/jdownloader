package org.jdownloader.api.toolbar;

import jd.controlling.linkcrawler.CrawledLink;

public class LinkCheckLink extends CrawledLink {

    private String linkCheckID = null;

    public LinkCheckLink(String url, String linkCheckID) {
        super(url);
        this.linkCheckID = linkCheckID;
    }

    public String getLinkCheckID() {
        return linkCheckID;
    }

}
