//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.awt.Dialog.ModalityType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.components.youtube.ClipDataCache;
import org.jdownloader.plugins.components.youtube.Projection;
import org.jdownloader.plugins.components.youtube.StreamCollection;
import org.jdownloader.plugins.components.youtube.VariantIDStorable;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.IfUrlisAVideoAndPlaylistAction;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.PlaylistAndChannelCrawlerAddOrder;
import org.jdownloader.plugins.components.youtube.YoutubeConfig.ProfileCrawlMode;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioInterface;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.ImageVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNodeVisitor;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;
import jd.plugins.hoster.YoutubeDashV2;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TbCmV2 extends PluginForDecrypt {
    private static final int DDOS_WAIT_MAX        = Application.isJared(null) ? 1000 : 10;
    private static final int DDOS_INCREASE_FACTOR = 15;

    public TbCmV2(PluginWrapper wrapper) {
        super(wrapper);
    };

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "youtube.com", "music.youtube.com", "youtube-nocookie.com", "yt.not.allowed", "youtube.googleapis.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    // TODO: Remove the following commented out lines
    // old patterns: "https?://(?:www\\.)?youtube-nocookie\\.com/embed/.+", "https?://([a-z]+\\.)?yt\\.not\\.allowed/.+",
    // "https?://([a-z]+\\.)?youtube\\.com/(embed/|.*?watch.*?v(%3D|=)|shorts/|view_play_list\\?p=|playlist\\?(p|list)=|.*?g/c/|.*?grid/user/|v/|user/|channel/|c/|course\\?list=)[%A-Za-z0-9\\-_]+(.*?index=\\d+)?(.*?page=\\d+)?(.*?list=[%A-Za-z0-9\\-_]+)?(\\#variant=\\S++)?|watch_videos\\?.*?video_ids=.+",
    // "https?://youtube\\.googleapis\\.com/(v/|user/|channel/|c/)[%A-Za-z0-9\\-_]+(\\#variant=\\S+)?",
    // "https?://([a-z]+\\.)?youtube\\.com/@[^/]+"
    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String pattern = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/";
            pattern += "(";
            pattern += "embed\\?v=" + VIDEO_ID_PATTERN + ".*";
            pattern += "|watch\\?v=" + VIDEO_ID_PATTERN + ".*";
            pattern += "|shorts/" + VIDEO_ID_PATTERN + ".*";
            pattern += "|(?:view_play_list|playlist)\\?(p|list)=.+";
            pattern += "|watch_videos\\?.+";
            pattern += "|video_ids=.+";
            pattern += "|channel/.+";
            pattern += "|c/.+";
            pattern += "|user/.+";
            pattern += "|@.+";
            pattern += ")";
            /* Let's allow variant information to be present at the end of any item. */
            pattern += "(\\#variant=\\S+)?";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /**
     * Returns host from provided String.
     */
    public static String getBaseURL() {
        return "https://www.youtube.com";
    }

    /**
     * Returns a playlistID from provided url.
     */
    private String getListIDFromUrl(final String url) {
        try {
            final UrlQuery query = UrlQuery.parse(url);
            String playlistID = query.get("list");
            if (playlistID == null) {
                /* Older URLs */
                playlistID = query.get("p");
            }
            return playlistID;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final String VIDEO_ID_PATTERN = "([A-Za-z0-9\\-_]{11})";

    private String getVideoIDFromUrl(final String url) {
        String vuid = new Regex(url, "(?i)v=" + VIDEO_ID_PATTERN).getMatch(0);
        if (vuid == null) {
            vuid = new Regex(url, "(?i)v/" + VIDEO_ID_PATTERN).getMatch(0);
            if (vuid == null) {
                vuid = new Regex(url, "(?i)shorts/" + VIDEO_ID_PATTERN).getMatch(0);
                if (vuid == null) {
                    vuid = new Regex(url, "(?i)embed/(?!videoseries\\?)" + VIDEO_ID_PATTERN).getMatch(0);
                }
            }
        }
        return vuid;
    }

    private String getChannelIDFromUrl(final String url) {
        return new Regex(url, "/channel/([^/\\?]+)").getMatch(0);
    }

    private String getUsernameFromUrl(final String url) {
        String userName = new Regex(url, "(?i)/user/([^/\\?]+)").getMatch(0);
        if (userName == null) {
            userName = new Regex(url, "(?i)https?://[^/]+/@([^/\\?]+)").getMatch(0);
        }
        return userName;
    }

    /**
     * Returns true if given URL leads to a channel or profile "Shorts videos" -> Equivalent would be the "Shorts" tab in channel overview
     * in browser.
     */
    private static boolean isChannelOrProfileShorts(final String url) {
        return StringUtils.endsWithCaseInsensitive(url, "/shorts");
    }

    private boolean linkCollectorContainsEntryByID(final String videoID) {
        final AtomicBoolean containsFlag = new AtomicBoolean(false);
        LinkCollector.getInstance().visitNodes(new AbstractNodeVisitor<CrawledLink, CrawledPackage>() {
            @Override
            public Boolean visitPackageNode(CrawledPackage pkg) {
                if (containsFlag.get()) {
                    return null;
                } else {
                    return Boolean.TRUE;
                }
            }

            @Override
            public Boolean visitChildrenNode(CrawledLink node) {
                if (containsFlag.get()) {
                    return null;
                } else {
                    if (StringUtils.equalsIgnoreCase(getHost(), node.getHost())) {
                        final DownloadLink downloadLink = node.getDownloadLink();
                        if (downloadLink != null && StringUtils.equals(videoID, downloadLink.getStringProperty(YoutubeHelper.YT_ID))) {
                            containsFlag.set(true);
                            return null;
                        }
                    }
                    return Boolean.TRUE;
                }
            }
        }, true);
        return containsFlag.get();
    }

    private YoutubeConfig           cfg;
    private static Object           DIALOGLOCK = new Object();
    private String                  videoID;
    private String                  playlistID;
    private String                  channelID;
    private String                  userName;
    private AbstractVariant         requestedVariant;
    private HashMap<String, Object> globalPropertiesForDownloadLink;
    private YoutubeHelper           helper;

    @Override
    protected DownloadLink createOfflinelink(String link, String filename, String message) {
        final DownloadLink ret = super.createOfflinelink(link, filename, message);
        logger.log(new Exception("Debug:" + filename + "|" + message));
        return ret;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        // nullify, for debugging purposes!
        videoID = null;
        playlistID = null;
        channelID = null;
        userName = null;
        globalPropertiesForDownloadLink = new HashMap<String, Object>();
        cfg = PluginJsonConfig.get(YoutubeConfig.class);
        String addedLink = param.getCryptedUrl();
        if (StringUtils.containsIgnoreCase(addedLink, "youtube-nocookie.com")) {
            // TODO: Solve this in a more elegant way
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            addedLink = addedLink.replaceFirst("https?://(?:www\\.)?youtube-nocookie\\.com/embed/", "https://youtube.com/watch?v=");
            final DownloadLink link = createDownloadlink(addedLink);
            link.setContainerUrl(addedLink);
            ret.add(link);
            return ret;
        } else if (StringUtils.containsIgnoreCase(addedLink, "yt.not.allowed")) {
            // TODO: Check what this is/was there for
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (cfg.isAndroidSupportEnabled()) {
                if (addedLink.matches("https?://[\\w\\.]*yt\\.not\\.allowed/[%a-z_A-Z0-9\\-]+")) {
                    addedLink = addedLink.replaceFirst("yt\\.not\\.allowed", "youtu.be");
                } else {
                    addedLink = addedLink.replaceFirst("yt\\.not\\.allowed", "youtube.com");
                }
                final DownloadLink link = createDownloadlink(addedLink);
                link.setContainerUrl(addedLink);
                ret.add(link);
            }
            return ret;
        }
        final String finalContainerURL = addedLink;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                e.setContainerUrl(finalContainerURL);
                distribute(e);
                return super.add(e);
            }
        };
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "PREF", "hl=en-GB");
        String cleanedurl = Encoding.urlDecode(addedLink, true);
        final String requestedVariantString = new Regex(cleanedurl, "(?i)\\#variant=(\\S*)").getMatch(0);
        if (StringUtils.isNotEmpty(requestedVariantString)) {
            requestedVariant = AbstractVariant.get(Base64.decodeToString(requestedVariantString));
            cleanedurl = cleanedurl.replaceAll("\\#variant=\\S+", "");
        }
        videoID = getVideoIDFromUrl(cleanedurl);
        // for watch_videos, found within youtube.com music
        final String video_ids_comma_separated = new Regex(cleanedurl, "video_ids=([a-zA-Z0-9\\-_,]+)").getMatch(0);
        if (video_ids_comma_separated != null) {
            // first uid in array is the video the user copy url on.
            videoID = new Regex(video_ids_comma_separated, "(" + VIDEO_ID_PATTERN + ")").getMatch(0);
        }
        helper = new YoutubeHelper(br, getLogger());
        if (helper.isConsentCookieRequired()) {
            helper.setConsentCookie(br, null);
        }
        helper.login(getLogger(), false);
        playlistID = getListIDFromUrl(cleanedurl);
        userName = getUsernameFromUrl(cleanedurl);
        channelID = getChannelIDFromUrl(cleanedurl);
        final String userChannel = new Regex(cleanedurl, "/c/([^/\\?]+)").getMatch(0);
        if (StringUtils.isEmpty(channelID) && StringUtils.isNotEmpty(userChannel)) {
            // TODO: Check if this is still needed
            logger.info("Trying to find channelID");
            helper.getPage(br, getBaseURL() + "/c/" + userChannel);
            channelID = br.getRegex("(?i)/channel/(UC[A-Za-z0-9\\-_]+)/videos").getMatch(0);
            if (StringUtils.isEmpty(channelID)) {
                // its within meta tags multiple times (ios/ipad/iphone) also
                helper.parse();
                channelID = getChannelID(helper, br);
            }
            if (StringUtils.isEmpty(channelID)) {
                logger.warning("Failed to find channelID");
            }
        }
        if (StringUtils.isEmpty(channelID) && StringUtils.isEmpty(userName) && StringUtils.isEmpty(playlistID) && StringUtils.isEmpty(videoID)) {
            /* This should be a rare case but it can happen since we are supporting a lot of different URL formats. */
            logger.info("Unsupported URL");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_ID, playlistID);
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_CHANNEL_ID, channelID);
        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_USER_ID, userName);
        /* @Developer: Enable this boolean if pagination is broken and you are unable to quickly fix it. */
        final boolean paginationIsBroken = false;
        final boolean isChannelOrProfileShorts = isChannelOrProfileShorts(cleanedurl);
        final short maxItemsPerPage = 100;
        final ArrayList<YoutubeClipData> videoIdsToAdd = new ArrayList<YoutubeClipData>();
        int userDefinedMaxPlaylistOrProfileItemsLimit = cfg.getPlaylistAndProfileCrawlerMaxItemsLimit();
        final String logtextForUserDisabledCrawlerByLimitSetting = "Doing nothing because user has disabled channel/profile/playlist crawler by setting limit to 0";
        if (StringUtils.isEmpty(playlistID) && StringUtils.isEmpty(userName) && StringUtils.isEmpty(userChannel) && !StringUtils.isEmpty(videoID)) {
            /* Single video */
            videoIdsToAdd.add(new org.jdownloader.plugins.components.youtube.YoutubeClipData(videoID));
        } else {
            /* Channel/Playlist/User or single Video + playlist in one URL. */
            synchronized (DIALOGLOCK) {
                if (this.isAbort()) {
                    logger.info("Thread Aborted!");
                    return ret;
                }
                /* Ask user: Prevents accidental crawling of entire Play-List or Channel-List or User-List. */
                IfUrlisAPlaylistAction playListAction = cfg.getLinkIsPlaylistUrlAction();
                final String humanReadableTypeOfUrlToCrawl;
                final String dialogTitle;
                if (playlistID != null) {
                    humanReadableTypeOfUrlToCrawl = "Playlist";
                    dialogTitle = "Playlist | " + playlistID;
                } else if (userName != null && isChannelOrProfileShorts) {
                    humanReadableTypeOfUrlToCrawl = "Channel Shorts";
                    dialogTitle = "Channel | " + userName + " | Shorts";
                } else if (userName != null) {
                    humanReadableTypeOfUrlToCrawl = "Channel";
                    dialogTitle = "Channel via username | " + userName;
                } else {
                    humanReadableTypeOfUrlToCrawl = "Channel";
                    dialogTitle = "Channel via channelID | " + channelID;
                }
                final String buttonTextCrawlPlaylistOrProfile;
                if (paginationIsBroken) {
                    buttonTextCrawlPlaylistOrProfile = humanReadableTypeOfUrlToCrawl + " [max first " + maxItemsPerPage + " items]";
                } else if (userDefinedMaxPlaylistOrProfileItemsLimit > 0) {
                    buttonTextCrawlPlaylistOrProfile = humanReadableTypeOfUrlToCrawl + " [max first " + userDefinedMaxPlaylistOrProfileItemsLimit + " item(s)]";
                } else {
                    buttonTextCrawlPlaylistOrProfile = humanReadableTypeOfUrlToCrawl;
                }
                if ((StringUtils.isNotEmpty(playlistID) || StringUtils.isNotEmpty(channelID) || StringUtils.isNotEmpty(userName)) && StringUtils.isEmpty(videoID)) {
                    if (userDefinedMaxPlaylistOrProfileItemsLimit == 0) {
                        logger.info(logtextForUserDisabledCrawlerByLimitSetting);
                        return ret;
                    }
                    if (playListAction == IfUrlisAPlaylistAction.ASK) {
                        String messageDialogText = "This URL is a " + humanReadableTypeOfUrlToCrawl + " link. What would you like to do?";
                        if (paginationIsBroken) {
                            messageDialogText += "\r\nJDownloader can only crawl the first " + maxItemsPerPage + " items automatically.\r\nIf there are more than " + maxItemsPerPage + " items, you need to use external tools to grab the single URLs to all videos and add those to JD manually.";
                        }
                        messageDialogText += "\r\nIf you wish to hide this dialog, you can pre-select your preferred option under Settings -> Plugins -> youtube.com.";
                        final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, dialogTitle, JDL.L("plugins.host.youtube.isplaylist.question.message", messageDialogText), null, JDL.L("plugins.host.youtube.isplaylist.question.onlyplaylist", buttonTextCrawlPlaylistOrProfile), JDL.L("plugins.host.youtube.isvideoandplaylist.question.nothing", "Do nothing?")) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }

                            @Override
                            public boolean isRemoteAPIEnabled() {
                                return true;
                            }
                        };
                        try {
                            UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                            playListAction = IfUrlisAPlaylistAction.PROCESS;
                        } catch (final DialogCanceledException e) {
                            logger.log(e);
                            playListAction = IfUrlisAPlaylistAction.NOTHING;
                        } catch (final DialogClosedException e) {
                            logger.log(e);
                            playListAction = IfUrlisAPlaylistAction.NOTHING;
                        }
                    }
                    logger.info("LinkIsPlaylistUrlAction:" + playListAction);
                    switch (playListAction) {
                    case PROCESS:
                        break;
                    case NOTHING:
                    default:
                        return ret;
                    }
                } else {
                    /* Check if link contains a video and a playlist */
                    IfUrlisAVideoAndPlaylistAction PlaylistVideoAction = cfg.getLinkIsVideoAndPlaylistUrlAction();
                    if ((StringUtils.isNotEmpty(playlistID) || StringUtils.isNotEmpty(video_ids_comma_separated)) && StringUtils.isNotEmpty(videoID)) {
                        if (PlaylistVideoAction == IfUrlisAVideoAndPlaylistAction.ASK) {
                            /* Ask user */
                            final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, "Crawl video " + this.videoID + " or playlist " + this.playlistID, "This YouTube link contains a video and a playlist. What do you want do download?", null, "Only video", buttonTextCrawlPlaylistOrProfile) {
                                @Override
                                public ModalityType getModalityType() {
                                    return ModalityType.MODELESS;
                                }

                                @Override
                                public boolean isRemoteAPIEnabled() {
                                    return true;
                                }
                            };
                            try {
                                UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                                PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.VIDEO_ONLY;
                            } catch (final DialogCanceledException e) {
                                logger.log(e);
                                PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.PLAYLIST_ONLY;
                            } catch (final DialogClosedException e) {
                                logger.log(e);
                                PlaylistVideoAction = IfUrlisAVideoAndPlaylistAction.NOTHING;
                            }
                        }
                        logger.info("LinkIsVideoAndPlaylistUrlAction:" + PlaylistVideoAction);
                        switch (PlaylistVideoAction) {
                        case PLAYLIST_ONLY:
                            break;
                        case VIDEO_ONLY:
                            videoIdsToAdd.add(new org.jdownloader.plugins.components.youtube.YoutubeClipData(videoID));
                            break;
                        default:
                            logger.info("Doing nothing");
                            return ret;
                        }
                    }
                    if (userDefinedMaxPlaylistOrProfileItemsLimit == 0) {
                        /*
                         * Small workaround: User wants us to crawl playlist but set this limit to 0 -> It would be kind of not logical to
                         * ask him first and then do nothing so let's remove that limit in this case.
                         */
                        userDefinedMaxPlaylistOrProfileItemsLimit = -1;
                    }
                }
            }
        }
        if (videoIdsToAdd.isEmpty()) {
            /* Playlist / Channel */
            if (userDefinedMaxPlaylistOrProfileItemsLimit == 0) {
                /* This should never happen but it is a double-check left here on purpose. */
                logger.info(logtextForUserDisabledCrawlerByLimitSetting);
                return ret;
            }
            try {
                if (!isChannelOrProfileShorts && !StringUtils.isEmpty(userName) && StringUtils.isEmpty(playlistID) && cfg.getProfileCrawlMode() == ProfileCrawlMode.PLAYLIST) {
                    /*
                     * the user channel parser only parses 1050 videos. this workaround finds the user channel playlist and parses this
                     * playlist instead
                     */
                    logger.info("Trying to find playlistID for profile-playlist 'Uploads by " + userName + "'");
                    helper.getPage(br, "https://www.youtube.com/@" + userName + "/featured");
                    helper.parse();
                    // channel title isn't user_name. user_name is /user/ reference. check logic in YoutubeHelper.extractData()!
                    final String channelTitle = extractWebsiteTitle(br);
                    if (channelTitle != null) {
                        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_CHANNEL_TITLE, channelTitle);
                    }
                    globalPropertiesForDownloadLink.put(YoutubeHelper.YT_USER_NAME, userName);
                    // you can convert channelid UC[STATICHASH] (UserChanel) ? to UU[STATICHASH] (UsersUpload) which is covered below
                    channelID = getChannelID(helper, br);
                    if (channelID == null) {
                        logger.info("Unable to find playlistID -> Crawler is broken or profile does not exist");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    globalPropertiesForDownloadLink.put(YoutubeHelper.YT_CHANNEL_ID, channelID);
                    /* channelID starts with "UC" */
                    playlistID = "UU" + channelID.substring(2);
                }
                if (!isChannelOrProfileShorts && StringUtils.isEmpty(playlistID) && !StringUtils.isEmpty(channelID) && cfg.getProfileCrawlMode() == ProfileCrawlMode.PLAYLIST) {
                    /*
                     * you can not use this with /c or /channel based urls, it will pick up false positives. see
                     * https://www.youtube.com/channel/UCOSGEokQQcdAVFuL_Aq8dlg, it will find list=PLc-T0ryHZ5U_FtsfHQopuvQugBvRoVR3j which
                     * only contains 27 videos not the entire channels 112
                     */
                    logger.info("Trying to find playlistID for channel-playlist 'Uploads by " + channelID + "'");
                    helper.getPage(br, getBaseURL() + "/channel/" + channelID);
                    playlistID = br.getRegex("list=([A-Za-z0-9\\-_]+)\"[^<>]+play-all-icon-btn").getMatch(0);
                    if (StringUtils.isEmpty(playlistID) && channelID.startsWith("UC")) {
                        /* channel has no play all button. */
                        playlistID = "UU" + channelID.substring(2);
                    }
                    if (playlistID == null) {
                        logger.info("Unable to find playlistID -> Crawler is broken or profile does not exist");
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
                final ArrayList<YoutubeClipData> playlist = crawlPlaylistOrChannel(helper, br, playlistID, userName, channelID, cleanedurl, userDefinedMaxPlaylistOrProfileItemsLimit);
                if (playlist != null) {
                    videoIdsToAdd.addAll(playlist);
                }
                videoIdsToAdd.addAll(parseVideoIds(video_ids_comma_separated));
            } catch (InterruptedException e) {
                logger.log(e);
                return ret;
            }
        }
        boolean reversePlaylistNumber = false;
        final boolean isCrawlDupeCheckEnabled = cfg.isCrawlDupeCheckEnabled();
        final Set<String> videoIDsdupeCheck = new HashSet<String>();
        for (YoutubeClipData vid : videoIdsToAdd) {
            if (isCrawlDupeCheckEnabled && linkCollectorContainsEntryByID(vid.videoID)) {
                logger.info("CrawlDupeCheck skip:" + vid.videoID);
                continue;
            } else if (!videoIDsdupeCheck.add(vid.videoID)) {
                logger.info("Duplicated Video skip:" + vid.videoID);
                continue;
            }
            try {
                // make sure that we reload the video
                final boolean hasCache = ClipDataCache.hasCache(helper, vid.videoID);
                final YoutubeClipData old = vid;
                try {
                    vid = ClipDataCache.load(helper, vid);
                } catch (Exception e) {
                    logger.log(e);
                    if (hasCache) {
                        ClipDataCache.clearCache(vid.videoID);
                        vid = ClipDataCache.load(helper, vid);
                    } else {
                        throw e;
                    }
                }
                if (vid.playlistEntryNumber == -1 && old.playlistEntryNumber > 0) {
                    vid.playlistEntryNumber = reversePlaylistNumber ? (videoIdsToAdd.size() - old.playlistEntryNumber + 1) : old.playlistEntryNumber;
                } else if (vid.playlistEntryNumber == -1 && StringUtils.equals(videoID, vid.videoID)) {
                    final String index = new Regex(cleanedurl, "(?i)index=(\\d+)").getMatch(0);
                    if (index != null) {
                        vid.playlistEntryNumber = Integer.parseInt(index);
                    }
                }
            } catch (Exception e) {
                logger.log(e);
                String emsg = null;
                try {
                    emsg = e.getMessage().toString();
                } catch (NullPointerException npe) {
                    // e.message can be null...
                }
                if (emsg != null && StringUtils.isEmpty(vid.error)) {
                    vid.error = emsg;
                }
                if (vid.streams == null || StringUtils.isNotEmpty(vid.error)) {
                    ret.add(createOfflinelink(YoutubeDashV2.generateContentURL(vid.videoID), "Error - " + vid.videoID + (vid.title != null ? " [" + vid.title + "]:" : "") + " " + vid.error, vid.error));
                    continue;
                }
            }
            if (vid.streams == null || StringUtils.isNotEmpty(vid.error)) {
                ret.add(createOfflinelink(YoutubeDashV2.generateContentURL(vid.videoID), "Error - " + vid.videoID + (vid.title != null ? " [" + vid.title + "]:" : "") + " " + vid.error, vid.error));
                if (vid.streams == null) {
                    continue;
                }
            }
            final List<AbstractVariant> enabledVariants = new ArrayList<AbstractVariant>(AbstractVariant.listVariants());
            final HashSet<VariantGroup> enabledVariantGroups = new HashSet<VariantGroup>();
            final VideoResolution maxVideoResolution = CFG_YOUTUBE.CFG.getMaxVideoResolution();
            {
                // nest this, so we don't have variables table full of entries that get called only once
                final List<VariantIDStorable> disabled = CFG_YOUTUBE.CFG.getDisabledVariants();
                final HashSet<String> disabledIds = new HashSet<String>();
                if (disabled != null) {
                    for (VariantIDStorable v : disabled) {
                        disabledIds.add(v.createUniqueID());
                    }
                }
                final HashSet<AudioBitrate> disabledAudioBitrates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioBitrates());
                final HashSet<AudioCodec> disabledAudioCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedAudioCodecs());
                final HashSet<FileContainer> disabledFileContainers = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedFileContainers());
                final HashSet<VariantGroup> disabledGroups = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedGroups());
                final HashSet<Projection> disabledProjections = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedProjections());
                final HashSet<VideoResolution> disabledResolutions = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedResolutions());
                final HashSet<VideoCodec> disabledVideoCodecs = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoCodecs());
                final HashSet<VideoFrameRate> disabledFramerates = createHashSet(CFG_YOUTUBE.CFG.getBlacklistedVideoFramerates());
                for (final Iterator<AbstractVariant> it = enabledVariants.iterator(); it.hasNext();) {
                    AbstractVariant cur = it.next();
                    if (disabledGroups.contains(cur.getGroup())) {
                        it.remove();
                        continue;
                    }
                    if (disabledFileContainers.contains(cur.getContainer())) {
                        it.remove();
                        continue;
                    }
                    if (cur instanceof AudioInterface) {
                        if (disabledAudioBitrates.contains(((AudioInterface) cur).getAudioBitrate())) {
                            it.remove();
                            continue;
                        }
                        if (disabledAudioCodecs.contains(((AudioInterface) cur).getAudioCodec())) {
                            it.remove();
                            continue;
                        }
                    }
                    if (cur instanceof VideoVariant) {
                        if (disabledVideoCodecs.contains(((VideoVariant) cur).getVideoCodec())) {
                            it.remove();
                            continue;
                        }
                        if (disabledResolutions.contains(((VideoVariant) cur).getVideoResolution())) {
                            it.remove();
                            continue;
                        }
                        if (disabledFramerates.contains(((VideoVariant) cur).getiTagVideo().getVideoFrameRate())) {
                            it.remove();
                            continue;
                        }
                        if (disabledProjections.contains(((VideoVariant) cur).getProjection())) {
                            it.remove();
                            continue;
                        }
                    }
                    if (cur instanceof ImageVariant) {
                        if (disabledResolutions.contains(VideoResolution.getByHeight(((ImageVariant) cur).getHeight()))) {
                            it.remove();
                            continue;
                        }
                    }
                    if (disabledIds.contains(new AbstractVariantWrapper(cur).getVariableIDStorable().createUniqueID())) {
                        it.remove();
                        continue;
                    }
                    enabledVariantGroups.add(cur.getGroup());
                }
            }
            // write all available variants to groups and allVariants
            List<VariantInfo> foundVariants = vid.findVariants();
            VideoVariant bestVideoResolution = null;
            {
                final Iterator<VariantInfo> it = foundVariants.iterator();
                while (it.hasNext()) {
                    final VariantInfo vi = it.next();
                    if (vi.getVariant() instanceof VideoVariant) {
                        final VideoVariant videoVariant = (VideoVariant) vi.getVariant();
                        if (bestVideoResolution == null || bestVideoResolution.getVideoHeight() < videoVariant.getVideoHeight()) {
                            bestVideoResolution = videoVariant;
                        }
                        if (videoVariant.getVideoHeight() > maxVideoResolution.getHeight()) {
                            it.remove();
                        }
                    }
                }
            }
            vid.bestVideoItag = bestVideoResolution;
            List<VariantInfo> subtitles = enabledVariantGroups.contains(VariantGroup.SUBTITLES) ? vid.findSubtitleVariants() : new ArrayList<VariantInfo>();
            ArrayList<VariantInfo> descriptions = enabledVariantGroups.contains(VariantGroup.DESCRIPTION) ? vid.findDescriptionVariant() : new ArrayList<VariantInfo>();
            if (subtitles != null) {
                foundVariants.addAll(subtitles);
            }
            if (descriptions != null) {
                foundVariants.addAll(descriptions);
            }
            List<YoutubeVariantCollection> links = YoutubeVariantCollection.load();
            if (requestedVariant != null) {
                // create a dummy collection
                links = new ArrayList<YoutubeVariantCollection>();
                ArrayList<VariantIDStorable> varList = new ArrayList<VariantIDStorable>();
                varList.add(new VariantIDStorable(requestedVariant));
                links.add(new YoutubeVariantCollection("Dummy", varList));
            }
            final HashMap<String, AbstractVariant> allowedVariantsMap = new HashMap<String, AbstractVariant>();
            for (AbstractVariant v : enabledVariants) {
                final VariantIDStorable storable = new VariantIDStorable(v);
                allowedVariantsMap.put(storable.createUniqueID(), v);
            }
            final HashMap<VariantInfo, String[]> foundVariableMap = new HashMap<VariantInfo, String[]>();
            for (VariantInfo v : foundVariants) {
                final VariantIDStorable storable = new VariantIDStorable(v.getVariant());
                foundVariableMap.put(v, new String[] { storable.createUniqueID(), storable.createGroupingID(), storable.getContainer() });
            }
            if (CFG_YOUTUBE.CFG.isCollectionMergingEnabled()) {
                for (YoutubeVariantCollection l : links) {
                    if (!l.isEnabled()) {
                        continue;
                    }
                    final ArrayList<VariantInfo> linkVariants = new ArrayList<VariantInfo>();
                    final ArrayList<VariantInfo> cutLinkVariantsDropdown = new ArrayList<VariantInfo>();
                    final HashSet<String> customAlternateSet = l.createUniqueIDSetForDropDownList();
                    if (customAlternateSet.size() > 0) {
                        for (Entry<VariantInfo, String[]> foundVariant : foundVariableMap.entrySet()) {
                            final String uId = foundVariant.getValue()[0];
                            if (customAlternateSet.contains(uId)) {
                                if (allowedVariantsMap.containsKey(uId)) {
                                    final VariantInfo variant = foundVariant.getKey();
                                    cutLinkVariantsDropdown.add(variant);
                                    helper.extendedDataLoading(variant, foundVariants);
                                }
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(l.getGroupingID())) {
                        final String groupingID = l.getGroupingID();
                        for (Entry<VariantInfo, String[]> foundVariant : foundVariableMap.entrySet()) {
                            final String gId = foundVariant.getValue()[1];
                            final String cId = foundVariant.getValue()[2];
                            if (StringUtils.equals(groupingID, gId) || StringUtils.equals(groupingID, cId)) {
                                final String uId = foundVariant.getValue()[0];
                                if (allowedVariantsMap.containsKey(uId)) {
                                    final VariantInfo variant = foundVariant.getKey();
                                    linkVariants.add(variant);
                                    helper.extendedDataLoading(variant, foundVariants);
                                }
                            }
                        }
                    } else if (l.getVariants() != null && l.getVariants().size() > 0) {
                        HashSet<String> idSet = l.createUniqueIDSet();
                        for (Entry<VariantInfo, String[]> foundVariant : foundVariableMap.entrySet()) {
                            final String uId = foundVariant.getValue()[0];
                            if (idSet.contains(uId)) {
                                if (allowedVariantsMap.containsKey(uId)) {
                                    final VariantInfo variant = foundVariant.getKey();
                                    linkVariants.add(variant);
                                    helper.extendedDataLoading(variant, foundVariants);
                                }
                            }
                        }
                    } else {
                        continue;
                    }
                    Collections.sort(cutLinkVariantsDropdown, new Comparator<VariantInfo>() {
                        @Override
                        public int compare(VariantInfo o1, VariantInfo o2) {
                            return o2.compareTo(o1);
                        }
                    });
                    Collections.sort(linkVariants, new Comparator<VariantInfo>() {
                        @Override
                        public int compare(VariantInfo o1, VariantInfo o2) {
                            return o2.compareTo(o1);
                        }
                    });
                    // remove dupes
                    VariantInfo last = null;
                    for (final Iterator<VariantInfo> it = linkVariants.iterator(); it.hasNext();) {
                        VariantInfo cur = it.next();
                        if (last != null) {
                            if (StringUtils.equals(cur.getVariant().getTypeId(), last.getVariant().getTypeId())) {
                                it.remove();
                                continue;
                            }
                        }
                        last = cur;
                    }
                    last = null;
                    for (final Iterator<VariantInfo> it = cutLinkVariantsDropdown.iterator(); it.hasNext();) {
                        VariantInfo cur = it.next();
                        if (last != null) {
                            if (StringUtils.equals(cur.getVariant().getTypeId(), last.getVariant().getTypeId())) {
                                it.remove();
                                continue;
                            }
                        }
                        last = cur;
                    }
                    // for (final Iterator<VariantInfo> it = linkVariants.iterator(); it.hasNext();) {
                    // VariantInfo cur = it.next();
                    // System.out.println(cur.getVariant().getBaseVariant() + "\t" + cur.getVariant().getQualityRating());
                    // }
                    if (linkVariants.size() > 0) {
                        DownloadLink lnk = createLink(l, linkVariants.get(0), cutLinkVariantsDropdown.size() > 0 ? cutLinkVariantsDropdown : linkVariants);
                        ret.add(lnk);
                        if (linkVariants.get(0).getVariant().getGroup() == VariantGroup.SUBTITLES) {
                            List<String> extras = CFG_YOUTUBE.CFG.getExtraSubtitles();
                            if (extras != null) {
                                for (String s : extras) {
                                    if (s != null) {
                                        for (VariantInfo vi : linkVariants) {
                                            if (vi.getVariant() instanceof SubtitleVariant) {
                                                if ("*".equals(s)) {
                                                    lnk = createLink(l, vi, cutLinkVariantsDropdown.size() > 0 ? cutLinkVariantsDropdown : linkVariants);
                                                    ret.add(lnk);
                                                } else if (StringUtils.equalsIgnoreCase(((SubtitleVariant) vi.getVariant()).getGenericInfo().getLanguage(), s)) {
                                                    lnk = createLink(l, vi, cutLinkVariantsDropdown.size() > 0 ? cutLinkVariantsDropdown : linkVariants);
                                                    ret.add(lnk);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ArrayList<VariantInfo> linkVariants = new ArrayList<VariantInfo>();
                for (Entry<VariantInfo, String[]> foundVariant : foundVariableMap.entrySet()) {
                    final String uId = foundVariant.getValue()[0];
                    if (allowedVariantsMap.containsKey(uId)) {
                        final VariantInfo variant = foundVariant.getKey();
                        linkVariants.add(variant);
                        helper.extendedDataLoading(variant, foundVariants);
                    }
                }
                Collections.sort(linkVariants, new Comparator<VariantInfo>() {
                    @Override
                    public int compare(VariantInfo o1, VariantInfo o2) {
                        return o2.compareTo(o1);
                    }
                });
                // remove dupes
                // System.out.println("Link " + l.getName());
                VariantInfo last = null;
                for (final Iterator<VariantInfo> it = linkVariants.iterator(); it.hasNext();) {
                    VariantInfo cur = it.next();
                    if (last != null) {
                        if (StringUtils.equals(cur.getVariant().getTypeId(), last.getVariant().getTypeId())) {
                            it.remove();
                            continue;
                        }
                    }
                    last = cur;
                }
                for (VariantInfo vi : linkVariants) {
                    ArrayList<VariantInfo> lst = new ArrayList<VariantInfo>();
                    lst.add(vi);
                    DownloadLink lnk = createLink(new YoutubeVariantCollection(), vi, lst);
                    ret.add(lnk);
                }
            }
            if (this.isAbort()) {
                logger.info("Aborted!");
                return ret;
            }
        }
        return ret;
    }

    private String getChannelID(final YoutubeHelper helper, final Browser br) {
        String channelID = helper != null ? helper.getChannelIdFromMaps() : null;
        if (channelID == null) {
            channelID = br.getRegex("<meta itemprop=\"channelId\" content=\"(UC[A-Za-z0-9\\-_]+)\"").getMatch(0);
            if (channelID == null) {
                channelID = br.getRegex("yt\\.setConfig\\(\\s*'CHANNEL_ID'\\s*,\\s*\"(UC[A-Za-z0-9\\-_]+)\"").getMatch(0);
                if (channelID == null) {
                    channelID = br.getRegex("rssURL\"\\s*:\\s*\"https?://[^\"]*channel_ID=(UC[A-Za-z0-9\\-_]+)\"").getMatch(0);
                }
            }
        }
        return channelID;
    }

    private <T> HashSet<T> createHashSet(List<T> list) {
        HashSet<T> ret = new HashSet<T>();
        if (list != null) {
            ret.addAll(list);
        }
        return ret;
    }

    private DownloadLink createLink(YoutubeVariantCollection l, VariantInfo variantInfo, List<VariantInfo> alternatives) {
        try {
            YoutubeClipData clip = null;
            if (clip == null && variantInfo.getVideoStreams() != null) {
                clip = variantInfo.getVideoStreams().get(0).getClip();
            }
            if (clip == null && variantInfo.getAudioStreams() != null) {
                clip = variantInfo.getAudioStreams().get(0).getClip();
            }
            if (clip == null && variantInfo.getDataStreams() != null) {
                clip = variantInfo.getDataStreams().get(0).getClip();
            }
            boolean hasVariants = false;
            ArrayList<String> altIds = new ArrayList<String>();
            if (alternatives != null) {
                for (VariantInfo vi : alternatives) {
                    if (!StringUtils.equals(variantInfo.getVariant()._getUniqueId(), vi.getVariant()._getUniqueId())) {
                        hasVariants = true;
                    }
                    altIds.add(vi.getVariant().getStorableString());
                }
            }
            final String linkID = YoutubeHelper.createLinkID(clip.videoID, variantInfo.getVariant());
            final DownloadLink ret = createDownloadlink(linkID);
            final YoutubeHelper helper = new YoutubeHelper(br, getLogger());
            ClipDataCache.referenceLink(helper, ret, clip);
            // thislink.setAvailable(true);
            ret.setContentUrl(getBaseURL() + "/watch?v=" + clip.videoID + "#variant=" + Encoding.urlEncode(Base64.encode(variantInfo.getVariant().getStorableString())));
            // thislink.setProperty(key, value)
            ret.setProperty(YoutubeHelper.YT_ID, clip.videoID);
            ret.setProperty(YoutubeHelper.YT_COLLECTION, l.getName());
            for (Entry<String, Object> es : globalPropertiesForDownloadLink.entrySet()) {
                if (es.getKey() != null && !ret.hasProperty(es.getKey())) {
                    ret.setProperty(es.getKey(), es.getValue());
                }
            }
            clip.copyToDownloadLink(ret);
            // thislink.getTempProperties().setProperty(YoutubeHelper.YT_VARIANT_INFO, variantInfo);
            ret.setVariantSupport(hasVariants);
            ret.setProperty(YoutubeHelper.YT_VARIANTS, altIds);
            // Object cache = downloadLink.getTempProperties().getProperty(YoutubeHelper.YT_VARIANTS, null);
            // thislink.setProperty(YoutubeHelper.YT_VARIANT, variantInfo.getVariant()._getUniqueId());
            YoutubeHelper.writeVariantToDownloadLink(ret, variantInfo.getVariant());
            // variantInfo.fillExtraProperties(thislink, alternatives);
            String filename = helper.createFilename(ret);
            ret.setFinalFileName(filename);
            ret.setLinkID(linkID);
            FilePackage fp = FilePackage.getInstance();
            final String fpName = helper.replaceVariables(ret, helper.getConfig().getPackagePattern());
            // req otherwise returned "" value = 'various', regardless of user settings for various!
            if (StringUtils.isNotEmpty(fpName)) {
                fp.setName(fpName);
                // let the packagizer merge several packages that have the same name
                fp.setAllowMerge(true);
                fp.add(ret);
            }
            long estimatedFileSize = 0;
            final AbstractVariant variant = variantInfo.getVariant();
            switch (variant.getType()) {
            case VIDEO:
            case DASH_AUDIO:
            case DASH_VIDEO:
                final StreamCollection audioStreams = clip.getStreams(variant.getBaseVariant().getiTagAudio());
                if (audioStreams != null && audioStreams.size() > 0) {
                    for (YoutubeStreamData stream : audioStreams) {
                        if (stream.getContentLength() > 0) {
                            estimatedFileSize += stream.getContentLength();
                            break;
                        } else if (stream.estimatedContentLength() > 0) {
                            estimatedFileSize += stream.estimatedContentLength();
                            break;
                        }
                    }
                }
                final StreamCollection videoStreams = clip.getStreams(variant.getBaseVariant().getiTagVideo());
                if (videoStreams != null && videoStreams.size() > 0) {
                    for (YoutubeStreamData stream : videoStreams) {
                        if (stream.getContentLength() > 0) {
                            estimatedFileSize += stream.getContentLength();
                            break;
                        } else if (stream.estimatedContentLength() > 0) {
                            estimatedFileSize += stream.estimatedContentLength();
                            break;
                        }
                    }
                }
                break;
            case IMAGE:
                final StreamCollection dataStreams = clip.getStreams(variant.getiTagData());
                if (dataStreams != null && dataStreams.size() > 0) {
                    for (YoutubeStreamData stream : dataStreams) {
                        if (stream.getContentLength() > 0) {
                            estimatedFileSize += stream.getContentLength();
                            break;
                        } else if (stream.estimatedContentLength() > 0) {
                            estimatedFileSize += stream.estimatedContentLength();
                            break;
                        }
                    }
                }
                break;
            default:
                break;
            }
            if (estimatedFileSize > 0) {
                ret.setDownloadSize(estimatedFileSize);
            }
            ret.setAvailableStatus(AvailableStatus.TRUE);
            return ret;
        } catch (Exception e) {
            getLogger().log(e);
            return null;
        }
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

    private ArrayList<YoutubeClipData> crawlPlaylistOrChannel(final YoutubeHelper helper, final Browser br, final String playlistID, final String userName, final String channelID, final String referenceUrl, final int maxItemsLimit) throws Exception {
        if (StringUtils.isEmpty(playlistID) && StringUtils.isEmpty(userName) && StringUtils.isEmpty(channelID)) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        } else if (maxItemsLimit == 0) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        if (helper.getAccountLoggedIn() == null) {
            /*
             * Only set User-Agent if we're not logged in because login session can be bound to User-Agent and tinkering around with
             * different User-Agents and the same cookies is just a bad idea!
             */
            // firefox gets different result than chrome! lets hope switching wont cause issue.
            br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(BrowserName.Chrome));
        }
        br.getHeaders().put("Accept-Charset", null);
        final boolean isChannelOrProfileShorts = isChannelOrProfileShorts(referenceUrl);
        String userOrPlaylistURL;
        if (playlistID != null) {
            userOrPlaylistURL = getBaseURL() + "/playlist?list=" + playlistID;
        } else if (channelID != null) {
            /* Channel via channelID (legacy - urls containing only channelID are not common anymore) */
            userOrPlaylistURL = getBaseURL() + "/channel/" + channelID;
            if (isChannelOrProfileShorts) {
                userOrPlaylistURL += "/shorts";
            } else {
                userOrPlaylistURL += "/videos";
            }
        } else {
            /* Channel/User */
            userOrPlaylistURL = getBaseURL() + "/@" + userName;
            if (isChannelOrProfileShorts) {
                userOrPlaylistURL += "/shorts";
            } else {
                userOrPlaylistURL += "/videos";
            }
        }
        // TODO: Add better/proper check for offline/invalid channel/playlist/user.
        helper.getPage(br, userOrPlaylistURL);
        final String originalURL = br.getURL();
        final ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        // user list it's not a playlist.... just a channel decryption. this can return incorrect information.
        final String playListTitleHTML = extractWebsiteTitle(br);
        if (playListTitleHTML != null) {
            globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_TITLE, Encoding.htmlDecode(playListTitleHTML).trim());
        }
        helper.parse();
        Map<String, Object> ytConfigData = null;
        boolean isJson = false;
        Browser pbr = br.cloneBrowser();
        int videoPositionCounter = 1;
        int round = 0;
        final String INNERTUBE_CLIENT_NAME = helper.getYtCfgSet() != null ? String.valueOf(helper.getYtCfgSet().get("INNERTUBE_CONTEXT_CLIENT_NAME")) : null;
        final String INNERTUBE_API_KEY = helper.getYtCfgSet() != null ? String.valueOf(helper.getYtCfgSet().get("INNERTUBE_API_KEY")) : null;
        final String INNERTUBE_CLIENT_VERSION = helper.getYtCfgSet() != null ? String.valueOf(helper.getYtCfgSet().get("INNERTUBE_CLIENT_VERSION")) : null;
        /* Now stuff that is required when user is logged in. */
        final String DELEGATED_SESSION_ID = helper.getYtCfgSet() != null ? String.valueOf(helper.getYtCfgSet().get("DELEGATED_SESSION_ID")) : null;
        final Set<String> playListDupes = new HashSet<String>();
        Integer totalNumberofItems = null;
        String userWishedSort = null;
        /* Check if user wishes different sort than default */
        final PlaylistAndChannelCrawlerAddOrder addOrder = cfg.getPlaylistAndChannelCrawlerAddOrder();
        if (addOrder == PlaylistAndChannelCrawlerAddOrder.LATEST) {
            userWishedSort = "Latest";
        } else if (addOrder == PlaylistAndChannelCrawlerAddOrder.POPULAR) {
            userWishedSort = "Popular";
        } else if (addOrder == PlaylistAndChannelCrawlerAddOrder.OLDEST) {
            userWishedSort = "Oldest";
        }
        do {
            /* First try old HTML handling though by now [June 2023] all data is provided via json. */
            String nextPageToken = null;
            checkErrors(pbr);
            final int playListDupesSizeOld = playListDupes.size();
            boolean reachedUserDefinedMaxItemsLimit = false;
            if (!isJson) {
                logger.info("Jumping into json handling");
                isJson = true;
            }
            final Map<String, Object> rootMap;
            List<Map<String, Object>> varray = null;
            if (round == 0) {
                rootMap = helper.getYtInitialData();
                if (userWishedSort != null && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    // Latest, Popular, Oldest
                    final String sortToken = (String) this.findSortToken(rootMap, userWishedSort);
                    if (sortToken == null) {
                        logger.info("Unable to sort by " + userWishedSort + ": Either this item is not sortable or it is already sorted in the wished order");
                    } else {
                        logger.info("Token for sort is: " + sortToken);
                    }
                }
                Map<String, Object> playlisttab = null;
                Map<String, Object> shortstab = null;
                Map<String, Object> videostab = null;
                ytConfigData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(rootMap, "responseContext/webResponseContextExtensionData/ytConfigData");
                String videosCountText = null;
                final List<Map<String, Object>> tabs = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(rootMap, "contents/twoColumnBrowseResultsRenderer/tabs");
                if (tabs != null) {
                    for (final Map<String, Object> tab : tabs) {
                        /* We will get this one if a real playlist is our currently opened tab. */
                        final Map<String, Object> tabRenderer = (Map<String, Object>) tab.get("tabRenderer");
                        final Object varrayPlaylistProbe = JavaScriptEngineFactory.walkJson(tabRenderer, "content/sectionListRenderer/contents/{}/itemSectionRenderer/contents/{}/playlistVideoListRenderer/contents");
                        if (varrayPlaylistProbe != null) {
                            /* Real playlist */
                            playlisttab = tab;
                            varray = (List<Map<String, Object>>) varrayPlaylistProbe;
                            break;
                        } else if (tabRenderer != null) {
                            /* Channel/User */
                            final String title = (String) tabRenderer.get("title");
                            final Boolean selected = (Boolean) tabRenderer.get("selected");
                            final List<Map<String, Object>> varrayTmp = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(tabRenderer, "content/richGridRenderer/contents");
                            if ("Shorts".equalsIgnoreCase(title)) {
                                shortstab = tab;
                                if (Boolean.TRUE.equals(selected) && varrayTmp != null) {
                                    varray = varrayTmp;
                                }
                            } else if ("Videos".equalsIgnoreCase(title)) {
                                videostab = tab;
                                if (Boolean.TRUE.equals(selected) && varrayTmp != null) {
                                    varray = varrayTmp;
                                }
                            } else {
                                /* Other/Unsupported tab -> Ignore */
                            }
                        }
                    }
                    if (shortstab == null && isChannelOrProfileShorts && videostab != null) {
                        logger.info("User wanted shorts but channel doesn't contain shorts tab -> Only videos is possible");
                    } else if (shortstab != null && !isChannelOrProfileShorts) {
                        logger.info("User wanted videos but channel doesn't contain videos tab -> Only shorts is possible");
                    }
                }
                /* This message can also contain information like "2 unavailable videos won't be displayed in this list". */
                final String errormessage = (String) JavaScriptEngineFactory.walkJson(rootMap, "alerts/{0}/alertRenderer/text/runs/{0}/text");
                if (varray == null && errormessage != null) {
                    throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "CHANNEL_OR_PLAYLIST_OFFLINE_" + this.playlistID, errormessage);
                }
                /* Find extra information about playlist */
                final Map<String, Object> playlistHeaderRenderer = (Map<String, Object>) JavaScriptEngineFactory.walkJson(rootMap, "header/playlistHeaderRenderer");
                if (playlistHeaderRenderer != null) {
                    /* This is the better source for playlist title than html. */
                    final String playlistTitle = (String) JavaScriptEngineFactory.walkJson(playlistHeaderRenderer, "title/simpleText");
                    if (playlistTitle != null) {
                        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_TITLE, playlistTitle);
                    }
                    videosCountText = (String) JavaScriptEngineFactory.walkJson(playlistHeaderRenderer, "numVideosText/runs/{0}/text");
                }
                /* Find extra information about channel */
                if (!isChannelOrProfileShorts) {
                    final Map<String, Object> channelHeaderRenderer = (Map<String, Object>) JavaScriptEngineFactory.walkJson(rootMap, "header/c4TabbedHeaderRenderer");
                    if (channelHeaderRenderer != null) {
                        videosCountText = (String) JavaScriptEngineFactory.walkJson(channelHeaderRenderer, "videosCountText/runs/{0}/text");
                    }
                }
                if (videosCountText != null) {
                    videosCountText = videosCountText.replaceAll("(\\.|,)", "");
                    if (videosCountText.matches("\\d+")) {
                        totalNumberofItems = Integer.valueOf(videosCountText);
                        globalPropertiesForDownloadLink.put(YoutubeHelper.YT_PLAYLIST_SIZE, totalNumberofItems);
                    }
                }
            } else {
                rootMap = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> onResponseReceivedActions = (List<Map<String, Object>>) rootMap.get("onResponseReceivedActions");
                final Map<String, Object> lastReceivedAction = onResponseReceivedActions.get(onResponseReceivedActions.size() - 1);
                varray = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(lastReceivedAction, "appendContinuationItemsAction/continuationItems");
                if (varray == null) {
                    /* E.g. at the beginning after sorting */
                    varray = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(lastReceivedAction, "reloadContinuationItemsCommand/continuationItems");
                }
            }
            if (varray == null) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final Map<String, Object> vid : varray) {
                /* Playlist */
                String id = (String) JavaScriptEngineFactory.walkJson(vid, "playlistVideoRenderer/videoId");
                if (id == null) {
                    /* /@profile/videos */
                    id = (String) JavaScriptEngineFactory.walkJson(vid, "richItemRenderer/content/videoRenderer/videoId");
                    if (id == null) {
                        /* Reel/Short */
                        id = (String) JavaScriptEngineFactory.walkJson(vid, "richItemRenderer/content/reelItemRenderer/videoId");
                    }
                }
                /* Typically last item (item 101) will contain the continuationToken. */
                final String continuationToken = (String) JavaScriptEngineFactory.walkJson(vid, "continuationItemRenderer/continuationEndpoint/continuationCommand/token");
                if (id != null) {
                    playListDupes.add(id);
                    ret.add(new YoutubeClipData(id, videoPositionCounter++));
                    if (playListDupes.size() == maxItemsLimit) {
                        reachedUserDefinedMaxItemsLimit = true;
                        break;
                    }
                } else if (continuationToken != null) {
                    /* Typically last item contains token for next page */
                    nextPageToken = continuationToken;
                } else {
                    logger.info("Found unknown playlist item: " + vid);
                }
            }
            /* Check for some abort conditions */
            final int numberofNewItemsThisRun = playListDupes.size() - playListDupesSizeOld;
            logger.info("Crawled page " + round + " | Found items on this page [== pagination_size]: " + numberofNewItemsThisRun + " | Found items so far: " + playListDupes.size() + "/" + totalNumberofItems + " | nextPageToken = " + nextPageToken + " | Max items limit: " + maxItemsLimit);
            if (this.isAbort()) {
                throw new InterruptedException();
            } else if (reachedUserDefinedMaxItemsLimit) {
                logger.info("Stopping because: Reached max items limit of " + maxItemsLimit);
                break;
            } else if (numberofNewItemsThisRun == 0) {
                logger.info("Stopping because: No new videoIDs found on current page");
                break;
            } else if (nextPageToken == null) {
                logger.info("Stopping because: No next page found");
                break;
            } else {
                /* Try to continue to next page */
                if (StringUtils.isEmpty(INNERTUBE_CLIENT_NAME) || StringUtils.isEmpty(INNERTUBE_API_KEY) || StringUtils.isEmpty(INNERTUBE_CLIENT_VERSION)) {
                    /* This should never happen. */
                    logger.info("Stopping because: Pagination is broken due to missing 'INNERTUBE' variable");
                    break;
                }
                final Map<String, Object> context = new HashMap<String, Object>();
                final Map<String, Object> client = new HashMap<String, Object>();
                /* Field "visitorData" is required for e.g. /@profile/shorts" but not for "real" playlists.yin */
                String visitorData = ytConfigData != null ? (String) ytConfigData.get("visitorData") : null;
                if (visitorData != null) {
                    client.put("visitorData", visitorData);
                }
                client.put("clientName", INNERTUBE_CLIENT_NAME);
                client.put("clientVersion", INNERTUBE_CLIENT_VERSION);
                client.put("originalUrl", originalURL);
                context.put("client", client);
                final Map<String, Object> paginationPostData = new HashMap<String, Object>();
                paginationPostData.put("context", context);
                paginationPostData.put("continuation", nextPageToken);
                round = antiDdosSleep(round);
                /* Set headers on every run as some tokens (Authorization header!) contain timestamps so they can expire. */
                prepBrowserWebAPI(br, helper.getAccountLoggedIn());
                if (DELEGATED_SESSION_ID != null) {
                    br.getHeaders().put("X-Goog-Pageid", DELEGATED_SESSION_ID);
                }
                br.postPageRaw("/youtubei/v1/browse?key=" + INNERTUBE_API_KEY + "&prettyPrint=false", JSonStorage.serializeToJson(paginationPostData));
            }
        } while (true);
        int missingVideos = 0;
        if (totalNumberofItems != null) {
            missingVideos = totalNumberofItems.intValue() - ret.size();
        }
        logger.info("parsePlaylist method returns: " + ret.size() + " VideoIDs | Number of possibly missing videos [due to private/offrline/GEO-block]: " + missingVideos);
        return ret;
    }

    private Object findSortToken(final Object o, final String sortText) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            final String thisSortText = (String) JavaScriptEngineFactory.walkJson(entrymap, "chipCloudChipRenderer/text/simpleText");
            final String thisContinuationCommand = (String) JavaScriptEngineFactory.walkJson(entrymap, "chipCloudChipRenderer/navigationEndpoint/continuationCommand/token");
            if (sortText.equalsIgnoreCase(thisSortText) && thisContinuationCommand != null) {
                return thisContinuationCommand;
            }
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                // final String key = entry.getKey();
                final Object value = entry.getValue();
                if (value instanceof List || value instanceof Map) {
                    final Object pico = findSortToken(value, sortText);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object ret = findSortToken(arrayo, sortText);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return null;
        } else {
            return o;
        }
    }

    public static Browser prepBrowserWebAPI(final Browser br, final Account account) throws PluginException {
        final String domain = "youtube.com";
        final String domainWithProtocol = "https://www." + domain;
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Origin", domainWithProtocol);
        br.getHeaders().put("X-Referer", domainWithProtocol);
        br.getHeaders().put("X-Origin", domainWithProtocol);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/json");
        if (account != null) {
            /* For logged in users: */
            final String sapisidhash = GoogleHelper.getSAPISidHash(br, domainWithProtocol);
            if (sapisidhash != null) {
                br.getHeaders().put("Authorization", "SAPISIDHASH " + sapisidhash);
            }
            br.getHeaders().put("X-Goog-Authuser", "0");
        }
        return br;
    }

    protected String extractWebsiteTitle(final Browser br) {
        return Encoding.htmlOnlyDecode(br.getRegex("<meta name=\"title\"\\s+[^<>]*content=\"(.*?)(?:\\s*-\\s*Youtube\\s*)?\"").getMatch(0));
    }

    /**
     * @param round
     * @return
     * @throws InterruptedException
     */
    protected int antiDdosSleep(int round) throws InterruptedException {
        sleep(((DDOS_WAIT_MAX * (Math.min(DDOS_INCREASE_FACTOR, round++))) / DDOS_INCREASE_FACTOR), getCurrentLink().getCryptedLink());
        return round;
    }

    /**
     * parses 'video_ids=' array, primarily used with watch_videos link
     */
    public ArrayList<YoutubeClipData> parseVideoIds(String video_ids) throws IOException, InterruptedException {
        // /watch_videos?title=Trending&video_ids=0KSOMA3QBU0,uT3SBzmDxGk,X7Xf8DsTWgs,72WhEqeS6AQ,Qc9c12q3mrc,6l7J1i1OkKs,zeu2tI-tqvs,o3mP3mJDL2k,jYdaQJzcAcw&feature=c4-overview&type=0&more_url=
        ArrayList<YoutubeClipData> ret = new ArrayList<YoutubeClipData>();
        int counter = 1;
        if (StringUtils.isNotEmpty(video_ids)) {
            String[] videos = new Regex(video_ids, "(" + VIDEO_ID_PATTERN + ")").getColumn(0);
            if (videos != null) {
                for (String vid : videos) {
                    ret.add(new YoutubeClipData(vid, counter++));
                }
            }
        }
        return ret;
    }

    private void checkErrors(Browser br) throws InterruptedException {
        if (br.containsHTML(">404 Not Found<")) {
            throw new InterruptedException("404 Not Found");
        } else if (br.containsHTML("iframe style=\"display:block;border:0;\" src=\"/error")) {
            throw new InterruptedException("Unknown Error");
        } else if (br.containsHTML("<h2>\\s*This channel does not exist\\.\\s*</h2>")) {
            throw new InterruptedException("Channel does not exist.");
        }
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}