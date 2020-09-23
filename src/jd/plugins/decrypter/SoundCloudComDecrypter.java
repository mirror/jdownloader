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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
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
import jd.plugins.hoster.SoundcloudCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://((?:www\\.|m\\.)?(soundcloud\\.com/[^<>\"\\']+(?:\\?format=html\\&page=\\d+|\\?page=\\d+)?|snd\\.sc/[A-Za-z0-9]+)|api\\.soundcloud\\.com/tracks/\\d+(?:\\?secret_token=[A-Za-z0-9\\-_]+)?|api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?\\&)secret_token=[A-Za-z0-9\\-_]+)" })
public class SoundCloudComDecrypter extends PluginForDecrypt {
    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String            EXCEPTION_LINKOFFLINE         = "EXCEPTION_LINKOFFLINE";
    private final String            EXCEPTION_LINKINVALID         = "EXCEPTION_LINKINVALID";
    private final String            TYPE_INVALID                  = "https?://(www\\.)?soundcloud\\.com/(you|tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|#?search|upload|people|dashboard|#/)($|/.*?)";
    private final Pattern           TYPE_API_PLAYLIST             = Pattern.compile("https?://(www\\.|m\\.)?api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?&)secret_token=[A-Za-z0-9\\-_]+");
    private final Pattern           TYPE_API_TRACK                = Pattern.compile("https?://(www\\.|m\\.)?api\\.soundcloud\\.com/tracks/\\d+(\\?secret_token=[A-Za-z0-9\\-_]+)?");
    private final Pattern           TYPE_SINGLE_SET               = Pattern.compile("https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/sets/[A-Za-z0-9\\-_]+");
    private final Pattern           TYPE_USER_SETS                = Pattern.compile("https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/sets");
    private final Pattern           TYPE_USER_IN_PLAYLIST         = Pattern.compile("https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/sets");
    private final Pattern           TYPE_USER_LIKES               = Pattern.compile("https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/likes");
    private final Pattern           TYPE_USER_REPOST              = Pattern.compile("https?://(www\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/repost");
    private final Pattern           TYPE_GROUPS                   = Pattern.compile("https?://(www\\.)?soundcloud\\.com/groups/[A-Za-z0-9\\-_]+");
    /* Single soundcloud tracks, posted via smartphone/app. */
    private final String            subtype_mobile_facebook_share = "https?://(m\\.)?soundcloud\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+\\?fb_action_ids=.+";
    private final Pattern           TYPE_SHORT                    = Pattern.compile("https?://snd\\.sc/[A-Za-z0-9]+");
    private int                     max_entries_per_request       = 100;
    /* Settings */
    private final String            GRAB_PURCHASE_URL             = "GRAB_PURCHASE_URL";
    private final String            GRAB500THUMB                  = "GRAB500THUMB";
    private final String            GRABORIGINALTHUMB             = "GRABORIGINALTHUMB";
    private final String            CUSTOM_PACKAGENAME            = "CUSTOM_PACKAGENAME";
    private final String            CUSTOM_DATE                   = "CUSTOM_DATE";
    private SubConfiguration        CFG                           = null;
    private String                  originalLink                  = null;
    private String                  parameter                     = null;
    private ArrayList<DownloadLink> decryptedLinks                = null;
    private FilePackage             fp                            = null;
    private boolean                 decryptPurchaseURL            = false;
    private boolean                 decrypt500Thumb               = false;
    private boolean                 decryptOriginalThumb          = false;
    private String                  username                      = null;
    private String                  playlistname                  = null;
    private String                  url_username                  = null;

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private void addLink(final DownloadLink link) {
        if (link != null) {
            if (fp != null) {
                link.setParentNode(fp);
            }
            decryptedLinks.add(link);
        }
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
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
        try {
            this.decryptedLinks = decryptedLinks;
            CFG = SubConfiguration.getConfig(this.getHost());
            originalLink = Encoding.htmlDecode(param.toString());
            if (originalLink.contains("#")) {
                originalLink = originalLink.substring(0, originalLink.indexOf("#"));
            }
            decryptPurchaseURL = CFG.getBooleanProperty(GRAB_PURCHASE_URL, SoundcloudCom.defaultGRAB_PURCHASE_URL);
            decrypt500Thumb = CFG.getBooleanProperty(GRAB500THUMB, SoundcloudCom.defaultGRAB500THUMB);
            decryptOriginalThumb = CFG.getBooleanProperty(GRABORIGINALTHUMB, SoundcloudCom.defaultGRABORIGINALTHUMB);
            /* Sometimes slow servers */
            br.setConnectTimeout(3 * 60 * 1000);
            br.setReadTimeout(3 * 60 * 1000);
            br.setFollowRedirects(false);
            /* They can have huge pages, allow eight times the normal load limit */
            /* Login if possible, helps to get links which need the user to be logged in */
            final List<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs != null && accs.size() > 0) {
                try {
                    final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
                    ((jd.plugins.hoster.SoundcloudCom) hostPlugin).login(this.br, accs.get(0), false);
                } catch (final PluginException e) {
                    logger.log(e);
                }
            }
            try {
                /* Correct added links */
                correctInputLinks();
                url_username = new Regex(parameter, "soundcloud\\.com/([^<>\"/]+)/?").getMatch(0);
                br.setFollowRedirects(true);
                final boolean decryptList = isList();
                if (decryptList) {
                    if (TYPE_SINGLE_SET.matcher(parameter).find() || TYPE_API_PLAYLIST.matcher(parameter).find()) {
                        decryptSet();
                    } else if (TYPE_USER_SETS.matcher(parameter).find()) {
                        decryptUserSets();
                    } else if (TYPE_USER_IN_PLAYLIST.matcher(parameter).find()) {
                        decryptUserInPlaylists();
                    } else if (TYPE_GROUPS.matcher(parameter).find()) {
                        decryptGroups();
                    } else {
                        decryptUser();
                    }
                } else {
                    /* If the user wants to download the thumbnail as well it's a bit more complicated */
                    try {
                        resolve(parameter);
                        /* Add soundcloud link */
                        DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                        final Map<String, Object> entry = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                        final String title = (String) entry.get("title");
                        if (username == null) {
                            username = getString(entry, "user.username");
                            if (username == null) {
                                username = title;
                            }
                        }
                        if (playlistname == null) {
                            playlistname = "";
                        }
                        setFilePackage(username, playlistname);
                        dl = setDlDataJson(dl, entry);
                        /* Estimate filesize */
                        final Number duration = (Number) entry.get("duration");
                        if (duration != null && duration.longValue() > 0) {
                            dl.setDownloadSize(128 / 8 * 1024 * duration.longValue() / 1000);
                        }
                        addLink(dl);
                        if (decryptPurchaseURL) {
                            getPurchaseURL(entry);
                        }
                        if (decrypt500Thumb) {
                            get500Thumbnail(dl, entry);
                        }
                        if (decryptOriginalThumb) {
                            getOriginalThumbnail(dl, entry);
                        }
                    } catch (final Exception e) {
                        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\"404 - Not Found\"")) {
                            return decryptedLinks;
                        }
                        logger.info("Failed to get thumbnail/purchase_url, adding song link only");
                        addLink(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
                    }
                }
            } catch (final DecrypterException e) {
                logger.log(e);
                if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKINVALID)) {
                    return decryptedLinks;
                } else if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                    logger.info("Link offline (empty set-link): " + parameter);
                    final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/offlinedecrypted/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    dl.setAvailable(false);
                    dl.setProperty("offline", true);
                    dl.setFinalFileName(new Regex(parameter, "soundcloud\\.com/(.+)").getMatch(0));
                    addLink(dl);
                } else {
                    throw e;
                }
            }
            return decryptedLinks;
        } finally {
            this.decryptedLinks = null;
            this.fp = null;
        }
    }

    private void correctInputLinks() throws Exception {
        parameter = originalLink.replace("http://", "https://").replaceAll("(/download|\\\\)", "").replaceFirst("://(www|m)\\.", "://");
        if (parameter.matches(TYPE_INVALID)) {
            logger.info("Invalid link: " + parameter);
            throw new DecrypterException(EXCEPTION_LINKINVALID);
        }
        if (TYPE_SHORT.matcher(parameter).find()) {
            br.setFollowRedirects(false);
            /* Use ORIGINAL_LINK because https is not available for short links */
            br.getPage(originalLink);
            final String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            parameter = newparameter;
        } else if (TYPE_API_TRACK.matcher(parameter).find()) {
            final UrlQuery query = new UrlQuery();
            query.add("format", "json");
            query.add("client_id", SoundcloudCom.getClientId(br));
            final String secret_token = new UrlQuery().parse(originalLink).get("secret_token");
            if (secret_token != null) {
                query.add("secret_token", secret_token);
            }
            br.getPage(parameter + "?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Link offline (offline track link): " + parameter);
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            final LinkedHashMap<String, Object> api_data = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            String newparameter = (String) api_data.get("permalink_url");
            if (newparameter == null) {
                newparameter = br.getRegex("\"permalink_url\":\"(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[A-Za-z0-9\\-_]+)?)\"").getMatch(0);
                /* Maybe we got XML instead of json */
                if (newparameter == null) {
                    newparameter = br.getRegex("<permalink\\-url>(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[A-Za-z0-9\\-_]+)?)</permalink\\-url>").getMatch(0);
                    if (newparameter == null) {
                        logger.warning("Decrypter failed on redirect link: " + parameter);
                        throw new DecrypterException("Decrypter broken for link: " + parameter);
                    }
                }
            }
            newparameter = newparameter.replace("http://", "https://");
            parameter = newparameter;
        } else if (parameter.matches(subtype_mobile_facebook_share)) {
            parameter = "https://soundcloud.com/" + new Regex(parameter, "soundcloud\\.com/([A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)").getMatch(0);
        }
    }

    /**
     * Decrypts all tracks of a single set / playlist
     *
     * @throws Exception
     */
    private void decryptSet() throws Exception {
        String secret_token = null;
        if (parameter.matches("https?://[^/]+/[^/]+/sets/[^/]+/s-[A-Za-z0-9]+")) {
            /* Private set --> URL contains so called 'secret_token' */
            secret_token = new Regex(parameter, "/(s-.+)$").getMatch(0);
        } else if (TYPE_API_PLAYLIST.matcher(parameter).find()) {
            secret_token = new UrlQuery().parse(parameter).get("secret_token");
        }
        resolve(parameter);
        Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        playlistname = getString(data, "title");
        username = getString(data, "user.username");
        /* Old: https://api.soundcloud.com/playlists/<playlist_id> */
        final String permalink_url = getString(data, "permalink_url");
        final String playlist_uri = getString(data, "uri");
        final String playlist_id = getString(data, "id");
        if (playlist_uri == null || playlist_id == null) {
            return;
        }
        List<Map<String, Object>> tracks;
        final String usernameOfSet = username;
        /** Use APIv2 */
        final UrlQuery queryplaylist = new UrlQuery();
        queryplaylist.add("representation", "full");
        queryplaylist.add("client_id", SoundcloudCom.getClientId(br));
        queryplaylist.add("app_version", SoundcloudCom.getAppVersion(br));
        queryplaylist.add("format", "json");
        if (secret_token != null) {
            queryplaylist.add("secret_token", secret_token);
        }
        br.getPage(SoundcloudCom.API_BASEv2 + "/playlists/" + playlist_id + "?" + queryplaylist.toString());
        data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        tracks = (List<Map<String, Object>>) data.get("tracks");
        if (tracks == null || tracks.size() == 0 || usernameOfSet == null) {
            if (getString(data, "duration").equals("0")) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            } else if (tracks != null && tracks.size() == 0) {
                logger.info("Probably GEO-Blocked");
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("null");
        }
        setFilePackage(username, playlistname);
        final ArrayList<Object> trackItemsFound = new ArrayList<Object>();
        /*
         * We will not get info about all tracks via this request - therefore we need to make another API call, collect the rest and then
         * add the URLs.
         */
        final ArrayList<String> trackIdsForPagination = new ArrayList<String>();
        /* This is basically a debug switch. */
        boolean forceItemsToQueue = false;
        for (final Map<String, Object> item : tracks) {
            final String track_id = getString(item, "id");
            if (StringUtils.isEmpty(track_id)) {
                throw new DecrypterException("null");
            }
            final Object permalink = item.get("permalink");
            if (permalink == null || forceItemsToQueue) {
                /* Track info not given --> Save trackID to get track object later */
                forceItemsToQueue = true;
                trackIdsForPagination.add(track_id);
            } else if (permalink != null) {
                /* Track info already given --> Save Object for later */
                trackItemsFound.add(item);
            }
        }
        /*
         * Check if pagination is required.
         */
        if (!trackIdsForPagination.isEmpty()) {
            logger.info("Handling pagination for " + trackIdsForPagination.size() + " objects");
            final StringBuilder sb = new StringBuilder();
            final ArrayList<String> tempIDs = new ArrayList<String>();
            /* Collect all found items here because they're not returning them in the requested order. */
            final ArrayList<Object> unsortedTempTrackItemsFound = new ArrayList<Object>();
            int index = 0;
            int paginationPage = 0;
            while (!this.isAbort()) {
                logger.info("Handling pagination page" + (paginationPage + 1));
                tempIDs.clear();
                while (true) {
                    /* 2020-09-23: We request max. 50 IDs at once. */
                    if (index == trackIdsForPagination.size() || tempIDs.size() == 50) {
                        break;
                    } else {
                        tempIDs.add(trackIdsForPagination.get(index));
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final String idTemp : tempIDs) {
                    sb.append(idTemp);
                    sb.append("%2C");
                }
                final UrlQuery querytracks = new UrlQuery();
                querytracks.add("playlistId", playlist_id);
                querytracks.add("ids", sb.toString());
                querytracks.add("client_id", SoundcloudCom.getClientId(br));
                querytracks.add("app_version", SoundcloudCom.getAppVersion(br));
                querytracks.add("format", "json");
                if (secret_token != null) {
                    querytracks.add("playlistSecretToken", secret_token);
                }
                br.getPage(SoundcloudCom.API_BASEv2 + "/tracks?" + querytracks.toString());
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                for (final Object tracko : ressourcelist) {
                    unsortedTempTrackItemsFound.add(tracko);
                }
                if (index == trackIdsForPagination.size()) {
                    logger.info("Pagination has reached end");
                    break;
                }
                paginationPage++;
            }
            /* Be sure to add new items sorted to main Array */
            for (final String sortedTrackID : trackIdsForPagination) {
                boolean foundTrackID = false;
                for (final Object tempTrackItem : unsortedTempTrackItemsFound) {
                    final LinkedHashMap<String, Object> tempTrackData = (LinkedHashMap<String, Object>) tempTrackItem;
                    final String unsortedTrackID = getString(tempTrackData, "id");
                    if (unsortedTrackID.equals(sortedTrackID)) {
                        /* Found item --> Add it to main Array - it is now in order. */
                        trackItemsFound.add(tempTrackItem);
                        foundTrackID = true;
                        break;
                    }
                }
                if (!foundTrackID) {
                    /* This should never happen but it's not a reason to throw an Exception! */
                    logger.warning("Failed to find trackID: " + sortedTrackID);
                }
            }
        }
        /* Finally add items so they appear in LinkGrabber. */
        int counter = 1;
        for (final Object tracko : trackItemsFound) {
            data = (Map<String, Object>) tracko;
            final String permalink = getString(data, "permalink");
            if (permalink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("null");
            }
            String userPermalink = getString(data, "user.permalink");
            DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + userPermalink + "/" + permalink);
            dl.setProperty(SoundcloudCom.PROPERTY_setsposition, counter + ".");
            dl.setProperty(SoundcloudCom.PROPERTY_playlist_id, playlist_id);
            dl = setDlDataJson(dl, data);
            if (!StringUtils.isEmpty(secret_token) && !StringUtils.isEmpty(permalink_url)) {
                /*
                 * 2020-03-11: 'Secret'/private playlists do not have Content-URLs which go to their individual tracks(?) --> Set URL of the
                 * set they are in as contentURL.
                 */
                dl.setContentUrl(permalink_url);
            }
            final Number duration = (Number) data.get("duration");
            if (duration != null && duration.longValue() > 0) {
                dl.setDownloadSize(128 / 8 * 1024 * duration.longValue() / 1000);
            }
            if (secret_token != null) {
                dl.setProperty(SoundcloudCom.PROPERTY_secret_token, secret_token);
            }
            addLink(dl);
            get500Thumbnail(dl, data);
            getOriginalThumbnail(dl, data);
            counter++;
        }
    }

    private void setFilePackage(String usernName, String playListName) throws ParseException {
        String date = br.getRegex("<created\\-at type=\"datetime\">([^<>\"]*?)</created\\-at>").getMatch(0);
        if (date == null) {
            date = this.br.getRegex("published on <time pubdate>([^<>\"]*?)</time>").getMatch(0);
        }
        if (date == null) {
            date = this.br.getRegex("\"last_modified\":\"([^<>\"]*?)\"").getMatch(0);
        }
        if (usernName == null && playListName == null) {
            return;
        }
        if (usernName == null) {
            usernName = "Unknown user";
        } else {
            usernName = Encoding.htmlDecode(usernName.trim());
        }
        if (playListName == null) {
            playListName = username;
        } else {
            playListName = Encoding.htmlDecode(playListName.trim());
        }
        final String fpName = getFormattedPackagename(usernName, playListName, date);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.setProperty(LinkCrawler.PACKAGE_IGNORE_VARIOUS, true);
        this.fp = fp;
    }

    public static String getString(Map<String, Object> data, String string) {
        Object newData = data;
        for (String s : string.split("\\.")) {
            newData = ((Map<String, Object>) newData).get(s);
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
        Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String user_id = getString(data, "id");
        if (user_id == null) {
            return;
        }
        setFilePackage(username, playlistname);
        int page = 1;
        int offset = 0;
        do {
            String next_page_url = "https://api.soundcloud.com/users/" + user_id + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + offset + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
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
        Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String user_id = getString(data, "id");
        if (StringUtils.isEmpty(user_id)) {
            return;
        }
        int page = 1;
        int offset = 0;
        setFilePackage(username, playlistname);
        do {
            String next_page_url = "https://api.soundcloud.com/tracks/" + user_id + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + page + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private void decryptGroups() throws Exception {
        url_username = new Regex(parameter, "/groups/(.+)").getMatch(0);
        resolve(parameter);
        data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String group_id = getString(data, "id");
        if (group_id == null) {
            decryptedLinks = null;
            return;
        }
        final long items_count = ((Number) data.get("track_count")).longValue();
        final long pages = items_count / max_entries_per_request;
        int current_page = 1;
        int offset = 0;
        setFilePackage(username, playlistname);
        do {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            final String next_page_url = "https://api.soundcloud.com/groups/" + group_id + "/tracks?app_version=" + SoundcloudCom.getAppVersion(br) + "&client_id=" + SoundcloudCom.getClientId(br) + "&limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=approved_at";
            br.getPage(next_page_url);
            final int pre = decryptedLinks.size();
            final List<Map<String, Object>> collection = parseCollection();
            if (decryptedLinks.size() != pre + max_entries_per_request) {
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                break;
            } else {
                offset += collection.size();
            }
            current_page++;
        } while (!this.isAbort());
        logger.info("Seems like we decrypted all likes-pages - stopping");
    }

    private Map<String, Object> data = null;

    public List<Map<String, Object>> parseCollection() throws Exception, DecrypterException, ParseException, IOException {
        data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> collection = (List<Map<String, Object>>) data.get("collection");
        return parseTracklist(collection);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseTracklist(List<Map<String, Object>> collection) throws Exception {
        for (final Map<String, Object> item : collection) {
            DownloadLink dl = null;
            final String type = getString(item, "type");
            Map<String, Object> entry = null;
            if (type == null) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("track".equals(type)) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("track_repost".equals(type) || "track-repost".equals(type)) {
                entry = (Map<String, Object>) item.get("track");
            } else if ("playlist-repost".equals(type) || "playlist_repost".equals(type)) {
                entry = (Map<String, Object>) item.get("playlist");
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
            final Object track_count = entry.get("track_count");
            if (permalink_url == null || url == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                continue;
            }
            if (type != null && type.equals("playlist")) {
                if (track_count != null && track_count instanceof Number && ((Number) track_count).intValue() == 0) {
                    continue;
                }
                dl = createDownloadlink("https://soundcloud.com/" + url_username + "/sets/" + url);
            } else if (type != null && type.equals("playlist-repost")) {
                if (track_count != null && track_count instanceof Number && ((Number) track_count).intValue() == 0) {
                    continue;
                }
                dl = createDownloadlink(permalink_url);
            } else {
                String track_url = permalink_url.replace("http://", "https://").replace("soundcloud.com", "soundclouddecrypted.com");
                if (!track_url.endsWith("/" + url)) {
                    track_url += "/" + url;
                }
                dl = createDownloadlink(track_url);
                dl = setDlDataJson(dl, entry);
                final Number duration = (Number) entry.get("duration");
                if (duration != null && duration.longValue() > 0) {
                    dl.setDownloadSize(128 / 8 * 1024 * duration.longValue() / 1000);
                }
                get500Thumbnail(dl, entry);
                getOriginalThumbnail(dl, entry);
            }
            addLink(dl);
        }
        return collection;
    }

    /** E.g. complete user profile, all liked tracks of a user, all reposted tracks of a user */
    private void decryptUser() throws Exception {
        resolve(null);
        /* Decrypt all tracks of a user */
        final Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        username = getString(data, "username");
        final String userID = getString(data, "id");
        String url_base;
        if (new Regex(parameter, TYPE_USER_REPOST).matches()) {
            /* Reposts of a user */
            url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID + "/reposts";
            playlistname = "reposts";
        } else if (new Regex(parameter, TYPE_USER_LIKES).matches()) {
            /* Likes of a user */
            url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/likes";
            playlistname = "likes";
        } else {
            /* Complete user profile */
            url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID;
            playlistname = "";
        }
        if (userID == null) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        // seems to be a limit of the API (12.02.14),
        // still valid far as I can see raztoki20160208
        int maxPerCall = 200;
        setFilePackage(username, playlistname);
        final UrlQuery query = new UrlQuery();
        query.append("client_id", SoundcloudCom.getClientId(br), true);
        query.append("linked_partitioning", "1", true);
        query.append("app_version", SoundcloudCom.getAppVersion(br), true);
        query.append("limit", maxPerCall + "", true);
        String next_href = null;
        String offset = "0";
        url_base += "?" + query.toString();
        int page = 1;
        do {
            logger.info("Crawling pagination page: " + page);
            br.getPage(url_base + "&offset=" + offset);
            final List<Map<String, Object>> collection = parseCollection();
            if (collection == null || collection.size() == 0) {
                break;
            }
            this.data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            next_href = (String) this.data.get("next_href");
            offset = new Regex(next_href, "offset=([^&]+)").getMatch(0);
        } while (!this.isAbort() && !StringUtils.isEmpty(next_href) && offset != null);
    }

    private boolean isList() throws DecrypterException {
        if (parameter == null) {
            throw new DecrypterException("parameter == null");
        } else if (parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?") || parameter.matches("[^?]+/groups/.*") || parameter.matches("[^?]+/sets.*")) {
            return true;
        } else if (TYPE_USER_LIKES.matcher(parameter).find() || new Regex(parameter, TYPE_USER_REPOST).matches()) {
            return true;
        } else if (TYPE_API_PLAYLIST.matcher(parameter).find()) {
            return true;
        } else if (parameter.matches(".*?soundcloud\\.com(/[A-Za-z\\-_0-9]+){2,3}/?(\\?.+)?")) {
            /* 2020-03-17: WTF */
            return false;
        } else {
            return true;
        }
    }

    private DownloadLink get500Thumbnail(final DownloadLink audiolink, final Map<String, Object> source) throws ParseException {
        DownloadLink thumb = null;
        if (decrypt500Thumb) {
            try {
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
                    final String formattedFilename = SoundcloudCom.getFormattedFilename(thumb);
                    thumb.setFinalFileName(formattedFilename);
                    thumb.setAvailable(true);
                    addLink(thumb);
                }
            } catch (final ParseException e) {
                logger.warning("Failed to find 500x500 thumbnail...");
            }
        }
        return thumb;
    }

    private DownloadLink getOriginalThumbnail(final DownloadLink audiolink, final Map<String, Object> source) throws ParseException {
        DownloadLink thumb = null;
        if (decryptOriginalThumb) {
            try {
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
                    final String formattedFilename = SoundcloudCom.getFormattedFilename(thumb);
                    thumb.setFinalFileName(formattedFilename);
                    thumb.setAvailable(true);
                    addLink(thumb);
                }
            } catch (final ParseException e) {
                logger.warning("Failed to find original thumbnail...");
            }
        }
        return thumb;
    }

    private void getPurchaseURL(final Map<String, Object> source) throws ParseException {
        if (this.decryptPurchaseURL) {
            try {
                final String purchase_url = getString(source, "purchase_url");
                if (!StringUtils.isEmpty(purchase_url)) {
                    logger.info("Found purchase_url");
                    addLink(createDownloadlink(purchase_url));
                } else {
                    logger.info("Failed to find purchase_url - probably doesn't exist");
                }
            } catch (final Throwable e) {
                logger.warning("Failed to find purchase_url...");
            }
        }
    }

    /**
     * Accesses the soundcloud resolve url which returns important information about the user-profile or url (if parameter != null).
     *
     * @throws Exception
     */
    private void resolve(final String forced_url) throws Exception {
        String resolveurl;
        if (forced_url != null) {
            resolveurl = forced_url;
        } else {
            resolveurl = "https://soundcloud.com/" + url_username;
        }
        br.getPage(SoundcloudCom.API_BASEv2 + "/resolve?url=" + Encoding.urlEncode(resolveurl) + "&_status_code_map%5B302%5D=10&_status_format=json&client_id=" + SoundcloudCom.getClientId(br));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\"404 \\- Not Found\"")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
    }

    private final static String defaultCustomPackagename = "*channelname* - *playlistname*";

    private String getFormattedPackagename(final String channelname, String playlistname, String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = defaultCustomPackagename;
        }
        /* Check for missing data */
        if (playlistname == null) {
            playlistname = "-";
        }
        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            try {
                final String userDefinedDateFormat = CFG.getStringProperty(CUSTOM_DATE);
                final SimpleDateFormat formatter;
                if (date.matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2} \\+\\d+")) {
                    formatter = new SimpleDateFormat("yyyy/MM/ddHH:mm:ss");
                } else if (date.contains("/")) {
                    formatter = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss Z");
                } else {
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                }
                final Date dateStr = formatter.parse(date);
                formattedDate = formatter.format(dateStr);
                final String defaultformattedDate = formattedDate;
                if (userDefinedDateFormat != null) {
                    try {
                        final SimpleDateFormat customFormatter = new SimpleDateFormat(userDefinedDateFormat);
                        formattedDate = customFormatter.format(dateStr);
                    } catch (final Exception e) {
                        // prevent user error killing plugin.
                        formattedDate = defaultformattedDate;
                    }
                }
            } catch (final Exception e) {
                // prevent user error killing plugin.
                formattedDate = null;
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

    private DownloadLink setDlDataJson(final DownloadLink dl, final Map<String, Object> data) throws Exception {
        dl.setProperty("plain_url_username", url_username);
        if (data != null) {
            final AvailableStatus status = SoundcloudCom.checkStatusJson(this, dl, data, false);
            dl.setAvailableStatus(status);
        }
        return dl;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}