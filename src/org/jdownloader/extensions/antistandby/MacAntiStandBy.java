package org.jdownloader.extensions.antistandby;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;

public class MacAntiStandBy extends Thread {

    private final AntiStandbyExtension     jdAntiStandby;
    private static final int               sleep       = 5000;
    private final LogSource                logger;
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
                enableAntiStandby(jdAntiStandby.requiresAntiStandby());
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
        if (enabled) {
            Process process = lastProcess.get();
            if (process != null) {
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    return;
                }
            }
            process = createProcess();
            lastProcess.set(process);
            if (process != null) {
                logger.fine("JDAntiStandby: Start");
            } else {
                logger.fine("JDAntiStandby: Failed");
            }
        } else {
            final Process process = lastProcess.getAndSet(null);
            if (process != null) {
                process.destroy();
                if (Application.getJavaVersion() >= Application.JAVA18 && Application.getJavaVersion() < Application.JAVA19) {
                    try {
                        final Method method = process.getClass().getMethod("destroyForcibly", new Class[] {});
                        if (method != null) {
                            method.setAccessible(true);
                            method.invoke(process, new Object[] {});
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                logger.fine("JDAntiStandby: Stop");
            }
        }
    }

    private Process createProcess() {
        try {
            final ProcessBuilder probuilder = ProcessBuilderFactory.create(new String[] { "pmset", "noidle" });
            logger.info("Call pmset nodile");
            return probuilder.start();
        } catch (IOException e) {
            logger.log(e);
        }
        return null;
    }
}
