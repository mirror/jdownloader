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
import java.util.HashSet;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multidown.co.il" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class MultidownCoIl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MultidownCoIl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://multidown.co.il/");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String page = null;
        String hosts = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        page = br.getPage("http://multidown.co.il/api.php?user=" + username + "&pass=" + pass + "&link={stupid_workaround_to_get_pw_ok}");
        String error = "";
        try {
            error = getRegexTag(page, "error").getMatch(0);
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "server error. Please try later.", 10 * 60 * 1000l);
        }
        if (!error.equalsIgnoreCase("Host not supported") && !error.equalsIgnoreCase("Host not supported or under maintenance") && !error.equalsIgnoreCase("\u05e9\u05e8\u05ea \u05dc\u05d0 \u05d6\u05de\u05d9\u05df")) {
            // wrong pass
            account.setValid(false);
            ac.setStatus("account invalid. Wrong password?");
            return ac;
        }
        // account is valid, check if expired:
        page = br.getPage("http://multidown.co.il/api.php?user=" + username + "&pass=" + pass);
        long daysLeft = -1;
        try {
            daysLeft = Long.parseLong(getRegexTag(page, "daysleft").getMatch(0));
        } catch (Exception e) {
        }
        account.setValid(true);
        long validuntil = System.currentTimeMillis() + (daysLeft * 1000 * 60 * 60 * 24);
        ac.setValidUntil(validuntil);
        HashSet<String> supportedHosts = new HashSet<String>();
        hosts = br.getPage("http://multidown.co.il/api.php?hosts=1");
        if (hosts == null || hosts.isEmpty()) {
            account.setValid(false);
            ac.setStatus("cn not get supported hosters.");
            return ac;
        }
        String hosters[] = new Regex(hosts, "'([^,]*)'").getColumn(0);
        for (String host : hosters) {
            supportedHosts.add(host.trim());
        }
        /* workaround for uploaded.to */
        if (supportedHosts.contains("uploaded.net")) {
            supportedHosts.add("uploaded.to");
        }
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", new ArrayList<String>(supportedHosts));
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://multidown.co.il/terms-of-service/";
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
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        showMessage(link, "Phase 1/2: Generating Link");
        String page = br.getPage("http://multidown.co.il/api.php?user=" + user + "&pass=" + pw + "&link=" + url);
        String error = "";
        try {
            error = getRegexTag(page, "error").getMatch(0);
        } catch (Exception e) {
            // we handle it 2 lines later
        }
        if (!(error == null || error.isEmpty())) {
            // better error handling possible if we got more information multihoster
            showMessage(link, "Error: " + error);
            tempUnavailableHoster(acc, link, 20 * 60 * 1000l);
        }
        // hopefully no error, page should contain downloadlink
        String genlink = "";
        try {
            genlink = getRegexTag(page, "url").getMatch(0);
        } catch (Exception e) {
            // we handle it later
        }
        genlink = genlink.replaceAll("\\\\/", "/");
        if (!(genlink.startsWith("http://") || genlink.startsWith("https://"))) {
            logger.severe("Multidown.co.il(Error): " + genlink);
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
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 0);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe("Multidown.co.il(Error): " + br.toString());
            // disable hoster for 5min
            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        showMessage(link, "Phase 2/2: Download begins!");
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

    private Regex getRegexTag(String someText, String tag) {
        // example: "error":"Host not supported"
        return new Regex(someText, "\"" + tag + "\":\"([^\"]*)\"");
    }

}