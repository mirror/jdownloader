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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/((divx|play)/[A-Z0-9]+|download/.+)" }, flags = { 0 })
public class DuckLoad extends PluginForHost {

    public DuckLoad(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL().replace("youload.to/", "duckload.com/");
        theLink = theLink.replace(".html", "").replace("/divx/", "/play/");
        if (!theLink.contains("www.")) theLink = theLink.replace("http://", "http://www.");
        link.setUrlDownload(theLink);
    }

    private static final String MAINPAGE = "http://duckload.com/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "dl_set_lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File not found\\.|download\\.notfound)") || !br.containsHTML("stream_wait_table_bottom")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) @ DuckLoad\\.com</title>").getMatch(0);
        // Server doesn't give us the correct name so we set it here
        if (filename != null) link.setFinalFileName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int waitThis = 20;
        if (!downloadLink.getDownloadURL().contains("/download/")) {
            waitThis = 10;
            String wait = br.getRegex("id=\"number\">(\\d+)</span> seconds").getMatch(0);
            if (wait != null) waitThis = Integer.parseInt(wait);
        }
        sleep(waitThis * 1001l, downloadLink);
        br.postPage(br.getURL(), "secret=&next=true");
        String dllink = br.getRegex("<param name=\"src\" value=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://dl\\d+\\.duckload\\.com/Get/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[A-Z0-9]+)\"").getMatch(0);
            if (dllink == null) {
                String part1 = br.getRegex("\\'ident=(.*?)\\&\\';").getMatch(0);
                String part2 = br.getRegex("\\'token=(.*?)\\&\\';").getMatch(0);
                String part3 = br.getRegex("\\'filename=(.*?)\\&\\';").getMatch(0);
                if (part1 == null || part2 == null || part3 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dllink = "http://www.duckload.com/api/as2/link/" + part1 + "/" + part2 + "/" + part3;
                int secondWait = 10;
                String secondWaitRegexed = br.getRegex("\\'timetowait=(\\d+)\\&\\'").getMatch(0);
                if (secondWaitRegexed != null) secondWait = Integer.parseInt(secondWaitRegexed);
                sleep(secondWait * 1001l, downloadLink);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL() != null && br.getURL().contains("/error/")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
