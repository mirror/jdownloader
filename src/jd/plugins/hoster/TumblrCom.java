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
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tumblr.com" }, urls = { "http://[\\w\\.\\-]*?tumblrdecrypted\\.com/post/\\d+" }, flags = { 0 })
public class TumblrCom extends PluginForHost {

    private String dllink = null;

    public TumblrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tumblr.com/terms_of_service";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("tumblrdecrypted.com/", "tumblr.com/"));
    }

    private void getDllink() throws IOException {
        br.setFollowRedirects(false);
        dllink = br.getRegex("\"><img src=\"(( +)?http://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.jpg)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(( +)?http://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.(jpg|gif|png))\"").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String ADDITION = "P3BsZWFkPXBsZWFzZS1kb250LWRvd25sb2FkLXRoaXMtb3Itb3VyLWxhd3llcnMtd29udC1sZXQtdXMtaG9zdC1hdWRpbw==";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The URL you requested could not be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dllink = checkDirectLink(downloadLink, "audiodirectlink");
        if (dllink != null) {
            dllink += Encoding.Base64Decode(ADDITION);
        } else {
            String filename = downloadLink.getFinalFileName();
            if (filename == null) filename = new Regex(br.getURL(), "tumblr\\.com/post/\\d+/(.+)").getMatch(0);
            if (filename == null) filename = new Regex(downloadLink.getDownloadURL(), "tumblr\\.com/post/(\\d+)").getMatch(0);
            filename = filename.trim();
            if (br.containsHTML(">renderVideo\\(")) {
                dllink = br.getRegex("\\'(http://[^<>\"/]*?\\.tumblr\\.com/video_file/\\d+/[^<>\"/]*?)\\'").getMatch(0);
                downloadLink.setFinalFileName(filename + ".mp4");
            } else if (br.containsHTML("class=\"audio_player\"")) {
                dllink = br.getRegex("\\?audio_file=(http[^<>\"]*?)\\&color").getMatch(0);
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dllink = Encoding.htmlDecode(dllink.trim()) + Encoding.Base64Decode(ADDITION);
            } else {
                getDllink();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dllink = Encoding.htmlDecode(dllink.trim());
                String ext = dllink.substring(dllink.lastIndexOf("."));
                if (ext == null || ext.length() > 5) ext = ".mp3";
                downloadLink.setFinalFileName(filename + ext);
            }
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}