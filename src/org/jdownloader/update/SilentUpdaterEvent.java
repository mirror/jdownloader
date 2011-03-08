package org.jdownloader.update;

import java.io.File;
import java.io.IOException;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;

public class SilentUpdaterEvent extends ShutdownEvent {
    private static final SilentUpdaterEvent INSTANCE = new SilentUpdaterEvent();

    /**
     * get the only existing instance of SilentUpdaterEvent. This is a singleton
     * 
     * @return
     */
    public static SilentUpdaterEvent getInstance() {
        return SilentUpdaterEvent.INSTANCE;
    }

    private String updaterJarName = "Updater.jar";
    private String exeName        = "JDownloader.exe";
    private String jarName        = "JDownloader.jar";
    private String appName        = "JDownloader.app";

    /**
     * Create a new instance of SilentUpdaterEvent. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private SilentUpdaterEvent() {

    }

    @Override
    public void run() {

        final File root = Application.getResource(updaterJarName);
        if (!root.exists()) {
            System.err.println(root + " is missing");
            return;
        }
        final String tiny[] = new String[] { "java", "-jar", updaterJarName, "-restart", " " };

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
