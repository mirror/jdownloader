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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "neodebrid.com" }, urls = { "https?://(?:www\\.)?neodebrid\\.com/dl/([A-Z0-9]+)" })
public class NeodebridCom extends PluginForHost {
    /** Tags: cocoleech.com */
    private static final String          API_BASE                   = "https://neodebrid.com/api/v2";
    private static final boolean         api_supports_free_accounts = false;
    private static MultiHosterManagement mhm                        = new MultiHosterManagement("neodebrid.com");
    private static final int             defaultMAXDOWNLOADS        = -1;
    /** 2019-08-26: In my tests, neither chunkload nor resume were possible (premium account!) */
    private static final boolean         account_premium_resume     = false;
    private static final int             account_premium_maxchunks  = 1;
    /** 2019-08-26: TODO: Check/update these Free Account limits */
    private static final boolean         account_FREE_resume        = true;
    private static final int             account_FREE_maxchunks     = 0;

    @SuppressWarnings("deprecation")
    public NeodebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://neodebrid.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://neodebrid.com/tos";
    }

    private Browser newBrowserAPI() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private Browser newBrowserWebsite() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        newBrowserWebsite();
        br.getPage(link.getPluginPatternMatcher());
        if (this.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<b>Filename\\s*:\\s*</b>([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = getFID(link);
        }
        String filesize = br.getRegex("<b>Filesize\\s*:\\s*</b>([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDLSelfhosted(null, link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDLSelfhosted(account, link);
    }

    private void handleDLSelfhosted(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directurl_selfhosted");
        if (dllink == null) {
            dllink = getDllinkWebsiteCaptcha(account, link);
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, account_FREE_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                /* 2019-08-26: This happens quite often! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
            }
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadlink did not lead to downloadable content");
        }
        this.dl.startDownload();
    }

    private void handleDLMultihoster(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        final boolean resume;
        final int maxchunks;
        if (account.getType() == AccountType.FREE) {
            resume = account_premium_resume;
            maxchunks = account_premium_maxchunks;
        } else {
            resume = account_FREE_resume;
            maxchunks = account_FREE_maxchunks;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            /* 402 - Payment required */
            if (dl.getConnection().getResponseCode() == 402) {
                /* 2019-05-03: E.g. free account[or expired premium], only 1 download per day (?) possible */
                account.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "No traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            handleErrorsAPI(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowserAPI();
        mhm.runCheck(account, link);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            if (account.getType() == AccountType.FREE && !api_supports_free_accounts) {
                /* first try to get saved directurl */
                dllink = this.checkDirectLink(link, this.getHost() + "directurl_selfhosted");
                if (dllink == null) {
                    dllink = generateDllinkWebsiteFreeMode(account, link);
                }
            } else {
                dllink = getDllinkAPI(account, link);
            }
        }
        handleDLMultihoster(account, link, dllink);
    }

    /** Generated downloadlinks via API. */
    private String getDllinkAPI(final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        br.setFollowRedirects(true);
        this.loginAPI(account);
        getAPISafe(API_BASE + "/download?token=" + this.getApiToken(account) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)), account, link);
        return PluginJSonUtils.getJsonValue(br, "download");
    }

    /** Generates downloadlinks via website. */
    private String generateDllinkWebsiteFreeMode(final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        newBrowserWebsite();
        /* 2019-08-24: Not required */
        // this.loginAPI(account);
        /* Try to re-use previously generated URLs so we're not wasting traffic! */
        String internal_url = link.getStringProperty(this.getHost() + "selfhosted_free_download_url", null);
        boolean generate_new_internal_url = internal_url == null;
        if (internal_url != null) {
            br.getPage(internal_url);
            generate_new_internal_url = this.isOffline(this.br);
        }
        if (generate_new_internal_url) {
            logger.info("Generating new directurl");
            br.getPage("https://" + this.getHost() + "/process?link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            handleErrorsWebsite(br, account, link);
            internal_url = br.getRegex("(https?://[^/]+/dl/[A-Z0-9]+)").getMatch(0);
            if (StringUtils.isEmpty(internal_url)) {
                logger.warning("Failed to find generated 'internal_url'");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find generated 'internal_url' or unknown error occured");
            } else if (isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setProperty(this.getHost() + "selfhosted_free_download_url", internal_url);
            this.br.getPage(internal_url);
        } else {
            logger.info("Re-using previously generated- and saved directurl");
        }
        return getDllinkWebsiteCaptcha(account, link);
    }

    private String getDllinkWebsiteCaptcha(final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        /* 2019-08-24: Login is not required */
        // this.loginAPI(account);
        Form captchaform = br.getFormbyActionRegex(".*/redirect.*");
        if (captchaform == null) {
            /* Fallback */
            captchaform = br.getForm(0);
        }
        if (captchaform == null) {
            logger.warning("captchaform is null");
        }
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
        captchaform.put("g-recaptcha-response", recaptchaV2Response);
        br.setFollowRedirects(false);
        br.submitForm(captchaform);
        final String dllink = br.getRedirectLocation();
        if (!StringUtils.isEmpty(dllink)) {
            /* Store directurl */
            link.setProperty(this.getHost() + "directurl_selfhosted", dllink);
        }
        return dllink;
    }

    private boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Link not found, please re-generate your link");
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
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
        this.br = newBrowserAPI();
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        if (br.getURL() == null || !br.getURL().contains("/info?token")) {
            br.getPage(API_BASE + "/info?token=" + this.getApiToken(account));
        }
        final String expireTimestampStr = PluginJSonUtils.getJson(br, "timestamp");
        /* 2019-07-05: Will usually return 'Unlimited' for premium accounts and 'XX GB' for free accounts */
        String traffic_leftStr = PluginJSonUtils.getJson(br, "traffic_left");
        long validuntil = 0;
        int filesPerDayLeft = 0;
        if (expireTimestampStr != null && expireTimestampStr.matches("\\d+")) {
            validuntil = Long.parseLong(expireTimestampStr) * 1000l;
        }
        if (validuntil < System.currentTimeMillis()) {
            /* 2019-08-26: API will always return static value '1 GB' trafficleft for free accounts which is wrong! */
            /*
             * 2019-08-26: FREE (account) Limits are based on IP which means it makes no difference whether we download without account or
             * with a free account!
             */
            final boolean enable_free_account_traffic_workaround = true;
            String accountStatus = "Free account";
            if (enable_free_account_traffic_workaround) {
                /* Try to find correct 'trafficleft' value via website. */
                this.br.getPage("https://" + this.getHost() + "/home");
                final Regex filesLeftRegex = br.getRegex(">Files per day :</b>\\s*(\\d+)\\s*/\\s*(\\d+)\\s*<br>");
                final Regex trafficRegex = br.getRegex(">Traffic :</b>\\s*([^<>\"]+)\\s*/\\s*([^<>\"]+)<");
                final String trafficLeftStrTmp = trafficRegex.getMatch(0);
                final String trafficMaxStrTmp = trafficRegex.getMatch(1);
                final String filesPerDayUsedStr = filesLeftRegex.getMatch(0);
                final String filesPerDayMaxStr = filesLeftRegex.getMatch(1);
                if (filesPerDayMaxStr != null && filesPerDayUsedStr != null) {
                    filesPerDayLeft = Integer.parseInt(filesPerDayMaxStr) - Integer.parseInt(filesPerDayUsedStr);
                } else {
                    filesPerDayLeft = 0;
                }
                if (trafficLeftStrTmp != null && trafficMaxStrTmp != null) {
                    traffic_leftStr = trafficLeftStrTmp;
                    ai.setTrafficMax(trafficMaxStrTmp);
                }
                accountStatus += " [" + filesPerDayLeft + " files per day left today]";
            }
            account.setType(AccountType.FREE);
            ai.setStatus(accountStatus);
            // account.setMaxSimultanDownloads(1);
        } else {
            /* Premium */
            /* Premium has unlimited files per day */
            filesPerDayLeft = -1;
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            ai.setValidUntil(validuntil, this.br);
        }
        if ("Unlimited".equalsIgnoreCase(traffic_leftStr)) {
            ai.setUnlimitedTraffic();
        } else {
            if (filesPerDayLeft == 0) {
                logger.info("Setting ZERO trafficleft because filesPerDayLeft is 0");
                ai.setTrafficLeft(0);
            } else {
                ai.setTrafficLeft(SizeFormatter.getSize(traffic_leftStr));
            }
        }
        /*
         * Get list of supported hosts. Sadly they do not display free/premium hosts but always return all supported hosts. On their website
         * there are two lists: https://neodebrid.com/status
         */
        br.getPage(API_BASE + "/status");
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> hosters = (ArrayList<Object>) entries.get("result");
        for (final Object hostero : hosters) {
            entries = (LinkedHashMap<String, Object>) hostero;
            String host = (String) entries.get("host");
            final String status = (String) entries.get("status");
            if (host != null && "online".equalsIgnoreCase(status)) {
                supportedhostslist.add(host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account) throws IOException, PluginException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                String api_token = getApiToken(account);
                String status = null;
                if (api_token != null) {
                    br.getPage(API_BASE + "/info?token=" + this.getApiToken(account));
                    status = PluginJSonUtils.getJson(br, "status");
                    if ("success".equalsIgnoreCase(status)) {
                        logger.info("Stored token was valid");
                        return;
                    } else {
                        logger.info("Stored token was INVALID, performing full login");
                    }
                }
                br.getPage(API_BASE + "/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                /** 2019-07-05: No idea how long this token is valid! */
                api_token = PluginJSonUtils.getJson(br, "api_token");
                status = PluginJSonUtils.getJson(br, "status");
                if (!"success".equalsIgnoreCase(status) || StringUtils.isEmpty(api_token)) {
                    /* E.g. {"error":"bad username OR bad password"} */
                    final String fail_reason = PluginJSonUtils.getJson(br, "reason");
                    if (!StringUtils.isEmpty(fail_reason)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, fail_reason, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("api_token", api_token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty("api_token");
                }
                throw e;
            }
        }
    }

    private String getApiToken(final Account account) {
        return account.getStringProperty("api_token", null);
    }

    /** getPage with errorhandling */
    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.br.getPage(accesslink);
        handleErrorsAPI(this.br, account, link);
    }

    private void handleErrorsAPI(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String status = PluginJSonUtils.getJson(br, "status");
        final String errorStr = PluginJSonUtils.getJson(br, "reason");
        if (!"success".equalsIgnoreCase(status)) {
            if (errorStr != null) {
                if (errorStr.equalsIgnoreCase("Filehost not supported.")) {
                    mhm.putError(account, link, 5 * 60 * 1000l, errorStr);
                } else if (errorStr.equalsIgnoreCase("Token not found.")) {
                    logger.info("api_token has expired --> Deleting it - and trying again");
                    account.removeProperty("api_token");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else if (errorStr.equalsIgnoreCase("User not premium.")) {
                    logger.info("User has a free account and plugin tried to download from premium-only host");
                    account.removeProperty("api_token");
                    mhm.putError(account, link, 5 * 60 * 1000l, "This host is not supported via free account");
                }
            }
            logger.info("Unknown API error happened");
            mhm.handleErrorGeneric(account, link, "generic_api_error", 50, 5 * 60 * 1000l);
        }
    }

    private void handleErrorsWebsite(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        /** 2019-08-24: TODO */
        if (br.containsHTML("Error: Maximum filesize for free users")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Max freeuser filesize reached");
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}