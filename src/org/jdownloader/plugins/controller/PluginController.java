package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.DecrypterPlugin;
import jd.plugins.HostPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.DebugMode;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class PluginController<T extends Plugin> {
    public static class PluginClassInfo<T extends Plugin> {
        public byte[]       sha256;
        public int          interfaceVersion = -1;
        public long         revision         = -1;
        public List<String> dependencies     = null;
        public Class<T>     clazz;
    }

    public static enum CHECK_RESULT {
        FAILED_LASTMODIFIED,
        FAILED_DEPENDENCIES,
        SUCCESSFUL_DEPENDENCIES,
        SUCCESSFUL;
        public static boolean isSuccessFul(CHECK_RESULT result) {
            switch (result) {
            case SUCCESSFUL:
            case SUCCESSFUL_DEPENDENCIES:
                return true;
            default:
                return false;
            }
        }
    }

    protected final static HashSet<String> IGNORELIST = new HashSet<String>();
    static {
        IGNORELIST.add("YoutubeDashConfigPanel");
        IGNORELIST.add("RTMPDownload");
        IGNORELIST.add("YoutubeHelper");
        IGNORELIST.add("K2SApi");
        IGNORELIST.add("antiDDoSForDecrypt");
        IGNORELIST.add("antiDDoSForHost");
    }

    protected static byte[] getFileHashBytes(InputStream is, final MessageDigest md, final byte[] mdCache) throws IOException {
        try {
            md.reset();
            int n = 0;
            while ((n = is.read(mdCache)) >= 0) {
                if (n > 0) {
                    md.update(mdCache, 0, n);
                }
            }
            return md.digest();
        } finally {
            try {
                is.close();
            } catch (final Throwable e) {
            }
        }
    }

    protected CHECK_RESULT checkForChanges(Map<Object, List<String>> dependenciesCache, PluginClassLoaderChild classLoader, LazyPluginClass lazyPluginClass, long lastFileModification) throws Exception {
        if (lazyPluginClass.getLastModified() != lastFileModification) {
            return CHECK_RESULT.FAILED_LASTMODIFIED;
        } else if (lazyPluginClass.getDependencies() != null) {
            if (dependenciesCache.containsKey(lazyPluginClass.getDependencies())) {
                return CHECK_RESULT.SUCCESSFUL_DEPENDENCIES;
            } else {
                final Iterator<String> it = lazyPluginClass.getDependencies().iterator();
                while (it.hasNext()) {
                    final String className = it.next();
                    final Class<?> checkClazz = classLoader.loadClass(className);
                    final String revision;
                    final HostPlugin hostPlugin = checkClazz.getAnnotation(HostPlugin.class);
                    if (hostPlugin != null) {
                        revision = hostPlugin.revision();
                    } else {
                        final DecrypterPlugin decrypterPlugin = checkClazz.getAnnotation(DecrypterPlugin.class);
                        if (decrypterPlugin != null) {
                            revision = decrypterPlugin.revision();
                        } else {
                            return CHECK_RESULT.FAILED_DEPENDENCIES;
                        }
                    }
                    if (!StringUtils.equals(revision, it.next())) {
                        return CHECK_RESULT.FAILED_DEPENDENCIES;
                    }
                }
                dependenciesCache.put(lazyPluginClass.getDependencies(), lazyPluginClass.getDependencies());
                return CHECK_RESULT.SUCCESSFUL_DEPENDENCIES;
            }
        } else {
            return CHECK_RESULT.SUCCESSFUL;
        }
    }

    protected static byte[] getFileHashBytes(final File arg, final MessageDigest md, final byte[] mdCache) throws IOException {
        if (arg == null || !arg.isFile()) {
            return null;
        } else {
            final FileInputStream fis = new FileInputStream(arg);
            return getFileHashBytes(fis, md, mdCache);
        }
    }

    protected LinkedHashMap<Class<?>, String> getClassHierarchy(LinkedHashMap<Class<?>, String> dependencies, final Class<? extends Plugin> clazz) {
        final boolean hierarchyStart;
        if (dependencies == null) {
            hierarchyStart = true;
            dependencies = new LinkedHashMap<Class<?>, String>();
        } else {
            hierarchyStart = false;
        }
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            if (PluginForHost.class.equals(currentClazz) || PluginForDecrypt.class.equals(currentClazz)) {
                break;
            } else if (!dependencies.containsKey(currentClazz)) {
                final HostPlugin hostPlugin = PluginForHost.class.isAssignableFrom(currentClazz) ? currentClazz.getAnnotation(HostPlugin.class) : null;
                final DecrypterPlugin decryptPlugin = PluginForDecrypt.class.isAssignableFrom(currentClazz) ? currentClazz.getAnnotation(DecrypterPlugin.class) : null;
                if (hostPlugin != null) {
                    dependencies.put(currentClazz, hostPlugin.revision());
                } else if (decryptPlugin != null) {
                    dependencies.put(currentClazz, decryptPlugin.revision());
                } else {
                    dependencies.put(currentClazz, null);
                }
                final PluginDependencies pluginDependencies = currentClazz.getAnnotation(PluginDependencies.class);
                if (pluginDependencies != null) {
                    for (Class<? extends Plugin> dependency : pluginDependencies.dependencies()) {
                        if (!dependencies.containsKey(dependency)) {
                            getClassHierarchy(dependencies, dependency);
                        }
                    }
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }
        if (hierarchyStart) {
            dependencies.remove(clazz);
        }
        return dependencies;
    }

    protected List<String> getClassDependencies(Map<Object, List<String>> dependenciesCache, Class<? extends Plugin> clazz) throws Exception {
        final LinkedHashMap<Class<?>, String> clazzHierarchy = getClassHierarchy(null, clazz);
        List<String> dependencies = new ArrayList<String>();
        for (final Entry<Class<?>, String> dependency : clazzHierarchy.entrySet()) {
            if (dependency.getValue() != null) {
                dependencies.add(dependency.getKey().getName());
                dependencies.add(dependency.getValue());
            }
        }
        if (dependencies.size() > 0) {
            if (dependenciesCache.containsKey(dependencies)) {
                dependencies = dependenciesCache.get(dependencies);
            } else {
                dependenciesCache.put(dependencies, dependencies);
            }
        }
        if (dependencies.size() > 0) {
            return dependencies;
        } else {
            return null;
        }
    }

    protected abstract PluginClassInfo<T> getPluginClassInfo(Map<Object, List<String>> dependenciesCache, Class<T> clazz) throws Exception;

    protected abstract String getPluginPath();

    protected List<PluginInfo<T>> scan(LogSource logger, final List<? extends LazyPlugin<T>> pluginCache, final AtomicLong lastFolderModification) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && lastFolderModification != null) {
            lastFolderModification.set(-1l);
        }
        if (JVMVersion.isMinimum(JVMVersion.JAVA_1_7)) {
            return new PluginScannerNIO<T>(this).scan(logger, getPluginPath(), pluginCache, lastFolderModification);
        } else {
            return new PluginScannerFiles<T>(this).scan(logger, getPluginPath(), pluginCache, lastFolderModification);
        }
    }
}
