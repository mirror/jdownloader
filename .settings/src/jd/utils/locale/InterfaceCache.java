package jd.utils.locale;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

public class InterfaceCache {
    private static HashMap<File, TInterface> CACHE = new HashMap<File, TInterface>();

    public static TInterface get(File trans) {
        TInterface ret = CACHE.get(trans);
        if (ret == null) {
            ret = new TInterface(trans);
            CACHE.put(trans, ret);
        }
        return ret;
    }

    public static Collection<TInterface> list() {
        return CACHE.values();
    }

}
