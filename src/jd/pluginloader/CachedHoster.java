package jd.pluginloader;

/**
 * Dataclass! This class is serialized to a json object to cache class
 * informations
 * 
 * @author thomas
 * 
 */
public class CachedHoster extends CachedPlugin {

    @SuppressWarnings("unused")
    private CachedHoster() {
        // do not remove. this empty Constructor is required for JSonSTorage
        super();
    }

    public CachedHoster(int[] flags, String[] names, String[] patterns, String revision, int interfaceVersion) {
        super(flags, names, patterns, revision, interfaceVersion);
    }
}
