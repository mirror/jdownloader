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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porntrex.com" }, urls = { "https?://(?:www\\.)?porntrex\\.com/video/\\d+/[a-z0-9\\-]+/?" })
public class PorntrexCom extends PluginForHost {
    public PorntrexCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "Preferred_format", FORMATS, "Preferred Format").setDefaultValue(0));
    }

    /* Connection stuff */
    private static final boolean  free_resume       = true;
    private static final int      free_maxchunks    = 0;
    private static final int      free_maxdownloads = -1;
    private String                dllink            = null;
    private boolean               server_issues     = false;
    private static final String[] FORMATS           = new String[] { "Best available", "720p", "480p", "360p" };

    @Override
    public String getAGBLink() {
        return "https://www.Porntrex.com/support/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Sorry, this video was deleted|a private video")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)/?$").getMatch(0).replace("-", " ");
        String filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        link.setName(filename);
        final SubConfiguration cfg = getPluginConfig();
        final int Preferred_format = cfg.getIntegerProperty("Preferred_format", 0);
        logger.info("Debug info: Preferred_format: " + Preferred_format);
        /* Preferred_format 0 = best, 1 = 720p, 2 = 480p, 3 = 360p */
        String pf = "";
        switch (Preferred_format) {
        case 0:
            pf = "best";
            break;
        case 1:
            pf = "720p";
            break;
        case 2:
            pf = "480p";
            break;
        case 3:
            pf = "360p";
            break;
        default:
            pf = "best";
        }
        logger.info("Debug info: Preferred_format: " + pf);
        String vqs[] = { "720p", "480p", "360p" };
        for (final String vq : vqs) {
            dllink = br.getRegex("video[^']+url\\d?:\\s*'(http[^']+)'\\,[^\\,]+" + vq).getMatch(0);
            logger.info("Debug info: Preferred_format: " + pf + ", checking format: " + vq + " ,dllink: " + dllink);
            if (dllink != null && pf == "best" || pf == "720p" && vq.equals("720p") || pf == "480p" && vq.equals("480p") || pf == "360p" && vq.equals("360p")) {
                logger.info("Debug info: checking dllink: " + dllink);
                checkDllink(link);
                if (dllink == null) {
                    continue;
                } else {
                    logger.info("Debug info: Preferred_format " + pf + ", found: " + vq);
                    break;
                }
            }
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final DownloadLink link) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                dllink = null;
            }
        } catch (final Exception e) { // Connection problem
            dllink = null;
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
