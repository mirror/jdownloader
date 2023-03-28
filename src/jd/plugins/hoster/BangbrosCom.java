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
import java.util.Random;

import org.appwork.utils.StringUtils;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BangbrosComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bangbros.com", "mygf.com" }, urls = { "bangbrosdecrypted://.+", "mygfdecrypted://.+" })
public class BangbrosCom extends PluginForHost {
    public BangbrosCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.bangbrosnetwork.com/bangbrothers/join");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.bangbros.com/terms.htm";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   PROPERTY_FID                 = "fid";
    public static final String   PROPERTY_PRODUCT_ID          = "product_id";
    public static final String   PROPERTY_QUALITY             = "quality";
    public static final String   PROPERTY_MAINLINK            = "mainlink";
    public static final String   PROPERTY_STREAMING_DIRECTURL = "streaming_directurl";
    private final String         DOMAIN_PREFIX_PREMIUM        = "members.";
    private String               dllink                       = null;
    private boolean              logged_in                    = false;

    public static Browser prepBR(final Browser br) {
        /* This may happen after logging in but usually login process will be okay anways. */
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        return br;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll("[a-z0-9]+decrypted://", "https://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String final_filename = link.getStringProperty("decryptername", null);
        final boolean loginRequired = BangbrosComCrawler.requiresAccount(this.getMainlink(link));
        if (account != null) {
            this.login(this.br, account, false);
            logged_in = true;
        } else {
            logged_in = false;
        }
        if (!this.logged_in && loginRequired) {
            link.getLinkStatus().setStatusText("Cannot check this link type without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        final String mainlink = this.getMainlink(link);
        boolean allowRefreshDirecturl = true;
        if (mainlink.matches(BangbrosComCrawler.type_userinput_video_couldbe_trailer)) {
            br.getPage(mainlink);
            if (BangbrosComCrawler.isOffline(this.br, mainlink)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRegex("var\\s*?videoLink\\s*?=\\s*?\\'(http[^<>\"\\']+)\\';").getMatch(0);
            if (dllink == null) {
                /* 2019-01-14: New */
                dllink = br.getRegex("<source src=\"(?:https?:)?(//[^<>\"]+/trailerx/[^<>\"]+)\" type=\\'video/mp4\\' />").getMatch(0);
            }
        } else {
            final String qualityIdentifier = link.getStringProperty(PROPERTY_QUALITY);
            if (qualityIdentifier != null && qualityIdentifier.matches("\\d+p")) {
                dllink = link.getStringProperty(PROPERTY_STREAMING_DIRECTURL);
                if (dllink == null) {
                    logger.info("Looking for streaming URL...");
                    final String fid = this.getFID(link);
                    final String productid = link.getStringProperty(PROPERTY_PRODUCT_ID);
                    if (fid == null || productid == null) {
                        /* This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    this.br.getPage("https://" + DOMAIN_PREFIX_PREMIUM + this.getHost() + "/product/" + productid + "/movie/" + fid + "/" + qualityIdentifier);
                    if (BangbrosComCrawler.isOffline(this.br, mainlink)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    dllink = BangbrosComCrawler.regexStreamingURL(br, qualityIdentifier);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    allowRefreshDirecturl = false;
                }
            } else {
                dllink = link.getPluginPatternMatcher();
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    if (!allowRefreshDirecturl) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media file?");
                    }
                    logger.info("Refreshing directurl");
                    refreshDirecturl(link, account);
                    con = br.openHeadConnection(dllink);
                    if (!this.looksLikeDownloadableContent(con)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media file?");
                    }
                }
                link.setVerifiedFileSize(con.getCompleteContentLength());
                if (final_filename == null) {
                    final_filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (final_filename != null) {
            link.setFinalFileName(final_filename);
        }
        return AvailableStatus.TRUE;
    }

    private void refreshDirecturl(final DownloadLink link, final Account account) throws Exception {
        logger.info("Trying to refresh expired directurl");
        final String fid = getFID(link);
        final String quality = link.getStringProperty(PROPERTY_QUALITY);
        // String product_id = link.getStringProperty("productid");
        // if (product_id == null) {
        // /* Fallback to most popular product 'bangbros'. */
        // product_id = "1";
        // }
        final String mainlink = link.getStringProperty(PROPERTY_MAINLINK);
        if (fid == null || quality == null || mainlink == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final BangbrosComCrawler crawlerplugin = (BangbrosComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
        final ArrayList<DownloadLink> results = crawlerplugin.decryptIt(new CryptedLink(mainlink), account, null);
        DownloadLink freshLink = null;
        for (final DownloadLink result : results) {
            if (quality.equals(result.getStringProperty(PROPERTY_QUALITY))) {
                freshLink = result;
                break;
            }
        }
        if (freshLink == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        correctDownloadLink(freshLink);
        this.dllink = freshLink.getPluginPatternMatcher();
        link.setPluginPatternMatcher(freshLink.getPluginPatternMatcher());
        link.setProperties(freshLink.getProperties());
    }

    private String getFID(final DownloadLink dl) {
        return dl.getStringProperty(PROPERTY_FID);
    }

    private String getMainlink(final DownloadLink dl) {
        return dl.getStringProperty(PROPERTY_MAINLINK);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("free_directlink", this.dllink);
        dl.startDownload();
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
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    logger.info("Performing user-cookie login");
                    br.setCookies(account.getHoster(), userCookies);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    if (checkLogin(br)) {
                        logger.info("User cookie login successful");
                        return;
                    } else {
                        logger.info("User cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException("Cookies expired");
                        } else {
                            throw new AccountInvalidException("Cookies invalid");
                        }
                    }
                }
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    if (checkLogin(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                        prepBR(br);
                    }
                }
                logger.info("Performing full login");
                /* 2020-08-24: Cookie login is NEEDED for mygf.com and can be used for bangbros.com too but is not needed! */
                if (account.getHoster().equals("mygf.com") && userCookies == null) {
                    if (!account.hasEverBeenValid()) {
                        showCookieLoginInfo();
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Enter cookies to login", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                logger.info("Performing full login");
                /*
                 * 2020-08-21: Not all websites support https e.g. mygf.com doesn't so we rather use http here and let them redirect us to
                 * https if available.
                 */
                br.getPage("https://" + DOMAIN_PREFIX_PREMIUM + this.getHost() + "/login");
                final Form loginform = br.getForm(0);
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login%5Busername%5D", Encoding.urlEncode(account.getUser()));
                loginform.put("login%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                loginform.put("profiler_input", Integer.toString(new Random().nextInt(1000)));
                if (loginform.containsHTML("g-recaptcha")) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(loginform);
                if (!br.getURL().contains("/library")) {
                    br.getPage("/library");
                }
                if (!isLoggedin(br) || !br.getURL().contains("/library")) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean checkLogin(final Browser br) throws IOException {
        br.getPage("https://" + DOMAIN_PREFIX_PREMIUM + this.getHost() + "/library");
        if (isLoggedin(br)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoggedin(final Browser br) {
        // final String logincookie = br.getCookie(account.getHoster(), "bangbros_remember_me");
        // final String logincookie2 = br.getCookie(account.getHoster(), "st_login");
        // final boolean isLoggedin = (logincookie != null && !"deleted".equalsIgnoreCase(logincookie)) || (logincookie2 != null &&
        // !"deleted".equalsIgnoreCase(logincookie2));
        return br.containsHTML("all_purchased_switcher") || br.getCookie(this.getHost(), "bangbros_remember_me", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        /* 2020-08-12: Assume that all valid accounts are premium accounts */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (StringUtils.isEmpty(dllink)) {
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
        } else if (getMainlink(link) != null && getMainlink(link).matches(BangbrosComCrawler.type_userinput_video_couldbe_trailer)) {
            /* Multihost download only possible for specified linktype. */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (buildForThisPlugin != null && !StringUtils.equals(this.getHost(), buildForThisPlugin.getHost())) {
            return getMainlink(downloadLink);
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the bangbros.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_480p", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_720p", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_1080p", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_2160p", "Grab 2160p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_photos", "Grab photos (.zip containing images)?").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_screencaps", "Grab screencaps (.zip containing images)?").setDefaultValue(false));
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