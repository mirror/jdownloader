package jd.controlling;

import org.appwork.utils.event.queue.QueueAction;

public abstract class IOEQAction extends QueueAction<Void, RuntimeException> {

    @Override
    protected Void run() throws RuntimeException {
        try {
            ioeqRun();
        } catch (final Throwable e) {
            JDLogger.exception(e);
        }
        return null;
    }

    abstract public void ioeqRun();
}
