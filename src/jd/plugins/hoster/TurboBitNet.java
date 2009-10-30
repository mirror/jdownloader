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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://[\\w\\.]*?(bluetooths.pp.ru|dz-files.ru|file.alexforum.ws|file.grad.by|file.krut-warez.ru|filebit.org|files.best-trainings.org.ua|files.wzor.ws|gdefile.ru|letitshare.ru|mnogofiles.com|share.uz|sibit.net|turbo-bit.ru|turbobit.net|turbobit.ru|upload.mskvn.by|vipbit.ru|files.prime-speed.ru|filestore.net.ru|turbobit.ru|upload.dwmedia.ru|upload.uz|xrfiles.ru)/[a-z0-9]+\\.html" }, flags = { 0 })
public class TurboBitNet extends PluginForHost {

    public TurboBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://turbobit.net/rules";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<div class=\"code-404\">404</div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = br.getRegex("<span class='file-icon .*?'>&nbsp;</span><b>(.*?)</b></h1>").getMatch(0);
        String fileSize = br.getRegex("<div><b>Размер файла:</b> (.*?).</div></div>").getMatch(0);
        if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        fileSize = fileSize.replaceAll("М", "M");
        fileSize = fileSize.replaceAll("к", "k");
        fileSize = fileSize + "b";
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(Regex.getSize(fileSize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile(".*/(.*?)\\.html")).getMatch(0);
        br.getPage("http://turbobit.net/download/free/" + id);
        if (br.containsHTML("Попробуйте повторить через")) {
            int wait = Integer.parseInt(br.getRegex("<span id='timeout'>(\\d+)</span></h1>").getMatch(0));
            if (wait < 31) {
                sleep(wait * 1000l, downloadLink);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001);
        }
        String captchaUrl = br.getRegex("<img alt=\"Captcha\" src=\"(.*?)\" width=\"150\" height=\"50\" />").getMatch(0);
        if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form form = br.getForm(2);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);

            form.put("captcha_response", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("updateTime: function()")) break;
        }
        if (!br.containsHTML("updateTime: function()")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        int tt = Integer.parseInt(br.getRegex("limit: (\\d+),").getMatch(0));
        sleep(tt * 1001l, downloadLink);
        br.getPage("http://turbobit.net/download/timeout/" + id);

        String downloadUrl = br.getRegex("<a href='(.*?)'>").getMatch(0);
        if (downloadUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}