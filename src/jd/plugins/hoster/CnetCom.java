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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cnet.com" }, urls = { "http://(www\\.)?download\\.cnet\\.com/[A-Za-z0-9\\-_]+/[^<>\"/]*?\\.html" }, flags = { 0 })
public class CnetCom extends PluginForHost {

    public CnetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://download.cnet.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Whoops\\! You broke the Internet\\!<|>No, really,  it looks like you clicked on a borked link)") || br.getURL().contains("/most-popular/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // External mirrors are of course not supported
        if (br.containsHTML(">Visit Site<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- CNET Download\\.com</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("\\&fileName=([^<>\"]*?)(\\'|\")").getMatch(0);
        String filesize = br.getRegex(">File size:</span>([^<>\"]*?)</li>").getMatch(0);
        if (filesize == null) filesize = br.getRegex(">File Size:</span> <span>([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML("class=\"dlNowCTA\">Visit Site</span>")) link.getLinkStatus().setStatusText("Not downloadable (external download, see browser)");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // Maybe we're already on the download page
        String dllink = br.getRegex("\\'(http://software\\-files\\-[a-z0-9]+\\.cnet\\.com/s/software/[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) {
            String continueLink = br.getRegex("class=\"downloadNow\"> <a href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (continueLink == null) continueLink = br.getRegex("\"(http://(www\\.)?dw\\.com\\.com/redir\\?[^<>\"]*?)\"").getMatch(0);
            if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(continueLink);
            dllink = br.getRegex("src:\\'(http[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(http://software\\-files\\-[a-z0-9]+\\.cnet\\.com/s/software/[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
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