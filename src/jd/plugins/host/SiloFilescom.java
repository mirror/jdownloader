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
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class SiloFilescom extends PluginForHost {

    public SiloFilescom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://silofiles.com/regeln.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() == null) {
            downloadLink.setName(br.getRegex("Dateiname:<b>(.*?)</b>").getMatch(0));
            downloadLink.setDownloadSize(Regex.getSize(br.getRegex("Dateigr.*?e:<b>(.*?)</b></tr>").getMatch(0)));
            return true;
        }
        return false;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        /* Downloadlimit */
        if (br.containsHTML("<span>Maximale Parallele")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);

        /* Datei herunterladen */
        br.openDownload(downloadLink, br.getRegex("document.location=\"(.*?)\"").getMatch(0)).startDownload();
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
