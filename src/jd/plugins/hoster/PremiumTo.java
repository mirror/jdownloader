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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.to" }, urls = { "https?://torrent\\.premium\\.to/(t|z)/[^<>/\"]+(/[^<>/\"]+){0,1}(/\\d+)*" }, flags = { 2 })
public class PremiumTo extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static HashMap<String, Integer>                connectionLimits   = new HashMap<String, Integer>();
    private static AtomicBoolean                           shareOnlineLocked  = new AtomicBoolean(false);
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static Object                                  LOCK               = new Object();
    private final String                                   lang               = System.getProperty("user.language");

    public PremiumTo(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000L);
        this.enablePremium("http://premium.to/");
        /* limit connections for share-online to one */
        connectionLimits.put("share-online.biz", 1);
        /* limit connections for keep2share to one */
        connectionLimits.put("keep2share.cc", 1);
        connectionLimits.put("k2s.cc", 1);
    }

    @Override
    public Boolean rewriteHost(DownloadLink link) {
        if (link != null && "premium4.me".equals(link.getHost())) {
            link.setHost(getHost());
            return true;
        }
        return false;
    }

    public Boolean rewriteHost(Account acc) {
        if (acc != null && "premium4.me".equals(acc.getHoster())) {
            acc.setHoster(getHost());
            return true;
        }
        return false;
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        prepBr.setAcceptLanguage("en, en-gb;q=0.8");
        prepBr.setConnectTimeout(90 * 1000);
        prepBr.setReadTimeout(90 * 1000);
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setAllowedResponseCodes(new int[] { 400 });
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setProperty("multiHostSupport", Property.NULL);
            account.setValid(false);
            throw e;
        }
        final String traffic = br.getPage("http://premium.to/traffic.php").trim() + " MB";
        final String hosts = br.getPage("http://premium.to/hosts.php");
        ac.setTrafficLeft(traffic);
        account.setValid(true);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        String hosters[] = new Regex(hosts.trim(), "(.+?)(;|$)").getColumn(0);
        if (hosters != null) {
            for (String hoster : hosters) {
                if (hoster == null || hoster.length() == 0) {
                    continue;
                }
                supportedHosts.add(hoster.trim());
            }
        }
        ac.setStatus("Premium Account");
        ac.setProperty("multiHostSupport", supportedHosts);
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://premium.to/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        String url = link.getDownloadURL();
        // allow resume and up to 10 chunks
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, -10);
        dl.startDownload();
    }

    private void login(Account account, boolean force) throws Exception {
        final boolean redirect = br.isFollowingRedirects();
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
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
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.postPageRaw("http://premium.to/login.php", "{\"u\":\"" + Encoding.urlEncode(account.getUser()) + "\", \"p\":\"" + Encoding.urlEncode(account.getPass()) + "\", \"r\":true}");
                if (br.getHttpConnection().getResponseCode() == 400) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.getCookie("premium.to", "auth") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(redirect);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        try {
            dl = null;
            String url = link.getDownloadURL().replaceFirst("https?://", "");

            // this here is bullshit... multihoster side should do all the corrections.
            /* begin code from premium.to support */
            if (url.startsWith("http://")) {
                url = url.substring(7);
            }
            if (url.startsWith("www.")) {
                url = url.substring(4);
            }
            if (url.startsWith("freakshare.com/")) {
                url = url.replaceFirst("freakshare.com/", "fs.com/");
            } else if (url.startsWith("depositfiles.com/")) {
                url = url.replaceFirst("depositfiles.com/", "df.com/");
            } else if (url.startsWith("netload.in/")) {
                url = url.replaceFirst("netload.in/", "nl.in/");
            } else if (url.startsWith("filepost.com/")) {
                url = url.replaceFirst("filepost.com/", "fp.com/");
            } else if (url.startsWith("extabit.com/")) {
                url = url.replaceFirst("extabit.com/", "eb.com/");
            } else if (url.startsWith("turbobit.net/")) {
                url = url.replaceFirst("turbobit.net/", "tb.net/");
            } else if (url.startsWith("filefactory.com/")) {
                url = url.replaceFirst("filefactory.com/", "ff.com/");
            } else if (url.startsWith("k2s.cc/")) {
                // doesn't work...
                // url = url.replaceFirst("k2s.cc/", "keep2share.cc/");
            }
            /* end code from premium.to support */
            if (url.startsWith("oboom.com/")) {
                url = url.replaceFirst("oboom.com/#", "oboom.com/");
            }
            url = Encoding.urlEncode(url);
            showMessage(link, "Phase 1/3: Login...");
            login(acc, false);
            showMessage(link, "Phase 2/3: Get link");

            int connections = getConnections(link.getHost());
            if (link.getChunks() != -1) {
                connections = link.getChunks();
            }
            if (link.getBooleanProperty(PremiumTo.NOCHUNKS, false)) {
                connections = 1;
            }

            dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://premium.to/getfile.php?link=" + url, true, connections);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                if (dl.getConnection().getResponseCode() == 420) {
                    int timesFailed = link.getIntegerProperty("timesfailedpremiumto_420dlerror", 0);
                    link.getLinkStatus().setRetryCount(0);
                    logger.info("premium.to: Download attempt failed because of server error 420");
                    if (timesFailed <= 10) {
                        timesFailed++;
                        link.setProperty("timesfailedpremiumto_420dlerror", timesFailed);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Download could not be started (420)");
                    } else {
                        link.setProperty("timesfailedpremiumto_420dlerror", Property.NULL);
                        logger.info("premium.to: 420 download error - disabling current host!");
                        tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                    }
                }
                br.followConnection();

                logger.severe("PremiumTo(Error): " + br.toString());
                if (br.containsHTML("File not found")) {
                    // probably a plugin error - we need to add a url fix (see above)
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Support defect: " + new URL(link.getDownloadURL()).getHost());
                }
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    /* disable hoster for 1h */
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000);
                    /* reset retry counter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 3 + ")";
                showMessage(link, msg);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
            }
            showMessage(link, "Phase 3/3: Download...");
            try {
                dl.startDownload();
            } catch (final PluginException ex) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PremiumTo.NOCHUNKS, false) == false) {
                    link.setProperty(PremiumTo.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } finally {
            if (link.getHost().equals("share-online.biz")) {
                shareOnlineLocked.set(false);
            }
        }
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {

        String dlink = Encoding.urlDecode(link.getDownloadURL(), true);

        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        long fileSize = -1;

        ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
        if (accs.size() == 0) {
            // try without login (only possible for links with token)
            try {
                con = br.openGetConnection(dlink);
                if (!con.getContentType().contains("html")) {
                    fileSize = con.getLongContentLength();
                    if (fileSize == -1) {
                        link.getLinkStatus().setStatusText("Only downlodable via account!");
                        return AvailableStatus.UNCHECKABLE;
                    }
                    String name = con.getHeaderField("Content-Disposition");
                    if (name != null) {
                        // filter the filename from content disposition and decode it...
                        name = new Regex(name, "filename.=UTF-8\'\'([^\"]+)").getMatch(0);
                        name = Encoding.UTF8Decode(name).replaceAll("%20", " ");
                        if (name != null) {
                            link.setFinalFileName(name);
                        }
                    }
                    link.setDownloadSize(fileSize);
                    return AvailableStatus.TRUE;
                } else {
                    return AvailableStatus.UNCHECKABLE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

        } else {
            // if accounts available try all whether the link belongs to it links with token should work anyway
            for (Account acc : accs) {
                login(acc, false);

                try {
                    con = br.openGetConnection(dlink);
                    if (!con.getContentType().contains("html")) {
                        fileSize = con.getLongContentLength();

                        if (fileSize > -1) {
                            link.setDownloadSize(fileSize);
                            String name = con.getHeaderField("Content-Disposition");

                            if (name != null) {
                                // filter the filename from content disposition and decode it...
                                name = new Regex(name, "filename.=UTF-8\'\'([^\"]+)").getMatch(0);
                                name = Encoding.UTF8Decode(name).replaceAll("%20", " ");
                                if (name != null) {
                                    link.setFinalFileName(name);
                                }
                            }
                            return AvailableStatus.TRUE;
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
            return AvailableStatus.UNCHECKABLE;
        }
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
        if (downloadLink.getHost().equals("share-online.biz")) {
            if (shareOnlineLocked.get()) {
                return false;
            }
            shareOnlineLocked.set(true);
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private int getConnections(String host) {
        if (connectionLimits.containsKey(host)) {
            return connectionLimits.get(host);
        }
        // default is up to 10 connections
        return -10;
    }

}