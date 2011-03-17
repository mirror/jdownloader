package jd.controlling.captcha;

import jd.plugins.PluginForDecrypt;

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
        if (PluginForDecrypt.isAborted(item.getInitTime(), item.getHost())) return null;

        return super.addWait(item);
    }

    public void blockByHost(String host) {
        PluginForDecrypt.abortQueuedByHost(host);

        synchronized (queueLock) {
            for (final QueuePriority prio : prios) {
                for (final QueueAction<?, ? extends Throwable> item : queue.get(prio)) {
                    /* kill item */
                    if (((CaptchaDialogQueueEntry) item).getHost().equals(host)) {
                        item.kill();
                        synchronized (item) {
                            item.notify();
                        }
                    }
                }
                /* clear queue */
                queue.get(prio).clear();
            }
        }
    }

    public void blockAll() {
        PluginForDecrypt.abortQueued();
        this.killQueue();
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
