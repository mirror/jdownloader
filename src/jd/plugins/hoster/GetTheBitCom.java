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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "getthebit.com" }, urls = { "http://[\\w\\.]*?getthebit\\.com/f/[a-z]+/[a-z]+/.+" }, flags = { 0 })
public class GetTheBitCom extends PluginForHost {

    public GetTheBitCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.getthebit.com/index.php?s=users&ev=about";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(false);
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("Файл был удален пользователем или нарушает авторские права")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>GettheBit.Com - (.*?) \\(.*?\\)</title>").getMatch(0);

        String filesize = br.getRegex("&nbsp;\\(<b>(.*?)</b>\\)</h2>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String freeurl = br.getRegex("class=\"free\"><a href=\"(.*?)\"").getMatch(0);
        if (freeurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(freeurl);
        String redirectframe = br.getRegex("frame src=\"(.*?)\"").getMatch(0);
        if (redirectframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(redirectframe);
        int tt = Integer.parseInt(br.getRegex("var wtime = (\\d+);").getMatch(0));
        sleep(tt * 1001l, downloadLink);
        br.getPage(redirectframe);
        for (int i = 0; i <= 5; i++) {
            Form captchaForm = br.getForm(1);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String captchaUrl = br.getRegex("><img src=\"(.*?)\"").getMatch(0);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("kcapcha", code);
            br.submitForm(captchaForm);
            if (!br.containsHTML("Вы ввели неправильный код проверки")) break;
            continue;
        }
        if (br.containsHTML("Вы ввели неправильный код проверки")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("ссылку <a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }
}
