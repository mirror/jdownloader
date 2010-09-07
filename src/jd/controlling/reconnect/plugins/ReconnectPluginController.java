package jd.controlling.reconnect.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.CodeVerifier;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UserIF;
import jd.nrouter.IPCheck;
import jd.nutils.IPAddress;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.logging.Log;

import com.sun.istack.internal.Nullable;

public class ReconnectPluginController {
    public static final String                     PRO_ACTIVEPLUGIN = "ACTIVEPLUGIN";

    private static final ReconnectPluginController INSTANCE         = new ReconnectPluginController();

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

    protected static final Logger   LOG = JDLogger.getLogger();

    private ReconnectPluginController() {
        this.storage = JSonStorage.getStorage("ReconnectPluginController");
        this.scan();

    }

    /**
     * Activates Special reconnect for the givven reconnect plugin
     * 
     * @param upnpRouterPlugin
     */
    public void activatePluginReconnect(final RouterPlugin plg) {
        this.setActivePlugin(plg);
        JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.PLUGIN);
        JDUtilities.getConfiguration();
    }

    /**
     * Performs a reconnect
     * 
     * @return true if ip changed
     */
    public final boolean doReconnect() {
        final RouterPlugin active = this.getActivePlugin();
        if (active == DummyRouterPlugin.getInstance()) { return false; }

        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            /*
             * disabled ipcheck, let run 1 reconnect round and guess it has been
             * successful
             */
            this.doReconnectInternal(1);

            return true;
        } else {
            int maxretries = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RETRIES, 5);
            boolean ret = false;
            int retry = 0;
            if (maxretries < 0) {
                maxretries = Integer.MAX_VALUE;
            } else if (maxretries == 0) {
                maxretries = 1;
            }
            for (retry = 0; retry < maxretries; retry++) {
                if ((ret = this.doReconnectInternal(retry + 1)) == true) {
                    break;
                }
            }
            return ret;
        }
    }

    public final boolean doReconnectInternal(final int retry) {
        final ProgressController progress = new ProgressController(this.toString(), 10, "gui.images.reconnect");
        progress.setStatusText(JDL.L("reconnect.progress.1_retries", "Reconnect #") + retry);
        try {
            final Configuration configuration = JDUtilities.getConfiguration();
            final int waitForIp = configuration.getIntegerProperty(ReconnectMethod.PARAM_WAITFORIPCHANGE, 30);
            final int checkInterval = this.getIpCheckInterval();

            final int waittime = this.getWaittimeBeforeFirstIPCheck();

            ReconnectPluginController.LOG.info("Starting " + this.toString() + " #" + retry);
            final String preIp = this.getExternalIP();

            progress.increase(1);
            progress.setStatusText(JDL.L("reconnect.progress.2_oldIP", "Reconnect Old IP:") + preIp);

            try {
                this.getActivePlugin().doReconnect(progress);
            } catch (final Exception e) {
                progress.doFinalize();
                e.printStackTrace();
                ReconnectPluginController.LOG.severe("An error occured while processing the reconnect ... Terminating");
                return false;
            }

            ReconnectPluginController.LOG.finer("Initial Waittime: " + waittime + " seconds");
            try {
                Thread.sleep(waittime * 1000);
            } catch (final InterruptedException e) {
            }
            String afterIP = this.getExternalIP();
            progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
            long endTime = System.currentTimeMillis() + waitForIp * 1000;
            ReconnectPluginController.LOG.info("Wait " + waitForIp + " sec for new ip");
            while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
                ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                try {
                    Thread.sleep(checkInterval * 1000);
                } catch (final InterruptedException e) {
                }
                afterIP = this.getExternalIP();
                progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", afterIP, preIp));
            }
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) { return true; }
            ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
            if ((afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE) && !afterIP.equals(preIp)) {
                ReconnectPluginController.LOG.warning("JD could disconnect your router, but could not connect afterwards. Try to rise the option 'Wait until first IP Check'");
                endTime = System.currentTimeMillis() + 120 * 1000;
                while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
                    ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (final InterruptedException e) {
                    }
                    afterIP = this.getExternalIP();
                    progress.setStatusText(JDL.LF("reconnect.progress.3_ipcheck", "Reconnect New IP: %s / %s", preIp, afterIP));
                }
            }

            if (!afterIP.equals(preIp) && !(afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
                ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
                /* Reconnect scheint erfolgreich gewesen zu sein */
                /* nun IP validieren */
                if (!IPAddress.validateIP(afterIP)) {
                    ReconnectPluginController.LOG.warning("IP " + afterIP + " was filtered by mask: " + SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"));
                    UserIF.getInstance().displayMiniWarning(JDL.L("reconnect.ipfiltered.warning.title", "Wrong IP!"), JDL.LF("reconnect.ipfiltered.warning.short", "The IP %s is not allowed!", afterIP));
                    Reconnecter.setCurrentIP(RouterPlugin.NOT_AVAILABLE);
                    return false;
                } else {
                    progress.doFinalize();
                    Reconnecter.setCurrentIP(afterIP);
                    return true;
                }
            }
            return false;
        } finally {
            progress.doFinalize();
        }
    }

    /**
     * returns the currently active routerplugin. Only one plugin may be active
     * 
     * @return
     */
    public RouterPlugin getActivePlugin() {

        RouterPlugin active = ReconnectPluginController.getInstance().getPluginByID(this.storage.get(ReconnectPluginController.PRO_ACTIVEPLUGIN, DummyRouterPlugin.getInstance().getID()));
        if (active == null) {
            active = DummyRouterPlugin.getInstance();
        }
        return active;
    }

    /**
     * Returns the current external IP. Checks the current active plugin, and
     * uses IPCHeck class as fallback
     * 
     * @return
     */
    public String getExternalIP() {
        String ret = RouterPlugin.NOT_AVAILABLE;

        cond: if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {

            // use own ipcheck if possible
            if (this.getActivePlugin().isIPCheckEnabled()) {
                try {
                    ret = ReconnectPluginController.getInstance().getActivePlugin().getExternalIP();
                    break cond;
                } catch (final Throwable e) {

                    this.getActivePlugin().setCanCheckIP(false);
                    Log.exception(e);
                }

            }
            ret = IPCheck.getIPAddress();
        }
        Reconnecter.setCurrentIP(ret);
        return ret;
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

    /**
     * returns how long the controller has to wait between two ip checks
     * 
     * @return
     */
    private int getIpCheckInterval() {
        if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {

            // use own ipcheck if possible
            if (this.getActivePlugin().isIPCheckEnabled()) { return this.getActivePlugin().getIpCheckInterval();

            }
            return 5;

        }
        // ip check disabled
        return 0;
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

    /**
     * returns the Storagemodel
     * 
     * @return
     */
    public Storage getStorage() {
        return this.storage;
    }

    private int getWaittimeBeforeFirstIPCheck() {
        if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {

            // use own ipcheck if possible
            if (this.getActivePlugin().isIPCheckEnabled()) { return this.getActivePlugin().getWaittimeBeforeFirstIPCheck();

            }
            return JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_IPCHECKWAITTIME, 5);

        }
        // ip check disabled
        return 0;
    }

    /**
     * Scans for reconnection plugins
     */
    private void scan() {
        final File[] files = JDUtilities.getResourceFile("reconnect").listFiles(new JDFileFilter(null, ".rec", false));
        this.plugins = new ArrayList<RouterPlugin>();
        this.plugins.add(DummyRouterPlugin.getInstance());
        final ArrayList<URL> urls = new ArrayList<URL>();
        if (files != null) {
            final int length = files.length;

            for (int i = 0; i < length; i++) {
                try {
                    if (CodeVerifier.getInstance().isJarAllowed(files[i])) {
                        urls.add(files[i].toURI().toURL());
                    }
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
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

    public void setActivePlugin(final RouterPlugin selectedItem) {
        this.storage.put(ReconnectPluginController.PRO_ACTIVEPLUGIN, selectedItem.getID());

    }
}
