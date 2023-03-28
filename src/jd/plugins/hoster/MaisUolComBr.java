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
package jd.plugins.hoster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mais.uol.com.br" }, urls = { "https?://((?:www\\.)?mais\\.uol\\.com\\.br/view/(?:[a-z0-9]+/[A-Za-z0-9\\-]+|\\d+)|player\\.mais\\.uol\\.com\\.br/\\?mediaId=\\d+\\&type=video)" })
public class MaisUolComBr extends PluginForHost {
    public MaisUolComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllinkHTTP      = null;
    private String  dllinkHLSMaster = null;
    private boolean server_issues   = false;

    @Override
    public String getAGBLink() {
        return "http://mais.uol.com.br/";
    }

    private static final String TYPE_NORMAL     = "(?i)https?://(?:www\\.)?mais\\.uol\\.com\\.br/view/([a-z0-9]+)/([A-Za-z0-9\\-]+)";
    private static final String TYPE_VIEW_SHORT = "(?i)https?://(?:www\\.)?mais\\.uol\\.com\\.br/view/(\\d+)$";
    private static final String TYPE_EMBED      = "(?i)https?://player\\.mais\\.uol\\.com\\.br/\\?mediaId=(\\d+)\\&type=video";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllinkHTTP = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400, 500 });
        String mediaID;
        Map<String, Object> entries;
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            mediaID = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(TYPE_VIEW_SHORT)) {
            mediaID = new Regex(link.getPluginPatternMatcher(), TYPE_VIEW_SHORT).getMatch(0);
        } else {
            /* 2021-08-16: TODO: Find a way to get the v4 API json with only a single HTTP request */
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 | br.getHttpConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("> M\\&iacute;dia n\\&atilde;o encontrada|class=\"msg alert\"") || !this.canHandle(this.br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            mediaID = br.getRegex("name=\"mediaId\" value=\"(\\d+)\"").getMatch(0);
            if (mediaID == null) {
                mediaID = br.getRegex("mediaId=(\\d+)\"").getMatch(0);
            }
            if (mediaID == null) {
                /* 2020-11-23: ID is inside URL */
                mediaID = new Regex(br.getURL(), ".*/(\\d+)$").getMatch(0);
            }
            if (mediaID == null) {
                /* Find mediaID */
                final String urlpart = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
                br.getPage("https://api.mais.uol.com.br/apiuol/v3/media/detail/" + urlpart);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                entries = (Map<String, Object>) entries.get("item");
                mediaID = entries.get("mediaId").toString();
            }
        }
        link.setLinkID(this.getHost() + "://" + mediaID);
        br.getPage("https://api.mais.uol.com.br/apiuol/v4/player/data/" + mediaID);
        /* Old way */
        // br.getPage("https://api.mais.uol.com.br/apiuol/v3/player/" + mediaID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        entries = (Map<String, Object>) entries.get("item");
        String filename = (String) entries.get("title");
        final Object formatsO = entries.get("formats");
        final Object audioUrlO = entries.get("audioUrl");
        if (audioUrlO != null && audioUrlO instanceof String) {
            /* 2017-02-14 */
            dllinkHTTP = (String) audioUrlO;
            if (StringUtils.isEmpty(dllinkHTTP) || !dllinkHTTP.startsWith("//")) {
                dllinkHTTP = null;
            } else {
                dllinkHTTP = "http:" + dllinkHTTP;
            }
        }
        if (formatsO != null && dllinkHTTP == null) {
            if (formatsO instanceof List) {
                /* Old handling */
                final List<Object> ressourcelist = (List) formatsO;
                /* Old */
                for (final Object o : ressourcelist) {
                    final Map<String, Object> format = (Map<String, Object>) o;
                    final int id = ((Number) format.get("id")).intValue();
                    if (id == 9 || id == 2) {
                        dllinkHTTP = (String) format.get("url");
                        break;
                    }
                }
            } else {
                entries = (Map<String, Object>) formatsO;
                final Iterator<Entry<String, Object>> formatiterate = entries.entrySet().iterator();
                /*
                 * 2021-08-16: Skip http URLs as they require additional parameters which we only get by doing another API request. For HLS
                 * URLs these parameters are not required!
                 */
                final boolean skipHttpURLs = true;
                while (formatiterate.hasNext()) {
                    final Entry<String, Object> entry = formatiterate.next();
                    entries = (Map<String, Object>) entry.getValue();
                    if (StringUtils.equalsIgnoreCase(entry.getKey(), "HLS")) {
                        if (StringUtils.isEmpty(this.dllinkHLSMaster)) {
                            this.dllinkHLSMaster = (String) entries.get("url");
                        }
                    } else {
                        if (skipHttpURLs) {
                            continue;
                        } else if (StringUtils.isEmpty(dllinkHTTP)) {
                            this.dllinkHTTP = (String) entries.get("url");
                        }
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        if (dllinkHTTP != null && dllinkHTTP.contains(".mp3")) {
            link.setFinalFileName(filename + ".mp3");
        } else {
            link.setFinalFileName(filename + ".mp4");
        }
        if (!StringUtils.isEmpty(this.dllinkHTTP)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllinkHTTP);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(this.dllinkHTTP) && StringUtils.isEmpty(this.dllinkHLSMaster)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!StringUtils.isEmpty(this.dllinkHTTP)) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllinkHTTP, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
            }
            dl.startDownload();
        } else {
            br.getPage(this.dllinkHLSMaster);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
