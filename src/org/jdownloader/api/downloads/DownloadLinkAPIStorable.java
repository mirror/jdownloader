package org.jdownloader.api.downloads;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;

public class DownloadLinkAPIStorable implements Storable {

    public long getUUID() {
        DownloadLink llink = link;
        if (llink != null) return llink.getUniqueID().getID();
        return 0;
    }

    public void setUUId(long id) {
    }

    public String getName() {
        DownloadLink llink = link;
        if (llink != null) return llink.getView().getDisplayName();
        return null;
    }

    public void setName(String name) {
        DownloadWatchDog.getInstance().renameLink(link, name);
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }

    private DownloadLink                                      link;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public DownloadLinkAPIStorable(/* Storable */) {
        this.link = null;
    }

    public DownloadLinkAPIStorable(DownloadLink link) {
        this.link = link;
    }
}
