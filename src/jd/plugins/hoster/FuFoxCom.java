//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.html.Form;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: $", interfaceVersion = 3, names = { "fufox.net" }, urls = { "https?://(?:www\\.)?fufox\\.(?:com|net)/dl/[A-Za-z0-9]{14}" }, flags = { 2 })
/**
 *
 *http://www.fufox.net/dl/55211A88983F42
 *http://www.fufox.net/dl/55211C0DBDB005
 *http://www.fufox.net/dl/55214FF4B8E9F6
 * @author raztoki
 *
 */
public class FuFoxCom extends PluginForHost {

    public FuFoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fufox.net/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fufox.net/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("fufox.com/", "fufox.net/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Convert old links to new ones
        correctDownloadLink(link);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<h1>This file does not exist\\.</h1>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] ff = br.getRegex("<h1\\s*>(.*?) \\(([\\d\\.]+ [a-z]{1,2})\\)</h1>").getRow(0);
        if (ff == null || ff.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(ff[0].trim());
        if (ff.length == 2 && ff[1] != null) {
            link.setDownloadSize(SizeFormatter.getSize(ff[1].replace("Mo", "Mb").replace("Go", "Gb")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private final void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        final Form download = br.getFormbyProperty("id", "downloadform");
        if (download == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String recaptchaV2Response = getRecaptchaV2Response();
        download.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(download);
        final String dllink = br.getRegex("<a href=\"([^\"]+)[^\r\n]+Save on computer</a>").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, HTMLEntities.unhtmlentities(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        // settings
        br.getPage("/settings.html");
        final String space = br.getRegex(">([\\d\\.]+ [a-z]{1,2})</span>\\s*<[^>]+>Disk Usage<").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space.trim().replace("Mo", "Mb").replace("Go", "Gb"));
        }
        ai.setUnlimitedTraffic();
        /* Only free is supported at the moment */
        if (true) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final String accept = br.getHeaders().get("Accept");
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(br.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.setHeader("X-Requested-With", "XMLHttpRequest");
                br.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
                br.postPage("http://www.fufox.net/php/ajax/ajax_login.php", "auto=1&hashkey=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(br.getHost(), "hash") == null && br.getCookie(br.getHost(), "hash_key_crypted") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(br.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            } finally {
                br.setHeader("X-Requested-With", null);
                if (accept != null) {
                    br.setHeader("Accept", accept);
                }
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getType().equals(Account.AccountType.FREE)) {
            doFree(link);
        }
        String dllink = br.getRegex("").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}