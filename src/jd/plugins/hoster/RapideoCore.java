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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public abstract class RapideoCore extends PluginForHost {
    public RapideoCore(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + getHost() + "/rejestracja");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        /* Prefer English language */
        br.setCookie(br.getHost(), "lang2", "EN");
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/legal";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    private static final String PROPERTY_ACCOUNT_AUTH_TOKEN = "auth_token";

    protected abstract MultiHosterManagement getMultiHosterManagement();

    /**
     * rapideo.net: enc.rapideo.pl </br>
     * nopremium.pl: crypt.nopremium.pl
     */
    protected abstract String getAPIV1Base();

    /**
     * 2024-08-29: They got another endpoint which is e.g. hidden in their qnap download addon: </br>
     * https://www.rapideo.net/software/qnap/rapideo.addon
     */
    protected String getAPIBase2() {
        return "https://www." + getHost() + "/api/rest";
    }

    /**
     * rapideo.net: newrd </br>
     * nopremium.pl: nopremium
     */
    protected abstract String getAPIV1SiteParam();

    /** Returns value used as password for API requests. */
    protected abstract String getPasswordAPIV1(final Account account);

    /** This controls the endpoint to be used when accessing this service. */
    protected ENDPOINT_TYPE getEndpointType() {
        return ENDPOINT_TYPE.APIv2;
    }

    public enum ENDPOINT_TYPE {
        APIv1,
        APIv2,
        WEBSITE
    }

    /** Returns pre-filled map containing basic json POST data for APIv2 rquests. */
    private Map<String, Object> getPostmapAPIv2() {
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("device", "JDownloader");
        postdata.put("version", this.getVersion());
        return postdata;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final ENDPOINT_TYPE accesstype = getEndpointType();
        if (accesstype == ENDPOINT_TYPE.APIv1) {
            return this.fetchAccountInfoAPIv1(account);
        } else if (accesstype == ENDPOINT_TYPE.APIv2) {
            return this.fetchAccountInfoAPIv2(account);
        } else {
            return this.fetchAccountInfoWebsite(account);
        }
    }

    private AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        loginWebsite(account, true);
        final AccountInfo ac = new AccountInfo();
        /* Obtain "Traffic left" value from website */
        String trafficLeftStr = br.getRegex("\">\\s*(?:Account balance|Stan Twojego konta)\\s*:\\s*(\\d+(\\.\\d{1,2})? [A-Za-z]{1,5})").getMatch(0);
        if (trafficLeftStr == null) {
            /* nopremium.pl */
            trafficLeftStr = br.getRegex("Pozostały transfer:\\s*<span[^>]*>([^<]+)</span>").getMatch(0);
        }
        if (trafficLeftStr != null) {
            ac.setTrafficLeft(SizeFormatter.getSize(trafficLeftStr));
        } else {
            logger.warning("Failed to find trafficleft value");
        }
        /* Get a list of all supported hosts */
        br.getPage("/twoje_pliki");
        checkErrorsWebsite(br, null, account);
        final HashSet<String> crippledhosts = new HashSet<String>();
        /*
         * This will not catch stuff like <li><strong>nitroflare (beta) </strong></li> ... but we are lazy and ignore this. Such hosts will
         * be found too by the 2nd regex down below.
         */
        final String[] crippledHosts = br.getRegex("<li>([A-Za-z0-9\\-\\.]+)</li>").getColumn(0);
        if (crippledHosts != null && crippledHosts.length > 0) {
            for (final String crippledhost : crippledHosts) {
                crippledhosts.add(crippledhost);
            }
        }
        /* Alternative place to obtain supported hosts from */
        final String[] crippledhosts2 = br.getRegex("class=\"active\"[^<]*>([^<]+)</a>").getColumn(0);
        if (crippledhosts2 != null && crippledhosts2.length > 0) {
            for (final String crippledhost : crippledhosts2) {
                crippledhosts.add(crippledhost);
            }
        }
        /* Alternative place to obtain supported hosts from */
        String htmlMetadataStr = br.getRegex("name=\"keywords\" content=\"([\\w, ]+)").getMatch(0);
        if (htmlMetadataStr != null) {
            htmlMetadataStr = htmlMetadataStr.replace(" ", "");
            final String[] crippledhosts3 = htmlMetadataStr.split(",");
            if (crippledhosts3 != null && crippledhosts3.length > 0) {
                for (final String crippledhost : crippledhosts3) {
                    crippledhosts.add(crippledhost);
                }
            }
        }
        final List<String> supportedHosts = new ArrayList<String>();
        /* Sanitize data and add to final list */
        for (String crippledhost : crippledhosts) {
            crippledhost = crippledhost.toLowerCase(Locale.ENGLISH);
            if (crippledhost.equalsIgnoreCase("mega")) {
                supportedHosts.add("mega.nz");
            } else {
                supportedHosts.add(crippledhost);
            }
        }
        /*
         * They only have accounts with traffic, no free/premium difference (other than no traffic) - we treat no-traffic as FREE --> Cannot
         * download anything
         */
        if (trafficLeftStr != null && ac.getTrafficLeft() > 0) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        if ((ac.getTrafficLeft() == 0 || trafficLeftStr == null) && AccountType.FREE.equals(account.getType()) && br.containsHTML("Check your email and")) {
            /* Un-activated free account without traffic -> Not usable at all. */
            throw new AccountUnavailableException("Check your email and click the confirmation link to activate your account", 5 * 60 * 1000l);
        }
        return ac;
    }

    private AccountInfo fetchAccountInfoAPIv1(final Account account) throws Exception {
        final Map<String, Object> root = loginAPIV1(br.cloneBrowser(), account);
        final AccountInfo ac = new AccountInfo();
        final Object trafficLeftO = root.get("balance");
        if (trafficLeftO != null && trafficLeftO.toString().matches("\\d+")) {
            ac.setTrafficLeft(Long.parseLong(trafficLeftO.toString()) * 1024 * 1024);
        } else {
            ac.setTrafficLeft(0);
        }
        final List<String> supportedHosts = getSupportedHostsAPIv1(account);
        /*
         * They only have accounts with traffic, no free/premium difference (other than no traffic) - we treat no-traffic as FREE --> Cannot
         * download anything
         */
        if (trafficLeftO != null && ac.getTrafficLeft() > 0) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    private AccountInfo fetchAccountInfoAPIv2(final Account account) throws Exception {
        final String authtoken = this.getAuthTokenApiV2(account);
        final Map<String, Object> postdata = this.getPostmapAPIv2();
        postdata.put("authtoken", authtoken);
        final PostRequest req = br.createJSonPostRequest(this.getAPIBase2() + "/account", postdata);
        br.getPage(req);
        final Map<String, Object> root = this.checkErrorsAPIv2(br, null, account);
        final Map<String, Object> accountmap = (Map<String, Object>) root.get("account");
        final Object premium_expire_dateO = accountmap.get("premium_expire_date");
        final AccountInfo ac = new AccountInfo();
        final Object transfer_left_gb = accountmap.get("transfer_left_gb");
        final Object bonus_left_gb = accountmap.get("bonus_left_gb");
        double trafficLeft_gb = 0;
        if (transfer_left_gb != null && transfer_left_gb.toString().matches("\\d+(\\.\\d+)?")) {
            trafficLeft_gb = Double.parseDouble(transfer_left_gb.toString());
        }
        if (bonus_left_gb != null && bonus_left_gb.toString().matches("\\d+(\\.\\d+)?")) {
            trafficLeft_gb += Double.parseDouble(bonus_left_gb.toString());
        }
        ac.setTrafficLeft((long) (trafficLeft_gb * 1024 * 1024 * 1024));
        /**
         * 2024-08-29: TODO: I've never seen an account with expire-date so for now I'll only set it for accounts with zero traffic left.
         */
        if (premium_expire_dateO instanceof Number && trafficLeft_gb == 0) {
            // ac.setValidUntil(((Number) premium_expire_dateO).longValue());
        }
        /* There is no v2 endpoint available to fetch list of supported hosts -> Use v1 endpoint. */
        final List<String> supportedHosts = getSupportedHostsAPIv1(account);
        /*
         * They only have accounts with traffic, no free/premium difference (other than no traffic) - we treat no-traffic as FREE --> Cannot
         * download anything
         */
        if (Boolean.TRUE.equals(accountmap.get("premium_active")) || trafficLeft_gb > 0) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    private List<String> getSupportedHostsAPIv1(final Account account) throws IOException {
        br.getPage("https://www." + getHost() + "/clipboard.php?json=3");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final List<Map<String, Object>> hosterlist = (List<Map<String, Object>>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
        final boolean skipNonZeroSdownloadItems = false;
        for (final Map<String, Object> hosterinfo : hosterlist) {
            final List<String> domains = (List<String>) hosterinfo.get("domains");
            final Object sdownload = hosterinfo.get("sdownload");
            if (sdownload == null || domains == null) {
                /* Skip invalid items (yes they exist, tested 2024-08-20) */
                continue;
            }
            if (sdownload.toString().equals("0") || skipNonZeroSdownloadItems == false) {
                supportedHosts.addAll(domains);
            } else {
                logger.info("Skipping serverside disabled domains: " + domains);
            }
        }
        return supportedHosts;
    }

    private void loginWebsite(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                if (!verifyCookies) {
                    /* Don't verify */
                    return;
                }
                logger.info("Checking login cookies");
                br.getPage("https://www." + getHost() + "/");
                if (loggedINWebsite(this.br)) {
                    logger.info("Successfully loggedin via cookies");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Failed to login via cookies");
                }
            }
            logger.info("Attempting full login");
            br.getPage("https://www." + getHost() + "/login");
            checkErrorsWebsite(br, null, account);
            /*
             * 2020-11-02: There are two Forms matching this but the first one is the one we want - the 2nd one is the
             * "register new account" Form.
             */
            final Form loginform = br.getFormbyKey("remember");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find loginform");
            }
            loginform.put("login", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            loginform.put("remember", "on");
            br.submitForm(loginform);
            checkErrorsWebsite(br, null, account);
            if (!loggedINWebsite(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return;
        }
    }

    private boolean loggedINWebsite(final Browser br) {
        if (br.containsHTML("Logged in as\\s*:|Zalogowany jako\\s*:")) {
            return true;
        } else {
            return false;
        }
    }

    private Map<String, Object> loginAPIV1(final Browser br, final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            final UrlQuery query = new UrlQuery();
            query.add("site", getAPIV1SiteParam());
            query.add("output", "json");
            query.add("loc", "1");
            query.add("info", "1");
            query.add("username", Encoding.urlEncode(account.getUser()));
            query.add("password", getPasswordAPIV1(account));
            br.postPage(getAPIV1Base(), query);
            final Map<String, Object> root = this.checkErrorsAPIv1(br, null, account);
            return root;
        }
    }

    private Map<String, Object> loginAPIV2(final Browser br, final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            final Map<String, Object> postdata = getPostmapAPIv2();
            postdata.put("login", account.getUser());
            postdata.put("password", account.getPass());
            final PostRequest req = br.createJSonPostRequest(this.getAPIBase2() + "/login", postdata);
            br.getPage(req);
            final Map<String, Object> resp = this.checkErrorsAPIv2(br, null, account);
            if (!Boolean.TRUE.equals(resp.get("logged"))) {
                /* This should never happen */
                throw new AccountInvalidException();
            }
            final String auth_token = resp.get("authtoken").toString();
            if (StringUtils.isEmpty(auth_token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* This token looks to be valid forever. */
            account.setProperty(PROPERTY_ACCOUNT_AUTH_TOKEN, auth_token);
            return resp;
        }
    }

    private String getAuthTokenApiV2(final Account account) throws IOException, PluginException, InterruptedException {
        final String storedToken = account.getStringProperty(PROPERTY_ACCOUNT_AUTH_TOKEN);
        if (storedToken != null) {
            return storedToken;
        } else {
            this.loginAPIV2(br, account);
            return account.getStringProperty(PROPERTY_ACCOUNT_AUTH_TOKEN);
        }
    }

    private void checkErrorsWebsite(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        String geoLoginFailure = br.getRegex(">\\s*(You login from a different location than usual[^<]*)<").getMatch(0);
        if (geoLoginFailure == null) {
            /* nopremium.pl, they only have Polish version. */
            geoLoginFailure = br.getRegex(">\\s*(Logujesz się z innego miejsca niż zazwyczaj[^<]+)").getMatch(0);
        }
        if (geoLoginFailure != null) {
            /* 2020-11-02: Login from unusual location -> User has to confirm via URL send by mail and then try again in JD (?!). */
            throw new AccountUnavailableException(geoLoginFailure + "\r\nOnce done, refresh your account in JDownloader.", 5 * 60 * 1000l);
        } else if (br.containsHTML("<title>\\s*Dostęp zabroniony\\s*</title>")) {
            errorBannedIP(account);
        }
    }

    /**
     * Parses json with basic errorhandling.
     *
     * @throws Exception
     */
    private Map<String, Object> parseJsonAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        try {
            final Map<String, Object> entries = (Map<String, Object>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            return entries;
        } catch (final JSonMapperException e) {
            /* Check for html based errors */
            this.checkErrorsWebsite(br, link, account);
            /* Check misc API responses */
            if (br.getHttpConnection().getResponseCode() == 403) {
                errorBannedIP(account);
            }
            /* Dead end */
            final String errortext = "Invalid API response";
            if (link == null) {
                throw new AccountUnavailableException(errortext, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext);
            }
        }
    }

    private Map<String, Object> checkErrorsAPIv1(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> entries = parseJsonAPI(br, link, account);
        return checkErrorsAPIv1(entries, link, account);
    }

    private Map<String, Object> checkErrorsAPIv1(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Number errnoO = (Number) entries.get("errno");
        final String errstring = (String) entries.get("errstring");
        if (errstring == null || errnoO == null) {
            /* No error */
            return entries;
        }
        final int errorcode = errnoO.intValue();
        /* 2024-08-21: At this moment, we only handle account related error messages here. */
        final HashSet<Integer> accountInvalidErrors = new HashSet<Integer>();
        /* {"errno":0,"errstring":"Nieprawid\u0142owa nazwa u\u017cytkownika\/has\u0142o"} */
        accountInvalidErrors.add(0);
        final HashSet<Integer> accountUnavailableErrors = new HashSet<Integer>();
        /* {"errno":80,"errstring":"Zbyt wiele pr\u00f3b logowania - dost\u0119p zosta\u0142 tymczasowo zablokowany"} */
        accountUnavailableErrors.add(80);
        final HashSet<Integer> downloadErrors = new HashSet<Integer>();
        /*
         * {"errno":1001,
         * "errstring":"Aby doda\u0107 pliki z tego hostingu nale\u017cy zaznaczy\u0107 opcj\u0119 pobierania plik\u00f3w na serwer"}
         */
        downloadErrors.add(1001);
        if (accountInvalidErrors.contains(errorcode)) {
            /* Permanent account errors like invalid user/pw */
            throw new AccountInvalidException(errstring);
        } else if (accountUnavailableErrors.contains(errorcode)) {
            /* Temporary account errors */
            throw new AccountUnavailableException(errstring, 5 * 60 * 1000l);
        } else if (downloadErrors.contains(errorcode) && link != null) {
            /* Temporary account errors */
            this.getMultiHosterManagement().handleErrorGeneric(account, link, errstring, 3);
            /* This shall never be reached! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            /* Unknown errors */
            if (link != null) {
                /* Treat as download related error. */
                this.getMultiHosterManagement().handleErrorGeneric(account, link, errstring, 50);
                /* This shall never be reached! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                /* Treat as account related error. */
                throw new AccountInvalidException(errstring);
            }
        }
    }

    private Map<String, Object> checkErrorsAPIv2(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> entries = parseJsonAPI(br, link, account);
        return checkErrorsAPIv2(entries, link, account);
    }

    private Map<String, Object> checkErrorsAPIv2(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> filemap = (Map<String, Object>) entries.get("file");
        if (filemap != null) {
            /* Handle download related errors */
            /*
             * Example:
             * {"file":{"index":1,"error":15,"message":"Hostingnieobs\u0142ugiwany-skorzystajznaszejwyszukiwarkiwpisuj...","url":"https:..."
             * }}
             */
            final Number errorO = (Number) filemap.get("error");
            final String message = (String) filemap.get("message");
            if (errorO == null || message == null) {
                /* No error */
                return entries;
            }
            final int errorcode = errorO.intValue();
            /* Errors which shall disable the host immediately. */
            final HashSet<Integer> permanentErrors = new HashSet<Integer>();
            permanentErrors.add(15);
            final MultiHosterManagement mhm = getMultiHosterManagement();
            if (permanentErrors.contains(errorcode)) {
                mhm.putError(account, link, 5 * 60 * 1000l, message);
            } else {
                mhm.handleErrorGeneric(account, link, message, 50);
            }
            /* This shall never be reached! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Handle login related errors */
        final Number errorO = (Number) entries.get("error");
        final String message = (String) entries.get("message");
        if (message == null || errorO == null) {
            /* No error */
            return entries;
        }
        final int errorcode = errorO.intValue();
        if (errorcode == 1) {
            /* { "error": 1, "message": "Authtoken invalid"} */
            account.removeProperty(PROPERTY_ACCOUNT_AUTH_TOKEN);
            throw new AccountUnavailableException(message, 1 * 60 * 1000l);
        } else {
            /* 2024-08-21: At this moment, we only handle account related error messages here. */
            final HashSet<Integer> accountInvalidErrors = new HashSet<Integer>();
            /* { "error": 3, "message": "Invalid username or password"} */
            accountInvalidErrors.add(3);
            final HashSet<Integer> accountUnavailableErrors = new HashSet<Integer>();
            final HashSet<Integer> downloadErrors = new HashSet<Integer>();
            if (accountInvalidErrors.contains(errorcode)) {
                /* Permanent account errors like invalid user/pw */
                throw new AccountInvalidException(message);
            } else if (accountUnavailableErrors.contains(errorcode)) {
                /* Temporary account errors */
                throw new AccountUnavailableException(message, 5 * 60 * 1000l);
            } else if (downloadErrors.contains(errorcode) && link != null) {
                /* Temporary account errors */
                this.getMultiHosterManagement().handleErrorGeneric(account, link, message, 3);
                /* This shall never be reached! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                /* Unknown errors */
                if (link != null) {
                    /* Treat as download related error. */
                    this.getMultiHosterManagement().handleErrorGeneric(account, link, message, 50);
                    /* This shall never be reached! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* Treat as account related error. */
                    throw new AccountInvalidException(message);
                }
            }
        }
    }

    private final void errorBannedIP(final Account account) throws AccountUnavailableException {
        throw new AccountUnavailableException("Your IP has been banned", 5 * 60 * 1000l);
    }

    private String getDirecturlAPIv1(final DownloadLink link, final Account account) throws Exception {
        final MultiHosterManagement mhm = getMultiHosterManagement();
        final String url_urlencoded = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
        final Browser brc = br.cloneBrowser();
        final UrlQuery query = new UrlQuery();
        query.add("site", getAPIV1SiteParam());
        query.add("output", "json");
        // query.add("loc", "1");
        query.add("info", "0");
        query.add("username", Encoding.urlEncode(account.getUser()));
        query.add("password", getPasswordAPIV1(account));
        query.add("url", url_urlencoded);
        brc.postPage(getAPIV1Base(), query);
        /* We expect the whole response to be an URL. */
        final String dllink = brc.getRequest().getHtmlCode();
        try {
            new URL(dllink);
        } catch (final MalformedURLException e) {
            /* This should never happen */
            this.checkErrorsAPIv1(brc, link, account);
            mhm.handleErrorGeneric(account, link, "API returned invalid downloadlink", 50);
        }
        return dllink;
    }

    private String getDirecturlAPIv2(final DownloadLink link, final Account account) throws Exception {
        final String authtoken = this.getAuthTokenApiV2(account);
        final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
        final Map<String, Object> postdata = this.getPostmapAPIv2();
        postdata.put("authtoken", authtoken);
        postdata.put("url", url);
        final PostRequest req = br.createJSonPostRequest(this.getAPIBase2() + "/files/check", postdata);
        br.getPage(req);
        final Map<String, Object> resp = this.checkErrorsAPIv2(br, link, account);
        final Map<String, Object> filemap = (Map<String, Object>) resp.get("file");
        final String hash = filemap.get("hash").toString();
        final Map<String, Object> postdata2 = this.getPostmapAPIv2();
        postdata2.put("authtoken", authtoken);
        postdata2.put("hash", hash);
        /* Important! Without this parameter, the following request will not return the field "file.url"! */
        postdata2.put("mode", "qnap");
        final PostRequest req2 = br.createJSonPostRequest(this.getAPIBase2() + "/files/download", postdata2);
        br.getPage(req2);
        final Map<String, Object> resp2 = this.checkErrorsAPIv2(br, link, account);
        final Map<String, Object> filemap2 = (Map<String, Object>) resp2.get("file");
        final String dllink = filemap2.get("url").toString();
        return dllink;
    }

    private String getDirecturlWebsite(final DownloadLink link, final Account account) throws Exception {
        final MultiHosterManagement mhm = getMultiHosterManagement();
        final String url_urlencoded = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
        loginWebsite(account, false);
        br.getPage("https://www." + getHost() + "/twoje_pliki");
        checkErrorsWebsite(br, link, account);
        br.postPage("/twoje_pliki", "loadfiles=1");
        br.postPage("/twoje_pliki", "loadfiles=2");
        br.postPage("/twoje_pliki", "loadfiles=3");
        final int random = new Random().nextInt(1000000);
        final DecimalFormat df = new DecimalFormat("000000");
        final String random_session = df.format(random);
        final String filename = link.getName();
        br.postPage("/twoje_pliki", "session=" + random_session + "&links=" + url_urlencoded);
        if (br.containsHTML("strong>\\s*Brak transferu\\s*</strong>")) {
            throw new AccountUnavailableException("Out of traffic", 1 * 60 * 1000l);
        }
        final String id = br.getRegex("data\\-id=\"([a-z0-9]+)\"").getMatch(0);
        if (id == null) {
            mhm.handleErrorGeneric(account, link, "Failed to find transferID", 50);
        }
        br.postPage("/twoje_pliki", "downloadprogress=1");
        br.postPage("/progress", "session=" + random_session + "&total=1");
        br.postPage("/twoje_pliki", "insert=1&ds=false&di=false&note=false&notepaths=" + url_urlencoded + "&sids=" + id + "&hids=&iids=&wids=");
        br.postPage("/twoje_pliki", "loadfiles=1");
        br.postPage("/twoje_pliki", "loadfiles=2");
        br.postPage("/twoje_pliki", "loadfiles=3");
        /* Sometimes it takes over 10 minutes until the file has been downloaded to the remote server. */
        for (int i = 1; i <= 280; i++) {
            br.postPage("/twoje_pliki", "downloadprogress=1");
            checkErrorsWebsite(br, link, account);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> standardFiles = (List<Map<String, Object>>) entries.get("StandardFiles");
            Map<String, Object> activeDownloadingFileInfo = null;
            for (final Map<String, Object> standardFile : standardFiles) {
                final String thisFilename = (String) standardFile.get("filename");
                final String thisFilenameFull = (String) standardFile.get("filename_full");
                /* Find our file as multiple files could be downloading at the same time. */
                if (thisFilename.equalsIgnoreCase(filename) || thisFilenameFull.equalsIgnoreCase(filename)) {
                    activeDownloadingFileInfo = standardFile;
                    break;
                }
            }
            if (activeDownloadingFileInfo == null) {
                mhm.handleErrorGeneric(account, link, "Failed to locate added info to actively downloading file", 20);
            }
            final String status = activeDownloadingFileInfo.get("status").toString();
            if (status.equalsIgnoreCase("finish")) {
                return activeDownloadingFileInfo.get("download_url").toString();
            } else if (status.equalsIgnoreCase("initialization")) {
                this.sleep(3 * 1000l, link);
                continue;
            } else {
                /* Serverside download has never been started or stopped for unknown reasons. */
                logger.warning("Serverside download failed?!");
                break;
            }
        }
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = "directurl_" + this.getHost();
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        final MultiHosterManagement mhm = getMultiHosterManagement();
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            mhm.runCheck(account, link);
            final ENDPOINT_TYPE accesstype = getEndpointType();
            if (accesstype == ENDPOINT_TYPE.APIv1) {
                dllink = getDirecturlAPIv1(link, account);
            } else if (accesstype == ENDPOINT_TYPE.APIv2) {
                dllink = getDirecturlAPIv2(link, account);
            } else {
                /* Website */
                dllink = getDirecturlWebsite(link, account);
            }
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed to generate downloadurl", 20);
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                /* Use API errorhandling here since it will do a fallback to website errorhandling if response is not a json response. */
                this.checkErrorsAPIv1(br, link, account);
                // this.checkErrorsWebsite(br, link, account);
                mhm.handleErrorGeneric(account, link, "Final downloadlink did not lead to downloadable content", 50);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?", e);
            } else {
                throw e;
            }
        }
        link.setProperty(directurlproperty, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            getMultiHosterManagement().runCheck(account, link);
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}