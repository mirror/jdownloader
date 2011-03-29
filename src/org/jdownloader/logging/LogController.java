package org.jdownloader.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging.LogFormatter;

public class LogController {
    private static final LogController INSTANCE = new LogController();

    /**
     * get the only existing instance of LogController. This is a singleton
     * 
     * @return
     */
    public static LogController getInstance() {
        return LogController.INSTANCE;
    }

    /**
     * Create a new instance of LogController. This is a singleton class. Access
     * the only existing instance by using {@link #getInstance()}.
     */
    private LogController() {

    }

    public Logger createLogger(Class<?> clazz) {
        Logger ret = Logger.getLogger(clazz.getName());

        if (ret.getHandlers().length == 0) {
            final ConsoleHandler cHandler = new ConsoleHandler();
            cHandler.setLevel(Level.ALL);
            cHandler.setFormatter(new LogFormatter());
            ret.addHandler(cHandler);
            try {
                // log file max size 100K, 3 rolling files, append-on-open     
                Handler fileHandler = new FileHandler(Application.getResource("logs/" + ret.getName()).getAbsolutePath(), 100000, 5, true);
                fileHandler.setFormatter(new LogFormatter());
                ret.addHandler(fileHandler);
            } catch (final Exception e) {
                Log.exception(e);
            }
        }

        ret.setLevel(Level.ALL);
        return ret;

    }
}
