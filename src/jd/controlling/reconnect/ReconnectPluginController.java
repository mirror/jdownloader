package jd.controlling.reconnect;

import java.awt.event.ActionEvent;
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

import javax.swing.AbstractAction;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.CodeVerifier;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.batch.ExternBatchReconnectPlugin;
import jd.controlling.reconnect.plugins.extern.ExternReconnectPlugin;
import jd.controlling.reconnect.plugins.liveheader.CLRConverter;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnect;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.locale.Loc;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;

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

    public void autoFind() {
        // first try all plugins that have an automode
        this.storage.put("AUTOFIND", true);

        final ConfirmDialog cDialog = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.message", "This wizard will help yout to find the correct reconnection settings for your internet connection.\r\nYou can start this wizard at any later time from the file->settings->reconection panel.\r\n\r\nContinue now?"), null, null, null);
        cDialog.setLeftActions(new AbstractAction(Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.more", " ... read more")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                try {

                    CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=16450"));
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }

        });
        if (!Dialog.isOK(Dialog.getInstance().showDialog(cDialog))) { return; }
        // save plugin to restore it later
        final RouterPlugin restore = this.getActivePlugin();
        final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

            private String statusMessage;

            public int getProgress() {
                return -1;
            }

            public String getString() {
                return this.statusMessage;
            }

            public void run() throws Exception {

                ReconnectPluginController.this.setActivePlugin(DummyRouterPlugin.getInstance());
                // used for progresscontroll only
                int fastest = Integer.MAX_VALUE;
                RouterPlugin fastestPlugin = null;

                for (final RouterPlugin plg : ReconnectPluginController.this.plugins) {
                    if (plg.hasAutoDetection()) {
                        this.statusMessage = Loc.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName());
                        final int time = plg.autoDetection();
                        if (time >= 0 && time < fastest) {
                            fastest = time;
                            fastestPlugin = plg;
                        }
                    }
                }
                // if we find a working reconnect without any interaction, this
                // is great
                if (fastestPlugin != null) {
                    ReconnectPluginController.this.setActivePlugin(fastestPlugin);
                    return;
                }
                for (final RouterPlugin plg : ReconnectPluginController.this.plugins) {
                    if (!plg.hasAutoDetection() && plg.hasDetectionWizard()) {
                        if (plg.runDetectionWizard() > 0) {
                            this.statusMessage = Loc.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName());

                            ReconnectPluginController.this.setActivePlugin(plg);
                            return;
                        }
                    }
                }

            }

        }, 0, Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressdialog.message", "JDownloader now tries to find the correct settings to perform a reconnect.\r\nThis might take a few minutes. Please be patient!"), null);

        Dialog.getInstance().showDialog(dialog);

        if (this.getActivePlugin() != DummyRouterPlugin.getInstance()) {

            Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), Loc.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.success", "Successfull!\r\nJDownloader performs the reconnect by using '%s'.", this.getActivePlugin().getName()), JDTheme.II("gui.images.ok", 32, 32), null, null);
        } else {

            this.setActivePlugin(restore);
            final ConfirmDialog conDialog = new ConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.failed", "Failed!\r\nJDownloader could not find working reconnect settings for your connection.\r\nPlease contact Support for further help."), JDTheme.II("gui.images.bad", 32, 32), null, null);

            conDialog.setLeftActions(new AbstractAction(Loc.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.support", "Support")) {

                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    try {
                        CrossSystem.openURL(new URL("http://support.jdownloader.org/index.php?_m=tickets&_a=submit"));
                    } catch (final MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                }

            });

            Dialog.getInstance().showDialog(conDialog);
        }
    }

    /**
     * Maps old reconnect panel, to new one. can be removed after 2.*
     * 
     * @return
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
            ret = CLRConverter.createLiveHeader(clr);

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
     * @throws InterruptedException
     * @throws ReconnectException
     */
    public synchronized final boolean doReconnect() throws InterruptedException, ReconnectException {

        final RouterPlugin active = this.getActivePlugin();
        if (active == DummyRouterPlugin.getInstance()) { throw new ReconnectException("Invalid Plugin"); }
        return this.doReconnect(this.getActivePlugin());

    }

    /**
     * Performs a reconnect with plugin plg.
     * 
     * @param retry
     * @param plg
     * @return
     * @throws InterruptedException
     * @throws ReconnectException
     */
    public final boolean doReconnect(final RouterPlugin plg) throws InterruptedException, ReconnectException {

        final int waittime = this.getWaittimeBeforeFirstIPCheck();
        // make sure that we have the current ip
        IPController.getInstance().getIP();
        plg.doReconnect();

        ReconnectPluginController.LOG.finer("Initial Waittime: " + waittime + " seconds");

        Thread.sleep(waittime * 1000);

        return IPController.getInstance().validate(this.getWaitForIPTime(), this.getIpCheckInterval());

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
     * returns how long the controller has to wait between two ip checks
     * 
     * @return
     */
    private int getIpCheckInterval() {
        if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            // use own ipcheck if possible
            if (this.getActivePlugin().getIPCheckProvider() != null) { return this.getActivePlugin().getIPCheckProvider().getIpCheckInterval(); }
            return 5;
        }
        // ip check disabled
        return 0;
    }

    /**
     * Returns the plugin that has the given ID.
     */
    @Nullable
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

    private int getWaitForIPTime() {
        return JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_WAITFORIPCHANGE, 30);
    }

    private int getWaittimeBeforeFirstIPCheck() {
        if (!SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {

            // use own ipcheck if possible
            if (this.getActivePlugin().getIPCheckProvider() != null) { return this.getActivePlugin().getWaittimeBeforeFirstIPCheck(); }
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
     * @param selectedItem
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
