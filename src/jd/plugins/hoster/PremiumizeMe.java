//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64OutputStream;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountJsonConfig;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumize.me" }, urls = { "https?://dt\\d+.energycdn.com/(torrentdl|dl)/.+" })
public class PremiumizeMe extends UseNet {
    private static MultiHosterManagement mhm          = new MultiHosterManagement("premiumize.me");
    private static final String          SENDDEBUGLOG = "SENDDEBUGLOG";
    private static final String          NOCHUNKS     = "NOCHUNKS";
    private static final String          FAIL_STRING  = "premiumizeme";

    /*
     * IMPORTANT INFORMATION: According to their support we can 'hammer' their API every 5 minutes so we could even make an
     * "endless retries" mode which, on fatal errors, waits 5 minutes, then tries again.
     */
    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium(getProtocol() + "premiumize.me");
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SENDDEBUGLOG, "Send debug logs to PremiumizeMe automatically?").setDefaultValue(true));
    }

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new PluginConfigPanelNG() {
            @Override
            public void updateContents() {
            }

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "ssldownloadsenabled".equals(keyHandler.getKey());
            }

            @Override
            public void save() {
            }
        };
    }

    public static interface PremiumizeMeConfigInterface extends UsenetAccountConfigInterface {
        public class Translation {
            public String getSSLDownloadsEnabled_label() {
                return _JDT.T.lit_ssl_enabled();
            }
        }

        public static final PremiumizeMeConfigInterface.Translation TRANSLATION = new Translation();

        @DefaultBooleanValue(true)
        @Order(10)
        boolean isSSLDownloadsEnabled();

        void setSSLDownloadsEnabled(boolean b);
    };

    @Override
    public String getAGBLink() {
        return getProtocol() + "premiumize.me/?show=tos";
    }

    public Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie("https://premiumize.me", "lang", "english");
        br.setCookie("http://premiumize.me", "lang", "english");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 400, 401, 402, 403, 404, 428, 502, 503, 509 });
        return br;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new PremiumizeMeAccountFactory(callback);
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
            br = newBrowser();
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openHeadConnection(link.getDownloadURL());
                } catch (final BrowserException e) {
                    return AvailableStatus.UNCHECKABLE;
                }
                if (!con.getContentType().contains("html") && con.isOK()) {
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(Encoding.urlDecode(Plugin.getFileNameFromHeader(con), false));
                    }
                    link.setVerifiedFileSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } catch (final Throwable e) {
                return AvailableStatus.UNCHECKABLE;
            } finally {
                try {
                    /* make sure we close connection */
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (StringUtils.equals(getHost(), downloadLink.getHost()) && account == null) {
            // generated links do not require an account
            return true;
        }
        return account != null;
    }

    private Object getConnectionSettingsValue(final String host, final Account account, final String key) {
        Map<String, Object> connection_settings = null;
        AccountInfo ai = null;
        if (account != null && (ai = account.getAccountInfo()) != null && (connection_settings = (Map<String, Object>) ai.getProperty("connection_settings")) != null) {
            Map<String, Object> settings = (Map<String, Object>) connection_settings.get(host);
            if (settings != null) {
                return settings.get(key);
            }
        }
        return null;
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null && link != null) {
            if (isUsenetLink(link)) {
                return 10;
            } else {
                final Object ret = getConnectionSettingsValue(link.getHost(), account, "max_connections_per_hoster");
                if (ret != null && ret instanceof Integer) {
                    return (Integer) ret;
                }
            }
        }
        if (link != null && account == null) {
            if (StringUtils.equals(getHost(), link.getHost())) {
                // generated links do not require an account
                return Integer.MAX_VALUE;
            } else {
                // multihost requires an account
                return 0;
            }
        }
        // needed to signal free downloads are possible
        return Integer.MAX_VALUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        handlePremium(downloadLink, null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        requestFileInformation(link);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        if (dl.getConnection().getContentType().contains("html") || !dl.getConnection().isOK()) {
            br.followConnection();
            if (br.containsHTML("Please check your fair use status at")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Please check your fair use status");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxConnections = 0;
        boolean resume = true;
        Object ret = getConnectionSettingsValue(link.getHost(), account, "max_connections_per_file");
        if (ret != null && ret instanceof Integer) {
            maxConnections = (Integer) ret;
            logger.info("Host:" + link.getHost() + " is limited to " + maxConnections + " chunks");
            if (maxConnections > 1) {
                maxConnections = -maxConnections;
            }
        }
        ret = getConnectionSettingsValue(link.getHost(), account, "resume");
        if (ret != null && ret instanceof Boolean) {
            resume = (Boolean) ret;
            logger.info("Host:" + link.getHost() + " allows resume: " + resume);
        }
        if (resume == false) {
            logger.info("Host:" + link.getHost() + " does not allow resume, set chunks to 1");
            maxConnections = 1;
        }
        if (link.getBooleanProperty(PremiumizeMe.NOCHUNKS, false) == true) {
            maxConnections = 1;
        }
        br.setCurrentURL(null);
        try {
            final String url;
            if (((PremiumizeMeConfigInterface) AccountJsonConfig.get(account)).isSSLDownloadsEnabled()) {
                url = dllink.replaceFirst("^http://", "https://");
            } else {
                url = dllink.replaceFirst("^https://", "http://");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, resume, maxConnections);
        } catch (final SocketTimeoutException e) {
            logger.info("SocketTimeoutException on downloadstart");
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_sockettimeout", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_sockettimeout", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_sockettimeout", Property.NULL);
                logger.info("SocketTimeoutException on downloadstart -> Show 'Connection problems' error'");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection problems", 5 * 60 * 1000l);
            }
        }
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumizeMe.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumizeMe.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                return;
            }
            return;
        } else if (dl.getConnection().getContentType() != null && !dl.getConnection().getContentType().contains("html") && !dl.getConnection().getContentType().contains("text")) {
            /*
             * no content disposition, but api says that some hoster might not have one
             */
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumizeMe.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumizeMe.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                return;
            }
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
            sendErrorLog(link, account);
            handleAPIErrors(br, account, link);
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_unknown2", 1);
            if (timesFailed <= 3) {
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_unknown2", timesFailed);
                logger.info("Unknown error2 - retrying");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_unknown2", Property.NULL);
                logger.info("Unknown error2 - disabling current host!");
                mhm.putError(account, link, 60 * 60 * 1000l, "error2");
            }
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST, FEATURE.USENET };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            mhm.runCheck(account, link);
            br = newBrowser();
            showMessage(link, "Task 1: Generating Link");
            /* request Download */
            final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            br.getPage(getProtocol() + "api.premiumize.me/pm-api/v1.php?method=directdownloadlink&params[login]=" + Encoding.urlEncode(account.getUser()) + "&params[pass]=" + Encoding.urlEncode(account.getPass()) + "&params[link]=" + Encoding.urlEncode(url));
            if (br.containsHTML(">403 Forbidden<")) {
                int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING, 0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailed" + FAIL_STRING, timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    link.setProperty("timesfailed" + FAIL_STRING, Property.NULL);
                    mhm.putError(account, link, 60 * 60 * 1000l, "Server error");
                }
            }
            handleAPIErrors(br, account, link);
            String dllink = br.getRegex("location\":\"(https?[^\"]+)").getMatch(0);
            if (dllink == null) {
                sendErrorLog(link, account);
                int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_unknown", 0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailed" + FAIL_STRING + "_unknown", timesFailed);
                    logger.info("Unknown error - retrying");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown Error");
                } else {
                    link.setProperty("timesfailed" + FAIL_STRING + "_unknown", Property.NULL);
                    logger.info("Unknown error - disabling current host!");
                    mhm.putError(account, link, 60 * 60 * 1000l, "Unknown Error");
                }
            }
            dllink = dllink.replaceAll("\\\\/", "/");
            showMessage(link, "Task 2: Download begins!");
            try {
                handleDL(account, link, dllink);
            } catch (Exception e) {
                try {
                    if (dl.externalDownloadStop() == false) {
                        LogSource.exception(logger, e);
                        sendErrorLog(link, account);
                    }
                } catch (final Throwable e1) {
                }
                throw e;
            }
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        final String type = br.getRegex("type\":\"(.*?)\"").getMatch(0);
        // free / expired accounts are pointless adding
        if ("free".equalsIgnoreCase(type)) {
            ai.setProperty("multiHostSupport", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String status = type;
        // https://secure.premiumize.me/<extuid>/<port>/proxy.pac
        String extuid = br.getRegex("extuid\":\"(.*?)\"").getMatch(0);
        account.setProperty("extuid", extuid);
        if (status == null) {
            status = "Unknown Account Type";
        }
        String expire = br.getRegex("\"expires\":(\\d+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil((Long.parseLong(expire)) * 1000);
        }
        final String fairUse = br.getRegex("fairuse_left\":([\\d\\.]+)").getMatch(0);
        if (fairUse != null) {
            final double d = Double.parseDouble(fairUse);
            status = status + ": FairUsage " + (100 - ((int) (d * 100.0))) + "%";
            // 7 day rolling average
            // AVERAGE = way to display percentage value. prevent controlling
            // from using figure. Just a GUI display for the user.
            // "fairuse_left":0.99994588120502,
            // ai.setTrafficLeft(AVERAGE(Integer.parseInt(fairUse.trim()) *
            // 100));
        }
        ai.setStatus(status);
        String trafficleft_bytes = br.getRegex("trafficleft_bytes\":(-?[\\d\\.]+)").getMatch(0);
        if (trafficleft_bytes != null) {
            if (trafficleft_bytes.contains(".")) {
                trafficleft_bytes = trafficleft_bytes.replaceFirst("\\..+$", "");
            }
            ai.setTrafficMax(SizeFormatter.getSize("220 GByte", true, true));
            if (Long.parseLong(trafficleft_bytes) <= 0) {
                trafficleft_bytes = "0";
            }
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        String hostsSup = br.getPage(getProtocol() + "api.premiumize.me/pm-api/v1.php?method=hosterlist&params[login]=" + Encoding.urlEncode(account.getUser()) + "&params[pass]=" + Encoding.urlEncode(account.getPass()));
        handleAPIErrors(br, account, null);
        HashMap<String, Object> response = JSonStorage.restoreFromString(br.toString(), new HashMap<String, Object>().getClass());
        if (response == null || (response = (HashMap<String, Object>) response.get("result")) == null) {
            response = new HashMap<String, Object>();
        }
        final String HostsJSON = PluginJSonUtils.getJsonArray(this.br, "tldlist");
        final String[] hosts = new Regex(HostsJSON, "\"([a-zA-Z0-9\\.\\-]+)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        if ("premium".equals(type)) {
            account.setType(AccountType.PREMIUM);
            supportedHosts.add("usenet");
        } else {
            account.setType(AccountType.FREE);
        }
        supportedHosts.add("daofile.com");
        ai.setMultiHostSupport(this, supportedHosts);
        ai.setProperty("connection_settings", response.get("connection_settings"));
        return ai;
    }

    private static String getProtocol() {
        if (Application.getJavaVersion() < Application.JAVA17) {
            return "http://";
        } else {
            return "https://";
        }
    }

    private void login(Account account) throws Exception {
        br = newBrowser();
        final String username = Encoding.urlEncode(account.getUser());
        if (username == null || !username.trim().matches("^\\d{9}$")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use Customer ID and PIN for logging in (find in your account area on the website)", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final String password = Encoding.urlEncode(account.getPass());
        if (password == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use Customer ID and PIN for logging in (find in your account area on the website)", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.getPage(getProtocol() + "api.premiumize.me/pm-api/v1.php?method=accountstatus&params[login]=" + username.trim() + "&params[pass]=" + password);
        handleAPIErrors(br, account, null);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException {
        String statusCode = br.getRegex("\"status\":(\\d+)").getMatch(0);
        if (statusCode == null) {
            return;
        }
        String statusMessage = br.getRegex("\"statusmessage\":\"([^\"]+)").getMatch(0);
        try {
            int status = Integer.parseInt(statusCode);
            switch (status) {
            case 0:
                // should not happen since we don't allow free accounts... free accounts can not donwload... pointless having them enabled
                if ("Your account is not premium".equalsIgnoreCase(statusMessage)) {
                    throw new AccountRequiredException("Your account type is not supported");
                }
                // 20150425
                // {"result":null,"statusmessage":"Daily limit reached for this host!","status":0}
                if (statusMessage == null) {
                    statusMessage = "Download limit reached for this host!";
                }
                mhm.putError(account, downloadLink, 10 * 60 * 1000l, statusMessage);
            case 2:
                /* E.g. Error: file_get_contents[...] */
                logger.info("Errorcode 2: Strange error");
                if (downloadLink.getIntegerProperty("timesfailed" + FAIL_STRING, 0) >= 5) {
                    /* Retried enough times --> Temporarily disable host! */
                    mhm.putError(account, downloadLink, 5 * 60 * 1000l, "Errorcode 2");
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Errorcode 2");
            case 200:
                /* all okay */
                return;
            case 400:
                /* not a valid link, do not try again with this multihoster */
                if (statusMessage == null) {
                    statusMessage = "Invalid DownloadLink";
                }
                mhm.putError(null, downloadLink, 3 * 60 * 60 * 1000l, statusMessage);
                break;
            case 401:
                /* not logged in, disable account. */
                if (statusMessage == null) {
                    statusMessage = "Login error";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 402:
                /* account with outstanding payment,disable account */
                if (statusMessage == null) {
                    statusMessage = "Account payment required in order to download";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 403:
                /* forbidden, banned ip , temp disable account */
                // additional info provided to the user for this error message.
                String statusMessage1 = "Login prevented by MultiHoster! Please contact them for resolution";
                if (statusMessage == null) {
                    statusMessage = statusMessage1;
                } else {
                    statusMessage += statusMessage + " :: " + statusMessage1;
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 404:
                /* file offline */
                if (statusMessage != null && statusMessage.matches(".*?Video not yet released.*?")) {
                    /* E.g. Brazzers.com */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Video not yet released");
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 428:
                /* hoster currently not possible,block host for 30 mins */
                if (statusMessage == null) {
                    statusMessage = "Hoster currently not possible";
                }
                mhm.putError(null, downloadLink, 30 * 60 * 1000l, statusMessage);
                break;
            case 500:
                /* link limit reached, disable plugin for this link */
                if (statusMessage == null) {
                    statusMessage = "Link limit reached";
                }
                mhm.putError(account, downloadLink, 10 * 60 * 1000l, statusMessage);
                break;
            case 502:
                /* unknown technical error, block host for 3 mins */
                if (statusMessage == null) {
                    statusMessage = "Unknown technical error";
                }
                // tempUnavailableHoster(account, downloadLink, 3 * 60 * 1000);
                /* only disable plugin for this link */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage, 3 * 60 * 1000l);
            case 503:
                /*
                 * temp multihoster issue, maintenance period, block host for 3 mins
                 */
                if (statusMessage == null) {
                    statusMessage = "Hoster temporarily not possible";
                }
                statusMessage = downloadLink.getHost() + ": " + statusMessage;
                /* only disable plugin for this link */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage, 3 * 60 * 1000l);
            case 509:
                /* fair use limit reached ,block host for 10 mins */
                if (statusMessage == null) {
                    statusMessage = "Fair use limit reached!";
                }
                mhm.putError(account, downloadLink, 10 * 60 * 1000l, statusMessage);
            default:
                /* unknown error, do not try again with this multihoster */
                if (statusMessage == null) {
                    statusMessage = "Unknown error code, please inform JDownloader Development Team";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info("PremiumizeMe Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
            throw e;
        }
        logger.info("PremiumizeMe Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", false, 119));
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", true, 563));
        return ret;
    }

    @SuppressWarnings("deprecation")
    private void sendErrorLog(DownloadLink link, Account acc) {
        try {
            if (getPluginConfig().getBooleanProperty(SENDDEBUGLOG, true) == false) {
                return;
            }
            String postString = "uid=" + Encoding.urlEncode(acc.getUser()) + "&link=" + Encoding.urlEncode(link.getDownloadURL());
            ByteArrayOutputStream bos;
            GZIPOutputStream logBytes = new GZIPOutputStream(new Base64OutputStream(bos = new ByteArrayOutputStream()));
            logBytes.write(((LogSource) logger).toString().getBytes("UTF-8"));
            logBytes.close();
            postString = postString + "&error=" + Encoding.urlEncode(bos.toString("UTF-8"));
            Browser br2 = br.cloneBrowser();
            br2.postPage(getProtocol() + "api.premiumize.me/pm-api/jderror.php?method=log", postString);
        } catch (final Throwable e) {
            LogSource.exception(logger, e);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public static class PremiumizeMeAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      IDHELP           = "Enter your account id (9 digits)";
        private final String      PINHELP          = "Enter your pin";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private String getUsername() {
            if (IDHELP.equals(this.name.getText())) {
                return null;
            }
            return this.name.getText();
        }

        private final ExtTextField     name;
        private final ExtPasswordField pass;
        private static String          EMPTYPW = "                 ";
        private final JLabel           idLabel;

        public PremiumizeMeAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel(_GUI.T.premiumize_add_account_click_here()));
            add(new JLink(getProtocol() + "www.premiumize.me/account"));
            add(idLabel = new JLabel(_GUI.T.premiumize_add_account_idlabel()));
            add(this.name = new ExtTextField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            });
            name.setHelpText(IDHELP);
            add(new JLabel("PIN:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String userName = getUsername();
            if (userName == null || !userName.trim().matches("^\\d{9}$")) {
                idLabel.setForeground(Color.RED);
                return false;
            }
            idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(getUsername(), getPassword());
        }
    }
}