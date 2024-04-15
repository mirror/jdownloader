//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CheckableLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alldebrid.com" }, urls = { "https?://alldebrid\\.com/f/([A-Za-z0-9\\-_]+)" })
public class AllDebridCom extends PluginForHost {
    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        /* 2020-03-27: As long as we're below 4 API requests per second we're fine according to admin. */
        setStartIntervall(1000);
        this.enablePremium("https://alldebrid.com/offer/");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public String getAGBLink() {
        return "https://www.alldebrid.com/tos/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return "alldebrid://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    protected boolean supportsUpdateDownloadLink(CheckableLink checkableLink) {
        return checkableLink != null && checkableLink.getDownloadLink() != null;
    }

    private boolean isSelfhosted(final DownloadLink link) {
        if (link != null && this.canHandle(link.getPluginPatternMatcher())) {
            return true;
        } else {
            return false;
        }
    }

    private static MultiHosterManagement  mhm                                  = new MultiHosterManagement("alldebrid.com");
    public static final String            api_base                             = "https://api.alldebrid.com/v4";
    // this is used by provider which calculates unique token to agent/client.
    public static final String            agent_raw                            = "JDownloader";
    private static final String           agent                                = "agent=" + agent_raw;
    private final String                  PROPERTY_APIKEY_CREATED_TIMESTAMP    = "APIKEY_CREATED_TIMESTAMP";
    private static final String           PROPERTY_apikey                      = "apiv4_apikey";
    private final String                  PROPERTY_maxchunks                   = "alldebrid_maxchunks";
    private final AccountInvalidException exceptionFreeAccountsAreNotSupported = new AccountInvalidException("Free accounts are not supported!");

    public String login(final Account account, final AccountInfo accountInfo, final boolean validateApikey) throws Exception {
        synchronized (account) {
            String apikey = getStoredApiKey(account);
            if (apikey != null) {
                if (!validateApikey) {
                    setAuthHeader(br, apikey);
                    return apikey;
                }
                logger.info("Attempting login with existing apikey");
                getAccountInfo(account, accountInfo, apikey);
                logger.info("Apikey login successful");
                return apikey;
            }
            /* Full login */
            logger.info("Performing full login");
            br.getPage(api_base + "/pin/get?" + agent);
            final Map<String, Object> entries = this.handleErrors(account, null);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final String user_url = data.get("user_url").toString();
            final String check_url = data.get("check_url").toString();
            final int maxSecondsServerside = ((Number) data.get("expires_in")).intValue();
            final Thread dialog = showPINLoginInformation(user_url, maxSecondsServerside);
            int secondsWaited = 0;
            final int waitSecondsPerLoop = 3;
            try {
                for (int i = 0; i <= 23; i++) {
                    logger.info("Waiting for user to authorize application. Seconds waited: " + secondsWaited + "/" + maxSecondsServerside);
                    Thread.sleep(waitSecondsPerLoop * 1000);
                    secondsWaited += waitSecondsPerLoop;
                    br.getPage(check_url);
                    if (secondsWaited >= maxSecondsServerside) {
                        logger.info("Stopping because: Timeout #1 | User did not perform authorization within " + maxSecondsServerside + " seconds");
                        break;
                    }
                    /** Example response: { "status": "success", "data": { "activated": false, "expires_in": 590 }}} */
                    final Map<String, Object> resp = this.handleErrors(account, null);
                    final Map<String, Object> respData = (Map<String, Object>) resp.get("data");
                    apikey = (String) respData.get("apikey");
                    final int secondsLeftServerside = ((Number) data.get("expires_in")).intValue();
                    if (!StringUtils.isEmpty(apikey)) {
                        logger.info("Stopping because: Found apikey!");
                        break;
                    } else if (secondsLeftServerside < waitSecondsPerLoop) {
                        logger.info("Stopping because: Timeout #2");
                        break;
                    } else if (!dialog.isAlive()) {
                        logger.info("Stopping because: Dialog closed!");
                        break;
                    }
                }
            } finally {
                dialog.interrupt();
            }
            if (StringUtils.isEmpty(apikey)) {
                throw new AccountInvalidException("User failed to authorize PIN/Code. Do not close the pairing dialog until you have confirmed the PIN/Code via browser!");
            }
            account.setProperty(PROPERTY_apikey, apikey);
            /* Save this property - it might be useful in the future. */
            account.setProperty(PROPERTY_APIKEY_CREATED_TIMESTAMP, System.currentTimeMillis());
            getAccountInfo(account, accountInfo, apikey);
            setAuthHeader(br, apikey);
            return apikey;
        }
    }

