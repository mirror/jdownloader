//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "partagora.com" }, urls = { "http://partagoradecrypted\\.com/\\d+" })
public class PartagoraCom extends PluginForHost {

    public PartagoraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
    }

    @Override
    public String getAGBLink() {
        return "http://partagora.com/terms-of-service";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url = link.getStringProperty("mainlink", null);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(url);
        if (br.containsHTML("class=\"noFile\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    public void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handlePWProtected(downloadLink);
        String req_token = this.br.getCookie(MAINPAGE, "__RequestVerificationToken");
        if (req_token == null) {
            req_token = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        }
        final String fid = downloadLink.getStringProperty("plain_fid", null);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        final String dllink_stream = this.br.getRegex("data\\-player=\"video\" data\\-player\\-file=\"(http[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            if (req_token != null) {
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/action/DownloadFile?location=fi&f=" + fid, "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(req_token));
                dllink = PluginJSonUtils.getJsonValue(br, "DownloadUrl");
            }
            if (dllink == null) {
                /* Try fallback to stream-url. Usually needed if a file is only downloadable via account. */
                // dllink = dllink_stream; // This is only 10 minutes video
            }
            if (dllink == null) {
                if (this.br.containsHTML("\"login")) {
                    /* Free- or premium account needed! */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getResponseCode() != 206) {
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

    private void handlePWProtected(final DownloadLink dl) throws PluginException, IOException {
        String passCode = dl.getStringProperty("pass", null);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
            final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (int i = 1; i <= 3; i++) {
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", dl);
                }
                br.postPageRaw("/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    passCode = null;
                    dl.setProperty("pass", Property.NULL);
                    continue;
                }
                break;
            }
            if (br.containsHTML("\"IsSuccess\":false")) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            /* We don't want to work with the encoded json bla html response */
            br.getPage(dl.getStringProperty("mainlink", null));
        }
    }

    private static final String MAINPAGE = "http://partagora.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("deprecation")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(MAINPAGE, cookies);
                    return;
                }
                // br.setAllowedResponseCodes(500);
                br.getPage(MAINPAGE + "/");
                br.getPage("/action/Account/Login?returnUrl=%2F&TimeStamp=" + System.currentTimeMillis());
                String req_token = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (req_token == null) {
                    req_token = this.br.getCookie(MAINPAGE, "__RequestVerificationToken");
                }
                if (req_token == null) {
                    req_token = this.br.getCookie(MAINPAGE, "__RequestVerificationToken_v2");
                }
                if (req_token == null) {
                    req_token = "undefined";
                }

                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                // this.br.getHeaders().put("Content-Type", "application/json; charset=utf-8");
                this.br.getHeaders().put("Referer", MAINPAGE + "/");
                br.postPage("/action/Account/Login?returnUrl=%2F", "__RequestVerificationToken=" + req_token + "&UserName=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, ".ASPXAUTH") == null && br.getCookie(MAINPAGE, ".ASPXAUTH_v2") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage(MAINPAGE + "/");
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        /* Only free accounts are supported so far */
        if (true) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Registered (free) user");
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        br.getPage(link.getStringProperty("mainlink", null));
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            doFree(link, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "account_premium_directlink");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.ChomikujPlScript;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}