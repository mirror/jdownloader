package jd.plugins.hoster;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.controlling.DownloadLinkView;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.segment.SegmentDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.components.youtube.ClipDataCache;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeFinalLinkResource;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.keepForCompatibility.SubtitleVariantOld;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.DownloadType;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.YoutubeSubtitleStorable;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.QueryInfo;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashResult;
import jd.plugins.hoster.youtube.YoutubeDashConfigPanel;
import jd.plugins.hoster.youtube.YoutubeLinkGrabberExtender;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com" }, urls = { "youtubev2://.+" }, flags = { 2 })
public class YoutubeDashV2 extends PluginForHost {

    private static final String    YT_ALTERNATE_VARIANT = "YT_ALTERNATE_VARIANT";

    private static final String    DASH_AUDIO_FINISHED  = "DASH_AUDIO_FINISHED";

    private static final String    DASH_VIDEO_FINISHED  = "DASH_VIDEO_FINISHED";

    private static final String    DASH_AUDIO_LOADED    = "DASH_AUDIO_LOADED";

    private static final String    DASH_VIDEO_LOADED    = "DASH_VIDEO_LOADED";

    private final String           DASH_AUDIO_CHUNKS    = "DASH_AUDIO_CHUNKS";

    private final String           DASH_VIDEO_CHUNKS    = "DASH_VIDEO_CHUNKS";

    private YoutubeDashConfigPanel configPanel;

