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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filebit.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class FileBitPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "filebit.pl";
    private static String                                  SESSIONID          = null;

    public FileBitPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filebit.pl/oferta");
    }

    @Override
    public String getAGBLink() {
        return "http://filebit.pl/regulamin";
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
        br.setAllowedResponseCodes(new int[] { 401, 204, 403, 404, 497, 500, 503 });
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

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        br.setCurrentURL(null);
        int maxChunks = -10;
        maxChunks = (int) account.getLongProperty("maxconnections", 1);
        if (maxChunks > 20) maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;
        link.setProperty("filebitpldirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                int timesFailed = link.getIntegerProperty("timesfailedfilebitpl_403dlerror", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailedfilebitpl_403dlerror", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Download could not be started (403)");
                } else {
                    link.setProperty("timesfailedfilebitpl_403dlerror", Property.NULL);
                    logger.info(NICE_HOST + ": 403 download error - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            br.followConnection();
            if (br.containsHTML("<title>FileBit\\.pl \\- Error</title>")) {
                int timesFailed = link.getIntegerProperty("timesfailedfilebitpl_knowndlerror", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailedfilebitpl_knowndlerror", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Download could not be started");
                } else {
                    link.setProperty("timesfailedfilebitpl_knowndlerror", Property.NULL);
                    logger.info(NICE_HOST + ": Known error - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedfilebitpl_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedfilebitpl_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailedfilebitpl_unknowndlerror", Property.NULL);
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
                if (link.getBooleanProperty(FileBitPl.NOCHUNKS, false) == false) {
                    link.setProperty(FileBitPl.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(FileBitPl.NOCHUNKS, false) == false) {
                link.setProperty(FileBitPl.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        showMessage(link, "Task 1: Generating Link");
        String dllink = checkDirectLink(link, "filebitpldirectlink");
        if (dllink == null) {
            /* request Download */
            this.login(account);
            br.getPage("http://filebit.pl/api/index.php?a=getFile&sessident=" + SESSIONID + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
            handleAPIErrors(br, account, link);
            // final String expires = getJson("expires");
            dllink = getJson("downloadurl");
            if (dllink == null) {
                logger.info(NICE_HOST + ": Unknown error");
                int timesFailed = link.getIntegerProperty("timesfailedfilebitpl_unknown", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfailedfilebitpl_unknown", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    link.setProperty("timesfailedfilebitpl_unknown", Property.NULL);
                    logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account);
        br.getPage("http://filebit.pl/api/index.php?a=accountStatus&sessident=" + SESSIONID);
        handleAPIErrors(br, account, null);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        final String premium = getJson("premium");
        if (premium != null && !premium.matches("0|1")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expire = getJson("expires");
        if (expire != null) {
            final Long expirelng = Long.parseLong(expire);
            if (expirelng == -1) {
                ai.setValidUntil(expirelng);
            } else {
                ai.setValidUntil(System.currentTimeMillis() + expirelng);
            }
        }
        final String trafficleft_bytes = getJson("transferLeft");
        if (trafficleft_bytes != null) {
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        int maxSimultanDls = Integer.parseInt(getJson("maxsin"));
        if (maxSimultanDls < 1) {
            maxSimultanDls = 1;
        } else if (maxSimultanDls > 20) {
            maxSimultanDls = 20;
        }
        account.setMaxSimultanDownloads(maxSimultanDls);
        long maxChunks = Integer.parseInt(getJson("maxcon"));
        if (maxChunks > 1) maxChunks = -maxChunks;
        account.setProperty("maxconnections", maxChunks);
        br.getPage("http://filebit.pl/api/index.php?a=getHostList");
        handleAPIErrors(br, account, null);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("\"hostdomains\":\\[(.*?)\\]").getColumn(0);
        for (final String domains : hostDomains) {
            final String[] realDomains = new Regex(domains, "\"(.*?)\"").getColumn(0);
            for (final String realDomain : realDomains) {
                supportedHosts.add(realDomain);
            }
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
        if (!"1".equals(premium)) {
            account.setProperty("free", true);
            ai.setStatus("Free Account");
        } else {
            account.setProperty("free", false);
            ai.setStatus("Premium Account");
        }
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void login(final Account account) throws IOException, PluginException {
        br.getPage("http://filebit.pl/api/index.php?a=login&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        handleAPIErrors(br, account, null);
        SESSIONID = getJson("sessident");
        if (SESSIONID == null) {
            // This should never happen
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":((\\-)?\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
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
        String statusCode = br.getRegex("\"errno\":(\\d+)").getMatch(0);
        if (statusCode == null && br.containsHTML("\"result\":true"))
            statusCode = "999";
        else if (statusCode == null) statusCode = "0";
        String statusMessage = null;
        try {
            int status = Integer.parseInt(statusCode);
            switch (status) {
            case 0:
                /* Everything ok */
                break;
            case 2:
                /* Login or password missing -> disable account */
                statusMessage = "\r\nInvalid account / Ungültiger Account";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* Account invalid -> disable account. */
                statusMessage = "\r\nInvalid account / Ungültiger Account";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 10:
                /* Link offline */
                statusMessage = "Link offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 11:
                /* Host not supported -> Remove it from hostList */
                statusMessage = "Host not supported";
                tempUnavailableHoster(account, downloadLink, 3 * 60 * 60 * 1000l);
            case 12:
                /* Host offline -> Disable for 5 minutes */
                statusMessage = "Host offline";
                tempUnavailableHoster(account, downloadLink, 5 * 60 * 1000l);
            default:
                /* unknown error, do not try again with this multihoster */
                statusMessage = "Unknown API error code, please inform JDownloader Development Team";
                logger.info(NICE_HOST + ": Unknown error");
                int timesFailed = downloadLink.getIntegerProperty("timesfailedfilebitpl_unknown_api", 0);
                downloadLink.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    downloadLink.setProperty("timesfailedfilebitpl_unknown_api", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    downloadLink.setProperty("timesfailedfilebitpl_unknown_api", Property.NULL);
                    logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statusCode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}