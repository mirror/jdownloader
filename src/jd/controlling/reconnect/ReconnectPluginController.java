package jd.controlling.reconnect;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.pluginsinc.batch.ExternBatchReconnectPlugin;
import jd.controlling.reconnect.pluginsinc.extern.ExternReconnectPlugin;
import jd.controlling.reconnect.pluginsinc.liveheader.CLRConverter;
import jd.controlling.reconnect.pluginsinc.liveheader.LiveHeaderReconnect;
import jd.controlling.reconnect.pluginsinc.upnp.UPNPRouterPlugin;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.event.ProcessCallBack;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class ReconnectPluginController {
    private static final String                    JD_CONTROLLING_RECONNECT_PLUGINS = "jd/controlling/reconnect/plugins/";

    private static final ReconnectPluginController INSTANCE                         = new ReconnectPluginController();

    public static ReconnectPluginController getInstance() {
        return ReconnectPluginController.INSTANCE;
    }

    private ArrayList<RouterPlugin> plugins;

    private final ReconnectConfig   storage;

    private ReconnectPluginController() {
        this.storage = JsonConfig.create(ReconnectConfig.class);
        this.scan();
    }

    public static void main(String[] args) {
        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.AutoDetectAction_actionPerformed_dooptimization_title(), _GUI._.AutoDetectAction_actionPerformed_dooptimization_msg(1, "ff", "ggf"), NewTheme.I().getIcon("ok", 32), _GUI._.AutoDetectAction_run_optimization(), _GUI._.AutoDetectAction_skip_optimization());
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<ReconnectResult> autoFind(final ProcessCallBack feedback) throws InterruptedException {

        final ArrayList<ReconnectResult> scripts = new ArrayList<ReconnectResult>();

        for (final RouterPlugin plg : ReconnectPluginController.this.plugins) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            try {

                feedback.setStatus(plg, null);
                ArrayList<ReconnectResult> founds = plg.runDetectionWizard(feedback);
                if (founds != null) scripts.addAll(founds);
                if (scripts.size() > 0) break;
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {

            }

        }

        if (JsonConfig.create(ReconnectConfig.class).getOptimizationRounds() > 1 && scripts.size() > 0) {
            int i = 1;
            long bestTime = Long.MAX_VALUE;
            long optiduration = 0;
            for (ReconnectResult found : scripts) {

                bestTime = Math.min(bestTime, found.getSuccessDuration());
                optiduration += found.getSuccessDuration() * (JsonConfig.create(ReconnectConfig.class).getOptimizationRounds() - 1) * 1.5;
            }
            try {

                Dialog.getInstance().showConfirmDialog(0, _GUI._.AutoDetectAction_actionPerformed_dooptimization_title(), _GUI._.AutoDetectAction_actionPerformed_dooptimization_msg(scripts.size(), TimeFormatter.formatMilliSeconds(optiduration, 0), TimeFormatter.formatMilliSeconds(bestTime, 0)), NewTheme.I().getIcon("ok", 32), _GUI._.AutoDetectAction_run_optimization(), _GUI._.AutoDetectAction_skip_optimization());
                feedback.setProgress(this, 0);
                for (int ii = 0; ii < scripts.size(); ii++) {
                    ReconnectResult found = scripts.get(ii);
                    feedback.setStatusString(this, _GUI._.AutoDetectAction_run_optimize(found.getInvoker().getName()));
                    final int step = ii;
                    found.optimize(new ProcessCallBackAdapter() {

                        public void setProgress(Object caller, int percent) {
                            feedback.setProgress(caller, (step) * (100 / scripts.size()) + percent / scripts.size());
                        }

                        public void setStatusString(Object caller, String string) {
                            feedback.setStatusString(caller, _GUI._.AutoDetectAction_run_optimize(string));
                        }

                    });

                }
            } catch (DialogNoAnswerException e) {

            }
        }

        Collections.sort(scripts, new Comparator<ReconnectResult>() {

            public int compare(ReconnectResult o1, ReconnectResult o2) {
                return new Long(o1.getAverageSuccessDuration()).compareTo(new Long(o2.getAverageSuccessDuration()));
            }
        });

        return scripts;
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
     * Performs a reconnect with plugin plg.
     * 
     * @param retry
     * @param plg
     * @return
     * @throws InterruptedException
     * @throws ReconnectException
     */
    public final boolean doReconnect(final RouterPlugin plg, LogSource logger) throws InterruptedException, ReconnectException {
        final int waittime = Math.max(this.getWaittimeBeforeFirstIPCheck(), 0);
        // make sure that we have the current ip
        logger.info("IP Before=" + IPController.getInstance().getIP());
        try {
            ReconnectInvoker invoker = plg.getReconnectInvoker();
            if (invoker == null) { throw new ReconnectException("Reconnect Plugin  " + plg.getName() + " is not set up correctly. Invoker==null"); }
            invoker.setLogger(logger);
            invoker.run();
            logger.finer("Initial Waittime: " + waittime + " seconds");
            Thread.sleep(waittime * 1000);
            return IPController.getInstance().validateAndWait(this.getWaitForIPTime(), Math.max(0, storage.getSecondsToWaitForOffline()), this.getIpCheckInterval());
        } catch (RuntimeException e) {
            logger.log(e);
            throw new ReconnectException(e);
        } finally {
            logger.info("IP AFTER=" + IPController.getInstance().getIP());
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
        int ret = 0;
        if (!storage.isIPCheckGloballyDisabled()) {
            // use own ipcheck if possible
            if (this.getActivePlugin().getIPCheckProvider() != null) {
                ret = this.getActivePlugin().getIPCheckProvider().getIpCheckInterval();
            } else {
                ret = 5;
            }

        }
        // ip check disabled
        return Math.max(ret, 0);
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
        return Math.max(storage.getSecondsToWaitForIPChange(), 0);
    }

    private int getWaittimeBeforeFirstIPCheck() {
        int ret = 0;
        if (!storage.isIPCheckGloballyDisabled()) {
            // use own ipcheck if possible
            if (this.getActivePlugin().getIPCheckProvider() != null) {
                ret = this.getActivePlugin().getWaittimeBeforeFirstIPCheck();
            } else {
                ret = storage.getSecondsBeforeFirstIPCheck();
            }
        }
        // ip check disabled
        return Math.max(ret, 0);
    }

    /**
     * Scans for reconnection plugins
     */
    private void scan() {
        try {
            final File[] files = JDUtilities.getResourceFile("reconnect").listFiles(new JDFileFilter(null, ".reconnect", false));
            this.plugins = new ArrayList<RouterPlugin>();
            this.plugins.add(DummyRouterPlugin.getInstance());
            plugins.add(new ExternBatchReconnectPlugin());
            plugins.add(new ExternReconnectPlugin());
            plugins.add(new UPNPRouterPlugin());
            plugins.add(new LiveHeaderReconnect());

            final ArrayList<URL> urls = new ArrayList<URL>();
            if (files != null) {
                final int length = files.length;

                for (int i = 0; i < length; i++) {
                    try {
                        urls.add(files[i].toURI().toURL());
                        Application.addUrlToClassPath(files[i].toURI().toURL(), getClass().getClassLoader());
                    } catch (final Throwable e) {
                        LogController.CL().log(e);
                    }
                }
            }

            Enumeration<URL> found = getClass().getClassLoader().getResources(ReconnectPluginController.JD_CONTROLLING_RECONNECT_PLUGINS);
            Pattern pattern = Pattern.compile(Pattern.quote(JD_CONTROLLING_RECONNECT_PLUGINS) + "(\\w+)/");
            while (found.hasMoreElements()) {
                URL url = found.nextElement();

                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    // // jarred addon (JAR)
                    String path = url.getPath();
                    File jarFile = new File(new URL(path.substring(0, path.lastIndexOf('!'))).toURI());
                    JarInputStream jis = null;
                    try {
                        jis = new JarInputStream(new FileInputStream(jarFile));
                        JarEntry e;

                        while ((e = jis.getNextJarEntry()) != null) {

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
                    } finally {
                        try {
                            jis.close();
                        } catch (final Throwable e) {
                        }
                    }
                } else {

                    for (File dir : new File(url.toURI()).listFiles(new FileFilter() {

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
            LogController.CL().log(e);
        }

    }

    private void load(String pkg) {
        try {
            URL infourl = Application.getRessourceURL(JD_CONTROLLING_RECONNECT_PLUGINS + pkg + "/info.json");
            if (infourl == null) {
                LogController.CL().finer("Could not load Reconnect Plugin " + pkg);
                return;
            }

            ReconnectPluginInfo plgInfo = JSonStorage.restoreFromString(IO.readURLToString(infourl), new TypeRef<ReconnectPluginInfo>() {
            }, null);
            if (plgInfo == null) {
                LogController.CL().finer("Could not load Reconnect Plugin (no info.json)" + pkg);
                return;
            }
            Class<?> clazz = getClass().getClassLoader().loadClass(JD_CONTROLLING_RECONNECT_PLUGINS.replace("/", ".") + pkg + "." + plgInfo.getClassName());
            for (RouterPlugin plg : plugins) {
                if (plg.getClass() == clazz) {
                    LogController.CL().finer("Dupe found: " + pkg);
                    return;
                }
            }
            plugins.add((RouterPlugin) clazz.newInstance());
        } catch (Throwable e) {
            LogController.CL().log(e);
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
