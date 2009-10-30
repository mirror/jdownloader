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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freefolder.net" }, urls = { "http://[\\w\\.]*?freefolder\\.net/f/[A-Z0-9]+" }, flags = { 0 })
public class FreeFolderNet extends PluginForHost {

    public FreeFolderNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://freefolder.net/support.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Sorry, no file here! It seems, that you made a mistake in URL or this file was removed")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex reg = br.getRegex("<a href=\"http://freefolder\\.net/f/.*?\">(.*?)</a> \\|(.*?)</td>");
        String filesize = reg.getMatch(1);
        String filename = reg.getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setFinalFileName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Regex reg = br.getRegex("<a href=\"http://freefolder\\.net/f/.*?\">(.*?)</a> \\|(.*?)</td>");
        String filesize = reg.getMatch(1);
        String filename1 = reg.getMatch(0);
        for (int i = 0; i <= 5; i++) {

            br.getPage(downloadLink.getDownloadURL());
            br.setFollowRedirects(false);
            Form captchaForm = br.getFormbyKey("captcha");
            if (captchaForm == null && filename1 != null && filesize != null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium"); }
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchaurl = br.getRegex("false\"><img src=\"(.*?)\"").getMatch(0);
            if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("captcha", code);
            captchaForm.put("btn_free.x", "0");
            captchaForm.put("btn_free.y", "0");
            br.submitForm(captchaForm);
            String captchacheck = br.getRedirectLocation();
            if (captchacheck != null) continue;
            break;
        }
        String captchacheck = br.getRedirectLocation();
        if (captchacheck != null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dlpage = br.getRegex("url: \"(.*?)\",").getMatch(0);
        if (dlpage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        int tt = Integer.parseInt(br.getRegex("var timeleft = (\\d+);").getMatch(0));
        sleep(tt * 1001l, downloadLink);
        br.getPage(dlpage);
        String server = br.getRegex("<server>(.*?)</server>").getMatch(0);
        String linkpart = br.getRegex("<link>(.*?)</link>").getMatch(0);
        String filename = br.getRegex("<filename>(.*?)</filename>").getMatch(0);
        if (server == null || linkpart == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + server + ".freefolder.net/d/" + linkpart + "/" + filename;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
}