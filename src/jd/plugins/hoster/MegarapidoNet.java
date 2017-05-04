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
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megarapido.net" }, urls = { "" })
public class MegarapidoNet extends PluginForHost {

    /* Tags: conexaomega.com.br, megarapido.net, superdown.com.br */

    private static final String                            DOMAIN                       = "https://megarapido.net/";
    private static final String                            NICE_HOST                    = "megarapido.net";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|-)", "");
    private static final String                            NOCHUNKS                     = NICE_HOSTproperty + "NOCHUNKS";
    private static final String                            NORESUME                     = NICE_HOSTproperty + "NORESUME";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String                            default_UA                   = "JDownloader";

    private static Object                                  LOCK                         = new Object();
    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public MegarapidoNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://megarapido.net/planos");
    }

    @Override
    public String getAGBLink() {
        return "https://megarapido.net/termos-e-condicoes";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", default_UA);
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

        login(account);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            this.getAPISafe("https://" + this.getHost() + "/_gerar.php?link=" + Encoding.urlEncode(link.getDownloadURL()) + "&rnd=1." + System.currentTimeMillis());
            dllink = this.br.getRegex("(http[^<>\"\\|\\']+)").getMatch(0);
            if (dllink == null || dllink.length() > 500) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 5);
            }
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (link.getBooleanProperty(NICE_HOSTproperty + NOCHUNKS, false)) {
            maxChunks = 1;
        }
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(MegarapidoNet.NORESUME, false)) {
            resume = false;
            link.setProperty(MegarapidoNet.NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(MegarapidoNet.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
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
                    if (link.getBooleanProperty(NICE_HOSTproperty + MegarapidoNet.NOCHUNKS, false) == false) {
                        link.setProperty(NICE_HOSTproperty + MegarapidoNet.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                e.printStackTrace();
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + MegarapidoNet.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + MegarapidoNet.NOCHUNKS, Boolean.valueOf(true));
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account);
        if (!br.getURL().endsWith("/gerador")) {
            br.getPage("/gerador");
        }
        final String segundos = br.getRegex("var\\s*segundos\\s*=\\s*(\\d+)").getMatch(0);
        if (segundos != null) {
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(segundos) * 1000l);
            ai.setUnlimitedTraffic();
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            final Regex premium_time = br.getRegex("(\\d{1,2}) DIAS, (\\d{1,2}) HORAS, (\\d{1,2}) MINUTOS E (\\d{1,2}) SEGUNDOS");
            final String[][] time_matches = premium_time.getMatches();
            if (time_matches.length == 0) {
                /* Prevent downloads via free account - they have no traffic! */
                ai.setTrafficLeft(0);
                ai.setStatus("Free Account");
                account.setType(AccountType.FREE);
            } else {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(time_matches[0][0]) * 24 * 60 * 60 * 1000 + Long.parseLong(time_matches[0][1]) * 60 * 60 * 1000l + Long.parseLong(time_matches[0][2]) * 60 * 1000l + Long.parseLong(time_matches[0][2]) * 1000l);
                ai.setUnlimitedTraffic();
                ai.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
            }
        }
        account.setValid(true);
        final String[] possible_domains = { "to", "de", "com", "net", "co.nz", "in", "co", "me", "biz", "ch", "pl", "us", "cc" };
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostDomainsInfo = br.getRegex("class=\"has-tip\" title=\"([^<>\"]*?)\"").getColumn(0);
        for (final String domain : hostDomainsInfo) {
            final String crippledhost = domain.toLowerCase();
            /* First cover special cases */
            if (crippledhost.equals("mediafire")) {
                /* There is also mediafire.bz and so on but .com is the right one in this case! */
                supportedHosts.add("mediafire.com");
            } else if (crippledhost.equals("uploaded")) {
                supportedHosts.add("uploaded.net");
            } else if (crippledhost.equals("minhateca.com.br")) {
                supportedHosts.add("minhateca.com.br");
            } else if (crippledhost.equals("nowvideo.sx")) {
                supportedHosts.add("nowvideo.sx");
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

    private void login(final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                this.br = newBrowser();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + this.getHost() + "/");
                    if (br.containsHTML("/sair\\.php")) {
                        /* Existing cookies are valid --> Save new timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                    if (br.getCookies(DOMAIN) != null) {
                        br.clearCookies(DOMAIN);
                    }
                }
                String postData = "login=" + Encoding.urlEncode(currAcc.getUser()) + "&senha=" + Encoding.urlEncode(currAcc.getPass());
                this.getAPISafe("https://" + this.getHost() + "/login");

                final DownloadLink dummyLink = new DownloadLink(this, "Account", "megarapido.net", DOMAIN, true);
                this.setDownloadLink(dummyLink);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);

                this.postAPISafe("/painel_user/ajax/ajax.php", postData);
                final String userLanguage = System.getProperty("user.language");
                if (br.containsHTML(">Login ou Senha inválidos") || br.getCookie(DOMAIN, "login") == null || br.getCookie(DOMAIN, "auth") == null) {
                    if ("de".equalsIgnoreCase(userLanguage)) {
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

    /**
     * 0 = everything ok, 1-99 = "error"-errors
     */
    private void updatestatuscode() {
        final String error = br.getRegex("class=alert-message error > ([^<>]*?)</div>").getMatch(0);
        if (error != null) {
            if (error.contains("Desculpe-nos, no momento o servidor")) {
                statuscode = 1;
            } else {
                statuscode = 666;
            }
        } else {
            statuscode = 0;
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
                /* Host currently not supported --> deactivate it for some hours. */
                statusMessage = "Host is currently not supported";
                tempUnavailableHoster(3 * 60 * 1000l);
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
            tempUnavailableHoster(2 * 60 * 1000l);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_MultihosterScript;
    }

}