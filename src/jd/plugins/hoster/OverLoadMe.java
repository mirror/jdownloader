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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "over-load.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class OverLoadMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            DOMAIN             = "https://api.over-load.me/";
    private static final String                            NICE_HOST          = "over-load.me";
    private static final String                            APIKEY             = "MDAwMS05YWRhMzI5Y2Y1ODk0ZjM2MGQxM2FjM2I5MTU4OGExYjUzMGE3NmVlNDg4MTQtNTMwYTc2ZWUtNGMwMS0zYWIwMTJlYw==";
    private int                                            STATUSCODE         = 0;
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    public OverLoadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.over-load.me/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.over-load.me/terms.php";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
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
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = (int) account.getLongProperty("chunklimit", 1);
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Download not available at the moment")) {
                handleErrorRetries(account, link, "download_not_available", 5);
            }
            handleErrorRetries(account, link, "unknowndlerror", 5);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                logger.info("Download failed -> Maybe re-trying with only 1 chunk");
                /* unknown error, we disable multiple chunks */
                disableChunkload(link);
                logger.info("Download failed -> Retry with 1 chunk did not solve the problem");
            }
        } catch (final PluginException e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            /* This may happen if the downloads stops at 99,99% - a few retries usually help in this case */
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info(NICE_HOST + ": DOWNLOAD_INCOMPLETE");
                logger.info("DOWNLOAD_INCOMPLETE -> Maybe re-trying with only 1 chunk");
                /* unknown error, we disable multiple chunks */
                disableChunkload(link);
                logger.info("DOWNLOAD_INCOMPLETE -> Retry with 1 chunk did not solve the problem");
                handleErrorRetries(account, link, "dl_incomplete", 5);
            }
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(OverLoadMe.NOCHUNKS, false) == false) {
                link.setProperty(OverLoadMe.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        showMessage(link, "Task 1: Generating Link");
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* request Download */
            this.accessAPISafe(account, link, DOMAIN + "getdownload.php?auth=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = getJson("downloadlink");
            if (dllink == null) {
                handleErrorRetries(account, link, "dllinknull", 5);
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

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
        }
    }

    private void disableChunkload(final DownloadLink dl) throws PluginException {
        /* unknown error, we disable multiple chunks */
        if (dl.getBooleanProperty(OverLoadMe.NOCHUNKS, false) == false) {
            dl.setProperty(OverLoadMe.NOCHUNKS, Boolean.valueOf(true));
            dl.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        accessAPISafe(account, null, DOMAIN + "account.php?user=" + Encoding.urlEncode(account.getUser()) + "&auth=" + Encoding.urlEncode(account.getPass()));
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        final String premium = getJson("membership");
        if (premium != null && !premium.matches("Premium")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expire = getJson("expirationunix");
        if (expire != null) {
            final Long expirestamp = Long.parseLong(expire);
            if (expirestamp == -1) {
                ai.setValidUntil(expirestamp);
            } else {
                ai.setValidUntil(expirestamp * 1000);
            }
        }
        final String trafficleft_bytes = getJson("transferLeft");
        if (trafficleft_bytes != null) {
            ai.setTrafficLeft(trafficleft_bytes);
        } else {
            ai.setUnlimitedTraffic();
        }
        long maxchunks = Integer.parseInt(getJson("chunklimit"));
        if (maxchunks <= 0) {
            maxchunks = 1;
        } else if (maxchunks > 20) {
            maxchunks = 20;
        }
        if (maxchunks > 1) {
            maxchunks = -maxchunks;
        }
        account.setProperty("chunklimit", maxchunks);
        int simultandls = Integer.parseInt(getJson("downloadlimit"));
        if (simultandls <= 0) {
            simultandls = 1;
        } else if (simultandls > 20) {
            simultandls = 20;
        }
        account.setMaxSimultanDownloads(simultandls);
        accessAPISafe(account, null, DOMAIN + "hoster.php?auth=" + Encoding.Base64Decode(APIKEY));
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        for (final String domain : hostDomains) {
            supportedHosts.add(domain);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        ai.setStatus("Premium User");
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":((\\-)?\\d+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
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

    private void accessAPISafe(final Account acc, final DownloadLink dl, final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br, acc, dl);
    }

    private void updatestatuscode() {
        if (br.containsHTML("Invalid auth\\.")) {
            STATUSCODE = 1;
        } else if (br.containsHTML("No authcode send\\.")) {
            STATUSCODE = 1;
        } else if (br.containsHTML("User or Auth not found")) {
            STATUSCODE = 1;
        } else if (br.containsHTML("Invalid URL\\!")) {
            STATUSCODE = 2;
        } else if (br.containsHTML("You used your daily traffic for")) {
            STATUSCODE = 3;
        } else if (br.containsHTML("Could not connect to server, or could not use database")) {
            STATUSCODE = 4;
        } else if (br.containsHTML("Download as Freeuser not possible")) {
            STATUSCODE = 5;
        } else if (br.containsHTML("Systemerror, please try again later")) {
            STATUSCODE = 6;
        } else if (br.containsHTML("File seems to be offline")) {
            STATUSCODE = 7;
        } else if (br.containsHTML("Error fetching dowwloadlink, please try again later")) {
            STATUSCODE = 8;
        } else if (br.containsHTML("No account found, please try again later")) {
            STATUSCODE = 9;
        } else if (br.containsHTML("Hoster offline or Invalid URL")) {
            STATUSCODE = 10;
        } else if (br.containsHTML("Your premium Membership has expired")) {
            STATUSCODE = 11;
        } else if (br.containsHTML("No API-Key send")) {
            STATUSCODE = 12;
        } else if (br.containsHTML("Nothing set")) {
            STATUSCODE = 13;
        } else if (br.containsHTML("Download not available at the moment")) {
            STATUSCODE = 14;
        } else if (br.containsHTML("No traffic left or server is full")) {
            STATUSCODE = 15;
        } else if (br.containsHTML("\"err\":1")) {
            STATUSCODE = 666;
        } else {
            STATUSCODE = 0;
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException {
        String statusMessage = null;
        try {
            switch (STATUSCODE) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                final String lang = System.getProperty("user.language");
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Gehe sicher, dass du als Passwort den Auth-code eingegeben hast. Diesen findest du hier:\r\nover-load.me/account.php\r\n2. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n3. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. Make sure that you entered the Auth-code as password. You can find it here:\r\nover-load.me/account.php\r\n2. If your password contains special characters, change it (remove them) and try again!\r\n3. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 2:
                /* Invalid link -> Disable current host */
                statusMessage = "\r\nCurrent host is not supported";
                tempUnavailableHoster(account, downloadLink, 3 * 60 * 60 * 1000l);
            case 3:
                /* No traffic left for current host */
                statusMessage = "No traffic left for current host";
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
            case 4:
                /* Database connect error */
                statusMessage = "Could not connect to database";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 5:
                /* Free accounts are not supported */
                statusMessage = "Download impossible as free user";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 6:
                /* System error */
                statusMessage = "Systemerror";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 7:
                /* File offline */
                statusMessage = "File offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 8:
                /* Error fetching download url -> Disable for 5 minutes */
                statusMessage = "Error fetching download url";
                tempUnavailableHoster(account, downloadLink, 5 * 60 * 1000l);
            case 9:
                /* No account found -> Disable host for 30 minutes */
                statusMessage = "No account found";
                tempUnavailableHoster(account, downloadLink, 30 * 60 * 1000l);
            case 10:
                /* Host offline or invalid url -> Disable for 5 minutes */
                statusMessage = "Host offline";
                tempUnavailableHoster(account, downloadLink, 5 * 60 * 1000l);
            case 11:
                /* Account expired -> Disable account */
                statusMessage = "Account expired  |  Account abgelaufenF";
                account.getAccountInfo().setExpired(true);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 12:
                /* No API code sent -> Disable account */
                statusMessage = "No API code sent";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 13:
                /* Nothing set -> Disable account */
                statusMessage = "Nothing set";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 14:
                /* Download not possible at the moment */
                statusMessage = "Download not possible at the moment";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage);
            case 15:
                /* No traffic left or server is full -> Disable for 1 hour */
                statusMessage = "No traffic left or server is full";
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
            case 666:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                int timesFailed = downloadLink.getIntegerProperty("NICE_HOSTproperty + timesfailed_unknownapierror", 0);
                downloadLink.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_unknownapierror", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown API error");
                } else {
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_unknownapierror", Property.NULL);
                    logger.info(NICE_HOST + ": Unknown API error - disabling current host!");
                    tempUnavailableHoster(account, downloadLink, 30 * 60 * 1000l);
                }
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + STATUSCODE + " statusMessage: " + statusMessage);
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