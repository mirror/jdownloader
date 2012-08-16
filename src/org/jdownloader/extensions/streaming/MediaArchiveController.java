package org.jdownloader.extensions.streaming;

import java.util.List;

import jd.controlling.packagecontroller.PackageController;

import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;

public class MediaArchiveController extends PackageController<MediaFolder, MediaItem> {

    private StreamingExtension extension;

    public MediaArchiveController(StreamingExtension streamingExtension) {
        extension = streamingExtension;
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

}
