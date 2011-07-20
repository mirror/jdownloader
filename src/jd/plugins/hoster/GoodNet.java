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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "good.net" }, urls = { "http://(www\\.)?gjerzu4zr4jk555hd/.+" }, flags = { 0 })
public class GoodNet extends PluginForHost {

    public GoodNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://forums.good.net/phpBB/viewtopic.php?f=7&t=14&sid=";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gjerzu4zr4jk555hd", "good.net"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String link = parameter.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                parameter.setName(getFileNameFromHeader(con));
                parameter.setDownloadSize(con.getLongContentLength());
            } else {
                br.followConnection();
                br.getPage(link);
                String filename = br.getRegex("<title>(.*?): </title>").getMatch(0);
                if (filename == null) filename = br.getRegex("basefilename\">(.*?)</span>").getMatch(0);
                String filesize = br.getRegex("id=\"humansize\">(.*?)</span").getMatch(0);
                if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                parameter.setName(filename.trim());
                if (filesize != null) {
                    filesize = filesize.replace("i", "");
                    parameter.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        String dllink = null;
        if (!con.getContentType().contains("html")) {
            dllink = link.getDownloadURL();
            con.disconnect();
        } else {
            br.followConnection();
            dllink = br.getRegex("\"(/dl/.*?/wait)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!dllink.contains("good.net/")) dllink = "http://good.net" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            dllink = br.getRegex("thelinkA\" href=\"(http.*?)\"").getMatch(0);
            if (dllink != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            if ((dl.getConnection().getContentType().contains("html"))) br.followConnection();
            if (br.containsHTML("(Please ensure there are no more than.*?downloads going on concurrently|There are.*?connections open to good.net servers from)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

}
