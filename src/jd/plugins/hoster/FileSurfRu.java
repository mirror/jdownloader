////    jDownloader - Downloadmanager
////    Copyright (C) 2009  JD-Team support@jdownloader.org
////
////    This program is free software: you can redistribute it and/or modify
////    it under the terms of the GNU General Public License as published by
////    the Free Software Foundation, either version 3 of the License, or
////    (at your option) any later version.
////
////    This program is distributed in the hope that it will be useful,
////    but WITHOUT ANY WARRANTY; without even the implied warranty of
////    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
////    GNU General Public License for more details.
////
////    You should have received a copy of the GNU General Public License
////    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//package jd.plugins.hoster;
//
//import jd.PluginWrapper;
//import jd.parser.Regex;
//import jd.parser.html.Form;
//import jd.plugins.BrowserAdapter;
//import jd.plugins.DownloadLink;
//import jd.plugins.HostPlugin;
//import jd.plugins.LinkStatus;
//import jd.plugins.Plugin;
//import jd.plugins.PluginException;
//import jd.plugins.PluginForHost;
//import jd.plugins.DownloadLink.AvailableStatus;
//
//@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesurf.ru" }, urls = { "http://[\\w\\.]*?filesurf\\.ru/[0-9]+" }, flags = { 0 })
//public class FileSurfRu extends PluginForHost {
//
//    public FileSurfRu(PluginWrapper wrapper) {
//        super(wrapper);
//        // TODO Auto-generated constructor stub
//    }
//
//    @Override
//    public String getAGBLink() {
//        return "http://filesurf.ru/rules.html";
//    }
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
import jd.plugins.Plugin;
import jd.plugins.BrowserAdapter;

//filesurf.ru by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesurf.ru" }, urls = { "http://[\\w\\.]*?(filesurf|4ppl|files\\.youmama|upload\\.xradio)\\.ru/[0-9]+" }, flags = { 0 })
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
        br.getPage(parameter.getDownloadURL());
        if (!br.containsHTML("<form")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("���� <b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("�������� <b>(.*?)</b>").getMatch(0);
        filesize = filesize.replace("����", "Bytes");
        filesize = filesize.replaceAll(",", "");
        filesize = filesize.replaceAll("\\.", "");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        if (br.containsHTML("password")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            captchaForm.put("password", passCode);
        }
        String captchaid = br.getRegex("src=\"/captcha.png\\?key=(.*?)\"><br>").getMatch(0);
        String captchaurl = "http://filesurf.ru/captcha.png?key=" + captchaid;
        String code = getCaptchaCode(captchaurl, link);
        // Captcha Usereingabe in die Form einfügen
        captchaForm.put("respond", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("captcha")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("<br><br>������ ��� ����������:<br><b><a href=\"(.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        BrowserAdapter.openDownload(br, link, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
