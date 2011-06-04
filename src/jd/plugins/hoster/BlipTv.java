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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * @author typek_pb
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blip.tv" }, urls = { "http://(\\w+\\.)?blip\\.tv/(file/\\d+(/)?|[\\p{L}\\w-%]+/[\\p{L}\\w-%]+)" }, flags = { 0 })
public class BlipTv extends PluginForHost {

    private String dlink = null;

    public BlipTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://blip.tv/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        /* 0.95xx comp */
        if (br.getRedirectLocation() != null) {
            // deutsche Umlaute fuehren in der 0.95xx zu einem redirect loop!
            br.getPage(br.getRedirectLocation().replaceAll("%83%C2", ""));
        }
        String id = br.getRegex("data-posts-id=\"(\\d+)").getMatch(0);
        if (id == null) {
            id = br.getRegex("\tdata-episode=\"(\\d+)").getMatch(0);
        }
        if (id == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>(.*?)\\s\\|").getMatch(0);
        br.getPage("http://blip.tv/rss/flash/" + id);
        if (filename == null) {
            filename = br.getRegex("<item>.*?<title>(.*?)</title>").getMatch(0);
        }
        dlink = br.getRegex("<enclosure url=\"(.*?)\"").getMatch(0);
        if (dlink == null) {
            dlink = br.getRegex("<media:content url=\"(.*?)\"").getMatch(0);
        }
        if (dlink == null || filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String ext = dlink.substring(dlink.lastIndexOf("."), dlink.length());
        ext = ext == null ? ".flv" : ext;
        if (filename.endsWith(".")) {
            filename = filename.substring(0, filename.length() - 1);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ext);
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
