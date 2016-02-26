package org.jdownloader.osevents.multios;

import java.util.HashMap;
import java.util.HashSet;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public abstract class SignalEventSource implements sun.misc.SignalHandler {
    private final LogSource                               logger;

    private final HashMap<String, sun.misc.SignalHandler> oldHandlers = new HashMap<String, sun.misc.SignalHandler>();
    private final HashSet<String>                         ignored     = new HashSet<String>();

    public SignalEventSource() {
        logger = LogController.getInstance().getLogger(SignalEventSource.class.getName());
        init();
    }

    protected void init() {
        reg("INT");
        reg("TERM");
        reg("HUP");
    }

    public boolean setIgnore(String signalName, boolean ignore) {
        try {
            final sun.misc.Signal signal = new sun.misc.Signal(signalName);
            synchronized (ignored) {
                if (ignore) {
                    return ignored.add(signal.getName());
                } else {
                    return ignored.remove(signal.getName());
                }
            }
        } catch (Throwable e) {
            logger.info("Cannot handle: " + signalName);
        }
        return false;
    }

    private void reg(final String signalName) {
        try {
            final sun.misc.Signal signal = new sun.misc.Signal(signalName);
            synchronized (oldHandlers) {
                oldHandlers.put(signalName, sun.misc.Signal.handle(signal, this));
            }
            logger.info("Can handle: " + signalName);
        } catch (Throwable e) {
            logger.info("Cannot handle: " + signalName);
        }
    }

    public void handle(final sun.misc.Signal signal) {
        logger.info("Signal handler called for signal " + signal);
        try {
            synchronized (ignored) {
                if (ignored.contains(signal.getName())) {
                    return;
                }
            }
            onSignal(signal.getName(), signal.getNumber());
            final sun.misc.SignalHandler oldHandler;
            synchronized (oldHandlers) {
                oldHandler = oldHandlers.get(signal.getName());
            }
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
