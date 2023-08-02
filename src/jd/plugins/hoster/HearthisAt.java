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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hearthis.at" }, urls = { "https?://(?:www\\.)?hearthis\\.at/([^/]+)/([A-Za-z0-9-]+)/?" })
public class HearthisAt extends PluginForHost {
    public HearthisAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private boolean   free_resume        = false;
    private int       free_maxchunks     = 1;
    private final int free_maxdownloads  = -1;
    private String    dllink             = null;
    private boolean   isOfficialDownload = false;

    @Override
    public String getAGBLink() {
        return "https://hearthis.at/nutzungsbedingungen/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        return urlinfo.getMatch(0) + " - " + urlinfo.getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String extDefault = ".mp3";
        dllink = null;
        if (!link.isNameSet()) {
            link.setName(getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
        final String url_title = urlinfo.getMatch(1);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.containsHTML("track\\-detail track_")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (StringUtils.containsIgnoreCase(br.getURL(), "/search/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // String filename = br.getRegex("data-playlist-title=\"([^<>\"]*?)\"").getMatch(0); // Always get "pause" (1st match)
        String title = br.getRegex("<title>([^<>]*?) \\|[^<>]+</title>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = urlinfo.getMatch(0) + " - " + url_title;
        }
        // final String downloadlink_stream = br.getRegex("data-mp3=\"(http[^<>\"]*?)\"").getMatch(0); // Always get dummy link (1st)
        String downloadlink_stream = null;
        String[] mp3Links = br.getRegex("data-mp3=\"(http[^<>\"]*?)\"").getColumn(0);
        for (String mp3Link : mp3Links) {
            if (!mp3Link.contains("/untitled/")) {
                downloadlink_stream = mp3Link;
                break;
            }
        }
        br.setFollowRedirects(true);
        if (dllink != null && (!dllink.contains(extDefault) && !dllink.contains("index.php"))) {
            dllink = null;
        }
        if (isDownload) {
            /* Doing this during linkcheck would take a lot of time! */
            String externalDownloadURL = br.getRegex("<a class=\"btn btn-external no-ajaxloader\"\\s*href=\"(https://[^\"]+)\"[^>]+ title=\"(?:FREE )?DOWNLOAD\"").getMatch(0);
            if (externalDownloadURL == null) {
                /* 2020-08-04: More wide open attempt e.g. doesn't matter what's the "name of their download button". */
                externalDownloadURL = br.getRegex("<a class=\"btn btn-external no-ajaxloader\"\\s*href=\"(https://[^\"]+)\"[^>]+ title=\"").getMatch(0);
            }
            if (externalDownloadURL != null) {
                logger.info("Seems like official download is available");
                br.getPage(externalDownloadURL);
                String officialDownloadURL = br.getRegex("\"((/|http)[^\"]+/" + url_title + "/download/[^\"]+)\"").getMatch(0);
                if (officialDownloadURL == null) {
                    /* Wider attempt */
                    officialDownloadURL = br.getRegex("\"((/|http)[^\"]+/download/[^\"]+)\"").getMatch(0);
                }
                if (officialDownloadURL != null) {
                    logger.info("Successfully found official downloadurl");
                    if (!officialDownloadURL.startsWith("http")) {
                        officialDownloadURL = "https://" + this.getHost() + officialDownloadURL;
                    }
                    this.dllink = officialDownloadURL;
                    isOfficialDownload = true;
                } else {
                    logger.warning("Failed to find official downloadurl");
                }
            } else {
                logger.info("No official downloadurl available");
            }
        }
        if (dllink == null) {
            dllink = this.br.getRegex("\"(https?://download\\.hearthis\\.at/\\?track=[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = this.br.getRegex("\"(https?://[^<>\"]*?)\">click here to download</a>").getMatch(0);
        }
        if (dllink == null) {
            /* We failed to get a 'real' downloadlink --> Fallback to stream (lower quality) */
            /* Stream urls can be downloaded with multiple connections and resume is possible! */
            free_resume = true;
            free_maxchunks = 1; // 2021-09-30: Disabled chunkload as it causes issues
            dllink = downloadlink_stream;
            logger.info("\n\n-> Failed to get download link -> Checking stream link: " + dllink + "\n");
        }
        title = Encoding.htmlDecode(title);
        title = title.trim();
        if (!link.isNameSet()) {
            link.setFinalFileName(title + extDefault);
        }
        if (!StringUtils.isEmpty(this.dllink) && !isDownload) {
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Referer", "https://" + this.getHost() + "/");
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                /* required */
                br2.getHeaders().put(OPEN_RANGE_REQUEST);
                /* Do NOT use HEAD request here! */
                con = br2.openGetConnection(dllink);
                handleConnectionErrors(br2, con);
                if (con.getCompleteContentLength() > 0) {
                    if (DownloadInterface.isNewHTTPCore()) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    } else {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                }
                final String ext = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (ext != null) {
                    link.setFinalFileName(title + "." + ext);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (link.getBooleanProperty("ticket89269", false)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // required to enforce use of range requests
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume && DownloadInterface.isNewHTTPCore(), free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (isOfficialDownload) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download broken serverside?");
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Audio file broken?");
            }
        }
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
