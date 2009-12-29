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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "openfile.ru" }, urls = { "http://[\\w\\.]*?openfile\\.ru/[0-9]+" }, flags = { 0 })
public class OpenFileRu extends PluginForHost {

    public OpenFileRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://openfile.ru/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        // Without stting the charset the jd browser shows only weird text
        // instead of the real one!
        br.setCustomCharset("windows-1251");
        // Cookie is there to skip that captcha but it didn't work...wll but
        // also the cookie doesn't hurt^^
        br.setCookie("http://openfile.ru", "MG_1145", "7");
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Файл удален или поступила жалоба от правообладателя")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)с OpenFile.ru</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("Вы собираетесь скачать файл.*?<b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("Размер: <strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        filesize = filesize.replaceAll("(&nbsp;|Б)", "");
        filesize = filesize.replaceAll("Г", "G");
        filesize = filesize.replaceAll("М", "M");
        filesize = filesize.replaceAll("к", "k");
        filesize = filesize + "b";
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        // TODO:Find out what that means, i called it "Server error" but i don't
        // even know what that text means...
        if (br.containsHTML("Причина: поступила жалоба от правообладателя")) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (br.containsHTML("Файл удален. Причина: истек срок хранения файла")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
        // Just a failed try to skip the captcha!
        // String id = new Regex(link.getDownloadURL(),
        // "openfile\\.ru/(\\d+)").getMatch(0);
        // br.getPage("http://openfile.ru/files/mg-tov.php?id=" + id);
        // String getlink = br.getRegex("var URL = '(.*?)'").getMatch(0);
        // if (getlink == null) getlink =
        // br.getRegex("'(/files/mg\\.php\\?getlink=1\\&id=[0-9]+)'").getMatch(0);
        // // Ticket Time
        // int tt = 31;
        // sleep(tt * 1001, link);
        //
        // Browser br2 = br.cloneBrowser();
        // br2.getPage("http://openfile.ru" + getlink);
        // System.out.print(br2.toString());
        // String dllink =
        // br2.getRegex("'(/files/mg\\.php\\?getlink=1\\&id=[0-9]+)'").getMatch(0);
        for (int i = 0; i <= 5; i++) {
            Form captchaform = br.getFormByKey("secret");
            String captchaurl = br.getRegex("style=\"color:.*?>Повторите.*?/td>.*?<td><img src=\"(.*?)\"").getMatch(0);
            if (captchaform == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode("http://openfile.ru" + captchaurl, link);
            captchaform.put("secret", code);
            br.submitForm(captchaform);
            if (br.containsHTML("(Неверно указано|код подтверждения)")) {
                logger.info("Wronmg captcha entered, retrying...");
                continue;
            }
            break;
        }
        if (br.containsHTML("(Неверно указано|код подтверждения)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String freelink = br.getRegex("<tr valign=\"top\">.*?<td></td>.*?<td><a href=\"(.*?)\"").getMatch(0);
        if (freelink == null) freelink = br.getRegex("\"(/files/mg-tov\\.php\\?id=[0-9]+)\"").getMatch(0);
        if (freelink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://openfile.ru" + freelink);
        String getlink = br.getRegex("var URL = '(.*?)'").getMatch(0);
        if (getlink == null) getlink = br.getRegex("'(/files/mg\\.php\\?getlink=1\\&id=[0-9]+)'").getMatch(0);
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("seconds\">(.*?)</").getMatch(0);
        int tt = 30;
        if (ttt != null) tt = Integer.parseInt(ttt);
        sleep(tt * 1001, link);
        br.getPage("http://openfile.ru" + getlink);
        String dllink = br.getRegex("link\":\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"http://dl([0-9]+)\\.openfile\\.ru/download/.*?/.*?/.*?/.*?/.*?\"").getMatch(0);
        // TODO:Hoster allows to connections at all so you can eigher download 1
        // file with 2 chunks or 2 files with one, a new controller could solve
        // this problem by being able to set the number of the max connections
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return 1;
    }

}
