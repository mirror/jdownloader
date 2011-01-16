package jd.pluginloader;

/**
 * Dataclass! This class is serialized to a json object to cache class
 * informations
 * 
 * @author thomas
 * 
 */
public class CachedDecrypter extends CachedPlugin {

    @SuppressWarnings("unused")
    private CachedDecrypter() {
        super();
        // do not remove. this empty Constructor is required for JSonSTorage
    }

    public CachedDecrypter(int[] flags, String[] names, String[] patterns, String revision, int interfaceVersion) {
        super(flags, names, patterns, revision, interfaceVersion);
    }

}
