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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "superdown.com.br" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class SuperdownComBr extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            DOMAIN             = "https://superdown.com.br/";
    private static final String                            NICE_HOST          = "superdown.com.br";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);
    private static Object                                  LOCK               = new Object();
    private static final String[][]                        HOSTS              = { { "4shared", "4shared.com" }, { "asfile", "asfile.com" }, { "Bitshare", "bitshare.com" }, { "datafile", "datafile.com," }, { "ddlstorage", "ddlstorage.com" }, { "Depfile", "depfile.com" }, { "depositfiles", "depositfiles.com" }, { "dizzcloud", "dizzcloud.com" }, { "easybytez", "easybytez.com" }, { "extmatrix", "extmatrix.com" }, { "fayloobmennik", "fayloobmennik.net" }, { "filecloud", "filecloud.io" }, { "Filefactory", "filefactory.com" }, { "filemonkey", "filemonkey.in" }, { "fileom", "fileom.com" }, { "Filepost", "filepost.com" }, { "filesflash", "filesflash.com" }, { "filesmonster", "filesmonster.com" }, { "Firedrive", "firedrive.com" }, { "Freakshare", "freakshare.com" }, { "hugefiles", "hugefiles.net" }, { "hulkfile", "hulkfile.eu" }, { "Keep2share", "keep2share.cc" },
            { "kingfiles", "kingfiles.net" }, { "Letitbit", "letitbit.net" }, { "Luckyshare", "luckyshare.net" }, { "lumfile", "lumfile.com" }, { "Mediafire", "mediafire.com" }, { "megairon", "megairon.net" }, { "Megashares", "megashares.com" }, { "mightyupload", "mightyupload.com" }, { "Netload", "netload.in" }, { "novafile", "novafile.com" }, { "putlocker", "putlocker.com" }, { "Rapidgator", "rapidgator.net" }, { "Rapidshare", "rapidshare.com" }, { "Ryushare", "ryushare.com" }, { "Sendspace", "sendspace.com" }, { "Shareflare", "shareflare.net" }, { "Terafile", "terafile.co" }, { "Turbobit", "turbobit.net" }, { "ultramegabit", "ultramegabit.com" }, { "Uploadable", "uploadable.ch" }, { "Uploaded.to", "uploaded.net" }, { "uppit", "uppit.com" }, { "videomega", "videomega.tv" }, { "Zippyshare", "zippyshare.com" }, { "1Fichier", "1fichier.com" }, { "2shared", "2shared.com" },
            { "Crocko", "crocko.com" }, { "Gigasize", "gigasize.com" }, { "Jumbofiles", "jumbofiles.com" }, { "Mega", "mega.co.nz" }, { "Minhateca", "minhateca.com.br" }, { "Uploading", "uploading.com" }, { "Uptobox", "uptobox.com" }, { "Vip-file", "vip-file.com" } };

    public SuperdownComBr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.superdown.com.br/en/planos");
    }

    @Override
    public String getAGBLink() {
        return "http://www.superdown.com.br/en/termos";
    }

    private void prepBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.setCookie(DOMAIN, "locale", "en");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
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
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
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
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
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
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBrowser();
        showMessage(link, "Task 1: Generating Link");
        this.login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* request Download */
            br.getPage("http://www.superdown.com.br/_gerar?link=" + Encoding.urlEncode(link.getDownloadURL()) + "&rnd=0." + System.currentTimeMillis());
            dllink = br.getRegex("(http://[^<>\"]*?)\\|").getMatch(0);
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
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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
        account.setMaxSimultanDownloads(20);
        maxPrem.set(20);
        prepBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account, false);
        account.setValid(true);
        account.setConcurrentUsePossible(true);

        br.getPage("http://www.superdown.com.br/en/");

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
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        /* Apply supported hosts depending on account type */
        for (final String[] filehost : HOSTS) {
            final String crippledHost = filehost[0];
            final String realHost = filehost[1];
            if (br.containsHTML("<b>" + crippledHost + ":</b>[\t\n\r ]+(Available|Testing)")) {
                supportedHosts.add(realHost);
            }
        }
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }
        ai.setStatus("Premium account");
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                br.postPage("http://www.superdown.com.br/login", "lembrar=on&email=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass()));
                final String lang = System.getProperty("user.language");
                /* Check if we have a free account (free accounts cannot download anything anyways) */
                if (br.containsHTML("style=\"font-size:15px\">Buy a Plan</a>")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.getCookie(DOMAIN, "key") == null || !br.containsHTML("class=\"pull\\-left expira\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}