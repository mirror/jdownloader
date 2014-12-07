package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledPackageQuery;

public class CrawledPackageQueryStorable extends CrawledPackageQuery implements Storable {
    public static final CrawledPackageQueryStorable FULL = new CrawledPackageQueryStorable();
    static {
        FULL.setAvailableOfflineCount(true);
        FULL.setAvailableOnlineCount(true);
        FULL.setAvailableTempUnknownCount(true);
        FULL.setAvailableUnknownCount(true);
        FULL.setBytesTotal(true);
        FULL.setChildCount(true);
        FULL.setComment(true);
        FULL.setEnabled(true);
        FULL.setHosts(true);
        FULL.setSaveTo(true);
        FULL.setStatus(true);
        ;

    }

    public CrawledPackageQueryStorable(/* Storable */) {

    }

}