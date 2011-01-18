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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vspace.cc" }, urls = { "http://[\\w\\.]*?vspace\\.cc/file/[A-Z0-9]+\\.html" }, flags = { 0 })
public class VspaceCc extends PluginForHost {

    public VspaceCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://home.vspace.cc/";
    }

    private static final String CAPTCHATEXT   = "/image\\.php";
    private static final String CAPTCHAFAILED = ">認證失敗";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("\\(File does not exist\\)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<td width=\"226\">\\&nbsp;<font size=2>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex(">檔案大小:</td>[\t\n\r ]+<td>\\&nbsp;<font size=2>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String fid = new Regex(downloadLink.getDownloadURL(), "vspace\\.cc/file/([A-Z0-9]+)\\.html").getMatch(0);
        String cUrl = br.getRegex("\\{do_down\\(\\'[A-Z0-9]+\\',\\'(.*?)\\'\\)").getMatch(0);
        if (cUrl == null) cUrl = br.getRegex("\"http://vspace\\.cc/\\?act=purl\\&url=(.*?)\\&st=ing\"").getMatch(0);
        if (fid == null || cUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 0; i <= 3; i++) {
            String getUrl = "http://vspace.cc/file/ajax_p.php?idParse=down&showDiv=act_msg&num=" + getCaptchaCode("http://vspace.cc/image.php", downloadLink) + "&fid=" + fid + "&c_url=" + cUrl + "&parm=" + System.currentTimeMillis();
            br.getPage(getUrl);
            if (br.containsHTML(CAPTCHAFAILED)) continue;
            break;
        }
        if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dlID = br.getRegex("DisableEnable\\(this\\.id,\\'(.*?)\\'").getMatch(0);
        if (dlID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://file.vspace.cc/file/dp.php?idParse=down_ok&showDiv=act_msg&fid=" + dlID;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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