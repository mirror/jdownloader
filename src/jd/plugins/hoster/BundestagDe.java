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

import java.util.Map;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bundestag.de" }, urls = { "https?://(?:www\\.)?bundestag\\.de/mediathek.+" })
public class BundestagDe extends PluginForHost {
    public BundestagDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllinkHTTP        = null;
    private String               dllinkHLSMaster   = null;

    @Override
    public String getAGBLink() {
        return "http://www.bundestag.de/impressum";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllinkHTTP = null;
        String contentID = new Regex(link.getPluginPatternMatcher(), "(?i)ids=(\\d+)").getMatch(0);
        if (contentID == null) {
            contentID = new Regex(link.getPluginPatternMatcher(), "(?i)id=(\\d+)").getMatch(0);
        }
        if (contentID == null) {
            /* Seems like user added an invalid url. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.isNameSet()) {
            link.setName(contentID + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(400);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 400) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("class=\"error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setLinkID(this.getHost() + "://" + contentID);
        String filename;
        final String url_filename = contentID;
        final Regex titleinfo = this.br.getRegex("<h2><span class=\"datum\">([^<>]*?)</span><br />([^<>]*?)</h2>");
        String title = titleinfo.getMatch(0);
        String titleJson = null;
        String subtitle = titleinfo.getMatch(1);
        dllinkHTTP = this.br.getRegex("name=\"data\\-downloadUrl\" value=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (dllinkHTTP == null) {
            dllinkHTTP = regexDllinkHTTP();
        }
        if (dllinkHTTP == null) {
            /* 2017-01-25: New */
            this.br.getPage("https://www.bundestag.de/mediathekoverlay?view=main&videoid=" + contentID);
            if (this.br.toString().length() <= 50) {
                /* Probably no video content/offline. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllinkHTTP = regexDllinkHTTP();
        }
        /* Find HLS downloadurl and nice title. */
        if (title == null || this.dllinkHTTP == null) {
            br.getPage("https://webtv.bundestag.de/player/macros/_v_q_0_de/_s_embed_fade_old/pl/data/playlist_html.json?playout=hls&noflash=true&theov=2.83.1&singleton=" + contentID);
            /* Double-check */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final Map<String, Object> videoInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "pl/entries/{0}");
            titleJson = (String) videoInfo.get("title");
            final Map<String, Object> video = (Map<String, Object>) videoInfo.get("video");
            this.dllinkHLSMaster = (String) video.get("src");
            if (StringUtils.isEmpty(this.dllinkHLSMaster)) {
                logger.warning("Failed to find HLS master");
            } else {
                logger.info("Successfully found HLS master");
            }
        }
        if (titleJson != null) {
            filename = titleJson;
        } else if (subtitle != null && title != null) {
            filename = title.trim() + " - " + subtitle.trim();
        } else {
            filename = url_filename;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = Encoding.htmlDecode(filename).trim();
        link.setFinalFileName(filename + ".mp4");
        if (dllinkHTTP != null) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllinkHTTP), link, filename, ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    private String regexDllinkHTTP() {
        return br.getRegex("\"(https?://[^<>]+/ondemand/[^<>]+\\.mp4[^<>]*?)\"").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(this.dllinkHTTP) && StringUtils.isEmpty(this.dllinkHLSMaster)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Prefer HTTP download over HLS download */
        if (!StringUtils.isEmpty(this.dllinkHTTP)) {
            /* HTTP download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllinkHTTP, free_resume, free_maxchunks);
            handleConnectionErrors(br, dl.getConnection());
            dl.startDownload();
        } else {
            /* HLS download */
            br.getPage(this.dllinkHLSMaster);
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br)).getDownloadurl());
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
