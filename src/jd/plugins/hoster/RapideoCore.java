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

    public RapideoCore(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + getHost() + "/rejestracja");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/legal";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    protected abstract MultiHosterManagement getMultiHosterManagement();

    /**
     * rapideo.net: enc.rapideo.pl </br>
     * nopremium.pl: crypt.nopremium.pl
     */
    protected abstract String getAPIBase();

    /**
     * rapideo.net: newrd </br>
     * nopremium.pl: nopremium
     */
    protected abstract String getAPISiteParam();

    /** If this returns true, only API will be used, otherwise only website. */
    protected abstract boolean useAPI();

    /** Returns value used as password for API requests. */
    protected abstract String getPasswordAPI(final Account account);

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (this.useAPI()) {
            return this.fetchAccountInfoAPI(account);
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

    private AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final Map<String, Object> root = loginAPI(br.cloneBrowser(), account);
        final AccountInfo ac = new AccountInfo();
        final Object trafficLeftO = root.get("balance");
        if (trafficLeftO != null && trafficLeftO.toString().matches("\\d+")) {
            ac.setTrafficLeft(Long.parseLong(trafficLeftO.toString()) * 1024 * 1024);
        } else {
            ac.setTrafficLeft(0);
        }
        /* API */
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

    private Map<String, Object> loginAPI(final Browser br, final Account account) throws IOException, PluginException, InterruptedException {
        final UrlQuery query = new UrlQuery();
        query.add("site", getAPISiteParam());
        query.add("output", "json");
        query.add("loc", "1");
        query.add("info", "1");
        query.add("username", Encoding.urlEncode(account.getUser()));
        query.add("password", getPasswordAPI(account));
        br.postPage(getAPIBase(), query);
        final Map<String, Object> root = this.checkErrorsAPI(br, null, account);
        return root;
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

    private Map<String, Object> checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        Map<String, Object> entries = null;
        try {
            entries = (Map<String, Object>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
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
        return checkErrorsAPI(entries, link, account);
    }

    private final void errorBannedIP(final Account account) throws AccountUnavailableException {
        throw new AccountUnavailableException("Your IP has been banned", 5 * 60 * 1000l);
    }

    private Map<String, Object> checkErrorsAPI(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = "directurl_" + this.getHost();
        final String storedDirecturl = link.getStringProperty("directurl_" + this.getHost());
        final MultiHosterManagement mhm = getMultiHosterManagement();
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            mhm.runCheck(account, link);
            loginWebsite(account, false);
            final String url_urlencoded = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            if (this.useAPI()) {
                final Browser brc = br.cloneBrowser();
                final UrlQuery query = new UrlQuery();
                query.add("site", getAPISiteParam());
                query.add("output", "json");
                // query.add("loc", "1");
                query.add("info", "0");
                query.add("username", Encoding.urlEncode(account.getUser()));
                query.add("password", getPasswordAPI(account));
                query.add("url", url_urlencoded);
                brc.postPage(getAPIBase(), query);
                /* We expect the whole response to be an URL. */
                dllink = brc.getRequest().getHtmlCode();
                try {
                    new URL(dllink);
                } catch (final MalformedURLException e) {
                    /* This should never happen */
                    this.checkErrorsAPI(brc, link, account);
                    mhm.handleErrorGeneric(account, link, "API returned invalid downloadlink", 50);
                }
            } else {
                /* Website */
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
                        dllink = (String) activeDownloadingFileInfo.get("download_url");
                        break;
                    } else if (status.equalsIgnoreCase("initialization")) {
                        this.sleep(3 * 1000l, link);
                        continue;
                    } else {
                        /* Serverside download has never been started or stopped for unknown reasons. */
                        logger.warning("Serverside download failed?!");
                        break;
                    }
                }
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
                this.checkErrorsAPI(br, link, account);
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
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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