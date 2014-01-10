package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.hoster.YoutubeDashV2.YoutubeConfig;
import jd.utils.locale.JDL;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.utils.SubtitleConverter;

import de.savemytube.flv.FLV;

public class YoutubeHelper {

    public static enum YoutubeITAG {
        // fake id
        SUBTITLE(10002, 0.1),
        // fake id
        IMAGE_MAX(10001, 0.4),
        // fake id
        IMAGE_HQ(10002, 0.3),
        // fake id
        IMAGE_MQ(10003, 0.2),
        // fake id
        IMAGE_LQ(10004, 0.1),
        DASH_AUDIO_128K_AAC(140, YoutubeHelper.AAC_128),
        DASH_AUDIO_128K_WEBM(171, YoutubeHelper.VORBIS_128),
        DASH_AUDIO_192K_WEBM(172, YoutubeHelper.VORBIS_192),
        DASH_AUDIO_256K_AAC(141, YoutubeHelper.AAC_256),
        DASH_AUDIO_48K_AAC(139, YoutubeHelper.AAC_48),
        DASH_VIDEO_1080P_H264(137, 1080.4),
        // http://www.youtube.com/watch?v=gBabKoHSErI
        DASH_VIDEO_1440P_H264(264, 1440.4),
        DASH_VIDEO_144P_H264(160, 144.4),

        DASH_VIDEO_240P_H264(133, 240.4),
        DASH_VIDEO_360P_H264(134, 360.4),
        DASH_VIDEO_480P_H264(135, 480.4),
        DASH_VIDEO_720P_H264(136, 720.4),
        DASH_VIDEO_ORIGINAL_H264(138, 2160.4),
        DASH_WEBM_VIDEO_1080P_VP9(248, 1080.3),
        DASH_WEBM_VIDEO_720P_VP9(247, 720.3),
        DASH_WEBM_VIDEO_480P_VP9(244, 480.3),
        DASH_WEBM_VIDEO_360P_VP9(243, 360.3),
        DASH_WEBM_VIDEO_240P_VP9(242, 240.3),

        FLV_VIDEO_360P_H264_AUDIO_AAC(34, 360.1d),
        FLV_VIDEO_480P_H264_AUDIO_AAC(35, 480.1d),
        FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3(6, 240.11d + YoutubeHelper.MP3_64),

        FLV_VIDEO_LOW_240P_H263_AUDIO_MP3(5, 240.10d + YoutubeHelper.MP3_64),

        // 192 kbits aac
        MP4_VIDEO_1080P_H264_AUDIO_AAC(37, 1080.4 + YoutubeHelper.AAC_192),
        // not sure
        MP4_VIDEO_240P_H264_AUDIO_AAC_3D(83, 240.4 + YoutubeHelper.AAC_64),
        MP4_VIDEO_360P_H264_AUDIO_AAC(18, 360.4 + YoutubeHelper.AAC_128),
        MP4_VIDEO_360P_H264_AUDIO_AAC_3D(82, 360.4 + YoutubeHelper.AAC_128),

        MP4_VIDEO_520P_H264_AUDIO_AAC_3D(856, 520.4 + YoutubeHelper.AAC_128),
        // 192 kbits aac
        MP4_VIDEO_720P_H264_AUDIO_AAC(22, 720.4 + YoutubeHelper.AAC_192),
        MP4_VIDEO_720P_H264_AUDIO_AAC_3D(84, 720.4 + YoutubeHelper.AAC_192),

        // http://www.h3xed.com/web-and-internet/youtube-audio-quality-bitrate-240p-360p-480p-720p-1080p
        MP4_VIDEO_AUDIO_ORIGINAL(38, 2160.4 + YoutubeHelper.AAC_192),
        // very different audio bitrates!!!
        THREEGP_VIDEO_144P_H264_AUDIO_AAC(17, 144.0 + YoutubeHelper.AAC32_ESTIMATE),
        THREEGP_VIDEO_240P_H263_AUDIO_AAC(132, 240.0 + YoutubeHelper.AAC_48_ESTIMATE),
        THREEGP_VIDEO_240P_H264_AUDIO_AAC(36, 240.01 + YoutubeHelper.AAC_48_ESTIMATE),

        // not sure - did not find testvideos
        WEBM_VIDEO_1080P_VP8_AUDIO_VORBIS(46, 1080.3 + YoutubeHelper.VORBIS_192),

        WEBM_VIDEO_360P_VP8_AUDIO_128K_VORBIS_3D(100, 360.3 + YoutubeHelper.VORBIS_128),
        WEBM_VIDEO_360P_VP8_AUDIO_192K_VORBIS_3D(101, 360.3 + YoutubeHelper.VORBIS_192),

        WEBM_VIDEO_360P_VP8_AUDIO_VORBIS(43, 360.3 + YoutubeHelper.VORBIS_128),
        // not sure - did not find testvideos
        WEBM_VIDEO_480P_VP8_AUDIO_VORBIS(44, 480.3 + YoutubeHelper.VORBIS_128),
        WEBM_VIDEO_720P_VP8_AUDIO_192K_VORBIS_3D(102, 360.3 + YoutubeHelper.VORBIS_192),
        // not sure - did not find testvideos
        WEBM_VIDEO_720P_VP8_AUDIO_VORBIS(45, 720.3 + YoutubeHelper.VORBIS_192);

