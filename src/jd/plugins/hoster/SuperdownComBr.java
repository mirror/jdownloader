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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "superdown.com.br" }, urls = { "https?://[\\w]+\\.superdown\\.com\\.br/(?:superdown/)?\\w+/[a-zA-Z0-9]+/\\d+/\\S+" })
public class SuperdownComBr extends antiDDoSForHost {

    /* Tags: conexaomega.com.br, megarapido.net, superdown.com.br */

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            DOMAIN             = "https://superdown.com.br/";
    private static final String                            NICE_HOST          = "superdown.com.br";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private final String                                   html_loggedin      = "href=\"[^<>\"]*?logout[^<>\"]*?\"";
    private static Object                                  LOCK               = new Object();
    private static final String[][]                        HOSTS              = { { "mega", "mega.co.nz" }, { "oboom", "oboom.com" }, { "4shared", "4shared.com" }, { "datafile", "datafile.com" }, { "ddlstorage", "ddlstorage.com" }, { "Depfile", "depfile.com" }, { "depositfiles", "depositfiles.com" }, { "easybytez", "easybytez.com" }, { "extmatrix", "extmatrix.com" }, { "fayloobmennik", "fayloobmennik.net" }, { "filecloud", "filecloud.io" }, { "Filefactory", "filefactory.com" }, { "filesflash", "filesflash.com" }, { "filesmonster", "filesmonster.com" }, { "Freakshare", "freakshare.com" }, { "hugefiles", "hugefiles.net" }, { "Keep2share", "keep2share.cc" }, { "lumfile", "lumfile.com" }, { "Mediafire", "mediafire.com" }, { "novafile", "novafile.com" }, { "Rapidgator", "rapidgator.net" }, { "Sendspace", "sendspace.com" }, { "Turbobit", "turbobit.net" },
            { "ultramegabit", "ultramegabit.com" }, { "uploadable", "uploadable.ch" }, { "uploaded.to", "uploaded.net" }, { "uppit", "uppit.com" }, { "Zippyshare", "zippyshare.com" }, { "1Fichier", "1fichier.com" }, { "2shared", "2shared.com" }, { "Gigasize", "gigasize.com" }, { "Mega", "mega.co.nz" }, { "Minhateca", "minhateca.com.br" }, { "Uptobox", "uptobox.com" } };

