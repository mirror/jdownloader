package jd.plugins.hoster;

import java.net.URL;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

//IMPORTANT: this class must stay in jd.plugins.hoster because it extends another plugin (UseNet) which is only available through PluginClassLoader
abstract public class ZeveraCore extends UseNet {
    /* Connection limits */
    private static final boolean  ACCOUNT_PREMIUM_RESUME       = true;
    private static final int      ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    // private static Object LOCK = new Object();
    private MultiHosterManagement mhm                          = null;
    private static final boolean  allow_free_account_downloads = false;

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (isDirectURL(link.getDownloadLink())) {
            final String new_url = link.getPluginPatternMatcher().replaceAll("[a-z0-9]+decrypted://", "https://");
            link.setPluginPatternMatcher(new_url);
        }
    }

    public ZeveraCore(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        // this.enablePremium("https://www." + this.getHost() + "/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/legal#tos";
    }

    @Override
    public void init() {
        mhm = new MultiHosterManagement(this.getHost());
    }

    /** Must override!! */
    public String getClientID() {
        return null;
    }

    /**
     * Returns whether resume is supported or not for current download mode based on account availability and account type. <br />
     * Override this function to set resume settings!
     */
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    /**
     * Returns how many max. chunks per file are allowed for current download mode based on account availability and account type. <br />
     * Override this function to set chunks settings!
     */
    public int getDownloadModeMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* Direct URLs can be downloaded without account! */
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        /* No free downloads at all possible by default */
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    protected Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        prepBrowser(br, getHost());
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            return requestFileInformationDirectURL(this.br, link);
        }
    }

    protected AvailableStatus requestFileInformationDirectURL(final Browser br, final DownloadLink link) throws Exception {
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br, br.createHeadRequest(link.getPluginPatternMatcher()));
            if (!con.getContentType().contains("html") && con.isOK()) {
                if (link.getFinalFileName() == null) {
                    link.setFinalFileName(Encoding.urlDecode(Plugin.getFileNameFromHeader(con), false));
                }
                link.setVerifiedFileSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else if (con.getResponseCode() == 404) {
                /* Usually 404 == offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /*
                 * E.g. 403 because of bad fair use status (or offline, 2019-05-08: This is confusing, support was told to change it to a
                 * 404 if a cloud file has been deleted by the user and is definitely offline!)
                 */
                return AvailableStatus.UNCHECKABLE;
            }
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
        if (isDirectURL(downloadLink) && account == null) {
            /* Generated links can be downloaded without account. */
            return true;
        } else {
            return account != null;
        }
    }

    public boolean isDirectURL(final DownloadLink downloadLink) {
        return StringUtils.equals(getHost(), downloadLink.getHost());
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (isDirectURL(downloadLink)) {
            /* DirectURLs can be downloaded without logging in */
            handleDL_DIRECT(null, downloadLink);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (!isDirectURL(link)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            handleDL_DIRECT(account, link);
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else if (isDirectURL(link)) {
            handleDL_DIRECT(account, link);
        } else {
            this.br = prepBR(this.br);
            mhm.runCheck(account, link);
            login(this.br, account, false, getClientID());
            String dllink = getDllink(this.br, account, link, getClientID(), this);
            if (StringUtils.isEmpty(dllink)) {
                handleAPIErrors(this.br, link, account);
                mhm.handleErrorGeneric(account, link, "dllinknull", 2, 5 * 60 * 1000l);
            }
            handleDL_MOCH(account, link, dllink);
        }
    }

    private void handleDL_MOCH(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(account.getHoster() + "directlink", dllink);
        try {
            antiCloudflare(br, dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                handleAPIErrors(this.br, link, account);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(account.getHoster() + "directlink", Property.NULL);
            throw e;
        }
    }

    protected void antiCloudflare(Browser br, final String url) throws Exception {
        final Request request = br.createHeadRequest(url);
        prepBrowser(br, request.getURL().getHost());
        final URLConnectionAdapter con = openAntiDDoSRequestConnection(br, request);
        con.disconnect();
    }

    /** Account is not required */
    private void handleDL_DIRECT(final Account account, final DownloadLink link) throws Exception {
        antiCloudflare(br, link.getPluginPatternMatcher());
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getResponseCode() == 403) {
            /*
             * 2019-05-08: This most likely only happens for offline cloud files. They've been notified to update their API to return a
             * clear 404 for offline files.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Most likely you have reached your bandwidth limit, or you don't have this file in your cloud anymore!", 3 * 60 * 1000l);
        }
        final String contenttype = dl.getConnection().getContentType();
        if (contenttype.contains("html")) {
            // br.followConnection();
            // handleAPIErrors(this.br);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 3 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    public String getDllink(final Browser br, final Account account, final DownloadLink link, final String client_id, final PluginForHost hostPlugin) throws Exception {
        String dllink = checkDirectLink(br, link, account.getHoster() + "directlink");
        if (dllink == null) {
            /* TODO: Check if the cache function is useful for us */
            // br.getPage("https://www." + account.getHoster() + "/api/cache/check?client_id=" + client_id + "&pin=" +
            // Encoding.urlEncode(account.getPass()) + "&items%5B%5D=" +
            // Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, hostPlugin)));
            final String hash_md5 = link.getMD5Hash();
            final String hash_sha1 = link.getSha1Hash();
            final String hash_sha256 = link.getSha256Hash();
            String getdata = "?client_id=" + client_id + "&pin=" + Encoding.urlEncode(getAPIKey(account)) + "&src=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, hostPlugin));
            if (hash_md5 != null) {
                getdata += "&hash_md5=" + hash_md5;
            }
            if (hash_sha1 != null) {
                getdata += "&hash_sha1=" + hash_sha1;
            }
            if (hash_sha256 != null) {
                getdata += "&hash_sha256=" + hash_sha256;
            }
            getPage(br, "https://www." + account.getHoster() + "/api/transfer/directdl" + getdata);
            dllink = PluginJSonUtils.getJsonValue(br, "location");
        }
        return dllink;
    }

    public String checkDirectLink(final Browser br, final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = prepBR(this.br);
        final AccountInfo ai = fetchAccountInfoAPI(this.br, getClientID(), account);
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Browser br, final String client_id, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account, true, client_id);
        /* 2018-12-07: Rare serverside issue returns bad values e.g.: "limit_used":9.3966473825276e-5 */
        final String fair_use_used_str = PluginJSonUtils.getJson(br, "limit_used");
        final String space_used = PluginJSonUtils.getJson(br, "space_used");
        final String premium_until_str = PluginJSonUtils.getJson(br, "premium_until");
        if (space_used != null && space_used.matches("\\d+")) {
            ai.setUsedSpace(Long.parseLong(space_used));
        }
        /* E.g. free account: "premium_until":false */
        final long premium_until = (premium_until_str != null && premium_until_str.matches("\\d+")) ? Long.parseLong(premium_until_str) * 1000 : 0;
        if (premium_until > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(getMaxSimultanPremiumDownloadNum());
            if (!StringUtils.isEmpty(fair_use_used_str)) {
                final double d = Double.parseDouble(fair_use_used_str);
                final int fairUsagePercent = (int) (d * 100.0);
                if (fairUsagePercent >= 100) {
                    /* Fair use limit reached --> No traffic left, no downloads possible at the moment */
                    ai.setTrafficLeft(0);
                    ai.setStatus("Premium | Fair usage: " + fairUsagePercent + "% (limit reached)");
                } else {
                    ai.setUnlimitedTraffic();
                    ai.setStatus("Premium | Fair usage: " + fairUsagePercent + "%");
                }
            } else {
                /* This should never happen */
                ai.setStatus("Premium | Fair usage: unknown");
                ai.setUnlimitedTraffic();
            }
            ai.setValidUntil(premium_until);
        } else {
            /* Expired == FREE */
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(getMaxSimultaneousFreeAccountDownloads());
            setFreeAccountTraffic(ai);
        }
        getPage(br, "/api/services/list?client_id=" + client_id + "&pin=" + Encoding.urlEncode(getAPIKey(account)));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final ArrayList<String> supportedHosts = new ArrayList<String>();
        final ArrayList<String> directdl = (ArrayList<String>) entries.get("directdl");
        // final ArrayList<String> cache = (ArrayList<String>) entries.get("cache");
        final HashSet<String> list = new HashSet<String>();
        if ("premiumize.me".equalsIgnoreCase(account.getHoster())) {
            /* Some premiumize-only features */
            list.add("usenet");
            if (account.getType() == AccountType.FREE && allow_free_account_downloads) {
                handleFreeModeLoginDialog("https://www.premiumize.me/free");
            }
        }
        if (directdl != null) {
            list.addAll(directdl);
        }
        ai.setMultiHostSupport(this, new ArrayList<String>(list));
        return ai;
    }

    @SuppressWarnings("deprecation")
    private void handleFreeModeLoginDialog(final String url) {
        final boolean showAlways = false;
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (showAlways || config.getBooleanProperty("featuredialog_login_Shown", Boolean.FALSE) == false) {
                if (showAlways || config.getProperty("featuredialog_login_Shown2") == null) {
                    showFreeModeLoginInformation(url);
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_login_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_login_Shown2", "shown");
                config.save();
            }
        }
    }

    private Thread showFreeModeLoginInformation(final String url) throws Exception {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = br.getHost() + " ermöglicht ab sofort auch kostenlose Downloads";
                        message += "Hallo liebe(r) " + br.getHost() + " NutzerIn\r\n";
                        message += "Ab sofort kannst du diesen Anbieter auch nutzen ohne zu bezahlen!\r\n";
                        message += "Mehr infos dazu findest du unter:\r\n" + new URL(url) + "\r\n";
                    } else {
                        title = br.getHost() + " allows free downloads from now on";
                        message += "Hello dear " + br.getHost() + " user\r\n";
                        message += "From now on this service lets you download for free as well.\r\n";
                        message += "More information:\r\n" + new URL(url) + "\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(url);
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

    @SuppressWarnings("deprecation")
    private void handleFreeModeDownloadDialog(final String url) {
        final boolean showAlways = true;
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (showAlways || config.getBooleanProperty("featuredialog_download_Shown", Boolean.FALSE) == false) {
                if (showAlways || config.getProperty("featuredialog_download_Shown2") == null) {
                    showFreeModeDownloadInformation(url);
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("featuredialog_download_Shown", Boolean.TRUE);
                config.setProperty("featuredialog_download_Shown2", "shown");
                config.save();
            }
        }
    }

    private Thread showFreeModeDownloadInformation(final String url) throws Exception {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final boolean xSystem = CrossSystem.isOpenBrowserSupported();
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = br.getHost() + " möchte einen kostenlosen Download starten";
                        message += "Hallo liebe(r) " + br.getHost() + " NutzerIn\r\n";
                        if (xSystem) {
                            message += "Um kostenlos von diesem Anbieter herunterladen zu können musst du den 'free mode' im Fenster das sich gleich öffnet aktivieren.\r\n";
                        } else {
                            message += "Um kostenlos von diesem Anbieter herunterladen zu können musst du den 'free mode' unter dieser Adresse aktivieren:\r\n" + new URL(url) + "\r\n";
                        }
                    } else {
                        title = br.getHost() + " wants to start a free download";
                        message += "Hello dear " + br.getHost() + " user\r\n";
                        if (xSystem) {
                            message += "To be able to use the free mode of this service, you will have to enable it in the browser-window which will open soon.\r\n";
                        } else {
                            message += "To be able to use the free mode of this service, you will have to enable it here:\r\n" + new URL(url) + "\r\n";
                        }
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(url);
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

    public void setFreeAccountTraffic(final AccountInfo ai) {
        /* Default = Free accounts do not have any traffic. */
        ai.setTrafficLeft(0);
    }

    public void login(Browser br, final Account account, final boolean force, final String clientID) throws Exception {
        synchronized (account) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            br = prepBR(br);
            loginAPI(br, clientID, account, force);
        }
    }

    public void loginAPI(final Browser br, final String clientID, final Account account, final boolean force) throws Exception {
        getPage(br, "https://www." + account.getHoster() + "/api/account/info?client_id=" + clientID + "&pin=" + Encoding.urlEncode(getAPIKey(account)));
        final String status = PluginJSonUtils.getJson(br, "status");
        if (!"success".equalsIgnoreCase(status)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "API key invalid! Make sure you entered your current API key which can be found here: " + account.getHoster() + "/account", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (account.getHoster().equalsIgnoreCase("premiumize.me")) {
            /* 2019-02-10: Workaround for their Usenet support. Their Usenet login-servers only accept APIKEY:APIKEY. */
            account.setUser(account.getPass());
        }
    }

    private String getAPIKey(final Account account) {
        return account.getPass().trim();
    }

    /**
     * Keep this for possible future API implementation
     *
     * @throws InterruptedException
     */
    private void handleAPIErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        /* E.g. {"status":"error","error":"topup_required","message":"Please purchase premium membership or activate free mode."} */
        final String status = PluginJSonUtils.getJson(br, "status");
        if ("error".equalsIgnoreCase(status)) {
            String errortype = PluginJSonUtils.getJson(br, "error");
            if (errortype == null) {
                errortype = "unknowndlerror";
            }
            String message = PluginJSonUtils.getJson(br, "message");
            if (message == null) {
                /* Fallback */
                message = errortype;
            }
            if ("topup_required".equalsIgnoreCase(errortype)) {
                /* {"status":"error","error":"topup_required","message":"Please purchase premium membership or activate free mode."} */
                if (account != null && account.getType() == AccountType.FREE && allow_free_account_downloads) {
                    handleFreeModeDownloadDialog("https://www." + this.br.getHost() + "/free");
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, message, 30 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, message);
                }
            } else if ("content not in cache".equalsIgnoreCase(message)) {
                /* 2019-02-19: Not all errors have an errortype given */
                /* E.g. {"status":"error","message":"content not in cache"} */
                /* Case: User tries to download non-cached-file via free account. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message);
            } else {
                /* Unknown error */
                mhm.handleErrorGeneric(account, link, errortype, 2, 5 * 60 * 1000l);
            }
        }
    }

    public static String getCloudID(final String url) {
        if (url.contains("folder_id")) {
            return new Regex(url, "folder_id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        } else {
            return new Regex(url, "id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}