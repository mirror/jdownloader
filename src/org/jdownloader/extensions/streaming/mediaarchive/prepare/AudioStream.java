package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AC3AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AMRAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Atrac3PlusAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mp3AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mpeg2AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.WmaAudioStream;
import org.jdownloader.logging.LogController;

public class AudioStream implements Storable {
    private static final LogSource LOGGER = LogController.getInstance().getLogger(AudioStream.class.getName());

    public AudioStream(/* Storable */) {

    }

    private int bitrate;

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
    private int    samplingRate;
    private int    channels;

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public Class<? extends InternalAudioStream> mapDlnaStream() {
        if ("aac".equals(codec)) {
            return AACAudioStream.class;
        } else if ("mp3".equals(codec)) {
            return Mp3AudioStream.class;
        } else if ("mp2".equals(codec)) {
            return Mpeg2AudioStream.class;
        } else if ("ac3".equals(codec)) {
            return AC3AudioStream.class;
        } else if ("amr".equals(codec)) {
            return AMRAudioStream.class;
        } else if ("atrac4".equals(codec)) {
            return Atrac3PlusAudioStream.class;
        } else if ("wma".equals(codec)) { return WmaAudioStream.class; }
        LOGGER.info("Unknown AudioCodec: " + codec);
        LOGGER.info("Unknown AudioCodec: \r\n" + JSonStorage.toString(this));
        return null;
    }

    public int getChannels() {
        return channels;
    }

}
