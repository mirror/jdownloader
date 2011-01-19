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
import java.util.Random;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.nutils.JDImage;
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
    private static Logger           LOG;
    private static boolean          instanceStarted            = false;
    public static SingleAppInstance SINGLE_INSTANCE_CONTROLLER = null;

    private static boolean          Init_Complete              = false;
    private static JSonWrapper      webConfig;

    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        // set DockIcon (most used in Building)
        try {
            com.apple.eawt.Application.getApplication().setDockIconImage(JDImage.getImage("logo/jd_logo_128_128"));
        } catch (final Throwable e) {
            /* not every mac has this */
            Main.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            e.printStackTrace();
        }

        // Use ScreenMenu in every LAF
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // native Mac just if User Choose Aqua as Skin
        if (LookAndFeelController.getPlaf().getName().equals("Apple Aqua")) {
            // Mac Java from 1.3
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            System.setProperty("com.apple.hwaccel", "true");

            // Mac Java from 1.4
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "true");
        }

        try {
            MacOSApplicationAdapter.enableMacSpecial();
        } catch (final Throwable e) {
            Main.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            e.printStackTrace();
        }

        /*
         * TODO: Pfade müssen nicht absolut angegeben werden. Detmud: verstehe
         * diesen Codezeilen nicht, wenn wer weiß was sie sollen bitte
         * defenieren
         */
        if (System.getProperty("java.version").startsWith("1.5")) {
            final File info15 = JDUtilities.getResourceFile("../../info_15.plist");
            final File info = JDUtilities.getResourceFile("../../info.plist");
            if (info15.exists()) {
                if (info.delete()) {
                    info15.renameTo(JDUtilities.getResourceFile("../../info.plist"));
                }
            }
        }
    }

    public static boolean isInitComplete() {
        return Main.Init_Complete;
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (!CheckJava.check()) {
            Main.LOG.warning("Javacheck: Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (JDUtilities.getJavaVersion() < 1.6 && !OSDetector.isMac()) {
            final int returnValue = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDL.LF("gui.javacheck.newerjavaavailable.title", "Outdated Javaversion found: %s!", JDUtilities.getJavaVersion()), JDL.L("gui.javacheck.newerjavaavailable.msg", "Although JDownloader runs on your javaversion, we advise to install the latest java updates. \r\nJDownloader will run more stable, faster, and will look better. \r\n\r\nVisit http://jdownloader.org/download."), JDTheme.II("gui.images.warning", 32, 32), null, null);
            if ((returnValue & UserIO.RETURN_DONT_SHOW_AGAIN) == 0) {
                try {
                    JLink.openURL("http://jdownloader.org/download/index?updatejava=1");
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * @throws IOException
     */
    public static void loadDynamics() throws Exception {
        final ArrayList<String> classes = new ArrayList<String>();
        final URLClassLoader classLoader = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL() }, Thread.currentThread().getContextClassLoader());
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            /* dynamics aus eclipse heraus laden */

            final Enumeration<URL> resources = classLoader.getResources("jd/dynamics/");
            final ArrayList<String> dynamics = new ArrayList<String>();
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                if (resource.toURI().getPath() != null) {
                    final String[] files = new File(resource.toURI().getPath()).list();
                    if (files != null) {
                        for (final String file : files) {
                            dynamics.add(new File(file).getName());
                        }
                    }
                }
            }
            if (dynamics.size() == 0) { return; }
            for (final String dynamic : dynamics) {
                if (!dynamic.contains("$") && !classes.contains("/jd/dynamics/" + dynamic) && !dynamic.equalsIgnoreCase("DynamicPluginInterface.class")) {
                    System.out.println("Plugins: " + dynamic);
                    classes.add("/jd/dynamics/" + dynamic);
                }
            }
        } else {

            /* dynamics in der public laden */
            Main.LOG.finest("Run dynamics");
            if (WebUpdater.getPluginList() == null) { return; }
            for (final Entry<String, FileUpdate> entry : WebUpdater.PLUGIN_LIST.entrySet()) {
                System.out.println("Plugins: " + entry.getKey());
                if (entry.getKey().startsWith("/jd/dynamics/") && !entry.getKey().contains("DynamicPluginInterface")) {
                    Main.LOG.finest("Found dynamic: " + entry.getKey());
                    if (!entry.getValue().equals()) {

                        if (!new WebUpdater().updateUpdatefile(entry.getValue())) {
                            Main.LOG.warning("Could not update " + entry.getValue());
                            continue;
                        } else {
                            Main.LOG.finest("Update OK!");
                        }
                    }
                    if (!entry.getKey().contains("$") && !classes.contains(entry.getKey())) {
                        classes.add(entry.getKey());
                    }
                }
            }
        }
        for (final String clazz : classes) {
            try {
                Class<?> plgClass;
                Main.LOG.finest("Init Dynamic " + clazz);
                plgClass = classLoader.loadClass(clazz.replace("/", ".").replace(".class", "").substring(1));
                if (plgClass == null) {
                    Main.LOG.info("Could not load " + clazz);
                    continue;
                }
                if (plgClass == DynamicPluginInterface.class) {
                    continue;
                }
                final Constructor<?> con = plgClass.getConstructor(new Class[] {});
                final DynamicPluginInterface dplg = (DynamicPluginInterface) con.newInstance(new Object[] {});
                dplg.execute();
            } catch (final Throwable e) {
                JDLogger.exception(Level.FINER, e);
            }
        }
    }

    public static void main(final String args[]) {
        Main.LOG = JDLogger.getLogger();
        // Mac OS specific
        if (OSDetector.isMac()) {
            // Set MacApplicationName
            // Must be in Main
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
            Main.initMACProperties();
        }
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        // Disable the GUI rendering on the graphic card
        System.setProperty("sun.java2d.d3d", "false");

        Main.LOG.info("Start JDownloader");

        for (final String p : args) {
            if (p.equalsIgnoreCase("-debug")) {
                JDInitFlags.SWITCH_DEBUG = true;
                Main.LOG.info("DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-brdebug")) {
                JDInitFlags.SWITCH_DEBUG = true;
                Browser.setGlobalVerbose(true);
                Main.LOG.info("Browser DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-config")) {
                new Config();

                return;
            } else if (p.equalsIgnoreCase("-trdebug")) {
                JDL.DEBUG = true;
                Main.LOG.info("Translation DEBUG Modus aktiv");
            } else if (p.equalsIgnoreCase("-rfu")) {
                JDInitFlags.SWITCH_RETURNED_FROM_UPDATE = true;
            }
        }
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            JDInitFlags.SWITCH_DEBUG = true;
        }

        Main.preInitChecks();
        JDUtilities.setJDargs(args);

        for (int i = 0; i < args.length; i++) {

            if (args[i].equalsIgnoreCase("-branch")) {
                webConfig = JSonWrapper.get("WEBUPDATE");
                if (args[i + 1].equalsIgnoreCase("reset")) {
                    webConfig.setProperty(WebUpdater.PARAM_BRANCH, null);
                    if (webConfig.hasChanges()) {
                        webConfig.save();
                        Main.LOG.info("Switching back to default JDownloader branch");
                    }
                } else {

                    webConfig.setProperty(WebUpdater.PARAM_BRANCH, args[i + 1]);
                    if (webConfig.hasChanges()) {
                        webConfig.save();
                        Main.LOG.info("Switching to " + args[i + 1] + " JDownloader branch");
                    }
                }

                i++;
            } else if (args[i].equals("-prot")) {

                Main.LOG.finer(args[i] + " " + args[i + 1]);
                i++;
            } else if (args[i].equals("-lng")) {

                Main.LOG.finer(args[i] + " " + args[i + 1]);
                if (new File(args[i + 1]).exists() && args[i + 1].trim().endsWith(".loc")) {
                    Main.LOG.info("Use custom languagefile: " + args[i + 1]);
                    JDL.setStaticLocale(args[i + 1]);
                }
                i++;

            } else if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                Main.LOG.finer(args[i] + " parameter");
                JDInitFlags.SWITCH_NEW_INSTANCE = true;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                ParameterManager.showCmdHelp();
                System.exit(0);

            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {

                    Main.LOG.setLevel(Level.OFF);
                    final String captchaValue = JAntiCaptcha.getCaptcha(args[i + 1], args[i + 2]);
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
            Main.SINGLE_INSTANCE_CONTROLLER = new SingleAppInstance("JD", JDUtilities.getJDHomeDirectoryFromEnvironment());
            Main.SINGLE_INSTANCE_CONTROLLER.setInstanceMessageListener(new InstanceMessageListener() {
                public void parseMessage(final String[] args) {
                    ParameterManager.processParameters(args);
                }
            });
            Main.SINGLE_INSTANCE_CONTROLLER.start();
            Main.instanceStarted = true;
        } catch (final AnotherInstanceRunningException e) {
            Main.LOG.info("existing jD instance found!");
            Main.instanceStarted = false;
        } catch (final Exception e) {
            JDLogger.exception(e);
            Main.LOG.severe("Instance Handling not possible!");
            Main.instanceStarted = true;
        }

        JDController.getInstance();

        if (Main.instanceStarted || JDInitFlags.SWITCH_NEW_INSTANCE) {
            JDTheme.setTheme("default");
            if (JDInitFlags.SHOW_SPLASH) {
                if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPLASH, true)) {
                    Main.LOG.info("init Splash");
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            try {
                                new SplashScreen(JDController.getInstance());
                            } catch (final Exception e) {
                                JDLogger.exception(e);
                            }
                            return null;
                        }

                    }.waitForEDT();
                }
            }

            Main.start(args);
        } else {
            if (args.length > 0) {
                Main.LOG.info("Send parameters to existing jD instance and exit");
                Main.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(args);
            } else {
                Main.LOG.info("There is already a running jD instance");
                Main.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(new String[] { "--focus" });
            }
            System.exit(0);
        }
    }

    private static void preInitChecks() {
        Main.javaCheck();
    }

    public static boolean returnedfromUpdate() {
        return JDInitFlags.SWITCH_RETURNED_FROM_UPDATE;
    }

    private static void start(final String args[]) {
        if (!JDInitFlags.STOP) {
            final Main main = new Main();
            EventQueue.invokeLater(new Runnable() {
                public void run() {

                    main.go();
                    for (final String p : args) {
                        Main.LOG.finest("Param: " + p);
                    }
                    ParameterManager.processParameters(args);
                }
            });
        }
    }

    private void go() {
        final JDInit init = new JDInit();
        final JDController controller = JDController.getInstance();
        init.init();

        Main.LOG.info(new Date().toString());
        Main.LOG.info("init Configuration");

        if (init.loadConfiguration() == null) {
            UserIO.getInstance().requestMessageDialog("JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }
        if (JDInitFlags.SWITCH_DEBUG) {
            Main.LOG.info("DEBUG MODE ACTIVATED");
            Main.LOG.setLevel(Level.ALL);
        } else {
            JDLogger.removeConsoleHandler();
        }
        if (!OutdatedParser.parseFile(JDUtilities.getResourceFile("outdated.dat"))) {
            Main.LOG.severe("COULD NOT DELETE OUTDATED FILES.RESTART REQUIRED");
            final int answer = UserIO.getInstance().requestConfirmDialog(0, JDL.L("jd.Main.removerestart.title", "Updater"), JDL.L("jd.Main.removerestart.message", "Could not remove outdated libraries. Restart recommended!"), null, JDL.L("jd.Main.removerestart.ok", "Restart now!"), JDL.L("jd.Main.removerestart.cancel", "Continue"));
            if (UserIO.isOK(answer)) {
                JDUtilities.restartJD(true);
                while (true) {
                    try {
                        Thread.sleep(5000);
                    } catch (final InterruptedException e) {

                        // TODO: Cant be Empty
                    }
                }
            }
        }

        Main.LOG.info("init Webupdate");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.webupdate", "Check updates")));

        Main.LOG.info("init plugins");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.initplugins", "Init plugins")));

        init.initPlugins();

        Locale.setDefault(Locale.ENGLISH);

        Main.LOG.info("init downloadqueue");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.controller", "Start controller")));
        init.initControllers();
        Main.LOG.info("init gui");
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, SplashScreen.SPLASH_PROGRESS, JDL.L("gui.splash.progress.paintgui", "Paint user interface")));

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                init.initGUI(controller);
                return null;
            }
        }.waitForEDT();

        Main.LOG.info("Initialisation finished");

        final HashMap<String, String> head = new HashMap<String, String>();
        head.put("rev", JDUtilities.getRevision());
        JDUtilities.getConfiguration().setProperty("head", head);

        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());

        for (final Object it : propKeys) {
            final String key = it.toString();
            Main.LOG.finer(key + "=" + pr.get(key));
        }

        Main.LOG.info("Revision: " + JDUtilities.getRevision());
        Main.LOG.finer("Runtype: " + JDUtilities.getRunType());

        init.checkUpdate();
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_INIT_COMPLETE, null));
        Main.Init_Complete = true;

        try {
            Thread.sleep(3000);
        } catch (final InterruptedException e) {
            JDLogger.exception(e);
        }

        /*
         * Keeps the home working directory for developers up2date
         */
        Main.LOG.info("update start");

        new Thread("Update and dynamics") {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    WebUpdate.doUpdateCheck(false);

                    Main.loadDynamics();

                    WebUpdate.dynamicPluginsFinished();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        Main.LOG.info("update end");
    }

}
