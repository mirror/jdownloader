package org.jdownloader.plugins.scanner;

public class CachedDecrypterInfo extends CachePluginInfo {
    public CachedDecrypterInfo(String name, String pattern, String revision, String hash, String classPath) {
        super(name, pattern, revision, hash, classPath);
    }

    public CachedDecrypterInfo() {
        // implements Storable!!
        super();
    }
}
