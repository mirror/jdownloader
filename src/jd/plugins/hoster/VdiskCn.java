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
import jd.config.Property;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vdisk.cn" }, urls = { "http://(www\\.)?([a-z0-9]+\\.)?vdisk\\.cn/down/index/[A-Z0-9]+" }, flags = { 0 })
public class VdiskCn extends PluginForHost {

    // No HTTPS
    // Found hard to test this hoster, has many server issues.
    // locked it to 2(dl) * -4(chunk) = 8 total connection
    // other: they keep changing final download links url structure, best to use
    // regex only on finallink static info and not html

    private static final String UA = RandomUserAgent.generate();

    public VdiskCn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        /** We only use the main domain */
        link.setUrlDownload(link.getDownloadURL().replaceAll("(www\\.)?([a-z0-9]+\\.)?vdisk\\.cn/down", "www.vdisk.cn/down"));
    }

    @Override
    public String getAGBLink() {
        return "http://vdisk.cn/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = downloadLink.getStringProperty("freelink");
        boolean startDL = false;
        if (dllink != null) {
            try {
                br.setReadTimeout(3 * 60 * 1000);
                br.setFollowRedirects(true);
                br.setCookie("http://vdisk.cn/", "lang", "en");
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
                if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                } else {
                    startDL = true;
                }
            } catch (Exception e) {
                startDL = false;
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            requestFileInformation(downloadLink);
            dllink = br.getRegex("(http://[\\w\\.]+?vdisk\\.cn/[^/]+/[0-9A-Z]{2}/[A-Z0-9]{32}\\?key=[a-z0-9]{32}[^\"\\>]+)").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (startDL == false) dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", UA);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        br.setCookie("http://vdisk.cn/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(文件已删除,无法下载\\.|>此文件涉嫌有害信息不允许下载\\!<|>找不到您需要的页面\\!<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(?i)文件名称: <b>(.*?)</b><br>").getMatch(0);
        if (filename == null) filename = br.getRegex("(?i)<META content=\"(.*?)\" name=\"description\">").getMatch(0);
        String filesize = br.getRegex("(?i)文件大小: ([\\d\\.]+ ?(GB|MB|KB|B))").getMatch(0);
        if (filesize == null) {
            logger.warning("Can't find filesize, Please report issue to JDownloader Development!");
            logger.warning("Continuing...");
        }
        String MD5sum = br.getRegex("(?i)文件校验: ([A-Z0-9]{32})").getMatch(0);
        if (MD5sum == null) {
            logger.warning("Can't find MD5sum, Please report issue to JDownloader Development!");
            logger.warning("Continuing...");
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (MD5sum != null) link.setMD5Hash(MD5sum);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}