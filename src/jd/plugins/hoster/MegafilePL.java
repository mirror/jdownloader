package jd.plugins.hoster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

@HostPlugin(revision = "$Revision: 2 $", interfaceVersion = 3, names = { "megafile.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-dpspwjrlfosjdhgidshg12" })
public class MegafilePL extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public MegafilePL(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megafile.pl/");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlEncode(account.getUser());
        String password = Encoding.urlEncode(account.getPass());
        final String checkLogin = br.postPage("https://" + this.getHost() + "/managersAPI/accountInfo", "username=" + username + "&password=" + password);

        /* ERROR HANDLING */
        try {
            String[] accountInfo = checkLogin.split(":");
            if (accountInfo[0].contains("ERROR")) {
                ac.setStatus(accountInfo[1]);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, accountInfo[1], PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ac.setStatus("Account valid");
                ac.setTrafficLeft(Long.parseLong(accountInfo[1]));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost());
        }

        /* 2017-02-22: Use website to get better hostlist as that is impossible via API! */
        this.br.getPage("https://" + this.getHost() + "/howitworks");
        final String[] supportedHostsUgly = this.br.getRegex("/images/([A-Za-z0-9\\.\\-]+)\\.mini\\.png").getColumn(0);
        final String supportedHostsStatic[] = { "catshare.net", "rapidu.net", "fileshark.pl", "lunaticfiles.com", "sharehost.eu", "uploaded.to", "turbobit.net", "rapidgator.net", "uploadrocket.net", "filefactory.com", "hitfile.net", "fastshare.cz", "hugefiles.net", "1fichier.com", "uptobox.com", "alfafile.net", "datafile.com", "keep2share.cc", "filejoker.net", "depositfiles.com", "depfile.com", "nitroflare.com", "chomikuj.pl", "" };
        final List<String> supportedHosts;
        if (supportedHostsUgly != null && supportedHostsUgly.length > 0) {
            supportedHosts = Arrays.asList(supportedHostsUgly);
        } else {
            supportedHosts = Arrays.asList(supportedHostsStatic);
        }
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "https://megafile.pl/regulations";
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

        // br.setFollowRedirects(true);
        final String genlink = br.postPage("https://" + this.getHost() + "/managersAPI/downloadLink", "username=" + username + "&password=" + password + "&link=" + url);

        // JOptionPane.showMessageDialog(null, genlink);
        showMessage(link, "Phase 2/2: Download begins!");

        dl = jd.plugins.BrowserAdapter.openDownload(br.cloneBrowser(), link, genlink, true, 1);

        // if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
        // br.followConnection();
        // if (br.containsHTML("You don't have enought transfer to download this file")) {
        // /* No transfer left */
        // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        // } else if (br.containsHTML("Unknown error") || br.containsHTML("Link is inactive")) {
        // /* File not found */
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // }

        dl.setAllowFilenameFromURL(true);
        if (dl.getConnection().getResponseCode() == 404) {
            /* 2017-03-01: file offline --> Do NOT trust that message for this multihoster! */
            logger.info("MOCH 404 --> Do temporarily disable host as this offline message is not trustworthy!");
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            tempUnavailableHoster(acc, link, 1 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
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
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
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