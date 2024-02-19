//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import org.appwork.utils.DebugMode;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
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
import jd.plugins.decrypter.VscoCoCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VscoCo extends PluginForHost {
    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://" + getHost() + "/user/signup");
        }
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (cookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    /* Connection stuff */
    private static final int   free_maxchunks    = 0;
    public static final String PROPERTY_MEDIA_ID = "media_id";
    public static final String PROPERTY_QUALITY  = "quality";
    private final String       PROPERTY_HLS_URL  = "hls_url";
    private final boolean      cookieLoginOnly   = true;

    public static List<String[]> getPluginDomains() {
        return VscoCoCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (int i = 0; i < getPluginDomains().size(); i++) {
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://vsco.co/about/terms_of_use";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getMediaID(link);
        if (linkid != null && (isHLSVideo(link) || link.getStringProperty(PROPERTY_HLS_URL) != null)) {
            return this.getHost() + "://" + "/" + getUsername(link) + "/" + linkid + "/" + getQuality(link);
        } else {
            /*
             * Do not return special linkID for older http videos and images as we rely purely on the http links for dupechecking. Also for
             * "backwards compatibility" see https://board.jdownloader.org/showthread.php?p=509192#post509192
             */
            return super.getLinkID(link);
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private String getMediaID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_MEDIA_ID);
    }

    private String getQuality(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_QUALITY, "best");
    }

    private String getUsername(final DownloadLink link) {
        return link.getStringProperty(VscoCoCrawler.PROPERTY_USERNAME);
    }

    private boolean isVideo(final DownloadLink link) {
        if (link.getName().contains(".mp4") || link.getPluginPatternMatcher().contains(".mp4")) {
            return true;
        } else if (isHLSVideo(link)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isHLSVideo(final DownloadLink link) {
        if (link.getPluginPatternMatcher().contains(".m3u8")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        URLConnectionAdapter con = null;
        final String singleHLSURL = link.getStringProperty(PROPERTY_HLS_URL);
        try {
            if (singleHLSURL != null) {
                con = br.openHeadConnection(singleHLSURL);
            } else if (isHLSVideo(link)) {
                con = br.openGetConnection(link.getPluginPatternMatcher());
            } else {
                /* 2022-08-30: No HEAD-request allowed! */
                con = br.openGetConnection(link.getPluginPatternMatcher());
            }
            if (LinkCrawlerDeepInspector.looksLikeMpegURL(con)) {
                if (singleHLSURL == null) {
                    /* First run: Find best quality */
                    br.followConnection();
                    final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
                    final HlsContainer bestQuality = HlsContainer.findBestVideoByBandwidth(qualities);
                    link.setProperty(PROPERTY_QUALITY, bestQuality.getHeight());
                    link.setProperty(PROPERTY_HLS_URL, bestQuality.getDownloadurl());
                    logger.info("Set best quality on first full linkcheck: " + bestQuality.getHeight() + "p");
                }
            } else if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (isHLSVideo(link)) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, link.getStringProperty(PROPERTY_HLS_URL));
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), this.isResumeable(link, null), free_maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            }
            dl.startDownload();
        }
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            final String accounturl = "/user/account";
            if (cookies != null || userCookies != null) {
                logger.info("Attempting cookie login");
                if (userCookies != null) {
                    br.setCookies(this.getHost(), userCookies);
                } else {
                    br.setCookies(this.getHost(), cookies);
                }
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
                // Chrome/121.0.0.0 Safari/537.36");
                br.getPage("https://" + this.getHost() + accounturl);
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    if (userCookies != null) {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
            }
            if (cookieLoginOnly) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            logger.info("Performing full login");
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // TODO: Add username + password login support
            br.getPage("https://" + this.getHost() + "/login.php");
            final Form loginform = br.getFormbyProperty("name", "bla");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(accounturl);
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("class=\"sign-out\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.removeProperty(PROPERTY_HLS_URL);
    }
}