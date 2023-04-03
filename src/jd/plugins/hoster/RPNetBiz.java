//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.rpnet.biz" }, urls = { "" })
public class RPNetBiz extends PluginForHost {
    private static final String          mName                    = "rpnet.biz";
    private static final String          mProt                    = "http://";
    private static final String          api_base                 = "https://premium.rpnet.biz/";
    private static MultiHosterManagement mhm                      = new MultiHosterManagement("premium.rpnet.biz");
    private static final int             HDD_WAIT_THRESHOLD       = 10 * 60000;                                    // 10 mins in
    private static final String          PROPERTY_queue_id        = "rpnet_queue_id";
    private static final String          PROPERTY_max_connections = "rpnet_api_max_connections";
    private static final int             default_maxchunks        = -6;

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://"));
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public String getAGBLink() {
        return api_base + "tos.php";
    }

    public void prepBrowser() {
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* This should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception, PluginException {
        /* This should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* This should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (account.getUser() == null || !account.getUser().matches("\\d+") || StringUtils.isEmpty(account.getPass()) || !account.getPass().matches("[a-f0-9]{40}")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your customer ID as username and your API Key as password!\r\nYou can find this data here: premium.rpnet.biz/account", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final AccountInfo ai = new AccountInfo();
        prepBrowser();
        br.getPage(api_base + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + URLEncoder.encode(account.getPass(), "UTF-8") + "&action=showAccountInformation");
        if (br.toString().contains("Invalid authentication.")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid User : API Key", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.containsHTML("IP Ban in effect for")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account is temporarily banned", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final Map<String, Object> accountinfo = (Map<String, Object>) entries.get("accountInfo");
        final String currentServer = (String) accountinfo.get("currentServer");
        final long expiryDate = ((Number) accountinfo.get("premiumExpiry")).longValue();
        ai.setValidUntil(expiryDate * 1000, br);
        final String hosts = br.getPage(api_base + "hostlist.php");
        if (hosts != null) {
            final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts.split(",")));
            ai.setMultiHostSupport(this, supportedHosts);
        }
        account.setType(AccountType.PREMIUM);
        String status = "Premium Account";
        if (!StringUtils.isEmpty(currentServer)) {
            status += String.format(" [Current server: %s]", currentServer);
        }
        ai.setStatus(status);
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String downloadURL = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
        prepBrowser();
        String generatedLink = checkDirectLink(link, "cachedDllink");
        /* Try to restore last API maxchunks value so it will be used for stored directurls too! */
        int maxChunks = (int) link.getLongProperty(PROPERTY_max_connections, default_maxchunks);
        String filename = null;
        Object maxChunksO = null;
        /*
         * 2020-05-29: Their queue download does not return any detailed status (only "completed" or "Queued") and needs a long time to
         * complete thus we're trying to avoid this from blocking download slots.
         */
        Map<String, Object> entries = null;
        final boolean allowQueueToBlockDownloadSlot = false;
        if (generatedLink == null) {
            long queueID = link.getLongProperty(PROPERTY_queue_id, 0);
            if (queueID > 0) {
                logger.info("Continuing with stored queueID: " + queueID);
            } else {
                setStatusText(link, "Generating Link");
                /* request Download */
                String apiDownloadLink = api_base + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&action=generate&links=" + Encoding.urlEncode(downloadURL);
                br.getPage(apiDownloadLink);
                entries = restoreFromString(br.toString(), TypeRef.MAP);
                checkErrors(link, account, entries);
                Object downloadsO = entries.get("downloads");
                if (downloadsO == null) {
                    downloadsO = entries.get("links");
                }
                if (downloadsO == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (downloadsO instanceof Map) {
                    entries = (Map<String, Object>) downloadsO;
                } else {
                    entries = ((List<Map<String, Object>>) downloadsO).get(0);
                }
                checkErrors(link, account, entries);
                maxChunksO = entries.get("max_connections");
                final Object queueIDO = entries.get("id");
                if (queueIDO != null) {
                    // logger.info("Queue download");
                    queueID = ((Number) queueIDO).longValue();
                }
            }
            if (queueID > 0) {
                // QueueID given? => request the download from rpnet hdd
                logger.info("Queue download");
                link.setProperty(PROPERTY_queue_id, queueID);
                final PluginProgress waitProgress = new PluginProgress(0, 100, null) {
                    protected long lastCurrent    = -1;
                    protected long lastTotal      = -1;
                    protected long startTimeStamp = -1;

                    @Override
                    public PluginTaskID getID() {
                        return PluginTaskID.WAIT;
                    }

                    @Override
                    public String getMessage(Object requestor) {
                        if (requestor instanceof ETAColumn) {
                            final long eta = getETA();
                            if (eta >= 0) {
                                return TimeFormatter.formatMilliSeconds(eta, 0);
                            }
                            return "";
                        }
                        return "Waiting for upload to rpnet HDD";
                    }

                    @Override
                    public void updateValues(long current, long total) {
                        super.updateValues(current, total);
                        if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
                            lastTotal = total;
                            lastCurrent = current;
                            startTimeStamp = System.currentTimeMillis();
                            // this.setETA(-1);
                            return;
                        }
                        long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                        if (currentTimeDifference <= 0) {
                            return;
                        }
                        long speed = (current * 10000) / currentTimeDifference;
                        if (speed == 0) {
                            return;
                        }
                        long eta = ((total - current) * 10000) / speed;
                        this.setETA(eta);
                    }
                };
                waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                waitProgress.setProgressSource(this);
                try {
                    long lastProgressChange = System.currentTimeMillis();
                    int lastProgress = -1;
                    while (System.currentTimeMillis() - lastProgressChange < HDD_WAIT_THRESHOLD) {
                        if (isAbort()) {
                            logger.info("Process aborted by user");
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                        // br.getPage(api_base + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" +
                        // Encoding.urlEncode(account.getPass()) + "&action=downloadInformation&id=" + queueID);
                        br.getPage(api_base + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&action=downloadsInformation&type=queue&ids%5B%5D=" + queueID);
                        entries = restoreFromString(br.toString(), TypeRef.MAP);
                        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "downloads/{0}");
                        checkErrors(link, account, entries);
                        if (entries == null) {
                            /* Maybe invalid/old queueID? */
                            link.removeProperty(PROPERTY_queue_id);
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error in queue handling", 5 * 60 * 1000l);
                        }
                        String statusText = null;
                        statusText = (String) entries.get("text_status");
                        int currentProgress = 0;
                        if ("completed".equalsIgnoreCase(statusText)) {
                            currentProgress = 100;
                        } else {
                            try {
                                currentProgress = Integer.parseInt(statusText.substring(1, statusText.length() - 1));
                            } catch (final Throwable e) {
                            }
                        }
                        // download complete?
                        if (currentProgress == 100) {
                            generatedLink = (String) entries.get("rpnet_link");
                            filename = (String) entries.get("filename");
                            maxChunksO = entries.get("max_connections");
                            break;
                        } else {
                            if (!allowQueueToBlockDownloadSlot) {
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait for queue download to finish", 1 * 60 * 1000l);
                            }
                            link.addPluginProgress(waitProgress);
                            waitProgress.updateValues(currentProgress, 100);
                            for (int sleepRound = 0; sleepRound < 10; sleepRound++) {
                                if (isAbort()) {
                                    throw new PluginException(LinkStatus.ERROR_RETRY);
                                } else {
                                    Thread.sleep(1000);
                                }
                            }
                            if (currentProgress != lastProgress) {
                                lastProgressChange = System.currentTimeMillis();
                                lastProgress = currentProgress;
                            }
                        }
                    }
                } finally {
                    link.removePluginProgress(waitProgress);
                }
            } else {
                logger.info("Direct download");
                generatedLink = (String) entries.get("generated");
                filename = (String) entries.get("filename");
            }
        }
        setStatusText(link, "Download begins!");
        if (StringUtils.isEmpty(generatedLink)) {
            mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 20);
        }
        if (maxChunksO != null) {
            maxChunks = Integer.valueOf(maxChunksO.toString());
            logger.info("Found API maxConnections value: " + maxChunks);
        }
        if (maxChunks == 1) {
            maxChunks = 1;
        } else if (maxChunks > 0) {
            maxChunks = -maxChunks;
        } else {
            maxChunks = 0;
        }
        if (maxChunksO != null) {
            /* Save to re-use later */
            link.setProperty(PROPERTY_max_connections, (long) maxChunks);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, true, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
            }
            link.setProperty("cachedDllink", generatedLink);
            dl.startDownload();
            return;
        } catch (final PluginException e1) {
            if (e1.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info("rpnet.biz: ERROR_DOWNLOAD_INCOMPLETE --> Quitting loop");
                throw e1;
            } else if (e1.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_FAILED) {
                logger.info("rpnet.biz: ERROR_DOWNLOAD_FAILED --> Quitting loop");
                throw e1;
            }
        }
    }

    /**
     * Handle errors in multihoster mode
     *
     * @throws InterruptedException
     * @throws PluginException
     */
    private void checkErrors(final DownloadLink link, final Account account, final Map<String, Object> entries) throws PluginException, InterruptedException {
        if (entries != null) {
            final String error = (String) entries.get("error");
            if (!StringUtils.isEmpty(error)) {
                mhm.handleErrorGeneric(account, link, error, 20);
            }
        }
    }

    private void setStatusText(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }
}