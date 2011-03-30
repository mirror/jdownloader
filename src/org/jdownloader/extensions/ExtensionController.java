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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.jdownloader.translate.JDT;

public class ExtensionController {
    private static final ExtensionController INSTANCE = new ExtensionController();

    /**
     * get the only existing instance of ExtensionController. This is a
     * singleton
     * 
     * @return
     */
    public static ExtensionController getInstance() {
        return ExtensionController.INSTANCE;
    }

    private HashMap<Class<AbstractExtension>, AbstractExtensionWrapper> map;
    private ArrayList<AbstractExtensionWrapper>                         list;

    /**
     * Create a new instance of ExtensionController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ExtensionController() {

        map = new HashMap<Class<AbstractExtension>, AbstractExtensionWrapper>();
        list = new ArrayList<AbstractExtensionWrapper>();
        JDController.getInstance().addControlListener(new ControlListener() {

            public void controlEvent(ControlEvent event) {
                if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE) {
                    JDController.getInstance().removeControlListener(this);

                    for (AbstractExtensionWrapper plg : list) {
                        if (plg._getExtension() != null && plg._getExtension().getGUI() != null) {

                            plg._getExtension().getGUI().restore();
                        }
                    }
                }
            }
        });
    }

    public void load() {

        loadUnpacked();
        loadJared();
    }

    @SuppressWarnings("unchecked")
    private void loadJared() {

        File[] addons = Application.getResource("extensions").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (addons == null) return;
        main: for (File jar : addons) {
            try {
                URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURI().toURL() });

                final Enumeration<URL> urls = cl.getResources(AbstractExtension.class.getPackage().getName().replace('.', '/'));
                URL url;
                while (urls.hasMoreElements()) {
                    url = urls.nextElement();
                    if (url.getProtocol().equalsIgnoreCase("jar")) {
                        // jarred addon (JAR)
                        File jarFile = new File(new URL(url.toString().substring(4, url.toString().lastIndexOf('!'))).toURI());
                        final JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
                        JarEntry e;

                        while ((e = jis.getNextJarEntry()) != null) {
                            try {
                                Matcher matcher = Pattern.compile(Pattern.quote(AbstractExtension.class.getPackage().getName().replace('.', '/')) + "/(\\w+)/(\\w+Extension)\\.class").matcher(e.getName());
                                if (matcher.find()) {
                                    String pkg = matcher.group(1);
                                    String clazzName = matcher.group(2);
                                    Class<?> clazz = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + pkg + "." + clazzName);

                                    if (AbstractExtension.class.isAssignableFrom(clazz)) {

                                        initModule((Class<AbstractExtension>) clazz);
                                        continue main;
                                    }

                                }
                            } catch (Throwable e1) {
                                Log.exception(e1);
                            }

                        }
                    }
                }

            } catch (Throwable e) {
                Log.exception(e);
            }

        }

    }

    @SuppressWarnings("unchecked")
    private void loadUnpacked() {
        URL ret = getClass().getResource("/");
        File root;
        if (ret.getProtocol().equalsIgnoreCase("file")) {
            try {
                root = new File(ret.toURI());
            } catch (URISyntaxException e) {
                Log.exception(e);
                Log.L.finer("Did not load unpacked Extensions from " + ret);
                return;
            }
        } else {
            Log.L.finer("Did not load unpacked Extensions from " + ret);
            return;
        }
        root = new File(root, AbstractExtension.class.getPackage().getName().replace('.', '/'));
        Log.L.finer("Load Extensions from: " + root.getAbsolutePath());
        File[] folders = root.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        ClassLoader cl = getClass().getClassLoader();
        main: for (File f : folders) {
            File[] modules = f.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith("Extension.class");
                }
            });
            boolean loaded = false;
            for (File module : modules) {

                Class<?> cls;
                try {
                    cls = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + module.getParentFile().getName() + "." + module.getName().substring(0, module.getName().length() - 6));

                    if (AbstractExtension.class.isAssignableFrom(cls)) {
                        loaded = true;
                        initModule((Class<AbstractExtension>) cls);
                        continue main;
                    }
                } catch (IllegalArgumentException e) {
                    Log.L.warning("Did not init Extension " + module + " : " + e.getMessage());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            if (!loaded) {
                Log.L.warning("Could not load any Extension Module from " + f);
            }
        }

    }

    private void initModule(Class<AbstractExtension> cls) throws InstantiationException, IllegalAccessException, StartException, IOException, ClassNotFoundException {

        AbstractExtensionWrapper cached = getCache(cls);

        if (cached._isEnabled()) {
            cached.init();
        }
        map.put(cls, cached);

        list.add(cached);

    }

    private AbstractExtensionWrapper getCache(Class<AbstractExtension> cls) throws StartException, InstantiationException, IllegalAccessException, IOException {
        File cache = Application.getResource("tmp/extensioncache/" + cls.getName() + ".lng_" + JDT.getLanguage() + ".v" + AbstractExtension.readVersion(cls) + ".json");
        AbstractExtensionWrapper cached = JSonStorage.restoreFrom(cache, true, (byte[]) null, new TypeRef<AbstractExtensionWrapper>() {

        }, null);

        if (cached == null) {
            Log.L.info("Update Cache " + cache);
            cached = AbstractExtensionWrapper.create(cls);
            JSonStorage.saveTo(cache, cached);
        } else {
            cached._setClazz(cls);
        }
        return cached;
    }

    public boolean isExtensionActive(Class<? extends AbstractExtension> class1) {
        AbstractExtensionWrapper plg = map.get(class1);
        if (plg != null && plg._getExtension() != null && plg._getExtension().isEnabled()) return true;
        return false;
    }

    public ArrayList<AbstractExtensionWrapper> getExtensions() {

        return list;
    }

    /**
     * Returns a list of all currently running extensions
     * 
     * @return
     */
    public ArrayList<AbstractExtension> getEnabledExtensions() {
        ArrayList<AbstractExtension> ret = new ArrayList<AbstractExtension>();
        for (AbstractExtensionWrapper aew : list) {
            if (aew._getExtension() != null && aew._getExtension().isEnabled()) ret.add(aew._getExtension());
        }
        return ret;
    }

}
