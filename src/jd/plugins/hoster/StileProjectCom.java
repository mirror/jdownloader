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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.decrypter.StileProjectComDecrypter.class })
public class StileProjectCom extends PluginForHost {
    private String             dllink       = null;
    public static final String TYPE_EMBED   = "https?://[^/]+/embed/(\\d+)";
    public static final String TYPE_NORMAL  = "https?://[^/]+/video/([a-z0-9\\-_]+)-(\\d+)\\.html";
    public static final String TYPE_NORMAL2 = "https?://[^/]+/videos/(\\d+)/([a-z0-9\\-_]+)/?";

    public StileProjectCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.StileProjectComDecrypter.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:video/[a-z0-9\\-]+-\\d+\\.html|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.stileproject.com/contact";
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

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitleCleaned(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle.replace("-", " ").trim() + ".mp4";
        } else {
            return this.getFID(link) + ".mp4";
        }
    }

    private String getURLTitleCleaned(final String url) {
        String title = new Regex(url, TYPE_NORMAL).getMatch(0);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", "https://www." + this.getHost());
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().matches(TYPE_EMBED)) {
            final String realVideoURL = br.getRegex(TYPE_NORMAL).getMatch(-1);
            if (realVideoURL == null || !realVideoURL.contains(this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(realVideoURL);
            /* Double-check */
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.canHandle(br.getURL())) {
                link.setPluginPatternMatcher(br.getURL());
            }
        }
        final String titleByURL = getURLTitleCleaned(br.getURL());
        if (titleByURL != null) {
            link.setFinalFileName(titleByURL.replace("-", " ").trim() + ".mp4");
        }
        this.dllink = this.getdllink();
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

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*404 Error Page") || br.containsHTML("video_removed_dmca\\.jpg\"|error\">We're sorry")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getdllink() throws Exception {
        String directurl = br.getRegex("<source src=\"(https?://[^<>\"]+)[^<>]+type='video/mp4'").getMatch(0);
        if (StringUtils.isEmpty(directurl)) {
            directurl = br.getRegex("var desktopFile\\s*=\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        }
        if (directurl == null) {
            final Regex videoMETA = br.getRegex("(VideoFile|VideoMeta)_(\\d+)");
            final String type = videoMETA.getMatch(0);
            final String id = videoMETA.getMatch(1);
            final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
            if (type == null || id == null || cb == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String postData = "cacheBuster=" + System.currentTimeMillis() + "&jsonRequest=%7B%22path%22%3A%22" + type + "%5F" + id + "%22%2C%22cb%22%3A%22" + cb + "%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22returnType%22%3A%22json%22%2C%22file%22%3A%22" + type + "%5F" + id + "%22%2C%22htmlHostDomain%22%3A%22www%2Estileproject%2Ecom%22%2C%22height%22%3A%22508%22%2C%22appdataurl%22%3A%22http%3A%2F%2Fwww%2Estileproject%2Ecom%2Fgetcdnurl%2F%22%2C%22playerOnly%22%3A%22true%22%2C%22request%22%3A%22getAllData%22%2C%22width%22%3A%22640%22%7D";
            br.postPage("/getcdnurl/", postData);
            directurl = br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
        }
        return directurl;
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