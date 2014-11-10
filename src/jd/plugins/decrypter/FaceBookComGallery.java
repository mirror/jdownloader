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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "https?://(www\\.)?(on\\.fb\\.me/[A-Za-z0-9]+\\+?|facebook\\.com/.+)" }, flags = { 0 })
public class FaceBookComGallery extends PluginForDecrypt {

    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static Object         LOCK                           = new Object();
    private static final String   FACEBOOKMAINPAGE               = "http://www.facebook.com";
    private int                   DIALOGRETURN                   = -1;
    private static final String   FASTLINKCHECK_PICTURES         = "FASTLINKCHECK_PICTURES";

    private static final String   TYPE_FBSHORTLINK               = "http(s)?://(www\\.)?on\\.fb\\.me/[A-Za-z0-9]+\\+?";
    private static final String   TYPE_SINGLE_PHOTO              = "http(s)?://(www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+";
    private static final String   TYPE_SINGLE_VIDEO_ALL          = "https?://(www\\.)?facebook\\.com/(video/video|photo|video)\\.php\\?v=\\d+";
    private static final String   TYPE_SINGLE_VIDEO_EMBED        = "https?://(www\\.)?facebook\\.com/video/embed\\?video_id=\\d+";
    private static final String   TYPE_SET_LINK_PHOTO            = "http(s)?://(www\\.)?facebook\\.com/(media/set/\\?set=|[^<>\"/]*?/media_set\\?set=)o?a[0-9\\.]+(\\&type=\\d+)?";
    private static final String   TYPE_SET_LINK_VIDEO            = "https?://(www\\.)?facebook\\.com/media/set/\\?set=vb\\.\\d+";
    private static final String   TYPE_ALBUMS_LINK               = "https?://(www\\.)?facebook\\.com/.+photos_albums";
    private static final String   TYPE_PHOTOS_OF_LINK            = "https?://(www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_of";
    private static final String   TYPE_PHOTOS_ALL_LINK           = "https?://(www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_all";
    private static final String   TYPE_PHOTOS_STREAM_LINK        = "https?://(www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_stream";
    private static final String   TYPE_PHOTOS_LINK               = "https?://(www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos";
    private static final String   TYPE_GROUPS_PHOTOS             = "https?://(www\\.)?facebook\\.com/groups/\\d+/photos/";
    private static final String   TYPE_GROUPS_FILES              = "https?://(www\\.)?facebook\\.com/groups/\\d+/files/";
    private static final String   TYPE_PROFILE_PHOTOS            = "https?://(www\\.)?facebook\\.com/profile\\.php\\?id=\\d+\\&sk=photos\\&collection_token=[A-Z0-9%]+";

    private static final int      MAX_LOOPS_GENERAL              = 150;
    private static final int      MAX_PICS_DEFAULT               = 5000;

    private static String         MAINPAGE                       = "http://www.facebook.com";
    private static final String   CRYPTLINK                      = "facebookdecrypted.com/";
    private static final String   EXCEPTION_LINKOFFLINE          = "EXCEPTION_LINKOFFLINE";

    private static final String   CONTENTUNAVAILABLE             = ">Dieser Inhalt ist derzeit nicht verfügbar|>This content is currently unavailable<";
    private String                PARAMETER                      = null;
    private boolean               FASTLINKCHECK_PICTURES_ENABLED = false;
    private boolean               logged_in                      = false;
    final ArrayList<DownloadLink> decryptedLinks                 = new ArrayList<DownloadLink>();

