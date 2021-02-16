package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class PluginController<T extends Plugin> {
    protected static final File TMP_INVALIDPLUGINS = Application.getTempResource("invalidplugins");

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

    protected abstract CHECK_RESULT checkForChanges(Map<Object, List<String>> dependenciesCache, PluginClassLoaderChild classLoader, LazyPluginClass lazyPluginClass, long lastFileModification) throws Exception;

    protected static byte[] getFileHashBytes(final File arg, final MessageDigest md, final byte[] mdCache) throws IOException {
        if (arg == null || !arg.isFile()) {
            return null;
        } else {
            final FileInputStream fis = new FileInputStream(arg);
            return getFileHashBytes(fis, md, mdCache);
        }
    }

    protected abstract PluginClassInfo<T> getPluginClassInfo(Map<Object, List<String>> dependenciesCache, Class<T> clazz) throws Exception;

    protected abstract String getPluginPath();

    protected List<PluginInfo<T>> scan(LogSource logger, final List<? extends LazyPlugin<T>> pluginCache, final AtomicLong lastFolderModification) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && lastFolderModification != null) {
            lastFolderModification.set(-1l);
        }
        if (Application.getJavaVersion() >= Application.JAVA17) {
            return new PluginScannerNIO<T>(this).scan(logger, getPluginPath(), pluginCache, lastFolderModification);
        } else {
            return new PluginScannerFiles<T>(this).scan(logger, getPluginPath(), pluginCache, lastFolderModification);
        }
    }
}
