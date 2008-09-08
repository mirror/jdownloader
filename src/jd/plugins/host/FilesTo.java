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
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.Browser;
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

public class FilesTo extends PluginForHost {

    static private final String AGB_LINK = "http://www.files.to/content/aup";
    static private final String CAPTCHA_WRONG = "Der eingegebene code ist falsch";

    static private final String CODER = "JD-Team";
    static private final String FILE_NOT_FOUND = "Die angeforderte Datei konnte nicht gefunden werden";

    private Pattern CAPTCHA_FLE = Pattern.compile("<img src=\"(http://www.files\\.to/captcha_[0-9]+\\.jpg)");
    private String captchaAddress;
    private Pattern DOWNLOAD_URL = Pattern.compile("action\\=\"(http://.*?files\\.to/dl/.*?)\">");

    private Pattern FILE_INFO_NAME = Pattern.compile("<p>Name: <span id=\"downloadname\">(.*?)</span></p>");
    private Pattern FILE_INFO_SIZE = Pattern.compile("<p>Gr&ouml;&szlig;e: (.*? (KB|MB|B)<)/p>");

    private String finalURL;
    private String session;
    private Pattern SESSION = Pattern.compile("action\\=\"\\?(PHPSESSID\\=.*?)\"");
    private HTTPConnection urlConnection;

    public FilesTo() {
        super();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {

            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL().toString()));

            if (requestInfo.getHtmlCode() == null) {
                return false;
            } else {

                // Datei gelöscht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) { return false; }

                String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILE_INFO_NAME).getMatch(0));
                int fileSize = getFileSize(Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILE_INFO_SIZE).getMatch(0)));
                downloadLink.setName(fileName);

                try {

                    downloadLink.setDownloadSize(fileSize);

                } catch (Exception e) {
                }

            }

            return true;

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }

        return false;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    private int getFileSize(String source) {

        int size = 0;

        if (source.contains("KB")) {
            source = new Regex(source, "(.*?) KB").getMatch(0);
            size = Integer.parseInt(source) * 1024;
        } else if (source.contains("MB")) {
            source = new Regex(source, "(.*?) MB").getMatch(0);
            size = Integer.parseInt(source) * 1024 * 1024;
        }

        return size;

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        RequestInfo requestInfo;

        String parameterString = downloadLink.getDownloadURL().toString();

        requestInfo = HTTP.getRequest(new URL(parameterString));

        // Datei gelöscht?
        if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;

        }

        if (requestInfo.getHtmlCode() != null) {
            session = new Regex(requestInfo.getHtmlCode(), SESSION).getMatch(0);
            captchaAddress = new Regex(requestInfo.getHtmlCode(), CAPTCHA_FLE).getMatch(0) + "?" + session;
        } else {
            logger.severe("Unknown error.. retry in 20 sekunden");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            sleep(20000, downloadLink);
            return;
        }

        File file = this.getLocalCaptchaFile(this);

        if (!Browser.download(file, captchaAddress) || !file.exists()) {
            logger.severe("Captcha download failed: " + captchaAddress);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;

        }
        String code = this.getCaptchaCode(file, downloadLink);

        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        requestInfo = HTTP.postRequest(new URL(parameterString + "?"), session, parameterString + "?" + session, requestHeaders, "txt_ccode=" + code + "&btn_next=", true);

        if (requestInfo.getHtmlCode() == null) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            sleep(20000, downloadLink);
            return;

        } else if (requestInfo.containsHTML(CAPTCHA_WRONG)) {
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;

        }

        finalURL = new Regex(requestInfo.getHtmlCode(), DOWNLOAD_URL).getMatch(0);

        // Download vorbereiten
        urlConnection = new HTTPConnection(new URL(finalURL).openConnection());

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
        finalURL = null;
        urlConnection = null;
    }

    @Override
    public void resetPluginGlobals() {
    }

}
