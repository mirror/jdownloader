//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alldebrid.com" }, urls = { "https?://(?:[a-z]\\d+\\.alldebrid\\.com|[a-z0-9]+\\.alld\\.io)/dl/[a-z0-9]+/.+" })
public class AllDebridCom extends antiDDoSForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000l);
        this.enablePremium("http://www.alldebrid.com/offer/");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private static final String NICE_HOST         = "alldebrid.com";
    private static final String NICE_HOSTproperty = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private static final String NOCHUNKS          = "NOCHUNKS";
    private final String        hash1             = "593f356a67e32332c13d6692d1fe10b7";

    private int                 statuscode        = 0;
    private Account             currAcc           = null;
    private DownloadLink        currDownloadLink  = null;

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        HashMap<String, String> accDetails = new HashMap<String, String>();
        AccountInfo ac = new AccountInfo();
        getPage("https://www.alldebrid.com/api.php?action=info_user&login=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        handleErrors();

        /* parse api response in easy2handle hashmap */
        String info[][] = br.getRegex("<([^<>]*?)>([^<]*?)</.*?>").getMatches();

        for (String data[] : info) {
            accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String type = accDetails.get("type");
        if ("premium".equals(type)) {
            /* only platinum and premium support */
            getPage("https://www.alldebrid.com/api.php?action=get_host");
            String hoster[] = br.toString().split(",\\s*");
            if (hoster != null) {
                /* workaround for buggy getHost call */
                supportedHosts.add("tusfiles.net");
                for (String host : hoster) {
                    if (host == null || host.length() == 0) {
                        continue;
                    }
                    host = host.replace("\"", "").trim();
                    // hosts that returned decrypted finallinks bound to users ip session. Can not use multihosters..
                    try {
                        if (host.equals("depositfiles.com") && accDetails.get("limite_dp") != null && Integer.parseInt(accDetails.get("limite_dp")) == 0) {
                            logger.info("NOT adding the following host to array of supported hosts as its daily limit is reached: " + host);
                            continue;
                        }
                    } catch (final Exception e) {
                        logger.severe(e.toString());
                    }
                    try {
                        if (host.equals("filefactory.com") && accDetails.get("limite_ff") != null && Integer.parseInt(accDetails.get("limite_ff")) == 0) {
                            logger.info("NOT adding the following host to array of supported hosts as its daily limit is reached: " + host);
                            continue;
                        }
                    } catch (final Exception e) {
                        logger.severe(e.toString());
                    }
                    try {
                        if (host.equals("filesmonster.com") && accDetails.get("limite_fm") != null && Integer.parseInt(accDetails.get("limite_fm")) == 0) {
                            logger.info("NOT adding the following host to array of supported hosts as its daily limit is reached: " + host);
                            continue;
                        }
                    } catch (final Exception e) {
                        logger.severe(e.toString());
                    }
                    supportedHosts.add(host);
                }
            }
            /* Timestamp given in remaining seconds. */
            final String secondsLeft = accDetails.get("timestamp");
            if (secondsLeft != null) {
                account.setValid(true);
                final long validuntil = System.currentTimeMillis() + (Long.parseLong(secondsLeft) * 1001);
                ac.setValidUntil(validuntil);
            } else {
                /* no daysleft available?! */
                account.setValid(false);
            }
        } else {
            /* all others are invalid */
            account.setValid(false);
        }
        if (account.isValid()) {
            ac.setMultiHostSupport(this, supportedHosts);
            ac.setStatus("Premium Account");
        } else {
            ac.setProperty("multiHostSupport", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return ac;
    }

    private void handleErrors() throws PluginException {
        final String error = PluginJSonUtils.getJsonValue(br, "error");
        if (br.toString().matches("(?i)login fail(?:ed)?")) {
            // wrong password and they say this for blocked ip subnet.
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWrong Username:Password, or IP subnet block.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if ("too mutch fail, blocked for 6 hour".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nToo many incorrect attempts at login!\r\nYou've been blocked for 6 hours", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (hash1.equalsIgnoreCase(JDHash.getMD5(br.toString()))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou've been blocked from the API!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "500 internal server error", 15 * 60 * 1000l);
        } else if (StringUtils.startsWithCaseInsensitive(error, "To many downloads")) {
            // some limitation on concurrent download for this host.
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.alldebrid.com/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(null, link, link.getDownloadURL());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(account, link, link.getDownloadURL());
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account acc, final DownloadLink link, final String genlink) throws Exception {
        if (genlink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        showMessage(link, "Task 2: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(AllDebridCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (br != null && PluginJSonUtils.parseBoolean(PluginJSonUtils.getJsonValue(br, "paws"))) {
            final String host = Browser.getHost(link.getDownloadURL());
            final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {

                @Override
                public HashInfo getHashInfo() {
                    return null;
                }

                @Override
                public long getVerifiedFileSize() {
                    return -1;
                }

                @Override
                public String getHost() {
                    return host;
                }

            };
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLinkDownloadable, br.createGetRequest(genlink), true, maxChunks);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, maxChunks);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You are not premium so you can't download this file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Premium required to download this file.");
            } else if (br.containsHTML(">An error occured while processing your request<")) {
                logger.info("Retrying: Failed to generate alldebrid.com link because API connection failed for host link: " + link.getDownloadURL());
                handleErrorRetries("Unknown error", 3, 30 * 60 * 1000l);
            }
            if (!isDirectLink(link)) {
                if (br.containsHTML("range not ok")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
                /* unknown error */
                logger.severe("Error: Unknown Error");
                // disable hoster for 5min
                tempUnavailableHoster(5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        /* save generated link, only if... it it comes from handleMulti */
        if (!isDirectLink(link)) {
            link.setProperty("genLinkAllDebrid", genlink);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) {
                    return;
                }
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull") || "Unexpected rangeheader format:null".equals(errormessage))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(AllDebridCom.NOCHUNKS, false) == false) {
                    link.setProperty(AllDebridCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** TODO: Replace errorhandling stuff with new API statuscode-errorhamdling */
    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        showMessage(link, "Phase 1/2: Generating link");

        String host_downloadlink = link.getDownloadURL();
        /* here we can get a 503 error page, which causes an exception */
        getPage("https://www.alldebrid.com/service.php?pseudo=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(host_downloadlink) + "&json=true");
        final String genlink = PluginJSonUtils.getJsonValue(br, "link");
        // todo: fix this, as json now old error handling will be wrong -raztok20160906
        if (genlink != null) {
            if ("banned".equalsIgnoreCase(genlink.trim())) {
                // account is banned
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account is banned", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (genlink.endsWith("alldebrid_server_not_allowed.txt")) {
                // they show ip banned in this fashion now, confirmed with admin/support. -raztoki20170310
                /*
                 * {"link":"http:\/\/www.alldebrid.com\/alldebrid_server_not_allowed.txt","host":"uploadedto","filename":"Ip not allowed."
                 * ,"icon":"\/lib\/images\/hosts\/uploadedto.png","streaming":[],"nb":0,"error":"","paws":false}
                 */
                statuscode = 1;
                handleAPIErrors(br);
            }
        }
        if (genlink == null || !genlink.matches("https?://.+")) {
            logger.severe("Error: " + genlink);
            handleErrors();
            if (genlink.contains("_limit")) {
                /* limit reached for this host, wait 4h */
                tempUnavailableHoster(4 * 60 * 60 * 1000l);
            }
            updatestatuscode();
            handleAPIErrors(br);
            // we need a final error handling for situations when
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDL(account, link, genlink);
    }

    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            // define custom browser headers and language settings.
            prepBr.getHeaders().put("User-Agent", "JDownloader");
            prepBr.setCustomCharset("utf-8");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink dl) throws PluginException, IOException {
        setConstants(null, dl);
        prepBrowser(br, dl.getDownloadURL());
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if ((con.isContentDisposition() || con.isOK()) && !con.getContentType().contains("html")) {
                if (dl.getFinalFileName() == null) {
                    dl.setFinalFileName(getFileNameFromHeader(con));
                }
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
            getLogger().log(e);
            dl.setAvailable(false);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = other errors
     */
    private void updatestatuscode() {
        String error = PluginJSonUtils.getJsonValue(br, "error");
        if (error != null) {
            if (error.equals("Ip not allowed.")) {
                statuscode = 1;
            } else if (error.equals("Hoster unsupported or under maintenance.") || StringUtils.containsIgnoreCase(error, "Host is under maintenance")) {
                statuscode = 2;
            } else {
                statuscode = 666;
            }
        } else {
            error = br.getRegex("<span style='color:#a00;'>(.*?)</span>").getMatch(0);
            if (error == null) {
                /* No way to tell that something unpredictable happened here --> status should be fine. */
                statuscode = 0;
            } else {
                if (StringUtils.containsIgnoreCase(error, "Host is under maintenance")) {
                    statuscode = 2;
                } else if (error.equals("Invalid link")) {
                    /* complete html example: 1,;,https://tusfiles.net/xxxxxxxxxxxx : <span style='color:#a00;'>Invalid link</span>,;,0 */
                    statuscode = 101;
                } else if (error.equals("Link is dead")) {
                    statuscode = 102;
                } else {
                    statuscode = 666;
                }
            }
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* No email entered --> Should never happen as we validate user-input before -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nDedicated Server/VPN/proxy entdeckt - Account gesperrt!";
                } else {
                    statusMessage = "\r\nDedicated Server/VPN/Proxy detected, account disabled!";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                statusMessage = "Host unsupported or in maintenance";
                handleErrorRetries("hoster_unsupported_or_in_maintenance", 20, 5 * 60 * 1000);
            case 101:
                statusMessage = "Invalid link --> Probably unsupported host";
                tempUnavailableHoster(10 * 60 * 1000l);
            case 102:
                statusMessage = "'Link is dead' --> We don't trust this serverside error --> Retry";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "alldebrid API says 'link is dead''", 60 * 1000l);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries("unknownAPIerror", 10, 2 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(disableTime);
        }
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (isDirectLink(downloadLink)) {
            // generated links do not require an account to download
            return true;
        }
        return true;
    }

    private boolean isDirectLink(final DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}