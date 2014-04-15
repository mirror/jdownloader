package org.jdownloader.api.jdanywhere.api.storable;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;

public class DownloadLinkInfoStorable implements Storable {

    public String getName() {
        if (link == null) return null;
        return link.getView().getDisplayName();
    }

    public String getComment() {
        if (link == null) return null;
        return link.getComment();
    }

    public String getHost() {
        if (link == null) return null;
        return link.getHost();
    }

    public String getBrowserurl() {
        if (link == null) return null;
        return link.getBrowserUrl();
    }

    public String getDirectory() {
        return link.getParentNode().getDownloadDirectory();
    }

    public String getPassword() {
        return link.getDownloadPassword();
    }

    private DownloadLink link;

    @SuppressWarnings("unused")
    private DownloadLinkInfoStorable() {
    }

    public DownloadLinkInfoStorable(DownloadLink link) {
        this.link = link;
    }
}
