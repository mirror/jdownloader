//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitshare.com" }, urls = { "http://(www\\.)?bitshare\\.com/(\\?(f|m)=[a-z0-9]{8}|files/[a-z0-9]{8})" }, flags = { 2 })
public class BitShareCom extends PluginForHost {

    private static final String  JSONHOST         = "http://bitshare.com/files-ajax/";
    private static final String  AJAXIDREGEX      = "var ajaxdl = \"(.*?)\";";
    private static final String  FILEIDREGEX      = "bitshare\\.com/.*?([a-z0-9]{8})";
    private static final String  DLLINKREGEX      = "SUCCESS#(http://.+)";
    private static final String  MAINPAGE         = "http://bitshare.com/";
    private static AtomicInteger maxPrem          = new AtomicInteger(1);
    private static Object        LOCK             = new Object();
    private final char[]         FILENAMEREPLACES = new char[] { '-' };
    private static final String  LINKOFFLINE      = "(>We are sorry, but the requested file was not found in our database|>Error \\- File not available<|The file was deleted either by the uploader, inactivity or due to copyright claim)";

    private String               agent            = null;

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://bitshare.com/?f=" + new Regex(link.getDownloadURL(), "([a-z0-9]{8})$").getMatch(0));
    }

    public BitShareCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1000l);
        this.enablePremium("http://bitshare.com/premium.html");
    }

    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier.replaceAll("([^a-zA-Z0-9]+)", "");
    }

    public boolean isHosterManipulatesFilenames() {
        return true;
    }

    public char[] getFilenameReplaceMap() {
        return FILENAMEREPLACES;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://bitshare.com/myaccount.html");
        String filesNum = br.getRegex("<a href=\"http://bitshare\\.com/myfiles\\.html\">(\\d+) files</a>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        String space = br.getRegex("<b>Storage</b><br />([\r\n\t ]+)?([\\d+\\.]+ (b|mb|gb|tb)) / \\d+").getMatch(1);
        if (space != null) ai.setUsedSpace(space.trim());
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (account.getBooleanProperty("freeaccount")) {
            ai.setStatus("Registered (free) User");
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
        } else {
            String expire = br.getRegex("Valid until: (.*?)</div>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "yyyy-MM-dd", null));
            }
            ai.setStatus("Premium User");
            try {
                maxPrem.set(5);
                account.setMaxSimultanDownloads(5);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://bitshare.com/terms-of-service.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(LINKOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (br.containsHTML("Only Premium members can access this file\\.<")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.bitsharecom.premiumonly", "Only downloadable for premium users!"));
        if (br.containsHTML("Sorry, you cant download more then 1 files at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("> Your Traffic is used up for today")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        try {
            br.cloneBrowser().getPage("http://bitshare.com/getads.html");
            br.cloneBrowser().getPage("http://bitshare.com/getads.html");
        } catch (final Throwable e) {
            logger.info("Adv-Speed-gain failed!");
        }
        final String fileID = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        final String ajax_url = JSONHOST + fileID + "/request.html";
        if (br.containsHTML("You reached your hourly traffic limit")) {
            String wait = br.getRegex("id=\"blocktimecounter\">(\\d+) Seconds</span>").getMatch(0);
            if (wait != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            } else {
                wait = br.getRegex("var blocktime = (\\d+);").getMatch(0);
                if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l); }
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        String tempID = br.getRegex(AJAXIDREGEX).getMatch(0);
        if (fileID == null || tempID == null) {
            logger.warning("fileID or tempID is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Try to find stream links */
        String dllink = br.getRegex("scaling: \\'fit\\',[\t\n\r ]+url: \\'(http://[^<>\"\\']+)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://s\\d+\\.bitshare\\.com/stream/[^<>\"\\']+)\\'").getMatch(0);
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br2.getHeaders().put("Accept", "text/html, */*");
        br2.postPage(ajax_url, "request=generateID&ajaxid=" + tempID);
        br.cloneBrowser().getPage("http://bitshare.com/getiframepopup_download.html?anticache=" + System.currentTimeMillis());
        String regexedWait = null;
        regexedWait = br2.getRegex("\\w+:(\\d+):").getMatch(0);
        int wait = 45;
        if (regexedWait != null) {
            wait = Integer.parseInt(regexedWait);
            logger.info("Waittime Regex Worked!, regexed waittime = " + wait);
        } else {
            logger.warning("Waittime Regex Failed!, failsafe waittime = " + wait);
        }
        wait += 3;
        sleep(wait * 1001l, downloadLink);
        String id = br.getRegex("http://api\\.recaptcha\\.net/challenge\\?k=(.*?)\"").getMatch(0);
        if (id != null) {
            Boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                id = br.getRegex("http://api\\.recaptcha\\.net/challenge\\?k=(.*?)\"").getMatch(0);
                if (id == null) {
                    logger.warning("id is null...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                id = id.trim();
                Form reCaptchaForm = new Form();
                reCaptchaForm.setMethod(Form.MethodType.POST);
                reCaptchaForm.setAction(ajax_url);
                reCaptchaForm.put("request", "validateCaptcha");
                reCaptchaForm.put("ajaxid", tempID);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setForm(reCaptchaForm);
                rc.setId(id);
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                br2.postPage(ajax_url, "request=validateCaptcha&ajaxid=" + tempID + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br2.containsHTML("ERROR:incorrect\\-captcha")) {
                    try {
                        invalidateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                    br.getPage(downloadLink.getDownloadURL());
                    continue;
                } else {
                    try {
                        validateLastChallengeResponse();
                    } catch (final Throwable e) {
                    }
                }
                failed = false;
                break;
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        /** For files */
        if (dllink == null) {
            br2.postPage(JSONHOST + fileID + "/request.html", "request=getDownloadURL&ajaxid=" + tempID);
            if (br2.containsHTML("Your Traffic is used up for today"))
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            else if (br2.containsHTML("Sorry, you cant download more then|You are not able to start a new download right now"))
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
            else if (br2.containsHTML("ERROR#SESSION ERROR\\!")) {
                logger.info("Session error, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            dllink = br2.getRegex(DLLINKREGEX).getMatch(0);
            if (dllink == null) {
                logger.severe(br2.toString());
                logger.warning("The dllink couldn't be found!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // Remove new line
        dllink = dllink.replaceAll("%0D%0A", "").trim();
        logger.info("Fixed dllink...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            errorHandling(downloadLink, br);
            logger.warning("Unhandled error happened before the download");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    private void errorHandling(final DownloadLink link, final Browser br) throws PluginException {
        if (br.getURL().contains("filenotfound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>|bad try)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        if (br.containsHTML("No input file specified|Connection to main server failed")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        if (br.containsHTML("You don\\'t have necessary rights to start this download or your session has timed out")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 60 * 1000l);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(LINKOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (account.getBooleanProperty("freeaccount")) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                String tempID = br.getRegex(AJAXIDREGEX).getMatch(0);
                String fileID = new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage(JSONHOST + fileID + "/request.html", "request=generateID&ajaxid=" + tempID);
                br.postPage(JSONHOST + fileID + "/request.html", "request=getDownloadURL&ajaxid=" + tempID);
                dllink = br.getRegex(DLLINKREGEX).getMatch(0);
            }
            if (dllink == null) {
                if (br.containsHTML("Your Traffic is used up for today.")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* max 15 connections at all */
            // Remove new line
            dllink = dllink.trim();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -3);
            if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getLongContentLength() < link.getDownloadSize()) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                errorHandling(link, br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    // TODO: Implement API: http://bitshare.com/openAPI.html and maybe use
    // downloadhandling from their tool: http://bitshare.com/tool.html
    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                if (agent == null) {
                    /*
                     * we first have to load the plugin, before we can reference it
                     */
                    JDUtilities.getPluginForHost("mediafire.com");
                    agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
                }
                br.getHeaders().put("User-Agent", agent);
                br.setCookie(MAINPAGE, "language_selection", "EN");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.postPage("http://bitshare.com/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&rememberlogin=&submit=Login");
                if (br.containsHTML(">Free</a></b>")) {
                    account.setProperty("freeaccount", true);
                } else if (br.containsHTML("\\(<b>Premium</b>\\)")) {
                    account.setProperty("freeaccount", Property.NULL);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("freeaccount", Property.NULL);
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        prepBR();
        br.postPage("http://bitshare.com/api/openapi/general.php", "action=getFileStatus&files=" + Encoding.urlEncode(link.getDownloadURL()));
        // http://svn.jdownloader.org/issues/21377
        final String ip = br.getRedirectLocation();
        if (ip != null && ip.contains("http://192.168.")) {
            // some local server fkup?
            Thread.sleep(5000);
            br = new Browser();
            prepBR();
            br.postPage("http://bitshare.com/api/openapi/general.php", "action=getFileStatus&files=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("http://192.168.")) return AvailableStatus.UNCHECKABLE;

        }

        if (br.containsHTML("#offline#")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        if (br.containsHTML("No htmlCode read")) {
            link.getLinkStatus().setStatusText("Server error, uncheckable");
            return AvailableStatus.UNCHECKABLE;
        }

        final Regex fileInfo = br.getRegex("#online#[a-z0-9]{8}#([^<>\"]*?)#(\\d+)");

        if (br.containsHTML("(>We are sorry, but the requested file was not found in our database|>Error \\- File not available<|The file was deleted either by the uploader, inactivity or due to copyright claim)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = fileInfo.getMatch(0);
        final String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String suggestedName = null;
        /* special filename modding if we have a suggested filename */
        if ((suggestedName = (String) link.getProperty("SUGGESTEDFINALFILENAME", (String) null)) != null) {
            String finalFilename2 = suggestedName.replaceAll("(\\[|\\])", "-");
            if (finalFilename2.equalsIgnoreCase(filename)) {
                link.setFinalFileName(suggestedName);
            }
        }
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    private void prepBR() {
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        br.setCookie(MAINPAGE, "language_selection", "EN");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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