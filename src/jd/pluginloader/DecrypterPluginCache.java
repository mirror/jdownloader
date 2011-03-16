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
public class DecrypterPluginCache extends PluginCache<CachedDecrypter> {
    private static final DecrypterPluginCache INSTANCE = new DecrypterPluginCache();

    public static DecrypterPluginCache getInstance() {
        return DecrypterPluginCache.INSTANCE;
    }

    private static final String TMP_PLUGINCACHE_JSON = "tmp/DecrypterPluginCache.json";

    private DecrypterPluginCache() {
        super(TMP_PLUGINCACHE_JSON);

    }

    @Override
    protected HashMap<String, CachedDecrypter> restore(String cachedString) {

        return JSonStorage.restoreFromString(cachedString, new org.appwork.storage.TypeRef<HashMap<String, CachedDecrypter>>() {
        }, new HashMap<String, CachedDecrypter>());

    }

}
