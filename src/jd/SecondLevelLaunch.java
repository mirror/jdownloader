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
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;

import javax.net.ssl.SSLException;
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
import jd.gui.swing.AWTMacOSApplicationAdapter;
import jd.gui.swing.EAWTMacOSApplicationAdapter;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.nutils.zip.SharedMemoryState;
import jd.plugins.DownloadLink;

import org.appwork.console.ConsoleDialog;
import org.appwork.controlling.SingleReachableState;
import org.appwork.shutdown.ShutdownController;
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
import org.appwork.utils.IO;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.JarHandlerWorkaround;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.JavaSSLSocketStreamFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.os.Docker;
import org.appwork.utils.os.Snap;
import org.appwork.utils.os.hardware.HardwareType;
import org.appwork.utils.os.hardware.HardwareTypeInterface;
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
    public final static long                 startup               = System.currentTimeMillis();

    // private static JSonWrapper webConfig;
    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
        System.setProperty("apple.awt.application.name", "JDownloader");
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
        System.setProperty("com.apple.macos.useScreenMenuBar", "true");
        new Thread() {
            public void run() {
                try {
                    final LogInterface logger = LoggerFactory.getDefaultLogger();
                    // single bundle installer
                    final File file = getMacInfoPlistPath();
                    String cFBundleIdentifier = null;
                    if (file != null && file.isFile()) {
                        cFBundleIdentifier = new Regex(IO.readFileToString(file), "<key>CFBundleIdentifier</key>.*?<string>(.+?)</string>").getMatch(0);
                    }
                    LoggerFactory.getDefaultLogger().info("MAC Bundle Identifier: " + cFBundleIdentifier);
                    if (StringUtils.isEmpty(cFBundleIdentifier)) {
                        cFBundleIdentifier = "org.jdownloader.launcher";
                        logger.info("Use default Bundle Identifier: " + cFBundleIdentifier);
                    } else {
                        logger.info("Use found Bundle Identifier: " + cFBundleIdentifier);
                    }
                    if (StringUtils.isNotEmpty(cFBundleIdentifier)) {
                        Process process = null;
                        try {
                            logger.info("Try to disable AppNap");
                            final ProcessBuilder p = ProcessBuilderFactory.create("defaults", "write", cFBundleIdentifier, "NSAppSleepDisabled", "-bool", "YES");
                            process = p.start();
                            final String ret = IO.readInputStreamToString(process.getInputStream());
                            logger.info("Disable AppNap:" + ret);
                        } catch (Throwable e) {
                            logger.log(e);
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            } finally {
                                process = null;
                            }
                        }
                        try {
                            logger.info("Try to set AppleWindowTabbingMode to manual");
                            final ProcessBuilder p = ProcessBuilderFactory.create("defaults", "write", cFBundleIdentifier, "AppleWindowTabbingMode", "manual");
                            process = p.start();
                            final String ret = IO.readInputStreamToString(process.getInputStream());
                            logger.info("Set AppleWindowTabbingMode to manual:" + ret);
                        } catch (Throwable e) {
                            logger.log(e);
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            } finally {
                                process = null;
                            }
                        }
                        try {
                            if (JVMVersion.isMinimum(JVMVersion.JAVA_1_8) && CrossSystem.getOS().isMinimum(OperatingSystem.MAC_MOJAVE)) {
                                logger.info("Try to set NSRequiresAquaSystemAppearance to NO");
                                final ProcessBuilder p = ProcessBuilderFactory.create("defaults", "write", cFBundleIdentifier, "NSRequiresAquaSystemAppearance", "-bool", "NO");
                                process = p.start();
                                final String ret = IO.readInputStreamToString(process.getInputStream());
                                logger.info("Set NSRequiresAquaSystemAppearance::" + ret);
                            }
                        } catch (Throwable e) {
                            logger.log(e);
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            }
                        }
                        try {
                            final ProcessBuilder p = ProcessBuilderFactory.create("defaults", "read", cFBundleIdentifier);
                            process = p.start();
                            final String ret = IO.readInputStreamToString(process.getInputStream());
                            LoggerFactory.getDefaultLogger().info("App Defaults: \r\n" + ret);
                        } finally {
                            try {
                                if (process != null) {
                                    process.destroy();
                                }
                            } catch (final Throwable e) {
                            } finally {
                                process = null;
                            }
                        }
                    }
                } catch (Throwable e) {
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
            if (Application.getJavaVersion() < Application.JAVA19) {
                EAWTMacOSApplicationAdapter.enableMacSpecial();
            } else {
                AWTMacOSApplicationAdapter.enableMacSpecial();
            }
        } catch (final Throwable ignore) {
            LoggerFactory.getDefaultLogger().info("Error Initializing  Mac Look and Feel Special: " + ignore);
            LoggerFactory.getDefaultLogger().log(ignore);
        }
    }

    private static File getMacInfoPlistPath() {
        final String ownBundlePath = System.getProperty("i4j.ownBundlePath");
        if (StringUtils.endsWithCaseInsensitive(ownBundlePath, ".app")) {
            // folder installer
            final File file = new File(new File(ownBundlePath), "Contents/Info.plist");
            if (file.exists()) {
                return file;
            }
        }
        // old singlebundle installer
        final File file = Application.getResource("../../Info.plist");
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
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
        try {
            switch (CrossSystem.getOSFamily()) {
            case MAC:
                initMACProperties();
                break;
            case LINUX:
            case BSD:
                initLinuxSpecials();
                break;
            case WINDOWS:
                initWindowsSpecials();
                break;
            default:
                break;
            }
        } catch (Throwable ignore) {
            LoggerFactory.getDefaultLogger().log(ignore);
        }
        /* hack for ftp plugin to use new ftp style */
        System.setProperty("ftpStyle", "new");
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        try {
            System.setProperty("org.jdownloader.revision", "JDownloader2");
            final File buildJSon = Application.getResource("build.json");
            if (buildJSon.isFile()) {
                final Map<String, Object> versionMap = JSonStorage.restoreFromString(IO.readFileToString(buildJSon), TypeRef.HASHMAP);
                if (versionMap != null) {
                    final Iterator<Entry<String, Object>> it = versionMap.entrySet().iterator();
                    while (it.hasNext()) {
                        final Entry<String, Object> next = it.next();
                        if (next.getKey().endsWith("Revision")) {
                            System.setProperty("jd.revision." + next.getKey().toLowerCase(Locale.ENGLISH), next.getValue().toString());
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
            LoggerFactory.getDefaultLogger().log(ignore);
        }
        if (CrossSystem.isUnix() || CrossSystem.isMac()) {
            try {
                final File jar = Application.getResource("JDownloader.jar");
                if (jar.isFile()) {
                    /*
                     * double click on JDownloader.jar requires executable flag
                     */
                    LoggerFactory.getDefaultLogger().info("setExecutable:" + jar.setExecutable(true));
                }
            } catch (Throwable ignore) {
                LoggerFactory.getDefaultLogger().log(ignore);
            }
        }
        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());
        for (final Object it : propKeys) {
            final String key = it.toString();
            LoggerFactory.getDefaultLogger().finer(key + "=" + pr.get(key));
        }
        LoggerFactory.getDefaultLogger().info("OS:" + CrossSystem.getOSFamily() + "|" + CrossSystem.getOS() + "|64bit:" + CrossSystem.is64BitOperatingSystem());
        LoggerFactory.getDefaultLogger().info("CPU:" + CrossSystem.getARCHFamily() + "|64bit:" + CrossSystem.is64BitArch());
        LoggerFactory.getDefaultLogger().info("JVM:" + JVMVersion.get() + "|" + JVMVersion.getJVMVersion() + "|64bit:" + Application.is64BitJvm());
        try {
            final HardwareTypeInterface hardwareType = HardwareType.getHardware();
            if (hardwareType != null) {
                LoggerFactory.getDefaultLogger().info("Hardware detected:" + hardwareType);
            }
        } catch (final Throwable ignore) {
            LoggerFactory.getDefaultLogger().log(ignore);
        }
        try {
            if (Docker.isInsideDocker()) {
                LoggerFactory.getDefaultLogger().info("Docker detected:" + Docker.getDockerContainerID());
            }
        } catch (final Throwable ignore) {
            LoggerFactory.getDefaultLogger().log(ignore);
        }
        try {
            if (Snap.isInsideSnap()) {
                LoggerFactory.getDefaultLogger().info("Snap detected:" + Snap.getSnapInstanceName());
            }
        } catch (final Throwable ignore) {
            LoggerFactory.getDefaultLogger().log(ignore);
        }
        try {
            java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            if (arguments != null) {
                LoggerFactory.getDefaultLogger().finer("VMArgs: " + arguments.toString());
            }
            java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            final long maxHeap = memory.getMax();
            LoggerFactory.getDefaultLogger().info("MaxMemory=" + maxHeap + "bytes (" + (maxHeap / (1024 * 1024)) + "Megabytes)");
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        LoggerFactory.getDefaultLogger().info("JDownloader2");
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

    private static void start(final String args[]) {
        go();
    }

    private static void go() {
        LoggerFactory.getDefaultLogger().info("Initialize JDownloader2");
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                final LogSource logger = LogController.getInstance().getLogger("UncaughtExceptionHandler");
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
            final File jnaTmp = Application.getResource("tmp/jna");
            if (!jnaTmp.isDirectory()) {
                jnaTmp.mkdir();
            }
            System.setProperty("jna.tmpdir", jnaTmp.getAbsolutePath());
            if (CrossSystem.isWindows() || CrossSystem.isMac()) {
                com.sun.jna.Native.setCallbackExceptionHandler(new com.sun.jna.Callback.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(com.sun.jna.Callback arg0, Throwable e) {
                        final LogSource logger = LogController.getInstance().getLogger("NativeExceptionHandler");
                        logger.severe("Uncaught Exception in: " + arg0 + "|" + e.getMessage());
                        logger.log(e);
                        logger.close();
                    }
                });
            }
        } catch (java.lang.UnsatisfiedLinkError e) {
            if (e.getMessage() != null && e.getMessage().contains("Can't find dependent libraries")) {
                // probably the path contains unsupported special chars
                LoggerFactory.getDefaultLogger().info("The Library Path probably contains special chars: " + Application.getResource("tmp/jna").getAbsolutePath());
                if (!Application.isHeadless()) {
                    final ExceptionDialog d = new ExceptionDialog(UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, _GUI.T.lit_error_occured(), _GUI.T.special_char_lib_loading_problem(Application.getHome(), "Java Native Interface"), e, null, _GUI.T.lit_close()) {
                        @Override
                        public ModalityType getModalityType() {
                            return ModalityType.MODELESS;
                        }
                    };
                    UIOManager.I().show(ExceptionDialogInterface.class, d);
                }
            }
            LoggerFactory.getDefaultLogger().log(e);
        } catch (final Throwable e1) {
            LoggerFactory.getDefaultLogger().log(e1);
        }
        UJCECheck.check();
        //
        if (!Application.isHeadless() && Application.isJared(SecondLevelLaunch.class)) {
            final LogSource edtLogger = LogController.getInstance().getLogger("BlockingEDT");
            edtLogger.setInstantFlush(true);
            new SlowEDTDetector(10000l, edtLogger);
        }
        /* these can be initiated without a gui */
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    final long javaVersion = Application.getJavaVersion();
                    if (javaVersion < Application.JAVA18) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: java version < 1.8! Java-" + javaVersion);
                    } else if (javaVersion <= 18006000l) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: java version <= 1.8.0_06! Java-" + javaVersion);
                    } else if (CFG_GENERAL.CFG.isPreferBouncyCastleForTLS()) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: enabled! Java-" + javaVersion);
                    } else if (!UJCECheck.isSuccessful()) {
                        HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                        LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: UJCECheck was not successful! Java-" + javaVersion);
                    } else {
                        try {
                            // synology has broken/incomplete java packages that are missing ECDHE_ECDSA
                            JavaSSLSocketStreamFactory.isCipherSuiteSupported(new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" });
                            LoggerFactory.getDefaultLogger().info("Use 'JSSE' for default SSLSocketStreamFactory! Java-" + javaVersion);
                        } catch (final SSLException e) {
                            HTTPConnectionImpl.setDefaultSSLSocketStreamFactory(new BCTLSSocketStreamFactory());
                            LoggerFactory.getDefaultLogger().info("Use 'BouncyCastle' for default SSLSocketStreamFactory because: " + e + "! Java-" + javaVersion);
                        }
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
                    final InternetConnectionSettings config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
                    Browser.setGlobalReadTimeout(config.getHttpReadTimeout());
                    Browser.setGlobalConnectTimeout(config.getHttpConnectTimeout());
                    Browser.setGlobalIPVersion(config.getPreferredIPVersion());
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
                    if (Application.isHeadless()) {
                        ConsoleDialog.showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                    } else {
                        Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);
                    }
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
            try {
                LookAndFeelController.getInstance().init();
            } catch (final Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
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
                                                    skipMode = CLIPBOARD_SKIP_MODE.ON_STARTUP;
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
                                                    case ON_STARTUP:
                                                    default:
                                                        break;
                                                    }
                                                }
                                                org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITOR_PROCESS_HTMLFLAVOR.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
                                                    @Override
                                                    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                                                    }

                                                    @Override
                                                    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                                                        ClipboardMonitoring.setHtmlFlavorAllowed(Boolean.TRUE.equals(newValue));
                                                    }
                                                });
                                                org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_SKIP_MODE.getEventSender().addListener(new GenericConfigEventListener<Enum>() {
                                                    @Override
                                                    public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
                                                    }

                                                    @Override
                                                    public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                                                        ClipboardMonitoring.getINSTANCE().setClipboardSkipMode((CLIPBOARD_SKIP_MODE) newValue);
                                                    }
                                                });
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
                            }
                        } finally {
                            OperatingSystemEventSender.getInstance();
                            if (!(CrossSystem.isWindows() || CrossSystem.isMac()) && Application.isHeadless()) {
                                OperatingSystemEventSender.getInstance().setIgnoreSignal("HUP", true);
                            }
                        }
                        if (CrossSystem.isWindows() && !Application.isHeadless() && CFG_GENERAL.CFG.isSambaPrefetchEnabled()) {
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
                final Thread initThread = JDGui.getInstance().getInitThread();
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
