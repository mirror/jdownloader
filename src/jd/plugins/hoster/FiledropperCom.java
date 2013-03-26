//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filedropper.com" }, urls = { "http://[\\w\\.]*?filedropper\\.com/[A-Za-z0-9-_]+" }, flags = { 0 })
public class FiledropperCom extends PluginForHost {

    public FiledropperCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filedropper.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form captchaForm = null;
        URLConnectionAdapter con = null;
        boolean valid = false;

        for (int i = 0; i <= 5; i++) {
            captchaForm = br.getForm(0);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            String captchaUrl = br.getRegex("src=\"(securimage/securimage_show\\.php\\?sid=[0-9a-z]{32})\"").getMatch(0);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("code", code);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, false, 1);
            con = dl.getConnection();
            if (dl.getConnection().getContentType().contains("html")) {
                dl.getConnection().disconnect();
                valid = false;
                br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            valid = true;
            break;
        }
        if (valid == false) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("ISO-8859-1");
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("File Details:.*?Filename: (.*?) <br>").getMatch(0);
        String filesize = br.getRegex("File Details:.*?Size: (.*?), Type:.*?<br>").getMatch(0);
        if (!(filename == null || filesize == null)) {
            filename = Encoding.htmlDecode(filename);
            downloadLink.setName(filename);
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")) / 1024);
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}