    private void getAccountInfo(final Account account, final AccountInfo ai, final String apikey) throws Exception {
        synchronized (account) {
            setAuthHeader(br, apikey);
            br.getPage(api_base + "/user?" + agent);
            final Map<String, Object> root = handleErrors(account, null);
            final Map<String, Object> data = (Map<String, Object>) root.get("data");
            final Map<String, Object> user = (Map<String, Object>) data.get("user");
            final String userName = (String) user.get("username");
            if (!StringUtils.isEmpty(userName)) {
                account.setUser(userName);
            }
            /*
             * Do not store any (old) website login credentials! The only thing we need is the users' apikey and we will store this as a
             * property in our Account object!
             */
            account.setPass(null);
            if ((Boolean) user.get("isPremium") == Boolean.TRUE) {
                final Number premiumUntil = (Number) user.get("premiumUntil");
                if (premiumUntil != null) {
                    ai.setValidUntil(premiumUntil.longValue() * 1000l, br);
                }
                if ((Boolean) user.get("isTrial") == Boolean.TRUE) {
                    /*
                     * 2020-03-27: Premium "test" accounts which last 7 days and have a total of 25GB as quota. Once that limit is reached,
                     * they can only download from "Free" hosts (only a hand full of hosts).
                     */
                    ai.setStatus("Premium Account (Free trial, reverts to free once traffic is used up)");
                    final Number remainingTrialQuota = (Number) user.get("remainingTrialQuota");
                    if (remainingTrialQuota != null) {
                        /* 2020-03-27: Hardcoded maxTraffic value */
                        final long maxTraffic = SizeFormatter.getSize("25GB");
                        final long remainingTrialTraffic = remainingTrialQuota.longValue() * 1000 * 1000;
                        ai.setTrafficLeft(remainingTrialTraffic);
                        if (remainingTrialTraffic <= maxTraffic) {
                            ai.setTrafficMax(maxTraffic);
                        }
                    }
                } else {
                    /* "Real" premium account. */
                    ai.setStatus("Premium Account");
                }
                account.setType(AccountType.PREMIUM);
            } else {
                /*
                 * "Real" free account (or expired trial premium [= user downloaded more than 25GB trial quota]) --> Cannot download and
                 * cannot even login via API officially!
                 */
                throw exceptionFreeAccountsAreNotSupported;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo accountInfo = new AccountInfo();
        login(account, accountInfo, true);
        /* They got 3 arrays of types of supported websites --> We want to have the "hosts" Array only! */
        br.getPage(api_base + "/user/hosts?" + agent + "&hostsOnly=true");
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> supportedHostsInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/hosts");
        final Iterator<Entry<String, Object>> iterator = supportedHostsInfo.entrySet().iterator();
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        while (iterator.hasNext()) {
            try {
                final Entry<String, Object> hostO = iterator.next();
                final Map<String, Object> entry = (Map<String, Object>) hostO.getValue();
                String host_without_tld = (String) entry.get("name");
                if (StringUtils.isEmpty(host_without_tld)) {
                    host_without_tld = hostO.getKey();
                }
                /*
                 * 2020-04-01: This check will most likely never be required as free accounts officially cannot be used via API at all and
                 * JD also does not accept them but we're doing this check nevertheless.
                 */
                final String type = (String) entry.get("type");
                if (account.getType() == AccountType.FREE && !"free".equalsIgnoreCase(type)) {
                    logger.info("Skipping host because it cannot be used with free accounts: " + host_without_tld);
                    continue;
                }
                /*
                 * Most hosts either have no limit or traffic limit ("traffic"). Some have a 'max downloads per day' limit instead -->
                 * "nb_download". We cannot handle this and thus ignore it. Also ignore trafficlimit because they could always deliver
                 * cached content for which the user will not be charged traffic at all!
                 */
                // final String quotaType = (String) entries.get("quotaType");
                // final long quotaLeft = JavaScriptEngineFactory.toLong(entries.get("quota"), -1);
                /* Skip currently disabled hosts --> 2020-03-26: Do not skip any hosts anymore, display all in JD RE: admin */
                // if (Boolean.FALSE.equals(entry.get("status"))) {
                // continue;
                // }
                /* Add all domains of host */
                final Object domainsO = entry.get("domains");
                if (domainsO == null && StringUtils.isEmpty(host_without_tld)) {
                    logger.info("WTF: " + host_without_tld);
                    continue;
                }
                if (domainsO != null) {
                    final List<String> domains = (List<String>) entry.get("domains");
                    for (final String domain : domains) {
                        supportedHosts.add(domain);
                    }
                } else {
                    /* Fallback - this should usually not happen */
                    logger.info("Adding host_without_tld: " + host_without_tld);
                    supportedHosts.add(host_without_tld);
                }
            } catch (final Throwable e) {
                /* Skip broken/unexpected json objects */
            }
        }
        accountInfo.setMultiHostSupport(this, supportedHosts);
        return accountInfo;
    }

    private Thread showPINLoginInformation(final String pin_url, final int timeoutSeconds) {
        final String host = getHost();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Alldebrid - Login";
                        message += "Hallo liebe(r) alldebrid NutzerIn\r\n";
                        message += "Um deinen " + host + " Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "2. Bestätige die PIN/Code im Browser.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Alldebrid - Login";
                        message += "Hello dear alldebrid user\r\n";
                        message += "In order to use " + host + " in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it wasn't opened automatically:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "2. Confirm the PIN/Code you see in the browser window.\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(timeoutSeconds * 1000);
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

    /**
     * This is executed whenever user is suddently logged out by alldebrid as a security measurement and needs to confirm his login via
     * email.
     */
    public void authBlockedLogin(final Account account, final DownloadLink link, final Map<String, Object> errormap) throws Exception {
        final String msg = errormap.get("message").toString();
        final String token = errormap.get("token").toString();
        if (StringUtils.isEmpty(msg)) {
            /* This should never happen */
            throw new IllegalArgumentException();
        }
        if (StringUtils.isEmpty(token)) {
            /* No token given -> No way for us to refresh apikey */
            account.removeProperty(PROPERTY_apikey);
            throw new AccountInvalidException("Authorization has been denied by account owner.");
        }
        final AccountUnavailableException exceptionOnFailure = new AccountUnavailableException(msg, 10 * 60 * 1000);
        final PluginException exceptionOnDownloadAndSuccess = new PluginException(LinkStatus.ERROR_RETRY, "Retry after successful AUTH_BLOCKED handling");
        final String oldAuth = br.getHeaders().getHeader(HTTPConstants.HEADER_REQUEST_AUTHORIZATION).getValue();
        synchronized (account) {
            final String newAuth = br.getHeaders().getHeader(HTTPConstants.HEADER_REQUEST_AUTHORIZATION).getValue();
            if (!StringUtils.equals(oldAuth, newAuth)) {
                /* Previous download-attempt already successfully ran through this handling so we don't need to do this here again. */
                throw (PluginException) exceptionOnDownloadAndSuccess.fillInStackTrace();
            }
            logger.info("Performing auth blocked login");
            final String check_url = api_base + "/user/verif?agent=" + agent_raw + "&token=" + token;
            final int maxSecondsWait = 600;
            final Thread dialog = showNewLocationLoginInformation(null, msg, maxSecondsWait);
            int secondsWaited = 0;
            final int waitSecondsPerLoop = 3;
            String apikey = null;
            boolean throwThisException = false;
            try {
                try {
                    for (int i = 0; i <= 23; i++) {
                        logger.info("Waiting for user to authorize application. Seconds waited: " + secondsWaited + "/" + maxSecondsWait);
                        Thread.sleep(waitSecondsPerLoop * 1000);
                        secondsWaited += waitSecondsPerLoop;
                        br.getPage(check_url);
                        if (secondsWaited >= maxSecondsWait) {
                            logger.info("Stopping because: Timeout #1 | User did not perform authorization within " + maxSecondsWait + " seconds");
                            break;
                        }
                        /** Example response: { "status": "success", "data": { "activated": false, "expires_in": 590 }}} */
                        final Map<String, Object> resp = this.handleErrors(account, link);
                        final Map<String, Object> data = (Map<String, Object>) resp.get("data");
                        final String verifStatus = data.get("verif").toString();
                        if (verifStatus.equals("denied")) {
                            /* User opened link from email and denied this login-attempt. */
                            throwThisException = true;
                            throw new AccountInvalidException("Authorization has been denied by account owner");
                        }
                        apikey = (String) data.get("apikey");
                        if (!StringUtils.isEmpty(apikey)) {
                            logger.info("Stopping because: Found apikey!");
                            break;
                        } else if (!dialog.isAlive()) {
                            logger.info("Stopping because: Dialog closed!");
                            break;
                        }
                    }
                } finally {
                    dialog.interrupt();
                }
            } catch (final Exception e) {
                if (throwThisException) {
                    throw e;
                } else {
                    throw (AccountUnavailableException) exceptionOnFailure.fillInStackTrace();
                }
            }
            if (StringUtils.isEmpty(apikey)) {
                logger.warning("Failed for unknown reasons");
                throw (AccountUnavailableException) exceptionOnFailure.fillInStackTrace();
            }
            logger.info("Using new apikey: " + apikey);
            account.setProperty(PROPERTY_apikey, apikey);
            setAuthHeader(br, apikey);
            if (link == null) {
                throw new AccountUnavailableException("Retry after blocked login has been cleared", 5 * 1000);
            } else {
                throw (PluginException) exceptionOnDownloadAndSuccess.fillInStackTrace();
            }
        }
    }

    private Thread showNewLocationLoginInformation(final String pin_url, final String api_errormsg, final int timeoutSeconds) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Alldebrid - Login von neuem Standort bestätigen";
                        message += "Hallo liebe(r) alldebrid NutzerIn";
                        message += "\r\nDu versuchst gerade, deinen Account an einem neuen Standort zu verwenden.";
                        message += "\r\nBestätige diesen Loginversuch mit dem Link, den du per E-Mail erhalten hast, um deinen Account in JD weiterverwenden zu können.";
                        message += "\r\nDieser dialog schließt sich automatisch, sobald du den Login bestätigt hast.";
                        if (api_errormsg != null) {
                            message += "\r\nFehlermeldung der Alldebrid API:";
                            message += "\r\n" + api_errormsg;
                        }
                    } else {
                        title = "Alldebrid - Confirm new location";
                        message += "Hello dear alldebrid user";
                        message += "\r\nYou are trying to use this service from a new location.";
                        message += "\r\nYou've received an e-mail with a link to confirm this new location.";
                        message += "\r\n Confirm this e-mail to continue using your Alldebrid account in JDownloader.";
                        message += "\r\nOnce accepted, this dialog will be closed automatically.";
                        if (api_errormsg != null) {
                            message += "\r\nMessage from Alldebrid API:";
                            message += "\r\n" + api_errormsg;
                        }
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(timeoutSeconds * 1000);
                    if (pin_url != null && CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
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

    /** See https://docs.alldebrid.com/v4/#all-errors */
    private Map<String, Object> handleErrors(final Account account, final DownloadLink link) throws PluginException, Exception {
        /* 2020-03-25: E.g. {"status": "error", "error": {"code": "AUTH_BAD_APIKEY","message": "The auth apikey is invalid"}} */
        Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String status = (String) entries.get("status");
        if (!"error".equalsIgnoreCase(status)) {
            return entries;
        }
        final boolean isSelfhosted = this.isSelfhosted(link);
        final Map<String, Object> errormap = (Map<String, Object>) entries.get("error");
        final String code = (String) errormap.get("code");
        String message = (String) errormap.get("message");
        if (StringUtils.isEmpty(message)) {
            /* We always want to have a human readable errormessage */
            message = "Unknown error";
        }
        if (code.equalsIgnoreCase("AUTH_BAD_APIKEY")) {
            /* This is the only error which allows us to remove the apikey and re-login. */
            account.removeProperty(PROPERTY_apikey);
            throw new AccountInvalidException("Invalid login: " + message + "\r\nRefresh account to renew apikey.");
        } else if (code.equalsIgnoreCase("AUTH_BLOCKED")) {
            /* Apikey GEO-blocked or IP blocked */
            this.authBlockedLogin(account, link, errormap);
        } else if (code.equalsIgnoreCase("AUTH_USER_BANNED")) {
            throw new AccountInvalidException(message);
        } else if (code.equalsIgnoreCase("DELAYED_INVALID_ID")) {
            /* This can only happen in '/link/delayed' handling */
            mhm.handleErrorGeneric(account, link, message, 10);
        } else if (code.equalsIgnoreCase("NO_SERVER")) {
            /*
             * 2020-03-26: Formerly known as "/alldebrid_server_not_allowed.txt" --> "Servers are not allowed to use this feature" --> Retry
             * later
             */
            mhm.handleErrorGeneric(account, link, message, 50);
        } else if (code.equalsIgnoreCase("LINK_HOST_NOT_SUPPORTED")) {
            if (isSelfhosted) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            }
        } else if (code.equalsIgnoreCase("LINK_TEMPORARY_UNAVAILABLE")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 30 * 1000l);
        } else if (code.equalsIgnoreCase("LINK_DOWN")) {
            /* URL is offline according to multihoster --> Do not trust this error --> Skip to next download candidate instead! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (code.equalsIgnoreCase("LINK_PASS_PROTECTED")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "URL is password protected", 5 * 60 * 1000l);
        } else if (code.equalsIgnoreCase("LINK_HOST_UNAVAILABLE")) {
            /* Host under maintenance or not available */
            mhm.putError(account, link, 5 * 60 * 1000l, message);
        } else if (code.equalsIgnoreCase("LINK_HOST_LIMIT_REACHED")) {
            mhm.putError(account, link, 5 * 60 * 1000l, message);
        } else if (code.equalsIgnoreCase("LINK_TOO_MANY_DOWNLOADS")) {
            /* Some hosts' simultaneous downloads are limited by this multihost - this may sometimes happen. */
            mhm.putError(account, link, 5 * 60 * 1000l, message);
        } else if (code.equalsIgnoreCase("LINK_HOST_FULL")) {
            /* API docs: 'All servers are full for this host please retry later' */
            mhm.putError(account, link, 5 * 60 * 1000l, message);
        } else if (code.equalsIgnoreCase("FREE_TRIAL_LIMIT_REACHED")) {
            /*
             * Traffic of free trial account is used-up --> Account is now basically a free account and free accounts are unsupported!
             */
            throw exceptionFreeAccountsAreNotSupported;
        } else if (code.equalsIgnoreCase("MUST_BE_PREMIUM")) {
            /* Single URL is not downloadable with current (free?) alldebrid account */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
        } else if (code.equalsIgnoreCase("PIN_ALREADY_AUTHED")) {
            /* "You already have a valid auth apikey" --> Not sure about this --> Temp. disable account */
            throw new AccountUnavailableException(message, 5 * 60 * 1000);
        } else if (code.equalsIgnoreCase("PIN_EXPIRED") || code.equalsIgnoreCase("PIN_INVALID")) {
            /* Do not use given errormessage here as it is irritating. */
            throw new AccountInvalidException("Invalid login!");
        } else {
            /*
             * Unknown/Generic error --> Assume it is a download issue but display it as temp. account issue if no DownloadLink is given.
             */
            /*
             * E.g. LINK_ERROR, LINK_IS_MISSING, STREAM_INVALID_GEN_ID, STREAM_INVALID_STREAM_ID, DELAYED_INVALID_ID, REDIRECTOR_XXX,
             * MAGNET_XXX, USER_LINK_INVALID, MISSING_NOTIF_ENDPOINT
             */
            logger.info("Unknown API error");
            message = "Unknown API error: " + message;
            if (link != null) {
                mhm.handleErrorGeneric(account, link, message, 50);
            } else {
                throw new AccountUnavailableException(message, 5 * 60 * 1000);
            }
        }
        return entries;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = getDirectLinkProperty(link, account);
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            dllink = this.generteFreshDirecturl(link, account);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, this.getMaxChunks(account, link));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl did not lead to downloadable content");
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    private static void setAuthHeader(final Browser br, final String auth) {
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + auth);
    }

    private String getDirectLinkProperty(final DownloadLink link, final Account account) {
        return this.getHost() + "directurl";
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final String directlinkproperty = getDirectLinkProperty(link, account);
        final String pawsProperty = getDirectLinkProperty(link, account) + "_paws";
        /* Try to re-use previously generated directurl */
        String dllink = checkDirectLink(account, link, directlinkproperty);
        if (!StringUtils.isEmpty(dllink)) {
            logger.info("Re-using stored directurl: " + dllink);
        } else {
            dllink = generteFreshDirecturl(link, account);
        }
        dllink = updateProtocolInDirecturl(dllink);
        final boolean useVerifiedFileSize;
        /* "paws" handling = old handling to disable setting verified filesize via API. */
        final boolean paws = link.getBooleanProperty(pawsProperty, false);
        if (!paws) {
            logger.info("don't use verified filesize because 'paws'!");
            useVerifiedFileSize = false;
        } else if (link.getVerifiedFileSize() > 0) {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            final URLConnectionAdapter check = br.openGetConnection(dllink);
            try {
                if (!looksLikeDownloadableContent(check)) {
                    brc.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
                } else if (check.getCompleteContentLength() < 0) {
                    logger.info("don't use verified filesize because complete content length isn't available:" + check.getCompleteContentLength() + "==" + link.getVerifiedFileSize());
                    useVerifiedFileSize = false;
                } else if (check.getCompleteContentLength() == link.getVerifiedFileSize()) {
                    logger.info("use verified filesize because it matches complete content length:" + check.getCompleteContentLength() + "==" + link.getVerifiedFileSize());
                    useVerifiedFileSize = true;
                } else {
                    if (link.getDownloadCurrent() > 0) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot resume different file:" + check.getCompleteContentLength() + "!=" + link.getVerifiedFileSize());
                    } else {
                        logger.info("don't use verified filesize because it doesn't match complete content length:" + check.getCompleteContentLength() + "!=" + link.getVerifiedFileSize());
                        useVerifiedFileSize = false;
                    }
                }
            } finally {
                check.disconnect();
            }
        } else {
            logger.info("use verified filesize");
            useVerifiedFileSize = true;
        }
        final String host = Browser.getHost(link.getDownloadURL());
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {
                return null;
            }

            @Override
            public long getVerifiedFileSize() {
                if (useVerifiedFileSize) {
                    return super.getVerifiedFileSize();
                } else {
                    return -1;
                }
            }

            @Override
            public String getHost() {
                final DownloadInterface dli = getDownloadInterface();
                if (dli != null) {
                    final URLConnectionAdapter connection = dli.getConnection();
                    if (connection != null) {
                        return connection.getURL().getHost();
                    }
                }
                return host;
            }
        };
        final int chunks = getMaxChunks(account, link);
        logger.info("Max allowed chunks: " + chunks);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLinkDownloadable, br.createGetRequest(dllink), true, chunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkRateLimit(br, dl.getConnection(), account, link);
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
            } else if (dl.getConnection().getResponseCode() == 416) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416 - try later or with less chunks");
            } else if (br.containsHTML("range not ok")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Range not ok");
            } else {
                /* unknown error */
                logger.severe("Error: Unknown Error");
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to a file", 50);
            }
        }
        dl.startDownload();
    }

    private String generteFreshDirecturl(final DownloadLink link, final Account account) throws Exception {
        logger.info("Generating fresh directurl");
        this.login(account, new AccountInfo(), false);
        String downloadPassword = link.getDownloadPassword();
        Form dlform = new Form();
        dlform.setMethod(MethodType.GET);
        dlform.setAction(api_base + "/link/unlock");
        final String url;
        final boolean isSelfhosted = isSelfhosted(link);
        if (isSelfhosted) {
            url = link.getPluginPatternMatcher();
        } else {
            url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
        }
        dlform.put("link", Encoding.urlEncode(url));
        dlform.put("agent", agent_raw);
        if (!StringUtils.isEmpty(downloadPassword)) {
            dlform.put("password", Encoding.urlEncode(downloadPassword));
        }
        int counter = 0;
        final String pwprotectedErrorString = "LINK_PASS_PROTECTED";
        do {
            counter++;
            if (counter > 1) {
                downloadPassword = getUserInput("Password?", link);
                dlform.put("password", Encoding.urlEncode(downloadPassword));
            }
            br.submitForm(dlform);
            try {
                handleErrors(account, link);
            } catch (final PluginException e) {
                if (br.containsHTML(pwprotectedErrorString)) {
                    /*
                     * Stored password was wrong or this was the first attempt and we didn't know the item was password protected so now we
                     * know we need to ask the user to enter a download password.
                     */
                    counter++;
                    continue;
                } else {
                    throw e;
                }
            }
            logger.info("Breaking loop because: User entered correct password or none was needed");
            break;
        } while (counter <= 3);
        if (!StringUtils.isEmpty(downloadPassword)) {
            /* Store password e.g. for the next try */
            link.setDownloadPassword(downloadPassword);
        }
        final Map<String, Object> entries = handleErrors(account, link);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        if (isSelfhosted) {
            link.setFinalFileName(data.get("filename").toString());
            link.setVerifiedFileSize(((Number) data.get("filesize")).longValue());
        }
        // TODO: Use json parser here
        final String delayID = PluginJSonUtils.getJson(br, "delayed");
        if (!StringUtils.isEmpty(delayID)) {
            /* See https://docs.alldebrid.com/v4/#delayed-links */
            if (!cacheDLChecker(delayID)) {
                /* Error or serverside download not finished in given time. */
                logger.info("Delayed handling failure");
                handleErrors(account, link);
                mhm.handleErrorGeneric(account, link, "Serverside download failure", 50, 5 * 60 * 1000l);
            } else {
                logger.info("Delayed handling success");
            }
        }
        link.setProperty(PROPERTY_maxchunks, data.get("max_chunks"));
        link.setProperty(getDirectLinkProperty(link, account) + "_paws", data.get("paws"));
        final String dllink = (String) data.get("link");
        if (StringUtils.isEmpty(dllink) || !dllink.matches("https?://.+")) {
            /* This should never happen */
            mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 1 * 60 * 1000l);
        }
        link.setProperty(getDirectLinkProperty(link, account), dllink);
        return dllink;
    }

    private String updateProtocolInDirecturl(String dllink) throws MalformedURLException {
        if (!PluginJsonConfig.get(AlldebridComConfig.class).isUseHTTPSForDownloads()) {
            // https://svn.jdownloader.org/issues/87886
            final URL url = new URL(dllink);
            if (StringUtils.equals("https", url.getProtocol())) {
                logger.info("https for final downloadurls is disabled:" + dllink);
                String newDllink = dllink.replaceFirst("https://", "http://");
                if (url.getPort() != -1) {
                    // remove custom https port
                    newDllink = dllink.replace(":" + url.getPort() + "/", "/");
                }
                /* Check if link has changed */
                if (!newDllink.equals(dllink)) {
                    logger.info("New final downloadurl: " + newDllink);
                    dllink = newDllink;
                }
            }
        }
        return dllink;
    }

    /* Stolen from LinkSnappyCom plugin */
    private boolean cacheDLChecker(final String delayID) throws Exception {
        final PluginProgress waitProgress = new PluginProgress(0, 100, null) {
            protected long lastCurrent    = -1;
            protected long lastTotal      = -1;
            protected long startTimeStamp = -1;

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.WAIT;
            }

            @Override
            public String getMessage(Object requestor) {
                if (requestor instanceof ETAColumn) {
                    final long eta = getETA();
                    if (eta >= 0) {
                        return TimeFormatter.formatMilliSeconds(eta, 0);
                    }
                    return "";
                }
                return "Preparing your delayed file";
            }

            @Override
            public void updateValues(long current, long total) {
                super.updateValues(current, total);
                if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
                    lastTotal = total;
                    lastCurrent = current;
                    startTimeStamp = System.currentTimeMillis();
                    // this.setETA(-1);
                    return;
                }
                long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                if (currentTimeDifference <= 0) {
                    return;
                }
                long speed = (current * 10000) / currentTimeDifference;
                if (speed == 0) {
                    return;
                }
                long eta = ((total - current) * 10000) / speed;
                this.setETA(eta);
            }
        };
        waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        waitProgress.setProgressSource(this);
        int lastProgress = -1;
        try {
            /* See https://docs.alldebrid.com/v4/#delayed-links */
            Form dlform = new Form();
            dlform.setMethod(MethodType.GET);
            dlform.setAction(api_base + "/link/delayed");
            dlform.put("id", delayID);
            dlform.put("agent", agent_raw);
            final int maxWaitSeconds = 300;
            /* 2020-03-27: API docs say checking every 5 seconds is recommended */
            final int waitSecondsPerLoop = 5;
            int waitSecondsLeft = maxWaitSeconds;
            /* 1 = still processing, 2 = Download link available, 3 = Error */
            int delayedStatus = 1;
            Integer currentProgress = 0;
            do {
                logger.info(String.format("Waiting for file to get loaded onto server - seconds left %d / %d", waitSecondsLeft, maxWaitSeconds));
                this.getDownloadLink().addPluginProgress(waitProgress);
                waitProgress.updateValues(currentProgress.intValue(), 100);
                for (int sleepRound = 0; sleepRound < waitSecondsPerLoop; sleepRound++) {
                    if (isAbort()) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else {
                        Thread.sleep(1000);
                    }
                }
                if (currentProgress.intValue() != lastProgress) {
                    // lastProgressChange = System.currentTimeMillis();
                    lastProgress = currentProgress.intValue();
                }
                br.submitForm(dlform);
                try {
                    /* We have to use the parser here because json contains two 'status' objects ;) */
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                    delayedStatus = (int) JavaScriptEngineFactory.toLong(data.get("status"), 3);
                    final int tmpCurrentProgress = (int) ((Number) data.get("progress")).doubleValue() * 100;
                    if (tmpCurrentProgress > currentProgress) {
                        /* Do not allow the progress to "go back". */
                        currentProgress = tmpCurrentProgress;
                    }
                    /* 2020-03-27: We cannot trust their 'time_left' :D */
                    // final int timeLeft = (int) JavaScriptEngineFactory.toLong(entries.get("time_left"), 30);
                    if (currentProgress >= 100) {
                        /* 2020-03-27: Do not trust their 100% :D */
                        currentProgress = 85;
                    }
                } catch (final Throwable e) {
                    logger.info("Error parsing json response");
                    logger.log(e);
                    break;
                }
                waitSecondsLeft -= waitSecondsPerLoop;
            } while (waitSecondsLeft > 0 && delayedStatus == 1);
            if (delayedStatus == 2) {
                return true;
            } else {
                return false;
            }
        } finally {
            this.getDownloadLink().removePluginProgress(waitProgress);
        }
    }

    private String checkDirectLink(final Account account, final DownloadLink link, final String property) {
        final String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                // HEAD requests are causing issues serverside, headers are missing in combination with keepalive
                con = br2.openGetConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    br2.followConnection(true);
                    checkRateLimit(br2, con, account, link);
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                link.setProperty(property + "_paws", Property.NULL);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            return null;
        }
    }

    private int getMaxChunks(final Account account, final DownloadLink link) {
        /* 2020-04-12: Chunks limited to 16 RE: admin */
        final int defaultMaxChunks = -16;
        int chunks = 1;
        if (link.hasProperty(PROPERTY_maxchunks)) {
            chunks = link.getIntegerProperty(PROPERTY_maxchunks, defaultMaxChunks);
            if (chunks <= 0) {
                chunks = 1;
            } else if (chunks > 1) {
                chunks = -chunks;
            }
        } else {
            /* Default */
            chunks = defaultMaxChunks;
        }
        synchronized (RATE_LIMITED) {
            final HashSet<String> set = RATE_LIMITED.get(account);
            if (set != null && set.contains(link.getHost())) {
                chunks = 1;
            }
        }
        return chunks;
    }

    private static WeakHashMap<Account, HashSet<String>> RATE_LIMITED = new WeakHashMap<Account, HashSet<String>>();

    private void checkRateLimit(Browser br, URLConnectionAdapter con, final Account account, final DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML("rate limiting, please retry") || con.getResponseCode() == 429) {
            Browser.setRequestIntervalLimitGlobal(br.getHost(), 2000);
            synchronized (RATE_LIMITED) {
                HashSet<String> set = RATE_LIMITED.get(account);
                if (set == null) {
                    set = new HashSet<String>();
                    RATE_LIMITED.put(account, set);
                }
                set.add(downloadLink.getHost());
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many requests:" + br.getHost());
        }
    }

    public static String getStoredApiKey(final Account account) {
        return account.getStringProperty(PROPERTY_apikey);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* Account needed to check such links. */
            return AvailableStatus.UNCHECKABLE;
        } else {
            if (!link.isNameSet()) {
                link.setName(this.getFID(link));
            }
            generteFreshDirecturl(link, account);
            /* No exception = File is online */
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return AlldebridComConfig.class;
    }

    public static interface AlldebridComConfig extends PluginConfigInterface {
        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        public static class TRANSLATION {
            public String getUseHTTPSForDownloads_label() {
                return "Use https for final downloadurls?";
            }
        }

        @AboutConfig
        @DefaultBooleanValue(true)
        boolean isUseHTTPSForDownloads();

        void setUseHTTPSForDownloads(boolean b);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}