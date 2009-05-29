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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class ZidduCom extends PluginForHost {

    public ZidduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form form = br.getFormbyProperty("name", "dfrm");
        br.submitForm(form);
        if (br.containsHTML("File\\snot\\s+found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        form = br.getFormbyProperty("name", "securefrm");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String code = getCaptchaCode("http://www.ziddu.com/CaptchaSecurityImages.php?width=40&height=38&characters=2", downloadLink);
        form.put("securitycode", code);

        dl = br.openDownload(downloadLink, form, true, 1);
        /*
         * Folgendes nicht optimal da bei .isContentDisposition == false immer
         * angenommen wird dass das Captcha falsch war.
         */
        if (!dl.getConnection().isContentDisposition()) throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
        dl.setResume(true);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Tested up to 15
        return 15;
    }

    // @Override
    public String getAGBLink() {
        return "http://www.ziddu.com/termsandconditions.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        String Url = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.getPage(Url);
        if (br.getRedirectLocation() != null && (br.getRedirectLocation().contains("errortracking") || br.getRedirectLocation().contains("notfound"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("textblue14\">(.*?)</span>").getMatch(0));
        String filesize = br.getRegex("File\\sSize\\s:.*normal12black\">(.*?)\\s+</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
    }

}