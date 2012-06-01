//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 15714 $", interfaceVersion = 2, names = { "filecondo.com" }, urls = { "http://(\\w+\\.)?filecondodecrypted\\.com/download_regular_active\\.php\\?file=[A-Za-z0-9]+\\&part=\\d+" }, flags = { 0 })
public class FileCondoCom extends PluginForHost {

    public FileCondoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filecondo.com/term.php";
    }

    /** Links come from a decrypter */
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("filecondodecrypted.com/", "filecondo.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        final String mainLink = link.getStringProperty("mainlink");
        if (mainLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(mainLink);
        if (br.containsHTML("à¹„à¸¡à¹ˆà¸žà¸šà¹„à¸Ÿà¸¥à¹Œ / Link à¸œà¸´à¸”") || br.toString().length() < 200) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            final Regex waittime = br.getRegex("ฟล์เท่านั้น<br/>กรุณาลองใหม่อีกครั้งในเวลา (\\d+):(\\d+):(\\d+) <br/>");
            final String hours = waittime.getMatch(0);
            final String minutes = waittime.getMatch(1);
            final String seconds = waittime.getMatch(2);
            if (hours != null && minutes != null && seconds != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, ((Integer.parseInt(hours) * 60 * 60) + Integer.parseInt(minutes) * 60) * 1001l);
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