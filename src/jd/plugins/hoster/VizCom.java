//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "viz.com" }, urls = { "http://vizdecrypted/\\d+_\\d+_\\d+" })
public class VizCom extends PluginForHost {
    public VizCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String default_extension    = ".jpg";
    /* Connection stuff */
    private final boolean       FREE_RESUME          = false;
    private final int           FREE_MAXCHUNKS       = 1;
    private final int           FREE_MAXDOWNLOADS    = 20;
    private final boolean       ACCOUNT_RESUME       = false;
    private final int           ACCOUNT_MAXCHUNKS    = 1;
    private final int           ACCOUNT_MAXDOWNLOADS = 20;
    private String              dllink               = null;
    private boolean             server_issues        = false;
    private boolean             premiumonly          = false;

    @Override
    public String getAGBLink() {
        return "https://www.viz.com/terms";
    }

    private Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 500 });
        br.setCookie(this.getHost(), "curtain_seen", "true");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        dllink = null;
        server_issues = false;
        premiumonly = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (account != null) {
            this.login(account, false);
        }
        final String[] ids = link.getPluginPatternMatcher().replace("http://vizdecrypted/", "").split("_");
        final String manga_id = ids[0];
        final String page = ids[1];
        final String page_for_url = ids[2];
        /* Access mainpage to get cookies - important! */
        this.br.getPage("https://www.viz.com/");
        jd.plugins.decrypter.VizCom.accessPage(this.br, manga_id, page_for_url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = manga_id + "_" + page;
        final String json_value_data = PluginJSonUtils.getJsonValue(this.br, "data");
        String filename = null;
        if (filename == null) {
            filename = url_filename;
        }
        /*
         * 2019-08-14: It seems like they are crypting the images. You can recognize parts of the content but they look more like a puzzle.
         */
        dllink = br.getRegex("\"(http[^<>\"]+" + page + "\\.jpg?[^<>\"]+)").getMatch(0);
        if (dllink == null && br.toString().startsWith("http")) {
            dllink = br.toString();
        }
        if (dllink == null && this.br.containsHTML("blankpage.jpg")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if ("no_auth".equalsIgnoreCase(json_value_data)) {
            premiumonly = true;
            logger.info("premiumonly = true has been set");
        } else if (dllink == null) { // and !premiumonly
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setName(filename);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (premiumonly) {
            logger.info("Account required OR GEO-blocked OR not downloadable with current account (free account but premium required?) OR item needs to be purchased separately");
            throw new AccountRequiredException();
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    /**
     * 2019-08-13: This host GEO-blocks a lot of content for outside US users. Login however should always work fine! There is a page which
     * shows additional account information/purchases which is not accessible when GEO-blocked:
     * https://www.viz.com/account/manage_membership
     */
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                boolean isLoggedin = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://www." + this.getHost() + "/account");
                    isLoggedin = this.isLoggedin();
                }
                if (!isLoggedin) {
                    br.getPage("https://www." + this.getHost() + "/account");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    // Grab the now required authenticity_token value from refresh_login_links and use it to properly log in.
                    br.getPage("/account/refresh_login_links");
                    final String authenticity_token = br.getRegex("var AUTH_TOKEN\\s*?=\\s*?\"([^\\\"]*)\";").getMatch(0);
                    br.postPage("/account/try_login", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&authenticity_token=" + Encoding.urlEncode(authenticity_token) + "&rem_user=1");
                    final String okay = PluginJSonUtils.getJsonValue(this.br, "ok");
                    if (this.br.getHttpConnection().getResponseCode() != 200 || !"1".equals(okay)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(br.getHost(), "", Cookies.NOTDELETEDPATTERN) != null || br.containsHTML("id=\"account_date_day\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            throw e;
        }
        /*
         * 2017-01-25: Treat all as free accounts - accounts do not have an exact status - users can buy single items which are then
         * unlocked to view/download in their account.
         */
        account.setMaxSimultanDownloads(ACCOUNT_MAXDOWNLOADS);
        String accountStatus = null;
        try {
            br.getPage("/account/manage_membership");
            String expireStr = br.getRegex(">Member until ([A-Za-z]+  \\d{1,2}, \\d{4})</a>").getMatch(0);
            if (br.containsHTML(">CONTENT NOT AVAILABLE<|>\\s*?Unfortunately this content is not available in your location")) {
                /* GEO-blocked - basically we cannot find out the real account type in this state */
                accountStatus = "Registered (free) user [GEO-blocked(only US IPs are allowed)]";
                account.setType(AccountType.FREE);
            } else {
                /* NOT GEO-blocked */
                if (expireStr == null) {
                    accountStatus = "Registered (free) user";
                    account.setType(AccountType.FREE);
                } else {
                    expireStr = expireStr.replace(" ", "");
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expireStr, "MMMdd','yyyy", Locale.ENGLISH), br);
                    accountStatus = "Premium user";
                    account.setType(AccountType.PREMIUM);
                }
                accountStatus += " [NOT GEO-blocked]";
            }
        } catch (final Throwable e) {
        }
        ai.setStatus(accountStatus);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        doFree(link, ACCOUNT_RESUME, ACCOUNT_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
