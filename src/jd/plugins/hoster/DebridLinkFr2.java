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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.components.config.DebridLinkFrConfig;
import org.jdownloader.plugins.components.config.DebridLinkFrConfig.PreferredDomain;
import org.jdownloader.plugins.components.realDebridCom.api.json.CodeResponse;
import org.jdownloader.plugins.components.realDebridCom.api.json.TokenResponse;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 4, names = { "debrid-link.fr" }, urls = { "" })
public class DebridLinkFr2 extends PluginForHost {
    private static MultiHosterManagement mhm                                                 = new MultiHosterManagement("debrid-link.fr");
    private static final String          PROPERTY_DIRECTURL                                  = "directurl";
    private static final String          PROPERTY_MAXCHUNKS                                  = "maxchunks";
    private static final String          PROPERTY_ACCOUNT_ACCESS_TOKEN                       = "access_token";
    private static final String          PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_CREATED     = "refresh_token_timestamp_created";
    private static final String          PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_VALID_UNTIL = "access_token_timestamp_valid_until";
    private static final String          PROPERTY_ACCOUNT_REFRESH_TOKEN                      = "refresh_token";
    private static final Set<String>     quotaReachedHostsList                               = new HashSet<String>();
    /** Contains timestamp when quotas of all quota limited hosts will be reset. */
    private static final AtomicLong      nextQuotaReachedResetTimestamp                      = new AtomicLong(0l);

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private int getMaxChunks(final DownloadLink link) {
        final int maxChunksStored = link.getIntegerProperty(PROPERTY_MAXCHUNKS, 1);
        if (maxChunksStored > 1) {
            /* Minus maxChunksStored -> Up to X chunks */
            return -maxChunksStored;
        } else {
            return maxChunksStored;
        }
    }

