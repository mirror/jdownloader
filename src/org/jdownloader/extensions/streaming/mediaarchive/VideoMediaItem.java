package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractAudioVideoContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMKVContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.video.AbstractAudioVideoProfile;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.VideoStream;
import org.jdownloader.logging.LogController;

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

    public long getSystemBitrate() {
        return systemBitrate;
    }

    private long                   duration;
    private long                   systemBitrate;
    private ArrayList<VideoStream> videoStreams;

    public VideoMediaItem(DownloadLink dl) {
        super(dl);
        audioStreams = new ArrayList<AudioStream>();
        videoStreams = new ArrayList<VideoStream>();
    }

    public void setVideoStreams(ArrayList<VideoStream> videoStreams) {
        this.videoStreams = videoStreams;
    }

    public void setSystemBitrate(long parseInt) {
        this.systemBitrate = parseInt;
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

    public boolean matches(AbstractAudioVideoProfile p) {
        boolean sysBitrate = p.checkSystemBitrate(getSystemBitrate());
        boolean container = p.checkContainer(mapDlnaContainer());
        boolean video = false;
        boolean audio = false;

        for (VideoStream vs : getVideoStreams()) {
            Class<? extends InternalVideoStream> streamType = vs.mapDlnaStream();
            if (streamType == null) continue;
            for (InternalVideoStream ps : p.getVideoStreams()) {
                boolean vCodec = streamType.isAssignableFrom(ps.getClass());
                boolean vBitrate = ps.checkBitrate(vs.getBitrate());
                boolean vFrameRate = ps.checkFrameRate(vs.getFrameRate());
                boolean vRes = ps.checkResolution(vs.getWidth(), vs.getHeight());
                boolean vPAR = ps.checkPixelAspectRatio(vs.getPixelAspectRatio());

                video = vBitrate && vFrameRate && vRes && vPAR && vCodec && vPAR;
                if (video) break;
            }

        }

        for (AudioStream as : getAudioStreams()) {
            Class<? extends InternalAudioStream> streamType = as.mapDlnaStream();
            if (streamType == null) continue;
            for (InternalAudioStream ps : p.getAudioStreams()) {
                boolean aCodec = streamType.isAssignableFrom(ps.getClass());
                boolean aBitrate = ps.checkBitrate(as.getBitrate());
                boolean aSamplingrate = ps.checkSamplingRate(as.getSamplingRate());
                boolean aChannels = ps.checkChannels(as.getChannels());
                audio = aBitrate && aCodec && aSamplingrate && aChannels;
                if (audio) break;
            }

        }

        return sysBitrate && container && audio && video;
    }

    private static final LogSource LOGGER = LogController.getInstance().getLogger(AudioStream.class.getName());

    public String toString() {
        return getContainerFormat() + " SysBitRate: " + getSystemBitrate() + " Duration: " + getDuration() + "\r\n " + JSonStorage.toString(getVideoStreams()) + "\r\n" + JSonStorage.toString(getAudioStreams());
    }

    private Class<? extends AbstractAudioVideoContainer> mapDlnaContainer() {
        if ("mov,mp4,m4a,3gp,3g2,mj2".equals(getContainerFormat())) {
            //
            return VideoMp4Container.class;
            //
        }
        if ("matroska,webm".equalsIgnoreCase(getContainerFormat())) { return VideoMKVContainer.class; }
        if ("mpegts".equalsIgnoreCase(getContainerFormat())) return VideoTransportStreamContainer.class;
        if ("avi".equalsIgnoreCase(getContainerFormat())) {
            // avi not supported by dlna?
            return null;
        }
        LOGGER.info("Unknown Container: " + getContainerFormat());

        return null;
    }

}
