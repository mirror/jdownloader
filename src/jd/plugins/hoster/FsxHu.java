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

import java.io.BufferedInputStream;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsx.hu" }, urls = { "http://s.*?.fsx.hu/.+/.+" }, flags = { 0 })
public class FsxHu extends PluginForHost {

    public FsxHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fsx.hu/index.php?m=home&o=szabalyzat";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("V.lassz az ingyenes let.lt.s .s a regisztr.ci. k.z.l!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        br.getPage("http://www.fsx.hu/download.php?i=1");
        String filename = br.getRegex("<font color=\"#FF0000\" size=\"4\">(.+?)</font>").getMatch(0);
        String filesize = br.getRegex("<strong>M.ret:</strong> (.+?) B.jt").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    private void downloadImage(String url) throws Exception {
        jd.http.Browser br1 = br.cloneBrowser();
        URLConnectionAdapter con = br1.openGetConnection(url);
        BufferedInputStream input = new BufferedInputStream(con.getInputStream());

        byte[] b = new byte[1024];
        while (input.read(b) != -1) {
        }
        input.close();

        con.disconnect();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies("www.fsx.hu");
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);

        downloadImage("http://www.fsx.hu/img/button-letoltes1.gif");
        downloadImage("http://www.fsx.hu/img/bg0.gif");
        downloadImage("http://www.fsx.hu/img/bg1dl.gif");
        downloadImage("http://www.fsx.hu/img/bg3.gif");
        downloadImage("http://www.fsx.hu/img/bg4b.gif");
        downloadImage("http://www.fsx.hu/img/bg5.gif");
        downloadImage("http://www.fsx.hu/img/bg6.gif");
        downloadImage("http://www.fsx.hu/img/style.css");
        br.getPage("http://www.fsx.hu/download.php");

        while (downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {
            String url1 = br.getRegex("<a id='dlink' href=\"(.+?)\">").getMatch(0);
            String url2 = br.getRegex("elem\\.href = elem\\.href \\+ \"(.+?)\";").getMatch(0);
            if (url1 != null && url2 != null) {
                String url = url1 + url2;
                dl = BrowserAdapter.openDownload(br, downloadLink, url);
                dl.startDownload();
                return;
            }

            String serverQueueLength = br.getRegex("<font color=\"#FF0000\"><strong>(\\d+?)</strong></font> felhaszn.l. van el.tted").getMatch(0);

            if (serverQueueLength == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            // next run of handleFree() will report the file as deleted
            // if it is really deleted because fsx.hu sometimes reports
            // timeouted sessions as non-existing/removed downloads
            if (br.containsHTML("A kiv.lasztott f.jl nem tal.lhat. vagy elt.vol.t.sra ker.lt."))
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 1 * 1000l);
            else if (br.containsHTML("A kiv.lasztott f.jl let.lt.s.t nem kezdted meg")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 1000l);

            this.sleep(15000L, downloadLink, JDL.LF("plugins.hoster.fsxhu.waiting", "%s users in queue, ", serverQueueLength));

            br.getPage("http://www.fsx.hu/download.php");
        }
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public int getMaxConnections() {
        return 1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
