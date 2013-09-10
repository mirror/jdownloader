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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "https?://(www\\.)?vk\\.com/(?!doc\\d+)(audio(\\.php)?(\\?album_id=\\d+\\&id=|\\?id=)(\\-)?\\d+|audios\\d+|(video(\\-)?\\d+_\\d+(\\?list=[a-z0-9]+)?|videos\\d+|(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged)|video_ext\\.php\\?oid=\\d+\\&id=\\d+(\\&hash=[a-z0-9]+)?|video\\?gid=\\d+)|(photos|tag)\\d+|albums\\-?\\d+|([A-Za-z0-9_\\-]+#/)?album(\\-)?\\d+_\\d+|photo(\\-)?\\d+_\\d+|wall(\\-)?\\d+|[A-Za-z0-9\\-_\\.]+)" }, flags = { 0 })
public class VKontakteRu extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    // Note: PATTERN_VIDEO_SINGLE links should all be decryptable without account but this is not implemented (yet)

    private static Object LOCK = new Object();

    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String EXCEPTION_ACCPROBLEM               = "EXCEPTION_ACCPROBLEM";
    private static final String EXCEPTION_LINKOFFLINE              = "EXCEPTION_LINKOFFLINE";

    private final String        FASTLINKCHECK                      = "FASTLINKCHECK";
    private final String        FASTPICTURELINKCHECK               = "FASTPICTURELINKCHECK";
    private final String        FASTAUDIOLINKCHECK                 = "FASTAUDIOLINKCHECK";
    private static final String ALLOW_BEST                         = "ALLOW_BEST";
    private static final String ALLOW_240P                         = "ALLOW_240P";
    private static final String ALLOW_360P                         = "ALLOW_360P";
    private static final String ALLOW_480P                         = "ALLOW_480P";
    private static final String ALLOW_720P                         = "ALLOW_720P";

    private static final String FILEOFFLINE                        = "(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)";
    private static final String DOMAIN                             = "http://vk.com";
    private static final String PATTERN_AUDIO_GENERAL              = "https?://(www\\.)?vk\\.com/audio.*?";
    private static final String PATTERN_AUDIO_ALBUM                = "https?://(www\\.)?vk\\.com/(audio(\\.php)?\\?id=(\\-)?\\d+|audios(\\-)?\\d+)";
    private static final String PATTERN_VIDEO_SINGLE_ALL           = "https?://(www\\.)?vk\\.com/(video(\\-)?\\d+_\\d+(\\?list=[a-z0-9]+)?|video_ext\\.php\\?oid=\\d+\\&id=\\d+(\\&hash=[a-z0-9]+)?)";
    private static final String PATTERN_VIDEO_SINGLE_ORIGINAL      = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+";
    private static final String PATTERN_VIDEO_SINGLE_ORIGINAL_LIST = "https?://(www\\.)?vk\\.com/video(\\-)?\\d+_\\d+\\?list=[a-z0-9]+";
    private static final String PATTERN_VIDEO_SINGLE_EMBED         = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=\\d+\\&id=\\d+";
    private static final String PATTERN_VIDEO_SINGLE_EMBED_HASH    = "https?://(www\\.)?vk\\.com/video_ext\\.php\\?oid=\\d+\\&id=\\d+\\&hash=[a-z0-9]+";
    private static final String PATTERN_VIDEO_ALBUM                = "https?://(www\\.)?vk\\.com/(video\\?section=tagged\\&id=\\d+|video\\?id=\\d+\\&section=tagged|videos(\\-)?\\d+)";
    private static final String PATTERN_VIDEO_COMMUNITY_ALBUM      = "https?://(www\\.)?vk\\.com/video\\?gid=\\d+";
    private static final String PATTERN_PHOTO_SINGLE               = "https?://(www\\.)?vk\\.com/photo(\\-)?\\d+_\\d+";
    private static final String PATTERN_PHOTO_ALBUM                = ".*?(tag|album(\\-)?\\d+_|photos)\\d+";
    private static final String PATTERN_PHOTO_ALBUMS               = "https?://(www\\.)?vk\\.com/(albums(\\-)?\\d+|id\\d+\\?z=albums\\d+)";
    private static final String PATTERN_WALL_LINK                  = "https?://(www\\.)?vk\\.com/wall(\\-)?\\d+";
    private static final String PATTERN_PUBLIC_LINK                = "https?://(www\\.)?vk\\.com/public\\d+";
    private static final String PATTERN_CLUB_LINK                  = "https?://(www\\.)?vk\\.com/club\\d+";
    private static final String PATTERN_ID_LINK                    = "https?://(www\\.)?vk\\.com/id\\d+";

    private SubConfiguration    cfg                                = null;
    private String              MAINPAGE                           = null;

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString().replace("vkontakte.ru/", "vk.com/").replace("https://", "http://");
        cfg = SubConfiguration.getConfig("vkontakte.ru");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        prepBrowser(br);
        boolean loginrequired = true;
        /** Check/fix links before browser access START */
        if (parameter.matches(PATTERN_PHOTO_SINGLE)) {
            /**
             * Single photo links, those are just passed to the hosterplugin! Example:http://vk.com/photo125005168_269986868
             */
            final DownloadLink decryptedPhotolink = getSinglePhotoDownloadLink(new Regex(parameter, "((\\-)?\\d+_\\d+)").getMatch(0));
            decryptedLinks.add(decryptedPhotolink);
            return decryptedLinks;
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH)) {
            loginrequired = false;
        }
        /** Check/fix links before browser access END */
        synchronized (LOCK) {
            try {
                if (!loginrequired) {
                    br.getPage(parameter);
                } else {
                    /** Login process */
                    if (!getUserLogin(false)) {
                        logger.info("Existing account is invalid or no account available, cannot decrypt link: " + parameter);
                        return decryptedLinks;
                    }
                    br.setFollowRedirects(true);
                    br.getPage(parameter);
                    /**
                     * Retry if login failed Those are 2 different errormessages but refreshing the cookies works fine for both
                     * */
                    String cookie = br.getCookie("http://vk.com", "remixsid");
                    if (br.containsHTML(">Security Check<") || "deleted".equals(cookie) || cookie == null || cookie.equals("")) {
                        this.getPluginConfig().setProperty("logincounter", "-1");
                        this.getPluginConfig().save();
                        br.clearCookies(DOMAIN);
                        br.clearCookies("login.vk.com");
                        if (!getUserLogin(true)) {
                            logger.info("Logindata invalid/refreshing cookies failed, stopping...");
                            return null;
                        }
                        logger.info("Cookies refreshed successfully, continuing to decrypt...");
                    }
                    /** Login process end */
                }

                MAINPAGE = new Regex(br.getURL(), "(https?://vk\\.com)").getMatch(0);

                /** Replace section start */
                String newLink = parameter;
                if (parameter.matches(PATTERN_PUBLIC_LINK) || parameter.matches(PATTERN_CLUB_LINK)) {
                    // group and club links --> wall links
                    newLink = MAINPAGE + "/wall-" + new Regex(parameter, "(\\d+)$").getMatch(0);
                    getPageSafe(newLink);
                } else if (parameter.matches(PATTERN_ID_LINK)) {
                    // Change id links -> albums links
                    newLink = MAINPAGE + "/albums" + new Regex(parameter, "(\\d+)$").getMatch(0);
                    getPageSafe(newLink);
                } else {
                    // No case matched, in order to avoid too many requests we
                    // only access the link here in this case
                    if (!br.getURL().equals(parameter)) getPageSafe(parameter);
                    if (br.containsHTML("id=\"profile_main_actions\"")) {
                        // Change profile links -> albums links
                        String profileAlbums = br.getRegex("id=\"profile_albums\">[\t\n\r ]+<a href=\"(/albums\\d+)\"").getMatch(0);
                        if (profileAlbums == null) profileAlbums = br.getRegex("id=\"profile_photos_module\">[\t\n\r ]+<a href=\"(/albums\\d+)").getMatch(0);
                        if (profileAlbums == null) {
                            logger.warning("Failed to find profileAlbums for profile-link: " + parameter);
                            return null;
                        }
                        newLink = MAINPAGE + profileAlbums;
                        getPageSafe(newLink);
                    } else if (br.containsHTML("id=\"public_like_module\"")) {
                        // Change public page links -> wall links
                        final String wallID = br.getRegex("\"wall_oid\":(\\-\\d+)").getMatch(0);
                        if (wallID == null) {
                            logger.warning("Failed to find wallID for public-page-link: " + parameter);
                            return null;
                        }
                        newLink = MAINPAGE + "/wall" + wallID;
                        getPageSafe(newLink);
                    } else if (br.containsHTML("class=\"group_like_enter_desc\"")) {
                        // For open community links
                        String wallID = br.getRegex("class=\"post all own\" onmouseover=\"wall\\.postOver\\(\\'((\\-)?\\d+)").getMatch(0);
                        // Hm try to get it from another place
                        if (wallID == null) wallID = br.getRegex("href=\"/wall((\\-)?\\d+)_\\d+\" onclick=\"return showWiki").getMatch(0);
                        if (wallID == null) {
                            logger.warning("Failed to find wallID for open-community-link: " + parameter);
                            return null;
                        }
                        newLink = MAINPAGE + "/wall" + wallID;
                        getPageSafe(newLink);
                    }
                }
                if (newLink.equals(parameter)) {
                    logger.info("Link was not changed, continuing with: " + parameter);
                } else {
                    logger.info("Link was changed!");
                    logger.info("Old link: " + parameter);
                    logger.info("New link: " + newLink);
                    logger.info("Continuing with: " + newLink);
                    parameter = newLink;
                }
                /** Replace section end */

                /** Errorhandling start */
                if (br.containsHTML("Unknown error")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
                if (br.containsHTML("Access denied")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
                /** Errorhandling end */

                /** Decryption process START */
                if (parameter.matches(PATTERN_AUDIO_GENERAL)) {
                    if (parameter.matches(PATTERN_AUDIO_ALBUM)) {
                        /** Audio album */
                        decryptedLinks = decryptAudioAlbum(decryptedLinks, parameter);
                        logger.info("Decrypted " + decryptedLinks.size() + " audio-links out of a audio-album-link");
                    } else {
                        /** Single playlists */
                        decryptedLinks = decryptAudioPlaylist(decryptedLinks, parameter);
                        logger.info("Decrypted " + decryptedLinks.size() + " audio-links out of a audio-playlist-link");
                    }
                } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ALL)) {
                    /** Single video */
                    decryptedLinks = decryptSingleVideo(decryptedLinks, parameter);
                } else if (parameter.matches(PATTERN_VIDEO_ALBUM)) {
                    /**
                     * Video-Albums Example: http://vk.com/videos575934598 Example2: http://vk.com/video?section=tagged&id=46468795637
                     */
                    decryptedLinks = decryptVideoAlbum(decryptedLinks, parameter);
                    logger.info("Decrypted " + decryptedLinks.size() + " video-links out of a video album");
                } else if (parameter.matches(PATTERN_VIDEO_COMMUNITY_ALBUM)) {
                    /**
                     * Community-Albums Exaple: http://vk.com/video?gid=41589556
                     */
                    decryptCommunityVideoAlbum(decryptedLinks, parameter);
                    logger.info("Decrypted " + decryptedLinks.size() + " community-video-links out of a community-video-album");
                } else if (parameter.matches(PATTERN_PHOTO_ALBUM)) {
                    /**
                     * Photo album Examples: http://vk.com/photos575934598 http://vk.com/id28426816 http://vk.com/album87171972_0
                     */
                    decryptedLinks = decryptPhotoAlbum(decryptedLinks, parameter);
                    logger.info("Decrypted " + decryptedLinks.size() + " photo-links out of a single-photo-album-link");
                } else if (parameter.matches(PATTERN_PHOTO_ALBUMS)) {
                    /**
                     * Photo albums lists/overviews Example: http://vk.com/albums46486585
                     */
                    decryptedLinks = decryptPhotoAlbums(decryptedLinks, parameter);
                    logger.info("Decrypted " + decryptedLinks.size() + " photo-album-links out of a multiple-photo-albums-link");
                } else {
                    // Unsupported link -> Errorhandling -> Either link offline
                    // or plugin broken
                    if (!parameter.matches(PATTERN_WALL_LINK)) {
                        if (br.containsHTML("class=\"profile_blocked\"")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
                        logger.warning("Cannot decrypt unsupported linktype: " + parameter);
                        return null;
                    } else {
                        if (br.containsHTML("You are not allowed to view this community\\&#39;s wall")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
                        decryptedLinks = decryptWallLink(decryptedLinks, parameter);
                        logger.info("Decrypted " + decryptedLinks.size() + " photo-links out of a wall-link");
                    }
                }
            } catch (final BrowserException e) {
                logger.warning("Browser exception thrown: " + e.getMessage());
                logger.warning("Decrypter failed for link: " + parameter);
            } catch (final DecrypterException e) {
                try {
                    if (e.getMessage().equals(EXCEPTION_ACCPROBLEM)) {
                        logger.info("Account problem! Stopped decryption of link: " + parameter);
                        return decryptedLinks;
                    } else if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                        final DownloadLink offline = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + new Regex(parameter, "(\\d+)$").getMatch(0));
                        offline.setProperty("offline", true);
                        offline.setName(new Regex(parameter, "vk\\.com/(.+)").getMatch(0));
                        decryptedLinks.add(offline);
                        return decryptedLinks;
                    }
                } catch (final Exception x) {
                }
                throw e;
            }
            if (decryptedLinks != null && decryptedLinks.size() > 0) {
                logger.info("Done, decrypted: " + decryptedLinks.size() + " links!");
                sleep(2500l, param);
            }
        }
        return decryptedLinks;

    }

    private ArrayList<DownloadLink> decryptAudioAlbum(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws IOException {
        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postData = null;
        if (new Regex(parameter, "vk\\.com/audio\\?id=\\-\\d+").matches()) {
            postData = "act=load_audios_silent&al=1&edit=0&id=0&gid=" + new Regex(parameter, "(\\d+)$").getMatch(0);
        } else {
            postData = "act=load_audios_silent&al=1&edit=0&gid=0&id=" + new Regex(parameter, "((\\-)?\\d+)$").getMatch(0);
        }
        br.postPage("http://vk.com/audio", postData);
        final String completeData = br.getRegex("\\{\"all\":\\[(\\[.*?\\])\\]\\}").getMatch(0);
        if (completeData == null) return null;
        final String[] audioData = completeData.split(",\\[");
        if (audioData == null || audioData.length == 0) return null;
        for (final String singleAudioData : audioData) {
            final String[] singleAudioDataAsArray = new Regex(singleAudioData, "\\'(.*?)\\'").getColumn(0);
            // This is the audio ID
            final String finallink = "http://vkontaktedecrypted.ru/audiolink/" + singleAudioDataAsArray[1];
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setProperty("postdata", postData);
            dl.setProperty("directlink", Encoding.htmlDecode(singleAudioDataAsArray[2]));
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(singleAudioDataAsArray[5].trim()) + " - " + Encoding.htmlDecode(singleAudioDataAsArray[6].trim()) + ".mp3");
            if (cfg.getBooleanProperty(FASTAUDIOLINKCHECK, false)) dl.setAvailable(true);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptAudioPlaylist(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        if (br.containsHTML("id=\"not_found\"")) {
            logger.info("Empty link: " + parameter);
            return decryptedLinks;
        }

        final String albumID = new Regex(parameter, "album_id=(\\d+)").getMatch(0);
        final String fpName = br.getRegex("onclick=\"Audio\\.loadAlbum\\(" + albumID + "\\)\">[\t\n\r ]+<div class=\"label\">([^<>\"]*?)</div>").getMatch(0);

        int overallCounter = 1;
        final DecimalFormat df = new DecimalFormat("00000");
        final String[][] audioLinks = br.getRegex("\"(http://cs\\d+\\.(vk\\.com|userapi\\.com)/u\\d+/audio/[a-z0-9]+\\.mp3),\\d+\".*?return false\">([^<>\"]*?)</a></b> &ndash; <span class=\"title\">([^<>\"]*?)</span><span class=\"user\"").getMatches();
        if (audioLinks == null || audioLinks.length == 0) return null;
        for (String audioInfo[] : audioLinks) {
            String finallink = audioInfo[0];
            if (finallink == null) return null;
            finallink = "directhttp://" + finallink;
            final DownloadLink dl = createDownloadlink(finallink);
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[3].trim()) + " - " + Encoding.htmlDecode(audioInfo[2].trim()) + ".mp3");
            if (cfg.getBooleanProperty(FASTAUDIOLINKCHECK, false)) dl.setAvailable(true);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptSingleVideo(ArrayList<DownloadLink> decryptedLinks, String parameter) throws Exception {
        // Offline1
        if (br.containsHTML("(class=\"button_blue\"><button id=\"msg_back_button\">Wr\\&#243;\\&#263;</button>|<div class=\"body\">[\t\n\r ]+Access denied)")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
        // Offline2
        if (br.containsHTML("class=\"title\">Error</div>")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
        // Offline3
        if (br.containsHTML("was removed from public access by request of the copyright holder")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
        return findVideolinks(parameter);
    }

    private ArrayList<DownloadLink> findVideolinks(final String parameter) throws Exception {
        br.setFollowRedirects(false);
        final ArrayList<DownloadLink> foundVideolinks = new ArrayList<DownloadLink>();
        String correctedBR = br.toString().replace("\\", "");
        String embedHash = null;
        String oid = null;
        String id = null;
        /** Find OID and ID START */
        if (parameter.matches(PATTERN_VIDEO_SINGLE_EMBED) || parameter.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH)) {
            final Regex idsRegex = new Regex(parameter, "vk\\.com/video_ext\\.php\\?oid=(\\d+)\\&id=(\\d+)");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(1);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL)) {
            final Regex idsRegex = new Regex(parameter, "((\\-)?\\d+)_(\\d+)$");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(2);
        } else if (parameter.matches(PATTERN_VIDEO_SINGLE_ORIGINAL_LIST)) {
            final Regex idsRegex = new Regex(parameter, "((\\-)?\\d+)_(\\d+)\\?");
            oid = idsRegex.getMatch(0);
            id = idsRegex.getMatch(2);
        }
        /** Find OID and ID END */
        if (parameter.matches(PATTERN_VIDEO_SINGLE_EMBED_HASH)) {
            // Embedded video with hash -> Cannot be an extern embed video
            embedHash = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        } else {
            /** Find embed links START */
            // Find youtube.com link if it exists
            String embeddedVideo = new Regex(correctedBR, "youtube\\.com/embed/(.*?)\\?autoplay=").getMatch(0);
            if (embeddedVideo != null) {
                foundVideolinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + embeddedVideo));
                return foundVideolinks;
            }
            // Find rutube.ru link if it exists
            embeddedVideo = new Regex(correctedBR, "url: \\'(https?://video\\.rutube\\.ru/[a-z0-9]+)\\'").getMatch(0);
            if (embeddedVideo != null) {
                foundVideolinks.add(createDownloadlink(embeddedVideo));
                return foundVideolinks;
            }
            // Find vimeo.com link if it exists
            embeddedVideo = new Regex(correctedBR, "player\\.vimeo\\.com/video/(\\d+)").getMatch(0);
            if (embeddedVideo != null) {
                foundVideolinks.add(createDownloadlink("http://vimeo.com/" + embeddedVideo));
                return foundVideolinks;
            }
            embeddedVideo = new Regex(correctedBR, "pdj\\.com/i/swf/og\\.swf\\?jsonURL=(http[^<>\"]*?)\\'").getMatch(0);
            if (embeddedVideo != null) {
                br.getPage(Encoding.htmlDecode(embeddedVideo));
                correctedBR = br.toString().replace("\\", "");
                embeddedVideo = new Regex(correctedBR, "@download_url\":\"(http://promodj\\.com/[^<>\"]*?)\"").getMatch(0);
                if (embeddedVideo == null) return null;
                foundVideolinks.add(createDownloadlink(Encoding.htmlDecode(embeddedVideo)));
                return foundVideolinks;
            }
            embeddedVideo = new Regex(correctedBR, "url: \\'(http://player\\.digitalaccess\\.ru/[^<>\"/]*?\\&siteId=\\d+)\\&").getMatch(0);
            if (embeddedVideo != null) {
                foundVideolinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(embeddedVideo)));
                return foundVideolinks;
            }
            embeddedVideo = new Regex(correctedBR, "\\?file=(http://(www\\.)?1tv\\.ru/[^<>\"]*?)\\'").getMatch(0);
            if (embeddedVideo != null) {
                br.getPage(Encoding.htmlDecode(embeddedVideo));
                embeddedVideo = br.getRegex("<media:content url=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (embeddedVideo != null) {
                    foundVideolinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(embeddedVideo)));
                    return foundVideolinks;
                }
            }
            embeddedVideo = new Regex(correctedBR, "url: \\'//(www\\.)?kinopoisk\\.ru/player/vk/f/([^<>\"]*?)\\'").getMatch(1);
            if (embeddedVideo != null) {
                // http://www.kinopoisk.ru/player/vk/f/518097/ad/6/10/tr/62368/file/kinopoisk.ru-Le-Skylab-99112.mp4
                final Regex dlInfo = new Regex(embeddedVideo, "(\\d+)/ad/\\d+/\\d+/[a-z]{2}/(\\d+)/file/(.+)");
                final String trid = dlInfo.getMatch(1);
                final String film = dlInfo.getMatch(0);
                final String tid = dlInfo.getMatch(2);
                if (trid == null || film == null || tid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String redirectLink = "http://www.kinopoisk.ru/gettrailer.php?trid=" + trid + "&film=" + film + "&from_src=vk&tid=" + tid;
                br.getPage(redirectLink);
                final String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                foundVideolinks.add(createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink)));
                return foundVideolinks;
            }
            /** This one isn't supported via plugin yet */
            embeddedVideo = new Regex(correctedBR, "url: \\'(//myvi\\.ru[^<>\"]*?)\\'").getMatch(0);
            if (embeddedVideo != null) {
                foundVideolinks.add(createDownloadlink(embeddedVideo));
                return foundVideolinks;
            }
            /** Find embed links END */

            /**
             * We couldn't find any external videos so it must be on their servers -> send it to the hosterplugin
             */
            embedHash = br.getRegex("\\\\\"hash2\\\\\":\\\\\"([a-z0-9]+)\\\\\"").getMatch(0);
            if (embedHash == null) {
                if (!br.containsHTML("VideoPlayer4_0\\.swf\\?")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        /** Find needed information */
        getPageSafe("http://vk.com/video_ext.php?oid=" + oid + "&id=" + id + "&hash=" + embedHash);
        String filename = br.getRegex("var video_title = \\'([^<>\"]*?)\\';").getMatch(0);
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final LinkedHashMap<String, String> foundQualities = findAvailableVideoQualities();
        if (foundQualities == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        fp.setName(filename);
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        boolean fastLinkcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        if (cfg.getBooleanProperty(ALLOW_BEST, false)) {
            final ArrayList<String> list = new ArrayList<String>(foundQualities.keySet());
            final String highestAvailableQualityValue = list.get(0);
            selectedQualities.add(highestAvailableQualityValue);
        } else {
            /** User selected nothing -> Decrypt everything */
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
            if (q240p) selectedQualities.add("240p");
            if (q360p) selectedQualities.add("360p");
            if (q480p) selectedQualities.add("480p");
            if (q720p) selectedQualities.add("720p");
        }
        for (final String selectedQualityValue : selectedQualities) {
            final String finallink = foundQualities.get(selectedQualityValue);
            if (finallink != null) {
                final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/videolink/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                dl.setFinalFileName(filename + "_" + selectedQualityValue + finallink.substring(finallink.lastIndexOf(".")));
                dl.setProperty("directlink", finallink);
                dl.setProperty("userid", oid);
                dl.setProperty("videoid", id);
                dl.setProperty("embedhash", embedHash);
                dl.setProperty("selectedquality", selectedQualityValue);
                dl.setProperty("nologin", true);
                if (fastLinkcheck) dl.setAvailable(true);
                fp.add(dl);
                foundVideolinks.add(dl);
            }
        }
        if (foundVideolinks.size() == 0) {
            logger.info("None of the selected qualities were found, decrypting done...");
            return foundVideolinks;
        }
        return foundVideolinks;
    }

    private ArrayList<DownloadLink> decryptPhotoAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException, DecrypterException {
        final String type = "singlephotoalbum";
        if (parameter.contains("#/album")) {
            parameter = "http://vk.com/album" + new Regex(parameter, "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        } else if (parameter.matches(".*?vk\\.com/(photos|id)\\d+")) {
            parameter = parameter.replaceAll("vk\\.com/(photos|id)", "vk.com/album") + "_0";
        }
        if (!parameter.equalsIgnoreCase(br.getURL())) br.getPage(parameter);
        if (br.containsHTML(FILEOFFLINE) || br.containsHTML("(В альбоме нет фотографий|<title>DELETED</title>)")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
        if (br.containsHTML("There are no photos in this album")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }
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
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[][] regexesPage1 = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final String[][] regexesAllOthers = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, 80, 40, 80, parameter, "al=1&part=1&offset=");
        String albumID = new Regex(parameter, "/(album.+)").getMatch(0);
        for (final String element : decryptedData) {
            if (albumID == null) albumID = "tag" + new Regex(element, "\\?tag=(\\d+)").getMatch(0);
            /** Pass those goodies over to the hosterplugin */
            final DownloadLink dl = getSinglePhotoDownloadLink(element);
            dl.setProperty("albumid", albumID);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "/(album|tag)(.+)").getMatch(1));
        fp.setProperty("CLEANUP_NAME", false);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private DownloadLink getSinglePhotoDownloadLink(final String photoID) throws IOException {
        final boolean fastLinkcheck = cfg.getBooleanProperty(FASTPICTURELINKCHECK, false);
        final DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + photoID);
        if (fastLinkcheck) dl.setAvailable(true);
        return dl;
    }

    private ArrayList<DownloadLink> decryptPhotoAlbums(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        final String type = "multiplephotoalbums";
        if (parameter.matches(".*?vk\\.com/id\\d+\\?z=albums\\d+")) {
            parameter = "http://vk.com/albums" + new Regex(parameter, "(\\d+)$").getMatch(0);
            if (!parameter.equalsIgnoreCase(br.getURL())) br.getPage(parameter);
        } else {
            /* not needed as we already have requested this page */
            // br.getPage(parameter);
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) albums?</title>").getMatch(0);
        // Language independant
        if (numberOfEntrys == null) numberOfEntrys = br.getRegex("class=\"summary\">(\\d+)").getMatch(0);
        final String startOffset = br.getRegex("var preload = \\[(\\d+),\"").getMatch(0);
        if (numberOfEntrys == null || startOffset == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Photos are placed in different locations, find them all */
        final String[][] regexesPage1 = { { "class=\"photo_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "0" } };
        final String[][] regexesAllOthers = { { "class=\"photo(_album)?_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "1" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, Integer.parseInt(startOffset), 12, 18, parameter, "al=1&part=1&offset=");
        for (String element : decryptedData) {
            final String decryptedLink = "http://vk.com/" + element;
            decryptedLinks.add(createDownloadlink(decryptedLink));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptVideoAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String albumID = new Regex(parameter, "((\\-)?\\d+)$").getMatch(0);
        final int numberOfEntrys = Integer.parseInt(br.getRegex("\"videoCount\":(\\d+)").getMatch(0));
        int totalCounter = 0;
        int onlineCounter = 0;
        int offlineCounter = 0;
        while (totalCounter < numberOfEntrys) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
            }
            String[] videos = null;
            if (totalCounter < 12) {
                final String jsVideoArray = br.getRegex("videoList: \\{\\'all\\': \\[(.*?)\\]\\]\\}").getMatch(0);
                if (jsVideoArray == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
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
                    singleVideo = singleVideo.replace(", ", "_");
                    logger.info("Decrypting video " + totalCounter + " / " + numberOfEntrys);
                    String completeVideolink = "http://vk.com/video" + singleVideo;
                    br.getPage(completeVideolink);
                    ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
                    temp = decryptSingleVideo(temp, completeVideolink);
                    if (temp == null) {
                        logger.warning("Decrypter broken for link: " + parameter + "\n");
                        logger.warning("stopped at: " + completeVideolink);
                        return null;
                    } else if (temp.size() == 0) {
                        offlineCounter++;
                        logger.info("Continuing, found " + offlineCounter + " offline/invalid videolinks so far...");
                        continue;
                    }
                    final DownloadLink finallink = temp.get(0);
                    decryptedLinks.add(finallink);
                    onlineCounter++;
                } finally {
                    totalCounter++;
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        logger.info("Total links found: " + totalCounter);
        logger.info("Total online links: " + onlineCounter);
        logger.info("Total offline links: " + offlineCounter);
        return decryptedLinks;
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
        return br.getRegex("\"" + key + "\":\"(http:[^<>\"]*?)\"").getMatch(0);
    }

    private ArrayList<DownloadLink> decryptCommunityVideoAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        final String communityAlbumID = new Regex(parameter, "(\\d+)$").getMatch(0);
        final String type = "communityvideoalbum";
        if (!parameter.equalsIgnoreCase(br.getURL())) br.getPage(parameter);
        if (br.getURL().equals("http://vk.com/video")) {
            logger.info("Empty Community Video Album: " + parameter);
            return decryptedLinks;
        }
        String numberOfEntrys = br.getRegex("class=\"summary fl_l\">(\\d+) videos</div>").getMatch(0);
        if (numberOfEntrys == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[][] regexesPage1 = { { "id=\"video_cont((\\-)?\\d+_\\d+)\"", "0" } };
        final String[][] regexesAllOthers = { { "\\[((\\-)?\\d+, \\d+), \\'http", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePagesCommunityVideo(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, 12, 12, 12, "http://vk.com/al_video.php", "act=load_videos_silent&al=1&oid=-" + communityAlbumID + "&offset=12");
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
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptWallLink(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException, DecrypterException {
        String userID = new Regex(parameter, "vk\\.com/wall(\\-)?(\\d+)").getMatch(1);
        if (userID == null) {
            br.getPage("https://api.vk.com/method/resolveScreenName?screen_name=" + new Regex(parameter, "vk\\.com/(.+)").getMatch(0));
            userID = br.getRegex("\"object_id\":(\\d+)").getMatch(0);
            if (userID == null) return null;
            parameter = "http://vk.com/wall-" + userID;
        }
        if (!parameter.equalsIgnoreCase(br.getURL())) br.getPage(parameter);
        if (br.containsHTML(">404 Not Found<")) { throw new DecrypterException(EXCEPTION_LINKOFFLINE); }

        /** Find offset start */
        String endOffset = br.getRegex("href=\"/wall(\\-)?" + userID + "[^<>\"/]*?offset=(\\d+)\" onclick=\"return nav\\.go\\(this, event\\)\"><div class=\"pg_in\">\\&raquo;</div>").getMatch(1);
        if (endOffset == null) {
            int highestFoundOffset = 10;
            final String[] offsets = br.getRegex("(\\?|\\&)offset=(\\d+)\"").getColumn(1);
            if (offsets != null && offsets.length != 0) {
                for (final String offset : offsets) {
                    final int curOffset = Integer.parseInt(offset);
                    if (curOffset > highestFoundOffset) highestFoundOffset = curOffset;
                }
                endOffset = Integer.toString(highestFoundOffset);
            } else {
                endOffset = "10";
            }
        }
        /** Find offset end */

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int correntOffset = 0;
        int maxOffset = Integer.parseInt(endOffset);
        while (correntOffset < maxOffset) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user, stopping...");
                    break;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.851 Stable
            }
            correntOffset += 20;
            logger.info("Starting to decrypt offset " + correntOffset + " / " + maxOffset);
            if (correntOffset > 30) {
                br.postPage(br.getURL(), "al=1&part=1&offset=" + correntOffset);
                if (br.toString().length() < 100) {
                    logger.info("Wall decrypted, stopping...");
                    break;
                }
            }

            // First get all photo links
            // Correct browser html
            br.getRequest().setHtmlCode(br.toString().replace("&quot;", "'"));
            final String[][] photoInfo = br.getRegex("showPhoto\\(\\'((\\-)?\\d+_\\d+)\\', \\'((wall|album)(\\-)?\\d+_\\d+)\\', \\{\\'temp\\':\\{(\\'base\\':.*?\\]\\})").getMatches();
            final String[] albums = br.getRegex("\"(http://vk\\.com/album\\-\\d+_\\d+)\"").getColumn(0);
            final String[] audiolinks = br.getRegex("<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">(.*?)</table>").getColumn(0);
            final String[] videolinks = br.getRegex("event\\);\"  href=\"(/video(\\-)?\\d+_\\d+(\\?list=[a-z0-9]+)?)\"").getColumn(0);
            if ((photoInfo == null || photoInfo.length == 0) && (albums == null || albums.length == 0) && (audiolinks == null || audiolinks.length == 0) && (videolinks == null || videolinks.length == 0)) {
                logger.info("Current offset has no downloadable links, continuing...");
                continue;
            }

            if (photoInfo != null && photoInfo.length != 0) {
                for (final String[] singlePhotoInfo : photoInfo) {
                    final String pictureID = singlePhotoInfo[0];
                    final String other_id = singlePhotoInfo[2];
                    final String directlinks = singlePhotoInfo[5];

                    final DownloadLink dl = getSinglePhotoDownloadLink(pictureID);
                    if (other_id.matches("album\\-\\d+_\\d+")) {
                        dl.setProperty("albumid", other_id);
                    } else {
                        dl.setProperty("wall_id", other_id);
                    }
                    dl.setProperty("directlinks", directlinks);
                    decryptedLinks.add(dl);
                }
                logger.info("Found " + photoInfo.length + " photo links in offset " + correntOffset);
            }

            if (albums != null && albums.length != 0) {
                for (final String album : albums) {
                    decryptedLinks.add(createDownloadlink(album));
                }
                logger.info("Found " + albums.length + " album links in offset " + correntOffset);
            }
            if (audiolinks != null && audiolinks.length != 0) {
                for (final String audioTable : audiolinks) {
                    final String finallink = new Regex(audioTable, "\"(http[^<>\"]*?\\.mp3),\\d+\"").getMatch(0);
                    String artist = new Regex(audioTable, "onclick=\"return nav\\.go\\(this, event\\);\">([^<>]*?)</a></b>").getMatch(0);
                    String title = new Regex(audioTable, "class=\"title\" id=\"title[0-9\\-_]+\">([^<>]*?)</span>").getMatch(0);
                    if (finallink != null && artist != null && title != null) {
                        artist = Encoding.htmlDecode(artist.trim());
                        title = Encoding.htmlDecode(title.trim());
                        final DownloadLink audioLink = createDownloadlink("directhttp://" + finallink);
                        audioLink.setFinalFileName(artist + " - " + title + ".mp3");
                        audioLink.setAvailable(true);
                        decryptedLinks.add(audioLink);
                    }
                }
                logger.info("Found " + albums.length + " audio links in offset " + correntOffset);
            }
            if (videolinks != null && videolinks.length != 0) {
                for (final String videolink : videolinks) {
                    decryptedLinks.add(createDownloadlink("https://vk.com" + videolink));
                }
            }
            logger.info("Decrypted offset " + correntOffset + " / " + maxOffset);
            logger.info("Found " + decryptedLinks.size() + " links so far");
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName("-" + userID);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private ArrayList<String> decryptMultiplePages(final String parameter, final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, int increase, int alreadyOnPage, final String postPage, final String postData) throws IOException {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) maxLoops = 0;
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

    // Same as above with additional errorhandling for community video links
    private ArrayList<String> decryptMultiplePagesCommunityVideo(final String parameter, final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, int increase, int alreadyOnPage, final String postPage, final String postData) throws IOException {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + " entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) maxLoops = 0;
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
        if (!br.getURL().equals(parameter)) {
            if (br.getURL().contains("login.php?act=security_check")) {
                br.getPage(parameter);
                final boolean hasPassed = handleSecurityCheck(parameter);
                if (!hasPassed) {
                    logger.warning("Security check failed for link: " + parameter);
                    throw new DecrypterException(EXCEPTION_ACCPROBLEM);
                }
                br.getPage(parameter);
            } else {
                br.getPage(parameter);
            }
        }
    }

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
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hostPlugin, aa);
        return true;
    }

    private boolean handleSecurityCheck(final String parameter) throws IOException {
        final Browser ajaxBR = br.cloneBrowser();
        boolean hasPassed = false;
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 0; i <= 3; i++) {
            logger.info("Entering security check...");
            final String to = br.getRegex("to: \\'([^<>\"]*?)\\'").getMatch(0);
            final String hash = br.getRegex("hash: \\'([^<>\"]*?)\\'").getMatch(0);
            if (to == null || hash == null) { return false; }
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

    private void prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
        // Set english language
        br.setCookie("http://vk.com/", "remixlang", "3");
        br.setReadTimeout(3 * 60 * 1000);
        br.setCookiesExclusive(false);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}