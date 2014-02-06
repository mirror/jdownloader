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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadizer.net" }, urls = { "http://(www\\.)?uploadizer\\.net/\\?d=[A-Z0-9]+" }, flags = { 2 })
public class UploadizerNet extends PluginForHost {

    public UploadizerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/service.php");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST     = "http://uploadizer.net";
    private static final int    DEFAULTWAITTIME = 0;

    // MhfScriptBasic 2.0
    // FREE limits: Chunks * Maxdls
    // PREMIUM limits: Chunks * Maxdls
    // Captchatype: null, mhfstandard, reCaptcha
    // Other notes:
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(en|ru|fr|es|de)/file/", "file/"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        // Offline links should also have recognizable filenames
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                // Set name and remove hosters tags
                downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)).replace("[UZ]", ""));
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();

            if (br.containsHTML(">AccessKey is expired, please request")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL server error, waittime skipped?");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getData(final String data) {
        String result = br.getRegex(">" + data + "</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"]*?)</li>").getMatch(0);
        if (result == null) result = br.getRegex("<b>" + data + "</b></td>[\t\n\r ]+<td align=left( width=\\d+px)?>([^<>\"]*?)</td>").getMatch(1);
        return result;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
                br.setCookie(COOKIE_HOST, "yab_mylang", "en");
                br.getPage(COOKIE_HOST + "/login.php");
                final String lang = System.getProperty("user.language");
                Form form = br.getFormbyProperty("name", "lOGIN");
                if (form == null) form = br.getForm(0);
                if (form == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                form.put("user", Encoding.urlEncode(account.getUser()));
                form.put("pass", Encoding.urlEncode(account.getPass()));
                // If the referer is still in the form (and if it is a valid
                // downloadlink) the download starts directly after logging in
                // so we
                // MUST remove it!
                form.remove("refer_url");
                form.put("autologin", "0");
                br.submitForm(form);
                br.getPage(COOKIE_HOST + "/members.php");
                final String premium = br.getRegex("return overlay\\(this, \\'package_details\\',\\'width=\\d+px,height=\\d+px,center=1,resize=1,scrolling=1\\'\\)\">(Premium)</a>").getMatch(0);
                if (br.getCookie(COOKIE_HOST, "mfh_passhash") == null || "0".equals(br.getCookie(COOKIE_HOST, "mfh_uid"))) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (premium == null) {
                    account.setProperty("freeacc", true);
                } else {
                    account.setProperty("freeacc", false);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
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
            return ai;
        }
        br.getPage(COOKIE_HOST + "/members.php");
        String expired = getData("Expired\\?");
        if (expired != null) {
            expired = expired.trim();
            if (expired.equalsIgnoreCase("No"))
                ai.setExpired(false);
            else if (expired.equalsIgnoreCase("Yes")) ai.setExpired(true);
        }
        String expires = getData("Package Expire Date");
        if (expires != null) {
            expires = expires.trim();
            if (!expires.equals("Never")) {
                String[] e = expires.split("/");
                Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[0]) - 1, Integer.parseInt(e[1]));
                ai.setValidUntil(cal.getTimeInMillis());
            }
        }
        final String create = getData("Register Date");
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[0]) - 1, Integer.parseInt(c[1]));
            ai.setCreateTime(cal.getTimeInMillis());
        }
        final String files = getData("Hosted Files");
        if (files != null) {
            ai.setFilesNum(Integer.parseInt(files.trim()));
        }
        if (account.getBooleanProperty("freeacc")) {
            ai.setStatus("Registered (free) User");
        } else {
            ai.setStatus("Premium User");
        }
        account.setValid(true);

        return ai;
    }

    public void handlePremium(final DownloadLink parameter, final Account account) throws Exception {
        requestFileInformation(parameter);
        login(account, false);
        br.setFollowRedirects(false);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        if (account.getBooleanProperty("freeacc")) {
            doFree(parameter);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, parameter.getDownloadURL(), true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
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

}