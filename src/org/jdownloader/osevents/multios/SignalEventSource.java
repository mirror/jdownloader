package org.jdownloader.osevents.multios;

import java.util.HashMap;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public abstract class SignalEventSource implements sun.misc.SignalHandler {
    private LogSource                               logger;

    private HashMap<String, sun.misc.SignalHandler> oldHandlers = new HashMap<String, sun.misc.SignalHandler>();

    public SignalEventSource() {
        logger = LogController.getInstance().getLogger(SignalEventSource.class.getName());
    }

    public void init() {
        reg("INT");
        reg("TERM");
    }

    private void reg(String signal) {
        try {
            sun.misc.Signal diagSignal = new sun.misc.Signal(signal);
            oldHandlers.put(signal, sun.misc.Signal.handle(diagSignal, this));
            logger.info("Can handle: " + signal);
        } catch (Exception e) {
            logger.info("Cannot handle: " + signal);
        }

    }

    public void handle(sun.misc.Signal signal) {
        logger.info("Signal handler called for signal " + signal);
        try {
            onSignal(signal.getName(), signal.getNumber());
            sun.misc.SignalHandler oldHandler = oldHandlers.get(signal.getName());
            // Chain back to previous handler, if one exists
            if (oldHandler != null && oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
                oldHandler.handle(signal);
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }

    public abstract void onSignal(String name, int number);

}
