package jd.plugins.hoster;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkDatabindingInterface;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.YoutubeClipData;
import jd.plugins.components.YoutubeCustomVariantStorable;
import jd.plugins.components.YoutubeITAG;
import jd.plugins.components.YoutubeStreamData;
import jd.plugins.components.YoutubeSubtitleInfo;
import jd.plugins.components.YoutubeVariant;
import jd.plugins.components.YoutubeVariantInterface;
import jd.plugins.components.YoutubeVariantInterface.VariantGroup;
import jd.plugins.decrypter.YoutubeHelper;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.raf.HashResult;
import jd.utils.locale.JDL;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.swing.action.BasicAction;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.controlling.DownloadLinkView;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloadcore.v15.Downloadable;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.UpdateController;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com" }, urls = { "youtubev2://.+" }, flags = { 2 })
public class YoutubeDashV2 extends PluginForHost {

    private static final String    DASH_AUDIO_FINISHED = "DASH_AUDIO_FINISHED";

    private static final String    DASH_VIDEO_FINISHED = "DASH_VIDEO_FINISHED";

    private static final String    DASH_AUDIO_LOADED   = "DASH_AUDIO_LOADED";

    private static final String    DASH_VIDEO_LOADED   = "DASH_VIDEO_LOADED";

    private final String           DASH_AUDIO_CHUNKS   = "DASH_AUDIO_CHUNKS";

    private final String           DASH_VIDEO_CHUNKS   = "DASH_VIDEO_CHUNKS";

    private YoutubeConfig          cfg;
    private YoutubeHelper          cachedHelper;
    private YoutubeDashConfigPanel configPanel;

    @Override
    public String getAGBLink() {
        //
        return "http://youtube.com/t/terms";
    }

