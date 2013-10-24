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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debrid-link.fr" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridLinkFr extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);

    public DebridLinkFr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://debrid-link.fr/?page=premium");
    }

    @Override
    public String getAGBLink() {
        return "http://debrid-link.fr/?page=mention";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        final String username = Encoding.urlEncode(account.getUser());
        final String pass = Encoding.urlEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("http://debrid-link.fr/api/?act=1&user=" + username + "&pass=" + pass);

        // Errorhandling
        int errorcode = 0;
        final String errorcodeString = getJson("ID");
        if (errorcodeString != null) errorcode = Integer.parseInt(getJson("ID"));
        switch (errorcode) {
        case 1:
            ac.setStatus("Account is banned");
            account.setValid(false);
            return ac;
        case 2:
            ac.setStatus("Account is banned until midnight");
            account.setValid(false);
            return ac;
        case 3:
            ac.setStatus("Username or password wrong");
            account.setValid(false);
            return ac;
        }
        if ("KO".equals(getJson("CONNECTED"))) {
            ac.setStatus("invalid login. Wrong password?");
            account.setValid(false);
            return ac;

        }

        // Differ between accounttypes
        final int statuscode = Integer.parseInt(getJson("ACCOUNT"));
        switch (statuscode) {
        case 0:
            ac.setStatus("Free Account (not supported)");
            account.setValid(false);
            return ac;
        case 1:
            ac.setStatus("Premium Account");
            account.setValid(true);
            ac.setValidUntil(System.currentTimeMillis() + Long.parseLong(getJson("PREMIUMLEFT")) * 1000);
            break;
        case 2:
            ac.setStatus("Lifetime Premium Account");
            account.setValid(true);
            break;
        }

        int maxChunks = Integer.parseInt(getJson("NB_CONNECT_PER_DOWNLOAD"));
        int maxOverallConnections = Integer.parseInt(getJson("NB_CONNECT_PER_SERVER"));
        int calculatedMaxDls = maxOverallConnections / maxChunks;
        // One download less than allowed to reduce server error risc
        calculatedMaxDls -= 1;
        maxPrem.set(calculatedMaxDls);
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }

        // now let's get a list of all supported hosts:
        br.getPage("http://debrid-link.fr/api/?act=1");
        final String hostArray = br.getRegex("\\{\"HOST\":\\[(.*?)\\]").getMatch(0);
        hosts = new Regex(hostArray, "\"([^\"]*)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                // Correct links as the array consists of regexes and sometimes only host names
                host = host.trim();
                if (host.contains("uploaded|")) {
                    supportedHosts.add("uploaded.to");
                } else if (host.contains("1fichier|")) {
                    supportedHosts.add("1fichier.com");
                } else if (host.contains("youtube.com")) {
                    // Don't add youtube
                    continue;
                } else if (host.equals("4shared")) {
                    supportedHosts.add("4shared.com");
                } else if (host.equals("extabit")) {
                    supportedHosts.add("extabit.com");
                } else if (host.equals("2shared")) {
                    supportedHosts.add("2shared.com");
                } else {
                    supportedHosts.add(host);
                }
            }
        }
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via debrid-link.fr available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via debrid-link.fr available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
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
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String user = Encoding.urlEncode(acc.getUser());
        final String pw = Encoding.urlEncode(acc.getPass());
        String url = link.getDownloadURL();
        if ("1fichier.com".equals(link.getHost()) && !url.endsWith("/")) {
            url = url + "/";
        }
        showMessage(link, "Phase 1/2: Generating link");
        br.getPage("http://debrid-link.fr/api/?act=1&user=" + user + "&pass=" + pw + "&link=" + Encoding.urlEncode(url));

        int errorcode = 0;
        final String errorcodeString = getJson("ID");
        if (errorcodeString != null) errorcode = Integer.parseInt(getJson("ID"));
        switch (errorcode) {
        case 1:
            logger.info("This account is banned");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        case 2:
            logger.info("This account is banned till midnight");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 5:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 6:
            logger.info("This link is unsupported by sebrid-link.fr: " + link.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FATAL, "Bad link format, please contact the debrid-link.fr support!");
        case 7:
            logger.info("The file hoster is under maintenance");
            tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
            break;
        case 8:
            logger.info("No server available for the file hoster");
            tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
            break;
        case 9:
            logger.info("Limit for host globally reached, temporarily disabling it...");
            tempUnavailableHoster(acc, link, 2 * 60 * 60 * 1000l);
            break;
        case 10:
            logger.info("Limit for host for user reached, temporarily disabling it...");
            tempUnavailableHoster(acc, link, 2 * 60 * 60 * 1000l);
            break;
        case 11:
            logger.info("Flood protection enabled, temporarily disabling premium account");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 12:
            logger.info("Bad link format, please contact the debrid-link.fr support!");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Bad link format, please contact the debrid-link.fr support!");
        case 13:
            logger.info("Downloadlink generation failed!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Downloadlink generation failed, please contact the debrid-link.fr support!");
        }

        int maxChunks = -Integer.parseInt(getJson("NB_CONNECT_PER_DOWNLOAD"));
        String dllink = getJson("LINKDL");
        if (dllink == null) {
            logger.warning("Unhandled download error on debrid-link,fr:");
            logger.warning(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<img src=\\'http://debrid\\-link\\.fr/images/logo\\.png")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            logger.warning("Unhandled download error on debrid-link.fr:");
            logger.warning(br.toString());
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

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}