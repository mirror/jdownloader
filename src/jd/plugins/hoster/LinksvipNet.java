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
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksvip.net" }, urls = { "" })
public class LinksvipNet extends PluginForHost {
    private static final String                            NICE_HOST                 = "linksvip.net";
    private static final String                            NICE_HOSTproperty         = NICE_HOST.replaceAll("(\\.|\\-)", "");
    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME    = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final String                                   default_UA                = "JDownloader";
    private static final boolean                           USE_API                   = false;
    private final String                                   website_html_loggedin     = "/login/logout\\.php";
    private static Object                                  LOCK                      = new Object();
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap        = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currentAcc                = null;
    private DownloadLink                                   currentLink               = null;
    private static MultiHosterManagement                   mhm                       = new MultiHosterManagement("linksvip.net");

    public LinksvipNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://linksvip.net/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "https://linksvip.net/";
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
        this.currentAcc = acc;
        this.currentLink = dl;
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

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
        setConstants(account, link);
        mhm.runCheck(currentAcc, currentLink);
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
        String dllink = getDllink(link);
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(currentAcc, currentLink, "dllinknull", 2, 5 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final DownloadLink link) throws IOException, PluginException {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            if (USE_API) {
                dllink = getDllinkAPI(link);
            } else {
                dllink = getDllinkWebsite(link);
            }
        }
        return dllink;
    }

    private String getDllinkAPI(final DownloadLink link) throws IOException, PluginException {
        return null;
    }

    private String getDllinkWebsite(final DownloadLink link) throws IOException, PluginException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        this.postAPISafe("https://" + this.getHost() + "/GetLinkFs", "pass=undefined&hash=undefined&captcha=&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
        final String dllink = PluginJSonUtils.getJsonValue(this.br, "linkvip");
        return dllink;
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
                mhm.handleErrorGeneric(currentAcc, currentLink, "unknowndlerror", 2, 5 * 60 * 1000l);
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
        final AccountInfo ai;
        if (USE_API) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /*
         * 2017-11-29: Lifetime premium not (yet) supported via website mode! But by the time we might need the website version again, they
         * might have stopped premium lifetime sales already as that has never been a good idea for any (M)OCH.
         */
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("https://" + this.getHost() + "/");
        final boolean isPremium = br.containsHTML("class=\"badge\"[^>]+>Premium</span>");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (isPremium) {
            account.setType(AccountType.PREMIUM);
            final String expire = br.getRegex("Hạn dùng <span [^>]*?>(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2} (?:AM|PM))</span>").getMatch(0);
            if (expire != null) {
                /* Only set expiredate if we find it */
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy hh:mm a", Locale.US), br);
            }
            br.getPage("/host-support.html");
            final String[] hostlist = br.getRegex("domain=([^<>\"]+)\"").getColumn(0);
            if (hostlist != null) {
                supportedHosts = new ArrayList<String>(Arrays.asList(hostlist));
            }
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        br.getPage("/my-account.html");
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        return null;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            this.br = prepBR(this.br);
            if (USE_API) {
                loginAPI(account, force);
            } else {
                loginWebsite(account, force);
            }
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        try {
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                this.br.setCookies(this.getHost(), cookies);
                /*
                 * Even though login is forced first check if our cookies are still valid --> If not, force login!
                 */
                br.getPage("https://" + this.getHost() + "/");
                if (br.containsHTML(website_html_loggedin)) {
                    return;
                }
                /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                this.br = prepBR(new Browser());
            }
            br.getPage("https://" + this.getHost() + "/");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.postAPISafe("/login/", "auto_login=true&u=" + Encoding.urlEncode(currentAcc.getUser()) + "&p=" + Encoding.urlEncode(currentAcc.getPass()));
            final String status = PluginJSonUtils.getJson(br, "status");
            if ("1".equals(status)) {
                /* Login should be okay and we should get the cookies now! */
                br.getPage("/login/logined.php");
            }
            if (!br.containsHTML(website_html_loggedin)) {
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

    private void loginAPI(final Account account, final boolean force) throws Exception {
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
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}