//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vids.myspace.com" }, urls = { "https?://(www\\.)?(myspace\\.com/(([a-z0-9\\-_]+/)?video/[a-z0-9\\-_]+/\\d+|[a-z0-9\\-_]+/music/song/[a-z0-9\\-_\\.]+)|mediaservices\\.myspace\\.com/services/media/embed\\.aspx/m=\\d+)" }, flags = { 0 })
public class VidsMySpaceCom extends PluginForHost {

    private static final String SONGURL = "https?://(www\\.)?myspace\\.com/[a-z0-9\\-_]+/music/song/[a-z0-9\\-_\\.]+";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // correction of old embded link format.
        String[] movuid = new Regex(link.getDownloadURL(), "(https?).+embed\\.aspx/m=(\\d+)").getRow(0);
        if (movuid != null && movuid.length == 2) link.setUrlDownload(movuid[0] + "://myspace.com/video/" + movuid[1]);
    }

    public VidsMySpaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.myspace.com/index.cfm?fuseaction=misc.terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            // This usually only happens for embed links
            if (con.getResponseCode() == 500) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.getURL().contains("myspace.com/error")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        if (link.getDownloadURL().matches(SONGURL)) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".m4a");
        } else {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+) Video by[^\"]+\"").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        // Plugin broken
        if (true) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dlurl = null;
        if (link.getDownloadURL().matches(SONGURL)) {
            dlurl = br.getRegex("data\\-stream\\-url=\"(rtmp[^<>\"]*?)\"").getMatch(0);
        } else {
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("https://myspace.com/ajax/streamurl", "mediaType=video&mediaId=" + new Regex(link.getDownloadURL(), "(\\d+)").getMatch(0));
            dlurl = br.getRegex("\"(rtmp[^<>\"]*?)\"").getMatch(0);
        }
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlurl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
    public void resetPluginGlobals() {
    }
}