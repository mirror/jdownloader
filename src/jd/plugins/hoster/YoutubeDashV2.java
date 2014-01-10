package jd.plugins.hoster;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.decrypter.YoutubeHelper;
import jd.plugins.decrypter.YoutubeHelper.ClipData;
import jd.plugins.decrypter.YoutubeHelper.StreamData;
import jd.plugins.decrypter.YoutubeHelper.SubtitleInfo;
import jd.plugins.decrypter.YoutubeHelper.YoutubeITAG;
import jd.plugins.decrypter.YoutubeHelper.YoutubeVariant;
import jd.plugins.decrypter.YoutubeHelper.YoutubeVariant.Type;
import jd.plugins.decrypter.YoutubeHelper.YoutubeVariant.VariantGroup;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.DownloadPluginProgress;
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
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloadcore.v15.Downloadable;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.plugins.PluginForHostV2;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision: 24000 $", interfaceVersion = 3, names = { "youtube.jd" }, urls = { "youtubev2://.+" }, flags = { 2 })
public class YoutubeDashV2 extends PluginForHostV2 {

    private final String  DASH_AUDIO_SIZE     = "DASH_AUDIO_SIZE";
    private final String  DASH_AUDIO_LOADED   = "DASH_AUDIO_LOADED";
    private final String  DASH_AUDIO_CHUNKS   = "DASH_AUDIO_CHUNKS";
    private final String  DASH_AUDIO_FINISHED = "DASH_AUDIO_FINISHED";

    private final String  DASH_VIDEO_SIZE     = "DASH_VIDEO_SIZE";
    private final String  DASH_VIDEO_LOADED   = "DASH_VIDEO_LOADED";
    private final String  DASH_VIDEO_CHUNKS   = "DASH_VIDEO_CHUNKS";
    private final String  DASH_VIDEO_FINISHED = "DASH_VIDEO_FINISHED";

    private YoutubeConfig cfg;

