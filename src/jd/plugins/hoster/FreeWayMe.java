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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FreeWayMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);

    public FreeWayMe(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(4 * 1000l);
        this.enablePremium("https://www.free-way.me/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.free-way.me/agb";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String page = null;
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        page = br.getPage("https://www.free-way.me/ajax/jd.php?id=1&user=" + username + "&pass=" + pass);
        // "Invalid login" / "Banned" / "Valid login"
        if (page.equalsIgnoreCase("Valid login")) {
            account.setValid(true);
        } else if (page.equalsIgnoreCase("Invalid login")) {
            ac.setStatus("invalid login. Wrong password?");
            account.setValid(false);
            return ac;
        } else if (page.equalsIgnoreCase("Banned")) {
            ac.setStatus("account banned");
            account.setValid(true);
            account.setEnabled(false);
            return ac;
        } else {
            // unknown error
            ac.setStatus("unknown account status");
            account.setValid(false);
            return ac;
        }
        // account should be valid now, let's get account information:
        page = br.getPage("https://www.free-way.me/ajax/jd.php?id=4&user=" + username);
        Long guthaben = Long.parseLong(getRegexTag(page, "guthaben").getMatch(0));
        ac.setTrafficLeft(guthaben * 1024 * 1024);
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        String accountType = getRegexTag(page, "premium").getMatch(0);
        ac.setValidUntil(-1);
        if (accountType.equalsIgnoreCase("Flatrate")) {
            ac.setUnlimitedTraffic();
            long validUntil = Long.parseLong(getRegexTag(page, "Flatrate").getMatch(0));
            ac.setValidUntil(validUntil * 1000);
        } else if (accountType.equalsIgnoreCase("Spender")) {
            ac.setUnlimitedTraffic();
        }
        // now let's get a list of all supported hosts:
        page = br.getPage("https://www.free-way.me/ajax/jd.php?id=3");
        hosts = br.getRegex("\"([^\"]*)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via free-way.me available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via free-way.me available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private Regex getRegexTag(String content, String tag) {
        return new Regex(content, "\"" + tag + "\":\"([^\"]*)\"");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        String dllink = "https://www.free-way.me/load.php?multiget=2&user=" + user + "&pw=" + pw + "&url=" + url;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String page = br.toString();
            String error = "";
            try {
                error = (new Regex(page, "<p id='error'>([^<]*)</p>")).getMatch(0);
            } catch (Exception e) {
                // we handle this few lines later
            }
            if (error.equalsIgnoreCase("Ungültiger Login")) {
                acc.setTempDisabled(true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (error.equalsIgnoreCase("Ungültige URL")) {
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Sie haben nicht genug Traffic, um diesen Download durchzuführen.")) {
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
            } else if (error.startsWith("Sie können nicht mehr parallele Downloads durchführen")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            } else if (error.startsWith("Ung&uuml;ltiger Hoster")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Dieser Hoster ist aktuell leider nicht aktiv.")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Diese Datei wurde nicht gefunden.")) {
                tempUnavailableHoster(acc, link, 1 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Es ist ein unbekannter Fehler aufgetreten (#1)")) {
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Retry in few secs" + msg, 20 * 1000l);
            } else if (error.startsWith("Die Datei darf maximal")) {
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l);
            }
            logger.info("Unhandled download error on free-way.me: " + page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
        dl.startDownload();
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