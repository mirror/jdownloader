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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fux.com", "4tube.com", "porntube.com", "pornerbros.com" }, urls = { "https?://(?:www\\.)?fux\\.com/(?:video|embed)/\\d+/?(?:[\\w-]+)?", "https?://(?:www\\.)?4tube\\.com/(?:embed|videos)/\\d+/?(?:[\\w-]+)?|https?://m\\.4tube\\.com/videos/\\d+/?(?:[\\w-]+)?", "https?://(?:www\\.)?(?:porntube\\.com/videos/[a-z0-9\\-]+_\\d+|embed\\.porntube\\.com/\\d+|porntube\\.com/embed/\\d+)", "https?://(?:www\\.)?(?:pornerbros\\.com/videos/[a-z0-9\\-]+_\\d+|embed\\.pornerbros\\.com/\\d+|pornerbros\\.com/embed/\\d+)" })
public class FuxCom extends PluginForHost {
    public FuxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* tags: fux.com, porntube.com, 4tube.com, pornerbros.com */
    @Override
    public String getAGBLink() {
        return "http://www.fux.com/legal/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String protocol_of_mobile_URL = new Regex(link.getPluginPatternMatcher(), "^(https?://)m\\..+").getMatch(0);
        if (protocol_of_mobile_URL != null) {
            /* E.g. 4tube.com, Change mobile-website-URL --> Desktop URL */
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll(protocol_of_mobile_URL + "m.", protocol_of_mobile_URL));
        }
        final String linkid = this.getLinkID(link);
        if (link.getPluginPatternMatcher().matches(".+4tube\\.com/embed/\\d+")) {
            /* Special case! */
            link.setPluginPatternMatcher(String.format("https://www.4tube.com/videos/%s/dummytext", linkid));
        } else if (link.getPluginPatternMatcher().matches(".+(porntube|pornerbros)\\.com/embed/\\d+")) {
            /* Special case! */
            final String host = link.getHost();
            link.setPluginPatternMatcher(String.format("https://www.%s/videos/dummytext_%s", host, linkid));
        } else {
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("/embed/", "/video/"));
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid = new Regex(link.getPluginPatternMatcher(), "/(?:videos|embed)/(\\d+)").getMatch(0);
        if (linkid == null) {
            /* E.g. porntube.com & pornerbros.com OLD embed linkformat */
            linkid = new Regex(link.getPluginPatternMatcher(), "https?://embed\\.[^/]+/(\\d+)").getMatch(0);
        }
        if (linkid == null) {
            /* E.g. pornerbros.com */
            linkid = new Regex(link.getPluginPatternMatcher(), "_(\\d+)$").getMatch(0);
        }
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    // private boolean isEmbed(final String url) {
    // return url.matches(".+(embed\\..+|/embed/).+");
    // }
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().matches(".+/videos?\\?error=\\d+")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2019-04-29: Always use 'Fallback filename' as it works for all supported websites and will usually give us a 'good looking'
         * filename.
         */
        String filename = getFallbackFilename(link);
        final String source;
        final String b64 = br.getRegex("window\\.INITIALSTATE = \\'([^\"\\']+)\\'").getMatch(0);
        if (b64 != null) {
            /* 2018-11-14: fux.com: New */
            source = Encoding.htmlDecode(Encoding.Base64Decode(b64));
        } else {
            source = br.toString();
        }
        /* 2019-04-29: fux.com */
        String mediaID = new Regex(source, "\"mediaId\":([0-9]{2,})").getMatch(0);
        if (mediaID == null) {
            /* 2019-04-29: E.g. 4tube.com and all others (?) */
            mediaID = getMediaid(this.br);
        }
        String availablequalities = br.getRegex("\\((\\d+)\\s*,\\s*\\d+\\s*,\\s*\\[([0-9,]+)\\]\\);").getMatch(0);
        if (availablequalities != null) {
            availablequalities = availablequalities.replace(",", "+");
        } else {
            /* Fallback */
            availablequalities = "1080+720+480+360+240";
        }
        if (mediaID == null || filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Origin", "http://www.fux.com");
        final boolean newWay = true;
        final String host = br.getHost();
        if (host.equals("fux.com")) {
            if (newWay) {
                /* 2019-04-29 */
                br.postPage("https://token.fux.com/" + mediaID + "/desktop/" + availablequalities, "");
            } else {
                /* Leave this in as it might still be usable as a fallback in the future! */
                br.postPage("https://tkn.fux.com/" + mediaID + "/desktop/" + availablequalities, "");
            }
        } else {
            br.postPage("https://token." + host + "/" + mediaID + "/desktop/" + availablequalities, "");
        }
        // seems to be listed in order highest quality to lowest. 20130513
        dllink = getDllink();
        filename = Encoding.htmlDecode(filename.trim());
        String ext = ".mp4";
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            if (dllink.contains(".m4v")) {
                ext = ".m4v";
            } else if (dllink.contains(".mp4")) {
                ext = ".mp4";
            } else {
                ext = ".flv";
            }
        }
        link.setFinalFileName(filename + ext);
        // In case the link redirects to the finallink
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink.trim());
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getFilenameURL(final String url) {
        String filename_url = new Regex(url, "/videos/\\d+/(.+)").getMatch(0);
        if (filename_url == null) {
            /* E.g. pornerbros.com */
            filename_url = new Regex(url, "/videos/(.+)_\\d+$").getMatch(0);
        }
        return filename_url;
    }

