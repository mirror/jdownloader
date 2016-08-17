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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "commons.wikimedia.org" }, urls = { "https?://commons\\.wikimedia\\.org/wiki/File:.+|https?://[a-z]{2}\\.wikipedia\\.org/wiki/([^/]+/media/)?[A-Za-z0-9]+:.+" }) 
public class CommonsWikimediaOrg extends PluginForHost {

    public CommonsWikimediaOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".jpg";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               DLLINK            = null;

    @Override
    public String getAGBLink() {
        return "https://wikimediafoundation.org/wiki/Terms_of_Use";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        long filesize = -1;
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * URL is different for every country e.g. https://en.wikipedia.org/wiki/File:Krak%C3%B3w_G%C5%82%C3%B3wny_(budynek_dworca).JPG,
         * https://pl.wikipedia.org/wiki/Plik:Dworzec_Krak%C3%B3w_G%C5%82%C3%B3wny.jpg
         */

        String filename = br.getRegex("\"wgTitle\":\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            String url_filename = new Regex(link.getDownloadURL(), "/wiki/(?:[^/]+/media/)?[A-Za-z0-9]+:(.+)").getMatch(0);
            if (url_filename == null) {
                url_filename = new Regex(link.getDownloadURL(), "/File:(.+)").getMatch(0);
            }
            filename = url_filename;
        }
        String filesize_str = this.br.getRegex("file size: (\\d+(?:\\.\\d{1,2})? [A-Za-z]+)").getMatch(0);
        if (filesize_str == null) {
            filesize_str = this.br.getRegex("(?-i)class=\"fileInfo\">[^<]*?(\\d+(\\.\\d+)?\\s*[KMGT]B)").getMatch(0);
        }
        DLLINK = br.getRegex("id=\"file\"><a href=\"(http[^<>\"]*?)\"").getMatch(0);
        if (DLLINK == null) {
            /* E.g. https://commons.wikimedia.org/wiki/File:BBH_gravitational_lensing_of_gw150914.webm */
            DLLINK = br.getRegex("<a href=\"(https?://[^<>\"]+)\"[^<>]+>Original file</a>").getMatch(0);
        }
        if (DLLINK == null) {
            /* E.g. https://commons.wikimedia.org/wiki/File:Sintel_movie_720x306.ogv */
            DLLINK = br.getRegex("<source src=\"(https?://[^<>\"]+)\"[^<>]+data\\-title=\"Original").getMatch(0);
        }
        if (DLLINK == null) {
            /* E.g. https://zh.wikipedia.org/wiki/File:%E9%84%AD%E7%A7%80%E6%96%87_%E5%8E%BB%E6%84%9B%E5%90%A7.jpg */
            DLLINK = br.getRegex("\"fullImageLink\"\\s*id=\"file\"><a href=\"((https?)?:?//.*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!DLLINK.startsWith("http")) {
            DLLINK = URLHelper.parseLocation(br._getURL(), DLLINK);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (filesize_str != null) {
            filesize = SizeFormatter.getSize(filesize_str);
        } else {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openHeadConnection(DLLINK);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                link.setProperty("directlink", DLLINK);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, free_resume, free_maxchunks);
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
