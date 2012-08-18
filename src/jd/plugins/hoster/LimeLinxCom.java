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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "limelinx.com" }, urls = { "http://[\\w\\.]*?limelinx\\.com/(files/)?[0-9a-z]+" }, flags = { 0 })
public class LimeLinxCom extends PluginForHost {

    public LimeLinxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://limelinx.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("DownloadLI.*?downloadFile\\('(.*?)'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("DownloadLI.*?download_file\\('(.*?)'").getMatch(0);
        }
        if (dllink != null) {
            if (!dllink.startsWith("http") && dllink.endsWith("ptth")) {
                dllink = new StringBuilder(dllink).reverse().toString();
            }
        }
        if (dllink == null) dllink = br.getRegex("<a href=\"(https?://[^\"\\'<>]+)\">Download</a>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // dllink is encrypted
        String encodedLink = Encoding.Base64Decode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, encodedLink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(File Not Found|The file you were looking for could not be found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Ready to download <b>(.*?)</b>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>File Name: <b>(.*?)</b></h1>").getMatch(0);
            if (filename == null) filename = br.getRegex("Sampling file <b>(.*?)</b>").getMatch(0);
        }
        String filesize = br.getRegex("File Size: <b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
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