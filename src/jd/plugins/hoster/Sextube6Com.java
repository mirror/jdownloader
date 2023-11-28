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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class Sextube6Com extends PluginForHost {
    public Sextube6Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private final String PROPERTY_INTERNAL_VIDEOID = "videoid";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sextube-6.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(video/[a-z0-9\\-]+\\.html|flplayer\\.php\\?id=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://sextube-6.com/dmca.html";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String videoid = getInternalVideoid(link);
        if (videoid != null) {
            return this.getHost() + "://" + videoid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getInternalVideoid(final DownloadLink link) {
        final String storedInternalVideoid = link.getStringProperty(PROPERTY_INTERNAL_VIDEOID);
        if (storedInternalVideoid != null) {
            return storedInternalVideoid;
        } else {
            return getVideoidFromURL(link.getPluginPatternMatcher());
        }
    }

    private static String getVideoidFromURL(final String url) {
        try {
            return UrlQuery.parse(url).get("id");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getInternalVideoid(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String internalVideoidFromURL = getVideoidFromURL(link.getPluginPatternMatcher());
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String embedurl = br.getRegex("(/flplayer\\.php\\?id=\\d+)").getMatch(0);
        if (embedurl != null && !br.getURL().contains(embedurl)) {
            br.getPage(embedurl);
        }
        final String internalVideoidFromHTML = UrlQuery.parse(br.getURL()).get("id");
        if (internalVideoidFromURL == null && internalVideoidFromHTML != null) {
            /* Only store property if videoid is not part of URL added by user. */
            link.setProperty(PROPERTY_INTERNAL_VIDEOID, internalVideoidFromHTML);
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = br.getRegex("class=\"block\\-title\">[\t\n\r ]+<h\\d+>([^<>]*?)<").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            title = title.replaceFirst("(?i)SexTube6?$", "");
            link.setFinalFileName(title + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String doubleb64 = br.getRegex("atob\\(\"([^\"]+)").getMatch(0);
        final String html = Encoding.Base64Decode(Encoding.Base64Decode(doubleb64));
        final String[][] hlsinfolist = new Regex(html, "title=\"(\\d+)p\"[^>]*src=\"(https?://[^\"]+)\"").getMatches();
        if (hlsinfolist == null || hlsinfolist.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Find best video quality */
        String dllink = null;
        int heightMax = -1;
        for (final String[] hlsinfo : hlsinfolist) {
            final String heightStr = hlsinfo[0];
            final int height = Integer.parseInt(heightStr);
            final String url = hlsinfo[1];
            if (dllink == null || height > heightMax) {
                heightMax = height;
                dllink = url;
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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