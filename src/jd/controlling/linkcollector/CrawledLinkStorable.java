package jd.controlling.linkcollector;

import jd.controlling.downloadcontroller.DownloadLinkStorable;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;

public class CrawledLinkStorable implements Storable {

    private CrawledLink link;

    public String getName() {
        return link._getName();
    }

    public void setName(String name) {
        link.setName(name);
    }

    public boolean isEnabled() {
        return link.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        link.setEnabled(enabled);
    }

    @SuppressWarnings("unused")
    private CrawledLinkStorable(/* Storable */) {
        this.link = new CrawledLink((String) null);
    }

    public CrawledLinkStorable(CrawledLink link) {
        this.link = link;
    }

    public DownloadLinkStorable getDownloadLink() {
        return new DownloadLinkStorable(link.getDownloadLink());
    }

    public void setDownloadLink(DownloadLinkStorable link) {
        this.link.setDownloadLink(link._getDownloadLink());
    }

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        link.setCreated(created);
    }

    /**
     * @return the created
     */
    public long getCreated() {
        return link.getCreated();
    }

    public CrawledLink _getCrawledLink() {
        return link;
    }

}
