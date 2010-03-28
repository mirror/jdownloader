//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org  http://jdownloader.org
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
//

package jd;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.MacOSController;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.http.Browser;
import jd.nutils.OSDetector;
import jd.nutils.OutdatedParser;
import jd.update.FileUpdate;
import jd.update.WebUpdater;
import jd.utils.CheckJava;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.appwork.utils.singleapp.AnotherInstanceRunningException;
import org.appwork.utils.singleapp.InstanceMessageListener;
import org.appwork.utils.singleapp.SingleAppInstance;

/**
 * @author JD-Team
 */
public class Main {

    private static Logger LOG;
    private static boolean instanceStarted = false;
    public static SingleAppInstance SINGLE_INSTANCE_CONTROLLER = null;

    public static boolean returnedfromUpdate() {
        return JDInitFlags.SWITCH_RETURNED_FROM_UPDATE;
    }

    public static void main(String args[]) {

        System.setProperty("file.encoding", "UTF-8");
        // OSDetector.setOSString(System.getProperty("os.name"));
        // System.setProperty("os.name", "Windows Vista m.a.c");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        LOG = JDLogger.getLogger();
        initMACProperties();
        LOG.info("Start JDownloader");
        for (String p : args) {
            if (p.equalsIgnoreCase("-debug")) {
                JDInitFlags.SWITCH_DEBUG = true;
                LOG.info("DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-brdebug")) {
                JDInitFlags.SWITCH_DEBUG = true;
                Browser.setVerbose(true);
                LOG.info("Browser DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-config")) {
                new Config();

                return;
            } else if (p.equalsIgnoreCase("-trdebug")) {
                JDL.DEBUG = true;
                LOG.info("Translation DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-rfu")) {
                JDInitFlags.SWITCH_RETURNED_FROM_UPDATE = true;
            }
        }
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            JDInitFlags.SWITCH_DEBUG = true;
        }
        UserIO.setInstance(UserIOGui.getInstance());
        preInitChecks();
        JDUtilities.setJDargs(args);

