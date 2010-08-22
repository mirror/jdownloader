//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shragle.com" }, urls = { "http://[\\w\\.]*?shragle\\.(com|de)/files/[\\w]+/.*" }, flags = { 2 })
public class ShragleCom extends PluginForHost {

    static String apikey = "078e5ca290d728fd874121030efb4a0d";

    public ShragleCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.shragle.com/index.php?p=accounts&ref=386");
        setStartIntervall(5000l);

    }

    @Override
    public void init() {
        br.setRequestIntervalLimit(this.getHost(), 800);
    }

    @Override
    public String getAGBLink() {
        return "http://www.shragle.com/index.php?cat=about&p=faq";
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.shragle.com/index.php?p=login");
        br.postPage("http://www.shragle.com/index.php?p=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&cookie=1&submit=Login");
        String Cookie = br.getCookie("http://www.shragle.com", "userID");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = br.getCookie("http://www.shragle.com", "username");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = br.getCookie("http://www.shragle.com", "password");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        br.getPage("http://www.shragle.com/?cat=user");
        if (br.containsHTML(">Premium-Upgrade<")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        br.getPage("http://www.shragle.com/api.php?key=" + apikey + "&action=checkUser&useMD5=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(JDHash.getMD5(account.getPass())));
        String accountinfos[] = br.getRegex("(.*?)\\|(.*?)\\|(.+)").getRow(0);
        if (accountinfos == null) {
            account.setValid(false);
            return ai;
        }
        ai.setPremiumPoints(Long.parseLong(accountinfos[2].trim()));
        if (accountinfos[0].trim().equalsIgnoreCase("1")) {
            account.setValid(false);
            ai.setStatus("No Premium Account");
        } else if (accountinfos[0].trim().equalsIgnoreCase("2")) {
            account.setValid(true);
        }
        ai.setValidUntil(Long.parseLong(accountinfos[1]) * 1000l);
        return ai;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.de/", "\\.com/"));
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.setCookie("http://www.shragle.com", "lang", "de_DE");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, -4);
        } else {
            Form form = br.getFormbyProperty("name", "download");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 0);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType() != null && con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("bereits eine Datei herunter")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already loading, please wait!", 10 * 60 * 1000l);
            if (br.containsHTML("The selected file was not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if ((br.containsHTML("Die von Ihnen angeforderte Datei") && br.containsHTML("Bitte versuchen Sie es"))) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        String id = new Regex(downloadLink.getDownloadURL(), "shragle.com/files/(.*?)/").getMatch(0);
        String[] data = Regex.getLines(br.getPage("http://www.shragle.com/api.php?key=" + apikey + "&action=getStatus&fileID=" + id));
        if (data.length != 4) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = data[0];
        String size = data[1];
        String md5 = data[2];
        // status 0: all ok 1: abused
        String status = data[3];
        if (!status.equals("0")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(name.trim());
        downloadLink.setDownloadSize(Long.parseLong(size));
        downloadLink.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setDebug(true);
        br.setCookie("http://www.shragle.com", "lang", "de_DE");
        if (downloadLink.getDownloadURL().contains("?")) {
            br.getPage(downloadLink.getDownloadURL() + "&jd=1");
        } else {
            br.getPage(downloadLink.getDownloadURL() + "?jd=1");
        }
        br.setDebug(true);
        boolean mayfail = br.getRegex("Download-Server ist unter").matches();
        String wait = br.getRegex(Pattern.compile("Bitte warten Sie(.*?)Minuten", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l); }
        wait = br.getRegex("var downloadWait =(.*?);").getMatch(0);
        Form form = br.getFormbyProperty("name", "download");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (wait == null) wait = "10";
        sleep(Long.parseLong(wait.trim()) * 1000l, downloadLink);
        br.setFollowRedirects(true);

        form.setAction(form.getAction() + "?jd=1");

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType() != null && con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("bereits eine Datei herunter")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already loading, please wait!", 10 * 60 * 1000l);
            if (br.containsHTML("The selected file was not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if ((br.containsHTML("Die von Ihnen angeforderte Datei") && br.containsHTML("Bitte versuchen Sie es")) || mayfail) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 1000;
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
