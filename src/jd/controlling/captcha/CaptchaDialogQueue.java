package jd.controlling.captcha;

import java.util.HashMap;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

public class CaptchaDialogQueue extends Queue {

    private final static CaptchaDialogQueue INSTANCE = new CaptchaDialogQueue();

    public static CaptchaDialogQueue getInstance() {
        return INSTANCE;
    }

    /**
     * timestamp when user last used the "do not show further captchas" option
     * after canceling a captcha dialog
     */
    private long                  allBlock;
    /**
     * Store the timestamps when user decided to choose the
     * "do not show further captchas of this host" after canceling a captcha
     */
    private HashMap<String, Long> hostBlockMap;

    private CaptchaDialogQueue() {
        super("CaptchaDialogQueue");
        hostBlockMap = new HashMap<String, Long>();

    }

    public String addWait(final CaptchaDialogQueueEntry item) {
        // block dialog, because the rquest is older than the block
        if (item.getInitTime() < allBlock) return null;
        // bolc host request, becasue the requesting plugin is older than the
        // block
        Long hostBlockTime = hostBlockMap.get(item.getHost());
        if (hostBlockTime != null && item.getInitTime() < hostBlockTime) return null;
        return super.addWait(item);

    }

    public void blockByHost(String host) {
        hostBlockMap.put(host, System.currentTimeMillis());
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
        allBlock = System.currentTimeMillis();
        this.killQueue();

    }

}
