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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multihosters.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MultihostersCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MultihostersCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(4 * 1000l);
        this.enablePremium("http://www.multihosters.com/Members/SelectProduct.aspx");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        logger.info("Multihosters: Accountinfo called");
        AccountInfo ac = new AccountInfo();
        ac.setProperty("multiHostSupport", Property.NULL);
        final boolean follow = br.isFollowingRedirects();
        br.setFollowRedirects(true);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        String loginPage = null;
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        long trafficLeft = -1;
        String hosts = null;
        try {
            loginPage = br.getPage("http://www.multihosters.com/jDownloader.ashx?cmd=accountinfo&login=" + username + "&pass=" + pass);
            String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);

            String EndSubscriptionDate = new Regex(infos[1], "EndSubscriptionDate:(.+)").getMatch(0);
            ac.setValidUntil(TimeFormatter.getMilliSeconds(EndSubscriptionDate, "yyyy/MM/dd HH:mm:ss", null));

            String AvailableTodayTraffic = new Regex(infos[3], "AvailableTodayTraffic:(.+)").getMatch(0);
            logger.info("Multihosters: AvailableTodayTraffic=" + AvailableTodayTraffic);
            if (AvailableTodayTraffic.equals("0")) {
                ac.setUnlimitedTraffic();
            } else {
                ac.setTrafficLeft(SizeFormatter.getSize(AvailableTodayTraffic + "mb"));
            }

            if (ac.isExpired()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            trafficLeft = Long.parseLong(AvailableTodayTraffic);
            hosts = br.getPage("http://www.multihosters.com/jDownloader.ashx?cmd=gethosters");

        } catch (Exception e) {
            ac.setStatus("Account is invalid. Wrong password?");
            account.setValid(false);
            return ac;
        } finally {
            br.setFollowRedirects(follow);
        }
        if (loginPage == null || trafficLeft < 0) {
            account.setValid(false);
            account.setTempDisabled(false);
            ac.setStatus("Account invalid");
        } else {

            ArrayList<String> supportedHosts = new ArrayList<String>();
            if (!"0".equals(hosts.trim())) {
                String hoster[] = new Regex(hosts, "(.*?)(,|$)").getColumn(0);
                if (hoster != null) {
                    for (String host : hoster) {
                        if (host == null || host.length() == 0) continue;
                        supportedHosts.add(host.trim());
                    }
                }
            }
            account.setValid(true);
            if (supportedHosts.size() == 0) {
                ac.setStatus("Account valid: 0 Hosts via multihosters.com available");
            } else {
                ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via multihosters.com available");
                ac.setProperty("multiHostSupport", supportedHosts);
            }
        }
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://multihosters.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return 6;
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
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        showMessage(link, "Phase 1/1: Get Link and begin Download");
        String dllink = "http://www.multihosters.com/jDownloader.ashx?cmd=generatedownloaddirect&login=" + user + "&pass=" + pw + "&olink=" + url + "&FilePass=";

        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);

        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        }
        /*
         * download is not contentdisposition, so remove this host from premiumHosts list
         */
        br.followConnection();
        /* temp disabled the host */
        if (br.containsHTML("No trafic")) {
            // account has no traffic, disable hoster for 1h
            tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("Error")) {
            // stupid error msg
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 2) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
            }
            String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 2 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 15 * 1000l);
        } else {
            // sth else, unknown error
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 2) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
            }
            String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 2 + ")";

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 15 * 1000l);
        }

        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

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