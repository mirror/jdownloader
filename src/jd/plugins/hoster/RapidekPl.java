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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidek.pl" }, urls = { "" })
public class RapidekPl extends PluginForHost {
    private static final String          API_BASE                     = "https://rapidek.pl/api";
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("rapidek.pl");
    private static final boolean         account_PREMIUM_resume       = true;
    /** 2020-03-21: phg: In my tests, it is OK for the chunkload with the value of 5 */
    private static final int             account_PREMIUM_maxchunks    = 0;
    private static final int             account_PREMIUM_maxdownloads = -1;

    @SuppressWarnings("deprecation")
    public RapidekPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidek.pl/doladuj");
    }

    @Override
    public String getAGBLink() {
        return "https://rapidek.pl/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginAPI(account, false);
            br.postPageRaw(API_BASE + "/file-download/init", String.format("{\"Url\":\"%s\"}", link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            final String internalID = PluginJSonUtils.getJson(br, "DownloadId");
            if (StringUtils.isEmpty(internalID) || !internalID.matches("\"?[a-f0-9\\-]+\"?")) {
                mhm.handleErrorGeneric(account, link, "Bad DownloadId", 20);
            }
            br.postPageRaw(API_BASE + "/file-download/download-info", String.format("{\"downloadId\":\"%s\"}", internalID));
            /* TODO: Add serverside download handling */
            // dllink = API_BASE + "/file?id=" + internalID;
            // if (dllink == null) {
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "dllinknull", 5 * 60 * 1000l);
            // }
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed o find final downloadurl", 20);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_resume, account_PREMIUM_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Unknown download error", 20);
        }
        this.dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || !con.isOK() || con.getLongContentLength() == -1) {
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
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/account/info")) {
            br.getPage(API_BASE + "/account/info");
        }
        final String expireDate = PluginJSonUtils.getJson(br, "AccountExpiry");
        final String trafficLeft = PluginJSonUtils.getJson(br, "AvailableTransferBytes");
        if (expireDate == null && trafficLeft == null) {
            /* Free account --> Cannot download anything */
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        } else {
            /* Premium account */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(account_PREMIUM_maxdownloads);
            if (expireDate != null && expireDate.matches("\\d+")) {
                ai.setValidUntil(Long.parseLong(expireDate));
            }
            /* 2020-06-05: So far, my test account is only traffic based without an expiredate given (psp) */
            if (trafficLeft != null && trafficLeft.matches("\\d+")) {
                ai.setTrafficLeft(trafficLeft);
            } else {
                ai.setUnlimitedTraffic();
            }
        }
        /*
         * 2020-06-05: API only returns the following this I've enabled this workaround for now:
         * {"Services":["premiumize.com","rapidu.net"]}
         */
        final boolean useForumWorkaround = true;
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        if (useForumWorkaround) {
            /* 2020-06-05: Possible workaround */
            br.getPage("https://rapidekforum.pl/all.php");
            final String[] hosts = br.getRegex("([^;]+);").getColumn(0);
            for (final String host : hosts) {
                /* Do not add domain of this multihost plugin */
                if (host.equalsIgnoreCase(this.getHost())) {
                    continue;
                }
                supportedHosts.add(host);
            }
        } else {
            br.getPage(API_BASE + "/services/list");
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final ArrayList<Object> hostsO = (ArrayList<Object>) entries.get("Services");
            for (final Object hostO : hostsO) {
                supportedHosts.add((String) hostO);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void loginAPI(final Account account, final boolean force) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                String token = account.getStringProperty("token");
                if (cookies != null && token != null) {
                    logger.info("Trying to login via token");
                    br.setCookies(API_BASE, cookies);
                    br.getHeaders().put("Authorization", "Bearer " + token);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l && !force) {
                        logger.info("Not checking token at all as it's still fresh");
                        return;
                    }
                    br.getPage(API_BASE + "/account/info");
                    if (br.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Token login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Token login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("accept", "application/json");
                br.postPageRaw(API_BASE + "/account/sign-in", String.format("{\"Username\":\"%s\",\"Password\":\"%s\"}", account.getUser(), account.getPass()));
                token = PluginJSonUtils.getJson(br, "Token");
                if (StringUtils.isEmpty(token)) {
                    /* 2020-06-05: E.g. "nullified" response: {"Result":"Unknown","AvailableTransferBytes":0} */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                account.setProperty("token", token);
                br.getHeaders().put("Authorization", "Bearer " + token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() throws PluginException {
        return br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}