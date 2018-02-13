//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.WdrDeMediathek.WdrDeConfigInterface;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wdr.de", "one.ard.de", "sportschau.de" }, urls = { "https?://([a-z0-9]+\\.)?wdr\\.de/([^<>\"]+\\.html|tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp)", "https?://(?:www\\.)?one\\.ard\\.de/[^/]+/[a-z0-9]+\\.jsp\\?vid=\\d+", "https?://(?:www\\.)?sportschau\\.de/.*?\\.html" })
public class WdrDeDecrypt extends PluginForDecrypt {
    private static final String Q_LOW               = "Q_LOW";
    private static final String Q_MEDIUM            = "Q_MEDIUM";
    private static final String Q_HIGH              = "Q_HIGH";
    private static final String Q_720               = "Q_720";
    private static final String Q_BEST              = "Q_BEST";
    private static final String Q_SUBTITLES         = "Q_SUBTITLES";
    private static final String TYPE_INVALID        = "http://([a-z0-9]+\\.)?wdr\\.de/mediathek/video/sendungen/index\\.html";
    private static final String TYPE_ROCKPALAST     = "http://(www\\.)?wdr\\.de/tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp";
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>  all_known_qualities = Arrays.asList("http_720", "http_540", "http_360", "http_288", "http_270", "http_180");

