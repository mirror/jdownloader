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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadingit.com" }, urls = { "http://[\\w\\.]*?uploadingit\\.com/(d/[A-Z0-9]{16}|file/[a-zA-Z0-9]+/.+)" }, flags = { 0 })
public class UploadingItCom extends PluginForHost {

    public UploadingItCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadingit.com/help/termsofservice";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>Invalid Download - Uploadingit</title>|\">Sorry, but according to our database the download link you have entered is not valid\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Oh Snap! File Not Found!<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"download_filename\">(.*?)</div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<td style=\"width:150px\">(.*?)</td>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Visit Uploadingit\\.com for free file hosting\\.\\&quot;\\&gt;Download (.*?) from Uploadingit\\.com\\&lt;/a\\&gt;\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"downloadTitle\">(.*?)<").getMatch(0);
                }
                if (filename == null) {
                    filename = br.getRegex("<title>(Downloading: )?(.*?) - Uploadingit</title>").getMatch(1);
                }
            }
        }
        String filesize = br.getRegex(" class=\"download_filesize\">\\((.*?)\\)</div>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"downloadSize\">\\((.*?)\\)<").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("downloadShareURL.*?value=\"(http:.*?)\"").getMatch(0);
        sleep(10 * 1000l, downloadLink);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(dllink);
        dllink = br.getRegex("\\('(file/download/.*?)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(10 * 1000l, downloadLink);
        dllink = "http://uploadingit.com/" + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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