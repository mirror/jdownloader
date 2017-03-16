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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "geradorturbo.com" }, urls = { "" })
public class GeradorturboCom extends PluginForHost {

    private final String                                   DOMAIN                       = "http://geradorturbo.com/";
    private final String                                   NICE_HOST                    = "geradorturbo.com";
    private final String                                   NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private final String                                   HTML_LOGOUT                  = "/logout\"";

    /* Connection limits */
    private final boolean                                  ACCOUNT_PREMIUM_RESUME       = true;
    private final int                                      ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int                                      ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final int                                      ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private final String                                   default_UA                   = "JDownloader";

    private static Object                                  LOCK                         = new Object();
    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public GeradorturboCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://geradorturbo.com/cadastro");
    }

    @Override
    public String getAGBLink() {
        return "http://geradorturbo.com/termos";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", default_UA);
        br.setCookie(this.getHost(), "english", "+");
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

        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            this.postAPISafe("http://" + this.getHost() + "/07/8037/gerador-all.php?rand=0." + System.currentTimeMillis(), "captcha=none&urllist=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = br.getRegex("title=\\'click here to download\\' href=\\'(http[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("href=\\'(http[^<>\"\\']+)\\'").getMatch(0);
            }
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 50, 2 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        final boolean resume = ACCOUNT_PREMIUM_RESUME;
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, ACCOUNT_PREMIUM_MAXCHUNKS);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/downloader");
        String enddays = this.br.getRegex(">(\\d+) days</b>").getMatch(0);
        if (enddays == null) {
            enddays = this.br.getRegex("(\\d+) days").getMatch(0);
        }
        long expiredate = 0;
        if (enddays != null) {
            expiredate = System.currentTimeMillis() + (Long.parseLong(enddays) * 24 * 60 * 60 * 1000);
        }
        /*
         * Free users = Can't download anything.
         */
        if (expiredate < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            ai.setStatus("Registered (free) account");
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account");
            ai.setValidUntil(expiredate);
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        final String[] possible_domains = { "to", "de", "com", "net", "co.nz", "in", "co", "me", "biz", "ch", "pl", "cc" };
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] crippledDomains = this.br.getRegex("a\\.([A-Za-z0-9]+)\\{[\t\n\r ]*?background:").getColumn(0);
        for (String crippledhost : crippledDomains) {
            crippledhost = crippledhost.toLowerCase();
            /* First cover special cases */
            if (crippledhost.equals("shareonline")) {
                supportedHosts.add("share-online.biz");
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                this.br = newBrowser();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    /* Even though login is forced first check if our cookies are still valid --> If not, force login! */
                    br.getPage("http://" + this.getHost() + "/en.php");
                    if (br.containsHTML(HTML_LOGOUT)) {
                        return;
                    }
                    /* Clear cookies/headers to prevent unknown errors as we'll perform a full login below now. */
                    this.br = newBrowser();
                }
                String postData = "btnSubmited=Log+in&txtEmail=" + Encoding.urlEncode(currAcc.getUser()) + "&txtSenha=" + Encoding.urlEncode(currAcc.getPass());
                br.getPage("http://" + this.getHost() + "/en.php");
                br.getPage("/login.php");

                if (this.br.containsHTML("\"g\\-recaptcha\"")) {
                    if (this.getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), DOMAIN, true);
                        this.setDownloadLink(dummyLink);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }

                this.postAPISafe("http://" + this.getHost() + "/login.php", postData);
                if (!br.containsHTML(HTML_LOGOUT)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    /** Not used at the moment but keep this for future implementations! */
    private void updatestatuscode() {
    }

    /** Not used at the moment but keep this for future implementations! */
    private void handleAPIErrors(final Browser br) throws PluginException {
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_MultihosterScript;
    }

}