    @Override
    public Class<? extends ConfigInterface> getConfigInterface() {
        return YoutubeConfig.class;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            YoutubeHelper helper = getCachedHelper();
            helper.setupProxy();
            helper.login(account, true, true);

        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.youtube.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public static final class DashDownloadPluginProgress extends DownloadPluginProgress {
        private final long           totalSize;
        private final PluginProgress progress;
        private final long           chunkOffset;

        public DashDownloadPluginProgress(Downloadable downloadable, DownloadInterface downloadInterface, Color color, long totalSize, PluginProgress progress, long chunkOffset) {
            super(downloadable, downloadInterface, color);
            this.totalSize = totalSize;
            this.progress = progress;
            this.chunkOffset = chunkOffset;
        }

        @Override
        public long getCurrent() {
            return chunkOffset + ((DownloadInterface) progress.getProgressSource()).getTotalLinkBytesLoadedLive();
        }

        @Override
        public long getTotal() {
            return totalSize;
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTimeStamp;
        }

        public long getSpeed() {
            return ((DownloadInterface) progress.getProgressSource()).getManagedConnetionHandler().getSpeed();
        }
    }

    public static interface YoutubeConfig extends ConfigInterface {
        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isCreateBestVideoVariantLinkEnabled();

        void setCreateBestVideoVariantLinkEnabled(boolean b);

        @DefaultBooleanValue(false)
        @AboutConfig
        boolean isCustomChunkValueEnabled();

        void setCustomChunkValueEnabled(boolean b);

        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isCreateBestAudioVariantLinkEnabled();

        void setCreateBestAudioVariantLinkEnabled(boolean b);

        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isCreateBestImageVariantLinkEnabled();

        void setCreateBestImageVariantLinkEnabled(boolean b);

        @DefaultBooleanValue(false)
        @AboutConfig
        boolean isFastLinkCheckEnabled();

        void setFastLinkCheckEnabled(boolean b);

        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isCreateBest3DVariantLinkEnabled();

        void setCreateBest3DVariantLinkEnabled(boolean b);

        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isCreateBestSubtitleVariantLinkEnabled();

        void setCreateBestSubtitleVariantLinkEnabled(boolean b);

        @DefaultBooleanValue(false)
        @AboutConfig
        boolean isProxyEnabled();

        void setProxyEnabled(boolean b);

        @AboutConfig
        HTTPProxyStorable getProxy();

        void setProxy(HTTPProxyStorable address);

        public static enum IfUrlisAVideoAndPlaylistAction implements LabelInterface {

            ASK {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_IfUrlisAVideoAndPlaylistAction_ASK();
                }
            },
            VIDEO_ONLY {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_IfUrlisAVideoAndPlaylistAction_VIDEO_ONLY();
                }
            },
            PLAYLIST_ONLY {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_IfUrlisAVideoAndPlaylistAction_PLAYLIST_ONLY();
                }
            },
            NOTHING {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_IfUrlisAVideoAndPlaylistAction_NOTHING();
                }
            },
            ;

        }

        @AboutConfig
        String[] getBlacklistedVariants();

        void setBlacklistedVariants(String[] variants);

        @AboutConfig
        String[] getExtraVariants();

        void setExtraVariants(String[] variants);

        @AboutConfig
        @DefaultEnumValue("ASK")
        IfUrlisAVideoAndPlaylistAction getLinkIsVideoAndPlaylistUrlAction();

        void setLinkIsVideoAndPlaylistUrlAction(IfUrlisAVideoAndPlaylistAction action);

        @DefaultBooleanValue(false)
        @AboutConfig
        boolean isPreferHttpsEnabled();

        void setPreferHttpsEnabled(boolean b);

        public static enum GroupLogic implements LabelInterface {
            BY_MEDIA_TYPE {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_GroupLogic_BY_MEDIA_TYPE();
                }
            },
            BY_FILE_TYPE {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_GroupLogic_BY_FILE_TYPE();
                }
            },
            NO_GROUP {
                @Override
                public String getLabel() {
                    return _JDT._.YoutubeDash_GroupLogic_NO_GROUP();
                }
            }
        }

        @AboutConfig
        @DefaultEnumValue("BY_MEDIA_TYPE")
        GroupLogic getGroupLogic();

        void setGroupLogic(GroupLogic group);

        // @DefaultBooleanValue(true)
        // @AboutConfig
        // boolean isBestGroupVariantEnabled();
        //
        // void setBestGroupVariantEnabled(boolean b);

        @AboutConfig
        @DefaultStringValue("*videoname* (*quality*).*ext*")
        String getFilenamePattern();

        void setFilenamePattern(String name);

        @AboutConfig
        @DefaultIntValue(15)
        int getChunksCount();

        void setChunksCount(int count);

        @AboutConfig
        String[] getPreferedSubtitleLanguages();

        void setPreferedSubtitleLanguages(String[] lngKeys);

        @AboutConfig
        ArrayList<String> getSubtitleWhiteList();

        void setSubtitleWhiteList(ArrayList<String> list);

        @AboutConfig
        ArrayList<String> getExtraSubtitles();

        void setExtraSubtitles(ArrayList<String> list);

        @AboutConfig
        @DefaultBooleanValue(true)
        boolean isSubtitlesEnabled();

        void setSubtitlesEnabled(boolean b);

        @AboutConfig
        ArrayList<YoutubeCustomVariantStorable> getCustomVariants();

        void setCustomVariants(ArrayList<YoutubeCustomVariantStorable> list);

        @AboutConfig
        @DefaultStringValue("*videoname*")
        String getPackagePattern();

        void setPackagePattern(String pattern);

        @AboutConfig
        @DefaultBooleanValue(true)
        void setSubtitleCopyforEachVideoVariant(boolean b);

        boolean isSubtitleCopyforEachVideoVariant();
    }

    @Override
    public ConfigContainer getConfig() {
        throw new WTFException("Not implemented");
    }

    @Override
    public SubConfiguration getPluginConfig() {
        throw new WTFException("Not implemented");
    }

    public YoutubeDashV2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.youtube.com/login?next=/index");

    }

    @Override
    public PluginConfigPanelNG createConfigPanel() {
        YoutubeDashConfigPanel panel = this.configPanel;
        if (panel == null) {
            panel = this.configPanel = new YoutubeDashConfigPanel(getDescription());
        }
        return panel;
    }

    public class UrlCollection {
        private String data;

        public UrlCollection(String videoUrl, String audioUrl, String dataUrl) {
            this.audio = audioUrl;
            this.video = videoUrl;
            this.data = dataUrl;
        }

        String video;
        String audio;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        cfg = PluginJsonConfig.get(YoutubeConfig.class);
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);
        if (!downloadLink.getDownloadURL().startsWith("youtubev2://")) {
            convertOldLink(downloadLink);
        }
        if (Thread.currentThread() instanceof LinkCheckerThread && cfg.isFastLinkCheckEnabled()) return AvailableStatus.UNCHECKED;

        YoutubeHelper helper = getCachedHelper();

        YoutubeVariantInterface variant = getVariant(downloadLink);
        URLConnectionAdapter con;
        long totalSize = -1;
        // youtube uses redirects - maybe for loadbalancing
        br.setFollowRedirects(true);
        switch (variant.getType()) {
        case SUBTITLES:
            for (int i = 0; i < 2; i++) {
                UrlCollection urls = getUrlPair(downloadLink);
                String encoding = br.getHeaders().get("Accept-Encoding");
                try {
                    br.getHeaders().put("Accept-Encoding", "");
                    con = br.openGetConnection(urls.data);
                    con.disconnect();
                } finally {
                    br.getHeaders().put("Accept-Encoding", encoding);
                }
                if (!con.getContentType().startsWith("text/xml") || con.getResponseCode() != 200) {
                    if (i == 0) {
                        resetStreamUrls(downloadLink);
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                totalSize = con.getLongContentLength();
                break;
            }

            break;
        case IMAGE:
            for (int i = 0; i < 2; i++) {
                UrlCollection urls = getUrlPair(downloadLink);
                con = br.openGetConnection(urls.data);
                con.disconnect();
                if (!con.getContentType().startsWith("image/jpeg") || con.getResponseCode() != 200) {
                    if (i == 0) {
                        resetStreamUrls(downloadLink);
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                totalSize = con.getLongContentLength();
                break;
            }
            break;

        default:

            for (int i = 0; i < 2; i++) {
                totalSize = 0;
                UrlCollection urls = getUrlPair(downloadLink);

                if (StringUtils.isNotEmpty(urls.video)) {
                    con = br.openGetConnection(urls.video);
                    con.disconnect();
                    if (con.getResponseCode() == 200) {
                        totalSize += con.getLongContentLength();
                        data.setDashVideoSize(con.getLongContentLength());

                    } else {
                        if (i == 0) {
                            resetStreamUrls(downloadLink);
                            continue;
                        }
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

                    }
                }

                if (StringUtils.isNotEmpty(urls.audio)) {
                    con = br.openGetConnection(urls.audio);
                    con.disconnect();
                    if (con.getResponseCode() == 200) {
                        totalSize += con.getLongContentLength();
                        data.setDashAudioSize(con.getLongContentLength());

                        break;
                    } else {

                        if (i == 0) {
                            resetStreamUrls(downloadLink);
                            continue;
                        }
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } else {
                    break;
                }
            }

        }

        // HTTP/1.1 403 Forbidden
        // helper.login(false, false);

        // // we should cache this information:

        downloadLink.setFinalFileName(helper.createFilename(downloadLink));
        String oldLinkName = downloadLink.getStringProperty("name", null);
        if (StringUtils.isNotEmpty(oldLinkName)) {
            // old link?

            downloadLink.setFinalFileName(oldLinkName);
        }

        downloadLink.setCustomFileOutputFilenameAppend(null);
        YoutubeVariantInterface v = getVariant(downloadLink);
        if (v.hasConverer(downloadLink)) {
            downloadLink.setCustomFileOutputFilenameAppend(".tmp");
        }

        if (totalSize > 0) {
            downloadLink.setVerifiedFileSize(totalSize);
        }

        return AvailableStatus.TRUE;
    }

    private void convertOldLink(DownloadLink link) throws PluginException {
        try {

            link.setProperty(YoutubeHelper.YT_ID, link.getStringProperty("ytID", null));
            if (link.getBooleanProperty("DASH", false)) {
                String video = link.getStringProperty("DASH_VIDEO");
                String audio = link.getStringProperty("DASH_AUDIO");
                YoutubeITAG videoTag = StringUtils.isEmpty(video) ? null : YoutubeITAG.get(Integer.parseInt(video));
                YoutubeITAG audioTag = StringUtils.isEmpty(audio) ? null : YoutubeITAG.get(Integer.parseInt(audio));
                YoutubeVariant variant = null;
                for (YoutubeVariant v : YoutubeVariant.values()) {
                    if (v.getiTagAudio() == audioTag && v.getiTagVideo() == videoTag) {
                        variant = v;
                        break;
                    }
                }
                if (variant == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Old Link. Please readd the Link"); }
                LinkCollector.getInstance().setActiveVariantForLink(link, variant);
            } else {
                String url = link.getDownloadURL();

                YoutubeVariantInterface variant = null;
                String[] matches = new Regex(url, "http://img.youtube.com/vi/(.+)/(.+)\\.jpg").getRow(0);
                if (matches != null) {
                    link.setProperty(YoutubeHelper.YT_ID, matches[0]);
                    if (matches[1].equals("default")) {
                        variant = YoutubeVariant.IMAGE_LQ;
                    } else if (matches[1].equals("mqdefault")) {

                        variant = YoutubeVariant.IMAGE_MQ;

                    } else if (matches[1].equals("hqdefault")) {

                        variant = YoutubeVariant.IMAGE_HQ;

                    } else if (matches[1].equals("maxresdefault")) {

                        variant = YoutubeVariant.IMAGE_MAX;
                    }
                }
                if (variant == null) {
                    LinkedHashMap<String, String> params = Request.parseQuery(url);
                    if (url.contains("/api/timedtext")) {
                        link.setProperty(YoutubeHelper.YT_ID, params.get("v"));
                        variant = new SubtitleVariant(params.get("lang"));
                    } else {
                        String itag = params.get("itag");
                        YoutubeITAG videoTag = YoutubeITAG.get(Integer.parseInt(itag));

                        for (YoutubeVariant v : YoutubeVariant.values()) {
                            if (v.getiTagAudio() == null && v.getiTagVideo() == videoTag) {
                                variant = v;
                                break;
                            }
                        }
                    }
                }
                if (variant == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Old Link. Please readd the Link"); }
                LinkCollector.getInstance().setActiveVariantForLink(link, variant);
            }

        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Old Link. Please readd the Link");
        }

    }

    private UrlCollection getUrlPair(DownloadLink downloadLink) throws Exception {
        String videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
        String audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
        String dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        YoutubeVariantInterface variant = getVariant(downloadLink);
        if (StringUtils.isEmpty(dataUrl) && variant.getiTagData() != null) {
            updateUrls(downloadLink);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        if (StringUtils.isEmpty(videoUrl) && variant.getiTagVideo() != null) {
            updateUrls(downloadLink);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        if (StringUtils.isEmpty(audioUrl) && variant.getiTagAudio() != null) {
            updateUrls(downloadLink);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        return new UrlCollection(videoUrl, audioUrl, dataUrl);
    }

    protected YoutubeVariantInterface getVariant(DownloadLink downloadLink) throws PluginException {

        String var = downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT);
        YoutubeVariantInterface ret = getCachedHelper().getVariantById(var);
        if (ret == null) {
            getLogger().warning("Invalid Variant: " + var);
            throw new PluginException(LinkStatus.ERROR_FATAL, "INVALID VARIANT: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));

        }
        return ret;

    }

    private void updateUrls(DownloadLink downloadLink) throws Exception {
        YoutubeVariantInterface variant = getVariant(downloadLink);
        String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        YoutubeClipData vid = new YoutubeClipData(videoID);
        YoutubeHelper helper = getCachedHelper();
        Map<YoutubeITAG, YoutubeStreamData> info = helper.loadVideo(vid);
        if (info == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, vid.error);
        // write properties in old links and update properties in all others
        downloadLink.setProperty(YoutubeHelper.YT_TITLE, vid.title);
        downloadLink.setProperty(YoutubeHelper.YT_ID, vid.videoID);
        downloadLink.setProperty(YoutubeHelper.YT_AGE_GATE, vid.ageCheck);
        downloadLink.setProperty(YoutubeHelper.YT_CHANNEL, vid.channel);
        downloadLink.setProperty(YoutubeHelper.YT_USER, vid.user);
        downloadLink.setProperty(YoutubeHelper.YT_DATE, vid.date);
        downloadLink.setProperty(YoutubeHelper.YT_LENGTH_SECONDS, vid.length);

        if (variant.getGroup() == YoutubeVariantInterface.VariantGroup.SUBTITLES) {

            String code = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, null);

            for (YoutubeSubtitleInfo si : helper.loadSubtitles(vid)) {

                if (si.getLang().equals(code)) {

                    downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, si._getUrl(vid.videoID));
                    break;
                }

            }
            return;
        }

        if (variant.getiTagData() != null) {
            YoutubeStreamData data = info.get(variant.getiTagData());
            if (data == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagData() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, data.getUrl());
        }
        if (variant.getiTagAudio() != null) {
            YoutubeStreamData audioStream = info.get(variant.getiTagAudio());
            if (audioStream == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagAudio() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_AUDIO, audioStream.getUrl());
        }

        if (variant.getiTagVideo() != null) {
            YoutubeStreamData videoStream = info.get(variant.getiTagVideo());
            if (videoStream == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagVideo() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_VIDEO, videoStream.getUrl());
        }

    }

    public static interface YoutubeProperties extends DownloadLinkDatabindingInterface {
        public static final String DASH_VIDEO_SIZE = "DASH_VIDEO_SIZE";
        public static final String DASH_AUDIO_SIZE = "DASH_AUDIO_SIZE";

        @Key(DASH_VIDEO_SIZE)
        long getDashVideoSize();

        @Key(DASH_VIDEO_SIZE)
        void setDashVideoSize(long longContentLength);

        @Key(DASH_AUDIO_SIZE)
        long getDashAudioSize();

        @Key(DASH_AUDIO_SIZE)
        void setDashAudioSize(long longContentLength);

        @Key(DASH_VIDEO_FINISHED)
        void setDashVideoFinished(boolean b);

        @Key(DASH_AUDIO_FINISHED)
        void setDashAudioFinished(boolean b);

        @Key(DASH_VIDEO_FINISHED)
        boolean isDashVideoFinished();

        @Key(DASH_AUDIO_FINISHED)
        boolean isDashAudioFinished();

        @Key(DASH_VIDEO_LOADED)
        long getDashVideoBytesLoaded();

        @Key(DASH_AUDIO_LOADED)
        long getDashAudioBytesLoaded();

        @Key(DASH_VIDEO_LOADED)
        void setDashVideoBytesLoaded(long bytesLoaded);

        @Key(DASH_AUDIO_LOADED)
        void setDashAudioBytesLoaded(long bytesLoaded);

    }

    private Boolean downloadDashStream(final DownloadLink downloadLink, final YoutubeProperties data, final boolean isVideoStream) throws Exception {
        final long totalSize = downloadLink.getDownloadSize();

        UrlCollection urls = getUrlPair(downloadLink);
        GetRequest request = null;
        final String dashName;
        final String dashChunksProperty;

        // final String dashLoadedProperty;
        // final String dashFinishedProperty;
        final long chunkOffset;
        if (isVideoStream) {
            request = new GetRequest(urls.video);
            dashName = getDashVideoFileName(downloadLink);
            dashChunksProperty = DASH_VIDEO_CHUNKS;

            chunkOffset = 0;
        } else {
            request = new GetRequest(urls.audio);
            dashName = getDashAudioFileName(downloadLink);
            dashChunksProperty = DASH_AUDIO_CHUNKS;

            chunkOffset = data.getDashVideoSize();
        }
        request.setProxy(br.getProxy());
        final String dashPath = new File(downloadLink.getDownloadDirectory(), dashName).getAbsolutePath();
        final DownloadLink dashLink = new DownloadLink(this, dashName, getHost(), request.getUrl(), true);
        dashLink.setLivePlugin(this);
        final LinkStatus videoLinkStatus = new LinkStatus(dashLink);
        Downloadable dashDownloadable = new DownloadLinkDownloadable(dashLink) {

            long[] chunkProgress = null;
            {
                Object ret = downloadLink.getProperty(dashChunksProperty, null);
                if (ret != null && ret instanceof long[]) {
                    chunkProgress = (long[]) ret;
                } else {
                    if (ret != null && ret instanceof List) {
                        /* restored json-object */
                        List<Object> list = ((List<Object>) ret);
                        long[] ret2 = new long[list.size()];
                        for (int i = 0; i < ret2.length; i++) {
                            ret2[i] = Long.valueOf(list.get(0).toString());
                        }
                        chunkProgress = ret2;
                    }
                }
            }

            @Override
            public void setResumeable(boolean value) {
                downloadLink.setResumeable(value);
            }

            @Override
            public boolean isResumable() {
                return true;
            }

            @Override
            public void addDownloadTime(long ms) {
                downloadLink.addDownloadTime(ms);
            }

            @Override
            public void setHashResult(HashResult result) {
            }

            @Override
            public void setFinalFileOutput(String absolutePath) {
            }

            @Override
            public String getFinalFileOutput() {
                return dashPath;
            }

            @Override
            public void lockFiles(File... files) throws FileIsLockedException {
                /**
                 * do nothing, handleDownload does all the locking
                 */
            }

            @Override
            public void unlockFiles(File... files) {
                /**
                 * do nothing, handleDownload does all the locking
                 */
            }

            @Override
            public void waitForNextConnectionAllowed() throws InterruptedException {
                YoutubeDashV2.this.waitForNextConnectionAllowed(downloadLink);
            }

            @Override
            public String getFileOutput() {
                return dashPath;
            }

            @Override
            public int getLinkStatus() {
                return videoLinkStatus.getStatus();
            }

            @Override
            public long getVerifiedFileSize() {
                if (isVideoStream) {
                    return data.getDashVideoSize();
                } else {
                    return data.getDashAudioSize();
                }
            }

            @Override
            public long getKnownDownloadSize() {
                if (isVideoStream) {
                    return data.getDashVideoSize();
                } else {
                    return data.getDashAudioSize();
                }
            }

            @Override
            public void setDownloadTotalBytes(long l) {
            }

            @Override
            public long[] getChunksProgress() {
                return chunkProgress;
            }

            @Override
            public void setChunksProgress(long[] ls) {
                chunkProgress = ls;
                if (ls == null) {
                    downloadLink.setProperty(dashChunksProperty, Property.NULL);
                } else {
                    downloadLink.setProperty(dashChunksProperty, ls);
                }
            }

            @Override
            public void setLinkStatus(int finished) {
                if (isVideoStream) {

                    data.setDashVideoFinished(LinkStatus.FINISHED == finished);

                } else {
                    data.setDashAudioFinished(LinkStatus.FINISHED == finished);

                }

            }

            @Override
            public void setVerifiedFileSize(long length) {
                if (isVideoStream) {
                    if (length >= 0) {
                        data.setDashVideoSize(length);

                    } else {
                        data.setDashVideoSize(-1);
                    }
                } else {
                    if (length >= 0) {
                        data.setDashAudioSize(length);

                    } else {
                        data.setDashAudioSize(-1);
                    }
                }

            }

            @Override
            public String getFinalFileName() {
                return dashName;
            }

            @Override
            public void setFinalFileName(String newfinalFileName) {
            }

            @Override
            public long getDownloadTotalBytes() {
                if (isVideoStream) {
                    return data.getDashVideoBytesLoaded();
                } else {
                    return data.getDashAudioBytesLoaded();
                }

            }

            @Override
            public boolean isDoFilesizeCheckEnabled() {
                return true;
            }

            @Override
            public void setDownloadBytesLoaded(long bytes) {
                if (isVideoStream) {
                    if (bytes < 0) {
                        data.setDashVideoBytesLoaded(0);
                    } else {
                        data.setDashVideoBytesLoaded(bytes);
                    }
                } else {
                    if (bytes < 0) {
                        data.setDashAudioBytesLoaded(0);
                    } else {
                        data.setDashAudioBytesLoaded(bytes);
                    }
                }

                downloadLink.setDownloadCurrent(chunkOffset + bytes);
            }

            @Override
            public boolean isHashCheckEnabled() {
                return false;
            }

            @Override
            public SingleDownloadController getDownloadLinkController() {
                return downloadLink.getDownloadLinkController();
            }

            @Override
            public PluginProgress setPluginProgress(final PluginProgress progress) {
                if (progress != null && progress instanceof DownloadPluginProgress) {
                    DownloadPluginProgress dashVideoProgress = new DashDownloadPluginProgress(this, (DownloadInterface) progress.getProgressSource(), progress.getColor(), totalSize, progress, chunkOffset);
                    return downloadLink.setPluginProgress(dashVideoProgress);
                }
                return downloadLink.setPluginProgress(progress);
            }
        };
        dl = BrowserAdapter.openDownload(br, dashDownloadable, request, true, getChunksPerStream());
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("audio") && !this.dl.getConnection().getContentType().startsWith("application")) {
            if (dl.getConnection().getResponseCode() == 500) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI._.hoster_servererror("Youtube"), 5 * 60 * 1000l); }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean ret = dl.startDownload();
        if (dl.externalDownloadStop()) return null;
        return ret;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        handlePremium(downloadLink, null);

    }

    public static void main(String[] args) {
        for (YoutubeVariant v : YoutubeVariant.values()) {

            // @Default(lngs = { "en" }, values = { "240p MP4-Video" })
            // String YoutubeVariant_name_MP4_DASH_240_AAC128();
            System.out.println("@Default(lngs = { \"en\" }, values = { \"" + v.getQualityExtension() + "\" })");
            ;
            System.out.println("String YoutubeVariant_qualityExtension_" + v.name() + "();");
        }
    }

    private int getChunksPerStream() {
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        if (!cfg.isCustomChunkValueEnabled()) return 0;
        int maxChunks = cfg.getChunksCount();
        if (maxChunks <= 0) {
            maxChunks = 0;
        }
        return maxChunks;
    }

    public boolean hasConfig() {
        return true;
    }

    public void handleDash(final DownloadLink downloadLink, final YoutubeProperties data, Account account) throws Exception {

        DownloadLinkView oldView = downloadLink.getView();
        try {
            downloadLink.setView(new DefaultDownloadLinkViewImpl() {
                @Override
                public long getBytesLoaded() {
                    if (data.isDashVideoFinished()) {

                        return super.getBytesLoaded() + data.getDashVideoSize();
                    } else {
                        return super.getBytesLoaded();
                    }

                }

            });
            FFmpeg ffmpeg = new FFmpeg();

            // debug

            requestFileInformation(downloadLink);

            final SingleDownloadController dlc = downloadLink.getDownloadLinkController();
            List<File> locks = new ArrayList<File>();
            locks.addAll(listProcessFiles(downloadLink));
            try {
                for (File lock : locks) {
                    logger.info("Lock " + lock);
                    dlc.lockFile(lock);
                }
                String videoStreamPath = getVideoStreamPath(downloadLink);
                if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                    data.setDashVideoFinished(true);

                }
                YoutubeVariantInterface variant = getVariant(downloadLink);
                boolean loadVideo = !data.isDashVideoFinished();

                if (videoStreamPath == null || variant.getType() == YoutubeVariantInterface.DownloadType.DASH_AUDIO) {
                    /* Skip video if just audio should be downloaded */
                    loadVideo = false;
                } else {
                    loadVideo |= !new File(videoStreamPath).exists();
                }

                if (loadVideo) {
                    /* videoStream not finished yet, resume/download it */
                    Boolean ret = downloadDashStream(downloadLink, data, true);
                    if (ret == null) return;
                    if (!ret) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (new File(getAudioStreamPath(downloadLink)).exists()) {
                    data.setDashAudioFinished(true);
                }
                /* videoStream is finished */
                boolean loadAudio = !data.isDashAudioFinished();
                loadAudio |= !new File(getAudioStreamPath(downloadLink)).exists();
                if (loadAudio) {
                    /* audioStream not finished yet, resume/download it */
                    Boolean ret = downloadDashStream(downloadLink, data, false);
                    if (ret == null) return;
                    if (!ret) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (new File(getAudioStreamPath(downloadLink)).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                    /* audioStream also finished */
                    /* Do we need an exception here? If a Video is downloaded it is always finished before the audio part. TheCrap */
                    if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                        FFMpegProgress progress = new FFMpegProgress();
                        progress.setProgressSource(this);
                        PluginProgress old = null;
                        try {
                            old = downloadLink.setPluginProgress(progress);
                            if (ffmpeg.muxToMp4(progress, downloadLink.getFileOutput(), videoStreamPath, getAudioStreamPath(downloadLink))) {
                                downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                new File(videoStreamPath).delete();
                                new File(getAudioStreamPath(downloadLink)).delete();
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                            }
                        } finally {
                            downloadLink.compareAndSetPluginProgress(progress, old);
                        }
                    } else {
                        if (variant instanceof YoutubeVariant) {
                            YoutubeVariant ytVariant = (YoutubeVariant) variant;

                            if (ytVariant.getFileExtension().toLowerCase(Locale.ENGLISH).equals("aac")) {

                                FFMpegProgress progress = new FFMpegProgress();
                                progress.setProgressSource(this);
                                PluginProgress old = null;
                                try {
                                    old = downloadLink.setPluginProgress(progress);

                                    if (ffmpeg.generateAac(progress, downloadLink.getFileOutput(), getAudioStreamPath(downloadLink))) {
                                        downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                        new File(getAudioStreamPath(downloadLink)).delete();
                                    } else {
                                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                                    }
                                } finally {
                                    downloadLink.compareAndSetPluginProgress(progress, old);
                                }
                            } else if (ytVariant.getFileExtension().toLowerCase(Locale.ENGLISH).equals("m4a")) {

                                FFMpegProgress progress = new FFMpegProgress();
                                progress.setProgressSource(this);
                                PluginProgress old = null;
                                try {
                                    old = downloadLink.setPluginProgress(progress);

                                    if (ffmpeg.generateM4a(progress, downloadLink.getFileOutput(), getAudioStreamPath(downloadLink))) {
                                        downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                        new File(getAudioStreamPath(downloadLink)).delete();
                                    } else {
                                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                                    }
                                } finally {
                                    downloadLink.compareAndSetPluginProgress(progress, old);
                                }

                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }

                    }
                }
            } catch (final FileIsLockedException e) {
                throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
            } finally {
                for (File lock : locks) {
                    dlc.unlockFile(lock);
                }
            }
        } finally {
            downloadLink.setView(oldView);
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);
        cfg = PluginJsonConfig.get(YoutubeConfig.class);

        if (!downloadLink.getDownloadURL().startsWith("youtubev2://")) {
            convertOldLink(downloadLink);
        }
        YoutubeHelper helper = getCachedHelper();
        YoutubeVariantInterface variant = getVariant(downloadLink);

        if (account != null) {
            helper.login(account, false, false);
        }
        // if (!Application.isJared(null)) throw new RuntimeException("Shit happened");
        boolean resume = true;
        switch (variant.getType()) {
        case IMAGE:
            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink).data, resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("image/")) {
                if (dl.getConnection().getResponseCode() == 500) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI._.hoster_servererror("Youtube"), 5 * 60 * 1000l); }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            break;
        case SUBTITLES:

            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink).data, resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("text/xml")) {
                if (dl.getConnection().getResponseCode() == 500) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI._.hoster_servererror("Youtube"), 5 * 60 * 1000l); }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }

            break;
        case VIDEO:

            if (variant instanceof YoutubeVariant) {
                if (((YoutubeVariant) variant).name().contains("DEMUX") || ((YoutubeVariant) variant).name().contains("MP3")) {
                    checkFFmpeg(downloadLink, _GUI._.YoutubeDash_handleDownload_youtube_demux());
                }
            }

            this.setBrowserExclusive();
            //

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink).video, resume, getChunksPerStream());
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("application")) {
                if (dl.getConnection().getResponseCode() == 500) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI._.hoster_servererror("Youtube"), 5 * 60 * 1000l); }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            break;
        case DASH_AUDIO:
        case DASH_VIDEO:
            checkFFmpeg(downloadLink, _GUI._.YoutubeDash_handleDownload_youtube_dash());
            handleDash(downloadLink, data, null);
            break;

        }
        if (variant.hasConverer(downloadLink)) {
            long lastMod = new File(downloadLink.getFileOutput()).lastModified();
            variant.convert(downloadLink);

            try {

                if (lastMod > 0 && JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                    new File(downloadLink.getFileOutput()).setLastModified(lastMod);
                }
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
        }

        switch (variant.getType()) {
        case SUBTITLES:
            // rename subtitles to match the videos.
            // this code
            if (cfg.isSubtitleCopyforEachVideoVariant()) {
                FilePackage pkg = downloadLink.getParentNode();
                boolean readL2 = pkg.getModifyLock().readLock();
                File finalFile = new File(downloadLink.getFileOutput(false, false));
                boolean copied = false;
                try {
                    String myID = downloadLink.getStringProperty(YoutubeHelper.YT_ID, null);
                    for (DownloadLink child : pkg.getChildren()) {
                        try {
                            if (myID.equals(child.getStringProperty(YoutubeHelper.YT_ID, null))) {
                                LinkVariant v = getActiveVariantByLink(child);
                                if (v instanceof YoutubeVariant) {
                                    switch (((YoutubeVariant) v).getGroup()) {
                                    case VIDEO:
                                    case VIDEO_3D:
                                        String ext = Files.getExtension(child.getFinalFileName());
                                        if (StringUtils.isNotEmpty(ext)) {
                                            String base = child.getFinalFileName().substring(0, child.getFinalFileName().length() - ext.length() - 1);

                                            String code = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, "");
                                            Locale locale = TranslationFactory.stringToLocale(code);

                                            File newFile;
                                            IO.copyFile(finalFile, newFile = new File(finalFile.getParentFile(), base + "." + locale.getDisplayLanguage() + ".srt"));

                                            try {

                                                if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                                                    newFile.setLastModified(finalFile.lastModified());
                                                }
                                            } catch (final Throwable e) {
                                                LogSource.exception(logger, e);
                                            }
                                            downloadLink.setFinalFileOutput(newFile.getAbsolutePath());
                                            downloadLink.setFinalFileName(newFile.getName());
                                            copied = true;
                                        }
                                        break;
                                    default:
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            getLogger().log(e);
                        }

                    }
                    if (copied) finalFile.delete();
                } finally {
                    pkg.getModifyLock().readUnlock(readL2);
                }
            }

            break;
        }

    }

    private void checkFFmpeg(DownloadLink downloadLink, String reason) throws SkipReasonException, InterruptedException {
        FFmpeg ffmpeg = new FFmpeg();
        synchronized (DownloadWatchDog.getInstance()) {

            if (!ffmpeg.isAvailable()) {
                if (UpdateController.getInstance().getHandler() == null) {
                    getLogger().warning("Please set FFMPEG: BinaryPath in advanced options");
                    throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                }
                FFMpegInstallProgress progress = new FFMpegInstallProgress();
                progress.setProgressSource(this);
                PluginProgress old = null;
                try {
                    old = downloadLink.setPluginProgress(progress);
                    FFmpegProvider.getInstance().install(progress, reason);
                } finally {
                    downloadLink.compareAndSetPluginProgress(progress, old);
                }
                ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
                if (!ffmpeg.isAvailable()) {
                    //

                    List<String> requestedInstalls = UpdateController.getInstance().getHandler().getRequestedInstalls();
                    if (requestedInstalls != null && requestedInstalls.contains(org.jdownloader.controlling.ffmpeg.InstallThread.getFFmpegExtensionName())) {
                        throw new SkipReasonException(SkipReason.UPDATE_RESTART_REQUIRED);

                    } else {
                        throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                    }

                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                    // _GUI._.YoutubeDash_handleFree_ffmpegmissing());
                }
            }
        }
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        resetStreamUrls(downloadLink);
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);
        data.setDashAudioBytesLoaded(0);
        data.setDashAudioFinished(false);
        data.setDashAudioSize(0);
        data.setDashVideoBytesLoaded(0);
        data.setDashVideoFinished(false);
        data.setDashVideoSize(0);
        downloadLink.setProperty(DASH_VIDEO_CHUNKS, Property.NULL);
        downloadLink.setProperty(DASH_AUDIO_CHUNKS, Property.NULL);

    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getDescription() {
        return "JDownloader's YouTube Plugin helps downloading videoclips from youtube.com. YouTube provides different video formats and qualities. JDownloader is able to extract audio after download, and save it as mp3 file. \r\n - Hear your favourite YouTube Clips on your MP3 Player.";
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = super.listProcessFiles(link);

        try {
            YoutubeVariantInterface variant = getVariant(link);
            if (variant == null) return ret;
            ret.addAll(variant.listProcessFiles(link));
            switch (variant.getType()) {
            case DASH_AUDIO:
            case DASH_VIDEO:
                String vs = getVideoStreamPath(link);
                String as = getAudioStreamPath(link);
                if (StringUtils.isNotEmpty(vs)) {
                    // aac only does not have video streams
                    ret.add(new File(vs));
                    ret.add(new File(vs + ".part"));
                }
                if (StringUtils.isNotEmpty(as)) {
                    ret.add(new File(as));
                    ret.add(new File(as + ".part"));
                }

                break;

            default:

            }
        } catch (PluginException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private YoutubeHelper getCachedHelper() {
        YoutubeHelper ret = cachedHelper;
        if (ret == null || ret.getBr() != this.br) {
            ret = new YoutubeHelper(br, PluginJsonConfig.get(YoutubeConfig.class), getLogger());

        }
        ret.setupProxy();
        return ret;
    }

    public String getAudioStreamPath(DownloadLink link) throws PluginException {
        String audioFilenName = getDashAudioFileName(link);
        if (StringUtils.isEmpty(audioFilenName)) return null;
        return new File(link.getDownloadDirectory(), audioFilenName).getAbsolutePath();
    }

    public String getDashAudioFileName(DownloadLink link) throws PluginException {
        YoutubeVariantInterface var = getVariant(link);
        switch (var.getType()) {
        case DASH_AUDIO:
        case DASH_VIDEO:
            break;
        default:
            return null;
        }
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var.getiTagVideo() + "_" + var.getiTagAudio() + ".dashAudio";
    }

    public String getVideoStreamPath(DownloadLink link) throws PluginException {
        String videoFileName = getDashVideoFileName(link);
        if (StringUtils.isEmpty(videoFileName)) return null;
        return new File(link.getDownloadDirectory(), videoFileName).getAbsolutePath();
    }

    public String getDashVideoFileName(DownloadLink link) throws PluginException {
        YoutubeVariantInterface var = getVariant(link);
        switch (var.getType()) {
        case DASH_AUDIO:
        case DASH_VIDEO:
            break;
        default:
            return null;
        }
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var.getiTagVideo() + "_" + var.getiTagAudio() + ".dashVideo";
    }

    public class SubtitleVariant implements YoutubeVariantInterface {

        private Locale locale;
        private String code;

        public SubtitleVariant(String code) {
            this.code = code;
            locale = TranslationFactory.stringToLocale(code);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof SubtitleVariant)) return false;
            return code.equals(((SubtitleVariant) obj).code);
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }

        public String getName() {
            return _GUI._.YoutubeDash_getName_subtitles_(locale.getDisplayName());
        }

        public Icon getIcon() {
            return null;
        }

        @Override
        public String getFileExtension() {
            return "srt";
        }

        @Override
        public String getUniqueId() {
            return code;
        }

        @Override
        public String getMediaTypeID() {
            return VariantGroup.SUBTITLES.name();
        }

        @Override
        public YoutubeITAG getiTagVideo() {
            return null;
        }

        @Override
        public YoutubeITAG getiTagAudio() {
            return null;
        }

        @Override
        public YoutubeITAG getiTagData() {
            return null;
        }

        @Override
        public double getQualityRating() {
            return 0;
        }

        @Override
        public String getTypeId() {
            return code;
        }

        @Override
        public DownloadType getType() {
            return DownloadType.SUBTITLES;
        }

        @Override
        public VariantGroup getGroup() {
            return VariantGroup.SUBTITLES;
        }

        @Override
        public void convert(DownloadLink downloadLink) {
        }

        @Override
        public String getQualityExtension() {
            return code;
        }

        @Override
        public String modifyFileName(String formattedFilename, DownloadLink link) {
            return formattedFilename;
        }

        @Override
        public boolean hasConverer(DownloadLink downloadLink) {
            return false;
        }

        @Override
        public List<File> listProcessFiles(DownloadLink link) {
            return null;
        }

        @Override
        public String getExtendedName() {
            return _GUI._.YoutubeDash_getName_subtitles_(locale.getDisplayName());
        }

    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        Object variant = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT, null);

        if (variant != null && variant instanceof SubtitleVariant) {
            //
            return (LinkVariant) variant;
        }
        if (variant != null && variant instanceof YoutubeVariantInterface) {
            //
            return (YoutubeVariantInterface) variant;
        }
        String name = downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT, null);
        try {
            if (name != null) {

                YoutubeVariantInterface v = getCachedHelper().getVariantById(name);
                if (v == YoutubeVariant.SUBTITLES) {

                    SubtitleVariant var = new SubtitleVariant(downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE));
                    downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, var);
                    return var;
                } else {
                    downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, v);
                    downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, v);
                    return v;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().log(e);
        }
        return null;
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {

        if (variant == null) return;
        if (variant instanceof SubtitleVariant) {

            // reset Streams urls. we need new ones

            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.setVerifiedFileSize(-1);
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, Property.NULL);
            downloadLink.setProperty(YoutubeHelper.YT_VARIANT, YoutubeVariant.SUBTITLES.name());
            downloadLink.setProperty(YoutubeHelper.YT_EXT, YoutubeVariant.SUBTITLES.getFileExtension());
            downloadLink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE, ((SubtitleVariant) variant).code);
            String filename;
            downloadLink.setFinalFileName(filename = getCachedHelper().createFilename(downloadLink));
            downloadLink.setUrlDownload("youtubev2://" + YoutubeVariant.SUBTITLES + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/");
            try {
                downloadLink.setLinkID("youtubev2://" + YoutubeVariant.SUBTITLES + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/" + URLEncode.encodeRFC2396(filename));
            } catch (UnsupportedEncodingException e) {
                downloadLink.setLinkID("youtubev2://" + YoutubeVariant.SUBTITLES + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/" + filename);
            }

        } else if (variant instanceof YoutubeVariantInterface) {
            YoutubeVariantInterface v = (YoutubeVariantInterface) variant;
            // reset Streams urls. we need new ones

            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.setVerifiedFileSize(-1);
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, Property.NULL);
            downloadLink.setProperty(YoutubeHelper.YT_VARIANT, v.getUniqueId());
            downloadLink.setProperty(YoutubeHelper.YT_EXT, v.getFileExtension());
            String filename;
            downloadLink.setFinalFileName(filename = getCachedHelper().createFilename(downloadLink));
            downloadLink.setUrlDownload("youtubev2://" + v.getUniqueId() + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/");
            try {
                downloadLink.setLinkID("youtubev2://" + v.getUniqueId() + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/" + URLEncode.encodeRFC2396(filename));
            } catch (UnsupportedEncodingException e) {
                downloadLink.setLinkID("youtubev2://" + v.getUniqueId() + "/" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "/" + filename);
            }

        }
        if (downloadLink.getStringProperty(YoutubeHelper.YT_TITLE, null) == null) {
            // old link?
            String oldLinkName = downloadLink.getStringProperty("name", null);
            downloadLink.setFinalFileName(oldLinkName);
        }

    }

    protected void resetStreamUrls(DownloadLink downloadLink) {
        downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_AUDIO, Property.NULL);
        downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_VIDEO, Property.NULL);
        downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, Property.NULL);
    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        return downloadLink.hasProperty(YoutubeHelper.YT_VARIANTS);
    }

    @Override
    public List<LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        if (hasVariantToChooseFrom(downloadLink) == false) return null;

        Object ret = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANTS, null);
        if (ret != null) return (List<LinkVariant>) ret;
        String lngCodes = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE_LIST, null);
        if (StringUtils.isNotEmpty(lngCodes)) {

            // subtitles variants
            List<LinkVariant> ret2 = new ArrayList<LinkVariant>();
            for (String code : JSonStorage.restoreFromString(lngCodes, new TypeRef<ArrayList<String>>() {
            })) {
                ret2.add(new SubtitleVariant(code));
            }
            Collections.sort(ret2, new Comparator<LinkVariant>() {

                @Override
                public int compare(LinkVariant o1, LinkVariant o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());

                }

            });
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANTS, ret2);
            return ret2;
        }
        String idsString = downloadLink.getStringProperty(YoutubeHelper.YT_VARIANTS, "[]");
        ArrayList<String> ids = JSonStorage.restoreFromString(idsString, new TypeRef<ArrayList<String>>() {
        });
        List<LinkVariant> ret2 = new ArrayList<LinkVariant>();
        for (String id : ids) {
            try {
                ret2.add(getCachedHelper().getVariantById(id));
            } catch (Exception e) {
                getLogger().log(e);
            }
        }
        downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANTS, ret2);
        return ret2;
    }

    @Override
    public void extendLinkgrabberContextMenu(final JComponent parent, final PluginView<CrawledLink> pv) {
        final JMenu setVariants = new JMenu(_GUI._.YoutubeDashV2_extendLinkgrabberContextMenu_context_menu());
        setVariants.setIcon(DomainInfo.getInstance(getHost()).getFavIcon());
        setVariants.setEnabled(false);

        final JMenu addVariants = new JMenu(_GUI._.YoutubeDashV2_extendLinkgrabberContextMenu_context_menu_add());

        addVariants.setIcon(new BadgeIcon(DomainInfo.getInstance(getHost()).getFavIcon(), new AbstractIcon(IconKey.ICON_ADD, 16), 4, 4));
        addVariants.setEnabled(false);
        new Thread("Collect Variants") {
            public void run() {
                HashMap<String, YoutubeVariantInterface> map = new HashMap<String, YoutubeVariantInterface>();
                final HashMap<String, ArrayList<YoutubeVariantInterface>> list = new HashMap<String, ArrayList<YoutubeVariantInterface>>();
                for (CrawledLink cl : pv.getChildren()) {
                    List<LinkVariant> v = getVariantsByLink(cl.getDownloadLink());
                    if (v != null) {
                        for (LinkVariant lv : v) {
                            if (lv instanceof YoutubeVariantInterface) {
                                if (map.put(((YoutubeVariantInterface) lv).getTypeId(), (YoutubeVariantInterface) lv) == null) {
                                    ArrayList<YoutubeVariantInterface> l = list.get(((YoutubeVariantInterface) lv).getGroup().name());
                                    if (l == null) {
                                        l = new ArrayList<YoutubeVariantInterface>();
                                        list.put(((YoutubeVariantInterface) lv).getGroup().name(), l);
                                    }
                                    l.add((YoutubeVariantInterface) lv);
                                }
                            }
                        }
                    }
                }

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        add(setVariants, addVariants, pv, list, VariantGroup.VIDEO);
                        add(setVariants, addVariants, pv, list, VariantGroup.AUDIO);

                        add(setVariants, addVariants, pv, list, VariantGroup.IMAGE);

                        add(setVariants, addVariants, pv, list, VariantGroup.VIDEO_3D);
                        add(setVariants, addVariants, pv, list, VariantGroup.SUBTITLES);

                    }

                    private void add(JMenu setVariants, JMenu addVariants, final PluginView<CrawledLink> pv, HashMap<String, ArrayList<YoutubeVariantInterface>> map, final VariantGroup group) {
                        ArrayList<YoutubeVariantInterface> list = map.get(group.name());
                        if (list == null || list.size() == 0) return;
                        final Comparator<YoutubeVariantInterface> comp;
                        Collections.sort(list, comp = new Comparator<YoutubeVariantInterface>() {

                            @Override
                            public int compare(YoutubeVariantInterface o1, YoutubeVariantInterface o2) {
                                return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
                            }
                        });
                        setVariants.setEnabled(true);
                        addVariants.setEnabled(true);

                        addToAddMenu(addVariants, pv, map, group, list, comp);
                        addToSetMenu(setVariants, pv, map, group, list, comp);

                    }

                    protected void addToAddMenu(JMenu addSubmenu, final PluginView<CrawledLink> pv, HashMap<String, ArrayList<YoutubeVariantInterface>> map, final VariantGroup group, ArrayList<YoutubeVariantInterface> list, final Comparator<YoutubeVariantInterface> comp) {
                        JMenu groupMenu = new JMenu(group.getLabel());
                        if (map.size() == 1) {
                            groupMenu = addSubmenu;
                        } else {
                            addSubmenu.add(groupMenu);
                        }
                        groupMenu.add(new JMenuItem(new BasicAction() {
                            {
                                setName(_GUI._.YoutubeDashV2_add_best(group.getLabel()));
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                for (CrawledLink cl : pv.getChildren()) {
                                    List<YoutubeVariantInterface> variants = new ArrayList<YoutubeVariantInterface>();
                                    for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                        if (v instanceof YoutubeVariantInterface) variants.add((YoutubeVariantInterface) v);
                                    }
                                    Collections.sort(variants, comp);
                                    for (YoutubeVariantInterface variant : variants) {
                                        if (variant.getGroup() == group) {
                                            CrawledLink newLink = LinkCollector.getInstance().addAdditional(cl, variant);

                                            if (newLink != null) {
                                                checkableLinks.add(newLink);
                                            } else {
                                                Toolkit.getDefaultToolkit().beep();
                                            }
                                            break;
                                        }

                                    }

                                }

                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                            }

                        }));
                        for (final YoutubeVariantInterface v : list) {
                            groupMenu.add(new JMenuItem(new BasicAction() {
                                {

                                    setName(CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled() ? v.getExtendedName() : v.getName());

                                }

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                    for (CrawledLink cl : pv.getChildren()) {
                                        for (LinkVariant variants : getVariantsByLink(cl.getDownloadLink())) {
                                            if (variants instanceof YoutubeVariantInterface) {
                                                if (((YoutubeVariantInterface) variants).getTypeId().equals(v.getTypeId())) {
                                                    CrawledLink newLink = LinkCollector.getInstance().addAdditional(cl, v);

                                                    if (newLink != null) {
                                                        checkableLinks.add(newLink);
                                                    } else {
                                                        Toolkit.getDefaultToolkit().beep();
                                                    }
                                                }
                                            }
                                        }

                                    }

                                    LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                    linkChecker.check(checkableLinks);
                                }

                            }));

                        }

                        groupMenu.add(new JMenuItem(new BasicAction() {
                            {
                                setName(_GUI._.YoutubeDashV2_add_worst(group.getLabel()));
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                for (CrawledLink cl : pv.getChildren()) {
                                    List<YoutubeVariantInterface> variants = new ArrayList<YoutubeVariantInterface>();
                                    for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                        if (v instanceof YoutubeVariantInterface) variants.add((YoutubeVariantInterface) v);
                                    }
                                    Collections.sort(variants, comp);
                                    for (int i = variants.size() - 1; i >= 0; i--) {
                                        YoutubeVariantInterface variant = variants.get(i);
                                        if (variant.getGroup() == group) {
                                            CrawledLink newLink = LinkCollector.getInstance().addAdditional(cl, variant);

                                            if (newLink != null) {
                                                checkableLinks.add(newLink);
                                            } else {
                                                Toolkit.getDefaultToolkit().beep();
                                            }
                                            break;
                                        }

                                    }

                                }

                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                            }

                        }));
                    }

                    protected void addToSetMenu(JMenu setVariants, final PluginView<CrawledLink> pv, HashMap<String, ArrayList<YoutubeVariantInterface>> map, final VariantGroup group, ArrayList<YoutubeVariantInterface> list, final Comparator<YoutubeVariantInterface> comp) {
                        JMenu groupMenu = new JMenu(group.getLabel());
                        if (map.size() == 1) {
                            groupMenu = setVariants;
                        } else {
                            setVariants.add(groupMenu);
                        }
                        groupMenu.add(new JMenuItem(new BasicAction() {
                            {
                                setName(_GUI._.YoutubeDashV2_add_best(group.getLabel()));
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                for (CrawledLink cl : pv.getChildren()) {
                                    List<YoutubeVariantInterface> variants = new ArrayList<YoutubeVariantInterface>();
                                    for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                        if (v instanceof YoutubeVariantInterface) variants.add((YoutubeVariantInterface) v);
                                    }
                                    Collections.sort(variants, comp);
                                    for (YoutubeVariantInterface variant : variants) {
                                        if (variant.getGroup() == group) {
                                            LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), variant);

                                            checkableLinks.add(cl);
                                            break;
                                        }

                                    }

                                }

                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                            }

                        }));
                        for (final YoutubeVariantInterface v : list) {
                            groupMenu.add(new JMenuItem(new BasicAction() {
                                {
                                    setName(CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled() ? v.getExtendedName() : v.getName());
                                }

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                    for (CrawledLink cl : pv.getChildren()) {
                                        for (LinkVariant variants : getVariantsByLink(cl.getDownloadLink())) {
                                            if (variants instanceof YoutubeVariantInterface) {
                                                if (((YoutubeVariantInterface) variants).getTypeId().equals(v.getTypeId())) {
                                                    LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), v);
                                                    checkableLinks.add(cl);
                                                }
                                            }
                                        }

                                    }

                                    LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                    linkChecker.check(checkableLinks);
                                }

                            }));

                        }

                        groupMenu.add(new JMenuItem(new BasicAction() {
                            {
                                setName(_GUI._.YoutubeDashV2_add_worst(group.getLabel()));
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                for (CrawledLink cl : pv.getChildren()) {
                                    List<YoutubeVariantInterface> variants = new ArrayList<YoutubeVariantInterface>();
                                    for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                        if (v instanceof YoutubeVariantInterface) variants.add((YoutubeVariantInterface) v);
                                    }
                                    Collections.sort(variants, comp);
                                    for (int i = variants.size() - 1; i >= 0; i--) {
                                        YoutubeVariantInterface variant = variants.get(i);
                                        if (variant.getGroup() == group) {
                                            LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), variant);

                                            checkableLinks.add(cl);
                                            break;
                                        }

                                    }

                                }

                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                            }

                        }));
                    }
                };

            };
        }.start();

        parent.add(setVariants);
        parent.add(addVariants);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    protected void setConfigElements() {
    }

    @Override
    public void reset() {
    }
}
