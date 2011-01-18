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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archiv.to" }, urls = { "http://[\\w\\.]*?archiv\\.to/(\\?Module\\=Details\\&HashID\\=|GET/)FILE[A-Z0-9]+" }, flags = { 0 })
public class ArchivTo extends PluginForHost {

    public ArchivTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://archiv.to/Legal.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(The desired file could not be found|Maybe owner deleted it or check your Hash again)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Originaldatei</td>.*?<td class=.*?>: <a href=\".*?>(.*?)</a>").getMatch(0);
        String filesize = br.getRegex(">Dateigr.+e</td>.*?<td class=.*?>:(.*?)\\(").getMatch(0);
        if (filesize == null) filesize = br.getRegex("\\(~(.*?)\\)").getMatch(0);
        String md5hash = br.getRegex(">MD5-Hash</td>.*?<td class=.*?>:(.*?)<").getMatch(0);
        if (filename == null && filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename == null) filename = new Regex(downloadLink.getDownloadURL(), ".*?FILE([A-Z0-9]+)").getMatch(0) + ".flv";
        downloadLink.setFinalFileName(filename);
        if (md5hash != null) downloadLink.setMD5Hash(md5hash.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(downloadLink);
        br.setDebug(true);
        String browsercontent = Encoding.htmlDecode(br.toString());
        if (!browsercontent.contains("/gat/") && !browsercontent.contains("/GAT/")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String fileid = new Regex(downloadLink.getDownloadURL(), ".*?(FILE[A-Z0-9]+)").getMatch(0);
        String dllink = "http://archiv.to/GAT/" + fileid;
        dllink = Encoding.htmlDecode(dllink);
        // If it is a flash link we need this
        if (br.containsHTML("name=\"flashvars\"")) dllink = dllink + "/?start=0";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
