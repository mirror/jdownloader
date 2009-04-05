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

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.event.ControlEvent;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JDEventQueue;
import jd.gui.skins.simple.JDToolBar;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.update.WebUpdater;
import jd.utils.CheckJava;
import jd.utils.JDFileReg;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MacOSController;
import jd.utils.WebUpdate;

/**
 * @author astaldo/JD-Team
 */

public class Main {

    private static Logger LOGGER;
    private static SplashScreen splashScreen;

    public static String getCaptcha(String path, String host) {

        boolean hasMethod = JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host);

        if (hasMethod) {

            File file;

            if (path.contains("http://")) {
                try {
                    Browser br = new Browser();
                    URLConnectionAdapter httpConnection;
                    httpConnection = br.openGetConnection(path);

                    if (httpConnection.getLongContentLength() == -1 || httpConnection.getLongContentLength() == 0) return "Could not download captcha image";

                    String seperator = "/";

                    if (System.getProperty("os.name").toLowerCase().contains("win") || System.getProperty("os.name").toLowerCase().contains("nt")) {
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
            Image captchaImage = new JFrame().getToolkit().getImage(file.getAbsolutePath());
            MediaTracker mediaTracker = new MediaTracker(jf);
            mediaTracker.addImage(captchaImage, 0);

            try {
                mediaTracker.waitForID(0);
            } catch (InterruptedException e) {
                return e.getStackTrace().toString();
            }

            mediaTracker.removeImage(captchaImage);
            JAntiCaptcha jac = new JAntiCaptcha(JDUtilities.getJACMethodsDirectory(), host);
            Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captcha);
            file.delete();

            return captchaCode;

        } else {

            return "jDownloader has no method for " + host;

        }

    }

    public static boolean returnedfromUpdate() {
        return JDInitFlags.SWITCH_RETURNED_FROM_UPDATE;
    }

    public static void main(String args[]) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        LOGGER = JDUtilities.getLogger();
        initMACProperties();

        for (String p : args) {
            if (p.equalsIgnoreCase("-debug")) {
                JDInitFlags.SWITCH_DEBUG = true;
            }
            if (p.equalsIgnoreCase("-rfb")) {
                JDInitFlags.SWITCH_RETURNED_FROM_UPDATE = true;
            }
        }
        System.out.println("JJJJ");
        preInitChecks();

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-prot")) {

                LOGGER.info(args[i] + " " + args[i + 1]);
                i++;

            } else if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                if (!JDInitFlags.ENOUGH_MEMORY) {
                    JDUtilities.restartJD(args);
                }

