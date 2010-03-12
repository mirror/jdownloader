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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesavr.com" }, urls = { "http://[\\w\\.]*?filesavr\\.com/[A-Za-z0-9]+(_\\d+)?" }, flags = { 0 })
public class FileSavrCom extends PluginForHost {

    public FileSavrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesavr.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("\\.\\./images/download2\\.png")) {
            String filename = new Regex(downloadLink.getDownloadURL(), "http://[\\w\\.]*?filesavr\\.com/([A-Za-z0-9]+(_\\d+)?)").getMatch(0);

            if (filename != null) {
                downloadLink.setName(filename);
                return AvailableStatus.TRUE;
            }

        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form captchaForm = null;
        URLConnectionAdapter con = null;
        boolean valid = false;

        for (int i = 0; i <= 5; i++) {
            captchaForm = br.getForm(0);
            String captchaUrl = br.getRegex("(securimage/securimage_show\\.php\\?sid=[0-9a-z]{32})\"").getMatch(0);
            if (captchaForm == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaUrl = "http://www.filesavr.com/" + captchaUrl;
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }

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
