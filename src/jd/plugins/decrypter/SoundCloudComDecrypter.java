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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.SoundcloudCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://((?:www\\.|m\\.)?(soundcloud\\.com/[^<>\"\\']+(?:\\?format=html\\&page=\\d+|\\?page=\\d+)?|snd\\.sc/[A-Za-z0-9]+)|api\\.soundcloud\\.com/tracks/\\d+(?:\\?secret_token=[A-Za-z0-9\\-_]+)?|api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?\\&)secret_token=[A-Za-z0-9\\-_]+)" })
public class SoundCloudComDecrypter extends PluginForDecrypt {
    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String            TYPE_INVALID                  = "https?://[^/]+/(you|tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|#?search|upload|people|dashboard|#/)($|/.*?)";
    private final Pattern           TYPE_API_PLAYLIST             = Pattern.compile("https?://(www\\.|m\\.)?api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?&)secret_token=[A-Za-z0-9\\-_]+");
    private final Pattern           TYPE_API_TRACK                = Pattern.compile("https?://(www\\.|m\\.)?api\\.soundcloud\\.com/tracks/\\d+(\\?secret_token=[A-Za-z0-9\\-_]+)?");
    private final Pattern           TYPE_SINGLE_SET               = Pattern.compile("https?://[^/]+/[A-Za-z0-9\\-_]+/sets/[A-Za-z0-9\\-_]+");
    private final Pattern           TYPE_USER_SETS                = Pattern.compile("https?://[^/]+/[A-Za-z0-9\\-_]+/sets");
    private final Pattern           TYPE_USER_IN_PLAYLIST         = Pattern.compile("https?://[^/]+/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)/sets");
    private final Pattern           TYPE_USER_LIKES               = Pattern.compile("https?://[^/]+/[A-Za-z0-9\\-_]+/likes");
    private final Pattern           TYPE_USER_TRACKS              = Pattern.compile("https?://[^/]+/[A-Za-z0-9\\-_]+/tracks");
    private final Pattern           TYPE_USER_REPOST              = Pattern.compile("https?://[^/]+/[A-Za-z0-9\\-_]+/repost");
    private final Pattern           TYPE_GROUPS                   = Pattern.compile("https?://[^/]+/groups/([A-Za-z0-9\\-_]+)");
    /* Single soundcloud tracks, posted via smartphone/app. */
    private final String            subtype_mobile_facebook_share = "https?://[^/]+/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+\\?fb_action_ids=.+";
    private final Pattern           TYPE_SHORT                    = Pattern.compile("https?://snd\\.sc/[A-Za-z0-9]+");
    /* Settings */
    private final String            GRAB_PURCHASE_URL             = "GRAB_PURCHASE_URL";
    private final String            GRAB500THUMB                  = "GRAB500THUMB";
    private final String            GRABORIGINALTHUMB             = "GRABORIGINALTHUMB";
    private final String            CUSTOM_PACKAGENAME            = "CUSTOM_PACKAGENAME";
    private final String            CUSTOM_DATE                   = "CUSTOM_DATE";
    private SubConfiguration        CFG                           = null;
    private String                  parameter                     = null;
    private ArrayList<DownloadLink> decryptedLinks                = null;
    private FilePackage             fp                            = null;
    private boolean                 decryptPurchaseURL            = false;
    private boolean                 decrypt500Thumb               = false;
    private boolean                 decryptOriginalThumb          = false;
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
        this.decryptedLinks = decryptedLinks;
        CFG = SubConfiguration.getConfig(this.getHost());
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
            final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.SoundcloudCom) hostPlugin).login(this.br, accs.get(0), false);
        }
        /* Correct added links */
        correctInputLinks(param);
        url_username = new Regex(parameter, "soundcloud\\.com/([^<>\"/]+)/?").getMatch(0);
        br.setFollowRedirects(true);
        final boolean decryptList = isList(param.getCryptedUrl());
        if (decryptList) {
            if (TYPE_SINGLE_SET.matcher(parameter).find() || TYPE_API_PLAYLIST.matcher(parameter).find()) {
                crawlSet(param);
            } else if (TYPE_USER_SETS.matcher(parameter).find()) {
                crawlUserSets(param);
            } else if (TYPE_USER_IN_PLAYLIST.matcher(parameter).find()) {
                crawlUserInPlaylists(param);
            } else if (TYPE_GROUPS.matcher(parameter).find()) {
                crawlGroups(param);
            } else {
                crawlUser(param);
            }
        } else {
            /* If the user wants to download the thumbnail as well it's a bit more complicated */
            resolve(parameter);
            /* Add soundcloud link */
            final DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
            final Map<String, Object> track = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> user = (Map<String, Object>) track.get("user");
            final String username = (String) user.get("username");
            if (playlistname == null) {
                playlistname = "";
            }
            setFilePackage(username, playlistname);
            setDlDataJson(dl, track);
            addLink(dl);
            try {
                if (decryptPurchaseURL) {
                    crawlPurchaseURL(track);
                }
                if (decrypt500Thumb) {
                    get500Thumbnail(dl, track);
                }
                if (decryptOriginalThumb) {
                    getOriginalThumbnail(dl, track);
                }
            } catch (final Exception e) {
                logger.log(e);
                logger.info("Failed to get thumbnail/purchase_url, adding song link only");
            }
        }
        return decryptedLinks;
    }

    private void correctInputLinks(final CryptedLink param) throws Exception {
        String newurl = Encoding.htmlDecode(param.getCryptedUrl());
        if (newurl.contains("#")) {
            newurl = newurl.substring(0, newurl.indexOf("#"));
        }
        param.setCryptedUrl(newurl);
        parameter = newurl.replace("http://", "https://").replaceAll("(/download|\\\\)", "").replaceFirst("://(www|m)\\.", "://");
        if (parameter.matches(TYPE_INVALID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (TYPE_SHORT.matcher(parameter).find()) {
            br.setFollowRedirects(false);
            /* Use ORIGINAL_LINK because https is not available for short links */
            br.getPage(parameter);
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
            final String secret_token = new UrlQuery().parse(parameter).get("secret_token");
            if (secret_token != null) {
                query.add("secret_token", secret_token);
            }
            br.getPage(parameter + "?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
    private void crawlSet(final CryptedLink param) throws Exception {
        String secret_token = null;
        if (parameter.matches("https?://[^/]+/[^/]+/sets/[^/]+/s-[A-Za-z0-9]+")) {
            /* Private set --> URL contains so called 'secret_token' */
            secret_token = new Regex(parameter, "/(s-.+)$").getMatch(0);
        } else if (TYPE_API_PLAYLIST.matcher(parameter).find()) {
            secret_token = new UrlQuery().parse(parameter).get("secret_token");
        }
        resolve(this.br, parameter);
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> user = (Map<String, Object>) entries.get("user");
        playlistname = (String) entries.get("title");
        final String username = (String) user.get("username");
        /* Old: https://api.soundcloud.com/playlists/<playlist_id> */
        final String permalink_url = (String) entries.get("permalink_url");
        final String playlist_uri = (String) entries.get("uri");
        final String playlist_id = entries.get("id").toString();
        if (playlist_uri == null || playlist_id == null) {
            return;
        }
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
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) entries.get("tracks");
        if (tracks == null || tracks.size() == 0 || usernameOfSet == null) {
            if (entries.get("duration").toString().equals("0")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (tracks != null && tracks.size() == 0) {
                logger.info("Probably GEO-Blocked");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
            final String track_id = item.get("id").toString();
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
                    final String unsortedTrackID = tempTrackData.get("id").toString();
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
            entries = (Map<String, Object>) tracko;
            final Map<String, Object> trackUser = (Map<String, Object>) entries.get("user");
            final String permalink = (String) entries.get("permalink");
            if (StringUtils.isEmpty(permalink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + trackUser.get("permalink") + "/" + permalink);
            dl.setProperty(SoundcloudCom.PROPERTY_setsposition, counter + ".");
            dl.setProperty(SoundcloudCom.PROPERTY_playlist_id, playlist_id);
            setDlDataJson(dl, entries);
            if (!StringUtils.isEmpty(secret_token) && !StringUtils.isEmpty(permalink_url)) {
                /*
                 * 2020-03-11: 'Secret'/private playlists do not have Content-URLs which go to their individual tracks(?) --> Set URL of the
                 * set they are in as contentURL.
                 */
                dl.setContentUrl(permalink_url);
            }
            if (secret_token != null) {
                dl.setProperty(SoundcloudCom.PROPERTY_secret_token, secret_token);
            }
            addLink(dl);
            get500Thumbnail(dl, entries);
            getOriginalThumbnail(dl, entries);
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
            playListName = usernName;
        } else {
            playListName = Encoding.htmlDecode(playListName.trim());
        }
        final String fpName = getFormattedPackagename(usernName, playListName, date);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.setProperty(LinkCrawler.PACKAGE_IGNORE_VARIOUS, true);
        this.fp = fp;
    }

    /**
     * Decrypts all sets of a user
     *
     * @throws Exception
     */
    private void crawlUserSets(final CryptedLink param) throws Exception {
        final int max_entries_per_request = 5;
        final String username = new Regex(parameter, "soundcloud\\.com/([^<>\"/]+)/?").getMatch(0);
        resolve(null);
        Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String userID = data.get("id").toString();
        if (StringUtils.isEmpty(userID)) {
            logger.info("Failed to find userID");
            return;
        }
        setFilePackage(username, playlistname);
        int page = 1;
        int offset = 0;
        do {
            logger.info("Crawling page: " + page);
            final String next_page_url = "https://api.soundcloud.com/users/" + userID + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + offset + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (final Map<String, Object> playlist : collection) {
                final List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                logger.info("Seems like we decrypted all likes-pages - stopping");
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
    }

    /**
     * Decrypts all sets (playlists) of a users' category 'In Playlists''
     *
     * @throws Exception
     */
    private void crawlUserInPlaylists(final CryptedLink param) throws Exception {
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_IN_PLAYLIST).getMatch(0);
        final int max_entries_per_request = 5;
        resolve(parameter);
        Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String userID = data.get("id").toString();
        if (StringUtils.isEmpty(userID)) {
            return;
        }
        int page = 1;
        int offset = 0;
        setFilePackage(username, playlistname);
        do {
            String next_page_url = "https://api.soundcloud.com/tracks/" + userID + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + page + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                final List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                parseTracklist(tracklist);
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                logger.info("Seems like we decrypted all likes-pages - stopping");
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
    }

    private void crawlGroups(final CryptedLink param) throws Exception {
        final String username = new Regex(parameter, TYPE_GROUPS).getMatch(0);
        resolve(parameter);
        final Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String groupID = data.get("id").toString();
        if (StringUtils.isEmpty(groupID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final long items_count = ((Number) data.get("track_count")).longValue();
        final int max_entries_per_request = 100;
        final long pages = items_count / max_entries_per_request;
        int current_page = 1;
        int offset = 0;
        setFilePackage(username, playlistname);
        do {
            logger.info("Decrypting page " + current_page + " of probably " + pages);
            final String next_page_url = "https://api.soundcloud.com/groups/" + groupID + "/tracks?app_version=" + SoundcloudCom.getAppVersion(br) + "&client_id=" + SoundcloudCom.getClientId(br) + "&limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=approved_at";
            br.getPage(next_page_url);
            final int pre = decryptedLinks.size();
            final List<Map<String, Object>> collection = getCollection(this.br);
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
    // private Map<String, Object> data = null;

    public List<Map<String, Object>> getCollection(final Browser br) throws Exception, DecrypterException, ParseException, IOException {
        final Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> collection = (List<Map<String, Object>>) data.get("collection");
        return parseTracklist(collection);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseTracklist(List<Map<String, Object>> collection) throws Exception {
        for (final Map<String, Object> item : collection) {
            DownloadLink dl = null;
            final String type = (String) item.get("type");
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
            final String permalink_url = (String) entry.get("permalink_url");
            final String url = (String) entry.get("permalink");
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
                setDlDataJson(dl, entry);
                get500Thumbnail(dl, entry);
                getOriginalThumbnail(dl, entry);
            }
            addLink(dl);
        }
        return collection;
    }

    /** E.g. complete user profile, all liked tracks of a user, all reposted tracks of a user */
    private void crawlUser(final CryptedLink param) throws Exception {
        resolve(null);
        /* Decrypt all tracks of a user */
        Map<String, Object> user = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String username = (String) user.get("username");
        final String userID = user.get("id").toString();
        if (StringUtils.isEmpty(userID) || StringUtils.isEmpty(username)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String url_base;
        if (new Regex(parameter, TYPE_USER_REPOST).matches()) {
            /* Reposts of a user */
            url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID + "/reposts";
            playlistname = "reposts";
        } else if (new Regex(parameter, TYPE_USER_LIKES).matches()) {
            /* Likes of a user */
            url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/likes";
            playlistname = "likes";
        } else if (new Regex(parameter, TYPE_USER_TRACKS).matches()) {
            /* Tracks of a user */
            url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/tracks";
            playlistname = "tracks";
        } else {
            /* Complete user profile */
            url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID;
            playlistname = "";
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
            final List<Map<String, Object>> collection = getCollection(this.br);
            if (collection == null || collection.size() == 0) {
                break;
            }
            user = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            next_href = (String) user.get("next_href");
            offset = new Regex(next_href, "offset=([^&]+)").getMatch(0);
        } while (!this.isAbort() && !StringUtils.isEmpty(next_href) && offset != null);
    }

    private boolean isList(final String url) throws DecrypterException {
        if (url == null) {
            throw new DecrypterException("parameter == null");
        } else if (url.matches("(?i).*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?") || url.matches("(?i)[^?]+/groups/.*") || url.matches("(?i)[^?]+/sets.*")) {
            return true;
        } else if (TYPE_USER_LIKES.matcher(url).find() || new Regex(url, TYPE_USER_REPOST).matches() || new Regex(url, TYPE_USER_TRACKS).matches()) {
            return true;
        } else if (TYPE_API_PLAYLIST.matcher(url).find()) {
            return true;
        } else if (url.matches("(?i).*?soundcloud\\.com(/[A-Za-z\\-_0-9]+){2,3}/?(\\?.+)?")) {
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
                String artworkurl = (String) source.get("artwork_url");
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
                String artworkurl = (String) source.get("artwork_url");
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

    private void crawlPurchaseURL(final Map<String, Object> track) throws ParseException {
        if (this.decryptPurchaseURL) {
            try {
                final String purchase_url = (String) track.get("purchase_url");
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
    @Deprecated
    private void resolve(final String url) throws Exception {
        String resolveurl;
        if (url != null) {
            resolveurl = url;
        } else {
            resolveurl = "https://soundcloud.com/" + url_username;
        }
        br.getPage(SoundcloudCom.API_BASEv2 + "/resolve?url=" + Encoding.urlEncode(resolveurl) + "&_status_code_map%5B302%5D=10&_status_format=json&client_id=" + SoundcloudCom.getClientId(br));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"404 \\- Not Found\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void resolve(final Browser br, final String url) throws Exception {
        br.getPage(SoundcloudCom.API_BASEv2 + "/resolve?url=" + Encoding.urlEncode(url) + "&_status_code_map%5B302%5D=10&_status_format=json&client_id=" + SoundcloudCom.getClientId(br));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"404 \\- Not Found\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    private void setDlDataJson(final DownloadLink dl, final Map<String, Object> data) throws Exception {
        if (data != null) {
            final AvailableStatus status = SoundcloudCom.checkStatusJson(dl, data);
            dl.setAvailableStatus(status);
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}