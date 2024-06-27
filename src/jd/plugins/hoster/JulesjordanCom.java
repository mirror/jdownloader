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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.JulesjordanComDecrypter;

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

    private String  dllink        = null;
    private boolean server_issues = false;

    public static Browser prepBR(final Browser br, final String host) {
        br.setFollowRedirects(true);
        return br;
    }

    public int getMaxChunks(final Account account) {
        return 0;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // final String decrypter_filename = link.getStringProperty("decrypter_filename", null);
        if (isTrailerURL(link.getPluginPatternMatcher())) {
            /* Trailer download */
            getPage(getURLFree(link.getPluginPatternMatcher()));
            if (JulesjordanComDecrypter.isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            long width_max = 0;
            String dllink_temp = null;
            final String[] jsons = this.br.getRegex("movie\\[\"[^\"]+\"\\]\\[\"[^\"]+\"\\] = (\\{.*?\\})").getColumn(0);
            for (final String json : jsons) {
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
                final long width = JavaScriptEngineFactory.toLong(entries.get("movie_width"), 0);
                dllink_temp = (String) entries.get("path");
                if (width > width_max && !StringUtils.isEmpty(dllink_temp)) {
                    width_max = width;
                    dllink = dllink_temp;
                }
            }
            if (dllink == null) {
                /* 2023-07-15 */
                dllink = br.getRegex("<source data-bitrate=\"trailer\"[^>]*src=\"(http[^\"]+)").getMatch(0);
            }
            String title = getURLName(link.getPluginPatternMatcher()).replace("-", " ");
            title = Encoding.htmlDecode(title).trim();
            /* Do NOT set final filename yet!! */
            link.setName(title + ".mp4");
            if (!StringUtils.isEmpty(dllink)) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
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
        } else {
            /* Full video (premium) download */
            dllink = link.getPluginPatternMatcher();
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 410) {
                    logger.info("Directurl expired --> Trying to refresh it");
                    if (account == null) {
                        logger.info("Cannot refresh directurl because: Account missing");
                        throw new AccountRequiredException();
                    }
                    this.login(account, false);
                    /* Refresh directurl */
                    final String mainlink = link.getStringProperty("mainlink");
                    final String quality = link.getStringProperty("quality");
                    if (mainlink == null || quality == null) {
                        /* This should never happen */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    br.getPage(mainlink);
                    /* Check if content has been removed in the meanwhile. */
                    if (JulesjordanComDecrypter.isOffline(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final HashMap<String, String> allQualities = JulesjordanComDecrypter.findAllQualities(this.br);
                    dllink = allQualities.get(quality);
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to refresh directurl");
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to refresh directurl");
                    }
                    con = br.openHeadConnection(dllink);
                }
                if (this.looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                } else {
                    this.server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, this.getMaxChunks(account));
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
        dl.startDownload();
    }

    private void handleGeneralErrors() throws PluginException {
        /* Fill me up */
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        return true;
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        final boolean is_this_plugin = link.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* Original plugin is always allowed to download. */
            return true;
        } else if (!link.isEnabled() && "".equals(link.getPluginPatternMatcher())) {
            /*
             * setMultiHostSupport uses a dummy DownloadLink, with isEnabled == false. we must set to true for the host to be added to the
             * supported host array.
             */
            return true;
        } else {
            /* MOCHs should only be tried for compatible URLs. */
            return isMultihostDownloadAllowed(link);
        }
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
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
                    if (!force) {
                        logger.info("Trust cookies without checking");
                        return;
                    } else {
                        br.getPage("https://www." + account.getHoster() + "/members/index.php");
                        if (isLoggedin(br)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearCookies(null);
                        }
                    }
                }
                logger.info("Performing full login");
                // br.getPage("https://www." + this.getHost() + "/trial/index.php");
                br.getPage("https://www." + account.getHoster() + "/members/");
                br.setCookie(br.getHost(), "CookieScriptConsent", "{\"action\":\"accept\"}");
                String postdata = "rlm=My+Server&for=https%253a%252f%252fwww%252ejulesjordan%252ecom%252fmembers%252f&uid=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass()) + "&rmb=y";
                final String code = this.getCaptchaCode("/img.cptcha", this.getDownloadLink());
                postdata += "&img=" + Encoding.urlEncode(code);
                br.getHeaders().put("Origin", "https://www.julesjordan.com");
                /* 2021-07-26: Without these headers we'll always get http response 500! */
                br.getHeaders().put("Cache-Control", "max-age=0");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                // if (br.getCookie(br.getHost(), "pcar%5fTXkgU2VydmVy", Cookies.NOTDELETEDPATTERN) != null) {
                // /* 2021-07-26: Debug-test: Remove "pcar" cookie */
                // final Cookies thiscookies = br.getCookies(br.getHost());
                // final Cookies newcookies = new Cookies();
                // for (final Cookie cookie : thiscookies.getCookies()) {
                // if (cookie.getKey().equals("pcar%5fTXkgU2VydmVy")) {
                // continue;
                // }
                // newcookies.add(cookie);
                // }
                // br.clearCookies(br.getHost());
                // br.setCookies(newcookies);
                // }
                br.postPage("/auth.form", postdata);
                if (br.containsHTML("captcha_x=1")) {
                    /* User entered invalid login-captcha */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else if (br.getURL().matches("(?i).+/expired/?$")) {
                    /* Login is valid but account is not premium anymore. */
                    throw new AccountInvalidException("Account is expired");
                } else if (!isLoggedin(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        if (br.containsHTML("(?i)members/logout")) {
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
        /*
         * 2017-08-02: No way to verify premium status and/or expire date - I guess if an account works, it always has a subscription
         * (premium status) ...
         */
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String buildExternalDownloadURL(final DownloadLink link, final PluginForHost buildForThisPlugin) {
        if (!StringUtils.equals(this.getHost(), buildForThisPlugin.getHost()) && isMultihostDownloadAllowed(link)) {
            return getURLFree(link.getPluginPatternMatcher());
        } else {
            return super.buildExternalDownloadURL(link, buildForThisPlugin);
        }
    }

    public static String getTitle(final Browser br) {
        final String title = br.getRegex("class=\"title_bar\">([^<>\"]+)<").getMatch(0);
        return title;
    }

    private boolean isMultihostDownloadAllowed(final DownloadLink dl) {
        return isTrailerURL(dl.getPluginPatternMatcher());
    }

    public static boolean isTrailerURL(final String inputurl) {
        return inputurl != null && (inputurl.matches("(?i).+/(?:trial|members)/.+\\.html$"));
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
        return -1;
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