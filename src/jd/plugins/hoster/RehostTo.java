//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rehost.to" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs-rehost" }, flags = { 2 })
public class RehostTo extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            mName              = "rehost.to";
    private static final String                            mProt              = "https://";

    public RehostTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName);
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/?page=tos";
    }

    public Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(Account account, DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        if (link.getDownloadURL().contains("filesmonster")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -10);
        }
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else if (dl.getConnection().getContentType() != null && !dl.getConnection().getContentType().contains("html") && !dl.getConnection().getContentType().contains("text")) {
            /* no content disposition, but api says that some hoster might not have one */
            dl.startDownload();
            return;
        } else {
            /* download is not contentdisposition, so remove this host from premiumHosts list */
            br.followConnection();
            handleAPIErrors(br, account, link);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void handleMultiHost(DownloadLink link, Account account) throws Exception {
        String dllink = null;
        br = newBrowser();
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        br.postPage(mProt + mName + "/process_download.php", "user=cookie&pass=" + account.getStringProperty("long_ses") + "&dl=" + Encoding.urlEncode(link.getDownloadURL()));
        handleAPIErrors(br, account, link);
        if (br.getRedirectLocation() != null) dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        // reset to null prior to checking
        account.setProperty("long_ses", null);
        login(account);
        // set new value to account property, allowing for parallel downloads from multiple accounts.
        account.setProperty("long_ses", br.getRegex("long_ses=([a-z0-9]+)").getMatch(0));
        br.getPage(mProt + mName + "/api.php?cmd=get_premium_credits&long_ses=" + account.getStringProperty("long_ses"));
        if (br.getRegex("(\\d+,0)").getMatch(0) != null) {
            // expired account.
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        // credits_date
        String[][] c_d = br.getRegex("(\\d+),(\\d+)").getMatches();
        if ((c_d != null && c_d.length != 0) && (c_d[0][0] != null && c_d[0][1] != null)) {
            ai.setValidUntil((Long.parseLong(c_d[0][1]) * 1000));
            // not used because this data belongs to a group of hosters, you can still download from the unlimited. When JD gets to 0 it
            // will stop using the account thus breaking the plugin for the unlimited group. Listen to error messages when no credits left,
            // and remove from array.
            // to get back into the correct JD format this formula required
            // ai.setTrafficLeft((Long.parseLong(c_d[0][0]) / 1000 * 1024 * 1024 * 1024));
            ai.setUnlimitedTraffic();
        } else {
            // one of the above fields was null.
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setStatus("Premium");
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);

        hostUpdate(br, account, ai);

        return ai;
    }

    private void login(Account account) throws Exception {
        br = newBrowser();
        br.getPage(mProt + mName + "/api.php?cmd=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
        if (!br.containsHTML("long_ses=[a-z0-9]+") || br.containsHTML("ERROR: Login failed\\.")) {
            // assume it's not a valid account
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login credintials incorrect", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("type\":\"free\"")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Free accounts are not supported", PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 'long timeout' to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private AccountInfo hostUpdate(Browser br, Account account, AccountInfo ai) throws Exception {
        String hostsSup = br.getPage(mProt + mName + "/api.php?cmd=get_supported_och_dl&long_ses=" + account.getStringProperty("long_ses"));
        String[] hosts = new Regex(hostsSup, "([^\",]+),?").getColumn(0);
        if (hosts == null || hosts.length == 0) { return null; }
        ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        // Workaround for old freakshare domain
        if (supportedHosts.contains("freakshare.net") || !supportedHosts.contains("freakshare.com")) supportedHosts.add("freakshare.com");
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void handleAPIErrors(Browser br, Account account, DownloadLink downloadLink) throws Exception {
        String statusMessage = null;
        String error = null;
        // error message usually provided by redirect url!
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("&error="))
            error = new Regex(br.getRedirectLocation(), "error=([^&]+)").getMatch(0);
        // but on download we are following redirects. We need to catch them on the current browser url!
        else if (br.getURL().contains("&error="))
            error = new Regex(br.getURL(), "error=([^&]+)").getMatch(0);
        else
            return;
        try {
            if (error.equals("low_prem_credits")) {
                /*
                 * 'low_prem_credits': this code tells you that the login information posted to process_download.php is invalid (cross check with
                 * get_premum_credits) or that the user ran out of premium traffic. If the premium credits are low and downloads are already running this error
                 * might occur even if get_premium_credits shows otherwise.
                 */
                statusMessage = "You are out of premium credits.";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (error.equals("download_failed")) {
                /*
                 * 'download_failed': file download failed. file might not exist anymore or download failed for an unknown reason (i.e. hosting server ran into
                 * a timeout or our premium login information has become invalid. Please let JD wait 15 min and try again. Repeat that for 3 times until
                 * download is marked as failed.
                 */
                statusMessage = "Download Failed! Temporarily unavailable.";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage, 15 * 60 * 1000);
                // tempUnavailableHoster(account, downloadLink, 15 * 60 * 1000);
            } else if (error.equals("no_prem_available")) {
                /*
                 * 'no_prem_available': there is currently no premium account available in our system to process this download. Please update
                 * get_supported_och_dl.
                 */
                if (error.equals("no_prem_available"))
                    statusMessage = "Download not currently possible. " + mName + " is out of hoster data";
                else
                    // quota exhausted
                    statusMessage = "Out of quota for " + downloadLink.getHost();
                AccountInfo ai = new AccountInfo();
                hostUpdate(br, account, ai);
                if (ai.getProperty("multiHostSupport") != null) account.getAccountInfo().setProperty("multiHostSupport", ai.getProperty("multiHostSupport"));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (error.equals("invalid_link")) {
                /*
                 * 'invalid_link': the provided link format is invalid. Plugin outdated? Contact rehost.
                 */
                statusMessage = "Invalid download link please contact :" + mName + " for further assistance.";
                throw new PluginException(LinkStatus.ERROR_FATAL);
            } else if (error.startsWith("traffic_")) {
                statusMessage = "No Traffic available for this host: " + error;
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (statusMessage == null) statusMessage = "Unknown error code, please inform JDownloader Development Team";
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + error + " statusMessage: " + statusMessage);
            if (br.getRedirectLocation() != null) br.followConnection();
            throw e;
        }

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}