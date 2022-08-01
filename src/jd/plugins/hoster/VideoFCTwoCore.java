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
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
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
public abstract class VideoFCTwoCore extends PluginForHost {
    public VideoFCTwoCore(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private String              httpDownloadurl               = null;
    private String              hlsMaster                     = null;
    private String              hlsDownloadurl                = null;
    private String              trailerURL                    = null;
    private static final String SETTING_fastLinkCheck         = "fastLinkCheck";
    private static final String SETTING_allowTrailerDownload  = "allowTrailerDownload";
    private final boolean       default_SETTING_fastLinkCheck = true;
    private final boolean       default_allowTrailerDownload  = false;
    private static final String PROPERTY_PREMIUMONLY          = "PREMIUMONLY";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_fastLinkCheck, "Enable fast linkcheck?\r\nFilesize won't be displayed until download is started.").setDefaultValue(default_SETTING_fastLinkCheck));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_allowTrailerDownload, "Download trailer if full video is not available?").setDefaultValue(default_allowTrailerDownload));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    protected String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return new Regex(link.getPluginPatternMatcher(), "(?:i=|content/)(.+)").getMatch(0);
        }
    }

    private Browser prepareBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
        br.setCustomCharset("utf-8");
        return br;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
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
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new AccountRequiredException();
        } else if (isOffline(fid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String newAPIVideotoken = br.getRegex("\\'ae\\'\\s*?,\\s*?\\'([a-f0-9]{32})\\'").getMatch(0);
        if (newAPIVideotoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* video.fc2.com */
        if (this.getHost().contains("fc2.com")) {
            br.getHeaders().put("X-FC2-Video-Access-Token", newAPIVideotoken);
        } else {
            /* video.laxd.com */
            br.getHeaders().put("X-LAXD-Video-Access-Token", newAPIVideotoken);
        }
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
        String chosenQuality = null;
        final String[] qualitiesInBestOrder = new String[] { "hq", "nq", "lq" };
        for (final String possibleQuality : qualitiesInBestOrder) {
            this.httpDownloadurl = (String) playlist.get(possibleQuality);
            if (!StringUtils.isEmpty(this.httpDownloadurl)) {
                chosenQuality = possibleQuality;
                break;
            }
        }
        if (chosenQuality != null) {
            logger.info("Chosen quality: " + chosenQuality);
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
        if (!StringUtils.isEmpty(httpDownloadurl) && (isDownload || !this.getPluginConfig().getBooleanProperty(SETTING_fastLinkCheck, default_SETTING_fastLinkCheck))) {
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
                    checkErrorsNoFileLastResort(con);
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
    protected void login(final Account account, final boolean verifyCookies, final String checkURL) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            prepareBrowser(this.br);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Cookies storedCookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (storedCookies != null || userCookies != null) {
                if (userCookies != null) {
                    this.br.setCookies(userCookies);
                } else if (storedCookies != null) {
                    this.br.setCookies(storedCookies);
                }
                if (!verifyCookies) {
                    logger.info("Trust cookies without login");
                    return;
                } else {
                    logger.info("Attempting cookie login");
                    br.getPage(checkURL);
                    if (isLoggedINVideoSubdomain(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        /*
                         * When logging in via cookies user can enter whatever he wants in username field -> Make sure that we're using
                         * unique usernames!
                         */
                        final String username = br.getRegex("class=\"c-userIcon-101\" title=\"([^\"]+)\"").getMatch(0);
                        if (userCookies != null && username != null) {
                            account.setUser(Encoding.htmlDecode(username).trim());
                        }
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            /* User has provided cookies instead of password --> No full login possible! */
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        }
                    }
                }
            }
            logger.info("Performing full login");
            if (!StringUtils.contains(account.getUser(), "@")) {
                throw new AccountInvalidException("Please enter your E-Mail address as username!");
            }
            final boolean useAltLogin = true;
            final Form loginform;
            if (useAltLogin) {
                /* Alternative login way: Possibly less captchas when using this one */
                br.getPage(this.getAccountNameSpaceLogin());
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
                /* 2021-08-06: only for video.fc2.com */
                br.getHeaders().put("Referer", "https://video." + Browser.getHost(this.getHost(), false) + "/");
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
            final String loginCookieKey = "fclo";
            /* Important! Without this cookie we won't be able to login!! */
            if (br.getCookie(br.getHost(), loginCookieKey, Cookies.NOTDELETEDPATTERN) == null) {
                br.setCookie(br.getHost(), loginCookieKey, System.currentTimeMillis() + "%2C" + Locale.getDefault().getLanguage());
            }
            // /* 2022-03-28: Workaround-attempt for website doing unlimited redirects to home.laxd.com (same can happen in browser!). */
            // br.setFollowRedirects(false);
            // final String redirect = br.getRedirectLocation();
            // if (redirect != null) {
            // br.getPage(redirect);
            // }
            // br.setFollowRedirects(true);
            br.submitForm(loginform);
            // br.submitForm(loginform);
            if (br.getURL().contains("error=1")) {
                /* Login failed */
                throw new AccountInvalidException();
            }
            {
                /*
                 * 2021-01-04: Small workaround for bad redirect to wrong page on 2FA login required or redirect-loop e.g. on home.laxd.com
                 * (same happens via browser).
                 */
                // final boolean required2FALogin = br.getRedirectLocation() != null &&
                // br.getRedirectLocation().contains("login_authentication.php");
                // if (!br.getURL().contains("login_authentication.php") && required2FALogin && this.getHost().equals("video.fc2.com")) {
                // br.getPage("https://secure.id.fc2.com/login_authentication.php");
                // }
                final Form twoFactorLogin = get2FALoginForm(this.br);
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
                        throw new AccountInvalidException("Invalid 2-factor-authentication code format!");
                    }
                    logger.info("Submitting 2FA code");
                    twoFactorLogin.put("code", twoFACode);
                    br.submitForm(twoFactorLogin);
                    final String redirectAfterLogin = br.getRegex("http-equiv=\"Refresh\" content=\"0; url=(https?:/[^\"]+)\"").getMatch(0);
                    if (redirectAfterLogin != null) {
                        br.getPage(redirectAfterLogin);
                    }
                    if (get2FALoginForm(this.br) != null) {
                        throw new AccountInvalidException("2FA auth failed!");
                    } else {
                        logger.info("2FA login seems to be successful");
                    }
                }
            }
            /*
             * If everything goes as planned, we should be redirected to video subdomain but in case we're not we'll also check logged in
             * state for their main portal fc2.com.
             */
            if (br.getHost(true).equals(Browser.getHost(checkURL, true))) {
                logger.info("Automatic redirect to video subdomain after login successful");
            } else {
                logger.info("Automatic redirect to video subdomain after login failed. Current URL: " + br.getURL());
                if (!isLoggedINFC2(br)) {
                    throw new AccountInvalidException();
                }
                br.getPage(checkURL);
            }
            if (!br.getURL().equals(checkURL) && !isLoggedINVideoSubdomain(br)) {
                logger.info("Accessing target URL: " + checkURL);
                br.getPage(checkURL);
            }
            if (!isLoggedINVideoSubdomain(br)) {
                /* This should never happen */
                throw new AccountInvalidException("fc2 Account seems to be valid but video subdomain login login failed.");
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
        }
    }

    private Form get2FALoginForm(final Browser br) {
        return br.getFormbyActionRegex(".*login_authentication\\.php.*");
    }

    protected abstract String getAccountNameSpaceLogin();

    protected abstract String getAccountNameSpacePremium();

    protected abstract String getAccountNameSpaceForLoginCheck();

    private boolean isLoggedINFC2(final Browser br) {
        if (br.containsHTML("/logout\\.php")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if used is logged in according to HTML code. </br>
     * Designed to work for most of all video.<supportedDomain>.com
     */
    private boolean isLoggedINVideoSubdomain(final Browser br) {
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
            this.login(account, true, this.getAccountNameSpaceForLoginCheck());
            account.setType(AccountType.FREE);
            ai.setUnlimitedTraffic();
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
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 602:
                /* reconnect */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading");
            case 603:
                link.setProperty(PROPERTY_PREMIUMONLY, true);
                break;
            default:
                logger.info("Unknown error code: " + error);
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
                checkErrorsNoFileLastResort(br.getHttpConnection());
            }
            dl.startDownload();
        }
    }

    /**
     * Use this whenever you get a non-file response when a file is to be expected.
     *
     * @throws PluginException
     */
    private void checkErrorsNoFileLastResort(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (br.containsHTML("not found")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'not found'", 5 * 60 * 1000l);
        } else {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
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