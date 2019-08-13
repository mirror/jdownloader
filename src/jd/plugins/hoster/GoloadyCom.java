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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GoloadyCom extends PluginForHost {
    public GoloadyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
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

    /** 2019-08-12: Main domain was changed from speed4up.com to speed4up.net */
    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
    private final boolean ACCOUNT_PREMIUM_RESUME       = false;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        // String dllink = null;
        if (dllink == null) {
            /* 2019-08-13: E.g. premium download or free account download of self-uploaded files */
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                if (isPasswordProtectedContent()) {
                    /* TODO: 2019-08-13: Finish & test this */
                    if (true) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected URLs are not yet supported for this host");
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
                    br.postPage("/api/0/verifyfiledownloadpasscode?useraccess=&access_token=" + "TODO_FIXME", "owner=" + owner + "&file=" + fileID_internal + "&pass_code=" + Encoding.urlEncode(passCode));
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
                        this.br.postPage("/captcha/php/check_captcha.php", "captcha_code=" + Encoding.urlEncode(code) + "&token=" + freetoken);
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
                boolean isLoggedin = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + this.getHost() + "/");
                    isLoggedin = this.isLoggedin();
                }
                if (!isLoggedin) {
                    if (true) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login-support-development is not yet finished", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.getPage("https://www." + this.getHost() + "/user/login");
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
                    br.postPage("/api/0/signmein?useraccess=&access_token=" + "TODO_FIXME", "keep=1&mail=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    final String result = PluginJSonUtils.getJson(br, "result");
                    if (!isLoggedin() || "error".equalsIgnoreCase(result)) {
                        /* 2019-08-13 e.g. {"result":"error","message":"This Mail-server is Banned."} */
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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
        return br.getCookie(this.getHost(), "userdata", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        if (br.containsHTML("")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2019-08-13: Important: Do NOT login before availablecheck!! */
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getPluginPatternMatcher());
        if (account.getType() == AccountType.FREE) {
            handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleDownload(link, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "premium_directlink");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
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