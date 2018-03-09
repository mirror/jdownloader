//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepup.net" }, urls = { "https?://(?:www\\.|sp\\d+\\.)?filepup\\.net/(?:files|get)/[A-Za-z0-9]+\\.html" })
public class FilePupNet extends PluginForHost {
    public FilePupNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filepup.net/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    private static final String MAINPAGE                     = "http://www.filepup.net";
    private static final String APIKEY                       = "vwUhhGH6lPH3auk6SM144PBg3PRQg";
    /* Connection stuff */
    private final boolean       FREE_RESUME                  = false;
    private final int           FREE_MAXCHUNKS               = 1;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = false;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean       ACCOUNT_PREMIUM_RESUME       = true;
    private final int           ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private Browser prepBR(final Browser br) {
        br.setCookie(this.getHost(), "cookieconsent_dismissed", "yes");
        br.setFollowRedirects(true);
        return br;
    }

    // Using FlexshareScript 1.1.2 API Version
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use API with JDownloader API Key
        prepBR(this.br);
        br.getPage("http://www." + this.getHost() + "/api/info.php?api_key=" + APIKEY + "&file_id=" + new Regex(link.getDownloadURL(), "/(?:files|get)/([A-Za-z0-9]+)").getMatch(0));
        if (br.containsHTML("file does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("\\[file_name\\] => (.*?)\n").getMatch(0);
        String filesize = br.getRegex("\\[file_size\\] => (\\d+)\n").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set final filename here because hoster taggs files
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            /*
             * It may happen that the linkcheck via API is fine but free download will just return 404 while the file is online and
             * downloadable via (premium) account!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
        String getLink = getLink();
        if (getLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // waittime
        final String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 30;
        if (ttt != null) {
            tt = Integer.parseInt(ttt);
        }
        if (tt > 240) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, "task=download", resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            downloadErrorhandling();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLink() {
        String getLink = br.getRegex("disabled=\"disabled\" onclick=\"document\\.location=\\'(.*?)\\';\"").getMatch(0);
        if (getLink == null) {
            getLink = br.getRegex("(\\'|\")(" + "http://(www\\.)?([a-z0-9]+\\.)?" + MAINPAGE.replaceAll("(http://|www\\.)", "") + "/get/[A-Za-z0-9]+/\\d+/[^<>\"/]+)(\\'|\")").getMatch(1);
        }
        return getLink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(this.br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("http://www." + this.getHost() + "/index.php");
                    if (isLoggedIn()) {
                        this.br.setCookies(this.getHost(), cookies);
                        return;
                    }
                    this.br = prepBR(new Browser());
                }
                // br.getPage("");
                br.postPage("http://www." + this.getHost() + "/loginaa.php", "task=dologin&return=.%2Fmembers%2Fmyfiles.php&submit=Sign+In&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (this.br.containsHTML("You are already logged in on another device")) {
                    /* E.g. "<p class="description">You are already logged in on another device. Please log out first!</p> " */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "You are already logged in on another device. Please log out first!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (!isLoggedIn()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIn() {
        return this.br.containsHTML("class=\"fa fa\\-sign\\-out\"") || this.br.containsHTML("/logout\\.php\"");
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
        final String expire = br.getRegex("\\(Expires (\\d{2}\\-\\d{2}\\-\\d{4})").getMatch(0);
        ai.setUnlimitedTraffic();
        if (expire == null) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account");
        }
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        return ai;
    }

    /** 2016-08-19: No (premium) download API available(yet): http://www.filepup.net/api/docs.php */
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String getLink = getLink();
            if (getLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download", ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                downloadErrorhandling();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void downloadErrorhandling() throws PluginException, IOException {
        if (dl.getConnection().getResponseCode() == 405) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 405", 30 * 60 * 1000l);
        }
        br.followConnection();
        if (br.containsHTML(">You have reached the limit of")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        } else if (br.containsHTML(">\\s*This file does not exist\\.\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String unknownError = br.getRegex("class=\"error\">(.*?)\"").getMatch(0);
        if (unknownError != null) {
            logger.warning("Unknown error occured: " + unknownError);
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