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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class DataHu extends PluginForHost {

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());   
        br.getPage( downloadLink.getDownloadURL());
        String[] dat = br.getRegex("<div class=\"download_filename\">(.*?)<\\/div>.*\\:(.*?)<div class=\"download_not_start\">").getRow(0);
        long length = Regex.getSize(dat[1].trim());
        downloadLink.setDownloadSize(length);
        downloadLink.setName(dat[0].trim());
        return true;

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        br.setFollowRedirects(true);
        getFileInformation(downloadLink);
        String link = br.getRegex(Pattern.compile("window.location.href='(.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0);

        // data.hu lässt max 3 verbindungen zu.. ob die durch chunkload oder
        // sumultane verbindungen zu stande kommen ist egal...
        int free = this.waitForFreeConnection(downloadLink);

        RAFDownload.download(downloadLink, br.openGetConnection(link), true, free * -1);

    }

    public int getTimegapBetweenConnections() {
        return 500;
    }

    public int getMaxConnections() {
        return 3;
    }

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