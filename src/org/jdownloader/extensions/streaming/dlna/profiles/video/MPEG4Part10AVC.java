package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AC3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.MP3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer.TimeStamp;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AMRAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.HEAACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.AVCStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;

public class MPEG4Part10AVC extends AbstractMpegProfile {

    protected static final List<InternalVideoStream> CIF_RESOLUTIONS                               = new ArrayList<InternalVideoStream>();

    protected static final List<InternalVideoStream> MP_L3_SD_RESOLUTIONS                          = new ArrayList<InternalVideoStream>();
    protected static final List<InternalVideoStream> MP_SD_RESOLUTIONS                             = new ArrayList<InternalVideoStream>();
    protected static final List<InternalVideoStream> MP_HD_RESOLUTIONS                             = new ArrayList<InternalVideoStream>();
    private static final AbstractMediaContainer[]    VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS   = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.VALID, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };
    private static final AbstractMediaContainer[]    VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.WITHOUT, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };

    private static final AbstractMediaContainer[]    VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS    = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.ZERO, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };
    static {
        fillCIF(CIF_RESOLUTIONS);

    }

    static {
        MP_HD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._1920x1080).addFrameRate(FrameRate.FPS_30));
        MP_HD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._1920x1152).addFrameRate(FrameRate.FPS_30));
        MP_HD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._1920x540).addFrameRate(FrameRate.FPS_30));
        MP_HD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._1280x720).addFrameRate(FrameRate.FPS_30));

    }

    static {
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._625_D1_720x576).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._625_4SIF_704x576).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525_4SIF_704x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._VGA_640x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._VGA_16_9_640x360).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._625_3_4D1_544x576).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525_3_4D1_544x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._625_2_3D1_480x576).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525_2_3D1_480x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._9_16_VGA_4_3_480x360).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._9_16_VGA_16_9_480x270).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._625_1_2D1_352x576).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525_1_2D1_352x480).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525SIF_352x240).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._QVGA_16_9_320x180).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._1_7_VGA_4_3_240x180).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._1_9_VGA_4_3_208x160).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._QCIF_176x144).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._525QSIF8_176x120).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._SQVGA_4_3_160x120).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._1_16_VGA_4_3_160x112).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._SQVGA_16_9_160x90).addFrameRate(FrameRate.FPS_30));
        MP_SD_RESOLUTIONS.add(createMpSdBase().addResolution(Resolution._SQCIF_128x96).addFrameRate(FrameRate.FPS_30));

    }
    static {
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._625_D1_720x576).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._VGA_640x480).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._VGA_16_9_640x360).addFrameRate(FrameRate.FPS_30));

    }

    private static AVCStream createBaselineConstraint() {
        AVCStream stream = new AVCStream();
        stream.addPixelAspectRatio(1, 1);
        stream.addPixelAspectRatio(12, 11);
        stream.addPixelAspectRatio(10, 11);
        stream.addPixelAspectRatio(16, 11);
        stream.addPixelAspectRatio(40, 33);
        stream.addPixelAspectRatio(44, 33);
        stream.addPixelAspectRatio(64, 33);
        stream.addPixelAspectRatio(160, 99);
        stream.addPixelAspectRatio(18, 11);
        stream.addPixelAspectRatio(15, 11);
        stream.addPixelAspectRatio(24, 11);
        stream.addPixelAspectRatio(60, 33);
        stream.addPixelAspectRatio(20, 11);
        stream.addPixelAspectRatio(32, 11);
        stream.addPixelAspectRatio(80, 33);

        return stream;
    }

    /*
     * Setup Audio Profiles
     */
    private static final InternalAudioStream   AUDIO_PROFILE_AAC_LTP_MULTI5 = new AACAudioStream().addProfileTags("ltp").addChannelRange(3, 6);
    private static final InternalAudioStream   AUDIO_PROFILE_AAC_LTP_MULTI7 = new AACAudioStream().addProfileTags("ltp").addChannelRange(7, 8);
    private static final InternalAudioStream   AUDIO_PROFILE_AAC_LTP_STEREO = new AACAudioStream().addProfileTags("ltp").addChannelRange(1, 2);
    public static final InternalAudioStream    AUDIO_PROFILE_AAC_MULTI5     = new AACAudioStream().addChannelRange(3, 6);

    private static final InternalAudioStream   AUDIO_PROFILE_AAC_STEREO     = new AACAudioStream().addChannelRange(1, 2);

    protected static final InternalAudioStream AUDIO_PROFILE_AMR_STEREO     = new AMRAudioStream("audio/AMR").addBitrateRange(1, 2);

    // layers guessed from name
    protected static final InternalAudioStream AUDIO_PROFILE_HEAAC_L2       = new HEAACAudioStream().setLayers(new int[] { 2 }).addChannelRange(1, 2);

    private static final InternalAudioStream   AUDIO_PROFILE_AC3_STEREO     = AC3Audio.AC3.getStream();
    private static final InternalAudioStream   AUDIO_PROFILE_MP3_STEREO     = MP3Audio.MP3.getStream();

    /*
     * Setup Base Profiles. Most of all Profiles can be derived from these
     */
    static class BaseProfile_CIF15 extends MPEG4Part10AVC {

        public BaseProfile_CIF15(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(1, 600000);
            fillCIF(getVideoStreams());
            for (InternalVideoStream s : getVideoStreams()) {
                s.addBitrateRange(0, 384000);
            }
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_CIF15_540 extends MPEG4Part10AVC {

        public BaseProfile_CIF15_540(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(520000, 540000);
            getVideoStreams().addAll(CIF_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    public static void init() {
    }

    static class BaseProfile_CIF15_520 extends MPEG4Part10AVC {

        public BaseProfile_CIF15_520(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(0, 520000);
            getVideoStreams().addAll(CIF_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_CIF30_940 extends MPEG4Part10AVC {

        public BaseProfile_CIF30_940(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(600000, 940000);
            getVideoStreams().addAll(CIF_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_L2_CIF30 extends MPEG4Part10AVC {

        public BaseProfile_L2_CIF30(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(940000, 1300000);
            getVideoStreams().addAll(CIF_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_CIF30 extends MPEG4Part10AVC {

        public BaseProfile_CIF30(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(0, 3000000);
            fillCIF(getVideoStreams());
            for (InternalVideoStream s : getVideoStreams()) {
                s.addBitrateRange(0, 2000000);
            }
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_MP_L3_SD extends MPEG4Part10AVC {

        public BaseProfile_MP_L3_SD(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(0, 4000000);
            getVideoStreams().addAll(MP_L3_SD_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_MP_SD extends MPEG4Part10AVC {

        public BaseProfile_MP_SD(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(0, 10000000);
            getVideoStreams().addAll(MP_SD_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_MP_HD extends MPEG4Part10AVC {

        public BaseProfile_MP_HD(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(0, 20000000);
            getVideoStreams().addAll(MP_HD_RESOLUTIONS);
            addAudioStream(audioProfile);
        }

    }

    static class BaseProfile_QCIF15 extends MPEG4Part10AVC {

        public BaseProfile_QCIF15(String id, InternalAudioStream audioProfile) {
            super(id);
            addSystemBitrateRange(1, 256000);
            fillCIF(getVideoStreams());
            for (InternalVideoStream s : getVideoStreams()) {
                s.addBitrateRange(0, 128000);
            }
            addAudioStream(audioProfile);
        }

    }

    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_AAC              = new BaseProfile_CIF15("AVC_MP4_BL_CIF15_AAC", AUDIO_PROFILE_AAC_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_AAC_520          = new BaseProfile_CIF15_520("AVC_MP4_BL_CIF15_AAC_520", AUDIO_PROFILE_AAC_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_AAC_LTP          = new BaseProfile_CIF15("AVC_MP4_BL_CIF15_AAC_LTP", AUDIO_PROFILE_AAC_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_AAC_LTP_520      = new BaseProfile_CIF15_520("AVC_MP4_BL_CIF15_AAC_LTP_520", AUDIO_PROFILE_AAC_LTP_STEREO);

    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_AMR              = new BaseProfile_CIF15("AVC_MP4_BL_CIF15_AMR", AUDIO_PROFILE_AMR_STEREO);

    public static final MPEG4Part10AVC AVC_MP4_BL_CIF15_HEAAC            = new BaseProfile_L2_CIF30("AVC_MP4_BL_CIF15_HEAAC", AUDIO_PROFILE_HEAAC_L2);

    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_AAC_LTP          = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_AAC_LTP", AUDIO_PROFILE_AAC_LTP_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_AAC_LTP_MULT5    = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_AAC_LTP_MULT5", AUDIO_PROFILE_AAC_LTP_MULTI5);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_AAC_MULT5        = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_AAC_MULT5", AUDIO_PROFILE_AAC_MULTI5);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_AC3              = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_AC3", AUDIO_PROFILE_AC3_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_HEAAC_L2         = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_HEAAC_L2", AUDIO_PROFILE_HEAAC_L2);
    public static final MPEG4Part10AVC AVC_MP4_BL_CIF30_MPEG1_L3         = new BaseProfile_CIF30("AVC_MP4_BL_CIF30_HEAAC_L2", AUDIO_PROFILE_MP3_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_L12_CIF15_HEAAC        = new MPEG4Part10AVC("AVC_MP4_BL_L12_CIF15_HEAAC") {
                                                                             {
                                                                                 addSystemBitrateRange(1, 600000);
                                                                                 addVideoStream(createCIF15Base().addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(new FrameRate(20, 1)).addBitrateRange(0, 384000));

                                                                                 getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_HEAAC_L2);
                                                                             }
                                                                         };
    public static final MPEG4Part10AVC AVC_MP4_BL_L1B_QCIF15_HEAAC       = new BaseProfile_QCIF15("AVC_MP4_BL_L1B_QCIF15_HEAAC", AUDIO_PROFILE_HEAAC_L2);

    // l3l seems to be missing in the specs.

    public static final MPEG4Part10AVC AVC_MP4_BL_L2_CIF30_AAC           = new BaseProfile_L2_CIF30("AVC_MP4_BL_L2_CIF30_AAC", AUDIO_PROFILE_AAC_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_L3_SD_AAC              = new BaseProfile_MP_L3_SD("AVC_MP4_BL_L3_SD_AAC", AUDIO_PROFILE_AAC_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_BL_L3L_SD_AAC             = new BaseProfile_MP_L3_SD("AVC_MP4_BL_L3L_SD_AAC", AUDIO_PROFILE_AAC_STEREO);

    public static final MPEG4Part10AVC AVC_MP4_BL_L3L_SD_HEAAC           = new BaseProfile_MP_L3_SD("AVC_MP4_BL_L3L_SD_HEAAC", AUDIO_PROFILE_HEAAC_L2);

    public static final MPEG4Part10AVC AVC_MP4_MP_SD_AAC_LTP             = new BaseProfile_MP_SD("AVC_MP4_MP_SD_AAC_LTP", AUDIO_PROFILE_AAC_LTP_STEREO);

    public static final MPEG4Part10AVC AVC_MP4_MP_SD_AAC_LTP_MULT5       = new BaseProfile_MP_SD("AVC_MP4_MP_SD_AAC_LTP_MULT5", AUDIO_PROFILE_AAC_LTP_MULTI5);
    public static final MPEG4Part10AVC AVC_MP4_MP_SD_AAC_LTP_MULT7       = new BaseProfile_MP_SD("AVC_MP4_MP_SD_AAC_LTP_MULT7", AUDIO_PROFILE_AAC_LTP_MULTI7);
    // wu atrac
    public static final MPEG4Part10AVC AVC_MP4_MP_SD_AAC_MULT5           = new BaseProfile_MP_SD("AVC_MP4_MP_SD_AAC_MULT5", AUDIO_PROFILE_AAC_MULTI5);
    public static final MPEG4Part10AVC AVC_MP4_MP_SD_AC3                 = new BaseProfile_MP_SD("AVC_MP4_MP_SD_AC3", AUDIO_PROFILE_AC3_STEREO);
    public static final MPEG4Part10AVC AVC_MP4_MP_SD_HEAAC_L2            = new BaseProfile_MP_SD("AVC_MP4_MP_SD_HEAAC_L2", AUDIO_PROFILE_HEAAC_L2);
    public static final MPEG4Part10AVC AVC_MP4_MP_SD_MPEG1_L3            = new BaseProfile_MP_SD("AVC_MP4_MP_SD_MPEG1_L3", AUDIO_PROFILE_MP3_STEREO);

    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_MULT5            = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_MULT5", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_MULT5_T          = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_MULT5_T", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_MULT5_ISO        = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_MULT5_ISO", AUDIO_PROFILE_AAC_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_HEAAC_L2             = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_HEAAC_L2", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_HEAAC_L2_T           = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_HEAAC_L2_T", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_HEAAC_L2_ISO         = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_HEAAC_L2_ISO", AUDIO_PROFILE_HEAAC_L2));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_MPEG1_L3             = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_MPEG1_L3", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_MPEG1_L3_T           = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_MPEG1_L3_T", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_MPEG1_L3_ISO         = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_MPEG1_L3_ISO", AUDIO_PROFILE_MP3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_AC3                  = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_AC3", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AC3_T                = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_AC3_T", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AC3_ISO              = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_AC3_ISO", AUDIO_PROFILE_AC3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP              = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_T            = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_T", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_ISO          = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_ISO", AUDIO_PROFILE_AAC_LTP_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT5        = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT5", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT5_T      = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT5_T", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT5_ISO    = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT5_ISO", AUDIO_PROFILE_AAC_LTP_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT7        = toDLNA_TS______(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT7", AUDIO_PROFILE_AAC_LTP_MULTI7));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT7_T      = toDLNA_TS_____T(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT7_T", AUDIO_PROFILE_AAC_LTP_MULTI7));
    public static final MPEG4Part10AVC AVC_TS_MP_SD_AAC_LTP_MULT7_ISO    = toDLNA_TS___ISO(new BaseProfile_MP_SD("AVC_TS_MP_SD_AAC_LTP_MULT7_ISO", AUDIO_PROFILE_AAC_LTP_MULTI7));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_MULT5         = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_MULT5", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_MULT5_T       = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_MULT5_T", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_MULT5_ISO     = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_MULT5_ISO", AUDIO_PROFILE_AAC_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_HEAAC_L2          = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_HEAAC_L2", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_HEAAC_L2_T        = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_HEAAC_L2_T", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_HEAAC_L2_ISO      = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_HEAAC_L2_ISO", AUDIO_PROFILE_HEAAC_L2));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_MPEG1_L3          = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_MPEG1_L3", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_MPEG1_L3_T        = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_MPEG1_L3_T", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_MPEG1_L3_ISO      = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_MPEG1_L3_ISO", AUDIO_PROFILE_MP3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AC3               = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AC3", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AC3_T             = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AC3_T", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AC3_ISO           = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AC3_ISO", AUDIO_PROFILE_AC3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP           = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP_T         = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP_T", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP_ISO       = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP_ISO", AUDIO_PROFILE_AAC_LTP_STEREO));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP_MULT5     = toDLNA_TS______(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP_MULT5", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP_MULT5_T   = toDLNA_TS_____T(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP_MULT5_T", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_LTP_MULT5_ISO = toDLNA_TS___ISO(new BaseProfile_CIF30("AVC_TS_BL_CIF30_AAC_LTP_MULT5_ISO", AUDIO_PROFILE_AAC_LTP_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_940           = toDLNA_TS______(new BaseProfile_CIF30_940("AVC_TS_BL_CIF30_AAC_940", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_940_T         = toDLNA_TS_____T(new BaseProfile_CIF30_940("AVC_TS_BL_CIF30_AAC_940_T", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF30_AAC_940_ISO       = toDLNA_TS___ISO(new BaseProfile_CIF30_940("AVC_TS_BL_CIF30_AAC_940_ISO", AUDIO_PROFILE_AAC_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_MULT5            = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_MULT5", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_MULT5_T          = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_MULT5_T", AUDIO_PROFILE_AAC_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_MULT5_ISO        = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_MULT5_ISO", AUDIO_PROFILE_AAC_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_HEAAC_L2             = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_HEAAC_L2", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_HEAAC_L2_T           = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_HEAAC_L2_T", AUDIO_PROFILE_HEAAC_L2));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_HEAAC_L2_ISO         = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_HEAAC_L2_ISO", AUDIO_PROFILE_HEAAC_L2));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_MPEG1_L3             = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_MPEG1_L3", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_MPEG1_L3_T           = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_MPEG1_L3_T", AUDIO_PROFILE_MP3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_MPEG1_L3_ISO         = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_MPEG1_L3_ISO", AUDIO_PROFILE_MP3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AC3                  = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AC3", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AC3_T                = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AC3_T", AUDIO_PROFILE_AC3_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AC3_ISO              = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AC3_ISO", AUDIO_PROFILE_AC3_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC                  = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_T                = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_T", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_ISO              = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_ISO", AUDIO_PROFILE_AAC_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP              = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_T            = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_T", AUDIO_PROFILE_AAC_LTP_STEREO));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_ISO          = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_ISO", AUDIO_PROFILE_AAC_LTP_STEREO));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT5        = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT5", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT5_T      = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT5_T", AUDIO_PROFILE_AAC_LTP_MULTI5));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT5_ISO    = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT5_ISO", AUDIO_PROFILE_AAC_LTP_MULTI5));

    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT7        = toDLNA_TS______(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT7", AUDIO_PROFILE_AAC_LTP_MULTI7));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT7_T      = toDLNA_TS_____T(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT7_T", AUDIO_PROFILE_AAC_LTP_MULTI7));
    public static final MPEG4Part10AVC AVC_TS_MP_HD_AAC_LTP_MULT7_ISO    = toDLNA_TS___ISO(new BaseProfile_MP_HD("AVC_TS_MP_HD_AAC_LTP_MULT7_ISO", AUDIO_PROFILE_AAC_LTP_MULTI7));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC               = toDLNA_TS______(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_T             = toDLNA_TS_____T(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC_T", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_ISO           = toDLNA_TS___ISO(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC_ISO", AUDIO_PROFILE_AAC_STEREO));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_540           = toDLNA_TS______(new BaseProfile_CIF15_540("AVC_TS_BL_CIF15_AAC_540", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_540_T         = toDLNA_TS_____T(new BaseProfile_CIF15_540("AVC_TS_BL_CIF15_AAC_540_T", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_540_ISO       = toDLNA_TS___ISO(new BaseProfile_CIF15_540("AVC_TS_BL_CIF15_AAC_540_ISO", AUDIO_PROFILE_AAC_STEREO));

    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_LTP           = toDLNA_TS______(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC_LTP", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_LTP_T         = toDLNA_TS_____T(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC_LTP_T", AUDIO_PROFILE_AAC_STEREO));
    public static final MPEG4Part10AVC AVC_TS_BL_CIF15_AAC_LTP_ISO       = toDLNA_TS___ISO(new BaseProfile_CIF15("AVC_TS_BL_CIF15_AAC_LTP_ISO", AUDIO_PROFILE_AAC_STEREO));

    // gp3 container are missing - do we need them?

    private static MPEG4Part10AVC toDLNA_TS___ISO(MPEG4Part10AVC profile) {
        profile.containers = VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
        profile.mimeType = MimeType.VIDEO_MPEG;
        return profile;
    }

    private static MPEG4Part10AVC toDLNA_TS_____T(MPEG4Part10AVC profile) {
        profile.containers = VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
        profile.mimeType = MimeType.VIDEO_MPEG_TS;
        return profile;
    }

    private static MPEG4Part10AVC toDLNA_TS______(MPEG4Part10AVC profile) {
        profile.containers = VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
        profile.mimeType = MimeType.VIDEO_MPEG_TS;
        return profile;
    }

    private static InternalVideoStream createCIF15Base() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2" });
    }

    private static InternalVideoStream createMpL3SdBase() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2", "1.3", "2", "2.1", "2.2", "3" }).setProfileTags(AVCStream.BASELINE_CONSTRAINED/*
                                                                                                                                                                          * ,
                                                                                                                                                                          * "main"
                                                                                                                                                                          * ,
                                                                                                                                                                          * "baseline"
                                                                                                                                                                          */);
    }

    private static InternalVideoStream createMpSdBase() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2", "1.3", "2", "2.1", "2.2", "3" }).setProfileTags(AVCStream.BASELINE_CONSTRAINED/*
                                                                                                                                                                          * ,
                                                                                                                                                                          * "main"
                                                                                                                                                                          */);
    }

    // TS

    private static void fillCIF(List<InternalVideoStream> cifResolutions) {
        cifResolutions.add(createCIF15Base().addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_15));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._525SIF_352x240).addFrameRate(new FrameRate(18, 1)));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(new FrameRate(20, 1)));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._QVGA_16_9_320x180).addFrameRate(new FrameRate(20, 1)));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._1_7_VGA_4_3_240x180).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._1_9_VGA_4_3_208x160).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._QCIF_176x144).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._525QSIF8_176x120).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._SQVGA_4_3_160x120).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._1_16_VGA_4_3_160x112).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._SQVGA_16_9_160x90).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._SQCIF_128x96).addFrameRate(FrameRate.FPS_30));
        cifResolutions.add(createCIF15Base().addResolution(Resolution._1_7_VGA_16_9_240x135).addFrameRate(FrameRate.FPS_30));
    }

    public MPEG4Part10AVC(String id) {
        super(id);
        mimeType = MimeType.VIDEO_MPEG_4;
        this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };

    }

}
