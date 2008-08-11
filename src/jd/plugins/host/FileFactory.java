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
import jd.http.Encoding;
import jd.http.GetRequest;
import jd.http.HTTPConnection;
import jd.http.PostRequest;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

// http://filefactory.com/file/b1bf90/
// http://filefactory.com/f/ef45b5179409a229/ 

public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static final String CAPTCHA_WRONG = "verification code you entered was incorrect";
    private static final String DOWNLOAD_LIMIT = "exceeded the download limit";

    private static final String FILENAME = "<h1 style=\"width:370px;\">(.*)</h1>";
    private static final String FILESIZE = "Size: (.*?)(B|KB|MB)<br />";
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    static private final String host = "filefactory.com";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "downloading too many files";
    // src=
    // "/securimage/securimage_show.php?f=044a7b&amp;h=c5b0bfa214ecf57d7f5250582c8004a3"
    // alt="Verification code
    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/securimage/securimage_show.php\\?[^\"]*)\" alt=");
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?filefactory\\.com(/|//)file/.{6}/?", Pattern.CASE_INSENSITIVE);
    // <p style="margin:30px 0 20px"><a
    // href="http://dl054.filefactory.com/dlp/6a1dad/"><img
    // src="/images/begin_download.gif"
    private static final String PREMIUM_LINK = "<p style=\"margin:30px 0 20px\"><a href=\"(http://[a-z0-9]+\\.filefactory\\.com/dlp/[a-z0-9]+/.*?)\"";
    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";
    private static final String WAIT_TIME = "wait ([0-9]+) (minutes|seconds)";
    private String actionString;

    private String captchaAddress;
    private File captchaFile;
    private String postTarget;
    private RequestInfo requestInfo;

    private int wait;

    public FileFactory() {

        super();
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
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
        parameter.setUrlDownload(parameter.getDownloadURL().replaceAll(".com//", ".com/"));
        parameter.setUrlDownload(parameter.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        DownloadLink downloadLink = null;

        downloadLink = parameter;
        logger.info(downloadLink.getDownloadURL());
        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);

        if (requestInfo.containsHTML(NOT_AVAILABLE)) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;

        } else if (requestInfo.containsHTML(SERVER_DOWN)) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;

        } else if (requestInfo.containsHTML(NO_SLOT)) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;

        }

        String newURL = "http://" + requestInfo.getConnection().getURL().getHost() + new Regex(requestInfo.getHtmlCode(), baseLink).getFirstMatch();
        logger.info(newURL);

        if (newURL != null) {

            newURL = newURL.replaceAll("' \\+ '", "");
            requestInfo = HTTP.getRequest((new URL(newURL)), null, downloadLink.getName(), true);
            actionString = "http://www.filefactory.com/" + new Regex(requestInfo.getHtmlCode(), frameForCaptcha).getFirstMatch();
            actionString = actionString.replaceAll("&amp;", "&");
            requestInfo = HTTP.getRequest((new URL(actionString)), "viewad11=yes", newURL, true);
            // captcha Adresse finden
            captchaAddress = "http://www.filefactory.com" + new Regex(requestInfo.getHtmlCode(), patternForCaptcha).getFirstMatch();
            captchaAddress = captchaAddress.replaceAll("&amp;", "&");
            // post daten lesen
            postTarget = HTMLParser.getFormInputHidden(requestInfo.getHtmlCode());

        }

        logger.info(captchaAddress + " : " + postTarget);
        // step.setStatus(PluginStep.STATUS_DONE);

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        captchaFile = this.getLocalCaptchaFile(this);

        if (!Browser.download(captchaFile, captchaAddress) || !captchaFile.exists()) {

            logger.severe("Captcha Download failed: " + captchaAddress);

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
            // ImageIO
            // Error");
            return;

        }
        // step.setParameter(captchaFile);
        // wird in diesem step null zurückgegeben findet keine
        // captchaerkennung statt. der captcha wird im nächsten schritt
        // erkannt

        // case PluginStep.STEP_DOWNLOAD:

        String captchaCode = this.getCaptchaCode(captchaFile, downloadLink);
        
        captchaFile.renameTo(new File(captchaFile.getParentFile(),captchaFile.getName()+"_"+captchaCode+"_."+JDUtilities.getFileExtension(captchaFile)));
        try {
            logger.info(postTarget + "&captcha=" + captchaCode);
            requestInfo = HTTP.postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + captchaCode, true);

            if (requestInfo.getHtmlCode().contains(CAPTCHA_WRONG)) {

                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);

                return;

            }

            postTarget = new Regex(requestInfo.getHtmlCode(), patternForDownloadlink).getFirstMatch();
            postTarget = postTarget.replaceAll("&amp;", "&");

        } catch (Exception e) {

            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return;

        }

        requestInfo = HTTP.postRequestWithoutHtmlCode((new URL(postTarget)), requestInfo.getCookie(), actionString, "", false);
        HTTPConnection urlConnection = requestInfo.getConnection();

        // downloadlimit reached
        if (urlConnection.getHeaderField("Location") != null) {

            // filefactory.com/info/premium.php/w/
            requestInfo = HTTP.getRequest(new URL(urlConnection.getHeaderField("Location")), null, null, true);

            if (requestInfo.getHtmlCode().contains(DOWNLOAD_LIMIT)) {

                logger.severe("Download limit reached as free user");

                String waitTime = new Regex(requestInfo.getHtmlCode(), WAIT_TIME).getFirstMatch(1);
                String unit = new Regex(requestInfo.getHtmlCode(), WAIT_TIME).getFirstMatch(2);
                wait = 0;

                if (unit.equals("minutes")) {
                    wait = Integer.parseInt(waitTime);
                    logger.info("wait" + " " + String.valueOf(wait + 1) + " minutes");
                    wait = wait * 60000 + 60000;
                } else if (unit.equals("seconds")) {
                    wait = Integer.parseInt(waitTime);
                    logger.info("wait" + " " + String.valueOf(wait + 5) + " seconds");
                    wait = wait * 1000 + 5000;
                }

                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.info("Traffic Limit reached....");
                // step.setParameter((long) wait);
                return;

            } else {

                requestInfo = HTTP.postRequestWithoutHtmlCode((new URL(postTarget)), requestInfo.getCookie(), actionString, "", false);
                urlConnection = requestInfo.getConnection();

            }

        }

        if (requestInfo.getConnection().getHeaderField("Location") != null) {
            requestInfo = HTTP.getRequest(new URL(requestInfo.getConnection().getHeaderField("Location")));

            if (requestInfo.containsHTML(PATTERN_DOWNLOADING_TOO_MANY_FILES)) {

                logger.info("You are downloading too many files at the same time. Wait 10 seconds(or reconnect) an retry afterwards");

                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // step.setStatus(PluginStep.STATUS_ERROR);

                linkStatus.setValue( 60000l);

                return;
            }
            logger.info(requestInfo.getHtmlCode());
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());

        dl.startDownload();

    }

    // by eXecuTe
    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        if (user == null || pass == null) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Please enter premium data");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            // step.setParameter(JDLocale.L("plugins.host.premium.loginError",
            // "Loginfehler"));
           // getProperties().setProperty(PROPERTY_USE_PREMIUM, false);
            return;

        }

        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:
        PostRequest req = new PostRequest(downloadLink.getDownloadURL());
        req.setPostVariable("email", Encoding.urlEncode(user));
        req.setPostVariable("password", Encoding.urlEncode(pass));
        req.setPostVariable("login", "Log+In");
        req.load();

        GetRequest greq = new GetRequest(downloadLink.getDownloadURL());
        greq.getCookies().putAll(req.getCookies());
        greq.load();

        if (greq.containsHTML(NOT_AVAILABLE)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        } else if (greq.containsHTML(SERVER_DOWN)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        } else {
            String link = "http://www.filefactory.com" + greq.getLocation();
            /* falls direct-download eingeschalten ist */
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
            if (requestInfo.getHeaders().get("Content-Type").toString().contains("text/html")) {
                /* falls direct-download ausgeschalten ist */
                requestInfo = HTTP.getRequest(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
                link = new Regex(requestInfo.getHtmlCode(), Pattern.compile(PREMIUM_LINK, Pattern.CASE_INSENSITIVE)).getFirstMatch();
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
            }
        }

        // step.setStatus(PluginStep.STATUS_DONE);

        // case PluginStep.STEP_GET_CAPTCHA_FILE:

        // //step.setStatus(PluginStep.STATUS_SKIP);
        // step.setStatus(PluginStep.STATUS_SKIP);

        // return;

        // case PluginStep.STEP_DOWNLOAD:

        HTTPConnection urlConnection = requestInfo.getConnection();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        try {

            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);

            if (requestInfo.containsHTML(NOT_AVAILABLE)) {
                return false;
            } else if (requestInfo.containsHTML(SERVER_DOWN)) {
                return false;
            } else {

                String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode().replaceAll("\\&\\#8203\\;", ""), FILENAME).getFirstMatch());
                int length = 0;
                Double fileSize = Double.parseDouble(new Regex(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch(1).replaceAll(",", ""));
                //                
                // // Dateiname ist auf der Seite nur gekürzt auslesbar ->
                // linkchecker
                // // http://www.filefactory.com/file/d0b032/
                // /http://www.filefactory.com/file/0f4d0c/
                // requestInfo = HTTP.postRequest(new
                // URL("http://www.filefactory.com/tools/link_checker.php"),
                // null,
                // null, null,
                // "link_text="+fileFactoryUrlEncode(downloadLink.getDownloadURL
                // ()), true);
                // String f2 = new Regex(requestInfo.getHtmlCode(),
                // FILENAME).getFirstMatch();
                // if(f2!=null)fileName=f2;
                // if(fileName==null)return false;
                // fileName = fileName.replaceAll(" <br>", "").trim();
                //				
                // Double fileSize = Double.parseDouble(new
                // Regex(requestInfo.getHtmlCode(),
                // FILESIZE).getFirstMatch(1).replaceAll(",", ""));
                String unit = new Regex(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch(2);

                if (unit.equals("B")) {
                    length = (int) Math.round(fileSize);
                }
                if (unit.equals("KB")) {
                    length = (int) Math.round(fileSize * 1024);
                }
                if (unit.equals("MB")) {
                    length = (int) Math.round(fileSize * 1024 * 1024);
                }

                downloadLink.setName(fileName);
                downloadLink.setDownloadSize(length);

            }

        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 1;
        }
    }

    @Override
   */ public String getPluginName() {
        return host;
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

    // codierung ist nicht standardkonform
    // http%3A%2F%2Fwww.filefactory.com%2Ffile%2Fd0b032%2F
    /*
     * private static String fileFactoryUrlEncode(String str) {
     * 
     * String allowed =
     * "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.\\&=;";
     * String ret = ""; int i;
     * 
     * for (i = 0; i < str.length(); i++) {
     * 
     * char letter = str.charAt(i);
     * 
     * if (allowed.indexOf(letter) >= 0) { ret += letter; } else { ret += "%" +
     * Integer.toString(letter, 16).toUpperCase(); } }
     * 
     * return ret; }
     */

    @Override
    public void init() {
        // currentStep = null;
    }

    @Override
    public void reset() {

        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;
        wait = 0;

        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public void resetPluginGlobals() {

        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;
        wait = 0;

    }

    private void setConfigElements() {

       

    }

}
