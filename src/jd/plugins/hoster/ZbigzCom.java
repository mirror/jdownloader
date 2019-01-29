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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zbigz.com" }, urls = { "https?://(?:www\\.)?zbigz\\.com/file/[a-z0-9]+/\\d+|https?://api\\.zbigz\\.com/v1/storage/get/[a-f0-9]+" })
public class ZbigzCom extends antiDDoSForHost {
    public ZbigzCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://zbigz.com/page-premium-overview");
    }

    @Override
    public String getAGBLink() {
        return "http://zbigz.com/page-therms-of-use";
    }

    private String              DLLINK   = null;
    private static final String NOCHUNKS = "NOCHUNKS";

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, false);
            final boolean enable_antiddos_workaround = true;
            if (enable_antiddos_workaround) {
                br.getPage(downloadLink.getPluginPatternMatcher());
            } else {
                super.getPage(downloadLink.getPluginPatternMatcher());
            }
            if (br.containsHTML("Page not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).trim());
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            return AvailableStatus.TRUE;
        } else {
            downloadLink.getLinkStatus().setStatusText("Status can only be checked with account enabled");
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                getPage("https://zbigz.com/login");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "application/json, application/xml, text/plain, text/html, *.*");
                postPage("https://api.zbigz.com/v1/account/info", "undefined=undefined");
                /* Important header!! */
                br.getHeaders().put("Origin", "https://zbigz.com");
                final PostFormDataRequest authReq = br.createPostFormDataRequest("https://api.zbigz.com/v1/account/auth/token");
                authReq.addFormData(new FormData("undefined", "undefined"));
                super.sendRequest(authReq);
                final String auth_token_name = PluginJSonUtils.getJson(br, "auth_token_name");
                final String auth_token_value = PluginJSonUtils.getJson(br, "auth_token_value");
                if (StringUtils.isEmpty(auth_token_name) || StringUtils.isEmpty(auth_token_value)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final PostFormDataRequest loginReq = br.createPostFormDataRequest("/v1/account/sign-in");
                loginReq.addFormData(new FormData("login", account.getUser()));
                loginReq.addFormData(new FormData("email", account.getUser()));
                loginReq.addFormData(new FormData("password", account.getPass()));
                loginReq.addFormData(new FormData("csrf_name", auth_token_name));
                loginReq.addFormData(new FormData("csrf_value", auth_token_value));
                loginReq.addFormData(new FormData("recaptcha", ""));
                super.sendRequest(loginReq);
                final String sessiontoken = PluginJSonUtils.getJson(br, "session");
                if (StringUtils.isEmpty(sessiontoken)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            throw e;
        }
        /* Browser: https://zbigz.com/account */
        final PostFormDataRequest accountInfoReq = br.createPostFormDataRequest("https://api.zbigz.com/v1/account/info");
        accountInfoReq.addFormData(new FormData("undefined", "undefined"));
        super.sendRequest(accountInfoReq);
        final String premium_valid_date = PluginJSonUtils.getJson(br, "premium_valid_date");
        final String premium_days = PluginJSonUtils.getJson(br, "premium_days");
        if (!StringUtils.isEmpty(premium_valid_date) || "true".equalsIgnoreCase(premium_days)) {
            account.setType(AccountType.PREMIUM);
            ai.setUnlimitedTraffic();
            /* TODO: Add proper code to display expire-date */
            // ai.setValidUntil(TimeFormatter.getMilliSeconds(premium_valid_date, "dd MMM yyyy", Locale.ENGLISH));
            // final String traffic = info.getMatch(1);
            // ai.setTrafficLeft(SizeFormatter.getSize(traffic));
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        int chunks = -5;
        if (link.getBooleanProperty(ZbigzCom.NOCHUNKS, false)) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}