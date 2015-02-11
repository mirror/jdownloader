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
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "viva.tv", "funnyclips.cc", "comedycentral.tv", "nick.de", "nicknight.de", "nickjr.de", "mtv.de", "mtviggy.com", "mtv.com", "movies.mtv.de", "southpark.de", "southpark.cc.com" }, urls = { "http://www\\.viva\\.tv/(musikvideo|news|shows|musik/video)/\\d+([a-z0-9\\-]+)?", "http://de\\.funnyclips\\.cc/(listen/.+|[A-Za-z0-9\\-]+/\\d+[A-Za-z0-9\\-]+)", "http://www\\.comedycentral\\.tv/(shows|neuigkeiten)/\\d+([a-z0-9\\-]+)?", "http://www\\.nick\\.de/shows/\\d+[a-z0-9\\-]+(/videos/\\d+[a-z0-9\\-]+)?", "http://www\\.nicknight\\.de/shows/\\d+[a-z0-9\\-]+(/videos/\\d+[a-z0-9\\-]+)?", "http://www\\.nickjr\\.de/videos/\\d+([a-z0-9\\-]+)?", "http://www\\.mtv\\.de/(shows/\\d+[a-z0-9\\-]+/staffeln/\\d+/folgen/\\d+[a-z0-9\\-]+|artists/[a-z0-9\\-]+/videos/[a-z0-9\\-]+|news/\\d+[a-z0-9\\-]+)",
        "http://www\\.mtviggy_jd_decrypted_jd_\\.com/videos/[a-z0-9\\-]+/|http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtviggy\\.com:\\d+", "http://www\\.mtv\\.com/(shows/[a-z0-9\\-]+/[^<>\"]+|videos/[^<>\"]+\\.jhtml|videos/\\?vid=\\d+)|http://media\\.mtvnservices\\.com/embed/mgid:uma:video:mtv\\.com:\\d+", "http://movies\\.mtv\\.de/(?!playlists)videos/(trailer/)?[a-z0-9\\-]+/[a-z0-9]+", "http://www\\.southpark\\.de/clips/[a-z0-9]+/[a-z0-9\\-]+|http://media\\.mtvnservices\\.com/mgid:arc:video:southparkstudios\\.com:[a-z0-9\\-]+", "http://media\\.mtvnservices\\.com/mgid:arc:video:southparkstudios_jd_decrypted_jd_\\.com:[a-z0-9\\-]+" }, flags = { 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32 })
public class VivaTv extends PluginForHost {

