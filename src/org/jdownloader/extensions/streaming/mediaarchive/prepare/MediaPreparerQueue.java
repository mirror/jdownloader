package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaArchiveController;

public class MediaPreparerQueue extends Queue {

    private MediaArchiveController mediaArchiveController;

    public MediaPreparerQueue(MediaArchiveController mediaArchiveController) {
        super("MediaPreparerQueue");
        this.mediaArchiveController = mediaArchiveController;

    }

    @Override
    protected void onItemHandled(QueueAction<?, ? extends Throwable> item) {

        mediaArchiveController.firePreparerQueueUpdate();
    }

    public ExtractionController getCurrentQueueEntry() {
        return (ExtractionController) this.getCurrentJob();
    }

    @Override
    public boolean isEmpty() {
        return this.getCurrentJob() == null && super.isEmpty();
    }

    public List<PrepareJob> getJobs() {
        Collection<PrepareJob> ret = new HashSet<PrepareJob>();
        QueueAction<?, ?> cj = getCurrentJob();
        if (cj != null) {
            ret.add((PrepareJob) cj);
        }
        for (QueueAction<?, ?> e : getEntries()) {
            ret.add((PrepareJob) e);
        }

        return new ArrayList<PrepareJob>(ret);
    }
}
