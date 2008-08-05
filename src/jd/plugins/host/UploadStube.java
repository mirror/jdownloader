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
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class UploadStube extends PluginForHost {
    private static final String HOST = "uploadstube.de";

    static private final Pattern patternSupported = Pattern.compile("http://.*?uploadstube\\.de/download.php\\?file=.*", Pattern.CASE_INSENSITIVE);

    //

    public UploadStube() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getAGBLink() {

        return "http://www.uploadstube.de/regeln.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            downloadLink.setName(new Regex(requestInfo.getHtmlCode(), "<b>Dateiname: </b>(.*?) <br>").getFirstMatch());

            try {
                String[] fileSize = new Regex(requestInfo.getHtmlCode(), "<b>Dateigr..e:</b> ([0-9\\.]*) (.*?)<br>").getMatches()[0];
                double length = Double.parseDouble(fileSize[0].trim());
                int bytes;
                String type = fileSize[1].toLowerCase();
                if (type.equalsIgnoreCase("kb")) {
                    bytes = (int) (length * 1024);
                } else if (type.equalsIgnoreCase("mb")) {
                    bytes = (int) (length * 1024 * 1024);
                } else {
                    bytes = (int) length;
                }
                downloadLink.setDownloadSize(bytes);
            } catch (Exception e) {
            }
            return true;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (aborted) {
        // logger.warning("Plugin abgebrochen");
        // linkStatus.addStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        // }

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
        String dlurl = new Regex(requestInfo.getHtmlCode(), "onClick=\"window.location=..(http://www.uploadstube.de/.*?)..\">.;").getFirstMatch();
        if (dlurl == null) {
            logger.severe("Datei nicht gefunden");
            linkStatus.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(dlurl), requestInfo.getCookie(), downloadLink.getDownloadURL(), true);
        HTTPConnection urlConnection = requestInfo.getConnection();
        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Automatisch erstellter Methoden-Stub
    }
}
