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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(names = { "usershare.net"}, urls ={ "http://[\\w\\.]*?usershare\\.net/[0-9a-zA-Z.]+"}, flags = {0})
public class Usershare extends PluginForHost {

    public Usershare(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://usershare.net/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String linkurl = br.getRegex(".*?flashvars=.*?<a href=\"(.*?)\"><img src=\"/images/download_btn.jpg\" border=0>").getMatch(0);
        if (linkurl == null) {
            linkurl = br.getRegex(".*?document.oncontextmenu=new Function.*?<a href=\"(.*?)\"><img src=\"/images/download_btn.jpg\" border=0>").getMatch(0);
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        br.setFollowRedirects(true);
        dl = br.openDownload(link, linkurl, true, -10);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist") || br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String filesize = br.getRegex("Size:</b></td><td>(.*?)<small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }
}
