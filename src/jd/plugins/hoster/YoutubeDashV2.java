package jd.plugins.hoster;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.action.BasicAction;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.controlling.DownloadLinkView;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.ffmpeg.FFMpegException;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegMetaData;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.segment.SegmentDownloader;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.gui.views.linkgrabber.columns.VariantColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.components.youtube.ClipDataCache;
import org.jdownloader.plugins.components.youtube.StreamCollection;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeFinalLinkResource;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeHostPluginInterface;
import org.jdownloader.plugins.components.youtube.YoutubeLinkGrabberExtender;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.choosevariantdialog.YoutubeVariantSelectionDialog;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeDashConfigPanel;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.keepForCompatibility.SubtitleVariantOld;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.DownloadType;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.components.youtube.variants.YoutubeSubtitleStorable;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

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
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youtube.com" }, urls = { "youtubev2://.+" })
public class YoutubeDashV2 extends PluginForHost implements YoutubeHostPluginInterface {
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
    public Class<? extends PluginConfigInterface> getConfigInterface() {
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
        List<String> ret = new ArrayList<String>();
        // update linkid
        final String linkid = downloadLink.getLinkID();
        if (linkid != null && !isDownloading) {
            switch (variant.getType()) {
            case SUBTITLES: {
                final String subtitleID = YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), variant, Arrays.asList(getVariantsIDList(downloadLink)));
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
                final String videoID = YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), variant, Arrays.asList(getVariantsIDList(downloadLink)));
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
                final String encoding = br.getHeaders().get("Accept-Encoding");
                try {
                    br.getHeaders().remove("Accept-Encoding");
                    con = br.openGetConnection(urls.getDataStreams().get(0).getUrl());
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
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
                try {
                    con = br.openGetConnection(urls.getDataStreams().get(0).getUrl());
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
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
                int maxAlternativesChecks = CFG_YOUTUBE.CFG.getAutoAlternativeSearchDepths();
                boolean hasCache = ClipDataCache.hasCache(helper, downloadLink);
                boolean triedToResetTheCache = !hasCache;
                test: while (maxAlternativesChecks-- >= 0) {
                    AbstractVariant vv;
                    checkedAlternatives.add(vv = getVariant(downloadLink));
                    try {
                        // do no set variant, do this inloop
                        totalSize = 0;
                        boolean ok = false;
                        VariantInfo urls = getAndUpdateVariantInfo(downloadLink);
                        logger.info("Try " + urls);
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
                                    ok |= estimatedSize > 0;
                                    firstException = null;
                                    break;
                                } else {
                                    String url = cache.getBaseUrl();
                                    // if (false && vv.getQualityRating() > VideoResolution.P_360.getRating()) {
                                    // url = url.replace("signature=", "signature=BAD");
                                    // }
                                    br.openRequestConnection(new HeadRequest(url)).disconnect();
                                    con = br.getHttpConnection();
                                    if (con.getResponseCode() == 200) {
                                        workingVideoStream = cache;
                                        // downloadLink.setProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, cache);
                                        totalSize += con.getLongContentLength();
                                        data.setDashVideoSize(con.getLongContentLength());
                                        firstException = null;
                                        ok |= true;
                                        break;
                                    } else {
                                        if (si.getSrc() != null) {
                                            Log.info("Failed for Stream Source: " + si.getSrc());
                                        }
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
                            for (final YoutubeStreamData si : urls.getAudioStreams()) {
                                final YoutubeFinalLinkResource cache = new YoutubeFinalLinkResource(si);
                                if (cache.getSegments() != null) {
                                    verifiedSize = false;
                                    long estimatedSize = guessTotalSize(cache.getBaseUrl(), cache.getSegments());
                                    if (estimatedSize == -1) {
                                        // if (i == 0) {
                                        // resetStreamUrls(downloadLink);
                                        // continue;
                                        // }
                                        if (si.getSrc() != null) {
                                            logger.info("Stream Source: " + si.getSrc());
                                        }
                                        if (firstException == null) {
                                            firstException = new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                        }
                                        continue;
                                    }
                                    totalSize += estimatedSize;
                                    workingAudioStream = cache;
                                    ok |= estimatedSize > 0;
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
                                        ok |= true;
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
                        if (!ok) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        break test;
                    } catch (PluginException e) {
                        if (e.getMessage() != null) {
                            if (StringUtils.equalsIgnoreCase(e.getMessage(), "This video is private")) {
                                throw e;
                            }
                            if (e.getMessage().contains(ClipDataCache.THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY)) {
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
                        if (e.getMessage() != null) {
                            if (StringUtils.equalsIgnoreCase(e.getMessage(), "This video is private")) {
                                throw e;
                            }
                            if (e.getMessage().contains(ClipDataCache.THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY)) {
                                throw e;
                            }
                        }
                        // issue Bug #82347
                        // at this point, the urls are freshly reloaded. There are several mysterious mediaurls (the do not contain any ei=
                        // parameter) that will return 403.
                        // if these exist, this code below would break the alternative searching.
                        // so if you ever experience this IP Mismatch issue, double check against this case.
                        // for now, I guess it's safe to remove these lines.
                        // if (con != null && con.getResponseCode() == 403) {
                        // // Probably IP Mismatch
                        // // do not check alternatives
                        // throw e;
                        // }
                        // age Protection. If age protection is active, all requests may return 403 without an youtube account
                        if (con != null && con.getResponseCode() == 403) {
                            YoutubeClipData clipData = ClipDataCache.hasCache(helper, downloadLink) ? ClipDataCache.get(helper, downloadLink) : null;
                            if (clipData != null && clipData.ageCheck) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                            }
                        }
                        LinkVariant alternative = getAlternatives(downloadLink, orgVariant, checkedAlternatives);
                        if (alternative != null) {
                            logger.info("Try next alternative variant: " + alternative);
                            downloadLink.getTempProperties().setProperty(YT_ALTERNATE_VARIANT, alternative);
                            continue test;
                        }
                        downloadLink.getTempProperties().removeProperty(YT_ALTERNATE_VARIANT);
                        throw e;
                    }
                }
                boolean hasCachedVideo = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO) != null;
                boolean hasCachedAudio = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO) != null;
                boolean hasCachedData = downloadLink.getProperty(YoutubeHelper.YT_STREAM_DATA_DATA) != null;
                if (!hasCachedVideo && !hasCachedAudio && !hasCachedData) {
                    YoutubeClipData clipData = ClipDataCache.hasCache(helper, downloadLink) ? ClipDataCache.get(helper, downloadLink) : null;
                    if (clipData != null && clipData.ageCheck) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    private LinkVariant getAlternatives(DownloadLink downloadLink, AbstractVariant variant, HashSet<LinkVariant> blacklisted) throws Exception {
        final YoutubeHelper helper;
        final YoutubeClipData clipData = ClipDataCache.get(helper = new YoutubeHelper(new Browser(), LoggerFactory.getDefaultLogger()), downloadLink);
        switch (variant.getGroup()) {
        case DESCRIPTION:
            return null;
        case SUBTITLES:
            List<VariantInfo> subtitles = clipData.findSubtitleVariants();
            if (subtitles != null) {
                for (VariantInfo v : subtitles) {
                    if (StringUtils.equals(v.getVariant()._getUniqueId(), variant._getUniqueId())) {
                        if (!blacklisted.contains(v.getVariant())) {
                            return v.getVariant();
                        }
                    }
                }
                Locale choosenLocale = ((SubtitleVariant) variant).getGenericInfo()._getLocale();
                for (VariantInfo v : subtitles) {
                    Locale vLocale = ((SubtitleVariant) v.getVariant()).getGenericInfo()._getLocale();
                    if (StringUtils.equals(vLocale.getLanguage(), choosenLocale.getLanguage())) {
                        if (!blacklisted.contains(v.getVariant())) {
                            return v.getVariant();
                        }
                    }
                }
            }
            break;
        case AUDIO:
        case IMAGE:
        case VIDEO:
            List<VariantInfo> variants = clipData.findVariants();
            helper.extendedDataLoading(variants);
            // sorts the best matching variants first. (based on quality rating)
            Collections.sort(variants, new Comparator<VariantInfo>() {
                @Override
                public int compare(VariantInfo o1, VariantInfo o2) {
                    return o2.compareTo(o1);
                }
            });
            for (VariantInfo v : variants) {
                if (StringUtils.equals(v.getVariant()._getUniqueId(), variant._getUniqueId())) {
                    if (!blacklisted.contains(v.getVariant())) {
                        return v.getVariant();
                    }
                }
            }
            VariantInfo vCur = null;
            VariantInfo vLast = null;
            for (int i = 0; i < variants.size(); i++) {
                vCur = variants.get(i);
                int comCur = variant.compareTo(vCur.getVariant());
                if (comCur == 0) {
                    if (!blacklisted.contains(vCur.getVariant())) {
                        return vCur.getVariant();
                    }
                } else if (comCur > 0) {
                    if (vLast != null) {
                        if (!blacklisted.contains(vLast.getVariant())) {
                            return vLast.getVariant();
                        }
                    } else {
                        if (!blacklisted.contains(vCur.getVariant())) {
                            return vCur.getVariant();
                        }
                    }
                }
                vLast = vCur;
            }
            for (VariantInfo v : variants) {
                if (StringUtils.equals(v.getVariant().getTypeId(), variant.getTypeId())) {
                    if (!blacklisted.contains(v.getVariant())) {
                        return v.getVariant();
                    }
                }
            }
            for (VariantInfo v : variants) {
                if (v.getVariant().getGroup() == variant.getGroup()) {
                    if (v.getVariant().getContainer() == variant.getContainer()) {
                        if (variant instanceof VideoVariant && v.getVariant() instanceof VideoVariant) {
                            if (((VideoVariant) v.getVariant()).getVideoCodec() == ((VideoVariant) variant).getVideoCodec()) {
                                if (((VideoVariant) v.getVariant()).getAudioCodec() == ((VideoVariant) variant).getAudioCodec()) {
                                    if (!blacklisted.contains(v.getVariant())) {
                                        return v.getVariant();
                                    }
                                }
                            }
                        } else if (variant instanceof AudioVariant && v.getVariant() instanceof AudioVariant) {
                            if (v.getVariant().getContainer() == variant.getContainer()) {
                                if (((AudioVariant) v.getVariant()).getAudioCodec() == ((AudioVariant) variant).getAudioCodec()) {
                                    if (!blacklisted.contains(v.getVariant())) {
                                        return v.getVariant();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (VariantInfo v : variants) {
                if (v.getVariant().getGroup() == variant.getGroup()) {
                    if (v.getVariant().getContainer() == variant.getContainer()) {
                        if (!blacklisted.contains(v.getVariant())) {
                            return v.getVariant();
                        }
                    }
                }
            }
        }
        return null;
    };

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
        final YoutubeFinalLinkResource video = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
        final YoutubeFinalLinkResource audio = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, YoutubeFinalLinkResource.TYPE_REF);
        final YoutubeFinalLinkResource data = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_DATA, YoutubeFinalLinkResource.TYPE_REF);
        if (video != null || audio != null || data != null) {
            // seems like we have cached final informations
            StreamCollection lstAudio = null;
            StreamCollection lstVideo = null;
            StreamCollection lstData = null;
            if (video != null) {
                lstVideo = new StreamCollection();
                lstVideo.add(video.toStreamDataObject());
            }
            if (audio != null) {
                lstAudio = new StreamCollection();
                lstAudio.add(audio.toStreamDataObject());
            }
            if (data != null) {
                lstData = new StreamCollection();
                lstData.add(data.toStreamDataObject());
            }
            final VariantInfo ret = new VariantInfo(getVariant(downloadLink), lstAudio, lstVideo, lstData);
            if (ret.isValid()) {
                return ret;
            }
        }
        return updateUrls(downloadLink);
    }

    protected AbstractVariant getVariant(final DownloadLink downloadLink) throws PluginException {
        final Object alternative = downloadLink.getTempProperties().getProperty(YT_ALTERNATE_VARIANT);
        if (alternative != null && alternative instanceof AbstractVariant) {
            return (AbstractVariant) alternative;
        }
        final AbstractVariant ret = AbstractVariant.get(downloadLink);
        if (ret == null) {
            getLogger().warning("Invalid Variant: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
            throw new PluginException(LinkStatus.ERROR_FATAL, "INVALID VARIANT: " + downloadLink.getStringProperty(YoutubeHelper.YT_VARIANT));
        }
        return ret;
    }

    private VariantInfo updateUrls(DownloadLink downloadLink) throws Exception {
        final AbstractVariant variant = getVariant(downloadLink);
        final YoutubeClipData clipData = ClipDataCache.get(new YoutubeHelper(br, getLogger()), downloadLink);
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
            for (YoutubeSubtitleStorable si : clipData.subtitles) {
                if (StringUtils.equals(stVariant.getGenericInfo().getLanguage(), si.getLanguage())) {
                    stVariant.setGenericInfo(si);
                    SubtitleVariantInfo vi = new SubtitleVariantInfo(stVariant, clipData);
                    // downloadLink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, vi);
                    return vi;
                }
            }
            return null;
        }
        final StreamCollection audioStreams = clipData.getStreams(variant.getBaseVariant().getiTagAudio());
        final StreamCollection videoStreams = clipData.getStreams(variant.getiTagVideo());
        final StreamCollection dataStreams = clipData.getStreams(variant.getiTagData());
        if (variant.getBaseVariant().getiTagAudio() != null && audioStreams == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid Audio Stream");
            // return null;
        }
        if (variant.getiTagVideo() != null && videoStreams == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid Video Stream");
            // return null;
        }
        if (variant.getiTagData() != null && dataStreams == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid Data Stream");
            // return null;
        }
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
            // if (streamData == null) {
            // requestFileInformation(downloadLink);
            // streamData = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
            // if (streamData == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // }
            dashName = getDashVideoFileName(downloadLink);
            dashChunksProperty = DASH_VIDEO_CHUNKS;
            chunkOffset = 0;
        } else {
            streamData = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, YoutubeFinalLinkResource.TYPE_REF);
            if (streamData == null) {
                requestFileInformation(downloadLink);
                streamData = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_AUDIO, YoutubeFinalLinkResource.TYPE_REF);
                if (streamData == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
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

            @Override
            public void setAvailableStatus(AvailableStatus availableStatus) {
                downloadLink.setAvailableStatus(availableStatus);
            }

            @Override
            public void setAvailable(boolean available) {
                downloadLink.setAvailable(available);
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
        final YoutubeConfig youtubeConfig = PluginJsonConfig.get(YoutubeConfig.class);
        final String[] segments = streamData.getSegments();
        final GetRequest request;
        if (youtubeConfig.isRateBypassEnabled() && segments == null) {
            request = new GetRequest(URLHelper.parseLocation(new URL(streamData.getBaseUrl()), "&ratebypass=yes&cmbypass=yes"));
        } else {
            request = new GetRequest(streamData.getBaseUrl());
        }
        if (segments != null) {
            dl = new SegmentDownloader(dashLink, dashDownloadable, br, new URL(request.getUrl()), segments);
            final boolean ret = dl.startDownload();
            if (dl.externalDownloadStop()) {
                return null;
            }
            return ret;
        }
        final List<HTTPProxy> possibleProxies = br.getProxy().getProxiesByURL(request.getURL());
        request.setProxy((possibleProxies == null || possibleProxies.size() == 0) ? null : possibleProxies.get(0));
        dl = BrowserAdapter.openDownload(br, dashDownloadable, request, true, getChunksPerStream(youtubeConfig));
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

    private int getChunksPerStream(YoutubeConfig cfg) {
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

    private FFmpegMetaData getFFmpegMetaData(final DownloadLink downloadLink) {
        if (downloadLink != null && PluginJsonConfig.get(YoutubeConfig.class).isMetaDataEnabled()) {
            final FFmpegMetaData ffMpegMetaData = new FFmpegMetaData();
            ffMpegMetaData.setTitle(downloadLink.getStringProperty(YoutubeHelper.YT_TITLE, null));
            ffMpegMetaData.setArtist(downloadLink.getStringProperty(YoutubeHelper.YT_CHANNEL_TITLE, null));
            final String contentURL = downloadLink.getContentUrl();
            if (contentURL != null) {
                ffMpegMetaData.setComment(contentURL.replaceFirst("(#variant=.+)", ""));
            }
            final long timestamp = downloadLink.getLongProperty(YoutubeHelper.YT_DATE, -1);
            if (timestamp > 0) {
                final GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(timestamp);
                ffMpegMetaData.setYear(calendar);
            }
            if (!ffMpegMetaData.isEmpty()) {
                return ffMpegMetaData;
            }
        }
        return null;
    }

    @Override
    public FFmpeg getFFmpeg(final DownloadLink downloadLink) {
        final FFmpegMetaData ffMpegMetaData = getFFmpegMetaData(downloadLink);
        if (ffMpegMetaData != null && !ffMpegMetaData.isEmpty()) {
            return new FFmpeg() {
                private final UniqueAlltimeID metaDataProcessID = new UniqueAlltimeID();
                private HttpServer            httpServer        = null;
                private File                  metaFile          = null;

                private final boolean isWriteFileEnabled() {
                    return true;
                }

                @Override
                protected void parseLine(boolean stdStream, StringBuilder ret, String line) {
                    if (line != null && StringUtils.contains(line, "Input/output error") && StringUtils.contains(line, "/meta")) {
                        PluginJsonConfig.get(YoutubeConfig.class).setMetaDataEnabled(false);
                        if (logger != null) {
                            logger.severe("Firewall/AV blocks JDownloader<->ffmpeg meta data communication. Auto disable meta data support!");
                        }
                    }
                }

                private final HttpServer startHttpServer() {
                    try {
                        final HttpServer httpServer = new HttpServer(0);
                        httpServer.setLocalhostOnly(true);
                        httpServer.registerRequestHandler(new HttpRequestHandler() {
                            @Override
                            public boolean onPostRequest(PostRequest request, HttpResponse response) throws BasicRemoteAPIException {
                                return false;
                            }

                            @Override
                            public boolean onGetRequest(org.appwork.utils.net.httpserver.requests.GetRequest request, HttpResponse response) throws BasicRemoteAPIException {
                                try {
                                    final String id = request.getParameterbyKey("id");
                                    if (id != null && metaDataProcessID.getID() == Long.parseLong(request.getParameterbyKey("id")) && "/meta".equals(request.getRequestedPath())) {
                                        if (logger != null) {
                                            logger.info("Providing ffmpeg meta data");
                                        }
                                        final String content = ffMpegMetaData.getFFmpegMetaData();
                                        final byte[] bytes = content.getBytes("UTF-8");
                                        response.setResponseCode(HTTPConstants.ResponseCode.get(200));
                                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/plain; charset=utf-8"));
                                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                                        final OutputStream out = response.getOutputStream(true);
                                        out.write(bytes);
                                        out.flush();
                                        return true;
                                    }
                                } catch (final IOException e) {
                                    if (logger != null) {
                                        logger.log(e);
                                    }
                                }
                                return false;
                            }
                        });
                        httpServer.start();
                        if (logger != null) {
                            logger.info("Opened http server to serve meta on port " + httpServer.getPort());
                        }
                        return httpServer;
                    } catch (final IOException e) {
                        if (logger != null) {
                            logger.log(e);
                        }
                    }
                    return null;
                }

                private File writeMetaFile() {
                    final File ret = Application.getTempResource("ffmpeg_meta_" + UniqueAlltimeID.create());
                    try {
                        IO.writeStringToFile(ret, ffMpegMetaData.getFFmpegMetaData());
                        if (logger != null) {
                            logger.info("Wrote meta to " + ret);
                        }
                        return ret;
                    } catch (final Throwable e) {
                        ret.delete();
                        if (logger != null) {
                            logger.log(e);
                        }
                    }
                    return null;
                }

                private void stopMetaFileProvider() {
                    final File metaFile = this.metaFile;
                    if (metaFile != null) {
                        this.metaFile = null;
                        metaFile.delete();
                    }
                    final HttpServer httpServer = this.httpServer;
                    if (httpServer != null) {
                        this.httpServer = null;
                        httpServer.stop();
                    }
                }

                @Override
                public boolean muxToMp4(FFMpegProgress progress, String out, String videoIn, String audioIn) throws InterruptedException, IOException, FFMpegException {
                    if (isWriteFileEnabled()) {
                        metaFile = writeMetaFile();
                    } else {
                        httpServer = startHttpServer();
                    }
                    try {
                        return super.muxToMp4(progress, out, videoIn, audioIn);
                    } finally {
                        stopMetaFileProvider();
                    }
                }

                @Override
                public boolean generateM4a(FFMpegProgress progress, String out, String audioIn) throws IOException, InterruptedException, FFMpegException {
                    if (isWriteFileEnabled()) {
                        metaFile = writeMetaFile();
                    } else {
                        httpServer = startHttpServer();
                    }
                    try {
                        return super.generateM4a(progress, out, audioIn);
                    } finally {
                        stopMetaFileProvider();
                    }
                }

                @Override
                public boolean demuxM4a(FFMpegProgress progress, String out, String audioIn) throws InterruptedException, IOException, FFMpegException {
                    if (isWriteFileEnabled()) {
                        metaFile = writeMetaFile();
                    } else {
                        httpServer = startHttpServer();
                    }
                    try {
                        return super.demuxM4a(progress, out, audioIn);
                    } finally {
                        stopMetaFileProvider();
                    }
                }

                @Override
                protected boolean demux(FFMpegProgress progress, String out, String audioIn, String[] demuxCommands) throws InterruptedException, IOException, FFMpegException {
                    if (httpServer != null || metaFile != null) {
                        final ArrayList<String> newDemuxCommands = new ArrayList<String>();
                        boolean metaParamsAdded = false;
                        String lastDemuxCommand = null;
                        for (final String demuxCommand : demuxCommands) {
                            if ("%audio".equals(lastDemuxCommand) && !metaParamsAdded) {
                                newDemuxCommands.add("-i");
                                if (httpServer != null) {
                                    newDemuxCommands.add("http://127.0.0.1:" + httpServer.getPort() + "/meta?id=" + metaDataProcessID.getID());
                                } else {
                                    newDemuxCommands.add(metaFile.getAbsolutePath());
                                }
                                newDemuxCommands.add("-map_metadata");
                                newDemuxCommands.add("1");
                                metaParamsAdded = true;
                            }
                            newDemuxCommands.add(demuxCommand);
                            lastDemuxCommand = demuxCommand;
                        }
                        if ("%audio".equals(lastDemuxCommand) && !metaParamsAdded) {
                            newDemuxCommands.add("-i");
                            if (httpServer != null) {
                                newDemuxCommands.add("http://127.0.0.1:" + httpServer.getPort() + "/meta?id=" + metaDataProcessID.getID());
                            } else {
                                newDemuxCommands.add(metaFile.getAbsolutePath());
                            }
                            newDemuxCommands.add("-map_metadata");
                            newDemuxCommands.add("1");
                            metaParamsAdded = true;
                        }
                        return super.demux(progress, out, audioIn, newDemuxCommands.toArray(new String[0]));
                    } else {
                        return super.demux(progress, out, audioIn, demuxCommands);
                    }
                }

                @Override
                protected boolean mux(FFMpegProgress progress, String out, String videoIn, String audioIn, String[] muxCommands) throws InterruptedException, IOException, FFMpegException {
                    if (httpServer != null || metaFile != null) {
                        final ArrayList<String> newMuxCommands = new ArrayList<String>();
                        boolean metaParamsAdded = false;
                        for (final String muxCommand : muxCommands) {
                            if ("-map".equals(muxCommand) && !metaParamsAdded) {
                                newMuxCommands.add("-i");
                                if (httpServer != null) {
                                    newMuxCommands.add("http://127.0.0.1:" + httpServer.getPort() + "/meta?id=" + metaDataProcessID.getID());
                                } else {
                                    newMuxCommands.add(metaFile.getAbsolutePath());
                                }
                                newMuxCommands.add("-map_metadata");
                                newMuxCommands.add("2");
                                metaParamsAdded = true;
                            }
                            newMuxCommands.add(muxCommand);
                        }
                        return super.mux(progress, out, videoIn, audioIn, newMuxCommands.toArray(new String[0]));
                    } else {
                        return super.mux(progress, out, videoIn, audioIn, muxCommands);
                    }
                }
            };
        } else {
            return new FFmpeg();
        }
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
            // debug
            requestFileInformation(downloadLink);
            final SingleDownloadController dlc = downloadLink.getDownloadLinkController();
            final List<File> locks = new ArrayList<File>();
            locks.addAll(listProcessFiles(downloadLink));
            HttpServer httpServer = null;
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
                final AbstractVariant variant = getVariant(downloadLink);
                boolean loadVideo = !data.isDashVideoFinished();
                if (videoStreamPath == null || variant.getType() == DownloadType.DASH_AUDIO) {
                    /* Skip video if just audio should be downloaded */
                    loadVideo = false;
                } else {
                    loadVideo |= !(new File(videoStreamPath).exists() && new File(videoStreamPath).length() > 0);
                }
                if (loadVideo) {
                    /* videoStream not finished yet, resume/download it */
                    final Boolean ret = downloadDashStream(downloadLink, data, true);
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
                    final Boolean ret = downloadDashStream(downloadLink, data, false);
                    if (ret == null) {
                        return;
                    }
                    if (!ret) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (new File(audioStreamPath).exists() && !new File(downloadLink.getFileOutput()).exists()) {
                    downloadLink.setAvailable(true);
                    final FFmpeg ffmpeg = getFFmpeg(downloadLink);
                    /* audioStream also finished */
                    /* Do we need an exception here? If a Video is downloaded it is always finished before the audio part. TheCrap */
                    if (videoStreamPath != null && new File(videoStreamPath).exists()) {
                        final FFMpegProgress progress = new FFMpegProgress();
                        progress.setProgressSource(this);
                        try {
                            downloadLink.addPluginProgress(progress);
                            final VideoCodec codec = variant.getiTagVideo().getVideoCodec();
                            switch (codec) {
                            case VP8:
                            case VP9:
                            case VP9_HDR:
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
                if (httpServer != null) {
                    httpServer.stop();
                }
                for (final File lock : locks) {
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
    public void handlePremium(final DownloadLink downloadLink, Account account) throws Exception {
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
            if (sd == null) {
                requestFileInformation(downloadLink);
                sd = downloadLink.getObjectProperty(YoutubeHelper.YT_STREAM_DATA_VIDEO, YoutubeFinalLinkResource.TYPE_REF);
                if (sd == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, sd.getBaseUrl(), resume, getChunksPerStream(PluginJsonConfig.get(YoutubeConfig.class)));
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
            dl = new HLSDownloader(downloadLink, br, getAndUpdateVariantInfo(downloadLink).getVideoStreams().get(0).getUrl()) {
                @Override
                protected boolean isMapMetaDataEnabled() {
                    return PluginJsonConfig.get(YoutubeConfig.class).isMetaDataEnabled();
                }

                @Override
                protected FFmpegMetaData getFFmpegMetaData() {
                    return YoutubeDashV2.this.getFFmpegMetaData(downloadLink);
                }
            };
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
                                    String ext = Files.getExtension(child.getFinalFileName());
                                    if (StringUtils.isNotEmpty(ext)) {
                                        String base = child.getFinalFileName().substring(0, child.getFinalFileName().length() - ext.length() - 1);
                                        final String displayLanguage;
                                        if (variant instanceof SubtitleVariant) {
                                            displayLanguage = ((SubtitleVariant) variant).getDisplayLanguage();
                                        } else {
                                            final Locale locale = new SubtitleVariantOld(downloadLink.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, ""))._getLocale();
                                            displayLanguage = locale.getDisplayLanguage();
                                        }
                                        final File newFile;
                                        if (StringUtils.isEmpty(displayLanguage)) {
                                            newFile = new File(finalFile.getParentFile(), base + ".srt");
                                        } else {
                                            newFile = new File(finalFile.getParentFile(), base + "." + displayLanguage + ".srt");
                                        }
                                        IO.copyFile(finalFile, newFile);
                                        try {
                                            if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified()) {
                                                newFile.setLastModified(finalFile.lastModified());
                                            }
                                        } catch (final Throwable e) {
                                            getLogger().log(e);
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
        final YoutubeProperties data = downloadLink.bindData(YoutubeProperties.class);
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
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var.getBaseVariant().name() + "_" + Hash.getMD5(var._getUniqueId()) + ".dashAudio";
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
        return link.getStringProperty(YoutubeHelper.YT_ID, null) + "_" + var.getBaseVariant().name() + "_" + Hash.getMD5(var._getUniqueId()) + ".dashVideo";
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
        downloadLink.setContentUrl("https://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "#variant=" + Encoding.urlEncode(Base64.encode(((AbstractVariant) variant).getStorableString())));
        if (variant instanceof AbstractVariant) {
            AbstractVariant v = (AbstractVariant) variant;
            // reset Streams urls. we need new ones
            resetStreamUrls(downloadLink);
            downloadLink.setDownloadSize(-1);
            downloadLink.setVerifiedFileSize(-1);
            if (downloadLink.hasProperty(LinkCollector.SOURCE_VARIANT_ID)) {
                downloadLink.removeProperty(YoutubeHelper.YT_COLLECTION);
            }
            List<LinkVariant> variants = getVariantsByLink(downloadLink);
            boolean has = false;
            for (LinkVariant vv : variants) {
                if (StringUtils.equals(vv._getUniqueId(), variant._getUniqueId())) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                // extend variants list
                List<String> altIds = new ArrayList<String>();
                altIds.add(((AbstractVariant) variant).getStorableString());
                String[] variantIds = getVariantsIDList(downloadLink);
                for (String s : variantIds) {
                    altIds.add(s);
                }
                downloadLink.setProperty(YoutubeHelper.YT_VARIANTS, altIds);
                downloadLink.getTempProperties().removeProperty(YoutubeHelper.YT_VARIANTS);
            }
            YoutubeHelper.writeVariantToDownloadLink(downloadLink, v);
            String filename;
            downloadLink.setFinalFileName(filename = new YoutubeHelper(br, getLogger()).createFilename(downloadLink));
            downloadLink.setPluginPatternMatcher(YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), (AbstractVariant) variant, Arrays.asList(getVariantsIDList(downloadLink))));
            downloadLink.setContentUrl("https://www.youtube.com" + "/watch?v=" + downloadLink.getStringProperty(YoutubeHelper.YT_ID) + "#variant=" + Encoding.urlEncode(Base64.encode(v.getStorableString())));
            downloadLink.setLinkID(YoutubeHelper.createLinkID(downloadLink.getStringProperty(YoutubeHelper.YT_ID), (AbstractVariant) variant, Arrays.asList(getVariantsIDList(downloadLink))));
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
                        UrlQuery info;
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
    public JComponent getVariantPopupComponent(DownloadLink downloadLink) {
        return super.getVariantPopupComponent(downloadLink);
    }

    @Override
    public boolean fillVariantsPopup(final VariantColumn variantColumn, final JPopupMenu popup, final AbstractNode value, final LinkVariant selected, final ComboBoxModel<LinkVariant> dm) {
        final CrawledLink link = (CrawledLink) value;
        VariantGroup group = ((AbstractVariant) selected).getGroup();
        switch (group) {
        case AUDIO:
        case IMAGE:
        case VIDEO:
        case SUBTITLES:
            popup.add(new JMenuItem(new BasicAction() {
                {
                    setSmallIcon(new AbstractIcon(IconKey.ICON_REFRESH, 18));
                    setName(_GUI.T.youtube_choose_variant());
                }

                @Override
                public void actionPerformed(final ActionEvent e) {
                    showChangeOrAddVariantDialog(link, (AbstractVariant) selected);
                }
            }));
            popup.add(new JMenuItem(new BasicAction() {
                {
                    setSmallIcon(new AbstractIcon(IconKey.ICON_ADD, 18));
                    setName(_GUI.T.youtube_add_variant());
                }

                @Override
                public void actionPerformed(final ActionEvent e) {
                    showChangeOrAddVariantDialog(link, null);
                }
            }));
        }
        variantColumn.fillPopupWithPluginSettingsButton(popup, link);
        popup.add(new JSeparator());
        variantColumn.fillPopupWithVariants(popup, value, selected, dm);
        return true;
    }

    @Override
    public void extendLinkgrabberContextMenu(final JComponent parent, final PluginView<CrawledLink> pv, Collection<PluginView<CrawledLink>> allPvs) {
        new YoutubeLinkGrabberExtender(this, parent, pv, allPvs).run();
    }

    @Override
    public boolean onLinkCollectorDupe(CrawledLink existingLink, CrawledLink newLink) {
        // merge Variants
        DownloadLink existingDlink = existingLink.getDownloadLink();
        DownloadLink newDLink = newLink.getDownloadLink();
        List<LinkVariant> variantsExisting = getVariantsByLink(existingDlink);
        List<LinkVariant> variantsNewLink = getVariantsByLink(newDLink);
        // clear cache
        ArrayList<LinkVariant> ret = new ArrayList<LinkVariant>();
        HashSet<String> dupe = new HashSet<String>();
        if (variantsExisting != null) {
            for (LinkVariant v : variantsExisting) {
                if (dupe.add(((AbstractVariant) v).getTypeId())) {
                    ret.add(v);
                }
            }
        }
        if (variantsNewLink != null) {
            for (LinkVariant v : variantsNewLink) {
                if (dupe.add(((AbstractVariant) v).getTypeId())) {
                    ret.add(v);
                }
            }
        }
        Collections.sort(ret, new Comparator<LinkVariant>() {
            @Override
            public int compare(LinkVariant o1, LinkVariant o2) {
                AbstractVariant a1 = (AbstractVariant) o1;
                AbstractVariant a2 = (AbstractVariant) o2;
                return a2.compareTo(a1);
            }
        });
        ArrayList<String> newIDList = new ArrayList<String>();
        for (LinkVariant vi : ret) {
            newIDList.add(((AbstractVariant) vi).getStorableString());
        }
        if (newIDList.size() != variantsExisting.size()) {
            existingDlink.getTempProperties().removeProperty(YoutubeHelper.YT_VARIANTS);
            existingDlink.setProperty(YoutubeHelper.YT_VARIANTS, newIDList);
            existingDlink.setVariantSupport(newIDList.size() > 1);
            // setActiveVariantByLink(existingDlink, ret.get(0));
        }
        return false;
    }

    public boolean onLinkCrawlerDupeFilterEnabled(CrawledLink existingLink, CrawledLink newLink) {
        return false;
    }

    @Override
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    protected void setConfigElements() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void showChangeOrAddVariantDialog(final CrawledLink link, final AbstractVariant s) {
        ProgressGetter pg = new ProgressGetter() {
            @Override
            public void run() throws Exception {
                final YoutubeHelper helper;
                final YoutubeClipData clipData = ClipDataCache.get(helper = new YoutubeHelper(new Browser(), getLogger()), link.getDownloadLink());
                final ArrayList<VariantInfo> vs = new ArrayList<VariantInfo>();
                vs.addAll(clipData.findVariants());
                vs.addAll(clipData.findDescriptionVariant());
                vs.addAll(clipData.findSubtitleVariants());
                helper.extendedDataLoading(vs);
                new Thread("Choose Youtube Variant") {
                    public void run() {
                        YoutubeVariantSelectionDialog d;
                        try {
                            UIOManager.I().show(null, d = new YoutubeVariantSelectionDialog(link, s, clipData, vs)).throwCloseExceptions();
                            if (s == null) {
                                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);
                                List<LinkVariant> variants = d.getVariants();
                                for (LinkVariant v : variants) {
                                    CrawledLink newLink = LinkCollector.getInstance().addAdditional(link, v);
                                    if (newLink != null) {
                                        checkableLinks.add(newLink);
                                    }
                                }
                                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                linkChecker.check(checkableLinks);
                            } else {
                                LinkVariant variant = d.getVariant();
                                if (variant != null) {
                                    LinkCollector.getInstance().setActiveVariantForLink(link, variant);
                                }
                            }
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                    };
                }.start();
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };
        ProgressDialog dialog = new ProgressDialog(pg, 0, _GUI.T.lit_please_wait(), _GUI.T.youtube_scan_variants(), new AbstractIcon(IconKey.ICON_WAIT, 32));
        UIOManager.I().show(null, dialog);
    }
}
