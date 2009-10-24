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

import jd.PluginWrapper;
import jd.http.Browser;
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
    private static final Object PREMLOCK = new Object();
    private String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0";

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.uploading.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    public boolean isPremium() throws IOException {
        boolean follow = br.isFollowingRedirects();
        br.setFollowRedirects(true);
        br.getPage("http://www.uploading.com/");
        br.setFollowRedirects(follow);
        if (br.containsHTML("UPGRADE TO PREMIUM")) return false;
        if (br.containsHTML("Membership: Premium")) return true;
        return false;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", userAgent);
        br.setCookie("http://www.uploading.com/", "language", "1");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.getPage("http://www.uploading.com/");
        br.postPage("http://uploading.com/general/login_form/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on");
        if (br.getCookie("http://www.uploading.com/", "remembered_user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
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

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        boolean free = false;
        br.setDebug(true);
        synchronized (PREMLOCK) {
            login(account);
            // if (!isPremium()) {
            // simultanpremium = 1;
            // free = true;
            // } else {
            // if (simultanpremium + 1 > 20) {
            // simultanpremium = 20;
            // } else {
            // simultanpremium++;
            // }
            // }
            fileCheck(link);
        }
        if (free) {
            handleFree0(link);
            return;
        }
        String redirect = getDownloadUrl(br, link);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            String error = dl.getConnection().getRequest().getCookies().get("error").getValue();
            br.followConnection();
            if (error != null && error.contains("wait")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 1000l * 15);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    public void handleFree0(DownloadLink link) throws Exception {
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        if (br.containsHTML("you have reached your daily download limi")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
        if (br.containsHTML("Your IP address is currently downloading a file")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("Only Premium users can download files larger than")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
        if (br.containsHTML("You have reached the daily downloads limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
        String redirect = getDownloadUrl(br, link);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            String error = dl.getConnection().getRequest().getCookies().get("error").getValue();
            br.followConnection();
            if (error != null && error.contains("wait")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 1000l * 15);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    public AvailableStatus fileCheck(DownloadLink downloadLink) throws PluginException, IOException {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("but due to abuse or through deletion by")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("File size: <b>(.*?)</b>").getMatch(0);
        String filename = br.getRegex(">Download(.*?)for free on uploading.com").getMatch(0).trim();
        if (filename == null) {
            filename = br.getRegex(">File download</h2><br/>.*?<h2>(.*?)</h2>").getMatch(0);
            if (filename == null) {
                // Last try to get the filename, if this
                String fname = new Regex(downloadLink.getDownloadURL(), "uploading\\.com/files/\\w+/([a-zA-Z0-9 ._]+)").getMatch(0);
                fname = fname.replace(" ", "_");
                if (br.containsHTML(fname)) {
                    filename = fname;
                }

            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", userAgent);
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "language", "1");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        return fileCheck(downloadLink);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        handleFree0(downloadLink);
    }

    public String getDownloadUrl(Browser br, DownloadLink downloadLink) throws PluginException, IOException {
        String varLink = br.getRegex("var file_link = '(http://.*?)'").getMatch(0);
        if (varLink != null) {
            sleep(2000, downloadLink);
            return varLink;
        }
        br.setFollowRedirects(false);
        String fileID = br.getRegex("file_id: (\\d+)").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String starttimer = br.getRegex("start_timer\\((\\d+)\\);").getMatch(0);
        String redirect = null;
        if (starttimer != null) {
            sleep((Long.parseLong(starttimer) + 2) * 1000l, downloadLink);
            br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&action=get_link&pass=");
            redirect = br.getRegex("link\": \"(http.*?)\"").getMatch(0);
            if (redirect != null) {
                redirect = redirect.replaceAll("\\\\/", "/");
            } else {
                if (br.containsHTML("Please wait")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        } else {
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
        }
        if (redirect == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        return redirect;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}
