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
import jd.nutils.encoding.Encoding;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loaded.it" }, urls = { "http://[\\w\\.]*?loaded\\.it/show/[a-z|0-9]+/.+" }, flags = { 0 })
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
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Datei konnte nicht gefunden werden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.requestFileInformation(link);
        br.setDebug(true);
        String postCode = br.getRegex("name=\"code\" value=\"(.*?)\"").getMatch(0);
        if (postCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = "10";
        ttt = br.getRegex("var time_wait = (.*?);").getMatch(0);
        int tt = Integer.parseInt(ttt);
        sleep(tt * 1001, link);
        br.postPage(link.getDownloadURL(), "code=" + Encoding.urlEncode(postCode));
        String server = br.getRegex("hostname\" value=\"(.*?)\"").getMatch(0);
        String hash = br.getRegex("hash\" value=\"(.*?)\"").getMatch(0);
        String filename = br.getRegex("filename\" value=\"(.*?)\"").getMatch(0);
        if (server == null || hash == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + server + "/get/" + hash + "/" + filename;
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
