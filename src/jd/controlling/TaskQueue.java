package jd.controlling;

import java.util.concurrent.ScheduledExecutorService;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.event.queue.Queue;
import org.jdownloader.logging.LogController;

public class TaskQueue extends Queue {

    public final static ScheduledExecutorService TIMINGQUEUE = DelayedRunnable.getNewScheduledExecutorService();

    private static TaskQueue                     INSTANCE    = new TaskQueue();

    public static TaskQueue getQueue() {
        return INSTANCE;
    }

    private TaskQueue() {
        super("TaskQueue");
    }

    @Override
    public void killQueue() {
        LogController.CL().log(new Throwable("YOU CANNOT KILL ME!"));
    }

}
