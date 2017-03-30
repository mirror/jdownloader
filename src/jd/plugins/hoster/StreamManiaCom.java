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

import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

// based on raztoki's plugin for jdownloader 1
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "streammania.com", "brapid.sk" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32323", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32323" })
public class StreamManiaCom extends antiDDoSForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    /*
     * 3 Hoster use streammania domain as private api, but there are different public domains
     */
    private String                                         hostPublicDomain   = "streammania.com";

    private static final String                            NICE_HOST          = "brapid.sk";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private Account                                        currAcc            = null;
    private DownloadLink                                   currDownloadLink   = null;

    public StreamManiaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www." + hostPublicDomain + "/premium.php");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        getPage("http://www." + hostPublicDomain + "/api/get_pa_info.php?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.toString().startsWith("ERROR: Auth")) {
            throw new PluginException(PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        /* parse api response in easy2handle hashmap */
        String info[] = br.getRegex("(\\d+)($|\\|)").getColumn(0);
        boolean isunlimited = "1".equalsIgnoreCase(info[1]);
        long validUntil = Long.parseLong(info[2]);
        long inC = Long.parseLong(info[0]) * 1024 * 1024l;
        long outC = Long.parseLong(info[3]) * 1024 * 1024l;
        if (validUntil == 0) {
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            return ac;
        }
        ac.setValidUntil(validUntil * 1000);
        if (!isunlimited) {
            ac.setTrafficLeft(Math.min(inC, outC));
        } else {
            ac.setUnlimitedTraffic();
        }
        getPage("http://www." + hostPublicDomain + "/api/get_filehosters.php");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String hoster[] = br.getRegex("(.+?)($|\\|)").getColumn(0);
        if (hoster != null) {
            for (final String host : hoster) {
                supportedHosts.add(host.trim());
            }
        }
        if (account.isValid()) {
            ac.setMultiHostSupport(this, supportedHosts);
        } else {
            account.setValid(false);
            ac.setStatus("Account Invalid");
        }
        return ac;
    }

    @Override
    public String getAGBLink() {
        return "http://www." + hostPublicDomain + "/";
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
        br.setFollowRedirects(true);
        dl = null;
        showMessage(link, "Phase 1/2: Get download link");
        getPage("http://www." + hostPublicDomain + "/api/get_ddl.php?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
        String genlink = br.toString();
        String maxChunksString = br.getRequest().getResponseHeader("X-MaxChunks");
        // Tested with share-online.biz: max 4 chunks possible
        int maxChunks = 1;
        if (maxChunksString != null) {
            try {
                maxChunks = -(Integer.parseInt(maxChunksString));
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
        }
        if (!genlink.startsWith("http://")) {
            handleErrorRetries("bad_dllink", 50, 5 * 60 * 1000);
        }
        showMessage(link, "Phase 2/2: Start download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* File eventually offline */
            dl.getConnection().disconnect();
            handleErrorRetries("server_error_404", 20, 5 * 60 * 1000);
        }
        final String serverfilename = getFileNameFromHeader(dl.getConnection());
        if ("Too_many_open_connections.txt".equals(serverfilename)) {
            logger.info("received 'Too_many_open_connections.txt' as server filename...");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe(hostPublicDomain + "(Error): " + br.toString());
            /* disable for 20 min */
            tempUnavailableHoster(20 * 60 * 1000);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("final downloadlink leads to html code...");
            br.followConnection();
            handleErrorRetries("unknowndlerror", 20, 10 * 60 * 1000);
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
    private void handleErrorRetries(final String error, final int maxRetries, final long waittime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(waittime);
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
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}