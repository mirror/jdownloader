package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableDeprecatedSince;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledLinkQuery;

public class CrawledLinkQueryStorable extends CrawledLinkQuery implements Storable {

    public static final CrawledLinkQueryStorable FULL = new CrawledLinkQueryStorable();
    static {
        FULL.setAdvancedStatus(true);
        FULL.setAvailability(true);
        FULL.setBytesTotal(true);
        FULL.setComment(true);
        FULL.setEnabled(true);
        FULL.setHost(true);
        FULL.setPriority(true);
        FULL.setStatus(true);
        FULL.setUrl(true);
        FULL.setVariantIcon(true);
        FULL.setVariantID(true);
        FULL.setVariantName(true);
        FULL.setAddedDate(true);
    }

    public CrawledLinkQueryStorable() {
        super(/* Storable */);
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