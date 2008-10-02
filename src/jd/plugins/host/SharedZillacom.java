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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class SharedZillacom extends PluginForHost {

    private String passCode = "";

    public SharedZillacom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://sharedzilla.com/en/terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        br.getPage(downloadLink.getDownloadURL());
        if (!br.containsHTML("Upload not found")) {
            downloadLink.setName(br.getRegex("nowrap title=\"(.*?)\">").getMatch(0));
            downloadLink.setDownloadSize(Regex.getSize(br.getRegex("<span title=\"(.*?)\">").getMatch(0)));
            return true;
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        /* ID holen */
        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("get\\?id=(\\d+)", Pattern.CASE_INSENSITIVE)).getMatch(0);

        /* Password checken */
        if (br.containsHTML("Password protected")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }
        /* Free Download starten */
        br.setFollowRedirects(false);
        br.postPage("http://sharedzilla.com/en/downloaddo", "id=" + id + "&upload_password=" + Encoding.urlEncode(passCode));
        if (br.getRedirectLocation() == null) {
            // br.postPage("http://sharedzilla.com/en/downloaddo", "id=" + id +
            // "&upload_password=" + Encoding.urlEncode(passCode));
            if (br.containsHTML("<p>Password is wrong!</p>")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);

        br.openDownload(downloadLink, br.getRedirectLocation(), true, 1).startDownload();
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
