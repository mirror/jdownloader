package org.jdownloader.plugins.controller.host;

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

import org.appwork.utils.IO;
import org.appwork.utils.awfc.AWFCUtils;
import org.jdownloader.plugins.controller.LazyPluginClass;

public class LazyHostPluginCache {

    private static final int CACHEVERSION = 6;

    public static List<LazyHostPlugin> read(File file) throws IOException {
        final ArrayList<LazyHostPlugin> ret = new ArrayList<LazyHostPlugin>(4096);
        if (file.exists()) {
            final InputStream bis = new ByteArrayInputStream(IO.readFile(file));
            final AWFCUtils is = new AWFCUtils(bis);
            if (CACHEVERSION != is.readShort()) {
                throw new IOException("Outdated CacheVersion");
            }
            final int lazyPluginClassSize = is.readShort();
            final byte[] sha256 = new byte[32];
            final byte[] stringBuffer = new byte[32767];
            for (int lazyPluginClassIndex = 0; lazyPluginClassIndex < lazyPluginClassSize; lazyPluginClassIndex++) {
                final LazyPluginClass lazyPluginClass = new LazyPluginClass(is.readString(stringBuffer), is.ensureRead(32, sha256), is.readLong(), (int) is.readLong(), is.readLong());
                final int lazyHostPluginSize = is.readShort();
                for (int lazyHostPluginIndex = 0; lazyHostPluginIndex < lazyHostPluginSize; lazyHostPluginIndex++) {
                    final LazyHostPlugin lazyHostPlugin = new LazyHostPlugin(lazyPluginClass, is.readString(stringBuffer), is.readString(stringBuffer), null, null);
                    final int flags = is.ensureRead();
                    lazyHostPlugin.setPremium((flags & (1 << 0)) != 0);
                    lazyHostPlugin.setHasConfig((flags & (1 << 1)) != 0);
                    lazyHostPlugin.setHasLinkRewrite((flags & (1 << 2)) != 0);
                    lazyHostPlugin.setHasAccountRewrite((flags & (1 << 3)) != 0);
                    if ((flags & (1 << 5)) != 0) {
                        lazyHostPlugin.setPremiumUrl(is.readString(stringBuffer));
                    }
                    if ((flags & (1 << 4)) != 0) {
                        lazyHostPlugin.setConfigInterface(is.readString(stringBuffer));
                    }
                    ret.add(lazyHostPlugin);
                }
            }
        }
        return ret;
    }

    public static void write(List<LazyHostPlugin> lazyPlugins, File file) throws IOException {
        final HashMap<LazyPluginClass, List<LazyHostPlugin>> lazyPluginsMap = new HashMap<LazyPluginClass, List<LazyHostPlugin>>();
        if (lazyPlugins != null) {
            for (LazyHostPlugin lazyPlugin : lazyPlugins) {
                List<LazyHostPlugin> lazyPluginClasses = lazyPluginsMap.get(lazyPlugin.getLazyPluginClass());
                if (lazyPluginClasses == null) {
                    lazyPluginClasses = new ArrayList<LazyHostPlugin>();
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
            os.writeShort(lazyPluginsMap.size());
            for (final Entry<LazyPluginClass, List<LazyHostPlugin>> lazyPluginMapEntry : lazyPluginsMap.entrySet()) {
                final LazyPluginClass lazyPluginClass = lazyPluginMapEntry.getKey();
                os.writeString(lazyPluginClass.getClassName());
                bos.write(lazyPluginClass.getSha256());
                os.writeLong(lazyPluginClass.getLastModified());
                os.writeLong(Math.max(0, lazyPluginClass.getInterfaceVersion()));
                os.writeLong(Math.max(0, lazyPluginClass.getRevision()));
                /* plugins */
                final List<LazyHostPlugin> plugins = lazyPluginMapEntry.getValue();
                os.writeShort(plugins.size());
                for (final LazyHostPlugin plugin : plugins) {
                    os.writeString(plugin.getPatternSource());
                    os.writeString(plugin.getDisplayName());
                    byte flags = 0;
                    if (plugin.isPremium()) {
                        flags |= (1 << 0);
                    }
                    if (plugin.isHasConfig()) {
                        flags |= (1 << 1);
                    }
                    if (plugin.isHasLinkRewrite()) {
                        flags |= (1 << 2);
                    }
                    if (plugin.isHasAccountRewrite()) {
                        flags |= (1 << 3);
                    }
                    if (plugin.isHasConfig() && plugin.getConfigInterface() != null) {
                        flags |= (1 << 4);
                    }
                    if (plugin.isPremium() && plugin.getPremiumUrl() != null) {
                        flags |= (1 << 5);
                    }
                    bos.write(flags);
                    if (plugin.isPremium() && plugin.getPremiumUrl() != null) {
                        os.writeString(plugin.getPremiumUrl());
                    }
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
