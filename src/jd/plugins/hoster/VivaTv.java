//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "viva.tv", "funnyclips.cc", "comedycentral.tv", "nick.de", "nicknight.de", "nickjr.de", "mtv.de", "mtviggy.com", "mtvdesi.com", "mtvk.com", "mtv.com", "movies.mtv.de" }, urls = { "http://www\\.viva\\.tv/(musikvideo|news|shows|musik/video)/\\d+([a-z0-9\\-]+)?", "http://de\\.funnyclips\\.cc/(listen/.+|[A-Za-z0-9\\-]+/\\d+[A-Za-z0-9\\-]+)", "http://www\\.comedycentral\\.tv/(shows|neuigkeiten)/\\d+([a-z0-9\\-]+)?", "http://www\\.nick\\.de/shows/\\d+[a-z0-9\\-]+(/videos/\\d+[a-z0-9\\-]+)?", "http://www\\.nicknight\\.de/shows/\\d+[a-z0-9\\-]+(/videos/\\d+[a-z0-9\\-]+)?", "http://www\\.nickjr\\.de/videos/\\d+([a-z0-9\\-]+)?", "http://www\\.mtv\\.de/(shows/\\d+[a-z0-9\\-]+/staffeln/\\d+/folgen/\\d+[a-z0-9\\-]+|artists/[a-z0-9\\-]+/videos/[a-z0-9\\-]+|news/\\d+[a-z0-9\\-]+)",
        "http://www\\.mtviggy\\.com/videos/[a-z0-9\\-]+/|http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtviggy\\.com:\\d+", "http://www\\.mtvdesi\\.com/(videos/)?[a-z0-9\\-]+", "http://www\\.mtvk\\.com/videos/[a-z0-9\\-]+", "http://www\\.mtv\\.com/(shows/[a-z0-9\\-]+/[^<>\"]+|videos/[^<>\"]+\\.jhtml|videos/\\?vid=\\d+)|http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtv\\.com:\\d+", "http://movies\\.mtv\\.de/(?!playlists)videos/(trailer/)?[a-z0-9\\-]+/[a-z0-9]+" }, flags = { 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32 })
public class VivaTv extends PluginForHost {