    /*
     * Dear whoever is looking at this - this is a classic example of spaghetticode. If you like spaghettis, go ahead, and get you some
     * tomatoe sauce and eat it but if not, well have fun re-writing this from scratch ;) Wikipedia:
     * http://en.wikipedia.org/wiki/Spaghetti_code
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString().replace("#!/", "");
        PARAMETER = parameter;
        if (PARAMETER.matches(TYPE_SINGLE_VIDEO_ALL) || PARAMETER.matches(TYPE_SINGLE_VIDEO_EMBED) || PARAMETER.contains("/video.php?v")) {
            final String id = new Regex(PARAMETER, "(\\d+)$").getMatch(0);
            final DownloadLink fina = createDownloadlink("https://www.facebookdecrypted.com/video.php?v=" + id);
            decryptedLinks.add(fina);
            return decryptedLinks;
        } else if (PARAMETER.matches(TYPE_SINGLE_PHOTO)) {
            final String id = new Regex(PARAMETER, "(\\d+)$").getMatch(0);
            final DownloadLink fina = createDownloadlink("https://www.facebookdecrypted.com/photo.php?fbid=" + id);
            decryptedLinks.add(fina);
            return decryptedLinks;
        }
        br.getHeaders().put("User-Agent", jd.plugins.hoster.FaceBookComVideos.Agent);
        br.setFollowRedirects(false);
        FASTLINKCHECK_PICTURES_ENABLED = SubConfiguration.getConfig("facebook.com").getBooleanProperty(FASTLINKCHECK_PICTURES, false);
        try {
            if (parameter.matches(TYPE_FBSHORTLINK)) {
                br.getPage(parameter);
                final String finallink = br.getRedirectLocation();
                if (br.containsHTML(">Something\\'s wrong here")) {
                    logger.info("Link offline: " + parameter);
                    final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                    offline.setFinalFileName(new Regex(parameter, "facebook\\.com/(.+)").getMatch(0));
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
                return decryptedLinks;
            }
            synchronized (LOCK) {
                logged_in = login();
            }
            getpagefirsttime(PARAMETER);

            /* temporarily unavailable (or forever, or permission/rights needed) || empty album */
            if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</") || br.containsHTML("class=\"fbStarGridBlankContent\"")) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }

            if (PARAMETER.matches(TYPE_ALBUMS_LINK)) {
                decryptAlbums();
            } else if (PARAMETER.matches(TYPE_PHOTOS_OF_LINK)) {
                decryptPhotosOf();
            } else if (PARAMETER.matches(TYPE_PHOTOS_ALL_LINK)) {
                decryptPhotosAll();
            } else if (PARAMETER.matches(TYPE_PHOTOS_STREAM_LINK)) {
                decryptPhotoStreamTimeline();
            } else if (PARAMETER.matches(TYPE_PHOTOS_LINK)) {
                // Old handling removed 05.12.13 in rev 23262
                decryptPicsGeneral(null);
            } else if (br.getURL().contains("profile.php?id=")) {
                decryptPicsProfile();
            } else if (PARAMETER.matches(TYPE_SET_LINK_PHOTO)) {
                decryptSetLinkPhoto();
            } else if (PARAMETER.matches(TYPE_SET_LINK_VIDEO)) {
                decryptVideoSet();
            } else if (parameter.matches(TYPE_GROUPS_PHOTOS)) {
                decryptGroupsPhotos();
            } else {
                // Should never happen
                logger.info("Unsupported linktype: " + PARAMETER);
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
        } catch (final DecrypterException e) {
            try {
                if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                    final DownloadLink offline = createDownloadlink("directhttp://" + PARAMETER);
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    offline.setName(new Regex(PARAMETER, "facebook\\.com/(.+)").getMatch(0));
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
            } catch (final Exception x) {
            }
            throw e;
        }
        if (decryptedLinks == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return null;
        } else if (decryptedLinks.size() == 0) {
            logger.info("Link offline: " + PARAMETER);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptAlbums() throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)\\- Photos \\| Facebook</title>").getMatch(0);
        final String profileID = getProfileID();
        final String user = getUser();
        if (user == null || profileID == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        if (fpName == null) {
            fpName = "Facebook_video_albums_of_user_" + new Regex(PARAMETER, "facebook\\.com/(.*?)/photos_albums").getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        boolean dynamicLoadAlreadyDecrypted = false;

        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            int currentMaxPicCount = 18;

            String[] links;
            if (i > 1) {
                String currentLastAlbumid = br.getRegex("\"last_album_id\":\"(\\d+)\"").getMatch(0);
                if (currentLastAlbumid == null) {
                    currentLastAlbumid = br.getRegex("\"last_album_id\":(\\d+)").getMatch(0);
                }
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastAlbumid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastAlbumid == null) {
                    logger.warning("Decrypter maybe broken for link: " + PARAMETER);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TimelinePhotoAlbumsPagelet?data=%7B%22profile_id%22%3A" + profileID + "%2C%22tab_key%22%3A%22photos_albums%22%2C%22sk%22%3A%22photos_albums%22%2C%22page_index%22%3A" + i + "%2C%22last_album_id%22%3A%22" + currentLastAlbumid + "%22%7D&__user=" + user + "&__a=1&__dyn=&__req=a";
                br.getPage(loadLink);
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/media/set/\\?set=a\\.[0-9\\.]+)\\&amp;type=\\d+\"").getColumn(0);
                currentMaxPicCount = 12;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/[^<>\"]*?)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for the following link or account needed: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                decryptedLinks.add(createDownloadlink(link));
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

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

    }

    private void decryptPhotosAll() throws Exception {
        decryptPicsGeneral("AllPhotosAppCollectionPagelet");
    }

    private void decryptPhotosOf() throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
        if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</")) {
            logger.info("The link is either offline or an account is needed to grab it: " + PARAMETER);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String profileID = getProfileID();
        String fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        final String user = getUser();
        final String token = br.getRegex("\"tab_count\":\\d+,\"token\":\"([^<>\"]*?)\"").getMatch(0);
        if (token == null || user == null || profileID == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        if (fpName == null) {
            fpName = "Facebook_photos_of_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
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
                    logger.warning("Decrypter maybe broken for link: " + PARAMETER);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TaggedPhotosAppCollectionPagelet?data=%7B%22collection_token%22%3A%22" + token + "%22%2C%22cursor%22%3A%22" + cursor + "%22%2C%22tab_key%22%3A%22photos_of%22%2C%22profile_id%22%3A" + profileID + "%2C%22overview%22%3Afalse%2C%22ftid%22%3Anull%2C%22order%22%3Anull%2C%22sk%22%3A%22photos%22%7D&__user=" + user + "&__a=1&__dyn=7n8ahyj2qmp5xl2u5F92Ke82e8w&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 40;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for the following link or account needed: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String picID : links) {
                // Another fail safe to prevent loops
                if (picID.equals(lastfirstID)) {
                    stop = true;
                    break;
                }
                final DownloadLink dl = createPicDownloadlink(picID);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not supported in old 0.9.581 Stable
                }
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
        fp.addLinks(decryptedLinks);

    }

    private void decryptPicsProfile() throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
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
        final String user = getUser();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        if (user == null || profileID == null || ajaxpipeToken == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook_profile_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
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
            try {
                if (this.isAbort()) {
                    logger.info("Decryption stopped at request " + i);
                    return;
                }
            } catch (final Throwable e) {
                // Not supported in old 0.9.581 Stable
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
                String loadLink = "https://www.facebook.com/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A" + currentLastFbid + "%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + additionalPostData + "%22%2C%22overview%22%3Afalse%2C%22active_collection%22%3A69%2C%22collection_token%22%3A%22" + Encoding.urlEncode(collection_token) + "%22%2C%22cursor%22%3A0%2C%22tab_id%22%3A%22u_0_t%22%2C%22order%22%3Anull%2C%22importer_state%22%3Anull%7D&__user=" + user + "&__a=1&__dyn=7n8ahyngCBDBzpQ9UoGhk4BwzCxO4oKA8ABGfirWo8popyUWdDx24QqUgKm58y&__req=jsonp_" + i + "&__rev=141954&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 32;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                if (profile_setlink) {
                    links = br.getRegex("hovercard\\.php\\?fbid=(\\d+)").getColumn(0);
                } else {
                    links = br.getRegex("class=\"photoTextTitle\" href=\"(https?://(www\\.)?facebook\\.com/media/set/\\?set=(a|vb)\\.[^<>\"]*?)\"").getColumn(0);
                }
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + PARAMETER);
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
                logger.info("facebook.com: Found less pics than a full page -> Continuing anyways");
            }
            logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("facebook.com: Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("facebook.com: Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("facebook.com: Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        fp.addLinks(decryptedLinks);
        logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("facebook.com: -> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void decryptSetLinkPhoto() throws Exception {
        String type;
        String collection_token;
        String profileID;
        String setID;
        profileID = br.getRegex("\"profile_id\":(\\d+)").getMatch(0);
        if (profileID == null) {
            profileID = br.getRegex("follow_profile\\.php\\?profile_id=(\\d+)").getMatch(0);
        }
        collection_token = br.getRegex("\\[\"pagelet_timeline_app_collection_([^<>\"]*?)\"\\]").getMatch(0);
        type = "3";
        setID = new Regex(PARAMETER, "set=([a-z0-9\\.]+)").getMatch(0);
        String fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)\\| Facebook</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = br.getRegex("class=\"fbPhotoAlbumTitle\">([^<>\"]*?)</h1>").getMatch(0);
        }
        final String ajaxpipeToken = getajaxpipeToken();
        final String user = getUser();
        String lastfirstID = "";
        if (ajaxpipeToken == null || user == null || type == null || setID == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        String data = null;
        if (collection_token != null) {
            data = "{\"scroll_load\":true,\"last_fbid\":JDL_LAST_FBID_JDL,\"fetch_size\":32,\"profile_id\":" + profileID + "\",\"overview\":false,\"active_collection\":69,\"collection_token\":\"" + Encoding.urlEncode(collection_token) + "\",\"cursor\":0,\"tab_id\":\"u_0_t\",\"order\":null,\"importer_state\":null}";
        } else {
            data = "{\"scroll_load\":true,\"last_fbid\":\"JDL_LAST_FBID_JDL\",\"fetch_size\":32,\"profile_id\":" + profileID + ",\"viewmode\":null,\"set\":\"" + setID + "\",\"type\":\"3\"}";
        }
        data = Encoding.urlEncode(data);
        if (fpName == null) {
            fpName = "Facebook_album_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim()) + " - " + setID;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());

        for (int i = 1; i <= MAX_LOOPS_GENERAL; i++) {
            int currentMaxPicCount = 28;
            String[] links;
            if (i > 1) {
                final String currentLastFbid = getLastFBID();
                if (currentLastFbid == null) {
                    logger.info("Cannot find more links, stopping decryption...");
                    break;
                }
                String loadLink = "https://www.facebook.com/ajax/pagelet/generic.php/TimelinePhotosAlbumPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipeToken + "&no_script_path=1&data=" + data.replace("JDL_LAST_FBID_JDL", currentLastFbid) + "&__user=" + user + "&__a=1&__dyn=7n8ahyngCBDBzpQ9UoGhk4BwzCxO4oKA8ABGfirWo8popyUWdDx24QqUgKm58y&__req=jsonp_" + i + "&__rev=1414761&__adt=" + i;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 32;
            } else {
                links = br.getRegex("hovercard\\.php\\?fbid=(\\d+)").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for the following link or account needed: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String picID : links) {
                // Another fail safe to prevent loops
                if (picID.equals(lastfirstID)) {
                    stop = true;
                }
                final DownloadLink dl = createPicDownloadlink(picID);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    // Not supported in old 0.9.581 Stable
                }
                decryptedLinks.add(dl);
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount || profileID == null) {
                stop = true;
            }
            if (stop) {
                logger.info("Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        fp.addLinks(decryptedLinks);
    }

    private void decryptPhotoStreamTimeline() throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
        if (br.containsHTML(">Dieser Inhalt ist derzeit nicht verfügbar</")) {
            logger.info("The link is either offline or an account is needed to grab it: " + PARAMETER);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        String fpName = getPageTitle();
        final String rev = getRev();
        final String user = getUser();
        final String dyn = getDyn();
        final String ajaxpipe_token = getajaxpipeToken();
        final String profileID = getProfileID();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        if (user == null || profileID == null || ajaxpipe_token == null || profileID == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook_photos_stream_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        final ArrayList<String> allids = new ArrayList<String>();
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
            try {
                if (this.isAbort()) {
                    logger.info("Decryption stopped at request " + i);
                    return;
                }
            } catch (final Throwable e) {
                // Not supported in old 0.9.581 Stable
            }

            int currentMaxPicCount = 20;

            logger.info("Decrypting page " + i + " of ??");

            String[] links;
            if (i > 1) {
                final String currentLastFbid = getLastFBID();
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastFbid == null) {
                    logger.warning("Decrypter maybe broken for link: " + PARAMETER);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/TimelinePhotosStreamPagelet?ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A" + currentLastFbid + "%2C%22fetch_size%22%3A32%2C%22profile_id%22%3A" + profileID + "%2C%22tab_key%22%3A%22photos_stream%22%2C%22ajaxpipe%22%3A%221%22%2C%22ajaxpipe_token%22%3A%22" + ajaxpipe_token + "%22%2C%22quickling%22%3A%7B%22version%22%3A%221162685%3B0%3B1%3B0%3B%22%7D%2C%22__user%22%3A%22" + user + "%22%2C%22__a%22%3A%221%22%2C%22__dyn%22%3A%22" + dyn + "%22%2C%22__req%22%3A%22jsonp_2%22%2C%22__rev%22%3A%22" + rev + "%22%2C%22__adt%22%3A%222%22%2C%22sk%22%3A%22photos_stream%22%2C%22pager_fired_on_init%22%3Atrue%7D&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=jsonp_" + i + "&__rev=" + rev + "&__adt=4";
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 40;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + PARAMETER);
                return;
            }
            boolean stop = false;
            for (final String picID : links) {
                if (!allids.contains(picID)) {
                    allids.add(picID);
                    final DownloadLink dl = createPicDownloadlink(picID);
                    fp.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not supported in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                    decryptedPicsNum++;
                }
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                logger.info("facebook.com: Found less pics than a full page -> Continuing anyways");
            }
            logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("facebook.com: Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("facebook.com: Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("facebook.com: Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        fp.addLinks(decryptedLinks);
        logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("facebook.com: -> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void decryptVideoSet() throws IOException, DecrypterException {
        if (!logged_in) {
            logger.info("Decrypting video album without login: " + PARAMETER);
        }
        getpagefirsttime(PARAMETER);
        if (br.containsHTML(CONTENTUNAVAILABLE)) {
            logger.info("Link offline: " + PARAMETER);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        String fpName = br.getRegex("<title id=\"pageTitle\">([^<>\"]*?)videos }}| Facebook</title>").getMatch(0);

        final String[] links = br.getRegex("uiVideoLinkMedium\" href=\"(https?://(www\\.)?facebook\\.com/photo\\.php\\?v=\\d+)").getColumn(0);
        for (final String link : links) {
            final DownloadLink dl = createDownloadlink(link.replace("facebook.com/", CRYPTLINK));
            try {
                dl.setContentUrl(link);
            } catch (final Throwable e) {
                dl.setBrowserUrl(link);
            }
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
    }

    private void decryptGroupsPhotos() throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
        String fpName = getPageTitle();
        final String rev = getRev();
        final String user = getUser();
        final String dyn = getDyn();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        final String ajaxpipe_token = getajaxpipeToken();
        final String controller = "GroupPhotosetPagelet";
        final String group_id = new Regex(PARAMETER, "groups/(\\d+)/").getMatch(0);
        if (user == null || ajaxpipe_token == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook__groups_photos_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        final ArrayList<String> allids = new ArrayList<String>();
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
            try {
                if (this.isAbort()) {
                    logger.info("Decryption stopped at request " + i);
                    return;
                }
            } catch (final Throwable e) {
                // Not supported in old 0.9.581 Stable
            }

            int currentMaxPicCount = 20;

            String[] links;
            if (i > 1) {
                if (br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
                    logger.info("facebook.com: Server says the set is fully loaded -> Stopping");
                    break;
                }
                final String currentLastFbid = getLastFBID();
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (currentLastFbid == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (currentLastFbid == null) {
                    logger.warning("Decrypter maybe broken for link: " + PARAMETER);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/" + controller + "?ajaxpipe=1&ajaxpipe_token=" + ajaxpipe_token + "&no_script_path=1&data=%7B%22scroll_load%22%3Atrue%2C%22last_fbid%22%3A" + currentLastFbid + "%2C%22fetch_size%22%3A120%2C%22group_id%22%3A" + group_id + "%7D&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=" + i + "&__rev=" + rev;
                getPage(loadLink);
                links = br.getRegex("\"(https?://(www\\.)?facebook\\.com/(photo\\.php\\?(fbid|v)=|media/set/\\?set=oa\\.)\\d+)").getColumn(0);
                currentMaxPicCount = 40;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("\"(https?://(www\\.)?facebook\\.com/(photo\\.php\\?(fbid|v)=|media/set/\\?set=oa\\.)\\d+)").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + PARAMETER);
                return;
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String single_link : links) {
                final String current_id = new Regex(single_link, "(\\d+)$").getMatch(0);
                if (!allids.contains(current_id)) {
                    allids.add(current_id);
                    final DownloadLink dl = createDownloadlink(single_link.replace("facebook.com/", CRYPTLINK));
                    try {
                        dl.setContentUrl(single_link);
                    } catch (final Throwable e) {
                        dl.setBrowserUrl(single_link);
                    }
                    if (!logged_in) {
                        dl.setProperty("nologin", true);
                    }
                    if (FASTLINKCHECK_PICTURES_ENABLED) {
                        dl.setAvailable(true);
                    }
                    try {/* JD2 only */
                        dl.setContentUrl(single_link);
                    } catch (Throwable e) {/* Stable */
                        dl.setBrowserUrl(single_link);
                    }
                    // Set temp name, correct name will be set in hosterplugin later
                    dl.setName(getTemporaryDecryptName(single_link));
                    fp.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not supported in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                    decryptedPicsNum++;
                }
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                logger.info("facebook.com: Found less pics than a full page -> Continuing anyways");
            }
            logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("facebook.com: Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("facebook.com: Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("facebook.com: Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        fp.addLinks(decryptedLinks);
        logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("facebook.com: -> Even though it seems like we don't have all images, that's all ;)");
        }

    }

    // TODO: Use this everywhere as it should work universal
    private void decryptPicsGeneral(String controller) throws Exception {
        if (!logged_in) {
            logger.info("Cannot decrypt link without valid account: " + PARAMETER);
            return;
        }
        String fpName = getPageTitle();
        final String rev = getRev();
        final String user = getUser();
        final String dyn = getDyn();
        final String appcollection = br.getRegex("\"pagelet_timeline_app_collection_(\\d+:\\d+)(:\\d+)?\"").getMatch(0);
        final String profileID = getProfileID();
        final String totalPicCount = br.getRegex("data-medley-id=\"pagelet_timeline_medley_photos\">Photos<span class=\"_gs6\">(\\d+((,|\\.)\\d+)?)</span>").getMatch(0);
        if (controller == null) {
            controller = br.getRegex("\"photos\",\\[\\{\"controller\":\"([^<>\"]*?)\"").getMatch(0);
        }
        if (user == null || profileID == null || appcollection == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            return;
        }
        if (fpName == null) {
            fpName = "Facebook_photos_of_of_user_" + user;
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        boolean dynamicLoadAlreadyDecrypted = false;
        final ArrayList<String> allids = new ArrayList<String>();
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
            try {
                if (this.isAbort()) {
                    logger.info("Decryption stopped at request " + i);
                    return;
                }
            } catch (final Throwable e) {
                // Not supported in old 0.9.581 Stable
            }

            int currentMaxPicCount = 20;

            String[] links;
            if (i > 1) {
                if (br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
                    logger.info("facebook.com: Server says the set is fully loaded -> Stopping");
                    break;
                }
                final String cursor = br.getRegex("\\[\"pagelet_timeline_app_collection_[^<>\"]*?\",\\{\"[^<>\"]*?\":\"[^<>\"]*?\"\\},\"([^<>\"]*?)\"").getMatch(0);
                // If we have exactly currentMaxPicCount pictures then we reload one
                // time and got all, 2nd time will then be 0 more links
                // -> Stop
                if (cursor == null && dynamicLoadAlreadyDecrypted) {
                    break;
                } else if (cursor == null) {
                    logger.warning("Decrypter maybe broken for link: " + PARAMETER);
                    logger.info("Returning already decrypted links anyways...");
                    break;
                }
                // final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/" + controller + "?data=%7B%22collection_token%22%3A%22" +
                // Encoding.urlEncode(appcollection) + "%3A5%22%2C%22cursor%22%3A%22" + cursor +
                // "%22%2C%22tab_key%22%3A%22photos_all%22%2C%22profile_id%22%3A" + profileowner +
                // "%2C%22overview%22%3Afalse%2C%22ftid%22%3Anull%2C%22order%22%3Anull%2C%22sk%22%3A%22photos%22%2C%22importer_state%22%3Anull%7D&__user="
                // + user + "&__a=1&__dyn=7n8apij2qmumdDgDxyIJ3Ga58Ciq2W8GA8ABGeqheCu6po&__req=" + i + "&__rev=1162685";
                final String loadLink = MAINPAGE + "/ajax/pagelet/generic.php/" + controller + "?data=%7B%22collection_token%22%3A%22" + Encoding.urlEncode(appcollection) + "%3A5%22%2C%22cursor%22%3A%22" + cursor + "%22%2C%22tab_key%22%3A%22photos_all%22%2C%22profile_id%22%3A" + profileID + "%2C%22overview%22%3Afalse%2C%22ftid%22%3Anull%2C%22order%22%3Anull%2C%22sk%22%3A%22photos%22%2C%22importer_state%22%3Anull%7D&__user=" + user + "&__a=1&__dyn=" + dyn + "&__req=" + i + "&__rev=" + rev;
                br.getPage(loadLink);
                links = br.getRegex("ajax\\\\/photos\\\\/hovercard\\.php\\?fbid=(\\d+)\\&").getColumn(0);
                currentMaxPicCount = 40;
                dynamicLoadAlreadyDecrypted = true;
            } else {
                links = br.getRegex("id=\"pic_(\\d+)\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                logger.warning("Decryption done or decrypter broken: " + PARAMETER);
                return;
            }
            boolean stop = false;
            logger.info("Decrypting page " + i + " of ??");
            for (final String picID : links) {
                if (!allids.contains(picID)) {
                    allids.add(picID);
                    final DownloadLink dl = createPicDownloadlink(picID);
                    fp.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not supported in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                    decryptedPicsNum++;
                }
            }
            // currentMaxPicCount = max number of links per segment
            if (links.length < currentMaxPicCount) {
                logger.info("facebook.com: Found less pics than a full page -> Continuing anyways");
            }
            logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
            if (timesNochange == 3) {
                logger.info("facebook.com: Three times no change -> Aborting decryption");
                stop = true;
            }
            if (decryptedPicsNum >= totalPicsNum) {
                logger.info("facebook.com: Decrypted all pictures -> Stopping");
                stop = true;
            }
            if (decryptedPicsNum == lastDecryptedPicsNum) {
                timesNochange++;
            } else {
                timesNochange = 0;
            }
            lastDecryptedPicsNum = decryptedPicsNum;
            if (stop) {
                logger.info("facebook.com: Seems like we're done and decrypted all links, stopping at page: " + i);
                break;
            }
        }
        fp.addLinks(decryptedLinks);
        logger.info("facebook.com: Decrypted " + decryptedPicsNum + " of " + totalPicsNum);
        if (decryptedPicsNum < totalPicsNum && br.containsHTML("\"TimelineAppCollection\",\"setFullyLoaded\"")) {
            logger.info("facebook.com: -> Even though it seems like we don't have all images, that's all ;)");
        }
    }

    private void getpagefirsttime(final String parameter) throws IOException {
        // Redirects from "http" to "https" can happen
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String scriptRedirect = br.getRegex("<script>window\\.location\\.replace\\(\"(https?:[^<>\"]*?)\"\\);</script>").getMatch(0);
        if (scriptRedirect != null) {
            scriptRedirect = Encoding.htmlDecode(scriptRedirect.replace("\\", ""));
            br.getPage(scriptRedirect);
        }
        String normal_redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"0; URL=(/[^<>\"]*?)\"").getMatch(0);
        if (normal_redirect != null) {
            normal_redirect = Encoding.htmlDecode(normal_redirect);
            normal_redirect = normal_redirect.replace("u00253A", "%3A");
            br.getPage("https://www.facebook.com" + normal_redirect);
        }
        if (br.getURL().contains("https://")) {
            MAINPAGE = "https://www.facebook.com";
        }
    }

    private DownloadLink createPicDownloadlink(final String picID) {
        final String final_link = "http://www." + CRYPTLINK + "photo.php?fbid=" + picID;
        final String real_link = "http://www.facebook.com/photo.php?fbid=" + picID;
        final DownloadLink dl = createDownloadlink(final_link);
        try {
            dl.setContentUrl(real_link);
        } catch (final Throwable e) {
            dl.setBrowserUrl(real_link);
        }
        if (!logged_in) {
            dl.setProperty("nologin", true);
        }
        if (FASTLINKCHECK_PICTURES_ENABLED) {
            dl.setAvailable(true);
        }
        // Set temp name, correct name will be set in hosterplugin later
        dl.setName(getTemporaryDecryptName(final_link));
        return dl;
    }

    private String getProfileID() {
        return br.getRegex("data\\-gt=\"\\&#123;\\&quot;profile_owner\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
    }

    private String getajaxpipeToken() {
        return br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
    }

    private String getRev() {
        String rev = br.getRegex("\"revision\":(\\d+)").getMatch(0);
        if (rev == null) {
            rev = "1162685";
        }
        return rev;
    }

    private String getPageTitle() {
        return br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
    }

    private String getDyn() {
        return "7n8apij2qmumdDgDxyIJ3Ga58Ciq2W8GA8ABGeqheCu6po";
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

    private String getTemporaryDecryptName(final String input_link) {
        final String lid = new Regex(input_link, "(\\d+)$").getMatch(0);
        String temp_name = lid;
        if (input_link.matches(TYPE_SINGLE_PHOTO)) {
            temp_name += ".jpg";
        } else if (input_link.matches(TYPE_SINGLE_VIDEO_ALL)) {
            temp_name += ".mp4";
        }
        return temp_name;
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

    private String getUser() {
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

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

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
                ((jd.plugins.hoster.FaceBookComVideos) facebookPlugin).login(aa, false, this.br);
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