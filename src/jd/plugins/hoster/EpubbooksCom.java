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

import java.io.IOException;
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "epubbooks.com" }, urls = { "https?://(?:www\\.)?epubbooks\\.com/downloads/(\\d+)" })
public class EpubbooksCom extends PluginForHost {
    public EpubbooksCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.epubbooks.com/sign_up");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.epubbooks.com/terms";
    }

    /* Connection stuff */
    private final int    FREE_MAXDOWNLOADS            = 20;
    private final int    ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    public static String ACCOUNT_REQUIRED             = "accountrequired";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String mainlink = link.getStringProperty("mainlink");
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(mainlink);
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (requiresAccount(link) && account == null) {
            throw new AccountRequiredException();
        }
        if (account != null) {
            /* Premium download */
            login(br, account, false);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), false, 1);
        } else {
            /* Free download */
            br.getHeaders().put("Referer", link.getStringProperty("mainlink"));
            final String fileID = this.getFID(link);
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.postPageRaw("https://www." + this.getHost() + "/downloads", "{\"id\": " + fileID + "}");
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String downloadID = entries.get("id").toString();
            final String dllink = brc.getURL("/downloads/" + downloadID + "/file").toExternalForm();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (this.br.containsHTML("You have reached your \\d+\\-hour download limit")) {
                /* Usually 24-H downloadlimit. */
                /* 2017-01-23: Limit seems to be 4-10 files per day - there are currently no premium accounts at all. */
                final String msg = " Downloadlimit reached";
                final long time = 5 * 60 * 1000;
                if (account != null) {
                    throw new AccountUnavailableException(msg, time);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, msg, time);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private boolean requiresAccount(final DownloadLink link) {
        return link.getBooleanProperty(ACCOUNT_REQUIRED, false);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://www." + account.getHoster() + "/");
                    if (isLoggedinHtml(br)) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                }
                br.getPage("https://www." + account.getHoster() + "/login");
                final String csrftoken = br.getRegex("name=\"csrf\\-token\" content=\"([^<>\"]+)\"").getMatch(0);
                if (csrftoken == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String postdata = "utf8=%E2%9C%93&authenticity_token=" + Encoding.urlEncode(csrftoken) + "&user%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bremember_me%5D=0&user%5Bremember_me%5D=1&commit=Sign+in";
                br.postPage(br.getURL(), postdata);
                if (br.getCookie(account.getHoster(), "remember_user_token") == null || !isLoggedinHtml(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    public static boolean isLoggedinHtml(final Browser br) {
        return br.containsHTML("/logout");
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (link != null && requiresAccount(link) && account == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}