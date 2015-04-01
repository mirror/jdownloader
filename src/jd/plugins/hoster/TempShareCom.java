//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "temp-share.com" }, urls = { "https?://(www\\.)?temp-share\\.com/f/[a-z0-9]+" }, flags = { 0 })
public class TempShareCom extends PluginForHost {

    private static final String mainPage = "http://temp-share.com";

    /**
     * @author raztoki
     */
    public TempShareCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
    }

    @Override
    public String getAGBLink() {
        return mainPage + "/company/terms";
    }

    private static final String MAINTENANCE = ">UNDER CONSTRUCTION<";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());

        if (br.containsHTML(MAINTENANCE)) {
            link.getLinkStatus().setStatusText("Site is under maintenance");
            return AvailableStatus.UNCHECKABLE;
        } else if (!br.getURL().contains("/f/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">Time has expired for this file\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final String filename = br.getRegex("id=\\'filename\\'>([^<>\"]*?)</h1>").getMatch(0);
        final String filesize = br.getRegex("\\((\\d{1,4}(?:\\.\\d{1,2})? (?:MB|KB|GB))\\)").getMatch(0);

        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(MAINTENANCE)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Site is under maintenance", 3 * 60 * 1000l);
        }
        final String server = br.getRegex("\\'(https?://[^<>\"]*?/download)\\'").getMatch(0);
        final String publickey = br.getRegex("id=\\'publickey\\' value=\\'([a-z0-9]+)\\'").getMatch(0);
        if (server == null || publickey == null) {
            logger.warning("Could not find 'dllink', please report this issue to the JDownloader Development Team");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dllink = server + "/" + publickey + "/";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}