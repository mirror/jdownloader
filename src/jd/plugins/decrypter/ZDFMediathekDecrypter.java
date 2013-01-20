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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zdfmediathek.de" }, urls = { "http://(www\\.)?zdf\\.de/ZDFmediathek#?/[^<>\"]*?beitrag/video/\\d+(/[^<>\"/\\?#]{1})?" }, flags = { 0 })
public class ZDFMediathekDecrypter extends PluginForDecrypt {

    private static final String Q_LOW      = "Q_LOW";
    private static final String Q_HIGH     = "Q_HIGH";
    private static final String Q_VERYHIGH = "Q_VERYHIGH";
    private static final String Q_HD       = "Q_HD";
    private static final String Q_BEST     = "Q_BEST";
    private boolean             BEST       = false;

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden")) {
            logger.info("This link might be offline: " + parameter);
            return decryptedLinks;
        }

        SubConfiguration cfg = SubConfiguration.getConfig("zdf.de");
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        decryptedLinks.addAll(getDownloadLinks(parameter, cfg));

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> getDownloadLinks(String data, SubConfiguration cfg) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        try {
            String ID = new Regex(data, "/beitrag/video/(\\d+)").getMatch(0);
            if (ID != null) {
                br.getPage("/ZDFmediathek/xmlservice/web/beitragsDetails?id=" + ID + "&ak=web");
                if (br.containsHTML("<statuscode>wrongParameter</statuscode>")) return ret;

                String title = getTitle(br);
                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                for (String streams[] : br.getRegex("<formitaet basetype=\"([^\"]+)\" isDownload=\"[^\"]+\">(.*?)</formitaet>").getMatches()) {
                    if (!streams[0].contains("mp4_http")) continue;
                    for (String stream[] : new Regex(streams[1], "<quality>([^<]+)</quality>.*?<url>([^<]+)<.*?<filesize>(\\d+)<.*?<facet>(progressive|restriction_useragent)</").getMatches()) {
                        String fmt = stream[0];
                        if (fmt != null) fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
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
                                    fmt = "hd";
                                }
                            }
                        }

                        final String name = title + "@" + fmt.toUpperCase(Locale.ENGLISH) + extension;
                        final DownloadLink link = createDownloadlink(data.replace("http://", "decrypted://"));
                        link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setBrowserUrl(data);
                        link.setProperty("directURL", stream[1]);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", stream[0]);
                        link.setProperty("streamingType", "http");
                        link.setProperty("LINKDUPEID", "zdf" + ID + name + fmt);

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
                    if (newRet.size() > 1) {
                        FilePackage fp = FilePackage.getInstance();
                        fp.setName(title);
                        fp.addLinks(newRet);
                    }
                    ret = newRet;
                }
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

}