    /**
     * Maybe useful in the future - this guy has a lot of knowledge and makes good scripts:
     * https://github.com/bromix/plugin.video.youtube/tree/master/resources/lib
     */
    @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }

    @Override
    public Class<? extends ConfigInterface> getConfigInterface() {
        return YoutubeConfig.class;
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public void setBrowser(Browser brr) {

        if (CFG_YOUTUBE.CFG.isProxyEnabled()) {
            final HTTPProxyStorable proxy = CFG_YOUTUBE.CFG.getProxy();

            if (proxy != null) {
                HTTPProxy prxy = HTTPProxy.getHTTPProxy(proxy);
                if (prxy != null) {
                    this.br.setProxy(prxy);
                } else {

                }
                return;
            }

        }
        super.setBrowser(brr);

    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {

            new YoutubeHelper(br, getLogger()).login(account, true, true);

        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        //
        ai.setStatus(_GUI.T.lit_account_is_ok());
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
            final long ret = chunkOffset + ((DownloadInterface) progress.getProgressSource()).getTotalLinkBytesLoadedLive();
            return ret;
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

    public String getMirrorID(DownloadLink link) {
        return "Youtube:" + link.getStringProperty(YoutubeHelper.YT_VARIANT) + link.getName() + "_" + link.getView().getBytesTotal();
    }

    private boolean isDownloading = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {

        String id = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);

        if (CFG_YOUTUBE.CFG.isFastLinkCheckEnabled() && !LinkChecker.isForcedLinkCheck(downloadLink) && downloadLink.getDownloadLinkController() == null) {

            return AvailableStatus.UNCHECKED;

        }

        YoutubeHelper helper = new YoutubeHelper(br, getLogger());
        helper.setupProxy();

        AbstractVariant variant = getVariant(downloadLink);

        // update linkid
        final String linkid = downloadLink.getLinkID();
        if (linkid != null && !isDownloading) {
            switch (variant.getType()) {
            case SUBTITLES: {

                final String subtitleID = YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), variant);
                if (!subtitleID.equals(linkid)) {
                    // update it
                    downloadLink.setLinkID(subtitleID);
                    return AvailableStatus.TRUE;
                }
                break;
            }
            case VIDEO:
            case DASH_AUDIO:
            case DASH_VIDEO: {
                final String videoID = YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), variant);

                if (!videoID.equals(linkid)) {
                    // update it
                    downloadLink.setLinkID(videoID);
                    return AvailableStatus.TRUE;
                }
                break;
            }
            case IMAGE:
                break;
            case DESCRIPTION:
                break;
            default:
                break;
            }
        }

        URLConnectionAdapter con = null;
        boolean verifiedSize = true;
        long totalSize = -1;
        // youtube uses redirects - maybe for load balancing
        br.setFollowRedirects(true);
        switch (variant.getType()) {
        case SUBTITLES:
            for (int i = 0; i < 2; i++) {
                VariantInfo urls = getAndUpdateVariantInfo(downloadLink);
                if (urls == null || urls.getDataStreams() == null || urls.getDataStreams().size() == 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String encoding = br.getHeaders().get("Accept-Encoding");
                br.getHeaders().remove("Accept-Encoding");
                try {
                    GetRequest r = new GetRequest(urls.getDataStreams().get(0).getUrl());

                    br.openRequestConnection(r).disconnect();

                    con = r.getHttpConnection();

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
        case DESCRIPTION:
            return requestFileInformationDescription(downloadLink, id, helper);
        case IMAGE:
            for (int i = 0; i < 2; i++) {
                VariantInfo urls = getAndUpdateVariantInfo(downloadLink);
                if (urls == null || urls.getDataStreams() == null || urls.getDataStreams().size() == 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                con = br.openGetConnection(urls.getDataStreams().get(0).getUrl());
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
        case HLS_VIDEO:
            return AvailableStatus.TRUE;

        default:
            br.setFollowRedirects(true);

            HashSet<LinkVariant> checkedAlternatives = new HashSet<LinkVariant>();
            AbstractVariant orgVariant = getVariant(downloadLink);
            try {
                int maxAlternativesChecks = 10;
                boolean triedToResetTheCache = false;
                test: while (maxAlternativesChecks-- > 0) {

                    AbstractVariant vv;
                    checkedAlternatives.add(vv = getVariant(downloadLink));
                    try {
                        // do no set variant, do this inloop

                        totalSize = 0;
                        VariantInfo urls = getAndUpdateVariantInfo(downloadLink);
                        YoutubeFinalLinkResource workingVideoStream = null;
                        YoutubeFinalLinkResource workingAudioStream = null;
                        YoutubeFinalLinkResource workingDataStream = null;

                        if (urls != null && urls.getVideoStreams() != null) {
                            PluginException firstException = null;
                            for (YoutubeStreamData si : urls.getVideoStreams()) {

                                YoutubeFinalLinkResource cache = new YoutubeFinalLinkResource(si);

                                if (cache.getSegments() != null) {
                                    verifiedSize = false;
                                    long estimatedSize = guessTotalSize(cache.getBaseUrl(), cache.getSegments());
                                    if (estimatedSize == -1) {

                                        if (firstException == null) {
                                            firstException = new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                        }
                                        continue;
                                    }
                                    workingVideoStream = cache;
                                    // downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, cache);
                                    totalSize += estimatedSize;
                                    firstException = null;
                                    break;
                                } else {
                                    String url = cache.getBaseUrl();
                                    if (false && vv.getQualityRating() > VideoResolution.P_360.getRating()) {
                                        url = url.replace("signature=", "signature=BAD");
                                    }
                                    br.openRequestConnection(new HeadRequest(url)).disconnect();
                                    con = br.getHttpConnection();
                                    if (con.getResponseCode() == 200) {
                                        workingVideoStream = cache;
                                        // downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, cache);
                                        totalSize += con.getLongContentLength();
                                        data.setDashVideoSize(con.getLongContentLength());
                                        firstException = null;
                                        break;
                                    } else {

                                        // if (i == 0) {
                                        // resetStreamUrls(downloadLink);
                                        // continue;
                                        // }
                                        if (firstException == null) {
                                            firstException = new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                        }
                                        continue;

                                    }
                                }
                            }
                            if (firstException != null) {
                                throw firstException;
                            }
                        }
                        if (variant.getiTagAudioOrVideoItagEquivalent() != variant.getiTagVideo() && urls != null && urls.getAudioStreams() != null) {
                            PluginException firstException = null;

                            for (YoutubeStreamData si : urls.getAudioStreams()) {
                                YoutubeFinalLinkResource cache = new YoutubeFinalLinkResource(si);
                                si = null;
                                if (cache.getSegments() != null) {
                                    verifiedSize = false;
                                    long estimatedSize = guessTotalSize(cache.getBaseUrl(), cache.getSegments());
                                    if (estimatedSize == -1) {
                                        // if (i == 0) {
                                        // resetStreamUrls(downloadLink);
                                        // continue;
                                        // }
                                        if (firstException == null) {
                                            firstException = new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                        }
                                        continue;
                                    }
                                    totalSize += estimatedSize;
                                    workingAudioStream = cache;
                                    ;
                                    // downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, );
                                    firstException = null;
                                    break;
                                } else {
                                    String url = cache.getBaseUrl();
                                    br.openRequestConnection(new HeadRequest(url)).disconnect();
                                    con = br.getHttpConnection();
                                    if (con.getResponseCode() == 200) {
                                        workingAudioStream = cache;
                                        // downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, new YoutubeFinalLinkResource(si));
                                        totalSize += con.getLongContentLength();
                                        data.setDashAudioSize(con.getLongContentLength());
                                        firstException = null;
                                        break;
                                    } else {

                                        // if (i == 0) {
                                        // resetStreamUrls(downloadLink);
                                        // continue;
                                        // }
                                        if (firstException == null) {
                                            firstException = new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                        }
                                        continue;
                                    }
                                }
                            }
                            if (firstException != null) {
                                throw firstException;
                            }
                        }
                        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, workingAudioStream);
                        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, workingVideoStream);
                        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_DATA, workingDataStream);

                        if (totalSize <= 0) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        break test;

                    } catch (PluginException e) {

                        if (e.getErrorMessage() != null) {

                            if (StringUtils.equalsIgnoreCase(e.getErrorMessage(), "This video is private")) {
                                throw e;
                            }

                            if (e.getErrorMessage().contains(_JDT.T.CountryIPBlockException_createCandidateResult())) {
                                throw e;
                            }
                        }
                        boolean hasCachedVideo = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO) != null;
                        boolean hasCachedAudio = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO) != null;
                        boolean hasCachedData = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_DATA) != null;

                        if (hasCachedVideo || hasCachedAudio || hasCachedData || !triedToResetTheCache) {
                            // the url has been restored from older cached streamdata
                            downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, Property.NULL);
                            downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, Property.NULL);
                            downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_DATA, Property.NULL);
                            if (!triedToResetTheCache) {
                                triedToResetTheCache = true;
                                ClipDataCache.clearCache(downloadLink);
                            }
                            continue test;
                        }
                        if (e.getErrorMessage() != null) {

                            if (StringUtils.equalsIgnoreCase(e.getErrorMessage(), "This video is private")) {
                                throw e;
                            }

                            if (e.getErrorMessage().contains(_JDT.T.CountryIPBlockException_createCandidateResult())) {
                                throw e;
                            }
                        }

                        if (con != null && con.getResponseCode() == 403) {
                            // Probably IP Mismatch
                            // do not check alternatives
                            throw e;
                        }
                        List<LinkVariant> alternatives = getVariantsByLink(downloadLink);
                        if (alternatives != null) {
                            for (LinkVariant v : alternatives) {

                                if (!checkedAlternatives.contains(v)) {
                                    logger.info("Try next alternative variant: " + v);

                                    downloadLink.getTempProperties().setProperty(YT_ALTERNATE_VARIANT, v);
                                    continue test;

                                }
                            }
                        }
                        downloadLink.getTempProperties().removeProperty(YT_ALTERNATE_VARIANT);
                        throw e;
                    }

                }
            } finally {

                AbstractVariant alternative = (AbstractVariant) downloadLink.getTempProperties().getProperty(YT_ALTERNATE_VARIANT);
                downloadLink.getTempProperties().removeProperty(YT_ALTERNATE_VARIANT);
                if (alternative != null) {
                    LinkCollector.getInstance().setActiveVariantForLink(downloadLink, alternative);

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

        downloadLink.setInternalTmpFilenameAppend(null);
        AbstractVariant v = getVariant(downloadLink);
        if (v.hasConverter(downloadLink)) {
            downloadLink.setInternalTmpFilenameAppend(".tmp");
        }

        if (verifiedSize && totalSize > 0) {
            downloadLink.setVerifiedFileSize(totalSize);
        } else if (!verifiedSize && totalSize > 0) {
            downloadLink.setDownloadSize(totalSize);
        }

        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationDescription(DownloadLink downloadLink, String id, YoutubeHelper helper) throws Exception, UnsupportedEncodingException {
        String description = downloadLink.getTempProperties().getStringProperty(YoutubeHelper.YT_DESCRIPTION);
        if (StringUtils.isEmpty(description)) {
            YoutubeClipData vid;
            vid = ClipDataCache.get(helper, id);
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_DESCRIPTION, description = vid.description);
        }
        downloadLink.setDownloadSize(description.getBytes("UTF-8").length);
        return AvailableStatus.TRUE;
    }

    private long guessTotalSize(String base, String[] segs) throws IOException {
        long videoSize = 0;
        int max = Math.min(11, segs.length);
        for (int i = 1; i < max; i++) {

            String url = segs[i].toLowerCase(Locale.ENGLISH).startsWith("http") ? segs[i] : (base + segs[i]);
            br.openRequestConnection(new HeadRequest(url)).disconnect();
            URLConnectionAdapter con = br.getHttpConnection();
            if (con.getResponseCode() == 200) {

                videoSize += con.getLongContentLength();
            } else {
                return -1;
            }
        }
        // first segment is a init segment and has only ~802 bytes
        return (segs.length - 1) * (videoSize / (max - 1)) + 802;
    }

    private VariantInfo getAndUpdateVariantInfo(DownloadLink downloadLink) throws Exception {
        // VariantInfo variantInfo = (VariantInfo) downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT_INFO);
        // if (variantInfo != null) {
        //
        // return variantInfo;
        // }

        YoutubeFinalLinkResource video = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
        YoutubeFinalLinkResource audio = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, YoutubeFinalLinkResource.TYPE_REF);
        YoutubeFinalLinkResource data = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_DATA, YoutubeFinalLinkResource.TYPE_REF);
        if (video != null || audio != null || data != null) {
            // seems like we have cached final informations
            ArrayList<YoutubeStreamData> lstAudio = null;
            ArrayList<YoutubeStreamData> lstVideo = null;
            ArrayList<YoutubeStreamData> lstData = null;
            if (video != null) {
                lstVideo = new ArrayList<YoutubeStreamData>();
                lstVideo.add(video.toStreamDataObject());
            }

            if (audio != null) {
                lstAudio = new ArrayList<YoutubeStreamData>();
                lstAudio.add(audio.toStreamDataObject());
            }
            if (data != null) {
                lstData = new ArrayList<YoutubeStreamData>();
                lstData.add(data.toStreamDataObject());
            }

            VariantInfo ret = new VariantInfo(getVariant(downloadLink), lstAudio, lstVideo, lstData);
            if (ret.isValid()) {
                return ret;
            }
        }

        return updateUrls(downloadLink);
        // variantInfo = (VariantInfo) downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANT_INFO);
        //
        // return variantInfo;
    }

    private String[] getStringArrayFromDownloadlinkProperty(DownloadLink downloadLink, String key) {
        String json = downloadLink.getStringProperty(key);
        if (json == null) {
            return null;
        }
        return JSonStorage.restoreFromString(json, String[].class);
    }

    protected AbstractVariant getVariant(DownloadLink downloadLink) throws PluginException {
        Object alternative = downloadLink.getTempProperties().getProperty(YT_ALTERNATE_VARIANT);
        if (alternative != null && alternative instanceof AbstractVariant) {
            return (AbstractVariant) alternative;
        }

        AbstractVariant ret = AbstractVariant.get(downloadLink);

        if (ret == null) {
            getLogger().warning("Invalid Variant: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
            throw new PluginException(LinkStatus.ERROR_FATAL, "INVALID VARIANT: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));

        }
        return ret;

    }

    private VariantInfo updateUrls(DownloadLink downloadLink) throws Exception {
        AbstractVariant variant = getVariant(downloadLink);

        YoutubeClipData clipData = ClipDataCache.get(new YoutubeHelper(br, getLogger()), downloadLink);

        if (variant instanceof SubtitleVariant) {
            SubtitleVariant stVariant = ((SubtitleVariant) variant);

            for (YoutubeSubtitleStorable si : clipData.subtitles) {

                if (StringUtils.equals(stVariant.getGenericInfo()._getUniqueId(), si._getUniqueId())) {

                    stVariant.setGenericInfo(si);
                    SubtitleVariantInfo vi = new SubtitleVariantInfo(stVariant, clipData);
                    // downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, vi);

                    return vi;
                }

            }
            return null;
        }
        List<YoutubeStreamData> audioStreams = clipData.getStreams(variant.getBaseVariant().getiTagAudio());
        List<YoutubeStreamData> videoStreams = clipData.getStreams(variant.getiTagVideo());
        List<YoutubeStreamData> dataStreams = clipData.getStreams(variant.getiTagData());
        VariantInfo vi = new VariantInfo(variant, audioStreams, videoStreams, dataStreams);
        // downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, vi);
        return vi;
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

        // VariantInfo urls = getUrlPair(downloadLink);

        final String dashName;
        final String dashChunksProperty;

        // final String dashLoadedProperty;
        // final String dashFinishedProperty;
        final long chunkOffset;
        YoutubeFinalLinkResource streamData = null;
        if (isVideoStream) {
            streamData = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);

            dashName = getDashVideoFileName(downloadLink);
            dashChunksProperty = DASH_VIDEO_CHUNKS;
            chunkOffset = 0;
        } else {
            streamData = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, YoutubeFinalLinkResource.TYPE_REF);
            dashName = getDashAudioFileName(downloadLink);
            dashChunksProperty = DASH_AUDIO_CHUNKS;
            final AbstractVariant variant = getVariant(downloadLink);
            if (variant.getType() == DownloadType.DASH_AUDIO) {
                chunkOffset = 0;
            } else {
                chunkOffset = data.getDashVideoSize();
            }
        }

        final String dashPath = new File(downloadLink.getDownloadDirectory(), dashName).getAbsolutePath();
        final DownloadLink dashLink = new DownloadLink(this, dashName, getHost(), streamData.getBaseUrl(), true) {
            @Override
            public SingleDownloadController getDownloadLinkController() {
                return downloadLink.getDownloadLinkController();
            }
        };
        dashLink.setLivePlugin(this);
        final LinkStatus videoLinkStatus = new LinkStatus(dashLink);
        final String host = Browser.getHost(streamData.getBaseUrl());
        Downloadable dashDownloadable = new DownloadLinkDownloadable(dashLink) {

            volatile long[] chunkProgress = null;

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
            public String getHost() {
                return host;
            }

            @Override
            public void setResumeable(boolean value) {
                downloadLink.setResumeable(value);
            }

            public long[] getChunksProgress() {
                return chunkProgress;
            }

            public void setChunksProgress(long[] ls) {
                chunkProgress = ls;
                if (ls == null) {
                    downloadLink.setProperty(dashChunksProperty, Property.NULL);
                } else {
                    downloadLink.setProperty(dashChunksProperty, ls);
                }
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
                long ret = -1l;
                if (isVideoStream) {
                    ret = data.getDashVideoSize();
                } else {
                    ret = data.getDashAudioSize();
                }
                if (ret <= 0) {
                    ret = -1;
                }
                return ret;
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

            final HashMap<PluginProgress, PluginProgress> pluginProgressMap = new HashMap<PluginProgress, PluginProgress>();

            @Override
            public void addPluginProgress(final PluginProgress progress) {
                final PluginProgress mapped;
                synchronized (pluginProgressMap) {
                    if (pluginProgressMap.containsKey(progress)) {
                        mapped = pluginProgressMap.get(progress);
                    } else if (progress != null && progress instanceof DownloadPluginProgress) {
                        mapped = new DashDownloadPluginProgress(this, (DownloadInterface) progress.getProgressSource(), progress.getColor(), totalSize, progress, chunkOffset);
                        pluginProgressMap.put(progress, mapped);
                    } else {
                        mapped = progress;
                    }
                }
                downloadLink.addPluginProgress(mapped);
            }

            @Override
            public boolean removePluginProgress(PluginProgress remove) {
                final PluginProgress mapped;
                synchronized (pluginProgressMap) {
                    if (pluginProgressMap.containsKey(remove)) {
                        mapped = pluginProgressMap.remove(remove);
                    } else {
                        mapped = remove;
                    }
                }
                return downloadLink.removePluginProgress(mapped);
            }

        };

        GetRequest request = new GetRequest(streamData.getBaseUrl());
        String[] segments = streamData.getSegments();

        if (segments != null) {

            dl = new SegmentDownloader(dashLink, dashDownloadable, br, request.getUrl(), segments);
            return dl.startDownload();

        }
        List<HTTPProxy> possibleProxies = br.getProxy().getProxiesByURL(request.getURL());
        request.setProxy((possibleProxies == null || possibleProxies.size() == 0) ? null : possibleProxies.get(0));

        dl = BrowserAdapter.openDownload(br, dashDownloadable, request, true, getChunksPerStream());

        if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("audio") && !this.dl.getConnection().getContentType().startsWith("application")) {
            if (dl.getConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI.T.hoster_servererror("Youtube"), 5 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean ret = dl.startDownload();
        if (dl.externalDownloadStop()) {
            return null;
        }

        return ret;

    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        handlePremium(downloadLink, null);

    }

    private int getChunksPerStream() {
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);
        if (!cfg.isCustomChunkValueEnabled()) {
            return 0;
        }
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

        DownloadLinkView oldView = null;
        DefaultDownloadLinkViewImpl newView = null;
        try {
            newView = new DefaultDownloadLinkViewImpl() {
                @Override
                public long getBytesLoaded() {
                    if (data.isDashVideoFinished()) {
                        return super.getBytesLoaded() + data.getDashVideoSize();
                    } else {
                        return super.getBytesLoaded();
                    }

                }
            };
            oldView = downloadLink.setView(newView);
            FFmpeg ffmpeg = new FFmpeg();

            // debug

            requestFileInformation(downloadLink);

            final SingleDownloadController dlc = downloadLink.getDownloadLinkController();
            final List<File> locks = new ArrayList<File>();
            locks.addAll(listProcessFiles(downloadLink));
            try {
                new DownloadLinkDownloadable(downloadLink).checkIfWeCanWrite(new ExceptionRunnable() {

                    @Override
                    public void run() throws Exception {
                        try {
                            for (File lock : locks) {
                                logger.info("Lock " + lock);
                                dlc.lockFile(lock);
                            }
                        } catch (FileIsLockedException e) {
                            for (File lock : locks) {
                                dlc.unlockFile(lock);
                            }
                            throw e;
                        }

                    }
                }, null);

                final String videoStreamPath = getVideoStreamPath(downloadLink);
                if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                    data.setDashVideoFinished(true);

                }
                AbstractVariant variant = getVariant(downloadLink);
                boolean loadVideo = !data.isDashVideoFinished();
                if (videoStreamPath == null || variant.getType() == DownloadType.DASH_AUDIO) {
                    /* Skip video if just audio should be downloaded */
                    loadVideo = false;
                } else {
                    loadVideo |= !(new File(videoStreamPath).exists() && new File(videoStreamPath).length() > 0);
                }

                if (loadVideo) {
                    /* videoStream not finished yet, resume/download it */
                    Boolean ret = downloadDashStream(downloadLink, data, true);
                    if (ret == null) {
                        return;
                    }
                    if (!ret) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                /* videoStream is finished */
                final String audioStreamPath = getAudioStreamPath(downloadLink);
                if (audioStreamPath != null && new File(audioStreamPath).exists()) {
                    data.setDashAudioFinished(true);
                }
                boolean loadAudio = !data.isDashAudioFinished();
                loadAudio |= !(new File(audioStreamPath).exists() && new File(audioStreamPath).length() > 0);
                if (loadAudio) {
                    /* audioStream not finished yet, resume/download it */
                    Boolean ret = downloadDashStream(downloadLink, data, false);
                    if (ret == null) {
                        return;
                    }
                    if (!ret) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (new File(audioStreamPath).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                    /* audioStream also finished */
                    /* Do we need an exception here? If a Video is downloaded it is always finished before the audio part. TheCrap */

                    if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                        final FFMpegProgress progress = new FFMpegProgress();
                        progress.setProgressSource(this);
                        try {
                            downloadLink.addPluginProgress(progress);
                            VideoCodec codec = variant.getiTagVideo().getVideoCodec();
                            switch (codec) {
                            case VP8:
                            case VP9:
                            case VP9_BETTER_PROFILE_1:
                            case VP9_BETTER_PROFILE_2:
                            case VP9_WORSE_PROFILE_1:
                                if (ffmpeg.muxToWebm(progress, downloadLink.getFileOutput(), videoStreamPath, audioStreamPath)) {
                                    downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                    new File(videoStreamPath).delete();
                                    new File(audioStreamPath).delete();
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());

                                }
                                break;
                            default:
                                if (ffmpeg.muxToMp4(progress, downloadLink.getFileOutput(), videoStreamPath, audioStreamPath)) {
                                    downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                    new File(videoStreamPath).delete();
                                    new File(audioStreamPath).delete();
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());

                                }
                            }

                        } finally {
                            downloadLink.removePluginProgress(progress);
                        }
                    } else {

                        // YoutubeVariant ytVariant = (variant instanceof YoutubeCustomConvertVariant) ? ((YoutubeCustomConvertVariant)
                        // variant).getSource() : (YoutubeVariant) variant;
                        switch (variant.getContainer()) {
                        case AAC:

                            FFMpegProgress progress = new FFMpegProgress();
                            progress.setProgressSource(this);
                            try {
                                downloadLink.addPluginProgress(progress);

                                if (ffmpeg.generateAac(progress, downloadLink.getFileOutput(), audioStreamPath)) {
                                    downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                    new File(audioStreamPath).delete();
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());

                                }
                            } finally {
                                downloadLink.removePluginProgress(progress);
                            }
                            break;
                        case M4A:
                            progress = new FFMpegProgress();
                            progress.setProgressSource(this);
                            try {
                                downloadLink.addPluginProgress(progress);

                                if (ffmpeg.generateM4a(progress, downloadLink.getFileOutput(), audioStreamPath)) {
                                    downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                    new File(audioStreamPath).delete();
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());

                                }
                            } finally {
                                downloadLink.removePluginProgress(progress);
                            }
                            break;
                        case OGG:
                            progress = new FFMpegProgress();
                            progress.setProgressSource(this);
                            try {
                                downloadLink.addPluginProgress(progress);

                                if (ffmpeg.generateOggAudio(progress, downloadLink.getFileOutput(), audioStreamPath)) {
                                    downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                                    new File(audioStreamPath).delete();
                                } else {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, _GUI.T.YoutubeDash_handleFree_error_());

                                }
                            } finally {
                                downloadLink.removePluginProgress(progress);
                            }
                        }

                    }
                }
            } catch (final FileIsLockedException e) {
                logger.log(e);
                throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
            } finally {

                for (File lock : locks) {
                    dlc.unlockFile(lock);
                }

            }
        } finally {
            if (oldView != null) {
                downloadLink.setView(oldView);
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        isDownloading = true;
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);

        YoutubeHelper helper = new YoutubeHelper(br, getLogger());
        AbstractVariant variant = getVariant(downloadLink);

        if (account != null) {
            helper.login(account, false, false);
        }
        // if (!Application.isJared(null)) throw new RuntimeException("Shit happened");
        boolean resume = true;
        switch (variant.getType()) {
        case DESCRIPTION:
            downloadDescription(downloadLink);
            return;
        case IMAGE:
            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getAndUpdateVariantInfo(downloadLink).getDataStreams().get(0).getUrl(), resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("image/")) {
                if (dl.getConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI.T.hoster_servererror("Youtube"), 5 * 60 * 1000l);
                }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            break;
        case SUBTITLES:

            this.setBrowserExclusive();

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);

            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getAndUpdateVariantInfo(downloadLink).getDataStreams().get(0).getUrl(), resume, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("text/xml")) {
                if (dl.getConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI.T.hoster_servererror("Youtube"), 5 * 60 * 1000l);
                }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

            break;
        case VIDEO:

            if (variant.getBaseVariant().name().contains("DEMUX") || variant.getBaseVariant().name().contains("MP3")) {
                checkFFmpeg(downloadLink, _GUI.T.YoutubeDash_handleDownload_youtube_demux());
            }

            this.setBrowserExclusive();
            //

            this.requestFileInformation(downloadLink);
            this.br.setDebug(true);
            // downloadLink.setInternalTmpFilenameAppend(fileName);

            YoutubeFinalLinkResource sd = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, sd.getBaseUrl(), resume, getChunksPerStream());
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video") && !this.dl.getConnection().getContentType().startsWith("application")) {
                if (dl.getConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, _GUI.T.hoster_servererror("Youtube"), 5 * 60 * 1000l);
                }

                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if (!this.dl.startDownload()) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            break;
        case HLS_VIDEO:
            checkFFmpeg(downloadLink, "HLS Download");

            dl = new HLSDownloader(downloadLink, br, getAndUpdateVariantInfo(downloadLink).getVideoStreams().get(0).getUrl());
            ((HLSDownloader) dl).setAcceptDownloadStopAsValidEnd(true);
            if (!this.dl.startDownload()) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            break;
        case DASH_AUDIO:
        case DASH_VIDEO:
            checkFFmpeg(downloadLink, _GUI.T.YoutubeDash_handleDownload_youtube_dash());
            handleDash(downloadLink, data, null);
            break;

        }
        if (variant.hasConverter(downloadLink)) {
            long lastMod = new File(downloadLink.getFileOutput()).lastModified();

            variant.convert(downloadLink, this);

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
            if (CFG_YOUTUBE.CFG.isSubtitleCopyforEachVideoVariant()) {
                FilePackage pkg = downloadLink.getParentNode();
                boolean readL2 = pkg.getModifyLock().readLock();
                File finalFile = new File(downloadLink.getFileOutput(false, false));
                boolean copied = false;
                try {
                    String myID = downloadLink.getStringProperty(YoutubeHelper.YT_ID, null);
                    for (DownloadLink child : pkg.getChildren()) {
                        try {
                            if (myID.equals(child.getStringProperty(YoutubeHelper.YT_ID, null))) {
                                AbstractVariant v = getVariant(child);

                                switch (v.getGroup()) {
                                case VIDEO:
                                case VIDEO_3D:
                                    String ext = Files.getExtension(child.getFinalFileName());
                                    if (StringUtils.isNotEmpty(ext)) {
                                        String base = child.getFinalFileName().substring(0, child.getFinalFileName().length() - ext.length() - 1);

                                        Locale locale = new SubtitleVariantOld(downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, ""))._getLocale();

                                        File newFile;
                                        IO.copyFile(finalFile, newFile = new File(finalFile.getParentFile(), base + "." + locale.getDisplayLanguage() + ".srt"));

                                        try {

                                            if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                                                newFile.setLastModified(finalFile.lastModified());
                                            }
                                        } catch (final Throwable e) {
                                            LogSource.exception(logger, e);
                                        }

                                        downloadLink.setFinalFileName(newFile.getName());
                                        copied = true;
                                    }
                                    break;
                                default:
                                    break;
                                }

                            }
                        } catch (Exception e) {
                            getLogger().log(e);
                        }

                    }
                    if (copied) {
                        finalFile.delete();
                    }
                } finally {
                    pkg.getModifyLock().readUnlock(readL2);
                }
            }

            break;
        }

    }

    private void downloadDescription(DownloadLink downloadLink) throws Exception {
        if (requestFileInformation(downloadLink) != AvailableStatus.TRUE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String description = downloadLink.getTempProperties().getStringProperty(YoutubeHelper.YT_DESCRIPTION, null);
        if (description == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final byte[] bytes = description.getBytes("UTF-8");
        final File outputFile = new File(downloadLink.getFileOutput());
        final DiskSpaceReservation reservation = new DiskSpaceReservation() {

            @Override
            public long getSize() {
                return Math.max(0, bytes.length - outputFile.length());
            }

            @Override
            public File getDestination() {
                return outputFile;
            }
        };
        //
        final DownloadLinkDownloadable downloadable = new DownloadLinkDownloadable(downloadLink);
        if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {

            @Override
            public void run() throws Exception {
                downloadable.checkAndReserve(reservation);
                try {
                    downloadable.lockFiles(outputFile);
                } catch (FileIsLockedException e) {
                    downloadable.unlockFiles(outputFile);
                    throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                }
            }
        }, null)) {
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
        }
        try {
            final long size = bytes.length;
            IO.writeToFile(outputFile, bytes);
            downloadable.setDownloadTotalBytes(size);
            downloadable.setDownloadBytesLoaded(size);
            downloadable.setLinkStatus(LinkStatus.FINISHED);
        } finally {
            downloadable.unlockFiles(outputFile);
        }
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return super.isProxyRotationEnabledForLinkChecker();
    }

    @Override
    public void resetDownloadlink(DownloadLink downloadLink) {
        resetStreamUrls(downloadLink);
        YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);
        data.setDashAudioBytesLoaded(0);
        data.setDashAudioFinished(false);
        data.setDashAudioSize(-1);
        data.setDashVideoBytesLoaded(0);
        data.setDashVideoFinished(false);
        data.setDashVideoSize(-1);
        downloadLink.setProperty(DASH_VIDEO_CHUNKS, Property.NULL);
        downloadLink.setProperty(DASH_AUDIO_CHUNKS, Property.NULL);
        ClipDataCache.clearCache(downloadLink);

    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getDescription() {
        return "JDownloader's YouTube Plugin helps downloading videoclips from youtube.com. YouTube provides different video formats and qualities. JDownloader is able to extract audio after download, and save it as mp3 file. \r\n - Hear your favourite YouTube Clips on your MP3 Player.";
    }

    protected FilePair[] listFilePairsToMove(DownloadLink link, String currentDirectory, String currentName, String newDirectory, String newName) {
        List<FilePair> ret = new ArrayList<PluginForHost.FilePair>();
        ret.add(new FilePair(new File(new File(currentDirectory), currentName + ".part"), new File(new File(newDirectory), newName + ".part")));
        ret.add(new FilePair(new File(new File(currentDirectory), currentName), new File(new File(newDirectory), newName)));
        try {
            AbstractVariant<?> variant = getVariant(link);
            if (variant != null) {

                for (File f : variant.listProcessFiles(link)) {
                    FilePair fp = new FilePair(new File(new File(currentDirectory), f.getName()), new File(new File(newDirectory), newName + f.getName().substring(currentName.length())));
                    ret.add(fp);
                    fp = new FilePair(new File(new File(currentDirectory), f.getName() + ".part"), new File(new File(newDirectory), newName + f.getName().substring(currentName.length()) + ".part"));
                    ret.add(fp);
                }

                switch (variant.getType()) {
                case DASH_AUDIO:
                case DASH_VIDEO:
                    String vs = getVideoStreamPath(link);
                    String as = getAudioStreamPath(link);
                    if (StringUtils.isNotEmpty(vs)) {
                        // aac only does not have video streams
                        // ret.add(new File(vs));
                        // ret.add(new File(vs + ".part"));

                        ret.add(new FilePair(new File(new File(currentDirectory), new File(vs).getName() + ".part"), new File(new File(newDirectory), new File(vs).getName() + ".part")));
                        ret.add(new FilePair(new File(new File(currentDirectory), new File(vs).getName()), new File(new File(newDirectory), new File(vs).getName())));

                    }
                    if (StringUtils.isNotEmpty(as)) {
                        ret.add(new FilePair(new File(new File(currentDirectory), new File(as).getName() + ".part"), new File(new File(newDirectory), new File(as).getName() + ".part")));
                        ret.add(new FilePair(new File(new File(currentDirectory), new File(as).getName()), new File(new File(newDirectory), new File(as).getName())));
                    }

                    break;

                default:

                }
            }
        } catch (PluginException e) {
            e.printStackTrace();
        }

        return ret.toArray(new FilePair[] {});
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        List<File> ret = super.listProcessFiles(link);

        try {
            AbstractVariant variant = getVariant(link);
            if (variant == null) {
                return ret;
            }
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

    public String getAudioStreamPath(DownloadLink link) throws PluginException {
        String audioFilenName = getDashAudioFileName(link);
        if (StringUtils.isEmpty(audioFilenName)) {
            return null;
        }
        return new File(link.getDownloadDirectory(), audioFilenName).getAbsolutePath();
    }

    public String getDashAudioFileName(DownloadLink link) throws PluginException {
        AbstractVariant var = getVariant(link);
        switch (var.getType()) {
        case DASH_AUDIO:
        case DASH_VIDEO:
            break;
        default:
            return null;
        }
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var._getUniqueId() + ".dashAudio";
    }

    public String getVideoStreamPath(DownloadLink link) throws PluginException {
        String videoFileName = getDashVideoFileName(link);
        if (StringUtils.isEmpty(videoFileName)) {
            return null;
        }
        return new File(link.getDownloadDirectory(), videoFileName).getAbsolutePath();
    }

    public String getDashVideoFileName(DownloadLink link) throws PluginException {
        AbstractVariant var = getVariant(link);
        switch (var.getType()) {
        case DASH_AUDIO:
        case DASH_VIDEO:
            break;
        default:
            return null;
        }
        // add both - audio and videoid to the path. else we might get conflicts if we download 2 qualities with the same audiostream
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var._getUniqueId() + ".dashVideo";
    }

    @Override
    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {

        try {
            return getVariant(downloadLink);
        } catch (PluginException e) {
            logger.log(e);
            return null;
        }

    }

    @Override
    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        YoutubeConfig cfg = PluginJsonConfig.get(YoutubeConfig.class);

        if (variant == null) {
            return;
        }
        // if (variant instanceof SubtitleVariant) {
        //
        // // reset Streams urls. we need new ones
        //
        // resetStreamUrls(downloadLink);
        // downloadLink.setDownloadSize(-1);
        // downloadLink.setVerifiedFileSize(-1);
        // downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT, Property.NULL);
        // downloadLink.setProperty(YoutubeHelper.YT_VARIANT, VariantBase.SUBTITLES.name());
        // downloadLink.setProperty(YoutubeHelper.YT_EXT, VariantBase.SUBTITLES.getFileExtension());
        // downloadLink.setProperty(YoutubeHelper.YT_SUBTITLE_CODE, ((SubtitleVariantOld) variant)._getIdentifier());
        // String filename;
        // downloadLink.setFinalFileName(filename = getCachedHelper(downloadLink).createFilename(downloadLink));
        //
        // downloadLink.setPluginPatternMatcher(YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID),
        // (AbstractVariant) variant));
        //
        // // if (prefers) {
        // downloadLink.setContentUrl("https://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) +
        // "&variant=" + Encoding.urlEncode(((SubtitleVariantOld) variant).getTypeId()));
        // // } else {
        // // downloadLink.setContentUrl("http://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) +
        // // "&variant=" + variant);
        // //
        // // }
        // final String subtitleID = YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), (AbstractVariant)
        // variant);
        // downloadLink.setLinkID(subtitleID);
        //
        // } else
        //
        if (variant instanceof AbstractVariant) {
            AbstractVariant v = (AbstractVariant) variant;
            // reset Streams urls. we need new ones

            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.setVerifiedFileSize(-1);
            // try {
            // if (((YoutubeBasicVariant) v).getGenericInfo().getAlternatives() == null) {
            // YoutubeBasicVariant oldV = getVariant(downloadLink);
            // ((YoutubeBasicVariant) v).getGenericInfo().setAlternatives(getVariant(downloadLink).getGenericInfo().getAlternatives());
            //
            // }
            // } catch (PluginException e) {
            // e.printStackTrace();
            // }
            YoutubeHelper.writeVariantToDownloadLink(downloadLink, v);

            String filename;
            downloadLink.setFinalFileName(filename = new YoutubeHelper(br, getLogger()).createFilename(downloadLink));
            downloadLink.setPluginPatternMatcher(YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), (AbstractVariant) variant));
            // if (prefers) {
            downloadLink.setContentUrl("https://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "&variant=" + Encoding.urlEncode(((AbstractVariant) variant)._getUniqueId()));
            // } else {
            // downloadLink.setContentUrl("http://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) +
            // "&variant=" + variant);
            //
            // }
            downloadLink.setLinkID(YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), (AbstractVariant) variant));

        }
        if (downloadLink.getStringProperty(YoutubeHelper.YT_TITLE, null) == null) {
            // old link?
            String oldLinkName = downloadLink.getStringProperty("name", null);
            downloadLink.setFinalFileName(oldLinkName);
        }

    }

    protected void resetStreamUrls(DownloadLink downloadLink) {
        // downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, Property.NULL);
        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, Property.NULL);
        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, Property.NULL);
        downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_DATA, Property.NULL);

    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        String[] variantIds = getVariantsIDList(downloadLink);
        ;
        return variantIds != null && variantIds.length > 0;

    }

    private String[] getVariantsIDList(DownloadLink downloadLink) {
        // old compatibility
        Object jsonString = downloadLink.getProperty(YoutubeHelper.YT_VARIANTS);
        if (jsonString != null && jsonString instanceof String) {
            String[] lst = JSonStorage.restoreFromString((String) jsonString, TypeRef.STRING_ARRAY);
            downloadLink.setProperty(YoutubeHelper.YT_VARIANTS, lst);
        }

        String subtitles = downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE_LIST);
        if (StringUtils.isNotEmpty(subtitles)) {
            downloadLink.removeProperty(YoutubeHelper.YT_SUBTITLE_CODE_LIST);
            String[] queryList = JSonStorage.restoreFromString(subtitles, TypeRef.STRING_ARRAY);
            ArrayList<String> subtitleIDs = new ArrayList<String>();
            if (queryList != null) {
                for (String q : queryList) {
                    try {
                        SubtitleVariant v = new SubtitleVariant();
                        v.setGenericInfo(new YoutubeSubtitleStorable());
                        QueryInfo info;

                        info = Request.parseQuery(q);

                        v.getGenericInfo().setKind(info.get("kind"));
                        v.getGenericInfo().setLanguage(info.get("lng"));
                        v.getGenericInfo().setSourceLanguage(info.get("src"));
                        subtitleIDs.add(v.getStorableString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
            downloadLink.setProperty(YoutubeHelper.YT_VARIANTS, subtitleIDs.toArray(new String[] {}));
        }
        return downloadLink.getObjectProperty(YoutubeHelper.YT_VARIANTS, TypeRef.STRING_ARRAY);
    }

    @Override
    public List<LinkVariant> getVariantsByLink(final DownloadLink downloadLink) {
        if (hasVariantToChooseFrom(downloadLink) == false) {
            return null;
        }
        try {

            Object cache = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANTS, null);
            if (cache != null) {
                return (List<LinkVariant>) cache;
            }
            List<LinkVariant> ret = new ArrayList<LinkVariant>();

            String[] variantIds = getVariantsIDList(downloadLink);
            for (String s : variantIds) {
                AbstractVariant vv = AbstractVariant.get(s);
                if (vv != null) {
                    ret.add(vv);
                }
            }
            downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANTS, ret);
            return ret;
        } catch (Throwable e) {
            logger.log(e);
            ;

            return null;
        }

    }

    @Override
    public void extendLinkgrabberContextMenu(final JComponent parent, final PluginView<CrawledLink> pv, Collection<PluginView<CrawledLink>> allPvs) {
        new YoutubeLinkGrabberExtender(this, parent, pv, allPvs).run();

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
