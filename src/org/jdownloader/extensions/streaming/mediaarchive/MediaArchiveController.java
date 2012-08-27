package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.plugins.FilePackage;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.MediaPreparerQueue;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareEntry;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.PrepareJob;

public class MediaArchiveController implements MediaListListener {

    private StreamingExtension      extension;
    private MediaArchiveEventSender eventSender;
    private MediaPreparerQueue      preparerQueue;
    private VideoListController     videoController;
    private AudioListController     audioController;
    private ImageListController     imageController;
    private List<VideoMediaItem>    videoList;
    private List<AudioMediaItem>    audioList;
    private List<ImageMediaItem>    imageList;
    private List<MediaNode>         list;

    public List<MediaNode> getList() {
        return list;
    }

    private HashMap<String, MediaNode> map;

    public MediaArchiveController(StreamingExtension streamingExtension) {
        extension = streamingExtension;
        preparerQueue = new MediaPreparerQueue(this);
        eventSender = new MediaArchiveEventSender();
        videoController = new VideoListController();
        audioController = new AudioListController();
        imageController = new ImageListController();
        videoController.getEventSender().addListener(this, true);
        audioController.getEventSender().addListener(this, true);
        imageController.getEventSender().addListener(this, true);
        update();
    }

    private void update() {
        HashMap<String, MediaNode> map = new HashMap<String, MediaNode>();
        // List<VideoMediaItem> videoList = videoController.getList();
        // Collections.sort(videoList, new Comparator<MediaItem>() {
        //
        // @Override
        // public int compare(MediaItem o1, MediaItem o2) {
        // return o1.getName().compareTo(o2.getName());
        // }
        // });
        List<MediaNode> list = new ArrayList<MediaNode>();
        videoList = videoController.getList();
        audioList = audioController.getList();
        imageList = imageController.getList();
        for (MediaNode mn : videoList) {
            map.put(mn.getUniqueID(), mn);
            list.add(mn);
        }
        for (MediaNode mn : audioList) {
            map.put(mn.getUniqueID(), mn);
            list.add(mn);
        }
        for (MediaNode mn : imageList) {
            map.put(mn.getUniqueID(), mn);
            list.add(mn);
        }
        this.list = list;
        this.map = map;
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

    public MediaNode getItemById(String objectID) {
        return map.get(objectID);
    }

    @Override
    public void onContentChanged(MediaListController<?> caller) {
        update();
    }

}
