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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshaker.com" }, urls = { "http://[\\w\\.]*?fileshaker\\.com/.+" }, flags = { 0 })
public class FileshakerCom extends PluginForHost {

    public FileshakerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileshaker.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.getPage(url);
        if (!br.getURL().equals("http://www.fileshaker.com/")) {
            String downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<br><br>File: (.*)<br><br>", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (downloadName == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(downloadName.trim());
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        Form captchaForm = null;
        URLConnectionAdapter con = null;
        boolean valid = false;

        for (int i = 0; i <= 5; i++) {
            captchaForm = br.getForm(0);
            String captchaUrl = br.getRegex("(securimage/securimage_show\\.php\\?sid=[0-9a-z]{32})\"").getMatch(0);
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaUrl = "http://www.fileshaker.com/" + captchaUrl;
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaForm.put("code", code);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, false, 1);
            con = dl.getConnection();

            if (!dl.getConnection().getContentType().contains("html")) {
                valid = true;
                break;
            } else {
                con.disconnect();
                br.getPage(downloadLink.getDownloadURL());
            }

        }

        if (valid == false) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
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
