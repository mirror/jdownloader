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
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//appscene.org by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "appscene.org" }, urls = { "http://[\\w\\.]*?appscene\\.org/(download/[0-9a-zA-Z]+|download\\.php\\?id=\\d+)" }, flags = { 0 })
public class AppSceneOrg extends PluginForHost {

    public AppSceneOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.appscene.org/about.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("or has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        for (int i = 0; i <= 3; i++) {
            Form captchaForm = br.getForm(0);
            String captchaurl = br.getRegex("\"(http://www.appscene.org/captcha/.*?)\"").getMatch(0);
            if (captchaForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("captcha", code);
            br.submitForm(captchaForm);
            if (br.getRedirectLocation().contains("appscene.org/download")) {
                br.getPage(br.getRedirectLocation());
                continue;
            }
            break;
        }
        if (br.getRedirectLocation().contains("appscene.org/download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
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
