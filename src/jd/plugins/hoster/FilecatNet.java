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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filecat.net" }, urls = { "https?://(?:www\\.)?filecat\\.net/f/([A-Za-z0-9_\\-]+)" })
public class FilecatNet extends PluginForHost {
    public FilecatNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filecat.net/pricing");
    }

    @Override
    public String getAGBLink() {
        return "https://filecat.net/";
    }

    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = 0;
    private final int           FREE_MAXDOWNLOADS            = 1;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final boolean       ACCOUNT_PREMIUM_RESUME       = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private static final String WEBSITE_API_BASE             = "https://api.filecat.net";
    private final String        PROPERTY_PREMIUMONLY         = "premiumonly";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private Browser prepBRWebsite(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        prepBRWebsite(this.br);
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(new int[] { 400 });
        br.getPage(WEBSITE_API_BASE + "/file/" + fid);
        if (this.br.getHttpConnection().getResponseCode() == 400 || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String filename = (String) entries.get("name");
        final Number filesize = (Number) entries.get("size");
        if (!StringUtils.isEmpty(filename)) {
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        if (Boolean.TRUE.equals(entries.get("premonly"))) {
            link.setProperty(PROPERTY_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_PREMIUMONLY);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    /** For free- and account modes! */
    private void handleDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final String fid = this.getFID(link);
            if (link.getBooleanProperty(PROPERTY_PREMIUMONLY, false) && (account == null || account.getType() != AccountType.PREMIUM)) {
                throw new AccountRequiredException();
            }
            br.getHeaders().put("Accept", "application/json, text/plain, */*");
            br.getHeaders().put("X-URL", link.getPluginPatternMatcher());
            br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
            /* Important! This sets the crucial "PHPSESSID" cookie!! */
            initSessionWebsite(this.br);
            final PostRequest preWaitReq = br.createJSonPostRequest("/dwnldreq", "{\"id\":null,\"file_uid\":\"" + fid + "\",\"captcha_token\":null}");
            br.openRequestConnection(preWaitReq);
            br.loadConnection(null);
            Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String reject_reason = (String) entries.get("reject_reason");
            if (reject_reason != null) {
                // final String reject_msg = PluginJSonUtils.getJson(br, "reject_msg");
                if (reject_reason.equalsIgnoreCase("ip_daily_downloads_limit")) {
                    /* No exact waittime given */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
                }
            }
            final String id = entries.get("id").toString();
            final Number wait_sec = (Number) entries.get("wait_sec");
            final long timestampBeforeCaptcha = Time.systemIndependentCurrentJVMTimeMillis();
            if (Boolean.TRUE.equals(entries.get("captcha_needed"))) {
                /* 2019-07-08: Hardcoded reCaptchaV2 key */
                final String rcKey = "6LfFS28UAAAAAIaK3SXWYWZ_iPK-zfOr-NmZaY0f";
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcKey);
                final String recaptchaV2Response = rc2.getToken();
                waitTime(link, timestampBeforeCaptcha, wait_sec.intValue());
                final PostRequest afterCaptchaReq = br.createJSonPostRequest("/dwnldreq", "{\"id\":" + id + ",\"file_uid\":\"" + fid + "\",\"captcha_token\":\"" + recaptchaV2Response + "\"}");
                br.openRequestConnection(afterCaptchaReq);
                br.loadConnection(null);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            } else {
                /* No captcha, only wait */
                waitTime(link, timestampBeforeCaptcha, wait_sec.intValue());
            }
            dllink = (String) entries.get("link");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dllink.startsWith("http")) {
                dllink = br._getURL().getProtocol() + "://" + dllink;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void initSessionWebsite(final Browser br) throws IOException {
        br.getPage(WEBSITE_API_BASE + "/app");
    }

    private boolean preDownloadWaittimeSkippable() {
        return false;
    }

    /**
     * Handles pre download (pre-captcha) waittime. If WAITFORCED it ensures to always wait long enough even if the waittime RegEx fails.
     */
    protected void waitTime(final DownloadLink link, final long timeBefore, int waitSeconds) throws PluginException {
        /* Ticket Time */
        if (this.preDownloadWaittimeSkippable()) {
            /* Very rare case! */
            logger.info("Skipping pre-download waittime: " + waitSeconds);
        } else {
            final int extraWaitSeconds = 1;
            int passedTime = (int) ((Time.systemIndependentCurrentJVMTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
            /*
             * Check how much time has passed during eventual captcha event before this function has been called and see how much time is
             * left to wait.
             */
            waitSeconds -= passedTime;
            if (passedTime > 0) {
                /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                logger.info("Total passed time during captcha: " + passedTime);
            }
            if (waitSeconds > 0) {
                logger.info("Waiting final waittime: " + waitSeconds);
                sleep(waitSeconds * 1000l, link);
            } else if (waitSeconds < -extraWaitSeconds) {
                /* User needed more time to solve the captcha so there is no waittime left :) */
                logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
            } else {
                /* No waittime at all */
                logger.info("Found no waittime");
            }
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
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
        return FREE_MAXDOWNLOADS;
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                prepBRWebsite(this.br);
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean loggedInViaCookies = false;
                final Cookies cookies = account.loadCookies("");
                Map<String, Object> entries = null;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return null;
                    }
                    br.getPage(WEBSITE_API_BASE + "/user/get");
                    if (isLoggedinCookies(br)) {
                        loggedInViaCookies = true;
                        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                        logger.info("Cookie login successful");
                    } else {
                        loggedInViaCookies = false;
                        logger.info("Cookie login failed");
                    }
                }
                if (!loggedInViaCookies) {
                    /* Full login */
                    initSessionWebsite(this.br);
                    final PostRequest preWaitReq = br.createJSonPostRequest("/user/signin", "{\"email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\"}");
                    br.openRequestConnection(preWaitReq);
                    br.loadConnection(null);
                    br.getPage(WEBSITE_API_BASE + "/user/get");
                    if (!this.isLoggedinCookies(br)) {
                        /* Not logged in -> Their WebAPI will not return json! */
                        throw new AccountInvalidException();
                    }
                    entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final String email = entries != null ? (String) entries.get("email") : null;
                    if (!StringUtils.equalsIgnoreCase(email, account.getUser())) {
                        final String message = entries != null ? (String) entries.get("message") : null;
                        throw new AccountInvalidException(message);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                return entries;
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedinCookies(final Browser br) {
        return br.getCookie(this.getHost(), "SESS", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> entries = this.login(account, true);
        // final String privileged = PluginJSonUtils.getJson(br, "privileged");
        // final String directdownloads = PluginJSonUtils.getJson(br, "directdownloads");
        if (Boolean.TRUE.equals(entries.get("premium"))) {
            final String expire = (String) entries.get("premiumby");
            if (!StringUtils.isEmpty(expire)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        } else {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
        }
        /* 2019-07-10: No known traffic limits so far */
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
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