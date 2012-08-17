package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.List;

import jd.controlling.packagecontroller.PackageController;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.MediaPreparerQueue;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareEntry;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchiveController extends PackageController<MediaFolder, MediaItem> {

    private StreamingExtension      extension;
    private MediaArchiveEventSender eventSender;
    private MediaPreparerQueue      preparerQueue;

    public MediaArchiveController(StreamingExtension streamingExtension) {
        extension = streamingExtension;
        preparerQueue = new MediaPreparerQueue(this);
        eventSender = new MediaArchiveEventSender();
    }

    @Override
    protected void _controllerParentlessLinks(List<MediaItem> links, QueuePriority priority) {
    }

    @Override
    protected void _controllerPackageNodeRemoved(MediaFolder pkg, QueuePriority priority) {
    }

    @Override
    protected void _controllerStructureChanged(QueuePriority priority) {
    }

    @Override
    protected void _controllerPackageNodeAdded(MediaFolder pkg, QueuePriority priority) {
    }

    public void mount(FilePackage fp) {
        PrepareEntry pe = new PrepareEntry(fp);
        preparerQueue.addAsynch(new PrepareJob(pe));
        firePreparerQueueUpdate();
    }

    public void firePreparerQueueUpdate() {
        eventSender.fireEvent(new MediaArchiveEvent(this, MediaArchiveEvent.Type.PREPARER_QUEUE_UPDATE));
    }

    public MediaArchiveEventSender getEventSender() {
        return eventSender;
    }

    public boolean isPreparerQueueEmpty() {
        return preparerQueue.isEmpty();
    }

    public List<PrepareJob> getPreparerJobs() {

        return preparerQueue.getJobs();
    }

}
