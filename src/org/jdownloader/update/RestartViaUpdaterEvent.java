package org.jdownloader.update;

import java.io.File;
import java.io.IOException;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;

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

    /**
     * Create a new instance of RestartViaUpdaterEvent. This is a singleton
     * class. Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartViaUpdaterEvent() {
        this.setHookPriority(Integer.MAX_VALUE);

    }

    @Override
    public void run() {

        final File root = Application.getResource(RestartController.JARNAME);
        if (!root.exists()) {
            System.err.println(root + " is missing");
            return;
        }

        final String tiny[] = new String[] { RestartController.JAVA_INTERPRETER, "-jar", RestartController.UPDATER_JARNAME, "-restart", RestartController.getRestartCommandLine() };
        if (Application.getResource(RestartController.JARNAME).exists()) {
            System.out.println(Application.getResource(RestartController.JARNAME) + " exists");
        } else {
            System.err.println(Application.getResource(RestartController.JARNAME) + " is Missing");
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

        pbroot = new File(Application.getRoot());

        System.out.println("Root: " + pbroot);
        pb.directory(pbroot);
        try {
            pb.start();
        } catch (final IOException e) {
        }
    }

}
