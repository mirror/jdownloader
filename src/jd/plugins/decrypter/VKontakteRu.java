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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "https?://(?:www\\.|m\\.)?(?:vk\\.com|vkontakte\\.ru|vkontakte\\.com)/(?!doc[\\d\\-]+_[\\d\\-]+|picturelink|audiolink|videolink)[a-z0-9_/=\\.\\-\\?\\&]+" }, flags = { 0 })
public class VKontakteRu extends PluginForDecrypt {

    /** TODO: Note: PATTERN_VIDEO_SINGLE links should all be decryptable without account but this is not implemented (yet) */

    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setRequestIntervalLimitGlobal("vk.com", 250, 20, 30000);
        } catch (final Throwable e) {
        }
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private String getBaseURL() {
        return jd.plugins.hoster.VKontakteRuHoster.getBaseURL();
    }

    private String getProtocol() {
        return jd.plugins.hoster.VKontakteRuHoster.getProtocol();
    }

    private static final String     EXCEPTION_ACCPROBLEM                    = "EXCEPTION_ACCPROBLEM";
    private static final String     EXCEPTION_LINKOFFLINE                   = "EXCEPTION_LINKOFFLINE";
    private static final String     EXCEPTION_API_UNKNOWN                   = "EXCEPTION_API_UNKNOWN";

    /* Settings */
    private static final String     FASTLINKCHECK_VIDEO                     = "FASTLINKCHECK_VIDEO";
    private static final String     FASTLINKCHECK_PICTURES                  = "FASTLINKCHECK_PICTURES";
    private static final String     FASTLINKCHECK_AUDIO                     = "FASTLINKCHECK_AUDIO";
    private static final String     ALLOW_BEST                              = "ALLOW_BEST";
    private static final String     ALLOW_240P                              = "ALLOW_240P";
    private static final String     ALLOW_360P                              = "ALLOW_360P";
    private static final String     ALLOW_480P                              = "ALLOW_480P";
    private static final String     ALLOW_720P                              = "ALLOW_720P";
    private static final String     ALLOW_1080P                             = "ALLOW_1080P";
    private static final String     VKWALL_GRAB_ALBUMS                      = "VKWALL_GRAB_ALBUMS";
    private static final String     VKWALL_GRAB_PHOTOS                      = "VKWALL_GRAB_PHOTOS";
    private static final String     VKWALL_GRAB_AUDIO                       = "VKWALL_GRAB_AUDIO";
    private static final String     VKWALL_GRAB_VIDEO                       = "VKWALL_GRAB_VIDEO";
    private static final String     VKWALL_GRAB_LINK                        = "VKWALL_GRAB_LINK";
    private static final String     VKWALL_GRAB_DOCS                        = "VKWALL_GRAB_DOCS";
    private static final String     VKVIDEO_USEIDASPACKAGENAME              = "VKVIDEO_USEIDASPACKAGENAME";
    private static final String     VKAUDIOS_USEIDASPACKAGENAME             = "VKAUDIOS_USEIDASPACKAGENAME";
    private static final String     VKDOCS_USEIDASPACKAGENAME               = "VKDOCS_USEIDASPACKAGENAME";

    /* Settings 'in action' */
    private boolean                 vkwall_grabalbums;
    private boolean                 vkwall_grabphotos;
    private boolean                 vkwall_grabaudio;
    private boolean                 vkwall_grabvideo;
    private boolean                 vkwall_grablink;
    private boolean                 vkwall_grabdocs;
    private boolean                 vkwall_graburlsinsideposts;
    private String                  vkwall_graburlsinsideposts_regex;
    private String                  vkwall_graburlsinsideposts_regex_default;

    /* Some supported url patterns */
    private static final String     PATTERN_SHORT                           = "https?://(www\\.)?vk\\.cc/[A-Za-z0-9]+";
    private static final String     PATTERN_URL_EXTERN                      = "https?://(?:www\\.)?vk\\.com/away\\.php\\?to=.+";
    private static final String     PATTERN_GENERAL_AUDIO                   = "https?://(www\\.)?vk\\.com/audio.*?";
    private static final String     PATTERN_AUDIO_ALBUM                     = "https?://(?:www\\.)?vk\\.com/(?:audio(?:\\.php)?\\?id=(?:\\-)?\\d+|audios(?:\\-)?\\d+).*?";
    private static final String     PATTERN_AUDIO_PAGE                      = "https?://(www\\.)?vk\\.com/page\\-\\d+_\\d+.*?";
    private static final String     PATTERN_AUDIO_PAGE_oid                  = "https?://(www\\.)?vk\\.com/pages\\?oid=\\-\\d+\\&p=(?!va_c)[^<>/\"]+";
    private static final String     PATTERN_AUDIO_AUDIOS_ALBUM              = "https?://(www\\.)?vk\\.com/audios\\-\\d+\\?album_id=\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_MODULE             = "https?://(www\\.)?vk\\.com/[A-Za-z0-9\\-_\\.]+\\?z=video(\\-W)?\\d+_\\d+/.+";
    private static final String     PATTERN_VIDEO_SINGLE_SEARCH             = "https?://(www\\.)?vk\\.com/search\\?(c\\[q\\]|c%5Bq%5D)=[^<>\"/]*?\\&c(\\[section\\]|%5Bsection%5D)=video(\\&c(\\[sort\\]|%5Bsort%5D)=\\d+)?\\&z=video(\\-)?\\d+_\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL           = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL_LIST      = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+\\?list=[a-z0-9]+";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED              = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+.*?";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED_HASH         = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+\\&hash=[a-z0-9]+.*?";
    private static final String     PATTERN_VIDEO_ALBUM                     = "https?://(www\\.)?vk\\.com/(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged|videos(\\-)?\\d+)";
    private static final String     PATTERN_VIDEO_ALBUM_WITH_UNKNOWN_PARAMS = "https?://(www\\.)?vk\\.com/videos(\\-)?\\d+\\?.+";
    private static final String     PATTERN_VIDEO_COMMUNITY_ALBUM           = "https?://(www\\.)?vk\\.com/video\\?gid=\\d+";
    private static final String     PATTERN_PHOTO_SINGLE                    = "https?://(www\\.)?vk\\.com/photo(\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_PHOTO_SINGLE_Z                  = "https?://(?:www\\.)?vk\\.com/.+z=photo(?:\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_PHOTO_MODULE                    = "https?://(www\\.)?vk\\.com/[A-Za-z0-9\\-_\\.]+\\?z=photo(\\-)?\\d+_\\d+/(wall|album)\\-\\d+_\\d+";
    private static final String     PATTERN_PHOTO_ALBUM                     = ".*?(tag|album(?:\\-)?\\d+_|photos(?:\\-)?)\\d+";
    private static final String     PATTERN_PHOTO_ALBUMS                    = "https?://(?:www\\.)?vk\\.com/.*?albums(?:\\-)?\\d+";
    private static final String     PATTERN_GENERAL_WALL_LINK               = "https?://(www\\.)?vk\\.com/wall(?:\\-)?\\d+(?:\\-maxoffset=\\d+\\-currentoffset=\\d+)?";
    private static final String     PATTERN_WALL_LOOPBACK_LINK              = "https?://(www\\.)?vk\\.com/wall\\-\\d+\\-maxoffset=\\d+\\-currentoffset=\\d+";
    private static final String     PATTERN_WALL_POST_LINK                  = "https?://(?:www\\.)?vk\\.com/wall(?:\\-)?\\d+_\\d+";
    private static final String     PATTERN_WALL_POST_LINK_2                = "https?://(?:www\\.)?vk\\.com/wall\\-\\d+.+w=wall(?:\\-)?\\d+_\\d+.*?";
    private static final String     PATTERN_PUBLIC_LINK                     = "https?://(www\\.)?vk\\.com/public\\d+";
    private static final String     PATTERN_CLUB_LINK                       = "https?://(www\\.)?vk\\.com/club\\d+.*?";
    private static final String     PATTERN_EVENT_LINK                      = "https?://(www\\.)?vk\\.com/event\\d+";
    private static final String     PATTERN_ID_LINK                         = "https?://(www\\.)?vk\\.com/id\\d+";
    private static final String     PATTERN_DOCS                            = "https?://(www\\.)?vk\\.com/docs\\?oid=\\-\\d+";

    /* Some html text patterns: English, Russian, German, Polish */
    public static final String      TEMPORARILYBLOCKED                      = "You tried to load the same page more than once in one second|Вы попытались загрузить более одной однотипной страницы в секунду|Pr\\&#243;bujesz za\\&#322;adowa\\&#263; wi\\&#281;cej ni\\&#380; jedn\\&#261; stron\\&#281; w ci\\&#261;gu sekundy|Sie haben versucht die Seite mehrfach innerhalb einer Sekunde zu laden";
    private static final String     FILEOFFLINE                             = "(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)";

    /* Possible/Known types of single vk-wall-posts */
    private static final String     wallpost_type_photo                     = "photo";
    private static final String     wallpost_type_doc                       = "doc";
    private static final String     wallpost_type_audio                     = "audio";
    private static final String     wallpost_type_link                      = "link";
    private static final String     wallpost_type_video                     = "video";
    private static final String     wallpost_type_album                     = "album";
    private static final String     wallpost_type_poll                      = "poll";

    /* Internal settings / constants */
    /*
     * Whenever we found this number of links or more, quit the decrypter and add a [b]LOOPBACK_LINK[/b] to continue later in order to avoid
     * memory problems/freezes.
     */
    private static final short      MAX_LINKS_PER_RUN                       = 5000;

    /* Used whenever we request arrays via API */
    private static final int        API_MAX_ENTRIES_PER_REQUEST             = 100;

    private SubConfiguration        cfg                                     = null;
    private static final String     MAINPAGE                                = "https://vk.com";

    private String                  CRYPTEDLINK_FUNCTIONAL                  = null;
    private String                  CRYPTEDLINK_ORIGINAL                    = null;
    private CryptedLink             CRYPTEDLINK                             = null;
    private boolean                 fastcheck_photo                         = false;
    private boolean                 fastcheck_audio                         = false;

    private final boolean           docs_add_unique_id                      = true;

    private ArrayList<DownloadLink> decryptedLinks                          = null;

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
        // set setters,
        br = new Browser();
        this.CRYPTEDLINK_ORIGINAL = param.toString();
        this.CRYPTEDLINK = param;
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
        cfg = SubConfiguration.getConfig("vkontakte.ru");
        fastcheck_photo = cfg.getBooleanProperty(FASTLINKCHECK_PICTURES, false);
        fastcheck_audio = cfg.getBooleanProperty(FASTLINKCHECK_AUDIO, false);
        vkwall_grabalbums = cfg.getBooleanProperty(VKWALL_GRAB_ALBUMS, false);
        vkwall_grabphotos = cfg.getBooleanProperty(VKWALL_GRAB_PHOTOS, false);
        vkwall_grabaudio = cfg.getBooleanProperty(VKWALL_GRAB_AUDIO, false);
        vkwall_grabvideo = cfg.getBooleanProperty(VKWALL_GRAB_VIDEO, false);
        vkwall_grablink = cfg.getBooleanProperty(VKWALL_GRAB_LINK, false);
        vkwall_grabdocs = cfg.getBooleanProperty(VKWALL_GRAB_DOCS, false);
        vkwall_graburlsinsideposts = cfg.getBooleanProperty(jd.plugins.hoster.VKontakteRuHoster.VKWALL_GRAB_URLS_INSIDE_POSTS, jd.plugins.hoster.VKontakteRuHoster.default_WALL_ALLOW_lookforurlsinsidewallposts);
        vkwall_graburlsinsideposts_regex_default = jd.plugins.hoster.VKontakteRuHoster.default_VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX;
        vkwall_graburlsinsideposts_regex = cfg.getStringProperty(jd.plugins.hoster.VKontakteRuHoster.VKWALL_GRAB_URLS_INSIDE_POSTS_REGEX, vkwall_graburlsinsideposts_regex_default);

        prepBrowser(br);
        prepCryptedLink();
        boolean loginrequired = true;
        /* Check/fix links before browser access START */
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_SHORT)) {
            loginrequired = false;
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
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_SINGLE) || (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_SINGLE_Z) && !CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_MODULE))) {
            /**
             * Single photo links, those are just passed to the hoster plugin! Example:http://vk.com/photo125005168_269986868
             */
            final DownloadLink decryptedPhotolink = getSinglePhotoDownloadLink(new Regex(CRYPTEDLINK_ORIGINAL, "photo((\\-)?\\d+_\\d+)").getMatch(0));
            decryptedLinks.add(decryptedPhotolink);
            return decryptedLinks;
        } else if (isSingeVideo(CRYPTEDLINK_ORIGINAL)) {
            loginrequired = false;
            if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_MODULE) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_SEARCH) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
                CRYPTEDLINK_FUNCTIONAL = MAINPAGE + "/" + new Regex(CRYPTEDLINK_ORIGINAL, "(video(\\-)?\\d+_\\d+)").getMatch(0);
            }
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUM)) {
            loginrequired = false;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_ALBUM)) {
            loginrequired = false;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUMS)) {
            loginrequired = false;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_MODULE)) {
            loginrequired = false;
        }
        /* Check/fix links before browser access END */
        synchronized (LOCK) {
            boolean loggedIN = getUserLogin(false);
            try {
                if (loginrequired) {
                    /* Login process */
                    if (!loggedIN) {
                        logger.info("Existing account is invalid or no account available, cannot decrypt link: " + CRYPTEDLINK_FUNCTIONAL);
                        return decryptedLinks;
                    }
                    /* Login process end */
                }

                /* Replace section start */
                String newLink = CRYPTEDLINK_FUNCTIONAL;
                if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PUBLIC_LINK) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_CLUB_LINK) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_EVENT_LINK)) {
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
                } else if (isKnownType()) {
                    /* Don't change anything */
                } else {
                    /* We either have a public community or profile --> Get the owner_id and change the link to a wall-link */
                    final String url_owner = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/([^\\?\\&=]+)").getMatch(0);
                    if (url_owner == null || url_owner.equals("")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    /* We either have a public community or profile --> Get the owner_id and change the link to a wall-link */
                    final String ownerName = resolveScreenName_API(url_owner);
                    if (ownerName == null) {
                        logger.warning("Decryption failed - unsupported link? --> " + CRYPTEDLINK_FUNCTIONAL);
                        return null;
                    }
                    final String type = PluginJSonUtils.getJson(br, "type");
                    if (type == null) {
                        logger.warning("Failed to find type for link: " + CRYPTEDLINK_FUNCTIONAL);
                        return null;
                    }
                    if (type.equals("user")) {
                        newLink = MAINPAGE + "/albums" + ownerName;
                    } else {
                        newLink = MAINPAGE + "/wall-" + ownerName;
                    }
                }
                if (newLink.equals(CRYPTEDLINK_FUNCTIONAL)) {
                    logger.info("Link was not changed, continuing with: " + CRYPTEDLINK_FUNCTIONAL);
                } else {
                    logger.info("Link was changed!");
                    logger.info("Old link: " + CRYPTEDLINK_FUNCTIONAL);
                    logger.info("New link: " + newLink);
                    logger.info("Continuing with: " + newLink);
                    CRYPTEDLINK_FUNCTIONAL = newLink;
                }
                /* Replace section end */

                /* Decryption process START */
                if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_MODULE)) {
                    decryptWallPostSpecifiedPhoto();
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
                    decryptAudioPage();
                } else if (isSingeVideo(CRYPTEDLINK_FUNCTIONAL)) {
                    /* Single video */
                    decryptSingleVideo_API(CRYPTEDLINK_FUNCTIONAL);
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_VIDEO_ALBUM)) {
                    /**
                     * Video-Albums Example: http://vk.com/videos575934598 Example2: http://vk.com/video?section=tagged&id=46468795637
                     */
                    decryptVideoAlbum();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_VIDEO_COMMUNITY_ALBUM)) {
                    /**
                     * Community-Albums Example: http://vk.com/video?gid=41589556
                     */
                    decryptCommunityVideoAlbum();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_ALBUM)) {
                    /**
                     * Photo album Examples: http://vk.com/photos575934598 http://vk.com/id28426816 http://vk.com/album87171972_0
                     */
                    decryptPhotoAlbum();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PHOTO_ALBUMS)) {
                    /**
                     * Photo albums lists/overviews Example: http://vk.com/albums46486585
                     */
                    decryptPhotoAlbums();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_WALL_POST_LINK)) {
                    /**
                     * Single posts of wall links: https://vk.com/wall-28122291_906
                     */
                    decryptWallPost_API();
                    if (decryptedLinks.size() == 0) {
                        logger.info("Check your plugin settings -> They affect the results!");
                    }
                } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_GENERAL_WALL_LINK)) {
                    if (br.containsHTML("You are not allowed to view this community\\&#39;s wall|Вы не можете просматривать стену этого сообщества|Nie mo\\&#380;esz ogl\\&#261;da\\&#263; \\&#347;ciany tej spo\\&#322;eczno\\&#347;ci")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    } else if (br.containsHTML("id=\"wall_empty\"")) {
                        logger.info("Wall is empty: " + CRYPTEDLINK_FUNCTIONAL);
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    decryptWallLink_API();
                    logger.info("Decrypted " + decryptedLinks.size() + " total links out of a wall-link");
                    if (decryptedLinks.size() == 0) {
                        logger.info("Check your plugin settings -> They affect the results!");
                    }
                } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_DOCS)) {
                    decryptDocs();
                } else {
                    /*
                     * Unsupported link --> Should never happen -> Error handling -> Either link offline or plugin broken
                     */
                    if (br.containsHTML("class=\"profile_blocked\"")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    logger.warning("Cannot decrypt unsupported linktype: " + CRYPTEDLINK_FUNCTIONAL);
                    return null;
                }
            } catch (final BrowserException e) {
                logger.warning("Browser exception thrown: " + e.getMessage());
                logger.warning("Decrypter failed for link: " + CRYPTEDLINK_FUNCTIONAL);
                e.printStackTrace();
            } catch (final DecrypterException e) {
                try {
                    if (e.getMessage().equals(EXCEPTION_ACCPROBLEM)) {
                        logger.info("Account problem! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                        return decryptedLinks;
                    } else if (e.getMessage().equals(EXCEPTION_API_UNKNOWN)) {
                        logger.info("Unknown API problem occured! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                        return decryptedLinks;
                    } else if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                        decryptedLinks.add(createOffline(this.CRYPTEDLINK_ORIGINAL));
                        return decryptedLinks;
                    }
                } catch (final Exception x) {
                }
                throw e;
            }
        }
        if (decryptedLinks == null) {
            logger.warning("vk.com: Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
            return null;
        } else {
            logger.info("vk.com: Done, decrypted: " + decryptedLinks.size() + " links!");
        }
        return decryptedLinks;
    }

    /** Checks if the type of a link is clear, meaning we're sure we have no vk.com/username link if this is returns true. */
    private boolean isKnownType() {
        final boolean isKnown = CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_MODULE) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUMS) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_AUDIO_PAGE) || isSingeVideo(CRYPTEDLINK_ORIGINAL) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_WALL_LINK) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_AUDIO) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_COMMUNITY_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_POST_LINK) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_MODULE) || this.CRYPTEDLINK_ORIGINAL.matches(PATTERN_AUDIO_PAGE_oid) || this.CRYPTEDLINK_ORIGINAL.matches(PATTERN_DOCS);
        return isKnown;
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private void decryptAudioAlbum() throws Exception {
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((?:\\-)?\\d+)$").getMatch(0);
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        String fpName = null;
        if (cfg.getBooleanProperty(VKAUDIOS_USEIDASPACKAGENAME, false)) {
            fpName = "audios" + owner_ID;
        } else {
            fpName = br.getRegex("\"htitle\":\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = "vk.com audio - " + owner_ID;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String postData = jd.plugins.hoster.VKontakteRuHoster.getAudioAlbumPostString(this.CRYPTEDLINK_FUNCTIONAL, owner_ID);
        br.postPage(getBaseURL() + "/audio", postData);
        final String[] audioData = jd.plugins.hoster.VKontakteRuHoster.getAudioDataArray(this.br);
        if (audioData == null || audioData.length == 0) {
            decryptedLinks = null;
            return;
        }
        for (final String singleAudioData : audioData) {
            final String[] singleAudioDataAsArray = new Regex(singleAudioData, "'(.*?)'").getColumn(0);
            final String owner_id = singleAudioDataAsArray[0];
            final String content_id = singleAudioDataAsArray[1];
            final String directlink = singleAudioDataAsArray[2];
            final String artist = singleAudioDataAsArray[5];
            final String title = singleAudioDataAsArray[6];
            if (owner_id == null || content_id == null || directlink == null || artist == null || title == null) {
                decryptedLinks = null;
                return;
            }
            final String linkid = owner_id + "_" + content_id;
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + linkid);
            dl.setContentUrl(this.CRYPTEDLINK_FUNCTIONAL);
            dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
            dl.setProperty("directlink", Encoding.htmlDecode(directlink));
            dl.setProperty("content_id", content_id);
            dl.setProperty("owner_id", owner_id);
            dl.setFinalFileName(Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(title.trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            dl.setLinkID(linkid);
            fp.add(dl);
            decryptedLinks.add(dl);
        }
    }

    /** NOT using API audio pages and audio playlist's are similar, TODO: Return host-plugin links here to improve the overall stability. */
    @SuppressWarnings("deprecation")
    private void decryptAudiosAlbum() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("id=\"not_found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
            dl.setProperty("content_id", content_id);
            dl.setProperty("owner_id", owner_id);
            fp.add(dl);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
    }

    /** NOT using API audio pages and audio playlist's are similar, TODO: Return host-plugin links here to improve the overall stability. */
    private void decryptAudioPlaylist() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("id=\"not_found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }

        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "album_id=(\\d+)").getMatch(0);
        final String fpName = br.getRegex("onclick=\"Audio\\.loadAlbum\\(" + albumID + "\\)\">[\t\n\r ]+<div class=\"label\">([^<>\"]*?)</div>").getMatch(0);
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
        }
        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        final String[][] audioLinks = br.getRegex("\"(https?://cs\\d+\\.(vk\\.com|userapi\\.com|vk\\.me)/u\\d+/audio/[a-z0-9]+\\.mp3),\\d+\".*?return false\">([^<>\"]*?)</a></b> \\&ndash; <span class=\"title\">([^<>\"]*?)</span><span class=\"user\"").getMatches();
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
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[3].trim()) + " - " + Encoding.htmlDecode(audioInfo[2].trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            if (fp != null) {
                fp.add(dl);
            }
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }

    }

    /**
     * NOT Using API, TODO: Return host-plugin links here to improve the overall stability.
     *
     * @throws Exception
     */
    private void decryptAudioPage() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("Page not found") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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

    /** Using API */
    private void decryptSingleVideo_API(final String parameter) throws Exception {
        // Check if it's really offline
        final String[] ids = findVideoIDs(parameter);
        final String oid = ids[0];
        final String id = ids[1];
        apiGetPageSafe(getProtocol() + "vk.com/video.php?act=a_flash_vars&vid=" + oid + "_" + id);
        if (br.containsHTML(jd.plugins.hoster.VKontakteRuHoster.HTML_VIDEO_NO_ACCESS) || br.containsHTML(jd.plugins.hoster.VKontakteRuHoster.HTML_VIDEO_REMOVED_FROM_PUBLIC_ACCESS) || this.br.toString().length() < 150) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        try {
            br.setFollowRedirects(false);
            String correctedBR = br.toString().replace("\\", "");
            String embedHash = null;
            String filename = null;
            String embeddedVideo = PluginJSonUtils.getJson(br, "extra_data");
            if (embeddedVideo != null) {
                embeddedVideo = Encoding.htmlDecode(embeddedVideo);
                if (embeddedVideo.startsWith("http")) {
                    decryptedLinks.add(createDownloadlink(embeddedVideo));
                } else if (embeddedVideo.matches("[a-f0-9]{32}")) {
                    decryptedLinks.add(createDownloadlink("http://rutube.ru/video/" + embeddedVideo));
                } else {
                    decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + embeddedVideo));
                }
                return;
            }
            embedHash = new Regex(correctedBR, "\"hash\":\"([a-z0-9]+)\"").getMatch(0);
            if (embedHash == null) {
                decryptedLinks = null;
                return;
            }
            filename = new Regex(correctedBR, "\"md_title\":\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            final FilePackage fp = FilePackage.getInstance();
            /* Find needed information */
            final LinkedHashMap<String, String> foundQualities = findAvailableVideoQualities();
            if (foundQualities == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                decryptedLinks = null;
                return;
            }
            filename = Encoding.htmlDecode(filename.trim());
            filename = encodeUnicode(filename);
            if (cfg.getBooleanProperty(VKVIDEO_USEIDASPACKAGENAME, false)) {
                fp.setName("video" + oid + "_" + id);
            } else {
                fp.setName(filename);
            }
            /* Decrypt qualities, selected by the user */
            final ArrayList<String> selectedQualities = new ArrayList<String>();
            final boolean fastLinkcheck = cfg.getBooleanProperty(FASTLINKCHECK_VIDEO, false);
            if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
                final ArrayList<String> list = new ArrayList<String>(foundQualities.keySet());
                final String highestAvailableQualityValue = list.get(0);
                selectedQualities.add(highestAvailableQualityValue);
            } else {
                /* User selected nothing -> Decrypt everything */
                boolean q240p = cfg.getBooleanProperty(ALLOW_240P, false);
                boolean q360p = cfg.getBooleanProperty(ALLOW_360P, false);
                boolean q480p = cfg.getBooleanProperty(ALLOW_480P, false);
                boolean q720p = cfg.getBooleanProperty(ALLOW_720P, false);
                boolean q1080p = cfg.getBooleanProperty(ALLOW_1080P, false);
                if (!q240p && !q360p && !q480p && !q720p && !q1080p) {
                    q240p = true;
                    q360p = true;
                    q480p = true;
                    q720p = true;
                    q1080p = true;
                }
                if (q240p) {
                    selectedQualities.add("240p");
                }
                if (q360p) {
                    selectedQualities.add("360p");
                }
                if (q480p) {
                    selectedQualities.add("480p");
                }
                if (q720p) {
                    selectedQualities.add("720p");
                }
                if (q1080p) {
                    selectedQualities.add("1080p");
                }
            }
            for (final String selectedQualityValue : selectedQualities) {
                final String finallink = foundQualities.get(selectedQualityValue);
                if (finallink != null) {
                    final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                    final String linkid = oid + "_" + id;
                    dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
                    dl.setContentUrl(getProtocol() + "vk.com/video" + linkid);
                    String ext = finallink.substring(finallink.lastIndexOf("."));
                    if (ext.length() > 5 && finallink.contains(".mp4")) {
                        ext = ".mp4";
                    } else if (ext.length() > 5 && finallink.contains(".flv")) {
                        ext = ".flv";
                    } else {
                        ext = ".mp4";
                    }
                    final String finalfilename = filename + "_" + selectedQualityValue + ext;
                    dl.setFinalFileName(finalfilename);
                    dl.setProperty("directfilename", finalfilename);
                    dl.setProperty("directlink", finallink);
                    dl.setProperty("userid", oid);
                    dl.setProperty("videoid", id);
                    dl.setProperty("embedhash", embedHash);
                    dl.setProperty("selectedquality", selectedQualityValue);
                    dl.setProperty("nologin", true);
                    if (fastLinkcheck) {
                        dl.setAvailable(true);
                    }
                    dl.setLinkID(linkid + "_" + selectedQualityValue);
                    fp.add(dl);
                    decryptedLinks.add(dl);
                }
            }

        } catch (final Throwable e) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
    }

    private String[] findVideoIDs(final String parameter) {
        final String[] ids = new String[2];
        String oid = null;
        String id = null;
        if (parameter.matches(PATTERN_VIDEO_SINGLE_EMBED) || parameter.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH)) {
            final Regex idsRegex = new Regex(parameter, "vk\\.com/video_ext\\.php\\?oid=((?:\\-)?\\d+)\\&id=(\\d+)");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL)) {
            final Regex idsRegex = new Regex(parameter, "((?:\\-)?\\d+)_(\\d+)$");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
            final Regex idsRegex = new Regex(parameter, "((?:\\-)?\\d+)_(\\d+)\\?");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(1);
        }
        ids[0] = oid;
        ids[1] = id;
        return ids;
    }

    /** NOT using API */
    private void decryptPhotoAlbum() throws Exception {
        final String type = "singlephotoalbum";
        if (this.CRYPTEDLINK_FUNCTIONAL.contains("#/album")) {
            this.CRYPTEDLINK_FUNCTIONAL = getProtocol() + "vk.com/album" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(".*?vk\\.com/(photos|id)(?:\\-)?\\d+")) {
            this.CRYPTEDLINK_FUNCTIONAL = this.CRYPTEDLINK_FUNCTIONAL.replaceAll("vk\\.com/(?:photos|id)(?:\\-)?", "vk.com/album") + "_0";
        }
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML(FILEOFFLINE) || br.containsHTML("В альбоме нет фотографий|<title>DELETED</title>")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        if (br.containsHTML("There are no photos in this album")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) zdj&#281").getMatch(0);
        if (numberOfEntrys == null) {
            numberOfEntrys = br.getRegex("count: (\\d+),").getMatch(0);
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
        if (numberOfEntrys == null) {
            logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
            decryptedLinks = null;
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/(?:album|tag)(.+)").getMatch(0));
        fp.setProperty("CLEANUP_NAME", false);
        final String[][] regexesPage1 = { { "><a href=\"/photo(-?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final String[][] regexesAllOthers = { { "><a href=\"/photo(-?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(type, numberOfEntrys, regexesPage1, regexesAllOthers, 80, 40, 80, this.CRYPTEDLINK_FUNCTIONAL, "al=1&part=1&offset=");
        String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/(album.+)").getMatch(0);
        for (final String content_id : decryptedData) {
            if (albumID == null) {
                albumID = "tag" + new Regex(content_id, "\\?tag=(\\d+)").getMatch(0);
            }
            /* Pass those goodies over to the hosterplugin */
            final DownloadLink dl = getSinglePhotoDownloadLink(content_id);
            final String linkid = albumID + "_" + content_id;
            dl.setProperty("albumid", albumID);
            dl.setLinkID(linkid);
            fp.add(dl);
            decryptedLinks.add(dl);
        }
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID) throws IOException {
        final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + photoID);
        dl.setProperty("mainlink", this.CRYPTEDLINK_FUNCTIONAL);
        if (fastcheck_photo) {
            dl.setAvailable(true);
        }
        dl.setName(photoID);
        dl.setContentUrl(getProtocol() + "vk.com/photo" + photoID);
        return dl;
    }

    /** NOT Using API */
    private void decryptPhotoAlbums() throws NumberFormatException, Exception {
        /*
         * Another possibility to get these (but still no API): https://vk.com/al_photos.php act=show_albums&al=1&owner=<owner_id> AblumsXXX
         * --> XXX may also be the owner_id, depending on linktype.
         */
        getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("class=\"photos_no_content\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String type = "multiplephotoalbums";
        if (this.CRYPTEDLINK_FUNCTIONAL.contains("z=")) {
            this.CRYPTEDLINK_FUNCTIONAL = getProtocol() + "vk.com/albums" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "albums((?:\\-)?\\d+)").getMatch(0);
            if (!this.CRYPTEDLINK_FUNCTIONAL.equalsIgnoreCase(br.getURL())) {
                getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
            }
        } else {
            /* not needed as we already have requested this page */
            // getPage(br,parameter);
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) albums?</title>").getMatch(0);
        // Language independent
        if (numberOfEntrys == null) {
            numberOfEntrys = br.getRegex("class=\"summary\">(\\d+)").getMatch(0);
        }
        final String startOffset = br.getRegex("var preload = \\[(\\d+),\"").getMatch(0);
        if (numberOfEntrys == null || startOffset == null) {
            logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
            decryptedLinks = null;
            return;
        }
        /** Photos are placed in different locations, find them all */
        final String[][] regexesPage1 = { { "class=\"photo_row\" id=\"(tag\\d+|album-?\\d+_\\d+)", "0" } };
        final String[][] regexesAllOthers = { { "class=\"photo(_album)?_row\" id=\"(tag\\d+|album-?\\d+_\\d+)", "1" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(type, numberOfEntrys, regexesPage1, regexesAllOthers, Integer.parseInt(startOffset), 12, 18, this.CRYPTEDLINK_FUNCTIONAL, "al=1&part=1&offset=");
        if (decryptedData != null && decryptedData.size() != 0) {
            for (String element : decryptedData) {
                final String decryptedLink = getProtocol() + "vk.com/" + element;
                decryptedLinks.add(createDownloadlink(decryptedLink));
            }
        }
    }

    /** NOT Using API --> NOT possible */
    private void decryptVideoAlbum() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("The owner of this video has either been suspended or deleted")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(-?\\d+)$").getMatch(0);
        String numberofentries = PluginJSonUtils.getJson(br, "videoCount");
        if (numberofentries == null) {
            numberofentries = br.getRegex("class=\"video_summary_count\">(\\d+)<").getMatch(0);
        }
        if (numberofentries == null) {
            numberofentries = PluginJSonUtils.getJson(br, "count");
        }
        final int numberOfEntrys = Integer.parseInt(numberofentries);
        int totalCounter = 0;
        while (totalCounter < numberOfEntrys) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                return;
            }
            String[] videos = null;
            if (totalCounter < 12) {
                final String jsVideoArray = br.getRegex("\"all\":\\{\"silent\":1,\"list\":\\[(.*?)\\],\"count\"").getMatch(0);
                if (jsVideoArray != null) {
                    videos = new Regex(jsVideoArray, "\\[((\\-)?\\d+,\\d+),\"").getColumn(0);
                } else {
                    videos = br.getRegex("class=\"video_row_info_name\">[\t\n\r ]+<a href=\"/video((\\-)?\\d+_\\d+)\"").getColumn(0);
                    if (videos == null || videos.length == 0) {
                        logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
                        decryptedLinks = null;
                        return;
                    }
                }
            } else {
                br.postPage(getBaseURL() + "/al_video.php", "act=load_videos_silent&al=1&offset=" + totalCounter + "&oid=" + albumID);
                videos = br.getRegex("\\[(?:\")?((\\-)?\\d+(?:\")?,(?:\")?\\d+)(?:\")?,\"").getColumn(0);
            }
            if (videos == null || videos.length == 0) {
                break;
            }
            for (String singleVideo : videos) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user, stopping...");
                        return;
                    }
                    singleVideo = singleVideo.replace(",", "_");
                    singleVideo = singleVideo.replace(" ", "");
                    singleVideo = singleVideo.replace("\"", "");
                    logger.info("Decrypting video " + totalCounter + " / " + numberOfEntrys);
                    final String completeVideolink = getProtocol() + "vk.com/video" + singleVideo;
                    this.decryptedLinks.add(createDownloadlink(completeVideolink));
                } finally {
                    totalCounter++;
                }
            }
        }
        logger.info("Total videolinks found: " + totalCounter);
    }

    /** Same function in hoster and decrypter plugin, sync it!! */
    private LinkedHashMap<String, String> findAvailableVideoQualities() {
        /** Find needed information */
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        /* Use cachexxx as workaround e.g. for special videos that need groups permission. */
        final String[][] qualities = { { "cache1080", "url1080", "1080p" }, { "cache720", "url720", "720p" }, { "cache480", "url480", "480p" }, { "cache360", "url360", "360p" }, { "cache240", "url240", "240p" } };
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        for (final String[] qualityInfo : qualities) {
            String finallink = PluginJSonUtils.getJson(br, qualityInfo[0]);
            if (finallink == null) {
                finallink = PluginJSonUtils.getJson(br, qualityInfo[1]);
            }
            if (finallink != null) {
                foundQualities.put(qualityInfo[2], finallink);
            }
        }
        return foundQualities;
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    private void decryptCommunityVideoAlbum() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        final String communityAlbumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
        final String type = "communityvideoalbum";
        if (br.getURL().equals("http://vk.com/video") || br.getURL().equals("https://vk.com/video")) {
            logger.info("Empty Community Video Album: " + this.CRYPTEDLINK_FUNCTIONAL);
            return;
        }
        String numberOfEntrys = br.getRegex("class=\"summary fl_l\">(\\d+) videos</div>").getMatch(0);
        if (numberOfEntrys == null) {
            logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
            decryptedLinks = null;
            return;
        }
        final String[][] regexesPage1 = { { "id=\"video_cont((\\-)?\\d+_\\d+)\"", "0" } };
        final String[][] regexesAllOthers = { { "\\[((\\-)?\\d+, \\d+), 'http", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePagesCommunityVideo(this.CRYPTEDLINK_FUNCTIONAL, type, numberOfEntrys, regexesPage1, regexesAllOthers, 12, 12, 12, getProtocol() + "vk.com/al_video.php", "act=load_videos_silent&al=1&oid=-" + communityAlbumID + "&offset=12");
        final int numberOfFoundVideos = decryptedData.size();
        logger.info("Found " + numberOfFoundVideos + " videos...");
        /**
         * Those links will go through the decrypter again, then they'll finally end up in the vkontakte hoster plugin or in other video
         * plugins
         */
        for (String singleVideo : decryptedData) {
            singleVideo = singleVideo.replace(", ", "_");
            final String completeVideolink = getProtocol() + "vk.com/video" + singleVideo.replace(", ", "");
            decryptedLinks.add(createDownloadlink(completeVideolink));
        }
    }

    /** Using API */
    @SuppressWarnings("unchecked")
    private void decryptWallLink_API() throws Exception {
        long total_numberof_entries;
        final String userID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall((\\-)?\\d+)").getMatch(0);
        final String wallID = "wall" + userID;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(userID);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int currentOffset = 0;
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_LOOPBACK_LINK)) {
            final Regex info = new Regex(CRYPTEDLINK_ORIGINAL, "\\-maxoffset=(\\d+)\\-currentoffset=(\\d+)");
            total_numberof_entries = Long.parseLong(info.getMatch(0));
            currentOffset = Integer.parseInt(info.getMatch(1));
            logger.info("PATTERN_WALL_LOOPBACK_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        } else {
            apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + userID + "&count=1&offset=0&filter=all&extended=0");
            total_numberof_entries = Long.parseLong(br.getRegex("\\{\"response\"\\:\\[(\\d+)").getMatch(0));
            logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        }

        while (currentOffset < total_numberof_entries) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                break;
            }
            logger.info("Starting to decrypt offset " + currentOffset + " / " + total_numberof_entries);
            apiGetPageSafe("https://api.vk.com/method/wall.get?format=json&owner_id=" + userID + "&count=" + API_MAX_ENTRIES_PER_REQUEST + "&offset=" + currentOffset + "&filter=all&extended=0");

            Map<String, Object> map = (Map<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());

            if (map == null) {
                return;
            }
            List<Object> response = (List<Object>) map.get("response");
            for (Object entry : response) {
                if (entry instanceof Map) {
                    decryptWallPost(wallID, (Map<String, Object>) entry, fp);
                }
            }

            logger.info("Decrypted offset " + currentOffset + " / " + total_numberof_entries);
            logger.info("Found " + decryptedLinks.size() + " links so far");
            if (decryptedLinks.size() >= MAX_LINKS_PER_RUN) {
                logger.info("Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Returning link to continue");
                final DownloadLink loopBack = createDownloadlink(this.CRYPTEDLINK_FUNCTIONAL + "-maxoffset=" + total_numberof_entries + "-currentoffset=" + currentOffset);
                fp.add(loopBack);
                decryptedLinks.add(loopBack);
                break;
            }
            currentOffset += API_MAX_ENTRIES_PER_REQUEST;
        }
    }

    /** Decrypts media of single API wall-post json objects. */
    @SuppressWarnings({ "deprecation", "unchecked" })
    private void decryptWallPost(final String wall_ID, final Map<String, Object> entry, FilePackage fp) throws IOException {
        final long id = ((Number) entry.get("id")).longValue();
        final long fromId = ((Number) entry.get("from_id")).longValue();
        final long toId = ((Number) entry.get("to_id")).longValue();
        final String wall_list_id = wall_ID + "_" + id;
        /* URL to show this post. */
        final String wall_single_post_url = "https://vk.com/" + wall_list_id;
        final String post_text = (String) entry.get("text");

        List<Map<String, Object>> attachments = (List<Map<String, Object>>) entry.get("attachments");
        if (attachments == null) {
            return;
        }
        for (Map<String, Object> attachment : attachments) {
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
                    owner_id = typeObject.get("owner_id").toString();
                }
                DownloadLink dl = null;
                String content_id = null;
                String title = null;
                String filename = null;
                if (type.equals(wallpost_type_photo) && vkwall_grabphotos) {
                    content_id = typeObject.get("pid").toString();
                    final String album_id = typeObject.get("aid").toString();
                    final String wall_single_photo_content_url = getProtocol() + "vk.com/" + wall_ID + "?own=1&z=photo" + owner_id + "_" + content_id + "/" + wall_list_id;

                    dl = getSinglePhotoDownloadLink(owner_id + "_" + content_id);
                    /*
                     * Override previously set content URL as this really is the direct link to the picture which works fine via browser.
                     */
                    dl.setContentUrl(wall_single_photo_content_url);
                    dl.setProperty("postID", id);
                    dl.setProperty("albumid", album_id);
                    dl.setProperty("owner_id", owner_id);
                    dl.setProperty("directlinks", typeObject);
                    dl.setProperty("photo_list_id", wall_list_id);
                    dl.setProperty("photo_module", "wall");
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
                    dl.setProperty("postID", id);
                    dl.setProperty("fromId", fromId);
                    dl.setProperty("toId", toId);
                    dl.setProperty("directlink", url);
                    dl.setProperty("owner_id", owner_id);
                    if (fastcheck_audio) {
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
                    dl = createDownloadlink(getProtocol() + "vk.com/video" + owner_id + "_" + content_id);
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
                        dl.setProperty("content_id", content_id);
                        dl.setLinkID(owner_id + "_" + content_id);
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

        /* Check if user wants to add urls inside the posted text */
        if (vkwall_graburlsinsideposts && post_text != null) {
            final String[] post_text_urls = HTMLParser.getHttpLinks(post_text, null);
            if (post_text_urls != null) {
                for (final String posted_url : post_text_urls) {
                    boolean add_url = true;
                    try {
                        add_url = posted_url.matches(vkwall_graburlsinsideposts_regex);
                        logger.info("User-Defined wall-post-url-RegEx seems to be correct");
                    } catch (final Throwable e) {
                        logger.warning("User-Defined wall-post-url-RegEx seems to be wrong");
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

    }

    /** Using API, finds and adds contents of a single wall post. */
    @SuppressWarnings("unchecked")
    private void decryptWallPost_API() throws Exception {
        final String postID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall((\\-)?\\d+_\\d+)").getMatch(0);
        final String wallID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/(wall(\\-)?\\d+)_\\d+").getMatch(0);

        apiGetPageSafe("https://api.vk.com/method/wall.getById?posts=" + postID + "&extended=0&copy_history_depth=2");
        Map<String, Object> map = (Map<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());

        if (map == null) {
            return;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(postID);
        List<Object> response = (List<Object>) map.get("response");
        for (Object entry : response) {
            if (entry instanceof Map) {
                decryptWallPost(wallID, (Map<String, Object>) entry, fp);
            }
        }
        logger.info("Found " + decryptedLinks.size() + " links");
    }

    /** Works offline, simply converts the added link into a link for the host plugin and sets needed IDs. */
    @SuppressWarnings("deprecation")
    private void decryptWallPostSpecifiedPhoto() throws Exception {
        String module;
        String list_id = null;
        list_id = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((?:wall|album)(\\-)?\\d+_\\d+)$").getMatch(0);
        if (list_id.contains("wall")) {
            module = "wall";
        } else {
            module = "public";
        }
        final String owner_id = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "photo((?:\\-)?\\d+)_\\d+").getMatch(0);
        final String content_id = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "photo(?:\\-)?\\d+_(\\d+)").getMatch(0);
        final DownloadLink dl = getSinglePhotoDownloadLink(owner_id + "_" + content_id);
        final String linkid = owner_id + "_" + content_id;
        dl.setContentUrl(CRYPTEDLINK_FUNCTIONAL);
        dl.setProperty("owner_id", owner_id);
        dl.setProperty("content_id", content_id);
        dl.setProperty("photo_module", module);
        dl.setProperty("photo_list_id", list_id);
        dl.setLinkID(linkid);
        decryptedLinks.add(dl);
        return;
    }

    /**
     * NOT Using API
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private void decryptDocs() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("Unfortunately, you are not a member of this group and cannot view its documents") || br.getRedirectLocation() != null) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String owner_ID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((?:\\-)?\\d+)$").getMatch(0);
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
            final DownloadLink dl = getSinglePhotoDownloadLink("https://vk.com/doc" + owner_ID + "_" + content_ID);
            final String linkid = owner_ID + "_" + content_ID;
            dl.setContentUrl(CRYPTEDLINK_FUNCTIONAL);
            dl.setName(Encoding.htmlDecode(filename));
            dl.setDownloadSize(SizeFormatter.getSize(filesize));

            dl.setProperty("owner_id", owner_ID);
            dl.setProperty("content_id", content_ID);
            dl.setLinkID(linkid);
            fp.add(dl);
            decryptedLinks.add(dl);
        }
        return;
    }

    /** NOT using API - general method --> NEVER change a running system! */
    private ArrayList<String> decryptMultiplePages(final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, int increase, int alreadyOnPage, final String postPage, final String postData) throws Exception {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) {
            maxLoops = 0;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinks = 0;

        for (int i = 0; i <= maxLoops; i++) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                break;
            }
            if (i > 0) {
                postPageSafe(postPage, postData + offset);
                for (String regex[] : regexesAllOthers) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
                offset += increase;
            } else {
                for (String regex[] : regexesPageOne) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
            }
            if (addedLinks < increase || decryptedData.size() >= Integer.parseInt(numberOfEntries)) {
                logger.info("Fail safe #1 activated, stopping page parsing at page " + (i + 1) + " of " + (maxLoops + 1) + ", returned " + decryptedData.size() + " results.");
                break;
            }
            if (decryptedData.size() > Integer.parseInt(numberOfEntries)) {
                logger.warning("Somehow this decrypter got more than the total number of video -> Maybe a bug -> Please report: " + this.CRYPTEDLINK_FUNCTIONAL);
                logger.info("Decrypter " + decryptedData.size() + "entries...");
                break;
            }
            sleep(this.cfg.getLongProperty(jd.plugins.hoster.VKontakteRuHoster.SLEEP_PAGINATION_GENERAL, jd.plugins.hoster.VKontakteRuHoster.defaultSLEEP_PAGINATION_GENERAL), this.CRYPTEDLINK);
            logger.info("Parsing page " + (i + 1) + " of " + maxLoops);
        }

        return decryptedData;
    }

    /** NOT using API - general method --> NEVER change a running system! */
    private ArrayList<String> decryptMultiplePagesCommunityVideo(final String parameter, final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, int increase, int alreadyOnPage, final String postPage, final String postData) throws Exception {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) {
            maxLoops = 0;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinks = 0;

        for (int i = 0; i <= maxLoops; i++) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user, stopping...");
                break;
            }
            if (i > 0) {
                br.postPage(postPage, postData + offset);
                for (String regex[] : regexesAllOthers) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
                offset += increase;
            } else {
                for (String regex[] : regexesPageOne) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
            }
            if (addedLinks < increase || decryptedData.size() == Integer.parseInt(numberOfEntries)) {
                logger.info("Fail safe #1 activated, stopping page parsing at page " + (i + 1) + " of " + (maxLoops + 1));
                break;
            }
            if (addedLinks > increase) {
                logger.info("Fail safe #2 activated, stopping page parsing at page " + (i + 1) + " of " + (maxLoops + 1));
                break;
            }
            if (decryptedData.size() > Integer.parseInt(numberOfEntries)) {
                logger.warning("Somehow this decrypter got more than the total number of video -> Maybe a bug -> Please report: " + parameter);
                logger.info("Decrypter " + decryptedData.size() + "entries...");
                break;
            }
            sleep(this.cfg.getLongProperty(jd.plugins.hoster.VKontakteRuHoster.SLEEP_PAGINATION_COMMUNITY_VIDEO, jd.plugins.hoster.VKontakteRuHoster.defaultSLEEP_SLEEP_PAGINATION_COMMUNITY_VIDEO), this.CRYPTEDLINK);
            logger.info("Parsing page " + (i + 1) + " of " + (maxLoops + 1));
        }
        if (decryptedData == null || decryptedData.size() == 0) {
            logger.warning("Decrypter couldn't find theData for linktype: " + type + "\n");
            logger.warning("Decrypter broken for link: " + parameter + "\n");
            return null;
        }
        logger.info("Found " + decryptedData.size() + " links for linktype: " + type);

        return decryptedData;
    }

    private void getPageSafe(final String parameter) throws Exception {
        // for loop will fail here...
        int i = 0;
        while (true) {
            getPage(br, parameter);
            i++;
            // what ever this is.. -raz
            if (br.containsHTML("server number not set \\(0\\)")) {
                logger.info("Server says 'server number not set' --> Retrying");
                if (i > 3) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                sleep(3000, CRYPTEDLINK);
                continue;
            } else if (br.containsHTML(TEMPORARILYBLOCKED)) {
                if (i > 10) {
                    logger.info("Could not bypass ");
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                logger.info("Trying to avoid block " + i + " / 10");
                sleep(this.cfg.getLongProperty(jd.plugins.hoster.VKontakteRuHoster.SLEEP_TOO_MANY_REQUESTS, jd.plugins.hoster.VKontakteRuHoster.defaultSLEEP_TOO_MANY_REQUESTS), CRYPTEDLINK);
                continue;
            } else if (br.getURL().equals(parameter)) {
                // If our current url is already the one we want to access here, break dance!
                break;
            } else if (!parameter.equals(br.getURL()) && i > 3) {
                // for this type we only want to retry x times!
                throw new DecrypterException("Url isn't what we want! And we have exausted retry!");
            } else {
                continue;
            }
        }
        siteGeneralErrorhandling();
    }

    private void postPageSafe(final String page, final String postData) throws Exception {
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
                sleep(this.cfg.getLongProperty(jd.plugins.hoster.VKontakteRuHoster.SLEEP_TOO_MANY_REQUESTS, jd.plugins.hoster.VKontakteRuHoster.defaultSLEEP_TOO_MANY_REQUESTS), CRYPTEDLINK);
                continue;
            }
            if (failed) {
                logger.info("Successfully avoided block!");
            }
            return;
        }
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
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 8:
            logger.info("Invalid request");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 9:
            logger.info("Flood control");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 10:
            logger.info("Internal server error");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 16:
            logger.info("HTTP authorization failed");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 17:
            logger.info("Validation required");
            String redirectUri = PluginJSonUtils.getJson(br, "redirect_uri");
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
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 21:
            logger.info("Permission to perform this action is allowed only for Standalone and OpenAPI applications");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 150:
            logger.info("Invalid timestamp");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 200:
            logger.info("Access to album denied ");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 201:
            logger.info("Access to audio denied");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 203:
            logger.info("Access to group denied");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 300:
            logger.info("This album is full");
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        case 500:
            logger.info("Permission denied. You must enable votes processing in application settings");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 600:
            logger.info("Permission denied. You have no access to operations specified with given object(s)");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        case 603:
            logger.info("Some ads error occured");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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
        final String errcodeSTR = PluginJSonUtils.getJson(br, "error_code");
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
    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final boolean force) throws Exception {
        if (account == null) {
            final PluginForHost hostPlugin = JDUtilities.getPluginForHost("vkontakte.ru");
            account = AccountController.getInstance().getValidAccount(hostPlugin);
            if (account == null) {
                logger.warning("There is no account available, continuing without logging in (if possible)");
                return false;
            }
        }
        try {
            jd.plugins.hoster.VKontakteRuHoster.login(this.br, account);
        } catch (final PluginException e) {
            logger.warning("Login failed - continuing without login");
            account.setValid(false);
            return false;
        }
        logger.info("Logged in successfully");
        return true;
    }

    /**
     * Returns the ownerID which belongs to a name e.g. vk.com/some_name
     *
     * @throws Exception
     */
    private String resolveScreenName_API(final String screenname) throws Exception {
        apiGetPageSafe("https://api.vk.com/method/resolveScreenName?screen_name=" + screenname);
        final String ownerID = PluginJSonUtils.getJson(br, "object_id");
        return ownerID;
    }

    @SuppressWarnings("deprecation")
    private boolean siteHandleSecurityCheck(final String parameter) throws Exception {
        // this task shouldn't be done without an account!, ie. login should have taken place
        if (account == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }

        // this is effectively a login (verification) task! We should synchronise before continuing!
        synchronized (jd.plugins.hoster.VKontakteRuHoster.LOCK) {
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
                    org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/ask");

                    final String to = ajaxBR.getRegex("to: '([^<>\"]*?)'").getMatch(0);
                    final String hash = ajaxBR.getRegex("hash: '([^<>\"]*?)'").getMatch(0);
                    if (to == null || hash == null) {
                        org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/tohash_missing");

                        return false;
                    }
                    String[] preAndPost = ajaxBR.getRegex("class=\"label ta_r\">([^<]+)</div></td>.*?class=\"phone_postfix\">([^<]+)</span></td>").getRow(0);

                    if (preAndPost == null || preAndPost.length != 2) {
                        org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/prepost_missing");
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
                            org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/bad_input");
                            continue;
                        }
                    }
                    phone = code;
                    code = code.substring(start.length(), code.length() - end.length());
                    ajaxBR.postPage(getBaseURL() + "/login.php", "act=security_check&al=1&al_page=3&code=" + code + "&hash=" + Encoding.urlEncode(hash) + "&to=" + Encoding.urlEncode(to));

                    if (!ajaxBR.containsHTML(">Unfortunately, the numbers you have entered are incorrect")) {
                        hasPassed = true;
                        account.setProperty("phone", phone);
                        org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/success");
                        break;
                    } else {
                        phone = null;
                        account.setProperty("phone", Property.NULL);
                        org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/missing_digits/failed");
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
                    org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/digits4/ask");
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
                        org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/digits4/success");
                        break;
                    }
                    org.jdownloader.statistics.StatsManager.I().track("vkontakte/verify/digits4/failed");
                    if (ajaxBR.containsHTML("You can try again in \\d+ hour")) {
                        logger.info("Failed security check, account is banned for some hours!");
                        break;
                    }
                }
                return hasPassed;
            }
        }
    }

    private DownloadLink createOffline(final String parameter) {
        final DownloadLink offline = createOfflinelink(parameter, new Regex(CRYPTEDLINK_FUNCTIONAL, "vk\\.com/(.+)").getMatch(0), null);
        offline.setContentUrl(parameter);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(offline.getName());
        fp.add(offline);
        return offline;
    }

    private boolean isSingeVideo(final String input) {
        return (input.matches(PATTERN_VIDEO_SINGLE_MODULE) || input.matches(PATTERN_VIDEO_SINGLE_SEARCH) || input.matches(PATTERN_VIDEO_SINGLE_ORIGINAL) || input.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST) || input.matches(PATTERN_VIDEO_SINGLE_EMBED) || input.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH));
    }

    /** Handles basic offline errors. */
    private void siteGeneralErrorhandling() throws DecrypterException {
        /* General errorhandling start */
        if (br.containsHTML("Unknown error|Неизвестная ошибка|Nieznany b\\&#322;\\&#261;d")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        } else if (br.containsHTML("Access denied|Ошибка доступа")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        } else if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("vk.com/blank.php")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        /* General errorhandling end */
    }

    /** Sets basic values/cookies */
    private void prepBrowser(final Browser br) {
        jd.plugins.hoster.VKontakteRuHoster.prepBrowser(br);
    }

    /**
     * Basic preparations on user-added links. Make sure to remove unneeded things so that in the end, our links match the desired
     * linktypes. This is especially important because we get required IDs out of these urls or even access them directly without API.
     */
    private void prepCryptedLink() {
        /* Correct encoding, domain and protocol. */
        CRYPTEDLINK_ORIGINAL = Encoding.htmlDecode(CRYPTEDLINK_ORIGINAL).replaceAll("(m\\.)?(vkontakte|vk)\\.(ru|com)/", "vk.com/");
        /* We cannot simply remove all parameters which we usually don't need because...we do sometimes need em! */
        if (this.CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_ALBUM_WITH_UNKNOWN_PARAMS)) {
            this.CRYPTEDLINK_ORIGINAL = removeParamsFromURL(CRYPTEDLINK_ORIGINAL);
        } else {
            /* Remove unneeded parameters. */
            final String[] unwantedParts = { "(\\?profile=\\d+)", "(\\?rev=\\d+)", "(/rev)$", "(\\?albums=\\d+)", "(\\?own=(?:0|1))" };
            for (final String unwantedPart : unwantedParts) {
                final String unwantedData = new Regex(this.CRYPTEDLINK_ORIGINAL, unwantedPart).getMatch(0);
                if (unwantedData != null) {
                    this.CRYPTEDLINK_ORIGINAL = this.CRYPTEDLINK_ORIGINAL.replace(unwantedData, "");
                }
            }
        }
        final String wall_id = new Regex(this.CRYPTEDLINK_ORIGINAL, "\\?w=(wall(\\-)?\\d+_\\d+)").getMatch(0);
        if (wall_id != null) {
            this.CRYPTEDLINK_ORIGINAL = "http://vk.com/" + wall_id;
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

    /** Correct filenames for Windows users */
    private String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}