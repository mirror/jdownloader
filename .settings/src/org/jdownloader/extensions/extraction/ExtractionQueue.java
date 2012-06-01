package org.jdownloader.extensions.extraction;

import java.util.ArrayList;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

public class ExtractionQueue extends Queue {

    public ExtractionQueue() {
        super("ExtractionQueue");
    }

    public ExtractionController getCurrentQueueEntry() {
        return (ExtractionController) this.getCurrentJob();
    }

    public ArrayList<ExtractionController> getJobs() {
        ArrayList<ExtractionController> ret = new ArrayList<ExtractionController>();

        for (QueueAction<?, ?> e : getEntries()) {
            ret.add((ExtractionController) e);
        }
        return ret;
    }

}
