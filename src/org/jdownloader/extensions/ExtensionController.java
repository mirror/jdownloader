package org.jdownloader.extensions;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.JDInitFlags;
import jd.Launcher;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;

public class ExtensionController {
    private static final ExtensionController INSTANCE = new ExtensionController();

    /**
     * get the only existing instance of ExtensionController. This is a singleton
     * 
     * @return
     */
    public static ExtensionController getInstance() {
        return ExtensionController.INSTANCE;
    }

    private List<LazyExtension>            list;

    private ExtensionControllerEventSender eventSender;

    /**
     * Create a new instance of ExtensionController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ExtensionController() {
        eventSender = new ExtensionControllerEventSender();
        list = Collections.unmodifiableList(new ArrayList<LazyExtension>());

    }

    private File getCache() {
        return Application.getResource("tmp/extensioncache/extensionInfos.json");
    }

    public ExtensionControllerEventSender getEventSender() {
        return eventSender;
    }

    public void init() {
        synchronized (this) {
            java.util.List<LazyExtension> ret = new ArrayList<LazyExtension>();
            final long t = System.currentTimeMillis();
            try {
                if (JDInitFlags.REFRESH_CACHE) {
                    try {
                        /* do a fresh scan */
                        ret = load();
                    } catch (Throwable e) {
                        Log.L.severe("@ExtensionController: update failed!");
                        Log.exception(e);
                    }
                } else {
                    /* try to load from cache */
                    try {
                        ret = loadFromCache();
                    } catch (Throwable e) {
                        Log.L.severe("@ExtensionController: cache failed!");
                        Log.exception(e);
                    }
                    if (ret.size() == 0) {
                        try {
                            /* do a fresh scan */
                            ret = load();
                        } catch (Throwable e) {
                            Log.L.severe("@ExtensionController: update failed!");
                            Log.exception(e);
                        }
                    }
                }
            } finally {
                Log.L.info("@ExtensionController: init in" + (System.currentTimeMillis() - t) + "ms :" + ret.size());
            }
            if (ret.size() == 0) {
                Log.L.severe("@ExtensionController: WTF, no extensions!");
            }
            try {
                Collections.sort(ret, new Comparator<LazyExtension>() {

                    public int compare(LazyExtension o1, LazyExtension o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            } catch (final Throwable e) {
                Log.exception(e);
            }
            list = Collections.unmodifiableList(ret);
            Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {

                public void run() {
                    List<LazyExtension> llist = list;
                    for (LazyExtension plg : llist) {
                        if (plg._getExtension() != null && plg._getExtension().getGUI() != null) {
                            plg._getExtension().getGUI().restore();
                        }
                    }
                }

            });
        }
        getEventSender().fireEvent(new ExtensionControllerEvent(this, ExtensionControllerEvent.Type.UPDATED));
    }

    private java.util.List<LazyExtension> loadFromCache() throws InstantiationException, IllegalAccessException, ClassNotFoundException, StartException {
        java.util.List<LazyExtension> cache = JSonStorage.restoreFrom(getCache(), true, null, new TypeRef<ArrayList<LazyExtension>>() {
        }, new ArrayList<LazyExtension>());

        java.util.List<LazyExtension> lst = new ArrayList<LazyExtension>(cache);
        for (Iterator<LazyExtension> it = lst.iterator(); it.hasNext();) {

            LazyExtension l = it.next();
            if (l.getJarPath() == null || !new File(l.getJarPath()).exists()) { throw new InstantiationException("Jar Path " + l.getJarPath() + " is invalid"); }
            if (l._isEnabled()) {
                // if exception occures here, we do a complete rescan. cache
                // might be out of date
                l.init();

            }

        }
        return lst;
    }

    private synchronized java.util.List<LazyExtension> load() {
        java.util.List<LazyExtension> ret = new ArrayList<LazyExtension>();
        try {
            if (Application.isJared(ExtensionController.class)) {
                ret = loadJared();
            } else {
                ret = loadUnpacked();
            }
            JSonStorage.saveTo(getCache(), ret);
        } catch (final Throwable e) {
            Log.exception(e);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<LazyExtension> loadJared() {
        java.util.List<LazyExtension> ret = new ArrayList<LazyExtension>();
        File[] addons = Application.getResource("extensions").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (addons != null) {
            HashSet<File> dupes = new HashSet<File>();
            HashSet<URL> urlDupes = new HashSet<URL>();
            main: for (File jar : addons) {
                try {

                    URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURI().toURL() }, null);
                    final Enumeration<URL> urls = cl.getResources(AbstractExtension.class.getPackage().getName().replace('.', '/'));
                    URL url;
                    Pattern pattern = Pattern.compile(Pattern.quote(AbstractExtension.class.getPackage().getName().replace('.', '/')) + "/(\\w+)/(\\w+Extension)\\.class");

                    while (urls.hasMoreElements()) {
                        url = urls.nextElement();
                        if (urlDupes.add(url)) {

                            if ("jar".equalsIgnoreCase(url.getProtocol())) {

                                // jarred addon (JAR)
                                File jarFile = new File(new URL(url.getPath().substring(0, url.getPath().lastIndexOf('!'))).toURI());

                                if (dupes.add(jarFile)) {
                                    FileInputStream fis = null;
                                    JarInputStream jis = null;
                                    try {
                                        jis = new JarInputStream(fis = new FileInputStream(jarFile));
                                        JarEntry e;
                                        while ((e = jis.getNextJarEntry()) != null) {
                                            try {

                                                Matcher matcher = pattern.matcher(e.getName());

                                                if (matcher.find()) {
                                                    String pkg = matcher.group(1);
                                                    String clazzName = matcher.group(2);
                                                    Class<?> clazz = new URLClassLoader(new URL[] { jar.toURI().toURL() }).loadClass(AbstractExtension.class.getPackage().getName() + "." + pkg + "." + clazzName);

                                                    if (AbstractExtension.class.isAssignableFrom(clazz)) {

                                                        initModule((Class<AbstractExtension<?, ?>>) clazz, ret, jarFile);
                                                        continue main;
                                                    }
                                                }
                                            } catch (Throwable e1) {
                                                Log.exception(e1);
                                            }
                                        }
                                    } finally {
                                        try {
                                            jis.close();
                                        } catch (final Throwable e) {
                                        }
                                        try {
                                            fis.close();
                                        } catch (final Throwable e) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.exception(e);
                }
            }
        }
        return ret;

    }

    @SuppressWarnings("unchecked")
    private java.util.List<LazyExtension> loadUnpacked() {
        java.util.List<LazyExtension> retl = new ArrayList<LazyExtension>();
        URL ret = getClass().getResource("/");
        File root;
        if ("file".equalsIgnoreCase(ret.getProtocol())) {
            try {
                root = new File(ret.toURI());
            } catch (URISyntaxException e) {
                Log.exception(e);
                Log.L.finer("Did not load unpacked Extensions from " + ret);
                return retl;
            }
        } else {
            Log.L.finer("Did not load unpacked Extensions from " + ret);
            return retl;
        }
        root = new File(root, AbstractExtension.class.getPackage().getName().replace('.', '/'));
        Log.L.finer("Load Extensions from: " + root.getAbsolutePath());
        File[] folders = root.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (folders != null) {
            ClassLoader cl = getClass().getClassLoader();
            main: for (File f : folders) {
                File[] modules = f.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith("Extension.class");
                    }
                });
                boolean loaded = false;
                if (modules != null) {
                    for (File module : modules) {
                        Class<?> cls;
                        try {
                            cls = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + module.getParentFile().getName() + "." + module.getName().substring(0, module.getName().length() - 6));
                            if (AbstractExtension.class.isAssignableFrom(cls)) {
                                initModule((Class<AbstractExtension<?, ?>>) cls, retl, f);
                                loaded = true;
                                continue main;
                            }
                        } catch (IllegalArgumentException e) {
                            Log.L.warning("Did not init Extension " + module + " : " + e.getMessage());
                        } catch (Throwable e) {
                            Log.exception(e);
                            Dialog.getInstance().showExceptionDialog("Error", e.getMessage(), e);
                        }
                    }
                    if (!loaded) {
                        Log.L.warning("Could not load any Extension Module from " + f);
                    }
                }
            }
        }
        return retl;
    }

    private java.util.List<LazyExtension> initModule(Class<AbstractExtension<?, ?>> cls, java.util.List<LazyExtension> list, File jarFile) throws InstantiationException, IllegalAccessException, StartException, IOException, ClassNotFoundException {
        if (list == null) list = new ArrayList<LazyExtension>();
        String id = cls.getName().substring(27);

        Log.L.fine("Load Extension: " + id);
        LazyExtension extension = LazyExtension.create(id, cls);
        extension.setJarPath(jarFile.getAbsolutePath());
        extension._setPluginClass(cls);
        if (extension._isEnabled()) {
            extension.init();
        }
        list.add(extension);
        return list;
    }

    public boolean isExtensionActive(Class<? extends AbstractExtension<?, ?>> class1) {
        List<LazyExtension> llist = list;
        for (LazyExtension l : llist) {
            if (class1.getName().equals(l.getClassname())) {
                if (l._getExtension() != null && l._getExtension().isEnabled()) return true;
            }
        }
        return false;
    }

    public List<LazyExtension> getExtensions() {
        return list;
    }

    /**
     * Returns a list of all currently running extensions
     * 
     * @return
     */
    public java.util.List<AbstractExtension<?, ?>> getEnabledExtensions() {
        java.util.List<AbstractExtension<?, ?>> ret = new ArrayList<AbstractExtension<?, ?>>();
        List<LazyExtension> llist = list;
        for (LazyExtension aew : llist) {
            if (aew._getExtension() != null && aew._getExtension().isEnabled()) ret.add(aew._getExtension());
        }
        return ret;
    }

    public <T extends AbstractExtension<?, ?>> LazyExtension getExtension(Class<T> class1) {
        List<LazyExtension> llist = list;
        for (LazyExtension l : llist) {
            if (class1.getName().equals(l.getClassname())) { return l; }
        }
        return null;
    }

}
