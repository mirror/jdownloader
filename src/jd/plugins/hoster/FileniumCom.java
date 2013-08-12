//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filenium.com" }, urls = { "http://\\w+\\.filenium\\.com/get/\\w+/.+" }, flags = { 2 })
public class FileniumCom extends PluginForHost {

    /**
     * TODO: better error handling, as there exits none at the moment
     */
    private static Object LOCK = new Object();

    public FileniumCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filenium.com/checkuserjd");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://filenium.com/legal";
    }

    /** The list of server values displayed to the user */
    private final String[]                                 domainsList        = new String[] { "filenium.com", "ams.filenium.com", "lon.filenium.com" };
    private final String                                   domains            = "domains";
    private String                                         SELECTEDDOMAIN     = domainsList[0];
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            DLFAILED           = "<title>Error: Cannot get access at this time, check your link or advise us of this error to fix\\.</title>";

    public Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie("http://filenium.com", "langid", "1");
        br.getHeaders().put("Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            LinkedList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                logger.info("No account present, Please add a premium account.");
                for (DownloadLink dl : urls) {
                    /* no check possible */
                    dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                }
                return false;
            }
            login(accs.get(0), false);
            br.setFollowRedirects(true);
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(dl.getDownloadURL());
                    if (con.isContentDisposition()) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setDownloadSize(con.getLongContentLength());
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                } finally {
                    try {
                        /* make sure we close connection */
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        checkLinks(new DownloadLink[] { link });
        if (AvailableStatus.FALSE == getAvailableStatus(link)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        handleDL(link, link.getDownloadURL(), false, account);
    }

    private void handleDL(final DownloadLink link, String dllink, final boolean liveLink, final Account account) throws Exception {
        // Right now this errorhandling doesn't make that much sense but if the
        // connection limits change, it could help
        final boolean chunkError = link.getBooleanProperty("chunkerror", false);
        int maxChunks = 1;
        if (chunkError) maxChunks = 1;
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            if (!dl.startDownload()) {
                if (chunkError) {
                    // Wait and allow chunks again for the next try
                    link.setProperty("chunkerror", false);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                } else {
                    // Retry again without chunks
                    link.setProperty("chunkerror", true);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Possible chunk error");
                }
            }
            return;
        } else {
            final int responseCode = dl.getConnection().getResponseCode();
            if (liveLink == false && responseCode == 404) {
                /* link is a pre-generated one and 404 = file offline */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (responseCode == 503) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached!", 5 * 60 * 1000l);
            }
            /*
             * download is not contentdisposition, so remove this host from
             * premiumHosts list
             */
            br.followConnection();
            if (br.containsHTML(">Error: Al recuperar enlace\\. No disponible temporalmente, disculpa las molestias<")) tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            generalErrorhandling(link, account);
            // Seems like we're on the mainpage -> Traffic exhausted
            if (br.containsHTML("filenium\\.com/favicon\\.ico\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        login(acc, false);
        showMessage(link, "Task 1: Generating ink");
        String dllink = br.getPage("http://" + SELECTEDDOMAIN + "/?filenium&filez=" + Encoding.urlEncode(link.getDownloadURL()));
        generalErrorhandling(link, acc);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, dllink, true, acc);
    }

    private void generalErrorhandling(final DownloadLink link, final Account acc) throws PluginException {
        // This can either mean that it's a temporary error or that the hoster
        // should be deactivated
        if (br.containsHTML(DLFAILED)) {
            int timesFailed = link.getIntegerProperty("timesfailedfilenium", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedfilenium", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                link.setProperty("timesfailedfilenium", Property.NULL);
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(8);
        final String expire = br.getRegex("(?i)<expiration\\-txt>([^<]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", Locale.ENGLISH));
        }
        String acctype = br.getRegex("(?i)<type>(\\w+)</type>").getMatch(0);
        if (acctype != null) {
            ai.setStatus(acctype + " User");
        }
        String hostsSup = br.cloneBrowser().getPage("http://" + SELECTEDDOMAIN + "/jddomains");
        String[] hosts = new Regex(hostsSup, "\"([^\"]+)\",").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        supportedHosts.remove("vk.com");
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                SELECTEDDOMAIN = getConfiguredDomain();
                /** Load cookies */
                br = newBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://filenium.com", key, value);
                        }
                        return;
                    }
                }
                br.getPage("http://" + SELECTEDDOMAIN + "/checkuserjd?user=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()));
                final String lang = System.getProperty("user.language");
                if (br.getCookie("http://filenium.com", "secureid") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // if (!br.containsHTML("type>premium<")) {
                // if ("de".equalsIgnoreCase(lang)) {
                // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                // "\r\nDer eingegebene Account ist ein kostenloser Account.\r\nFür diesen Anbieter unterstützt JDownloader nur premium Accounts!",
                // PluginException.VALUE_ID_PREMIUM_DISABLE);
                // } else {
                // throw new PluginException(LinkStatus.ERROR_PREMIUM,
                // "\r\nPThe added account is a free account.\r\nFor this service, JDownloader only supports premium accounts!",
                // PluginException.VALUE_ID_PREMIUM_DISABLE);
                // }
                // }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://filenium.com");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private String getConfiguredDomain() {
        switch (getPluginConfig().getIntegerProperty(this.domains, -1)) {
        case 0:
            return domainsList[0];
        case 1:
            return domainsList[1];
        default:
            return domainsList[2];
        }
    }

    private void showMessage(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), domains, domainsList, JDL.L("plugins.host.FileniumCom.domains", "Use this domain:")).setDefaultValue(0));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}