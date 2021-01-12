//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "c-span.org" }, urls = { "https?://(?:www\\.)?c\\-span\\.org/video/\\?\\d+(?:\\-\\d+)?/[a-z0-9\\-]+" })
public class CspanOrg extends PluginForHost {
    public CspanOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.c-span.org/about/termsAndConditions/";
    }

    // private static final String app = "cfx/st";
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL().replace("http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\\'og:title\\' content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = new Regex(link.getPluginPatternMatcher(), "([^/]+)$").getMatch(0);
        }
        final String uploadDate = br.getRegex("itemprop='uploadDate'>(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        if (uploadDate != null) {
            filename = uploadDate + "_" + filename;
        }
        link.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        // http://www.c-span.org/common/services/flashXml.php?programid=424926&version=2014-01-23
        final String progid = this.br.getRegex("name=\\'progid' value=\\'(\\d+)\\'").getMatch(0);
        if (progid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2021-01-11: Old non working rtmp-endpoint: "https://www.c-span.org/common/services/flashXml.php?programid=" + progid */
        String hlsMaster = br.getRegex("(https?://[^/]+/program/program\\." + progid + "[^<>\"]+\\.m3u8)").getMatch(0);
        String dllink_http = null;
        if (hlsMaster == null) {
            /* First try to get mobile http URL */
            long bitrate_max = 0;
            long bitrate_temp = 0;
            /* 2017-05-09: Added this code as backup */
            this.br.getPage("https://www.c-span.org/assets/player/ajax-player.php?os=android&html5=program&id=" + progid);
            Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            hlsMaster = (String) JavaScriptEngineFactory.walkJson(entries, "video/files/{0}/path/#text");
            try {
                /* Find highest http quality */
                final List<Object> qualities = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "video/files/{0}/qualities");
                for (final Object fileo : qualities) {
                    entries = (Map<String, Object>) fileo;
                    bitrate_temp = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "bitrate/#text"), 0);
                    if (bitrate_temp > bitrate_max) {
                        dllink_http = (String) JavaScriptEngineFactory.walkJson(entries, "file/#text");
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            if (dllink_http != null) {
                /* http download */
                /* Important! */
                dllink_http = Encoding.htmlDecode(dllink_http);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink_http, true, 0);
                if (!looksLikeDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl.startDownload();
                return;
            }
        }
        /* hls download */
        if (hlsMaster == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getPage(hlsMaster);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(brc));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
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