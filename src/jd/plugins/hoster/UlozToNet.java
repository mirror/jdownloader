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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class UlozToNet extends PluginForHost {

    public UlozToNet(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://ulozto.net/podminky/";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        System.out.print(br);
        String name = br.getRegex(Pattern.compile("<div style=\"font-size:16px;color:000;\"><b>(.*?)</b>")).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex(Pattern.compile("Velikost souboru je <b>(.*?)</b> <br />")).getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String captchaUrl = br.getRegex(Pattern.compile("<img id=\"captcha\" name=\"captcha\" width=\"175\" height=\"70\" src=\"(.*?)\" alt=\"img\">")).getMatch(0);
        String code = getCaptchaCode(captchaUrl, downloadLink);
        Form captchaForm = br.getFormbyProperty("name", "dwn");
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        captchaForm.put("captcha_user", code);
        br.setFollowRedirects(false);
        br.submitForm(captchaForm);

        String dlLink = br.getRedirectLocation();
        if (dlLink == null) {
            if (br.containsHTML("falschen Code eingegeben")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }

        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, dlLink, false, 1);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getMaxSimultanDownloadNum();
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