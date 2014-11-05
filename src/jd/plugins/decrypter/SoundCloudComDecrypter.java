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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://((www\\.|m\\.)?(soundcloud\\.com/[^<>\"\\']+(\\?format=html\\&page=\\d+|\\?page=\\d+)?|snd\\.sc/[A-Za-z0-9]+)|api\\.soundcloud\\.com/tracks/\\d+(\\?secret_token=[A-Za-z0-9\\-_]+)?|api\\.soundcloud\\.com/playlists/\\d+\\?secret_token=[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class SoundCloudComDecrypter extends PluginForDecrypt {

    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     EXCEPTION_LINKOFFLINE   = "EXCEPTION_LINKOFFLINE";

    private static final String     TYPE_INVALID            = "https?://(www\\.)?soundcloud\\.com/(you/|tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|#?search|upload|people|dashboard|#/).*?";
    private static final String     TYPE_API_PLAYLIST       = "https?://(www\\.|m\\.)?api\\.soundcloud\\.com/playlists/\\d+\\?secret_token=[A-Za-z0-9\\-_]+";
    private static final String     TYPE_API_TRACK          = "https?://(www\\.|m\\.)?api\\.soundcloud\\.com/tracks/\\d+(\\?secret_token=[A-Za-z0-9\\-_]+)?";
    private static final String     TYPE_SINGLE_SET         = "https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/sets/[A-Za-z0-9\\-_]+";
    private static final String     TYPE_USER_SETS          = "https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/sets";
    private static final String     TYPE_USER_LIKES         = "https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/likes";
    private static final String     TYPE_GROUPS             = "https?://(www\\.)?soundcloud\\.com/groups/[A-Za-z0-9\\-_]+";

    private static final String     TYPE_SHORT              = "https?://snd\\.sc/[A-Za-z0-9]+";

    private static final int        max_entries_per_request = 100;

    private static final String     GRAB500THUMB            = "GRAB500THUMB";
    private static final String     GRABORIGINALTHUMB       = "GRABORIGINALTHUMB";
    private static final String     CUSTOM_PACKAGENAME      = "CUSTOM_PACKAGENAME";
    private static final String     CUSTOM_DATE             = "CUSTOM_DATE";

    private PluginForHost           HOSTPLUGIN              = null;
    private SubConfiguration        CFG                     = null;
    private String                  ORIGINAL_LINK           = null;
    private String                  parameter               = null;
    private ArrayList<DownloadLink> decryptedLinks          = new ArrayList<DownloadLink>();
    private boolean                 decrypt500Thumb         = false;
    private boolean                 decryptOriginalThumb    = false;
    private String                  username                = null;
    private String                  playlistname            = null;
    private String                  url_username            = null;

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CFG = SubConfiguration.getConfig("soundcloud.com");
        ORIGINAL_LINK = Encoding.htmlDecode(param.toString());
        if (ORIGINAL_LINK.contains("#")) {
            ORIGINAL_LINK = ORIGINAL_LINK.substring(0, ORIGINAL_LINK.indexOf("#"));
        }
        decrypt500Thumb = CFG.getBooleanProperty(GRAB500THUMB, false);
        decryptOriginalThumb = CFG.getBooleanProperty(GRABORIGINALTHUMB, false);
        // Sometimes slow servers
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        try {
            // They can have huge pages, allow eight times the normal load limit
            br.setLoadLimit(8388608);
        } catch (final Throwable e) {
            // Not available in old 0.9.581 Stable
        }
        // Login if possible, helps to get links which need the user to be logged in
        HOSTPLUGIN = JDUtilities.getPluginForHost("soundcloud.com");
        final Account aa = AccountController.getInstance().getValidAccount(HOSTPLUGIN);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).login(this.br, aa, false);
            } catch (final PluginException e) {
            }
        }

        try {
            /* Correct links */
            correctInputLinks();
            url_username = new Regex(parameter, "soundcloud\\.com/([^<>\"/]+)/?").getMatch(0);

            br.setFollowRedirects(true);

            final boolean decryptList = isList();
            if (decryptList) {
                if (parameter.matches(TYPE_SINGLE_SET)) {
                    decryptSet();
                } else if (parameter.matches(TYPE_USER_SETS)) {
                    decryptUserSets();
                } else if (parameter.matches(TYPE_USER_LIKES)) {
                    decryptLikes();
                } else if (parameter.matches(TYPE_GROUPS)) {
                    decryptGroups();
                } else {
                    decryptUser();
                }
                final String date = br.getRegex("<created\\-at type=\"datetime\">([^<>\"]*?)</created\\-at>").getMatch(0);
                if (username == null) {
                    username = "Unknown user";
                }
                username = Encoding.htmlDecode(username.trim());
                if (playlistname == null) {
                    playlistname = username;
                }
                playlistname = Encoding.htmlDecode(playlistname.trim());
                final String fpName = getFormattedPackagename(username, playlistname, date);
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            } else {
                /* If the user wants to download the thumbnail also it's a bit more complicated */
                if (decrypt500Thumb || decryptOriginalThumb) {
                    try {
                        br.getPage(parameter);
                        if (br.containsHTML("\"404 \\- Not Found\"")) {
                            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                        }
                        resolve(null);
                        /* Add soundcloud link */
                        DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                        dl = setDlData(dl, br.toString());
                        decryptedLinks.add(dl);

                        get500Thumbnail(dl, this.br.toString());
                        getOriginalThumbnail(dl, this.br.toString());
                    } catch (final Exception e) {
                        logger.info("Failed to get thumbnail, adding song link only");
                        decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
                    }
                } else {
                    DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                    dl = setDlData(dl, null);
                    decryptedLinks.add(dl);
                }
            }
        } catch (final DecrypterException e) {
            if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                logger.info("Link offline (empty set-link): " + parameter);
                final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/offlinedecrypted/" + System.currentTimeMillis() + new Random().nextInt(100000));
                dl.setAvailable(false);
                dl.setProperty("offline", true);
                dl.setFinalFileName(new Regex(parameter, "soundcloud\\.com/(.+)").getMatch(0));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            throw e;
        } catch (final BrowserException e) {
            logger.info("Link offline (BrowserException): " + parameter);
            final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/offlinedecrypted/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            dl.setFinalFileName(new Regex(parameter, "soundcloud\\.com/(.+)").getMatch(0));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void correctInputLinks() throws IOException, DecrypterException {
        parameter = ORIGINAL_LINK.replace("http://", "https://").replaceAll("(/download|\\\\)", "").replaceFirst("://(www|m)\\.", "://");
        if (parameter.matches(TYPE_INVALID)) {
            logger.info("Invalid link: " + parameter);
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        if (parameter.matches(TYPE_SHORT)) {
            br.setFollowRedirects(false);
            /* Use ORIGINAL_LINK because https is not available for short links */
            br.getPage(ORIGINAL_LINK);
            final String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            parameter = newparameter;
        } else if (parameter.matches(TYPE_API_TRACK)) {
            String get_data = "?format=json&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;
            final String secret_token = new Regex(ORIGINAL_LINK, "secret_token=([A-Za-z0-9\\-_]+)").getMatch(0);
            if (secret_token != null) {
                get_data += "&secret_token=" + secret_token;
            }
            br.getPage(parameter + get_data);
            if (br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Link offline (offline track link): " + parameter);
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            String newparameter = br.getRegex("\"permalink_url\":\"(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[A-Za-z0-9\\-_]+)?)\"").getMatch(0);
            /* Maybe we got XML instead of json */
            if (newparameter == null) {
                newparameter = br.getRegex("<permalink\\-url>(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[A-Za-z0-9\\-_]+)?)</permalink\\-url>").getMatch(0);
            }
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            newparameter = newparameter.replace("http://", "https://");
            parameter = newparameter;
        } else if (parameter.matches(TYPE_API_PLAYLIST)) {
            final Regex info = new Regex(parameter, "api\\.soundcloud\\.com/playlists/(\\d+)\\?secret_token=([A-Za-z0-9\\-_]+)");
            br.getPage("https://api.sndcdn.com/playlists/" + info.getMatch(0) + "/?representation=compact&secret_token=" + info.getMatch(1) + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&format=json");
            String newparameter = br.getRegex("\"permalink_url\":\"(http://(www\\.)?soundcloud\\.com/[^<>\"/]*?/sets/[^<>\"]*?)\"").getMatch(0);
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            newparameter = newparameter.replace("http://", "https://");
            parameter = newparameter;
        }
    }

    /** Decrypts all tracks of a single set */
    private void decryptSet() throws IOException, DecrypterException, ParseException {
        resolve(parameter);
        playlistname = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (playlistname == null) {
            playlistname = new Regex(parameter, "/sets/(.+)$").getMatch(0);
        }
        username = getXML("username", br.toString());
        final String playlist_id = br.getRegex("<id type=\"integer\">(\\d+)</id>").getMatch(0);
        if (playlist_id == null) {
            return;
        }
        br.getPage("https://api.soundcloud.com/playlists/" + playlist_id + "?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&app_version=" + jd.plugins.hoster.SoundcloudCom.APP_VERSION);
        final String[] items = br.getRegex("<track>(.*?)</track>").getColumn(0);
        final String usernameOfSet = new Regex(parameter, "soundcloud\\.com/(.*?)/sets/?").getMatch(0);
        if (items == null || items.length == 0 || usernameOfSet == null) {
            if (br.containsHTML("<duration type=\"integer\">0</duration>")) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("null");
        }
        int counter = 1;
        for (final String item : items) {
            final String permalink = getXML("permalink", item);
            if (permalink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            String song_username = new Regex(item, "<kind>user</kind>[\t\n\r ]+<permalink>([^<>\"]*?)</permalink>").getMatch(0);
            if (song_username == null) {
                song_username = usernameOfSet;
            }
            DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + song_username + "/" + permalink);
            dl.setProperty("setsposition", counter + ".");
            dl = setDlData(dl, item);
            decryptedLinks.add(dl);
            get500Thumbnail(dl, item);
            getOriginalThumbnail(dl, item);
            counter++;
        }
    }

    /** Decrypts all sets of a user */
    private void decryptUserSets() throws IOException, DecrypterException, ParseException {
        resolve(null);
        final String user_id = getXML("id", br.toString());
        final long items_count = Long.parseLong(getXML("playlist-count", br.toString()));
        final long pages = items_count / max_entries_per_request;
        String next_page_url = "https://api.soundcloud.com/users/" + user_id + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=0&order=favorited_at&page_number=1&page_size=" + max_entries_per_request + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;
        int current_page = 1;
        while (next_page_url != null) {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            next_page_url = Encoding.htmlDecode(next_page_url);
            br.getPage(next_page_url);

            if (current_page == 1) {
                playlistname = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
                username = getXML("id", br.toString());
                if (playlistname == null) {
                    playlistname = username;
                }
            }

            final String[] items = br.getRegex("<playlist>(.*?)</playlist>").getColumn(0);
            if (items == null || items.length == 0) {
                if (br.containsHTML("<duration type=\"integer\">0</duration>")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            for (final String item : items) {
                final String permalink = getXML("permalink", item);
                if (permalink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new DecrypterException("null");
                }
                final String song_username = new Regex(item, "<kind>user</kind>[\t\n\r ]+<permalink>([^<>\"]*?)</permalink>").getMatch(0);
                final DownloadLink dl = createDownloadlink("https://soundcloud.com/" + song_username + "/sets/" + permalink);
                decryptedLinks.add(dl);
            }
            next_page_url = br.getRegex("<likes next\\-href=\"(https?://api\\.soundcloud\\.com/[^<>\"]*?)\"").getMatch(0);
            current_page++;
        }
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptLikes() throws IOException, DecrypterException, ParseException {
        br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter.replace("/likes", "")) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);
        final String user_id = getXML("id", br.toString());
        final long items_count = Long.parseLong(getXML("likes-count", br.toString()));
        final long pages = items_count / max_entries_per_request;
        String next_page_url = "https://api.soundcloud.com/e1/users/" + user_id + "/likes?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=0&order=favorited_at&page_number=1&page_size=" + max_entries_per_request + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;
        int current_page = 1;
        while (next_page_url != null) {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            next_page_url = Encoding.htmlDecode(next_page_url);
            br.getPage(next_page_url);

            if (current_page == 1) {
                playlistname = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
                username = getXML("id", br.toString());
                if (playlistname == null) {
                    playlistname = username;
                }
            }

            final String[] items = br.getRegex("<track>(.*?)</track>").getColumn(0);
            if (items == null || items.length == 0) {
                if (br.containsHTML("<duration type=\"integer\">0</duration>")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            for (final String item : items) {
                final String permalink = getXML("permalink", item);
                if (permalink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new DecrypterException("null");
                }
                final String song_username = new Regex(item, "<kind>user</kind>[\t\n\r ]+<permalink>([^<>\"]*?)</permalink>").getMatch(0);
                DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + song_username + "/" + permalink);
                dl = setDlData(dl, item);
                decryptedLinks.add(dl);
                get500Thumbnail(dl, item);
                getOriginalThumbnail(dl, item);
            }
            next_page_url = br.getRegex("<likes next\\-href=\"(https?://api\\.soundcloud\\.com/[^<>\"]*?)\"").getMatch(0);
            current_page++;
        }
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptGroups() throws IOException, DecrypterException, ParseException {
        /*
         * Set url-username (again) here because the general RegEx won't work for this linktype - simply use the name of the group as
         * url_username.
         */
        url_username = new Regex(parameter, "/groups/(.+)").getMatch(0);
        resolve(null);
        final String group_id = br.getRegex("<id type=\"integer\">(\\d+)</id>").getMatch(0);
        if (group_id == null) {
            decryptedLinks = null;
            return;
        }
        final long items_count = Long.parseLong(getXML("track-count", br.toString()));
        final long pages = items_count / max_entries_per_request;
        String next_page_url = "https://api.soundcloud.com/groups/" + group_id + "/tracks?app_version=" + jd.plugins.hoster.SoundcloudCom.APP_VERSION + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&limit=" + max_entries_per_request + "&linked_partitioning=1&offset=0&order=approved_at";
        int current_page = 1;
        while (next_page_url != null) {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            next_page_url = Encoding.htmlDecode(next_page_url);
            br.getPage(next_page_url);

            if (current_page == 1) {
                playlistname = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
                username = getXML("id", br.toString());
                if (playlistname == null) {
                    playlistname = username;
                }
            }

            final String[] items = br.getRegex("<track>(.*?)</track>").getColumn(0);
            if (items == null || items.length == 0) {
                if (br.containsHTML("<duration type=\"integer\">0</duration>")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            for (final String item : items) {
                final String permalink = getXML("permalink", item);
                if (permalink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new DecrypterException("null");
                }
                final String song_username = new Regex(item, "<kind>user</kind>[\t\n\r ]+<permalink>([^<>\"]*?)</permalink>").getMatch(0);
                DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + song_username + "/" + permalink);
                dl = setDlData(dl, item);
                decryptedLinks.add(dl);
                get500Thumbnail(dl, item);
                getOriginalThumbnail(dl, item);
            }
            next_page_url = br.getRegex("next\\-href=\"(https?://api\\.soundcloud\\.com/[^<>\"]*?)\"").getMatch(0);
            current_page++;
        }
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptUser() throws IOException, DecrypterException, ParseException {
        resolve(null);
        // Decrypt all tracks of a user
        username = getXML("username", br.toString());
        if (username == null) {
            username = getJson("username");
        }
        if (username == null) {
            username = new Regex(parameter, "soundcloud\\.com/(.+)").getMatch(0);
        }
        String userID = br.getRegex("<uri>https://api\\.soundcloud\\.com/users/(\\d+)").getMatch(0);
        if (userID == null) {
            userID = br.getRegex("id type=\"integer\">(\\d+)").getMatch(0);
        }
        if (userID == null) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        // seems to be a limit of the API (12.02.14)
        int maxPerCall = 200;
        int offset = 0;
        while (true) {
            br.getPage("https://api.sndcdn.com/e1/users/" + userID + "/sounds?limit=" + maxPerCall + "&offset=" + offset + "&linked_partitioning=1&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);
            final String[] items = br.getRegex("<stream\\-item>(.*?)</stream\\-item>").getColumn(0);
            if (items == null || items.length == 0) {
                if (br.containsHTML("<stream\\-items type=\"array\"/>")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            for (final String item : items) {
                DownloadLink dl = null;
                final String type = getXML("type", br.toString());
                final String permalink_url = getXML("permalink-url", item);
                final String url = getXML("permalink", item);
                if (type == null || permalink_url == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new DecrypterException("null");
                }
                if (type.equals("playlist")) {
                    dl = createDownloadlink("https://soundcloud.com/" + url_username + "/sets/" + url);
                } else {
                    final String track_url = permalink_url.replace("http://", "https://").replace("soundcloud.com", "soundclouddecrypted.com") + "/" + url;
                    dl = createDownloadlink(track_url);
                    dl = setDlData(dl, item);
                    get500Thumbnail(dl, item);
                    getOriginalThumbnail(dl, item);
                }
                decryptedLinks.add(dl);
            }
            if (items.length != maxPerCall) {
                break;
            } else {
                offset += maxPerCall;
            }
        }

    }

    private boolean isList() throws DecrypterException {
        if (parameter == null) {
            throw new DecrypterException("parameter == null");
        } else if (parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?") || parameter.contains("/groups/") || parameter.contains("/sets")) {
            return true;
        } else if (parameter.matches(TYPE_USER_LIKES)) {
            return true;
        } else if (parameter.matches(".*?soundcloud\\.com(/[A-Za-z\\-_0-9]+){2,3}/?")) {
            return false;
        } else {
            return true;
        }
    }

    private String getXML(final String parameter, final String source) {
        return jd.plugins.hoster.SoundcloudCom.getXML(parameter, source);
    }

    private DownloadLink get500Thumbnail(final DownloadLink audiolink, final String source) throws ParseException {
        DownloadLink thumb = null;
        if (decrypt500Thumb) {
            try {
                // Handle thumbnail stuff
                String artworkurl = new Regex(source, "<artwork\\-url>(https?://[^<>\"]*?\\-large\\.jpg(\\?[a-z0-9]+)?)</artwork\\-url>").getMatch(0);
                if (artworkurl != null) {
                    artworkurl = artworkurl.replace("-large.jpg", "-t500x500.jpg");
                    thumb = createDownloadlink("directhttp://" + artworkurl);
                    thumb.setProperty("originaldate", audiolink.getStringProperty("originaldate", null));
                    thumb.setProperty("plainfilename", audiolink.getStringProperty("plainfilename", null) + "_500x500");
                    thumb.setProperty("linkid", audiolink.getStringProperty("linkid", null));
                    thumb.setProperty("channel", audiolink.getStringProperty("channel", null));
                    thumb.setProperty("type", "jpg");
                    thumb.setProperty("plain_url_username", url_username);
                    final String formattedFilename = jd.plugins.hoster.SoundcloudCom.getFormattedFilename(thumb);
                    thumb.setFinalFileName(formattedFilename);
                    thumb.setAvailable(true);
                    decryptedLinks.add(thumb);
                }
            } catch (final ParseException e) {
                logger.info("Failed to get 500x500 thumbnail...");
            }
        }

        return thumb;
    }

    private DownloadLink getOriginalThumbnail(final DownloadLink audiolink, final String source) throws ParseException {
        DownloadLink thumb = null;
        if (decryptOriginalThumb) {
            try {
                // Handle thumbnail stuff
                String artworkurl = new Regex(source, "<artwork\\-url>(https?://[^<>\"]*?\\-large\\.jpg(\\?[a-z0-9]+)?)</artwork\\-url>").getMatch(0);
                if (artworkurl != null) {
                    artworkurl = artworkurl.replace("-large.jpg", "-original.jpg");
                    thumb = createDownloadlink("directhttp://" + artworkurl);
                    thumb.setProperty("originaldate", audiolink.getStringProperty("originaldate", null));
                    thumb.setProperty("plainfilename", audiolink.getStringProperty("plainfilename", null) + "_original");
                    thumb.setProperty("linkid", audiolink.getStringProperty("linkid", null));
                    thumb.setProperty("channel", audiolink.getStringProperty("channel", null));
                    thumb.setProperty("type", "jpg");
                    thumb.setProperty("plain_url_username", url_username);
                    final String formattedFilename = jd.plugins.hoster.SoundcloudCom.getFormattedFilename(thumb);
                    thumb.setFinalFileName(formattedFilename);
                    thumb.setAvailable(true);
                    decryptedLinks.add(thumb);
                }
            } catch (final ParseException e) {
                logger.info("Failed to get original thumbnail...");
            }
        }
        return thumb;
    }

    /**
     * Accesses the soundcloud resolve url which returns important information about the user-profile or url (if parameter != null).
     *
     * @throws DecrypterException
     */
    private void resolve(final String forced_url) throws IOException, DecrypterException {
        String resolveurl;
        if (forced_url != null) {
            resolveurl = Encoding.urlEncode(forced_url);
        } else {
            resolveurl = Encoding.urlEncode("https://soundcloud.com/" + url_username);
        }
        br.getPage("https://api.sndcdn.com/resolve?url=" + resolveurl + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);
        if (br.containsHTML("\"404 \\- Not Found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":\"([^\"]+)").getMatch(0);
        }
        return result;
    }

    private final static String defaultCustomPackagename = "*channelname* - *playlistname*";

    private String getFormattedPackagename(final String channelname, final String playlistname, String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = defaultCustomPackagename;
        }
        if (!formattedpackagename.contains("*channelname*") && !formattedpackagename.contains("*playlistname*")) {
            formattedpackagename = defaultCustomPackagename;
        }

        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            // 2011-08-10T22:50:49Z
            date = date.replace("T", ":");
            final String userDefinedDateFormat = CFG.getStringProperty(CUSTOM_DATE);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null) {
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            } else {
                formattedpackagename = formattedpackagename.replace("*date*", "");
            }
        }
        if (formattedpackagename.contains("*url_username*")) {
            formattedpackagename = formattedpackagename.replace("*url_username*", url_username);
        }
        if (formattedpackagename.contains("*channelname*")) {
            formattedpackagename = formattedpackagename.replace("*channelname*", channelname);
        }
        // Insert playlistname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*playlistname*", playlistname);

        return formattedpackagename;
    }

    /** Sets data on a DoiwnloadLink - if source parameter is given, link will be checked and additional parameters will be set */
    private DownloadLink setDlData(final DownloadLink dl, final String source) throws ParseException, IOException {
        dl.setProperty("plain_url_username", url_username);
        if (source != null) {
            final AvailableStatus status = jd.plugins.hoster.SoundcloudCom.checkStatus(dl, source, false);
            dl.setAvailableStatus(status);
        }
        return dl;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}