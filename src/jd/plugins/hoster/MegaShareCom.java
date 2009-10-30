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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.com" }, urls = { "http://[\\w\\.]*?megashare\\.com/[0-9]+" }, flags = { 0 })
public class MegaShareCom extends PluginForHost {

    public MegaShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.megashare.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form submit = br.getForm(0);
        submit.setPreferredSubmit(2);
        br.submitForm(submit);
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Optional handling if they change the page

        // Form freedl = new Form();
        // freedl.setMethod(Form.MethodType.POST);
        // // form.setAction(downloadLink.getDownloadURL());
        // if (br.containsHTML("FreePremDz")) {
        // freedl.put("FreePremDz", "free premium");
        // } else {
        // String FREEdz = br.getRegex("name=\"(FreeDz.*?)\"").getMatch(0);
        // if (FREEdz == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // freedl.put(FREEdz, "FREE");
        // }
        // br.submitForm(freedl);
        // if (!br.containsHTML("security\\.php")) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form form = br.getFormbyProperty("name", "downloader");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Waittime handling could be useful if they check if the users are
        // really waiting
        // if (form == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // String wait = br.getRegex("var c = (\\d+);").getMatch(0);
        // if (wait != null) {
        // int tt = Integer.parseInt(wait);
        // sleep(tt * 1001l, downloadLink);
        // }
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, -3);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
