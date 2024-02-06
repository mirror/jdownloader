//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.VKontakteRuHoster;
import jd.plugins.hoster.VKontakteRuHoster.Quality;
import jd.plugins.hoster.VKontakteRuHoster.QualitySelectionMode;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vk.com" }, urls = { "https?://(?:www\\.|m\\.|new\\.)?(?:(?:vk\\.com|vkontakte\\.ru|vkontakte\\.com)/(?!doc[\\d\\-]+_[\\d\\-]+|picturelink|audiolink)[a-z0-9_/=\\.\\-\\?&%@:\\!]+)" })
public class VKontakteRu extends PluginForDecrypt {
    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        VKontakteRuHoster.setRequestIntervalLimits();
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    private static String getBaseURL() {
        return VKontakteRuHoster.getBaseURL();
    }

    private static String getProtocol() {
        return VKontakteRuHoster.getProtocol();
    }

    private static final String EXCEPTION_API_UNKNOWN                     = "EXCEPTION_API_UNKNOWN";
    /* Settings */
    private boolean             vkwall_grabalbums;
    private boolean             vkwall_grabphotos;
    private boolean             vkwall_grabaudio;
    private boolean             vkwall_grabvideo;
    private boolean             vkwall_grablink;
    private boolean             vkwall_comments_grab_comments;
    private boolean             vkwall_grabdocs;
    private boolean             vkwall_graburlsinsideposts;
    private boolean             vkwall_comment_grabphotos;
    private boolean             vkwall_comment_grabaudio;
    private boolean             vkwall_comment_grabvideo;
    private boolean             vkwall_comment_grablink;
    private boolean             photos_store_picture_directurls;
    private String              vkwall_graburlsinsideposts_regex;
    private String              vkwall_graburlsinsideposts_regex_default;
    /* Some supported url patterns */
    private static final String PATTERN_URL_EXTERN                        = "(?i)https?://[^/]+/away\\.php\\?to=.+";
    /** 2022-01-19: Audio stuff is deprecated because it's all DRM protected. No need to fix crawl methods for audio content! */
    @Deprecated
    private static final String PATTERN_GENERAL_AUDIO                     = "(?i)https?://[^/]+/audio.*?";
    @Deprecated
    private static final String PATTERN_AUDIO_ALBUM                       = "(?i)https?://[^/]+/(?:audio(?:\\.php)?\\?id=(?:\\-)?\\d+|audios(?:\\-)?\\d+).*?";
    @Deprecated
    private static final String PATTERN_AUDIO_PAGE                        = "(?i)https?://[^/]+/page\\-\\d+_\\d+.*?";
    @Deprecated
    private static final String PATTERN_AUDIO_PAGE_oid                    = "(?i)https?://[^/]+/pages\\?oid=\\-\\d+\\&p=(?!va_c)[^<>/\"]+";
    @Deprecated
    private static final String PATTERN_AUDIO_AUDIOS_ALBUM                = "(?i)https?://[^/]+/(audios\\-\\d+\\?album_id=\\d+|music/album/-?\\d+_\\d+)";
    @Deprecated
    private static final String PATTERN_AUDIO_AUDIOS_ALBUM_2020           = "(?i)https?://[^/]+/music/album/(-?\\d+)_(\\d+).*?";
    public static final String  PATTERN_VIDEO_SINGLE_Z                    = "(?i)https?://[^/]+/.*?z=video(-?\\d+_\\d+).*?";
    private static final String PATTERN_CLIP_SINGLE_Z                     = "(?i)https?://[^/]+/.*?z=clip((?:\\-)?\\d+_\\d+).*?";
    private static final String PATTERN_VIDEO_SINGLE_ORIGINAL             = "(?i)https?://[^/]+/video((?:\\-)?\\d+_\\d+).*";
    private static final String PATTERN_CLIP_SINGLE_ORIGINAL              = "(?i)https?://[^/]+/clip((?:\\-)?\\d+_\\d+).*";
    private static final String PATTERN_VIDEO_SINGLE_ORIGINAL_WITH_LISTID = "(?i)https?://[^/]+/video(-?\\d+_\\d+)\\?list=([a-z0-9]+)";
    private static final String PATTERN_VIDEO_SINGLE_ORIGINAL_LIST        = "(?i)https?://[^/]+/video(\\-)?\\d+_\\d+\\?list=[a-z0-9]+";
    private static final String PATTERN_VIDEO_SINGLE_EMBED                = "(?i)https?://[^/]+/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+.*?";
    private static final String PATTERN_VIDEO_SINGLE_EMBED_HASH           = "(?i)https?://[^/]+/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+\\&hash=[^&]+.*?";
    private static final String PATTERN_VIDEO_ALBUM                       = "(https?://)?.*?/(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged|video/.*|videos(?:-)?\\d+(?:\\?section=[^\\&]+)?)";
    private static final String PATTERN_VIDEO_COMMUNITY_ALBUM             = "(https?://)?[^/]*/video\\?gid=\\d+.*";
    private static final String PATTERN_PHOTO_SINGLE                      = "https?://[^/]+/photo(\\-)?\\d+_\\d+.*?";
    private static final String PATTERN_PHOTO_SINGLE_Z                    = "(?i)https?://[^/]+/([a-z0-9]+).*z=photo(-?\\d+_\\d+)((/|%2F).+)";
    private static final String PATTERN_PHOTO_ALBUM                       = ".*?(tag|album(?:\\-)?\\d+_|photos(?:\\-)?)\\d+";
    private static final String PATTERN_PHOTO_ALBUMS                      = "(?i)https?://[^/]+/.*?albums((?:\\-)?\\d+)";
    private static final String PATTERN_GENERAL_WALL_LINK                 = "(?i)https?://[^/]+/wall(-?\\d+).*";
    private static final String PATTERN_USER_STORY                        = "(?i)https?://[^/]+/[^\\?]+\\?w=story-?(\\d+)_(\\d+).*";
    private static final String PATTERN_WALL_POST_LINK                    = ".+wall(?:\\-)?\\d+_\\d+.*?";
    private static final String PATTERN_WALL_CLIPS                        = "(?)https?://[^/]+/clips/([^/]+)";
    private static final String PATTERN_PUBLIC_LINK                       = "(?i)https?://[^/]+/public(\\d+)";
    private static final String PATTERN_CLUB_LINK                         = "(?i)https?://[^/]+/club(\\d+).*?";
    private static final String PATTERN_EVENT_LINK                        = "(?i)https?://[^/]+/event(\\d+)";
    private static final String PATTERN_ID_LINK                           = "(?i)https?://[^/]+/id(-?\\d+).*";
    private static final String PATTERN_DOCS                              = "(?i)https?://[^/]+/docs\\?oid=\\-\\d+";
    /* Possible/Known types of single vk-wall-posts */
    private static final String wallpost_type_photo                       = "photo";
    private static final String wallpost_type_doc                         = "doc";
    private static final String wallpost_type_audio                       = "audio";
    private static final String wallpost_type_link                        = "link";
    private static final String wallpost_type_video                       = "video";
    private static final String wallpost_type_album                       = "album";
    private static final String wallpost_type_poll                        = "poll";
    /* Internal settings / constants */
    /*
     * Whenever this number of items gets reached, return array gets cleared to reduce memory usage.
     */
    private final short         MAX_LINKS_PER_RUN                         = 5000;
    /* Used whenever we request arrays via API */
    private final int           API_MAX_ENTRIES_PER_REQUEST               = 100;
    private SubConfiguration    cfg                                       = null;
    private boolean             fastcheck_photo                           = false;
    private boolean             fastcheck_audio                           = false;
    private boolean             vkwall_use_api                            = false;
    private final boolean       docs_add_unique_id                        = true;
    private HashSet<String>     global_dupes                              = new HashSet<String>();
    /* Properties especially for DownloadLink instances that go back into this crawler */
    private final String        VIDEO_PROHIBIT_FASTCRAWL                  = "prohibit_fastcrawl";
    /* Ugly public variables */
    private boolean             grabPhotoAlbums                           = false;

    private String fixAddedURL(final String url) {
        String newurl = url;
        /* Prefer https protocol */
        newurl = newurl.replaceFirst("(?i)http://", "https://");
        /* Remove old/mobile subdomain */
        newurl = newurl.replaceFirst("^https://m\\.", "https://");
        return newurl;
    }

    private ArrayList<DownloadLink> getReturnArray() {
        return new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                distribute(e);
                return super.add(e);
            }

