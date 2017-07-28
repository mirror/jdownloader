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

import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "boyfriendtv.com", "ashemaletube.com", "pornoxo.com", "worldsex.com", "bigcamtube.com", "xogogo.com", "bigass.ws", "smv.to", "porneq.com", "cliplips.com" }, urls = { "https?://(?:www\\.)?boyfriendtv\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?ashemaletube\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?pornoxo\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?worldsex\\.com/videos/[a-z0-9\\-]+\\-\\d+(?:\\.html|/)?", "http://(?:www\\.)?bigcamtube\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?xogogo\\.com/videos/\\d+/[a-z0-9\\-]+\\.html", "http://(?:www\\.)?bigass\\.ws/videos/\\d+/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?smv\\.to/detail/[A-Za-z0-9]+", "https?://(?:www\\.)?porneq\\.com/video/\\d+/[a-z0-9\\-]+/?", "https?://(?:www\\.)?cliplips\\.com/videos/\\d+/[a-z0-9\\-]+/" })
public class UnknownPornScript5 extends PluginForHost {

    public UnknownPornScript5(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.1 */
    // other: Should work for all (porn) sites that use the "jwplayer" with http URLs: http://www.jwplayer.com/
    private static final String type_allow_title_as_filename = ".+FOR_WEBSITES_FOR_WHICH_HTML_TITLE_TAG_CONTAINS_GOOD_FILENAME.+";
    private static final String default_Extension            = ".mp4";
    /* Connection stuff */
    private static final int    free_maxdownloads            = -1;
    private boolean             resumes                      = true;
    private int                 chunks                       = 0;
    private String              dllink                       = null;
    private boolean             server_issues                = false;

    @Override
    public String getAGBLink() {
        return "http://www.boyfriendtv.com/tos.html";
    }

    private void setConstants(final Account account) {
        if (account == null) {
            if ("xogogo.com".equals(getHost())) {
                resumes = true;
                chunks = 1;
            } else {
                resumes = true;
                chunks = 0;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        final String host = downloadLink.getHost();
        br = new Browser();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHost().equals("bigcamtube.com") && br.toString().length() <= 100) {
            /*
             * 2017-01-20: Workaround for bug (same via browser). First request sets cookies but server does not return html - 2nd request
             * returns html.
             */
            br.getPage(downloadLink.getDownloadURL());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. responsecode 404: boyfriendtv.com */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Now lets find the url_filename as a fallback in case we cannot find the filename inside the html code. */
        String url_filename = null;
        final String[] urlparts = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/[^/]+/(.+)").getMatch(0).split("/");
        String url_id = null;
        for (String urlpart : urlparts) {
            if (urlpart.matches("\\d+")) {
                url_id = urlpart;
            } else {
                url_filename = urlpart;
                break;
            }
        }
        if (url_filename == null && url_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_filename == null) {
            url_filename = url_id;
        } else {
            url_filename = url_filename.replace(".html", "");
            if (url_id == null) {
                /* In case we have an ID, it might be in the url_filename --> Find it */
                /* First check if we find it at the beginning. */
                url_id = new Regex(url_filename, "^(\\d+\\-).+").getMatch(0);
                if (url_id == null) {
                    /* Secondly check if we find it at the end. */
                    url_id = new Regex(url_filename, ".+(\\-\\d+)$").getMatch(0);
                }
            }
            if (url_id != null) {
                /* Remove url_id from url_filename */
                url_filename = url_filename.replace(url_id, "");
            }
        }
        /* Make it look nicer! */
        url_filename = url_filename.replace("-", " ");
        String filename = regexStandardTitleWithHost(host);
        if (filename == null) {
            /* Works e.g. for: boyfriendtv.com, ashemaletube.com, pornoxo.com */
            filename = this.br.getRegex("<div id=\"maincolumn2\">\\s*?<h1>([^<>]*?)</h1>").getMatch(0);
        }
        if (filename == null && downloadLink.getDownloadURL().matches(type_allow_title_as_filename)) {
            filename = this.br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = url_filename;
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        downloadLink.setName(filename + default_Extension);
        getDllink();
        String ext = default_Extension;
        if (!inValidateDllink(this.dllink)) {
            filename = filename.trim();
            ext = getFileNameExtensionFromString(dllink);
            if (ext == null || ext.length() > 5) {
                ext = default_Extension;
            }
            /* Set final filename! */
            downloadLink.setFinalFileName(filename + ext);
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            try {
                if ("xogogo.com".equals(getHost())) {
                    dllink = Encoding.urlDecode(dllink, true) + "&start=0";
                    final HeadRequest gr = new HeadRequest(dllink);
                    gr.setURL(new URL(dllink));
                    con = br2.openRequestConnection(gr);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            downloadLink.setName(filename + ext);
        }
        return AvailableStatus.TRUE;
    }

    private void getDllink() throws PluginException {
        /* Find correct js-source, then find dllink inside of it. */
        String jwplayer_source = null;
        final String[] scripts = this.br.getRegex("<script[^>]*?>(.*?)</script>").getColumn(0);
        for (final String script : scripts) {
            if (script.contains("jwplayer")) {
                this.dllink = searchDllinkInsideJWPLAYERSource(script);
                if (this.dllink != null) {
                    jwplayer_source = script;
                    break;
                }
            }
        }
        if (jwplayer_source == null && dllink == null) {
            /*
             * No player found --> Chances are high that there is no playable content --> Video offline
             *
             * This can also be seen as a "last chance offline" errorhandling for websites for which the above offline-errorhandling doesn't
             * work!
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static String searchDllinkInsideJWPLAYERSource(final String jwplayer_source) {
        /* Source #1 */
        String dllink = new Regex(jwplayer_source, "('|\")file\\1:\\s*('|\")(http.*?)\\2").getMatch(2);
        if (inValidateDllink(dllink)) {
            /* E.g. worldsex.com */
            dllink = new Regex(jwplayer_source, "file[\t\n\r ]*?:[\t\n\r ]*?('|\")(http.*?)\\1").getMatch(1);
        }
        if (inValidateDllink(dllink)) {
            /*
             * E.g. kt_player + jwplayer (can also be KernelVideoSharingCom), example: xfig.net[dedicated plugin], cliplips.com)<br />
             * Important: Do not pickup the slash at the end!
             */
            dllink = new Regex(jwplayer_source, "var videoFile=\"(http[^<>\"]*)/\";").getMatch(0);
        }
        if (inValidateDllink(dllink)) {
            /* Check for multiple videoqualities --> Find highest quality */
            int maxquality = 0;
            String sources_source = new Regex(jwplayer_source, "(?:\")?sources(?:\")?\\s*?:\\s*?\\[(.*?)\\]").getMatch(0);
            if (sources_source != null) {
                sources_source = sources_source.replace("\\", "");
                final String[] qualities = new Regex(sources_source, "(file: \".*?)\n").getColumn(0);
                for (final String quality_info : qualities) {
                    final String p = new Regex(quality_info, "label:\"(\\d+)p").getMatch(0);
                    int pint = 0;
                    if (p != null) {
                        pint = Integer.parseInt(p);
                    }
                    if (pint > maxquality) {
                        maxquality = pint;
                        dllink = new Regex(quality_info, "file[\t\n\r ]*?:[\t\n\r ]*?\"(http[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        }
        return dllink;
    }

    public static boolean inValidateDllink(final String dllink) {
        if (dllink == null) {
            return true;
        } else if (dllink.endsWith(".vtt")) {
            /* We picked up the subtitle url instead of the video downloadurl! */
            return true;
        }
        return false;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        setConstants(null);
        requestFileInformation(downloadLink);
        if (inValidateDllink(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if ("xogogo.com".equals(getHost())) {
            dllink = Encoding.urlDecode(dllink, true) + "&start=0";
            final Request gr = new GetRequest(dllink);
            gr.setURL(new URL(dllink));
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, gr, resumes, chunks);
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
        }
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
    }

    private String regexStandardTitleWithHost(final String host) {
        final String[] hostparts = host.split("\\.");
        final String host_relevant_part = hostparts[0];
        String site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + Pattern.quote(host) + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (site_title == null) {
            site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) at " + Pattern.quote(host) + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
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
        return SiteTemplate.UnknownPornScript5;
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
