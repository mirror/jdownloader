package jd.controlling.linkcollector;

import jd.controlling.downloadcontroller.DownloadLinkStorable;
import jd.controlling.linkcrawler.ArchiveInfoStorable;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;

public class CrawledLinkStorable implements Storable {

    private CrawledLink link;
    private String      id  = null;
    private long        UID = -1;

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

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

    public long getUID() {
        DownloadLink dll = link.getDownloadLink();
        if (dll != null) return dll.getUniqueID().getID();
        return link.getUniqueID().getID();
    }

    public void setUID(long id) {
        this.UID = id;
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
        DownloadLink dll = link.getDownloadLink();
        if (dll != null) {
            if (UID != -1) dll.getUniqueID().setID(UID);
        }
        if (UID != -1) link.getUniqueID().setID(UID);
        return link;
    }

    public ArchiveInfoStorable getArchiveInfo() {
        return new ArchiveInfoStorable(link.getArchiveInfo());
    }

    public void setArchiveInfo(ArchiveInfoStorable info) {
        if (info != null) link.setArchiveInfo(info._getArchiveInfo());
    }

}
