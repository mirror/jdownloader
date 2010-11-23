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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitfile.ru" }, urls = { "http://[\\w\\.]*?letitfile\\.(ru|com)/download/id\\d+" }, flags = { 0 })
public class LetitFileRu extends PluginForHost {

    public LetitFileRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String URLPIECE = "get_file";

    @Override
    public String getAGBLink() {
        return "http://letitfile.ru/rules/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("letitfile.com", "letitfile.ru"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Файл не найден")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(File name|Имя файла): <b>(.*?)</b>").getMatch(1);
        String filesize = br.getRegex("(File size|Размер файла): <b>(.*?)</b>").getMatch(1);
        if (filename == null || filesize == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replace("к", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        link.setDownloadSize(Regex.getSize(filesize));
        if (br.containsHTML("Владелец данного файла разрешил скачивать файл только пользователям")) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.LetitFileRu.Only4Premium", "This file is only downloadable for premium users!"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Владелец данного файла разрешил скачивать файл только пользователям")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.LetitFileRu.Only4Premium", "This file is only downloadable for premium users!"));
        String dllink = null;
        for (int i = 0; i <= 3; i++) {
            if (!br.containsHTML("/captcha/cap.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaUrl = "http://letitfile.ru/captcha/cap.php";
            String code = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "cap=" + code + "&down=y&x=0&y=0");
            if (!br.getURL().contains(URLPIECE)) continue;
            // if (br.containsHTML("Введите код с картинки")) continue;
            break;
        }
        if (!br.getURL().contains(URLPIECE)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = br.getRegex("\\(\"\\.dtl\"\\)\\.html\\(\\'Ваша <a href=\"(/.*?)\">ссылка</a>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(/getfile/id\\d+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("class=\"timer\">(\\d+)</span").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            if (Integer.parseInt(ttt) < 100) tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        dllink = "http://letitfile.ru" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}