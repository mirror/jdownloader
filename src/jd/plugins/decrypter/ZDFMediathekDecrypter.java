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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zdfmediathek.de" }, urls = { "http://(www\\.)?zdf\\.de/ZDFmediathek#?/[^<>\"]*?beitrag/video/\\d+" }, flags = { 0 })
public class ZDFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_SUBTITLES = "Q_SUBTITLES";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_HIGH      = "Q_HIGH";
    private static final String Q_VERYHIGH  = "Q_VERYHIGH";
    private static final String Q_HD        = "Q_HD";
    private boolean             BEST        = false;

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("ZDFmediathek#/", "ZDFmediathek/");

        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Check...if offline, add to llinkgrabber so user can see it
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=high");
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }

        final SubConfiguration cfg = SubConfiguration.getConfig("zdf.de");
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
        if (rev < 10000) { return true; }
        return false;
    }

    private ArrayList<DownloadLink> getDownloadLinks(final String data, final SubConfiguration cfg) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        try {
            String ID = new Regex(data, "beitrag/video/(\\d+)").getMatch(0);
            if (ID != null) {
                br.getPage("/ZDFmediathek/xmlservice/web/beitragsDetails?id=" + ID + "&ak=web");
                if (br.containsHTML("<statuscode>wrongParameter</statuscode>")) return ret;

                String title = getTitle(br);
                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";
                final Browser br2 = br.cloneBrowser();

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                for (String streams[] : br2.getRegex("<formitaet basetype=\"([^\"]+)\" isDownload=\"[^\"]+\">(.*?)</formitaet>").getMatches()) {

                    if (!(streams[0].contains("mp4_http") || streams[0].contains("mp4_rtmp_zdfmeta"))) continue;

                    for (String stream[] : new Regex(streams[1], "<quality>([^<]+)</quality>.*?<url>([^<]+)<.*?<filesize>(\\d+)<").getMatches()) {

                        if (streams[0].contains("mp4_http") && !new Regex(streams[1], ("<facet>(progressive|restriction_useragent|podcast)</")).matches()) continue;
                        /* only http stream for the old stable */
                        if (streams[0].contains("mp4_rtmp_zdfmeta") && isStableEnviroment()) continue;
                        if (stream[1].endsWith(".meta") && stream[1].contains("streaming") && stream[1].startsWith("http")) {
                            br2.getPage(stream[1]);
                            stream[1] = br2.getRegex("<default\\-stream\\-url>(.*?)</default\\-stream\\-url>").getMatch(0);
                            if (stream[1] == null) continue;
                        }

                        String url = stream[1];
                        String fmt = stream[0];
                        if (fmt != null) fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
                        if (fmt != null) {
                            /* best selection is done at the end */
                            if ("low".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_LOW, false) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "low";
                                }
                            } else if ("high".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_HIGH, false) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "high";
                                }
                            } else if ("veryhigh".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_VERYHIGH, false) || BEST) == false) {
                                    continue;
                                } else {
                                    fmt = "veryhigh";
                                }
                            } else if ("hd".equals(fmt)) {
                                if ((cfg.getBooleanProperty(Q_HD, false) || BEST) == false) {
                                    continue;
                                } else {
                                    if (streams[0].contains("mp4_rtmp")) {
                                        if (isStableEnviroment()) continue;
                                        if (url.startsWith("http://")) {
                                            Browser rtmp = new Browser();
                                            rtmp.getPage(stream[1]);
                                            url = rtmp.getRegex("<default\\-stream\\-url>([^<]+)<").getMatch(0);
                                        }
                                        if (url == null) continue;
                                    }
                                    fmt = "hd";
                                }
                            }
                        }

                        final String name = title + "@" + fmt.toUpperCase(Locale.ENGLISH) + extension;
                        final DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://") + "&quality=" + fmt);
                        link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setBrowserUrl(data);
                        link.setProperty("directURL", url);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", stream[0]);
                        link.setProperty("streamingType", "http");

                        try {
                            link.setDownloadSize(SizeFormatter.getSize(stream[2]));
                        } catch (Throwable e) {
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
                        if (keep == null) keep = bestMap.get("veryhigh");
                        if (keep == null) keep = bestMap.get("high");
                        if (keep == null) keep = bestMap.get("low");
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
                        final String name = title + "@" + "SUBTITLE.xml";
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

    private String getTitle(Browser br) {
        String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title == null) title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}