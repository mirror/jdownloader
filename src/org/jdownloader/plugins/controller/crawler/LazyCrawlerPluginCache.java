package org.jdownloader.plugins.controller.crawler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.awfc.AWFCUtils;
import org.appwork.utils.net.CountingInputStream;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.LazyPluginClass;
import org.jdownloader.plugins.controller.host.LazyHostPluginCache;

public class LazyCrawlerPluginCache {
    private static final long CACHEVERSION = 29042024001l + LazyPlugin.FEATURE.CACHEVERSION;

    public static List<LazyCrawlerPlugin> read(File file, final AtomicLong lastModification) throws IOException {
        final ArrayList<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        if (file.exists()) {
            final Map<List<FEATURE>, FEATURE[]> featureCache = new HashMap<List<FEATURE>, FEATURE[]>();
            final Map<Object, List<String>> dependenciesCache = new HashMap<Object, List<String>>();
            final ByteArrayOutputStream byteBuffer = LazyHostPluginCache.readFile(file);
            final CountingInputStream bis = new CountingInputStream(new ByteArrayInputStream(byteBuffer.toByteArray(), 0, byteBuffer.size()));
            try {
                final AWFCUtils is = new AWFCUtils(bis);
                final long version = is.readLong();
                if (CACHEVERSION != version) {
                    throw new IOException("Outdated CacheVersion:" + version + "|" + CACHEVERSION);
                }
                final long lastCacheModified = is.readLong();
                final int lazyPluginClassSize = is.readShort();
                final byte[] stringBuffer = new byte[32767];
                for (int lazyPluginClassIndex = 0; lazyPluginClassIndex < lazyPluginClassSize; lazyPluginClassIndex++) {
                    final String className = is.readString(stringBuffer);
                    final byte[] sha256 = is.ensureRead(32, null);
                    final long lastModified = is.readLong();
                    final int interfaceVersion = (int) is.readLong();
                    final long revision = is.readLong();
                    final int dependenciesCount = is.readShort();
                    List<String> dependencies = null;
                    if (dependenciesCount > 0) {
                        dependencies = new ArrayList<String>(dependenciesCount);
                        for (int index = 0; index < dependenciesCount; index++) {
                            dependencies.add(is.readString(stringBuffer));
                        }
                        if (dependenciesCache.containsKey(dependencies)) {
                            dependencies = dependenciesCache.get(dependencies);
                        } else {
                            dependenciesCache.put(dependencies, dependencies);
                        }
                    }
                    final LazyPluginClass lazyPluginClass = new LazyPluginClass(className, sha256, lastModified, interfaceVersion, revision, dependencies);
                    final int lazyCrawlerPluginSize = is.readShort();
                    for (int lazyCrawlerPluginIndex = 0; lazyCrawlerPluginIndex < lazyCrawlerPluginSize; lazyCrawlerPluginIndex++) {
                        final LazyCrawlerPlugin lazyCrawlerPlugin = new LazyCrawlerPlugin(lazyPluginClass, is.readString(stringBuffer), is.readString(stringBuffer), null, null);
                        lazyCrawlerPlugin.setPluginUsage(is.readLongOptimized());
                        lazyCrawlerPlugin.setMaxConcurrentInstances((int) is.readLongOptimized());
                        final int flags = is.ensureRead();
                        lazyCrawlerPlugin.setHasConfig((flags & (1 << 1)) != 0);
                        if ((flags & (1 << 4)) != 0) {
                            lazyCrawlerPlugin.setConfigInterface(is.readString(stringBuffer));
                        }
                        final int sitesSupportedNumber = is.readShort();
                        if (sitesSupportedNumber == 0) {
                            lazyCrawlerPlugin.setSitesSupported(null);
                        } else {
                            final List<String> sitesSupportedNames = new ArrayList<String>();
                            for (int index = 0; index < sitesSupportedNumber; index++) {
                                sitesSupportedNames.add(is.readString(stringBuffer));
                            }
                            lazyCrawlerPlugin.setSitesSupported(sitesSupportedNames.toArray(new String[0]));
                        }
                        if ((flags & (1 << 2)) != 0) {
                            final ArrayList<FEATURE> features = new ArrayList<FEATURE>(FEATURE.values().length);
                            for (final FEATURE feature : FEATURE.values()) {
                                if (is.readBoolean()) {
                                    features.add(feature);
                                }
                            }
                            FEATURE[] cached = featureCache.get(features);
                            if (cached == null && !featureCache.containsKey(features)) {
                                if (features.size() == 0) {
                                    cached = null;
                                } else {
                                    cached = features.toArray(new FEATURE[0]);
                                }
                                featureCache.put(features, cached);
                            }
                            lazyCrawlerPlugin.setFeatures(cached);
                        }
                        ret.add(lazyCrawlerPlugin);
                    }
                }
                if (lastModification != null) {
                    lastModification.set(lastCacheModified);
                }
            } catch (final IOException e) {
                throw new IOException(e.getMessage() + " (" + bis.transferedBytes() + "/" + bis.available() + ")", e);
            }
        }
        return ret;
    }

