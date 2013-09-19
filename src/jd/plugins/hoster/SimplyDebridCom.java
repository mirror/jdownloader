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
import java.util.Arrays;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "simply-debrid.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class SimplyDebridCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public SimplyDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://simply-debrid.com/buy.php");
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public String getAGBLink() {
        return "http://simply-debrid.com/privacy.php";
    }

    private boolean createSession(Account account) {
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());
        String page = "";
        try {
            page = br.getPage("http://simply-debrid.com/api.php?login=1&u=" + user + "&p=" + pw);
        } catch (Exception e) {
            return false;
        }
        return page.equalsIgnoreCase("02: loggin success");
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return 10;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());
        String page = "";
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        if (!createSession(account)) {
            // account is invalid
            ac.setStatus("account is invalid. Wrong password?");
            account.setValid(false);
            return ac;
        }
        // account is valid, let's fetch account details:
        page = br.getPage("http://simply-debrid.com/api.php?login=2&u=" + user + "&p=" + pw);
        String[] accInfo = page.split(";");
        if (!accInfo[0].equalsIgnoreCase("1")) {
            // account is not a premium account
            ac.setStatus("Account is not a premium account.");
            account.setValid(false);
            return ac;
        }
        // we have a valid premium account - let's check the expire date:
        ac.setValidUntil(TimeFormatter.getMilliSeconds(accInfo[2], "dd/MM/yyyy", null));

        // now it's time to get all supported hosts
        page = br.getPage("http://simply-debrid.com/api.php?list=1");
        hosts = (new Regex(page, "([^;]+)")).getColumn(0);
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
        account.setValid(true);
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via simply-debrid.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via simply-debrid.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        String url = Encoding.urlEncode(link.getDownloadURL());
        String page = "";
        showMessage(link, "Phase 1/3: Get a valid session");
        if (!createSession(account)) {
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* reset retrycounter */
                link.getLinkStatus().setRetryCount(0);
                // disable hoster for 15min
                tempUnavailableHoster(account, link, 15 * 60 * 1000l);
            }
            String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        // valid session, we can generate downloadlink:
        showMessage(link, "Phase 2/3: Generate download link");
        String dllink = null;
        try {
            dllink = br.getPage("http://simply-debrid.com/api.php?dl=" + url);
        } catch (Throwable e) {
            showMessage(link, "Server überlastet!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 1 * 60 * 1000l);
        }
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Unknown API error, disabling current host...");
            tempUnavailableHoster(account, link, 2 * 60 * 60 * 1000l);
        }

        // crazy API
        if (dllink.contains("Erreur")) dllink = new Regex(dllink, "(.*?)Erreur").getMatch(0);
        if (!(dllink.startsWith("http://") || dllink.startsWith("https://")) || dllink.endsWith("/Invalid link") || dllink.contains("php_network_getaddresses: getaddrinfo failed: Name or service not known")) {
            if (dllink.contains("UNDER MAINTENANCE")) {
                // disable host for 30min
                tempUnavailableHoster(account, link, 30 * 60 * 1000l);
            }
            if (dllink.contains("03: Invalid link") || dllink.endsWith("/Invalid link") || dllink.contains("php_network_getaddresses: getaddrinfo failed: Name or service not known")) {
                // link is invalid
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 3) {
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    // disable hoster for 20min
                    tempUnavailableHoster(account, link, 20 * 60 * 1000l);
                }
                String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
            }
            logger.info("Error parsing Simply-Debrid download response: " + page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        // all ok, start downloading...
        br.setFollowRedirects(true);
        int maxChunks = 0;
        if (link.getBooleanProperty(SimplyDebridCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.getPage(dllink);
            if (br.containsHTML("This error have been sent to our services this one is going to be fixed as soon as possible<")) {
                logger.info("Possible simply-debrid.com bug, NO JDownloader bug!");
                logger.info("Directlink: " + dllink);
            }
            if (br.containsHTML("php_network_getaddresses: getaddrinfo failed: No address associated with hostname")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            logger.severe("Simply-debrid(Error): " + dllink);
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
            String msg = "(" + (link.getLinkStatus().getRetryCount() + 1) + "/" + 3 + ")";
            showMessage(link, msg);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
        }
        showMessage(link, "Phase 3/3: Download...");
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            /* unknown error, we disable multiple chunks */
            if (link.getBooleanProperty(SimplyDebridCom.NOCHUNKS, false) == false) {
                link.setProperty(SimplyDebridCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
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