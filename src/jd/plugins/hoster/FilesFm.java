//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.fm" }, urls = { "http://[\\w\\.]*?files\\.fm/(download\\.php\\?id=[0-9]+\\&type=[0-9]|file/[0-9]+/)" }, flags = { 0 })
public class FilesFm extends PluginForHost {

    public FilesFm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://files.fm/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {

        this.setBrowserExclusive();
        br.setCookie("http://www.files.fm", "yab_mylang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("File name:</b></td>.*?<td align=.*?width=[0-9]+px>(.*?)</td>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Click this to report for(.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("File size:</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form captchaForm = br.getFormbyProperty("name", "myform");
        if (!br.containsHTML("http://files.fm/captcha.php") || captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // password handling
        String passCode = null;
        if (br.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            captchaForm.put("downloadpw", passCode);
        }
        String captchaurl = "http://files.fm/captcha.php";
        String code = getCaptchaCode(captchaurl, downloadLink);
        captchaForm.put("captchacode", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("no free download slots available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available");
        if (br.containsHTML("You have got max allowed bandwidth size per hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1001l);
        if (br.containsHTML("Captcha number error") || br.containsHTML("Password Error")) {
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl = BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
