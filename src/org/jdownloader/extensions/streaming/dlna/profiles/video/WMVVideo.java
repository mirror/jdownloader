package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.MP3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.WMAAudio;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoASFContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoWMVContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.WindowsMediaVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.WindowsMediaVideoStream.Level;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.WindowsMediaVideoStream.Profile;

public class WMVVideo extends AbstractMpegProfile {

    public static final WMVVideo WMVMED_BASE  = new WMVVideo("WMVMED_BASE") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.MAIN, Level.MEDIUM));
                                                      addAudioStream(WMAAudio.WMABASE.getStream());
                                                  }

                                              };
    public static final WMVVideo WMVMED_FULL  = new WMVVideo("WMVMED_FULL") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.MAIN, Level.MEDIUM));
                                                      addAudioStream(WMAAudio.WMAFULL.getStream());
                                                  }

                                              };

    public static final WMVVideo WMVMED_PRO   = new WMVVideo("WMVMED_PRO") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.MAIN, Level.MEDIUM));
                                                      addAudioStream(WMAAudio.WMAPRO.getStream());
                                                  }

                                              };

    public static final WMVVideo WMVHIGH_FULL = new WMVVideo("WMVHIGH_FULL") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.MAIN, Level.HIGH));
                                                      addAudioStream(WMAAudio.WMAFULL.getStream());
                                                  }

                                              };

    public static void init() {
    }

    public static final WMVVideo WMVHIGH_PRO  = new WMVVideo("WMVHIGH_PRO") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.MAIN, Level.HIGH));
                                                      addAudioStream(WMAAudio.WMAPRO.getStream());
                                                  }

                                              };

    public static final WMVVideo WMVSPLL_BASE = new WMVVideo("WMVSPLL_BASE") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.SIMPLE, Level.LOW));
                                                      addAudioStream(WMAAudio.WMABASE.getStream());
                                                  }

                                              };

    public static final WMVVideo WMVSPML_BASE = new WMVVideo("WMVSPML_BASE") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.SIMPLE, Level.MEDIUM));
                                                      addAudioStream(WMAAudio.WMABASE.getStream());
                                                  }

                                              };
    public static final WMVVideo WMVSPML_MP3  = new WMVVideo("WMVSPML_MP3") {
                                                  {
                                                      getVideoStreams().addAll(createBaseStreams(Profile.SIMPLE, Level.MEDIUM));
                                                      addAudioStream(MP3Audio.MP3.getStream());
                                                  }

                                              };

    public WMVVideo(String id) {
        super(id);
        mimeType = MimeType.VIDEO_WMV;
        this.containers = new AbstractMediaContainer[] { new VideoWMVContainer(), new VideoASFContainer() };

    }

    protected static WindowsMediaVideoStream profileMain(WindowsMediaVideoStream stream) {
        stream.setProfileTags(new String[] { "main" });

        return stream;
    }

    protected List<InternalVideoStream> createBaseStreams(Profile p, Level l) {
        ArrayList<InternalVideoStream> ret = new ArrayList<InternalVideoStream>();

        switch (p) {
        case ADVANCED:
            switch (l) {
            case L0:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 2000000).addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_30));

                break;
            case L1:

                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 10000000).addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_30));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 10000000).addResolution(Resolution._625_D1_720x576).addFrameRate(FrameRate.FPS_25));

                break;

            case L2:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 20000000).addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_60));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 20000000).addResolution(Resolution._1280x720).addFrameRate(FrameRate.FPS_30));

                break;
            case L3:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 45000000).addResolution(Resolution._1920x1080).addFrameRate(FrameRate.FPS_24));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 45000000).addResolution(Resolution._1920x1080).addFrameRate(FrameRate.FPS_30));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 45000000).addResolution(Resolution._1280x720).addFrameRate(FrameRate.FPS_60));
                break;
            case L4:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 135000000).addResolution(Resolution._1920x1080).addFrameRate(FrameRate.FPS_60));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 135000000).addResolution(Resolution._2048x1536).addFrameRate(FrameRate.FPS_24));

                break;
            }
            break;
        case MAIN:
            switch (l) {
            case HIGH:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 20000000).addResolution(Resolution._1920x1080).addFrameRate(FrameRate.FPS_30));
                break;
            case MEDIUM:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 10000000).addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_30));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 10000000).addResolution(Resolution._625_D1_720x576).addFrameRate(FrameRate.FPS_25));
                break;

            case LOW:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 2000000).addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(FrameRate.FPS_24));
                break;
            }
            break;
        case SIMPLE:
            switch (l) {

            case MEDIUM:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 384000).addResolution(Resolution._240x176).addFrameRate(FrameRate.FPS_30));
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 384000).addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_15));
                break;

            case LOW:
                ret.add(new WindowsMediaVideoStream(p, l).addBitrateRange(1, 96000).addResolution(Resolution._QCIF_176x144).addFrameRate(FrameRate.FPS_15));
                break;
            }
            break;

        }

        return ret;
    }
}
