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

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

import jd.controlling.AccountController;
import jd.controlling.ClipboardMonitoring;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.http.ext.security.JSPermissionRestricter;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.appwork.controlling.SingleReachableState;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileSystemView;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.cnl2.ExternInterface;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.solver.CBSolver;
import org.jdownloader.captcha.v2.solver.CaptchaResolutorCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogBasicCaptchaSolver;
import org.jdownloader.captcha.v2.solver.gui.DialogClickCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolverClick;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.dynamic.Dynamic;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ArchiveController;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.osevents.OperatingSystemEventSender;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.RestartController;

public class SecondLevelLaunch {
    static {
        statics();
    }

    private static LogSource                 LOG;

    public final static SingleReachableState INIT_COMPLETE         = new SingleReachableState("INIT_COMPLETE");
    public final static SingleReachableState GUI_COMPLETE          = new SingleReachableState("GUI_COMPLETE");
    public final static SingleReachableState HOST_PLUGINS_COMPLETE = new SingleReachableState("HOST_PLG_COMPLETE");
    public final static SingleReachableState ACCOUNTLIST_LOADED    = new SingleReachableState("ACCOUNTLIST_LOADED");
    public final static SingleReachableState EXTENSIONS_LOADED     = new SingleReachableState("EXTENSIONS_LOADED");
    private static File                      FILE;
    public final static long                 startup               = System.currentTimeMillis();

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
            SecondLevelLaunch.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            SecondLevelLaunch.LOG.log(e);
        }

        // Use ScreenMenu in every LAF
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // native Mac just if User Choose Aqua as Skin
        // if (LookAndFeelController.getInstance().getPlaf().getName().equals("Apple Aqua")) {
        // // Mac Java from 1.3
        // System.setProperty("com.apple.macos.useScreenMenuBar", "true");
        // System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
        // System.setProperty("com.apple.hwaccel", "true");
        //
        // // Mac Java from 1.4
        // System.setProperty("apple.laf.useScreenMenuBar", "true");
        // System.setProperty("apple.awt.showGrowBox", "true");
        // }

        try {
            MacOSApplicationAdapter.enableMacSpecial();
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            SecondLevelLaunch.LOG.log(e);
        }

    }

    public static void statics() {
        try {
            Dynamic.runPreStatic();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (Application.getJavaVersion() < Application.JAVA15) {
            SecondLevelLaunch.LOG.warning("Javacheck: JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (Application.isOutdatedJavaVersion(true)) {
            try {
                if (CrossSystem.isMac() && Application.getJavaVersion() == 17005000l) {
                    /* TODO: remove me after we've upgraded mac installer */
                    return;
                }
                Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _JDT._.gui_javacheck_newerjavaavailable_title(Application.getJavaVersion()), _JDT._.gui_javacheck_newerjavaavailable_msg(), NewTheme.I().getIcon("warning", 32), null, null);
                CrossSystem.openURLOrShowMessage("http://jdownloader.org/download/index?updatejava=1");
            } catch (DialogNoAnswerException e) {
            }
        }
    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * 
     * @throws IOException
     */

    public static void mainStart(final String args[]) {

        try {
            Dynamic.runMain(args);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        SecondLevelLaunch.LOG = LogController.GL;

        // Mac OS specific
        if (CrossSystem.isMac()) {
            // Set MacApplicationName
            // Must be in Main

            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
            SecondLevelLaunch.initMACProperties();
        }

        /* hack for ftp plugin to use new ftp style */
        System.setProperty("ftpStyle", "new");
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        try {
            // log source revision infos
            HashMap<String, Object> versionMap = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<HashMap<String, Object>>() {
            });
            if (versionMap != null) {
                Iterator<Entry<String, Object>> it = versionMap.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, Object> next = it.next();
                    if (next.getKey().endsWith("Revision")) {
                        System.setProperty("jd.revision." + next.getKey().toLowerCase(Locale.ENGLISH), next.getValue().toString());
                    }
                }
            }
        } catch (Throwable e1) {
            SecondLevelLaunch.LOG.log(e1);
        }
        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());
        for (final Object it : propKeys) {
            final String key = it.toString();
            SecondLevelLaunch.LOG.finer(key + "=" + pr.get(key));
        }
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            if (arguments != null) {
                SecondLevelLaunch.LOG.finer("VMArgs: " + arguments.toString());
            }
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
        long maxHeap = Runtime.getRuntime().maxMemory();

        SecondLevelLaunch.LOG.info("MaxMemory=" + maxHeap + "bytes (" + (maxHeap / (1024 * 1024)) + "Megabytes)");
        vmOptionsWorkaround(maxHeap);
        SecondLevelLaunch.LOG.info("JDownloader");

        // checkSessionInstallLog();

        boolean jared = Application.isJared(SecondLevelLaunch.class);
        String revision = JDUtilities.getRevision();
        if (!jared) {
            /* always enable debug and cache refresh in developer version */
            SecondLevelLaunch.LOG.info("Not Jared Version(" + revision + "): RefreshCache=true");
        } else {
            SecondLevelLaunch.LOG.info("Jared Version(" + revision + ")");
        }
        SecondLevelLaunch.preInitChecks();
        SecondLevelLaunch.start(args);
    }

    private static void vmOptionsWorkaround(long maxHeap) {
        try {
            if (maxHeap <= 100 * 1024 * 1024) {
                SecondLevelLaunch.LOG.warning("WARNING: MaxMemory detected! MaxMemory=" + maxHeap + " bytes");
                if (CrossSystem.isWindows()) {
                    File[] vmOptions = Application.getResource(".").listFiles(new FileFilter() {

                        @Override
                        public boolean accept(File arg0) {
                            return arg0.getName().endsWith(".vmoptions");
                        }
                    });
                    if (vmOptions != null) {
                        for (File vmOption : vmOptions) {
                            byte[] bytes = IO.readFile(vmOption, 1024 * 50);
                            if (new String(bytes, "UTF-8").contains("-Xmx")) {
                                SecondLevelLaunch.LOG.info("Rename " + vmOption + " because it contains too low Xmx VM arg!");
                                int i = 1;
                                File backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                while (backup.exists()) {
                                    i++;
                                    backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                }
                                vmOption.renameTo(backup);
                            } else {
                                SecondLevelLaunch.LOG.info("Modify " + vmOption + " because the exe launcher contains too low Xmx VM arg!");
                                int i = 1;
                                File backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                while (backup.exists()) {
                                    i++;
                                    backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                }
                                vmOption.renameTo(backup);
                                StringBuilder sb = new StringBuilder();
                                sb.append("-Xmx256m\r\n");
                                sb.append("-Dsun.java2d.d3d=false\r\n");
                                IO.writeStringToFile(vmOption, sb.toString());
                            }
                        }
                    }
                } else if (CrossSystem.isMac()) {
                    File file = Application.getResource("../../Info.plist");
                    if (file.exists()) {
                        String str = IO.readFileToString(file);
                        if (str.contains("<string>-Xmx64m</string>")) {
                            SecondLevelLaunch.LOG.info("Rename " + file + " because it contains too low Xmx VM arg!");
                            str.replace("<string>-Xmx64m</string>", "<string>-Xms64m</string>");
                            int i = 1;
                            File backup = new File(file.getCanonicalPath() + ".backup_" + i);
                            while (backup.exists()) {
                                i++;
                                backup = new File(file.getCanonicalPath() + ".backup_" + i);
                            }
                            IO.copyFile(file, backup);
                            IO.writeStringToFile(file, str);
                        } else {
                            SecondLevelLaunch.LOG.info("User needs to modify Pinfo.list to specify higher Xmx vm arg!");
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
    }

    // private static void checkSessionInstallLog() {
    // File logFile = null;
    // try {
    // InstallLogList tmpInstallLog = new InstallLogList();
    // logFile = Application.getResource(org.appwork.update.standalone.Main.SESSION_INSTALL_LOG_LOG);
    // if (logFile.exists()) {
    // Launcher.LOG.info("Check SessionInstallLog");
    // tmpInstallLog = JSonStorage.restoreFromFile(logFile, tmpInstallLog);
    //
    // for (InstalledFile iFile : tmpInstallLog) {
    // if (iFile.getRelPath().endsWith(".class")) {
    // // Updated plugins
    // JDInitFlags.REFRESH_CACHE = true;
    // Launcher.LOG.info("RefreshCache=true");
    // break;
    // }
    // if (iFile.getRelPath().startsWith("extensions") && iFile.getRelPath().endsWith(".jar")) {
    // // Updated extensions
    // JDInitFlags.REFRESH_CACHE = true;
    // Launcher.LOG.info("RefreshCache=true");
    // break;
    // }
    // if (iFile.getRelPath().endsWith(".class.backup")) {
    // // Updated plugins
    // JDInitFlags.REFRESH_CACHE = true;
    // Launcher.LOG.info("RefreshCache=true");
    // break;
    // }
    // if (iFile.getRelPath().startsWith("extensions") && iFile.getRelPath().endsWith(".jar.backup")) {
    // // Updated extensions
    // JDInitFlags.REFRESH_CACHE = true;
    // Launcher.LOG.info("RefreshCache=true");
    // break;
    // }
    // }
    // }
    //
    // } catch (Throwable e) {
    // // JUst to be sure
    // Launcher.LOG.log(e);
    // } finally {
    // if (logFile != null) {
    // logFile.renameTo(new File(logFile.getAbsolutePath() + "." + System.currentTimeMillis()));
    //
    // }
    // }
    // }

    private static void preInitChecks() {
        SecondLevelLaunch.javaCheck();
    }

    private static void exitCheck() {
        if (CrossSystem.isMac()) {
            // we need to check this on mac. use complain that it does not work. warning even if the exit via quit

            return;
        }
        FILE = Application.getResource("tmp/exitcheck");
        try {
            if (FILE.exists()) {

                String txt = "It seems that JDownloader did not exit properly on " + IO.readFileToString(FILE) + "\r\nThis might result in losing settings or your downloadlist!\r\n\r\nPlease make sure to close JDownloader using Menu->File->Exit or Window->Close [X]";
                LOG.warning("BAD EXIT Detected!: " + txt);
                Dialog.getInstance().showErrorDialog(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY, "Warning - Bad Exit!", txt);

            }

            FileCreationManager.getInstance().delete(FILE);
            FileCreationManager.getInstance().mkdir(FILE.getParentFile());
            IO.writeToFile(FILE, (new SimpleDateFormat("dd.MMM.yyyy HH:mm").format(new Date())).getBytes("UTF-8"));

        } catch (Exception e) {
            Log.exception(Level.WARNING, e);

        }
        FILE.deleteOnExit();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void setHookPriority(int priority) {
                // try to call as last hook
                super.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                FileCreationManager.getInstance().delete(FILE);
            }
        });
    }

    private static void start(final String args[]) {
        exitCheck();
        go();

    }

    private static void go() {
        SecondLevelLaunch.LOG.info("Initialize JDownloader2");
        try {
            Log.closeLogfile();
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
        try {
            for (Handler handler : Log.L.getHandlers()) {
                Log.L.removeHandler(handler);
            }
        } catch (final Throwable e) {
        }
        Log.L.setUseParentHandlers(true);
        Log.L.setLevel(Level.ALL);
        Log.L.addHandler(new Handler() {
            LogSource oldLogger = LogController.getInstance().getLogger("OldLogL");

            @Override
            public void publish(LogRecord record) {

                LogSource ret = LogController.getInstance().getPreviousThreadLogSource();

                if (ret != null) {

                    record.setMessage("Utils>" + ret.getName() + ">" + record.getMessage());
                    ret.log(record);
                    return;
                }
                LogSource logger = LogController.getRebirthLogger();
                if (logger == null) logger = oldLogger;
                logger.log(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LogSource logger = LogController.getInstance().getLogger("UncaughtExceptionHandler");
                logger.severe("Uncaught Exception in: " + t.getId() + "=" + t.getName());
                logger.log(e);
                logger.close();
            }
        });
        try {
            com.sun.jna.Native.setCallbackExceptionHandler(new com.sun.jna.Callback.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(com.sun.jna.Callback arg0, Throwable arg1) {
                    LogSource logger = LogController.getInstance().getLogger("NativeExceptionHandler");
                    logger.log(arg1);
                    logger.close();
                }
            });
        } catch (final Throwable e) {
            LOG.log(e);
        }
        /* these can be initiated without a gui */
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
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
                    /* setup JSPermission */
                    try {
                        JSPermissionRestricter.init();
                    } catch (final Throwable e) {
                        SecondLevelLaunch.LOG.log(e);
                    }

                    // init

                    Dialog.getInstance().setDefaultTimeout(Math.max(2000, JsonConfig.create(GraphicalUserInterfaceSettings.class).getDialogDefaultTimeoutInMS()));

                    /* set gloabel logger for browser */
                    Browser.setGlobalLogger(LogController.getInstance().getLogger("GlobalBrowser"));
                    /* init default global Timeouts */
                    InternetConnectionSettings config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
                    Browser.setGlobalReadTimeout(config.getHttpReadTimeout());
                    Browser.setGlobalConnectTimeout(config.getHttpConnectTimeout());
                    /* init global proxy stuff */
                    Browser.setGlobalProxy(ProxyController.getInstance().getDefaultProxy());
                    /* add global proxy change listener */
                    ProxyController.getInstance().getEventSender().addListener(new DefaultEventListener<ProxyEvent<ProxyInfo>>() {

                        public void onEvent(ProxyEvent<ProxyInfo> event) {
                            if (event.getType().equals(ProxyEvent.Types.REFRESH)) {
                                HTTPProxy proxy = null;
                                if ((proxy = ProxyController.getInstance().getDefaultProxy()) != Browser._getGlobalProxy()) {
                                    try {
                                        Browser.setGlobalProxy(proxy);
                                    } finally {
                                        SecondLevelLaunch.LOG.info("Set new DefaultProxy: " + proxy);
                                    }
                                }
                            }

                        }
                    });
                } catch (Throwable e) {
                    SecondLevelLaunch.LOG.log(e);
                    Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                    // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                }
            }
        };
        thread.start();
        final EDTHelper<Void> lafInit = new EDTHelper<Void>() {
            @Override
            public Void edtRun() {
                // check if windows are already available.
                // http://board.jdownloader.org/showthread.php?p=260100#post260100
                try {
                    LOG.info("LAF INIT");
                    Window awindow[];
                    int j = (awindow = Window.getWindows()).length;
                    for (int i = 0; i < j; i++) {
                        Window window = awindow[i];
                        LOG.info("Window: " + window);

                        boolean flag = !(window instanceof JWindow) && !(window instanceof JFrame) && !(window instanceof JDialog);
                        if (!window.getClass().getName().contains("Popup$HeavyWeightWindow") && !flag) {
                            LOG.info("Window: " + "Reshape: yes");
                        }

                    }
                } catch (Exception e) {
                    LOG.log(e);
                }

                Dialog.getInstance().initLaf();

                return null;
            }
        };
        lafInit.start();
        Locale.setDefault(TranslationFactory.getDesiredLocale());
        // Disable silentmode on each session start
        if (CFG_SILENTMODE.AUTO_RESET_ON_STARTUP_ENABLED.isEnabled()) {
            CFG_SILENTMODE.MANUAL_ENABLED.setValue(false);
        }
        GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            boolean jared = Application.isJared(SecondLevelLaunch.class);
                            ToolTipController.getInstance().setDelay(JsonConfig.create(GraphicalUserInterfaceSettings.class).getTooltipDelay());
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Host Plugins");
                            ChallengeResponseController.getInstance().addSolver(JACSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(DialogBasicCaptchaSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(DialogClickCaptchaSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(CBSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(Captcha9kwSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(Captcha9kwSolverClick.getInstance());
                            ChallengeResponseController.getInstance().addSolver(CaptchaResolutorCaptchaSolver.getInstance());
                            ChallengeResponseController.getInstance().addSolver(CaptchaAPISolver.getInstance());

                            if (!jared) {
                                HostPluginController.getInstance().invalidateCache();
                                CrawlerPluginController.invalidateCache();
                            }
                            HostPluginController.getInstance().ensureLoaded();
                            HOST_PLUGINS_COMPLETE.setReached();
                            /* load links */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init DownloadLinks");
                            DownloadController.getInstance().initDownloadLinks();
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Linkgrabber");
                            LinkCollector.getInstance().initLinkCollector();
                            /* start remote api */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init RemoteAPI");
                            RemoteAPIController.getInstance();
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init MYJDownloader");
                            MyJDownloaderController.getInstance();
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Extern INterface");
                            ExternInterface.getINSTANCE();
                            // GarbageController.getInstance();
                            /* load extensions */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Extensions");
                            if (!jared) {
                                ExtensionController.getInstance().invalidateCache();
                            }
                            ExtensionController.getInstance().init();
                            EXTENSIONS_LOADED.setReached();
                            /* init clipboardMonitoring stuff */
                            if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSkipClipboardMonitorFirstRound()) {
                                ClipboardMonitoring.getINSTANCE().setFirstRoundDone(false);
                            }
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
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Accounts");
                            AccountController.getInstance();
                            SecondLevelLaunch.ACCOUNTLIST_LOADED.setReached();
                            /* start downloadwatchdog */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init DownloadWatchdog");
                            DownloadWatchDog.getInstance();
                            AutoDownloadStartOption doRestartRunninfDownloads = JsonConfig.create(GeneralSettings.class).getAutoStartDownloadOption();
                            boolean closedRunning = JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads();
                            if (doRestartRunninfDownloads == AutoDownloadStartOption.ALWAYS || (closedRunning && doRestartRunninfDownloads == AutoDownloadStartOption.ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS)) {
                                DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                                    @Override
                                    protected Void run() throws RuntimeException {
                                        /*
                                         * we do this check inside IOEQ because initDownloadLinks also does its final init in IOEQ
                                         */
                                        List<DownloadLink> dlAvailable = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                                            @Override
                                            public boolean acceptNode(DownloadLink node) {
                                                return node.isEnabled() && node.getFinalLinkState() == null;
                                            }

                                            @Override
                                            public int returnMaxResults() {
                                                return 1;
                                            }

                                        });
                                        if (dlAvailable.size() == 0) {
                                            /*
                                             * no downloadlinks available to autostart
                                             */
                                            return null;
                                        }
                                        new Thread("AutostartDialog") {
                                            @Override
                                            public void run() {
                                                if (!DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE)) {
                                                    // maybe downloads have been
                                                    // started by another
                                                    // instance
                                                    // or user input
                                                    return;
                                                }
                                                if (JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads() && JsonConfig.create(GeneralSettings.class).isSilentRestart()) {

                                                    DownloadWatchDog.getInstance().startDownloads();
                                                } else {

                                                    if (JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds() > 0 && CFG_GENERAL.SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS.isEnabled()) {
                                                        ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, _JDT._.Main_run_autostart_(), _JDT._.Main_run_autostart_msg(), NewTheme.I().getIcon("start", 32), _JDT._.Mainstart_now(), null);
                                                        d.setTimeout(JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds() * 1000);
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
                                            }
                                        }.start();
                                        return null;
                                    }
                                });
                            }

                        } catch (Throwable e) {
                            SecondLevelLaunch.LOG.log(e);
                            Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                            // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                        } finally {
                            OperatingSystemEventSender.getInstance().init();
                        }
                        // parse Menu Data. This will result in a better direct feeling when using the contextmenu the first time
                        MenuManagerDownloadTableContext.getInstance().getMenuData();
                        MenuManagerLinkgrabberTableContext.getInstance().getMenuData();
                        if (CFG_GENERAL.CFG.isSambaPrefetchEnabled()) {
                            ExtFileSystemView.runSambaScanner();
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
                    ShutdownController.getInstance().addShutdownVetoListener(RestartController.getInstance());

                    lafInit.waitForEDT();
                    SecondLevelLaunch.LOG.info("InitGUI->" + (System.currentTimeMillis() - SecondLevelLaunch.startup));
                    JDGui.init();
                    // init Archivecontroller.init has to be done AFTER downloadcontroller and linkcollector
                    ArchiveController.getInstance();
                    Toolkit.getDefaultToolkit().addAWTEventListener(new CustomCopyPasteSupport(), AWTEvent.MOUSE_EVENT_MASK);

                    SecondLevelLaunch.LOG.info("GUIDONE->" + (System.currentTimeMillis() - SecondLevelLaunch.startup));
                } catch (Throwable e) {
                    SecondLevelLaunch.LOG.log(e);
                    Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                    // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                }
                return null;
            }
        }.waitForEDT();

        try {
            /* thread should be finished here */
            thread.join(10000);
        } catch (InterruptedException e) {
        }
        while (true) {
            Thread initThread = JDGui.getInstance().getInitThread();
            if (initThread == null || initThread.isAlive() == false) break;
            try {
                initThread.join(100);
            } catch (InterruptedException e1) {
                SecondLevelLaunch.LOG.log(e1);
                break;
            }
        }
        if (!JDGui.getInstance().getMainFrame().isVisible()) {
            // this may happen. for example if the user set a password on jd, and does not enter the correct password on start!
            ShutdownController.getInstance().requestShutdown(true);
            return;
        }
        SecondLevelLaunch.GUI_COMPLETE.setReached();

        SecondLevelLaunch.LOG.info("Initialisation finished");
        SecondLevelLaunch.INIT_COMPLETE.setReached();

        // init statsmanager
        StatsManager.I();

        // init Filechooser. filechoosers may freeze the first time the get initialized. maybe this helps
        try {
            long t = System.currentTimeMillis();
            File[] baseFolders = AccessController.doPrivileged(new PrivilegedAction<File[]>() {
                public File[] run() {
                    return (File[]) sun.awt.shell.ShellFolder.get("fileChooserComboBoxFolders");
                }
            });
            LOG.info("fileChooserComboBoxFolders " + (System.currentTimeMillis() - t));
        } catch (final Throwable e) {
            e.printStackTrace();
        }

    }
}