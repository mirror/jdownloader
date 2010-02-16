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
import jd.captcha.easy.load.LoadImage;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "onlinedisk.ru" }, urls = { "http://[\\w\\.]*?onlinedisk\\.ru/(file|view)/[0-9]+" }, flags = { 0 })
public class OnlineDiskRu extends PluginForHost {

    public OnlineDiskRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        setBrowserExclusive();
        return "http://www.onlinedisk.ru/conditions/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("windows-1251");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Возможно, файл, который Вы запрашиваете, был удален") || br.containsHTML("image/404.jpg") || br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (downloadLink.getDownloadURL().contains("/view/")) {
            if (!br.containsHTML("id='photo'")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "onlinedisk\\.ru/view/([0-9]+)").getMatch(0));
        } else {
            String filename = br.getRegex("Файл: <b style='font-size:[0-9]+px;'>(.*?)</b>").getMatch(0);
            String fileSize = br.getRegex("<b>Размер файла.*?:</b>(.*?)</div>").getMatch(0);
            if (fileSize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            fileSize = fileSize.trim();
            fileSize = fileSize.replace("б", "");
            fileSize = fileSize.replace("Г", "G");
            fileSize = fileSize.replace("м", "M");
            fileSize = fileSize.replace("к", "k");
            fileSize = fileSize + "b";
            fileSize = fileSize.replace("айт", "");
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(fileSize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains("/file/")) {
            String captchaUrl = br.getRegex("class='captcha'>.*?<img src='(.*?)'").getMatch(0);
            Form captchaform = null;
            for (int i = 0; i <= 5; i++) {
                captchaform = br.getForm(0);
                if (captchaUrl == null || captchaform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode(captchaUrl, downloadLink);
                captchaform.put("kaptcha", code);
                br.submitForm(captchaform);
                if (br.containsHTML("name='kaptcha'") || br.containsHTML("class='captcha'")) continue;
                break;
            }
            if (br.containsHTML("name='kaptcha'") || br.containsHTML("class='captcha'")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            Form finalform = br.getForm(0);
            if (finalform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalform, true, 1);
        } else {
            String dllink = "http://www.onlinedisk.ru/get_image.php?id=" + new Regex(downloadLink.getDownloadURL(), "onlinedisk\\.ru/view/([0-9]+)").getMatch(0);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            String ending = LoadImage.getFileType(dllink, dl.getConnection().getContentType());
            downloadLink.setFinalFileName(downloadLink.getName() + ending);
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