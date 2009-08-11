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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploading.com" }, urls = { "http://[\\w\\.]*?uploading\\.com/files/\\w+/.+" }, flags = { 2 })
public class UploadingCom extends PluginForHost {
    private static int simultanpremium = 1;

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.uploading.com/premium/");
    }

    // @Override
    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.uploading.com/");
        if (br.containsHTML("UPGRADE TO PREMIUM")) return false;
        if (br.containsHTML("Premium account")) return true;
        return false;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage("http://www.uploading.com/");
        br.getPage("http://www.uploading.com/login/");
        br.postPage("http://www.uploading.com/login/", "log_ref=&login=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://www.uploading.com/", "ulogin") == null || br.getCookie("http://www.uploading.com/", "upass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            account.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        br.getPage("http://www.uploading.com/profile/");
        String validUntil = br.getRegex("Premium Account access is valid until (.*?)\\.").getMatch(0);
        account.setValid(true);
        /* Workaround for buggy expire date */
        if (!validUntil.trim().equalsIgnoreCase("0000-00-00")) {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-MM-dd", null));
        }
        return ai;
    }

    // @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        if (!isPremium()) {
            simultanpremium = 1;
            handleFree0(link);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.getPage(link.getDownloadURL());
        Form form = br.getForm(2);
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br,link, form, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            dl = jd.plugins.BrowserAdapter.openDownload(br,link, form, true, 1);
        }
        dl.startDownload();
    }

    public void handleFree0(DownloadLink link) throws Exception {
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        if (br.containsHTML("Only Premium users can download files larger than")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
        }
        br.setFollowRedirects(false);
        form = br.getFormbyProperty("id", "downloadform");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
//        this.sleep(70000l, link);
        int tt = Integer.parseInt(br.getRegex("<script>.*?var.*?=(\\d+);").getMatch(0));
        sleep(tt * 1001l, link);
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br,link, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage(downloadLink.getDownloadURL());
        br.cloneBrowser().getPage("http://img.uploading.com/css/blue.main.css");
        String quant = br.getRegex("<img src=\"(http://pixel.quantserve.com/pixel/.*?)\"").getMatch(0);
        Browser brc = br.cloneBrowser();
        URLConnectionAdapter con = brc.openGetConnection(quant);
        con.disconnect();
        con = brc.openGetConnection("http://img.uploading.com/bb_bg_big.png");
        con.disconnect();
        Regex info = br.getRegex(Pattern.compile("<img src=\"http://uploading.com/images/ico_big_download_file.gif\" class=\"big_ico\" alt=\"\"/>.*<h2>(.*?)</h2><br/>.*<b>Size:</b>(.*?)<br/><br/>", Pattern.DOTALL));
        String filesize = info.getMatch(1);
        String filename = info.getMatch(0);
        if (filesize == null || filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        if (br.containsHTML("Only Premium users can download files larger than")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
        }
        br.setFollowRedirects(false);
        form = br.getFormbyProperty("id", "downloadform");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
//        this.sleep(100000l, downloadLink);
        int tt = Integer.parseInt(br.getRegex("<script>.*?var.*?=(\\d+);").getMatch(0));
        sleep(tt * 1001l, downloadLink);
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, br.getRedirectLocation(), false, 1);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    // @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    // @Override
    public void resetPluginGlobals() {

    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
