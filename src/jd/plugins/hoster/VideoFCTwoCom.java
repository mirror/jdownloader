//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class VideoFCTwoCom extends PluginForHost {
    public VideoFCTwoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fc2.com");
        setConfigElements();
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "video.fc2.com", "xiaojiadianvideo.asia", "jinniumovie.be" });
        ret.add(new String[] { "video.laxd.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://" + buildHostsPatternPart(domains) + "/((?:[a-z]{2}/)?(?:a/)?flv2\\.swf\\?i=|(?:[a-z]{2}/)?(?:a/)?content/)\\w+");
        }
        return ret.toArray(new String[0]);
    }

    private String              httpDownloadurl              = null;
    private String              hlsMaster                    = null;
    private String              hlsDownloadurl               = null;
    private String              trailerURL                   = null;
    private static final String SETTING_fastLinkCheck        = "fastLinkCheck";
    private static final String SETTING_allowTrailerDownload = "allowTrailerDownload";
    private final boolean       default_SETTING_fastLinkCheck        = true;
    private final boolean       default_allowTrailerDownload = false;
    private static final String PROPERTY_PREMIUMONLY         = "PREMIUMONLY";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_fastLinkCheck, "Enable fastlinkch?\r\nFilesize won't be displayed until download is started.").setDefaultValue(default_SETTING_fastLinkCheck));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_allowTrailerDownload, "Download trailer if full video is not available?").setDefaultValue(default_allowTrailerDownload));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(?:i=|content/)(.+)").getMatch(0);
    }

    private Browser prepareBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
        br.setCustomCharset("utf-8");
        return br;
    }

    @Override
    public String getAGBLink() {
        return "http://help.fc2.com/common/tos/en/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (this.getHost().equals("video.fc2.com")) {
            final boolean subContent = new Regex(link.getPluginPatternMatcher(), "/a/content/").matches();
            link.setPluginPatternMatcher("https://video.fc2.com/en/" + (subContent ? "a/content/" : "content/") + new Regex(link.getPluginPatternMatcher(), "([A-Za-z0-9]+)/?$").getMatch(0));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + ".mp4");
        }
        /* Login if account is available. */
        if (account != null) {
            this.login(account, true, link.getPluginPatternMatcher());
        } else {
            prepareBrowser(br);
            br.setFollowRedirects(true);
            br.getPage(link.getPluginPatternMatcher());
        }
        if (isOffline(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String newAPIVideotoken = br.getRegex("\\'ae\\'\\s*?,\\s*?\\'([a-f0-9]{32})\\'").getMatch(0);
        if (newAPIVideotoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* video.fc2.com */
        br.getHeaders().put("X-FC2-Video-Access-Token", newAPIVideotoken);
        /* video.laxd.com */
        br.getHeaders().put("X-LAXD-Video-Access-Token", newAPIVideotoken);
        br.getPage("https://" + this.getHost() + "/api/v3/videoplayer/" + fid + "?" + newAPIVideotoken + "=1&tk=&fs=0");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filenamePrefix = "";
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String title = (String) entries.get("title");
        final String uploadername = (String) JavaScriptEngineFactory.walkJson(entries, "owner/name");
        br.getPage("/api/v3/videoplaylist/" + fid + "?sh=1&fs=0");
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> playlist = (Map<String, Object>) entries.get("playlist");
        /* HTTP Streams */
        final String[] qualitiesInBestOrder = new String[] { "hq", "nq", "lq" };
        for (final String possibleQuality : qualitiesInBestOrder) {
            this.httpDownloadurl = (String) playlist.get(possibleQuality);
            if (!StringUtils.isEmpty(this.httpDownloadurl)) {
                break;
            }
        }
        /* HLS streams */
        this.hlsMaster = (String) playlist.get("master");
        /* Trailer -> Also http stream */
        this.trailerURL = (String) playlist.get("sample");
        if (StringUtils.isEmpty(this.httpDownloadurl) && StringUtils.isEmpty(this.hlsMaster) && !StringUtils.isEmpty(this.trailerURL) && this.getPluginConfig().getBooleanProperty(SETTING_allowTrailerDownload, default_allowTrailerDownload)) {
            logger.info("Trailer download is allowed and trailer is available");
            /* Trailers are always available as http streams */
            this.httpDownloadurl = this.trailerURL;
            filenamePrefix = "TRAILER_";
        }
        if (!StringUtils.isEmpty(title)) {
            String filename;
            if (!StringUtils.isEmpty(uploadername)) {
                filename = uploadername + "_" + title;
            } else {
                filename = title;
            }
            filename = filenamePrefix + filename;
            filename = filename.replaceAll("\\p{Z}", " ");
            // why do we do this?? http://board.jdownloader.org/showthread.php?p=304933#post304933
            // filename = filename.replaceAll("[\\.\\d]{3,}$", "");
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = filename.replaceAll("(:|,|\\s)", "_");
            filename += ".mp4";
            link.setFinalFileName(filename);
        }
        if (isDownload || !this.getPluginConfig().getBooleanProperty(SETTING_fastLinkCheck, default_SETTING_fastLinkCheck) && !StringUtils.isEmpty(httpDownloadurl)) {
            br.getHeaders().put("Referer", null);
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(httpDownloadurl);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else if (con.getContentType().contains("application/vnd.apple.mpegurl")) {
                    /* HLS download */
                    this.hlsDownloadurl = con.getURL().toString();
                    this.httpDownloadurl = null;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?");
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    /*
     * IMPORTANT NOTE: Free (unregistered) Users can watch (&download) videos up to 2 hours in length - if videos are longer, users can only
     * watch the first two hours of them - afterwards they will get this message: https://i.snipboard.io/FGl1E.jpg
     */
    private void login(final Account account, final boolean verifyCookies, final String checkURL) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            if (!StringUtils.contains(account.getUser(), "@")) {
                throw new AccountInvalidException("Please enter your E-Mail address as username!");
            }
            prepareBrowser(this.br);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Cookies storedCookies = account.loadCookies("");
            final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass(), getLogger());
            if (storedCookies != null || userCookies != null) {
                if (userCookies != null) {
                    this.br.setCookies(userCookies);
                } else {
                    this.br.setCookies(storedCookies);
                }
                if (!verifyCookies) {
                    logger.info("Trust cookies without login");
                    return;
                } else {
                    logger.info("Attempting cookie login");
                    br.getPage(checkURL);
                    if (isLoggedINVideoFC2(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            /* User has provided cookies instead of password --> No full login possible! */
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException("Login cookies expired");
                            } else {
                                throw new AccountInvalidException("Login cookies invalid");
                            }
                        }
                    }
                }
            }
            logger.info("Performing full login");
            final boolean useAltLogin = true;
            final Form loginform;
            if (useAltLogin) {
                /* Alternative login way: Possibly less captchas when using this one */
                br.getPage("https://fc2.com/en/login.php?ref=video&switch_language=en");
                loginform = br.getFormbyProperty("name", "form_login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                if (loginform.hasInputFieldByName("image")) {
                    // loginform.put("Submit.x", new Random().nextInt(100) + "");
                    // loginform.put("Submit.y", new Random().nextInt(100) + "");
                    loginform.put("image.x", new Random().nextInt(100) + "");
                    loginform.put("image.y", new Random().nextInt(100) + "");
                    loginform.remove("image");
                }
                /* Make sure cookies are valid for as long as possible. */
                loginform.put("keep_login", "1");
                final boolean skipCaptcha = loginform.hasInputFieldByName("recaptchaep");
                if (loginform.hasInputFieldByName("recaptcha") && !skipCaptcha) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("recaptchaep", Encoding.urlEncode(recaptchaV2Response));
                }
            } else {
                /* 2021-08-06 */
                br.getHeaders().put("Referer", "https://video.fc2.com/");
                br.getPage("https://secure.id.fc2.com/index.php?mode=login&done=video");
                // br.getPage("https://secure.id.fc2.com/index.php?mode=login");
                loginform = br.getFormbyProperty("name", "form_login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("email", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                /* Make sure cookies are valid for as long as possible. */
                loginform.put("keep_login", "1");
                loginform.remove("Submit");
                if (loginform.hasInputFieldByName("recaptcha")) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("recaptcha", Encoding.urlEncode(recaptchaV2Response));
                }
            }
            // br.getHeaders().put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"");
            // br.getHeaders().put("sec-ch-ua-mobile", "\"Windows\"");
            // br.getHeaders().put("Origin", "https://fc2.com");
            // br.getHeaders().put("Accept",
            // "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            // br.getHeaders().put("Sec-Fetch-Site", "same-site");
            // br.getHeaders().put("Sec-Fetch-Mode", "navigate");
            // br.getHeaders().put("Sec-Fetch-User", "?1");
            // br.getHeaders().put("Sec-Fetch-Dest", "document");
            // br.getHeaders().put("Referer", "https://fc2.com/");
            final String loginCookieKey = "fclo";
            /* Important! Without this cookie we won't be able to login!! */
            if (br.getCookie(br.getHost(), loginCookieKey, Cookies.NOTDELETEDPATTERN) == null) {
                br.setCookie(br.getHost(), loginCookieKey, System.currentTimeMillis() + "%2C" + Locale.getDefault().getLanguage());
            }
            br.submitForm(loginform);
            if (br.getURL().contains("error=1")) {
                /* Login failed */
                throw new AccountInvalidException();
            }
            {
                /*
                 * TODO: 2020-12-17: Check 2FA login handling below as it is untested.
                 */
                /* 2021-01-04: Small workaround for bad redirect to wrong page on 2FA login required */
                final boolean required2FALogin = br.getRedirectLocation() != null && br.getRedirectLocation().contains("login_authentication.php");
                if (!br.getURL().contains("login_authentication.php") && required2FALogin) {
                    br.getPage("https://secure.id.fc2.com/login_authentication.php");
                }
                final Form twoFactorLogin = br.getFormbyActionRegex(".*login_authentication\\.php.*");
                if (twoFactorLogin != null) {
                    logger.info("2FA login required");
                    final DownloadLink dl_dummy;
                    if (this.getDownloadLink() != null) {
                        dl_dummy = this.getDownloadLink();
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                    }
                    String twoFACode = getUserInput("Enter Google 2-Factor Authentication code?", dl_dummy);
                    if (twoFACode != null) {
                        twoFACode = twoFACode.trim();
                    }
                    if (twoFACode == null || !twoFACode.matches("[A-Za-z0-9]{6}")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiges Format der 2-faktor-Authentifizierung!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid 2-factor-authentication code format!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    logger.info("Submitting 2FA code");
                    twoFactorLogin.put("code", twoFACode);
                    br.submitForm(twoFactorLogin);
                }
            }
            /*
             * If everything goes as planned, we should be redirected to video.fc2.com but in case we're not we'll also check logged in
             * state for their main portal fc2.com.
             */
            if (br.getHost(true).equals("fc2.com")) {
                logger.info("Automatic redirect to video.fc2.com after login failed");
                if (!isLoggedINFC2(br)) {
                    throw new AccountInvalidException();
                }
                br.getPage(checkURL);
            } else {
                logger.info("Automatic redirect to video.fc2.com after login successful");
            }
            if (!br.getURL().equals(checkURL) && !isLoggedINVideoFC2(br)) {
                logger.info("Accessing target URL: " + checkURL);
                br.getPage(checkURL);
            }
            if (!isLoggedINVideoFC2(br)) {
                /* This should never happen */
                throw new AccountInvalidException("fc2 Account seems to be valid but 'video.fc2.com' login failed.");
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
        }
    }

    private boolean isLoggedINFC2(final Browser br) {
        if (br.containsHTML("/logout\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoggedINVideoFC2(final Browser br) {
        if (br.containsHTML("/logoff\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            this.login(account, true, "https://video.fc2.com/");
            /* Switch to english language */
            final String relativeURL_Payment = "/payment/fc2_premium/";
            final String userSelectedLanguage = br.getCookie(this.getHost(), "language", Cookies.NOTDELETEDPATTERN);
            if (!StringUtils.equalsIgnoreCase(userSelectedLanguage, "en")) {
                br.getHeaders().put("Referer", "https://" + this.br.getHost(true) + relativeURL_Payment);
                br.getPage("/a/language_change.php?lang=en");
            }
            if (!br.getURL().contains(relativeURL_Payment)) {
                br.getPage(relativeURL_Payment);
            }
            /* Check for multiple traits - we want to make sure that we correctly recognize premium accounts! */
            boolean isPremium = br.containsHTML("class=\"c-header_main_mamberType\"[^>]*><span[^>]*>Premium|>\\s*Contract Extension|>\\s*Premium Member account information");
            String expire = br.getRegex("(\\d{4}/\\d{2}/\\d{2})[^>]*Automatic renewal date").getMatch(0);
            if (!isPremium && expire != null) {
                isPremium = true;
            }
            if (isPremium) {
                /* Only set expire date if we find one */
                if (expire != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd", Locale.ENGLISH), this.br);
                }
                ai.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            ai.setUnlimitedTraffic();
            /* Switch back to users' previously set language if that != en */
            if (userSelectedLanguage != null && !userSelectedLanguage.equalsIgnoreCase("en")) {
                logger.info("Switching back to users preferred language: " + userSelectedLanguage);
                br.getPage("/a/language_change.php?lang=" + userSelectedLanguage);
            }
            return ai;
        }
    }

    private void doDownload(final Account account, final DownloadLink link) throws Exception {
        /* OLD-API handling */
        final String error = br.getRegex("^err_code=(\\d+)").getMatch(0);
        if (error != null) {
            switch (Integer.parseInt(error)) {
            case 503:
                // :-)
                break;
            case 601:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 602:
                /* reconnect */
                logger.info("video.fc2.com: reconnect is needed!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 603:
                link.setProperty(PROPERTY_PREMIUMONLY, true);
                break;
            default:
                logger.info("video.fc2.com: Unknown error code: " + error);
            }
        }
        link.removeProperty(PROPERTY_PREMIUMONLY);
        if (StringUtils.isEmpty(this.httpDownloadurl) && StringUtils.isEmpty(this.hlsMaster) && StringUtils.isEmpty(this.hlsDownloadurl)) {
            if (!StringUtils.isEmpty(this.trailerURL)) {
                /* Even premium accounts won't be able to watch such content - it has to be bought separately! */
                logger.info("This content needs to be purchased individually otherwise only a trailer is available!");
                // throw new AccountRequiredException();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Only trailer available. Enable trailer download in settings to allow trailer downloads.");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (onlyForPremiumUsers(link)) {
            throw new AccountRequiredException();
        }
        /* Only download HLS streams if no http download is available */
        if (StringUtils.isEmpty(this.httpDownloadurl)) {
            /* hls download */
            if (StringUtils.isEmpty(this.hlsDownloadurl)) {
                br.getPage(this.hlsMaster);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                this.hlsDownloadurl = hlsbest.getDownloadurl();
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, this.hlsDownloadurl);
            dl.startDownload();
        } else {
            /* http download */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.httpDownloadurl, true, -4);
            if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && requestHeadersHasKeyNValueContains(br, "server", "nginx")) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Service unavailable. Try again later.", 5 * 60 * 1000l);
            } else if (!looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML("not found")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null, true);
        doDownload(null, link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        doDownload(account, link);
    }

    private boolean isOffline(final String contentID) {
        return br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("err.php") || !br.getURL().contains(contentID) || br.toString().contains("(Removed) **************");
    }

    private String getMimi(String s) {
        return JDHash.getMD5(s + "_" + "gGddgPfeaf_gzyr");
    }

    private String getKey() {
        String javaScript = br.getRegex("eval(\\(f.*?)[\r\n]+").getMatch(0);
        if (javaScript == null) {
            javaScript = br.getRegex("(var __[0-9a-zA-Z]+ = \'undefined\'.*?\\})[\r\n]+\\-\\->").getMatch(0);
        }
        if (javaScript == null) {
            return null;
        }
        Object result = new Object();
        ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        try {
            if (!javaScript.startsWith("var")) {
                engine.eval(engine.eval(javaScript).toString());
            }
            engine.eval(javaScript);
            engine.eval("var window = new Object();");
            result = inv.invokeFunction("getKey");
        } catch (Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

    private void prepareFinalLink() {
        if (httpDownloadurl != null) {
            httpDownloadurl = httpDownloadurl.replaceAll("\\&mid=", "?mid=");
            String t = new Regex(httpDownloadurl, "cdnt=(\\d+)").getMatch(0);
            String h = new Regex(httpDownloadurl, "cdnh=([0-9a-f]+)").getMatch(0);
            httpDownloadurl = new Regex(httpDownloadurl, "(.*?)\\&sec=").getMatch(0);
            if (t != null && h != null) {
                httpDownloadurl = httpDownloadurl + "&px-time=" + t + "&px-hash=" + h;
            }
        }
    }

    private boolean onlyForPremiumUsers(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_PREMIUMONLY)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * If import Browser request headerfield contains key of k && key value of v
     *
     * @author raztoki
     */
    private boolean requestHeadersHasKeyNValueContains(final Browser ibr, final String k, final String v) {
        if (k == null || v == null || ibr == null || ibr.getHttpConnection() == null) {
            return false;
        } else if (ibr.getHttpConnection().getHeaderField(k) != null && ibr.getHttpConnection().getHeaderField(k).toLowerCase(Locale.ENGLISH).contains(v.toLowerCase(Locale.ENGLISH))) {
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
}