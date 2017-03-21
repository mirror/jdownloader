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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "solidfiles.com" }, urls = { "https?://(?:www\\.)?solidfiles\\.com/(?:d|v)/[a-z0-9]+/?" })
public class SolidFilesCom extends PluginForHost {

    public SolidFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.setStartIntervall(500l);
        this.enablePremium("https://www.solidfiles.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.solidfiles.com/terms/";
    }

    public static final String   DECRYPTFOLDERS               = "DECRYPTFOLDERS";
    private static final String  NOCHUNKS                     = "NOCHUNKS";

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_FREE_RESUME          = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Offline links should also get nice filenames */
        if (!link.isNameSet()) {
            link.setName(new Regex(link.getDownloadURL(), "solidfiles\\.com/v/([a-z0-9]+)").getMatch(0));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/error/") || br.containsHTML(">404<|>Not found<|>We couldn\\'t find the file you requested|Access to this file was disabled|The file you are trying to download has|>File not available|This file/folder has been disabled") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJsonValue(br, "name");
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) (?:-|\\|) Solidfiles</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        String filesize = PluginJSonUtils.getJsonValue(br, "size");
        if (filesize == null) {
            filesize = br.getRegex("class=\"filesize\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("dt>File size<.*?dd>(.*?)</").getMatch(0);
        }
        if (filesize == null) {
            /* 2017-03-21 */
            filesize = br.getRegex("</copy\\-button>([^<>\"]*?) \\-").getMatch(0);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception {
        if (br.containsHTML("We're currently processing this file and it's unfortunately not available yet")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is not available yet", 5 * 60 * 1000l);
        }
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            dllink = br.getRegex("class=\"direct-download regular-download\"[^\r\n]+href=\"(https?://[^\"']+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(\"|'|)(https?://s\\d+\\.solidfilesusercontent\\.com/.*?)\\1").getMatch(1);
            }
            if (dllink == null) {
                logger.warning("Final downloadlink is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        int maxChunks = maxchunks;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dllink = dllink.trim();
        downloadLink.setProperty(directlinkproperty, dllink);
        final long downloadCurrentRaw = downloadLink.getDownloadCurrentRaw();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 - use less connections and try again", 10 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                if (this.dl.externalDownloadStop()) {
                    return;
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw e;
            }
            if (downloadLink.getBooleanProperty(NOCHUNKS, false) == false) {
                if (downloadLink.getDownloadCurrent() > downloadCurrentRaw + (1024 * 1024l)) {
                    throw e;
                } else {
                    /* disable multiple chunks => use only 1 chunk and retry */
                    downloadLink.setProperty(NOCHUNKS, Boolean.TRUE);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
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

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                String bearertoken = getBearertoken(account);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www." + this.getHost() + "/login");
                String token = this.br.getRegex("name=token content=([^<>\"]+)>").getMatch(0);
                if (token == null) {
                    token = this.br.getCookie(this.br.getHost(), "csrftoken");
                }
                if (token == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("/login", "csrfmiddlewaretoken=" + Encoding.urlEncode(token) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                bearertoken = PluginJSonUtils.getJsonValue(this.br, "access_token");
                if (br.getCookie(this.br.getHost(), "sessionid") == null || bearertoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("bearertoken", bearertoken);
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
        prepBRAjax(account);
        this.br.getPage("https://www." + this.getHost() + "/api/payments?limit=25&offset=0");
        ai.setUnlimitedTraffic();
        final String subscriptioncount = PluginJSonUtils.getJsonValue(this.br, "count");
        if (subscriptioncount == null || subscriptioncount.equals("0")) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            /* TODO: Add expire date */
            // final String expire = null;
            // if (expire != null) {
            // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            // }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        account.setValid(true);
        return ai;
    }

    private void prepBRAjax(final Account account) {
        final String csrftoken = getCsrftoken();
        final String bearertoken = getBearertoken(account);
        if (csrftoken != null) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (bearertoken != null) {
            br.getHeaders().put("Authorization", "Bearer " + bearertoken);
        }
    }

    private String getBearertoken(final Account account) {
        return account.getStringProperty("bearertoken", null);
    }

    private String getCsrftoken() {
        return this.br.getCookie(this.br.getHost(), "csrftoken");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            /* TODO: Add premium support! */
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = br.getRegex("").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SolidFilesCom.DECRYPTFOLDERS, JDL.L("plugins.hoster.solidfilescom.decryptfolders", "Decrypt subfolders in folders")).setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}