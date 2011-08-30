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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "doperoms.com" }, urls = { "http://(www\\.)?doperoms\\.com/roms/[^/]+/.*?\\.html" }, flags = { 0 })
public class DopeRomsCom extends PluginForHost {

    public DopeRomsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.doperoms.com/dmca.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", link.getDownloadURL());
        br.getPage("http://www.doperoms.com/set_language.php?lang=EN");
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://www.doperoms.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<H1>(.*?) <br/><font").getMatch(0);
        String filesize = br.getRegex("<br/>File Size: (.*?)<br/>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String md5 = br.getRegex(">MD5 Checksum: ([a-z0-9]+)<br/><br").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String continueLink = br.getRegex("<br/><br/><br/><br/><br/>[\t\n\r ]+<a href=\"(/.*?)\"").getMatch(0);
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.doperoms.com" + continueLink);
        String dllink = br.getRegex("<\\!\\-\\- End BidVertiser code \\-\\-> <br /><br /><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<b>Download</b><br/><a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://(www\\.)?doperoms\\.com/files/roms/[^/]+/GETFILE_.*?)\"").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">403 \\- Forbidden<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}