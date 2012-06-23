//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filetolink.com" }, urls = { "http://(www\\.)?filetolink\\.com/([a-z0-9]+|d/\\?h=[a-z0-9]{32}\\&t=\\d{10}\\&f=[a-z0-9]{8})" }, flags = { 0 })
public class FileToLinkCom extends PluginForHost {

    private String DLLINK = null;

    public FileToLinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filetolink.com/contact.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        System.out.println(br.toString() + "\n");
        System.out.println(br.toString() + "\n");
        if (br.containsHTML(">Sorry, this file does not exist\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>[a-z0-9_\\-]+/([^<>\"]*?) : File Sharing \\- Upload and Send big Files : FileToLink</title>").getMatch(0);
        String filesize = br.getRegex("Size:</td><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String finallink = br.getRegex("<META HTTP\\-EQUIV=\"Refresh\" CONTENT=\"0\\; URL=(/download/\\?h=[0-9a-z]+\\&t=\\d+\\&f=[0-9a-z]+)\"/>\\'").getMatch(0);
        if (finallink != null) {
            finallink = new Regex(br.getURL(), "(https?://.*\\.com)/.*").getMatch(0) + finallink;
        } else {
            // Maybe facebook login required, let's skip that shit
            final Regex noFB = br.getRegex("\\&redirect_url=http%3A%2F%2F(www\\.)?filetolink\\.com%2Fd%2F%3Fh%3D([a-z0-9]+)%26t%3D(\\d+)%26f%3D([a-z0-9]+)\"");
            if (noFB.getMatches().length < 2) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            finallink = "http://www.filetolink.com/download/?h=" + noFB.getMatch(1) + "&t=" + noFB.getMatch(2) + "&f=" + noFB.getMatch(3);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}