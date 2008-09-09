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

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    private static final String HARDWARE_DEFECT = "Hardware-Defekt!";

    private static final String NOT_FOUND = "Datei existiert nicht";

    public FastLoadNet(String cfgName) {
        super(cfgName);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadurl);

        if (br.containsHTML(NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }

        if (br.containsHTML(HARDWARE_DEFECT)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4));
            return false;
        }

        String fileName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<th.*?><b>Datei</b></th>.*?<font.*?;\">(.*?)</font>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        long length = 0;
        // downloadinfos gefunden? -> download verfügbar
        if (fileName != null) {
            downloadLink.setName(fileName.trim());
            downloadLink.setDownloadSize(length);
            return true;
        }
        downloadLink.setName(downloadurl.substring(downloadurl.indexOf("pid=") + 4));
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
        if (br.containsHTML(NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        if (br.containsHTML(HARDWARE_DEFECT)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        String dl_url = br.getRegex(Pattern.compile("type=\"button\" onclick=\"location='(.*?)'\" value=\"download", Pattern.CASE_INSENSITIVE)).getMatch(0);

        // Download vorbereiten
        HTTPConnection urlConnection = br.openGetConnection(dl_url);
        long length = urlConnection.getContentLength();

        if (urlConnection.getContentType() != null) {

            if (urlConnection.getContentType().contains("text/html")) {

                if (length == 184) {
                    logger.info("System overload: Retry in 20 seconds");
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    linkStatus.setValue(20 * 60 * 1000l);
                    return;
                } else if (length == 169) {
                    logger.severe("File not found: File is deleted from Server");
                    linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    return;
                } else {
                    logger.severe("Unknown error page - [Length: " + length + "]");
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                    return;
                }
            }
            // Download starten
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setResume(false);
            dl.setChunkNum(1);
            dl.startDownload();
            return;

        } else {
            logger.severe("Couldn't get HTTP connection");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
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