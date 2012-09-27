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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drop.st" }, urls = { "http://(www\\.)?drop\\.st/[A-Za-z0-9]+" }, flags = { 0 })
public class DropSt extends PluginForHost {

    public DropSt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://drop.st/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Upload not found<|>Sorry, we couldn\\'t find the upload you are looking for|>It might got deleted by its uploader or due to a breach|<title>drop something / drop\\.st</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("class=\"label left\"><strong>([^<>\"\\']+)</strong><span>([^<>\"\\']+)</span>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"\\']+) / drop\\.st</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("class=\"center\"><form action=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://s\\d+\\.drop\\.st/download/[A-Za-z0-9_\\-]+/[A-Za-z0-9_\\-]+/[A-Za-z0-9_\\-]+)\"").getMatch(0);
        // for image links
        if (dllink == null) {
            dllink = br.getRegex("\"(http://(www\\.)?s\\d+.drop\\.st/images/[A-Za-z0-9]+/large/[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) dllink = dllink.replace("/large/", "/original/");
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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