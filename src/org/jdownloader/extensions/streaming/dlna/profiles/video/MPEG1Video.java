package org.jdownloader.extensions.streaming.dlna.profiles.video;

import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMpeg1Container;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mpeg2AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.Mpeg1VideoStream;

public class MPEG1Video extends AbstractMpegProfile {

    public static final MPEG1Video MPEG1 = new MPEG1Video("MPEG1");

    public MPEG1Video(String id) {
        super(id);
        this.containers = new AbstractMediaContainer[] { new VideoMpeg1Container() };
        addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 2).addBitrateRange(0, 224000).addSamplingRateRange(44100, 44100));
        addVideoStream(new Mpeg1VideoStream().addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_25).addBitrateRange(1150000, 1150000));
        addVideoStream(new Mpeg1VideoStream().addResolution(Resolution._525SIF_352x240).addFrameRate(FrameRate.FPS_29_97_30000_1001).addFrameRate(FrameRate.FPS_23_97_24000_1001).addBitrateRange(1150000, 1150000));

    }

}
