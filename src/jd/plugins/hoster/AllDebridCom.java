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
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

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
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
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

    @Override
    public void extendDownloadsTableContextMenu(JComponent parent, PluginView<DownloadLink> pv, Collection<PluginView<DownloadLink>> views) {
        if (pv.size() == 1) {
            final JMenuItem changeURLMenuItem = createChangeURLMenuItem(pv.get(0));
            if (changeURLMenuItem != null) {
                parent.add(changeURLMenuItem);
            }
        }
    }

    private static MultiHosterManagement mhm                             = new MultiHosterManagement("alldebrid.com");
    private static String                api_base                        = "https://api.alldebrid.com";
    // this is used by provider which calculates unique token to agent/client.
    private static final String          agent                           = "agent=JDownloader";
    /* False = use old username&password login (Deprecated!) */
    private static final String          PROPERTY_TOKEN_EXPIRE_TIMESTAMP = "TOKEN_EXPIRE_TIMESTAMP";

    public String fetchToken(final Account account, final AccountInfo accountInfo) throws Exception {
        synchronized (account) {
            try {
                String token = account.getStringProperty(TOKEN, null);
                if (token != null) {
                    /*
                     * 2019-05-31: TODO: Also check validity of that token and renew it if it (nearly) expired, see:
                     * PROPERTY_TOKEN_EXPIRE_TIMESTAMP
                     */
                    loginAccount(account, accountInfo, token);
                }
                /* Full login */
                int error = parseError();
                if (token == null || error == 1 || error == 5) {
                    logger.info("Performing full login");
                    /*
                     * 2019-05-31: TODO: This way, a user could add one account XX times as username/password are not required. Find an
                     * identifier so we can prohibit this. Username/E-Mail combination should do the trick.
                     */
                    /**
                     * TODO: we could login with username/password and accept the pin automatically
                     */
                    getPage(api_base + "/pin/get?" + agent);
                    final String user_url = PluginJSonUtils.getJson(br, "user_url");
                    final String check_url = PluginJSonUtils.getJson(br, "check_url");
                    if (StringUtils.isEmpty(user_url) || StringUtils.isEmpty(check_url)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Thread dialog = showPINLoginInformation(user_url);
                    try {
                        for (int i = 0; i <= 23; i++) {
                            logger.info("Waiting for user to authorize application: " + i);
                            Thread.sleep(5000);
                            getPage(check_url);
                            error = parseError();
                            if (error != -1) {
                                /* Something went wrong */
                                break;
                            } else {
                                token = PluginJSonUtils.getJson(br, "token");
                                if (!StringUtils.isEmpty(token)) {
                                    break;
                                } else if (!dialog.isAlive()) {
                                    logger.info("Dialog closed!");
                                    break;
                                }
                            }
                        }
                    } finally {
                        dialog.interrupt();
                    }
                    if (StringUtils.isEmpty(token)) {
                        throw new AccountInvalidException("User failed to authorize PIN");
                    }
                    final String expires_in_secondsStr = PluginJSonUtils.getJson(br, "expires_in");
                    if (expires_in_secondsStr != null && expires_in_secondsStr.matches("\\d+")) {
                        /* 2019-05-31: TODO: Check how long this token really lasts. */
                        account.setProperty(PROPERTY_TOKEN_EXPIRE_TIMESTAMP, System.currentTimeMillis() + (Long.parseLong(expires_in_secondsStr) - 30) * 1000);
                    }
                    loginAccount(account, accountInfo, token);
                } else {
                    logger.info("Token login successful");
                }
                return token;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(TOKEN);
                }
                throw e;
            }
        }
    }

    private void loginAccount(final Account account, final AccountInfo accountInfo, final String token) throws Exception {
        synchronized (account) {
            getPage(api_base + "/user/login?" + agent + "&token=" + token);
            handleErrors(account, null);
            final boolean isPremium = PluginJSonUtils.parseBoolean(PluginJSonUtils.getJson(br, "isPremium"));
            if (!isPremium) {
                throw new AccountInvalidException("Free accounts are not supported!");
            } else {
                final String premiumUntil = PluginJSonUtils.getJson(br, "premiumUntil");
                final long until = Long.parseLong(premiumUntil) * 1000l;
                if (accountInfo != null) {
                    accountInfo.setValidUntil(until);
                }
                if (until > System.currentTimeMillis()) {
                    account.setProperty(TOKEN, token);
                } else {
                    throw new AccountInvalidException("Premium status expired!");
                }
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo accountInfo = new AccountInfo();
        fetchToken(account, accountInfo);
        // /hosts/domains will return offline hosts.
        getPage(api_base + "/hosts");
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
            accountInfo.setMultiHostSupport(this, supportedHosts);
        }
        return accountInfo;
    }

    private Thread showPINLoginInformation(final String pin_url) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Alldebrid.com - neue Login-Methode";
                        message += "Hallo liebe(r) alldebrid.com NutzerIn\r\n";
                        message += "Seit diesem Update hat sich die Login-Methode dieses Anbieters geändert um die sicherheit zu erhöhen!\r\n";
                        message += "Um deinen Account weiterhin in JDownloader verwenden zu können musst du folgende Schritte beachten:\r\n";
                        message += "1. Gehe sicher, dass du im Browser in deinem Alldebrid Account eingeloggt bist.\r\n";
                        message += "2. Öffne diesen Link im Browser:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "3. Bestätige die PIN im Browser.\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Alldebrid.com - New login method";
                        message += "Hello dear alldebrid.com user\r\n";
                        message += "This update has changed the login method of alldebrid.com in favor of security.\r\n";
                        message += "In order to keep using this service in JDownloader you need to follow these steps:\r\n";
                        message += "1. Make sure that you're logged in your alldebrid.com account with your default browser.\r\n";
                        message += "2. Open this URL in your browser:\r\n\t'" + pin_url + "'\t\r\n";
                        message += "3. Confirm the PIN you see in the browser window.\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(pin_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Integer parseError() {
        final String error = PluginJSonUtils.getJsonValue(br, "errorCode");
        if (error == null || !error.matches("\\d+")) {
            return -1;
        } else {
            return Integer.parseInt(error);
        }
    }

    private void handleErrors(final Account account, final DownloadLink downloadLink) throws PluginException, Exception {
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
            if (account != null) {
                synchronized (account) {
                    account.removeProperty("token");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        // download related
        // 30 This link is not valid or not supported.
        // 31 This link is not available on the file hoster website.
        // 32 Host unsupported or under maintenance.
        // 39 Generic unlocking error.
        case 30: {
            // tested by placing url in thats on a provider not in the supported host map. returns error 30. -raz
            // mhm.putError(null, this.currDownloadLink, 30 * 60 * 1000l, "Host provider not supported");
            // can't use the above in the situation where one link format is allowed and another is not... will result in the whole host
            // been removed
            // from the supported map for 30minutes.
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unsupported link", 30 * 60 * 1000l);
        }
        case 32:
            mhm.putError(account, downloadLink, 30 * 60 * 1000l, "Down for maintance");
        case 33:
            // {"error":"You have reached the free trial limit (7 days \/\/ 25GB downloaded or host uneligible for free
            // trial)","errorCode":33}
            throw new AccountInvalidException("You have reached the free trial limit!");
        case 31:
        case 39:
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        case 35: {
            // {"error":"All servers are full for this host, please retry later","errorCode":35}
            mhm.handleErrorGeneric(account, downloadLink, "No available slots for this host", 10, 5 * 60 * 1000l);
        }
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
        mhm.runCheck(account, link);
        showMessage(link, "Phase 1/2: Generating link");
        synchronized (account) {
            String token = loadToken(account);
            final String unlock = api_base + "/link/unlock?" + agent + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            getPage(unlock + "&token=" + token);
            if (11 == parseError()) {
                account.removeProperty(TOKEN);
                token = loadToken(account);
                getPage(unlock + "&token=" + token);
            }
            handleErrors(account, link);
        }
        final String genlink = PluginJSonUtils.getJsonValue(br, "link");
        if (genlink == null || !genlink.matches("https?://.+")) {
            // we need a final error handling for situations when
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filename = PluginJSonUtils.getJsonValue(br, "filename");
        if (StringUtils.equalsIgnoreCase(filename, "Ip not allowed.") || StringUtils.endsWithCaseInsensitive(genlink, "/alldebrid_server_not_allowed.txt")) {
            throw new AccountUnavailableException("Ip not allowed", 6 * 60 * 1000l);
        }
        handleDL(account, link, genlink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account acc, final DownloadLink link, final String genlink) throws Exception {
        if (genlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Task 2: Download begins!");
        final boolean useVerifiedFileSize;
        if (br != null && PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(br, "paws"))) {
            logger.info("don't use verified filesize because 'paws'!");
            useVerifiedFileSize = false;
        } else if (link.getVerifiedFileSize() > 0) {
            final Browser brc = br.cloneBrowser();
            final URLConnectionAdapter check = openAntiDDoSRequestConnection(brc, brc.createGetRequest(genlink));
            try {
                if (check.getCompleteContentLength() < 0) {
                    logger.info("don't use verified filesize because complete content length isn't available!");
                    useVerifiedFileSize = false;
                } else if (check.getCompleteContentLength() == link.getVerifiedFileSize()) {
                    logger.info("use verified filesize because it matches complete content length");
                    useVerifiedFileSize = true;
                } else {
                    if (link.getDownloadCurrent() > 0) {
                        logger.info("cannot resume different file!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        logger.info("don't use verified filesize because it doesn't match complete content length!");
                        useVerifiedFileSize = false;
                    }
                }
            } finally {
                check.disconnect();
            }
        } else {
            logger.info("use verified filesize");
            useVerifiedFileSize = true;
        }
        final String host = Browser.getHost(link.getDownloadURL());
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {
                return null;
            }

            @Override
            public long getVerifiedFileSize() {
                if (useVerifiedFileSize) {
                    return super.getVerifiedFileSize();
                } else {
                    return -1;
                }
            }

            @Override
            public String getHost() {
                final DownloadInterface dli = getDownloadInterface();
                if (dli != null) {
                    final URLConnectionAdapter connection = dli.getConnection();
                    if (connection != null) {
                        return connection.getURL().getHost();
                    }
                }
                return host;
            }
        };
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLinkDownloadable, br.createGetRequest(genlink), true, 0);
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
                mhm.handleErrorGeneric(acc, link, "Unknown error", 3, 30 * 60 * 1000l);
            }
            if (!isDirectLink(link)) {
                if (br.containsHTML("range not ok")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                /* unknown error */
                logger.severe("Error: Unknown Error");
                // disable hoster for 5min
                mhm.putError(acc, link, 5 * 60 * 1000l, "Unknown Error");
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        /* save generated link, only if... it it comes from handleMulti */
        if (!isDirectLink(link)) {
            link.setProperty("genLinkAllDebrid", genlink);
        }
        dl.startDownload();
    }

    private final String TOKEN = "token";

    private String loadToken(final Account account) throws Exception {
        synchronized (account) {
            String ret = account.getStringProperty(TOKEN, null);
            if (ret == null) {
                ret = fetchToken(account, null);
                if (ret == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    return ret;
                }
            } else {
                return ret;
            }
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
        prepBrowser(br, dl.getDownloadURL());
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createGetRequest(dl.getDownloadURL()));
            if (con.isOK() && (con.isContentDisposition() || !con.getContentType().contains("html"))) {
                if (dl.getFinalFileName() == null) {
                    dl.setFinalFileName(getFileNameFromHeader(con));
                }
                if (con.getLongContentLength() > 0) {
                    dl.setVerifiedFileSize(con.getLongContentLength());
                }
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (PluginException e) {
            throw e;
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
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