        public static YoutubeITAG get(final int itag) {
            for (final YoutubeITAG tag : YoutubeITAG.values()) {
                if (tag.getITAG() == itag) { return tag; }
            }
            return null;
        }

        private final int itag;

        double            qualityRating;

        private YoutubeITAG(final int itag, final double quality) {
            this.itag = itag;
            this.qualityRating = quality;

        }

        public int getITAG() {
            return this.itag;
        }

    }

    public static class StreamData {

        private ClipData clip;

        public void setClip(ClipData clip) {
            this.clip = clip;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setItag(YoutubeITAG itag) {
            this.itag = itag;
        }

        private String url;

        public ClipData getClip() {
            return clip;
        }

        public String getUrl() {
            return url;
        }

        public YoutubeITAG getItag() {
            return itag;
        }

        private YoutubeITAG itag;

        public StreamData(final ClipData vid, String url, YoutubeITAG itag) {
            this.clip = vid;
            this.itag = itag;
            this.url = url;

        }
    }

    public interface FilenameModifier {

        String run(String formattedFilename, DownloadLink link);

    }

    public interface Converter {

        void run(DownloadLink downloadLink);

    }

    public static class SubtitleNamer implements FilenameModifier {

        @Override
        public String run(String formattedFilename, DownloadLink link) {
            String code = link.getStringProperty(YT_SUBTITLE_CODE, "");
            Locale locale = Locale.forLanguageTag(code);
            formattedFilename = formattedFilename.replaceAll("\\*quality\\*", _GUI._.YoutubeDash_getName_subtitles_filename(locale.getDisplayName()));
            return formattedFilename;
        }
    }

    public static class SRTConverter implements Converter {

        @Override
        public void run(DownloadLink downloadLink) {

            try {
                downloadLink.setPluginProgress(new PluginProgress(0, 100, null) {
                    {
                        setIcon(new AbstractIcon(IconKey.ICON_TEXT, 18));

                    }

                    @Override
                    public long getCurrent() {
                        return 95;
                    }

                    @Override
                    public Icon getIcon(Object requestor) {
                        if (requestor instanceof ETAColumn) return null;
                        return super.getIcon(requestor);
                    }

                    @Override
                    public String getMessage(Object requestor) {
                        if (requestor instanceof ETAColumn) return "";
                        return "Convert";
                    }
                });
                File file = new File(downloadLink.getFileOutput());

                SubtitleConverter.convertGoogleCC2SRTSubtitles(file, new File(file.getAbsolutePath().replaceFirst("\\.srt\\.tmp$", ".srt")));

                try {
                    // downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                    downloadLink.setCustomFileOutputFilenameAppend(null);
                    downloadLink.setCustomFileOutputFilename(null);
                } catch (final Throwable e) {
                }
            } finally {
                downloadLink.setPluginProgress(null);
            }

        }

    }

    public static class FlvToMp3Converter implements Converter {

        @Override
        public void run(DownloadLink downloadLink) {

            YoutubeHelper.convertToMp3(downloadLink);

        }

    }

    public static final SubtitleNamer SUBTITLE_RENAMER     = new SubtitleNamer();
    public static final SRTConverter  XML_TO_SRT_CONVERTER = new SRTConverter();

    public enum YoutubeVariant implements LinkVariant {
        SUBTITLES(null, VariantGroup.SUBTITLES, Type.SUBTITLES, "SubRip Subtitle File", "Subtitles", "srt", null, null, YoutubeITAG.SUBTITLE, SUBTITLE_RENAMER, XML_TO_SRT_CONVERTER),
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

