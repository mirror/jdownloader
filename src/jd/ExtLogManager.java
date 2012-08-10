package jd;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jdownloader.logging.LogController;

public class ExtLogManager extends LogManager {

    private LogController logController = null;

    public LogController getLogController() {
        return logController;
    }

    public void setLogController(LogController logController) {
        this.logController = logController;
    }

    @Override
    public synchronized Logger getLogger(String name) {

        if (logController == null) {
            // Init Loop!!!
            System.out.println("Could not redirect Logger: " + name);

            return super.getLogger(name);
        } else {

            if (name.startsWith("org.fourthline")) {
                System.out.println("Redirect Logger: " + name);
                return logController.getLogger(name);
            } else {
                System.out.println("Ignoreed: " + name);
                return super.getLogger(name);
            }
        }
    }

}