                LOGGER.info(args[i] + " parameter");
                JDInitFlags.SWITCH_NEW_INSTANCE = true;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                Server.showCmdHelp();
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
        splashScreen = null;
        JDTheme.setTheme("default");
        if (JDInitFlags.SHOW_SPLASH) {
            if (JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_SHOW_SPLASH, true)) {
                new GuiRunnable() {

                    @Override
                    public Object runSave() {
                        try {
                            splashScreen = new SplashScreen(JDTheme.I("gui.splash"));

                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.languages", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.settings", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.controller", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.update", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.plugins", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.screen", 32, 32)));
                            splashScreen.addProgressImage(new SplashProgressImage(JDTheme.I("gui.splash.dllist", 32, 32)));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (AWTException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return null;
                    }

                }.waitForEDT();

            }
        }
        Interaction.initTriggers();
        Main.setSplashStatus(splashScreen, 10, JDLocale.L("gui.splash.text.loadLanguage", "lade Sprachen"));

       
        JDSounds.setSoundTheme("default");

        if (!JDInitFlags.SWITCH_NEW_INSTANCE && Main.tryConnectToServer(args)) {

            if (args.length > 0) {

                LOGGER.info("Send parameters to existing jD instance and exit");
                System.exit(0);

            } else {

                LOGGER.info("There is already a running jD instance");
                Main.tryConnectToServer(new String[] { "--focus" });
                System.exit(0);

            }

        } else {

            if (!JDInitFlags.STOP && !JDInitFlags.ENOUGH_MEMORY) {
                JDUtilities.restartJD(args);
            }

            try {

                // listen for command line arguments from new jD instances //
                final Server server = new Server();
                server.go();
                final String[] processArgs = args;

                if (!JDInitFlags.STOP) {

                    final Main main = new Main();

                    EventQueue.invokeLater(new Runnable() {
                        public void run() {

                            Toolkit.getDefaultToolkit().getSystemEventQueue().push(new JDEventQueue());
                            main.go();
                            for (String p : processArgs) {
                                LOGGER.severe("Param: " + p);
                            }
                            // post start parameters //
                            try {
                                server.processParameters(processArgs);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }

            } catch (RemoteException e) {

                LOGGER.severe("Server could not be started - ignore parameters");
                e.printStackTrace();

                if (!JDInitFlags.STOP) {

                    Main main = new Main();
                    main.go();

                    for (String p : args) {
                        LOGGER.severe("Param: " + p);
                    }

                }

            }

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
        }
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (!CheckJava.check()) {
            System.out.println("Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }

    }

    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        // Mac specific //
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            LOGGER.info("apple.laf.useScreenMenuBar=true");
            LOGGER.info("com.apple.mrj.application.growbox.intrudes=false");
            LOGGER.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            new MacOSController();
        }

    }

    private static void setSplashStatus(SplashScreen splashScreen, int i, String l) {
        // System.out.println(l);
        if (splashScreen == null) { return; }

        splashScreen.setNextImage();
        // splashScreen.setText(l);
        // splashScreen.setValue(splashScreen.getValue() + i);

    }

    private static Boolean tryConnectToServer(String args[]) {

        String url = "//127.0.0.1/jDownloader";

        try {
            Properties p = System.getProperties();
            p.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "100");
            ServerInterface server = (ServerInterface) Naming.lookup(url);
            server.processParameters(args);
            return true;

        } catch (Exception ex) {

            return false;

        }

    }

    @SuppressWarnings("unchecked")
    private void go() {
        final JDInit init = new JDInit(splashScreen);
        LOGGER.info("Register plugins");
        init.init();
        init.loadImages();

        Main.setSplashStatus(splashScreen, 10, JDLocale.L("gui.splash.text.configLoaded", "Lade Konfiguration"));
     
        String old = JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getStringProperty("LOCALE", null);
        if (old != null) {
            JDUtilities.getSubConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, old);
            JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).setProperty("LOCALE", null);
            JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).save();
            JDUtilities.getSubConfig(JDLocale.CONFIG).save();
        }
        if (init.loadConfiguration() == null) {

            JOptionPane.showMessageDialog(null, "JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }
        

        // JFrame.setDefaultLookAndFeelDecorated(true);
        // JDialog.setDefaultLookAndFeelDecorated(true);
        if (JDInitFlags.SWITCH_DEBUG) {
            JDUtilities.getLogger().setLevel(Level.ALL);
        }
        // JDInit.setupProxy();
        // JDInit.setupSocks();
        WebUpdater.getConfig("WEBUPDATE").save();
        init.removeFiles();

        Main.setSplashStatus(splashScreen, 10, JDLocale.L("gui.splash.text.initcontroller", "Starte Controller"));

        final JDController controller = init.initController();

        if (JDInitFlags.SWITCH_DEBUG || JDUtilities.getConfiguration().getBooleanProperty(Configuration.LOGGER_FILELOG, false)) {
            try {
                File log = JDUtilities.getResourceFile("logs/" + (JDInitFlags.SWITCH_DEBUG ? "debug" : "") + "log_" + System.currentTimeMillis() + ".log");
                if (!log.getParentFile().exists()) {
                    log.getParentFile().mkdirs();
                }
                controller.setLogFileWriter(new BufferedWriter(new FileWriter(log)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Main.setSplashStatus(splashScreen, 10, JDLocale.L("gui.splash.text.webupdate", "Check Updates"));
        new WebUpdate().doWebupdate(false);

        Main.setSplashStatus(splashScreen, 15, JDLocale.L("gui.splash.text.loadPlugins", "Lade Plugins"));
        init.initPlugins();

        Main.setSplashStatus(splashScreen, 20, JDLocale.L("gui.splash.text.loadGUI", "Lade Benutzeroberfläche"));
        new GuiRunnable() {

            @Override
            public Object runSave() {
                init.initGUI(controller);
                return null;
            }

        }.waitForEDT();

        Main.setSplashStatus(splashScreen, 20, JDLocale.L("gui.splash.text.loaddownloadqueue", "Lade Downloadliste"));
        init.initDownloadController();

        Main.setSplashStatus(splashScreen, 100, JDLocale.L("gui.splash.text.finished", "Fertig"));

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
        LOGGER.info("Runtype: " + JDUtilities.getRunType());

        try {
            splashScreen.finish();
        } catch (Exception e) {
            // TODO: handle exception
        }
        init.checkUpdate();

        Level level = JDUtilities.getLogger().getLevel();
        // logger.info(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).
        // getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS,
        // false).toString());
        if ((JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) && (JDUtilities.getConfiguration().getBooleanProperty(Configuration.LOGGER_FILELOG, false) || level.equals(Level.ALL) || level.equals(Level.FINER) || level.equals(Level.FINE)) && !JDInitFlags.SWITCH_DEBUG && (!JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_DISABLE_CONFIRM_DIALOGS, false))) {
            JDUtilities.getGUI().showHelpMessage(JDLocale.L("main.start.logwarning.title", "Logwarnung"), JDLocale.LF("main.start.logwarning.body", "ACHTUNG. Das Loglevel steht auf %s und der Dateischreiber ist %s. \r\nDiese Einstellungen belasten das System und sind nur zur Fehlersuche geeignet.", level.getName(), JDUtilities.getConfiguration().getBooleanProperty(Configuration.LOGGER_FILELOG, false) ? JDLocale.L("main.status.active", "an") : JDLocale.L("main.status.inactive", "aus")), true, JDLocale.L("main.urls.faq", "http://jdownloader.org/faq.php?lng=deutsch"), null, 10);
        }

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_INIT_COMPLETE, null));

        JDFileReg.registerFileExts();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new PackageManager().interact(this);

    }

}