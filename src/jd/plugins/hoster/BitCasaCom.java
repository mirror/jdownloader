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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitcasa.com" }, urls = { "https://my\\.bitcasa\\.com/send/[a-z0-9]+/[a-z0-9]+|https?://l\\.bitcasa\\.com/[A-Za-z0-9\\-]+" }, flags = { 0 })
public class BitCasaCom extends PluginForHost {

    public BitCasaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.bitcasa.com/legal";
    }

    private static final String TYPE_NORMAL = "https://my\\.bitcasa\\.com/send/[a-z0-9]+/[a-z0-9]+";
    private static final String TYPE_SHORT  = "https?://l\\.bitcasa\\.com/[A-Za-z0-9\\-]+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.getURL().matches(TYPE_NORMAL) && !br.getURL().matches(TYPE_SHORT)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (link.getDownloadURL().matches(TYPE_SHORT)) {
            if (!br.getURL().matches(TYPE_NORMAL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setUrlDownload(br.getURL());
        }
        /* Filename/size is not available for password protected links */
        if (br.containsHTML("type=\"password\" name=\"password\"")) {
            link.getLinkStatus().setStatusText("This link is password protected");
            return AvailableStatus.TRUE;
        }
        final String filename = br.getRegex("\"name\": \"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("\"size\": (\\d+)").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String passCode = downloadLink.getStringProperty("pass", null);
        requestFileInformation(downloadLink);
        if (br.containsHTML("type=\"password\" name=\"password\"")) {
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            }
            if (passCode == null || passCode.equals("")) {
                passCode = null;
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML("type=\"password\" name=\"password\"")) {
                passCode = null;
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            downloadLink.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("\"(/download\\-send/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "https://my.bitcasa.com" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}