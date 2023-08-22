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
import java.net.URL;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.TumblrComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "https://[a-z0-9]+\\.media\\.tumblr\\.com/.+|https?://vtt\\.tumblr\\.com/tumblr_[A-Za-z0-9]+\\.mp4" })
public class TumblrCom extends PluginForHost {
    public static final long trust_cookie_age = 300000l;

    public TumblrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.tumblr.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.tumblr.com/terms_of_service";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /* API docs: https://www.tumblr.com/docs/en/api/v2#what-you-need */
    public static final String  API_BASE                 = "https://www.tumblr.com/api/v2";
    public static final String  API_BASE_WITHOUT_VERSION = "https://www.tumblr.com/api";
    private static final String PROPERTY_APIKEY          = "apikey";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        prepareBrowserForDownload(this.br, link);
        final Browser brc = br.cloneBrowser();
        final URLConnectionAdapter con = brc.openHeadConnection(link.getPluginPatternMatcher());
        try {
            connectionAvailablecheck(link, brc, con);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    /** Sets some important headers. Especially .gifv (.webp) images may not be downloadable without these headers! */
    private void prepareBrowserForDownload(final Browser br, final DownloadLink link) {
        this.br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        this.br.getHeaders().put("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        br.setFollowRedirects(true);
    }

    private void connectionAvailablecheck(final DownloadLink link, final Browser br, final URLConnectionAdapter con) throws Exception {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
        }
        if (con.getCompleteContentLength() > 0) {
            link.setVerifiedFileSize(con.getCompleteContentLength());
        }
        String fileName = Plugin.getFileNameFromDispositionHeader(con);
        if (fileName == null) {
            try {
                fileName = Plugin.getFileNameFromURL(new URL(link.getPluginPatternMatcher()));
            } catch (IOException e) {
                logger.log(e);
            }
        }
        if (fileName != null) {
            link.setFinalFileName(fileName);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        prepareBrowserForDownload(this.br, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 1);
        this.connectionAvailablecheck(link, br, dl.getConnection());
        dl.startDownload();
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Cookies userCookies = account.loadUserCookies();
            String apikey = account.getStringProperty(PROPERTY_APIKEY);
            if (userCookies == null) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            if (userCookies != null && apikey != null) {
                br.setCookies(account.getHoster(), userCookies);
                br.getHeaders().put("Authorization", "Bearer " + apikey);
                if (!force) {
                    logger.info("Trust cookies without check");
                    return;
                } else {
                    /* Check cookies */
                    br.getHeaders().put("Authorization", "Bearer " + apikey);
                    br.getPage(API_BASE + "/user/info?fields%5Bblogs%5D=avatar%2Cname%2Ctitle%2Curl%2Ccan_message%2Cdescription%2Cis_adult%2Cuuid%2Cis_private_channel%2Cposts%2Cis_group_channel%2C%3Fprimary%2C%3Fadmin%2C%3Fdrafts%2C%3Ffollowers%2C%3Fqueue%2C%3Fhas_flagged_posts%2Cmessages%2Cask%2C%3Fcan_submit%2C%3Ftweet%2Cmention_key%2C%3Ftimezone_offset%2C%3Fanalytics_url%2C%3Fis_premium_partner%2C%3Fis_blogless_advertiser%2C%3Fcan_onboard_to_paywall%2C%3Fis_tumblrpay_onboarded%2C%3Fis_paywall_on%2C%3Flinked_accounts");
                    try {
                        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                        if (entries.containsKey("errors")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        logger.info("Cookie login successful");
                        return;
                    } catch (final Throwable e) {
                        logger.exception("Cookie login with stored token failed", e);
                    }
                    /* Delete cookies / Headers to perform a full login (= try to obtain new token/apikey) */
                    this.br.clearAll();
                }
            }
            logger.info("Performing full cookie login");
            br.setCookies(userCookies);
            br.getPage("https://www." + this.getHost() + "/dashboard");
            apikey = PluginJSonUtils.getJson(br, "API_TOKEN");
            if (StringUtils.isEmpty(apikey)) {
                if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                }
            }
            br.getHeaders().put("Authorization", "Bearer " + apikey);
            br.getPage(API_BASE + "/user/info?fields%5Bblogs%5D=avatar%2Cname%2Ctitle%2Curl%2Ccan_message%2Cdescription%2Cis_adult%2Cuuid%2Cis_private_channel%2Cposts%2Cis_group_channel%2C%3Fprimary%2C%3Fadmin%2C%3Fdrafts%2C%3Ffollowers%2C%3Fqueue%2C%3Fhas_flagged_posts%2Cmessages%2Cask%2C%3Fcan_submit%2C%3Ftweet%2Cmention_key%2C%3Ftimezone_offset%2C%3Fanalytics_url%2C%3Fis_premium_partner%2C%3Fis_blogless_advertiser%2C%3Fcan_onboard_to_paywall%2C%3Fis_tumblrpay_onboarded%2C%3Fis_paywall_on%2C%3Flinked_accounts");
            try {
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                if (entries.containsKey("errors")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } catch (final Throwable e) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, null, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
            }
            account.setProperty(PROPERTY_APIKEY, apikey);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Object> user = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/user");
        /* User could enter any name as username during cookie login -> Fixed this -> Make sure that name is unique */
        account.setUser(user.get("name").toString());
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* Account is never required to download tumblr.com directurls! */
        handleFree(link);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return TumblrComConfig.class;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}