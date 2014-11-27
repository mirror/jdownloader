//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.http.Browser;
import jd.http.StaticProxySelector;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fastix.ru" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
/**
 * @author pspzockerscene
 * @author raztoki
 *
 */
public class FastixRu extends PluginForHost {

    /** Using API: http://fastix.ru/apidoc */
    private final String                                   NOCHUNKS                     = "NOCHUNKS";
    private final String                                   DOMAIN                       = "http://fastix.ru/api_v2/";
    private final String                                   NICE_HOST                    = "fastix.ru";
    private final String                                   NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");

    /* Connection limits */
    private final boolean                                  ACCOUNT_PREMIUM_RESUME       = true;
    private final int                                      ACCOUNT_PREMIUM_MAXCHUNKS    = -2;
    private final int                                      ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    // used for checkDirectLink
    private boolean                                        supportsHeadConnection       = true;
    // this session info
    private int                                            statuscode                   = 0;
    private Account                                        account                      = null;
    private DownloadLink                                   downloadLink                 = null;

    public FastixRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fastix.ru/");
        /* Server returns 503 errors if we request too fast */
        this.setStartIntervall(5 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://fastix.ru/static/abuse";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.account = acc;
        this.downloadLink = dl;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        br = newBrowser();
        String dllink = checkDirectLink(NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            // from google translate
            // sub = getdirectlink - generation direct links 40 запросов в минуту
            //
            // Generates a direct link to the file
            // Sample query
            //
            // http://fastix.ru/api_v2/?apikey=[apikey]&sub=getdirectlink&link=[link]&ip=[ip_address]&ssl=[forced_ssl]
            // More Search Options
            //
            // [Ip] - receives an IP address from which permitted injection (if not specified, the pump may all)
            // [Ssl] - true if should be forced to download https
            // Please note: In the account profile can also be configured IP restrictions and forced SSL. But they will be ignored if
            // present in the API request these settings
            // Important Parameter [ip] can not take the address format IPv6, as well as masks (approx. 192.168. *. *)

            // IP might not even be needed in the scheme of things?? - raztoki
            final HTTPProxy proxyThatWillBeUsed = br.getProxy().getProxiesByUrl(DOMAIN).get(0);
            final String externalIP = new BalancedWebIPCheck(new StaticProxySelector(proxyThatWillBeUsed)).getExternalIP().getIP();
            /* External IP of the user is needed for this request, also enforce SSL */
            getAPISafe(DOMAIN + "?apikey=" + getAPIKEY() + "&sub=getdirectlink&link=" + JSonUtils.escape(link.getDownloadURL()) + "&ip=" + JSonUtils.escape(externalIP) + "&ssl=true");
            dllink = getJson("downloadlink");
            if (dllink == null) {
                handleErrorRetries("dllinknull", 5);
            }
        }
        handleDL(dllink);
    }

    private void handleDL(final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        downloadLink.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, maxChunks);
        try {
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                handleErrorRetries("unknowndlerror", 5);
            }
            try {
                if (!dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (downloadLink.getBooleanProperty(NOCHUNKS, false) == false) {
                        downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(NOCHUNKS, false) == false) {
                    downloadLink.setProperty(NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
        } catch (final Exception e) {
            downloadLink.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        } catch (final Throwable e) {
            downloadLink.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw new WTFException(e);
        }
    }

    private String checkDirectLink(final String property) {
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                if (supportsHeadConnection) {
                    con = br.openHeadConnection(dllink);
                } else {
                    con = br.openGetConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (Exception e) {
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        getAPISafe(DOMAIN + "?sub=get_apikey&email=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
        final String apikey = getJson("apikey");
        if (apikey == null) {
            // maybe unhandled error reason why apikey is null!
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty("fastixapikey", apikey);
        getAPISafe(DOMAIN + "?apikey=" + getAPIKEY() + "&sub=getaccountdetails");

        final String points = getJson("points");
        // null or parse exceptions will result in 0 traffic, users should complain and we can 'fix'
        long p = 0;
        try {
            p = Long.parseLong(points);
        } catch (final Exception e) {
        }
        p = p * 1024 * 1024;
        // prevent negative values from messing with traffic left.
        if (p < 0) {
            p = 0;
        }
        ai.setTrafficLeft(p);

        /*
         * Other methods to get this list: allowed_fileshares, allowed_sources - directing_status is the best as they refresh it every 15
         * minutes and it contains lists of working/non working and partially working services
         */
        getAPISafe(DOMAIN + "?apikey=" + getAPIKEY() + "&sub=directing_status");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] lists = { "partially", "up" };
        for (final String list : lists) {
            String jsonlist = getJsonArray(list);
            if (jsonlist != null) {
                logger.info("Adding hosts for list: " + list);
                final String[] hostDomains = getJsonResultsFromArray(jsonlist);
                if (hostDomains != null) {
                    supportedHosts.addAll(Arrays.asList(hostDomains));
                }
            } else {
                logger.info("NOT Adding hosts for list: " + list + " (EMPTY/UNAVAILABLE)");
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        /* All accounts are 'premium' but free accounts simply have ZERO traffic (points) */
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        ai.setStatus("Premium Account");
        return ai;
    }

    private String getAPIKEY() {
        return JSonUtils.escape(account.getStringProperty("fastixapikey", null));
    }

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors();
    }

    private void updatestatuscode() {
        final String stcode = getJson("error_code");
        if (stcode == null) {
            statuscode = 0;
        } else {
            statuscode = Integer.parseInt(stcode);
        }
    }

    private void handleAPIErrors() throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 100:
                statusMessage = "Link offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 101:
                /* No traffic left or server is full -> Disable for 1 hour */
                statusMessage = "Unsupported filehost";
                tempUnavailableHoster(3 * 60 * 60 * 1000l);
            case 102:
                statusMessage = "Filehost is currently unavailable";
                tempUnavailableHoster(15 * 60 * 60 * 1000l);
            case 103:
                /* This should never happen but if, let's disable the current host for some time */
                statusMessage = "Folderlinks of filehosts are not supported";
                tempUnavailableHoster(15 * 60 * 60 * 1000l);
            case 201:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 300:
                statusMessage = "Not enough traffic left to download (this file)";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 500:
                statusMessage = "Fatal API error: API limit reached";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 503:
                statusMessage = "API key invalid";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 504:
                statusMessage = "API key incompatible";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 505:
                /* Login or password invalid -> disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 506:
                statusMessage = "Too many requested - IP is banned for 5 minutes";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries("unknownAPIerror", 10);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = JSonUtils.unescape(result);
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

    /**
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(\\[[^\\]]+\\])").getMatch(0);
        if (result != null) {
            result = JSonUtils.unescape(result);
        }
        return result;
    }

    /**
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return getJsonArray(br.toString(), key);
    }

    /**
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        if (source == null) {
            return null;
        }
        final String[] result = new Regex(source, "\"([^\"]+)\"\\s*(?:,|\\])").getColumn(0);
        return result;
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries) throws PluginException {
        int timesFailed = downloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        downloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            downloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            downloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* TODO: Repalce this plugin_defect with the line above once the plugin is in a stable state */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}