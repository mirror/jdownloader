package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class PluginController<T extends Plugin> {

    private final static HashSet<String> ignoreList = new HashSet<String>();
    static {
        ignoreList.add("YoutubeDashConfigPanel");
        ignoreList.add("RTMPDownload");
        ignoreList.add("YoutubeHelper");
        ignoreList.add("K2SApi");
    }

    protected List<PluginInfo<T>> scan(LogSource logger, String hosterpath, Map<LazyPluginClass, ArrayList<LazyPlugin>> pluginCache) throws Exception {
        DirectoryStream<Path> stream = null;
        PluginClassLoaderChild cl = null;
        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        MessageDigest md = null;
        final String pkg = hosterpath.replace("/", ".");
        final byte[] mdCache = new byte[32767];
        final HashMap<String, LazyPluginClass> lazyPluginClassMap;
        if (pluginCache != null && pluginCache.size() > 0) {
            lazyPluginClassMap = new HashMap<String, LazyPluginClass>();
            for (final LazyPluginClass lazyPluginClass : pluginCache.keySet()) {
                lazyPluginClassMap.put(lazyPluginClass.getClassName() + ".class", lazyPluginClass);
            }
        } else {
            lazyPluginClassMap = null;
        }
        final long timeStamp = System.currentTimeMillis();
        try {
            stream = Files.newDirectoryStream(Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath).toPath(), "*.class");
            for (final Path path : stream) {
                final String pathFileName = path.getFileName().toString();
                final String className = pathFileName.substring(0, pathFileName.length() - 6);
                if (className.indexOf("$") < 0 && !ignoreList.contains(className)) {
                    byte[] sha256 = null;
                    final BasicFileAttributes pathAttr = Files.readAttributes(path, BasicFileAttributes.class);
                    if (lazyPluginClassMap != null) {
                        final LazyPluginClass lazyPluginClass = lazyPluginClassMap.get(pathFileName);
                        if (lazyPluginClass != null && (lazyPluginClass.getLastModified() == pathAttr.lastModifiedTime().toMillis() || ((md = MessageDigest.getInstance("SHA-256")) != null && (sha256 = getFileHashBytes(path.toFile(), md, mdCache)) != null && Arrays.equals(sha256, lazyPluginClass.getSha256())))) {
                            for (final LazyPlugin lazyPlugin : pluginCache.get(lazyPluginClass)) {
                                // logger.finer("Cached: " + className + "|" + lazyPlugin.getDisplayName() + "|" +
                                // lazyPluginClass.getRevision());
                                final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, null);
                                pluginInfo.setLazyPlugin(lazyPlugin);
                                ret.add(pluginInfo);
                            }
                            continue;
                        }
                    }
                    Class<T> pluginClass = null;
                    long[] infos = null;
                    try {
                        if (cl == null) {
                            cl = PluginClassLoader.getInstance().getChild();
                        }
                        if (md == null) {
                            md = MessageDigest.getInstance("SHA-256");
                        }
                        pluginClass = (Class<T>) cl.loadClass(pkg + "." + className);
                        if (!Modifier.isAbstract(pluginClass.getModifiers()) && Plugin.class.isAssignableFrom(pluginClass)) {
                            infos = getInfos(pluginClass);
                            if (infos == null) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.finer("Failed: " + className);
                        logger.log(e);
                        continue;
                    }
                    if (sha256 == null) {
                        sha256 = getFileHashBytes(path.toFile(), md, mdCache);
                    }
                    final LazyPluginClass lazyPluginClass = new LazyPluginClass(className, sha256, pathAttr.lastModifiedTime().toMillis(), (int) infos[0], infos[1]);
                    final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, pluginClass);
                    // logger.finer("Scaned: " + className + "|" + lazyPluginClass.getRevision());
                    ret.add(pluginInfo);
                }
            }
            return ret;
        } finally {
            logger.info("@PluginController: scan took " + (System.currentTimeMillis() - timeStamp) + "ms for " + ret.size());
            if (stream != null) {
                stream.close();
            }
        }
    }

    protected abstract long[] getInfos(Class<T> clazz);

    private static byte[] getFileHashBytes(final File arg, final MessageDigest md, final byte[] mdCache) throws IOException {
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
