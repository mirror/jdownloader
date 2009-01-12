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
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FilezzzCom extends PluginForHost {

    public FilezzzCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filezzz.com/terms.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<font size=6>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String downloadSize = (br.getRegex(Pattern.compile("file size (.*?)\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (downloadName == null || downloadSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(downloadName.trim());
        downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        this.sleep(4000l, downloadLink);
        /* Seite aktualisieren */
        br.getPage(downloadLink.getDownloadURL());
        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href='(http://open\\.filezzz\\.com/.*?)'>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl = br.openDownload(downloadLink, linkurl, true, -2);
        HTTPConnection con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}