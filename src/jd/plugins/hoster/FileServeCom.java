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
import java.lang.reflect.Field;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://(www\\.)?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 2 })
public class FileServeCom extends PluginForHost {

    public String         FILEIDREGEX         = "fileserve\\.com/file/([a-zA-Z0-9]+)(http:.*)?";
    public static String  agent               = RandomUserAgent.generate();
    // public static String agent =
    // "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0) Gecko/20100101 Firefox/8.0";
    private static Object LOCK                = new Object();
    private final String  COOKIE_HOST         = "http://fileserve.com/";
    private final String  DLYOURFILESUSERTEXT = "You can only download files which YOU uploaded!";
    private final String  DLYOURFILESTEXT     = "(>FileServe can only be used to download and retrieve files that you have uploaded personally|>If this file belongs to you, please login to download it directly from your file manager)";

    public FileServeCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileserve.com/premium.php");
    }

    /**
     * To get back the linkchecker API, revert to revision 15263, it was removed because it was broken (error 500)
     */
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        boolean seemsOkay = false;
        try {
            br.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("shortens=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links, probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 40) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(getID(dl)));
                    c++;
                }
                sb.append("&submit=Submit+Query");
                this.br.postPage("http://app.fileserve.com/api/linkchecker/", sb.toString());
                for (final DownloadLink dl : links) {
                    final String fileid = getID(dl);
                    if (fileid == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "\"" + fileid + "\":\\{(.*?)\\}";
                    final String theData = this.br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, "status\":\"(.*?)\".*?filename\":\"(.*?)\".*?filesize\":\"(\\d+)");
                    final String status = linkinformation.getMatch(0);
                    String filename = linkinformation.getMatch(1);
                    String filesize = linkinformation.getMatch(2);
                    if (filename == null || filesize == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equalsIgnoreCase("Available") || filename.equals("--") || filesize.equals("--")) {
                        filename = fileid;
                        dl.setAvailable(false);
                    } else {
                        seemsOkay = true;
                        dl.setAvailable(true);
                    }
                    dl.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(Long.parseLong(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        if (seemsOkay == false) {
            for (DownloadLink link : urls) {
                link.setAvailableStatus(AvailableStatus.UNCHECKED);
            }
        }
        return seemsOkay;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        link.setUrlDownload("http://fileserve.com/file/" + getID(link));
    }

    private void correctHeaders(Browser brrr) {
        brrr.getHeaders().put("User-Agent", agent);
        brrr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        brrr.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        brrr.getHeaders().put("Accept-Encoding", "gzip");
        brrr.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        brrr.getHeaders().put("Referer", "");
    }

    public void doFree(final DownloadLink downloadLink, boolean direct) throws Exception, PluginException {
        if (true) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
        sleep(new Random().nextInt(11) * 1000l, downloadLink);
        String dllink = null;
        if (direct) {
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
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
            String secret = br2.getRegex("ppp:\"(.*?)\"").getMatch(0);
            if (secret == null) secret = "301";
            br2.getPage("http://www.fileserve.com/script/fileserve.js");
            br2.getPage("http://www.fileserve.com/styles/landing/DL42/landing.css");
            br2.getPage("http://www.fileserve.com/script/recaptcha_ajax.js");
            setAjaxHeaders(br2);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=check");
            boolean nocaptcha = false;
            if (!br2.containsHTML("success\":\"showCaptcha\"")) {
                handleCaptchaErrors(br2, downloadLink);
                handleErrors(br2);
                logger.info("There seems to be an error, no captcha is shown!");
                logger.info(br2.toString());
                nocaptcha = true;
            }
            Boolean failed = true;
            if (nocaptcha == false) {
                for (int i = 0; i <= 10; i++) {
                    final String id = this.br.getRegex("var reCAPTCHA_publickey=\\'(.*?)\\';").getMatch(0);
                    if (!this.br.containsHTML("api\\.recaptcha\\.net") || id == null || fileId == null) {
                        handleCaptchaErrors(br2, downloadLink);
                        logger.warning("id or fileId is null or the browser doesn't contain the reCaptcha text...");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Form reCaptchaForm = new Form();
                    reCaptchaForm.setMethod(Form.MethodType.POST);
                    reCaptchaForm.setAction("http://www.fileserve.com/checkReCaptcha.php");
                    reCaptchaForm.put("recaptcha_shortencode_field", fileId);
                    reCaptchaForm.put("ppp", Encoding.urlEncode(secret));
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                    rc.setForm(reCaptchaForm);
                    rc.setId(id);
                    rc.load();
                    final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                    final String c = this.getCaptchaCode(cf, downloadLink);
                    if (c == null || c.length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Recaptcha failed");
                    rc.getForm().put("recaptcha_response_field", Encoding.urlEncode(c));
                    rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
                    br2.submitForm(rc.getForm());
                    if (br2.containsHTML("incorrect\\-captcha\\-sol")) {
                        handleCaptchaErrors(br2, downloadLink);
                        this.br.getPage(downloadLink.getDownloadURL());
                        continue;
                    }
                    failed = false;
                    break;
                }
                if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                handleCaptchaErrors(br2, downloadLink);
                handleErrors(br2);
            }
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
            br.setDebug(true);
            this.br.postPage(downloadLink.getDownloadURL(), "download=normal");
            dllink = this.br.getRedirectLocation();
        }
        if (dllink == null) {
            this.handleErrors(br);
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("dllink=" + dllink);
        if (dllink.contains("maintenance")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server maintenance", 60 * 60 * 1000l); }
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
        if (downloadLink.getFinalFileName() == null) {
            /* workaround for buggy server response, see #3545 */
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (name != null) {
                name = name.replaceAll("\\%\\%", "%25%");
                name = Encoding.htmlDecode(name);
                downloadLink.setFinalFileName(name);
            }
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            loginAPI(br, account);
        } catch (PluginException e) {
            try {
                logger.warning("API login failed, trying login via website!");
                loginSite(account, true);
            } catch (PluginException e2) {
                account.setValid(false);
            }
        }
        if (account.getAccountInfo() != null) {
            return account.getAccountInfo();
        } else {
            return ai;
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    private boolean getDownloadUrlPage(DownloadLink downloadLink) throws IOException {
        // To get the english language
        boolean b = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            br.postPage(downloadLink.getDownloadURL(), "locale=en-us");
        } finally {
            br.setFollowRedirects(b);
        }
        if (br.getRedirectLocation() != null) { return true; }
        if (!br.getURL().equals(downloadLink.getDownloadURL())) br.getPage(downloadLink.getDownloadURL());
        return false;
    }

    private String getID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void handleCaptchaErrors(Browser br2, DownloadLink downloadLink) throws IOException, PluginException {
        // Handles captcha errors and additionsl limits
        logger.info("Checking captcha errors...");
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Unexpected captcha error happened");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String fail = br2.getRegex("\"(fail|error)\":\"(.*?)\"").getMatch(1);
        String waittime = br2.getRegex("\"(waitTime|msg)\":(\\d+)").getMatch(1);
        if (fail != null && waittime != null) {
            if (fail.equals("captcha-fail") || fail.equals("captchaFail")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail + "&waitTime=" + waittime);
            // Just an additional check
            if (br2.containsHTML("Please retry later\\.<") || br2.containsHTML(">Your IP has failed the captcha too many times")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        } else if (fail != null) {
            // This could be a limit message which appears after posting this,
            // it should then be handled with handleErrors
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail);
        }
    }

    private void handleErrors(Browser br2) throws Exception {
        logger.info("Handling errors...");
        if (br2.containsHTML("<li>The file access is protected, please")) {
            logger.info("The file access is protected");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br2.containsHTML("Your daily download limit has been reached")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 2 * 60 * 60 * 1000l);
        if (br2.containsHTML("li>This file has been deleted by the system")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("li>This file was either in breach of a copyright holder or deleted by the uploader")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("The file could not be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("File not available, please register as <a href=\"/login\\.php\">Premium</a> Member to download<br")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileServeCom.errors.only4premium", "This file is only downloadable for premium users")); }
        if (br.getURL().contains("landing-error.php?error_code=1702") || br2.containsHTML("(>Your download link has expired|/landing\\-error\\.php\\?error_code=1702)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.FileServeCom.errors.dllinkexpired", "Download link expired, contact the fileserve support"), 10 * 60 * 1000l); }
        if (br2.containsHTML("Captcha error") || this.br.containsHTML("incorrect-captcha")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        final String wait = br2.getRegex("You (have to|need to) wait (\\d+) seconds to start another download").getMatch(1);
        if (wait != null) {
            logger.info("WaitRegex: " + wait);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
        }
        if (br2.containsHTML("<p>You can only download 1 file at a time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        if (br2.containsHTML(">FileServe can only be used to download and retrieve files that you have uploaded personally")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileServeCom.errors.only4owner", "This file is only downloadable for the one who uploaded it!")); }

        /** Landing error section */
        if (br2.containsHTML("landing-error\\.php\\?error_code=404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("(landing-406\\.php|landing\\-error\\.php\\?error_code=1703)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
        if (br2.containsHTML("landing\\-error\\.php\\?error_code=1707") || br2.getURL().contains("/landing-1707.html")) {
            logger.info("Received error 1707, account temporarily blocked, user has to change password to use it again!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br2.containsHTML("(landing\\-error\\.php\\?error_code=2702|is already downloading a file</li>|is already downloading a file <br>|landing\\-1403)") || br2.getURL().contains("landing-2702.html") || br.getURL().contains("landing-1403")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP is already downloading", 5 * 60 * 1000l);
        // error 612(Account gesperrt) --> Dialogfenster aufrufen
        if (br2.containsHTML("landing\\-error\\.php\\?error_code=612")) {
            Account blockedAcc = AccountController.getInstance().getValidAccount(this);
            if (blockedAcc != null && blockedAcc.isEnabled() && blockedAcc.isValid()) {
                handleErrorsShowDialog(blockedAcc);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "YOUR IP IS BANNED BY FILESERVE, PLEASE CONTACT THE FILESERVE SUPPORT!");
            }
        }
        if (br2.containsHTML("landing\\-error\\.php") || br2.getURL().contains("landing-")) {
            logger.warning("Unknown landing error!");
            logger.warning("Url = " + br2.getURL());
            logger.warning("html code = " + br2.toString());
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown landing error, please contact our support!");
        }
    }

    private void handleErrorsShowDialog(Account blocked) throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO
                .getInstance()
                .requestConfirmDialog(
                        UserIO.STYLE_LARGE,
                        "FileServe landing error 612(Account '" + blocked.getUser() + "' blocked)!",
                        "English:\n\nIt seems like your account has been blocked by fileserve.\r\nTo unblock it, please visit the fileserve Supportforum and contact the User \"RickyFS\".\r\nBy clicking on OK a browser instance will open which leads to the fileserve Supportforum.\r\n\nYour premium account has been deactivated in JD to prevent further problems.\r\nIf you leave it that way JDownloader will continue to download as a free user from fileserve.com.\r\rPolish:\r\rTwoje konto Premium zosta³o prawdopodobnie zablokowane przez serwis fileserve.\nAby je odblokowaæ, odwied¼ forum wsparcia tego serwisu i skontaktuj siê z u¿ytkownikiem \"RickyFS\".\nNaci¶niêcie klawisza OK wywo³a przegl±darkê z odpowiedni± stron± tego forum.\r\n\nTwoje konto Premium zosta³o wy³±czone przez JD aby zapobiec dalszym problemom.\rJD bêdzie kontynuowa³ ¶ci±ganie z serwisu fileserve.com, lecz jako u¿ytkownik darmowy (Free).\r\n\r\nJDTeam",
                        null, "OK", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.wjunction.com/95-file-hosts-official-support/35113-fileserve-make-money-upto-%2425-per-1000-downloads-official-thread.html"));
            } else {
                return;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.requestFileInformation(downloadLink);
        boolean direct = getDownloadUrlPage(downloadLink);
        this.doFree(downloadLink, direct);
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        String username = Encoding.urlEncode(account.getUser());
        String password = Encoding.urlEncode(account.getPass());
        String id = getID(link);
        br.postPage("http://app.fileserve.com/api/download/premium/", "username=" + username + "&password=" + password + "&shorten=" + id);
        String code = br.getRegex("error_code\":(\\d+)").getMatch(0);
        // Disabled because file(s) can still be downloaded via alternative
        // errorhandling
        // if ("305".equalsIgnoreCase(code) || "500".equalsIgnoreCase(code)) {
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
        // "ServerError", 30 * 60 * 1000l); }
        if ("403".equalsIgnoreCase(code) || "605".equalsIgnoreCase(code)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if ("606".equalsIgnoreCase(code) || "607".equalsIgnoreCase(code) || "608".equalsIgnoreCase(code)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String dllink = null;
        if ("302".equalsIgnoreCase(code)) dllink = br.getRegex("next\":\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            /**
             * This is another method to get the downloadlink. If this doesn't work, try via normal browser login
             */
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(username + ":" + password));
            br.getPage(link.getDownloadURL());
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            logger.warning("API couldn't find downloadlink, trying web-login and web-download.");
            loginSite(account, false);
            br.getPage(link.getDownloadURL());
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            if (br.containsHTML(DLYOURFILESTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replaceAll("\\\\/", "/");
        br.setFollowRedirects(true);
        // Sometimes slow servers
        br.setReadTimeout(60 * 1000);
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
        if (link.getFinalFileName() == null) {
            /* workaround for buggy server response, see #3545 */
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (name != null) {
                name = name.replaceAll("\\%\\%", "%25%");
                name = Encoding.htmlDecode(name);
                link.setFinalFileName(name);
            }
        }
        this.dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private Browser loginAPI(final Browser useBr, final Account account) throws IOException, PluginException {
        Browser br = useBr;
        if (br == null) br = new Browser();
        this.setBrowserExclusive();
        AccountInfo ai = account.getAccountInfo();
        if (ai == null) ai = new AccountInfo();
        account.setAccountInfo(ai);
        String username = Encoding.urlEncode(account.getUser());
        String password = Encoding.urlEncode(account.getPass());
        br.postPage("http://app.fileserve.com/api/login/", "username=" + username + "&password=" + password + "&submit=Submit+Query");
        String type = br.getRegex("type\":\"(.*?)\"").getMatch(0);
        if (!"premium".equalsIgnoreCase(type)) {
            String error_code = br.getRegex("error_code\":(\\d+)").getMatch(0);
            if ("110".equals(error_code)) {
                String error = br.getRegex("error_message\":\"(.*?)\"").getMatch(0);
                if (error != null) {
                    ai.setStatus(error);
                }
            }
            if ("free".equalsIgnoreCase(type)) {
                ai.setStatus("Free accounts are not supported!");
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String expire = br.getRegex("expireTime\":\"(.*?)\"").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", null));
            if (ai.isExpired()) {
                ai.setStatus("Expired");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Premium Account ok");
        return br;
    }

    @SuppressWarnings("unchecked")
    private void loginSite(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.postPage("http://fileserve.com/login.php", "loginUserName=" + Encoding.urlEncode(account.getUser()) + "&loginUserPassword=" + Encoding.urlEncode(account.getPass()) + "&autoLogin=on&ppp=102&loginFormSubmit=Login");
                if (br.getCookie(COOKIE_HOST, "cookie") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage("http://fileserve.com/dashboard.php");
                String type = br.getRegex("<h5>Account type:</h5>[\t\n\r ]+<h3>(Premium) <a").getMatch(0);
                if (type == null) type = br.getRegex("<h4>Account Type</h4></td> <td><h5 class=\"inline\">(Premium) </h5>").getMatch(0);
                if (type == null) {
                    logger.warning("No premiumaccount or login broken");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                String uploadedFiles = br.getRegex("<h5>Files uploaded:</h5>[\t\n\r ]+<h3>(\\d+)<span>").getMatch(0);
                String expire = br.getRegex("<h4>Premium Until</h4></td>[\t\n\r ]+<td><h5>(.*?) EST").getMatch(0);
                AccountInfo ai = new AccountInfo();
                account.setAccountInfo(ai);
                if (uploadedFiles != null) ai.setFilesNum(Integer.parseInt(uploadedFiles));
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", null));
                ai.setStatus("Premium Account ok, API login failed, site login OK");
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctHeaders(this.br);
        if (this.checkLinks(new DownloadLink[] { link }) == false) {
            /* linkcheck broken */
            try {
                br.getPage(link.getDownloadURL());
            } catch (final UnknownHostException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("fileserve.com/maintenance.html")) {
                link.getLinkStatus().setStatusText("This host is currently under maintenance");
                link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            }
            this.handleErrors(br);
            String filename = br.getRegex("down_arrow.*?h1>(.*?)<").getMatch(0);
            String filesize = br.getRegex("down_arrow.*?span.*?strong>.*?([0-9\\. GBMK]+)").getMatch(0);
            if (filename != null) {
                link.setName(filename);
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            if (filename != null) {
                link.setAvailableStatus(AvailableStatus.TRUE);
            } else {
                link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            }
        }
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void setAjaxHeaders(Browser brrr) {
        brrr.getHeaders().put("Accept", "application/json, text/javascript, */*");
        brrr.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        brrr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}