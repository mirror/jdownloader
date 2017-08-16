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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

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
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "geradorturbo.com" }, urls = { "" })
public class GeradorturboCom extends antiDDoSForHost {

    private final String                 DOMAIN                       = "http://geradorturbo.com/";
    private final String                 NICE_HOST                    = "geradorturbo.com";
    private final String                 NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private final String                 HTML_LOGOUT                  = "/logout\"";

    /* Connection limits */
    private final boolean                ACCOUNT_PREMIUM_RESUME       = true;
    private final int                    ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int                    ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private final int                    ACCOUNT_FREE_MAXDOWNLOADS    = 1;

    private static Object                LOCK                         = new Object();
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("geradorturbo.com");
    private Account                      currAcc                      = null;
    private DownloadLink                 currDownloadLink             = null;

    public GeradorturboCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://geradorturbo.com/cadastro");
    }

    @Override
    public String getAGBLink() {
        return "http://geradorturbo.com/termos";
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            prepBr.setFollowRedirects(true);
            prepBr.setCookie(this.getHost(), "english", "+");
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
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
        br = new Browser();
        setConstants(account, link);
        mhm.runCheck(this.currAcc, this.currDownloadLink);
        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            getPage("http://" + this.getHost() + "/downloader");
            // javascript reference is important
            final String js = br.getRegex("\"(/\\d+/\\d+/)ajax-all\\.js").getMatch(0);
            if (js == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            postPage(js + "gerador-all.php?rand=0." + System.currentTimeMillis(), "captcha=none&urllist=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = br.getRegex("title='click here to download' href='(http[^<>\"']+)'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("href='(http[^<>\"']+)'").getMatch(0);
            }
            if (dllink == null) {
                if (br.containsHTML("Servidor indisponível no momento")) {
                    // Servidor indisponível no momento (Erro #3)
                    // Server currently unavailable
                    mhm.putError(this.currAcc, this.currDownloadLink, 10 * 60 * 1000l, " Server currently unavailable");
                }
                /* Should never happen */
                mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "dllinknull", 10, 2 * 60 * 1000l);
            }
        }
        handleDL(dllink);
    }

    private void handleDL(final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        this.currDownloadLink.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.currDownloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                mhm.handleErrorGeneric(this.currAcc, this.currDownloadLink, "unknowndlerror", 5, 2 * 60 * 1000l);
            }
            dl.startDownload();
        } catch (final Exception e) {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
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
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        getPage("/downloader");
        String enddays = br.getRegex(">(\\d+) days</b>").getMatch(0);
        if (enddays == null) {
            enddays = br.getRegex("(\\d+) days").getMatch(0);
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
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setValidUntil(expiredate);
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        final String[] possible_domains = { "to", "de", "com", "net", "co.nz", "in", "co", "me", "biz", "ch", "pl", "cc" };
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] crippledDomains = br.getRegex("a\\.([A-Za-z0-9]+)\\{[\t\n\r ]*?background:").getColumn(0);
        for (String crippledhost : crippledDomains) {
            crippledhost = crippledhost.toLowerCase();
            /* First cover special cases */
            if (crippledhost.equals("shareonline")) {
                supportedHosts.add("share-online.biz");
            } else if ("onefichier".equals(crippledhost)) {
                supportedHosts.add("1fichier.com");
            } else if ("twoshared".equals(crippledhost)) {
                supportedHosts.add("2shared.com");
            } else if ("shared".equals(crippledhost)) {
                supportedHosts.add("4shared.com");
            } else if ("nowdownloadch".equals(crippledhost)) {
                supportedHosts.add("nowdownload.eu");
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
                br = new Browser();
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    /* Even though login is forced first check if our cookies are still valid --> If not, force login! */
                    br.getPage("http://" + this.getHost() + "/en.php");
                    if (br.containsHTML(HTML_LOGOUT)) {
                        return;
                    }
                    /* Clear cookies/headers to prevent unknown errors as we'll perform a full login below now. */
                    br = new Browser();
                }
                String postData = "btnSubmited=Log+in&txtEmail=" + Encoding.urlEncode(currAcc.getUser()) + "&txtSenha=" + Encoding.urlEncode(currAcc.getPass());
                br.getPage("http://" + this.getHost() + "/en.php");
                br.getPage("/login.php");

                if (br.containsHTML("\"g-recaptcha\"")) {
                    if (this.getDownloadLink() == null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), DOMAIN, true);
                        this.setDownloadLink(dummyLink);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }

                postPage("http://" + this.getHost() + "/login.php", postData);
                if (!br.containsHTML(HTML_LOGOUT)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
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
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_MultihosterScript;
    }

}