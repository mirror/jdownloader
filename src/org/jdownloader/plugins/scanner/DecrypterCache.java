package org.jdownloader.plugins.scanner;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class DecrypterCache extends ArrayList<CachedDecrypterInfo> implements Storable {
    public DecrypterCache() {
        // required for Storable
        super();
    }
}
