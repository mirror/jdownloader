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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidox.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class RapidoxPl extends PluginForHost {

    private static final String                            CLEAR_DOWNLOAD_HISTORY       = "CLEAR_DOWNLOAD_HISTORY";
    private static final String                            CLEAR_ALLOWED_IP_ADDRESSES   = "CLEAR_ALLOWED_IP_ADDRESSES";
    private static final String                            DOMAIN                       = "http://rapidox.pl/";
    private static final String                            NICE_HOST                    = "rapidox.pl";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                     = NICE_HOSTproperty + "NOCHUNKS";
    private static final String                            NORESUME                     = NICE_HOSTproperty + "NORESUME";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* How long do we want to wait until the file is on their servers so we can download it? */
    private int                                            maxreloads                   = 200;
    private int                                            wait_between_reload          = 3;

    private static Object                                  LOCK                         = new Object();
    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public RapidoxPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidox.pl/promocje");
    }

    @Override
    public String getAGBLink() {
        return "http://rapidox.pl/regulamin";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
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
        this.br = newBrowser();
        setConstants(account, link);
        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            this.postAPISafe("http://rapidox.pl/panel/pobierz-plik", "check_links=" + Encoding.urlEncode(link.getDownloadURL()));
            final String requestId = br.getRegex("rapidox.pl/panel/pobierz\\-plik/(\\d+)").getMatch(0);
            if (requestId == null) {
                handleErrorRetries("requestIdnull", 5);
            }
            String hash = null;
            String dlid = null;
            int counter = 0;
            int getreadymaxreloads = 5;
            do {
                this.sleep(wait_between_reload * 1000l, link);
                br.getPage("/panel/pobierz-plik/" + requestId);
                hash = br.getRegex("name=\"form_hash\" value=\"([a-z0-9]+)\"").getMatch(0);
                dlid = br.getRegex("name=\"download\\[\\]\" value=\"(\\d+)\"").getMatch(0);
            } while (hash == null && dlid == null && counter <= getreadymaxreloads);
            if (hash == null || dlid == null) {
                handleErrorRetries("hash_requestid_null", 5);
            }
            /* Modify name so we can actually find our final downloadlink. */
            String fname = link.getName();
            fname = fname.replace(" ", "_");
            fname = fname.replaceAll("(;|\\&)", "");
            /* File is on their servers --> Chose download method */
            this.postAPISafe("/panel/lista-plikow", "download%5B%5D=" + dlid + "&form_hash=" + hash + "&download_files=Pobierz%21&pobieranie=posrednie");
            /* Access list of downloadable files/links & find correct final downloadlink */
            do {
                this.sleep(wait_between_reload * 1000l, link);
                this.getAPISafe("/panel/lista-plikow");
                /* TODO: Maybe find a more reliable way to get the final downloadlink... */
                final String[] alldirectlinks = br.getRegex("\"(http://[a-z0-9]+\\.rapidox\\.pl/[A-Za-z0-9]+/[^<>\"]*?)\"").getColumn(0);
                if (alldirectlinks != null && alldirectlinks.length > 1) {
                    for (final String directlink : alldirectlinks) {
                        if (directlink.contains(fname)) {
                            dllink = directlink;
                            break;
                        }
                    }
                }
            } while (counter <= maxreloads && dllink == null);
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 5);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (link.getBooleanProperty(NICE_HOSTproperty + NOCHUNKS, false)) {
            maxChunks = 1;
        }
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(RapidoxPl.NORESUME, false)) {
            resume = false;
            link.setProperty(RapidoxPl.NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(RapidoxPl.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 5);
            }
            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(NICE_HOSTproperty + RapidoxPl.NOCHUNKS, false) == false) {
                        link.setProperty(NICE_HOSTproperty + RapidoxPl.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                e.printStackTrace();
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + RapidoxPl.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + RapidoxPl.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/panel/twoje-informacje");
        String traffic_max = br.getRegex("Dopuszczalny transfer:</b>[\t\n\r ]+([0-9 ]+ MB)").getMatch(0);
        String traffic_available = br.getRegex("Do wykorzystania:[\t\n\r ]+([0-9 ]+ MB)").getMatch(0);
        if (traffic_max == null || traffic_available == null) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        /* Fix e.g. 500 000 MB (500 GB) --> 500000MB */
        traffic_max = traffic_max.replace(" ", "");
        traffic_available = traffic_available.replace(" ", "");
        /* Free users = They have no package --> Accept them but set zero traffic left */
        if (traffic_max.equals("0MB")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account");
            ai.setUnlimitedTraffic();
            ai.setTrafficLeft(SizeFormatter.getSize(traffic_available));
            ai.setTrafficMax(SizeFormatter.getSize(traffic_max));
        }
        account.setValid(true);
        /* Only add hosts which are listed as 'on' (working) */
        this.getAPISafe("http://rapidox.pl/panel/status_hostingow");
        final String[] possible_domains = { "to", "de", "com", "net", "co.nz", "in", "co", "me", "biz", "ch", "pl" };
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String hosttable = br.getRegex("</tr></thead><tr>(.*?)</tr></table>").getMatch(0);
        final String[] hostDomainsInfo = hosttable.split("<td width=\"50px\"");
        for (final String domaininfo : hostDomainsInfo) {
            String crippledhost = new Regex(domaininfo, "title=\"([^<>\"]+)\"").getMatch(0);
            final String status = new Regex(domaininfo, "<td>(on|off)").getMatch(0);
            if (crippledhost == null || status == null) {
                continue;
            } else if (status.equals("off") || !status.equals("on")) {
                logger.info("This host is currently not active, NOT adding it to the supported host array: " + crippledhost);
                continue;
            }
            crippledhost = crippledhost.toLowerCase();
            /* First cover special cases */
            if (crippledhost.equals("share_online")) {
                supportedHosts.add("share-online.biz");
            } else if (crippledhost.equals("ul.to") || crippledhost.equals("uploaded")) {
                supportedHosts.add("uploaded.net");
            } else if (crippledhost.equals("vipfile")) {
                supportedHosts.add("vip-file.com");
            } else if (crippledhost.equals("_4shared")) {
                supportedHosts.add("4shared.com");
            } else {
                /* Finally, go insane... */
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
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = newBrowser();
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
                            this.br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                String postData = "login83=" + Encoding.urlEncode(currAcc.getUser()) + "&password83=" + Encoding.urlEncode(currAcc.getPass());
                this.getAPISafe("http://rapidox.pl/zaloguj_sie");

                /*
                 * TODO: Check if login with captcha works and prevent captchas from appearing when auto account refresh is done! Maybe add
                 * a method to try to prevent login captcha as they identify the user who made a lot of login attempts only via User-Agent,
                 * not via IP.
                 */
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    /* Handle stupid login captcha */
                    final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null) {
                        logger.warning("Expected login captcha is not there!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "rapidox.pl", DOMAIN, true);
                    final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                    postData += "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&redirect=";
                }

                this.postAPISafe("/panel/login", postData);
                if (br.containsHTML(">Wystąpił błąd\\! Nieprawidłowy login lub hasło\\.")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                this.getAPISafe("/panel/index");
                /* Double check */
                if (!br.containsHTML(">Wyloguj się<")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(DOMAIN);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
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

    @SuppressWarnings("unused")
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
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors.
     */
    private void updatestatuscode() {
        if (br.containsHTML("Wybierz linki z innego hostingu\\.")) {
            statuscode = 1;
        }
        statuscode = 0;
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Host currently not supported --> deactivate it for some hours. */
                statusMessage = "Host is currently not supported";
                tempUnavailableHoster(3 * 60 * 60 * 1000l);
                break;
            default:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
    private void handleErrorRetries(final String error, final int maxRetries) throws PluginException {
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
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* TODO: Remove plugin defect once all known errors are correctly handled */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}