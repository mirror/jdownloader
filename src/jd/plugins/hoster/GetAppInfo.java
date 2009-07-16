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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"getapp.info"}, urls = {"http://[\\w\\.]*?getapp.info/download/.+"}, flags = {0})
public class GetAppInfo extends PluginForHost {

    public GetAppInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.appscene.org/about.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The file requested is either invalid or may have been claimed by copyright holders.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = Encoding.htmlDecode(br.getRegex("File Name:<font color=\"#0088CC\">(.*?)</font><br>").getMatch(0));
        String filesize = br.getRegex("File Size:\\s+<font color=\"#0088CC\">(.*?( MB| KB| GB| b))</font><br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        //Link zum Captcha
        String captchaurl = "http://getapp.info/image.php";
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(1);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        //Captcha Usereingabe in die Form einfügen
        captchaForm.put("secure", code);
        //Auskommentierte Wartezeit, die momentan nicht gebraucht wird, da man sie überspringen kann
        //sleep(13000l, downloadLink);
        //sendet die ganze Form
        br.submitForm(captchaForm);
        if (!br.containsHTML("Download!</font>")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String downloadURL = br.getRegex("href='(.*?)'>").getMatch(0);
        dl = br.openDownload(downloadLink, downloadURL, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
}
