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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.UserIF;
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.http.ext.security.JSPermissionRestricter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.CommandSwitchListener;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.controlling.SingleReachableState;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.update.inapp.RestartController;
import org.appwork.update.inapp.RlyExitListener;
import org.appwork.update.inapp.WebupdateSettings;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.singleapp.AnotherInstanceRunningException;
import org.appwork.utils.singleapp.InstanceMessageListener;
import org.appwork.utils.singleapp.SingleAppInstance;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.ExternInterface;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.dynamic.Dynamic;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.gui.uiserio.JDSwingUserIO;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.images.NewTheme;
import org.jdownloader.jdserv.stats.StatsManager;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.translate._JDT;
import org.jdownloader.update.JDUpdater;

public class Launcher {
    static {
        try {
            statics();
        } catch (Throwable e) {
            e.printStackTrace();
            RestartController.getInstance().restartViaUpdater(false);
            // TODO: call Updater.jar
        }
    }

    private static Logger              LOG;
    private static boolean             instanceStarted            = false;
    public static SingleAppInstance    SINGLE_INSTANCE_CONTROLLER = null;

    public static SingleReachableState INIT_COMPLETE              = new SingleReachableState("INIT_COMPLETE");
    public static SingleReachableState GUI_COMPLETE               = new SingleReachableState("GUI_COMPLETE");
    private static ParameterParser     PARAMETERS;
    public final static long           startup                    = System.currentTimeMillis();

    // private static JSonWrapper webConfig;

    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        // set DockIcon (most used in Building)
        try {
            com.apple.eawt.Application.getApplication().setDockIconImage(NewTheme.I().getImage("logo/jd_logo_128_128", -1));
        } catch (final Throwable e) {
            /* not every mac has this */
            Launcher.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            e.printStackTrace();
        }

