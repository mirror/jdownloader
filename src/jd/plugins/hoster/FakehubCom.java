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

import java.util.HashMap;
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
import jd.http.requests.PostRequest;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fakehub.com" }, urls = { "https?://(?:new\\.|site-)?ma\\.fakehub\\.com/download/\\d+/[A-Za-z0-9\\-_]+/|http://fakehubdecrypted.+" })
public class FakehubCom extends PluginForHost {
    public FakehubCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.fakehub.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.supportmg.com/terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_premium_pic             = ".+\\.jpg.*?";
    public static final String   html_loggedin                = "/member/profile/";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br) {
        return jd.plugins.hoster.BrazzersCom.pornportalPrepBR(br, jd.plugins.decrypter.FakehubCom.DOMAIN_PREFIX_PREMIUM + jd.plugins.decrypter.FakehubCom.DOMAIN_BASE);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://fakehubdecrypted", "http://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(this.br, aa, false);
        dllink = link.getDownloadURL();
        final String fid = link.getStringProperty("fid", null);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } else {
                if (link.getDownloadURL().matches(type_premium_pic)) {
                    /* Refresh directurl */
                    final String number_formatted = link.getStringProperty("picnumber_formatted", null);
                    if (fid == null || number_formatted == null) {
                        /* User added url without decrypter --> Impossible to refresh this directurl! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    this.br.getPage(jd.plugins.decrypter.FakehubCom.getPicUrl(fid));
                    if (jd.plugins.decrypter.FakehubCom.isOffline(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final String pictures[] = jd.plugins.decrypter.FakehubCom.getPictureArray(this.br);
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

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    setAPIHeader(br);
                    br.getPage("https://site-api.project1service.com/v1/self");
                    if (br.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                    // br.getPage("https://site-ma." + account.getHoster() + "/login");
                    // if (StringUtils.containsIgnoreCase(br.getURL(), "/access/login")) {
                    // logger.info("Cookie login failed --> Performing full login");
                    // br = prepBR(new Browser());
                    // account.clearCookies("");
                    // } else {
                    // /* Set API header in case we're performing API requests later */
                    // setAPIHeader(br);
                    // account.saveCookies(br.getCookies(account.getHoster()), "");
                    // logger.info("Cookie login successful");
                    // return;
                    // }
                }
                if (cookies == null) {
                    br.setCookie(getHost(), "bonusPageViews", "1");
                    br.setFollowRedirects(true);
                    br.getPage("https://site-ma." + account.getHoster() + "/login");
                    final String api_base = PluginJSonUtils.getJson(br, "dataApiUrl");
                    final String ip = PluginJSonUtils.getJson(br, "ip");
                    if (!setAPIHeader(br)) {
                        logger.warning("Failed to set API headers");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (StringUtils.isEmpty(api_base)) {
                        logger.warning("Failed to find api_base");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getHeaders().put("Content-Type", "application/json");
                    br.getHeaders().put("referer", "https://site-ma." + account.getHoster() + "/login");
                    br.getHeaders().put("sec-fetch-dest", "empty");
                    br.getHeaders().put("sec-fetch-mode", "cors");
                    br.getHeaders().put("sec-fetch-site", "cross-site");
                    if (!StringUtils.isEmpty(ip)) {
                        br.getHeaders().put("x-forwarded-for", ip);
                    }
                    final String recaptchaSiteKey = PluginJSonUtils.getJson(br, "recaptchaSiteKey");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("username", account.getUser());
                    map.put("password", account.getPass());
                    map.put("failureUrl", "https://site-ma." + this.getHost() + "/access/failure");
                    map.put("successUrl", "https://site-ma." + this.getHost() + "/access/success");
                    final DownloadLink dlinkbefore = getDownloadLink();
                    if (!StringUtils.isEmpty(recaptchaSiteKey)) {
                        try {
                            if (dlinkbefore == null) {
                                setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://ma-site." + account.getHoster(), true));
                            }
                            final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, recaptchaSiteKey);
                            map.put("googleReCaptchaResponse", captcha.getToken());
                        } finally {
                            if (dlinkbefore != null) {
                                setDownloadLink(dlinkbefore);
                            }
                        }
                    }
                    br.setAllowedResponseCodes(new int[] { 400 });
                    final PostRequest postRequest = br.createPostRequest(api_base + "/v1/authenticate/redirect", JSonStorage.toString(map));
                    // postRequest.getHeaders().put("Instance", br.getCookie(getHost(), "instance_token"));
                    br.getPage(postRequest);
                    map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    final String authenticationUrl = (String) map.get("authenticationUrl");
                    if (StringUtils.isEmpty(authenticationUrl)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Now continue without API */
                    br.getPage(authenticationUrl);
                    final Form continueform = br.getFormbyKey("response");
                    if (continueform != null) {
                        /* Redirect from probiller.com to main website --> Login complete */
                        br.submitForm(continueform);
                    }
                    if (!isLoggedIN()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.getCookie(getHost(), "access_token_ma", Cookies.NOTDELETEDPATTERN) != null;
    }

    private boolean setAPIHeader(final Browser br) {
        final String instance_token = br.getCookie(br.getHost(), "instance_token");
        final String access_token_ma = br.getCookie(br.getHost(), "access_token_ma");
        boolean foundCookie = false;
        if (instance_token != null) {
            br.getHeaders().put("Instance", instance_token);
            foundCookie = true;
        }
        if (access_token_ma != null) {
            br.getHeaders().put("Authorization", access_token_ma);
            foundCookie = true;
        }
        /* TODO */
        br.getHeaders().put("Origin", "https://site-ma.fakehub.com");
        return foundCookie;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            try {
                login(this.br, account, true);
                final AccountInfo ai = new AccountInfo();
                setAPIHeader(br);
                if (br.getURL() == null || !br.getURL().contains("/v1/self")) {
                    br.getPage("https://site-api.project1service.com/v1/self");
                }
                if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final Boolean isExpired = (Boolean) map.get("isExpired");
                final Boolean isTrial = (Boolean) map.get("isTrial");
                if (Boolean.TRUE.equals(isTrial) || Boolean.TRUE.equals(isExpired)) {
                    account.setType(AccountType.FREE);
                    /* 2020-04-01: Free accounts cannot be used for anything! */
                    ai.setTrafficLeft(0);
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
                account.setConcurrentUsePossible(true);
                ai.setUnlimitedTraffic();
                return ai;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the fakehub.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_1080p_6000", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_720p_2600", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_480p_1500", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_368p_850", "Grab 360p (mp4)?").setDefaultValue(true));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
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