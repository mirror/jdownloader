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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

// http://tvthek,orf.at/live/... --> HDS
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvthek.orf.at" }, urls = { "https?://(?:www\\.)?tvthek\\.orf\\.at/(?:index\\.php/)?(?:programs?|topic|profile)/.+" })
public class ORFMediathekDecrypter extends PluginForDecrypt {
    private static final String TYPE_TOPIC    = "https?://(www\\.)?tvthek\\.orf\\.at/topic/.+";
    private static final String TYPE_PROGRAMM = "https?://(www\\.)?tvthek\\.orf\\.at/programs?/.+";

    public ORFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/index.php/", "/");
        this.br.setAllowedResponseCodes(500);
        this.br.setLoadLimit(this.br.getLoadLimit() * 4);
        br.getPage(parameter);
        int status = br.getHttpConnection().getResponseCode();
        if (status == 301 || status == 302) {
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                parameter = br.getRedirectLocation();
                br.getPage(parameter);
            }
        } else if (status != 200) {
            final DownloadLink link = this.createOfflinelink(parameter);
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        if (br.containsHTML("(404 \\- Seite nicht gefunden\\.|area_headline error_message\">Keine Sendung vorhanden<)") || !br.containsHTML("jsb_VideoPlaylist") || status == 404 || status == 500) {
            final DownloadLink link = this.createOfflinelink(parameter);
            link.setName(new Regex(parameter, "tvthek\\.orf\\.at/programs/(.+)").getMatch(0));
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        decryptedLinks.addAll(getDownloadLinks(parameter, SubConfiguration.getConfig("orf.at")));
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            if (parameter.matches(TYPE_TOPIC)) {
                logger.warning("MAYBE Decrypter out of date for link: " + parameter);
            } else {
                logger.warning("Decrypter for sure out of date for link: " + parameter);
            }
            return null;
        }
        return decryptedLinks;
    }

    @SuppressWarnings({ "deprecation", "unchecked", "unused", "rawtypes" })
    private ArrayList<DownloadLink> getDownloadLinks(final String data, final SubConfiguration cfg) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String nicehost = new Regex(data, "https?://(?:www\\.)?([^/]+)").getMatch(0);
        final String decryptedhost = "http://" + nicehost + "decrypted";
        String date_formatted = null;
        final String date = PluginJSonUtils.getJsonValue(this.br, "date");
        if (date != null) {
            date_formatted = formatDate(date);
        }
        boolean allow_HTTP = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HTTP_STREAM, true);
        boolean allow_HDS = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HDS_STREAM, true);
        boolean allow_HLS = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HLS_STREAM, true);
        if (allow_HDS == allow_HLS == allow_HTTP == false) {
            allow_HDS = true;
            allow_HLS = true;
            allow_HTTP = true;
        }
        final boolean BEST = SubConfiguration.getConfig("orf.at").getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_BEST, true);
        try {
            String json = this.br.getRegex("class=\"jsb_ jsb_VideoPlaylist\" data\\-jsb=\"([^<>\"]+)\"").getMatch(0);
            if (json != null) {
                String quality = null, key = null, title = null;
                /* jsonData --> HashMap */
                json = Encoding.htmlDecode(json);
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                entries = (LinkedHashMap<String, Object>) entries.get("playlist");
                String fpName = "";
                title = (String) entries.get("title");
                final String id_playlist = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
                if (id_playlist.equals("0")) {
                    return null;
                }
                ArrayList<Object> video = (ArrayList) entries.get("videos");
                final Map<String, List<DownloadLink>> map = new HashMap<String, List<DownloadLink>>();
                if (title == null) {
                    title = getTitle(br);
                }
                if (date_formatted != null) {
                    fpName = date_formatted + "_";
                }
                fpName += title + "_" + id_playlist;
                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) {
                    extension = ".mp3";
                }
                for (final Object videoo : video) {
                    final LinkedHashMap<String, Object> entries_video = (LinkedHashMap<String, Object>) videoo;
                    final ArrayList<Object> sources_video = (ArrayList) entries_video.get("sources");
                    ArrayList<Object> subtitle_list = null;
                    final Object sources_subtitle_o = entries_video.get("subtitles");
                    final String id_individual_video = Long.toString(JavaScriptEngineFactory.toLong(entries_video.get("id"), 0));
                    if (id_individual_video.equals("0")) {
                        return null;
                    }
                    final String description = (String) entries_video.get("description");
                    String titlethis = (String) entries_video.get("title");
                    if (titlethis == null) {
                        titlethis = description;
                    }
                    if (titlethis != null && titlethis.length() > 80) {
                        titlethis = titlethis.substring(0, 80);
                    }
                    String vIdTemp = "";
                    String bestFMT = null;
                    String subtitle = null;
                    FilePackage fp = null;
                    if (titlethis != null) {
                        fp = FilePackage.getInstance();
                        fp.setName(fpName);
                    }
                    for (final Object sourceo : sources_video) {
                        subtitle = null;
                        final LinkedHashMap<String, Object> entry_source = (LinkedHashMap<String, Object>) sourceo;
                        /* Backward compatibility with xml method */
                        final String url_directlink_video = (String) entry_source.get("src");
                        String fmt = (String) entry_source.get("quality");
                        final String protocol = (String) entry_source.get("protocol");
                        final String delivery = (String) entry_source.get("delivery");
                        // final String subtitleUrl = (String) entry_source.get("SubTitleUrl");
                        if (isEmpty(url_directlink_video) && isEmpty(fmt) && isEmpty(protocol) && isEmpty(delivery)) {
                            continue;
                        }
                        if (sources_subtitle_o != null) {
                            /* [0] = .srt, [1] = WEBVTT .vtt */
                            subtitle_list = (ArrayList) sources_subtitle_o;
                            if (subtitle_list.size() > 1) {
                                subtitle = (String) JavaScriptEngineFactory.walkJson(subtitle_list.get(1), "src");
                            } else if (subtitle_list.size() == 1) {
                                subtitle = (String) JavaScriptEngineFactory.walkJson(subtitle_list.get(0), "src");
                            } else {
                                subtitle = null;
                            }
                        }
                        // available protocols: http, rtmp, rtsp, hds, hls
                        if (!"http".equals(protocol)) {
                            continue;
                        } else if ("progressive".equals(delivery) && !allow_HTTP) {
                            continue;
                        } else if ("hls".equals(delivery) && !allow_HLS) {
                            continue;
                        } else if ("hds".equals(delivery) && !allow_HDS) {
                            continue;
                        } else if (url_directlink_video == null || isEmpty(fmt)) {
                            continue;
                        }
                        long filesize = 0;
                        final String selector = protocol + delivery;
                        String fileName = titlethis + "@" + selector;
                        fileName += "_" + id_playlist + "_" + id_individual_video;
                        fileName += "@" + humanReadableQualityIdentifier(fmt.toUpperCase(Locale.ENGLISH).trim());
                        fileName = fileName.replaceAll("\"", "");
                        fileName = fileName.replaceAll(":\\s|\\s\\|\\s", " - ").trim();
                        final String ext_from_directurl = getFileNameExtensionFromString(url_directlink_video);
                        if (ext_from_directurl.length() == 4 && !StringUtils.equalsIgnoreCase(ext_from_directurl, ".f4m") && !StringUtils.equalsIgnoreCase(ext_from_directurl, ".hls")) {
                            extension = ext_from_directurl;
                        }
                        final String fmtQuality = fmt;
                        fmt = humanReadableQualityIdentifier(fmt.toUpperCase(Locale.ENGLISH).trim());
                        boolean sub = true;
                        if (fileName.equals(vIdTemp)) {
                            sub = false;
                        }
                        if ("VERYHIGH".equals(fmt) || "ADAPTIV".equals(fmt) || BEST) {
                            /*
                             * VERYHIGH is always available but is not always REALLY available which means we have to check this here and
                             * skip it if needed! Filesize is also needed to find BEST quality.
                             */
                            if ("progressive".equals(delivery)) {
                                boolean veryhigh_is_available = true;
                                try {
                                    final URLConnectionAdapter con = br.openHeadConnection(url_directlink_video);
                                    if (!con.isOK()) {
                                        veryhigh_is_available = false;
                                    } else {
                                        /*
                                         * Basically we already did the availablecheck here so for this particular quality we don't have to
                                         * do it again in the linkgrabber!
                                         */
                                        filesize = con.getLongContentLength();
                                    }
                                    try {
                                        con.disconnect();
                                    } catch (final Throwable e) {
                                    }
                                } catch (final Throwable e) {
                                    veryhigh_is_available = false;
                                }
                                if (!veryhigh_is_available) {
                                    continue;
                                }
                            }
                        }
                        /* best selection is done at the end */
                        if ("LOW".equals(fmt)) {
                            if ((cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_LOW, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "LOW";
                            }
                        } else if ("MEDIUM".equals(fmt)) {
                            if ((cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_MEDIUM, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "MEDIUM";
                            }
                        } else if ("HIGH".equals(fmt)) {
                            if ((cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_HIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "HIGH";
                            }
                        } else if ("VERYHIGH".equals(fmt)) {
                            if ((cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_VERYHIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "VERYHIGH";
                            }
                        } else if ("ADAPTIV".equals(fmt)) {
                            if (true) {
                                continue;
                            } else {
                                if ((cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_VERYHIGH, true) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "ADAPTIV";
                                }
                            }
                        } else {
                            if (unknownQualityIdentifier(fmt)) {
                                logger.info("ORFMediathek Decrypter: unknown quality identifier --> " + fmt);
                                logger.info("Link: " + data);
                            }
                            continue;
                        }
                        final String final_filename_without_extension = fileName + (protocol != null ? "_" + protocol : "");
                        final String final_filename_video = final_filename_without_extension + extension;
                        final DownloadLink link = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        final String server_filename = getFileNameFromURL(new URL(url_directlink_video));
                        String linkid_video = id_playlist + id_individual_video + fmt + protocol + delivery;
                        if (server_filename != null) {
                            /* Server filename should always be available! */
                            linkid_video += server_filename;
                        }
                        link.setFinalFileName(final_filename_video);
                        link.setContentUrl(data);
                        link.setProperty("directURL", url_directlink_video);
                        link.setProperty("directName", final_filename_video);
                        link.setProperty("directFMT", fmt);
                        link.setProperty("directQuality", fmtQuality);
                        link.setProperty("mainlink", data);
                        if (protocol == null && delivery == null) {
                            link.setAvailable(true);
                            link.setProperty("streamingType", "rtmp");
                        } else {
                            link.setProperty("streamingType", protocol);
                            link.setProperty("delivery", delivery);
                            if (filesize > 0) {
                                link.setAvailable(true);
                                link.setDownloadSize(filesize);
                            } else if (!"http".equals(protocol)) {
                                link.setAvailable(true);
                            } else if (!"progressive".equals(delivery)) {
                                link.setAvailable(true);
                            }
                        }
                        if (fp != null) {
                            link._setFilePackage(fp);
                        }
                        link.setLinkID(linkid_video);
                        List<DownloadLink> list = map.get(fmt);
                        if (list == null) {
                            list = new ArrayList<DownloadLink>();
                            map.put(fmt, list);
                        }
                        list.add(link);
                        if (sub) {
                            if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_SUBTITLES, false)) {
                                if (!isEmpty(subtitle)) {
                                    final String final_filename_subtitle = final_filename_without_extension + ".srt";
                                    final DownloadLink subtitle_downloadlink = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                                    subtitle_downloadlink.setProperty("directURL", subtitle);
                                    subtitle_downloadlink.setProperty("directName", final_filename_subtitle);
                                    subtitle_downloadlink.setProperty("streamingType", "subtitle");
                                    subtitle_downloadlink.setProperty("mainlink", data);
                                    subtitle_downloadlink.setAvailable(true);
                                    subtitle_downloadlink.setFinalFileName(final_filename_subtitle);
                                    subtitle_downloadlink.setContentUrl(data);
                                    subtitle_downloadlink.setLinkID(linkid_video + "_subtitle");
                                    if (fp != null) {
                                        subtitle_downloadlink._setFilePackage(fp);
                                    }
                                    final String subtitle_list_key = "sub" + fmt;
                                    list = map.get(subtitle_list_key);
                                    if (list == null) {
                                        list = new ArrayList<DownloadLink>();
                                        map.put(subtitle_list_key, list);
                                    }
                                    list.add(subtitle_downloadlink);
                                    vIdTemp = fileName;
                                }
                            }
                        }
                    }
                    if (BEST) {
                        final String[] qualities = { "VERYHIGH", "HIGH", "MEDIUM", "LOW" };
                        for (final String qual : qualities) {
                            List<DownloadLink> list = map.get(qual);
                            if (list != null) {
                                ret.addAll(list);
                                /* Add subtitle */
                                list = map.get("sub" + qual);
                                if (list != null) {
                                    ret.addAll(list);
                                }
                                break;
                            }
                        }
                    } else {
                        for (final List<DownloadLink> links : map.values()) {
                            ret.addAll(links);
                        }
                    }
                    map.clear();
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        return ret;
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<title>(.*?)\\s*\\-\\s*ORF TVthek</title>").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("\'playerTitle\':\\s*\'([^\'])\'$").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        return title;
    }

    private String humanReadableQualityIdentifier(String s) {
        final String humanreabable;
        if ("Q1A".equals(s)) {
            humanreabable = "LOW";
        } else if ("Q4A".equals(s)) {
            humanreabable = "MEDIUM";
        } else if ("Q6A".equals(s)) {
            humanreabable = "HIGH";
        } else if ("Q8C".equals(s)) {
            humanreabable = "VERYHIGH";
        } else if ("QXB".equals(s)) {
            humanreabable = "ADAPTIV";
        } else {
            humanreabable = null;
        }
        return humanreabable;
    }

    private boolean unknownQualityIdentifier(String s) {
        if (s.matches("(DESCRIPTION|SMIL|SUBTITLEURL|DURATION|TRANSCRIPTURL|TITLE|QUALITY|QUALITY_STRING|PROTOCOL|TYPE|DELIVERY)")) {
            return false;
        }
        return true;
    }

    private String formatDate(String input) {
        if (input.contains(":")) {
            input = input.substring(0, input.lastIndexOf(":"));
        }
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm", Locale.GERMAN);
        // if (input.matches("\\d+T\\d{2}\\d{2}")) {
        // date = TimeFormatter.getMilliSeconds(input, "yyyyMMdd'T'HHmm", Locale.GERMAN);
        // } else {
        // if (input.contains(":")) {
        // input = input.substring(0, input.lastIndexOf(":")) + "00";
        // }
        // date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.GERMAN);
        // }
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