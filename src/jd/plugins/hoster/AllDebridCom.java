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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

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
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alldebrid.com" }, urls = { "https?://s\\d+\\.alldebrid\\.com/dl/[a-z0-9]+/.+" }, flags = { 2 })
public class AllDebridCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public AllDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(4 * 1000l);
        this.enablePremium("http://www.alldebrid.com/offer/");
    }

    private static final String NOCHUNKS = "NOCHUNKS";
    private final String        hash1    = "593f356a67e32332c13d6692d1fe10b7";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        prepBrowser(br);
        HashMap<String, String> accDetails = new HashMap<String, String>();
        AccountInfo ac = new AccountInfo();
        br.getPage("https://www.alldebrid.com/api.php?action=info_user&login=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        handleErrors();

        /* parse api response in easy2handle hashmap */
        String info[][] = br.getRegex("<([^<>]*?)>([^<]*?)</.*?>").getMatches();

        for (String data[] : info) {
            accDetails.put(data[0].toLowerCase(Locale.ENGLISH), data[1].toLowerCase(Locale.ENGLISH));
        }
        ArrayList<String> supportedHosts = new ArrayList<String>();
        String type = accDetails.get("type");
        if ("premium".equals(type)) {
            /* only platinium and premium support */
            br.getPage("https://www.alldebrid.com/api.php?action=get_host");
            String hoster[] = br.toString().split(",\\s*[\r\n]{1,2}\\s*");
            if (hoster != null) {
                /* workaround for buggy getHost call */
                supportedHosts.add("tusfiles.net");
                for (String host : hoster) {
                    if (host == null || host.length() == 0) {
                        continue;
                    }
                    host = host.trim();
                    host = host.substring(1, host.length() - 1);
                    // hosts that returned decrypted finallinks bound to users ip session. Can not use multihosters..
                    try {
                        if (host.equals("rapidshare.com") && accDetails.get("limite_rs") != null && Integer.parseInt(accDetails.get("limite_rs")) == 0) {
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.severe(e.toString());
                    }
                    try {
                        if (host.equals("depositfiles.com") && accDetails.get("limite_dp") != null && Integer.parseInt(accDetails.get("limite_dp")) == 0) {
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.severe(e.toString());
                    }
                    try {
                        if (host.equals("filefactory.com") && accDetails.get("limite_ff") != null && Integer.parseInt(accDetails.get("limite_ff")) == 0) {
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.severe(e.toString());
                    }
                    try {
                        if (host.equals("filesmonster.com") && accDetails.get("limite_fm") != null && Integer.parseInt(accDetails.get("limite_fm")) == 0) {
                            continue;
                        }
                    } catch (final Throwable e) {
                        logger.severe(e.toString());
                    }
                    supportedHosts.add(host);
                }
            }
            String daysLeft = accDetails.get("date");
            if (daysLeft != null) {
                account.setValid(true);
                long validuntil = System.currentTimeMillis() + (Long.parseLong(daysLeft) * 1000 * 60 * 60 * 24);
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
            ac.setStatus("Account valid");
        } else {
            ac.setProperty("multiHostSupport", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNormal accounts are not supported!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return ac;
    }

    private void handleErrors() throws PluginException {
        if ("login fail".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nWrong User Password", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if ("too mutch fail, blocked for 6 hour".equals(br.toString())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nToo many incorrect attempts at login!\r\nYou've been blocked for 6 hours", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (hash1.equalsIgnoreCase(JDHash.getMD5(br.toString()))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou've been blocked from the API!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(null, link, link.getDownloadURL());
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        handleDL(account, link, link.getDownloadURL());
    }

    private void handleDL(final Account acc, final DownloadLink link, final String genlink) throws Exception {
        showMessage(link, "Task 2: Download begins!");
        int maxChunks = 0;
        if (link.getBooleanProperty(AllDebridCom.NOCHUNKS, false)) {
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
                logger.info("Retrying: Failed to generate alldebrid.com link because API connection failed for host link: " + link.getDownloadURL());
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
            if (errormessage != null && (errormessage.startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || errormessage.equals("Unerwarteter Mehrfachverbindungsfehlernull"))) {
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        prepBrowser(br);
        showMessage(link, "Phase 1/2: Generating link");

        // here we can get a 503 error page, which causes an exception
        String genlink = br.getPage("https://www.alldebrid.com/service.php?pseudo=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()) + "&view=1");

        if (genlink == null || !genlink.matches("https?://.+")) {
            int retry = link.getIntegerProperty("retryCount", 0);
            logger.severe("Error: " + genlink);
            handleErrors();
            if (genlink.contains("Hoster unsupported or under maintenance.")) {
                // disable host for 4h
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            } else if (genlink.contains("_limit")) {
                /* limit reached for this host, wait 4h */
                tempUnavailableHoster(acc, link, 4 * 60 * 60 * 1000l);
            } else if (genlink.contains("\"error\":\"Ip not allowed.\"")) {
                // dedicated server/colo ip range, not allowed!
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDedicated server detected, account disabled", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        if (isDirectLink(downloadLink)) {
            // generated links do not require an account to download
            return true;
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