package org.jdownloader.statistics;

import java.util.logging.Level;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;

public abstract class AsynchLogger extends QueueAction<Void, RuntimeException> {

    abstract public void doRemoteCall();

    public boolean handleException(final Throwable e) {
        Log.L.finer("Stats Logger Failed: " + e.getMessage());
        Log.exception(Level.FINEST, e);
        return false;
    }

    @Override
    final protected Void run() throws RuntimeException {
        doRemoteCall();
        return null;
    }

}
