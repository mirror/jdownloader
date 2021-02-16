package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController.CHECK_RESULT;
import org.jdownloader.plugins.controller.PluginController.PluginClassInfo;

public class PluginScannerNIO<T extends Plugin> {
    private final static boolean      isJava16orOlder = Application.getJavaVersion() <= Application.JAVA16;
    private final PluginController<T> pluginController;

    protected PluginController<T> getPluginController() {
        return pluginController;
    }

    public PluginScannerNIO(PluginController<T> pluginController) {
        this.pluginController = pluginController;
    }

    protected List<PluginInfo<T>> scan(LogSource logger, String hosterpath, final List<? extends LazyPlugin<T>> pluginCache, final AtomicLong lastFolderModification) throws Exception, OutOfMemoryError {
        DirectoryStream<Path> stream = null;
        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        final long timeStamp = System.currentTimeMillis();
        final Map<Object, List<String>> dependenciesCache = new HashMap<Object, List<String>>();
        try {
            final long lastFolderModifiedCheck = lastFolderModification != null ? lastFolderModification.get() : -1;
            final Path folder = Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath).toPath();
            long lastFolderModifiedScanStart = Files.readAttributes(folder, BasicFileAttributes.class).lastModifiedTime().toMillis();
            if (pluginCache == null || pluginCache.size() == 0 || lastFolderModifiedCheck <= 0) {
                logger.info("@PluginController(NIO): no plugin cache available|LastModified:" + lastFolderModifiedCheck);
            } else {
                boolean lastModifiedTimestampUnchanged = lastFolderModifiedScanStart == lastFolderModifiedCheck;
                if (lastModifiedTimestampUnchanged) {
                    try {
                        while (true) {
                            final String uniqueID = UniqueAlltimeID.create();
                            final File testLastModified = new File(folder.toFile(), uniqueID);
                            if (!testLastModified.exists()) {
                                IO.secureWrite(testLastModified, uniqueID, SYNC.META_AND_DATA);
                                if (!testLastModified.delete()) {
                                    testLastModified.deleteOnExit();
                                }
                                final long testLastFolderModifiedScanStart = Files.readAttributes(folder, BasicFileAttributes.class).lastModifiedTime().toMillis();
                                if (testLastFolderModifiedScanStart == lastFolderModifiedScanStart) {
                                    logger.info("@PluginController(NIO): lastModified timestamp change test: failed");
                                    lastModifiedTimestampUnchanged = false;
                                } else {
                                    logger.info("@PluginController(NIO): lastModified timestamp change test: successful");
                                    lastFolderModifiedScanStart = testLastFolderModifiedScanStart;
                                }
                                break;
                            }
                        }
                    } catch (IOException e) {
                        logger.exception("@PluginController(NIO): lastModified timestamp change test: error", e);
                        lastModifiedTimestampUnchanged = false;
                    }
                }
                if (lastModifiedTimestampUnchanged) {
                    for (final LazyPlugin<T> lazyPlugin : pluginCache) {
                        final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPlugin.getLazyPluginClass(), lazyPlugin);
                        ret.add(pluginInfo);
                    }
                    logger.info("@PluginController(NIO): plugin cache valid|Size:" + pluginCache.size() + "|LastModified:" + lastFolderModifiedScanStart);
                    if (lastFolderModification != null) {
                        lastFolderModification.set(lastFolderModifiedScanStart);
                    }
                    return ret;
                } else {
                    logger.info("@PluginController(NIO): plugin cache invalid|Size:" + pluginCache.size() + "|LastModified:" + lastFolderModifiedScanStart);
                }
            }
            final String pkg = hosterpath.replace("/", ".");
            final HashMap<String, List<LazyPlugin<T>>> lazyPluginClassMap;
            if (pluginCache != null && pluginCache.size() > 0) {
                lazyPluginClassMap = new HashMap<String, List<LazyPlugin<T>>>();
                for (final LazyPlugin<T> lazyPlugin : pluginCache) {
                    List<LazyPlugin<T>> list = lazyPluginClassMap.get(lazyPlugin.getLazyPluginClass().getClassName());
                    if (list == null) {
                        list = new ArrayList<LazyPlugin<T>>();
                        lazyPluginClassMap.put(lazyPlugin.getLazyPluginClass().getClassName(), list);
                    }
                    list.add(lazyPlugin);
                }
            } else {
                lazyPluginClassMap = null;
            }
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] mdCache = new byte[32767];
            PluginClassLoaderChild cl = null;
            stream = Files.newDirectoryStream(folder, "*.class");
            for (final Path path : stream) {
                try {
                    final String pathFileName = path.getFileName().toString();
                    final String className = pathFileName.substring(0, pathFileName.length() - 6);
                    if (className.indexOf("$") < 0 && !PluginController.IGNORELIST.contains(className)) {
                        if (cl == null || isJava16orOlder) {
                            cl = PluginClassLoader.getInstance().getChild();
                            cl.setMapStaticFields(false);
                        }
                        final BasicFileAttributes pathAttr = Files.readAttributes(path, BasicFileAttributes.class);
                        final long lastFileModification = pathAttr.lastModifiedTime().toMillis();
                        if (lazyPluginClassMap != null) {
                            final List<LazyPlugin<T>> lazyPlugins = lazyPluginClassMap.get(className);
                            if (lazyPlugins != null && lazyPlugins.size() > 0) {
                                final LazyPluginClass lazyPluginClass = lazyPlugins.get(0).getLazyPluginClass();
                                try {
                                    if (lazyPluginClass != null && CHECK_RESULT.isSuccessFul(getPluginController().checkForChanges(dependenciesCache, cl, lazyPluginClass, lastFileModification))) {
                                        for (final LazyPlugin<T> lazyPlugin : lazyPlugins) {
                                            // logger.finer("Cached: " + className + "|" + lazyPlugin.getDisplayName() + "|" +
                                            // lazyPluginClass.getRevision());
                                            final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, lazyPlugin);
                                            ret.add(pluginInfo);
                                        }
                                        continue;
                                    }
                                } catch (final Throwable e) {
                                    logger.exception("Failed: " + className, e);
                                }
                            }
                        }
                        PluginClassInfo<T> pluginClassInfo = null;
                        try {
                            final Class<T> pluginClass = (Class<T>) cl.loadClass(pkg + "." + className);
                            if (!Modifier.isAbstract(pluginClass.getModifiers()) && Plugin.class.isAssignableFrom(pluginClass)) {
                                pluginClassInfo = getPluginController().getPluginClassInfo(dependenciesCache, pluginClass);
                                if (pluginClassInfo == null) {
                                    continue;
                                } else {
                                    pluginClassInfo.sha256 = PluginController.getFileHashBytes(path.toFile(), md, mdCache);
                                }
                            } else {
                                continue;
                            }
                        } catch (final OutOfMemoryError e) {
                            logger.exception("Failed: " + className, e);
                            throw e;
                        } catch (final Throwable e) {
                            logger.exception("Failed: " + className, e);
                            continue;
                        }
                        //
                        final LazyPluginClass lazyPluginClass = new LazyPluginClass(className, pluginClassInfo.sha256, lastFileModification, pluginClassInfo.interfaceVersion, pluginClassInfo.revision, pluginClassInfo.dependencies);
                        final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, pluginClassInfo.clazz);
                        // logger.finer("Scaned: " + className + "|" + lazyPluginClass.getRevision());
                        ret.add(pluginInfo);
                    }
                } catch (final OutOfMemoryError e) {
                    logger.exception("Failed: " + path, e);
                    throw e;
                } catch (Throwable e) {
                    logger.exception("Failed: " + path, e);
                }
            }
            if (stream != null) {
                stream.close();
            }
            final long lastFolderModifiedScanStop = Files.readAttributes(folder, BasicFileAttributes.class).lastModifiedTime().toMillis();
            if (lastFolderModifiedScanStart != lastFolderModifiedScanStop) {
                logger.info("@PluginController(NIO): folder modification during scan detected!LastModified:" + lastFolderModifiedScanStart + "!=" + lastFolderModifiedScanStop);
                Thread.sleep(1000);
                return scan(logger, hosterpath, pluginCache, lastFolderModification);
            } else {
                if (lastFolderModification != null) {
                    lastFolderModification.set(lastFolderModifiedScanStop);
                }
                return ret;
            }
        } finally {
            logger.info("@PluginController(NIO): scan took " + (System.currentTimeMillis() - timeStamp) + "ms for " + ret.size());
            if (stream != null) {
                stream.close();
            }
        }
    }
}
