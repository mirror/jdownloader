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
import java.util.Random;

import jd.PluginWrapper;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "duckload.com" }, urls = { "http://[\\w\\.]*?(duckload\\.com|youload\\.to)/(download/\\d+/.+|(divx|play)/[A-Z0-9\\.-]+|[a-zA-Z0-9\\.]+)" }, flags = { 0 })
public class DuckLoad extends PluginForHost {

    private static final String MAINPAGE = "http://duckload.com/";

    public DuckLoad(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.requestFileInformation(downloadLink);
        int waitThis = 20;
        if (!downloadLink.getDownloadURL().contains("/download/")) {
            waitThis = 10;
            final String wait = this.br.getRegex("id=\"number\">(\\d+)</span> seconds").getMatch(0);
            if (wait != null) {
                waitThis = Integer.parseInt(wait);
            }
        }
        this.sleep(waitThis * 1001l, downloadLink);
        this.br.postPage(this.br.getURL(), "secret=&next=true");
        /* Cookie Begin */
        final Random rndCookie = new Random();
        final int rndX = rndCookie.nextInt(999999999 - 100000000) + 100000000;
        final int rndY = rndCookie.nextInt(99999999 - 10000000) + 10000000;
        final long ts = System.currentTimeMillis();
        this.br.setCookie(DuckLoad.MAINPAGE, "__utma", rndY + "." + rndX + "." + ts + "." + ts + "." + ts + ".1");
        this.br.setCookie(DuckLoad.MAINPAGE, "__utmb", rndY + ".7.10." + ts);
        this.br.setCookie(DuckLoad.MAINPAGE, "__utmc", "" + rndY + "");
        this.br.setCookie(DuckLoad.MAINPAGE, "__utmz", rndY + "." + ts + ".1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)");
        /* Cookie End */
        String dllink = this.br.getRegex("<param name=\"src\" value=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = this.br.getRegex("\"(http://dl\\d+\\.duckload\\.com/Get/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[A-Z0-9]+)\"").getMatch(0);
            /* swf-Download */
            if ((dllink == null) && this.br.containsHTML("duckloadplayer\\.swf")) {
                final String[] dl_id = this.br.getRequest().getUrl().getPath().split("/");
                final String appendLink = "undefined&showTopBar=undefined";
                dllink = "http://flash.duckload.com/video/video_api.php?id=" + dl_id[dl_id.length - 1] + "&cookie=";
                this.br.getPage(dllink + appendLink);
                final String part1 = this.br.getRegex("ident\":\\ \"(.*?)\",").getMatch(0);
                final String part2 = this.br.getRegex("link\":\\ \"(.*?)\"").getMatch(0).replace("\\/", "/");
                dllink = "http://dl" + part1 + ".duckload.com" + part2;
                if ((part1 == null) || (part2 == null)) {
                    dllink = null;
                }
            } else {
                final String part1 = this.br.getRegex("\\'ident=(.*?)\\';").getMatch(0);
                final String part2 = this.br.getRegex("\\'token=(.*?)\\&\\';").getMatch(0);
                final String part3 = this.br.getRegex("\\'\\&filename=(.*?)\\&\\';").getMatch(0);
                if ((part1 == null) || (part2 == null) || (part3 == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                dllink = "http://www.duckload.com/api/as2/link/" + part1 + "/" + part2 + "/" + part3;
                int secondWait = 10;
                final String secondWaitRegexed = this.br.getRegex("\\'timetowait=(\\d+)\\&\\'").getMatch(0);
                if (secondWaitRegexed != null) {
                    secondWait = Integer.parseInt(secondWaitRegexed);
                }
                this.sleep(secondWait * 1001l, downloadLink);
            }
        }
        this.dl = BrowserAdapter.openDownload(this.br, downloadLink, dllink, true, -2);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            if (((this.br.getURL() != null) && this.br.getURL().contains("/error/")) || this.br.containsHTML("ErrorCode: e983")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error"); }
            if (this.br.getRequest().getUrl().toString().contentEquals("http://" + this.br.getHost() + "/")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Redirect error"); }
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setCookie(DuckLoad.MAINPAGE, "dl_set_lang", "en");
        this.br.setFollowRedirects(true);
        this.br.getPage(link.getDownloadURL());
        if (this.br.containsHTML("(File not found\\.|download\\.notfound)") || !this.br.containsHTML("stream_wait_table_bottom")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = this.br.getRegex("<title>(.*?) @ DuckLoad\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = link.getName();
        }
        // Server doesn't give us the correct name so we set it here
        link.setFinalFileName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}
