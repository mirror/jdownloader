//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alldebrid.com" }, urls = { "https?://(?:[a-z]\\d+\\.alldebrid\\.com|[a-z0-9]+\\.alld\\.io)/dl/[a-z0-9]+/.+" })
public class AllDebridCom extends antiDDoSForHost {
    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000l);
        this.enablePremium("http://www.alldebrid.com/offer/");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private static MultiHosterManagement mhm              = new MultiHosterManagement("alldebrid.com");
    private static String                api              = "https://api.alldebrid.com";
    private static final String          NOCHUNKS         = "NOCHUNKS";
    private Account                      currAcc          = null;
    private DownloadLink                 currDownloadLink = null;
    private String                       token            = null;
    private static Object                accLock          = new Object();
    // this is used by provider which calculates unique token to agent/client.
    private static final String          agent            = "agent=JDownloader";

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        setConstants(account, null);
        synchronized (accLock) {
            if (token != null) {
                getPage(api + "/user/login?" + agent + "&token=" + token);
            }
            {
                final int error = parseError();
                if (token == null || error == 1 || error == 5) {
                    getPage(api + "/user/login?" + agent + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                }
            }
            handleErrors();
            {
                final Boolean isPremium = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(br, "isPremium"));
                if (!isPremium) {
                    throw new AccountInvalidException("Free accounts are not supported!");
                }
                token = PluginJSonUtils.getJson(br, "token");
                account.setProperty("token", token);
                final String premiumUntil = PluginJSonUtils.getJson(br, "premiumUntil");
                ac.setValidUntil(Long.parseLong(premiumUntil) * 1000l);
            }
        }
        {
            // /hosts/domains will return offline hosts.
            getPage(api + "/hosts");
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> hosts = (ArrayList<Object>) entries.get("hosts");
            if (hosts != null) {
                final ArrayList<String> supportedHosts = new ArrayList<String>();
                for (final Object host : hosts) {
                    final LinkedHashMap<String, Object> entry = (LinkedHashMap<String, Object>) host;
                    if (Boolean.FALSE.equals(entry.get("status"))) {
                        continue;
                    }
                    final String hostPrimary = (String) entry.get("domain");
                    // seen null values within their map..
                    if (hostPrimary == null) {
                        continue;
                    }
                    supportedHosts.add(hostPrimary);
                    final ArrayList<String> hostSecondary = (ArrayList<String>) entry.get("altDomains");
                    if (hostSecondary != null) {
                        for (final String sh : hostSecondary) {
                            // prevention is better than cure?
                            if (sh != null) {
                                supportedHosts.add(sh);
                            }
                        }
                    }
                }
                ac.setMultiHostSupport(this, supportedHosts);
            }
        }
        return ac;
    }

    private Integer parseError() {
        final String error = PluginJSonUtils.getJsonValue(br, "errorCode");
        if (error == null || !error.matches("\\d+")) {
            return -1;
        }
        return Integer.parseInt(error);
    }

    private void handleErrors() throws PluginException {
        // 1 Invalid token.
        // 2 Invalid user or password.
        // 3 Geolock protection active, please login on the website.
        // 4 User is banned.
        // 5 Please provide both username and password for authentification, or a valid token.
        // 100 Too many login attempts, please wait.
        // 101 Too many login attempts, blocked for 15 min.
        // 102 Too many login attempts, blocked for 6 hours.
        switch (parseError()) {
        // everything is aok
        case -1:
            return;
            // login related
        case 2:
            throw new AccountInvalidException("Invalid User/Password!");
        case 3:
            throw new AccountInvalidException("Geo Blocked!");
        case 4:
            throw new AccountInvalidException("Banned Account!");
        case 100:
            throw new AccountUnavailableException("Too many login attempts", 2 * 60 * 1000l);
        case 101:
            throw new AccountUnavailableException("Too many login attempts", 15 * 60 * 1000l);
        case 102: {
            throw new AccountUnavailableException("Too many login attempts", 6 * 60 * 60 * 1000l);
        }
        case 1:
        case 5: {
            this.currAcc.removeProperty("token");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        // download related
        // 30 This link is not valid or not supported.
        // 31 This link is not available on the file hoster website.
        // 32 Host unsupported or under maintenance.
        // 39 Generic unlocking error.
        case 30: {
            // tested by placing url in thats on a provider not in the supported host map. returns error 30. -raz
            mhm.putError(null, this.currDownloadLink, 30 * 60 * 1000l, "Host provider not supported");
        }
        case 32:
            mhm.putError(null, this.currDownloadLink, 30 * 60 * 1000l, "Down for maintance");
        case 31:
        case 39:
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.alldebrid.com/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(null, link, link.getDownloadURL());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(account, link, link.getDownloadURL());
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        mhm.runCheck(this.currAcc, this.currDownloadLink);
        showMessage(link, "Phase 1/2: Generating link");
        synchronized (accLock) {
            final boolean cache = loadToken(account, link);
            logger.info("Cached 'token' = " + String.valueOf(cache));
            final String unlock = api + "/link/unlock?" + agent + "&link=" + Encoding.urlEncode(link.getPluginPatternMatcher());
            getPage(unlock + "&token=" + token);
            if (11 == parseError()) {
                loadToken(account, link);
                getPage(unlock + "&token=" + token);
            }
            handleErrors();
        }
        final String genlink = PluginJSonUtils.getJsonValue(br, "link");
        if (genlink == null || !genlink.matches("https?://.+")) {
            // we need a final error handling for situations when
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDL(account, link, genlink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account acc, final DownloadLink link, final String genlink) throws Exception {
        if (genlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Task 2: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(AllDebridCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (br != null && PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(br, "paws"))) {
            final String host = Browser.getHost(link.getDownloadURL());
            final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
                @Override
                public HashInfo getHashInfo() {
                    return null;
                }

                @Override
                public long getVerifiedFileSize() {
                    return -1;
                }

                @Override
                public String getHost() {
                    return host;
                }
            };
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLinkDownloadable, br.createGetRequest(genlink), true, maxChunks);
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, genlink, true, maxChunks);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You are not premium so you can't download this file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Premium required to download this file.");
            } else if (br.containsHTML(">An error occured while processing your request<")) {
                logger.info("Retrying: Failed to generate alldebrid.com link because API connection failed for host link: " + link.getDownloadURL());
                mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "Unknown error", 3, 30 * 60 * 1000l);
            }
            if (!isDirectLink(link)) {
                if (br.containsHTML("range not ok")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                /* unknown error */
                logger.severe("Error: Unknown Error");
                // disable hoster for 5min
                mhm.putError(this.currAcc, this.currDownloadLink, 5 * 60 * 1000l, "Unknown Error");
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        /* save generated link, only if... it it comes from handleMulti */
        if (!isDirectLink(link)) {
            link.setProperty("genLinkAllDebrid", genlink);
        }
        if (!dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) {
                    return;
                }
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull") || "Unexpected rangeheader format:null".equals(errormessage))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(AllDebridCom.NOCHUNKS, false) == false) {
                    link.setProperty(AllDebridCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private boolean loadToken(final Account account, final DownloadLink downloadLink) throws Exception {
        synchronized (accLock) {
            if (token == null || account.getProperty("token", null) == null) {
                fetchAccountInfo(account);
                if (token == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                setConstants(account, downloadLink);
                return false;
            }
            return true;
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            // define custom browser headers and language settings.
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink dl) throws Exception {
        setConstants(null, dl);
        prepBrowser(br, dl.getDownloadURL());
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if ((con.isContentDisposition() || con.isOK()) && !con.getContentType().contains("html")) {
                if (dl.getFinalFileName() == null) {
                    dl.setFinalFileName(getFileNameFromHeader(con));
                }
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
            getLogger().log(e);
            dl.setAvailable(false);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        if (this.currAcc != null) {
            this.token = this.currAcc.getStringProperty("token", null);
        } else {
            this.token = null;
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (isDirectLink(downloadLink)) {
            // generated links do not require an account to download
            return true;
        }
        return true;
    }

    private boolean isDirectLink(final DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}