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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@SuppressWarnings("deprecation")
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FaceBookComGallery extends PluginForDecrypt {
    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    /* must be static so all plugins share same lock */
    private static Object       LOCK             = new Object();
    private static final String FACEBOOKMAINPAGE = "https://www.facebook.com";
    private int                 DIALOGRETURN     = -1;

    public static String[] getAnnotationNames() {
        return new String[] { "facebook.com" };
    }

    public static String[] getAnnotationUrls() {
        return new String[] { TYPE_FBSHORTLINK + "|" + TYPE_FB_REDIRECT_TO_EXTERN_SITE + "|" + TYPE_SINGLE_PHOTO + "|" + TYPE_SINGLE_VIDEO_MANY_TYPES + "|" + TYPE_SINGLE_VIDEO_EMBED + "|" + TYPE_SINGLE_VIDEO_VIDEOS + "|" + TYPE_SET_LINK_PHOTO + "|" + TYPE_SET_LINK_VIDEO + "|" + TYPE_PHOTOS_ALBUMS_LINK + "|" + TYPE_PHOTOS_OF_LINK + "|" + TYPE_PHOTOS_ALL_LINK + "|" + TYPE_PHOTOS_STREAM_LINK + "|" + TYPE_PHOTOS_STREAM_LINK_2 + "|" + TYPE_PHOTOS_LINK + "|" + TYPE_PHOTOS_LINK_2 + "|" + TYPE_GROUPS_PHOTOS + "|" + TYPE_GROUPS_FILES + "|" + TYPE_PROFILE_PHOTOS + "|" + TYPE_PROFILE_ALBUMS + "|" + TYPE_NOTES + "|" + TYPE_MESSAGE };
    }

    // can be url encoded
    private static final String         COMPONENT_USERNAME              = "(?:[\\%a-zA-Z0-9\\-]+)";
    private static final String         TYPE_FBSHORTLINK                = "https?://(?:www\\.)?on\\.fb\\.me/[A-Za-z0-9]+\\+?";
    private static final String         TYPE_FB_REDIRECT_TO_EXTERN_SITE = "https?://l\\.facebook\\.com/(?:l/[^/]+/.+|l\\.php\\?u=.+)";
    private static final String         TYPE_SINGLE_PHOTO               = "https?://(?:www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+.*?";
    private static final String         TYPE_SINGLE_VIDEO_MANY_TYPES    = "https?://(?:www\\.)?facebook\\.com/(video/video|photo|video)\\.php\\?v=\\d+";
    private static final String         TYPE_SINGLE_VIDEO_EMBED         = "https?://(?:www\\.)?facebook\\.com/video/embed\\?video_id=\\d+";
    private static final String         TYPE_SINGLE_VIDEO_VIDEOS        = "https?://(?:www\\.)?facebook\\.com/.+/videos.*?/\\d+.*?";
    private static final String         TYPE_SET_LINK_PHOTO             = "https?://(?:www\\.)?facebook\\.com/(media/set/\\?set=|media_set\\?set=)o?a[0-9\\.]+(&type=\\d+)?";
    private static final String         TYPE_SET_LINK_VIDEO             = "https?://(?:www\\.)?facebook\\.com/(media/set/\\?set=|media_set\\?set=)vb\\.\\d+.*?";
    private static final String         TYPE_PHOTOS_ALBUMS_LINK         = "https?://(?:www\\.)?facebook\\.com/.+photos_albums";
    private static final String         TYPE_PHOTOS_OF_LINK             = "https?://(?:www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_of.*";
    private static final String         TYPE_PHOTOS_ALL_LINK            = "https?://(?:www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_all.*";
    private static final String         TYPE_PHOTOS_STREAM_LINK         = "https?://(?:www\\.)?facebook\\.com/[^/]+/photos_stream.*";
    private static final String         TYPE_PHOTOS_STREAM_LINK_2       = "https?://(?:www\\.)?facebook\\.com/pages/[^/]+/\\d+\\?sk=photos_stream&tab=.*";
    private static final String         TYPE_PHOTOS_LINK                = "https?://(?:www\\.)?facebook\\.com/" + COMPONENT_USERNAME + "/photos.*";
    private static final String         TYPE_PHOTOS_LINK_2              = "https?://(?:www\\.)?facebook\\.com/pg/" + COMPONENT_USERNAME + "/photos.*";
    private static final String         TYPE_GROUPS_PHOTOS              = "https?://(?:www\\.)?facebook\\.com/groups/\\d+/photos/";
    private static final String         TYPE_GROUPS_FILES               = "https?://(?:www\\.)?facebook\\.com/groups/\\d+/files/";
    private static final String         TYPE_PROFILE_PHOTOS             = "^https?://(?:www\\.)?facebook\\.com/profile\\.php\\?id=\\d+&sk=photos&collection_token=\\d+(?:%3A|:)\\d+(?:%3A|:)5$";
    private static final String         TYPE_PROFILE_ALBUMS             = "^https?://(?:www\\.)?facebook\\.com/profile\\.php\\?id=\\d+&sk=photos&collection_token=\\d+(?:%3A|:)\\d+(?:%3A|:)6$";
    private static final String         TYPE_NOTES                      = "https?://(?:www\\.)?facebook\\.com/(notes/|note\\.php\\?note_id=).+";
    private static final String         TYPE_MESSAGE                    = "https?://(?:www\\.)?facebook\\.com/messages/.+";
    private static final int            MAX_LOOPS_GENERAL               = 150;
    private static final int            MAX_PICS_DEFAULT                = 5000;
    public static final String          REV_2                           = "1938577";
    public static final String          REV_3                           = "2276891";
    private static String               MAINPAGE                        = "https://www.facebook.com";
    private static final String         CRYPTLINK                       = "facebookdecrypted.com/";
    private static final String         EXCEPTION_LINKOFFLINE           = "EXCEPTION_LINKOFFLINE";
    private static final String         EXCEPTION_NOTLOGGEDIN           = "EXCEPTION_NOTLOGGEDIN";
    private static final String         EXCEPTION_PLUGINDEFECT          = "EXCEPTION_PLUGINDEFECT";
    private static final String         CONTENTUNAVAILABLE              = ">Dieser Inhalt ist derzeit nicht verfügbar|>This content is currently unavailable<";
    private String                      parameter                       = null;
    private boolean                     fastLinkcheckPictures           = true;                                                                                                                 // jd.plugins.hoster.FaceBookComVideos.FASTLINKCHECK_PICTURES_DEFAULT;
    private boolean                     logged_in                       = false;
    private ArrayList<DownloadLink>     decryptedLinks                  = null;
    private final LinkedHashSet<String> dupe                            = new LinkedHashSet<String>();                                                                                           ;
    private boolean                     debug                           = false;

    /*
     * Dear whoever is looking at this - this is a classic example of spaghetticode. If you like spaghettis, go ahead, and get you some
     * tomatoe sauce and eat it but if not, well have fun re-writing this from scratch ;) Wikipedia:
     * http://en.wikipedia.org/wiki/Spaghetti_code
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // for debugging
        if (debug) {
            disableLogger();
        }
        br = new Browser();
        fp = null;
        decryptedLinks = new ArrayList<DownloadLink>();
        dupe.clear();
        br.setAllowedResponseCodes(400);
        br.setLoadLimit(br.getLoadLimit() * 4);
        parameter = param.toString().replace("#!/", "");
        // fastLinkcheckPictures = getPluginConfig().getBooleanProperty(jd.plugins.hoster.FaceBookComVideos.FASTLINKCHECK_PICTURES,
        // jd.plugins.hoster.FaceBookComVideos.FASTLINKCHECK_PICTURES_DEFAULT);
        if (parameter.matches(TYPE_SINGLE_VIDEO_MANY_TYPES) || parameter.matches(TYPE_SINGLE_VIDEO_EMBED) || parameter.matches(TYPE_SINGLE_VIDEO_VIDEOS) || parameter.contains("/video.php?v=")) {
            String id;
            if (parameter.matches(TYPE_SINGLE_VIDEO_VIDEOS)) {
                id = new Regex(parameter, "/videos.*?/(\\d+)/?.*?$").getMatch(0);
            } else {
                id = new Regex(parameter, "(?:v=|video_id=)(\\d+)").getMatch(0);
            }
            final DownloadLink dl = createDownloadlink("https://www.facebookdecrypted.com/video.php?v=" + id);
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (parameter.matches(TYPE_SINGLE_PHOTO)) {
            final String id = new Regex(parameter, "fbid=(\\d+)").getMatch(0);
            final DownloadLink dl = createDownloadlink("https://www.facebookdecrypted.com/photo.php?fbid=" + id);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (parameter.matches(TYPE_FB_REDIRECT_TO_EXTERN_SITE)) {
            decryptedLinks.add(createDownloadlink(getRedirectToExternalSite(parameter)));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        try {
            if (parameter.matches(TYPE_FBSHORTLINK)) {
                br.getPage(parameter);
                final String finallink = br.getRedirectLocation();
                if (br.containsHTML(">Something's wrong here")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
                return decryptedLinks;
            }
            synchronized (LOCK) {
                if (true) {
                    logged_in = login();
                }
            }
            getpagefirsttime(parameter);
            /* temporarily unavailable (or forever, or permission/rights needed) || empty album */
            if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</") || br.containsHTML("class=\"fbStarGridBlankContent\"") || br.containsHTML("<h2 class=\"accessible_elem\">[^<]+</h2>")) {
                /*
                 * problem is with this is , plugin would have to work with multiple languages ! so
                 * ">Sorry, this content isn&#039;t available right now</h2>" would fail .
                 */
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            } else if (br.getURL().matches("https?://(?:www\\.)facebook\\.com/login\\.php.*?")) {
                // login required to perform task
                throw new DecrypterException(EXCEPTION_NOTLOGGEDIN);
            } else if (parameter.matches(TYPE_PHOTOS_ALBUMS_LINK)) {
                decryptProfilePhotosAlbums();
            } else if (parameter.matches(TYPE_PHOTOS_OF_LINK)) {
                decryptPhotosOf();
            } else if (parameter.matches(TYPE_PHOTOS_ALL_LINK)) {
                decryptPhotosAll();
            } else if (parameter.matches(TYPE_PHOTOS_STREAM_LINK) || parameter.matches(TYPE_PHOTOS_STREAM_LINK_2) || parameter.matches(TYPE_SET_LINK_PHOTO)) {
                v2decryptSets();
            } else if (parameter.matches(TYPE_PHOTOS_LINK)) {
                // Old handling removed 05.12.13 in rev 23262
                // decryptPicsGeneral(null); 2018 doesn't work anymore for single photo link
                decryptSinglePic();
            } else if (parameter.matches(TYPE_PHOTOS_LINK_2)) { // pg = photo gallery?
                // Needs a method to get the content
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            } else if (parameter.matches(TYPE_PROFILE_PHOTOS)) {
                decryptPicsProfile();
            } else if (parameter.matches(TYPE_PROFILE_ALBUMS)) {
                v2decryptProfileAlbums();
            } else if (parameter.matches(TYPE_SET_LINK_VIDEO)) {
                v2decryptSets();
            } else if (parameter.matches(TYPE_GROUPS_PHOTOS)) {
                decryptGroupsPhotos();
            } else if (parameter.matches(TYPE_NOTES)) {
                decryptNotes();
            } else if (parameter.matches(TYPE_MESSAGE)) {
                v2decryptMessagePhotos();
            } else {
                logger.info("Unsupported linktype: " + parameter);
                return decryptedLinks;
                // because facebook picks up so many false positives do not throw exception or add offline links.
                // throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
        } catch (final DecrypterException e) {
            if (StringUtils.equals(e.getMessage(), EXCEPTION_LINKOFFLINE)) {
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "facebook\\.com/(.+)").getMatch(0), null));
                return decryptedLinks;
            } else if (StringUtils.equals(e.getMessage(), EXCEPTION_NOTLOGGEDIN)) {
                decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, "facebook\\.com/(.+)").getMatch(0), "Your not logged in! This could have influenced this negative outcome"));
                return decryptedLinks;
            } else if (StringUtils.equals(e.getMessage(), EXCEPTION_PLUGINDEFECT)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            throw e;
        }
        if (decryptedLinks == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        } else if (decryptedLinks.size() == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptProfilePhotosAlbums() throws Exception {
        // note this fucks up unicode!
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)(?:- Photos \\| Facebook)?</title>").getMatch(0);
        final String profileID = getProfileID();
        final String user = getUser(this.br);
        if (user == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String rev = getRev(this.br);
        if (fpName == null) {
            fpName = "Facebook_video_albums_of_user_" + new Regex(parameter, "facebook\\.com/(.*?)/photos_albums").getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            if (this.isAbort()) {
                logger.info("User aborted decryption");
                break;
            }
            int currentMaxPicCount = 18;
            String[] links;
            if (i > 1) {
                String cursor = this.br.getRegex("\\{\"__m\":\"__elem_559218ec_0_0\"\\},\"([^<>\"]*?)\"\\]").getMatch(0);
                if (cursor == null) {
                    cursor = this.br.getRegex("\"cursor\":\"([^<>\"]*?)\"").getMatch(0);
                }
                String collection_token = this.br.getRegex("\"pagelet_timeline_app_collection_([^<>\"/]*?)\"").getMatch(0);
                if (collection_token == null) {
                    /* E.g. from json */
                    collection_token = this.br.getRegex("\"collection_token\":\"([^<>\"]*?)\"").getMatch(0);
                }
                if (collection_token == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (collection_token == null) {
                    logger.warning("Decrypter maybe broken for link: " + parameter);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                // for some reason the collection token returns as 4 but should be 6 in request.
                final String data = "{\"collection_token\":\"" + collection_token.replaceFirst(":4$", ":6") + "\",\"cursor\":\"" + cursor + "\",\"tab_key\":\"photos_albums\",\"profile_id\":" + profileID + ",\"overview\":false,\"ftid\":null,\"order\":null,\"sk\":\"photos\",\"importer_state\":null}";
                final String loadLink = "/ajax/pagelet/generic.php/PhotoAlbumsAppCollectionPagelet?__pc=EXP1%3ADEFAULT&dpr=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__dyn=7AmajEzUGByEogDxyG9gigmzFEbEyGgyi8zQC-C26m6oKezob4q68K5UcU-8wwG3O7R88y8aGjzEZ7KuEjxC264EK14DyU9XxemFEW2PxO2i5o&__req=k&__a=" + (i - 1) + "&__rev=" + rev;
                br.getHeaders().put("Accept", "*/*");
                try {
                    final String result = br.cloneBrowser().getPage(loadLink).toString().replace("\\", "");
                    // note this fucks up unicode!
                    br.getRequest().setHtmlCode(result);
                } catch (BrowserException b) {
                    final String detailedCause = b.getCause() != null ? b.getCause().getMessage() : null;
                    if ("500 Internal Server Error".equals(detailedCause)) {
                        return;
                    }
                }
                links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/media/set/\\?set=(?:a|vb)\\.[0-9\\.]+)\\&amp;type=\\d+\"").getColumn(0);
                currentMaxPicCount = 12;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/[^<>\"]*?)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                if (!logged_in) {
                    logger.warning("You may need to be logged in, no results found!");
                    throw new DecrypterException(EXCEPTION_NOTLOGGEDIN);
                }
                logger.warning("Decrypter broken for the following link or account needed: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            logger.info("Decrypting page " + i + " of ??");
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                final DownloadLink dl = createDownloadlink(link);
                fp.add(dl);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount || profileID == null) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
    }

    private void decryptPhotosAll() throws Exception {
        decryptPicsGeneral("AllPhotosAppCollectionPagelet");
    }

    private void decryptPhotosOf() throws Exception {
        if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</")) {
            logger.info("The link is either offline or an account is needed to grab it: " + parameter);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String profileID = getProfileID();
        String fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        final String user = getUser(this.br);
        final String token = br.getRegex("\"tab_count\":\\d+,\"token\":\"([^<>\"]*?)\"").getMatch(0);
        if (token == null || user == null || profileID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        if (fpName == null) {
            fpName = "Facebook_photos_of_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        String lastfirstID = "";
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            int currentMaxPicCount = 20;
            String[] links;
            if (i > 1) {
                if (br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
                    break;
                }
                final String cursor = br.getRegex("\\[\"pagelet_timeline_app_collection_[^<>\"]*?\",\\{\"[^<>\"]*?\":\"[^<>\"]*?\"\\},\"([^<>\"]*?)\"").getMatch(0);
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (cursor == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (cursor == null) {
                    logger.warning("Decrypter maybe broken for link: " + parameter);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TaggedPhotosAppCollectionPagelet?data=%7B%22collection_token%22%3A%22" + token + "%22%2C%22cursor%22%3A%22" + cursor + "%22%2C%22tab_key%22%3A%22photos_of%22%2C%22profile_id%22%3A" + profileID + "%2C%22overview%22%3Afalse%2C%22ftid%22%3Anull%2C%22order%22%3Anull%2C%22sk%22%3A%22photos%22%7D&__user=" + user + "&__a=1&__dyn=" + getDyn() + "&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for the following link or account needed: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String picID : links) {
                // Another fail safe to prevent loops
                if (picID.equals(lastfirstID)) {
                    stop = true;
                    break;
                }
                if (!dupe.add(picID)) {
                    continue;
                }
                final DownloadLink dl = createPicDownloadlink(picID);
                fp.add(dl);
                distribute(dl);
                decryptedLinks.add(dl);
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                stop = true;
            }
            if (stop) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
    }

    private void decryptPicsProfile() throws Exception {
        String fpName = getPageTitle();
        final boolean profile_setlink = br.getURL().contains("&set=");
        boolean dynamicLoadAlreadyDecrypted = false;
        String type;
        String collection_token = null;
        String profileID;
        String setID;
        String additionalPostData = null;
        final String ajaxpipeToken = getajaxpipeToken();
        if (profile_setlink) {
            profileID = new Regex(br.getURL(), "profile\\.php\\?id=(\\d+)\\&").getMatch(0);
            collection_token = new Regex(br.getURL(), "collection_token=([^<>\"]*?)\\&").getMatch(0);
            type = new Regex(br.getURL(), "type=(\\d+)").getMatch(0);
            setID = new Regex(br.getURL(), "set=([a-z0-9\\.]+)").getMatch(0);
            additionalPostData = "%2C%22tab_key%22%3A%22photos%22%2C%22id%22%3A%22" + profileID + "%22%2C%22sk%22%3A%22photos%22%2C%22collection_token%22%3A%22" + Encoding.urlEncode(collection_token) + "%22%2C%22set%22%3A%22" + setID + "%22%2C%22type%22%3A%22" + type;
        } else {
            profileID = getProfileID();
        }
        final String user = getUser(this.br);
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        if (user == null || profileID == null || ajaxpipeToken == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook_profile_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        // Use this as default as an additional fail safe
        long totalPicsNum = MAX_PICS_DEFAULT;
        if (totalPicCount != null) {
            totalPicsNum = Long.parseLong(totalPicCount.replaceAll("(\\.|,)", ""));
        }
        int lastDecryptedPicsNum = 0;
        int decryptedPicsNum = 0;
        int timesNochange = 0;
        prepBrPhotoGrab();
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            if (this.isAbort()) {
                logger.info("Decryption stopped at request " + i);
                return;
            }
            int currentMaxPicCount = 20;
            String[] links;
            if (i > 1) {
                final String currentLastFbid = getLastFBID();
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastFbid == null || additionalPostData == null) {
                    logger.info("Cannot find more links, stopping decryption...");
                    break;
                }
                String loadLink = "https://www.facebook.com/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A" + currentLastFbid + "%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + additionalPostData + "%22%2C%22overview%22%3Afalse%2C%22active_collection%22%3A69%2C%22collection_token%22%3A%22" + Encoding.urlEncode(collection_token) + "%22%2C%22cursor%22%3A0%2C%22tab_id%22%3A%22u_0_t%22%2C%22order%22%3Anull%2C%22importer_state%22%3Anull%7D&__user=" + user + "&__a=1&__dyn=7n8ahyngCBDBzpQ9UoGhk4BwzCxO4oKA8ABGfirWo8popyUWdDx24QqUgKm58y&__req=jsonp_" + i + "&__rev=" + REV_2 + "&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 32;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                if (profile_setlink) {
                    links = br.getRegex("hovercard\\.php\\?fbid=(\\d+)").getColumn(0);
                } else {
                    links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/media/set/\\?set=(a|vb)\\.[^<>\"]*?)\"").getColumn(0);
                    if (links == null || links.length == 0) {
                        links = br.getRegex("hovercard\\.php\\?fbid=(\\d+)").getColumn(0);
                    }
                }
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + parameter);
                return;
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (String entry : links) {
                entry = Encoding.htmlDecode(entry);
                final DownloadLink dl;
                if (entry.matches("\\d+")) {
                    dl = createPicDownloadlink(entry);
                } else {
                    dl = createDownloadlink(entry);
                }
                fp.add(dl);
                decryptedLinks.add(dl);
                decryptedPicsNum++;
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                logger.info("Found less pics than a full page -> Continuing anyways");
            }
            logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("-> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    /* TODO: Ad multiple pages handling for this! */
    private void v2decryptProfileAlbums() throws Exception {
        final String url_username = new Regex(parameter, "facebook\\.com/([^<>\"\\?/]+)").getMatch(0);
        final String rev = getRev(this.br);
        final String user = getUser(this.br);
        final String dyn = getDyn();
        final String ajaxpipe_token = getajaxpipeToken();
        final String profileID = getProfileID();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        if (user == null || profileID == null || ajaxpipe_token == null || profileID == null) {
            /* Errorhandling for offline cases is just hard to do - we can never be 100% sure why it fails! */
            logger.info("Decrypter broken or url offline: " + parameter);
            return;
        }
        boolean dynamicLoadAlreadyDecrypted = false;
        // Use this as default as an additional fail safe
        long totalPicsNum = MAX_PICS_DEFAULT;
        if (totalPicCount != null) {
            totalPicsNum = Long.parseLong(totalPicCount.replaceAll("(\\.|,)", ""));
        }
        int timesNochange = 0;
        // prepBrPhotoGrab();
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            if (this.isAbort()) {
                logger.info("Decryption stopped at request " + i);
                return;
            }
            long linkscount_before = 0;
            long linkscount_after = 0;
            long addedlinks = 0;
            int currentMaxPicCount = 20;
            logger.info("Decrypting page " + i + " of ??");
            linkscount_before = this.decryptedLinks.size();
            if (i > 1) {
                final String currentLastFbid = getLastFBID();
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastFbid == null) {
                    logger.warning("Decrypter maybe broken for link: " + parameter);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String urlload = null;
                final String data;
                // if (this.PARAMETER.matches(TYPE_SET_LINK_VIDEO)) {
                // data = "{\"scroll_load\":true,\"last_fbid\":" + currentLastFbid + ",\"fetch_size\":32,\"viewmode\":null,\"profile_id\":"
                // + profileID + ",\"set\":\"" + setid + "\",\"type\":2}";
                // urlload = "/ajax/pagelet/generic.php/TimelinePhotoSetPagelet?__pc=EXP1:DEFAULT&ajaxpipe=1&ajaxpipe_token=" +
                // ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn +
                // "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=" + i;
                // }
                if (urlload == null) {
                    break;
                }
                br.getPage(urlload);
                br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
                currentMaxPicCount = 40;
                dynamicLoadAlreadyDecrypted = true;
            }
            v2crawlAlbumsPhoto();
            linkscount_after = this.decryptedLinks.size();
            addedlinks = linkscount_after - linkscount_before;
            // currentMaxPicCount = max number of links per segment
            if (addedlinks < currentMaxPicCount) {
                logger.info("Found less pics than a full page -> Continuing anyways");
            }
            logger.info("Decrypted " + this.decryptedLinks.size() + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("Three times no change -> Aborting decryption");
                break;
            }
            if (this.decryptedLinks.size() >= totalPicsNum) {
                logger.info("Decrypted all pictures -> Stopping");
                break;
            }
            if (this.decryptedLinks.size() == linkscount_before) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
        }
        if (this.decryptedLinks.size() < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("-> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void v2decryptSets() throws Exception {
        final String set_type = "3";
        String profileDisplayName = getProfileDisplayName();
        String albumTitle = getPageAlbumTitle();
        String userProfile = getPageTitle();
        if ("Other Albums".equals(albumTitle)) {
            // this picked up as false positive
            albumTitle = null;
        }
        if (userProfile != null && albumTitle != null && userProfile.equals(albumTitle)) {
            albumTitle = null;
        }
        if (userProfile != null && albumTitle == null && profileDisplayName != null) {
            // cleanup
            userProfile = userProfile.replaceFirst("^" + profileDisplayName + "'s ", "");
            // Amend into 'profilename - setname/albumname'
            if (!userProfile.startsWith(profileDisplayName)) {
                userProfile = profileDisplayName + " - " + userProfile;
            }
        } else if (userProfile != null && albumTitle != null && StringUtils.startsWithCaseInsensitive(albumTitle, userProfile)) {
            // cleanup of 's of video urls, for video section... slightly different.
            albumTitle = albumTitle.replaceFirst("^" + Pattern.quote(userProfile) + "'s\\s+", "");
        }
        String fpName = (userProfile != null && albumTitle != null && !userProfile.equals(albumTitle) ? userProfile + " - " + albumTitle : (albumTitle != null ? albumTitle : (userProfile != null ? userProfile : null)));
        String url_username = new Regex(parameter, "facebook\\.com/(?!media)([^<>\"\\?/]+)").getMatch(0);
        if (url_username == null) {
            // redirects happen... and it can become available.
            url_username = new Regex(br.getURL(), "facebook\\.com/(?!media)([^<>\"\\?/]+)").getMatch(0);
        }
        final String rev = getRev(this.br);
        final String user = getUser(this.br);
        final String dyn = getDyn();
        final String ajaxpipe_token = getajaxpipeToken();
        final String profileID = getProfileID();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        final String setid = new Regex(this.parameter, "/set/\\?set=([a-z0-9\\.]+)").getMatch(0);
        final String tab = new Regex(this.parameter, "tab=([^/\\&]+)").getMatch(0);
        String activecollection = null;
        String collection_token = br.getRegex("\\[\"pagelet_timeline_app_collection_([^<>\"]*?)\"\\]").getMatch(0);
        if (collection_token != null) {
            activecollection = new Regex(collection_token, ":(\\d+)$").getMatch(0);
        }
        if (fpName == null) {
            fpName = "Facebook_photos_stream_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName);
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        // Use this as default as an additional fail safe
        long totalPicsNum = MAX_PICS_DEFAULT;
        if (totalPicCount != null) {
            totalPicsNum = Long.parseLong(totalPicCount.replaceAll("(\\.|,)", ""));
        }
        int timesNochange = 0;
        prepBrPhotoGrab();
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            if (this.isAbort()) {
                logger.info("Decryption stopped at request " + i);
                return;
            }
            long linkscount_before = 0;
            long linkscount_after = 0;
            long addedlinks = 0;
            int currentMaxPicCount = 20;
            logger.info("Decrypting page " + i + " of ??");
            linkscount_before = this.decryptedLinks.size();
            if (i > 1) {
                final String currentLastFbid = getLastFBID();
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastFbid == null && parameter.matches(TYPE_SET_LINK_VIDEO)) {
                    // not everything is a defect/broken
                    break;
                } else if (currentLastFbid == null) {
                    logger.warning("Decrypter maybe broken for link: " + parameter);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String urlload;
                final String data;
                final int fetchSize = 1000;
                if (this.parameter.matches(TYPE_SET_LINK_PHOTO)) {
                    // handling here, since we can't do it else where with so many different request types.
                    if (currentLastFbid == null || profileID == null || setid == null || set_type == null || ajaxpipe_token == null || dyn == null || rev == null || user == null) {
                        break;
                    }
                    if (collection_token != null) {
                        // handling here, since we can't do it else where with so many different request types.
                        if (activecollection == null) {
                            break;
                        }
                        data = "{\"scroll_load\":true,\"last_fbid\":" + currentLastFbid + ",\"fetch_size\":" + fetchSize + ",\"profile_id\":" + profileID + ",\"tab_key\":\"media_set\",\"set\":\"" + setid + "\",\"type\":\"" + set_type + "\",\"sk\":\"photos\",\"overview\":false,\"active_collection\":" + activecollection + ",\"collection_token\":\"" + collection_token + "\",\"cursor\":0,\"tab_id\":\"u_0_u\",\"order\":null,\"importer_state\":null}";
                    } else {
                        data = "{\"scroll_load\":true,\"last_fbid\":\"" + currentLastFbid + "\",\"fetch_size\":" + fetchSize + ",\"profile_id\":" + profileID + ",\"viewmode\":null,\"set\":\"" + setid + "\",\"type\":\"" + set_type + "\"}";
                    }
                    urlload = "/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?__pc=EXP1%3ADEFAULT&ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=" + i;
                } else if (this.parameter.matches(TYPE_SET_LINK_VIDEO)) {
                    // handling here, since we can't do it else where with so many different request types.
                    if (currentLastFbid == null || profileID == null || setid == null || ajaxpipe_token == null || dyn == null || rev == null || user == null) {
                        break;
                    }
                    data = "{\"scroll_load\":true,\"last_fbid\":" + currentLastFbid + ",\"fetch_size\":" + fetchSize + ",\"viewmode\":null,\"profile_id\":" + profileID + ",\"set\":\"" + setid + "\",\"type\":2}";
                    urlload = "/ajax/pagelet/generic.php/TimelinePhotoSetPagelet?__pc=EXP1:DEFAULT&ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=" + i;
                } else if (this.parameter.matches(TYPE_PHOTOS_STREAM_LINK)) {
                    // handling here, since we can't do it else where with so many different request types.
                    if (currentLastFbid == null || profileID == null || ajaxpipe_token == null || dyn == null || rev == null || user == null) {
                        break;
                    }
                    if (tab != null) {
                        /* E.g. https://www.facebook.com/JenniLee.Official/photos_stream?tab=photos */
                        data = "{\"scroll_load\":true,\"last_fbid\":" + currentLastFbid + ",\"fetch_size\":" + fetchSize + ",\"profile_id\":" + profileID + ",\"is_medley_view\":true,\"" + tab + "\":\"photos\",\"is_page_new_view\":true}";
                        urlload = "/ajax/pagelet/generic.php/TimelinePhotosPagelet?__pc=EXP1:DEFAULT&ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=" + i;
                    } else {
                        data = "{\"scroll_load\":true,\"last_fbid\":" + currentLastFbid + ",\"fetch_size\":" + fetchSize + ",\"profile_id\":" + profileID + ",\"tab\":\"photos_stream\",\"vanity\":\"" + url_username + "\",\"sk\":\"photos_stream\",\"tab_key\":\"photos_stream\",\"page\":" + profileID + ",\"is_medley_view\":true,\"pager_fired_on_init\":true}";
                        urlload = "/ajax/pagelet/generic.php/TimelinePhotosStreamPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=" + i;
                    }
                } else {
                    logger.warning("Unsupported case");
                    return;
                }
                br.getPage(urlload);
                // this is dangerous as its ruins unicode also! -raztoki20160127
                br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
                // as it seems to find double!
                currentMaxPicCount = fetchSize * 2;
                dynamicLoadAlreadyDecrypted = true;
            }
            if (this.parameter.matches(TYPE_SET_LINK_VIDEO)) {
                v2crawlVideos();
            } else {
                v2crawlImages();
            }
            linkscount_after = this.decryptedLinks.size();
            addedlinks = linkscount_after - linkscount_before;
            // currentMaxPicCount = max number of links per segment
            if (addedlinks < (i > 1 ? currentMaxPicCount / 2 : currentMaxPicCount)) {
                logger.info("Found less pics than a full page -> Continuing anyways");
            }
            logger.info("Decrypted " + this.decryptedLinks.size() + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("Three times no change -> Aborting decryption");
                break;
            }
            // if (this.decryptedLinks.size() >= totalPicsNum) {
            // logger.info("Decrypted all pictures -> Stopping");
            // break;
            // }
            if (this.decryptedLinks.size() == linkscount_before) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
        }
        if (this.decryptedLinks.size() < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("-> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void v2crawlImages() {
        String[] photoids = br.getRegex("ajax/photos/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
        if (photoids == null || photoids.length == 0) {
            photoids = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
        }
        for (final String picID : photoids) {
            if (!dupe.add(picID)) {
                continue;
            }
            final DownloadLink dl = createPicDownloadlink(picID);
            fp.add(dl);
            distribute(dl);
            decryptedLinks.add(dl);
        }
    }

    private void v2crawlVideos() {
        // String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)videos \\| Facebook</title>").getMatch(0);
        String[] links = br.getRegex("uiVideoLinkMedium\" href=\"https?://(?:www\\.)?facebook\\.com/(?:photo|video)\\.php\\?v=(\\d+)").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("ajaxify=\"(?:[^\"]+/videos/vb\\.\\d+/|[^\"]+/video\\.php\\?v=)(\\d+)").getColumn(0);
        }
        for (final String videoid : links) {
            if (!dupe.add(videoid)) {
                continue;
            }
            final String videolink = "https://facebookdecrypted.com/video.php?v=" + videoid;
            final DownloadLink dl = createDownloadlink(videolink);
            dl.setContentUrl(videoid);
            dl.setLinkID(videoid);
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            dl.setName(videoid);
            dl.setAvailable(true);
            fp.add(dl);
            decryptedLinks.add(dl);
        }
    }

    private void v2crawlAlbumsPhoto() {
        // String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)videos \\| Facebook</title>").getMatch(0);
        final String[] links = br.getRegex("href=\"(https?://(?:www\\.)?facebook\\.com/media/set/\\?set=a\\.[^<>\"]*?)\"").getColumn(0);
        for (String url_album : links) {
            if (!dupe.add(url_album)) {
                continue;
            }
            url_album = Encoding.htmlDecode(url_album);
            final DownloadLink dl = createDownloadlink(url_album);
            if (fp != null) {
                fp.add(dl);
            }
            decryptedLinks.add(dl);
        }
    }

    private FilePackage fp = null;

    private void decryptGroupsPhotos() throws Exception {
        String fpName = getPageTitle();
        final String rev = getRev(this.br);
        final String user = getUser(this.br);
        final String dyn = "7AzHK5kcxx2u6V45E9odoyEhy8jXWo466EeVEoyUnwgU6C7RyUcWwAyUG4UeUuwh9UcU88lwIxWcwJwpV9UK";
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        final String ajaxpipe_token = getajaxpipeToken();
        // this should be 'GroupPhotosetPagelet'
        String controller = updateControllerAndData(br);
        if (user == null || ajaxpipe_token == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook__groups_photos_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        // Use this as default as an additional fail safe
        long totalPicsNum = MAX_PICS_DEFAULT;
        if (totalPicCount != null) {
            totalPicsNum = Long.parseLong(totalPicCount.replaceAll("(\\.|,)", ""));
        }
        int lastDecryptedPicsNum = 0;
        int decryptedPicsNum = 0;
        int timesNochange = 0;
        prepBrPhotoGrab();
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            if (this.isAbort()) {
                logger.info("Decryption stopped at request " + i);
                return;
            }
            int currentMaxPicCount = 20;
            String[] links;
            if (i > 1) {
                if (br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
                    logger.info("Server says the set is fully loaded -> Stopping");
                    break;
                }
                final String loadLink = "/ajax/pagelet/generic.php/" + controller + "?dpr=1&ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__be=0&__pc=EXP1%3ADEFAULT&__rev=" + rev + "__adt=" + i;
                getPage(loadLink);
                links = br.getRegex("\"(https?://(www\\.)?facebook\\.com/(photo\\.php\\?(fbid|v)=|media/set/\\?set=oa\\.)\\d+)").getColumn(0);
                currentMaxPicCount = 40;
            } else {
                links = br.getRegex("\"(https?://(www\\.)?facebook\\.com/(photo\\.php\\?(fbid|v)=|media/set/\\?set=oa\\.)\\d+)").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + parameter);
                return;
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String single_link : links) {
                final String current_id = new Regex(single_link, "(\\d+)$").getMatch(0);
                if (!dupe.add(current_id)) {
                    continue;
                }
                final DownloadLink dl = createDownloadlink(single_link.replace("facebook.com/", CRYPTLINK));
                dl.setContentUrl(single_link);
                if (!logged_in) {
                    dl.setProperty("nologin", true);
                }
                if (fastLinkcheckPictures) {
                    dl.setAvailable(true);
                }
                dl.setContentUrl(single_link);
                // Set temp name, correct name will be set in hosterplugin later
                dl.setName(current_id);
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                fp.add(dl);
                distribute(dl);
                decryptedLinks.add(dl);
                decryptedPicsNum++;
            }
            if (i >= 2) {
                // we need to update last last_fbid
                controller = updateControllerAndData(br);
                if (controller == null || data == null) {
                    logger.info("Couldn't find 'updateController' response, task completed!");
                    stop = true;
                }
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                logger.info("Found less pics than a full page -> Continuing anyways");
            }
            logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("-> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    /** Crawls all images of a Facebook private conversation */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void v2decryptMessagePhotos() throws Exception {
        final String url_username = new Regex(this.parameter, "/messages/(.+)").getMatch(0);
        if (!this.logged_in) {
            logger.info("Login required to decrypt photos of private messages");
            return;
        }
        this.getpagefirsttime(this.parameter);
        final String thread_fbid = this.br.getRegex("" + url_username + "\",\"id\":\"fbid:(\\d+)\"").getMatch(0);
        final String user = getUser(this.br);
        final String token = get_fb_dtsg(this.br);
        if (thread_fbid == null || user == null || token == null) {
            /* Probably offline url */
            return;
        }
        final String postdata = "thread_id=" + thread_fbid + "&offset=0&limit=100000&__user=" + user + "&__a=1&__dyn=" + getDyn() + "&__req=c&fb_dtsg=" + token + "&ttstamp=" + get_ttstamp() + "&__rev=" + getRev(this.br);
        /* First find all image-IDs */
        this.br.postPage("/ajax/messaging/attachments/sharedphotos.php?__pc=EXP1%3ADEFAULT", postdata);
        /* Secondly access each image individually to find its' final URL and download it */
        String json = this.br.getRegex("for \\(;;\\);(\\{.+)").getMatch(0);
        if (json == null) {
            this.decryptedLinks = null;
            return;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final Object imagesData = JavaScriptEngineFactory.walkJson(entries, "payload/imagesData");
        final ArrayList<Object> ressourcelist = (ArrayList) imagesData;
        for (final Object pic_o : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) pic_o;
            final String image_id = (String) entries.get("fbid");
            final String directlink = (String) entries.get("");
            final DownloadLink dl = createPicDownloadlink(image_id);
            dl.setProperty("is_private", true);
            dl.setProperty("thread_fbid", thread_fbid);
            if (fp != null) {
                fp.add(dl);
            }
            this.decryptedLinks.add(dl);
        }
        // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/imagesData");
        // final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
        // while (it.hasNext()) {
        // final Entry<String, Object> entry = it.next();
        // final String image_id = entry.getKey();
        // final DownloadLink dl = createPicDownloadlink(image_id);
        // dl.setProperty("is_private", true);
        // dl.setProperty("thread_fbid", thread_fbid);
        // this.decryptedLinks.add(dl);
        // }
    }

    private void decryptNotes() throws Exception {
        // does not fetch comments, only the body!
        final String html = br.getRegex("<div class=\"_4-u3 _5cla\">.*?(?:</div>){3}").getMatch(-1);
        if (html == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        // set packagename
        final String fpName = new Regex(html, "<h2 class=\"_5clb\">(.*?)</h2>").getMatch(0);
        if (StringUtils.isNotEmpty(fpName)) {
            if (fp == null) {
                fp = FilePackage.getDefaultFilePackage();
            }
            fp.setName(fpName);
        }
        // parser can be quite slow... I assume due to cleanups
        final String[] urls = HTMLParser.getHttpLinks(html, null);
        // prevent loops!
        dupe.add(parameter);
        dupe.add(br.getURL());
        for (final String url : urls) {
            if (!dupe.add(url)) {
                continue;
            }
            // anything with l.facebook.com/
            if (url.matches(TYPE_FB_REDIRECT_TO_EXTERN_SITE)) {
                final String link = getRedirectToExternalSite(url);
                if (!dupe.add(link)) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(link));
            } else {
                this.decryptedLinks.add(this.createDownloadlink(url));
            }
        }
    }

    private String getRedirectToExternalSite(final String url) throws DecrypterException {
        String external_url = new Regex(url, "/l\\.php\\?u=([^&]+)").getMatch(0);
        if (StringUtils.isNotEmpty(external_url)) {
            external_url = Encoding.urlDecode(external_url, false);
            return external_url;
        }
        external_url = new Regex(url, "facebook\\.com/l/[^/]+/(.+)").getMatch(0);
        if (StringUtils.isNotEmpty(external_url)) {
            return "http://" + external_url;
        }
        throw new DecrypterException(EXCEPTION_PLUGINDEFECT);
    }

    private String data = null;

    // TODO: Use this everywhere as it should work universal
    private void decryptPicsGeneral(String controller) throws Exception {
        String fpName = getPageTitle();
        final String rev = getRev(this.br);
        final String user = getUser(this.br);
        final String dyn = getDyn();
        final String appcollection = br.getRegex("\"pagelet_timeline_app_collection_(\\d+:\\d+)(:\\d+)?\"").getMatch(0);
        final String profileID = getProfileID();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        final String ajaxpipe = getajaxpipeToken();
        data = null;
        if (controller == null) {
            controller = br.getRegex("\"photos\",\\[\\{\"controller\":\"([^<>\"]*?)\"").getMatch(0);
            if (controller == null) {
                controller = updateControllerAndData(br);
            }
        }
        if (user == null || profileID == null || (appcollection == null && data == null)) {
            logger.info("Debug info: user: " + user + ", profileID: " + profileID + ", appcollection: " + appcollection + ", data: " + data);
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        if (fpName == null) {
            fpName = "Facebook_photos_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        // Use this as default as an additional fail safe
        long totalPicsNum = MAX_PICS_DEFAULT;
        if (totalPicCount != null) {
            totalPicsNum = Long.parseLong(totalPicCount.replaceAll("(\\.|,)", ""));
        }
        int lastDecryptedPicsNum = 0;
        int decryptedPicsNum = 0;
        int timesNochange = 0;
        prepBrPhotoGrab();
        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            final Browser br = this.br.cloneBrowser();
            if (this.isAbort()) {
                logger.info("Decryption stopped at request " + i);
                return;
            }
            String[] links;
            if (i > 1) {
                if (br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
                    logger.info("Server says the set is fully loaded -> Stopping");
                    break;
                }
                final String loadLink = "/ajax/pagelet/generic.php/" + controller + "?dpr=1&ajaxpipe=1&ajaxpipe_token=" + Encoding.urlEncode(ajaxpipe) + "&no_script_path=1&data=" + Encoding.urlEncode(data) + "&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__pc=EXP1%3ADEFAULT&__rev=" + REV_3 + "&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
            } else {
                links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + parameter);
                return;
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String picID : links) {
                if (dupe.add(picID)) {
                    final DownloadLink dl = createPicDownloadlink(picID);
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                    decryptedPicsNum++;
                }
            }
            if (i >= 2) {
                // we need to update last last_fbid
                controller = updateControllerAndData(br);
                if (controller == null || data == null) {
                    logger.info("Couldn't find 'updateController' response, task completed!");
                    stop = true;
                }
            }
            logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        logger.info("Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("-> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void decryptSinglePic() throws Exception {
        String flink = br.getRegex("data-ploi=\"(.*?)\"").getMatch(0);
        if (flink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        logger.info("Debug info: Found: " + flink);
        flink = Encoding.htmlDecode(flink);
        String filename = new Regex(parameter, "facebook.com/(.*?)/\\?type=3").getMatch(0);
        // final DownloadLink dl = createPicDownloadlink(link);
        final DownloadLink dl = createDownloadlink(flink);
        dl.setContentUrl(flink);
        dl.setName(filename);
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
        if (fastLinkcheckPictures) {
            dl.setAvailable(true);
        }
        decryptedLinks.add(dl);
    }

    private String updateControllerAndData(final Browser br) throws DecrypterException {
        String d = "return new ScrollingPager\\(\"[^\"]+\"\\s*,\\s*\"(.*?)\"\\s*,\\s*(\\{.*?\\})";
        String controller = br.getRegex(d).getMatch(0);
        data = br.getRegex(d).getMatch(1);
        if (controller == null && data == null) {
            // json
            String q = Character.toString((char) 92) + Character.toString((char) 92) + "\"";
            Pattern e = Pattern.compile("return new ScrollingPager\\(" + q + "[^\"]+" + q + "\\s*,\\s*" + q + "(.*?)" + q + "\\s*,\\s*(\\{.*?\\})");
            controller = br.getRegex(e).getMatch(0);
            data = br.getRegex(e).getMatch(1);
            if (data != null) {
                data = data.replace("\\\"", "\"");
            }
        }
        return controller;
    }

    private void getpagefirsttime(final String parameter) throws IOException {
        // Redirects from "http" to "https" can happen
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String scriptRedirect = br.getRegex("<script>window\\.location\\.replace\\(\"(https?:[^<>\"]*?)\"\\);</script>").getMatch(0);
        if (scriptRedirect != null) {
            scriptRedirect = PluginJSonUtils.unescape(scriptRedirect);
            br.getPage(scriptRedirect);
        }
        String normal_redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"0; URL=(/[^<>\"]*?)\"").getMatch(0);
        /* Do not access the noscript versions of links - this will cause out of date errors! */
        if (normal_redirect != null && !normal_redirect.contains("fb_noscript=1")) {
            normal_redirect = Encoding.htmlDecode(normal_redirect);
            normal_redirect = normal_redirect.replace("u00253A", "%3A");
            br.getPage("https://www.facebook.com" + normal_redirect);
        }
        if (br.getURL().contains("https://")) {
            MAINPAGE = "https://www.facebook.com";
        }
    }

    private DownloadLink createPicDownloadlink(final String picID) {
        final String final_link = "https://www." + CRYPTLINK + "photo.php?fbid=" + picID;
        final String real_link = "https://www.facebook.com/photo.php?fbid=" + picID;
        final DownloadLink dl = createDownloadlink(final_link);
        dl.setContentUrl(real_link);
        if (!logged_in) {
            dl.setProperty("nologin", true);
        }
        if (fastLinkcheckPictures) {
            dl.setAvailable(true);
        }
        // Set temp name, correct name will be set in hosterplugin later
        dl.setName(picID);
        dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
        return dl;
    }

    private String getProfileID() {
        String profileid = br.getRegex("data-gt=\"\\&#123;\\&quot;profile_owner\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        if (profileid == null) {
            profileid = br.getRegex("PageHeaderPageletController_(\\d+)\"").getMatch(0);
            if (profileid == null) {
                profileid = br.getRegex("data-profileid=\"(\\d+)\"").getMatch(0);
                if (profileid == null) {
                    profileid = br.getRegex("\\\\\"profile_id\\\\\":(\\d+)").getMatch(0);
                }
            }
        }
        return profileid;
    }

    private String getajaxpipeToken() {
        final String ajaxpipe = br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
        return ajaxpipe;
    }

    public static String getRev(final Browser br) {
        String rev = br.getRegex("\"revision\":(\\d+)").getMatch(0);
        if (rev == null) {
            rev = REV_2;
        }
        return rev;
    }

    private String getPageTitle() {
        String pageTitle = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        if (pageTitle != null) {
            pageTitle = HTMLEntities.unhtmlentities(pageTitle);
            pageTitle = pageTitle.trim().replaceFirst("\\s*\\|\\s*Facebook$", "");
        }
        return pageTitle;
    }

    private String getPageAlbumTitle() {
        String albumTitle = br.getRegex("<h1 class=\"fbPhotoAlbumTitle\">([^<>]*?)<").getMatch(0);
        if (albumTitle != null) {
            albumTitle = HTMLEntities.unhtmlentities(albumTitle);
            albumTitle = albumTitle.trim();
        }
        return albumTitle;
    }

    private String getProfileDisplayName() {
        String profileDisplayName = (!logged_in) ? br.getRegex(">By <a href=\"https?://www\\.facebook\\.com/[^/]+/\">(.*?)</").getMatch(0) : br.getRegex("<a class=\"nameButton uiButton uiButtonOverlay\".*?<span class=\"uiButtonText\">(.*?)</span></a>").getMatch(0);
        if (profileDisplayName != null) {
            profileDisplayName = HTMLEntities.unhtmlentities(profileDisplayName);
            profileDisplayName = profileDisplayName.trim();
        }
        return profileDisplayName;
    }

    public static String getDyn() {
        return "7xeXxmdwgp8fqwOyax68xfLFwgoqwgEoyUnwgU6C7QdwPwDyUG4UeUuwh8eUny8lwIwHwJwr9U";
    }

    public static String get_fb_dtsg(final Browser br) {
        final String fb_dtsg = br.getRegex("name=\\\\\"fb_dtsg\\\\\" value=\\\\\"([^<>\"]*?)\\\\\"").getMatch(0);
        return fb_dtsg;
    }

    private String getLastFBID() {
        String currentLastFbid = br.getRegex("\"last_fbid\\\\\":\\\\\"(\\d+)\\\\\\\"").getMatch(0);
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\\\\\":(\\d+)").getMatch(0);
        }
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\":(\\d+)").getMatch(0);
        }
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\":\"(\\d+)\"").getMatch(0);
        }
        return currentLastFbid;
    }

    private void prepBrPhotoGrab() {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private void getPage(final String link) throws IOException {
        br.getPage(link);
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
    }

    public static String getUser(final Browser br) {
        String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
        if (user == null) {
            user = br.getRegex("detect_broken_proxy_cache\\(\"(\\d+)\", \"c_user\"\\)").getMatch(0);
        }
        // regex verified: 10.2.2014
        if (user == null) {
            user = br.getRegex("\\[(\\d+)\\,\"c_user\"").getMatch(0);
        }
        return user;
    }

    public static final String get_ttstamp() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean login() throws Exception {
        /** Login stuff begin */
        final PluginForHost facebookPlugin = JDUtilities.getPluginForHost("facebook.com");
        Account aa = AccountController.getInstance().getValidAccount(facebookPlugin);
        boolean addAcc = false;
        if (aa == null) {
            SubConfiguration config = null;
            try {
                config = this.getPluginConfig();
                if (config.getBooleanProperty("infoShown", Boolean.FALSE) == false) {
                    if (config.getProperty("infoShown2") == null) {
                        showFreeDialog();
                    } else {
                        config = null;
                    }
                } else {
                    config = null;
                }
            } catch (final Throwable e) {
            } finally {
                if (config != null) {
                    config.setProperty("infoShown", Boolean.TRUE);
                    config.setProperty("infoShown2", "shown");
                    config.save();
                }
            }
            // User wants to use the account
            if (this.DIALOGRETURN == 0) {
                String username = UserIO.getInstance().requestInputDialog("Enter Loginname for facebook.com :");
                if (username == null) {
                    return false;
                }
                String password = UserIO.getInstance().requestInputDialog("Enter password for facebook.com :");
                if (password == null) {
                    return false;
                }
                aa = new Account(username, password);
                addAcc = true;
            }
        }
        if (aa != null) {
            try {
                ((jd.plugins.hoster.FaceBookComVideos) facebookPlugin).login(aa, this.br);
                // New account is valid, let's add it to the premium overview
                if (addAcc) {
                    AccountController.getInstance().addAccount(facebookPlugin, aa);
                }
                return true;
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid, returnung empty linklist!");
                return false;
            }
        }
        return false;
        /** Login stuff end */
    }

    /**
     * Code below = prevents Eclipse from freezing as it removes the log output of this thread!
     **/
    private void disableLogger() {
        final LogInterface logger = new LogInterface() {
            @Override
            public void warning(String msg) {
            }

            @Override
            public void severe(String msg) {
            }

            @Override
            public void log(Throwable e) {
            }

            @Override
            public void info(String msg) {
            }

            @Override
            public void finest(String msg) {
            }

            @Override
            public void finer(String msg) {
            }

            @Override
            public void fine(String msg) {
            }
        };
        this.setLogger(logger);
        ((LinkCrawlerThread) Thread.currentThread()).setLogger(logger);
    }

    private void showFreeDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        if ("de".equalsIgnoreCase(lng)) {
                            title = "Facebook.com Gallerie/Photo Download";
                            message = "Du versucht gerade, eine Facebook Gallerie/Photo zu laden.\r\n";
                            message += "Für die meisten dieser Links wird ein gültiger Facebook Account benötigt!\r\n";
                            message += "Deinen Account kannst du in den Einstellungen als Premiumaccount hinzufügen.\r\n";
                            message += "Solltest du dies nicht tun, kann JDownloader nur Facebook Links laden, die keinen Account benötigen!\r\n";
                            message += "Willst du deinen Facebook Account jetzt hinzufügen?\r\n";
                        } else {
                            title = "Facebook.com gallery/photo download";
                            message = "You're trying to download a Facebook gallery/photo.\r\n";
                            message += "For most of these links, a valid Facebook account is needed!\r\n";
                            message += "You can add your account as a premium account in the settings.\r\n";
                            message += "Note that if you don't do that, JDownloader will only be able to download Facebook links which do not need a login.\r\n";
                            message += "Do you want to enter your Facebook account now?\r\n";
                        }
                        DIALOGRETURN = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}