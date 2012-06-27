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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ugoupload.net" }, urls = { "http://(www\\.)?ugoupload\\.net/[A-Za-z0-9]+" }, flags = { 0 })
public class UGoUploadNet extends PluginForHost {

    public UGoUploadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ugoupload.net/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("ugoupload.net/index.html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[][] fileInfo = br.getRegex("<th class=\"descr\">[\t\n\r ]+<strong>[\r\n\t ]+(.+)\\(([\\d\\.]+ (KB|MB|GB))\\)<br/>").getMatches();
        if (fileInfo == null || fileInfo.length == 0) {
            fileInfo = br.getRegex("(.*?) \\(([\\d\\.]+ (KB|MB|GB))\\)").getMatches();
            if ((fileInfo == null || fileInfo.length == 0) || fileInfo[0][0] == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        }
        String filename = fileInfo[0][0];
        String filesize = null;
        if (fileInfo[0][1] != null) filesize = fileInfo[0][1];
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(http://[^<>\"\\']*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://(www\\.)?ugoupload\\.net/[A-Za-z0-9]+/[^<>\"]*?\\?d=1)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 250;
        final String waittime = br.getRegex("var seconds = (\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
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