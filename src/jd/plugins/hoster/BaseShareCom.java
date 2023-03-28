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

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BaseShareCom extends antiDDoSForHost {
    public BaseShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String PATTERN_EMBED  = "https?://[^/]+/songs/embed/id/(\\d+)";
    private final String PATTERN_NORMAL = "https?://[^/]+/([^/]+)/songs/([^/]+)/(\\d+)/?";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "baseshare.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(songs/embed/id/\\d+|[^/]+/songs/[^/]+/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://baseshare.com/site/page/view/tos";
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
        final Regex urlinfoEmbed = new Regex(link.getPluginPatternMatcher(), PATTERN_EMBED);
        final Regex urlinfoNormal = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL);
        if (urlinfoEmbed.matches()) {
            return urlinfoEmbed.getMatch(0);
        } else {
            return urlinfoNormal.getMatch(2);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Regex urlinfoEmbed = new Regex(link.getPluginPatternMatcher(), PATTERN_EMBED);
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            final Regex urlinfoNormal = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL);
            if (urlinfoEmbed.matches()) {
                link.setName(urlinfoEmbed.getMatch(0) + ".mp3");
            } else {
                link.setName(urlinfoNormal.getMatch(0) + " - " + urlinfoNormal.getMatch(1) + ".mp3");
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (urlinfoEmbed.matches()) {
            final String urlNormal = br.getRegex(PATTERN_NORMAL).getMatch(-1);
            if (urlNormal == null) {
                /* Embed item does not link to normal item/link -> Must be offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            getPage(urlNormal);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String artist = br.getRegex("<h1>([^<>]*?)</h1>").getMatch(0);
        String title = br.getRegex("<h2>([^<>]*?)</h2>").getMatch(0);
        String filename = null;
        if (dllink == null) {
            dllink = br.getRegex("(/uploads/(songs|zips)/[^<>\"]*?\\.mp3)\"").getMatch(0);
        }
        if (artist == null || title == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        artist = encodeUnicode(Encoding.htmlDecode(artist)).trim();
        title = encodeUnicode(Encoding.htmlDecode(title)).trim();
        filename = artist + " - " + title;
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp3");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final boolean serverSendsFilesize = false;
        if (dllink != null && serverSendsFilesize) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
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

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* for the crawler, so we have only one session of antiddos */
    public void getPage(final String url) throws Exception {
        super.getPage(url);
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
