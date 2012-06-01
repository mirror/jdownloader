//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision: 16078 $", interfaceVersion = 2, names = { "gigafront.de" }, urls = { "http://(www\\.)?gigafront\\.de/downloads,id\\d+,.*?\\.html" }, flags = { 0 })
public class GigaFrontDe extends PluginForHost {

    public GigaFrontDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gigafront.de/contact.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL() + "?guest=1");
        String dllink = br.getRegex("class=\"gast_download\" href=\"(misc.*?)\">").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(misc\\.php\\?action=downloadfile\\&amp;id=\\d+\\&amp;sechash=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://www.gigafront.de/" + Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Das angeforderte Dokument konnte nicht gefunden werden|<title>apexx \\- CMS \\&amp; Portalsystem \\| Information</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"download_detail_title\">Name:</div>[\t\n\r ]+<div class=\"download_detail_value\">[\t\n\r ]+<span class=\"download_detail_blue_text\">([^<>\"\\'/]+)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title> Download: ([^<>\"\\'/]+) \\- GIGAFRONT</title>").getMatch(0);
        String filesize = br.getRegex("class=\"download_detail_title\">Dateigröße:</div>[\t\n\r ]+<div class=\"download_detail_value\">[\t\n\r ]+<span class=\"download_detail_span\">([^<>\"\\'/]+)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}