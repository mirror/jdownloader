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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datenklo.net" }, urls = { "http://[\\w\\.]*?datenklo\\.net/dl-[a-zA-Z0-9]{5}" }, flags = { 0 })
public class DatenKloNet extends PluginForHost {

    public DatenKloNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.datenklo.net/agb";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("(Du bist keinem g&uuml;ltigen Download-Link gefolgt|Vielleicht wurde der Eintrag gel&ouml;scht oder du hast dich vertippt)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h4>(.*?)</h4>").getMatch(0);
        if (filename == null) filename = br.getRegex("<td>Datei: </td>.*?<td>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("<td>Dateigr.*?: </td>.*?<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        for (int i = 0; i <= 5; i++) {
            Form captchaForm = br.getForm(0);
            String captchaUrl = br.getRegex(">Captcha: </td>.*?<img src=\"(/lib.*?)\"").getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex("(/lib/captcha/CaptchaImage\\.php\\?uid=.*?)\"").getMatch(0);
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaUrl = "http://www.datenklo.net" + captchaUrl;
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("down_captcha", code);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("nicht korrekt abgetippt")) {
                    br.getPage(downloadLink.getDownloadURL());
                    continue;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            break;
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("nicht korrekt abgetippt")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
