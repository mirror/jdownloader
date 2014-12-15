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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "https?://(www\\.)?(vk\\.com|vkontakte\\.ru)/(?!doc\\d+)(audio(\\.php)?(\\?album_id=\\d+\\&id=|\\?id=)(\\-)?\\d+|audios\\d+|page\\-\\d+_\\d+|(video(\\-)?\\d+_\\d+(\\?list=[a-z0-9]+)?|videos\\d+|(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged)|video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+(\\&hash=[a-z0-9]+)?|video\\?gid=\\d+|public\\d+\\?z=video(\\-)?\\d+_\\d+((%2F|/)[a-z0-9]+)?|search\\?(c\\[q\\]|c%5Bq%5D)=[^<>\"/]*?\\&c(\\[section\\]|%5Bsection%5D)=video(\\&c(\\[sort\\]|%5Bsort%5D)=\\d+)?\\&z=video(\\-)?\\d+_\\d+)|(photos|tag)\\d+|albums\\-?\\d+|([A-Za-z0-9_\\-]+#/)?album(\\-)?\\d+_\\d+|photo(\\-)?\\d+_\\d+|(wall\\-\\d+_\\d+|wall\\-\\d+\\-maxoffset=\\d+\\-currentoffset=\\d+|wall\\-\\d+)|[A-Za-z0-9\\-_\\.]+)|https?://(www\\.)?vk\\.cc/[A-Za-z0-9]+" }, flags = { 0 })
public class VKontakteRu extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    // Note: PATTERN_VIDEO_SINGLE links should all be decryptable without account but this is not implemented (yet)

    private static Object LOCK = new Object();

    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private static final String     EXCEPTION_ACCPROBLEM                 = "EXCEPTION_ACCPROBLEM";
    private static final String     EXCEPTION_LINKOFFLINE                = "EXCEPTION_LINKOFFLINE";
    private static final String     EXCEPTION_API_UNKNOWN                = "EXCEPTION_API_UNKNOWN";

    /* Settings */
    private static final String     FASTLINKCHECK_VIDEO                  = "FASTLINKCHECK_VIDEO";
    private static final String     FASTLINKCHECK_PICTURES               = "FASTLINKCHECK_PICTURES";
    private static final String     FASTLINKCHECK_AUDIO                  = "FASTLINKCHECK_AUDIO";
    private static final String     ALLOW_BEST                           = "ALLOW_BEST";
    private static final String     ALLOW_240P                           = "ALLOW_240P";
    private static final String     ALLOW_360P                           = "ALLOW_360P";
    private static final String     ALLOW_480P                           = "ALLOW_480P";
    private static final String     ALLOW_720P                           = "ALLOW_720P";
    private static final String     VKWALL_GRAB_ALBUMS                   = "VKWALL_GRAB_ALBUMS";
    private static final String     VKWALL_GRAB_PHOTOS                   = "VKWALL_GRAB_PHOTOS";
    private static final String     VKWALL_GRAB_AUDIO                    = "VKWALL_GRAB_AUDIO";
    private static final String     VKWALL_GRAB_VIDEO                    = "VKWALL_GRAB_VIDEO";
    private static final String     VKWALL_GRAB_LINK                     = "VKWALL_GRAB_LINK";
    private static final String     VKVIDEO_USEIDASPACKAGENAME           = "VKVIDEO_USEIDASPACKAGENAME";

    /* Settings 'in action' */
    private boolean                 wall_grabalbums;
    private boolean                 wall_grabphotos;
    private boolean                 wall_grabaudio;
    private boolean                 wall_grabvideo;
    private boolean                 wall_grablink;

    /* Some supported url patterns */
    private static final String     PATTERN_SHORT                        = "https?://(www\\.)?vk\\.cc/[A-Za-z0-9]+";
    private static final String     PATTERN_GENERAL_AUDIO                = "https?://(www\\.)?vk\\.com/audio.*?";
    private static final String     PATTERN_AUDIO_ALBUM                  = "https?://(www\\.)?vk\\.com/(audio(\\.php)?\\?id=(\\-)?\\d+|audios(\\-)?\\d+)";
    private static final String     PATTERN_AUDIO_PAGE                   = "https?://(www\\.)?vk\\.com/page\\-\\d+_\\d+";
    private static final String     PATTERN_GENERAL_VIDEO_SINGLE         = "https?://(www\\.)?vk\\.com/(video(\\-)?\\d+_\\d+(\\?list=[a-z0-9]+)?|video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+(\\&hash=[a-z0-9]+)?|public\\d+\\?z=video(\\-)?\\d+_\\d+((%2F|/)[a-z0-9]+)?|search\\?(c\\[q\\]|c%5Bq%5D)=[^<>\"/]*?\\&c(\\[section\\]|%5Bsection%5D)=video(\\&c(\\[sort\\]|%5Bsort%5D)=\\d+)?\\&z=video(\\-)?\\d+_\\d+)";
    private static final String     PATTERN_VIDEO_SINGLE_PUBLIC          = "https?://(www\\.)?vk\\.com/public\\d+\\?z=video(\\-)?\\d+_\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_PUBLIC_EXTENDED = "https?://(www\\.)?vk\\.com/public\\d+\\?z=video(\\-)?\\d+_\\d+((%2F|/)[a-z0-9]+)?";
    private static final String     PATTERN_VIDEO_SINGLE_SEARCH          = "https?://(www\\.)?vk\\.com/search\\?(c\\[q\\]|c%5Bq%5D)=[^<>\"/]*?\\&c(\\[section\\]|%5Bsection%5D)=video(\\&c(\\[sort\\]|%5Bsort%5D)=\\d+)?\\&z=video(\\-)?\\d+_\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL        = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_ORIGINAL_LIST   = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+\\?list=[a-z0-9]+";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED           = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+";
    private static final String     PATTERN_VIDEO_SINGLE_EMBED_HASH      = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=(\\-)?\\d+\\&id=\\d+\\&hash=[a-z0-9]+";
    private static final String     PATTERN_VIDEO_ALBUM                  = "https?://(www\\.)?vk\\.com/(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged|videos(\\-)?\\d+)";
    private static final String     PATTERN_VIDEO_COMMUNITY_ALBUM        = "https?://(www\\.)?vk\\.com/video\\?gid=\\d+";
    private static final String     PATTERN_PHOTO_SINGLE                 = "https?://(www\\.)?vk\\.com/photo(\\-)?\\d+_\\d+";
    private static final String     PATTERN_PHOTO_ALBUM                  = ".*?(tag|album(\\-)?\\d+_|photos)\\d+";
    private static final String     PATTERN_PHOTO_ALBUMS                 = "https?://(www\\.)?vk\\.com/(albums(\\-)?\\d+|id\\d+\\?z=albums\\d+)";
    private static final String     PATTERN_GENERAL_WALL_LINK            = "https?://(www\\.)?vk\\.com/wall(\\-)?\\d+(\\-maxoffset=\\d+\\-currentoffset=\\d+)?";
    private static final String     PATTERN_WALL_LOOPBACK_LINK           = "https?://(www\\.)?vk\\.com/wall\\-\\d+\\-maxoffset=\\d+\\-currentoffset=\\d+";
    private static final String     PATTERN_WALL_POST_LINK               = "https?://(www\\.)?vk\\.com/wall\\-\\d+_\\d+";
    private static final String     PATTERN_PUBLIC_LINK                  = "https?://(www\\.)?vk\\.com/public\\d+";
    private static final String     PATTERN_CLUB_LINK                    = "https?://(www\\.)?vk\\.com/club\\d+";
    private static final String     PATTERN_EVENT_LINK                   = "https?://(www\\.)?vk\\.com/event\\d+";
    private static final String     PATTERN_ID_LINK                      = "https?://(www\\.)?vk\\.com/id\\d+";

    /* Some html text patterns: English, Russian, German, Polish */
    public static final String      TEMPORARILYBLOCKED                   = "You tried to load the same page more than once in one second|Вы попытались загрузить более одной однотипной страницы в секунду|Pr\\&#243;bujesz za\\&#322;adowa\\&#263; wi\\&#281;cej ni\\&#380; jedn\\&#261; stron\\&#281; w ci\\&#261;gu sekundy|Sie haben versucht die Seite mehrfach innerhalb einer Sekunde zu laden";
    private static final String     FILEOFFLINE                          = "(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)";
    private static final String     domain                               = "http://vk.com";

    /* API constants */
    private static final String     api_version                          = "5.26";
    private static final String     api_access_token_vkopt               = "YWNmNTdlZjdiZDIwZWM0MGM0ZmRiZTkzOGI5YzM2MWUxZmNlYTUyZjkyNTcwZDk3Y2FlODg4ODYwYzM1MGI5YjQ2MTIzYzk2MWNjNmM5YzExY2U3Mw==";
    /* Used whenever we request arrays via API */
    private static final int        api_max_entries_per_request          = 100;

    /* Internal settings */
    /*
     * Whenever we found this number of links or more, quit the decrypter and add a [b]LOOPBACK_LINK[/b] to cuntinue later in order to avoid
     * memory problems/freezes.
     */
    private static final short      MAX_LINKS_PER_RUN                    = 5000;

    private SubConfiguration        cfg                                  = null;
    private String                  MAINPAGE                             = null;

    private String                  CRYPTEDLINK_FUNCTIONAL               = null;
    private String                  CRYPTEDLINK_ORIGINAL                 = null;
    private CryptedLink             CRYPTEDLINK                          = null;
    private boolean                 fastcheck_photo                      = false;
    private boolean                 fastcheck_audio                      = false;

    private ArrayList<DownloadLink> decryptedLinks                       = null;

    @Override
    protected DownloadLink createDownloadlink(String link) {
        DownloadLink ret = super.createDownloadlink(link);

        return ret;
    }

    /* General errorhandling language implementation: English | Rus | Polish */
    /*
     * Information: General linkstructure: vk.com/ownerID_contentID --> ownerID is always positive for users, negative for communities (also
     * groups??)
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                try {
                    distribute(e);
                } catch (Throwable e1) {
                }
                return super.add(e);
            }

            @Override
            public boolean addAll(Collection<? extends DownloadLink> c) {
                try {
                    distribute(c.toArray(new DownloadLink[] {}));
                } catch (Throwable e) {
                }
                return super.addAll(c);
            }
        };
        br.setFollowRedirects(true);
        CRYPTEDLINK_ORIGINAL = Encoding.htmlDecode(param.toString()).replace("vkontakte.ru/", "vk.com/").replace("https://", "http://");
        CRYPTEDLINK_FUNCTIONAL = CRYPTEDLINK_ORIGINAL;
        CRYPTEDLINK = param;
        /* Set settings */
        cfg = SubConfiguration.getConfig("vkontakte.ru");
        fastcheck_photo = cfg.getBooleanProperty(FASTLINKCHECK_PICTURES, false);
        fastcheck_audio = cfg.getBooleanProperty(FASTLINKCHECK_AUDIO, false);
        wall_grabalbums = cfg.getBooleanProperty(VKWALL_GRAB_ALBUMS, false);
        wall_grabphotos = cfg.getBooleanProperty(VKWALL_GRAB_PHOTOS, false);
        wall_grabaudio = cfg.getBooleanProperty(VKWALL_GRAB_AUDIO, false);
        wall_grabvideo = cfg.getBooleanProperty(VKWALL_GRAB_VIDEO, false);
        wall_grablink = cfg.getBooleanProperty(VKWALL_GRAB_LINK, false);
        if (wall_grabalbums == false && wall_grabphotos == false && wall_grabaudio == false && wall_grabvideo == false && wall_grablink == false) {
            wall_grabalbums = true;
            wall_grabphotos = true;
            wall_grabaudio = true;
            wall_grabvideo = true;
            wall_grablink = true;
        }

        prepBrowser(br);
        boolean loginrequired = true;
        /* Check/fix links before browser access START */
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_SHORT)) {
            loginrequired = false;
            br.getPage(CRYPTEDLINK_ORIGINAL);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("vk.com: Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_SINGLE)) {
            /**
             * Single photo links, those are just passed to the hosterplugin! Example:http://vk.com/photo125005168_269986868
             */
            final DownloadLink decryptedPhotolink = getSinglePhotoDownloadLink(new Regex(CRYPTEDLINK_ORIGINAL, "((\\-)?\\d+_\\d+)").getMatch(0));
            decryptedLinks.add(decryptedPhotolink);
            return decryptedLinks;
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_VIDEO_SINGLE)) {
            loginrequired = false;
            if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_PUBLIC_EXTENDED)) {
                /* Don't change anything */
            } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_PUBLIC)) {
                CRYPTEDLINK_FUNCTIONAL = "https://vk.com/" + new Regex(CRYPTEDLINK_ORIGINAL, "(video(\\-)?\\d+_\\d+)").getMatch(0);
            } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_SEARCH)) {
                CRYPTEDLINK_FUNCTIONAL = "https://vk.com/" + new Regex(CRYPTEDLINK_ORIGINAL, "(video(\\-)?\\d+_\\d+)$").getMatch(0);
            }
        } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_ALBUM)) {
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

                /* Set correct domain to work with */
                br.getPage("http://vk.com/");
                MAINPAGE = new Regex(br.getURL(), "(https?://vk\\.com)").getMatch(0);

                /* Replace section start */
                String newLink = CRYPTEDLINK_FUNCTIONAL;
                if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_PUBLIC_LINK) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_CLUB_LINK) || CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_EVENT_LINK)) {
                    /* group and club links --> wall links */
                    newLink = MAINPAGE + "/wall-" + new Regex(CRYPTEDLINK_FUNCTIONAL, "((\\-)?\\d+)$").getMatch(0);
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_ID_LINK)) {
                    /* Change id links -> albums links */
                    newLink = MAINPAGE + "/albums" + new Regex(CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_WALL_LOOPBACK_LINK)) {
                    /* Remove loopback-part as it only contains information which we need later but not in the link */
                    newLink = new Regex(CRYPTEDLINK_FUNCTIONAL, "(http://(www\\.)?vk\\.com/wall(\\-)?\\d+)").getMatch(0);
                } else if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_SINGLE_PUBLIC_EXTENDED) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_PHOTO_ALBUMS) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_AUDIO_PAGE) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_VIDEO_SINGLE) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_WALL_LINK) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_GENERAL_AUDIO) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_VIDEO_COMMUNITY_ALBUM) || CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_POST_LINK)) {
                    /* Don't change anything */
                } else {
                    /* We either have a public community or profile --> Get the owner_id and change the link to a wall-link */
                    final String ownerID = resolveScreenNameAPI(new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/(.+)").getMatch(0));
                    final String type = this.getJson("type");
                    if (ownerID == null || type == null) {
                        logger.warning("Failed to find ownerID for link: " + CRYPTEDLINK_FUNCTIONAL);
                        return null;
                    }
                    if (type.equals("user")) {
                        newLink = MAINPAGE + "/albums" + ownerID;
                    } else {
                        newLink = MAINPAGE + "/wall-" + ownerID;
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
                if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_GENERAL_AUDIO)) {
                    if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_ALBUM)) {
                        /* Audio album */
                        decryptAudioAlbum();
                    } else {
                        /* Single playlists */
                        decryptAudioPlaylist();
                    }
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_AUDIO_PAGE)) {
                    /* Audio page */
                    decryptAudioPage();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_GENERAL_VIDEO_SINGLE)) {
                    /* Single video */
                    decryptSingleVideo(CRYPTEDLINK_FUNCTIONAL);
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_VIDEO_ALBUM)) {
                    /**
                     * Video-Albums Example: http://vk.com/videos575934598 Example2: http://vk.com/video?section=tagged&id=46468795637
                     */
                    decryptVideoAlbum();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_VIDEO_COMMUNITY_ALBUM)) {
                    /**
                     * Community-Albums Exaple: http://vk.com/video?gid=41589556
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
                    if (br.containsHTML("class=\"photos_no_content\"")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    decryptPhotoAlbums();
                } else if (CRYPTEDLINK_FUNCTIONAL.matches(PATTERN_WALL_POST_LINK)) {
                    /**
                     * Single posts of wall links: https://vk.com/wall-28122291_906
                     */
                    decryptWallPost();
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
                    decryptWallLink();
                    logger.info("Decrypted " + decryptedLinks.size() + " total links out of a wall-link");
                    if (decryptedLinks.size() == 0) {
                        logger.info("Check your plugin settings -> They affect the results!");
                    }
                } else {
                    /*
                     * Unsupported link --> Should never happen -> Errorhandling -> Either link offline or plugin broken
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
            } catch (final DecrypterException e) {
                try {
                    if (e.getMessage().equals(EXCEPTION_ACCPROBLEM)) {
                        logger.info("Account problem! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                        return decryptedLinks;
                    } else if (e.getMessage().equals(EXCEPTION_API_UNKNOWN)) {
                        logger.info("Unknown API problem occured! Stopped decryption of link: " + CRYPTEDLINK_FUNCTIONAL);
                        return decryptedLinks;
                    } else if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                        final DownloadLink offline = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                        offline.setAvailable(false);
                        offline.setProperty("offline", true);
                        offline.setName(new Regex(CRYPTEDLINK_FUNCTIONAL, "vk\\.com/(.+)").getMatch(0));
                        decryptedLinks.add(offline);
                        return decryptedLinks;
                    }
                } catch (final Exception x) {
                }
                throw e;
            }
            sleep(2500l, param);
        }
        if (decryptedLinks == null) {
            logger.warning("vk.com: Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
            return null;
        } else {
            logger.info("vk.com: Done, decrypted: " + decryptedLinks.size() + " links!");
        }
        return decryptedLinks;
    }

    /**
     * NOT Using API
     * 
     * @throws Exception
     */
    private void decryptAudioAlbum() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        String fpName = br.getRegex("\"htitle\":\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) {
            fpName = "vk.com audio - " + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postData = null;
        if (new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/audio\\?id=\\-\\d+").matches()) {
            postData = "act=load_audios_silent&al=1&edit=0&id=0&gid=" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
        } else {
            postData = "act=load_audios_silent&al=1&edit=0&gid=0&id=" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((\\-)?\\d+)$").getMatch(0) + "&please_dont_ddos=2";
        }
        br.postPage("http://vk.com/audio", postData);
        final String completeData = br.getRegex("\\{\"all\":\\[(\\[.*?\\])\\]\\}").getMatch(0);
        if (completeData == null) {
            decryptedLinks = null;
            return;
        }
        final String[] audioData = completeData.split(",\\[");
        if (audioData == null || audioData.length == 0) {
            decryptedLinks = null;
            return;
        }
        for (final String singleAudioData : audioData) {
            final String[] singleAudioDataAsArray = new Regex(singleAudioData, "\\'(.*?)\\'").getColumn(0);
            final String owner_id = singleAudioDataAsArray[0];
            final String content_id = singleAudioDataAsArray[1];
            final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + owner_id.replace("-", "") + "_" + content_id);
            dl.setProperty("directlink", Encoding.htmlDecode(singleAudioDataAsArray[2]));
            dl.setProperty("content_id", content_id);
            dl.setProperty("owner_id", owner_id);
            dl.setFinalFileName(Encoding.htmlDecode(singleAudioDataAsArray[5].trim()) + " - " + Encoding.htmlDecode(singleAudioDataAsArray[6].trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
    }

    /** NOT using API audio pages and audio playlists are similar */
    private void decryptAudioPlaylist() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("id=\"not_found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }

        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "album_id=(\\d+)").getMatch(0);
        final String fpName = br.getRegex("onclick=\"Audio\\.loadAlbum\\(" + albumID + "\\)\">[\t\n\r ]+<div class=\"label\">([^<>\"]*?)</div>").getMatch(0);

        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        final String[][] audioLinks = br.getRegex("\"(https?://cs\\d+\\.(vk\\.com|userapi\\.com|vk\\.me)/u\\d+/audio/[a-z0-9]+\\.mp3),\\d+\".*?return false\">([^<>\"]*?)</a></b> &ndash; <span class=\"title\">([^<>\"]*?)</span><span class=\"user\"").getMatches();
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
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[3].trim()) + " - " + Encoding.htmlDecode(audioInfo[2].trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
    }

    /**
     * NOT Using API
     * 
     * @throws Exception
     */
    private void decryptAudioPage() throws Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML("Page not found")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }

        final String pageID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "page\\-(\\d+_\\d+)").getMatch(0);
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = "vk.com page " + pageID;
        }

        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        final String[][] audioLinks = br.getRegex("\"(https?://cs[a-z0-9]+\\.(vk\\.com|userapi\\.com|vk\\.me)/u\\d+/audio[^<>\"]*?)\".*?onclick=\"return nav\\.go\\(this, event\\);\">([^<>\"]*?)</a></b> \\&ndash; <span class=\"title\" id=\"title\\d+_\\d+_\\d+\">([^<>\"]*?)</span>").getMatches();
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
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[2].trim()) + " - " + Encoding.htmlDecode(audioInfo[3].trim()) + ".mp3");
            if (fastcheck_audio) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
    }

    /** Using API */
    private void decryptSingleVideo(final String parameter) throws Exception {
        // Check if it's really offline
        final String[] ids = findVideoIDs(parameter);
        final String oid = ids[0];
        final String id = ids[1];
        apiGetPageSafe("http://vk.com/video.php?act=a_flash_vars&vid=" + oid + "_" + id);
        if (br.containsHTML("NO_ACCESS") || br.containsHTML("No htmlCode read")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        try {
            br.setFollowRedirects(false);
            String correctedBR = br.toString().replace("\\", "");
            String embedHash = null;
            String filename = null;
            String embeddedVideo = br.getRegex("\"extra_data\":\"([^<>\"]*?)\"").getMatch(0);
            if (embeddedVideo != null) {
                embeddedVideo = Encoding.htmlDecode(embeddedVideo.replace("\\", ""));
                if (embeddedVideo.startsWith("http")) {
                    decryptedLinks.add(createDownloadlink(embeddedVideo));
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
                if (q240p == false && q360p == false && q480p == false && q720p == false) {
                    q240p = true;
                    q360p = true;
                    q480p = true;
                    q720p = true;
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
            }
            for (final String selectedQualityValue : selectedQualities) {
                final String finallink = foundQualities.get(selectedQualityValue);
                if (finallink != null) {
                    final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + System.currentTimeMillis() + new Random().nextInt(1000000));

                    try {/* JD2 only */
                        dl.setContentUrl("https://vk.com/video" + oid + "_" + id);
                    } catch (Throwable e) {/* Stable */
                        dl.setBrowserUrl("https://vk.com/video" + oid + "_" + id);
                    }

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
            final Regex idsRegex = new Regex(parameter, "vk\\.com/video_ext\\.php\\?oid=((\\-)?\\d+)\\&id=(\\d+)");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(2);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL)) {
            final Regex idsRegex = new Regex(parameter, "((\\-)?\\d+)_(\\d+)$");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(2);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
            final Regex idsRegex = new Regex(parameter, "((\\-)?\\d+)_(\\d+)\\?");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(2);
        }
        ids[0] = oid;
        ids[1] = id;
        return ids;
    }

    /** NOT using API */
    private void decryptPhotoAlbum() throws Exception {
        final String type = "singlephotoalbum";
        if (this.CRYPTEDLINK_FUNCTIONAL.contains("#/album")) {
            this.CRYPTEDLINK_FUNCTIONAL = "http://vk.com/album" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        } else if (this.CRYPTEDLINK_FUNCTIONAL.matches(".*?vk\\.com/(photos|id)\\d+")) {
            this.CRYPTEDLINK_FUNCTIONAL = this.CRYPTEDLINK_FUNCTIONAL.replaceAll("vk\\.com/(photos|id)", "vk.com/album") + "_0";
        }
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        if (br.containsHTML(FILEOFFLINE) || br.containsHTML("(В альбоме нет фотографий|<title>DELETED</title>)")) {
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
        final String[][] regexesPage1 = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final String[][] regexesAllOthers = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(type, numberOfEntrys, regexesPage1, regexesAllOthers, 80, 40, 80, this.CRYPTEDLINK_FUNCTIONAL, "al=1&part=1&offset=");
        String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/(album.+)").getMatch(0);
        for (final String element : decryptedData) {
            if (albumID == null) {
                albumID = "tag" + new Regex(element, "\\?tag=(\\d+)").getMatch(0);
            }
            /** Pass those goodies over to the hosterplugin */
            final DownloadLink dl = getSinglePhotoDownloadLink(element);
            dl.setProperty("albumid", albumID);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(this.CRYPTEDLINK_FUNCTIONAL, "/(album|tag)(.+)").getMatch(1));
        fp.setProperty("CLEANUP_NAME", false);
        fp.addLinks(decryptedLinks);
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID) throws IOException {
        final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + photoID);
        if (fastcheck_photo) {
            dl.setAvailable(true);
        }
        dl.setName(photoID);

        try {/* JD2 only */
            dl.setContentUrl("http://vk.com/photo" + photoID);
        } catch (Throwable e) {
            /* Stable */
            dl.setBrowserUrl("http://vk.com/photo" + photoID);
        }

        return dl;
    }

    /** NOT Using API */
    private void decryptPhotoAlbums() throws NumberFormatException, Exception {
        this.getPageSafe(this.CRYPTEDLINK_FUNCTIONAL);
        final String type = "multiplephotoalbums";
        if (this.CRYPTEDLINK_FUNCTIONAL.matches(".*?vk\\.com/id\\d+\\?z=albums\\d+")) {
            this.CRYPTEDLINK_FUNCTIONAL = "http://vk.com/albums" + new Regex(this.CRYPTEDLINK_FUNCTIONAL, "(\\d+)$").getMatch(0);
            if (!this.CRYPTEDLINK_FUNCTIONAL.equalsIgnoreCase(br.getURL())) {
                br.getPage(this.CRYPTEDLINK_FUNCTIONAL);
            }
        } else {
            /* not needed as we already have requested this page */
            // br.getPage(parameter);
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) albums?</title>").getMatch(0);
        // Language independant
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
        final String[][] regexesPage1 = { { "class=\"photo_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "0" } };
        final String[][] regexesAllOthers = { { "class=\"photo(_album)?_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "1" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(type, numberOfEntrys, regexesPage1, regexesAllOthers, Integer.parseInt(startOffset), 12, 18, this.CRYPTEDLINK_FUNCTIONAL, "al=1&part=1&offset=");
        if (decryptedData != null && decryptedData.size() != 0) {
            for (String element : decryptedData) {
                final String decryptedLink = "http://vk.com/" + element;
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
        final String albumID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "((\\-)?\\d+)$").getMatch(0);
        final int numberOfEntrys = Integer.parseInt(br.getRegex("\"videoCount\":(\\d+)").getMatch(0));
        int totalCounter = 0;
        int onlineCounter = 0;
        int offlineCounter = 0;
        while (totalCounter < numberOfEntrys) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    return;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
            }
            String[] videos = null;
            if (totalCounter < 12) {
                final String jsVideoArray = br.getRegex("videoList: \\{\\'all\\': \\[(.*?)\\]\\]\\}").getMatch(0);
                if (jsVideoArray == null) {
                    logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL);
                    decryptedLinks = null;
                    return;
                }
                videos = new Regex(jsVideoArray, "\\[((\\-)?\\d+, \\d+), \\'").getColumn(0);
            } else {
                br.postPage("https://vk.com/al_video.php", "act=load_videos_silent&al=1&offset=" + totalCounter + "&oid=" + albumID);
                videos = br.getRegex("\\[((\\-)?\\d+, \\d+), \\'").getColumn(0);
            }
            if (videos == null || videos.length == 0) {
                break;
            }
            for (String singleVideo : videos) {
                try {
                    try {
                        if (this.isAbort()) {
                            logger.info("Decryption aborted by user, stopping...");
                            return;
                        }
                    } catch (final Throwable e) {
                        // Not available in 0.9.851 Stable
                    }
                    singleVideo = singleVideo.replace(", ", "_");
                    logger.info("Decrypting video " + totalCounter + " / " + numberOfEntrys);
                    final String completeVideolink = "http://vk.com/video" + singleVideo;
                    try {
                        br.getPage(completeVideolink);
                        decryptSingleVideo(completeVideolink);
                    } catch (final DecrypterException e) {
                        /* Catch offline case and handle it */
                        final DownloadLink offline = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                        offline.setProperty("offline", true);
                        offline.setName(singleVideo);
                        decryptedLinks.add(offline);
                    }
                    if (decryptedLinks == null) {
                        logger.warning("Decrypter broken for link: " + this.CRYPTEDLINK_FUNCTIONAL + "\n");
                        logger.warning("stopped at: " + completeVideolink);
                        decryptedLinks = null;
                        return;
                    } else if (decryptedLinks.size() == 0) {
                        offlineCounter++;
                        logger.info("Continuing, found " + offlineCounter + " offline/invalid videolinks so far...");
                        continue;
                    }
                    onlineCounter++;
                } finally {
                    totalCounter++;
                }
            }
        }
        logger.info("Total links found: " + totalCounter);
        logger.info("Total online links: " + onlineCounter);
        logger.info("Total offline links: " + offlineCounter);
    }

    /** Same function in hoster and decrypterplugin, sync it!! */
    private LinkedHashMap<String, String> findAvailableVideoQualities() {
        /** Find needed information */
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        final String[][] qualities = { { "url720", "720p" }, { "url480", "480p" }, { "url360", "360p" }, { "url240", "240p" } };
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        for (final String[] qualityInfo : qualities) {
            final String finallink = getJson(qualityInfo[0]);
            if (finallink != null) {
                foundQualities.put(qualityInfo[1], finallink);
            }
        }
        return foundQualities;
    }

    private String getJson(final String key) {
        return getJson(this.br.toString(), key);
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.\\-]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^\"]*?)\"").getMatch(1);
        }
        return result;
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
        if (br.getURL().equals("http://vk.com/video")) {
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
        final String[][] regexesAllOthers = { { "\\[((\\-)?\\d+, \\d+), \\'http", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePagesCommunityVideo(this.CRYPTEDLINK_FUNCTIONAL, type, numberOfEntrys, regexesPage1, regexesAllOthers, 12, 12, 12, "http://vk.com/al_video.php", "act=load_videos_silent&al=1&oid=-" + communityAlbumID + "&offset=12");
        final int numberOfFoundVideos = decryptedData.size();
        logger.info("Found " + numberOfFoundVideos + " videos...");
        /**
         * Those links will go through the decrypter again, then they'll finally end up in the vkontakte hoster plugin or in other video
         * plugins
         */
        for (String singleVideo : decryptedData) {
            singleVideo = singleVideo.replace(", ", "_");
            final String completeVideolink = "http://vk.com/video" + singleVideo.replace(", ", "");
            decryptedLinks.add(createDownloadlink(completeVideolink));
        }
    }

    /** Using API */
    private void decryptWallLink() throws Exception {
        long total_numberof_entries;
        final String userID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall((\\-)?\\d+)").getMatch(0);

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int currentOffset = 0;
        if (CRYPTEDLINK_ORIGINAL.matches(PATTERN_WALL_LOOPBACK_LINK)) {
            final Regex info = new Regex(CRYPTEDLINK_ORIGINAL, "\\-maxoffset=(\\d+)\\-currentoffset=(\\d+)");
            total_numberof_entries = Long.parseLong(info.getMatch(0));
            currentOffset = Integer.parseInt(info.getMatch(1));
            logger.info("PATTERN_WALL_LOOPBACK_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        } else {
            apiPostPageSafe("https://vk.com/api.php", "v=" + api_version + "&format=json&owner_id=" + userID + "&count=1&offset=0&filter=all&extended=0&method=wall.get&access_token=" + Encoding.Base64Decode(api_access_token_vkopt) + "&oauth=1");
            total_numberof_entries = Long.parseLong(getJson("count"));
            logger.info("PATTERN_WALL_LINK has a max offset of " + total_numberof_entries + " and a current offset of " + currentOffset);
        }
        // final BigDecimal bd = new BigDecimal((double) total_numberof_entries / entries_per_request);
        // final int total_numberof_requests = bd.setScale(0, BigDecimal.ROUND_UP).intValue();

        while (currentOffset < total_numberof_entries) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
            }
            logger.info("Starting to decrypt offset " + currentOffset + " / " + total_numberof_entries);
            this.sleep(500, CRYPTEDLINK);
            apiPostPageSafe("https://vk.com/api.php", "v=" + api_version + "&format=json&owner_id=" + userID + "&count=" + api_max_entries_per_request + "&offset=" + currentOffset + "&filter=all&extended=0&method=wall.get&access_token=" + Encoding.Base64Decode(api_access_token_vkopt) + "&oauth=1");
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            final String poststext = br.getRegex("\"items\":\\[(.+)\\]\\}\\}").getMatch(0);
            if (poststext == null) {
                return;
            }
            final String[] posts = poststext.split("\"reposts\":\\{\"count\":\\d+,\"user_reposted\":\\d+\\}\\},");
            // First get all photo links
            // Correct browser html
            for (final String post : posts) {
                final String post_id = getJson(post, "id");
                logger.info("Decrypting post: " + post_id);
                /* Check if the post actually contains downloadable media */
                if (!post.contains("\"attachments\"")) {
                    logger.info("This post contains no media --> Skipping it");
                    continue;
                }
                decryptWallSinglePostJson(post);
            }

            logger.info("Decrypted offset " + currentOffset + " / " + total_numberof_entries);
            logger.info("Found " + decryptedLinks.size() + " links so far");
            if (decryptedLinks.size() >= MAX_LINKS_PER_RUN) {
                logger.info("Reached " + MAX_LINKS_PER_RUN + " links per run limit -> Returning link to continue");
                final DownloadLink loopBack = createDownloadlink(this.CRYPTEDLINK_FUNCTIONAL + "-maxoffset=" + total_numberof_entries + "-currentoffset=" + currentOffset);
                decryptedLinks.add(loopBack);
                break;
            }
            currentOffset += api_max_entries_per_request;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(userID);
        fp.addLinks(decryptedLinks);
    }

    /** Decrypts media of single API wall-post json objects */
    private void decryptWallSinglePostJson(final String source) throws IOException {
        final String attachmentstext = new Regex(source, "\"attachments\":\\[(.*?)\\}\\}\\],").getMatch(0);
        if (attachmentstext == null) {
            return;
        }
        final String[] attachments = attachmentstext.split("\\}\\},\\{");
        for (final String attachment : attachments) {
            final String content_id = getJson(attachment, "id");
            final String owner_id = getJson(attachment, "owner_id");
            final String title = getJson(attachment, "title");
            final String url = getJson(attachment, "url");
            final String type = getJson(attachment, "type");
            if (type == null) {
                return;
            }
            if (type.equals("photo") && wall_grabphotos) {
                final String album_id = getJson(attachment, "album_id");
                if (content_id == null || album_id == null || owner_id == null) {
                    return;
                }

                final DownloadLink dl = getSinglePhotoDownloadLink(owner_id + "_" + content_id);
                dl.setProperty("albumid", album_id);
                dl.setProperty("owner_id", owner_id);
                dl.setProperty("content_id", content_id);
                dl.setProperty("directlinks", attachment);
                decryptedLinks.add(dl);
            } else if (type.equals("doc")) {
                if (title == null || url == null) {
                    return;
                }
                final DownloadLink dl = createDownloadlink(url);
                dl.setDownloadSize(Long.parseLong(getJson(attachment, "size")));
                dl.setName(title);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            } else if (type.equals("audio") && wall_grabaudio) {
                final String artist = getJson(attachment, "artist");
                if (owner_id == null || content_id == null || artist == null || title == null || url == null) {
                    return;
                }
                final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/audiolink/" + owner_id + "_" + content_id);
                dl.setProperty("directlink", url);
                dl.setProperty("content_id", content_id);
                dl.setProperty("owner_id", owner_id);
                if (fastcheck_audio) {
                    dl.setAvailable(true);
                }
                dl.setFinalFileName(artist + " - " + title + ".mp3");
                decryptedLinks.add(dl);
            } else if (type.equals("link") && wall_grablink) {
                if (url == null) {
                    return;
                }
                final DownloadLink dl = createDownloadlink(url);
                decryptedLinks.add(dl);
            } else if (type.equals("video") && wall_grabvideo) {
                if (content_id == null || owner_id == null) {
                    return;
                }
                final DownloadLink dl = createDownloadlink("https://vk.com/video" + owner_id + "_" + content_id);
                decryptedLinks.add(dl);
            } else if (type.equals("album") && wall_grabalbums) {
                final String album_id = getJson(attachment, "album_id");
                if (album_id == null || owner_id == null) {
                    return;
                }
                final DownloadLink dl = createDownloadlink("https://vk.com/album" + owner_id + "_" + album_id);
                decryptedLinks.add(dl);
            } else if (type.equals("poll")) {
                logger.info("Current post only contains a poll --> Skipping it");
            } else {
                logger.warning("Either the type of the current post is unsupported or unselected: " + type);
            }
        }
    }

    /** Using API */
    private void decryptWallPost() throws Exception {
        final String postID = new Regex(this.CRYPTEDLINK_FUNCTIONAL, "vk\\.com/wall(\\-\\d+_\\d+)").getMatch(0);
        apiPostPageSafe("https://vk.com/api.php", "v=" + api_version + "&format=json&posts=" + postID + "&extended=0&method=wall.getById&access_token=" + Encoding.Base64Decode(api_access_token_vkopt) + "&oauth=1");
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        if (!br.containsHTML("\"attachments\"")) {
            logger.info("This single post contains no media --> Skipping it");
            return;
        }
        decryptWallSinglePostJson(br.toString());
        logger.info("Found " + decryptedLinks.size() + " links");

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(postID);
        fp.addLinks(decryptedLinks);
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
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
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
                logger.info("Fail safe #1 activated, stopping page parsing at page " + i + " of " + maxLoops);
                break;
            }
            if (decryptedData.size() > Integer.parseInt(numberOfEntries)) {
                logger.warning("Somehow this decrypter got more than the total number of video -> Maybe a bug -> Please report: " + this.CRYPTEDLINK_FUNCTIONAL);
                logger.info("Decrypter " + decryptedData.size() + "entries...");
                break;
            }
            logger.info("Parsing page " + i + " of " + maxLoops);
        }

        return decryptedData;
    }

    /** NOT using API - general method --> NEVER change a running system! */
    private ArrayList<String> decryptMultiplePagesCommunityVideo(final String parameter, final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, int increase, int alreadyOnPage, final String postPage, final String postData) throws IOException {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) {
            maxLoops = 0;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinks = 0;

        for (int i = 0; i <= maxLoops; i++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
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
                logger.info("Fail safe #1 activated, stopping page parsing at page " + i + " of " + maxLoops);
                break;
            }
            if (addedLinks > increase) {
                logger.info("Fail safe #2 activated, stopping page parsing at page " + i + " of " + maxLoops);
                break;
            }
            if (decryptedData.size() > Integer.parseInt(numberOfEntries)) {
                logger.warning("Somehow this decrypter got more than the total number of video -> Maybe a bug -> Please report: " + parameter);
                logger.info("Decrypter " + decryptedData.size() + "entries...");
                break;
            }
            logger.info("Parsing page " + i + " of " + maxLoops);
        }
        if (decryptedData == null || decryptedData.size() == 0) {
            logger.warning("Decrypter couldn't find theData for linktype: " + type + "\n");
            logger.warning("Decrypter broken for link: " + parameter + "\n");
            return null;
        }
        logger.info("Found " + decryptedData.size() + " links for linktype: " + type);

        return decryptedData;
    }

    // Handle all kinds of stuff that disturbs the downloadflow
    private void getPageSafe(final String parameter) throws Exception {
        // If our current url is already the one we want to access here, don't access it!
        for (int i = 1; i <= 3; i++) {
            if (br.getURL() == null || !br.getURL().equals(parameter) || br.getRedirectLocation() != null) {
                if (br.getURL() != null && br.getURL().contains("login.php?act=security_check")) {
                    final boolean hasPassed = siteHandleSecurityCheck(parameter);
                    if (!hasPassed) {
                        logger.warning("Security check failed for link: " + parameter);
                        throw new DecrypterException(EXCEPTION_ACCPROBLEM);
                    }
                    br.getPage(parameter);
                } else if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("login.vk.com/?role=fast")) {
                    if (!getUserLogin(true)) {
                        throw new DecrypterException(EXCEPTION_ACCPROBLEM);
                    }
                    br.getPage(parameter);
                } else {
                    br.getPage(parameter);
                }
            } else if (br.containsHTML("server number not set \\(0\\)")) {
                logger.info("Server says 'server number not set' --> Retrying");
                br.getPage(parameter);
                continue;
            } else {
                break;
            }
        }
        siteGeneralErrorhandling();
    }

    private void postPageSafe(final String page, final String postData) throws Exception {
        boolean failed = true;
        boolean failed_once = false;
        for (int i = 1; i <= 10; i++) {
            br.postPage(page, postData);
            if (br.containsHTML(TEMPORARILYBLOCKED)) {
                failed_once = true;
                logger.info("Trying to avoid block " + i + " / 10");
                this.sleep(3000, CRYPTEDLINK);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) {
            logger.warning("Failed to avoid block!");
            throw new DecrypterException("Blocked");
        } else if (!failed && failed_once) {
            logger.info("Successfully avoided block!");
        }
    }

    /**
     * Returns the ownerID which belongs to a name e.g. vk.com/some_name
     * 
     * @throws Exception
     */
    private String resolveScreenNameAPI(final String screenname) throws Exception {
        apiPostPageSafe("https://vk.com/api.php", "v=" + api_version + "&format=json&screen_name=" + screenname + "&method=utils.resolveScreenName&access_token=" + Encoding.Base64Decode(api_access_token_vkopt) + "&oauth=1");
        final String ownerID = br.getRegex("\"object_id\":(\\d+)").getMatch(0);
        return ownerID;
    }

    private void apiGetPageSafe(final String parameter) throws Exception {
        int counter = 1;
        do {
            br.getPage(parameter);
        } while (apiHandleErrors() && counter <= 3);
    }

    private void apiPostPageSafe(final String page, final String postData) throws Exception {
        int counter = 1;
        do {
            br.postPage(page, postData);
        } while (apiHandleErrors() && counter <= 3);
        if (getCurrentAPIErrorcode() > -1) {
            throw new DecrypterException(EXCEPTION_API_UNKNOWN);
        }
    }

    /**
     * Handles these error-codes: https://vk.com/dev/errors
     * 
     * @return true = ready to retry, false = problem - failed!
     */
    private boolean apiHandleErrors() throws Exception {
        final String errcodeSTR = br.getRegex("\"error_code\":(\\d+)").getMatch(0);
        if (errcodeSTR == null) {
            return false;
        }
        final int errcode = Integer.parseInt(errcodeSTR);
        switch (errcode) {
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
            int counter = 1;
            boolean loginsucceeded = false;
            do {
                loginsucceeded = getUserLogin(true);
            } while (!loginsucceeded && counter <= 3);
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
            break;
        }
        return false;
    }

    /** Returns current API 'error_code', returns -1 if there is none */
    private int getCurrentAPIErrorcode() {
        final String errcodeSTR = br.getRegex("\"error_code\":(\\d+)").getMatch(0);
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

    /** Log in the account of the hostplugin */
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("vkontakte.ru");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.VKontakteRuHoster) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    private boolean siteHandleSecurityCheck(final String parameter) throws IOException {
        final Browser ajaxBR = br.cloneBrowser();
        boolean hasPassed = false;
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 0; i <= 3; i++) {
            logger.info("Entering security check...");
            final String to = br.getRegex("to: \\'([^<>\"]*?)\\'").getMatch(0);
            final String hash = br.getRegex("hash: \\'([^<>\"]*?)\\'").getMatch(0);
            if (to == null || hash == null) {
                return false;
            }
            final String code = UserIO.getInstance().requestInputDialog("Enter the last 4 digits of your phone number for vkontakte.ru :");
            ajaxBR.postPage("https://vk.com/login.php", "act=security_check&al=1&al_page=3&code=" + code + "&hash=" + Encoding.urlEncode(hash) + "&to=" + Encoding.urlEncode(to));
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
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0");
        // Set english language
        br.setCookie("http://vk.com/", "remixlang", "3");
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookiesExclusive(false);
        br.setFollowRedirects(false);
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