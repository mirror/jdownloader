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

import java.util.Random;

import org.appwork.utils.encoding.Base64;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiomack.com" }, urls = { "https?://(www\\.)?audiomack\\.com/(song/[a-z0-9\\-_]+/[a-z0-9\\-_]+|(?:embed\\d-)?large/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+|api/music/url/(?:album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+|song/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+))" })
public class AudioMa extends PluginForHost {
    public AudioMa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.audiomack.com/about/terms-of-service";
    }

    private static final String  TYPE_API       = "http://(www\\.)?audiomack\\.com/api/music/url/(?:album/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/\\d+|song/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)";
    private static final boolean use_oembed_api = false;
    private static final boolean use_oauth_api  = true;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        if (use_oauth_api) {
            br.getPage(link.getPluginPatternMatcher());
            br.getPage(getOAuthQueryString(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String artist = PluginJSonUtils.getJsonValue(br, "artist");
            final String songname = PluginJSonUtils.getJsonValue(br, "title");
            if (artist == null || songname == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = artist + " - " + songname;
        } else if (use_oembed_api) {
            br.getPage("http://www.audiomack.com/oembed?format=json&url=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML(">Did not find any music with url")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String artist = PluginJSonUtils.getJsonValue(br, "author_name");
            final String songname = PluginJSonUtils.getJsonValue(br, "title");
            if (artist == null || songname == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = artist + " - " + songname;
        } else if (link.getDownloadURL().matches(TYPE_API)) {
            br.getPage(link.getStringProperty("mainlink", null));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = link.getStringProperty("plain_filename", null);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">Page not found<")) {
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
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename != null && link.getFinalFileName() == null) {
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink;
        if (use_oauth_api) {
            dllink = PluginJSonUtils.getJsonValue(br, "download_url");
            if (dllink == null) {
                dllink = PluginJSonUtils.getJsonValue(br, "streaming_url");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            /* Access real link here in case we used the oembed API above */
            if (use_oembed_api && !br.getURL().equals(downloadLink.getDownloadURL())) {
                br.getPage(downloadLink.getDownloadURL());
            }
            /* Prefer downloadlink --> Higher quality version */
            dllink = br.getRegex("\"(http://(www\\.)?music\\.audiomack\\.com/tracks/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                if (downloadLink.getDownloadURL().matches(TYPE_API)) {
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static String getOAuthQueryString(final Browser br) throws Exception {
        String ogurl = br.getRegex("\"og:url\" content=\"([^\"]+)\"").getMatch(0);
        String[] match = new Regex(ogurl, ".+?/(?:embed/)?(song|album|playlist)/(.+?)/(.+)$").getRow(0);
        if (match == null || match.length != 3) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String musicType = match[0];
        final String artistId = match[1];
        final String musicSlug = match[2];
        // src='/static/dist/desktop/252.3d3a7d50d9de7c1fefa0.js'
        String jsurl = br.getRegex("src='([^']+?/252\\.[0-9a-f]+?\\.js)").getMatch(0);
        final Browser cbr = br.cloneBrowser();
        cbr.getPage(jsurl);
        String apiUrl = cbr.getRegex("API_URL:\"([^\"]+)\"").getMatch(0);
        String apiVersion = cbr.getRegex("API_VERSION:\"([^\"]+)\"").getMatch(0);
        String apiConsumerKey = cbr.getRegex("API_CONSUMER_KEY:\"([^\"]+)\"").getMatch(0);
        String apiConsumerSecret = cbr.getRegex("API_CONSUMER_SECRET:\"([^\"]+)\"").getMatch(0);
        String method = "GET";
        String requestUrlFmt;
        if ("playlist".equals(musicType)) {
            requestUrlFmt = "%s/%s/%s/%s/%s";
        } else {
            requestUrlFmt = "%s/%s/music/%s/%s/%s";
        }
        String requestUrl = String.format(requestUrlFmt, apiUrl, apiVersion, musicType, artistId, musicSlug);
        String requestParam = String.format("oauth_consumer_key=%s&oauth_nonce=%s&oauth_signature_method=HMAC-SHA1&oauth_timestamp=%d&oauth_version=1.0", apiConsumerKey, generateNonce(32), (int) (System.currentTimeMillis() / 1000l));
        String seed = String.format("%s&%s&%s", method, Encoding.urlEncode(requestUrl), Encoding.urlEncode(requestParam));
        String oauthSignature = getOAuthSignature(seed, apiConsumerSecret + "&");
        return String.format("%s?%s&oauth_signature=%s", requestUrl, requestParam, oauthSignature);
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