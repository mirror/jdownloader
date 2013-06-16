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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "issuu.com" }, urls = { "http://issuudecrypted\\.com/[a-z0-9\\-_\\.]+/docs/[a-z0-9\\-_]+" }, flags = { 2 })
public class IssuuCom extends PluginForHost {

    public IssuuCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://issuu.com/acceptterms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("issuudecrypted.com/", "issuu.com/"));
    }

    private String DOCUMENTID = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">We can\\'t find what you\\'re looking for")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        DOCUMENTID = br.getRegex("\"documentId\":\"([^<>\"]*?)\"").getMatch(0);
        if (DOCUMENTID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(link.getStringProperty("finalname", null));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered users");
    }

    private static final String MAINPAGE = "http://issuu.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
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
                br.getPage("https://api.issuu.com/query?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&permission=f&loginExpiration=standard&action=issuu.user.login&format=json&jsonCallback=_jqjsp&_" + System.currentTimeMillis() + "=");
                if (br.getCookie(MAINPAGE, "site.model.token") == null || br.containsHTML("\"message\":\"Login failed\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private boolean isValidMailAdress(final String value) {
        return value.matches(".+@.+");
    }

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
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        final String token = br.getCookie(MAINPAGE, "site.model.token");
        br.getPage("http://api.issuu.com/query?documentId=" + this.DOCUMENTID + "&username=" + "ultimaaaate" + "&token=" + Encoding.urlEncode(token) + "&action=issuu.document.download&format=json&jsonCallback=_jqjsp&_" + System.currentTimeMillis() + "=");
        if (br.containsHTML("\"message\":\"Document access denied\"")) throw new PluginException(LinkStatus.ERROR_FATAL, "This document is not downloadable");
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