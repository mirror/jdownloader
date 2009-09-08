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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploading.com" }, urls = { "http://[\\w\\.]*?uploading\\.com/files/\\w+/.+" }, flags = { 2 })
public class UploadingCom extends PluginForHost {
    private static int simultanpremium = 1;

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.uploading.com/premium/");
    }

    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    public boolean isPremium() throws IOException {
        br.getPage("http://www.uploading.com/");
        if (br.containsHTML("UPGRADE TO PREMIUM")) return false;
        if (br.containsHTML("Membership: Premium")) return true;
        return false;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage("http://www.uploading.com/");
        br.postPage("http://uploading.com/general/login_form/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on");
        if (br.getCookie("http://www.uploading.com/", "remembered_user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
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
        account.setValid(true);
        ai.setValidUntil(br.getCookies("http://www.uploading.com/").get("remembered_user").getExpireDate());
        return ai;
    }

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
        String fileID = br.getRegex("file_id: (\\d+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String redirect = null;
        for (int i = 0; i < 5; i++) {
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_1");
            String wait = br.getRegex("\"answer\": \"(\\d+)\"").getMatch(0);
            if (wait != null) {
                sleep(1000l * Long.parseLong(wait.trim()), link);
            } else {
                sleep(1000l, link);
            }
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_2");
            redirect = br.getRegex("redirect\": \"(http.*?)\"").getMatch(0);
            if (redirect != null) {
                redirect = redirect.replaceAll("\\\\/", "/");
                break;
            }
            sleep(1000l, link);
        }
        if (redirect == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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
        if (br.containsHTML("Only Premium users can download files larger than")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium"); }
        br.setFollowRedirects(false);
        String fileID = br.getRegex("file_id: (\\d+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String redirect = null;
        for (int i = 0; i < 5; i++) {
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_1");
            String wait = br.getRegex("\"answer\": \"(\\d+)\"").getMatch(0);
            if (wait != null) {
                sleep(1000l * Long.parseLong(wait.trim()), link);
            } else {
                sleep(1000l, link);
            }
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_2");
            redirect = br.getRegex("redirect\": \"(http.*?)\"").getMatch(0);
            if (redirect != null) {
                redirect = redirect.replaceAll("\\\\/", "/");
                break;
            }
            sleep(1000l, link);
        }
        if (redirect == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, false, 1);
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage(downloadLink.getDownloadURL());
        Regex info = br.getRegex(Pattern.compile("ico_big_download_file.gif\" class=\"big_ico\" alt=\"\"/>.*<h2>(.*?)</h2><br/>.*<b>Size:</b>(.*?)<br/><br/>", Pattern.DOTALL));
        String filesize = info.getMatch(1);
        String filename = info.getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (br.containsHTML("You have reached the daily downloads limit")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l); }
        if (br.containsHTML("Only Premium users can download files larger than")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium"); }
        br.setFollowRedirects(false);
        String fileID = br.getRegex("file_id: (\\d+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String redirect = null;
        for (int i = 0; i < 5; i++) {
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_1");
            String wait = br.getRegex("\"answer\": \"(\\d+)\"").getMatch(0);
            if (wait != null) {
                sleep(1000l * Long.parseLong(wait.trim()), downloadLink);
            } else {
                sleep(1000l, downloadLink);
            }
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=step_2");
            redirect = br.getRegex("redirect\": \"(http.*?)\"").getMatch(0);
            if (redirect != null) {
                redirect = redirect.replaceAll("\\\\/", "/");
                break;
            }
            sleep(1000l, downloadLink);
        }
        if (redirect == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, redirect, false, 1);
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public int getTimegapBetweenConnections() {
        return 100;
    }

    public void reset() {
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void resetPluginGlobals() {

    }

    public void resetDownloadlink(DownloadLink link) {

    }

}
