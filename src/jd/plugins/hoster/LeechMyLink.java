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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.components.UnavailableHost;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision: 30649 $", interfaceVersion = 3, names = { "leechmy.link" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class LeechMyLink extends antiDDoSForHost {

    private static final HashMap<Account, HashMap<String, UnavailableHost>> hostUnavailableMap = new HashMap<Account, HashMap<String, UnavailableHost>>();
    private static final String                                             NICE_HOSTproperty  = "leechmylink";

    private Account                                                         currentAcc         = null;
    private DownloadLink                                                    currentLink        = null;
    private boolean                                                         resumes            = true;
    private int                                                             chunks             = 0;
    private String                                                          dllink             = null;

    public LeechMyLink(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://leechmy.link/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://leechmy.link/tos";
    }

    private void setConstants(final Account account, final DownloadLink downloadLink) throws PluginException {
        if (downloadLink == null && account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (downloadLink != null && account != null) {
            currentLink = downloadLink;
            currentAcc = account;
            final String dl_host = downloadLink.getDefaultPlugin().getHost();
            final Object ret = account.getProperty("accountProperties", null);
            if (ret != null && ret instanceof HashMap) {
                @SuppressWarnings("unchecked")
                final HashMap<String, HashMap<String, Object>> ap = (HashMap<String, HashMap<String, Object>>) ret;
                final HashMap<String, Object> h = ap.get(dl_host);
                if (h == null) {
                    // return defaults
                    return;
                }
                final int c = h.containsKey("chunks") ? ((Number) h.get("chunks")).intValue() : chunks;
                chunks = (c > 1 ? -c : c);
                final Boolean r = (Boolean) (h.containsKey("resumes") ? h.get("resumes") : resumes);
                if (Boolean.FALSE.equals(r) && chunks == 1) {
                    resumes = r;
                } else {
                    resumes = true;
                }
            }
        } else {
            currentAcc = account;
        }
    }

    protected Browser prepBrowser(final Browser prepBr) {
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        return prepBr;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        final AccountInfo ac = new AccountInfo();
        prepBrowser(br);
        login(account, true);
        final String type = getJson("account_type");
        final String expire_timestamp = getJson("expiry_timestamp");
        if ("premium".equalsIgnoreCase(type) && !inValidate(expire_timestamp)) {
            // unix timestamp is in seconds, we reference in milli
            ac.setValidUntil(Long.parseLong(expire_timestamp) * 1000, br);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            account.setType(AccountType.PREMIUM);
            ac.setStatus("Premium Account");
            ac.setUnlimitedTraffic();
        } else {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            account.setType(AccountType.FREE);
            ac.setStatus("Registered (free) account");
            /* 2016-01-28: No way to download with free accounts (anymore) */
            ac.setTrafficLeft(0);
        }
        // now let's get a list of all supported hosts:
        br.getPage("/api/filehosts");
        final String[] array = getJsonResultsFromArray(br.toString());
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        // connection info map
        final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
        for (final String a : array) {
            HashMap<String, Object> e = new HashMap<String, Object>();
            final String active = getJson(a, "active");
            final String host = getJson(a, "host");
            // no need to add if its not active/present.
            if (Boolean.parseBoolean(active) == false || inValidate(host) == true) {
                continue;
            }
            supportedHosts.add(host);
            final String resumable = getJson(a, "resumeable");
            if (resumable != null) {
                e.put("resumes", Boolean.parseBoolean(resumable));
            }
            final String maxChunks = getJson(a, "maxChunks");
            if (!inValidate(maxChunks)) {
                final int chunks = Integer.parseInt(maxChunks);
                e.put("chunks", chunks);
            }
            final String maxDownloads = getJson(a, "maxDownloads");
            if (!inValidate(maxDownloads)) {
                final int maxDl = Integer.parseInt(maxDownloads);
                e.put("maxSimDl", maxDl);
            }
            final String trafficLimit = getJson(a, "trafficlimit");
            if (!inValidate(trafficLimit)) {
                final int traffic = Integer.parseInt(trafficLimit);
                e.put("trafficLimit", traffic);
            }
            if (!e.isEmpty()) {
                con.put(host, e);
            }
        }
        currentAcc.setProperty("accountProperties", con);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifrd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(getHost(), key, value);
                        }
                        return;
                    }
                }

                br.postPage("http://leechmy.link/api/user", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if ("error".equalsIgnoreCase(getJson("status"))) {
                    final String msg = getJson("msg");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(getHost()));
            } catch (final PluginException e) {
                dumpAccountSessionInfo();
                throw e;
            } finally {
                br.setFollowRedirects(ifrd);
            }
        }
    }

    private void dumpAccountSessionInfo() throws PluginException {
        if (currentAcc == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        currentAcc.setProperty("name", Property.NULL);
        currentAcc.setProperty("password", Property.NULL);
        currentAcc.setProperty("cookies", Property.NULL);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(null);
            UnavailableHost nue = unavailableMap != null ? unavailableMap.get(link.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this multihoster: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(null);
                    }
                }
            }
            unavailableMap = hostUnavailableMap.get(account);
            nue = unavailableMap != null ? unavailableMap.get(link.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this account: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        prepBrowser(br);
        login(account, false);
        dllink = checkDirectLink(link, NICE_HOSTproperty + "_directlink");
        if (inValidate(dllink)) {
            // prevent ddos work around
            if (link.getBooleanProperty("hasFailed", false)) {
                final int hasFailedInt = link.getIntegerProperty("hasFailedWait", 60);
                // nullify old storeables
                link.setProperty("hasFailed", Property.NULL);
                link.setProperty("hasFailedWait", Property.NULL);
                sleep(hasFailedInt * 1001, link);
            }
            br.postPage("http://leechmy.link/api/leecher", "link=" + Encoding.urlEncode(link.getDownloadURL()) + (link.getDownloadPassword() != null ? "&lpass=" + Encoding.urlEncode(link.getDownloadPassword()) : ""));
            /*
             * Errorresponse on free account download attempt: {"status":"error","msg":"You are not logged in"
             * ,"link":"http:...","isVideo":false}
             */
            dllink = getJson("download_link");
            if ("error".equalsIgnoreCase(getJson("status"))) {
                final String msg = getJson("msg");
                if (StringUtils.startsWithCaseInsensitive(msg, "Received HTTP error code from filehost")) {
                    // {"status":"error","msg":"Received HTTP error code from filehost. Please Retry.","link":"urlremoved"}
                    handleErrorRetries(msg, 10, 15 * 60 * 1000l);
                } else if (StringUtils.startsWithCaseInsensitive(msg, "Link dead")) {
                    // {"status":"error","msg":"Link dead (2)","link":"urlremoved"}
                    // in test environment, they reported false positive.. we can not trust this multihoster to report correctly
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 30 * 60 * 1000l);
                } else if (StringUtils.startsWithCaseInsensitive(msg, "Failed to generate.")) {
                    // {"status":"error","msg":"Failed to generate. Please try again.","link":"urlremoved","isVideo":false}
                    handleErrorRetries(msg, 5, 15 * 60 * 1000l);
                } else if (StringUtils.startsWithCaseInsensitive(msg, "Link is not recognized")) {
                    // {"status":"error","msg":"Link is not recognized","link":"urlremoved","isVideo":false}
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 30 * 60 * 1000l);
                } else if (StringUtils.endsWithCaseInsensitive(msg, "You are not logged in")) {
                    // {"status":"error","msg":"You are not logged in","link":"http:\/\/www.share-online.biz\/dl\/....","isVideo":false}
                    // dump cookies
                    dumpAccountSessionInfo();
                    handleErrorRetries(msg, 2, 10 * 60 * 1000l);
                }
            }
            if (inValidate(dllink)) {
                // unhandled erorr?
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumes, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(NICE_HOSTproperty + "_directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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
        int timesFailed = currentLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info(error + " -> Retrying");
            timesFailed++;
            this.currentLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            // prevent ddos
            this.currentLink.setProperty("hasFailed", true);
            this.currentLink.setProperty("hasFailedWait", 60);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currentLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(error + " -> Disabling current host");
            tempUnavailableHoster(disableTime, error);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final long timeout, final String reason) throws PluginException {
        if (this.currentLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }

        final UnavailableHost nue = new UnavailableHost(System.currentTimeMillis() + timeout, reason);

        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(this.currentAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, UnavailableHost>();
                hostUnavailableMap.put(this.currentAcc, unavailableMap);
            }
            unavailableMap.put(this.currentLink.getHost(), nue);
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}