    private String getFallbackFilename(final DownloadLink dl) {
        final String linkid = this.getLinkID(dl);
        String filename_url = getFilenameURL(dl.getPluginPatternMatcher());
        /*
         * Sites will usually redirect to URL which contains title so if the user adds a short URL, there is still a chance to get a
         * filename via URL!
         */
        // final String filename_url_browser = getFilenameURL(br.getURL());
        /* URL-filename may also be present in HTML */
        String filename_url_browser_html = br.getRegex("/videos?/" + linkid + "/([A-Za-z0-9\\-_]+)").getMatch(0);
        if (filename_url_browser_html == null) {
            /* E.g. porntube.com & pornerbros.com */
            filename_url_browser_html = br.getRegex("/videos?/([A-Za-z0-9\\-_]+)_" + linkid).getMatch(0);
        }
        if (filename_url == null) {
            filename_url = getFilenameURL(br.getURL());
        }
        final String fallback_filename;
        if (filename_url != null && filename_url_browser_html != null && filename_url_browser_html.length() > filename_url.length()) {
            /* Title in current browser URL is longer than in the user-added URL --> Use that */
            fallback_filename = linkid + "_" + filename_url_browser_html;
        } else if (filename_url != null) {
            fallback_filename = linkid + "_" + filename_url;
        } else {
            fallback_filename = linkid;
        }
        return fallback_filename;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    final String getDllink() {
        String finallink = null;
        final String[] qualities = new String[] { "1080", "720", "480", "360", "240" };
        for (final String quality : qualities) {
            if (br.containsHTML("\"" + quality + "\"")) {
                finallink = br.getRegex("\"" + quality + "\":\\{\"status\":\"success\",\"token\":\"(http[^<>\"]*?)\"").getMatch(0);
                if (finallink != null && checkDirectLink(finallink) != null) {
                    break;
                }
            }
        }
        /* Hm probably this is only needed if only one quality exists */
        if (finallink == null) {
            finallink = br.getRegex("\"token\":\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        return finallink;
    }

    public static String getMediaid(final Browser br) throws IOException {
        return getMediaid(br, br.toString());
    }

    public static String getMediaid(final Browser br, final String source) throws IOException {
        final Regex info = new Regex(source, "\\.ready\\(function\\(\\) \\{embedPlayer\\((\\d+), \\d+, \\[(.*?)\\],");
        String mediaID = info.getMatch(0);
        if (mediaID == null) {
            mediaID = new Regex(source, "\\$\\.ajax\\(url, opts\\);[\t\n\r ]+\\}[\t\n\r ]+\\}\\)\\((\\d+),").getMatch(0);
        }
        if (mediaID == null) {
            mediaID = new Regex(source, "id=\"download\\d+p\" data\\-id=\"(\\d+)\"").getMatch(0);
        }
        if (mediaID == null) {
            // just like 4tube/porntube/fux....<script id="playerembed" src...
            final String embed = new Regex(source, "/js/player/(?:embed|web)/\\d+(?:\\.js)?").getMatch(-1);
            if (embed != null) {
                br.getPage(embed);
                mediaID = br.getRegex("\\((\\d+)\\s*,\\s*\\d+\\s*,\\s*\\[([0-9,]+)\\]\\);").getMatch(0); // $.ajax(url,opts);}})(
            }
        }
        return mediaID;
    }

    private String checkDirectLink(String directlink) {
        if (directlink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(directlink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    directlink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                directlink = null;
            }
        }
        return directlink;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript6;
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