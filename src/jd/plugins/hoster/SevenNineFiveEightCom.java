//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "7958.com" }, urls = { "http://(www\\.)?[a-z0-9]+\\.7958\\.com(\\.cn)?/down_\\d+\\.html" }, flags = { 0 })
public class SevenNineFiveEightCom extends PluginForHost {

    public SevenNineFiveEightCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.7958.com/tiaokuan.html";
    }

    @Override
    public String rewriteHost(String host) {
        if ("7958.com".equals(getHost())) {
            if (host == null || "7958.com".equals(host)) {
                return "7958.com.cn";
            }
        }
        return super.rewriteHost(host);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("7958.com/", "7958.com.cn/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().contains("/404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/down_\\d+\\.html target=_blank>([^<>\"]*?)</a>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"downfenxianginput\"[\t\n\r ]+value=\"\\[url=http://[^<>\"]+\\]下载：([^<>\"]*?)\\[/url\\]\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("下载：([^<>\"]*?)\\[/url\\]\" class=\"ltdm\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<h2 style=\"float:left;width:auto\">([^<>\"]*?)</h2>").getMatch(0);
        }
        String filesize = br.getRegex("> 文件大小：([^<>\"]*?)</td>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">文件大小：([^<>\"]*?)</td>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex(">文件大小：([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize + "b"));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final String user_host = new Regex(downloadLink.getDownloadURL(), "(https?://[a-z0-9\\.]+\\.7958\\.com\\.cn)/").getMatch(0);
            br.getPage(downloadLink.getDownloadURL().replace("/down_", "/download_"));
            dllink = br.getRegex("\"(/download/downfile\\?sid=\\d+)\"").getMatch(0);
            if (dllink == null) {
                logger.info("Only downloadable via premium");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered/premium users");
            }
            dllink = user_host + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}