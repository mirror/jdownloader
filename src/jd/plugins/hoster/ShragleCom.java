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
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shragle.com" }, urls = { "http://[\\w\\.]*?shragle\\.(com|de)/files/[\\w]+/.*" }, flags = { 2 })
public class ShragleCom extends PluginForHost {

    static String apikey = "078e5ca290d728fd874121030efb4a0d";

    public ShragleCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.shragle.com/index.php?p=accounts&ref=386");
        this.setStartIntervall(5000l);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.de/", "\\.com/").replace("http://shragle", "http://www.shragle"));
    }

    private String AGENT = RandomUserAgent.generate();

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        this.br.getPage("http://www.shragle.com/api.php?key=" + ShragleCom.apikey + "&action=checkUser&useMD5=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(JDHash.getMD5(account.getPass())));
        final String accountinfos[] = this.br.getRegex("(.*?)\\|(.*?)\\|(.+)").getRow(0);
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
    public String getAGBLink() {
        return "http://www.shragle.com/index.php?cat=about&p=faq";
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.correctDownloadLink(downloadLink);
        this.br.setFollowRedirects(true);
        this.requestFileInformation(downloadLink);
        this.br.setCookie("http://www.shragle.com", "lang", "de_DE");
        if (downloadLink.getDownloadURL().contains("?")) {
            this.br.getPage(downloadLink.getDownloadURL() + "&jd=1");
        } else {
            this.br.getPage(downloadLink.getDownloadURL() + "?jd=1");
        }
        final boolean mayfail = this.br.getRegex("Download-Server ist unter").matches();
        String wait = this.br.getRegex(Pattern.compile("Bitte warten Sie(.*?)Minuten", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait.trim()) * 60 * 1000l); }
        wait = this.br.getRegex("var downloadWait =(.*?);").getMatch(0);
        final Form form = this.br.getFormbyProperty("name", "download");
        if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (wait == null) {
            wait = "10";
        }
        this.sleep(Long.parseLong(wait.trim()) * 1000l, downloadLink);
        form.setAction(form.getAction());
        form.remove("submit");
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, form, true, 1);
        final URLConnectionAdapter con = this.dl.getConnection();
        if ((con.getContentType() != null) && con.getContentType().contains("html")) {
            this.br.followConnection();
            if (this.br.containsHTML("bereits eine Datei herunter")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already loading, please wait!", 10 * 60 * 1000l); }
            if (this.br.containsHTML("The selected file was not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if ((this.br.containsHTML("Die von Ihnen angeforderte Datei") && this.br.containsHTML("Bitte versuchen Sie es")) || mayfail) {
                if (downloadLink.getLinkStatus().getRetryCount() > 2) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.correctDownloadLink(downloadLink);
        this.requestFileInformation(downloadLink);
        this.login(account);
        br.forceDebug(true);
        this.br.setCookie("http://www.shragle.com", "lang", "de_DE");
        this.br.setFollowRedirects(false);
        this.br.getPage(downloadLink.getDownloadURL());
        if ((this.br.getRedirectLocation() != null) && this.br.getRedirectLocation().contains("index.php")) {
            this.br.getPage(this.br.getRedirectLocation());
        }
        if (this.br.getRedirectLocation() != null) {
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, this.br.getRedirectLocation(), true, -4);
        } else {
            final Form form = this.br.getFormbyProperty("name", "download");
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, form, true, 0);
        }
        final URLConnectionAdapter con = this.dl.getConnection();
        if ((con.getContentType() != null) && con.getContentType().contains("html")) {
            this.br.followConnection();
            System.out.println(br);
            if (this.br.containsHTML("bereits eine Datei herunter")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP is already loading, please wait!", 10 * 60 * 1000l); }
            if (this.br.containsHTML("The selected file was not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (this.br.containsHTML("tige Session-ID.")) { throw new PluginException(LinkStatus.ERROR_RETRY);

            }
            if ((this.br.containsHTML("Die von Ihnen angeforderte Datei") && this.br.containsHTML("Bitte versuchen Sie es"))) {
                if (downloadLink.getLinkStatus().getRetryCount() > 2) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 30 * 60 * 1000l);
            }
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public void init() {
        this.br.setRequestIntervalLimit(this.getHost(), 800);
    }

    private void login(final Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", AGENT);
        this.br.setFollowRedirects(true);
        this.br.getPage("http://www.shragle.com/index.php?p=login");
        this.br.postPage("http://www.shragle.com/index.php?p=login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&cookie=1&submit=Login");
        String Cookie = this.br.getCookie("http://www.shragle.com", "userID");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = this.br.getCookie("http://www.shragle.com", "username");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        Cookie = this.br.getCookie("http://www.shragle.com", "password");
        if (Cookie == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        this.br.getPage("http://www.shragle.com/?cat=user");
        if (this.br.containsHTML(">Premium-Upgrade<")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", AGENT);
        final String id = new Regex(downloadLink.getDownloadURL(), "shragle.com/files/(.*?)/").getMatch(0);
        final String[] data = Regex.getLines(this.br.getPage("http://www.shragle.com/api.php?key=" + ShragleCom.apikey + "&action=getStatus&fileID=" + id));
        if (data.length != 4) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String name = data[0];
        final String size = data[1];
        final String md5 = data[2];
        // status 0: all ok 1: abused
        final String status = data[3];
        if (!status.equals("0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setFinalFileName(name.trim());
        downloadLink.setDownloadSize(Long.parseLong(size));
        downloadLink.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
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
}