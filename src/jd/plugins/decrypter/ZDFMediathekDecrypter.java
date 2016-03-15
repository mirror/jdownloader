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
import jd.plugins.hoster.DummyScriptEnginePlugin;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "phoenix.de", "neo-magazin-royale.de", "heute.de", "zdfneo.de", "zdfkultur.de", "zdfinfo.de", "zdfsport.de", "tivi.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/.+", "https?://(?:www\\.)?phoenix\\.de/content/\\d+|http://(?:www\\.)?phoenix\\.de/podcast/runde/video/rss\\.xml", "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+", "https?://(?:www\\.)?heute\\.de/.+", "https?://(?:www\\.)?zdfneo\\.de/.+", "https?://(?:www\\.)?zdfkultur\\.de/.+", "https?://(?:www\\.)?zdfinfo\\.de/.+", "https?://(?:www\\.)?zdfsport\\.de/.+", "https?://(?:www\\.)?tivi\\.de/(mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?|tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+)" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class ZDFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES        = "Q_SUBTITLES";
    private static final String Q_BEST             = "Q_BEST";
    private static final String Q_LOW              = "Q_LOW";
    private static final String Q_HIGH             = "Q_HIGH";
    private static final String Q_VERYHIGH         = "Q_VERYHIGH";
    private static final String Q_HD               = "Q_HD";
    private static final String FASTLINKCHECK      = "FASTLINKCHECK";
    private boolean             BEST               = false;

    ArrayList<DownloadLink>     decryptedLinks     = new ArrayList<DownloadLink>();
    private String              PARAMETER          = null;
    private String              PARAMETER_ORIGINAL = null;
    boolean                     fastlinkcheck      = false;

    private final String        TYPE_PHOENIX       = "https?://(?:www\\.)?phoenix\\.de/content/\\d+";
    private final String        TYPE_PHOENIX_RSS   = "http://(?:www\\.)?phoenix\\.de/podcast/runde/video/rss\\.xml";
    private final String        TYPE_TIVI          = "https?://(?:www\\.)?tivi\\.de/(?:mediathek/[a-z0-9\\-]+\\-(\\d+)/[a-z0-9\\-]+\\-(\\d+)/?|tiviVideos/beitrag/title/(\\d+)/(\\d+)\\?view=.+)";
    private final String        TYPE_TIVI_1        = "https?://(?:www\\.)?tivi\\.de/mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?";
    private final String        TYPE_TIVI_2        = "https?://(?:www\\.)?tivi\\.de/tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+";
    private final String        TYPE_ZDF           = "https://(?:www\\.)?zdf\\.de/ZDFmediathek#?/[^<>\"]*?beitrag/video/\\d+(?:.+)?";

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
    /**
     * TODO: Maybe add support for tivi.de but, similar to phoenix.de, we'd have to use another url to access their XML containing the final
     * video urls - just stupid!
     */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(500);
        final SubConfiguration cfg = SubConfiguration.getConfig("zdf.de");
        PARAMETER_ORIGINAL = param.toString();
        PARAMETER = PARAMETER_ORIGINAL.replace("ZDFmediathek#/", "ZDFmediathek/");
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        this.fastlinkcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        /* Check for invalid (Livestream) links */
        if (PARAMETER.contains("beitrag/live/") && !PARAMETER.matches(".+beitrag/video/\\d+.+")) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return decryptedLinks;
        }

        setBrowserExclusive();
        br.setFollowRedirects(true);
        if (PARAMETER_ORIGINAL.matches(TYPE_PHOENIX_RSS)) {
            decryptPhoenixRSS();
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_TIVI) || PARAMETER_ORIGINAL.matches(TYPE_PHOENIX)) {
            getDownloadLinksZdfOld(cfg);
        } else {
            getDownloadLinksZdfNew(cfg);
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
                decrypterurl = "decrypted://tivi.de/content/" + param_1 + param_2 + "&quality=%s";
                br.getPage("/tiviVideos/beitrag/" + param_1 + "/" + param_2 + "?view=flashXml");
            } else {
                /*
                 * When browsing the ZDFMediathek, the url will get longer and longer and can contain multiple video-IDs. However, the
                 * current one is the last one --> Make sure we get that!
                 */
                /* E.g. 'beitrag/video/12345' or 'beitrag/einzelsendung/12345' */
                String[] ids = new Regex(PARAMETER, "beitrag/[^/]+/(\\d+)").getColumn(0);
                if (ids != null && ids.length > 0) {
                    id = ids[ids.length - 1];
                }
                if (id == null) {
                    /* Let's look for embedded zdfmediathek videoids in the html. */
                    br.getPage(this.PARAMETER);
                    if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                        decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                        return decryptedLinks;
                    }
                    /* neo-magazin-royale.de, heute.de */
                    final String[] regexes = { "data\\-assetid=\"(\\d+)\"", "\"videoId\"[\t\n\r ]*?:[\t\n\r ]*?\"(\\d+)\"" };
                    for (final String regex : regexes) {
                        ids = this.br.getRegex(regex).getColumn(0);
                        if (ids != null && ids.length > 0) {
                            for (final String videoid : ids) {
                                final String video_mainlink = createZdfLink(videoid);
                                decryptedLinks.add(this.createDownloadlink(video_mainlink));
                            }
                        }
                    }
                    return decryptedLinks;
                }
                decrypterurl = "decrypted://www.zdf.de/ZDFmediathek/beitrag/video/" + id + "&quality=%s";
                br.getPage("/ZDFmediathek/xmlservice/web/beitragsDetails?id=" + id + "&ak=web");
                /* Make sure link is decrypter-compatible */
                PARAMETER = "http://www.zdf.de/ZDFmediathek/beitrag/video/" + id;
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
                    final String name = date_formatted + "_zdf_" + show + " - " + title + "@" + fmtUPPR + extension;
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
                final String name = date_formatted + "_zdf_" + show + " - " + title + "@" + dlfmt + ".xml";
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

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    private ArrayList<DownloadLink> getDownloadLinksZdfNew(final SubConfiguration cfg) {
        final boolean grabSubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        String date = null;
        String date_formatted = null;
        String id = null;
        String title = null;
        String show = null;
        String subtitleURL = null;
        String subtitleInfo = null;
        String decrypterurl = null;
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();

        try {
            /*
             * When browsing the ZDFMediathek, the url will get longer and longer and can contain multiple video-IDs. However, the current
             * one is the last one --> Make sure we get that!
             */
            String[] ids = new Regex(PARAMETER, "beitrag/[^/]+/(\\d+)").getColumn(0);
            if (ids != null && ids.length > 0) {
                id = ids[ids.length - 1];
            }
            if (id != null && this.PARAMETER.matches(".+beitrag/einzelsendung/\\d+.*?")) {
                /* Decrypt multiple videoids */
                this.br.getPage("http://www.zdf.de/ZDFmediathek/xmlservice/web/aktuellste?ak=web&maxLength=50&ganzeSendungen=false&id=" + id + "&offset=0");
                final String[] videoids = this.br.getRegex("<assetId>(\\d+)</assetId>").getColumn(0);
                if (videoids != null && videoids.length > 0) {
                    for (final String videoid : videoids) {
                        final String video_mainlink = createZdfLink(videoid);
                        decryptedLinks.add(this.createDownloadlink(video_mainlink));
                    }
                }
                return decryptedLinks;
            } else if (id == null) {
                /* No videoid given in url added by user --> Let's look for embedded zdfmediathek videoids in the html. */
                br.getPage(this.PARAMETER);
                if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                    decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                    return decryptedLinks;
                }
                /* neo-magazin-royale.de, heute.de */
                final String[] regexes = { "data\\-assetid=\"(\\d+)\"", "\"videoId\"[\t\n\r ]*?:[\t\n\r ]*?\"(\\d+)\"" };
                for (final String regex : regexes) {
                    ids = this.br.getRegex(regex).getColumn(0);
                    if (ids != null && ids.length > 0) {
                        for (final String videoid : ids) {
                            final String video_mainlink = createZdfLink(videoid);
                            decryptedLinks.add(this.createDownloadlink(video_mainlink));
                        }
                    }
                }
                return decryptedLinks;
            }
            decrypterurl = "decrypted://www.zdf.de/ZDFmediathek/beitrag/video/" + id + "&quality=%s";
            /* Old: http://www.zdf.de/ZDFmediathek/xmlservice/web/beitragsDetails?id=<id>&ak=web */
            /* New February 2016: */
            br.getPage("http://www.zdf.de/ZDFmediathek/xmlservice/v2/web/beitragsDetails?ak=web&id=" + id);
            if (this.br.containsHTML(">wrongParameter<")) {
                ret.add(this.createDownloadlink(PARAMETER));
                return ret;
            }
            /* Make sure link is decrypter-compatible */
            PARAMETER = "http://www.zdf.de/ZDFmediathek/beitrag/video/" + id;
            if (br.containsHTML("<debuginfo>Kein Beitrag mit ID") || br.containsHTML("<statuscode>wrongParameter</statuscode>")) {
                decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
                return decryptedLinks;
            }
            subtitleInfo = br.getRegex("<caption>(.*?)</caption>").getMatch(0);
            final String basename = getXML("basename");
            if (basename == null) {
                return null;
            }

            date = getXML("airtime");
            title = getTitle(br);
            show = this.getXML("originChannelTitle");
            if (show == null) {
                /* E.g. for tivi.de */
                show = this.getXML("ns2:broadcast-name");
            }
            String extension = ".mp4";
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

            this.br.getPage("http://www.zdf.de/ptmd/vod/mediathek/" + basename);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) entries.get("formitaeten");
            ArrayList<Object> templist = null;

            for (final Object formato : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) formato;
                final String type = (String) entries.get("type");
                String fmt = (String) entries.get("quality");
                String url = (String) DummyScriptEnginePlugin.walkJson(entries, "playouts/main/uris/{0}");
                if (type == null || fmt == null || url == null) {
                    continue;
                }

                /* E.g. http://www.metafilegenerator.de/ondemand/zdf/hbbtv/none/zdf/16/03/160304_top_mom_2328k_p35v12.mp4 */
                boolean isHBBTV = false;
                final String fixme = new Regex(url, "https?://(?:www\\.)?metafilegenerator\\.de/ondemand/zdf/hbbtv/([A-Za-z0-9]+/zdf/\\d+/\\d+/[^<>\"]+\\.mp4)").getMatch(0);
                if (fixme != null) {
                    /* E.g. http://rodl.zdf.de/none/zdf/16/03/160304_top_mom_2328k_p35v12.mp4 */
                    /* Fix invalid / unauthorized hbbtv urls so that we get downloadable http urls */
                    url = "http://rodl.zdf.de/" + fixme;
                    isHBBTV = true;
                }
                if (!isHBBTV) {
                    /* Okay hbbtv formats do not necessarily have hbbtv urls ... */
                    try {
                        templist = (ArrayList) entries.get("facets");
                        for (final Object faceto : templist) {
                            final String facet = (String) faceto;
                            if ("hbbtv".equals(facet)) {
                                isHBBTV = true;
                                break;
                            }
                        }
                    } catch (final Throwable e) {
                    }
                }

                if (fmt != null) {
                    fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
                }

                // if (!type.contains("mp4_http")) {
                // continue;
                // }
                /* low is usually not available via hbbtv so we will use a non hbbtv variant for this */
                final boolean isLowNonHbbtvQuality = "low".equals(fmt) && type.contains("mp4_http");
                /** 2016-03-14: Only allow hbbtv variants as they have higher bitrates than other http variants */
                if (!isHBBTV && !isLowNonHbbtvQuality) {
                    continue;
                }
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
                        /* TODO: Check this! */
                        // if (type.contains("mp4_rtmp")) {
                        // if (url.startsWith("http://")) {
                        // Browser rtmp = new Browser();
                        // rtmp.getPage(stream[1]);
                        // url = rtmp.getRegex("<default\\-stream\\-url>([^<]+)<").getMatch(0);
                        // }
                        // if (url == null) {
                        // continue;
                        // }
                        // }
                        fmt = "hd";
                    }
                }
                final String fmtUPPR = fmt.toUpperCase(Locale.ENGLISH);
                final String name = date_formatted + "_zdf_" + show + " - " + title + "@" + fmtUPPR + extension;
                final DownloadLink link = createDownloadlink(String.format(decrypterurl, fmt));
                if (this.fastlinkcheck) {
                    link.setAvailable(true);
                }
                link.setFinalFileName(name);
                link.setContentUrl(PARAMETER_ORIGINAL);
                link.setProperty("date", date_formatted);
                link.setProperty("directURL", url);
                link.setProperty("directName", name);
                link.setProperty("directQuality", type);
                link.setProperty("streamingType", "http");
                link.setProperty("directfmt", fmtUPPR);

                DownloadLink best = bestMap.get(fmt);
                if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                    bestMap.put(fmt, link);
                }
                newRet.add(link);
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
                final String name = date_formatted + "_zdf_" + show + " - " + title + "@" + dlfmt + ".xml";
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

    private String createZdfLink(final String videoid) {
        return "http://www.zdf.de/ZDFmediathek/beitrag/video/" + videoid + "/#JDownloader";
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>\"]*?)\\]\\]></" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
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