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
import java.awt.Dialog.ModalityType;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

import jd.controlling.AccountController;
import jd.controlling.ClipboardMonitoring;
import jd.controlling.DelayWriteController;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.ProxyController;
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.nutils.zip.SharedMemoryState;
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
import org.appwork.tools.ide.iconsetmaker.IconSetMakerAdapter;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.ExceptionDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.IOErrorHandler;
import org.appwork.utils.JarHandlerWorkaround;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.os.SecuritySoftwareException;
import org.appwork.utils.os.SecuritySoftwareInfo;
import org.appwork.utils.os.SecuritySoftwareResponse;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SlowEDTDetector;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExceptionDialog;
import org.appwork.utils.swing.dialog.ExtFileSystemView;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.cnl2.ExternInterface;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ArchiveController;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.jna.windows.JNAWindowsWindowManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.net.BCTLSSocketStreamFactory;
import org.jdownloader.osevents.OperatingSystemEventSender;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.scripting.JSHtmlUnitPermissionRestricter;
import org.jdownloader.scripting.JSRhinoPermissionRestricter;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.CLIPBOARD_SKIP_MODE;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.RlyWarnLevel;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.RestartController;

import com.btr.proxy.selector.pac.PACScriptEngineFactory;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptParser;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.ProxyEvaluationException;
import com.btr.proxy.selector.pac.RhinoPacScriptParser;

public class SecondLevelLaunch {
    static {
        statics();
    }
    public final static SingleReachableState UPDATE_HANDLER_SET    = new SingleReachableState("UPDATE_HANDLER_SET");
    public final static SingleReachableState INIT_COMPLETE         = new SingleReachableState("INIT_COMPLETE");
    public final static SingleReachableState GUI_COMPLETE          = new SingleReachableState("GUI_COMPLETE");
    public final static SingleReachableState HOST_PLUGINS_COMPLETE = new SingleReachableState("HOST_PLG_COMPLETE");
    public final static SingleReachableState ACCOUNTLIST_LOADED    = new SingleReachableState("ACCOUNTLIST_LOADED");
    public final static SingleReachableState EXTENSIONS_LOADED     = new SingleReachableState("EXTENSIONS_LOADED");
    public static File                       FILE;
    public final static long                 startup               = System.currentTimeMillis();

