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
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "transload.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class TransloadMe extends PluginForHost {

    private static final String                            DOMAIN                       = "http://transload.me/";
    private static final String                            NICE_HOST                    = "transload.me";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME                     = NICE_HOSTproperty + "NORESUME";
    private final String                                   HTML_LOGGEDIN                = "user\\.php\\?action=logout\"";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* How long do we want to wait until the file is on their servers so we can download it? */
    private int                                            maxreloads                   = 200;
    private int                                            wait_between_reload          = 3;
    private static final String                            default_UA                   = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:46.0) Gecko/20100101 Firefox/46.0";

    private static AtomicReference<String>                 agent                        = new AtomicReference<String>(null);
    private static Object                                  LOCK                         = new Object();
    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public TransloadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.transload.me/?p=register");
    }

    @Override
    public String getAGBLink() {
        return "http://en.transload.me/?p=login&redir=helpdesk";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", default_UA);
        br.getHeaders().put("Accept-Language", "de,en;q=0.7,en-US;q=0.3");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
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
    public boolean canHandle(DownloadLink downloadLink, Account account) {
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
        this.br = newBrowser();
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
            this.getAPISafe("http://transload.me/download2.php?prev=false&my_url=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = this.br.getRegex("(https?://transload\\.me/download/\\?\\d+)").getMatch(0);
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 5, 2 * 60 * 1000l);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
            link.setProperty(NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 5, 2 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Throwable e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/?p=download");
        /* Language independant RegEx */
        long traffic_available = 0;
        final String traffic_available_str = br.getRegex("(\\d+(?:\\.\\d{1,4})? [A-Za-z]+) <a href=\"/\\?p=price\"").getMatch(0);
        if (traffic_available_str != null) {
            traffic_available = SizeFormatter.getSize(traffic_available_str);
        }
        /*
         * Free users = They have no package --> Accept them but set zero traffic left. Of couse traffic left <= 0 --> Also free account.
         */
        if (traffic_available == 0) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account");
            ai.setTrafficLeft(traffic_available);
        }
        account.setValid(true);
        final String[] domains = this.br.getRegex("/host/small/[^/]+\\.png\" title=\"([^<>\"]*?)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(domains));
        ai.setMultiHostSupport(this, supportedHosts);

        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = newBrowser();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (force) {
                        /* Even though login is forced first check if our cookies are still valid --> If not, force login! */
                        br.getPage("http://en." + this.getHost() + "/");
                        if (br.containsHTML(HTML_LOGGEDIN)) {
                            return;
                        }
                        /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                        if (br.getCookies(DOMAIN) != null) {
                            br.clearCookies(DOMAIN);
                        }
                    } else {
                        /* Trust cookies! */
                        return;
                    }
                }
                br.setFollowRedirects(true);

                this.getAPISafe("http://transload.me/?p=login");
                String postData = "action=login&r=0&redir=&login=" + Encoding.urlEncode(currAcc.getUser()) + "&password=" + Encoding.urlEncode(currAcc.getPass());
                if (this.br.containsHTML("g\\-recaptcha")) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), DOMAIN, true);
                    this.setDownloadLink(dummyLink);
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }
                this.postAPISafe("/user.php", postData);
                if (!this.br.containsHTML(HTML_LOGGEDIN)) {
                    if (System.getProperty("user.language").equals("de")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            /* wait 30 mins to retry this host */
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

    /* Please do not remove this function - future usage!! */
    private void updatestatuscode() {
        // if (br.containsHTML("Wybierz linki z innego hostingu\\.")) {
        // statuscode = 1;
        // } else if (br.containsHTML("Jeśli widzisz ten komunikat prosimy niezwłocznie skontaktować się z nami pod")) {
        // statuscode = 2;
        // } else {
        // statuscode = 0;
        // }
    }

    /* Please do not remove this function - future usage!! */
    private void handleAPIErrors(final Browser br) throws PluginException {
        // String statusMessage = null;
        // try {
        // switch (statuscode) {
        // case 0:
        // /* Everything ok */
        // break;
        // case 1:
        // /* Host currently not supported --> deactivate it for some hours. */
        // statusMessage = "Host is currently not supported";
        // tempUnavailableHoster(5 * 60 * 1000l);
        // break;
        // case 2:
        // /* Host currently not supported --> deactivate it for some hours. */
        // statusMessage = "Your IP is banned";
        // final String userLanguage = System.getProperty("user.language");
        // if ("de".equalsIgnoreCase(userLanguage)) {
        // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDeine IP wurde gebannt!",
        // PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        // } else {
        // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\ntest!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        // }
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