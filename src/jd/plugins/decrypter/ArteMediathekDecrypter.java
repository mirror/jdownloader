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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "www.arte.tv" }, urls = { "http://(videos|www).arte\\.tv/(guide/[a-z]{2}/[0-9\\-]+|[a-z]{2}/videos)/.+" }, flags = { 0 })
public class ArteMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES = "Q_SUBTITLES";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_HIGH      = "Q_HIGH";
    private static final String Q_VERYHIGH  = "Q_VERYHIGH";
    private static final String Q_HD        = "Q_HD";
    private static final String HBBTV       = "HBBTV";
    private static final String THUMBNAIL   = "THUMBNAIL";
    private boolean             BEST        = false;

    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        int status = br.getHttpConnection().getResponseCode();
        if (!parameter.contains("tv/guide/") && status == 200) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://"));
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        /* new arte+7 handling */
        if (status == 301 || status == 302) {
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                parameter = br.getRedirectLocation();
                br.getPage(parameter);
            }
        } else if (status != 200) {
            // Check...if offline, add to llinkgrabber so user can see it
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://"));
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }

        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        decryptedLinks.addAll(getDownloadLinks(parameter, cfg));

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) return true;
        return false;
    }

    private ArrayList<DownloadLink> getDownloadLinks(final String data, final SubConfiguration cfg) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        try {
            int languageVersion = 1;
            String lang = new Regex(data, "/guide/(\\w+)/.+").getMatch(0);
            if (lang != null) {
                if ("fr".equalsIgnoreCase(lang)) languageVersion = 2;
            }
            lang = language(languageVersion);

            String ID = new Regex(data, "/guide/\\w+/([0-9\\-]+)/").getMatch(0);
            if (ID != null && lang != null) {
                String title = getTitle(br);

                String tvguideUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/player/" + lang + "/" + ID + "_PLUS7-" + lang + "/ALL/ALL.json";
                String vsrRegex = "\"VSR\":\\{(.*?\\})\\}";
                String strRegex = "\"(.*?)\"\\s*:\\s*\\{(.*?)\\}";
                String valRegex = "\"(.*?)\"\\s*:\\s*\"?(.*?)\"?,";
                if (cfg.getBooleanProperty(HBBTV, false)) {
                    br.getHeaders().put("User-Agent", "HbbTV/1.1.1 (;;;;;) jd-arte.tv-plugin");
                    tvguideUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/" + lang + "/" + ID + "_PLUS7-" + lang + "/HBBTV/ALL.json";
                    vsrRegex = "\"VSR\":\\[(.*?)\\]";
                    strRegex = "\\{(.*?\"VQU\":\"([^\"]+)\".*?)\\}";
                    valRegex = "\"([^\"]+)\":\"([^\"]+)\"";
                }
                br.getPage(tvguideUrl);
                if (br.containsHTML("<statuscode>wrongParameter</statuscode>")) return ret;

                /* parsing json */
                HashMap<String, HashMap<String, String>> streamValues = new HashMap<String, HashMap<String, String>>();
                HashMap<String, String> streamValue;
                String vsr = br.getRegex(vsrRegex).getMatch(0);
                if (vsr == null) {
                    final DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://"));
                    link.setAvailable(false);
                    link.setProperty("offline", true);
                    ret.add(link);
                    return ret;
                }
                for (int i = 0; i < 2; i++) {
                    for (String[] ss : new Regex(vsr, strRegex).getMatches()) {
                        String l = new Regex(ss[0], "(\\d)").getMatch(0);
                        if (l != null) {
                            if (i == 0) {
                                if (Integer.parseInt(l) != languageVersion) continue;
                            } else {
                                languageVersion = Integer.parseInt(l);
                            }
                        }
                        streamValue = new HashMap<String, String>();
                        if (cfg.getBooleanProperty(HBBTV, false)) {
                            String tmp = ss[0];
                            ss[0] = ss[1];
                            ss[1] = tmp;
                        }
                        for (String[] peng : new Regex(ss[1], valRegex).getMatches()) {
                            streamValue.put(peng[0], peng[1]);
                        }
                        streamValues.put(ss[0], streamValue);
                    }

                    if (streamValues.size() > 0) break;
                }
                String VRA = br.getRegex("\"VRA\":\"([^\"]+)\"").getMatch(0);
                String VRU = br.getRegex("\"VRU\":\"([^\"]+)\"").getMatch(0);

                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                String lastQualityFMT = null;

                for (Entry<String, HashMap<String, String>> next : streamValues.entrySet()) {
                    streamValue = new HashMap<String, String>(next.getValue());
                    String streamType = next.getKey();
                    /* only http streams for the old stable */
                    if (!streamType.matches("HTTP_REACH_EQ_\\d|SQ|EQ|HQ") && isStableEnviroment()) continue;

                    String url = streamValue.get("url");
                    if (url == null) url = streamValue.get("VUR");

                    if (!streamType.matches("HTTP_REACH_EQ_\\d|SQ|EQ|HQ")) {
                        if (!url.startsWith("mp4:")) url = "mp4:" + url;
                        url = streamValue.get("streamer") + url;
                    }
                    String fmt = streamValue.get("quality");
                    if (fmt == null) fmt = hbbtv(streamValue.get("VQU"));
                    String quality = fmt;
                    if (fmt != null) fmt = fmt.split("\\-")[0].toLowerCase(Locale.ENGLISH).trim();
                    if (fmt != null) {
                        quality = quality.replaceAll("\\s", "");
                        /* best selection is done at the end */
                        if ("ld".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "ld";
                            }
                        } else if ("md".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "md";
                            }
                        } else if ("sd".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_VERYHIGH, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "sd";
                            }
                        } else if ("hd".equals(fmt)) {
                            if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == false) {
                                continue;
                            } else {
                                fmt = "hd";
                            }
                        }
                    }
                    lastQualityFMT = fmt.toUpperCase(Locale.ENGLISH);

                    final String name = title + "@" + quality + "_" + language(languageVersion) + extension;
                    DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://") + "&quality=" + quality);

                    link.setFinalFileName(name);
                    link.setBrowserUrl(data);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", name);
                    link.setProperty("tvguideUrl", tvguideUrl);
                    link.setProperty("VRA", convertDateFormat(VRA));
                    link.setProperty("VRU", convertDateFormat(VRU));

                    if (!cfg.getBooleanProperty(HBBTV, true)) {
                        link.setAvailable(true);
                        link.setProperty("directQuality", streamValue.get("quality"));
                        link.setProperty("streamingType", streamType);
                        link.setProperty("flashplayer", "http://www.arte.tv/player/v2//jwplayer6/mediaplayer.6.3.3242.swf");
                    } else {
                        link.setProperty("directQuality", streamValue.get("VQU"));
                        link.setProperty("streamingType", streamValue.get("VFO"));
                    }

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
                            lastQualityFMT = "VERYHIGH";
                            keep = bestMap.get("sd");
                        }
                        if (keep == null) {
                            lastQualityFMT = "HIGH";
                            keep = bestMap.get("md");
                        }
                        if (keep == null) {
                            lastQualityFMT = "LOW";
                            keep = bestMap.get("ld");
                        }
                        if (keep != null) {
                            newRet.clear();
                            newRet.add(keep);
                        }
                    }
                }
                if (cfg.getBooleanProperty(Q_SUBTITLES, false)) {
                    String subtitleInfo = br.getRegex("<caption>(.*?)</caption>").getMatch(0);
                    if (subtitleInfo != null) {
                        String subtitleUrl = new Regex(subtitleInfo, "<url>(http://utstreaming\\.zdf\\.de/tt/\\d{4}/[A-Za-z0-9_\\-]+\\.xml)</url>").getMatch(0);
                        final String startTime = new Regex(subtitleInfo, "<offset>(\\-)?(\\d+)</offset>").getMatch(1);
                        final String name = title + "@" + lastQualityFMT + ".xml";
                        final DownloadLink link = createDownloadlink("decrypted://zdf.de/subtitles/" + System.currentTimeMillis() + new Random().nextInt(1000000));
                        link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setProperty("directURL", subtitleUrl);
                        link.setProperty("directName", name);
                        link.setProperty("streamingType", "subtitle");
                        link.setProperty("starttime", startTime);
                        newRet.add(link);
                    }
                }
                if (cfg.getBooleanProperty(THUMBNAIL, false)) {
                    String thumbnailUrl = br.getRegex("\"programImage\":\"([^\"]+)\"").getMatch(0);
                    if (thumbnailUrl != null) {
                        final DownloadLink link = createDownloadlink(thumbnailUrl);
                        link.setFinalFileName(title + ".jpg");
                        newRet.add(link);
                    }
                }
                if (newRet.size() > 1) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(title);
                    fp.addLinks(newRet);
                }
                ret = newRet;
            } else {
                /*
                 * no other qualities
                 */
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        for (DownloadLink dl : ret) {
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
        }
        return ret;
    }

    private String language(int id) {
        if (id == 1) return "D";
        return "F";
    }

    private String hbbtv(String s) {
        if (s == null) return null;
        if ("SQ".equals(s)) return "HD";
        if ("EQ".equals(s)) return "MD";
        if ("HQ".equals(s)) return "SD";
        return "unknown";
    }

    private String convertDateFormat(String s) {
        if (s == null) return null;
        if (s.matches("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+ \\+\\d+")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.getDefault());
            SimpleDateFormat convdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            try {
                Date date = null;
                try {
                    date = df.parse(s);
                    s = convdf.format(date);
                } catch (Throwable e) {
                    df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.ENGLISH);
                    date = df.parse(s);
                    s = convdf.format(date);
                }
            } catch (Throwable e) {
                return s;
            }
        }
        return s;
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<title>(.*?) \\| ARTE</title>").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) title = br.getRegex("<h1 itemprop=\"name\" class=\"span\\d+\">([^<]+)</h1>").getMatch(0);
        if (title == null) title = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\| ARTE\">").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}