//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TnaFlixCom extends PluginForHost {
    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private static final String TYPE_NORMAL           = "(?i)https?://[^/]+/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)";
    private static final String TYPE_embed            = "(?i)https?://[^/]*?player\\.[^/]+/video/(\\d+)";
    private static final String TYPE_embedding_player = ".+/embedding_player/embedding_feed\\.php\\?viewkey=([a-z0-9]+)";

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
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(view_video\\.php\\?viewkey=[a-z0-9]+|video/\\d+|.*?video\\d+)|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tnaflix.com" });
        return ret;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid = getViewkey(link.getPluginPatternMatcher());
        if (linkid == null) {
            linkid = getVideoID(link.getPluginPatternMatcher());
        }
        if (linkid != null) {
            return link.getHost() + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getViewkey(final String url) {
        return new Regex(url, "viewkey=([a-z0-9]+)").getMatch(0);
    }

    private String getVideoID(final String url) {
        String videoid = new Regex(url, "video(\\d+)$").getMatch(0);
        if (videoid == null) {
            videoid = new Regex(url, TYPE_embed).getMatch(0);
        }
        return videoid;
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    private String getURLName(final DownloadLink link) {
        String urlname = new Regex(link.getPluginPatternMatcher(), "(?i)https?://[^/]+/.*/([^/]+)/video\\d+$").getMatch(0);
        if (urlname == null) {
            /* Fallback */
            urlname = this.getViewkey(link.getPluginPatternMatcher());
        } else {
            urlname = urlname.replace("-", " ");
        }
        return urlname;
    }

    private String getContentURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_embedding_player)) {
            /* Convert embed urls --> Original urls */
            return link.getPluginPatternMatcher().replaceFirst("(?i)http://", "https://").replace("embedding_player/embedding_feed", "view_video");
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("https://" + link.getHost(), "content_filter2", "type%3Dstraight%26filter%3Dcams");
        br.setCookie("https://" + link.getHost(), "content_filter3", "type%3Dstraight%2Ctranny%2Cgay%26filter%3Dcams");
        String filename = null;
        br.getPage(this.getContentURL(link));
        if (br.containsHTML("class=\"errorPage page404\"|> This video is set to private") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().length() < 30) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (redirect.contains("errormsg=true")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (redirect.contains("video")) {
                link.setUrlDownload(br.getRedirectLocation());
            }
            br.getPage(redirect);
        }
        filename = br.getRegex("<title>([^<>]*?) \\- TNAFlix Porn Videos</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
            if (filename == null) {
                String videoid = this.getVideoID(link.getPluginPatternMatcher());
                if (videoid == null) {
                    videoid = this.getVideoID(br.getURL());
                }
                if (videoid != null) {
                    filename = br.getRegex("video" + videoid + "\"[^>]*target[^>]*>\\s*(.*?)\\s*<").getMatch(0);
                }
            }
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = getURLName(link) + ".mp4";
            link.setName(filename);
        } else {
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        String vkey = this.getViewkey(link.getPluginPatternMatcher());
        String videoid = this.getVideoID(link.getPluginPatternMatcher());
        if (videoid == null) {
            videoid = this.getVideoID(br.getURL());
        }
        if (vkey == null) {
            vkey = this.br.getRegex("id=\"vkey\" type=\"hidden\" value=\"([A-Za-z0-9]+)\"").getMatch(0);
        }
        // final String nkey = this.br.getRegex("id=\"nkey\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
        /* This link doesn't have quality choice: https://www.tnaflix.com/view_video.php?viewkey=b5a6fcf68b48e6dd6734 */
        /* This may sometimes return 403 - avoid it if possible! */
        String download = br.getRegex("<div class=\"playlist_listing\" data-loaded=\"true\">(.*?)</div>").getMatch(0);
        if (download == null) {
            /* This may sometimes return 403 - avoid it if possible! */
            download = br.getRegex("download href=\"((https?:)?//[^<>\"]+)\"").getMatch(0);
        }
        String dllink = null;
        if (download != null) {
            /* Official download */
            if (StringUtils.isNotEmpty(dllink)) {
                dllink = br.getURL(dllink).toExternalForm();
            } else {
                final String[] qualities = { "720", "480", "360", "240", "144" };
                for (final String quality : qualities) {
                    dllink = new Regex(download, "href=(\"|')((?:https?:)?//.*?)\\1>Download in " + quality).getMatch(1);
                    if (dllink != null) {
                        break;
                    }
                }
            }
        }
        if (dllink == null) {
            if (videoid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("https://" + getHost() + "/ajax/video-player/" + videoid);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String html = entries.get("html").toString();
            final String[] qualities = new Regex(html, "(https?://[^\"]+)\" type=\"video/mp4").getColumn(0);
            int bestHeight = -1;
            for (final String quality : qualities) {
                final String heightStr = new Regex(quality, "(\\d+)p\\.mp4").getMatch(0);
                int height = -1;
                if (heightStr != null) {
                    height = Integer.parseInt(heightStr);
                }
                if (dllink == null || height > bestHeight) {
                    bestHeight = height;
                    dllink = quality;
                }
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 416) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
            } else {
                /*
                 * 403 error usually means we've tried to download an official downloadurl which may only be available for loggedin users!
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
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