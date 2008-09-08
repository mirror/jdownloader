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
import jd.utils.JDLocale;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    // Suchmasken
    private static final String DOWNLOAD_INFO = "<th.*?><b>Datei</b></th>[\\s]*?<th.*?><b>Gr&ouml;sse</b></th>[\\s]*?</tr>[\\s]*?<tr>[\\s]*?<td.*?><font.*?>(.*?)</font></td>[\\s]*?<td.*?><font.*?>(.*?) MB</font></td>";

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
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";
        try {

            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadurl));

            if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4, downloadurl.length() - 6));
                return false;

            }

            if (requestInfo.getHtmlCode().contains(HARDWARE_DEFECT)) {

                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(20 * 60 * 1000l);
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4, downloadurl.length() - 6));
                return false;

            }

            String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getMatch(0)).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getMatch(1).trim()) * 1024 * 1024);

            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadSize(length);
                } catch (Exception e) {
                }

                return true;

            } else {
                downloadLink.setName(downloadurl.substring(downloadurl.indexOf("pid=") + 4));
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // unbekannter fehler
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
        String downloadurl = downloadLink.getDownloadURL() + "&lg=de";

        RequestInfo requestInfo = HTTP.getRequest(new URL(downloadurl));
        String cookie = requestInfo.getCookie();

        if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;

        }

        if (requestInfo.getHtmlCode().contains(HARDWARE_DEFECT)) {

            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;

        }

        int maxCaptchaTries = 10;
        while (true) {
            File file = this.getLocalCaptchaFile(this);

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL("http://fast-load.net/includes/captcha.php"), cookie, downloadurl, true);

            if (!Browser.download(file, requestInfo.getConnection()) || !file.exists()) {

                logger.severe("Captcha download failed: http://fast-load.net/includes/captcha.php");

                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.captchadownloaderror", "Captcha could not be downloaded"));
                return;

            }

            String code = getCaptchaCode(file, downloadLink);
            if (code == null) {
                if (maxCaptchaTries-- > 0) {
                    logger.warning("Captcha wrong. Retries left: " + maxCaptchaTries);
                    continue;
                } else {
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    linkStatus.setErrorMessage(requestInfo.getHtmlCode());
                    return;
                }

            }
            String pid = downloadurl.substring(downloadurl.indexOf("pid=") + 4, downloadurl.indexOf("&"));

            requestInfo = HTTP.postRequestWithoutHtmlCode(new URL("http://fast-load.net/download.php"), cookie, downloadurl, "fid=" + pid + "&captcha_code=" + code, true);

            // Download vorbereiten
            HTTPConnection urlConnection = requestInfo.getConnection();
            long length = urlConnection.getContentLength();

            if (urlConnection.getContentType() != null) {

                if (urlConnection.getContentType().contains("text/html")) {

                    if (length == 13) {

                        if (maxCaptchaTries-- > 0) {
                            logger.warning("Captcha wrong. Retries left: " + maxCaptchaTries);
                            continue;
                        } else {
                            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                            linkStatus.setErrorMessage(requestInfo.getHtmlCode());
                            return;
                        }

                    } else if (length == 184) {

                        logger.info("System overload: Retry in 20 seconds");
                        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                        linkStatus.setErrorMessage(requestInfo.getHtmlCode());
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