//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.TypeRef;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "https?://(?:www\\.)?filer\\.net/(?:app\\.php/)?(?:get|dl)/([a-z0-9]+)" })
public class FilerNet extends PluginForHost {
    private int                 statusCode                                             = 0;
    private String              statusMessage                                          = null;
    private static final int    STATUSCODE_APIDISABLED                                 = 400;
    private static final String ERRORMESSAGE_APIDISABLEDTEXT                           = "API is disabled, please wait or use filer.net from your browser";
    private static final int    STATUSCODE_DOWNLOADTEMPORARILYDISABLED                 = 500;
    private static final String ERRORMESSAGE_DOWNLOADTEMPORARILYDISABLEDTEXT           = "Download temporarily disabled!";
    private static final int    STATUSCODE_UNKNOWNERROR                                = 599;
    private static final String ERRORMESSAGE_UNKNOWNERRORTEXT                          = "Unknown file error";
    private static final String DIRECT_WEB                                             = "directlinkWeb";
    private static final String DIRECT_API                                             = "directlinkApi";
    private static final String SETTING_ENABLE_API_FOR_FREE_AND_FREE_ACCOUNT_DOWNLOADS = "ENABLE_API_FOR_FREE_AND_FREE_ACCOUNT_DOWNLOADS";
    private static final String DISABLE_HTTPS                                          = "DISABLE_HTTPS";
    private static final String SETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS                  = "WAIT_MINUTES_ON_NO_FREE_SLOTS";
    private static final int    defaultSETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS           = 10;
    // API Docs: https://filer.net/api
    public static final String  API_BASE                                               = "https://api.filer.net/api";                                      // "https://filer.net/api";
    public static final String  BASE                                                   = "https://filer.net";                                              // "https://filer.net/api";

