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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "trannytube.tv" }, urls = { "https?://(?:www\\.)?trannytube\\.tv/(?:[a-z]{2}/)?(movies/\\d+/[a-z0-9\\-]+|embed/\\d+)" })
public class TrannytubeTv extends PluginForHost {
    public TrannytubeTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: porn plugin
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private final String         PATTERN_NORMAL    = "(?i)https?://[^/]+/(?:[a-z]{2}/)?movies/(\\d+)/([a-z0-9\\-]+)";
    private final String         PATTERN_EMBED     = "(?i)https?://[^/]+/(?:[a-z]{2}/)?embed/(\\d+)";
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.trannytube.tv/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return "trannytube.tv://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), PATTERN_EMBED).getMatch(0);
        }
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        final String fid = this.getFID(link);
        String urlSlug = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(1);
        boolean setFallbackName = false;
        if (!link.isNameSet()) {
            if (urlSlug != null) {
                link.setName(urlSlug.replace("-", " ").trim() + default_extension);
            } else {
                link.setName(fid + default_extension);
            }
            setFallbackName = true;
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(PATTERN_EMBED)) {
            /* This will redirect to PATTERN_NORMAL. */
            br.getPage("https://www." + this.getHost() + "/movies/" + fid + "/dummy-slug");
        } else {
            br.getPage(link.getPluginPatternMatcher().replaceFirst("(?i)http://", "https://"));
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (setFallbackName) {
            urlSlug = new Regex(br.getURL(), PATTERN_NORMAL).getMatch(0);
            if (urlSlug != null) {
                link.setName(urlSlug.replace("-", " ").trim() + default_extension);
            }
        }
        String title = br.getRegex("<title>([^<>\"]+) at Tranny Tube TV</title>").getMatch(0);
        dllink = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(?:file|url):[\t\n\r ]*?(?:\"|\\')(http[^<>\"]*?)(?:\"|\\')").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\"[^<]*type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[a-z0-9]+\\.trannytube\\.tv/video\\d+/f/[^<>\"]+)").getMatch(0);
        }
        String ext = null;
        if (title != null) {
            String filename = Encoding.htmlDecode(title);
            filename = filename.trim();
            if (dllink != null) {
                ext = getFileNameExtensionFromString(dllink, default_extension);
            } else {
                ext = default_extension;
            }
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        if (!StringUtils.isEmpty(dllink)) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
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