    // private static JSonWrapper webConfig;
    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
        // set DockIcon (most used in Building)
        try {
            com.apple.eawt.Application.getApplication().setDockIconImage(NewTheme.I().getImage("logo/jd_logo_128_128", -1));
        } catch (final Throwable e) {
            /* not every mac has this */
            LoggerFactory.getDefaultLogger().info("Error Initializing  Mac Look and Feel Special: " + e);
            LoggerFactory.getDefaultLogger().log(e);
        }
        // Use ScreenMenu in every LAF
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        new Thread() {
            public void run() {
                try {
                    LoggerFactory.getDefaultLogger().info("Try to disable AppNap");
                    // single bundle installer
                    File file = getInfoPlistPath();
                    String cFBundleIdentifier = null;
                    if (file != null && file.exists()) {
                        cFBundleIdentifier = new Regex(IO.readFileToString(file), "<key>CFBundleIdentifier</key>.*?<string>(.+?)</string>").getMatch(0);
                    }
                    LoggerFactory.getDefaultLogger().info("MAC Bundle Identifier: " + cFBundleIdentifier);
                    if (cFBundleIdentifier == null) {
                        cFBundleIdentifier = "org.jdownloader.launcher";
                        LoggerFactory.getDefaultLogger().info("Use MAC Default Bundle Identifier: " + cFBundleIdentifier);
                    }
                    if (StringUtils.isNotEmpty(cFBundleIdentifier)) {
                        ProcessBuilder p = ProcessBuilderFactory.create("defaults", "write", cFBundleIdentifier, "NSAppSleepDisabled", "-bool", "YES");
                        Process process = null;
                        try {
                            process = p.start();
                            String ret = IO.readInputStreamToString(process.getInputStream());
                            LoggerFactory.getDefaultLogger().info("Disable App Nap");
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
                            LoggerFactory.getDefaultLogger().info("App Defaults: \r\n" + ret);
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
                    LoggerFactory.getDefaultLogger().log(e);
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
            LoggerFactory.getDefaultLogger().info("Error Initializing  Mac Look and Feel Special: " + e);
            LoggerFactory.getDefaultLogger().log(e);
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
        JarHandlerWorkaround.init();
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
            LoggerFactory.getDefaultLogger().warning("Javacheck: JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (Application.isOutdatedJavaVersion(true)) {
            try {
                LoggerFactory.getDefaultLogger().severe("BUGGY Java Version detected: " + Application.getJavaVersion());
                if (CrossSystem.isMac() && Application.getJavaVersion() >= 17005000l && Application.getJavaVersion() <= 17006000l) {
                    /* TODO: remove me after we've upgraded mac installer */
                    return;
                }
                Dialog.getInstance().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _JDT.T.gui_javacheck_newerjavaavailable_title(Application.getJavaVersion()), _JDT.T.gui_javacheck_newerjavaavailable_msg(), new AbstractIcon(IconKey.ICON_WARNING, 32), null, null);
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
        /* setup JSPermission */
        try {
            // Ensure that proxyVole uses Rhino and not the build-in engines.
            // Rhino is Sandboxed by JSRhinoPermissionRestricter
            PacProxySelector.SCRIPT_ENGINE_FACTORY = new PACScriptEngineFactory() {
                @Override
                public PacScriptParser selectEngine(PacProxySelector selector, PacScriptSource pacSource) throws ProxyEvaluationException {
                    return new RhinoPacScriptParser(pacSource);
                }
            };
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        try {
            JSRhinoPermissionRestricter.init();
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        try {
            JSHtmlUnitPermissionRestricter.init();
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        // Mac OS specific
        if (CrossSystem.isMac()) {
            // Set MacApplicationName
            // Must be in Main
            SecondLevelLaunch.initMACProperties();
        } else if (CrossSystem.isUnix()) {
            initLinuxSpecials();
        } else if (CrossSystem.isWindows()) {
            initWindowsSpecials();
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
            LoggerFactory.getDefaultLogger().log(e1);
        }
        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());
        for (final Object it : propKeys) {
            final String key = it.toString();
            LoggerFactory.getDefaultLogger().finer(key + "=" + pr.get(key));
        }
        LoggerFactory.getDefaultLogger().info("JavaVersion=" + Application.getJavaVersion());
        long maxHeap = -1;
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            if (arguments != null) {
                LoggerFactory.getDefaultLogger().finer("VMArgs: " + arguments.toString());
            }
            java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            maxHeap = memory.getMax();
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        LoggerFactory.getDefaultLogger().info("MaxMemory=" + maxHeap + "bytes (" + (maxHeap / (1024 * 1024)) + "Megabytes)");
        if (!Application.isHeadless()) {
            vmOptionsWorkaround(maxHeap);
        }
        LoggerFactory.getDefaultLogger().info("JDownloader2");
        // checkSessionInstallLog();
        boolean jared = Application.isJared(SecondLevelLaunch.class);
        String revision = JDUtilities.getRevision();
        if (!jared) {
            /* always enable debug and cache refresh in developer version */
            LoggerFactory.getDefaultLogger().info("Non Jared Version(" + revision + "): RefreshCache=true");
        } else {
            LoggerFactory.getDefaultLogger().info("Jared Version(" + revision + ")");
        }
        SecondLevelLaunch.preInitChecks();
        SecondLevelLaunch.start(args);
    }

    private static void initWindowsSpecials() {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                try {
                    if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_7)) {
                        org.jdownloader.crosssystem.windows.WindowsApplicationAdapter.getInstance();
                    }
                } catch (Throwable e) {
                    Log.log(e);
                }
            }
        });
    }

    private static void initLinuxSpecials() {
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

    private static void vmOptionsWorkaround(long maxHeap) {
        final IOErrorHandler errorHandler = IO.getErrorHandler();
        try {
            IO.setErrorHandler(null);
            if (maxHeap > 0 && maxHeap <= 256 * 1024 * 1024) {
                LoggerFactory.getDefaultLogger().warning("WARNING: MaxMemory detected! MaxMemory=" + maxHeap + " bytes");
                if (CrossSystem.isWindows() || CrossSystem.isUnix()) {
                    final java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                    final List<String> arguments = runtimeMxBean.getInputArguments();
                    boolean xmxArgFound = false;
                    for (final String arg : arguments) {
                        if (arg != null && arg.startsWith("-Xmx")) {
                            xmxArgFound = true;
                            break;
                        }
                    }
                    final File[] vmOptions = Application.getResource(".").listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File arg0) {
                            return arg0.getName().endsWith(".vmoptions");
                        }
                    });
                    if (vmOptions != null) {
                        for (File vmOption : vmOptions) {
                            final byte[] bytes = IO.readFile(vmOption, 1024 * 50);
                            if (new String(bytes, "UTF-8").contains("-Xmx")) {
                                LoggerFactory.getDefaultLogger().info("Rename " + vmOption + " because it contains too low Xmx VM arg!");
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
                                LoggerFactory.getDefaultLogger().info("Modify " + vmOption + " because the exe launcher contains too low Xmx VM arg!");
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
                                    final StringBuilder sb = new StringBuilder();
                                    if (CrossSystem.isWindows()) {
                                        sb.append("-Xmx512m\r\n");
                                        sb.append("-Dsun.java2d.d3d=false\r\n");
                                    } else if (CrossSystem.isLinux()) {
                                        sb.append("-Xmx512m\n\n");
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
                        if (StringUtils.isNotEmpty(launcher)) {
                            LoggerFactory.getDefaultLogger().info("Create .vmoptions for " + launcher + " because the exe launcher contains too low Xmx VM arg!");
                            if (CrossSystem.isWindows()) {
                                launcher = launcher.replaceFirst("\\.exe$", ".vmoptions");
                            } else {
                                launcher = launcher + ".vmoptions";
                            }
                            final File vmOption = new File(launcher);
                            final StringBuilder sb = new StringBuilder();
                            if (CrossSystem.isWindows()) {
                                sb.append("-Xmx512m\r\n");
                                sb.append("-Dsun.java2d.d3d=false\r\n");
                            } else if (CrossSystem.isLinux()) {
                                sb.append("-Xmx512m\n\n");
                            }
                            if (vmOption.exists() == false || vmOption.delete()) {
                                IO.writeStringToFile(vmOption, sb.toString());
                            }
                        }
                    }
                } else if (CrossSystem.isMac()) {
                    final File file = getInfoPlistPath();
                    if (file != null && file.exists() && file.isFile()) {
                        String str = IO.readFileToTrimmedString(file);
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
                                LoggerFactory.getDefaultLogger().info("Workaround for buggy Java 1.7 update 5");
                            }
                        } else if (str.contains("<string>-Xmx64m</string>")) {
                            str = str.replace("<string>-Xmx64m</string>", "<string>-Xms64m</string>");
                            writeChanges = true;
                        }
                        if (writeChanges) {
                            LoggerFactory.getDefaultLogger().info("Modify " + file + " because it contains too low Xmx VM arg!");
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
                                LoggerFactory.getDefaultLogger().info("Cannot modify, because the Laucher is signed. User has to reinstall.");
                            }
                        } else {
                            LoggerFactory.getDefaultLogger().info("User needs to modify Pinfo.list to specify higher Xmx vm arg!");
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
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
                        LoggerFactory.getDefaultLogger().warning("BAD EXIT Detected!: " + txt);
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
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
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
        LoggerFactory.getDefaultLogger().info("Initialize JDownloader2");
        // try {
        // Log.closeLogfile();
        // } catch (final Throwable e) {
        // LoggerFactory.getDefaultLogger().log(e);
        // }
        // try {
        // for (Handler handler : org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().getHandlers()) {
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().removeHandler(handler);
        // }
        // } catch (final Throwable e) {
        // }
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().setUseParentHandlers(true);
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().setLevel(Level.ALL);
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().addHandler(new Handler() {
        // LogSource oldLogger = LogController.getInstance().getLogger("OldLogL");
        //
        // @Override
        // public void publish(LogRecord record) {
        //
        // LogSource ret = LogController.getInstance().getPreviousThreadLogSource();
        //
        // if (ret != null) {
        //
        // record.setMessage("Utils>" + ret.getName() + ">" + record.getMessage());
        // ret.log(record);
        // return;
        // }
        // LogSource logger = LogController.getRebirthLogger();
        // if (logger == null) {
        // logger = oldLogger;
        // }
        // logger.log(record);
        // }
        //
        // @Override
        // public void flush() {
        // }
        //
        // @Override
        // public void close() throws SecurityException {
        // }
        // });
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LogSource logger = LogController.getInstance().getLogger("UncaughtExceptionHandler");
                logger.severe("Uncaught Exception in: " + t.getId() + "=" + t.getName() + "|" + e.getMessage());
                logger.log(e);
                logger.close();
            }
        });
        try {
            System.setProperty("jna.debug_load", "true");
            System.setProperty("jna.debug_load.jna", "true");
            final String jnaNoSysKey = "jna.nosys";
            if (StringUtils.isEmpty(System.getProperty(jnaNoSysKey))) {
                System.setProperty(jnaNoSysKey, "true");
            }
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
        } catch (java.lang.UnsatisfiedLinkError e) {
            if (e.getMessage() != null && e.getMessage().contains("Can't find dependent libraries")) {
                StatsManager.I().track("UnsatisfiedLinkError/JNA");
                // probably the path contains unsupported special chars
                LoggerFactory.getDefaultLogger().info("The Library Path probably contains special chars: " + Application.getResource("tmp/jna").getAbsolutePath());
                ExceptionDialog d = new ExceptionDialog(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, _GUI.T.lit_error_occured(), _GUI.T.special_char_lib_loading_problem(Application.getHome(), "Java Native Interface"), e, null, _GUI.T.lit_close()) {
                    @Override
                    public ModalityType getModalityType() {
                        return ModalityType.MODELESS;
                    }
                };
                UIOManager.I().show(ExceptionDialogInterface.class, d);
            } else {
                StatsManager.I().track("UnsatisfiedLinkError/Diff");
            }
            LoggerFactory.getDefaultLogger().log(e);
        } catch (final Throwable e1) {
            LoggerFactory.getDefaultLogger().log(e1);
        }
        UJCECheck.check();
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
                    if (Application.getJavaVersion() < Application.JAVA17) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: java version < 1.7");
                    } else if (StringUtils.containsIgnoreCase(System.getProperty("java.vm.name"), "OpenJDK")) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: OpenJDK VM detected");
                    } else if (CFG_GENERAL.CFG.isPreferBouncyCastleForTLS()) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: enabled");
                    } else if (!UJCECheck.isSuccessful()) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: UJCECheck was not successful");
                    } else {
                        LoggerFactory.getDefaultLogger().info("Use 'JSSE' for default SSLSocketStreamFactory!");
                    }
                } catch (final Throwable e) {
                    LoggerFactory.getDefaultLogger().log(e);
                }
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
                    if (CrossSystem.isWindows()) {
                        if (CFG_GENERAL.CFG.isWindowsJNAIdleDetectorEnabled()) {
                            try {
                                /* speed up the init of the following libs */
                                org.jdownloader.jna.windows.User32 u = org.jdownloader.jna.windows.User32.INSTANCE;
                                org.jdownloader.jna.windows.Kernel32 k = org.jdownloader.jna.windows.Kernel32.INSTANCE;
                            } catch (final Throwable e) {
                                LoggerFactory.getDefaultLogger().log(e);
                            }
                        }
                        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_2000)) {
                            try {
                                WindowManager.setCustom(new JNAWindowsWindowManager());
                            } catch (final Throwable e) {
                                LoggerFactory.getDefaultLogger().log(e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    LoggerFactory.getDefaultLogger().log(e);
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
                    LoggerFactory.getDefaultLogger().info("LAF INIT");
                    Window awindow[];
                    int j = (awindow = Window.getWindows()).length;
                    for (int i = 0; i < j; i++) {
                        Window window = awindow[i];
                        LoggerFactory.getDefaultLogger().info("Window: " + window);
                        boolean flag = !(window instanceof JWindow) && !(window instanceof JFrame) && !(window instanceof JDialog);
                        if (!window.getClass().getName().contains("Popup$HeavyWeightWindow") && !flag) {
                            LoggerFactory.getDefaultLogger().info("Window: " + "Reshape: yes");
                        }
                    }
                } catch (Exception e) {
                    LoggerFactory.getDefaultLogger().log(e);
                }
                Dialog.getInstance().initLaf();
                return null;
            }
        };
        if (!org.appwork.utils.Application.isHeadless()) {
            lafInit.start();
        } else {
            LookAndFeelController.getInstance().init();
            // LAFOptions.init("org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel");
            // try {
            // final String theme = LAFOptions.getInstance().getCfg().getIconSetID();
            //
            // org.jdownloader.images.NewTheme.getInstance().setTheme(theme);
            //
            // } catch (Throwable e) {
            // LoggerFactory.getDefaultLogger().log(e);
            // }
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
                                    if (!org.appwork.utils.Application.isHeadless()) {
                                        final GraphicalUserInterfaceSettings guiConfig = JsonConfig.create(GraphicalUserInterfaceSettings.class);
                                        new EDTRunner() {
                                            /*
                                             * moved to edt. rar init freezes under linux
                                             */
                                            @Override
                                            protected void runInEDT() {
                                                /* init clipboardMonitoring stuff */
                                                CLIPBOARD_SKIP_MODE skipMode = guiConfig.getClipboardSkipMode();
                                                if (skipMode == null) {
                                                    skipMode = CLIPBOARD_SKIP_MODE.NEVER;
                                                }
                                                ClipboardMonitoring.setHtmlFlavorAllowed(guiConfig.isClipboardMonitorProcessHTMLFlavor());
                                                if (org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled()) {
                                                    ClipboardMonitoring.setClipboardSkipMode(skipMode);
                                                    ClipboardMonitoring.getINSTANCE().startMonitoring();
                                                } else {
                                                    switch (skipMode) {
                                                    case NEVER:
                                                    case ON_ENABLE:
                                                        ClipboardMonitoring.setClipboardSkipMode(skipMode);
                                                        break;
                                                    }
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
                                            }
                                        }.start(true);
                                    }
                                    new Thread("ExecuteWhenGuiReachedThread: Init Clipboard and ChallengeResponseController") {
                                        public void run() {
                                            ChallengeResponseController.getInstance().init();
                                        };
                                    }.start();
                                }
                            });
                            DownloadController.DOWNLOADLIST_LOADED.executeWhenReached(new Runnable() {
                                @Override
                                public void run() {
                                    final AutoDownloadStartOption doRestartRunninfDownloads = JsonConfig.create(GeneralSettings.class).getAutoStartDownloadOption();
                                    final boolean closedWithRunningDownload = JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads();
                                    if (AutoDownloadStartOption.ALWAYS == doRestartRunninfDownloads || (closedWithRunningDownload && AutoDownloadStartOption.ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS == doRestartRunninfDownloads)) {
                                        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                                            @Override
                                            protected Void run() throws RuntimeException {
                                                /*
                                                 * we do this check inside IOEQ because initDownloadLinks also does its final init in IOEQ
                                                 */
                                                final List<DownloadLink> dlAvailable = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
                                                    @Override
                                                    public boolean acceptNode(final DownloadLink node) {
                                                        return node.isEnabled() && node.getFinalLinkState() == null;
                                                    }

                                                    @Override
                                                    public int returnMaxResults() {
                                                        return 1;
                                                    }
                                                });
                                                if (dlAvailable.size() > 0) {
                                                    new Thread("AutostartDialog") {
                                                        @Override
                                                        public void run() {
                                                            if (!DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE)) {
                                                                // maybe downloads have been started by another instance or user input
                                                                return;
                                                            }
                                                            final GeneralSettings generalSettings = JsonConfig.create(GeneralSettings.class);
                                                            if (generalSettings.getAutoStartCountdownSeconds() > 0 && CFG_GENERAL.SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS.isEnabled()) {
                                                                final ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT.T.Main_run_autostart_(), _JDT.T.Main_run_autostart_msg(), new AbstractIcon(IconKey.ICON_START, 32), _JDT.T.Mainstart_now(), null);
                                                                d.setTimeout(generalSettings.getAutoStartCountdownSeconds() * 1000);
                                                                try {
                                                                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, d);
                                                                    ret.throwCloseExceptions();
                                                                } catch (DialogNoAnswerException e) {
                                                                    if (!e.isCausedByTimeout()) {
                                                                        return;
                                                                    }
                                                                }
                                                            }
                                                            DownloadWatchDog.getInstance().startDownloads();
                                                        }
                                                    }.start();
                                                }
                                                return null;
                                            }
                                        });
                                    }
                                }
                            });
                            /* Start shared memory state update */
                            if (CrossSystem.isWindows() && JsonConfig.create(GeneralSettings.class).isSharedMemoryStateEnabled()) {
                                SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                                    @Override
                                    public void run() {
                                        new Thread("ExecuteWhenGuiReachedThread: Init SharedMemoryState") {
                                            public void run() {
                                                /* init clipboardMonitoring stuff */
                                                SharedMemoryState.getInstance().startUpdates();
                                            };
                                        }.start();
                                    }
                                });
                            }
                            SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
                                @Override
                                public void run() {
                                    DelayWriteController.getInstance().init();
                                }
                            });
                        } catch (Throwable e) {
                            LoggerFactory.getDefaultLogger().log(e);
                            if (Application.isHeadless()) {
                                ConsoleDialog.showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                            } else {
                                Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                                // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                            }
                        } finally {
                            OperatingSystemEventSender.getInstance();
                            if (!(CrossSystem.isWindows() || CrossSystem.isMac()) && Application.isHeadless()) {
                                OperatingSystemEventSender.getInstance().setIgnoreSignal("HUP", true);
                            }
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
                        if (!Application.isJared(null) && System.getProperty("iconsets") != null) {
                            Toolkit.getDefaultToolkit().addAWTEventListener(new IconSetMakerAdapter(), AWTEvent.MOUSE_EVENT_MASK);
                        }
                        lafInit.waitForEDT();
                        LoggerFactory.getDefaultLogger().info("InitGUI->" + (System.currentTimeMillis() - SecondLevelLaunch.startup));
                        JDGui.init();
                        // init Archivecontroller.init has to be done AFTER downloadcontroller and linkcollector
                        ArchiveController.getInstance();
                        Toolkit.getDefaultToolkit().addAWTEventListener(new CustomCopyPasteSupport(), AWTEvent.MOUSE_EVENT_MASK);
                        LoggerFactory.getDefaultLogger().info("GUIDONE->" + (System.currentTimeMillis() - SecondLevelLaunch.startup));
                    } catch (Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);
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
                    LoggerFactory.getDefaultLogger().log(e1);
                    break;
                }
            }
            JDGui.getInstance().badLaunchCheck();
        }
        SecondLevelLaunch.GUI_COMPLETE.setReached();
        LoggerFactory.getDefaultLogger().info("Initialisation finished");
        SecondLevelLaunch.INIT_COMPLETE.setReached();
        // init statsmanager
        StatsManager.I();

        new Thread("Print Sec Infos") {
            public void run() {
                if (CrossSystem.isWindows()) {
                    SecuritySoftwareResponse sw = null;
                    try {
                        LoggerFactory.getDefaultLogger().info("AntiVirusProduct START");
                        sw = CrossSystem.getAntiVirusSoftwareInfo();
                        HashMap<String, String> infos = createInfoMap(sw);
                        StatsManager.I().track(100, "secur", "av", infos, CollectionName.SECURITY);
                    } catch (UnsupportedOperationException e) {
                    } catch (Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);
                        HashMap<String, String> infos = new HashMap<String, String>();
                        if (e instanceof SecuritySoftwareException) {
                            infos.put("response", ((SecuritySoftwareException) e).getResponse());
                        }
                        infos.put("error", e.getMessage());
                        infos.put("exception", Exceptions.getStackTrace(e));
                        StatsManager.I().track(100, "secur", "av/error", infos, CollectionName.SECURITY);
                    } finally {
                        LoggerFactory.getDefaultLogger().info("AntiVirusProduct END");
                    }
                    try {
                        LoggerFactory.getDefaultLogger().info("FirewallProduct START");
                        sw = CrossSystem.getFirewallSoftwareInfo();
                        HashMap<String, String> infos = createInfoMap(sw);
                        StatsManager.I().track(100, "secur", "fw", infos, CollectionName.SECURITY);
                    } catch (UnsupportedOperationException e) {
                    } catch (Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);
                        HashMap<String, String> infos = new HashMap<String, String>();
                        if (e instanceof SecuritySoftwareException) {
                            infos.put("response", ((SecuritySoftwareException) e).getResponse());
                        }
                        infos.put("error", e.getMessage());
                        infos.put("exception", Exceptions.getStackTrace(e));
                        StatsManager.I().track(100, "secur", "fw/error", infos, CollectionName.SECURITY);
                    } finally {
                        LoggerFactory.getDefaultLogger().info("FirewallProduct END");
                    }
                    switch (CrossSystem.getOS()) {
                    case WINDOWS_10:
                    case WINDOWS_7:
                    case WINDOWS_VISTA:
                    case WINDOWS_8:
                        try {
                            LoggerFactory.getDefaultLogger().info("AntiSpywareProduct START");
                            sw = CrossSystem.getAntiSpySoftwareInfo();
                            HashMap<String, String> infos = createInfoMap(sw);
                            StatsManager.I().track(100, "secur", "as", infos, CollectionName.SECURITY);
                        } catch (UnsupportedOperationException e) {
                        } catch (Throwable e) {
                            LoggerFactory.getDefaultLogger().log(e);
                            HashMap<String, String> infos = new HashMap<String, String>();
                            infos.put("error", e.getMessage());
                            infos.put("exception", Exceptions.getStackTrace(e));
                            if (e instanceof SecuritySoftwareException) {
                                infos.put("response", ((SecuritySoftwareException) e).getResponse());
                            }
                            StatsManager.I().track(100, "secur", "as/error", infos, CollectionName.SECURITY);
                        } finally {
                            LoggerFactory.getDefaultLogger().info("AntiSpywareProduct END");
                        }
                    }
                }
            }

            private HashMap<String, String> createInfoMap(SecuritySoftwareResponse sw) {
                ArrayList<String> names = new ArrayList<String>();
                ArrayList<String> signedReportingExe = new ArrayList<String>();
                ArrayList<String> signedProductExe = new ArrayList<String>();
                ArrayList<String> state = new ArrayList<String>();
                for (SecuritySoftwareInfo s : sw) {
                    names.add(s.getName());
                    if (OperatingSystem.WINDOWS_XP == CrossSystem.getOS()) {
                        if (s.get("pathToEnableOnAccessUI") != null) {
                            signedProductExe.add(new File(s.get("pathToEnableOnAccessUI")).getName());
                        }
                        if (s.get("pathToUpdateUI") != null) {
                            signedReportingExe.add(new File(s.get("pathToUpdateUI")).getName());
                        }
                    } else {
                        if (s.get("pathToSignedReportingExe") != null) {
                            signedReportingExe.add(new File(s.get("pathToSignedReportingExe")).getName());
                        }
                        if (s.get("pathToSignedProductExe") != null) {
                            signedProductExe.add(new File(s.get("pathToSignedProductExe")).getName());
                        }
                    }
                    state.add(s.get("productState"));
                }
                HashMap<String, String> infos = new HashMap<String, String>();
                infos.put("names", JSonStorage.serializeToJson(names));
                infos.put("reporting", JSonStorage.serializeToJson(signedReportingExe));
                infos.put("product", JSonStorage.serializeToJson(signedProductExe));
                infos.put("states", JSonStorage.serializeToJson(state));
                infos.put("response", sw.getResponse());
                return infos;
            };
        }.start();
        // init Filechooser. filechoosers may freeze the first time the get initialized. maybe this helps
        if (!Application.isHeadless()) {
            try {
                long t = System.currentTimeMillis();
                File[] baseFolders = AccessController.doPrivileged(new PrivilegedAction<File[]>() {
                    public File[] run() {
                        return (File[]) sun.awt.shell.ShellFolder.get("fileChooserComboBoxFolders");
                    }
                });
                LoggerFactory.getDefaultLogger().info("fileChooserComboBoxFolders " + (System.currentTimeMillis() - t));
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }
}