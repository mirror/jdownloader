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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.PinterestComDecrypter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PinterestCom extends PluginForHost {
    public PinterestCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pinterest.com/");
        setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    /* 2021-01-28: Full login + invisible reCaptcha Enterprise --> Broken -> Use cookie login only */
    private static final boolean enforceCookieLoginOnly = true;

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (enforceCookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.USERNAME_IS_EMAIL, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.USERNAME_IS_EMAIL, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pinterest.com", "pinterest.at", "pinterest.de", "pinterest.fr", "pinterest.it", "pinterest.es", "pinterest.co.uk", "pinterest.se" });
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
            ret.add("https?://(?:(?:www|[a-z]{2})\\.)?" + buildHostsPatternPart(domains) + "/pin/[A-Za-z0-9\\-_]+/");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://about.pinterest.com/de/terms-service";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null) {
            final String pin_id = getPinID(link.getPluginPatternMatcher());
            if (pin_id != null) {
                return "pinterest_pin://" + pin_id;
            }
        }
        return super.getLinkID(link);
    }

    public static String getPinID(final String pin_url) {
        return new Regex(pin_url, "(?i)pin/([^/]+)/?$").getMatch(0);
    }

    /* Site constants */
    public static final String x_app_version             = "6cedd5c";
    @Deprecated
    public static final String PROPERTY_DIRECTURL_LEGACY = "free_directlink";
    public static final String PROPERTY_DIRECTURL_LIST   = "directlink_list";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String pinID = getPinID(link.getPluginPatternMatcher());
        /* Display ids for offline links */
        if (!link.isNameSet()) {
            link.setName(pinID);
        }
        this.setBrowserExclusive();
        /* 2021-03-02: PINs may redirect to other PINs in very rare cases -> Handle that */
        br.getPage(link.getPluginPatternMatcher());
        String redirect = br.getRegex("window\\.location\\s*=\\s*\"([^\"]+)\"").getMatch(0);
        if (redirect != null) {
            /* We want the full URL. */
            redirect = br.getURL(redirect).toString();
        }
        if (!new Regex(br.getURL(), PinterestComDecrypter.PATTERN_PIN).patternFind()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (redirect != null && new Regex(redirect, PinterestComDecrypter.PATTERN_PIN).patternFind() && !redirect.contains(pinID)) {
            final String newPinID = getPinID(redirect);
            logger.info("Old pinID: " + pinID + " | New pinID: " + newPinID + " | New URL: " + redirect);
            link.setPluginPatternMatcher(redirect);
        } else if (redirect != null && redirect.contains("show_error=true")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> pinMap;
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            login(account, false);
            pinMap = PinterestComDecrypter.getPINMap(this.br, link.getPluginPatternMatcher());
            /* We don't have to be logged in to perform downloads so better log out to try to avoid account bans. */
            br.clearCookies(null);
        } else {
            pinMap = PinterestComDecrypter.getPINMap(this.br, link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        PinterestComDecrypter.setInfoOnDownloadLink(link, pinMap);
        return AvailableStatus.TRUE;
    }

    /**
     * Sets Headers for Pinterest API usage. <br />
     */
    public static void prepAPIBR(final Browser br) throws PluginException {
        if (br.getRequest() == null) {
            try {
                /* Access mainpage to get that cookie which we'll need later. */
                br.getPage("https://www.pinterest.de/");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final String csrftoken = br.getCookie(br.getHost(), "csrftoken");
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Pinterest-AppState", "active");
        br.getHeaders().put("X-NEW-APP", "1");
        br.getHeaders().put("X-APP-VERSION", x_app_version);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        br.getHeaders().put("X-CSRFToken", csrftoken);
    }

    public static boolean isOffline(final Browser br, final String pin_id) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!br.getURL().contains(pin_id)) {
            /* PIN redirected to other PIN --> initial PIN is offline! */
            /* Or: PIN is not present in URL --> PIN is offline */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, false, 1);
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        List<String> storedDirecturls = getStoredDirecturls(link);
        if (storedDirecturls == null || storedDirecturls.isEmpty()) {
            requestFileInformation(link);
            storedDirecturls = getStoredDirecturls(link);
            if (storedDirecturls == null || storedDirecturls.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        int index = 0;
        /*
         * Try to download all available items. In some rare cases, the original image is not downloadable so the next best item will be
         * downloaded instead.
         */
        for (final String storedDirecturl : storedDirecturls) {
            logger.info("Attempting to download item " + index + "/" + storedDirecturls.size() + " | " + storedDirecturl);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, storedDirecturl, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                /* Looks good -> Download that item */
                break;
            } else {
                br.followConnection(true);
                if (index == storedDirecturls.size() - 1) {
                    /* This is the last item */
                    /* Make sure that new directurls will be obtained for next try. */
                    link.removeProperty(PROPERTY_DIRECTURL_LEGACY);
                    link.removeProperty(PROPERTY_DIRECTURL_LIST);
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Broken media?");
                } else {
                    logger.info("Invalid directurl: " + storedDirecturl);
                }
                index++;
                // continue
            }
        }
        dl.startDownload();
    }

    private List<String> getStoredDirecturls(final DownloadLink link) {
        final String legacyItem = link.getStringProperty(PROPERTY_DIRECTURL_LEGACY);
        if (legacyItem != null) {
            final List<String> ret = new ArrayList<String>();
            ret.add(legacyItem);
            return ret;
        } else {
            return (List<String>) link.getProperty(PinterestCom.PROPERTY_DIRECTURL_LIST);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            br.setAllowedResponseCodes(new int[] { 401 });
            String last_used_host = account.getStringProperty("host");
            if (last_used_host == null) {
                /* Fallback and required on first run */
                last_used_host = "pinterest.com";
            }
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            if (enforceCookieLoginOnly && userCookies == null) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            if (userCookies != null) {
                /* They got a lot of different domains -> Choose the one the user was using */
                final Cookie sessCookie = userCookies.get("_pinterest_sess");
                if (sessCookie != null && !StringUtils.isEmpty(sessCookie.getHost()) && sessCookie.getHost().contains("pinterest")) {
                    last_used_host = sessCookie.getHost();
                }
                br.setCookies(last_used_host, userCookies);
                br.getPage("https://www." + last_used_host);
                if (!validateCookies) {
                    /* Do not validate cookies */
                    return;
                }
                if (!this.isLoggedINHTML(br)) {
                    logger.warning("Cookie login failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
                account.saveCookies(br.getCookies(last_used_host), "");
                account.setProperty("host", br.getHost());
                return;
            }
            if (cookies != null) {
                br.setCookies(last_used_host, cookies);
                if (!validateCookies) {
                    /* Do not validate cookies */
                    return;
                }
                br.getPage("https://www." + last_used_host + "/");
                if (this.isLoggedINHTML(br)) {
                    account.saveCookies(br.getCookies(last_used_host), "");
                    account.setProperty("host", br.getHost());
                    return;
                }
                /* Full login required */
            }
            logger.info("Full login required");
            /* May redirect to e.g. pinterest.de */
            br.getPage("https://www." + this.getHost() + "/login/?action=login");
            String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Ldx7ZkUAAAAAF3SZ05DRL2Kdh911tCa3qFP0-0r") {
                protected boolean isEnterprise(final String source) {
                    return true;
                }

                protected TYPE getType(String source) {
                    return TYPE.INVISIBLE;
                }
            }.getToken();
            // prepAPIBR(br);
            // String postData = "source_url=/login/&data={\"options\":{\"username_or_email\":\"" + account.getUser() +
            // "\",\"password\":\"" + account.getPass() +
            // "\"},\"context\":{}}&module_path=App()>LoginPage()>Login()>Button(class_name=primary,+text=Anmelden,+type=submit,+size=large)";
            // // postData = Encoding.urlEncode(postData);
            // final String urlpart = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0);
            // br.postPageRaw(urlpart + "/resource/UserSessionResource/create/", postData);
            br.postPage("https://accounts.pinterest.com/v3/login/handshake/", "username_or_email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&token=" + recaptchaV2Response);
            if (br.getHttpConnection().getResponseCode() != 200 || br.getCookie(br.getHost(), "_pinterest_sess", Cookies.NOTDELETEDPATTERN) == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            account.setProperty("host", br.getHost());
        }
    }

    private boolean isLoggedINHTML(final Browser br) {
        if (br.containsHTML("\"isAuth(enticated)?\"\\s*:\\s*true")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, false, 1);
    }

    @Override
    public String getDescription() {
        return "JDownloader's Pinterest plugin helps downloading pictures from pinterest.com.";
    }

    public static final String  ENABLE_DESCRIPTION_IN_FILENAMES             = "ENABLE_DESCRIPTION_IN_FILENAMES";
    public static final boolean defaultENABLE_DESCRIPTION_IN_FILENAMES      = false;
    public static final String  ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS        = "ENABLE_CRAWL_ALTERNATIVE_URLS_INSIDE_COMMENTS";
    public static final boolean defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_DESCRIPTION_IN_FILENAMES, "Add pin-description to filenames?\r\nNOTE: If enabled, Filenames might get very long!").setDefaultValue(defaultENABLE_DESCRIPTION_IN_FILENAMES));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS, "Crawl alternative source URLs e.g. (higher quality) imgur.com URLs?").setDefaultValue(defaultENABLE_CRAWL_ALTERNATIVE_SOURCE_URLS));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_DIRECTURL_LEGACY);
        link.removeProperty(PROPERTY_DIRECTURL_LIST);
    }
}