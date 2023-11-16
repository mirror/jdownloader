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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "coub.com" }, urls = { "https?://(?:www\\.)?coub\\.com/(?:view|embed)/([A-Za-z0-9]+)" })
public class CoubCom extends PluginForHost {
    public CoubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://coub.com/tos";
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

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;
    private String               DLLINK            = null;

    @SuppressWarnings("unchecked")
    /** Using API: http://coub.com/dev/docs */
    /**
     * Example for profile decrypter:
     * http://coub.com/api/v2/timeline/channel/22e53751f21ebf9707d4707fc452cb72?per_page=9&permalink=22e53751f21ebf9707d4707fc452cb72&order_by=newest&page=3
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        final String fid = getFID(link);
        /* 2020-06-23: Some items only have downloadlinks available in the website-json (??) */
        final boolean use_api = true;
        final Map<String, Object> entries;
        if (use_api) {
            this.br.getPage("https://" + this.getHost() + "/api/v2/coubs/" + fid);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                /* {"error":"You are not authorized to access this page.","exc":"CanCan::AccessDenied"} */
                logger.info("Possible private content --> Not sure but probably offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } else {
            this.br.getPage("https://" + this.getHost() + "/view/" + fid);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String json = br.getRegex("<script [^>]*coubPageCoubJson[^>]*>(.*?)</script>").getMatch(0);
            entries = restoreFromString(json, TypeRef.MAP);
        }
        final String created_at = (String) entries.get("created_at");
        String filename = getFilename(this, entries, fid);
        DLLINK = (String) JavaScriptEngineFactory.walkJson(entries, "file_versions/share/default");
        if (filename == null || StringUtils.isEmpty(created_at)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String date_formatted = new Regex(created_at, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (date_formatted == null) {
            /* Fallback */
            date_formatted = created_at;
        }
        link.setFinalFileName(date_formatted + "_" + filename);
        /* Format URL so that it is valid */
        if (!StringUtils.isEmpty(DLLINK)) {
            DLLINK = DLLINK.replace("%{type}", "mp4").replace("%{version}", "big");
            URLConnectionAdapter con = null;
            try {
                /* Do NOT use HEAD requests here! */
                con = br.openGetConnection(DLLINK);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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

    public static String getFilename(final Plugin plugin, final Map<String, Object> entries, final String fid) {
        String filename = (String) entries.get("raw_video_title");
        if (StringUtils.isEmpty(filename)) {
            filename = (String) entries.get("title");
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback: This should never happen! */
            filename = fid;
        }
        if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4") && !StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
            filename += ".mp4";
        }
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (StringUtils.isEmpty(this.DLLINK)) {
            logger.info("Seems like this video has never been downloaded before --> Generating downloadurl");
            br.getPage("/api/v2/coubs/" + this.getFID(link) + "/share_video_status");
            this.DLLINK = PluginJSonUtils.getJson(br, "url");
            final String status = PluginJSonUtils.getJson(br, "status");
            if (!StringUtils.isEmpty(this.DLLINK)) {
                /* E.g. {"status":"ready","url":"https://coubsecure-s.akamaihd.net/get/bla.mp4"} */
                logger.info("Successfully found downloadurl after first API call");
            } else if (!StringUtils.isEmpty(status) && (status.equalsIgnoreCase("queued") || status.equalsIgnoreCase("working"))) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Waiting for downloadlink generation", 1 * 60 * 1000l);
            } else if (!StringUtils.isEmpty(status)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API status: " + status, 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, resumable, maxchunks);
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
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}