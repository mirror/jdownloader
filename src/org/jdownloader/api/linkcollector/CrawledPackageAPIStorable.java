package org.jdownloader.api.linkcollector;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageAPIStorable implements Storable {

    private CrawledPackage                                    pkg;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public CrawledPackageAPIStorable(/* Storable */) {

    }

    public CrawledPackageAPIStorable(CrawledPackage pkg) {
        this.pkg = pkg;
    }

    public String getName() {
        CrawledPackage lpkg = pkg;
        if (lpkg != null) return lpkg.getName();
        return null;
    }

    public long getUUID() {
        CrawledPackage lpkg = pkg;
        if (lpkg != null) return lpkg.getUniqueID().getID();
        return 0;
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}
