package jd.controlling.reconnect.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.JComponent;

import jd.controlling.ProgressController;
import jd.controlling.reconnect.ReconnectMethod;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.logging.Log;

import com.sun.istack.internal.Nullable;

public class ReconnectPluginController extends ReconnectMethod {
    private static final ReconnectPluginController INSTANCE = new ReconnectPluginController();

    private static List<Class<?>> findPlugins(final URL directory, final String packageName, final ClassLoader classLoader) throws ClassNotFoundException {
        final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        File[] files = null;

        try {
            files = new File(directory.toURI().getPath()).listFiles();
        } catch (final Exception e) {
        }

        if (files == null) {
            try {
                // it's a jar
                final String path = directory.toString().substring(4);
                // split path | intern path
                final String[] splitted = path.split("!");

                splitted[1] = splitted[1].substring(1);

                final JarInputStream jarFile = new JarInputStream(new FileInputStream(new File(new URL(splitted[0]).toURI())));
                JarEntry e;

                String jarName;
                while ((e = jarFile.getNextJarEntry()) != null) {
                    jarName = e.getName();
                    if (jarName.startsWith(splitted[1]) && jarName.endsWith(".class")) {
                        final Class<?> c = classLoader.loadClass(jarName.substring(0, jarName.length() - 6).replace("/", "."));
                        final boolean a = RouterPlugin.class.isAssignableFrom(c);

                        if (c != null && a) {
                            classes.add(c);
                        }
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        } else {
            String fileName = null;
            for (final File file : files) {
                try {
                    fileName = file.getName();
                    if (file.isDirectory()) {
                        try {
                            classes.addAll(ReconnectPluginController.findPlugins(file.toURI().toURL(), packageName + "." + fileName, classLoader));
                        } catch (final MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } else if (fileName.endsWith(".class")) {
                        final Class<?> c = classLoader.loadClass(packageName + '.' + fileName.substring(0, fileName.length() - 6));
                        if (c != null && RouterPlugin.class.isAssignableFrom(c) && !packageName.endsWith(".plugins")) {
                            classes.add(c);
                        }
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }

    public static ReconnectPluginController getInstance() {
        return ReconnectPluginController.INSTANCE;
    }

    private ArrayList<RouterPlugin> plugins;

    private final Storage           storage;

    private ReconnectPluginController() {
        this.storage = JSonStorage.getStorage("ReconnectPluginController");
        this.scan();

    }

    /**
     * returns the currently active routerplugin. Only one plugin may be active
     * 
     * @return
     */
    public RouterPlugin getActivePlugin() {

        RouterPlugin active = ReconnectPluginController.getInstance().getPluginByID(this.storage.get("ACTIVEPLUGIN", DummyRouterPlugin.getInstance().getID()));
        if (active == null) {
            active = DummyRouterPlugin.getInstance();
        }
        return active;
    }

    /**
     * returns the gui panel. This mopdule uses the new appwork JSonStorage and
     * does not need to use teh old ConfigPanel System.
     * 
     * @return
     */
    public JComponent getGUI() {
        // TODO Auto-generated method stub
        return ReconnectPluginConfigGUI.getInstance();
    }

    @Nullable
    /**
     * Returns the plugin that has the given ID.
     */
    public RouterPlugin getPluginByID(final String activeID) {
        for (final RouterPlugin plg : this.plugins) {
            if (plg.getID().equals(activeID)) { return plg; }
        }
        return null;
    }

    /**
     * Returns all registered Plugins
     * 
     * @return
     */
    public ArrayList<RouterPlugin> getPlugins() {

        return this.plugins;
    }

    @Override
    protected void initConfig() {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean runCommands(final ProgressController progress) {
        final RouterPlugin active = this.getActivePlugin();
        if (active == DummyRouterPlugin.getInstance()) { return false; }
        return active.doReconnect(progress);
    }

    /**
     * Scans for reconnection plugins
     */
    private void scan() {
        final File[] files = JDUtilities.getResourceFile("reconnect").listFiles(new JDFileFilter(null, ".rec", false));
        this.plugins = new ArrayList<RouterPlugin>();
        this.plugins.add(DummyRouterPlugin.getInstance());
        if (files != null) {
            final File file;
            final String name;
            final String absolutePath;

            final int length = files.length;
            final ArrayList<URL> urls = new ArrayList<URL>();
            for (int i = 0; i < length; i++) {
                try {
                    urls.add(files[i].toURI().toURL());
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[] {}));
            try {
                final Enumeration<URL> resources = classLoader.getResources("jd/controlling/reconnect/plugins/");

                final HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();
                while (resources.hasMoreElements()) {

                    classloop: for (final Class<?> c : ReconnectPluginController.findPlugins(resources.nextElement(), "jd.controlling.reconnect.plugins", classLoader)) {

                        if (classes.containsKey(c.getName())) {
                            continue;
                        }
                        // in Eclipse, we often load class files from classptha
                        // AND the jars. classfiles are loaded before jars. we
                        // always use the FIRST class found.

                        classes.put(c.getName(), c);
                        final RouterPlugin plg = (RouterPlugin) c.newInstance();
                        // do not add two different plugins with the same ID
                        for (final RouterPlugin p : this.plugins) {
                            if (p.getID().equals(plg)) {
                                Log.L.severe("Tried to add two plugins with ID: " + p.getID());
                                continue classloop;
                            }
                        }
                        this.plugins.add(plg);
                    }

                }

            } catch (final Exception e) {
                e.printStackTrace();
            }

        }

    }

    public void setActivePlugin(final RouterPlugin selectedItem) {
        this.storage.put("ACTIVEPLUGIN", selectedItem.getID());

    }
}
