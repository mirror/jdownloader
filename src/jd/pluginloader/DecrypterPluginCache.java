package jd.pluginloader;

import java.io.IOException;
import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

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
        try {
            return JSonStorage.restoreFromString(cachedString, new TypeRef<HashMap<String, CachedDecrypter>>() {
            }, new HashMap<String, CachedDecrypter>());
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<String, CachedDecrypter>();

    }

}
