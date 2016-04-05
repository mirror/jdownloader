package org.jdownloader.plugins.components.youtube;

import java.util.Date;

import org.jdownloader.gui.translate._GUI;

import jd.http.QueryInfo;

public enum YoutubeITAG {
    DASH_AUDIO_128K_AAC(140, null, null, "AAC", "128kbit", AudioCodec.AAC, AudioBitrate.KBIT_128),
    // DASH_AUDIO_48K_OPUS(249, null, null, "Opus", "38kbit", YoutubeITAG.OPUS_48),
    DASH_AUDIO_128K_WEBM(171, null, null, "Vorbis", "128kbit", AudioCodec.VORBIS, AudioBitrate.KBIT_128),
    DASH_AUDIO_192K_WEBM(172, null, null, "Vorbis", "192kbit", AudioCodec.VORBIS, AudioBitrate.KBIT_192),
    DASH_AUDIO_256K_AAC(141, null, null, "AAC", "256kbit", AudioCodec.AAC, AudioBitrate.KBIT_256),
    DASH_AUDIO_48K_AAC(139, null, null, "AAC", "48kbit", AudioCodec.AAC, AudioBitrate.KBIT_48),
    DASH_AUDIO_OPUS_160KBIT(251, null, null, "Opus", "160kbit", AudioCodec.OPUS, AudioBitrate.KBIT_160) {
        @Override
        public String getCustomQualityExtension(Object caller) {
            return getQualityAudio() + " Opus";
        }
    },
    DASH_AUDIO_OPUS_48KBIT(249, null, null, "Opus", "48kbit", AudioCodec.OPUS, AudioBitrate.KBIT_48) {
        @Override
        public String getCustomQualityExtension(Object caller) {
            return getQualityAudio() + " Opus";
        }
    },
    DASH_AUDIO_OPUS_64KBIT(250, null, null, "Opus", "64kbit", AudioCodec.OPUS, AudioBitrate.KBIT_64) {
        @Override
        public String getCustomQualityExtension(Object caller) {
            return getQualityAudio() + " Opus";
        }
    },
    DASH_VIDEO_1080_H264_FPS60(299, "H264", "1080p 60fps", null, null, VideoResolution.P_1080, VideoContainer.MP4, VideoCodec.H264, MediaTagsVarious.VIDEO_FPS_60),
    DASH_VIDEO_1080P_H264(137, "H264", "1080p", null, null, VideoResolution.P_1080, VideoContainer.MP4, VideoCodec.H264),
    // http://www.youtube.com/watch?v=gBabKoHSErI
    DASH_VIDEO_1440P_H264(264, "H264", "1440p", null, null, VideoResolution.P_1440, VideoContainer.MP4, VideoCodec.H264),
    DASH_VIDEO_144P_H264(160, "H264", "144p", null, null, VideoResolution.P_144, VideoContainer.MP4, VideoCodec.H264),

    DASH_VIDEO_2160_H264_FPS_60(266, "H264", "2160p 60fps", null, null, VideoResolution.P_2160, VideoContainer.MP4, VideoCodec.H264, MediaTagsVarious.VIDEO_FPS_60),
    DASH_VIDEO_2160_H264(266, "H264", "2160p", null, null, VideoResolution.P_2160, VideoContainer.MP4, VideoCodec.H264),

    DASH_VIDEO_240P_H264(133, "H264", "240p", null, null, VideoResolution.P_240, VideoContainer.MP4, VideoCodec.H264),

    DASH_VIDEO_360P_H264(134, "H264", "360p", null, null, VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264),
    DASH_VIDEO_480P_H264(135, "H264", "480p", null, null, VideoResolution.P_480, VideoContainer.MP4, VideoCodec.H264),
    DASH_VIDEO_720_H264_FPS60(298, "H264", "720p 60fps", null, null, VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, MediaTagsVarious.VIDEO_FPS_60),

