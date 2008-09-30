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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class YourFileSendercom extends PluginForHost {

    public YourFileSendercom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfilesender.com/terms.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        String downloadurl = downloadLink.getDownloadURL();
        this.setBrowserExclusive();
        br.getPage(downloadurl);
        if (!br.containsHTML("alert('File Not Found")) {
            String linkInfo[] = new Regex(br, Pattern.compile("<P>You have requested the file <strong>(.*?)</strong> \\(([0-9\\.,]+) (.*?)\\)\\.<br", Pattern.CASE_INSENSITIVE)).getRow(0);

            if (linkInfo[2].matches("KBytes")) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkInfo[1].replaceAll(",", "").replaceAll("\\.0+", "")) * 1024.0));
            }
            downloadLink.setName(linkInfo[0]);
            return true;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        if (br.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000L);

        }

        String link = Encoding.htmlDecode(new Regex(br, Pattern.compile("unescape\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        /* 10 Seks warten */
        sleep(10000, downloadLink);

        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, link);
        dl.setFilesizeCheck(false);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