    public WdrDeDecrypt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Other, unsupported linktypes:
     *
     * http://www1.wdr.de/daserste/monitor/videos/videomonitornrvom168.html
     */
    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = fixVideourl(param.toString());
        final String url_name = new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0);
        final String tvStationName = new Regex(parameter, "https?://(?:www(?:\\d+)?\\.)?([^\\.]+)\\.").getMatch(0);
        br.setFollowRedirects(true);
        if (parameter.matches(TYPE_ROCKPALAST)) {
            final DownloadLink dl = createDownloadlink("http://wdrdecrypted.de/?format=mp4&quality=1x1&hash=" + JDHash.getMD5(parameter));
            dl.setProperty("mainlink", parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.getPage(parameter);
        /* Are we on a video-information/overview page? We have to access the url which contains the player! */
        String videourl_forward = br.getRegex("class=\"videoLink\" >[\t\n\r ]+<a href=\"(/[^<>\"]*?)\"").getMatch(0);
        if (videourl_forward != null) {
            videourl_forward = fixVideourl(videourl_forward);
            this.br.getPage(videourl_forward);
        }
        /* fernsehen/.* links |mediathek/.* links */
        if (parameter.matches(TYPE_INVALID) || parameter.contains("filterseite-") || br.getURL().contains("/fehler.xml") || br.getHttpConnection().getResponseCode() == 404 || br.getURL().length() < 38) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(url_name);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String json_api_url;
        String date = null;
        String sendung = null;
        String episode_name = null;
        if (parameter.matches(".+(wdr\\.de|sportschau\\.de)/.+")) {
            final Regex inforegex = br.getRegex("(\\d{2}\\.\\d{2}\\.\\d{4}) \\| ([^<>]*?) \\| Das Erste</p>");
            date = inforegex.getMatch(0);
            sendung = br.getRegex("<strong>([^<>]*?)<span class=\"hidden\">:</span></strong>[\t\n\r ]+Die Sendungen im Überblick[\t\n\r ]+<span>\\[mehr\\]</span>").getMatch(0);
            if (sendung == null) {
                sendung = br.getRegex(">Sendungen</a></li>[\t\n\r ]+<li>([^<>]*?)<span class=\"hover\">").getMatch(0);
            }
            if (sendung == null) {
                sendung = br.getRegex("<li class=\"active\" >[\t\n\r ]+<strong>([^<>]*?)</strong>").getMatch(0);
            }
            if (sendung == null) {
                sendung = br.getRegex("<div id=\"initialPagePart\">[\t\n\r ]+<h1>[\t\n\r ]+<span>([^<>]*?)<span class=\"hidden\">:</span>").getMatch(0);
            }
            if (sendung == null) {
                sendung = br.getRegex("<title>([^<>]*?)\\- WDR Fernsehen</title>").getMatch(0);
            }
            if (sendung == null) {
                sendung = br.getRegex("class=\"ressort\">Startseite ([^<>\"]+)<").getMatch(0);
            }
            if (sendung == null) {
                sendung = br.getRegex("<a href=\"/fernsehen/([^<>\"/]+)/startseite/index\\.html\">Startseite</a>").getMatch(0);
            }
            if (sendung == null) {
                sendung = inforegex.getMatch(1);
            }
            episode_name = br.getRegex("</li><li>[^<>\"/]+: ([^<>]*?)<span class=\"hover\"").getMatch(0);
            if (episode_name == null) {
                episode_name = br.getRegex("class=\"hover\">:([^<>]*?)</span>").getMatch(0);
            }
            if (episode_name == null) {
                episode_name = br.getRegex("class=\"siteHeadline hidden\">([^<>]*?)<").getMatch(0);
            }
            json_api_url = this.br.getRegex("(?:\\'|\")mediaObj(?:\\'|\"):\\s*?\\{\\s*?(?:\\'|\")url(?:\\'|\"):\\s*?(?:\\'|\")(https?://[^<>\"]+\\.js)(?:\\'|\")").getMatch(0);
        } else {
            final String thisvideo_src = einsfestivalGetVideoSrc(this.br);
            sendung = einsfestivalGetTitleSubtitleWithErrorhandlingFromVideoSrc(thisvideo_src);
            json_api_url = new Regex(thisvideo_src, "adaptivePath\\s*?:\\s*?\\'(http://[^<>\"\\']+)\\'").getMatch(0);
        }
        if (sendung != null) {
            sendung = encodeUnicode(Encoding.htmlDecode(sendung).trim());
        }
        String plain_name = null;
        if (episode_name != null) {
            episode_name = Encoding.htmlDecode(episode_name).trim();
            episode_name = encodeUnicode(episode_name);
            if (sendung != null) {
                plain_name = sendung + " - " + episode_name;
            } else {
                plain_name = episode_name;
            }
        } else {
            if (sendung == null) {
                /* Finally fallback to url-information */
                sendung = url_name.replace(".html", "");
                sendung = encodeUnicode(sendung);
            }
            plain_name = sendung;
        }
        if (json_api_url == null) {
            /* No player --> No downloadable video/content */
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(url_name);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.getPage(json_api_url);
        final String finallink_audio = PluginJSonUtils.getJsonValue(this.br, "audioURL");
        /* Check for audio stream */
        if (finallink_audio != null && !finallink_audio.equals("")) {
            final DownloadLink audio = createDownloadlink("http://wdrdecrypted.de/?format=mp3&quality=0x0&hash=" + JDHash.getMD5(parameter));
            audio.setProperty("mainlink", parameter);
            audio.setProperty("direct_link", finallink_audio);
            audio.setProperty("plain_filename", plain_name + ".mp3");
            decryptedLinks.add(audio);
        } else {
            final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
            final WdrDeConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.WdrDeMediathek.WdrDeConfigInterface.class);
            final boolean grab_subtitle = cfg.isGrabSubtitleEnabled();
            final boolean grabBestWithinUserSelection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
            final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
            final boolean fastlinkcheck = cfg.isFastLinkcheckEnabled();
            boolean grab_180 = cfg.isGrabHTTP180pVideoEnabled();
            boolean grab_270 = cfg.isGrabHTTP270pVideoEnabled();
            boolean grab_288 = cfg.isGrabHTTP288pVideoEnabled();
            boolean grab_360 = cfg.isGrabHTTP360pVideoEnabled();
            boolean grab_540 = cfg.isGrabHTTP540pVideoEnabled();
            boolean grab_720 = cfg.isGrabHTTP720pVideoEnabled();
            boolean grab_best = cfg.isGrabBESTEnabled();
            List<String> all_selected_qualities = new ArrayList<String>();
            if (grab_180) {
                all_selected_qualities.add("http_180");
            }
            if (grab_270) {
                all_selected_qualities.add("http_270");
            }
            if (grab_288) {
                all_selected_qualities.add("http_288");
            }
            if (grab_360) {
                all_selected_qualities.add("http_360");
            }
            if (grab_540) {
                all_selected_qualities.add("http_540");
            }
            if (grab_720) {
                all_selected_qualities.add("http_720");
            }
            final boolean user_selected_nothing = all_selected_qualities.size() == 0;
            if (user_selected_nothing) {
                logger.info("User selected no quality at all --> Adding ALL qualities instead");
                all_selected_qualities = all_known_qualities;
            }
            String subtitle_url = null;
            String flashvars = null;
            if (date == null) {
                date = PluginJSonUtils.getJsonValue(this.br, "trackerClipAirTime");
            }
            flashvars = this.br.toString();
            subtitle_url = PluginJSonUtils.getJsonValue(flashvars, "captionURL");
            final String mediaType = PluginJSonUtils.getJson(this.br, "mediaType");
            if (!mediaType.equalsIgnoreCase("vod")) {
                /* E.g. 'live' */
                logger.info("Bad mediaType - nothing downloadable there");
                return decryptedLinks;
            }
            final String date_formatted = formatDate(date);
            /* We know how their http links look - this way we can avoid HDS/HLS/RTMP */
            /*
             * http://adaptiv.wdr.de/z/medp/ww/fsk0/104/1046579/,1046579_11834667,1046579_11834665,1046579_11834669,.mp4.csmil/manifest.f4
             */
            // //wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/139/1394333/,1394333_16295554,1394333_16295556,1394333_16295555,1394333_16295557,1394333_16295553,1394333_16295558,.mp4.csmil/master.m3u8
            final Regex hds_convert = new Regex(flashvars, "^[^/]*?//[^/]+/[a-z0-9]+/med[a-z0-9]+/.*?([a-z]+)/(fsk\\d+/\\d+/\\d+)/,([a-z0-9_,]+),\\.mp4\\.csmil/");
            String region = hds_convert.getMatch(0);
            final String fsk_url = hds_convert.getMatch(1);
            final String quality_string = hds_convert.getMatch(2);
            final String hls_master = new Regex(flashvars, "(//[^<>\"]+\\.m3u8[^<>\"]*?)").getMatch(0);
            if (region == null || fsk_url == null || quality_string == null || hls_master == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            region = correctRegionString(region);
            /* Access HLS master to find correct resolution for each ID ... */
            br.getPage("http:" + hls_master);
            final String[] resolutionsInOrder = this.br.getRegex("RESOLUTION=(\\d+x\\d+)").getColumn(0);
            final String[] qualities = quality_string.split(",");
            if (qualities == null || qualities.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (int counter = 0; counter <= qualities.length - 1; counter++) {
                if (counter > qualities.length - 1) {
                    break;
                }
                /* Old */
                // String final_url = "http://http-ras.wdr.de/CMS2010/mdb/ondemand/" + region + "/" + fsk_url + "/";
                /* 2016-02-16 new e.g. http://ondemand-ww.wdr.de/medp/fsk0/105/1058266/1058266_12111633.mp4 */
                /* 2018-02-13: new e.g. http://wdrmedien-a.akamaihd.net/medp/ondemand/de/fsk0/158/1580248/1580248_18183304.mp4 */
                String final_url = "http://wdrmedien-a.akamaihd.net/medp/ondemand/" + region + "/";
                final_url += fsk_url + "/";
                final String linkid = qualities[counter];
                final String resolution = resolutionsInOrder[counter];
                final String[] height_width = resolution.split("x");
                final String width = height_width[1];
                final_url += linkid + ".mp4";
                String final_video_name = "";
                if (date_formatted != null) {
                    final_video_name += date_formatted + "_";
                }
                final_video_name += tvStationName + "_" + plain_name + "_" + resolution + ".mp4";
                final DownloadLink dl_video = createDownloadlink("http://wdrdecrypted.de/?format=mp4&quality=" + resolution);
                dl_video.setProperty("mainlink", parameter);
                dl_video.setProperty("direct_link", final_url);
                dl_video.setProperty("plain_filename", final_video_name);
                dl_video.setProperty("plain_resolution", resolution);
                dl_video.setFinalFileName(final_video_name);
                dl_video.setContentUrl(parameter);
                dl_video.setLinkID(linkid);
                if (fastlinkcheck) {
                    dl_video.setAvailable(true);
                }
                all_found_downloadlinks.put("http_" + width, dl_video);
            }
            final HashMap<String, DownloadLink> all_selected_downloadlinks = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, grab_best, grabBestWithinUserSelection, grabUnknownQualities);
            /* Finally add selected URLs */
            final Iterator<Entry<String, DownloadLink>> it = all_selected_downloadlinks.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, DownloadLink> entry = it.next();
                final DownloadLink keep = entry.getValue();
                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && subtitle_url != null) {
                    String subtitle_filename = "";
                    if (date_formatted != null) {
                        subtitle_filename = subtitle_filename + "_";
                    }
                    subtitle_filename += "wdr_" + plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://wdrdecrypted.de/?format=xml&quality=" + resolution);
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setLinkID(keep.getLinkID() + "subtitle");
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    dl_subtitle.setContentUrl(parameter);
                    decryptedLinks.add(dl_subtitle);
                }
                decryptedLinks.add(keep);
            }
            final FilePackage fp = FilePackage.getInstance();
            String packagename = "";
            if (date_formatted != null) {
                packagename += date_formatted + "_";
            }
            packagename += tvStationName + "_" + plain_name;
            fp.setName(packagename);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private HashMap<String, DownloadLink> handleQualitySelection(final HashMap<String, DownloadLink> all_found_downloadlinks, final List<String> all_selected_qualities, final boolean grab_best, final boolean grab_best_out_of_user_selection, final boolean grab_unknown) {
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
        if (grab_best) {
            for (final String possibleQuality : this.all_known_qualities) {
                if (all_found_downloadlinks.containsKey(possibleQuality)) {
                    all_selected_downloadlinks.put(possibleQuality, all_found_downloadlinks.get(possibleQuality));
                    break;
                }
            }
            if (all_selected_downloadlinks.isEmpty()) {
                logger.info("Possible issue: Best selection found nothing --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            }
        } else {
            boolean atLeastOneSelectedItemExists = false;
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grab_unknown) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grab_unknown) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            } else {
                if (grab_best_out_of_user_selection) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
            }
        }
        return all_selected_downloadlinks;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_downloadlinks) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_downloadlinks.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_downloadlinks.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_downloadlinks;
        }
        return newMap;
    }

    // private String getVideoresolutionBasedOnIdPosition(final int position) {
    // final String resolution;
    // switch (position) {
    // case 0:
    // resolution = "960x540";
    // break;
    // case 1:
    // resolution = "640x360";
    // break;
    // case 2:
    // resolution = "512x288";
    // break;
    // default:
    // resolution = "WTF";
    // break;
    // }
    // return resolution;
    // }
    //
    // private String getQualitynameBasedOnIdPosition(final int position) {
    // final String resolution;
    // switch (position) {
    // case 0:
    // resolution = "Q_HIGH";
    // break;
    // case 1:
    // resolution = "Q_MEDIUM";
    // break;
    // case 2:
    // resolution = "Q_LOW";
    // break;
    // default:
    // resolution = "WTF";
    // break;
    // }
    // return resolution;
    // }
    private String fixVideourl(final String input) {
        String output = input;
        /* Remove unneeded url part */
        final String player_part = new Regex(input, "(\\-videoplayer(_size\\-[A-Z])?\\.html)").getMatch(0);
        if (player_part != null) {
            output = input.replace(player_part, ".html");
        }
        return output;
    }

    public static final String correctRegionString(final String input) {
        String output;
        if (input.equals("de")) {
            output = "de";
        } else {
            output = "weltweit";
        }
        return output;
    }

    private String formatDate(String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy HH:mm", Locale.GERMAN);
        } else if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        } else {
            /* 2015-06-23T20:15+02:00 --> 2015-06-23T20:15:00+0200 */
            input = input.substring(0, input.lastIndexOf(":")) + "00";
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mmZZ", Locale.GERMAN);
        }
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

    private static String einsfestivalGetTitleSubtitleFromVideoSrc(final String videosrc) {
        return new Regex(videosrc, "startAlt\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
    }

    private static String einsfestivalGetTitleSubtitleAlternativeFromVideoSrc(final String videosrc) {
        return new Regex(videosrc, "zmdbTitle\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
    }

    private static String einsfestivalGetTitleSubtitleWithErrorhandlingFromVideoSrc(final String videosrc) {
        final String title_subtitle_alternative = einsfestivalGetTitleSubtitleAlternativeFromVideoSrc(videosrc);
        String title_subtitle = einsfestivalGetTitleSubtitleFromVideoSrc(videosrc);
        /* Avoid extremely long filenames with unneeded information! */
        if (title_subtitle == null || title_subtitle.matches(".+(SENDER:|SENDETITEL:|UNTERTITEL:).+")) {
            title_subtitle = title_subtitle_alternative;
        }
        return title_subtitle;
    }

    private static String einsfestivalGetVideoSrc(final Browser br) {
        final String videoid = getVideoid(br.getURL());
        if (videoid == null) {
            return null;
        }
        return br.getRegex("arrVideos\\[" + videoid + "\\]\\s*?=\\s*?\\{(.*?)\\}").getMatch(0);
    }

    private static String getVideoid(final String url) {
        return new Regex(url, "(\\d+)$").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}