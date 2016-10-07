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
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "issuu.com" }, urls = { "https?://issuudecrypted\\.com/[a-z0-9\\-_\\.]+/docs/[a-z0-9\\-_]+" })
public class IssuuCom extends PluginForHost {

    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://issuu.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://issuu.com/acceptterms";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("issuudecrypted.com/", "issuu.com/"));
    }

    private String DOCUMENTID = null;

    /** Using oembed API: http://developers.issuu.com/api/oembed.html */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* Tyically this oembed API returns 501 for offline content */
        this.br.setAllowedResponseCodes(501);
        this.br.setFollowRedirects(true);
        final String filename = link.getStringProperty("finalname", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.getPage("https://issuu.com/oembed?format=json&url=" + Encoding.urlEncode(link.getDownloadURL()));
        if (this.br.getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private static final String MAINPAGE = "http://issuu.com";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                final String lang = System.getProperty("user.language");
                if (isValidMailAdress(account.getUser())) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte den Benutzername und nicht die Mailadresse in das 'Benutzername' Feld eingeben!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInstead of using your mailadress, please enter your username in the 'username' field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setFollowRedirects(false);
                br.getHeaders().put("Accept", "*/*");
                // br.getPage("https://api.issuu.com/query?username=" + Encoding.urlEncode(account.getUser()) + "&password=" +
                // Encoding.urlEncode(account.getPass()) +
                // "&permission=f&loginExpiration=standard&action=issuu.user.login&format=json&jsonCallback=_jqjsp&_" +
                // System.currentTimeMillis() + "=");
                this.br.getPage("https://" + this.getHost() + "/signin?onLogin=%2F");
                String postdata = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&permission=f&loginExpiration=standard&action=issuu.user.login&format=json";
                final String csrf = this.br.getCookie(this.getHost(), "issuu.model.lcsrf");
                if (csrf != null) {
                    postdata += "&loginCsrf=" + Encoding.urlEncode(csrf);
                }
                br.postPage("https://" + this.br.getHost() + "/query", postdata);
                if (br.getCookie(MAINPAGE, "site.model.token") == null || br.containsHTML("\"message\":\"Login failed\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
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

    private boolean isValidMailAdress(final String value) {
        return value.matches(".+@.+");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        DOCUMENTID = this.br.getRegex("\"thumbnail_url\":\"https?://image\\.issuu\\.com/([^<>\"/]*?)/").getMatch(0);
        if (DOCUMENTID == null) {
            this.br.getPage(link.getDownloadURL());
            if (br.containsHTML(">We can\\'t find what you\\'re looking for") || this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DOCUMENTID = PluginJSonUtils.getJsonValue(br, "documentId");
        }
        if (DOCUMENTID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        login(account, true);
        final String token = br.getCookie(MAINPAGE, "site.model.token");
        br.getPage("http://api." + this.getHost() + "/query?documentId=" + this.DOCUMENTID + "&username=" + Encoding.urlEncode(account.getUser()) + "&token=" + Encoding.urlEncode(token) + "&action=issuu.document.download&format=json&jsonCallback=_jqjsp&_" + System.currentTimeMillis() + "=");
        final String code = PluginJSonUtils.getJsonValue(br, "code");
        final String message = PluginJSonUtils.getJsonValue(br, "message");
        if ("015".equals(code) || "Download limit reached".equals(message)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if ("Document access denied".equals(message)) {
            /* TODO: Find errorcode for this */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This document is not downloadable");
        }
        String dllink = br.getRegex("\"url\":\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        // We HAVE to wait here, otherwise we'll get an 0 b file
        sleep(3 * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}