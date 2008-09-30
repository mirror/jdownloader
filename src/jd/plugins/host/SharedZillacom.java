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

import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.GetRequest;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class SharedZillacom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private String passCode = "";
    private RequestInfo requestInfo;

    public SharedZillacom(PluginWrapper wrapper) {
        super(wrapper);
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
                passCode = Plugin.getUserInput(null, downloadLink);
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
        
        GetRequest request = new GetRequest(requestInfo.getLocation());
        request.getCookies().addAll(Request.parseCookies(requestInfo.getCookie(), "sharedzilla.com"));
        dl = new RAFDownload(this, downloadLink, request);
        dl.setChunkNum(1);/*
                           * bei dem speed lohnen mehrere chunks nicht, da es
                           * nicht schneller wird
                           */
        dl.setResume(true);
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
