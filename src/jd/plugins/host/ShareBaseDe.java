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
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class ShareBaseDe extends PluginForHost {

    private static final String HOST = "sharebase.de";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?sharebase\\.de/files/[a-zA-Z0-9]{10}\\.html", Pattern.CASE_INSENSITIVE);

    private static final String DL_LIMIT = "Das Downloaden ohne Downloadlimit ist nur mit einem Premium-Account";

    private static final String DOWLOAD_RUNNING = "Von deinem Computer ist noch ein Download aktiv";

    private static final Pattern FILEINFO = Pattern.compile("<span class=\"font1\">(.*?) </span>\\((.*?)\\)</td>", Pattern.CASE_INSENSITIVE);

    // private static final String SIM_DL = "Das gleichzeitige Downloaden";

    private static final Pattern WAIT = Pattern.compile("Du musst noch (.*?):(.*?):(.*?) warten!", Pattern.CASE_INSENSITIVE);

    public ShareBaseDe() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://sharebase.de/pp.html";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.setDebug(true);

            String page = br.getPage(downloadLink.getDownloadURL());
            logger.info(page);
            String[] infos = new Regex(page, FILEINFO).getRow(0);

            downloadLink.setName(infos[0].trim());
            downloadLink.setDownloadSize(Regex.getSize(infos[1].trim()));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        br.setDebug(true);
        String page = br.getPage(downloadLink.getDownloadURL());
        logger.info(page);
        String fileName = Encoding.htmlDecode(new Regex(page, FILEINFO).getMatch(0));

        if (br.containsHTML(DOWLOAD_RUNNING)) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(60 * 1000);
            return;
        }

        // Download-Limit erreicht
        if (br.containsHTML(DL_LIMIT)) {
            String[] temp = new Regex(page, WAIT).getRow(0);
            int waittime = 0;

            if (temp[0] != null && temp[1] != null && temp[2] != null) {
                try {
                    waittime += Integer.parseInt(temp[2]);
                    waittime += Integer.parseInt(temp[1]) * 60;
                    waittime += Integer.parseInt(temp[0]) * 60 * 60;
                } catch (Exception Exc) {
                    waittime = 600;
                }
            }

            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(waittime * 1000);
            return;
        }

        // DownloadInfos nicht gefunden? --> Datei nicht vorhanden
        if (fileName == null) {
            logger.severe("download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        fileName = fileName.trim();

        br.postPage(downloadLink.getDownloadURL(), "doit=Download+starten");
        logger.info(br.getRedirectLocation());
        // String finishURL =
        // Encoding.htmlDecode(requestInfo.getConnection().getHeaderField
        // ("Location"));
        String finishURL = Encoding.htmlDecode(br.getRedirectLocation());

        if (finishURL == null) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        // Download vorbereiten
        // HTTPConnection urlConnection = new HTTPConnection(new
        // URL(finishURL).openConnection());
        HTTPConnection urlConnection = br.openGetConnection(finishURL);

        // Download starten
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();

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
