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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SendCm extends XFileSharingProBasic {
    public SendCm(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-11-06: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "send.cm", "sendit.cloud", "usersfiles.com" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("usersfiles.com");
        return deadDomains;
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
    public String rewriteHost(final String host) {
        /* 2022-07-27: sendit.cloud and usersfiles.com have been merged from another plugin into this one. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -10;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, Account account, final boolean isDownload) throws Exception {
        final Account accountToUseInSpecialHandling;
        if (account != null) {
            accountToUseInSpecialHandling = account;
        } else {
            /* Get ANY available account. */
            accountToUseInSpecialHandling = AccountController.getInstance().getValidAccount(this.getHost());
        }
        if (isFreeAccountWithPremiumTraffic(accountToUseInSpecialHandling)) {
            /* Special to avoid Cloudflare problems */
            this.loginWebsite(link, accountToUseInSpecialHandling, false);
            final String directurl = buildSpecialJDownloaderURL(link);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                // con = brc.openHeadConnection(directurl);
                con = brc.openGetConnection(directurl);
                if (!this.looksLikeDownloadableContent(con)) {
                    brc.followConnection(true);
                    this.checkErrors(brc, brc.getRequest().getHtmlCode(), link, accountToUseInSpecialHandling, false);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Special '/jd' handling failed");
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String filename = Plugin.getFileNameFromHeader(con);
                if (filename != null) {
                    link.setFinalFileName(filename);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            return AvailableStatus.TRUE;
        } else {
            final AvailableStatus status = super.requestFileInformationWebsite(link, account, isDownload);
            // 2023-04-12: MD5 seems to be base64 encoded but doesn't match, maybe just fake info?!*/
            final String sha256 = br.getRegex("(?i)SHA-256\\s*:\\s*</b>\\s*([a-f0-9]{64})\\s*</span>").getMatch(0);
            if (sha256 != null) {
                link.setSha256Hash(sha256);
            }
            return status;
        }
    }

    @Override
    protected void resolveShortURL(final Browser br, final DownloadLink link, final Account account) throws Exception {
        /* 2023-04-21: Special handling for when downloadlimit is reached during this handling -> Look for fuid in cookies. */
        synchronized (link) {
            try {
                super.resolveShortURL(br, link, account);
            } catch (final PluginException exc) {
                logger.info("Attempting special handling to find real FUID");
                final Cookies cookies = br.getCookies(br.getHost());
                final Set<String> realFUIDs = new HashSet<String>();
                for (final Cookie cookie : cookies.getCookies()) {
                    if (cookie.getKey() != null && cookie.getKey().matches("c_[A-Za-z0-9]+")) {
                        final String value = cookie.getValue();
                        if (value != null && value.matches("[A-Za-z0-9]{12}")) {
                            realFUIDs.add(value);
                        }
                    }
                }
                if (realFUIDs.size() != 1) {
                    /* No result or more than one -> Problem! */
                    logger.info("Failed to find real FUID");
                    throw exc;
                } else {
                    /* Success! */
                    final String realFUID = realFUIDs.iterator().next();
                    final String contentURL = this.getContentURL(link);
                    final String urlNew = URLHelper.parseLocation(new URL(this.getMainPage(link)), buildNormalURLPath(link, realFUID));
                    logger.info("resolve URL|old: " + contentURL + "|new:" + urlNew);
                    link.setPluginPatternMatcher(urlNew);
                }
            }
        }
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        if (isFreeAccountWithPremiumTraffic(account)) {
            /**
             * 2023-10-16: Special: For "Free accounts" with paid "Premium bandwidth". </br>
             * Looks like this is supposed to help with Cloudflare problems.
             */
            final String directurl;
            if (allowAPIDownloadIfApikeyIsAvailable(link, account)) {
                directurl = this.getDllinkAPI(link, account);
            } else {
                requestFileInformationWebsite(link, account, true);
                directurl = buildSpecialJDownloaderURL(link);
            }
            handleDownload(link, account, null, directurl, null);
        } else {
            super.doFree(link, account);
        }
    }

    private String buildSpecialJDownloaderURL(final DownloadLink link) {
        final String contentURL = this.getContentURL(link);
        final String specialURL = contentURL + "/jd";
        return specialURL;
    }

    private boolean isFreeAccountWithPremiumTraffic(final Account account) {
        if (account != null && account.getType() == AccountType.FREE && account.getAccountInfo() != null && account.getAccountInfo().getTrafficLeft() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean loginWebsite(final DownloadLink link, final Account account, final boolean validateCookies) throws Exception {
        try {
            return super.loginWebsite(link, account, validateCookies);
        } catch (final PluginException e) {
            Form twoFAForm = null;
            final String formKey2FA = "new_ip_token";
            final Form[] forms = br.getForms();
            for (final Form form : forms) {
                final InputField twoFAField = form.getInputField(formKey2FA);
                if (twoFAField != null) {
                    twoFAForm = form;
                    break;
                }
            }
            if (twoFAForm == null) {
                /* Login failed */
                throw e;
            }
            logger.info("2FA code required");
            final DownloadLink dl_dummy;
            if (this.getDownloadLink() != null) {
                dl_dummy = this.getDownloadLink();
            } else {
                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
            }
            String twoFACode = getUserInput("Enter verification code sent to your E-Mail", dl_dummy);
            if (twoFACode != null) {
                twoFACode = twoFACode.trim();
            }
            if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiges Format der 2-faktor-Authentifizierung!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2-factor-authentication code format!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            logger.info("Submitting 2FA code");
            twoFAForm.put(formKey2FA, twoFACode);
            this.submitForm(twoFAForm);
            if (!this.isLoggedin(br)) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger 2-faktor-Authentifizierungscode!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2-factor-authentication code!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    @Override
    public String getLoginURL() {
        return getMainPage() + "/?op=login";
    }

    @Override
    protected String regExTrafficLeft(final Browser br) {
        final String betterTrafficLeft = br.getRegex(">\\s*Premium Bandwidth.*?<h3[^>]*>(\\d+[^<]+)</h3>").getMatch(0);
        if (betterTrafficLeft != null) {
            return betterTrafficLeft;
        } else {
            return super.regExTrafficLeft(br);
        }
    }

    @Override
    public String[] scanInfo(final String html, String[] fileInfo) {
        fileInfo = super.scanInfo(html, fileInfo);
        String betterFilename = br.getRegex("class\\s*=\\s*\"modal-title\"\\s*id=\"qr\"[^>]*>\\s*([^<]*?)\\s*</h\\d+>").getMatch(0);
        if (betterFilename == null) {
            betterFilename = br.getRegex("data-feather\\s*=\\s*\"file\"[^>]*>\\s*</i>\\s*([^<]*?)\\s*</h\\d+>").getMatch(0);
            if (betterFilename == null) {
                betterFilename = br.getRegex("(?i)\\&text=([^\"]+)\" target=\"_blank\">\\s*Share on Telegram").getMatch(0);
            }
        }
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        final String betterFilesize = br.getRegex("(?i)id=\"downloadbtn[>]*><i [^>]*></i>\\s*Download \\[([^<\\]]+)\\]</button>").getMatch(0);
        if (betterFilesize != null) {
            fileInfo[1] = betterFilesize;
        }
        return fileInfo;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String correctedBR) {
        if (br.containsHTML("(?i)>\\s*The file you were looking for doesn")) {
            return true;
        } else {
            return super.isOffline(link, br, correctedBR);
        }
    }

    @Override
    public boolean isPremiumOnly(final Browser br) {
        if (br.containsHTML("(?i)>\\s*This file is available for")) {
            /* 2022-10-04 */
            return true;
        } else {
            return super.isPremiumOnly(br);
        }
    }

    @Override
    public boolean isPasswordProtectedHTML(final Browser br, final Form pwForm) {
        if (pwForm != null && pwForm.containsHTML("Enter File Password")) {
            return true;
        } else {
            return super.isPasswordProtectedHTML(br, pwForm);
        }
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        /* 2022-10-04 */
        return true;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /* 2021-09-29 */
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2023-05-28 */
        return false;
    }

    @Override
    protected boolean allowToGenerateAPIKeyInWebsiteMode() {
        /* 2022-10-05: Their API handling is broken serverside and will never return and API key sending the request to generate one. */
        return false;
    }

    @Override
    protected boolean supportsShortURLs() {
        return true;
    }

    @Override
    protected String regexAPIKey(final Browser br) {
        final String apikey = br.getRegex("(?i)<span class=\"input-group-text\"[^>]*>\\s*([a-z0-9]{20,})\\s*</span>").getMatch(0);
        if (apikey != null) {
            return apikey;
        } else {
            return super.regexAPIKey(br);
        }
    }

    private String getApikey(final Account account) {
        String apikey = this.getAPIKeyFromAccount(account);
        final boolean tryApiHack = false;
        if (apikey == null && tryApiHack && this.isAPIKey(account.getPass())) {
            apikey = account.getPass();
            /* Dirty hack */
            if (tryApiHack && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                account.setProperty(PROPERTY_ACCOUNT_apikey, apikey);
            }
        }
        return apikey;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        /* 2023-10-27: Special: Try API login first */
        final String apikey = getApikey(account);
        if (apikey != null) {
            /* Prefer API */
            try {
                final AccountInfo ai = this.fetchAccountInfoAPI(this.br, account);
                logger.info("Returning AccountInfo solely obtained via API");
                return ai;
            } catch (final Exception e) {
                logger.log(e);
                logger.info("API login failed");
            }
        }
        /* Fallback to upper handling */
        return super.fetchAccountInfo(account);
    }

    @Override
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(br, account);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        /** 2019-07-31: Better compare expire-date against their serverside time if possible! */
        final String server_timeStr = (String) entries.get("server_time");
        final Map<String, Object> result = (Map<String, Object>) entries.get("result");
        long expire_milliseconds_precise_to_the_second = 0;
        final String email = (String) result.get("email");
        final long currentTime;
        if (server_timeStr != null && server_timeStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            currentTime = TimeFormatter.getMilliSeconds(server_timeStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else {
            /* Fallback */
            currentTime = System.currentTimeMillis();
        }
        String premium_expireStr = (String) result.get("premium_expire");
        if (StringUtils.isEmpty(premium_expireStr)) {
            /*
             * 2019-05-30: Seems to be a typo by the guy who develops the XFS script in the early versions of thei "API mod" :D 2019-07-28:
             * Typo is fixed in newer XFSv3 versions - still we'll keep both versions in just to make sure it will always work ...
             */
            premium_expireStr = (String) entries.get("premim_expire");
        }
        final String premium_bandwidthStr = (String) result.get("premium_bandwidth");
        /*
         * 2019-08-22: For newly created free accounts, an expire-date will always be given, even if the account has never been a premium
         * account. This expire-date will usually be the creation date of the account then --> Handling will correctly recognize it as a
         * free account!
         */
        if (premium_expireStr != null && premium_expireStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            expire_milliseconds_precise_to_the_second = TimeFormatter.getMilliSeconds(premium_expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if (premium_bandwidthStr != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(premium_bandwidthStr));
        } else {
            ai.setUnlimitedTraffic();
        }
        final long premiumDurationMilliseconds = expire_milliseconds_precise_to_the_second - currentTime;
        if (premiumDurationMilliseconds <= 0) {
            /* Expired premium or no expire date given --> It is usually a Free Account */
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Expire date is in the future --> It is a premium account */
            ai.setValidUntil(System.currentTimeMillis() + premiumDurationMilliseconds);
            setAccountLimitsByType(account, AccountType.PREMIUM);
        }
        {
            /* Now set less relevant account information */
            final Object balanceO = result.get("balance");
            if (balanceO != null) {
                ai.setAccountBalance(SizeFormatter.getSize(balanceO.toString()));
            }
            /* 2019-07-26: values can also be "inf" for "Unlimited": "storage_left":"inf" */
            // final long storage_left = JavaScriptEngineFactory.toLong(entries.get("storage_left"), 0);
            final Object storage_used_bytesO = result.get("storage_used");
            if (storage_used_bytesO != null) {
                ai.setUsedSpace(SizeFormatter.getSize(storage_used_bytesO.toString()));
            }
        }
        if (this.enableAccountApiOnlyMode() && !StringUtils.isEmpty(email)) {
            /*
             * Each account is unique. Do not care what the user entered - trust what API returns! </br> This is not really important - more
             * visually so that something that makes sense is displayed to the user in his account managers' "Username" column!
             */
            account.setUser(email);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* Devs only */
            String accStatus;
            if (ai.getStatus() != null) {
                accStatus = ai.getStatus();
            } else {
                accStatus = account.getType().toString();
            }
            ai.setStatus("[API] " + accStatus);
        }
        return ai;
    }

    @Override
    protected boolean allowAPIDownloadIfApikeyIsAvailable(final DownloadLink link, final Account account) {
        final String apikey = getApikey(account);
        if (apikey != null && (isFreeAccountWithPremiumTraffic(account) || account.getType() == AccountType.PREMIUM) && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return true;
        } else {
            return false;
        }
    }
}