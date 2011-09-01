package jd.controlling.captcha;

import jd.controlling.IOPermission;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

public class CaptchaDialogQueue extends Queue {

    private final static CaptchaDialogQueue INSTANCE = new CaptchaDialogQueue();

    public static CaptchaDialogQueue getInstance() {
        return INSTANCE;
    }

    private CaptchaDialogQueueEntry currentItem = null;

    private CaptchaDialogQueue() {
        super("CaptchaDialogQueue");
    }

    public String addWait(final CaptchaDialogQueueEntry item) {
        IOPermission io = item.getIOPermission();
        if (io != null && !io.isCaptchaAllowed(item.getHost())) return null;
        return super.addWait(item);
    }

    public CaptchaDialogQueueEntry getCurrentQueueEntry() {
        return this.currentItem;
    }

    @Override
    protected <T extends Throwable> void startItem(final QueueAction<?, T> item, final boolean callExceptionhandler) throws T {
        this.currentItem = (CaptchaDialogQueueEntry) item;
        try {
            super.startItem(item, callExceptionhandler);
        } finally {
            this.currentItem = null;
        }
    }

}
