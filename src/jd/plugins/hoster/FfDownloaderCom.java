//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ffdownloader.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class FfDownloaderCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "ffdownloader.com";

    public FfDownloaderCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://premiumize.me");
    }

    @Override
    public String getAGBLink() {
        return "https://premiumize.me/?show=tos";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 401, 403, 497, 500, 503 });
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
        br.setCurrentURL(null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedffdownloadercom_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedffdownloadercom_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailedffdownloadercom_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(FfDownloaderCom.NOCHUNKS, false) == false) {
                    link.setProperty(FfDownloaderCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(FfDownloaderCom.NOCHUNKS, false) == false) {
                link.setProperty(FfDownloaderCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        br.postPage("https://ffdownloader.com/api/v1/directLink", "_format=json&scheme=https");
        handleAPIErrors(br, account, link);
        String dllink = br.getRegex("location\":\"(http[^\"]+)").getMatch(0);
        if (dllink == null) {
            logger.info(NICE_HOST + ": Unknown error");
            int timesFailed = link.getIntegerProperty("timesfailedffdownloadercom_unknown", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedffdownloadercom_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailedffdownloadercom_unknown", Property.NULL);
                logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        dllink = dllink.replaceAll("\\\\/", "/");
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account);
        br.getPage("https://ffdownloader.com/api/v1/user/profile?_format=json");
        handleAPIErrors(br, account, null);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        final String status = br.getRegex("account_type\":\"([^<>\"]*?)\"").getMatch(0);
        if (!"premium".equals(status)) {
            account.setValid(false);
            return ai;
        }
        String expire = br.getRegex("\"time_left\":(\\d+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expire)) * 1000);
        }
        final String trafficleft_bytes = br.getRegex("traffic_left\":\"((\\-?)\\d+)\"").getMatch(0);
        if (trafficleft_bytes != null) {
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        br.getPage("https://ffdownloader.com/api/v1/hosters?_format=json");
        handleAPIErrors(br, account, null);
        String[] hosts = null;
        final String supportedhoststext = br.getRegex("\\{\"hoster\":\\[(.*?)\\]\\}").getMatch(0);
        if (supportedhoststext != null) {
            hosts = new Regex(supportedhoststext, "\"([a-zA-Z0-9\\.\\-]+)\"").getColumn(0);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        ai.setProperty("multiHostSupport", supportedHosts);
        if (supportedHosts.size() == 0) {
            ai.setStatus("Account valid: 0 Hosts via " + NICE_HOST + " available");
        } else {
            ai.setStatus("Account valid: " + supportedHosts.size() + " Hosts via " + NICE_HOST + " available");
            ai.setProperty("multiHostSupport", supportedHosts);
        }
        return ai;
    }

    private void login(final Account account) {
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
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
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException {
        String statusCode = br.getRegex("\"status\":(\\d+)").getMatch(0);
        if (statusCode == null) return;
        String statusMessage = null;
        try {
            int status = this.br.getRequest().getHttpConnection().getResponseCode();
            switch (status) {
            case 200:
                /* all okay */
                return;
            case 401:
                /* account invalid, disable account. */
                statusMessage = "You are not authenticated";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 403:
                /* account invalid, disable account. */
                statusMessage = "You are not authenticated";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 497:
                /* https should be used - this error should never happen! */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 500:
                /* Internal server error - should never happen */
                if (statusMessage == null) statusMessage = "Internal server error";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 503:
                /*
                 * temp multihoster issue, maintenance period, block host for 3 mins
                 */
                statusMessage = "Hoster temporarily not possible";
                /* only disable plugin for this link */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage, 10 * 60 * 1000l);
            default:
                /* unknown error, do not try again with this multihoster */
                statusMessage = "Unknown error code, please inform JDownloader Development Team";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
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