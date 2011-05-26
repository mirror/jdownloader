package jd.controlling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.appwork.utils.event.queue.Queue;

/**
 * InOrderExecutionQueue, use this Queue to enqueue Actions that should be run
 * in order as they where added
 * 
 * @author daniel
 * 
 */
public class IOEQ {

    public static final ScheduledExecutorService TIMINGQUEUE = Executors.newSingleThreadScheduledExecutor();
    private static final Queue                   INSTANCE    = new Queue("InOrderExcecutionQueue") {
                                                                 @Override
                                                                 public void killQueue() {
                                                                     /*
                                                                      * this
                                                                      * queue
                                                                      * can't be
                                                                      * killed
                                                                      */
                                                                 }
                                                             };

    private IOEQ() {
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

    public static Queue getQueue() {
        return INSTANCE;
    }
}
