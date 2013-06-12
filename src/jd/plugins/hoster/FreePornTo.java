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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeporn.to" }, urls = { "http://(www\\.)?freeporn\\.to/movie/\\d+" }, flags = { 0 })
public class FreePornTo extends PluginForHost {

    private String DLLINK = null;

    public FreePornTo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.freeporn.to/";
    }

    private void getDllink() {
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        final String postRawData = new String(JDHexUtils.getByteArray("0000000000010019")) + "freepornmedia.getMediaFor" + new String(JDHexUtils.getByteArray("00022F31000000510A00000002020018")) + "897AFBC8734234234A93487F" + new String(new byte[] { 0x02, 0x00, (byte) DLLINK.length() }) + DLLINK;
        br.getHeaders().put("Content-Type", "application/x-amf");
        br.setFollowRedirects(false);

        try {
            br.postPageRaw("http://www.freeporn.to/amfgateway/gateway.php", postRawData);
        } catch (final Throwable e) {
            DLLINK = null;
        }
        DLLINK = DLLINK == null ? DLLINK : br.getRegex("(http://.*?)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>The requested movie couldn´t be found\\.<|<title>Free Porn Movies / Videos / Clips</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<div class=\"titletext\">\\d+\\.\\d+\\.\\d+ \\- \\d{2}:\\d{2} \\- (.*?)</div>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Free Porn Movies / Videos / Clips: (.*?)</title>").getMatch(0);
        }
        DLLINK = br.getRegex("addVariable\\(\"sfile\",\"(http.*?)\"\\);").getMatch(0);
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);

        getDllink();
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
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