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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "supershare.pl" }, urls = { "http://[\\w\\.]*?supershare\\.pl/\\?d=[A-F0-9]+" }, flags = { 0 })
public class SuperSharePl extends PluginForHost {

    public SuperSharePl(PluginWrapper wrapper) {
        super(wrapper);
        // this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://supershare.pl/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://supershare.pl", "yab_mylang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File size:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // br.setDebug(true);
        String filename = downloadLink.getName();
        String getlink = br.getRegex("downloadlink\\s+=\\s+'(.*?)';").getMatch(0);
        if (getlink == null) getlink = br.getRegex("file=(.*?)\"").getMatch(0);
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        // this.sleep(10000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, false, 1);
        downloadLink.setFinalFileName(filename);
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
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
