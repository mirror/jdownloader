package jd.pluginloader;

import jd.DecryptPluginWrapper;
import jd.plugins.DecrypterPlugin;

import org.appwork.utils.logging.Log;

/**
 * This class represents a DecrypterPLugin. it does not load the class if there
 * are cached plugininformations.
 * 
 * @see DecrypterPluginCache
 * @author thomas
 * 
 */
public class VirtualDecrypterClass {

    public static VirtualDecrypterClass create(VirtualClass c) throws PluginLoaderException {
        VirtualDecrypterClass ret = new VirtualDecrypterClass(c);
        if (c.isLoaded()) {
            // if class already has been loaded, we init the V-class by this
            // real classfile
            initByClass(ret, c.getLoadedClass());
        } else {
            // check if there are cahed plugininfos
            CachedDecrypter cache = DecrypterPluginCache.getInstance().getEntry(c);
            if (cache == null) {
                // load class, and write infos to cache
                try {
                    c.loadClass();

                    initByClass(ret, c.getLoadedClass());
                    cache = new CachedDecrypter(ret.flags, ret.names, ret.patterns, ret.revision, ret.interfaceVersion);
                    writeToCache(c, cache);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                // init cplugin by cache
                initByCache(ret, cache);
            }
        }
        return ret;
    }

    private int interfaceVersion;

    /**
     * writes cache infos to vclass
     * 
     * @param ret
     * @param cache
     */
    private static void initByCache(VirtualDecrypterClass ret, CachedDecrypter cache) {
        ret.flags = cache.getFlags();
        ret.names = cache.getNames();
        ret.patterns = cache.getPatterns();
        ret.revision = cache.getRevision();
        ret.interfaceVersion = cache.getInterfaceVersion();
    }

    private static void writeToCache(VirtualClass c, CachedDecrypter cache) {
        DecrypterPluginCache.getInstance().update(c, cache);
    }

    /**
     * Reads class annotations and write them to this
     * 
     * @param ret
     * @param loadedClass
     * @throws PluginLoaderException
     */
    private static void initByClass(VirtualDecrypterClass ret, Class<?> loadedClass) throws PluginLoaderException {
        DecrypterPlugin an = (DecrypterPlugin) loadedClass.getAnnotation(DecrypterPlugin.class);
        if (an == null) { throw new PluginLoaderException("No Valid Plugin: " + loadedClass); }
        ret.flags = an.flags();
        ret.names = an.names();
        ret.patterns = an.urls();
        ret.revision = an.revision();
        ret.interfaceVersion = an.interfaceVersion();

    }

    private int[]        flags;

    private String[]     names;

    private String[]     patterns;

    private String       revision;

    private VirtualClass vClass;

    private VirtualDecrypterClass(VirtualClass c) {
        this.vClass = c;
    }

    private int getInterfaceVersion() {
        return interfaceVersion;
    }

    /**
     * Creates DecryptPluginWrapper for this plugin. DecryptPluginWrapper
     * finally load the real class if required
     */
    public void initWrapper() {
        for (int i = 0; i < names.length; i++) {
            try {
                new DecryptPluginWrapper(names[i], vClass.getSimpleName(), patterns[i], flags[i], revision);
            } catch (final Throwable e) {
                Log.L.severe("Could not load " + vClass);

            }
        }
    }

    private boolean isOutdated() {

        if (getInterfaceVersion() != DecrypterPlugin.INTERFACE_VERSION) {
            Log.L.warning("Outdated Plugin found: " + vClass);
            return true;
        }
        return false;
    }

    public boolean isValid() {
        if (isOutdated()) return false;
        return true;
    }

}
