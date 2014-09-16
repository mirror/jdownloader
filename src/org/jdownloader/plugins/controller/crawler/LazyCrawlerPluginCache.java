package org.jdownloader.plugins.controller.crawler;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.IO;
import org.appwork.utils.awfc.AWFCUtils;
import org.jdownloader.plugins.controller.LazyPluginClass;

public class LazyCrawlerPluginCache {

    private static final int CACHEVERSION = 7;

    public static List<LazyCrawlerPlugin> read(File file, final AtomicLong lastModification) throws IOException {
        final ArrayList<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        if (file.exists()) {
            final InputStream bis = new ByteArrayInputStream(IO.readFile(file));
            final AWFCUtils is = new AWFCUtils(bis);
            if (CACHEVERSION != is.readShort()) {
                throw new IOException("Outdated CacheVersion");
            }
            final long lastModified = is.readLong();
            final int lazyPluginClassSize = is.readShort();
            final byte[] sha256 = new byte[32];
            final byte[] stringBuffer = new byte[32767];
            for (int lazyPluginClassIndex = 0; lazyPluginClassIndex < lazyPluginClassSize; lazyPluginClassIndex++) {
                final LazyPluginClass lazyPluginClass = new LazyPluginClass(is.readString(stringBuffer), is.ensureRead(32, sha256), is.readLong(), (int) is.readLong(), is.readLong());
                final int lazyCrawlerPluginSize = is.readShort();
                for (int lazyHostPluginIndex = 0; lazyHostPluginIndex < lazyCrawlerPluginSize; lazyHostPluginIndex++) {
                    final LazyCrawlerPlugin lazyCrawlerPlugin = new LazyCrawlerPlugin(lazyPluginClass, is.readString(stringBuffer), is.readString(stringBuffer), null, null);
                    final int flags = is.ensureRead();
                    lazyCrawlerPlugin.setHasConfig((flags & (1 << 1)) != 0);
                    if ((flags & (1 << 4)) != 0) {
                        lazyCrawlerPlugin.setConfigInterface(is.readString(stringBuffer));
                    }
                    ret.add(lazyCrawlerPlugin);
                }
            }
            if (lastModification != null) {
                lastModification.set(lastModified);
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
            os.writeShort(CACHEVERSION);
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
                /* plugins */
                final List<LazyCrawlerPlugin> plugins = lazyPluginMapEntry.getValue();
                os.writeShort(plugins.size());
                for (final LazyCrawlerPlugin plugin : plugins) {
                    os.writeString(plugin.getPatternSource());
                    os.writeString(plugin.getDisplayName());
                    byte flags = 0;
                    if (plugin.isHasConfig()) {
                        flags |= (1 << 1);
                    }
                    if (plugin.isHasConfig() && plugin.getConfigInterface() != null) {
                        flags |= (1 << 4);
                    }
                    bos.write(flags);
                    if (plugin.isHasConfig() && plugin.getConfigInterface() != null) {
                        os.writeString(plugin.getConfigInterface());
                    }
                }
            }
            bos.close();
            fos = null;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }
}
