package jd.controlling.captcha;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import jd.controlling.IOPermission;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

public class CaptchaDialogQueue extends Queue {

    private final static CaptchaDialogQueue INSTANCE = new CaptchaDialogQueue();

    public static CaptchaDialogQueue getInstance() {
        return INSTANCE;
    }

    private BasicCaptchaDialogQueueEntry currentItem = null;

    private CaptchaDialogQueue() {
        super("CaptchaDialogQueue");
    }

    public void addWait(final ChallengeDialogQueueEntry<?> item) {
        IOPermission io = item.getIOPermission();
        if (io != null && !io.isCaptchaAllowed(item.getHost().getTld())) return;
        // CaptchaEventSender.getInstance().fireEvent(new CaptchaTodoEvent(item.getCaptchaController()));

        try {
            if (!item.isFinished()) {

                super.addWait(item);
            }
        } finally {

        }

    }

    public BasicCaptchaDialogQueueEntry getCurrentQueueEntry() {
        return this.currentItem;
    }

    @Override
    protected <T extends Throwable> void startItem(final QueueAction<?, T> item, final boolean callExceptionhandler) throws T {
        this.currentItem = (BasicCaptchaDialogQueueEntry) item;
        try {
            super.startItem(item, callExceptionhandler);
        } finally {
            this.currentItem = null;
        }
    }

    public List<BasicCaptchaDialogQueueEntry> getJobs() {
        java.util.List<BasicCaptchaDialogQueueEntry> ret = new ArrayList<BasicCaptchaDialogQueueEntry>();
        synchronized (this.queueLock) {
            BasicCaptchaDialogQueueEntry cur = currentItem;
            if (cur != null) {
                ret.add(cur);
            }
            for (final QueuePriority prio : this.prios) {
                ListIterator<QueueAction<?, ? extends Throwable>> li = this.queue.get(prio).listIterator();
                while (li.hasNext()) {
                    QueueAction<?, ? extends Throwable> next = li.next();
                    if (next instanceof BasicCaptchaDialogQueueEntry) {
                        ret.add((BasicCaptchaDialogQueueEntry) next);
                    }
                }
            }
        }
        return ret;
    }

    public BasicCaptchaDialogQueueEntry getCaptchabyID(long id) {
        synchronized (this.queueLock) {
            for (final QueuePriority prio : this.prios) {
                ListIterator<QueueAction<?, ? extends Throwable>> li = this.queue.get(prio).listIterator();
                while (li.hasNext()) {
                    QueueAction<?, ? extends Throwable> next = li.next();
                    if (next instanceof BasicCaptchaDialogQueueEntry) {
                        if (((BasicCaptchaDialogQueueEntry) next).getID().getID() == id) { return (BasicCaptchaDialogQueueEntry) next; }
                    }
                }
            }
            BasicCaptchaDialogQueueEntry cur = currentItem;
            if (cur != null && cur.getID().getID() == id) { return cur; }
        }
        return null;
    }
}