    DASH_VIDEO_720P_H264(136, "H264", "720p", null, null, VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264),
    DASH_VIDEO_ITAG308_VP9_1440P_60FPS(308, "VP9", "1440p 60fps", null, null, VideoResolution.P_1440, VideoContainer.WEBM, VideoCodec.VP9, MediaTagsVarious.VIDEO_FPS_60),
    DASH_VIDEO_ITAG313_VP9_2160P_30FPS(313, "vp9 Low Quality Profile", "2160p", null, null, VideoResolution.P_2160, VideoContainer.WEBM, VideoCodec.VP9_WORSE_PROFILE_1),
    DASH_VIDEO_ITAG315_VP9_2160P_60FPS(315, "VP9", "2160p 60fps", null, null, VideoResolution.P_2160, VideoContainer.WEBM, VideoCodec.VP9, MediaTagsVarious.VIDEO_FPS_60),
    // has usually a lower quality than DASH_VIDEO_2160_H264_FPS_60
    DASH_VIDEO_ORIGINAL_H264(138, "H264", "Original (2160p)", null, null, VideoResolution.P_2160_ESTIMATED, VideoContainer.MP4, VideoCodec.H264),
    // https://www.youtube.com/watch?v=sLprVF6d7Ug
    DASH_VIDEO_ORIGINAL_H264_4320P_24FPS(138, "H264", "4320p 24fps", null, null, VideoResolution.P_4320, VideoContainer.MP4, VideoCodec.H264),

    // https://www.youtube.com/watch?v=ZSn3Tvc7jQU
    // DASH_WEBM_VIDEO_1080P_VP9_60FPS(299, "VP9", "1080p", null, null, VideoResolution.VIDEO_RESOLUTION_1080P,
    // VideoCodec.VIDEO_CODEC_VP9),
    DASH_WEBM_VIDEO_1080P_VP9(248, "VP9", "1080p", null, null, VideoResolution.P_1080, VideoContainer.WEBM, VideoCodec.VP9),
    // https://www.youtube.com/watch?v=T3ny9zIckP0
    // the 2610 stream on itag 266 is deklared as 60fps stream in the backend, but it is actually just 30fps. maybe 60fps will come soon on
    // the same itag?

    DASH_WEBM_VIDEO_1080P_VP9_60FPS(303, "VP9", "1080p 60fps", null, null, VideoResolution.P_1080, VideoContainer.WEBM, VideoCodec.VP9, MediaTagsVarious.VIDEO_FPS_60),
    DASH_WEBM_VIDEO_1440P_VP9(271, "VP9", "1440p", null, null, VideoResolution.P_1440, VideoContainer.WEBM, VideoCodec.VP9),

