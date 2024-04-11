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

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.controller.LazyPlugin;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HdsexOrg extends PluginForHost {
    public HdsexOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "lang", "en");
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        /**
         * 2024-01-24: The two domains look to belong to the same owner/project but also they seem to host two different instances of the
         * website here where e.g. not all videos of hdsex.org are available on hdsex2.com.
         */
        ret.add(new String[] { "hdsex.org" });
        ret.add(new String[] { "hdsex2.com" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-12-21: Changed main domain from hdsex.org to hdsex2.com since they had major server issue when streaming via hdsex.org */
        return this.rewriteHost(getPluginDomains(), host);
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
            ret.add("https?://(?:(?:www|[a-z]{2})\\.)?" + buildHostsPatternPart(domains) + "/(?:video|embed)/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://hdsex.org/documents/terms-and-conditions";
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
            link.setName(this.getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.getPage("https://" + this.getHost() + "/video/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("video-title=\"([^\"]+)").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = br.getRegex("<meta itemprop=\"name\" content=\"([^\"]+)\"").getMatch(0);
        }
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
        String hlsBest = br.getRegex("\"(https?://[^\"]*\\.m3u8[^\"]*)\"").getMatch(0);
        if (StringUtils.isEmpty(hlsBest)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        hlsBest = Encoding.htmlDecode(hlsBest);
        br.getHeaders().put("Origin", "https://" + br.getHost());
        br.getHeaders().put("Referer", "https://" + br.getHost() + "/");
        br.getPage(hlsBest);
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hlsBest);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* Last checked: 2024-04-11: Limited to 1 as their CDN servers are very very slow. */
        return 1;
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