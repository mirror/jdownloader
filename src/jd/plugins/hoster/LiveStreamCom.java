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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livestream.com" }, urls = { "https?://(www\\.)?livestream\\.com/[^<>\"]+/videos/\\d+" })
public class LiveStreamCom extends PluginForHost {

    @SuppressWarnings("deprecation")
    public LiveStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https + http available
    // other:

    /* Example main: http://livestream.com/cnet/LG/videos/85498136 */
    /*
     * http:
     * http://pdvod.new.livestream.com/events/00000000003cfce1/893bc539-0412-415a-b0be-438b5f81b086_678.mp4?start=614&end=3454&__gda__=
     * 1430264265_cd1870f0b469d93ce3955d4bd400e203
     */
    /*
     * akamai HD streaming: http://api.new.livestream.com/accounts/687825/events/3996897/videos/85498136.secure.smil -->
     * https://livestreamvod-f.akamaihd.net/events/00000000003cfce1/893bc539-0412-415a-b0be-438b5f81b086_2320.mp4
     */
    /* hls: http://api.new.livestream.com/accounts/687825/events/3996897/videos/85498136.m3u8 */
    /*
     * Thumbnail url (also containing important info):
     * http://img.new.livestream.com/events/00000000003cfce1/893bc539-0412-415a-b0be-438b5f81b086_1320.jpg
     */

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://livestream.com/terms";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes", "unused" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        String filename = null;
        dllink = null;
        final String lid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        final long llidlong = Long.parseLong(lid);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("window\\.config[\t\n\r ]*?=[\t\n\r ]*?(\\{.+);</script>").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        entries = (LinkedHashMap<String, Object>) entries.get("event");
        entries = (LinkedHashMap<String, Object>) entries.get("feed");
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("data");
        for (final Object reso : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) reso;
            final String type = (String) entries.get("type");
            if (type == null || !type.equals("video")) {
                continue;
            }
            entries = (LinkedHashMap<String, Object>) entries.get("data");
            final long tempid = JavaScriptEngineFactory.toLong(entries.get("id"), -1);
            if (tempid == llidlong) {
                if (isJDStable()) {
                    /* http */
                    dllink = (String) entries.get("progressive_url");
                } else {
                    /* https */
                    dllink = (String) entries.get("secure_progressive_url");
                }
                filename = (String) entries.get("caption");
                break;
            }
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = lid + "_" + filename;
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = openConnection(br2, dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html") && con.isOK()) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
