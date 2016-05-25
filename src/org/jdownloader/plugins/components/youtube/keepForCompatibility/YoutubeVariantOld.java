package org.jdownloader.plugins.components.youtube.keepForCompatibility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverter;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToMP3Audio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterMP4ToAACAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeConverterMP4ToM4AAudio;
import org.jdownloader.plugins.components.youtube.converter.YoutubeSRTConverter;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.DownloadType;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public enum YoutubeVariantOld {
    AAC_128(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_128();
        }

    },

    AAC_256(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_256();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_256();
        }
    },
    AAC_48(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_48();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_48();
        }
    },
    DASH_AUDIO_OPUS_160KBIT(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {

            return _GUI.T.YoutubeVariant_name_generic_audio("160kbit", "Ogg");
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_audio("160kbit");
        }
    },
    DASH_AUDIO_OPUS_48KBIT(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_audio("48kbit", "Ogg");
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_audio("48kbit");
        }
    },
    DASH_AUDIO_OPUS_64KBIT(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "ogg", null, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_audio("64kbit", "Ogg");
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_audio("64kbit");
        }
    },
    DASH_VP9_1080P_30FPS_OPUS_160KBIT("1080P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p");
        }
    },
    DASH_VP9_1080P_30FPS_OPUS_48KBIT("1080P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p");
        }
    },
    DASH_VP9_1080P_30FPS_OPUS_64KBIT("1080P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p");
        }
    },
    DASH_VP9_1080P_60FPS_OPUS_160KBIT("1080P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    DASH_VP9_1080P_60FPS_OPUS_48KBIT("1080P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    DASH_VP9_1080P_60FPS_OPUS_64KBIT("1080P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    DASH_VP9_1440P_30FPS_OPUS_160KBIT("1440P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p");
        }
    },
    DASH_VP9_1440P_30FPS_OPUS_48KBIT("1440P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p");
        }
    },
    DASH_VP9_1440P_30FPS_OPUS_64KBIT("1440P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p");
        }
    },
    DASH_VP9_1440P_60FPS_OPUS_160KBIT("1440P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p 60fps");
        }
    },

    DASH_VP9_1440P_60FPS_OPUS_48KBIT("1440P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p 60fps");
        }
    },
    DASH_VP9_1440P_60FPS_OPUS_64KBIT("1440P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p 60fps");
        }
    },

    DASH_VP9_1440P_60FPS_VORBIS_128KBIT("1440P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p 60fps");
        }
    },

    DASH_VP9_1440P_60FPS_VORBIS_192KBIT("1440P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG308_VP9_1440P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1440p 60fps");
        }
    },
    DASH_VP9_144P_30FPS_OPUS_160KBIT("144P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("144p");
        }
    },

    DASH_VP9_144P_30FPS_OPUS_48KBIT("144P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("144p");
        }
    },
    DASH_VP9_144P_30FPS_OPUS_64KBIT("144P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("144p");
        }
    },
    DASH_VP9_2160P_30FPS_OPUS_160KBIT("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_VP9_2160P_30FPS_OPUS_160KBIT_2("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },

    DASH_VP9_2160P_30FPS_OPUS_48KBIT("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_VP9_2160P_30FPS_OPUS_48KBIT_2("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_VP9_2160P_30FPS_OPUS_64KBIT("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },

    DASH_VP9_2160P_30FPS_OPUS_64KBIT_2("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_VP9_2160P_30FPS_VORBIS_128KBIT("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_VP9_2160P_30FPS_VORBIS_192KBIT("2160P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG313_VP9_2160P_30FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },

    DASH_VP9_2160P_60FPS_OPUS_160KBIT("2160P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    DASH_VP9_2160P_60FPS_OPUS_48KBIT("2160P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    DASH_VP9_2160P_60FPS_OPUS_64KBIT("2160P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    DASH_VP9_2160P_60FPS_VORBIS_128KBIT("2160P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },

    DASH_VP9_2160P_60FPS_VORBIS_192KBIT("2160P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_VIDEO_ITAG315_VP9_2160P_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    DASH_VP9_240P_30FPS_OPUS_160KBIT("240P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p");
        }
    },
    DASH_VP9_240P_30FPS_OPUS_48KBIT("240P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p");
        }
    },

    DASH_VP9_240P_30FPS_OPUS_64KBIT("240P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p");
        }
    },

    DASH_VP9_360P_30FPS_OPUS_160KBIT("360P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p");
        }
    },
    DASH_VP9_360P_30FPS_OPUS_48KBIT("360P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p");
        }
    },
    DASH_VP9_360P_30FPS_OPUS_64KBIT("360P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_160KBIT("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_160KBIT_2("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },

    DASH_VP9_480P_30FPS_OPUS_160KBIT_3("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },

    DASH_VP9_480P_30FPS_OPUS_48KBIT("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_48KBIT_2("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_48KBIT_3("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_64KBIT("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_480P_30FPS_OPUS_64KBIT_2("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },

    DASH_VP9_480P_30FPS_OPUS_64KBIT_3("480P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    DASH_VP9_720P_30FPS_OPUS_160KBIT("720P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    DASH_VP9_720P_30FPS_OPUS_48KBIT("720P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },

    DASH_VP9_720P_30FPS_OPUS_64KBIT("720P_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    DASH_VP9_720P_60FPS_OPUS_160KBIT("720P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_160KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },
    DASH_VP9_720P_60FPS_OPUS_48KBIT("720P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_48KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },
    DASH_VP9_720P_60FPS_OPUS_64KBIT("720P_60fps_WebM", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_OPUS_64KBIT, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },

    DEMUX_AAC_128_720P_3D_V1("AAC_128", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_128();
        }

    },
    DEMUX_AAC_128_720P_V1("AAC_128", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_128();
        }

    },
    DEMUX_AAC_152_720P_3D_V3("AAC_152", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_152();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_152();
        }

    },
    DEMUX_AAC_152_720P_V3("AAC_152", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_152();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_152();
        }

    },
    DEMUX_AAC_192_720P_3D_V4("AAC_192", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_192();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_192();
        }

    },
    DEMUX_AAC_192_720P_V4("AAC_192", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_192();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_192();
        }

    },
    DEMUX_AAC_96_360P_3D_V1("AAC_96", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_96();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_96();
        }

    },
    DEMUX_AAC_96_360P_V1("AAC_96", VariantGroup.AUDIO, DownloadType.VIDEO, "aac", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToAACAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_AAC_96();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_AAC_96();
        }

    },

    DEMUX_M4A_128_720P_3D_V1("M4A_128", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_128();
        }

        // public double getQualityRating() {
        // // -0.0001 because loading DEMUX_M4A_128_360P_V4 would be better
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        // }
    },
    DEMUX_M4A_128_720P_V1("M4A_128", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_128();
        }

        // public double getQualityRating() {
        // // -0.0001 because loading DEMUX_M4A_128_360P_V4 would be better
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating() - 0.00000720;
        // }
    },
    DEMUX_M4A_152_720P_3D_V3("M4A_152", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D_V3, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_152();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_152();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        // }
    },
    DEMUX_M4A_152_720P_V3("M4A_152", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_152();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_152();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_152.getRating() - 0.00000720;
        // }
    },
    DEMUX_M4A_192_720P_3D_V4("M4A_192", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_192();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_192();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        // }
    },
    DEMUX_M4A_192_720P_V4("M4A_192", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_192();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_192();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_192.getRating() - 0.00000720;
        // }
    },

    DEMUX_M4A_96_360P_3D_V1("M4A_96", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_96();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_96();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        // }
    },
    DEMUX_M4A_96_360P_V1("M4A_96", VariantGroup.AUDIO, DownloadType.VIDEO, "m4a", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, YoutubeConverterMP4ToM4AAudio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_96();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_96();
        }
        //
        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_96.getRating() - 0.00000360;
        // }
    },
    FLV_240_LOW(null, VariantGroup.VIDEO, DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_FLV_240_LOW();
        }
    }, // the 2610 stream on itag 266 is deklared as 60fps stream in the backend, but it is actually just 30fps. maybe 60fps will come soon
    FLV_270_HIGH(null, VariantGroup.VIDEO, DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_FLV_270_HIGH();
        }
    }, // the 2610 stream on itag 266 is deklared as 60fps stream in the backend, but it is actually just 30fps. maybe 60fps will come soon
    FLV_360(null, VariantGroup.VIDEO, DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_FLV_360();
        }
    },
    FLV_480(null, VariantGroup.VIDEO, DownloadType.VIDEO, "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_FLV_480();
        }
    },
    IMAGE_HQ(null, VariantGroup.IMAGE, DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_HQ, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_HQ();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_HQ();
        }
    },
    IMAGE_LQ(null, VariantGroup.IMAGE, DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_LQ, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_LQ();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_LQ();
        }
    },
    IMAGE_MAX(null, VariantGroup.IMAGE, DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MAX, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_MAX();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MAX();
        }
    },
    IMAGE_MQ(null, VariantGroup.IMAGE, DownloadType.IMAGE, "jpg", null, null, YoutubeITAG.IMAGE_MQ, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_IMAGE_MQ();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_IMAGE_MQ();
        }
    },
    M4A_128(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_128();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_128();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_128.getRating();
        // }
    },
    M4A_256(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_256();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_256();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_256.getRating();
        // }
    },
    M4A_48(null, VariantGroup.AUDIO, DownloadType.DASH_AUDIO, "m4a", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_M4A_48();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_M4A_48();
        }

        // public double getQualityRating() {
        // return AudioCodec.M4A.getRating() + AudioBitrate.KBIT_48.getRating();
        // }
    },
    MP3_1("MP3_64KBit", VariantGroup.AUDIO, DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToMP3Audio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_MP3_1();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP3_1();
        }

        public List<File> listProcessFiles(DownloadLink link) {
            List<File> ret = super.listProcessFiles(link);
            File mp3 = new File(link.getFileOutputForPlugin(true, true));

            ret.add(new File(mp3 + ".avi"));
            ret.add(new File(mp3 + ".mp3"));
            ret.add(new File(mp3 + ".tmp"));
            ret.add(new File(mp3 + ".tmp.part"));
            return ret;
        }
    },
    MP3_2("MP3_64KBit", VariantGroup.AUDIO, DownloadType.VIDEO, "mp3", YoutubeITAG.FLV_VIDEO_HIGH_270P_H263_AUDIO_MP3, null, null, org.jdownloader.plugins.components.youtube.converter.YoutubeConverterFLVToMP3Audio.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_MP3_2();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP3_2();
        }

        public List<File> listProcessFiles(DownloadLink link) {
            List<File> ret = super.listProcessFiles(link);
            File mp3 = new File(link.getFileOutputForPlugin(true, true));

            ret.add(new File(mp3 + ".avi"));
            ret.add(new File(mp3 + ".mp3"));
            ret.add(new File(mp3 + ".tmp"));
            ret.add(new File(mp3 + ".tmp.part"));
            return ret;
        }

    },
    MP4_1080("MP4_1080", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_1080();
        }
    },

    MP4_360_AAC96("MP4_360", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_V1, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_360();
        }
    },

    MP4_3D_1080(null, VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_3D_1080();
        }
    },

    MP4_3D_240(null, VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_3D_240();
        }
    },

    MP4_3D_720(null, VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_3D_720();
        }
    },
    MP4_720("MP4_720", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_720();
        }
    },
    MP4_720_128AAC("MP4_720", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V1, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_720();
        }
    },

    MP4_720_152AAC("MP4_720", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC_V3, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_720();
        }
    },
    MP4_DASH_1080_AAC128("MP4_1080", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1080_AAC128();
        }
    },

    MP4_DASH_1080_AAC256("MP4_1080", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1080_AAC256();
        }
    },
    MP4_DASH_1080_AAC48("MP4_1080", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1080_AAC48();
        }
    },

    MP4_DASH_144_AAC128("MP4_144", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_144_AAC128();
        }
    },
    MP4_DASH_144_AAC256("MP4_144", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_144_AAC256();
        }
    },

    MP4_DASH_144_AAC48("MP4_144", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_144_AAC48();
        }
    },
    MP4_DASH_1440_AAC128("MP4_1440", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1440_AAC128();
        }
    },

    MP4_DASH_1440_AAC256("MP4_1440", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1440_AAC256();
        }
    },
    MP4_DASH_1440_AAC48("MP4_1440", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_1440_AAC48();
        }
    },

    MP4_DASH_240_AAC128("MP4_240", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_240_AAC128();
        }
    },
    MP4_DASH_240_AAC256("MP4_240", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_240_AAC256();
        }
    },
    MP4_DASH_240_AAC48("MP4_240", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_240_AAC48();
        }
    },
    MP4_DASH_360_AAC128("MP4_360", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_360_AAC128();
        }
    },
    MP4_DASH_360_AAC256("MP4_360", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_360_AAC256();
        }
    },
    MP4_DASH_360_AAC48("MP4_360", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_360_AAC48();
        }
    },
    MP4_DASH_480_AAC128("MP4_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_480_AAC128();
        }
    },
    MP4_DASH_480_AAC256("MP4_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_480_AAC256();
        }
    },

    MP4_DASH_480_AAC48("MP4_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_480_AAC48();
        }
    },
    MP4_DASH_720_AAC128("MP4_720", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_720_AAC128();
        }
    },

    MP4_DASH_720_AAC256("MP4_720", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_720_AAC256();
        }
    },
    MP4_DASH_720_AAC48("MP4_720", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_MP4_DASH_720_AAC48();
        }
    },
    MP4_DASH_ORIGINAL_AAC128("MP4_2160p4k8k", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("Original 2160p");
        }
    },

    MP4_DASH_ORIGINAL_AAC256("MP4_2160p4k8k", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("Original 2160p");
        }
    },
    MP4_DASH_ORIGINAL_AAC48("MP4_2160p4k8k", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_4K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("Original 2160p");
        }
    },

    MP4_DASH_VIDEO_1080_H264_FPS_60_AAC128("MP4_1080p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    MP4_DASH_VIDEO_1080_H264_FPS_60_AAC256("MP4_1080p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    MP4_DASH_VIDEO_1080_H264_FPS_60_AAC48("MP4_1080p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_1080_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p 60fps");
        }
    },
    // the 2610 stream on itag 266 is deklared as 60fps stream in the backend, but it is actually just 30fps. maybe 60fps will come soon on
    // the same itag?
    MP4_DASH_VIDEO_2160_H264_FPS_60_AAC128("MP4_2160p", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    // on the same itag?
    MP4_DASH_VIDEO_2160_H264_FPS_60_AAC256("MP4_2160p", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    // on the same itag?
    MP4_DASH_VIDEO_2160_H264_FPS_60_AAC48("MP4_2160p", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264_FPS_60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p 60fps");
        }
    },
    MP4_DASH_VIDEO_720_H264_FPS_60_AAC128("MP4_720p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },
    MP4_DASH_VIDEO_720_H264_FPS_60_AAC256("MP4_720p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },
    MP4_DASH_VIDEO_720_H264_FPS_60_AAC48("MP4_720p_60fps", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_720_H264_FPS60, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p 60fps");
        }
    },
    MP4_ORIGINAL("MP4_ORIGINAL", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    SUBTITLES(null, VariantGroup.SUBTITLES, DownloadType.SUBTITLES, "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSRTConverter.getInstance()) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_SUBTITLES();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_SUBTITLES();
        }

    },
    DESCRIPTION(null, VariantGroup.DESCRIPTION, DownloadType.DESCRIPTION, "txt", null, null, YoutubeITAG.DESCRIPTION, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_DESCRIPTION();
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_DESCRIPTION();
        }

    },
    THREEGP_144(null, VariantGroup.VIDEO, DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {

            return _GUI.T.YoutubeVariant_nametag_generic_video("144p");
        }
    },
    THREEGP_240_HIGH(null, VariantGroup.VIDEO, DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p[HQ]");

        }
    },
    THREEGP_240_LOW(null, VariantGroup.VIDEO, DownloadType.VIDEO, "3gp", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p[LQ]");

        }
    },
    WEBM_1080(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("1080p");

        }
    },
    WEBM_360(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p");
        }
    },
    WEBM_3D_360_128(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p 3D");
        }
    },
    WEBM_3D_360_192(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p 3D");
        }
    },
    WEBM_3D_720(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    WEBM_480(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    WEBM_720(null, VariantGroup.VIDEO, DownloadType.VIDEO, "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    WEBM_DASH_1080_60FPS_VORBIS128("WEBM_1080_60FPS", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1080_60FPS();
        }
    },
    WEBM_DASH_1080_60FPS_VORBIS192("WEBM_1080_60FPS", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1080_60FPS();
        }
    },
    WEBM_DASH_1080_VORBIS128("WEBM_1080", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1080();
        }
    },
    WEBM_DASH_1080_VORBIS192("WEBM_1080", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1080P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1080();
        }
    },
    WEBM_DASH_144_VORBIS128("WEBM_144", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_144();
        }
    },
    WEBM_DASH_144_VORBIS192("WEBM_144", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_144P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_144();
        }
    },
    WEBM_DASH_1440_VORBIS128("WEBM_1440", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1440();
        }
    },
    WEBM_DASH_1440_VORBIS192("WEBM_1440", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_1440P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_1440();
        }
    },
    WEBM_DASH_2160_VORBIS128("WEBM_2160", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_2160();
        }
    },
    WEBM_DASH_2160_VORBIS192("WEBM_2160", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_2160P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_2160();
        }
    },
    WEBM_DASH_240_VORBIS128("WEBM_240", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_240();
        }
    },
    WEBM_DASH_240_VORBIS192("WEBM_240", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_240P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_240();
        }
    },
    WEBM_DASH_360_VORBIS128("WEBM_360", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_360();
        }
    },
    WEBM_DASH_360_VORBIS192("WEBM_360", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_360P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_360();
        }
    },
    WEBM_DASH_480_VORBIS128_1("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_480_VORBIS128_2("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_480_VORBIS128_3("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_480_VORBIS192_1("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_480_VORBIS192_2("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_2, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_480_VORBIS192_3("WEBM_480", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_480P_VP9_3, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_480();
        }
    },
    WEBM_DASH_720_60FPS_VORBIS128("WEBM_720_60FPS", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_720_60FPS();
        }
    },
    WEBM_DASH_720_60FPS_VORBIS192("WEBM_720_60FPS", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9_60FPS, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_filenametag_WEBM_720_60FPS();
        }
    },
    WEBM_DASH_720_VORBIS128("WEBM_720", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_128K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    WEBM_DASH_720_VORBIS192("WEBM_720", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "webm", YoutubeITAG.DASH_WEBM_VIDEO_720P_VP9, YoutubeITAG.DASH_AUDIO_192K_WEBM, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },

    DASH_H264_2160P_30FPS_AAC_128KBIT("2160P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_H264_2160P_30FPS_AAC_256KBIT("2160P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_H264_2160P_30FPS_AAC_48KBIT("2160P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_2160_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("2160p");
        }
    },
    DASH_H264_4320P_24FPS_AAC_128KBIT("4320P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("4320p 24fps");
        }
    },
    DASH_H264_4320P_24FPS_AAC_256KBIT("4320P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("4320p 24fps");
        }
    },
    DASH_H264_4320P_24FPS_AAC_48KBIT("4320P_Mp4", VariantGroup.VIDEO, DownloadType.DASH_VIDEO, "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264_GENERIC_8K, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("4320p 24fps");
        }

    },

    H264_480P_24FPS_AAC_128KBIT("480P_Mp4", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG59_H264_480P_24FPS_AAC_128KBIT, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p 24fps");
        }
    },
    H264_480P_24FPS_AAC_128KBIT_2("480P_Mp4", VariantGroup.VIDEO, DownloadType.VIDEO, "mp4", YoutubeITAG.MP4_ITAG78_H264_480P_24FPS_AAC_128KBIT, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p 24fps");
        }
    } // hls
    ,
    HLS_MP4_720("MP4_720", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_720P_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("720p");
        }
    },
    HLS_MP4_480("MP4_480", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_480P_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("480p");
        }
    },
    HLS_MP4_360("MP4_360", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_360P_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("360p");
        }
    },
    HLS_MP4_240("MP4_240", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_15FPS_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p");
        }
    },
    HLS_MP4_240_2("MP4_240", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_240P_AUDIO_AAC_2, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("240p");
        }
    },
    HLS_MP4_72("MP4_72", VariantGroup.VIDEO, DownloadType.HLS_VIDEO, "mp4", YoutubeITAG.HLS_VIDEO_MP4_72P_6FPS_AUDIO_AAC, null, null, null) {

        public String _getName(Object caller) {
            return _GUI.T.YoutubeVariant_name_generic_video2(getQualityExtension(null), getVideoCodec(), getAudioQuality(), getAudioCodec());
        }

        public String getQualityExtension(Object caller) {
            return _GUI.T.YoutubeVariant_nametag_generic_video("72p");
        }
    };

    private YoutubeConverter   converter;
    final private String       fileExtension;

    final private VariantGroup group;
    final private String       id;

    final private YoutubeITAG  iTagAudio;

    final private YoutubeITAG  iTagData;
    final private YoutubeITAG  iTagVideo;

    final private DownloadType type;

    private YoutubeVariantOld(final String id, final VariantGroup group, final DownloadType type, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeConverter converter) {
        this.group = group;
        this.id = id;

        this.fileExtension = fileExtension;
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.iTagData = data;
        this.converter = converter;

    }

    public String _getExtendedName(Object caller) {

        return _getName(caller) + " - ";
    }

    private static final Icon VIDEO = new AbstractIcon(IconKey.ICON_VIDEO, 16);
    private static final Icon AUDIO = new AbstractIcon(IconKey.ICON_AUDIO, 16);
    private static final Icon IMAGE = new AbstractIcon(IconKey.ICON_IMAGE, 16);
    private static final Icon TEXT  = new AbstractIcon(IconKey.ICON_TEXT, 16);

    public Icon _getIcon(Object caller) {
        final VariantGroup lGroup = getGroup();
        if (lGroup != null) {
            switch (lGroup) {
            case AUDIO:
                return AUDIO;
            case VIDEO:

                return VIDEO;
            case IMAGE:
                return IMAGE;
            default:
                return TEXT;
            }
        }
        return null;
    }

    public abstract String _getName(Object caller);

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
            return getiTagAudio().getAudioCodec().getLabel();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getAudioCodec().getLabel();
        }
        return null;
    }

    public String getAudioQuality() {
        if (getiTagAudio() != null) {
            return getiTagAudio().getAudioBitrate().getLabel();
        }
        if (getiTagVideo() != null) {
            return getiTagVideo().getAudioBitrate().getLabel();
        }
        return null;
    }

    public String getFileExtension() {
        return this.fileExtension;
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

    public String getMediaTypeID() {
        return getGroup().name();
    }

    public abstract String getQualityExtension(Object caller);

    public String getResolution() {
        String ret = null;
        if (getiTagData() != null) {

            ret = getiTagData().getVideoResolution().getLabel();
        }
        if (getiTagVideo() != null) {
            ret = getiTagVideo().getVideoResolution().getLabel();
        }
        return ret;
    }

    public DownloadType getType() {
        return this.type;
    }

    public String getTypeId() {

        if (this.id == null) {
            return this.name();
        }
        return this.id;
    }

    public String getVideoCodec() {
        String ret = null;
        if (getiTagData() != null) {

            ret = getiTagData().getVideoCodec().getLabel();
        }
        if (getiTagVideo() != null) {
            ret = getiTagVideo().getVideoCodec().getLabel();
        }
        return ret;
    }

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
            if (converter instanceof YoutubeConverterMP4ToAACAudio) {
                return true;
            }
            if (converter instanceof YoutubeConverterMP4ToM4AAudio) {
                return true;
            }
            if (converter instanceof YoutubeConverterFLVToMP3Audio) {
                return true;
            }

        }
        return false;
    }

    public String _getTooltipDescription(Object caller) {
        return _getExtendedName(caller) + " [" + name() + "]";
    }

    public List<File> listProcessFiles(DownloadLink link) {
        ArrayList<File> ret = new ArrayList<File>();

        return ret;
    }

    public boolean isValidFor(YoutubeClipData vid) {
        return false;
    }

    // public String modifyFileName(String formattedFilename, DownloadLink link) {
    // if (filenameModifier != null) {
    // return filenameModifier.run(formattedFilename, link);
    // }
    // return formattedFilename;
    // }

    public String getStorableString() {
        return null;
    }

    public String getId() {
        if (id == null) {
            return name();
        }
        return id;
    }

}