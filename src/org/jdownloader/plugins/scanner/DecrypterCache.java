package org.jdownloader.plugins.scanner;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class DecrypterCache extends ArrayList<CachedDecrypterInfo> implements Storable {
    private static final long serialVersionUID = 7874035912104967930L;

    public DecrypterCache() {
        // required for Storable
        super();
    }
}
