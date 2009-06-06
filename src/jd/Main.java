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

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import it.sauronsoftware.junique.MessageHandler;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.captcha.JACController;
import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.dynamics.DynamicPluginInterface;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JDEventQueue;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.userio.SimpleUserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.OSDetector;
import jd.update.FileUpdate;
import jd.update.WebUpdater;
import jd.utils.CheckJava;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MacOSController;
import jd.utils.WebUpdate;

/**
 * @author JD-Team
 */

public class Main {

    private static boolean BETA = false;
    private static Logger LOGGER;
    public static SplashScreen SPLASH;
    private static String instanceID = Main.class.getName();
    private static boolean instanceStarted = false;

    public static boolean isBeta() {
        return BETA;
    }

    public static String getCaptcha(String path, String host) {

        boolean hasMethod = JACMethod.hasMethod(host);

        if (hasMethod) {

            File file;

            if (path.contains("http://")) {
                try {
                    Browser br = new Browser();
                    URLConnectionAdapter httpConnection;
                    httpConnection = br.openGetConnection(path);

                    if (httpConnection.getLongContentLength() == -1 || httpConnection.getLongContentLength() == 0) return "Could not download captcha image";

                    String seperator = "/";

                    if (OSDetector.getOSString().toLowerCase().contains("win") || OSDetector.getOSString().toLowerCase().contains("nt")) {
                        seperator = "\\";
                    }

                    String filepath = System.getProperty("user.dir") + seperator + "jac_captcha.img";
                    file = new File(filepath);

                    br.downloadConnection(file, httpConnection);
                } catch (IOException e) {
                    return "Downloaderror";
                }

            } else {

                file = new File(path);
                if (!file.exists()) { return "File does not exist"; }

            }

            JFrame jf = new JFrame();
            try {
                Image captchaImage = ImageIO.read(file);
                MediaTracker mediaTracker = new MediaTracker(jf);
                mediaTracker.addImage(captchaImage, 0);

                mediaTracker.waitForID(0);

                mediaTracker.removeImage(captchaImage);
                JAntiCaptcha jac = new JAntiCaptcha(JDUtilities.getJACMethodsDirectory(), host);
                Captcha captcha = jac.createCaptcha(captchaImage);
                String captchaCode = jac.checkCaptcha(captcha);
                file.delete();

                return captchaCode;
            } catch (Exception e) {
                return e.getStackTrace().toString();
            }
        } else {

            return "jDownloader has no method for " + host;

        }

    }

    public static boolean returnedfromUpdate() {
        return JDInitFlags.SWITCH_RETURNED_FROM_UPDATE;
    }

    public static void main(String args[]) {
        System.setProperty("file.encoding", "UTF-8");
        OSDetector.setOSString(System.getProperty("os.name"));
        // System.setProperty("os.name", "Windows Vista m.a.c");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        LOGGER = JDLogger.getLogger();
        initMACProperties();
        LOGGER.info("Start JDownloader");
        for (String p : args) {
            if (p.equalsIgnoreCase("-debug")) {
                JDInitFlags.SWITCH_DEBUG = true;
            }
            if (p.equalsIgnoreCase("-rfb")) {
                JDInitFlags.SWITCH_RETURNED_FROM_UPDATE = true;
            }
        }
        UserIO.setInstance(SimpleUserIO.getInstance());
        preInitChecks();
        JDUtilities.setJDargs(args);

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-prot")) {

                LOGGER.finer(args[i] + " " + args[i + 1]);
                i++;

            } else if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                if (!JDInitFlags.ENOUGH_MEMORY) {
                    JDUtilities.restartJD();
                }

