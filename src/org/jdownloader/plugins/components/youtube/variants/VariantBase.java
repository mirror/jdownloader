package org.jdownloader.plugins.components.youtube.variants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.plugins.components.youtube.ExternalToolRequired;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverter;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToAACAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToM4AAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToMP3Audio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterMP4ToAACAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterMP4ToM4AAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeSRTConverter;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public enum VariantBase {
    IMAGE_HQ(VariantGroup.IMAGE, DownloadType.IMAGE, FileContainer.JPG, null, null, YoutubeITAG.IMAGE_HQ, null),
    IMAGE_LQ(VariantGroup.IMAGE, DownloadType.IMAGE, FileContainer.JPG, null, null, YoutubeITAG.IMAGE_LQ, null),
    IMAGE_MAX(VariantGroup.IMAGE, DownloadType.IMAGE, FileContainer.JPG, null, null, YoutubeITAG.IMAGE_MAX, null),
    IMAGE_MQ(VariantGroup.IMAGE, DownloadType.IMAGE, FileContainer.JPG, null, null, YoutubeITAG.IMAGE_MQ, null),
    SUBTITLES(VariantGroup.SUBTITLES, DownloadType.SUBTITLES, FileContainer.SRT, null, null, YoutubeITAG.SUBTITLE, YoutubeSRTConverter.getInstance()),
    DESCRIPTION(VariantGroup.DESCRIPTION, DownloadType.DESCRIPTION, FileContainer.TXT, null, null, YoutubeITAG.DESCRIPTION, null),
    FLV_H264_360P_30FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.FLV, YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null),
    DEMUX_AAC_FLV_H264_360P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, YoutubeConverterFLVToAACAudio.getInstance()),
    DEMUX_M4A_FLV_H264_360P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, YoutubeConverterFLVToM4AAudio.getInstance()),
    FLV_H264_480P_30FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.FLV, YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null),
    DEMUX_AAC_FLV_H264_480P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, YoutubeConverterFLVToAACAudio.getInstance()),
    DEMUX_M4A_FLV_H264_480P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, YoutubeConverterFLVToM4AAudio.getInstance()),
    DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.MP3, YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, YoutubeConverterFLVToMP3Audio.getInstance()),
    FLV_H263_240P_30FPS_MP3_64KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.FLV, YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null),
    DEMUX_MP3_FLV_H263_240P_30FPS_MP3_64KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.MP3, YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, YoutubeConverterFLVToMP3Audio.getInstance()),
    THREEGP_H263_144P_15FPS_AMR_12KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.THREEGP, YoutubeITAG.THREEGP4_ITAG13_H263_144P_15FPS_AMRNB_12KBIT, null, null, null),
    MP4_H264_1080P_30FPS_AAC_192KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_1080P_30FPS_AAC_192KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null),
    DEMUX_AAC_MP4_H264_1080P_30FPS_AAC_192KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_1080P_30FPS_AAC_192KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_240P_30FPS_AAC_96KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null),
    DEMUX_AAC_MP4_H264_240P_30FPS_AAC_96KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_240P_30FPS_AAC_96KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_360P_30FPS_AAC_96KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_360P_30FPS_AAC_96KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null),
    DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_192KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_192KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_128KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_152KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_720P_30FPS_AAC_152KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null),
    DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_2160P_30FPS_AAC_192KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null),
    DEMUX_AAC_MP4_H264_2160P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_2160P_30FPS_AAC_192KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    THREEGP_H264_144P_30FPS_AAC_31KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.THREEGP, YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null),
    THREEGP_H263_240P_30FPS_AAC_32KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.THREEGP, YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null),
    THREEGP_H264_240P_30FPS_AAC_32KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.THREEGP, YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null),
    WEBM_VP8_1080P_30FPS_VORBIS_192KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null),
    WEBM_VP8_360P_30FPS_VORBIS_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null),
    WEBM_VP8_480P_30FPS_VORBIS_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null),
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT_3D(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null),
    MP4_H264_480P_24FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_480P_24FPS_AAC_128KBIT_2(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.MP4, YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null),
    DEMUX_AAC_MP4_H264_480P_24FPS_AAC_128KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.AAC, YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_MP4_H264_480P_24FPS_AAC_128KBIT_2(VariantGroup.AUDIO, DownloadType.VIDEO, FileContainer.M4A, YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    WEBM_VP8_720P_30FPS_VORBIS_192KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.WEBM, YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null),
    HLS_MP4_H264_72P_6FPS_AAC_24KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_72P_6FPS_AAC_24KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_72P_6FPS_AAC_24KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_240P_15FPS_AAC_48KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_15FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_240P_15FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_240P_30FPS_AAC_48KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_240P_30FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_240P_30FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_360P_30FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_360P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_360P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_480P_30FPS_AAC_128KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_480P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_480P_30FPS_AAC_128KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_720P_30FPS_AAC_256KBIT_2(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC_300, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    HLS_MP4_H264_1080P_30FPS_AAC_256KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_1080P_30FPS_AAC_256KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_1080P_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_1080P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    AAC_128KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    M4A_AAC_128KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1440P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_144P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_144P_15FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_2160P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1920P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_240P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_360P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_480P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_720P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_720P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_128KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_128KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_4320P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    OGG_VORBIS_128KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    OGG_VORBIS_192KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    MP4_H264_1080P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    AAC_256KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    M4A_AAC_256KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1440P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_144P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_144P_15FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_2160P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1920P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_240P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_360P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_480P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_720P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_720P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_4320P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1080P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    AAC_48KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    M4A_AAC_48KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1440P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_144P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_144P_15FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_2160P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1920P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_240P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_360P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_480P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_720P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_720P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_2160P_30FPS_AAC_48KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1080P_30FPS_AAC_48KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_4320P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    OGG_OPUS_160KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_144P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_240P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_360P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_480P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_720P_30FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_720P_60FPS_OPUS_160KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    OGG_OPUS_48KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_144P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_240P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_360P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_480P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_720P_30FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_720P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_1440P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    OGG_OPUS_64KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_1920P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_2160P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_1080P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_1080P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_1440P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_144P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_4320P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_2160P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_240P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_360P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_480P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_720P_30FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_720P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_1440P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    OGG_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.OGG, null, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_WORSE_PROFILE_1_1920P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_WORSE_PROFILE_1_2160P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_1920P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_2160P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_1080P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_1080P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_1440P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_144P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_4320P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_2160P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_240P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_360P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_480P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_BETTER_PROFILE_1_480P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_BETTER_PROFILE_2_480P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_720P_30FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_720P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    MP4_H264_1080P_60FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    AAC_AAC_SPATIAL_256KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    M4A_AAC_SPATIAL_256KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1440P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_144P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_144P_15FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_60FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_240P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_360P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_720P_60FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_720P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_4320P_30FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1280P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1280P_60FPS_AAC_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1280P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1280P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_128KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_1920P_60FPS_AAC_48KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    HLS_MP4_H264_144P_15FPS_AAC_48KBIT(VariantGroup.VIDEO, DownloadType.HLS_VIDEO, FileContainer.MP4, YoutubeITAG.HLS_VIDEO_MP4_144P_AUDIO_AAC, null, null, null),
    DEMUX_AAC_HLS_MP4_H264_144P_15FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.AAC, YoutubeITAG.HLS_VIDEO_MP4_144P_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()),
    DEMUX_M4A_HLS_MP4_H264_144P_15FPS_AAC_48KBIT(VariantGroup.AUDIO, DownloadType.HLS_VIDEO, FileContainer.M4A, YoutubeITAG.HLS_VIDEO_MP4_144P_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()),
    MP4_H264_1080P_60FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    AAC_AAC_SPATIAL_192KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    M4A_AAC_SPATIAL_192KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1440P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_144P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_144P_15FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_60FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_240P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_360P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_720P_60FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_720P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1280P_60FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_192KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_192KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_192KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_4320P_30FPS_AAC_SPATIAL_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_60FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    AAC_AAC_SPATIAL_384KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.AAC, null, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    M4A_AAC_SPATIAL_384KBIT_DASH(VariantGroup.AUDIO, DownloadType.DASH_AUDIO, FileContainer.M4A, null, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1440P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_144P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_144P_15FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_144P_H264_FPS15, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264_FPS_60, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_60FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_1920_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_240P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_360P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_720P_60FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_720P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1280P_60FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG304_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1920P_60FPS_AAC_SPATIAL_384KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ITAG305_MP4_1280P_60FPS, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_2160P_30FPS_AAC_SPATIAL_384KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_1080P_30FPS_AAC_SPATIAL_384KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_1080P, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_4320P_30FPS_AAC_SPATIAL_384KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    FLV_H263_270P_30FPS_MP3_64KBIT(VariantGroup.VIDEO, DownloadType.VIDEO, FileContainer.FLV, YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null),
    MP4_H264_480P_30FPS_AAC_128KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_256K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_192KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_192K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_SPATIAL_384KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_384K_AAC_SPATIAL, null, null),
    MP4_H264_480P_30FPS_AAC_256KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MP4_H264_480P_30FPS_AAC_48KBIT_DASH_2(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MP4, YoutubeITAG.DASH_VIDEO_480P_H264_2, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    WEBM_VP9_HDR_2160P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_1440P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_1080P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_720P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_480P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_360P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_240P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_144P_60FPS_VORBIS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null),
    WEBM_VP9_HDR_2160P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_1440P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_1080P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_720P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_480P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_360P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_240P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_144P_60FPS_VORBIS_192KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null),
    WEBM_VP9_HDR_2160P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_1440P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_1080P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_720P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_480P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_360P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_240P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_144P_60FPS_VORBIS_SPATIAL_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_256K_WEBM_SPATIAL, null, null),
    WEBM_VP9_HDR_2160P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_1440P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_1080P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_720P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_480P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_360P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_240P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_144P_60FPS_OPUS_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null),
    WEBM_VP9_HDR_2160P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_1440P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_1080P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_720P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_480P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_360P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_240P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_144P_60FPS_OPUS_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null),
    WEBM_VP9_HDR_2160P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_1440P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_1080P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_720P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_480P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_360P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_240P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    WEBM_VP9_HDR_144P_60FPS_OPUS_64KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.WEBM, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null),
    MKV_VP9_1440P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_1920P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_2160P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_1920P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_2160P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_2160P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_1440P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_1080P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_720P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_480P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_360P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_240P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_HDR_144P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_1080P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_1080P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_1440P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_144P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_4320P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_2160P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_240P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_360P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_480P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_1_480P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_2_480P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_720P_30FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_720P_60FPS_AAC_128KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null),
    MKV_VP9_1440P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_1920P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_2160P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_1920P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_2160P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_2160P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_1440P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_1080P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_720P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_480P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_360P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_240P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_HDR_144P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_1080P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_1080P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_1440P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_144P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_4320P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_2160P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_240P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_360P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_480P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_1_480P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_2_480P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_720P_30FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_720P_60FPS_AAC_256KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null),
    MKV_VP9_1440P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_1920P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_1920P_30FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_WORSE_PROFILE_1_2160P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_1920P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_1920P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_2160P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_2160P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_1440P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_1080P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_1080P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_720P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_720P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_480P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_480P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_360P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_360P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_240P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_240P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_HDR_144P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_VIDEO_VP9_144P_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_1080P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_1080P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_1440P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_144P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_4320P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_4320P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_2160P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_240P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_360P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_480P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_1_480P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_BETTER_PROFILE_2_480P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_720P_30FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null),
    MKV_VP9_720P_60FPS_AAC_48KBIT_DASH(VariantGroup.VIDEO, DownloadType.DASH_VIDEO, FileContainer.MKV, YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null)
    // ###APPEND###
    ;
    public static HashMap<String, VariantBase> COMPATIBILITY_MAP = new HashMap<String, VariantBase>();
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
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_360P_3D_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_360P_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_720P_3D_V1", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_128_720P_V1", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_152_720P_3D_V3", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_152_720P_V3", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_152KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_192_720P_3D_V4", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_AAC_192_720P_V4", DEMUX_AAC_MP4_H264_720P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_96_360P_3D_V1", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_AAC_96_360P_V1", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_360P_3D_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_360P_V4", DEMUX_AAC_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_720P_3D_V1", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_128_720P_V1", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_128KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_152_720P_3D_V3", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_152_720P_V3", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_152KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_192_720P_3D_V4", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT_2);
        COMPATIBILITY_MAP.put("DEMUX_M4A_192_720P_V4", DEMUX_M4A_MP4_H264_720P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_96_360P_3D_V1", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("DEMUX_M4A_96_360P_V1", DEMUX_M4A_MP4_H264_360P_30FPS_AAC_96KBIT_2);
        COMPATIBILITY_MAP.put("FLV_240_LOW", FLV_H263_240P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("FLV_270_HIGH", DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("FLV_360", FLV_H264_360P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("FLV_480", FLV_H264_480P_30FPS_AAC_128KBIT);
        COMPATIBILITY_MAP.put("M4A_128", M4A_AAC_128KBIT_DASH);
        COMPATIBILITY_MAP.put("M4A_256", M4A_AAC_256KBIT_DASH);
        COMPATIBILITY_MAP.put("M4A_48", M4A_AAC_48KBIT_DASH);
        COMPATIBILITY_MAP.put("MP3_1", DEMUX_MP3_FLV_H263_240P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("MP3_2", DEMUX_MP3_FLV_H263_270P_30FPS_MP3_64KBIT);
        COMPATIBILITY_MAP.put("MP4_1080", MP4_H264_1080P_30FPS_AAC_192KBIT);
        COMPATIBILITY_MAP.put("MP4_360", VariantBase.MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("MP4_360_AAC96", MP4_H264_360P_30FPS_AAC_96KBIT);
        COMPATIBILITY_MAP.put("MP4_3D_1080", MP4_H264_1080P_30FPS_AAC_192KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_3D_240", MP4_H264_240P_30FPS_AAC_96KBIT_3D);
        COMPATIBILITY_MAP.put("MP4_3D_360", VariantBase.MP4_H264_360P_30FPS_AAC_96KBIT_3D);
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
    private YoutubeConverter   converter;
    final private VariantGroup group;
    final private YoutubeITAG  iTagAudio;
    final private YoutubeITAG  iTagData;
    final private YoutubeITAG  iTagVideo;
    final private DownloadType type;
    private FileContainer      container;

    private VariantBase(final VariantGroup group, final DownloadType type, FileContainer container, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeConverter converter) {
        this.group = group;
        this.container = container;
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.iTagData = data;
        this.converter = converter;
    }

    public FileContainer getContainer() {
        return container;
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) throws Exception {
        if (converter != null) {
            converter.run(downloadLink, plugin);
        }
    }

    public VideoFrameRate getFrameRate(Object link) {
        return iTagVideo.getVideoFrameRate();
    }

    public VariantGroup getGroup() {
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

    public boolean isDemux() {
        return getiTagAudio() == null && getGroup() == VariantGroup.AUDIO;
    }

    // public double getQualityRating() {
    //
    // if (converter != null) {
    // return converter.getQualityRating(this, qualityRating);
    // }
    // if (isDemux()) {
    // // the higher the video overhead, the higher the penalty
    // double penaltyForDemux = getiTagVideo().getVideoResolution().getHeight() / 100000000d;
    // return getiTagVideo().getAudioCodec().getRating() + getiTagVideo().getAudioBitrate().getRating() - penaltyForDemux;
    // }
    // return this.qualityRating;
    // }
    public DownloadType getType() {
        return this.type;
    }

    public boolean hasConverter(DownloadLink downloadLink) {
        return converter != null;
    }

    public boolean isValidFor(YoutubeClipData vid) {
        switch (getGroup()) {
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

    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();
        return ret;
    }
    // public String modifyFileName(String formattedFilename, DownloadLink link) {
    // if (filenameModifier != null) {
    // return filenameModifier.run(formattedFilename, link);
    // }
    // return formattedFilename;
    // }

    // public static String getCompatibleVariantName(String variantName) {
    // VariantBase ret = COMPATIBILITY_MAP.get(variantName);
    // if (ret != null) {
    // return ret.name();
    // }
    // return variantName;
    // }
    public static VariantBase get(String variantName) {
        VariantBase ret = COMPATIBILITY_MAP.get(variantName);
        if (ret != null) {
            return ret;
        }
        try {
            return valueOf(variantName);
        } catch (Throwable e) {
            Log.log(e);
            return null;
        }
    }
}