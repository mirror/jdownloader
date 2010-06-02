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

import java.awt.Toolkit;
import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.DatabaseConnector;
import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.controlling.GarbageController;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.events.waitcursor.WaitCursorEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.nutils.ClassFinder;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.plugins.DecrypterPlugin;
import jd.plugins.HostPlugin;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * @author JD-Team
 */

public class JDInit {

    private static final boolean TEST_INSTALLER = false;

    private static final Logger LOG = JDLogger.getLogger();

    private static ClassLoader CL;

    private boolean installerVisible = false;

    public JDInit() {
    }

    public void checkUpdate() {
        if (JDUtilities.getResourceFile("webcheck.tmp").exists() && JDIO.readFileToString(JDUtilities.getResourceFile("webcheck.tmp")).indexOf("(Revision" + JDUtilities.getRevision() + ")") > 0) {
            UserIO.getInstance().requestTextAreaDialog("Error", "Failed Update detected!", "It seems that the previous webupdate failed.\r\nPlease ensure that your java-version is equal- or above 1.5.\r\nMore infos at http://www.syncom.org/projects/jdownloader/wiki/FAQ.\r\n\r\nErrorcode: \r\n" + JDIO.readFileToString(JDUtilities.getResourceFile("webcheck.tmp")));
            JDUtilities.getResourceFile("webcheck.tmp").delete();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        }
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
            String old = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
            if (!old.equals(JDUtilities.getRevision())) {
                LOG.info("Detected that JD just got updated");
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));
                int status = UserIO.getInstance().requestHelpDialog(UserIO.NO_CANCEL_OPTION, JDL.LF("system.update.message.title", "Updated to version %s", JDUtilities.getRevision()), JDL.L("system.update.message", "Update successfull"), JDL.L("system.update.showchangelogv2", "What's new?"), "http://jdownloader.org/changes/index");
                if (JDFlags.hasAllFlags(status, UserIO.RETURN_OK) && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_SHOW_CHANGELOG, true)) {
                    try {
                        JLink.openURL("http://jdownloader.org/changes/index");
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }
        submitVersion();
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
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }).start();
    }

    public void init() {
        initBrowser();

    }

    public void initBrowser() {
        Browser.setLogger(JDLogger.getLogger());
        Browser.init();
        /* init default global Timeouts */
        Browser.setGlobalReadTimeout(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 100000));
        Browser.setGlobalConnectTimeout(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000));

        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false)) {
            final String host = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_HOST, "");
            final int port = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PROXY_PORT, 8080);
            final String user = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            final String pass = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");
            if ("".equals(host.trim())) {
                LOG.warning("Proxy disabled. No host");
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.USE_PROXY, false);
                return;
            }

            final JDProxy pr = new JDProxy(Proxy.Type.HTTP, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {
            final String user = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            final String pass = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");
            final String host = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.SOCKS_HOST, "");
            final int port = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.SOCKS_PORT, 1080);
            if ("".equals(host.trim())) {
                LOG.warning("Socks Proxy disabled. No host");
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.USE_SOCKS, false);
                return;
            }

            final JDProxy pr = new JDProxy(Proxy.Type.SOCKS, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }
        Browser.init();
    }

    public void initControllers() {
        GarbageController.getInstance();
        DownloadController.getInstance();
        /* add ShutdownHook so we have chance to save database properly */
        Runtime.getRuntime().addShutdownHook(new Thread() {
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
                } catch (Throwable e) {
                }
            }
        });
    }

    public void initGUI(final JDController controller) {
        LookAndFeelController.setUIManager();
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new WaitCursorEventQueue());
        ActionController.initActions();
        SwingGui.setInstance(JDGui.getInstance());
        UserIF.setInstance(SwingGui.getInstance());
        controller.addControlListener(SwingGui.getInstance());
    }

    public void initPlugins() {
        try {
            movePluginUpdates(JDUtilities.getResourceFile("update"));
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
        try {
            loadCPlugins();
            loadPluginOptional();
            for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
                if (plg.isLoaded()) {
                    try {
                        if (plg.isEnabled() && !plg.getPlugin().startAddon()) {
                            LOG.severe("Error loading Optional Plugin:" + plg.getClassName());
                        }
                    } catch (Throwable e) {
                        LOG.severe("Error loading Optional Plugin: " + e.getMessage());
                        JDLogger.exception(e);
                    }
                }
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }

    /**
     * @param resourceFile
     */
    private void movePluginUpdates(final File dir) {
        if (!JDUtilities.getResourceFile("update").exists() || !dir.isDirectory()) { return; }

        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                movePluginUpdates(f);
            } else {
                // Create relativ path
                final File update = JDUtilities.getResourceFile("update");
                final File root = update.getParentFile();
                String n = JDUtilities.getResourceFile("update").getAbsolutePath();
                n = f.getAbsolutePath().replace(n, "").substring(1);
                final File newFile = new File(root, n).getAbsoluteFile();
                LOG.info("./update -> real  " + n + " ->" + newFile.getAbsolutePath());
                LOG.info("Exists: " + newFile.exists());

                if (!newFile.getParentFile().exists()) {
                    LOG.info("Parent Exists: false");
                    if (newFile.getParentFile().mkdirs()) {
                        LOG.info("^^CREATED");
                    } else {
                        LOG.info("^^CREATION FAILED");
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

    public boolean installerWasVisible() {
        return installerVisible;
    }

    public Configuration loadConfiguration() {
        Object obj = JDUtilities.getDatabaseConnector().getData(Configuration.NAME);

        if (obj == null) LOG.finest("Fresh install?");

        if (!TEST_INSTALLER && obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {
            final Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
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
            LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_THEME, "default"));

            JDUtilities.getDatabaseConnector().saveConfiguration(Configuration.NAME, JDUtilities.getConfiguration());
            installerVisible = true;
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_FINISH));
            /**
             * Workaround to enable JGoodies for MAC oS
             */

            LookAndFeelController.setUIManager();
            final Installer inst = new Installer();

            if (!inst.isAborted()) {
                final File home = JDUtilities.getResourceFile(".");
                // if (home.canWrite() &&
                // !JDUtilities.getResourceFile("noupdate.txt").exists()) {
                // try {
                // new WebUpdate().doWebupdate(true);
                // JDUtilities.getConfiguration().save();
                // JDUtilities.getDatabaseConnector().shutdownDatabase();
                // logger.info(JDUtilities.runCommand("java", new String[] {
                // "-jar", "jdupdate.jar", "/restart", "/rt" +
                // JDUtilities.RUNTYPE_LOCAL_JARED },
                // home.getAbsolutePath(), 0));
                // System.exit(0);
                // } catch (Exception e) {
                // jd.controlling.JDLogger.getLogger().log(java.util.logging.
                // Level.SEVERE,"Exception occurred",e);
                // // System.exit(0);
                // }
                // }
                if (!home.canWrite()) {
                    LOG.severe("INSTALL abgebrochen");
                    UserIO.getInstance().requestMessageDialog(JDL.L("installer.error.noWriteRights", "Error. You do not have permissions to write to the dir"));
                    JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(1);
                }
            } else {
                LOG.severe("INSTALL abgebrochen2");
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("rsdf", "R", ".+\\.rsdf");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("dlc", "D", ".+\\.dlc");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("jdc", "J", ".+\\.jdc");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("metalink", "MetaLink", ".+\\.metalink");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            new CPluginWrapper("Amazon MP3", "AMZ", ".+\\.amz");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void loadPluginForDecrypt() {
        try {
            for (final Class<?> c : ClassFinder.getClasses("jd.plugins.decrypter", getPluginClassLoader())) {
                try {
                    final DecrypterPlugin help = c.getAnnotation(DecrypterPlugin.class);
                    if (help == null) continue;

                    if (help.interfaceVersion() != DecrypterPlugin.INTERFACE_VERSION) {
                        LOG.warning("Outdated Plugin found: " + help);
                        continue;
                    }

                    String simpleName = c.getSimpleName();
                    String[] names = help.names();
                    String[] patterns = help.urls();
                    int[] flags = help.flags();
                    final String revision = help.revision();
                    LOG.finest("Try to load " + c + " Revision: " + Formatter.getRevision(revision));
                    /*
                     * TODO: Change this String to test the changes from plugins
                     * with public static methods instead of annotation, e.g.
                     * cms
                     */
                    String dump = "";
                    // See if there are cached annotations
                    if (names.length == 0) {
                        final SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForDecrypt");
                        names = cfg.getGenericProperty(c.getName() + "_names_" + dump + revision, names);
                        patterns = cfg.getGenericProperty(c.getName() + "_pattern_" + dump + revision, patterns);
                        flags = cfg.getGenericProperty(c.getName() + "_flags_" + dump + revision, flags);
                    }
                    // if not, try to load them from static functions
                    if (names.length == 0) {
                        names = (String[]) c.getMethod("getAnnotationNames").invoke(null);
                        patterns = (String[]) c.getMethod("getAnnotationUrls").invoke(null);
                        flags = (int[]) c.getMethod("getAnnotationFlags").invoke(null);
                        final SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForDecrypt");
                        cfg.setProperty(c.getName() + "_names_" + revision, names);
                        cfg.setProperty(c.getName() + "_pattern_" + revision, patterns);
                        cfg.setProperty(c.getName() + "_flags_" + revision, flags);
                        cfg.save();
                    }

                    for (int i = 0; i < names.length; i++) {
                        try {
                            new DecryptPluginWrapper(names[i], simpleName, patterns[i], flags[i], revision);
                        } catch (Throwable e) {
                            LOG.severe("Could not load " + c);
                            JDLogger.exception(e);
                        }
                    }
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }

    /**
     * Returns a classloader to load plugins (class files); Depending on runtype
     * (dev or local jared) a different classoader is used to load plugins
     * either from installdirectory or from rundirectory
     * 
     * @return
     */
    public static ClassLoader getPluginClassLoader() {
        if (CL == null) {
            try {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    try {
                        System.out.println(JDUtilities.getResourceFile("java").toURI().toURL());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    CL = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL() }, Thread.currentThread().getContextClassLoader());
                } else {
                    CL = Thread.currentThread().getContextClassLoader();
                }
            } catch (MalformedURLException e) {
                JDLogger.exception(e);
            }
        }
        return CL;
    }

    public static void loadPluginForHost() {
        try {
            for (final Class<?> c : ClassFinder.getClasses("jd.plugins.hoster", getPluginClassLoader())) {
                try {
                    final HostPlugin help = c.getAnnotation(HostPlugin.class);
                    if (help == null) continue;

                    if (help.interfaceVersion() != HostPlugin.INTERFACE_VERSION) {
                        LOG.warning("Outdated Plugin found: " + help);
                        continue;
                    }

                    String simpleName = c.getSimpleName();
                    String[] names = help.names();
                    String[] patterns = help.urls();
                    int[] flags = help.flags();
                    final String revision = help.revision();
                    LOG.finest("Try to load " + c + " Revision: " + Formatter.getRevision(revision));
                    /*
                     * TODO: Change this String to test the changes from plugins
                     * with public static methods instead of annotation, e.g.
                     * cms
                     */
                    String dump = "";
                    // See if there are cached annotations
                    if (names.length == 0) {
                        final SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForHost");
                        names = cfg.getGenericProperty(c.getName() + "_names_" + dump + revision, names);
                        patterns = cfg.getGenericProperty(c.getName() + "_pattern_" + dump + revision, patterns);
                        flags = cfg.getGenericProperty(c.getName() + "_flags_" + dump + revision, flags);
                    }
                    // if not, try to load them from static functions
                    if (names.length == 0) {
                        names = (String[]) c.getMethod("getAnnotationNames").invoke(null);
                        patterns = (String[]) c.getMethod("getAnnotationUrls").invoke(null);
                        flags = (int[]) c.getMethod("getAnnotationFlags").invoke(null);
                        final SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForHost");
                        cfg.setProperty(c.getName() + "_names_" + revision, names);
                        cfg.setProperty(c.getName() + "_pattern_" + revision, patterns);
                        cfg.setProperty(c.getName() + "_flags_" + revision, flags);
                        cfg.save();
                    }

                    for (int i = 0; i < names.length; i++) {
                        try {
                            new HostPluginWrapper(names[i], simpleName, patterns[i], flags[i], revision);
                        } catch (Throwable e) {
                            LOG.severe("Could not load " + c);
                            JDLogger.exception(e);
                        }
                    }
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }

    public void loadPluginOptional() {
        final ArrayList<String> list = new ArrayList<String>();
        try {
            for (final Class<?> c : ClassFinder.getClasses("jd.plugins.optional", JDUtilities.getJDClassLoader())) {
                try {
                    final String cName = c.getName();
                    if (list.contains(cName)) {
                        System.out.println("Already loaded: " + c);
                        continue;
                    }

                    final OptionalPlugin help = c.getAnnotation(OptionalPlugin.class);
                    if (help == null) continue;

                    if ((help.windows() && OSDetector.isWindows()) || (help.linux() && OSDetector.isLinux()) || (help.mac() && OSDetector.isMac())) {
                        if (JDUtilities.getJavaVersion() >= help.minJVM() && PluginOptional.ADDON_INTERFACE_VERSION == help.interfaceversion()) {
                            new OptionalPluginWrapper(c, help);
                            list.add(cName);
                        }
                    }
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }

}
