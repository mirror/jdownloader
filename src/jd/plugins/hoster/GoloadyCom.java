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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GoloadyCom extends antiDDoSForHost {
    public GoloadyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.goloady.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.goloady.com/help/privacy";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "goloady.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9\\-_]+)(?:/[^/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    /* 2019-08-13: Account untested, set FREE limits */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean ACCOUNT_FREE_RESUME          = false;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*?The file you are trying to download is no longer available|>\\s*?This could be due to the following reasons>\\s*?The file has been removed because of")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("class=\"downloadfileinfo[^\"]*?\">([^<>\"]+) \\((\\d+(?:\\.\\d{1,2})? [A-Za-z]{1,5})\\)</div>");
        String filename = finfo.getMatch(0);
        final String filename_url = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/[^/]+/(.+)$").getMatch(0);
        if (filename == null) {
            filename = filename_url;
        }
        if (filename == null) {
            filename = this.getFID(link);
        }
        String filesize = finfo.getMatch(1);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPrivateContent() {
        return br.containsHTML(">\\s*?Content you have requested is Private");
    }

    private boolean isPasswordProtectedContent() {
        return br.containsHTML("class=\"passwordLockedFile\"");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        // String dllink = null;
        /* 2020-04-07: Password protected content will also set this to false but password protected content is very rare! */
        boolean directDownloadEnabled = true;
        if (dllink == null) {
            /* 2019-08-13: E.g. premium download or free account download of self-uploaded files */
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                /* 2020-04-07: E.g. premium account with disabled direct download */
                dllink = br.getRegex("(/download\\.php[^<>\"\\']+)").getMatch(0);
                directDownloadEnabled = false;
            }
            if (dllink == null) {
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                if (isPasswordProtectedContent()) {
                    /*
                     * 2020-04-27: Serverside broken e.g. https://www.goloady.com/file/UKwbfe5PPn38TIDq-ygq9g/1mb.test --> Password = 123456
                     */
                    final boolean pw_protected_is_serverside_broken = true;
                    if (pw_protected_is_serverside_broken) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected URLs are serverside broken: Contact goloady support");
                    }
                    final String owner = br.getRegex("own=\"(\\d+)\"").getMatch(0);
                    final String fileID_internal = br.getRegex("file=\"(\\d+)\"").getMatch(0);
                    if (owner == null || fileID_internal == null) {
                        logger.warning("Password handling failed: owner = " + owner + " | fileID_internal = " + fileID_internal);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    String passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
                    }
                    postPage("/api/0/verifyfiledownloadpasscode?useraccess=&access_token=" + "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94", "owner=" + owner + "&file=" + fileID_internal + "&pass_code=" + Encoding.urlEncode(passCode));
                    final String result = PluginJSonUtils.getJson(br, "result");
                    if (StringUtils.isEmpty(result) || result.equalsIgnoreCase("failed")) {
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
                    }
                    /* Store valid downloadpassword for previous download-attempts */
                    link.setDownloadPassword(passCode);
                }
                String free_server = br.getRegex("freeaccess=\"([^\"]+)\"").getMatch(0);
                final String freetoken = br.getRegex("freetoken=\"([^\"]+)\"").getMatch(0);
                if (freetoken == null || free_server == null) {
                    handleErrors();
                    if (account != null && account.getType() == AccountType.PREMIUM) {
                        errorEnableDirectDownload();
                    }
                    /* This token is used for all captcha attempts for current downloadlink! */
                    logger.warning("freetoken = " + freetoken + " | free_server = " + free_server);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* 2019-08-13: Captcha is skippable */
                final boolean captchaSkippable = true;
                if (!captchaSkippable) {
                    boolean success = false;
                    for (int i = 0; i <= 4; i++) {
                        final long timeBefore = System.currentTimeMillis();
                        final String code = this.getCaptchaCode("/captcha/php/captcha.php", link);
                        if (i == 0) {
                            /* Wait only for first attempt */
                            waitTime(link, timeBefore);
                        }
                        postPage("/captcha/php/check_captcha.php", "captcha_code=" + Encoding.urlEncode(code) + "&token=" + freetoken);
                        if (br.toString().trim().equalsIgnoreCase("not_match")) {
                            continue;
                        }
                        /* Success = response equals "code_match" (without "") */
                        success = true;
                        break;
                    }
                    if (!success) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                if (Encoding.isUrlCoded(free_server)) {
                    free_server = Encoding.urlDecode(free_server, true);
                }
                dllink = free_server + "download.php?accesstoken=" + freetoken;
            }
            if (dllink == null) {
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (!directDownloadEnabled && account != null && account.getType() == AccountType.PREMIUM) {
                errorEnableDirectDownload();
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    /** 2020-04-07: With this disabled, downloads will fail quite often thus users should enable direct downloads! */
    private void errorEnableDirectDownload() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Enable 'Direct Download' in account settings");
    }

    protected void waitTime(final DownloadLink downloadLink, final long timeBefore) throws PluginException {
        /* 2019-08-13: Default waittime: 45 seconds */
        /* Ticket Time */
        /* 2019-08-13: Waittime is skippable */
        final boolean waitSkippable = true;
        final String waitStr = br.getRegex("id=\"freetimer\"[^>]*?>(\\d+)<").getMatch(0);
        if (waitSkippable) {
            /* Very rare case! */
            logger.info("Skipping pre-download waittime: " + waitStr);
        } else {
            final int extraWaitSeconds = 1;
            int wait;
            if (waitStr != null && waitStr.matches("\\d+")) {
                int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
                logger.info("Found waittime, parsing waittime: " + waitStr);
                wait = Integer.parseInt(waitStr);
                /*
                 * Check how much time has passed during eventual captcha event before this function has been called and see how much time
                 * is left to wait.
                 */
                wait -= passedTime;
                if (passedTime > 0) {
                    /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                    logger.info("Total passed time during captcha: " + passedTime);
                }
            } else {
                /* No waittime at all */
                wait = 0;
            }
            if (wait > 0) {
                logger.info("Waiting final waittime: " + wait);
                sleep(wait * 1000l, downloadLink);
            } else if (wait < -extraWaitSeconds) {
                /* User needed more time to solve the captcha so there is no waittime left :) */
                logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
            } else {
                /* No waittime at all */
                logger.info("Found no waittime");
            }
        }
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML(">\\s*?This link only for premium user")) {
            /* 2019-08-13: It seems like basically all files are premiumonly(?) */
            throw new AccountRequiredException();
        } else if (isPrivateContent()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private content, only downloadable by owner");
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    getPage("https://www." + this.getHost() + "/me");
                    /* 2020-04-07: Seems like their cookies are only valid for a very short time */
                    if (this.isLoggedin()) {
                        logger.info("Successfully loggedin via cookies");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Failed to login via cookies");
                    }
                }
                logger.info("Performing full login");
                getPage("https://www." + this.getHost() + "/user/login");
                final String js = br.getRegex("(/java/mycloud\\.js\\?\\d+)").getMatch(0);
                if (js == null) {
                    logger.warning("Failed to find js");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Browser brc = br.cloneBrowser();
                getPage(brc, js);
                final String access_token = brc.getRegex("app\\s*:\\s*'([a-z0-9]+)'").getMatch(0);
                if (access_token == null) {
                    logger.warning("Failed to find access_token");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // if (br.containsHTML("")) {
                // final DownloadLink dlinkbefore = this.getDownloadLink();
                // final DownloadLink dl_dummy;
                // if (dlinkbefore != null) {
                // dl_dummy = dlinkbefore;
                // } else {
                // dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                // this.setDownloadLink(dl_dummy);
                // }
                // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                // if (dlinkbefore != null) {
                // this.setDownloadLink(dlinkbefore);
                // }
                // // g-recaptcha-response
                // }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getHeaders().put("origin", "https://www." + this.getHost());
                br.getHeaders().put("referer", "https://www." + this.getHost() + "/user/login");
                br.getHeaders().put("sec-fetch-dest", "empty");
                br.getHeaders().put("sec-fetch-mode", "cors");
                br.getHeaders().put("sec-fetch-site", "same-origin");
                final UrlQuery query = new UrlQuery();
                query.append("keep", "1", false);
                query.append("email", account.getUser(), true);
                query.append("password", account.getPass(), true);
                postPage("/api/0/signmein?useraccess=&access_token=" + access_token, query.toString());
                final String result = PluginJSonUtils.getJson(br, "result");
                String logincookie = PluginJSonUtils.getJson(br, "doz");
                if ("error".equalsIgnoreCase(result) || StringUtils.isEmpty(logincookie)) {
                    /* 2019-08-13 e.g. {"result":"error","message":"This Mail-server is Banned."} */
                    final String errormsg = PluginJSonUtils.getJson(br, "message");
                    if (!StringUtils.isEmpty(errormsg)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, errormsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* 2020-04-07: This string is already urlencoded but needs to be urlencoded twice! */
                logincookie = Encoding.urlEncode(logincookie);
                br.setCookie(br.getURL(), "userdata", logincookie);
                getPage("/me");
                /* Double-check */
                if (!this.isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.containsHTML("/user/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/me")) {
            getPage("/me");
        }
        final Regex trafficRegex = br.getRegex(">Used Bandwidth</div>\\s*?<div class =\\s*?\"usedspace_percentage\"[^>]*?>([^<>\"]+) / ([^<>\"]+)</div>");
        final String traffic_usedStr = trafficRegex.getMatch(0);
        final String traffic_maxStr = trafficRegex.getMatch(1);
        final String expireStr = br.getRegex(">\\s*Expires on (\\d{4}-\\d{1,2}-\\d{1,2})").getMatch(0);
        long expireTimestamp = 0;
        if (expireStr != null) {
            expireTimestamp = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd", Locale.ENGLISH);
        }
        if (expireTimestamp < System.currentTimeMillis()) {
            /* Free & expired premium */
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            /* Premium */
            ai.setValidUntil(expireTimestamp, br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        long traffic_left = 0;
        if (traffic_usedStr != null && traffic_maxStr != null) {
            long traffic_used = SizeFormatter.getSize(traffic_usedStr);
            if (traffic_used == -1) {
                /* E.g. website displays "--- / 35GB" */
                traffic_used = 0;
            }
            final long traffic_max = SizeFormatter.getSize(traffic_maxStr);
            traffic_left = traffic_max - traffic_used;
        }
        if (traffic_left > 0) {
            ai.setTrafficLeft(traffic_left);
        } else {
            logger.info("Failed to find trafficleft");
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2019-08-13: Important: Do NOT login before availablecheck!! */
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getPluginPatternMatcher());
        if (account.getType() == AccountType.FREE) {
            handleDownload(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleDownload(link, account, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "premium_directlink");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}