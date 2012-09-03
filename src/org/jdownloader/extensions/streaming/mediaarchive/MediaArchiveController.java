package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
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
    private MediaRoot               root;

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
        MediaRoot root = new MediaRoot();
        // List<VideoMediaItem> videoList = videoController.getList();
        // Collections.sort(videoList, new Comparator<MediaItem>() {
        //
        // @Override
        // public int compare(MediaItem o1, MediaItem o2) {
        // return o1.getName().compareTo(o2.getName());
        // }
        // });
        List<MediaNode> list = new ArrayList<MediaNode>();
        // videoList = ;
        // audioList = audioController.getList();
        // imageList = imageController.getList();
        put(root, new MediaFolder("video", T._.nodename_video()));
        put(root.getFolder("video"), new MediaFolder("Archive", "Video Archive").addChildren(videoController.getList()));
        put(root.getFolder("video"), new MediaFolder("DownloadList", "Download List"));
        put(root.getFolder("video"), new MediaFolder("Linkgrabber", "Linkgrabber"));
        put(root, new MediaFolder("audio", T._.nodename_audio()).addChildren(audioController.getList()));
        put(root, new MediaFolder("image", T._.nodename_image()).addChildren(imageController.getList()));

        this.root = root;
    }

    private void put(MediaFolder parent, MediaNode child) {
        parent.addChild(child);
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

    public void mount(final FilePackage fp) {

        for (final DownloadLink dl : fp.getChildren()) {
            final List<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(dl);
            PrepareEntry pe = new PrepareEntry() {

                @Override
                public List<DownloadLink> getLinks() {
                    return ret;
                }

                @Override
                public String getName() {
                    return "(" + dl.getHost() + ") " + dl.getName();
                }

            };
            preparerQueue.addAsynch(new PrepareJob(this, pe) {
                protected void onMedia(MediaItem node) {
                    super.onMedia(node);

                }

            });
        }
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

    @Override
    public void onContentChanged(MediaListController<?> caller) {
        update();
    }

    public MediaFolder getDirectory(String objectID) {
        MediaFolder ret = root.getFolder(objectID);
        if (ret == null) return root;
        return ret;
    }

    public MediaNode getItemById(String objectID) {
        return root.get(objectID);
    }

    public void refreshMetadata(final MediaItem mi) {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(mi.getDownloadLink());
        PrepareEntry pe = new PrepareEntry() {

            @Override
            public List<DownloadLink> getLinks() {

                return ret;
            }

            @Override
            public String getName() {
                return mi.getName();
            }

        };
        preparerQueue.addAsynch(new PrepareJob(this, pe) {
            protected void onMedia(MediaItem node) {

                if (node.getClass() != mi.getClass()) { return; }
                mi.update(node);
                if (node instanceof VideoMediaItem) {
                    videoController.onRefresh((VideoMediaItem) mi);
                } else if (node instanceof AudioMediaItem) {
                    audioController.onRefresh((AudioMediaItem) mi);
                } else if (node instanceof ImageMediaItem) {
                    imageController.onRefresh((ImageMediaItem) mi);
                }

                firePreparerQueueUpdate();
            }

        });
        firePreparerQueueUpdate();

    }

    public static List<MediaNode> getAllChildren(List<MediaNode> children) {
        ArrayList<MediaNode> ret = new ArrayList<MediaNode>();
        for (MediaNode mn : children) {
            ret.add(mn);
            if (mn instanceof MediaFolder) {
                ret.addAll(getAllChildren(((MediaFolder) mn).getChildren()));
            }
        }
        return ret;
    }

}