    public DebridLinkFr2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://debrid-link.fr/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://debrid-link.fr/tos";
    }

    private static Browser prepBR(final Browser prepBr) {
        /* Define custom browser headers and language settings. */
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("UTF-8");
        prepBr.setFollowRedirects(true);
        prepBr.setAllowedResponseCodes(new int[] { 400 });
        return prepBr;
    }

    private String getApiBase() {
        return "https://" + getConfiguredDomain() + "/api/v2";
    }

    private String getApiBaseOauth() {
        return "https://" + getConfiguredDomain() + "/api/oauth";
    }

    private String getClientID() {
        return "5PyzfhqQM0PgFPEArhBadw";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        login(account, true);
        if (br.getRequest() == null || !br.getURL().contains("/account/infos")) {
            callAPIGetAccountInfo(br);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("value");
        /* Set censored E-Mail address of user as username. */
        final String emailCensored = (String) entries.get("email");
        if (!StringUtils.isEmpty(emailCensored)) {
            account.setUser(emailCensored);
        }
        /* User could have entered his real password -> We don't want to store that! */
        account.setPass(null);
        final int accountType = ((Number) entries.get("accountType")).intValue();
        final long premiumLeft = ((Number) entries.get("premiumLeft")).longValue();
        final String registerDate = (String) entries.get("registerDate");
        final Object serverDetected = entries.get("serverDetected");
        final boolean isFree;
        switch (accountType) {
        case 0:
            ac.setStatus("Free Account");
            account.setType(AccountType.FREE);
            ac.setValidUntil(-1);
            isFree = true;
            break;
        case 1:
            if (premiumLeft > 0) {
                ac.setValidUntil(System.currentTimeMillis() + (premiumLeft * 1000l));
            }
            if (premiumLeft < 0 || ac.isExpired()) {
                ac.setStatus("Premium Account (Expired)");
                account.setType(AccountType.FREE);
                ac.setValidUntil(-1);
                isFree = true;
            } else {
                ac.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
                isFree = false;
            }
            break;
        case 2:
            ac.setStatus("Lifetime Account");
            account.setType(AccountType.LIFETIME);
            ac.setValidUntil(-1);
            isFree = false;
            break;
        default:
            isFree = true;
            ac.setStatus("Unknown account type: " + accountType);
            account.setType(AccountType.FREE);
            logger.warning("Unknown account type: " + accountType);
            break;
        }
        if (!StringUtils.isEmpty(registerDate)) {
            ac.setCreateTime(TimeFormatter.getMilliSeconds(registerDate, "yyyy-MM-dd", Locale.ENGLISH));
        }
        /* Update list of supported hosts */
        /* https://debrid-link.com/api_doc/v2/downloader-regex */
        br.getPage(this.getApiBase() + "/downloader/hosts?keys=status%2CisFree%2Cname%2Cdomains");
        final List<String> supportedHosts = new ArrayList<String>();
        entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final List<Object> hosters = (List<Object>) entries.get("value");
        final HashMap<String, String> name2RealHostMap = new HashMap<String, String>();
        final AccountInfo dummyAccInfo = new AccountInfo();
        for (final Object hostO : hosters) {
            entries = (Map<String, Object>) hostO;
            final String hostname = (String) entries.get("name");
            final int status = ((Number) entries.get("status")).intValue();
            final boolean isFreeHost = ((Boolean) entries.get("isFree")).booleanValue();
            /* Don't add hosts if they are down or disabled, */
            if (status == -1 || status == 0) {
                // logger.info("NOT adding host " + name + " to host array because it is down or disabled");
                continue;
            } else if (isFree && !isFreeHost) {
                /* Don't add hosts which are not supported via the current account type - important for free accounts. */
                // logger.info("NOT adding host " + name + " to host array because user has a free account and this is not a free host");
                continue;
            } else {
                /* Add all domains of this host */
                final List<Object> domainsO = (List<Object>) entries.get("domains");
                final ArrayList<String> supportedHostsTmp = new ArrayList<String>();
                for (final Object domainO : domainsO) {
                    // supportedHosts.add((String) domainO);
                    supportedHostsTmp.add((String) domainO);
                }
                /* Workaround: Find our "real host" which we internally use -> We need this later! */
                final List<String> hostsReal = dummyAccInfo.setMultiHostSupport(this, supportedHostsTmp);
                if (hostsReal != null && !hostsReal.isEmpty()) {
                    int index = -1;
                    for (final String realHost : hostsReal) {
                        index += 1;
                        if (realHost == null) {
                            continue;
                        } else {
                            if (!supportedHosts.contains(realHost)) {
                                supportedHosts.add(realHost);
                            }
                            if (index == 0) {
                                /* Allow only exactly one host as mapping -> Should be fine in most of all cases */
                                name2RealHostMap.put(hostname, realHost);
                            }
                        }
                    }
                }
            }
        }
        ac.setMultiHostSupport(this, supportedHosts);
        br.getPage(this.getApiBase() + "/downloader/limits/all");
        entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("value");
        /** How much percent of the (daily?) quota is used up so far? */
        final Map<String, Object> usagePercentMap = (Map<String, Object>) entries.get("usagePercent");
        final int usedPercent = ((Number) usagePercentMap.get("current")).intValue();
        if (!StringUtils.isEmpty(ac.getStatus())) {
            ac.setStatus(ac.getStatus() + " | " + usedPercent + "% used");
        }
        final Map<String, Object> nextResetSecondsMap = (Map<String, Object>) entries.get("nextResetSeconds");
        final int nextResetSeconds = ((Number) nextResetSecondsMap.get("value")).intValue();
        synchronized (quotaReachedHostsList) {
            quotaReachedHostsList.clear();
            nextQuotaReachedResetTimestamp.set(System.currentTimeMillis() + nextResetSeconds * 1001l);
            final List<Object> hosters2 = (List<Object>) entries.get("hosters");
            for (final Object hostO : hosters2) {
                entries = (Map<String, Object>) hostO;
                final String hostname = (String) entries.get("name");
                final Map<String, Object> trafficLimit = (Map<String, Object>) entries.get("daySize");
                final Map<String, Object> downloadsNumberLimit = (Map<String, Object>) entries.get("dayCount");
                /* Check if user has exceeded any of the current host limits -> Then we won't allow this user to download. */
                boolean userHasReachedLimit = ((Number) trafficLimit.get("current")).longValue() >= ((Number) trafficLimit.get("value")).longValue() || ((Number) downloadsNumberLimit.get("current")).longValue() >= ((Number) downloadsNumberLimit.get("value")).longValue();
                if (userHasReachedLimit) {
                    logger.info("Currently quota limited host: " + hostname);
                    final String realHost = name2RealHostMap.get(hostname);
                    if (realHost == null) {
                        logger.warning("WTF inconsistent map is missing entry for: " + hostname);
                    } else {
                        quotaReachedHostsList.add(realHost);
                    }
                }
            }
        }
        /**
         * 2021-02-23: This service doesn't allow users to use it whenever they use a VPN/Proxy. </br> Accounts can be checked but downloads
         * will not work!
         */
        if (serverDetected != null && serverDetected instanceof Boolean && ((Boolean) serverDetected).booleanValue()) {
            throw new AccountUnavailableException("VPN/Proxy detected: Turn it off to be able to use this account", 5 * 60 * 1000l);
        }
        return ac;
    }

    private boolean hostHasReachedQuotaLimit(final String host, final boolean removeFlag) {
        synchronized (quotaReachedHostsList) {
            if (quotaReachedHostsList.contains(host) && nextQuotaReachedResetTimestamp.get() > System.currentTimeMillis()) {
                return false;
            } else if ((removeFlag && quotaReachedHostsList.remove(host)) || quotaReachedHostsList.contains(host)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean accountAccessTokenExpired(final Account account) {
        return System.currentTimeMillis() > account.getLongProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_VALID_UNTIL, 0);
    }

    private boolean accountAccessTokenNeedsRefresh(final Account account) {
        /* Refresh token every 24 hours. */
        return System.currentTimeMillis() - account.getLongProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_CREATED, 0) > 24 * 60 * 60 * 1000l;
    }

    private String accountGetAccessToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
    }

    private String accountGetRefreshToken(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN);
    }

    private void dumpSession(final Account account) {
        account.removeProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
        account.removeProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN);
        account.removeProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_VALID_UNTIL);
    }

    private void callAPIGetAccountInfo(final Browser br) throws IOException {
        br.getPage(this.getApiBase() + "/account/infos");
    }

    private void login(final Account account, final boolean verifyLogin) throws Exception {
        synchronized (account) {
            prepBR(this.br);
            long expiresIn = 0;
            if (this.accountGetAccessToken(account) != null && !accountAccessTokenExpired(account) && !accountAccessTokenNeedsRefresh(account)) {
                /* Check existing token */
                logger.info("Attempting token login");
                br.getHeaders().put("Authorization", "Bearer " + this.accountGetAccessToken(account));
                if (!verifyLogin) {
                    logger.info("Trust token without check");
                    return;
                } else {
                    this.callAPIGetAccountInfo(br);
                    errHandling(account, null);
                    logger.info("Token login successful | Token is valid for: " + TimeFormatter.formatMilliSeconds(account.getLongProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_VALID_UNTIL, 0) - System.currentTimeMillis(), 0));
                    return;
                }
            } else if (this.accountGetRefreshToken(account) != null) {
                logger.info("Trying to refresh access_token");
                final UrlQuery query = new UrlQuery();
                query.add("client_id", Encoding.urlEncode(this.getClientID()));
                query.add("refresh_token", this.accountGetRefreshToken(account));
                query.add("grant_type", "refresh_token");
                prepBR(this.br);
                br.postPage(this.getApiBaseOauth() + "/token", query);
                final TokenResponse tokenResponse = JSonStorage.restoreFromString(this.br.toString(), new TypeRef<TokenResponse>(TokenResponse.class) {
                });
                if (!StringUtils.isEmpty(tokenResponse.getAccess_token())) {
                    logger.info("Refresh token successful");
                    br.getHeaders().put("Authorization", "Bearer " + tokenResponse.getAccess_token());
                    account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, tokenResponse.getAccess_token());
                    account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_CREATED, System.currentTimeMillis());
                    this.accountSetTokenValidity(account, tokenResponse.getExpires_in());
                    return;
                } else {
                    /* This should never happen except maybe if the user has manually revoked API access! */
                    logger.info("Refresh token failed");
                    /* Dump session to make sure we do full login next time! */
                    this.dumpSession(account);
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Sitzung abgelaufen: Aktualisiere diesen Account, um dich neu einzuloggen", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Session expired: Refresh this account to re-login", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
            } else {
                logger.info("Full login required");
                final UrlQuery queryDevicecode = new UrlQuery();
                queryDevicecode.add("client_id", Encoding.urlEncode(this.getClientID()));
                queryDevicecode.add("scopes", "get.account,get.post.downloader");
                prepBR(this.br);
                br.postPage(this.getApiBaseOauth() + "/device/code", queryDevicecode);
                final CodeResponse code = JSonStorage.restoreFromString(this.br.toString(), new TypeRef<CodeResponse>(CodeResponse.class) {
                });
                final Thread dialog = showPINLoginInformation(code.getVerification_url(), code.getUser_code());
                final UrlQuery queryPollingLogin = new UrlQuery();
                queryPollingLogin.add("client_id", Encoding.urlEncode(this.getClientID()));
                queryPollingLogin.appendEncoded("code", code.getDevice_code());
                queryPollingLogin.appendEncoded("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
                // queryPollingLogin.add("grant_type", "http%3A%2F%2Foauth.net%2Fgrant_type%2Fdevice%2F1.0");
                long waitedSeconds = 0;
                final Browser br2 = this.br.cloneBrowser();
                try {
                    do {
                        logger.info("Waiting for user to authorize application: " + waitedSeconds);
                        Thread.sleep(code.getInterval() * 1000l);
                        waitedSeconds += code.getInterval();
                        br2.postPage(this.getApiBaseOauth() + "/token", queryPollingLogin);
                        /*
                         * E.g. returns the following as long as we're waiting for the user to authorize: {"error":"authorization_pending"}
                         */
                        final TokenResponse tokenResponse = JSonStorage.restoreFromString(br2.toString(), new TypeRef<TokenResponse>(TokenResponse.class) {
                        });
                        if (!StringUtils.isEmpty(tokenResponse.getAccess_token())) {
                            expiresIn = tokenResponse.getExpires_in();
                            account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, tokenResponse.getAccess_token());
                            account.setProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN, tokenResponse.getRefresh_token());
                            account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_CREATED, System.currentTimeMillis());
                            this.accountSetTokenValidity(account, expiresIn);
                            br.getHeaders().put("Authorization", "Bearer " + this.accountGetAccessToken(account));
                            return;
                        } else if (!dialog.isAlive()) {
                            logger.info("Dialog closed!");
                            break;
                        } else if (waitedSeconds >= code.getExpires_in()) {
                            logger.info("Timeout reached: " + code.getExpires_in());
                            break;
                        }
                    } while (true);
                } finally {
                    dialog.interrupt();
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    /**
     * Sets token validity. </br> 2021-02-19: Token validity is set to 1 month via: https://debrid-link.fr/webapp/account/apps
     */
    private void accountSetTokenValidity(final Account account, final long expiresIn) {
        account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_TIMESTAMP_VALID_UNTIL, System.currentTimeMillis() + expiresIn * 1000l);
    }

    private Thread showPINLoginInformation(final String pin_url, final String user_code) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Debrid-link.fr - Login";
                        message += "Hallo liebe(r) debrid-link NutzerIn\r\n";
                        message += "Um deinen debrid-link Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "2. Gib diesen PIN Code ein: " + user_code + "\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Debrid-link.fr - Login";
                        message += "Hello dear debrid-link user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "2. Enter this PIN code: " + user_code + "\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(pin_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** List of errors: https://debrid-link.com/api_doc/v2/errors */
    private void errHandling(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        try {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final boolean success = ((Boolean) entries.get("success")).booleanValue();
            if (success) {
                return;
            }
            String error = (String) entries.get("error");
            if (error == null) {
                /* Fallback */
                error = "Unknown error";
            }
            /* First handle account errors */
            if ("badToken".equals(error)) {
                /**
                 * E.g. if user revokes access via debrid-link website. Delete access_token so next time we can try to refresh session.
                 */
                account.removeProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
                throw new AccountUnavailableException("Session expired", 1 * 60 * 1000l);
            } else if ("serverNotAllowed".equals(error)) {
                /** Temporary IP (account) ban (used used a VPN/Proxy which is not allowed) */
                throw new AccountUnavailableException("Dedicated Server/VPN/Proxy detected, account disabled!", 10 * 60 * 1000l);
            } else if ("disabledServerHost".equals(error)) {
                /** Happens if downloading from single hosts is not allowed via VPN/proxy. */
                mhm.putError(account, link, 5 * 60 * 1000l, "Host prohibits VPN/Proxy usage");
            } else if ("fileNotAvailable".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The file seem to be temporarily unavailable on the host side", 30 * 60 * 1000l);
            } else if ("floodDetected".equals(error)) {
                throw new AccountUnavailableException("API Flood, will retry in 1 hour!", 30 * 60 * 1001l);
            } else if ("accountLocked".equals(error)) {
                throw new AccountUnavailableException("Account locked", 30 * 60 * 1001l);
            } else if ("fileNotFound".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted error 'file not found'", 5 * 60 * 1000l);
            } else if ("notDebrid".equals(error)) {
                mhm.putError(account, link, 2 * 60 * 1000l, "Disabled filehost: server error notDebrid");
            } else if ("disabledHost".equals(error)) {
                /* The filehoster is disabled */
                mhm.putError(account, link, 5 * 60 * 1000l, "Disabled filehost");
            } else if ("notFreeHost".equals(error)) {
                mhm.putError(account, link, 5 * 60 * 1000l, "Disabled filehost as it is only available for premium users");
            } else if ("maintenanceHost".equals(error) || "noServerHost".equals(error)) {
                /* Some generic "Host currently doesn't work" traits! */
                mhm.putError(account, link, 2 * 60 * 1000l, error);
            } else if ("maxLinkHost".equals(error)) {
                synchronized (quotaReachedHostsList) {
                    quotaReachedHostsList.add(link.getHost());
                }
                mhm.putError(account, link, 2 * 60 * 1000l, "Max links per day limit reached for this host");
            } else if ("maxDataHost".equals(error)) {
                synchronized (quotaReachedHostsList) {
                    quotaReachedHostsList.add(link.getHost());
                }
                mhm.putError(account, link, 2 * 60 * 1000l, "Max data per day limit reached for this host");
            } else if ("maxLink".equals(error)) {
                throw new AccountUnavailableException("Download limit reached: Max links per day", 10 * 60 * 1001l);
            } else if ("maxData".equals(error)) {
                throw new AccountUnavailableException("Download limit reached: Max traffic per day", 10 * 60 * 1001l);
            } else if ("freeServerOverload".equals(error)) {
                /* I assume this means free account downloads from this host are not possible at the moment? */
                mhm.putError(account, link, 10 * 60 * 1000l, "Free account downloads not possible at the moment");
            } else if (isErrorPasswordRequiredOrWrong(error)) {
                /* This error will usually be handled outside of here! */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else if ("maxSimultaneousFilesHost".equals(error)) {
                mhm.putError(account, link, 3 * 60 * 1000l, "Too many simultaneous downloads over this host via debrid-link");
            } else {
                if (link != null) {
                    mhm.handleErrorGeneric(account, link, error, 50);
                } else {
                    throw new AccountUnavailableException("Unknown error: " + error, 5 * 60 * 1001l);
                }
            }
        } catch (final JSonMapperException parserFailure) {
            logger.exception("Json parsing failed -> Probably HTML response", parserFailure);
            throw parserFailure;
        }
    }

    private boolean isErrorPasswordRequiredOrWrong(final String str) {
        return str != null && str.equalsIgnoreCase("badFilePassword");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception, PluginException {
        /* This should never be called. */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        /* Extra check for pre-stored "quota reached" state -> Saves a few http requests */
        if (hostHasReachedQuotaLimit(link.getHost(), false)) {
            try {
                mhm.putError(account, link, 5 * 60 * 1000l, "Quota reached");
            } finally {
                hostHasReachedQuotaLimit(link.getHost(), true);
            }
        }
        mhm.runCheck(account, link);
        this.login(account, false);
        if (!attemptStoredDownloadurlDownload(link)) {
            boolean enteredCorrectPassword = false;
            String passCode = link.getDownloadPassword();
            for (int wrongPasswordAttempts = 0; wrongPasswordAttempts <= 2; wrongPasswordAttempts++) {
                final UrlQuery query = new UrlQuery();
                query.appendEncoded("url", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                if (passCode != null) {
                    query.appendEncoded("password", passCode);
                }
                br.postPage(this.getApiBase() + "/downloader/add", query);
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                if (this.isErrorPasswordRequiredOrWrong((String) entries.get("error"))) {
                    wrongPasswordAttempts += 1;
                    passCode = getUserInput("Password?", link);
                    /*
                     * Do not reset initial password. Multihosters are prone to error - we do not want to remove the users' initial manually
                     * typed in PW!
                     */
                    // link.setDownloadPassword(null);
                    continue;
                } else {
                    enteredCorrectPassword = true;
                    break;
                }
            }
            if (!enteredCorrectPassword) {
                /* Allow next candidate to try */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wrong password entered");
            } else if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            this.errHandling(account, link);
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("value");
            link.setProperty(PROPERTY_MAXCHUNKS, entries.get("chunk"));
            final String dllink = (String) entries.get("downloadUrl");
            if (dllink == null) {
                logger.warning("Failed to find dllink");
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), getMaxChunks(link));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                try {
                    errHandling(account, link);
                } catch (JSonMapperException ignore) {
                    logger.log(ignore);
                }
                if (br.containsHTML("Unable to download the file on the host server")) {
                    mhm.handleErrorGeneric(account, link, "Unable to download the file on the host server", 50, 5 * 60 * 1000l);
                }
                logger.warning("Unhandled download error on Service Provider side:");
                mhm.handleErrorGeneric(account, link, "final_downloadurl_isnot_a_file", 50, 5 * 60 * 1000l);
            }
            link.setProperty(PROPERTY_DIRECTURL, dl.getConnection().getURL().toString());
        }
        /*
         * 2019-09-06: They sometimes return wrong final filenames e.g. for vidoza.net filenames will all get changed to "video.mp4". This
         * is a small workaround. In general, it prefers the filenames of the original plugin if they're longer.
         */
        String final_filename = null;
        final String previous_filename = link.getName();
        final String this_filename = getFileNameFromHeader(dl.getConnection());
        if (previous_filename != null && this_filename != null && (this_filename.length() < previous_filename.length() || link.getHost().contains("vidoza"))) {
            final_filename = previous_filename;
        } else {
            final_filename = this_filename;
        }
        if (final_filename != null) {
            link.setFinalFileName(final_filename);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, null), this.getMaxChunks(link));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final IOException e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e2) {
            }
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    protected String getConfiguredDomain() {
        /* Returns user-defined value which can be used to circumvent GEO-block. */
        PreferredDomain cfgdomain = PluginJsonConfig.get(DebridLinkFrConfig.class).getPreferredDomain();
        if (cfgdomain == null) {
            cfgdomain = PreferredDomain.DEFAULT;
        }
        switch (cfgdomain) {
        case DOMAIN1:
            return "debrid-link.com";
        case DEFAULT:
        default:
            return this.getHost();
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DebridLinkFrConfig.class;
    }
}