package org.jdownloader.update;

import java.io.File;
import java.io.IOException;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;

public class RestartViaUpdaterEvent extends ShutdownEvent {
    private static final RestartViaUpdaterEvent INSTANCE = new RestartViaUpdaterEvent();

    /**
     * get the only existing instance of RestartViaUpdaterEvent. This is a
     * singleton
     * 
     * @return
     */
    public static RestartViaUpdaterEvent getInstance() {
        return RestartViaUpdaterEvent.INSTANCE;
    }

    private String updaterJarName = "Updater.jar";
    private String exeName        = "JDownloader.exe";
    private String jarName        = "JDownloader.jar";
    private String appName        = "JDownloader.app";

    /**
     * Create a new instance of RestartViaUpdaterEvent. This is a singleton
     * class. Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartViaUpdaterEvent() {

    }

    @Override
    public void run() {

        final File root = Application.getResource(updaterJarName);
        if (!root.exists()) {
            System.err.println(root + " is missing");
            return;
        }
        final String tiny[] = new String[] { "java", "-jar", updaterJarName, "-restart", "" };
        /*
         * all other start jar version
         */
        tiny[tiny.length - 1] = "java -jar \"" + jarName + "\"";
        if (Application.getResource(jarName).exists()) {
            System.out.println(Application.getResource(jarName) + " exists");
        } else {
            System.err.println(Application.getResource(jarName) + " is Missing");
        }

        if (CrossSystem.isWindows()) {

            /*
             * windows starts exe launcher
             */
            if (Application.getResource(exeName).exists()) {
                Log.L.warning("Windows " + Application.getResource(exeName) + " exists");
                tiny[tiny.length - 1] = "\"" + exeName + "\"";
            } else {
                Log.L.warning("Windows " + Application.getResource(exeName) + " is missing");

            }
        } else if (CrossSystem.isMac()) {

            String appname = appName;
            try {
                appname = Application.getResource("../../../../").getCanonicalFile().getName();

                System.out.println("APPNAME " + appname + " - " + Application.getResource("../../../../").getCanonicalPath());
            } catch (final IOException e) {

                e.printStackTrace();
            }

            if (Application.getResource("../../../../" + appname).exists()) {
                tiny[tiny.length - 1] = "open -n \"../../../../" + appname + "\"";
            } else {
                Log.L.warning("MAX " + Application.getResource("../../../../" + appname) + " is missing");

            }

        }

        /*
         * build complete call arguments for tinybootstrap
         */
        final StringBuilder sb = new StringBuilder();

        for (final String arg : tiny) {
            sb.append(arg + " ");
        }

        System.out.println("UpdaterCall: " + sb.toString());

        final ProcessBuilder pb = new ProcessBuilder(tiny);
        /*
         * needed because the root is different for jre/class version
         */
        File pbroot = null;
        if (Application.isJared(this.getClass())) {
            pbroot = new File(Application.getRoot()).getParentFile();
        } else {
            pbroot = new File(Application.getRoot());
        }
        System.out.println("Root: " + pbroot);
        pb.directory(pbroot);
        try {
            pb.start();
        } catch (final IOException e) {
        }
    }

}
