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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/(?!download|livestream).+" }, flags = { 32 })
public class RDMdthk extends PluginForDecrypt {

    /* Settings */
    private static final String                 Q_LOW            = "Q_LOW";
    private static final String                 Q_MEDIUM         = "Q_MEDIUM";
    private static final String                 Q_HIGH           = "Q_HIGH";
    private static final String                 Q_HD             = "Q_HD";
    private static final String                 Q_BEST           = "Q_BEST";
    private static final String                 Q_HTTP_ONLY      = "Q_HTTP_ONLY";
    private static final String                 AUDIO            = "AUDIO";
    private static final String                 Q_SUBTITLES      = "Q_SUBTITLES";
    private boolean                             BEST             = false;
    private boolean                             HTTP_ONLY        = false;
    private int                                 notForStable     = 0;

    /* Constants */
    private static final String                 AGE_RESTRICTED   = "(Diese Sendung ist für Jugendliche unter \\d+ Jahren nicht geeignet\\. Der Clip ist deshalb nur von \\d+ bis \\d+ Uhr verfügbar\\.)";
    private static final String                 UNSUPPORTEDLINKS = "http://(www\\.)?ardmediathek\\.de/(tv/live\\?kanal=\\d+|dossiers/.*)";

    /* Variables */

    private final ArrayList<DownloadLink>       newRet           = new ArrayList<DownloadLink>();
    private final HashMap<String, DownloadLink> bestMap          = new HashMap<String, DownloadLink>();
    private String                              subtitleLink     = null;
    private boolean                             grab_subtitle    = false;

