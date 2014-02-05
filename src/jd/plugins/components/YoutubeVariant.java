package jd.plugins.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

public enum YoutubeVariant implements YoutubeVariantInterface {
    SUBTITLES(null, YoutubeVariantInterface.VariantGroup.SUBTITLES, YoutubeVariantInterface.DownloadType.SUBTITLES, "SubRip Subtitle File", "Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSubtitleNamer.getInstance(), YoutubeSRTConverter.getInstance()),

    IMAGE_MAX(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "Best Quality Image", "BQ", "jpg", null, null, YoutubeITAG.IMAGE_MAX, null, null),
    IMAGE_HQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "High Quality Image", "HQ", "jpg", null, null, YoutubeITAG.IMAGE_HQ, null, null),
    IMAGE_MQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "Medium Quality Image", "MQ", "jpg", null, null, YoutubeITAG.IMAGE_MQ, null, null),
    IMAGE_LQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "Low Quality Image", "LQ", "jpg", null, null, YoutubeITAG.IMAGE_LQ, null, null),
    AAC_128(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "128kbit/s AAC-Audio", "128kbit", "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),

    AAC_256(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "256kbit/s AAC-Audio", "256kbit", "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    AAC_48(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "48kbit/s AAC-Audio", "48kbit", "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),

    FLV_240_HIGH(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "240p FLV-Video(high)", "240p[HQ]", "flv", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_240_LOW(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "240p FLV-Video(low)", "240p[LQ]", "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_360(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "360p FLV-Video", "360p", "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    FLV_480(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "480p FLV-Video", "480p", "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),

    MP3_1("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            // slightly higher rating as MP3_2. audio quality is the same, but total size is less
            return YoutubeITAG.MP3_64 + 0.001;
        }

        @Override
        public List<File> listProcessFiles(DownloadLink link) {
            List<File> ret = super.listProcessFiles(link);
            File mp3 = new File(link.getFileOutput(true, true));

            ret.add(new File(mp3 + ".avi"));
            ret.add(new File(mp3 + ".mp3"));
            ret.add(new File(mp3 + ".tmp"));
            ret.add(new File(mp3 + ".tmp.part"));
            return ret;
        }
    },
    MP3_2("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            return YoutubeITAG.MP3_64;
        }

        @Override
        public List<File> listProcessFiles(DownloadLink link) {
            List<File> ret = super.listProcessFiles(link);
            File mp3 = new File(link.getFileOutput(true, true));

            ret.add(new File(mp3 + ".avi"));
            ret.add(new File(mp3 + ".mp3"));
            ret.add(new File(mp3 + ".tmp"));
            ret.add(new File(mp3 + ".tmp.part"));
            return ret;
        }

    },
    MP4_1080("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "1080p MP4-Video", "1080p", "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, null),

    MP4_360("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "360p MP4-Video", "360p", "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    MP4_3D_240(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "240p MP4-3D-Video", "240p 3D", "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_3D_360(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "360p MP4-3D-Video", "360p 3D", "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_3D_520(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "520p MP4-3D-Video", "520p 3D", "mp4", YoutubeITAG.MP4_VIDEO_520P_H264_AUDIO_AAC_3D, null, null, null, null),

    MP4_3D_720(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "720p MP4-3D-Video", "720p 3D", "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_720("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "720p MP4-Video", "720p", "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, null),
    MP4_DASH_1080_AAC128("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1080p MP4-Video", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_1080_AAC256("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1080p MP4-Video", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_1080_AAC48("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1080p MP4-Video", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_144_AAC128("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "144p MP4-Video", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_144_AAC256("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "144p MP4-Video", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_144_AAC48("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "144p MP4-Video", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_1440_AAC128("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1440p MP4-Video", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_1440_AAC256("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1440p MP4-Video", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_1440_AAC48("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "1440p MP4-Video", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),

    MP4_DASH_240_AAC128("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "240p MP4-Video", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_240_AAC256("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "240p MP4-Video", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_240_AAC48("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "240p MP4-Video", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_360_AAC128("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "360p MP4-Video", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_360_AAC256("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "360p MP4-Video", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_360_AAC48("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "360p MP4-Video", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_480_AAC128("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "480p MP4-Video", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_480_AAC256("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "480p MP4-Video", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_480_AAC48("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "480p MP4-Video", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_720_AAC128("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "720p MP4-Video", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),

    MP4_DASH_720_AAC256("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "720p MP4-Video", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_720_AAC48("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "720p MP4-Video", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC128("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "2160p MP4-Video", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC256("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "2160p MP4-Video", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC48("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "2160p MP4-Video", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_ORIGINAL("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "2160p MP4-Video", "2160p", "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, null),
    THREEGP_144(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "144p 3GP Video", "144p", "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_240_HIGH(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "240p 3GP Video(high)", "240p[HQ]", "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),

    THREEGP_240_LOW(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "240p 3GP Video(low)", "240p[LQ]", "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    WEBM_1080(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "1080p WebM-Video", "1080p", "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_360(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "360p WebM-Video", "360p", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_3D_360_128(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "360p WebM-3D-Video(128Kbit/s Audio)", "360p 3D [128kbit Audio]", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null, null),
    WEBM_3D_360_192(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "360p WebM-3D-Video(192Kbit/s Audio)", "360p 3D [192kbit Audio]", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_3D_720(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "720p WebM-3D-Video", "720p 3D", "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_480(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "480p WebM-Video", "480p", "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_720(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "720p WebM-Video", "720p", "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null);

    final private String                               fileExtension;
    final private YoutubeVariantInterface.VariantGroup group;
    final private String                               id;
    final private YoutubeITAG                          iTagAudio;
    final private YoutubeITAG                          iTagVideo;
    final private String                               name;
    final private String                               qualityExtension;

    private final double                               qualityRating;

    final private YoutubeVariantInterface.DownloadType type;
    final private YoutubeITAG                          iTagData;
    private YoutubeConverter                           converter;
    private YoutubeFilenameModifier                    filenameModifier;

    private YoutubeVariant(final String id, final YoutubeVariantInterface.VariantGroup group, final YoutubeVariantInterface.DownloadType type, final String name, final String qualityExtension, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeFilenameModifier filenameModifier, YoutubeConverter converter) {
        this.group = group;
        this.id = id;
        this.name = name;
        this.qualityExtension = qualityExtension;
        this.fileExtension = fileExtension;
        if (type == null) throw new NullPointerException();
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.qualityRating = 0d + (video != null ? video.qualityRating : 0) + (audio != null ? audio.qualityRating : 0) + (data != null ? data.qualityRating : 0);
        this.iTagData = data;
        this.converter = converter;
        this.filenameModifier = filenameModifier;
    }

    @Override
    public boolean hasConverer(DownloadLink downloadLink) {
        return converter != null;
    }

    public YoutubeITAG getiTagData() {
        return iTagData;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public YoutubeVariantInterface.VariantGroup getGroup() {
        return this.group;
    }

    public Icon getIcon() {
        return null;
    }

    public String getTypeId() {
        if (this.id == null) { return this.name(); }
        return this.id;
    }

    public YoutubeITAG getiTagAudio() {
        return this.iTagAudio;
    }

    public YoutubeITAG getiTagVideo() {
        return this.iTagVideo;
    }

    public String getName() {
        return this.name;
    }

    public String getQualityExtension() {
        return this.qualityExtension;
    }

    public double getQualityRating() {
        return this.qualityRating;
    }

    public YoutubeVariantInterface.DownloadType getType() {
        return this.type;
    }

    public void convert(DownloadLink downloadLink) {
        if (converter != null) converter.run(downloadLink);
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (filenameModifier != null) return filenameModifier.run(formattedFilename, link);
        return formattedFilename;
    }

    public String getUniqueId() {
        return name();
    }

    public String getMediaTypeID() {
        return getGroup().name();
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();

        return ret;
    }

}