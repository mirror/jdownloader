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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tweetmymixtape.com", "tweetmysong.com" }, urls = { "^http://(www\\.)?tweetmymixtape\\.com/(?i-)[a-z0-9]{6}$", "^http://(www\\.)?tweetmysong\\.com/(?i-)[a-z0-9]{6}$" }, flags = { 0, 0 })
public class TweetMyMixTapeCom extends PluginForHost {

    public TweetMyMixTapeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tweetmymixtape.com/Terms.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("alt=\"retweet\" /></a>[\t\n\r ]+<a href=\"(/.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/admin/postback/(mixdownloader|songdownloader)\\.ashx\\?SongKey=[^\"\\'<>]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("file: \"(http://.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("file=(http://tweetmymixtape\\.com/admin/playvideo\\.aspx\\?.*?)\\&amp;image=").getMatch(0);
                }
            }
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
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/Song-Removed.htm") || br.containsHTML("(>Default Page Title<|>This song was removed by us or the artist|>Song Removed\\!<|>You will redirected to the homepage in|>If you are not redirected,)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("src=\"/frontpages/images/leftside\\.png\" alt=\"\" /><h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\" />").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) by ").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML("(/playvideo\\.aspx\\?videokey|>Loading the video player<)"))
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        else
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}