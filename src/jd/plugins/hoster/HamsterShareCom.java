//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hamstershare.com" }, urls = { "http://[\\w\\.]*?hamstershare\\.com/dload/[0-9a-z]+" }, flags = { 0 })
public class HamsterShareCom extends PluginForHost {

    public HamsterShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setCookie("http://hamstershare.com", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not exists")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div id=\"h2\">(.*?)</div>").getMatch(0);
        String fileSize = br.getRegex("<b>File Size: (.*?)</b></div>").getMatch(0);
        if (filename == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("window\\.document\\.location\\.href='(.*?)'\"").getMatch(0);

        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -20);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public String getAGBLink() {
        return "http://hamstershare.com/terms";
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
