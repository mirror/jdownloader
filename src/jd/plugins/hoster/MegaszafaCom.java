//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megaszafa.com" }, urls = { "http://(www\\.)*?(en\\.)*?megaszafa\\.com/(plik,(\\d+?)\\.html|[0-9A-Za-z]+/.*)" }, flags = { 2 })
public class MegaszafaCom extends PluginForHost {

    public MegaszafaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.megaszafa.com/zarejestruj.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.megaszafa.com/strona,regulamin-serwisu,2.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());

        String browserUrl = link.getBrowserUrl();
        String fileId = "";
        if (browserUrl.contains("plik,"))
            fileId = new Regex(browserUrl, "plik,(\\d+?)\\.html").getMatch(0);
        else {

            fileId = new Regex(browserUrl, "megaszafa\\.com/([0-9A-Za-z]+)/").getMatch(0);
            fileId = new Regex(fileId, "(\\d+)").getMatch(0);
        }

        if (fileId == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "File Id is incorrect!"); }

        String fileSize = br.getRegex("<div class=\"size\" id=\"" + fileId + "Size\">(.*?)</div>").getMatch(0);
        if (fileSize == null) fileSize = br.getRegex("<tr><td>Rozmiar:</td><td id=\"" + fileId + "Size\"><b>(.*?)</b></td></tr>").getMatch(0);

        br.postPage(MAINPAGE + "plikInformacje.html", "id=" + fileId);
        String fileStatus = getJson("status", br);
        if (!fileStatus.equals("1")) { throw new PluginException(LinkStatus.ERROR_FATAL, "File is unavailable!"); }
        String fileErros = getJson("errors", br);
        String fileDetails = getJson("extra", br);
        String fileName = new Regex(fileDetails, "<div class=(.*?)name(.*?)>(.*?)<(.*?)div>").getMatch(2);

        if (fileName == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "FileName not found!"); }
        link.setName(Encoding.htmlDecode(fileName.trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        link.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
        link.setProperty("fileId", fileId);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Free (unregistered) downloads are not supported!");
        // requestFileInformation(downloadLink);
        // doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String fileId = downloadLink.getProperty("fileId").toString();
        br.postPage(MAINPAGE + "plikInformacje.html", "id=" + fileId);

        br.postPage(MAINPAGE + "pobierzPlik,info.html", "id=" + fileId);
        String fileStatus = getJson("status", br);
        if (!fileStatus.equals("1")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "File Status is <> 1!"); }
        String fileErros = getJson("errors", br);
        String fileDetails = getJson("extra", br);

        br.getPage(MAINPAGE + "pobierzPlik," + fileId + ",wybierz.html");

        br.postPage(MAINPAGE + "pobierzPlik,info.html", "id=" + fileId);

        br.setFollowRedirects(false);

        String dllink = MAINPAGE + "pobierzPlik," + fileId + ".html";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.info("Megaszafa error: " + dl.getConnection().getContentType());
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://www.megaszafa.com/";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);

                if (force) {
                    br.setFollowRedirects(true);
                    br.postPage(MAINPAGE + "zaloguj.html", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=false");
                    if (br.containsHTML("\"error\":")) {
                        String error = getJson("error", br);
                        if (error != null) {
                            account.setProperty("cookies", Property.NULL);
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, error);
                        }
                    }
                }

                // Load cookies
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
                    }
                    return;
                }
                br.setFollowRedirects(false);
                String profileUrl = br.getRegex("\"url\":\".*?/(profil,.*?\\.html)\"").getMatch(0);

                br.getPage(MAINPAGE + profileUrl);
                if (br.containsHTML("Taki profil nie istnieje\\.")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nMegaszafa.com: Invalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE); }
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
            ai.setStatus("Login failed or not Premium");
            UserIO.getInstance().requestMessageDialog(0, "Megaszafa.com Login Error", "Login failed!\r\nPlease check your Username and Password!");
            account.setValid(false);
            return ai;
        }
        String dailyLimitLeft = br.getRegex("<div class=\"profileTransfer\">[\t\n\r ]+Transfer:[\t ]+([ 0-9\\.A-Za-z]+)[\t\n\r ]+</div>").getMatch(0);
        if (dailyLimitLeft != null) {
            ai.setTrafficMax(SizeFormatter.getSize("20 GB"));
            ai.setTrafficLeft(SizeFormatter.getSize(dailyLimitLeft));
        } else
            ai.setUnlimitedTraffic();

        account.setValid(true);
        ai.setStatus("Registered user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        doFree(link);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    public static String getJson(final String parameter, final Browser br2) {
        String result = br2.getRegex("\"" + parameter + "\":(.*?)[,|\\}]").getMatch(0);
        return result;
    }
}