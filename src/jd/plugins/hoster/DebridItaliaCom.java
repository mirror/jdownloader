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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debriditalia.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOMasdfasdfsadfsdgfd32423" }, flags = { 2 })
public class DebridItaliaCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public DebridItaliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.debriditalia.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.debriditalia.com/index.php";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    private static final String  NOCHUNKS                     = "NOCHUNKS";
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    // don't touch the following!
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        ac.setUnlimitedTraffic();
        if (!loginAPI(account)) {
            if (br.containsHTML("<status>expired</status>")) {
                ac.setStatus("Account is expired!");
                ac.setExpired(true);
                account.setValid(false);
                return ac;
            }
            ac.setStatus("Account is invalid. Wrong username or password?");
            account.setValid(false);
            return ac;
        }
        final String expire = br.getRegex("<expiration>(\\d+)</expiration>").getMatch(0);
        if (expire == null) {
            ac.setStatus("Account is invalid. Invalid or unsupported accounttype!");
            account.setValid(false);
            return ac;
        }
        ac.setValidUntil(Long.parseLong(expire) * 1000l);

        // now let's get a list of all supported hosts:
        br.getPage("http://debriditalia.com/api.php?hosts");
        hosts = br.getRegex("\"([^<>\"]*?)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String host : hosts) {
            supportedHosts.add(host);
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
        ac.setStatus("Account valid");
        ac.setProperty("multiHostSupport", supportedHosts);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {

        showMessage(link, "Generating link");
        String dllink = checkDirectLink(link, "debriditaliadirectlink");
        if (dllink == null) {
            final String encodedLink = Encoding.urlEncode(link.getDownloadURL());
            br.getPage("http://debriditalia.com/api.php?generate=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + Encoding.urlEncode(acc.getPass()) + "&link=" + encodedLink);
            // Either server error or the host is broken (we have to find out by retrying)
            if (br.containsHTML("ERROR: not_available")) {
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfaileddebriditalia", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    link.setProperty("timesfaileddebriditalia", Property.NULL);
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }

            }
            dllink = br.getRegex("(https?://(\\w+\\.)?debriditalia\\.com/dl/.+)").getMatch(0);
            if (dllink == null) {
                logger.info("debriditalia.com: Unknown error - final downloadlink is missing");
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_unknownerror_dllink_missing", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    logger.info("debriditalia.com: Unknown error - final downloadlink is missing -> Disabling current host");
                    link.setProperty("timesfaileddebriditalia_unknownerror_dllink_missing", Property.NULL);
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
        }

        int chunks = 0;
        if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false)) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink.trim()), true, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) {
                logger.info("debriditalia.com: Unknown download error");
                int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_unknowndlerror", 1);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 10) {
                    timesFailed++;
                    link.setProperty("timesfaileddebriditalia_unknowndlerror", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
                } else {
                    link.setProperty("timesfaileddebriditalia_unknowndlerror", Property.NULL);
                    logger.info("debriditalia.com: Unknown download error -> Disabling current host");
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
            logger.info("debriditalia.com: Unknown download error 2" + br.toString());

            int timesFailed = link.getIntegerProperty("timesfaileddebriditalia_unknowndlerror2", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfaileddebriditalia_unknowndlerror2", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                logger.info("debriditalia.com: Unknown download error 2: Disabling current host");
                link.setProperty("timesfaileddebriditalia_unknowndlerror2", Property.NULL);
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }

        }
        // Directlinks can be used for up to 2 days
        link.setProperty("debriditaliadirectlink", dllink);
        try {
            // add a download slot
            controlPrem(+1);
            // start the dl

            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) return;
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                        link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(DebridItaliaCom.NOCHUNKS, false) == false) {
                    link.setProperty(DebridItaliaCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } finally {
            // remove download slot
            controlPrem(-1);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private boolean loginAPI(final Account acc) throws IOException {
        br.getPage("http://debriditalia.com/api.php?check=on&u=" + Encoding.urlEncode(acc.getUser()) + "&p=" + Encoding.urlEncode(acc.getPass()));
        if (!br.containsHTML("<status>valid</status>")) return false;
        return true;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout) throws PluginException {
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
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

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (downloadLink.getDownloadURL().contains("clz.to/")) dllink = downloadLink.getDownloadURL();
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     * 
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     * 
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlPrem(final int num) {
        logger.info("maxFree was = " + maxPrem.get());
        maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxPrem.get());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}