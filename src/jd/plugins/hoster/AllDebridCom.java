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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

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
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alldebrid.com" }, urls = { "" })
public class AllDebridCom extends antiDDoSForHost {
    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        /* 2020-03-27: As long as we're below 4 API requests per second we're fine according to admin. */
        setStartIntervall(1000);
        this.enablePremium("http://www.alldebrid.com/offer/");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void extendDownloadsTableContextMenu(JComponent parent, PluginView<DownloadLink> pv, Collection<PluginView<DownloadLink>> views) {
        if (pv.size() == 1) {
            final JMenuItem changeURLMenuItem = createChangeURLMenuItem(pv.get(0));
            if (changeURLMenuItem != null) {
                parent.add(changeURLMenuItem);
            }
        }
    }

    private static MultiHosterManagement mhm                               = new MultiHosterManagement("alldebrid.com");
    private static String                api_base                          = "https://api.alldebrid.com/v4";
    // this is used by provider which calculates unique token to agent/client.
    private static final String          agent                             = "agent=JDownloader";
    private static final String          agent_raw                         = "JDownloader";
    private static final String          PROPERTY_APIKEY_CREATED_TIMESTAMP = "APIKEY_CREATED_TIMESTAMP";
    /* New APIv4 apikey --> Replaces old token */
    private static final String          PROPERTY_apikey                   = "apiv4_apikey";
    /* Property of old Deprecated APIv3. Only required to migrate users' accounts which were logged-in V3 to V4. */
    private static final String          PROPERTY_old_token                = "token";

    public String fetchApikey(final Account account, final AccountInfo accountInfo) throws Exception {
        synchronized (account) {
            try {
                String apikey = account.getStringProperty(PROPERTY_apikey, null);
                final String old_token = account.getStringProperty(PROPERTY_old_token, null);
                if (apikey == null && old_token != null) {
                    /* 2020-03-27: TODO: Remove this 4-8 weeks after release of plugin with APIv4 */
                    logger.info("Trying to migrate account from APIv3 --> APIv4");
                    getPage(api_base + "/migrate?" + agent + "&token=" + Encoding.urlEncode(old_token));
                    /* Example good response: {"status": "success","data": {"apikey": "Newv4Apikey"}} */
                    /* Example BAD response (no json but plaintext): "Bad token" (without "") --> This should never happen */
                    apikey = PluginJSonUtils.getJson(br, "apikey");
                    if (!StringUtils.isEmpty(apikey)) {
                        logger.info("Migration successful");
                        account.setProperty(PROPERTY_apikey, apikey);
                        /* TODO: Is this a good idea? There will be no way back! */
                        // account.removeProperty(PROPERTY_old_token);
                    } else {
                        logger.warning("Migration failed");
                    }
                } else if (old_token != null) {
                    /* TODO: Remove this once we delete the old token on migration. */
                    logger.info("This account has been upgraded from APIv3 to APIv4 in the past");
                }
                if (apikey != null) {
                    try {
                        loginAccount(account, accountInfo, apikey);
                        logger.info("Apikey login successful");
                        return apikey;
                    } catch (final PluginException elogin) {
                        final String error = parseError();
                        if ("AUTH_BAD_APIKEY".equalsIgnoreCase(error)) {
                            logger.info("Apikey login failed");
                            /* Perform full login */
                        } else {
                            throw elogin;
                        }
                    }
                }
                /* Full login */
                logger.info("Performing full login");
                if (apikey == null && old_token != null) {
                    /* Only display this message to users with pre-existing but invalid login-token/apikey */
                    showMigrationToNewAPIInformation();
                }
                getPage(api_base + "/pin/get?" + agent);
                final String user_url = PluginJSonUtils.getJson(br, "user_url");
                final String check_url = PluginJSonUtils.getJson(br, "check_url");
                if (StringUtils.isEmpty(user_url) || StringUtils.isEmpty(check_url)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Thread dialog = showPINLoginInformation(user_url);
                try {
                    for (int i = 0; i <= 23; i++) {
                        logger.info("Waiting for user to authorize application: " + i);
                        Thread.sleep(5000);
                        getPage(check_url);
                        final String error = parseError();
                        if (error != null) {
                            /* Something went wrong */
                            break;
                        } else {
                            apikey = PluginJSonUtils.getJson(br, "apikey");
                            if (!StringUtils.isEmpty(apikey)) {
                                break;
                            } else if (!dialog.isAlive()) {
                                logger.info("Dialog closed!");
                                break;
                            }
                        }
                    }
                } finally {
                    dialog.interrupt();
                }
                if (StringUtils.isEmpty(apikey)) {
                    throw new AccountInvalidException("User failed to authorize PIN/Code. Do not close the pairing dialog until you have confirmed the PIN/Code via browser!");
                }
                /* Save this property - it might be useful in the future. */
                account.setProperty(PROPERTY_APIKEY_CREATED_TIMESTAMP, System.currentTimeMillis());
                loginAccount(account, accountInfo, apikey);
                this.setAuthHeader(br, apikey);
                return apikey;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(PROPERTY_apikey);
                }
                throw e;
            }
        }
    }

