package org.jdownloader.updatev2.restart;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.RestartController;

public abstract class Restarter {

    private LogSource         logger;
    private RestartController controller;

    protected Restarter() {
        System.out.println("Create Restarter");
        logger = LogController.getInstance().getLogger(getClass().getName());
    }

    public LogSource getLogger() {
        return logger;
    }

    public void restart(List<String> parameters) {
        try {

            System.out.println("RestartIt");
            List<String> lst = getApplicationStartCommands();
            System.out.println("appcommands");

            lst.addAll(parameters);
            System.out.println("cmd " + lst);

            ProcessBuilder p = ProcessBuilderFactory.create(lst);
            p.directory(getRunInDirectory());
            System.out.println("Start process");
            Process process = p.start();
            System.out.println("Read errorstream");
            logger.logAsynch(process.getErrorStream());
            System.out.println("Read inputstream");
            logger.logAsynch(process.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().log(e);
        }

    }

    protected File getRunInDirectory() {
        return Application.getResource("tmp").getParentFile();
    }

    protected abstract List<String> getApplicationStartCommands();

    protected abstract List<String> getJVMApplicationStartCommands();

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
