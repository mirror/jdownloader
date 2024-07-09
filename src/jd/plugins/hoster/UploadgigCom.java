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

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadgig.com" }, urls = { "https?://(?:www\\.)?uploadgig\\.com/file/download/([A-Za-z0-9]+)(/[A-Za-z0-9%\\.\\-_]+)?" })
public class UploadgigCom extends antiDDoSForHost {
    @Override
    protected long getStartIntervall(DownloadLink downloadLink, Account account) {
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return 2000;
        }
        return super.getStartIntervall(downloadLink, account);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public UploadgigCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://uploadgig.com/premium");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    @Override
    public void init() {
        try {
            Browser.setBurstRequestIntervalLimitGlobal("uploadgig.com", 500, 10, 20000);
        } catch (Throwable t) {
        }
    }

    @Override
    public String getAGBLink() {
        return "https://uploadgig.com/page/content/term-of-service";
    }

    private void setConstants(final Account account) {
        if (account == null) {
            // non account
            chunks = 1;
            resumes = false;
            directlinkproperty = "freelink";
        } else if (AccountType.FREE.equals(account.getType())) {
            // free account
            chunks = 1;
            resumes = false;
            directlinkproperty = "freelink2";
        } else if (AccountType.PREMIUM.equals(account.getType())) {
            // prem account
            chunks = 0; // 2021-10-20
            resumes = true;
            directlinkproperty = "premium_directlink";
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    private boolean resumes            = false;
    private int     chunks             = 1;
    private String  directlinkproperty = null;

    private Browser prepBR(final Browser br) {
        /* 2020-06-15: Use old website (?) */
        br.getHeaders().put("Direct", "1");
        br.setAllowedResponseCodes(new int[] { 406 });
        return br;
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("(?i)^http://", "https://");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        prepBR(br);
        final String contenturl = getContentURL(link);
        final String url_filename = new Regex(contenturl, "([^/]+)$").getMatch(0);
        if (!link.isNameSet() && url_filename != null) {
            /* Set temp name */
            link.setName(url_filename);
        }
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"filename\">([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"pg_title\">Download \"([^<>\"]+)\"").getMatch(0);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        final String filesize = br.getRegex("class=\"filesize\">\\[([^<]+)\\]<").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        setConstants(null);
        doFree(null, link);
    }

    private void doFree(final Account account, final DownloadLink link) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        String directurl = this.checkDirectLink(br, link, directlinkproperty);
        if (directurl != null && !testLink(br, link, directurl, false)) {
            directurl = null;
        }
        if (directurl == null) {
            if (account != null) {
                // login method might have validated cookies
                getPage(getContentURL(link));
            }
            /* 2020-07-10: See http://uploadgig.com/static/tpl2/js/f45862367.js?v=0.0.2 */
            // premium only content
            if (br.containsHTML(">\\s*This file can be downloaded by Premium Member only")) {
                throw new AccountRequiredException();
            }
            String csrf_tester = br.getCookie(this.getHost(), "firewall");
            if (csrf_tester == null) {
                csrf_tester = br.getRegex("name=\"csrf_tester\"\\s*?value=\"([^<>\"]+)\"").getMatch(0);
                if (csrf_tester != null) {
                    br.setCookie(this.br.getHost(), "firewall", csrf_tester);
                }
            }
            final String fid = getFID(link);
            br.setCookie(br.getHost(), "last_file_code", fid);
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            String postData = "file_id=" + fid + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
            if (csrf_tester != null) {
                postData += "&csrf_tester=" + csrf_tester;
            }
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Accept", "*/*");
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(br2, "/file/free_dl", postData);
            errorhandlingFree(br2);
            if (br2.getHttpConnection().getResponseCode() == 403) {
                /* Usually only happens with wrong POST values */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
            }
            final String waittime_str = PluginJSonUtils.getJsonValue(br2, "cd");
            final String idStr = PluginJSonUtils.getJson(br2, "id");
            final String url = PluginJSonUtils.getJson(br2, "sp");
            final String params = PluginJSonUtils.getJson(br2, "q");
            if (StringUtils.isEmpty(url) || StringUtils.isEmpty(params) || StringUtils.isEmpty(idStr) || !idStr.matches("\\d+") || StringUtils.isEmpty(waittime_str) || !waittime_str.matches("\\d+")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error occured");
            }
            final long substraction_value = calc(br);
            final long id = Long.parseLong(idStr) - substraction_value;
            directurl = url + "id=" + id + "&" + params;
            // directurl = directurl.replace("/start/", "/s/");
            this.sleep(Integer.parseInt(waittime_str) * 1001l, link);
            testLink(br, link, directurl, true);
            // they use javascript to determine finallink...
            // getDllink(br2);
        }
        if (dl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            // ok browser set by another method is now lost.
            final Browser dlBr = dl.getDownloadable().getContextBrowser();
            dlBr.followConnection(true);
            /* E.g. "The download link has expired, please buy premium account or start download file from the beginning." */
            errorhandlingGeneral(dlBr);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    protected long calc(Browser br) throws Exception {
        final String calc = br.getRegex("<script>\\s*window\\[[^<>]*\\]\\s*=\\s*(.*?)\\s*;\\s*</script>").getMatch(0);
        if (calc == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("var result=" + calc);
            final Number result = (Number) engine.get("result");
            return result.longValue();
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }

    private boolean testLink(final Browser br, final DownloadLink downloadLink, final String dllink, boolean throwException) throws Exception {
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, downloadLink, dllink, resumes, chunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                brc.followConnection(true);
                errorhandlingGeneral(brc);
                return false;
            } else {
                return true;
            }
        } catch (final Exception e) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable ee) {
            }
            if (throwException) {
                throw e;
            } else if (e instanceof PluginException) {
                throw e;
            } else {
                logger.log(e);
                return false;
            }
        }
    }

    private String checkDirectLink(final Browser br, final DownloadLink link, final String property) throws Exception {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            boolean throwException = false;
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                con = brc.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    brc.followConnection(true);
                    try {
                        errorhandlingGeneral(brc);
                    } catch (PluginException e) {
                        if (e.getLinkStatus() == LinkStatus.ERROR_FATAL) {
                            throwException = true;
                        }
                        throw e;
                    }
                    throw new IOException();
                }
            } catch (final Exception e) {
                if (throwException) {
                    throw e;
                } else {
                    link.removeProperty(property);
                    logger.log(e);
                    return null;
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void errorhandlingFree(final Browser checkBR) throws PluginException {
        if ("b".equals(checkBR.toString())) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Proxy/VPN blocked", 1 * 60 * 60 * 1000l);
        } else if ("m".equals(checkBR.toString())) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reached the download limit for the hour", 1 * 60 * 60 * 1000l);
        } else if ("0".equals(checkBR.toString())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        } else if ("fl".equalsIgnoreCase(checkBR.toString())) {
            throw new AccountRequiredException();
        } else if ("rfd".equalsIgnoreCase(checkBR.toString())) {
            /* File exceeded max number of free downloads --> Buy premium */
            throw new AccountRequiredException();
        }
        // "0" and "e" shouldn't happen
    }

    private void errorhandlingGeneral(final Browser br) throws PluginException {
        if (br.containsHTML("File not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // File not found, for more information contact the file owner(uploader) or send us a ticket
        } else if (br.containsHTML("The download link has expired")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'The download link has expired'", 30 * 60 * 1000l);
        } else if (br.containsHTML("Sorry, this server is under maintenance|>please try to download other files, other servers are")) {
            /* 2020-12-04 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server under maintenance", 5 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403 && br.containsHTML("Blocked!\\s*<br>\\s*If you are using VPN or proxy, disable your proxy and try again")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Blocked connection!");
        }
        checkAccountErrors(this.br);
    }

    private void checkAccountErrors(final Browser br) throws AccountUnavailableException {
        if (br.getHttpConnection().getResponseCode() == 406) {
            /* 2021-01-20: HTTP/1.1 406 Not Acceptable Content: "406 Too many tries." */
            throw new AccountUnavailableException("406", 10 * 60 * 1000l);
        }
    }

    /**
     * 2021-02-01: Important: Host only allows one session per account. E.g. if user logs in via browser, this will invalidate JDownloaders
     * session -> Can cause a lot of login-captchas!
     */
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Avoid login-captcha whenever possible */
                    logger.info("Attempting cookie login");
                    br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l && !force) {
                        logger.info("Trust login cookies as they're not yet that old");
                        return;
                    }
                    logger.info("Checking login cookies");
                    getPage("http://" + this.getHost());
                    /**
                     * 2021-01-01: It is also possible to check via: https://uploadgig.com/login/form --> Returns raw text "You are
                     * currently logged in." on success!
                     */
                    if (this.isLoggedIN()) {
                        logger.info("Cookie login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Attempting full login");
                getPage("https://" + account.getHoster());
                final Browser brc = br.cloneBrowser();
                getPage(brc, "https://" + account.getHoster() + "/login/form");
                final Form loginform = brc.getForm(0);
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    checkAccountErrors(this.br);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                loginform.put("rememberme", "1");
                /*
                 * Handle login-captcha if required. If IP is flagged, login-captcha is always required. In all other cases, captcha won't
                 * be required for the first login attempt of a session.
                 */
                final boolean captchaHidden = loginform.containsHTML("class=\"row hideme\"");
                if (this.containsRecaptchaV2Class(brc) && !captchaHidden) {
                    /* 2020-05-15: New */
                    logger.info("Login captcha required");
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brc).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                } else {
                    logger.info("Login captcha NOT required");
                }
                Request request = brc.createFormRequest(loginform);
                request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                sendRequest(request);
                if (!isLoggedIN()) {
                    checkAccountErrors(this.br);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    private boolean isLoggedIN() {
        final boolean missing = br.getCookie(this.getHost(), "fs_secure", Cookies.NOTDELETEDPATTERN) != null;
        return missing;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        getPage("/user/my_account");
        final Regex trafficregex = br.getRegex("<dt>Daily traffic usage:?</dt>\\s*<dd>\\s*([0-9\\,\\.]+)\\s*/\\s*([0-9\\,\\.]+)\\s*MB");
        final String traffic_used_str = trafficregex.getMatch(0);
        final String traffic_max_str = trafficregex.getMatch(1);
        final String timeLeft = br.getRegex(">\\s*\\(([a-z0-9\\s]*)left\\s*\\)\\s*</").getMatch(0);
        Long timeStamp = null;
        String expire = br.getRegex("Package expire date:</dt>\\s*<dd>\\s*(\\d{4}/\\d{2}/\\d{2})").getMatch(0);
        if (expire == null) {
            expire = br.getRegex(">\\s*(\\d{4}/\\d{2}/\\d{2})<").getMatch(0);
        }
        if (expire != null) {
            timeStamp = TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd", Locale.ENGLISH);
            final String months = new Regex(timeLeft, "(\\d+) months?").getMatch(0);
            final String days = new Regex(timeLeft, "(\\d+) days?").getMatch(0);
            final String hours = new Regex(timeLeft, "(\\d+) hours?").getMatch(0);
            final String minutes = new Regex(timeLeft, "(\\d+) minutes?").getMatch(0);
            if (hours != null) {
                timeStamp += TimeUnit.HOURS.toMillis(Integer.parseInt(hours));
            }
            if (minutes != null) {
                timeStamp += TimeUnit.MINUTES.toMillis(Integer.parseInt(minutes));
            }
            if (months == null && days == null && (hours != null || minutes != null)) {
                timeStamp = System.currentTimeMillis();
                if (hours != null) {
                    timeStamp += TimeUnit.HOURS.toMillis(Integer.parseInt(hours));
                }
                if (minutes != null) {
                    timeStamp += TimeUnit.MINUTES.toMillis(Integer.parseInt(minutes));
                }
            }
            ai.setValidUntil(timeStamp);
        }
        if (timeStamp == null || ai.isExpired()) {
            ai.setValidUntil(-1);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        }
        if (traffic_used_str != null && traffic_max_str != null) {
            final long traffic_used = SizeFormatter.getSize(traffic_used_str + "MB");
            final long traffic_max = SizeFormatter.getSize(traffic_max_str + " MB");
            final long traffic_left = Math.max(0, traffic_max - traffic_used);
            ai.setTrafficMax(traffic_max);
            ai.setTrafficLeft(traffic_left);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        setConstants(account);
        /* 2020-05-15: Always verify cookies for this host! */
        login(account, true);
        if (account.getType() == AccountType.FREE) {
            doFree(account, link);
        } else {
            br.setFollowRedirects(false);
            String directurl = this.checkDirectLink(br, link, directlinkproperty);
            if (directurl != null && !testLink(br, link, directurl, false)) {
                directurl = null;
            }
            if (directurl == null) {
                // can be redirect
                final String contenturl = getContentURL(link);
                getPage(contenturl);
                directurl = br.getRedirectLocation();
                if (directurl == null) {
                    directurl = contenturl;
                }
                testLink(br, link, directurl, false);
            }
            if (dl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                final Browser dlBr = dl.getDownloadable().getContextBrowser();
                dlBr.followConnection(true);
                if (dlBr.containsHTML("Your \\d+Gb daily download traffic has been used\\.")) {
                    account.setNextDayAsTempTimeout(dlBr);
                }
                errorhandlingGeneral(dlBr);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
                dl.startDownload();
            }
        }
    }

    @Override
    protected void runPostRequestTask(Browser ibr) throws Exception {
        if (ibr.getHttpConnection() != null && ibr.getHttpConnection().getResponseCode() == 429) {
            if ("ERROR 702".equals(ibr.toString())) {
                // I have no idea what this means
                /* 2020-05-15: psp: Wow brilliant comment :D */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "702 Error", 2 * 60 * 1000l);
            }
            // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429", 5 * 60 * 1000l);
        }
        super.runPostRequestTask(ibr);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}