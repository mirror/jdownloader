//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youporn.com" }, urls = { "http://[\\w\\.]*?youporn\\.com/watch/\\d+/?.+/?" }, flags = { 0 })
public class YouPornCom extends PluginForHost {

    String dlLink = null;

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(parameter.getDownloadURL(), "user_choice=Enter");
        String matches = br.getRegex("addVariable\\('file', encodeURIComponent\\('(.*?)'\\)\\);").getMatch(0);
        if (matches == null) matches = br.getRegex("var file_url.*?=.*?'(.*?)'").getMatch(0);
        String filename = br.getRegex("<title>(.*?)- Free Porn Videos - YouPorn.com Lite \\(BETA\\)</title>").getMatch(0);
        if (matches == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(matches);
        dlLink = br.getRegex("location>(http://.*?)<").getMatch(0);
        if (dlLink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename != null) parameter.setFinalFileName(filename.trim().replaceAll(" ", "-") + ".flv");
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dlLink = Encoding.htmlDecode(dlLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlLink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
