package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.List;

import jd.plugins.FilePackage;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.MediaPreparerQueue;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareEntry;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchiveController {

    private StreamingExtension      extension;
    private MediaArchiveEventSender eventSender;
    private MediaPreparerQueue      preparerQueue;
    private VideoListController     videoController;
    private AudioListController     audioController;
    private ImageListController     imageController;

    public MediaArchiveController(StreamingExtension streamingExtension) {
        extension = streamingExtension;
        preparerQueue = new MediaPreparerQueue(this);
        eventSender = new MediaArchiveEventSender();
        videoController = new VideoListController();
        audioController = new AudioListController();
        imageController = new ImageListController();

    }

    public VideoListController getVideoController() {
        return videoController;
    }

    public AudioListController getAudioController() {
        return audioController;
    }

    public ImageListController getImageController() {
        return imageController;
    }

    public StreamingExtension getExtension() {
        return extension;
    }

    public void mount(FilePackage fp) {
        PrepareEntry pe = new PrepareEntry(fp);
        preparerQueue.addAsynch(new PrepareJob(this, pe));
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

    public void addMedia(MediaItem node) {
        if (node instanceof VideoMediaItem) {
            videoController.add((VideoMediaItem) node);
        } else if (node instanceof AudioMediaItem) {
            audioController.add((AudioMediaItem) node);
        } else if (node instanceof ImageMediaItem) {
            imageController.add((ImageMediaItem) node);
        }
    }

}
