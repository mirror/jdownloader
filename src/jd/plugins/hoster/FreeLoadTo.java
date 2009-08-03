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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//movshare by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeload.to" }, urls = { "http://[\\w\\.]*?freeload\\.to/divx\\.php\\?file_id=[a-z|0-9]+" }, flags = { 0 })
public class FreeLoadTo extends PluginForHost {

    public FreeLoadTo(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
        setBrowserExclusive();
    }

    @Override
    public String getAGBLink() {
        return "http://freeload.to/disclaimer.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("player_not_found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex(Pattern.compile("ideo/divx\" src=\"(.*?)\" custo")).getMatch(0);
        dl = br.openDownload(downloadLink, dllink, true, -20);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}