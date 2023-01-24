//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "file4go.net" }, urls = { "https?://(?:www\\.)?file4go\\.(?:net|com)/[^/]+/([a-zA-Z0-9_=]+)" })
public class File4GoCom extends antiDDoSForHost {
    public File4GoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

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

    private static final String MAINPAGE = "http://www.file4go.net";

    /** 2023-01-24: They're GEO-blocking all except brazil IPs via Cloudflare! */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "animesonline", "1");
        br.setCookie(MAINPAGE, "musicasab", "1");
        br.setCookie(MAINPAGE, "poup", "1");
        br.setCookie(MAINPAGE, "noadvtday", "0");
        br.setCookie(MAINPAGE, "hellpopab", "1");
        // do not follow redirects, as they can lead to 404 which is faked based on country of origin ?
        getPage(getContentURL(link));
        redirectControl(br);
        if (br.containsHTML(">404 ARQUIVO N�O ENCOTRADO<|Arquivo Temporariamente Indisponivel|ARQUIVO DELATADO PELO USUARIO OU REMOVIDO POR <|ARQUIVO DELATADO POR <b>INATIVIDADE|O arquivo Não foi encotrado em nossos servidores")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">Nome:</b>\\s*(.*?)\\s*(?:</p>)?</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div id=\"titulo_a\">\\s*(.*?)\\s*</div>").getMatch(0);
        }
        String filesize = br.getRegex("(?i)>\\s*Tamanho\\s*:\\s*</b>\\s*(.*?)\\s*</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("(?i)<b>File size\\s*:\\s*</b>\\s*([^<>\"]+)\\s*</").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if ("".equals(filename) && "0 Bytes".equals(filesize)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private String getContentURL(final DownloadLink link) {
        final String hostFromAddedURL = Browser.getHost(link.getPluginPatternMatcher());
        return link.getPluginPatternMatcher().replaceFirst(hostFromAddedURL, this.getHost());
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String directLink = checkDirectLink(link, "directlink");
        if (directLink == null) {
            Form form = br.getFormByRegex(".*CRIAR DOWNLOAD.*");
            if (form == null) {
                form = br.getFormbyActionRegex(".*/getdownload.*");
            }
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            submitForm(form);
            if (br.containsHTML("(?i)REMOVED DMCA\\!")) {
                /* 2023-01-24 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            directLink = getDllink(br);
            if (directLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directLink, true, -2);
        /* resume no longer supported */
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.containsHTML("BAIXE SIMULTANEAMENTE COM VELOCIDADE MÁXIMA")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink", directLink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!force) {
                        /* Do not verify cookies */
                        return;
                    }
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                logger.info("Performing full login");
                getPage(MAINPAGE);
                redirectControl(br);
                postPage("/login.html", "acao=logar&login=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass()));
                redirectControl(br);
                final String lang = System.getProperty("user.language");
                if (br.getHttpConnection().getResponseCode() == 404) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nServer Fehler 404 - Login momentan nicht möglich!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIServer error 404 - login not possible at the moment!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (isLoggedIN(br)) {
                    if ("de".equalsIgnoreCase(lang)) {
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

    private boolean isLoggedIN(final Browser br) {
        if (br.getCookie(MAINPAGE, "FILE4GO", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * because this host will randomly redirect to /error.php... Think this is actually because referrer is blank on first request. login
     * can randomly do it also, and be identified as free user, but if you have referrer it doesn't -raztoki20160809
     *
     * @param br
     * @throws Exception
     */
    private void redirectControl(Browser br) throws Exception {
        String redirect = null;
        int redirectNum = 0;
        while ((redirect = br.getRedirectLocation()) != null && !redirect.endsWith("/error.php") && redirectNum < 10) {
            getPage(redirect);
            redirectNum++;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("(?i)>\\s*Premium Stop: (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) </b>").getMatch(0);
        if (expire == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd hh:mm:ss", Locale.ENGLISH));
        }
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        getPage(getContentURL(link));
        final String dllink = getDllink(br);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink(final Browser br) {
        String dllink = br.getRegex("(?i)href=\"(https?://[^\"]+)\">\\s*Download").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(?i)class\\s*=\\s*\"novobotao download\"[^>]*href\\s*=\\s*\"([^\"]+)\">").getMatch(0);
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}