        MP3_1("MP3", VariantGroup.AUDIO, Type.FLV_TO_MP3, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_LOW_240P_H263_AUDIO_MP3, null, null, null, new FlvToMp3Converter()) {
            @Override
            public double getQualityRating() {
                // slightly higher rating as MP3_2. audio quality is the same, but total size is less
                return YoutubeHelper.MP3_64 + 0.001;
            }
        },
        MP3_2("MP3", VariantGroup.AUDIO, Type.FLV_TO_MP3, "64kbit/s Mp3-Audio", "64kbit", "mp3", YoutubeITAG.FLV_VIDEO_HIGH_240P_H263_AUDIO_MP3, null, null, null, new FlvToMp3Converter()) {
            @Override
            public double getQualityRating() {
                return YoutubeHelper.MP3_64;
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

        final String              fileExtension;
        final VariantGroup        group;
        final private String      id;
        final private YoutubeITAG iTagAudio;
        final private YoutubeITAG iTagVideo;
        final private String      name;
        final String              qualityExtension;

        private double            qualityRating;

        public void setQualityRating(double qualityRating) {
            this.qualityRating = qualityRating;
        }

        final private Type        type;
        final private YoutubeITAG iTagData;
        private Converter         converter;
        private FilenameModifier  filenameModifier;

        private YoutubeVariant(final String id, final VariantGroup group, final Type type, final String name, final String qualityExtension, final String fileExtension, final YoutubeITAG video, final YoutubeITAG audio, YoutubeITAG data, FilenameModifier filenameModifier, Converter converter) {
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

        public VariantGroup getGroup() {
            return this.group;
        }

        @Override
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

        @Override
        public String getName() {
            return this.name;
        }

        public String getQualityExtension() {
            return this.qualityExtension;
        }

        public double getQualityRating() {
            return this.qualityRating;
        }

        public Type getType() {
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

    public static class ClipData {

        /**
         * 
         */

        public String  user;
        public String  channel;
        public long    date;
        public String  error;
        public boolean ageCheck;
        public String  title;
        public String  videoID;
        int            playlistEntryNumber;
        public int     length;

        public ClipData(final String videoID) {
            this(videoID, -1);
        }

        @Override
        public String toString() {
            return videoID + "/" + title;
        }

        public ClipData(final String videoID, final int playlistEntryNumber) {

            this.videoID = videoID;
            this.playlistEntryNumber = playlistEntryNumber;
        }

    }

    public static final double                 AAC_128            = 0.1284;
    public static final double                 AAC_192            = 0.1924;
    public static final double                 AAC_256            = 0.2564;
    public static final double                 AAC_48             = 0.0484;
    public static final double                 AAC_48_ESTIMATE    = 0.0474;

    public static final double                 AAC_64             = 0.0644;

    public static final double                 AAC32_ESTIMATE     = 0.0324;

    // mp3 64 bit is lower than aac48bit
    public static final double                 MP3_64             = 0.0442;

    public static final double                 VORBIS_128         = 0.1283;

    public static final double                 VORBIS_192         = 0.1923;

    public static final double                 VORBIS_96          = 0.0963;

    private final Browser                      br;
    private final YoutubeConfig                cfg;

    private final LogSource                    logger;
    private String                             base;

    public static final String                 YT_EXT             = "YT_EXT";
    public static final String                 YT_TITLE           = "YT_TITLE";
    public static final String                 YT_PLAYLIST_INT    = "YT_PLAYLIST_INT";
    public static final String                 YT_ID              = "YT_ID";
    public static final String                 YT_AGE_GATE        = "YT_AGE_GATE";
    public static final String                 YT_CHANNEL         = "YT_CHANNEL";
    public static final String                 YT_USER            = "YT_USER";
    public static final String                 YT_DATE            = "YT_DATE";
    public static final String                 YT_VARIANTS        = "YT_VARIANTS";
    public static final String                 YT_VARIANT         = "YT_VARIANT";
    public static final String                 YT_STREAMURL_VIDEO = "YT_STREAMURL_VIDEO";
    public static final String                 YT_STREAMURL_AUDIO = "YT_STREAMURL_AUDIO";

    private static WeakHashMap<String, String> JS_CACHE           = new WeakHashMap<String, String>();

    private static String handleRule(String s, final String line) throws PluginException {

        final String method = new Regex(line, "\\.([\\w\\d]+?)\\(\\s*\\)").getMatch(0);
        if ("reverse".equals(method)) {
            //
            s = new StringBuilder(s).reverse().toString();

            return s;
        }
        // slice
        final String i = new Regex(line, "\\.slice\\((\\d+)\\)").getMatch(0);
        if (i != null) {
            //
            s = s.substring(Integer.parseInt(i));

            return s;
        }

        final String idx = new Regex(line, "=..\\([^,]+\\,(\\d+)\\)").getMatch(0);
        if (idx != null) {
            final int idxI = Integer.parseInt(idx);
            s = YoutubeHelper.pk(s, idxI);

            return s;

        }

        if (new Regex(line, "\\.split\\(\"\"\\)").matches()) { return s; }
        if (new Regex(line, "\\.join\\(\"\"\\)").matches()) { return s; }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Signature Rule: " + line);

    }

    protected static void convertToMp3(DownloadLink downloadLink) {
        try {
            downloadLink.setPluginProgress(new PluginProgress(0, 100, null) {
                {
                    setIcon(new AbstractIcon(IconKey.ICON_AUDIO, 18));

                }

                @Override
                public long getCurrent() {
                    return 95;
                }

                @Override
                public Icon getIcon(Object requestor) {
                    if (requestor instanceof ETAColumn) return null;
                    return super.getIcon(requestor);
                }

                @Override
                public String getMessage(Object requestor) {
                    if (requestor instanceof ETAColumn) return "";
                    return "Create Mp3";
                }
            });
            File file = new File(downloadLink.getFileOutput());

            new FLV(downloadLink.getFileOutput(), true, true);

            file.delete();
            File finalFile = new File(downloadLink.getFileOutput().replaceAll(".tmp$", ""));
            finalFile.delete();
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".mp3")).renameTo(finalFile);
            new File(downloadLink.getFileOutput().replaceAll(".tmp$", ".avi")).delete();
            downloadLink.setDownloadSize(finalFile.length());
            downloadLink.setDownloadCurrent(finalFile.length());
            try {
                downloadLink.setFinalFileOutput(finalFile.getAbsolutePath());
                downloadLink.setCustomFileOutputFilenameAppend(null);
                downloadLink.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
        } finally {
            downloadLink.setPluginProgress(null);
        }
    }

    protected static String pk(final String s, final int idxI) {
        final char c = s.charAt(0);
        final StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(idxI % s.length()));
        sb.append(s.substring(1, idxI));
        sb.append(c);
        sb.append(s.substring(idxI + 1));
        return sb.toString();
    }

    public YoutubeHelper(final Browser br, final YoutubeConfig cfg, final LogSource logger) {
        this.br = br;
        this.logger = logger;

        this.cfg = cfg;

        if (cfg.isPreferHttpsEnabled()) {
            this.base = "https://www.youtube.com";
        } else {
            this.base = "http://www.youtube.com";
        }
    }

    /**
     * *
     * 
     * @param br
     * 
     * @param s
     * @return
     * @throws IOException
     * @throws PluginException
     */
    String descrambleSignature(final String sig) throws IOException, PluginException {
        if (sig == null) { return null; }

        String jsUrl = this.br.getMatch("\"js\"\\: \"(.+?)\"");
        jsUrl = jsUrl.replace("\\/", "/");
        jsUrl = "http:" + jsUrl;

        String js = YoutubeHelper.JS_CACHE.get(jsUrl);
        if (js == null) {
            js = this.br.cloneBrowser().getPage(jsUrl);
            YoutubeHelper.JS_CACHE.put(jsUrl, js);
        }
        final String descrambler = new Regex(js, "\\w+\\.signature\\=([\\w\\d]+)\\([\\w\\d]+\\)").getMatch(0);

        final String func = "function " + descrambler + "\\(([^)]+)\\)\\{(.+?return.*?)\\}";
        final String des = new Regex(js, Pattern.compile(func)).getMatch(1);
        String s = sig;
        // Debug code.
        // Context cx = null;
        // Object result = null;
        // try {
        //
        // try {
        // cx = ContextFactory.getGlobal().enterContext();
        //
        // } catch (java.lang.SecurityException e) {
        // /* in case classshutter already set */
        // }
        // Scriptable scope = cx.initStandardObjects();
        // String all = new Regex(js, Pattern.compile("function " + descrambler +
        // "\\(([^)]+)\\)\\{(.+?return.*?)\\}.*?\\{.*?\\}")).getMatch(-1);
        // result = cx.evaluateString(scope, all + " " + descrambler + "(\"" + sig + "\")", "<cmd>", 1, null);
        //
        // } finally {
        // try {
        // Context.exit();
        // } catch (final Throwable e) {
        // }
        // }
        for (final String line : new Regex(des, "[^;]+").getColumn(-1)) {

            s = YoutubeHelper.handleRule(s, line);
        }

        return s;

    }

    protected void extractData(final ClipData vid) {
        if (StringUtils.isEmpty(vid.title) && this.br.containsHTML("&title=")) {
            final String match = this.br.getRegex("&title=([^&$]+)").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.replaceAll("\\+", " ").trim());
            }

        }
        if (StringUtils.isEmpty(vid.title)) {
            final String match = this.br.getRegex("<title>(.*?) - YouTube</title>").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.replaceAll("\\+", " ").trim());

            }
        }

