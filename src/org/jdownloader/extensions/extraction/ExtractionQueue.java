package org.jdownloader.extensions.extraction;

import java.util.ListIterator;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

public class ExtractionQueue extends Queue {

    private ExtractionController currentItem = null;

    public ExtractionQueue() {
        super("ExtractionQueue");
    }

    public ExtractionController getCurrentQueueEntry() {
        return this.currentItem;
    }

    @Override
    protected <T extends Throwable> void startItem(final QueueAction<?, T> item, final boolean callExceptionhandler) throws T {
        this.currentItem = (ExtractionController) item;
        try {
            super.startItem(item, callExceptionhandler);
        } finally {
            this.currentItem = null;
        }
    }

    public int size() {
        int counter = 0;
        synchronized (this.queueLock) {
            if (currentItem != null) {
                counter++;
            }
            for (final QueuePriority prio : this.prios) {
                ListIterator<QueueAction<?, ? extends Throwable>> li = this.queue.get(prio).listIterator();
                while (li.hasNext()) {
                    QueueAction<?, ? extends Throwable> next = li.next();
                    counter++;
                }
            }
        }
        return counter;
    }
}
