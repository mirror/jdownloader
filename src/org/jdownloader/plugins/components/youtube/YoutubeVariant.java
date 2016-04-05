package org.jdownloader.plugins.components.youtube;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;

import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public enum YoutubeVariant implements YoutubeVariantInterface {
    SUBTITLES(null, YoutubeVariantInterface.VariantGroup.SUBTITLES, YoutubeVariantInterface.DownloadType.SUBTITLES, "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSubtitleNamer.getInstance(), YoutubeSRTConverter.getInstance()),
    DESCRIPTION(null, YoutubeVariantInterface.VariantGroup.DESCRIPTION, YoutubeVariantInterface.DownloadType.DESCRIPTION, "txt", null, null, YoutubeITAG.DESCRIPTION, null, null),
    IMAGE_HQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_HQ, null, null),
    IMAGE_LQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_LQ, null, null),
    IMAGE_MAX(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MAX, null, null),
    IMAGE_MQ(null, YoutubeVariantInterface.VariantGroup.IMAGE, YoutubeVariantInterface.DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MQ, null, null)

    ,
    FLV_H264_360P_30FPS_AAC_128KBIT("360P_FLV", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    FLV_H264_3D_360P_30FPS_AAC_128KBIT("360P_FLV_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_FLV_H264_360P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeFLVToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_FLV_H264_360P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeFLVToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    FLV_H264_480P_30FPS_AAC_128KBIT("480P_FLV", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),
    FLV_H264_3D_480P_30FPS_AAC_128KBIT("480P_FLV_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_FLV_H264_480P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, YoutubeFLVToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_FLV_H264_480P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, YoutubeFLVToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    FLV_H263_270P_30FPS_MP3_64KBIT("270P_FLV", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, null),
    FLV_H263_3D_270P_30FPS_MP3_64KBIT("270P_FLV_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, null),
    DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT("MP3_64", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null, YoutubeFLVToMP3Audio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.MP3.getRating() + AudioBitrate.KBIT_64.getRating() - 0.00000270;
        }
    },
    FLV_H263_240P_30FPS_MP3_64KBIT("240P_FLV", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_H263_3D_240P_30FPS_MP3_64KBIT("240P_FLV_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    DEMUX_MP3_FLV_H263_240P_30FPS_MP3_64KBIT("MP3_64", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, YoutubeFLVToMP3Audio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.MP3.getRating() + AudioBitrate.KBIT_64.getRating() - 0.00000240;
        }
    },
    THREEGP_H263_144P_15FPS_AMRNB_12KBIT("144P_3GP", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT, null, null, null, null),
    THREEGP_H263_3D_144P_15FPS_AMRNB_12KBIT("144P_3GP_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT, null, null, null, null),
    MP4_H264_1080P_30FPS_AAC_192KBIT("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    MP4_H264_3D_1080P_30FPS_AAC_192KBIT("1080P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT_2("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT_2("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000001080;
        }
    },
    MP4_H264_3D_240P_30FPS_AAC_96KBIT("240P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_240P_30FPS_AAC_96KBIT("AAC_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_MP4_H264_240P_30FPS_AAC_96KBIT("M4A_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000240;
        }
    },
    MP4_H264_360P_30FPS_AAC_128KBIT("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    MP4_H264_3D_360P_30FPS_AAC_128KBIT("360P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_128KBIT_2("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_128KBIT_2("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    MP4_H264_3D_360P_30FPS_AAC_96KBIT("360P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT("AAC_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT("M4A_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    MP4_H264_360P_30FPS_AAC_96KBIT("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT_2("AAC_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT_2("M4A_96", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        }
    },
    MP4_H264_720P_30FPS_AAC_192KBIT("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    MP4_H264_3D_720P_30FPS_AAC_192KBIT("720P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT_2("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT_2("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        }
    },
    MP4_H264_3D_720P_30FPS_AAC_128KBIT("720P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    MP4_H264_3D_720P_30FPS_AAC_152KBIT("720P_MP4_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT("AAC_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT("M4A_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_128KBIT("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT_2("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        }
    },
    MP4_H264_720P_30FPS_AAC_152KBIT("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT_2("AAC_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT_2("M4A_152", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        }
    },
    MP4_H264_2206P_30FPS_AAC_192KBIT("2206P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, null),
    DEMUX_AAC_MP4_H264_2206P_30FPS_AAC_192KBIT("AAC_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000002206;
        }
    },
    DEMUX_M4A_MP4_H264_2206P_30FPS_AAC_192KBIT("M4A_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.000002206;
        }
    },
    THREEGP_MPEG4_144P_30FPS_AAC_24KBIT("144P_3GP", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_MPEG4_3D_144P_30FPS_AAC_24KBIT("144P_3GP_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_MPEG4_180P_30FPS_AAC_32KBIT("180P_3GP", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    THREEGP_MPEG4_3D_180P_30FPS_AAC_32KBIT("180P_3GP_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    THREEGP_MPEG4_180P_30FPS_AAC_32KBIT_2("180P_3GP", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_MPEG4_3D_180P_30FPS_AAC_32KBIT_2("180P_3GP_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),
    WEBM_VP8_1080P_30FPS_VORBIS_192KBIT("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_1080P_30FPS_VORBIS_192KBIT("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_360P_30FPS_VORBIS_128KBIT("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null, null),
    WEBM_VP8_3D_360P_30FPS_VORBIS_192KBIT("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_VP8_360P_30FPS_VORBIS_128KBIT("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_360P_30FPS_VORBIS_128KBIT_2("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_480P_30FPS_VORBIS_128KBIT("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_480P_30FPS_VORBIS_128KBIT("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_720P_30FPS_VORBIS_192KBIT("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    MP4_H264_480P_24FPS_AAC_128KBIT("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    MP4_H264_480P_24FPS_AAC_128KBIT_2("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT_2("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "aac", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT_2("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_VP8_3D_720P_30FPS_VORBIS_192KBIT_2("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null),
    HLS_MP4_H264_72P_6FPS_AAC_24KBIT("72P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_72P_6FPS_AAC_24KBIT("AAC_24", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_24.getRating() - 0.0000072;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_72P_6FPS_AAC_24KBIT("M4A_24", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_24.getRating() - 0.0000072;
        }
    },
    HLS_MP4_H264_240P_15FPS_AAC_48KBIT("240P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_15FPS_AAC_48KBIT("AAC_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_240P_15FPS_AAC_48KBIT("M4A_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    HLS_MP4_H264_240P_30FPS_AAC_48KBIT("240P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_30FPS_AAC_48KBIT("AAC_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_240P_30FPS_AAC_48KBIT("M4A_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_48.getRating() - 0.00000240;
        }
    },
    HLS_MP4_H264_360P_30FPS_AAC_128KBIT("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_360P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_360P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000360;
        }
    },
    HLS_MP4_H264_480P_30FPS_AAC_128KBIT("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_480P_30FPS_AAC_128KBIT("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_480P_30FPS_AAC_128KBIT("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000480;
        }
    },
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT("AAC_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT("M4A_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("AAC_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2("M4A_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.00000720;
        }
    },
    HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("AAC_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "aac", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, YoutubeMP4ToAACAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC.getRating() + AudioBitrate.KBIT_256.getRating() - 0.000001080;
        }
    },
    DEMUX_M4A_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT("M4A_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.HLS_VIDEO, "m4a", YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null, YoutubeMP4ToM4AAudio.getInstance()) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_256.getRating() - 0.000001080;
        }
    },
    MP4_H264_1080P_60FPS_AAC_128KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    AAC_128KBIT_DASH("AAC_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    M4A_AAC_128KBIT_DASH("M4A_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_128.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_128KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_128KBIT_DASH("1440P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_128KBIT_DASH("144P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_128KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_128KBIT_DASH("240P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_128KBIT_DASH("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_128KBIT_DASH("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_128KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_128KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH_2("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_H264_4320P_24FPS_AAC_128KBIT_DASH("4320P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_4320P_24FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_128KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_1440P_60FPS_VORBIS_128KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    OGG_VORBIS_128KBIT_DASH("VORBIS_128", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_128KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_2160P_60FPS_VORBIS_128KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_128KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_1080P_30FPS_VORBIS_128KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_128KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_1080P_60FPS_VORBIS_128KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_128KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_1440P_30FPS_VORBIS_128KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_128KBIT_DASH("144P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_144P_30FPS_VORBIS_128KBIT_DASH("144P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_128KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_2160P_30FPS_VORBIS_128KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_128KBIT_DASH("240P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_240P_30FPS_VORBIS_128KBIT_DASH("240P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_DASH("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_360P_30FPS_VORBIS_128KBIT_DASH("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_128KBIT_DASH("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_480P_30FPS_VORBIS_128KBIT_DASH("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_128KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_720P_30FPS_VORBIS_128KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_128KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_3D_720P_60FPS_VORBIS_128KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_192KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_1440P_60FPS_VORBIS_192KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    OGG_VORBIS_192KBIT_DASH("VORBIS_192", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_192KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_2160P_60FPS_VORBIS_192KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_192KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_1080P_30FPS_VORBIS_192KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_192KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_1080P_60FPS_VORBIS_192KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_192KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_1440P_30FPS_VORBIS_192KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_192KBIT_DASH("144P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_144P_30FPS_VORBIS_192KBIT_DASH("144P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_192KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_2160P_30FPS_VORBIS_192KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_192KBIT_DASH("240P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_240P_30FPS_VORBIS_192KBIT_DASH("240P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_DASH("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_360P_30FPS_VORBIS_192KBIT_DASH("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_192KBIT_DASH("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_480P_30FPS_VORBIS_192KBIT_DASH("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_192KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_720P_30FPS_VORBIS_192KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_192KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    WEBM_VP9_3D_720P_60FPS_VORBIS_192KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null, null),
    MP4_H264_1080P_60FPS_AAC_256KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    AAC_256KBIT_DASH("AAC_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    M4A_AAC_256KBIT_DASH("M4A_256", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_256.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_256KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_256KBIT_DASH("1440P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_256KBIT_DASH("144P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_256KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_256KBIT_DASH("240P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_256KBIT_DASH("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_256KBIT_DASH("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_256KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_256KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH_2("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_4320P_24FPS_AAC_256KBIT_DASH("4320P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_4320P_24FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_H264_1080P_60FPS_AAC_48KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    AAC_48KBIT_DASH("AAC_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    M4A_AAC_48KBIT_DASH("M4A_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null) {
        @Override
        public double getQualityRating() {

            return AudioCodec.AAC_M4A.getRating() + AudioBitrate.KBIT_48.getRating();
        }
    },
    MP4_H264_1080P_30FPS_AAC_48KBIT_DASH("1080P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_1440P_30FPS_AAC_48KBIT_DASH("1440P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_144P_30FPS_AAC_48KBIT_DASH("144P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_60FPS_AAC_48KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_240P_30FPS_AAC_48KBIT_DASH("240P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_360P_30FPS_AAC_48KBIT_DASH("360P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_480P_30FPS_AAC_48KBIT_DASH("480P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_720P_60FPS_AAC_48KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_720P_30FPS_AAC_48KBIT_DASH("720P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH_2("2160P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_H264_4320P_24FPS_AAC_48KBIT_DASH("4320P_MP4", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_4320P_24FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_160KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_1440P_60FPS_OPUS_160KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    OGG_OPUS_160KBIT_DASH("OPUS_160", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_160KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_2160P_60FPS_OPUS_160KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_160KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_1080P_30FPS_OPUS_160KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_160KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_1080P_60FPS_OPUS_160KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_160KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_1440P_30FPS_OPUS_160KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_160KBIT_DASH("144P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_144P_30FPS_OPUS_160KBIT_DASH("144P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_160KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_2160P_30FPS_OPUS_160KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_160KBIT_DASH("240P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_240P_30FPS_OPUS_160KBIT_DASH("240P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_160KBIT_DASH("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_360P_30FPS_OPUS_160KBIT_DASH("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_160KBIT_DASH("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_480P_30FPS_OPUS_160KBIT_DASH("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_160KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_720P_30FPS_OPUS_160KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_160KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_3D_720P_60FPS_OPUS_160KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_48KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_1440P_60FPS_OPUS_48KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    OGG_OPUS_48KBIT_DASH("OPUS_48", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_48KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_2160P_60FPS_OPUS_48KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_48KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_1080P_30FPS_OPUS_48KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_48KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_1080P_60FPS_OPUS_48KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_48KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_1440P_30FPS_OPUS_48KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_48KBIT_DASH("144P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_144P_30FPS_OPUS_48KBIT_DASH("144P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_48KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_2160P_30FPS_OPUS_48KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_48KBIT_DASH("240P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_240P_30FPS_OPUS_48KBIT_DASH("240P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_48KBIT_DASH("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_360P_30FPS_OPUS_48KBIT_DASH("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_48KBIT_DASH("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_480P_30FPS_OPUS_48KBIT_DASH("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_48KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_720P_30FPS_OPUS_48KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_48KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_3D_720P_60FPS_OPUS_48KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_64KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_1440P_60FPS_OPUS_64KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    OGG_OPUS_64KBIT_DASH("OPUS_64", YoutubeVariantInterface.VariantGroup.AUDIO, YoutubeVariantInterface.DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_64KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_2160P_60FPS_OPUS_64KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_64KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_1080P_30FPS_OPUS_64KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_64KBIT_DASH("1080P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_1080P_60FPS_OPUS_64KBIT_DASH("1080P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_64KBIT_DASH("1440P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_1440P_30FPS_OPUS_64KBIT_DASH("1440P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_144P_30FPS_OPUS_64KBIT_DASH("144P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_144P_30FPS_OPUS_64KBIT_DASH("144P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_64KBIT_DASH("2160P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_2160P_30FPS_OPUS_64KBIT_DASH("2160P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_240P_30FPS_OPUS_64KBIT_DASH("240P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_240P_30FPS_OPUS_64KBIT_DASH("240P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_360P_30FPS_OPUS_64KBIT_DASH("360P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_360P_30FPS_OPUS_64KBIT_DASH("360P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_480P_30FPS_OPUS_64KBIT_DASH("480P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_480P_30FPS_OPUS_64KBIT_DASH("480P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_30FPS_OPUS_64KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_720P_30FPS_OPUS_64KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_720P_60FPS_OPUS_64KBIT_DASH("720P_WEBM", YoutubeVariantInterface.VariantGroup.VIDEO, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null),
    WEBM_VP9_3D_720P_60FPS_OPUS_64KBIT_DASH("720P_WEBM_3D", YoutubeVariantInterface.VariantGroup.VIDEO_3D, YoutubeVariantInterface.DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null, null)
    // ###APPEND###
    ;

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

    public static HashMap<String, YoutubeVariant> COMPATIBILITY_MAP = new HashMap<String, YoutubeVariant>();

    static {
        // ###APPEND_COMPATIBILITY_MAP###
    }

    @Override
    public String _getExtendedName() {
        return name() + " - " + getQualityRating();
    }

    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);
    private static final Icon IMAGE = new AbstractIcon(IconKey.ICON_IMAGE, 16);
    private static final Icon TEXT  = new AbstractIcon(IconKey.ICON_TEXT, 16);

    public Icon _getIcon() {
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

    public String _getName() {
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
            return _GUI.T.YoutubeVariant_name_generic_audio(getAudioQuality(), getAudioCodec());
        case VIDEO:
        case VIDEO_3D:
            String cust = iTagVideo.getCustomName(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(), getVideoCodec(), getAudioQuality(), getAudioCodec());
        default:
            cust = iTagData.getCustomName(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            return iTagData.getDisplayName();
        }

    }

    public String getQualityExtension() {
        switch (getGroup()) {
        case AUDIO:
            if (iTagAudio != null) {
                String cust = iTagAudio.getCustomQualityExtension(this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }
                return iTagAudio.getQualityAudio();
            } else {
                // demux
                String cust = iTagVideo.getCustomQualityExtension(this);
                if (StringUtils.isNotEmpty(cust)) {
                    return cust;
                }
                return iTagVideo.getQualityAudio();
            }
        case VIDEO:
            String cust = iTagVideo.getCustomQualityExtension(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            return iTagVideo.getQualityVideo();

        case VIDEO_3D:
            cust = iTagVideo.getCustomQualityExtension(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            String ret = iTagVideo.getQualityVideo();
            if (!ret.contains("3D")) {
                ret += " 3D";
            }
            return ret;
        default:
            cust = iTagData.getCustomQualityExtension(this);
            if (StringUtils.isNotEmpty(cust)) {
                return cust;
            }
            return iTagData.getQualityVideo();

        }
    }

    public String _getUniqueId() {
        return name();
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        if (converter != null) {
            converter.run(downloadLink, plugin);
        }
    }

    public String getAudioCodec() {
        if (getiTagAudio() != null) {
            return getiTagAudio().getCodecAudio();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getCodecAudio();
        }
        return null;
    }

    public String getAudioQuality() {
        if (getiTagAudio() != null) {
            return getiTagAudio().getQualityAudio();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getQualityAudio();
        }
        return null;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public YoutubeVariantInterface.VariantGroup getGroup() {
        return this.group;
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

    public double getQualityRating() {
        return this.qualityRating;
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

    public String getVideoCodec() {
        if (getiTagData() != null) {
            return getiTagData().getCodecVideo();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getCodecVideo();
        }
        return null;
    }

    @Override
    public boolean hasConverter(DownloadLink downloadLink) {
        return converter != null;
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
    public String _getTooltipDescription() {
        return _getExtendedName();
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

}