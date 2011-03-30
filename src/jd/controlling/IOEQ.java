package jd.controlling;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

/**
 * InOrderExecutionQueue, use this Queue to enqueue Actions that should be run
 * in order as they where added
 * 
 * @author daniel
 * 
 */
public class IOEQ {

    private static final Queue INSTANCE = new Queue("InOrderExcecutionQueue") {
    };

    private IOEQ() {
    }

    /**
     * use this function to add customized QueueActions
     * 
     * @param <E>
     * @param <T>
     * @param action
     */
    public static <E, T extends Throwable> void add(final QueueAction<?, T> action) {
        INSTANCE.addAsynch(action);
    }

    /**
     * use this function to add Runnables
     * 
     * @param run
     */
    public static void add(final Runnable run) {
        add(run, false);
    }

    public static void add(final Runnable run, final boolean allowAsync) {
        INSTANCE.addAsynch(new IOEQAction() {

            public void ioeqRun() {
                run.run();
            }

            @Override
            protected boolean allowAsync() {
                return allowAsync;
            }
        });
    }
}
