package jd.controlling.linkcollector;

import java.util.HashMap;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;

public class CrawledLinkStorable implements Storable {

    private CrawledLink link;

    private CrawledLinkStorable(/* Storable */) {
    }

    public CrawledLinkStorable(CrawledLink link) {
        this.link = link;
    }

    public String getName() {
        return link.getName();
    }

    public HashMap<String, Object> getProperties() {
        return link.getDownloadLink().getProperties();
    }

    public long getSize() {
        return link.getSize();
    }

    public String getURL() {
        return link.getURL();
    }

    public String getHost() {
        return link.getHost();
    }

}
