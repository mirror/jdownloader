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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fastfileshare.com.ar" }, urls = { "http://[\\w\\.]*?fastfileshare\\.com\\.ar/index\\.php\\?p=download\\&hash=[A-Za-z0-9]+" }, flags = { 0 })
public class FastFileShareComAr extends PluginForHost {

    public FastFileShareComAr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://fastfileshare.com.ar/index.php?p=rules";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Use the english language
        link.setUrlDownload(link.getDownloadURL() + "&langSwitch=english&");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("('File Not Found|The specified file can not found in our servers)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<strong>Filename:</strong></div></td>.*?<td width=\"\\d+%\"><div align=\"left\" class=\"style47\">(.*?)<img").getMatch(0);
        String filesize = br.getRegex("<strong>Filesize:</strong></div></td>.*?<td><div align=\"left\"  class=\"style47\">(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String md5 = br.getRegex("<strong>Suma MD5: </strong></div></td>.*?<td width=\"\\d+%\"><div align=\"left\" class=\"style47\">(.*?)</div>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (!br.containsHTML("/temp/")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form dlform = br.getForm(0);
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = null;
        for (int i = 0; i <= 5; i++) {
            String captchaurl = br.getRegex("class=\"captchapict\" src=\"(.*?)\"").getMatch(0);
            if (captchaurl == null) {
                logger.info("Captcharegex 1 failed!");
                captchaurl = br.getRegex("\"(http://fastfileshare\\.com\\.ar/temp/[a-z0-9]+\\.jpg)\"").getMatch(0);
            }
            if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            dlform.put("private_key", code);
            br.submitForm(dlform);
            dllink = br.getRedirectLocation();
            if (dllink == null) continue;
            break;
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}