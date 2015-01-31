//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "auengine.com" }, urls = { "http://(www\\.)?auengine\\.(?:com|io)/(embed\\.php\\?file=.+|embed/[a-zA-Z0-9]+)" }, flags = { 0 })
public class AuEngineCom extends PluginForHost {

    // raztoki embed video player template.

    private String dllink = null;

    public AuEngineCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.auengine.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        // Offline links should also have nice filenames
        final String offlineFilename = new Regex(downloadLink.getDownloadURL(), "auengine\\.(?:com|io)/embed\\.php\\?file=(.+)").getMatch(0);
        if (offlineFilename != null) {
            downloadLink.setName(offlineFilename);
        }
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        dllink = br.getRegex("url: '(http[^']+auengine\\.(?:com|io)(%2F|/)videos[^']+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("video_link = '(http[^']+auengine\\.(?:com|io)[^']+)").getMatch(0);
            // this will preserve hd over sd.
            // NOTE: not all videos comply with 1080|720|etc they can be 710|381 https://svn.jdownloader.org/issues/62623
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'1\\d{3}p'").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'7\\d{2}p'").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'4\\d{2}p'").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'3\\d{2}p'").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'2\\d{2}p'").getMatch(0);
            }
            // non matching to \d{3}p
            if (dllink == null) {
                dllink = br.getRegex("file\\s*:\\s*'(http[^']+auengine\\.(?:com|io)[^']+)',\\s*label\\s*:\\s*'Video'").getMatch(0);
            }
        }
        if (filename == null || dllink == null) {
            if (br.containsHTML("<h1>Page not found</h1>|The page you are looking for does not exist\\.")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, false);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openGetConnection(dllink);
            } catch (final Throwable e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setFinalFileName(filename);
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