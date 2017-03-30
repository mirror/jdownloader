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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.components.antiDDoSForHost;

/**
 * 24.11.15 Update by Bilal Ghouri:
 *
 * - Host has removed captcha and added attempts-account based system. API calls have been updated as well.<br />
 * - Cookies are valid for 30 days after last use. After that, Session Expired error will occur. In which case, Login() should be called to
 * get new cookies and store them for further use. <br />
 * - Better Error handling through exceptions.
 *
 * @author raztoki
 * @author psp
 * @author bilalghouri
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class LinkSnappyCom extends antiDDoSForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public LinkSnappyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://linksnappy.com/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://linksnappy.com/tos";
    }

    private static final String USE_API                = "USE_API";
    private static final String CLEAR_DOWNLOAD_HISTORY = "CLEAR_DOWNLOAD_HISTORY";
    private static final int    MAX_DOWNLOAD_ATTEMPTS  = 10;
    private int                 i                      = 1;
    private DownloadLink        currentLink            = null;
    private Account             currentAcc             = null;
    private boolean             resumes                = true;
    private int                 chunks                 = 0;
    private String              dllink                 = null;

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        return api_fetchAccountInfo(account);
    }

    private AccountInfo api_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        ArrayList<String> supportedHosts = new ArrayList<String>();
        /** Load cookies */
        final Cookies cookies;
        synchronized (account) {
            cookies = account.loadCookies("");
        }
        if (cookies != null) {
            br.setCookies(this.getHost(), cookies);
            getPage("https://linksnappy.com/api/USERDETAILS");
        }
        if (cookies == null || br.containsHTML("Session Expired")) {
            login(currentAcc);
            getPage("https://linksnappy.com/api/USERDETAILS");
        }
        if ("ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
            final String error = PluginJSonUtils.getJsonValue(br, "error");
            ac.setStatus(error);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String accountType = null;
        final String expire = PluginJSonUtils.getJsonValue(br, "expire");
        if ("lifetime".equals(expire)) {
            accountType = "Lifetime Premium Account";
            currentAcc.setType(AccountType.LIFETIME);
        } else if ("expired".equals(expire)) {
            /* Free account = also expired */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!\r\nIf your account is Premium contact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            ac.setValidUntil(Long.parseLong(expire) * 1000);
            accountType = "Premium Account";
            currentAcc.setType(AccountType.PREMIUM);
        }
        ac.setStatus(accountType);
        /* Find traffic left */
        final String trafficLeft = PluginJSonUtils.getJsonValue(br, "trafficleft");
        final String maxtraffic = PluginJSonUtils.getJsonValue(br, "maxtraffic");
        if ("unlimited".equals(trafficLeft)) {
            ac.setUnlimitedTraffic();
        } else {
            /* Also check for negative traffic */
            if (trafficLeft.contains("-")) {
                ac.setTrafficLeft(0);
            } else {
                ac.setTrafficLeft(Long.parseLong(trafficLeft));
            }
            if (maxtraffic != null) {
                ac.setTrafficMax(Long.parseLong(maxtraffic));
            }
        }
        /* now it's time to get all supported hosts */
        getPage("https://linksnappy.com/api/FILEHOSTS");
        if ("ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
            final String error = PluginJSonUtils.getJsonValue(br, "error");
            if ("Account has exceeded the daily quota".equals(error)) {
                dailyLimitReached();
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + error, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String hostText = br.getRegex("\\{\"status\":\"OK\",\"error\":false,\"return\":\\{(.*?\\})\\}").getMatch(0);
        if (hostText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // connection info map
        final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
        String[] hosts = new Regex(hostText, "([a-z0-9\\-]+\\.){1,}([a-z]{2,4})[^\\}]+\\}").getColumn(-1);
        for (final String hostInfo : hosts) {
            HashMap<String, Object> e = new HashMap<String, Object>();
            final String host = new Regex(hostInfo, "[^\"]+").getMatch(-1);
            if (hosts == null) {
                continue;
            }
            final String status = PluginJSonUtils.getJsonValue(hostInfo, "Status");
            String quota = PluginJSonUtils.getJsonValue(hostInfo, "Quota");
            if (quota != null) {
                if (quota.matches("\\d+")) {
                    e.put("quota", Long.parseLong(quota));
                } else if ("unlimited".equalsIgnoreCase(quota)) {
                    e.put("quota", -1);
                } else {
                    // this should not happen.
                    logger.warning("Possible plugin defect!");
                }
            }
            String usage = PluginJSonUtils.getJsonValue(hostInfo, "Usage");
            if (usage != null) {
                e.put("usage", Long.parseLong(usage));
            }
            final String resumes = PluginJSonUtils.getJsonValue(hostInfo, "resume");
            if (resumes != null) {
                e.put("resumes", (resumes.matches("\\d+") && Integer.parseInt(resumes) == 1 ? true : false));
            }
            final String connlimit = PluginJSonUtils.getJsonValue(hostInfo, "connlimit");
            e.put("chunks", (connlimit != null && connlimit.matches("\\d+") ? Integer.parseInt(connlimit) : 0));
            if (!e.isEmpty()) {
                con.put(host, e);
            }
            if (!"1".equals(status)) {
                continue;
            } else if ((usage != null && quota != null && !"unlimited".equals(quota)) && (Long.parseLong(quota) - Long.parseLong(usage)) <= 0) {
                continue;
            } else if (host != null) {
                supportedHosts.add(host);
            }
        }
        currentAcc.setProperty("accountProperties", con);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    private void dailyLimitReached() throws PluginException {
        final String host = br.getRegex("You have exceeded the daily ([a-z0-9\\-\\.]+) Download quota \\(").getMatch(0);
        if (host != null) {
            /* Daily specific host downloadlimit reached --> Disable host for some time */
            logger.info("Daily limit reached for host: " + host);
            logger.info("--> Temporarily Disabling " + host);
            tempUnavailableHoster(currentAcc, currentLink, 10 * 60 * 1000l);
        } else {
            /* Daily total downloadlimit for account is reached */
            final String lang = System.getProperty("user.language");
            logger.info("Daily limit reached");
            /* Workaround for account overview display bug so users see at least that there is no traffic left */
            this.currentAcc.getAccountInfo().setTrafficLeft(0);
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nTageslimit erreicht!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDaily limit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
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
        long tt = link.getLongProperty("filezize", -1);
        if (link.getView().getBytesLoaded() <= 0 || tt == -1) {
            long a = link.getView().getBytesTotalEstimated();
            if (a != -1) {
                link.setProperty("filezize", a);
                tt = a;
            }
        }
        /** Load cookies */
        setConstants(account, link);
        // br.setCookiesExclusive(true);
        final Cookies cookies;
        synchronized (account) {
            cookies = account.loadCookies("");
        }
        if (cookies != null) {
            br.setCookies(this.getHost(), cookies);
        } else {
            login(currentAcc);
        }
        dllink = link.getStringProperty("linksnappycomdirectlink", null);
        if (dllink != null) {
            dllink = (attemptDownload() ? dllink : null);
        }
        if (dllink == null) {
            /* Reset value because otherwise if attempts fail, JD will try again with the same broken dllink. */
            link.setProperty("linksnappycomdirectlink", Property.NULL);
            for (i = 1; i <= MAX_DOWNLOAD_ATTEMPTS; i++) {
                getPage("https://linksnappy.com/api/linkgen?genLinks=" + encode("{\"link\"+:+\"" + Encoding.urlEncode(link.getDownloadURL()) + "\"}"));
                if (br.containsHTML("Session Expired")) {
                    login(currentAcc);
                    getPage("https://linksnappy.com/api/linkgen?genLinks=" + encode("{\"link\"+:+\"" + Encoding.urlEncode(link.getDownloadURL()) + "\"}"));
                }
                if (!attemptDownload()) {
                    continue;
                }
                break;
            }
        }
        if (dl.getConnection() != null && dl.getConnection().getResponseCode() == 503) {
            stupidServerError();
        } else if (dl.getConnection() != null && dl.getConnection().getResponseCode() == 999) {
            br.followConnection();
            dailyLimitReached();
        } else if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection() != null) {
                br.followConnection();
            }
            logger.info("Unknown download error");
            int timesFailed = link.getIntegerProperty("timesfailedlinksnappycom_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedlinksnappycom_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info("Unknown download error -> Disabling current host");
                link.setProperty("timesfailedlinksnappycom_unknowndlerror", Property.NULL);
                tempUnavailableHoster(account, link, 5 * 60 * 1000l);
            }
        }
        link.setProperty("linksnappycomdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
            } else {
                /*
                 * Check if user wants JD to clear serverside download history in linksnappy account after each download - only possible via
                 * account - also make sure we get no exception as our download was successful NOTE: Even failed downloads will appear in
                 * the download history - but they will also be cleared once you have one successful download.
                 */
                if (this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY, default_clear_download_history)) {
                    boolean history_deleted = false;
                    try {
                        getPage("https://linksnappy.com/api/DELETELINK?type=filehost&hash=all");
                        if ("OK".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "status"))) {
                            history_deleted = true;
                        }
                    } catch (final Throwable e) {
                        history_deleted = false;
                    }
                    try {
                        if (history_deleted) {
                            logger.warning("Delete history succeeded!");
                        } else {
                            logger.warning("Delete history failed");
                        }
                    } catch (final Throwable e2) {
                    }
                }
            }
        } catch (final PluginException e) {
            if (e.getMessage() != null && e.getMessage().contains("java.lang.ArrayIndexOutOfBoundsException")) {
                if ((tt / 10) > currentLink.getView().getBytesTotal()) {
                    // this is when linksnappy dls text as proper filename
                    System.out.print("bingo");
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Problem with multihoster");
                }
            }
            throw e;
        }
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

    private boolean attemptDownload() throws Exception {
        if (br != null && br.getHttpConnection() != null) {
            if ("FAILED".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "status"))) {
                final String err = PluginJSonUtils.getJsonValue(br, "error");
                if (err != null && !err.matches("\\s*") && !"".equalsIgnoreCase(err)) {
                    if ("ERROR Code: 087".equalsIgnoreCase(err)) {
                        // "status":"FAILED","error":"ERROR Code: 087"
                        // I assume offline (webui says his host is offline, but not the api host list.
                        tempUnavailableHoster(currentAcc, currentLink, 10 * 60 * 1000l);
                    } else if (new Regex(err, "Invalid .*? link\\. Cannot find Filename\\.").matches()) {
                        logger.info("Error: Disabling current host");
                        tempUnavailableHoster(currentAcc, currentLink, 5 * 60 * 1000);
                    } else if (new Regex(err, "Invalid file URL format\\.").matches()) {
                        /*
                         * Update by Bilal Ghouri: Should not disable at this error, it means the host is online but the link format is not
                         * added on linksnappy, the user should report the link in this case.
                         */
                        // logger.info("Disabling current host");
                        // tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000);
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Link format is unknown. Report this link on LinkSnappy.");
                    } else if (new Regex(err, "File not found").matches()) {
                        if (i + 1 == MAX_DOWNLOAD_ATTEMPTS) {
                            // multihoster is not trusted source for offline...
                            logger.warning("Maybe the Hoster link is really offline! Confirm in browser!");
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        /* we just try again */
                        logger.info("Attempt failed: 'file not found' error");
                        return false;
                    }
                }
            }
            dllink = PluginJSonUtils.getJsonValue(br, "generated");
            if (dllink == null) {
                logger.info("Direct downloadlink not found");
                int timesFailed = currentLink.getIntegerProperty("timesfailedlinksnappycom_dllinkmissing", 0);
                currentLink.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    currentLink.setProperty("timesfailedlinksnappycom_dllinkmissing", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error - final downloadlink not found");
                } else {
                    logger.info("Direct downloadlink not found -> Disabling current host");
                    currentLink.setProperty("timesfailedlinksnappycom_dllinkmissing", Property.NULL);
                    tempUnavailableHoster(currentAcc, currentLink, 5 * 60 * 1000l);
                }
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, currentLink, dllink, resumes, chunks);
        } catch (final SocketTimeoutException e) {
            final boolean timeoutedBefore = currentLink.getBooleanProperty("sockettimeout");
            if (timeoutedBefore) {
                currentLink.setProperty("sockettimeout", false);
                throw e;
            }
            currentLink.setProperty("sockettimeout", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } catch (final BrowserException ebr) {
            logger.info("Attempt failed: Got BrowserException for link: " + dllink);
            return false;
        }
        if (dl.getConnection() != null && dl.getConnection().getResponseCode() == 503) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            logger.info("Attempt failed: Got 503 error for link: " + dllink);
            return false;
        }
        return true;
    }

    private void login(final Account account) throws Exception {
        synchronized (account) {
            this.br = new Browser();
            this.br.setCookiesExclusive(true);
            getPage("https://linksnappy.com/api/AUTHENTICATE?" + "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final String ResponseStatus = PluginJSonUtils.getJsonValue(br, "status");
            if ("ERROR".equals(ResponseStatus)) {
                final String ErrorMessage = PluginJSonUtils.getJsonValue(br, "error");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + ErrorMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("OK".equals(ResponseStatus)) {
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
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
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    private String encode(String value) {
        value = value.replace("\"", "%22");
        value = value.replace(":", "%3A");
        value = value.replace("{", "%7B");
        value = value.replace("}", "%7D");
        value = value.replace(",", "%2C");
        return value;
    }

    // Max 10 retries via link, 5 seconds waittime between = max 2 minutes trying -> Then deactivate host
    private void stupidServerError() throws PluginException {
        // it's only null on login
        if (currentLink == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
        }
        int timesFailed = currentLink.getIntegerProperty("timesfailedlinksnappy", 0);
        if (timesFailed <= 9) {
            timesFailed++;
            currentLink.setProperty("timesfailedlinksnappy", timesFailed);
            // Only wait 10 seconds because without forcing it, these servers will always bring up errors
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
        } else {
            currentLink.setProperty("timesfailedlinksnappy", Property.NULL);
            tempUnavailableHoster(currentAcc, currentLink, 5 * 60 * 1000l);
        }
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setConnectTimeout(30 * 1000);
            prepBr.setReadTimeout(30 * 1000);
            prepBr.setAllowedResponseCodes(999);
            prepBr.getHeaders().put("User-Agent", "JDownloader");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private final boolean default_api                    = true;
    private final boolean default_clear_download_history = false;

    public void setConfigElements() {
        final ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_API, JDL.L("plugins.hoster.linksnappycom.useAPI", "Use API (recommended)?")).setDefaultValue(default_api);
        getConfig().addEntry(ce);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY, JDL.L("plugins.hoster.linksnappycom.clear_serverside_download_history", "Clear download history in linksnappy account after each successful download?")).setDefaultValue(default_clear_download_history).setEnabledCondidtion(ce, false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}