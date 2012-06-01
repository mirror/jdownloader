//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision: 15431 $", interfaceVersion = 2, names = { "video.msn.com" }, urls = { "http://(www\\.)?video\\.de\\.msn\\.com/watch/video/[a-z0-9\\-]+/[a-z0-9]+" }, flags = { 0 })
public class VideoMsnCom extends PluginForHost {

    private String DLLINK = null;

    public VideoMsnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decodeHex(String s) {
        final String[] damnHex = new Regex(s, "\\\\x(.{2})").getColumn(0);
        if (damnHex != null) {
            for (String hex : damnHex) {
                char chr = (char) Integer.parseInt(hex, 16);
                s = s.replace(hex, String.valueOf(chr));
            }
        }
        return s;
    }

    @Override
    public String getAGBLink() {
        /** No correct TOSlink found */
        return "http://de.msn.com/impressum.aspx";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) { return true; }
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            if (isStableEnviroment()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Developer Version of JD needed!"); }
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setUrl(DLLINK);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://video.de.msn.com/", "zip", "c:de");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Die angeforderte Seite wurde nicht gefunden|>Die von Ihnen angeforderte Webseite existiert|um zur Startseite zurückzukehren\\.</h3>|<title>Fehler</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("title: \\'(.*?)\\'").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\\'vxpInfoPanelTitle\\'>(.*?)</div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"title\" content=\"(.*?) auf MSN Video\"").getMatch(0);
            }
        }
        DLLINK = br.getRegex("MediaFiles: \\[\\'\\', \\'((http|rtmp).*?)\\'\\]").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = decodeHex(DLLINK);
        DLLINK = DLLINK.replace("\\x", "");
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".flv";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        if (DLLINK.startsWith("http")) {
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) downloadLink.setDownloadSize(con.getLongContentLength());
                else
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            // rtmp
            return AvailableStatus.TRUE;
        }
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