//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "http://(www\\.)?keep2share\\.cc/file/[a-z0-9]+" }, flags = { 2 })
public class Keep2ShareCc extends PluginForHost {

    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://keep2share.cc/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://keep2share.cc/page/terms.html";
    }

    private static final String DOWNLOADPOSSIBLE = ">Downloading file:<";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Sorry, an error occurred while processing your request")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename, filesize = null;
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            filename = br.getRegex(">Downloading file:</span><br>[\t\n\r ]+<span class=\"c2\">.*?alt=\"\" style=\"\">([^<>\"]*?)</span>").getMatch(0);
            filesize = br.getRegex("File size ([^<>\"]*?)</div>").getMatch(0);
        } else {
            filename = br.getRegex(">File: <span>([^<>\"]*?)</span>").getMatch(0);
            filesize = br.getRegex(">Size: ([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML("File size to large\\!<") || br.containsHTML("Only <b>Premium</b> access<br>")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
        }
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(DOWNLOADPOSSIBLE) || br.containsHTML(">If you want downloading file on slow speed")) {
                dllink = getDllink();
            } else {
                if (br.containsHTML("Traffic limit exceed\\!<br>|Download count files exceed!<br>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                final String uniqueID = br.getRegex("name=\"slow_id\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (uniqueID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.postPage(br.getURL(), "slow_id=" + uniqueID);
                final Regex reconWaittime = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this file");
                if (reconWaittime.getMatches().length == 1) {
                    int hours = 0, minutes = 0, seconds = 0;
                    hours = Integer.parseInt(reconWaittime.getMatch(0));
                    minutes = Integer.parseInt(reconWaittime.getMatch(1));
                    seconds = Integer.parseInt(reconWaittime.getMatch(2));
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1001));
                }
                if (br.containsHTML("Free account does not allow to download more than one file at the same time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    logger.info("Detected captcha method \"Re Captcha\" for this host");
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                    if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    rc.setId(id);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode(cf, downloadLink);
                    br.postPage(br.getURL(), "CaptchaForm%5Bcode%5D=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    final String captchaLink = br.getRegex("\"(/file/captcha\\.html\\?[^\"]+)\"").getMatch(0);
                    if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    final String code = getCaptchaCode("http://keep2share.cc" + captchaLink, downloadLink);
                    br.postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                    if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }

                /** Skippable */
                int wait = 30;
                final String waittime = br.getRegex("<div id=\"download\\-wait\\-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
                if (waittime != null) wait = Integer.parseInt(waittime);
                sleep(wait * 1001l, downloadLink);
                br.postPage(br.getURL(), "free=1&uniqueId=" + uniqueID);
                dllink = getDllink();
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)(\\'|\")").getMatch(1);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://keep2share.cc" + dllink;
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "http://keep2share.cc";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://keep2share.cc/login.html");
                String postData = "LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&yt0=Login&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                // Handle stupid login captcha
                final String captchaLink = br.getRegex("\"(/auth/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
                if (captchaLink != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "keep2share.cc", "http://keep2share.cc", true);
                    final String code = getCaptchaCode("http://keep2share.cc" + captchaLink, dummyLink);
                    postData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(code);
                }
                br.postPage("http://keep2share.cc/login.html", postData);
                if (!br.containsHTML("class=\"premium\">Premium:")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("class=\"premium\">Premium:[\t\n\r ]+(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
        if (expire == null) {
            if (br.containsHTML(">Premium:[\t\n\r ]+LifeTime")) {
                ai.setStatus("Premium Lifetime User");
            } else {
                account.setValid(false);
                return ai;
            }
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
            ai.setStatus("Premium User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("\\'(/file/url\\.html\\?file=[a-z0-9]+)\\'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "http://keep2share.cc" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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