        if (vid.length <= 0) {
            final String match = this.br.getRegex("\"length_seconds\"\\: (\\d+)").getMatch(0);
            if (match != null) {
                vid.length = Integer.parseInt(match);

            }
        }
        if (StringUtils.isEmpty(vid.title)) {
            final String match = this.br.getRegex("<meta name=\"title\" content=\"(.*?)\">").getMatch(0);
            if (match != null) {
                vid.title = Encoding.htmlDecode(match.trim());

            }
        }

        if (vid.date <= 0) {
            final Locale locale = Locale.ENGLISH;
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", locale);
            String date = this.br.getRegex("id=\"eow-date\" class=\"watch-video-date\" >(\\d{2}\\.\\d{2}\\.\\d{4})</span>").getMatch(0);
            if (date == null) {
                formatter = new SimpleDateFormat("dd MMM yyyy", locale);
                date = this.br.getRegex("class=\"watch-video-date\" >([ ]+)?(\\d{1,2} [A-Za-z]{3} \\d{4})</span>").getMatch(1);
            }
            if (date != null) {
                try {
                    vid.date = formatter.parse(date).getTime();
                } catch (final Exception e) {
                    final LogSource log = LogController.getInstance().getPreviousThreadLogSource();
                    log.log(e);

                }
            }
        }
        if (StringUtils.isEmpty(vid.channel)) {
            final String match = this.br.getRegex("feature=watch\"[^>]+dir=\"ltr[^>]+>(.*?)</a>(\\s+)?<span class=\"yt-user").getMatch(0);
            if (match != null) {
                vid.channel = Encoding.htmlDecode(match.trim());

            }
        }
        if (StringUtils.isEmpty(vid.user)) {
            final String match = this.br.getRegex("temprop=\"url\" href=\"http://(www\\.)?youtube\\.com/user/([^<>\"]*?)\"").getMatch(0);
            if (match != null) {
                vid.user = Encoding.htmlDecode(match.trim());

            }
        }

    }

    public void getPage(final String relativeUrl) throws IOException {
        this.br.getPage(this.base + relativeUrl);
    }

