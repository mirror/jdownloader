package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.AC3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.audio.MP3Audio;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AMRAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.HEAACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.AVCStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;

public class MPEG4Part10AVC extends AbstractMpegProfile {
    private static final InternalAudioStream         AUDIO_PROFILE_AAC_LTP_MULTI9 = new AACAudioStream().setProfileTags(new String[] { "lc", "ltp" }).addChannelRange(1, 8);
    private static final InternalAudioStream         AUDIO_PROFILE_AAC_LTP_MULTI5 = new AACAudioStream().setProfileTags(new String[] { "lc", "ltp" }).addChannelRange(1, 6);
    private static final InternalAudioStream         AUDIO_PROFILE_AAC_LTP_STEREO = new AACAudioStream().setProfileTags(new String[] { "lc", "ltp" }).addChannelRange(1, 2);
    private static final InternalAudioStream         AUDIO_PROFILE_AAC_STEREO     = new AACAudioStream().addChannelRange(1, 2);
    public static final InternalAudioStream          AUDIO_PROFILE_AAC_MULTI5     = new AACAudioStream().addChannelRange(1, 6);
    protected static final List<InternalVideoStream> CIF_RESOLUTIONS              = new ArrayList<InternalVideoStream>();
    static {
        fillCIF(CIF_RESOLUTIONS);

    }

