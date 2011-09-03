package jd.controlling.reconnect;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;

import jd.config.Configuration;
import jd.controlling.CodeVerifier;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.batch.ExternBatchReconnectPlugin;
import jd.controlling.reconnect.plugins.extern.ExternReconnectPlugin;
import jd.controlling.reconnect.plugins.liveheader.CLRConverter;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnect;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.jdownloader.images.NewTheme;

public class ReconnectPluginController {
    private static final String                    JD_CONTROLLING_RECONNECT_PLUGINS = "jd/controlling/reconnect/plugins/";

    private static final ReconnectPluginController INSTANCE                         = new ReconnectPluginController();

    public static ReconnectPluginController getInstance() {
        return ReconnectPluginController.INSTANCE;
    }

    private ArrayList<RouterPlugin> plugins;

    private final ReconnectConfig   storage;

    protected static final Logger   LOG = JDLogger.getLogger();

    private ReconnectPluginController() {
        this.storage = JsonConfig.create(ReconnectConfig.class);

        this.scan();
    }

    public void autoFind() {
        // first try all plugins that have an automode

        final ConfirmDialog cDialog = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.message", "This wizard will help yout to find the correct reconnection settings for your internet connection.\r\nYou can start this wizard at any later time from the file->settings->reconection panel.\r\n\r\nContinue now?"), null, null, null);
        cDialog.setLeftActions(new AbstractAction(JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.more", " ... read more")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                try {

                    CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=16450"));
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            }

        });
        try {
            Dialog.getInstance().showDialog(cDialog);
        } catch (DialogClosedException e2) {
            return;
        } catch (DialogCanceledException e2) {
            return;
        }
        // save plugin to restore it later
        final RouterPlugin restore = this.getActivePlugin();
        final ProgressDialog dialog = new ProgressDialog(new ReconnectWizardProgress() {

            public void run() throws Exception {
                try {

                    ReconnectPluginController.this.setActivePlugin(DummyRouterPlugin.getInstance());
                    // used for progresscontroll only
                    int fastest = Integer.MAX_VALUE;
                    RouterPlugin fastestPlugin = null;
                    this.setProgress(-1);
                    for (final RouterPlugin plg : ReconnectPluginController.this.plugins) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                        if (plg.hasAutoDetection()) {
                            System.err.println("Start " + plg);
                            this.setStatusMessage(JDL.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName()));
                            final int time = plg.runAutoDetection(this);
                            this.setStatusMessage(JDL.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName()));

                            this.setProgress(-1);
                            if (time >= 0 && time < fastest) {
                                fastest = time;
                                fastestPlugin = plg;
                            }
                        }
                    }
                    // if we find a working reconnect without any interaction,
                    // this
                    // is great
                    if (fastestPlugin != null) {
                        ReconnectPluginController.this.setActivePlugin(fastestPlugin);
                        return;
                    }
                    this.setProgress(-1);
                    for (final RouterPlugin plg : ReconnectPluginController.this.plugins) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                        if (plg.hasDetectionWizard()) {
                            System.err.println("Start " + plg);
                            this.setStatusMessage(JDL.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName()));

                            if (plg.runDetectionWizard(this) > 0) {

                                ReconnectPluginController.this.setActivePlugin(plg);
                                return;
                            }
                            this.setStatusMessage(JDL.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressGetter.status", "...testing %s", plg.getName()));

                            this.setProgress(-1);
                        }
                    }
                } catch (Exception e) {
                    throw e;
                }
            }

        }, 0, JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.progressdialog.message", "JDownloader now tries to find the correct settings to perform a reconnect.\r\nThis might take a few minutes. Please be patient!"), null) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void packed() {
                this.setWaitForTermination(1);
            }
        };

        try {
            Dialog.getInstance().showDialog(dialog);
        } catch (DialogClosedException e2) {
            e2.printStackTrace();
        } catch (DialogCanceledException e2) {
            e2.printStackTrace();
        }

        if (this.getActivePlugin() != DummyRouterPlugin.getInstance()) {

            try {
                Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), JDL.LF("jd.controlling.reconnect.ReconnectPluginController.autoFind.success", "Successful!\r\nJDownloader performs the reconnect by using '%s'.", this.getActivePlugin().getName()), NewTheme.I().getIcon("true", 32), null, null);
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        } else {

            this.setActivePlugin(restore);
            final ConfirmDialog conDialog = new ConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.title", "Reconnect Wizard"), JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.failed", "Failed!\r\nJDownloader could not find working reconnect settings for your connection.\r\nPlease contact Support for further help."), NewTheme.I().getIcon("false", 32), null, null);

            conDialog.setLeftActions(new AbstractAction(JDL.L("jd.controlling.reconnect.ReconnectPluginController.autoFind.support", "Support")) {

                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    try {
                        CrossSystem.openURL(new URL("http://support.jdownloader.org/index.php?_m=tickets&_a=submit"));
                    } catch (final MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                }

            });

            try {
                Dialog.getInstance().showDialog(conDialog);
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
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
        System.out.println("IP Before=" + IPController.getInstance().getIP());
        try {
            plg.doReconnect();

            ReconnectPluginController.LOG.finer("Initial Waittime: " + waittime + " seconds");

            Thread.sleep(waittime * 1000);

            return IPController.getInstance().validate(this.getWaitForIPTime(), this.getIpCheckInterval());
        } finally {
            System.out.println("IP AFTER=" + IPController.getInstance().getIP());
        }

    }

    /**
     * returns the currently active routerplugin. Only one plugin may be active
     * 
     * @return
     */
    public RouterPlugin getActivePlugin() {
        // convert only once
        String id = storage.getActivePluginID();
        if (id == null) {
            id = this.convertFromOldSystem();
            this.storage.setActivePluginID(id);
        }
        RouterPlugin active = ReconnectPluginController.getInstance().getPluginByID(id);
        if (active == null) {
            active = DummyRouterPlugin.getInstance();
            this.storage.setActivePluginID(active.getID());
        }
        return active;
    }

    /**
     * returns how long the controller has to wait between two ip checks
     * 
     * @return
     */
    private int getIpCheckInterval() {
        if (!storage.isIPCheckGloballyDisabled()) {
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

    private int getWaitForIPTime() {
        return storage.getSecondsToWaitForIPChange();
    }

    private int getWaittimeBeforeFirstIPCheck() {

        if (!storage.isIPCheckGloballyDisabled()) {

            // use own ipcheck if possible
            if (this.getActivePlugin().getIPCheckProvider() != null) { return this.getActivePlugin().getWaittimeBeforeFirstIPCheck(); }
            return storage.getSecondsBeforeFirstIPCheck();

        }
        // ip check disabled
        return 0;
    }

    /**
     * Scans for reconnection plugins
     */
    private void scan() {
        try {
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
                            Application.addUrlToClassPath(files[i].toURI().toURL(), getClass().getClassLoader());
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                    } catch (final NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }

            Enumeration<URL> found = getClass().getClassLoader().getResources(ReconnectPluginController.JD_CONTROLLING_RECONNECT_PLUGINS);
            Pattern pattern = Pattern.compile(Pattern.quote(JD_CONTROLLING_RECONNECT_PLUGINS) + "(\\w+)/");
            while (found.hasMoreElements()) {
                URL url = found.nextElement();

                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    // // jarred addon (JAR)
                    File jarFile = new File(new URL(url.toString().substring(4, url.toString().lastIndexOf('!'))).toURI());
                    final JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));

                    JarEntry e;

                    while ((e = jis.getNextJarEntry()) != null) {
                        System.out.println(e.getName());

                        // try {
                        Matcher matcher = pattern.matcher(e.getName());
                        while (matcher.find()) {
                            try {
                                String pkg = matcher.group(1);
                                load(pkg);

                                System.out.println(pkg);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                } else {
                    System.out.println(url);
                    for (File dir : new File(url.toString().substring(6)).listFiles(new FileFilter() {

                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    })) {
                        File file = new File(dir, "info.json");
                        if (file.exists()) {
                            load(dir.getName());
                        }
                    }
                    //
                }
            }
        } catch (Throwable e) {
            Log.exception(e);
        }

    }

    private void load(String pkg) {
        try {
            URL infourl = Application.getRessourceURL(JD_CONTROLLING_RECONNECT_PLUGINS + pkg + "/info.json");
            if (infourl == null) {
                Log.L.finer("Could not load Reconnect Plugin " + pkg);
                return;
            }

            ReconnectPluginInfo plgInfo = JSonStorage.restoreFromString(IO.readURLToString(infourl), new TypeRef<ReconnectPluginInfo>() {
            }, null);
            if (plgInfo == null) {
                Log.L.finer("Could not load Reconnect Plugin (no info.json)" + pkg);
                return;
            }
            Class<?> clazz = getClass().getClassLoader().loadClass(JD_CONTROLLING_RECONNECT_PLUGINS.replace("/", ".") + pkg + "." + plgInfo.getClassName());
            for (RouterPlugin plg : plugins) {
                if (plg.getClass() == clazz) {
                    Log.L.finer("Dupe found: " + pkg);
                    return;
                }
            }
            plugins.add((RouterPlugin) clazz.newInstance());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Sets the active reconnect plugin
     * 
     * @param selectedItem
     */
    public void setActivePlugin(final RouterPlugin selectedItem) {
        this.storage.setActivePluginID(selectedItem.getID());
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
