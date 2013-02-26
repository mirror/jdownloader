package jd.controlling.captcha;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import jd.controlling.IOPermission;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

@Deprecated
public class CaptchaDialogQueue extends Queue {

    private final static CaptchaDialogQueue INSTANCE = new CaptchaDialogQueue();

    @Deprecated
    public static CaptchaDialogQueue getInstance() {
        return INSTANCE;
    }

    private BasicCaptchaDialogHandler currentItem = null;

    private CaptchaDialogQueue() {
        super("CaptchaDialogQueue");
    }

    @Deprecated
    public void addWait(final ChallengeDialogHandler<?> item) {
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

    public BasicCaptchaDialogHandler getCurrentQueueEntry() {
        return this.currentItem;
    }

    @Override
    protected <T extends Throwable> void startItem(final QueueAction<?, T> item, final boolean callExceptionhandler) throws T {
        this.currentItem = (BasicCaptchaDialogHandler) item;
        try {
            super.startItem(item, callExceptionhandler);
        } finally {
            this.currentItem = null;
        }
    }

    public List<BasicCaptchaDialogHandler> getJobs() {
        java.util.List<BasicCaptchaDialogHandler> ret = new ArrayList<BasicCaptchaDialogHandler>();
        synchronized (this.queueLock) {
            BasicCaptchaDialogHandler cur = currentItem;
            if (cur != null) {
                ret.add(cur);
            }
            for (final QueuePriority prio : this.prios) {
                ListIterator<QueueAction<?, ? extends Throwable>> li = this.queue.get(prio).listIterator();
                while (li.hasNext()) {
                    QueueAction<?, ? extends Throwable> next = li.next();
                    if (next instanceof BasicCaptchaDialogHandler) {
                        ret.add((BasicCaptchaDialogHandler) next);
                    }
                }
            }
        }
        return ret;
    }

    public BasicCaptchaDialogHandler getCaptchabyID(long id) {
        synchronized (this.queueLock) {
            for (final QueuePriority prio : this.prios) {
                ListIterator<QueueAction<?, ? extends Throwable>> li = this.queue.get(prio).listIterator();
                while (li.hasNext()) {
                    QueueAction<?, ? extends Throwable> next = li.next();
                    if (next instanceof BasicCaptchaDialogHandler) {
                        if (((BasicCaptchaDialogHandler) next).getID().getID() == id) { return (BasicCaptchaDialogHandler) next; }
                    }
                }
            }
            BasicCaptchaDialogHandler cur = currentItem;
            if (cur != null && cur.getID().getID() == id) { return cur; }
        }
        return null;
    }
}
