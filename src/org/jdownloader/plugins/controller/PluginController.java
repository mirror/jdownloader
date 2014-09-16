package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;

public abstract class PluginController<T extends Plugin> {

    protected final static HashSet<String> IGNORELIST = new HashSet<String>();
    static {
        IGNORELIST.add("YoutubeDashConfigPanel");
        IGNORELIST.add("RTMPDownload");
        IGNORELIST.add("YoutubeHelper");
        IGNORELIST.add("K2SApi");
    }

    protected abstract long[] getInfos(Class<T> clazz);

    protected List<PluginInfo<T>> scan(LogSource logger, String hosterpath, final List<? extends LazyPlugin> pluginCache, final AtomicLong lastFolderModification) throws Exception {
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return new PluginScannerNIO<T>(this).scan(logger, hosterpath, pluginCache, lastFolderModification);
        }
        return new PluginScannerFiles<T>(this).scan(logger, hosterpath, pluginCache, lastFolderModification);
    }

    protected static byte[] getFileHashBytes(final File arg, final MessageDigest md, final byte[] mdCache) throws IOException {
        if (arg == null || !arg.exists() || arg.isDirectory()) {
            return null;
        }
        FileInputStream fis = null;
        try {
            md.reset();
            fis = new FileInputStream(arg);
            int n = 0;
            while ((n = fis.read(mdCache)) >= 0) {
                if (n > 0) {
                    md.update(mdCache, 0, n);
                }
            }
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        return md.digest();
    }
}
