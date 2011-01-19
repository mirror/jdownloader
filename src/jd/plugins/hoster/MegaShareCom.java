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

import java.io.File;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.com" }, urls = { "http://[\\w\\.]*?megashare\\.com/[0-9]+" }, flags = { 2 })
public class MegaShareCom extends PluginForHost {

    public MegaShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        this.enablePremium("http://www.megashare.com/premium.php");
    }

    private static final String FILEIDREGEX      = "megashare.com/(\\d+)";
    private static final String TIMEDIFFVARREGEX = "name=\"time_diff\" value=\"(\\d+)\"";
    private static final String PRVALREGEX       = "name=\"\\d+prVal\" value=\"(\\d+)\"";

    @Override
    public String getAGBLink() {
        return "http://www.megashare.com/tos.php";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String post = "loginid=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&yes=submit";
        br.postPage("http://www.megashare.com/login.php", post);
        br.setFollowRedirects(false);
        if (br.getCookie("http://www.megashare.com", "username") == null || br.getCookie("http://www.megashare.com", "password") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        br.postPage(br.getURL(), "PremDz.x=" + new Random().nextInt(10) + "&PremDz.y=" + new Random().nextInt(10) + "&PremDz=PREMIUM");
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form form = br.getFormbyProperty("name", "downloader");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String id = form.getVarsMap().get("id");
        String timeDiff = form.getVarsMap().get("time_diff");
        if (id == null || timeDiff == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String post = "yesss.x=" + new Random().nextInt(10) + "&yesss.y=" + new Random().nextInt(10) + "&yesss=Download&id=" + id + "&time_diff=" + timeDiff + "&req_auth=n";
        String passCode = null;
        // This password handling is probably broken
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            post += "&auth_nm=" + passCode;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getURL(), post, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Invalid Captcha Value")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("This file is password protected.")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex specialstuff = br.getRegex("name=\"(\\d+prVal)\" value=\"(\\d+)\">");
        String fileId = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        String freeDz = br.getRegex("\"(FreeDz\\d+)\"").getMatch(0);
        if (!br.containsHTML("free premium") && !br.containsHTML("FREE") || fileId == null || specialstuff.getMatch(0) == null || specialstuff.getMatch(1) == null || freeDz == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String preferThat = "free+premium";
        String preferThat2 = "FreePremDz";
        if (!br.containsHTML("free premium")) {
            preferThat = "FREE";
            preferThat2 = freeDz;
        }
        String post = specialstuff.getMatch(0) + "=" + specialstuff.getMatch(1) + "&" + preferThat2 + ".x=" + new Random().nextInt(10) + "&" + preferThat2 + ".y=" + new Random().nextInt(10) + "&" + preferThat2 + "=" + preferThat;
        br.postPage(downloadLink.getDownloadURL(), post);
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("addthis_open\\(this, \\'\\', \\'http://(www\\.)?MegaShare\\.com\\d+\\', \\'(.*?)\\'\\)").getMatch(1);
        if (filename != null) downloadLink.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String reconnectWaittime = br.getRegex("var c = (\\d+);").getMatch(0);
        if (reconnectWaittime != null) {
            if (Integer.parseInt(reconnectWaittime) > 320) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWaittime) * 1001l);
        }
        String timeDiffVar = br.getRegex(TIMEDIFFVARREGEX).getMatch(0);
        String fileId = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        String prVal = br.getRegex(PRVALREGEX).getMatch(0);
        if (timeDiffVar == null || fileId == null || prVal == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String postData = "wComp=1&" + fileId + "prVal=" + prVal + "&id=" + fileId + "&time_diff=" + timeDiffVar + "&req_auth=n";
        int wait = 10;
        if (reconnectWaittime != null) wait = Integer.parseInt(reconnectWaittime);
        sleep(wait * 1001l, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), postData);
        File captchaFile = getLocalCaptchaFile();
        int i = 15;
        while (i-- > 0) {
            if (!br.containsHTML("security\\.php\\?i=")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaimg = br.getRegex("id=\"cimg\" src=\"(.*?)\"").getMatch(0);
            if (captchaimg == null) captchaimg = br.getRegex("\"(security\\.php\\?i=\\d+)\"").getMatch(0);
            if (captchaimg == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaimg = "http://www.megashare.com/" + captchaimg;
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaimg));
            String hash = JDHash.getMD5(captchaFile);
            // Seems to be a captchaerror (captcahs without any letters)
            if (hash.equals("eb92a5ddf69784ee2de24bca0c6299d4") || hash.equals("d054cfcd69daca6fe8b8d84f3ece9be3")) {
                continue;
            } else {
                break;
            }
        }
        String captchaCode = null;
        for (int o = 0; o <= 3; o++) {
            captchaCode = getCaptchaCode(captchaFile, downloadLink);
            if (captchaCode.length() == 5) break;
        }
        if (captchaCode.length() != 5) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String userVal = br.getRegex("name=\"user_val\" value=\"(\\d+)\"").getMatch(0);
        prVal = br.getRegex(PRVALREGEX).getMatch(0);
        timeDiffVar = br.getRegex(TIMEDIFFVARREGEX).getMatch(0);
        if (userVal == null || timeDiffVar == null || prVal == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        postData = "wComp=&" + fileId + "prVal=" + prVal + "&captcha_code=" + captchaCode + "&email=&yesss.x=" + String.valueOf(new Random().nextInt(100)) + "&yesss.y=" + String.valueOf(new Random().nextInt(100)) + "&yesss=Download&user_val=" + userVal + "&id=" + fileId + "&time_diff=" + timeDiffVar + "&req_auth=n";
        String passCode = null;
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            postData += "&auth_nm=" + passCode;
        }
        br.setFollowRedirects(true);
        // Unlimited chunks are possible but cause servererrors
        // ("DOWNLOAD_IMCOMPLETE")
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL().toLowerCase(), postData, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid Captcha Value")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("This file is password protected.")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("get premium access")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
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
