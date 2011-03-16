package jd.pluginloader;

import java.util.HashMap;

import org.appwork.storage.JSonStorage;

/**
 * Caches all plugin info in a JSON File.
 * 
 * @see PluginCache
 * @author thomas
 * 
 */
public class HosterPluginCache extends PluginCache<CachedHoster> {
    private static final HosterPluginCache INSTANCE = new HosterPluginCache();

    public static HosterPluginCache getInstance() {
        return HosterPluginCache.INSTANCE;
    }

    private static final String TMP_PLUGINCACHE_JSON = "tmp/HosterPluginCache.json";

    private HosterPluginCache() {

        super(TMP_PLUGINCACHE_JSON);
    }

    @Override
    protected HashMap<String, CachedHoster> restore(String cachedString) {

        return JSonStorage.restoreFromString(cachedString, new org.appwork.storage.TypeRef<HashMap<String, CachedHoster>>() {
        }, new HashMap<String, CachedHoster>());

    }

}
