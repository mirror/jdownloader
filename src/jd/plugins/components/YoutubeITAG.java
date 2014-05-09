package jd.plugins.components;

import java.util.Date;

public enum YoutubeITAG {

    // fake id
    SUBTITLE(10002, "srt", null, null, null, 0.1),
    // fake id
    IMAGE_MAX(10001, "jpg", "1400x1080", null, null, 0.4),
    // fake id
    IMAGE_HQ(10002, "jpg", "480x360", null, null, 0.3),
    // fake id
    IMAGE_MQ(10003, "jpg", "320x180", null, null, 0.2),
    // fake id
    IMAGE_LQ(10004, "jpg", "120x90", null, null, 0.1),
    DASH_AUDIO_128K_AAC(140, null, null, "AAC", "128kbit", YoutubeITAG.AAC_128),
    DASH_AUDIO_128K_WEBM(171, null, null, "Vorbis", "128kbit", YoutubeITAG.VORBIS_128),
    DASH_AUDIO_192K_WEBM(172, null, null, "Vorbis", "192kbit", YoutubeITAG.VORBIS_192),
    DASH_AUDIO_256K_AAC(141, null, null, "AAC", "256kbit", YoutubeITAG.AAC_256),
    DASH_AUDIO_48K_AAC(139, null, null, "AAC", "48kbit", YoutubeITAG.AAC_48),
    DASH_VIDEO_1080P_H264(137, "H264", "1080p", null, null, 1080.4),
    // http://www.youtube.com/watch?v=gBabKoHSErI
    DASH_VIDEO_1440P_H264(264, "H264", "1440p", null, null, 1440.4),
    DASH_VIDEO_144P_H264(160, "H264", "144p", null, null, 144.4),

    DASH_VIDEO_240P_H264(133, "H264", "240p", null, null, 240.4),
    DASH_VIDEO_360P_H264(134, "H264", "360p", null, null, 360.4),
    DASH_VIDEO_480P_H264(135, "H264", "480p", null, null, 480.4),
    DASH_VIDEO_720P_H264(136, "H264", "720p", null, null, 720.4),
    DASH_VIDEO_ORIGINAL_H264(138, "H264", "Original", null, null, 2160.4),

    DASH_WEBM_VIDEO_1080P_VP9(248, "VP9", "1080p", null, null, 1080.3),
    DASH_WEBM_VIDEO_720P_VP9(247, "VP9", "720p", null, null, 720.3),
    DASH_WEBM_VIDEO_480P_VP9_3(246, "VP9", "480p", null, null, 482.3),
    DASH_WEBM_VIDEO_480P_VP9_2(245, "VP9", "480p", null, null, 481.3),
    DASH_WEBM_VIDEO_480P_VP9(244, "VP9", "480p", null, null, 480.3),
    DASH_WEBM_VIDEO_360P_VP9(243, "VP9", "360p", null, null, 360.3),
    DASH_WEBM_VIDEO_240P_VP9(242, "VP9", "240p", null, null, 240.3),

    FLV_VIDEO_360P_H264_AUDIO_AAC(34, "H264", "360p", "AAC", "128kbit", 360.1d),
    FLV_VIDEO_480P_H264_AUDIO_AAC(35, "H264", "480p", "AAC", "128kbit", 480.1d),
    FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3(6, "Sorenson H.263", "270p", "MP3", "64kbit", 270.11d + YoutubeITAG.MP3_64),

    FLV_VIDEO_LOW_240P_H263_AUDIO_MP3(5, "Sorenson H.263", "240p", "MP3", "64kbit", 240.10d + YoutubeITAG.MP3_64),

    // 192 kbits aac
    MP4_VIDEO_1080P_H264_AUDIO_AAC(37, "H264", "1080p", "AAC", "192kbit", 1080.4 + YoutubeITAG.AAC_192),
    // not sure
    MP4_VIDEO_240P_H264_AUDIO_AAC_3D(83, "H264", "240p", "AAC", "96kbit", 240.4 + YoutubeITAG.AAC_96),
    MP4_VIDEO_360P_H264_AUDIO_AAC(18, "H264", "360p", "AAC", "128kbit", 360.4 + YoutubeITAG.AAC_128),
    MP4_VIDEO_360P_H264_AUDIO_AAC_V1(18, "H264", "360p", "AAC", "96kbit", 360.4 + YoutubeITAG.AAC_96),

