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

import jd.PluginWrapper;
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
import jd.plugins.components.MultiHosterManagement;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tout-debrid.eu" }, urls = { "" })
public class ToutDebridEu extends antiDDoSForHost {
    private static final String          PROTOCOL                  = "https://";
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("tout-debrid.eu");

    public ToutDebridEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://tout-debrid.eu/offres");
    }

    @Override
    public String getAGBLink() {
        return "http://tout-debrid.eu/faq";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        /* 2019-06-13: They've blocked this User-Agent for unknown reasons. Accessing their login-page will return 403 when it is used! */
        // br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
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
        mhm.runCheck(account, link);
        login(account, false);
        final String dllink = getDllink(link);
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(account, link, "dllinknull", 50, 2 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null) {
            dllink = getDllinkWebsite(link);
        }
        return dllink;
    }

    private String getDllinkAPI(final DownloadLink link) throws IOException, PluginException {
        return null;
    }

    private String getDllinkWebsite(final DownloadLink link) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        postPage(br, "/generateur-all.php?rand=0." + System.currentTimeMillis(), "captcha=none&urllist=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
        String dllink = br.getRegex("title=\\'click here to download\\' href=\\'(https?://[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("href=\\'(https?://[^/\"\\']+/relais/[^<>\"\\']+)").getMatch(0);
        }
        return dllink;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        link.setProperty(this.getHost() + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(this.getHost() + "directlink", Property.NULL);
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
        this.br = prepBR(this.br);
        final AccountInfo ai = fetchAccountInfoWebsite(account);
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /*
         * 2017-11-29: Lifetime premium not (yet) supported via website mode! But by the time we might need the website version again, they
         * might have stopped premium lifetime sales already as that has never been a good idea for any (M)OCH.
         */
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.containsHTML("/gerador")) {
            getPage("/gerador");
        }
        final boolean isPremium = br.containsHTML("Compte:\\s*?<b>Premium</b>");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        if (isPremium) {
            account.setType(AccountType.PREMIUM);
            final String daysLeft = br.getRegex("Restant\\s*?:\\s*?<b>(\\d+) jours</b>").getMatch(0);
            if (daysLeft != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(daysLeft) * 24 * 60 * 1000, br);
            }
            final String[] hostlist = br.getRegex("icones/([^<>\"\\']+)\\.[a-z0-9]+\"").getColumn(0);
            if (hostlist != null) {
                supportedHosts = new ArrayList<String>(Arrays.asList(hostlist));
            }
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        return null;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            br.setAllowedResponseCodes(507);
            this.br = prepBR(this.br);
            account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 15 * 60 * 1000l);
            loginWebsite(account, force);
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        try {
            Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                final String cookieAge = TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - account.getCookiesTimeStamp(""), 0);
                this.br.setCookies(this.getHost(), cookies);
                /*
                 * Even though login is forced first check if our cookies are still valid --> If not, force login!
                 */
                getPage(PROTOCOL + this.getHost() + "/debrideur");
                if (isLoggedinHTML()) {
                    logger.info("Login via cached cookies successful:" + account.getType() + " | CookieAge:" + cookieAge);
                    cookies = this.br.getCookies(this.getHost());
                    final Cookie owner = cookies.get("owner");
                    if (owner != null) {
                        owner.setExpires(null);
                    }
                    account.saveCookies(cookies, "");
                    return;
                } else {
                    logger.info("Login via cached cookies failed:" + account.getType() + " | CookieAge:" + cookieAge);
                }
                /* Clear cookies to prevent unknown errors as we'll perform a full login below now. */
                this.br = prepBR(new Browser());
            }
            getPage(PROTOCOL + this.getHost() + "/login");
            final Form login = br.getFormbyActionRegex(".*login\\.php.*");
            if (login == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            login.put("txtEmail", Encoding.urlEncode(account.getUser()));
            login.put("txtMdp", Encoding.urlEncode(account.getPass()));
            if (login.containsHTML("txtImgCode")) {
                final String image = login.getRegex("img\\s*src\\s*=\\s*\"(.*?)\"").getMatch(0);
                if (image == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), "http://" + getHost(), true);
                final String captcha = getCaptchaCode(image, dummyLink);
                if (StringUtils.isEmpty(captcha)) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                login.put("txtImgCode", Encoding.urlEncode(captcha));
            }
            submitForm(login);
            if (!isLoggedinHTML()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            cookies = this.br.getCookies(this.getHost());
            final Cookie owner = cookies.get("owner");
            if (owner != null) {
                owner.setExpires(null);
            }
            account.saveCookies(cookies, "");
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
    }

    private boolean isLoggedinHTML() {
        return br.containsHTML("/logout\"");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}