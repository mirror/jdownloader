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

import org.appwork.utils.formatter.TimeFormatter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "olget.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class OlgetCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public OlgetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://olget.com/register.php");
    }

    private boolean login(Account acc) {
        String username = Encoding.urlEncode(acc.getUser());
        String pass = Encoding.urlEncode(acc.getPass());
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        try {
            br.postPage("http://olget.com/login.php", "b.x=33&b.y=11&password=" + pass + "&username=" + username);
        } catch (Exception e) {
            return false;
        }
        return true;

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        ac.setProperty("multiHostSupport", Property.NULL);
        if (!login(account)) {
            ac.setStatus("can not reach olget.com server.");
            account.setValid(false);
            return ac;
        }
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        // get accountinfo
        String json = br.getPage("http://olget.com/api_access.php?u=" + username + "&p=" + pass);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
        } catch (Exception e) {
            account.setValid(false);
            ac.setStatus("can not understand server answer.");
            return ac;
        }
        String status = rootNode.get("status").getTextValue().trim();
        if (status.equalsIgnoreCase("error")) {
            String errormsg = rootNode.get("msg").getTextValue();
            ac.setStatus(errormsg);
            account.setValid(false);
            return ac;
        } else if (!status.equalsIgnoreCase("success")) {
            ac.setStatus("unknown error.");
            account.setValid(false);
            return ac;
        }
        String accountType = rootNode.get("account_type").getTextValue().trim();
        if (accountType.equalsIgnoreCase("Free")) {
            ac.setStatus("free account");
            account.setValid(false);
            return ac;
        } else if (accountType.equalsIgnoreCase("Lifetime")) {
            account.setValid(true);
        } else if (accountType.equalsIgnoreCase("Premium")) {
            String expire = rootNode.get("expire").getTextValue().trim();
            ac.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM, yyyy  hh:mm:ss", null));
            account.setValid(true);
        } else {
            ac.setStatus("unknown error.");
            account.setValid(false);
            return ac;
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();

        String page = br.getPage("http://olget.com/api/filehosts");
        String hosts[] = new Regex(page, "\"([^\"]*)\":\"([0-9A-Za-z]*)\"").getColumn(0);
        for (String host : hosts) {
            if ((host != null) && !host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via olget.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via olget.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }

        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://olget.com/TOS.php";
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
        String username = Encoding.urlEncode(acc.getUser());
        String pass = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        showMessage(link, "Phase 1/3: Login");
        if (!login(acc)) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l); }
        showMessage(link, "Phase 2/3: Get link");
        String json = br.getPage("http://olget.com/api/linker?u=" + username + "&p=" + pass + "&l=" + url);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        String status = rootNode.get("status").getTextValue().trim();
        if (!status.equalsIgnoreCase("success")) {
            String msg = rootNode.get("msg").getTextValue().trim();
            if (msg.contains("limit") || msg.contains("bigger")) {
                // disable hoster 2h
                tempUnavailableHoster(acc, link, 2 * 60 * 60 * 1000l);
            } else if (msg.contains("Proper")) {
                // disable hoster for 5min
                tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            } else if (msg.contains("logged")) {
                // should never happend
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
            }
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
            String retryMsg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + retryMsg, 20 * 1000l);
        }
        String genlink = rootNode.get("link").getTextValue().trim();
        if (!(genlink.startsWith("http://") || genlink.startsWith("https://"))) {
            // unknown error, wait
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
            String retryMsg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + retryMsg, 20 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            tempUnavailableHoster(acc, link, 20 * 60 * 1000l);
        }
        showMessage(link, "Phase 3/3: Begin download");
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