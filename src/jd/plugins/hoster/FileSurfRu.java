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

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesurf.ru" }, urls = { "http://[\\w\\.]*?(filesurf|4ppl|files\\.youmama)\\.ru/[0-9]+" }, flags = { 0 })
public class FileSurfRu extends PluginForHost {

    public FileSurfRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filesurf.ru/rules.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1251");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Запрошенный файл не существует")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Файл <b>(.*?)</b>").getMatch(0);
        // String filesize = br.getRegex("(\\d+,[0-9.]+ Кб)").getMatch(0);
        String filesize = br.getRegex("<b>([0-9,]+ байт)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace("Кб", "KB").replace("байт", "byte");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String passCode = null;
        br.getPage(link.getDownloadURL());
        for (int i = 0; i <= 3; i++) {
            Form captchaForm = br.getForm(0);
            String captchaid = br.getRegex("src=\"/captcha.png\\?key=(.*?)\"><br>").getMatch(0);
            if (captchaForm == null || captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (br.containsHTML("password")) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                captchaForm.put("password", passCode);
            }
            String captchaurl = "http://filesurf.ru/captcha.png?key=" + captchaid;
            String code = getCaptchaCode(captchaurl, link);
            // Captcha Usereingabe in die Form einfügen
            captchaForm.put("respond", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("captcha")) {
                logger.warning("Wrong password!");
                link.setProperty("pass", null);
                continue;
            }
            break;
        }
        if (br.containsHTML("captcha")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("Ссылка для скачивания:<br><b><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://[a-z]+\\.filesurf\\.ru/\\d+/\\d+/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
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
        return -1;
    }

}
