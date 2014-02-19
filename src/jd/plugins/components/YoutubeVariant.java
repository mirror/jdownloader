package jd.plugins.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public enum YoutubeVariant implements YoutubeVariantInterface {
    AAC_128(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_128();
        }
    },

    AAC_256(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_256();
        }
    },
    AAC_48(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_48();
        }
    },
    DEMUX_AAC_128_360P_3D_V4("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_128();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_128 - 0.00000360;
        }
    },
    DEMUX_AAC_128_360P_V4("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_128();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_128 - 0.00000360;
        }
    },
    DEMUX_AAC_128_720P_3D_V1("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_128();
        }

        @Override
        public double getQualityRating() {
            // -0,0001 because its better to load the 360p to demux
            return YoutubeITAG.AAC_128 - 0.00000720;
        }
    },

    DEMUX_AAC_128_720P_V1("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_128();
        }

        @Override
        public double getQualityRating() {
            // -0,0001 because its better to load the 360p to demux
            return YoutubeITAG.AAC_128 - 0.00000720;
        }
    },
    DEMUX_AAC_152_720P_3D_V3("AAC_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_152();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_152();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_152 - 0.00000720;
        }
    },

    DEMUX_AAC_152_720P_V3("AAC_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_152();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_152();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_152 - 0.00000720;
        }
    },

    DEMUX_AAC_192_720P_3D_V4("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_192();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_192();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_192 - 0.00000720;
        }
    },
    DEMUX_AAC_192_720P_V4("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_192();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_192();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_192 - 0.00000720;
        }
    },

    DEMUX_AAC_96_360P_3D_V1("AAC_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_96();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_96();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_96 - 0.00000360;
        }
    },
    DEMUX_AAC_96_360P_V1("AAC_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMp4ToAACAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_AAC_96();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_AAC_96();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_96 - 0.00000360;
        }
    },
    DEMUX_M4A_128_360P_3D_V4("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_128 + BETTER_BECAUSE_M4A - 0.00000360;
        }
    },
    DEMUX_M4A_128_360P_V4("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_128 + BETTER_BECAUSE_M4A - 0.00000360;
        }
    },

    DEMUX_M4A_128_720P_3D_V1("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }

        @Override
        public double getQualityRating() {
            // -0.0001 because loading DEMUX_M4A_128_360P_V4 would be better
            return YoutubeITAG.AAC_128 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },
    DEMUX_M4A_128_720P_V1("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }

        @Override
        public double getQualityRating() {
            // -0.0001 because loading DEMUX_M4A_128_360P_V4 would be better
            return YoutubeITAG.AAC_128 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },
    DEMUX_M4A_152_720P_3D_V3("M4A_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_152();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_152();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_152 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },

    DEMUX_M4A_152_720P_V3("M4A_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_152();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_152();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_152 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },
    DEMUX_M4A_192_720P_3D_V4("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_192();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_192();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_192 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },
    DEMUX_M4A_192_720P_V4("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_192();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_192();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_192 + BETTER_BECAUSE_M4A - 0.00000720;
        }
    },

    DEMUX_M4A_96_360P_V1("M4A_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_96();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_96();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_96 + BETTER_BECAUSE_M4A - 0.00000360;
        }
    },
    DEMUX_M4A_96_360P_3D_V1("M4A_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMp4ToM4aAudio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_96();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_96();
        }

        @Override
        public double getQualityRating() {
            return YoutubeITAG.AAC_96 + BETTER_BECAUSE_M4A - 0.00000360;
        }
    },
    FLV_270_HIGH(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_FLV_270_HIGH();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_FLV_270_HIGH();
        }
    },
    FLV_240_LOW(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_FLV_240_LOW();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_FLV_240_LOW();
        }
    },

    FLV_360(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_FLV_360();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_FLV_360();
        }
    },
    FLV_480(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_FLV_480();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_FLV_480();
        }
    },
    IMAGE_HQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_HQ, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_IMAGE_HQ();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_IMAGE_HQ();
        }
    },

    IMAGE_LQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_LQ, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_IMAGE_LQ();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_IMAGE_LQ();
        }
    },

    IMAGE_MAX(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MAX, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_IMAGE_MAX();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_IMAGE_MAX();
        }
    },
    IMAGE_MQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MQ, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_IMAGE_MQ();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_IMAGE_MQ();
        }
    },
    M4A_128(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + BETTER_BECAUSE_M4A;
        }
    },
    M4A_256(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_256();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + BETTER_BECAUSE_M4A;
        }
    },
    M4A_48(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_48();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + BETTER_BECAUSE_M4A;
        }
    },

    MP3_1("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, jd.plugins.components.YoutubeFlvToMp3Audio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP3_1();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP3_1();
        }

        @Override
        public double getQualityRating() {
            // slightly higher rating as MP3_2. audio quality is the same, but total size is less
            return YoutubeITAG.MP3_64 + 0.0001;
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

    MP3_2("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, jd.plugins.components.YoutubeFlvToMp3Audio.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP3_2();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP3_2();
        }

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
    MP4_1080("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_1080();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_1080();
        }
    },
    MP4_360("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_360();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_360();
        }
    },
    MP4_360_AAC96("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_360();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_360();
        }
    },
    MP4_3D_240(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_3D_240();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_3D_240();
        }
    },

    MP4_3D_360(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_3D_360();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_3D_360();
        }
    },
    MP4_3D_1080(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_180P_H264_AUDIO_AAC_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_3D_1080();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_3D_1080();
        }
    },
    MP4_3D_720(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_3D_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_3D_720();
        }
    },

    MP4_720("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_720();
        }
    },
    MP4_720_128AAC("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_720();
        }
    },
    MP4_720_152AAC("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_720();
        }
    },
    MP4_DASH_1080_AAC128("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1080_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1080_AAC128();
        }
    },

    MP4_DASH_1080_AAC256("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1080_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1080_AAC256();
        }
    },
    MP4_DASH_1080_AAC48("MP4_1080", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1080_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1080_AAC48();
        }
    },

    MP4_DASH_144_AAC128("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_144_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_144_AAC128();
        }
    },
    MP4_DASH_144_AAC256("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_144_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_144_AAC256();
        }
    },
    MP4_DASH_144_AAC48("MP4_144", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_144_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_144_AAC48();
        }
    },
    MP4_DASH_1440_AAC128("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1440_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1440_AAC128();
        }
    },
    MP4_DASH_1440_AAC256("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1440_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1440_AAC256();
        }
    },
    MP4_DASH_1440_AAC48("MP4_1440", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_1440_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_1440_AAC48();
        }
    },
    MP4_DASH_240_AAC128("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_240_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_240_AAC128();
        }
    },
    MP4_DASH_240_AAC256("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_240_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_240_AAC256();
        }
    },

    MP4_DASH_240_AAC48("MP4_240", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_240_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_240_AAC48();
        }
    },
    MP4_DASH_360_AAC128("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_360_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_360_AAC128();
        }
    },
    MP4_DASH_360_AAC256("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_360_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_360_AAC256();
        }
    },
    MP4_DASH_360_AAC48("MP4_360", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_360_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_360_AAC48();
        }
    },
    MP4_DASH_480_AAC128("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_480_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_480_AAC128();
        }
    },
    MP4_DASH_480_AAC256("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_480_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_480_AAC256();
        }
    },
    MP4_DASH_480_AAC48("MP4_480", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_480_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_480_AAC48();
        }
    },
    MP4_DASH_720_AAC128("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_720_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_720_AAC128();
        }
    },

    MP4_DASH_720_AAC256("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_720_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_720_AAC256();
        }
    },
    MP4_DASH_720_AAC48("MP4_720", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_720_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_720_AAC48();
        }
    },
    MP4_DASH_ORIGINAL_AAC128("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_ORIGINAL_AAC128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_ORIGINAL_AAC128();
        }
    },
    MP4_DASH_ORIGINAL_AAC256("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_ORIGINAL_AAC256();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_ORIGINAL_AAC256();
        }
    },
    MP4_DASH_ORIGINAL_AAC48("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_DASH_ORIGINAL_AAC48();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_DASH_ORIGINAL_AAC48();
        }
    },
    MP4_ORIGINAL("MP4_ORIGINAL", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_ORIGINAL();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_ORIGINAL();
        }
    },
    SUBTITLES(null, YoutubeVariantInterface.VariantGroup.SUBTITLES, YoutubeVariantInterface.DownloadType.SUBTITLES, "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSubtitleNamer.getInstance(), YoutubeSRTConverter.getInstance()) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_SUBTITLES();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_SUBTITLES();
        }

    },
    THREEGP_144(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_THREEGP_144();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_THREEGP_144();
        }
    },
    THREEGP_240_HIGH(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_THREEGP_240_HIGH();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_THREEGP_240_HIGH();
        }
    },

    THREEGP_240_LOW(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_THREEGP_240_LOW();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_THREEGP_240_LOW();
        }
    },

    WEBM_1080(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_1080();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_1080();
        }
    },
    WEBM_360(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_360();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_360();
        }
    },
    WEBM_3D_360_128(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_3D_360_128();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_3D_360_128();
        }
    },
    WEBM_3D_360_192(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_3D_360_192();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_3D_360_192();
        }
    },

    WEBM_3D_720(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_3D_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_3D_720();
        }
    },
    WEBM_480(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_480();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_480();
        }
    },

    WEBM_720(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_WEBM_720();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_WEBM_720();
        }
    };

    private static final double                        BETTER_BECAUSE_M4A = 0.00001000;

    private YoutubeConverter                           converter;
    final private String                               fileExtension;
    private YoutubeFilenameModifier                    filenameModifier;
    final private YoutubeVariantInterface.VariantGroup group;
    final private String                               id;

    final private YoutubeITAG                          iTagAudio;

    final private YoutubeITAG                          iTagData;
    final private YoutubeITAG                          iTagVideo;
    private final double                               qualityRating;
    final private YoutubeVariantInterface.DownloadType type;

    private YoutubeVariant(final String id, final YoutubeVariantInterface.VariantGroup group, final YoutubeVariantInterface.DownloadType type, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeFilenameModifier filenameModifier, YoutubeConverter converter) {
        this.group = group;
        this.id = id;

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

    public void convert(DownloadLink downloadLink) throws Exception {
        if (converter != null) converter.run(downloadLink);
    }

    /**
     * returns true if this variant requires a video tool like ffmpge for muxing, demuxing or container converting
     * 
     * @return
     */
    public boolean isVideoToolRequired() {
        if (iTagVideo != null && iTagAudio != null) return true;
        if (iTagVideo != null && iTagVideo.name().contains("DASH")) return true;
        if (iTagAudio != null && iTagAudio.name().contains("DASH")) return true;
        if (converter != null) {
            if (converter instanceof YoutubeMp4ToAACAudio) return true;
            if (converter instanceof YoutubeMp4ToM4aAudio) return true;
            if (converter instanceof YoutubeFlvToMp3Audio) return true;
            if (converter instanceof YoutubeExternConverter) return true;

        }
        return false;
    }

    @Override
    public String getExtendedName() {
        switch (getGroup()) {
        case AUDIO:
            if (getiTagVideo() != null) {
                return getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI._.lit_audio() + " [" + getAudioQuality() + "-" + getAudioCodec() + "-DEMUX]";

            } else {
                return getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI._.lit_audio() + " [" + getAudioQuality() + "-" + getAudioCodec() + "]";

            }

        case IMAGE:
            return getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI._.lit_image() + " [" + getResolution() + "]";
        case SUBTITLES:
            return getName();
        case VIDEO:
            return getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI._.lit_video() + "[" + getResolution() + "-" + getVideoCodec() + "_" + getAudioQuality() + "-" + getAudioCodec() + "]";

        case VIDEO_3D:
            return getFileExtension().toUpperCase(Locale.ENGLISH) + "-" + _GUI._.lit_3d_video() + "[" + getResolution() + "-" + getVideoCodec() + "_" + getAudioQuality() + "-" + getAudioCodec() + "]";

        }
        return null;
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

    public YoutubeITAG getiTagAudio() {
        return this.iTagAudio;
    }

    public YoutubeITAG getiTagData() {
        return iTagData;
    }

    public YoutubeITAG getiTagVideo() {
        return this.iTagVideo;
    }

    public String getMediaTypeID() {
        return getGroup().name();
    }

    public abstract String getName();

    public abstract String getQualityExtension();

    public double getQualityRating() {
        return this.qualityRating;
    }

    public YoutubeVariantInterface.DownloadType getType() {
        return this.type;
    }

    public String getTypeId() {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) return name();
        if (this.id == null) { return this.name(); }
        return this.id;
    }

    public String getUniqueId() {
        return name();
    }

    @Override
    public boolean hasConverer(DownloadLink downloadLink) {
        return converter != null;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();

        return ret;
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (filenameModifier != null) return filenameModifier.run(formattedFilename, link);
        return formattedFilename;
    }

    public String getAudioCodec() {
        if (getiTagAudio() != null) { return getiTagAudio().getCodecAudio(); }
        if (getiTagVideo() != null) { return getiTagVideo().getCodecAudio(); }
        return null;
    }

    public String getAudioQuality() {
        if (getiTagAudio() != null) { return getiTagAudio().getQualityAudio(); }
        if (getiTagVideo() != null) { return getiTagVideo().getQualityAudio(); }
        return null;
    }

    public String getResolution() {
        String ret = null;
        if (getiTagData() != null) {

            ret = getiTagData().getQualityVideo();
        }
        if (getiTagVideo() != null) {
            ret = getiTagVideo().getQualityVideo();
        }
        return ret;
    }

    public String getVideoCodec() {
        if (getiTagData() != null) { return getiTagData().getCodecVideo(); }
        if (getiTagVideo() != null) { return getiTagVideo().getCodecVideo(); }
        return null;
    }

}