//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cboxera.com" }, urls = { "" })
public class CboxeraCom extends PluginForHost {
    private static final String          API_BASE            = "https://api.cboxera.com";
    /* 2020-03-24: Static implementation as key is nowhere to be found via API request. */
    private static final String          RECAPTCHAv2_SITEKEY = "6Ldq4FwUAAAAAJ81U4lQEvQXps384V7eCWJWxdjf";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("cboxera.com");
    private static final int             defaultMAXDOWNLOADS = -1;
    private static final int             defaultMAXCHUNKS    = 0;
    private static final boolean         defaultRESUME       = true;
    private static final String          PROPERTY_logintoken = "token";
    private static final String          PROPERTY_directlink = "directlink";

    @SuppressWarnings("deprecation")
    public CboxeraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.cboxera.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.cboxera.com/terms";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 401 });
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never get called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + PROPERTY_directlink);
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginAPI(account, false);
            br.postPageRaw(API_BASE + "/private/generatelink", String.format("{\"link\":\"%s\"}", link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            dllink = PluginJSonUtils.getJsonValue(br, "dlink");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        link.setProperty(this.getHost() + PROPERTY_directlink, dl.getConnection().getURL().toString());
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 20, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/private/user/info")) {
            br.getPage(API_BASE + "/private/user/info");
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("subscription");
        boolean is_premium = ((Boolean) entries.get("is_vip"));
        final String trafficleft = (String) entries.get("bw_limit");
        if (!is_premium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            final long premium_days_left = JavaScriptEngineFactory.toLong(entries.get("days"), -1);
            final long validuntil = System.currentTimeMillis() + premium_days_left * 24 * 60 * 60 * 1000l;
            ai.setValidUntil(validuntil, this.br);
        }
        ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        br.getPage(API_BASE + "/public/host-status");
        final String account_type_key;
        if (account.getType() == AccountType.FREE) {
            account_type_key = "free";
        } else {
            account_type_key = "vip";
        }
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        for (final Object hostO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) hostO;
            final String domain = (String) entries.get("name");
            if (StringUtils.isEmpty(domain)) {
                /* This should never happen */
                continue;
            }
            /* Get host information for on current account type. */
            entries = (LinkedHashMap<String, Object>) entries.get(account_type_key);
            final String supported = (String) entries.get("supported");
            if ("no".equalsIgnoreCase(supported)) {
                logger.info("Skipping host because: unsupported: " + domain);
                continue;
            }
            try {
                /* Given in this format: "5 GB" */
                final String size_limitStr = (String) entries.get("size_limit");
                final String bandwidth_limitStr = (String) entries.get("bandwidth_limit");
                final long size_limit = SizeFormatter.getSize(size_limitStr);
                final long bandwidth_limit = SizeFormatter.getSize(bandwidth_limitStr);
                if (size_limit <= 0 || bandwidth_limit <= 0) {
                    logger.info("Skipping host because: no traffic available: " + domain);
                    continue;
                }
            } catch (final Throwable e) {
                /* Ignore this */
            }
            supportedhostslist.add(domain);
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account, final boolean forceAuthCheck) throws IOException, PluginException, InterruptedException {
        String token = account.getStringProperty(PROPERTY_logintoken);
        if (token != null) {
            logger.info("Attempting token login");
            br.getHeaders().put("Authorization", "Bearer " + token);
            /*
             * 2020-03-24: No idea how long their token is supposed to last but I guess it should be permanent because a full login requires
             * a captcha!
             */
            if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !forceAuthCheck) {
                /* We trust these cookies --> Do not check them */
                logger.info("Trust login cookies as they're not yet that old");
                return;
            }
            br.getPage(API_BASE + "/private/user/info");
            if (br.getHttpConnection().getResponseCode() == 200) {
                logger.info("Token login successful");
                /* We don't really need the cookies but the timestamp ;) */
                account.saveCookies(br.getCookies(br.getHost()), "");
                return;
            } else {
                /* Most likely 401 unauthorized */
                logger.info("Token login failed");
            }
        }
        /* Drop previous headers & cookies */
        br = this.prepBR(new Browser());
        final DownloadLink dlinkbefore = this.getDownloadLink();
        String recaptchaV2Response = null;
        try {
            final DownloadLink dl_dummy;
            if (dlinkbefore != null) {
                dl_dummy = dlinkbefore;
            } else {
                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                this.setDownloadLink(dl_dummy);
            }
            /* 2020-03-24: This is their reCaptchaV2 domain which means this needs to be accessed to be able to solve the captcha! */
            // br.getPage("https://www." + this.getHost() + "/login/");
            /* Set request so we do not actually have to call the website in this plugin which is using the API for 100% of all requests! */
            br.setRequest(br.createGetRequest("https://www." + this.getHost() + "/login/"));
            recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, RECAPTCHAv2_SITEKEY).getToken();
        } finally {
            this.setDownloadLink(dlinkbefore);
        }
        final String postData = String.format("{\"email\": \"%s\",\"password\": \"%s\",\"token\":\"%s\"}", account.getUser(), account.getPass(), recaptchaV2Response);
        br.postPageRaw(API_BASE + "/public/login", postData);
        token = PluginJSonUtils.getJson(br, "token");
        if (StringUtils.isEmpty(token)) {
            handleErrors(br, account, null);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        account.setProperty(PROPERTY_logintoken, token);
        /* We don't really need the cookies but the timestamp ;) */
        account.saveCookies(br.getCookies(br.getHost()), "");
        br.getHeaders().put("Authorization", "Bearer " + token);
    }

    private void handleErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String errormsg = getErrormessage(this.br);
        if (!StringUtils.isEmpty(errormsg)) {
            if (errormsg.equalsIgnoreCase("No Authorization was found") || errormsg.equalsIgnoreCase("Invalid Password")) {
                /* Usually goes along with http response 401 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errormsg.equalsIgnoreCase("Invalid token")) {
                /* Existing session expired. */
                /* Usually goes along with http response 401. Temp. disable account so token can be refreshed on next account check! */
                throw new AccountUnavailableException(errormsg, 1 * 60 * 1000l);
            }
        }
    }

    private String getErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "msg");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}