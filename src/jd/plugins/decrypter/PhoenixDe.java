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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "phoenix.de", "tivi.de" }, urls = { "https?://(?:www\\.)?phoenix\\.de/content/\\d+|http://(?:www\\.)?phoenix\\.de/podcast/[A-Za-z0-9]+/video/rss\\.xml", "https?://(?:www\\.)?tivi\\.de/(mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?|tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+)" })
public class PhoenixDe extends PluginForDecrypt {

    public PhoenixDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String Q_SUBTITLES        = "Q_SUBTITLES";
    private static final String Q_BEST             = "Q_BEST";
    private static final String Q_LOW              = "Q_LOW";
    private static final String Q_HIGH             = "Q_HIGH";
    private static final String Q_VERYHIGH         = "Q_VERYHIGH";
    private static final String Q_HD               = "Q_HD";
    private static final String FASTLINKCHECK      = "FASTLINKCHECK";
    private boolean             BEST               = false;

    ArrayList<DownloadLink>     decryptedLinks     = new ArrayList<DownloadLink>();
    private String              PARAMETER = null;
    boolean                     fastlinkcheck      = false;

    private final String        TYPE_PHOENIX       = "https?://(?:www\\.)?phoenix\\.de/content/\\d+";
    private final String        TYPE_PHOENIX_RSS   = "http://(?:www\\.)?phoenix\\.de/podcast/.+";

    private final String        TYPE_TIVI          = "https?://(?:www\\.)?tivi\\.de/(?:mediathek/[a-z0-9\\-]+\\-(\\d+)/[a-z0-9\\-]+\\-(\\d+)/?|tiviVideos/beitrag/title/(\\d+)/(\\d+)\\?view=.+)";
    private final String        TYPE_TIVI_1        = "https?://(?:www\\.)?tivi\\.de/mediathek/[a-z0-9\\-]+\\-\\d+/[a-z0-9\\-]+\\-\\d+/?";
    private final String        TYPE_TIVI_2        = "https?://(?:www\\.)?tivi\\.de/tiviVideos/beitrag/title/\\d+/\\d+\\?view=.+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(500);
        PARAMETER = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig(Browser.getHost(PARAMETER));
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        this.fastlinkcheck = cfg.getBooleanProperty(FASTLINKCHECK, false);
        if (PARAMETER.matches(TYPE_PHOENIX_RSS)) {
            decryptPhoenixRSS();
        } else {
            getDownloadLinksZdfOld(cfg);
        }

        return decryptedLinks;
    }

    private void decryptPhoenixRSS() throws IOException {
        br.getPage(this.PARAMETER);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER));
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
            if (PARAMETER.matches(TYPE_PHOENIX)) {
                br.getPage(PARAMETER);
                if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                    decryptedLinks.add(this.createOfflinelink(PARAMETER));
                    return decryptedLinks;
                }
                id = br.getRegex("id=\"phx_vod_(\\d+)\"").getMatch(0);
                if (id == null) {
                    decryptedLinks.add(this.createOfflinelink(PARAMETER));
                    return decryptedLinks;
                }
                id_filename = id;
                decrypterurl = "decrypted://phoenix.de/content/" + id + "&quality=%s";
                br.getPage("/php/zdfplayer-v1.3/data/beitragsDetails.php?ak=web&id=" + id);
            } else if (this.PARAMETER.matches(TYPE_TIVI)) {
                final String param_1;
                final String param_2;
                if (this.PARAMETER.matches(TYPE_TIVI_1)) {
                    param_1 = new Regex(this.PARAMETER, TYPE_TIVI).getMatch(0);
                    param_2 = new Regex(this.PARAMETER, TYPE_TIVI).getMatch(1);
                } else {
                    param_1 = new Regex(this.PARAMETER, TYPE_TIVI).getMatch(2);
                    param_2 = new Regex(this.PARAMETER, TYPE_TIVI).getMatch(3);
                }
                id_filename = param_1 + "_" + param_2;
                decrypterurl = "decrypted://tivi.de/content/" + param_1 + param_2 + "&quality=%s";
                br.getPage("http://www.tivi.de/tiviVideos/beitrag/" + param_1 + "/" + param_2 + "?view=flashXml");
            }
            if (br.containsHTML("<debuginfo>Kein Beitrag mit ID") || br.containsHTML("<statuscode>wrongParameter</statuscode>")) {
                decryptedLinks.add(this.createOfflinelink(PARAMETER));
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

            date_formatted = jd.plugins.decrypter.ZDFMediathekDecrypter.formatDateZDF(date);

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
                    link.setContentUrl(PARAMETER);
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
                subtitle.setContentUrl(PARAMETER);
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

    private String getTitle(final Browser br) {
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

}
