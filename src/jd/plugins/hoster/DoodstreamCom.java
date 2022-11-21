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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DoodstreamCom extends XFileSharingProBasic {
    public DoodstreamCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-08-31: null<br />
     * other:<br />
     */
    private static final String TYPE_STREAM   = "https?://[^/]+/e/.+";
    private static final String TYPE_DOWNLOAD = "https?://[^/]+/d/.+";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dood.re", "dood.so", "doodstream.com", "dood.to", "doodapi.com", "dood.watch", "dood.cx", "doodstream.co", "dood.la", "dood.ws", "dood.pm", "dood.sh", "dood.one", "dood.tech", "dood.wf" });
        return ret;
    }

    @Override
    public boolean loginWebsite(final DownloadLink link, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            final boolean followRedirects = br.isFollowingRedirects();
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                /*
                 * 2019-08-20: Some hosts (rare case) will fail on the first attempt even with correct logindata and then demand a captcha.
                 * Example: filejoker.net
                 */
                final Cookies userCookies = account.loadUserCookies();
                /**
                 * Important! Domains may change frequently! </br>
                 * Let it redirect us to their current main domain so we know which domain to set the cookies on.
                 */
                br.getPage(getMainPage());
                if (userCookies != null) {
                    /* Fallback */
                    logger.info("Verifying user-login-cookies");
                    br.setCookies(br.getHost(), userCookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return false;
                    }
                    if (this.verifyCookies(account, userCookies)) {
                        logger.info("Successfully logged in via cookies");
                        String cookiesUsername = br.getCookie(br.getHost(), "login", Cookies.NOTDELETEDPATTERN);
                        if (StringUtils.isEmpty(cookiesUsername)) {
                            cookiesUsername = br.getCookie(br.getHost(), "email", Cookies.NOTDELETEDPATTERN);
                        }
                        if (!StringUtils.isEmpty(cookiesUsername)) {
                            cookiesUsername = Encoding.htmlDecode(cookiesUsername).trim();
                            /**
                             * During cookie login, user can enter whatever he wants into username field.</br>
                             * Most users will enter their real username but to be sure to have unique usernames we don't trust them and try
                             * to get the real username out of our cookies.
                             */
                            if (!StringUtils.isEmpty(cookiesUsername) && !account.getUser().equals(cookiesUsername)) {
                                account.setUser(cookiesUsername);
                            }
                        }
                        return true;
                    } else {
                        logger.info("User cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    logger.info("Stored login-Cookies are available");
                    br.setCookies(br.getHost(), cookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return false;
                    }
                    logger.info("Verifying login-cookies");
                    if (this.verifyCookies(account, cookies)) {
                        logger.info("Successfully logged in via cookies");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Full login required");
                if (this.requiresCookieLogin()) {
                    /**
                     * Cookie login required but user did not put cookies into the password field: </br>
                     * Ask user to login via exported browser cookies e.g. xubster.com.
                     */
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                br.clearCookies(getMainPage());
                final UrlQuery query = new UrlQuery();
                query.add("op", "login_ajax");
                query.add("login", Encoding.urlEncode(account.getUser()));
                query.add("password", Encoding.urlEncode(account.getPass()));
                query.add("loginotp", "");
                query.add("g-recaptcha-response", "");
                query.add("_", Long.toString(System.currentTimeMillis()));
                getPage("/?" + query.toString());
                if (br.getRequest().getResponseHeader("content-type").contains("application/json")) {
                    final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (response.get("status").toString().equalsIgnoreCase("otp_sent")) {
                        /* {"status":"otp_sent","message":"OTP has been sent to your mail"} */
                        logger.info("2FA code required");
                        final DownloadLink dl_dummy;
                        if (this.getDownloadLink() != null) {
                            dl_dummy = this.getDownloadLink();
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                        }
                        String twoFACode = getUserInput("Enter Google 2-Factor Authentication code: " + response.get("message").toString(), dl_dummy);
                        if (twoFACode != null) {
                            twoFACode = twoFACode.trim();
                        }
                        if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                                throw new AccountUnavailableException("\r\nUngültiges Format der 2-faktor-Authentifizierung!", 1 * 60 * 1000l);
                            } else {
                                throw new AccountUnavailableException("\r\nInvalid 2-factor-authentication code format!", 1 * 60 * 1000l);
                            }
                        }
                        logger.info("Submitting 2FA code");
                        query.addAndReplace("loginotp", twoFACode);
                        br.getPage("/?" + query.toString());
                        /**
                         * E.g. wrong code: {"status":"fail","message":"Wrong login OTP."} </br>
                         * On success it will redirect us to a non-json page!
                         */
                        if (!this.isLoggedin(br)) {
                            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                                throw new AccountUnavailableException("\r\nUngültiger 2-faktor-Authentifizierungscode!", 1 * 60 * 1000l);
                            } else {
                                throw new AccountUnavailableException("\r\nInvalid 2-factor-authentication code!", 1 * 60 * 1000l);
                            }
                        }
                    }
                }
                if (!this.isLoggedin(this.br)) {
                    if (getCorrectBR(br).contains("op=resend_activation")) {
                        /* User entered correct logindata but hasn't activated his account yet. */
                        throw new AccountUnavailableException("\r\nYour account has not yet been activated!\r\nActivate it via the URL you received via E-Mail and try again!", 5 * 60 * 1000l);
                    }
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new AccountInvalidException("\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.");
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new AccountInvalidException("\r\nNieprawidłowa nazwa użytkownika / hasło!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.");
                    } else {
                        throw new AccountInvalidException("\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                br.setFollowRedirects(followRedirects);
            }
        }
    }

    @Override
    public String rewriteHost(final String host) {
        /**
         * 2021-01-15: Main domain has changed from doodstream.com to dood.so </br>
         * 2022-11-21: Changed to: dood.re
         */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return DoodstreamCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartDoodstream() {
        return "/(?:e|d)/[a-z0-9]+";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + DoodstreamCom.getDefaultAnnotationPatternPartDoodstream());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return -2;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        if (linkpart != null) {
            link.setPluginPatternMatcher(getMainPage() + "/" + linkpart);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-08-31: Special */
        return true;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String url = dl.getPluginPatternMatcher();
            if (url != null) {
                final String result = new Regex(new URL(url).getPath(), "([a-z0-9]+)$").getMatch(0);
                return result;
            }
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink dl) {
        return null;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isShortURL(DownloadLink link) {
        return false;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String html) {
        /**
         * 2021-08-20: Hoster is playing cat & mouse games by adding fake "file not found" texts. </br>
         * An empty embed iframe is a sign that the item is offline.
         */
        if (new Regex(html, "<iframe src=\"/e/\"").matches()) {
            /* 2021-26-04 */
            // all videos are now
            // <h1>Not Found</h1>
            // <p>video you are looking for is not found.</p>
            if (new Regex(html, "minimalUserResponseInMiliseconds\\s*=").matches()) {
                return false;
            } else if (new Regex(html, "'(/cptr/.*?)'").getMatch(0) != null) {
                return false;
            } else {
                return true;
            }
        } else if (br.containsHTML("(?i)<h1>\\s*Oops\\! Sorry\\s*</h1>\\s*<p>\\s*File you are looking for is not found")) {
            /* 2021-08-26 */
            return true;
        } else if (br.containsHTML("(?i)<title>\\s*Video not found\\s*\\|\\s*DoodStream\\s*</title>")) {
            return true;
        } else if (br.containsHTML("(?i)<h1>\\s*Not Found\\s*</h1>\\s*<p>\\s*video you are looking for is not found") && link.getPluginPatternMatcher().matches(TYPE_STREAM) && !br.containsHTML("</video>")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getFallbackFilename(DownloadLink dl) {
        String fallBack = super.getFallbackFilename(dl);
        if (!StringUtils.endsWithCaseInsensitive(fallBack, ".mp4")) {
            fallBack += ".mp4";
        }
        return fallBack;
    }

    protected String doodExe(final String crp, final String crs) {
        try {
            if ("N_crp".equals(crp)) {
                return crs;
            }
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.eval("var _0x4ee0=[\"\\x30\\x20\\x42\\x79\\x74\\x65\",\"\\x6C\\x6F\\x67\",\"\\x66\\x6C\\x6F\\x6F\\x72\",\"\\x70\\x6F\\x77\",\"\\x72\\x6F\\x75\\x6E\\x64\",\"\\x20\",\"\\x42\\x79\\x74\\x65\\x73\",\"\\x4B\\x42\",\"\\x4D\\x42\",\"\\x47\\x42\",\"\\x54\\x42\",\"\\x30\",\"\\x30\\x30\",\"\\x3A\",\"\\x20\\x48\\x72\\x73\",\"\\x20\\x4D\\x69\\x6E\\x73\",\"\\x20\\x53\\x65\\x63\",\"\",\"\\x6A\\x6F\\x69\\x6E\",\"\\x73\\x6F\\x72\\x74\",\"\\x73\\x70\\x6C\\x69\\x74\",\"\\x6C\\x65\\x6E\\x67\\x74\\x68\",\"\\x63\\x68\\x61\\x72\\x41\\x74\",\"\\x69\\x6E\\x64\\x65\\x78\\x4F\\x66\",\"\\x2B\",\"\\x72\\x65\\x70\\x6C\\x61\\x63\\x65\\x41\\x6C\\x6C\",\"\\x2B\\x2D\\x2D\\x2B\",\"\\x5D\",\"\\x2B\\x2D\\x2B\",\"\\x5B\",\"\\x2B\\x2E\\x2E\\x2B\",\"\\x29\",\"\\x2B\\x2E\\x2B\",\"\\x28\"];");
            engine.eval("_0x4ee0[25]=\"replace\"");
            engine.eval("function doodExe(_0xc93ex9,_0xc93ex6){for(var _0xc93ex5=_0xc93ex9[_0x4ee0[20]](_0x4ee0[17])[_0x4ee0[19]]()[_0x4ee0[18]](_0x4ee0[17]),_0xc93ex7=_0x4ee0[17],_0xc93exa=0;_0xc93exa< _0xc93ex6[_0x4ee0[21]];_0xc93exa+= 1){_0xc93ex7+= _0xc93ex5[_0x4ee0[22]](_0xc93ex9[_0x4ee0[23]](_0xc93ex6[_0x4ee0[22]](_0xc93exa)))}; return _0xc93ex7= (_0xc93ex7= (_0xc93ex7= (_0xc93ex7= (_0xc93ex7= _0xc93ex7[_0x4ee0[25]](_0x4ee0[32],_0x4ee0[33]))[_0x4ee0[25]](_0x4ee0[30],_0x4ee0[31]))[_0x4ee0[25]](_0x4ee0[28],_0x4ee0[29]))[_0x4ee0[25]](_0x4ee0[26],_0x4ee0[27]))[_0x4ee0[25]](_0x4ee0[24],_0x4ee0[5])}");
            engine.eval("var result=doodExe(\"" + crp + "\",\"" + crs + "\");");
            return engine.get("result").toString().replace("+", " ");// replace vs replaceAll
        } catch (final Throwable e) {
            logger.log(e);
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean downloadsStarted) throws Exception {
        correctDownloadLink(link);
        /* First, set fallback-filename */
        if (!link.isNameSet()) {
            setWeakFilename(link);
        }
        this.br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        /* Allow redirects to other content-IDs but files should be offline if there is e.g. a redirect to an unsupported URL format. */
        if (isOffline(link, this.br, getCorrectBR(br)) || !this.canHandle(this.br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String cptr = br.getRegex("'(/cptr/.*?)'").getMatch(0);
        if (cptr != null && link.getFinalFileName() == null) {
            final Browser brc = br.cloneBrowser();
            brc.getPage(cptr);
            try {
                final Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                String filename = doodExe((String) JavaScriptEngineFactory.walkJson(response, "ttl/crp"), (String) JavaScriptEngineFactory.walkJson(response, "ttl/crs"));
                if (!StringUtils.isEmpty(filename)) {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
                final String filesize = doodExe((String) JavaScriptEngineFactory.walkJson(response, "siz/crp"), (String) JavaScriptEngineFactory.walkJson(response, "siz/crs"));
                if (!StringUtils.isEmpty(filesize)) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (link.getFinalFileName() == null) {
            if (link.getPluginPatternMatcher().matches(TYPE_STREAM)) {
                /* First try to get filename from Chromecast json */
                String filename = new Regex(getCorrectBR(br), "<title>\\s*([^<>\"]*?)\\s*-\\s*DoodStream(?:\\.com)?\\s*</title>").getMatch(0);
                if (filename == null) {
                    filename = new Regex(getCorrectBR(br), "<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
                    if (filename == null) {
                        filename = new Regex(getCorrectBR(br), "videoInfo\\s*:\\s*\\{\\s*title\\s*:\\s*\"(.*?)\"").getMatch(0);
                    }
                }
                if (StringUtils.isEmpty(filename)) {
                    link.setName(this.getFallbackFilename(link));
                } else {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
            } else {
                String filename = br.getRegex("<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
                if (filename == null) {
                    filename = new Regex(getCorrectBR(br), "videoInfo\\s*:\\s*\\{\\s*title\\s*:\\s*\"(.*?)\"").getMatch(0);
                }
                if (StringUtils.isEmpty(filename)) {
                    link.setName(this.getFallbackFilename(link));
                } else {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
                final String filesize = br.getRegex("class\\s*=\\s*\"size\">.*?</i>\\s*([^<>\"]+)\\s*<").getMatch(0);
                if (!StringUtils.isEmpty(filesize)) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    protected void checkSSLInspection(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (br.containsHTML(">\\s*SSL Inspection\\s*<") || br.containsHTML(">\\s*Would you like to proceed with this session\\?\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        String dllink = checkDirectLink(link, account);
        if (StringUtils.isEmpty(dllink)) {
            requestFileInformationWebsite(link, account, true);
            if (link.getPluginPatternMatcher().matches(TYPE_DOWNLOAD)) {
                /* Basically the same as the other type but hides that via iFrame. */
                final String embedURL = br.getRegex("<iframe[^>]*src=\"(/e/[a-z0-9]+)\"").getMatch(0);
                if (embedURL == null) {
                    checkSSLInspection(br, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    this.getPage(embedURL);
                }
            }
            String captchaContainer = br.getRegex("\\$\\.get\\(\"(/[^\"]+op=validate\\&gc_response=)").getMatch(0);
            if (captchaContainer != null) {
                final Browser brc = br.cloneBrowser();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brc).getToken();
                this.getPage(brc, captchaContainer + Encoding.urlEncode(recaptchaV2Response), true);
                sleep(1000, link);
                getPage(br.getURL());// location.reload();
                captchaContainer = br.getRegex("\\$\\.get\\(\"(/[^\"]+op=validate\\&gc_response=)").getMatch(0);
                if (captchaContainer != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String continue_url = br.getRegex("'(/pass_md5/[^<>\"\\']+)'").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String token = br.getRegex("\\&token=([a-z0-9]+)").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.getPage(continue_url);
            /* Make sure we got a valid URL befopre continuing! */
            final URL dlurl = new URL(br.toString());
            // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            dllink = dlurl.toString();
            dllink += "?token=" + token + "&expiry=" + System.currentTimeMillis();
        }
        handleDownload(link, account, dllink, null);
    }

    @Override
    protected String regexAPIKey(final Browser br) {
        final String apikey = br.getRegex("value=\"([^\"]+)\" readonly class=\"form-control\">\\s*<button class=\"btn btn-primary btn-block regenerate-key\"").getMatch(0);
        if (apikey != null) {
            return apikey;
        } else {
            return super.regexAPIKey(br);
        }
    }

    @Override
    protected String regexGenerateAPIKeyURL(final Browser br) {
        final String generateAPIKeyURL = br.getRegex("(/genrate-api/[^\"]+)").getMatch(0);
        if (generateAPIKeyURL != null) {
            return generateAPIKeyURL;
        } else {
            return super.regexGenerateAPIKeyURL(br);
        }
    }

    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser brc, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            logger.info("[FilesizeMode] Trying to find official video downloads");
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
        }
        final String continueURL = br.getRegex("\"(/download/[a-z0-9]+/[a-z]/[^\"]+)\"").getMatch(0);
        if (continueURL == null) {
            /* No official download possible */
            return null;
        }
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return null;
        }
        /* 2022-07-08: 5 seconds of pre-download-wait is skippable */
        final boolean skipWaittime = true;
        if (!skipWaittime) {
            this.waitTime(link, Time.systemIndependentCurrentJVMTimeMillis());
            logger.info("Waiting extra wait seconds: " + getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds());
            this.sleep(getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds() * 1000l, link);
        }
        getPage(brc, continueURL);
        /* 2019-08-29: This Form may sometimes be given e.g. deltabit.co */
        final Form download1 = brc.getFormByInputFieldKeyValue("op", "download1");
        if (download1 != null) {
            this.submitForm(brc, download1);
            this.checkErrors(brc, brc.toString(), link, account, false);
        }
        String dllink = brc.getRegex("(https?://[^/]+/[A-Za-z0-9]+/[^/]+\\?token=[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            dllink = brc.getRegex("onclick=\"window\\.open\\('(https?://[^<>\"\\']+)'").getMatch(0);
        }
        if (dllink == null) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = brc.getRegex("<a href=\"(https?[^\"]+)\"[^>]*>Direct Download Link</a>").getMatch(0);
        }
        if (dllink == null) {
            dllink = this.getDllink(link, account, brc, brc.toString());
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
        } else {
            logger.info("Successfully found dllink via official video download");
        }
        return dllink;
    }

    /* *************************** PUT API RELATED METHODS HERE *************************** */
    @Override
    protected String getAPIBase() {
        /* 2020-08-31: See here: https://doodstream.com/api-docs */
        // final String custom_apidomain = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
        // if (custom_apidomain != null) {
        // return custom_apidomain;
        // } else {
        // return "https://doodapi.com/api";
        // }
        return "https://doodapi.com/api";
    }
}