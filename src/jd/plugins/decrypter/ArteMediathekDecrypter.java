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
import java.text.ParseException;
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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "www.arte.tv" }, urls = { "http://((videos|www)\\.)?arte\\.tv/(guide/[a-z]{2}/[0-9\\-]+|[a-z]{2}/videos)/.+|http://concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+" }, flags = { 0 })
public class ArteMediathekDecrypter extends PluginForDecrypt {

    private static final String EXCEPTION_LINKOFFLINE = "EXCEPTION_LINKOFFLINE";

    private static final String TYPE_CONCERT          = "http://(www\\.)?concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+";
    private static final String TYPE_GUIDE            = "http://((videos|www)\\.)?arte\\.tv/guide/[a-z]{2}/[0-9\\-]+";

    private static final String Q_SUBTITLES           = "Q_SUBTITLES";
    private static final String Q_BEST                = "Q_BEST";
    private static final String Q_LOW                 = "Q_LOW";
    private static final String Q_HIGH                = "Q_HIGH";
    private static final String Q_VERYHIGH            = "Q_VERYHIGH";
    private static final String Q_HD                  = "Q_HD";
    private static final String HBBTV                 = "HBBTV";
    private static final String THUMBNAIL             = "THUMBNAIL";

    private static final String Q_LOW_INTERN          = "ld|300p";
    private static final String Q_HIGH_INTERN         = "md|406p";
    private static final String Q_VERYHIGH_INTERN     = "sd|400p";
    private static final String Q_HD_INTERN           = "hd|720p";
    private String              VRU                   = null;

