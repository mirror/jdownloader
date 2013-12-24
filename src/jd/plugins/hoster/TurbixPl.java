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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

//IMPOORTANT: Sync with the EasyFilesPl code
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "turbix.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class TurbixPl extends PluginForHost {

    // Based on API: http://easyfiles.pl/api_dokumentacja.php?api_en=1
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "turbix.pl";
    private static final String                            API_HTTP           = "http://www.";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private int                                            STATUSCODE         = 0;

    public TurbixPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://" + NICE_HOST + "/cennik.html");
    }

    @Override
    public String getAGBLink() {
        return "http://" + NICE_HOST + "/regulamin.html";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
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
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ac = new AccountInfo();
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        // TODO: Add support for 1 stupid login captcha type
        safeAPIRequest(API_HTTP + NICE_HOST + "/api2.php?login=" + username + "&pass=" + pass + "&cmd=get_acc_details", account, null);
        // Check for reCaptcha
        final String rcID = br.getRegex("google\\.com/recaptcha/api/challenge\\?k=(.+)").getMatch(0);
        if (rcID != null) {
            final DownloadLink dummyLink = new DownloadLink(this, "Account", NICE_HOST, "http://" + NICE_HOST, true);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, dummyLink);
                apiRequest(API_HTTP + NICE_HOST + "/api2.php?login=" + username + "&pass=" + pass + "&recaptcha_challenge=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response=" + Encoding.urlEncode(c) + "&cmd=get_acc_details", account, null);
                if (STATUSCODE == 3) {
                    rc.reload();
                    continue;
                }
                break;
            }
            handleAPIErrors(this.br, account, dummyLink);
        }
        final String[] information = br.toString().split(":");
        ac.setTrafficLeft(SizeFormatter.getSize(Long.parseLong(information[0]) + "MB"));
        try {
            int maxSim = Integer.parseInt(information[0]);
            if (maxSim > 20)
                maxSim = 20;
            else if (maxSim < 0) maxSim = 1;
            maxPrem.set(maxSim);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        ac.setStatus("Premium User");
        // now let's get a list of all supported hosts:
        br.getPage(API_HTTP + NICE_HOST + "/api2.php?cmd=get_hosts");
        hosts = br.toString().split(":");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
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

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via " + NICE_HOST + " available");
        } else {
            supportedHosts.remove("youtube.com");
            supportedHosts.remove("vimeo.com");
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via " + NICE_HOST + " available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private void apiRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.getPage(requestUrl);
        updatestatuscode();
    }

    private void safeAPIRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.getPage(requestUrl);
        updatestatuscode();
        handleAPIErrors(this.br, acc, dl);
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
        this.br = newBrowser();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "finallink");
        if (dllink == null) {
            String dlid = link.getStringProperty(NICE_HOSTproperty + "dlid", null);
            dlid = null;
            if (dlid == null) {
                final String url = Encoding.urlEncode(link.getDownloadURL());
                showMessage(link, "Phase 1/3: Starting internal download on " + NICE_HOST);
                safeAPIRequest(API_HTTP + NICE_HOST + "/api.php?cmd=download_files_direct&links=" + url + "&login=" + Encoding.urlEncode(acc.getUser()) + "&pass=" + Encoding.urlEncode(acc.getPass()), acc, link);
                final String[] data = br.toString().split(":");
                dlid = data[1];
                link.setProperty(NICE_HOSTproperty + "dlid", dlid);
            }
            showMessage(link, "Phase 2/3: Checking status of internal download on " + NICE_HOST);
            boolean success = false;
            for (int i = 1; i <= 120; i++) {
                apiRequest(API_HTTP + NICE_HOST + "/api.php?cmd=get_file_status&id=" + dlid + "&login=" + Encoding.urlEncode(acc.getUser()) + "&pass=" + Encoding.urlEncode(acc.getPass()), acc, link);
                if (br.containsHTML("downloaded")) {
                    success = true;
                    break;
                } else if (!br.toString().trim().matches("\\d{1,3}(\\.\\d{1,2})?")) {
                    logger.info(NICE_HOST + ": Fails to download link to server");
                    break;
                }
                logger.info(NICE_HOST + ": Progress of internal download of current link: " + this.br.toString().trim());
                this.sleep(5000l, link);
            }
            if (!success) {
                handleAPIErrors(this.br, acc, link);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            safeAPIRequest(API_HTTP + NICE_HOST + "/api.php?cmd=get_link&id=" + dlid + "&login=" + Encoding.urlEncode(acc.getUser()) + "&pass=" + Encoding.urlEncode(acc.getPass()), acc, link);

            if (br.toString().trim().equals("error")) {
                logger.info(NICE_HOST + ": Final link is null_1");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_dllinknull_1", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull_1", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Final download link not found");
                } else {
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull_1", Property.NULL);
                    logger.info(NICE_HOST + ": Final link is null -> Disabling current host");
                    tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
                }
            }
            dllink = br.toString();
            if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) {
                logger.info(NICE_HOST + ": Final link is null_2");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_dllinknull_2", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull_2", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Final download link not found");
                } else {
                    link.setProperty(NICE_HOSTproperty + "failedtimes_dllinknull_2", Property.NULL);
                    logger.info(NICE_HOST + ": Final link is null -> Plugin is broken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dllink = dllink.replace("\\", "");
        }
        showMessage(link, "Phase 3/3: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(TurbixPl.NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown download error -> Plugin is broken");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(NICE_HOSTproperty + "finallink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(TurbixPl.NOCHUNKS, false) == false) {
                    link.setProperty(TurbixPl.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(TurbixPl.NOCHUNKS, false) == false) {
                link.setProperty(TurbixPl.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
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

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void updatestatuscode() {
        String statusCode = null;
        if (br.toString().matches("\\d{2}.+") && !br.containsHTML(":")) statusCode = br.getRegex("(\\d{2})").getMatch(0);
        if (statusCode != null) {
            STATUSCODE = Integer.parseInt(statusCode);
        } else {
            if (br.containsHTML("premium_no_transfer 1")) {
                STATUSCODE = 600;
            } else if (br.containsHTML("> \\(Pobieranie zablokowane z powodu dubla")) {
                STATUSCODE = 601;
            } else if (br.containsHTML(">Your IP address")) {
                STATUSCODE = 666;
            } else {
                STATUSCODE = 0;
            }
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException {
        String statusMessage = null;
        try {
            switch (STATUSCODE) {
            case 0:
                // Everything ok
                break;
            case 1:
                /* Signing in was disabled by administration -> disable account */
                statusMessage = "Signing in was disabled by administration";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /* Account banned -> disable account */
                statusMessage = "Account banned";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* Wrong captcha code -> disable account */
                statusMessage = "Wrong captcha code entered";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);

            case 4:
                /* Wrong reCaptcha code -> disable account */
                statusMessage = "Wrong reCaptcha code entered";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 5:
                // This should never happen
                /* No captcha type defined -> disable account */
                statusMessage = "No captcha type defined";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 6:
                /* Wrong login or password -> disable account */
                statusMessage = "Wrong login or password";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);

            case 7:
                /* No hosts available -> disable account */
                statusMessage = "No hosts available";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 8:
                /* Downloading files with hour option is disabled -> disable account */
                statusMessage = "Downloading files with hour option is disabled";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 9:
                /* Wrong hour (only numbers from 0 to 23) -> disable account */
                statusMessage = "Wrong hour (only numbers from 0 to 23)";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 10:
                /* Downloading files into name drive is disabled by Administration -> disable account */
                statusMessage = "Downloading files into name drive is disabled by Administration";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 11:
                /* Direct download is disabled by Administration -> disable account */
                statusMessage = "Direct download is disabled by Administration";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 12:
                /* Download is disabled by Administration -> disable account */
                statusMessage = "Download is disabled by Administration";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 13:
                /* No links given -> disable current host */
                statusMessage = "No links given";
                tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
            case 14:
                /* Not enough download slots -> temporarily disable account */
                statusMessage = "Not enough download slots";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 15:
                /* No file id given -> throw fatal error */
                statusMessage = "No file id given";
                throw new PluginException(LinkStatus.ERROR_FATAL, "No file id given");
            case 16:
                /* Files list is empty -> disable account */
                statusMessage = "Files list is empty";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 17:
                /* Downloading list is empty -> disable account */
                statusMessage = "Downloading list is empty";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 18:
                /* Cannot remove file -> disable account */
                statusMessage = "Cannot remove file";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 19:
                /* Cannot abort downloading -> disable account */
                statusMessage = "Cannot abort downloading";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 20:
                /* Cannot get file link -> disable host */
                statusMessage = "Cannot get file link";
                tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
            case 21:
                /* Cannot get file name -> disable host */
                statusMessage = "Cannot get file name";
                tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
            case 22:
                /* Cannot extend file -> disable account */
                statusMessage = "Cannot extend file";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 23:
                /* Cannot return transfer -> disable account */
                statusMessage = "Cannot return transfer";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 24:
                // Happens if you make a request without any command to use a function
                /* No command given -> disable account */
                statusMessage = "No command given";
                logger.info("STATUSCODE: " + STATUSCODE + ": " + "No command given -> Everything is allright");
                break;
            case 600:
                /* No accounts available for host -> disable host */
                statusMessage = "No accounts available for current host";
                tempUnavailableHoster(account, downloadLink, 1 * 60 * 60 * 1000l);
            case 601:
                /* No accounts available for host -> disable host */
                statusMessage = "Too many API requests for the same link";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many API requests for the same link", 5 * 60 * 1000l);
            case 666:
                /* IP banned -> disable account */
                statusMessage = "Your IP has been banned";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            default:
                /* Unknown errorcode -> disable account */
                statusMessage = "Unknown errorcode";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);

            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + STATUSCODE + " statusMessage: " + statusMessage);
            throw e;
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