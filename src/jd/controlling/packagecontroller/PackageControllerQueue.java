package jd.controlling.packagecontroller;

import javax.swing.SwingUtilities;

import org.appwork.utils.DebugMode;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogInterface;

public class PackageControllerQueue extends Queue {

    public static abstract class ReadOnlyQueueAction<T, E extends Throwable> extends QueueAction<T, E> {

        public ReadOnlyQueueAction() {
            super();
        }

        public ReadOnlyQueueAction(QueuePriority prio) {
            super(prio);
        }
    }

    protected PackageControllerQueue(String id) {
        super(id);
    }

    @Override
    public void killQueue() {
        getLogger().log(new Throwable("YOU CANNOT KILL ME!"));
        /*
         * this queue can't be killed
         */
    }

    public <E, T extends Throwable> E addWait(QueueAction<E, T> item) throws T {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && SwingUtilities.isEventDispatchThread()) {
            getLogger().log(new Exception("This should be done via callback to avoid queue<->edt deadlocks"));
        }
        return super.addWait(item);
    };

    @Override
    public QueueAction<?, ? extends Throwable> peek() {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return null;
        }
        try {
            return super.peek();
        } catch (Throwable e) {
            // compatibility with incompatible core
            return null;
        }
    }

    public boolean executeQueuedAction(final QueueAction<?, ?> item) {
        if (item != null && !item.gotStarted() && isQueueThread(item) && isQueued(item)) {
            try {
                super.startItem(item, true);
            } catch (Throwable e) {
                getLogger().log(e);
            }
            remove(item);
            return true;
        } else {
            return false;
        }
    }

    protected LogInterface getLogger() {
        return org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger();
    }

}
