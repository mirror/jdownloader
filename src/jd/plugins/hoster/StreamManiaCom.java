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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

// based on raztoki's plugin for jdownloader 1
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "streammania.com", "brapid.sk", "megafastlink.eu" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32323", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32323", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32323" }, flags = { 2, 2, 2 })
public class StreamManiaCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    /*
     * 3 Hoster use streammania domain as private api, but there are different public domains
     */
    private String                                         hostPublicDomain   = "streammania.com";
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            FAIL_STRING        = "streammaniacom";

    public StreamManiaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www." + hostPublicDomain + "/premium.php");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setDebug(true);
        String username = Encoding.urlEncode(account.getUser());
        String pass = Encoding.urlEncode(account.getPass());
        String page = null;
        String hosts = null;
        try {
            page = br.getPage("http://www." + hostPublicDomain + "/api/get_pa_info.php?login=" + username + "&password=" + pass);
            hosts = br.getPage("http://www." + hostPublicDomain + "/api/get_filehosters.php");
        } catch (Exception e) {
            account.setTempDisabled(true);
            account.setValid(true);
            ac.setProperty("multiHostSupport", Property.NULL);
            return ac;
        }
        if (page.startsWith("ERROR: Auth")) {
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus(page);
            return ac;
        }
        /* parse api response in easy2handle hashmap */
        String info[] = new Regex(page, "(\\d+)($|\\|)").getColumn(0);
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
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (hosts != null) {
            String hoster[] = new Regex(hosts, "(.+?)($|\\|)").getColumn(0);
            for (String host : hoster) {
                if (hosts != null) {
                    supportedHosts.add(host.trim());
                }
            }

        }
        if (account.isValid()) {
            if (supportedHosts.size() == 0) {
                ac.setStatus("Account valid: 0 Hosts via " + hostPublicDomain + " available");
                ac.setProperty("multiHostSupport", Property.NULL);
            } else {
                ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via " + hostPublicDomain + " available");
                ac.setProperty("multiHostSupport", supportedHosts);
            }
        } else {
            account.setTempDisabled(false);
            account.setValid(false);
            ac.setProperty("multiHostSupport", Property.NULL);
            ac.setStatus("Account invalid");
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
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        String url = Encoding.urlEncode(link.getDownloadURL());
        br.setFollowRedirects(true);
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        showMessage(link, "Phase 1/2: Get download link");
        String genlink = br.getPage("http://www." + hostPublicDomain + "/api/get_ddl.php?login=" + user + "&password=" + pw + "&url=" + url);
        String maxChunksString = br.getRequest().getResponseHeader("X-MaxChunks");
        // Tested with share-online.biz: max 4 chunks possible
        int maxChunks = 1;
        if (link.getBooleanProperty(NOCHUNKS, false) && !true) {
            maxChunks = 1;
        } else if (maxChunksString != null) {
            try {
                maxChunks = -(Integer.parseInt(maxChunksString));
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
            }
        }
        if (!genlink.startsWith("http://")) {
            logger.severe(hostPublicDomain + "(Error): " + genlink);
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            if (link.getLinkStatus().getRetryCount() >= 3) {
                /* reset retry counter */
                link.getLinkStatus().setRetryCount(0);
                /* disable hoster for one hour */
                tempUnavailableHoster(acc, link, 60 * 60 * 1000);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry in few secs" + msg, 10 * 1000l);
        }
        showMessage(link, "Phase 2/2: Start download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, genlink, true, maxChunks);
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String serverfilename = getFileNameFromHeader(dl.getConnection());
        if ("Too_many_open_connections.txt".equals(serverfilename)) {
            logger.info("received 'Too_many_open_connections.txt' as server filename...");
            /* unknown error, we disable multiple chunks */
            if (link.getBooleanProperty(StreamManiaCom.NOCHUNKS, false) == false) {
                link.setProperty(StreamManiaCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* unknown error */
            br.followConnection();
            logger.severe(hostPublicDomain + "(Error): " + br.toString());
            /* disable for 20 min */
            tempUnavailableHoster(acc, link, 20 * 60 * 1000);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("final downloadlink leads to html code...");
            br.followConnection();
            logger.info(this.getHost() + ": unknowndlerror");
            int timesFailed = link.getIntegerProperty("timesfailed" + FAIL_STRING + "_unknowndlerror", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailed" + FAIL_STRING + "_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                link.setProperty("timesfailed" + FAIL_STRING + "_unknowndlerror", Property.NULL);
                logger.info(this.getHost() + ": unknowndlerror --> Remporarily desabling current host");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000);
            }
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(StreamManiaCom.NOCHUNKS, false) == false) {
                    link.setProperty(StreamManiaCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(StreamManiaCom.NOCHUNKS, false) == false) {
                link.setProperty(StreamManiaCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
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