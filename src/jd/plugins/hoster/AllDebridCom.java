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
import java.util.Locale;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "alldebrid.com" }, urls = { "http://[\\w\\.]*?alldebrid\\.(com|de)/" }, flags = { 2 })
public class AllDebridCom extends PluginForHost {

    private PluginForHost                                        plugin             = null;
    private static ArrayList<String>                             premiumHosts       = new ArrayList<String>();
    private static final Object                                  LOCK               = new Object();

    private static final HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.alldebrid.com/");
    }

    private void resetAvailablePremium() {
        synchronized (LOCK) {
            premiumHosts.clear();
            hostUnavailableMap.clear();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            HashMap<String, String> accDetails = new HashMap<String, String>();
            AccountInfo ac = new AccountInfo();
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            String page = null;
            String hosts = null;
            try {
                page = br.getPage("http://www.alldebrid.com/api.php?action=info_user&login=" + username + "&pw=" + pass);
                hosts = br.getPage("http://www.alldebrid.com/api.php?action=get_host");
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("AllDebrid Server Error, temp disabled");
                return ac;
            }
            /* parse api response in easy2handle hashmap */
            String info[][] = new Regex(page, "<([^<>]*?)>([^<]*?)</.*?>").getMatches();

            for (String data[] : info) {
                accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
            }
            String type = accDetails.get("type");
            if ("premium".equals(type)) {
                /* only platinium and premium support */
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (hosts != null) {
                        String hoster[] = new Regex(hosts, "\"(.*?)\"").getColumn(0);
                        if (hosts != null) {
                            for (String host : hoster) {
                                if (hosts == null || host.length() == 0) continue;
                                host = host.trim();
                                try {
                                    if (host.equals("rapidshare.com") && accDetails.get("limite_rs") != null && Integer.parseInt(accDetails.get("limite_rs")) == 0) continue;
                                } catch (final Throwable e) {
                                    logger.severe(e.toString());
                                }
                                try {
                                    if (host.equals("depositfiles.com") && accDetails.get("limite_dp") != null && Integer.parseInt(accDetails.get("limite_dp")) == 0) continue;
                                } catch (final Throwable e) {
                                    logger.severe(e.toString());
                                }
                                try {
                                    if (host.equals("filefactory.com") && accDetails.get("limite_ff") != null && Integer.parseInt(accDetails.get("limite_ff")) == 0) continue;
                                } catch (final Throwable e) {
                                    logger.severe(e.toString());
                                }
                                try {
                                    if (host.equals("hotfile.com") && accDetails.get("limite_hf") != null && Integer.parseInt(accDetails.get("limite_hf")) == 0) continue;
                                } catch (final Throwable e) {
                                    logger.severe(e.toString());
                                }
                                try {
                                    if (host.equals("filesmonster.com") && accDetails.get("limite_fm") != null && Integer.parseInt(accDetails.get("limite_fm")) == 0) continue;
                                } catch (final Throwable e) {
                                    logger.severe(e.toString());
                                }
                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                }
                String daysLeft = accDetails.get("date");
                if (daysLeft != null) {
                    account.setValid(true);
                    long validuntil = System.currentTimeMillis() + (Long.parseLong(daysLeft) * 1000 * 60 * 60 * 24);
                    ac.setValidUntil(validuntil);
                } else {
                    /* no daysleft available?! */
                    account.setValid(false);
                }
            } else {
                /* all others are invalid */
                account.setValid(false);
            }
            if (account.isValid()) {
                if (premiumHosts.size() == 0) {
                    ac.setStatus("Account valid: 0 Hosts via alldebrid.com available");
                } else {
                    ac.setStatus("Account valid: " + premiumHosts.size() + " Hosts via alldebrid.com available");
                }
            } else {
                account.setTempDisabled(false);
                account.setValid(false);
                resetAvailablePremium();
                ac.setStatus("Account invalid");
            }
            ac.setProperty("multiHostSupport", premiumHosts);
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    @Override
    public String getAGBLink() {
        return "http://www.alldebrid.com/tos/";
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.alldebrid.com/buy/";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        showMessage(link, "Phase 1/2: Generating Link");
        String genlink = br.getPage("http://www.alldebrid.com/service.php?pseudo=" + user + "&password=" + pw + "&link=" + url + "&view=1");
        if (!genlink.startsWith("http://")) {
            logger.severe("AllDebrid(Error): " + genlink);
            if (genlink.contains("_limit")) {
                try {
                    /* limit reached for this host, wait 4h */
                    tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
                } catch (Exception e) {
                }
            }
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                try {
                    // disable hoster for 30min
                    tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                } catch (Exception e) {
                }
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("AllDebrid(Error): " + br.toString());
            // disable hoster for 5min
            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        /* save generated link */
        link.setProperty("genLinkAllDebrid", genlink);
        showMessage(link, "Phase 2/2: Download begins!");
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void reset() {
        if (plugin != null) {
            plugin.reset();
        }
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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

}