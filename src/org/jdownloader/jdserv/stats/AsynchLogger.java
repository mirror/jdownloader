package org.jdownloader.jdserv.stats;

import org.appwork.utils.event.queue.QueueAction;

public abstract class AsynchLogger extends QueueAction<Void, RuntimeException> {

    abstract public void doRemoteCall();

    @Override
    final protected Void run() throws RuntimeException {
        doRemoteCall();
        return null;
    }

}
