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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sunporno.com" }, urls = { "https?://(www\\.)?(sunporno\\.com/videos/|embeds\\.sunporno\\.com/embed/)\\d+" })
public class SunPornoCom extends PluginForHost {

    /* DEV NOTES */
    /* Porn_plugin */

    private String DLLINK = null;

    public SunPornoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sunporno.com/pages/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://www.sunporno.com/videos/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("sunporno.com/404.php") || br.containsHTML("(>The file you have requested was not found on this server|<title>404</title>|This video has been deleted)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String betterDllink = null;
        String filename = br.getRegex("class=\"block-headline-right\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        DLLINK = br.getRegex("addVariable\\(\\'file\\', \\'(https?://.*?)\\'\\)").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("\\'(https?://\\d+\\.\\d+\\.\\d+\\.\\d+/v/[a-z0-9]+/.*?)\\'").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(https?://vstreamcdn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // DLLINK = Encoding.htmlDecode(DLLINK);
        final String key = new Regex(DLLINK, "(key=.+)").getMatch(0);
        if (key != null) {
            /* Avoids 403 issues. */
            this.br.getPage("//www.sunporno.com/?area=movieFilePather&callback=movieFileCallbackFunc&id=1135032&url=" + Encoding.urlEncode(key) + "&_=" + System.currentTimeMillis());
            DLLINK = PluginJSonUtils.getJsonValue(this.br, "path");
            betterDllink = PluginJSonUtils.getJsonValue(this.br, "path");
            if (betterDllink != null && betterDllink.startsWith("http")) {
                DLLINK = betterDllink;
            } else {
                DLLINK = Encoding.htmlDecode(DLLINK);
            }
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + DLLINK.substring(DLLINK.length() - 4, DLLINK.length()));
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(DLLINK);
            /* Workaround for buggy porn servers: */
            if (con.getResponseCode() == 404) {
                /*
                 * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                 */
                final String redirect_url = con.getRequest().getUrl();
                con = br.openHeadConnection(redirect_url);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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