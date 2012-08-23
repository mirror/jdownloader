package org.jdownloader.extensions.streaming.dlna.profiles.video;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoGp3Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoMp4Container;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer.TimeStamp;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AC3AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AMRAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Atrac3PlusAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.HEAACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mp3AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mpeg2AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.Mpeg4VideoStream;

public class MPEG4Part2 extends AbstractMpegProfile {
    private static final AbstractMediaContainer[] _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS   = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.VALID, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };
    private static final AbstractMediaContainer[] _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.WITHOUT, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };
    private static final AbstractMediaContainer[] _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS    = new AbstractMediaContainer[] { new VideoTransportStreamContainer(TimeStamp.ZERO, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T) };

    static class MPEG4Part2AdvancedSimpleProfileLevel4 extends MPEG4Part2 {
        protected MPEG4Part2AdvancedSimpleProfileLevel4(String id) {
            super(id);
            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile64()));
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile128()));
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile384()));
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile4000()));
        }
    }

    static class MPEG4Part2AdvancedSimpleProfileLevel5 extends MPEG4Part2 {
        protected MPEG4Part2AdvancedSimpleProfileLevel5(String id) {
            super(id);
            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(setupAspLevel5(createSimpleProfile64()));
            addVideoStream(setupAspLevel5(createSimpleProfile128()));
            addVideoStream(setupAspLevel5(createSimpleProfile384()));
            addVideoStream(setupAspLevel5(createSimpleProfile8000()));
        }
    }

    public static void init() {
    }

    static class MPEG4Part2AdvancedSimpleProfileLevel6 extends MPEG4Part2 {
        protected MPEG4Part2AdvancedSimpleProfileLevel6(String id) {
            super(id);
            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(createSimpleProfile64().addResolution(Resolution._1280x720));
            addVideoStream(createSimpleProfile384().addResolution(Resolution._1280x720));
            addVideoStream(setupL6(createSimpleProfile4000()));
            addVideoStream(setupL6(createSimpleProfile12000()));
        }
    }

    static class MPEG4Part2H263Profile0Level10 extends MPEG4Part2 {
        protected MPEG4Part2H263Profile0Level10(String id) {
            super(id);
            mimeType = MimeType.VIDEO_3GP;
            this.containers = new AbstractMediaContainer[] { VideoGp3Container.INSTANCE };
            Mpeg4VideoStream simpleProfile64k = new Mpeg4VideoStream();
            simpleProfile64k.setContentType("video/x-h263");
            simpleProfile64k.setProfileTags(new String[] { "0" });
            simpleProfile64k.setLevel(new String[] { "10" });
            simpleProfile64k.addPixelAspectRatio(12, 11);
            simpleProfile64k.addPixelAspectRatio(16, 11);
            simpleProfile64k.addBitrateRange(1, 64000);
            simpleProfile64k.addFrameRate(FrameRate.FPS_15);

        }
    }

    static class MPEG4Part2SimpleProfileLevel2 extends MPEG4Part2 {
        protected MPEG4Part2SimpleProfileLevel2(String id) {
            super(id);
            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(createSimpleProfileLevel2().addResolution(Resolution._CIF_352x288).addFrameRate(FrameRate.FPS_15).addPixelAspectRatio(12, 11).addPixelAspectRatio(16, 11));
            addVideoStream(createSimpleProfileLevel2().addResolution(Resolution._QVGA_4_3_320x240).addFrameRate(FrameRate.FPS_15).addPixelAspectRatio(1, 1));
            addVideoStream(createSimpleProfileLevel2().addResolution(Resolution._QVGA_16_9_320x180).addFrameRate(FrameRate.FPS_15).addPixelAspectRatio(1, 1));
            addVideoStream(createSimpleProfileLevel2().addResolution(Resolution._QCIF_176x144).addFrameRate(FrameRate.FPS_30).addPixelAspectRatio(12, 11).addPixelAspectRatio(16, 11));
            addVideoStream(createSimpleProfileLevel2().addResolution(Resolution._SQCIF_128x96).addFrameRate(FrameRate.FPS_30).addPixelAspectRatio(12, 11).addPixelAspectRatio(16, 11));

        }
    }

    static class MPEG4Part2SimpleProfileLevel3 extends MPEG4Part2 {
        protected MPEG4Part2SimpleProfileLevel3(String id) {
            super(id);
            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile64()));
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile128()));
            addVideoStream(setupSpAndAspLevel3(createSimpleProfile384()));
        }
    }

    static class MPEG4Part2SimpleProfileLevel3VGA extends MPEG4Part2 {
        protected MPEG4Part2SimpleProfileLevel3VGA(String id) {
            super(id);

            mimeType = MimeType.VIDEO_MPEG_4;
            this.containers = new AbstractMediaContainer[] { VideoMp4Container.INSTANCE };
            addVideoStream(createSimpleProfileLevel3VGA().addResolution(Resolution._VGA_640x480));
            addVideoStream(createSimpleProfileLevel3VGA().addResolution(Resolution._VGA_16_9_640x360));
        }
    }

    public static final MPEG4Part2 MPEG4_H263_MP4_P0_L10_AAC          = new MPEG4Part2H263Profile0Level10("MPEG4_H263_MP4_P0_L10_AAC") {

                                                                          {

                                                                              /*
                                                                               * BItrate is calculated by gupnp as
                                                                               * systembitrate-,maxvideobitrate
                                                                               */
                                                                              addAudioStream(new AACAudioStream().addBitrateRange(1, 86000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_H263_MP4_P0_L10_AAC_LTP      = new MPEG4Part2H263Profile0Level10("MPEG4_H263_MP4_P0_L10_AAC_LTP") {

                                                                          {
                                                                              /*
                                                                               * BItrate is calculated by gupnp as
                                                                               * systembitrate-,maxvideobitrate
                                                                               */
                                                                              addAudioStream(new AACAudioStream().addBitrateRange(1, 86000).setProfileTags(new String[] { "lc", "ltp" }).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_AAC               = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_AAC") {

                                                                          {
                                                                              addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_ATRAC3plus        = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_ATRAC3plus") {

                                                                          {
                                                                              addAudioStream(new Atrac3PlusAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_HEAAC             = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_HEAAC") {

                                                                          {
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_HEAAC_MULT5       = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_HEAAC_MULT5") {

                                                                          {
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L4_SO_AAC         = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_MP4_ASP_L4_SO_AAC") {

                                                                          {
                                                                              /*
                                                                               * BItrate is calculated by gupnp as
                                                                               * systembitrate-,maxvideobitrate
                                                                               */
                                                                              addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L4_SO_HEAAC       = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_MP4_ASP_L4_SO_HEAAC") {

                                                                          {
                                                                              /*
                                                                               * BItrate is calculated by gupnp as
                                                                               * systembitrate-,maxvideobitrate
                                                                               */
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L4_SO_HEAAC_MULT5 = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_MP4_ASP_L4_SO_HEAAC_MULT5") {

                                                                          {
                                                                              /*
                                                                               * BItrate is calculated by gupnp as
                                                                               * systembitrate-,maxvideobitrate
                                                                               */
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L5_SO_AAC         = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_L5_SO_AAC") {

                                                                          {
                                                                              addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L5_SO_HEAAC       = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_L5_SO_HEAAC") {

                                                                          {
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_ASP_L5_SO_HEAAC_MULT5 = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_ASP_L5_SO_HEAAC_MULT5") {

                                                                          {
                                                                              addAudioStream(new HEAACAudioStream().addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_SP_AAC                = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_MP4_SP_AAC") {

                                                                          {
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addBitrateRange(1, 216000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_AAC_LTP            = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_MP4_SP_AAC_LTP") {

                                                                          {

                                                                              addAudioStream(new AACAudioStream().addBitrateRange(1, 216000).setProfileTags(new String[] { "lc", "ltp" }).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_ATRAC3plus         = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_MP4_SP_ATRAC3plus") {

                                                                          {

                                                                              addAudioStream(new Atrac3PlusAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_SP_HEAAC              = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_MP4_SP_HEAAC") {

                                                                          {

                                                                              /* gupnp */addAudioStream(new HEAACAudioStream().addBitrateRange(1, 216000).addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_SP_L2_AAC             = new MPEG4Part2SimpleProfileLevel2("MPEG4_P2_MP4_SP_L2_AAC") {

                                                                          {

                                                                              addAudioStream(new AACAudioStream().addBitrateRange(1, 128000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_L2_AMR             = new MPEG4Part2SimpleProfileLevel2("MPEG4_P2_MP4_SP_L2_AMR") {

                                                                          {

                                                                              addAudioStream(new AMRAudioStream("audio/AMR").addChannelRange(1, 2));

                                                                          }
                                                                      };

    /* Does this profile even exist? SP_L5 does not make much sense. l5 is asp */
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_L5_AAC             = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_MP4_SP_L5_AAC") {

                                                                          {
                                                                              addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_L6_AAC             = new MPEG4Part2AdvancedSimpleProfileLevel6("MPEG4_P2_MP4_SP_L6_AAC") {

                                                                          {
                                                                              addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_MP4_SP_VGA_AAC            = new MPEG4Part2SimpleProfileLevel3VGA("MPEG4_P2_MP4_SP_VGA_AAC") {

                                                                          {

                                                                              /* gupnp */addAudioStream(new AACAudioStream().addBitrateRange(1, 256000).addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_MP4_SP_VGA_HEAAC          = new MPEG4Part2SimpleProfileLevel3VGA("MPEG4_P2_MP4_SP_VGA_HEAAC") {

                                                                          {

                                                                              addAudioStream(new HEAACAudioStream().addBitrateRange(1, 256000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AAC                = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AAC") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AAC_ISO            = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AAC_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AAC_T              = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AAC_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addChannelRange(1, 2));

                                                                          }
                                                                      };

    // acording to libdlna its MPEG4_P2_TS_ASP_AC3_L3 ... I guess without l3 is correct..
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AC3                = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AC3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AC3_ISO            = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_ASP_AC3_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AC3_L3             = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AC3_L3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    // MPEG4_VIDEO_PROFILE_P2_SP_L3_VGA

    public static final MPEG4Part2 MPEG4_P2_TS_ASP_AC3_T              = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_AC3_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_MPEG1_L3           = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_MPEG1_L3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_ASP_MPEG1_L3_ISO       = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_MPEG1_L3_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_ASP_MPEG1_L3_T         = new MPEG4Part2AdvancedSimpleProfileLevel5("MPEG4_P2_TS_ASP_MPEG1_L3_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_AAC                 = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AAC") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addBitrateRange(1, 216000).addChannelRange(1, 2));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_AAC_ISO             = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AAC_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addBitrateRange(1, 216000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_SP_AAC_T               = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AAC_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              /* gupnp */addAudioStream(new AACAudioStream().addBitrateRange(1, 216000).addChannelRange(1, 2));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_SP_AC3                 = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AC3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_AC3_ISO             = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AC3_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    // libdlna uses the _l3 extension. google has matches without... let's use both
    public static final MPEG4Part2 MPEG4_P2_TS_SP_AC3_L3              = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AC3_L3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_SP_AC3_T               = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_AC3_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_CO_AC3                 = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_AC3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_CO_AC3_ISO             = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_AC3_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_CO_AC3_T               = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_AC3_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new AC3AudioStream("audio/x-ac3").addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG1_L3            = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG1_L3") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG1_L3_ISO        = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG1_L3_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG1_L3_T          = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG1_L3_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new Mp3AudioStream());

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG2_L2            = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG2_L2") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;

                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG2_L2_ISO        = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG2_L2_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_SP_MPEG2_L2_T          = new MPEG4Part2SimpleProfileLevel3("MPEG4_P2_TS_SP_MPEG2_L2_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_CO_MPEG2_L2            = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_MPEG2_L2") {
                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS;

                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };
    public static final MPEG4Part2 MPEG4_P2_TS_CO_MPEG2_L2_ISO        = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_MPEG2_L2_ISO") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS;
                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };

    public static final MPEG4Part2 MPEG4_P2_TS_CO_MPEG2_L2_T          = new MPEG4Part2AdvancedSimpleProfileLevel4("MPEG4_P2_TS_CO_MPEG2_L2_T") {

                                                                          {
                                                                              this.containers = _VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS;
                                                                              addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 6));

                                                                          }
                                                                      };

    protected static Mpeg4VideoStream createSimpleProfile12000() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "6" });
        simpleProfile64k.addBitrateRange(1, 12000000);

        return simpleProfile64k;
    }

    protected static Mpeg4VideoStream createSimpleProfile128() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "0b", "2" });
        simpleProfile64k.addBitrateRange(1, 128000);
        return simpleProfile64k;
    }

    protected static Mpeg4VideoStream createSimpleProfile384() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "3" });
        simpleProfile64k.addBitrateRange(1, 384000);

        return simpleProfile64k;
    }

    protected static Mpeg4VideoStream createSimpleProfile4000() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "4a" });
        simpleProfile64k.addBitrateRange(1, 4000000);
        return simpleProfile64k;
    }

    protected static Mpeg4VideoStream createSimpleProfile64() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "0", "1" });
        simpleProfile64k.addBitrateRange(1, 64000);
        return simpleProfile64k;
    }

    protected static Mpeg4VideoStream createSimpleProfile8000() {
        Mpeg4VideoStream simpleProfile64k = createSimpleProfileStream();
        simpleProfile64k.setLevel(new String[] { "5" });
        simpleProfile64k.addBitrateRange(1, 8000000);
        return simpleProfile64k;
    }

    protected static InternalVideoStream createSimpleProfileLevel2() {
        Mpeg4VideoStream simpleProfile64k = new Mpeg4VideoStream();
        simpleProfile64k.setProfileTags(new String[] { "simple" });

        simpleProfile64k.setLevel(new String[] { "0", "0b", "1", "2", });

        simpleProfile64k.addBitrateRange(1, 128000);

        return simpleProfile64k;
    }

    protected static InternalVideoStream createSimpleProfileLevel3VGA() {
        Mpeg4VideoStream simpleProfile64k = new Mpeg4VideoStream();
        simpleProfile64k.setProfileTags(new String[] { "simple" });
        simpleProfile64k.addFrameRate(FrameRate.FPS_30);
        simpleProfile64k.setLevel(new String[] { "0", "0b", "1", "2", "3" });
        simpleProfile64k.addBitrateRange(1, 3000000);
        return simpleProfile64k;
    }

    private static Mpeg4VideoStream createSimpleProfileStream() {
        Mpeg4VideoStream simpleProfile64k = new Mpeg4VideoStream();
        simpleProfile64k.setProfileTags(new String[] { "simple" });
        simpleProfile64k.addPixelAspectRatio(1, 1);
        simpleProfile64k.addPixelAspectRatio(12, 11);
        simpleProfile64k.addPixelAspectRatio(10, 11);
        simpleProfile64k.addPixelAspectRatio(16, 11);
        simpleProfile64k.addPixelAspectRatio(40, 33);
        simpleProfile64k.addFrameRate(FrameRate.FPS_30);
        return simpleProfile64k;
    }

    protected static InternalVideoStream setupAspLevel5(Mpeg4VideoStream stream) {

        stream.addResolution(Resolution._625_D1_720x576);
        stream.addResolution(Resolution._525_D1_720x480);
        stream.addResolution(Resolution._625_4SIF_704x576);
        stream.addResolution(Resolution._525_4SIF_704x480);
        stream.addResolution(Resolution._VGA_640x480);
        stream.addResolution(Resolution._VGA_16_9_640x360);
        stream.addResolution(Resolution._625_3_4D1_544x576);
        stream.addResolution(Resolution._525_3_4D1_544x480);
        stream.addResolution(Resolution._625_2_3D1_480x576);
        stream.addResolution(Resolution._525_2_3D1_480x480);
        stream.addResolution(Resolution._9_16_VGA_4_3_480x360);
        stream.addResolution(Resolution._9_16_VGA_16_9_480x270);
        stream.addResolution(Resolution._625_1_2D1_352x576);
        stream.addResolution(Resolution._525_1_2D1_352x480);
        stream.addResolution(Resolution._CIF_352x288);
        stream.addResolution(Resolution._525SIF_352x240);
        stream.addResolution(Resolution._QVGA_4_3_320x240);
        stream.addResolution(Resolution._QVGA_16_9_320x180);
        stream.addResolution(Resolution._1_7_VGA_4_3_240x180);
        stream.addResolution(Resolution._1_9_VGA_4_3_208x160);
        stream.addResolution(Resolution._QCIF_176x144);
        stream.addResolution(Resolution._525QSIF8_176x120);
        stream.addResolution(Resolution._SQVGA_4_3_160x120);
        stream.addResolution(Resolution._1_16_VGA_4_3_160x112);
        stream.addResolution(Resolution._SQVGA_16_9_160x90);
        stream.addResolution(Resolution._SQCIF_128x96);
        stream.addFrameRate(FrameRate.FPS_30);
        return stream;
    }

    // according to gupnp, there is a level 6
    protected static InternalVideoStream setupL6(Mpeg4VideoStream stream) {
        // not sure about this one.

        /* libdlna l5 */stream.addResolution(Resolution._625_D1_720x576);
        /* libdlna l5 */stream.addResolution(Resolution._525_D1_720x480);
        /* libdlna l5 */stream.addResolution(Resolution._625_4SIF_704x576);
        /* libdlna l5 */stream.addResolution(Resolution._525_4SIF_704x480);

        /* gupnp */stream.addResolution(Resolution._VGA_640x480);
        /* gupnp */stream.addResolution(Resolution._VGA_16_9_640x360);
        /* libdlna l5 */stream.addResolution(Resolution._625_3_4D1_544x576);
        /* libdlna l5 */stream.addResolution(Resolution._525_3_4D1_544x480);
        /* libdlna l5 */stream.addResolution(Resolution._625_2_3D1_480x576);
        /* libdlna l5 */stream.addResolution(Resolution._525_2_3D1_480x480);
        /* libdlna l5 */stream.addResolution(Resolution._9_16_VGA_4_3_480x360);
        /* libdlna l5 */stream.addResolution(Resolution._9_16_VGA_16_9_480x270);
        /* gupnp */stream.addResolution(Resolution._625_1_2D1_352x576);
        /* gupnp */stream.addResolution(Resolution._525_1_2D1_352x480);
        /* gupnp */stream.addResolution(Resolution._CIF_352x288);
        /* gupnp */stream.addResolution(Resolution._525SIF_352x240);
        /* gupnp */stream.addResolution(Resolution._QVGA_4_3_320x240);
        /* gupnp */stream.addResolution(Resolution._QVGA_16_9_320x180);
        /* gupnp */stream.addResolution(Resolution._1_7_VGA_4_3_240x180);
        /* gupnp */stream.addResolution(Resolution._1_9_VGA_4_3_208x160);
        /* gupnp */stream.addResolution(Resolution._QCIF_176x144);
        /* gupnp */stream.addResolution(Resolution._525QSIF8_176x120);
        /* gupnp */stream.addResolution(Resolution._SQVGA_4_3_160x120);
        /* gupnp */stream.addResolution(Resolution._1_16_VGA_4_3_160x112);
        /* gupnp */stream.addResolution(Resolution._1280x720);

        /* libdlna l5 */stream.addResolution(Resolution._SQVGA_16_9_160x90);
        /* libdlna l5 */stream.addResolution(Resolution._SQCIF_128x96);
        /* libdlna l5 */stream.addFrameRate(FrameRate.FPS_30);
        return stream;
    }

    /**
     * http://www.hthoma.de/video/mpeg4_video_tut/index.html
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupSpAndAspLevel3(Mpeg4VideoStream stream) {

        stream.addResolution(Resolution._CIF_352x288);
        stream.addResolution(Resolution._525SIF_352x240);
        stream.addResolution(Resolution._QVGA_4_3_320x240);
        stream.addResolution(Resolution._QVGA_16_9_320x180);
        stream.addResolution(Resolution._1_7_VGA_4_3_240x180);
        stream.addResolution(Resolution._1_9_VGA_4_3_208x160);
        stream.addResolution(Resolution._QCIF_176x144);
        stream.addResolution(Resolution._525QSIF8_176x120);
        stream.addResolution(Resolution._SQVGA_4_3_160x120);
        stream.addResolution(Resolution._1_16_VGA_4_3_160x112);
        stream.addResolution(Resolution._SQVGA_16_9_160x90);
        stream.addResolution(Resolution._SQCIF_128x96);
        stream.addFrameRate(FrameRate.FPS_30);
        return stream;
    }

    public MPEG4Part2(String id) {
        super(id);

        // Audio Streams

    }

}
