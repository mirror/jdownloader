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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ivoox.com" }, urls = { "https?://(?:[a-z]+\\.)?ivoox\\.com/(?:[a-z]{2}/)?[a-z0-9\\-]+audios\\-mp3_rf_(\\d+)_\\d+\\.html" })
public class IvooxCom extends PluginForHost {
    public IvooxCom(PluginWrapper wrapper) {
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

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp3";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://de.ivoox.com/en/informacion-legal_il.html";
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
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + default_extension);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<h2>Descripción de ([^<>]+)</h2>").getMatch(0);
        if (title == null) {
            /* 2016-10-14: They messed up escaping in this html. */
            title = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"\\s*/\\s*>").getMatch(0);
        }
        String official_download = br.getRegex("downloadlink\\'\\)\\.load\\(\\'([^<>\"\\']+)\\'\\)").getMatch(0);
        if (official_download != null) {
            if (!official_download.startsWith("/")) {
                /*
                 * 2020-01-22: Use getBaseURL because if we don't, we might fail to include the language part e.g. '/de/' --> It will then
                 * redirect to mainpage --> Failure
                 */
                official_download = br.getBaseURL() + official_download;
            }
            br.getPage(official_download);
            dllink = br.getRegex("downloadFollow\\(event,\\'(https[^<>\"]+)\\'\\)").getMatch(0);
        } else {
            /* 2019-02-05: Old way! */
            dllink = "http://files.ivoox.com/listen/" + fid;
        }
        /* Important: Direct-URL can only be used one time!! */
        if (dllink != null && !isDownload) {
            if (title != null) {
                title = Encoding.htmlDecode(title);
                title = title.trim();
                link.setName(title + default_extension);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                link.setVerifiedFileSize(con.getCompleteContentLength());
                final String extByMimeType = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (extByMimeType != null) {
                    link.setFinalFileName(title + "." + extByMimeType);
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

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws Exception {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        this.handleConnectionErrors(br, dl.getConnection());
        final String filename = link.getName();
        final String extByMimeType = Plugin.getExtensionFromMimeTypeStatic(dl.getConnection().getContentType());
        if (extByMimeType != null && filename != null) {
            link.setFinalFileName(this.correctOrApplyFileNameExtension(filename, "." + extByMimeType));
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