    private void loginAccount(final Account account, final AccountInfo ai, final String apikey) throws Exception {
        synchronized (account) {
            this.setAuthHeader(br, apikey);
            getPage(api_base + "/user?" + agent);
            handleErrors(account, null);
            final String userName = PluginJSonUtils.getJson(br, "username");
            if (userName != null && userName.length() > 2) {
                // don't store the complete username
                final String shortuserName = userName.substring(0, userName.length() / 2) + "****";
                account.setUser(shortuserName);
            }
            /* Do not store any old website login credentials! */
            account.setPass(null);
            final boolean isPremium = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(br, "isPremium"));
            final boolean isTrial = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(br, "isTrial"));
            if (!isPremium) {
                /*
                 * Real free account (or expired trial premium [= user downloaded more than 25GB trial quota]) --> Cannot download and
                 * cannot even login via API officially!
                 */
                freeAccountsAreUnsupported();
            } else {
                final String premiumUntilStr = PluginJSonUtils.getJson(br, "premiumUntil");
                final long premiumUntil = Long.parseLong(premiumUntilStr) * 1000l;
                if (ai != null) {
                    ai.setValidUntil(premiumUntil, br);
                }
                /* Only save apikey for premium accounts */
                if (premiumUntil < System.currentTimeMillis()) {
                    throw new AccountInvalidException("Premium expired!");
                }
                account.setProperty(PROPERTY_apikey, apikey);
                if (isTrial) {
                    /*
                     * 2020-03-27: Premium "test" accounts which last 7 days and have a total of 25GB as quota. Once that limit is reached,
                     * they can only download from "Free" hosts (only a hand full of hosts).
                     */
                    ai.setStatus("Premium Account (Free trial, reverts to free once traffic is used up)");
                    /* 2020-03-27: Hardcoded */
                    final long maxTraffic = SizeFormatter.getSize("25GB");
                    final long remainingTraffic = Long.parseLong(PluginJSonUtils.getJson(br, "remainingTrialQuota")) * 1000 * 1000;
                    ai.setTrafficLeft(remainingTraffic);
                    if (remainingTraffic <= remainingTraffic) {
                        ai.setTrafficMax(maxTraffic);
                    }
                } else {
                    /* "Real" premium account. */
                    ai.setStatus("Premium Account");
                }
                account.setType(AccountType.PREMIUM);
            }
        }
    }

    private void freeAccountsAreUnsupported() throws AccountInvalidException {
        throw new AccountInvalidException("Free accounts are not supported!");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo accountInfo = new AccountInfo();
        fetchApikey(account, accountInfo);
        /* They got 3 arrays of types of supported websites --> We want to have the "hosts" Array only! */
        getPage(api_base + "/user/hosts?" + agent + "&hostsOnly=true");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/hosts");
        final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        while (iterator.hasNext()) {
            try {
                final Entry<String, Object> hostO = iterator.next();
                final LinkedHashMap<String, Object> entry = (LinkedHashMap<String, Object>) hostO.getValue();
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
                    final ArrayList<String> domains = (ArrayList<String>) entry.get("domains");
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

    private Thread showPINLoginInformation(final String pin_url) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Alldebrid.com - Login";
                        message += "Hallo liebe(r) alldebrid NutzerIn\r\n";
                        message += "Um deinen Alldebrid Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "2. Bestätige die PIN/Code im Browser.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Alldebrid.com - Login";
                        message += "Hello dear alldebrid user\r\n";
                        message += "In order to use this service in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "3. Confirm the PIN/Code you see in the browser window.\r\n";
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

    /* 2020-03-25: TODO: Remove this dialog 2020-06 */
    private Thread showMigrationToNewAPIInformation() throws InterruptedException {
        final boolean displayAPIMigrationMessage = true;
        final String key_api_migration_information = "api_migration_2020_03_25";
        final boolean msg_was_displayed_to_user_already = this.getPluginConfig().getBooleanProperty(key_api_migration_information, false);
        if (!displayAPIMigrationMessage || msg_was_displayed_to_user_already) {
            logger.info("API migration information message has already been shown to user");
            return null;
        }
        logger.info("Displaying API migration information message");
        final int max_wait_seconds = 120;
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Alldebrid.com - neue API Version APIv4";
                        message += "Hallo liebe(r) alldebrid NutzerIn\r\n";
                        message += "Seit diesem Update hat sich die von uns verwendete API Version dieses Anbieters von v3 auf v4 geändert.\r\n";
                        message += "Du wurdest u.U. deshalb automatisch ausgeloggt.\r\n";
                        message += "Logge dich erneut ein, um deinen alldebrid Account weiterhin in JDownloader verwenden zu können.\r\n";
                        message += "Bitte entschuldige die Unannehmlichkeiten.\r\n";
                    } else {
                        title = "Alldebrid.com - New API Version APIv4";
                        message += "Hello dear alldebrid user\r\n";
                        message += "With this update we are changing the used alldebrid API version from V3 to V4.\r\n";
                        message += "You might have just been automatically logged-out because of this.\r\n";
                        message += "Simply re-login to continue using your alldebrid account in JDownloader.\r\n";
                        message += "Sorry for the trouble!\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(max_wait_seconds * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        /* Save this property so user will only see this dialog once! */
        this.getPluginConfig().setProperty(key_api_migration_information, true);
        /* Lave the user some time to read this dialog because another one will open soon to login! */
        for (int i = 0; i < max_wait_seconds; i++) {
            Thread.sleep(1000l);
            if (!thread.isAlive()) {
                /* Continue once user has closed the dialog */
                break;
            }
        }
        return thread;
    }

    private String parseError() {
        final String status = PluginJSonUtils.getJson(br, "status");
        if ("error".equalsIgnoreCase(status)) {
            return PluginJSonUtils.getJson(br, "code");
        }
        return null;
    }

    /* See https://docs.alldebrid.com/v4/#all-errors */
    private void handleErrors(final Account account, final DownloadLink link) throws PluginException, Exception {
        /* 2020-03-25: E.g. {"status": "error", "error": {"code": "AUTH_BAD_APIKEY","message": "The auth apikey is invalid"}} */
        final String status = PluginJSonUtils.getJson(br, "status");
        final String code = PluginJSonUtils.getJson(br, "code");
        String message = PluginJSonUtils.getJson(br, "message");
        if ("error".equalsIgnoreCase(status)) {
            if (StringUtils.isEmpty(message)) {
                /* We always want to have a human readable errormessage */
                message = "Unknown error";
            }
            if (code.equalsIgnoreCase("AUTH_BAD_APIKEY")) {
                /* Do not use given errormessage here as it is irritating. */
                if (link != null) {
                    /*
                     * If this happens during download attempt, temp. disable account for a very short time so next check will trigger a
                     * full login.
                     */
                    throw new AccountUnavailableException("Session expired: " + message, 10 * 60 * 1000);
                } else {
                    throw new AccountInvalidException("Invalid login: " + message);
                }
            } else if (code.equalsIgnoreCase("AUTH_BLOCKED")) {
                /* Apikey GEO-blocked or IP blocked */
                throw new AccountUnavailableException(message, 10 * 60 * 1000);
            } else if (code.equalsIgnoreCase("AUTH_USER_BANNED")) {
                throw new AccountInvalidException(message);
            } else if (code.equalsIgnoreCase("DELAYED_INVALID_ID")) {
                /* This can only happen in '/link/delayed' handling */
                mhm.handleErrorGeneric(account, link, message, 10);
            } else if (code.equalsIgnoreCase("NO_SERVER")) {
                /*
                 * 2020-03-26: Formerly known as "/alldebrid_server_not_allowed.txt" --> "Servers are not allowed to use this feature" -->
                 * Retry later
                 */
                mhm.handleErrorGeneric(account, link, message, 50);
            } else if (code.equalsIgnoreCase("LINK_HOST_NOT_SUPPORTED")) {
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            } else if (code.equalsIgnoreCase("LINK_DOWN")) {
                /* URL is offline according to multihoster --> Do not trust this error --> Skip to next download candidate instead! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted offline error", 5 * 60 * 1000l);
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
                freeAccountsAreUnsupported();
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
                 * Unknown/Generic error --> Assume it is a download issue but display it as temp. account issue if no DownloadLink is
                 * given.
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
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.alldebrid.com/tos/";
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void setAuthHeader(final Browser br, final String auth) {
        br.getHeaders().put("Authorization", "Bearer " + auth);
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final String directlinkproperty = this.getHost() + "directurl";
        /* Try to re-use previously generated directurl */
        String dllink = checkDirectLink(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            showMessage(link, "Phase 1/2: Generating link");
            synchronized (account) {
                final String apikey = loadApikey(account);
                setAuthHeader(this.br, apikey);
                String downloadPassword = link.getDownloadPassword();
                Form dlform = new Form();
                dlform.setMethod(MethodType.GET);
                dlform.setAction(api_base + "/link/unlock");
                dlform.put("link", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
                dlform.put("agent", agent_raw);
                if (!StringUtils.isEmpty(downloadPassword)) {
                    dlform.put("password", Encoding.urlEncode(downloadPassword));
                }
                int counter = 0;
                String error = null;
                do {
                    this.submitForm(dlform);
                    error = this.parseError();
                    if ("LINK_PASS_PROTECTED".equalsIgnoreCase(error)) {
                        /* Stored password was wrong or first attempt so now we know we need a download password. */
                        /* {"error":"Link is password protected","errorCode":38} */
                        downloadPassword = getUserInput("Password?", link);
                        dlform.put("password", Encoding.urlEncode(downloadPassword));
                    }
                    counter++;
                } while ("LINK_PASS_PROTECTED".equalsIgnoreCase(error) && counter <= 2);
                handleErrors(account, link);
                if (!StringUtils.isEmpty(downloadPassword)) {
                    /* Store password e.g. for the next try */
                    link.setDownloadPassword(downloadPassword);
                }
            }
            final String delayID = PluginJSonUtils.getJson(br, "delayed");
            if (!StringUtils.isEmpty(delayID)) {
                /* See https://docs.alldebrid.com/v4/#delayed-links */
                if (!cacheDLChecker(delayID)) {
                    /* Error or serverside download not finished in given time. */
                    logger.info("Delayed handling failure");
                    handleErrors(account, link);
                    mhm.handleErrorGeneric(account, link, "serverside_download_failure", 50, 5 * 60 * 1000l);
                } else {
                    logger.info("Delayed handling success");
                }
            }
            try {
                /* We need the parser as some URLs may have streams available with multiple qualities and multiple downloadurls */
                // dllink = PluginJSonUtils.getJsonValue(br, "link");
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("data");
                dllink = (String) entries.get("link");
            } catch (final Throwable e) {
                logger.log(e);
            }
            if (dllink == null || !dllink.matches("https?://.+")) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 1 * 60 * 1000l);
            }
            // final String filename = PluginJSonUtils.getJsonValue(br, "filename");
            link.setProperty(directlinkproperty, dllink);
        }
        handleDL(account, link, dllink);
    }

    /* Stolen from LinkSnappyCom */
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
                this.submitForm(dlform);
                try {
                    /* We have to use the parser here because json contains two 'status' objects ;) */
                    LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    entries = (LinkedHashMap<String, Object>) entries.get("data");
                    delayedStatus = (int) JavaScriptEngineFactory.toLong(entries.get("status"), 3);
                    final int tmpCurrentProgress = (int) ((Number) entries.get("progress")).doubleValue() * 100;
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
                    delayedStatus = 3;
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, String genlink) throws Exception {
        if (genlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Task 2: Download begins!");
        final boolean useVerifiedFileSize;
        if (br != null && PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(br, "paws"))) {
            logger.info("don't use verified filesize because 'paws'!");
            useVerifiedFileSize = false;
        } else if (link.getVerifiedFileSize() > 0) {
            final Browser brc = br.cloneBrowser();
            final URLConnectionAdapter check = openAntiDDoSRequestConnection(brc, brc.createGetRequest(genlink));
            try {
                if (check.getCompleteContentLength() < 0) {
                    logger.info("don't use verified filesize because complete content length isn't available!");
                    useVerifiedFileSize = false;
                } else if (check.getCompleteContentLength() == link.getVerifiedFileSize()) {
                    logger.info("use verified filesize because it matches complete content length");
                    useVerifiedFileSize = true;
                } else {
                    if (link.getDownloadCurrent() > 0) {
                        logger.info("cannot resume different file!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        logger.info("don't use verified filesize because it doesn't match complete content length!");
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
        if (!PluginJsonConfig.get(AlldebridComConfig.class).isUseHTTPSForDownloads()) {
            // https://svn.jdownloader.org/issues/87886
            final URL url = new URL(genlink);
            if (StringUtils.equals("https", url.getProtocol())) {
                logger.info("https for final downloadurls is disabled:" + genlink);
                genlink = genlink.replace("https://", "http://");
                if (url.getPort() != -1) {
                    // remove custom https port
                    genlink = genlink.replace(":" + url.getPort() + "/", "/");
                }
                logger.info("New final downloadurl: " + genlink);
            }
        }
        /* 2020-04-12: Chunks limited to 16 RE: admin */
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLinkDownloadable, br.createGetRequest(genlink), true, -16);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("range not ok")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            /* unknown error */
            logger.severe("Error: Unknown Error");
            // disable hoster for 5min
            mhm.putError(account, link, 5 * 60 * 1000l, "Final downloadurl does not lead to a file");
        }
        dl.startDownload();
    }

    private String loadApikey(final Account account) throws Exception {
        synchronized (account) {
            String ret = account.getStringProperty(PROPERTY_apikey, null);
            if (ret == null) {
                ret = fetchApikey(account, null);
                if (ret == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    return ret;
                }
            } else {
                return ret;
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            // define custom browser headers and language settings.
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
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