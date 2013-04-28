package org.jdownloader.extensions.jdanywhere.api.storable;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;

public class DownloadLinkStorable implements Storable {

    public long getId() {
        if (link == null) return 0;
        return link.getUniqueID().getID();
    }

    public long getUUID() {
        if (link == null) return 0;
        return link.getUniqueID().getID();
    }

    public String getName() {
        if (link == null) return null;
        return link.getName();
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    private DownloadLink     link;
    private QueryResponseMap infoMap = null;

    public DownloadLinkStorable(/* Storable */) {
        this.link = null;
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return this.link.getAvailableStatus().toString();
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getDownloadSize();
    }

    public long getDone() {
        if (link == null) return -1l;
        return link.getDownloadCurrent();
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public long getSpeed() {
        if (link == null) return 0;
        return link.getDownloadSpeed();
    }

    public long getAdded() {
        if (link == null) return -1l;
        return link.getCreated();
    }

    public long getFinished() {
        if (link == null) return -1l;
        return link.getFinishedDate();
    }

    public int getPriority() {
        if (link == null) return 0;
        return link.getPriority();
    }

    public int getChunks() {
        if (link == null) return 0;
        return link.getChunks();
    }

    public LinkStatusJobStorable getLinkStatus() {
        if (link == null) return null;

        LinkStatus ls = link.getLinkStatus();
        LinkStatusJobStorable lsj = new LinkStatusJobStorable();

        lsj.setActive(ls.isPluginActive());
        lsj.setFinished(ls.isFinished());
        lsj.setInProgress(ls.isPluginInProgress());
        lsj.setLinkID(link.getUniqueID().toString());
        lsj.setStatus(ls.getStatus());
        lsj.setStatusText(ls.getMessage(false));
        return lsj;
    }

    public DownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }
}
