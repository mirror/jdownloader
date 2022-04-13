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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PornRabbitCom extends PluginForHost {
    private String              dllink      = null;
    private static final String TYPE_EMBED  = "https?://[^/]+/embed/(\\d+)";
    private static final String TYPE_NORMAL = "https?://[^/]+/video/([a-z0-9\\-]+)\\-(\\d+)\\.html";

    public PornRabbitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.PornRabbitComDecrypter.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:video/[a-z0-9\\-]+\\-\\d+\\.html|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornrabbit.com/terms_of_service.html";
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

    private String getURLTitleCleaned(final DownloadLink link) {
        return getURLTitleCleaned(link.getPluginPatternMatcher());
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitleCleaned(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle.replace("-", " ").trim() + ".mp4";
        } else {
            return this.getFID(link) + ".mp4";
        }
    }

    public static String getURLTitleCleaned(final String url) {
        String title = new Regex(url, TYPE_NORMAL).getMatch(0);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        dllink = null;
        final Browser br2 = br.cloneBrowser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
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
        final String titleByURL = getURLTitleCleaned(br.getURL());
        if (titleByURL != null) {
            link.setFinalFileName(titleByURL.replace("-", " ").trim() + ".mp4");
        }
        String path = br.getRegex("path=(VideoFile_\\d+)\\&").getMatch(0);
        final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
        if (path != null && cb != null) {
            // Try to get stream link first as downloadlinks might be broken (4 KB files)
            path = Encoding.urlEncode(path);
            try {
                br2.postPage("https://www." + this.getHost() + "/getcdnurl/", "jsonRequest=%7B%22playerOnly%22%3A%22true%22%2C%22height%22%3A%22412%22%2C%22file%22%3A%22" + path + "%22%2C%22htmlHostDomain%22%3A%22www%2Epornrabbit%2Ecom%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22path%22%3A%22" + path + "%22%2C%22request%22%3A%22getAllData%22%2C%22cb%22%3A%22" + cb + "%22%2C%22appdataurl%22%3A%22http%3A%2F%2Fwww%2Epornrabbit%2Ecom%2Fgetcdnurl%2F%22%2C%22width%22%3A%22744%22%2C%22returnType%22%3A%22json%22%7D&cacheBuster=" + System.currentTimeMillis());
                dllink = br2.getRegex("\"file\"\\s*:\\s*\"(http://[^<>\"]*?)\"").getMatch(0);
            } catch (final Exception e) {
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=(?:\"|\\')(https?://[^<>\"\\']*?)(?:\"|\\')[^>]*?type=(?:\"|\\')(?:video/)?(?:mp4|flv)(?:\"|\\')").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("var desktopFile = '(https?://[^<>\"\\']+)';").getMatch(0);
        }
        if (dllink != null) {
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br2.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)(>Page Not Found<|>Sorry but the page you are looking for has|video_removed_dmca\\.jpg\")")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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