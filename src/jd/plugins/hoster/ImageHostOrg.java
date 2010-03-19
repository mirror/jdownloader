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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagehost.org" }, urls = { "http://[\\w\\.]*?imagehost\\.org/(download/[0-9]+/.+|[0-9]+/.+)" }, flags = { 0 })
public class ImageHostOrg extends PluginForHost {

    public ImageHostOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.imagehost.org/?p=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getDownloadURL().contains("/download/")) {
            // Handling for normal (file) links
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("</i> not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("<title>(.*?)- ImageHost.org</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("ajax_request\\.open\\(\"POST\",\"http://b\\.imagehost\\.org/ajax/\\d+/(.*?)\",true\\)").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<tr><th colspan=\"2\">(.*?)</th></tr>").getMatch(0);
                }
            }
            String filesize = br.getRegex("\"Download <a href=.*?</a> \\((.*?)\\)\"").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setDownloadSize(Regex.getSize(filesize));
            link.setName(filename.trim());
        } else {
            // Handling for direct (picture) links
            URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
            if (con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            int filesize = con.getContentLength();
            if (filesize == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = getFileNameFromHeader(con);
            link.setDownloadSize(filesize);
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = null;
        if (downloadLink.getDownloadURL().contains("/download/")) {
            // Handling for normal (file) links
            String postlink = downloadLink.getDownloadURL().replace("/download/", "/ajax/");
            String poststuff = "a=ajax&rand=" + new Regex(Math.random() * 100000, "(\\d+)").getMatch(0);
            br.postPage(postlink, poststuff);
            dllink = br.getRegex("<json>(.*?)</json>").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = Encoding.htmlDecode(dllink.trim());
            dllink = dllink.replaceAll("(\\\\|\")", "");
        } else {
            // Handling for direct (picture) links
            dllink = downloadLink.getDownloadURL();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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