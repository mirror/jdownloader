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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "neodebrid.com" }, urls = { "" })
public class NeodebridCom extends PluginForHost {
    /** Tags: cocoleech.com */
    private static final String          API_BASE            = "https://neodebrid.com/api/v2";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("neodebrid.com");
    private static final int             defaultMAXDOWNLOADS = -1;
    /** 2019-07-05: In my tests, neither chunkload nor resume were possible (premium account!) */
    private static final int             defaultMAXCHUNKS    = 1;
    private static final boolean         defaultRESUME       = false;

    @SuppressWarnings("deprecation")
    public NeodebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://neodebrid.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://neodebrid.com/tos";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
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
        } else {
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            getAPISafe(API_BASE + "/download?token=" + this.getApiToken(account) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)), account, link);
            dllink = PluginJSonUtils.getJsonValue(br, "download");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            /* 402 - Payment required */
            if (dl.getConnection().getResponseCode() == 402) {
                /* 2019-05-03: E.g. free account[or expired premium], only 1 download per day (?) possible */
                account.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "No traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            handleKnownErrors(this.br, account, link);
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
        this.br = newBrowser();
        mhm.runCheck(account, link);
        handleDL(account, link);
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
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        if (br.getURL() == null || !br.getURL().contains("/info")) {
            br.getPage(API_BASE + "/info?token=" + this.getApiToken(account));
        }
        final String expireTimestampStr = PluginJSonUtils.getJson(br, "timestamp");
        /* 2019-07-05: Will usually return 'Unlimited' for premium accounts and 'XX GB' for free accounts */
        final String traffic_leftStr = PluginJSonUtils.getJson(br, "traffic_left");
        long validuntil = 0;
        if (expireTimestampStr != null && expireTimestampStr.matches("\\d+")) {
            validuntil = Long.parseLong(expireTimestampStr) * 1000l;
        }
        if (validuntil < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            // account.setMaxSimultanDownloads(1);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            ai.setValidUntil(validuntil, this.br);
        }
        if ("Unlimited".equalsIgnoreCase(traffic_leftStr)) {
            ai.setUnlimitedTraffic();
        } else {
            ai.setTrafficLeft(SizeFormatter.getSize(traffic_leftStr));
        }
        /* Continue via API */
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
        handleKnownErrors(this.br, account, link);
    }

    private void handleKnownErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String status = PluginJSonUtils.getJson(br, "status");
        final String errorStr = PluginJSonUtils.getJson(br, "reason");
        if (!"success".equalsIgnoreCase(status)) {
            if (errorStr != null) {
                if (errorStr.equalsIgnoreCase("Filehost not supported.")) {
                    mhm.putError(account, link, 5 * 60 * 1000l, errorStr);
                } else if (errorStr.equalsIgnoreCase("Token not found.")) {
                    logger.info("api_token has expired");
                    account.setProperty("api_token", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            mhm.handleErrorGeneric(account, link, "generic_api_error", 50, 5 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}