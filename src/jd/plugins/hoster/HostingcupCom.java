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

import java.io.File;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hostingcup.com" }, urls = { "http://www\\.hostingcup\\.com/[0-9a-zA-Z]+\\.html" }, flags = { 2 })
public class HostingcupCom extends PluginForHost {

    public HostingcupCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://hostingcup.com/tos.html";
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://hostingcup.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("<B>Filename:.+?<TD noWrap>(.+?)</TD></TR>").getMatch(0);
        String filesize = br.getRegex("<SMALL>\\(([\\d]+) bytes\\)</SMALL>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);

        Recaptcha rc = new Recaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, link);
        rc.setCode(c);
        if (br.containsHTML(">Wrong captcha<")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("Expired session")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);

        String dl_url = br.getRegex("<a href=\"([^><]*?" + link.getName() + ").+?" + "</a>").getMatch(0);
        if (dl_url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, true, 1);
        dl.startDownload();
    }

    public void reset() {
    }
    
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }
    
    

    public void resetDownloadlink(DownloadLink link) {
    }

}