    public RDMdthk(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = Encoding.htmlDecode(param.toString());
        boolean offline = false;
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            offline = true;
        }
        // Add offline link so user can see it
        if ((!br.containsHTML("data\\-ctrl\\-player=") && !br.containsHTML("id=\"box_video_player\"")) && !br.getURL().contains("/dossiers/") && !br.containsHTML(AGE_RESTRICTED) || parameter.matches(UNSUPPORTEDLINKS) || offline) {
            final DownloadLink dl = createDownloadlink("directhttp://" + parameter);
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
        final String fsk = br.getRegex(AGE_RESTRICTED).getMatch(0);
        final String realBaseUrl = new Regex(br.getBaseURL(), "(^.*\\.de)").getMatch(0);

        String ID;
        if (parameter.matches("http://(www\\.)?mediathek\\.daserste\\.de/topvideos/[a-z0-9\\-_]+")) {
            ID = new Regex(parameter, "/topvideos/(\\d+)").getMatch(0);
        } else {
            // ardmediathek.de
            ID = new Regex(parameter, "\\?documentId=(\\d+)").getMatch(0);
            // mediathek.daserste.de
            if (ID == null) {
                ID = new Regex(parameter, realBaseUrl + "/[^/]+/[^/]+/(\\d+)").getMatch(0);
            }
            if (ID == null) {
                ID = new Regex(parameter, realBaseUrl + "/suche/(\\d+)").getMatch(0);
            }
        }
        if (ID == null) {
            logger.info("ARDMediathek: MediaID is null! Regex broken?");
            return null;
        }

        /* Dossiers - maybe old code */
        String pages[] = br.getRegex("value=\"(/ard/servlet/ajax\\-cache/\\d+/view=list/documentId=" + ID + "/goto=\\d+/index.html)").getColumn(0);
        if (pages.length < 1) {
            pages = new String[1];
        }
        Collections.reverse(Arrays.asList(pages));

        for (int i = 0; i < pages.length; ++i) {
            final String[][] streams = br.getRegex("mt\\-icon_(audio|video).*?<a href=\"([^\"]+)\" class=\"mt\\-fo_source\" rel=\"[^\"]+\"[ onclick=\"[^\"]+\"]*?>([^<]+)<").getMatches();
            boolean b = false;
            for (final String[] s : streams) {
                if (!s[1].contains(ID)) {
                    continue;
                }
                b = true;
            }
            for (final String[] s : streams) {
                if ("audio".equalsIgnoreCase(s[0]) && !includeAudio) {
                    continue;
                }
                if (b && !s[1].contains(ID)) {
                    continue;
                }
                decryptedLinks.addAll(getDownloadLinks(cfg, realBaseUrl + s[1], ID, title));
                try {
                    if (this.isAbort()) {
                        return decryptedLinks;
                    }
                } catch (Throwable e) {
                    /* does not exist in 09581 */
                }
            }
            if (pages.length == 1) {
                break;
            }
            br.getPage(pages[i]);
        }
        // Single link
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            decryptedLinks.addAll(getDownloadLinks(cfg, parameter, ID, title));
        }

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
        if (rev < 10000) {
            return true;
        }
        return false;
    }

    /* INFORMATION: network = akamai or limelight == RTMP */
    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> getDownloadLinks(SubConfiguration cfg, String... s) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        grab_subtitle = cfg.getBooleanProperty(Q_SUBTITLES, false);

        try {
            final String original_ard_link = s[0];
            final String original_ard_ID = s[1];
            String title = s[2];
            if (title == null) {
                title = getTitle(br);
            }
            if (original_ard_ID != null) {
                final Browser br = new Browser();
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage(original_ard_link);
                if (br.containsHTML("(<h1>Leider konnte die gew\\&uuml;nschte Seite<br />nicht gefunden werden\\.</h1>|Die angeforderte Datei existiert leider nicht)")) {
                    return ret;
                }
                br.getPage("http://www.ardmediathek.de/play/media/" + original_ard_ID + "?devicetype=pc&features=flash");
                subtitleLink = getJson("_subtitleUrl", br.toString());
                String url = null, fmt = null;
                int t = 0;

                String extension = ".mp4";
                /* TODO: FIX */
                if (br.getRegex("new MediaCollection\\(\"audio\",").matches()) {
                    extension = ".mp3";
                }
                final String array_text = br.getRegex("\"_mediaArray\":\\[(.*?)],\"_sortierArray\"").getMatch(0);
                final String[] quality_info = array_text.split("\\},\\{");

                String lastQualityFMT = null;
                for (final String qual_info : quality_info) {
                    final String server = getJson("_server", qual_info);
                    String network = getJson("_cdn", qual_info);
                    /* Basically we change it for the filename */
                    if (network == null) {
                        network = "default_nonetwork";
                    }
                    final String quality_number = getJson("_quality", qual_info);
                    // rtmp --> hds or rtmp
                    String directlink = null;
                    /* TODO: Add all available streamlinks */
                    String directlinktext = new Regex(qual_info, "\"_stream\":\\[(.*?)\\]").getMatch(0);
                    if (directlinktext != null) {
                        directlinktext = directlinktext.replace("\"", "");
                        final String[] directlinks = directlinktext.split(",");
                        if (directlinks.length != 2) {
                            continue;
                        }
                        if ((cfg.getBooleanProperty(Q_HIGH, true) || BEST) == true) {
                            url = fixWDRdirectlink(array_text, directlinks[1]) + "@";
                            fmt = "high";
                            addQuality(fmt, network, title, ".mp4", false, url, quality_number, t, s[0]);
                        }
                        if ((cfg.getBooleanProperty(Q_HD, true) || BEST) == true) {
                            url = fixWDRdirectlink(array_text, directlinks[0]) + "@";
                            fmt = "hd";
                            addQuality(fmt, network, title, ".mp4", false, url, quality_number, t, s[0]);
                        }
                        lastQualityFMT = fmt.toUpperCase(Locale.ENGLISH);
                        continue;
                    } else {
                        directlink = getJson("_stream", qual_info);
                    }
                    url = directlink;
                    final boolean isRTMP = (server != null && !server.equals("") && server.startsWith("rtmp://")) && !url.startsWith("http");
                    /* Skip HDS */
                    if (url.endsWith("manifest.f4m")) {
                        continue;
                    }
                    /* Skip unneeded playlists */
                    if ("default".equals(network) && url.endsWith("m3u")) {
                        continue;
                    }
                    /* Server needed for rtmp links */
                    if (!url.startsWith("http://") && isEmpty(server)) {
                        continue;
                    }

                    // http or hds t=0 or t=1
                    t = Integer.valueOf(quality_number);
                    url += "@";
                    // rtmp t=?
                    if (isRTMP) {
                        url = server + "@" + directlink.split("\\?")[0];
                    } else {
                        url = fixWDRdirectlink(array_text, url);
                    }
                    // only http streams for old stable
                    if (isRTMP && isStableEnviroment()) {
                        notForStable++;
                        continue;
                    }
                    /* Skip rtmp streams if user wants http only */
                    if (isRTMP && HTTP_ONLY) {
                        continue;
                    }

                    fmt = "hd";

                    switch (Integer.valueOf(quality_number)) {
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
                    addQuality(fmt, network, title, extension, isRTMP, url, quality_number, t, s[0]);
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
                            if (grab_subtitle && subtitleLink != null && !isEmpty(subtitleLink)) {
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

    private void addQuality(final String fmt, final String network, final String title, final String extension, final boolean isRTMP, final String url, final String quality_number, final int t, final String orig_link) {
        final String quality_part = fmt.toUpperCase(Locale.ENGLISH) + "-" + network;
        final String plain_name = title + "@" + quality_part;
        final String full_name = plain_name + extension;

        final DownloadLink link = createDownloadlink(orig_link.replace("http://", "decrypted://") + "&quality=" + fmt + "&network=" + network);
        /* RTMP links have no filesize anyways --> No need to check them in host plugin */
        if (isRTMP) {
            link.setAvailable(true);
        }
        link.setFinalFileName(full_name);
        try {/* JD2 only */
            link.setContentUrl(orig_link);
        } catch (Throwable e) {/* Stable */
            link.setBrowserUrl(orig_link);
        }
        link.setProperty("directURL", url);
        link.setProperty("directName", full_name);
        link.setProperty("plain_name", plain_name);
        link.setProperty("plain_quality_part", quality_part);
        link.setProperty("plain_name", plain_name);
        link.setProperty("plain_network", network);
        link.setProperty("directQuality", quality_number);
        link.setProperty("streamingType", t);

        /* Add subtitle link for every quality so players will automatically find it */
        if (grab_subtitle && subtitleLink != null && !isEmpty(subtitleLink)) {
            final String subtitle_filename = plain_name + ".xml";
            final String finallink = "http://www.ardmediathek.de" + subtitleLink + "@" + quality_part;
            final DownloadLink dl_subtitle = createDownloadlink(orig_link.replace("http://", "decrypted://") + "&quality=subtitles" + fmt + "&network=" + network);
            try {/* JD2 only */
                dl_subtitle.setContentUrl(orig_link);
            } catch (Throwable e) {/* Stable */
                dl_subtitle.setBrowserUrl(orig_link);
            }
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

    /*
     * Special workaround for HDS only streams.
     */
    private String fixWDRdirectlink(final String mainsource, String wdrlink) {
        final Regex wdrwtf = new Regex(wdrlink, "http://ondemand-de.wdr.de/medstdp/(fsk\\d+)/(\\d+)/(\\d+)/([0-9_]+)\\.mp4");
        String region = new Regex(mainsource, "adaptiv\\.wdr\\.de/[a-z0-9]+/medstdp/([a-z]{2})/").getMatch(0);
        final String fsk = wdrwtf.getMatch(0);
        final String number = wdrwtf.getMatch(1);
        final String main_id = wdrwtf.getMatch(2);
        final String quality_id = wdrwtf.getMatch(3);
        if (region != null && fsk != null && number != null && main_id != null && quality_id != null) {
            region = jd.plugins.decrypter.WdrDeDecrypt.correctRegionString(region);
            wdrlink = "http://http-ras.wdr.de/CMS2010/mdb/ondemand/" + region + "/" + fsk + "/" + number + "/" + main_id + "/" + quality_id + ".mp4";
        }
        return wdrlink;
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<(div|span) class=\"(MainBoxHeadline|BoxHeadline)\">([^<]+)</").getMatch(2);
        String titleUT = br.getRegex("<span class=\"(BoxHeadlineUT|boxSubHeadline)\">([^<]+)</").getMatch(1);
        if (titleUT == null) {
            titleUT = br.getRegex("<h3 class=\"mt\\-title\"><a>([^<>\"]*?)</a></h3>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("class=\"mt\\-icon mt\\-icon_video\"></span><img src=\"[^\"]+\" alt=\"([^\"]+)\"").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("class=\"mt\\-icon mt\\-icon\\-toggle_arrows\"></span>([^<>\"]*?)</a>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        title = title.replaceAll("\\n|\\t|,", "").trim();
        return title;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}