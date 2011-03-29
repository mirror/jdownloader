package org.jdownloader.plugins.scanner;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class AddonCache extends ArrayList<CachedAddon> implements Storable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AddonCache() {
        // required by Storable
        super();
    }
}
