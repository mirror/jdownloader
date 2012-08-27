package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.VideoStream;

public class VideoMediaItem extends MediaItem {

    private ArrayList<AudioStream> audioStreams;

    @Override
    public String getMimeTypeString() {
        return "video/" + getContainerFormat();
    }

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

    public VideoMediaItem(DownloadLink dl) {
        super(dl);
        audioStreams = new ArrayList<AudioStream>();
        videoStreams = new ArrayList<VideoStream>();
    }

    public void setVideoStreams(ArrayList<VideoStream> videoStreams) {
        this.videoStreams = videoStreams;
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

    public ArrayList<VideoStream> getVideoStreams() {
        return videoStreams;
    }

}
