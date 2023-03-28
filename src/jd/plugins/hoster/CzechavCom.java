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

import jd.PluginWrapper;
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
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CzechavComCrawler;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.CzechavComConfigInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czechav.com" }, urls = { "" })
public class CzechavCom extends PluginForHost {
    public CzechavCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.czechav.com/en/join/");
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public String getAGBLink() {
        return "http://www.czechav.com/en/tos/";
    }

    /* Connection stuff */
    // private static final boolean FREE_RESUME = false;
    // private static final int FREE_MAXCHUNKS = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static final String   PROPERTY_IMAGE_POSITION      = "image_position";

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (account == null) {
            throw new AccountRequiredException();
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(link.getPluginPatternMatcher());
            if (!this.looksLikeDownloadableContent(con)) {
                /* Refresh directurl */
                logger.info("Refreshing directurl...");
                final String videoResolution = CzechavComCrawler.getVideoResolution(link.getPluginPatternMatcher());
                final int imagePosition = link.getIntegerProperty(PROPERTY_IMAGE_POSITION, -1);
                if (videoResolution == null && imagePosition == -1) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final CzechavComCrawler plg = (CzechavComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
                final ArrayList<DownloadLink> results = plg.crawlMedia(new CryptedLink(link.getContainerUrl()), true);
                DownloadLink hit = null;
                for (final DownloadLink result : results) {
                    if (result.hasProperty(PROPERTY_IMAGE_POSITION) && result.getIntegerProperty(PROPERTY_IMAGE_POSITION) == imagePosition) {
                        hit = result;
                        break;
                    } else {
                        final String videoResolutionTmp = CzechavComCrawler.getVideoResolution(result.getPluginPatternMatcher());
                        if (StringUtils.equals(videoResolutionTmp, videoResolution)) {
                            hit = result;
                            break;
                        }
                    }
                }
                if (hit == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperties(hit.getProperties());
                link.setPluginPatternMatcher(hit.getPluginPatternMatcher());
                logger.info("New directurl: " + hit.getPluginPatternMatcher());
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media?");
                }
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
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
                    if (!force) {
                        /* Do not verify cookies */
                        return;
                    }
                    br.getPage("https://" + account.getHoster() + "/members/galleries/");
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        return;
                    } else {
                        logger.info("Cookie login failed --> Performing full login");
                        account.clearCookies("");
                        br.clearCookies(br.getHost());
                    }
                }
                br.getPage("https://" + this.getHost() + "/members/login/");
                Form loginform = br.getFormbyProperty("id", "login_form");
                if (loginform == null) {
                    loginform = br.getForm(0);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember_me", "on");
                br.submitForm(loginform);
                if (!this.isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/members/logout")) {
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
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        final boolean is_this_plugin = link.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            /* The original plugin is always allowed to download. */
            return true;
        } else {
            /* Multihost download impossible. */
            return false;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null && account.getType() == AccountType.PREMIUM) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return CzechavComConfigInterface.class;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the czechav.com plugin.";
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