    public static void write(List<LazyCrawlerPlugin> lazyPlugins, File file, final AtomicLong lastModification) throws IOException {
        final HashMap<LazyPluginClass, List<LazyCrawlerPlugin>> lazyPluginsMap = new HashMap<LazyPluginClass, List<LazyCrawlerPlugin>>();
        if (lazyPlugins != null) {
            for (LazyCrawlerPlugin lazyPlugin : lazyPlugins) {
                List<LazyCrawlerPlugin> lazyPluginClasses = lazyPluginsMap.get(lazyPlugin.getLazyPluginClass());
                if (lazyPluginClasses == null) {
                    lazyPluginClasses = new ArrayList<LazyCrawlerPlugin>();
                    lazyPluginsMap.put(lazyPlugin.getLazyPluginClass(), lazyPluginClasses);
                }
                lazyPluginClasses.add(lazyPlugin);
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            final BufferedOutputStream bos = new BufferedOutputStream(fos, 32767);
            final AWFCUtils os = new AWFCUtils(bos);
            os.writeLong(CACHEVERSION);
            final long lastModified = lastModification != null ? lastModification.get() : 0;
            os.writeLong(lastModified);
            os.writeShort(lazyPluginsMap.size());
            for (final Entry<LazyPluginClass, List<LazyCrawlerPlugin>> lazyPluginMapEntry : lazyPluginsMap.entrySet()) {
                final LazyPluginClass lazyPluginClass = lazyPluginMapEntry.getKey();
                os.writeString(lazyPluginClass.getClassName());
                bos.write(lazyPluginClass.getSha256());
                os.writeLong(lazyPluginClass.getLastModified());
                os.writeLong(Math.max(0, lazyPluginClass.getInterfaceVersion()));
                os.writeLong(Math.max(0, lazyPluginClass.getRevision()));
                // plugin dependencies
                if (lazyPluginClass.getDependencies() == null || lazyPluginClass.getDependencies().size() == 0) {
                    os.writeShort(0);
                } else {
                    os.writeShort(lazyPluginClass.getDependencies().size());
                    for (final String dependency : lazyPluginClass.getDependencies()) {
                        os.writeString(dependency);
                    }
                }
                /* plugins */
                final List<LazyCrawlerPlugin> plugins = lazyPluginMapEntry.getValue();
                os.writeShort(plugins.size());
                for (final LazyCrawlerPlugin plugin : plugins) {
                    os.writeString(plugin.getPatternSource());
                    os.writeString(plugin.getDisplayName());
                    os.writeLongOptimized(plugin.getPluginUsage());
                    os.writeLongOptimized(plugin.getMaxConcurrentInstances());
                    byte flags = 0;
                    if (plugin.isHasConfig()) {
                        flags |= (1 << 1);
                    }
                    final FEATURE[] features = plugin.getFeatures();
                    if (features != null && features.length > 0) {
                        flags |= (1 << 2);
                    }
                    if (plugin.isHasConfig() && plugin.getConfigInterface() != null) {
                        flags |= (1 << 4);
                    }
                    bos.write(flags);
                    if (plugin.isHasConfig() && plugin.getConfigInterface() != null) {
                        os.writeString(plugin.getConfigInterface());
                    }
                    String[] sitesSupported = plugin.getSitesSupported();
                    if (sitesSupported != null && sitesSupported.length == 1 && plugin.getHost().equals(sitesSupported[0])) {
                        // memory optimization to avoid storing of default value
                        sitesSupported = null;
                    }
                    os.writeShort(sitesSupported == null ? 0 : sitesSupported.length);
                    if (sitesSupported != null) {
                        for (String siteSupported : sitesSupported) {
                            os.writeString(siteSupported);
                        }
                    }
                    if (features != null && features.length > 0) {
                        for (final FEATURE feature : FEATURE.values()) {
                            os.writeBoolean(feature.isSet(features));
                        }
                    }
                }
            }
            bos.flush();
            bos.close();
            fos.close();
            fos = null;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }
}
