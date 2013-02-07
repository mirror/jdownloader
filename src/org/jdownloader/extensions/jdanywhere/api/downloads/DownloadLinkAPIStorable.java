package org.jdownloader.extensions.jdanywhere.api.downloads;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;

public class DownloadLinkAPIStorable implements Storable {

    // mobile
    public long getId() {
        if (link == null) return 0;
        return link.getUniqueID().getID();
    }

    public void setId(long id) {
    }

    public String getComment() {
        if (link == null) return null;
        return link.getComment();
    }

    // end mobile
    public long getUUID() {
        if (link == null) return 0;
        return link.getUniqueID().getID();
    }

    public void setUUId(long id) {
    }

    public String getName() {
        if (link == null) return null;
        return link.getName();
    }

    public void setName(String name) {
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setComment(String comment) {
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    private DownloadLink     link;
    private QueryResponseMap infoMap = null;

    public void setHost(String hoster) {
    }

    public DownloadLinkAPIStorable(/* Storable */) {
        this.link = null;
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return this.link.getAvailableStatus().toString();
    }

    public void setOnlinestatus(String onlinestatus) {
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getDownloadSize();
    }

    public void setSize(long size) {
    }

    public long getDone() {
        if (link == null) return -1l;
        return link.getDownloadCurrent();
    }

    public void setDone(long done) {
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public void setEnabled(boolean enabled) {
    }

    public long getSpeed() {
        if (link == null) return 0;
        return link.getDownloadSpeed();
    }

    public void setSpeed(long speed) {
    }

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

    public int getPriority() {
        if (link == null) return 0;
        return link.getPriority();
    }

    public void setPriority(int priority) {
        link.setPriority(priority);
    }

    public int getChunks() {
        if (link == null) return 0;
        return link.getChunks();
    }

    public void setChunks(int chunks) {
    }

    public String getBrowserurl() {
        if (link == null) return null;
        return link.getBrowserUrl();
    }

    public void setBrowserurl(String browserurl) {
    }

    public LinkStatusJob getLinkStatus() {
        if (link == null) return null;

        LinkStatus ls = link.getLinkStatus();
        LinkStatusJob lsj = new LinkStatusJob();

        lsj.setActive(ls.isPluginActive());
        lsj.setFinished(ls.isFinished());
        lsj.setInProgress(ls.isPluginInProgress());
        lsj.setLinkID(link.getUniqueID().toString());
        lsj.setStatus(ls.getStatus());
        lsj.setStatusText(ls.getMessage(false));
        return lsj;
    }

    public DownloadLinkAPIStorable(DownloadLink link) {
        this.link = link;
    }
}