    @SuppressWarnings("deprecation")
    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filer.net/upgrade");
        this.setStartIntervall(2000l);
        setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser() {
            @Override
            public URLConnectionAdapter openRequestConnection(Request request, final boolean followRedirects) throws IOException {
                /**
                 * 2024-02-20: Ensure to enforce user-preferred protocol. </br> This can also be seen as a workaround since filer.net
                 * redirects from https to http on final download-attempt so without this, http protocol would be used even if user
                 * preferred https. Atm we don't know if this is a filer.net serverside bug or if this is intentional. Asked support about
                 * this, waiting for feedback
                 */
                request.setURL(new URL(rewriteProtocol(request.getURL().toExternalForm())));
                return super.openRequestConnection(request, followRedirects);
            }

            @Override
            public Browser createNewBrowserInstance() {
                return FilerNet.this.createNewBrowserInstance();
            }
        };
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filer.net" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:app\\.php/)?(?:get|dl)/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        final String url = rewriteProtocol("https://" + this.getHost() + "/get/" + getFileID(link));
        link.setUrlDownload(url);
    }

    public String getAPI_BASE() {
        return rewriteProtocol(API_BASE);
    }

    public String rewriteProtocol(String url) {
        if (this.getPluginConfig().getBooleanProperty(DISABLE_HTTPS, false)) {
            return url.replaceFirst("^https://", "http://");
        } else {
            return url.replaceFirst("^http://", "https://");
        }
    }

    private final String getFileID(DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String fileID = getFileID(link);
        if (fileID != null) {
            return getHost() + "://" + fileID;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://filer.net/agb.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        callAPI(null, getAPI_BASE() + "/status/" + getFID(link) + ".json");
        if (statusCode == STATUSCODE_APIDISABLED) {
            link.getLinkStatus().setStatusText(ERRORMESSAGE_APIDISABLEDTEXT);
            return AvailableStatus.UNCHECKABLE;
        } else if (statusCode == 505) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (statusCode == STATUSCODE_DOWNLOADTEMPORARILYDISABLED) {
            link.getLinkStatus().setStatusText(ERRORMESSAGE_DOWNLOADTEMPORARILYDISABLEDTEXT);
        } else if (statusCode == STATUSCODE_UNKNOWNERROR) {
            link.getLinkStatus().setStatusText(ERRORMESSAGE_UNKNOWNERRORTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        link.setFinalFileName(PluginJSonUtils.getJson(br, "name"));
        link.setDownloadSize(Long.parseLong(PluginJSonUtils.getJson(br, "size")));
        /* hash != md5, its the hash of fileID */
        link.setMD5Hash(null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        doFree(null, link);
    }

    @SuppressWarnings({ "deprecation" })
    public void doFree(final Account account, final DownloadLink downloadLink) throws Exception {
        if (this.getPluginConfig().getBooleanProperty(SETTING_ENABLE_API_FOR_FREE_AND_FREE_ACCOUNT_DOWNLOADS, true)) {
            doFreeAPI(account, downloadLink);
        } else {
            doFreeWebsite(account, downloadLink);
        }
    }

    private void doFreeAPI(final Account account, final DownloadLink link) throws Exception {
        handleDownloadErrors();
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        String dllink = checkDirectLink(link, DIRECT_API);
        if (dllink == null) {
            callAPI(null, rewriteProtocol(BASE) + "/get/" + getFID(link) + ".json");
            handleErrorsAPI(account);
            if (statusCode == 203) {
                // they can repeat this twice
                int i = 0;
                do {
                    final String token = PluginJSonUtils.getJson(br, "token");
                    if (token == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final int wait = getWait();
                    sleep(wait * 1001l + 1000l, link);
                    callAPI(null, rewriteProtocol(BASE) + "/get/" + getFID(link) + ".json?token=" + token);
                    // they can make you wait again...
                    handleErrorsAPI(account);
                } while (statusCode == 203 && ++i <= 2);
            }
            if (statusCode == 202) {
                int maxCaptchaTries = 4;
                int tries = 0;
                while (tries <= maxCaptchaTries) {
                    tries++;
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdB1kcUAAAAAAVPepnD-6TEd4BXKzS7L4FZFkpO");
                    final String recaptchaV2Response = rc2.getToken();
                    if (recaptchaV2Response == null) {
                        continue;
                    }
                    br.setFollowRedirects(false);
                    br.postPage(rewriteProtocol(BASE) + "/get/" + getFID(link) + ".json", "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&hash=" + getFID(link));
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        updateStatuscode();
                        if (statusCode == 501) {
                            errorNoFreeSlotsAvailable();
                        } else if (statusCode == 502) {
                            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 1 * 60 * 1000l);
                        } else {
                            continue;
                        }
                    } else {
                        break;
                    }
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
        }
        /* This should never happen! */
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            handleErrors(account, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        link.setProperty(DIRECT_API, dl.getConnection().getURL().toString());
        this.dl.startDownload();
    }

    private void errorNoFreeSlotsAvailable() throws PluginException {
        final int waitMinutes = this.getPluginConfig().getIntegerProperty(SETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS, defaultSETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS);
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", waitMinutes * 60 * 1000l);
    }

    private void doFreeWebsite(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, DIRECT_WEB);
        if (dllink == null) {
            br.getPage(link.getPluginPatternMatcher());
            handleErrors(account, false);
            Form continueForm = br.getFormbyKey("token");
            if (continueForm != null) {
                /* Captcha is not always required! */
                int wait = 60;
                final String waittime_str = br.getRegex("id=\"time\">(\\d+)<").getMatch(0);
                if (waittime_str != null) {
                    wait = Integer.parseInt(waittime_str);
                }
                sleep(wait * 1001l, link);
                br.submitForm(continueForm);
            }
            int maxCaptchaTries = 4;
            int tries = 0;
            while (tries <= maxCaptchaTries) {
                logger.info(String.format("Captcha loop %d of %d", tries + 1, maxCaptchaTries + 1));
                continueForm = br.getFormbyKey("hash");
                if (continueForm == null) {
                    handleErrors(account, false);
                    logger.info("Failed to find continueForm");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                final String recaptchaV2Response = rc2.getToken();
                if (recaptchaV2Response == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                continueForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.setFollowRedirects(false);
                br.submitForm(continueForm);
                dllink = br.getRedirectLocation();
                if (dllink != null) {
                    break;
                }
                tries++;
                continue;
            }
            if (dllink == null && this.br.containsHTML("data-sitekey")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (dllink == null) {
                /* This should never happen! */
                handleErrors(account, false);
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            handleErrors(account, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        link.setProperty(DIRECT_WEB, dl.getConnection().getURL().toString());
        this.dl.startDownload();
    }

    private int getWait() {
        final String tiaw = PluginJSonUtils.getJson(br, "wait");
        if (tiaw != null) {
            return Integer.parseInt(tiaw);
        } else {
            return 15;
        }
    }

    /** Prepares browser for website (=non-API) requests. */
    private Browser prepBrowserWebsite(final Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.73.11 (KHTML, like Gecko) Version/7.0.1 Safari/537.73.11");
        return prepBr;
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    public void loginAPI(final Account account) throws Exception {
        synchronized (account) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            callAPI(account, getAPI_BASE() + "/profile.json");
        }
    }

    /* E.g. used for free account downloads */
    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowserWebsite(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://" + this.getHost());
                    if (this.isLoggedInWebsite(br)) {
                        logger.info("Successfully logged in via cookies");
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                this.br.getPage("https://" + this.getHost() + "/login");
                final Form loginform = this.br.getFormbyKey("_username");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin broken, please contact the JDownloader Support!");
                    }
                }
                loginform.put("_username", Encoding.urlEncode(account.getUser()));
                loginform.put("_password", Encoding.urlEncode(account.getPass()));
                loginform.remove("_remember_me");
                loginform.put("_remember_me", "on");
                this.br.submitForm(loginform);
                if (!isLoggedInWebsite(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username or password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedInWebsite(final Browser br) {
        if (br.getCookie(this.getHost(), "REMEMBERME", Cookies.NOTDELETEDPATTERN) != null && br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        loginAPI(account);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        if (data.get("state").toString().equalsIgnoreCase("free")) {
            account.setType(AccountType.FREE);
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.PREMIUM);
            final Long trafficLeft = (Long) ReflectionUtils.cast(data.get("traffic"), Long.class);
            ai.setTrafficLeft(trafficLeft.longValue());
            final Object maxtrafficObject = data.get("maxtraffic");
            if (maxtrafficObject != null) {
                final Long maxtrafficValue = (Long) ReflectionUtils.cast(maxtrafficObject, Long.class);
                ai.setTrafficMax(maxtrafficValue.longValue());
            } else {
                ai.setTrafficMax(SizeFormatter.getSize("125gb"));// fallback
            }
            final Long validUntil = (Long) ReflectionUtils.cast(data.get("until"), Long.class);
            ai.setValidUntil(validUntil.longValue() * 1000);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setBrowserExclusive();
        if (account.getType() == AccountType.FREE) {
            loginWebsite(account, false);
            requestFileInformation(link);
            doFree(account, link);
        } else {
            requestFileInformation(link);
            handleDownloadErrors();
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            br.setFollowRedirects(false);
            callAPI(account, getAPI_BASE() + "/dl/" + getFID(link) + ".json");
            if (statusCode == 504) {
                if (StringUtils.isEmpty(statusMessage)) {
                    throw new AccountUnavailableException("Traffic limit reached", 60 * 60 * 1000l);
                } else {
                    throw new AccountUnavailableException(statusMessage, 60 * 60 * 1000l);
                }
            } else if (statusCode == STATUSCODE_UNKNOWNERROR) {
                throw new PluginException(LinkStatus.ERROR_FATAL, ERRORMESSAGE_UNKNOWNERRORTEXT);
            }
            final String dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Important!! */
            br.getHeaders().put("Authorization", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            /* 2021-09-20: Error 500 may happen quite often. */
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                this.handleErrors(account, true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.setAllowFilenameFromURL(true);
            this.dl.startDownload();
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
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void handleErrors(final Account account, final boolean afterDownload) throws PluginException {
        // Temporary errorhandling for a bug which isn't handled by the API
        final String errorcodeStr = new Regex(br.getURL(), "(?i).+/error/(\\d+)").getMatch(0);
        if (errorcodeStr != null) {
            final int errorcode = Integer.parseInt(errorcodeStr);
            if (errorcode == 415) {
                /* 2024-02-09: This error may happen frequently thus I've lowered the wait time. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 415", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error " + errorcodeStr, 15 * 60 * 1000l);
            }
        }
        if (afterDownload && br.containsHTML("filer\\.net/register")) {
            errorNoFreeSlotsAvailable();
        } else if (br.containsHTML("(?i)>\\s*Maximale Verbindungen erreicht")) {
            errorNoFreeSlotsAvailable();
        } else if (br.containsHTML("(?i)>\\s*Leider sind alle kostenlosen Download-Slots belegt|Im Moment sind leider alle Download-Slots für kostenlose Downloads belegt|Bitte versuche es später erneut oder behebe das Problem mit einem Premium")) {
            /* 2020-05-01 */
            errorNoFreeSlotsAvailable();
        }
        if (br.containsHTML("(?i)>\\s*Free Download Limit erreicht\\s*<")) {
            final String time = br.getRegex("<span id=\"time\">(\\d+)<").getMatch(0);
            if (account != null) {
                if (time != null) {
                    throw new AccountUnavailableException("Limit reached", (Integer.parseInt(time) + 60) * 1000l);
                } else {
                    throw new AccountUnavailableException("Limit reached", 60 * 60 * 1000l);
                }
            } else {
                if (time != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Free limit reached", (Integer.parseInt(time) + 60) * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Free limit reached", 60 * 60 * 1000l);
                }
            }
        }
        if (afterDownload) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content", 3 * 60 * 1000l);
        }
    }

    private void handleDownloadErrors() throws PluginException {
        if (statusCode == STATUSCODE_APIDISABLED) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, ERRORMESSAGE_APIDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (statusCode == STATUSCODE_DOWNLOADTEMPORARILYDISABLED) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ERRORMESSAGE_DOWNLOADTEMPORARILYDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (statusCode == STATUSCODE_UNKNOWNERROR) {
            throw new PluginException(LinkStatus.ERROR_FATAL, ERRORMESSAGE_UNKNOWNERRORTEXT);
        }
    }

    private void handleErrorsAPI(final Account account) throws PluginException {
        if (statusCode == 501) {
            errorNoFreeSlotsAvailable();
        } else if (statusCode == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 1 * 60 * 1000l);
        } else if (statusCode == 504) {
            if (account == null || AccountType.FREE.equals(account.getType())) {
                throw new AccountRequiredException(statusMessage);
            } else {
                if (StringUtils.isEmpty(statusMessage)) {
                    throw new AccountUnavailableException("Traffic limit reached", 60 * 60 * 1000l);
                } else {
                    throw new AccountUnavailableException(statusMessage, 60 * 60 * 1000l);
                }
            }
        }
        // 203 503 wait
        final int wait = getWait();
        if (statusCode == 503) {
            // Waittime too small->Don't reconnect
            if (wait < 61) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads...", wait * 1001l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
            }
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([a-z0-9]+)$").getMatch(0);
    }

    private void callAPI(final Account account, final String url) throws Exception {
        final URLConnectionAdapter con = br.openGetConnection(url);
        if (con.getResponseCode() == 401 && account != null) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.followConnection();
        updateStatuscode();
    }

    private void updateStatuscode() {
        final String code = PluginJSonUtils.getJson(br, "code");
        statusMessage = PluginJSonUtils.getJson(br, "status");
        if ("hour download limit reached".equals(code)) {
            statusCode = 503;
        } else if ("user download slots filled".equals(code)) {
            statusCode = 501; // unsure if 501 or 502
        } else if ("file captcha input needed".equals(code)) {
            statusCode = 202;
        } else if ("file wait needed".equals(code)) {
            statusCode = 203;
        } else if (code != null) {
            statusCode = Integer.parseInt(code);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty("errorFailure");
        link.removeProperty("directlinkWeb");
        link.removeProperty("directlinkApi");
    }

    @Override
    public String getDescription() {
        return "Download files with the filer.net plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ENABLE_API_FOR_FREE_AND_FREE_ACCOUNT_DOWNLOADS, "Enable API for free- and free account downloads?\r\nBy disabling this you will force JD to use the website instead.\r\nThis could lead to unexpected errors.").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DISABLE_HTTPS, "Use HTTP instead of HTTPS").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), SETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS, "Wait minutes on error 'no free slots available'", 1, 600, 1).setDefaultValue(defaultSETTING_WAIT_MINUTES_ON_NO_FREE_SLOTS));
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null || !AccountType.PREMIUM.equals(acc.getType())) {
            /* no/free account, yes we can expect captcha */
            return true;
        } else {
            return false;
        }
    }
}