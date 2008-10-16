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
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class Dataupde extends PluginForHost {
    public Dataupde(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    private String downloadurl;

    @Override
    public String getAGBLink() {
        return "http://www.dataup.de/agb";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            downloadurl = downloadLink.getDownloadURL();
            br.getPage(downloadurl);

            if (!Regex.matches(br, "\\>Fehler\\!\\<")) {
                String filename = br.getRegex("helvetica;\">(.*?)</div>").getMatch(0);
                String filesizeString = br.getRegex("<label>Größe: (.*?)<\\/label><br \\/>").getMatch(0);
                downloadLink.setDownloadSize(Regex.getSize(filesizeString));
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
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

        /* 10 seks warten, kann weggelassen werden */
        // this.sleep(10000, downloadLink);
        /* DownloadLink holen */
        Form form = br.getForms()[2];

        br.setFollowRedirects(false);

        dl = new RAFDownload(this, downloadLink, br.createFormRequest(form));
        dl.setChunkNum(1);
        dl.setResume(false);
        HTTPConnection urlConnection = dl.connect(br);
        /* Datei herunterladen */
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        /* DownloadLimit? */
        if (br.getRedirectLocation() != null) {
            linkStatus.setValue(120000L);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
