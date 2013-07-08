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
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridItaliaCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    // private static Object LOCK = new Object();
    // private static final String COOKIE_HOST = "http://debriditalia.com";

    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.debriditalia.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.debriditalia.com/index.php";
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
        ac.setUnlimitedTraffic();
        prepApiBr(br);
        if (!loginAPI(account)) {
            ac.setStatus("Account is invalid. Wrong username or password?");
            account.setValid(false);
            return ac;
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            ac.setStatus("Account is invalid. Invalid or unsupported accounttype!");
            account.setValid(false);
            return ac;
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l);

        // now let's get a list of all supported hosts:
        br.getPage("http://www.debriditalia.com/index.php");
        hosts = br.getRegex("\"/images/[A-Z0-9]{2}\\.png\" hspace=\"0\" vspace=\"\\d+\" onmouseover=\"tooltip\\.show\\(this\\)\" onmouseout=\"tooltip\\.hide\\(this\\)\" title=\"([^<>\"]*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            host = host.toLowerCase();
            if (host.equals("hotfile")) {
                supportedHosts.add("hotfile.com");
            } else if (host.equals("netload")) {
                supportedHosts.add("netload.in");
            } else if (host.equals("putlocker")) {
                supportedHosts.add("putlocker.com");
            } else if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
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

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via debriditalia.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via debriditalia.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {

        final String encodedLink = Encoding.urlEncode(link.getDownloadURL());
        /** Way without API */
        // br.postPage("http://www.debriditalia.com/downloader.php",
        // "fpass=&op=genera2&generalink=%3E%3E%3E+Get+premium+links+%3C%3C%3C&links=" + encodedLink);
        // br.postPage("http://www.debriditalia.com/linkgen2.php", "xjxfun=convertiLink&xjxr=" + System.currentTimeMillis() +
        // "&xjxargs[]=S%3C!%5BCDATA%5B" + encodedLink + "&xjxargs[]=S&xjxargs[]=Slink0&xjxargs[]=S&xjxargs[]=S");
        br.getPage("http://debriditalia.com/api.php?generate=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + Encoding.urlEncode(acc.getPass()) + "&link=" + encodedLink);
        if (br.containsHTML("ERROR: not_available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        final String dllink = br.getRegex("(https?://(\\w+\\.)?debriditalia\\.com/dl/.+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            logger.info("Unhandled download error on debriditalia.com: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    // @SuppressWarnings("unchecked")
    // private boolean login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
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
    // return true;
    // }
    // }
    // br.setFollowRedirects(true);
    // br.getPage("http://www.debriditalia.com/login.php?u=" + Encoding.urlEncode(account.getUser()) + "&p=" +
    // Encoding.urlEncode(account.getPass()) + "&sid=" + System.currentTimeMillis());
    // if (br.getCookie(COOKIE_HOST, "auth") == null) {
    // account.setValid(false);
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nFalscher Benutzername/Passwort!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // /** Save cookies */
    // final HashMap<String, String> cookies = new HashMap<String, String>();
    // final Cookies add = this.br.getCookies(COOKIE_HOST);
    // for (final Cookie c : add.getCookies()) {
    // cookies.put(c.getKey(), c.getValue());
    // }
    // account.setProperty("name", Encoding.urlEncode(account.getUser()));
    // account.setProperty("pass", Encoding.urlEncode(account.getPass()));
    // account.setProperty("cookies", cookies);
    // return true;
    // } catch (final PluginException e) {
    // account.setProperty("cookies", Property.NULL);
    // return false;
    // }
    // }
    // }

    private boolean loginAPI(final Account acc) throws IOException {
        br.getPage("http://debriditalia.com/api.php?check=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + Encoding.urlEncode(acc.getPass()));
        if (!br.containsHTML("<status>valid</status>")) return false;
        return true;
    }

    private void prepApiBr(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout) throws PluginException {
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
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