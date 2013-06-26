package org.jdownloader.updatev2.restart;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.RestartController;

public abstract class Restarter {

    private LogSource         logger;
    private RestartController controller;

    protected Restarter() {

        logger = LogController.getInstance().getLogger(getClass().getName());
        logger.setInstantFlush(true);
        logger.info("Create Restarter");
    }

    public LogSource getLogger() {
        return logger;
    }

    public void restart(File root, List<String> parameters) {
        try {
            if (parameters.contains("-norestart")) {
                //
                logger.info("Do not restart due to -norestart parameter");
                return;
            }
            logger.info("RestartIt");
            List<String> lst = getApplicationStartCommands(root);
            logger.info("appcommands");
            logger.info("appcmd " + lst);
            lst.addAll(parameters);
            logger.info("cmdline " + lst);

            ProcessBuilder p = ProcessBuilderFactory.create(lst);
            p.directory(getRunInDirectory(root));
            logger.info("Start process");
            Process process = p.start();
            logger.info("Read errorstream");
            logger.logAsynch(process.getErrorStream());
            logger.info("Read inputstream");
            logger.logAsynch(process.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().log(e);
        }

    }

    protected File getRunInDirectory(File root) {
        return root;
    }

    protected abstract List<String> getApplicationStartCommands(File root);

    protected abstract List<String> getJVMApplicationStartCommands(File root);

    public static Restarter getInstance(RestartController restartController) {
        Restarter restarter;
        if (CrossSystem.isWindows()) {
            restarter = new WindowsRestarter();
        } else if (CrossSystem.isMac()) {
            restarter = new MacRestarter();
        } else {
            restarter = new LinuxRestarter();
        }
        restarter.setController(restartController);
        return restarter;
    }

    private void setController(RestartController restartController) {
        controller = restartController;
    }

    public RestartController getController() {
        return controller;
    }

}
