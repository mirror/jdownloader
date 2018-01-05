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
import java.util.Random;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "submityourflicks.com" }, urls = { "https?://(www\\.)?submityourflicks\\.com/(\\d+[a-z0-9\\-]+\\.html|embconfig/\\d+|embedded/\\d+)" })
public class SubmitYourFlicksCom extends PluginForHost {
    /* Name of their old (removed) portal: submityourtapes.com */
    private String dllink = null;

    public SubmitYourFlicksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.submityourflicks.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String EMBEDLINK = "http://(www\\.)?submityourflicks\\.com/(embconfig|embedded)/\\d+";

    public void correctDownloadLink(final DownloadLink link) {
        final String addedlink = link.getDownloadURL().toLowerCase();
        if (addedlink.matches(EMBEDLINK)) {
            link.setUrlDownload("http://submityourflicks.com/" + new Regex(addedlink, "(\\d+)$").getMatch(0) + "-" + System.currentTimeMillis() + new Random().nextInt(100000) + ".html");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("submityourflicks.com/404.php") || br.containsHTML("(<title>Wops 404 \\.\\.\\.</title>|class=\"style1\">404 \\- this page does not exist|http-equiv=refresh content=\"2; url=http://www\\.submityourflicks\\.com|>Content Removed<)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("utm_campaign")) {
            /* Advertising-redirect */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^>]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) (?:-|at) SubmitYourFlicks</title>").getMatch(0);
        }
        dllink = br.getRegex("addVariable\\(\"file\", \"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("name=\"FlashVars\" value=\"file=(http.*?)http://(www\\.)?submityourflicks\\.com").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("contentUrl\" content=\"(http://videos\\.cdn\\.submityourflicks\\.com/[^\"]+)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("file[\t\n\r ]*?:[\t\n\r ]*?\"(http[^<>\"]+)\"").getMatch(0);
        }
        if (dllink == null) {
            final String clip = PluginJSonUtils.getJsonNested(br.toString(), "clip");
            dllink = new Regex((clip != null ? clip : ""), "url\\s*:\\s*'(.*?)'").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + getFileNameExtensionFromString(dllink, ".mp4"));
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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