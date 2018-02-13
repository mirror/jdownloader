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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filebit.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class FileBitPl extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            APIKEY             = "YWI3Y2E2NWM3OWQxYmQzYWJmZWU3NTRiNzY0OTM1NGQ5ODI3ZjlhNmNkZWY3OGE1MjQ0ZjU4NmM5NTNiM2JjYw==";
    private static String                                  SESSIONID          = null;
    /*
     * 2018-02-13: Their API is broken and only returns 404. Support did not respond which is why we now have website- and API support ...
     */
    private static final boolean                           USE_API            = false;
    /* Default value is 10 */
    private static AtomicInteger                           maxPrem            = new AtomicInteger(10);
    private static MultiHosterManagement                   mhm                = new MultiHosterManagement("filebit.pl");

    public FileBitPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filebit.pl/oferta");
    }

    @Override
    public String getAGBLink() {
        return "http://filebit.pl/regulamin";
    }

    private Browser newBrowserAPI() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 401, 204, 403, 404, 497, 500, 503 });
        return br;
    }

    private Browser newBrowserWebsite() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        br.setCurrentURL(null);
        final int maxChunks = (int) account.getLongProperty("maxconnections", 1);
        link.setProperty("filebitpldirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                mhm.handleErrorGeneric(account, link, "403dlerror", 20);
            }
            br.followConnection();
            if (br.containsHTML("<title>FileBit\\.pl \\- Error</title>")) {
                mhm.handleErrorGeneric(account, link, "dlerror_known", 20);
            }
            mhm.handleErrorGeneric(account, link, "dlerror_unknown", 50);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        String dllink = checkDirectLink(link, "filebitpldirectlink");
        if (StringUtils.isEmpty(dllink)) {
            /* request Download */
            dllink = getDllink(account, link);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 20);
            }
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final Account account, final DownloadLink link) throws Exception {
        final String dllink;
        if (USE_API) {
            dllink = getDllinkAPI(account, link);
        } else {
            dllink = getDllinkWebsite(account, link);
        }
        return dllink;
    }

    private String getDllinkAPI(final Account account, final DownloadLink link) throws Exception {
        this.loginAPI(account);
        br.getPage("http://filebit.pl/api/index.php?a=getFile&sessident=" + SESSIONID + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
        handleAPIErrors(br, account, link);
        // final String expires = getJson("expires");
        return PluginJSonUtils.getJson(this.br, "downloadurl");
    }

    private String getDllinkWebsite(final Account account, final DownloadLink link) throws Exception {
        this.loginWebsite(account);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("/includes/ajax.php", "a=serverNewFile&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&t=" + System.currentTimeMillis());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
        entries = (LinkedHashMap<String, Object>) entries.get("array");
        // final String downloadlink_expires = (String) entries.get("expire");
        // final String internal_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
        return (String) entries.get("download");
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowserAPI();
        if (USE_API) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        br.getPage("http://filebit.pl/api/index.php?a=accountStatus&sessident=" + SESSIONID);
        handleAPIErrors(br, account, null);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        final String premium = PluginJSonUtils.getJson(this.br, "premium");
        if (premium != null && !premium.matches("0|1")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expire = PluginJSonUtils.getJson(this.br, "expires");
        if (expire != null) {
            final Long expirelng = Long.parseLong(expire);
            if (expirelng == -1) {
                ai.setValidUntil(expirelng);
            } else {
                ai.setValidUntil(System.currentTimeMillis() + expirelng);
            }
        }
        final String trafficleft_bytes = PluginJSonUtils.getJson(this.br, "transferLeft");
        if (trafficleft_bytes != null) {
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        int maxSimultanDls = Integer.parseInt(PluginJSonUtils.getJson(this.br, "maxsin"));
        if (maxSimultanDls < 1) {
            maxSimultanDls = 1;
        } else if (maxSimultanDls > 20) {
            maxSimultanDls = 20;
        }
        account.setMaxSimultanDownloads(maxSimultanDls);
        maxPrem.set(maxSimultanDls);
        long maxChunks = Integer.parseInt(PluginJSonUtils.getJson(this.br, "maxcon"));
        if (maxChunks > 1) {
            maxChunks = -maxChunks;
        }
        account.setProperty("maxconnections", maxChunks);
        br.getPage("http://filebit.pl/api/index.php?a=getHostList");
        handleAPIErrors(br, account, null);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("\"hostdomains\":\\[(.*?)\\]").getColumn(0);
        for (final String domains : hostDomains) {
            final String[] realDomains = new Regex(domains, "\"(.*?)\"").getColumn(0);
            for (final String realDomain : realDomains) {
                supportedHosts.add(realDomain);
            }
        }
        if (!"1".equals(premium)) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        newBrowserWebsite();
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        br.getPage("/wykaz");
        account.setConcurrentUsePossible(true);
        final boolean isPremium = br.containsHTML("KONTO <span>PREMIUM</span>");
        if (!isPremium) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        long timeleftMilliseconds = 0;
        final Regex timeleftHoursMinutes = br.getRegex("class=\"name\">(\\d+) godzin, (\\d+) minut</p>");
        final String timeleftDays = br.getRegex("<span class=\"long\" style=\"[^<>\"]+\">(\\d+) DNI</span>").getMatch(0);
        final String timeleftHours = timeleftHoursMinutes.getMatch(0);
        final String timeleftMinutes = timeleftHoursMinutes.getMatch(1);
        if (timeleftDays != null) {
            timeleftMilliseconds += Long.parseLong(timeleftDays) * 24 * 60 * 60 * 1000;
        }
        if (timeleftHours != null) {
            timeleftMilliseconds += Long.parseLong(timeleftHours) * 60 * 60 * 1000;
        }
        if (timeleftMinutes != null) {
            timeleftMilliseconds += Long.parseLong(timeleftMinutes) * 60 * 1000;
        }
        if (timeleftMilliseconds > 0) {
            ai.setValidUntil(System.currentTimeMillis() + timeleftMilliseconds, this.br);
        }
        final Regex trafficRegex = br.getRegex("Pobrano dzisiaj:<br />\\s*?<strong style=\"[^\"]*?\">([^<>\"]+) z <span style=\"[^<>\"]*?\">([^<>\"]+)</span>");
        final String trafficUsedTodayStr = trafficRegex.getMatch(0);
        final String traffixMaxTodayStr = trafficRegex.getMatch(1);
        if (trafficUsedTodayStr != null && traffixMaxTodayStr != null) {
            final long trafficMaxToday = SizeFormatter.getSize(traffixMaxTodayStr);
            ai.setTrafficLeft(trafficMaxToday - SizeFormatter.getSize(trafficUsedTodayStr));
            ai.setTrafficMax(trafficMaxToday);
        } else {
            /* This is wrong but we do not want our plugin to break just because of some missing information. */
            ai.setUnlimitedTraffic();
        }
        if (!isPremium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        final String hostTableText = br.getRegex("<ul class=\"wykazLista\">(.*?)</ul>").getMatch(0);
        if (hostTableText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostInfo = hostTableText.split("<li>");
        for (final String singleHostInfo : hostInfo) {
            String host = new Regex(singleHostInfo, "<b>([^<>\"]+)</b>").getMatch(0);
            final boolean isActive = singleHostInfo.contains("online.png");
            if (StringUtils.isEmpty(host) || !isActive) {
                continue;
            }
            host = host.toLowerCase();
            supportedHosts.add(host);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    // private void login(final Account account) throws IOException, PluginException, InterruptedException {
    // if (USE_API) {
    // loginAPI(account);
    // } else {
    // loginWebsite(account);
    // }
    // }
    private void loginAPI(final Account account) throws IOException, PluginException, InterruptedException {
        newBrowserAPI();
        br.getPage("http://filebit.pl/api/index.php?a=login&apikey=" + Encoding.Base64Decode(APIKEY) + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        handleAPIErrors(br, account, null);
        SESSIONID = PluginJSonUtils.getJson(this.br, "sessident");
        if (SESSIONID == null) {
            // This should never happen
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private boolean isLoggedinHTMLWebsite() {
        return br.containsHTML("class=\"wyloguj\"");
    }

    private void loginWebsite(final Account account) throws IOException, PluginException, InterruptedException {
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            /* Avoid full login whenever possible as it requires a captcha to be solved ... */
            br.setCookies(account.getHoster(), cookies);
            br.getPage("http://" + account.getHoster() + "/");
            if (isLoggedinHTMLWebsite()) {
                return;
            }
            /* Perform full login */
            br.clearCookies(account.getHoster());
        }
        br.getPage("http://" + account.getHoster() + "/");
        Form loginform = null;
        final Form[] forms = br.getForms();
        for (final Form aForm : forms) {
            if (aForm.containsHTML("/panel/login")) {
                loginform = aForm;
                break;
            }
        }
        if (loginform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        String reCaptchaKey = br.getRegex("\\'sitekey\\'\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
        if (reCaptchaKey == null) {
            /* 2018-02-13: Fallback-key */
            reCaptchaKey = "6Lcu5AcUAAAAAC9Hkb6eFqM2P_YLMbI39eYi7KUm";
        }
        final DownloadLink dlinkbefore = this.getDownloadLink();
        if (dlinkbefore == null) {
            this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "http://" + account.getHoster(), true));
        }
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
        if (dlinkbefore != null) {
            this.setDownloadLink(dlinkbefore);
        }
        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(loginform);
        final String redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;URL=(http[^<>\"]+)\" />").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        SESSIONID = this.br.getCookie(this.br.getHost(), "PHPSESSID");
        if (SESSIONID == null || !isLoggedinHTMLWebsite()) {
            // This should never happen
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        account.saveCookies(br.getCookies(account.getHoster()), "");
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException, InterruptedException {
        String statusCode = br.getRegex("\"errno\":(\\d+)").getMatch(0);
        if (statusCode == null && br.containsHTML("\"result\":true")) {
            statusCode = "999";
        } else if (statusCode == null) {
            statusCode = "0";
        }
        String statusMessage = null;
        try {
            int status = Integer.parseInt(statusCode);
            switch (status) {
            case 0:
                /* Everything ok */
                break;
            case 2:
                /* Login or password missing -> disable account */
                statusMessage = "\r\nInvalid account / Ungültiger Account";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* Account invalid -> disable account. */
                statusMessage = "\r\nInvalid account / Ungültiger Account";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 10:
                /* Link offline */
                statusMessage = "Link offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 11:
                /* Host not supported -> Remove it from hostList */
                statusMessage = "Host not supported";
                tempUnavailableHoster(account, downloadLink, 3 * 60 * 60 * 1000l);
            case 12:
                /* Host offline -> Disable for 5 minutes */
                statusMessage = "Host offline";
                tempUnavailableHoster(account, downloadLink, 5 * 60 * 1000l);
            case 101:
                /* MOCH server maintenance -> Disable for 5 minutes */
                statusMessage = "MOCH server maintenance";
                tempUnavailableHoster(account, downloadLink, 5 * 60 * 1000l);
            default:
                /* unknown error, do not try again with this multihoster */
                statusMessage = "Unknown API error code, please inform JDownloader Development Team";
                mhm.handleErrorGeneric(account, downloadLink, "unknown_api_error", 20);
            }
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}