    protected void handleContentWarning(final Browser br) throws Exception {

        // youtube shows an extra screen the first time a user wants to see a age-protected video.
        // <div class="content">
        // <h1 id="unavailable-message" class="message">
        // Content Warning
        //
        // </h1>
        // <div id="unavailable-submessage" class="submessage">
        // <div id="watch7-player-age-gate-content">
        // <p>This video may be inappropriate for some users.</p>
        //
        // <p>By confirming, you agree that this warning will no longer be shown in the future.</p>
        // <form action="/verify_age?action_confirm=1" method="POST">
        // <input type="hidden" name="next_url" value="/watch?v=p7S_u5TzI-I">
        // <input type="hidden" name="set_racy" value="true">
        // <input type="hidden" name="session_token" value="d5tNczUUbnz7-G160SZlqEFiM798MTM4OTE3NzA4M0AxMzg5MDkwNjgz"/>
        // <button onclick=";return true;" class=" yt-uix-button yt-uix-button-primary yt-uix-button-size-default" type="submit"
        // role="button"><span class="yt-uix-button-content">I understand and wish to proceed </span></button>
        // </form>
        //
        // <p class="safety-mode-message">If you would instead prefer to avoid potentially inappropriate content, consider
        // activating YouTube's <a href="//support.google.com/youtube/bin/answer.py?answer=174084&amp;hl=en-GB">Safety Mode</a>.</p>
        // </div>

        final Form forms[] = br.getForms();
        if (forms != null) {
            for (final Form form : forms) {
                if (form.getAction() != null && form.getAction().contains("verify_age")) {
                    this.logger.info("Verify Age");
                    br.submitForm(form);
                    break;
                }
            }
        }

    }

    public Map<YoutubeITAG, StreamData> loadVideo(final ClipData vid) throws Exception {
        final Map<YoutubeITAG, StreamData> ret = new HashMap<YoutubeITAG, StreamData>();
        final YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);

        this.br.setFollowRedirects(true);
        /* this cookie makes html5 available and skip controversy check */

        this.br.setCookie("youtube.com", "PREF", "f2=40100000&hl=en-GB");
        this.br.getHeaders().put("User-Agent", "Wget/1.12");
        this.br.getPage(this.base + "/watch?v=" + vid.videoID);
        if (this.br.containsHTML("id=\"unavailable-submessage\" class=\"watch-unavailable-submessage\"")) { return null; }

        this.extractData(vid);
        boolean getVideoInfoWorkaroundUsed = false;
        if (this.br.containsHTML("age-gate")) {
            vid.ageCheck = true;

            this.handleContentWarning(this.br);

            if (this.br.containsHTML("age-gate")) {
                // try to bypass
                getVideoInfoWorkaroundUsed = true;
                this.br.getPage(this.base + "/get_video_info?video_id=" + vid.videoID);
            }
            this.extractData(vid);
        }

        // check if video is private
        final String unavailableReason = this.br.getRegex("<div id=\"player-unavailable\" class=\"  player-width player-height    player-unavailable \">.*?<h. id=\"unavailable-message\"[^>]*?>([^<]+)").getMatch(0);
        if (unavailableReason != null) {
            vid.error = Encoding.htmlDecode(unavailableReason.replaceAll("\\+", " ").trim());
            return null;
        }

        String html5_fmt_map;
        String dashFmt;
        if (getVideoInfoWorkaroundUsed) {
            // age check bypass active
            // testurl age-check: http://www.youtube.com/watch?v=nNYEG9kmnQk
            html5_fmt_map = this.br.getRegex("url_encoded_fmt_stream_map=(.*?)(&|$)").getMatch(0);
            html5_fmt_map = Encoding.htmlDecode(html5_fmt_map);

            dashFmt = this.br.getRegex("adaptive_fmts=(.*?)(&|$)").getMatch(0);
            dashFmt = Encoding.htmlDecode(dashFmt);

        } else {
            // regular url testlink: http://www.youtube.com/watch?v=4om1rQKPijI

            html5_fmt_map = this.br.getRegex("\"url_encoded_fmt_stream_map\": (\".*?\")").getMatch(0);

            html5_fmt_map = JSonStorage.restoreFromString(html5_fmt_map, new TypeRef<String>() {
            });

            dashFmt = this.br.getRegex("\"adaptive_fmts\": (\".*?\")").getMatch(0);
            dashFmt = JSonStorage.restoreFromString(dashFmt, new TypeRef<String>() {
            });
        }

        for (final String line : html5_fmt_map.split("\\,")) {
            final StreamData match = this.parseLine(vid, line);
            if (match != null) {
                ret.put(match.itag, match);
            }
        }
        if (dashFmt != null) {
            for (final String line : dashFmt.split("\\,")) {
                final StreamData match = this.parseLine(vid, line);
                if (match != null) {
                    ret.put(match.itag, match);
                }
            }
        }

        for (StreamData sd : loadThumbnails(vid)) {
            ret.put(sd.itag, sd);
        }

