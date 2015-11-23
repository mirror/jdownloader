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
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class LinkSnappyCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public LinkSnappyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HTTP_S + "linksnappy.com/members/index.php?act=register");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return HTTP_S + "linksnappy.com/index.php?act=tos";
    }

    private static Object       LOCK                   = new Object();
    private static final String USE_API                = "USE_API";
    private static final String CLEAR_DOWNLOAD_HISTORY = "CLEAR_DOWNLOAD_HISTORY";

    private static final String COOKIE_HOST            = "http://linksnappy.com";
    private static final String HTTP_S                 = "https://";
    private static final int    MAX_DOWNLOAD_ATTEMPTS  = 10;
    private int                 i                      = 1;

    private DownloadLink        currentLink            = null;
    private Account             currentAcc             = null;
    private boolean             resumes                = true;
    private int                 chunks                 = 0;

    private String              dllink                 = null;

    /* 75 GB, Last checked: 18.06.2015 */
    private static final long   default_traffic_max    = 80530636800L;
    public static final long    site_trust_cookie_age  = 30000l;
    /* Last checked: 18.06.2015 */
    private static final String reCaptchaV2_sitekey    = "6LfhJgQTAAAAAIM7Pz3XxW1QMWssU51lcN-kUDRA";

    /**
     * Status 22.06.15: Host has ddos problems. Along with that they added login captchas. For the website version of the plugin that works
     * fine - not so for the API login. Admin does not want to add it for API as well so as long as it only occurs in rare cases users
     * should barely notice that.
     */
    /*
     * TODO: Implement correct connection limits - admin said, max 36 connectins per file (no total limit), waiting for more input and/or
     * even api implementation
     */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac;
        setConstants(account, null);
        prepBrowser(br);
        if (api_active()) {
            ac = api_fetchAccountInfo();
        } else {
            ac = site_fetchAccountInfo();
        }
        account.setMaxSimultanDownloads(-1);
        account.setValid(true);
        return ac;
    }

    private AccountInfo api_fetchAccountInfo() throws Exception {
        final AccountInfo ac = new AccountInfo();
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final String lang = System.getProperty("user.language");
        try {
            if (!api_login(currentAcc)) {
                ac.setStatus("Account is invalid. Wrong username or password?");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin Server-Fehler!\r\nBitte versuche es später erneut!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin server-error!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            }
            throw e;
        }
        String accountType = null;
        final String expire = br.getRegex("\"expire\":\"([^<>\"]*?)\"").getMatch(0);
        if ("lifetime".equals(expire)) {
            accountType = "Lifetime Premium account";
        } else if ("expired".equals(expire)) {
            /* Free account = also expired */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!\r\nIf your account is Premium contact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            ac.setValidUntil(Long.parseLong(expire) * 1000);
            accountType = "Premium account";
        }
        /* = all are premium anyways */
        currentAcc.setType(AccountType.PREMIUM);
        ac.setStatus(accountType);
        /* Find traffic left */
        if (br.containsHTML("\"trafficleft\":\"unlimited\"")) {
            ac.setUnlimitedTraffic();
        } else {
            final String trafficLeft = br.getRegex("\"trafficleft\":((\\-)?[0-9\\.]+)").getMatch(0);
            /* Also check for negative traffic */
            if (trafficLeft.contains("-")) {
                ac.setTrafficLeft(0);
            } else {
                ac.setTrafficLeft(Long.parseLong(trafficLeft));
            }
            /* API does not (yet) return max daily traffic - use default value! */
            ac.setTrafficMax(default_traffic_max);
        }

        /* now it's time to get all supported hosts */
        getPage("/lseAPI.php?act=FILEHOSTS&username=" + Encoding.urlEncode(currentAcc.getUser()) + "&password=" + JDHash.getMD5(currentAcc.getPass()));
        if (br.containsHTML("\"error\":\"Account has exceeded")) {
            dailyLimitReached();
        }
        final String hostText = br.getRegex("\\{\"status\":\"OK\",\"error\":false,\"return\":\\{(.*?\\})\\}\\}").getMatch(0);
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
            final String status = getJson(hostInfo, "Status");
            String quota = getJson(hostInfo, "Quota");
            if ("null".equalsIgnoreCase(quota)) {
                quota = null;
            }
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
            String usage = getJson(hostInfo, "Usage");
            if ("null".equalsIgnoreCase(usage)) {
                usage = null;
            }
            if (usage != null) {
                e.put("usage", Long.parseLong(usage));
            }
            final String resumes = getJson(hostInfo, "resume");
            if (resumes != null) {
                e.put("resumes", (resumes.matches("\\d+") && Integer.parseInt(resumes) == 1 ? true : false));
            }
            final String connlimit = getJson(hostInfo, "connlimit");
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
        supportedHosts.remove("mega.co.nz");// buggy support
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
            tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000l);
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

    private AccountInfo site_fetchAccountInfo() throws Exception {
        final AccountInfo ac = new AccountInfo();
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final String lang = System.getProperty("user.language");
        try {
            if (!site_login(currentAcc)) {
                ac.setStatus("Account is invalid. Wrong username/password or login captcha?!");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin Server-Fehler!\r\nBitte versuche es später erneut!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLogin server-error!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            }
            throw e;
        }
        /* Via site, only lifetime is supported at the moment */
        String accountType = "Lifetime Premium account";
        ac.setStatus(accountType);

        /* Find traffic left */
        if (br.containsHTML("<strong>Daily traffic left:</strong> Unlimited")) {
            ac.setUnlimitedTraffic();
        } else {
            String trafficleft = br.getRegex("<strong>Daily traffic left:</strong> (-?\\d+(\\.\\d+)? ?[A-Z]{1,2})").getMatch(0);
            if (trafficleft != null) {
                /* Also check for negative traffic */
                if (trafficleft.contains("-")) {
                    ac.setTrafficLeft(0);
                } else {
                    if (trafficleft.matches("-?\\d+(\\.\\d+)? ?G")) {
                        trafficleft += "B";
                    }
                    ac.setTrafficLeft(SizeFormatter.getSize(trafficleft));
                }
            }
            String max_traffic = null;
            try {
                br.getPage("/tos");
                max_traffic = br.getRegex("accounts to a daily transfer of (\\d+GB) a day").getMatch(0);
            } catch (final Throwable e) {
            }
            if (max_traffic != null) {
                ac.setTrafficMax(SizeFormatter.getSize(max_traffic));
            } else {
                ac.setTrafficMax(default_traffic_max);
            }
        }

        /* now it's time to get all supported hosts */
        // present on most pages
        String[] hosts = br.getRegex("style=\" background: url\\(/templates/images/filehosts/small/(([a-z0-9\\-]+\\.){1,}([a-z]{2,4}))\\.png\\)").getColumn(0);
        if (hosts != null && hosts.length != 0) {
            for (final String host : hosts) {
                supportedHosts.add(host);
            }
        }
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
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
        prepBrowser(br);
        setConstants(account, link);
        final boolean use_api = api_active();
        dllink = link.getStringProperty("linksnappycomdirectlink", null);
        if (dllink != null) {
            dllink = (attemptDownload() ? dllink : null);
        }
        if (dllink == null) {
            /* Reset value because otherwise if attempts fail, JD will try again with the same broken dllink. */
            link.setProperty("linksnappycomdirectlink", Property.NULL);
            if (use_api) {
                for (i = 1; i <= MAX_DOWNLOAD_ATTEMPTS; i++) {
                    getPage(HTTP_S + "gen.linksnappy.com/genAPI.php?genLinks=" + encode("{\"link\"+:+\"" + Encoding.urlEncode(link.getDownloadURL()) + "\",+\"username\"+:+\"" + Encoding.urlEncode(account.getUser()) + "\",+\"password\"+:+\"" + Encoding.urlEncode(account.getPass()) + "\"}"));
                    if (!attemptDownload()) {
                        continue;
                    }
                    break;
                }
            } else {
                this.site_login(account);
                for (i = 1; i <= MAX_DOWNLOAD_ATTEMPTS; i++) {
                    /*
                     * IMPORTANT: Even though we're on the site here, https is not forced here - last time I checked it did not even work
                     * via https (20.05.14)
                     */
                    getPage(HTTP_S + "gen.linksnappy.com/genAPI.php?callback=jQuery" + System.currentTimeMillis() + "_" + System.currentTimeMillis() + "&genLinks=%7B%22link%22+%3A+%22" + Encoding.urlEncode(link.getDownloadURL()) + "%22%2C+%22type%22+%3A+%22%22%2C+%22linkpass%22+%3A+%22%22%2C+%22fmt%22+%3A+%2235%22%2C+%22ytcountry%22+%3A+%22usa%22%7D&_=" + System.currentTimeMillis());
                    if (br.containsHTML("\"status\": \"Error\"")) {
                        if (br.containsHTML("\"error\": \"Unauthorized\"")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAPI problems 'Unauthorized'!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnknown problem!\r\nPlease try again later!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    if (!attemptDownload()) {
                        continue;
                    }
                    break;
                }
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
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
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
                // disabled at the request of linksnappy admin. This shouldn't be needed for misconfiguration of chunks, as they provide
                // chunk values.
                // disabling chunks on first error is bad, one could have network issue/or harddrive issue with preallocation of filesize.
                // It
                // doesn't justify resetting chunk value on the first error! - raztoki
                //
                // /* unknown error, we disable multiple chunks */
                // if (link.getBooleanProperty(LinkSnappyCom.NOCHUNKS, false) == false) {
                // link.setProperty(LinkSnappyCom.NOCHUNKS, Boolean.valueOf(true));
                // throw new PluginException(LinkStatus.ERROR_RETRY);
                // }
            } else {
                /*
                 * Check if user wants JD to clear serverside download history in linksnappy account after each download - only possible via
                 * account - also make sure we get no exception as our download was successful NOTE: Even failed downloads will appear in
                 * the download history - but they will also be cleared once you have one successful download.
                 */
                if (!use_api && this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY, default_clear_download_history)) {
                    boolean history_deleted = false;
                    try {
                        br.getPage(HTTP_S + "linksnappy.com/includes/deletelinks.php?id=all");
                        if (br.toString().trim().equals("OK")) {
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
            // // New V2 errorhandling
            // /* unknown error, we disable multiple chunks */
            // if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(LinkSnappyCom.NOCHUNKS, false) == false) {
            // link.setProperty(LinkSnappyCom.NOCHUNKS, Boolean.valueOf(true));
            // throw new PluginException(LinkStatus.ERROR_RETRY);
            // }
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
            if ("FAILED".equalsIgnoreCase(getJson("status"))) {
                final String err = getJson("error");
                if (err != null || !err.matches("\\s*") || !"".equalsIgnoreCase(err)) {
                    if ("ERROR Code: 087".equalsIgnoreCase(err)) {
                        // "status":"FAILED","error":"ERROR Code: 087"
                        // I assume offline (webui says his host is offline, but not the api host list.
                        tempUnavailableHoster(currentAcc, currentLink, 1 * (60 * 60 * 1000l));
                    } else if (new Regex(err, "Invalid .*? link\\. Cannot find Filename\\.").matches()) {
                        logger.info("Error: Disabling current host");
                        tempUnavailableHoster(currentAcc, currentLink, 5 * 60 * 1000);
                    } else if (new Regex(err, "Invalid file URL format\\.").matches()) {
                        logger.info("Disabling current host");
                        tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000);
                    } else if (new Regex(err, "File not found").matches()) {
                        if (i + 1 == MAX_DOWNLOAD_ATTEMPTS) {
                            // multihoster is not trusted source for offline...
                            logger.warning("Maybe the Hoster link is really offline! Confirm in browser!");
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        /* Bullshit, we just try again */
                        logger.info("Attempt failed: bullshit 'file not found' error");
                        return false;
                    }
                }
            }

            dllink = getJson("generated");
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
                    tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000l);
                }
            }
        }
        // shouldn't be needed! linksnappy now provides chunk values in hostmap.
        // chunks = (currentLink.getBooleanProperty(NOCHUNKS, false) ? 1 : chunks);

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

    private boolean api_login(final Account account) throws Exception {
        this.br.setCookiesExclusive(true);
        getPage(HTTP_S + "gen.linksnappy.com/lseAPI.php?act=USERDETAILS");
        String postdata = "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + JDHash.getMD5(account.getPass());
        if (this.br.containsHTML("\"error\":\"CAPTCHA\"")) {
            final DownloadLink dummy = new DownloadLink(this, "Account", "linksnappy.com", "http://linksnappy.com", true);
            if (this.getDownloadLink() == null) {
                this.setDownloadLink(dummy);
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaV2_sitekey).getToken();
            postdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
        }
        getPage(HTTP_S + "gen.linksnappy.com/lseAPI.php?act=USERDETAILS" + postdata);
        if (br.containsHTML("\"status\":\"ERROR\"")) {
            return false;
        }
        return true;
    }

    private boolean site_login(final Account account) throws Exception {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            prepBrowser(this.br);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                this.br.setCookies(this.getHost(), cookies);
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= site_trust_cookie_age) {
                    /* We trust these cookies --> Do not check them */
                    return true;
                }
                this.br.getPage("https://linksnappy.com/myaccount?lang=en");
                if (br.containsHTML("id=\"signedin\"")) {
                    /* Refresh timestamp */
                    account.saveCookies(br.getCookies(COOKIE_HOST), "");
                    return true;
                }
                logger.info("Failed to re-use saved cookies - performing full login (captcha might be needed)");
                /* Remove old cookies & Headers */
                this.br = new Browser();
            }
            String getdata = "?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
            br.getPage(HTTP_S + "linksnappy.com/");
            if (br.containsHTML("grecaptcha\\.render") && !this.br.containsHTML("// regCaptcha = grecaptcha\\.render\\(\\'regCaptcha\\', \\{")) {
                final DownloadLink dummy = new DownloadLink(this, "Account", "linksnappy.com", "http://linksnappy.com", true);
                if (this.getDownloadLink() == null) {
                    this.setDownloadLink(dummy);
                }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaV2_sitekey).getToken();
                getdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
            }
            this.br.getPage("https://linksnappy.com/api/AUTHENTICATE" + getdata);
            if (br.getCookie(COOKIE_HOST, "Auth") == null || this.br.containsHTML("\"error\":true")) {
                return false;
            }
            /* Valid account --> Check if the account type is supported */
            br.getPage("/myaccount");
            if (br.containsHTML("<strong>Account Type:</strong>[\t\n\r ]+Free")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!\r\nIf your account is Premium contact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            /* Unsupported account type? */
            if (!br.containsHTML("<strong>Account Type:</strong>[\t\n\r ]+(?:Lifetime|Elite)")) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }

            account.saveCookies(this.br.getCookies(this.getHost()), "");
            return true;
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
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

    private void getPage(final String page) throws IOException, PluginException {
        boolean failed = true;
        for (int i = 1; i <= 10; i++) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(page);
                if (con.getResponseCode() == 503) {
                    logger.info("Try " + i + ": Got 503 error for link: " + page);
                    continue;
                }
                br.followConnection();
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            failed = false;
            break;
        }
        if (failed) {
            stupidServerError();
        }
    }

    private void postPageSecure(final String page, final String postData) throws IOException, PluginException {
        boolean failed = true;
        for (int i = 1; i <= 10; i++) {
            URLConnectionAdapter con = null;
            try {
                con = br.openPostConnection(page, postData);
                if (con.getResponseCode() == 503) {
                    logger.info("Try " + i + ": Got 503 error for link: " + page);
                    continue;
                }
                br.followConnection();
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            failed = false;
            break;
        }
        if (failed) {
            stupidServerError();
        }
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
            tempUnavailableHoster(currentAcc, currentLink, 60 * 60 * 1000l);
        }
    }

    private Browser prepBrowser(final Browser prepBr) {
        prepBr.setConnectTimeout(60 * 1000);
        prepBr.setReadTimeout(60 * 1000);
        prepBr.setAllowedResponseCodes(999);
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCookie(COOKIE_HOST, "lang", "en");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private boolean api_active() {
        return this.getPluginConfig().getBooleanProperty(USE_API, default_api);
    }

    private final boolean default_api                    = true;
    private final boolean default_clear_download_history = false;

    public void setConfigElements() {
        final ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_API, JDL.L("plugins.hoster.linksnappycom.useAPI", "Use API (recommended)?")).setDefaultValue(default_api);
        getConfig().addEntry(ce);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY, JDL.L("plugins.hoster.linksnappycom.clear_serverside_download_history", "Clear download history in linksnappy account after each successful download?")).setDefaultValue(default_clear_download_history).setEnabledCondidtion(ce, false));
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}