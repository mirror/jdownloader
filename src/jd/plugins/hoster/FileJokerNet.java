//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FileJokerNet extends XFileSharingProBasic {
    public FileJokerNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        this.setConfigElements();
        /* 2019-08-20: Without this, users might experience 503 errors via API and captchas via website */
        this.setStartIntervall(2000l);
    }

    private static final boolean use_special_zeus_cloud_api = true;
    private static final String  PROPERTY_SESSIONID         = "zeus_cloud_sessionid";
    private static final String  PROPERTY_EMAIL             = "email";

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        super.prepBrowser(prepBr, host);
        final String custom_referer = this.getPluginConfig().getStringProperty("CUSTOM_REFERER", null);
        if (!StringUtils.isEmpty(custom_referer)) {
            prepBr.getHeaders().put("Referer", custom_referer);
        }
        return prepBr;
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null 4dignum solvemedia reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filejoker.net" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-08-15: Special */
        final Regex finfo = new Regex(correctedBR, "class=\"name-size\">([^<>\"]+)<small>\\(([^<>\"]+)\\)</small>");
        fileInfo[0] = finfo.getMatch(0);
        fileInfo[1] = finfo.getMatch(1);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            /* Fallback to template method */
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-08-15: Special */
        return false;
    }

    @Override
    protected String regexWaittime() {
        /* 2019-08-15: Special */
        String wait = new Regex(correctedBR, "class=\"alert\\-success\">(\\d+)</span>").getMatch(0);
        if (StringUtils.isEmpty(wait)) {
            wait = super.regexWaittime();
        }
        return wait;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-08-15: Special */
        super.checkErrors(link, account, checkAll);
        if (new Regex(correctedBR, "(You have reached the download(\\-| )limit|You have to wait|Wait .*? to download for free)").matches()) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            String wait = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(wait, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) {
                tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            }
            String tmpmin = new Regex(wait, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) {
                tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            }
            String tmpsec = new Regex(wait, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(wait, "\\s+(\\d+)\\s+days?").getMatch(0);
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
        if (correctedBR.contains("There is not enough traffic available to download this file")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic available", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (correctedBR.contains("You don't have permission to download this file")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You don't have permission to download this file", 30 * 60 * 1000l);
        }
    }

    @Override
    protected String getDllink(final DownloadLink downloadLink, final Account account, final Browser br, String src) {
        /* 2019-08-15: Special */
        String dllink = new Regex(src, "\"(https?://fs\\d+[^/]+/[^\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            /* Fallback to template handling */
            dllink = super.getDllink(downloadLink, account, br, src);
        }
        return dllink;
    }

    @Override
    protected String getRelativeAccountInfoURL() {
        /* 2019-08-20: Special */
        return "/profile";
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
    private void handleSecurityVerification() throws Exception {
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
                super.submitForm(securityVerification);
                br.setFollowRedirects(redirectSetting);
            }
        }
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
            if (!StringUtils.isEmpty(sessionid)) {
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                    /* We trust these cookies as they're not that old --> Do not check them */
                    logger.info("Trust login-sessionid without checking as it should still be fresh");
                    return false;
                }
                /* First check if old session is still valid */
                getPage(br, this.getMainPage() + "/zapi?op=my_account&session=" + sessionid);
                /* Check for e.g. "{"error":"invalid session"}" */
                loggedIN = StringUtils.isEmpty(PluginJSonUtils.getJson(br, "error"));
            }
            if (!loggedIN) {
                getPage(br, "https://" + account.getHoster() + "/zapi?op=login&email=" + account.getUser() + "&pass=" + account.getPass());
                final String error = PluginJSonUtils.getJson(br, "error");
                sessionid = PluginJSonUtils.getJson(br, "session");
                if (!StringUtils.isEmpty(error) || StringUtils.isEmpty(sessionid)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("zeus_cloud_sessionid", sessionid);
            }
        } finally {
            convertSpecialAPICookiesToWebsiteCookies(account, sessionid);
        }
        return true;
    }

    /** This is different from the official XFS "API-Mod" API!! */
    private final AccountInfo fetchAccountInfoAPIZeusCloudManager(final Browser br, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPIZeusCloudManager(br, account, true);
        final String sessionid = getAPIZeusCloudManagerSession(account);
        if (br.getURL() == null || !br.getURL().contains("/zapi?op=my_account&session=")) {
            getPage(br, this.getMainPage() + "/zapi?op=my_account&session=" + sessionid);
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
        /* 2019-05-30: TODO: Add support for lifetime accounts */
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
        convertSpecialAPICookiesToWebsiteCookies(account, sessionid);
        account.saveCookies(br.getCookies(br.getURL()), "");
        return ai;
    }

    private final void handlePremiumAPIZeusCloudManager(final DownloadLink link, final Account account) throws Exception {
        loginAPIZeusCloudManager(this.br, account, false);
        /* Important! Set fuid as we do not check availibility via website via requestFileInformationWebsite! */
        this.fuid = getFUIDFromURL(link);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            final String sessionid = this.getAPIZeusCloudManagerSession(account);
            this.getPage(this.getMainPage() + "/zapi?op=download1&session=" + sessionid + "&file_code=" + fuid);
            checkErrorsAPIZeusCloudManager(link, account);
            final String download_id = PluginJSonUtils.getJson(br, "download_id");
            if (StringUtils.isEmpty(download_id)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "download_id is missing");
            }
            this.getPage(this.getMainPage() + "/zapi?op=download2&session=" + sessionid + "&file_code=" + fuid + "&download_id=" + download_id);
            checkErrorsAPIZeusCloudManager(link, account);
            dllink = PluginJSonUtils.getJson(br, "direct_link");
        }
        this.handleDownload(link, account, dllink, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /*
         * 2019-08-20: Download via free accounts via API does not work so let's only allow API downloads for premium users | Trying to
         * download via API via Free Account will only return a waittime:
         * {"file_size":"200000000","file_name":"200Mo.dat","file_code":"dummymidummy",
         * "message":"Wait 5 hours 59 minutes 25 seconds to download for free."}
         */
        // if (use_special_zeus_cloud_api && AccountType.PREMIUM.equals(account.getType())) {
        /* 2019-08-20: TODO: Verify this! */
        if (use_special_zeus_cloud_api) {
            handlePremiumAPIZeusCloudManager(link, account);
        } else {
            super.handlePremium(link, account);
        }
    }

    /**
     * Converts API values to normal website cookies so we can easily switch to website mode or login via API and then use website later OR
     * download via website right away.
     */
    private final void convertSpecialAPICookiesToWebsiteCookies(final Account account, final String sessionid) {
        this.br.setCookie(account.getHoster(), "xfss", sessionid);
        final String email = account.getStringProperty(PROPERTY_EMAIL, null);
        if (!StringUtils.isEmpty(email)) {
            account.setProperty(PROPERTY_EMAIL, email);
            this.br.setCookie(br.getHost(), "email", email);
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        if (use_special_zeus_cloud_api) {
            return fetchAccountInfoAPIZeusCloudManager(this.br, account);
        } else {
            return super.fetchAccountInfoWebsite(account);
        }
    }

    @Override
    public boolean loginWebsite(final Account account, final boolean force) throws Exception {
        if (use_special_zeus_cloud_api) {
            return loginAPIZeusCloudManager(this.br, account, force);
        } else {
            return super.loginWebsite(account, force);
        }
    }

    private final void checkErrorsAPIZeusCloudManager(final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        final String error = PluginJSonUtils.getJson(br, "error");
        final String message = PluginJSonUtils.getJson(br, "message");
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
            } else {
                /* 2019-08-20: TODO: Add support for more errorcases */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error");
            }
        } else if (!StringUtils.isEmpty(message)) {
            if (new Regex(message, "(You have reached the download(\\-| )limit|You have to wait|Wait .*? to download for free)").matches()) {
                /* adjust this regex to catch the wait time string for COOKIE_HOST */
                final String wait = message;
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
        }
        checkResponseCodeErrors(br.getHttpConnection());
    }

    private final String getAPIZeusCloudManagerSession(final Account account) {
        return account.getStringProperty(PROPERTY_SESSIONID, null);
    }

    private final void invalidateAPIZeusCloudManagerSession(final Account account) throws PluginException {
        account.removeProperty("zeus_cloud_sessionid");
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid sessionid", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
    }

    @Override
    protected boolean allows_multiple_login_attempts_in_one_go() {
        /* 2019-08-20: Special */
        return true;
    }

    private void setConfigElements() {
        /* 2019-08-20: This host grants (free-)users with special Referer values better downloadspeeds. */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), "CUSTOM_REFERER", "Set custom Referer here").setDefaultValue(null));
    }
}