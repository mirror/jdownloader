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
import jd.plugins.download.RAFDownload;import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://www.xup.in/dl,43227676/YourFilesBiz.java/

public class XupIn extends PluginForHost {

    private static final String CODER = "jD-Team";
    private static final String HOST = "xup.in";
    private static final String PLUGIN_VERSION = "0.2.0";
    private static final String AGB_LINK = "http://www.xup.in/terms/";

    static private final Pattern PATTERN_SUPPORTED = Pattern.compile("http://[\\w\\.]*?xup\\.in/dl,\\d+/?.+?", Pattern.CASE_INSENSITIVE);
    private static final int MAX_SIMULTAN_DOWNLOADS = Integer.MAX_VALUE;

    private String vid = "";
    private String vtime = "";
    private String vpass = "";
    private String captchaAddress = "";
    private String cookie = "";

    private static final String DOWNLOAD_SIZE = "<li class=\"iclist\">File Size: (.*?) Mbyte</li>";
    private static final String DOWNLOAD_NAME = "<legend> <b>Download: (.*?)</b> </legend>";
    private static final String NAME_FROM_URL = "http://.*?xup\\.in/dl,[0-9]+/(.*?)";
    private static final String VID = "value=\"(.*?)\" name=\"vid\"";
    private static final String VTIME = "value=\"([0-9]+)\" name=\"vtime\"";
    private static final String NOT_FOUND = "File does not exist";
    private static final String PASSWORD_PROTECTED = "Bitte Passwort eingeben";

    public XupIn() {

        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        /*
         * //steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
         * haben aktuell keine captchas
         */
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + PLUGIN_VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTED;
    }

    @Override
    public void reset() {

        vid = "";
        vtime = "";
        vpass = "";
        captchaAddress = "";
        cookie = "";

    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) { LinkStatus linkStatus=downloadLink.getLinkStatus();

        try {

            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));

            if (requestInfo.containsHTML(NOT_FOUND)) {

                if (new Regex(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch() != null) downloadLink.setName(new Regex(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch());
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return false;

            }

            String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim()) * 1024 * 1024);

            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
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

     public void handle(DownloadLink downloadLink) throws Exception{ LinkStatus linkStatus=downloadLink.getLinkStatus();

        // if (aborted) {
        //    		
        // logger.warning("Plugin aborted");
        // linkStatus.addStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        //            
        // }

        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

          //  switch (step.getStep()) {

            //case PluginStep.STEP_PAGE:

                requestInfo = HTTP.getRequest(downloadUrl);

                if (requestInfo.containsHTML(NOT_FOUND)) {

                    if (new Regex(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch() != null) downloadLink.setName(new Regex(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch());
                    linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                if (requestInfo.containsHTML(PASSWORD_PROTECTED)) {

                    vpass = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));

                }

                String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
                downloadLink.setName(fileName);

                try {

                    int length = (int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim()) * 1024 * 1024);
                    downloadLink.setDownloadMax(length);

                } catch (Exception e) {

                    linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                cookie = requestInfo.getCookie();

                if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {

                    logger.severe("File already is in progress: " + downloadLink.getFileOutput());
                    linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                if (new File(downloadLink.getFileOutput()).exists()) {

                    logger.severe("File already exists: " + downloadLink.getFileOutput());
                    linkStatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                vid = new Regex(requestInfo.getHtmlCode(), VID).getFirstMatch();
                vtime = new Regex(requestInfo.getHtmlCode(), VTIME).getFirstMatch();
                captchaAddress = new Regex(requestInfo.getHtmlCode(), VTIME).getFirstMatch();
                return;

            //case PluginStep.STEP_GET_CAPTCHA_FILE:

                File file = this.getLocalCaptchaFile(this);

                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL("http://www.xup.in/captcha.php"), cookie, downloadLink.getDownloadURL(), true);

                if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {

                    logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                    //this.sleep(nul,downloadLink);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);//step.setParameter("Captcha ImageIO Error");
                    return;

                } else {

                    //step.setParameter(file);
                    //step.setStatus(PluginStep.STATUS_USER_INPUT);

                }

                break;

            //case PluginStep.STEP_DOWNLOAD:

                String vchep = this.getCaptchaCode(file);

                if (vpass != "") {
                    requestInfo = HTTP.postRequestWithoutHtmlCode(downloadUrl, cookie, downloadLink.getDownloadURL(), "vid=" + vid + "&vtime=" + vtime + "&vpass=" + vpass + "&vchep=" + vchep, true);
                } else {
                    requestInfo = HTTP.postRequestWithoutHtmlCode(downloadUrl, cookie, downloadLink.getDownloadURL(), "vid=" + vid + "&vtime=" + vtime + "&vchep=" + vchep, true);
                }

                HTTPConnection urlConnection = requestInfo.getConnection();
                int length = urlConnection.getContentLength();

                if (urlConnection.getContentType().contains("text/html")) {

                    logger.severe("Captcha code or password wrong");
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                if (Math.abs(length - downloadLink.getDownloadMax()) > 1024 * 1024) {

                    logger.severe("Filesize Error");
                    linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;

                }

                // Download starten
                dl = new RAFDownload(this, downloadLink, urlConnection);

               dl.startDownload(); \r\n if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                    linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                    //step.setStatus(PluginStep.STATUS_ERROR);

                }

                return;

            }

            return;

        } catch (IOException e) {

            e.printStackTrace();
            return;

        }

    }

}