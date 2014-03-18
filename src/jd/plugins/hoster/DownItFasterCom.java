package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downitfaster.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-dpspwjrlfosjdhgidshg12" }, flags = { 2 })
public class DownItFasterCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public DownItFasterCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.downitfaster.com/premium");
    }

    // PLEASE CONTACT RAZTOKI on IRC, before committing any new work.

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String password = Encoding.urlEncode(account.getPass());
        String checkLogin = br.postPage("http://downitfaster.com/api/login.php", "login=" + username + "&password=" + password);
        // ERROR HANDLING NEEDED HERE ALSO.
        String[] accountInfo = checkLogin.split(":");
        String premiumUntil = accountInfo[1];
        long validUntil = System.currentTimeMillis() + (Long.parseLong(premiumUntil) * 1000 * 60 * 60 * 24);
        ac.setValidUntil(validUntil);
        ac.setUnlimitedTraffic();

        if (checkLogin.contains("logged")) {
            ac.setStatus("Premium user");
            account.setValid(true);
        } else {
            ac.setStatus("Invalid login or password");
            account.setValid(false);
        }

        String hosts[] = br.getPage("http://downitfaster.com/api/supportedHosts.php").split("<br />");

        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", supportedHosts);

        account.setMaxSimultanDownloads(20);

        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://www.downitfaster.com/tos";
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
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String username = Encoding.urlEncode(acc.getUser());
        String password = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL()).replace("http://", "");
        showMessage(link, "Phase 1/2: Generating link");

        br.setFollowRedirects(true);
        String genlink = br.getPage("http://www.downitfaster.com/api/generateLink.php?login=" + username + "&password=" + password + "&link=" + url + "&JDownloader=true");
        // ERROR HANDLING NEEDED HERE
        // JOptionPane.showMessageDialog(null, genlink);
        showMessage(link, "Phase 2/2: Download begins!");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, 1);

        dl.setAllowFilenameFromURL(true);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            dl.startDownload();
        } catch (Throwable e) {
            link.getLinkStatus().setStatusText("Unknown error.");
            throw new PluginException(LinkStatus.ERROR_RETRY);
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
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}