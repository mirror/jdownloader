//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "phoenix.de", "neo-magazin-royale.de", "heute.de", "tivi.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/.+/[A-Za-z0-9_\\-]+\\.html", "https?://(?:www\\.)?phoenix\\.de/content/\\d+|http://(?:www\\.)?phoenix\\.de/podcast/[A-Za-z0-9]+/video/rss\\.xml", "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+", "https?://(?:www\\.)?heute\\.de/.+", "https?://(?:www\\.)?tivi\\.de/(mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?|tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+)" })
public class ZDFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES                   = "Q_SUBTITLES";
    private static final String Q_BEST                        = "Q_BEST";
    private static final String Q_LOW                         = "Q_LOW";
    private static final String Q_HIGH                        = "Q_HIGH";
    private static final String Q_VERYHIGH                    = "Q_VERYHIGH";
    private static final String Q_HD                          = "Q_HD";
    private static final String FASTLINKCHECK                 = "FASTLINKCHECK";
    private boolean             BEST                          = false;

    ArrayList<DownloadLink>     decryptedLinks                = new ArrayList<DownloadLink>();
    private String              PARAMETER                     = null;
    private String              PARAMETER_ORIGINAL            = null;
    boolean                     fastlinkcheck                 = false;

    private final String        TYPE_PHOENIX                  = "https?://(?:www\\.)?phoenix\\.de/content/\\d+";
    private final String        TYPE_PHOENIX_RSS              = "http://(?:www\\.)?phoenix\\.de/podcast/.+";
    private final String        TYPE_TIVI                     = "https?://(?:www\\.)?tivi\\.de/(?:mediathek/[a-z0-9\\-]+\\-(\\d+)/[a-z0-9\\-]+\\-(\\d+)/?|tiviVideos/beitrag/title/(\\d+)/(\\d+)\\?view=.+)";
    private final String        TYPE_TIVI_1                   = "https?://(?:www\\.)?tivi\\.de/mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?";
    private final String        TYPE_TIVI_2                   = "https?://(?:www\\.)?tivi\\.de/tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+";
    private final String        TYPE_ZDF                      = "https?://(?:www\\.)?zdf\\.de/.+";
    private final String        TYPE_ZDF_MEDIATHEK            = "https?://(?:www\\.)?zdf\\.de/.+/[^/]+\\.html";
    private final String        TYPE_ZDF_EMBEDDED_HEUTE       = "https?://(?:www\\.)?heute\\.de/.+";
    private final String        TYPE_ZDF_EMBEDDED_NEO_MAGAZIN = "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+";

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
    /**
     * TODO: Maybe add support for tivi.de but, similar to phoenix.de, we'd have to use another url to access their XML containing the final
     * video urls - just stupid!
     */
    /** Related sites: see RegExes, and also: 3sat.de */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(500);
        final SubConfiguration cfg = SubConfiguration.getConfig("zdf.de");
        PARAMETER = param.toString();
        PARAMETER_ORIGINAL = param.toString();
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        this.fastlinkcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);

        setBrowserExclusive();
        br.setFollowRedirects(true);

        if (PARAMETER_ORIGINAL.matches(TYPE_PHOENIX_RSS)) {
            decryptPhoenixRSS();
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_TIVI) || PARAMETER_ORIGINAL.matches(TYPE_PHOENIX)) {
            getDownloadLinksZdfOld(cfg);
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_HEUTE)) {
            this.crawlEmbeddedUrlsHeute();
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_NEO_MAGAZIN)) {
            this.crawlEmbeddedUrlsNeoMagazin(cfg);
        } else if (PARAMETER_ORIGINAL.matches(TYPE_ZDF_MEDIATHEK)) {
            getDownloadLinksZdfNew(cfg);
        } else {
            logger.info("Unsupported URL(s)");
        }

        if (decryptedLinks == null) {
            logger.warning("Decrypter out of date for link: " + PARAMETER);
            return null;
        }
        return decryptedLinks;
    }

    private void decryptPhoenixRSS() throws IOException {
        br.getPage(this.PARAMETER);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final String date_general = getXML("pubDate");
        String title_general = getXML("title");
        final String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
        if (items == null || items.length == 0 || title_general == null || date_general == null) {
            this.decryptedLinks = null;
            return;
        }
        final String fpname = encodeUnicode(formatDatePHOENIX(date_general) + "_" + title_general);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        for (final String item : items) {
            final String url = getXML(item, "guid");
            final String title = getXML(item, "title");
            final String description = getXML(item, "description");
            final String date = getXML(item, "pubDate");
            final String tvstation = getXML(item, "itunes:author");
            final String filesize = new Regex(item, "length=\\'(\\d+)\\'").getMatch(0);
            if (url == null || title == null || date == null || tvstation == null || filesize == null) {
                this.decryptedLinks = null;
                return;
            }
            final DownloadLink dl = this.createDownloadlink("directhttp://" + url);
            String final_filename = formatDatePHOENIX(date) + "_" + tvstation + "_" + title + ".mp4";
            final_filename = encodeUnicode(final_filename);
            if (description != null) {
                dl.setComment(description);
            }
            dl.setProperty("date", date);
            dl.setFinalFileName(final_filename);
            dl.setDownloadSize(Long.parseLong(filesize));
            if (this.fastlinkcheck) {
                dl.setAvailable(true);
            }
            this.decryptedLinks.add(dl);
        }
        fp.addLinks(decryptedLinks);
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> getDownloadLinksZdfOld(final SubConfiguration cfg) {
        final boolean grabSubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        String date = null;
        String date_formatted = null;
        String id = null;
        String id_filename = null;
        String title = null;
        String show = null;
        String subtitleURL = null;
        String subtitleInfo = null;
        String decrypterurl = null;
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();

        try {
            if (PARAMETER_ORIGINAL.matches(TYPE_PHOENIX)) {
                br.getPage(PARAMETER);
                if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                    decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                    return decryptedLinks;
                }
                id = br.getRegex("id=\"phx_vod_(\\d+)\"").getMatch(0);
                if (id == null) {
                    decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                    return decryptedLinks;
                }
                id_filename = id;
                decrypterurl = "decrypted://phoenix.de/content/" + id + "&quality=%s";
                br.getPage("/php/zdfplayer-v1.3/data/beitragsDetails.php?ak=web&id=" + id);
            } else if (this.PARAMETER_ORIGINAL.matches(TYPE_TIVI)) {
                final String param_1;
                final String param_2;
                if (this.PARAMETER_ORIGINAL.matches(TYPE_TIVI_1)) {
                    param_1 = new Regex(this.PARAMETER_ORIGINAL, TYPE_TIVI).getMatch(0);
                    param_2 = new Regex(this.PARAMETER_ORIGINAL, TYPE_TIVI).getMatch(1);
                } else {
                    param_1 = new Regex(this.PARAMETER_ORIGINAL, TYPE_TIVI).getMatch(2);
                    param_2 = new Regex(this.PARAMETER_ORIGINAL, TYPE_TIVI).getMatch(3);
                }
                id_filename = param_1 + "_" + param_2;
                decrypterurl = "decrypted://tivi.de/content/" + param_1 + param_2 + "&quality=%s";
                br.getPage("http://www.tivi.de/tiviVideos/beitrag/" + param_1 + "/" + param_2 + "?view=flashXml");
            }
            if (br.containsHTML("<debuginfo>Kein Beitrag mit ID") || br.containsHTML("<statuscode>wrongParameter</statuscode>")) {
                decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                return decryptedLinks;
            }

            date = getXML("airtime");
            title = getTitle(br);
            show = this.getXML("originChannelTitle");
            if (show == null) {
                /* E.g. for tivi.de */
                show = this.getXML("ns2:broadcast-name");
            }
            String extension = ".mp4";
            subtitleInfo = br.getRegex("<caption>(.*?)</caption>").getMatch(0);
            if (subtitleInfo != null) {
                subtitleURL = new Regex(subtitleInfo, "<url>(https?://utstreaming\\.zdf\\.de/tt/\\d{4}/[A-Za-z0-9_\\-]+\\.xml)</url>").getMatch(0);
            }
            if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) {
                extension = ".mp3";
            }
            if (date == null || title == null || show == null) {
                return null;
            }
            show = Encoding.htmlDecode(show);
            show = encodeUnicode(show);

            date_formatted = formatDateZDF(date);

            final Browser br2 = br.cloneBrowser();
            final String[][] downloads = br2.getRegex("<[^>]*?formitaet basetype=\"([^\"]+)\" isDownload=\"[^\"]+\">(.*?)</[^>]*?formitaet>").getMatches();
            for (String streams[] : downloads) {

                if (!(streams[0].contains("mp4_http") || streams[0].contains("mp4_rtmp_zdfmeta"))) {
                    continue;
                }

                for (String stream[] : new Regex(streams[1], "<[^>]*?quality>([^<]+)</[^>]*?quality>.*?<[^>]*?url>([^<]+)<.*?<[^>]*?filesize>(\\d+)<").getMatches()) {

                    if (streams[0].contains("mp4_http") && !new Regex(streams[1], ("<[^>]*?facet>(progressive|restriction_useragent|podcast|hbbtv)</[^>]*?facet>")).matches()) {
                        continue;
                    }
                    if (streams[0].contains("mp4_rtmp_zdfmeta")) {
                        continue;
                    }
                    if (stream[1].endsWith(".meta") && stream[1].contains("streaming") && stream[1].startsWith("http")) {
                        br2.getPage(stream[1]);
                        stream[1] = br2.getRegex("<default\\-stream\\-url>(.*?)</default\\-stream\\-url>").getMatch(0);
                        if (stream[1] == null) {
                            continue;
                        }
                    }

                    String url = stream[1];
                    String fmt = stream[0];
                    if (fmt != null) {
                        fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
                    }
                    if (fmt != null) {
                        /* best selection is done at the end */
                        if ("low".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "low";
                            }
                        } else if ("high".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "high";
                            }
                        } else if ("veryhigh".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_VERYHIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "veryhigh";
                            }
                        } else if ("hd".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == false) {
                                continue;
                            } else {
                                if (streams[0].contains("mp4_rtmp")) {
                                    if (url.startsWith("http://")) {
                                        Browser rtmp = new Browser();
                                        rtmp.getPage(stream[1]);
                                        url = rtmp.getRegex("<default\\-stream\\-url>([^<]+)<").getMatch(0);
                                    }
                                    if (url == null) {
                                        continue;
                                    }
                                }
                                fmt = "hd";
                            }
                        }
                    }

                    final String fmtUPPR = fmt.toUpperCase(Locale.ENGLISH);
                    final String name = date_formatted + "_zdf_" + show + " - " + title + "_" + id_filename + "@" + fmtUPPR + extension;
                    final DownloadLink link = createDownloadlink(String.format(decrypterurl, fmt));
                    if (this.fastlinkcheck) {
                        link.setAvailable(true);
                    }
                    link.setFinalFileName(name);
                    link.setContentUrl(PARAMETER_ORIGINAL);
                    link.setProperty("date", date_formatted);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", name);
                    link.setProperty("directQuality", stream[0]);
                    link.setProperty("streamingType", "http");
                    link.setProperty("directfmt", fmtUPPR);

                    if (!url.contains("hinweis_fsk")) {
                        try {
                            link.setDownloadSize(SizeFormatter.getSize(stream[2]));
                        } catch (Throwable e) {
                        }
                    }

                    DownloadLink best = bestMap.get(fmt);
                    if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                        bestMap.put(fmt, link);
                    }
                    newRet.add(link);
                }
            }
            if (newRet.size() > 0) {
                if (BEST) {
                    /* only keep best quality */
                    DownloadLink keep = bestMap.get("hd");
                    if (keep == null) {
                        keep = bestMap.get("veryhigh");
                    }
                    if (keep == null) {
                        keep = bestMap.get("high");
                    }
                    if (keep == null) {
                        keep = bestMap.get("low");
                    }
                    if (keep != null) {
                        newRet.clear();
                        newRet.add(keep);
                    }
                }
            }
            ret = newRet;
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        for (final DownloadLink dl : ret) {
            if (grabSubtitles && subtitleURL != null) {
                final String dlfmt = dl.getStringProperty("directfmt", null);
                final String startTime = new Regex(subtitleInfo, "<offset>(\\-)?(\\d+)</offset>").getMatch(1);
                final String name = date_formatted + "_zdf_" + show + " - " + title + "_" + id_filename + "@" + dlfmt + ".xml";
                final DownloadLink subtitle = createDownloadlink(String.format(decrypterurl, dlfmt + "subtitle"));
                subtitle.setAvailable(true);
                subtitle.setFinalFileName(name);
                subtitle.setProperty("date", date_formatted);
                subtitle.setProperty("directURL", subtitleURL);
                subtitle.setProperty("directName", name);
                subtitle.setProperty("streamingType", "subtitle");
                subtitle.setProperty("starttime", startTime);
                subtitle.setContentUrl(PARAMETER_ORIGINAL);
                subtitle.setLinkID(name);
                decryptedLinks.add(subtitle);
            }
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(date_formatted + "_zdf_" + show + " - " + title);
            fp.addLinks(decryptedLinks);
        }
        return ret;
    }

    private void crawlEmbeddedUrlsHeute() throws Exception {
        br.getPage(this.PARAMETER);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final String[] ids = this.br.getRegex("\"videoId\"\\s*?:\\s*?\"([^\"]*?)\"").getColumn(0);
        for (final String videoid : ids) {
            final String mainlink = "https://www.zdf.de/nachrichten/heute-journal/" + videoid + ".html";
            decryptedLinks.add(this.createDownloadlink(mainlink));
        }
        return;
    }

    private void crawlEmbeddedUrlsNeoMagazin(final SubConfiguration cfg) throws Exception {
        br.getPage(this.PARAMETER);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final boolean neomagazinroyale_only_add_current_episode = cfg.getBooleanProperty(jd.plugins.hoster.ZdfDeMediathek.NEOMAGAZINROYALE_DE_ADD_ONLY_CURRENT_EPISODE, jd.plugins.hoster.ZdfDeMediathek.defaultNEOMAGAZINROYALE_DE_ADD_ONLY_CURRENT_EPISODE);
        final String[] htmls = this.br.getRegex("<div[^>]*?class=\"modules\" id=\"teaser\\-\\d+\"[^>]*?>.*?</div>\\s*?</div>\\s*?</div>\\s*?</div>").getColumn(-1);
        for (final String html : htmls) {
            /* TODO: Improve this url but for now, that will work fine ... */
            final String videoid = new Regex(html, "data\\-sophoraid=\"([^\"]+)\"").getMatch(0);
            final String title = new Regex(html, "class=\"headline\"[^>]*?><h3[^>]*?class=\"h3 zdf\\-\\-primary\\-light\"[^>]*?>([^<>]+)<").getMatch(0);
            if (videoid == null) {
                /* Probably no video content. */
                continue;
            }

            final String mainlink = "https://www.zdf.de/comedy/neo-magazin-mit-jan-boehmermann/" + videoid + ".html";
            /* Check if user only wants current Neo Magazin episode and if we have it. */
            if (neomagazinroyale_only_add_current_episode && title != null && new Regex(title, Pattern.compile(".*?NEO MAGAZIN ROYALE.*?vom.*?", Pattern.CASE_INSENSITIVE)).matches()) {
                /* Clear list */
                decryptedLinks.clear();
                /* Only add this one entry */
                decryptedLinks.add(this.createDownloadlink(mainlink));
                /* Return --> Done */
                return;
            }
            decryptedLinks.add(this.createDownloadlink(mainlink));
        }
        return;
    }

    private String getApiTokenFromHtml() {
        String apitoken = this.br.getRegex("apiToken\\s*?:\\s*?\\'([a-f0-9]+)\\'").getMatch(0);
        if (apitoken == null) {
            apitoken = PluginJSonUtils.getJsonNested(this.br, "apiToken");
        }
        return apitoken;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private ArrayList<DownloadLink> getDownloadLinksZdfNew(final SubConfiguration cfg) throws Exception {
        /* TODO */
        // final boolean grabSubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        // String subtitleURL = null;
        // String subtitleInfo = null;
        final String sophoraID = new Regex(this.PARAMETER, "/([^/]+)\\.html").getMatch(0);
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        // final String apitoken = getApiToken();
        /* 2016-12-21: By hardcoding the apitoken we can save one http request thus have a faster crawl process :) */
        final String apitoken = "f4ba81fa117681c42383194a7103251db2981962";
        this.br.getHeaders().put("Api-Auth", "Bearer " + apitoken);
        this.br.getPage("https://api.zdf.de/content/documents/" + sophoraID + ".json?profile=player");

        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER));
            return ret;
        }

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
        LinkedHashMap<String, Object> entries_2 = null;

        final String contentType = (String) entries.get("contentType");
        final String title = (String) entries.get("title");
        final String editorialDate = (String) entries.get("editorialDate");
        final String tvStation = (String) entries.get("tvService");
        final boolean hasVideo = ((Boolean) entries.get("hasVideo")).booleanValue();
        String date_formatted;

        entries_2 = (LinkedHashMap<String, Object>) entries.get("http://zdf.de/rels/brand");
        final String show = (String) entries_2.get("title");

        entries_2 = (LinkedHashMap<String, Object>) entries.get("mainVideoContent");
        entries_2 = (LinkedHashMap<String, Object>) entries_2.get("http://zdf.de/rels/target");
        final String id;
        final String player_url_template = (String) entries_2.get("http://zdf.de/rels/streams/ptmd-template");

        if (!hasVideo) {
            logger.info("Content is not a video --> Nothing to download");
            return ret;
        }

        if (inValidate(contentType) || inValidate(title) || inValidate(editorialDate) || inValidate(tvStation) || inValidate(player_url_template)) {
            return null;
        }

        date_formatted = new Regex(editorialDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        id = new Regex(player_url_template, "/([^/]+)$").getMatch(0);
        if (date_formatted == null || id == null) {
            return null;
        }
        short counter = 0;
        boolean grabDownloadUrls = false;
        boolean finished = false;
        do {
            if (counter == 0) {
                accessPlayerJson(player_url_template, "ngplayer_2_3");
            } else if (!grabDownloadUrls || counter > 1) {
                /* Fail safe && case when there are no additional downloadlinks available. */
                finished = true;
                break;
            } else {
                accessPlayerJson(player_url_template, "zdf_pd_download_1");
                finished = true;
            }

            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            final Object downloadAllowed_o = JavaScriptEngineFactory.walkJson(entries, "attributes/downloadAllowed/value");
            if (downloadAllowed_o != null && downloadAllowed_o instanceof Boolean) {
                /* Usually this is set in the first loop to decide whether a 2nd loop is required. */
                grabDownloadUrls = ((Boolean) downloadAllowed_o).booleanValue();
            }
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("priorityList");
            // final ArrayList<Object> subtitles = (ArrayList<Object>) entries.get("captions");

            for (final Object priority_o : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) priority_o;
                final ArrayList<Object> formitaeten = (ArrayList<Object>) entries.get("formitaeten");
                for (final Object formitaet_o : formitaeten) {
                    entries = (LinkedHashMap<String, Object>) formitaet_o;

                    final boolean isAdaptive = ((Boolean) entries.get("isAdaptive")).booleanValue();
                    if (isAdaptive) {
                        /* 2016-12-21: Skip all segment downloads - only allow http for now. */
                        continue;
                    }

                    final ArrayList<Object> qualities = (ArrayList<Object>) entries.get("qualities");
                    /* TODO: Skip unwanted types here! */
                    final String type = (String) entries.get("type");
                    for (final Object qualities_o : qualities) {
                        entries = (LinkedHashMap<String, Object>) qualities_o;

                        final String quality = (String) entries.get("quality");
                        if (inValidate(quality)) {
                            continue;
                        }

                        entries = (LinkedHashMap<String, Object>) entries.get("audio");
                        final ArrayList<Object> tracks = (ArrayList<Object>) entries.get("tracks");
                        for (final Object tracks_o : tracks) {
                            entries = (LinkedHashMap<String, Object>) tracks_o;

                            final String cdn = (String) entries.get("cdn");
                            final String clss = (String) entries.get("class");
                            final String language = (String) entries.get("language");
                            final long filesize = JavaScriptEngineFactory.toLong(entries.get("filesize"), 0);
                            String uri = (String) entries.get("uri");
                            final String ext;
                            if (inValidate(cdn) || inValidate(clss) || inValidate(language) || inValidate(uri)) {
                                continue;
                            }

                            if (type.contains("vorbis") && uri.contains("webm")) {
                                ext = ".webm";
                            } else {
                                ext = ".mp4";
                            }

                            final String linkid = id + "_" + type + "_" + cdn + "_" + language + "_" + quality;
                            final String final_filename = encodeUnicode(date_formatted + "_" + tvStation + "_" + show + "_" + title + "_" + linkid + ext);

                            /* TODO: Check if this might still be useful ... */
                            // boolean isHBBTV = false;
                            // final String fixme = new Regex(uri,
                            // "https?://(?:www\\.)?metafilegenerator\\.de/ondemand/zdf/hbbtv/([A-Za-z0-9]+/zdf/\\d+/\\d+/[^<>\"]+\\.mp4)").getMatch(0);
                            // if (fixme != null) {
                            // /* E.g. http://rodl.zdf.de/none/zdf/16/03/160304_top_mom_2328k_p35v12.mp4 */
                            // /* Fix invalid / unauthorized hbbtv urls so that we get downloadable http urls */
                            // uri = "http://rodl.zdf.de/" + fixme;
                            // isHBBTV = true;
                            // }

                            final DownloadLink dl = createDownloadlink(uri.replaceAll("https?://", "decryptedmediathek://"));
                            /* Usually the filesize is only given for the official downloads. */
                            if (filesize > 0) {
                                dl.setAvailable(true);
                                dl.setDownloadSize(filesize);
                            } else if (this.fastlinkcheck) {
                                dl.setAvailable(true);
                            }
                            dl.setFinalFileName(final_filename);
                            dl.setProperty("date", date_formatted);
                            dl.setProperty("directName", final_filename);
                            dl.setProperty("streamingType", type);
                            dl.setContentUrl(PARAMETER_ORIGINAL);
                            dl.setLinkID(linkid);
                            decryptedLinks.add(dl);
                        }
                    }

                }
            }
            counter++;
        } while (!finished);

        // for (final DownloadLink dl : ret) {
        // if (grabSubtitles && subtitleURL != null) {
        // final String dlfmt = dl.getStringProperty("directfmt", null);
        // final String startTime = new Regex(subtitleInfo, "<offset>(\\-)?(\\d+)</offset>").getMatch(1);
        // final String name = date_formatted + "_zdf_" + show + " - " + title + "@" + dlfmt + ".xml";
        // final DownloadLink subtitle = createDownloadlink(String.format(decrypterurl, dlfmt + "subtitle"));
        // subtitle.setAvailable(true);
        // subtitle.setFinalFileName(name);
        // subtitle.setProperty("date", date_formatted);
        // subtitle.setProperty("directURL", subtitleURL);
        // subtitle.setProperty("directName", name);
        // subtitle.setProperty("streamingType", "subtitle");
        // subtitle.setProperty("starttime", startTime);
        // subtitle.setContentUrl(PARAMETER_ORIGINAL);
        // subtitle.setLinkID(name);
        // decryptedLinks.add(subtitle);
        // }
        // decryptedLinks.add(dl);
        // }
        if (decryptedLinks.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(date_formatted + "_zdf_" + show + " - " + title);
            fp.addLinks(decryptedLinks);
        }
        return ret;
    }

    private void accessPlayerJson(final String player_url_template, final String playerID) throws IOException {
        /* E.g. "/tmd/2/{playerId}/vod/ptmd/mediathek/161215_sendungroyale065ddm_nmg" */
        final String player_url = player_url_template.replace("{playerId}", playerID);
        this.br.getPage(player_url);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        title = encodeUnicode(title);
        return title;
    }

    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>]*?)\\]\\]></" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "( type=\"[^<>/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        }
        return result;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String formatDateZDF(String input) {
        final long date;
        if (input.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}")) {
            /* tivi.de */
            input = input.substring(0, input.lastIndexOf(":")) + "00";
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.GERMAN);
        } else {
            /* zdf.de/zdfmediathek */
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy HH:mm", Locale.GERMAN);
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

    private String formatDatePHOENIX(String input) {
        /* It contains the day twice --> Fix that */
        if (input.contains(",")) {
            input = input.substring(input.lastIndexOf(",") + 2);
        }
        // Tue, 23 Jun 2015 11:33:00 +0200
        final long date = TimeFormatter.getMilliSeconds(input, "dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}