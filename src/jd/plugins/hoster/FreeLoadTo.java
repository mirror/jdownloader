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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeload.to" }, urls = { "http://[\\w\\.]*?(freeload|mcload)\\.to/(divx\\.php\\?file_id=|\\?Mod=Divx\\&Hash=)[a-z0-9]+" }, flags = { 0 })
public class FreeLoadTo extends PluginForHost {

    public FreeLoadTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://freeload.to/disclaimer.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        String fileid = new Regex(link.getDownloadURL(), "divx\\.php\\?file_id=([a-z0-9]+)").getMatch(0);
        if (fileid != null) link.setUrlDownload("http://freeload.to/?Mod=Divx&Hash=" + fileid);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        // String jscookiepage =
        // br.getRegex("javascript\" src=\"(.*?)\"").getMatch(0);
        // String cookie = br.getRegex("onload=.*?'(.*?)'").getMatch(0);
        // String onload = br.getRegex("onload=.*?'(/.*?)'").getMatch(0);
        // if (jscookiepage != null) {
        // /* this cookie is needed to reach the site */
        // Browser brc = br.cloneBrowser();
        // brc.getPage("http://freeload.to" + jscookiepage);
        // String cookie2 = brc.getRegex("escape.*?\"(.*?)\"").getMatch(0);
        // br.setCookie("http://freeload.to", "sitechrx", cookie + cookie2);
        // }
        // br.getPage(onload);
        if (br.containsHTML("player_not_found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\".*?/uploads/.*?/.*?/.*?/(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("video/divx\" src=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("src\" value=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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