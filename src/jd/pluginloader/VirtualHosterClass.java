package jd.pluginloader;

import jd.HostPluginWrapper;
import jd.plugins.HostPlugin;

import org.appwork.utils.logging.Log;

/**
 * This Class is a representation of the real classfile. it uses cached
 * annotations to avoid classloading at starttime
 * 
 * @author thomas
 * 
 */
public class VirtualHosterClass {

    public static VirtualHosterClass create(VirtualClass c) throws PluginLoaderException {
        VirtualHosterClass ret = new VirtualHosterClass(c);

        if (c.isLoaded()) {
            // class is already loaded. we init by real class file. not cached!
            initByClass(ret, c.getLoadedClass());
        } else {
            CachedHoster cache = HosterPluginCache.getInstance().getEntry(c);
            if (cache == null) {
                // no cache available
                try {
                    // load class file
                    c.loadClass();
                    // init by classfle
                    initByClass(ret, c.getLoadedClass());
                    // write infos to cache
                    cache = new CachedHoster(ret.flags, ret.names, ret.patterns, ret.revision, ret.interfaceVersion);
                    writeToCache(c, cache);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                // init by cache
                initByCache(ret, cache);
            }
        }
        return ret;
    }

    /**
     * Writes cached class infos to ret
     * 
     * @param ret
     * @param cache
     */
    private static void initByCache(VirtualHosterClass ret, CachedHoster cache) {
        ret.flags = cache.getFlags();
        ret.names = cache.getNames();
        ret.patterns = cache.getPatterns();
        ret.revision = cache.getRevision();
        ret.interfaceVersion = cache.getInterfaceVersion();
    }

    private static void writeToCache(VirtualClass c, CachedHoster cache) {
        HosterPluginCache.getInstance().update(c, cache);
    }

    private int interfaceVersion;

    /**
     * init by class. reads class annotations and writes infos to ret
     * 
     * @param ret
     * @param loadedClass
     * @throws PluginLoaderException
     */
    private static void initByClass(VirtualHosterClass ret, Class<?> loadedClass) throws PluginLoaderException {
        HostPlugin an = (HostPlugin) loadedClass.getAnnotation(HostPlugin.class);
        if (an == null) { throw new PluginLoaderException("No Valid Plugin: " + loadedClass.getName()); }
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

    private VirtualHosterClass(VirtualClass c) {
        this.vClass = c;
    }

    private int getInterfaceVersion() {
        return interfaceVersion;
    }

    /**
     * creates HostPluginWrapper for the plugin representation. Class may be
     * unloaded until now. HostPluginWrapper finally is able to load the class
     * as soon as it is required
     */

    public void initWrapper() {
        for (int i = 0; i < names.length; i++) {
            try {
                new HostPluginWrapper(names[i], vClass.getSimpleName(), patterns[i], flags[i], revision);
            } catch (final Throwable e) {
                Log.L.severe("Could not load " + vClass);

            }
        }
    }

    /**
     * 
     * @return true if the plugin does not fit to current interface version
     */
    private boolean isOutdated() {

        if (getInterfaceVersion() != HostPlugin.INTERFACE_VERSION) {
            Log.L.warning("Outdated Plugin found: " + vClass);
            return true;
        }
        return false;
    }

    public boolean isValid() {
        if (vClass.getSimpleName().contains("$")) {
            // inner classes
            return false;
        }

        if (isOutdated()) return false;
        return true;
    }

}
