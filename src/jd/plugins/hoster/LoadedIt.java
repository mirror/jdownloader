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

import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loaded.it" }, urls = { "http://(www\\.)?loaded\\.it/(show/[a-z0-9]+/[A-Za-z0-9_\\-% \\.]+|(flash|divx)/[a-z0-9]+/)" }, flags = { 0 })
public class LoadedIt extends PluginForHost {

    public LoadedIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://loaded.it/agb/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Datei konnte nicht gefunden werden") || !br.containsHTML("<FORM ACTION=\"https://www\\.paypal\\.com/cgi-bin/webscr")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Set random name so we don't have problems when downloading
        if (new Regex(parameter.getDownloadURL(), ".*?(flash|divx)/[a-z0-9]+").matches()) parameter.setName(Integer.toString(new Random().nextInt(1000000)));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.requestFileInformation(link);
        String postCode = br.getRegex("name=\"code\" value=\"(.*?)\"").getMatch(0);
        if (postCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("var time_wait = (.*?);").getMatch(0);
        int tt = 10;
        if (ttt != null) tt = Integer.parseInt(ttt);
        sleep(tt * 1001, link);
        br.postPage(link.getDownloadURL(), "code=" + Encoding.urlEncode(postCode));
        String dllink = br.getRegex("(\\'|\")(http://stor\\d+\\.loaded\\.it/movie/.*?)(\\'|\")").getMatch(1);
        if (dllink == null) {
            dllink = br.getRegex("<param name=\"src\" value=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<embed src=\"(http://.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = BrowserAdapter.openDownload(br, link, dllink, true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
