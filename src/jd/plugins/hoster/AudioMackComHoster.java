//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.parser.UrlQuery;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AudioMackComHoster extends PluginForHost {
    public AudioMackComHoster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "http://www.audiomack.com/about/terms-of-service";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "audiomack.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(";
            /* Older RegExes */
            regex += "song/[a-z0-9\\-_]+/[a-z0-9\\-_]+|(?:embed\\d-)?large/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+|api/music/url/(?:album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+|song/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)";
            /* 2021-04-20 */
            regex += "|[a-z0-9\\-_]+/song/[A-Za-z0-9\\-_]+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    // private static final String TYPE_API =
    // "(?i)http?://(www\\.)?audiomack\\.com/api/music/url/(?:album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+|song/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)";
    private Map<String, Object> results                          = null;
    private static final String extDefault                       = ".m4a";
    public static final String  PROPERTY_TRACK_ID                = "track_id";
    public static final String  PROPERTY_ALBUM_ID                = "album_id";
    public static final String  PROPERTY_PLAYLIST_POSITION       = "playlist_position";
    public static final String  PROPERTY_PLAYLIST_NUMBEROF_ITEMS = "playlist_numberof_items";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String trackID = link.getStringProperty(PROPERTY_TRACK_ID);
        final String albumID = link.getStringProperty(PROPERTY_ALBUM_ID);
        if (trackID != null) {
            return "audiomack://track" + trackID + "/album/" + albumID;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        results = null;
        if (!link.isNameSet()) {
            link.setName(this.getLinkID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*This song cannot be found or has been removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String ogurl = br.getRegex("\"og:url\" content=\"([^\"]+)\"").getMatch(0);
        final String[] match = new Regex(ogurl, "(?i).*/([^/]+)/(song)/([^/]+)").getRow(0);
        if (match == null || match.length != 3) {
            /* Unsupported URL and/or plugin failure */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String musicType = match[1];
        final String artistID = match[0];
        final String musicSlug = match[2];
        br.getPage(getOAuthQueryString("song", artistID, musicSlug));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        results = (Map<String, Object>) entries.get("results");
        final String status = (String) results.get("status");
        if (status.equalsIgnoreCase("suspended")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parseSingleSongData(link, results);
        return AvailableStatus.TRUE;
    }

    public static void parseSingleSongData(final DownloadLink link, final Map<String, Object> track) {
        link.setProperty(PROPERTY_TRACK_ID, track.get("id"));
        final Map<String, Object> album_details = (Map<String, Object>) track.get("album_details");
        if (album_details != null) {
            link.setProperty(PROPERTY_ALBUM_ID, album_details.get("id"));
        }
        final String artist = (String) track.get("artist");
        final String title = (String) track.get("title");
        final int playlistPosition = link.getIntegerProperty(PROPERTY_PLAYLIST_POSITION, -1);
        String feat = (String) track.get("featuring");
        String filename;
        final boolean titleStartsWithPositionNumber = title.matches("^\\d+\\..+");
        if (playlistPosition != -1 && !titleStartsWithPositionNumber) {
            filename = String.format("%d %s - %s", playlistPosition, artist, title);
            filename = playlistPosition + ". " + artist + " - " + title;
        } else if (titleStartsWithPositionNumber) {
            filename = title;
        } else {
            filename = artist + " - " + title;
        }
        if (StringUtils.isNotEmpty(feat)) {
            filename = String.format("%s (feat. %s)", filename, feat.replaceFirst(", ([^,]+)$", " & $1"));
        }
        filename += extDefault;
        link.setFinalFileName(filename);
        final String durationSecondsStr = track.get("duration").toString();
        if (durationSecondsStr != null && durationSecondsStr.matches("\\d+")) {
            /* Estimate filesize */
            final int durationSeconds = Integer.parseInt(durationSecondsStr);
            if (durationSeconds > 0) {
                link.setDownloadSize(256 * 1024l / 8 * durationSeconds);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = (String) results.get("download_url");
        if (StringUtils.isEmpty(dllink)) {
            dllink = (String) results.get("streaming_url");
        }
        if (StringUtils.isEmpty(dllink)) {
            /* 2022-10-13 */
            final boolean pluginBroken = false;
            if (pluginBroken) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String trackID = link.getStringProperty(PROPERTY_TRACK_ID);
            final String albumID = link.getStringProperty(PROPERTY_ALBUM_ID);
            final String requesturl = "https://api.audiomack.com/v1/music/play/" + trackID;
            final UrlQuery query = new UrlQuery();
            if (albumID != null) {
                query.add("album_id", albumID);
            }
            // query.add("section", "Song%20Page");
            final String apiurl = getSignedURL(requesturl, query, true);
            br.getPage(apiurl);
            final Map<String, Object> dlresponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            dllink = (String) dlresponse.get("signedUrl");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Errorcode " + dlresponse.get("errorcode") + " | " + dlresponse.get("message").toString());
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2023-11-23: Allow max 1 connection per file in order to spare the servers' capacities. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken song?");
        }
        dl.startDownload();
    }

    public static String getOAuthQueryString(final String musicType, final String artistID, final String musicSlug) throws Exception {
        if (musicType == null) {
            throw new IllegalArgumentException();
        }
        final String apiUrl = "https://api.audiomack.com";
        final String apiVersion = "v1";
        final String requestUrl;
        if ("playlist".equals(musicType)) {
            requestUrl = apiUrl + "/" + apiVersion + "/" + musicType + "/" + artistID + "/" + musicSlug;
        } else {
            requestUrl = apiUrl + "/" + apiVersion + "/music/" + musicType + "/" + artistID + "/" + musicSlug;
        }
        return getSignedURL(requestUrl, new UrlQuery(), false);
    }

    private static String getSignedURL(final String url, final UrlQuery query, final boolean isSingleSong) {
        final String method = "GET";
        final String apiConsumerKey = "audiomack-js";
        final String apiConsumerSecret = "f3ac5b086f3eab260520d8e3049561e6";
        /* Order of the query key value pairs is important! */
        query.add("environment", "desktop-web");
        if (isSingleSong) {
            query.add("hq", "true");
        }
        query.add("oauth_consumer_key", apiConsumerKey);
        query.add("oauth_nonce", generateNonce(32));
        query.add("oauth_signature_method", "HMAC-SHA1");
        query.add("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000l));
        query.add("oauth_version", "1.0");
        if (isSingleSong) {
            query.add("section", "Song%20Page");
        }
        final String seed = method + "&" + Encoding.urlEncode(url) + "&" + Encoding.urlEncode(query.toString());
        final String oauthSignature = getOAuthSignature(seed, apiConsumerSecret + "&");
        query.add("oauth_signature", oauthSignature);
        return url + "?" + query.toString();
    }

    private static String generateNonce(final int range) {
        String alphaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < range; i++) {
            int pos = rand.nextInt(alphaNum.length());
            sb.append(alphaNum.substring(pos, pos + 1));
        }
        return sb.toString();
    }

    private static String getOAuthSignature(final String query, final String key) {
        HMac hmac = new HMac(new SHA1Digest());
        byte[] buf = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(key.getBytes()));
        byte[] qbuf = query.getBytes();
        hmac.update(qbuf, 0, qbuf.length);
        hmac.doFinal(buf, 0);
        return Encoding.urlEncode(Base64.encodeToString(buf, false));
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}