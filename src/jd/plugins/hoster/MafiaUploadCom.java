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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mafiaupload.com" }, urls = { "http://(www\\.)?mafiaupload\\.com/do\\.php\\?id=\\d+" }, flags = { 0 })
public class MafiaUploadCom extends PluginForHost {

    public MafiaUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mafiaupload.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>The file has been deleted by the owner of against DMCA|>Go back to MAFIAUPLOAD<|<title>MAFIAUPLOAD \\- Free Files Hosting</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\"title\">\\&#9679; (.*?) Download</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<td class=\"td\">File Name</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) Download \\&#9679; MAFIAUPLOAD \\- Free Files Hosting</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("<td class=\"td\">File Size</td>[\t\n\r ]+<td class=\"tddata\">(.*?)</td>").getMatch(0);
        if (filesize == null) filesize = br.getRegex(">Click here to download</a><br /><span>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String ext = br.getRegex("<td class=\"td\">File extension</td>[\t\n\r ]+<td class=\"tddata\">(.*?)</td>").getMatch(0);
        if (ext == null)
            link.setName(Encoding.htmlDecode(filename.trim()));
        else
            link.setName(Encoding.htmlDecode(filename.trim()) + "." + ext.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        sleep(3 * 1000l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://www.mafiaupload.com/do.php?down=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0), true, 0);
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