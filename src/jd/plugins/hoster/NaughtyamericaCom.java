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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.NaughtyamericaConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.NaughtyamericaComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyamerica.com" }, urls = { "http://naughtyamericadecrypted.+" })
public class NaughtyamericaCom extends PluginForHost {
    public NaughtyamericaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://natour.naughtyamerica.com/signup/signup.php");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        // br.addAllowedResponseCodes(new int[] { 456 });
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (allowCookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.naughtyamerica.com/terms-of-service.html";
    }

    /* Connection stuff */
    private static final boolean allowCookieLoginOnly         = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    /*
     * 2017-01-23: Max 100 connections tital seems to be a stable value - I'd not recommend allowing more as this will most likely cause
     * failing downloads which start over and over.
     */
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String         type_pic                     = ".+\\.jpg.*?";
    private String               dllink                       = null;
    public static final String   PROPERTY_CONTENT_ID          = "fid";
    public static final String   PROPERTY_VIDEO_QUALITY       = "quality";
    public static final String   PROPERTY_URL_SLUG            = "filename_url";
    public static final String   PROPERTY_PICTURE_NUMBER      = "picnumber";
    public static final String   PROPERTY_CRAWLER_FILENAME    = "crawler_filename";
    public static final String   PROPERTY_MAINLINK            = "mainlink";

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll("(?i)https?://naughtyamericadecrypted", "https://"));
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.PREMIUM.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2022-03-17: This property should always exist but to keep compatibility to older plugin revisions we're not yet enforcing it. */
        final String crawlerForcedFilename = link.getStringProperty(PROPERTY_CRAWLER_FILENAME);
        if (crawlerForcedFilename != null) {
            link.setFinalFileName(crawlerForcedFilename);
        }
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, false);
            dllink = link.getPluginPatternMatcher();
        } else {
            logger.info("No account available, checking trailer download");
            final String urlSlug = link.getStringProperty("filename_url");
            if (urlSlug == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String main_video_url_free = NaughtyamericaComCrawler.getVideoUrlFree(urlSlug);
            br.getPage(main_video_url_free);
            if (NaughtyamericaComCrawler.isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* 2019-01-18: Trailer is only available in a single quality */
            dllink = br.getRegex("file\\s*?:\\s*?\"(https?[^<>\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                link.getLinkStatus().setStatusText("Trailer not found: Cannot find download filesize without valid premium account");
                return AvailableStatus.TRUE;
            }
        }
        URLConnectionAdapter con = null;
        try {
            Browser brc = br.cloneBrowser();
            con = brc.openHeadConnection(dllink);
            if (!looksLikeDownloadableContent(con)) {
                logger.info("Final downloadurl seems to be expired");
                try {
                    brc.followConnection(true);
                } catch (IOException ignore) {
                    logger.log(ignore);
                }
                final String directURL = refreshDirecturl(aa, link);
                brc = br.cloneBrowser();
                con = brc.openHeadConnection(directURL);
                if (!looksLikeDownloadableContent(con)) {
                    brc.followConnection(true);
                    errorNoFile();
                }
                /* Save new directURL for next usage. */
                link.setPluginPatternMatcher(directURL);
                dllink = directURL;
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            final String filenameFromConnection = getFileNameFromHeader(con);
            if (link.getFinalFileName() == null && filenameFromConnection != null) {
                link.setFinalFileName(Encoding.htmlDecode(filenameFromConnection).trim());
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private String refreshDirecturl(final Account account, final DownloadLink link) throws Exception {
        final String mainlink = link.getStringProperty(PROPERTY_MAINLINK);
        if (mainlink == null) {
            /* This should never happen! Can happen for URLs added via revision 45663 and before. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Let crawler run once again. */
        final PluginForDecrypt crawler = this.getNewPluginForDecryptInstance(this.getHost());
        /* Find expected result. */
        final CryptedLink forCrawler = new CryptedLink(mainlink, link);
        final ArrayList<DownloadLink> results = ((jd.plugins.decrypter.NaughtyamericaComCrawler) crawler).crawlContent(forCrawler, true);
        DownloadLink result = null;
        if (link.hasProperty(PROPERTY_PICTURE_NUMBER)) {
            /* Image */
            for (final DownloadLink tmp : results) {
                if (StringUtils.equals(tmp.getStringProperty(PROPERTY_PICTURE_NUMBER), link.getStringProperty(PROPERTY_PICTURE_NUMBER))) {
                    result = tmp;
                    break;
                }
            }
        } else {
            /* Video */
            for (final DownloadLink tmp : results) {
                if (StringUtils.equals(this.getLinkID(tmp), this.getLinkID(link))) {
                    result = tmp;
                    break;
                }
            }
        }
        if (result == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to refresh directurl");
        }
        this.correctDownloadLink(result);
        final String directURL = result.getPluginPatternMatcher();
        return directURL;
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty("fid");
    }

    private String getUrlSlug(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_URL_SLUG);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(null, link);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                /* For developers: Disable this Boolean if normal login process breaks down and you're unable or too lazy to fix it! */
                final Cookies userCookies = account.loadUserCookies();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null || userCookies != null) {
                    /*
                     * Try to avoid login captcha by re-using cookies.
                     */
                    if (userCookies != null) {
                        br.setCookies(userCookies);
                    } else {
                        br.setCookies(cookies);
                    }
                    if (!force) {
                        return;
                    }
                    br.getPage("https://" + NaughtyamericaComCrawler.DOMAIN_PREFIX_PREMIUM + account.getHoster());
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        } else {
                            /* Full login required */
                            br.clearAll();
                        }
                    }
                }
                if (allowCookieLoginOnly) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                logger.info("Performing full login");
                br.getPage("https://" + NaughtyamericaComCrawler.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/");
                br.getPage("https://" + NaughtyamericaComCrawler.DOMAIN_PREFIX_PREMIUM + account.getHoster() + "/login");
                final Regex httpRedirect = br.getRegex("http-equiv=\"refresh\" content=\"(\\d+);\\s*url=(/[^<>\"]+)\"");
                if (httpRedirect.matches()) {
                    /* 2019-01-21: Hmm leads to HTTP/1.1 405 Not Allowed */
                    /*
                     * <meta http-equiv="refresh" content="10; url=/distil_r_captcha.html?requestId=<requestId>c&httpReferrer=%2Flogin" />
                     */
                    final String waitStr = httpRedirect.getMatch(0);
                    final String redirectURL = httpRedirect.getMatch(1);
                    int wait = 10;
                    if (waitStr != null) {
                        wait = Integer.parseInt(waitStr);
                    }
                    Thread.sleep(wait * 1001l);
                    br.getPage(redirectURL);
                }
                Form loginform = br.getFormbyKey("username");
                if (loginform == null) {
                    loginform = br.getForm(0);
                }
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("dest", "");
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                /* Handle login captcha if required */
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                final Request request = br.createFormRequest(loginform);
                request.getHeaders().put("Origin", "https://members.naughtyamerica.com");
                br.getPage(request);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /* Redirect from probiller.com to main website --> Login complete */
                    br.submitForm(continueform);
                }
                if (br.getURL().contains("/postLogin")) {
                    br.getPage("//" + NaughtyamericaComCrawler.DOMAIN_PREFIX_PREMIUM + br.getHost());
                }
                if (br.getURL().contains("beta.") || br.getURL().contains("/login")) {
                    /* 2016-12-12: Redirects to their beta-page might happen --> Go back to the old/stable version of their webpage. */
                    br.getPage("https://" + NaughtyamericaComCrawler.DOMAIN_PREFIX_PREMIUM + account.getHoster());
                }
                final String loginCookie = br.getCookie(br.getHost(), "nrc", Cookies.NOTDELETEDPATTERN);
                if (!isLoggedIN(br) && loginCookie == null) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout\"")) {
            return true;
        } else {
            return false;
        }
    }

    /***
     * 2022-03-17: Not much account information for us to crawl. User purchases can be found on website but expire-date or next bill date is
     * nowhere to be found: </br>
     * https://members.naughtyamerica.com/account/purchases
     */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        /* 2022-03-17: Their cookies are not valid for a long time -> Be sure to keep them active */
        account.setRefreshTimeout(5 * 60 * 1000l);
        br.getPage("/account/profile");
        String username = null;
        final Form profileform = br.getFormbyActionRegex(".*/account/profile");
        if (profileform != null) {
            final InputField ifield = profileform.getInputField("display_name");
            if (ifield != null) {
                username = ifield.getValue();
            }
        }
        if (username == null) {
            logger.warning("Failed to find real username inside html code");
        }
        if (account.loadUserCookies() != null && username != null) {
            /*
             * Try to use unique usernames even if user did use cookie login as in theory he can enter whatever he wants into that username
             * field when using cookie login.
             */
            account.setUser(username);
        }
        return ai;
    }

    private void handleDownload(final Account account, final DownloadLink link) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            /* Usually only happens in free mode e.g. trailer download --> But no trailer is available */
            throw new AccountRequiredException();
        }
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), ACCOUNT_PREMIUM_MAXCHUNKS);
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), FREE_MAXCHUNKS);
        }
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            errorNoFile();
        }
        dl.startDownload();
    }

    private void errorNoFile() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Final downloadurl did not return file-content");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDownload(account, link);
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /* 2019-01-18: Without account: Trailer download, with account: full video download */
        return true;
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        final boolean is_this_plugin = link.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else if (!link.isEnabled() && "".equals(link.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* Multihosts should not be tried for picture-downloads! */
            return !link.getDownloadURL().matches(type_pic);
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        if (StringUtils.equals("premiumize.me", buildForThisPlugin.getHost())) {
            return jd.plugins.decrypter.NaughtyamericaComCrawler.getVideoUrlFree(getUrlSlug(link));
        } else {
            return super.buildExternalDownloadURL(link, buildForThisPlugin);
        }
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the naughtyamerica.com plugin.";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return NaughtyamericaConfig.class;
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
}