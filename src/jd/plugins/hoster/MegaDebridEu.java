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
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mega-debrid.eu" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MegaDebridEu extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    public MegaDebridEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mega-debrid.eu/index.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.mega-debrid.eu/index.php";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(true);
        String user = Encoding.urlEncode(account.getUser());
        String pw = Encoding.urlEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);

        // account is valid, let's fetch account details:
        br.getPage("http://mega-debrid.eu/api.php?action=connectUser&login=" + user + "&password=" + pw);
        if (!"ok".equalsIgnoreCase(getJson("response_code"))) {
            ac.setStatus("\r\nInvalid username/password!\r\nFalscher Benutzername/Passwort!");
            logger.severe("mega-debrid.eu: Error, can not parse left days. Account: " + account.getUser() + " Pass: " + account.getPass() + " API response: " + br.toString());
            account.setValid(false);
            return ac;
        }
        account.setProperty("token", getJson("token"));
        ac.setValidUntil(-1);
        try {
            final String daysLeft = getJson("vip_end");
            ac.setValidUntil(Long.parseLong(daysLeft) * 1000l);
        } catch (Exception e) {
            ac.setStatus("Can not get expire date!");
            logger.severe("Multi-debrid.com: Error, can not parse left days. API response: " + br.toString());
            account.setValid(false);
            return ac;
        }

        // now it's time to get all supported hosts
        br.getPage("http://www.mega-debrid.eu/api.php?action=getHosters");
        if (!"ok".equalsIgnoreCase(getJson("response_code"))) {
            ac.setStatus("can not get supported hosts");
            logger.severe("mega-debrid.eu: Error, can not parse supported hosts. API response: " + br.toString());
            account.setValid(false);
            return ac;
        }
        String hostsString = (new Regex(br.toString().replace("]", ">").replace('[', '<'), "\"hosters\":<([^>]*)")).getMatch(0);
        hosts = (new Regex(hostsString, "\"([^\"]*)\"")).getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            for (String host : hosts) {
                if (host.equals("uploaded.net"))
                    supportedHosts.add("uploaded.to");
                else
                    supportedHosts.add(host.trim());
            }
        }
        account.setValid(true);
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via mega-debrid.eu available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via mega-debrid.eu available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String url = Encoding.urlEncode(link.getDownloadURL());

        showMessage(link, "Phase 1/2: Generate download link");
        br.setFollowRedirects(true);
        br.postPage("http://www.mega-debrid.eu/api.php?action=getLink&token=" + account.getStringProperty("token", null), "link=" + url);
        if (br.containsHTML("Erreur : Probl\\\\u00e8me D\\\\u00e9brideur")) {
            logger.info("Unknown error, disabling current host for 3 hours!");
            tempUnavailableHoster(account, link, 3 * 60 * 60 * 1000l);
        }
        String dllink = br.getRegex("\"debridLink\":\"(.*?)\"\\}").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "").replace("\"", "");
        showMessage(link, "Phase 2/2: Download");

        int maxChunks = 0;
        if (link.getBooleanProperty(MegaDebridEu.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(MegaDebridEu.NOCHUNKS, false) == false) {
                    link.setProperty(MegaDebridEu.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(MegaDebridEu.NOCHUNKS, false) == false) {
                        link.setProperty(MegaDebridEu.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
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

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}