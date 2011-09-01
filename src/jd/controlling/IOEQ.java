package jd.controlling;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.logging.Log;

/**
 * InOrderExecutionQueue, use this Queue to enqueue Actions that should be run
 * in order as they where added
 * 
 * @author daniel
 * 
 */
public class IOEQ {

    public final static ScheduledThreadPoolExecutor TIMINGQUEUE = new ScheduledThreadPoolExecutor(1);
    static {
        TIMINGQUEUE.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        TIMINGQUEUE.allowCoreThreadTimeOut(true);
    }
    private static final Queue                      INSTANCE    = new Queue("InOrderExcecutionQueue") {
                                                                    @Override
                                                                    public void killQueue() {
                                                                        Log.exception(new Throwable("YOU CANNOT KILL ME!"));
                                                                        /*
                                                                         * this
                                                                         * queue
                                                                         * can't
                                                                         * be
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
