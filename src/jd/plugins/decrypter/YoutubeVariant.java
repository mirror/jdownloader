package jd.plugins.decrypter;

import javax.swing.Icon;

import jd.plugins.DownloadLink;

import org.jdownloader.controlling.linkcrawler.LinkVariant;

public enum YoutubeVariant implements LinkVariant {
    SUBTITLES(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "SubRip Subtitle File", "Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, YoutubeSubtitleNamer.getInstance(), YoutubeSRTConverter.getInstance()),
    // SUBTITLES_AF(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Afrikaans) SubRip Subtitle File", "Afrikaans Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_AR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Arabic) SubRip Subtitle File", "Arabic Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_AZ(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Azerbaijani) SubRip Subtitle File", "Azerbaijani Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_BE(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Belarusian) SubRip Subtitle File", "Belarusian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_BG(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Bulgarian) SubRip Subtitle File", "Bulgarian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_BN(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Bengali) SubRip Subtitle File", "Bengali Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_BS(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Bosnian) SubRip Subtitle File", "Bosnian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_CA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Catalan) SubRip Subtitle File", "Catalan Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_CEB(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Cebuano) SubRip Subtitle File", "Cebuano Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_CS(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Czech) SubRip Subtitle File", "Czech Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_CY(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Welsh) SubRip Subtitle File", "Welsh Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_DA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Danish) SubRip Subtitle File", "Danish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_DE(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(German) SubRip Subtitle File", "German Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_EL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Greek) SubRip Subtitle File", "Greek Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_EN(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(English) SubRip Subtitle File", "English Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_EO(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Esperanto) SubRip Subtitle File", "Esperanto Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_ES(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Spanish) SubRip Subtitle File", "Spanish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_ES_MX(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Spanish - Mexico) SubRip Subtitle File",
    // "Spanish-Mexico Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    //
    // SUBTITLES_ET(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Estonian) SubRip Subtitle File", "Estonian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_EU(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Basque) SubRip Subtitle File", "Basque Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_FA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Persian) SubRip Subtitle File", "Persian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_FI(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Finnish) SubRip Subtitle File", "Finnish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_FIL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Filipino) SubRip Subtitle File", "Filipino Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_FR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(French) SubRip Subtitle File", "French Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_GA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Irish) SubRip Subtitle File", "Irish Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_GL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Galician) SubRip Subtitle File", "Galician Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_GU(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Gujarati) SubRip Subtitle File", "Gujarati Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_HI(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Hindi) SubRip Subtitle File", "Hindi Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_HMN(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Hmong) SubRip Subtitle File", "Hmong Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_HR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Croatian) SubRip Subtitle File", "Croatian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_HT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Haitian) SubRip Subtitle File", "Haitian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_HU(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Hungarian) SubRip Subtitle File", "Hungarian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_ID(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Indonesian) SubRip Subtitle File", "Indonesian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_IS(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Icelandic) SubRip Subtitle File", "Icelandic Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_IT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Italian) SubRip Subtitle File", "Italian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_IW(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Hebrew) SubRip Subtitle File", "Hebrew Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_JA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Japanese) SubRip Subtitle File", "Japanese Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_JV(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Javanese) SubRip Subtitle File", "Javanese Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_KA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Georgian) SubRip Subtitle File", "Georgian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_KM(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Khmer) SubRip Subtitle File", "Khmer Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_KN(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Kannada) SubRip Subtitle File", "Kannada Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_KO(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Korean) SubRip Subtitle File", "Korean Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_LA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Latin) SubRip Subtitle File", "Latin Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_LO(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Lao) SubRip Subtitle File", "Lao Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_LT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Lithuanian) SubRip Subtitle File", "Lithuanian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_LV(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Latvian) SubRip Subtitle File", "Latvian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_MK(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Macedonian) SubRip Subtitle File", "Macedonian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_MR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Marathi) SubRip Subtitle File", "Marathi Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_MS(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Malay) SubRip Subtitle File", "Malay Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_MT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Maltese) SubRip Subtitle File", "Maltese Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_NL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Dutch) SubRip Subtitle File", "Dutch Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_NO(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Norwegian) SubRip Subtitle File", "Norwegian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_PL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Polish) SubRip Subtitle File", "Polish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_PT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Portuguese) SubRip Subtitle File", "Portuguese Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_PT_BR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Portuguese - Brazil) SubRip Subtitle File",
    // "Portuguese-Brazil Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    //
    // SUBTITLES_RO(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Romanian) SubRip Subtitle File", "Romanian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_RU(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Russian) SubRip Subtitle File", "Russian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SK(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Slovak) SubRip Subtitle File", "Slovak Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SL(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Slovenian) SubRip Subtitle File", "Slovenian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SQ(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Albanian) SubRip Subtitle File", "Albanian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Serbian) SubRip Subtitle File", "Serbian Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SV(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Swedish) SubRip Subtitle File", "Swedish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_SW(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Swahili) SubRip Subtitle File", "Swahili Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_TA(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Tamil) SubRip Subtitle File", "Tamil Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_TE(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Telugu) SubRip Subtitle File", "Telugu Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_TH(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Thai) SubRip Subtitle File", "Thai Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_TR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Turkish) SubRip Subtitle File", "Turkish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_UK(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Ukrainian) SubRip Subtitle File", "Ukrainian Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_UR(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Urdu) SubRip Subtitle File", "Urdu Subtitles", "srt", null, null,
    // YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_VI(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Vietnamese) SubRip Subtitle File", "Vietnamese Subtitles", "srt",
    // null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_YI(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Yiddish) SubRip Subtitle File", "Yiddish Subtitles", "srt", null,
    // null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_ZH_HANS(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Simplified Chinese) SubRip Subtitle File",
    // "Simplified Chinese Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
    // SUBTITLES_ZH_HANT(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "(Traditional Chinese) SubRip Subtitle File",
    // "Traditional Chinese Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),

    IMAGE_MAX(null, VariantGroup.IMAGE, Type.IMAGE, "Best Quality Image", "BQ", "jpg", null, null, YoutubeITAG.IMAGE_MAX, null, null),
    IMAGE_HQ(null, VariantGroup.IMAGE, Type.IMAGE, "High Quality Image", "HQ", "jpg", null, null, YoutubeITAG.IMAGE_HQ, null, null),
    IMAGE_MQ(null, VariantGroup.IMAGE, Type.IMAGE, "Medium Quality Image", "MQ", "jpg", null, null, YoutubeITAG.IMAGE_MQ, null, null),
    IMAGE_LQ(null, VariantGroup.IMAGE, Type.IMAGE, "Low Quality Image", "LQ", "jpg", null, null, YoutubeITAG.IMAGE_LQ, null, null),
    AAC_128(null, VariantGroup.AUDIO, Type.DASH_AUDIO, "128kbit/s AAC-Audio", "128kbit", "aac", null, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    AAC_256(null, VariantGroup.AUDIO, Type.DASH_AUDIO, "256kbit/s AAC-Audio", "256kbit", "aac", null, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    AAC_48(null, VariantGroup.AUDIO, Type.DASH_AUDIO, "48kbit/s AAC-Audio", "48kbit", "aac", null, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),

    FLV_240_HIGH(null, VariantGroup.VIDEO, Type.VIDEO, "240p FLV-Video(high)", "240p[HQ]", "flv", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_240_LOW(null, VariantGroup.VIDEO, Type.VIDEO, "240p FLV-Video(low)", "240p[LQ]", "flv", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, null),
    FLV_360(null, VariantGroup.VIDEO, Type.VIDEO, "360p FLV-Video", "360p", "flv", YoutubeITAG.FLV_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    FLV_480(null, VariantGroup.VIDEO, Type.VIDEO, "480p FLV-Video", "480p", "flv", YoutubeITAG.FLV_VIDEO_480P_H264_AUDIO_AAC, null, null, null, null),

    MP3_1("MP3", VariantGroup.AUDIO, Type.FLV_TO_MP3, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            // slightly higher rating as MP3_2. audio quality is the same, but total size is less
            return YoutubeITAG.MP3_64 + 0.001;
        }
    },
    MP3_2("MP3", VariantGroup.AUDIO, Type.FLV_TO_MP3, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, YoutubeFlvToMp3Converter.getInstance()) {
        @Override
        public double getQualityRating() {
            return YoutubeITAG.MP3_64;
        }

    },
    MP4_1080("MP4_1080", VariantGroup.VIDEO, Type.VIDEO, "1080p MP4-Video", "1080p", "mp4", YoutubeITAG.MP4_VIDEO_1080P_H264_AUDIO_AAC, null, null, null, null),

    MP4_360("MP4_360", VariantGroup.VIDEO, Type.VIDEO, "360p MP4-Video", "360p", "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC, null, null, null, null),
    MP4_3D_240(null, VariantGroup.VIDEO_3D, Type.VIDEO, "240p MP4-3D-Video", "240p 3D", "mp4", YoutubeITAG.MP4_VIDEO_240P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_3D_360(null, VariantGroup.VIDEO_3D, Type.VIDEO, "360p MP4-3D-Video", "360p 3D", "mp4", YoutubeITAG.MP4_VIDEO_360P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_3D_520(null, VariantGroup.VIDEO_3D, Type.VIDEO, "520p MP4-3D-Video", "520p 3D", "mp4", YoutubeITAG.MP4_VIDEO_520P_H264_AUDIO_AAC_3D, null, null, null, null),

    MP4_3D_720(null, VariantGroup.VIDEO_3D, Type.VIDEO, "720p MP4-3D-Video", "720p 3D", "mp4", YoutubeITAG.MP4_VIDEO_520P_H264_AUDIO_AAC_3D, null, null, null, null),
    MP4_720("MP4_720", VariantGroup.VIDEO, Type.VIDEO, "720p MP4-Video", "720p", "mp4", YoutubeITAG.MP4_VIDEO_720P_H264_AUDIO_AAC, null, null, null, null),
    MP4_DASH_1080_AAC128("MP4_1080", VariantGroup.VIDEO, Type.DASH_VIDEO, "1080p MP4-Video(dash)", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_1080_AAC256("MP4_1080", VariantGroup.VIDEO, Type.DASH_VIDEO, "1080p MP4-Video(dash)", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_1080_AAC48("MP4_1080", VariantGroup.VIDEO, Type.DASH_VIDEO, "1080p MP4-Video(dash)", "1080p", "mp4", YoutubeITAG.DASH_VIDEO_1080P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_144_AAC128("MP4_144", VariantGroup.VIDEO, Type.DASH_VIDEO, "144p MP4-Video(dash)", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_144_AAC256("MP4_144", VariantGroup.VIDEO, Type.DASH_VIDEO, "144p MP4-Video(dash)", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_144_AAC48("MP4_144", VariantGroup.VIDEO, Type.DASH_VIDEO, "144p MP4-Video(dash)", "144p", "mp4", YoutubeITAG.DASH_VIDEO_144P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_1440_AAC128("MP4_1440", VariantGroup.VIDEO, Type.DASH_VIDEO, "1440p MP4-Video(dash)", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_1440_AAC256("MP4_1440", VariantGroup.VIDEO, Type.DASH_VIDEO, "1440p MP4-Video(dash)", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_1440_AAC48("MP4_1440", VariantGroup.VIDEO, Type.DASH_VIDEO, "1440p MP4-Video(dash)", "1440p", "mp4", YoutubeITAG.DASH_VIDEO_1440P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),

    MP4_DASH_240_AAC128("MP4_240", VariantGroup.VIDEO, Type.DASH_VIDEO, "240p MP4-Video(dash)", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_240_AAC256("MP4_240", VariantGroup.VIDEO, Type.DASH_VIDEO, "240p MP4-Video(dash)", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),

    MP4_DASH_240_AAC48("MP4_240", VariantGroup.VIDEO, Type.DASH_VIDEO, "240p MP4-Video(dash)", "240p", "mp4", YoutubeITAG.DASH_VIDEO_240P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_360_AAC128("MP4_360", VariantGroup.VIDEO, Type.DASH_VIDEO, "360p MP4-Video(dash)", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_360_AAC256("MP4_360", VariantGroup.VIDEO, Type.DASH_VIDEO, "360p MP4-Video(dash)", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_360_AAC48("MP4_360", VariantGroup.VIDEO, Type.DASH_VIDEO, "360p MP4-Video(dash)", "360p", "mp4", YoutubeITAG.DASH_VIDEO_360P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_480_AAC128("MP4_480", VariantGroup.VIDEO, Type.DASH_VIDEO, "480p MP4-Video(dash)", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_480_AAC256("MP4_480", VariantGroup.VIDEO, Type.DASH_VIDEO, "480p MP4-Video(dash)", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_480_AAC48("MP4_480", VariantGroup.VIDEO, Type.DASH_VIDEO, "480p MP4-Video(dash)", "480p", "mp4", YoutubeITAG.DASH_VIDEO_480P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_720_AAC128("MP4_720", VariantGroup.VIDEO, Type.DASH_VIDEO, "720p MP4-Video(dash)", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),

    MP4_DASH_720_AAC256("MP4_720", VariantGroup.VIDEO, Type.DASH_VIDEO, "720p MP4-Video(dash)", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_720_AAC48("MP4_720", VariantGroup.VIDEO, Type.DASH_VIDEO, "720p MP4-Video(dash)", "720p", "mp4", YoutubeITAG.DASH_VIDEO_720P_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC128("MP4_ORIGINAL", VariantGroup.VIDEO, Type.DASH_VIDEO, "2160p MP4-Video(dash)", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_128K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC256("MP4_ORIGINAL", VariantGroup.VIDEO, Type.DASH_VIDEO, "2160p MP4-Video(dash)", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_256K_AAC, null, null, null),
    MP4_DASH_ORIGINAL_AAC48("MP4_ORIGINAL", VariantGroup.VIDEO, Type.DASH_VIDEO, "2160p MP4-Video(dash)", "2160p", "mp4", YoutubeITAG.DASH_VIDEO_ORIGINAL_H264, YoutubeITAG.DASH_AUDIO_48K_AAC, null, null, null),
    MP4_ORIGINAL("MP4_ORIGINAL", VariantGroup.VIDEO, Type.VIDEO, "2160p MP4-Video", "2160p", "mp4", YoutubeITAG.MP4_VIDEO_AUDIO_ORIGINAL, null, null, null, null),
    THREEGP_144(null, VariantGroup.VIDEO, Type.VIDEO, "144p 3GP Video", "144p", "gp3", YoutubeITAG.THREEGP_VIDEO_144P_H264_AUDIO_AAC, null, null, null, null),
    THREEGP_240_HIGH(null, VariantGroup.VIDEO, Type.VIDEO, "240p 3GP Video(high)", "240p[HQ]", "gp3", YoutubeITAG.THREEGP_VIDEO_240P_H264_AUDIO_AAC, null, null, null, null),

    THREEGP_240_LOW(null, VariantGroup.VIDEO, Type.VIDEO, "240p 3GP Video(low)", "240p[LQ]", "gp3", YoutubeITAG.THREEGP_VIDEO_240P_H263_AUDIO_AAC, null, null, null, null),
    WEBM_1080(null, VariantGroup.VIDEO, Type.VIDEO, "1080p WebM-Video", "1080p", "webm", YoutubeITAG.WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_360(null, VariantGroup.VIDEO, Type.VIDEO, "360p WebM-Video", "360p", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_3D_360_128(null, VariantGroup.VIDEO_3D, Type.VIDEO, "360p WebM-3D-Video(128Kbit/s Audio)", "360p 3D [128kbit Audio]", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D, null, null, null, null),
    WEBM_3D_360_192(null, VariantGroup.VIDEO_3D, Type.VIDEO, "360p WebM-3D-Video(192Kbit/s Audio)", "360p 3D [192kbit Audio]", "webm", YoutubeITAG.WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_3D_720(null, VariantGroup.VIDEO_3D, Type.VIDEO, "720p WebM-3D-Video", "720p 3D", "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D, null, null, null, null),
    WEBM_480(null, VariantGroup.VIDEO, Type.VIDEO, "480p WebM-Video", "480p", "webm", YoutubeITAG.WEBM_VIDEO_480P_VP8_AUDIO_VORBIS, null, null, null, null),
    WEBM_720(null, VariantGroup.VIDEO, Type.VIDEO, "720p WebM-Video", "720p", "webm", YoutubeITAG.WEBM_VIDEO_720P_VP8_AUDIO_VORBIS, null, null, null, null);

    public static enum Type {
        DASH_AUDIO,
        DASH_VIDEO,
        FLV_TO_MP3,
        VIDEO,
        /**
         * Static videos have a static url in YT_STATIC_URL
         */
        IMAGE,
        SUBTITLES

    }

    public static enum VariantGroup {
        AUDIO,
        VIDEO,
        VIDEO_3D,
        IMAGE,
        SUBTITLES
    }

    final String                      fileExtension;
    final YoutubeVariant.VariantGroup group;
    final private String              id;
    final private YoutubeITAG         iTagAudio;
    final private YoutubeITAG         iTagVideo;
    final private String              name;
    final String                      qualityExtension;

    private double                    qualityRating;

    public void setQualityRating(double qualityRating) {
        this.qualityRating = qualityRating;
    }

    final private YoutubeVariant.Type type;
    final private YoutubeITAG         iTagData;
    private YoutubeConverter          converter;
    private YoutubeFilenameModifier   filenameModifier;

    private YoutubeVariant(final String id, final YoutubeVariant.VariantGroup group, final YoutubeVariant.Type type, final String name, final String qualityExtension, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, YoutubeFilenameModifier filenameModifier, YoutubeConverter converter) {
        this.group = group;
        this.id = id;
        this.name = name;
        this.qualityExtension = qualityExtension;
        this.fileExtension = fileExtension;
        if (type == null) throw new NullPointerException();
        this.type = type;
        this.iTagVideo = video;
        this.iTagAudio = audio;
        this.qualityRating = 0d + (video != null ? video.qualityRating : 0) + (audio != null ? audio.qualityRating : 0);
        this.iTagData = data;
        this.converter = converter;
        this.filenameModifier = filenameModifier;
    }

    public YoutubeITAG getiTagData() {
        return iTagData;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public YoutubeVariant.VariantGroup getGroup() {
        return this.group;
    }

    public Icon getIcon() {
        return null;
    }

    public String getId() {
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

    public YoutubeVariant.Type getType() {
        return this.type;
    }

    public void convert(DownloadLink downloadLink) {
        if (converter != null) converter.run(downloadLink);
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        if (filenameModifier != null) return filenameModifier.run(formattedFilename, link);
        return formattedFilename;
    }

}