package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.VideoStream;

public class VideoMediaItem extends MediaItem {

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
    private String                 containerFormat;
    private long                   size;

    public VideoMediaItem(DownloadLink dl) {
        super(dl);
        audioStreams = new ArrayList<AudioStream>();
        videoStreams = new ArrayList<VideoStream>();
    }

    public void setBitrate(long parseInt) {
        this.bitrate = parseInt;
    }

    public void setDuration(long l) {
        this.duration = l;
    }

    public void addVideoStream(VideoStream as) {
        videoStreams.add(as);
    }

    public void addAudioStream(AudioStream as) {
        audioStreams.add(as);
    }

    // video container type
    public void setContainerFormat(String majorBrand) {
        this.containerFormat = majorBrand;

    }

    public ArrayList<VideoStream> getVideoStreams() {
        return videoStreams;
    }

    public String getContainerFormat() {
        return containerFormat;
    }

    public void setSize(long l) {
        this.size = l;
    }

    public long getSize() {
        return size;
    }
}
