//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornfun.com" }, urls = { "https?://(?:www\\.)?pornfun\\.com/videos/\\d+/[a-z0-9\\-]+/?" })
public class PornfunCom extends PluginForHost {
    public PornfunCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = 1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              isDownload        = false;

    @Override
    public String getAGBLink() {
        return "https://pornfun.com/terms/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        downloadLink.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        br.getPage(downloadLink.getDownloadURL());
        final String filename_url = jd.plugins.hoster.KernelVideoSharingCom.regexURLFilenameAuto(this.br, downloadLink);
        if (br.containsHTML("KernelTeamVideoSharingSystem\\.js|KernelTeamImageRotator_")) {
            /* <script src="/js/KernelTeamImageRotator_3.8.1.jsx?v=3"></script> */
            /* <script type="text/javascript" src="http://www.hclips.com/js/KernelTeamVideoSharingSystem.js?v=3.8.1"></script> */
        }
        String filename = jd.plugins.hoster.KernelVideoSharingCom.regexFilenameAuto(br, downloadLink);
        if (StringUtils.isEmpty(filename_url)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/404.php")) {
            /* Definitly offline - set url filename to avoid bad names! */
            downloadLink.setName(filename_url);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename);
        dllink = jd.plugins.hoster.KernelVideoSharingCom.getDllink(br, this);
        final String ext;
        if (dllink != null && !dllink.contains(".m3u8")) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            /* Fallback */
            ext = ".mp4";
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        // this prevents another check when download is about to happen! -raztoki
        if (isDownload) {
            return AvailableStatus.TRUE;
        }
        if (dllink != null && !dllink.contains(".m3u8")) {
            URLConnectionAdapter con = null;
            try {
                // if you don't do this then referrer is fked for the download! -raztoki
                final Browser br = this.br.cloneBrowser();
                // In case the link redirects to the finallink -
                br.setFollowRedirects(true);
                try {
                    con = br.openHeadConnection(dllink);
                    final String workaroundURL = jd.plugins.hoster.KernelVideoSharingCom.getHttpServerErrorWorkaroundURL(br.getHttpConnection());
                    if (workaroundURL != null) {
                        con = br.openHeadConnection(workaroundURL);
                    }
                } catch (final BrowserException e) {
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                final long filesize = con.getLongContentLength();
                if (!con.getContentType().contains("html") && filesize > 100000) {
                    downloadLink.setDownloadSize(filesize);
                    final String redirect_url = br.getHttpConnection().getRequest().getUrl();
                    if (redirect_url != null) {
                        dllink = redirect_url;
                        logger.info("dllink: " + dllink);
                    }
                } else {
                    server_issues = true;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        isDownload = true;
        requestFileInformation(downloadLink);
        if (StringUtils.isEmpty(dllink)) {
            /* 2016-12-02: At this stage we should have a working hls to http workaround so we should never get hls urls. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (this.dllink.contains(".m3u8")) {
            /* hls download */
            /* Access hls master. */
            br.getPage(this.dllink);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            /* http download */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
            final String workaroundURL = jd.plugins.hoster.KernelVideoSharingCom.getHttpServerErrorWorkaroundURL(dl.getConnection());
            if (workaroundURL != null) {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, workaroundURL, free_resume, free_maxchunks);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 503) {
                    /* Should only happen in rare cases */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached", 5 * 60 * 1000l);
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
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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
