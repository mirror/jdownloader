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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "localhostr.com" }, urls = { "http://(www\\.)?(localhostr\\.com|lh\\.rs)/[A-Za-z0-9]+" }, flags = { 0 })
public class LocalHostrCom extends PluginForHost {

    public LocalHostrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://localhostr.com/terms/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("lh.rs/", "localhostr.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404<|>File not found|>We can\\'t find the file you\\'re looking for)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fInfo = br.getRegex("<div id=\"download\\-left\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>[\t\n\r ]+<h3>([^<>\"]*?)</h3>");
        final Regex gifLinkRegex = br.getRegex("<h1>(.*?)<small style=\"float:right\"> Size  \\d+x\\d+ / (.*?) / \\d+ Views</small></h1>");
        String filename = br.getRegex("<title>([^<>\"]*?) \\- Localhostr</title>").getMatch(0);
        if (filename == null) {
            filename = fInfo.getMatch(0);
            if (filename == null) filename = gifLinkRegex.getMatch(0);
        }
        String filesize = fInfo.getMatch(1);
        if (filesize == null) filesize = gifLinkRegex.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("\"(/file/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://localhostr.com" + dllink;
        if (br.containsHTML("I know the risk")) {
            dllink = dllink + "?agree=on";
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