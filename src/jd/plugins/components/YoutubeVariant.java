package jd.plugins.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public enum YoutubeVariant implements YoutubeVariantInterface {
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
    M4A_128(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_128();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + 0.001;
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_128();
        }
    },

    M4A_256(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_256();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + 0.001;
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_256();
        }
    },
    M4A_48(null, YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_M4A_48();
        }

        @Override
        public double getQualityRating() {
            return super.getQualityRating() + 0.001;
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_M4A_48();
        }
    },

    FLV_240_HIGH(null, YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_FLV_240_HIGH();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_FLV_240_HIGH();
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

    MP3_1("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            // slightly higher rating as MP3_2. audio quality is the same, but total size is less
            return YoutubeITAG.MP3_64 + 0.001;
        }

        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP3_1();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP3_1();
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
    MP3_2("MP3_64KBit", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            return YoutubeITAG.MP3_64;
        }

        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP3_2();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP3_2();
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
    MP4_3D_520(null, YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_520P_H264_AUDIO_AAC_3D, null, null, null, null) {
        @Override
        public String getName() {
            return _GUI._.YoutubeVariant_name_MP4_3D_520();
        }

        @Override
        public String getQualityExtension() {
            return _GUI._.YoutubeVariant_filenametag_MP4_3D_520();
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

    final private String                               fileExtension;
    final private YoutubeVariantInterface.VariantGroup group;
    final private String                               id;
    final private YoutubeITAG                          iTagAudio;
    final private YoutubeITAG                          iTagVideo;

    private final double                               qualityRating;

    final private YoutubeVariantInterface.DownloadType type;
    final private YoutubeITAG                          iTagData;
    private YoutubeConverter                           converter;
    private YoutubeFilenameModifier                    filenameModifier;

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

    @Override
    public boolean hasConverer(DownloadLink downloadLink) {
        return converter != null;
    }

    @Override
    public String getExtendedName() {
        return getName() + " (" + name() + ")";
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

    public abstract String getName();

    public abstract String getQualityExtension();

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