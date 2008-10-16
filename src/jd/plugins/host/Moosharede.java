//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class Moosharede extends PluginForHost {
    public Moosharede(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mooshare.de/infos.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() == null) {
                String filename = br.getRegex("<center>Dateiname: <b>(.*?)</b>").getMatch(0);
                if (filename != "") {
                    downloadLink.setName(filename);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* DownloadLink holen */
        String url = br.getRegex("popup\\('(.*?)',").getMatch(0);
        br.openGetConnection(url);

        /* Datei herunterladen */

        dl = RAFDownload.download(downloadLink, br.createGetRequest(null), true, 0);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
