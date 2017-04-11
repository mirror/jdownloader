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
import java.util.regex.Pattern;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mygirlfriendporn.com", "pornyeah.com", "bondagebox.com", "fetishbox.com", "luxuretv.com", "homemoviestube.com", "watchgfporn.com" }, urls = { "https?://(?:www\\.)?mygirlfriendporn\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?pornyeah\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?bondagebox\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?fetishbox\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.|en\\.)?luxuretv\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html", "http://(?:www\\.)?homemoviestube\\.com/videos/\\d+/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?watchgfporn\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html" })
public class UnknownPornScript4 extends PluginForHost {

    public UnknownPornScript4(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.1 */
    // other:

    /* Extension which will be used if no correct extension is found */

    private static final String  type_1            = "^https?://(?:www\\.)?[^/]+/videos/[a-z0-9\\-]+\\-\\d+\\.html$";
    /* E.g. homemoviestube.com */
    private static final String  type_2            = "^http://(?:www\\.)?[^/]+/videos/\\d+/[a-z0-9\\-]+\\.html$";

    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private String               rtmpurl           = null;

    @Override
    public String getAGBLink() {
        return "http://www.mygirlfriendporn.com/contact.php";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        rtmpurl = null;
        final String host = downloadLink.getHost();
        final Browser br2 = new Browser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().endsWith("/404.php")) {
            /* E.g. 404.php: http://www.bondagebox.com/ */
            /* E.g. responsecode 404: http://www.pornyeah.com */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(host + "/")) {
            /* E.g. http://www.watchgfporn.com/videos/she-fucked-just-about-all-of-us-that-night-9332.html */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String url_filename = null;
        if (downloadLink.getDownloadURL().matches(type_1)) {
            /* Fits for 99% e.g. pornyeah.com */
            url_filename = new Regex(downloadLink.getDownloadURL(), "/videos/([a-z0-9\\-]+)\\-\\d+\\.html").getMatch(0).replace("-", " ");
        } else {
            /* E.g. homemoviestube.com */
            url_filename = new Regex(downloadLink.getDownloadURL(), "/videos/\\d+/([a-z0-9\\-]+)\\.html$").getMatch(0).replace("-", " ");
        }
        String filename = regexStandardTitleWithHost(host);
        if (filename == null) {
            filename = url_filename;
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        String flashvars = this.br.getRegex("\"flashvars\",\"([^<>\"]*?)\"").getMatch(0);
        if (flashvars == null) {
            /* E.g. homemoviestube.com */
            flashvars = this.br.getRegex("flashvars=\"([^<>\"]+)").getMatch(0);
        }
        if (flashvars != null) {
            dllink = new Regex(flashvars, "(http://(?:www\\.)?[^/]+/playerConfig\\.php[^<>\"/\\&]+)").getMatch(0);
            if (dllink != null) {
                dllink = Encoding.htmlDecode(dllink);
                br2.getPage(dllink);
                dllink = br2.getRegex("flvMask:(http://[^<>\"]*?)(%7C|;)").getMatch(0);
                rtmpurl = br2.getRegex("conn:(rtmp://[^<>\"]*?);").getMatch(0);
                if (dllink == null && rtmpurl == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"([^<>\"]*?)\"").getMatch(0);
        }
        String ext = default_Extension;
        if (dllink != null && dllink.startsWith("http")) {
            dllink = Encoding.htmlDecode(dllink);
            filename = filename.trim();
            ext = getFileNameExtensionFromString(dllink, default_Extension);
            /* Set final filename! */
            downloadLink.setFinalFileName(filename + ext);
            URLConnectionAdapter con = null;
            br2.setFollowRedirects(true);
            try {
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    br2.followConnection();
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            /* Possible rtmp download or plugin failure --> Do NOT set final filename! */
            downloadLink.setName(filename + ext);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.startsWith("http")) {
            /* 99% use http - e.g. homemoviestube.com */
            downloadLink.setFinalFileName(downloadLink.getName());
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            /* E.g. fetishbox.com, bondagebox.com */
            final String swfvfy = br.getRegex("new SWFObject\\(\"(http[^<>\"]*?\\.swf)\"").getMatch(0);
            if (rtmpurl == null || swfvfy == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String app = new Regex(rtmpurl, "([a-z0-9]+/)$").getMatch(0);
            if (app == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.endsWith(".mp4")) {
                /* e.g. http://www.fetishbox.com/videos/blonde-bitchy-bratty-barbie--35044.html */
                dllink = "mp4:" + dllink;
                /* Correct filename */
                downloadLink.setFinalFileName(downloadLink.getName().replace(".flv", ".mp4"));
            } else {
                dllink = "flv:" + dllink;
                /* Correct filename */
                downloadLink.setFinalFileName(downloadLink.getName().replace(".mp4", ".flv"));
            }
            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(rtmpurl);
            rtmp.setPlayPath(dllink);
            rtmp.setApp(app);
            rtmp.setFlashVer("WIN 18,0,0,203");
            rtmp.setSwfVfy(swfvfy);
            rtmp.setResume(false);
            ((RTMPDownload) dl).startDownload();
        }
    }

    private String regexStandardTitleWithHost(final String host) {
        final String[] hostparts = host.split("\\.");
        final String host_relevant_part = hostparts[0];
        String site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + host + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (site_title == null) {
            site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) at " + host + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (site_title == null) {
            site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) at " + host_relevant_part + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        return site_title;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript4;
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
