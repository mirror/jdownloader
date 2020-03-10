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
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
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
        this.enablePremium("https://zbigz.com/page-premium-overview");
    }

    @Override
    public String getAGBLink() {
        return "https://zbigz.com/page-therms-of-use";
    }

    private String dllink = null;

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa == null) {
            link.getLinkStatus().setStatusText("Status can only be checked with account enabled");
            return AvailableStatus.UNCHECKABLE;
        }
        login(aa, false);
        final boolean enable_antiddos_workaround = true;
        if (enable_antiddos_workaround) {
            br.getPage(link.getPluginPatternMatcher());
        } else {
            super.getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Page not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = br.getRedirectLocation();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).trim());
                link.setDownloadSize(con.getLongContentLength());
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
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private static final String WEBSITE_API_BASE = "https://api.zbigz.com/v1";

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                /* 2019-11-04: Always try to re-use cookies to avoid login captchas! */
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    final PostFormDataRequest accountInfoReq = br.createPostFormDataRequest(WEBSITE_API_BASE + "/account/info");
                    accountInfoReq.addFormData(new FormData("undefined", "undefined"));
                    // br.clearCookies("zbigz.com");
                    super.sendRequest(accountInfoReq);
                    final String email = PluginJSonUtils.getJson(br, "email");
                    if (!StringUtils.isEmpty(email)) {
                        return;
                    } else {
                        /* Full login required */
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                getPage("https://zbigz.com/login");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Accept", "application/json, application/xml, text/plain, text/html, *.*");
                postPage(WEBSITE_API_BASE + "/account/info", "undefined=undefined");
                /* Important header!! */
                br.getHeaders().put("Origin", "https://zbigz.com");
                final PostFormDataRequest authReq = br.createPostFormDataRequest(WEBSITE_API_BASE + "/account/auth/token");
                authReq.addFormData(new FormData("undefined", "undefined"));
                super.sendRequest(authReq);
                final String auth_token_name = PluginJSonUtils.getJson(br, "auth_token_name");
                final String auth_token_value = PluginJSonUtils.getJson(br, "auth_token_value");
                if (StringUtils.isEmpty(auth_token_name) || StringUtils.isEmpty(auth_token_value)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final PostFormDataRequest loginReq = br.createPostFormDataRequest("/v1/account/sign-in");
                /* 2019-11-04: Seems like login captcha is always required */
                final DownloadLink dlinkbefore = this.getDownloadLink();
                try {
                    final DownloadLink dl_dummy;
                    if (dlinkbefore != null) {
                        dl_dummy = dlinkbefore;
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                        this.setDownloadLink(dl_dummy);
                    }
                    /* 2019-11-04: Hardcoded reCaptchaV2 key */
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lei4loUAAAAACp9km05L8agghrMMNNSYo5Mfmhj").getToken();
                    loginReq.addFormData(new FormData("recaptcha", Encoding.urlEncode(recaptchaV2Response)));
                } finally {
                    this.setDownloadLink(dlinkbefore);
                }
                loginReq.addFormData(new FormData("login", account.getUser()));
                loginReq.addFormData(new FormData("email", account.getUser()));
                loginReq.addFormData(new FormData("password", account.getPass()));
                loginReq.addFormData(new FormData("csrf_name", auth_token_name));
                loginReq.addFormData(new FormData("csrf_value", auth_token_value));
                super.sendRequest(loginReq);
                /* 2019-11-04: This will also be set as cookie with key "session". */
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
        if (br.getURL() == null || !br.getURL().contains("/account/info")) {
            final PostFormDataRequest accountInfoReq = br.createPostFormDataRequest(WEBSITE_API_BASE + "/account/info");
            accountInfoReq.addFormData(new FormData("undefined", "undefined"));
            super.sendRequest(accountInfoReq);
        }
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -5);
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