    @Override
    public String getAGBLink() {
        //
        return "http://youtube.com/t/terms";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            YoutubeHelper helper = new YoutubeHelper(br, PluginJsonConfig.get(YoutubeConfig.class), getLogger());
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

    public static interface YoutubeConfig extends ConfigInterface {

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
        YoutubeVariant[] getExtraVariants();

        void setExtraVariants(YoutubeVariant[] variants);

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

        @DefaultBooleanValue(true)
        @AboutConfig
        boolean isBestGroupVariantEnabled();

        void setBestGroupVariantEnabled(boolean b);

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
        return new YoutubeDashConfigPanel(getDescription());
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

        YoutubeHelper helper = new YoutubeHelper(br, cfg, getLogger());
        helper.setupProxy();
        helper.login(false, false);
        YoutubeVariant variant = getVariant(downloadLink);
        URLConnectionAdapter con;
        long totalSize = -1;
        switch (variant.getType()) {
        case SUBTITLES:
            for (int i = 0; i < 2; i++) {
                UrlCollection urls = getUrlPair(downloadLink, helper);
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
                UrlCollection urls = getUrlPair(downloadLink, helper);
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
                UrlCollection urls = getUrlPair(downloadLink, helper);

                if (StringUtils.isNotEmpty(urls.video)) {
                    con = br.openGetConnection(urls.video);
                    con.disconnect();
                    if (con.getResponseCode() == 200) {
                        totalSize += con.getLongContentLength();
                        downloadLink.setProperty(DASH_VIDEO_SIZE, con.getLongContentLength());
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
                        downloadLink.setProperty(DASH_AUDIO_SIZE, con.getLongContentLength());
                    } else {

                        if (i == 0) {
                            resetStreamUrls(downloadLink);
                            continue;
                        }
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            }

        }

        // HTTP/1.1 403 Forbidden
        // helper.login(false, false);

        // // we should cache this information:

        downloadLink.setFinalFileName(YoutubeHelper.createFilename(downloadLink));
        downloadLink.setCustomFileOutputFilenameAppend(null);
        switch (getVariant(downloadLink).getType()) {
        case FLV_TO_MP3:
            downloadLink.setCustomFileOutputFilenameAppend(".tmp");
            break;
        case SUBTITLES:
            downloadLink.setCustomFileOutputFilenameAppend(".tmp");
            break;
        default:

        }

        if (totalSize > 0) {
            downloadLink.setVerifiedFileSize(totalSize);
        }

        return AvailableStatus.TRUE;
    }

    private UrlCollection getUrlPair(DownloadLink downloadLink, YoutubeHelper helper) throws Exception {
        String videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
        String audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
        String dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        YoutubeVariant variant = getVariant(downloadLink);
        if (StringUtils.isEmpty(dataUrl) && variant.getiTagData() != null) {
            updateUrls(downloadLink, helper);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        if (StringUtils.isEmpty(videoUrl) && variant.getiTagVideo() != null) {
            updateUrls(downloadLink, helper);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        if (StringUtils.isEmpty(audioUrl) && variant.getiTagAudio() != null) {
            updateUrls(downloadLink, helper);
            videoUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO);
            audioUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO);
            dataUrl = downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_DATA);
        }
        return new UrlCollection(videoUrl, audioUrl, dataUrl);
    }

    protected YoutubeVariant getVariant(DownloadLink downloadLink) throws PluginException {
        try {
            String var = downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT);
            return YoutubeVariant.valueOf(var);
        } catch (Exception e) {
            getLogger().log(e);
            throw new PluginException(LinkStatus.ERROR_FATAL, "INVALID VARIANT: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
        }

    }

    private void updateUrls(DownloadLink downloadLink, YoutubeHelper helper) throws Exception {
        YoutubeVariant variant = getVariant(downloadLink);
        String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        ClipData vid = new ClipData(videoID);

        Map<YoutubeITAG, StreamData> info = helper.loadVideo(vid);
        if (info == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, vid.error);
        if (variant.getGroup() == VariantGroup.SUBTITLES) {

            String code = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, null);

            for (SubtitleInfo si : helper.loadSubtitles(vid)) {

                if (si.getLang().equals(code)) {

                    downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, si._getUrl(vid.videoID));
                }

            }
            return;
        }

        if (variant.getiTagData() != null) {
            StreamData data = info.get(variant.getiTagData());
            if (data == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagData() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_DATA, data.getUrl());
        }
        if (variant.getiTagAudio() != null) {
            StreamData audioStream = info.get(variant.getiTagAudio());
            if (audioStream == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagAudio() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_AUDIO, audioStream.getUrl());
        }

        if (variant.getiTagVideo() != null) {
            StreamData videoStream = info.get(variant.getiTagVideo());
            if (videoStream == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Variant not found: " + variant + "(Itag missing: " + variant.getiTagVideo() + ")");
            downloadLink.setProperty(YoutubeHelper.YT_STREAMURL_VIDEO, videoStream.getUrl());
        }

    }

    private boolean downloadDashStream(final DownloadLink downloadLink, boolean videoORAudio) throws Exception {
        final long totalSize = downloadLink.getDownloadSize();
        GetRequest request = null;
        final String dashName;
        final String dashChunksProperty;
        final String dashSizeProperty;
        final String dashLoadedProperty;
        final String dashFinishedProperty;
        final long chunkOffset;
        if (videoORAudio) {
            request = new GetRequest(downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_VIDEO));
            dashName = getDashVideoFileName(downloadLink);
            dashChunksProperty = DASH_VIDEO_CHUNKS;
            dashSizeProperty = DASH_VIDEO_SIZE;
            dashLoadedProperty = DASH_VIDEO_LOADED;
            dashFinishedProperty = DASH_VIDEO_FINISHED;
            chunkOffset = 0;
        } else {
            request = new GetRequest(downloadLink.getStringProperty(YoutubeHelper.YT_STREAMURL_AUDIO));
            dashName = getDashAudioFileName(downloadLink);
            dashChunksProperty = DASH_AUDIO_CHUNKS;
            dashSizeProperty = DASH_AUDIO_SIZE;
            dashLoadedProperty = DASH_AUDIO_LOADED;
            dashFinishedProperty = DASH_AUDIO_FINISHED;
            chunkOffset = downloadLink.getLongProperty(DASH_VIDEO_SIZE, -1l);
        }
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
                return downloadLink.getLongProperty(dashSizeProperty, -1l);
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
                if (LinkStatus.FINISHED == finished) {
                    downloadLink.setProperty(dashFinishedProperty, true);
                } else {
                    downloadLink.setProperty(dashFinishedProperty, Property.NULL);
                }
            }

