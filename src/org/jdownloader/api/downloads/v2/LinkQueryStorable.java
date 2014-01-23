package org.jdownloader.api.downloads.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.downloadlist.DownloadLinkQuery;

public class LinkQueryStorable extends DownloadLinkQuery implements Storable {
    public LinkQueryStorable() {
        super(/* Storable */);
    }

}