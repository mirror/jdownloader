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

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class SharedZillacom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "sharedzilla.com";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?sharedzilla\\.com/(en|ru)/get\\?id=\\d+", Pattern.CASE_INSENSITIVE);

    private String passCode = "";
    private RequestInfo requestInfo;

    public SharedZillacom() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://sharedzilla.com/en/terms";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            if (!requestInfo.containsHTML("Upload not found")) {
                String filename = new Regex(requestInfo.getHtmlCode(), Pattern.compile("nowrap title=\"(.*?)\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
                String filesize = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<span title=\"(.*?) Bytes\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (filesize != null) {
                    downloadLink.setDownloadSize(new Integer(filesize));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
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
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        /* ID holen */
        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile("get\\?id=(\\d+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        /* Password checken */
        if (requestInfo.containsHTML("Password protected")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                    passCode = "";
                }
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }
        /* Free Download starten */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL("http://sharedzilla.com/en/downloaddo"), requestInfo.getCookie(), downloadLink.getDownloadURL(), "id=" + id + "&upload_password=" + Encoding.urlEncode(passCode), false);
        if (requestInfo.getLocation() == null) {
            requestInfo = HTTP.postRequest(new URL("http://sharedzilla.com/en/downloaddo"), requestInfo.getCookie(), downloadLink.getDownloadURL(), null, "id=" + id + "&upload_password=" + Encoding.urlEncode(passCode), false);
            if (requestInfo.containsHTML("<p>Password is wrong!</p>")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(requestInfo.getLocation()), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        /* Datei herunterladen */
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);/*
                           * bei dem speed lohnen mehrere chunks nicht, da es
                           * nicht schneller wird
                           */
        dl.setResume(true);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
