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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fileload.io" }, urls = { "https?://(?:www\\.)?fileloaddecrypted\\.io/[A-Za-z0-9]+/[^/]+" }) 
public class FileloadIo extends PluginForHost {

    public FileloadIo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fileload.io/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://fileload.io/tos";
    }

    /* Notes: Also known as/before: netload.in, dateisenden.de */
    private static final String API_BASE                                = "https://api.fileload.io/";
    public static final boolean USE_API_FOR_FREE_UNREGISTERED_DOWNLOADS = true;
    /* Connection stuff */
    private final boolean       FREE_RESUME                             = true;
    private final int           FREE_MAXCHUNKS                          = 1;
    private final int           FREE_MAXDOWNLOADS                       = 1;
    private final boolean       ACCOUNT_FREE_RESUME                     = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS                  = -2;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS               = 1;
    private final boolean       ACCOUNT_PREMIUM_RESUME                  = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS               = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS            = 20;

    private String              account_auth_token                      = null;
    private String              dllink                                  = null;
    private boolean             server_issues                           = false;
    private String              folderid                                = null;
    private String              linkid                                  = null;

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("fileloaddecrypted.io/", "fileload.io/"));
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    public static Browser prepBRWebsite(final Browser br) {
        return br;
    }

    /*
     * This hoster plugin is VERY basic because it is not clear whether this service will gain any popularity and maybe there will be an API
     * in the future!
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        folderid = getFolderid(link.getDownloadURL());
        linkid = getFreeLinkid(link);
        if (folderid == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBRAPI(this.br);
        final String folder_url_part = folderid + "/" + Encoding.urlEncode(getFilenameProperty(link));
        br.getPage("https://api." + this.getHost() + "/onlinestatus/" + folder_url_part);
        final String error = PluginJSonUtils.getJson(this.br, "error");
        if (error != null) {
            /* E.g. "{"error":"unknown_transfer_id","action":"You must enter a valid transfer_id"}" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sha1 = PluginJSonUtils.getJson(this.br, "");
        final String filesize_bytes = PluginJSonUtils.getJson(this.br, "filesize_bytes");
        final String status = PluginJSonUtils.getJson(this.br, "status");
        if (filesize_bytes == null || status == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!"online".equalsIgnoreCase(status)) {
            /* Double-check */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (sha1 != null) {
            link.setSha1Hash(sha1);
        }
        link.setDownloadSize(Long.parseLong(filesize_bytes));
        return AvailableStatus.TRUE;
    }

    private void getDllinkWebsite() throws IOException, PluginException {
        if (linkid == null) {
            /* Premiumonly */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        this.br.getPage("https://" + this.getHost() + "/index.php?id=5&f=attemptDownload&transfer_id=" + folderid + "&file_id=" + linkid + "&download=true");
        final String status = PluginJSonUtils.getJson(this.br, "status");
        if ("too_many_requests".equalsIgnoreCase(status)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Too many requests'", 3 * 60 * 1000l);
        }
        dllink = PluginJSonUtils.getJson(this.br, "link");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String getFilenameProperty(final DownloadLink dl) {
        return dl.getStringProperty("directfilename", null);
    }

    public static boolean mainlinkIsOffline(final Browser br) {
        final boolean isOffline = br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 \\- Not Found|The requested page was not found");
        return isOffline;
    }

    private String getFolderid(final String url) {
        return new Regex(url, "fileload\\.io/([A-Za-z0-9]+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    private String getFreeLinkid(final DownloadLink dl) {
        String free_linkid = dl.getStringProperty("free_download_fileid", null);
        if (free_linkid == null) {
            /* Fallback/Old handling - until 2016-06-13, linkid was RegExed out of downloadurl */
            free_linkid = new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
        }
        return free_linkid;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (USE_API_FOR_FREE_UNREGISTERED_DOWNLOADS) {
            downloadAPI(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
        } else {
            downloadFreeWebsite(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
        }
    }

    private void downloadFreeWebsite(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        this.br = prepBRWebsite(new Browser());
        getDllinkWebsite();
        /* Website */
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (dllink == null || !dllink.startsWith("http")) {
            dllink = checkDirectLink(downloadLink, directlinkproperty);
            if (dllink == null) {
                handleErrorsAPI();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (this.br.containsHTML("class=\"block\"")) {
                /*
                 * <h1>Oops! No premium...</h1> <h2>You can't download more than one file without an premium account. Hang tight.</h2>
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before more downloads can be started", 3 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setFollowRedirects(false);
            getAuthToken(account);
            if (this.account_auth_token != null) {
                br.getPage(API_BASE + "accountinfo/" + Encoding.urlEncode(this.account_auth_token));
                if (PluginJSonUtils.getJson(this.br, "email") != null) {
                    /* Saved account_auth_token is still valid! */
                    return;
                }
            }
            br.getPage(API_BASE + "login/" + Encoding.urlEncode(account.getUser()) + "/" + JDHash.getMD5(account.getPass()));
            account_auth_token = PluginJSonUtils.getJson(this.br, "login_token");
            if (br.containsHTML("login_failed") || account_auth_token == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            saveAuthToken(account);
        }
    }

    private void getAuthToken(final Account acc) {
        this.account_auth_token = acc.getStringProperty("account_auth_token", null);
    }

    private void saveAuthToken(final Account acc) {
        acc.setProperty("account_auth_token", this.account_auth_token);
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
        final boolean isPremium = "1".equals(PluginJSonUtils.getJson(this.br, "premium"));
        if (!isPremium) {
            account.setType(AccountType.FREE);
            /* Free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            final String valid_millisecs = PluginJSonUtils.getJson(this.br, "valid_millisecs");
            ai.setValidUntil(Long.parseLong(valid_millisecs));
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        final String directlinkproperty;
        int maxchunks;
        boolean resume;
        if (account.getType() == AccountType.FREE) {
            directlinkproperty = "directlink_free_account";
            maxchunks = ACCOUNT_FREE_MAXCHUNKS;
            resume = ACCOUNT_FREE_RESUME;
        } else {
            directlinkproperty = "directlink_premium_account";
            maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
            resume = ACCOUNT_PREMIUM_RESUME;
        }
        downloadAPI(link, resume, maxchunks, directlinkproperty);
    }

    private void downloadAPI(final DownloadLink link, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception {
        String dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            String api_download_url_call = API_BASE + "download/";
            if (this.account_auth_token != null) {
                /* E.g. (free|premium) account download. */
                api_download_url_call += Encoding.urlEncode(this.account_auth_token) + "/";
            }
            api_download_url_call += this.folderid + "/" + Encoding.urlEncode(getFilenameProperty(link));
            br.getPage(api_download_url_call);
            dllink = PluginJSonUtils.getJson(this.br, "download_link");
            if (dllink == null || !dllink.startsWith("http")) {
                handleErrorsAPI();
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
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
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void handleErrorsAPI() throws PluginException {
        final String error = PluginJSonUtils.getJson(this.br, "error");
        final String status = PluginJSonUtils.getJson(this.br, "status");
        if (error != null) {
            if (error.equalsIgnoreCase("file_not_found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.warning("Unknown API error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (status != null) {
            if (status.equalsIgnoreCase("You have used up your free quota! Please try again later.")) {
                /* Only for free(account) download as far as I know. */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            } else if (status.equalsIgnoreCase("unavailable")) {
                /* Only for free(account) download as far as I know. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not possible at the moment");
            }
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