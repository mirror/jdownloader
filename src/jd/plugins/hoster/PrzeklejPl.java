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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "przeklej.pl" }, urls = { "http://[\\w\\.]*?przeklej\\.pl/(d/\\w+/|\\d+|plik/)[^\\s]+" }, flags = { 2 })
public class PrzeklejPl extends PluginForHost {

    private static final String PATTERN_PASSWORD_WRONG = "B.*?dnie podane has";

    public PrzeklejPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.przeklej.pl/wybierz-platnosci");
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://przeklej.pl/regulamin";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("_", "-"));
    }

    private static final String REGISTEREDONLY = "> możesz pobrać, jeżeli jesteś zalogowany";
    private static final String NOFREEMESSAGE  = "Only downloadable for registered users";
    private static final String FINALLINKREGEX = "class=\"download\" href=\"(.*?)\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h1 style=\"font-size: 40px;\">Podana strona nie istnieje</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(REGISTEREDONLY)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.PrzeklejPl.errors.nofreedownloadlink", NOFREEMESSAGE));
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<title>przeklej.pl -(.*?)\\. Wrzucaj", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (filename == null) {
            Encoding.htmlDecode(br.getRegex(Pattern.compile("title=\"Pobierz plik\">(.*?)</a>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        }
        String filesize = br.getRegex(Pattern.compile("class=\"size\" style=\"font-weight: normal;\">[\t\n\r ]+\\((.*?)\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(Pattern.compile("You are trying to download a <strong>(.*?)</strong> file", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        String passCode = null;
        boolean resumable = true;
        int maxchunks = 0;
        if (!br.containsHTML("<span class=\"unbold\">Wprowad")) {
            String linkurl = br.getRegex(FINALLINKREGEX).getMatch(0);
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            linkurl = "http://www.przeklej.pl" + linkurl;
            br.getPage(linkurl);
            linkurl = br.getRedirectLocation();
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, resumable, maxchunks);
            handleErrors();
            dl.startDownload();
        } else {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password", downloadLink);
            } else {
                passCode = downloadLink.getStringProperty("pass", null);
            }
            Form form = br.getFormbyProperty("name", "haselko");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            form.put(Encoding.urlEncode("haslo[haslo]"), passCode);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = br.openFormConnection(form);
            if (!con.isContentDisposition()) {
                br.followConnection();
                if (br.containsHTML(PATTERN_PASSWORD_WRONG)) {
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                con.disconnect();
                downloadLink.setProperty("pass", passCode);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, resumable, maxchunks);
                handleErrors();
                dl.startDownload();
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(REGISTEREDONLY)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.PrzeklejPl.errors.nofreedownloadlink", NOFREEMESSAGE));
        doFree(downloadLink);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getHeaders().put("Referer", "");
        br.setCustomCharset("utf-8");
        br.postPage("http://www.przeklej.pl/loguj", "login%5Blogin%5D=" + account.getUser() + "&login%5Bpass%5D=" + account.getPass());
        if (!br.containsHTML("<html><head><meta http\\-equiv=\"refresh\" content=\"0;url=http://www\\.przeklej\\.pl/\"/></head></html>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Registered (free) User");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<span>Wielkość pliku przekracza Twój dostępny <strong>abonament</strong>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.PrzeklejPl.errors.AccountDisabled", "Account ran out of traffic"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private void handleErrors() throws Exception {
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Podana strona nie istnieje")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
