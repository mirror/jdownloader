package org.jdownloader.api.downloads.v2;

import jd.plugins.DownloadLink;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadLinkStorable;

public class DownloadLinkAPIStorableV2 extends DownloadLinkStorable implements Storable {

    public DownloadLinkAPIStorableV2(/* Storable */) {

    }

    public DownloadLinkAPIStorableV2(DownloadLink link) {
        setName(link.getView().getDisplayName());
        setUuid(link.getUniqueID().getID());
    }

}
