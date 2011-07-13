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
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drtuber.com" }, urls = { "http://(www\\.)?drtuber\\.com/(video/\\d+|player/config_embed3\\.php\\?vkey=[a-z0-9]+)" }, flags = { 0 })
public class DrTuberCom extends PluginForHost {

    private String DLLINK = null;

    public DrTuberCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.drtuber.com/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        // Check if link is an embedded link e.g. from a decrypter
        String vk = new Regex(downloadLink.getDownloadURL(), "vkey=(.+)").getMatch(0);
        if (vk != null) {
            br.getPage(downloadLink.getDownloadURL() + "&pkey=" + JDHash.getMD5(vk + Encoding.Base64Decode("bismarckPleaseHelpMe")));
            // System.out.print(br.toString());
            String finallink = br.getRegex("type=video_click\\&amp;target_url=(http.*?)</url>").getMatch(0);
            if (finallink == null) {
                logger.warning("Failed to find original link for: " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setUrlDownload(Encoding.htmlDecode(finallink));
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(This video cannot be found\\.|Are you sure you typed in the correct url\\?<)") || br.getURL().contains("missing=true")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>(.*?) - DrTuber\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 class=\"name\">(.*?)</h1>").getMatch(0);
        }
        final String vKey = br.getRegex("vkey=([0-9a-z]+)").getMatch(0);
        if (vKey == null) {
            logger.warning("vKey is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        String continueLink = br.getRegex("addVariable\\(\\'config\\', \\'(/.*?)\\'\\);").getMatch(0);
        if (continueLink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        continueLink = "http://drtuber.com" + Encoding.htmlDecode(continueLink) + "&pkey=" + JDHash.getMD5(vKey + Encoding.Base64Decode("dHJhRHJ1YkVuMnpha3U="));
        br.getPage(continueLink);
        DLLINK = br.getRegex("<video_file>(http://.*?\\.flv)</video_file>").getMatch(0);
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        final Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