    MP4_VIDEO_360P_H264_AUDIO_AAC_3D(82, "H264", "360p", "AAC", "128kbit", 360.4 + YoutubeITAG.AAC_128),
    MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1(82, "H264", "360p", "AAC", "96kbit", 360.4 + YoutubeITAG.AAC_96),
    MP4_VIDEO_180P_H264_AUDIO_AAC_3D(85, "H264", "1080p", "AAC", "192kbit", 1080.4 + YoutubeITAG.AAC_192),
    // 192 kbits aac
    MP4_VIDEO_720P_H264_AUDIO_AAC(22, "H264", "720p", "AAC", "192kbit", 720.4 + YoutubeITAG.AAC_192),
    MP4_VIDEO_720P_H264_AUDIO_AAC_V1(22, "H264", "720p", "AAC", "128kbit", 720.4 + YoutubeITAG.AAC_128),
    MP4_VIDEO_720P_H264_AUDIO_AAC_V3(22, "H264", "720p", "AAC", "152kbit", 720.4 + YoutubeITAG.AAC_152),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D(84, "H264", "720p", "AAC", "192kbit", 720.4 + YoutubeITAG.AAC_192),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3(84, "H264", "720p", "AAC", "152kbit", 720.4 + YoutubeITAG.AAC_152),
    MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1(84, "H264", "720p", "AAC", "128kbit", 720.4 + YoutubeITAG.AAC_128),
    // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
    MP4_VIDEO_AUDIO_ORIGINAL(38, "H264", "Original", "AAC", "192kbit", 2160.4 + YoutubeITAG.AAC_192),
    // very different audio bitrates!!!
    THREEGP_VIDEO_144P_H264_AUDIO_AAC(17, "MPEG-4 Visual", "144p", "AAC", "24kbit", 144.0 + YoutubeITAG.AAC32_ESTIMATE),
    // LIve stream?
    // I tested several streams, and this itag contains 180p and 32kbit audio
    THREEGP_VIDEO_240P_H263_AUDIO_AAC(132, "MPEG-4 Visual", "180p", "AAC", "32kbit", 180.0 + YoutubeITAG.AAC32_ESTIMATE),

    THREEGP_VIDEO_240P_H264_AUDIO_AAC(36, "MPEG-4 Visual", "180p", "AAC", "32kbit", 180.01 + YoutubeITAG.AAC32_ESTIMATE),

    // not sure - did not find testvideos
    WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS(46, "VP8", "1080p", "Vorbis", "192kbit", 1080.3 + YoutubeITAG.VORBIS_192),

    WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D(100, "VP8", "360p", "Vorbis", "128kbit", 360.3 + YoutubeITAG.VORBIS_128),
    WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D(101, "VP8", "360p", "Vorbis", "192kbit", 360.3 + YoutubeITAG.VORBIS_192),

    WEBM_VIDEO_360P_VP8_AUDIO_VORBIS(43, "VP8", "360p", "Vorbis", "128kbit", 360.3 + YoutubeITAG.VORBIS_128),
    // not sure - did not find testvideos
    WEBM_VIDEO_480P_VP8_AUDIO_VORBIS(44, "VP8", "480p", "Vorbis", "128kbit", 480.3 + YoutubeITAG.VORBIS_128),
    WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D(102, "VP8", "720p", "Vorbis", "192kbit", 720.3 + YoutubeITAG.VORBIS_192),
    // not sure - did not find testvideos
    WEBM_VIDEO_720P_VP8_AUDIO_VORBIS(45, "VP8", "720p", "Vorbis", "192kbit", 720.3 + YoutubeITAG.VORBIS_192);
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
        // 1080p 128 kbps AAC
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

    }

    public static YoutubeITAG get(int itag) {
        for (final YoutubeITAG tag : YoutubeITAG.values()) {
            if (tag.getITAG() == itag) {

            return tag;

            }
        }
        return null;
    }

    public static YoutubeITAG get(final int itag, long uploadDate) {
        YoutubeITAGVersion version = YoutubeITAGVersion.V4;
        ;
        for (YoutubeITAGVersion v : YoutubeITAGVersion.values()) {
            if (v.matches(uploadDate)) {
                version = v;
                break;
            }
        }
        switch (itag) {
        case 18:
            switch (version) {
            case V1:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_V1;
            default:
                return MP4_VIDEO_360P_H264_AUDIO_AAC;
            }
        case 22:
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
            switch (version) {
            case V1:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1;
            default:
                return MP4_VIDEO_360P_H264_AUDIO_AAC_3D;
            }
        case 84:
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

        return get(itag);
    }

    private final int          itag;

    double                     qualityRating;

    private String             codecAudio;

    private String             qualityAudio;

    private String             codecVideo;

    private String             qualityVideo;

    public static final double VORBIS_96       = 0.0963;

    public static final double VORBIS_192      = 0.1923;

    public static final double VORBIS_128      = 0.1283;

    // mp3 64 bit is lower than aac48bit
    public static final double MP3_64          = 0.0442;

    public static final double AAC32_ESTIMATE  = 0.0324;

    public static final double AAC_64          = 0.0644;

    public static final double AAC_48_ESTIMATE = 0.0474;

    public static final double AAC_48          = 0.0484;

    public static final double AAC_256         = 0.2564;

    public static final double AAC_192         = 0.1924;

    public static final double AAC_128         = 0.1284;
    public static final double AAC_152         = 0.1524;
    public static final double AAC_96          = 0.0964;

    private YoutubeITAG(final int itag, String codecTagVideo, String qualityTagVideo, String codecTagAudio, String qualityTagAudio, final double quality) {
        this.itag = itag;
        this.qualityRating = quality;
        this.codecAudio = codecTagAudio;
        this.qualityAudio = qualityTagAudio;
        this.codecVideo = codecTagVideo;
        this.qualityVideo = qualityTagVideo;

    }

    public String getCodecAudio() {
        return codecAudio;
    }

    public String getQualityAudio() {
        return qualityAudio;
    }

    public String getCodecVideo() {
        return codecVideo;
    }

    public String getQualityVideo() {
        return qualityVideo;
    }

    public int getItag() {
        return itag;
    }

    public double getQualityRating() {
        return qualityRating;
    }

    public int getITAG() {
        return this.itag;
    }

}