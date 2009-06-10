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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class Dataupde extends PluginForHost {
    public Dataupde(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public String getAGBLink() {
        return "http://www.dataup.de/agb";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        String id = new Regex(downloadLink.getDownloadURL(), "dataup\\.de/(\\d+)").getMatch(0);
        br.getPage("http://dataup.de/data/api/status.php?id=" + id.trim());
        String[] data = br.getRegex("(.*?)#(\\d+)#(\\d+)#(.*)").getRow(0);
        downloadLink.setFinalFileName(data[0]);
        downloadLink.setDownloadSize(Long.parseLong(data[1]));
        if (data[2].equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setMD5Hash(data[3].trim());
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        this.setBrowserExclusive();
        br.setCookie("http://www.dataup.de/", "language", "en");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());

        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        /* 10 seks warten, kann weggelassen werden */

        /* DownloadLink holen */
        Form form;
        if (br.containsHTML("class=\"button_divx\" value")) {
            form = br.getForms()[3];
            br.submitForm(form);
            form = br.getForms()[2];
        } else {
            form = br.getForms()[2];
        }
        this.sleep(10000, downloadLink);

        dl = br.openDownload(downloadLink, form, true, 1);

        if (dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        /* DownloadLimit? */
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 180000);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {

    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
