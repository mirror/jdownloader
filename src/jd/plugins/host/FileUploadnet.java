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

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class FileUploadnet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "file-upload.net";

    static private final Pattern PAT_Download = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_VIEW = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_Member = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_SUPPORTED = Pattern.compile(PAT_Download.pattern() + "|" + PAT_VIEW.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE);
    private String downloadurl;

    public FileUploadnet() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.file-upload.net/to-agb.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        br.setCookiesExclusive(true);br.clearCookies(HOST);
        br.setFollowRedirects(false);
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();

                br.getPage(downloadurl);
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Download \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b></td><td>(.*?)Kbyte<td>").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                br.getPage(downloadurl);
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b>(.*?)Kbyte").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
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

        if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
            /* DownloadFiles */
            downloadurl = br.getRegex("action=\"(.*?)\" method=\"post\"").getMatch(0);
            Form form = br.getForm(0);
            br.openFormConnection(form);

        } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
            /* DownloadFiles */
            downloadurl = br.getRegex("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getMatch(0);
            br.openGetConnection(downloadurl);
        } else {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Datei herunterladen */
        HTTPConnection urlConnection = br.getHttpConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
