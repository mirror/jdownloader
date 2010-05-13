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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.com" }, urls = { "http://[\\w\\.]*?megashare\\.com/[0-9]+" }, flags = { 2 })
public class MegaShareCom extends PluginForHost {

    public MegaShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        this.enablePremium("http://www.megashare.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.megashare.com/tos.php";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.megashare.com/login.php");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.put("loginid", Encoding.urlEncode(account.getUser()));
        form.put("passwd", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML("You are logged in as a PREMIUM Member") || br.containsHTML("Invalid Username or Password") || br.getCookie("http://www.megashare.com", "username") == null || br.getCookie("http://www.megashare.com", "username") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        Form premform = br.getForm(0);
        if (premform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        premform.setPreferredSubmit("PREMIUM");
        br.submitForm(premform);
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form form = br.getFormbyProperty("name", "downloader");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String accel = br.getRegex("name=\"(accel.*?)\"").getMatch(0);
        if (accel == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form dlForm = new Form();
        dlForm.put("yesss", "Download");
        dlForm.put("id", form.getVarsMap().get("id"));
        dlForm.put("time_diff", form.getVarsMap().get("time_diff"));
        dlForm.put("req_auth", form.getVarsMap().get("req_auth"));
        dlForm.setAction(downloadLink.getDownloadURL());
        dlForm.setMethod(Form.MethodType.POST);
        String passCode = null;
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            dlForm.put("auth_nm", passCode);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlForm, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
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
        String fileId = new Regex(downloadLink.getDownloadURL(), "megashare.com/(\\d+)").getMatch(0);
        if (!br.containsHTML("free premium") && !br.containsHTML("FREE") || fileId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String preferThat = "free+premium";
        String preferThat2 = "FreePremDz";
        if (!br.containsHTML("free premium")) {
            preferThat = "FREE";
            preferThat2 = "FreeDz" + fileId;
        }
        String post = preferThat2 + ".x=" + new Random().nextInt(10) + "&" + preferThat2 + ".y=" + new Random().nextInt(10) + "&" + preferThat2 + "=" + preferThat;
        br.postPage(downloadLink.getDownloadURL(), post);
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getFormbyProperty("name", "downloader");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        File captchaFile = getLocalCaptchaFile();
        int i = 15;
        while (i-- > 0) {
            try {
                String captchaimg = br.getRegex("id=\"cimg\" src=\"(.*?)\"").getMatch(0);
                if (captchaimg == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                captchaimg = "http://megashare.com/" + captchaimg;
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaimg));
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

            String hash = JDHash.getMD5(captchaFile);
            // Seems to be a captchaerror (captcahs without any letters)
            if (hash.equals("eb92a5ddf69784ee2de24bca0c6299d4")) {
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
        String accel = br.getRegex("name=\"(accel.*?)\"").getMatch(0);
        if (accel == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.remove(accel);
        form.remove(accel);
        form.remove(accel);
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        String passCode = null;
        if (br.containsHTML("This file is password protected.")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("auth_nm", passCode);
        }
        form.put("captcha_code", captchaCode);
        form.put("yesss", "Download");
        form.put("yesss.x", (new Random().nextInt(10) + ""));
        form.put("yesss.y", (new Random().nextInt(10) + ""));
        br.setFollowRedirects(false);
        br.submitForm(form);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            if (br.containsHTML("get premium access")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            System.out.print(br.toString());
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
