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

import jd.config.Configuration;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DepositFiles extends PluginForHost {

    static private final String CODER = "JD-Team";

    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1.3";

    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final String HOST = "depositfiles.com";

    // private Pattern HIDDENPARAM = Pattern.compile("<input type=\"hidden\"
    // name=\"gateway_result\" value=\"([\\d]+)\">", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?depositfiles\\.com(/en/|/de/|/ru/|/)files/[0-9]+", Pattern.CASE_INSENSITIVE);

    private static final String PATTERN_PREMIUM_FINALURL = "var dwnsrc = \"(.*?)\";";

    // private Pattern ICID = Pattern.compile("name=\"icid\" value=\"(.*?)\"");

    private static final String PATTERN_PREMIUM_REDIRECT = "window.location.href = '(.*?)';";

    private String cookie;

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr&ouml;&szlig;e: <b>(.*?)</b>");

    // private String captchaAddress;

    // Rechtschreibfehler übernommen
    private String PASSWORD_PROTECTED = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    // private String icid;

    public DepositFiles() {

        super();
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // //steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
        this.enablePremium();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();

        DownloadLink downloadLink = parameter;
        Browser br = new Browser();
        Browser.clearCookies(HOST);

        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:

        String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
        link = link.replace("com/ru/files/", "com/de/files/");
        link = link.replace("com/files/", "com/de/files/");
        downloadLink.setUrlDownload(link);

        String finalURL = link;

        br.getPage(finalURL);
        // br.getRequest();
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {

            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (new File(downloadLink.getFileOutput()).exists()) {

            logger.severe("File already exists. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) {

            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setParameter(60000l);
            return;

        }
        

        Form[] forms = br.getForms();
        
        if(forms.length<2){
          String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/span>").getFirstMatch();
            if(wait!=null){
                linkStatus.setValue(Regex.getMilliSeconds(wait)); 
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                
                return;
            }
            
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
          
            return;
        }
        br.submitForm(forms[1]);
        // requestInfo = HTTP.postRequest(new URL(finalURL),
        // requestInfo.getCookie(), finalURL, null,
        // "x=15&y=7&gateway_result=" +
        // SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(),
        // HIDDENPARAM, 1), true);
        // br.getRequest();
        if (br.containsHTML(PASSWORD_PROTECTED)) {

            String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
            br.postPage(finalURL, "go=1&gateway_result=1&file_password=" + password);

        }

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {

            logger.severe("Unknown error. Retry in 20 seconds");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }
        // else {
        //
        // icid = SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(),
        // ICID, 1);
        //
        // if (icid == null) {
        //
        // logger.severe("Session error");
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // return;
        //
        // }
        //
        // captchaAddress =
        // "http://depositfiles.com/get_download_img_code.php?icid=" +
        // icid;
        // cookie = requestInfo.getCookie();
        // return;
        //
        // }

        // //case PluginStep.STEP_GET_CAPTCHA_FILE:
        //
        // File file = this.getLocalCaptchaFile(this);
        //
        // if (!JDUtilities.download(file, captchaAddress) ||
        // !file.exists()) {
        //
        // logger.severe("Captcha donwload failed: " + captchaAddress);
        // //this.sleep(nul,downloadLink);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(DownloadLink.
        // STATUS_ERROR_CAPTCHA_IMAGEERROR);
        // return;
        //
        // }
        // else {
        //
        // //step.setParameter(file);
        // //step.setStatus(PluginStep.STATUS_USER_INPUT);
        // return;
        // }
        finalURL = new Regex(br, "var dwnsrc = \"(.*?)\";").getFirstMatch();
        if (finalURL == null) {
            if (br.containsHTML("IP-Addresse")) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDLocale.L("plugins.host.depositfiles.iplocked", "Your IP is already downloading a file"));
                linkStatus.setValue(10000l);
                return;
            }
            if (br.containsHTML("download_limit")) {
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                linkStatus.setValue(300000l);
                return;

            }
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        // case PluginStep.STEP_PENDING:
        // step.setStatus(PluginStep.STATUS_SKIP);

        // this.sleep(60000,downloadLink);
        // return;

        // case PluginStep.STEP_DOWNLOAD:

        // String code = this.getCaptchaCode(captchaFile);
        //
        // if (code == null || code.length() != 4) {
        //
        // logger.severe("Captcha donwload failed: " + captchaAddress);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(DownloadLink.
        // STATUS_ERROR_CAPTCHA_IMAGEERROR);
        // return;
        //
        // }
        //
        // if (code.length() != 4) {
        //
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG
        // );
        // return;
        //
        // }

        // requestInfo = HTTP.postRequestWithoutHtmlCode(new
        // URL(finalURL + "#"), cookie, finalURL, "img_code=" + code +
        // "&icid=" + icid + "&file_password&gateway_result=1&go=1",
        // true);

        HTTPConnection con = br.openGetConnection(finalURL);
        if (con.getHeaderField("Location") != null && con.getHeaderField("Location").indexOf("error") > 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        dl = new RAFDownload(this, downloadLink, con);

        dl.startDownload();

    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        Browser.clearCookies(HOST);
        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:

        String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
        link = link.replace("com/ru/files/", "com/de/files/");
        link = link.replace("com/files/", "com/de/files/");
        downloadLink.setUrlDownload(link);

        String finalURL = link;

        br.getPage(finalURL);
        cookie = br.getRequest().getCookieString();
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (new File(downloadLink.getFileOutput()).exists()) {
            logger.severe("File already exists. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // Datei geloescht?
        if (requestInfo.containsHTML(FILE_NOT_FOUND)) {
            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setParameter(60000l);
            return;

        }

        Form[] forms = br.getForms();
        Form login = forms[0];
        login.getVars().put("login", user);
        login.getVars().put("password", pass);
        login.getVars().put("x", "30");
        login.getVars().put("y", "11");
        br.submitForm(login);

        cookie += "; " + br.getRequest().getCookieString();

        finalURL = br.getRegex(PATTERN_PREMIUM_REDIRECT).getFirstMatch();
        br.getPage(finalURL);
        if (br.containsHTML(PASSWORD_PROTECTED)) {

            String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
            br.postPage(finalURL, "go=1&gateway_result=1&file_password=" + password);
        } else {
            logger.info(br + "");
        }
        finalURL = br.getRegex(PATTERN_PREMIUM_FINALURL).getFirstMatch();

        if (finalURL == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;

        }

        // //case PluginStep.STEP_GET_CAPTCHA_FILE:
        // //step.setStatus(PluginStep.STATUS_SKIP);
        // downloadLink.getLinkStatus().setStatusText("Premiumdownload");
        // step = nextStep(step);

        // case PluginStep.STEP_PENDING:

        // step.setStatus(PluginStep.STATUS_SKIP);

        // case PluginStep.STEP_DOWNLOAD:

        HTTPConnection con = br.openGetConnection(finalURL);

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        logger.info("Filename: " + Plugin.getFileNameFormHeader(con));

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        RequestInfo requestInfo;
        String link = downloadLink.getDownloadURL().replace("/en/files/", "/de/files/");
        link = link.replace("/ru/files/", "/de/files/");

        try {

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), null, null, false);

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                return false;
            } else {

                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = HTTP.getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                } else {
                    requestInfo = HTTP.readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) { return false; }

                String fileName = new Regex(requestInfo.getHtmlCode(), FILE_INFO_NAME).getFirstMatch();
                downloadLink.setName(fileName);
                String fileSizeString = new Regex(requestInfo.getHtmlCode(), FILE_INFO_SIZE).getFirstMatch();
                int length = (int) Regex.getSize(fileSizeString);

                downloadLink.setDownloadSize(length);

            }

            return true;

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }

        return false;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void reset() {
       
    }

    @Override
    public void resetPluginGlobals() {
      
    }

    private void setConfigElements() {

    }

}
