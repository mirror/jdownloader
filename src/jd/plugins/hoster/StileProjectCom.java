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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "stileproject.com" }, urls = { "https?://(?:www\\.)?stileproject\\.com/(?:video/[a-z0-9\\-]+-\\d+\\.html|embed/\\d+)" })
public class StileProjectCom extends PluginForHost {
    private String dllink = null;

    public StileProjectCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.stileproject.com/page/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private static final String TYPE_EMBED  = "https?://[^/]+/embed/(\\d+)";
    private static final String TYPE_NORMAL = "https?://[^/]+/video/([a-z0-9\\-]+)-(\\d+)\\.html";

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
        }
    }

    private String getURLTitle(final DownloadLink link) {
        return getURLTitle(link.getPluginPatternMatcher());
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitle(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle.replace("-", " ").trim() + ".mp4";
        } else {
            return this.getFID(link) + ".mp4";
        }
    }

    private String getURLTitle(final String url) {
        return new Regex(url, TYPE_NORMAL).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", "https://www." + this.getHost());
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*404 Error Page") || br.containsHTML("video_removed_dmca\\.jpg\"|error\">We're sorry")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().matches(TYPE_EMBED)) {
            final String realVideoURL = br.getRegex(TYPE_NORMAL).getMatch(-1);
            if (realVideoURL == null || !realVideoURL.contains(this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setPluginPatternMatcher(realVideoURL);
            br.getPage(realVideoURL);
        }
        final String titleByURL = getURLTitle(br.getURL());
        if (titleByURL != null) {
            link.setFinalFileName(titleByURL.replace("-", " ").trim() + ".mp4");
        }
        // String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+).html$").getMatch(0);
        // String embedURL = "https://www.stileproject.com/embed/" + fid;
        // br.getPage(embedURL);
        getdllink();
        if (!StringUtils.isEmpty(dllink)) {
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // Same code as for CelebrityCuntNet
    private void getdllink() throws Exception {
        dllink = br.getRegex("<source src=\"(https?://[^<>\"]+)[^<>]+type='video/mp4'").getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("var desktopFile\\s*=\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        }
        if (dllink == null) {
            final Regex videoMETA = br.getRegex("(VideoFile|VideoMeta)_(\\d+)");
            final String type = videoMETA.getMatch(0);
            final String id = videoMETA.getMatch(1);
            final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
            if (type == null || id == null || cb == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String postData = "cacheBuster=" + System.currentTimeMillis() + "&jsonRequest=%7B%22path%22%3A%22" + type + "%5F" + id + "%22%2C%22cb%22%3A%22" + cb + "%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22returnType%22%3A%22json%22%2C%22file%22%3A%22" + type + "%5F" + id + "%22%2C%22htmlHostDomain%22%3A%22www%2Estileproject%2Ecom%22%2C%22height%22%3A%22508%22%2C%22appdataurl%22%3A%22http%3A%2F%2Fwww%2Estileproject%2Ecom%2Fgetcdnurl%2F%22%2C%22playerOnly%22%3A%22true%22%2C%22request%22%3A%22getAllData%22%2C%22width%22%3A%22640%22%7D";
            br.postPage("/getcdnurl/", postData);
            dllink = br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}