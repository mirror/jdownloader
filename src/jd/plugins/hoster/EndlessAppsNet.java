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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision", interfaceVersion = 2, names = { "endlessapps.net" }, urls = { "http://[\\w\\.]*?endlessapps\\.net/dl\\.php\\?file=.+" }, flags = { 0 })
public class EndlessAppsNet extends PluginForHost {

    public EndlessAppsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://endlessapps.net/legal.php";
    }

    
    
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String infolink = link.getDownloadURL();
        br.getPage(infolink);
        String dllink;
        dllink = br.getRegex("<input type=\"submit\" class=\"button\" value=\"Download\" onClick=\"window.location='(.*?)'\" >").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.setFinalFileName(null);
        br.setDebug(true);
        dl = br.openDownload(link, dllink, false, 1);
        dl.startDownload();
        

    }

    @Override
   
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("<p>File: </p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<p>File: (.*?)</p>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    
    

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 6526 $");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 6;
    }

}