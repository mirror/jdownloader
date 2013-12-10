package org.jdownloader.api.linkcollector;

import java.util.HashMap;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageAPIStorable implements Storable {

    private CrawledPackage          pkg;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

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

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}
