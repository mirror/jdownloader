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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitster.cz" }, urls = { "https?://(?:www\\.)?bitster\\.(?:cz|sk)/(?:#?file|download)/[a-z0-9]+" }, flags = { 2 })
public class BitsterCz extends PluginForHost {

    public BitsterCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://bitster.cz/");
    }

    @Override
    public String getAGBLink() {
        return "https://bitster.cz/terms";
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

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private String               fid                          = null;

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.fid = getFID(link);
        link.setLinkID(this.fid);
        this.setBrowserExclusive();
        prepBR(this.br);
        this.br.getPage("https://bitster.cz/api/file_validate?param=" + this.fid);
        if (this.br.getHttpConnection().getResponseCode() == 404 || !this.br.toString().equals("\"True\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.getPage("/page/getpagecontent?page=/file/" + this.fid + "&loadedpage=&_=" + System.currentTimeMillis());
        this.br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
        /* Do not perform any additional offline checks - we trust their API! */
        // if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Hups, soubor byl smaz")) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        String filename = br.getRegex("property=\"og:title\"[\t\n\r ]*?content=\"([^<>\"]*?) \\- Bitster\\.cz\"").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("OpenSansRegular text-grey9 text-left break-word-all\"u003ern([^<>\"]*?)rn[\t\n\r ]+u003c/div").getMatch(0);
        }
        if (filename == null) {
            filename = this.fid;
        }
        final String filesize = br.getRegex("text-white left-30 margin-0 width-auto\"u003e([^<>\"/]*?) / .+").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        /* Do NOT set final filenames here!! Trust their server filenames! */
        link.setName(filename);
        if (filesize != null) {
            /* Filesize should usually be given - we just don't want our plugin to fail because of missing filesize! */
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            dllink = getDllink();
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

    private String getDllink() throws IOException, PluginException {
        getPage("/api/file_download?param=" + this.fid);
        final String dllink = this.br.toString().replace("\"", "");
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
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

    // private static final String MAINPAGE = "http://bitster.cz";
    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage("https://bitster.cz/api/validatebasicauth");
                if (!this.br.toString().equals("\"True\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Basic Auth should be fine but let's save the cookies anyways! */
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
        /* Also possible: /user/getcreditsheadermenuvalue?_=1445712393099 */
        /* Also possible: /user/getprofitheadermenuvalue?_=1445712393100 */
        this.br.getPage("/user/getcreditsheadervalue?_=" + System.currentTimeMillis());
        final String trafficleft_str = this.br.getRegex("\"(\\d+(?:\\.\\d+)? ?(KB|MB|GB|TB))\"").getMatch(0);
        long trafficleft = 0;
        if (trafficleft_str != null) {
            trafficleft = SizeFormatter.getSize(trafficleft_str);
        }
        if (trafficleft <= 0) {
            /*
             * Okay let's assume they only have traffic based premium models so basically all accounts would be premium so only if there is
             * zero traffic --> Free account
             */
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
            ai.setTrafficLeft(0);
        } else {
            /**/
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
            ai.setTrafficLeft(trafficleft);
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            /*
             * This will probably never happen as free accounts usually have 0 traffic so they will never be used - nevertheless, that might
             * be useful for the future ...
             */
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = getDllink();
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

    private void getPage(final String url) throws IOException, PluginException {
        this.br.getPage(url);
        apiHandleErrors();
    }

    private void apiHandleErrors() throws PluginException {
        if (this.br.containsHTML("\"DELETED\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("\"ABUSED\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("\"NOTFOUND\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("\"NOTAVAILABLE\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        } else if (this.br.containsHTML("\"ERROR\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 1 * 60 * 60 * 1000l);
        } else if (this.br.containsHTML("\"NOTLOGGEDIN\"")) {
            // TODO
        } else if (this.br.containsHTML("\"NOTENOUGHCREDITS\"")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (this.br.containsHTML("\"PRIVATE\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private file");
        } else if (this.br.containsHTML("\"LOCKED\"")) {
            // TODO
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}