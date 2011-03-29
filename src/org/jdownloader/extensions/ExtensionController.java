package org.jdownloader.extensions;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
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
import jd.plugins.optional.PluginOptional;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;

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

    private HashMap<Class<PluginOptional>, PluginOptional> map;
    private ArrayList<PluginOptional>                      list;

    /**
     * Create a new instance of ExtensionController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ExtensionController() {

        map = new HashMap<Class<PluginOptional>, PluginOptional>();
        list = new ArrayList<PluginOptional>();
        JDController.getInstance().addControlListener(new ControlListener() {

            public void controlEvent(ControlEvent event) {
                if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE) {
                    JDController.getInstance().removeControlListener(this);

                    for (PluginOptional plg : list) {
                        if (plg.getGUI() != null) {

                            plg.getGUI().restore();
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

                final Enumeration<URL> urls = cl.getResources(PluginOptional.class.getPackage().getName().replace('.', '/'));
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
                                Matcher matcher = Pattern.compile(Pattern.quote(PluginOptional.class.getPackage().getName().replace('.', '/')) + "/(\\w+)/(\\w+Extension)\\.class").matcher(e.getName());
                                if (matcher.find()) {
                                    String pkg = matcher.group(1);
                                    String clazzName = matcher.group(2);
                                    Class<?> clazz = cl.loadClass(PluginOptional.class.getPackage().getName() + "." + pkg + "." + clazzName);

                                    if (PluginOptional.class.isAssignableFrom(clazz)) {

                                        initModule((Class<PluginOptional>) clazz);
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
        root = new File(root, PluginOptional.class.getPackage().getName().replace('.', '/'));
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
                    cls = cl.loadClass(PluginOptional.class.getPackage().getName() + "." + module.getParentFile().getName() + "." + module.getName().substring(0, module.getName().length() - 6));

                    if (PluginOptional.class.isAssignableFrom(cls)) {
                        loaded = true;
                        initModule((Class<PluginOptional>) cls);
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

    private void initModule(Class<PluginOptional> cls) throws InstantiationException, IllegalAccessException, StartException {

        PluginOptional plg = (PluginOptional) cls.newInstance();
        plg.init();

        if (!plg.isWindowsRunnable() && CrossSystem.isWindows()) throw new IllegalArgumentException("Module is not for windows");
        if (!plg.isMacRunnable() && CrossSystem.isMac()) throw new IllegalArgumentException("Module is not for mac");
        if (!plg.isLinuxRunnable() && CrossSystem.isLinux()) throw new IllegalArgumentException("Module is not for linux");

        map.put(cls, plg);

        list.add(plg);

    }

    public boolean isExtensionActive(Class<? extends PluginOptional> class1) {
        PluginOptional plg = map.get(class1);
        if (plg != null && plg.isRunning()) return true;
        return false;
    }

    public ArrayList<PluginOptional> getExtensions() {

        return list;
    }

}
