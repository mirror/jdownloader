//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de" }, urls = { "https?://cdn\\d+\\.spiegel\\.de/images/image[^<>\"/]+|https?://(?:www\\.)?spiegel\\.de/video/(?:embedurl/)?[a-z0-9\\-_]*?video\\-[a-z0-9\\-_]*?\\.html" })
public class SpiegelDe extends PluginForHost {
    private final Pattern pattern_supported_image = Pattern.compile("https?://cdn\\d+\\.spiegel\\.de/images/image[^<>\"/]+");
    private String        DLLINK                  = null;

    public SpiegelDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.spiegel.de/agb";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        /* Offline urls should also have nice filenames! */
        String filename_url = getURLFilename(link);
        link.setName(filename_url);
        String filename = null;
        DLLINK = link.getDownloadURL();
        /* Prefer filenames set in decrypter in case user added a complete gallery. */
        filename = link.getStringProperty("decryptedfilename", null);
        if (filename == null) {
            filename = new Regex(DLLINK, "/images/(.+)").getMatch(0);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(DLLINK);
            if (!this.looksLikeDownloadableContent(con)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    private String getURLFilename(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "spiegel\\.de/images/(image[^<>\"/]+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    public void handleFree(final DownloadLink link) throws Exception {
        this.requestFileInformation(link);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, DLLINK, false, 1);
        this.dl.startDownload();
    }

    /* Prevent multihoster download as it makes absolutely no sense at all for all SPIEGEL services! */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    public void reset() {
    }

    public void resetDownloadlink(final DownloadLink link) {
    }
}