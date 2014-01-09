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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "watchfreeinhd.com" }, urls = { "http://(www\\.)?watchfreeinhd\\.(com|org)/[A-Za-z0-9]+" }, flags = { 0 })
public class WatchFreeInHdCom extends PluginForHost {

    public WatchFreeInHdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.watchfreeinhd.com/tos.html";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("watchfreeinhd.org/", "watchfreeinhd.com/"));
    }

    private boolean POSTAGAIN = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(link.getDownloadURL(), "agree=Yes%2C+let+me+watchf");
        if (br.containsHTML("<strong>Error:</strong>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("<b>Video size:</b>([^<>\"]*?)</div>").getMatch(0);
        if (filesize != null)
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        else
            POSTAGAIN = true;
        link.setFinalFileName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (POSTAGAIN) br.postPage(downloadLink.getDownloadURL(), "agree=Yes%2C+let+me+watchf");
        String dllink = br.getRegex("\"(http://cdn\\.watchfreeinhd\\.com:\\d+/flv/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<div id=\"playerHolder\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
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