//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plunder.com" }, urls = { "http://[\\w\\.]*?(youdownload\\.eu|binarybooty\\.com|mashupscene\\.com|plunder\\.com|files\\.youdownload\\.com)/((-download-[a-z0-9]+|.+\\-download\\-.+)\\.htm|(?!/)[0-9a-z]+)" }, flags = { 2 })
public class PlunderCom extends PluginForHost {

    /**
     * @author pspzockerscene
     */
    public PlunderCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.plunder.com/Register/");
    }

    private static final String NOCHUNKS = "NOCHUNKS";

    @Override
    public String getAGBLink() {
        return "http://www.plunder.com/x/tos";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        // Sometimes the links are outdated but they show the new links, the
        // problem is they they new and newer links could also be moved so this
        // is why we need the following part!
        for (int i = 0; i <= 5; i++) {
            String objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
            if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
            if (objectMoved != null) {
                objectMoved = Encoding.htmlDecode(objectMoved);
                if (!objectMoved.contains("http")) objectMoved = "http://www.plunder.com" + objectMoved;
                br.getPage(objectMoved);
                objectMoved = br.getRegex("<h2>Object moved to <a href=\"(.*?)\">here</a>").getMatch(0);
                if (objectMoved == null) objectMoved = br.getRegex("This document may be found <a HREF=\"(.*?)\"").getMatch(0);
                if (objectMoved != null) continue;
                downloadLink.setUrlDownload(br.getURL());
                break;
            } else {
                break;
            }
        }
        br.setFollowRedirects(true);
        if (br.getURL().contains("/search/?f=")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1>([^<>\"]*?) Download</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) download[\t\n\r ]+</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL() + "?showlink=1");
        String dllink = br.getRegex("/h2><a href=(\"|')?(http://[^<>\"']+)").getMatch(1);
        if (dllink == null) dllink = br.getRegex("(\"|')?(http://[a-z0-9]+\\.plunder\\.com/x/[^<>\"']+)").getMatch(1);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            String checklink = br.getURL();
            if (br.containsHTML("Too many downloads from this IP") || checklink.contains("/blocked")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads!", 10 * 60 * 1000l);
            if (br.containsHTML("You must log in to download more this session") || checklink.contains("/login/") || checklink.contains("/register/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Register or perform a reconnect to download more!", 10 * 60 * 1001l);
            if (checklink.contains("/error")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://plunder.com";
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://www.plunder.com/login/");
                final String lang = System.getProperty("user.language");
                final Form loginform = br.getForm(0);
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("Username", account.getUser());
                loginform.put("Password", account.getPass());
                br.submitForm(loginform);
                if (br.getCookie(MAINPAGE, "loginx") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("\\'(http[^<>\"]*?)\\'>Download Now</a>").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = 1;
        if (link.getBooleanProperty(PlunderCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, maxChunks);
        } catch (final UnknownHostException uhe) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(PlunderCom.NOCHUNKS, false) == false) {
                    link.setProperty(PlunderCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(PlunderCom.NOCHUNKS, false) == false) {
                link.setProperty(PlunderCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}