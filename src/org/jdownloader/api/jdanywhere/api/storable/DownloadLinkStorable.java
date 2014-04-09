package org.jdownloader.api.jdanywhere.api.storable;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;
import org.jdownloader.api.jdanywhere.JDAnywhereAPI;
import org.jdownloader.api.jdanywhere.api.Helper;
import org.jdownloader.plugins.FinalLinkState;

public class DownloadLinkStorable implements Storable {

    private boolean _messageIsCountdown = false;

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

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    private DownloadLink                                      link;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public DownloadLinkStorable(/* Storable */) {
        this.link = null;
    }

    public String getOnlinestatus() {
        if (link == null) return null;
        return this.link.getAvailableStatus().toString();
    }

    public long getSize() {
        if (link == null) return -1l;
        return link.getView().getBytesTotal();
    }

    public long getDone() {
        if (link == null) return -1l;
        return link.getView().getBytesLoaded();
    }

    public boolean isEnabled() {
        if (link == null) return true;
        return link.isEnabled();
    }

    public long getSpeed() {
        if (link == null) return 0;
        return link.getView().getSpeedBps();
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
        LinkStatusJobStorable lsj = new LinkStatusJobStorable();
        lsj.setFinished(FinalLinkState.CheckFinished(link.getFinalLinkState()));
        lsj.setInProgress(link.getDownloadLinkController() != null);
        lsj.setLinkID(link.getUniqueID().toString());

        if (!JDAnywhereAPI.getInstance().events.CheckForProgressMsg(link)) {
            lsj.setStatusText(Helper.getMessage(link));
        } else {
            lsj.setStatusText("Countdown running");
            _messageIsCountdown = true;
        }
        if (link.isEnabled())
            lsj.setStatus(1);
        else
            lsj.setStatus(0);
        return lsj;
    }

    public DownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }

    public boolean getMessageIsCountdown() {
        return _messageIsCountdown;
    }

}
