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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "spankwire.com" }, urls = { "https?://(?:www\\.)?spankwire\\.com/(?:.*?/video\\d+|EmbedPlayer\\.aspx/?\\?ArticleId=\\d+)" })
public class SpankWireCom extends PluginForHost {

    public String dllink = null;

    public SpankWireCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.spankwire.com/Terms.aspx";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    // main code by external user "hpdub33"
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://spankwire.com/", "performance_timing", "video");
        br.setCookie("http://spankwire.com/", "Tablet_False", "");
        br.setCookie("http://spankwire.com/", "init", "Straight^^false^false^^None");
        br.setCookie("http://spankwire.com/", "adultd", "1");

        br.getPage(downloadLink.getDownloadURL());
        // Invalid link
        if (br.getURL().equals("http://www.spankwire.com/") || br.containsHTML("(removedCopyright|removedTOS|removedDefault)\\.jpg")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Link offline
        if (br.containsHTML(">This (article|video) has been (deleted|disabled)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = null;
        if (!downloadLink.getDownloadURL().contains("EmbedPlayer.aspx")) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- Spankwire\\.com\\s*?</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<meta name=\"Description\" content=\"Watch (.*?) on Spankwire now\\!").getMatch(0);
                }
            }
        } else {
            if (br.containsHTML("This video is temporarily unavailable")) {
                downloadLink.getLinkStatus().setStatusText("This video is temporarily unavailable!");
                downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
                return AvailableStatus.TRUE;
            }
            if (this.br.containsHTML("playerData\\.articleTitle\\s+=\\s+null")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("playerData.articleTitle\\s+= (\'|\")([^\']*?)(\'|\")").getMatch(1);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename.trim());
                filename = filename.replaceAll("\\+", " ");
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename.trim());
        // File not found can have good name
        if (br.containsHTML("playerData\\.isVideoUnavailable\\s*=\\s*true;")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = finddllink();
        if (dllink == null) {
            if (br.containsHTML("playerData.cdnPath180\\s+= encodeURIComponent\\(''\\)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        if (dllink.contains(".mp4")) {
            downloadLink.setFinalFileName(filename + ".mp4");
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        final URLConnectionAdapter con = br2.openHeadConnection(dllink);
        if (!con.getContentType().contains("html")) {
            downloadLink.setDownloadSize(con.getLongContentLength());
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains("EmbedPlayer.aspx") && br.containsHTML("This video is temporarily unavailable")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This video is temporarily unavailable!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String finddllink() throws PluginException {
        final String[] qualities = { "720", "480", "240", "180", "144" };
        final int count_max = qualities.length;
        int count_offline = 0;
        int count_notfound = 0;
        String dllink = null;
        String dllink_plain = null;
        for (final String quality : qualities) {
            dllink_plain = br.getRegex("cdnPath" + quality + "[^<>\"]*?(\"|\')([^<>\"\\']*?)(\"|\')").getMatch(1);
            if (dllink_plain == null) {
                count_notfound++;
                continue;
            }
            if (dllink_plain.equals("")) {
                count_offline++;
                continue;
            }
            if (dllink_plain.matches("(?-i)(?:https?:)?[^<>\"\\']*?\\.mp4[^<>\"\\']*?")) {
                dllink = dllink_plain;
                break;
            }
        }
        if (dllink == null) {
            if (count_offline == count_max) {
                /* No downloadlink available --> Video is not streamable --> Offline ?! */
                /* E.g. http://www.spankwire.com/More-Teenager-Girls-On-Porn-Load/video1888571/ */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (count_offline > count_notfound) {
                /* E.g. http://www.spankwire.com/More-Teenager-Girls-On-Porn-Load/video1888571/ */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (dllink != null) {
            dllink = dllink.replace("\\", "");
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}