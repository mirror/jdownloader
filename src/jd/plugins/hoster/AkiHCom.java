//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AkiHCom extends PluginForHost {
    public AkiHCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        /* 2024-06-17: HLS download = not resumable */
        return false;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "aki-h.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/([A-Za-z0-9]+)/?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms/";
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
        final String publicVideoID = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(publicVideoID + extDefault);
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(publicVideoID)) {
            /* E.g. redirect to https://aki-h.com/error/404/ */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("class=\"uc-name\"[^>]*>([^<]+)</div>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        // final String publicVideoID = this.getFID(link);
        final String internalVideoID = br.getRegex("window\\.player_container\\(0, (\\d+)\\);").getMatch(0);
        if (internalVideoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getPage("https://v.aki-h.com/v/" + internalVideoID);
        // br.getHeaders().put("Referer", "https://v.aki-h.com/v/" + internalVideoID);
        br.getHeaders().put("Referer", "https://aki-h.com/");
        br.getPage("https://v.aki-h.com/v/" + internalVideoID);
        final String vid = br.getRegex("var vid = '([^\"\\']+)'").getMatch(0);
        if (vid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getHeaders().put("Referer", "https://aki-h.stream/v/" + vid);
        br.getPage("https://v.aki-h.com/f/" + vid);
        final String nextID = br.getRegex("/v/([A-Za-z0-9]+)").getMatch(0);
        if (nextID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", "https://aki-h.stream/v/" + nextID);
        br.getPage("https://aki-h.stream/file/" + nextID + "/");
        this.checkFFmpeg(link, "Download a HLS Stream");
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        this.dl = new HLSDownloader(link, this.br, url_hls);
        this.dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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