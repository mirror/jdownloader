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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.parser.UrlQuery;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.controller.LazyPlugin;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AudioMa extends PluginForHost {
    public AudioMa(PluginWrapper wrapper) {
        super(wrapper);
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

    private static final String  TYPE_API       = "http://(www\\.)?audiomack\\.com/api/music/url/(?:album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+|song/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)";
    private static final boolean use_oembed_api = false;
    private static final boolean use_oauth_api  = true;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.AudioExtensions.MP3);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        if (use_oauth_api) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(getOAuthQueryString(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("results");
            final String status = (String) entries.get("status");
            if (status.equalsIgnoreCase("suspended")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String artist = (String) entries.get("artist");
            final String songTitle = (String) entries.get("title");
            if (StringUtils.isEmpty(artist) || StringUtils.isEmpty(songTitle)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = artist + " - " + songTitle;
        } else if (use_oembed_api) {
            br.getPage("http://www." + this.getHost() + "/oembed?format=json&url=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*Did not find any music with url")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String artist = PluginJSonUtils.getJsonValue(br, "author_name");
            final String songname = PluginJSonUtils.getJsonValue(br, "title");
            if (StringUtils.isEmpty(artist) || StringUtils.isEmpty(songname)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = artist + " - " + songname;
        } else if (link.getPluginPatternMatcher().matches(TYPE_API)) {
            br.getPage(link.getStringProperty("mainlink"));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = link.getStringProperty("plain_filename");
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.containsHTML("(?i)>\\s*Page not found\\s*<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<aside class=\"span2\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]*?) \\- download and stream \\| AudioMack</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename != null && link.getFinalFileName() == null) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim() + ".mp3");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink;
        if (use_oauth_api) {
            dllink = PluginJSonUtils.getJsonValue(br, "download_url");
            if (StringUtils.isEmpty(dllink)) {
                dllink = PluginJSonUtils.getJsonValue(br, "streaming_url");
            }
            if (StringUtils.isEmpty(dllink)) {
                /* 2022-10-13 */
                final boolean pluginBroken = true;
                if (pluginBroken) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String dlurl = getOAuthQueryStringDownload(br);
                br.postPage(dlurl, "session=TODO&environment=desktop-web&section=Play%20Song");
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* Access real link here in case we used the oembed API above */
            if (use_oembed_api && !br.getURL().equals(link.getDownloadURL())) {
                br.getPage(link.getDownloadURL());
            }
            /* Prefer downloadlink --> Higher quality version */
            dllink = br.getRegex("\"(http://(www\\.)?music\\.audiomack\\.com/tracks/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                if (link.getDownloadURL().matches(TYPE_API)) {
                } else {
                    final String apilink = br.getRegex("\"(http://(www\\.)?audiomack\\.com/api/[^<>\"]*?)\"").getMatch(0);
                    if (apilink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(apilink);
                }
                dllink = PluginJSonUtils.getJsonValue(br, "url");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static String getOAuthQueryString(final Browser br) throws Exception {
        /* 2019-01-30: Used hardcoded values as they change js more often and API vars have also changed */
        String ogurl = br.getRegex("\"og:url\" content=\"([^\"]+)\"").getMatch(0);
        String[] match = new Regex(ogurl, ".*/([^/]+)/(song|album|playlist)/([^/]+)").getRow(0);
        if (match == null || match.length != 3) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String musicType = match[1];
        final String artistId = match[0];
        final String musicSlug = match[2];
        // src='/static/dist/desktop/252.3d3a7d50d9de7c1fefa0.js'
        // String jsurl = br.getRegex("src='([^']+?/275\\.[0-9a-f]+?\\.chunk\\.js)").getMatch(0);
        final Browser cbr = br.cloneBrowser();
        // cbr.getPage(jsurl);
        String apiUrl = cbr.getRegex("API_URL:\"([^\"]+)\"").getMatch(0);
        /* Use hardcoded value */
        apiUrl = "https://api.audiomack.com";
        String apiVersion = cbr.getRegex("API_VERSION:\"([^\"]+)\"").getMatch(0);
        apiVersion = "v1";
        String apiConsumerKey = cbr.getRegex("API_CONSUMER_KEY:\"([^\"]+)\"").getMatch(0);
        apiConsumerKey = "audiomack-js";
        String apiConsumerSecret = cbr.getRegex("API_CONSUMER_SECRET:\"([^\"]+)\"").getMatch(0);
        apiConsumerSecret = "f3ac5b086f3eab260520d8e3049561e6";
        String method = "GET";
        final String requestUrl;
        if ("playlist".equals(musicType)) {
            requestUrl = apiUrl + "/" + apiVersion + "/" + musicType + "/" + artistId + "/" + musicSlug;
        } else {
            requestUrl = apiUrl + "/" + apiVersion + "/music/" + musicType + "/" + artistId + "/" + musicSlug;
        }
        final UrlQuery query = new UrlQuery();
        query.add("oauth_consumer_key", apiConsumerKey);
        query.add("oauth_nonce", generateNonce(32));
        query.add("oauth_signature_method", "HMAC-SHA1");
        query.add("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000l));
        query.add("oauth_version", "1.0");
        final String seed = method + "&" + Encoding.urlEncode(requestUrl) + "&" + Encoding.urlEncode(query.toString());
        final String oauthSignature = getOAuthSignature(seed, apiConsumerSecret + "&");
        query.add("oauth_signature", oauthSignature);
        return requestUrl + "?" + query.toString();
    }

    public static String getOAuthQueryStringDownload(final Browser br) throws Exception {
        final String apiConsumerKey = "audiomack-js";
        final String apiConsumerSecret = "f3ac5b086f3eab260520d8e3049561e6";
        String method = "POST";
        final String requestUrl = "https://api.audiomack.com/v1/music/20472080/play";
        final UrlQuery query = new UrlQuery();
        query.add("oauth_consumer_key", apiConsumerKey);
        query.add("oauth_nonce", generateNonce(32));
        query.add("oauth_signature_method", "HMAC-SHA1");
        query.add("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000l));
        query.add("oauth_version", "1.0");
        final String seed = method + "&" + Encoding.urlEncode(requestUrl) + "&" + Encoding.urlEncode(query.toString());
        String oauthSignature = getOAuthSignature(seed, apiConsumerSecret + "&");
        query.add("oauth_signature", oauthSignature);
        return requestUrl + "?" + query.toString();
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}