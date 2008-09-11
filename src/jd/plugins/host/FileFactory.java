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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
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

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "downloading too many files";

    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/securimage/securimage_show.php\\?[^\"]*)\" alt=");
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";
    private static final String WAIT_TIME = "wait ([0-9]+) (minutes|seconds)";
    private String actionString;

    private String captchaAddress;
    private File captchaFile;
    private String postTarget;
    private RequestInfo requestInfo;

    private int wait;

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        if (parameter.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost)PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(parameter);
           
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

        Browser.download(captchaFile, captchaAddress);
         
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

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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

        br.getPage("http://www.filefactory.com/members/details/premium/usage/");

        String[] dat = br.getRegex("You have downloaded (.*?) in the last 24 hours.*?Your daily limit is (.*?), and your download usage will be reset ").getRow(0);
        long gone;
        if (dat == null && Regex.matches(br, "You have not downloaded anything")) {

            gone = 0;
        } else {

            gone = Regex.getSize(dat[0].replace(",", ""));
        }
        ai.setTrafficMax(12 * 1024 * 1024 * 1024l);
        ai.setTrafficLeft(12 * 1024 * 1024 * 1024l - gone);
        return ai;
    }

    // by eXecuTe
    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost)PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            
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
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");

        Form login = br.getFormbyValue("Log in");
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(true);
        HTTPConnection con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.getHeaderField("Content-Disposition") == null) {
            br.followConnection();

            if (br.containsHTML(NOT_AVAILABLE)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            } else if (br.containsHTML(SERVER_DOWN)) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(20 * 60 * 1000l);
                return;
            } else {
                String red = br.getRegex("Description: .*?p style=.*?><a href=\"(.*?)\".*?>.*?Click here to begin your download").getMatch(0);
                con = br.openGetConnection(red);

            }
        }

        dl = new RAFDownload(this, downloadLink, con);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.startDownload();
        return;
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
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return true;
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(NOT_AVAILABLE)) {
            br.setFollowRedirects(false);
            return false;
        } else if (br.containsHTML(SERVER_DOWN)) {
            br.setFollowRedirects(false);
            return false;
        } else {

            String fileName = Encoding.htmlDecode(new Regex(br.toString().replaceAll("\\&\\#8203\\;", ""), FILENAME).getMatch(0));

            String fileSize = new Regex(br.toString(), FILESIZE).getMatch(-1);

            downloadLink.setName(fileName);
            downloadLink.setDownloadSize(Regex.getSize(fileSize));

        }
        br.setFollowRedirects(false);
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return super.getFileInformationString(downloadLink);
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void init() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
