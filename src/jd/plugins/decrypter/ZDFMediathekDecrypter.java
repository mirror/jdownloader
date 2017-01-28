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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "neo-magazin-royale.de", "heute.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/.+/[A-Za-z0-9_\\-]+\\.html", "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+", "https?://(?:www\\.)?heute\\.de/.+" })
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

    private final String        TYPE_ZDF                      = "https?://(?:www\\.)?zdf\\.de/.+";
    private final String        TYPE_ZDF_MEDIATHEK            = "https?://(?:www\\.)?zdf\\.de/.+/[^/]+\\.html";
    private final String        TYPE_ZDF_EMBEDDED_HEUTE       = "https?://(?:www\\.)?heute\\.de/.+";
    private final String        TYPE_ZDF_EMBEDDED_NEO_MAGAZIN = "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+";

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
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

        if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_HEUTE)) {
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

    // private String getApiTokenFromHtml() {
    // String apitoken = this.br.getRegex("apiToken\\s*?:\\s*?\\'([a-f0-9]+)\\'").getMatch(0);
    // if (apitoken == null) {
    // apitoken = PluginJSonUtils.getJsonNested(this.br, "apiToken");
    // }
    // return apitoken;
    // }

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

    public static String formatDateZDF(String input) {
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}