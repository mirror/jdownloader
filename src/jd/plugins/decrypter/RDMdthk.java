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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/(?!download|livestream)[\\w\\-]+/([\\w\\-]+/)?[\\w\\-]+(\\?documentId=\\d+)?" }, flags = { 32 })
public class RDMdthk extends PluginForDecrypt {

    private static final String Q_LOW        = "Q_LOW";
    private static final String Q_MEDIUM     = "Q_MEDIUM";
    private static final String Q_HIGH       = "Q_HIGH";
    private static final String Q_HD         = "Q_HD";
    private static final String Q_BEST       = "Q_BEST";
    private static final String Q_HTTP_ONLY  = "Q_HTTP_ONLY";
    private static final String AUDIO        = "AUDIO";
    private static final String Q_SUBTITLES  = "Q_SUBTITLES";
    private boolean             BEST         = false;
    private boolean             HTTP_ONLY    = false;
    private int                 notForStable = 0;

    public RDMdthk(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        boolean offline = false;
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            offline = true;
        }
        // Add offline link so user can see it
        if (!br.containsHTML("var \\$jPlayer =") && !parameter.contains("/dossiers/") || offline) {
            final DownloadLink dl = createDownloadlink(parameter.replace("http://", "decrypted://") + "&quality=offline&network=default");
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        final SubConfiguration cfg = SubConfiguration.getConfig("ard.de");
        boolean includeAudio = cfg.getBooleanProperty(AUDIO, true);
        BEST = cfg.getBooleanProperty(Q_BEST, false);
        HTTP_ONLY = cfg.getBooleanProperty(Q_HTTP_ONLY, false);

        final String title = br.getRegex("<meta name=\"dcterms\\.title\" content=\"([^\"]+)\"").getMatch(0);
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
                decryptedLinks.addAll(getDownloadLinks(cfg, realBaseUrl + s[1], ID, title));
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
        if (decryptedLinks == null || decryptedLinks.size() == 0) decryptedLinks.addAll(getDownloadLinks(cfg, parameter, ID, title));

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
        final boolean grab_subtitle = cfg.getBooleanProperty(Q_SUBTITLES, false);

        try {
            String title = s[2];
            if (title == null) title = getTitle(br);
            if (s[1] != null) {
                Browser br = new Browser();
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage(s[0]);
                if (br.containsHTML("(<h1>Leider konnte die gew\\&uuml;nschte Seite<br />nicht gefunden werden\\.</h1>|Die angeforderte Datei existiert leider nicht)")) return ret;
                final String subtitleLink = br.getRegex("mediaCollection\\.setSubtitleUrl\\(\"(/static/avportal/untertitel_mediathek/\\d+\\.xml)\"").getMatch(0);
                String url = null, fmt = null;
                int t = 0;

                String extension = ".mp4";
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) extension = ".mp3";

                ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                final HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                String lastQualityFMT = null;
                for (String quality[] : br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (\\d+), \"([^\"]+|)\", \"([^\"]+)\", \"([^\"]+)\"\\);").getMatches()) {
                    // rtmp --> hds or rtmp
                    url = quality[3];
                    if ("akamai".equals(quality[4]) || "limelight".equals(quality[4])) {
                        if (url.endsWith("manifest.f4m")) continue;
                    }
                    if ("default".equals(quality[4])) {
                        if (url.endsWith("m3u")) continue;
                    }
                    if (!url.startsWith("http://")) {
                        if (isEmpty(quality[2])) continue;
                    }
                    // get streamtype id
                    t = Integer.valueOf(quality[0]);
                    // http or hds t=0 or t=1
                    url += "@";
                    // rtmp t=?
                    if (quality[2].startsWith("rtmp")) url = quality[2] + "@" + quality[3].split("\\?")[0];
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

                    lastQualityFMT = fmt.toUpperCase(Locale.ENGLISH);
                    final String quality_part = fmt.toUpperCase(Locale.ENGLISH) + "-" + quality[4];
                    final String plain_name = title + "@" + quality_part;
                    final String full_name = plain_name + extension;
                    final String network = quality[4];

                    /* Skip rtmp streams if user wants http only */
                    if (network.equals("akamai") && HTTP_ONLY) continue;

                    final DownloadLink link = createDownloadlink(s[0].replace("http://", "decrypted://") + "&quality=" + fmt + "&network=" + network);
                    if (t == 1 ? false : true) link.setAvailable(true);
                    link.setFinalFileName(full_name);
                    link.setBrowserUrl(s[0]);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", full_name);
                    link.setProperty("plain_name", plain_name);
                    link.setProperty("plain_quality_part", quality_part);
                    link.setProperty("plain_name", plain_name);
                    link.setProperty("plain_network", network);
                    link.setProperty("directQuality", quality[1]);
                    link.setProperty("streamingType", t);

                    /* Add subtitle link for every quality so players will automatically find it */
                    if (grab_subtitle && subtitleLink != null) {
                        final String subtitle_filename = plain_name + ".xml";
                        final String finallink = "http://www.ardmediathek.de" + subtitleLink + "@" + quality_part;
                        final DownloadLink dl_subtitle = createDownloadlink(s[0].replace("http://", "decrypted://") + "&quality=subtitles" + fmt + "&network=" + network);
                        dl_subtitle.setAvailable(true);
                        dl_subtitle.setFinalFileName(subtitle_filename);
                        dl_subtitle.setProperty("directURL", finallink);
                        dl_subtitle.setProperty("directName", subtitle_filename);
                        dl_subtitle.setProperty("streamingType", "subtitle");
                        newRet.add(dl_subtitle);
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
                            lastQualityFMT = "HIGH";
                            keep = bestMap.get("high");
                        }
                        if (keep == null) {
                            lastQualityFMT = "MEDIUM";
                            keep = bestMap.get("medium");
                        }
                        if (keep == null) {
                            lastQualityFMT = "LOW";
                            keep = bestMap.get("low");
                        }
                        if (keep != null) {
                            newRet.clear();
                            newRet.add(keep);

                            /* We have to re-add the subtitle for the best quality if wished by the user */
                            if (grab_subtitle && subtitleLink != null) {
                                final String plain_name = keep.getStringProperty("plain_name", null);
                                final String plain_quality_part = keep.getStringProperty("plain_quality_part", null);
                                final String plain_network = keep.getStringProperty("plain_network", null);
                                final String subtitle_filename = plain_name + ".xml";
                                final String finallink = "http://www.ardmediathek.de" + subtitleLink + "@" + plain_quality_part;
                                final DownloadLink dl_subtitle = createDownloadlink(s[0].replace("http://", "decrypted://") + "&quality=subtitles" + lastQualityFMT + "&network=" + plain_network);
                                dl_subtitle.setAvailable(true);
                                dl_subtitle.setFinalFileName(subtitle_filename);
                                dl_subtitle.setProperty("directURL", finallink);
                                dl_subtitle.setProperty("directName", subtitle_filename);
                                dl_subtitle.setProperty("streamingType", "subtitle");
                                newRet.add(dl_subtitle);
                            }

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
        for (final DownloadLink dl : ret) {
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
        if (titleUT == null) titleUT = br.getRegex("<h3 class=\"mt\\-title\"><a>([^<>\"]*?)</a></h3>").getMatch(0);
        if (title == null) title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        if (title == null) title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (title == null) title = br.getRegex("class=\"mt\\-icon mt\\-icon_video\"></span><img src=\"[^\"]+\" alt=\"([^\"]+)\"").getMatch(0);
        if (title == null) title = br.getRegex("class=\"mt\\-icon mt\\-icon\\-toggle_arrows\"></span>([^<>\"]*?)</a>").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        title = title.replaceAll("\\n|\\t|,", "").trim();
        return title;
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}