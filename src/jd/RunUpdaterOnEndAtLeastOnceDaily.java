package jd;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.update.inapp.RestartDirectEvent;
import org.appwork.update.inapp.RestartViaUpdaterEvent;
import org.appwork.update.inapp.SilentUpdaterEvent;
import org.appwork.utils.Application;
import org.appwork.utils.processes.ProcessBuilderFactory;

public class RunUpdaterOnEndAtLeastOnceDaily extends ShutdownEvent {
    private static final RunUpdaterOnEndAtLeastOnceDaily INSTANCE = new RunUpdaterOnEndAtLeastOnceDaily();

    /**
     * get the only existing instance of RunUpdaterOnEndAtLeastOnceDaily. This is a singleton
     * 
     * @return
     */
    public static RunUpdaterOnEndAtLeastOnceDaily getInstance() {
        return RunUpdaterOnEndAtLeastOnceDaily.INSTANCE;
    }

    /**
     * Create a new instance of RunUpdaterOnEndAtLeastOnceDaily. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private RunUpdaterOnEndAtLeastOnceDaily() {
        setHookPriority(Integer.MIN_VALUE);
    }

    private static final byte WINDOWS = 0;
    private static final byte MAC     = 1;
    private static final byte LINUX   = 2;
    private static byte       OS;
    static {
        OS = getOSID(System.getProperty("os.name"));
    }

    public static byte getOSID(final String osString) {
        if (osString == null) {
            /* fallback to Windows */
            return WINDOWS;
        }
        final String OS = osString.toLowerCase();
        if (OS.contains("windows 7")) {
            return WINDOWS;
        } else if (OS.contains("windows xp")) {
            return WINDOWS;
        } else if (OS.contains("windows vista")) {
            return WINDOWS;
        } else if (OS.contains("windows 2000")) {
            return WINDOWS;
        } else if (OS.contains("windows 2003")) {
            return WINDOWS;
        } else if (OS.contains("windows server 2008")) {
            return WINDOWS;
        } else if (OS.contains("nt")) {
            return WINDOWS;
        } else if (OS.contains("windows")) {
            return WINDOWS;
        } else if (OS.contains("mac")) {
            return MAC;
        } else {
            return LINUX;
        }

    }

    public static String getRestartCommandLine() {
        final StringBuilder sb = new StringBuilder();

        for (final String s : getRestartParameters()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (s.contains(" ")) {
                sb.append("\"");
                sb.append(s);
                sb.append("\"");
            } else {
                sb.append(s);
            }

        }

        return sb.toString();
    }

    public static java.util.List<String> getRestartParameters() {

        final java.util.List<String> nativeParameters = new ArrayList<String>();
        if (OS == WINDOWS) {
            /*
             * windows starts exe launcher
             */

            nativeParameters.add(getResource("JDownloader.exe").getAbsolutePath());
            return nativeParameters;

        } else if (OS == MAC) {

            String appname = "JDownloader.app";
            File apppath = null;

            File root = getRootByClass(RunUpdaterOnEndAtLeastOnceDaily.class, null);
            final HashSet<File> loopMap = new HashSet<File>();
            while (root != null && loopMap.add(root)) {
                if (root.getName().endsWith(".app")) {
                    apppath = root.getParentFile();
                    appname = root.getName();
                    break;

                }
                root = root.getParentFile();

            }

            if (apppath == null) {
                apppath = getResource("../../../../");
            }
            if (new File(apppath, appname).exists()) {

                nativeParameters.add("open");
                nativeParameters.add("-n");
                nativeParameters.add(new File(apppath, appname).getAbsolutePath());

                return nativeParameters;
            }

        }
        return getJVMParameters();
    }

    private static java.util.List<String> getJVMParameters() {
        final java.util.List<String> jvmParameter = new ArrayList<String>();

        jvmParameter.add(getJavaBinary());
        jvmParameter.add("-jar");

        final List<String> lst = ManagementFactory.getRuntimeMXBean().getInputArguments();

        boolean xmsset = false;
        boolean useconc = false;
        boolean minheap = false;
        boolean maxheap = false;

        for (final String h : lst) {
            if (h.contains("Xmx")) {
                if (Runtime.getRuntime().maxMemory() < 533000000) {
                    jvmParameter.add("-Xmx512m");
                    continue;
                }
            } else if (h.contains("xms")) {
                xmsset = true;
            } else if (h.contains("XX:+useconc")) {
                useconc = true;
            } else if (h.contains("minheapfree")) {
                minheap = true;
            } else if (h.contains("maxheapfree")) {
                maxheap = true;
            } else if (h.startsWith("-agentlib:")) {
                continue;
            }

            jvmParameter.add(h);
        }
        if (!xmsset) {

            jvmParameter.add("-Xms64m");
        }
        if (OS == LINUX) {

            if (!useconc) {
                jvmParameter.add("-XX:+UseConcMarkSweepGC");
            }
            if (!minheap) {
                jvmParameter.add("-XX:MinHeapFreeRatio=0");
            }
            if (!maxheap) {
                jvmParameter.add("-XX:MaxHeapFreeRatio=0");
            }
        }

        jvmParameter.add("JDownloader.jar");

        return jvmParameter;
    }

    public static File getResource(final String relative) {
        return getRootByClass(RunUpdaterOnEndAtLeastOnceDaily.class, relative);
    }

    public static File getRootByClass(final Class<?> class1, final String subPaths) {
        // this is the jar file
        URL loc;

        loc = class1.getProtectionDomain().getCodeSource().getLocation();

        File appRoot;
        try {
            appRoot = new File(loc.toURI());

            if (appRoot.isFile()) {
                appRoot = appRoot.getParentFile();
            }
            if (subPaths != null) { return new File(appRoot, subPaths); }
            return appRoot;
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getJavaBinary() {
        String jvmPath = null;
        String javaBinary = "java";
        if (OS == WINDOWS) {
            javaBinary = "javaw.exe";
        }
        jvmPath = javaBinary;
        final String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            /* get path from system property */
            final File java = new File(new File(javaHome), "/bin/" + javaBinary);
            if (java.exists() && java.isFile()) {
                jvmPath = java.getAbsolutePath();

            }
        }
        return jvmPath;
    }

    @Override
    public void run() {

        if (!Application.isJared(Launcher.class)) return;
        if (ShutdownController.getInstance().hasShutdownEvent(RestartViaUpdaterEvent.getInstance())) return;
        if (ShutdownController.getInstance().hasShutdownEvent(RestartDirectEvent.getInstance())) return;
        if (ShutdownController.getInstance().hasShutdownEvent(SilentUpdaterEvent.getInstance())) return;

        if (System.getProperty("noBetaUpdater") != null) return;
        java.util.List<String> command = new ArrayList<String>();
        command.add(getJavaBinary());
        command.add("-jar");
        command.add("Updater.jar");
        command.add("-guiless");
        // command.add("-restart");
        // command.add(getRestartCommandLine().replace("\"", "\\\""));

        final String tiny[] = command.toArray(new String[] {});
        /*
         * build complete call arguments for tinybootstrap
         */
        final StringBuilder sb = new StringBuilder();

        for (final String arg : tiny) {

            sb.append(arg);
            sb.append(" ");

        }
        System.out.println(sb);
        final ProcessBuilder pb = ProcessBuilderFactory.create(tiny);
        /*
         * needed because the root is different for jre/class version
         */
        File pbroot = null;

        pbroot = getRootByClass(RunUpdaterOnEndAtLeastOnceDaily.class, null);

        pb.directory(pbroot);
        try {
            pb.start();
        } catch (final Throwable e) {
            e.printStackTrace();
        }

    }
}