        return ret;
    }

    private List<StreamData> loadThumbnails(ClipData vid) {
        ArrayList<StreamData> ret = new ArrayList<StreamData>();
        String best = br.getRegex("<meta property=\"og\\:image\" content=\".*?/(\\w+\\.jpg)\">").getMatch(0);
        ret.add(new StreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/default.jpg", YoutubeITAG.IMAGE_LQ));
        if (best != null && best.equals("default.jpg")) return ret;
        ret.add(new StreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/mqdefault.jpg", YoutubeITAG.IMAGE_MQ));
        if (best != null && best.equals("mqdefault.jpg")) return ret;
        ret.add(new StreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/hqdefault.jpg", YoutubeITAG.IMAGE_HQ));
        if (best != null && best.equals("hqdefault.jpg")) return ret;

        ret.add(new StreamData(vid, "http://img.youtube.com/vi/" + vid.videoID + "/maxresdefault.jpg", YoutubeITAG.IMAGE_MAX));

        return ret;
    }

    public void login(final Account account, final boolean refresh, final boolean showDialog) throws Exception {

        try {
            this.br.setDebug(true);
            this.br.setCookiesExclusive(true);
            this.br.clearCookies("youtube.com");
            br.setCookie("http://youtube.com", "PREF", "hl=en-GB");
            if (account.getProperty("cookies") != null) {
                @SuppressWarnings("unchecked")
                final HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty("cookies");
                if (cookies != null) {
                    if (cookies.containsKey("LOGIN_INFO")) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("youtube.com", key, value);
                        }

                        if (refresh == false) {
                            return;
                        } else {
                            this.br.getPage("http://www.youtube.com");
                            if (!this.br.containsHTML("<span class=\"yt-uix-button-content\">Sign In </span></button></div>")) { return; }
                        }
                    }
                }
            }

            this.br.setFollowRedirects(true);
            this.br.getPage(this.replaceHttps("http://www.youtube.com/"));
            /* first call to google */
            this.br.getPage("https://www.google.com/accounts/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");
            String checkConnection = this.br.getRegex("iframeUri: \\'(https.*?)\\'").getMatch(0);
            if (checkConnection != null) {
                /*
                 * don't know if this is important but seems to set pstMsg to 1 ;)
                 */
                checkConnection = Encoding.unescape(checkConnection);
                try {
                    this.br.cloneBrowser().getPage(checkConnection);
                } catch (final Exception e) {
                    this.logger.info("checkConnection failed, continuing anyways...");
                }
            }
            final Form form = this.br.getForm(0);
            form.put("pstMsg", "1");
            form.put("dnConn", "https%3A%2F%2Faccounts.youtube.com&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252F");
            form.put("Email", Encoding.urlEncode(account.getUser()));
            form.put("Passwd", Encoding.urlEncode(account.getPass()));
            form.put("GALX", this.br.getCookie("http://www.google.com", "GALX"));
            form.put("timeStmp", "");
            form.put("secTok", "");
            form.put("rmShown", "1");
            form.put("signIn", "Anmelden");
            form.put("asts", "");
            this.br.setFollowRedirects(false);
            final String cook = this.br.getCookie("http://www.google.com", "GALX");
            if (cook == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.br.submitForm(form);
            if (this.br.getRedirectLocation() == null) {
                final String page = Encoding.htmlDecode(this.br.toString());
                final String red = new Regex(page, "url='(https?://.*?)'").getMatch(0);
                if (red == null) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                this.br.getPage(red);
            }
            /* second call to google */
            this.br.getPage(this.br.getRedirectLocation());
            if (this.br.containsHTML("Google will check if this")) {
                if (showDialog) {
                    UserIO.getInstance().requestMessageDialog(0, "Youtube Login Error", "Please logout and login again at youtube.com, account check needed!");
                }
                account.setValid(false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }

            // 2-step verification
            if (this.br.containsHTML("2-step verification")) {
                final String step = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("plugins.hoster.youtube.2step.title", "2-Step verification required"), JDL.L("plugins.hoster.youtube.2step.message", "Youtube.com requires Google's 2-Step verification. Please input the code from your phone or the backup list."), "", null, null, null);
                Form stepform = this.br.getForm(0);
                stepform.put("smsUserPin", step);
                stepform.remove("exp");
                stepform.remove("ltmpl");
                this.br.setFollowRedirects(true);
                this.br.submitForm(stepform);

                if (this.br.containsHTML("The code you entered didn&#39;t verify")) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.2step.failed", "2-Step verification code couldn't be verified!"));
                }

                stepform = this.br.getForm(0);
                if (stepform != null) {
                    stepform.remove("nojssubmit");
                    this.br.submitForm(stepform);
                    this.br.getPage(this.replaceHttps("http://www.youtube.com/signin?action_handle_signin=true"));
                } else {
                    String url = this.br.getRegex("\"(https?://www\\.youtube\\.com/signin\\?action_handle_signin.*?)\"").getMatch(0);
                    if (url != null) {
                        url = Encoding.unescape(url);
                        this.br.getPage(url);
                    }
                }
            } else if (this.br.containsHTML("class=\"gaia captchahtml desc\"")) {
                if (true) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                }
                final String captchaLink = this.br.getRegex("<img src=\\'(https?://accounts\\.google\\.com/Captcha\\?[^<>\"]*?)\\'").getMatch(0);
                if (captchaLink == null) {
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.youtube.logincaptcha.failed", "The captcha login verification is broken. Please contact our support."));
                }
                // final DownloadLink dummyLink = new DownloadLink(this, "Account", "youtube.com", "http://youtube.com", true);
                // final String c = getCaptchaCode(captchaLink, dummyLink);
                // Lots of stuff needed here
                // br.postPage("https://accounts.google.com/LoginVerification", "");
                throw new WTFException("Not Implemented");

            } else {
                this.br.setFollowRedirects(true);
                this.br.getPage(this.br.getRedirectLocation());

                final String location = Encoding.unescape(this.br.getRegex("location\\.replace\\(\"(.*?)\"").getMatch(0));
                this.br.getPage(location);
            }
            if (this.br.getCookie("http://www.youtube.com", "LOGIN_INFO") == null) {
                account.setValid(false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies cYT = this.br.getCookies("youtube.com");
            for (final Cookie c : cYT.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            // set login cookie of the account.
            account.setProperty("cookies", cookies);
        } catch (final PluginException e) {
            account.setProperty("cookies", null);
            throw e;
        }

    }

    public void login(final boolean refresh, final boolean showDialog) {

        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n, refresh, showDialog);
                        if (n.isValid()) { return; }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }
        return;
    }

    private static final String DATE_FORMAT           = "dd.MM.yyyy_HH-mm-ss";
    public static final String  YT_LENGTH_SECONDS     = "YT_LENGTH_SECONDS";
    public static final String  YT_STATIC_URL         = "YT_STATIC_URL";
    public static final String  YT_STREAMURL_DATA     = "YT_STREAMURL_DATA";
    public static final String  YT_SUBTITLE_CODE      = "YT_SUBTITLE_CODE";
    public static final String  YT_SUBTITLE_CODE_LIST = "YT_SUBTITLE_CODE_LIST";

    public static String createFilename(DownloadLink link) {
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        LogSource logger = LogController.getInstance().getLogger(TbCmV2.class.getName());
        String formattedFilename = cfg.getFilenamePattern();
        // validate the pattern
        if ((!formattedFilename.contains("*videoname*") && !formattedFilename.contains("*videoid*")) || !formattedFilename.contains("*ext*")) formattedFilename = jd.plugins.hoster.Youtube.defaultCustomFilename;
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = "*videoname**quality**ext*";
        try {
            formattedFilename = YoutubeVariant.valueOf(link.getStringProperty(YoutubeHelper.YT_VARIANT, "")).modifyFileName(formattedFilename, link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // playlistnumber
        String format = new Regex(formattedFilename, "\\*videonumber\\[(.+?)\\]\\*").getMatch(0);

        if (StringUtils.isEmpty(format)) format = "0000";
        DecimalFormat df;
        try {
            df = new DecimalFormat(format);
        } catch (Exception e) {
            logger.log(e);
            df = new DecimalFormat("0000");
        }
        int playlistNumber = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_INT, -1);
        formattedFilename = formattedFilename.replaceAll("\\*videonumber(\\[[^\\]]+\\])?\\*", playlistNumber >= 0 ? df.format(playlistNumber) : "");
        // date
        format = new Regex(formattedFilename, "\\*date\\[(.+?)\\]\\*").getMatch(0);
        if (StringUtils.isEmpty(format)) format = DATE_FORMAT;
        SimpleDateFormat formatter;
        try {
            formatter = new SimpleDateFormat(format);
        } catch (Exception e) {
            logger.log(e);
            formatter = new SimpleDateFormat(DATE_FORMAT);
        }
        long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE, -1);
        formattedFilename = formattedFilename.replaceAll("\\*date(\\[[^\\]]+\\])?\\*", timestamp >= 0 ? formatter.format(timestamp) : "");
        // channelname
        formattedFilename = formattedFilename.replace("*channelname*", link.getStringProperty(YoutubeHelper.YT_CHANNEL, ""));
        formattedFilename = formattedFilename.replace("*username*", link.getStringProperty(YoutubeHelper.YT_USER, ""));
        formattedFilename = formattedFilename.replace("*agegate*", link.getBooleanProperty(YoutubeHelper.YT_AGE_GATE, false) + "");
        formattedFilename = formattedFilename.replace("*ext*", link.getStringProperty(YoutubeHelper.YT_EXT, "unknown"));
        formattedFilename = formattedFilename.replace("*videoid*", link.getStringProperty(YoutubeHelper.YT_ID, ""));
        try {
            String var = link.getStringProperty(YoutubeHelper.YT_VARIANT, "");

            formattedFilename = formattedFilename.replace("*quality*", YoutubeVariant.valueOf(var).getQualityExtension());
        } catch (Exception e) {
            // old variant
            formattedFilename = formattedFilename.replace("*quality*", "[INVALID LINK!]");
        }
        formattedFilename = formattedFilename.replace("*videoname*", link.getStringProperty(YoutubeHelper.YT_TITLE, "")).replace("*title*", link.getStringProperty(YoutubeHelper.YT_TITLE, ""));
        formattedFilename = formattedFilename.replace("*variant*", link.getStringProperty(YoutubeHelper.YT_VARIANT, ""));
        try {
            formattedFilename = YoutubeVariant.valueOf(link.getStringProperty(YoutubeHelper.YT_VARIANT, "")).modifyFileName(formattedFilename, link);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedFilename;
    }

    protected StreamData parseLine(final ClipData vid, final String line) throws MalformedURLException, IOException, PluginException {

        final LinkedHashMap<String, String> query = Request.parseQuery(line);
        if (line.contains("conn=rtmp")) {
            logger.info("Stream is not supported: " + query);
            vid.error = "RTMP(E) Stream not supported";
            return null;
        }
        String url = Encoding.htmlDecode(Encoding.urlDecode(query.get("url"), true));

        String signature = new Regex(url, "(sig|signature)=(.*?)(\\&|$)").getMatch(1);

        if (StringUtils.isEmpty(signature)) {
            // verified 7.1.24
            // non dash?
            signature = query.get("sig");
        }
        if (StringUtils.isEmpty(signature)) {
            signature = query.get("signature");
        }
        if (StringUtils.isEmpty(signature)) {
            // verified 7.1.213
            signature = this.descrambleSignature(query.get("s"));
        }

        if (url != null && !url.contains("sig")) {

            url = url + "&signature=" + signature;
        }
        int bitrate = -1;
        String bitrateString = query.get("bitrate");
        if (StringUtils.isNotEmpty(bitrateString)) {
            bitrate = Integer.parseInt(bitrateString);
        }
        final YoutubeITAG itag = YoutubeITAG.get(Integer.parseInt(query.get("itag")));

        final String quality = Encoding.urlDecode(query.get("quality"), false);
        System.out.println(query);
        if (url != null && itag != null) {

            return new StreamData(vid, url, itag);
        } else {

            this.logger.info("Unkown Line: " + line);
            this.logger.info(query + "");
        }
        return null;
    }

    private String replaceHttps(final String s) {

        final boolean prefers = this.cfg.isPreferHttpsEnabled();

        if (prefers) {
            return s.replaceFirst("http://", "https://");
        } else {
            return s.replaceFirst("https://", "http://");
        }
    }

    public void setupProxy() {

        if (this.cfg.isProxyEnabled()) {
            final HTTPProxyStorable proxy = this.cfg.getProxy();

            // int PROXY_PORT = cfg.getProxyPort();
            // if (StringUtils.isEmpty(PROXY_ADDRESS) || PROXY_PORT < 0) return;
            // PROXY_ADDRESS = new Regex(PROXY_ADDRESS, "^[0-9a-zA-Z]+://").matches() ? PROXY_ADDRESS : "http://" + PROXY_ADDRESS;
            // org.appwork.utils.net.httpconnection.HTTPProxy proxy =
            // org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(PROXY_ADDRESS + ":" + PROXY_PORT);

            if (proxy != null) {
                this.br.setProxy(HTTPProxy.getHTTPProxy(proxy));
            }

        }
        this.br.setProxy(this.br.getThreadProxy());
    }

    public static class SubtitleInfo {

        private String _base;

        public String getLang() {
            return lang;
        }

        public String getName() {
            return name;
        }

        public String getKind() {
            return kind;
        }

        public String getLangOrg() {
            return langOrg;
        }

        private String lang;

        public void setLang(String lang) {
            this.lang = lang;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public void setLangOrg(String langOrg) {
            this.langOrg = langOrg;
        }

        private String name;
        private String kind;
        private String langOrg;

        public SubtitleInfo(/* Storable */) {

        }

        public SubtitleInfo(String ttsUrl, String lang, String name, String kind, String langOrg) {
            this._base = ttsUrl;
            this.lang = lang;
            this.name = name;
            this.kind = kind;
            this.langOrg = langOrg;
        }

        public String _getUrl(String videoId) throws UnsupportedEncodingException {
            return _base + "&kind=" + URLEncoder.encode(kind, "UTF-8") + "&format=1&ts=" + System.currentTimeMillis() + "&type=track&lang=" + URLEncoder.encode(lang, "UTF-8") + "&name=" + URLEncoder.encode(name, "UTF-8") + "&v=" + URLEncoder.encode(videoId, "UTF-8");
        }

    }

    public ArrayList<SubtitleInfo> loadSubtitles(ClipData vid) throws IOException {
        ArrayList<SubtitleInfo> urls = new ArrayList<SubtitleInfo>();
        String ttsUrl = br.getRegex("\"ttsurl\": (\"http.*?\")").getMatch(0);
        if (ttsUrl != null) {
            ttsUrl = JSonStorage.restoreFromString(ttsUrl, new TypeRef<String>() {

            });
        } else {
            return urls;
        }

        br.getPage(replaceHttps(ttsUrl + "&asrs=1&fmts=1&tlangs=1&ts=" + System.currentTimeMillis() + "&type=list"));

        ttsUrl = ttsUrl.replaceFirst("v=[a-zA-Z0-9\\-_]+", "");

        String[] matches = br.getRegex("<track id=\"(.*?)\".*?/>").getColumn(0);
        HashSet<String> duplicate = new HashSet<String>();

        for (String trackID : matches) {
            String lang = br.getRegex("<track id=\"" + trackID + "\".*?lang_code=\"(.*?)\".*?/>").getMatch(0);
            String name = br.getRegex("<track id=\"" + trackID + "\".*?name=\"(.*?)\".*?/>").getMatch(0);
            String kind = br.getRegex("<track id=\"" + trackID + "\".*?kind=\"(.*?)\".*?/>").getMatch(0);
            String langOrg = br.getRegex("<track id=\"" + trackID + "\".*?lang_original=\"(.*?)\".*?/>").getMatch(0);
            String langTrans = br.getRegex("<track id=\"" + trackID + "\".*?lang_translated=\"(.*?)\".*?/>").getMatch(0);
            if (name == null) name = "";
            if (kind == null) kind = "";
            if (duplicate.add(lang) == false) continue;
            if (StringUtils.isNotEmpty(langTrans)) {
                langOrg = langTrans;
            }
            if (StringUtils.isEmpty("langOrg")) {
                langOrg = new Locale("lang").getDisplayLanguage(Locale.ENGLISH);
            }
            urls.add(new SubtitleInfo(ttsUrl, lang, name, kind, langOrg));

        }
        return urls;
    }

}