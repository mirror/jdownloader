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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asfile.com" }, urls = { "http://(www\\.)?asfile\\.com/file/[A-Za-z0-9]+" }, flags = { 0 })
public class AsFileCom extends PluginForHost {

    public AsFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://asfile.com/en/page/offer";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>ASfile\\.com</title>|>Page not found<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("Download: <strong>([^<>\"\\']+)</strong><br/> \\(([^<>\"\\']+)\\)");
        String filename = br.getRegex("<meta name=\"title\" content=\"Free download ([^<>\"\\']+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Free download ([^<>\"\\']+)</title>").getMatch(0);
            if (filename == null) {
                filename = fileInfo.getMatch(0);
            }
        }
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("/free\\-download/")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String fileID = new Regex(downloadLink.getDownloadURL(), "asfile\\.com/file/(.+)").getMatch(0);
        br.getPage("http://asfile.com/en/free-download/file/" + fileID);
        final String hash = br.getRegex("hash: \\'([a-z0-9]+)\\'").getMatch(0);
        final String storage = br.getRegex("storage: \\'([^<>\"\\']+)\\'").getMatch(0);
        if (hash == null || storage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String waittime = br.getRegex("class=\"orange\">(\\d+)</span>").getMatch(0);
        int wait = 60;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://asfile.com/en/index/convertHashToLink", "hash=" + hash + "&path=" + fileID + "&storage=" + Encoding.urlEncode(storage) + "&name=" + Encoding.urlEncode(downloadLink.getName()));
        final String correctedBR = br.toString().replace("\\", "");
        String dllink = new Regex(correctedBR, "\"url\":\"(http:[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://s\\d+\\.asfile\\.com/file/free/[a-z0-9]+/\\d+/[A-Za-z0-9]+/[^<>\"\\'/]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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