    private static boolean      pluginloaded          = false;

    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* TODO: Add support for subtitled version + RTMP */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.matches(TYPE_CONCERT)) {
            if (!br.containsHTML("id=\"section\\-player\"")) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
        } else {
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
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
        }

        try {
            decryptedLinks.addAll(getDownloadLinks(parameter, br));
        } catch (final Exception e) {
            if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            throw e;
        }

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
        if (rev < 10000) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> getDownloadLinks(final String parameter, final Browser ibr) throws DecrypterException, IOException, ParseException {
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
        String vsr;
        String tvguideUrl;
        String title;
        // this allows drop to frame, and prevents subsequent NPE!
        br = ibr.cloneBrowser();

        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        final boolean BEST = cfg.getBooleanProperty(Q_BEST, false);
        final boolean preferHBBTV = cfg.getBooleanProperty(HBBTV, false);
        boolean grabSubtitles = cfg.getBooleanProperty(Q_SUBTITLES, false);
        /* Subtitled versions are only available in HBBTv */
        if (!preferHBBTV && grabSubtitles) {
            grabSubtitles = false;
        }
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        int languageVersion = 1;
        String lang = new Regex(parameter, "/guide/(\\w+)/.+").getMatch(0);
        if (lang != null) {
            if ("fr".equalsIgnoreCase(lang)) {
                languageVersion = 2;
            }
        }
        lang = language(languageVersion);

        String vsrRegex = "\"VSR\":\\{(.*?\\})\\}";

        if (parameter.matches(TYPE_CONCERT)) {
            tvguideUrl = br.getRegex("\"(http://concert\\.arte\\.tv/[a-z]{2}/player/\\d+)").getMatch(0);
            if (tvguideUrl == null) {
                return null;
            }
            br.getPage(tvguideUrl);
        } else {
            String ID = new Regex(parameter, "/guide/\\w+/([0-9\\-]+)/").getMatch(0);
            if (ID == null || lang == null) {
                return ret;
            }
            String tv_channel = br.getRegex("data\\-vid=(\"|\\')" + ID + "(_[A-Za-z0-9_\\-]+)\\-[A-Za-z]+(\"|\\')>").getMatch(1);
            if (tv_channel == null) {
                if (!br.containsHTML("arte_vp_config=")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                return null;
            }
            tvguideUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/player/" + lang + "/" + ID + tv_channel + "-" + lang + "/ALL/ALL.json";
            /* Old but useful code */
            // if (preferHBBTV) {
            // br.getHeaders().put("User-Agent", "HbbTV/1.1.1 (;;;;;) jd-arte.tv-plugin");
            // tvguideUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/" + lang + "/" + ID + tv_channel + "-" + lang +
            // "/HBBTV/ALL.json";
            // }
            br.getPage(tvguideUrl);
        }
        title = getTitleAPI(br);
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        if (br.containsHTML("<statuscode>wrongParameter</statuscode>")) {
            return ret;
        }
        String VRA = br.getRegex("\"VRA\":\"([^\"]+)\"").getMatch(0);
        VRU = br.getRegex("\"VRU\":\"([^\"]+)\"").getMatch(0);
        if (VRU.matches("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+ \\+\\d+")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.getDefault());
            final Date date = df.parse(VRU);
            /* Maybe their rights to show the video expired */
            if (date.getTime() <= System.currentTimeMillis()) {
                final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://"));
                link.setFinalFileName(title);
                link.setAvailable(false);
                link.setProperty("offline", true);
                ret.add(link);
                return ret;
            }
        }
        title = unescape(title);

        vsr = br.getRegex(vsrRegex).getMatch(0);
        /* If it's just empty, the video is probably not available in the users' country. */
        if (vsr == null || vsr.equals("")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://"));
            link.setFinalFileName(title);
            link.setAvailable(false);
            link.setProperty("offline", true);
            ret.add(link);
            return ret;
        }

        /* Needed for checks later - only set languageVersion to 3 if we know that it's actually available! */
        if (vsr.contains("\"versionCode\":\"VOF-STMF\"") && grabSubtitles) {
            logger.info("Subtitled versions available!");
            languageVersion = 3;
        }

        final String[][] qualities = new Regex(vsr, "\"([^<>\"/]*?)\":\\{(.*?)\\}").getMatches();
        String extension = ".mp4";
        for (final String[] qualinfoy : qualities) {
            final String info = qualinfoy[1];
            final String streamType = qualinfoy[0];
            final String streamer = getJson(info, "streamer");
            final String original_fmt = getJson(info, "quality");
            final String videoformat = getJson(info, "videoFormat");
            String fmt = original_fmt;
            if (fmt == null) {
                fmt = getJson(info, "VQU");
            }
            String url = getJson(info, "url");
            if (url == null) {
                url = getJson(info, "vur");
            }

            /* Check version/only download selected language-version! */
            String l = new Regex(info, "\"versionCode\":\"(VO|VF|VOF\\-STMF)\"").getMatch(0);
            if (l == null) {
                continue;
            }
            if ("VO".equals(l)) {
                l = "1";
            } else if ("VF".equals(l)) {
                l = "2";
            } else {
                l = "3";
            }
            final int langint = Integer.parseInt(l);
            if (langint != languageVersion) {
                continue;
            }
            /* Language check END */

            /* Do not decrypt hls streams */
            if (streamType.contains("HLS")) {
                continue;
            }
            /* only http streams for the old stable */
            if (!streamType.matches("HTTP_REACH_EQ_\\d|SQ|EQ|HQ") && isStableEnviroment()) {
                continue;
            }
            /* Obey HBBTv setting */
            if (preferHBBTV && !"HBBTV".equals(videoformat)) {
                continue;
            } else if (!preferHBBTV && "HBBTV".equals(videoformat) && !url.matches("rtmp://.+")) {
                /* No HBBTv preferred and/or no rtmp url available --> Ignore quality */
                continue;
            }
            /* this assumes that all non protocol urls belong to rtmp. */
            if (url != null && !url.matches("(https?|rtmp)://.+")) {
                if (!url.startsWith("mp4:")) {
                    url = "mp4:" + url;
                }
                if (streamer != null) {
                    url = streamer + url;
                }
            }
            String quality = fmt;
            if (fmt != null) {
                fmt = fmt.split("\\-")[0].toLowerCase(Locale.ENGLISH).trim();
            }

            if (fmt != null) {
                quality = quality.replaceAll("\\s", "");
                /* best selection is done at the end */
                if (new Regex(fmt, Q_LOW_INTERN).matches()) {
                    if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                        continue;
                    } else {
                        fmt = "ld";
                    }
                } else if (new Regex(fmt, Q_HIGH_INTERN).matches()) {
                    if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
                        continue;
                    } else {
                        fmt = "md";
                    }
                } else if (new Regex(fmt, Q_VERYHIGH_INTERN).matches()) {
                    if ((cfg.getBooleanProperty(Q_VERYHIGH, true) || BEST) == false) {
                        continue;
                    } else {
                        fmt = "sd";
                    }
                } else if (new Regex(fmt, Q_HD_INTERN).matches()) {
                    if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == false) {
                        continue;
                    } else {
                        fmt = "hd";
                    }
                }
            }

            final String name = title + "@" + quality + "_" + language(languageVersion) + extension;
            final DownloadLink link;
            if (parameter.contains("?")) {
                link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=" + quality);
            } else {
                link = createDownloadlink(parameter.replace("http://", "decrypted://") + "?quality=" + quality);
            }

            link.setFinalFileName(name);
            try {/* JD2 only */
                link.setContentUrl(parameter);
            } catch (Throwable e) {/* Stable */
                link.setBrowserUrl(parameter);
            }
            link.setProperty("directURL", url);
            link.setProperty("directName", name);
            link.setProperty("tvguideUrl", tvguideUrl);
            link.setProperty("VRA", convertDateFormat(VRA));
            link.setProperty("VRU", convertDateFormat(VRU));

            if ("HBBTV".equals(videoformat)) {
                link.setProperty("directQuality", getJson(info, "VQU"));
                link.setProperty("streamingType", getJson(info, "VFO"));
            } else {
                link.setAvailable(true);
                link.setProperty("directQuality", original_fmt);
                link.setProperty("streamingType", streamType);
                link.setProperty("flashplayer", "http://www.arte.tv/player/v2//jwplayer6/mediaplayer.6.3.3242.swf");
            }
            ret.add(link);
        }
        // /* parsing json */
        // HashMap<String, HashMap<String, String>> streamValues = new HashMap<String, HashMap<String, String>>();
        // HashMap<String, String> streamValue;
        // for (int i = 0; i < 2; i++) {
        // final String[][] qualityInfo = new Regex(vsr, strRegex).getMatches();
        // for (String[] ss : qualityInfo) {
        // if (hbbtvGRABBED) {
        // String tmp = ss[0];
        // ss[0] = ss[1];
        // ss[1] = tmp;
        // } else {
        // /* Check version/only download selected language, obey subtitle setting! */
        // String l = new Regex(ss[1], "\"versionCode\":\"(VO|VF|VOF\\-STMF)\"").getMatch(0);
        // if (l != null) {
        // if ("VO".equals(l)) {
        // l = "1";
        // } else if ("VF".equals(l)) {
        // l = "2";
        // } else {
        // l = "3";
        // }
        // if (i == 0) {
        // if (grabSubtitles && l.equals("3")) {
        // break;
        // } else if (Integer.parseInt(l) != languageVersion) {
        // continue;
        // }
        // } else {
        // languageVersion = Integer.parseInt(l);
        // }
        // }
        // }
        // streamValue = new HashMap<String, String>();
        // for (String[] peng : new Regex(ss[1], valRegex).getMatches()) {
        // streamValue.put(peng[0], peng[1]);
        // }
        // streamValues.put(ss[0], streamValue);
        // }
        //
        // if (streamValues.size() > 0) {
        // break;
        // }
        // }
        //
        // extension = ".mp4";
        // if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) {
        // extension = ".mp3";
        // }
        //
        //
        // for (Entry<String, HashMap<String, String>> next : streamValues.entrySet()) {
        // streamValue = new HashMap<String, String>(next.getValue());
        // String streamType = next.getKey();
        // /* only http streams for the old stable */
        // if (!streamType.matches("HTTP_REACH_EQ_\\d|SQ|EQ|HQ") && isStableEnviroment()) {
        // continue;
        // }
        // /* Do not decrypt hls streams */
        // if (streamType.contains("HLS")) {
        // continue;
        // }
        // String url = streamValue.get("url");
        // if (url == null) {
        // url = streamValue.get("VUR");
        // }
        // // this assumes that all non protocol urls belong to rtmp.
        // if (url != null && !url.matches("(https?|rtmp)://.+")) {
        // if (!url.startsWith("mp4:")) {
        // url = "mp4:" + url;
        // }
        // if (streamValue.get("streamer") != null) {
        // url = streamValue.get("streamer") + url;
        // }
        // }
        // String fmt = streamValue.get("quality");
        // if (fmt == null) {
        // fmt = hbbtv(streamValue.get("VQU"));
        // }
        // String quality = fmt;
        // if (fmt != null) {
        // fmt = fmt.split("\\-")[0].toLowerCase(Locale.ENGLISH).trim();
        // }
        // if (fmt != null) {
        // quality = quality.replaceAll("\\s", "");
        // /* best selection is done at the end */
        // if (new Regex(fmt, Q_LOW_INTERN).matches()) {
        // if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
        // continue;
        // } else {
        // fmt = "ld";
        // }
        // } else if (new Regex(fmt, Q_HIGH_INTERN).matches()) {
        // if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
        // continue;
        // } else {
        // fmt = "md";
        // }
        // } else if (new Regex(fmt, Q_VERYHIGH_INTERN).matches()) {
        // if ((cfg.getBooleanProperty(Q_VERYHIGH, true) || BEST) == false) {
        // continue;
        // } else {
        // fmt = "sd";
        // }
        // } else if (new Regex(fmt, Q_HD_INTERN).matches()) {
        // if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == false) {
        // continue;
        // } else {
        // fmt = "hd";
        // }
        // }
        // }
        // lastQualityFMT = fmt.toUpperCase(Locale.ENGLISH);
        //
        // final String name = title + "@" + quality + "_" + language(languageVersion) + extension;
        // final DownloadLink link;
        // if (parameter.contains("?")) {
        // link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=" + quality);
        // } else {
        // link = createDownloadlink(parameter.replace("http://", "decrypted://") + "?quality=" + quality);
        // }
        //
        // link.setFinalFileName(name);
        // try {/* JD2 only */
        // link.setContentUrl(parameter);
        // } catch (Throwable e) {/* Stable */
        // link.setBrowserUrl(parameter);
        // }
        // link.setProperty("directURL", url);
        // link.setProperty("directName", name);
        // link.setProperty("tvguideUrl", tvguideUrl);
        // link.setProperty("VRA", convertDateFormat(VRA));
        // link.setProperty("VRU", convertDateFormat(VRU));
        //
        // if (hbbtvGRABBED) {
        // link.setProperty("directQuality", streamValue.get("VQU"));
        // link.setProperty("streamingType", streamValue.get("VFO"));
        // } else {
        // link.setAvailable(true);
        // link.setProperty("directQuality", streamValue.get("quality"));
        // link.setProperty("streamingType", streamType);
        // link.setProperty("flashplayer", "http://www.arte.tv/player/v2//jwplayer6/mediaplayer.6.3.3242.swf");
        // }
        //
        // DownloadLink best = bestMap.get(fmt);
        // if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
        // bestMap.put(fmt, link);
        // }
        // newRet.add(link);
        // }

        if (ret.size() > 0) {
            if (BEST) {
                /* only keep best quality */
                DownloadLink keep = bestMap.get("hd");
                if (keep == null) {
                    keep = bestMap.get("sd");
                }
                if (keep == null) {
                    keep = bestMap.get("md");
                }
                if (keep == null) {
                    keep = bestMap.get("ld");
                }
                if (keep != null) {
                    newRet.clear();
                    newRet.add(keep);
                }
            } else {
                newRet = ret;
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
        for (DownloadLink dl : newRet) {
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
        }
        return ret;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private String language(int id) {
        if (id == 1) {
            return "D";
        }
        return "F";
    }

    private String hbbtv(String s) {
        if (s == null) {
            return null;
        }
        if ("SQ".equals(s)) {
            return "HD";
        }
        if ("EQ".equals(s)) {
            return "MD";
        }
        if ("HQ".equals(s)) {
            return "SD";
        }
        return "unknown";
    }

    private String convertDateFormat(String s) {
        if (s == null) {
            return null;
        }
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

    private String getTitleSITE(final Browser br) {
        String title = br.getRegex("<title>(.*?) \\| ARTE</title>").getMatch(0);
        // what is ut?
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) {
            title = br.getRegex("<h1 itemprop=\"name\" class=\"span\\d+\">([^<]+)</h1>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\| ARTE\">").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        return title;
    }

    private String getTitleAPI(final Browser br) {
        String title = br.getRegex("\"VTI\":\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        return title;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}