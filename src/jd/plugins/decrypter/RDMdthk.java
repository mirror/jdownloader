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
import java.util.Arrays;
import java.util.Collections;
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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/[\\w\\-]+/([\\w\\-]+/)?[\\w\\-]+(\\?documentId=\\d+)?" }, flags = { 32 })
public class RDMdthk extends PluginForDecrypt {

    private static final String Q_LOW        = "Q_LOW";
    private static final String Q_MEDIUM     = "Q_MEDIUM";
    private static final String Q_HIGH       = "Q_HIGH";
    private static final String Q_HD         = "Q_HD";
    private static final String Q_BEST       = "Q_BEST";
    private static final String AUDIO        = "AUDIO";
    private boolean             BEST         = false;
    private int                 notForStable = 0;

    public RDMdthk(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(<h1>Leider konnte die gew&uuml;nschte Seite<br />nicht gefunden werden.</h1>|Die angeforderte Datei existiert leider nicht)")) {
            logger.info("This link might be offline: " + parameter);
            return decryptedLinks;
        }

        SubConfiguration cfg = SubConfiguration.getConfig("ard.de");
        boolean includeAudio = cfg.getBooleanProperty(AUDIO, true);
        BEST = cfg.getBooleanProperty(Q_BEST, false);

        final String fsk = br.getRegex("(Diese Sendung ist für Jugendliche unter \\d+ Jahren nicht geeignet\\. Der Clip ist deshalb nur von \\d+ bis \\d+ Uhr verfügbar\\.)").getMatch(0);
        final String realBaseUrl = new Regex(br.getBaseURL(), "(^.*\\.de)").getMatch(0);

        // ardmediathek.de
        String ID = new Regex(parameter, "\\?documentId=(\\d+)").getMatch(0);
        // mediathek.daserste.de
        if (ID == null) ID = new Regex(parameter, realBaseUrl + "/[^/]+/[^/]+/(\\d+)").getMatch(0);
        if (ID == null) ID = new Regex(parameter, realBaseUrl + "/suche/(\\d+)").getMatch(0);
        if (ID == null) {
            logger.info("ARDMediathek: MediaID is null! Regex broken?");
            return null;
        }

        String next = br.getRegex("href=\"(/ard/servlet/ajax\\-cache/\\d+/view=switch/documentId=" + ID + "/index.html)").getMatch(0);
        if (next != null) br.getPage(next);
        // Dossiers
        String pages[] = br.getRegex("value=\"(/ard/servlet/ajax\\-cache/\\d+/view=list/documentId=" + ID + "/goto=\\d+/index.html)").getColumn(0);
        if (pages.length < 1) pages = new String[1];
        Collections.reverse(Arrays.asList(pages));

        for (int i = 0; i < pages.length; ++i) {
            final String[][] streams = br.getRegex("mt\\-icon_(audio|video).*?<a href=\"([^\"]+)\" class=\"mt\\-fo_source\" rel=\"[^\"]+\"[ onclick=\"[^\"]+\"]*?>([^<]+)<").getMatches();
            boolean b = false;
            for (final String[] s : streams) {
                if (!s[1].contains(ID)) continue;
                b = true;
            }
            progress.setRange(streams.length);
            for (final String[] s : streams) {
                progress.increase(1);
                if ("audio".equalsIgnoreCase(s[0]) && !includeAudio) continue;
                if (b && !s[1].contains(ID)) continue;
                decryptedLinks.addAll(getDownloadLinks(cfg, realBaseUrl + s[1], ID));
                try {
                    if (this.isAbort()) return decryptedLinks;
                } catch (Throwable e) {
                    /* does not exist in 09581 */
                }
            }
            progress.setRange(0);
            if (pages.length == 1) break;
            br.getPage(pages[i]);
        }
        // Single link
        if (decryptedLinks == null || decryptedLinks.size() == 0) decryptedLinks.addAll(getDownloadLinks(cfg, parameter, ID));

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            if (notForStable > 0) {
                logger.info("ARD-Mediathek: Only flash content is available. Not downloadable with JD1, please use JD2!");
                return decryptedLinks;
            }
            if (fsk != null) {
                logger.info("ARD-Mediathek: " + fsk);
                return decryptedLinks;
            }
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

    private ArrayList<DownloadLink> getDownloadLinks(SubConfiguration cfg, String... s) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        try {
            if (s[1] != null) {
                Browser br = new Browser();
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage(s[0]);
                if (br.containsHTML("(<h1>Leider konnte die gew&uuml;nschte Seite<br />nicht gefunden werden.</h1>|Die angeforderte Datei existiert leider nicht)")) return ret;
                String title = getTitle(br);
                String url = null, fmt = null;
                int t = 0;

                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                for (String quality[] : br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (\\d+), \"([^\"]+|)\", \"([^\"]+)\", \"([^\"]+)\"\\);").getMatches()) {
                    // get streamtype id
                    t = Integer.valueOf(quality[0]);
                    // http t=1
                    url = quality[3] + "@";
                    // rtmp t=0
                    if (t == 0) url = quality[2] + "@" + quality[3].split("\\?")[0];
                    if (t == 1 && url.endsWith("m3u8@") || t > 1) continue;
                    fmt = "hd";

                    // only http streams for old stable
                    if (url.startsWith("rtmp") && isStableEnviroment()) {
                        notForStable++;
                        continue;
                    }

                    switch (Integer.valueOf(quality[1])) {
                    case 0:
                        if ((cfg.getBooleanProperty(Q_LOW, true) || BEST) == false) {
                            continue;
                        } else {
                            fmt = "low";
                        }
                        break;
                    case 1:
                        if ((cfg.getBooleanProperty(Q_MEDIUM, true) || BEST) == false) {
                            continue;
                        } else {
                            fmt = "medium";
                        }
                        break;
                    case 2:
                        if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == false) {
                            continue;
                        } else {
                            fmt = "high";
                        }
                        break;
                    case 3:
                        if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == false) {
                            continue;
                        } else {
                            fmt = "hd";
                        }
                        break;
                    }

                    final String name = title + "@" + fmt.toUpperCase(Locale.ENGLISH) + extension;
                    final DownloadLink link = createDownloadlink(s[0].replace("http://", "decrypted://") + "&quality=" + fmt);
                    if (t == 1 ? false : true) link.setAvailable(true);
                    link.setFinalFileName(name);
                    link.setBrowserUrl(s[0]);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", name);
                    link.setProperty("directQuality", quality[1]);
                    link.setProperty("streamingType", t);

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
                        if (keep == null) keep = bestMap.get("high");
                        if (keep == null) keep = bestMap.get("medium");
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
        String title = br.getRegex("<(div|span) class=\"(MainBoxHeadline|BoxHeadline)\">([^<]+)</").getMatch(2);
        String titleUT = br.getRegex("<span class=\"(BoxHeadlineUT|boxSubHeadline)\">([^<]+)</").getMatch(1);
        if (title == null) title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        if (title == null) title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

}