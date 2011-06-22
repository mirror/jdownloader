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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedfile.cz" }, urls = { "http://(www\\.)?speedfile\\.cz/((cs|en|de)/)?\\d+/[a-z0-9\\-]+" }, flags = { 0 })
public class SpeedFileCz extends PluginForHost {

    public SpeedFileCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://speedfile.cz/pages/terms/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(cs|en|de)/", "").replace("speedfile.cz/", "speedfile.cz/en/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h1>Page Not Found</h1>|<p>The page you requested could not be found|or that the page no longer exists\\.|>Tento soubor byl odstraněn)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Speedfile \\| (.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1><span id=\"vyraz\">(.*?)</span></h1>").getMatch(0);
            }
        }
        String filesize = br.getRegex("><big>(.*?) \\| Downloaded ").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String continueLink = br.getRegex("id=\"request\" class=\"caps\" href=\"(/.*?)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("\"(/file/download/\\d+\\?hash=[a-z0-9]+)\"").getMatch(0);
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        continueLink = "http://speedfile.cz" + continueLink;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(continueLink);
        sleep(10 * 1001l, downloadLink);
        br.getPage(continueLink);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}