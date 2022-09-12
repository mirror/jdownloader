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
package jd.plugins.decrypter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rai.tv" }, urls = { "https?://[A-Za-z0-9\\.]*?(?:rai\\.tv|raiyoyo\\.rai\\.it)/.+day=\\d{4}\\-\\d{2}\\-\\d{2}.*|https?://[A-Za-z0-9\\.]*?(?:rai\\.tv|rai\\.it|raiplay\\.it)/.+\\.html|https?://(?:www\\.)?raiplay\\.it/programmi/.+" })
public class RaiItDecrypter extends PluginForDecrypt {
    public RaiItDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_DAY               = ".+day=\\d{4}\\-\\d{2}\\-\\d{2}.*";
    private static final String TYPE_RAIPLAY_PROGRAMMI = "https?://[^/]+/programmi/([^/]+).*";
    // private static final String TYPE_RAIPLAY_IT = "https?://.+raiplay\\.it/.+";
    private static final String TYPE_CONTENTITEM       = ".+/dl/[^<>\"]+/ContentItem\\-[a-f0-9\\-]+\\.html$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setFollowRedirects(true);
        br.setLoadLimit(this.br.getLoadLimit() * 3);
        if (param.getCryptedUrl().matches(TYPE_DAY)) {
            return crawlWholeDay(param);
        } else if (param.getCryptedUrl().matches(TYPE_RAIPLAY_PROGRAMMI)) {
            return crawlProgrammi(param);
        } else {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                return this.crawlSingleVideoNew(param);
            } else {
                return crawlSingleVideo(param);
            }
            // return crawlSingleVideo(param);
        }
    }

    /* Old channel config url (see also rev 35204): http://www.rai.tv/dl/RaiTV/iphone/android/smartphone/advertising_config.html */
    private ArrayList<DownloadLink> crawlWholeDay(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        final String mainlink_urlpart = new Regex(parameter, "(?:\\?|#)(.+)").getMatch(0);
        final String[] dates = new Regex(parameter, "(\\d{4}\\-\\d{2}\\-\\d{2})").getColumn(0);
        final String[] channels = new Regex(parameter, "ch=(\\d+)").getColumn(0);
        final String[] videoids = new Regex(parameter, "v=(\\d+)").getColumn(0);
        final String date_user_input = dates[dates.length - 1];
        final String date_user_input_underscore = date_user_input.replace("-", "_");
        final String date_user_input_in_json_format = convertInputDateToJsonDateFormat(date_user_input);
        final DownloadLink offline = this.createOfflinelink(parameter);
        final String filename_offline;
        if (mainlink_urlpart != null) {
            filename_offline = date_user_input + "_" + mainlink_urlpart + ".mp4";
        } else {
            filename_offline = date_user_input + ".mp4";
        }
        offline.setFinalFileName(filename_offline);
        String id_of_single_video_which_user_wants_to_have_only = null;
        String chnumber_str = null;
        if (channels != null && channels.length > 0) {
            chnumber_str = channels[channels.length - 1];
        }
        if (chnumber_str == null) {
            /* Small fallback */
            chnumber_str = "1";
        }
        if (videoids != null && videoids.length > 0) {
            id_of_single_video_which_user_wants_to_have_only = videoids[videoids.length - 1];
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(date_user_input_underscore);
        Map<String, Object> entries = null;
        Map<String, Object> entries2 = null;
        final String channel_name = "Rai" + chnumber_str;
        final String channel_name_with_space = "Rai " + chnumber_str;
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.getPage("http://www.rai.it/dl/palinsesti/Page-e120a813-1b92-4057-a214-15943d95aa68-json.html?canale=" + channel_name + "&giorno=" + date_user_input);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        /* Fix sometimes invalid json - very strange way of sending errors! */
        this.br.getRequest().setHtmlCode(this.br.toString().replaceAll("\\[an error occurred\\s*?while processing this directive\\s*?\\]", ""));
        Object parsedJson = JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        final List<Object> daysList;
        if (parsedJson instanceof HashMap) {
            entries2 = (Map<String, Object>) parsedJson;
            daysList = (List<Object>) entries2.get(channel_name_with_space);
        } else {
            entries = (Map<String, Object>) parsedJson;
            daysList = (List<Object>) entries.get(channel_name_with_space);
        }
        boolean foundUserDate = false;
        /* Walk through all days. */
        for (final Object dayO : daysList) {
            if (dayO instanceof HashMap) {
                entries2 = (Map<String, Object>) dayO;
            } else {
                entries = (Map<String, Object>) dayO;
            }
            final String date_of_this_item = (String) getObjectFromMap(entries, entries2, "giorno");
            if (date_of_this_item == null || !date_of_this_item.equals(date_user_input_in_json_format)) {
                /* Date is missing or not the date we want? Skip item! */
                continue;
            }
            foundUserDate = true;
            /* Get all items of the day. */
            final List<Object> itemsOfThatDayList = (List<Object>) getObjectFromMap(entries, entries2, "palinsesto");
            for (final Object itemsOfThatDayListO : itemsOfThatDayList) {
                if (itemsOfThatDayListO instanceof Map) {
                    entries2 = (Map<String, Object>) itemsOfThatDayListO;
                } else {
                    entries = (Map<String, Object>) itemsOfThatDayListO;
                }
                /* Get all programms of that day. */
                final List<Object> programmsList = (List<Object>) getObjectFromMap(entries, entries2, "programmi");
                /* Finally decrypt the programms. */
                for (final Object programmO : programmsList) {
                    if (programmO instanceof Map) {
                        entries2 = (Map<String, Object>) programmO;
                    } else {
                        entries = (Map<String, Object>) programmO;
                    }
                    if ((entries == null || entries.isEmpty()) && (entries2 == null || entries2.isEmpty())) {
                        continue;
                    }
                    final boolean hasVideo = ((Boolean) getObjectFromMap(entries, entries2, "hasVideo")).booleanValue();
                    final String webLink = (String) getObjectFromMap(entries, entries2, "webLink");
                    if (!hasVideo || webLink == null || !webLink.startsWith("/")) {
                        continue;
                    }
                    final String url_for_user;
                    final String url_rai_replay = new Regex(webLink, "raiplay/(video/.+)").getMatch(0);
                    if (url_rai_replay != null) {
                        url_for_user = "http://www.raiplay.it/" + url_rai_replay;
                    } else {
                        url_for_user = "http://www.rai.it" + webLink;
                    }
                    decryptedLinks.add(this.createDownloadlink(url_for_user));
                }
            }
        }
        if (!foundUserDate) {
            logger.info("Failed to find date which the user wanted --> Crawled nothing");
        }
        return decryptedLinks;
    }

    /** Crawls list of URLs which lead to single videos. */
    private ArrayList<DownloadLink> crawlProgrammi(final CryptedLink param) throws Exception {
        final String showNameURL = new Regex(param.getCryptedUrl(), TYPE_RAIPLAY_PROGRAMMI).getMatch(0);
        if (showNameURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        final String var1 = br.getRegex("block=\"(PublishingBlock-[a-f0-9\\-]+)\"").getMatch(0);
        String var2 = br.getRegex("\"id\"\\s*:\\s*\"(ContentSet-[a-f0-9\\-]+)\"").getMatch(0);
        if (var2 == null) {
            /* 2021-03-15: E.g. https://www.raiplay.it/programmi/report */
            var2 = br.getRegex("set=\"(ContentSet-[a-f0-9\\-]+)\"").getMatch(0);
        }
        if (var1 == null || var2 == null) {
            logger.info("Failed to find any downloadable content");
            return decryptedLinks;
        }
        br.getPage("/programmi/" + showNameURL + "/" + var1 + "/" + var2 + "/episodes.json");
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Object> seasonsO = (List<Object>) entries.get("seasons");
        for (final Object seasonO : seasonsO) {
            entries = (Map<String, Object>) seasonO;
            final List<Object> categoriesO = (List<Object>) entries.get("episodes");
            for (final Object categoryO : categoriesO) {
                entries = (Map<String, Object>) categoryO;
                final List<Object> itemsO = (List<Object>) entries.get("cards");
                for (final Object cardO : itemsO) {
                    entries = (Map<String, Object>) cardO;
                    final String type = (String) entries.get("type");
                    final boolean isLive = ((Boolean) entries.get("is_live")).booleanValue();
                    String contentURL = (String) entries.get("weblink");
                    if (!type.equalsIgnoreCase("RaiPlay Video Item") || isLive || StringUtils.isEmpty(contentURL)) {
                        /* Skip iunsupported items */
                        continue;
                    }
                    contentURL = br.getURL(contentURL).toString();
                    decryptedLinks.add(this.createDownloadlink(contentURL));
                }
            }
        }
        return decryptedLinks;
    }

    /* Get value from parsed json if we sometimes have LinkedHashMap and sometimes HashMap. */
    final Object getObjectFromMap(final Map<String, Object> entries, final Map<String, Object> entries2, final String key) {
        final Object jsono;
        if (entries2 != null) {
            jsono = entries2.get(key);
        } else if (entries != null) {
            jsono = entries.get(key);
        } else {
            jsono = null;
        }
        return jsono;
    }

    /** 2021-03-15: New approach */
    private ArrayList<DownloadLink> crawlSingleVideoNew(final CryptedLink param) throws DecrypterException, Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl().replace(".html", ".json"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> trackInfo = (Map<String, Object>) entries.get("track_info");
        final Map<String, Object> showInfo = (Map<String, Object>) entries.get("program_info");
        String title = (String) entries.get("name");
        final String showTitle = (String) showInfo.get("name");
        String episodeTitle = (String) entries.get("episode_title");
        String extension = "mp4";
        String date_formatted = (String) trackInfo.get("date");
        String description = (String) entries.get("description");
        String seasonnumber = (String) entries.get("season");
        String episodenumber = (String) entries.get("episode");
        final String relinker_url = (String) JavaScriptEngineFactory.walkJson(entries, "video/content_url");
        String filename = date_formatted + "_raitv_" + showTitle;
        if (!StringUtils.isEmpty(episodeTitle)) {
            filename += " - " + episodeTitle;
        }
        /* Add series information if available */
        if ((seasonnumber != null && seasonnumber.matches("\\d+")) || (episodenumber != null && episodenumber.matches("\\d+"))) {
            /* 2018-07-19: Also add series information if only seasonnumber or only episodenumber is available. */
            String seriesString = "";
            final DecimalFormat df = new DecimalFormat("00");
            if (seasonnumber != null && seasonnumber.matches("\\d+")) {
                seriesString += "S" + df.format(Integer.parseInt(seasonnumber));
            }
            if (episodenumber != null && episodenumber.matches("\\d+")) {
                seriesString += "E" + df.format(Integer.parseInt(episodenumber));
            }
            filename += seriesString + "_";
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        decryptRelinker(param, decryptedLinks, relinker_url, filename, extension, fp, description);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param) throws DecrypterException, Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        String dllink = null;
        String title = null;
        String extension = "mp4";
        String date = null;
        String date_formatted = null;
        String description = null;
        String seasonnumber = null;
        String episodenumber = null;
        this.br.getPage(parameter);
        final String jsredirect = this.br.getRegex("document\\.location\\.replace\\(\\'(http[^<>\"]*?)\\'\\)").getMatch(0);
        if (jsredirect != null && jsredirect.length() >= parameter.length()) {
            this.br.getPage(jsredirect.trim());
        }
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Do NOT use value of "videoURL_MP4" here! */
        /* E.g. http://www.rai.tv/dl/RaiTV/programmi/media/ContentItem-70996227-7fec-4be9-bc49-ba0a8104305a.html */
        dllink = this.br.getRegex("var[\t\n\r ]*?videoURL[\t\n\r ]*?=[\t\n\r ]*?\"((?:https?:)?//[^<>\"]+)\"").getMatch(0);
        String content_id_from_url = null;
        if (parameter.matches(TYPE_CONTENTITEM)) {
            content_id_from_url = new Regex(parameter, "(\\-[a-f0-9\\-]+)\\.html$").getMatch(0);
        }
        title = this.br.getRegex("property=\"og:title\" content=\"([^<>\"]+) \\- video \\- RaiPlay\"").getMatch(0);
        if (title == null) {
            title = this.br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        }
        date = this.br.getRegex("content=\"(\\d{4}\\-\\d{2}\\-\\d{2}) \\d{2}:\\d{2}:\\d{2}\" property=\"gen\\-date\"").getMatch(0);
        if (date == null) {
            /* 2020-02-18: New */
            date = this.br.getRegex("<meta property=\"data\" content=\"(\\d{2}-\\d{2}-\\d{4})\"").getMatch(0);
        }
        /* 2017-05-02: Avoid the same string multiple times in filenames. */
        final String name_show = this.br.getRegex("<meta property=\"nomeProgramma\" content=\"([^<>\"]+)\"/>").getMatch(0);
        final String name_episode = this.br.getRegex("<meta property=\"programma\" content=\"([^<>\"]+)\"/>").getMatch(0);
        if (!StringUtils.isEmpty(name_show) && !StringUtils.isEmpty(name_episode) && name_show.equals(name_episode) && counterString(title.toLowerCase(), name_show.toLowerCase()) > 1) {
            title = name_show;
        }
        episodenumber = br.getRegex("class=\"subtitle\\-spacing\">\\s*?Ep (\\d+)</span>").getMatch(0);
        seasonnumber = br.getRegex("class=\"subtitle\\-spacing\">St (\\d+)</span>").getMatch(0);
        final String contentset_id = this.br.getRegex("var[\t\n\r ]*?urlTop[\t\n\r ]*?=[\t\n\r ]*?\"[^<>\"]+/ContentSet([A-Za-z0-9\\-]+)\\.html").getMatch(0);
        final String content_id_from_html = this.br.getRegex("id=\"ContentItem(\\-[a-f0-9\\-]+)\"").getMatch(0);
        if (dllink == null) {
            dllink = findRelinkerUrl();
        }
        if (br.getHttpConnection().getResponseCode() == 404 || (contentset_id == null && content_id_from_html == null && dllink == null)) {
            /* Probably not a video/offline */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (dllink != null) {
            if (dllink.startsWith("//")) {
                dllink = "https:" + dllink;
            }
            if (title == null) {
                /* Streamurls directly in html */
                title = this.br.getRegex("id=\"idMedia\">([^<>]+)<").getMatch(0);
            }
            if (title == null) {
                title = this.br.getRegex("var videoTitolo\\d*?=\\d*?\"([^<>\"]+)\";").getMatch(0);
            }
            if (date == null) {
                /* 2017-01-21: New */
                date = this.br.getRegex("<meta property=\"titolo_episodio\" value=\"Puntata del (\\d{2}/\\d{2}/\\d{4})\"/>").getMatch(0);
            }
            if (date == null) {
                date = this.br.getRegex("data\\-date=\"(\\d{2}/\\d{2}/\\d{4})\"").getMatch(0);
            }
            if (date == null) {
                /* 2017-02-02: New */
                date = this.br.getRegex("avaibility\\-start=\"(\\d{4}\\-\\d{2}\\-\\d{2})\"").getMatch(0);
            }
            if (date == null) {
                /* 2017-02-23: New */
                date = this.br.getRegex("data\\-titolo=\"[^\"]+del (\\d{2}/\\d{2}/\\d{4})\"").getMatch(0);
            }
            if (date == null) {
                /* 2017-05-19: New */
                date = this.br.getRegex("itemprop=\"datePublished\" content=\"(\\d{2}\\-\\d{2}\\-\\d{4})\"").getMatch(0);
            }
        } else {
            Map<String, Object> entries = null;
            if (content_id_from_html != null) {
                /* Easiest way to find videoinfo */
                this.br.getPage("http://www.rai.tv/dl/RaiTV/programmi/media/ContentItem" + content_id_from_html + ".html?json");
                if (br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            }
            if (entries == null) {
                final List<Object> ressourcelist;
                final String list_json_from_html = this.br.getRegex("\"list\"[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\}[\t\n\r ]*?\\])").getMatch(0);
                if (list_json_from_html != null) {
                    ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(list_json_from_html);
                } else {
                    br.getPage("http://www.rai.tv/dl/RaiTV/ondemand/ContentSet" + contentset_id + ".html?json");
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        decryptedLinks.add(this.createOfflinelink(parameter));
                        return decryptedLinks;
                    }
                    entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    ressourcelist = (List<Object>) entries.get("list");
                }
                if (content_id_from_url == null) {
                    /* Hm probably not a video */
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                String content_id_temp = null;
                boolean foundVideoInfo = false;
                for (final Object videoo : ressourcelist) {
                    entries = (Map<String, Object>) videoo;
                    content_id_temp = (String) entries.get("itemId");
                    if (content_id_temp != null && content_id_temp.contains(content_id_from_url)) {
                        foundVideoInfo = true;
                        break;
                    }
                }
                if (!foundVideoInfo) {
                    /* Probably offline ... */
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            }
            date = (String) entries.get("date");
            title = (String) entries.get("name");
            description = (String) entries.get("desc");
            final String type = (String) entries.get("type");
            if (type.equalsIgnoreCase("RaiTv Media Video Item")) {
            } else {
                /* TODO */
                logger.warning("Unsupported media type!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            extension = "mp4";
            dllink = (String) entries.get("h264");
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("m3u8");
                extension = "mp4";
            }
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("wmv");
                extension = "wmv";
            }
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("mediaUri");
                extension = "mp4";
            }
        }
        if (title == null) {
            title = content_id_from_url;
        }
        date_formatted = jd.plugins.hoster.RaiTv.formatDate(date);
        title = Encoding.htmlDecode(title);
        title = date_formatted + "_raitv_" + title;
        /* Add series information if available */
        if (seasonnumber != null || episodenumber != null) {
            /* 2018-07-19: Also add series information if only seasonnumber or only episodenumber is available. */
            String seriesString = "";
            final DecimalFormat df = new DecimalFormat("00");
            if (seasonnumber != null) {
                seriesString += "S" + df.format(Integer.parseInt(seasonnumber));
            }
            if (episodenumber != null) {
                seriesString += "E" + df.format(Integer.parseInt(episodenumber));
            }
            title += seriesString + "_";
        }
        title = encodeUnicode(title);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        decryptRelinker(param, decryptedLinks, dllink, title, extension, fp, description);
        return decryptedLinks;
    }

    /* http://stackoverflow.com/questions/767759/occurrences-of-substring-in-a-string */
    private int counterString(final String s, final String search) {
        final Pattern p = Pattern.compile(search);
        final Matcher m = p.matcher(s);
        int count = 0;
        while (m.find()) {
            count += 1;
        }
        return count;
    }

    public void accessCont(final Browser br, final String cont) throws IOException {
        /**
         * # output=20 url in body<br />
         * # output=23 HTTP 302 redirect<br />
         * # output=25 url and other parameters in body, space separated<br />
         * # output=44 XML (not well formatted) in body<br />
         * # output=45 XML (website standard) in body<br />
         * # output=47 json in body<br />
         * # output=56 XML (with CDATA) in body<br />
         * # pl=native,flash,silverlight<br />
         * # BY DEFAULT (website): pl=mon,flash,native,silverlight<br />
         * # A stream will be returned depending on the UA (and pl parameter?)<br />
         */
        final String url = "http://mediapolisvod.rai.it/relinker/relinkerServlet.htm?cont=" + cont + "&output=56";
        logger.info("calling relinkServletUrl: " + url);
        br.getPage(url);
    }

    private void decryptRelinker(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks, final String relinker_url, String title, String extension, final FilePackage fp, final String description) throws Exception {
        String dllink = relinker_url;
        if (extension != null && extension.equalsIgnoreCase("wmv")) {
            /* E.g. http://www.tg1.rai.it/dl/tg1/2010/rubriche/ContentItem-9b79c397-b248-4c03-a297-68b4b666e0a5.html */
            logger.info("Download http .wmv video");
        } else {
            final String cont = jd.plugins.hoster.RaiTv.getContFromRelinkerUrl(relinker_url);
            if (cont == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Drop previous Headers & Cookies */
            final Browser brc = jd.plugins.hoster.RaiTv.prepVideoBrowser(new Browser());
            accessCont(brc, cont);
            if (brc.containsHTML("video_no_available\\.mp4")) {
                /* Offline/Geo-Blocked */
                /* XML response with e.g. this (and some more): <url>http://download.rai.it/video_no_available.mp4</url> */
                if (title == null) {
                    /* Fallback */
                    title = cont;
                }
                final DownloadLink offline = this.createOfflinelink(param.getCryptedUrl(), "GEOBLOCKED_" + title, "GEOBLOCKED");
                offline.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                decryptedLinks.add(offline);
                return;
            }
            dllink = jd.plugins.hoster.RaiTv.getDllink(brc);
            logger.info("found m3u8: " + dllink);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (extension == null && dllink.contains(".mp4")) {
                extension = "mp4";
            } else if (extension == null && dllink.contains(".wmv")) {
                extension = "wmv";
            } else if (extension == null) {
                /* Final fallback */
                extension = "mp4";
            }
            if (!jd.plugins.hoster.RaiTv.dllinkIsDownloadable(dllink)) {
                logger.info("Unsupported streaming protocol");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final Regex hdsconvert = new Regex(dllink, "(https?://[^/]+/z/podcastcdn/.+\\.csmil)/manifest\\.f4m");
        if (hdsconvert.matches()) {
            /* Convert hds --> hls */
            dllink = hdsconvert.getMatch(0).replace("/z/", "/i/") + "/index_1_av.m3u8";
        }
        final String[] staticBitrateList = new String[] { "2400", "1800", "1200", "700" };
        if (dllink.contains(".m3u8")) {
            // https?:\/\/[^\/]+\/i\/VOD\/(teche_root\/YT_ITALIA_TECHE_HD\/[0-9]*_)([0-9,]+)\.mp4(?:.csmil)?\/index_[0-9]+_av.m3u8\?null=[0-9]+&id=[A-Za-z0-9]+%3d%3d&hdntl=exp=[0-9]+~acl=%2f\*~data=hdntl~hmac=[A-Za-z0-9]+
            String http_url_part = new Regex(dllink, "https?://[^/]+/i/(podcastcdn/[^/]+/[^/]+/[^/]+/[^/]+_)[0-9,]+\\.mp4(?:\\.csmil)?/master\\.m3u8").getMatch(0);
            if (http_url_part == null) {
                http_url_part = new Regex(dllink, "/(podcastcdn.*/\\d+_),\\d+.*").getMatch(0);
            }
            if (http_url_part != null) {
                /*
                 * 2017-02-09: Convert hls urls to http urls and add higher quality 1800 url! doesn't work for everyone
                 */
                final String possibleBitratesStr = new Regex(dllink, "_,?(\\d+[0-9,]+)(?:\\.mp4|/playlist\\.m3u8)").getMatch(0);
                final String[] bitrateList;
                if (possibleBitratesStr != null) {
                    logger.info("Using dynamic bitratelist via: " + possibleBitratesStr);
                    bitrateList = possibleBitratesStr.split(",");
                } else {
                    logger.info("Using static bitratelist");
                    bitrateList = staticBitrateList;
                }
                logger.info("Converting HLS -> HTTP URLs");
                for (final String staticBitrate : bitrateList) {
                    // final String directlink_http = String.format("http://creativemedia3.rai.it/%s%s.mp4", http_url_part, bitrate);
                    /* 2021-03-11 */
                    final String directlink_http = "http://creativemedia7-rai-it.akamaized.net/" + http_url_part + staticBitrate + ".mp4";
                    final DownloadLink dl = this.createDownloadlink("directhttp://" + directlink_http);
                    dl.setFinalFileName(title + "_" + staticBitrate + "." + extension);
                    dl._setFilePackage(fp);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    decryptedLinks.add(dl);
                }
            } else {
                /* https://svn.jdownloader.org/issues/84276 */
                logger.warning("Crawling HLS: Split audio/video could cause JD to only download video without audio");
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.getPage(dllink);
                if (brc.getRegex("Access Denied").matches()) {
                    logger.severe("Access denied! The hmac is corrupt, maybe. Try to set a coherent User-Agent.");
                }
                final List<HlsContainer> allqualities = HlsContainer.getHlsQualities(brc);
                for (final HlsContainer singleHlsQuality : allqualities) {
                    logger.info("found quality: " + singleHlsQuality.getStreamURL());
                    final DownloadLink dl = this.createDownloadlink(singleHlsQuality.getStreamURL());
                    final String filename = title + "_" + singleHlsQuality.getStandardFilename();
                    dl.setFinalFileName(filename);
                    dl._setFilePackage(fp);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    decryptedLinks.add(dl);
                }
            }
        } else {
            /* Single http url --> We can sometimes grab multiple qualities */
            if (dllink.contains("_1800.mp4")) {
                /* Multiple qualities availab.e */
                for (final String staticBitrate : staticBitrateList) {
                    final String directlink = dllink.replace("_1800.mp4", "_" + staticBitrate + ".mp4");
                    final DownloadLink dl = this.createDownloadlink("directhttp://" + directlink);
                    dl.setFinalFileName(title + "_" + staticBitrate + "." + extension);
                    dl._setFilePackage(fp);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    decryptedLinks.add(dl);
                }
            } else {
                /* Only one quality available. */
                final DownloadLink dl = this.createDownloadlink("directhttp://" + dllink);
                dl.setFinalFileName(title + "." + extension);
                dl._setFilePackage(fp);
                if (description != null) {
                    dl.setComment(description);
                }
                decryptedLinks.add(dl);
            }
        }
    }

    private String convertInputDateToJsonDateFormat(final String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.ENGLISH);
        } else {
            date = TimeFormatter.getMilliSeconds(input, "yyyy_MM_dd", Locale.ENGLISH);
        }
        String formattedDate = null;
        final String targetFormat = "dd-MM-yyyy";
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

    private String findRelinkerUrl() throws Exception {
        String jsonUrl = this.br.getRegex("data-video-json=\"(/video/[-\\/A-Za-z0-9]+\\.json)\"").getMatch(0);
        logger.info("found jsonUrl: " + jsonUrl);
        this.br.getPage("https://www.raiplay.it" + jsonUrl);
        Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        String relinkServletUrl = (String) ((Map<String, Object>) entries.get("video")).get("content_url");
        logger.info("found relinkServletUrl: " + relinkServletUrl);
        return relinkServletUrl;
    }
}
