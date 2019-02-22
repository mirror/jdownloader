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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multiup.org" }, urls = { "" })
public class MultiupOrg extends PluginForHost {
    private static final String          API_BASE            = "http://multiup.org/api";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("multiup.org");
    /** TODO: Set correct limits */
    private static final int             defaultMAXDOWNLOADS = 1;
    private static final int             defaultMAXCHUNKS    = -2;
    private static final boolean         defaultRESUME       = true;

    @SuppressWarnings("deprecation")
    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://multiup.org/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://multiup.org/en/terms-and-conditions";
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
        }
        return true;
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
            /** TODO: Find better way to login */
            fetchAccountInfo(account);
            final UrlQuery dlQuery = new UrlQuery();
            dlQuery.add("link", link.getPluginPatternMatcher());
            final String passCode = link.getDownloadPassword();
            if (passCode != null) {
                dlQuery.add("password", passCode);
            }
            /** TODO: WTF this also works without login */
            br.postPage(API_BASE + "/generate-debrid-link", dlQuery);
            dllink = PluginJSonUtils.getJsonValue(br, "debrid_link");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 10, 5 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
        final long direct_url_end_time = downloadLink.getLongProperty("direct_url_end_time", 0) * 1000;
        String dllink = downloadLink.getStringProperty(property);
        if (direct_url_end_time > 0 && direct_url_end_time < System.currentTimeMillis()) {
            logger.info("Directlink expired");
            downloadLink.setProperty(property, Property.NULL);
            dllink = null;
        } else if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final UrlQuery loginQuery = new UrlQuery();
        loginQuery.add("username", account.getUser());
        loginQuery.add("password", account.getPass());
        br.postPage(API_BASE + "/login", loginQuery);
        final String error = PluginJSonUtils.getJson(br, "error");
        if (!"success".equalsIgnoreCase(error)) {
            /* E.g. {"error":"bad username OR bad password"} */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        /** TODO: 2019-02-22: Waiting for an API can to get account status (free/premium) - accept all accounts as premium for now */
        final boolean is_premium = true;
        if (!is_premium) {
            account.setType(AccountType.FREE);
            /* No downloads possible via free account */
            ai.setTrafficLeft(0);
            account.setMaxSimultanDownloads(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            final String validuntil = PluginJSonUtils.getJsonValue(br, "premium_end_time");
            if (validuntil != null && validuntil.matches("\\d+")) {
                ai.setStatus("Premium time");
                ai.setValidUntil(Long.parseLong(validuntil) * 1000l, this.br);
            }
            ai.setUnlimitedTraffic();
        }
        this.getAPISafe(API_BASE + "/get-list-hosts", account, null);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("hosts");
        final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        while (iterator.hasNext()) {
            final Entry<String, Object> entry = iterator.next();
            final String host = entry.getKey();
            supportedhostslist.add(host);
        }
        account.setConcurrentUsePossible(true);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.br.getPage(accesslink);
        handleKnownErrors(this.br, account, link);
    }

    private void handleKnownErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final int errorcode = getErrorcode(br);
        // final String errormsg = getErrormessage(this.br);
        switch (errorcode) {
        case 0:
            break;
        case 401:
            /* Login failed */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        case 400:
            /* Bad request, this should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        case 404:
            mhm.handleErrorGeneric(account, link, "hoster_offline_or_unsupported", 10, 5 * 60 * 1000l);
        case 503:
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 - Service unavailable");
        default:
            /* Unknown issue */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private int getErrorcode(final Browser br) {
        String status = PluginJSonUtils.getJson(br, "status");
        if (status != null) {
            /* Return errorcode */
            return Integer.parseInt(status);
        } else {
            /* Everything ok */
            return 0;
        }
    }

    private String getErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "details");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}