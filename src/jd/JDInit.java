//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.controlling.DownloadController;
import jd.controlling.GarbageController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.http.HTTPProxy;
import jd.http.ext.security.JSPermissionRestricter;
import jd.nutils.ClassFinder;
import jd.nutils.OSDetector;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.pluginloader.DecrypterPluginCache;
import jd.pluginloader.HosterPluginCache;
import jd.pluginloader.VirtualClass;
import jd.pluginloader.VirtualDecrypterClass;
import jd.pluginloader.VirtualHosterClass;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.update.updateclient.UpdaterConstants;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.lobobrowser.util.OS;

/**
 * @author JD-Team
 */

public class JDInit {
    static {
        try {
            JSPermissionRestricter.init();
        } catch (final Throwable e) {
            e.printStackTrace();
        }

    }

    private static final boolean TEST_INSTALLER = false;

    private static final Logger  LOG            = JDLogger.getLogger();

    private static ClassLoader   CL;

    /**
     * Returns a classloader to load plugins (class files); Depending on runtype
     * (dev or local jared) a different classoader is used to load plugins
     * either from installdirectory or from rundirectory
     * 
     * @return
     */
    public static ClassLoader getPluginClassLoader() {
        if (JDInit.CL == null) {
            try {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    try {
                        System.out.println(JDUtilities.getResourceFile("java").toURI().toURL());
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                    JDInit.CL = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL() }, Thread.currentThread().getContextClassLoader());
                } else {
                    JDInit.CL = Thread.currentThread().getContextClassLoader();
                }
            } catch (final MalformedURLException e) {
                JDLogger.exception(e);
            }
        }
        return JDInit.CL;
    }

    public static void loadPluginForDecrypt() {

        try {
            for (final VirtualClass c : ClassFinder.getClasses("jd.plugins.decrypter", JDInit.getPluginClassLoader())) {
                try {
                    VirtualDecrypterClass vc = VirtualDecrypterClass.create(c);
                    if (!vc.isValid()) {
                        continue;
                    }

                    vc.initWrapper();

                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (final Throwable e) {
            JDLogger.exception(e);
        } finally {
            try {
                DecrypterPluginCache.getInstance().save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadPluginForHost() {
        try {
            // find all classfiles in package jd.plugins.hoster
            for (final VirtualClass c : ClassFinder.getClasses("jd.plugins.hoster", JDInit.getPluginClassLoader())) {
                try {
                    // create a virtualhoster classfile.
                    VirtualHosterClass vc = VirtualHosterClass.create(c);
                    // ignore it if it is not valid. outdated or anything like
                    // hits
                    if (!vc.isValid()) {
                        continue;
                    }

                    // initclass. create Hostwrapper
                    vc.initWrapper();

                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (final Throwable e) {
            JDLogger.exception(e);
        } finally {
            try {
                HosterPluginCache.getInstance().save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JDInit() {
    }

    public void checkUpdate() {

        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
            final String old = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
            if (!old.equals(JDUtilities.getRevision())) {
                JDInit.LOG.info("Detected that JD just got updated");
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));
                final ConfirmDialog dialog = new ConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, JDL.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDL.L("system.update.message", "Update successfull"), null, null, null);
                dialog.setLeftActions(new AbstractAction(JDL.L("system.update.showchangelogv2", "What's new?")) {

                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(final ActionEvent e) {
                        try {
                            OS.launchBrowser("http://jdownloader.org/changes/index");
                        } catch (final IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                });
                try {
                    Dialog.getInstance().showDialog(dialog);
                } catch (DialogClosedException e1) {

                } catch (DialogCanceledException e1) {

                }
            }
        }
        this.submitVersion();
    }

    public void init() {
        this.initBrowser();

    }

    public void initBrowser() {
        Browser.setGlobalLogger(JDLogger.getLogger());

        /* init default global Timeouts */
        Browser.setGlobalReadTimeout(JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.PARAM_DOWNLOAD_READ_TIMEOUT, 100000));
        Browser.setGlobalConnectTimeout(JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000));

        if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(UpdaterConstants.USE_PROXY, false)) {
            final String host = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_HOST, "");
            final int port = JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.PROXY_PORT, 8080);
            final String user = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_USER, "");
            final String pass = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_PASS, "");
            if ("".equals(host.trim())) {
                JDInit.LOG.warning("Proxy disabled. No host");
                JSonWrapper.get("DOWNLOAD").setProperty(UpdaterConstants.USE_PROXY, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }
        if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(UpdaterConstants.USE_SOCKS, false)) {
            final String user = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_USER_SOCKS, "");
            final String pass = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_PASS_SOCKS, "");
            final String host = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.SOCKS_HOST, "");
            final int port = JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.SOCKS_PORT, 1080);
            if ("".equals(host.trim())) {
                JDInit.LOG.warning("Socks Proxy disabled. No host");
                JSonWrapper.get("DOWNLOAD").setProperty(UpdaterConstants.USE_SOCKS, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }

    }

    public void initControllers() {
        GarbageController.getInstance();
        DownloadController.getInstance();
        /* add ShutdownHook so we have chance to save database properly */

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                try {
                    if (DatabaseConnector.isDatabaseShutdown()) {
                        System.out.println("ShutDownHook: normal shutdown event, nothing to do.");
                        return;
                    } else {
                        System.out.println("ShutDownHook: unexpected shutdown event, hurry up and save database!");
                        JDController.getInstance().prepareShutdown(true);
                        System.out.println("ShutDownHook: unexpected shutdown event, could finish saving database!");
                        return;
                    }
                } catch (final Throwable e) {
                }
            }
        });

    }

    public void initGUI(final JDController controller) {
        LookAndFeelController.setUIManager();
        EDTEventQueue.initEventQueue();
        ActionController.initActions();
        SwingGui.setInstance(JDGui.getInstance());
        UserIF.setInstance(SwingGui.getInstance());
        controller.addControlListener(SwingGui.getInstance());
    }

    public void initPlugins() {
        try {
            this.movePluginUpdates(JDUtilities.getResourceFile("update"));
        } catch (final Throwable e) {
            JDLogger.exception(e);
        }
        try {
            this.loadCPlugins();
            this.loadPluginOptional();
            for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
                if (plg.isLoaded()) {
                    try {
                        if (plg.isEnabled() && !plg.getPlugin().startAddon()) {
                            JDInit.LOG.severe("Error loading Optional Plugin:" + plg.getClassName());
                            /* could not start, so set disabled again */
                            plg.setEnabled(false);
                        }
                    } catch (final Throwable e) {
                        JDInit.LOG.severe("Error loading Optional Plugin: " + e.getMessage());
                        /* could not start, so set disabled again */
                        plg.setEnabled(false);
                        JDLogger.exception(e);
                    }
                }
            }
        } catch (final Throwable e) {
            JDLogger.exception(e);
        }
    }

    public Configuration loadConfiguration() {
        final Object obj = JDUtilities.getDatabaseConnector().getData(Configuration.NAME);

        if (obj == null) {
            JDInit.LOG.finest("Fresh install?");
        }

        if (!JDInit.TEST_INSTALLER && obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {
            final Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            JDInit.LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_THEME, "default"));
        } else {
            final File cfg = JDUtilities.getResourceFile("config");
            if (!cfg.exists()) {
                if (!cfg.mkdirs()) {
                    System.err.println("Could not create configdir");
                    return null;
                }
                if (!cfg.canWrite()) {
                    System.err.println("Cannot write to configdir");
                    return null;
                }
            }
            final Configuration configuration = new Configuration();
            JDUtilities.setConfiguration(configuration);
            JDInit.LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_THEME, "default"));

            JDUtilities.getDatabaseConnector().saveConfiguration(Configuration.NAME, JDUtilities.getConfiguration());
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));

            LookAndFeelController.setUIManager();
            final Installer inst = new Installer();

            if (!inst.isAborted()) {
                final File home = JDUtilities.getResourceFile(".");
                if (!home.canWrite()) {
                    JDInit.LOG.severe("INSTALL abgebrochen");
                    UserIO.getInstance().requestMessageDialog(JDL.L("installer.error.noWriteRights", "Error. You do not have permissions to write to the dir"));
                    JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(1);
                }
            } else {
                JDInit.LOG.severe("INSTALL abgebrochen2");
                UserIO.getInstance().requestMessageDialog(JDL.L("installer.abortInstallation", "Error. User aborted installation."));
                JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                System.exit(0);
            }
        }
        return JDUtilities.getConfiguration();
    }

    public void loadCPlugins() {
        try {
            new CPluginWrapper("ccf", "C", ".+\\.ccf");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("rsdf", "R", ".+\\.rsdf");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("dlc", "D", ".+\\.dlc");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("jdc", "J", ".+\\.jdc");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("metalink", "MetaLink", ".+\\.metalink");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("Amazon MP3", "AMZ", ".+\\.amz");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public void loadPluginOptional() {

        final ArrayList<String> list = new ArrayList<String>();
        try {
            for (final VirtualClass vc : ClassFinder.getClasses("jd.plugins.optional", JDUtilities.getJDClassLoader())) {
                try {

                    Class<?> c = vc.loadClass();

                    final String cName = c.getName();
                    if (c.getSimpleName().contains("$")) {
                        /* do not load inner classes */
                        continue;
                    }
                    if (list.contains(cName)) {
                        System.out.println("Already loaded: " + c);
                        continue;
                    }

                    final OptionalPlugin help = c.getAnnotation(OptionalPlugin.class);
                    if (help == null) {
                        continue;
                    }
                    double cJavaV = Application.getJavaVersion() / 10000000.0d;
                    if (help.windows() && OSDetector.isWindows() || (help.linux() && OSDetector.isLinux()) || help.mac() && OSDetector.isMac()) {
                        if (cJavaV >= help.minJVM() && PluginOptional.ADDON_INTERFACE_VERSION == help.interfaceversion()) {
                            new OptionalPluginWrapper(c, help);
                            list.add(cName);
                        }
                    }
                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (final Throwable e) {
            JDLogger.exception(e);
        }
    }

    private void movePluginUpdates(final File dir) {
        if (!JDUtilities.getResourceFile("update").exists() || !dir.isDirectory()) { return; }

        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                this.movePluginUpdates(f);
            } else {
                // Create relativ path
                final File update = JDUtilities.getResourceFile("update");
                final File root = update.getParentFile();
                String n = JDUtilities.getResourceFile("update").getAbsolutePath();
                n = f.getAbsolutePath().replace(n, "").substring(1);
                final File newFile = new File(root, n).getAbsoluteFile();
                JDInit.LOG.info("./update -> real  " + n + " -> " + newFile.getAbsolutePath());
                JDInit.LOG.info("Exists: " + newFile.exists());

                if (!newFile.getParentFile().exists()) {
                    JDInit.LOG.info("Parent Exists: false");
                    if (newFile.getParentFile().mkdirs()) {
                        JDInit.LOG.info("^^CREATED");
                    } else {
                        JDInit.LOG.info("^^CREATION FAILED");
                    }
                }

                newFile.delete();
                f.renameTo(newFile);
                File parent = newFile.getParentFile();

                while (parent.listFiles().length == 0) {
                    parent.delete();
                    parent = parent.getParentFile();
                }
            }
        }
        final String[] list = dir.list();
        if (list != null && list.length == 0) {
            dir.delete();
        }
    }

    private void submitVersion() {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    String os = "unk";
                    if (OSDetector.isLinux()) {
                        os = "lin";
                    } else if (OSDetector.isMac()) {
                        os = "mac";
                    } else if (OSDetector.isWindows()) {
                        os = "win";
                    }
                    String tz = System.getProperty("user.timezone");
                    if (tz == null) {
                        tz = "unknown";
                    }
                    final Browser br = new Browser();
                    br.setConnectTimeout(15000);
                    if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "").equals(JDUtilities.getRevision())) {
                        try {
                            final String prev = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
                            br.postPage("http://service.jdownloader.org/tools/s.php", "v=" + JDUtilities.getRevision().replaceAll(",|\\.", "") + "&p=" + prev + "&os=" + os + "&tz=" + Encoding.urlEncode(tz));
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_VERSION, JDUtilities.getRevision());
                            JDUtilities.getConfiguration().save();
                        } catch (final Exception e) {
                        }
                    }
                }
            }
        }).start();
    }

}
