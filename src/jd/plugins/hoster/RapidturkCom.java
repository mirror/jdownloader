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
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.DefaultAuthenticanFactory;
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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidturk.com" }, urls = { "https?://(?:www\\.)?rapidturk\\.com/files/[A-Za-z0-9]+\\.html" })
public class RapidturkCom extends Ftp {
    public RapidturkCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.rapidturk.com/premium/get");
    }

    @Override
    public String getAGBLink() {
        return "http://www.rapidturk.com/help/faq";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = true;
    private final int     FREE_MAXCHUNKS               = 0;
    private final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean ACCOUNT_FREE_RESUME          = true;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">Download:\\&nbsp;<h1> \\(\\)</h1>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = this.br.getRegex("class=\"fl\">Download:\\&nbsp;<h1>([^<>\"]+) \\(([^\\(\\)]+)\\)</h1>");
        String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
        this.br.getPage(this.br.getURL().replace("/files/", "/get/"));
        if (this.br.getURL().length() < 28 || this.br.containsHTML(">This file is available with Premium only")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        String dllink = br.getRegex("").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text") || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html")) {
            try {
                br.followConnection();
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
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

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* 2017-01-23: Always check cookies/session here - seems like sessions may expire randomly/within a few minutes. */
                    this.br.setCookies(this.getHost(), cookies);
                    this.br.getPage("http://www." + this.getHost() + "/members/myfiles");
                    if (isLoggedInHtml()) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                }
                br.postPage("http://www." + this.getHost() + "/account/login", "task=dologin&return=%2Fmembers%2Fmyfiles&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedInHtml()) {
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

    private boolean isLoggedInHtml() {
        return this.br.containsHTML("/account/logout");
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
        final String create = this.br.getRegex("Registered\\s*?:\\s*?(\\d{2}\\-\\d{2}\\-\\d{4} @ \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        final String expire = this.br.getRegex("Premium Expires\\s*?:\\s*?(\\d{2}\\-\\d{2}\\-\\d{4} @ \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        ai.setUnlimitedTraffic();
        if (create != null) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy '@' HH:mm:ss", Locale.ENGLISH));
        }
        if (expire == null) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy '@' HH:mm:ss", Locale.ENGLISH));
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
        sleep(2000, link);// required because of server issue, to fast loading
        br.getPage(link.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = br.getRegex("\\'(ftp://[^<>\"\\']+)\\'").getMatch(0);
            if (dllink == null) {
                /* 2017-02-03: New */
                dllink = this.br.getRegex("class=\"download\\-button\\-orange\" onclick=\"location\\.href=\\'([^<>\"\\']+)\\'").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dllink.startsWith("ftp://") && !dllink.startsWith("http")) {
                /* 2017-02-03: New */
                dllink = "ftp://" + dllink;
            }
            if (dllink.startsWith("http")) {
                final String auth = new Regex(dllink, "https?://([^/]+)@").getMatch(0);
                if (auth == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String username = new Regex(auth, "(.*?):").getMatch(0);
                final String password = new Regex(auth, ":(.+)").getMatch(0);
                dllink = dllink.replaceFirst(Pattern.quote(auth) + "@", "");
                br.setCustomAuthenticationFactory(new DefaultAuthenticanFactory(Browser.getHost(dllink), null, username, password));
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
                if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text") || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html")) {
                    try {
                        br.followConnection();
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
            } else {
                download(dllink, link, true);
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