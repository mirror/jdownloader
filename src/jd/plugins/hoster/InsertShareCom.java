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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "insertshare.com" }, urls = { "http://[\\w\\.]*?insertshare\\.com/download/[0-9A-Z-]+" }, flags = { 0 })
public class InsertShareCom extends PluginForHost {

    public InsertShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.insertshare.com/?p=terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Invalid download link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Currently a lot of free users are downloading files")) return AvailableStatus.UNCHECKABLE;
        String filename = br.getRegex("<h3>Downloading(.*?)\\|.*?</h3>").getMatch(0);
        String filesize = br.getRegex("<h3>Downloading.*?\\|(.*?)</h3>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Currently a lot of free users are downloading files")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 60 * 1000l);
        Form dlform = br.getForm(0);
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waittime = br.getRegex("display_c\\((\\d+)\\)").getMatch(0);
        int sleep = 60;
        if (waittime != null)sleep= Integer.parseInt(waittime);
        if (sleep < 180) {
            sleep(sleep * 1001, downloadLink);
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sleep * 1001l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, false, 1);
        if (dl.getConnection().getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}