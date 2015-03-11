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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
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
import jd.utils.locale.JDL;

/**
 * @author gandalf
 * @author raztoki
 * 
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mfhd.de" }, urls = { "http://mfhd\\.de/notsupported/blahblbha/[a-z]+" }, flags = { 2 })
public class Mfhd extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public Mfhd(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(4 * 1000l);
        this.enablePremium("http://mfhd.de");
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        prepBrowser(br);
        HashMap<String, String> accDetails = new HashMap<String, String>();
        AccountInfo ac = new AccountInfo();
        String user_info = br.getPage("http://mfhd.de/api.php?action=user&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        handleAccountErrors();

        /* parse api response in easy2handle hashmap */
        if (user_info.equals("valid")) {
            ArrayList<String> supportedHosts = this.getSupportedHosters();
            account.setValid(true);
            ac.setMultiHostSupport(this, supportedHosts);
            ac.setStatus("Account valid");
        } else {
            /* all others are invalid */
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNormal accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }

        return ac;
    }

    private ArrayList<String> getSupportedHosters() {
        try {
            String hostsSup = null;
            for (int retry = 0; retry < 3; retry++) {
                try {
                    hostsSup = br.cloneBrowser().getPage("http://mfhd.de/api.php?action=hosts");
                    break;
                } catch (SocketException e) {
                    if (retry == 2) {
                        throw e;
                    }
                    Thread.sleep(1000);
                }
            }
            String[] hosts = new Regex(hostsSup, "\"([^\"]+)\"").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            return supportedHosts;
        } catch (Throwable e) {
            // continue like previous! no need to nuke existing setter, next update will retry!
            return null;
        }
    }

    private void handleAccountErrors() throws PluginException {
        if ("invalid credentials".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWrong account credentials", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if ("account blocked".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been blocked!\r\nPlease contact an administrator for further information.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if ("account expired".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has been expired!\r\nPlease go to http://mfhd.de to reactivate your account.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://mfhd.de";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        return 3;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        showMessage(link, "Premium account needed!");
        throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(account, link, link.getDownloadURL());
    }

    private void handleDL(final Account acc, final DownloadLink link, final String genlink) throws Exception {
        showMessage(link, "Task 2: Download begins!");
        int maxChunks = -3;
        if (link.getBooleanProperty(Mfhd.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">An error occured while processing your request<")) {
                logger.info("Retrying: Failed to generate mfhd.de link because API connection failed for host link: " + link.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (!isDirectLink(link)) {
                /* unknown error */
                logger.severe("Error: Unknown Error");
                // disable hoster for 5min
                tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        /* save generated link, only if... it comes from handleMulti */
        if (!isDirectLink(link)) {
            link.setProperty("genLinkMfhd", genlink);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) {
                    return;
                }
            } catch (final Throwable e) {
            }
            final String errormessage = link.getLinkStatus().getErrorMessage();
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(Mfhd.NOCHUNKS, false) == false) {
                    link.setProperty(Mfhd.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        prepBrowser(br);
        showMessage(link, "Phase 1/2: Generating link");

        // here we can get a 503 error page, which causes an exception
        String genlink = br.getPage("http://mfhd.de/api.php?username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&action=info&url=" + Encoding.urlEncode(link.getDownloadURL()));

        /* Possible html, unhandled: 1,;,https://tusfiles.net/xxxxxxxxxxxx : <span style='color:#a00;'>Invalid link</span>,;,0 */
        if (genlink == null || !genlink.matches("https?://.+")) {
            int retry = link.getIntegerProperty("retryCount", 0);
            logger.severe("Error: " + genlink);
            handleAccountErrors();
            if (genlink.contains("invalid hoster")) {
                // disable host for 4h
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            } else if (genlink.contains("_limit")) {
                /* limit reached for this host, wait 4h */
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            } else if (genlink.equals("wrong request")) {
                /* invalid request, wait 1h */
                tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
            }
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (retry >= 3) {
                /* reset retrycounter */
                link.setProperty("retryCount", Property.NULL);
                // disable hoster for 30min
                tempUnavailableHoster(acc, link, 30 * 60 * 1000l);

            }
            String msg = "(" + (retry + 1) + "/" + 3 + ")";
            link.setProperty("retryCount", (retry + 1));
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 20 * 1000l);
        }
        handleDL(acc, link, genlink);
    }

    private boolean isDirectLink(final DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            return true;
        }
        return false;
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("utf-8");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        prepBrowser(br);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
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