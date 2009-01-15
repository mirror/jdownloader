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
import java.util.regex.Pattern;
import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class SavefileCom extends PluginForHost {

    public SavefileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.savefile.com/tos.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setDebug(true);
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        br.getPage(url);
        if (!br.containsHTML("File not found")) {
            downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("Filename: (.*?)	<br /> ", Pattern.CASE_INSENSITIVE)).getMatch(0));
            downloadSize = (br.getRegex(Pattern.compile("Filesize: (.*?)	<br />", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (!(downloadName == null || downloadSize == null)) {
                downloadLink.setName(downloadName);
                downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                return true;
            }
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        /* Link holen */
        String[][] ids = new Regex(br, Pattern.compile("ShowDownloadDialog\\('([0-9]+)', '([0-9a-zA-Z]+)'\\);", Pattern.CASE_INSENSITIVE)).getMatches();
        String fileID = ids[0][0];
        String sessionID = ids[0][1];
        if (fileID == null | sessionID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        br.getPage("http://www.savefile.com/downloadmax/" + fileID + "?PHPSESSID=" + sessionID);
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href=\"(.*?)\">Download file now", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl, true, 0);
        HTTPConnection con = dl.getConnection();
        if (con.getResponseCode() == 416) {
            // HTTP/1.1 416 Requested Range Not Satisfiable
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
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