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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.hls.HlsContainer;
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
    private boolean             grabBest                      = false;

    ArrayList<DownloadLink>     decryptedLinks                = new ArrayList<DownloadLink>();
    private String              PARAMETER                     = null;
    private String              PARAMETER_ORIGINAL            = null;
    private String              url_subtitle                  = null;

    private boolean             fastlinkcheck                 = false;
    private boolean             grabSubtitles                 = false;
    private long                filesizeSubtitle              = 0;

    private final String        TYPE_ZDF                      = "https?://(?:www\\.)?zdf\\.de/.+";
    private final String        TYPE_ZDF_EMBEDDED_HEUTE       = "https?://(?:www\\.)?heute\\.de/.+";
    private final String        TYPE_ZDF_EMBEDDED_NEO_MAGAZIN = "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+";

    private final String        API_BASE                      = "https://api.zdf.de/";

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
        grabBest = cfg.getBooleanProperty(Q_BEST, false);
        fastlinkcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        grabSubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);

        setBrowserExclusive();
        br.setFollowRedirects(true);

        if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_HEUTE)) {
            this.crawlEmbeddedUrlsHeute();
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_NEO_MAGAZIN)) {
            this.crawlEmbeddedUrlsNeoMagazin(cfg);
        } else if (PARAMETER_ORIGINAL.matches(TYPE_ZDF)) {
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

    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url.replaceAll("https?://", "decryptedmediathek://"));
        if (this.fastlinkcheck) {
            dl.setAvailable(true);
        }
        return dl;
    }

    private void crawlEmbeddedUrlsHeute() throws Exception {
        br.getPage(this.PARAMETER);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final String[] ids = this.br.getRegex("\"videoId\"\\s*?:\\s*?\"([^\"]*?)\"").getColumn(0);
        for (final String videoid : ids) {
            /* These urls go back into the decrypter. */
            final String mainlink = "https://www." + this.getHost() + "/nachrichten/heute-journal/" + videoid + ".html";
            decryptedLinks.add(super.createDownloadlink(mainlink));
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
            /* These urls go back into the decrypter. */
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
                decryptedLinks.add(super.createDownloadlink(mainlink));
                /* Return --> Done */
                return;
            }
            decryptedLinks.add(super.createDownloadlink(mainlink));
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
        final String sophoraID = new Regex(this.PARAMETER, "/([^/]+)\\.html").getMatch(0);
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        // final String apitoken = getApiToken();
        /* 2016-12-21: By hardcoding the apitoken we can save one http request thus have a faster crawl process :) */
        final String apitoken = "f4ba81fa117681c42383194a7103251db2981962";
        this.br.getHeaders().put("Api-Auth", "Bearer " + apitoken);
        this.br.getPage(API_BASE + "/content/documents/" + sophoraID + ".json?profile=player");

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

        /* 2017-02-03: Not required at the moment */
        // if (!hasVideo) {
        // logger.info("Content is not a video --> Nothing to download");
        // return ret;
        // }

        if (inValidate(contentType) || inValidate(title) || inValidate(editorialDate) || inValidate(tvStation) || inValidate(player_url_template)) {
            return null;
        }

        date_formatted = new Regex(editorialDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        id = new Regex(player_url_template, "/([^/]+)$").getMatch(0);
        if (date_formatted == null || id == null) {
            return null;
        }

        short counter = 0;
        short highestHlsMasterValue = 0;
        short hlsMasterValueTemp = 0;
        boolean grabDownloadUrls = false;
        boolean finished = false;
        do {

            if (this.isAbort()) {
                return ret;
            }

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
            final Object subtitleO = JavaScriptEngineFactory.walkJson(entries, "captions/{0}/uri");
            url_subtitle = subtitleO != null ? (String) subtitleO : null;
            if (this.url_subtitle != null & this.grabSubtitles) {
                /* Grab the filesize here once so if the user adds many links, JD will not check the same subtitle URL multiple times. */
                URLConnectionAdapter con = null;
                try {
                    con = this.br.openHeadConnection(this.url_subtitle);
                    if (!con.getContentType().contains("html")) {
                        filesizeSubtitle = con.getLongContentLength();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }

            for (final Object priority_o : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) priority_o;
                final ArrayList<Object> formitaeten = (ArrayList<Object>) entries.get("formitaeten");
                for (final Object formitaet_o : formitaeten) {
                    entries = (LinkedHashMap<String, Object>) formitaet_o;

                    final boolean isAdaptive = ((Boolean) entries.get("isAdaptive")).booleanValue();
                    final String type = (String) entries.get("type");
                    String protocol = "http";
                    if (isAdaptive && !type.contains("m3u8")) {
                        /* 2017-02-03: Skip HDS as HLS already contains all segment quelities. */
                        continue;
                    } else if (isAdaptive) {
                        protocol = "hls";
                    }

                    final ArrayList<Object> qualities = (ArrayList<Object>) entries.get("qualities");
                    /* TODO: Skip unwanted types here! */
                    for (final Object qualities_o : qualities) {

                        if (this.isAbort()) {
                            return ret;
                        }

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
                                /* http webm streams. */
                                ext = ".webm";
                            } else {
                                /* http mp4- and segment streams. */
                                ext = ".mp4";
                            }

                            String linkid;
                            String final_filename;
                            final String linkid_format = "%s_%s_%s_%s_%s_%s";
                            final String final_filename_format = "%s_%s_%s_%s_%s%s";

                            DownloadLink dl;
                            if (isAdaptive) {
                                /* Segment download */
                                final String hls_master_quality_str = new Regex(uri, "m3u8/(\\d+)/").getMatch(0);
                                if (hls_master_quality_str == null) {
                                    /*
                                     * Fatal failure - without this value we cannot know which hls masters we already crawled and which not
                                     * resulting in unnecessary http requests!
                                     */
                                    continue;
                                }
                                hlsMasterValueTemp = Short.parseShort(hls_master_quality_str);
                                if (hlsMasterValueTemp <= highestHlsMasterValue) {
                                    /* Skip hls masters which we have already decrypted. */
                                    continue;
                                }
                                /* Access (hls) master. */
                                this.br.getPage(uri);
                                final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(this.br);
                                for (final HlsContainer hlscontainer : allHlsContainers) {
                                    final String resolution = hlscontainer.getResolution();
                                    final String final_download_url = hlscontainer.getDownloadurl();
                                    linkid = String.format(linkid_format, id, type, cdn, language, protocol, resolution);
                                    final_filename = encodeUnicode(String.format(final_filename_format, date_formatted, tvStation, show, title, linkid, ext));
                                    dl = createDownloadlink(final_download_url);
                                    setDownloadlinkProperties(dl, date_formatted, final_filename, type, linkid);
                                    addDownloadLink(dl);
                                }
                                /* Set this so we do not crawl this particular hls master again next round. */
                                highestHlsMasterValue = hlsMasterValueTemp;

                            } else {
                                /* http download */
                                linkid = String.format(linkid_format, id, type, cdn, language, protocol, quality);
                                final_filename = encodeUnicode(String.format(final_filename_format, date_formatted, tvStation, show, title, linkid, ext));
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

                                dl = createDownloadlink(uri.replaceAll("https?://", "decryptedmediathek://"));
                                /* Usually the filesize is only given for the official downloads. */
                                if (filesize > 0) {
                                    dl.setAvailable(true);
                                    dl.setDownloadSize(filesize);
                                }
                                setDownloadlinkProperties(dl, date_formatted, final_filename, type, linkid);
                                addDownloadLink(dl);
                            }
                        }
                    }

                }
            }
            counter++;
        } while (!finished);

        /* TODO: Finally, check which qualities the user actually wants to have. */

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

    private void addDownloadLink(final DownloadLink dl) {
        decryptedLinks.add(dl);
        if (grabSubtitles && this.url_subtitle != null) {
            final String current_ext = dl.getFinalFileName().substring(dl.getFinalFileName().lastIndexOf("."));
            final String final_filename = dl.getFinalFileName().replace(current_ext, ".xml");
            final String linkid = dl.getLinkID() + "_subtitle";
            final DownloadLink dl_subtitle = this.createDownloadlink(this.url_subtitle);
            setDownloadlinkProperties(dl_subtitle, dl.getStringProperty("date", null), final_filename, "subtitle", linkid);
            if (filesizeSubtitle > 0) {
                dl_subtitle.setDownloadSize(filesizeSubtitle);
                dl_subtitle.setAvailable(true);
            }
            decryptedLinks.add(dl_subtitle);
        }
    }

    private void setDownloadlinkProperties(final DownloadLink dl, final String date_formatted, final String final_filename, final String type, final String linkid) {
        dl.setFinalFileName(final_filename);
        dl.setLinkID(linkid);
        dl.setProperty("date", date_formatted);
        dl.setProperty("directName", final_filename);
        dl.setProperty("streamingType", type);
        dl.setContentUrl(PARAMETER_ORIGINAL);
    }

    private void accessPlayerJson(final String player_url_template, final String playerID) throws IOException {
        /* E.g. "/tmd/2/{playerId}/vod/ptmd/mediathek/161215_sendungroyale065ddm_nmg" */
        String player_url = player_url_template.replace("{playerId}", playerID);
        if (player_url.startsWith("/")) {
            player_url = API_BASE + player_url;
        }
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