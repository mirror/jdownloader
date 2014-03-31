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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "simply-premium.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class SimplyPremiumCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "simply-premium.com";
    private static final String                            NICE_HOSTproperty  = "simplypremiumcom";
    private static String                                  APIKEY             = null;
    private static Object                                  LOCK               = new Object();

    /* = plugin defect errors possible */
    private static final boolean                           TEST_MODE          = true;

    /* Default value is 3 */
    private static AtomicInteger                           maxPrem            = new AtomicInteger(3);

    public SimplyPremiumCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.simply-premium.com/vip.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.simply-premium.com/terms_and_conditions.php";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        /* define custom browser headers and language settings. */
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
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

        final boolean resume_allowed = account.getBooleanProperty("resume_allowed", false);

        int maxChunks = 1;
        maxChunks = (int) account.getLongProperty("maxconnections", 1);
        if (maxChunks > 20) maxChunks = 0;
        if (link.getBooleanProperty(NOCHUNKS, false)) maxChunks = 1;
        if (!resume_allowed) maxChunks = 1;

        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume_allowed, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">Errormessage: You have too many simultaneous connections<")) {
                logger.info(NICE_HOST + ": Too many simultan connections");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
            }
            logger.info(NICE_HOST + ": Unknown download error");
            if (TEST_MODE) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(SimplyPremiumCom.NOCHUNKS, false) == false) {
                    link.setProperty(SimplyPremiumCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(SimplyPremiumCom.NOCHUNKS, false) == false) {
                link.setProperty(SimplyPremiumCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        getapikey(account);
        showMessage(link, "Task 1: Checking link");
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* request download information */
            br.setFollowRedirects(true);
            br.getPage("http://www.simply-premium.com/premium.php?info=&link=" + Encoding.urlEncode(link.getDownloadURL()));
            if (br.containsHTML("<error>NOTFOUND</error>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("<error>hostererror</error>")) {
                logger.info(NICE_HOST + ": Host is unavailable at the moment -> Disabling it");
                tempUnavailableHoster(account, link, 60 * 60 * 1000l);
            } else if (br.containsHTML("<error>maxconnection</error>")) {
                logger.info(NICE_HOST + ": Too many simultan connections");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
            } else if (br.containsHTML("<error>notvalid</error>")) {
                logger.info(NICE_HOST + ": Account invalid -> Disabling it");
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else if (br.containsHTML("<error>trafficlimit</error>")) {
                logger.info(NICE_HOST + ": Traffic limit reached -> Temp. disabling account");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }

            /* request download */
            dllink = getXML("download");
            if (dllink == null) {
                if (TEST_MODE) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                logger.info(NICE_HOST + ": dllinknull");
                int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "timesfailed_dllinknull", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknull", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "dllinknull");
                } else {
                    link.setProperty(NICE_HOSTproperty + "timesfailed_dllinknull", Property.NULL);
                    logger.info(NICE_HOST + ": dllinknull - disabling current host!");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
            }
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        getapikey(account);
        br.getPage("http://simply-premium.com/api/user.php?apikey=" + APIKEY);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        final String acctype = getXML("account_typ");
        if (acctype == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (!acctype.matches("1|2") || !br.containsHTML("<vip>1</vip>")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        String accdesc = null;
        if ("1".equals(acctype)) {
            ai.setUnlimitedTraffic();
            final String expire = getXML("timeend");
            final Long expirelng = Long.parseLong(expire);
            ai.setValidUntil(expirelng * 1000);
            accdesc = "Time account";
        } else {
            ai.setTrafficLeft(getXML("maxtraffic"));
            accdesc = "Volume account";
        }

        int maxSimultanDls = Integer.parseInt(getXML("max_downloads"));
        if (maxSimultanDls < 1) {
            maxSimultanDls = 1;
        } else if (maxSimultanDls > 20) {
            maxSimultanDls = 20;
        }
        account.setMaxSimultanDownloads(maxSimultanDls);

        long maxChunks = Integer.parseInt(getXML("chunks"));
        if (maxChunks > 1) maxChunks = -maxChunks;
        account.setProperty("maxconnections", maxChunks);

        final boolean resumeAllowed = "1".equals(getXML("resume"));
        account.setProperty("resume_allowed", resumeAllowed);

        maxPrem.set(maxSimultanDls);
        /* online=1 == show only working hosts */
        br.getPage("http://www.simply-premium.com/api/hosts.php?online=1");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomains = br.getRegex("<host>([^<>\"]*?)</host>").getColumn(0);
        for (final String domain : hostDomains) {
            supportedHosts.add(domain);
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
        ai.setStatus(accdesc + " - " + supportedHosts.size() + " hosts available");
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void getapikey(final Account acc) throws IOException, PluginException {
        synchronized (LOCK) {
            APIKEY = acc.getStringProperty(NICE_HOSTproperty + "apikey", null);
            if (APIKEY != null) {
                br.setCookie("http://simply-premium.com/", "apikey", APIKEY);
            } else {
                login(acc);
            }
        }
    }

    private void login(final Account account) throws IOException, PluginException {
        final String lang = System.getProperty("user.language");
        br.getPage("http://simply-premium.com/login.php?login_name=" + Encoding.urlEncode(account.getUser()) + "&login_pass=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("<error>captcha_required</error>")) {
            final DownloadLink dummyLink = new DownloadLink(this, "Account", "simply-premium.com", "http://simply-premium.com", true);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId("6LfBs-8SAAAAAKRWHd9j-sq-qpoOnjwoZlA4s3Ix");
            rc.load();
            for (int i = 1; i <= 3; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, dummyLink);
                br.getPage("http://simply-premium.com/login.php?login_name=" + Encoding.urlEncode(account.getUser()) + "&login_pass=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br.containsHTML("<error>captcha_required</error>")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("<error>captcha_required</error>")) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, ungültiges Passwort und/oder ungültiges Login-Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password and/or invalid login-captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        }
        /* TODO: Add correct handling for this case */
        if (br.containsHTML("<error>not_valid_ip</error>")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid / Ungültig", PluginException.VALUE_ID_PREMIUM_DISABLE);

        }
        APIKEY = br.getCookie("http://simply-premium.com/", "apikey");
        if (APIKEY == null || br.containsHTML("<error>not_valid</error>")) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setProperty(NICE_HOSTproperty + "apikey", APIKEY);
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

    private String getXML(final String parameter) {
        return br.getRegex("<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}