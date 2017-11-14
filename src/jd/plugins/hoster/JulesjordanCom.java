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

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "julesjordan.com" }, urls = { "https?://dl\\d+\\.julesjordan\\.com/dl/.+|https?://(?:www\\.)?julesjordan\\.com/(?:trial|members)/(?:movies|scenes)/[^/]+\\.html" })
public class JulesjordanCom extends antiDDoSForHost {
    public JulesjordanCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://enter.julesjordan.com/signup/signup.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.julesjordan.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   html_loggedin                = "members/logout";
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br, final String host) {
        br.setFollowRedirects(true);
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            /* 2017-08-02: Login not required, neither for premium-direct-downloadlinks */
            this.login(this.br, aa, false);
        }
        // final String decrypter_filename = link.getStringProperty("decrypter_filename", null);
        if (!isTrailerURL(link.getDownloadURL())) {
            dllink = link.getDownloadURL();
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 410) {
                    logger.info("Directurl expired --> Trying to refresh it");
                    /* Refresh directurl */
                    final String mainlink = link.getStringProperty("mainlink");
                    final String quality = link.getStringProperty("quality");
                    if (mainlink == null || quality == null) {
                        /* This should never happen */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    br.getPage(mainlink);
                    if (jd.plugins.decrypter.JulesjordanComDecrypter.isOffline(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final HashMap<String, String> allQualities = jd.plugins.decrypter.JulesjordanComDecrypter.findAllQualities(this.br);
                    dllink = allQualities.get(quality);
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to refresh directurl");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                    }
                    con = br.openHeadConnection(dllink);
                }
                if (!con.getContentType().contains("html")) {
                    link.setFinalFileName(getFileNameFromHeader(con));
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    this.server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* Trailer download */
            getPage(getURLFree(link.getDownloadURL()));
            if (jd.plugins.decrypter.JulesjordanComDecrypter.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            long width_max = 0;
            String dllink_temp = null;
            final String[] jsons = this.br.getRegex("movie\\[\"[^\"]+\"\\]\\[\"[^\"]+\"\\] = (\\{.*?\\})").getColumn(0);
            for (final String json : jsons) {
                final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
                final long width = JavaScriptEngineFactory.toLong(entries.get("movie_width"), 0);
                dllink_temp = (String) entries.get("path");
                if (width > width_max && !StringUtils.isEmpty(dllink_temp)) {
                    width_max = width;
                    dllink = dllink_temp;
                }
            }
            final String url_name = getURLName(link.getDownloadURL());
            String filename = null;
            /* Fallback in case we do not get any filename via html code */
            if (inValidate(filename)) {
                filename = url_name;
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            /* Do NOT set final filename yet!! */
            link.setName(filename + ".mp4");
            if (!StringUtils.isEmpty(dllink)) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handleGeneralErrors();
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            /*
             * If trailer download is possible but dllink == null in theory this would be a PLUGIN_DEFECT but I think that premiumonly
             * message is more suitable here as a trailer is usually not what you'd want to download.
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
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

    private void handleGeneralErrors() throws PluginException {
        /* Fill me up */
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        // return account != null;
        return true;
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* Original plugin is always allowed to download. */
            return true;
        } else if (!downloadLink.isEnabled() && "".equals(downloadLink.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* MOCHs should only be tried for compatible URLs. */
            return isMOCHUrlOnly(downloadLink);
        }
    }

    private static Object LOCK = new Object();

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(br, account.getHoster());
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /*
                     * Try to avoid login captcha at all cost! Important: ALWAYS check this as their cookies can easily become invalid e.g.
                     * when the user logs in via browser.
                     */
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://www." + account.getHoster() + "/members/index.php");
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    br = prepBR(new Browser(), account.getHoster());
                }
                br.getPage("https://www." + account.getHoster() + "/members/");
                String postdata = "rlm=My+Server&for=https%253a%252f%252fwww%252ejulesjordan%252ecom%252fmembers%252f&uid=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass()) + "&rmb=y";
                final DownloadLink dlinkbefore = this.getDownloadLink();
                if (dlinkbefore == null) {
                    this.setDownloadLink(new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true));
                }
                final String code = this.getCaptchaCode("/img.cptcha", this.getDownloadLink());
                postdata += "&img=" + Encoding.urlEncode(code);
                br.postPage("/auth.form", postdata);
                if (!br.containsHTML(html_loggedin)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
                if (dlinkbefore != null) {
                    this.setDownloadLink(dlinkbefore);
                }
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        /*
         * 2017-08-02: No way to verify premium status and/or expire date - I guess if an account works, it always has a subscription
         * (premium status) ...
         */
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (account.getType() == AccountType.FREE) {
            /* This should never happen! */
            logger.warning("Entering untested free account code");
            doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleGeneralErrors();
            if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink downloadLink, final PluginForHost buildForThisPlugin) {
        if (!StringUtils.equals(this.getHost(), buildForThisPlugin.getHost()) && isMOCHUrlOnly(downloadLink)) {
            return getURLFree(downloadLink.getDownloadURL());
        } else {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    public static String getTitle(final Browser br) {
        final String title = br.getRegex("class=\"title_bar\">([^<>\"]+)<").getMatch(0);
        return title;
    }

    private boolean isMOCHUrlOnly(final DownloadLink dl) {
        return isTrailerURL(dl.getDownloadURL());
    }

    public static boolean isTrailerURL(final String inputurl) {
        return inputurl != null && (inputurl.matches(".+/(?:trial|members)/.+\\.html$"));
    }

    public static String getURLName(final String inputurl) {
        final String url_name;
        if (isTrailerURL(inputurl)) {
            url_name = new Regex(inputurl, "/([^/]+)\\.html$").getMatch(0);
        } else {
            url_name = new Regex(inputurl, "/([^/]+\\.mp4)$").getMatch(0);
        }
        return url_name;
    }

    public static String getURLFree(final String inputurl) {
        if (inputurl == null) {
            return null;
        }
        final String host = Browser.getHost(inputurl);
        if (host == null) {
            return null;
        }
        final String linkpart = new Regex(inputurl, "https?://[^/]+/[^/]+/(.+)").getMatch(0);
        return String.format("https://www.%s/trial/%s", host, linkpart);
    }

    public static String getURLPremium(final String inputurl) {
        if (inputurl == null) {
            return null;
        }
        final String host = Browser.getHost(inputurl);
        if (host == null) {
            return null;
        }
        final String linkpart = new Regex(inputurl, "https?://[^/]+/[^/]+/(.+)").getMatch(0);
        return String.format("https://www.%s/members/%s", host, linkpart);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the julesjordan.com plugin.";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return JulesjordanComConfigInterface.class;
    }

    public static interface JulesjordanComConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }

            public String getAddUnknownQualitiesEnabled_label() {
                return _JDT.T.lit_add_unknown_formats();
            }

            public String getGrabHTTPMp4_4kEnabled_label() {
                return "Grab 4k HD MP4 10804k (mp4)?";
            }

            public String getGrabHTTPMp4_1080pEnabled_label() {
                return "Grab 1080p HD MP4 1080P (mp4)?";
            }

            public String getGrabHTTPMp4_720pHDEnabled_label() {
                return "Grab 720p HD MP4 720P (mp4)?";
            }

            public String getGrabHTTPMp4_MobileSDEnabled_label() {
                return "Grab Mobile SD MP4 (mp4)?";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(22)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4_4kEnabled();

        void setGrabHTTPMp4_4kEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4_1080pEnabled();

        void setGrabHTTPMp4_1080pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTPMp4_720pHDEnabled();

        void setGrabHTTPMp4_720pHDEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTPMp4_MobileSDEnabled();

        void setGrabHTTPMp4_MobileSDEnabled(boolean b);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}