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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opendrive.com" }, urls = { "https?://(www\\.)?([a-z0-9]+\\.)?opendrive\\.com/files\\?[A-Za-z0-9\\-_]+" })
public class OpenDriveCom extends PluginForHost {
    public OpenDriveCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.opendrive.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.opendrive.com/terms";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Avoid https because of old 0.9.581 Stable & this host does not force https (maybe only for login) */
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    private static final boolean use_api = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename, filesize;
        if (use_api) {
            logger.info("Using API");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* Call which is used inside folders to embed information of single filelinks */
            br.getPage("http://www.opendrive.com/ajax/file-info/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0));
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            if (!br.containsHTML("\"FileId\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getJson("Name");
            filesize = getJson("SizeOriginal");
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setDownloadSize(Long.parseLong(filesize));
        } else {
            logger.info("NOT using API");
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">File not found<|>or access limited<|List file info failed|File was not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<h1 class=\"filename\">([^<>\"]*?)  \\((\\d+(\\.\\d+)? [A-Za-z]+)\\)</h1>");
            filename = fInfo.getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<i class=\"fa fa-info\"></i>\\s*<h3>(.*?)</h3>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<div class=\"title bottom_border\"><span>([^<>\"]*?)</span>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<title>OpenDrive \\- ([^<>\"]*?)b</title>").getMatch(0);
                    }
                }
            }
            filesize = fInfo.getMatch(1);
            if (filesize == null) {
                filesize = br.getRegex("Size\\s*:\\s*<b>(.*?)</b>").getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("class=\"file_info size fl\"><b>Size:</b><span>([^<>\"]*?)</span></div>").getMatch(0);
                }
            }
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("File is private and cannot be downloaded\">Download</a>")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "File is private and cannot be downloaded");
        }
        String dllink;
        if (use_api) {
            dllink = getJson("DirectLink");
        } else {
            dllink = br.getRegex("\"(https?://(www\\.)?([a-z0-9]+\\.)?opendrive\\.com/api/v\\d+/download/file\\.json/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<a class=\"[^\"]*download\" href=\"(http[^<>\"]*?|/download/[^\"]+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(https?://(www\\.)?([a-z0-9]+\\.)?opendrive\\.com/files/[A-Za-z0-9\\-_]+/[^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if ("limit_exceeded.jpg".equals(getFileNameFromHeader(dl.getConnection()))) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Limit exeeded");
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://opendrive.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                this.br.setFollowRedirects(true);
                this.br.getPage("https://www.opendrive.com/login");
                Form loginform = this.br.getFormbyProperty("id", "login-form");
                if (loginform == null) {
                    loginform = this.br.getForm(0);
                }
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("login_username", account.getUser());
                loginform.put("login_password", account.getPass());
                loginform.remove("remember_me");
                loginform.put("remember_me", "on");
                for (int i = 0; i < 3; i++) { // Sometimes retry is needed, redirected to /login?ref=%2Ffiles&s=...
                    br.submitForm(loginform);
                    if (br.containsHTML(">Invalid  username or password<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (br.containsHTML("\"user-controls-menu\"")) {
                        break;
                    }
                    Thread.sleep(3 * 1000);
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
        br.getPage("/settings");
        if (br.containsHTML("<b>Personal account</b><br>[\t\n\r ]*?Basic")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        account.setValid(true);
        ai.setStatus("Premium account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final String dllink = br.getRegex("<a class=\"download\" href=\"(https://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        return getJson(this.br.toString(), parameter);
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
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