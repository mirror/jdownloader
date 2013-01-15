package org.jdownloader.api.linkcollector;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageAPIStorable implements Storable {

    private CrawledPackage   pkg;
    private QueryResponseMap infoMap = null;

    public CrawledPackageAPIStorable(/* Storable */) {

    }

    public CrawledPackageAPIStorable(CrawledPackage pkg) {
        this.pkg = pkg;
    }

    public String getName() {
        return pkg.getName();
    }

    public long getUUID() {
        return pkg.getUniqueID().getID();
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }
}
