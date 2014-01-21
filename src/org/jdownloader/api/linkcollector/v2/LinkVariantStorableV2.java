package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.LinkVariantStorable;

public class LinkVariantStorableV2 extends LinkVariantStorable implements Storable {
    public LinkVariantStorableV2(/* storable */) {

    }

    public LinkVariantStorableV2(String uniqueId, String name) {
        super();
        setId(uniqueId);
        setName(name);
    }

}
