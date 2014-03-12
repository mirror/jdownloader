//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "purevid.com" }, urls = { "http://(www\\.)?purevid\\.com/(\\?m=embed&id=[a-z0-9]+|v/[a-z0-9]+/)" }, flags = { 0 })
public class PureVidCom extends PluginForHost {
    // raztoki embed video player template.

    private String dllink = null;

    public PureVidCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobug.net/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        String fuid = new Regex(downloadLink.getDownloadURL(), "/(\\?m=embed&id=|v/)([a-z0-9]+)").getMatch(1);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("id=\"error\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Browser fl = br.cloneBrowser();
        fl.getHeaders().put("Referer", "http://www.purevid.com/include/fp.purevid-2.2.swf");
        fl.getHeaders().put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
        fl.getHeaders().put("Pragma", null);
        fl.getHeaders().put("Accept-Charset", null);
        fl.getHeaders().put("Cache-Control", null);
        fl.getPage("/?m=video_info_embed_dev&id=" + fuid);
        final String filename = fl.getRegex("\"titleHeader\":\"([^\"]+)").getMatch(0);
        final String sid = fl.getRegex("\"sid\",\"value\":\"(\\d+)\"").getMatch(0);
        dllink = fl.getRegex("\"url\":\"(http[^\"]+purevid\\.com[^\"]+/get[^\"]+)").getMatch(0);
        if (filename == null || sid == null || dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
        final String ext = dllink.substring(dllink.lastIndexOf("."));
        downloadLink.setFinalFileName(filename + ext);
        br.getPage("http://www.purevid.com/?m=statuscheck&token=&uid=&id=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)").getMatch(0) + "&sas=&sid=" + sid + "&cc=");
        if (br.containsHTML("\\?m=upgrade")) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable for registered/premium users");
            return AvailableStatus.TRUE;
        }
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (con.getResponseCode() == 403) return AvailableStatus.UNCHECKABLE;
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("\\?m=upgrade")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}