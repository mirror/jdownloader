//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserDownloadInterface;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "file-share.top" }, urls = { "https?://(?:www\\.)?file\\-share\\.top/file/\\d+/[^/]+" })
public class FileShareTop extends PluginForHost {
    private static final String  TYPE_CURRENT                 = "https?://(?:www\\.)?file\\-share\\.top/file/\\d+/[^/]+";
    private static final String  MAINPAGE                     = "http://file-share.top/";
    /* Connection stuff */
    // private static final boolean FREE_RESUME = true;
    // private static final int FREE_MAXCHUNKS = 0;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    public FileShareTop(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "dobiti/?zeme=1");
    }

    @Override
    public String[] siteSupportedNames() {
        // this shall prevent share-rapid.cz from been tested, it has no dns record.
        return new String[] { "file-share.top" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "share-rapid.cz".equals(host) || "sharerapid.cz".equals(host) || "sharerapid.sk".equals(host) || "megarapid.cz".equals(host) || "share-rapid.cz".equals(host)) {
            return "file-share.top";
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.getDownloadURL().matches(TYPE_CURRENT)) {
            /* Older urls --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setBrowserExclusive();
        prepBr(br);
        br.getPage(link.getDownloadURL());
        /* Set english language, auto redirects back! */
        br.setFollowRedirects(true);
        br.getPage("/lang/set/gb");
        checkOffline();
        String filename = br.getRegex("class=\"fa fa-file-o\"></i>([^<>\"]*?)<small>").getMatch(0);
        final String filesize = br.getRegex("<strong>Velikost:</strong>([^<>\"]*?)</p>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    private void checkOffline() throws PluginException {
        if (br.containsHTML("Nastala chyba 404") || br.containsHTML("Soubor byl smazán") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        long realTraffic = 0l;
        String trafficleft = null;
        /**
         * Expire unlimited -> Unlimited traffic for a specified amount of time Normal expire -> Expire date + trafficleft
         *
         */
        String expireUnlimited = br.getRegex("<td>Paušální stahování aktivní\\. Vyprší </td><td><strong>([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4} - [0-9]{1,2}:[0-9]{1,2})</strong>").getMatch(0);
        if (expireUnlimited != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireUnlimited, "dd.MM.yy - HH:mm", Locale.ENGLISH), br);
        }
        if (expireUnlimited == null) {
            expireUnlimited = br.getRegex("<td>Your unlimited download expires</td>\\s*<td>\\s*in \\d+ days\\s*<small>\\(([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4})\\)").getMatch(0);
            if (expireUnlimited != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireUnlimited, "dd.MM.yyyy", Locale.ENGLISH), br);
            }
        }
        if (expireUnlimited != null) {
            /* TODO: Check if this case still exists */
            ai.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account - Unlimited Traffic");
            account.setValid(true);
            return ai;
        } else {
            trafficleft = br.getMatch("class=\"fa fa\\-database\"></i>\\s*([^<>\"]*?)\\s*</a>");
            if (trafficleft != null) {
                logger.info("Available traffic equals: " + trafficleft);
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
                realTraffic = SizeFormatter.getSize(trafficleft);
            } else {
                ai.setUnlimitedTraffic();
            }
            final String expires = br.getMatch("Neomezený tarif vyprší</td><td><strong>\\s*([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4}\\s*-\\s*[0-9]{1,2}:[0-9]{1,2})\\s*</strong>");
            if (expires != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy - HH:mm", Locale.ENGLISH));
            }
        }
        if (realTraffic > 0l) {
            /**
             * Max simultan downloads (higher than 1) only works if you got any
             */
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
            final String maxSimultanDownloads = br.getRegex("<td>Max\\.\\s*počet paralelních stahování:\\s*</td><td>\\s*(\\d+)\\s*<a href").getMatch(0);
            if (maxSimultanDownloads != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(maxSimultanDownloads));
            } else {
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            }
            account.setConcurrentUsePossible(true);
        } else {
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "informace/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Account only */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dllink = checkDirectLink(downloadLink, "directlink_new");
        if (dllink == null) {
            requestFileInformation(downloadLink);
            br = new Browser();
            final long cookieTimeStamp = login(account, false);
            br.setCookie(getHost(), "arp_scroll_position", "0");
            br.getPage(downloadLink.getDownloadURL());
            dllink = downloadLink.getDownloadURL().replace("/file/", "/file/download/");
            /*
             * this site is stupid, here you request > get fileserver via redirect > then it might redirect into error code which then
             * infinite redirect loops! Since redirects are off, lets do a request and get the SERVER, if error, parse the error code, do
             * not follow!
             */
            br.getPage(dllink);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.endsWith("/login")) {
                synchronized (account) {
                    if (account.getCookiesTimeStamp("") == cookieTimeStamp) {
                        account.clearCookies("");
                    }
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Expired Session");
            }
        }
        /* Cookies not needed for the download process. */
        final Browser br2 = new Browser();
        prepBr(br2);
        br2.setCookie(dllink, "arp_scroll_position", "0");
        br2.getHeaders().put("Referer", br.getHeaders().get("Referer"));
        logger.info("Final downloadlink = " + dllink);
        final BrowserDownloadInterface brAd = new BrowserDownloadInterface() {
            @Override
            public void handleBlockedRedirect(final String redirect) throws PluginException {
                if (redirect.matches(".+/\\?error=2$")) {
                    // unknown error message type...
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
                } else if (redirect.matches(".+/\\?error=\\d+$")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Error Handling, Please report issue!");
                }
                super.handleBlockedRedirect(redirect);
            };
        };
        dl = brAd.openDownload(br2, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 400", 5 * 60 * 1000l);
            }
            br2.followConnection();
            if (br2.containsHTML("(was not found on this server|No htmlCode read)")) {
                /** Show other errormessage if free account was used */
                if (account.getType() == AccountType.FREE) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Maybe this file cannot be downloaded as a freeuser: Buy traffic or try again later", 60 * 60 * 1000);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000);
            } else if (br2.getURL().contains("/panel/credit")) {
                logger.info("Traffic empty / Not enough traffic to download this file");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNot enough traffic available!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            brAd.handleBlockedContent(br2);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    public long login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                /** Load cookies */
                br.setFollowRedirects(false);
                br.setDebug(true);
                prepBr(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.getPage(MAINPAGE);
                    if (this.br.getCookies(MAINPAGE).get("PHPSESSID", Cookies.NOTDELETEDPATTERN) == null) {
                        br.clearCookies(getHost());
                    } else {
                        return account.saveCookies(br.getCookies(getHost()), "");
                    }
                }
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE + "login");
                /* Set english language, auto redirects back! */
                br.getPage("/lang/set/gb");
                final String lang = System.getProperty("user.language");
                final Form form = this.br.getFormbyKey("password");
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                form.put("email", Encoding.urlEncode(account.getUser()));
                form.put("password", Encoding.urlEncode(account.getPass()));
                form.put("remember", "yes");
                br.submitForm(form);
                if (!br.containsHTML("class=\"fa fa-power-off\"") || this.br.getCookies(MAINPAGE).get("PHPSESSID", Cookies.NOTDELETEDPATTERN) == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
                return account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                br.setFollowRedirects(ifr);
            }
        }
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    private void prepBr(final Browser br) throws IOException {
        br.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        br.setCustomCharset("UTF-8");
        // DO NOT SET LANGUAGE HERE, its stored within COOKIE!
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}