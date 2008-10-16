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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class Przeslijnet extends PluginForHost {

    public Przeslijnet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www2.przeslij.net/#";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (!br.containsHTML("Invalid download link")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("<font color=#000000>(.*?)</font>").getMatch(0)));
                downloadLink.setDownloadSize(Regex.getSize(br.getRegex("File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*?)</td>").getMatch(0)));
                return true;
            }
        } catch (IOException e) {
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
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        /* Zwangswarten, 15seks */
        sleep(15000, downloadLink);

        /* Datei herunterladen */
        br.openDownload(downloadLink, Encoding.htmlDecode(br.getRegex("onClick=\"window\\.location=\\\\\'(.*?)\\\\\'").getMatch(0))).startDownload();
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
