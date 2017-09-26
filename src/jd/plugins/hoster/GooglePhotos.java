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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "photos.google.com" }, urls = { "https?://photos\\.google\\.com/share/[A-Za-z0-9\\-_]+/photo/[A-Za-z0-9\\-_]+\\?key=[A-Za-z0-9\\-_]+" })
public class GooglePhotos extends PluginForHost {
    public GooglePhotos(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* DEV NOTES */
    // Tags: Google Service
    // protocol: https
    // other:
    /** Settings stuff */
    public static final String FAST_LINKCHECK    = "FAST_LINKCHECK";
    /* Connection stuff */
    private boolean            free_resume       = true;
    private int                free_maxchunks    = 0;
    private final int          free_maxdownloads = -1;
    private String             dllink            = null;
    private boolean            serverissue       = false;

    @Override
    public String getAGBLink() {
        return "https://www.google.com/intl/de/policies/terms/";
    }

    /* TODO: Improve this, maybe download the video download, not the stream */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        serverissue = false;
        free_resume = true;
        free_maxchunks = 0;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url_title = new Regex(link.getDownloadURL(), "/photo/([A-Za-z0-9\\-_]+)").getMatch(0).replace("-", " ");
        link.setName(url_title);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean isvideo = false;
        boolean isVideoStreamDwnload = false;
        long filesize = 0;
        String filename = null;
        URLConnectionAdapter con = null;
        try {
            if (this.br.containsHTML("data\\-isvideo=\"true\"")) {
                try {
                    /*
                     * Special for videos to find the filename. If we access the 'wrong' videourl we'll get a .gifv filename - if we access
                     * the 'right' videourl we won't get a filename at all!
                     */
                    isvideo = true;
                    /* Try to get downloadlink */
                    dllink = br.getRegex("(https?://video\\.googleusercontent\\.com/[A-Za-z0-9\\-_]+)").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("(https?://video-downloads\\.googleusercontent\\.com/[A-Za-z0-9\\-_]+)").getMatch(0);
                    }
                    if (dllink == null) {
                        dllink = br.getRegex("u003d(https%3A%2F%2F.*?mp4)[^\",]*?(hd720|medium|small)(\"|,)").getMatch(0);
                        if (dllink != null) {
                            dllink = Encoding.urlDecode(Encoding.unicodeDecode(dllink), false);
                        } else {
                            /* Failed? Try to get streamlink! */
                            dllink = br.getRegex("data\\-media\\-key=\"[^<>\"]+\"data\\-url=\"(https[^<>\"]+)\"[^@]+data\\-isvideo=\"true\"").getMatch(0);
                        }
                        isVideoStreamDwnload = dllink != null;
                    }
                    if (dllink != null) {
                        con = br.openHeadConnection(dllink);
                        filename = getFileNameFromHeader(con);
                        if (isVideoStreamDwnload) {
                            filename = this.removeDoubleExtensions(filename, "mp4");
                            /* =m22?cpn=blablabla&c=WEB&cver=1.20160414 */
                            /* Correct videourl */
                            if (!dllink.contains("=m22")) {
                                dllink += "=m22?c=WEB";
                            }
                        } else {
                            /* Download of original file --> No workaround needed and filesize & final filename is known! */
                            filesize = con.getLongContentLength();
                            free_maxchunks = 1;
                            free_resume = false;
                        }
                    }
                } catch (final Throwable e) {
                }
            } else {
                /* For photos */
                dllink = br.getRegex("2\\][\t\n\r ]*?,\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink != null && (filesize == 0 || filename == null)) {
                if (con == null) {
                    con = br.openHeadConnection(dllink);
                }
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                    if (filename == null) {
                        filename = getFileNameFromHeader(con);
                    }
                    if (filename == null) {
                        filename = url_title;
                        if (isVideoStreamDwnload) {
                            filename += ".mp4";
                        } else if (!isvideo) {
                            filename += ".jpg";
                        } else {
                            /* Video download with unknown extension. */
                        }
                    } else if (isVideoStreamDwnload && !filename.endsWith(".mp4")) {
                        filename = url_title + ".mp4";
                    }
                    link.setProperty("directlink", dllink);
                } else {
                    serverissue = true;
                }
            }
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (serverissue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
        }
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

    /**
     * Removes double extensions (of video hosts) to correct ugly filenames such as 'some_videoname.mkv.flv.mp4'.<br />
     *
     * @param filename
     *            input filename whose extensions will be replaced by parameter defaultExtension.
     * @param defaultExtension
     *            Extension which is supposed to replace the (multiple) wrong extension(s).
     */
    private String removeDoubleExtensions(String filename, final String defaultExtension) {
        if (filename == null || defaultExtension == null) {
            return null;
        }
        String ext_temp = null;
        int index = 0;
        while (filename.contains(".")) {
            /* First let's remove all common video extensions */
            index = filename.lastIndexOf(".");
            ext_temp = filename.substring(index);
            if (ext_temp != null && ext_temp.matches("\\.(avi|divx|flv|mkv|mov|mp4|gifv|gif)")) {
                filename = filename.substring(0, index);
                continue;
            }
            break;
        }
        /* Add desired video extension */
        if (!filename.endsWith("." + defaultExtension)) {
            filename += "." + defaultExtension;
        }
        return filename;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, JDL.L("plugins.hoster.GooglePhotos.FastLinkcheck", "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.")).setDefaultValue(false));
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
