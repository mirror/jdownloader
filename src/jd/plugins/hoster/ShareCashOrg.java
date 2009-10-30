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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharecash.org" }, urls = { "http://[\\w\\.]*?sharecash\\.org/download\\.php\\?id=[0-9]+" }, flags = { 0 })
public class ShareCashOrg extends PluginForHost {

    public ShareCashOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Jiaz keine Ahnung ob diese Cookies gesetzt werden müssen oder net...
        br.setCookie("http://69.93.2.170/", "done", "yeppp");
        br.setCookie("http://69.93.2.170/", "ref", Encoding.urlEncode(downloadLink.getDownloadURL()));
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Name:(.*?)<br>").getMatch(0);
        String filesize = br.getRegex("Size:(.*?)<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String md5 = br.getRegex("md5:(.*?)<br>").getMatch(0);
        if (md5 != null) {
            downloadLink.setMD5Hash(md5.trim());
        }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        String oid = new Regex(downloadLink.getDownloadURL(), "sharecash\\.org/download\\.php\\?id=(\\d+)").getMatch(0);
        br.getPage("http://69.93.2.170/offer2.php?oid=" + oid);
        String refresh = br.getRegex("url=(.*?)\">").getMatch(0);
        if (refresh == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://69.93.2.170/" + Encoding.htmlDecode(refresh));
        String dllink0 = br.getRegex("onClick=\"document\\..*?document\\.location='(.*?)';").getMatch(0);
        if (dllink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://69.93.2.170/" + dllink0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public String getAGBLink() {
        return "http://sharecash.org/tos.php";
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
