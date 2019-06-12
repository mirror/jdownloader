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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "welt.de" }, urls = { "https?://(?:www\\.)?welt\\.de/mediathek/.*?/(?:video|sendung)\\d+/[A-Za-z0-9\\-]+\\.html" })
public class WeltDeMediathek extends PluginForHost {
    public WeltDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://www.welt.de/services/article122129231/Nutzungsbedingungen-DIE-WELT-Digital.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_title = new Regex(link.getDownloadURL(), "/mediathek/(.+)\\.html").getMatch(0);
        final String json_source_videourl = this.br.getRegex("\"page\"\\s*?:\\s*?(\\{.*?\\}),\\s+").getMatch(0);
        /* Tags: schema.org */
        final String json_source_videoinfo = this.br.getRegex("<script[^>]*?type=\"application/ld\\+json[^>]*?\">(.*?)</script>").getMatch(0);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source_videoinfo);
        String filename = "";
        String title = (String) entries.get("headline");
        final String description = (String) entries.get("description");
        final String date_formatted = formatDate((String) entries.get("datePublished"));
        String organization = (String) JavaScriptEngineFactory.walkJson(entries, "publisher/name");
        if (StringUtils.isEmpty(organization)) {
            organization = "DIE WELT";
        }
        if (title == null) {
            title = url_title;
        }
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += organization + "_" + title;
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        /* Find downloadlink */
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source_videourl);
            dllink = (String) JavaScriptEngineFactory.walkJson(entries, "content/media/{0}/file");
            if (dllink == null) {
                dllink = br.getRegex("(https?://[^\"]*?([A-Za-z0-9_]+_),([0-9,]+)\\.mp4\\.csmil/master\\.m3u8)").getMatch(0);
            }
            if (dllink.contains(".m3u8")) {
                /* Convert hls --> http (sometimes required) */
                final Regex hlsregex = new Regex(dllink, "https?://.*?/(?:i/)?(.*?)/([A-Za-z0-9_]+_),([0-9,]+)\\.mp4\\.csmil/master\\.m3u8");
                /* Usually both IDs are the same */
                final String id1 = hlsregex.getMatch(0);
                final String id2 = hlsregex.getMatch(1);
                /* Bitrates from lowest to highest. */
                final String[] bitrates = hlsregex.getMatch(2).split(",");
                int highest = -1;
                for (String bitrate : bitrates) {
                    if (highest == -1 || Integer.parseInt(bitrate) > highest) {
                        highest = Integer.parseInt(bitrate);
                    }
                }
                if (id1 != null && id2 != null && highest > 0) {
                    dllink = String.format("https://weltn24lfthumb-a.akamaihd.net/%s/%s%s.mp4", id1, id2, highest);
                }
            }
        } catch (final Throwable e) {
            getLogger().log(e);
        }
        final String ext;
        if (dllink != null && !dllink.equals("")) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null && dllink.startsWith("http")) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.isOK() && !con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String html_error = this.br.getRegex("<p[^>]*?class=\"o-text c-catch-up-error__text\">([^<>\"]+)</p>").getMatch(0);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (html_error != null) {
            /* 2018-04-11: E.g. 'Diese Sendung ist zur Zeit aus lizenzrechtlichen Gründen nicht verfügbar.' */
            throw new PluginException(LinkStatus.ERROR_FATAL, html_error);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
    }

    private String formatDate(final String input) {
        /* 2016-07-25T15:29:07Z */
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
