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
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Gulli extends PluginForHost {

    static private final String DOWNLOAD_URL = "http://share.gulli.com/download";

    static private final String HOST = "share.gulli.com";

    static private final String HOST_URL = "http://share.gulli.com/";

    static private final Pattern PAT_CAPTCHA = Pattern.compile("<img src=\"(/captcha[^\"]*)");

    static private final Pattern PAT_DOWNLOAD_ERROR = Pattern.compile("share.gulli.com/error([^\"]*)");

    static private final Pattern PAT_DOWNLOAD_LIMIT = Pattern.compile("timeLeft=([^\"]*)&");

    static private final Pattern PAT_DOWNLOAD_URL = Pattern.compile("<form action=\"/(download[^\"]*)");

    static private final Pattern PAT_FILE_ID = Pattern.compile("<input type=\"hidden\" name=\"file\" value=\"([^\"]*)");

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://share\\.gulli\\.com/files/[\\d]+.*", Pattern.CASE_INSENSITIVE);

    private String cookie;

    private String fileId;

    private HTTPConnection finalDownloadConnection;

    private String finalDownloadURL;

    public Gulli() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {

        return "http://share.gulli.com/faq";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        Browser.clearCookies(HOST);
        String[] infos = new Regex(br.getPage(downloadLink.getDownloadURL()), Pattern.compile("<h1>(.*?) \\((.*?)\\)</h1>", Pattern.CASE_INSENSITIVE)).getRow(0);

        downloadLink.setName(infos[0]);
        downloadLink.setDownloadSize(Regex.getSize(infos[1]));

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
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws IOException {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        RequestInfo requestInfo = null;
        String dlUrl = null;

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
        if (Regex.matches(requestInfo, "Fehler")) {
            String errortext = new Regex(requestInfo, "<h1>Fehler (.*?)</h1>.{1,10}<p>(.*?)<\\/p>").getMatch(1);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(errortext);
            return;
        }
        fileId = new Regex(requestInfo.getHtmlCode(), PAT_FILE_ID).getMatch(1 - 1);
        String captchaLocalUrl = new Regex(requestInfo.getHtmlCode(), PAT_CAPTCHA).getMatch(1 - 1);
        dlUrl = new Regex(requestInfo.getHtmlCode(), PAT_DOWNLOAD_URL).getMatch(1 - 1);

        File file = this.getLocalCaptchaFile(this);
        if (captchaLocalUrl == null) {
            logger.severe("Download for your Ip Temp. not available");
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(30 * 1000);
            return;
        } else {
            cookie = requestInfo.getCookie();
            logger.info(cookie);
            logger.finest("Captcha Page");
            String captchaUrl = "http://share.gulli.com" + captchaLocalUrl;

            if (!Browser.download(file, captchaUrl) || !file.exists()) {
                logger.severe("Captcha Download fehlgeschlagen: " + captchaUrl);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }

        }

        String captchaTxt = this.getCaptchaCode(file, downloadLink);

        logger.info("file=" + fileId + "&" + "captcha=" + captchaTxt);
        requestInfo = HTTP.postRequest(new URL(DOWNLOAD_URL), cookie, null, null, "file=" + fileId + "&" + "captcha=" + captchaTxt, true);

        dlUrl = new Regex(requestInfo.getHtmlCode(), PAT_DOWNLOAD_URL).getMatch(1 - 1);

        if (dlUrl == null) {
            logger.finest("Error Page");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        logger.info(dlUrl);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(HOST_URL + dlUrl), cookie, null, "action=download&file=" + fileId, false);
        String red;
        String waittime = null;
        String error = null;
        String url = HOST_URL + dlUrl;
        // Redirect folgen und dabei die Cookies weitergeben
        // share.gulli.com/error
        while ((red = requestInfo.getConnection().getHeaderField("Location")) != null && (waittime = new Regex(red, PAT_DOWNLOAD_LIMIT).getMatch(0)) == null && (error = new Regex(red, PAT_DOWNLOAD_ERROR).getMatch(0)) == null) {
            logger.info("red: " + red + " cookie: " + cookie);
            url = red;
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(red), cookie, null, false);
        }
        logger.info("abbruch bei :" + red);
        if (waittime != null) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(Integer.parseInt(waittime) * 60 * 1000);
            return;
        } else if (error != null) {
            logger.info("Error: " + error);
            if (error.indexOf("ticket") > 0) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
            } else {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            }
        } else {
            logger.info("URL: " + url);
            finalDownloadURL = url;
            finalDownloadConnection = requestInfo.getConnection();
        }

        logger.info("dl " + finalDownloadURL);

        dl = new RAFDownload(this, downloadLink, finalDownloadConnection);
        dl.startDownload();

    }

    @Override
    public void reset() {
        fileId = null;
        cookie = null;
        finalDownloadURL = null;
        finalDownloadConnection = null;
    }

    @Override
    public void resetPluginGlobals() {
    }
}
