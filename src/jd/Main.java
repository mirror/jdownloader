package jd;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JWindow;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.HTTPConnection;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author astaldo/JD-Team
 */

public class Main {

    private static Logger logger = JDUtilities.getLogger();

    public static void main(String args[]) {

        
//        int t=0;
//        for( t=0; t<200;t++)
//       JDUtilities.downloadBinary(JDUtilities.getResourceFile("cap/cap_"+t+".jpg").getAbsolutePath(), "http://www.fast-load.net/includes/captcha.php");
        Boolean newInstance = false;

        // pre start parameters //
        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                if (Runtime.getRuntime().maxMemory() < 100000000) {
                    JDUtilities.restartJD(args);
                }

                logger.info(args[i] + " parameter");

                newInstance = true;
                break;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                Server.showCmdHelp();
                System.exit(0);

            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {

                    logger.setLevel(Level.OFF);
                    String captchaValue = Main.getCaptcha(args[i + 1], args[i + 2]);
                    System.out.println("" + captchaValue);
                    System.exit(0);

                } else {

                    System.out.println("Error: Please define filepath and JAC method");
                    System.out.println("Usage: java -jar JDownloader.jar --captcha /path/file.png example.com");
                    System.exit(0);

                }

            }

        }

        // Mac specific //
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {

            logger.info("apple.laf.useScreenMenuBar=true");
            logger.info("com.apple.mrj.application.growbox.intrudes=false");
            logger.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");

        }

        JDTheme.setTheme("default");

        boolean stop = false;
        boolean extractSwitch = false;
        Vector<String> paths = new Vector<String>();
        long extractTime = 0;

        if (!newInstance && tryConnectToServer(args)) {

            if (args.length > 0) {

                logger.info("Send parameters to existing jD instance and exit");
                System.exit(0);

            } else {

                logger.info("There is already a running jD instance");
                tryConnectToServer(new String[] { "--focus" });
                System.exit(0);

            }

        } else {

            for (String currentArg : args) {

                if (currentArg.equals("--show") || currentArg.equals("-s")) {

                    JACController.showDialog(false);
                    extractSwitch = false;
                    stop = true;

                } else if (currentArg.equals("--train") || currentArg.equals("-t")) {

                    JACController.showDialog(true);
                    extractSwitch = false;
                    stop = true;

                } else if (currentArg.equals("--extract") || currentArg.equals("-e")) {

                    extractSwitch = true;
                    stop = true;

                } else if (extractSwitch) {

                    if (currentArg.equals("--rotate") || currentArg.equals("-r")) {

                        extractTime = -1;

                    } else if (extractTime == -1) {

                        if (currentArg.matches("[\\d]+")) {
                            extractTime = (int) Integer.parseInt(currentArg);
                        } else
                            extractTime = 0;

                    } else if (!currentArg.matches("[\\s]*")) {

                        paths.add(currentArg);

                    }

                } else {

                    extractSwitch = false;

                }

            }

            if (extractSwitch) {

                logger.info("Extract: [" + paths.toString() + " | " + extractTime + "]");
                Server.extract(paths, extractTime, true);

            }

            if (!stop && Runtime.getRuntime().maxMemory() < 100000000) {
                JDUtilities.restartJD(args);
            }

            try {

                // listen for command line arguments from new jD instances //
                Server server = new Server();
                server.go();

                if (!stop) {

                    Main main = new Main();
                    main.go();

                    // post start parameters //
                    server.processParameters(args);

                }

            } catch (RemoteException e) {

                logger.severe("Server could not be started - ignore parameters");
                e.printStackTrace();

                if (!stop) {

                    Main main = new Main();
                    main.go();

                }

            }

        }

    }

    @SuppressWarnings("unchecked")
    private void go() {

        JDInit init = new JDInit();
        logger.info("Register plugins");
        init.init();
        init.loadImages();

        JWindow window = new JWindow() {

            private static final long serialVersionUID = 1L;

            public void paint(Graphics g) {
                Image splashImage = JDUtilities.getImage("jd_logo_large");
                g.drawImage(splashImage, 0, 0, this);
            }

        };

        window.setSize(450, 100);
        window.setLocationRelativeTo(null);

        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_SHOW_SPLASH, true)) {
            window.setVisible(true);
        }

        init.loadConfiguration();
        init.setupProxy();
        
        init.removeFiles();

        /*
         * Übergangsfix. Die Interactions wurden in eine subconfig verlegt.
         * dieser teil kopiert bestehende events in die neue configfile
         */

        if (JDUtilities.getConfiguration().getInteractions().size() > 0 && JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, null) == null) {

            JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).setProperty(Configuration.PARAM_INTERACTIONS, JDUtilities.getConfiguration().getInteractions());
            JDUtilities.getConfiguration().setInteractions(new Vector<Interaction>());
            JDUtilities.saveConfig();

        }

        final JDController controller = init.initController();

        if (init.installerWasVisible()) {

            init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);

        } else {
            init.initPlugins();
            init.initGUI(controller);

            init.loadDownloadQueue();
            init.loadModules();
            init.checkUpdate();

            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), false);
            }

        }

        controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);
        // init.createQueueBackup();
        window.dispose();
        controller.getUiInterface().onJDInitComplete();
        Properties pr = System.getProperties();
        TreeSet propKeys = new TreeSet(pr.keySet());

        for (Iterator it = propKeys.iterator(); it.hasNext();) {
            String key = (String) it.next();
            logger.finer("" + key + "=" + pr.get(key));
        }

        logger.info("Revision: " + JDUtilities.getJDTitle());
        logger.info("Runtype: " + JDUtilities.getRunType());
        logger.info("Last author: " + JDUtilities.getLastChangeAuthor());
        logger.info("Application directory: " + JDUtilities.getCurrentWorkingDirectory(null));

        new PackageManager().interact(this);

        // org.apache.log4j.Logger lg = org.apache.log4j.Logger.getLogger("jd");
        // BasicConfigurator.configure();
        // lg.error("hallo Welt");
        // lg.setLevel(org.apache.log4j.Level.ALL);

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

    public static String getCaptcha(String path, String host) {

        boolean hasMethod = JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host);

        if (hasMethod) {

            File file;

            if (path.contains("http://")) {

                HTTPConnection httpConnection;

                try {
                    httpConnection = new HTTPConnection(new URL(path).openConnection());
                } catch (MalformedURLException e) {
                    return e.getStackTrace().toString();
                } catch (IOException e) {
                    return e.getStackTrace().toString();
                }

                if (httpConnection.getContentLength() == -1 || httpConnection.getContentLength() == 0) {

                return "Could not download captcha image";

                }

                String seperator = "/";

                if (System.getProperty("os.name").toLowerCase().contains("win") || System.getProperty("os.name").toLowerCase().contains("nt")) {
                    seperator = "\\";
                }

                String filepath = System.getProperty("user.dir") + seperator + "jac_captcha.img";
                file = new File(filepath);
                JDUtilities.download(file, path);

            } else {

                file = new File(path);
                if (!file.exists()) return "File does not exist";

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

}