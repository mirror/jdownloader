package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.ProleechLinkConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "proleech.link" }, urls = { "" })
public class ProLeechLink extends antiDDoSForHost {
    public ProLeechLink(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://proleech.link/signup");
        if (useAPIOnly()) {
            /* 2020-06-04: API allows more requests in a short time than website does */
            /* 2020-06-05: API does not have rate limits at all (RE: admin) */
            // this.setStartIntervall(1000l);
        } else {
            /* 2020-05-29: Try to avoid <div class="alert danger"><b>Too many requests! Please try again in a few seconds.</b></div> */
            this.setStartIntervall(5000l);
        }
    }

    private static MultiHosterManagement        mhm                                                               = new MultiHosterManagement("proleech.link");
    /** Contains all filenames of files we attempted to download or downloaded via this account. */
    private static CopyOnWriteArrayList<String> deleteDownloadHistoryFilenameWhitelist                            = new CopyOnWriteArrayList<String>();
    // private static List<String> deleteDownloadHistoryFilenameBlacklist = new ArrayList<String>();
    private final String                        API_BASE                                                          = "https://proleech.link/dl/debrid/deb_api.php";
    private final String                        PROPERTY_MAXCHUNKS                                                = "proleechlink_maxchunks";
    private final String                        PROPERTY_PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION = "PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION";
    private final String                        PROPERTY_ACCOUNT_apiuser                                          = "apiuser";
    private final String                        PROPERTY_ACCOUNT_apikey                                           = "apikey";
    private final String                        PROPERTY_ACCOUNT_api_login_dialog_shown                           = "api_login_dialog_shown";
    private final String                        PROPERTY_ACCOUNT_api_usage_hint_dialog_shown                      = "api_usage_hint_dialog_shown";

    @Override
    public String getAGBLink() {
        return "https://proleech.link/page/terms";
    }

    private boolean useAPIOnly() {
        return PluginJsonConfig.get(this.getConfigInterface()).isEnableAPI();
    }

    private boolean useAPILoginWorkaround() {
        return false;
    }

