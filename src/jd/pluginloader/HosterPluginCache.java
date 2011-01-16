package jd.pluginloader;

import java.io.IOException;
import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.TypeReference;

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
        try {
            return JSonStorage.restoreFromString(cachedString, new TypeReference<HashMap<String, CachedHoster>>() {
            }, new HashMap<String, CachedHoster>());
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<String, CachedHoster>();

    }

}
