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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium4.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd5654" }, flags = { 2 })
public class Premium4Me extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    private static HashMap<String, Integer>                connectionLimits   = new HashMap<String, Integer>();

    private static AtomicBoolean                           shareOnlineLocked  = new AtomicBoolean(false);

    public Premium4Me(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000L);
        this.enablePremium("http://premium4.me/");
        /* limit connections for share-online to one */
        connectionLimits.put("share-online.biz", 1);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        // String page = null;
        String hosts = null;
        String traffic = null;
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        try {
            br.postPageRaw("http://premium4.me/login.php", "{\"u\":\"" + username + "\", \"p\":\"" + pass + "\", \"r\":true}");
            if (br.getCookie("http://premium4.me", "auth") != null) {
                // page = br.getPage("http://premium4.me/account.php");
                traffic = br.getPage("http://premium4.me/traffic.php").trim() + " MB";
                hosts = br.getPage("http://premium4.me/hosters.php");
            }
        } catch (Exception e) {
            // account.setTempDisabled(true);
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("invalid login. Wrong password?");
            return ac;
        }
        if (br.getCookie("http://premium4.me", "auth") == null) {
            ac.setProperty("multiHostSupport", Property.NULL);
            account.setValid(false);
            return ac;
        }
        ac.setTrafficLeft(traffic);
        // String date = new Regex(page, "\"d\":\"(.*?)\",").getMatch(0);
        account.setValid(true);
        /* expire date does currently not work */
        // ac.setValidUntil(TimeFormatter.getMilliSeconds(date,
        // "dd MMM yyyy", null));
        ArrayList<String> supportedHosts = new ArrayList<String>();
        String hoster[] = new Regex(hosts.trim(), "(.+?)(;|$)").getColumn(0);
        if (hoster != null) {
            for (String host : hoster) {
                if (hosts == null || host.length() == 0) continue;
                supportedHosts.add(host.trim());
            }
        }
        if (account.isValid()) {
            if (supportedHosts.size() == 0) {
                ac.setProperty("multiHostSupport", Property.NULL);
                ac.setStatus("Account invalid");
            } else {
                ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via premium4.me available");
                ac.setProperty("multiHostSupport", supportedHosts);
            }
        } else {
            account.setTempDisabled(false);
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("Account invalid");
        }
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://premium4.me/";
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        boolean dofollow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.setConnectTimeout(90 * 1000);
            br.setReadTimeout(90 * 1000);
            br.setDebug(true);
            dl = null;
            String user = Encoding.urlEncode(acc.getUser());
            String pw = Encoding.urlEncode(acc.getPass());
            String url = link.getDownloadURL().replaceFirst("https?://", "");

            /* begin code from premium4me support */
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
            }
            /* end code from premium4me support */

            url = Encoding.urlEncode(url);
            showMessage(link, "Phase 1/3: Login...");
            br.postPageRaw("http://premium4.me/login.php", "{\"u\":\"" + user + "\", \"p\":\"" + pw + "\", \"r\":true}");
            if (br.getCookie("http://premium4.me", "auth") == null) {
                resetAvailablePremium(acc);
                acc.setValid(false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            br.setFollowRedirects(true);
            showMessage(link, "Phase 2/3: Get link");
            int connections = getConnections(link.getHost());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://premium4.me/getfile.php?link=" + url, true, connections);
            if (dl.getConnection().getResponseCode() == 404) {
                /* file offline */
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                logger.severe("Premium4Me(Error): " + br.toString());
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    /* disable hoster for 1h */
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000);
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                showMessage(link, msg);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
            }
            showMessage(link, "Phase 3/3: Download...");
            dl.startDownload();
        } finally {
            br.setFollowRedirects(dofollow);
            if (link.getHost().equals("share-online.biz")) {
                shareOnlineLocked.set(false);
            }
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
        if (downloadLink.getHost().equals("share-online.biz")) {
            if (shareOnlineLocked.get()) { return false; }
            shareOnlineLocked.set(true);
        }
        return true;
    }

    @Override
    public void reset() {
    }

    private void resetAvailablePremium(Account ac) {
        ac.setProperty("multiHostSupport", Property.NULL);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private int getConnections(String host) {
        if (connectionLimits.containsKey(host)) { return connectionLimits.get(host); }
        // default is up to 10 connections
        return -10;
    }

}