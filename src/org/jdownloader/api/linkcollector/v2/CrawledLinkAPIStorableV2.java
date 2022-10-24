package org.jdownloader.api.linkcollector.v2;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.Storable;
import org.appwork.storage.StorableDeprecatedSince;
import org.jdownloader.myjdownloader.client.bindings.LinkVariantStorable;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledLinkStorable;

public class CrawledLinkAPIStorableV2 extends CrawledLinkStorable implements Storable {
    public static void main(String[] args) {
        System.out.println(new SimpleMapper().objectToString(new CrawledLinkAPIStorableV2()));
    }

    @Override
    @AllowNonStorableObjects(value = { LinkVariantStorable.class })
    public LinkVariantStorable getVariant() {
        return super.getVariant();
    }

    public CrawledLinkAPIStorableV2(/* Storable */) {
    }

    public CrawledLinkAPIStorableV2(CrawledLink link) {
        setName(link.getName());
        setUuid(link.getUniqueID().getID());
    }

    @Override
    @Deprecated
    @StorableDeprecatedSince("2022-10-18T00:00+0200")
    public boolean isVariants() {
        return super.isVariants();
    }

    /**
     * 03.09.14
     *
     * @deprecated Use {@link #isVariantID()}
     * @return
     */
    @Deprecated
    @Override
    @StorableDeprecatedSince("2022-10-18T00:00+0200")
    public void setVariants(final boolean variants) {
        super.setVariants(variants);
    }
}
