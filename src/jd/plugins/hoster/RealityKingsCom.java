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

import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realitykings.com" }, urls = { "https?://(?:new\\.)?members\\.realitykings\\.com/video/download/\\d+/[A-Za-z0-9\\-_]+/|realitykingsdecrypted://.+" })
public class RealityKingsCom extends PluginForHost {
    public RealityKingsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.realitykings.com/tour/join/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://service.adultprovide.com/docs/terms.htm";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME          = false;
    private static final int     FREE_MAXCHUNKS       = 1;
    private static final int     FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_RESUME       = true;
    private static final int     ACCOUNT_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_MAXDOWNLOADS = 20;
    private final String         type_premium_pic     = ".+\\.jpg.*?";
    public static final String   html_loggedin        = "/member/profile/";
    private String               dllink               = null;
    private boolean              server_issues        = false;

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return jd.plugins.hoster.BrazzersCom.pornportalPrepBR(br, jd.plugins.decrypter.RealityKingsCom.DOMAIN_PREFIX_PREMIUM + jd.plugins.decrypter.RealityKingsCom.DOMAIN_BASE);
    }

    public static Browser prepBRAPI(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("realitykingsdecrypted://", "http://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null && !link.getBooleanProperty("free_downloadable", false)) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        if (aa != null) {
            /* Login whenever possible */
            this.login(this.br, aa, false);
        }
        dllink = link.getDownloadURL();
        final String fid = getFID(link);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } else {
                if (link.getPluginPatternMatcher().matches(type_premium_pic)) {
                    /* Refresh directurl */
                    final String number_formatted = link.getStringProperty("picnumber_formatted", null);
                    if (fid == null || number_formatted == null) {
                        /* User added url without decrypter --> Impossible to refresh this directurl! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    this.br.getPage(jd.plugins.decrypter.RealityKingsCom.getPicUrl(fid));
                    if (jd.plugins.decrypter.RealityKingsCom.isOffline(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final String pictures[] = jd.plugins.decrypter.RealityKingsCom.getPictureArray(this.br);
                    for (final String finallink : pictures) {
                        if (finallink.contains(number_formatted + ".jpg")) {
                            dllink = finallink;
                            break;
                        }
                    }
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* ... new URL should work! */
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        /* Set new url */
                        link.setUrlDownload(dllink);
                        /* If user copies url he should always get a valid one too :) */
                        link.setContentUrl(dllink);
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        server_issues = true;
                    }
                } else {
                    server_issues = true;
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("fid", null);
    }

    private boolean isFreeDownloadable(final DownloadLink dl) {
        return dl.getBooleanProperty("free_downloadable", false);
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!isFreeDownloadable(downloadLink)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_RESUME, ACCOUNT_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (!StringUtils.equals(this.getHost(), buildForThisPlugin.getHost()) && jd.plugins.decrypter.RealityKingsCom.isVideoURL(downloadLink.getDownloadURL())) {
            return jd.plugins.decrypter.RealityKingsCom.getVideoUrlFree(this.getFID(downloadLink));
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK          = new Object();
    private final String  MEMBER_DOMAIN = "MEMBER_DOMAIN";

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    if (StringUtils.containsIgnoreCase(account.getStringProperty(MEMBER_DOMAIN, null), "members.")) {
                        /* Old */
                        br.getPage("https://members.realitykings.com/");
                    } else {
                        /* 2019-09-12: new */
                        br.getPage("https://site-ma.realitykings.com");
                    }
                    if (StringUtils.containsIgnoreCase(br.getURL(), "/access/login")) {
                        logger.info("Cookie login failed --> Performing full login");
                        br = prepBR(new Browser());
                        account.clearCookies("");
                    } else {
                        /* Set API header in case we're performing API requests later */
                        setAPIHeader(br);
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        logger.info("Cookie login successful");
                        return;
                    }
                }
                final boolean useWebsiteAPILogin = true;
                if (useWebsiteAPILogin) {
                    loginWebsiteAPI(br, account, false);
                } else {
                    /* Old handling */
                    br.getPage(jd.plugins.decrypter.RealityKingsCom.getProtocol() + jd.plugins.decrypter.RealityKingsCom.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/access/login/");
                    Form loginForm = br.getFormbyActionRegex("/access/submit");
                    if (loginForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginForm.put("username", Encoding.urlEncode(account.getUser()));
                    loginForm.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                    final String redirect_http = br.getRedirectLocation();
                    if (redirect_http != null) {
                        br.getPage(redirect_http);
                    }
                }
                Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                    continueform = br.getFormbyKey("response");
                    if (continueform != null) {
                        /* Redirect from site-ma.realitykings.com.com to main website --> Login complete */
                        br.submitForm(continueform);
                    }
                }
                if (br.getURL().matches("^https?://members\\..+")) {
                    account.setProperty(MEMBER_DOMAIN, br._getURL().getHost());
                    br.getPage("https://members.realitykings.com/");
                } else {
                    account.setProperty(MEMBER_DOMAIN, br._getURL().getHost());
                    br.getPage("https://site-ma.realitykings.com");
                }
                if (StringUtils.containsIgnoreCase(br.getURL(), "/access/login")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername,Passwort und/oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password/login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.removeProperty(MEMBER_DOMAIN);
                }
                throw e;
            }
        }
    }

    private boolean setAPIHeader(final Browser br) {
        final String instance_token = br.getCookie(br.getHost(), "instance_token");
        final String access_token_ma = br.getCookie(getHost(), "access_token_ma");
        boolean foundCookie = false;
        if (instance_token != null) {
            br.getHeaders().put("Instance", instance_token);
            foundCookie = true;
        }
        if (access_token_ma != null) {
            br.getHeaders().put("Authorization", access_token_ma);
            foundCookie = true;
        }
        br.getHeaders().put("Origin", "https://site-ma.realitykings.com");
        return foundCookie;
    }

    public void loginWebsiteAPI(final Browser br, final Account account, final boolean force) throws Exception {
        prepBRAPI(br);
        br.getPage(jd.plugins.decrypter.RealityKingsCom.getProtocol() + "site-ma.realitykings.com/login");
        final String json = br.getRegex("window\\.__JUAN.rawInstance\\s*=\\s*(\\{.*?\\});\\s+").getMatch(0);
        /* 2019-09-12: Their json contains multiple site-keys! Be sure to grab the correct one!! */
        final String recaptchaSiteKey = PluginJSonUtils.getJson(json, "siteKey");
        br.getHeaders().put("Content-Type", "application/json");
        setAPIHeader(br);
        final String successUrl = "https://" + br.getHost(true) + "/access/success";
        final String failureUrl = "https://" + br.getHost(true) + "/access/failure";
        String recaptchaV2Response = "";
        if (!StringUtils.isEmpty(recaptchaSiteKey)) {
            /* Handle login-captcha if required */
            final DownloadLink dlinkbefore = this.getDownloadLink();
            final DownloadLink dl_dummy;
            if (dlinkbefore != null) {
                dl_dummy = dlinkbefore;
            } else {
                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                this.setDownloadLink(dl_dummy);
            }
            recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, recaptchaSiteKey).getToken();
            if (dlinkbefore != null) {
                this.setDownloadLink(dlinkbefore);
            }
        }
        final String postData = "{\"username\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\",\"googleReCaptchaResponse\":\"" + recaptchaV2Response + "\",\"successUrl\":\"" + successUrl + "\",\"failureUrl\":\"" + failureUrl + "\"}";
        /* 2019-09-12: This action can be found in their html inside json: dataApiUrl */
        br.postPageRaw("https://site-api.project1service.com/v1/authenticate/redirect", postData);
        final String authenticationUrl = PluginJSonUtils.getJson(br, "authenticationUrl");
        /*
         * 2019-09-12: E.g. [{ "code": 1000, "message": "Input validation errors.", "errors": [{ "code": 1700, "message":
         * "Recaptcha verification failed.", "field": "googleReCaptchaResponse" }]}]
         */
        // final String code = PluginJSonUtils.getJson(br, "code");
        if (StringUtils.isEmpty(authenticationUrl)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        /* This shall redirect us back to where we came from and get us login-cookies then! */
        br.getPage(authenticationUrl);
    }

    private AccountInfo fetchAccountInfoMembers(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        br.getPage("/member/profile/");
        final boolean isPremium = br.containsHTML("<dt>Membership type:</dt>\\s*?<dd>Paying</dd>");
        if (!isPremium) {
            /*
             * 2017-02-28: Added free account support. Advantages: View trailers (also possible without account), view picture galleries
             * (only possible via free/premium account, free is limited to max 99 viewable pictures!)
             */
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            final String days_remaining = br.getRegex("Remaining membership:</dt>\\s*?<dd>(\\d+) days</dd>").getMatch(0);
            if (days_remaining != null) {
                /* 2018-03-09: Expiredate might not always be available */
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(days_remaining) * 24 * 60 * 60 * 1000, br);
            }
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        }
        account.setMaxSimultanDownloads(ACCOUNT_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    private AccountInfo fetchAccountInfoSiteMa(final Account account) throws Exception {
        synchronized (account) {
            try {
                final AccountInfo ai = new AccountInfo();
                setAPIHeader(br);
                br.getPage("https://site-api.project1service.com/v1/self");
                if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final Boolean isExpired = (Boolean) map.get("isExpired");
                final Boolean isTrial = (Boolean) map.get("isTrial");
                if (Boolean.TRUE.equals(isTrial) || Boolean.TRUE.equals(isExpired)) {
                    /*
                     * 2017-02-28: Added free account support. Advantages: View trailers (also possible without account), view picture
                     * galleries (only possible via free/premium account, free is limited to max 99 viewable pictures!)
                     */
                    account.setType(AccountType.FREE);
                    ai.setStatus("Free Account");
                } else {
                    final String expiryDate = (String) map.get("expiryDate");
                    if (expiryDate != null) {
                        final long expireTimestamp = TimeFormatter.getMilliSeconds(expiryDate, "yyyy'-'MM'-'dd'T'HH':'mm':'ss", null);
                        if (expireTimestamp > 0) {
                            ai.setValidUntil(expireTimestamp, br);
                        }
                    }
                    account.setType(AccountType.PREMIUM);
                    ai.setStatus("Premium Account");
                }
                account.setMaxSimultanDownloads(ACCOUNT_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                ai.setUnlimitedTraffic();
                return ai;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.removeProperty(MEMBER_DOMAIN);
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            login(this.br, account, true);
            if (StringUtils.containsIgnoreCase(account.getStringProperty(MEMBER_DOMAIN, null), "members.")) {
                return fetchAccountInfoMembers(account);
            } else {
                try {
                    return fetchAccountInfoSiteMa(account);
                } catch (final PluginException e) {
                    if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                        login(this.br, account, true);
                        return fetchAccountInfoSiteMa(account);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_RESUME, ACCOUNT_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        /* TODO */
        return account != null || true;
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else if (!downloadLink.isEnabled() && "".equals(downloadLink.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* Multihosts can only download 'trailer' URLs */
            return jd.plugins.decrypter.RealityKingsCom.isVideoURL(downloadLink.getDownloadURL());
        }
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the realitykings.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_1080", "Grab HD MP4 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_3000", "Grab HD MP4 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_1500", "Grab SD MP4 (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_800", "Grab MPEG4 (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_mp4v_480", "Grab MOBILE HIGH (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_mp4v_320", "Grab MOBILE MEDIUM (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_3gp", "Grab MOBILE LOW (3gp)?").setDefaultValue(true));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}