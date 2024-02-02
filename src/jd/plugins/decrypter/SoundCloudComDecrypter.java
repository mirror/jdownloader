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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://((?:www\\.|m\\.)?soundcloud\\.com/[^<>\"\\']+(?:\\?format=html\\&page=\\d+|\\?page=\\d+)?|api\\.soundcloud\\.com/tracks/\\d+(?:\\?secret_token=[A-Za-z0-9\\-_]+)?|api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?\\&)secret_token=[A-Za-z0-9\\-_]+)" })
public class SoundCloudComDecrypter extends PluginForDecrypt {
    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private final Pattern    PATTERN_INVALID               = Pattern.compile("(?i)https?://[^/]+/(tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|#?search|upload|people|dashboard|#/)($|/.*?)");
    private final Pattern    TYPE_API_PLAYLIST             = Pattern.compile("(?i)https?://(www\\.|m\\.)?api\\.soundcloud\\.com/playlists/\\d+(?:\\?|.*?&)secret_token=[A-Za-z0-9\\-_]+");
    private final Pattern    TYPE_API_TRACK                = Pattern.compile("(?i)https?://(www\\.|m\\.)?api\\.soundcloud\\.com/tracks/\\d+(\\?secret_token=[A-Za-z0-9\\-_]+)?");
    private final Pattern    TYPE_SINGLE_SET               = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/sets/([A-Za-z0-9\\-_]+)");
    private final Pattern    TYPE_SINGLE_TRACK             = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)");
    private final Pattern    TYPE_USER_SETS                = Pattern.compile("(?i)https?://[^/]+/(?!you/)([A-Za-z0-9\\-_]+)/sets");
    private final Pattern    TYPE_USER_IN_PLAYLIST         = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)/sets");
    private final Pattern    TYPE_USER_LIKES               = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/likes");
    private final Pattern    TYPE_USER_LIKES_SELF          = Pattern.compile("(?i)https?://[^/]+/you/likes");
    private final Pattern    TYPE_USER_LIKES_PLAYLIST_SELF = Pattern.compile("(?i)https?://[^/]+/you/sets");
    private final Pattern    TYPE_USER_TRACKS              = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/tracks");
    private final Pattern    TYPE_USER_REPOST              = Pattern.compile("(?i)https?://[^/]+/([A-Za-z0-9\\-_]+)/repost");
    private final Pattern    TYPE_GROUPS                   = Pattern.compile("(?i)https?://[^/]+/groups/([A-Za-z0-9\\-_]+)");
    /* Single soundcloud tracks, posted via smartphone/app. */
    private final String     subtype_mobile_facebook_share = "(?i)https?://[^/]+/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+\\?fb_action_ids=.+";
    /* Settings */
    private final String     GRAB_PURCHASE_URL             = "GRAB_PURCHASE_URL";
    private final String     GRAB500THUMB                  = "GRAB500THUMB";
    private final String     GRABORIGINALTHUMB             = "GRABORIGINALTHUMB";
    private final String     CUSTOM_PACKAGENAME            = "CUSTOM_PACKAGENAME";
    private final String     CUSTOM_DATE                   = "CUSTOM_DATE";
    private SubConfiguration cfg                           = null;
    private boolean          crawlPurchaseURL              = false;
    private boolean          crawl500Thumb                 = false;
    private boolean          crawlOriginalThumb            = false;

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private String getContentURL(final CryptedLink param) throws PluginException {
        String url = param.getCryptedUrl();
        url = url.replaceFirst("#.+", "");// remove anchor
        url = url.replaceFirst("/$", "");// remove trailing slash
        url = url.replaceFirst("(?i)http://", "https://");
        url = url.replaceAll("(?i)(/download|\\\\)", "").replaceFirst("://(www|m)\\.", "://");
        if (url.matches(subtype_mobile_facebook_share)) {
            final String urlDecoded = Encoding.htmlDecode(url);
            url = "https://soundcloud.com/" + new Regex(urlDecoded, "(?i)soundcloud\\.com/([A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)").getMatch(0);
        }
        return url;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (new Regex(param.getCryptedUrl(), PATTERN_INVALID).patternFind()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        cfg = SubConfiguration.getConfig(this.getHost());
        crawlPurchaseURL = cfg.getBooleanProperty(GRAB_PURCHASE_URL, SoundcloudCom.defaultGRAB_PURCHASE_URL);
        crawl500Thumb = cfg.getBooleanProperty(GRAB500THUMB, SoundcloudCom.defaultGRAB500THUMB);
        crawlOriginalThumb = cfg.getBooleanProperty(GRABORIGINALTHUMB, SoundcloudCom.defaultGRABORIGINALTHUMB);
        /* They can have huge pages, allow eight times the normal load limit */
        /* Login whenever possible, helps to get links which need the user to be logged in e.g. users' own favorites. */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.SoundcloudCom) hostPlugin).login(this.br, acc, false);
        }
        final String contenturl = getContentURL(param);
        if (isList(contenturl)) {
            if (TYPE_SINGLE_SET.matcher(contenturl).find() || TYPE_API_PLAYLIST.matcher(contenturl).find()) {
                return crawlSet(contenturl);
            } else if (TYPE_USER_SETS.matcher(contenturl).find()) {
                return crawlUserSets(contenturl);
            } else if (TYPE_USER_IN_PLAYLIST.matcher(contenturl).find()) {
                return crawlUserInPlaylists(param);
            } else if (TYPE_GROUPS.matcher(contenturl).find()) {
                return crawlGroups(contenturl);
            } else {
                return crawlUser(contenturl, acc);
            }
        } else {
            /* Single track */
            return crawlSingleTrack(contenturl);
        }
    }

    private ArrayList<DownloadLink> crawlSingleTrack(final String contenturl) throws Exception {
        resolve(this.br, contenturl);
        final Map<String, Object> trackmap = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> user = (Map<String, Object>) trackmap.get("user");
        final String usernameSlug = user.get("permalink").toString();
        final String username = user.get("username").toString();
        final ArrayList<DownloadLink> ret = crawlProcessSingleTrack(trackmap);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(getFormattedPackagename(usernameSlug, username, null, trackmap.get("created_at").toString()));
        fp.setIgnoreVarious(true);
        fp.addLinks(ret);
        return ret;
    }

    private ArrayList<DownloadLink> crawlProcessSingleTrack(final Map<String, Object> track) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String permalink_url = track.get("permalink_url").toString();
        final DownloadLink dl = createDownloadlink(permalink_url.replace("soundcloud", "soundclouddecrypted"));
        parseFileInfo(dl, track);
        ret.add(dl);
        if (crawlPurchaseURL) {
            final DownloadLink purchaseurl = crawlPurchaseURL(track);
            if (purchaseurl != null) {
                ret.add(purchaseurl);
            }
        }
        if (crawl500Thumb) {
            final DownloadLink thumbnail500 = get500Thumbnail(dl, track);
            if (thumbnail500 != null) {
                ret.add(thumbnail500);
            }
        }
        if (crawlOriginalThumb) {
            final DownloadLink thumbnailOriginal = getOriginalThumbnail(dl, track);
            if (thumbnailOriginal != null) {
                ret.add(thumbnailOriginal);
            }
        }
        return ret;
    }

    /**
     * Crawls all tracks of a single set/playlist
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlSet(final String contenturl) throws Exception {
        String playlistSecretToken = null;
        final String setWithSecretToken = "(?i)https?://[^/]+/[^/]+/sets/[^/]+/s-([A-Za-z0-9]+)$";
        if (contenturl.matches(setWithSecretToken)) {
            /* Private set --> URL contains so called 'secret_token' */
            playlistSecretToken = new Regex(contenturl, setWithSecretToken).getMatch(0);
        } else if (TYPE_API_PLAYLIST.matcher(contenturl).find()) {
            playlistSecretToken = UrlQuery.parse(contenturl).get("secret_token");
        }
        resolve(this.br, contenturl);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> user = (Map<String, Object>) entries.get("user");
        final String playlistname = (String) entries.get("title");
        final String playlist_uri = (String) entries.get("uri");
        final String playlistID = entries.get("id").toString();
        final String created_at = (String) entries.get("created_at");
        if (StringUtils.isEmpty(playlist_uri) || StringUtils.isEmpty(playlistID) || StringUtils.isEmpty(created_at)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery queryplaylist = new UrlQuery();
        queryplaylist.add("representation", "full");
        queryplaylist.add("client_id", SoundcloudCom.getClientId(br));
        queryplaylist.add("app_version", SoundcloudCom.getAppVersion(br));
        queryplaylist.add("format", "json");
        if (playlistSecretToken != null) {
            queryplaylist.add("secret_token", playlistSecretToken);
        }
        br.getPage(SoundcloudCom.API_BASEv2 + "/playlists/" + playlistID + "?" + queryplaylist.toString());
        final Map<String, Object> playlistmap = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) playlistmap.get("tracks");
        if (tracks == null || tracks.size() == 0) {
            if (playlistmap.get("duration").toString().equals("0")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (tracks != null && tracks.size() == 0) {
                logger.info("Probably GEO-Blocked");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(this.getFormattedPackagename(user.get("permalink").toString(), user.get("username").toString(), playlistname, created_at));
        fp.setIgnoreVarious(true);
        final List<Map<String, Object>> trackItemsFound = new ArrayList<Map<String, Object>>();
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
            logger.info("Handling set pagination for " + trackIdsForPagination.size() + " objects");
            final StringBuilder sb = new StringBuilder();
            final ArrayList<String> tempIDs = new ArrayList<String>();
            /* Collect all found items here because they're not returning them in the requested order. */
            final List<Map<String, Object>> unsortedTempTrackItemsFound = new ArrayList<Map<String, Object>>();
            int index = 0;
            int paginationPage = 0;
            while (!this.isAbort()) {
                logger.info("Pagination page" + (paginationPage + 1));
                tempIDs.clear();
                while (true) {
                    /* 2020-09-23: We request max. 50 IDs at once. */
                    if (index == trackIdsForPagination.size() || tempIDs.size() == 50) {
                        logger.info("Stopping because: Page contains less items than full page");
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
                querytracks.add("playlistId", playlistID);
                querytracks.add("ids", sb.toString());
                querytracks.add("client_id", SoundcloudCom.getClientId(br));
                querytracks.add("app_version", SoundcloudCom.getAppVersion(br));
                querytracks.add("format", "json");
                if (playlistSecretToken != null) {
                    querytracks.add("playlistSecretToken", playlistSecretToken);
                }
                logger.info("Found items: " + index + " / " + trackIdsForPagination.size());
                br.getPage(SoundcloudCom.API_BASEv2 + "/tracks?" + querytracks.toString());
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
                for (final Map<String, Object> trackmap : ressourcelist) {
                    unsortedTempTrackItemsFound.add(trackmap);
                }
                if (index == trackIdsForPagination.size()) {
                    logger.info("Stopping because: Pagination has reached end");
                    break;
                }
                paginationPage++;
            }
            /* Be sure to add new items sorted to main Array - basically just to get PROPERTY_setsposition right! */
            for (final String sortedTrackID : trackIdsForPagination) {
                boolean foundTrackID = false;
                for (final Map<String, Object> tempTrackData : unsortedTempTrackItemsFound) {
                    final String unsortedTrackID = tempTrackData.get("id").toString();
                    if (unsortedTrackID.equals(sortedTrackID)) {
                        /* Found item --> Add it to main Array - it is now in order. */
                        trackItemsFound.add(tempTrackData);
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
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int counter = 1;
        for (final Map<String, Object> trackmap : trackItemsFound) {
            final ArrayList<DownloadLink> results = this.crawlProcessSingleTrack(trackmap);
            for (final DownloadLink result : results) {
                result.setProperty(SoundcloudCom.PROPERTY_setsposition, counter + ".");
                result.setProperty(SoundcloudCom.PROPERTY_playlist_id, playlistID);
                result._setFilePackage(fp);
                ret.add(result);
            }
            counter++;
        }
        return ret;
    }

    /**
     * Decrypts all sets of a user
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlUserSets(final String contenturl) throws Exception {
        final int max_entries_per_request = 5;
        resolve(this.br, contenturl);
        Map<String, Object> user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String userID = user.get("id").toString();
        if (StringUtils.isEmpty(userID)) {
            logger.info("Failed to find userID");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(getFormattedPackagename(user.get("permalink").toString(), user.get("username").toString(), "sets", null));
        fp.setIgnoreVarious(true);
        int page = 1;
        int offset = 0;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            logger.info("Crawling page: " + page);
            final String next_page_url = "https://api.soundcloud.com/users/" + userID + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + offset + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            final Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (final Map<String, Object> playlist : collection) {
                final List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                final ArrayList<DownloadLink> results = parseTracklist(fp, tracklist);
                for (final DownloadLink result : results) {
                    ret.add(result);
                    distribute(result);
                }
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                logger.info("Seems like we crawled all likes-pages - stopping");
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
        return ret;
    }

    /**
     * Decrypts all sets (playlists) of a users' category 'In Playlists''
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlUserInPlaylists(final CryptedLink param) throws Exception {
        final int max_entries_per_request = 5;
        resolve(this.br, param.getCryptedUrl());
        Map<String, Object> user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String userID = user.get("id").toString();
        if (StringUtils.isEmpty(userID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        int page = 1;
        int offset = 0;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(getFormattedPackagename(user.get("permalink").toString(), user.get("username").toString(), "playlists", null));
        fp.setIgnoreVarious(true);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            logger.info("Crawling page: " + page);
            String next_page_url = "https://api.soundcloud.com/tracks/" + userID + "/playlists?limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=favorited_at&page_number=" + page + "&page_size=" + max_entries_per_request + "&client_id=" + SoundcloudCom.getClientId(br);
            br.getPage(next_page_url);
            final Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> collection = (List<Map<String, Object>>) response.get("collection");
            for (Map<String, Object> playlist : collection) {
                final List<Map<String, Object>> tracklist = (List<Map<String, Object>>) playlist.get("tracks");
                final ArrayList<DownloadLink> results = parseTracklist(fp, tracklist);
                for (final DownloadLink result : results) {
                    ret.add(result);
                    distribute(result);
                }
            }
            if (collection == null || collection.size() < max_entries_per_request) {
                logger.info("Stopping because: Seems like we crawled all likes-pages");
                break;
            }
            page++;
            offset += collection.size();
        } while (!this.isAbort());
        return ret;
    }

    /** Crawl all tracks of a group. */
    private ArrayList<DownloadLink> crawlGroups(final String contenturl) throws Exception {
        final String usernameURL = new Regex(contenturl, TYPE_GROUPS).getMatch(0);
        if (usernameURL == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        resolve(this.br, contenturl);
        final Map<String, Object> data = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String groupID = data.get("id").toString();
        if (StringUtils.isEmpty(groupID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final long items_count = ((Number) data.get("track_count")).longValue();
        final int max_entries_per_request = 100;
        final long pages = items_count / max_entries_per_request;
        int currentPage = 1;
        int offset = 0;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(getFormattedPackagename(usernameURL, null, "groups", null));
        fp.setIgnoreVarious(true);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            logger.info("Crawling page " + currentPage + " of probably " + pages);
            final String next_page_url = "https://api.soundcloud.com/groups/" + groupID + "/tracks?app_version=" + SoundcloudCom.getAppVersion(br) + "&client_id=" + SoundcloudCom.getClientId(br) + "&limit=" + max_entries_per_request + "&linked_partitioning=1&offset=" + offset + "&order=approved_at";
            br.getPage(next_page_url);
            final int pre = ret.size();
            final ArrayList<DownloadLink> collection = processCollection(fp, this.br);
            if (ret.size() != pre + max_entries_per_request) {
                logger.warning("Crawled items mismatch");
                break;
            }
            for (final DownloadLink result : collection) {
                distribute(result);
                ret.add(result);
            }
            if (collection.size() < max_entries_per_request) {
                logger.info("Stopping because: Current page contains less items than " + max_entries_per_request);
                break;
            } else {
                /* Continue to next page */
                offset += collection.size();
                currentPage++;
            }
        } while (!this.isAbort());
        logger.info("Seems like we crawled all group-tracks - stopping");
        return ret;
    }

    protected ArrayList<DownloadLink> processCollection(final FilePackage fp, final Browser br) throws Exception, DecrypterException, ParseException, IOException {
        final Map<String, Object> data = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> collection = (List<Map<String, Object>>) data.get("collection");
        return parseTracklist(fp, collection);
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<DownloadLink> parseTracklist(final FilePackage fp, final List<Map<String, Object>> collection) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> item : collection) {
            final Map<String, Object> trackmap = (Map<String, Object>) item.get("track");
            final Map<String, Object> playlist = (Map<String, Object>) item.get("playlist");
            final String type = (String) item.get("type");
            // final String kind = (String) item.get("kind");
            // final Object track_count = entry.get("track_count");
            if (playlist != null) {
                final String url = playlist.get("permalink").toString();
                final Map<String, Object> user = (Map<String, Object>) playlist.get("user");
                /* Goes back into crawler to find individual playlist tracks. */
                ret.add(createDownloadlink("https://" + this.getHost() + "/" + user.get("permalink") + "/sets/" + url));
            } else if (type != null && type.equals("playlist-repost")) {
                /* Goes back into crawler to find individual playlist tracks. */
                // TODO: 2023-11-09: Check if this still exists
                final String permalinkURL = item.get("permalink_url").toString();
                ret.add(createDownloadlink(permalinkURL));
            } else {
                ret.addAll(this.crawlProcessSingleTrack(trackmap));
            }
        }
        return ret;
    }

    /**
     * Crawls collections in context of accounts e.g. complete user profile, all liked tracks of a user, all reposted tracks of a user, all
     * own likes of current user.
     */
    private ArrayList<DownloadLink> crawlUser(final String contenturl, final Account account) throws Exception {
        final String userID;
        String url_base;
        final String playlistname;
        String packagename = null;
        Map<String, Object> user = null;
        if (new Regex(contenturl, TYPE_USER_LIKES_SELF).patternFind()) {
            if (account == null) {
                throw new AccountRequiredException("Cannot crawl own liked tracks when no account is available");
            }
            userID = account.getStringProperty(SoundcloudCom.PROPERTY_ACCOUNT_userid);
            if (userID == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/track_likes";
            playlistname = "own liked tracks";
            packagename = getFormattedPackagename(account.getStringProperty(SoundcloudCom.PROPERTY_ACCOUNT_permalink), account.getStringProperty(SoundcloudCom.PROPERTY_ACCOUNT_username), playlistname, account.getStringProperty(SoundcloudCom.PROPERTY_ACCOUNT_created_at));
        } else if (new Regex(contenturl, TYPE_USER_LIKES_PLAYLIST_SELF).patternFind()) {
            if (account == null) {
                throw new AccountRequiredException("Cannot crawl own liked playlists when no account is available");
            }
            userID = account.getStringProperty(SoundcloudCom.PROPERTY_ACCOUNT_userid);
            if (userID == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/playlist_likes";
            playlistname = "own liked playlists";
            /*
             * Do not set packagename here because playlists will get processed again -> Go through crawler again and get custom
             * packagenames.
             */
        } else {
            resolve(this.br, contenturl);
            user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            userID = user.get("id").toString();
            if (StringUtils.isEmpty(userID)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (new Regex(contenturl, TYPE_USER_REPOST).patternFind()) {
                /* Reposts of a user */
                url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID + "/reposts";
                playlistname = "reposts";
            } else if (new Regex(contenturl, TYPE_USER_LIKES).patternFind()) {
                /* Likes of a user */
                url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/likes";
                playlistname = "likes";
            } else if (new Regex(contenturl, TYPE_USER_TRACKS).patternFind()) {
                /* Tracks of a user */
                url_base = SoundcloudCom.API_BASEv2 + "/users/" + userID + "/tracks";
                playlistname = "tracks";
            } else {
                /* Complete user profile */
                url_base = SoundcloudCom.API_BASEv2 + "/stream/users/" + userID;
                playlistname = "";
            }
            packagename = getFormattedPackagename(user.get("permalink").toString(), user.get("username").toString(), playlistname, user.get("created_at").toString());
        }
        /**
         * seems to be a limit of the API (12.02.14), </br> still valid far as I can see raztoki20160208
         */
        final int maxItemsPerCall = 200;
        FilePackage fp = null;
        if (packagename != null) {
            fp = FilePackage.getInstance();
            fp.setName(packagename);
            fp.setIgnoreVarious(true);
        }
        final UrlQuery query = new UrlQuery();
        query.append("client_id", SoundcloudCom.getClientId(br), true);
        query.append("linked_partitioning", "1", true);
        query.append("app_version", SoundcloudCom.getAppVersion(br), true);
        query.append("limit", maxItemsPerCall + "", true);
        String next_href = null;
        String offset = "0";
        url_base += "?" + query.toString();
        int page = 1;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            logger.info("Crawling pagination page: " + page + " | offset: " + offset);
            br.getPage(url_base + "&offset=" + offset);
            final ArrayList<DownloadLink> collection = processCollection(fp, this.br);
            if (collection == null || collection.size() == 0) {
                logger.info("Stopping because: Reached end");
                break;
            }
            for (final DownloadLink result : collection) {
                distribute(result);
                ret.add(result);
            }
            user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            next_href = (String) user.get("next_href");
            offset = new Regex(next_href, "offset=([^&]+)").getMatch(0);
        } while (!this.isAbort() && !StringUtils.isEmpty(next_href) && offset != null);
        return ret;
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

    private DownloadLink get500Thumbnail(final DownloadLink audiolink, final Map<String, Object> track) throws ParseException {
        return getThumbnail(audiolink, track, "-t500x500.jpg");
    }

    private DownloadLink getOriginalThumbnail(final DownloadLink audiolink, final Map<String, Object> track) throws ParseException {
        return getThumbnail(audiolink, track, "-original.jpg");
    }

    private DownloadLink getThumbnail(final DownloadLink audiolink, final Map<String, Object> track, final String urlReplaceStr) throws ParseException {
        /* Not all tracks have artwork available. */
        String artworkurl = (String) track.get("artwork_url");
        if (!StringUtils.isEmpty(artworkurl)) {
            if (urlReplaceStr != null) {
                artworkurl = artworkurl.replaceFirst("-large\\.jpg", urlReplaceStr);
            }
            final DownloadLink thumb = createDownloadlink(artworkurl);
            thumb.setProperties(audiolink.getProperties());
            if (urlReplaceStr != null) {
                final String thumbnailSizeHintForFilename = new Regex(urlReplaceStr, "-([A-Za-z0-9]+)\\.jpg$").getMatch(0);
                if (thumbnailSizeHintForFilename != null) {
                    thumb.setProperty(SoundcloudCom.PROPERTY_title, audiolink.getStringProperty(SoundcloudCom.PROPERTY_title) + "_" + thumbnailSizeHintForFilename);
                }
            }
            thumb.setProperty("type", "jpg");
            final String formattedFilename = SoundcloudCom.getFormattedFilename(thumb);
            thumb.setFinalFileName(formattedFilename);
            thumb.setAvailable(true);
            if (audiolink.getFilePackage() != null) {
                thumb._setFilePackage(audiolink.getFilePackage());
            }
            return thumb;
        }
        return null;
    }

    private DownloadLink crawlPurchaseURL(final Map<String, Object> track) throws ParseException {
        try {
            final String purchase_url = (String) track.get("purchase_url");
            if (!StringUtils.isEmpty(purchase_url)) {
                logger.info("Found purchase_url");
                final DownloadLink result = createDownloadlink(purchase_url);
                return result;
            } else {
                logger.info("Failed to find purchase_url - probably doesn't exist");
            }
        } catch (final Throwable e) {
            logger.log(e);
            logger.warning("Failed to find purchase_url...");
        }
        return null;
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

    private String getFormattedPackagename(final String usernameURL, final String usernameFull, String playlistname, final String date) throws ParseException {
        String formattedpackagename = cfg.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (StringUtils.isEmpty(formattedpackagename)) {
            formattedpackagename = defaultCustomPackagename;
        }
        /* Check for missing data */
        if (playlistname == null) {
            playlistname = "-";
        }
        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            try {
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE);
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
            }
            if (formattedDate != null) {
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            } else {
                formattedpackagename = formattedpackagename.replace("*date*", "");
            }
        }
        if (formattedpackagename.contains("*url_username*")) {
            formattedpackagename = formattedpackagename.replace("*url_username*", usernameURL);
        }
        if (formattedpackagename.contains("*channelname*")) {
            formattedpackagename = formattedpackagename.replace("*channelname*", usernameFull);
        }
        // Insert playlistname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*playlistname*", playlistname);
        return formattedpackagename;
    }

    private void parseFileInfo(final DownloadLink dl, final Map<String, Object> data) throws Exception {
        if (data != null) {
            final AvailableStatus status = SoundcloudCom.checkStatusJson(this, dl, data);
            dl.setAvailableStatus(status);
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}