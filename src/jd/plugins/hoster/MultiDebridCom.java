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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multi-debrid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MultiDebridCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MultiDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multi-debrid.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.multi-debrid.com/terms";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(true);
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());
        String page = "";
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);

        // account is valid, let's fetch account details:
        page = br.getPage("http://multi-debrid.com/api.php?user=" + user + "&pass=" + pw);
        String status = "";
        try {
            status = (new Regex(page, "\"status\":\"([^\"]*)\"")).getMatch(0);
        } catch (Exception e) {
            // we will handle this later
        }
        if (!status.equalsIgnoreCase("ok")) {
            ac.setStatus("Account is invalid. Wrong password?");
            logger.severe("Multi-debrid.com: Error, can not parse left days. Account: " + account.getUser() + " Pass: " + account.getPass() + " API response: " + page);
            account.setValid(false);
            return ac;
        }
        try {
            String daysLeft = (new Regex(page, "\"days_left\":([0-9]*)")).getMatch(0);
            long validuntil = System.currentTimeMillis() + (Long.parseLong(daysLeft) * 1000 * 60 * 60 * 24);
            ac.setValidUntil(validuntil);
        } catch (Exception e) {
            ac.setStatus("can not get left days.");
            logger.severe("Multi-debrid.com: Error, can not parse left days. API response: " + page);
            account.setValid(false);
            return ac;
        }

        // now it's time to get all supported hosts
        page = br.getPage("http://multi-debrid.com/api.php?hosts");
        status = "";
        try {
            status = (new Regex(page, "\"status\":\"([^\"]*)\"")).getMatch(0);
        } catch (Exception e) {
            // we will handle this later
        }
        if (!status.equalsIgnoreCase("ok")) {
            ac.setStatus("can not get supported hosts");
            logger.severe("Multi-debrid.com: Error, can not parse supported hosts. API response: " + page);
            account.setValid(false);
            return ac;
        }
        page = page.replace(']', '>');
        page = page.replace('[', '<');
        String hostsString = (new Regex(page, "\"hosts\":<([^>]*)")).getMatch(0);
        hosts = (new Regex(hostsString, "\"([^\"]*)\"")).getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            for (String host : hosts) {
                if (host != null) {
                    supportedHosts.add(host.trim());
                }
            }
        }
        account.setValid(true);
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via multi-debrid.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via multi-debrid.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        String url = Encoding.urlEncode(link.getDownloadURL());
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());

        // generate downloadlink:
        showMessage(link, "Phase 1/2: Generate download link");
        br.setFollowRedirects(true);
        String page = br.getPage("http://multi-debrid.com/api.php?user=" + user + "&pass=" + pw + "&link=" + url);

        // parse json
        if (page.contains("Max atteint !")) {
            // max for this host reached, try again in 1h
            tempUnavailableHoster(account, link, 60 * 60 * 1000l);
        }

        if (page.contains("nvalidlink")) {
            // error
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                // disable hoster for 20min
                tempUnavailableHoster(account, link, 20 * 60 * 1000l);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            logger.severe("multi-debrid: invalid response: " + br);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }

        String dllink = "";
        try {
            dllink = (new Regex(page, "\"link\":\"([^\"]*)")).getMatch(0);
            dllink = dllink.replaceAll("\\\\/", "/");
        } catch (Exception e) {
            // handle this few lines later
        }

        if (!(dllink.startsWith("http://") || dllink.startsWith("https://"))) {
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                // disable hoster for 20min
                tempUnavailableHoster(account, link, 20 * 60 * 1000l);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            logger.severe("multi-debrid: invalid unrestricted link: " + br);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        showMessage(link, "Phase 2/2: Download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            logger.severe("Multi-debrid(Error): " + dllink);
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* disable hoster for 1h */
                tempUnavailableHoster(account, link, 60 * 60 * 1000);
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            showMessage(link, msg);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
        }
        dl.startDownload();

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
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