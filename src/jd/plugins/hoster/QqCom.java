//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "qq.com" }, urls = { "http://(www\\.)?(fenxiang\\.qq\\.com/filedownload\\.php\\?code=[^<>\"#]+|urlxf\\.qq\\.com/\\?\\w+)" }, flags = { 0 })
public class QqCom extends PluginForHost {

    public QqCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.qq.com/contract.shtml";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (link.getDownloadURL().matches("https?://urlxf\\.qq\\.com/\\?\\w+")) {
            br.getPage(link.getDownloadURL());
            String redirect = br.getRegex("window.location=\"(http[^\"]+)").getMatch(0);
            if (redirect == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(redirect);
        }
        if (br.containsHTML("(>分享文件已过期或者链接错误，请确认后重试。<|>想了解更多有关QQ旋风资源分享的信息，请访问 <a href=)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("filename:\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"a_filename\" href=\"###\" title=\"([^<>\"]+)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("filename=\"([^<>\"]+)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<td id=\"inform_filename\" title=\"([^<>\"]+)\"").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("class=\"inform_wrap_box_important\" >([^<>\"]+)</td>").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("filesize:\"(\\d+)\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<li>大小：([^<>\"]+)</li>").getMatch(0);
        }
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = null;
        requestFileInformation(downloadLink);
        String[] dlValues = br.getRegex("<a id=\"btn_normaldl\" class=\"btn_normal\".+ftnlink=\"([^\"]+)\" ftncookie=\"([^\"]+)\" dllink=\"(.*?)\" (.*?)=\"(.*?)\" filename=\"(.*?)\"></a>").getRow(0);
        if (dlValues == null) dlValues = br.getRegex("<a id=\"btn_normaldl\" class=\"btn_normal\".+ftnlink=\"([^\"]+)\" ftncookie=\"([^\"]+)\" filename=\"(.*?)\"></a>").getRow(0);
        if (dlValues == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (dlValues.length == 3) {
            br.setCookie(br.getHost(), "FTN5K", dlValues[1]);
            dllink = dlValues[0] + "/" + Encoding.urlEncode(dlValues[2]);
        } else if (dlValues[0] != null && dlValues[1] != null && dlValues[5] != null) {
            br.setCookie(br.getHost(), "FTN5K", dlValues[1]);
            dllink = dlValues[0] + "/" + Encoding.urlEncode(dlValues[5]);
        } else {
            br.setCookie(br.getHost(), dlValues[3].toUpperCase(), dlValues[4]);
            dllink = dlValues[2] + "/" + Encoding.Base64Encode(dlValues[5]);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);

        if (dl.getConnection().getResponseCode() == 503) {
            if (dl.getConnection().getResponseMessage().equals("Service Unavailable")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, " Service Unavailable!"); }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}