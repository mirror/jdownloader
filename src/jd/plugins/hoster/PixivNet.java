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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.PixivNetConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixiv.net" }, urls = { "decryptedpixivnet://(?:www\\.)?.+|https?://(?:www\\.)?pixiv\\.net/ajax/illust/\\d+/ugoira_meta" })
public class PixivNet extends PluginForHost {
    public PixivNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.pixiv.net/");
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("decryptedpixivnet://", "https://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.pixiv.net/";
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
    private String              dllink                       = null;
    private boolean             server_issues                = false;
    /* DownloadLink Properties / Packagizer properties */
    public static final String  PROPERTY_MAINLINK            = "mainlink";
    public static final String  PROPERTY_GALLERYID           = "galleryid";
    public static final String  PROPERTY_GALLERYURL          = "galleryurl";
    public static final String  PROPERTY_UPLOADDATE          = "createdate";
    public static final String  PROPERTY_UPLOADER            = "uploader";
    public static final String  ANIMATION_META               = "animation_meta";
    private static final String TYPE_ANIMATION_META          = "https?://[^/]+/ajax/illust/(\\d+)/ugoira_meta";

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
        if (link.getPluginPatternMatcher().matches(TYPE_ANIMATION_META)) {
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
                    } else if (this.looksLikeDownloadableContent(con)) {
                        if (link.getFinalFileName() == null) {
                            link.setFinalFileName(this.getFID(link) + ".json");
                        }
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        this.dllink = link.getPluginPatternMatcher();
                        brc.followConnection();
                        link.setProperty(ANIMATION_META, brc.toString());
                    } else {
                        try {
                            brc.followConnection(true);
                        } catch (IOException e) {
                            logger.log(e);
                        }
                        server_issues = true;
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
        } else {
            Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                this.setBrowserExclusive();
                if (account != null) {
                    login(this, br, account, false);
                }
            }
            final String galleryurl = link.getStringProperty("galleryurl", null);
            if (galleryurl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(galleryurl);
            if (jd.plugins.decrypter.PixivNetGallery.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = link.getPluginPatternMatcher();
            URLConnectionAdapter con = null;
            if (account != null) {
                logger.info("Account is available --> Trying to download original quality");
                String original = dllink.replaceFirst("/img-master/", "/img-original/").replaceFirst("_master\\d+", "").replaceFirst("/c/\\d+x\\d+/", "/");
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
                        dllink = original;
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
                con = brc.openHeadConnection(dllink);
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (this.looksLikeDownloadableContent(con)) {
                    final String urlExtension = getFileNameExtensionFromURL(dllink);
                    final String nameExtension = Files.getExtension(link.getName());
                    if (!StringUtils.endsWithCaseInsensitive(urlExtension, nameExtension)) {
                        link.setFinalFileName(link.getName().replaceFirst("\\." + Pattern.quote(nameExtension) + "$", urlExtension));
                    }
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
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
        if (link.hasProperty(ANIMATION_META)) {
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
            if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(directlinkproperty, dllink);
            dl.startDownload();
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

    public static void login(Plugin plugin, final Browser br, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass(), plugin.getLogger());
                if (cookies != null) {
                    plugin.getLogger().info("Attempting normal cookie login");
                    if (checkCookieLogin(plugin, br, account, cookies, validateCookies)) {
                        return;
                    } else {
                        /* Full login required */
                        plugin.getLogger().info("Cookie login failed");
                    }
                }
                if (userCookies != null) {
                    plugin.getLogger().info("Attempting user cookie login");
                    if (checkCookieLogin(plugin, br, account, userCookies, validateCookies)) {
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
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "User cookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                plugin.getLogger().info("Performing full login");
                // br.getPage("https://accounts." + account.getHoster() +
                // "/login?lang=en&source=pc&view_type=page&ref=wwwtop_accounts_index");
                br.getPage("https://accounts." + account.getHoster() + "/login?lang=en");
                final String loginJson = br.getRegex("class=\"json-data\" value='(\\{.*?)\\'>").getMatch(0);
                final Map<String, Object> loginInfo = JSonStorage.restoreFromString(loginJson, TypeRef.HASHMAP);
                final Form loginform = new Form("https://accounts.pixiv.net/api/login?lang=en");
                loginform.setMethod(MethodType.POST);
                final String loginPostkey = loginInfo.get("pixivAccount.postKey").toString();
                final String loginTT = loginInfo.get("pixivAccount.tt").toString();
                final String loginSource = loginInfo.get("pixivAccount.source").toString(); // usually "pc"
                final String loginRef = loginInfo.get("pixivAccount.ref").toString(); // usually empty
                final String siteKeyEnterpriseInvisible = loginInfo.get("pixivAccount.recaptchaEnterpriseScoreSiteKey").toString();
                final String reCaptchaEnterpriseInvisibleAction = loginInfo.get("pixivAccount.recaptchaEnterpriseScoreAction").toString();
                final String siteKeyEnterpriseNormal = loginInfo.get("pixivAccount.recaptchaEnterpriseCheckboxSiteKey").toString();
                final boolean requiresNormalReCaptchaV2 = true;
                String reCaptchaV2Response = "";
                String reCaptchaEnterpriseInvisibleResponse = "";
                if (plugin instanceof PluginForHost) {
                    final PluginForHost plg = (PluginForHost) plugin;
                    final DownloadLink dlinkbefore = plg.getDownloadLink();
                    try {
                        if (dlinkbefore == null) {
                            plg.setDownloadLink(new DownloadLink(plg, "Account", plg.getHost(), "http://" + account.getHoster(), true));
                        }
                        final CaptchaHelperHostPluginRecaptchaV2 v3Captcha = new CaptchaHelperHostPluginRecaptchaV2(plg, br, siteKeyEnterpriseInvisible) {
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
                        if (requiresNormalReCaptchaV2) {
                            final CaptchaHelperHostPluginRecaptchaV2 v2Captcha = new CaptchaHelperHostPluginRecaptchaV2(plg, br, siteKeyEnterpriseNormal) {
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
                        }
                    } finally {
                        if (dlinkbefore != null) {
                            plg.setDownloadLink(dlinkbefore);
                        }
                    }
                } else if (plugin instanceof PluginForDecrypt) {
                    final PluginForDecrypt pluginForDecrypt = (PluginForDecrypt) plugin;
                    final CaptchaHelperCrawlerPluginRecaptchaV2 v3Captcha = new CaptchaHelperCrawlerPluginRecaptchaV2(pluginForDecrypt, br, siteKeyEnterpriseInvisible) {
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
                    if (requiresNormalReCaptchaV2) {
                        final CaptchaHelperCrawlerPluginRecaptchaV2 v2Captcha = new CaptchaHelperCrawlerPluginRecaptchaV2(pluginForDecrypt, br, siteKeyEnterpriseNormal) {
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
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("captcha", "");
                loginform.put("g_recaptcha_response", Encoding.urlEncode(reCaptchaV2Response));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("pixiv_id", Encoding.urlEncode(account.getUser()));
                loginform.put("post_key", Encoding.urlEncode(loginPostkey));
                loginform.put("source", Encoding.urlEncode(loginSource));
                loginform.put("app_ios", "0");
                loginform.put("ref", Encoding.urlEncode(loginRef));
                loginform.put("return_to", Encoding.urlEncode("https://www.pixiv.net/en/"));
                loginform.put("recaptcha_enterprise_score_token", Encoding.urlEncode(reCaptchaEnterpriseInvisibleResponse));
                loginform.put("tt", Encoding.urlEncode(loginTT));
                loginform.setAction("/api/login?lang=en");
                final Request loginRequest = br.createFormRequest(loginform);
                loginRequest.getHeaders().put("Accept", "application/json");
                loginRequest.getHeaders().put("Origin", "https://accounts." + account.getHoster());
                br.getPage(loginRequest);
                final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                if ((Boolean) response.get("error") == Boolean.TRUE) {
                    throw new AccountInvalidException();
                }
                final Map<String, Object> body = (Map<String, Object>) response.get("body");
                final String successURL = (String) JavaScriptEngineFactory.walkJson(body, "success/return_to");
                if (successURL == null) {
                    final Map<String, Object> validation_errors = (Map<String, Object>) body.get("validation_errors");
                    if (validation_errors != null && validation_errors.containsKey("captcha")) {
                        /* {"error":false,"message":"","body":{"validation_errors":{"captcha":"Complete the reCAPTCHA verification"}}} */
                        showCookieLoginInformation();
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    throw new AccountInvalidException();
                }
                br.getPage("https://www." + account.getHoster() + "/en");
                if (!isLoggedIN(br)) {
                    if (!account.hasEverBeenValid()) {
                        showCookieLoginInformation();
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

    public static Thread showCookieLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Pixiv.net - Login fehlgeschlagen - versuche es mit der Cookie Login Methode";
                        message += "Hallo liebe(r) Pixiv.net NutzerIn\r\n";
                        message += "Um deinen pixiv.net Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = "Pixiv.net - Login failed - try cookie login";
                        message += "Hello dear pixiv.net user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    // getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static boolean checkCookieLogin(final Plugin plugin, final Browser br, final Account account, final Cookies cookies, final boolean validateCookies) throws IOException {
        br.setCookies(account.getHoster(), cookies);
        if (!validateCookies) {
            plugin.getLogger().info("Trust cookies without check");
            return true;
        } else {
            br.getPage("https://www." + account.getHoster() + "/en");
            if (isLoggedIN(br)) {
                /* Refresh loggedin timestamp */
                plugin.getLogger().info("Cookie login successful");
                account.saveCookies(br.getCookies(account.getHoster()), "");
                return true;
            }
            plugin.getLogger().info("Cookie login failed");
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
        login(this, this.br, account, true);
        ai.setUnlimitedTraffic();
        if (br.containsHTML("premium\\s*:\\s*\\'yes\\'") || br.containsHTML("\"premium\"\\s*:\\s*true")) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
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
        login(this, this.br, account, false);
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