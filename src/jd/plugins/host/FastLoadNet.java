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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {

    private static final String CODER = "eXecuTe";

    // Suchmasken
    private static final String DOWNLOAD_INFO = "<th.*?><b>Datei</b></th>[\\s]*?<th.*?><b>Gr&ouml;sse</b></th>[\\s]*?</tr>[\\s]*?<tr>[\\s]*?<td.*?><font.*?>(.*?)</font></td>[\\s]*?<td.*?><font.*?>(.*?) MB</font></td>";

    private static final String HARDWARE_DEFECT = "Hardware-Defekt!";

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.2.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String HOST = "fast-load.net";

    private static final int MAX_SIMULTAN_DOWNLOADS = 8;

    private static final String NOT_FOUND = "Datei existiert nicht";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?fast-load\\.net(/|//)index\\.php\\?pid=[a-zA-Z0-9]+");

    private static final String PLUGIN_NAME = HOST;

    private String cookie = "";

    public FastLoadNet() {

        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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

        downloadLink.setUrlDownload(downloadLink.getDownloadURL() + "&lg=de");

        try {

            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));

            if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4, downloadLink.getDownloadURL().length() - 6));
                return false;

            }

            if (requestInfo.getHtmlCode().contains(HARDWARE_DEFECT)) {

                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4, downloadLink.getDownloadURL().length() - 6));
                return false;

            }

            String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(1)).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(2).trim()) * 1024 * 1024);

            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
                } catch (Exception e) {
                }

                return true;

            } else {
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4));
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
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
    public String getPluginName() {
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

        // switch (step.getStep()) {

        // case PluginStep.STEP_PAGE:

        downloadLink.setUrlDownload(downloadLink.getDownloadURL() + "&lg=de");

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
        cookie = requestInfo.getCookie();

        if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (requestInfo.getHtmlCode().contains(HARDWARE_DEFECT)) {

            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(1)).trim();
        downloadLink.setName(fileName);

        try {

            int length = (int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(2).trim()) * 1024 * 1024);
            downloadLink.setDownloadMax(length);

        } catch (Exception e) {

            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.exceptionToErrorMessage(e);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        int maxCaptchaTries = 10;
        while (true) {
            File file = this.getLocalCaptchaFile(this);

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL("http://fast-load.net/includes/captcha.php"), cookie, downloadLink.getDownloadURL(), true);

            if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {

                logger.severe("Captcha download failed: http://fast-load.net/includes/captcha.php");
                // step.setParameter(null);
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
                // ImageIO
                // Error");
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.captchadownloaderror", "Captcha could not be downloaded"));
                return;

            }

            // case PluginStep.STEP_DOWNLOAD:
            
            String code = getCaptchaCode(file, downloadLink);

            String pid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4, downloadLink.getDownloadURL().indexOf("&"));

            requestInfo = HTTP.postRequestWithoutHtmlCode(new URL("http://fast-load.net/download.php"), cookie, downloadLink.getDownloadURL(), "fid=" + pid + "&captcha_code=" + code, true);

            // Download vorbereiten
            HTTPConnection urlConnection = requestInfo.getConnection();
            long length = urlConnection.getContentLength();

            if (urlConnection.getContentType() != null) {

                if (urlConnection.getContentType().contains("text/html")) {

                    if (length == 13) {

                        if ((maxCaptchaTries--) > 0) {
                            logger.warning("Captcha wrong. Retries left: "+maxCaptchaTries);
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
                        // step.setStatus(PluginStep.STATUS_ERROR);
                        linkStatus.setValue(20000l);
                        return;

                    } else {

                        logger.severe("Unknown error page - [Length: " + length + "]");
                        linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                        // step.setStatus(PluginStep.STATUS_ERROR);
                        return;

                    }

                }

                downloadLink.setDownloadMax(length);
                downloadLink.setName(getFileNameFormHeader(urlConnection));

                // Download starten
                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setResume(false);
                dl.setChunkNum(1);
                dl.startDownload();
                return;

            } else {

                logger.severe("Couldn't get HTTP connection");
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;

            }
        }

    }

    @Override
    public void reset() {
        cookie = "";
    }

    @Override
    public void resetPluginGlobals() {
    }

}