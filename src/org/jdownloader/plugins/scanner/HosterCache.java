package org.jdownloader.plugins.scanner;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class HosterCache extends ArrayList<CachedHosterInfo> implements Storable {
    private static final long serialVersionUID = -8007699510354917318L;

    public HosterCache() {
        // empty COnsturctor required by Storable
        super();
    }
}
