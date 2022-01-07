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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
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
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.VKontakteRuHoster;
import jd.plugins.hoster.VKontakteRuHoster.Quality;
import jd.plugins.hoster.VKontakteRuHoster.QualitySelectionMode;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vk.com" }, urls = { "https?://(?:www\\.|m\\.|new\\.)?(?:(?:vk\\.com|vkontakte\\.ru|vkontakte\\.com)/(?!doc[\\d\\-]+_[\\d\\-]+|picturelink|audiolink)[a-z0-9_/=\\.\\-\\?&%]+|vk\\.cc/[A-Za-z0-9]+)" })
public class VKontakteRu extends PluginForDecrypt {
    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setBurstRequestIntervalLimitGlobal("vk.com", 500, 15, 30000);
        } catch (final Throwable e) {
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    private String getBaseURL() {
        return VKontakteRuHoster.getBaseURL();
    }

    private String getProtocol() {
        return VKontakteRuHoster.getProtocol();
    }

    private static final String     EXCEPTION_ACCPROBLEM                      = "EXCEPTION_ACCPROBLEM";
    private static final String     EXCEPTION_API_UNKNOWN                     = "EXCEPTION_API_UNKNOWN";
    /* Settings */
    private static final String     FASTLINKCHECK_PICTURES                    = "FASTLINKCHECK_PICTURES_V2";
    private static final String     FASTLINKCHECK_AUDIO                       = "FASTLINKCHECK_AUDIO";
    private static final String     VKWALL_GRAB_ALBUMS                        = "VKWALL_GRAB_ALBUMS";
    private static final String     VKWALL_GRAB_PHOTOS                        = "VKWALL_GRAB_PHOTOS";
    private static final String     VKWALL_GRAB_AUDIO                         = "VKWALL_GRAB_AUDIO";
    private static final String     VKWALL_GRAB_VIDEO                         = "VKWALL_GRAB_VIDEO";
    private static final String     VKWALL_GRAB_LINK                          = "VKWALL_GRAB_LINK";
    private static final String     VKWALL_GRAB_DOCS                          = "VKWALL_GRAB_DOCS";
    private static final String     VKAUDIOS_USEIDASPACKAGENAME               = "VKAUDIOS_USEIDASPACKAGENAME";
    private static final String     VKDOCS_USEIDASPACKAGENAME                 = "VKDOCS_USEIDASPACKAGENAME";
    /* Settings 'in action' */
    private boolean                 vkwall_grabalbums;
    private boolean                 vkwall_grabphotos;
    private boolean                 vkwall_grabaudio;
    private boolean                 vkwall_grabvideo;
    private boolean                 vkwall_grablink;
    private boolean                 vkwall_comments_grab_comments;
    private boolean                 vkwall_grabdocs;
    private boolean                 vkwall_graburlsinsideposts;
    private boolean                 vkwall_comment_grabphotos;
    private boolean                 vkwall_comment_grabaudio;
    private boolean                 vkwall_comment_grabvideo;
    private boolean                 vkwall_comment_grablink;
    private boolean                 photos_store_picture_directurls;
    private String                  vkwall_graburlsinsideposts_regex;
    private String                  vkwall_graburlsinsideposts_regex_default;
    /* Some supported url patterns */
    private static final String     PATTERN_SHORT                             = "https?://vk\\.cc/.+";
    private static final String     PATTERN_URL_EXTERN                        = "https?://[^/]+/away\\.php\\?to=.+";
    private static final String     PATTERN_GENERAL_AUDIO                     = "https?://[^/]+/audio.*?";
    private static final String     PATTERN_AUDIO_ALBUM                       = "https?://[^/]+/(?:audio(?:\\.php)?\\?id=(?:\\-)?\\d+|audios(?:\\-)?\\d+).*?";
    private static final String     PATTERN_AUDIO_PAGE                        = "https?://[^/]+/page\\-\\d+_\\d+.*?";
    private static final String     PATTERN_AUDIO_PAGE_oid                    = "https?://[^/]+/pages\\?oid=\\-\\d+\\&p=(?!va_c)[^<>/\"]+";
    private static final String     PATTERN_AUDIO_AUDIOS_ALBUM                = "https?://[^/]+/(audios\\-\\d+\\?album_id=\\d+|music/album/-?\\d+_\\d+)";
    private static final String     PATTERN_AUDIO_AUDIOS_ALBUM_2020           = "https?://[^/]+/music/album/(-?\\d+)_(\\d+).*?";
    public static final String      PATTERN_VIDEO_SINGLE_Z                    = "(?i)https?://[^/]+/.*?z=video((?:\\-)?\\d+_\\d+).*?";
    private static final String     PATTERN_CLIP_SINGLE_Z                     = "(?i)https?://[^/]+/.*?z=clip((?:\\-)?\\d+_\\d+).*?";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL             = "(?i)https?://[^/]+/video((?:\\-)?\\d+_\\d+)";
    private static final String     PATTERN_CLIP_SINGLE_ORIGINAL              = "(?i)https?://[^/]+/clip((?:\\-)?\\d+_\\d+)";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL_WITH_LISTID = "https?://[^/]+/video(\\-)?\\d+_\\d+\\?listid=[a-z0-9]+";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL_LIST        = "https?://[^/]+/video(\\-)?\\d+_\\d+\\?list=[a-z0-9]+";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED                = "https?://[^/]+/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+.*?";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED_HASH           = "https?://[^/]+/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+\\&hash=[a-z0-9]+.*?";
    private static final String     PATTERN_VIDEO_ALBUM                       = "https?://[^/]+/(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged|videos(?:-)?\\d+(?:\\?section=[^\\&]+)?)";
    private static final String     PATTERN_VIDEO_COMMUNITY_ALBUM             = "https?://[^/]+/video\\?gid=\\d+.*";
    private static final String     PATTERN_PHOTO_SINGLE                      = "https?://[^/]+/photo(\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_PHOTO_SINGLE_Z                    = "https?://[^/]+/.+z=photo(?:\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_PHOTO_MODULE                      = "https?://[^/]+/[A-Za-z0-9\\-_\\.]+\\?z=photo(\\-)?\\d+_\\d+/(wall|album)\\-\\d+_\\d+";
    private static final String     PATTERN_PHOTO_ALBUM                       = ".*?(tag|album(?:\\-)?\\d+_|photos(?:\\-)?)\\d+";
    private static final String     PATTERN_PHOTO_ALBUMS                      = "https?://[^/]+/.*?albums((?:\\-)?\\d+)";
    private static final String     PATTERN_GENERAL_WALL_LINK                 = "https?://[^/]+/wall(?:\\-)?\\d+(?:\\?maxoffset=\\d+\\&currentoffset=\\d+)?";
    private static final String     PATTERN_USER_STORY                        = "https?://[^/]+/[^\\?]+\\?w=story-?(\\d+)_(\\d+).*";
    private static final String     PATTERN_WALL_LOOPBACK_LINK                = "https?://[^/]+/wall\\-\\d+.*maxoffset=(\\d+)\\&currentoffset=(\\d+).*";
    private static final String     PATTERN_WALL_POST_LINK                    = ".+wall(?:\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_WALL_POST_LINK_2                  = "https?://[^/]+/wall\\-\\d+.+w=wall(?:\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_WALL_CLIPS                        = "(?)https?://[^/]+/clips/([^/]+)";
    private static final String     PATTERN_PUBLIC_LINK                       = "https?://[^/]+/public\\d+";
    private static final String     PATTERN_CLUB_LINK                         = "https?://[^/]+/club\\d+.*?";
    private static final String     PATTERN_EVENT_LINK                        = "https?://[^/]+/event\\d+";
    private static final String     PATTERN_ID_LINK                           = "https?://[^/]+/id\\d+";
    private static final String     PATTERN_DOCS                              = "https?://[^/]+/docs\\?oid=\\-\\d+";
    /* Some html text patterns: English, Russian, German, Polish */
    public static final String      TEMPORARILYBLOCKED                        = "You tried to load the same page more than once in one second|Вы попытались загрузить более одной однотипной страницы в секунду|Pr\\&#243;bujesz za\\&#322;adowa\\&#263; wi\\&#281;cej ni\\&#380; jedn\\&#261; stron\\&#281; w ci\\&#261;gu sekundy|Sie haben versucht die Seite mehrfach innerhalb einer Sekunde zu laden";
    private static final String     FILEOFFLINE                               = "(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)";
    /* Possible/Known types of single vk-wall-posts */
    private static final String     wallpost_type_photo                       = "photo";
    private static final String     wallpost_type_doc                         = "doc";
    private static final String     wallpost_type_audio                       = "audio";
    private static final String     wallpost_type_link                        = "link";
    private static final String     wallpost_type_video                       = "video";
    private static final String     wallpost_type_album                       = "album";
    private static final String     wallpost_type_poll                        = "poll";
    /* Internal settings / constants */
    /*
     * Whenever we found this number of links or more, quit the decrypter and add a [b]LOOPBACK_LINK[/b] to continue later in order to avoid
     * memory problems/freezes.
     */
    private static final short      MAX_LINKS_PER_RUN                         = 5000;
    /* Used whenever we request arrays via API */
    private static final int        API_MAX_ENTRIES_PER_REQUEST               = 100;
    private SubConfiguration        cfg                                       = null;
    private static final String     MAINPAGE                                  = "https://vk.com";
    private String                  CRYPTEDLINK_FUNCTIONAL                    = null;
    private String                  CRYPTEDLINK_ORIGINAL                      = null;
    private boolean                 fastcheck_photo                           = false;
    private boolean                 fastcheck_audio                           = false;
    private boolean                 vkwall_use_api                            = false;
    private final boolean           docs_add_unique_id                        = true;
    private ArrayList<DownloadLink> decryptedLinks                            = null;
    private ArrayList<String>       global_dupes                              = new ArrayList<String>();
    /* Properties especially for DownloadLinks which go back into this crawler */
    private final String            VIDEO_PROHIBIT_FASTCRAWL                  = "prohibit_fastcrawl";

    /* General error handling language implementation: English | Rus | Polish */
    /*
     * Information: General link structure: vk.com/ownerID_contentID --> ownerID is always positive for users, negative for communities and
     * groups.
     */
    @SuppressWarnings({ "deprecation", "serial" })
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // nullify existing this setters, as they can carry over from previous decryption tasks.
        account = null;
        this.CRYPTEDLINK_ORIGINAL = param.toString();
        this.decryptedLinks = new ArrayList<DownloadLink>() {
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
        br.setFollowRedirects(true);
        /* Set settings */
        cfg = SubConfiguration.getConfig("vk.com");
        fastcheck_photo = cfg.getBooleanProperty(FASTLINKCHECK_PICTURES, false);
        fastcheck_audio = cfg.getBooleanProperty(FASTLINKCHECK_AUDIO, false);
        vkwall_grabalbums = cfg.getBooleanProperty(VKWALL_GRAB_ALBUMS, false);
        vkwall_grabphotos = cfg.getBooleanProperty(VKWALL_GRAB_PHOTOS, false);
        vkwall_grabaudio = cfg.getBooleanProperty(VKWALL_GRAB_AUDIO, false);
        vkwall_grabvideo = cfg.getBooleanProperty(VKWALL_GRAB_VIDEO, false);
        vkwall_grablink = cfg.getBooleanProperty(VKWALL_GRAB_LINK, false);
        vkwall_grabdocs = cfg.getBooleanProperty(VKWALL_GRAB_DOCS, false);
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
        prepCryptedLink();
        /* Check/fix links before browser access START */
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_SHORT)) {
            br.setFollowRedirects(false);
            getPage(br, CRYPTEDLINK_ORIGINAL);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("vk.com: Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_URL_EXTERN)) {
            final String finallink = new Regex(CRYPTEDLINK_ORIGINAL, "\\?to=(.+)").getMatch(0);
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else if (isTypeSinglePicture(this.CRYPTEDLINK_ORIGINAL)) {
            /**
             * Single photo links, those are just passed to the hoster plugin! Example:http://vk.com/photo125005168_269986868
             */
            decryptWallPostSpecifiedPhoto(param);
            return decryptedLinks;
        } else if (isTypeSingleVideo(CRYPTEDLINK_ORIGINAL)) {
            if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_Z) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
                CRYPTEDLINK_FUNCTIONAL = MAINPAGE + "/" + new Regex(CRYPTEDLINK_ORIGINAL, "(video(?:\\-)?\\d+_\\d+)").getMatch(0);
            }
        }
        getUserLogin(false);
        if (needsAccount(param.getCryptedUrl()) && this.account == null) {
            throw new AccountRequiredException();
        }
        try {
            prepCryptedLink();
            /* Replace section start */
            String newLink = CRYPTEDLINK_FUNCTIONAL;
            if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PUBLIC_LINK) || isClubUrl(this.CRYPTEDLINK_FUNCTIONAL) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_EVENT_LINK)) {
                /* group and club links --> wall links */
                newLink = MAINPAGE + "/wall-" + new Regex(CRYPTEDLINK_FUNCTIONAL, "vk\\.com/[a-z]+((\\-)?\\d+)").getMatch(0);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_ID_LINK)) {
                /* Change id links -> albums links */
                newLink = MAINPAGE + "/albums" + new Regex(CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_WALL_LOOPBACK_LINK)) {
                /* Remove loopback-part as it only contains information which we need later but not in the link */
                newLink = new Regex(CRYPTEDLINK_FUNCTIONAL, "(https?://(www\\.)?vk\\.com/wall(\\-)?\\d+)").getMatch(0);
            } else if (this.CRYPTEDLINK_ORIGINAL.matches(PATTERN_AUDIO_ALBUM)) {
                newLink = "https://vk.com/audios" + new Regex(CRYPTEDLINK_FUNCTIONAL, "(?:audio(?:\\.php)?\\?id=|audios)((?:\\-)?\\d+)").getMatch(0);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_PAGE)) {
                /* PATTERN_AUDIO_PAGE RegEx is wide open --> Make sure that our URL is correct! */
                final String pageID = get_ID_PAGE(this.CRYPTEDLINK_FUNCTIONAL);
                newLink = getBaseURL() + "/page-" + pageID;
            } else if (this.CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_POST_LINK_2)) {
                newLink = getBaseURL() + "/wall" + new Regex(this.CRYPTEDLINK_ORIGINAL, "((?:\\-)?\\d+_\\d+)").getMatch(0);
            } else if (isKnownType(CRYPTEDLINK_ORIGINAL)) {
                /* Don't change anything */
            } else {
                /* We either have a public community or profile --> Do not change URL */
            }
            if (newLink.equals(CRYPTEDLINK_FUNCTIONAL)) {
                logger.info("Link was not changed, continuing with: " + CRYPTEDLINK_FUNCTIONAL);
            } else {
                logger.info("Link was changed!");
                logger.info("Old link:\r\nOLD:" + CRYPTEDLINK_FUNCTIONAL);
                logger.info("NEW: " + newLink);
                CRYPTEDLINK_FUNCTIONAL = newLink;
            }
            /* Replace section end */
            /* Decryption process START */
            if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_MODULE)) {
                decryptWallPostSpecifiedPhoto(param);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_AUDIOS_ALBUM_2020)) {
                decryptAudiosAlbum2020();
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_GENERAL_AUDIO)) {
                if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_ALBUM)) {
                    /* Audio album */
                    decryptAudioAlbum();
                } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_AUDIOS_ALBUM)) {
                    decryptAudiosAlbum();
                } else {
                    /* Single playlists */
                    decryptAudioPlaylist();
                }
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_PAGE) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_PAGE_oid)) {
                /* Audio page */
                crawlAudioPage();
            } else if (isTypeSingleVideo(CRYPTEDLINK_FUNCTIONAL)) {
                /* Single video */
                crawlSingleVideo(param);
            } else if (isVideoAlbum(param.getCryptedUrl())) {
                /* Video album */
                crawlVideoAlbum(param);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_ALBUM)) {
                /**
                 * Photo album Examples: http://vk.com/photos575934598 http://vk.com/id28426816 http://vk.com/album87171972_0
                 */
                crawlPhotoAlbumWebsite(param);
            } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_ALBUMS)) {
                /**
                 * Photo albums lists/overviews Example: http://vk.com/albums46486585
                 */
                crawlPhotoAlbums_Website(param);
            } else if (isSingleWallPost(this.CRYPTEDLINK_FUNCTIONAL)) {
                /**
                 * Single posts of wall links: https://vk.com/wall-28122291_906
                 */
                crawlWallPost(param);
                if (decryptedLinks.size() == 0) {
                    logger.info("Check your plugin settings -> They affect the results!");
                    logger.info("Also make sure that you have to rights to access this content e.g. sometimes an account is required.");
                }
            } else if (param.getCryptedUrl().matches(PATTERN_DOCS)) {
                crawlDocs(param);
            } else if (param.getCryptedUrl().matches(PATTERN_WALL_CLIPS)) {
                crawlWallClips(param);
            } else if (isUserStory(param.getCryptedUrl())) {
                this.crawlUserStory(param);
            } else {
                /* Wall link or unsupported link! */
                // no requests have been made yet, why are we checking for this? -raztoki20160809
                crawlWallLink(param);
                logger.info("Decrypted " + decryptedLinks.size() + " total links out of a wall-link");
                if (decryptedLinks.size() == 0) {
                    logger.info("Check your plugin settings -> They affect the results!");
                }
            }
        } catch (final DecrypterException e) {
            if (e.getMessage() != null) {
                if (e.getMessage().equals(EXCEPTION_ACCPROBLEM)) {
                    logger.info("Account problem! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                    return decryptedLinks;
                } else if (e.getMessage().equals(EXCEPTION_API_UNKNOWN)) {
                    logger.info("Unknown API problem occured! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                    return decryptedLinks;
                }
            }
            throw e;
        }
        if (decryptedLinks == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            logger.info("vk.com: Done, decrypted: " + decryptedLinks.size() + " links!");
        }
        return decryptedLinks;
    }

    private static boolean needsAccount(final String url) {
        /* TODO: Add more linktypes here! */
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
        } else if (isTypeSingleVideo(url)) {
            return true;
        } else if (isUserStory(url)) {
            return true;
        } else {
            final boolean isKnown = url.matches(PATTERN_VIDEO_SINGLE_Z) || url.matches(PATTERN_PHOTO_ALBUM) || url.matches(PATTERN_PHOTO_ALBUMS) || url.matches(PATTERN_AUDIO_PAGE) || url.matches(PATTERN_GENERAL_WALL_LINK) || url.matches(PATTERN_GENERAL_AUDIO) || url.matches(PATTERN_WALL_POST_LINK) || url.matches(PATTERN_PHOTO_MODULE) || url.matches(PATTERN_AUDIO_PAGE_oid) || url.matches(PATTERN_DOCS);
            return isKnown;
        }
    }

    private boolean isClubUrl(final String url) {
        return new Regex(url, PATTERN_CLUB_LINK).matches() && !new Regex(url, PATTERN_PHOTO_MODULE).matches() && !isTypeSingleVideo(url);
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private void decryptAudioAlbum() throws Exception {
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((?:\\-)?\\d+)$").getMatch(0);
        final String album_id = new Regex(this.CRYPTEDLINK_ORIGINAL, "album_id=(\\d+)").getMatch(0);
        String fpName = null;
        if (cfg.getBooleanProperty(VKAUDIOS_USEIDASPACKAGENAME, false)) {
            fpName = "audios" + owner_ID;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String postData;
        if (album_id != null) {
            postData = "act=load_silent&al=1&album_id=" + album_id + "&band=false&owner_id=" + owner_ID;
        } else {
            postData = "access_hash=&act=load_section&al=1&claim=0&offset=0&is_loading_all=1&owner_id=" + owner_ID + "&playlist_id=-1&type=playlist";
        }
        br.postPage(getBaseURL() + "/al_audio.php", postData);
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        if (StringUtils.isEmpty(fpName)) {
            fpName = (String) entries.get("title");
        }
        if (StringUtils.isEmpty(fpName)) {
            /* Last chance fallback */
            fpName = "vk.com audio - " + owner_ID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        /* TODO: 2020-05-11: Fix this */
        final ArrayList<Object> audioData = (ArrayList<Object>) entries.get("payload");
        if (audioData == null || audioData.size() == 0) {
            logger.info("Nothing found --> Probably offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        addJsonAudioObjects(audioData, fp);
    }

    /** NOT using API audio pages and audio playlist's are similar, TODO: Return host-plugin links here to improve the overall stability. */
    private void decryptAudiosAlbum() throws Exception {
        this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("id=\"not_found\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String audiotabletext = br.getRegex("<table class=\"audio_table\" cellspacing=\"0\" cellpadding=\"0\">(.*?)</table>").getMatch(0);
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "audios((?:\\-)?\\d+)").getMatch(0);
        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "album_id=(\\d+)").getMatch(0);
        final String fpName;
        if (cfg.getBooleanProperty(VKAUDIOS_USEIDASPACKAGENAME, false)) {
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
            decryptedLinks = null;
            return;
        }
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
                decryptedLinks = null;
                return;
            }
            final String linkid = owner_id + "_" + content_id;
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + linkid);
            dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
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
            dl.setContentUrl(this.CRYPTEDLINK_FUNCTIONAL);
            dl.setProperty("directlink", finallink);
            fp.add(dl);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
    }

    private void decryptAudiosAlbum2020() throws Exception {
        this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, PATTERN_AUDIO_AUDIOS_ALBUM_2020).getMatch(0);
        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, PATTERN_AUDIO_AUDIOS_ALBUM_2020).getMatch(1);
        final String fpName;
        if (cfg.getBooleanProperty(VKAUDIOS_USEIDASPACKAGENAME, false)) {
            fpName = "audios" + owner_ID;
        } else {
            fpName = "audios_album " + albumID;
        }
        FilePackage fp = null;
        fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final String json = br.getRegex("new PlayList\\(\\d+,\\s*(\\{.*?)\\);").getMatch(0);
        if (json == null) {
            /* Fallback */
            websiteCrawlContent(this.CRYPTEDLINK_FUNCTIONAL, br.toString(), fp, true, false, false, false, false, false);
            logger.info("Found " + decryptedLinks.size() + " items");
            return;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
        final ArrayList<Object> audiolist = (ArrayList<Object>) entries.get("list");
        addJsonAudioObjects(audiolist, fp);
        logger.info("Found " + decryptedLinks.size() + " items");
    }

    /** NOT using API audio pages and audio playlist's are similar, TODO: Return host-plugin links here to improve the overall stability. */
    private void decryptAudioPlaylist() throws Exception {
        FilePackage fp = FilePackage.getInstance();
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((?:\\-)?\\d+)_\\d+$").getMatch(0);
        final String playlist_id = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "_(\\d+)$").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(getBaseURL() + "/al_audio.php", "access_hash=&act=load_section&al=1&claim=0&is_loading_all=1&offset=0&owner_id=" + owner_ID + "&playlist_id=-1&type=playlist");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(regexJsonInsideHTML(this.br));
        String fpName = (String) entries.get("title");
        if (StringUtils.isEmpty(fpName)) {
            /* Last chance fallback */
            fpName = owner_ID + "_" + playlist_id;
        }
        /* Important! Even inside json, they html_encode some Strings! */
        fpName = Encoding.htmlDecode(fpName.trim());
        fp.setName(fpName);
        final ArrayList<Object> audioData = (ArrayList<Object>) entries.get("list");
        if (audioData == null || audioData.size() == 0) {
            logger.info("Nothing found --> Probably offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        addJsonAudioObjects(audioData, fp);
    }

    /** Creates DownloadLink Objects out of Objects of vk Audio Albums / Playlists after request of '/al_audio.php' */
    private void addJsonAudioObjects(final ArrayList<Object> audioData, final FilePackage fp) {
        for (final Object audioDataSingle : audioData) {
            final ArrayList<Object> singleAudioDataAsArray = (ArrayList<Object>) audioDataSingle;
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
                decryptedLinks = null;
                return;
            }
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + owner_id + "_" + content_id);
            dl.setContentUrl(this.CRYPTEDLINK_FUNCTIONAL);
            dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
            if (directlink != null && directlink.startsWith("http")) {
                dl.setProperty("directlink", directlink);
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
            decryptedLinks.add(dl);
        }
    }

    /** Returns json inside html inside '<!json>'-tag, typically after request '/al_audio.php' */
    public static String regexJsonInsideHTML(final Browser br) {
        return br.getRegex("<\\!json>(.*?)<\\!>").getMatch(0);
    }

    /**
     * NOT Using API, TODO: Return host-plugin links here to improve the overall stability.
     *
     * @throws Exception
     */
    private void crawlAudioPage() throws Exception {
        this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("Page not found") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_PAGE_oid) && fpName == null) {
            fpName = Encoding.htmlDecode(new Regex(CRYPTEDLINK_FUNCTIONAL, "\\&p=(.+)").getMatch(0));
        } else if (fpName == null) {
            final String pageID = get_ID_PAGE(this.CRYPTEDLINK_FUNCTIONAL);
            fpName = "vk.com page " + pageID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        // onclick="return nav.go(this, event);">KUNIO</a></b> &ndash; <span class="title" id="title-5010876_215480904_1">BUBBLEMAN -
        // LOVE&SNOW MIX [From ROCKMAN 2] </span><span
        final String[][] audioLinks = br.getRegex("\"(https?://[a-z0-9]+\\.(vk\\.com|userapi\\.com|vk\\.me)/[^<>\"]+/audio[^<>\"]*?)\".*?onclick=\"return nav\\.go\\(this, event\\);\">([^<>\"]*?)</a></b> \\&ndash; <span class=\"title\" id=\"title(?:\\-)?\\d+_\\d+_\\d+\">([^<>\"]*?)</span>").getMatches();
        if (audioLinks == null || audioLinks.length == 0) {
            decryptedLinks = null;
            return;
        }
        for (String audioInfo[] : audioLinks) {
            String finallink = audioInfo[0];
            if (finallink == null) {
                decryptedLinks = null;
                return;
            }
            finallink = "directhttp://" + finallink;
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
            /* Set filename so we have nice filenames for our directhttp links */
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[2].trim()) + " - " + Encoding.htmlDecode(audioInfo[3].trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            fp.add(dl);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
    }

    private final boolean containsErrorTitle(final Browser br) {
        final boolean result = br.containsHTML("<div class=\"message_page_title\">Error</div>");
        return result;
    }

    /** 2016-08-11: Using website, API not anymore! */
    private void crawlSingleVideo(final CryptedLink param) throws Exception {
        final String[] ids = findVideoIDs(param.getCryptedUrl());
        final String oid = ids[0];
        final String id = ids[1];
        final String oid_and_id = oid + "_" + id;
        String listID;
        if (param.getCryptedUrl().matches(PATTERN_VIDEO_SINGLE_Z)) {
            listID = new Regex(param.getCryptedUrl(), "z=video-?\\d+_\\d+(?:%2F|/)([A-Za-z0-9\\-_]+)").getMatch(0);
        } else {
            listID = UrlQuery.parse(param.getCryptedUrl()).get("listid");
            if (listID == null && param.getDownloadLink() != null) {
                listID = param.getDownloadLink().getStringProperty(VKontakteRuHoster.PROPERTY_VIDEO_LIST_ID);
            }
        }
        /* Check if fast-crawl is allowed */
        final QualitySelectionMode qualitySelectionMode = VKontakteRuHoster.getSelectedVideoQualitySelectionMode();
        final boolean userWantsMultipleQualities = qualitySelectionMode == QualitySelectionMode.ALL;
        final boolean linkCanBeFastCrawled = param.getDownloadLink() != null && !param.getDownloadLink().hasProperty(VIDEO_PROHIBIT_FASTCRAWL) && param.getDownloadLink().hasProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN);
        if (this.cfg.getBooleanProperty(VKontakteRuHoster.FASTCRAWL_VIDEO, VKontakteRuHoster.default_FASTCRAWL_VIDEO) && !userWantsMultipleQualities && linkCanBeFastCrawled) {
            final DownloadLink dl = this.createDownloadlink(param.getDownloadLink().getPluginPatternMatcher());
            /* Inherit all previously set properties */
            dl.setProperties(param.getDownloadLink().getProperties());
            dl.setFinalFileName(param.getDownloadLink().getStringProperty(VKontakteRuHoster.PROPERTY_GENERAL_TITLE_PLAIN) + "_fastcrawl.mp4");
            dl.setAvailable(true);
            this.decryptedLinks.add(dl);
            return;
        }
        try {
            br.setFollowRedirects(false);
            // webui, youtube stuff within -raztoki20160817
            VKontakteRuHoster.accessVideo(this.br, param.getCryptedUrl(), oid, id, listID);
            handleVideoErrors(br);
            String embeddedVideoURL = new Regex(PluginJSonUtils.unescape(br.toString()), "<iframe [^>]*src=('|\")(.*?)\\1").getMatch(1);
            if (embeddedVideoURL != null) {
                if (embeddedVideoURL.startsWith("//")) {
                    embeddedVideoURL = "https:" + embeddedVideoURL;
                }
                decryptedLinks.add(createDownloadlink(embeddedVideoURL));
                return;
            }
            final Map<String, Object> video = findVideoMap(this.br, id);
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
            final Map<String, String> foundQualities = findAvailableVideoQualities(video);
            if (foundQualities == null || foundQualities.isEmpty()) {
                /* Assume that content is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final FilePackage fp = FilePackage.getInstance();
            if (cfg.getBooleanProperty(VKontakteRuHoster.VKVIDEO_USEIDASPACKAGENAME, VKontakteRuHoster.default_VKVIDEO_USEIDASPACKAGENAME)) {
                fp.setName("video" + oid + "_" + id);
            } else {
                fp.setName(titleToUse);
            }
            final Map<String, String> selectedQualities = getSelectedVideoQualities(foundQualities, qualitySelectionMode, VKontakteRuHoster.getPreferredQualityString());
            if (selectedQualities.isEmpty()) {
                logger.info("User has selected unavailable qualities only (or only unknown qualities are available)");
                return;
            }
            final boolean fastLinkcheck = cfg.getBooleanProperty(VKontakteRuHoster.FASTLINKCHECK_VIDEO, true);
            final PluginForHost plugin = JDUtilities.getPluginForHost(getHost());
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
                dl.setProperty("directlink", finallink);
                dl.setProperty(VKontakteRuHoster.PROPERTY_VIDEO_SELECTED_QUALITY, thisQuality);
                dl.setProperty("nologin", true);
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
                if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
        } catch (final DecrypterException de) {
            throw de;
        }
    }

    public static Map<String, Object> findVideoMap(final Browser br, final String videoid) throws Exception {
        String json = br.getRegex("ajax\\.preload\\('al_video\\.php', \\{.*?\\}, (\\[.+)").getMatch(0);
        if (json == null) {
            /* E.g. information has been loaded via ajax request e.g. as part of a wall post/playlist */
            json = br.getRegex("^<\\!--(\\{.+\\})$").getMatch(0);
        }
        return (Map<String, Object>) recursiveFindVideoMap(JavaScriptEngineFactory.jsonToJavaObject(json), videoid);
    }

    public static Object recursiveFindVideoMap(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> cookieEntry : entrymap.entrySet()) {
                final String key = cookieEntry.getKey();
                final Object value = cookieEntry.getValue();
                if (key.equals("vid") && ((Number) value).toString().equals(videoid) && entrymap.containsKey("hd")) {
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

    private void handleVideoErrors(final Browser br) throws DecrypterException, PluginException {
        final boolean isError = containsErrorTitle(br);
        if ((isError && br.containsHTML("div class=\"message_page_body\">\\s+You need to be a member of this group to view its video files.")) || br.getHttpConnection().getResponseCode() == 403) {
            throw new AccountRequiredException();
        } else if (isError && br.containsHTML("<div class=\"message_page_body\">\\s*Access denied")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("The owner of this video has either been suspended or deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().contains("<\\/b> was removed from public access by request of the copyright holder.<\\/div>\\n<\\/div>\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().contains("This video is not available in your region")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.toString().contains("id=\"video_ext_msg\"")) {
            /* 2017-11-21: Basic trait for all kinds of errormessage (shall be language-independent) */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("profile_deleted_text")) {
            /* 2019-07-26: E.g. <h5 class="profile_deleted_text">Dieses Profil ist nur f&#252;r autorisierte Nutzer verf&#252;gbar.</h5> */
            /* 2020-11-30: E.g. <h5 class="profile_deleted_text">You have to log in to view this page.</h5> */
            /*
             * 2020-11-30: E.g. <h5 class="profile_deleted_text">This profile has been deleted.<br>Information on this profile is
             * unavailable.</h5>
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    private void crawlPhotoAlbumWebsite(final CryptedLink param) throws Exception {
        if (this.CRYPTEDLINK_FUNCTIONAL.contains("#/album")) {
            this.CRYPTEDLINK_FUNCTIONAL = getProtocol() + "vk.com/album" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(".*?vk\\.com/id(?:\\-)?\\d+")) {
            this.CRYPTEDLINK_FUNCTIONAL = this.CRYPTEDLINK_FUNCTIONAL.replaceAll("vk\\.com/|id(?:\\-)?", "vk.com/album") + "_0";
        }
        this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML(FILEOFFLINE) || br.containsHTML("В альбоме нет фотографий|<title>DELETED</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("There are no photos in this album")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) zdj&#281").getMatch(0);
        if (numberOfEntrys == null) {
            numberOfEntrys = br.getRegex("count\\s*:\\s*(\\d+),").getMatch(0);
            if (numberOfEntrys == null) {
                numberOfEntrys = br.getRegex("</a>(\\d+) zdj\\&#281;\\&#263;<span").getMatch(0);
                if (numberOfEntrys == null) {
                    numberOfEntrys = br.getRegex("\"count\":(\\d+)").getMatch(0);
                    if (numberOfEntrys == null) {
                        numberOfEntrys = br.getRegex(">(\\d+) photos in the album<").getMatch(0);
                    }
                }
            }
        }
        final String startOffset = br.getRegex("var preload\\s*=\\s*\\[(\\d+),\"").getMatch(0);
        /* 2019-10-25: If not found, stop after the first page! */
        // if (numberOfEntrys == null) {
        // throw new DecrypterException("Can not find 'numberOfEntries'");
        // }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(-?(\\d+_)?\\d+)$").getMatch(0));
        fp.setProperty("CLEANUP_NAME", false);
        final int page_start_value = 0;
        int page = page_start_value;
        int offset = 0;
        if (startOffset != null) {
            offset = Integer.parseInt(startOffset);
        }
        int addedLinks = 0;
        do {
            addedLinks = 0;
            page++;
            if (page > 1) {
                this.postPageSafe(param, br.getURL(), "al=1&al_ad=0&part=1&rev=&offset=" + offset);
            }
            final int linksnumBefore = decryptedLinks.size();
            websiteCrawlContent(this.CRYPTEDLINK_FUNCTIONAL, br.toString(), fp, false, false, true, false, false, this.photos_store_picture_directurls);
            final int linksnumAfter = decryptedLinks.size();
            addedLinks = linksnumAfter - linksnumBefore;
            if (page > page_start_value + 1 || startOffset == null) {
                offset += addedLinks;
            }
        } while (addedLinks >= 40 && numberOfEntrys != null && !this.isAbort());
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID, final String picture_preview_json) throws IOException {
        final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + photoID);
        dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
        if (fastcheck_photo) {
            dl.setAvailable(true);
        }
        final String dllink_temp = VKontakteRuHoster.getHighestQualityPicFromSavedJson(picture_preview_json);
        final String tempFilename = VKontakteRuHoster.photoGetFinalFilename(photoID, null, dllink_temp);
        dl.setName(tempFilename);
        dl.setContentUrl(getProtocol() + "vk.com/photo" + photoID);
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
        return dl;
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID) throws IOException {
        return getSinglePhotoDownloadLink(photoID, null);
    }

    private void crawlPhotoAlbums_Website(final CryptedLink param) throws NumberFormatException, Exception {
        /*
         * Another possibility to get these (but still no API): https://vk.com/al_photos.php act=show_albums&al=1&owner=<owner_id> AblumsXXX
         * --> XXX may also be the owner_id, depending on linktype.
         */
        getPage(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("class=\"photos_no_content\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String type = "multiplephotoalbums";
        if (this.CRYPTEDLINK_FUNCTIONAL.contains("z=")) {
            this.CRYPTEDLINK_FUNCTIONAL = getProtocol() + "vk.com/albums" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, PATTERN_PHOTO_ALBUMS).getMatch(0);
            if (!this.CRYPTEDLINK_FUNCTIONAL.equalsIgnoreCase(br.getURL())) {
                getPage(this.CRYPTEDLINK_FUNCTIONAL);
            }
        } else {
            /* not needed as we already have requested this page */
            // getPage(br,parameter);
        }
        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, PATTERN_PHOTO_ALBUMS).getMatch(0);
        if (albumID == null) {
            decryptedLinks = null;
            return;
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
            numberOfEntriesStr = br.getRegex("Show all (\\d+) albums").getMatch(0);
        }
        if (numberOfEntriesStr == null) {
            /* 2020-1^0-26 */
            numberOfEntriesStr = br.getRegex(">Photo Albums<span class=\"ui_crumb_count\">(\\d+)<").getMatch(0);
        }
        final String startOffset = br.getRegex("var preload\\s*=\\s*\\[(\\d+),\"").getMatch(0);
        if (numberOfEntriesStr == null) {
            this.handleVideoErrors(this.br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        numberOfEntriesStr = numberOfEntriesStr.replace(",", "");
        final int numberOfEntries = (int) StrictMath.ceil((Double.parseDouble(numberOfEntriesStr)));
        final int entries_per_page = 26;
        final int entries_alreadyOnPage = 0;
        logger.info("Decrypting " + numberOfEntriesStr + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((numberOfEntries - entries_alreadyOnPage) / entries_per_page);
        if (maxLoops < 0) {
            maxLoops = 0;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinksTotal = 0;
        int addedLinks;
        int realOffset = 0;
        if (startOffset != null) {
            realOffset = Integer.parseInt(startOffset);
        }
        for (int i = 0; i <= maxLoops; i++) {
            addedLinks = 0;
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                break;
            }
            final String correctedBR;
            if (i > 0) {
                postPageSafe(param, this.CRYPTEDLINK_FUNCTIONAL, "al=1&al_ad=0&part=1&offset=" + Math.max(0, realOffset - 1));
            }
            logger.info("Parsing page " + (i + 1) + " of " + (maxLoops + 1));
            correctedBR = br.toString().replace("\\", "");
            final int linksnumBefore = decryptedLinks.size();
            websiteCrawlContent(this.CRYPTEDLINK_FUNCTIONAL, correctedBR, fp, false, false, false, false, false, false);
            final int linksnumAfter = decryptedLinks.size();
            addedLinks = linksnumAfter - linksnumBefore;
            addedLinksTotal += addedLinks;
            if (startOffset == null || i > 0) {
                realOffset += addedLinks;
            }
            logger.info("Added items from this page: " + addedLinks);
            if (addedLinks == 0 || addedLinksTotal >= numberOfEntries) {
                logger.info("Fail safe #1 activated, stopping page parsing at page " + (i + 1) + " of " + (maxLoops + 1) + ", returning " + addedLinksTotal + " results");
                break;
            }
            sleep(this.cfg.getLongProperty(VKontakteRuHoster.SLEEP_PAGINATION_GENERAL, VKontakteRuHoster.defaultSLEEP_PAGINATION_GENERAL), param);
        }
    }

    private void crawlVideoAlbum(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        handleVideoErrors(br);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String internalSectionName;
        final String sectionNameURL = UrlQuery.parse(br.getURL()).get("section");
        if (sectionNameURL != null) {
            internalSectionName = sectionNameURL;
        } else {
            internalSectionName = "all";
        }
        String albumsJson = br.getRegex("extend\\(cur, (\\{\"albumsPreload\".*?\\})\\);\\s+").getMatch(0);
        if (albumsJson == null) {
            /* Wider RegEx */
            albumsJson = br.getRegex("extend\\(cur, (\\{\".*?\\})\\);\\s+").getMatch(0);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(albumsJson, TypeRef.HASHMAP);
        final String oid = Integer.toString(((Number) entries.get("oid")).intValue());
        final int maxItemsPerPage = ((Number) entries.get("VIDEO_SILENT_VIDEOS_CHUNK_SIZE")).intValue();
        Map<String, Object> videoInfoMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "pageVideosList/" + oid + "/" + internalSectionName);
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
                /* TODO: Convert this html String to String and set it as a DownloadLink property and/or even make use of it. */
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
                this.decryptedLinks.add(dl);
                offset++;
            }
            logger.info("Page: " + page + " | Crawled: " + offset + " / " + numberofVideos);
            if (!foundNewItems) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (offset >= numberofVideos) {
                logger.info("Stopping because: Found all items");
                break;
            } else if (this.isAbort()) {
                break;
            }
            final UrlQuery query = new UrlQuery();
            query.add("al", "1");
            query.add("need_albums", "0");
            query.add("offset", Integer.toString(offset));
            query.add("oid", oid);
            // query.add("rowlen", "3");
            if (!StringUtils.isEmpty(internalSectionName)) {
                query.add("section", Encoding.urlEncode(internalSectionName));
            }
            br.postPage(getBaseURL() + "/al_video.php?act=load_videos_silent", query.toString());
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            videoInfoMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/{1}/{0}/" + internalSectionName);
            page += 1;
        }
        logger.info("Total videolinks found: " + offset);
    }

    /**
     * Same function in hoster and decrypter plugin, sync it!!
     *
     * @throws Exception
     */
    public static Map<String, String> findAvailableVideoQualities(final Map<String, Object> video) throws Exception {
        final Map<String, String> foundQualities = new LinkedHashMap<String, String>();
        final String http_url = (String) video.get("postlive_mp4");
        final String http_quality = http_url != null ? new Regex(http_url, "(\\d+)\\.mp4").getMatch(0) : null;
        if (http_url != null && http_quality != null) {
            foundQualities.put(http_quality + "p", http_url);
        }
        if (foundQualities.isEmpty()) {
            /* Now try more fallbacks */
            /* Use cachexxx as workaround e.g. for special videos that need groups permission. */
            final String[][] qualities = { { "cache1080", "url1080", "1080p" }, { "cache720", "url720", "720p" }, { "cache480", "url480", "480p" }, { "cache360", "url360", "360p" }, { "cache240", "url240", "240p" }, { "cache144", "url144", "144p" } };
            for (final String[] qualityInfo : qualities) {
                String finallink = (String) video.get(qualityInfo[0]);
                if (finallink == null) {
                    finallink = (String) video.get(qualityInfo[1]);
                }
                if (finallink != null) {
                    foundQualities.put(qualityInfo[2], finallink);
                }
            }
            /* 2020-08-13: Last resort - only download HLS stream if nothing else is available! */
            if (foundQualities.isEmpty()) {
                final String hls_master = (String) video.get("hls");
                if (!StringUtils.isEmpty(hls_master)) {
                    foundQualities.put("HLS", hls_master);
                }
            }
        }
        return foundQualities;
    }

    public static Map<String, String> getSelectedVideoQualities(final Map<String, String> availableVideoQualities, final QualitySelectionMode mode, final String preferredVideoQuality) {
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
        /* TODO: Return something as fallback if user selection was not found or return nothing?? */
        // return fallbackQualities;
        return selectedQualities;
    }

    /** Handles complete walls */
    private void crawlWallLink(final CryptedLink param) throws Exception {
        if (vkwall_use_api) {
            crawlWallLinkAPI(param);
        } else {
            crawlWallWebsite(param);
        }
    }

    /**
     * Using API <br />
     * TODO: Add support for crawling comments (media/URLs in comments)
     */
    @SuppressWarnings("unchecked")
    private void crawlWallLinkAPI(final CryptedLink param) throws Exception {
        long total_numberof_entries;
        final String ownerID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall((\\-)?\\d+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(ownerID);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int currentOffset = 0;
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_LOOPBACK_LINK)) {
            /* TODO: Update this handling in case API handling ever gets fixed (see how it is done in website handling) */
            final UrlQuery query = UrlQuery.parse(this.CRYPTEDLINK_FUNCTIONAL);
            total_numberof_entries = Long.parseLong(query.get("maxoffset"));
            currentOffset = Integer.parseInt(query.get("currentoffset"));
            logger.info("PATTERN_WALL_LOOPBACK_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        } else {
            try {
                apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + ownerID + "&count=1&offset=0&filter=all&extended=0");
            } catch (final DecrypterException e) {
                if (this.getCurrentAPIErrorcode() == 15) {
                    /* Access denied --> We have to be logged in via API --> Try website-fallback */
                    logger.info("API wall decryption failed because of 'Access denied' --> Trying via website");
                    crawlWallWebsite(param);
                    return;
                }
                throw e;
            }
            total_numberof_entries = Long.parseLong(br.getRegex("\\{\"response\"\\:\\[(\\d+)").getMatch(0));
            logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        }
        while (currentOffset < total_numberof_entries) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                break;
            }
            logger.info("Starting to decrypt offset " + currentOffset + " / " + total_numberof_entries);
            apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + ownerID + "&count=" + API_MAX_ENTRIES_PER_REQUEST + "&offset=" + currentOffset + "&filter=all&extended=0");
            final Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (map == null) {
                return;
            }
            List<Object> response = (List<Object>) map.get("response");
            for (final Object entry : response) {
                if (entry instanceof Map) {
                    decryptWallPostJsonApi(ownerID, (Map<String, Object>) entry, fp);
                }
            }
            logger.info("Decrypted offset " + currentOffset + " / " + total_numberof_entries);
            logger.info("Found " + decryptedLinks.size() + " items so far");
            if (decryptedLinks.size() >= MAX_LINKS_PER_RUN) {
                logger.info("Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Returning link to continue");
                final UrlQuery query = UrlQuery.parse(this.CRYPTEDLINK_FUNCTIONAL);
                query.append("maxoffset", total_numberof_entries + "", false);
                query.append("currentoffset", currentOffset + "", false);
                final DownloadLink loopBack = createDownloadlink(this.CRYPTEDLINK_FUNCTIONAL + "?" + query.toString());
                fp.add(loopBack);
                decryptedLinks.add(loopBack);
                break;
            }
            currentOffset += API_MAX_ENTRIES_PER_REQUEST;
        }
    }

    /** Decrypts media of single API wall-post json objects. */
    @SuppressWarnings({ "unchecked" })
    private void decryptWallPostJsonApi(final String ownerID, final Map<String, Object> entry, FilePackage fp) throws IOException {
        final long postID = getPostIDFromSingleWallPostMap(entry);
        final long fromId = ((Number) entry.get("from_id")).longValue();
        final long toId = ((Number) entry.get("to_id")).longValue();
        final String wall_list_id = ownerID + "_" + postID;
        /* URL to show this post. */
        final String wall_single_post_url = "https://vk.com/wall" + wall_list_id;
        final String post_text = (String) entry.get("text");
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) entry.get("attachments");
        if (attachments == null) {
            return;
        }
        for (final Map<String, Object> attachment : attachments) {
            try {
                String owner_id = null;
                final String type = (String) attachment.get("type");
                if (type == null) {
                    return;
                }
                Map<String, Object> typeObject = (Map<String, Object>) attachment.get(type);
                if (typeObject == null) {
                    logger.warning("No Attachment for type " + type + " in " + attachment);
                    return;
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
                    dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
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
                    dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
                    /*
                     * Audiolinks have their directlinks and IDs but no "nice" links so let's simply use the link to the source wall post
                     * here so the user can easily find the title when opening it in browser.
                     */
                    dl.setContentUrl(wall_single_post_url);
                    dl.setProperty("postID", postID);
                    dl.setProperty("fromId", fromId);
                    dl.setProperty("toId", toId);
                    if (VKontakteRuHoster.audioIsValidDirecturl(url)) {
                        dl.setProperty("directlink", url);
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
                        videolink += "?listid=" + listID;
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
                    decryptedLinks.add(dl);
                }
            } catch (Throwable ee) {
                // catches casting errors etc.
                getLogger().info(attachment + "");
                getLogger().log(ee);
            }
        }
        crawlUrlsInsidePosts(post_text);
        if (this.vkwall_comments_grab_comments && !this.global_dupes.contains(wall_list_id)) {
            global_dupes.add(wall_list_id);
        }
    }

    /** Scans for URLs inside the (given) text of a wall post/comment. */
    private void crawlUrlsInsidePosts(String html) {
        if (html != null) {
            html = Encoding.htmlDecode(html);
        }
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
                    this.decryptedLinks.add(this.createDownloadlink(posted_url));
                } else {
                    logger.info("NOT ADDING url: " + posted_url);
                }
            }
        }
    }

    private long getPostIDFromSingleWallPostMap(final Map<String, Object> entry) {
        return ((Number) entry.get("id")).longValue();
    }

    private void crawlWallPost(final CryptedLink param) throws Exception {
        if (vkwall_use_api) {
            crawlWallPostAPI();
        } else {
            crawlWallPostWebsite(param);
        }
    }

    private void crawlWallPostWebsite(final CryptedLink param) throws Exception {
        final String owner_id = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/wall(-?\\d+)").getMatch(0);
        final String wall_post_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/wall(-?\\d+_\\d+)").getMatch(0);
        if (owner_id == null || wall_post_ID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        final FilePackage fp = FilePackage.getInstance();
        /* Set ID of current wall-post as packagename. */
        fp.setName(wall_post_ID);
        final int maxEntriesPerRequest = 20;
        int offset = 0;
        int page = 0;
        int totalItemsCrawledFromThisPost = 0;
        int totalNumberOfItems = 0;
        logger.info("Crawling single wall post");
        do {
            final int foundItemsOld = decryptedLinks.size();
            logger.info("Crawling (comments of) single wall post page: " + page);
            websiteCrawlContent(wall_post_ID, br.toString(), fp, this.vkwall_grabaudio, this.vkwall_grabvideo, this.vkwall_grabphotos, this.vkwall_grabdocs, this.vkwall_graburlsinsideposts, this.photos_store_picture_directurls);
            final int numberofItemsAddedThisLoop = decryptedLinks.size() - foundItemsOld;
            logger.info("Offset " + offset + " contained " + numberofItemsAddedThisLoop + " items in total so far (including inside replies)");
            offset += maxEntriesPerRequest;
            logger.info("Number of NEW added items: " + numberofItemsAddedThisLoop);
            totalItemsCrawledFromThisPost += numberofItemsAddedThisLoop;
            /* Avoid this sleep time if we stop after the current */
            if (!this.vkwall_comments_grab_comments) {
                logger.info("Stopping because user does not want to crawl items from replies to single posts");
                break;
            } else if (page == 0 && !br.containsHTML("class=\"replies_next replies_next_main\"")) {
                logger.info("Stopping because there is not more than 1 page");
                break;
            } else {
                /* Fail-safe */
                if (totalNumberOfItems > 0 && offset >= totalNumberOfItems) {
                    logger.info("Stopping because offset >= " + totalNumberOfItems);
                    break;
                }
                sleep(this.cfg.getLongProperty(VKontakteRuHoster.SLEEP_PAGINATION_GENERAL, VKontakteRuHoster.defaultSLEEP_PAGINATION_GENERAL), param);
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
                this.postPageSafe(param, "/al_wall.php", getRepliesQuery.toString());
                try {
                    final String json = br.getRegex("(\\{.*\\})").getMatch(0);
                    final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
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
        } while (!this.isAbort());
        logger.info("Found " + totalItemsCrawledFromThisPost + " items");
    }

    /**
     * Using API, finds and adds contents of a single wall post. <br/>
     * 2019-08-05: Requires authorization!
     */
    @SuppressWarnings("unchecked")
    private void crawlWallPostAPI() throws Exception {
        if (!isKnownType(this.CRYPTEDLINK_ORIGINAL)) {
            /* owner_id not given --> We need to find the owner_id */
            final String url_owner = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/([^\\?\\&=]+)").getMatch(0);
            if (StringUtils.isEmpty(url_owner)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String ownerName = resolveScreenName_API(url_owner);
            if (ownerName == null) {
                logger.warning("Decryption failed - Most likely an unsupported URL pattern! --> " + CRYPTEDLINK_FUNCTIONAL + "");
                /* do not return null, as this shows crawler error, and unsupported urls are not defects! */
                return;
            }
            final String type = PluginJSonUtils.getJsonValue(br, "type");
            if (type == null) {
                logger.warning("Failed to find type for link: " + CRYPTEDLINK_FUNCTIONAL);
                throw new DecrypterException("Plugin broken");
            }
            if (type.equals("user")) {
                this.CRYPTEDLINK_FUNCTIONAL = MAINPAGE + "/albums" + ownerName;
            } else {
                this.CRYPTEDLINK_FUNCTIONAL = MAINPAGE + "/wall-" + ownerName;
            }
        }
        final Regex wallRegex = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall((?:\\-)?\\d+)_(\\d+)");
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
        Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        try {
            /* Access original url as we sometimes need the listID for videos (see decryptWallPost). */
            this.apiGetPageSafe("https://vk.com/wall" + postIDWithOwnerID);
        } catch (final Throwable e) {
        }
        if (map == null) {
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(postIDWithOwnerID);
        List<Object> response = (List<Object>) map.get("response");
        for (Object entry : response) {
            if (entry instanceof Map) {
                decryptWallPostJsonApi(ownerID, (Map<String, Object>) entry, fp);
            }
        }
        logger.info("Found " + decryptedLinks.size() + " links");
    }

    /** Using Website */
    private void crawlWallWebsite(final CryptedLink param) throws Exception {
        long total_numberof_entries = -1;
        String ownerID = new Regex(param.getCryptedUrl(), "vk\\.com/wall((\\-)?\\d+)").getMatch(0);
        int currentOffset = 0;
        int counter_wall_start_from = 0;
        int counter_items_found_in_current_offset = 10;
        final int offset_increase = 10;
        String json_source = null;
        String postvalue_fixed = "";
        final UrlQuery queryOriginalURL = UrlQuery.parse(this.CRYPTEDLINK_ORIGINAL);
        final String urlMaxoffsetStr = queryOriginalURL.get("maxoffset");
        final String urlCurrentOffsetStr = queryOriginalURL.get("currentoffset");
        if (urlMaxoffsetStr != null && urlMaxoffsetStr.matches("\\d+") && urlCurrentOffsetStr != null && urlCurrentOffsetStr.matches("\\d+")) {
            /* URL matches pattern: PATTERN_WALL_LOOPBACK_LINK */
            total_numberof_entries = Long.parseLong(urlMaxoffsetStr);
            currentOffset = Integer.parseInt(urlCurrentOffsetStr);
            logger.info("PATTERN_WALL_LOOPBACK_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
            this.getPage(this.CRYPTEDLINK_FUNCTIONAL);
        } else {
            /* The URL could also be a directURL or unsupported -> Let's check for that first */
            logger.info("URL leads to downloadable content");
            URLConnectionAdapter con = this.br.openGetConnection(this.CRYPTEDLINK_ORIGINAL);
            if (this.looksLikeDownloadableContent(con)) {
                /* Very rare case */
                try {
                    con.disconnect();
                } catch (final Throwable ignore) {
                }
                final DownloadLink direct = this.createDownloadlink("directhttp://" + con.getURL().toString());
                if (con.getCompleteContentLength() > 0) {
                    direct.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String filename = Plugin.getFileNameFromURL(con.getURL());
                if (filename != null) {
                    direct.setName(filename);
                }
                direct.setAvailable(true);
                this.decryptedLinks.add(direct);
                return;
            }
            br.followConnection();
            /* 2020-02-07: This is most likely not given but we have other fail safes in place to stop once we're done. */
            json_source = this.br.getRegex("var opts\\s*?=\\s*?(\\{.*?\\});\\s+").getMatch(0);
            if (json_source != null) {
                total_numberof_entries = Long.parseLong(PluginJSonUtils.getJsonValue(json_source, "count"));
                logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
            } else {
                logger.info("PATTERN_WALL_LINK has a max offset of UNKNOWN and a current offset of " + currentOffset);
            }
        }
        if (ownerID == null) {
            /* We need to find the owner_id - without it we would only be able to find all entries from the first page. */
            json_source = br.getRegex("window\\[\\'public\\'\\]\\.init\\((\\{.*?)</script>").getMatch(0);
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
            if (StringUtils.isEmpty(ownerID) || ownerID.equals("null")) {
                logger.warning("Failed to find owner_id --> Can only crawl first page");
            } else {
                /* Correct crawled values */
                ownerID = "-" + ownerID;
            }
            if (postvalue_fixed == null) {
                postvalue_fixed = "";
            }
        }
        FilePackage fp = null;
        if (ownerID != null) {
            fp = FilePackage.getInstance();
            fp.setName(ownerID);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final int max_pages_without_downloadable_content_in_a_row = 5;
        int pages_without_downloadable_content_in_a_row = 0;
        boolean stop_because_all_items_found = false;
        do {
            counter_items_found_in_current_offset = 0;
            logger.info("Decrypting offset " + currentOffset);
            if (currentOffset > 0) {
                if (StringUtils.isEmpty(ownerID) || ownerID.equals("null")) {
                    logger.warning("Stopping after first loop because owner_id is not given");
                    break;
                }
                this.postPageSafe(param, "https://" + this.getHost() + "/al_wall.php", String.format("act=get_wall&al=1&fixed=%s&offset=%s&onlyCache=false&owner_id=%s&type=own&wall_start_from=%s", postvalue_fixed, currentOffset, ownerID, counter_wall_start_from));
                this.br.getRequest().setHtmlCode(JSonUtils.unescape(br.toString()));
            }
            final int numberof_items_old = this.decryptedLinks.size();
            if (this.vkwall_comments_grab_comments) {
                logger.info("User wants content of comments --> First adding comment URLs during wall-crawling");
                final String[] singleWallPostIDs = br.getRegex("wall(" + ownerID + "_\\d+)").getColumn(0);
                for (final String singleWallpostID : singleWallPostIDs) {
                    final DownloadLink dl = this.createDownloadlink("https://vk.com/wall" + singleWallpostID);
                    decryptedLinks.add(dl);
                }
            } else {
                logger.info("Crawling items inside single post as part of a wall");
                // decryptWallPostHTMLWebsite(wall_post_ID, br.toString(), fp);
                websiteCrawlContent(br.getURL(), br.toString(), fp, this.vkwall_grabaudio, this.vkwall_grabvideo, this.vkwall_grabphotos, this.vkwall_grabdocs, this.vkwall_graburlsinsideposts, this.photos_store_picture_directurls);
            }
            final int numberof_items_new = this.decryptedLinks.size();
            counter_items_found_in_current_offset = numberof_items_new - numberof_items_old;
            if (counter_items_found_in_current_offset == 0) {
                logger.info("Failed to find any items for offset: " + currentOffset);
                pages_without_downloadable_content_in_a_row++;
            } else {
                /* Reset this */
                pages_without_downloadable_content_in_a_row = 0;
            }
            logger.info("Found " + decryptedLinks.size() + " items so far");
            /*
             * Stop conditions are placed here and not in the while loops' footer on purpose to place loggers and get to know the exact stop
             * reason!
             */
            stop_because_all_items_found = total_numberof_entries != -1 && currentOffset >= total_numberof_entries;
            /* Increase counters */
            currentOffset += offset_increase;
            counter_wall_start_from += 10;
            if (pages_without_downloadable_content_in_a_row >= max_pages_without_downloadable_content_in_a_row) {
                logger.info(String.format("Stopping because: Failed to find more items for %d times in the row", pages_without_downloadable_content_in_a_row));
                break;
            } else if (stop_because_all_items_found) {
                /* Also output the exact number of found items as it could be higher than what we expected! */
                logger.info(String.format("Stopping because: Found all %d of %d items", decryptedLinks.size(), total_numberof_entries));
                break;
            } else if (decryptedLinks.size() >= MAX_LINKS_PER_RUN) {
                logger.info("Stopping because: Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Returning link to continue");
                final UrlQuery query = UrlQuery.parse(this.CRYPTEDLINK_FUNCTIONAL);
                query.append("maxoffset", total_numberof_entries + "", false);
                query.append("currentoffset", currentOffset + "", false);
                final DownloadLink loopBack = createDownloadlink(this.CRYPTEDLINK_FUNCTIONAL + "?" + query.toString());
                fp.add(loopBack);
                decryptedLinks.add(loopBack);
                break;
            }
        } while (!this.isAbort());
        logger.info("Done!");
    }

    /**
     * Crawls desired content from website html code from either a wall-POST or wall-COMMENT or ALBUMS URL from below a post.
     *
     * @throws IOException
     * @throws DecrypterException
     */
    private void websiteCrawlContent(final String url_source, final String html, final FilePackage fp, final boolean grabAudio, final boolean grabVideo, final boolean grabPhoto, final boolean grabDocs, final boolean grabURLsInsideText, final boolean store_picture_directurls) throws IOException, DecrypterException {
        if (url_source == null) {
            throw new DecrypterException("Decrypter broken");
        }
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
        /* TODO: Improve this handling */
        if (url_source != null && url_source.matches(PATTERN_PHOTO_ALBUMS)) {
            /* 2019-10-02: Newly added albums support */
            isContentFromWall = false;
            album_ID = new Regex(url_source, PATTERN_PHOTO_ALBUMS).getMatch(0);
            grabAlbums = true;
        } else if (url_source != null && url_source.matches(PATTERN_PHOTO_ALBUM)) {
            final String id_of_current_album = new Regex(url_source, "/album((?:\\-)?\\d+_\\d+)").getMatch(0);
            if (id_of_current_album != null) {
                photo_list_id = "album" + id_of_current_album;
            }
            photo_module = "photos";
        } else if (url_source != null && url_source.matches("^(?:\\-)?\\d+_\\d+$")) {
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
        }
        String ownerIDTemp = null;
        String contentIDTemp = null;
        /* TODO: Make sure this works for POSTs, COMMENTs and ALBUMs!! */
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
                decryptedLinks.add(dl);
            }
        }
        if (grabPhoto) {
            final String[] photo_ids = new Regex(html, "showPhoto\\(\\'(-?\\d+_\\d+)").getColumn(0);
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
                String photo_html = new Regex(html, "showPhoto\\(([^\\)]*?" + photoContentStr + "[^\\)]*?)\\)").getMatch(0);
                String single_photo_content_url = null;
                if (photo_html != null) {
                    photo_html = photo_html.replace("'", "");
                    final String[] photoInfoArray = photo_html.split(", ");
                    final String photo_list_id_tmp = photoInfoArray[1];
                    if (photo_list_id_tmp.matches("wall-?\\d+_\\d+")) {
                        photo_list_id = photo_list_id_tmp;
                        final String wall_post_reply_content_idTmp = new Regex(photo_list_id_tmp, "(\\d+)$").getMatch(0);
                        if (html.contains("?reply=" + wall_post_reply_content_idTmp)) {
                            isContentFromWallReply = true;
                            wall_post_reply_content_id = wall_post_reply_content_idTmp;
                            single_photo_content_url = getProtocol() + this.getHost() + "/wall" + url_source + "?reply=" + wall_post_reply_content_id + "&z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2Fwall" + ownerIDTemp + "_" + wall_post_reply_content_id;
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
                            single_photo_content_url = getProtocol() + this.getHost() + "/wall" + url_source + "?z=photo" + ownerIDTemp + "_" + contentIDTemp + "%2Fwall" + url_source;
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
                        picture_preview_json = new Regex(photo_html, "(\\{(?:\"|\\&quot;)(?:base|temp)(?:\"|\\&quot;).*?\\}),[^\\{\\}]+\\)").getMatch(0);
                    }
                }
                if (single_photo_content_url == null) {
                    /* Fallback and or photos inside photo album --> Set default content_url */
                    single_photo_content_url = getProtocol() + this.getHost() + "/photo" + ownerIDTemp + "_" + contentIDTemp;
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
                decryptedLinks.add(dl);
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
                decryptedLinks.add(dl);
            }
        }
        /* Videos */
        if (grabVideo) {
            this.crawlVideos(this.br, fp);
        }
        if (grabDocs) {
            /* 2021-01-08 */
            final String[] docHTMLs = br.getRegex("div class=\"page_doc_row\"[^>]*>(.*?)</div>\\s*</div>").getColumn(0);
            for (final String docHTML : docHTMLs) {
                final String url = new Regex(docHTML, "href=\"(/doc[^\"]+)\"").getMatch(0);
                final String filename = new Regex(docHTML, "target=\"_blank\">([^<>\"]+)</a>").getMatch(0);
                final String filesize = new Regex(docHTML, "class=\"page_doc_size\">([^<>\"]+)<").getMatch(0);
                if (url == null || filename == null || filesize == null) {
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
                decryptedLinks.add(dl);
            }
        }
        if (grabURLsInsideText && isContentFromWall) {
            crawlUrlsInsidePosts(wall_post_text);
        }
    }

    /** Works offline, simply converts the added link into a DownloadLink for the host plugin and sets required properties. */
    private void decryptWallPostSpecifiedPhoto(final CryptedLink param) throws Exception {
        String module = null;
        String list_id = null;
        /* URLs may contain multiple list_id-like strings! It is important to use the source URL as an orientation. */
        if (new Regex(param.getCryptedUrl(), "https?://[^/]+/photo-?\\d+_\\d+\\?tag=\\d+").matches()) {
            module = "photos";
            list_id = "tag" + new Regex(param.getCryptedUrl(), "(\\d+)$").getMatch(0);
        } else if (!isTypeSinglePicture(param.getCryptedUrl())) {
            if (param.getCryptedUrl().contains("/wall")) {
                module = "wall";
            } else {
                module = "public";
            }
            list_id = new Regex(param.getCryptedUrl(), "((?:wall|album)(\\-)?\\d+_\\d+)$").getMatch(0);
        }
        final String owner_id = new Regex(param.getCryptedUrl(), "photo(-?\\d+)_\\d+").getMatch(0);
        final String content_id = new Regex(param.getCryptedUrl(), "photo-?\\d+_(\\d+)").getMatch(0);
        final DownloadLink dl = getSinglePhotoDownloadLink(owner_id + "_" + content_id, null);
        dl.setContentUrl(param.getCryptedUrl());
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        if (module != null) {
            dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_module, module);
        }
        dl.setProperty(VKontakteRuHoster.PROPERTY_PHOTOS_photo_list_id, list_id);
        decryptedLinks.add(dl);
        return;
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    private void crawlDocs(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        if (br.containsHTML("Unfortunately, you are not a member of this group and cannot view its documents") || br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String owner_ID = new Regex(param.getCryptedUrl(), "((?:\\-)?\\d+)$").getMatch(0);
        String fpName = null;
        if (cfg.getBooleanProperty(VKDOCS_USEIDASPACKAGENAME, false)) {
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
                this.decryptedLinks = null;
                return;
            }
            final String filename = stringdata[1];
            final String content_ID = new Regex(docinfo, "^(?:\\[)?(\\d+)").getMatch(0);
            final DownloadLink dl = getSinglePhotoDownloadLink("https://vk.com/doc" + owner_ID + "_" + content_ID, null);
            dl.setContentUrl(CRYPTEDLINK_FUNCTIONAL);
            dl.setName(Encoding.htmlDecode(filename));
            dl.setDownloadSize(SizeFormatter.getSize(filesize));
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_owner_id, owner_ID);
            dl.setProperty(VKontakteRuHoster.PROPERTY_GENERAL_content_id, content_ID);
            fp.add(dl);
            decryptedLinks.add(dl);
        }
        return;
    }

    private void crawlUserStory(final CryptedLink param) throws Exception {
        this.getPage(param.getCryptedUrl());
        this.siteGeneralErrorhandling(this.br);
        final String json = br.getRegex("cur\\['stories_list_owner_feed-?\\d+'\\]=(\\[.+\\]);").getMatch(0);
        if (StringUtils.isEmpty(json)) {
            /* Probably user does not have a story at this moment or account is required to view those. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
            decryptedLinks.add(dl);
            dl._setFilePackage(fp);
        }
    }

    private void crawlWallClips(final CryptedLink param) throws Exception {
        final String clipCollectionName = new Regex(param.getCryptedUrl(), PATTERN_WALL_CLIPS).getMatch(0);
        this.getPage(br, param.getCryptedUrl());
        final FilePackage fp = FilePackage.getInstance();
        /* We want all links from this user/collection to go into the same package */
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
        fp.setName(clipCollectionName);
        this.crawlVideos(this.br, fp);
    }

    private boolean crawlVideos(final Browser br, final FilePackage fp) {
        final String[] videoHTMLs = br.getRegex("showVideo\\(([^\\)]+)\\)").getColumn(0);
        boolean foundNewItems = false;
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
            foundNewItems = true;
            /* Important: This URL may contain information without which crawler/hosterplugin would fail later! */
            final String contentURL;
            if (postID != null && listID != null) {
                /* Video is part of */
                contentURL = this.getProtocol() + this.getHost() + "/wall" + postID + "?z=video" + videoContentStr + "%2F" + listID;
            } else if (postID != null) {
                contentURL = this.getProtocol() + this.getHost() + "/wall" + postID + "?z=video" + videoContentStr;
            } else {
                contentURL = this.getProtocol() + this.getHost() + "/video" + videoContentStr;
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
            decryptedLinks.add(dl);
        }
        return foundNewItems;
    }

    private void postPageSafe(final CryptedLink param, final String page, final String postData) throws Exception {
        boolean failed = false;
        for (int counter = 1; counter <= 10; counter++) {
            br.postPage(page, postData);
            if (br.containsHTML(TEMPORARILYBLOCKED)) {
                if (counter + 1 > 10) {
                    logger.warning("Failed to avoid block!");
                    throw new DecrypterException("Blocked");
                }
                failed = true;
                logger.info("Trying to avoid block " + counter + " / 10");
                sleep(this.cfg.getLongProperty(VKontakteRuHoster.SLEEP_TOO_MANY_REQUESTS, VKontakteRuHoster.defaultSLEEP_TOO_MANY_REQUESTS), param);
                continue;
            }
            if (failed) {
                logger.info("Successfully avoided block!");
            }
            return;
        }
    }

    private void getPage(final String url) throws Exception {
        getPage(this.br, url);
        siteGeneralErrorhandling(this.br);
    }

    private void apiGetPageSafe(final String parameter) throws Exception {
        getPage(br, parameter);
        apiHandleErrors();
    }

    @SuppressWarnings("unused")
    private void apiPostPageSafe(final String page, final String postData) throws Exception {
        br.postPage(page, postData);
        apiHandleErrors();
    }

    /**
     * Handles these error-codes: https://vk.com/dev/errors
     *
     * @return true = ready to retry, false = problem - failed!
     */
    private boolean apiHandleErrors() throws Exception {
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
                boolean success = siteHandleSecurityCheck(redirectUri);
                if (success) {
                    logger.info("Verification Done");
                    return true;
                } else {
                    logger.info("Verification Failed");
                    return false;
                }
            }
            boolean loginsucceeded = getUserLogin(true);
            if (loginsucceeded) {
                logger.info("Succeeded to re-login");
                return true;
            } else {
                logger.warning("FAILED to re-login");
                throw new DecrypterException(EXCEPTION_ACCPROBLEM);
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
    private void getPage(final Browser br, final String url) throws Exception {
        final boolean ifr = br.isFollowingRedirects();
        // if false, we get page and continue!
        if (!ifr) {
            br.getPage(url);
            return;
        }
        // following code checks all redirects against conditions.
        try {
            int counter = 0;
            br.setFollowRedirects(false);
            String redirect = url;
            do {
                br.getPage(redirect);
                redirect = br.getRedirectLocation();
                if (redirect != null) {
                    if (redirect.contains("act=security_check") || redirect.contains("login.vk.com/?role=fast")) {
                        if (siteHandleSecurityCheck(redirect)) {
                            br.getPage(url);
                        } else {
                            throw new DecrypterException("Could not solve Security Questions");
                        }
                    } else {
                        // maybe multiple redirects before end outcome!
                        br.getPage(redirect);
                    }
                }
            } while ((redirect = br.getRedirectLocation()) != null && counter++ < 10);
            if (redirect != null && counter >= 10) {
                throw new DecrypterException("Too many redirects!");
            }
        } finally {
            br.setFollowRedirects(ifr);
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

    private Account account = null;

    /** Log in via hoster plugin */
    private boolean getUserLogin(final boolean force) throws Exception {
        if (account == null) {
            account = AccountController.getInstance().getValidAccount(getHost());
            if (account == null) {
                logger.warning("There is no account available, continuing without logging in (if possible)");
                return false;
            }
        }
        final PluginForHost hostPlugin = getNewPluginForHostInstance(getHost());
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
    private boolean siteHandleSecurityCheck(final String parameter) throws Exception {
        // this task shouldn't be done without an account!, ie. login should have taken place
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                    logger.info("Entering security check...");
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
                            logger.info("Failed security check, account is banned for some hours!");
                            break;
                        }
                    }
                }
                return hasPassed;
            } else {
                ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                for (int i = 0; i <= 3; i++) {
                    logger.info("Entering security check...");
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
                        logger.info("Failed security check, account is banned for some hours!");
                        break;
                    }
                }
                return hasPassed;
            }
        }
    }

    private static boolean isTypeSingleVideo(final String input) {
        return input.matches(PATTERN_VIDEO_SINGLE_Z) || input.matches(PATTERN_CLIP_SINGLE_ORIGINAL) || input.matches(PATTERN_VIDEO_SINGLE_ORIGINAL) || input.matches(PATTERN_CLIP_SINGLE_Z) || input.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_WITH_LISTID) || input.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST) || input.matches(PATTERN_VIDEO_SINGLE_EMBED) || input.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH);
    }

    private static boolean isTypeSinglePicture(final String input) {
        return (input.matches(PATTERN_PHOTO_SINGLE) || input.matches(PATTERN_PHOTO_SINGLE_Z) && !input.matches(PATTERN_PHOTO_MODULE));
    }

    private static boolean isSingleWallPost(final String input) {
        return input.matches(PATTERN_WALL_POST_LINK);
    }

    private static boolean isVideoAlbum(final String input) {
        return input.matches(PATTERN_VIDEO_ALBUM) || input.matches(PATTERN_VIDEO_COMMUNITY_ALBUM);
    }

    private static boolean isUserStory(final String input) {
        return input.matches(PATTERN_USER_STORY);
    }

    /**
     * Handles basic (offline) errors.
     *
     * @throws PluginException
     */
    private void siteGeneralErrorhandling(final Browser br) throws DecrypterException, PluginException {
        /* General errorhandling start */
        if (br.containsHTML("(?i)>\\s*Unknown error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Only logged in users can see this profile\\.<")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("(?i)>\\s*Access denied|>\\s*You do not have permission to do this")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("vk.com/blank.php")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* General errorhandling end */
    }

    /** Sets basic values/cookies */
    private void prepBrowser(final Browser br) {
        VKontakteRuHoster.prepBrowser(br, true);
    }

    /**
     * Basic preparations on user-added links. Make sure to remove unneeded things so that in the end, our links match the desired
     * linktypes. This is especially important because we get required IDs out of these urls or even access them directly without API.
     *
     * @param a
     *
     * @throws IOException
     */
    private void prepCryptedLink() throws IOException {
        /* Correct encoding, domain and protocol. */
        CRYPTEDLINK_ORIGINAL = Encoding.htmlDecode(CRYPTEDLINK_ORIGINAL).replaceAll("(m\\.|new\\.)?(vkontakte|vk)\\.(ru|com)/", "vk.com/");
        // CRYPTEDLINK_ORIGINAL = Encoding.htmlDecode(CRYPTEDLINK_ORIGINAL).replaceAll("(m\\.|new\\.)?(vkontakte|vk)\\.(ru|com)/",
        // Boolean.FALSE.equals(a) ? "new.vk.com/" : "vk.com/");
        /* We cannot simply remove all parameters which we usually don't need because...we do sometimes need em! */
        if (this.CRYPTEDLINK_ORIGINAL.contains("?") && !isKnownType(CRYPTEDLINK_ORIGINAL)) {
            this.CRYPTEDLINK_ORIGINAL = removeParamsFromURL(CRYPTEDLINK_ORIGINAL);
        } else {
            /* Remove unneeded parameters. */
            final String[] unwantedParts = { "(\\?profile=\\d+)", "(\\?rev=\\d+)", "(/rev)$", "(\\?albums=\\d+)" };
            for (final String unwantedPart : unwantedParts) {
                final String unwantedData = new Regex(this.CRYPTEDLINK_ORIGINAL, unwantedPart).getMatch(0);
                if (unwantedData != null) {
                    this.CRYPTEDLINK_ORIGINAL = this.CRYPTEDLINK_ORIGINAL.replace(unwantedData, "");
                }
            }
        }
        final String wall_id = new Regex(this.CRYPTEDLINK_ORIGINAL, "\\?w=(wall(\\-)?\\d+_\\d+)").getMatch(0);
        if (wall_id != null && !isTypeSinglePicture(this.CRYPTEDLINK_ORIGINAL)) {
            // respect imported protocol
            this.CRYPTEDLINK_ORIGINAL = Request.getLocation("/" + wall_id, new Browser().createGetRequest(CRYPTEDLINK_ORIGINAL));
        }
        this.CRYPTEDLINK_FUNCTIONAL = this.CRYPTEDLINK_ORIGINAL;
    }

    private String removeParamsFromURL(final String input) {
        String output;
        final String params = new Regex(input, "(\\?.+)").getMatch(0);
        if (params != null) {
            output = input.replace(params, "");
        } else {
            /* No parameters to remove */
            output = input;
        }
        return output;
    }

    private String get_ID_PAGE(final String url) {
        return new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/page\\-(\\d+_\\d+)").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}