    public VivaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viva.tv/agb";
    }

    /* The different linktypes: */
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
    private static final String  type_mtvmovies                    = "http://movies\\.mtv\\.de/.+";

    private static final String  type_southpark_de_clips           = "http://www\\.southpark\\.de/clips/[a-z0-9]+/[a-z0-9\\-]+";
    /* Links come from the decrypter */
    private static final String  type_southpark_de_embed           = "http://media\\.mtvnservices\\.com/mgid:arc:video:southparkstudios\\.com:[a-z0-9\\-]+";
    private static final String  type_southpark_de_episode         = "http://www\\.southpark\\.de/alle\\-episoden/s\\d{2}e\\d{2}[a-z0-9\\-]+";
    private static final String  type_southpark_cc_episode         = "http://southpark\\.cc\\.com/full\\-episodes/s\\d{2}e\\d{2}[a-z0-9\\-]+";

    /** Other: So far unsupported domains: mtvla.com */

    /* Plugin related things */
    private static final String  player_url                        = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
    /* Obey german law - very important! */
    private static final boolean rtmpe_supported                   = false;
    public static final String   default_ext                       = ".flv";

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
    /* Note: There might also be a way to get mobile (http) links, see YT-dl project. */

    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */

    private String               mgid                              = null;
    private String               feed_url                          = null;
    private String               mediagen_url                      = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        String user_added_url = link.getDownloadURL();
        /* Correct decrypted links */
        user_added_url = user_added_url.replace("_jd_decrypted_jd_", "");
        link.setUrlDownload(user_added_url);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = null;
        String ext = null;
        String description = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String decrypter_mainlink = link.getStringProperty("mainlink", null);
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
                String h2 = br.getRegex("class=\\'now_playing\\'.*?<h2>([^<>]*?)</h2>.*?class=\\'kobra-watch-count\\'").getMatch(0);
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
            filename = getFilenameMTVIGGY(this.br);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ext = ".flv";
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
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            feed_url = getFEEDurl("mtv.com");
            br.getPage(feed_url);
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_mtviggy_embedded)) {
            /* Handle embedded links just like they are - we do not even want to try to find/use the original video url. */
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            feed_url = getFEEDurl("mtvworldwide");
            br.getPage(feed_url);
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = default_ext;
        } else if (link.getDownloadURL().matches(type_mtvmovies)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("class=\"mtvn\\-player") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"/>").getMatch(0);
            ext = ".flv";
        } else if (link.getDownloadURL().matches(type_southpark_de_clips)) {
            br.getPage(link.getDownloadURL());
            if (!br.containsHTML("\"name\":\"mtvnPlayer\"") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String season = br.getRegex("<h2 class=\"clips_thumb_season\">([^<>]*?)</h2>").getMatch(0);
            final String episodename = br.getRegex("<h2>([^<>]*?)</h2>").getMatch(0);
            String clipname = br.getRegex("<h1 itemprop=\"name\">([^<>]*?)</h1>").getMatch(0);
            if (season == null || episodename == null || clipname == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            clipname = doFilenameEncoding(clipname);
            filename = doFilenameEncoding(season) + " - " + doFilenameEncoding(episodename) + " - " + clipname;
            /* 'Akt?' because sometimes they simply forgot the 't' */
            if (clipname.matches(".+\\- (\\d\\. Akt?|Act \\d)$") && !rtmpe_supported) {
                /* Special case: Define the mediagenURL here because we need to enforce usage of the http protocol. */
                find_mgid("southparkstudios.com");
                if (this.mgid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                this.mediagen_url = String.format(mediagenURLs.get("southpark.de_episode"), this.mgid, possibleAcceptMethodsValues.get("http"));
            }
            ext = default_ext;
        } else if (decrypter_mainlink != null && decrypter_mainlink.matches(type_southpark_de_episode)) {
            /* Handle embedded links just like they are - we do not even want to try to find/use the original video url. */
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            feed_url = getFEEDurl("southpark.de");
            br.getPage(feed_url);
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = default_ext;
            if (!rtmpe_supported) {
                /* Special case: Define the mediagenURL here because we need to enforce usage of the http protocol. */
                this.mediagen_url = String.format(mediagenURLs.get("southpark.de_episode"), this.mgid, possibleAcceptMethodsValues.get("http"));
            }
        } else if (decrypter_mainlink != null && decrypter_mainlink.matches(type_southpark_cc_episode)) {
            /* Handle embedded links just like they are - we do not even want to try to find/use the original video url. */
            mgid = new Regex(link.getDownloadURL(), "(mgid.+)").getMatch(0);
            feed_url = getFEEDurl("southpark.cc.com");
            br.getPage(feed_url);
            /* Check for invalid XML --> Video must be offline then */
            if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetTitle();
            description = feedGetDescription();
            ext = default_ext;
        }
        if (filename == null) {
            logger.warning("Unsupported url format or plugin broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = doFilenameEncoding(filename);
        link.setFinalFileName(filename + ext);

        if (description != null && description.length() > 20) {
            description = doEncoding(description);
            try {
                link.setComment(description);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        downloadLink.setProperty("page_url", br.getURL());
        /*
         * In case we got embedded links, we are already on the feed_url. Find- and access it if still needed.
         */
        if (feed_url == null && mediagen_url == null) {
            findFEEDurl(downloadLink);
            if (feed_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Referer", player_url);
            br.getPage(feed_url);
        }
        if (mediagen_url == null) {
            /* Find- and access mediagen. */
            mediagen_url = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?mediaGen[^<>\"]*?)(?:\\'|\")").getMatch(0);
            if (mediagen_url == null) {
                mediagen_url = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?/mediagen/[^<>\"/]*?)(?:\\'|\")").getMatch(0);
            }
            if (mediagen_url == null) {
                /* Check if maybe we just got a nearly empty response --> Video won't load in the browser either! */
                if (br.containsHTML("<rss xmlns:media=")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - video offline?");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            mediagen_url = fixMediagenURL(mediagen_url);
        }

        br.getHeaders().put("Referer", player_url);
        br.getPage(mediagen_url);

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
        final String[] srcs = br.getRegex("([a-z]+://[^<>\"]*?)</src>").getColumn(0);
        if (srcs == null || srcs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Now get the best quality (highest width) */
        String src_url = null;
        int best_width = 0;
        int tempwidth = 0;
        for (final String tmpsrc : srcs) {
            final String width = new Regex(tmpsrc, "_(\\d+)x\\d+_").getMatch(0);
            if (width != null) {
                tempwidth = Integer.parseInt(width);
                if (tempwidth > best_width) {
                    best_width = tempwidth;
                    src_url = tmpsrc;
                }
            }
        }
        /* No width given? Grab the last element of the array - if this is not the best resolution, improve the upper function. */
        if (src_url != null) {
            logger.info("Found BEST downloadlink");
        } else {
            logger.info("Failed to find BEST downloadlink");
            src_url = srcs[srcs.length - 1];
        }
        String httpurl;
        if (src_url.startsWith("http")) {
            /* In very rare cases we already have http urls */
            httpurl = src_url;
        } else {
            httpurl = convertRTMPtoHTTP(src_url);
        }
        if (httpurl != null) {
            /* Prefer http */
            downloadHTTP(downloadLink, httpurl);
        } else {
            downloadRTMP(downloadLink, src_url);
        }
    }

    @SuppressWarnings("deprecation")
    private void findFEEDurl(final DownloadLink downloadLink) throws PluginException {
        if (downloadLink.getDownloadURL().matches(type_viva) || downloadLink.getDownloadURL().matches(type_funnyclips) || downloadLink.getDownloadURL().matches(type_comedycentral) || downloadLink.getDownloadURL().matches(type_nick) || downloadLink.getDownloadURL().matches(type_nicknight) || downloadLink.getDownloadURL().matches(type_nickjr) || downloadLink.getDownloadURL().matches(type_mtv_de)) {
            find_mgid("mtvnn.com");
            if (this.mgid != null) {
                feed_url = getFEEDurl("ALL_OTHERS");
            } else {
                /* Should never be needed */
                feed_url = br.getRegex("mrss[\t\n\r ]+:[\t\n\r ]+(?:\\'|\")(https?://[^<>\"]*?)(?:\\'|\"),").getMatch(0);
            }
        } else if (downloadLink.getDownloadURL().matches(type_mtviggy)) {
            /* Special: This domain has it's own feed-URL. */
            find_mgid(downloadLink.getHost());
            feed_url = getFEEDurl("mtvworldwide");
        } else if (downloadLink.getDownloadURL().matches(type_mtv_com)) {
            /* Special: This domain has it's own feed-URL. */
            find_mgid(downloadLink.getHost());
            feed_url = this.getFEEDurl("mtv.com");
        } else if (downloadLink.getDownloadURL().matches(type_mtvmovies)) {
            /* Special: This domain has it's own feed-URL. */
            find_mgid("mtvmovies.com");
            feed_url = getFEEDurl("mtvmovies.com");
        } else if (downloadLink.getDownloadURL().matches(type_southpark_de_clips)) {
            /* Special: This domain has it's own feed-URL. */
            find_mgid("southparkstudios.com");
            feed_url = getFEEDurl("southpark.de");
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
    }

    private void downloadHTTP(final DownloadLink downloadLink, final String http_src) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, http_src, true, 0);
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
    }

    @SuppressWarnings("deprecation")
    private void downloadRTMP(final DownloadLink downloadLink, String rtmp_src) throws Exception {
        if (!rtmpe_supported) {
            /* Works in most cases. */
            rtmp_src = rtmp_src.replace("rtmpe://", "rtmp://");
        }
        final String ext = rtmp_src.substring(rtmp_src.lastIndexOf(".") + 1);
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
            playpath = new Regex(rtmp_src, "/(gsp\\..+)").getMatch(0);
            if (playpath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!rtmpe_supported) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "rtmpe:// not supported!");
            }
            rtmp_src = rtmp_src.replace("rtmp://", "rtmpe://");
        } else {
            swfurl = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
            app = new Regex(rtmp_src, "(ondemand/(?:(mtviestor|riptide)/)?)").getMatch(0);
            final Regex host_app = new Regex(rtmp_src, "(rtmp://[^/]*?/)ondemand/(.+)");
            rtmphost = host_app.getMatch(0);
            playpath = new Regex(rtmp_src, app + "(.+)").getMatch(0);
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
        rtmp.setPageUrl(downloadLink.getStringProperty("page_url", null));
        rtmp.setUrl(rtmp_src);
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

    private String feedGetTitle() {
        return feedGetTitle(this.br.toString());
    }

    public static String feedGetTitle(final String source) {
        String title = new Regex(source, "<media:title><\\!\\[CDATA\\[([^<>]*?)\\]\\]></media:title>").getMatch(0);
        if (title == null) {
            title = new Regex(source, "<media:title>([^<>]*?)</media:title>").getMatch(0);
        }
        return title;
    }

    private String feedGetDescription() {
        return feedGetDescription(this.br.toString());
    }

    public static String feedGetDescription(final String source) {
        String description = new Regex(source, "<media:description>[\t\n\r ]+<\\!\\[CDATA\\[(.*?)\\]\\]>[\t\n\r ]+</media:description>").getMatch(0);
        if (description == null) {
            description = new Regex(source, "<media:description>(.*?)</media:description>").getMatch(0);
        }
        return description;
    }

    /**
     * Sometimes there are spaces at the end of the link, undefined parameters or it's simply url encoded though we need it plain --> This
     * can lead to 404 errors! --> This function takes care of these problems.
     */
    private String fixMediagenURL(String feedurl) {
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

    /** Returns a mediagen-feed URL based on the domain. */
    private String getMEDIAGENurl(final String domain) throws PluginException {
        if (mgid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String feedurl = mediagenURLs.get(domain);
        feedurl = String.format(feedurl, mgid);
        return feedurl;
    }

    private void find_mgid(final String host) {
        mgid = br.getRegex("(mgid[a-z:]+" + host + ":[A-Za-z0-9_\\-]+)").getMatch(0);
    }

    /** Converts rtmp urls to http urls */
    private String convertRTMPtoHTTP(final String rtmpurl) {
        String httpurl = null;
        final String important_part = new Regex(rtmpurl, "rtmpe?://[^<>\"]+/(gsp\\..+)").getMatch(0);
        if (important_part != null) {
            /* Most times used for mtviggy.com */
            /* Also possible: http://a[1-20].akadl.mtvnservices.com/ depending on server/link structure */
            httpurl = "http://viacommtvstrmfs.fplive.net/" + important_part;
        } else if (rtmpurl.matches("rtmpe?://cp\\d+\\.edgefcs\\.net/ondemand/riptide/r2/.+")) {
            /* Most times used for gameone.de */
            /* Using (these particular) http urls will sometimes lead to 403/404 server errors. */
            httpurl = rtmpurl.replaceAll("^.*?/r2/", "http://cdn.riptide-mtvn.com/r2/");
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

    public static String doFilenameEncoding(String filename) {
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

    public static String getFilenameMTVIGGY(final Browser br) {
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title != null) {
            title = doFilenameEncoding(title);
        }
        return title;
    }

    /** Static list of FEED-urls. If one is missing they can be found by accessing the correct player-URL (see list below). */
    public static HashMap<String, String> feedURLs                    = new HashMap<String, String>() {
        {
            put("ALL_OTHERS", "http://api.mtvnn.com/v2/mrss.xml?uri=%s");
            put("mtvworldwide", "http://all.mtvworldverticals.com/feed-xml/?uri=%s");
            put("mtv.de", "http://movies.mtv.de/mrss/%s");
            put("mtvmovies.com", "http://movies.mtv.de/mrss/%s");
            put("mtv.com", "http://www.mtv.com/player/embed/AS3/rss/?uri=%s&ref=None");
            put("southpark.de", "http://www.southpark.de/feeds/video-player/mrss/%s");
            put("southpark.cc.com", "http://southpark.cc.com/feeds/video-player/mrss/%s");
            put("gameone.de", "http://www.gameone.de/api/mrss/");
            put("gameone.de_2", "https://gameone.de/api/mrss/");
        }
    };

    /** Static list of mediagen URLs. These are usually sub-URLs of feed-urls and they'll return the final downloadlinks. */
    /**
     * Possible parameters of mediagen URLs:
     *
     * uri=<mgid> --> The only parameter we really need. It contains the information which video we want to download.
     *
     * suppressRegisterBeacon=true|false --> Meaning unclear
     *
     * lang=de|en --> Preferred language
     *
     * acceptMethods= see 'possibleAcceptMethodsValues' HashMap below --> Defines the streaming method we prefer
     *
     */
    public static HashMap<String, String> mediagenURLs                = new HashMap<String, String>() {
        {
            /*
             * For some of these, we have to access the feed- or player
             * before to get the mediagen-URL. This means that having the
             * mgid is not always enough to get the final URLs.
             */
            put("videos.mtv.com", "http://videos.mtvnn.com/mediagen/<some kinda hash (length = 32)>");
            /* Seems like this one is used for most big mtv sites as well */
            put("nick.de", "http://intl.esperanto.mtvi.com/www/xml/media/mediaGen.jhtml?uri=%s");
            put("mtv.com", "http://www.mtv.com/meta/context/mediaGen?uri=%s");
            put("southpark.de_episode", "http://www.southpark.de/feeds/video-player/mediagen?uri=%s&suppressRegisterBeacon=true&lang=de&acceptMethods=%s");
            put("southpark.de_clips", "http://www.southpark.de/feeds/video-player/mediagen?uri=%s");
        }
    };

    public static HashMap<String, String> embedURLs                   = new HashMap<String, String>() {
        {
            /*
             * Only a small amount if embeddable - usually embedded links
             * are never needed but via them we gan get the players url
             * which contains the feed-URL so this list might be useful in
             * the future. Strong format --> Put mgid in.
             */
            put("ALL_OTHERS", "http://media.mtvnservices.com/%s");
            put("mtv.com", "http://media.mtvnservices.com/embed/%s/");
        }
    };

    /**
     * These are only accessed for embedded videos. They contain the feed-URLs. This list might be useful in the future. Strong format:
     * 0=mgid without the actual ID (so it ends with ':'), 1=FULL mgid Example: http://media.mtvnservices
     * .com/pmt-arc/e1/players/mgid:uma:video:mtv.com:/context49 /config.xml?uri=mgid
     * :uma:video:mtv.com:1234567&type=normal&ref=None&geo=DE& group=music&network= None&device=Other&bParam=videoTypeId=31
     * &ownerOrgId=2&keywords =Some+keywords +in+this+format&keywords =Some+more+keywords+in+this+format&keywords
     * =Even+more+keywords+in+this+format
     */
    public static HashMap<String, String> playerURLs                  = new HashMap<String, String>() {
        {
            put("mtv.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context49/config.xml?uri=%s");
            put("southpark.de", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context5/config.xml?uri=%s");
        }
    };

    public static HashMap<String, String> possibleAcceptMethodsValues = new HashMap<String, String>() {
        {
            /*
             * "acceptMethods" is a parameter of mediagen URLs. It's
             * optional but has an influence on the final URLs.
             */
            /* Default seting (if ever used) */
            put("default", "fms,hdn1,hds");
            /*
             * Returns http links but less available qualities and usually
             * not as good as their rtmp(e) streams
             */
            put("http", "http");
            put("hls", "http");
            put("hds", "http");
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