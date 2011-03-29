package org.jdownloader.plugins.scanner;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class HosterCache extends ArrayList<CachedHosterInfo> implements Storable {
    public HosterCache() {
        // empty COnsturctor required by Storable
        super();
    }
}
