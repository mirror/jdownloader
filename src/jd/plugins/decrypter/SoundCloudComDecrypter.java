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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import jd.plugins.hoster.DummyScriptEnginePlugin;
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
    private static final String     TYPE_USER_IN_PLAYLIST   = "https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/sets";
    private static final String     TYPE_USER_LIKES         = "https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/likes";
    private static final String     TYPE_GROUPS             = "https?://(www\\.)?soundcloud\\.com/groups/[A-Za-z0-9\\-_]+";

    private static final String     TYPE_SHORT              = "https?://snd\\.sc/[A-Za-z0-9]+";

    private static int              max_entries_per_request = 100;

    private static final String     GRAB500THUMB            = "GRAB500THUMB";
    private static final String     GRABORIGINALTHUMB       = "GRABORIGINALTHUMB";
    private static final String     CUSTOM_PACKAGENAME      = "CUSTOM_PACKAGENAME";
    private static final String     CUSTOM_DATE             = "CUSTOM_DATE";

    private PluginForHost           HOSTPLUGIN              = null;
    private SubConfiguration        CFG                     = null;
    private String                  ORIGINAL_LINK           = null;
    private String                  parameter               = null;
    private ArrayList<DownloadLink> decryptedLinks          = null;
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
                } else if (parameter.matches(TYPE_USER_IN_PLAYLIST)) {
                    decryptUserInPlaylists();
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
                /* If the user wants to download the thumbnail as well it's a bit more complicated */
                if (decrypt500Thumb || decryptOriginalThumb) {
                    try {

                        resolve(parameter);
                        /* Add soundcloud link */
                        DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                        Map<String, Object> entry = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
                        dl = setDlDataJson(dl, entry);
                        decryptedLinks.add(dl);

                        get500Thumbnail(dl, entry);
                        getOriginalThumbnail(dl, entry);
                    } catch (final Exception e) {
                        if (br.containsHTML("\"404 - Not Found\"")) {
                            return decryptedLinks;
                        }
                        logger.info("Failed to get thumbnail, adding song link only");
                        decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
                    }
                } else {
                    DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                    dl.setProperty("plain_url_username", url_username);
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

    /**
     * Decrypts all tracks of a single set
     * 
     * @throws Exception
     */
    private void decryptSet() throws Exception {
        resolve(parameter);
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());

        playlistname = getString(data, "title");

        username = getString(data, "user.username");
        final String playlist_uri = getString(data, "uri");
        if (playlist_uri == null) {
            return;
        }
        br.getPage(playlist_uri + "?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&app_version=" + jd.plugins.hoster.SoundcloudCom.APP_VERSION);

        data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) data.get("tracks");

        final String usernameOfSet = username;
        if (tracks == null || tracks.size() == 0 || usernameOfSet == null) {
            if (getString(data, "duration").equals("0")) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("null");
        }
        int counter = 1;
        for (final Map<String, Object> item : tracks) {
            final String permalink = getString(item, "permalink");
            if (permalink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            String userPermalink = getString(item, "user.permalink");

            DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + userPermalink + "/" + permalink);
            dl.setProperty("setsposition", counter + ".");
            dl = setDlDataJson(dl, item);
            decryptedLinks.add(dl);
            get500Thumbnail(dl, item);
            getOriginalThumbnail(dl, item);
            counter++;
        }
    }

    public static String getString(Map<String, Object> data, String string) {
        Object newData = data;
        for (String s : string.split("\\.")) {

            newData = ((Map<String, Object>) newData).get(s);
            //
            if (newData == null) {
                return null;
            }
        }
        if (newData == null) {
            return null;
        }
        return newData.toString();
    }

    /**
     * Decrypts all sets of a user
     * 
     * @throws Exception
     */
    private void decryptUserSets() throws Exception {
        max_entries_per_request = 5;
        url_username = new Regex(parameter, "soundcloud\\.com/([^<>\"/]+)/?").getMatch(0);
        resolve(null);
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
        final String user_id = getString(data, "id");

        if (user_id == null) {
            return;
        }

        int page = 1;
        int offset = 0;
        while (true) {

            String next_page_url = "https://api.soundcloud.com/users/" + user_id + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + offset + "&page_size=" + max_entries_per_request + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;

            br.getPage(next_page_url);
            Map<String, Object> response = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
            List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                if (isAbort()) {
                    return;
                }
                List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }

            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            }
            page++;
            offset += collection.size();

        }
        ;

        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    /**
     * Decrypts all sets (playlists) of a users' category 'In Playlists''
     * 
     * @throws Exception
     */
    private void decryptUserInPlaylists() throws Exception {
        max_entries_per_request = 5;
        resolve(parameter);
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
        final String user_id = getString(data, "id");

        if (user_id == null) {
            return;
        }

        int page = 1;
        int offset = 0;
        while (true) {

            String next_page_url = "https://api.soundcloud.com/tracks/" + user_id + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + page + "&page_size=" + max_entries_per_request + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;

            br.getPage(next_page_url);
            Map<String, Object> response = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
            List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                if (isAbort()) {
                    return;
                }
                List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }

            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            }
            page++;
            offset += collection.size();

        }
        ;
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptLikes() throws Exception {
        br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter.replace("/likes", "")) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
        String user_id = getString(data, "id");
        final long items_count = ((Number) data.get("likes_count")).intValue();
        final long pages = items_count / max_entries_per_request;
        int page = 1;
        while (true) {
            logger.info("Decrypting page " + page + " of probably " + pages);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            String next_page_url = "https://api.soundcloud.com/e1/users/" + user_id + "/likes?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + ((page - 1) * max_entries_per_request) + "&order=favorited_at&page_number=" + page + "&page_size=" + max_entries_per_request + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;

            br.getPage(next_page_url);

            List<Map<String, Object>> collection = parseCollection();
            page++;
            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            }

        }
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptGroups() throws Exception {

        url_username = new Regex(parameter, "/groups/(.+)").getMatch(0);
        resolve(parameter);
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());

        final String group_id = getString(data, "id");
        if (group_id == null) {
            decryptedLinks = null;
            return;
        }

        final long items_count = ((Number) data.get("track_count")).longValue();
        final long pages = items_count / max_entries_per_request;
        int current_page = 1;
        int offset = 0;
        while (true) {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user: " + parameter);
                    return;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            String next_page_url = "https://api.soundcloud.com/groups/" + group_id + "/tracks?app_version=" + jd.plugins.hoster.SoundcloudCom.APP_VERSION + "&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=approved_at";

            br.getPage(next_page_url);
            int pre = decryptedLinks.size();
            List<Map<String, Object>> collection = parseCollection();
            if (decryptedLinks.size() != pre + max_entries_per_request) {
                System.out.println(1);
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            } else {
                offset += collection.size();
            }
            current_page++;
        }
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    public List<Map<String, Object>> parseCollection() throws Exception, DecrypterException, ParseException, IOException {
        Map<String, Object> response = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());
        List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");

        return parseTracklist(collection);
    }

    public List<Map<String, Object>> parseTracklist(List<Map<String, Object>> collection) throws ParseException, IOException {
        for (final Map<String, Object> item : collection) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return null;
            }
            DownloadLink dl = null;
            String type = getString(item, "type");

            Map<String, Object> entry = null;
            if (type == null) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("track".equals(type)) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("track_repost".equals(type)) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("playlist".equals(type)) {
                entry = (Map<String, Object>) item.get("playlist");
            } else {
                entry = (Map<String, Object>) item.get(type);
            }
            if (entry == null) {
                // in some cases, there is no subgroup
                entry = item;
            }

            final String permalink_url = getString(entry, "permalink_url");
            final String url = getString(entry, "permalink");
            if (permalink_url == null || url == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                continue;
            }
            if (type != null && type.equals("playlist")) {
                dl = createDownloadlink("https://soundcloud.com/" + url_username + "/sets/" + url);
            } else {
                String track_url = permalink_url.replace("http://", "https://").replace("soundcloud.com", "soundclouddecrypted.com");
                if (!track_url.endsWith("/" + url)) {
                    track_url += "/" + url;
                }
                dl = createDownloadlink(track_url);
                dl = setDlDataJson(dl, entry);
                get500Thumbnail(dl, entry);
                getOriginalThumbnail(dl, entry);
            }
            decryptedLinks.add(dl);
        }
        return collection;
    }

    private void decryptUser() throws Exception {
        resolve(null);
        // Decrypt all tracks of a user
        Map<String, Object> data = DummyScriptEnginePlugin.jsonToJavaMap(br.toString());

        username = getString(data, "username");
        String userID = getString(data, "id");

        if (userID == null) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        // seems to be a limit of the API (12.02.14)
        int maxPerCall = 200;
        int offset = 0;

        while (true) {
            br.getPage("https://api.sndcdn.com/e1/users/" + userID + "/sounds?limit=" + maxPerCall + "&offset=" + offset + "&linked_partitioning=1&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);

            List<Map<String, Object>> collection = parseCollection();
            if (collection == null || collection.size() != maxPerCall) {
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

    private DownloadLink get500Thumbnail(final DownloadLink audiolink, final Map<String, Object> source) throws ParseException {
        DownloadLink thumb = null;
        if (decrypt500Thumb) {
            try {
                // Handle thumbnail stuff
                String artworkurl = getString(source, "artwork_url");

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

    private DownloadLink getOriginalThumbnail(final DownloadLink audiolink, final Map<String, Object> source) throws ParseException {
        DownloadLink thumb = null;
        if (decryptOriginalThumb) {
            try {
                // Handle thumbnail stuff
                String artworkurl = getString(source, "artwork_url");

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
            resolveurl = forced_url;
        } else {
            resolveurl = "https://soundcloud.com/" + url_username;
        }
        br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(resolveurl) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID);
        if (br.containsHTML("\"404 \\- Not Found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
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

    private DownloadLink setDlDataJson(final DownloadLink dl, final Map<String, Object> data) throws ParseException, IOException {
        dl.setProperty("plain_url_username", url_username);
        if (data != null) {
            final AvailableStatus status = jd.plugins.hoster.SoundcloudCom.checkStatusJson(dl, data, false);
            dl.setAvailableStatus(status);
        }
        return dl;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}