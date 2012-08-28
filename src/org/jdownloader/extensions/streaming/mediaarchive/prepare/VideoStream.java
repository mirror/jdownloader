package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.AVCStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.Mpeg2VideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.WindowsMediaVideoStream;
import org.jdownloader.logging.LogController;

public class VideoStream implements Storable {
    public VideoStream(/* Storable */) {

    }

    private static final LogSource LOGGER = LogController.getInstance().getLogger(VideoStream.class.getName());
    private int                    bitrate;

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private String codec;
    private int    duration;
    private int    index;
    private int    width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private int    height;
    private int[]  frameRate;
    private String profileTags;
    private int[]  pixelAspectRatio;

    public int[] getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int[] frameRateBreak) {
        this.frameRate = frameRateBreak;
    }

    public void setProfileTags(String strings) {
        profileTags = strings;
    }

    public String getProfileTags() {
        return profileTags;
    }

    public void setPixelAspectRatio(int[] parsePixelAspectRatio) {
        pixelAspectRatio = parsePixelAspectRatio;
    }

    public int[] getPixelAspectRatio() {
        if (pixelAspectRatio == null || pixelAspectRatio[0] == 0) return new int[] { 1, 1 };
        return pixelAspectRatio;
    }

    public Class<? extends InternalVideoStream> mapDlnaStream() {
        if ("h264".equals(getCodec())) {
            return AVCStream.class;
        } else if ("wmv".equals(getCodec())) {
            return WindowsMediaVideoStream.class;
        } else if ("mpeg2video".equals(getCodec())) { return Mpeg2VideoStream.class; }

        LOGGER.info("Unknown VideoCodec: " + codec);
        LOGGER.info("Unknown VideoCodec: \r\n" + JSonStorage.toString(this));
        return null;
    }

}
