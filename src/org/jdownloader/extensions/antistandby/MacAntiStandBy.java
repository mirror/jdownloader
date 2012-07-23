package org.jdownloader.extensions.antistandby;

import java.io.IOException;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class MacAntiStandBy extends Thread {

    private AntiStandbyExtension extension;
    private LogSource            logger;
    private Process              process;

    public MacAntiStandBy(AntiStandbyExtension antiStandbyExtension) {
        super("MacAntiStandByThread");
        extension = antiStandbyExtension;
        logger = LogController.CL(AntiStandbyExtension.class);
    }

    public void run() {

        while (true) {

            switch (extension.getMode()) {
            case DOWNLOADING:
                if (DownloadWatchDog.getInstance().getStateMachine().hasPassed(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                    if (!processIsRunning()) {

                        logger.fine("JDAntiStandby: Start");
                        doit();
                    }

                } else {
                    if (processIsRunning()) {

                        process.destroy();
                        process = null;
                    }
                }
                break;
            case RUNNING:
                if (!processIsRunning()) {
                    doit();
                }
                break;
            default:
                logger.finest("JDAntiStandby: Config error (unknown mode: " + extension.getMode() + ")");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.log(e);
                return;
            }
        }

    }

    private boolean processIsRunning() {

        if (process == null) return false;
        try {
            process.exitValue();
        } catch (IllegalThreadStateException e) {
            return true;
        }
        return false;
    }

    public void doit() {
        try {
            String[] command = { "pmset", "noidle" };
            ProcessBuilder probuilder = new ProcessBuilder(command);
            logger.info("Call pmset nodile");
            process = probuilder.start();

        } catch (IOException e) {
            logger.log(e);

        }
    }
}
