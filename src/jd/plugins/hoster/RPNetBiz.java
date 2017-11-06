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
import java.util.HashMap;
import java.util.List;

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

import org.appwork.storage.simplejson.JSonArray;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonNode;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premium.rpnet.biz" }, urls = { "http://(www\\.)?dl[^\\.]*.rpnet\\.biz/download/.*/([^/\\s]+)?" })
public class RPNetBiz extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            mName              = "rpnet.biz";
    private static final String                            mProt              = "http://";
    private static final String                            mPremium           = "https://premium.rpnet.biz/";
    private static final String                            FAIL_STRING        = "rpnetbiz";
    private static final int                               HDD_WAIT_THRESHOLD = 10 * 60000;                                   // 10 mins in

    // ms
    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://"));
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "rpnet.biz".equals(host)) {
            return "premium.rpnet.biz";
        }
        return super.rewriteHost(host);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public String getAGBLink() {
        return mPremium + "tos.php";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // tested with 20 seems fine.
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception, PluginException {
        handleDL(link, link.getDownloadURL(), -6);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, -6);
        URLConnectionAdapter con = dl.getConnection();
        List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
        if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
            if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setName(getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (StringUtils.isEmpty(account.getUser())) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "User name can not be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (StringUtils.isEmpty(account.getPass()) || !account.getPass().matches("[a-f0-9]{40}")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You need to use API Key as password", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        AccountInfo ai = new AccountInfo();
        prepBrowser();
        br.getPage(mPremium + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + URLEncoder.encode(account.getPass(), "UTF-8") + "&action=showAccountInformation");
        if (br.toString().contains("Invalid authentication.")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid User : API Key", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.containsHTML("IP Ban in effect for")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your accounr is temporarily banned", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        JSonObject node = (JSonObject) new JSonFactory(br.toString()).parse();
        JSonObject accountInfo = (JSonObject) node.get("accountInfo");
        long expiryDate = Long.parseLong(accountInfo.get("premiumExpiry").toString().replaceAll("\"", ""));
        ai.setValidUntil(expiryDate * 1000);
        // get the supported hosts
        String hosts = br.getPage(mPremium + "hostlist.php");
        if (hosts != null) {
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts.split(",")));
            ai.setMultiHostSupport(this, supportedHosts);
        }
        ai.setStatus("Premium Account");
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
        }
        return true;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        // This should never happen
        if (downloadLink.getHost().contains("rpnet")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "FATAL server error", 5 * 60 * 1000l);
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
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
        String downloadURL = link.getDownloadURL();
        prepBrowser();
        String generatedLink = checkDirectLink(link, "cachedDllink");
        int maxChunks = 0;
        if (generatedLink != null) {
            logger.info("Reusing cached download link");
        } else {
            // end of workaround
            showMessage(link, "Generating Link");
            /* request Download */
            String apiDownloadLink = mPremium + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&action=generate&links=" + Encoding.urlEncode(downloadURL);
            br.getPage(apiDownloadLink);
            JSonObject node = (JSonObject) new JSonFactory(br.toString().replaceAll("\\\\/", "/")).parse();
            JSonArray links = (JSonArray) node.get("links");
            // for now there is only one generated link per api call, could be changed in the future, therefore iterate anyway
            for (JSonNode linkNode : links) {
                JSonObject linkObj = (JSonObject) linkNode;
                JSonNode errorNode = linkObj.get("error");
                if (errorNode != null) {
                    // shows a more detailed error message returned by the API, especially if the DL Limit is reached for a host
                    String msg = errorNode.toString();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg);
                }
                // Only ID given? => request the download from rpnet hdd
                JSonNode idNode = linkObj.get("id");
                generatedLink = null;
                if (idNode != null) {
                    String id = idNode.toString();
                    int prevProgress = 0;
                    long prevTimestamp = System.currentTimeMillis();
                    while (System.currentTimeMillis() - prevTimestamp < HDD_WAIT_THRESHOLD) {
                        if (isAbort()) {
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                        br.getPage(mPremium + "client_api.php?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&action=downloadInformation&id=" + Encoding.urlEncode(id));
                        final JSonObject node2 = (JSonObject) new JSonFactory(br.toString().replaceAll("\\\\/", "/")).parse();
                        final JSonObject downloadNode = (JSonObject) node2.get("download");
                        final String tmp = downloadNode.get("status").toString();
                        final Integer progress = Integer.parseInt(tmp.substring(1, tmp.length() - 1));
                        // download complete?
                        if (progress == 100) {
                            String tmp2 = downloadNode.get("rpnet_link").toString();
                            final Object max_connections = downloadNode.get("max_connections");
                            if (max_connections != null) {
                                final int chunks = Integer.valueOf(max_connections.toString());
                                if (chunks > 0) {
                                    maxChunks = -chunks;
                                }
                            }
                            generatedLink = tmp2.substring(1, tmp2.length() - 1);
                            break;
                        }
                        sleep(10000, link, "Waiting for upload to rpnet HDD - " + progress + "%");
                        if (progress != prevProgress) {
                            prevTimestamp = System.currentTimeMillis();
                            prevProgress = progress;
                        }
                    }
                } else {
                    String tmp = ((JSonObject) linkNode).get("generated").toString();
                    final Object max_connections = ((JSonObject) linkNode).get("max_connections");
                    if (max_connections != null) {
                        final int chunks = Integer.valueOf(max_connections.toString());
                        if (chunks > 0) {
                            maxChunks = -chunks;
                        }
                    }
                    generatedLink = tmp.substring(1, tmp.length() - 1);
                    break;
                }
            }
        }
        showMessage(link, "Download begins!");
        if (StringUtils.isNotEmpty(generatedLink)) {
            try {
                handleDL(link, generatedLink, maxChunks);
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
        } else {
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_dlfailedunknown", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 10) {
                logger.info(this.getHost() + ": download failed -> Retrying");
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_dlfailedunknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_dlfailedunknown", Property.NULL);
                logger.info(this.getHost() + ": Download failed for unknown reasons -> Disabling current host");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
    }

    private void handleDL(DownloadLink link, String dllink, int maxChunks) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().isContentDisposition()) {
            link.setProperty("cachedDllink", dllink);
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }
        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openGetConnection(dllink);
                List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
                if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }
}