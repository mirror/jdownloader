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

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sunporno.com" }, urls = { "https?://(?:www\\.)?(sunporno\\.com/videos/|embeds\\.sunporno\\.com/embed/)\\d+" })
public class SunPornoCom extends PluginForHost {
    private String dllink = null;

    public SunPornoCom(PluginWrapper wrapper) {
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
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/pages/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String extDefault = ".mp4";
        final String videoid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(videoid + extDefault);
        }
        dllink = null;
        this.setBrowserExclusive();
        br.getPage("https://www." + this.getHost() + "/videos/" + videoid);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("sunporno.com/404.php") || br.containsHTML("(>The file you have requested was not found on this server|<title>404</title>|This video has been deleted)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String betterDllink = null;
        String title = br.getRegex("class=\"block-headline-right\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>\\s*(.*?)\\s*(?:(\\s+\\(New.*?\\))?\\s+-\\s+Sunporno.*?)?</title>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(title + extDefault);
        }
        dllink = br.getRegex("src:\"(https?://[^\"]+)\",type:\"video/mp4\",res:\"high\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("src:\"(https?://[^\"]+)\",type:\"video/mp4\",res:\"low\"").getMatch(0);
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            final boolean doKeyHandling = false; // 2024-02-29: Not needed anymore
            final String key = new Regex(dllink, "(key=.+)").getMatch(0);
            if (key != null && doKeyHandling) {
                /* 2019-09-06: This might not be needed anymore */
                /* Avoids 403 issues. */
                this.br.getPage("//www.sunporno.com/?area=movieFilePather&callback=movieFileCallbackFunc&id=1135032&url=" + Encoding.urlEncode(key) + "&_=" + System.currentTimeMillis());
                betterDllink = PluginJSonUtils.getJsonValue(this.br, "path");
                if (betterDllink != null && betterDllink.startsWith("http")) {
                    dllink = betterDllink;
                }
            }
            final Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                /* Workaround for buggy porn servers: */
                if (con.getResponseCode() == 404) {
                    /*
                     * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                     */
                    final String redirect_url = con.getRequest().getUrl();
                    con = br.openHeadConnection(redirect_url);
                }
                handleConnectionErrors(br2, con);
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript5;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}