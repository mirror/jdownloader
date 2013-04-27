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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Browser;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "replik.pl" }, urls = { "http://replik\\.pl/(.*)" }, flags = { 0 })
public class ReplikPl extends PluginForHost {

    private static Object        LOCK                   = new Object();
    private static final String  MAINPAGE               = "http://replik.pl/";
    private static final String  USERTEXT               = "Only downloadable for registered users!";
    private static AtomicInteger maxFree                = new AtomicInteger(2);
    private static AtomicInteger maxRegistered          = new AtomicInteger(2);
    private static AtomicInteger maxChunksForFree       = new AtomicInteger(4);
    private static AtomicInteger maxChunksForRegistered = new AtomicInteger(0);

    public ReplikPl(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/login");
    }

    @Override
    public String getAGBLink() {
        return "http://replik.pl/sp/regulamin.html";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    /* not used for now - only pl */
    // public void prepBrowser() {
    // br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8"); br.setCookie(MAINPAGE, "lang", "english");
    // }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String finalLink = br.getRegex("<input type=\"hidden\" name=\"download_url\" value=\"(.*)\" id=\"download_url\" /></div>").getMatch(0);
        String node = br.getRegex("<input type=\"hidden\" name=\"node_mid\" value=\"(.*)\" id=\"node_mid\" /></div>").getMatch(0);

        if (finalLink == null) {
            logger.warning("finalLink for link: " + link.getDownloadURL() + " is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "finalLink equals null!");
        }

        Browser brClone = br.cloneBrowser();
        brClone.getPage("http://" + link.getHost() + "/gt/" + node);
        final String token = getJson("token", brClone, false);
        if (token == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "download token is null!"); }
        try {
            if (finalLink.contains("http://")) {
                finalLink = finalLink + "?token=" + token;
            } else
                finalLink = "http://" + link.getHost() + finalLink + "?token=" + token;

            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, true, maxChunksForFree.get());
        } catch (Exception e) {
            if (br.getRequest().getHttpConnection().getResponseMessage().equals("Requested Range Not Satisfiable")) {
                logger.warning("The file is unavailable");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is unavailable!", 60 * 60 * 1000L);
            }
        }

        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(br.containsHTML("Not found") || br.containsHTML("404") ? LinkStatus.ERROR_FILE_NOT_FOUND : LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getContentLength() == 0L) {
            logger.warning("The final link seams to be 0 bytes!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link is 0 bytes!", 60 * 60 * 1000L);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        String finalLink = br.getRegex("<input type=\"hidden\" name=\"download_url\" value=\"(.*)\" id=\"download_url\" /></div>").getMatch(0);
        String node = br.getRegex("<input type=\"hidden\" name=\"node_mid\" value=\"(.*)\" id=\"node_mid\" /></div>").getMatch(0);

        if (finalLink == null) {
            logger.warning("finalLink for link: " + link.getDownloadURL() + " is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "finalLink equals null!");
        }

        Browser brClone = br.cloneBrowser();
        brClone.getPage("http://" + link.getHost() + "/gt/" + node);
        final String token = getJson("token", brClone, false);
        if (token == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "download token is null!"); }
        try {
            if (finalLink.contains("http://")) {
                finalLink = finalLink + "?token=" + token;
            } else
                finalLink = "http://" + link.getHost() + finalLink + "?token=" + token;

            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, true, maxChunksForRegistered.get());
        } catch (Exception e) {
            if (br.getRequest().getHttpConnection().getResponseMessage().equals("Requested Range Not Satisfiable")) {
                logger.warning("The file is unavailable");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File is unavailable!", 60 * 60 * 1000L);
            }
        }

        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(br.containsHTML("Not found") || br.containsHTML("404") ? LinkStatus.ERROR_FILE_NOT_FOUND : LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getContentLength() == 0L) {
            logger.warning("The final link seams to be 0 bytes!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link is 0 bytes!", 60 * 60 * 1000L);
        }
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Link offline
        if (br.containsHTML("Element nie może zostać wyświetlony")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML("Brak dostępu do strony")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Brak dostępu do strony!"); }

        final String fileName = br.getRegex("<h1 id=\"file_title\">[ \t\n\r\f]+<span>(.*?)</span>").getMatch(0);
        final String fileSize = br.getRegex("<td>Rozmiar:&nbsp;</td>[ \t\n\r\f]+<td>[ \t\n\r\f]+<strong>[ \t\n\r\f]+(.*?)[ \t\n\r\f]+</strong>[ \t\n\r\f]+</td>").getMatch(0);
        if (fileName == null || fileSize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "filename or filesize not recognized"); }
        link.setName(Encoding.htmlDecode(fileName.trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setAcceptLanguage("pl;q=0.8");
                br.setCookie(MAINPAGE, "lang", "pl");
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
                br.getPage(MAINPAGE + "/login");
                Form login = br.getForm(1);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username_or_email", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                // br.getPage("/");
                if (br.containsHTML("form_errors")) {
                    String errMsg = getJson("password", br, true);
                    if (errMsg != null)
                        logger.warning("Server reports:" + errMsg);
                    else {
                        errMsg = getJson("username", br, true);
                        if (errMsg != null)
                            logger.warning("Server reports:" + errMsg);
                        else
                            logger.warning("Couldn't determine account status!");
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://replik.pl");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                if (e.getErrorMessage() == null) e.setErrorMessage("Premium Account is invalid: it's free or not recognized!");
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus("Login failed");
            String errorMsg = "Please check your Username and Password!";
            UserIO.getInstance().requestMessageDialog(0, "Replik.pl user Error!", errorMsg);
            account.setValid(false);
            return ai;
        }
        br.getPage(MAINPAGE);
        if (br.containsHTML("name=\"logged_username\" value=\"" + account.getUser() + "\"")) {
            ai.setUnlimitedTraffic();
            account.setValid(true);
            ai.setStatus("Registered User");
        } else
            account.setValid(false);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    public int getMaxSimultanRegisteredDownloadNum() {
        return maxRegistered.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public static String getJson(final String parameter, final Browser br2, final boolean loginForm) {
        String result = null;
        if (loginForm)
            result = br2.getRegex(parameter + "\": \"(.*?)\"},").getMatch(0);
        else
            result = br2.getRegex(parameter + "\": \"(.{128})\"}").getMatch(0);
        return result;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}