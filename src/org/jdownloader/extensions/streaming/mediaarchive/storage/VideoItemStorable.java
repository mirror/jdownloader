package org.jdownloader.extensions.streaming.mediaarchive.storage;

import java.util.ArrayList;

import jd.controlling.downloadcontroller.DownloadLinkStorable;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.streaming.mediaarchive.VideoMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.VideoStream;

public class VideoItemStorable extends MediaItemStorable implements Storable {

    private VideoItemStorable(/* STorable */) {
        super();
    }

    private ArrayList<AudioStream> audioStreams;

    public ArrayList<AudioStream> getAudioStreams() {
        return audioStreams;
    }

    public void setAudioStreams(ArrayList<AudioStream> audioStreams) {
        this.audioStreams = audioStreams;
    }

    public long getDuration() {
        return duration;
    }

    public long getBitrate() {
        return bitrate;
    }

    private long                   duration;
    private long                   bitrate;
    private ArrayList<VideoStream> videoStreams;

    public void setBitrate(long parseInt) {
        this.bitrate = parseInt;
    }

    public void setDuration(long l) {
        this.duration = l;
    }

    public ArrayList<VideoStream> getVideoStreams() {
        return videoStreams;
    }

    public void setVideoStreams(ArrayList<VideoStream> videoStreams) {
        this.videoStreams = videoStreams;
    }

    public static MediaItemStorable create(VideoMediaItem mi) {
        VideoItemStorable ret = new VideoItemStorable();
        ret.setAudioStreams(mi.getAudioStreams());
        ret.setVideoStreams(mi.getVideoStreams());
        ret.setBitrate(mi.getBitrate());
        ret.setDuration(mi.getDuration());
        ret.setThumbnailPath(mi.getThumbnailPath());
        ret.setDownloadLink(new DownloadLinkStorable(mi.getDownloadLink()));
        ret.setSize(mi.getSize());
        ret.setContainerFormat(mi.getContainerFormat());
        return ret;
    }

    public VideoMediaItem toVideoMediaItem() {
        VideoMediaItem ret = new VideoMediaItem(getDownloadLink()._getDownloadLink());
        ret.setAudioStreams(getAudioStreams());
        ret.setVideoStreams(getVideoStreams());
        ret.setBitrate(getBitrate());
        ret.setDuration(getDuration());
        ret.setThumbnailPath(getThumbnailPath());

        return ret;
    }

}
