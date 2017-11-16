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
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "leech360.com" }, urls = { "" })
public class Leech360Com extends PluginForHost {
    private static final String                            NICE_HOST                    = "leech360.com";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final String                                   default_UA                   = "JDownloader";
    private final String                                   html_loggedin                = "id=\"linkpass\"";
    private static Object                                  LOCK                         = new Object();
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public Leech360Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://leech360.com/payment.html");
    }

    @Override
    public String getAGBLink() {
        return "https://leech360.com/terms-of-service.html";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", default_UA);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
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
        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.postAPISafe("https://" + this.getHost() + "/generate", "link_password=&link=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = PluginJSonUtils.getJsonValue(this.br, "download_url");
            if (StringUtils.isEmpty(dllink)) {
                /* E.g. "error_message":"some.filehost current offline or not support this time!" */
                handleErrorRetries("dllinknull", 5, 2 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 5, 2 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /*
         * Use 2nd browser object as we already got the page containing our supported hosts inside the login process and we do not want to
         * access it twice.
         */
        final Browser br2 = br.cloneBrowser();
        br2.getPage("/my-account.html");
        final String expire = br2.getRegex("Premium until: <strong class=\\'[^\2\\']+\\'>([^<>\"]+ (AM|PM))<").getMatch(0);
        if (expire != null) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd yyyy hh:mm a", Locale.US), br);
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        final Regex trafficRegex = br.getRegex("id=\"total_traffic_used\">([^<>]+)</span> / ([^<>]+)<");
        final String trafficUsedStr = trafficRegex.getMatch(0);
        final String trafficMaxStr = trafficRegex.getMatch(1);
        if (trafficUsedStr != null && trafficMaxStr != null) {
            final long trafficUsed = SizeFormatter.getSize(trafficUsedStr);
            final long trafficMax = SizeFormatter.getSize(trafficMaxStr);
            final long trafficLeft = trafficMax - trafficUsed;
            ai.setTrafficLeft(trafficLeft);
            ai.setTrafficMax(trafficMax);
        } else {
            /* Fallback */
            ai.setUnlimitedTraffic();
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String supportedHostsTable = this.br.getRegex("<tbody>(.*?)</tbody>").getMatch(0);
        final String tableLines[] = supportedHostsTable.split("<tr>");
        for (final String lineHTML : tableLines) {
            final String domain = new Regex(lineHTML, "class=\"bold\\-text\">([^<>\"]+)</td>").getMatch(0);
            if (domain == null) {
                continue;
            }
            final boolean individualLimitReached;
            final Regex individualLimitRegex = new Regex(lineHTML, ">([^<>\"]*?)</span> / (?:<span>)?([^<>\"]*?)<");
            final String individualTrafficUsedStr = individualLimitRegex.getMatch(0);
            final String individualTrafficLimitStr = individualLimitRegex.getMatch(1);
            if (individualTrafficUsedStr == null || individualTrafficLimitStr == null) {
                /* Do not fail because of this missing data. */
                individualLimitReached = false;
            } else if (individualTrafficLimitStr.contains("infin")) {
                individualLimitReached = false;
            } else {
                final long individualTrafficUsed = SizeFormatter.getSize(individualTrafficUsedStr);
                final long individualTrafficLimit = SizeFormatter.getSize(individualTrafficLimitStr);
                if (individualTrafficUsed >= individualTrafficLimit) {
                    individualLimitReached = true;
                } else {
                    individualLimitReached = false;
                }
            }
            final boolean hosterIsActive = lineHTML.contains("class='green-text'>ON<");
            final boolean hosterIsPremiumOnly = lineHTML.contains("class='orange-text'>PREMIUM<");
            if (!hosterIsActive) {
                logger.info("Skipping host (inactive): " + domain);
            } else if (hosterIsPremiumOnly && account.getType() != AccountType.PREMIUM) {
                logger.info("Skipping host (premiumonly): " + domain);
            } else if (individualLimitReached) {
                logger.info("Skipping host (limitReached): " + domain);
            } else {
                supportedHosts.add(domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    /*
                     * Even though login is forced first check if our cookies are still valid --> If not, force login!
                     */
                    br.getPage("https://" + this.getHost());
                    if (br.containsHTML(html_loggedin)) {
                        return;
                    }
                    /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                    this.br = prepBR(new Browser());
                }
                br.getPage("https://" + this.getHost() + "/sign-in.html");
                String postData = "username=" + Encoding.urlEncode(currAcc.getUser()) + "&password=" + Encoding.urlEncode(currAcc.getPass());
                this.postAPISafe("https://" + this.getHost() + "/sign-in.html", postData);
                if (!br.containsHTML(html_loggedin)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
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

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /** Keep this for possible future API implementation */
    private void updatestatuscode() {
    }

    /** Keep this for possible future API implementation */
    private void handleAPIErrors(final Browser br) throws PluginException {
        // String statusMessage = null;
        // try {
        // switch (statuscode) {
        // case 0:
        // /* Everything ok */
        // break;
        // case 1:
        // statusMessage = "";
        // tempUnavailableHoster(5 * 60 * 1000l);
        // break;
        // default:
        // handleErrorRetries("unknown_error_state", 50, 2 * 60 * 1000l);
        // }
        // } catch (final PluginException e) {
        // logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
        // throw e;
        // }
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}