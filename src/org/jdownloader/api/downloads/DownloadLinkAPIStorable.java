package org.jdownloader.api.downloads;

import java.util.HashMap;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;

public class DownloadLinkAPIStorable implements Storable {

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

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }

    private DownloadLink            link;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public DownloadLinkAPIStorable(/* Storable */) {
        this.link = null;
    }

    public DownloadLinkAPIStorable(DownloadLink link) {
        this.link = link;
    }
}
