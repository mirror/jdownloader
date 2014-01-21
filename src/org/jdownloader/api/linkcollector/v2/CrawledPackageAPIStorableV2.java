package org.jdownloader.api.linkcollector.v2;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.CrawledPackageStorable;

public class CrawledPackageAPIStorableV2 extends CrawledPackageStorable implements Storable {

    public CrawledPackageAPIStorableV2(/* STorable */) {

    }

    public CrawledPackageAPIStorableV2(CrawledPackage pkg) {
        setName(pkg.getName());
        setUuid(pkg.getUniqueID().getID());
    }

}
