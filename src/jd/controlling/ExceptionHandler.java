package jd.controlling;

import java.util.logging.Level;
import java.util.logging.Logger;

class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Logger logger;

    public ExceptionHandler(Logger logger) {
        super();
        this.logger = logger;
    }

    public void uncaughtException(Thread t, Throwable e) {
        handle(e);
    }

    public void handle(Throwable throwable) {
        try {
            logger.log(Level.SEVERE, "Uncaught Exception occured", throwable);
        } catch (Throwable t) {
            // don't let the exception get thrown out, will cause infinite
            // looping!
        }
    }

    public static void register(Logger logger) {
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(logger));
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
    }
}
