package org.jdownloader.update;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import jd.nutils.OSDetector;
import jd.utils.JDUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

public class RestartController implements ShutdownVetoListener {
    private static final RestartController INSTANCE         = new RestartController();
    public static String                   UPDATER_JARNAME  = "Updater.jar";
    public static String                   EXENAME          = "JDownloader.exe";
    public static String                   JARNAME          = "JDownloader.jar";
    public static String                   APPNAME          = "JDownloader.app";
    public static String                   JAVA_INTERPRETER = "java";
    static {
        try {
            String javaInterpreter = new File(new File(System.getProperty("sun.boot.library.path")), "javaw.exe").getAbsolutePath();
            if (new File(javaInterpreter).exists()) {
                JAVA_INTERPRETER = javaInterpreter;
            }
        } catch (Throwable e) {
            // nothing
        }

    }

    public static ArrayList<String> getRestartParameters() {

        ArrayList<String> nativeParameters = new ArrayList<String>();
        if (CrossSystem.isWindows()) {

            /*
             * windows starts exe launcher
             */
            if (Application.getResource(EXENAME).exists()) {
                Log.L.warning("Windows " + Application.getResource(EXENAME) + " exists");
                nativeParameters.add(EXENAME);
                return nativeParameters;
            } else {
                Log.L.warning("Windows " + Application.getResource(EXENAME) + " is missing");

            }
        } else if (CrossSystem.isMac()) {

            String appname = APPNAME;
            try {
                appname = Application.getResource("../../../../").getCanonicalFile().getName();

                System.out.println("APPNAME " + appname + " - " + Application.getResource("../../../../").getCanonicalPath());
            } catch (final IOException e) {

                e.printStackTrace();
            }

            if (Application.getResource("../../../../" + appname).exists()) {

                nativeParameters.add("open");
                nativeParameters.add("-n");
                nativeParameters.add("../../../../" + appname);
                return nativeParameters;
            } else {
                Log.L.warning("MAX " + Application.getResource("../../../../" + appname) + " is missing");

            }

        }
        return getJVMParameters();
    }

    private static ArrayList<String> getJVMParameters() {
        ArrayList<String> jvmParameter = new ArrayList<String>();

        jvmParameter.add(JAVA_INTERPRETER);
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
            }

            jvmParameter.add(h);
        }
        if (!xmsset) {

            jvmParameter.add("-Xms64m");
        }
        if (OSDetector.isLinux()) {

            if (!useconc) {
                jvmParameter.add("-XX:+UseConcMarkSweepGC");
            }
            if (!minheap) jvmParameter.add("-XX:MinHeapFreeRatio=0");
            if (!maxheap) jvmParameter.add("-XX:MaxHeapFreeRatio=0");
        }

        jvmParameter.add(JARNAME);
        return jvmParameter;
    }

    /**
     * get the only existing instance of RestartController. This is a singleton
     * 
     * @return
     */
    public static RestartController getInstance() {
        return RestartController.INSTANCE;
    }

    /**
     * Create a new instance of RestartController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartController() {
        ShutdownController.getInstance().addShutdownVetoListener(this);

    }

    public void restartViaUpdater() {
        Log.L.info("restartViaUpdater");
        ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().addShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().requestShutdown();
    }

    public void exitViaUpdater() {
        Log.L.info("exitViaUpdater");
        ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().addShutdownEvent(SilentUpdaterEvent.getInstance());

        // ShutdownController.getInstance().requestShutdown();

    }

    public boolean onShutdownRequest() throws ShutdownVetoException {

        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    public void directRestart() {
        Log.L.info("direct Restart");
        ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());
        ShutdownController.getInstance().addShutdownEvent(RestartDirectEvent.getInstance());
        ShutdownController.getInstance().requestShutdown();

    }

    public static String getRestartCommandLine() {
        StringBuilder sb = new StringBuilder();
        for (String s : getRestartParameters()) {
            if (sb.length() > 0) sb.append(" ");
            if (s.contains(" ")) {
                sb.append("\"");
                sb.append(s);
                sb.append("\"");
            } else {
                sb.append(s);
            }

        }
        System.out.println(sb);
        return sb.toString();
    }

    public void onShutdown() {

        if (ShutdownController.getInstance().hasShutdownEvent(SilentUpdaterEvent.getInstance())) {
            ArrayList<File> filesToInstall = JDUpdater.getInstance().getFilesToInstall();
            if (filesToInstall.size() > 0) {
                UpdateFoundDialog dialog = new UpdateFoundDialog(null, new Runnable() {

                    public void run() {

                    }

                }, filesToInstall.size());
                try {
                    Dialog.getInstance().showDialog(dialog);

                } catch (DialogNoAnswerException e) {
                    ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
                }

            }
        }

        if (JDUtilities.getController() != null) {
            JDUtilities.getController().prepareShutdown(false);
        }
    }

    public void exit() {
        ShutdownController.getInstance().requestShutdown();
    }
}
