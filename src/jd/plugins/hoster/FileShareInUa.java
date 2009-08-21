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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshare.in.ua" }, urls = { "http://[\\w\\.]*?fileshare\\.in\\.ua/[0-9]+" }, flags = { 0 })
public class FileShareInUa extends PluginForHost {

    public FileShareInUa(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://fileshare.in.ua/about.aspx";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        String freepage = link.getDownloadURL() + "?free";
        br.getPage(freepage);
        if (br.containsHTML("Возможно, файл был удален по просьбе владельца авторских прав")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("class=\"dnld_filename\">(.*?)</h1></td>").getMatch(0));
        String filesize = br.getRegex("размер: <b>(.*?)</b></div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String freepage = downloadLink.getDownloadURL() + "?free";
        br.getPage(freepage);
        String captchaid = br.getRegex("=\"border: 1px solid #e0e0e0;\" src=\"(.*?)\" width=\"10").getMatch(0);
        if (captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String captchaurl = "http://fileshare.in.ua" + captchaid;
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(2);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        captchaForm.put("capture", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("Цифры введены неверно")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        Form DLForm0 = br.getForm(2);
        if (DLForm0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(DLForm0);
        String dlframe = downloadLink.getDownloadURL() + "?fr";
        br.getPage(dlframe);
        String dllink0 = br.getRegex("yandex_bar\"  href=\"(.*?)\" id=\"dl_li").getMatch(0);
        if (dllink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String dllink1 = "http://fileshare.in.ua" + dllink0;
        br.getPage(dllink1);
        String dllink = br.getRedirectLocation();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -20);
        dl.startDownload();
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
}