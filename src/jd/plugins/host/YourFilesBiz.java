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

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class YourFilesBiz extends PluginForHost {
    private static final String HOST = "yourfiles.biz";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?yourfiles\\.biz/\\?d\\=[a-zA-Z0-9]+",Pattern.CASE_INSENSITIVE);

    public YourFilesBiz() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setCookiesExclusive(true);
        br.clearCookies(HOST);
        br.getPage(downloadLink.getDownloadURL());

        downloadLink.setName(br.getRegex("<td align=left width=20%><b>Dateiname:</b></td>[\\s]*?<td align=left width=80%>(.*?)</td>").getMatch(0).trim());
        downloadLink.setDownloadSize(Regex.getSize(br.getRegex("<td align=left><b>Dateigr.*?e:</b></td>.*?<td align=left>(.*?)</td>").getMatch(0).trim()));

        return true;

    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws IOException {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setCookiesExclusive(true);
        br.clearCookies(HOST);

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        HTTPConnection urlConnection = br.openGetConnection("http://" + new Regex(br.getPage(downloadLink.getDownloadURL()), Pattern.compile("value='http://(.*?)'>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        dl = new RAFDownload(this, downloadLink, urlConnection);
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