    private Browser prepBrAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    /**
     * Allow to try to use API to generate downloadlinks in WEBSITE mode if accountcheck finds api username and apikey? Handling will
     * fallback to website on failure!
     */
    private boolean tryAPIForDownloadingInWebsiteMode() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (this.useAPIOnly()) {
            return this.fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /* This will also remove timestamp so auto-switch again from website to API will be possible without issues! */
        account.clearCookies("api");
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, ai, true);
        /* Contains all filehosts available for free account users regardless of their status */
        String[] filehosts_free = null;
        /* Contains all filehosts available for premium users and listed as online/working */
        List<String> filehosts_premium_onlineArray = new ArrayList<String>();
        /* Contains all filehosts available for premium users and listed as online/working */
        List<String> filehosts_free_onlineArray = new ArrayList<String>();
        String traffic_max_dailyStr = null;
        {
            /* Grab free hosts */
            if (br.getURL() == null || !br.getURL().contains("/downloader")) {
                this.getPage("/downloader");
            }
            final String html_free_filehosts = br.getRegex("<section id=\"content\">.*?Free Filehosters</div>.*?</section>").getMatch(-1);
            filehosts_free = new Regex(html_free_filehosts, "domain=([^\"]+)").getColumn(0);
        }
        {
            /* Grab premium hosts */
            getPage("/page/hostlist");
            filehosts_premium_onlineArray = this.regexPremiumHostsOnlineWebsite(br);
            traffic_max_dailyStr = this.regexMaxDailyTrafficWebsite(br);
        }
        /* Set supported hosts depending on account type */
        if (account.getType() == AccountType.PREMIUM && !ai.isExpired()) {
            /* Premium account - bigger list of supported hosts */
            ai.setMultiHostSupport(this, filehosts_premium_onlineArray);
            if (traffic_max_dailyStr != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(traffic_max_dailyStr));
            }
        } else {
            /* Free & Expired[=Free] accounts - they support much less hosts */
            if (filehosts_free != null && filehosts_free.length != 0) {
                /*
                 * We only see the online status of each host in the big list. Now we have to find out which of all free filehosts are
                 * currently online/supported.
                 */
                for (final String host : filehosts_free) {
                    if (filehosts_premium_onlineArray.contains(host)) {
                        /* Filehost should be supported/online --> Add to the list we will later set */
                        filehosts_free_onlineArray.add(host);
                    }
                }
                ai.setMultiHostSupport(this, filehosts_free_onlineArray);
            }
        }
        /* Clear download history on every accountcheck (if selected by user) */
        clearDownloadHistoryWebsite(account, true);
        /* 2020-06-04: Workaround: Save apikey to try to use API for downloading */
        this.getPage("/jdownloader");
        final String apiuser = br.getRegex("(?i)<h4>\\s*API Username\\s*:\\s*</h4>\\s*<p>([^<>\"]+)</p>").getMatch(0);
        final String apikey = br.getRegex("class=\"apipass\"[^>]*>([a-z0-9]+)<").getMatch(0);
        String accstatus = ai.getStatus();
        if (accstatus == null) {
            accstatus = "";
        }
        if (apiuser != null && this.isAPIKey(apikey)) {
            logger.info(String.format("Successfully found apikey and user: %s:%s", apiuser, apikey));
            account.setProperty(PROPERTY_ACCOUNT_apiuser, apiuser);
            account.setProperty(PROPERTY_ACCOUNT_apikey, apikey);
            accstatus += " (Using API for download requests)";
        } else {
            logger.info("Failed to find apikey");
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                accstatus += " (Using Website only)";
            }
        }
        ai.setStatus(accstatus);
        return ai;
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        loginAPI(account, true);
        final AccountInfo ai = new AccountInfo();
        List<String> filehosts_premium_onlineArray = new ArrayList<String>();
        String trafficmaxDailyStr = null;
        if (useAPILoginWorkaround()) {
            /* 2020-06-04: Only premium users can get/see their apikey on the proleech website */
            account.setType(AccountType.PREMIUM);
            /* Get list of supported hosts from website */
            try {
                getPage("/page/hostlist");
                filehosts_premium_onlineArray = regexPremiumHostsOnlineWebsite(br);
                trafficmaxDailyStr = this.regexMaxDailyTrafficWebsite(br);
            } catch (final Exception e) {
                logger.log(e);
                logger.info("Failed to fetch list of supported hosts from website");
            }
            ai.setUnlimitedTraffic();
        } else {
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            /* Small workaround: Use website to find daily max traffic value as API doesn't provide that information. */
            try {
                final Browser brc = br.cloneBrowser();
                this.getPage(brc, "/page/hostlist");
                trafficmaxDailyStr = this.regexMaxDailyTrafficWebsite(brc);
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
            if (trafficmaxDailyStr == null) {
                /* 2022-19-08: Fallback: Static value taken from: https://proleech.link/page/hostlist */
                /* 2023-09-03: asked by admin to increase to 200GB */
                trafficmaxDailyStr = "200 GB";
            }
            final Object premiumO = entries.get("premium");
            final String premiumStatusStr = premiumO != null ? premiumO.toString() : null;
            String expiredate = (String) entries.get("subscriptions_date");
            if (StringUtils.equalsIgnoreCase(premiumStatusStr, "true") || StringUtils.equalsIgnoreCase(premiumStatusStr, "yes")) {
                account.setType(AccountType.PREMIUM);
                if (!StringUtils.isEmpty(expiredate) && expiredate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    /* 2020-06-04: Should expire at the end of the last day */
                    expiredate += " 23:59:59";
                    /* Use some less milliseconds to avoid displaying "one day too much". */
                    final long aLittleBitLess = 60000l;
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) - aLittleBitLess, br);
                }
                final long trafficmaxDaily = SizeFormatter.getSize(trafficmaxDailyStr);
                /* 2022-11-03: Do not use "traffic_left" as this field is always 0(?) */
                // final Number traffic_left = (Number) entries.get("traffic_left");
                final Object trafficusedTodayO = entries.get("used_today");
                if (trafficusedTodayO instanceof String) {
                    ai.setTrafficLeft(trafficmaxDaily - SizeFormatter.getSize(trafficusedTodayO.toString()));
                } else if (trafficusedTodayO instanceof Number) {
                    ai.setTrafficLeft(trafficmaxDaily - ((Number) trafficusedTodayO).longValue());
                }
                ai.setTrafficMax(trafficmaxDaily);
            } else {
                /* 2020-06-04: Only premium users can see their apikey so if this happens, we probably have an expired premium account. */
                account.setType(AccountType.FREE);
                /* 2020-06-08: Free account downloads are impossible via API, some are possible via website */
                ai.setTrafficLeft(0);
                ai.setTrafficMax(trafficmaxDailyStr);
            }
        }
        /* Host specific traffic limits would be here: http://proleech.link/dl/debrid/deb_api.php?limits */
        this.getPage(API_BASE + "?hosts");
        final String[] supportedhostsAPI = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.STRING_ARRAY);
        for (final String filehost_premium_online : supportedhostsAPI) {
            if (filehost_premium_online.contains("/")) {
                /* 2019-11-11: WTF They sometimes display multiple domains of one filehost in one entry, separated by ' / ' */
                logger.info("Special case: Multiple domains of one filehost given: " + filehost_premium_online);
                final String[] filehost_domains = filehost_premium_online.split("/");
                for (String filehost_domain : filehost_domains) {
                    filehost_domain = filehost_domain.trim();
                    filehosts_premium_onlineArray.add(filehost_domain);
                }
            } else {
                filehosts_premium_onlineArray.add(filehost_premium_online);
            }
        }
        ai.setMultiHostSupport(this, filehosts_premium_onlineArray);
        return ai;
    }

    private void loginAPI(final Account account, final boolean validate) throws Exception {
        final String saved_apiuser = account.getStringProperty(PROPERTY_ACCOUNT_apiuser);
        final String saved_apikey = account.getStringProperty(PROPERTY_ACCOUNT_apikey);
        String apiuser = null;
        String apikey = null;
        if (!this.isAPIKey(account.getPass()) && !this.isAPIKey(saved_apikey) && !this.isAPIKey(account.getPass())) {
            this.apiInvalidApikey(account);
        } else if (this.isAPIKey(account.getPass())) {
            apiuser = account.getUser();
            apikey = account.getPass();
        } else {
            /* Use saved credentials e.g. auto detected after website login. */
            apiuser = saved_apiuser;
            apikey = saved_apikey;
        }
        prepBrAPI(this.br);
        if (!validate) {
            /* Do not validate login credentials. */
            return;
        }
        logger.info("Performing full API login");
        if (useAPILoginWorkaround()) {
            final UrlQuery query = new UrlQuery();
            query.add("apiusername", Encoding.urlEncode(apiuser));
            query.add("apikey", Encoding.urlEncode(apikey));
            query.add("link", "null");
            this.getPage(API_BASE + "?" + query.toString());
            /* 2020-06-04: We expect this - otherwise probably wrong logindata: {"error":1,"message":"Link not supported or empty link."} */
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Number error = (Number) entries.get("error");
            if (error.intValue() != 1) {
                apiAccountInvalid(account);
            }
            /* 2020-06-04: Only premium users can get/see their apikey on the proleech website */
            account.setType(AccountType.PREMIUM);
        } else {
            final UrlQuery query = new UrlQuery();
            query.add("apiusername", Encoding.urlEncode(apiuser));
            query.add("apikey", Encoding.urlEncode(apikey));
            query.add("account", "");
            this.getPage(API_BASE + "?" + query.toString());
            this.checkErrorsAPI(null, account);
        }
        /* Just to save last login timestamp, we don't need these cookies! */
        account.saveCookies(br.getCookies(br.getHost()), "api");
    }

    private Thread showAPIUsageRecommended(final Account account, final boolean forceShowDialog) {
        synchronized (account) {
            if (account.hasProperty(PROPERTY_ACCOUNT_api_usage_hint_dialog_shown) && !forceShowDialog) {
                return null;
            }
            account.setProperty(PROPERTY_ACCOUNT_api_usage_hint_dialog_shown, true);
            final Thread thread = new Thread() {
                public void run() {
                    try {
                        String message = "";
                        final String title;
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            title = "Proleech.link - Webseiten Modus aktiv";
                            message += "Hallo liebe(r) proleech NutzerIn\r\n";
                            message += "Du verwendest proleech.link derzeit im Webseiten-Modus!\r\n";
                            message += "Wir empfehlen, den API Modus zu verwenden.\r\n";
                            message += "Diesen findest du unter: Einstellungen - Plugins - proleech.link - Enable API\r\n";
                            message += "Sobald der API Modus aktiviert ist, findest du deine speziellen JD Zugangsdaten unter: proleech.link/jdownloader\r\n";
                            message += "Falls du eben bereits deine API Zugangsdaten eingegeben hast wirst du gleich eine Fehlermeldung erhalten und musst es nach dem Ändern der Einstellung erneut versuchen.\r\n";
                        } else {
                            title = "Proleech.link - Website mode active";
                            message += "Hello dear proleech user\r\n";
                            message += "You are currently using proleech in website mode.\r\n";
                            message += "It is recommended to use the API mode instead.\r\n";
                            message += "You can enable it under Settings - Plugins - proleech.link - Enable API\r\n";
                            message += "Once enabled, you can find your special JD login credentials here: proleech.link/jdownloader\r\n";
                            message += "If you've already filled in your API login credentials just now, you will see an error soon and will have to enter your login credentials again after changing the above mentioned setting.\r\n";
                        }
                        final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                        dialog.setTimeout(2 * 60 * 1000);
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
    }

    private Thread showAPILoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    final String loginurl = "https://proleech.link/jdownloader";
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Proleech.link - Login";
                        message += "Hallo liebe(r) proleech NutzerIn\r\n";
                        message += "Um deinen Proleech Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + loginurl + "'\t\r\n";
                        message += "2. Suche dir auf der Seite deinen API Username und API Key heraus und gebe diese Informationen in JDownloader ein.\r\n";
                        message += "Achtung: Dein API Benutzername muss evtl. kleingeschrieben werden, unabhängig von der normalen Schreibweise deines Benutzernamens!\r\n";
                        message += "Du kannst deinen Account jetzt wie gewohnt in JDownloader verwenden.\r\n";
                    } else {
                        title = "Proleech.link - Login";
                        message += "Hello dear proleech user\r\n";
                        message += "In order to use your proleech account in JDownloader you will have to follow these instructions:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + loginurl + "'\t\r\n";
                        message += "2. Get your API Username and API Key displayed on this website and use this as your account login credentials.\r\n";
                        message += "Attention: Your API Username might need to be lowercase even if your original username contains uppercase characters!\r\n";
                        message += "Your account should now be accepted in JDownloader.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(loginurl);
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

    private List<String> regexPremiumHostsOnlineWebsite(final Browser br) throws PluginException {
        List<String> filehosts_premium_onlineArray = new ArrayList<String>();
        final String[] filehosts_premium_online = br.getRegex("<td>\\s*\\d+\\s*</td>\\s*<td>\\s*<img[^<]+/?>\\s*([^<]*?)\\s*</td>\\s*<td>\\s*<span\\s*class\\s*=\\s*\"label\\s*label-success\"\\s*>\\s*Online").getColumn(0);
        if (filehosts_premium_online == null || filehosts_premium_online.length == 0) {
            logger.warning("Failed to find list of supported hosts");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String filehost_premium_online : filehosts_premium_online) {
            if (filehost_premium_online.contains("/")) {
                /* 2019-11-11: WTF They sometimes display multiple domains of one filehost in one entry, separated by ' / ' */
                logger.info("Special case: Multiple domains of one filehost given: " + filehost_premium_online);
                final String[] filehost_domains = filehost_premium_online.split("/");
                for (String filehost_domain : filehost_domains) {
                    filehost_domain = filehost_domain.trim();
                    filehosts_premium_onlineArray.add(filehost_domain);
                }
            } else {
                filehosts_premium_onlineArray.add(filehost_premium_online);
            }
        }
        return filehosts_premium_onlineArray;
    }

    /* 2019-11-11: New: Max daily traffic value [80 GB at this moment] */
    /* 2022-19-08: New: Max daily traffic value [100 GB at this moment] */
    private String regexMaxDailyTrafficWebsite(final Browser br) {
        return br.getRegex("(\\d+(?:\\.\\d+)? GB) Daily Traff?ic").getMatch(0);
    }

    private boolean isLoggedinWebsite(final Browser br) throws PluginException {
        final boolean cookie_ok_amember_nr = br.getCookie("amember_nr", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookie_ok_amember_ru = br.getCookie("amember_ru", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookie_ok_amember_rp = br.getCookie("amember_rp", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookies_ok = cookie_ok_amember_nr && cookie_ok_amember_ru && cookie_ok_amember_rp;
        final boolean html_ok = br.containsHTML("/logout");
        logger.info("cookies_ok = " + cookies_ok);
        logger.info("html_ok = " + html_ok);
        /* 2019-11-11: Allow validation via cookies OR html code! */
        if (cookies_ok || html_ok) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    private boolean loginWebsite(final Account account, final AccountInfo ai, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust login-cookies without checking as they should still be fresh");
                        return false;
                    }
                    getPage("https://" + this.getHost() + "/member");
                    br.followRedirect();
                    loggedIN = this.isLoggedinWebsite(this.br);
                }
                if (!loggedIN) {
                    logger.info("Performing full login");
                    br.clearCookies(getHost());
                    getPage("https://" + this.getHost());
                    getPage("/login");
                    final Form loginform = br.getFormbyAction("/login");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("amember_login", URLEncoder.encode(account.getUser(), "UTF-8"));
                    loginform.put("amember_pass", URLEncoder.encode(account.getPass(), "UTF-8"));
                    final String loginAttemptID = br.getRegex("name=\"login_attempt_id\" value=\"(\\d+)\"").getMatch(0);
                    if (loginAttemptID != null) {
                        /* 2020-05-19 */
                        logger.info("Found loginAttemptID: " + loginAttemptID);
                        if (!loginform.hasInputFieldByName("login_attempt_id")) {
                            loginform.put("login_attempt_id", loginAttemptID);
                        }
                    } else {
                        logger.info("Failed to find loginAttemptID");
                    }
                    final boolean forceCaptcha = false;
                    if (loginform.containsHTML("recaptcha") || br.containsHTML("class=\"row row-wide row-login-recaptcha\"") || forceCaptcha) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        loginform.put("g-recaptcha-response", URLEncoder.encode(recaptchaV2Response, "UTF-8"));
                    }
                    submitForm(loginform);
                    br.followRedirect();
                    if (!br.getURL().contains("/member")) {
                        getPage("/member");
                    }
                    if (!isLoggedinWebsite(this.br)) {
                        /* On failure, recommend user to use API mode instead of website mode! */
                        showAPIUsageRecommended(account, true);
                        throw new AccountInvalidException();
                    }
                }
                /*
                 * 2019-11-19: Important! This is NOT the 'daily traffic used' - this is the total traffic ever used with the current
                 * account! We can display it in the account status but we cannot use this to calculate the remaining traffic!
                 */
                final String total_traffic_ever_used_with_this_accountStr = br.getRegex("(?i)Bandwidth Used\\s*:\\s*<span[^<>]*><b>\\s*(\\d+(?:\\.\\d{1,2})[ ]*[A-Za-z]+)\\s*<").getMatch(0);
                final String activeSubscription = br.getRegex("am-list-subscriptions\">\\s*<li[^<]*>(.*?)</li>").getMatch(0);
                String accountStatus = null;
                if (activeSubscription != null) {
                    final String expireDate = new Regex(activeSubscription, "([a-zA-Z]+\\s*\\d+,\\s*\\d{4})").getMatch(0);
                    if (expireDate != null) {
                        final long validUntil = TimeFormatter.getMilliSeconds(expireDate, "MMM' 'dd', 'yyyy", Locale.ENGLISH);
                        if (ai != null) {
                            ai.setValidUntil(validUntil);
                        }
                        account.setType(AccountType.PREMIUM);
                        /* 2019-11-19: Set premium account status in fetchAccountInfo */
                        accountStatus = "Premium User";
                        account.setConcurrentUsePossible(true);
                        account.setMaxSimultanDownloads(-1);
                    }
                } else {
                    account.setType(AccountType.FREE);
                    accountStatus = "Free User";
                    account.setConcurrentUsePossible(true);
                    if (ai != null) {
                        /* Only get/set hostlist if we're not currently trying to download a file (quick login) */
                        getPage("/downloader");
                        int maxfiles_per_day_used = 0;
                        int maxfiles_per_day_maxvalue = 0;
                        final Regex maxfiles_per_day = br.getRegex("(?i)<li>Files per day\\s*:\\s*?<b>\\s*?(\\d+)?\\s*?/\\s*?(\\d+)\\s*?</li>");
                        final String maxfiles_per_day_usedStr = maxfiles_per_day.getMatch(0);
                        final String maxfiles_per_day_maxvalueStr = maxfiles_per_day.getMatch(1);
                        final String max_free_filesize = br.getRegex("(?i)<li>Max\\. Filesize\\s*:\\s*<b>(\\d+ [^<>\"]+)</li>").getMatch(0);
                        if (maxfiles_per_day_usedStr != null) {
                            maxfiles_per_day_used = Integer.parseInt(maxfiles_per_day_usedStr);
                        }
                        if (maxfiles_per_day_maxvalueStr != null) {
                            maxfiles_per_day_maxvalue = Integer.parseInt(maxfiles_per_day_maxvalueStr);
                        }
                        /* ok = "/2" or "1/2", not ok = "2/2" */
                        if (max_free_filesize != null && maxfiles_per_day_maxvalue > 0) {
                            final int files_this_day_remaining = maxfiles_per_day_maxvalue - maxfiles_per_day_used;
                            if (files_this_day_remaining > 0) {
                                ai.setTrafficLeft(SizeFormatter.getSize(max_free_filesize));
                                /* 2019-08-14: Max files per day = 2 so a limit of 1 should be good! */
                                account.setMaxSimultanDownloads(1);
                            } else {
                                /*
                                 * No links remaining --> Display account with ZERO traffic left as it cannot be used to download at the
                                 * moment!
                                 */
                                logger.info("Cannot use free account because the max number of dail downloads has already been reached");
                                ai.setTrafficLeft(0);
                                account.setMaxSimultanDownloads(0);
                            }
                            accountStatus += " [" + files_this_day_remaining + "/" + maxfiles_per_day_maxvalueStr + " daily downloads remaining]";
                        } else {
                            logger.info("Cannot use free account because we failed to find any information about the current account limits");
                            account.setMaxSimultanDownloads(0);
                            ai.setTrafficLeft(0);
                            accountStatus += " [No downloads possible]";
                        }
                    }
                }
                if (ai != null) {
                    if (total_traffic_ever_used_with_this_accountStr != null) {
                        accountStatus += String.format(" [Total traffic ever used: %s]", total_traffic_ever_used_with_this_accountStr);
                    }
                    ai.setStatus(accountStatus);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                } else if (e.getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
                    /* 2020-06-18: Recommend API usage on e.g. Cloudflare failure or any other unexpected failures! */
                    showAPIUsageRecommended(account, false);
                }
                throw e;
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        /* 2019-11-11: Login is not required to check previously generated directurls */
        // login(account, null, false);
        final String directurlproperty = getHost();
        final String generatedDownloadURL = link.getStringProperty(directurlproperty);
        String dllink = null;
        /* We do not have to login to check previously generated downloadurls */
        boolean isLoggedIN = false;
        if (generatedDownloadURL != null) {
            /*
             * 2019-11-11: Seems like generated downloadurls are only valid for some seconds after genration but let's try to re-use them
             * anyways!
             */
            logger.info("Trying to re-use old generated downloadlink");
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedDownloadURL, true, 0);
                final boolean isOkay = isDownloadConnection(dl.getConnection());
                if (!isOkay) {
                    /* 2019-11-11: E.g. "Link expired! Please leech again." */
                    logger.info("Saved downloadurl did not work");
                    link.removeProperty(directurlproperty);
                    br.followConnection(true);
                } else {
                    dllink = generatedDownloadURL;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                logger.log(e);
            }
        }
        /*
         * TODO: Check if we need this to reset properties to force generation of new downloadurls to prevent infinite loops! Serverside
         * Cloud-download entries may as well link to non-working final downloadurls!
         */
        // final boolean is_forced_cloud_download = this.isForcedCloudDownload(link);
        // boolean found_downloadurl_in_cloud_downloads = false;
        if (dllink == null) {
            logger.info("Trying to generate final downloadurl");
            if (this.useAPIOnly()) {
                loginAPI(account, false);
                dllink = this.getDllinkAPI(account.getUser(), account.getPass(), link, account);
                if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                    /* This should never happen */
                    mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50);
                }
            } else {
                /* First, try to get downloadlinks for previously started cloud-downloads as this does not create new directurls */
                /* TODO: Try to grab previously generated direct-downloadurls for non-forced-cloud-downloads too */
                dllink = getDllinkWebsiteCloud(link, account);
                if (dllink != null) {
                    // found_downloadurl_in_cloud_downloads = true;
                } else {
                    final long userDefinedWaitHours = PluginJsonConfig.get(this.getConfigInterface()).getAllowDownloadlinkGenerationOnlyEveryXHours();
                    final long timestamp_next_downloadlink_generation_allowed = link.getLongProperty(PROPERTY_PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION, 0) + (userDefinedWaitHours * 60 * 60 * 1000);
                    if (userDefinedWaitHours > 0 && timestamp_next_downloadlink_generation_allowed > System.currentTimeMillis()) {
                        final long waittime_until_next_downloadlink_generation_is_allowed = timestamp_next_downloadlink_generation_allowed - System.currentTimeMillis();
                        final String waittime_until_next_downloadlink_generation_is_allowed_Str = TimeFormatter.formatSeconds(waittime_until_next_downloadlink_generation_is_allowed / 1000, 0);
                        logger.info("Next downloadlink generation is allowed in: " + waittime_until_next_downloadlink_generation_is_allowed_Str);
                        /*
                         * 2019-08-14: Set a small waittime here so links can be tried earlier again - so not set the long waittime
                         * waittime_until_next_downloadlink_generation_is_allowed!
                         */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Next downloadlink generation is allowed in " + waittime_until_next_downloadlink_generation_is_allowed_Str, 5 * 60 * 1000l);
                    }
                    logger.info("Generating fresh downloadurl");
                    final String apiuser = account.getStringProperty(PROPERTY_ACCOUNT_apiuser);
                    final String apikey = account.getStringProperty(PROPERTY_ACCOUNT_apikey);
                    boolean triedAPI = false;
                    if (tryAPIForDownloadingInWebsiteMode() && apiuser != null && apikey != null) {
                        logger.info("Trying to use API in website for downloading mode");
                        triedAPI = true;
                        try {
                            dllink = getDllinkAPI(apiuser, apikey, link, account);
                        } catch (final Exception e) {
                            logger.log(e);
                            logger.info("API in website mode failed");
                        }
                    }
                    if (StringUtils.isEmpty(dllink)) {
                        if (triedAPI) {
                            logger.info("Fallback to website");
                        } else {
                            logger.info("Using website");
                        }
                        final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
                        /* Login - first try without validating cookies! */
                        final boolean validatedCookies = loginWebsite(account, null, false);
                        final PostRequest post = new PostRequest("https://" + this.getHost() + "/dl/debrid/deb_process.php");
                        post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                        post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        post.put("urllist", URLEncoder.encode(url, "UTF-8"));
                        final String pass = link.getDownloadPassword();
                        if (StringUtils.isEmpty(pass)) {
                            post.put("pass", "");
                        } else {
                            post.put("pass", URLEncoder.encode(pass, "UTF-8"));
                        }
                        post.put("boxlinklist", "0");
                        sendRequest(post);
                        dllink = getDllinkWebsite(link, account);
                        if (StringUtils.isEmpty(dllink) && !validatedCookies && !this.isLoggedinWebsite(this.br)) {
                            /* Bad login - try again with fresh / validated cookies! */
                            loginWebsite(account, null, true);
                            sendRequest(post);
                            dllink = getDllinkWebsite(link, account);
                        }
                    }
                }
            }
            link.setProperty(PROPERTY_PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION, System.currentTimeMillis());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, this.getMaxChunks(link));
            /* Now we are definitely loggedIN */
            isLoggedIN = true;
        }
        final String server_filename = getFileNameFromDispositionHeader(dl.getConnection());
        if (!isDownloadConnection(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 2 * 60 * 1000l);
        }
        link.setProperty(directurlproperty, dllink);
        if (dl.startDownload()) {
            final String internal_filename = getInternalFilename(link);
            if (internal_filename != null) {
                deleteDownloadHistoryFilenameWhitelist.add(internal_filename);
            } else if (!StringUtils.isEmpty(server_filename)) {
                /* Try this as fallback */
                deleteDownloadHistoryFilenameWhitelist.add(server_filename);
            }
            clearDownloadHistoryWebsite(account, isLoggedIN);
        }
    }

    /** Deletes entries from serverside download history if: File is successfully downloaded or file is older than X days. */
    private void clearDownloadHistoryWebsite(final Account account, final boolean isLoggedIN) {
        if (!PluginJsonConfig.get(this.getConfigInterface()).isClearDownloadHistoryAfterEachDownload()) {
            logger.info("Not deleting download history - used has disabled this setting");
            return;
        } else if (this.useAPIOnly()) {
            logger.info("User wants download history cleared but: Cannot clear download history in API only mode!");
            return;
        }
        synchronized (account) {
            try {
                /* Only clear download history if user wants it AND if we're logged-in! */
                logger.info("Trying to clear download history");
                /*
                 * Do not use Cloudflare browser here - we do not want to get any captchas here! Rather fail than having to enter a captcha!
                 */
                if (br.getURL() == null || !br.getURL().contains("/mydownloads")) {
                    /* Only access this URL if it has not been accessed before! */
                    getPage("https://" + this.getHost() + "/mydownloads");
                }
                final String[] cloudDownloadRows = getDownloadHistoryRowsWebsite();
                final ArrayList<String> filename_entries_to_delete = new ArrayList<String>();
                /* First, let's check for old/dead entries and add them to our list of items we will remove later. */
                for (final String filenameToCheck : deleteDownloadHistoryFilenameWhitelist) {
                    if (!br.toString().contains(filenameToCheck)) {
                        filename_entries_to_delete.add(filenameToCheck);
                    }
                }
                if (cloudDownloadRows != null && cloudDownloadRows.length > 0) {
                    logger.info("Found " + cloudDownloadRows.length + " possible download_ids in history to delete");
                    String postData = "delete=Delete+selected";
                    int numberofDownloadIdsToDelete = 0;
                    for (final String cloudDownloadRow : cloudDownloadRows) {
                        final String download_id = new Regex(cloudDownloadRow, "id=\"checkbox\\[\\]\" value=\"(\\d+)\"").getMatch(0);
                        if (download_id == null) {
                            /* Skip invalid entries */
                            continue;
                        }
                        final String date_when_entry_was_addedStr = new Regex(cloudDownloadRow, ">\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s*<").getMatch(0);
                        final long delete_after_x_days = 1;
                        boolean isOldEntry = false;
                        final boolean isStillDownloadingToCloud = new Regex(cloudDownloadRow, "Transfering \\d{1,3}\\.\\d{1,2} ").matches();
                        boolean deletionAllowedByFilename = false;
                        String filename_of_current_entry = null;
                        for (final String deletionAllowedFilename : deleteDownloadHistoryFilenameWhitelist) {
                            if (cloudDownloadRow.contains(deletionAllowedFilename)) {
                                deletionAllowedByFilename = true;
                                filename_of_current_entry = deletionAllowedFilename;
                                filename_entries_to_delete.add(deletionAllowedFilename);
                                break;
                            }
                        }
                        if (date_when_entry_was_addedStr != null) {
                            final long current_time = getCurrentServerTime(br, System.currentTimeMillis());
                            final long date_when_entry_was_added = TimeFormatter.getMilliSeconds(date_when_entry_was_addedStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                            final long time_passed = current_time - date_when_entry_was_added;
                            if (time_passed > delete_after_x_days * 24 * 60 * 60 * 1000l) {
                                isOldEntry = true;
                            }
                        }
                        if (isStillDownloadingToCloud) {
                            logger.info("NOT deleting the following download_id because cloud download is still in progress: " + download_id);
                            continue;
                        } else if (!deletionAllowedByFilename && !isOldEntry) {
                            logger.info("NOT deleting the following download_id because its' filename is not allowed for deletion and it is not old enough: " + download_id);
                            continue;
                        }
                        if (isOldEntry) {
                            logger.info("Deleting the following entry as it is older than " + delete_after_x_days + " days: " + download_id);
                        } else {
                            logger.info("Deleting the following entry as deletion is allowed by filename: " + download_id + " | " + filename_of_current_entry);
                        }
                        postData += "&checkbox%5B%5D=" + download_id;
                        numberofDownloadIdsToDelete++;
                    }
                    if (numberofDownloadIdsToDelete == 0) {
                        /* This is unlikely but possible! */
                        logger.info("Found no download_ids to delete --> Probably all existing download_ids are download_ids with serverside cloud download in progress or they were added via website or via another JD instance and are not yet old enough to get deleted");
                    } else {
                        logger.info("Deleting " + numberofDownloadIdsToDelete + " of " + cloudDownloadRows.length + " download_ids");
                        br.postPage(br.getURL(), postData);
                        logger.info("Successfully cleared download history");
                    }
                    /*
                     * Cleanup deleteDownloadHistoryFilenameWhitelist - remove supposedly deleted elements from that list. And also elements
                     * which do not exist at all in the website list e.g. automatically deleted or deleted by other user in case people
                     * share accounts and download the same files.
                     */
                    for (final String deleted_filename : filename_entries_to_delete) {
                        deleteDownloadHistoryFilenameWhitelist.remove(deleted_filename);
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                logger.info("Error occured in delete-download-history handling");
            }
        }
    }

    private long getCurrentServerTime(final Browser br, final long fallback) {
        return getCurrentServerTime(br, "EEE, dd MMM yyyy HH:mm:ss z", fallback);
    }

    private long getCurrentServerTime(final Browser br, final String formatter, final long fallback_time) {
        long serverTime = -1;
        if (br != null && br.getHttpConnection() != null) {
            // lets use server time to determine time out value; we then need to adjust timeformatter reference +- time against server time
            final String dateString = br.getHttpConnection().getHeaderField("Date");
            if (dateString != null) {
                if (StringUtils.isNotEmpty(formatter) && dateString.matches("[A-Za-z]+, \\d{1,2} [A-Za-z]+ \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} [A-Z]+")) {
                    serverTime = TimeFormatter.getMilliSeconds(dateString, formatter, Locale.ENGLISH);
                } else {
                    final Date date = TimeFormatter.parseDateString(dateString);
                    if (date != null) {
                        serverTime = date.getTime();
                    }
                }
            }
        }
        if (serverTime == -1) {
            /* Fallback */
            serverTime = fallback_time;
        }
        return serverTime;
    }

    private String[] getDownloadHistoryRowsWebsite() {
        return br.getRegex("<tr>\\s*<td><input[^>]*name=\"checkbox\\[\\]\"[^>]+>.*?</tr>").getColumn(-1);
    }

    private String getDllinkWebsite(final DownloadLink link, final Account account) throws Exception {
        String dllink = br.getRegex("class=\"[^\"]*success\".*<a href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        /*
         * We can only identify our cloud-download-item by filename so let's get the filename they display as it may differ from the
         * filename we know/expect which means this is the only identifier we can use!
         */
        final String normal_download_internal_filename = br.getRegex("<a href=\"[^\"]+\"[^>]*?><b>([^<>\"]+)</a>").getMatch(0);
        String cloud_download_internal_filename = br.getRegex(">Transloading in progress.*?once completed\\!?</a>([^<>\"]+)</div>").getMatch(0);
        if (cloud_download_internal_filename == null) {
            /* 2019-12-01: 2nd version */
            cloud_download_internal_filename = br.getRegex(">Transloading in progress.*?once completed\\!?</a>\\s+<b>([^<>\"]*?)\\s*\\(<font color").getMatch(0);
        }
        if (dllink == null && cloud_download_internal_filename != null) {
            logger.info("Enforced cloud download: Needs to finish serverside download before we can download it --> Checking one time whether it might already be downloadable");
            link.setProperty("proleech_internal_filename", cloud_download_internal_filename);
            /* Mark as cloud download so that getDllinkCloud can be used! */
            link.setProperty("is_cloud_download", true);
            try {
                dllink = getDllinkWebsiteCloud(link, account);
            } catch (final PluginException e) {
            }
            if (dllink == null) {
                /**
                 * TODO: If a cloud download gets stuck serverside, we might need a handling to force-delete old entries and fully re-add
                 * them to the cloud downloader after a certain time ...
                 */
                logger.info("Unable to find downloadurl right away --> Waiting to retry later");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to proleech.link Cloud-downloader: Cloud download pending", 30 * 1000);
            }
        } else if (dllink != null && normal_download_internal_filename != null) {
            link.setProperty("proleech_internal_filename", normal_download_internal_filename);
        } else if (dllink != null && normal_download_internal_filename == null) {
            logger.info("Failed to find internal_filename --> We will probably be unable to delete this file");
        }
        if (StringUtils.isEmpty(dllink)) {
            final String website_errormessage = br.getRegex("class=\"[^\"]*danger\".*?<b>\\s*([^<>]+)").getMatch(0);
            if (website_errormessage != null) {
                /* 2019-11-11: E.g. "Too many requests! Please try again in a few seconds." */
                logger.info("Found errormessage on website:");
                logger.info(website_errormessage);
            }
            if (br.containsHTML(">\\s*?No link entered\\.?\\s*<")) {
                mhm.handleErrorGeneric(account, link, "no_link_entered", 50, 2 * 60 * 1000l);
            } else if (br.containsHTML(">\\s*Error getting the link from this account")) {
                mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
            } else if (br.containsHTML(">\\s*Our account has reached traffic limit")) {
                mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
            } else if (br.containsHTML(">\\s*This filehost is only enabled in")) {
                mhm.putError(account, link, 10 * 60 * 1000l, "This filehost is only available in premium mode");
            } else if (br.containsHTML(">\\s*You can only generate this link during Happy Hours")) {
                /* 2019-08-15: Can happen in free account mode - no idea when this "Happy Hour" is. Tested with uptobox.com URLs. */
                throw new AccountUnavailableException("You can only generate this link during Happy Hours", 5 * 60 * 1000l);
            } else if (website_errormessage != null) {
                /* Unknown failure but let's display errormessage from website to user. */
                // mhm.handleErrorGeneric(account, link, website_errormessage, 50, 2 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, website_errormessage, 5 * 60 * 1000l);
            } else {
                /* Unknown failure and we failed to find an errormessage */
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 2 * 60 * 1000l);
            }
        }
        return dllink;
    }

    /**
     * Tries to find final downloadurl for URLs which either had to be first downloaded to the proleech cloud or which were attempted to
     * download before and are still in the cloud.
     *
     * @throws Exception
     */
    private String getDllinkWebsiteCloud(final DownloadLink link, final Account account) throws Exception {
        if (link == null) {
            return null;
        }
        final boolean is_forced_cloud_download = this.isForcedCloudDownload(link);
        final String internal_filename = this.getInternalFilename(link);
        if (internal_filename != null || is_forced_cloud_download) {
            if (is_forced_cloud_download) {
                logger.info("Checking for directurl of forced cloud download URL");
            } else {
                logger.info("Checking for directurl of NON-forced cloud download URL");
            }
            getPage("https://" + this.getHost() + "/mydownloads");
            if (!this.isLoggedinWebsite(this.br)) {
                /* Ensure that we're logged-in */
                loginWebsite(account, null, true);
                getPage("/mydownloads");
            }
            final String errortext_basic = "Proleech.link Cloud-downloader:";
            boolean foundDownloadHistoryRow = false;
            String cloud_download_progress = null;
            String dllink = null;
            final String[] downloadHistoryRows = getDownloadHistoryRowsWebsite();
            if (downloadHistoryRows == null || downloadHistoryRows.length == 0) {
                logger.warning("Failed to find any download history objects --> Possible plugin failure");
                if (is_forced_cloud_download) {
                    logger.info("Failed to find any information about pending cloud download");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext_basic + " Failed to find any information about pending cloud download", 30 * 1000l);
                }
                /* No forced cloud download --> Continue so maybe upper handling will re-add downloadurl to list */
                return null;
            }
            for (final String downloadHistoryRow : downloadHistoryRows) {
                if (downloadHistoryRow.contains(internal_filename)) {
                    logger.info("Looks like we found the row containing our cloud-download");
                    foundDownloadHistoryRow = true;
                    /* RegEx for cloud-forced-downloads ("Save To Cloud" column) */
                    dllink = new Regex(downloadHistoryRow, "<a href=\"(https?://[^/]+/download/[^<>\"]+)\"").getMatch(0);
                    /* TODO: Add proper errorhandling for broken final downloadurls, then enable this. */
                    // if (dllink == null) {
                    // /* RegEx for non-cloud-forced-downloads ("Direct link" column) */
                    // dllink = new Regex(downloadHistoryRow, "<a href=\"(https?://[^/]+/d\\d+/[^<>\"]+)\"><u>Download</u>").getMatch(0);
                    // }
                    if (dllink != null) {
                        logger.info("Successfully found final downloadurl");
                        return dllink;
                    } else {
                        logger.info("Failed to find final downloadurl");
                        cloud_download_progress = new Regex(downloadHistoryRow, "Transfering (\\d{1,3}\\.\\d{1,2})\\s*?\\%").getMatch(0);
                        /* Continue! It can happen that there are multiple entries for the same filename! */
                    }
                }
            }
            if (dllink == null && is_forced_cloud_download) {
                /* This could also be a plugin failure but let's hope that a final downloadurl will be available later! */
                if (!foundDownloadHistoryRow) {
                    logger.info("Failed to find download history row");
                    /* TODO: Maybe remove this property to perform a full retry next time */
                    // link.removeProperty("proleech_internal_filename");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext_basic + " Failed to find cloud download information", 30 * 1000l);
                } else {
                    logger.info("Failed to find downloadurl for forced cloud download --> Serverside download is probably still running");
                    String error_text = errortext_basic + " Cloud download pending(?)";
                    if (!StringUtils.isEmpty(cloud_download_progress)) {
                        error_text = "[" + cloud_download_progress + "%] " + error_text;
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error_text, 15 * 1000l);
                }
            }
        }
        return null;
    }

    private String getDllinkAPI(final String apiuser, final String apikey, final DownloadLink link, final Account account) throws Exception {
        /* TODO: Check what happens when a user adds a "cloud download" URL in API mode */
        prepBrAPI(this.br);
        final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
        final UrlQuery query = new UrlQuery();
        query.add("apiusername", Encoding.urlEncode(apiuser));
        query.add("apikey", Encoding.urlEncode(apikey));
        query.add("link", Encoding.urlEncode(url));
        this.getPage(API_BASE + "?" + query.toString());
        checkErrorsAPI(link, account);
        /*
         * 2020-06-04: E.g. success response: {"error":0,"message":"OK","hoster":"http:CENSORED","link":"http:CENSORED","size":"10.15 MB"}
         */
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Number max_chunks = (Number) entries.get("max_chunks");
        if (max_chunks != null && max_chunks.intValue() > 0) {
            link.setProperty(PROPERTY_MAXCHUNKS, max_chunks);
        } else {
            link.removeProperty(PROPERTY_MAXCHUNKS);
        }
        return (String) entries.get("link");
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

    private void checkErrorsAPI(final DownloadLink link, final Account account) throws Exception {
        try {
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Number errorcode = (Number) entries.get("error");
            final String message = (String) entries.get("message");
            if (errorcode != null && message != null) {
                /* 2022-09-15: Possible not explicitely implemented error(s): */
                /*
                 * {"error":9,"message":"Your file is big! (12.0 MB). You do not have traffic- bandwidth limit 10.0 GB :)\r\n\t\t\t\t Time
                 * Left To Reset Your Bandwith For This Host: 9 Hours 11 Minutes 33 Seconds"}
                 */
                switch (errorcode.intValue()) {
                case 0:
                    /* No error */
                    break;
                case 4:
                    /* {"error":4,"message":" No account is working. Try repost later. "} */
                    mhm.handleErrorGeneric(account, link, message, 5);
                case -10:
                    /* 2020-05-06: According to admin: -10 Account is invalid. */
                    apiAccountInvalid(account);
                case -9:
                    /* 2020-05-06: According to admin: -9 Your account is locked due to sharing account. */
                    throw new AccountInvalidException(message);
                case -8:
                    /* 2020-05-06: According to admin: -8 Your account has problem. Please contact to admin. */
                    throw new AccountInvalidException(message);
                case -6:
                    apiAccountInvalid(account);
                case -5:
                    throw new AccountInvalidException("Free accounts are not supported");
                case -1:
                    /* {"error":-1,"message":"API key is invalid. Please update new API key https:\/\/proleech.link\/jdownloader."} */
                    apiAccountInvalid(account);
                case 1:
                    /* 2020-06-04: Rare error I guess? */
                    /* {"error":1,"message":"Link not supported or empty link."} */
                    mhm.handleErrorGeneric(account, link, message, 5);
                default:
                    /* Handle all other errors */
                    /**
                     * 0 Get details of account or Generated link1 Link not introduced or Link not supported or empty link.2 Please include
                     * 'http//:' in your link3 Link not supported4 Another error (Filehost out of traffic, can't generate file ...)7 Error:
                     * Link Dead or Host Temporarily Down8 Message reached the limits for host9 Your file is big
                     */
                    if (link == null) {
                        /* Account error */
                        throw new AccountUnavailableException(message, 5 * 60 * 1000l);
                    } else {
                        mhm.handleErrorGeneric(account, link, message, 50);
                    }
                }
            }
        } catch (final JSonMapperException xe) {
            final String msg = "Invalid API response";
            if (link == null) {
                /* Account error */
                throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
            } else {
                mhm.handleErrorGeneric(account, link, msg, 50);
            }
        }
    }

    private boolean isAPIKey(final String str) {
        if (str == null) {
            return false;
        }
        return str.matches("[a-z0-9]{21}");
    }

    /** Invalid API Username and/or API Key. */
    private void apiAccountInvalid(final Account account) throws PluginException {
        try {
            final boolean loginDialogDisplayedAlreadyForThisAccount = account.getBooleanProperty(PROPERTY_ACCOUNT_api_login_dialog_shown, false);
            if (!loginDialogDisplayedAlreadyForThisAccount) {
                showAPILoginInformation();
                account.setProperty(PROPERTY_ACCOUNT_api_login_dialog_shown, true);
            }
        } catch (final NullPointerException npe) {
            /* This should never happen, account should always exist! */
            logger.log(npe);
        }
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new AccountInvalidException("API Username/API Key ungültig!\r\n Siehe proleech.link/jdownloader");
        } else {
            throw new AccountInvalidException("API Username/API Key invalid!\r\n See proleech.link/jdownloader");
        }
    }

    /** Invalid API Key (API User may be valid). */
    private void apiInvalidApikey(final Account account) throws PluginException {
        try {
            final boolean loginDialogDisplayedAlreadyForThisAccount = account.getBooleanProperty(PROPERTY_ACCOUNT_api_login_dialog_shown, false);
            if (!loginDialogDisplayedAlreadyForThisAccount) {
                showAPILoginInformation();
                account.setProperty(PROPERTY_ACCOUNT_api_login_dialog_shown, true);
            }
        } catch (final NullPointerException npe) {
            /* This should never happen, account should always exist! */
            logger.log(npe);
        }
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new AccountInvalidException("API Key ungültig!\r\n Siehe proleech.link/jdownloader");
        } else {
            throw new AccountInvalidException("API Key invalid!\r\n See proleech.link/jdownloader");
        }
    }

    private boolean isForcedCloudDownload(final DownloadLink link) {
        return link.getBooleanProperty("is_cloud_download", false);
    }

    private String getInternalFilename(final DownloadLink link) {
        return link.getStringProperty("proleech_internal_filename", null);
    }

    private boolean isDownloadConnection(URLConnectionAdapter con) throws IOException {
        final String contentType = con.getContentType();
        final int responseCode = con.getResponseCode();
        switch (responseCode) {
        case 200:
        case 206:
            if (con.isContentDisposition()) {
                return true;
            } else if (StringUtils.containsIgnoreCase(contentType, "application/force-download")) {
                return true;
            } else if (StringUtils.containsIgnoreCase(contentType, "application/octet-stream")) {
                return true;
            } else {
                return false;
            }
        default:
            return false;
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public Class<? extends ProleechLinkConfig> getConfigInterface() {
        return ProleechLinkConfig.class;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
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
    public void handleFree(DownloadLink link) throws Exception {
        throw new AccountRequiredException();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}
