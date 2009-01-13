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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class DatenGigantCom extends PluginForHost {
    private static final Pattern PATTERN_FILENAME = Pattern.compile("<td align=left width=100px><b>File Name:</b></td>\\s+?$\\s+^\\s+<td align=left width=150px>(.+?)</td>", Pattern.MULTILINE | Pattern.UNIX_LINES);
    private static final Pattern PATTERN_FILESIZE = Pattern.compile("<td align=left><b>File Groesse:</b></td>\\s+?$\\s+?^\\s+?<td align=left>(.+?)</td>", Pattern.MULTILINE | Pattern.UNIX_LINES);
    private static final Pattern PATTERN_OFFLINE = Pattern.compile("Die angeforderte Datei wurde nicht gefunden<br />");
    private static final String AGB_LINK = "http://www.datengigant.com/en/rules.php";
    private static final Pattern PATTERN_DL_URL = Pattern.compile("<input type=button id=downloadbtn class=button_download name=downloadbtn value='bitte warten ' onclick='if\\(timeout>0\\) \\{alert\\(\"Bitte warte einen Moment, waehrend die Datei aus der Datenbank abgefragt wird!\"\\);return false;\\}document.location=\"(.+?)\"; this.disabled=true;'>");

    public DatenGigantCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.clearCookies(getHost());
            String url = downloadLink.getDownloadURL();
            br.getPage(url);
            downloadLink.setName(br.getRegex(PATTERN_FILENAME).getMatch(0));
            downloadLink.setDownloadSize(Regex.getSize(br.getRegex(PATTERN_FILESIZE).getMatch(0)));
            if (br.getRegex(PATTERN_OFFLINE).matches() || !br.getRegex(PATTERN_DL_URL).matches()) { return false; }
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        String dUrl = br.getRegex(PATTERN_DL_URL).getMatch(0);
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(dUrl));
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
