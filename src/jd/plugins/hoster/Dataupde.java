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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dataup.de", "dataup.to" }, urls = { "http://[\\w\\.]*?dataup\\.de/\\d+/(.*)", "http://[\\w\\.]*?dataup\\.to/\\d+/(.*)" }, flags = { 0, 0 })
public class Dataupde extends PluginForHost {
    public Dataupde(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dataup.to/agb";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        correctURL(downloadLink);
        this.setBrowserExclusive();
        try {
            String id = new Regex(downloadLink.getDownloadURL(), "dataup\\.de|to/(\\d+)").getMatch(0);
            br.getPage("http://dataup.to/data/api/status.php?id=" + id.trim());
            String[] data = br.getRegex("(.*?)#(\\d+)#(\\d+)#(.*)").getRow(0);
            downloadLink.setFinalFileName(data[0]);
            downloadLink.setDownloadSize(Long.parseLong(data[1]));
            if (data[2].equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setMD5Hash(data[3].trim());
            return AvailableStatus.TRUE;
        } catch (NullPointerException e) {
            return AvailableStatus.FALSE;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        this.setBrowserExclusive();
        br.setCookie("http://www.dataup.to/", "language", "en");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());

        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        int maxchunks = 1;
        /* DownloadLink holen */
        String dllink = null;
        if (br.containsHTML("DivXBrowserPlugin.cab")) {
            // Stream-links handling, also when downloading streams you can
            // download the file with multiple connections (chunks)
            maxchunks = 0;
            dllink = br.getRegex("video/divx\" src=\"(.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://q[0-9]+\\.dataup\\.to:[0-9]+/download\\.php\\?id=[0-9]+\\&name=.*?)\"").getMatch(0);
        } else {
            // Normal-links handling
            dllink = br.getRegex("div align=\"center\">.*?<form action=\"(.*?)\"").getMatch(0);
        }
        /* 10 seks warten, kann weggelassen werden */

        // this.sleep(10000, downloadLink);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxchunks);

        if (dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        /* DownloadLimit? */
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void correctURL(DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace(".de/", ".to/"));

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
