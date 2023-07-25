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

import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "videopress.com" }, urls = { "https?://(?:www\\.)?videopress\\.com/(?:embed|v)/([A-Za-z0-9]+)" })
public class VideopressCom extends PluginForHost {
    public VideopressCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension     = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume           = true;
    private static final int     free_maxchunks        = 0;
    private static final int     free_maxdownloads     = -1;
    private String               dllink                = null;
    private static final String  PROPERTY_PRIVATEVIDEO = "privatevideo";

    @Override
    public String getAGBLink() {
        return "https://videopress.com/";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + default_extension);
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(401);
        br.getPage("https://public-api.wordpress.com/rest/v1.1/videos/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* {"error":"unknown_media","message":"The specified video was not found."} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 401) {
            link.setProperty(PROPERTY_PRIVATEVIDEO, true);
            return AvailableStatus.TRUE;
        }
        link.removeProperty(PROPERTY_PRIVATEVIDEO);
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        String title = (String) entries.get("title"); // can be empty
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = fid;
        }
        dllink = (String) entries.get("original");
        title = Encoding.htmlDecode(title);
        title = title.trim();
        String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
            if (ext != null && !ext.matches("\\.(?:flv|mp4)")) {
                ext = default_extension;
            }
        } else {
            ext = default_extension;
        }
        if (!title.endsWith(ext)) {
            title += ext;
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(title);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?");
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
        requestFileInformation(link);
        if (this.isPrivateVideo(link)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private video");
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private boolean isPrivateVideo(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_PRIVATEVIDEO)) {
            return true;
        } else {
            return false;
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