        for (int i = 0; i < args.length; i++) {

            if (args[i].equalsIgnoreCase("-branch")) {
                SubConfiguration webConfig = SubConfiguration.getConfig("WEBUPDATE");
                if (args[i + 1].equalsIgnoreCase("reset")) {
                    webConfig.setProperty(WebUpdater.PARAM_BRANCH, null);
                    if (webConfig.hasChanges()) {
                        webConfig.save();
                        LOG.info("Switching back to default JDownloader branch");
                    }
                } else {

                    webConfig.setProperty(WebUpdater.PARAM_BRANCH, args[i + 1]);
                    if (webConfig.hasChanges()) {
                        webConfig.save();
                        LOG.info("Switching to " + args[i + 1] + " JDownloader branch");
                    }
                }

                i++;
            } else if (args[i].equals("-prot")) {

                LOG.finer(args[i] + " " + args[i + 1]);
                i++;
            } else if (args[i].equals("-lng")) {

                LOG.finer(args[i] + " " + args[i + 1]);
                if (new File(args[i + 1]).exists() && args[i + 1].trim().endsWith(".loc")) {
                    LOG.info("Use custom languagefile: " + args[i + 1]);
                    JDL.setStaticLocale(args[i + 1]);
                }
                i++;

            } else if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                if (!JDInitFlags.ENOUGH_MEMORY) {
                    JDUtilities.restartJDandWait();
                }

                LOG.finer(args[i] + " parameter");
                JDInitFlags.SWITCH_NEW_INSTANCE = true;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                ParameterManager.showCmdHelp();
                System.exit(0);

            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {

                    LOG.setLevel(Level.OFF);
                    String captchaValue = JAntiCaptcha.getCaptcha(args[i + 1], args[i + 2]);
                    System.out.println(captchaValue);
                    System.exit(0);

                } else {

                    System.out.println("Error: Please define filepath and JAC method");
                    System.out.println("Usage: java -jar JDownloader.jar --captcha /path/file.png example.com");
                    System.exit(0);

                }

            } else if (args[i].equals("--show") || args[i].equals("-s")) {

                JACController.showDialog(false);
                JDInitFlags.STOP = true;

            } else if (args[i].equals("--train") || args[i].equals("-t")) {

                JACController.showDialog(true);
                JDInitFlags.STOP = true;

            } else if (JDInitFlags.SHOW_SPLASH && args[i].matches("(--add-.*|--start-download|-[dDmfHr]|--stop-download|--minimize|--focus|--hide|--reconnect)")) {
                JDInitFlags.SHOW_SPLASH = false;
            }

        }
        try {
            SINGLE_INSTANCE_CONTROLLER = new SingleAppInstance("JD", JDUtilities.getJDHomeDirectoryFromEnvironment());
            SINGLE_INSTANCE_CONTROLLER.setInstanceMessageListener(new InstanceMessageListener() {
                public void parseMessage(String[] args) {
                    ParameterManager.processParameters(args);
                }
            });
            SINGLE_INSTANCE_CONTROLLER.start();
            instanceStarted = true;
        } catch (AnotherInstanceRunningException e) {
            LOG.info("existing jD instance found!");
            instanceStarted = false;
        } catch (Exception e) {
            JDLogger.exception(e);
            LOG.severe("Instance Handling not possible!");
            instanceStarted = true;
        }

        JDController.getInstance();

        if (instanceStarted || JDInitFlags.SWITCH_NEW_INSTANCE) {
            JDTheme.setTheme("default");
            if (JDInitFlags.SHOW_SPLASH) {
                if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPLASH, true)) {
                    LOG.info("init Splash");
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            try {
                                new SplashScreen(JDController.getInstance());
                            } catch (Exception e) {
                                JDLogger.exception(e);
                            }
                            return null;
                        }

                    }.waitForEDT();
                }
            }

            Interaction.deleteInteractions();

            start(args);
        } else {
            if (args.length > 0) {
                LOG.info("Send parameters to existing jD instance and exit");
                SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(args);
            } else {
                LOG.info("There is already a running jD instance");
                SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(new String[] { "--focus" });
            }
            System.exit(0);
        }
    }

    private static void start(final String args[]) {

        if (!JDInitFlags.STOP && !JDInitFlags.ENOUGH_MEMORY) {
            JDUtilities.restartJDandWait();
            return;
        }
        if (!JDInitFlags.STOP) {
            final Main main = new Main();
            EventQueue.invokeLater(new Runnable() {
                public void run() {

                    main.go();
                    for (String p : args) {
                        LOG.finest("Param: " + p);
                    }
                    ParameterManager.processParameters(args);
                }
            });
        }
    }

    private static void preInitChecks() {
        heapCheck();
        javaCheck();
    }

    private static void heapCheck() {
        JDInitFlags.ENOUGH_MEMORY = !(Runtime.getRuntime().maxMemory() < 100000000);
        if (!JDInitFlags.ENOUGH_MEMORY) {
            JDInitFlags.SHOW_SPLASH = false;
            LOG.warning("Heapcheck: Not enough heap. use: java -Xmx512m -jar JDownloader.jar");
        }
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (!CheckJava.check()) {
            LOG.warning("Javacheck: Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (JDUtilities.getJavaVersion() < 1.6 && !OSDetector.isMac()) {
            int returnValue = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.LF("gui.javacheck.newerjavaavailable.title", "Outdated Javaversion found: %s!", JDUtilities.getJavaVersion()), JDL.L("gui.javacheck.newerjavaavailable.msg", "Although JDownloader runs on your javaversion, we advise to install the latest java updates. \r\nJDownloader will run more stable, faster, and will look better. \r\n\r\nVisit http://jdownloader.org/download."), JDTheme.II("gui.images.warning", 32, 32), null, null);
            if ((returnValue & UserIO.RETURN_DONT_SHOW_AGAIN) == 0) {
                try {
                    JLink.openURL("http://jdownloader.org/download/index?updatejava=1");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        // Mac specific //
        if (OSDetector.isMac()) {

            // set Properties
            // Mac Java from 1.3
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("com.apple.hwaccel", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");

            // Mac Java from 1.4
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "false");

            try {
                new MacOSController();

            } catch (Exception e) {
                LOG.info("Error Initializing Look and Feel: " + e);
                e.printStackTrace();
            }

            /*
             * TODO: Pfade müssen nicht absolut angegeben werden.
             */
            if (System.getProperty("java.version").startsWith("1.5")) {
                File info15 = JDUtilities.getResourceFile("../../info_15.plist");
                File info = JDUtilities.getResourceFile("../../info.plist");
                if (info15.exists()) {
                    if (info.delete()) {
                        info15.renameTo(JDUtilities.getResourceFile("../../info.plist"));
                    }
                }
            }
        }
    }

    private void go() {
        final JDInit init = new JDInit();
        final JDController controller = JDController.getInstance();
        // JDUtilities.getController().fireControlEvent(new ControlEvent(this,
        // SplashScreen.SPLASH_PROGRESS, "This is JD :)"));
        init.init();
        LOG.info(new Date() + "");
        LOG.info("init Configuration");
        // JDUtilities.getController().fireControlEvent(new ControlEvent(this,
        // SplashScreen.SPLASH_PROGRESS, "Once upon a time..."));

        if (init.loadConfiguration() == null) {

            UserIO.getInstance().requestMessageDialog("JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }
        if (JDInitFlags.SWITCH_DEBUG) {
            LOG.info("DEBUG MODE ACTIVATED");
            LOG.setLevel(Level.ALL);
        } else {
            JDLogger.removeConsoleHandler();
        }
        if (!OutdatedParser.parseFile(JDUtilities.getResourceFile("outdated.dat"))) {
            LOG.severe("COULD NOT DELETE OUTDATED FILES.RESTART REQUIRED");
            int answer = UserIO.getInstance().requestConfirmDialog(0, JDL.L("jd.Main.removerestart.title", "Updater"), JDL.L("jd.Main.removerestart.message", "Could not remove outdated libraries. Restart recommended!"), null, JDL.L("jd.Main.removerestart.ok", "Restart now!"), JDL.L("jd.Main.removerestart.cancel", "Continue"));
            if (UserIO.isOK(answer)) {
                JDUtilities.restartJD(true);
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }

            }
        }
        LOG.info("init Controller");

        LOG.info("init Webupdate");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.webupdate", "Check updates")));

        LOG.info("init plugins");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.initplugins", "Init plugins")));

        init.initPlugins();

        Locale.setDefault(Locale.ENGLISH);

        LOG.info("init downloadqueue");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.controller", "Start controller")));
        init.initControllers();
        LOG.info("init gui");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.paintgui", "Paint user interface")));

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                init.initGUI(controller);
                return null;
            }
        }.waitForEDT();

        LOG.info("Initialisation finished");

        HashMap<String, String> head = new HashMap<String, String>();
        head.put("rev", JDUtilities.getRevision());
        JDUtilities.getConfiguration().setProperty("head", head);

        Properties pr = System.getProperties();
        TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());

        for (Object it : propKeys) {
            String key = it.toString();
            LOG.finer(key + "=" + pr.get(key));
        }

        LOG.info("Revision: " + JDUtilities.getRevision());
        LOG.finer("Runtype: " + JDUtilities.getRunType());

        init.checkUpdate();
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_INIT_COMPLETE, null));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            JDLogger.exception(e);
        }

        /*
         * Keeps the home working directory for developers up2date
         */
        LOG.info("update start");

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    WebUpdate.doUpdateCheck(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        try {
            loadDynamics();
        } catch (Exception e1) {
            JDLogger.exception(Level.FINEST, e1);
        }
        WebUpdate.dynamicPluginsFinished();

        LOG.info("update end");
    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * @throws IOException
     */
    public static void loadDynamics() throws Exception {
        ArrayList<String> classes = new ArrayList<String>();
        URLClassLoader classLoader = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL() }, Thread.currentThread().getContextClassLoader());
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            /* dynamics aus eclipse heraus laden */

            Enumeration<URL> resources = classLoader.getResources("jd/dynamics/");
            ArrayList<String> dynamics = new ArrayList<String>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.toURI().getPath() != null) {
                    String[] files = new File(resource.toURI().getPath()).list();
                    if (files != null) {
                        for (String file : files) {
                            dynamics.add(new File(file).getName());
                        }
                    }
                }
            }
            if (dynamics.size() == 0) return;
            for (String dynamic : dynamics) {
                if (!dynamic.contains("$") && !classes.contains("/jd/dynamics/" + dynamic) && !dynamic.equalsIgnoreCase("DynamicPluginInterface.class")) {
                    System.out.println("Plugins: " + dynamic);
                    classes.add("/jd/dynamics/" + dynamic);
                }
            }
        } else {

            /* dynamics in der public laden */
            LOG.finest("Run dynamics");
            if (WebUpdater.getPluginList() == null) return;
            for (Entry<String, FileUpdate> entry : WebUpdater.PLUGIN_LIST.entrySet()) {
                System.out.println("Plugins: " + entry.getKey());
                if (entry.getKey().startsWith("/jd/dynamics/") && !entry.getKey().contains("DynamicPluginInterface")) {
                    LOG.finest("Found dynamic: " + entry.getKey());
                    if (!entry.getValue().equals()) {

                        if (!new WebUpdater().updateUpdatefile(entry.getValue())) {
                            LOG.warning("Could not update " + entry.getValue());
                            continue;
                        } else {
                            LOG.finest("Update OK!");
                        }
                    }
                    if (!entry.getKey().contains("$") && !classes.contains(entry.getKey())) classes.add(entry.getKey());
                }
            }
        }
        for (String clazz : classes) {
            try {
                Class<?> plgClass;
                LOG.finest("Init Dynamic " + clazz);
                plgClass = classLoader.loadClass(clazz.replace("/", ".").replace(".class", "").substring(1));
                if (plgClass == null) {
                    LOG.info("Could not load " + clazz);
                    continue;
                }
                if (plgClass == DynamicPluginInterface.class) continue;
                Constructor<?> con = plgClass.getConstructor(new Class[] {});
                DynamicPluginInterface dplg = (DynamicPluginInterface) con.newInstance(new Object[] {});
                dplg.execute();
            } catch (Throwable e) {
                JDLogger.exception(Level.FINER, e);
            }
        }
    }

}