        // Use ScreenMenu in every LAF
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // native Mac just if User Choose Aqua as Skin
        if (LookAndFeelController.getInstance().getPlaf().getName().equals("Apple Aqua")) {
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
            Launcher.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            e.printStackTrace();
        }

    }

    public static void statics() {

        try {
            Dynamic.runPreStatic();
        } catch (Throwable e) {
            e.printStackTrace();

        }

        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());
        // do this call to keep the correct root in Application Cache

        NewUIO.setUserIO(new JDSwingUserIO());
        RlyExitListener.getInstance().setEnabled(true);
        RestartController.getInstance().setApp("JDownloader.app");
        RestartController.getInstance().setExe("JDownloader.exe");
        RestartController.getInstance().setJar("JDownloader.jar");
        RestartController.getInstance().setUpdaterJar("Updater.jar");
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (Application.getJavaVersion() < Application.JAVA15) {
            Launcher.LOG.warning("Javacheck: Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (Application.isOutdatedJavaVersion(true)) {
            try {
                Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, _JDT._.gui_javacheck_newerjavaavailable_title(Application.getJavaVersion()), _JDT._.gui_javacheck_newerjavaavailable_msg(), NewTheme.I().getIcon("warning", 32), null, null);
                CrossSystem.openURLOrShowMessage("http://jdownloader.org/download/index?updatejava=1");
            } catch (DialogNoAnswerException e) {
            }
        }
    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * @throws IOException
     */

    public static void mainStart(final String args[]) {

        try {
            Dynamic.runMain(args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Launcher.LOG = JDLogger.getLogger();
        // Mac OS specific
        if (CrossSystem.isMac()) {
            // Set MacApplicationName
            // Must be in Main
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
            Launcher.initMACProperties();
        }
        /* hack for ftp plugin to use new ftp style */
        System.setProperty("ftpStyle", "new");
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        // Disable the GUI rendering on the graphic card
        System.setProperty("sun.java2d.d3d", "false");
        try {
            // log source revision infos
            Log.L.info(IO.readFileToString(Application.getResource("build.json")));
        } catch (IOException e1) {
            Log.exception(e1);
        }
        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());
        for (final Object it : propKeys) {
            final String key = it.toString();
            Launcher.LOG.finer(key + "=" + pr.get(key));
        }
        Launcher.LOG.info("Start JDownloader");
        PARAMETERS = new ParameterParser(args);

        PARAMETERS.getEventSender().addListener(new CommandSwitchListener() {

            @Override
            public void executeCommandSwitch(CommandSwitch event) {

                if (event.getSwitchCommand().equalsIgnoreCase("forcelog")) {
                    JDInitFlags.SWITCH_FORCELOG = true;
                    Launcher.LOG.info("FORCED LOGGING Modus aktiv");
                }
                if (event.getSwitchCommand().equalsIgnoreCase("debug")) {
                    JDInitFlags.SWITCH_DEBUG = true;
                    Launcher.LOG.info("DEBUG Modus aktiv");
                }

                if (event.getSwitchCommand().equalsIgnoreCase("brdebug")) {
                    JDInitFlags.SWITCH_DEBUG = true;
                    Browser.setGlobalVerbose(true);
                    Launcher.LOG.info("Browser DEBUG Modus aktiv");

                }
                if (event.getSwitchCommand().equalsIgnoreCase("scan") || event.getSwitchCommand().equalsIgnoreCase("rfu")) {
                    JDInitFlags.REFRESH_CACHE = true;
                }
                if (event.getSwitchCommand().equalsIgnoreCase("trdebug")) {
                    JDL.DEBUG = true;
                    Launcher.LOG.info("Translation DEBUG Modus aktiv");
                }
                if (event.getSwitchCommand().equalsIgnoreCase("rfu")) {
                    JDInitFlags.SWITCH_RETURNED_FROM_UPDATE = true;
                }
            }
        });
        PARAMETERS.parse(null);
        RestartController.getInstance().setStartArguments(PARAMETERS.getRawArguments());

        if (!Application.isJared(Launcher.class)) {
            JDInitFlags.SWITCH_DEBUG = true;
        }

        Launcher.preInitChecks();

        for (int i = 0; i < args.length; i++) {

            if (args[i].equalsIgnoreCase("-branch")) {

                if (args[i + 1].equalsIgnoreCase("reset")) {
                    JDUpdater.getInstance().setForcedBranch(null);

                    Launcher.LOG.info("Switching back to default JDownloader branch");

                } else {
                    JDUpdater.getInstance().setForcedBranch(args[i + 1]);

                    Launcher.LOG.info("Switching to " + args[i + 1] + " JDownloader branch");

                }

                i++;
            } else if (args[i].equals("-prot")) {

                Launcher.LOG.finer(args[i] + " " + args[i + 1]);
                i++;

            } else if (args[i].equals("--new-instance") || args[i].equals("-n")) {

                Launcher.LOG.finer(args[i] + " parameter");
                JDInitFlags.SWITCH_NEW_INSTANCE = true;

            } else if (args[i].equals("--help") || args[i].equals("-h")) {

                ParameterManager.showCmdHelp();
                System.exit(0);

            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {

                    Launcher.LOG.setLevel(Level.OFF);
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

            }

        }
        try {
            Launcher.SINGLE_INSTANCE_CONTROLLER = new SingleAppInstance("JD", JDUtilities.getJDHomeDirectoryFromEnvironment());
            Launcher.SINGLE_INSTANCE_CONTROLLER.setInstanceMessageListener(new InstanceMessageListener() {
                public void parseMessage(final String[] args) {
                    ParameterManager.processParameters(args, false);
                }
            });
            Launcher.SINGLE_INSTANCE_CONTROLLER.start();
            Launcher.instanceStarted = true;
        } catch (final AnotherInstanceRunningException e) {
            Launcher.LOG.info("existing jD instance found!");
            Launcher.instanceStarted = false;
        } catch (final Exception e) {
            JDLogger.exception(e);
            Launcher.LOG.severe("Instance Handling not possible!");
            Launcher.instanceStarted = true;
        }

        if (Launcher.instanceStarted || JDInitFlags.SWITCH_NEW_INSTANCE) {
            Launcher.start(args);
        } else {
            if (args.length > 0) {
                Launcher.LOG.info("Send parameters to existing jD instance and exit");
                Launcher.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(args);
            } else {
                Launcher.LOG.info("There is already a running jD instance");
                Launcher.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(new String[] { "--focus" });
            }
            System.exit(0);
        }

    }

    private static void preInitChecks() {
        Launcher.javaCheck();
    }

    public static boolean returnedfromUpdate() {
        return JDInitFlags.SWITCH_RETURNED_FROM_UPDATE;
    }

    private static void start(final String args[]) {
        if (!JDInitFlags.STOP) {
            go();
            for (final String p : args) {
                Launcher.LOG.finest("Param: " + p);
            }
            ParameterManager.processParameters(args, true);
        }
    }

    private static void go() {
        Launcher.LOG.info(new Date().toString());
        Launcher.LOG.info("init Configuration");
        if (JDInitFlags.SWITCH_DEBUG) {
            Launcher.LOG.info("DEBUG MODE ACTIVATED");
            // new PerformanceObserver().start();
            Launcher.LOG.setLevel(Level.ALL);
            Log.L.setLevel(Level.ALL);
        } else {
            JDLogger.removeConsoleHandler();
        }

        CFG_GENERAL.BROWSER_COMMAND_LINE.getEventSender().addListener(new GenericConfigEventListener<String[]>() {

            @Override
            public void onConfigValidatorError(KeyHandler<String[]> keyHandler, String[] invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<String[]> keyHandler, String[] newValue) {
                CrossSystem.setBrowserCommandLine(newValue);
            }
        });
        CrossSystem.setBrowserCommandLine(CFG_GENERAL.BROWSER_COMMAND_LINE.getValue());
        /* these can be initiated without a gui */
        final Thread thread = new Thread() {
            @Override
            public void run() {
                /* setup JSPermission */
                try {
                    JSPermissionRestricter.init();
                } catch (final Throwable e) {
                    Log.exception(e);
                }
                /* set gloabel logger for browser */
                Browser.setGlobalLogger(JDLogger.getLogger());
                /* init default global Timeouts */
                Browser.setGlobalReadTimeout(JsonConfig.create(GeneralSettings.class).getHttpReadTimeout());
                Browser.setGlobalConnectTimeout(JsonConfig.create(GeneralSettings.class).getHttpConnectTimeout());
                /* init global proxy stuff */
                Browser.setGlobalProxy(ProxyController.getInstance().getDefaultProxy());
                /* add global proxy change listener */
                ProxyController.getInstance().getEventSender().addListener(new DefaultEventListener<ProxyEvent<ProxyInfo>>() {

                    public void onEvent(ProxyEvent<ProxyInfo> event) {
                        if (event.getType().equals(ProxyEvent.Types.REFRESH)) {
                            HTTPProxy proxy = null;
                            if ((proxy = ProxyController.getInstance().getDefaultProxy()) != Browser._getGlobalProxy()) {
                                Log.L.info("Set new DefaultProxy: " + proxy);
                                Browser.setGlobalProxy(proxy);
                            }
                        }

                    }
                });
            }
        };
        thread.start();
        new EDTHelper<Void>() {
            @Override
            public Void edtRun() {
                LookAndFeelController.getInstance().setUIManager();
                return null;
            }
        }.waitForEDT();
        Locale.setDefault(Locale.ENGLISH);
        GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        HostPluginController.getInstance().ensureLoaded();
                        /* load links */
                        DownloadController.getInstance().initDownloadLinks();
                        LinkCollector.getInstance().initLinkCollector();
                        /* start remote api */
                        RemoteAPIController.getInstance();
                        ExternInterface.getINSTANCE();
                        // GarbageController.getInstance();
                        /* load extensions */
                        ExtensionController.getInstance().init();
                        /* init clipboardMonitoring stuff */
                        if (org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled()) {
                            ClipboardMonitoring.getINSTANCE().startMonitoring();
                        }
                        org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                                if (Boolean.TRUE.equals(newValue) && ClipboardMonitoring.getINSTANCE().isMonitoring() == false) {
                                    ClipboardMonitoring.getINSTANCE().startMonitoring();
                                } else {
                                    ClipboardMonitoring.getINSTANCE().stopMonitoring();
                                }
                            }

                            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                            }
                        });
                        /* check for available updates */
                        // activate auto checker only if we are in jared mode
                        if (JsonConfig.create(WebupdateSettings.class).isAutoUpdateCheckEnabled() && Application.isJared(Launcher.class)) {
                            JDUpdater.getInstance().startChecker();
                        }
                        /* start downloadwatchdog */
                        DownloadWatchDog.getInstance();
                        AutoDownloadStartOption doRestartRunninfDownloads = JsonConfig.create(GeneralSettings.class).getAutoStartDownloadOption();
                        boolean closedRunning = JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads();
                        if (doRestartRunninfDownloads == AutoDownloadStartOption.ALWAYS || (closedRunning && doRestartRunninfDownloads == AutoDownloadStartOption.ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS)) {
                            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                                @Override
                                protected Void run() throws RuntimeException {
                                    /*
                                     * we do this check inside IOEQ because
                                     * initDownloadLinks also does its final
                                     * init in IOEQ
                                     */
                                    List<DownloadLink> dlAvailable = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                                        @Override
                                        public boolean isChildrenNodeFiltered(DownloadLink node) {
                                            return node.isEnabled() && node.getLinkStatus().hasStatus(LinkStatus.TODO);
                                        }

                                        @Override
                                        public int returnMaxResults() {
                                            return 1;
                                        }

                                    });
                                    if (dlAvailable.size() == 0) {
                                        /*
                                         * no downloadlinks available to
                                         * autostart
                                         */
                                        return null;
                                    }
                                    new Thread("AutostartDialog") {
                                        @Override
                                        public void run() {
                                            if (JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds() > 0 && CFG_GENERAL.SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS.isEnabled()) {
                                                ConfirmDialog d = new ConfirmDialog(Dialog.LOGIC_COUNTDOWN, _JDT._.Main_run_autostart_(), _JDT._.Main_run_autostart_msg(), NewTheme.I().getIcon("start", 32), _JDT._.Mainstart_now(), null);
                                                d.setCountdownTime(JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds());
                                                try {
                                                    Dialog.getInstance().showDialog(d);
                                                    DownloadWatchDog.getInstance().startDownloads();
                                                } catch (DialogNoAnswerException e) {
                                                    if (e.isCausedByTimeout()) {
                                                        DownloadWatchDog.getInstance().startDownloads();
                                                    }
                                                }
                                            } else {
                                                DownloadWatchDog.getInstance().startDownloads();
                                            }
                                        }
                                    }.start();
                                    return null;
                                }
                            });
                        }
                    }
                }.start();
            }

        });
        new EDTHelper<Void>() {
            @Override
            public Void edtRun() {
                /* init gui here */
                try {
                    Log.L.info("Init Gui");
                    JDGui.getInstance();
                    EDTEventQueue.initEventQueue();
                    Log.L.info("GUIDONE->" + (System.currentTimeMillis() - Launcher.startup));
                } catch (Throwable e) {
                    Log.exception(e);
                    Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                    RestartController.getInstance().restartViaUpdater(false);
                }
                return null;
            }
        }.waitForEDT();
        /* this stuff can happen outside edt */
        SwingGui.setInstance(JDGui.getInstance());
        UserIF.setInstance(SwingGui.getInstance());
        try {
            /* thread should be finished here */
            thread.join(10000);
        } catch (InterruptedException e) {
        }
        Launcher.GUI_COMPLETE.setReached();
        Launcher.LOG.info("Initialisation finished");
        Launcher.LOG.info("Revision: " + JDUtilities.getRevision());
        Launcher.LOG.info("Jared: " + Application.isJared(Launcher.class));
        Launcher.INIT_COMPLETE.setReached();

        // init statsmanager
        StatsManager.I();
    }
}