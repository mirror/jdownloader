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
import java.util.List;

import org.appwork.utils.StringUtils;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class LusciousNet extends PluginForHost {
    public LusciousNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = false;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://luscious.net/terms/";
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
        String fid = new Regex(link.getPluginPatternMatcher(), "/videos/([A-Za-z0-9\\-_]+)").getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), "/id/(\\d+)").getMatch(0);
        }
        return fid;
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "luscious.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(videos/[A-Za-z0-9\\-_]+|(?!albums/).*/id/(\\d+)/?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String default_extension = ".mp4";
        final String fid = this.getFID(link);
        this.dllink = checkDirectLink(link, "directlink");
        if (this.dllink != null) {
            /* Check has been done before -> Done! */
            return AvailableStatus.TRUE;
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(this.getFID(link))) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = null;
        final boolean obtainTitleFromURL = true;
        if (obtainTitleFromURL) {
            /* Remove contentID */
            title = fid.replaceFirst("_\\d+$", "");
            title = title.replace("-", " ");
            title = title.replace("_", " ");
            title = title.trim();
        }
        final String[] videourls = br.getRegex("<source[^>]*src=\"(https?://[^\"]+)\"").getColumn(0);
        if (videourls != null && videourls.length > 0) {
            /* Find best quality videourl */
            int heightMax = 0;
            for (final String videourl : videourls) {
                if (dllink == null) {
                    dllink = videourl;
                }
                final String videoHeightStr = new Regex(videourl, "(\\d+)\\.mp4$").getMatch(0);
                if (videoHeightStr != null) {
                    final int videoHeight = Integer.parseInt(videoHeightStr);
                    if (videoHeight > heightMax) {
                        heightMax = videoHeight;
                        dllink = videourl;
                    }
                }
            }
        }
        if (dllink == null) {
            /* Old handling: Single image download */
            dllink = br.getRegex("<picture><source[^>]*srcSet=\"(https?://[^\"]+)\"").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            final String ext;
            if (!StringUtils.isEmpty(dllink)) {
                ext = getFileNameExtensionFromString(dllink, default_extension);
            } else {
                ext = default_extension;
            }
            if (!title.endsWith(ext)) {
                title += ext;
            }
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(title);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                    link.setProperty("directlink", dllink);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(title);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
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
