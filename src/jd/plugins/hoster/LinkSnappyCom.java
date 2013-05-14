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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class LinkSnappyCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();
    private static final String                            COOKIE_HOST        = "http://linksnappy.com";

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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);

        login(account, true);
        getPageSecure("http://www.linksnappy.com/members/index.php?act=index");
        if (br.getCookie("http://www.linksnappy.com/", "lseSavePass") == null) {
            ac.setStatus("Account is invalid. Wrong password?");
            account.setValid(false);
            return ac;
        }
        final String expireDate = br.getRegex("<strong>Expire Date:</strong> ([^<>\"]*?) \\(\\d+ days left\\)").getMatch(0);
        if (expireDate == null) {
            ac.setStatus("Account is invalid. Unsupported accounttype?!");
            account.setValid(false);
            return ac;
        }
        // we have a valid premium account - let's check the expire date:
        ac.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "dd MMMM yyyy", Locale.ENGLISH));

        // now it's time to get all supported hosts
        getPageSecure("http://www.linksnappy.com/index.php?act=download");
        hosts = br.getRegex("images/filehosts/small/([a-z0-9\\-\\.]+)\\.png\\) left no\\-repeat;\">OK</li>").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
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
        supportedHosts.remove("youtube.com");
        account.setValid(true);
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via linksnappy.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via linksnappy.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        login(account, false);

        // all ok, start downloading...
        br.setFollowRedirects(true);
        for (int i = 1; i <= 10; i++) {
            getPageSecure("http://gen.linksnappy.com/genAPI.php?callback=jQuery" + System.currentTimeMillis() + "_" + System.currentTimeMillis() + "&genLinks=%7B%22link%22+%3A+%22" + Encoding.urlEncode(link.getDownloadURL()) + "%22%2C+%22type%22+%3A+%22%22%2C+%22linkpass%22+%3A+%22%22%2C+%22fmt%22+%3A+%2235%22%2C+%22ytcountry%22+%3A+%22usa%22%7D&_=" + System.currentTimeMillis());
            String dllink = br.getRegex("\"generated\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 1000l);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
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
        if (failed) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 1000l);
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
        if (failed) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 1 * 60 * 1000l);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(COOKIE_HOST, key, value);
                    }
                    return;
                }
            }
            postPageSecure("http://www.linksnappy.com/members/index.php?act=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");

            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(COOKIE_HOST);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
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