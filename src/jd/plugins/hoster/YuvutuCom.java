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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yuvutu.com" }, urls = { "http://(www\\.)?yuvutu.com/(video/\\d+(?:/[A-Za-z0-9\\-_]+)?|modules\\.php\\?name=Video\\&op=view\\&video_id=\\d+)" })
public class YuvutuCom extends PluginForHost {
    public String dllink = null;

    public YuvutuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yuvutu.com/modules.php?name=Video&op=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String linkid = this.getLinkid(downloadLink);
        downloadLink.setName(linkid + ".mp4");
        br.setCookie("http://www.yuvutu.com/", "lang", "english");
        br.setCookie("http://www.yuvutu.com/", "warningcookie", "viewed");
        br.setFollowRedirects(false);
        final URLConnectionAdapter conf = br.openGetConnection("http://www.yuvutu.com/modules.php?name=Video&op=view&video_id=" + linkid);
        if (conf.getResponseCode() == 410) {
            conf.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        conf.disconnect();
        // Link offline
        if (br.containsHTML(">The video you requested does not exist<|video_noexist\\.png\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // private file (login needed, we dont support at this stage!)
        if (br.containsHTML(">This video has been marked as private by the uploader.</h2>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<span itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("<div class=\"video\\-title\\-content\">([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null) {
            filename = this.br.getRegex("class=\"video\\-title-content\"><h1[^>]+>([^<>\"]+)<").getMatch(0);
        }
        if (filename == null) {
            filename = this.getLinkid(downloadLink);
        }
        /* 2021-02-24 embedlink is not correct anymore */
        // final String embedlink = br.getRegex("\"(/embed_video\\.php\\?uri=[^<>\"]*?)\"").getMatch(0);
        // if (embedlink == null || filename == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // br.getPage("http://www.yuvutu.com" + embedlink);
        dllink = br.getRegex("file\\s*?:\\s*?\"(http[^<>\"]+)").getMatch(0);
        if (filename == null || dllink == null) {
            if (!br.containsHTML("player\\.swf")) {
                /* Invalid link */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = Encoding.htmlDecode(filename.trim());
        String ext = new Regex(dllink, ".+(\\..{2,5})$").getMatch(0);
        if (ext == null) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(filename + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLinkid(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(?:video/|video_id=)(\\d+)").getMatch(0);
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