    public SuperdownComBr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.superdown.com.br/en/planos");
    }

    @Override
    public String getAGBLink() {
        return "http://www.superdown.com.br/en/termos";
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCustomCharset("utf-8");
            prepBr.setCookie(DOMAIN, "locale", "en");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { link });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            if (!StringUtils.equals((String) downloadLink.getProperty("usedPlugin", getHost()), getHost())) {
                return false;
            }
        }
        // direct link... shouldn't need account
        if (downloadLink.getDownloadURL().matches(this.getSupportedLinks().pattern())) {
            return true;
        }
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        handleDL(null, downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        handleDL(account, link, link.getDownloadURL());
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            br.setFollowRedirects(true);
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = null;
                try {
                    con = openAntiDDoSRequestConnection(br, br.createGetRequest(dl.getDownloadURL()));
                    if (con.isContentDisposition()) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setDownloadSize(con.getLongContentLength());
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                } finally {
                    try {
                        /* make sure we close connection */
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks, true);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(NICE_HOST + ": Unknown download error");
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty(NICE_HOSTproperty + "timesfailed_unknowndlerror", Property.NULL);
                logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            }
        }
        try {
            link.setProperty("usedPlugin", getHost());
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                logger.info("Download failed -> Maybe re-trying with only 1 chunk");
                /* unknown error, we disable multiple chunks */
                disableChunkload(link);
                logger.info("Download failed -> Retry with 1 chunk did not solve the problem");
            }
        } catch (final PluginException e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            /* This may happen if the downloads stops at 99,99% - a few retries usually help in this case */
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info(NICE_HOST + ": DOWNLOAD_INCOMPLETE");

                logger.info("DOWNLOAD_INCOMPLETE -> Maybe re-trying with only 1 chunk");
                /* unknown error, we disable multiple chunks */
                disableChunkload(link);
                logger.info("DOWNLOAD_INCOMPLETE -> Retry with 1 chunk did not solve the problem");

                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_dl_incomplete", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 5) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dl_incomplete", timesFailed);
                    logger.info(NICE_HOST + ": UDOWNLOAD_INCOMPLETE - Retrying!");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "timesfailed_dl_incomplete");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dl_incomplete", Property.NULL);
                    logger.info(NICE_HOST + ": UDOWNLOAD_INCOMPLETE - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(SuperdownComBr.NOCHUNKS, false) == false) {
                link.setProperty(SuperdownComBr.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
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
        showMessage(link, "Task 1: Generating Link");
        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* request Download */
            final String passCode = link.getStringProperty("pass", null);
            final String passwordParam;
            if (StringUtils.isNotEmpty(passCode)) {
                passwordParam = "&password=" + Encoding.urlEncode(passCode);
            } else {
                passwordParam = "";
            }
            getPage("http://www.superdown.com.br/_gerar?link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "&rnd=0." + System.currentTimeMillis() + passwordParam);
            dllink = br.getRegex("(https?://[^<>\"]*?)\\|").getMatch(0);
            if (br.containsHTML("Sua sess[^ ]+ expirou por inatividade\\. Efetue o login novamente\\.")) {
                account.setProperty("cookies", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Password Request")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password Request");
            }
            if (dllink == null && br.containsHTML("não é um servidor suportado pelo")) {
                // host has been picked up due to generic supported host adding (matches)
                final ArrayList supportedHosts = (ArrayList) Arrays.asList(account.getProperty("multiHostSupport", new String[] {}));
                supportedHosts.remove(link.getHost());
                account.getAccountInfo().setMultiHostSupport(this, supportedHosts);
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Not supported at this provider", 6 * 60 * 60 * 1000l);
            }
            if (dllink == null || (dllink != null && dllink.length() > 500)) {
                logger.info(NICE_HOST + ": Unknown error");
                int timesFailed = link.getIntegerProperty("NICE_HOSTproperty + timesfailed_dllinknull", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknull", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknull", Property.NULL);
                    logger.info(NICE_HOST + ": Unknown error - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (!con.isOK() || con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void disableChunkload(final DownloadLink dl) throws PluginException {
        /* unknown error, we disable multiple chunks */
        if (dl.getBooleanProperty(SuperdownComBr.NOCHUNKS, false) == false) {
            dl.setProperty(SuperdownComBr.NOCHUNKS, Boolean.valueOf(true));
            dl.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);

        getPage("/en/");

        final String expire_text = br.getRegex("class=\"clearfix pull\\-right contador\">(.*?)<li class=\"dias\"").getMatch(0);
        if (expire_text == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String[] days_digits = new Regex(expire_text, "<li>(\\d+)</li>").getColumn(0);
        String days_string = "";
        for (final String digit : days_digits) {
            days_string += digit;
        }
        ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(days_string) * 24 * 60 * 60 * 1000l);
        ai.setUnlimitedTraffic();
        account.setValid(true);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        /* Apply supported hosts depending on account type */
        for (final String[] filehost : HOSTS) {
            final String crippledHost = filehost[0];
            final String realHost = filehost[1];
            if (br.containsHTML("<b>.*?" + crippledHost + ".*?:</b>[\t\n\r ]+(Available|Testing)")) {
                supportedHosts.add(realHost);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        ai.setStatus("Premium Account");
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
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
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = prepBrowser(this.br, this.getHost());
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Re-use cookies whenever possible to avoid login captcha prompts. */
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://www." + this.getHost());
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Clear cookies/headers to prevent unknown errors as we'll perform a full login below now. */
                    this.br = prepBrowser(this.br, this.getHost());
                }
                this.br.getPage("https://www." + this.getHost() + "/login");
                String postData = "lembrar=on&email=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass());

                if (this.br.containsHTML("g\\-recaptcha")) {
                    /* Handle login captcha */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String siteKey = this.br.getRegex("var key\\s*?=\\s*?\"([^<>\"]+)\";").getMatch(0);
                    final String recaptchaV2Response;
                    if (siteKey != null) {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, siteKey).getToken();
                    } else {
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    }

                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }

                postPage("/login", postData);
                if (!this.br.containsHTML(html_loggedin)) {
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.setProperty(NOCHUNKS, Property.NULL);
        }
    }
}