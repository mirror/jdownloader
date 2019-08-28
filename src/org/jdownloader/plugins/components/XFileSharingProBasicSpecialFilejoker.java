package org.jdownloader.plugins.components;

import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XFileSharingProBasicSpecialFilejoker extends XFileSharingProBasic {
    public XFileSharingProBasicSpecialFilejoker(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XFileSharingProBasicSpecialFilejoker Version SEE SUPER-CLASS<br />
     * mods: Special XFS derivate which supports a special API used by some hosts. <br />
     */
    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, final String src) {
        /* 2019-08-21: Special for novafile.com & filejoker.net */
        String dllink = new Regex(src, "\"(https?://f?s\\d+[^/]+/[^\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            dllink = new Regex(src, "href=\"(https?://[^<>\"]+)\" class=\"btn btn[^\"]*\"").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = new Regex(src, "\"(https?://[^/]+/[a-z0-9]{50,100}/[^\"]+)\"").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            /* Fallback to template handling */
            dllink = super.getDllink(link, account, br, src);
        }
        if (account != null && !StringUtils.isEmpty(dllink)) {
            /* Set timestamp of last download on account - this might be useful later for our Free-Account handling */
            account.setProperty("lastdownload_timestamp_website", System.currentTimeMillis());
        }
        return dllink;
    }

    @Override
    public Form findFormF1Premium() throws Exception {
        /* 2019-08-20: Special */
        handleSecurityVerification();
        return super.findFormF1Premium();
    }

    @Override
    public Form findFormDownload1() throws Exception {
        /* 2019-08-20: Special */
        handleSecurityVerification();
        return super.findFormDownload1();
    }

    @Override
    protected void getPage(String page) throws Exception {
        /* 2019-08-20: Special */
        super.getPage(page);
        handleSecurityVerification();
    }

    /* 2019-08-20: Special */
    private final void handleSecurityVerification() throws Exception {
        if (br.getURL() != null && br.getURL().contains("op=captcha&id=")) {
            /*
             * 2019-01-23: Special - this may also happen in premium mode! This will only happen when accessing downloadurl. It gets e.g.
             * triggered when accessing a lot of different downloadurls in a small timeframe.
             */
            /* Tags: XFS_IP_CHECK /ip_check/ */
            Form securityVerification = br.getFormbyProperty("name", "F1");
            if (securityVerification == null) {
                securityVerification = br.getFormbyProperty("id", "f1");
            }
            if (securityVerification != null && securityVerification.containsHTML("data-sitekey")) {
                logger.info("Handling securityVerification");
                final boolean redirectSetting = br.isFollowingRedirects();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                securityVerification.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.setFollowRedirects(true);
                try {
                    super.submitForm(securityVerification);
                } finally {
                    br.setFollowRedirects(redirectSetting);
                }
            }
        }
    }

    @Override
    protected boolean allows_multiple_login_attempts_in_one_go() {
        return true;
    }

    /* *************************** SPECIAL API STUFF STARTS HERE *************************** */
    private static final String PROPERTY_SESSIONID = "zeus_cloud_sessionid";
    private static final String PROPERTY_EMAIL     = "email";

    /**
     * Turns on/off special API for (Free-)Account Login & Download. Keep this activated whenever possible as it will solve a lot of
     * issues/complicated handling which is required for website login and download! </br>
     * Sidenote: API Cookies will work fine for the website too so if enabled- and later disabled, login-captchas should still be avoided!
     */
    protected boolean useAPIZeusCloudManager() {
        return true;
    }

    /** Override this depending on host */
    protected String getRelativeAPIBaseAPIZeusCloudManager() {
        return null;
    }

    /** Override this depending on host (2019-08-21: Some use "login", some use "email" [login via email:password or username:password]) */
    protected String getRelativeAPILoginParamsFormatAPIZeusCloudManager() {
        return null;
    }

    /** 2019-08-20: API will also work fine with different User-Agent values. */
    private final Browser prepAPIZeusCloudManager(final Browser br) {
        br.getHeaders().put("User-Agent", "okhttp/3.8.0");
        return br;
    }

    private final boolean loginAPIZeusCloudManager(final Browser br, final Account account, final boolean force) throws Exception {
        prepAPIZeusCloudManager(br);
        boolean loggedIN = false;
        String sessionid = getAPIZeusCloudManagerSession(account);
        try {
            String error = null;
            if (!StringUtils.isEmpty(sessionid)) {
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                    /* We trust these cookies as they're not that old --> Do not check them */
                    logger.info("Trust login-sessionid without checking as it should still be fresh");
                    return false;
                }
                /* First check if old session is still valid */
                getPage(br, this.getMainPage() + getRelativeAPIBaseAPIZeusCloudManager() + "?op=my_account&session=" + sessionid);
                error = PluginJSonUtils.getJson(br, "error");
                /* Check for e.g. "{"error":"invalid session"}" */
                /*
                 * 2019-08-28: Errors may happen at this stage but we only want to perform a full login if we're absolutely sure that our
                 * current sessionID is invalid!
                 */
                if (!"invalid session".equalsIgnoreCase(error)) {
                    loggedIN = true;
                    this.checkErrorsAPIZeusCloudManager(null, account);
                }
            }
            if (!loggedIN) {
                getPage(br, this.getMainPage() + getRelativeAPIBaseAPIZeusCloudManager() + String.format(getRelativeAPILoginParamsFormatAPIZeusCloudManager(), Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass())));
                sessionid = PluginJSonUtils.getJson(br, "session");
                error = PluginJSonUtils.getJson(br, "error");
                this.checkErrorsAPIZeusCloudManager(null, account);
                if (StringUtils.isEmpty(sessionid)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("zeus_cloud_sessionid", sessionid);
            }
        } finally {
            convertSpecialAPICookiesToWebsiteCookiesAndSaveThem(account, sessionid);
        }
        return true;
    }

    /** This is different from the official XFS "API-Mod" API!! */
    private final AccountInfo fetchAccountInfoAPIZeusCloudManager(final Browser br, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPIZeusCloudManager(br, account, true);
        final String sessionid = getAPIZeusCloudManagerSession(account);
        if (br.getURL() == null || !br.getURL().contains(getRelativeAPIBaseAPIZeusCloudManager() + "?op=my_account&session=")) {
            getPage(br, this.getMainPage() + getRelativeAPIBaseAPIZeusCloudManager() + "?op=my_account&session=" + sessionid);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        /** 2019-08-20: Better compare expire-date against their serverside time if possible! */
        final String server_timeStr = (String) entries.get("server_time");
        long expire_milliseconds_precise_to_the_second = 0;
        final String email = (String) entries.get("usr_email");
        final long currentTime;
        if (server_timeStr != null && server_timeStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            currentTime = TimeFormatter.getMilliSeconds(server_timeStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else {
            currentTime = System.currentTimeMillis();
        }
        final long trafficleft = JavaScriptEngineFactory.toLong(entries.get("traffic_left"), 0);
        final String expireStr = (String) entries.get("usr_premium_expire");
        if (expireStr != null && expireStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            expire_milliseconds_precise_to_the_second = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        ai.setTrafficLeft(trafficleft * 1024 * 1024);
        if (expire_milliseconds_precise_to_the_second <= currentTime) {
            if (expire_milliseconds_precise_to_the_second > 0) {
                /*
                 * 2019-07-31: Most likely this logger will always get triggered because they will usually set the register date of new free
                 * accounts into "premium_expire".
                 */
                logger.info("Premium expired --> Free account");
            }
            /* Expired premium or no expire date given --> It is usually a Free Account */
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Expire date is in the future --> It is a premium account */
            ai.setValidUntil(expire_milliseconds_precise_to_the_second);
            setAccountLimitsByType(account, AccountType.PREMIUM);
        }
        if (email != null) {
            account.setProperty(PROPERTY_EMAIL, email);
        }
        convertSpecialAPICookiesToWebsiteCookiesAndSaveThem(account, sessionid);
        return ai;
    }

    private final void handlePremiumAPIZeusCloudManager(final DownloadLink link, final Account account) throws Exception {
        loginAPIZeusCloudManager(this.br, account, false);
        /* Important! Set fuid as we do not check availibility via website via requestFileInformationWebsite! */
        this.fuid = getFUIDFromURL(link);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String dllink = checkDirectLinkAPIZeusCloudManager(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            final String sessionid = this.getAPIZeusCloudManagerSession(account);
            this.getPage(this.getMainPage() + getRelativeAPIBaseAPIZeusCloudManager() + "?op=download1&session=" + sessionid + "&file_code=" + fuid);
            /*
             * 2019-08-21: Example response (free account download):
             * {"download_id":"xxxxxxxx","file_size":"200000000","file_name":"test.dat","file_code":"xxxxxxxxxxxx","countdown":"90"}
             */
            checkErrorsAPIZeusCloudManager(link, account);
            final String download_id = PluginJSonUtils.getJson(br, "download_id");
            final String waittimeSecondsStr = PluginJSonUtils.getJson(br, "countdown");
            if (StringUtils.isEmpty(download_id)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "download_id is missing");
            }
            if (!StringUtils.isEmpty(waittimeSecondsStr) && waittimeSecondsStr.matches("\\d+")) {
                /* E.g. free account download will usually have a waittime of 90 seconds before download can be started. */
                final int waittimeSeconds = Integer.parseInt(waittimeSecondsStr);
                this.sleep(waittimeSeconds * 1001l, link);
            }
            this.getPage(this.getMainPage() + getRelativeAPIBaseAPIZeusCloudManager() + "?op=download2&session=" + sessionid + "&file_code=" + fuid + "&download_id=" + download_id);
            /*
             * 2019-08-21: Example response (free account download):
             * {"file_size":"200000000","file_name":"test.dat","file_code":"xxxxxxxxxxxx",
             * "message":"Wait 5 hours 12 minutes 46 seconds to download for free."}
             */
            checkErrorsAPIZeusCloudManager(link, account);
            dllink = PluginJSonUtils.getJson(br, "direct_link");
            if (!StringUtils.isEmpty(dllink)) {
                /* Set timestamp of last download on account - this might be useful later for our Free-Account handling */
                account.setProperty("lastdownload_timestamp_api", System.currentTimeMillis());
            }
        }
        this.handleDownload(link, account, dllink, null);
    }

    protected final String checkDirectLinkAPIZeusCloudManager(final DownloadLink link, final String property) {
        /* 2019-08-28: Special */
        final String directurl = link.getStringProperty(property, null);
        if (StringUtils.isEmpty(directurl) || !directurl.startsWith("http")) {
            return null;
        } else {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(directurl));
                /* For video streams we often don't get a Content-Disposition header. */
                // final boolean isFile = con.isContentDisposition() || StringUtils.containsIgnoreCase(con.getContentType(), "video") ||
                // StringUtils.containsIgnoreCase(con.getContentType(), "audio") || StringUtils.containsIgnoreCase(con.getContentType(),
                // "application");
                /*
                 * 2019-08-28: contentDisposition is always there plus invalid URLs might have: Content-Type: application/octet-stream -->
                 * 'application' as Content-Type is no longer an indication that we can expect a file!
                 */
                final boolean isFile = con.isContentDisposition() || StringUtils.containsIgnoreCase(con.getContentType(), "video") || StringUtils.containsIgnoreCase(con.getContentType(), "audio");
                if (con.getResponseCode() == 503) {
                    /* Ok */
                    /*
                     * Too many connections but that does not mean that our directlink is invalid. Accept it and if it still returns 503 on
                     * download-attempt this error will get displayed to the user - such directlinks should work again once there are less
                     * active connections to the host!
                     */
                    logger.info("directurl lead to 503 | too many connections");
                    return directurl;
                } else if (!con.getContentType().contains("html") && con.getLongContentLength() > -1 && con.isOK() && isFile) {
                    return directurl;
                } else {
                    /* Failure */
                }
            } catch (final Exception e) {
                /* Failure */
                logger.log(e);
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (useAPIZeusCloudManager()) {
            handlePremiumAPIZeusCloudManager(link, account);
        } else {
            super.handlePremium(link, account);
        }
    }

    /**
     * Converts API values to normal website cookies so we can easily switch to website mode or login via API and then use website later OR
     * download via website right away.
     */
    private final void convertSpecialAPICookiesToWebsiteCookiesAndSaveThem(final Account account, final String sessionid) {
        this.br.setCookie(account.getHoster(), "xfss", sessionid);
        final String email = account.getStringProperty(PROPERTY_EMAIL, null);
        if (!StringUtils.isEmpty(email)) {
            account.setProperty(PROPERTY_EMAIL, email);
            this.br.setCookie(br.getHost(), "email", email);
        }
        account.saveCookies(br.getCookies(account.getHoster()), "");
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        if (useAPIZeusCloudManager()) {
            return fetchAccountInfoAPIZeusCloudManager(this.br, account);
        } else {
            return super.fetchAccountInfoWebsite(account);
        }
    }

    @Override
    public boolean loginWebsite(final Account account, final boolean force) throws Exception {
        if (useAPIZeusCloudManager()) {
            return loginAPIZeusCloudManager(this.br, account, force);
        } else {
            return super.loginWebsite(account, force);
        }
    }

    private final void checkErrorsAPIZeusCloudManager(final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        final String error = PluginJSonUtils.getJson(br, "error");
        final String message = PluginJSonUtils.getJson(br, "message");
        /* 2019-08-21: Special: Waittime errormessage can be in "error" or in "message". */
        final String waittimeRegex = ".*(You have reached the download(\\-| )limit|You have to wait|Wait .*? to download for free|You will not be able to download for).*";
        String wait = !StringUtils.isEmpty(error) ? new Regex(error, waittimeRegex).getMatch(-1) : null;
        if (StringUtils.isEmpty(wait) && !StringUtils.isEmpty(message)) {
            wait = new Regex(message, waittimeRegex).getMatch(-1);
        }
        if (wait != null) {
            /*
             * 2019-08-21: Example free account download:
             * {"error":"You've tried to download from 2 different IPs in the last 3 hours. You will not be able to download for 3 hours." }
             */
            // String wait = new Regex(message, "(You will not be able to download for.+)").getMatch(0);
            // if (wait == null) {
            // wait = message;
            // }
            final String tmphrs = new Regex(wait, "\\s+(\\d+)\\s+hours?").getMatch(0);
            final String tmpmin = new Regex(wait, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            final String tmpsec = new Regex(wait, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            final String tmpdays = new Regex(wait, "\\s+(\\d+)\\s+days?").getMatch(0);
            int waittime;
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime RegExes seem to be broken - using default waittime");
                waittime = 60 * 60 * 1000;
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            }
            logger.info("Detected reconnect waittime (milliseconds): " + waittime);
            /* Not enough wait time to reconnect -> Wait short and retry */
            if (waittime < 180000) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
            } else if (account != null) {
                throw new AccountUnavailableException("Download limit reached", waittime);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (!StringUtils.isEmpty(error)) {
            if (error.equalsIgnoreCase("Login failed")) {
                /* This should only happen on login attempt via email/username & password */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (error.equalsIgnoreCase("invalid session")) {
                invalidateAPIZeusCloudManagerSession(account);
            } else if (error.equalsIgnoreCase("no file")) {
                /* 2019-08-20: E.g. Request download1 or download2 of offline file via API */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (error.equalsIgnoreCase("Download session expired")) {
                /* 2019-08-20: E.g. Request download2 with old/invalid 'download_id' value via API. This should usually never happen! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 1 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Skipped countdown")) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FATAL);
            } else if (error.equalsIgnoreCase("hack activity detected")) {
                /*
                 * 2019-08-23: filejoker.net - basically an IP-block - login is blocked until user tries with new IP. It should work again
                 * then! This can be triggered by messing with their website, frequently logging-in (creating new login-sessions) and trying
                 * again with new Cloudflare-cookies frequently.
                 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Try again with new IP: API error '" + error + "'", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                /* This should not happen. If it does, improve errorhandling! */
                logger.warning("Unknown API error");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown API error", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        } else if (!StringUtils.isEmpty(message)) {
            if (message.contains("This file can only be downloaded by Premium")) {
                /*
                 * 2019-08-21: novafile.com: E.g. {"file_size":"500000000","file_name":"test.dat","file_code":"xxxxxxxxxxxx", "message":
                 * "<strong>This file can only be downloaded by Premium Members 400 MB.<br>Become a <a href='#tariffs'>Premium Member</a> and download any files instantly at maximum speed!</strong>"
                 * }
                 */
                throw new AccountRequiredException();
            }
            logger.warning("Possibly unhandled API errormessage: " + message);
        }
        checkResponseCodeErrors(br.getHttpConnection());
    }

    private final String getAPIZeusCloudManagerSession(final Account account) {
        return account.getStringProperty(PROPERTY_SESSIONID, null);
    }

    private final void invalidateAPIZeusCloudManagerSession(final Account account) throws PluginException {
        account.removeProperty(PROPERTY_SESSIONID);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid sessionid", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        /* 2019-08-23: Debugtest */
        // link.removeProperty("freelink2");
        // link.removeProperty("premlink");
        // link.removeProperty("freelink");
    }
}