    public VivaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viva.tv/agb";
    }

    private static final String  type_viva                         = "http://www\\.viva\\.tv/.+";
    private static final String  subtype_viva_news                 = "http://www\\.viva\\.tv/news/\\d+([a-z0-9\\-]+)?";
    private static final String  subtype_viva_shows                = "http://www\\.viva\\.tv/shows/\\d+([a-z0-9\\-]+)?";
    private static final String  subtype_viva_musicvideo           = "http://www\\.viva\\.tv/musikvideo/\\d+([a-z0-9\\-]+)?";
    private static final String  subtype_viva_music_interviews     = "http://www\\.viva\\.tv/musik/video/\\d+([a-z0-9\\-]+)?";

    private static final String  type_funnyclips                   = "http://de\\.funnyclips\\.cc/.+";

    private static final String  type_comedycentral                = "http://www\\.comedycentral\\.tv/.+";
    private static final String  subtype_shows_comedycentral       = "http://www\\.comedycentral\\.tv/shows/\\d+([a-z0-9\\-]+)?";
    private static final String  subtype_neuigkeiten_comedycentral = "http://www\\.comedycentral\\.tv/neuigkeiten/\\d+([a-z0-9\\-]+)?";

    private static final String  type_nick                         = "http://www\\.nick\\.de/.+";
    private static final String  type_nicknight                    = "http://www\\.nicknight\\.de/.+";
    private static final String  type_nickjr                       = "http://www\\.nickjr\\.de/.+";

    private static final String  type_mtv_de                       = "http://www\\.mtv\\.de/.+";

    private static final String  type_mtv_com                      = "http://www\\.mtv\\.com/.+";
    private static final String  type_mtv_com_embedded             = "http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtv\\.com:\\d+";
    private static final String  subtype_mtv_com_shows             = "http://www\\.mtv\\.com/shows/.+";
    private static final String  subtype_mtv_com_videos            = "http://www\\.mtv\\.com/(videos/[^<>\"]+\\.jhtml|videos/\\?vid=\\d+)";

    private static final String  type_mtviggy                      = "http://www\\.mtviggy\\.com/videos/[a-z0-9\\-]+/";
    private static final String  type_mtviggy_embedded             = "http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtviggy\\.com:\\d+";
    private static final String  type_mtvdesi                      = "http://www\\.mtvdesi\\.com/(videos/)?[a-z0-9\\-]+";
    private static final String  type_mtvk                         = "http://www\\.mtvk\\.com/videos/[a-z0-9\\-]+";
    private static final String  type_mtvmovies                    = "http://movies\\.mtv\\.de/.+";

    private static final String  mtv_player                        = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
    /* Obey german law - very important! */
    private static final boolean rtmpe_supported                   = false;

    /*
     * EVERY MTV project has mgid strings! Core of this is either a (6-7 digits?) long ID or a hash-like id e.g. xxx-yyy-ggg-hhh-ttt. Only
     * mgids with the short IDs can be embedded into other websites (as far as I've seen it).
     */
    /*
     * About feeds: Feeds are the comon way to get the video urls. Each MTV site has different feed-URLs. The easiest way to find them is
     * accessing the "players" page e.g.:
     * http://media.mtvnservices.com/pmt-arc/e1/players/mgid:arc:video:mtvmovies.com:/context16/context2/config
     * .xml?uri=<MGID_GOES_HERE>&type=network&ref=movies.mtv.de&geo=DE&group=intl&network=None&device=Othe. Feed url will be in the <feed>
     * XML tag.
     */
    /*
     * Known issues: External embedded urls (vevo.com) support missing, some mtv.com full episodes are not downloaded - instead, a teaser is
     * downloaded., Also see "TODO" things inside plugin code.
     */
    /* Note: There might also be a way to get mobile (http) links, see YT-dl project. */

    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */

    private String               mgid                              = null;
    private boolean              feed_active                       = false;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String user_added_url = link.getDownloadURL();
        if (user_added_url.matches(type_mtvdesi) || user_added_url.matches(type_mtvk)) {
            /* mtvdesi and mtvk only contains mtvniggy embedded links */
            link.setUrlDownload("http://www.mtviggy.com/videos/" + new Regex(user_added_url, "([a-z0-9\\-]+)$").getMatch(0) + "/");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = null;
        String ext = null;
        String description = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(type_viva)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("player\\.mtvnn\\.com/") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) \\- Musikvideo \\- VIVA\\.tv</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title: \\'([^<>]*?)\\',").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<h1 class=\\'title\\'>([^<>]*?)</h1>").getMatch(0);
            }
            if (link.getDownloadURL().matches(subtype_viva_shows) || link.getDownloadURL().matches(subtype_viva_musicvideo) || link.getDownloadURL().matches(subtype_viva_music_interviews)) {
                String h2 = br.getRegex("class=\\'now_playing\\'.*?<h2>([^<>]*?)</h2>.+data\\foreign_type").getMatch(0);
                if (h2 == null) {
                    h2 = br.getRegex("\">([^<>]*?)</a></h2>").getMatch(0);
                }
                String h3 = br.getRegex("class=\\'now_playing\\'.*?<h3>([^<>]*?)</h3>").getMatch(0);
                if (h3 == null) {
                    h3 = br.getRegex("\">([^<>]*?)</a></h3>").getMatch(0);
                }
                if (filename == null || h2 == null || h3 == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename += " - " + doFilenameEncoding(h2) + " - " + doFilenameEncoding(h3);
            }
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_funnyclips)) {
            try {
                br.getPage(link.getDownloadURL());
            } catch (final BrowserException e) {
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw e;
            }
            if (!br.containsHTML("class=\\'player\\'>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String show = br.getRegex("class=\\'franchise_title\\'>([^<>\"]*?)<").getMatch(0);
            String title = br.getRegex("<h2 class=\\'title\\'>([^<>\"]*?)</h2>").getMatch(0);
            if (show == null || title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            show = doFilenameEncoding(show);
            title = doFilenameEncoding(title);
            filename = show + " - " + title;
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_comedycentral)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) \\- Musikvideo \\- VIVA\\.tv</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title: \\'([^<>]*?)\\',").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<h1 class=\\'title\\'>([^<>]*?)</h1>").getMatch(0);
            }
            if (link.getDownloadURL().matches(subtype_shows_comedycentral)) {
                String h2 = br.getRegex("class=\\'now_playing\\'.*?<h2>([^<>]*?)</h2>.+class=\\'kobra-watch-count\\'").getMatch(0);
                if (h2 == null) {
                    h2 = br.getRegex("\">([^<>]*?)</a></h2>").getMatch(0);
                }
                String h3 = br.getRegex("class=\\'now_playing\\'.*?<h3>([^<>]*?)</h3>").getMatch(0);
                if (h3 == null) {
                    h3 = br.getRegex("\">([^<>]*?)</a></h3>").getMatch(0);
                }
                if (filename == null || h2 == null || h3 == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename += " - " + doFilenameEncoding(h2) + " - " + doFilenameEncoding(h3);
            }
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_nicknight)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?)\\- Nicknight</title>").getMatch(0);
            final String sub_title = br.getRegex("<h4 class=\\'title\\'>([^<>\"]*?)</h4>").getMatch(0);
            if (sub_title != null) {
                filename += " - " + doFilenameEncoding(sub_title);
            }
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_nickjr)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>\"]*?)\\- kostenlose Videos \\- NickJR\\.de</title>").getMatch(0);
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_nick)) {
            String vid = new Regex(link.getDownloadURL(), "/videos/(\\d+)").getMatch(0);
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("swfobject\\.createCSS") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (vid == null) {
                vid = br.getRegex("data\\-key=\\'local_playlist\\-(\\d+)'").getMatch(0);
            }
            String show = br.getRegex("data\\-name=\\'([^<>\"]*?)\\'").getMatch(0);
            if (show == null) {
                /* Assuming we're on a playlist */
                show = br.getRegex("<h2 class=(?:\\'|\")row\\-title videos(?:\\'|\")>([^<>]*?) Videos[\t\n\r ]+</h2>").getMatch(0);
            }
            if (show == null) {
                /* Assuming we're on a playlist */
                show = br.getRegex("<h2 class=(?:\\'|\")row\\-title videos(?:\\'|\")>([^<>]*?) Videos[\t\n\r ]+</h2>").getMatch(0);
            }
            if (show == null && vid != null) {
                show = br.getRegex("data\\-item\\-id=\\'" + vid + "\\'>.*?class=\\'title\\'>([^<>]*?)</p>").getMatch(0);
            }
            String title = br.getRegex("playlist\\-\\d+\\' data\\-title=(?:\\'|\")([^<>\"]*?)(?:\\'|\")").getMatch(0);
            if (show == null || title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            show = doFilenameEncoding(show);
            title = doFilenameEncoding(title);
            filename = show + " - " + title;
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtviggy)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("class=\"video\\-box\"") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ext = ".mp4";
        } else if (link.getDownloadURL().matches(type_mtv_de)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("property=\"og:video\"") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<h1 class=\"page\\-title\">([^<>]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>]*?)\\- Shows \\- MTV</title>").getMatch(0);
            }
            final String artist = br.getRegex("\"artist_name\":\"([^<>\"]*?)\"").getMatch(0);
            if (artist != null) {
                filename = doFilenameEncoding(artist) + " - " + filename;
            }
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtv_com)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("MTVN\\.VIDEO\\.PLAYER\\.instance")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>\"]*?) \\| MTV</title>").getMatch(0);
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtv_com) || link.getDownloadURL().matches(type_mtv_com_embedded)) {
            feed_active = true;
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            br.getPage(getFEEDurl("mtv.com"));
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtviggy_embedded)) {
            feed_active = true;
            /* Handle embedded links just like they are - we do not even want to try to find the original video url. */
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            br.getPage(getFEEDurl("mtvworldwide"));
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtvmovies)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("class=\"mtvn\\-player") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"/>").getMatch(0);
            ext = ".flv";
        }
        if (filename == null) {
            logger.warning("Unsupported url format or plugin broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = doFilenameEncoding(filename);
        link.setFinalFileName(filename + ext);

        if (description != null) {
            description = doEncoding(description);
            try {
                link.setComment(description);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String pageurl = br.getURL();
        String feed_url = null;
        /*
         * In case we got embedded links, we are already on the feed_url. Find- and access it if needed.
         */
        if (!feed_active) {
            if (downloadLink.getDownloadURL().matches(type_viva) || downloadLink.getDownloadURL().matches(type_funnyclips) || downloadLink.getDownloadURL().matches(type_comedycentral) || downloadLink.getDownloadURL().matches(type_nick) || downloadLink.getDownloadURL().matches(type_nicknight) || downloadLink.getDownloadURL().matches(type_nickjr) || downloadLink.getDownloadURL().matches(type_mtv_de)) {
                find_mgid("mtvnn.com");
                if (this.mgid != null) {
                    feed_url = getFEEDurl("ALL_OTHERS");
                } else {
                    /* Should never be needed */
                    feed_url = br.getRegex("mrss[\t\n\r ]+:[\t\n\r ]+(?:\\'|\")(https?://[^<>\"]*?)(?:\\'|\"),").getMatch(0);
                }
            } else if (downloadLink.getDownloadURL().matches(type_mtviggy)) {
                /* TODO: Add a decrypter for mtviggy links to get the vevo links in there - then we can remove this part of the code. */
                if (br.containsHTML("videoplayer\\.vevo\\.com/")) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Vevo.com is not yet supported");
                }
                /* Special: This domain has it's own feed-URL. */
                find_mgid(downloadLink.getHost());
                feed_url = getFEEDurl("mtvworldwide");
            } else if (downloadLink.getDownloadURL().matches(type_mtv_com)) {
                /* Special: This domain has it's own feed-URL. */
                find_mgid(downloadLink.getHost());
                String seriesID = null;
                if (br.getURL().matches(subtype_mtv_com_shows)) {
                    seriesID = br.getRegex("seriesId = (\\d+);").getMatch(0);
                } else {
                    seriesID = "None";
                }
                final String instance = br.getRegex("MTVN\\.VIDEO\\.PLAYER\\.instance = \\'([a-z0-9]+)\\';").getMatch(0);
                if (seriesID == null || instance == null || this.mgid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                feed_url = feedURLs.get("mtv.com");
                feed_url = String.format(feed_url, this.mgid, instance, seriesID);
            } else if (downloadLink.getDownloadURL().matches(type_mtvmovies)) {
                /* Special: This domain has it's own feed-URL. */
                find_mgid("mtvmovies.com");
                feed_url = getFEEDurl("mtvmovies.com");
            } else {
                /* Unknown URL format - should never happen! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Usually we'll have the correct feedurl here */
            if (feed_url == null) {
                feed_url = br.getRegex("(https?://api\\.mtvnn\\.com/v2/mrss\\.xml\\?uri=[^<>\"\\'/]+)").getMatch(0);
            }
            if (feed_url == null) {
                feed_url = br.getRegex("\\&mrss=(https?://api\\.mtvnn\\.com/[^<>\"]*?)\"").getMatch(0);
            }
            if (feed_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Referer", mtv_player);
            br.getPage(feed_url);
        }

        /* Find- and access mediagen. */
        String mediagen = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?mediaGen[^<>\"]*?)(?:\\'|\")").getMatch(0);
        if (mediagen == null) {
            mediagen = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?/mediagen/[^<>\"/]*?)(?:\\'|\")").getMatch(0);
        }
        if (mediagen == null) {
            /* Check if maybe we just got a nearly empty response --> Video won't load in the browser either! */
            if (br.containsHTML("<rss xmlns:media=")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - video offline?");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        mediagen = fixFeedURL(mediagen);
        br.getHeaders().put("Referer", mtv_player);
        br.getPage(mediagen);

        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Video temporarily or forever offline */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 60 * 1000l);
        } else if (br.containsHTML("status=\"esiblocked\"") || br.containsHTML("/error_country_block")) {
            /* Geo block */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available from your location");
        } else if (br.containsHTML(">Sorry, this video is not found or no longer available due to date or rights restrictions")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video doesn't exist anymore (?)", 60 * 60 * 1000l);
        }
        /* Chose highest quality available */
        final String[] srcs = br.getRegex("([a-z]+://[^<>\"]*?\\.(mp4|flv))</src>").getColumn(0);
        if (srcs == null || srcs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Now get the best quality (highest width) */
        String final_rtmpurl = null;
        int best_width = 0;
        int tempwidth = 0;
        for (final String tmpsrc : srcs) {
            final String width = new Regex(tmpsrc, "_(\\d+)x\\d+_").getMatch(0);
            if (width != null) {
                tempwidth = Integer.parseInt(width);
                if (tempwidth > best_width) {
                    best_width = tempwidth;
                    final_rtmpurl = tmpsrc;
                }
            }
        }
        /* No width given? Grab the last element of the array - if this is not the best resolution, improve the upper function. */
        if (final_rtmpurl != null) {
            logger.info("Found BEST downloadlink");
        } else {
            logger.info("Failed to find BEST downloadlink");
            final_rtmpurl = srcs[srcs.length - 1];
        }
        final_rtmpurl = final_rtmpurl.replace(Encoding.Base64Decode("cnRtcGU6Ly8="), "rtmp://");
        String httpurl;
        if (final_rtmpurl.startsWith("http")) {
            /* In very rare cases we already have http urls */
            httpurl = final_rtmpurl;
        } else {
            httpurl = convertRTMPtoHTTP(final_rtmpurl);
        }
        if (httpurl != null) {
            /* Prefer http */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, httpurl, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            final String ext = final_rtmpurl.substring(final_rtmpurl.lastIndexOf(".") + 1);
            String app;
            String rtmphost;
            String playpath;
            String swfurl;
            if (downloadLink.getDownloadURL().matches(type_mtviggy)) {
                /* Usually the convertRTMPtoHTTP will work and this code is never executed. */
                /* Original swfurl contains a lof more info but it's not needed! */
                swfurl = "http://media.mtvnservices.com/player/prime/mediaplayerprime.2.5.3.swf?uri=" + mgid;
                app = "viacommtvstrm";
                rtmphost = "rtmpe://viacommtvstrmfs.fplive.net:1935/viacommtvstrm";
                playpath = new Regex(final_rtmpurl, "/(gsp\\..+)").getMatch(0);
                if (playpath == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!rtmpe_supported) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "rtmpe:// not supported!");
                }
                final_rtmpurl = final_rtmpurl.replace("rtmp://", "rtmpe://");
            } else {
                swfurl = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
                app = new Regex(final_rtmpurl, "(ondemand/(?:(mtviestor|riptide)/)?)").getMatch(0);
                final Regex host_app = new Regex(final_rtmpurl, "(rtmp://[^/]*?/)ondemand/(.+)");
                rtmphost = host_app.getMatch(0);
                playpath = new Regex(final_rtmpurl, app + "(.+)").getMatch(0);
                if (rtmphost == null || playpath == null || app == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                rtmphost += app;
            }
            playpath = ext + ":" + playpath;
            /* Small fix for serverside wrong rtmp urls */
            playpath = playpath.replace("_od_flv.flv", "_od_flv");
            try {
                dl = new RTMPDownload(this, downloadLink, rtmphost);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(pageurl);
            rtmp.setUrl(final_rtmpurl);
            rtmp.setApp(app);
            rtmp.setPlayPath(playpath);
            /* Make sure we're using the correct protocol! */
            if (!rtmpe_supported) {
                rtmp.setProtocol(0);
            }
            rtmp.setFlashVer("WIN 16,0,0,305");
            rtmp.setSwfVfy(swfurl);
            /* Our rtmp resuming isn't the best plus we got a lot of different servers so better disable resume to prevent errors. */
            rtmp.setResume(false);
            ((RTMPDownload) dl).startDownload();
        }
    }

    private String feedGetTitle() {
        String title = br.getRegex("<media:title><\\!\\[CDATA\\[([^<>]*?)\\]\\]></media:title>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<media:title>([^<>]*?)</media:title>").getMatch(0);
        }
        return title;
    }

    private String feedGetDescription() {
        String description = br.getRegex("<media:description>[\t\n\r ]+<\\!\\[CDATA\\[(.*?)\\]\\]>[\t\n\r ]+</media:description>").getMatch(0);
        if (description == null) {
            description = br.getRegex("<media:description>(.*?)</media:description>").getMatch(0);
        }
        return description;
    }

    /**
     * Sometimes there are spaces at the end of the link, undefined parameters or it's simply url encoded though we need it plain --> This
     * can lead to 404 errors! --> This function takes care of these problems.
     */
    private String fixFeedURL(String feedurl) {
        feedurl = Encoding.htmlDecode(feedurl);
        feedurl = feedurl.trim();
        // feedurl = feedurl.replace("device={device}", "device=other");
        return feedurl;
    }

    /** Returns a feed URL based on the domain. */
    private String getFEEDurl(final String domain) throws PluginException {
        if (mgid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String feedurl = feedURLs.get(domain);
        feedurl = String.format(feedurl, mgid);
        return feedurl;
    }

    private void find_mgid(final String host) {
        mgid = br.getRegex("(mgid[a-z:]+" + host + ":[A-Za-z0-9_\\-]+)").getMatch(0);
    }

    /** Converts rtmp urls to http urls. Works especially for mtviggy.com. */
    private String convertRTMPtoHTTP(final String rtmpurl) {
        String httpurl = null;
        final String important_part = new Regex(rtmpurl, "rt[a-z]+://[^<>\"]+/(gsp\\..+)").getMatch(0);
        if (important_part != null) {
            httpurl = "http://viacommtvstrmfs.fplive.net/" + important_part;
        }
        return httpurl;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "/");
        output = output.replace("\\", "");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private String doEncoding(String data) {
        data = Encoding.htmlDecode(data).trim();
        data = HTMLEntities.unhtmlentities(data);
        data = HTMLEntities.unhtmlAmpersand(data);
        data = HTMLEntities.unhtmlAngleBrackets(data);
        data = HTMLEntities.unhtmlSingleQuotes(data);
        data = HTMLEntities.unhtmlDoubleQuotes(data);
        data = data.replaceAll("&apos;", "'");
        return data;
    }

    private String doFilenameEncoding(String filename) {
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        filename = HTMLEntities.unhtmlentities(filename);
        filename = HTMLEntities.unhtmlAmpersand(filename);
        filename = HTMLEntities.unhtmlAngleBrackets(filename);
        filename = HTMLEntities.unhtmlSingleQuotes(filename);
        filename = HTMLEntities.unhtmlDoubleQuotes(filename);
        filename = filename.replaceAll("&apos;", "'");
        return filename;
    }

    /** Statis list of FEED-urls. If one is missing they can be found by accessing the correct player-URL (see list below). */
    public static HashMap<String, String> feedURLs     = new HashMap<String, String>() {
                                                           {
                                                               put("ALL_OTHERS", "http://api.mtvnn.com/v2/mrss.xml?uri=%s");
                                                               put("mtvworldwide", "http://all.mtvworldverticals.com/feed-xml/?uri=%s");
                                                               put("mtv.de", "http://movies.mtv.de/mrss/%s");
                                                               put("mtvmovies.com", "http://movies.mtv.de/mrss/%s");
                                                               put("mtv.com", "http://www.mtv.com/player/embed/AS3/rss/?uri=%s&ref=None&instance=%s&seriesId=%s");
                                                           }
                                                       };

    public static HashMap<String, String> mediagenURLs = new HashMap<String, String>() {
                                                           {
                                                               /*
                                                                * For all of these, we have to access the feed- or player before to get the
                                                                * mediagen-URL. This means that having the mhid is NOT enough to get the
                                                                * final URLs.
                                                                */
                                                               put("ALL_OTHERS", "http://videos.mtvnn.com/mediagen/<some kinda hash (length = 32)>");
                                                               put("nick.de", "http://intl.esperanto.mtvi.com/www/xml/media/mediaGen.jhtml?uri=%s");
                                                               put("mtv.com", "http://www.mtv.com/meta/context/mediaGen?uri=%s");
                                                           }
                                                       };

    public static HashMap<String, String> embedURLs    = new HashMap<String, String>() {
                                                           {
                                                               /*
                                                                * Only a small amount if embeddable - usually embedded links are never
                                                                * needed but via them we gan get the players url which contains the feed-URL
                                                                * so this list might be useful in the future. Strong format --> Put mgid in.
                                                                */
                                                               put("mtv.com", "http://media.mtvnservices.com/embed/%s/");
                                                           }
                                                       };

    public static HashMap<String, String> playerURLs   = new HashMap<String, String>() {
                                                           {
                                                               /*
                                                                * These are only accessed for embedded videos. They contain the feed-URLs.
                                                                * This list might be useful in the future. Strong format: 0=mgid without the
                                                                * actual ID (so it ends with ':'), 1=FULL mgid Example:
                                                                * http://media.mtvnservices
                                                                * .com/pmt-arc/e1/players/mgid:uma:video:mtv.com:/context49
                                                                * /config.xml?uri=mgid
                                                                * :uma:video:mtv.com:1234567&type=normal&ref=None&geo=DE&
                                                                * group=music&network=
                                                                * None&device=Other&bParam=videoTypeId=31&ownerOrgId=2&keywords
                                                                * =Some+keywords
                                                                * +in+this+format&keywords=Some+more+keywords+in+this+format&keywords
                                                                * =Even+more+keywords+in+this+format
                                                                */
                                                               put("mtv.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context49/config.xml?uri=%s");
                                                           }
                                                       };

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}