//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.uvaga.by" }, urls = { "http://[\\w\\.]*?up\\.uvaga\\.by/download\\.php\\?filename=[a-z0-9]+" }, flags = { 0 })
public class UpAvagaBy extends PluginForHost {

    public UpAvagaBy(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Note: This site is NOT available in Germany
    @Override
    public String getAGBLink() {
        return "http://up.uvaga.by/rules.html";
    }

    private static final String MAINPAGE = "http://up.uvaga.by/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1251");
        br.getPage(link.getDownloadURL());
        Regex fileInformation = br.getRegex("<p>Имя файла <b>(.*?)</b> \\((.*?)\\)\\.</p>");
        if (br.containsHTML("<p>Неправильная ссылка на файл либо файл был удален\\.</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = fileInformation.getMatch(0);
        String filesize = fileInformation.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Waittime is there for all files exept files under 5 MB
        Regex waittime = br.getRegex("id='myclock'>(\\d+)\\.(\\d+)</span>");
        if (waittime.getMatch(0) == null || waittime.getMatch(1) == null) waittime = br.getRegex("var t=(\\d+)\\.(\\d+);");
        String minutes = waittime.getMatch(0);
        String seconds = waittime.getMatch(1);
        if (minutes != null & seconds != null) sleep(((Integer.parseInt(minutes) * 60) + Integer.parseInt(seconds)) * 1001l, downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getCookie(MAINPAGE, "PHPSESSID") == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = null;
        for (int i = 0; i <= 5; i++) {
            String captchaImageLink = "http://up.uvaga.by/kcaptcha/kap.php?ID=" + br.getCookie(MAINPAGE, "PHPSESSID");
            String captchaPage = "http://up.uvaga.by/get_file.php?keystring=" + getCaptchaCode(captchaImageLink, downloadLink) + "&filename=" + new Regex(downloadLink.getDownloadURL(), "filename=([a-z0-9]+)").getMatch(0);
            br.getPage(captchaPage);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            break;
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}