                LOGGER.finer(args[i] + " parameter");
                JDInitFlags.SWITCH_NEW_INSTANCE = true;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                ParameterManager.showCmdHelp();
                System.exit(0);

            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {

                    LOGGER.setLevel(Level.OFF);
                    String captchaValue = Main.getCaptcha(args[i + 1], args[i + 2]);
                    System.out.println("" + captchaValue);
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
            JUnique.acquireLock(instanceID, new MessageHandler() {
                private int counter = -1;
                private Vector<String> params = new Vector<String>();

                public String handle(String message) {
                    if (counter == -2) return null;
                    if (counter == -1) {
                        try {
                            counter = Integer.parseInt(message.trim());
                        } catch (Exception e) {
                            counter = -2;
                            return null;
                        }
                        if (counter == -1) counter = -2;/* Abort */
                    } else {
                        params.add(message);
                        counter--;
                        if (counter == 0) {
                            String[] args = (String[]) params.toArray(new String[params.size()]);
                            ParameterManager.processParameters(args);
                            counter = -1;
                            params = new Vector<String>();
                        }
                    }
                    return null;
                }
            });
            instanceStarted = true;
        } catch (AlreadyLockedException e) {
            LOGGER.info("existing jD instance found!");
            instanceStarted = false;
        } catch (Exception e) {
            LOGGER.severe("Instance Handling not possible!");
            instanceStarted = true;
        }
        if (instanceStarted || JDInitFlags.SWITCH_NEW_INSTANCE) {
            SPLASH = null;
            JDTheme.setTheme("default");
            if (JDInitFlags.SHOW_SPLASH) {
                if (SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_SHOW_SPLASH, true)) {
                    LOGGER.info("init Splash");
                    new GuiRunnable<Object>() {

                        // @Override
                        public Object runSave() {
                            try {
                                SPLASH = new SplashScreen(JDTheme.I("gui.splash"));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.languages", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.settings", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.controller", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.update", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.plugins", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.screen", 32, 32)));
                                SPLASH.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.dllist", 32, 32)));
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "Exception occurred", e);
                            }
                            return null;
                        }

                    }.waitForEDT();
                }
            }

            LOGGER.info("init Eventmanager");
            Interaction.initTriggers();
            LOGGER.info("init Localisation");
            Main.increaseSplashStatus();

            start(args);
        } else {
            if (args.length > 0) {
                LOGGER.info("Send parameters to existing jD instance and exit");
                JUnique.sendMessage(instanceID, "" + args.length);
                for (int i = 0; i < args.length; i++) {
                    JUnique.sendMessage(instanceID, args[i]);
                }
            } else {
                LOGGER.info("There is already a running jD instance");
                JUnique.sendMessage(instanceID, "1");
                JUnique.sendMessage(instanceID, "--focus");
            }
            System.exit(0);
        }
    }

    private static void start(String args[]) {
        if (!JDInitFlags.STOP && !JDInitFlags.ENOUGH_MEMORY) {
            JDUtilities.restartJD();
            return;
        }
        final String[] processArgs = args;
        if (!JDInitFlags.STOP) {
            final Main main = new Main();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Toolkit.getDefaultToolkit().getSystemEventQueue().push(new JDEventQueue());
                    main.go();
                    for (String p : processArgs) {
                        LOGGER.finest("Param: " + p);
                    }
                    ParameterManager.processParameters(processArgs);
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
            LOGGER.warning("Heapcheck: Not enough heap. use: java -Xmx512m -jar JDownloader.jar");
        }
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (!CheckJava.check()) {
            LOGGER.warning("Javacheck: Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (JDUtilities.getJavaVersion() < 1.6 && !OSDetector.isMac()) {
            int returnValue = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_CANCEL_OPTION, JDLocale.LF("gui.javacheck.newerjavaavailable.title", "Outdated Javaversion found: %s!", JDUtilities.getJavaVersion()), JDLocale.L("gui.javacheck.newerjavaavailable.msg", "Although JDownloader runs on your javaversion, we advise to install the latest java updates. \r\nJDownloader will run more stable, faster, and will look better. \r\n\r\nVisit http://jdownloader.org/download."), JDTheme.II("gui.images.warning", 32, 32), null, null);
            if ((returnValue & UserIO.RETURN_SKIPPED_BY_DONT_SHOW) == 0) {
                try {
                    JLinkButton.openURL("http://jdownloader.org/download/index?updatejava=1");
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
            LOGGER.info("apple.laf.useScreenMenuBar=true");
            LOGGER.info("com.apple.mrj.application.growbox.intrudes=false");
            LOGGER.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            new MacOSController();
            
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

    private static void increaseSplashStatus() {
        if (SPLASH == null) return;
        SPLASH.setNextImage();
    }

    @SuppressWarnings("unchecked")
    private void go() {
        final JDInit init = new JDInit(SPLASH);

        init.init();
        if (isBeta()) {
            Browser br = new Browser();
            br.setDebug(true);
            try {
                br.getPage("http://update1.jdownloader.org/betas/beta_" + JDUtilities.getRevision());
                if (br.getRequest().getHttpConnection().isOK()) {
                    BETA = false;
                }
            } catch (IOException e1) {
            }
        }
        LOGGER.info("init Configuration");
        Main.increaseSplashStatus();

        String old = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getStringProperty("LOCALE", null);
        if (old != null) {
            SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, old);
            SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).setProperty("LOCALE", null);
            SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).save();
            SubConfiguration.getConfig(JDLocale.CONFIG).save();
        }
        if (init.loadConfiguration() == null) {

            JOptionPane.showMessageDialog(null, "JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }
        if (JDInitFlags.SWITCH_DEBUG) {
            LOGGER.info("DEBUG MODE ACTIVATED");
            LOGGER.setLevel(Level.ALL);
        } else {
            JDLogger.removeConsoleHandler();
        }

        WebUpdater.getConfig("WEBUPDATE").save();
        init.removeFiles();
        LOGGER.info("init Controller");
        Main.increaseSplashStatus();

        final JDController controller = init.initController();

        LOGGER.info("init Webupdate");
        Main.increaseSplashStatus();

        new WebUpdate().doWebupdate(false);
        try {
            loadDynamics();
        } catch (Exception e1) {
            JDLogger.exception(Level.FINEST, e1);
        }
        WebUpdate.DynamicPluginsFinished();
        LOGGER.info("init plugins");
        Main.increaseSplashStatus();
        init.initPlugins();

        Locale.setDefault(Locale.ENGLISH);

        LOGGER.info("init gui");
        Main.increaseSplashStatus();
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                init.initGUI(controller);
                return null;
            }

        }.waitForEDT();
        LOGGER.info("init downloadqueue");
        Main.increaseSplashStatus();
        init.initControllers();

        LOGGER.info("Initialisation finished");
        controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);

        HashMap<String, String> head = new HashMap<String, String>();
        head.put("rev", JDUtilities.getRevision());
        JDUtilities.getConfiguration().setProperty("head", head);

        Properties pr = System.getProperties();
        TreeSet propKeys = new TreeSet(pr.keySet());

        for (Iterator it = propKeys.iterator(); it.hasNext();) {
            String key = (String) it.next();
            LOGGER.finer("" + key + "=" + pr.get(key));
        }

        LOGGER.info("Revision: " + JDUtilities.getJDTitle());
        LOGGER.finer("Runtype: " + JDUtilities.getRunType());

        if (!Main.isBeta()) init.checkUpdate();

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_INIT_COMPLETE, null));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred", e);
        }

        if (!Main.isBeta()) new PackageManager().interact(this);

    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * @throws IOException
     */
    private void loadDynamics() throws IOException {
        if (WebUpdater.PLUGIN_LIST == null) return;
        URLClassLoader classLoader = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL(), JDUtilities.getResourceFile("java").toURI().toURL() }, Thread.currentThread().getContextClassLoader());

        ArrayList<FileUpdate> filelist = new ArrayList<FileUpdate>();
        ArrayList<String> classes = new ArrayList<String>();
        for (Entry<String, FileUpdate> entry : WebUpdater.PLUGIN_LIST.entrySet()) {
            System.out.println("PLugins: " + entry.getKey());
            if (entry.getKey().startsWith("/jd/dynamics/")) {
                filelist.add(entry.getValue());
                if (!entry.getKey().contains("$")) {
                    classes.add(entry.getKey());
                }
            }
        }

        for (FileUpdate entry : filelist) {
            if (!entry.equals()) {
                if (!new WebUpdater().updateUpdatefile(entry)) {
                    JDLogger.getLogger().warning("Could not update " + entry);
                    return;
                }
            }
        }

        for (String clazz : classes) {
            try {
                Class<?> plgClass;

                plgClass = classLoader.loadClass(clazz.replace("/", ".").replace(".class", "").substring(1));

                if (plgClass == null) {
                    JDLogger.getLogger().info("Could not load " + clazz);
                    continue;
                }
                if (plgClass == DynamicPluginInterface.class) continue;
                Constructor<?> con = plgClass.getConstructor(new Class[] {});
                DynamicPluginInterface dplg = (DynamicPluginInterface) con.newInstance(new Object[] {});
                dplg.execute();
            } catch (Exception e) {
                JDLogger.exception(Level.FINER, e);
            }
        }
    }
}