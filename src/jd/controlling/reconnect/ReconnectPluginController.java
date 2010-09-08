package jd.controlling.reconnect;

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

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.CodeVerifier;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.plugins.batch.ExternBatchReconnectPlugin;
import jd.controlling.reconnect.plugins.extern.ExternReconnectPlugin;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnect;
import jd.gui.UserIF;
import jd.nutils.IPAddress;
import jd.nutils.io.JDFileFilter;
import jd.utils.CLRLoader;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.logging.Log;

import com.sun.istack.internal.Nullable;

public class ReconnectPluginController {
    public static final String                     PRO_ACTIVEPLUGIN = "ACTIVEPLUGIN2";

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
     * Maps old reconnect panel, to new one. can be removed after 2.*
     * 
     * @return
     */
    /*
     * 
     * 
     * public static final String PARAM_RECONNECT_TYPE = "RECONNECT_TYPE";
     * 
     * public static final int LIVEHEADER = 0; public static final int EXTERN =
     * 1; public static final int BATCH = 2; public static final int CLR = 3;
     * public static final int PLUGIN = 4;
     */
    private String convertFromOldSystem() {
        final int id = JDUtilities.getConfiguration().getIntegerProperty("RECONNECT_TYPE", 0);
        String[] ret;
        switch (id) {
        case 0:
            return LiveHeaderReconnect.ID;
        case 1:
            return ExternReconnectPlugin.ID;
        case 2:
            return ExternBatchReconnectPlugin.ID;
        case 3:
            // we need to convert clr script

            final String clr = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR);
            ret = CLRLoader.createLiveHeader(clr);

            if (ret != null) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, ret[1]);

            }
            return LiveHeaderReconnect.ID;

        }
        return DummyRouterPlugin.getInstance().getID();
    }

    /**
     * Performs a reconnect
     * 
     * @return true if ip changed
     */
    public synchronized final boolean doReconnect() {

        final RouterPlugin active = this.getActivePlugin();
        if (active == DummyRouterPlugin.getInstance()) { return false; }
        final ProgressController progress = new ProgressController(this.toString(), 10, "gui.images.reconnect");
        try {
            progress.increase(4);
            progress.setStatusText(JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginController.doReconnect_1", "Reconnect #") + 1);
            int retry;
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                /*
                 * disabled ipcheck, let run 1 reconnect round and guess it has
                 * been successful
                 */

                this.doReconnect(this.getActivePlugin());
                progress.setStatusText(JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginController.doReconnect_2", "Reconnection successfull"));

                return true;
            } else {
                int maxretries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RETRIES, 5);
                boolean ret = false;
                retry = 0;
                if (maxretries < 0) {
                    maxretries = Integer.MAX_VALUE;
                } else if (maxretries == 0) {
                    maxretries = 1;
                }
                progress.setRange(maxretries + 10, 10);
                for (retry = 0; retry < maxretries; retry++) {
                    ReconnectPluginController.LOG.info("Starting " + this.toString() + " #" + (retry + 1));
                    progress.increase(1);
                    progress.setStatusText(JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginController.doReconnect_1", "Reconnect #") + (retry + 1));
                    if ((ret = this.doReconnect(this.getActivePlugin())) == true) {
                        break;
                    }
                }
                return ret;
            }
        } finally {
            progress.doFinalize(1000);
        }
    }

    /**
     * Performs a reconnect with plugin plg.
     * 
     * @param retry
     * @param plg
     * @return
     */
    public final boolean doReconnect(final RouterPlugin plg) {

        final Configuration configuration = JDUtilities.getConfiguration();
        final int waitForIp = configuration.getIntegerProperty(Configuration.PARAM_WAITFORIPCHANGE, 30);
        final int checkInterval = this.getIpCheckInterval();

        final int waittime = this.getWaittimeBeforeFirstIPCheck();

        final String preIp = this.getExternalIP();

        try {
            plg.doReconnect();
        } catch (final Exception e) {

            e.printStackTrace();
            ReconnectPluginController.LOG.severe("An error occured while processing the reconnect ... Terminating");
            return false;
        }

        ReconnectPluginController.LOG.finer("Initial Waittime: " + waittime + " seconds");
        try {
            Thread.sleep(waittime * 1000);
        } catch (final InterruptedException e) {
        }
        boolean offline = false;
        String afterIP = this.getExternalIP();
        if (preIp != RouterPlugin.OFFLINE && preIp != RouterPlugin.NOT_AVAILABLE && (afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
            // connection is offline now.
            offline = true;
            ReconnectPluginController.LOG.finer("OFFLINE NOW");
        }
        long endTime = System.currentTimeMillis() + waitForIp * 1000;
        ReconnectPluginController.LOG.info("Wait " + waitForIp + " sec for new ip");
        while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {

            try {
                Thread.sleep(checkInterval * 1000);
            } catch (final InterruptedException e) {
            }
            afterIP = this.getExternalIP();
            ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
            if (!offline && preIp != RouterPlugin.OFFLINE && preIp != RouterPlugin.NOT_AVAILABLE && (afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
                // connection is offline now.
                offline = true;
                ReconnectPluginController.LOG.finer("OFFLINE NOW");
            }
            if (offline && preIp != RouterPlugin.OFFLINE && preIp != RouterPlugin.NOT_AVAILABLE && afterIP.equals(preIp)) {
                ReconnectPluginController.LOG.finer("Has been offline and is now online with same IP. Fail");
                return false;
            }

        }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) { return true; }
        ReconnectPluginController.LOG.finer("IP before: " + preIp + " after: " + afterIP);
        if ((afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE) && !afterIP.equals(preIp)) {
            ReconnectPluginController.LOG.warning("JD could disconnect your router, but could not connect afterwards. Try to rise the option 'Wait until first IP Check'");
            endTime = System.currentTimeMillis() + 120 * 1000;
            while (System.currentTimeMillis() <= endTime && (afterIP.equals(preIp) || afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {

                try {
                    Thread.sleep(checkInterval * 1000);
                } catch (final InterruptedException e) {
                }
                afterIP = this.getExternalIP();
                ReconnectPluginController.LOG.finer("IP before2: " + preIp + " after: " + afterIP);
                if (!offline && preIp != RouterPlugin.OFFLINE && preIp != RouterPlugin.NOT_AVAILABLE && (afterIP == RouterPlugin.OFFLINE || afterIP == RouterPlugin.NOT_AVAILABLE)) {
                    // connection is offline now.
                    offline = true;
                    ReconnectPluginController.LOG.finer("OFFLINE NOW");
                }
                if (offline && preIp != RouterPlugin.OFFLINE && preIp != RouterPlugin.NOT_AVAILABLE && afterIP.equals(preIp)) {
                    ReconnectPluginController.LOG.finer("Has been offline and is now online with same IP. Fail");
                    return false;
                }
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

                Reconnecter.setCurrentIP(afterIP);
                return true;
            }
        }
        return false;

    }

    /**
     * returns the currently active routerplugin. Only one plugin may be active
     * 
     * @return
     */
    public RouterPlugin getActivePlugin() {
        // convert only once
        String id = this.storage.get(ReconnectPluginController.PRO_ACTIVEPLUGIN, null);
        if (id == null) {
            id = this.convertFromOldSystem();
            this.storage.put(ReconnectPluginController.PRO_ACTIVEPLUGIN, id);
        }
        RouterPlugin active = ReconnectPluginController.getInstance().getPluginByID(id);
        if (active == null) {
            active = DummyRouterPlugin.getInstance();
            this.storage.put(ReconnectPluginController.PRO_ACTIVEPLUGIN, active.getID());
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
     * returns how long the controller has to wait between two ip checks
     * 
     * @return
     */
    private int getIpCheckInterval() {
        if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            // use own ipcheck if possible
            if (this.getActivePlugin().isIPCheckEnabled()) { return this.getActivePlugin().getIpCheckInterval(); }
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
            return JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_IPCHECKWAITTIME, 5);

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
                    e.printStackTrace();
                } catch (final NoSuchAlgorithmException e) {
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

    /**
     * Sets the active reconnect plugin
     * 
     * @param id
     */
    public void setActivePlugin(final RouterPlugin selectedItem) {
        this.storage.put(ReconnectPluginController.PRO_ACTIVEPLUGIN, selectedItem.getID());

    }

    /**
     * Sets the active reconnect plugin
     * 
     * @param id
     */
    public void setActivePlugin(final String id) {
        this.setActivePlugin(this.getPluginByID(id));

    }
}
