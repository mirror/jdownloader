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

import jd.PluginWrapper;
import jd.controlling.AccountController;
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
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "seedr.cc" }, urls = { "https?://[A-Za-z0-9\\-]+\\.seedr\\.cc/downloads/.+|http://seedrdecrypted\\.cc/\\d+" })
public class SeedrCc extends PluginForHost {
    public SeedrCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.seedr.cc/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www.seedr.cc/dynamic/terms";
    }

    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = -8;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = -8;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean       ACCOUNT_PREMIUM_RESUME       = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS    = -8;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String TYPE_DIRECTLINK              = "https?://[A-Za-z0-9\\-]+\\.seedr\\.cc/downloads/.+";
    private static final String TYPE_NORMAL                  = "http://seedrdecrypted\\.cc/\\d+";
    private boolean             server_issues                = false;
    private String              dllink                       = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        server_issues = false;
        String filename = null;
        if (link.getDownloadURL().matches(TYPE_DIRECTLINK)) {
            dllink = link.getDownloadURL();
        } else {
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null) {
                return AvailableStatus.UNCHECKABLE;
            }
            this.login(this.br, aa, false);
            prepAjaxBr(this.br);
            final String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            this.br.postPage("https://www." + this.getHost() + "/content.php?action=fetch_file", "folder_file_id=" + fid);
            dllink = PluginJSonUtils.getJsonValue(this.br, "url");
            filename = PluginJSonUtils.getJsonValue(this.br, "name");
        }
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    if (filename == null) {
                        link.setFinalFileName(getFileNameFromHeader(con));
                    }
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
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
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    prepAjaxBr(br);
                    br.postPage("https://www." + this.getHost() + "/content.php?action=get_settings", "");
                    if (!br.containsHTML("\"login_required\"")) {
                        br.setCookies(account.getHoster(), cookies);
                        return;
                    }
                    br = new Browser();
                }
                final DownloadLink dlinkbefore = this.getDownloadLink();
                if (dlinkbefore == null) {
                    this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "http://" + account.getHoster(), true));
                }
                br.getPage("https://www." + this.getHost());
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LfhAyMTAAAAAJD3uGiFfUcoXSiVsRKJedWSrSmv").getToken();
                if (dlinkbefore != null) {
                    this.setDownloadLink(dlinkbefore);
                }
                String postData = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&rememberme=on";
                postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                prepAjaxBr(br);
                br.postPageRaw("https://www.seedr.cc/actions.php?action=login", postData);
                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                this.br.postPage("https://www." + this.getHost() + "/content.php?action=get_devices", "");
                if (br.getCookie(account.getHoster(), "remember") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    public static void prepAjaxBr(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        prepAjaxBr(this.br);
        this.br.postPage("https://www." + this.getHost() + "/content.php?action=get_memory_bandwidth", "");
        final String is_premium = PluginJSonUtils.getJsonValue(this.br, "is_premium");
        final String bandwidth_used = PluginJSonUtils.getJsonValue(this.br, "bandwidth_used");
        final String bandwidth_max = PluginJSonUtils.getJsonValue(this.br, "bandwidth_max");
        final String space_used = PluginJSonUtils.getJsonValue(this.br, "space_used");
        if (space_used != null && space_used.matches("\\d+")) {
            ai.setUsedSpace(Long.parseLong(space_used));
        }
        if (is_premium == null || !is_premium.equals("1")) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Registered (free) user");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            if (bandwidth_used != null && bandwidth_max != null && bandwidth_used.matches("\\d+") && bandwidth_max.matches("\\d+")) {
                ai.setTrafficMax(Long.parseLong(bandwidth_max));
                ai.setTrafficLeft(ai.getTrafficMax() - Long.parseLong(bandwidth_used));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account");
        }
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            // String dllink = this.checkDirectLink(link, "premium_directlink");
            // if (dllink == null) {
            // dllink = br.getRegex("").getMatch(0);
            // if (dllink == null) {
            // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}