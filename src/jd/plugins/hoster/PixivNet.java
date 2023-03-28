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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.PixivNetGallery;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.PixivNetConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PixivNet extends PluginForHost {
    public PixivNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.pixiv.net/signup");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pixiv.net" });
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
            ret.add("decryptedpixivnet://(?:www\\.)?.+|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?:/ajax/illust/\\d+/ugoira_meta|/novel/show\\.php\\?id=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void init() {
        PixivNetGallery.setRequestIntervalLimitGlobal();
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("decryptedpixivnet://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return "https://policies.pixiv.net/en.html";
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        br.setFollowRedirects(true);
        return br;
    }

    public static String createGalleryUrl(final String galleryid) {
        /* 2020-04-07: Either update- or remove this */
        return String.format("https://www.pixiv.net/member_illust.php?mode=manga&illust_id=%s", galleryid);
    }

    public static String createSingleImageUrl(final String galleryid) {
        return String.format("https://www.pixiv.net/en/artworks/%s", galleryid);
    }

    /* Extension which will be used if no correct extension is found */
    public static final String  default_extension            = ".jpg";
    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = 1;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 1;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 1;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String              downloadSource               = null;
    /* DownloadLink Properties / Packagizer properties */
    public static final String  PROPERTY_MAINLINK            = "mainlink";
    public static final String  PROPERTY_GALLERYID           = "galleryid";
    public static final String  PROPERTY_GALLERYURL          = "galleryurl";
    public static final String  PROPERTY_UPLOADDATE          = "createdate";
    public static final String  PROPERTY_UPLOADER            = "uploader";
    public static final String  ANIMATION_META               = "animation_meta";
    private static final String TYPE_ANIMATION_META          = "https?://[^/]+/ajax/illust/(\\d+)/ugoira_meta";
    private static final String TYPE_NOVEL                   = "https?://[^/]+/novel/show\\.php\\?id=(\\d+)";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null) {
            if (link.getPluginPatternMatcher().matches(TYPE_ANIMATION_META)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_ANIMATION_META).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(TYPE_NOVEL)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_NOVEL).getMatch(0);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepBR(this.br);
        if (link.getPluginPatternMatcher().matches(TYPE_NOVEL)) {
            if (!link.isNameSet()) {
                link.setName(this.getFID(link) + ".txt");
            }
            br.getPage(link.getPluginPatternMatcher());
            checkErrors(br);
            final String novelID = this.getFID(link);
            final String json = br.getRegex("id=\"meta-preload-data\"[^>]*content='([^\\']*?)'").getMatch(0);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Object> novelInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "novel/" + novelID);
            final String createDate = novelInfo.get("createDate").toString();
            final String dateFormatted = new Regex(createDate, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            this.downloadSource = novelInfo.get("content").toString();
            link.setFinalFileName(dateFormatted + "_ " + novelInfo.get("userName") + " - " + novelInfo.get("title") + ".txt");
            try {
                link.setDownloadSize(this.downloadSource.getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else if (link.getPluginPatternMatcher().matches(TYPE_ANIMATION_META)) {
            if (!link.isNameSet()) {
                link.setName(this.getFID(link));
            }
            if (link.hasProperty(ANIMATION_META)) {
                /*
                 * Metadata is aready saved as property so we don't care if it is still online. All we have to do is to save it to a
                 * text-file once the user stars downloading.
                 */
                return AvailableStatus.TRUE;
            } else {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openGetConnection(link.getPluginPatternMatcher());
                    if (con.getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (!this.looksLikeDownloadableContent(con)) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Broken file?");
                    }
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(this.getFID(link) + ".json");
                    }
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    this.downloadSource = link.getPluginPatternMatcher();
                    brc.followConnection();
                    link.setProperty(ANIMATION_META, brc.toString());
                } finally {
                    try {
                        if (con != null) {
                            con.disconnect();
                        }
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                this.setBrowserExclusive();
                if (account != null) {
                    login(account, false);
                }
            }
            final String galleryurl = link.getStringProperty("galleryurl", null);
            if (galleryurl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(galleryurl);
            checkErrors(br);
            if (jd.plugins.decrypter.PixivNetGallery.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadSource = link.getPluginPatternMatcher();
            URLConnectionAdapter con = null;
            if (account != null) {
                logger.info("Account is available --> Trying to download original quality");
                String original = downloadSource.replaceFirst("/img-master/", "/img-original/").replaceFirst("_master\\d+", "").replaceFirst("/c/\\d+x\\d+/", "/");
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(original);
                    if (!con.isOK() || con.getContentType().contains("html")) {
                        con.disconnect();
                        if (original.matches(".*?\\.jpe?g$")) {
                            original = original.replaceFirst("\\.jpe?g$", ".png");
                            con = brc.openHeadConnection(original);
                        } else if (original.matches(".*?\\.png$")) {
                            original = original.replaceFirst("\\.png$", ".jpg");
                            con = brc.openHeadConnection(original);
                        }
                    }
                    if (this.looksLikeDownloadableContent(con)) {
                        logger.info("Original download: success");
                        downloadSource = original;
                        final String urlExtension = getFileNameExtensionFromURL(original);
                        final String nameExtension = Files.getExtension(link.getName());
                        if (!StringUtils.endsWithCaseInsensitive(urlExtension, nameExtension)) {
                            link.setFinalFileName(link.getName().replaceFirst("\\." + Pattern.quote(nameExtension) + "$", urlExtension));
                        }
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        return AvailableStatus.TRUE;
                    } else {
                        logger.info("Original download: failure");
                    }
                } finally {
                    try {
                        if (con != null) {
                            con.disconnect();
                        }
                    } catch (final Throwable e) {
                    }
                }
            } else {
                logger.info("Account is not available --> NOT trying to download original quality");
            }
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(downloadSource);
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Broken file?");
                }
                final String urlExtension = getFileNameExtensionFromURL(downloadSource);
                final String nameExtension = Files.getExtension(link.getName());
                if (!StringUtils.endsWithCaseInsensitive(urlExtension, nameExtension)) {
                    link.setFinalFileName(link.getName().replaceFirst("\\." + Pattern.quote(nameExtension) + "$", urlExtension));
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (link.getPluginPatternMatcher().matches(TYPE_NOVEL)) {
            if (this.downloadSource == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Write text to file */
            final File dest = new File(link.getFileOutput());
            IO.writeToFile(dest, this.downloadSource.getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
            /* Set filesize so user can see it in UI. */
            link.setVerifiedFileSize(dest.length());
            /* Set progress to finished - the "download" is complete ;) */
            link.getLinkStatus().setStatus(LinkStatus.FINISHED);
        } else if (link.hasProperty(ANIMATION_META)) {
            /* Write text to file. */
            final String metadata = link.getStringProperty(ANIMATION_META, null);
            if (StringUtils.isEmpty(metadata)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Write text to file */
            final File dest = new File(link.getFileOutput());
            IO.writeToFile(dest, metadata.getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
            /* Set filesize so user can see it in UI. */
            link.setVerifiedFileSize(dest.length());
            /* Set progress to finished - the "download" is complete ;) */
            link.getLinkStatus().setStatus(LinkStatus.FINISHED);
        } else {
            if (downloadSource == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadSource, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(directlinkproperty, downloadSource);
            dl.startDownload();
        }
    }

    public static void checkErrors(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 401) {
            /* Session expired or account required to access content. */
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("return_to=")) {
            throw new AccountRequiredException();
        }
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (urlConnection.getURL().toString().matches(TYPE_ANIMATION_META)) {
            return (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) && StringUtils.containsIgnoreCase(urlConnection.getContentType(), "json");
        } else {
            return super.looksLikeDownloadableContent(urlConnection);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    logger.info("Attempting user cookie login");
                    if (checkCookieLogin(account, userCookies, validateCookies)) {
                        /*
                         * User can put any name into "username" field when doing cookie login --> Try to set a valid, unique username to
                         * display in account manager.
                         */
                        final String pixivId = PluginJSonUtils.getJson(br, "pixivId");
                        if (!StringUtils.isEmpty(pixivId)) {
                            account.setUser(pixivId);
                        }
                        return;
                    } else {
                        /* Full login required but not possible! */
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    logger.info("Attempting normal cookie login");
                    if (checkCookieLogin(account, cookies, validateCookies)) {
                        return;
                    } else {
                        /* Full login required */
                        logger.info("Cookie login failed");
                    }
                }
                /**
                 * 2022-06-08: TODO: Full login via website is broken (captcha fails) --> Inform user to use cookie login in the meanwhile
                 * </br> RE ticket https://svn.jdownloader.org/issues/90125
                 */
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/login.php?ref=wwwtop_accounts_index");
                final String loginJson = br.getRegex("class=\"json-data\" value='(\\{.*?)\\'>").getMatch(0);
                final Map<String, Object> loginInfo = restoreFromString(loginJson, TypeRef.HASHMAP);
                final Form loginform = new Form();
                loginform.setAction("/ajax/login?lang=en");
                loginform.setMethod(MethodType.POST);
                final String loginTT = loginInfo.get("pixivAccount.tt").toString();
                String loginSource = (String) loginInfo.get("pixivAccount.source"); // usually "pc"
                if (loginSource == null) {
                    loginSource = "pc";
                }
                String loginRef = (String) loginInfo.get("pixivAccount.ref"); // usually empty
                if (loginRef == null) {
                    loginRef = "";
                }
                final String siteKeyEnterpriseInvisible = loginInfo.get("pixivAccount.recaptchaEnterpriseScoreSiteKey").toString();
                final String reCaptchaEnterpriseInvisibleAction = (String) loginInfo.get("pixivAccount.recaptchaEnterpriseScoreAction");
                final String siteKeyEnterpriseNormal = (String) loginInfo.get("pixivAccount.recaptchaEnterpriseCheckboxSiteKey");
                final boolean requiresNormalReCaptchaV2 = true;
                String reCaptchaV2Response = "";
                String reCaptchaEnterpriseInvisibleResponse = "";
                final CaptchaHelperHostPluginRecaptchaV2 v3Captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, siteKeyEnterpriseInvisible) {
                    @Override
                    protected boolean isEnterprise(String source) {
                        return true;
                    }

                    @Override
                    protected Map<String, Object> getV3Action(final String source) {
                        final Map<String, Object> ret = new HashMap<String, Object>();
                        ret.put("action", reCaptchaEnterpriseInvisibleAction);
                        return ret;
                    }
                };
                reCaptchaEnterpriseInvisibleResponse = v3Captcha.getToken();
                if (requiresNormalReCaptchaV2 && siteKeyEnterpriseNormal != null) {
                    final CaptchaHelperHostPluginRecaptchaV2 v2Captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, siteKeyEnterpriseNormal) {
                        @Override
                        protected boolean isEnterprise(String source) {
                            return true;
                        }

                        @Override
                        protected Map<String, Object> getV3Action(final String source) {
                            return null;
                        };
                    };
                    reCaptchaV2Response = v2Captcha.getToken();
                } else {
                    reCaptchaV2Response = "";
                }
                loginform.put("login_id", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                // loginform.put("post_key", Encoding.urlEncode(loginPostkey));
                loginform.put("source", Encoding.urlEncode(loginSource));
                loginform.put("app_ios", "0");
                loginform.put("ref", Encoding.urlEncode(loginRef));
                loginform.put("return_to", Encoding.urlEncode("https://www." + br.getHost() + "/en/"));
                loginform.put("g_recaptcha_response", Encoding.urlEncode(reCaptchaV2Response));
                loginform.put("recaptcha_enterprise_score_token", Encoding.urlEncode(reCaptchaEnterpriseInvisibleResponse));
                loginform.put("tt", Encoding.urlEncode(loginTT));
                br.getHeaders().put("Accept", "application/json");
                br.getHeaders().put("Origin", "https://accounts." + br.getHost());
                final Request loginRequest = br.createFormRequest(loginform);
                loginRequest.getHeaders().put("Accept", "application/json");
                loginRequest.getHeaders().put("Origin", "https://accounts." + br.getHost());
                br.getPage(loginRequest);
                final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.HASHMAP);
                if ((Boolean) response.get("error") == Boolean.TRUE) {
                    throw new AccountInvalidException();
                }
                final Map<String, Object> body = (Map<String, Object>) response.get("body");
                final Map<String, Object> errors = (Map<String, Object>) body.get("errors");
                if (errors != null) {
                    throw new AccountInvalidException();
                }
                final String successURL = (String) JavaScriptEngineFactory.walkJson(body, "success/return_to");
                if (successURL == null) {
                    final Map<String, Object> validation_errors = (Map<String, Object>) body.get("validation_errors");
                    if (validation_errors != null && validation_errors.containsKey("captcha")) {
                        /* {"error":false,"message":"","body":{"validation_errors":{"captcha":"Complete the reCAPTCHA verification"}}} */
                        showCookieLoginInfo();
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    throw new AccountInvalidException();
                }
                br.getPage("https://www." + account.getHoster() + "/en");
                if (!isLoggedIN(br)) {
                    if (!account.hasEverBeenValid()) {
                        showCookieLoginInfo();
                    }
                    throw new AccountInvalidException();
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

    private boolean checkCookieLogin(final Account account, final Cookies cookies, final boolean validateCookies) throws IOException {
        br.setCookies(account.getHoster(), cookies);
        if (!validateCookies) {
            /* Do not validate cookies */
            return true;
        } else {
            br.getPage("https://www." + account.getHoster() + "/en");
            if (isLoggedIN(br)) {
                /* Refresh loggedin timestamp */
                logger.info("Cookie login successful");
                account.saveCookies(br.getCookies(account.getHoster()), "");
                return true;
            }
            logger.info("Cookie login failed");
            br.clearCookies(br.getURL());
            return false;
        }
    }

    public static boolean isLoggedIN(final Browser br) {
        /* 2020-11-13: Don't use cookie to check - check via html! */
        // return br.getCookie(br.getHost(), "device_token", Cookies.NOTDELETEDPATTERN) != null;
        final String pixivId = PluginJSonUtils.getJson(br, "pixivId");
        return br.containsHTML("login\\s*:\\s*'yes'") || !StringUtils.isEmpty(pixivId);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        if (br.containsHTML("premium\\s*:\\s*\\'yes\\'") || br.containsHTML("\"premium\"\\s*:\\s*true")) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        /* 2017-02-06: So far there are only free accounts available for this host. */
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends PixivNetConfig> getConfigInterface() {
        return PixivNetConfig.class;
    }
}