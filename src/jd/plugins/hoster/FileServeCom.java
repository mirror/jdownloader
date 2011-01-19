//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://(www\\.)?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 2 })
public class FileServeCom extends PluginForHost {

    public String        FILEIDREGEX = "fileserve\\.com/file/([a-zA-Z0-9]+)(http:.*)?";
    public static String agent       = RandomUserAgent.generate();

    private boolean      isFree      = false;

    public FileServeCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileserve.com/premium.php");
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("submit=Check+Urls&urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                this.br.getPage("http://fileserve.com/link-checker.php");
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                this.br.postPage("http://fileserve.com/link-checker.php", sb.toString());
                for (final DownloadLink dl : links) {
                    final String fileid = new Regex(dl.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "(<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>))";
                    final String theData = this.br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, "<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>)");
                    final String status = linkinformation.getMatch(3);
                    String filename = linkinformation.getMatch(1);
                    final String filesize = linkinformation.getMatch(2);
                    if (filename == null || filesize == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("Available") || filename.equals("--") || filesize.equals("--")) {
                        filename = fileid;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        final String fileId = new Regex(link.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
        link.setUrlDownload("http://fileserve.com/file/" + fileId);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.handleErrors(br);
        final String fileId = this.br.getRegex("fileserve\\.com/file/([a-zA-Z0-9]+)").getMatch(0);
        this.br.setFollowRedirects(false);
        String captchaJSPage = this.br.getRegex("\"(/landing/.*?/download_captcha\\.js)\"").getMatch(0);
        if (captchaJSPage == null) {
            logger.warning("captchaJSPage is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        captchaJSPage = "http://fileserve.com" + captchaJSPage;
        final Browser br2 = this.br.cloneBrowser();
        br2.setCustomCharset("utf-8");
        // It doesn't work without accessing this page!!
        br2.getPage(captchaJSPage);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(downloadLink.getDownloadURL(), "checkDownload=check");
        if (!br2.containsHTML("success\":\"showCaptcha\"")) {
            logger.info("There seems to be an error, no captcha is shown!");
            handleCaptchaErrors(br2, downloadLink);
            handleErrors(br2);
        }
        // Captcha should appear always
        // if
        // (!this.br.containsHTML("<div id=\"captchaArea\" style=\"display:none;\">")
        // || br2.containsHTML("showCaptcha\\(\\);")) {
        Boolean failed = true;
        for (int i = 0; i <= 10; i++) {
            final String id = this.br.getRegex("var reCAPTCHA_publickey='(.*?)';").getMatch(0);
            if (!this.br.containsHTML("api\\.recaptcha\\.net") || id == null || fileId == null) {
                logger.warning("if or fileId is null or the browser doesn't contain the reCaptcha text...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(Form.MethodType.POST);
            reCaptchaForm.setAction("http://www.fileserve.com/checkReCaptcha.php");
            reCaptchaForm.put("recaptcha_shortencode_field", fileId);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
            rc.setForm(reCaptchaForm);
            rc.setId(id);
            rc.load();
            final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
            final String c = this.getCaptchaCode(cf, downloadLink);
            if (c == null || c.length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Recaptcha failed");
            rc.getForm().put("recaptcha_response_field", c);
            rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
            br2.submitForm(rc.getForm());
            if (br2.containsHTML("incorrect-captcha-sol")) {
                this.br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        handleCaptchaErrors(br2, downloadLink);
        handleErrors(br2);
        // }
        this.br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        if (!this.br.getHttpConnection().isOK()) {
            logger.warning("The connection is not okay...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String reconTime = br.toString();
        int tt = 60;
        if (reconTime.length() < 500) {
            reconTime = new Regex(reconTime, ".*?(\\d+).*?").getMatch(0);
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime.trim());
        } else {
            logger.warning("Couldn't find dynamic waittime");
            logger.warning(br.toString());
        }
        this.sleep(tt * 1001, downloadLink);
        br2.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        this.br.postPage(downloadLink.getDownloadURL(), "download=normal");
        final String dllink = this.br.getRedirectLocation();
        if (dllink == null) {
            this.handleErrors(br);
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, false, 1);
        if (this.dl.getConnection().getResponseCode() == 404) {
            logger.info("got a 404 error...");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection().getContentType().contains("html")) {
            logger.info("The finallink doesn't seem to be a file...");
            this.br.followConnection();
            this.handleErrors(br);
            logger.warning("Unexpected error at the last step...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account);
        } catch (final PluginException e) {
            if (this.br.containsHTML("Username doesn't exist")) {
                ai.setStatus("Username doesn't exist");
            } else {
                ai.setStatus("Account Invalid");
            }
            account.setValid(false);
            return ai;
        }
        final String uploadedFiles = this.br.getRegex("<h5>Files uploaded:</h5>\r\n[ ]+<h3>(\\d+)<span>").getMatch(0);
        if (uploadedFiles != null) {
            ai.setFilesNum(Integer.parseInt(uploadedFiles));
        }
        ai.setUnlimitedTraffic();
        if (this.isFree) {
            ai.setStatus("Registered (free) User");
        } else {
            ai.setStatus("Premium User");
        }
        final String expires = this.br.getRegex("<h4>Premium Until</h4></th> <td><h5>(.*?) EST</h5>").getMatch(0);
        if (expires != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd MMMM yyyy", null));
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        int maxdls = -1;
        try {
            if (AccountController.getInstance().getValidAccount(this).getStringProperty("type") != null) {
                maxdls = 1;
            }
        } catch (final Exception e) {

        }
        return maxdls;
    }

    private void handleErrors(Browser br2) throws PluginException {
        logger.info("Handling errors...");
        if (br2.containsHTML("li>This file has been deleted by the system")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("File not available, please register as <a href=\"/login\\.php\">Premium</a> Member to download<br")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileServeCom.errors.only4premium", "This file is only downloadable for premium users")); }
        if (br2.containsHTML(">Your download link has expired")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download link expired, contact fileserve support", 10 * 60 * 1000l); }
        if (br2.containsHTML("Captcha error") || this.br.containsHTML("incorrect-captcha")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        final String wait = br2.getRegex("You (have to|need to) wait (\\d+) seconds to start another download").getMatch(1);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l); }
        if (br2.containsHTML("landing-406.php")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
        if (br2.containsHTML("(<h1>404 - Page not found</h1>|<p>We are sorry...</p>|<p>The page you were trying to reach wasn't there\\.</p>|<p>You can only download 1 file at a time|URL=http://www\\.fileserve\\.com/landing-403\\.php\")")) { throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL Server error, contact fileserve support"); }
    }

    private void handleCaptchaErrors(Browser br2, DownloadLink downloadLink) throws IOException, PluginException {
        // Handles captcha errors and additionsl limits
        logger.info("Checking captcha errors...");
        String fail = br2.getRegex("\"(fail|error)\":\"(.*?)\"").getMatch(1);
        String waittime = br2.getRegex("\"(waitTime|msg)\":(\\d+)").getMatch(1);
        if (fail != null && waittime != null) {
            if (fail.equals("captcha-fail") || fail.equals("captchaFail")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail + "&waitTime=" + waittime);
            // Just an additional check
            if (br2.containsHTML("Please retry later\\.<") || br2.containsHTML(">Your IP has failed the captcha too many times")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        } else if (fail != null) {
            // This coiuld be a limit message which appears after posting this,
            // it should then be handled with handleErrors
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.requestFileInformation(downloadLink);
        this.getDownloadUrlPage(downloadLink);
        this.doFree(downloadLink);
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        this.login(account);
        if (this.isFree) {
            this.getDownloadUrlPage(link);
            this.doFree(link);
        } else {
            this.br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            handleErrors(br);
            this.br.postPage(link.getDownloadURL(), "download=premium");
            final String dllink = this.br.getRedirectLocation();
            if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, true, 0);
            if (this.dl.getConnection().getResponseCode() == 404) {
                this.br.followConnection();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.dl.getConnection().getContentType().contains("html")) {
                this.br.followConnection();
                if (this.dl.getConnection().getLongContentLength() == 0) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                this.handleErrors(br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.setFilenameFix(true);
            this.dl.startDownload();
        }
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        this.br.getPage("http://fileserve.com/login.php");
        if (this.br.containsHTML("This service is temporarily not available for your service area")) {
            final AccountInfo acInfo = new AccountInfo();
            logger.info("This service is temporarily not available for your service area");
            acInfo.setStatus("Your country is blocked by fileserve!");
            account.setAccountInfo(acInfo);
            // Show the user for 20 seconds that the account is blocked, the
            // deactivate it
            Thread.sleep(20 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        /* username and pass are limited to 20 chars */
        String username = account.getUser();
        String password = account.getPass();
        if (username != null && username.length() > 20) {
            username = username.substring(0, 20);
        }
        if (password != null && password.length() > 20) {
            password = password.substring(0, 20);
        }
        this.br.postPage("http://fileserve.com/login.php", "loginUserName=" + Encoding.urlEncode(username) + "&loginUserPassword=" + Encoding.urlEncode(password) + "&loginFormSubmit=Login");
        if (this.br.containsHTML("Username doesn't exist")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        this.br.getPage("http://fileserve.com/dashboard.php");
        String accType = this.br.getRegex("<h5>Account type:</h5>[\r\n ]+<h3>(Premium|Free)</h3>").getMatch(0);
        if (accType == null) {
            accType = this.br.getRegex("<h4>Account Type</h4></td> <td><h5 class=\"inline\">(Premium|Free)([ ]+)?</h5>").getMatch(0);
        }
        if (this.br.getCookie("http://fileserve.com", "cookie") == null || accType == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (!accType.equals("Premium")) {
            try {
                account.setMaxSimultanDownloads(1);
            } catch (final Throwable e) {
                /* not available in 0.9xxx */
            }
            account.setProperty("type", "free");
            this.isFree = true;
        } else {
            account.setProperty("type", null);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.br.getHeaders().put("User-Agent", agent);
        this.checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    private void getDownloadUrlPage(DownloadLink downloadLink) throws IOException {
        // To get the english language
        br.postPage(downloadLink.getDownloadURL(), "locale=en-us");
        if (!br.getURL().equals(downloadLink.getDownloadURL())) br.getPage(downloadLink.getDownloadURL());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}