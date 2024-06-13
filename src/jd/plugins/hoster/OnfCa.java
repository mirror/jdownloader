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

import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onf.ca", "nfb.ca" }, urls = { "https?://(?:www\\.)?onf\\.ca/film/([a-z0-9\\-_]+)", "https?://(?:www\\.)?nfb\\.ca/film/([a-z0-9\\-_]+)" })
public class OnfCa extends PluginForHost {
    public OnfCa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/about/important-notices/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            /* Set fallback name */
            link.setName(this.getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://www." + this.getHost() + "/remote/services/oembed/?url=" + Encoding.urlEncode(link.getPluginPatternMatcher()) + "&format=json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String title = (String) entries.get("title");
        if (!StringUtils.isEmpty(title)) {
            link.setFinalFileName(title + extDefault);
        }
        final String description = (String) entries.get("video_description");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        br.getPage("/film/" + this.getFID(link) + "/embed/player/?player_mode=&embed_mode=0&context_type=film");
        /* Double-check */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("id=\"MESSAGE_GEOBLOCKED\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
        }
        final String hlsMaster = br.getRegex("source\\s*:\\s*'(https?://[^<>\"\\']+\\.m3u8)").getMatch(0);
        if (hlsMaster == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(hlsMaster);
        br.getPage(hlsMaster);
        final HlsContainer hlsBest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hlsBest.getDownloadurl());
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