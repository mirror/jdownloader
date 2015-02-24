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
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "grab8.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class Grab8Com extends PluginForHost {

    private static final String                            NICE_HOST                      = "grab8.com";
    private static final String                            NICE_HOSTproperty              = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                       = NICE_HOSTproperty + "NOCHUNKS";
    private static final String                            NORESUME                       = NICE_HOSTproperty + "NORESUME";
    private static final String                            CLEAR_DOWNLOAD_HISTORY         = "CLEAR_DOWNLOAD_HISTORY";
    private final boolean                                  default_clear_download_history = false;
    private static final String                            default_premium_page           = "http://p3.grab8.com/2/index.php";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME         = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS      = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS   = 20;

    private int                                            statuscode                     = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap             = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                        = null;
    private DownloadLink                                   currDownloadLink               = null;
    private static Object                                  LOCK                           = new Object();

    public Grab8Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://grab8.com/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://grab8.com/";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        // br.getHeaders().put("User-Agent", "JDownloader");
        br.setConnectTimeout(180 * 1000);
        br.setReadTimeout(180 * 1000);
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        String transferID = link.getStringProperty("transferID", null);
        this.br = newBrowser();
        setConstants(account, link);
        this.login(false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            br.setFollowRedirects(true);
            br.getPage("http://grab8.com/member/index.php");
            final String premiumPage = br.getRegex("<b>Your Premium Page is at: </b><a href=(?:\\'|\")(http://[a-z0-9\\-\\.]+\\.grab8\\.com[^<>\"]*?)(?:\\'|\")").getMatch(0);
            if (premiumPage == null) {
                logger.warning("Transloadpage is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Save this for later usage! */
            link.setProperty("premiumPage", premiumPage);
            br.getPage(premiumPage);
            dllink = br.getBaseURL();
            br.setFollowRedirects(true);
            this.postAPISafe(br.getBaseURL() + "index.php", "referer=&yt_fmt=highest&tor_user=&tor_pass=&mu_cookie=&cookie=&email=&method=tc&partSize=10&proxy=&proxyuser=&proxypass=&premium_acc=on&premium_user=&premium_pass=&path=%2Fhome%2Fgrab8%2Fpublic_html%2F2%2Ffiles&link=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML("Transloading in progress<")) {
                /* We have to send this form to finally start the transfer */
                final Form continueForm = br.getForm(0);
                if (continueForm == null) {
                    handleErrorRetries("continueformnull", 10, 2 * 60 * 1000l);
                }
                br.submitForm(continueForm);
            }
            /*
             * If e.g. the user already transfered the file to the server but this code tries to do it again for whatever reason we will not
             * see the transferID in the html although it exists and hasn't changed. In this case we should still have it saved from the
             * first download attempt.
             */
            final String newtransferID = br.getRegex("name=\"files\\[\\]\" value=\"(\\d+)\"").getMatch(0);
            if (newtransferID != null) {
                transferID = newtransferID;
                logger.info("Successfully found transferID");
                link.setProperty("transferID", transferID);
            } else {
                logger.warning("Failed to find transferID");
            }
            /* Normal case: Requested file is downloaded to the multihost and downloadable via the multihost. */
            dllink = br.getRegex("File <b><a href=\"/\\d+/(files/[^<>\"]*?)\"").getMatch(0);
            /*
             * If we try an already existing link many times we'll get a site asking us if the download is broken thus it looks different so
             * we need another RegEx.
             */
            if (dllink == null) {
                dllink = br.getRegex("<b>Download: <a href=\"/\\d+/(files/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 10, 2 * 60 * 1000l);
            }
            /* Happens sometimes - in the tests it frequently happened with share-online.biz links */
            if (dllink.equals("files/ip")) {
                handleErrorRetries("dllink_invalid_ip", 10, 2 * 60 * 1000l);
            }
            dllink = br.getBaseURL() + dllink;
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        final String transferID = link.getStringProperty("transferID", null);
        final boolean deleteAfterDownload = this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY, false);
        final String premiumPage = link.getStringProperty("premiumPage", default_premium_page);
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        /* First set hardcoded limit */
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        /* Then check if chunks failed before. */
        if (link.getBooleanProperty(NICE_HOSTproperty + NOCHUNKS, false)) {
            maxChunks = 1;
        }
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(Grab8Com.NORESUME, false)) {
            resume = false;
            link.setProperty(Grab8Com.NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(Grab8Com.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 50, 2 * 60 * 1000l);
            }
            try {
                if (this.dl.startDownload()) {
                    if (transferID != null && deleteAfterDownload) {
                        boolean success = false;
                        try {
                            /* We can skip this first step and directly confirm that we want to delete that file. */
                            // br.postPage(premiumPage, "act=delete&files%5B%5D=" + transferID);
                            br.postPage(premiumPage, "act=delete_go&files%5B%5D=" + transferID + "&yes=Yes");
                            success = true;
                        } catch (final Throwable e) {
                        }
                        if (success) {
                            logger.info("Successfully deleted file from server");
                        } else {
                            logger.warning("Failed to delete file from server");
                        }
                    }
                } else {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(NICE_HOSTproperty + Grab8Com.NOCHUNKS, false) == false) {
                        link.setProperty(NICE_HOSTproperty + Grab8Com.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                e.printStackTrace();
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + Grab8Com.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + Grab8Com.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
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

    @SuppressWarnings({ "deprecation" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(true);
        br.getPage("/member/index.php");
        final String expire = br.getRegex(">Your account will expire on <[^>]+>([^<>\"]*?)</span>").getMatch(0);
        if (expire != null) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", Locale.ENGLISH));
            final String traffic_used = br.getRegex("Traffic use: <font color=#[A-F0-9]+>(\\d+)(\\.\\d{1,2})? MB</font>").getMatch(0);
            final String traffic_max = br.getRegex("Max Traffic: <font color=#[A-F0-9]+>(\\d+) MB</font>").getMatch(0);
            if (traffic_max == null || traffic_used == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final long traffic_max_long = Long.parseLong(traffic_max) * 1024 * 1024;
            ai.setTrafficLeft(traffic_max_long - (Long.parseLong(traffic_used) * 1024 * 1024));
            ai.setTrafficMax(traffic_max_long);
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Free users cannot download anything! */
            ai.setTrafficLeft(0);
        }
        account.setValid(true);
        final String[] possible_domains = { "to", "de", "com", "net", "co.nz", "in", "co", "me", "biz", "ch", "pl", "us" };
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomainsInfo = br.getRegex("<li><span>(.*?)</span></li>").getColumn(0);
        for (String crippledhost : hostDomainsInfo) {
            crippledhost = crippledhost.trim();
            crippledhost = crippledhost.toLowerCase();
            crippledhost = crippledhost.replaceAll("(<strike>|</strike>)", "");
            if (crippledhost.contains(".")) {
                /* Domain is already fine */
                supportedHosts.add(crippledhost);
            } else {
                /* Go insane */
                for (final String possibledomain : possible_domains) {
                    final String full_possible_host = crippledhost + "." + possibledomain;
                    supportedHosts.add(full_possible_host);
                }
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);

        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(final boolean force) throws IOException, PluginException {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = currAcc.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(currAcc.getUser()).equals(currAcc.getStringProperty("name", Encoding.urlEncode(currAcc.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(currAcc.getPass()).equals(currAcc.getStringProperty("pass", Encoding.urlEncode(currAcc.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (currAcc.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(NICE_HOST, key, value);
                        }
                        return;
                    }
                }
                this.getAPISafe("http://grab8.com/member/");
                postAPISafe("/member/login.php", "action=login&user=" + Encoding.urlEncode(currAcc.getUser()) + "&pass=" + Encoding.urlEncode(currAcc.getPass()));
                final String pass_cookie = br.getCookie(NICE_HOST, "pass");
                if (pass_cookie == null || pass_cookie.equals("NULL")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername,/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(NICE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                currAcc.setProperty("name", Encoding.urlEncode(currAcc.getUser()));
                currAcc.setProperty("pass", Encoding.urlEncode(currAcc.getPass()));
                currAcc.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                currAcc.setProperty("cookies", Property.NULL);
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

    /**
     * 0 = everything ok, 1-99 = "htmlerror"-errors
     */
    private void updatestatuscode() {
        final String error = br.getRegex("class=\"htmlerror\"><b>(.*?)</b></span>").getMatch(0);
        if (error != null) {
            if (error.equals("No premium account working")) {
                statuscode = 1;
            } else if (error.contains("username or password is incorrect")) {
                statuscode = 2;
            } else if (error.contains("Files not found")) {
                statuscode = 3;
            } else if (error.contains("the daily download limit of")) {
                statuscode = 4;
            } else {
                statuscode = 666;
            }
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                statusMessage = "'No premium account working' --> Host is temporarily disabled";
                tempUnavailableHoster(1 * 60 * 60 * 1000l);
            case 2:
                /* Invalid logindata */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername,/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 3:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 4:
                statusMessage = "Exceeded daily limit of host";
                tempUnavailableHoster(1 * 60 * 60 * 1000l);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries("unknownAPIerror", 10, 2 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
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
            // tempUnavailableHoster(disableTime);
            /** TODO: Remove this once the plugin is in a stable state */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY, JDL.L("plugins.hoster.grab8com.clear_serverside_download_history", "Delete downloaded file in grab8 account after successful download?")).setDefaultValue(default_clear_download_history));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}