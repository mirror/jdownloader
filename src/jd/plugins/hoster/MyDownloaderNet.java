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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mydownloader.net" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MyDownloaderNet extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MyDownloaderNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://mydownloader.net/buy_credits.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://mydownloader.net/privacy.php";
    }

    private static final String USE_API     = "USE_API";
    private static final String NOCHUNKS    = "NOCHUNKS";
    private static Object       LOCK        = new Object();
    private static final String COOKIE_HOST = "http://mydownloader.net/";

    private String              token       = null;
    private DownloadLink        currentLink = null;
    private Account             currentAcc  = null;

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.setConstants(account, null);
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        if (api_active()) {
            ac = api_fetchAccountInfo();
        } else {
            ac = site_fetchAccountInfo();
        }
        return ac;
    }

    @SuppressWarnings("deprecation")
    private AccountInfo api_fetchAccountInfo() throws Exception {
        final AccountInfo ac = new AccountInfo();
        try {
            /* login */
            token = api_login_getLoginToken(currentAcc);
        } catch (final Exception e) {
            currentAcc.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("\r\nCan't get login token. Wrong password?");
            return ac;
        }
        /* get account info */
        try {
            br.getPage("http://api.mydownloader.net/api.php?task=user_info&auth=" + token);
        } catch (Exception e) {
            currentAcc.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("Mydownloader.net server error.");
            return ac;
        }
        long traffic = (long) (Float.parseFloat(getXMLTag(br.toString(), "remaining_limit_mb").getMatch(0)) + 0.5f);
        ac.setTrafficLeft(traffic * 1024 * 1024l);
        String expire = getXMLTag(br.toString(), "experation_date").getMatch(0);
        if (!expire.equals("lifetime")) {
            if (expire.equalsIgnoreCase("trial")) {
                ac.setStatus("Trial");
                ac.setValidUntil(-1);
            } else if (expire.equalsIgnoreCase("expired")) {
                ac.setExpired(true);
                ac.setStatus("\r\nAccont expired!\r\nAccount abgelaufen!");
                /* Workaround for bug: http://svn.jdownloader.org/issues/11637 */
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount abgelaufen!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount expired!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
            }
        }
        ac.setStatus("Account valid");
        // get supported hoster
        br.getPage("http://api.mydownloader.net/api.php?task=supported_list&auth=" + token);
        String[] hosters = getXMLTag(br.toString(), "values").getColumn(0);
        if (hosters == null) {
            currentAcc.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            return ac;
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosters) {
            supportedHosts.add(host.trim());
        }
        ac.setMultiHostSupport(supportedHosts);
        return ac;
    }

    private AccountInfo site_fetchAccountInfo() throws Exception {
        final AccountInfo ac = new AccountInfo();
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final String lang = System.getProperty("user.language");
        try {
            if (!site_login(currentAcc, true)) {
                ac.setStatus("Account is invalid. Wrong username or password?!");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } catch (final BrowserException e) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin Server-Fehler!\r\nBitte versuche es später erneut!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin server-error!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
        ac.setStatus("Premium Account");
        final String expire_Date = br.getRegex("\"/buy_credits\\.php\">([^<>\"]*?)</a>").getMatch(0);
        if (expire_Date != null) {
            ac.setValidUntil(TimeFormatter.getMilliSeconds(expire_Date, "yyyy.MM.dd", Locale.ENGLISH));
        }
        final String trafficleft = br.getRegex(": <strong>(\\d+ Mb)</strong>").getMatch(0);
        if (trafficleft != null) {
            ac.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        } else {
            ac.setUnlimitedTraffic();
        }
        /* now it's time to get all supported hosts */
        // present on most pages
        String[] hosts = br.getRegex("title=\"Works properly\">[\t\n\r ]+<i class=\"fh fh\\-[a-z0-9]+\"></i>([^<>\"]*?)</li>").getColumn(0);
        if (hosts != null && hosts.length != 0) {
            for (final String host : hosts) {
                supportedHosts.add(host.trim());
            }
        }
        ac.setMultiHostSupport(supportedHosts);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /* no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        this.setConstants(acc, link);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String dllink = checkDirectLink(link, "mydownloadernetdirectlink");
        if (dllink == null) {
            if (this.api_active()) {
                dllink = this.api_getdllink();
            } else {
                dllink = this.site_getdllink();
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("MyDownloader(Error): " + br.toString());
            // disable hoster for 5min
            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
        }
        showMessage(link, "Phase 4/4: Begin download");
        link.setProperty("mydownloadernetdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(MyDownloaderNet.NOCHUNKS, false) == false) {
                    link.setProperty(MyDownloaderNet.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(MyDownloaderNet.NOCHUNKS, false) == false) {
                link.setProperty(MyDownloaderNet.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String api_getdllink() throws PluginException, IOException {
        showMessage(this.currentLink, "Phase 1/4: Login");
        String auth = api_login_getLoginToken(this.currentAcc);
        if (auth.isEmpty()) {
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (this.currentLink.getLinkStatus().getRetryCount() >= 3) {
                try {
                    // disable hoster for 30min
                    tempUnavailableHoster(this.currentAcc, this.currentLink, 30 * 60 * 1000l);
                } catch (Exception e) {
                }
                /* reset retrycounter */
                this.currentLink.getLinkStatus().setRetryCount(0);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            String msg = "(" + this.currentLink.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }

        String url = Encoding.urlEncode(this.currentLink.getDownloadURL());

        showMessage(this.currentLink, "Phase 2/4: Add Link");
        getPageSafe("http://api.mydownloader.net/api.php?task=add_url&auth=" + auth + "&url=" + url);
        if (getXMLTag(br.toString(), "url_error").getMatch(0) != null && !getXMLTag(br.toString(), "url_error").getMatch(0).isEmpty()) {
            // file already added
            // tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
        } else if (!getXMLTag(br.toString(), "fid").getMatch(0).isEmpty()) {
            url = getXMLTag(br.toString(), "fid").getMatch(0).trim(); // we can use fileid instead url if given
        }
        sleep(2 * 1000l, this.currentLink);

        int maxCount = 10;
        String genlink = "";
        String status = "";
        // wait until mydownloader has loaded file on their server
        while (genlink.isEmpty() && (maxCount > 0)) {
            br.getPage("http://api.mydownloader.net/api.php?task=file_info&auth=" + auth + "&file=" + url);
            status = getXMLTag(br.toString(), "status").getMatch(0);
            if (status.equalsIgnoreCase("new")) {
                // File was just added to our system
                // or
                sleep(10 * 1000l, this.currentLink, "Phase 3/4: File was just added to mydownloader system");
            } else if (status.equalsIgnoreCase("download")) {
                // File uploading to our servers was started
                sleep(10 * 1000l, this.currentLink, "Phase 3/4: File uploading to mydownloader servers was started");
            } else if (status.equalsIgnoreCase("turn")) {
                // File in queue of uploading
                sleep(15 * 1000l, this.currentLink, "Phase 3/4: File in queue of uploading to mydownloader");
            } else if (status.equalsIgnoreCase("mturn")) {
                // File in user type of queue, it happens when user has exceeded daily limit, file stands in user type of queue and
                // automatically starts when new limit will be added
                // disable for 1h
                tempUnavailableHoster(this.currentAcc, this.currentLink, 60 * 60 * 1000l);
            } else if (status.equalsIgnoreCase("download_ok") || status.equalsIgnoreCase("ok")) {
                // File is uploading to our servers and you can start downloading to your computer.
                // or: File is fully uploaded to our servers
                genlink = getXMLTag(br.toString(), "download_link").getMatch(0).trim();
                if (genlink.isEmpty()) {
                    if (this.currentLink.getLinkStatus().getRetryCount() >= 3) {
                        try {
                            // disable hoster for 30min
                            tempUnavailableHoster(this.currentAcc, this.currentLink, 30 * 60 * 1000l);
                        } catch (Exception e) {
                        }
                        /* reset retrycounter */
                        this.currentLink.getLinkStatus().setRetryCount(0);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    }
                    String msg = "(" + this.currentLink.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
                }
                genlink = "http://" + genlink;
            } else if (status.equalsIgnoreCase("received")) {
                // File is downloading by user to his computer
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if (status.equalsIgnoreCase("received_ok")) {
                // User file downloading completed
                // delete file and retry
                br.getPage("http://api.mydownloader.net/api.php?task=delete_file&auth=" + auth + "&file=" + url);
                throw new PluginException(LinkStatus.ERROR_RETRY, "File need to be deleted from mydownloader system first.", 1000l);

            } else if (status.equalsIgnoreCase("del")) {
                // File was deleted from our servers
                if (this.currentLink.getLinkStatus().getRetryCount() >= 3) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(this.currentAcc, this.currentLink, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    this.currentLink.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + this.currentLink.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            } else if (status.equalsIgnoreCase("err")) {
                // Error while file downloading
                if (this.currentLink.getLinkStatus().getRetryCount() >= 3) {
                    try {
                        // disable hoster for 30min
                        tempUnavailableHoster(this.currentAcc, this.currentLink, 30 * 60 * 1000l);
                    } catch (Exception e) {
                    }
                    /* reset retrycounter */
                    this.currentLink.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                String msg = "(" + this.currentLink.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            } else if (status.equalsIgnoreCase("efo")) {
                showMessage(this.currentLink, "Error: " + getXMLTag(br.toString(), "error").getMatch(0));
                tempUnavailableHoster(this.currentAcc, this.currentLink, 5 * 60 * 1000l);
            }
            maxCount--;
        }
        return genlink;
    }

    private String site_getdllink() throws Exception {
        showMessage(this.currentLink, "Phase 1/2: Start download to mydownloader");
        this.site_login(this.currentAcc, false);
        final String url_urlencoded = Encoding.urlEncode(this.currentLink.getDownloadURL());
        String url_plain = this.currentLink.getDownloadURL();
        if (url_plain.contains("keep2share.cc/") || url_plain.contains("k2s.cc/")) {
            url_plain = url_plain.replace("keep2share.cc/", "k2s.cc/");
            url_plain = url_plain.replace("https://", "http://");
        }
        br.postPage("http://mydownloader.net/downloads.php", "links=" + url_urlencoded);

        int maxCount = 18;
        String genlink = null;
        // wait until mydownloader has loaded file on their server
        while (genlink == null && (maxCount > 0)) {
            showMessage(this.currentLink, "Phase 2/2: Looking for downloadlink");
            this.sleep(10 * 1000, currentLink);
            br.getPage("http://mydownloader.net/downloads.php");
            final String[] tableentries = br.getRegex("<div class=\"finfo\" id=\"info_0\">(.*?)onclick=\"return control\\(\\'delete\\'").getColumn(0);
            if (tableentries != null && tableentries.length != 0) {
                for (final String table_entry : tableentries) {
                    if (table_entry.contains(url_plain)) {
                        genlink = new Regex(table_entry, "\"(http://mydownloader\\.net/file\\.php\\?file=[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
            maxCount--;
        }
        return genlink;
    }

    private void getPageSafe(final String url) throws IOException, PluginException {
        boolean failed = true;
        for (int i = 1; i <= 3; i++) {
            br.getPage(url);
            if (br.containsHTML("<error>AUTH_ERROR</error>")) {
                logger.info("Auth error: " + i + " of 3: Failed to get url: " + url);
                logger.info("--> Trying to get a new token");
                api_login_getLoginToken(this.currentAcc);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) {
            logger.info("Failed 3 times --> Temporarily disabling account");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
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
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        return true;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private String api_login_getLoginToken(final Account ac) throws IOException, PluginException {
        String user = Encoding.urlEncode(ac.getUser());
        String pw = Encoding.urlEncode(ac.getPass());
        String token = "";
        String page = br.getPage("http://api.mydownloader.net/api.php?task=auth&login=" + user + "&password=" + pw);
        if (br.containsHTML("too? many logins")) {
            logger.info("Server says 'too many logins' --> Temporarily disabling account");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        token = getXMLTag(page, "auth").getMatch(0);
        if (token.length() == 0) {
            String errormsg = getXMLTag(page, "error").getMatch(0);
            if (errormsg.equals("LOGIN_ERROR")) {
                throw new RuntimeException("Error: Wrong mydownloader.net password.");
            } else {
                throw new RuntimeException("Error: Unknown mydownloader login error.");
            }
        }
        return token;
    }

    @SuppressWarnings("unchecked")
    private boolean site_login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            br.setCookie(COOKIE_HOST, "lng", "en");
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) {
                acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            }
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(COOKIE_HOST, key, value);
                    }
                    return true;
                }
            }
            br.setFollowRedirects(true);
            br.postPage("http://mydownloader.net/members_login.php", "remember=on&act=login&email=" + Encoding.urlEncode(account.getUser()) + "&passw=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(COOKIE_HOST, "user_hash") == null) {
                return false;
            }

            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(COOKIE_HOST);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
            return true;
        }
    }

    private Regex getXMLTag(String xml, String tag) {
        return new Regex(xml, "<" + tag + ">([^<]*)</" + tag + ">");
    }

    private void setConstants(final Account account, final DownloadLink downloadLink) throws PluginException {
        if (downloadLink == null && account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (downloadLink != null || account != null) {
            currentLink = downloadLink;
            currentAcc = account;
        }
    }

    private boolean api_active() {
        return this.getPluginConfig().getBooleanProperty(USE_API, default_api);
    }

    private final boolean default_api = true;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_API, JDL.L("plugins.hoster.mydownloadernet.useAPI", "Use API (recommended)?")).setDefaultValue(default_api));
    }

}