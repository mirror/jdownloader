package org.jdownloader.extensions.antistandby;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;

public class MacAntiStandBy extends Thread {

    private final AntiStandbyExtension     jdAntiStandby;
    private static final int               sleep       = 5000;
    private final LogSource                logger;
    private final AtomicBoolean            lastState   = new AtomicBoolean(false);
    private final AtomicReference<Process> lastProcess = new AtomicReference<Process>(null);

    public MacAntiStandBy(AntiStandbyExtension antiStandbyExtension) {
        setDaemon(true);
        setName("MacAntiStandby");
        jdAntiStandby = antiStandbyExtension;
        logger = LogController.CL(AntiStandbyExtension.class);
    }

    public void run() {
        try {
            while (jdAntiStandby.isAntiStandbyThread()) {
                switch (jdAntiStandby.getMode()) {
                case DOWNLOADING:
                    if (DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                        enableAntiStandby(true);
                    } else {
                        enableAntiStandby(false);
                    }
                    break;
                case RUNNING:
                    enableAntiStandby(true);
                    break;
                default:
                    logger.finest("JDAntiStandby: Config error (unknown mode: " + jdAntiStandby.getMode() + ")");
                    break;
                }
                sleep(sleep);
            }
        } catch (Throwable e) {
            logger.log(e);
        } finally {
            try {
                enableAntiStandby(false);
            } catch (final Throwable e) {
            } finally {
                logger.fine("JDAntiStandby: Terminated");
                logger.close();
            }
        }
    }

    private void enableAntiStandby(final boolean enabled) {
        if (lastState.compareAndSet(!enabled, enabled)) {
            if (enabled) {
                final Process process = lastProcess.get();
                if (process != null) {
                    try {
                        process.exitValue();
                    } catch (IllegalThreadStateException e) {
                        return;
                    }
                }
                lastProcess.set(createProcess());
                logger.fine("JDAntiStandby: Start");
            } else {
                final Process process = lastProcess.getAndSet(null);
                if (process != null) {
                    process.destroy();
                    if (Application.getJavaVersion() >= Application.JAVA18) {
                        process.destroyForcibly();
                    }
                    logger.fine("JDAntiStandby: Stop");
                }
            }
        }
    }

    private Process createProcess() {
        try {
            String[] command = { "pmset", "noidle" };
            // windows debug
            // command = new String[] { "calc.exe" };
            ProcessBuilder probuilder = ProcessBuilderFactory.create(command);
            logger.info("Call pmset nodile");
            return probuilder.start();
        } catch (IOException e) {
            logger.log(e);
        }
        return null;
    }
}
