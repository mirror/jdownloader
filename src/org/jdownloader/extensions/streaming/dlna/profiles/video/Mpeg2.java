package org.jdownloader.extensions.streaming.dlna.profiles.video;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;
import org.jdownloader.extensions.streaming.dlna.profiles.container.AbstractMediaContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer;
import org.jdownloader.extensions.streaming.dlna.profiles.container.VideoTransportStreamContainer.TimeStamp;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.AACAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.LPCMAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.Mpeg2AudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.Mpeg2VideoStream;

public class Mpeg2 extends AbstractMpegProfile {

    public static final String                         LEVEL_HIGH                                    = "high";
    public static final String                         LEVEL_HIGH_1440                               = "high-1440";
    public static final String                         LEVEL_LOW                                     = "low";
    public static final String                         PROFILE_SIMPLE                                = "simple";
    public static final String                         LEVEL_MAIN                                    = "main";
    private static final VideoTransportStreamContainer VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS   = new VideoTransportStreamContainer(TimeStamp.VALID, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T);
    private static final VideoTransportStreamContainer VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS = new VideoTransportStreamContainer(TimeStamp.WITHOUT, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T);
    private static final VideoTransportStreamContainer VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS    = new VideoTransportStreamContainer(TimeStamp.ZERO, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2T);
    public static final VideoTransportStreamContainer  VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_PS    = new VideoTransportStreamContainer(TimeStamp.ZERO, Extensions.AUDIO_VIDEO_PS, Extensions.AUDIO_VIDEO_MP2P, Extensions.AUDIO_VIDEO_MP2P);
    public static final Mpeg2                          MPEG_PS_NTSC                                  = new Mpeg2("MPEG_PS_NTSC") {

                                                                                                         {
                                                                                                             mimeType = MimeType.VIDEO_MPEG;
                                                                                                             setupAudioPrivateAc3(this);
                                                                                                             setupAudioAc3(this);
                                                                                                             setupAudioMp2(this);
                                                                                                             setupAudioLpcm(this);
                                                                                                             this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_PS };

                                                                                                             addVideoStream(setup720x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup704x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup640x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup480x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup544x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup352x480(createMpegTsVideo()));
                                                                                                             addVideoStream(setup352x240(createMpegTsVideo()));

                                                                                                         }
                                                                                                     };

    public static void init() {
    }

    public static final Mpeg2   MPEG_PS_NTSC_XAC3      = new Mpeg2("MPEG_PS_NTSC_XAC3") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupXAc3(this);

                                                               addVideoStream(setup720x480(createMpegTsVideo()));
                                                               addVideoStream(setup704x480(createMpegTsVideo()));
                                                               addVideoStream(setup640x480(createMpegTsVideo()));
                                                               addVideoStream(setup480x480(createMpegTsVideo()));
                                                               addVideoStream(setup544x480(createMpegTsVideo()));
                                                               addVideoStream(setup352x480(createMpegTsVideo()));
                                                               addVideoStream(setup352x240(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_PS };

                                                           }
                                                       };

    public static final Mpeg2   MPEG_PS_PAL            = new Mpeg2("MPEG_PS_PAL") {

                                                           {
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_PS };
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);

                                                               addVideoStream(setupEU720x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU704x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU544x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU480x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU352x576(createMpegTsVideo()));
                                                               // libdlna claims that the last one is 252x288. 352 seems more
                                                               // likly
                                                               addVideoStream(setupEU352x288(createMpegTsVideo()));

                                                           }
                                                       };

    public static final Mpeg2   MPEG_PS_PAL_XAC3       = new Mpeg2("MPEG_PS_PAL_XAC3") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupXAc3(this);
                                                               // Video Streams
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_PS };

                                                               addVideoStream(setupEU720x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU704x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU544x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU480x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU352x576(createMpegTsVideo()));
                                                               // libdlna claims that the last one is 252x288. 352 seems more
                                                               // likly
                                                               addVideoStream(setupEU352x288(createMpegTsVideo()));

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_HD_NA          = new Mpeg2("MPEG_TS_HD_NA") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };

                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);

                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_HD_NA_ISO      = new Mpeg2("MPEG_TS_HD_NA_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);

                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_HD_NA_T        = new Mpeg2("MPEG_TS_HD_NA_T") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_HD_NA_XAC3     = new Mpeg2("MPEG_TS_HD_NA_XAC3") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               // setupAc3(this);
                                                               // setupPrivateAc3(this);
                                                               setupXAc3(this);
                                                               // Video Streams
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };

                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));

                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_HD_NA_XAC3_ISO = new Mpeg2("MPEG_TS_HD_NA_XAC3_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               // setupAc3(this);
                                                               // setupPrivateAc3(this);
                                                               setupXAc3(this);
                                                               // Video Streams
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_HD_NA_XAC3_T   = new Mpeg2("MPEG_TS_HD_NA_XAC3_T") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               // setupAc3(this);
                                                               // setupPrivateAc3(this);
                                                               setupXAc3(this);
                                                               // Video Streams

                                                               addVideoStream(setup1920x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x720(createMpegTsVideo()));
                                                               addVideoStream(setup1440x1080(createMpegTsVideo()));
                                                               addVideoStream(setup1280x1080(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_MP_LL_AAC      = new Mpeg2("MPEG_TS_MP_LL_AAC") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               addAudioStream(new AACAudioStream().addBitrateRange(1, 256000));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };

                                                               InternalVideoStream stream = new Mpeg2VideoStream().setLevel(new String[] { LEVEL_LOW });
                                                               stream.addBitrateRange(1, 4000000);
                                                               stream.setSystemStream(false);
                                                               stream.addProfileTags(PROFILE_MAIN);
                                                               stream.addResolution(Resolution._CIF_352x288);
                                                               stream.addFrameRate(FrameRate.FPS_30);

                                                               // Video Streams

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_MP_LL_AAC_ISO  = new Mpeg2("MPEG_TS_MP_LL_AAC_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               // setupAc3(this);
                                                               // setupPrivateAc3(this);

                                                               addAudioStream(new AACAudioStream().addBitrateRange(1, 256000));
                                                               InternalVideoStream stream = new Mpeg2VideoStream().setLevel(new String[] { LEVEL_LOW });
                                                               stream.addBitrateRange(1, 4000000);
                                                               stream.setSystemStream(false);
                                                               stream.addProfileTags(PROFILE_MAIN);
                                                               stream.addResolution(Resolution._CIF_352x288);
                                                               stream.addFrameRate(FrameRate.FPS_30);
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                               // Video Streams

                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_MP_LL_AAC_T    = new Mpeg2("MPEG_TS_MP_LL_AAC_T") {

                                                           {
                                                               // setupAc3(this);
                                                               // setupPrivateAc3(this);
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               addAudioStream(new AACAudioStream().addBitrateRange(1, 256000));
                                                               InternalVideoStream stream = new Mpeg2VideoStream().setLevel(new String[] { LEVEL_LOW });
                                                               stream.addBitrateRange(1, 4000000);
                                                               stream.setSystemStream(false);
                                                               stream.addProfileTags(PROFILE_MAIN);
                                                               stream.addResolution(Resolution._CIF_352x288);
                                                               stream.addFrameRate(FrameRate.FPS_30);

                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                               // Video Streams

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_EU          = new Mpeg2("MPEG_TS_SD_EU") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };

                                                               addVideoStream(setupEU720x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU544x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU480x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU352x576(createMpegTsVideo()));
                                                               // libdlna claims that the last one is 252x288. 352 seems more
                                                               // likly
                                                               addVideoStream(setupEU352x288(createMpegTsVideo()));

                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_SD_EU_ISO      = new Mpeg2("MPEG_TS_SD_EU_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               addVideoStream(setupEU720x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU544x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU480x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU352x576(createMpegTsVideo()));
                                                               // libdlna claims that the last one is 252x288. 352 seems more
                                                               // likly
                                                               addVideoStream(setupEU352x288(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_EU_T        = new Mpeg2("MPEG_TS_SD_EU_T") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);

                                                               addVideoStream(setupEU720x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU544x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU480x576(createMpegTsVideo()));
                                                               addVideoStream(setupEU352x576(createMpegTsVideo()));
                                                               // libdlna claims that the last one is 252x288. 352 seems more
                                                               // likly
                                                               addVideoStream(setupEU352x288(createMpegTsVideo()));
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_SD_NA          = new Mpeg2("MPEG_TS_SD_NA") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };

                                                               setupTransportStreamsSdNa();

                                                           }
                                                       };
    public static final Mpeg2   MPEG_TS_SD_NA_ISO      = new Mpeg2("MPEG_TS_SD_NA_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               setupTransportStreamsSdNa();
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_NA_T        = new Mpeg2("MPEG_TS_SD_NA_T") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupAudioPrivateAc3(this);
                                                               setupAudioAc3(this);
                                                               setupAudioMp2(this);
                                                               setupAudioLpcm(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                               setupTransportStreamsSdNa();

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_NA_XAC3     = new Mpeg2("MPEG_TS_SD_NA_XAC3") {

                                                           {

                                                               setupXAc3(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_ZEROTS_TS };
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupTransportStreamsSdNa();

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_NA_XAC3_ISO = new Mpeg2("MPEG_TS_SD_NA_XAC3_ISO") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG;
                                                               setupXAc3(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_WITHOUTTS_TS };
                                                               setupTransportStreamsSdNa();

                                                           }
                                                       };

    public static final Mpeg2   MPEG_TS_SD_NA_XAC3_T   = new Mpeg2("MPEG_TS_SD_NA_XAC3_T") {

                                                           {
                                                               mimeType = MimeType.VIDEO_MPEG_TS;
                                                               setupXAc3(this);
                                                               this.containers = new AbstractMediaContainer[] { VIDEO_TRANSPORT_STREAM_CONTAINER_VALIDTS_TS };
                                                               setupTransportStreamsSdNa();

                                                           }
                                                       };

    // ES Encapsulation over rtp
    public static final Mpeg2   MPEG_ES_NTSC           = MPEG_PS_NTSC.clone("MPEG_ES_NTSC");                    // ES
    // Encapsulation
    // over
    // rtp
    public static final Mpeg2   MPEG_ES_NTSC_XAC3      = MPEG_PS_NTSC_XAC3.clone("MPEG_ES_NTSC_XAC3");

    public static final Mpeg2   MPEG_ES_PAL            = MPEG_PS_PAL.clone("MPEG_ES_PAL");                      // es
    // encapsulation
    // via
    // RTP
    public static final Mpeg2   MPEG_ES_PAL_XAC3       = MPEG_PS_PAL_XAC3.clone("MPEG_ES_PAL_XAC3");

    public static final Mpeg2   MPEG_TS_HD_KO          = MPEG_TS_HD_NA.clone("MPEG_TS_HD_KO");
    public static final Mpeg2   MPEG_TS_HD_KO_ISO      = MPEG_TS_HD_NA_ISO.clone("MPEG_TS_HD_KO_ISO");
    public static final Mpeg2   MPEG_TS_HD_KO_T        = MPEG_TS_HD_NA_T.clone("MPEG_TS_HD_KO_T");
    public static final Mpeg2   MPEG_TS_HD_KO_XAC3     = MPEG_TS_HD_NA_XAC3.clone("MPEG_TS_HD_KO_XAC3");
    public static final Mpeg2   MPEG_TS_HD_KO_XAC3_ISO = MPEG_TS_HD_NA_XAC3_ISO.clone("MPEG_TS_HD_KO_XAC3_ISO");
    public static final Mpeg2   MPEG_TS_HD_KO_XAC3_T   = MPEG_TS_HD_NA_XAC3_T.clone("MPEG_TS_HD_KO_XAC3_T");

    public static final Mpeg2   MPEG_TS_SD_KO          = MPEG_TS_SD_NA.clone("MPEG_TS_SD_KO");
    public static final Mpeg2   MPEG_TS_SD_KO_ISO      = MPEG_TS_SD_NA_ISO.clone("MPEG_TS_SD_KO_ISO");
    public static final Mpeg2   MPEG_TS_SD_KO_T        = MPEG_TS_SD_NA_T.clone("MPEG_TS_SD_KO_T");
    public static final Mpeg2   MPEG_TS_SD_KO_XAC3     = MPEG_TS_SD_NA_XAC3.clone("MPEG_TS_SD_KO_XAC3");
    public static final Mpeg2   MPEG_TS_SD_KO_XAC3_ISO = MPEG_TS_SD_NA_XAC3_ISO.clone("MPEG_TS_SD_KO_XAC3_ISO");
    public static final Mpeg2   MPEG_TS_SD_KO_XAC3_T   = MPEG_TS_SD_NA_XAC3_T.clone("MPEG_TS_SD_KO_XAC3_T");
    private static final String PROFILE_MAIN           = "main";

    protected static InternalVideoStream createMpegTsVideo() {
        InternalVideoStream stream = new Mpeg2VideoStream().setLevel(new String[] { LEVEL_LOW, LEVEL_MAIN, LEVEL_HIGH_1440, LEVEL_HIGH });
        stream.addBitrateRange(1, 18881700);
        stream.setSystemStream(false);
        stream.addProfileTags(PROFILE_MAIN, PROFILE_SIMPLE);
        return stream;
    }

    /**
     * HD
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup1280x1080(InternalVideoStream stream) {
        stream.addResolution(Resolution._1280x1080);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addPixelAspectRatio(3, 2);

        return stream;
    }

    /**
     * HD
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup1280x720(InternalVideoStream stream) {
        stream.addResolution(Resolution._1280x720);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addPixelAspectRatio(1, 1);
        stream.addPixelAspectRatio(9, 16);
        return stream;
    }

    /**
     * HD
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup1440x1080(InternalVideoStream stream) {
        stream.addResolution(Resolution._1440x1080);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addPixelAspectRatio(4, 3);

        return stream;
    }

    /**
     * HD
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup1920x1080(InternalVideoStream stream) {
        stream.addResolution(Resolution._1920x1080);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addPixelAspectRatio(1, 1);
        stream.addPixelAspectRatio(9, 16);
        return stream;
    }

    protected static InternalVideoStream setup352x240(InternalVideoStream stream) {
        stream.addResolution(Resolution._SIF_352x240);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        // unknown aspect ratios. could not find infos about this
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup352x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._525_1_2D1_352x480);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addPixelAspectRatio(80, 33);
        stream.addPixelAspectRatio(20, 11);
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup480x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._525_2_3D1_480x480);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addPixelAspectRatio(16, 9);
        stream.addPixelAspectRatio(4, 3);
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup544x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._525_3_4D1_544x480);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addPixelAspectRatio(80, 51);
        stream.addPixelAspectRatio(20, 17);
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup640x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._VGA_640x480);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_59_94_60000_1001);
        stream.addFrameRate(FrameRate.FPS_60);
        stream.addPixelAspectRatio(1, 1);
        stream.addPixelAspectRatio(4, 3);
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup704x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._525_4SIF_704x480);
        stream.addFrameRate(FrameRate.FPS_23_97_24000_1001);
        stream.addFrameRate(FrameRate.FPS_24);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addFrameRate(FrameRate.FPS_30);
        stream.addFrameRate(FrameRate.FPS_59_94_60000_1001);
        stream.addFrameRate(FrameRate.FPS_60);
        stream.addPixelAspectRatio(40, 33);
        stream.addPixelAspectRatio(10, 11);
        return stream;
    }

    /**
     * SD NorthernAmerica
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setup720x480(InternalVideoStream stream) {
        stream.addResolution(Resolution._525_D1_720x480);
        stream.addFrameRate(FrameRate.FPS_29_97_30000_1001);
        stream.addPixelAspectRatio(32, 27);
        stream.addPixelAspectRatio(8, 9);
        return stream;
    }

    private static void setupAudioAc3(Mpeg2 mpegtsVideo) {
        mpegtsVideo.addAudioStream(new InternalAudioStream("audio/ac3").addChannelRange(3, 6).addBitrateRange(0, 448000).addSamplingRateRange(48000, 48000));

    }

    protected static void setupAudioLpcm(Mpeg2 mpegtsVideo) {

        mpegtsVideo.addAudioStream(new LPCMAudioStream(null).addBitrateRange(1, 1536000).addChannelRange(2, 2));
        mpegtsVideo.addAudioStream(new LPCMAudioStream(null).addBitrateRange(1, 768000).addChannelRange(1, 1));
    }

    protected static void setupAudioMp2(Mpeg2 mpegtsVideo) {
        mpegtsVideo.addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(1, 1).addBitrateRange(64000, 192000));
        mpegtsVideo.addAudioStream(new Mpeg2AudioStream(1, 1).addChannelRange(2, 2).addBitrateRange(64000, 384000));
    }

    private static void setupAudioPrivateAc3(Mpeg2 mpegtsVideo) {
        mpegtsVideo.addAudioStream(new InternalAudioStream("audio/x-private1-ac3").addChannelRange(3, 6).addBitrateRange(0, 448000).addSamplingRateRange(48000, 48000));
    }

    /**
     * EU
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupEU352x288(InternalVideoStream stream) {
        stream.addResolution(Resolution._CIF_352x288);
        stream.addFrameRate(FrameRate.FPS_25);
        stream.addPixelAspectRatio(16, 11);
        stream.addPixelAspectRatio(12, 11);
        return stream;
    }

    /**
     * EU
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupEU352x576(InternalVideoStream stream) {
        stream.addResolution(Resolution._625_1_2D1_352x576);
        stream.addFrameRate(FrameRate.FPS_25);
        stream.addPixelAspectRatio(32, 11);
        stream.addPixelAspectRatio(24, 11);
        return stream;
    }

    /**
     * EU
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupEU480x576(InternalVideoStream stream) {
        stream.addResolution(Resolution._625_2_3D1_480x576);
        stream.addFrameRate(FrameRate.FPS_25);
        stream.addPixelAspectRatio(32, 15);
        stream.addPixelAspectRatio(8, 5);
        return stream;
    }

    /**
     * EU
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupEU544x576(InternalVideoStream stream) {
        stream.addResolution(Resolution._625_3_4D1_544x576);
        stream.addFrameRate(FrameRate.FPS_25);
        stream.addPixelAspectRatio(32, 17);
        stream.addPixelAspectRatio(24, 17);
        return stream;
    }

    protected static InternalVideoStream setupEU704x576(InternalVideoStream stream) {
        stream.addResolution(Resolution._625_4SIF_704x576);
        stream.addFrameRate(FrameRate.FPS_25);
        // unknown aspect ratios. could not find infos about this
        return stream;
    }

    /**
     * EU
     * 
     * @param stream
     * @return
     */
    protected static InternalVideoStream setupEU720x576(InternalVideoStream stream) {
        stream.addResolution(Resolution._625_D1_720x576);
        stream.addFrameRate(FrameRate.FPS_25);
        stream.addPixelAspectRatio(64, 45);
        stream.addPixelAspectRatio(16, 15);
        return stream;
    }

    private static void setupXAc3(Mpeg2 mpegtsVideo) {
        mpegtsVideo.addAudioStream(new InternalAudioStream("audio/x-ac3").addChannelRange(3, 6).addBitrateRange(0, 640000).addSamplingRateRange(48000, 48000));
    }

    public Mpeg2(String id) {
        super(id);

        // Audio Streams

    }

    private Mpeg2 clone(String string) {
        Mpeg2 ret = new Mpeg2(string);
        ret.containers = containers;
        ret.mimeType = mimeType;
        ret.profileTags = profileTags;
        ret.getAudioStreams().addAll(getAudioStreams());
        ret.getVideoStreams().addAll(getVideoStreams());
        return ret;
    }

    protected void setupTransportStreamsSdNa() {
        addVideoStream(setup720x480(createMpegTsVideo()));
        addVideoStream(setup704x480(createMpegTsVideo()));
        addVideoStream(setup640x480(createMpegTsVideo()));
        addVideoStream(setup480x480(createMpegTsVideo()));
        addVideoStream(setup544x480(createMpegTsVideo()));
        addVideoStream(setup352x480(createMpegTsVideo()));
    }

}
