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

    public ProfileMatch matches(AbstractAudioVideoProfile p) {

        boolean sysBitrate = p.checkSystemBitrate(getSystemBitrate());
        boolean container = p.checkContainer(mapDlnaContainer());
        if (!container) return null;
        if (!sysBitrate) return null;
        InternalVideoStream matchingProfileVs = null;
        InternalAudioStream matchingProfileAs = null;
        VideoStream matchingVs = null;
        AudioStream matchingAs = null;
        vs: for (VideoStream vs : getVideoStreams()) {
            Class<? extends InternalVideoStream> streamType = vs.mapDlnaStream();
            if (streamType == null) continue;
            for (InternalVideoStream ps : p.getVideoStreams()) {
                boolean vCodec = streamType.isAssignableFrom(ps.getClass());
                boolean vBitrate = ps.checkBitrate(vs.getBitrate());
                boolean vFrameRate = ps.checkFrameRate(vs.getFrameRate());
                boolean vRes = ps.checkResolution(vs.getWidth(), vs.getHeight());
                boolean vPAR = ps.checkPixelAspectRatio(vs.getPixelAspectRatio());

                if (vBitrate && vFrameRate && vRes && vPAR && vCodec && vPAR) {
                    matchingProfileVs = ps;
                    matchingVs = vs;
                    break vs;
                }

            }

        }

        as: for (AudioStream as : getAudioStreams()) {
            Class<? extends InternalAudioStream> streamType = as.mapDlnaStream();
            if (streamType == null) continue;
            for (InternalAudioStream ps : p.getAudioStreams()) {
                boolean aCodec = streamType.isAssignableFrom(ps.getClass());
                boolean aBitrate = ps.checkBitrate(as.getBitrate());
                boolean aSamplingrate = ps.checkSamplingRate(as.getSamplingRate());
                boolean aChannels = ps.checkChannels(as.getChannels());
                if (aBitrate && aCodec && aSamplingrate && aChannels) {
                    matchingProfileAs = ps;
                    matchingAs = as;
                    break as;
                }
            }

        }

        if (sysBitrate && container && matchingProfileAs != null && matchingProfileVs != null) { return new ProfileMatch(p, matchingProfileVs, matchingProfileAs, matchingVs, matchingAs); }
        return null;
    }

    private static final LogSource LOGGER = LogController.getInstance().getLogger(AudioStream.class.getName());

    public String toString() {
        return getContainerFormat() + " SysBitRate: " + getSystemBitrate() + " Duration: " + getDuration() + "\r\n " + JSonStorage.serializeToJson(getVideoStreams()) + "\r\n" + JSonStorage.toString(getAudioStreams());
    }

    private Class<? extends AbstractAudioVideoContainer> mapDlnaContainer() {
        if (getContainerFormat() == null) return null;
        if ("mov,mp4,m4a,3gp,3g2,mj2".equals(getContainerFormat())) {
            //
            if ("isom".equalsIgnoreCase(getMajorBrand())) {
                // Format : MPEG-4
                // Format_Profile : Base Media
                // CodecID : isom
                return VideoMp4Container.class;
            }
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

    public void update(VideoMediaItem node) {
        super.update(node);
        this.audioStreams = node.audioStreams;
        this.duration = node.duration;
        this.videoStreams = node.videoStreams;
        this.systemBitrate = node.systemBitrate;

    }

}
