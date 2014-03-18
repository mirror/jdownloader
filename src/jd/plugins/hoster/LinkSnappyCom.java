//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class LinkSnappyCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public LinkSnappyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.linksnappy.com/members/index.php?act=register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.linksnappy.com/index.php?act=tos";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return 20;
    }

    private DownloadLink currentLink = null;
    private Account      currentAcc  = null;

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String hosts[] = null;
        final String lang = System.getProperty("user.language");
        ac.setProperty("multiHostSupport", Property.NULL);
        try {
            if (!login(account)) {
                ac.setStatus("Account is invalid. Wrong username or password?");
                account.setValid(false);
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin Server-Fehler!\r\nBitte versuche es später erneut!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin server-error!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            }
        }
        String accountType = null;
        final String expire = br.getRegex("\"expire\":\"([^<>\"]*?)\"").getMatch(0);
        if ("lifetime".equals(expire)) {
            accountType = "Lifetime Premium Account";
        } else {
            ac.setValidUntil(Long.parseLong(expire) * 1000);
            accountType = "Premium Account";
        }

        /* now it's time to get all supported hosts */
        ArrayList<String> supportedHosts = new ArrayList<String>();
        getPageSecure("http://gen.linksnappy.com/lseAPI.php?act=FILEHOSTS&username=" + account.getUser() + "&password=" + JDHash.getMD5(account.getPass()));
        if (br.containsHTML("\"error\":\"Account has exceeded")) {
            logger.info("Daily limit reached");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nTageslimit erreicht!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDaily limit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
        final String hostText = br.getRegex("\\{\"status\":\"OK\",\"error\":false,\"return\":\\{(.*?\\})\\}\\}").getMatch(0);
        hosts = hostText.split("\\},");
        for (final String hostInfo : hosts) {
            final String host = new Regex(hostInfo, "\"([^<>\"]*?)\":\\{\"Status\":\"1\"").getMatch(0);
            if (host != null) supportedHosts.add(host);
        }
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
        account.setValid(true);
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", supportedHosts);
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        currentLink = link;
        currentAcc = account;
        br.setFollowRedirects(true);

        for (int i = 1; i <= 10; i++) {
            getPageSecure("http://gen.linksnappy.com/genAPI.php?genLinks=" + encode("{\"link\"+:+\"" + link.getDownloadURL() + "\",+\"username\"+:+\"" + account.getUser() + "\",+\"password\"+:+\"" + account.getPass() + "\"}"));
            if (br.containsHTML("\"error\":\"Invalid file URL format\\.\"")) {
                logger.info("Linksnappy.com: disabling current host");
                tempUnavailableHoster(account, link, 60 * 60 * 1000);
            }
            // Bullshit, we just try again
            if (br.containsHTML("\"error\":\"File not found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 1000l);
            String dllink = br.getRegex("\"generated\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                logger.info("linksnappy.com: Direct downloadlink not found");
                int timesFailed = link.getIntegerProperty("timesfailedlinksnappycom_dllinkmissing", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailedlinksnappycom_dllinkmissing", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error - final downloadlink not found");
                } else {
                    logger.info("linksnappy.com: Direct downloadlink not found -> Disabling current host");
                    link.setProperty("timesfailedlinksnappycom_dllinkmissing", Property.NULL);
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            dllink = dllink.replace("\\", "");
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            } catch (final SocketTimeoutException e) {
                final boolean timeoutedBefore = link.getBooleanProperty("sockettimeout");
                if (timeoutedBefore) {
                    link.setProperty("sockettimeout", false);
                    throw e;
                }
                link.setProperty("sockettimeout", true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (dl.getConnection().getResponseCode() == 503) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable e) {
                }
                logger.info("Try " + i + ": Got 503 error for link: " + dllink);
                continue;
            }
        }
        if (dl.getConnection().getResponseCode() == 503) stupidServerError();
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("linksnappy.com: Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedlinksnappycom_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedlinksnappycom_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("linksnappy.com: Unknown download error -> Disabling current host");
                link.setProperty("timesfailedlinksnappycom_unknowndlerror", Property.NULL);
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private String encode(String value) {
        value = value.replace("\"", "%22");
        value = value.replace(":", "%3A");
        value = value.replace("{", "%7B");
        value = value.replace("}", "%7D");
        value = value.replace(",", "%2C");
        return value;
    }

    private void getPageSecure(final String page) throws IOException, PluginException {
        boolean failed = true;
        for (int i = 1; i <= 10; i++) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(page);
                if (con.getResponseCode() == 503) {
                    logger.info("Try " + i + ": Got 503 error for link: " + page);
                    continue;
                }
                br.followConnection();
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            failed = false;
            break;
        }
        if (failed) stupidServerError();
    }

    private void postPageSecure(final String page, final String postData) throws IOException, PluginException {
        boolean failed = true;
        for (int i = 1; i <= 10; i++) {
            URLConnectionAdapter con = null;
            try {
                con = br.openPostConnection(page, postData);
                if (con.getResponseCode() == 503) {
                    logger.info("Try " + i + ": Got 503 error for link: " + page);
                    continue;
                }
                br.followConnection();
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            failed = false;
            break;
        }
        if (failed) stupidServerError();
    }

    // Max 10 retries via link, 5 seconds waittime between = max 2 minutes trying -> Then deactivate host
    private void stupidServerError() throws PluginException {
        // it's only null on login
        if (currentLink == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
        int timesFailed = currentLink.getIntegerProperty("timesfailedlinksnappy", 0);
        if (timesFailed <= 9) {
            timesFailed++;
            currentLink.setProperty("timesfailedlinksnappy", timesFailed);
            // Only wait 10 seconds because without forcing it, these servers will always bring up errors
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
        } else {
            currentLink.setProperty("timesfailedlinksnappy", Property.NULL);
            tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000l);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @SuppressWarnings("unchecked")
    private boolean login(final Account account) throws Exception {
        /** Load cookies */
        br.setCookiesExclusive(true);
        getPageSecure("http://gen.linksnappy.com/lseAPI.php?act=USERDETAILS&username=" + account.getUser() + "&password=" + JDHash.getMD5(account.getPass()));
        if (br.containsHTML("\"status\":\"ERROR\"")) return false;
        return true;
    }

    /** Not needed anymore but is still working */
    // @SuppressWarnings("unchecked")
    // private void loginSite(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // /** Load cookies */
    // br.setCookiesExclusive(true);
    // final Object ret = account.getProperty("cookies", null);
    // boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name",
    // Encoding.urlEncode(account.getUser())));
    // if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass",
    // Encoding.urlEncode(account.getPass())));
    // if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
    // final HashMap<String, String> cookies = (HashMap<String, String>) ret;
    // if (account.isValid()) {
    // for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
    // final String key = cookieEntry.getKey();
    // final String value = cookieEntry.getValue();
    // this.br.setCookie(COOKIE_HOST, key, value);
    // }
    // return;
    // }
    // }
    // postPageSecure("http://www.linksnappy.com/members/index.php?act=login", "username=" + Encoding.urlEncode(account.getUser()) +
    // "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
    //
    // /** Save cookies */
    // final HashMap<String, String> cookies = new HashMap<String, String>();
    // final Cookies add = this.br.getCookies(COOKIE_HOST);
    // for (final Cookie c : add.getCookies()) {
    // cookies.put(c.getKey(), c.getValue());
    // }
    // account.setProperty("name", Encoding.urlEncode(account.getUser()));
    // account.setProperty("pass", Encoding.urlEncode(account.getPass()));
    // account.setProperty("cookies", cookies);
    // }
    // }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}