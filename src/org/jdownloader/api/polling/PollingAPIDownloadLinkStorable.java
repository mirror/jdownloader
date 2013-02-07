package org.jdownloader.api.polling;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;

public class PollingAPIDownloadLinkStorable implements Storable {

    private DownloadLink link;

    public PollingAPIDownloadLinkStorable(/* Storable */) {
        this.link = null;
    }

    public PollingAPIDownloadLinkStorable(DownloadLink link) {
        this.link = link;
    }

    public Long getUUID() {
        return link.getUniqueID().getID();
    }

    public Long getDone() {
        return link.getDownloadCurrent();
    }

    public Long getSize() {
        return link.getDownloadSize();
    }

    public Long getSpeed() {
        return link.getDownloadSpeed();
    }

    public Long getEta() {
        return link.getFinishedDate();
    }
}