            @Override
            public void setVerifiedFileSize(long length) {
                if (length >= 0) {
                    downloadLink.setProperty(dashSizeProperty, length);
                } else {
                    downloadLink.setProperty(dashSizeProperty, Property.NULL);
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
                return downloadLink.getLongProperty(dashLoadedProperty, -1);
            }

            @Override
            public boolean isDoFilesizeCheckEnabled() {
                return true;
            }

            @Override
            public void setDownloadBytesLoaded(long bytes) {
                if (bytes < 0) {
                    downloadLink.setProperty(dashLoadedProperty, Property.NULL);
                } else {
                    downloadLink.setProperty(dashLoadedProperty, bytes);
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
            public void setPluginProgress(final PluginProgress progress) {
                if (progress != null && progress instanceof DownloadPluginProgress) {
                    DownloadPluginProgress dashVideoProgress = new DownloadPluginProgress(this, (DownloadInterface) progress.getProgressSource(), progress.getColor()) {
                        @Override
                        public long getCurrent() {
                            return chunkOffset + ((DownloadInterface) progress.getProgressSource()).getTotalLinkBytesLoadedLive();
                        };

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
                    };
                    downloadLink.setPluginProgress(dashVideoProgress);
                    return;
                }
                downloadLink.setPluginProgress(progress);
            }
        };
        dl = BrowserAdapter.openDownload(br, dashDownloadable, request, true, getChunksPerStream());
        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("application")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        handlePremium(downloadLink, null);

    }

    private int getChunksPerStream() {
        int maxChunks = PluginJsonConfig.get(YoutubeConfig.class).getChunksCount();
        if (maxChunks <= 0) {
            maxChunks = 0;
        }
        return maxChunks;
    }

    public boolean hasConfig() {
        return true;
    }

    public void handleDash(final DownloadLink downloadLink, Account account) throws Exception {
        FFmpeg ffmpeg = new FFmpeg();
        if (!ffmpeg.isAvailable()) {
            FFMpegInstallProgress progress = new FFMpegInstallProgress();
            downloadLink.setPluginProgress(progress);
            try {
                FFmpegProvider.getInstance().install(progress, _GUI._.YoutubeDash_handleDownload_youtube_dash());
            } finally {
                downloadLink.setPluginProgress(null);
            }
            ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
            if (!ffmpeg.isAvailable()) {
                //
                throw new PluginException(LinkStatus.ERROR_FATAL, _GUI._.YoutubeDash_handleFree_ffmpegmissing());
            }
        }

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
                downloadLink.setProperty(DASH_VIDEO_FINISHED, true);
            }
            YoutubeVariant variant = getVariant(downloadLink);
            boolean loadVideo = !downloadLink.getBooleanProperty(DASH_VIDEO_FINISHED, false);

            if (videoStreamPath == null || variant.getType() == Type.DASH_AUDIO) {
                /* Skip video if just audio should be downloaded */
                loadVideo = false;
            } else {
                loadVideo |= !new File(videoStreamPath).exists();
            }

            if (loadVideo) {
                /* videoStream not finished yet, resume/download it */
                if (!downloadDashStream(downloadLink, true)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            if (new File(getAudioStreamPath(downloadLink)).exists()) {
                downloadLink.setProperty(DASH_AUDIO_FINISHED, true);
            }
            /* videoStream is finished */
            boolean loadAudio = !downloadLink.getBooleanProperty(DASH_AUDIO_FINISHED, false);
            loadAudio |= !new File(getAudioStreamPath(downloadLink)).exists();
            if (loadAudio) {
                /* audioStream not finished yet, resume/download it */
                if (!downloadDashStream(downloadLink, false)) {

                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            if (new File(getAudioStreamPath(downloadLink)).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                /* audioStream also finished */
                /* Do we need an exception here? If a Video is downloaded it is always finished before the audio part. TheCrap */
                if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                    FFMpegProgress progress = new FFMpegProgress();
                    downloadLink.setPluginProgress(progress);
                    try {
                        if (ffmpeg.merge(progress, downloadLink.getFileOutput(), videoStreamPath, getAudioStreamPath(downloadLink))) {
                            downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                            new File(videoStreamPath).delete();
                            new File(getAudioStreamPath(downloadLink)).delete();
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                        }
                    } finally {
                        downloadLink.setPluginProgress(null);
                    }
                } else {
                    /* Just renaming the temp-file didn't work */
                    FFMpegProgress progress = new FFMpegProgress();
                    downloadLink.setPluginProgress(progress);
                    try {
                        if (ffmpeg.generateAac(progress, downloadLink.getFileOutput(), getAudioStreamPath(downloadLink))) {
                            downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                            new File(getAudioStreamPath(downloadLink)).delete();
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI._.YoutubeDash_handleFree_error_());

                        }
                    } finally {
                        downloadLink.setPluginProgress(null);
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
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        YoutubeVariant variant = getVariant(downloadLink);
        cfg = PluginJsonConfig.get(YoutubeConfig.class);
        YoutubeHelper helper = new YoutubeHelper(br, cfg, getLogger());
        helper.setupProxy();
        if (account != null) {
            helper.login(account, false, false);
        }
        boolean resume = true;
        switch (variant.getType()) {
        case IMAGE:
            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink, helper).data, resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("image/")) {

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            break;
        case SUBTITLES:

            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink, helper).data, resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("text/xml")) {

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            break;
        case VIDEO:
        case FLV_TO_MP3:

            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getUrlPair(downloadLink, helper).video, resume, getChunksPerStream());
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("application")) {

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) { throw new PluginException(LinkStatus.ERROR_RETRY); }
            break;
        case DASH_AUDIO:
        case DASH_VIDEO:
            handleDash(downloadLink, null);
            break;
        }
        variant.convert(downloadLink);

    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        resetStreamUrls(downloadLink);
        downloadLink.setProperty("DASH_VIDEO_CHUNKS", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_CHUNKS", Property.NULL);
        downloadLink.setProperty("DASH_VIDEO_LOADED", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_LOADED", Property.NULL);
        downloadLink.setProperty("DASH_VIDEO_FINISHED", Property.NULL);
        downloadLink.setProperty("DASH_AUDIO_FINISHED", Property.NULL);

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
            switch (getVariant(link).getType()) {
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
            case FLV_TO_MP3:

                File mp3 = new File(link.getFileOutput(true, true));

                ret.add(new File(mp3 + ".avi"));
                ret.add(new File(mp3 + ".mp3"));
                ret.add(new File(mp3 + ".tmp"));
                ret.add(new File(mp3 + ".tmp.part"));
            }
        } catch (PluginException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public String getAudioStreamPath(DownloadLink link) throws PluginException {
        String audioFilenName = getDashAudioFileName(link);
        if (StringUtils.isEmpty(audioFilenName)) return null;
        return new File(link.getDownloadDirectory(), audioFilenName).getAbsolutePath();
    }

    public String getDashAudioFileName(DownloadLink link) throws PluginException {
        YoutubeVariant var = getVariant(link);
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
        YoutubeVariant var = getVariant(link);
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

    public class SubtitleVariant implements LinkVariant {

        private Locale locale;
        private String code;

        public SubtitleVariant(String code) {
            this.code = code;
            locale = Locale.forLanguageTag(code);
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

        @Override
        public String getName() {
            return _GUI._.YoutubeDash_getName_subtitles_(locale.getDisplayName());
        }

        @Override
        public Icon getIcon() {
            return null;
        }

    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        Object variant = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT, null);
        ClassLoader cl = SubtitleVariant.class.getClassLoader();
        if (variant != null && variant instanceof SubtitleVariant) {
            //
            return (LinkVariant) variant;
        }
        if (variant != null && variant instanceof YoutubeVariant) {
            //
            return (YoutubeVariant) variant;
        }
        String name = downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT, null);
        try {
            if (name != null) {

                YoutubeVariant v = YoutubeVariant.valueOf(name);
                ;

                switch (v) {
                case SUBTITLES:
                    SubtitleVariant var = new SubtitleVariant(downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE));
                    downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, var);
                    return var;
                default:
                    downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, v);
                    return v;
                }
            }
        } catch (Exception e) {
            getLogger().log(e);
        }
        return null;
    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        if (variant == null) return;
        if (variant instanceof YoutubeVariant) {
            YoutubeVariant v = (YoutubeVariant) variant;
            // reset Streams urls. we need new ones

            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.setProperty(YoutubeHelper.YT_VARIANT, v.name());
            downloadLink.setProperty(YoutubeHelper.YT_EXT, v.getFileExtension());
            downloadLink.setFinalFileName(YoutubeHelper.createFilename(downloadLink));

        } else if (variant instanceof SubtitleVariant) {

            // reset Streams urls. we need new ones

            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, Property.NULL);
            downloadLink.setProperty(YoutubeHelper.YT_VARIANT, YoutubeVariant.SUBTITLES.name());
            downloadLink.setProperty(YoutubeHelper.YT_EXT, YoutubeVariant.SUBTITLES.getFileExtension());
            downloadLink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE, ((SubtitleVariant) variant).code);
            downloadLink.setFinalFileName(YoutubeHelper.createFilename(downloadLink));
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
                ret2.add(YoutubeVariant.valueOf(id));
            } catch (Exception e) {
                getLogger().log(e);
            }
        }
        downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANTS, ret2);
        return ret2;
    }

    @Override
    public void extendLinkgrabberContextMenu(JComponent parent, PluginView<CrawledLink> pv) {
        super.extendLinkgrabberContextMenu(parent, pv);
    }

    protected void setConfigElements() {
    }

    @Override
    public void reset() {
    }
}
