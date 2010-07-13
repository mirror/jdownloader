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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nakido.com" }, urls = { "http://[\\w\\.]*?nakido\\.com/[A-Z0-9]+" }, flags = { 0 })
public class NakidoCom extends PluginForHost {

    public NakidoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nakido.com/contact";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.nakido.com", "lang", "en-us");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The page you have requested is not exists")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String filesize = br.getRegex(">Size:</td>[\n\r\t ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("lass=\"content\"><span>(.*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("urlForm\\.html_link\\.value = '<a href=\"http.*?\">(.*?)</a>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("\"http://www\\.nakido\\.com/flag\\.gif.*?/a><a href=\"http.*?\">(.*?)</a>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("\"http://www\\.nakido\\.com/nakido\\.gif.*?></a><br><a href=\"http.*?\">(.*?)</a>").getMatch(0);
                    }
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) {
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String dllink = br.getRegex("else.*?x\\.href='(.*?)'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("'(/[A-Z0-9]+/[A-Z0-9]+\\?attach.*?)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.equals("javascript:void(0)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available!");
        dllink = "http://www.nakido.com" + dllink;
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("You have reach concurrent connection to the server")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
            if (br.getURL().contains("SERVER_BUSY")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server busy");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}