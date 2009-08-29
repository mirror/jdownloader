//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

//plunder.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plunder.com" }, urls = { "http://[\\w\\.]*?(youdownload\\.eu|binarybooty\\.com|mashupscene\\.com|plunder\\.com|files\\.youdownload\\.com)/(-download-[a-z0-9]+|.+-download-.+)\\.htm" }, flags = { 0 })
public class PlunderCom extends PluginForHost {

    public PlunderCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.plunder.com/x/tos";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (br.getURL().contains("/search/?f=")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex reg = br.getRegex("Filename: </b>(.*?) \\((.*?)\\)<BR");
        String filename = reg.getMatch(0);
        String filesize = reg.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

       String dllink = br.getRegex("<BR /><a href=\"(.*?)\">Download").getMatch(0);
       if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("You must log in to download more this session")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            }
            String checklink = br.getURL();
            if (checklink.contains("/error")){
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }
}