    protected static final List<InternalVideoStream> MP_SD_RESOLUTIONS            = new ArrayList<InternalVideoStream>();
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
    protected static final List<InternalVideoStream> MP_L3_SD_RESOLUTIONS         = new ArrayList<InternalVideoStream>();
    static {
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._625_D1_720x576).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._525_D1_720x480).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._VGA_640x480).addFrameRate(FrameRate.FPS_30));
        MP_L3_SD_RESOLUTIONS.add(createMpL3SdBase().addResolution(Resolution._VGA_16_9_640x360).addFrameRate(FrameRate.FPS_30));

    }

    private static InternalVideoStream createMpL3SdBase() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2", "1.3", "2", "2.1", "2.2", "3" }).setProfileTags(new String[] { "constrained-baseline", "main", "baseline" });
    }

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

    private static InternalVideoStream createMpSdBase() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2", "1.3", "2", "2.1", "2.2", "3" }).setProfileTags(new String[] { "constrained-baseline", "main" });
    }

    private static InternalVideoStream createCIF15Base() {
        return createBaselineConstraint().setLevel(new String[] { "1", "1b", "1.1", "1.2" });
    }

    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_L1B_QCIF = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_L12_CIF15 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_CIF15 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_CIF15_520 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_CIF15_540 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_L2_CIF30 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_CIF30 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_CIF30_940 = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_L3L_SD = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_BL_L3_SD = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_MP_SD = new ArrayList<InternalVideoStream>();
    // protected static final List<InternalVideoStream> AVC_VIDEO_PROFILE_MP_HD = new ArrayList<InternalVideoStream>();

    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_AAC_520       = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_AAC_520") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 520000);
                                                                                      getVideoStreams().addAll(CIF_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_AAC_MULT5        = new MPEG4Part10AVC("AVC_MP4_MP_SD_AAC_MULT5") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_MULTI5);
                                                                                  }
                                                                              };

    protected static final HEAACAudioStream    AUDIO_PROFILE_HEAAC_L2         = new HEAACAudioStream();
    static {

        AUDIO_PROFILE_HEAAC_L2.addChannelRange(1, 2);
        // guessed from name?
        AUDIO_PROFILE_HEAAC_L2.setLayers(new int[] { 2 });
        ;

    }
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_HEAAC_L2         = new MPEG4Part10AVC("AVC_MP4_MP_SD_HEAAC_L2") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);

                                                                                      getAudioStreams().add(AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_MPEG1_L3         = new MPEG4Part10AVC("AVC_MP4_MP_SD_MPEG1_L3") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);

                                                                                      getAudioStreams().add(MP3Audio.MP3.getStream());
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_AC3              = new MPEG4Part10AVC("AVC_MP4_MP_SD_AC3") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AC3Audio.AC3.getStream());
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_AAC_LTP          = new MPEG4Part10AVC("AVC_MP4_MP_SD_AAC_LTP") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_LTP_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_AAC_LTP_MULT5    = new MPEG4Part10AVC("AVC_MP4_MP_SD_AAC_LTP_MULT5") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_LTP_MULTI5);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_MP_SD_AAC_LTP_MULT7    = new MPEG4Part10AVC("AVC_MP4_MP_SD_AAC_LTP_MULT7") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 10000000);
                                                                                      getVideoStreams().addAll(MP_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_LTP_MULTI9);
                                                                                  }
                                                                              };
    // wu atrac

    // l3l seems to be missing in the specs.

    public static final MPEG4Part10AVC         AVC_MP4_BL_L3L_SD_AAC          = new MPEG4Part10AVC("AVC_MP4_BL_L3L_SD_AAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 4000000);
                                                                                      getVideoStreams().addAll(MP_L3_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_STEREO);
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_L3L_SD_HEAAC        = new MPEG4Part10AVC("AVC_MP4_BL_L3L_SD_HEAAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 4000000);
                                                                                      getVideoStreams().addAll(MP_L3_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_L3_SD_AAC           = new MPEG4Part10AVC("AVC_MP4_BL_L3_SD_AAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 4000000);
                                                                                      getVideoStreams().addAll(MP_L3_SD_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_STEREO);
                                                                                  }
                                                                              };

    // BSAC missing

    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_AAC_MULT5     = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_AAC_MULT5") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_AAC_MULTI5);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_HEAAC_L2      = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_HEAAC_L2") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_MPEG1_L3      = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_HEAAC_L2") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(MP3Audio.MP3.getStream());
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_AC3           = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_AC3") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(AC3Audio.AC3.getStream());
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_AAC_LTP       = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_AAC_LTP") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_LTP_STEREO);
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF30_AAC_LTP_MULT5 = new MPEG4Part10AVC("AVC_MP4_BL_CIF30_AAC_LTP_MULT5") {
                                                                                  {
                                                                                      addSystemBitrateRange(0, 3000000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 2000000);
                                                                                      }
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_LTP_MULTI5);
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_L2_CIF30_AAC        = new MPEG4Part10AVC("AVC_MP4_BL_L2_CIF30_AAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(940000, 1300000);
                                                                                      getVideoStreams().addAll(CIF_RESOLUTIONS);
                                                                                      getAudioStreams().add(AUDIO_PROFILE_AAC_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_HEAAC         = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_HEAAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(940000, 1300000);
                                                                                      getVideoStreams().addAll(CIF_RESOLUTIONS);
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };
    protected static final InternalAudioStream AUDIO_PROFILE_AMR_STEREO       = new AMRAudioStream("audio/AMR").addBitrateRange(1, 2);
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_AMR           = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_AMR") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 600000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 384000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_AMR_STEREO);
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_AAC           = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_AAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 600000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 384000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_AAC_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_AAC_LTP       = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_AAC_LTP") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 600000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 384000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_AAC_LTP_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_CIF15_AAC_LTP_520   = new MPEG4Part10AVC("AVC_MP4_BL_CIF15_AAC_LTP_520") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 520000);
                                                                                      getVideoStreams().addAll(CIF_RESOLUTIONS);
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_AAC_LTP_STEREO);
                                                                                  }
                                                                              };
    public static final MPEG4Part10AVC         AVC_MP4_BL_L12_CIF15_HEAAC     = new MPEG4Part10AVC("AVC_MP4_BL_L12_CIF15_HEAAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 600000);
                                                                                      addVideoStream(createCIF15Base().addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(new FrameRate(20, 1)).addBitrateRange(0, 384000));

                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };

    public static final MPEG4Part10AVC         AVC_MP4_BL_L1B_QCIF15_HEAAC    = new MPEG4Part10AVC("AVC_MP4_BL_L1B_QCIF15_HEAAC") {
                                                                                  {
                                                                                      addSystemBitrateRange(1, 256000);
                                                                                      fillCIF(getVideoStreams());
                                                                                      for (InternalVideoStream s : getVideoStreams()) {
                                                                                          s.addBitrateRange(0, 128000);
                                                                                      }
                                                                                      getAudioStreams().add(MPEG4Part10AVC.AUDIO_PROFILE_HEAAC_L2);
                                                                                  }
                                                                              };

    public MPEG4Part10AVC(String id) {
        super(id);
        mimeType = MimeType.VIDEO_MPEG_4;
        this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };

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
        stream.setProfileTags(new String[] { "constrained-baseline" });

        return stream;
    }

}