    DASH_WEBM_VIDEO_144P_VP9(278, "VP9", "144p", null, null, VideoResolution.P_144, VideoContainer.WEBM, VideoCodec.VP9),
    // DASH_WEBM_VIDEO_720P_VP9(247, "VP9", "720p", null, null, VideoResolution.VIDEO_RESOLUTION_720P,
    // VideoContainer.WEBM, VideoCodec.VIDEO_CODEC_VP9),
    // https://www.youtube.com/watch?v=kdKgvII-pAg
    DASH_WEBM_VIDEO_2160P_VP9(272, "VP9", "2160p", null, null, VideoResolution.P_2160, VideoContainer.WEBM, VideoCodec.VP9),
    DASH_WEBM_VIDEO_240P_VP9(242, "VP9", "240p", null, null, VideoResolution.P_240, VideoContainer.WEBM, VideoCodec.VP9),
    DASH_WEBM_VIDEO_360P_VP9(243, "VP9", "360p", null, null, VideoResolution.P_360, VideoContainer.WEBM, VideoCodec.VP9),
    DASH_WEBM_VIDEO_480P_VP9(244, "VP9", "480p", null, null, VideoResolution.P_480, VideoContainer.WEBM, VideoCodec.VP9),
    DESCRIPTION(10002, "txt", null, null, null, MediaTagsVarious.DESCRIPTION) {
        @Override
        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_DESCRIPTION();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_DESCRIPTION();
        }

    },
    DASH_WEBM_VIDEO_480P_VP9_2(245, "vp9 High Quality Profile", "480p", null, null, VideoResolution.P_480, VideoContainer.WEBM, VideoCodec.VP9_BETTER_PROFILE_1),
    DASH_WEBM_VIDEO_480P_VP9_3(246, "vp9 Higher Quality Profile", "480p", null, null, VideoResolution.P_480, VideoContainer.WEBM, VideoCodec.VP9_BETTER_PROFILE_2),
    DASH_WEBM_VIDEO_720P_VP9(247, "VP9", "720p", null, null, VideoResolution.P_720, VideoContainer.WEBM, VideoCodec.VP9),
    // https://www.youtube.com/watch?v=T3ny9zIckP0
    DASH_WEBM_VIDEO_720P_VP9_60FPS(302, "VP9", "720p 60fps", null, null, VideoResolution.P_720, VideoContainer.WEBM, VideoCodec.VP9, MediaTagsVarious.VIDEO_FPS_60),
    FLV_VIDEO_360P_H264_AUDIO_AAC(34, "H264", "360p", "AAC", "128kbit", VideoResolution.P_360, VideoContainer.FLV, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    FLV_VIDEO_480P_H264_AUDIO_AAC(35, "H264", "480p", "AAC", "128kbit", VideoResolution.P_480, VideoContainer.FLV, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3(6, "Sorenson H.263", "270p", "MP3", "64kbit", VideoResolution.P_270, VideoContainer.FLV, VideoCodec.H263, AudioCodec.MP3, AudioBitrate.KBIT_64),
    FLV_VIDEO_LOW_240P_H263_AUDIO_MP3(5, "Sorenson H.263", "240p", "MP3", "64kbit", VideoResolution.P_240, VideoContainer.FLV, VideoCodec.H263, AudioCodec.MP3, AudioBitrate.KBIT_64),
    // we do not need such a bad itag. Ignore the variants

    THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT(13, "h263", "144p", "AMRNB", "12kbit", VideoResolution.P_144, VideoContainer.THREEGP, VideoCodec.H263, AudioCodec.AMRNB, AudioBitrate.KBIT_12, MediaTagsVarious.VIDEO_FPS_15),
    // fake id
    IMAGE_HQ(10002, "jpg", "480x360", null, null, ImageQuality.HIGH) {
        @Override
        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_HQ();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_HQ();
        }
    },
    // fake id
    IMAGE_LQ(10004, "jpg", "120x90", null, null, ImageQuality.LOW) {
        @Override
        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_LQ();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_LQ();
        }
    },

    // fake id
    IMAGE_MAX(10001, "jpg", "1400x1080", null, null, ImageQuality.HIGHEST) {
        @Override
        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_MAX();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MAX();
        }
    },

    // fake id
    IMAGE_MQ(10003, "jpg", "320x180", null, null, ImageQuality.NORMAL) {
        @Override
        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_MQ();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MQ();
        }
    },

    // 192 kbits aac
    MP4_VIDEO_1080P_H264_AUDIO_AAC(37, "H264", "1080p", "AAC", "192kbit", VideoResolution.P_1080, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_192),
    MP4_VIDEO_1080P_H264_AUDIO_AAC_3D(85, "H264", "1080p", "AAC", "192kbit", VideoResolution.P_1080, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_192),
    // not sure
    MP4_VIDEO_240P_H264_AUDIO_AAC_3D(83, "H264", "240p", "AAC", "96kbit", VideoResolution.P_240, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_96),

    MP4_VIDEO_360P_H264_AUDIO_AAC(18, "H264", "360p", "AAC", "128kbit", VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    MP4_VIDEO_360P_H264_AUDIO_AAC_3D(82, "H264", "360p", "AAC", "128kbit", VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1(82, "H264", "360p", "AAC", "96kbit", VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_96),
    MP4_VIDEO_360P_H264_AUDIO_AAC_V1(18, "H264", "360p", "AAC", "96kbit", VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_96),
    // 192 kbits aac
    MP4_VIDEO_720P_H264_AUDIO_AAC(22, "H264", "720p", "AAC", "192kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_192),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D(84, "H264", "720p", "AAC", "192kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_192),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1(84, "H264", "720p", "AAC", "128kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3(84, "H264", "720p", "AAC", "152kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_152),
    MP4_VIDEO_720P_H264_AUDIO_AAC_V1(22, "H264", "720p", "AAC", "128kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    MP4_VIDEO_720P_H264_AUDIO_AAC_V3(22, "H264", "720p", "AAC", "152kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_152),
    // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
    MP4_VIDEO_AUDIO_ORIGINAL(38, "H264", "Original", "AAC", "192kbit", VideoResolution.P_2160, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_192),
    // fake id
    SUBTITLE(10002, "srt", null, null, null, MediaTagsVarious.SUBTITLE) {

        public String getCustomName(Object caller) {
            return _GUI.T.YoutubeVariant_name_SUBTITLES();
        }

        @Override
        public String getCustomQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_SUBTITLES();
        }

    },

    // very different audio bitrates!!!
    THREEGP_VIDEO_144P_H264_AUDIO_AAC(17, "MPEG-4 Visual", "144p", "AAC", "24kbit", VideoResolution.P_144, VideoContainer.THREEGP, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_32_ESTIMATED),

    // LIve stream?
    // I tested several streams, and this itag contains 180p and 32kbit audio
    THREEGP_VIDEO_240P_H263_AUDIO_AAC(132, "MPEG-4 Visual", "180p", "AAC", "32kbit", VideoResolution.P_240, VideoContainer.THREEGP, VideoCodec.H263, AudioCodec.AAC, AudioBitrate.KBIT_32) {
        @Override
        public String getCustomQualityExtension(Object caller) {
            if (caller != null && caller instanceof YoutubeVariant) {
                switch (((YoutubeVariant) caller).getGroup()) {
                case VIDEO:
                case VIDEO_3D:
                    return getQualityVideo() + " [LQ]";
                default:
                    return null;
                }

            }
            return null;
        }
    },

    THREEGP_VIDEO_240P_H264_AUDIO_AAC(36, "MPEG-4 Visual", "180p", "AAC", "32kbit", VideoResolution.P_240, VideoContainer.THREEGP, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_32) {
        @Override
        public String getCustomQualityExtension(Object caller) {
            if (caller != null && caller instanceof YoutubeVariant) {
                switch (((YoutubeVariant) caller).getGroup()) {
                case VIDEO:
                case VIDEO_3D:
                    return getQualityVideo() + " [HQ]";
                default:
                    return null;
                }

            }
            return null;
        }
    },
    // not sure - did not find testvideos
    WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS(46, "VP8", "1080p", "Vorbis", "192kbit", VideoResolution.P_1080, VideoContainer.WEBM, VideoCodec.VP8, AudioCodec.VORBIS, AudioBitrate.KBIT_192),

    WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D(100, "VP8", "360p", "Vorbis", "128kbit", VideoResolution.P_360, VideoContainer.WEBM, VideoCodec.VP9, AudioCodec.VORBIS, AudioBitrate.KBIT_128),
    WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D(101, "VP8", "360p", "Vorbis", "192kbit", VideoResolution.P_360, VideoContainer.WEBM, VideoCodec.VP9, AudioCodec.VORBIS, AudioBitrate.KBIT_192),
    WEBM_VIDEO_360P_VP8_AUDIO_VORBIS(43, "VP8", "360p", "Vorbis", "128kbit", VideoResolution.P_360, VideoContainer.WEBM, VideoCodec.VP8, AudioCodec.VORBIS, AudioBitrate.KBIT_128),
    // not sure - did not find testvideos
    WEBM_VIDEO_480P_VP8_AUDIO_VORBIS(44, "VP8", "480p", "Vorbis", "128kbit", VideoResolution.P_480, VideoContainer.WEBM, VideoCodec.VP8, AudioCodec.VORBIS, AudioBitrate.KBIT_128),
    WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D(102, "VP8", "720p", "Vorbis", "192kbit", VideoResolution.P_720, VideoContainer.WEBM, VideoCodec.VP8, AudioCodec.VORBIS, AudioBitrate.KBIT_192),
    // https://www.youtube.com/watch?v=n-BXNXvTvV4
    // both Itags are almost the same video
    MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT(59, "H264", "480p", "AAC", "128kbit", VideoResolution.P_480, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT(78, "H264", "480p", "AAC", "128kbit", VideoResolution.P_480, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),

    // not sure - did not find testvideos
    WEBM_VIDEO_720P_VP8_AUDIO_VORBIS(45, "VP8", "720p", "Vorbis", "192kbit", VideoResolution.P_720, VideoContainer.WEBM, VideoCodec.VP8, AudioCodec.VORBIS, AudioBitrate.KBIT_192),
    // fps 6
    HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC(151, "H264", "72P", "AAC", "24kbit", VideoResolution.P_72, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_24, MediaTagsVarious.VIDEO_FPS_6),
    // fps 15
    HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC(132, "H264", "240P", "AAC", "48kbit", VideoResolution.P_240, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_48, MediaTagsVarious.VIDEO_FPS_15),

    HLS_VIDEO_MP4_240P_AUDIO_AAC_2(92, "H264", "240P", "AAC", "48kbit", VideoResolution.P_240, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_48),

    HLS_VIDEO_MP4_360P_AUDIO_AAC(93, "H264", "360P", "AAC", "128kbit", VideoResolution.P_360, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),

    HLS_VIDEO_MP4_480P_AUDIO_AAC(94, "H264", "480P", "AAC", "128kbit", VideoResolution.P_480, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_128),
    HLS_VIDEO_MP4_720P_AUDIO_AAC(95, "H264", "720p", "AAC", "256kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_256),
    HLS_VIDEO_MP4_720P_AUDIO_AAC_300(300, "H264", "720p", "AAC", "256kbit", VideoResolution.P_720, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_256),

    HLS_VIDEO_MP4_1080P_AUDIO_AAC(96, "H264", "1080p", "AAC", "256kbit", VideoResolution.P_1080, VideoContainer.MP4, VideoCodec.H264, AudioCodec.AAC, AudioBitrate.KBIT_256);

    public static enum YoutubeITAGVersion {
        // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
        // Before March 2011, YouTube used these audio qualities for several years:
        //
        // Resolution Audio Bit Rate Compression
        // 1080p 128 kbps AAC
        // 720p 128 kbps AAC
        // 480p 96 kbps AAC
        // 360p 96 kbps AAC
        // 240p 64 kbps MP3
        V1(0, new Date(2011 - 1900, 2, 1, 23, 59, 59).getTime()),
        // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
        // Videos uploaded March 2011 to May 2011 had these audio qualities, as long as the originally uploaded video had a high enough
        // audio bit rate or was lossless:
        //
        // Resolution Audio Bit Rate Compression
        // VideoResolution.VIDEO_RESOLUTION_1440Pp 128 kbps AAC
        // 720p 128 kbps AAC
        // 480p 128 kbps AAC
        // 360p 128 kbps AAC
        // 240p 64 kbps MP3
        V2(new Date(2011 - 1900, 2, 1, 0, 0, 0).getTime(), new Date(2011 - 1900, 4, 1, 23, 59, 59).getTime()),
        // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
        // Videos uploaded May 2011 to July 2012 had these audio qualities, as long as the originally uploaded video had a high enough audio
        // bit rate or was lossless:
        //
        // Resolution Audio Bit Rate Compression
        // Original 152 kbps AAC
        // 1080p 152 kbps AAC
        // 720p 152 kbps AAC
        // 480p 128 kbps AAC
        // 360p 128 kbps AAC
        // 240p 64 kbps MP3
        V3(new Date(2011 - 1900, 4, 1, 0, 0, 0).getTime(), new Date(2012 - 1900, 6, 1, 23, 59, 59).getTime()),
        // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
        // Videos uploaded July 2012 to date should have these audio qualities, as long as the originally uploaded video had a high enough
        // audio bit rate or was lossless:
        //
        // Resolution Audio Bit Rate Compression
        // Original 192 kbps AAC
        // 1080p 192 kbps AAC
        // 720p 192 kbps AAC
        // 480p 128 kbps AAC
        // 360p 128 kbps AAC
        // 240p 64 kbps MP3
        //
        // Occasionally you can find a 240p-only video that has 96 kbps audio bit rate, like this one. I'm not sure why.
        // "Original" resolution is any video size that is larger than 1920x1080. For example, if you upload a 1920x1200 or 2560x1600 video,
        // it should show the "Original" option. The audio quality is currently the same as 720p and 1080p.
        // YouTube will often update older videos to play better audio quality, if that higher quality audio was saved when it was
        // originally uploaded.
        V4(new Date(2012 - 1900, 6, 1, 0, 0, 0).getTime(), Long.MAX_VALUE);
        private long from;
        private long to;

        YoutubeITAGVersion(long from, long to) {
            this.from = from;
            this.to = to;
        }

        public boolean matches(long uploadDate) {
            return uploadDate >= from && uploadDate < to;

        }

        public static YoutubeITAGVersion getByDate(long uploadDate) {

            for (YoutubeITAGVersion v : YoutubeITAGVersion.values()) {
                if (v.matches(uploadDate)) {
                    return v;
                }
            }
            return YoutubeITAGVersion.V4;
        }

    }

    // // mp3 64 bit is lower than aac48bit
    // public static final MediaQualityTags MP3_64 = MediaQualityTags.MP3_64;0.0442;
    // public static final MediaQualityTags MP4 = MediaQualityTags.MP4;0.4;
    //
    // public static final MediaQualityTags FPS_60 = MediaQualityTags.FPS_60;0.05;
    // public static final MediaQualityTags VIDEO_RESOLUTION_1440P = VideoResolution.VIDEO_RESOLUTION_1440P;1440.0;
    //
    // public static final MediaQualityTags VIDEO_RESOLUTION_2160P = VideoResolution.VIDEO_RESOLUTION_2160P;
    // public static final MediaQualityTags VIDEO_RESOLUTION_1080P = VideoResolution.VIDEO_RESOLUTION_1080P;
    // public static final MediaQualityTags VORBIS_128 = MediaQualityTags.VORBIS_128;
    //
    // public static final MediaQualityTags VORBIS,MediaQualityTags.BITRATE_192 = MediaQualityTags.VORBIS,MediaQualityTags.BITRATE_192;
    //
    // public static final MediaQualityTags VORBIS_96 = MediaQualityTags.VORBIS_96;

    public static YoutubeITAG get(int itag, int width, int height, int fps, String type, QueryInfo query, long uploadDate) {
        YoutubeITAGVersion version = null;
        if (query == null) {
            query = new QueryInfo();
        }

        switch (itag) {
        case 132:
            // https://www.youtube.com/watch?v=KF47Za1lfjM
            if ("hls_playlist".equals(query.get("manifest")) || (type != null && (type.contains("avc") || type.contains("mp4")))) {
                return HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC;
            } else {
                return THREEGP_VIDEO_240P_H263_AUDIO_AAC;
            }

        case 138:
            switch (height) {
            case 4320:
                return DASH_VIDEO_ORIGINAL_H264_4320P_24FPS;
            default:
                return DASH_VIDEO_ORIGINAL_H264;
            }

        case 266:
            if (fps < 50) {
                return DASH_VIDEO_2160_H264;
            }
            return DASH_VIDEO_2160_H264_FPS_60;
        case 18:
            if (version == null) {
                version = YoutubeITAGVersion.getByDate(uploadDate);
            }
            switch (version) {
            case V1:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_V1;
            default:
                return MP4_VIDEO_360P_H264_AUDIO_AAC;
            }
        case 22:
            if (version == null) {
                version = YoutubeITAGVersion.getByDate(uploadDate);
            }
            switch (version) {
            case V1:
            case V2:
                return MP4_VIDEO_720P_H264_AUDIO_AAC_V1;
            case V3:
                return MP4_VIDEO_720P_H264_AUDIO_AAC_V3;
            default:
                return MP4_VIDEO_720P_H264_AUDIO_AAC;
            }
        case 82:
            if (version == null) {
                version = YoutubeITAGVersion.getByDate(uploadDate);
            }
            switch (version) {
            case V1:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1;
            default:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_3D;
            }
        case 84:
            if (version == null) {
                version = YoutubeITAGVersion.getByDate(uploadDate);
            }
            switch (version) {
            case V1:
            case V2:
                return MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1;
            case V3:
                return MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3;
            default:
                return MP4_VIDEO_720P_H264_AUDIO_AAC_3D;
            }
        }

        for (final YoutubeITAG tag : YoutubeITAG.values()) {
            if (tag.getITAG() == itag) {

                return tag;

            }
        }
        return null;
    }

    private static YoutubeITAGVersion getVersionByDate(long uploadDate) {
        return null;
    }

    private String                  codecAudio;
    private String                  codecVideo;
    private final int               itag;
    private String                  qualityAudio;

    private String                  qualityVideo;

    private MediaQualityInterface[] qualityTags;

    private double                  qualityRating = -1;

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, MediaTagsVarious various) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { various };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, ImageQuality image) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { image };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, AudioCodec tags, AudioBitrate bitrate) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { tags, bitrate };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, VideoResolution resolution, VideoContainer container, VideoCodec codec, AudioCodec audioCodec, AudioBitrate bitrate, MediaTagsVarious various) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { resolution, container, codec, audioCodec, bitrate, various };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, VideoResolution resolution, VideoContainer container, VideoCodec codec, AudioCodec audioCodec, AudioBitrate bitrate) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { resolution, container, codec, audioCodec, bitrate };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, VideoResolution resolution, VideoContainer container, VideoCodec codec, MediaTagsVarious various) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { resolution, container, codec, various };

    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, VideoResolution resolution, VideoContainer container, VideoCodec codec) {
        this(itag, codecTagVideo, qualityTagVideo, codecTagAudio, qualityTagAudio);

        this.qualityTags = new MediaQualityInterface[] { resolution, container, codec };

    }

    public boolean containsTag(MediaQualityInterface tag) {
        for (MediaQualityInterface s : qualityTags) {
            if (s == tag) {
                return true;
            }
        }
        return false;
    }

    public MediaQualityInterface[] getQualityTags() {
        return qualityTags;
    }

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio) {

        this.itag = itag;
        this.codecAudio = codecTagAudio;
        this.qualityAudio = qualityTagAudio;
        this.codecVideo = codecTagVideo;
        this.qualityVideo = qualityTagVideo;
    }

    // private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio,
    // MediaQualityTags... tags) {
    // this.itag = itag;
    // this.qualityTags = tags;
    // this.codecAudio = codecTagAudio;
    // this.qualityAudio = qualityTagAudio;
    // this.codecVideo = codecTagVideo;
    // this.qualityVideo = qualityTagVideo;
    //
    // }

    public String getCodecAudio() {
        return codecAudio;
    }

    public String getCodecVideo() {
        return codecVideo;
    }

    public String getDisplayName() {
        return null;
    }

    public String getQualityExtension() {
        return null;
    }

    public int getITAG() {
        return this.itag;
    }

    public String getQualityAudio() {
        return qualityAudio;
    }

    public double getQualityRating() {
        if (qualityRating >= 0) {
            return qualityRating;
        }
        double r = 0d;
        for (MediaQualityInterface t : qualityTags) {
            r += t.getRating();
        }
        qualityRating = r;

        return r;
    }

    public String getQualityVideo() {
        return qualityVideo;
    }

    public String getCustomName(Object caller) {
        return null;
    }

    public String getCustomQualityExtension(Object caller) {
        return null;
    }

}