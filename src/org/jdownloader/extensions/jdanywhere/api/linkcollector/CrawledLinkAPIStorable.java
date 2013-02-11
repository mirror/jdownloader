package org.jdownloader.extensions.jdanywhere.api.linkcollector;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.Storable;

public class CrawledLinkAPIStorable implements Storable {

    public long getId() {
        if (link == null) return 0;
        return link.getDownloadLink().getUniqueID().getID();
    }

    public void setId(long id) {
    }

    public String getName() {
        if (link == null) return null;
        return link.getName();
    }

    public void setName(String name) {
    }

    // public String getComment() {
    // if (link == null) return null;
    // return link.get();
    // }

    public void setComment(String comment) {
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    public void setHost(String hoster) {
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return "";
    }

    public void setOnlinestatus(String onlinestatus) {
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getDownloadLink().getDownloadSize();
    }

    public void setSize(long size) {
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public void setEnabled(boolean enabled) {
    }

    // public long getSpeed() {
    // if (link == null) return 0;
    // return link.getDownloadSpeed();
    // }

    public long getAdded() {
        if (link == null) return -1l;
        return link.getCreated();
    }

    public void setAdded(long added) {
    }

    public long getFinished() {
        if (link == null) return -1l;
        return link.getFinishedDate();
    }

    public void setFinished(long finished) {
    }

    // public int getPriority() {
    // if (link == null) return 0;
    // return link.getPriority();
    // }
    //
    // public void setPriority(int priority) {
    // link.setPriority(priority);
    // }

    public int getChunks() {
        if (link == null) return 0;
        return link.getChunks();
    }

    public void setChunks(int chunks) {
    }

    // public String getBrowserurl() {
    // if (link == null) return null;
    // return link.getBrowserUrl();
    // }

    public void setBrowserurl(String browserurl) {
    }

    // public LinkStatusJob getLinkStatus() {
    // if (link == null) return null;
    //
    // LinkStatus ls = link.getLinkStatus();
    // LinkStatusJob lsj = new LinkStatusJob();
    //
    // lsj.setActive(ls.isPluginActive());
    // lsj.setFinished(ls.isFinished());
    // lsj.setInProgress(ls.isPluginInProgress());
    // lsj.setLinkID(link.getUniqueID().toString());
    // lsj.setStatus(ls.getStatus());
    // lsj.setStatusText(ls.getMessage(false));
    // return lsj;
    // }

    private CrawledLink link;

    @SuppressWarnings("unused")
    private CrawledLinkAPIStorable() {
        this.link = null;
    }

    public CrawledLinkAPIStorable(CrawledLink link) {
        this.link = link;
    }
}
