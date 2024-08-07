package org.jdownloader.plugins.components.youtube;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.XML;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.ffmpeg.AbstractFFmpegBinary;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.components.youtube.YoutubeReplacer.DataOrigin;
import org.jdownloader.plugins.components.youtube.YoutubeReplacer.DataSource;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoInterface;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.components.youtube.variants.YoutubeSubtitleStorable;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericAudioInfo;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JSRhinoPermissionRestricter;
import org.jdownloader.scripting.JSShutterDelegate;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;
import org.jdownloader.updatev2.FilterList;
import org.jdownloader.updatev2.FilterList.Type;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdateHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jd.controlling.AccountController;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.http.StaticProxySelector;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class YoutubeHelper {
    static {
        final YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        String filepattern = cfg.getFilenamePattern();
        if (filepattern != null && !"*videoname* (*quality*).*ext*".equals(filepattern)) {
            // convert old format
            cfg.setVideoFilenamePattern(filepattern);
            cfg.setAudioFilenamePattern(filepattern);
            cfg.setFilenamePattern(null);
        }
        if (cfg.isProxyEnabled()) {
        }
        HTTPProxyStorable proxyStorable = cfg.getProxy();
        if (proxyStorable != null) {
            HTTPProxy proxy = HTTPProxy.getHTTPProxy(proxyStorable);
            SingleBasicProxySelectorImpl selector = new SingleBasicProxySelectorImpl(proxy);
            FilterList filterlist = new FilterList(Type.WHITELIST, new String[] { "youtube.com" });
            selector.setEnabled(cfg.isProxyEnabled());
            selector.setFilter(filterlist);
            ProxyController.getInstance().addProxy(selector);
            cfg.setProxy(null);
        }
    }
    private static final String REGEX_DASHMPD_FROM_JSPLAYER_SETUP          = "\"dashmpd\"\\s*:\\s*(\".*?\")";
    private static final String REGEX_ADAPTIVE_FMTS_FROM_JSPLAYER_SETUP    = "\"adaptive_fmts\"\\s*:\\s*(\".*?\")";
    private static final String REGEX_FMT_MAP_FROM_JSPLAYER_SETUP          = "\"url_encoded_fmt_stream_map\"\\s*:\\s*(\".*?\")";
    public static final String  PAID_VIDEO                                 = "Paid Video:";
    public static final String  YT_CHANNEL_ID                              = "YT_CHANNEL_ID";
    public static final String  YT_ATID                                    = "YT_@ID";
    public static final String  YT_CHANNEL_SIZE                            = "YT_CHANNEL_SIZE";
    public static final String  YT_DURATION                                = "YT_DURATION";
    public static final String  YT_DATE_UPLOAD                             = "YT_DATE_UPDATE";
    public static final String  YT_DATE_LIVESTREAM_START                   = "YT_DATE_LIVESTREAM_START";
    public static final String  YT_DATE_LIVESTREAM_END                     = "YT_DATE_LIVESTREAM_END";
    public static final String  YT_GOOGLE_PLUS_ID                          = "YT_GOOGLE_PLUS_ID";
    public static final String  YT_VIEWS                                   = "YT_VIEWS";
    private Browser             br;
    private String              channelPlaylistCrawlerContainerUrlOverride = null;

    public Browser getBr() {
        return br;
    }

    public void setBr(Browser br) {
        this.br = prepareBrowser(br);
    }

    private final YoutubeConfig cfg;
    private final LogInterface  logger;

    public LogInterface getLogger() {
        return logger;
    }

    private String                            base;
    // private List<YoutubeBasicVariant> variants;
    // public List<YoutubeBasicVariant> getVariants() {
    // return variants;
    // }
    // private Map<String, YoutubeBasicVariant> variantsMap;
    // public Map<String, YoutubeBasicVariant> getVariantsMap() {
    // return variantsMap;
    // }
    public static final List<YoutubeReplacer> REPLACER = new ArrayList<YoutubeReplacer>();
    static {
        REPLACER.add(new YoutubeReplacer("GROUP") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getGroup().getLabel();
                } catch (Throwable e) {
                    helper.logger.log(e);
                    return "[INVALID LINK!]";
                }
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_group();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("ITAG_AUDIO_NAME") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getiTagAudioOrVideoItagEquivalent().name();
                } catch (Throwable e) {
                }
                return "";
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_itag_audio_name();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("ITAG_VIDEO_NAME") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getiTagVideo().name();
                } catch (Throwable e) {
                }
                return "";
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_itag_video_name();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("ITAG_VIDEO_ID") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getiTagVideo().getITAG() + "";
                } catch (Throwable e) {
                }
                return "";
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_itag_video_id();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("ITAG_AUDIO_ID") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getiTagAudioOrVideoItagEquivalent().getITAG() + "";
                } catch (Throwable e) {
                }
                return "";
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_itag_audio_id();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("VARIANT", "V") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                if ("name".equalsIgnoreCase(mod)) {
                    return AbstractVariant.get(link)._getName(link);
                } else {
                    return AbstractVariant.get(link)._getUniqueId();
                }
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_variantid2();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("QUALITY") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_quality();
            }

            @Override
            public String replace(String name, YoutubeHelper helper, DownloadLink link) {
                return super.replace(name, helper, link);
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                try {
                    return variant.getFileNameQualityTag();
                } catch (Throwable e) {
                    helper.logger.log(e);
                    return "[INVALID LINK!]";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("COLLECTION", "COL") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_collection();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_COLLECTION, "");
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("VIDEOID", "VIDEO_ID", "ID") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_id();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_ID, "");
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("@ID", "ATID") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_atid();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_ATID, "");
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("360", "SPHERICAL") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_spherical();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    switch (((VideoVariant) variant).getProjection()) {
                    case SPHERICAL:
                    case SPHERICAL_3D:
                        return "Spherical";
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("THREED", "3D") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_3d();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    switch (((VideoVariant) variant).getProjection()) {
                    case ANAGLYPH_3D:
                    case SPHERICAL_3D:
                        return "3D";
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("FPS") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_fps();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    int fps = ((VideoInterface) variant).getVideoFrameRate();
                    if (fps > 0) {
                        return fps + "";
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("EXT", "EXTENSION") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_extension();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                return variant.getContainer().getExtension();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("HEIGHT", "H") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_height();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    return ((VideoInterface) variant).getVideoHeight() + "";
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("HEIGHT_BEST") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_best_height();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_BEST_VIDEO_HEIGHT, "");
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("WIDTH", "W") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_width();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    return ((VideoInterface) variant).getVideoWidth() + "";
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("USERNAME", "USER") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_USER_NAME, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_user();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("USERNAME_ALTERNATIVE", "USER_ALTERNATIVE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_USER_NAME_ALTERNATIVE, link.getStringProperty(YoutubeHelper.YT_USER_NAME, ""));
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_user() + " " + _GUI.T.YoutubeHelper_getDescription_alternative();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_ID", "PL_ID") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_PLAYLIST_ID, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_playlist_id();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_CREATOR", "PLAYLIST_USERNAME", "PL_USR") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_PLAYLIST_CREATOR, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_playlist_creator();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_NAME", "PLAYLIST_TITLE", "PL_NAME") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_PLAYLIST_TITLE, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_playlist_name();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_SIZE", "PL_SIZE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                final int ret = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_SIZE, -1);
                if (ret == -1) {
                    return "";
                } else {
                    return String.valueOf(ret);
                }
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_playlist_size();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_DESCRIPTION", "PL_DESCR") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_PLAYLIST_DESCRIPTION, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_playlist_description();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("PLAYLIST_POSITION", "PL_POS", "VIDEONUMBER") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_videonumber();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                final int playlistNumber = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_POSITION, -1);
                if (playlistNumber >= 0) {
                    // playlistnumber
                    DecimalFormat df = new DecimalFormat("0000");
                    try {
                        if (StringUtils.isNotEmpty(mod)) {
                            df = new DecimalFormat(mod);
                        }
                    } catch (Throwable e) {
                        helper.logger.log(e);
                    }
                    return df.format(playlistNumber);
                } else {
                    return "";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_PLAYLIST };
            }
        });
        REPLACER.add(new YoutubeReplacer("CHANNEL_ID", "CH_ID") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_CHANNEL_ID, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_channel_id();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("CHANNEL_SIZE", "CH_SIZE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                final int ret = link.getIntegerProperty(YoutubeHelper.YT_CHANNEL_SIZE, -1);
                if (ret == -1) {
                    return "";
                } else {
                    return String.valueOf(ret);
                }
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_channel_size();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("GOOGLEPLUS_ID") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_GOOGLE_PLUS_ID, "");
            }

            public DataSource getDataSource() {
                return DataSource.API_USERS;
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_googleplus_id();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("DURATION") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                final int secs = link.getIntegerProperty(YoutubeHelper.YT_DURATION, -1);
                if (secs <= 0) {
                    return "";
                } else {
                    if (StringUtils.isEmpty(mod)) {
                        return TimeFormatter.formatSeconds(secs, 0);
                    } else {
                        return secs + "s";
                    }
                }
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_duration();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("CHANNEL", "CHANNELNAME", "CHANNEL_NAME") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_CHANNEL_TITLE, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_channel();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("CHANNEL_ALTERNATIVE", "CHANNELNAME_ALTERNATIVE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_CHANNEL_TITLE_ALTERNATIVE, link.getStringProperty(YoutubeHelper.YT_CHANNEL_TITLE, ""));
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_channel() + " " + _GUI.T.YoutubeHelper_getDescription_alternative();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO, DataOrigin.YT_CHANNEL };
            }
        });
        REPLACER.add(new YoutubeReplacer("VIDEONAME", "VIDEO_NAME", "TITLE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_TITLE, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_title();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("VIDEONAME_ALTERNATIVE", "VIDEO_NAME_ALTERNATIVE", "TITLE_ALTERNATIVE") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_TITLE_ALTERNATIVE, link.getStringProperty(YoutubeHelper.YT_TITLE, ""));
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_title() + " " + _GUI.T.YoutubeHelper_getDescription_alternative();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("CATEGORY") {
            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_CATEGORY, "");
            }

            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_category();
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("DATE", "DATE_TIME", "DATE_PUBLISH") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_date();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                DateFormat formatter = null;
                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod, TranslationFactory.getDesiredLocale());
                    } catch (Throwable e) {
                        helper.logger.log(e);
                    }
                }
                if (formatter == null) {
                    formatter = DateFormat.getDateInstance(DateFormat.LONG, TranslationFactory.getDesiredLocale());
                }
                final long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE, -1);
                if (timestamp > 0) {
                    final String ret = formatter.format(timestamp);
                    helper.logger.info(" Youtube Replace DATE_PUBLISH " + mod + " - " + timestamp + " > " + ret);
                    return ret;
                } else {
                    return "";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("DATE_LIVESTREAM_START") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_date_livestream_start();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                DateFormat formatter = null;
                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod, TranslationFactory.getDesiredLocale());
                    } catch (Throwable e) {
                        helper.logger.log(e);
                    }
                }
                if (formatter == null) {
                    formatter = DateFormat.getDateInstance(DateFormat.LONG, TranslationFactory.getDesiredLocale());
                }
                final long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE_LIVESTREAM_START, -1);
                if (timestamp > 0) {
                    final String ret = formatter.format(timestamp);
                    helper.logger.info(" Youtube Replace DATE_LIVESTREAM_START " + mod + " - " + timestamp + " > " + ret);
                    return ret;
                } else {
                    return "";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("DATE_LIVESTREAM_END") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_date_livestream_start();
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                DateFormat formatter = null;
                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod, TranslationFactory.getDesiredLocale());
                    } catch (Throwable e) {
                        helper.logger.log(e);
                    }
                }
                if (formatter == null) {
                    formatter = DateFormat.getDateInstance(DateFormat.LONG, TranslationFactory.getDesiredLocale());
                }
                final long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE_LIVESTREAM_END, -1);
                if (timestamp > 0) {
                    final String ret = formatter.format(timestamp);
                    helper.logger.info(" Youtube Replace DATE_LIVESTREAM_END " + mod + " - " + timestamp + " > " + ret);
                    return ret;
                } else {
                    return "";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        // keep compatibility with wrong 'DATE_UDPATE' TAG, 06.10.2020
        REPLACER.add(new YoutubeReplacer("DATE_UPLOAD", "DATE_UDPATE") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_date_upload();
            }

            public DataSource getDataSource() {
                return DataSource.API_VIDEOS;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                DateFormat formatter = null;
                if (StringUtils.isNotEmpty(mod)) {
                    try {
                        formatter = new SimpleDateFormat(mod, TranslationFactory.getDesiredLocale());
                    } catch (Throwable e) {
                        helper.logger.log(e);
                    }
                }
                if (formatter == null) {
                    formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, TranslationFactory.getDesiredLocale());
                }
                final long timestamp = link.getLongProperty(YoutubeHelper.YT_DATE_UPLOAD, -1);
                if (timestamp > 0) {
                    final String ret = formatter.format(timestamp);
                    helper.logger.info(" Youtube Replace Update-Date " + mod + " - " + timestamp + " > " + ret);
                    return ret;
                } else {
                    return "";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("VIDEO_CODEC", "CODEC_VIDEO") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_videoCodec();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    VideoCodec v = ((VideoInterface) variant).getVideoCodec();
                    if (v != null) {
                        return v.getLabel();
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("RESOLUTION") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_resolution();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null && variant instanceof VideoVariant) {
                    VideoResolution v = ((VideoInterface) variant).getVideoResolution();
                    if (v != null) {
                        return v.getLabel();
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("BESTRESOLUTION", "RESOLUTION_BEST") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_resolution_best();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                // date
                final String var = link.getStringProperty(YoutubeHelper.YT_BEST_VIDEO, "");
                if (var == null) {
                    return "";
                }
                try {
                    return YoutubeITAG.valueOf(var).getVideoResolution().getLabel();
                } catch (Throwable e) {
                    helper.logger.log(e);
                    return "[INVALID LINK!]";
                }
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("AUDIO_CODEC", "CODEC_AUDIO") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_audioCodec();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null) {
                    if (variant instanceof AudioInterface) {
                        return ((AudioInterface) variant).getAudioCodec().getLabel();
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("LNG") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_language();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                final AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null) {
                    if (variant instanceof SubtitleVariant) {
                        final SubtitleVariant subtitle = ((SubtitleVariant) variant);
                        final String asr;
                        if (subtitle.getGenericInfo()._isSpeechToText()) {
                            asr = "_ASR";
                        } else {
                            asr = "";
                        }
                        if ("full".equalsIgnoreCase(mod)) {
                            final int multi = subtitle.getGenericInfo().getMulti();
                            if (multi > 0) {
                                return subtitle.getGenericInfo()._getLocale().getDisplayName() + "(" + multi + ")" + asr;
                            } else {
                                return subtitle.getGenericInfo()._getLocale().getDisplayName() + asr;
                            }
                        } else if ("display".equalsIgnoreCase(mod)) {
                            return subtitle.getGenericInfo()._getLocale().getDisplayLanguage() + asr;
                        } else {
                            return subtitle.getGenericInfo().getLanguage() + asr;
                        }
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("AUDIO_BITRATE") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_audioQuality();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                AbstractVariant variant = AbstractVariant.get(link);
                if (variant != null) {
                    if (variant instanceof AudioInterface) {
                        return ((AudioInterface) variant).getAudioBitrate().getKbit() + "";
                    }
                }
                return "";
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
        REPLACER.add(new YoutubeReplacer("VIEWS") {
            @Override
            public String getDescription() {
                return _GUI.T.YoutubeHelper_getDescription_views();
            }

            public DataSource getDataSource() {
                return DataSource.WEBSITE;
            }

            @Override
            protected String getValue(DownloadLink link, YoutubeHelper helper, String mod) {
                return link.getStringProperty(YoutubeHelper.YT_VIEWS, "");
            }

            @Override
            public DataOrigin[] getDataOrigins() {
                return new DataOrigin[] { DataOrigin.YT_SINGLE_VIDEO };
            }
        });
    }
    public static final String  YT_TITLE                         = "YT_TITLE";
    public static final String  YT_TITLE_ALTERNATIVE             = "YT_TITLE_ALTERNATIVE";
    public static final String  YT_CATEGORY                      = "YT_CATEGORY";
    public static final String  YT_ID                            = "YT_ID";
    public static final String  YT_CHANNEL_TITLE                 = "YT_CHANNEL";
    public static final String  YT_CHANNEL_TITLE_ALTERNATIVE     = "YT_CHANNEL_ALTERNATIVE";
    public static final String  YT_DATE                          = "YT_DATE";
    public static final String  YT_VARIANTS                      = "YT_VARIANTS";
    public static final String  YT_VARIANT                       = "YT_VARIANT";
    /**
     * @deprecated use {@link #YT_VARIANT_INFO}
     */
    public static final String  YT_STREAMURL_VIDEO               = "YT_STREAMURL_VIDEO";
    /**
     * @deprecated use {@link #YT_VARIANT_INFO}
     */
    public static final String  YT_STREAMURL_AUDIO               = "YT_STREAMURL_AUDIO";
    /**
     * @deprecated use {@link #YT_VARIANT_INFO}
     */
    public static final String  YT_STREAMURL_VIDEO_SEGMENTS      = "YT_STREAMURL_VIDEO_SEGMENTS";
    /**
     * @deprecated use {@link #YT_VARIANT_INFO}
     */
    public static final String  YT_STREAMURL_AUDIO_SEGMENTS      = "YT_STREAMURL_AUDIO_SEGMENTS";
    private static final String REGEX_HLSMPD_FROM_JSPLAYER_SETUP = "\"hlsvp\"\\s*:\\s*(\".*?\")";

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
        if (new Regex(line, "\\.split\\(\"\"\\)").matches()) {
            return s;
        }
        if (new Regex(line, "\\.join\\(\"\"\\)").matches()) {
            return s;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Signature Rule: " + line);
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

    public YoutubeHelper(final Browser br, final LogInterface logger) {
        this.setBr(br);
        this.logger = logger;
        this.cfg = CFG_YOUTUBE.CFG;
        this.base = "https://www.youtube.com";
    }

    protected Browser prepareBrowser(Browser br) {
        if (br != null) {
            // br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1
            // like Mac OS X;)");
            br.setCookie("youtube.com", "PREF", "f1=50000000&hl=en");
            br.setCookie("youtube.com", "hideBrowserUpgradeBox", "true");
        }
        return br;
    }

    public static class StreamMap {
        public StreamMap(String map, String src) {
            this.mapData = map;
            this.src = src;
        }

        public StreamMap(YoutubeStreamData streamData, String src) {
            this.streamData = streamData;
            this.src = src;
        }

        @Override
        public String toString() {
            if (mapData != null) {
                return mapData + " (" + src + ")";
            } else {
                return streamData + " (" + src + ")";
            }
        }

        @Override
        public int hashCode() {
            if (mapData != null) {
                return mapData.hashCode();
            } else {
                return streamData.itag.hashCode();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (obj == this) {
                return true;
            } else if (!(obj instanceof StreamMap)) {
                return false;
            } else if (mapData != null && ((StreamMap) obj).mapData != null) {
                return StringUtils.equalsIgnoreCase(mapData, ((StreamMap) obj).mapData);
            } else if (streamData != null && ((StreamMap) obj).streamData != null) {
                return streamData.itag == ((StreamMap) obj).streamData.itag;
            } else {
                return false;
            }
        }

        private YoutubeStreamData streamData = null;

        public YoutubeStreamData getStreamData() {
            return streamData;
        }

        private String mapData;
        private String src;
    }

    private HashMap<String, String>  jsCache             = new HashMap<String, String>();
    private HashSet<String>          subtitleUrls;
    private HashSet<StreamMap>       fmtMaps;
    private LinkedHashSet<StreamMap> mpdUrls;
    private HashMap<String, String>  videoInfo;
    private Account                  account;
    private final boolean            hlsEnabled          = true;
    private final boolean            dashMpdEnabled      = true;
    private final boolean            adaptiveFmtsEnabled = true;
    private final boolean            fmtMapEnabled       = true;
    private String                   html5PlayerJs;
    private YoutubeClipData          vid;
    private Map<String, Object>      ytInitialData;
    private Map<String, Object>      ytInitialPlayerResponse;
    private Map<String, Object>      ytPlayerConfig;
    private Map<String, Object>      ytCfgSet;

    /**
     * @return the ytInitialData
     */
    public final Map<String, Object> getYtInitialData() {
        return ytInitialData;
    }

    /**
     * @return the ytInitialPlayerResponse
     */
    public final Map<String, Object> getYtInitialPlayerResponse() {
        return ytInitialPlayerResponse;
    }

    /**
     * @return the ytplayerConfig
     */
    public final Map<String, Object> getYtPlayerConfig() {
        return ytPlayerConfig;
    }

    /**
     * @return the ytcfgSet
     */
    public final Map<String, Object> getYtCfgSet() {
        return ytCfgSet;
    }

    /** Returns account in which we are logged in at this moment. */
    public final Account getAccountLoggedIn() {
        return account;
    }

    String descrambleSignature(final String sig) throws IOException, PluginException {
        if (sig == null) {
            return null;
        } else {
            final String ret = descrambleSignatureNew(sig);
            if (StringUtils.isNotEmpty(ret)) {
                return ret;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    String descrambleThrottle(final String value) throws IOException, PluginException {
        String input = value;
        String output = input;
        final String playerID = getPlayerID(html5PlayerJs);
        final String resultCacheKey = value + "/" + playerID;
        if (output != null) {
            String function = null;
            final String cachedResult;
            synchronized (jsCache) {
                cachedResult = jsCache.get(resultCacheKey);
            }
            if (cachedResult == null) {
                final String functionCacheKey = "descrambleThrottle/" + playerID;
                synchronized (jsCache) {
                    function = jsCache.get(functionCacheKey);
                    if (function == null) {
                        final String html5PlayerSource = ensurePlayerSource();
                        // String[][] func = new Regex(html5PlayerSource,
                        // "(?x)(?:\\.get\\(\"n\"\\)\\)&&\\(b=|b=String\\.fromCharCode\\(110\\),c=a\\.get\\(b\\)\\)&&\\(c=)([a-zA-Z0-9$]+)(?:\\[(\\d+)\\])?\\([a-zA-Z0-9]\\)").getMatches();
                        // since 2024-08-06
                        function = new Regex(html5PlayerSource, "(=function\\(a\\)\\{var b=a\\.split\\(a\\.slice\\(0,0\\)\\),c=\\[.*?\\};)\n").getMatch(0);
                        if (function == null) {
                            // since 2024-07-31
                            function = new Regex(html5PlayerSource, "(=function\\(a\\)\\{var b=String\\.prototype\\.split\\.call\\(a,\\(\"\"\\,\"\"\\)\\),c=\\[.*?\\};)\n").getMatch(0);
                            if (function == null) {
                                // since 2024-07
                                function = new Regex(html5PlayerSource, "(=function\\(a\\)\\{var b=String\\.prototype\\.split\\.call\\(a,\"\"\\),c=\\[.*?\\};)\n").getMatch(0);
                                if (function == null) {
                                    // before 2024-07
                                    function = new Regex(html5PlayerSource, "(=function\\(a\\)\\{var b=a\\.split\\(\"\"\\),c=\\[.*?\\};)\n").getMatch(0);
                                }
                            }
                        }
                        if (function != null) {
                            jsCache.put(functionCacheKey, function);
                        }
                    }
                }
            }
            if (cachedResult != null) {
                output = cachedResult;
            } else if (function != null) {
                final JSShutterDelegate jsShutter = new JSShutterDelegate() {
                    @Override
                    public boolean isClassVisibleToScript(boolean trusted, String className) {
                        if ("org.mozilla.javascript.JavaScriptException".equals(className)) {
                            return true;
                        }
                        return trusted;
                    }
                };
                try {
                    JSRhinoPermissionRestricter.THREAD_JSSHUTTER.put(Thread.currentThread(), jsShutter);
                    final ScriptEngineManager manager = org.jdownloader.scripting.JavaScriptEngineFactory.getScriptEngineManager(this);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    final String js = "var calculate" + function + " var result=calculate(\"" + input + "\")";
                    engine.eval(js);
                    final String result = StringUtils.valueOfOrNull(engine.get("result"));
                    if (result != null) {
                        output = result;
                        if (result.startsWith("enhanced_except")) {
                            throw new Exception("Invalid result:" + result);
                        } else {
                            synchronized (jsCache) {
                                jsCache.put(resultCacheKey, output);
                            }
                        }
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                } finally {
                    JSRhinoPermissionRestricter.THREAD_JSSHUTTER.remove(Thread.currentThread());
                }
            }
            logger.info("nsig(" + vid.videoID + "," + playerID + "):" + input + "->" + output + "(cached:" + (cachedResult != null) + ")");
        }
        return output;
    }

    private String getPlayerID(String playerJS) {
        final String ret = new Regex(playerJS, "/player/([^/]+)/player_").getMatch(0);
        if (ret != null) {
            return ret;
        } else {
            return playerJS;
        }
    }

    String descrambleSignatureNew(final String sig) throws IOException, PluginException {
        if (sig == null) {
            return null;
        }
        final String playerID = getPlayerID(html5PlayerJs);
        final String resultCacheKey = "descrambleSignatureNew/" + playerID + "/";
        String all = null;
        String descrambler = null;
        String des = null;
        synchronized (jsCache) {
            all = jsCache.get(resultCacheKey + "all");
            descrambler = jsCache.get(resultCacheKey + "descrambler");
            des = jsCache.get(resultCacheKey + "des");
            if (descrambler == null) {
                final String html5PlayerSource = ensurePlayerSource();
                if (descrambler == null) {
                    descrambler = new Regex(html5PlayerSource, "\"signature\"\\s*,\\s*([\\$\\w]+)\\([\\$\\w\\.]+\\s*\\)\\s*\\)(\\s*\\)\\s*){0,};").getMatch(0);
                    if (descrambler == null) {
                        descrambler = new Regex(html5PlayerSource, "(?:^|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2})\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)").getMatch(0);
                        if (descrambler == null) {
                            descrambler = new Regex(html5PlayerSource, "([a-zA-Z0-9$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)").getMatch(0);
                            if (descrambler == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                    }
                }
                logger.info("FunctionName:" + descrambler);
                final String func = "(?<!\\.)" + Pattern.quote(descrambler) + "\\s*=\\s*function\\(([^)]+)\\)\\{(.+?return.*?)\\};";
                des = new Regex(html5PlayerSource, Pattern.compile(func, Pattern.DOTALL)).getMatch(1);
                all = new Regex(html5PlayerSource, Pattern.compile(func, Pattern.DOTALL)).getMatch(-1);
                logger.info("Function:" + all);
                if (all == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String requiredObjectName = new Regex(des, "([\\w\\d\\$]+)\\.([\\w\\d]{2})\\(").getMatch(0);
                if (requiredObjectName != null) {
                    final String requiredObject = new Regex(html5PlayerSource, Pattern.compile("var " + Pattern.quote(requiredObjectName) + "=\\{.*?\\}\\};", Pattern.DOTALL)).getMatch(-1);
                    if (requiredObject == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Missing object:" + requiredObject);
                    }
                    all += requiredObject;
                }
                all += ";";
                logger.info("Complete Function:" + all);
                jsCache.put(resultCacheKey + "all", all);
                jsCache.put(resultCacheKey + "descrambler", descrambler);
                jsCache.put(resultCacheKey + "des", des);
            }
        }
        while (true) {
            try {
                final ScriptEngineManager manager = org.jdownloader.scripting.JavaScriptEngineFactory.getScriptEngineManager(this);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                final String debugJS = all + " " + descrambler + "(\"" + sig + "\")";
                final String result = StringUtils.valueOfOrNull(engine.eval(debugJS));
                if (result != null) {
                    logger.info("sig1(" + vid.videoID + "," + playerID + "):" + sig + "->" + result);
                    return result;
                }
            } catch (final Throwable e) {
                if (e.getMessage() != null) {
                    // do not use language components of the error message. Only static identifies, otherwise other languages will fail!
                    // -raztoki
                    final String ee = new Regex(e.getMessage(), "ReferenceError: \"([\\$\\w]+)\".+<Unknown source>").getMatch(0);
                    // should only be needed on the first entry, then on after 'cache' should get result the first time!
                    if (ee != null) {
                        String html5PlayerSource = ensurePlayerSource();
                        // lets look for missing reference
                        final String ref = new Regex(html5PlayerSource, "var\\s+" + Pattern.quote(ee) + "\\s*=\\s*\\{.*?\\};").getMatch(-1);
                        if (ref != null) {
                            all = ref + "\r\n" + all;
                            continue;
                        } else {
                            logger.warning("Could not find missing var/function");
                        }
                    } else {
                        logger.warning("Could not find reference Error");
                    }
                }
                logger.log(e);
            }
            break;
        }
        String s = sig;
        try {
            logger.info("Des: " + des);
            final String[] t = new Regex(des, "[^;]+").getColumn(-1);
            logger.info("Des " + Arrays.toString(t));
            for (final String line : t) {
                s = YoutubeHelper.handleRule(s, line);
            }
            logger.info("sig2(" + vid.videoID + "," + playerID + "):" + sig + "->" + s);
            return s;
        } catch (final PluginException e) {
            logger.log(e);
            throw e;
        }
    }

    private static final Map<String, String> PLAYERJS_CACHE = new HashMap<String, String>();

    private String ensurePlayerSource() throws IOException {
        final String html5PlayerJs = this.html5PlayerJs;
        if (html5PlayerJs == null) {
            throw new IOException("no html5 player js");
        }
        final String key = getPlayerID(html5PlayerJs);
        synchronized (PLAYERJS_CACHE) {
            String ret = PLAYERJS_CACHE.get(key);
            if (ret == null) {
                ret = br.cloneBrowser().getPage(html5PlayerJs);
                PLAYERJS_CACHE.put(key, ret);
            }
            return ret;
        }
    }

    protected YoutubeClipData extractData(YoutubeClipData vid) {
        parseTitle(vid);
        parseDescription(vid);
        parsePublishedDate(vid);
        parseUploadedDate(vid);
        parseLivestreamInformation(vid);
        parseChannelID(vid);
        parseDuration(vid);
        parseChannelTitle(vid);
        parseUser(vid);
        parseAtID(vid);
        parseViews(vid);
        parseCategory(vid);
        return vid;
    }

    protected String parseViews(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.views)) {
            vid.views = parseViewsFromMaps(vid);
        }
        return vid.views;
    }

    protected String parseTitle(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.title)) {
            final String[] titles = getVidTitleFromMaps();
            if (titles != null && titles.length > 0) {
                vid.title = titles[0];
                if (titles.length > 1) {
                    vid.title_alternative = titles[1];
                }
            }
            if (StringUtils.isEmpty(vid.title)) {
                final String match = br.getRegex("document\\.title\\s*=\\s*\"(.*?) - YouTube\"").getMatch(0);
                if (StringUtils.isNotEmpty(match)) {
                    vid.title = Encoding.htmlDecode(match.replaceAll("\\+", " ").trim());
                }
            }
        }
        return vid.title;
    }

    protected String parseDescription(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.description)) {
            final String[] descriptions = getVidDescriptionFromMaps();
            if (descriptions != null && descriptions.length > 0) {
                vid.description = descriptions[0];
                if (descriptions.length > 1) {
                    vid.description_alternative = descriptions[1];
                }
            }
        }
        return vid.description;
    }

    protected long parsePublishedDate(YoutubeClipData vid) {
        if (vid.datePublished <= 0) {
            vid.datePublished = getPublishedDateFromMaps();
        }
        return vid.datePublished;
    }

    protected long parseUploadedDate(YoutubeClipData vid) {
        if (vid.dateUploaded <= 0) {
            vid.dateUploaded = getUploadedDateFromMaps();
        }
        return vid.dateUploaded;
    }

    protected void parseLivestreamInformation(YoutubeClipData vid) {
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            final Map<String, Object> liveBroadcastDetails = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/liveBroadcastDetails");
            if (liveBroadcastDetails != null) {
                vid.isLiveNow = (Boolean) liveBroadcastDetails.get("isLiveNow");
                final String startTimestampStr = (String) liveBroadcastDetails.get("startTimestamp");
                if (startTimestampStr != null) {
                    vid.dateLivestreamStart = TimeFormatter.getMilliSeconds(startTimestampStr, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                }
                final String endTimestampStr = (String) liveBroadcastDetails.get("endTimestamp");
                if (endTimestampStr != null) {
                    vid.dateLivestreamEnd = TimeFormatter.getMilliSeconds(endTimestampStr, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                }
            }
        }
        if (vid.dateUploaded <= 0) {
            vid.dateUploaded = getUploadedDateFromMaps();
        }
    }

    protected String parseChannelID(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.channelID)) {
            vid.channelID = getChannelIdFromMaps();
        }
        return vid.channelID;
    }

    protected int parseDuration(YoutubeClipData vid) {
        if (vid.duration <= 0) {
            vid.duration = getVidDurationFromMaps();
            if (vid.duration <= 0) {
                final String match = br.getRegex("\"length_seconds\"\\s*:\\s*(\\d+)").getMatch(0);
                if (StringUtils.isNotEmpty(match)) {
                    vid.duration = Integer.parseInt(match);
                }
            }
        }
        return vid.duration;
    }

    protected String parseUser(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.user)) {
            final String[] users = getUserFromMaps();
            if (users != null && users.length > 0) {
                vid.user = users[0];
                if (users.length > 1) {
                    vid.user_alternative = users[1];
                }
            }
            if (StringUtils.isEmpty(vid.user)) {
                final String match = br.getRegex("temprop=\"url\" href=\"https?://(www\\.)?youtube\\.com/(?:user/|@)([^<>\"\\?]+)\"").getMatch(1);
                // getVideoInfoWorkaroundUsed
                final String vidWorkAround = br.getRegex("&author=(.*?)&").getMatch(0);
                if (StringUtils.isNotEmpty(match)) {
                    vid.user = Encoding.htmlDecode(match.trim());
                } else if (vid.channelTitle != null) {
                    vid.user = vid.channelTitle;
                } else if (StringUtils.isNotEmpty(vidWorkAround)) {
                    vid.user = vidWorkAround;
                }
            }
        }
        return vid.user;
    }

    protected String parseAtID(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.atID)) {
            final Map<String, Object> map = getYtInitialPlayerResponse();
            if (map != null) {
                final String ownerProfileUrl = (String) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/ownerProfileUrl");
                vid.atID = new Regex(ownerProfileUrl, "youtube.com/@([^\\?/]+)").getMatch(0);
            }
        }
        return vid.atID;
    }

    protected String parseCategory(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.category)) {
            vid.category = getCategoryFromMaps();
        }
        return vid.category;
    }

    protected String parseChannelTitle(YoutubeClipData vid) {
        if (StringUtils.isEmpty(vid.channelTitle)) {
            final String[] titles = getChannelTitleFromMaps();
            if (titles != null && titles.length > 0) {
                vid.channelTitle = titles[0];
                if (titles.length > 1) {
                    vid.channelTitle_alternative = titles[1];
                }
            }
            if (StringUtils.isEmpty(vid.channelTitle)) {
                String match = br.getRegex("<div class=\"yt-user-info\"><a [^>]*data-name[^>]*>(.*?)</a>").getMatch(0);
                if (StringUtils.isEmpty(match) && StringUtils.isNotEmpty(vid.channelID)) {
                    // content warning regex.
                    match = br.getRegex("<div class=\"yt-user-info\">\\s*<a [^>]+ data-ytid=\"" + Pattern.quote(vid.channelID) + "\"[^>]*>(.*?)</a>").getMatch(0);
                }
                if (StringUtils.isNotEmpty(match)) {
                    vid.channelTitle = Encoding.htmlDecode(match.trim());
                }
            }
        }
        return vid.channelTitle;
    }

    public String parseViewsFromMaps(YoutubeClipData vid) {
        String result = null;
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (StringUtils.isEmpty(result) && map != null) {
            final Object viewCount = JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/viewCount");
            if (viewCount != null) {
                result = viewCount.toString();
            }
        }
        map = getYtInitialData();
        if (StringUtils.isEmpty(result) && map != null) {
            result = (String) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoPrimaryInfoRenderer/viewCount/videoViewCountRenderer/shortViewCount/simpleText");
        }
        return result;
    }

    public String[] getVidDescriptionFromMaps() {
        final List<String> ret = new ArrayList<String>();
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            final String shortDescription = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/shortDescription");
            if (shortDescription != null) {
                ret.add(shortDescription);
            }
        }
        map = getYtInitialData();
        if (map != null) {
            // this one is super long and more complicated!
            final List<Object> tmp = (List<Object>) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoSecondaryInfoRenderer/description/runs");
            if (tmp != null) {
                // Construct the "text"
                final StringBuilder sb = new StringBuilder();
                for (final Object t : tmp) {
                    final Map<String, Object> o = (Map<String, Object>) t;
                    final String url = (String) JavaScriptEngineFactory.walkJson(o, "navigationEndpoint/urlEndpoint/url");
                    final String text = (String) o.get("text");
                    if (text != null) {
                        if (url != null) {
                            try {
                                sb.append(br.getURL(url).toString());
                            } catch (IOException e) {
                                sb.append(url);
                            }
                        } else {
                            sb.append(text);
                        }
                    }
                }
                if (sb.length() > 0) {
                    final String description = sb.toString();
                    if (description != null && !ret.contains(description)) {
                        ret.add(description);
                    }
                }
            }
        }
        return ret.toArray(new String[0]);
    }

    public int getVidDurationFromMaps() {
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            final String tmp = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/lengthSeconds");
            if (tmp != null) {
                return Integer.parseInt(tmp);
            }
        }
        return -1;
    }

    public String[] getChannelTitleFromMaps() {
        final List<String> ret = new ArrayList<String>();
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            String title = (String) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/ownerChannelName");
            if (StringUtils.isNotEmpty(title) && !ret.contains(title)) {
                ret.add(title);
            }
            title = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/author");
            if (StringUtils.isNotEmpty(title) && !ret.contains(title)) {
                ret.add(title);
            }
        }
        map = getYtInitialData();
        if (map != null) {
            String title = "";
            final List<Map<String, Object>> titleRuns = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoSecondaryInfoRenderer/owner/videoOwnerRenderer/title/runs/");
            if (titleRuns != null) {
                for (Map<String, Object> titleRun : titleRuns) {
                    final String text = (String) titleRun.get("text");
                    if (StringUtils.isNotEmpty(text)) {
                        title = title + text;
                    }
                }
            }
            if (StringUtils.isNotEmpty(title) && !ret.contains(title)) {
                ret.add(title);
            }
        }
        map = getYtPlayerConfig();
        if (map != null) {
            final String title = (String) JavaScriptEngineFactory.walkJson(map, "args/author");
            if (StringUtils.isNotEmpty(title) && !ret.contains(title)) {
                ret.add(title);
            }
        }
        return ret.toArray(new String[0]);
    }

    public String getCategoryFromMaps() {
        String category = null;
        final Map<String, Object> map = getYtInitialPlayerResponse();
        if (StringUtils.isEmpty(category) && map != null) {
            category = (String) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/category");
        }
        return category;
    }

    public long getPublishedDateFromMaps() {
        String publishedDate = null;
        final Map<String, Object> map = getYtInitialPlayerResponse();
        if (StringUtils.isEmpty(publishedDate) && map != null) {
            publishedDate = (String) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/publishDate");
        }
        return parseDate(publishedDate);
    }

    public long getUploadedDateFromMaps() {
        String uploadedDate = null;
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            uploadedDate = (String) JavaScriptEngineFactory.walkJson(map, "microformat/playerMicroformatRenderer/uploadDate");
        }
        map = getYtInitialData();
        if (StringUtils.isEmpty(uploadedDate) && map != null) {
            uploadedDate = (String) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoPrimaryInfoRenderer/dateText/simpleText");
            if (StringUtils.isEmpty(uploadedDate)) {
                uploadedDate = (String) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoSecondaryInfoRenderer/dateText/simpleText");
            }
        }
        return parseDate(uploadedDate);
    }

    private long parseDate(String dateString) {
        if (dateString != null) {
            // time. just parse for the date pattern(s).
            String date = new Regex(dateString, "([A-Za-z]+ \\d+, \\d{4})").getMatch(0);
            if (date != null) {
                // seen in MMM dd, yyyy
                final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                try {
                    final long result = formatter.parse(date).getTime();
                    logger.info("Date(" + dateString + ") result " + result + " " + new Date(result));
                    return result;
                } catch (final Exception e) {
                    logger.log(e);
                }
            } else if (new Regex(dateString, "\\d{4}-\\d{2}-\\d{2}").matches()) {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy'-'MM'-'dd", Locale.ENGLISH);
                try {
                    final long result = formatter.parse(dateString).getTime();
                    logger.info("Date(" + dateString + ") result " + result + " " + new Date(result));
                    return result;
                } catch (final Exception e) {
                    logger.log(e);
                }
            } else if (new Regex(dateString, "\\d+\\s*(?:days?|hours?|minutes?|seconds?)").matches()) {
                // Streamed live 3 hours ago
                /*
                 * streamed today.. x hours minutes etc. to keep it universal just show a day reference like above. parse, then construct
                 * relative to users time. It should be equal to above as
                 */
                final String tmpdays = new Regex(dateString, "(\\d+)\\s+days?").getMatch(0);
                final String tmphrs = new Regex(dateString, "(\\d+)\\s+hours?").getMatch(0);
                final String tmpmin = new Regex(dateString, "(\\d+)\\s+minutes?").getMatch(0);
                final String tmpsec = new Regex(dateString, "(\\d+)\\s+seconds?").getMatch(0);
                long days = 0, hours = 0, minutes = 0, seconds = 0;
                if (StringUtils.isNotEmpty(tmpdays)) {
                    days = Integer.parseInt(tmpdays);
                }
                if (StringUtils.isNotEmpty(tmphrs)) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (StringUtils.isNotEmpty(tmpmin)) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (StringUtils.isNotEmpty(tmpsec)) {
                    seconds = Integer.parseInt(tmpsec);
                }
                final long time = System.currentTimeMillis() - ((days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                final Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                final long result = c.getTimeInMillis();
                logger.info("Date(" + dateString + ") result " + result + " " + new Date(result));
                return result;
            } else {
                logger.info("Unknown date format:" + dateString);
            }
        }
        return -1;
    }

    public String[] getVidTitleFromMaps() {
        final List<String> titles = new ArrayList<String>();
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            final String title = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/title");
            if (StringUtils.isNotEmpty(title)) {
                titles.add(title);
            }
        }
        map = getYtInitialData();
        if (map != null) {
            String title = (String) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoPrimaryInfoRenderer/title/simpleText");
            if (StringUtils.isNotEmpty(title) && !titles.contains(title)) {
                titles.add(title);
            }
            final List<Map<String, Object>> titleRuns = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoPrimaryInfoRenderer/title/runs/");
            if (titleRuns != null) {
                title = "";
                for (Map<String, Object> titleRun : titleRuns) {
                    final String text = (String) titleRun.get("text");
                    if (StringUtils.isNotEmpty(text)) {
                        title = title + text;
                    }
                }
            }
            if (StringUtils.isNotEmpty(title) && !titles.contains(title)) {
                titles.add(title);
            }
        }
        map = getYtPlayerConfig();
        if (map != null) {
            final String title = (String) JavaScriptEngineFactory.walkJson(map, "args/title");
            if (StringUtils.isNotEmpty(title) && !titles.contains(title)) {
                titles.add(title);
            }
        }
        return titles.toArray(new String[0]);
    }

    public String[] getUserFromMaps() {
        final List<String> ret = new ArrayList<String>();
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            final String author = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/author");
            if (StringUtils.isNotEmpty(author) && !ret.contains(author)) {
                ret.add(author);
            }
        }
        map = getYtInitialData();
        if (map != null) {
            String author = "";
            final List<Map<String, Object>> titleRuns = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoSecondaryInfoRenderer/owner/videoOwnerRenderer/title/runs/");
            if (titleRuns != null) {
                for (Map<String, Object> titleRun : titleRuns) {
                    final String text = (String) titleRun.get("text");
                    if (StringUtils.isNotEmpty(text)) {
                        author = author + text;
                    }
                }
            }
            if (StringUtils.isNotEmpty(author) && !ret.contains(author)) {
                ret.add(author);
            }
        }
        return ret.toArray(new String[0]);
    }

    public String getChannelIdFromMaps() {
        String result = null;
        Map<String, Object> map = getYtInitialPlayerResponse();
        if (map != null) {
            result = (String) JavaScriptEngineFactory.walkJson(map, "videoDetails/channelId");
        }
        map = getYtInitialData();
        if (StringUtils.isEmpty(result) && map != null) {
            result = (String) JavaScriptEngineFactory.walkJson(map, "contents/twoColumnWatchNextResults/results/results/contents/{}/videoSecondaryInfoRenderer/owner/videoOwnerRenderer/navigationEndpoint/browseEndpoint/browseId");
        }
        map = getYtPlayerConfig();
        if (StringUtils.isEmpty(result) && map != null) {
            result = (String) JavaScriptEngineFactory.walkJson(map, "args/ucid");
        }
        return result;
    }

    protected void handleContentWarning(final Browser ibr) throws Exception {
        // not necessarily age related but violence/disturbing content?
        // https://www.youtube.com/watch?v=Wx9GxXYKx_8 gets you
        // <script>window.location = "http:\/\/www.youtube.com\/verify_controversy?next_url=\/watch%3Fv%3DWx9GxXYKx_8"</script>
        // then from verify_controversy page
        // <form action="/verify_controversy?action_confirm=1" method="POST">
        // <p>
        // <input type="hidden" name="session_token" value="UYIQVFoBkQKGHKCionXaE-OXh4Z8MTM5MzE2NTcyNUAxMzkzMDc5MzI1"/>
        // <input type="hidden" name="referrer" value="">
        // <input type="hidden" name="next_url" value="/watch?v=Wx9GxXYKx_8">
        // <button onclick=";return true;" type="submit" class=" yt-uix-button yt-uix-button-primary yt-uix-button-size-default"
        // role="button"><span class="yt-uix-button-content">Continue </span></button>
        // or <a href="/">Cancel</a>
        // </p>
        // </form>
        String vc = ibr.getRegex("\"([^\"]+verify_controversy\\?next_url[^\"]+)\"").getMatch(0);
        if (vc != null) {
            vc = JSonStorage.restoreFromString("\"" + vc + "\"", TypeRef.STRING);
            ibr.getPage(vc);
        }
        // nsfw testlink https://www.youtube.com/watch?v=p7S_u5TzI-I
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
        final Form forms[] = ibr.getForms();
        if (forms != null) {
            for (final Form form : forms) {
                if (form.getAction() != null && form.getAction().contains("verify_age")) {
                    this.logger.info("Verify Age");
                    ibr.submitForm(form);
                    break;
                }
                if (form.getAction() != null && form.getAction().contains("verify_controversy")) {
                    this.logger.info("Verify Controversy");
                    ibr.submitForm(form);
                    break;
                }
            }
        }
    }

    public void loadVideo(final YoutubeClipData vid) throws Exception {
        // TODO: add Cache
        refreshVideo(vid);
    }

    private boolean addYoutubeStreamData(Map<YoutubeITAG, StreamCollection> map, YoutubeStreamData match) {
        if (!isSegmentLoadingAllowed(match)) {
            logger.info("itag not allowed(segments):" + match.toString());
            return false;
        } else if (!cfg.isExternMultimediaToolUsageEnabled() && match.getItag().name().contains("DASH_")) {
            logger.info("itag not allowed(dash):" + match.toString());
            return false;
        } else {
            logger.info("add(" + vid.videoID + "/" + getPlayerID(html5PlayerJs) + "):" + match.toString());
            StreamCollection lst = map.get(match.getItag());
            if (lst == null) {
                lst = new StreamCollection();
                map.put(match.getItag(), lst);
            }
            lst.add(match);
            return true;
        }
    }

    public Request getPage(Browser br, final String url) throws Exception {
        final GetRequest getRequest = br.createGetRequest(url);
        return getPage(br, getRequest);
    }

    protected boolean isAPIPrefered(Browser br) {
        final String serializedExperimentIdsString = br.getRegex("\"serializedExperimentIds\"\\s*:\\s*\"([0-9 ,]+)\"").getMatch(0);
        if (serializedExperimentIdsString != null) {
            final List<String> serializedExperimentIds = Arrays.asList(serializedExperimentIdsString.split(","));
            if (serializedExperimentIds.contains("51217476") || serializedExperimentIds.contains("51217102")) {
                return true;
            }
        }
        return false;
    }

    public Request getPage(Browser br, Request request) throws Exception {
        if (this.br != br) {
            setBr(br);
        }
        br.setCookie("youtube.com", "hideBrowserUpgradeBox", "true");
        br.getPage(request);
        if (br.getURL().matches(".*/supported_browsers\\?next.+")) {
            br.getPage(request.cloneRequest());
            if (br.getURL().matches(".*/supported_browsers\\?next.+")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (handleConsent(br)) {
            br.getPage(request.cloneRequest());
            if (handleConsent(br)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too Many Requests", 10 * 60 * 1000l);
        } else {
            if (isAPIPrefered(br) && false) {
                // retry web mode with different UA
                final String ua = request.getHeaders().getValue(HTTPConstants.HEADER_REQUEST_UPGRADE_INSECURE_REQUESTS);
                if (!StringUtils.containsIgnoreCase(ua, "com.google.ios.youtube")) {
                    br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)");
                    return getPage(br, request.cloneRequest());
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return br.getRequest();
    }

    public void setConsentCookie(final Browser browser, String id) {
        if (StringUtils.isEmpty(id)) {
            id = String.valueOf(new Random().nextInt(899) + 100);
        }
        String host = br.getHost();
        if (host == null) {
            host = "youtube.com";
        }
        // br.setCookie(host, "CONSENT", "YES+cb.20210328-17-p0.en+FX+" + id);
        // br.setCookie(host, "CONSENT", "YES+cb.20210328-17-p0.en+F+" + id);
        br.setCookie(host, "SOCS", "CAI");
    }

    private static volatile boolean CONSENT_COOKIE_REQUIRED = true;

    public boolean isConsentCookieRequired() {
        return CONSENT_COOKIE_REQUIRED;
    }

    protected boolean handleConsent(Browser br) throws Exception {
        final String consentRequired = br.getCookie(br.getHost(), "CONSENT");
        final String consentCookie = br.getCookie(br.getHost(), "SOCS");
        if (consentRequired != null || consentCookie != null) {
            if (!StringUtils.startsWithCaseInsensitive(consentCookie, "CAA")) {
                return false;
            } else if (StringUtils.startsWithCaseInsensitive(consentRequired, "YES")) {
                return false;
            } else {
                CONSENT_COOKIE_REQUIRED = true;
                final String id = new Regex(consentRequired, "PENDING\\+(.+)").getMatch(0);
                setConsentCookie(br, id);
                return true;
            }
        }
        return false;
    }

    public void refreshVideo(final YoutubeClipData vid) throws Exception {
        account = login(logger, false);
        this.vid = vid;
        final Map<YoutubeITAG, StreamCollection> ret = new HashMap<YoutubeITAG, StreamCollection>();
        final YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        br.setFollowRedirects(true);
        /* this cookie makes html5 available and skip controversy check */
        prepareBrowser(br);
        // cookie for new Style(Polymer?)
        // br.setCookie("youtube.com", "VISITOR_INFO1_LIVE", "Qa1hUZu3gtk");
        br.addAllowedResponseCodes(429);
        // &disable_polymer=true is causing missing file information, check/update handling
        getPage(br, base + "/watch?bpctr=9999999999&has_verified=1&hl=en&v=" + vid.videoID + "&gl=US");
        parse();
        vid.approxThreedLayout = br.getRegex("\"approx_threed_layout\"\\s*\\:\\s*\"([^\"]*)").getMatch(0);
        String[][] keyWordsGrid = br.getRegex("<meta\\s+property=\"([^\"]*)\"\\s+content=\"yt3d\\:([^\"]+)=([^\"]+)\">").getMatches();
        vid.keywords3D = new HashMap<String, String>();
        if (keyWordsGrid != null) {
            for (String[] keyValue : keyWordsGrid) {
                vid.keywords3D.put(keyValue[1], keyValue[2]);
            }
        }
        String keywords = br.getRegex("<meta name=\"keywords\" content=\"([^\"]*)").getMatch(0);
        vid.keywords = new HashSet<String>();
        if (keywords != null) {
            for (String s : keywords.split("[,]+")) {
                vid.keywords.add(s);
            }
        } else {
            final List<String> kws = (List<String>) JavaScriptEngineFactory.walkJson(getYtInitialPlayerResponse(), "videoDetails/keywords");
            if (kws != null) {
                for (String s : kws) {
                    vid.keywords.add(s);
                }
            }
        }
        handleRentalVideos();
        html5PlayerJs = getHtml5PlayerJs();
        final Map<String, Object> map = getYtInitialPlayerResponse();
        fmtMaps = new LinkedHashSet<StreamMap>();
        subtitleUrls = new LinkedHashSet<String>();
        mpdUrls = new LinkedHashSet<StreamMap>();
        videoInfo = new LinkedHashMap<String, String>();
        final String unavailableStatus = map != null ? (String) JavaScriptEngineFactory.walkJson(map, "playabilityStatus/status") : null;
        final String unavailableReason = getUnavailableReason(unavailableStatus);
        vid.ageCheck = br.containsHTML("\"status\"\\s*:\\s*\"LOGIN_REQUIRED\"");
        logger.info("Login required:" + vid.ageCheck + "|Reason:" + unavailableReason);
        this.handleContentWarning(br);
        int collected = 0;
        if (isAPIPrefered(br) || vid.ageCheck) {
            collected = collectMapsFromAPIResponse(br);
            logger.info("found collectMapsFromAPIResponse(" + vid.videoID + "):" + collected);
        }
        if (collected <= 0) {
            collected = collectMapsFromPlayerResponse(map, br.getURL());
            logger.info("found collectMapsFromPlayerResponse(" + vid.videoID + "):" + collected);
            collectMapsFormHtmlSource(br.getRequest().getHtmlCode(), "base");
        }
        // videos have data available even though they are blocked.
        extractData(vid);
        if (unavailableReason != null) {
            /*
             * If you consider using !unavailableReason.contains("this video is unavailable), you need to also ignore content warning
             */
            if (unavailableReason.contains("This video is private")) {
                // id=TY1LpddyWvs, date=20170903, author=raztoki
                // id=Vs4IJuhZ_1E, date=20170903, author=raztoki
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else if (unavailableReason.startsWith("This video does not exist")) {
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else if (unavailableReason.startsWith("This video has been removed")) {
                // currently covering
                // This video has been removed by the user. .:. ab4U0RwrOTI
                // This video has been removed because its content violated YouTube&#39;s Terms of Service. .:. 7RA4A-4QqHU
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else if (unavailableReason.contains("account associated with this video has been")) {
                // currently covering
                // This video is no longer available because the YouTube account associated with this video has been closed.
                // id=wBVhciYW9Og, date=20141222, author=raztoki
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else if ("This live event has ended.".equalsIgnoreCase(unavailableReason)) {
                // currently covering
                // This live event has ended.
                // id=qEJwOuvDf7I, date=20150412, author=raztoki
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else if (unavailableReason.contains("This video is no longer available due to a copyright claim")) {
                // currently covering
                // "One Monkey saves another Mo..."
                // This video is no longer available due to a copyright claim by ANI Media Pvt Ltd.
                // id=l8nBcj8ul7s, date=20141224, author=raztoki
                // id=6cER1kK3Qwg, date=20170903, author=raztoki
                // filename is shown in error.
                vid.title = new Regex(unavailableReason, "\"(.*?(?:\\.\\.\\.)?)\"\\n").getMatch(0);
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = "This video is no longer available due to a copyright claim";
                return;
            } else if (unavailableReason.startsWith("This video contains content from ") && unavailableReason.contains("who has blocked it in your country on copyright grounds")) {
                // not quite as the same as above.
                // This video contains content from Beta Film GmbH, who has blocked it in your country on copyright grounds.
                // id=cr8tgceA2qk, date=20170708, author=raztoki
                final String error = "Geo Blocked due to copyright grounds";
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = error;
                return;
            } else if (unavailableReason.equals("This video is unavailable.") || unavailableReason.equals(/* 15.12.2014 */"This video is not available.")) {
                // currently covering
                // Sorry about that. .:. 7BN5H7AVHUIE8 invalid uid.
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else {
                logger.warning("Continue Error:" + unavailableReason);
            }
        }
        doFeedScan();
        doUserAPIScan();
        List<YoutubeStreamData> incomplete = new ArrayList<YoutubeStreamData>();
        for (StreamMap fmt : fmtMaps) {
            if (fmt.streamData != null) {
                try {
                    final YoutubeStreamData match = fmt.streamData;
                    if (looksIncomplete(match)) {
                        incomplete.add(match);
                    } else {
                        addYoutubeStreamData(ret, match);
                    }
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else if (StringUtils.isNotEmpty(fmt.mapData)) {
                for (final String line : fmt.mapData.split(",")) {
                    try {
                        final YoutubeStreamData match = this.parseLine(Request.parseQuery(line), fmt);
                        if (looksIncomplete(match)) {
                            incomplete.add(match);
                        } else {
                            addYoutubeStreamData(ret, match);
                        }
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            }
        }
        if (incomplete.size() > 0) {
            for (YoutubeStreamData data : incomplete) {
                try {
                    if (!ret.containsKey(data.getItag())) {
                        addYoutubeStreamData(ret, data);
                    }
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        }
        if (!cfg.isExternMultimediaToolUsageEnabled()) {
            logger.info("ExternMultimediaToolUsageEnabled:disabled");
        } else {
            mpd: for (StreamMap mpdUrl : mpdUrls) {
                try {
                    if (StringUtils.isEmpty(mpdUrl.mapData)) {
                        continue;
                    }
                    final Browser clone = br.cloneBrowser();
                    clone.setFollowRedirects(true);
                    String newv = mpdUrl.mapData;
                    String scrambledSign = new Regex(mpdUrl.mapData, "/s/(.*?)/").getMatch(0);
                    if (StringUtils.isNotEmpty(scrambledSign)) {
                        scrambledSign = URLDecoder.decode(scrambledSign, "UTF-8");
                        String sign = descrambleSignature(scrambledSign);
                        newv = mpdUrl.mapData.replaceAll("/s/(.*?)/", "/signature/" + sign + "/");
                    }
                    clone.getPage(newv);
                    String xml = clone.getRequest().getHtmlCode();
                    if (!clone.getHttpConnection().isOK()) {
                        logger.severe("Bad Request: ");
                        logger.severe(clone.getHttpConnection() + "");
                        continue;
                    }
                    if (xml.trim().startsWith("#EXTM3U")) {
                        List<HlsContainer> containers = HlsContainer.getHlsQualities(clone);
                        for (HlsContainer c : containers) {
                            String[][] params = new Regex(c.getDownloadurl(), "/([^/]+)/([^/]+)").getMatches();
                            final UrlQuery query = Request.parseQuery(c.getDownloadurl());
                            if (params != null) {
                                for (int i = 1; i < params.length; i++) {
                                    query.addAndReplace(params[i][0], Encoding.htmlDecode(params[i][1]));
                                }
                            }
                            query.addIfNoAvailable("codecs", c.getCodecs());
                            query.addIfNoAvailable("type", query.get("codecs") + "-" + query.get("mime"));
                            query.addIfNoAvailable("fps", query.get("frameRate"));
                            query.addIfNoAvailable("width", c.getWidth() + "");
                            query.addIfNoAvailable("height", c.getHeight() + "");
                            if (query.containsKey("width") && query.containsKey("height")) {
                                query.addIfNoAvailable("size", query.get("width") + "x" + query.get("height"));
                            }
                            String fps = query.get("fps");
                            int projectionType = -1;
                            try {
                                String v = query.get("projection_type");
                                projectionType = v == null ? -1 : Integer.parseInt(v);
                            } catch (Throwable e) {
                                logger.log(e);
                            }
                            final String itagID = query.get("itag");
                            final YoutubeITAG itag = YoutubeITAG.get(Integer.parseInt(itagID), c.getWidth(), c.getHeight(), StringUtils.isEmpty(fps) ? -1 : Integer.parseInt(fps), query.getDecoded("type"), query, vid.datePublished);
                            if (itag == null) {
                                this.logger.info("Unknown Line: " + query);
                                this.logger.info(query + "");
                                try {
                                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !Application.isJared(null)) {
                                        Dialog.getInstance().showMessageDialog("Unknown ITag found: " + itagID + "\r\nAsk Coalado to Update the ItagHelper for Video ID: " + vid.videoID);
                                    }
                                } catch (Exception e) {
                                    logger.log(e);
                                }
                                continue;
                            } else if (Boolean.FALSE.equals(isSupported(itag))) {
                                this.logger.info("FFmpeg support for Itag'" + itag + "' is missing");
                                continue;
                            }
                            final YoutubeStreamData match = new YoutubeStreamData(mpdUrl.src, vid, c.getDownloadurl(), itag, query);
                            try {
                                match.setHeight(Integer.parseInt(query.get("height")));
                            } catch (Throwable e) {
                            }
                            try {
                                match.setWidth(Integer.parseInt(query.get("width")));
                            } catch (Throwable e) {
                            }
                            try {
                                match.setFps(query.get("fps"));
                            } catch (Throwable e) {
                            }
                            addYoutubeStreamData(ret, match);
                        }
                    } else if (dashMpdEnabled) {
                        final List<YoutubeStreamData> datas = parseDashManifest(mpdUrl.src, br, br.getURL(newv).toString());
                        for (YoutubeStreamData match : datas) {
                            addYoutubeStreamData(ret, match);
                        }
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (BrowserException e) {
                    logger.log(e);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        }
        if (unavailableReason != null) {
            if (ret.size() == 0) {
                logger.warning("Abort Error:" + unavailableReason);
                vid.error = unavailableReason;
                return;
            } else {
                logger.warning("Ignore Error:" + unavailableReason);
            }
        }
        for (YoutubeStreamData match : loadThumbnails()) {
            addYoutubeStreamData(ret, match);
        }
        for (Entry<YoutubeITAG, StreamCollection> es : ret.entrySet()) {
            Collections.sort(es.getValue(), new Comparator<YoutubeStreamData>() {
                @Override
                public int compare(YoutubeStreamData o1, YoutubeStreamData o2) {
                    int ret = CompareUtils.compareBoolean(o2.getUrl().contains("ei="), o1.getUrl().contains("ei="));
                    return ret;
                }
            });
        }
        vid.streams = ret;
        vid.subtitles = loadSubtitles();
    }

    private static boolean API_IOS_ENABLED = true;

    /**
     * https://github.com/zerodytrash/YouTube-Internal-Clients
     *
     *
     * does not return opus audio codec
     */
    protected Request buildAPI_IOS_Request(Browser br) throws Exception {
        if (!API_IOS_ENABLED) {
            logger.info("buildAPI_IOS_Request:disabled");
            return null;
        }
        final Map<String, Object> post = new LinkedHashMap<String, Object>();
        final Map<String, Object> client = new LinkedHashMap<String, Object>();
        client.put("clientName", "IOS");
        client.put("clientVersion", "19.29.1");
        client.put("deviceMake", "Apple");
        client.put("deviceModel", "iPhone16,2");
        client.put("userAgent", "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)");
        client.put("osName", "iPhone");
        client.put("osVersion", "17.5.1.21F90");
        client.put("hl", "en");
        client.put("timeZone", "UTC");
        client.put("utcOffsetMinutes", 0);
        final Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("client", client);
        post.put("context", context);
        post.put("videoId", vid.videoID);
        final Map<String, Object> contentPlaybackContext = new LinkedHashMap<String, Object>();
        contentPlaybackContext.put("html5Preferences", "HTML5_PREF_WANTS");
        final Map<String, Object> playbackContext = new LinkedHashMap<String, Object>();
        playbackContext.put("contentPlaybackContext", contentPlaybackContext);
        post.put("playbackContext", playbackContext);
        post.put("contentCheckOk", true);
        post.put("racyCheckOk", true);
        final PostRequest request = br.createJSonPostRequest("https://www.youtube.com/youtubei/v1/player?prettyPrint=false", JSonStorage.serializeToJson(post));
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, (String) client.get("userAgent"));
        request.getHeaders().put("X-Youtube-Client-Name", "5");
        request.getHeaders().put("X-Youtube-Client-Version", (String) client.get("clientVersion"));
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://www.youtube.com");
        return request;
    }

    private static boolean API_TV_ENABLED = true;

    /**
     * https://github.com/zerodytrash/YouTube-Internal-Clients
     *
     *
     * does also return opus audio codec, max video resolution is 1080p
     */
    protected Request buildAPI_TV_Request(Browser br) throws Exception {
        if (!API_TV_ENABLED) {
            logger.info("buildAPI_TV_Request:disabled");
            return null;
        }
        final Map<String, Object> post = new LinkedHashMap<String, Object>();
        final Map<String, Object> client = new LinkedHashMap<String, Object>();
        final int clientNameID;
        if (vid.ageCheck) {
            // This client can access age restricted videos (unless the uploader has disabled the 'allow embedding' option)
            client.put("clientName", "TVHTML5_SIMPLY_EMBEDDED_PLAYER");
            client.put("clientVersion", "2.0");
            clientNameID = 85;
        } else {
            client.put("clientName", "TVHTML5");
            client.put("clientVersion", "7.20240724.13.00");
            clientNameID = 7;
        }
        client.put("hl", "en");
        client.put("timeZone", "UTC");
        client.put("utcOffsetMinutes", 0);
        final Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("client", client);
        post.put("context", context);
        post.put("videoId", vid.videoID);
        final Map<String, Object> contentPlaybackContext = new LinkedHashMap<String, Object>();
        contentPlaybackContext.put("html5Preferences", "HTML5_PREF_WANTS");
        final String sts = getSts();
        if (sts == null) {
            return null;
        }
        contentPlaybackContext.put("signatureTimestamp", Integer.parseInt(sts));
        final Map<String, Object> playbackContext = new LinkedHashMap<String, Object>();
        playbackContext.put("contentPlaybackContext", contentPlaybackContext);
        post.put("playbackContext", playbackContext);
        post.put("contentCheckOk", true);
        post.put("racyCheckOk", true);
        final PostRequest request = br.createJSonPostRequest("https://www.youtube.com/youtubei/v1/player?prettyPrint=false", JSonStorage.serializeToJson(post));
        request.getHeaders().put("X-Youtube-Client-Name", Integer.toString(clientNameID));
        request.getHeaders().put("X-Youtube-Client-Version", (String) client.get("clientVersion"));
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://www.youtube.com");
        return request;
    }

    protected int collectMapsFromAPIResponse(Browser br) throws InterruptedException, Exception {
        if (!API_IOS_ENABLED && !API_TV_ENABLED) {
            logger.info("collectMapsFromAPIResponse:disabled");
            return -1;
        }
        int ret = 0;
        try {
            final Browser brc = br.cloneBrowser();
            final Request request = buildAPI_TV_Request(brc);
            if (request != null) {
                brc.getPage(request);
                if (brc.getRegex("\"status\"\\s*:\\s*\"LOGIN_REQUIRED\"").patternFind()) {
                    // skip parsing
                } else if (request.getHttpConnection().getResponseCode() == 200) {
                    final Map<String, Object> response = JSonStorage.restoreFromString(request.getHtmlCode(), TypeRef.MAP);
                    ret += collectMapsFromPlayerResponse(response, "api.tv");
                    if (ret > 0 && true) {
                        // tv client returns more than ios client, so we can stop here
                        return ret;
                    }
                } else {
                    throw new Exception("auto disable api due to unexpected responseCode:" + request.getHttpConnection().getResponseCode());
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            API_TV_ENABLED = false;
            logger.log(e);
        }
        if (!vid.ageCheck) {
            try {
                final Browser brc = br.cloneBrowser();
                final Request request = buildAPI_IOS_Request(brc);
                if (request != null) {
                    brc.getPage(request);
                    if (brc.getRegex("\"status\"\\s*:\\s*\"LOGIN_REQUIRED\"").patternFind()) {
                        // skip parsing
                    } else if (request.getHttpConnection().getResponseCode() == 200) {
                        final Map<String, Object> response = JSonStorage.restoreFromString(request.getHtmlCode(), TypeRef.MAP);
                        ret += collectMapsFromPlayerResponse(response, "api.ios");
                    } else {
                        throw new Exception("auto disable api due to unexpected responseCode:" + request.getHttpConnection().getResponseCode());
                    }
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                API_IOS_ENABLED = false;
                logger.log(e);
            }
        }
        return ret;
    }

    private boolean looksIncomplete(YoutubeStreamData data) {
        if (data.getSegments() == null && (data.getContentLength() < 0 && data.estimatedContentLength() < 0)) {
            final boolean isLive = StringUtils.containsIgnoreCase(data.getUrl(), "live=1");
            logger.info("looksIncomplete:" + data + "|live:" + isLive);
            return true;
        } else {
            return false;
        }
    }

    private String getSts() throws Exception {
        final String player = ensurePlayerSource();
        final String sts = new Regex(player, "sts\\s*(?::|=)\\s*(?:\")(\\d+)").getMatch(0);
        return sts;
    }

    private String getHtml5PlayerJs() throws IOException {
        final Map<String, Object> ytPlayerConfig = getYtPlayerConfig();
        String ret = (String) JavaScriptEngineFactory.walkJson(ytPlayerConfig, "assets/js");
        if (ret == null) {
            ret = (String) JavaScriptEngineFactory.walkJson(ytPlayerConfig, "jsUrl");
        }
        if (ret != null) {
            ret = ret.replace("\\/", "/");
            return br.getURL(ret).toExternalForm();
        }
        ret = br.getMatch("\"movie_player\"\\s*,\\s*\"jsUrl\"\\s*:\\s*\"([^\"<>]*?/base.js)\"[^<>]*n");
        if (ret == null) {
            ret = br.getMatch("src\\s*=\\s*\"((https?:)?//[^\"<>]*?/base.js)\"[^<>]*name=\"player\\\\?/base");
            if (ret == null) {
                ret = br.getMatch("src\\s*=\\s*\"([^\"<>]*?/base.js)\"[^<>]*n");
            }
        }
        if (ret != null) {
            return br.getURL(ret).toExternalForm();
        } else {
            return null;
        }
    }

    /**
     * ERROR <br />
     * LOGIN_REQUIRED <br />
     * UNPLAYABLE <br />
     *
     * @param unavailableStatus
     * @return
     * @author raztoki
     */
    private String getUnavailableReason(String unavailableStatus) {
        String result = null;
        if (StringUtils.isEmpty(unavailableStatus) || "OK".equals(unavailableStatus)) {
            return null;
        }
        if ("LOGIN_REQUIRED".equals(unavailableStatus)) {
            result = (String) JavaScriptEngineFactory.walkJson(getYtInitialPlayerResponse(), "playabilityStatus/errorScreen/playerErrorMessageRenderer/reason/simpleText");
        } else {
            // this covers "ERROR" and "UNPLAYABLE", probably covers others too. so make it future proof.
            result = (String) JavaScriptEngineFactory.walkJson(getYtInitialPlayerResponse(), "playabilityStatus/reason");
        }
        return result;
    }

    private boolean isSegmentLoadingAllowed(YoutubeStreamData match) {
        if (match == null) {
            return false;
        } else if (!cfg.isSegmentLoadingEnabled()) {
            if (match.getSegments() != null && match.getSegments().length > 0) {
                return false;
            } else if (match.getUrl() != null && match.getUrl().contains("hls_playlist")) {
                return false;
            }
        }
        return true;
    }

    private DocumentBuilder createXMLParser() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = XML.newSecureFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        docBuilder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return null;
            }
        });
        docBuilder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                logger.log(exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                logger.log(exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                logger.log(exception);
            }
        });
        return docBuilder;
    }

    private void collectFmtMap(String htmlCode, String regex, String src) {
        String map = new Regex(htmlCode, regex).getMatch(0);
        if (map != null) {
            map = JSonStorage.restoreFromString(map, TypeRef.STRING);
            if (StringUtils.isNotEmpty(map)) {
                // map = Encoding.urlDecode(map, false);
                if (!fmtMaps.contains(map)) {
                    fmtMaps.add(new StreamMap(map, src));
                }
            }
        }
    }

    private void collectMapsFormHtmlSource(String html, String src) {
        if (subtitleUrls.size() == 0) {
            String captionTracks = new Regex(html, "captionTracks\\\\\"\\s*:(\\[.*?\\])\\s*,").getMatch(0);
            if (captionTracks != null) {
                String decoded = captionTracks.replaceAll("\\\\u", "\\u");
                decoded = Encoding.unicodeDecode(decoded).replaceAll("\\\\", "");
                subtitleUrls.add(decoded);
            }
            collectSubtitleUrls(html, "['\"]TTS_URL['\"]\\s*:\\s*(['\"][^'\"]+['\"])");
        }
        if (fmtMapEnabled) {
            collectFmtMap(html, REGEX_FMT_MAP_FROM_JSPLAYER_SETUP, "fmtMapJSPlayer." + src);
        }
        if (adaptiveFmtsEnabled) {
            collectFmtMap(html, REGEX_ADAPTIVE_FMTS_FROM_JSPLAYER_SETUP, "adaptiveFmtsJSPlayer." + src);
        }
        if (dashMpdEnabled) {
            collectMpdMap(html, REGEX_DASHMPD_FROM_JSPLAYER_SETUP, "dashMpdJSPlayer." + src);
        }
        if (hlsEnabled) {
            collectMpdMap(html, REGEX_HLSMPD_FROM_JSPLAYER_SETUP, "hlsJSPlayer." + src);
        }
    }

    private boolean collectSubtitleUrls(String html, String pattern) {
        String json = new Regex(html, pattern).getMatch(0);
        if (json != null) {
            json = JSonStorage.restoreFromString(json, TypeRef.STRING);
            if (json != null) {
                subtitleUrls.add(json);
                return true;
            }
        }
        return false;
    }

    /** Wrapper */
    public YoutubeStreamData convert(final UrlQuery query, final String src) {
        final Map<String, Object> qualityMap = new HashMap<String, Object>();
        for (final Entry<String, String> es : query.toMap(true).entrySet()) {
            qualityMap.put(es.getKey(), es.getValue());
        }
        return this.convert(qualityMap, src);
    }

    public YoutubeStreamData convert(Map<String, Object> entry, final String src) {
        if (entry != null) {
            if (entry.containsKey("drm_families") || entry.containsKey("drmFamilies")) {
                logger.info("DRM?:" + JSonStorage.toString(entry));
                return null;
            }
            String url = (String) entry.get("url");
            int throttle = -1;
            try {
                if (StringUtils.isEmpty(url)) {
                    String cipher = (String) entry.get("cipher");
                    if (cipher == null) {
                        // 28.05.2020
                        cipher = (String) entry.get("signatureCipher");
                    }
                    final UrlQuery query = UrlQuery.parse(cipher);
                    String queryURL = query.get("url");
                    if (StringUtils.isEmpty(queryURL)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    queryURL = URLDecoder.decode(queryURL, "UTF-8");
                    if (queryURL.contains("&n=")) {
                        throttle = 0;
                        final String value = new Regex(queryURL, "&n=(.*?)(&|$)").getMatch(0);
                        final String result = descrambleThrottle(value);
                        if (result != null && !result.equals(value)) {
                            queryURL = queryURL.replaceFirst("(&n=" + value + ")", "&n=" + result);
                            throttle = 1;
                        }
                    }
                    if (query.containsKey("signature")) {
                        url = queryURL + "&signature=" + query.get("signature");
                    } else if (query.containsKey("sig")) {
                        url = queryURL + "&signature=" + query.get("sig");
                    } else if (query.containsKey("s")) {
                        String encrypted_sig = query.get("s");
                        encrypted_sig = URLDecoder.decode(encrypted_sig, "UTF-8");
                        final String signature = this.descrambleSignature(encrypted_sig);
                        if (query.containsKey("sp")) {
                            url = queryURL + "&" + query.get("sp") + "=" + Encoding.urlEncode(signature);
                        } else {
                            url = queryURL + "&signature=" + Encoding.urlEncode(signature);
                        }
                    }
                } else if (url.contains("&n=")) {
                    throttle = 0;
                    final String value = new Regex(url, "&n=(.*?)(&|$)").getMatch(0);
                    final String result = descrambleThrottle(value);
                    if (result != null && !result.equals(value)) {
                        url = url.replaceFirst("(&n=" + value + ")", "&n=" + result);
                        throttle = 1;
                    }
                }
            } catch (PluginException e) {
                logger.log(e);
                return null;
            } catch (IOException e) {
                logger.log(e);
                return null;
            }
            if (StringUtils.isEmpty(url)) {
                logger.info("URL?:" + JSonStorage.toString(entry));
                return null;
            }
            final String type = (String) entry.get("type");
            if (StringUtils.equalsIgnoreCase("FORMAT_STREAM_TYPE_OTF", type)) {
                logger.info("UNSUPPORTED OTF:" + JSonStorage.toString(entry));
                return null;
            }
            final Long width = JavaScriptEngineFactory.toLong(entry.get("width"), -1);
            final Long height = JavaScriptEngineFactory.toLong(entry.get("height"), -1);
            final Long contentLength = JavaScriptEngineFactory.toLong(entry.get("contentLength"), -1);
            final Long bitrate = JavaScriptEngineFactory.toLong(entry.get("bitrate"), -1);
            final Long averageBitrate = JavaScriptEngineFactory.toLong(entry.get("averageBitrate"), -1);
            final Long approxDurationMs = JavaScriptEngineFactory.toLong(entry.get("approxDurationMs"), -1);
            final Long itagID = JavaScriptEngineFactory.toLong(entry.get("itag"), -1);
            final Long fps = JavaScriptEngineFactory.toLong(entry.get("fps"), -1);
            final String stereoLayout = (String) entry.get("stereoLayout");
            final String projectionType = (String) entry.get("projectionType");
            long datePublished = -1;
            if (this.vid != null) {
                datePublished = vid.datePublished;
            }
            final YoutubeITAG itag = YoutubeITAG.get(itagID.intValue(), width.intValue(), height.intValue(), fps.intValue(), null, null, datePublished);
            if (itag == null) {
                logger.info("UNSUPPORTED/UNKNOWN?:" + JSonStorage.toString(entry));
                try {
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !Application.isJared(null)) {
                        Dialog.getInstance().showMessageDialog("Unknown ITag found: " + itagID + "\r\nAsk Coalado to Update the ItagHelper for Video ID: " + vid.videoID);
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            } else if (Boolean.FALSE.equals(isSupported(itag))) {
                this.logger.info("FFmpeg support for Itag'" + itag + "' is missing");
                return null;
            }
            final YoutubeStreamData ret = new YoutubeStreamData(src, vid, url, itag, null);
            ret.setThrottle(throttle);
            if (height > 0) {
                ret.setHeight(height.intValue());
            }
            if (width > 0) {
                ret.setWidth(width.intValue());
            }
            if (fps > 0) {
                ret.setFps(fps.toString());
            }
            if (contentLength > 0) {
                ret.setContentLength(contentLength.longValue());
            }
            if (bitrate > 0) {
                ret.setBitrate(bitrate.intValue());
            }
            if (averageBitrate > 0) {
                ret.setAverageBitrate(averageBitrate.intValue());
            }
            if (approxDurationMs > 0) {
                ret.setApproxDurationMs(approxDurationMs.longValue());
            }
            // stereoLayout:STEREO_LAYOUT_LEFT_RIGHT
            // projectionType:RECTANGULAR,MESH,EQUIRECTANGULAR
            if (StringUtils.equalsIgnoreCase(projectionType, "RECTANGULAR")) {
                ret.setProjectionType(0);
            } else if (StringUtils.equalsIgnoreCase(projectionType, "MESH")) {
                ret.setProjectionType(2);
            } else if (StringUtils.equalsIgnoreCase(projectionType, "EQUIRECTANGULAR")) {
                ret.setProjectionType(2);
            }
            return ret;
        }
        return null;
    }

    private int collectMapsFromPlayerResponse(Map<String, Object> map, String src) {
        int ret = 0;
        if (map != null) {
            final List<Map<String, Object>> captionTracks = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(map, "captions/playerCaptionsTracklistRenderer/captionTracks");
            if (captionTracks != null) {
                subtitleUrls.add(JSonStorage.toString(captionTracks));
            }
            final Map<String, Object> streamingData = (Map<String, Object>) map.get("streamingData");
            if (adaptiveFmtsEnabled && streamingData != null && streamingData.containsKey("adaptiveFormats")) {
                final List<Map<String, Object>> adaptiveFormats = (List<Map<String, Object>>) streamingData.get("adaptiveFormats");
                if (adaptiveFormats != null && adaptiveFormats.size() > 0) {
                    final String dataSrc = "new_adaptive_fmts_map." + src;
                    for (final Map<String, Object> format : adaptiveFormats) {
                        final YoutubeStreamData data = convert(format, dataSrc);
                        if (data != null) {
                            if (fmtMaps.add(new StreamMap(data, dataSrc))) {
                                ret++;
                            }
                        }
                    }
                }
            }
            if (fmtMapEnabled && streamingData != null && streamingData.containsKey("formats")) {
                final List<Map<String, Object>> formats = (List<Map<String, Object>>) streamingData.get("formats");
                if (formats != null && formats.size() > 0) {
                    final String dataSrc = "new_fmt_stream_map." + src;
                    for (final Map<String, Object> format : formats) {
                        final YoutubeStreamData data = convert(format, dataSrc);
                        if (data != null) {
                            if (fmtMaps.add(new StreamMap(data, dataSrc))) {
                                ret++;
                            }
                        }
                    }
                }
            }
            if (dashMpdEnabled && streamingData != null && streamingData.containsKey("dashManifestUrl")) {
                final String url = (String) streamingData.get("dashManifestUrl");
                if (StringUtils.isNotEmpty(url)) {
                    try {
                        final String dataSrc = "new_dashManifestUrl." + src;
                        final List<YoutubeStreamData> datas = parseDashManifest(dataSrc, br, br.getURL(url).toString());
                        for (YoutubeStreamData data : datas) {
                            if (fmtMaps.add(new StreamMap(data, dataSrc))) {
                                ret++;
                            }
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            }
        }
        return ret;
    }

    private List<YoutubeStreamData> parseDashManifest(final String src, Browser br, final String dashManifestURL) throws Exception {
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        getPage(brc, dashManifestURL);
        if (brc.getHttpConnection().getResponseCode() != 200 || !brc.containsHTML("<?xml")) {
            throw new Exception("no xml response");
        }
        final List<YoutubeStreamData> ret = new ArrayList<YoutubeStreamData>();
        final DocumentBuilder docBuilder = createXMLParser();
        final Document doc = docBuilder.parse(new InputSource(new StringReader(brc.toString())));
        final NodeList representations = doc.getElementsByTagName("Representation");
        for (int r = 0; r < representations.getLength(); r++) {
            final Element representation = (Element) representations.item(r);
            final Long itagID = JavaScriptEngineFactory.toLong(representation.getAttribute("id"), -1);
            final Long width = JavaScriptEngineFactory.toLong(representation.getAttribute("width"), -1);
            final Long height = JavaScriptEngineFactory.toLong(representation.getAttribute("height"), -1);
            final Long bitrate = JavaScriptEngineFactory.toLong(representation.getAttribute("bandwidth"), -1);
            final Long fps = JavaScriptEngineFactory.toLong(representation.getAttribute("frameRate"), -1);
            final Element baseUrlElement = (Element) representation.getElementsByTagName("BaseURL").item(0);
            final String baseURL = baseUrlElement.getTextContent();
            final YoutubeITAG itag = YoutubeITAG.get(itagID.intValue(), width.intValue(), height.intValue(), fps.intValue(), null, null, this.vid != null ? vid.datePublished : null);
            if (itag == null) {
                logger.info("UNSUPPORTED/UNKNOWN?");
                try {
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !Application.isJared(null)) {
                        Dialog.getInstance().showMessageDialog("Unknown ITag found: " + itagID + "\r\nAsk Coalado to Update the ItagHelper for Video ID: " + vid.videoID);
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
                continue;
            } else if (Boolean.FALSE.equals(isSupported(itag))) {
                this.logger.info("FFmpeg support for Itag'" + itag + "' is missing");
                continue;
            }
            final List<String> segments = new ArrayList<String>();
            final NodeList segmentList = representation.getElementsByTagName("SegmentList").item(0).getChildNodes();
            long approxDurationMs = 0;
            long estimatedContentLength = 0;
            for (int i = 0; i < segmentList.getLength(); i++) {
                final Element segment = (Element) segmentList.item(i);
                String url = null;
                if ("Initialization".equals(segment.getNodeName())) {
                    url = segment.getAttribute("sourceURL");
                    if (StringUtils.isNotEmpty(url)) {
                        segments.add(url);
                    }
                } else if ("SegmentURL".equals(segment.getNodeName())) {
                    url = segment.getAttribute("media");
                    if (StringUtils.isNotEmpty(url)) {
                        segments.add(url);
                    }
                }
                if (url != null) {
                    final long sqDuration = parseMPDDuration(url);
                    if (sqDuration > 0) {
                        approxDurationMs += sqDuration;
                    }
                    final long sqRange = parseMPDRange(url);
                    if (sqRange > 0) {
                        estimatedContentLength += sqRange;
                    }
                }
            }
            if (segments.size() == 0) {
                logger.info("UNSUPPORTED/UNKNOWN?");
                continue;
            }
            final YoutubeStreamData data = new YoutubeStreamData(src, vid, baseURL, itag, null);
            data.setSegments(segments.toArray(new String[0]));
            if (height > 0) {
                data.setHeight(height.intValue());
            }
            if (width > 0) {
                data.setWidth(width.intValue());
            }
            if (fps > 0) {
                data.setFps(fps.toString());
            }
            if (bitrate > 0) {
                data.setBitrate(bitrate.intValue());
            }
            if (estimatedContentLength > 0) {
                data.setEstimatedContentLength(estimatedContentLength);
            }
            if (approxDurationMs > 0) {
                data.setApproxDurationMs(approxDurationMs);
            }
            ret.add(data);
        }
        return ret;
    }

    private long parseMPDRange(final String url) {
        final String[] range = new Regex(url, "range/(\\d+)-(\\d+)").getRow(0);
        if (range != null) {
            return Long.parseLong(range[1]) - Long.parseLong(range[0]);
        } else {
            return -1;
        }
    }

    private long parseMPDDuration(final String url) {
        final String[] duration = new Regex(url, "dur/(\\d+)(\\.(\\d+))?").getRow(0);
        if (duration != null) {
            final String secs = duration[0];
            final String msns = duration[2];
            if (duration.length == 1 || msns == null) {
                return Long.parseLong(secs) * 1000;
            } else {
                long ret = Long.parseLong(secs) * 1000;
                if (msns.length() == 1) {
                    ret += Long.parseLong(msns) * 100;
                } else if (msns.length() == 2) {
                    ret += Long.parseLong(msns) * 10;
                } else if (msns.length() == 3) {
                    ret += Long.parseLong(msns);
                } else {
                    ret += Long.parseLong(msns.substring(0, 3));
                }
                return ret;
            }
        }
        return -1;
    }

    private void collectMpdMap(String htmlCode, String regex, String src) {
        String map = new Regex(htmlCode, regex).getMatch(0);
        if (map != null) {
            map = JSonStorage.restoreFromString(map, TypeRef.STRING);
            if (StringUtils.isNotEmpty(map)) {
                final String url = map;
                if (url != null) {
                    mpdUrls.add(new StreamMap(url, src));
                }
            }
        }
    }

    private void handleRentalVideos() throws Exception {
        String rentalText = br.getRegex("\"ypc_video_rental_bar_text\"\\s*\\:\\s*\"([^\"]+)").getMatch(0);
        if (StringUtils.isNotEmpty(rentalText)) {
            logger.warning("Download not possible: " + rentalText);
            throw new Exception(PAID_VIDEO + rentalText);
        }
        if (br.containsHTML("<meta itemprop=\"paid\" content=\"True\">")) {
            logger.warning("Download not possible: You have to pay to watch this video");
            throw new Exception(PAID_VIDEO + " Download not possible");
        }
        if (br.containsHTML("watch-checkout-offers") && !br.containsHTML("The Polymer Project Authors. All rights reserved")) {
            logger.warning("Download not possible: You have to pay to watch this video");
            throw new Exception(PAID_VIDEO + "Download not possible");
        }
    }

    private void doUserAPIScan() throws Exception {
        String checkName = cfg.getPackagePattern() + cfg.getVideoFilenamePattern() + cfg.getAudioFilenamePattern() + cfg.getSubtitleFilenamePattern() + cfg.getImageFilenamePattern() + cfg.getDescriptionFilenamePattern();
        boolean extended = false;
        // only load extra page, if we need the properties
        for (YoutubeReplacer r : REPLACER) {
            if (r.getDataSource() == DataSource.API_USERS && r.matches(checkName)) {
                extended = true;
                break;
            }
        }
        if (!extended) {
            return;
        }
        if (StringUtils.isEmpty(vid.user)) {
            return;
        }
        Browser clone = br.cloneBrowser();
        // if (cfg.isPreferHttpsEnabled()) {
        getPage(clone, "https://gdata.youtube.com/feeds/api/users/" + vid.user + "?v=2");
        // } else {
        // clone.getPage("http://gdata.youtube.com/feeds/api/users/" + vid.user + "?v=2");
        // }
        String googleID = clone.getRegex("<yt\\:googlePlusUserId>(.*?)</yt\\:googlePlusUserId>").getMatch(0);
        if (StringUtils.isNotEmpty(googleID)) {
            vid.userGooglePlusID = googleID;
        }
    }

    /**
     * this method calls an API which has been deprecated by youtube. TODO: Find new API!
     *
     * @deprecated
     * @param vid
     * @throws Exception
     */
    private void doFeedScan() throws Exception {
        if (true) {
            return;
        }
        String checkName = cfg.getFilenamePattern() + cfg.getPackagePattern() + cfg.getVideoFilenamePattern() + cfg.getAudioFilenamePattern() + cfg.getSubtitleFilenamePattern() + cfg.getImageFilenamePattern();
        boolean extended = false;
        // only load extra page, if we need the properties
        for (YoutubeReplacer r : REPLACER) {
            if (r.getDataSource() == DataSource.API_VIDEOS && r.matches(checkName)) {
                extended = true;
                break;
            }
        }
        if (!extended) {
            return;
        }
        Browser clone = br.cloneBrowser();
        // if (cfg.isPreferHttpsEnabled()) {
        getPage(clone, "https://gdata.youtube.com/feeds/api/videos/" + vid.videoID + "?v=2");
        // } else {
        // clone.getPage("http://gdata.youtube.com/feeds/api/videos/" + vid.videoID + "?v=2");
        // }
        if (vid.datePublished == -1) {
            try {
                // dd.MM.yyyy_HH-mm-ss
                // 2014-01-06T00:01:01.000Z
                final String date = clone.getRegex("<published>\\s*(.*?)\\s*</published>").getMatch(0);
                if (StringUtils.isNotEmpty(date)) {
                    DatatypeFactory f = DatatypeFactory.newInstance();
                    XMLGregorianCalendar xgc = f.newXMLGregorianCalendar(date);
                    vid.datePublished = xgc.toGregorianCalendar().getTime().getTime();
                }
            } catch (DatatypeConfigurationException e) {
                logger.log(e);
            }
        }
        if (vid.dateUploaded == -1) {
            try {
                // dd.MM.yyyy_HH-mm-ss
                // 2014-01-06T00:01:01.000Z
                final String date = clone.getRegex("<updated>\\s*(.*?)\\s*</updated>").getMatch(0);
                if (StringUtils.isNotEmpty(date)) {
                    DatatypeFactory f = DatatypeFactory.newInstance();
                    XMLGregorianCalendar xgc = f.newXMLGregorianCalendar(date);
                    vid.dateUploaded = xgc.toGregorianCalendar().getTime().getTime();
                }
            } catch (DatatypeConfigurationException e) {
                logger.log(e);
            }
        }
        if (StringUtils.isEmpty(vid.category)) {
            vid.category = clone.getRegex("<media:category.*?>\\s*(.*?)\\s*</media:category>").getMatch(0);
        }
        if (vid.duration == -1) {
            // duration
            String duration = clone.getRegex("duration\\s*=\\s*\"(\\d+)\"").getMatch(0);
            if (StringUtils.isEmpty(duration)) {
                duration = clone.getRegex("<yt\\:duration seconds\\s*=\\s*\"(\\d+)\" />").getMatch(0);
            }
            if (StringUtils.isNotEmpty(duration)) {
                vid.duration = Integer.parseInt(duration);
            }
        }
    }

    private boolean getThumbnailSize(Browser br, YoutubeStreamData match) {
        final Browser check = br.cloneBrowser();
        check.setFollowRedirects(true);
        try {
            final URLConnectionAdapter con = check.openHeadConnection(match.getUrl());
            try {
                if (con.isOK() && (con.isContentDisposition() || StringUtils.contains(con.getContentType(), "image"))) {
                    if (con.getCompleteContentLength() > 0) {
                        match.setContentLength(con.getCompleteContentLength());
                    }
                    return true;
                }
            } finally {
                con.disconnect();
            }
        } catch (final Exception e) {
            logger.log(e);
        }
        return false;
    }

    private List<YoutubeStreamData> loadThumbnails() {
        final StreamCollection ret = new StreamCollection();
        final String best = br.getRegex("<meta property=\"og\\:image\" content=\".*?/(\\w+\\.jpg)\">").getMatch(0);
        final LinkedHashMap<String, YoutubeITAG> thumbnails = new LinkedHashMap<String, YoutubeITAG>();
        thumbnails.put("maxresdefault.jpg", YoutubeITAG.IMAGE_MAX);
        thumbnails.put("hqdefault.jpg", YoutubeITAG.IMAGE_HQ);
        thumbnails.put("mqdefault.jpg", YoutubeITAG.IMAGE_MQ);
        thumbnails.put("default.jpg", YoutubeITAG.IMAGE_LQ);
        for (Entry<String, YoutubeITAG> thumbnail : thumbnails.entrySet()) {
            final YoutubeStreamData match = (new YoutubeStreamData(null, vid, "https://i.ytimg.com/vi/" + vid.videoID + "/" + thumbnail.getKey(), thumbnail.getValue(), null));
            if (getThumbnailSize(br.cloneBrowser(), match)) {
                ret.add(match);
            }
            if (ret.size() > 0 && StringUtils.equalsIgnoreCase(thumbnail.getKey(), best)) {
                return ret;
            }
        }
        return ret;
    }

    public void login(final Account account, final boolean refresh) throws Exception {
        synchronized (account) {
            br.setDebug(true);
            br.setCookiesExclusive(true);
            // delete all cookies
            br.clearCookies(null);
            final GoogleHelper googlehelper = new GoogleHelper(br);
            googlehelper.setLogger(br.getLogger());
            // helper.setLogger(this.getLogger());
            final Thread thread = Thread.currentThread();
            final boolean forceUpdateAndBypassCache = thread instanceof AccountCheckerThread && ((AccountCheckerThread) thread).getJob().isForce();
            if (forceUpdateAndBypassCache) {
                googlehelper.login(account, true);
            } else {
                googlehelper.login(account, refresh);
            }
            /* No Exception -> Success */
            this.account = account;
        }
    }

    private void getPageFollowRedirectsDuringLogin(Browser br, String string) throws IOException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        int max = 20;
        try {
            while (max-- > 0) {
                br.getPage(string);
                if (br.getRedirectLocation() != null) {
                    if (br.getCookie("youtube.com", "LOGIN_INFO") != null) {
                        break;
                    }
                    string = br.getRedirectLocation();
                    continue;
                }
                String redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"0\\; url=\\&\\#39\\;(.+)\\&\\#39\\;").getMatch(0);
                if (redirect != null) {
                    string = Encoding.htmlDecode(redirect);
                } else {
                    break;
                }
            }
        } finally {
            br.setFollowRedirects(before);
        }
    }

    /** Logs into valid youtube account. */
    public Account login(LogInterface logger, final boolean refresh) {
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
        { // debug
            List<Account> debug = AccountController.getInstance().getAllAccounts("google.com");
            if (debug != null) {
                accounts.addAll(debug);
            }
            debug = AccountController.getInstance().getAllAccounts("youtube.jd");
            if (debug != null) {
                accounts.addAll(debug);
            }
        }
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    try {
                        this.login(n, refresh);
                        if (n.isValid()) {
                            return n;
                        }
                    } catch (final Exception e) {
                        logger.log(e);
                        n.setValid(false);
                        // should we not try other accounts??
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static final String YT_LENGTH_SECONDS          = "YT_LENGTH_SECONDS";
    /**
     * @deprecated use {@link #YT_VARIANT_INFO}
     */
    public static final String YT_STREAMURL_DATA          = "YT_STREAMURL_DATA";
    @Deprecated
    public static final String YT_SUBTITLE_CODE           = "YT_SUBTITLE_CODE";          // Update YoutubeSubtitleName
    @Deprecated
    public static final String YT_SUBTITLE_CODE_LIST      = "YT_SUBTITLE_CODE_LIST";
    public static final String YT_BEST_VIDEO              = "YT_BEST_VIDEO";
    public static final String YT_BEST_VIDEO_HEIGHT       = "YT_BEST_VIDEO_HEIGHT";
    public static final String YT_DESCRIPTION             = "YT_DESCRIPTION";
    public static final String YT_DESCRIPTION_ALTERNATIVE = "YT_DESCRIPTION_ALTERNATIVE";
    // public static final String YT_VARIANT_INFO = "YT_VARIANT_INFO";
    public static final String YT_STREAM_DATA_VIDEO       = "YT_STREAM_DATA_VIDEO";
    public static final String YT_STREAM_DATA_AUDIO       = "YT_STREAM_DATA_AUDIO";
    public static final String YT_STREAM_DATA_DATA        = "YT_STREAM_DATA_DATA";
    public static final String YT_3D                      = "YT_3D";
    public static final String YT_COLLECTION              = "YT_COLLECTION";
    public static final String YT_PLAYLIST_CREATOR        = "YT_PLAYLIST_CREATOR";
    public static final String YT_PLAYLIST_TITLE          = "YT_PLAYLIST_TITLE";
    public static final String YT_PLAYLIST_ID             = "YT_PLAYLIST_ID";
    public static final String YT_PLAYLIST_SIZE           = "YT_PLAYLIST_SIZE";
    /* Position of video if it is part of a playlist. */
    public static final String YT_PLAYLIST_POSITION       = "YT_PLAYLIST_INT";
    public static final String YT_PLAYLIST_DESCRIPTION    = "YT_PLAYLIST_DESCRIPTION";
    public static final String YT_USER_ID                 = "YT_USER_ID";
    public static final String YT_USER_NAME               = "YT_USER_NAME";
    public static final String YT_USER_NAME_ALTERNATIVE   = "YT_USER_NAME_ALTERNATIVE";

    /** Returns true if given pattern contains a replacement entry which can only be available in context of a single crawled video. */
    public static boolean namePatternContainsSingleVideoSpecificEntries(final String pattern) {
        if (pattern == null) {
            return false;
        }
        for (final YoutubeReplacer rpl : YoutubeHelper.REPLACER) {
            final DataOrigin[] dataOrigins = rpl.getDataOrigins();
            if (rpl.matches(pattern) && dataOrigins != null && dataOrigins.length > 0 && dataOrigins[0] == DataOrigin.YT_SINGLE_VIDEO) {
                return true;
            }
        }
        return false;
    }

    public String createFilename(final DownloadLink link) {
        AbstractVariant variant = AbstractVariant.get(link);
        String formattedFilename = variant.getFileNamePattern();
        // validate the pattern
        if (!formattedFilename.toLowerCase(Locale.ENGLISH).contains("*ext*")) {
            formattedFilename = null;
        }
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = "*videoname* (*quality*) *ext*";
        }
        formattedFilename = replaceVariables(link, formattedFilename);
        final String playlistID = link.getStringProperty(YoutubeHelper.YT_PLAYLIST_ID);
        final int playlistPosition = link.getIntegerProperty(YoutubeHelper.YT_PLAYLIST_POSITION, -1);
        if (cfg.isPlaylistItemsIncludePlaylistPositionAtBeginningOfFilenames() && playlistID != null && playlistPosition != -1) {
            formattedFilename = playlistPosition + "." + formattedFilename;
        }
        return formattedFilename;
    }

    public String replaceVariables(DownloadLink link, String formattedFilename) {
        LogInterface logger = LogController.getInstance().getPreviousThreadLogSource();
        if (logger == null) {
            logger = Log.DF;
        }
        AbstractVariant variant = AbstractVariant.get(link);
        try {
            formattedFilename = variant.modifyFileName(formattedFilename, link);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        for (YoutubeReplacer r : REPLACER) {
            formattedFilename = r.replace(formattedFilename, this, link);
        }
        try {
            formattedFilename = variant.modifyFileName(formattedFilename, link);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return formattedFilename;
    }

    private static Set<AbstractFFmpegBinary.FLAG> FFMPEG_SUPPORTED_FLAGS = null;
    private final static Object                   FFMPEG_LOCK            = new Object();

    private Boolean isSupported(final YoutubeITAG itag) {
        if (itag != null) {
            synchronized (FFMPEG_LOCK) {
                if (FFMPEG_SUPPORTED_FLAGS == null) {
                    final FFmpeg ffmpeg = new FFmpeg(null) {
                        @Override
                        public LogInterface getLogger() {
                            return logger;
                        }
                    };
                    if (ffmpeg.isAvailable() && ffmpeg.isCompatible()) {
                        FFMPEG_SUPPORTED_FLAGS = ffmpeg.getSupportedFlags();
                    }
                }
                if (FFMPEG_SUPPORTED_FLAGS != null) {
                    if (itag.getVideoCodec() != null) {
                        switch (itag.getVideoCodec()) {
                        case AV1:
                            return FFMPEG_SUPPORTED_FLAGS.contains(AbstractFFmpegBinary.FLAG.AV1);
                        }
                    }
                    if (itag.getAudioCodec() != null) {
                        switch (itag.getAudioCodec()) {
                        case OPUS:
                        case OPUS_SPATIAL:
                            return FFMPEG_SUPPORTED_FLAGS.contains(AbstractFFmpegBinary.FLAG.OPUS);
                        case VORBIS:
                        case VORBIS_SPATIAL:
                            return FFMPEG_SUPPORTED_FLAGS.contains(AbstractFFmpegBinary.FLAG.VORBIS);
                        }
                    }
                }
            }
        }
        return null;
    }

    protected YoutubeStreamData parseLine(final UrlQuery query, StreamMap src) throws MalformedURLException, IOException, PluginException {
        if (StringUtils.equalsIgnoreCase(query.get("conn"), "rtmp")) {
            logger.info("Stream is not supported: " + query);
            vid.error = "RTMP(E) Stream not supported";
            return null;
        }
        if (StringUtils.equals(query.get("stream_type"), "3")) {
            logger.info("UNSUPPORTED OTF:" + query);
            return null;
        }
        String url = query.getDecoded("url");
        if (url == null) {
            String fallback_host = query.getDecoded("fallback_host");
            if (fallback_host != null) {
                url = new Regex(fallback_host, "url=(.+)").getMatch(0);
            }
        }
        if (!url.contains("ei=")) {
            System.out.println("ei");
        }
        // if an ei=... parameter is missing, the url is invalid and will probably return a 403 response code
        if (StringUtils.isEmpty(url)) {
            throw new WTFException("No Url found " + query);
        }
        int throttle = -1;
        if (url.contains("&n=")) {
            throttle = 0;
            final String value = new Regex(url, "&n=(.*?)(&|$)").getMatch(0);
            final String result = descrambleThrottle(value);
            if (result != null && !result.equals(value)) {
                url = url.replaceFirst("(&n=" + value + ")", "&n=" + result);
                throttle = 1;
            }
        }
        if (query.containsKey("signature")) {
            url = url + "&signature=" + query.get("signature");
        } else if (query.containsKey("sig")) {
            url = url + "&signature=" + query.get("sig");
        } else if (query.containsKey("s")) {
            String encrypted_sig = query.get("s");
            encrypted_sig = URLDecoder.decode(encrypted_sig, "UTF-8");
            final String signature = this.descrambleSignature(encrypted_sig);
            if (query.containsKey("sp")) {
                url = url + "&" + query.get("sp") + "=" + Encoding.urlEncode(signature);
            } else {
                url = url + "&signature=" + Encoding.urlEncode(signature);
            }
        }
        int bitrate = -1;
        String bitrateString = query.get("bitrate");
        if (StringUtils.isNotEmpty(bitrateString)) {
            bitrate = Integer.parseInt(bitrateString);
        }
        String size = query.get("size");
        int width = -1;
        int height = -1;
        if (StringUtils.isNotEmpty(size)) {
            String[] splitted = size.split("\\s*x\\s*");
            if (splitted != null && splitted.length == 2) {
                width = Integer.parseInt(splitted[0]);
                height = Integer.parseInt(splitted[1]);
            }
        }
        String fps = query.get("fps");
        String type = query.get("type");
        if (StringUtils.isNotEmpty(type)) {
            type = Encoding.urlDecode(type, false);
        }
        final String itagID = query.get("itag");
        try {
            final YoutubeITAG itag = YoutubeITAG.get(Integer.parseInt(itagID), width, height, StringUtils.isEmpty(fps) ? -1 : Integer.parseInt(fps), type, query, vid.datePublished);
            if (itag == null) {
                this.logger.info("Unknown ITAG: " + itagID);
                this.logger.info(url + "");
                this.logger.info(query + "");
                try {
                    if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && !Application.isJared(null)) {
                        Dialog.getInstance().showMessageDialog("Unknown ITag found: " + itagID + "\r\nAsk Coalado to Update the ItagHelper for Video ID: " + vid.videoID);
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
                return null;
            } else if (Boolean.FALSE.equals(isSupported(itag))) {
                this.logger.info("FFmpeg support for Itag'" + itag + "' is missing");
                return null;
            }
            final String quality = Encoding.urlDecode(query.get("quality"), false);
            logger.info(Encoding.urlDecode(JSonStorage.toString(query.list()), false));
            if (url != null) {
                final YoutubeStreamData vsd = new YoutubeStreamData(src.src, vid, url, itag, query);
                vsd.setThrottle(throttle);
                vsd.setHeight(height);
                vsd.setWidth(width);
                vsd.setFps(fps);
                return vsd;
            }
            return null;
        } catch (NumberFormatException e) {
            throw e;
        }
    }

    private void itagWarning(YoutubeITAG itag, String string, Object size) {
        this.logger.warning("Youtube WARNING! Bad Itag choosen: " + itag + " does not support " + string + " of " + size);
    }

    private String replaceHttps(final String s) {
        // final boolean prefers = this.cfg.isPreferHttpsEnabled();
        //
        // if (prefers) {
        return s.replaceFirst("http://", "https://");
        // } else {
        // return s.replaceFirst("https://", "http://");
        // }
    }

    public void setupProxy() {
        if (br == null) {
            return;
        }
        if (this.cfg.isProxyEnabled()) {
            final HTTPProxyStorable proxy = this.cfg.getProxy();
            // int PROXY_PORT = cfg.getProxyPort();
            // if (StringUtils.isEmpty(PROXY_ADDRESS) || PROXY_PORT < 0) return;
            // PROXY_ADDRESS = new Regex(PROXY_ADDRESS, "^[0-9a-zA-Z]+://").matches() ? PROXY_ADDRESS : "http://" + PROXY_ADDRESS;
            // org.appwork.utils.net.httpconnection.HTTPProxy proxy =
            // org.appwork.utils.net.httpconnection.HTTPProxy.parseHTTPProxy(PROXY_ADDRESS + ":" + PROXY_PORT);
            if (proxy != null) {
                HTTPProxy prxy = HTTPProxy.getHTTPProxy(proxy);
                if (prxy != null) {
                    br.setProxy(prxy);
                } else {
                }
                return;
            }
        }
        br.setProxy(br.getThreadProxy());
    }

    private ArrayList<YoutubeSubtitleStorable> loadSubtitles() throws Exception {
        Map<String, List<YoutubeSubtitleStorable>> urls = new LinkedHashMap<String, List<YoutubeSubtitleStorable>>();
        YoutubeSubtitleStorable defaultLanguage = null;
        for (String ttsUrl : subtitleUrls) {
            if (ttsUrl.startsWith("[")) {
                final List<Object> tts = JSonStorage.restoreFromString(ttsUrl, TypeRef.LIST);
                if (tts != null) {
                    for (final Object object : tts) {
                        if (object instanceof Map) {
                            final Map<String, Object> map = (Map<String, Object>) object;
                            final Object isTranslatable = map.get("isTranslatable");
                            if (!"true".equalsIgnoreCase(String.valueOf(isTranslatable))) {
                                continue;
                            }
                            String url = (String) map.get("baseUrl");
                            if (url == null) {
                                continue;
                            } else {
                                url = br.getURL(url).toString();
                            }
                            final String name;
                            if (map.get("name") instanceof Map) {
                                final Map<String, Object> nameMap = (Map<String, Object>) map.get("name");
                                name = StringUtils.valueOrEmpty((String) nameMap.get("simpleText"));
                            } else {
                                name = "";
                            }
                            final String lang = (String) map.get("languageCode");
                            final String kind = StringUtils.valueOrEmpty((String) map.get("kind"));
                            final String lngID = lang;
                            List<YoutubeSubtitleStorable> list = urls.get(lngID);
                            final YoutubeSubtitleStorable info = new YoutubeSubtitleStorable(null, name, lang, null, kind);
                            info.setFullUrl(url);
                            if (info._getLocale() == null) {
                                // unknown language
                                logger.info("Unknown Subtitle Language: " + JSonStorage.serializeToJson(info));
                                continue;
                            }
                            if (list == null) {
                                list = new ArrayList<YoutubeSubtitleStorable>();
                                urls.put(lngID, list);
                            }
                            if (list.size() > 0) {
                                info.setMulti(list.size());
                            }
                            list.add(info);
                            if (defaultLanguage == null && "true".equalsIgnoreCase(StringUtils.valueOfOrNull(map.get("lang_default")))) {
                                defaultLanguage = info;
                            }
                        }
                    }
                    break;
                }
            } else {
                String xml = getPage(br, replaceHttps(ttsUrl + "&asrs=1&fmts=1&tlangs=1&ts=" + System.currentTimeMillis() + "&type=list")).getHtmlCode();
                String name = null;
                DocumentBuilder docBuilder = createXMLParser();
                InputSource is = new InputSource(new StringReader(xml));
                Document doc = docBuilder.parse(is);
                NodeList tracks = doc.getElementsByTagName("track");
                for (int trackIndex = 0; trackIndex < tracks.getLength(); trackIndex++) {
                    Element track = (Element) tracks.item(trackIndex);
                    String trackID = track.getAttribute("id");
                    final String cantran = track.getAttribute("cantran");
                    if (!"true".equalsIgnoreCase(cantran)) {
                        continue;
                    }
                    String lang = track.getAttribute("lang_code");
                    name = track.hasAttribute("name") ? track.getAttribute("name") : name;
                    String kind = track.getAttribute("kind");
                    String langOrg = track.getAttribute("lang_original");
                    String langTrans = track.getAttribute("lang_translated");
                    if (name == null) {
                        name = "";
                    }
                    if (kind == null) {
                        kind = "";
                    }
                    final String lngID = lang;
                    if (StringUtils.isNotEmpty(langTrans)) {
                        langOrg = langTrans;
                    }
                    if (StringUtils.isEmpty(langOrg)) {
                        langOrg = TranslationFactory.stringToLocale(lang).getDisplayLanguage(Locale.ENGLISH);
                    }
                    List<YoutubeSubtitleStorable> list = urls.get(lngID);
                    final YoutubeSubtitleStorable info = new YoutubeSubtitleStorable(ttsUrl, name, lang, null, kind);
                    if (info._getLocale() == null) {
                        // unknown language
                        logger.info("Unknown Subtitle Language: " + JSonStorage.serializeToJson(info));
                        continue;
                    }
                    if (list == null) {
                        list = new ArrayList<YoutubeSubtitleStorable>();
                        urls.put(lngID, list);
                    }
                    if (list.size() > 0) {
                        info.setMulti(list.size());
                    }
                    list.add(info);
                    if (defaultLanguage == null && "true".equalsIgnoreCase(track.getAttribute("lang_default"))) {
                        defaultLanguage = info;
                    }
                    // System.out.println(lang);
                }
                if (defaultLanguage != null) {
                    NodeList targets = doc.getElementsByTagName("target");
                    for (int targetIndex = 0; targetIndex < targets.getLength(); targetIndex++) {
                        Element target = (Element) targets.item(targetIndex);
                        String targetID = target.getAttribute("id");
                        String lang = target.getAttribute("lang_code");
                        String kind = target.getAttribute("kind");
                        String langOrg = target.getAttribute("lang_original");
                        String langTrans = target.getAttribute("lang_translated");
                        String urlfrag = target.getAttribute("urlfrag");
                        if (name == null) {
                            name = "";
                        }
                        if (kind == null) {
                            kind = defaultLanguage.getKind();
                        }
                        final String lngID = lang;
                        if (StringUtils.isNotEmpty(langTrans)) {
                            langOrg = langTrans;
                        }
                        if (StringUtils.isEmpty(langOrg)) {
                            langOrg = TranslationFactory.stringToLocale(lang).getDisplayLanguage(Locale.ENGLISH);
                        }
                        List<YoutubeSubtitleStorable> list = urls.get(lngID);
                        if (list != null) {
                            continue;
                        }
                        final String cantran = target.getAttribute("cantran");
                        if (!"true".equalsIgnoreCase(cantran)) {
                            continue;
                        }
                        final YoutubeSubtitleStorable info = new YoutubeSubtitleStorable(ttsUrl, name, lang, defaultLanguage.getLanguage(), kind);
                        // br.getPage(new GetRequest(info.createUrl()));
                        if (info._getLocale() == null) {
                            // unknown language
                            logger.info("Unknown Subtitle Language: " + JSonStorage.serializeToJson(info));
                            continue;
                        }
                        if (list == null) {
                            list = new ArrayList<YoutubeSubtitleStorable>();
                            urls.put(lngID, list);
                        }
                        list.add(info);
                        // System.out.println("->" + lang);
                    }
                }
            }
        }
        if (defaultLanguage == null) {
            final List<YoutubeSubtitleStorable> en = urls.get("en");
            if (en != null) {
                defaultLanguage = en.get(0);
            } else {
                for (List<YoutubeSubtitleStorable> subtitles : urls.values()) {
                    for (YoutubeSubtitleStorable subtitle : subtitles) {
                        defaultLanguage = subtitle;
                        break;
                    }
                }
            }
        }
        if (defaultLanguage != null) {
            final Set<Locale> autoTranslatedSubtitlesSet = new HashSet<Locale>();
            final List<String> autoTranslatedSubtitles = CFG_YOUTUBE.CFG.getAutoTranslatedSubtitles();
            if (autoTranslatedSubtitles != null && autoTranslatedSubtitles.size() > 0) {
                for (String autoTranslatedSubtitle : autoTranslatedSubtitles) {
                    try {
                        final Locale locale = TranslationFactory.stringToLocale(autoTranslatedSubtitle);
                        if (!StringUtils.isEmpty(locale.getISO3Language())) {
                            autoTranslatedSubtitlesSet.add(locale);
                        }
                    } catch (Exception e) {
                        getLogger().log(e);
                    }
                }
            }
            final Locale desiredLocale = TranslationFactory.getDesiredLocale();
            for (final Locale autoTranslateSubtitle : autoTranslatedSubtitlesSet) {
                final String lng = autoTranslateSubtitle.getLanguage();
                final String iso3Lng = autoTranslateSubtitle.getISO3Language();
                if (urls.containsKey(lng)) {
                    continue;
                } else if (urls.containsKey(iso3Lng)) {
                    continue;
                } else {
                    final YoutubeSubtitleStorable info = new YoutubeSubtitleStorable(null, autoTranslateSubtitle.getDisplayName(desiredLocale), lng, defaultLanguage.getLanguage(), defaultLanguage.getKind());
                    final String url = defaultLanguage.getFullUrl() + "&tlang=" + lng;
                    info.setFullUrl(url);
                    final ArrayList<YoutubeSubtitleStorable> list = new ArrayList<YoutubeSubtitleStorable>();
                    list.add(info);
                    urls.put(lng, list);
                    urls.put(iso3Lng, list);
                }
            }
        }
        final ArrayList<YoutubeSubtitleStorable> ret = new ArrayList<YoutubeSubtitleStorable>();
        for (final List<YoutubeSubtitleStorable> list : urls.values()) {
            for (final YoutubeSubtitleStorable entry : list) {
                if (!ret.contains(entry)) {
                    ret.add(entry);
                }
            }
        }
        return ret;
    }

    // public List<YoutubeBasicVariant> getVariantByIds(String... extra) {
    // ArrayList<YoutubeBasicVariant> ret = new ArrayList<YoutubeBasicVariant>();
    // if (extra != null) {
    // for (String s : extra) {
    // YoutubeBasicVariant v = getVariantById(s);
    // if (v != null) {
    // ret.add(v);
    // }
    // }
    // }
    // return ret;
    // }
    public YoutubeConfig getConfig() {
        return cfg;
    }

    public static List<VariantIDStorable> readExtraList() {
        YoutubeConfig cf = PluginJsonConfig.get(YoutubeConfig.class);
        List<VariantIDStorable> list = new ArrayList<VariantIDStorable>();
        // List<VariantIDStorable> configList = cf.getExtra();
        // if (configList != null) {
        // for (VariantIDStorable obj : configList) {
        // if (obj != null) {
        // list.add(obj);
        // }
        // }
        // }
        // String[] strList = cf.getExtraVariants();
        // if (strList != null) {
        // for (String b : strList) {
        //
        // list.add(new VariantIDStorable(b));
        // }
        // cf.setExtra(list);
        // cf.setExtraVariants(null);
        // }
        return list;
    }

    public static List<VariantIDStorable> readBlacklist() {
        YoutubeConfig cf = PluginJsonConfig.get(YoutubeConfig.class);
        List<VariantIDStorable> list = new ArrayList<VariantIDStorable>();
        // List<VariantIDStorable> configList = cf.getDisabledVariants();
        // if (configList != null) {
        // for (VariantIDStorable obj : configList) {
        // if (obj != null) {
        // list.add(obj);
        // }
        // }
        // }
        // String[] strList = cf.getBlacklistedVariants();
        // if (strList != null) {
        // for (String b : strList) {
        //
        // list.add(new VariantIDStorable(b));
        // }
        // cf.setDisabledVariants(list);
        // cf.setBlacklistedVariants(null);
        // }
        return list;
    }

    public static String createLinkID(String videoID, AbstractVariant variant) {
        final String ret = "youtubev2://" + videoID + "/" + Hash.getMD5(Encoding.urlEncode(variant._getUniqueId()));
        return ret;
    }

    public static void writeVariantToDownloadLink(DownloadLink downloadLink, AbstractVariant v) {
        downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, v);
        downloadLink.setProperty(YoutubeHelper.YT_VARIANT, v.getStorableString());
    }

    public void extendedDataLoading(VariantInfo v, List<VariantInfo> variants) {
        extendedDataLoadingDemuxAudioBitrate(v, variants);
    }

    protected void checkFFProbe(FFprobe ffmpeg, String reason) throws SkipReasonException, InterruptedException {
        if (!ffmpeg.isAvailable()) {
            final UpdateHandler handler = UpdateController.getInstance().getHandler();
            if (handler == null) {
                logger.warning("Please set FFMPEG: BinaryPath in advanced options");
                throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
            }
            final FFMpegInstallProgress progress = new FFMpegInstallProgress();
            progress.setProgressSource(this);
            FFmpegProvider.getInstance().install(progress, reason);
            ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
            if (!ffmpeg.isAvailable()) {
                final List<String> requestedInstalls = handler.getRequestedInstalls();
                final String extensionID = org.jdownloader.controlling.ffmpeg.FFMpegInstallThread.getFFmpegExtensionName();
                if (requestedInstalls != null && extensionID != null && requestedInstalls.contains(extensionID)) {
                    throw new SkipReasonException(SkipReason.UPDATE_RESTART_REQUIRED);
                } else {
                    throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                }
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                // _GUI.T.YoutubeDash_handleFree_ffmpegmissing());
            }
        }
    }

    public void extendedDataLoading(List<VariantInfo> vs) {
        for (VariantInfo v : vs) {
            extendedDataLoading(v, vs);
        }
    }

    protected void extendedDataLoadingDemuxAudioBitrate(VariantInfo v, List<VariantInfo> variants) {
        if (CFG_YOUTUBE.CFG.isDoExtendedAudioBitrateLookupEnabled()) {
            final YoutubeITAG itagVideo = v.getVariant().getiTagVideo();
            if (itagVideo != null) {
                switch (itagVideo.getITAG()) {
                case 22:
                case 18:
                case 82:
                case 84:
                    int bitrate = v.getVideoStreams().getAudioBitrate();
                    if (bitrate <= 0) {
                        logger.info("Load Stream Probe for " + itagVideo + " - " + itagVideo.getITAG());
                        main: for (YoutubeStreamData vStream : v.getVideoStreams()) {
                            try {
                                if (vStream.getSegments() != null && vStream.getSegments().length > 0) {
                                    System.out.println("HLS");
                                } else {
                                    final Browser clone = br.cloneBrowser();
                                    final List<HTTPProxy> proxies = br.selectProxies(new URL("https://youtube.com"));
                                    if (proxies != null && proxies.size() > 0) {
                                        clone.setProxySelector(new StaticProxySelector(proxies.get(0)));
                                    }
                                    final FFprobe ffmpeg = new FFprobe(clone) {
                                        @Override
                                        public LogInterface getLogger() {
                                            return YoutubeHelper.this.logger;
                                        }
                                    };
                                    // probe.isAvailable()
                                    checkFFProbe(ffmpeg, "Detect the actual Audio Bitrate");
                                    StreamInfo streamInfo = ffmpeg.getStreamInfo(vStream.getUrl());
                                    if (streamInfo != null) {
                                        for (Stream stream : streamInfo.getStreams()) {
                                            if ("audio".equals(stream.getCodec_type())) {
                                                int aBitrate = (int) (Double.parseDouble(stream.getBit_rate()) / 1000);
                                                if (aBitrate > 0) {
                                                    bitrate = aBitrate;
                                                    v.getVideoStreams().setAudioBitrate(aBitrate);
                                                    break main;
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (bitrate > 0) {
                        for (VariantInfo av : variants) {
                            if (av.getVariant().getiTagVideo() == itagVideo) {
                                if (av.getVariant().getGenericInfo() instanceof GenericAudioInfo) {
                                    ((GenericAudioInfo) av.getVariant().getGenericInfo()).setaBitrate(bitrate);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private Map<String, Object> jsonToJavaMap(final String string, boolean throwException) throws Exception {
        try {
            return JavaScriptEngineFactory.jsonToJavaMap(string);
        } catch (Exception e) {
            if (throwException) {
                throw e;
            } else {
                if (!StringUtils.containsIgnoreCase(e.getMessage(), "window.ytplayer")) {
                    logger.log(e);
                }
                return null;
            }
        }
    }

    // it's important to clean fields because YoutubeHelper might be shared instance
    public void parse() throws Exception {
        {
            String ytInitialData = br.getRegex(">\\s*var\\s*ytInitialData\\s*=\\s*(\\{.*?\\})\\s*;?\\s*</script").getMatch(0);
            if (ytInitialData == null) {
                ytInitialData = br.getRegex("window\\[\"ytInitialData\"\\]\\s*=\\s*(\\{.*?\\})\\s*;\\s*[\r\n]").getMatch(0);
                if (ytInitialData == null) {
                    ytInitialData = br.getRegex("window\\[\"ytInitialData\"\\]\\s*=\\s*(?:JSON.parse)?\\s*\\(\\s*(\"\\{.*?\\}\")\\s*\\)\\s*;\\s*[\r\n]").getMatch(0);
                    if (ytInitialData != null) {
                        ytInitialData = JSonStorage.restoreFromString(ytInitialData, TypeRef.STRING);
                    }
                }
            }
            if (ytInitialData != null) {
                this.ytInitialData = jsonToJavaMap(ytInitialData, true);
            } else {
                this.ytInitialData = null;
            }
        }
        {
            String ytInitialPlayerResponse = br.getRegex(">\\s*var\\s*ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\})\\s*;?\\s*(</script|var\\s*(?:meta|head)\\s*=\\s*document)").getMatch(0);
            if (ytInitialPlayerResponse == null) {
                ytInitialPlayerResponse = br.getRegex("window\\[\"ytInitialPlayerResponse\"\\]\\s*=\\s*\\(\\s*(\\{.*?\\})\\s*\\)\\s*;\\s*[\r\n]").getMatch(0);
                if (ytInitialPlayerResponse == null) {
                    ytInitialPlayerResponse = br.getRegex("window\\[\"ytInitialPlayerResponse\"\\]\\s*=\\s*(\\{.*?\\})\\s*;\\s*[\r\n]").getMatch(0);
                    if (ytInitialPlayerResponse == null) {
                        ytInitialPlayerResponse = br.getRegex("window\\[\"ytInitialPlayerResponse\"\\]\\s*=\\s*(?:JSON.parse)?\\s*\\(\\s*(\"\\{.*?\\}\")\\s*\\)\\s*;\\s*[\r\n]").getMatch(0);
                        if (ytInitialPlayerResponse != null) {
                            ytInitialPlayerResponse = JSonStorage.restoreFromString(ytInitialPlayerResponse, TypeRef.STRING);
                        }
                    }
                }
            }
            if (ytInitialPlayerResponse != null) {
                this.ytInitialPlayerResponse = jsonToJavaMap(ytInitialPlayerResponse, true);
            } else {
                this.ytInitialPlayerResponse = null;
            }
        }
        {
            String ytplayerConfig = br.getRegex("ytplayer\\.(?:web_player_context_)?config\\s*=\\s*\\s*(\\{.*?\\});\\s*ytplayer\\.load").getMatch(0);
            if (ytplayerConfig == null) {
                ytplayerConfig = br.getRegex("ytplayer\\.(?:web_player_context_)?config\\s*=\\s*\\s*(\\{.*?\\});\\s*\\(\\s*function\\s*playerBootstrap").getMatch(0);
                if (ytplayerConfig == null) {
                    ytplayerConfig = br.getRegex("ytplayer\\.(?:web_player_context_)?config\\s*=\\s*\\s*(\\{.*?\\});").getMatch(0);
                }
            }
            if (ytplayerConfig != null) {
                this.ytPlayerConfig = jsonToJavaMap(ytplayerConfig, false);
                if (this.ytPlayerConfig != null) {
                    Object playerResponse = JavaScriptEngineFactory.walkJson(this.ytPlayerConfig, "args/player_response");
                    if (playerResponse == null) {
                        playerResponse = JavaScriptEngineFactory.walkJson(this.ytPlayerConfig, "args/raw_player_response");
                    }
                    if (playerResponse instanceof String) {
                        final Map<String, Object> ytInitialPlayerResponse = jsonToJavaMap(playerResponse.toString(), true);
                        if (this.ytInitialPlayerResponse == null) {
                            this.ytInitialPlayerResponse = ytInitialPlayerResponse;
                        } else {
                            logger.info("Merge ytInitialPlayerResponse");
                            // merge
                            this.ytInitialPlayerResponse.putAll(ytInitialPlayerResponse);
                        }
                    }
                }
            } else {
                this.ytPlayerConfig = null;
            }
        }
        {
            // there are many of these on the page
            final String ytcfgSet[] = br.getRegex("ytcfg\\.set\\((\\{.*?\\})\\);").getColumn(0);
            if (ytcfgSet != null) {
                final Map<String, Object> set = new HashMap<String, Object>();
                for (final String ytcfg : ytcfgSet) {
                    final Map<String, Object> map = jsonToJavaMap(ytcfg, false);
                    if (map != null && map.size() > 0) {
                        set.putAll(map);
                    }
                }
                if (set.size() > 0) {
                    this.ytCfgSet = set;
                } else {
                    this.ytCfgSet = null;
                }
            } else {
                this.ytCfgSet = null;
            }
        }
    }

    public String getChannelPlaylistCrawlerContainerUrlOverride(final String fallback) {
        if (channelPlaylistCrawlerContainerUrlOverride != null) {
            return channelPlaylistCrawlerContainerUrlOverride;
        } else {
            return fallback;
        }
    }

    /**
     * Use this inside channel crawler to set the actually used channel/playlist URL as this can differ from the URL initially added by the
     * user.
     */
    public void setChannelPlaylistCrawlerContainerUrlOverride(String channelPlaylistCrawlerContainerUrlOverride) {
        this.channelPlaylistCrawlerContainerUrlOverride = channelPlaylistCrawlerContainerUrlOverride;
    }
}