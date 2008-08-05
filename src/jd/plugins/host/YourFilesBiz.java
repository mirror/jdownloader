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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class YourFilesBiz extends PluginForHost {

    private static final String CODER = "eXecuTe";
    private static final String DOWNLOAD_LINK = "value='http://°'>";
    private static final String DOWNLOAD_NAME = "<td align=left width=20%><b>Dateiname:</b></td>\n       <td align=left width=80%>°</td>";

    // Suchmasken
    private static final String DOWNLOAD_SIZE = "  <tr class=tdrow1>°<td align=left><b>Dateigr°e:</b></td>°<td align=left>°</td>°</tr>";
    private static final String HOST = "yourfiles.biz";

    private static final int MAX_SIMULTAN_DOWNLOADS = Integer.MAX_VALUE;
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?yourfiles\\.biz/\\?d\\=[a-zA-Z0-9]+");

    private static final String PLUGIN_NAME = HOST;
    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1.0";
    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    private String downloadURL = "";
    private HTTPConnection urlConnection;

    public YourFilesBiz() {

        super();

        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

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
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        try {

            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));

            if (requestInfo.getHtmlCode().equals("")) {
                logger.severe("download not found");
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return false;
            }

            String fileName = Encoding.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_NAME, 0));
            Integer length = getFileSize(requestInfo.getHtmlCode());

            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadSize(length);
                } catch (Exception e) {
                }

                return true;

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // unbekannter fehler
        return false;

    }

    private int getFileSize(String source) {

        int size = 0;
        String sizeString = Encoding.htmlDecode(SimpleMatches.getSimpleMatch(source, DOWNLOAD_SIZE, 3));
        if (sizeString == null) {
            sizeString = "";
        }

        if (sizeString.contains("MB")) {
            sizeString = SimpleMatches.getSimpleMatch(sizeString, "° MB", 0);
            size = (int) Math.round(Double.parseDouble(sizeString) * 1024 * 1024);
        } else if (sizeString.contains("KB")) {
            sizeString = SimpleMatches.getSimpleMatch(sizeString, "° KB", 0);
            size = (int) Math.round(Double.parseDouble(sizeString) * 1024);
        } else if (sizeString.contains("Byte")) {
            sizeString = SimpleMatches.getSimpleMatch(sizeString, "° Byte", 0);
            size = (int) Math.round(Double.parseDouble(sizeString));
        }

        return size;

    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
   */ public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        URL downloadUrl = new URL(downloadLink.getDownloadURL());

        // switch (step.getStep()) {

        // case PluginStep.STEP_PAGE:

        requestInfo = HTTP.getRequest(downloadUrl);

        // serverantwort leer (weiterleitung) -> download nicht verfügbar
        if (requestInfo.getHtmlCode().equals("")) {
            logger.severe("download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        String fileName = Encoding.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_NAME, 0));
        downloadLink.setName(fileName);

        try {
            int length = getFileSize(requestInfo.getHtmlCode());
            downloadLink.setDownloadSize(length);
        } catch (Exception e) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // downloadLink auslesen
        downloadURL = "http://" + Encoding.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_LINK, 0));

        // case PluginStep.STEP_WAIT_TIME:

        // Download vorbereiten
        downloadLink.getLinkStatus().setStatusText("Verbindung aufbauen");
        urlConnection = new HTTPConnection(new URL(downloadURL).openConnection());

        // case PluginStep.STEP_DOWNLOAD:

        // Download starten
        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.startDownload();

    }

    @Override
    public void reset() {

        downloadURL = "";
        urlConnection = null;

    }

    @Override
    public void resetPluginGlobals() {
    }

}