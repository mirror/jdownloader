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
import java.lang.reflect.Field;
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
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.appwork.console.ConsoleDialog;
import org.appwork.controlling.SingleReachableState;
import org.appwork.resources.AWUTheme;
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
import org.appwork.utils.IOErrorHandler;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.SlowEDTDetector;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileSystemView;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.cnl2.ExternInterface;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ArchiveController;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.osevents.OperatingSystemEventSender;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;
import org.jdownloader.scripting.JSPermissionRestricter;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.RlyWarnLevel;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.gui.LAFOptions;

public class SecondLevelLaunch {
    static {

        statics();

    }

    private static LogSource                 LOG;
    public final static SingleReachableState UPDATE_HANDLER_SET    = new SingleReachableState("UPDATE_HANDLER_SET");
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

        new Thread() {
            public void run() {
                try {
                    LOG.info("Try to disable AppNap");

                    // single bundle installer
                    File file = getInfoPlistPath();
                    String cFBundleIdentifier = null;
                    if (file != null && file.exists()) {
                        cFBundleIdentifier = new Regex(IO.readFileToString(file), "<key>CFBundleIdentifier</key>.*?<string>(.+?)</string>").getMatch(0);

                    }

                    LOG.info("MAC Bundle Identifier: " + cFBundleIdentifier);
                    if (cFBundleIdentifier == null) {

                        cFBundleIdentifier = "org.jdownloader.launcher";

                        LOG.info("Use MAC Default Bundle Identifier: " + cFBundleIdentifier);
                    }
                    if (StringUtils.isNotEmpty(cFBundleIdentifier)) {
                        ProcessBuilder p = ProcessBuilderFactory.create("defaults", "write", cFBundleIdentifier, "NSAppSleepDisabled", "-bool", "YES");
                        Process process = null;
                        try {
                            process = p.start();
                            String ret = IO.readInputStreamToString(process.getInputStream());
                            LOG.info("Disable App Nap");
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            }
                        }
                        p = ProcessBuilderFactory.create("defaults", "read", cFBundleIdentifier);
                        try {
                            process = p.start();
                            String ret = IO.readInputStreamToString(process.getInputStream());
                            LOG.info("App Defaults: \r\n" + ret);
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOG.log(e);
                }
            };
        }.start();

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

    protected static File getInfoPlistPath() {

        String ownBundlePath = System.getProperty("i4j.ownBundlePath");
        if (StringUtils.isNotEmpty(ownBundlePath) && ownBundlePath.endsWith(".app")) {

            // folder installer
            File file = new File(new File(ownBundlePath), "Contents/Info.plist");
            if (file.exists()) {
                return file;
            }
        }

        // old singlebundle installer
        File file = Application.getResource("../../Info.plist");
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static void statics() {
        /**
         * The sorting algorithm used by java.util.Arrays.sort and (indirectly) by java.util.Collections.sort has been replaced. The new
         * sort implementation may throw an IllegalArgumentException if it detects a Comparable that violates the Comparable contract. The
         * previous implementation silently ignored such a situation. If the previous behavior is desired, you can use the new system
         * property, java.util.Arrays.useLegacyMergeSort, to restore previous mergesort behavior. Nature of Incompatibility: behavioral RFE:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6804124
         * 
         * Sorting live data (values changing during sorting) violates the general contract
         * 
         * java.lang.IllegalArgumentException: Comparison method violates its general contract!
         */
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (Application.getResource("disableJavaCheck").exists()) {
            return;
        }
        if (Application.getJavaVersion() < Application.JAVA15) {
            SecondLevelLaunch.LOG.warning("Javacheck: JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (Application.isOutdatedJavaVersion(true)) {
            try {
                SecondLevelLaunch.LOG.severe("BUGGY Java Version detected: " + Application.getJavaVersion());
                if (CrossSystem.isMac() && Application.getJavaVersion() >= 17005000l && Application.getJavaVersion() <= 17006000l) {
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
     * LÃ¤dt ein Dynamicplugin.
     * 
     * 
     * @throws IOException
     */

    public static void mainStart(final String args[]) {

        SecondLevelLaunch.LOG = LogController.GL;
        /* setup JSPermission */
        try {

            JSPermissionRestricter.init();

        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
        if (System.getProperty("nativeswing") != null) {
            // JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        }
        try {
            JSHtmlUnitPermissionRestricter.init();

        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
        // Mac OS specific
        if (CrossSystem.isMac()) {
            // Set MacApplicationName
            // Must be in Main
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
            SecondLevelLaunch.initMACProperties();
        } else if (CrossSystem.isLinux()) {
            // set WM Class explicitly
            try {
                if (!org.appwork.utils.Application.isHeadless()) {
                    // patch by Vampire
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    final Field awtAppClassName = Toolkit.getDefaultToolkit().getClass().getDeclaredField("awtAppClassName");
                    awtAppClassName.setAccessible(true);
                    awtAppClassName.set(toolkit, "JDownloader");
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }

        /* hack for ftp plugin to use new ftp style */
        System.setProperty("ftpStyle", "new");
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        try {
            System.setProperty("org.jdownloader.revision", "JDownloader2(Beta)");
            // log source revision infos
            HashMap<String, Object> versionMap = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), TypeRef.HASHMAP);
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
        SecondLevelLaunch.LOG.info("JavaVersion=" + Application.getJavaVersion());
        long maxHeap = -1;
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            if (arguments != null) {
                SecondLevelLaunch.LOG.finer("VMArgs: " + arguments.toString());
            }
            java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            maxHeap = memory.getMax();
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        }
        SecondLevelLaunch.LOG.info("MaxMemory=" + maxHeap + "bytes (" + (maxHeap / (1024 * 1024)) + "Megabytes)");
        vmOptionsWorkaround(maxHeap);
        SecondLevelLaunch.LOG.info("JDownloader2");

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
        final IOErrorHandler errorHandler = IO.getErrorHandler();
        try {
            IO.setErrorHandler(null);
            if (maxHeap > 0 && maxHeap <= 100 * 1024 * 1024) {
                SecondLevelLaunch.LOG.warning("WARNING: MaxMemory detected! MaxMemory=" + maxHeap + " bytes");
                if (CrossSystem.isWindows() || CrossSystem.isLinux()) {
                    java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                    List<String> arguments = runtimeMxBean.getInputArguments();
                    boolean xmxArgFound = false;
                    for (String arg : arguments) {
                        if (arg != null && arg.startsWith("-Xmx")) {
                            xmxArgFound = true;
                            break;
                        }
                    }
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
                                while (backup.exists() || i == 10) {
                                    i++;
                                    backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                }
                                if (backup.exists()) {
                                    backup.delete();
                                }
                                vmOption.renameTo(backup);
                            } else {
                                SecondLevelLaunch.LOG.info("Modify " + vmOption + " because the exe launcher contains too low Xmx VM arg!");
                                int i = 1;
                                File backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                while (backup.exists() || i == 10) {
                                    i++;
                                    backup = new File(vmOption.getAbsolutePath() + ".backup_" + i);
                                }
                                if (backup.exists()) {
                                    backup.delete();
                                }
                                if (vmOption.renameTo(backup)) {
                                    StringBuilder sb = new StringBuilder();
                                    if (CrossSystem.isWindows()) {
                                        sb.append("-Xmx256m\r\n");
                                        sb.append("-Dsun.java2d.d3d=false\r\n");
                                    } else if (CrossSystem.isLinux()) {
                                        sb.append("-Xmx256m\n\n");
                                    }
                                    if (vmOption.exists() == false || vmOption.delete()) {
                                        IO.writeStringToFile(vmOption, sb.toString());
                                    }
                                }
                            }
                        }
                    }
                    if (xmxArgFound) {
                        String launcher = System.getProperty("exe4j.launchName");
                        if (StringUtils.isEmpty(launcher)) {
                            launcher = System.getProperty("exe4j.moduleName");
                        }
                        SecondLevelLaunch.LOG.info("Create .vmoptions for " + launcher + " because the exe launcher contains too low Xmx VM arg!");
                        if (StringUtils.isNotEmpty(launcher)) {
                            if (CrossSystem.isWindows()) {
                                launcher = launcher.replaceFirst("\\.exe$", ".vmoptions");
                            } else {
                                launcher = launcher + ".vmoptions";
                            }
                            File vmOption = new File(launcher);
                            StringBuilder sb = new StringBuilder();
                            if (CrossSystem.isWindows()) {
                                sb.append("-Xmx256m\r\n");
                                sb.append("-Dsun.java2d.d3d=false\r\n");
                            } else if (CrossSystem.isLinux()) {
                                sb.append("-Xmx256m\n\n");
                            }
                            if (vmOption.exists() == false || vmOption.delete()) {
                                IO.writeStringToFile(vmOption, sb.toString());
                            }
                        }
                    }
                } else if (CrossSystem.isMac()) {
                    File file = getInfoPlistPath();
                    if (file != null && file.exists()) {

                        String str = IO.readFileToString(file);
                        boolean writeChanges = false;
                        if (Application.getJavaVersion() >= 17005000l && Application.getJavaVersion() <= 17006000l) {
                            /* in 1.7 update 5, Xms does not work, we need to specify Xmx */
                            if (str.contains("<string>-Xmx64m</string>")) {
                                str = str.replace("<string>-Xmx64m</string>", "<string>-Xmx256m</string>");
                                writeChanges = true;
                            } else if (str.contains("<string>-Xms64m</string>")) {
                                str = str.replace("<string>-Xms64m</string>", "<string>-Xmx256m</string>");
                                writeChanges = true;
                            }
                            if (writeChanges) {
                                SecondLevelLaunch.LOG.info("Workaround for buggy Java 1.7 update 5");
                            }
                        } else if (str.contains("<string>-Xmx64m</string>")) {
                            str = str.replace("<string>-Xmx64m</string>", "<string>-Xms64m</string>");
                            writeChanges = true;
                        }
                        if (writeChanges) {
                            SecondLevelLaunch.LOG.info("Modify " + file + " because it contains too low Xmx VM arg!");
                            if (!isMacLauncherSigned()) {
                                int i = 1;
                                File backup = new File(file.getCanonicalPath() + ".backup_" + i);
                                while (backup.exists() || i == 10) {
                                    i++;
                                    backup = new File(file.getCanonicalPath() + ".backup_" + i);
                                }
                                if (backup.exists()) {
                                    backup.delete();
                                }
                                IO.copyFile(file, backup);
                                if (file.exists() == false || file.delete()) {
                                    IO.writeStringToFile(file, str);
                                }
                            } else {
                                SecondLevelLaunch.LOG.info("Cannot modify, because the Laucher is signed. User has to reinstall.");
                            }
                        } else {
                            SecondLevelLaunch.LOG.info("User needs to modify Pinfo.list to specify higher Xmx vm arg!");
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            SecondLevelLaunch.LOG.log(e);
        } finally {
            IO.setErrorHandler(errorHandler);
        }
    }

    private static boolean isMacLauncherSigned() {
        String ownBundlePath = System.getProperty("i4j.ownBundlePath");
        if (StringUtils.isNotEmpty(ownBundlePath) && ownBundlePath.endsWith(".app")) {

            // folder installer
            File file = new File(new File(ownBundlePath), "Contents/﻿_CodeSignature");
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    private static void preInitChecks() {
        SecondLevelLaunch.javaCheck();
    }

    private static void exitCheck() {

        if (CrossSystem.isMac()) {
            // we need to check this on mac. use complain that it does not work. warning even if the exit via quit

            return;
        }

        FILE = Application.getTempResource("exitcheck");

        try {
            if (FILE.exists()) {
                final String error = IO.readFileToString(FILE);
                // we need an extra thread. else this blocking dialog would block the startup process and cause the update launcher to think
                // that starting jd failed.
                new Thread("ShowBadExit") {
                    public void run() {
                        String txt = "It seems that JDownloader did not exit properly on " + error + "\r\nThis might result in losing settings or your downloadlist!\r\n\r\nPlease make sure to close JDownloader using Menu->File->Exit or Window->Close [X]";
                        LOG.warning("BAD EXIT Detected!: " + txt);

                        if (!Application.isHeadless()) {
                            UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY, "Warning - Bad Exit!", txt, AWUTheme.I().getIcon(Dialog.ICON_ERROR, 32), null, null);
                        }

                    };
                }.start();

            }

            FileCreationManager.getInstance().delete(FILE, null);
            FileCreationManager.getInstance().mkdir(FILE.getParentFile());
            IO.writeToFile(FILE, (new SimpleDateFormat("dd.MMM.yyyy HH:mm").format(new Date())).getBytes("UTF-8"));

        } catch (Exception e) {
            Log.exception(Level.WARNING, e);

        }
        FILE.deleteOnExit();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public String toString() {
                return "ShutdownEvent: Delete " + FILE;
            }

            @Override
            public void setHookPriority(int priority) {
                // try to call as last hook
                super.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                FileCreationManager.getInstance().delete(FILE, null);
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
                if (logger == null) {
                    logger = oldLogger;
                }
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
            System.setProperty("jna.debug_load", "true");
            System.setProperty("jna.debug_load.jna", "true");
            System.setProperty("jna.nosystrue", "true");
            Application.getResource("tmp/jna").mkdir();
            System.setProperty("jna.tmpdir", Application.getResource("tmp/jna").getAbsolutePath());
            com.sun.jna.Native.setCallbackExceptionHandler(new com.sun.jna.Callback.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(com.sun.jna.Callback arg0, Throwable arg1) {
                    LogSource logger = LogController.getInstance().getLogger("NativeExceptionHandler");
                    logger.log(arg1);
                    logger.close();
                }
            });
        } catch (final Throwable e1) {
            LOG.log(e1);

        }
        LogSource edtLogger = LogController.getInstance().getLogger("BlockingEDT");
        edtLogger.setInstantFlush(true);
        if (!Application.isHeadless() && Application.isJared(SecondLevelLaunch.class)) {
            new SlowEDTDetector(10000l, edtLogger);
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

                    // init

                    Dialog.getInstance().setDefaultTimeout(Math.max(2000, JsonConfig.create(GraphicalUserInterfaceSettings.class).getDialogDefaultTimeoutInMS()));

                    /* set gloabel logger for browser */
                    Browser.setGlobalLogger(LogController.getInstance().getLogger("GlobalBrowser"));
                    /* init default global Timeouts */
                    InternetConnectionSettings config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
                    Browser.setGlobalReadTimeout(config.getHttpReadTimeout());
                    Browser.setGlobalConnectTimeout(config.getHttpConnectTimeout());
                    /* init global proxy stuff */
                    Browser.setGlobalProxy(ProxyController.getInstance());

                    if (CFG_GENERAL.CFG.isWindowsJNAIdleDetectorEnabled() && CrossSystem.isWindows()) {
                        try {
                            /* speed up the init of the following libs */
                            com.sun.jna.platform.win32.User32 u = com.sun.jna.platform.win32.User32.INSTANCE;
                            com.sun.jna.platform.win32.Kernel32 k = com.sun.jna.platform.win32.Kernel32.INSTANCE;
                        } catch (final Throwable e) {
                            SecondLevelLaunch.LOG.log(e);
                        }
                    }
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
        if (!org.appwork.utils.Application.isHeadless()) {
            lafInit.start();
        } else {
            LAFOptions.init("org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel");
        }
        Locale.setDefault(TranslationFactory.getDesiredLocale());
        // Disable silentmode on each session start
        if (CFG_SILENTMODE.AUTO_RESET_ON_STARTUP_ENABLED.isEnabled()) {
            CFG_SILENTMODE.MANUAL_ENABLED.setValue(false);
        }
        // reset speed limit enabled on start
        if (!CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_REMEMBERED_ENABLED.isEnabled()) {
            CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(false);
        }
        GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            final boolean jared = Application.isJared(SecondLevelLaunch.class);
                            if (!Application.isHeadless()) {
                                new EDTHelper<Void>() {

                                    @Override
                                    public Void edtRun() {
                                        ToolTipController.getInstance().setDelay(JsonConfig.create(GraphicalUserInterfaceSettings.class).getTooltipDelay());
                                        return null;
                                    }

                                }.start(true);
                            }
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Host Plugins");
                            HostPluginController.getInstance().ensureLoaded();
                            HOST_PLUGINS_COMPLETE.setReached();
                            PackagizerController.getInstance();
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
                            new Thread("Init ExtensionController") {
                                public void run() {
                                    Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Extensions");
                                    if (!jared) {
                                        ExtensionController.getInstance().invalidateCache();
                                    }
                                    ExtensionController.getInstance().init();
                                    EXTENSIONS_LOADED.setReached();
                                };
                            }.start();
                            new Thread("Init AccountController") {
                                public void run() {
                                    AccountController.getInstance();
                                    SecondLevelLaunch.ACCOUNTLIST_LOADED.setReached();
                                };
                            }.start();

                            /* start downloadwatchdog */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init DownloadWatchdog");
                            DownloadWatchDog.getInstance();
                            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                                @Override
                                public void run() {
                                    new Thread("Init ChallengeResponseController") {
                                        public void run() {
                                            /* init clipboardMonitoring stuff */
                                            if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSkipClipboardMonitorFirstRound()) {
                                                ClipboardMonitoring.setFirstRoundDone(false);
                                            }
                                            if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isClipboardMonitorProcessHTMLFlavor()) {
                                                ClipboardMonitoring.setHtmlFlavorAllowed(false);
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
                                            ChallengeResponseController.getInstance().init();

                                        };
                                    }.start();
                                }
                            });
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
                            if (Application.isHeadless()) {
                                ConsoleDialog.showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                            } else {
                                Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                                // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                            }
                        } finally {
                            OperatingSystemEventSender.getInstance().init();
                        }

                        if (!Application.isHeadless() && CFG_GENERAL.CFG.isSambaPrefetchEnabled()) {
                            ExtFileSystemView.runSambaScanner();
                        }
                    }
                }.start();
            }
        });

        if (CFG_GUI.CFG.getRlyWarnLevel() == RlyWarnLevel.HIGH) {
            ShutdownController.getInstance().addShutdownVetoListener(RestartController.getInstance());
        }
        CFG_GUI.RLY_WARN_LEVEL.getEventSender().addListener(new GenericConfigEventListener<Enum>() {

            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                if (CFG_GUI.CFG.getRlyWarnLevel() == RlyWarnLevel.HIGH) {
                    ShutdownController.getInstance().addShutdownVetoListener(RestartController.getInstance());
                } else {
                    ShutdownController.getInstance().removeShutdownVetoListener(RestartController.getInstance());
                }

            }

            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }
        });
        if (!Application.isHeadless()) {
            new EDTHelper<Void>() {
                @Override
                public Void edtRun() {
                    /* init gui here */
                    try {

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
        }

        try {
            /* thread should be finished here */
            thread.join(10000);
        } catch (InterruptedException e) {
        }
        if (!Application.isHeadless()) {
            while (true) {
                Thread initThread = JDGui.getInstance().getInitThread();
                if (initThread == null || initThread.isAlive() == false) {
                    break;
                }
                try {
                    initThread.join(100);
                } catch (InterruptedException e1) {
                    SecondLevelLaunch.LOG.log(e1);
                    break;
                }
            }
            JDGui.getInstance().badLaunchCheck();
        }

        SecondLevelLaunch.GUI_COMPLETE.setReached();
        SecondLevelLaunch.LOG.info("Initialisation finished");
        SecondLevelLaunch.INIT_COMPLETE.setReached();

        // init statsmanager
        StatsManager.I();
        switch (CrossSystem.getOS()) {
        case WINDOWS_XP:
            try {
                LOG.info("AntiVirusProduct");
                LOG.info(ProcessBuilderFactory.runCommand("wmic", "/NAMESPACE:\\\\root\\SecurityCenter", "path", "AntiVirusProduct").getStdOutString("UTF-8"));
            } catch (Throwable e1) {
                LOG.log(e1);
            }
            try {
                LOG.info("FirewallProduct");
                LOG.info(ProcessBuilderFactory.runCommand("wmic", "/NAMESPACE:\\\\root\\SecurityCenter", "path", "FirewallProduct").getStdOutString("UTF-8"));
            } catch (Throwable e1) {
                LOG.log(e1);
            }
        case WINDOWS_7:
        case WINDOWS_8:
        case WINDOWS_VISTA:
            try {
                LOG.info("AntiVirusProduct");
                LOG.info(ProcessBuilderFactory.runCommand("wmic", "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "AntiVirusProduct").getStdOutString("UTF-8"));
            } catch (Throwable e1) {
                LOG.log(e1);
            }
            try {
                LOG.info("FirewallProduct");
                LOG.info(ProcessBuilderFactory.runCommand("wmic", "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "FirewallProduct").getStdOutString("UTF-8"));
            } catch (Throwable e1) {
                LOG.log(e1);
            }
            try {
                LOG.info("AntiSpywareProduct");
                LOG.info(ProcessBuilderFactory.runCommand("wmic", "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "AntiSpywareProduct").getStdOutString("UTF-8"));
            } catch (Throwable e1) {
                LOG.log(e1);
            }
        }

        // init Filechooser. filechoosers may freeze the first time the get initialized. maybe this helps
        if (!Application.isHeadless()) {
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
}