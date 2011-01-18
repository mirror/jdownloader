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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hostuje.net" }, urls = { "http://[\\w\\.]*?hostuje\\.net/file\\.php\\?id=[a-zA-Z0-9]+" }, flags = { 0 })
public class HostUjeNet extends PluginForHost {

    public HostUjeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://hostuje.net/regulamin.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Podany plik nie zosta? odnaleziony")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[] infos = br.getRegex("<b>Plik:</b> (.*?)<br><b>Rozmiar:</b> (.*?)<br>").getRow(0);
        if (infos == null || infos[0] == null || infos[1] == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(infos[0].trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(infos[1].trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String code = getCaptchaCode(br.getBaseURL() + "obraz.php", downloadLink);
        Form captchaForm = br.getForm(1);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaForm.put("kod", code);
        captchaForm.put("REG", "1");
        br.setFollowRedirects(false);
        br.submitForm(captchaForm);
        String link = null;
        link = br.getRedirectLocation();
        if (link == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, false, 1);
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
