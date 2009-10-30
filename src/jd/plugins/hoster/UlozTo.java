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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uloz.to" }, urls = { "http://[\\w\\.]*?(uloz\\.to|ulozto\\.sk|ulozto\\.cz|ulozto\\.net)/[0-9]+/" }, flags = { 0 })
public class UlozTo extends PluginForHost {

    public UlozTo(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    @Override
    public String getAGBLink() {
        return "http://ulozto.net/podminky/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        // Wrong links show the mainpage so here we check if we got the mainpage
        // or not
        if (br.containsHTML("multipart/form-data")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(Pattern.compile("0px;color:#000\"><b>(.*?)</b>")).getMatch(0);

        String filesize = br.getRegex(Pattern.compile("Velikost souboru je <b>(.*?)</b> <br />")).getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(Pattern.compile("Veľkosť súboru je <b>(.*?)</b> <br />")).getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String captchaUrl = br.getRegex(Pattern.compile("<img id=\"captcha\" name=\"captcha\" width=\"175\" height=\"70\" src=\"(.*?)\" alt=\"img\">")).getMatch(0);
        String code = getCaptchaCode(captchaUrl, downloadLink);
        Form captchaForm = br.getFormbyProperty("name", "dwn");
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaForm.put("captcha_user", code);
        br.submitForm(captchaForm);
        String dllink = br.getRedirectLocation();
        // Usually this errorhandling is not needed but in case...
        if (dllink == null) {
            if (br.containsHTML("Neopsal jsi spr.vn. text z obr.zku")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("no#cpt")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
