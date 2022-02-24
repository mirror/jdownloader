package org.jdownloader.api.linkcollector;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;

import jd.controlling.linkcrawler.CrawledPackage;

public class CrawledPackageAPIStorable implements Storable {
    private CrawledPackage                                    pkg;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public CrawledPackageAPIStorable(/* Storable */) {
    }

    public CrawledPackageAPIStorable(CrawledPackage pkg) {
        this.pkg = pkg;
    }

    @StorableValidatorIgnoresMissingSetter
    public String getName() {
        CrawledPackage lpkg = pkg;
        if (lpkg != null) {
            return lpkg.getName();
        }
        return null;
    }

    @StorableValidatorIgnoresMissingSetter
    public long getUUID() {
        CrawledPackage lpkg = pkg;
        if (lpkg != null) {
            return lpkg.getUniqueID().getID();
        }
        return 0;
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}