            @Override
            public boolean addAll(Collection<? extends DownloadLink> c) {
                distribute(c.toArray(new DownloadLink[] {}));
                return super.addAll(c);
            }
        };
    }

    /* General error handling language implementation: English */
    /*
     * Information: General link structure: vk.com/ownerID_contentID --> ownerID is always positive for users, negative for communities and
     * groups.
     */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /*
         * URL that browser will redirect to when rate-limit is reached --> Try to extract the target-url out of it so if we're lucky we can
         * "skip" the rate-limit for that individual link.
         */
        if (param.getCryptedUrl().matches("^https?://[^/]*/429\\.html.+")) {
            // rewrite 429.html URLs
            final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
            String redirect429 = query.get("redirect429");
            if (redirect429 != null) {
                redirect429 = redirect429.replace("%3F", "?").replace("%26", "&");
                redirect429 = URLHelper.parseLocation(new URL(param.getCryptedUrl()), redirect429);
                ret.add(createDownloadlink(redirect429));
                return ret;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(true);
        /* Set settings */
        cfg = SubConfiguration.getConfig("vk.com");
        fastcheck_photo = cfg.getBooleanProperty(VKontakteRuHoster.FASTLINKCHECK_PICTURES, VKontakteRuHoster.default_FASTLINKCHECK_PICTURES);
        fastcheck_audio = cfg.getBooleanProperty(VKontakteRuHoster.FASTLINKCHECK_AUDIO, VKontakteRuHoster.default_FASTLINKCHECK_AUDIO);
        vkwall_grabalbums = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_ALBUMS, VKontakteRuHoster.default_VKWALL_GRAB_ALBUMS);
        vkwall_grabphotos = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_PHOTOS, VKontakteRuHoster.default_VKWALL_GRAB_PHOTOS);
        vkwall_grabaudio = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_AUDIO, VKontakteRuHoster.default_VKWALL_GRAB_AUDIO);
        vkwall_grabvideo = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_VIDEO, VKontakteRuHoster.default_VKWALL_GRAB_VIDEO);
        vkwall_grablink = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_URLS, VKontakteRuHoster.default_VKWALL_GRAB_URLS);
        vkwall_grabdocs = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_DOCS, VKontakteRuHoster.default_VKWALL_GRAB_DOCS);
        vkwall_graburlsinsideposts = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_URLS_INSIDE_POSTS, VKontakteRuHoster.default_WALL_ALLOW_lookforurlsinsidewallposts);
        vkwall_use_api = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_USE_API, VKontakteRuHoster.default_VKWALL_USE_API);
        vkwall_graburlsinsideposts_regex_default = VKontakteRuHoster.default_VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX;
        vkwall_graburlsinsideposts_regex = cfg.getStringProperty(VKontakteRuHoster.VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX, vkwall_graburlsinsideposts_regex_default);
        vkwall_comment_grabphotos = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_COMMENTS_PHOTOS, VKontakteRuHoster.default_VKWALL_GRAB_COMMENTS_PHOTOS);
        vkwall_comment_grabaudio = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_COMMENTS_AUDIO, VKontakteRuHoster.default_VKWALL_GRAB_COMMENTS_AUDIO);
        vkwall_comment_grabvideo = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_COMMENTS_VIDEO, VKontakteRuHoster.default_VKWALL_GRAB_COMMENTS_VIDEO);
        vkwall_comment_grablink = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_GRAB_COMMENTS_URLS, VKontakteRuHoster.default_VKWALL_GRAB_COMMENTS_URLS);
        vkwall_comments_grab_comments = vkwall_comment_grabphotos || vkwall_comment_grabaudio || vkwall_comment_grabvideo || vkwall_comment_grablink;
        photos_store_picture_directurls = cfg.getBooleanProperty(VKontakteRuHoster.VKWALL_STORE_PICTURE_DIRECTURLS, VKontakteRuHoster.default_VKWALL_STORE_PICTURE_DIRECTURLS);
        prepBrowser(br);
        /* First handle URLs that can be processed without requiring any http request */
        final String ownerIDFromSlashPublicLink = new Regex(param.getCryptedUrl(), PATTERN_PUBLIC_LINK).getMatch(0);
        final String ownerIDFromSlashEventLink = new Regex(param.getCryptedUrl(), PATTERN_EVENT_LINK).getMatch(0);
        if (ownerIDFromSlashPublicLink != null) {
            /* Change URL and let it run back through this crawler plugin again. */
            ret.add(this.createDownloadlink("https://" + this.getHost() + "/wall" + ownerIDFromSlashPublicLink));
            return ret;
        } else if (ownerIDFromSlashEventLink != null) {
            ret.add(this.createDownloadlink("https://" + this.getHost() + "/wall-" + ownerIDFromSlashEventLink));
            return ret;
        } else if (this.isClubUrl(param.getCryptedUrl())) {
            ret.add(this.createDownloadlink("https://" + this.getHost() + "/wall-" + new Regex(param.getCryptedUrl(), PATTERN_CLUB_LINK).getMatch(0)));
            return ret;
        } else if (param.getCryptedUrl().matches(PATTERN_URL_EXTERN)) {
            /*  */
            final String finallink = UrlQuery.parse(param.getCryptedUrl()).get("to");
            ret.add(createDownloadlink(finallink));
            return ret;
        } else if (isDocument(param.getCryptedUrl())) {
            /* Pass to host plugin */
            ret.add(createDownloadlink(param.getCryptedUrl()));
            return ret;
        } else if (isSinglePicture(param.getCryptedUrl())) {
            /* Pass to host plugin */
            ret.add(processSinglePicture(param));
            return ret;
        }
        /* Now process all items that require http requests to be able to return results. */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        login(acc, this.br);
        if (needsAccount(param.getCryptedUrl()) && acc == null) {
            throw new AccountRequiredException();
        }
        /* Decryption process START */
        if (param.getCryptedUrl().matches(PATTERN_AUDIO_AUDIOS_ALBUM_2020)) {
            return decryptAudiosAlbum2020(param);
        } else if (param.getCryptedUrl().matches(PATTERN_GENERAL_AUDIO)) {
            if (param.getCryptedUrl().matches(PATTERN_AUDIO_ALBUM)) {
                /* Audio album */
                return decryptAudioAlbumAndSection(param);
            } else if (param.getCryptedUrl().matches(PATTERN_AUDIO_AUDIOS_ALBUM)) {
                return decryptAudiosAlbum(param);
            } else {
                /* Single playlists */
                return decryptAudioPlaylist(param);
            }
        } else if (param.getCryptedUrl().matches(PATTERN_AUDIO_PAGE) || param.getCryptedUrl().matches(PATTERN_AUDIO_PAGE_oid)) {
            /* Audio page */
            return crawlAudioPage(param);
        } else if (isSingleVideo(param.getCryptedUrl())) {
            /* Single video */
            return crawlSingleVideo(param);
        } else if (isVideoAlbum(param.getCryptedUrl())) {
            /* Video album */
            return crawlVideoAlbum(param);
        } else if (param.getCryptedUrl().matches(PATTERN_PHOTO_ALBUM)) {
            /**
             * Photo album Examples: http://vk.com/photos575934598 http://vk.com/id28426816 http://vk.com/album87171972_0
             */
            return crawlPhotoAlbumWebsite(param);
        } else if (param.getCryptedUrl().matches(PATTERN_PHOTO_ALBUMS) || param.getCryptedUrl().matches(PATTERN_ID_LINK)) {
            /**
             * Photo albums lists/overviews Example: http://vk.com/albums46486585
             */
            return crawlPhotoAlbums_Website(param);
        } else if (isSingleWallPost(param.getCryptedUrl())) {
            /**
             * Single posts of wall links: https://vk.com/wall-28122291_906
             */
            return crawlWallPost(param);
        } else if (param.getCryptedUrl().matches(PATTERN_DOCS)) {
            return crawlDocs(param);
        } else if (param.getCryptedUrl().matches(PATTERN_WALL_CLIPS)) {
            return crawlWallClips(param);
        } else if (isUserStory(param.getCryptedUrl())) {
            return this.crawlUserStory(param);
        } else {
            /* Wall link or unsupported link! */
            return crawlWallLink(param);
        }
        // return null;
    }

    /** @return true: Account is known to be required for given URL. */
    private static boolean needsAccount(final String url) {
        if (isUserStory(url)) {
            return true;
        } else {
            return false;
        }
    }

    /** Checks if the type of a link is clear, meaning we're sure we have no vk.com/username link if this is returns true. */
    private static boolean isKnownType(final String url) {
        if (isVideoAlbum(url)) {
            return true;
        } else if (isSingleVideo(url)) {
            return true;
        } else if (isSinglePicture(url)) {
            return true;
        } else if (isUserStory(url)) {
            return true;
        } else {
            if (url.matches(PATTERN_VIDEO_SINGLE_Z) || url.matches(PATTERN_PHOTO_ALBUM) || url.matches(PATTERN_PHOTO_ALBUMS) || url.matches(PATTERN_AUDIO_PAGE) || url.matches(PATTERN_GENERAL_WALL_LINK) || url.matches(PATTERN_GENERAL_AUDIO) || url.matches(PATTERN_WALL_POST_LINK) || url.matches(PATTERN_AUDIO_PAGE_oid) || url.matches(PATTERN_DOCS)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean isClubUrl(final String url) {
        if (new Regex(url, PATTERN_CLUB_LINK).patternFind() && !isSinglePicture(url) && !isSingleVideo(url)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> decryptAudioAlbumAndSection(final CryptedLink param) throws Exception {
        final String owner_ID = new Regex(param.getCryptedUrl(), "((?:\\-)?\\d+)$").getMatch(0);
        final String album_id = UrlQuery.parse(param.getCryptedUrl()).get("album_id");
        final String section = UrlQuery.parse(param.getCryptedUrl()).get("section");
        if (StringUtils.equalsIgnoreCase(section, "playlists")) {
            /* Overview of playlists */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            this.siteGeneralErrorhandling(br);
            final String[] playlistURLs = br.getRegex("(/music/playlist/[^<>\"\\']+)").getColumn(0);
            if (playlistURLs == null || playlistURLs.length == 0) {
                logger.info("Nothing found --> Probably offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Found playlists: " + playlistURLs.length);
            for (final String playlistURL : playlistURLs) {
                ret.add(this.createDownloadlink(br.getURL(playlistURL).toString()));
            }
            return ret;
        } else {
            /* Single playlist */
            String fpName = null;
            if (cfg.getBooleanProperty(VKontakteRuHoster.VKAUDIOS_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKAUDIOS_USEIDASPACKAGENAME)) {
                fpName = "audios" + owner_ID;
            }
            final String postData;
            if (album_id != null) {
                postData = "act=load_silent&al=1&album_id=" + album_id + "&band=false&owner_id=" + owner_ID;
            } else {
                postData = "access_hash=&act=load_section&al=1&claim=0&offset=0&is_loading_all=1&owner_id=" + owner_ID + "&playlist_id=-1&type=playlist";
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(getBaseURL() + "/al_audio.php", postData);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            if (StringUtils.isEmpty(fpName)) {
                fpName = (String) entries.get("title");
            }
            if (StringUtils.isEmpty(fpName)) {
                /* Last chance fallback */
                fpName = "vk.com audio - " + owner_ID;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            /* TODO: 2020-05-11: Fix this */
            final List<Object> audioData = (List<Object>) entries.get("payload");
            if (audioData == null || audioData.size() == 0) {
                logger.info("Nothing found --> Probably offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return addJsonAudioObjects(param, audioData, fp);
        }
    }

    /** NOT using API audio pages and audio playlist's are similar, TODO: Return host-plugin links here to improve the overall stability. */
    private ArrayList<DownloadLink> decryptAudiosAlbum(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        if (br.containsHTML("id=\"not_found\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String audiotabletext = br.getRegex("<table class=\"audio_table\" cellspacing=\"0\" cellpadding=\"0\">(.*?)</table>").getMatch(0);
        final String owner_ID = new Regex(param.getCryptedUrl(), "audios((?:\\-)?\\d+)").getMatch(0);
        final String albumID = UrlQuery.parse(param.getCryptedUrl()).get("album_id");
        final String fpName;
        if (cfg.getBooleanProperty(VKontakteRuHoster.VKAUDIOS_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKAUDIOS_USEIDASPACKAGENAME)) {
            fpName = "audios" + owner_ID;
        } else {
            fpName = "audios_album " + albumID;
        }
        FilePackage fp = null;
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        final String[] audioinfo = new Regex(audiotabletext, "class=\"audio  no_actions fl_l\"(.*?)class=\"duration fl_r\"").getColumn(0);
        if (audioinfo == null || audioinfo.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        for (String audnfo : audioinfo) {
            final String artist = new Regex(audnfo, "event: event, name: '([^<>\"]*?)'").getMatch(0);
            // final String title = new Regex(audnfo, "return cancelEvent\\(event\\);\">([^<>\"]*?)</a>").getMatch(0);
            final String title = new Regex(audnfo, "class=\"title\">([^<>\"]*?)<").getMatch(0);
            final Regex idinfo = new Regex(audnfo, "id=\"play\\-(\\d+)_(\\d+)\"");
            final String owner_id = idinfo.getMatch(0);
            final String content_id = idinfo.getMatch(1);
            final String finallink = new Regex(audnfo, "\"(https?://cs[a-z0-9]+\\.(vk\\.com|userapi\\.com|vk\\.me)/u\\d+/audios?/[^<>\"/]+)\"").getMatch(0);
            if (finallink == null || artist == null || title == null || owner_id == null || content_id == null) {
                logger.info("artist: " + artist + ", title: " + title + ", owner_id: " + owner_id + ", content_id: " + content_id + ", finallink: " + finallink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String linkid = owner_id + "_" + content_id;
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + linkid);
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_mainlink, param.getCryptedUrl());
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(artist) + " - " + Encoding.htmlDecode(title) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            fp.add(dl);
            /*
             * Audiolinks have their directlinks and IDs but no "nice" links so let's simply use the link to the album to display to the
             * user.
             */
            dl.setContentUrl(param.getCryptedUrl());
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_directlink, finallink);
            fp.add(dl);
            ret.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        return ret;
    }

    private ArrayList<DownloadLink> decryptAudiosAlbum2020(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String owner_ID = new Regex(param.getCryptedUrl(), PATTERN_AUDIO_AUDIOS_ALBUM_2020).getMatch(0);
        final String albumID = new Regex(param.getCryptedUrl(), PATTERN_AUDIO_AUDIOS_ALBUM_2020).getMatch(1);
        final String fpName;
        if (cfg.getBooleanProperty(VKontakteRuHoster.VKAUDIOS_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKAUDIOS_USEIDASPACKAGENAME)) {
            fpName = "audios" + owner_ID;
        } else {
            fpName = "audios_album " + albumID;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final String json = br.getRegex("new PlayList\\(\\d+,\\s*(\\{.*?)\\);").getMatch(0);
        if (json == null) {
            /* Fallback */
            ret.addAll(websiteCrawlContent(param.getCryptedUrl(), br.toString(), fp, true, false, false, false, false, false));
            logger.info("Found " + ret.size() + " items");
        } else {
            Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
            final List<Object> audiolist = (List<Object>) entries.get("list");
            ret.addAll(addJsonAudioObjects(param, audiolist, fp));
            logger.info("Found " + ret.size() + " items");
        }
        return ret;
    }

    /** NOT using API audio pages and audio playlist's are similar. */
    private ArrayList<DownloadLink> decryptAudioPlaylist(final CryptedLink param) throws Exception {
        FilePackage fp = FilePackage.getInstance();
        final String owner_ID = new Regex(param.getCryptedUrl(), "((?:\\-)?\\d+)_\\d+$").getMatch(0);
        final String playlist_id = new Regex(param.getCryptedUrl(), "_(\\d+)$").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(getBaseURL() + "/al_audio.php", "access_hash=&act=load_section&al=1&claim=0&is_loading_all=1&offset=0&owner_id=" + owner_ID + "&playlist_id=-1&type=playlist");
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(regexJsonInsideHTML(this.br));
        String fpName = (String) entries.get("title");
        if (StringUtils.isEmpty(fpName)) {
            /* Last chance fallback */
            fpName = owner_ID + "_" + playlist_id;
        }
        /* Important! Even inside json, they html_encode some Strings! */
        fpName = Encoding.htmlDecode(fpName.trim());
        fp.setName(fpName);
        final List<Object> audioData = (List<Object>) entries.get("list");
        if (audioData == null || audioData.size() == 0) {
            logger.info("Nothing found --> Probably offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return addJsonAudioObjects(param, audioData, fp);
    }

    /**
     * Creates DownloadLink Objects out of Objects of vk Audio Albums / Playlists after request of '/al_audio.php'
     *
     * @throws PluginException
     */
    private ArrayList<DownloadLink> addJsonAudioObjects(final CryptedLink param, final List<Object> audioData, final FilePackage fp) throws PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Object audioDataSingle : audioData) {
            if (!(audioDataSingle instanceof List)) {
                continue;
            }
            final List<Object> singleAudioDataAsArray = (List<Object>) audioDataSingle;
            final String content_id = Long.toString(JavaScriptEngineFactory.toLong(singleAudioDataAsArray.get(0), 0));
            final String owner_id = Long.toString(JavaScriptEngineFactory.toLong(singleAudioDataAsArray.get(1), 0));
            final String directlink = (String) singleAudioDataAsArray.get(2);
            final String title = (String) singleAudioDataAsArray.get(3);
            final String artist = (String) singleAudioDataAsArray.get(4);
            String specialKeys = null;
            try {
                specialKeys = (String) singleAudioDataAsArray.get(13);
            } catch (final Throwable e) {
            }
            if (owner_id == null || owner_id.equals("0") || content_id == null || content_id.equals("0") || artist == null || artist.equals("") || title == null || title.equals("")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + owner_id + "_" + content_id);
            dl.setContentUrl(param.getCryptedUrl());
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_mainlink, param.getCryptedUrl());
            if (directlink != null && directlink.startsWith("http")) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_directlink, directlink);
            }
            if (!StringUtils.isEmpty(specialKeys)) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_AUDIO_special_id, specialKeys);
            }
            dl.setFinalFileName(Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(title.trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            ret.add(dl);
        }
        return ret;
    }

    /** Returns json inside html inside '<!json>'-tag, typically after request '/al_audio.php' */
    public static String regexJsonInsideHTML(final Browser br) {
        return br.getRegex("<\\!json>(.*?)<\\!>").getMatch(0);
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlAudioPage(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        if (br.containsHTML("Page not found") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (param.getCryptedUrl().matches(PATTERN_AUDIO_PAGE_oid) && fpName == null) {
            fpName = Encoding.htmlDecode(new Regex(param.getCryptedUrl(), "(?i)\\&p=(.+)").getMatch(0));
        } else if (fpName == null) {
            final String pageID = new Regex(param.getCryptedUrl(), "(?i)/page-(\\d+_\\d+)").getMatch(0);
            fpName = "vk.com page " + pageID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        final String[][] audioLinks = br.getRegex("\"(https?://[a-z0-9]+\\.(vk\\.com|userapi\\.com|vk\\.me)/[^<>\"]+/audio[^<>\"]*?)\".*?onclick=\"return nav\\.go\\(this, event\\);\">([^<>\"]*?)</a></b> \\&ndash; <span class=\"title\" id=\"title(?:\\-)?\\d+_\\d+_\\d+\">([^<>\"]*?)</span>").getMatches();
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        if (audioLinks != null && audioLinks.length > 0) {
            for (String audioInfo[] : audioLinks) {
                String finallink = audioInfo[0];
                if (finallink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(finallink));
                dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_mainlink, param.getCryptedUrl());
                /* Set filename so we have nice filenames for our directhttp links */
                dl.setFinalFileName(Encoding.htmlDecode(audioInfo[2].trim()) + " - " + Encoding.htmlDecode(audioInfo[3].trim()) + ".mp3");
                if (fastcheck_audio) {
                    dl.setAvailable(true);
                }
                ret.add(dl);
            }
        }
        /* 2023-04-21: Crawl external URLs and video URLs. Ignore everything else. */
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            final String host = Browser.getHost(url);
            if (isSingleVideo(url)) {
                /* vk video url */
                ret.add(this.createDownloadlink(url));
            } else if (host.equals(br.getHost()) && !this.canHandle(url)) {
                /* External URL */
                ret.add(this.createDownloadlink(url));
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.addLinks(ret);
        return ret;
    }

    private final boolean containsErrorTitle(final Browser br) {
        if (br.containsHTML("(?i)<div class=\"message_page_title\"[^>]*>Error</div>")) {
            return true;
        } else {
            return false;
        }
    }

    private QualitySelectionMode mode      = null;
    private Boolean              preferHLS = null;

    public void setQualitySelectionMode(QualitySelectionMode mode) {
        this.mode = mode;
    }

    public void setPreferHLS(final Boolean preferHLS) {
        this.preferHLS = preferHLS;
    }

    public QualitySelectionMode getQualitySelectionMode() {
        if (mode != null) {
            return mode;
        } else {
            return VKontakteRuHoster.getSelectedVideoQualitySelectionMode();
        }
    }

    public Boolean getPreferHLS() {
        if (this.preferHLS != null) {
            return this.preferHLS;
        } else {
            return VKontakteRuHoster.getConfigPreferHLS();
        }
    }

    private String preferredQualityString = null;

    public String getPreferredQualityString() {
        if (preferredQualityString != null) {
            return preferredQualityString;
        } else {
            return VKontakteRuHoster.getPreferredQualityString();
        }
    }

    public void setPreferredQualityString(String preferredQualityString) {
        this.preferredQualityString = preferredQualityString;
    }

    private ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        final String[] ids = findVideoIDs(param.getCryptedUrl());
        final String oid = ids[0];
        final String id = ids[1];
        final String oid_and_id = oid + "_" + id;
        String module = null;
        String listID;
        if (param.getCryptedUrl().matches(PATTERN_VIDEO_SINGLE_Z)) {
            listID = new Regex(param.getCryptedUrl(), "z=video-?\\d+_\\d+(?:%2F|/)([A-Za-z0-9\\-_]+)").getMatch(0);
        } else {
            listID = UrlQuery.parse(param.getCryptedUrl()).get("list");
        }
        if (param.getDownloadLink() != null) {
            if (listID == null) {
                listID = param.getDownloadLink().getStringProperty(VKontakteRuHoster.PROPERTY_VIDEO_LIST_ID);
            }
            module = param.getDownloadLink().getStringProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module);
        }
        if (module == null) {
            module = "public";
        }
        /* Check if fast-crawl is allowed */
        final QualitySelectionMode qualitySelectionMode = getQualitySelectionMode();
        final boolean userWantsMultipleQualities = qualitySelectionMode == QualitySelectionMode.ALL;
        final boolean linkCanBeFastCrawled = param.getDownloadLink() != null && !param.getDownloadLink().hasProperty(VIDEO_PROHIBIT_FASTCRAWL) && param.getDownloadLink().hasProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN);
        if (this.cfg.getBooleanProperty(VKontakteRuHoster.FASTCRAWL_VIDEO, VKontakteRuHoster.default_FASTCRAWL_VIDEO) && !userWantsMultipleQualities && linkCanBeFastCrawled) {
            final DownloadLink dl = this.createDownloadlink(param.getDownloadLink().getPluginPatternMatcher());
            /* Inherit all previously set properties */
            dl.setProperties(param.getDownloadLink().getProperties());
            dl.setFinalFileName(param.getDownloadLink().getStringProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN) + "_fastcrawl.mp4");
            dl.setAvailable(true);
            ret.add(dl);
            return ret;
        }
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(PATTERN_VIDEO_SINGLE_Z) && listID == null) {
            /**
             * 2022-01-07: Special: Extra request of original URL in beforehand required to find listID and/or set required cookies!
             */
            getPage(param.getCryptedUrl());
            final String thisListID = br.getRegex("\\?z=video" + oid_and_id + "%2F([a-f0-9]+)\"").getMatch(0);
            final UrlQuery query = new UrlQuery();
            query.add("act", "show");
            query.add("al", "1");
            query.add("autoplay", "0");
            if (thisListID != null) {
                /* Should always be given! */
                query.add("list", thisListID);
            }
            query.add("module", "");
            query.add("video", oid_and_id);
            br.getHeaders().put("Origin", "https://" + this.getHost());
            br.getHeaders().put("Referer", param.getCryptedUrl());
            this.getPage(br, br.createPostRequest("/al_video.php?act=show", query));
        } else if (listID != null) {
            final UrlQuery query = new UrlQuery();
            query.add("act", "show");
            query.add("al", "1");
            query.add("claim", "");
            query.add("dmcah", "");
            query.add("hd", "");
            query.add("list", listID);
            query.add("module", module);
            // query.add("playlist_id", "-12345678_-2");
            query.add("show_original", "");
            query.add("t", "");
            query.add("video", oid_and_id);
            br.getHeaders().put("Origin", "https://" + this.getHost());
            br.getHeaders().put("Referer", param.getCryptedUrl());
            // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.getPage(br, br.createPostRequest(getProtocol() + "vk.com/al_video.php?act=show", query));
        } else {
            getPage(getProtocol() + "vk.com/video" + oid_and_id);
        }
        String embeddedVideoURL = new Regex(PluginJSonUtils.unescape(br.toString()), "<iframe [^>]*src=('|\")(.*?)\\1").getMatch(1);
        if (embeddedVideoURL != null) {
            logger.info("Found embedded video: " + embeddedVideoURL);
            if (embeddedVideoURL.startsWith("//")) {
                embeddedVideoURL = "https:" + embeddedVideoURL;
            }
            ret.add(createDownloadlink(embeddedVideoURL));
            return ret;
        }
        final Map<String, Object> video = findVideoMap(this.br, id);
        if (video == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, this.getMessageBodyText(br));
        }
        final String embedHash = (String) video.get("embed_hash");
        if (embedHash == null) {
            logger.info("Video seems to be offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String author = (String) video.get("md_author");
        if (!StringUtils.isEmpty(author)) {
            author = Encoding.htmlDecode(author).trim();
        }
        final long date = ((Number) video.get("date")).longValue();
        final SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
        final String dateFormatted = sd.format(date * 1000);
        String titleToUse = null;
        String titlePlain = (String) video.get("md_title");
        if (!StringUtils.isEmpty(titlePlain)) {
            titlePlain = Encoding.htmlDecode(titlePlain).trim();
            titleToUse = titlePlain;
            if (!StringUtils.isEmpty(author) && !author.equalsIgnoreCase("DELETED") && this.cfg.getBooleanProperty(VKontakteRuHoster.VIDEO_ADD_NAME_OF_UPLOADER_TO_FILENAME, VKontakteRuHoster.default_VIDEO_ADD_NAME_OF_UPLOADER_TO_FILENAME)) {
                titleToUse = author + "_" + titleToUse + "_" + oid_and_id;
            } else {
                titleToUse = titleToUse + "_" + oid_and_id;
            }
        }
        /* Find needed information */
        final Map<String, String> foundQualities = new LinkedHashMap<String, String>();
        final String http_url = (String) video.get("postlive_mp4");
        final String http_quality = http_url != null ? new Regex(http_url, "(\\d+)\\.mp4").getMatch(0) : null;
        if (http_url != null && http_quality != null) {
            foundQualities.put(http_quality + "p", http_url);
        }
        /* Use cachexxx as workaround e.g. for special videos that need groups permission. */
        final String[][] knownQualities = { { "cache2160", "url2160", "2160p" }, { "cache1440", "url1440", "1440p" }, { "cache1080", "url1080", "1080p" }, { "cache720", "url720", "720p" }, { "cache480", "url480", "480p" }, { "cache360", "url360", "360p" }, { "cache240", "url240", "240p" }, { "cache144", "url144", "144p" } };
        for (final String[] qualityInfo : knownQualities) {
            String finallink = (String) video.get(qualityInfo[0]);
            if (finallink == null) {
                finallink = (String) video.get(qualityInfo[1]);
            }
            if (finallink != null && !foundQualities.containsKey(qualityInfo[2])) {
                foundQualities.put(qualityInfo[2], finallink);
            }
        }
        final String manifest = (String) video.get("manifest");
        if (manifest != null) {
            // mpd doesn't use split video/audio, 2021-04-19
            String representations[] = new Regex(manifest, "(<Representation.*?</Representation>)").getColumn(0);
            for (final String[] qualityInfo : knownQualities) {
                if (!foundQualities.containsKey(qualityInfo[2])) {
                    for (final String representation : representations) {
                        final int height = Integer.parseInt(qualityInfo[2].replaceAll("[^\\d]", ""));
                        if (representation.contains("height=\"" + height + "\"")) {
                            String url = new Regex(representation, "<BaseURL>\\s*(.*?)\\s*</BaseURL>").getMatch(0);
                            if (url != null) {
                                url = Encoding.htmlOnlyDecode(url);
                                foundQualities.put(qualityInfo[2], url);
                            }
                        }
                    }
                }
            }
        }
        boolean crawlHLS = false;
        final String hls_master = (String) video.get("hls");
        List<HlsContainer> hlsQualities = null;
        if (!StringUtils.isEmpty(hls_master)) {
            if (foundQualities.isEmpty()) {
                /* HLS as fallback */
                crawlHLS = true;
            } else if (this.getPreferHLS()) {
                foundQualities.clear();
                crawlHLS = true;
            }
            if (crawlHLS) {
                logger.info("Crawling HLS qualities");
                final Browser brc = br.cloneBrowser();
                brc.getPage(hls_master);
                hlsQualities = HlsContainer.getHlsQualities(brc);
                /* Workaround to make lower handling work: Put best item to first position. */
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(hlsQualities);
                final List<HlsContainer> hlsQualitiesBestFirst = new ArrayList<HlsContainer>();
                hlsQualitiesBestFirst.add(hlsbest);
                for (final HlsContainer hlsQuality : hlsQualities) {
                    if (!hlsQualitiesBestFirst.contains(hlsQuality)) {
                        hlsQualitiesBestFirst.add(hlsQuality);
                    }
                }
                for (final HlsContainer hlsQuality : hlsQualitiesBestFirst) {
                    final int correctedHeightValue = getHeightByWidth(hlsQuality.getWidth());
                    if (correctedHeightValue != -1) {
                        /* Small ugly workaround e.g. for 1280x718 --> 720p */
                        foundQualities.put(correctedHeightValue + "p", hlsQuality.getDownloadurl());
                    } else {
                        foundQualities.put(hlsQuality.getHeight() + "p", hlsQuality.getDownloadurl());
                    }
                }
            }
        }
        if (foundQualities.isEmpty()) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // create sorted linkedHashMap with best->worse quality sort order
        final Map<String, String> foundAndSortedQualities = new LinkedHashMap<String, String>();
        for (final String[] qualityInfo : knownQualities) {
            final String qualityHeightStr = qualityInfo[2];
            final String url = foundQualities.get(qualityHeightStr);
            if (url != null) {
                foundAndSortedQualities.put(qualityHeightStr, url);
            }
        }
        if (foundAndSortedQualities.isEmpty()) {
            logger.info("Quality sort failed -> Found only unknown qualities -> Adding them unsorted");
            foundAndSortedQualities.putAll(foundQualities);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (cfg.getBooleanProperty(VKontakteRuHoster.VKVIDEO_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKVIDEO_USEIDASPACKAGENAME)) {
            fp.setName("video" + oid + "_" + id);
        } else {
            fp.setName(titleToUse);
        }
        final Map<String, String> selectedQualities = getSelectedVideoQualities(foundAndSortedQualities, qualitySelectionMode, getPreferredQualityString());
        if (selectedQualities.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.PLUGIN_SETTINGS, "BAD_QUALITY_SELECTION_PLUGIN_SETTINGS_" + titleToUse, "You've selected only qualities which do not exist for this video. Adjust your plugin settings and try again.");
        }
        final boolean fastLinkcheck = cfg.getBooleanProperty(VKontakteRuHoster.FASTLINKCHECK_VIDEO, true);
        final PluginForHost plugin = getNewPluginForHostInstance(getHost());
        for (final Map.Entry<String, String> qualityEntry : selectedQualities.entrySet()) {
            final String thisQuality = qualityEntry.getKey();
            final String finallink = qualityEntry.getValue();
            final String contentURL = getProtocol() + this.getHost() + "/video" + oid_and_id + "#quality=" + thisQuality;
            /*
             * plugin instance is required to avoid being picked up by this plugin again because hoster and decrypter plugin do match on
             * same URL pattern
             */
            final DownloadLink dl = new DownloadLink(plugin, null, this.getHost(), contentURL, true);
            if (param.getDownloadLink() != null) {
                /* Inherit all properties from previous DownloadLink */
                dl.setProperties(param.getDownloadLink().getProperties());
            }
            final String finalfilename = titleToUse + "_" + thisQuality + ".mp4";
            dl.setFinalFileName(finalfilename);
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_directlink, finallink);
            dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_SELECTED_QUALITY, thisQuality);
            if (listID != null) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_LIST_ID, listID);
            }
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN, titlePlain);
            dl.setProperty(VKontakteRuHoster.VIDEO_QUALITY_SELECTION_MODE, cfg.getIntegerProperty(VKontakteRuHoster.VIDEO_QUALITY_SELECTION_MODE, VKontakteRuHoster.default_VIDEO_QUALITY_SELECTION_MODE));
            dl.setProperty(VKontakteRuHoster.PREFERRED_VIDEO_QUALITY, cfg.getIntegerProperty(VKontakteRuHoster.PREFERRED_VIDEO_QUALITY, VKontakteRuHoster.default_PREFERRED_VIDEO_QUALITY));
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_DATE, dateFormatted);
            if (!StringUtils.isEmpty(author)) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_UPLOADER, author);
            }
            if (crawlHLS) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_STREAM_TYPE, VKontakteRuHoster.VIDEO_STREAM_TYPE_HLS);
            } else {
                dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_STREAM_TYPE, VKontakteRuHoster.VIDEO_STREAM_TYPE_HTTP);
            }
            if (fastLinkcheck) {
                dl.setAvailable(true);
            }
            dl._setFilePackage(fp);
            ret.add(dl);
        }
        if (ret.isEmpty()) {
            logger.warning("Single video crawler is returning empty array");
        }
        return ret;
    }

    private int getHeightByWidth(final int width) {
        switch (width) {
        default:
            return -1;
        case 640:
            return 480;
        case 1280:
            return 720;
        case 1920:
            return 1080;
        }
    }

    private int getWidthByHeight(final int height) {
        switch (height) {
        default:
            return -1;
        case 480:
            return 640;
        case 720:
            return 1280;
        case 1080:
            return 1920;
        }
    }

    private static Map<String, Object> findVideoMap(final Browser br, final String videoid) throws Exception {
        String json = br.getRegex("ajax\\.preload\\('al_video\\.php', \\{.*?\\}, (\\[.*?\\])\\);\\n").getMatch(0);
        if (json == null) {
            /* E.g. information has been loaded via ajax request e.g. as part of a wall post/playlist */
            json = br.getRegex("^<\\!--(\\{.+\\})$").getMatch(0);
        }
        if (json == null) {
            return null;
        } else {
            return (Map<String, Object>) recursiveFindVideoMap(JavaScriptEngineFactory.jsonToJavaObject(json), videoid);
        }
    }

    public static Object recursiveFindVideoMap(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> cookieEntry : entrymap.entrySet()) {
                final String key = cookieEntry.getKey();
                final Object value = cookieEntry.getValue();
                if (key.equals("vid") && value != null && value.toString().equals(videoid) && entrymap.containsKey("hd")) {
                    return o;
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = recursiveFindVideoMap(value, videoid);
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
                    final Object pico = recursiveFindVideoMap(arrayo, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private String getMessageBodyText(final Browser br) {
        final String msgBody = br.getRegex("<div class=\"message_page_body\">([^<]+)<").getMatch(0);
        if (msgBody != null) {
            return Encoding.htmlDecode(msgBody).trim();
        } else {
            return null;
        }
    }

    private String[] findVideoIDs(final String parameter) {
        final String[] ids = new String[2];
        String ownerID = null;
        String contentID = null;
        if (parameter.matches(PATTERN_VIDEO_SINGLE_EMBED) || parameter.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH)) {
            final Regex idsRegex = new Regex(parameter, "vk\\.com/video_ext\\.php\\?oid=((?:\\-)?\\d+)\\&id=(\\d+)");
            ownerID = idsRegex.getMatch(0);
            contentID = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL) || parameter.matches(PATTERN_CLIP_SINGLE_ORIGINAL) || parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_WITH_LISTID)) {
            final Regex idsRegex = new Regex(parameter, "((?:\\-)?\\d+)_(\\d+)");
            ownerID = idsRegex.getMatch(0);
            contentID = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
            final Regex idsRegex = new Regex(parameter, "((?:\\-)?\\d+)_(\\d+)\\?");
            ownerID = idsRegex.getMatch(0);
            contentID = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_Z)) {
            final Regex idsRegex = new Regex(parameter, "z=video((?:\\-)?\\d+)_(\\d+)");
            ownerID = idsRegex.getMatch(0);
            contentID = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_CLIP_SINGLE_Z)) {
            final Regex idsRegex = new Regex(parameter, "z=clip((?:\\-)?\\d+)_(\\d+)");
            ownerID = idsRegex.getMatch(0);
            contentID = idsRegex.getMatch(1);
        }
        ids[0] = ownerID;
        ids[1] = contentID;
        return ids;
    }

    private ArrayList<DownloadLink> crawlPhotoAlbumWebsite(final CryptedLink param) throws Exception {
        final String contenturl;
        if (StringUtils.contains(param.getCryptedUrl(), "#/album")) {
            contenturl = getProtocol() + "vk.com/album" + new Regex(param.getCryptedUrl(), "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        } else if (param.getCryptedUrl().matches("(?i).*?vk\\.com/id(?:\\-)?\\d+")) {
            contenturl = param.getCryptedUrl().replaceAll("(?i)vk\\.com/|id(?:\\-)?", "vk.com/album") + "_0";
        } else {
            contenturl = this.fixAddedURL(param.getCryptedUrl());
        }
        this.getPage(contenturl);
        if (br.containsHTML("(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)") || br.containsHTML("(?i)В альбоме нет фотографий|<title>\\s*DELETED\\s*</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String numberOfItemsStr = br.getRegex("\\| (\\d+) zdj&#281").getMatch(0);
        if (numberOfItemsStr == null) {
            numberOfItemsStr = br.getRegex("count\\s*:\\s*(\\d+),").getMatch(0);
            if (numberOfItemsStr == null) {
                numberOfItemsStr = br.getRegex("</a>(\\d+) zdj\\&#281;\\&#263;<span").getMatch(0);
                if (numberOfItemsStr == null) {
                    numberOfItemsStr = br.getRegex(">(\\d+) photos in the album<").getMatch(0);
                }
            }
        }
        String albumsJson = br.getRegex("extend\\(cur, (\\{\".*?\\})\\);\\s+").getMatch(0);
        if (albumsJson == null) {
            /* 2023-11-17 */
            albumsJson = br.getRegex("extend\\(window\\.cur \\|\\| \\{\\}, (\\{\".*?\\})\\);").getMatch(0);
        }
        final Map<String, Object> albumInfo = restoreFromString(albumsJson, TypeRef.MAP);
        if (numberOfItemsStr != null && numberOfItemsStr.equals("0")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        final String startOffset = albumInfo != null ? (String) albumInfo.get("offset") : null;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(contenturl, "(-?(\\d+_)?\\d+)$").getMatch(0));
        fp.setCleanupPackageName(false);
        final int maxItemsPerPage = 40;
        int page = 1;
        int offset = 0;
        if (startOffset != null) {
            logger.info("Starting with offset: " + startOffset);
            offset = Integer.parseInt(startOffset);
        }
        offset = 40;
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        do {
            final int linksnumBefore = ret.size();
            ret.addAll(websiteCrawlContent(br.getURL(), br.toString(), fp, false, false, true, false, false, this.photos_store_picture_directurls));
            logger.info("Crawled page " + page + " | Offset: " + offset + " | Found items so far: " + ret.size() + "/" + numberOfItemsStr);
            final int linksnumAfter = ret.size();
            final int addedLinks = linksnumAfter - linksnumBefore;
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (addedLinks < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than max per page: " + addedLinks + "/" + maxItemsPerPage);
                break;
            } else {
                page++;
                offset += maxItemsPerPage;
                sleep(getPaginationSleepMillis(), param);
                this.getPage(br, br.createPostRequest(br.getURL(), "al=1&al_ad=0&part=1&rev=&offset=" + offset));
            }
        } while (true);
        return ret;
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID, final String picture_preview_json) throws IOException {
        final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + photoID);
        if (fastcheck_photo) {
            dl.setAvailable(true);
        }
        final String dllink_temp = VKontakteRuHoster.getHighestQualityPicFromSavedJson(picture_preview_json);
        final String tempFilename = VKontakteRuHoster.photoGetFinalFilename(photoID, null, dllink_temp);
        if (tempFilename != null) {
            dl.setName(tempFilename);
        }
        dl.setContentUrl(getProtocol() + "vk.com/photo" + photoID);
        return dl;
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID) throws IOException {
        return getSinglePhotoDownloadLink(photoID, null);
    }

    private ArrayList<DownloadLink> crawlPhotoAlbums_Website(final CryptedLink param) throws NumberFormatException, Exception {
        /*
         * Another possibility to get these (but still no API): https://vk.com/al_photos.php act=show_albums&al=1&owner=<owner_id> AblumsXXX
         * --> XXX may also be the owner_id, depending on linktype.
         */
        final String contenturl;
        final Regex idregex = new Regex(param.getCryptedUrl(), PATTERN_ID_LINK);
        if (idregex.patternFind()) {
            /* Change id links -> albums links */
            contenturl = "https://" + this.getHost() + "/albums" + idregex.getMatch(0);
        } else {
            contenturl = this.fixAddedURL(param.getCryptedUrl());
        }
        getPage(contenturl);
        if (br.containsHTML("class=\"photos_no_content\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String type = "multiplephotoalbums";
        if (contenturl.contains("z=")) {
            /* TODO: Check if this is still needed */
            final String albumID = new Regex(contenturl, PATTERN_PHOTO_ALBUMS).getMatch(0);
            if (albumID != null) {
                final String newURL = getProtocol() + this.getHost() + "/albums" + albumID;
                if (!br.getURL().equals(newURL)) {
                    getPage(newURL);
                }
            }
        } else {
            /* not needed as we already have requested this page */
            // getPage(br,parameter);
        }
        final String albumID = new Regex(contenturl, PATTERN_PHOTO_ALBUMS).getMatch(0);
        if (albumID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * 2020-01-27: Albums will go back into decrypter and each album's contents will go into separate packages with separate
         * packagenames.
         */
        final FilePackage fp = null;
        // fp.setName(albumID);
        String numberOfEntriesStr = br.getRegex("(?:\\||&#8211;|-) (\\d+) albums?</title>").getMatch(0);
        // Language independent
        if (numberOfEntriesStr == null) {
            numberOfEntriesStr = br.getRegex("class=\"summary\">(\\d+)").getMatch(0);
        }
        if (numberOfEntriesStr == null) {
            /* 2020-06-17 */
            numberOfEntriesStr = br.getRegex("(?i)Show all (\\d+) albums").getMatch(0);
        }
        if (numberOfEntriesStr == null) {
            /* 2020-10-26 */
            numberOfEntriesStr = br.getRegex("(?)class=\"ui_crumb\"[^>]*>[^<]+<span class=\"ui_crumb_count\"[^>]*>\\s*([\\d,]+)\\s*</span>").getMatch(0);
        }
        final String startOffset = br.getRegex("var preload\\s*=\\s*\\[(\\d+),\"").getMatch(0);
        if (numberOfEntriesStr != null) {
            numberOfEntriesStr = numberOfEntriesStr.replace(",", "");
        } else {
            logger.warning("Failed to find numberOfEntriesStr");
        }
        final int numberOfEntries = numberOfEntriesStr != null ? (int) StrictMath.ceil((Double.parseDouble(numberOfEntriesStr))) : -1;
        if (numberOfEntries == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int entries_per_page = 26;
        final int entries_alreadyOnPage = 0;
        logger.info("Crawling " + numberOfEntriesStr + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((numberOfEntries - entries_alreadyOnPage) / entries_per_page);
        if (maxLoops < 0) {
            maxLoops = 0;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinksTotal = 0;
        int realOffset = 0;
        if (startOffset != null) {
            realOffset = Integer.parseInt(startOffset);
        }
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        for (int i = 0; i <= maxLoops; i++) {
            int addedLinks = 0;
            final String correctedBR;
            logger.info("Parsing page " + (i + 1) + " of " + (maxLoops + 1));
            correctedBR = br.getRequest().getHtmlCode().replace("\\", "");
            final int linksnumBefore = ret.size();
            /* 2023-11-17: Very ugly I know */
            this.grabPhotoAlbums = true;
            ret.addAll(websiteCrawlContent(br.getURL(), correctedBR, fp, false, false, false, false, false, false));
            final int linksnumAfter = ret.size();
            addedLinks = linksnumAfter - linksnumBefore;
            addedLinksTotal += addedLinks;
            if (startOffset == null || i > 0) {
                realOffset += addedLinks;
            }
            logger.info("Added items from this page: " + addedLinks);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (numberOfEntriesStr == null) {
                logger.warning("Stopping because: Failed to find number of entries");
                break;
            } else if (addedLinks == 0 || addedLinksTotal >= numberOfEntries) {
                logger.info("Stopping because: Fail safe #1 activated, stopping page parsing at page " + (i + 1) + " of " + (maxLoops + 1) + ", returning " + addedLinksTotal + " results");
                break;
            } else {
                sleep(getPaginationSleepMillis(), param);
                this.getPage(br, br.createPostRequest(br.getURL(), "al=1&al_ad=0&part=1&offset=" + Math.max(0, realOffset - 1)));
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlVideoAlbum(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        this.getPage(param.getCryptedUrl());
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String internalSectionName;
        final String sectionNameURL = UrlQuery.parse(br.getURL()).get("section");
        if (sectionNameURL != null) {
            internalSectionName = sectionNameURL;
        } else {
            internalSectionName = "all";
        }
        String videoAlbumsJson = br.getRegex("let newCur\\s*=\\s*(\\{.*?\\});\\s+").getMatch(0);
        if (videoAlbumsJson == null) {
            /* Old regexes/fallback */
            videoAlbumsJson = br.getRegex("extend\\(cur, (\\{\"albumsPreload\".*?\\})\\);\\s+").getMatch(0);
            if (videoAlbumsJson == null) {
                /* Wider RegEx */
                videoAlbumsJson = br.getRegex("extend\\(cur, (\\{\".*?\\})\\);\\s+").getMatch(0);
            }
        }
        if (videoAlbumsJson == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = restoreFromString(videoAlbumsJson, TypeRef.MAP);
        final String oid = Integer.toString(((Number) entries.get("oid")).intValue());
        final int maxItemsPerPage = ((Number) entries.get("VIDEO_SILENT_VIDEOS_CHUNK_SIZE")).intValue();
        Map<String, Object> videoInfoMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "pageVideosList/" + oid + "/" + internalSectionName);
        if (videoInfoMap == null) {
            Map<String, Object> pageVideosList = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "pageVideosList/" + oid);
            if (pageVideosList != null && pageVideosList.size() == 1) {
                final Entry<String, Object> entry = pageVideosList.entrySet().iterator().next();
                internalSectionName = entry.getKey();
                videoInfoMap = (Map<String, Object>) entry.getValue();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final int numberofVideos = ((Number) videoInfoMap.get("count")).intValue();
        logger.info("numberofVideos=" + numberofVideos + "|maxVideosPerPage=" + maxItemsPerPage);
        int offset = 0;
        int page = 0;
        String containerURL = "https://" + this.getHost() + "/videos" + oid;
        if (internalSectionName != null) {
            containerURL += "=section=" + Encoding.urlEncode(internalSectionName);
        }
        final FilePackage fp = FilePackage.getInstance();
        String galleryTitle = br.getRegex("<title>([^>]+) \\| VK</title>").getMatch(0);
        if (cfg.getBooleanProperty(VKontakteRuHoster.VKVIDEO_ALBUM_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKVIDEO_ALBUM_USEIDASPACKAGENAME)) {
            fp.setName("videos" + oid);
        } else if (galleryTitle == null) {
            /* Fallback */
            fp.setName(oid + " - " + internalSectionName);
        } else {
            fp.setName(Encoding.htmlDecode(galleryTitle).trim() + " - " + internalSectionName);
        }
        final LinkedHashSet<Integer> dupes = new LinkedHashSet<Integer>();
        while (true) {
            List<List<Object>> videosO = (List<List<Object>>) videoInfoMap.get("list");
            if (videosO.isEmpty()) {
                logger.info("Stopping because: Current page does not contain any items");
                break;
            }
            boolean foundNewItems = false;
            for (final List<Object> videoInfos : videosO) {
                final int thisOwnerID = ((Number) videoInfos.get(0)).intValue();
                final int thisContentID = ((Number) videoInfos.get(1)).intValue();
                String videoTitle = (String) videoInfos.get(3);
                String uploader = null;
                final String videoAuthorHTML = (String) videoInfos.get(8);
                if (videoAuthorHTML != null) {
                    final String uploaderRaw = new Regex(videoAuthorHTML, "<a href=[^>]*>(.*?)</a>").getMatch(0);
                    if (uploaderRaw != null && !uploaderRaw.equalsIgnoreCase("DELETED")) {
                        uploader = uploaderRaw;
                        videoTitle = uploaderRaw + "_" + videoTitle;
                    }
                }
                if (uploader != null) {
                    videoTitle = uploader + "_" + videoTitle;
                }
                if (dupes.add(thisContentID)) {
                    // /* Fail-safe */
                    // logger.info("Stopping because: Found dupe");
                    // break pagination;
                    foundNewItems = true;
                }
                final String completeVideolink = getProtocol() + this.getHost() + "/video" + thisOwnerID + "_" + thisContentID;
                final DownloadLink dl = createDownloadlink(completeVideolink);
                dl.setContainerUrl(containerURL);
                dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN, Encoding.htmlDecode(videoTitle).trim());
                if (uploader != null) {
                    dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_UPLOADER, uploader);
                }
                if (videoInfos.size() >= 12) {
                    /* Check for external content. Fast-crawling is only possible for stuff hosted on vk.com! */
                    final String externalContentProviderName = (String) videoInfos.get(11);
                    if (!StringUtils.isEmpty(externalContentProviderName)) {
                        /* E.g. "Coub" or "Youtube" */
                        dl.setProperty(VIDEO_PROHIBIT_FASTCRAWL, true);
                    }
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                offset++;
            }
            logger.info("Page: " + page + " | Crawled: " + offset + " / " + numberofVideos);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (!foundNewItems) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (offset >= numberofVideos) {
                logger.info("Stopping because: Found all items");
                break;
            } else {
                final UrlQuery query = new UrlQuery();
                query.add("al", "1");
                query.add("need_albums", "0");
                query.add("offset", Integer.toString(offset));
                query.add("oid", oid);
                // query.add("rowlen", "3");
                if (!StringUtils.isEmpty(internalSectionName)) {
                    query.add("section", Encoding.urlEncode(internalSectionName));
                }
                sleep(getPaginationSleepMillis(), param);
                br.postPage(getBaseURL() + "/al_video.php?act=load_videos_silent", query.toString());
                final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.MAP);
                videoInfoMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(response, "payload/{1}/{0}/" + internalSectionName);
                page += 1;
            }
        }
        logger.info("Total videolinks found: " + offset);
        return ret;
    }

    private static Map<String, String> getSelectedVideoQualities(final Map<String, String> availableVideoQualities, final QualitySelectionMode mode, final String preferredVideoQuality) {
        final Map<String, String> selectedQualities = new HashMap<String, String>();
        // final Map<String, String> fallbackQualities = new HashMap<String, String>();
        final List<String> knownQualities = new ArrayList<String>();
        for (Quality quality : Quality.values()) {
            knownQualities.add(quality.getLabel());
        }
        if (mode == QualitySelectionMode.ALL) {
            selectedQualities.putAll(availableVideoQualities);
            return selectedQualities;
        } else if (mode == QualitySelectionMode.BEST) {
            /* Crawled qualities are pre-sorted from best to worst. */
            final Map.Entry<String, String> entry = availableVideoQualities.entrySet().iterator().next();
            selectedQualities.put(entry.getKey(), entry.getValue());
            return selectedQualities;
        } else if (mode == QualitySelectionMode.BEST_OF_SELECTED) {
            boolean allowNextBestQuality = false;
            for (final String quality : knownQualities) {
                if (preferredVideoQuality.equals(quality)) {
                    if (availableVideoQualities.containsKey(quality)) {
                        selectedQualities.put(preferredVideoQuality, availableVideoQualities.get(preferredVideoQuality));
                        return selectedQualities;
                    } else {
                        allowNextBestQuality = true;
                    }
                } else if (allowNextBestQuality && availableVideoQualities.containsKey(quality)) {
                    /* Use next-best (well, next worst) quality if user preferred quality was not found */
                    selectedQualities.put(quality, availableVideoQualities.get(quality));
                    return selectedQualities;
                }
            }
        } else {
            /* QualitySelectionMode.SELECTED_ONLY: User wants preferred quality ONLY */
            for (final Map.Entry<String, String> entry : availableVideoQualities.entrySet()) {
                if (entry.getKey().equals(preferredVideoQuality)) {
                    selectedQualities.put(entry.getKey(), entry.getValue());
                    return selectedQualities;
                } else {
                    // fallbackQualities.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return selectedQualities;
    }

    /** Handles complete walls */
    private ArrayList<DownloadLink> crawlWallLink(final CryptedLink param) throws Exception {
        if (vkwall_use_api) {
            return crawlWallLinkAPI(param);
        } else {
            return crawlWallWebsite(param);
        }
    }

    /**
     * Using API <br />
     * TODO: Add support for crawling comments (media/URLs in comments)
     */
    @SuppressWarnings("unchecked")
    private ArrayList<DownloadLink> crawlWallLinkAPI(final CryptedLink param) throws Exception {
        final String ownerID = new Regex(param.getCryptedUrl(), "(?i)https?://[^/]+/wall((\\-)?\\d+)").getMatch(0);
        if (ownerID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(ownerID);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        try {
            apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + ownerID + "&count=1&offset=0&filter=all&extended=0");
        } catch (final DecrypterException e) {
            if (this.getCurrentAPIErrorcode() == 15) {
                /* Access denied --> We have to be logged in via API --> Try website-fallback */
                logger.info("API wall decryption failed because of 'Access denied' --> Trying via website");
                return crawlWallWebsite(param);
            }
            throw e;
        }
        int foundItems = 0;
        int foundItems2 = 0;
        int currentOffset = 0;
        final int total_numberof_entries = Integer.parseInt(br.getRegex("\\{\"response\"\\:\\[(\\d+)").getMatch(0));
        logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        while (currentOffset < total_numberof_entries) {
            logger.info("Starting to crawl offset " + currentOffset + " / " + total_numberof_entries);
            if (currentOffset > 0) {
                sleep(getPaginationSleepMillis(), param);
            }
            apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + ownerID + "&count=" + API_MAX_ENTRIES_PER_REQUEST + "&offset=" + currentOffset + "&filter=all&extended=0");
            final Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (map == null) {
                return ret;
            }
            final int itemsBefore = ret.size();
            List<Object> response = (List<Object>) map.get("response");
            for (final Object entry : response) {
                if (entry instanceof Map) {
                    ret.addAll(decryptWallPostJsonApi(ownerID, (Map<String, Object>) entry, fp));
                }
            }
            final int newItems = ret.size() - itemsBefore;
            foundItems += newItems;
            foundItems2 += newItems;
            logger.info("Crawled offset " + currentOffset + " / " + total_numberof_entries);
            logger.info("Found " + foundItems + " items so far");
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
            if (foundItems2 >= MAX_LINKS_PER_RUN) {
                logger.info("Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Clearing array to prevent memory issues");
                ret.clear();
                foundItems2 = 0;
            }
            currentOffset += API_MAX_ENTRIES_PER_REQUEST;
        }
        return ret;
    }

    /** Decrypts media of single API wall-post json objects. */
    @SuppressWarnings({ "unchecked" })
    private ArrayList<DownloadLink> decryptWallPostJsonApi(final String ownerID, final Map<String, Object> entry, FilePackage fp) throws IOException {
        final long postID = getPostIDFromSingleWallPostMap(entry);
        final long fromId = ((Number) entry.get("from_id")).longValue();
        final long toId = ((Number) entry.get("to_id")).longValue();
        final String wall_list_id = ownerID + "_" + postID;
        /* URL to show this post. */
        final String wall_single_post_url = "https://vk.com/wall" + wall_list_id;
        final String post_text = (String) entry.get("text");
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) entry.get("attachments");
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        if (attachments == null) {
            return ret;
        }
        for (final Map<String, Object> attachment : attachments) {
            try {
                String owner_id = null;
                final String type = (String) attachment.get("type");
                if (type == null) {
                    return ret;
                }
                Map<String, Object> typeObject = (Map<String, Object>) attachment.get(type);
                if (typeObject == null) {
                    logger.warning("No Attachment for type " + type + " in " + attachment);
                    return ret;
                }
                /* links don't necessarily have an owner and we don't need it for them either. */
                if (type.equals(wallpost_type_photo) || type.equals(wallpost_type_doc) || type.equals(wallpost_type_audio) || type.equals(wallpost_type_video) || type.equals(wallpost_type_album)) {
                    owner_id = typeObject.get(VKontakteRuHoster.PROPERTY_GENERAL_owner_id).toString();
                }
                DownloadLink dl = null;
                String content_id = null;
                String title = null;
                String filename = null;
                if (type.equals(wallpost_type_photo) && vkwall_grabphotos) {
                    content_id = typeObject.get("pid").toString();
                    final String album_id = typeObject.get("aid").toString();
                    final String wall_single_photo_content_url = getProtocol() + "vk.com/wall" + ownerID + "?own=1&z=photo" + owner_id + "_" + content_id + "/" + wall_list_id;
                    dl = getSinglePhotoDownloadLink(owner_id + "_" + content_id, null);
                    /*
                     * Override previously set content URL as this really is the direct link to the picture which works fine via browser.
                     */
                    dl.setContentUrl(wall_single_photo_content_url);
                    dl.setProperty("postID", postID);
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_album_id, album_id);
                    dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_owner_id, owner_id);
                    /*
                     * 2019-08-06: TODO: When working on this API again, consider adding support for this again (ask psp, see example-URLs,
                     * see plugin setting VKWALL_STORE_PICTURE_DIRECTURLS)
                     */
                    // dl.setProperty("directlinks", typeObject); //requires a lot of memory but not used at all?
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_list_id, wall_list_id);
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module, "wall");
                } else if (type.equals(wallpost_type_doc) && vkwall_grabdocs) {
                    content_id = typeObject.get("did").toString();
                    title = Encoding.htmlDecode((String) typeObject.get("title"));
                    final String url = (String) typeObject.get("url");
                    if (title == null || url == null) {
                        continue;
                    }
                    filename = title;
                    if (docs_add_unique_id) {
                        filename = owner_id + "_" + content_id + "_" + filename;
                    }
                    dl = createDownloadlink(url);
                    // dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_mainlink, param.getCryptedUrl());
                    dl.setDownloadSize(((Number) typeObject.get("size")).longValue());
                    dl.setName(filename);
                    dl.setAvailable(true);
                } else if (type.equals(wallpost_type_audio) && vkwall_grabaudio) {
                    content_id = typeObject.get("aid").toString();
                    final String artist = Encoding.htmlDecode(typeObject.get("artist").toString());
                    title = Encoding.htmlDecode((String) typeObject.get("title"));
                    filename = artist + " - " + title + ".mp3";
                    final String url = (String) typeObject.get("url");
                    dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + owner_id + "_" + content_id);
                    // dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_mainlink, param.getCryptedUrl());
                    /*
                     * Audiolinks have their directlinks and IDs but no "nice" links so let's simply use the link to the source wall post
                     * here so the user can easily find the title when opening it in browser.
                     */
                    dl.setContentUrl(wall_single_post_url);
                    dl.setProperty("postID", postID);
                    dl.setProperty("fromId", fromId);
                    dl.setProperty("toId", toId);
                    if (VKontakteRuHoster.audioIsValidDirecturl(url)) {
                        dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_directlink, url);
                    }
                    if (fastcheck_audio) {
                        /* If the url e.g. equals "" --> Usually these tracks are GEO-blocked in the region in which the user is. */
                        dl.setAvailable(url != null && url.length() > 0);
                    }
                    dl.setFinalFileName(filename);
                } else if (type.equals(wallpost_type_link) && vkwall_grablink) {
                    final String url = (String) typeObject.get("url");
                    if (url == null) {
                        continue;
                    }
                    dl = createDownloadlink(url);
                } else if (type.equals(wallpost_type_video) && vkwall_grabvideo) {
                    content_id = typeObject.get("vid").toString();
                    String videolink = getProtocol() + "vk.com/video" + owner_id + "_" + content_id;
                    /*
                     * Try to find listID which sometimes is needed to decrypt videos as it grants permissions that would otherwise be
                     * missing!
                     */
                    final String listID = this.br.getRegex("\"/video" + owner_id + "_" + content_id + "\\?list=([a-z0-9]+)\"").getMatch(0);
                    if (listID != null) {
                        videolink += "?list=" + listID;
                    }
                    dl = createDownloadlink(videolink);
                } else if (type.equals(wallpost_type_album) && vkwall_grabalbums) {
                    // it's string here. no idea why
                    final String album_id = typeObject.get("aid").toString();
                    dl = createDownloadlink(getProtocol() + "vk.com/album" + owner_id + "_" + album_id);
                } else if (type.equals(wallpost_type_poll)) {
                    logger.info("Current post only contains a poll --> Skipping it");
                } else {
                    logger.warning("Either the type of the current post is unsupported or not selected by the user: " + type);
                }
                if (dl != null) {
                    if (owner_id != null && content_id != null) {
                        /*
                         * linkID is only needed for links which go into our host plugin. owner_id and content_id should always be available
                         * for that content.
                         */
                        if (filename != null) {
                            dl.setName(filename);
                        }
                        dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_content_id, content_id);
                    }
                    fp.add(dl);
                    ret.add(dl);
                }
            } catch (Throwable ee) {
                // catches casting errors etc.
                getLogger().info(attachment + "");
                getLogger().log(ee);
            }
        }
        ret.addAll(crawlUrlsInsidePosts(post_text));
        if (this.vkwall_comments_grab_comments && !this.global_dupes.contains(wall_list_id)) {
            global_dupes.add(wall_list_id);
        }
        return ret;
    }

    /** Scans for URLs inside the (given) text of a wall post/comment. */
    private ArrayList<DownloadLink> crawlUrlsInsidePosts(String html) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (html == null) {
            return ret;
        }
        html = Encoding.htmlDecode(html);
        /* Check if user wants to add urls inside the posted text */
        final String[] post_text_urls = HTMLParser.getHttpLinks(html, null);
        if (post_text_urls != null && vkwall_graburlsinsideposts) {
            for (final String posted_url : post_text_urls) {
                boolean add_url = true;
                try {
                    add_url = posted_url.matches(vkwall_graburlsinsideposts_regex);
                    logger.info("User-Defined wall-post-url-RegEx seems to be correct");
                } catch (final Throwable e) {
                    logger.warning("User-Defined wall-post-url-RegEx seems to be invalid");
                    /* Probably user entered invalid RegEx --> Fallback to default RegEx */
                    add_url = posted_url.matches(vkwall_graburlsinsideposts_regex_default);
                }
                if (add_url) {
                    if (!posted_url.contains("vk.com/")) {
                        logger.info("WTF url: " + posted_url);
                    }
                    logger.info("ADDING url: " + posted_url);
                    ret.add(this.createDownloadlink(posted_url));
                } else {
                    logger.info("NOT ADDING url: " + posted_url);
                }
            }
        }
        return ret;
    }

    private long getPostIDFromSingleWallPostMap(final Map<String, Object> entry) {
        return ((Number) entry.get("id")).longValue();
    }

    private ArrayList<DownloadLink> crawlWallPost(final CryptedLink param) throws Exception {
        if (vkwall_use_api) {
            return crawlWallPostAPI(param);
        } else {
            return crawlWallPostWebsite(param);
        }
    }

    private ArrayList<DownloadLink> crawlWallPostWebsite(final CryptedLink param) throws Exception {
        final String contenturl;
        final String wValue = UrlQuery.parse(param.getCryptedUrl()).get("w");
        if (wValue != null && wValue.matches("(?i)wall-?\\d+_\\d+")) {
            contenturl = "https://vk.com/" + wValue;
        } else {
            contenturl = this.fixAddedURL(param.getCryptedUrl());
        }
        final String owner_id = new Regex(contenturl, "(?i)wall(-?\\d+)").getMatch(0);
        final String wall_post_ID = new Regex(contenturl, "(?i)wall(-?\\d+_\\d+)").getMatch(0);
        if (owner_id == null || wall_post_ID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(contenturl);
        final FilePackage fp = FilePackage.getInstance();
        /* Set ID of current wall-post as packagename. */
        fp.setName(wall_post_ID);
        final int maxEntriesPerRequest = 20;
        int offset = 0;
        int page = 0;
        int totalNumberOfItems = 0;
        logger.info("Crawling single wall post");
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        do {
            final int numberofFoundItemsOld = ret.size();
            logger.info("Crawling (comments of) single wall post page: " + page);
            String htmlBeforeReplies = br.getRegex("div class=\"wall_text\"(.*?)\"like-button-count\"").getMatch(0);
            if (htmlBeforeReplies == null) {
                logger.warning("Failed to find single post text --> Fallback to complete HTML --> This way we might grab content from comments even if the user does not want this to happen!");
                htmlBeforeReplies = br.getRequest().getHtmlCode();
            }
            /* Grab media inside users' post */
            ret.addAll(websiteCrawlContent(contenturl, htmlBeforeReplies, fp, this.vkwall_grabaudio, this.vkwall_grabvideo, this.vkwall_grabphotos, this.vkwall_grabdocs, this.vkwall_graburlsinsideposts, this.photos_store_picture_directurls));
            /* Open RegEx: Just grab all html starting from comments section until end */
            final String htmlReplies = br.getRegex("<div class=\"replies\"(.+)").getMatch(0);
            if (htmlReplies != null && this.vkwall_comments_grab_comments) {
                /* Grab media inside replies/comments to users' post if wished by user. */
                ret.addAll(websiteCrawlContent(contenturl, br.getRequest().getHtmlCode(), fp, this.vkwall_comment_grabaudio, this.vkwall_comment_grabvideo, this.vkwall_comment_grabphotos, false, this.vkwall_comment_grablink, this.photos_store_picture_directurls));
            }
            final int numberofItemsAddedThisLoop = ret.size() - numberofFoundItemsOld;
            logger.info("Offset " + offset + " contained " + numberofItemsAddedThisLoop + " items in total so far (including inside replies)");
            offset += maxEntriesPerRequest;
            logger.info("Number of NEW added items: " + numberofItemsAddedThisLoop);
            /* Avoid this sleep time if we stop after the current */
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (!this.vkwall_comments_grab_comments) {
                logger.info("Stopping because user does not want to crawl items from replies/comments to single posts");
                break;
            } else if (page == 0 && htmlReplies == null) {
                logger.info("Stopping because there is not more than 1 page (no replies available)");
                break;
            } else {
                /* Fail-safe */
                if (totalNumberOfItems > 0 && offset >= totalNumberOfItems) {
                    logger.info("Stopping because offset >= " + totalNumberOfItems);
                    break;
                }
                sleep(getPaginationSleepMillis(), param);
                page++;
                /* Everything after the first request --> We only have replies */
                final UrlQuery getRepliesQuery = new UrlQuery();
                getRepliesQuery.add("act", "get_post_replies");
                getRepliesQuery.add("al", "1");
                getRepliesQuery.add("count", maxEntriesPerRequest + "");
                getRepliesQuery.add("item_id", "2");
                getRepliesQuery.add("offset", offset + "");
                getRepliesQuery.add("order", "asc");
                getRepliesQuery.add("owner_id", owner_id);
                /* 2020-12-17: No idea what it is -> Request works without that too */
                // getRepliesQuery.add("prev_id", "201");
                this.getPage(br, br.createPostRequest("/al_wall.php", getRepliesQuery));
                try {
                    final String json = br.getRegex("(\\{.*\\})").getMatch(0);
                    final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
                    final Map<String, Object> paginationInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/{1}/{2}");
                    if (totalNumberOfItems == 0) {
                        totalNumberOfItems = ((Number) paginationInfo.get("count")).intValue();
                        logger.info("Found total number of items: " + totalNumberOfItems);
                    }
                    final String html = (String) JavaScriptEngineFactory.walkJson(entries, "payload/{1}/{0}");
                    if (html == null) {
                        logger.info("Stopping because failed to find html inside json");
                        break;
                    }
                    br.getRequest().setHtmlCode(html);
                } catch (final Throwable e) {
                    /* Fail-safe */
                    logger.log(e);
                    logger.info("Stopping because failed to parse json response");
                    break;
                }
            }
        } while (true);
        logger.info("Found " + ret.size() + " items");
        return ret;
    }

    /** Returns time to wait between pagination http requests. */
    private long getPaginationSleepMillis() {
        return this.cfg.getLongProperty(VKontakteRuHoster.SLEEP_PAGINATION_GENERAL, VKontakteRuHoster.defaultSLEEP_PAGINATION_GENERAL);
    }

    /**
     * Using API, finds and adds contents of a single wall post. <br/>
     * 2019-08-05: Requires authorization!
     */
    @SuppressWarnings("unchecked")
    private ArrayList<DownloadLink> crawlWallPostAPI(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        final String contenturl;
        if (!isKnownType(param.getCryptedUrl())) {
            /* owner_id not given --> We need to find the owner_id */
            final String url_owner = new Regex(param.getCryptedUrl(), "vk\\.com/([^\\?\\&=]+)").getMatch(0);
            if (StringUtils.isEmpty(url_owner)) {
                /* Unsupported URL */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String ownerName = resolveScreenName_API(url_owner);
            if (ownerName == null) {
                logger.warning("Decryption failed - Most likely an unsupported URL pattern! --> " + param.getCryptedUrl() + "");
                /* do not return null, as this shows crawler error, and unsupported urls are not defects! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String type = PluginJSonUtils.getJsonValue(br, "type");
            if (type == null) {
                logger.warning("Failed to find type for link: " + param.getCryptedUrl());
                throw new DecrypterException("Plugin broken");
            }
            if (type.equals("user")) {
                contenturl = "https://" + this.getHost() + "/albums" + ownerName;
            } else {
                contenturl = "https://" + this.getHost() + "/wall-" + ownerName;
            }
        } else {
            contenturl = param.getCryptedUrl();
        }
        final Regex wallRegex = new Regex(contenturl, "(?i)vk\\.com/wall((?:\\-)?\\d+)_(\\d+)");
        final String ownerID = wallRegex.getMatch(0);
        final String postID = wallRegex.getMatch(1);
        final String postIDWithOwnerID = ownerID + "_" + postID;
        try {
            apiGetPageSafe("https://api.vk.com/method/wall.getById?posts=" + postIDWithOwnerID + "&extended=0&copy_history_depth=2");
        } catch (final DecrypterException e) {
            // if (this.getCurrentAPIErrorcode() == 15) {
            // /* Access denied --> We have to be logged in via API --> Try website-fallback */
            // logger.info("API wall decryption failed because of 'Access denied' --> Trying via website");
            // this.getPageSafe("https://vk.com/wall" + postIDWithOwnerID);
            // decryptSingleWallPostAndComments_Website(postIDWithOwnerID, br.toString(), null);
            // return;
            // }
            throw e;
        }
        Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.getRequest().getHtmlCode());
        try {
            /* Access original url as we sometimes need the listID for videos (see decryptWallPost). */
            this.apiGetPageSafe("https://vk.com/wall" + postIDWithOwnerID);
        } catch (final Throwable e) {
        }
        if (map == null) {
            return ret;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(postIDWithOwnerID);
        List<Object> response = (List<Object>) map.get("response");
        for (Object entry : response) {
            if (entry instanceof Map) {
                ret.addAll(decryptWallPostJsonApi(ownerID, (Map<String, Object>) entry, fp));
            }
        }
        logger.info("Found " + ret.size() + " links");
        return ret;
    }

    private ArrayList<DownloadLink> crawlWallWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        /* The URL could also be a directURL or unsupported -> Let's check for that first */
        URLConnectionAdapter con = this.br.openGetConnection(param.getCryptedUrl());
        if (this.looksLikeDownloadableContent(con)) {
            logger.info("URL leads to downloadable content");
            /* Very rare case */
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
            final DownloadLink direct = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(con.getURL().toExternalForm()));
            if (con.getCompleteContentLength() > 0) {
                direct.setVerifiedFileSize(con.getCompleteContentLength());
            }
            final String filename = Plugin.getFileNameFromDispositionHeader(con);
            if (filename != null) {
                direct.setFinalFileName(filename);
            }
            direct.setAvailable(true);
            ret.add(direct);
            return ret;
        }
        br.followConnection();
        int counter_wall_start_from = 0;
        int offset_increase = 10;
        String postvalue_fixed = null;
        int total_numberof_entries = -1;
        int currentOffset = 0;
        final String preGivenOffsetStr = UrlQuery.parse(param.getCryptedUrl()).get("offset");
        if (preGivenOffsetStr != null && preGivenOffsetStr.matches("\\d+")) {
            final int preGivenOffset = Integer.parseInt(preGivenOffsetStr);
            if (preGivenOffset > 0) {
                logger.info("Starting with pre given offset: " + preGivenOffset);
                currentOffset = preGivenOffset;
            }
        }
        String ownerID = null;
        /* 2020-02-07: This is most likely not given but we have other fail safes in place to stop once we're done. */
        // final String total_numberof_entriesStr = br.getRegex("id=\"page_wall_count_all\" value=\"(\\d+)\"").getMatch(0);
        final String optsJson = br.getRegex("var opts = (\\{.*?\\}), preload = ").getMatch(0);
        if (optsJson != null) {
            final Map<String, Object> entries = restoreFromString(optsJson, TypeRef.MAP);
            final Object ownerIDO = entries.get("owner_id");
            if (ownerIDO != null) {
                ownerID = ownerIDO.toString();
            }
            final Number total_numberof_entriesO = (Number) entries.get("count");
            if (total_numberof_entriesO != null) {
                total_numberof_entries = total_numberof_entriesO.intValue();
            }
            final Number per_pageO = (Number) entries.get("per_page");
            if (per_pageO != null) {
                offset_increase = per_pageO.intValue();
            }
        }
        if (total_numberof_entries == -1) {
            final String total_numberof_entriesStr = br.getRegex("id=\"page_wall_count_own\" value=\"(\\d+)\"").getMatch(0);
            if (total_numberof_entriesStr != null) {
                total_numberof_entries = Integer.parseInt(total_numberof_entriesStr);
            }
        }
        if (total_numberof_entries == -1) {
            logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        } else {
            logger.info("PATTERN_WALL_LINK has a max offset of UNKNOWN and a current offset of " + currentOffset);
        }
        /* Find owner_id */
        /* First try to obtain ownerID from URL */
        if (StringUtils.isEmpty(ownerID)) {
            ownerID = new Regex(param.getCryptedUrl(), "https?://[^/]+/(?:id|wall)(-?\\d+)").getMatch(0);
            if (ownerID == null) {
                /* Next from HTML */
                ownerID = br.getRegex("class=\"_wall_tab_own\">\\s*<a href=\"/wall(-?\\d+)\\?own=1\"").getMatch(0);
            }
            if (ownerID == null) {
                /* Old handling/Fallback */
                /* We need to find the owner_id - without it we would only be able to find all entries from the first page. */
                String json_source = br.getRegex("window\\[\\'public\\'\\]\\.init\\((\\{.*?)</script>").getMatch(0);
                if (json_source == null) {
                    json_source = br.getRegex("Profile\\.init\\((\\{.*?)</script>").getMatch(0);
                }
                if (json_source == null) {
                    /* Public groups */
                    json_source = br.getRegex("Groups\\.init\\((\\{.*?)</script>").getMatch(0);
                    if (json_source != null) {
                        ownerID = PluginJSonUtils.getJson(json_source, "group_id");
                    }
                }
                if (StringUtils.isEmpty(ownerID)) {
                    ownerID = PluginJSonUtils.getJson(json_source, "public_id");
                }
                postvalue_fixed = PluginJSonUtils.getJson(json_source, "fixed_post_id");
                if (StringUtils.isEmpty(ownerID) || ownerID.equals("null")) {
                    /* ownerID is given as double value --> Correct that */
                    ownerID = PluginJSonUtils.getJson(json_source, "user_id");
                    if (!StringUtils.isEmpty(ownerID) && ownerID.matches("\\d+\\.\\d+")) {
                        ownerID = ownerID.split("\\.")[0];
                    }
                }
                if (ownerID != null && !ownerID.equals("null")) {
                    /* Correct crawled values */
                    ownerID = "-" + ownerID;
                }
            }
        }
        if (StringUtils.isEmpty(ownerID) || ownerID.equals("null")) {
            logger.warning("Failed to find owner_id --> Can only crawl first page");
            ownerID = null;
        }
        if (postvalue_fixed == null) {
            postvalue_fixed = "";
        }
        FilePackage fp = null;
        if (ownerID != null) {
            fp = FilePackage.getInstance();
            fp.setName(ownerID);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final int max_pages_without_downloadable_content_in_a_row = 5;
        int pages_without_downloadable_content_in_a_row = 0;
        int totalNumberofResults = 0;
        int resultsUntilMemorySaveReset = 0;
        do {
            logger.info("Crawling offset " + currentOffset);
            if (currentOffset > 0) {
                if (StringUtils.isEmpty(ownerID)) {
                    logger.warning("Stopping because: Stopping after first loop because owner_id is not given");
                    break;
                }
                sleep(getPaginationSleepMillis(), param);
                this.getPage(br, br.createPostRequest("/al_wall.php", String.format("act=get_wall&al=1&fixed=%s&offset=%s&onlyCache=false&owner_id=%s&type=own&wall_start_from=%s", postvalue_fixed, currentOffset, ownerID, counter_wall_start_from)));
                this.br.getRequest().setHtmlCode(JSonUtils.unescape(br.toString()));
            }
            final int numberof_items_old = ret.size();
            if (this.vkwall_comments_grab_comments) {
                logger.info("User wants content of comments --> First adding comment URLs during wall-crawling");
                final String[] singleWallPostIDs = br.getRegex("wall(" + ownerID + "_\\d+)").getColumn(0);
                for (final String singleWallpostID : singleWallPostIDs) {
                    final DownloadLink dl = this.createDownloadlink("https://vk.com/wall" + singleWallpostID);
                    ret.add(dl);
                }
            } else {
                logger.info("Crawling items inside single post as part of a wall");
                ret.addAll(websiteCrawlContent(br.getURL(), br.toString(), fp, this.vkwall_grabaudio, this.vkwall_grabvideo, this.vkwall_grabphotos, this.vkwall_grabdocs, this.vkwall_graburlsinsideposts, this.photos_store_picture_directurls));
            }
            final int numberof_items_current = ret.size();
            final int counter_items_found_in_current_offset = numberof_items_current - numberof_items_old;
            totalNumberofResults += counter_items_found_in_current_offset;
            resultsUntilMemorySaveReset += counter_items_found_in_current_offset;
            if (counter_items_found_in_current_offset == 0) {
                logger.info("Failed to find any items for offset: " + currentOffset);
                pages_without_downloadable_content_in_a_row++;
            } else {
                /* Reset this */
                pages_without_downloadable_content_in_a_row = 0;
            }
            logger.info("Crawled offset " + currentOffset + " | Found items so far: " + totalNumberofResults);
            /*
             * Stop conditions are placed here and not in the while loops' footer on purpose to place loggers and get to know the exact stop
             * reason!
             */
            /* Increase counters */
            currentOffset += offset_increase;
            counter_wall_start_from += 10;
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (pages_without_downloadable_content_in_a_row >= max_pages_without_downloadable_content_in_a_row) {
                logger.info("Stopping because: Failed to find more items for " + pages_without_downloadable_content_in_a_row + " times in the row");
                break;
            }
            if (resultsUntilMemorySaveReset >= MAX_LINKS_PER_RUN) {
                logger.info("Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Clearing array to save memory");
                ret.clear();
                resultsUntilMemorySaveReset = 0;
            }
        } while (true);
        return ret;
    }

    /**
     * Crawls desired content from website html code from either a wall-POST or wall-COMMENT or ALBUMS URL from below a post.
     *
     * @throws IOException
     * @throws DecrypterException
     */
    private ArrayList<DownloadLink> websiteCrawlContent(final String url_source, final String html, final FilePackage fp, final boolean grabAudio, final boolean grabVideo, final boolean grabPhoto, final boolean grabDocs, final boolean grabURLsInsideText, final boolean store_picture_directurls) throws IOException, DecrypterException {
        if (url_source == null) {
            throw new DecrypterException("Decrypter broken");
        }
        final String url_source_without_params = URLHelper.getUrlWithoutParams(url_source);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        boolean grabAlbums = false;
        String wall_post_owner_id = null;
        String wall_post_content_id = null;
        String wall_post_reply_content_id = null;
        String wall_single_post_url = null;
        String album_ID = null;
        String photo_list_id = null;
        String photo_module = null;
        boolean isContentFromWall = false;
        boolean isContentFromWallReply = false;
        boolean isPostContentURLGivenAndTheSameForAllItems = false;
        if (url_source.matches(PATTERN_PHOTO_ALBUMS)) {
            /* 2019-10-02: Newly added albums support */
            isContentFromWall = false;
            album_ID = new Regex(url_source, PATTERN_PHOTO_ALBUMS).getMatch(0);
            grabAlbums = true;
        } else if (url_source.matches(PATTERN_PHOTO_ALBUM)) {
            final String id_of_current_album = new Regex(url_source, "/album((?:\\-)?\\d+_\\d+)").getMatch(0);
            if (id_of_current_album != null) {
                photo_list_id = "album" + id_of_current_album;
            }
            photo_module = "photos";
        } else if (url_source.matches("^-?\\d+_\\d+$")) {
            /* url_source = not an URL but our wall_IDs */
            isContentFromWall = true;
            photo_module = "wall";
            /*
             * No matter whether we crawl the content of a post or comment/reply below, we always need the IDs of the post to e.g. build
             * correct content-URLs reading to the comment/reply!
             */
            final String[] wall_post_IDs = url_source.split("_");
            wall_post_owner_id = wall_post_IDs[0];
            wall_post_content_id = wall_post_IDs[1];
            wall_single_post_url = String.format("https://vk.com/wall%s", url_source);
            isPostContentURLGivenAndTheSameForAllItems = true;
        } else if (url_source.matches(PATTERN_GENERAL_WALL_LINK)) {
            /* url_source = not an URL but our wall_IDs */
            isContentFromWall = true;
            photo_module = "wall";
        }
        String ownerIDTemp = null;
        String contentIDTemp = null;
        /* Make sure this works for POSTs, COMMENTs and ALBUMs!! */
        final String wall_post_text = new Regex(html, "<div class=\"wall_reply_text\">([^<>]+)</div>").getMatch(0);
        if (grabAlbums) {
            /* These will go back into the decrypter */
            final String[] albumIDs = br.getRegex("(/album[^\"]+)\"").getColumn(0);
            for (final String albumContentStr : albumIDs) {
                if (!global_dupes.add(albumContentStr)) {
                    /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                    logger.info("Skipping dupe: ");
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/" + albumContentStr);
                ret.add(dl);
            }
        }
        final String websiteJson = br.getRegex("extend\\(window\\.cur \\|\\| \\{\\}, (\\{\".*?\\})\\);").getMatch(0);
        if (websiteJson != null) {
            /* 2023-11-17: New */
            final Map<String, Object> entries = restoreFromString(websiteJson, TypeRef.MAP);
            final List<Map<String, Object>> apiPrefetchCache = (List<Map<String, Object>>) entries.get("apiPrefetchCache");
            Map<String, Object> albumsMap = null;
            Map<String, Object> photosMap = null;
            for (final Map<String, Object> apiPrefetchCacheItem : apiPrefetchCache) {
                final String method = apiPrefetchCacheItem.get("method").toString();
                if (method.equalsIgnoreCase("photos.getAlbums")) {
                    albumsMap = (Map<String, Object>) apiPrefetchCacheItem.get("response");
                } else if (method.equalsIgnoreCase("photos.get")) {
                    photosMap = (Map<String, Object>) apiPrefetchCacheItem.get("response");
                }
            }
            if (grabPhoto && photosMap != null) {
                final int count = ((Number) photosMap.get("count")).intValue();
                final List<Map<String, Object>> photos = (List<Map<String, Object>>) photosMap.get("items");
                logger.info("WebAPI photos.get: Count: " + count + " | Crawling now: " + photos.size());
                for (final Map<String, Object> photo : photos) {
                    final String owner_id = photo.get("owner_id").toString();
                    final String content_id = photo.get("id").toString();
                    final String photoContentStr = owner_id + "_" + content_id;
                    if (!global_dupes.add(photoContentStr)) {
                        /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                        logger.info("Skipping dupe: " + photoContentStr);
                        continue;
                    }
                    final DownloadLink photodl = getSinglePhotoDownloadLink(owner_id + "_" + content_id, null);
                    /*
                     * Override previously set content URL as this really is the direct link to the picture which works fine via browser.
                     */
                    photodl.setContentUrl("https://vk.com/photo" + owner_id + "_" + content_id);
                    if (isContentFromWall) {
                        photodl.setProperty("postID", wall_post_content_id);
                        photodl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_owner_id, owner_id);
                    } else {
                        /* Album content */
                        photodl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_album_id, album_ID);
                    }
                    /*
                     * 2020-01-27: Regarding photo_list_id and photo_module: If not set but required, it will more likely happen that a
                     * "Too many requests in a short time" message appears on download attempt!
                     */
                    if (photo_list_id != null) {
                        photodl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_list_id, photo_list_id);
                    }
                    if (photo_module != null) {
                        photodl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module, photo_module);
                    }
                    final List<Map<String, Object>> photosizes = (List<Map<String, Object>>) photo.get("sizes");
                    if (photosizes != null) {
                        final Map<String, Integer> sizeAltMapping = new HashMap<String, Integer>();
                        sizeAltMapping.put("s", 100);
                        sizeAltMapping.put("m", 200);
                        sizeAltMapping.put("x", 300);
                        sizeAltMapping.put("y", 400);
                        sizeAltMapping.put("z", 500);
                        sizeAltMapping.put("w", 600);
                        int bestHeight = -1;
                        String bestDirecturl = null;
                        for (final Map<String, Object> photosize : photosizes) {
                            int height = ((Number) photosize.get("height")).intValue();
                            if (height == 0) {
                                /* Fallback */
                                height = sizeAltMapping.get(photosize.get("type").toString());
                            }
                            if (bestDirecturl == null || height > bestHeight) {
                                bestHeight = height;
                                bestDirecturl = photosize.get("url").toString();
                            }
                        }
                        if (bestDirecturl != null) {
                            photodl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_directlink, bestDirecturl);
                        }
                    }
                    if (fp != null) {
                        photodl._setFilePackage(fp);
                    }
                    ret.add(photodl);
                }
            }
            if (grabPhotoAlbums && albumsMap != null) {
                final List<Map<String, Object>> albums = (List<Map<String, Object>>) albumsMap.get("items");
                for (final Map<String, Object> photoalbum : albums) {
                    String albumID = photoalbum.get("id").toString();
                    /* 2023-11-17: I have no idea what I'm doing lol */
                    albumID = albumID.replace("-6", "0");
                    albumID = albumID.replace("-7", "00");
                    final String url = "https://vk.com/album" + photoalbum.get("owner_id") + "_" + albumID;
                    final DownloadLink albumdl = this.createDownloadlink(url);
                    ret.add(albumdl);
                }
            }
        } else {
            logger.warning("Failed to find prefetched json in html");
        }
        if (grabPhoto) {
            String[] photo_ids = new Regex(html, "showPhoto\\(\\'(-?\\d+_\\d+)").getColumn(0);
            if (photo_ids == null || photo_ids.length == 0) {
                /* 2022-12-05 */
                photo_ids = new Regex(html, "data-photo-id=\"(-?\\d+_\\d+)\"").getColumn(0);
            }
            for (final String photoContentStr : photo_ids) {
                if (!global_dupes.add(photoContentStr)) {
                    /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                    logger.info("Skipping dupe: " + photoContentStr);
                    continue;
                }
                final String[] wall_id_info = photoContentStr.split("_");
                ownerIDTemp = wall_id_info[0];
                contentIDTemp = wall_id_info[1];
                String picture_preview_json = null;
                String photo_htmlOLD = new Regex(html, "showPhoto\\(([^\\)]*?" + photoContentStr + "[^\\)]*?)\\)").getMatch(0);
                String photo_htmlNEW = new Regex(html, "<div [^>]*data-photo-id=\"" + photoContentStr + "\".*?</div>\\s+</div>").getMatch(-1);
                String single_photo_content_url = getProtocol() + this.getHost() + "/photo" + ownerIDTemp + "_" + contentIDTemp;
                if (photo_htmlOLD != null) {
                    /* Old/deprecated! Remove in 2023-05 if still not needed anymore then! */
                    photo_htmlOLD = photo_htmlOLD.replace("'", "");
                    final String[] photoInfoArray = photo_htmlOLD.split(", ");
                    final String photo_list_id_tmp = photoInfoArray[1];
                    if (photo_list_id_tmp.matches("wall-?\\d+_\\d+")) {
                        photo_list_id = photo_list_id_tmp;
                        final String wall_post_reply_content_idTmp = new Regex(photo_list_id_tmp, "(\\d+)$").getMatch(0);
                        if (html.contains("?reply=" + wall_post_reply_content_idTmp)) {
                            isContentFromWallReply = true;
                            wall_post_reply_content_id = wall_post_reply_content_idTmp;
                            single_photo_content_url = url_source_without_params + "?reply=" + wall_post_reply_content_id + "&z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2Fwall" + ownerIDTemp + "_" + wall_post_reply_content_id;
                            if (!isPostContentURLGivenAndTheSameForAllItems) {
                                /* Try to find post_id - if this goes wrong we might not be able to download the content later on. */
                                final String postIDs = new Regex(html, "<div class=\"wall_text\"><div id=\"wpt(-\\d+_\\d+)\" class=\"wall_post_cont _wall_post_cont\"><div[^>]+>[^>]+</div><div[^>]+><a[^>]+showPhoto\\('" + photoContentStr).getMatch(0);
                                if (postIDs != null) {
                                    final String[] wall_post_IDs = postIDs.split("_");
                                    wall_post_owner_id = wall_post_IDs[0];
                                    wall_post_content_id = wall_post_IDs[1];
                                    wall_single_post_url = String.format("https://vk.com/wall%s%s", wall_post_owner_id, wall_post_content_id);
                                }
                            }
                        } else {
                            /* Link post containing the photo */
                            single_photo_content_url = getProtocol() + this.getHost() + "/wall" + "?z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2Fwall" + url_source;
                            photo_list_id = url_source;
                        }
                    } else if (photo_list_id_tmp.matches("tag-?\\d+")) {
                        /* Different type of wall photos which again need different IDs and have different contentURLs. */
                        isContentFromWallReply = true;
                        photo_list_id = photo_list_id_tmp;
                        if (!isPostContentURLGivenAndTheSameForAllItems) {
                            /* Try to find post_id - if this goes wrong we might not be able to download the content later on. */
                            final String tag_id = new Regex(photo_list_id, "(-?\\d+)$").getMatch(0);
                            single_photo_content_url = String.format("https://vk.com/photo%s?tag=%s", photoContentStr, tag_id);
                        }
                    }
                    if (photoInfoArray.length >= 3) {
                        picture_preview_json = photoInfoArray[2];
                    }
                    if (picture_preview_json == null) {
                        /* 2020-02-18: This should not be required anymore */
                        picture_preview_json = new Regex(photo_htmlOLD, "(\\{(?:\"|\\&quot;)(?:base|temp)(?:\"|\\&quot;).*?\\}),[^\\{\\}]+\\)").getMatch(0);
                    }
                } else if (photo_htmlNEW != null) {
                    /* 2022-12-05 */
                    final String photo_list_id_tmp = new Regex(photo_htmlNEW, "data-list-id=\"([^\"]+)").getMatch(0);
                    if (photo_list_id_tmp.matches("wall-?\\d+_\\d+")) {
                        photo_list_id = photo_list_id_tmp;
                        final String wall_post_reply_content_idTmp = new Regex(photo_list_id_tmp, "(\\d+)$").getMatch(0);
                        if (html.contains("?reply=" + wall_post_reply_content_idTmp)) {
                            isContentFromWallReply = true;
                            wall_post_reply_content_id = wall_post_reply_content_idTmp;
                            single_photo_content_url = url_source_without_params + "?reply=" + wall_post_reply_content_id + "&z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2Fwall" + ownerIDTemp + "_" + wall_post_reply_content_id;
                            if (!isPostContentURLGivenAndTheSameForAllItems) {
                                // TODO: Check/fix this part
                                /* Try to find post_id - if this goes wrong we might not be able to download the content later on. */
                                final String postIDs = new Regex(html, "<div class=\"wall_text\"><div id=\"wpt(-\\d+_\\d+)\" class=\"wall_post_cont _wall_post_cont\"><div[^>]+>[^>]+</div><div[^>]+><a[^>]+showPhoto\\('" + photoContentStr).getMatch(0);
                                if (postIDs != null) {
                                    final String[] wall_post_IDs = postIDs.split("_");
                                    wall_post_owner_id = wall_post_IDs[0];
                                    wall_post_content_id = wall_post_IDs[1];
                                    wall_single_post_url = String.format("https://vk.com/wall%s%s", wall_post_owner_id, wall_post_content_id);
                                }
                            }
                        } else {
                            /* Link post containing the photo */
                            single_photo_content_url = url_source_without_params + "?z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2F" + photo_list_id_tmp;
                            photo_list_id = photo_list_id_tmp;
                        }
                    } else if (photo_list_id_tmp.matches("tag-?\\d+")) {
                        /* Different type of wall photos which again need different IDs and have different contentURLs. */
                        isContentFromWallReply = true;
                        photo_list_id = photo_list_id_tmp;
                        if (!isPostContentURLGivenAndTheSameForAllItems) {
                            /* Try to find post_id - if this goes wrong we might not be able to download the content later on. */
                            final String tag_id = new Regex(photo_list_id, "(-?\\d+)$").getMatch(0);
                            single_photo_content_url = String.format("https://vk.com/photo%s?tag=%s", photoContentStr, tag_id);
                        }
                    }
                }
                if (isContentFromWallReply && !vkwall_grabphotos) {
                    logger.info("Skipping wall comment item because user has deselected such items: " + photoContentStr);
                    continue;
                }
                final DownloadLink dl = getSinglePhotoDownloadLink(ownerIDTemp + "_" + contentIDTemp, picture_preview_json);
                /*
                 * Override previously set content URL as this really is the direct link to the picture which works fine via browser.
                 */
                dl.setContentUrl(single_photo_content_url);
                if (isContentFromWall) {
                    dl.setProperty("postID", wall_post_content_id);
                    dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_owner_id, ownerIDTemp);
                } else {
                    /* Album content */
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_album_id, album_ID);
                }
                /*
                 * 2020-01-27: Regarding photo_list_id and photo_module: If not set but required, it will more likely happen that a
                 * "Too many requests in a short time" message appears on download attempt!
                 */
                if (photo_list_id != null) {
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_list_id, photo_list_id);
                }
                if (photo_module != null) {
                    dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module, photo_module);
                }
                if (store_picture_directurls) {
                    if (picture_preview_json != null) {
                        picture_preview_json = Encoding.htmlDecode(picture_preview_json);
                        dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_directurls_fallback, picture_preview_json);
                    }
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
            }
        }
        if (grabAudio) {
            /* Audiofiles */
            final String[] audio_ids = new Regex(html, "data\\-audio=\"\\[([^<>\"]+)\\]\"").getColumn(0);
            for (String audioInfoSingle : audio_ids) {
                audioInfoSingle = Encoding.htmlDecode(audioInfoSingle).replace("\"", "");
                final String[] audioInfoArray = audioInfoSingle.split(",");
                final String audioOwnerID = audioInfoArray[1];
                final String audioContentID = audioInfoArray[0];
                final String audioContentStr = audioOwnerID + "_" + audioContentID;
                if (!global_dupes.add(audioContentStr)) {
                    /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                    logger.info("Skipping dupe: ");
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + audioContentStr);
                final String artist = audioInfoArray[4];
                final String title = audioInfoArray[3];
                dl.setFinalFileName(Encoding.htmlDecode(artist + " - " + title) + ".mp3");
                if (fastcheck_audio) {
                    dl.setAvailable(true);
                }
                /* There is no official URL to these mp3 files --> Use url of the post whenever possible. */
                if (wall_single_post_url != null) {
                    dl.setContentUrl(wall_single_post_url);
                } else {
                    dl.setContentUrl(url_source);
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
            }
        }
        /* Videos */
        if (grabVideo) {
            ret.addAll(crawlVideos(this.br, fp));
        }
        if (grabDocs) {
            /* 2021-01-08 */
            final String[] docHTMLs = br.getRegex("div class=\"page_doc_row\"[^>]*>(.*?)</div>\\s*</div>").getColumn(0);
            for (final String docHTML : docHTMLs) {
                final String url = new Regex(docHTML, "href=\"(/doc[^\"]+)\"").getMatch(0);
                final String filename = new Regex(docHTML, "target=\"_blank\">([^<>\"]+)</a>").getMatch(0);
                final String filesize = new Regex(docHTML, "class=\"page_doc_size\">([^<>\"]+)<").getMatch(0);
                if (url == null || filename == null || filesize == null) {
                    /* Skip invalid items */
                    continue;
                } else if (!global_dupes.add(url)) {
                    /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                    logger.info("Skipping dupe: ");
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(this.getProtocol() + this.getHost() + url);
                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setAvailable(true);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
            }
        }
        if (grabURLsInsideText && isContentFromWall) {
            ret.addAll(this.crawlUrlsInsidePosts(wall_post_text));
        }
        return ret;
    }

    /** Works offline, simply converts the added link into a DownloadLink for the host plugin and sets required properties. */
    private DownloadLink processSinglePicture(final CryptedLink param) throws Exception {
        String module = null;
        String listID = null;
        /* URLs may contain multiple list_id-like strings! It is important to use the source URL as an orientation. */
        final Regex photoTag = new Regex(param.getCryptedUrl(), "https?://[^/]+/photo-?\\d+_\\d+\\?tag=(\\d+)");
        final Regex photoZ = new Regex(param.getCryptedUrl(), PATTERN_PHOTO_SINGLE_Z);
        if (photoTag.matches()) {
            module = "photos";
            listID = "tag" + new Regex(param.getCryptedUrl(), "(\\d+)$").getMatch(0);
        } else if (photoZ.matches()) {
            module = photoZ.getMatch(0); // e.g. wall, album, feed
            listID = photoZ.getMatch(2);
        }
        final String ownerID = new Regex(param.getCryptedUrl(), "photo(-?\\d+)_\\d+").getMatch(0);
        final String contentID = new Regex(param.getCryptedUrl(), "photo-?\\d+_(\\d+)").getMatch(0);
        if (ownerID == null || contentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink dl = getSinglePhotoDownloadLink(ownerID + "_" + contentID, null);
        dl.setContentUrl(param.getCryptedUrl());
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        if (module != null) {
            dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module, module);
        }
        dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_list_id, listID);
        return dl;
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlDocs(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        this.getPage(param.getCryptedUrl());
        if (br.containsHTML("(?i)Unfortunately, you are not a member of this group and cannot view its documents") || br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String owner_ID = new Regex(param.getCryptedUrl(), "((?:\\-)?\\d+)$").getMatch(0);
        String fpName = null;
        if (cfg.getBooleanProperty(VKontakteRuHoster.VKDOCS_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKDOCS_USEIDASPACKAGENAME)) {
            fpName = "docs" + owner_ID;
        } else {
            fpName = br.getRegex("\"htitle\":\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = "vk.com docs - " + owner_ID;
            }
        }
        final String alldocs = br.getRegex("cur\\.docs = \\[(.*?)\\];").getMatch(0);
        final String[] docs = alldocs.split("\\],\\[");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        for (final String docinfo : docs) {
            final String[] stringdata = new Regex(docinfo, "'([^<>\"']*?)'").getColumn(0);
            final String filesize = new Regex(docinfo, "(\\d{1,3} (?:kB|MB|GB))").getMatch(0);
            if (stringdata == null || stringdata.length < 2 || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String filename = stringdata[1];
            final String content_ID = new Regex(docinfo, "^(?:\\[)?(\\d+)").getMatch(0);
            final DownloadLink dl = getSinglePhotoDownloadLink("https://vk.com/doc" + owner_ID + "_" + content_ID, null);
            dl.setContentUrl(param.getCryptedUrl());
            dl.setName(Encoding.htmlDecode(filename));
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_owner_id, owner_ID);
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_content_id, content_ID);
            fp.add(dl);
            ret.add(dl);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlUserStory(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        this.siteGeneralErrorhandling(this.br);
        final String json = br.getRegex("cur\\['stories_list_owner_feed-?\\d+'\\]=(\\[.*?\\]);\\s+").getMatch(0);
        if (StringUtils.isEmpty(json)) {
            /* Probably user does not have a story at this moment or account is required to view those. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(json);
        if (ressourcelist.isEmpty()) {
            /* Probably user does not have a story at this moment. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> story = ressourcelist.get(0);
        final Map<String, Object> author = (Map<String, Object>) story.get("author");
        final String authorName = author.get("name").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(authorName + " - Story");
        final List<Map<String, Object>> items = (List<Map<String, Object>>) story.get("items");
        final DecimalFormat df = new DecimalFormat(String.valueOf(items.size()).replaceAll("\\d", "0"));
        int position = 0;
        for (final Map<String, Object> item : items) {
            position += 1;
            final String type = item.get("type").toString();
            String ext = null;
            String url = null;
            if (type.equals("video")) {
                url = item.get("video_url").toString();
                ext = ".mp4";
            } else if (type.equals("photo")) {
                url = item.get("photo_url").toString();
                ext = ".jpg";
            } else {
                logger.warning("Unsupported type: " + type);
                continue;
            }
            if (StringUtils.isEmpty(url)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setFinalFileName(authorName + "_" + df.format(position) + "_" + item.get("raw_id") + ext);
            dl.setAvailable(true);
            ret.add(dl);
            dl._setFilePackage(fp);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlWallClips(final CryptedLink param) throws Exception {
        final String clipCollectionName = new Regex(param.getCryptedUrl(), PATTERN_WALL_CLIPS).getMatch(0);
        this.getPage(br, param.getCryptedUrl());
        final FilePackage fp = FilePackage.getInstance();
        /* We want all links from this user/collection to go into the same package */
        fp.setAllowInheritance(true);
        fp.setName(clipCollectionName);
        return this.crawlVideos(this.br, fp);
    }

    private ArrayList<DownloadLink> crawlVideos(final Browser br, final FilePackage fp) {
        /* showInlineVideo = clips -> Short videos but internally both are the same */
        final String[] videoHTMLs = br.getRegex("(?:showVideo|showInlineVideo)\\(([^\\)]+)\\)").getColumn(0);
        final ArrayList<DownloadLink> ret = this.getReturnArray();
        for (String videoHTML : videoHTMLs) {
            if (Encoding.isHtmlEntityCoded(videoHTML)) {
                videoHTML = Encoding.htmlDecode(videoHTML);
            }
            videoHTML = videoHTML.replace("\"", "");
            videoHTML = videoHTML.replace("'", "");
            final String videoContentStr = new Regex(videoHTML, "^((?:-)?\\d+_\\d+)").getMatch(0);
            if (videoContentStr == null) {
                /* Skip invalid items */
                continue;
            }
            if (!global_dupes.add(videoContentStr)) {
                /* Important: Skip dupes so upper handling will e.g. see that nothing has been added! */
                logger.info("Skipping dupe: ");
                continue;
            }
            final String listID = new Regex(videoHTML, videoContentStr + ", ([a-f0-9]+)").getMatch(0);
            final String postID = new Regex(videoHTML, "post_id:((-)?\\d+_\\d+)").getMatch(0);
            /* Important: This URL may contain information without which crawler/hosterplugin would fail later! */
            final String contentURL;
            if (postID != null && listID != null) {
                /* Video is part of */
                contentURL = getProtocol() + getHost() + "/wall" + postID + "?z=video" + videoContentStr + "%2F" + listID;
            } else if (postID != null) {
                contentURL = getProtocol() + getHost() + "/wall" + postID + "?z=video" + videoContentStr;
            } else {
                contentURL = getProtocol() + getHost() + "/video" + videoContentStr;
            }
            final DownloadLink dl = this.createDownloadlink(contentURL);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            if (listID != null) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_LIST_ID, listID);
            }
            if (postID != null) {
                dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_wall_post_id, postID);
            }
            ret.add(dl);
        }
        return ret;
    }

    private void getPage(final String url) throws Exception {
        getPage(br, br.createGetRequest(url));
    }

    private void getPage(final Browser br, final Request request) throws Exception {
        getPage(this, null, this.br, request);
        siteGeneralErrorhandling(br);
    }

    private void getPage(final Browser br, final String url) throws Exception {
        getPage(this.br, br.createGetRequest(url));
    }

    private void apiGetPageSafe(final String url) throws Exception {
        getPage(this, null, br, br.createGetRequest(url));
        apiHandleErrors(null);
    }

    @SuppressWarnings("unused")
    private void apiPostPageSafe(final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        apiHandleErrors(null);
    }

    /**
     * Handles these error-codes: https://vk.com/dev/errors
     *
     * @return true = ready to retry, false = problem - failed!
     */
    private boolean apiHandleErrors(final Account account) throws Exception {
        final int errcode = getCurrentAPIErrorcode();
        switch (errcode) {
        case -1:
            break;
        case 1:
            logger.info("Unknown error occurred");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 2:
            logger.info("Application is disabled.");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 3:
            logger.info("Unknown method passed");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 4:
            logger.info("Incorrect signature");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 5:
            logger.info("User authorization failed");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 6:
            logger.info("Too many requests per second");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 7:
            logger.info("Permission to perform this action is denied");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 8:
            logger.info("Invalid request");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 9:
            logger.info("Flood control");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 10:
            logger.info("Internal server error");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 11:
            logger.info("In test mode application should be disabled or user should be authorized ");
            break;
        case 12:
            logger.info("Unable to compile code");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 13:
            logger.info("Runtime error occurred during code invocation");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 14:
            logger.info("Captcha needed");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 15:
            logger.info("Access denied");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 16:
            logger.info("HTTP authorization failed");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 17:
            logger.info("Validation required");
            String redirectUri = PluginJSonUtils.getJsonValue(br, "redirect_uri");
            logger.info("Redirect URI: " + redirectUri);
            if (redirectUri != null) {
                final boolean success = siteHandleSecurityCheck(this, account, br, redirectUri);
                if (success) {
                    logger.info("Verification Done");
                    return true;
                } else {
                    logger.info("Verification Failed");
                    return false;
                }
            }
        case 20:
            logger.info("Permission to perform this action is denied for non-standalone applications");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 21:
            logger.info("Permission to perform this action is allowed only for Standalone and OpenAPI applications");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 23:
            logger.info("This method was disabled");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 100:
            logger.info("One of the parameters specified was missing or invalid");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 101:
            logger.info("Invalid application API ID");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 113:
            logger.info("Invalid user id");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 150:
            logger.info("Invalid timestamp");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 200:
            logger.info("Access to album denied ");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 201:
            logger.info("Access to audio denied");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 203:
            logger.info("Access to group denied");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 300:
            logger.info("This album is full");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 500:
            logger.info("Permission denied. You must enable votes processing in application settings");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 600:
            logger.info("Permission denied. You have no access to operations specified with given object(s)");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 603:
            logger.info("Some ads error occured");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        default:
            if (errcode > -1) {
                throw new DecrypterException(EXCEPTION_API_UNKNOWN);
            }
            break;
        }
        return false;
    }

    /**
     * NO FUCKEN FOR/WHILE LOOPS HERE! <br/>
     *
     * @param br
     * @param url
     * @throws Exception
     */
    public static void getPage(final Plugin plugin, final Account account, final Browser br, final Request req) throws Exception {
        final boolean followRedirectsOld = br.isFollowingRedirects();
        try {
            int counterRedirect = 0;
            br.setFollowRedirects(false);
            String redirect = null;
            Request doRequest = req;
            do {
                br.getPage(doRequest);
                VKontakteRuHoster.handleTooManyRequests(plugin, br);
                redirect = br.getRedirectLocation();
                if (redirect != null) {
                    if (redirect.contains("act=security_check") || redirect.contains("login.vk.com/?role=fast")) {
                        if (siteHandleSecurityCheck(plugin, account, br, redirect)) {
                            VKontakteRuHoster.handleTooManyRequests(plugin, br);
                        } else {
                            throw new DecrypterException("Could not solve Security Questions");
                        }
                    } else {
                        // maybe multiple redirects before end outcome!
                        br.getPage(redirect);
                        VKontakteRuHoster.handleTooManyRequests(plugin, br);
                    }
                }
                doRequest = req.cloneRequest();
            } while ((redirect = br.getRedirectLocation()) != null && counterRedirect++ < 10);
            if (redirect != null && counterRedirect >= 10) {
                throw new DecrypterException("Too many redirects!");
            }
            /*
             * 2022-06-01: This error happens in some cases even if we do not request the same URL twice in under a second --> Retry up to
             * 10 times.
             */
            /*
             * TODO: Error can be displayed in different languages (randomly??) --> Be sure to set our preferred language [English] before!
             */
            if (containsErrorSamePageReloadTooFast(br)) {
                synchronized (VKontakteRuHoster.LOCK) {
                    final int maxAttempts = 10;
                    int counterErrorSamePageReloadTooFast = 0;
                    do {
                        counterErrorSamePageReloadTooFast++;
                        plugin.getLogger().info("You tried to load the same page more than once in one second | Attempt: " + counterErrorSamePageReloadTooFast + "/" + maxAttempts);
                        plugin.getLogger().info("URL: " + br.getURL());
                        Thread.sleep(10000);
                        br.getHeaders().put("Referer", "https://" + br.getHost() + "/");
                        doRequest = req.cloneRequest();
                        br.getPage(doRequest);
                        if (!containsErrorSamePageReloadTooFast(br)) {
                            break;
                        } else {
                            plugin.getLogger().info("Next try");
                        }
                    } while (containsErrorSamePageReloadTooFast(br) && counterErrorSamePageReloadTooFast <= maxAttempts);
                    if (containsErrorSamePageReloadTooFast(br)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You tried to load the same page more than once in one second");
                    } else {
                        plugin.getLogger().info("containsErrorSamePageReloadTooFast success after: " + counterErrorSamePageReloadTooFast);
                    }
                }
            }
        } finally {
            br.setFollowRedirects(followRedirectsOld);
        }
    }

    public static boolean containsErrorSamePageReloadTooFast(final Browser br) {
        if (br.containsHTML("(?i)You tried to load the same page more than once in one second|Вы попытались загрузить более одной однотипной страницы в секунду|Pr\\&#243;bujesz za\\&#322;adowa\\&#263; wi\\&#281;cej ni\\&#380; jedn\\&#261; stron\\&#281; w ci\\&#261;gu sekundy|Sie haben versucht die Seite mehrfach innerhalb einer Sekunde zu laden")) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns current API 'error_code', returns -1 if there is none */
    private int getCurrentAPIErrorcode() {
        final String errcodeSTR = PluginJSonUtils.getJsonValue(br, "error_code");
        if (errcodeSTR == null) {
            return -1;
        }
        return Integer.parseInt(errcodeSTR);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /** Log in via hoster plugin */
    private boolean login(final Account account, final Browser br) throws Exception {
        if (account == null) {
            logger.warning("There is no account available, continuing without logging in");
            return false;
        }
        final PluginForHost hostPlugin = getNewPluginForHostInstance(getHost());
        hostPlugin.setBrowser(br);
        try {
            ((jd.plugins.hoster.VKontakteRuHoster) hostPlugin).login(this.br, account, false);
            logger.info("Logged in successfully");
            return true;
        } catch (final PluginException e) {
            handleAccountException(hostPlugin, account, e);
            logger.exception("Login failed - continuing without login", e);
            return false;
        }
    }

    /**
     * 2019-07-26: TODO: This API call requires authorization from now on Returns the ownerID which belongs to a name e.g. vk.com/some_name
     *
     * @throws Exception
     */
    private String resolveScreenName_API(final String screenname) throws Exception {
        apiGetPageSafe("https://api.vk.com/method/resolveScreenName?screen_name=" + screenname);
        final String ownerID = PluginJSonUtils.getJsonValue(br, "object_id");
        return ownerID;
    }

    @SuppressWarnings("deprecation")
    static boolean siteHandleSecurityCheck(final Plugin plugin, final Account account, final Browser br, final String parameter) throws Exception {
        // this task shouldn't be done without an account!, ie. login should have taken place
        if (account == null) {
            throw new AccountRequiredException();
        }
        // this is effectively a login (verification) task! We should synchronise before continuing!
        synchronized (VKontakteRuHoster.LOCK) {
            final Browser ajaxBR = br.cloneBrowser();
            boolean hasPassed = false;
            ajaxBR.setFollowRedirects(true);
            ajaxBR.getPage(parameter);
            if (ajaxBR.getRedirectLocation() != null) {
                return true;
            }
            if (ajaxBR.containsHTML("missing digits")) {
                ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String phone = account.getStringProperty("phone", null);
                if (phone == null) {
                    phone = account.getUser();
                }
                if (phone != null) {
                    phone = phone.replaceAll("\\D", "");
                }
                for (int i = 0; i <= 3; i++) {
                    plugin.getLogger().info("Entering security check...");
                    final String to = ajaxBR.getRegex("to: '([^<>\"]*?)'").getMatch(0);
                    final String hash = ajaxBR.getRegex("hash: '([^<>\"]*?)'").getMatch(0);
                    if (to == null || hash == null) {
                        return false;
                    }
                    String[] preAndPost = ajaxBR.getRegex("class=\"label ta_r\">([^<]+)</div></td>.*?class=\"phone_postfix\">([^<]+)</span></td>").getRow(0);
                    if (preAndPost == null || preAndPost.length != 2) {
                        return false;
                    }
                    String end;
                    String start;
                    start = preAndPost[0].replaceAll("\\D", "");
                    end = Encoding.htmlDecode(preAndPost[1]).replaceAll("\\D", "");
                    String code = null;
                    if (phone != null) {
                        if (phone.startsWith(start) && phone.endsWith(end)) {
                            code = phone;
                        }
                    }
                    if (code == null) {
                        code = UserIO.getInstance().requestInputDialog("Please enter your phone number (Starts with " + start + " & ends with " + end + ")");
                        if (!code.startsWith(start) || !code.endsWith(end)) {
                            continue;
                        }
                    }
                    phone = code;
                    code = code.substring(start.length(), code.length() - end.length());
                    ajaxBR.postPage(getBaseURL() + "/login.php", "act=security_check&al=1&al_page=3&code=" + code + "&hash=" + Encoding.urlEncode(hash) + "&to=" + Encoding.urlEncode(to));
                    if (!ajaxBR.containsHTML(">Unfortunately, the numbers you have entered are incorrect")) {
                        hasPassed = true;
                        account.setProperty("phone", phone);
                        break;
                    } else {
                        phone = null;
                        account.setProperty("phone", Property.NULL);
                        if (ajaxBR.containsHTML("You can try again in \\d+ hour")) {
                            plugin.getLogger().info("Failed security check, account is banned for some hours!");
                            break;
                        }
                    }
                }
                return hasPassed;
            } else {
                ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                for (int i = 0; i <= 3; i++) {
                    plugin.getLogger().info("Entering security check...");
                    final String to = br.getRegex("to: '([^<>\"]*?)'").getMatch(0);
                    final String hash = br.getRegex("hash: '([^<>\"]*?)'").getMatch(0);
                    if (to == null || hash == null) {
                        return false;
                    }
                    final String code = UserIO.getInstance().requestInputDialog("Enter the last 4 digits of your phone number for vkontakte.ru :");
                    ajaxBR.postPage(getBaseURL() + "/login.php", "act=security_check&al=1&al_page=3&code=" + code + "&hash=" + Encoding.urlEncode(hash) + "&to=" + Encoding.urlEncode(to));
                    if (!ajaxBR.containsHTML(">Unfortunately, the numbers you have entered are incorrect")) {
                        hasPassed = true;
                        break;
                    }
                    if (ajaxBR.containsHTML("You can try again in \\d+ hour")) {
                        plugin.getLogger().info("Failed security check, account is banned for some hours!");
                        break;
                    }
                }
                return hasPassed;
            }
        }
    }

    public static boolean isDocument(final String url) {
        if (url.matches(VKontakteRuHoster.TYPE_DOCLINK_1) || url.matches(VKontakteRuHoster.TYPE_DOCLINK_2)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSingleVideo(final String url) {
        return url.matches(PATTERN_VIDEO_SINGLE_Z) || url.matches(PATTERN_CLIP_SINGLE_ORIGINAL) || url.matches(PATTERN_VIDEO_SINGLE_ORIGINAL) || url.matches(PATTERN_CLIP_SINGLE_Z) || url.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_WITH_LISTID) || url.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST) || url.matches(PATTERN_VIDEO_SINGLE_EMBED) || url.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH);
    }

    private static boolean isSinglePicture(final String url) {
        if (url.matches(PATTERN_PHOTO_SINGLE)) {
            return true;
        } else if (url.matches(PATTERN_PHOTO_SINGLE_Z)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSingleWallPost(final String input) {
        return input.matches(PATTERN_WALL_POST_LINK);
    }

    private static boolean isVideoAlbum(final String url) {
        return url.matches(PATTERN_VIDEO_ALBUM) || url.matches(PATTERN_VIDEO_COMMUNITY_ALBUM);
    }

    private static boolean isUserStory(final String input) {
        return input.matches(PATTERN_USER_STORY);
    }

    /**
     * Handles basic (offline) errors.
     *
     * @throws PluginException
     * @throws DecrypterRetryException
     */
    private void siteGeneralErrorhandling(final Browser br) throws DecrypterException, PluginException, DecrypterRetryException {
        /* General errorhandling start */
        if (br.containsHTML("(?i)>\\s*Unknown error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Only logged in users can see this profile\\.<")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("(?i)>\\s*Access denied|>\\s*You do not have permission to do this")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("vk.com/blank.php")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)This content is blocked in your country")) {
            /* 2022-06-01 */
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "This content is blocked in your country");
            throw new DecrypterRetryException(RetryReason.GEO, "GEO_BLOCKED", "This content is blocked in your country");
        } else if (br.containsHTML("(?i)You are not allowed to view this community")) {
            /* 2022-06-02 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"profile_deleted_text\"")) {
            /* Profile deleted or no permissions to view it */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().matches("https?://[^/]+/login\\?.*")) {
            throw new AccountRequiredException();
        }
        final String htmlrefresh = br.getRequest().getHTMLRefresh();
        if (StringUtils.containsIgnoreCase(htmlrefresh, "badbrowser.php")) {
            /**
             * If this happens user needs to change User-Agent of this plugin to continue using it. </br>
             * vk.com does not necessarily simply block a User-Agent value. They may as well just block it for specific users/IPs.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Blocked User-Agent");
        }
        /* General errorhandling end */
    }

    /** Sets basic values/cookies */
    private void prepBrowser(final Browser br) {
        VKontakteRuHoster.prepBrowser(br);
    }

    public static String generateContentURLVideo(final String oid, final String id) {
        return "https://vk.com/video" + oid + "_" + id;
    }

    @Override
    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}