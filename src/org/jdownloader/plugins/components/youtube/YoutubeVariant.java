package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public enum YoutubeVariant implements YoutubeVariantInterface {
    IMAGE_HQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_HQ, null, null),
    IMAGE_LQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_LQ, null, null),
    IMAGE_MAX(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MAX, null, null),
    IMAGE_MQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MQ, null, null),
    SUBTITLES(null, YoutubeVariantInterface.VariantGroup.SUBTITLES, YoutubeVariantInterface.DownloadType.SUBTITLES, "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSubtitleNamer.getInstance(), YoutubeSRTConverter.getInstance()),
    DESCRIPTION(null, YoutubeVariantInterface.VariantGroup.DESCRIPTION, YoutubeVariantInterface.DownloadType.DESCRIPTION, "txt", null, null, YoutubeITAG.DESCRIPTION, null, null),
    FLV_H264_360P_30FPS_AAC_128KBIT("FLV_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    FLV_H264_360P_30FPS_AAC_128KBIT_3D("FLV_360P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_FLV_H264_360P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeConverterFLVToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_FLV_H264_360P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeConverterFLVToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    FLV_H264_480P_30FPS_AAC_128KBIT("FLV_480P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),
    FLV_H264_480P_30FPS_AAC_128KBIT_3D("FLV_480P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_FLV_H264_480P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, YoutubeConverterFLVToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_FLV_H264_480P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, YoutubeConverterFLVToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    FLV_H263_270P_30FPS_MP3_64KBIT("FLV_270P_30FPS_MP3", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, null),
    FLV_H263_270P_30FPS_MP3_64KBIT_3D("FLV_270P_30FPS_MP3_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, null),
    DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT("MP3_64KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, YoutubeConverterFLVToMP3Audio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.MP3.getRating() + AudioBitrate.KBIT_64.getRating() - 0.00000270;
        }
    },
    FLV_H263_240P_30FPS_MP3_64KBIT("FLV_240P_30FPS_MP3", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_H263_240P_30FPS_MP3_64KBIT_3D("FLV_240P_30FPS_MP3_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    DEMUX_MP3_FLV_H263_240P_30FPS_MP3_64KBIT("MP3_64KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, YoutubeConverterFLVToMP3Audio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.MP3.getRating() + AudioBitrate.KBIT_64.getRating() - 0.00000240;
        }
    },
    THREEGP_H263_144P_15FPS_AMR_12KBIT("THREEGP_144P_15FPS_AMR", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT, null, null, null, null),
    THREEGP_H263_144P_15FPS_AMR_12KBIT_3D("THREEGP_144P_15FPS_AMR_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT, null, null, null, null),
    MP4_H264_1080P_30FPS_AAC_192KBIT("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT("AAC_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT("M4A_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    MP4_H264_1080P_30FPS_AAC_192KBIT_3D("MP4_1080P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT_2("AAC_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT_2("M4A_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    MP4_H264_240P_30FPS_AAC_96KBIT_3D("MP4_240P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_240P_30FPS_AAC_96KBIT("AAC_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_MP4_H264_240P_30FPS_AAC_96KBIT("M4A_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000240;
        }
    },
    MP4_H264_360P_30FPS_AAC_128KBIT("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    MP4_H264_360P_30FPS_AAC_128KBIT_3D("MP4_360P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT_2("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT_2("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    MP4_H264_360P_30FPS_AAC_96KBIT_3D("MP4_360P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT("AAC_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT("M4A_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    MP4_H264_360P_30FPS_AAC_96KBIT("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT_2("AAC_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT_2("M4A_96KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    MP4_H264_720P_30FPS_AAC_192KBIT("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT("AAC_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT("M4A_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_192KBIT_3D("MP4_720P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT_2("AAC_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT_2("M4A_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_128KBIT_3D("MP4_720P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_152KBIT_3D("MP4_720P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT("AAC_152KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT("M4A_152KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_128KBIT("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT_2("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_152KBIT("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT_2("AAC_152KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT_2("M4A_152KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    MP4_H264_2160P_30FPS_AAC_192KBIT("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, null),
    DEMUX_AAC_MP4_H264_2160P_30FPS_AAC_192KBIT("AAC_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000002160;
        }
    },
    DEMUX_M4A_MP4_H264_2160P_30FPS_AAC_192KBIT("M4A_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000002160;
        }
    },
    THREEGP_H264_144P_30FPS_AAC_31KBIT("THREEGP_144P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_H264_144P_30FPS_AAC_31KBIT_3D("THREEGP_144P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_H263_240P_30FPS_AAC_32KBIT("THREEGP_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    THREEGP_H263_240P_30FPS_AAC_32KBIT_3D("THREEGP_240P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    THREEGP_H264_240P_30FPS_AAC_32KBIT("THREEGP_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_H264_240P_30FPS_AAC_32KBIT_3D("THREEGP_240P_30FPS_AAC_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),
    WEBM_VP8_1080P_30FPS_VORBIS_192KBIT("WEBM_1080P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_1080P_30FPS_VORBIS_192KBIT_3D("WEBM_1080P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_3D("WEBM_360P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_3D("WEBM_360P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_VP8_360P_30FPS_VORBIS_128KBIT("WEBM_360P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_360P_30FPS_VORBIS_128KBIT_3D("WEBM_360P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_480P_30FPS_VORBIS_128KBIT("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_480P_30FPS_VORBIS_128KBIT_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT_3D("WEBM_720P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    MP4_H264_480P_24FPS_AAC_128KBIT("MP4_480P_24FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    MP4_H264_480P_24FPS_AAC_128KBIT_2("MP4_480P_24FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT_2("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT_2("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT("WEBM_720P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT_3D_2("WEBM_720P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null),
    HLS_MP4_H264_72P_6FPS_AAC_24KBIT("MP4_72P_6FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_72P_6FPS_AAC_24KBIT("AAC_24KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_24.getRating() - 0.0000072;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_72P_6FPS_AAC_24KBIT("M4A_24KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_24.getRating() - 0.0000072;
        }
    },
    HLS_MP4_H264_240P_15FPS_AAC_48KBIT("MP4_240P_15FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_15FPS_AAC_48KBIT("AAC_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_240P_15FPS_AAC_48KBIT("M4A_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    HLS_MP4_H264_240P_30FPS_AAC_48KBIT("MP4_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_30FPS_AAC_48KBIT("AAC_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_240P_30FPS_AAC_48KBIT("M4A_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    HLS_MP4_H264_360P_30FPS_AAC_128KBIT("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_360P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_360P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    HLS_MP4_H264_480P_30FPS_AAC_128KBIT("MP4_480P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_480P_30FPS_AAC_128KBIT("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_480P_30FPS_AAC_128KBIT("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT("AAC_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT("M4A_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("AAC_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("M4A_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("AAC_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("M4A_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.000001080;
        }
    },
    MP4_H264_1080P_60FPS_AAC_128KBIT_DASH("MP4_1080P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    AAC_128KBIT_DASH("AAC_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    M4A_AAC_128KBIT_DASH("M4A_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_128KBIT_DASH("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_128KBIT_DASH("MP4_1440P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_128KBIT_DASH("MP4_144P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_128KBIT_DASH("MP4_2160P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_128KBIT_DASH("MP4_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_128KBIT_DASH("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_128KBIT_DASH("MP4_480P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_128KBIT_DASH("MP4_720P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_128KBIT_DASH("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH_2("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_1080P_30FPS_AAC_128KBIT_DASH_2("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_4320P_30FPS_AAC_128KBIT_DASH("MP4_4320P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_128KBIT_DASH("WEBM_1440P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_128KBIT_DASH_3D("WEBM_1440P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    OGG_VORBIS_128KBIT_DASH("OGG_128KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_128KBIT_DASH("WEBM_2160P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_2160P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_128KBIT_DASH("WEBM_2160P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_128KBIT_DASH_3D("WEBM_2160P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_128KBIT_DASH("WEBM_1080P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_1080P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_128KBIT_DASH("WEBM_1080P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_128KBIT_DASH_3D("WEBM_1080P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_128KBIT_DASH("WEBM_1440P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_1440P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_128KBIT_DASH("WEBM_144P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_144P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_128KBIT_DASH("WEBM_2160P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_2160P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_128KBIT_DASH("WEBM_240P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_240P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_DASH("WEBM_360P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_360P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_128KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_128KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_128KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_128KBIT_DASH("WEBM_720P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_720P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_128KBIT_DASH("WEBM_720P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_128KBIT_DASH_3D("WEBM_720P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_192KBIT_DASH("WEBM_1440P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_192KBIT_DASH_3D("WEBM_1440P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    OGG_VORBIS_192KBIT_DASH("OGG_192KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_192KBIT_DASH("WEBM_2160P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_2160P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_192KBIT_DASH("WEBM_2160P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_192KBIT_DASH_3D("WEBM_2160P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_192KBIT_DASH("WEBM_1080P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_1080P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_192KBIT_DASH("WEBM_1080P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_192KBIT_DASH_3D("WEBM_1080P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_192KBIT_DASH("WEBM_1440P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_1440P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_192KBIT_DASH("WEBM_144P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_144P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_192KBIT_DASH("WEBM_2160P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_2160P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_192KBIT_DASH("WEBM_240P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_240P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_DASH("WEBM_360P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_360P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_192KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_192KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_192KBIT_DASH("WEBM_480P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_480P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_192KBIT_DASH("WEBM_720P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_720P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_192KBIT_DASH("WEBM_720P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_192KBIT_DASH_3D("WEBM_720P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    MP4_H264_1080P_60FPS_AAC_256KBIT_DASH("MP4_1080P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    AAC_256KBIT_DASH("AAC_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    M4A_AAC_256KBIT_DASH("M4A_256KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_256.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_256KBIT_DASH("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_256KBIT_DASH("MP4_1440P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_256KBIT_DASH("MP4_144P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_256KBIT_DASH("MP4_2160P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_256KBIT_DASH("MP4_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_256KBIT_DASH("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_256KBIT_DASH("MP4_480P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_256KBIT_DASH("MP4_720P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_256KBIT_DASH("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH_2("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1080P_30FPS_AAC_256KBIT_DASH_2("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_4320P_30FPS_AAC_256KBIT_DASH("MP4_4320P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1080P_60FPS_AAC_48KBIT_DASH("MP4_1080P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    AAC_48KBIT_DASH("AAC_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    M4A_AAC_48KBIT_DASH("M4A_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_48.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_48KBIT_DASH("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_48KBIT_DASH("MP4_1440P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_48KBIT_DASH("MP4_144P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_48KBIT_DASH("MP4_2160P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_48KBIT_DASH("MP4_240P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_48KBIT_DASH("MP4_360P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_48KBIT_DASH("MP4_480P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_48KBIT_DASH("MP4_720P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_48KBIT_DASH("MP4_720P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH_2("MP4_2160P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_1080P_30FPS_AAC_48KBIT_DASH_2("MP4_1080P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_4320P_30FPS_AAC_48KBIT_DASH("MP4_4320P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_160KBIT_DASH("WEBM_1440P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_160KBIT_DASH_3D("WEBM_1440P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    OGG_OPUS_160KBIT_DASH("OGG_160KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_160KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_160KBIT_DASH("WEBM_2160P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_160KBIT_DASH_3D("WEBM_2160P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_160KBIT_DASH("WEBM_1080P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_1080P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_160KBIT_DASH("WEBM_1080P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_160KBIT_DASH_3D("WEBM_1080P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_160KBIT_DASH("WEBM_1440P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_1440P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_160KBIT_DASH("WEBM_144P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_144P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_160KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_160KBIT_DASH("WEBM_240P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_240P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_160KBIT_DASH("WEBM_360P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_360P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_160KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_160KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_160KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_160KBIT_DASH("WEBM_720P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_720P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_160KBIT_DASH("WEBM_720P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_160KBIT_DASH_3D("WEBM_720P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_48KBIT_DASH("WEBM_1440P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_48KBIT_DASH_3D("WEBM_1440P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    OGG_OPUS_48KBIT_DASH("OGG_48KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_48KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_48KBIT_DASH("WEBM_2160P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_48KBIT_DASH_3D("WEBM_2160P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_48KBIT_DASH("WEBM_1080P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_1080P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_48KBIT_DASH("WEBM_1080P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_48KBIT_DASH_3D("WEBM_1080P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_48KBIT_DASH("WEBM_1440P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_1440P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_48KBIT_DASH("WEBM_144P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_144P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_48KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_48KBIT_DASH("WEBM_240P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_240P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_48KBIT_DASH("WEBM_360P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_360P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_48KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_48KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_48KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_48KBIT_DASH("WEBM_720P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_720P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_48KBIT_DASH("WEBM_720P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_48KBIT_DASH_3D("WEBM_720P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_64KBIT_DASH("WEBM_1440P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_64KBIT_DASH_3D("WEBM_1440P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    OGG_OPUS_64KBIT_DASH("OGG_64KBIT", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_64KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_64KBIT_DASH("WEBM_2160P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_64KBIT_DASH_3D("WEBM_2160P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_64KBIT_DASH("WEBM_1080P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_1080P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_64KBIT_DASH("WEBM_1080P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_64KBIT_DASH_3D("WEBM_1080P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_64KBIT_DASH("WEBM_1440P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_1440P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_64KBIT_DASH("WEBM_144P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_144P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_64KBIT_DASH("WEBM_2160P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_2160P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_64KBIT_DASH("WEBM_240P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_240P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_64KBIT_DASH("WEBM_360P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_360P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_64KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_64KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_64KBIT_DASH("WEBM_480P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_480P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_64KBIT_DASH("WEBM_720P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_720P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_64KBIT_DASH("WEBM_720P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_64KBIT_DASH_3D("WEBM_720P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_128KBIT_DASH("WEBM_4320P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_4320P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_192KBIT_DASH("WEBM_4320P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_4320P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_160KBIT_DASH("WEBM_4320P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_4320P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_48KBIT_DASH("WEBM_4320P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_4320P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_64KBIT_DASH("WEBM_4320P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_4320P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    MP4_H264_1920P_60FPS_AAC_128KBIT_DASH("MP4_1920P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_128KBIT_DASH("WEBM_1920P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_128KBIT_DASH_3D("WEBM_1920P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_128KBIT_DASH("WEBM_1920P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_128KBIT_DASH_3D("WEBM_1920P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_192KBIT_DASH("WEBM_1920P_30FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_192KBIT_DASH_3D("WEBM_1920P_30FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_192KBIT_DASH("WEBM_1920P_60FPS_VORBIS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_192KBIT_DASH_3D("WEBM_1920P_60FPS_VORBIS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    MP4_H264_1920P_60FPS_AAC_256KBIT_DASH("MP4_1920P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1920P_60FPS_AAC_48KBIT_DASH("MP4_1920P_60FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_160KBIT_DASH("WEBM_1920P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_160KBIT_DASH_3D("WEBM_1920P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_160KBIT_DASH("WEBM_1920P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_160KBIT_DASH_3D("WEBM_1920P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_48KBIT_DASH("WEBM_1920P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_48KBIT_DASH_3D("WEBM_1920P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_48KBIT_DASH("WEBM_1920P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_48KBIT_DASH_3D("WEBM_1920P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_64KBIT_DASH("WEBM_1920P_30FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_64KBIT_DASH_3D("WEBM_1920P_30FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_64KBIT_DASH("WEBM_1920P_60FPS_OPUS", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_64KBIT_DASH_3D("WEBM_1920P_60FPS_OPUS_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    MP4_H264_1920P_30FPS_AAC_128KBIT_DASH("MP4_1920P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_1920P_30FPS_AAC_256KBIT_DASH("MP4_1920P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1920P_30FPS_AAC_48KBIT_DASH("MP4_1920P_30FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_144P_15FPS_AAC_128KBIT_DASH("MP4_144P_15FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_144P_15FPS_AAC_256KBIT_DASH("MP4_144P_15FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_144P_15FPS_AAC_48KBIT_DASH("MP4_144P_15FPS_AAC", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null)
    // ###APPEND###
    ;

    private static final Icon                     AUDIO                = new AbstractIcon(IconKey.ICON_AUDIO, 16);

    public static HashMap<String, YoutubeVariant> COMPATIBILITY_MAP    = new HashMap<String, YoutubeVariant>();

    public static HashMap<String, String>         COMPATIBILITY_MAP_ID = new HashMap<String, String>();

    private static final Icon                     IMAGE                = new AbstractIcon(IconKey.ICON_IMAGE, 16);

    private static final Icon                     TEXT                 = new AbstractIcon(IconKey.ICON_TEXT, 16);

    private static final Icon                     VIDEO                = new AbstractIcon(IconKey.ICON_VIDEO, 16);

    static {

        COMPATIBILITY_MAP_ID.put("AAC_128", "AAC_128KBIT");
        COMPATIBILITY_MAP_ID.put("AAC_256", "AAC_256KBIT");
        COMPATIBILITY_MAP_ID.put("AAC_48", "AAC_48KBIT");
        COMPATIBILITY_MAP_ID.put("DASH_AUDIO_OPUS_160KBIT", "OGG_160KBIT");
        COMPATIBILITY_MAP_ID.put("DASH_AUDIO_OPUS_48KBIT", "OGG_48KBIT");
        COMPATIBILITY_MAP_ID.put("DASH_AUDIO_OPUS_64KBIT", "OGG_64KBIT");
        COMPATIBILITY_MAP_ID.put("1080P_WebM", "WEBM_1080P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("1080P_60fps_WebM", "WEBM_1080P_60FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("1440P_WebM", "WEBM_1440P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("1440P_60fps_WebM", "WEBM_1440P_60FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("144P_WebM", "WEBM_144P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("2160P_WebM", "WEBM_2160P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("2160P_60fps_WebM", "WEBM_2160P_60FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("240P_WebM", "WEBM_240P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("360P_WebM", "WEBM_360P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("480P_WebM", "WEBM_480P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("720P_WebM", "WEBM_720P_30FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("720P_60fps_WebM", "WEBM_720P_60FPS_OPUS");
        COMPATIBILITY_MAP_ID.put("AAC_152", "AAC_152KBIT");
        COMPATIBILITY_MAP_ID.put("AAC_192", "AAC_192KBIT");
        COMPATIBILITY_MAP_ID.put("AAC_96", "AAC_96KBIT");
        COMPATIBILITY_MAP_ID.put("M4A_128", "M4A_128KBIT");
        COMPATIBILITY_MAP_ID.put("M4A_152", "M4A_152KBIT");
        COMPATIBILITY_MAP_ID.put("M4A_192", "M4A_192KBIT");
        COMPATIBILITY_MAP_ID.put("M4A_96", "M4A_96KBIT");
        COMPATIBILITY_MAP_ID.put("FLV_240_LOW", "FLV_240P_30FPS_MP3");
        COMPATIBILITY_MAP_ID.put("FLV_270_HIGH", "FLV_270P_30FPS_MP3");
        COMPATIBILITY_MAP_ID.put("FLV_360", "FLV_360P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("FLV_480", "FLV_480P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("M4A_256", "M4A_256KBIT");
        COMPATIBILITY_MAP_ID.put("M4A_48", "M4A_48KBIT");
        COMPATIBILITY_MAP_ID.put("MP3_64KBit", "MP3_64KBIT");
        COMPATIBILITY_MAP_ID.put("MP4_1080", "MP4_1080P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_360", "MP4_360P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_3D_1080", "MP4_1080P_30FPS_AAC_3D");
        COMPATIBILITY_MAP_ID.put("MP4_3D_240", "MP4_240P_30FPS_AAC_3D");
        COMPATIBILITY_MAP_ID.put("MP4_3D_360", "MP4_360P_30FPS_AAC_3D");
        COMPATIBILITY_MAP_ID.put("MP4_3D_720", "MP4_720P_30FPS_AAC_3D");
        COMPATIBILITY_MAP_ID.put("MP4_720", "MP4_720P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_144", "MP4_144P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_1440", "MP4_1440P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_240", "MP4_240P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_480", "MP4_480P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_2160p4k8k", "MP4_2160P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_1080p_60fps", "MP4_1080P_60FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_2160p", "MP4_2160P_60FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_720p_60fps", "MP4_720P_60FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_ORIGINAL", "MP4_2160P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("THREEGP_144", "THREEGP_144P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("THREEGP_240_HIGH", "THREEGP_240P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("THREEGP_240_LOW", "THREEGP_240P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("WEBM_1080", "WEBM_1080P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_360", "WEBM_360P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_3D_360_128", "WEBM_360P_30FPS_VORBIS_3D");
        COMPATIBILITY_MAP_ID.put("WEBM_3D_360_192", "WEBM_360P_30FPS_VORBIS_3D");
        COMPATIBILITY_MAP_ID.put("WEBM_3D_720", "WEBM_720P_30FPS_VORBIS_3D");
        COMPATIBILITY_MAP_ID.put("WEBM_480", "WEBM_480P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_720", "WEBM_720P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_1080_60FPS", "WEBM_1080P_60FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_144", "WEBM_144P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_1440", "WEBM_1440P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_2160", "WEBM_2160P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_240", "WEBM_240P_30FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("WEBM_720_60FPS", "WEBM_720P_60FPS_VORBIS");
        COMPATIBILITY_MAP_ID.put("2160P_Mp4", "MP4_2160P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("4320P_Mp4", "MP4_4320P_30FPS_AAC");
        COMPATIBILITY_MAP_ID.put("480P_Mp4", "MP4_480P_24FPS_AAC");
        COMPATIBILITY_MAP_ID.put("MP4_72", "MP4_72P_6FPS_AAC");
        // ###APPEND_COMPATIBILITY_MAP_ID###
    }

    static {

        COMPATIBILITY_MAP.put("AAC_128", AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("AAC_256", AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("AAC_48", AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_AUDIO_OPUS_160KBIT", OGG_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_AUDIO_OPUS_48KBIT", OGG_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_AUDIO_OPUS_64KBIT", OGG_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_30FPS_OPUS_160KBIT", WEBM_VP9_1080P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_30FPS_OPUS_48KBIT", WEBM_VP9_1080P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_30FPS_OPUS_64KBIT", WEBM_VP9_1080P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_60FPS_OPUS_160KBIT", WEBM_VP9_1080P_60FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_60FPS_OPUS_48KBIT", WEBM_VP9_1080P_60FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1080P_60FPS_OPUS_64KBIT", WEBM_VP9_1080P_60FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_30FPS_OPUS_160KBIT", WEBM_VP9_1440P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_30FPS_OPUS_48KBIT", WEBM_VP9_1440P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_30FPS_OPUS_64KBIT", WEBM_VP9_1440P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_60FPS_OPUS_160KBIT", WEBM_VP9_1440P_60FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_60FPS_OPUS_48KBIT", WEBM_VP9_1440P_60FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_60FPS_OPUS_64KBIT", WEBM_VP9_1440P_60FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_60FPS_VORBIS_128KBIT", WEBM_VP9_1440P_60FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_1440P_60FPS_VORBIS_192KBIT", WEBM_VP9_1440P_60FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_144P_30FPS_OPUS_160KBIT", WEBM_VP9_144P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_144P_30FPS_OPUS_48KBIT", WEBM_VP9_144P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_144P_30FPS_OPUS_64KBIT", WEBM_VP9_144P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_160KBIT", WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_160KBIT_2", WEBM_VP9_2160P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_48KBIT", WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_48KBIT_2", WEBM_VP9_2160P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_64KBIT", WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_OPUS_64KBIT_2", WEBM_VP9_2160P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_VORBIS_128KBIT", WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_30FPS_VORBIS_192KBIT", WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_60FPS_OPUS_160KBIT", WEBM_VP9_2160P_60FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_60FPS_OPUS_48KBIT", WEBM_VP9_2160P_60FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_60FPS_OPUS_64KBIT", WEBM_VP9_2160P_60FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_60FPS_VORBIS_128KBIT", WEBM_VP9_2160P_60FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_2160P_60FPS_VORBIS_192KBIT", WEBM_VP9_2160P_60FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_240P_30FPS_OPUS_160KBIT", WEBM_VP9_240P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_240P_30FPS_OPUS_48KBIT", WEBM_VP9_240P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_240P_30FPS_OPUS_64KBIT", WEBM_VP9_240P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_360P_30FPS_OPUS_160KBIT", WEBM_VP9_360P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_360P_30FPS_OPUS_48KBIT", WEBM_VP9_360P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_360P_30FPS_OPUS_64KBIT", WEBM_VP9_360P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_160KBIT", WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_160KBIT_2", WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_160KBIT_3", WEBM_VP9_480P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_48KBIT", WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_48KBIT_2", WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_48KBIT_3", WEBM_VP9_480P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_64KBIT", WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_64KBIT_2", WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_480P_30FPS_OPUS_64KBIT_3", WEBM_VP9_480P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_30FPS_OPUS_160KBIT", WEBM_VP9_720P_30FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_30FPS_OPUS_48KBIT", WEBM_VP9_720P_30FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_30FPS_OPUS_64KBIT", WEBM_VP9_720P_30FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_60FPS_OPUS_160KBIT", WEBM_VP9_720P_60FPS_OPUS_160KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_60FPS_OPUS_48KBIT", WEBM_VP9_720P_60FPS_OPUS_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_VP9_720P_60FPS_OPUS_64KBIT", WEBM_VP9_720P_60FPS_OPUS_64KBIT_DASH);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_360P_3D_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_360P_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_720P_3D_V1", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_720P_V1", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_152_720P_3D_V3", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_152_720P_V3", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_192_720P_3D_V4", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_192_720P_V4", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_96_360P_3D_V1", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_96_360P_V1", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_360P_3D_V4", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_360P_V4", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_720P_3D_V1", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_720P_V1", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_152_720P_3D_V3", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_152_720P_V3", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_192_720P_3D_V4", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_192_720P_V4", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_96_360P_3D_V1", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_96_360P_V1", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT_2);
        COMPATIBILITY_MAP.put("FLV_240_LOW", FLV_H263_240P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("FLV_270_HIGH", FLV_H263_270P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("FLV_360", FLV_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("FLV_480", FLV_H264_480P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("M4A_128", M4A_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("M4A_256", M4A_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("M4A_48", M4A_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP3_1", DEMUX_MP3_FLV_H263_240P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("MP3_2", DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("MP4_1080", MP4_H264_1080P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("MP4_360", MP4_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("MP4_360_AAC96", MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("MP4_3D_1080", MP4_H264_1080P_30FPS_AAC_192KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_3D_240", MP4_H264_240P_30FPS_AAC_96KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_3D_360", MP4_H264_360P_30FPS_AAC_128KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_3D_720", MP4_H264_720P_30FPS_AAC_192KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_720", MP4_H264_720P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("MP4_720_128AAC", MP4_H264_720P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("MP4_720_152AAC", MP4_H264_720P_30FPS_AAC_152KBIT);
        COMPATIBILITY_MAP.put("MP4_DASH_1080_AAC128", MP4_H264_1080P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_1080_AAC256", MP4_H264_1080P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_1080_AAC48", MP4_H264_1080P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_144_AAC128", MP4_H264_144P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_144_AAC256", MP4_H264_144P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_144_AAC48", MP4_H264_144P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_1440_AAC128", MP4_H264_1440P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_1440_AAC256", MP4_H264_1440P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_1440_AAC48", MP4_H264_1440P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_240_AAC128", MP4_H264_240P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_240_AAC256", MP4_H264_240P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_240_AAC48", MP4_H264_240P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_360_AAC128", MP4_H264_360P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_360_AAC256", MP4_H264_360P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_360_AAC48", MP4_H264_360P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_480_AAC128", MP4_H264_480P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_480_AAC256", MP4_H264_480P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_480_AAC48", MP4_H264_480P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_720_AAC128", MP4_H264_720P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_720_AAC256", MP4_H264_720P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_720_AAC48", MP4_H264_720P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_ORIGINAL_AAC128", MP4_H264_2160P_30FPS_AAC_128KBIT_DASH_2);
        COMPATIBILITY_MAP.put("MP4_DASH_ORIGINAL_AAC256", MP4_H264_2160P_30FPS_AAC_256KBIT_DASH_2);
        COMPATIBILITY_MAP.put("MP4_DASH_ORIGINAL_AAC48", MP4_H264_2160P_30FPS_AAC_48KBIT_DASH_2);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_1080_H264_FPS_60_AAC128", MP4_H264_1080P_60FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_1080_H264_FPS_60_AAC256", MP4_H264_1080P_60FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_1080_H264_FPS_60_AAC48", MP4_H264_1080P_60FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_2160_H264_FPS_60_AAC128", MP4_H264_2160P_60FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_2160_H264_FPS_60_AAC256", MP4_H264_2160P_60FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_2160_H264_FPS_60_AAC48", MP4_H264_2160P_60FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_720_H264_FPS_60_AAC128", MP4_H264_720P_60FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_720_H264_FPS_60_AAC256", MP4_H264_720P_60FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_DASH_VIDEO_720_H264_FPS_60_AAC48", MP4_H264_720P_60FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP4_ORIGINAL", MP4_H264_2160P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("THREEGP_144", THREEGP_H264_144P_30FPS_AAC_31KBIT);
        COMPATIBILITY_MAP.put("THREEGP_240_HIGH", THREEGP_H264_240P_30FPS_AAC_32KBIT);
        COMPATIBILITY_MAP.put("THREEGP_240_LOW", THREEGP_H263_240P_30FPS_AAC_32KBIT);
        COMPATIBILITY_MAP.put("WEBM_1080", WEBM_VP8_1080P_30FPS_VORBIS_192KBIT);
        COMPATIBILITY_MAP.put("WEBM_360", WEBM_VP8_360P_30FPS_VORBIS_128KBIT);
        COMPATIBILITY_MAP.put("WEBM_3D_360_128", WEBM_VP9_360P_30FPS_VORBIS_128KBIT_3D);
        COMPATIBILITY_MAP.put("WEBM_3D_360_192", WEBM_VP9_360P_30FPS_VORBIS_192KBIT_3D);
        COMPATIBILITY_MAP.put("WEBM_3D_720", WEBM_VP8_720P_30FPS_VORBIS_192KBIT_3D);
        COMPATIBILITY_MAP.put("WEBM_480", WEBM_VP8_480P_30FPS_VORBIS_128KBIT);
        COMPATIBILITY_MAP.put("WEBM_720", WEBM_VP8_720P_30FPS_VORBIS_192KBIT);
        COMPATIBILITY_MAP.put("WEBM_DASH_1080_60FPS_VORBIS128", WEBM_VP9_1080P_60FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_1080_60FPS_VORBIS192", WEBM_VP9_1080P_60FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_1080_VORBIS128", WEBM_VP9_1080P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_1080_VORBIS192", WEBM_VP9_1080P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_144_VORBIS128", WEBM_VP9_144P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_144_VORBIS192", WEBM_VP9_144P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_1440_VORBIS128", WEBM_VP9_1440P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_1440_VORBIS192", WEBM_VP9_1440P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_2160_VORBIS128", WEBM_VP9_2160P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_2160_VORBIS192", WEBM_VP9_2160P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_240_VORBIS128", WEBM_VP9_240P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_240_VORBIS192", WEBM_VP9_240P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_360_VORBIS128", WEBM_VP9_360P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_360_VORBIS192", WEBM_VP9_360P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS128_1", WEBM_VP9_480P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS128_2", WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS128_3", WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS192_1", WEBM_VP9_480P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS192_2", WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_480_VORBIS192_3", WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_720_60FPS_VORBIS128", WEBM_VP9_720P_60FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_720_60FPS_VORBIS192", WEBM_VP9_720P_60FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_720_VORBIS128", WEBM_VP9_720P_30FPS_VORBIS_128KBIT_DASH);
        COMPATIBILITY_MAP.put("WEBM_DASH_720_VORBIS192", WEBM_VP9_720P_30FPS_VORBIS_192KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_2160P_30FPS_AAC_128KBIT", MP4_H264_2160P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_2160P_30FPS_AAC_256KBIT", MP4_H264_2160P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_2160P_30FPS_AAC_48KBIT", MP4_H264_2160P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_4320P_24FPS_AAC_128KBIT", MP4_H264_4320P_30FPS_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_4320P_24FPS_AAC_256KBIT", MP4_H264_4320P_30FPS_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("DASH_H264_4320P_24FPS_AAC_48KBIT", MP4_H264_4320P_30FPS_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("H264_480P_24FPS_AAC_128KBIT", MP4_H264_480P_24FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("H264_480P_24FPS_AAC_128KBIT_2", MP4_H264_480P_24FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("HLS_MP4_720", HLS_MP4_H264_720P_30FPS_AAC_256KBIT);
        COMPATIBILITY_MAP.put("HLS_MP4_480", HLS_MP4_H264_480P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("HLS_MP4_360", HLS_MP4_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("HLS_MP4_240", HLS_MP4_H264_240P_15FPS_AAC_48KBIT);
        COMPATIBILITY_MAP.put("HLS_MP4_240_2", HLS_MP4_H264_240P_30FPS_AAC_48KBIT);
        COMPATIBILITY_MAP.put("HLS_MP4_72", HLS_MP4_H264_72P_6FPS_AAC_24KBIT);
        // ###APPEND_COMPATIBILITY_MAP###
    }

    public static String getCompatibleTypeID(String b) {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
            YoutubeVariant variant = COMPATIBILITY_MAP.get(b);
            if (variant != null) {
                return variant.name();
            }
        } else {

            String typeID = COMPATIBILITY_MAP_ID.get(b);
            if (typeID != null) {
                return typeID;
            }
        }
        return b;
    }

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
        if (id != null && id.contains("_FPS_")) {
            System.out.println(id);
        }
        this.fileExtension = fileExtension;
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.qualityRating = 0d + (video != null ? video.getQualityRating() : 0) + (audio != null ? audio.getQualityRating() : 0) + (data != null ? data.getQualityRating() : 0);
        this.iTagData = data;
        this.converter = converter;
        this.filenameModifier = filenameModifier;

    }

    @Override
    public String _getExtendedName(Object caller) {
        return name() + " - " + getQualityRating();
    }

    public Icon _getIcon(Object caller) {
        final VariantGroup lGroup = getGroup();
        if (lGroup != null) {
            switch (lGroup) {
            case AUDIO:
                return AUDIO;
            case VIDEO:
            case VIDEO_3D:
                return VIDEO;
            case IMAGE:
                return IMAGE;
            default:
                return TEXT;
            }
        }
        return null;
    }

    public String _getName(Object caller) {
        switch (getGroup()) {
        case AUDIO:
            if (iTagAudio != null) {
                String cust = iTagAudio.getCustomName(this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }

            } else {
                String cust = iTagVideo.getCustomName(this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }

            }
            return _GUI.T.YoutubeVariant_name_generic_audio(getAudioBitrateLabel(caller), getAudioCodecLabel(caller));
        case VIDEO:
        case VIDEO_3D:
            String cust = iTagVideo.getCustomName(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            if (caller != null && caller instanceof AbstractConfigPanel) {
                // do nto show audio in the config panel
                cust = iTagVideo.getCustomQualityExtension(caller, this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }

                String res = iTagVideo.getVideoResolution(caller).getLabel(caller, this);
                String fps = iTagVideo.getVideoFrameRate(caller).getLabel(caller, this);
                String audio = getAudioCodecLabel(caller);
                return _GUI.T.YoutubeVariant_name_generic_video3(res, fps, audio);

            }
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(caller), iTagVideo.getVideoContainer(caller).getLabel(caller), getAudioBitrateLabel(caller), getAudioCodecLabel(caller));
        default:
            cust = iTagData.getCustomName(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            return iTagData.getDisplayName();
        }

    }

    @Override
    public String _getTooltipDescription(Object caller) {
        return _getExtendedName(caller);
    }

    public String _getUniqueId() {
        return name();
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        if (converter != null) {
            converter.run(downloadLink, plugin);
        }
    }

    public String getAudioBitrateLabel(Object caller) {
        if (iTagAudio != null) {
            return iTagAudio.getAudioBitrate(caller).getLabel(caller);
        }
        return iTagVideo.getAudioBitrate(caller).getLabel(caller);
    }

    public String getAudioCodecLabel(Object caller) {
        if (StringUtils.equalsIgnoreCase(fileExtension, "m4a")) {
            return "M4A";
        }
        if (iTagAudio != null) {
            return iTagAudio.getAudioCodec(caller).getLabel(caller);
        }
        return iTagVideo.getAudioCodec(caller).getLabel(caller);
    }

    // public String getAudioCodec() {
    // if (getiTagAudio() != null) {
    // return getiTagAudio().getAudioCodec(caller)
    // }
    // if (getiTagVideo() != null) {
    // return getiTagVideo().getCodecAudio();
    // }
    // return null;
    // }
    //
    // public String getAudioQuality() {
    // if (getiTagAudio() != null) {
    // return getiTagAudio().getQualityAudio();
    // }
    // if (getiTagVideo() != null) {
    // return getiTagVideo().getQualityAudio();
    // }
    // return null;
    // }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public VideoFrameRate getFrameRate(Object link) {

        return iTagVideo.getVideoFrameRate(link);
    }

    public YoutubeVariantInterface.VariantGroup getGroup() {
        return this.group;
    }

    public String getId() {
        return id;
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

    public String getQualityExtension(Object caller) {
        switch (getGroup()) {
        case AUDIO:
            if (iTagAudio != null) {
                String cust = iTagAudio.getCustomQualityExtension(caller, this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }
                return iTagAudio.getAudioBitrate(caller).getLabel(caller);
            } else {
                // demux
                String cust = iTagVideo.getCustomQualityExtension(caller, this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }
                return iTagVideo.getAudioBitrate(caller).getLabel(caller);
            }
        case VIDEO:
            String cust = iTagVideo.getCustomQualityExtension(caller, this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }

            String ret = iTagVideo.getVideoResolution(caller).getLabel(caller, this);
            VideoFrameRate fps = iTagVideo.getVideoFrameRate(caller);
            if (fps != null) {
                ret += " " + fps.getLabel(caller, this);
            }
            return ret;
        case VIDEO_3D:
            cust = iTagVideo.getCustomQualityExtension(caller, this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }

            ret = iTagVideo.getVideoResolution(caller).getLabel(caller, this);
            fps = iTagVideo.getVideoFrameRate(caller);
            if (fps != null) {
                ret += " " + fps.getLabel(caller, this);
            }

            ret += " 3D";

            if (caller != null && caller instanceof AbstractConfigPanel) {
                // add container for the config panel
                ret = ret + " " + getiTagVideo().getVideoContainer(caller).getLabel(caller);
            }
            return ret;
        default:
            return iTagData.getCustomQualityExtension(caller, this);

        }
    }

    public double getQualityRating() {
        return this.qualityRating;
    }

    public YoutubeVariantInterface.DownloadType getType() {
        return this.type;
    }

    public String getTypeId() {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
            return name();
        }
        if (this.id == null) {
            return this.name();
        }
        return this.id;
    }

    @Override
    public boolean hasConverter(DownloadLink downloadLink) {
        return converter != null;
    }

    public boolean isValidFor(YoutubeClipData vid) {
        switch (getGroup()) {
        case VIDEO:
            // && !StringUtils.equalsIgnoreCase("mp4", getFileExtension())
            if (vid.is3D() && !StringUtils.equalsIgnoreCase("mp4", getFileExtension())) {
                return false;
            }
            break;
        case VIDEO_3D:
            if (!vid.is3D()) {
                return false;
            }
            break;
        case SUBTITLES:
            return vid.subtitles != null && vid.subtitles.size() > 0;
        case DESCRIPTION:
            return StringUtils.isNotEmpty(vid.description);
        default:
        }

        return true;
    }

    /**
     * returns true if this variant requires a video tool like ffmpge for muxing, demuxing or container converting
     *
     * @return
     */
    public boolean isVideoToolRequired() {
        if (iTagVideo != null && iTagAudio != null) {
            return true;
        }
        if (iTagVideo != null && iTagVideo.name().contains("DASH")) {
            return true;
        }
        if (iTagAudio != null && iTagAudio.name().contains("DASH")) {
            return true;
        }
        if (converter != null) {
            if (converter instanceof ExternalToolRequired) {
                return true;
            }

        }
        return false;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();

        return ret;
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (filenameModifier != null) {
            return filenameModifier.run(formattedFilename, link);
        }
        return formattedFilename;
    }

    public static String getCompatibleVariantName(String variantName) {
        YoutubeVariant ret = COMPATIBILITY_MAP.get(variantName);
        if (ret != null) {
            return ret.name();
        }
        return variantName;
    }

}