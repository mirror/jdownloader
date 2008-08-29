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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.GetRequest;
import jd.http.HTTPConnection;
import jd.http.PostRequest;
import jd.parser.Form;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static final String CAPTCHA_WRONG = "verification code you entered was incorrect";
    private static final String DOWNLOAD_LIMIT = "exceeded the download limit";

    private static final String FILENAME = "<h1 style=\"width:370px;\">(.*)</h1>";
    private static final String FILESIZE = "Size: (.*?)(B|KB|MB)<br />";
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    static private final String HOST = "filefactory.com";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "downloading too many files";

    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/securimage/securimage_show.php\\?[^\"]*)\" alt=");
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    static private final Pattern patternSupported = Pattern.compile("sjdp://filefactory\\.com.*|http://[\\w\\.]*?filefactory\\.com(/|//)file/.{6}/?", Pattern.CASE_INSENSITIVE);

    private static final String PREMIUM_LINK = "<p style=\"margin:30px 0 20px\"><a href=\"(http://[a-z0-9]+\\.filefactory\\.com/(cache/)?dlp/[a-z0-9]+/.*?)\"";
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
        this.enablePremium();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
       	if(parameter.getDownloadURL().matches("sjdp://.*"))
   		{
   		new Serienjunkies().handleFree(parameter);
   		return;
   		}
        LinkStatus linkStatus = parameter.getLinkStatus();
        parameter.setUrlDownload(parameter.getDownloadURL().replaceAll(".com//", ".com/"));
        parameter.setUrlDownload(parameter.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        DownloadLink downloadLink = null;

        downloadLink = parameter;
        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);

        if (requestInfo.containsHTML(NOT_AVAILABLE)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;

        } else if (requestInfo.containsHTML(SERVER_DOWN)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;

        } else if (requestInfo.containsHTML(NO_SLOT)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        String newURL = "http://" + requestInfo.getConnection().getURL().getHost() + new Regex(requestInfo.getHtmlCode(), baseLink).getMatch(0);        

        if (newURL != null) {

            newURL = newURL.replaceAll("' \\+ '", "");
            requestInfo = HTTP.getRequest((new URL(newURL)), null, downloadLink.getName(), true);
            actionString = "http://www.filefactory.com/" + new Regex(requestInfo.getHtmlCode(), frameForCaptcha).getMatch(0);
            actionString = actionString.replaceAll("&amp;", "&");
            requestInfo = HTTP.getRequest((new URL(actionString)), "viewad11=yes", newURL, true);
            // captcha Adresse finden
            captchaAddress = "http://www.filefactory.com" + new Regex(requestInfo.getHtmlCode(), patternForCaptcha).getMatch(0);
            captchaAddress = captchaAddress.replaceAll("&amp;", "&");
            // post daten lesen
            postTarget = HTMLParser.getFormInputHidden(requestInfo.getHtmlCode());

        }

        captchaFile = this.getLocalCaptchaFile(this);

        if (!Browser.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
            logger.severe("Captcha Download failed: " + captchaAddress);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;

        }
        String captchaCode = this.getCaptchaCode(captchaFile, downloadLink);

        captchaFile.renameTo(new File(captchaFile.getParentFile(), captchaFile.getName() + "_" + captchaCode + "_." + JDUtilities.getFileExtension(captchaFile)));
        try {
            requestInfo = HTTP.postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + captchaCode, true);

            if (requestInfo.getHtmlCode().contains(CAPTCHA_WRONG)) {
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }

            postTarget = new Regex(requestInfo.getHtmlCode(), patternForDownloadlink).getMatch(0);
            postTarget = postTarget.replaceAll("&amp;", "&");

        } catch (Exception e) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            e.printStackTrace();
            return;

        }

        requestInfo = HTTP.postRequestWithoutHtmlCode((new URL(postTarget)), requestInfo.getCookie(), actionString, "", false);
        HTTPConnection urlConnection = requestInfo.getConnection();

        // downloadlimit reached
        if (urlConnection.getHeaderField("Location") != null) {
            requestInfo = HTTP.getRequest(new URL(urlConnection.getHeaderField("Location")), null, null, true);

            if (requestInfo.getHtmlCode().contains(DOWNLOAD_LIMIT)) {

                logger.severe("Download limit reached as free user");
                String waitTime = new Regex(requestInfo.getHtmlCode(), WAIT_TIME).getMatch(0);
                String unit = new Regex(requestInfo.getHtmlCode(), WAIT_TIME).getMatch(1);
                wait = 0;

                if (unit.equals("minutes")) {
                    wait = Integer.parseInt(waitTime);
                    logger.severe("wait" + " " + String.valueOf(wait + 1) + " minutes");
                    wait = wait * 60000 + 60000;
                } else if (unit.equals("seconds")) {
                    wait = Integer.parseInt(waitTime);
                    logger.severe("wait" + " " + String.valueOf(wait + 5) + " seconds");
                    wait = wait * 1000 + 5000;
                }

                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                linkStatus.setValue(wait);
                logger.severe("Traffic Limit reached....");

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
                linkStatus.setValue(60000l);
                return;
            }
            logger.severe(requestInfo.getHtmlCode());
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.startDownload();

    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();

        br.setCookiesExclusive(true);br.clearCookies(HOST);
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);

        if (br.containsHTML("record of an account with that email")) {
            ai.setValid(false);
            ai.setStatus("No account with this email");
            return ai;
        }
        if (br.containsHTML("password you entered is incorrect")) {
            ai.setValid(false);
            ai.setStatus("Account found, but password is wrong");
            return ai;
        }
        br.getPage("http://filefactory.com/rewards/summary/");
        String expire = br.getMatch("subscription will expire on <strong>(.*?)</strong>");
        if (expire == null) {
            ai.setValid(false);
            return ai;
        }
        // 17 October, 2008 (in 66 days).
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));
        String pThisMonth = br.getMatch("\\(Usable next month\\)</td>.*?<td.*?>(.*?)</td>").replaceAll("\\,", "");
        String pUsable = br.getMatch("Usable Accumulated Points</h2></td>.*?<td.*?><h2>(.*?)</h2></td>").replaceAll("\\,", "");

        ai.setPremiumPoints(Integer.parseInt(pThisMonth) + Integer.parseInt(pUsable));

        return ai;
    }

    // by eXecuTe
    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
    	
       	if(downloadLink.getDownloadURL().matches("sjdp://.*"))
   		{
   		new Serienjunkies().handleFree(downloadLink);
   		return;
   		}
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        if (user == null || pass == null) {
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            return;
        }

        PostRequest req = new PostRequest(downloadLink.getDownloadURL());
        req.setPostVariable("email", Encoding.urlEncode(user));
        req.setPostVariable("password", Encoding.urlEncode(pass));
        req.setPostVariable("login", "Log+In");
        req.load();

        GetRequest greq = new GetRequest(downloadLink.getDownloadURL());
        greq.getCookies().addAll(req.getCookies());
        greq.load();

        if (greq.containsHTML(NOT_AVAILABLE)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        } else if (greq.containsHTML(SERVER_DOWN)) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        } else {
            String link = "http://www.filefactory.com" + greq.getLocation();
            /* falls direct-download eingeschalten ist */
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
            if (requestInfo.getHeaders().get("Content-Type").toString().contains("text/html")) {
                /* falls direct-download ausgeschalten ist */
                requestInfo = HTTP.getRequest(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
                link = new Regex(requestInfo.getHtmlCode(), Pattern.compile(PREMIUM_LINK, Pattern.CASE_INSENSITIVE)).getMatch(0);

                if (link == null) {
                    logger.warning("Account Settings invalid");
                    linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                    linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                    linkStatus.setErrorMessage("Logins incorrect. Check Login and password");
                    return;

                }

                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), greq.getCookieString(), downloadLink.getDownloadURL(), true);
            }
        }

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
    	if(downloadLink.getDownloadURL().matches("sjdp://.*")) return true;
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        try {

            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);

            if (requestInfo.containsHTML(NOT_AVAILABLE)) {
                return false;
            } else if (requestInfo.containsHTML(SERVER_DOWN)) {
                return false;
            } else {

                String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode().replaceAll("\\&\\#8203\\;", ""), FILENAME).getMatch(0));
                int length = 0;
                Double fileSize = Double.parseDouble(new Regex(requestInfo.getHtmlCode(), FILESIZE).getMatch(0).replaceAll(",", ""));

                String unit = new Regex(requestInfo.getHtmlCode(), FILESIZE).getMatch(1);

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
    	if(downloadLink.getDownloadURL().matches("sjdp://.*")) return super.getFileInformationString(downloadLink);
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void init() {
    }

    @Override
    public void reset() {

        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;
        wait = 0;
    }

    @Override
    public void resetPluginGlobals() {

        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;
        wait = 0;

    }

}
