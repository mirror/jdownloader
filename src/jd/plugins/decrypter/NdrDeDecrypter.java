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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ndr.de" }, urls = { "https?://(?:www\\.)?ndr\\.de/.*?\\.html" })
public class NdrDeDecrypter extends PluginForDecrypt {
    public NdrDeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String domain    = "ndr.de";
    private final String[]      qualities = { "hd", "hq", "hi", "lo" };

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        LinkedHashMap<String, DownloadLink> foundqualities = new LinkedHashMap<String, DownloadLink>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://");
        final String url_id = new Regex(parameter, "([a-z0-9]+)\\.html$").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(domain);
        final boolean qsubtitles = cfg.getBooleanProperty("Q_SUBTITLES", true);
        final boolean fastlinkcheck = cfg.getBooleanProperty(jd.plugins.hoster.NdrDe.FAST_LINKCHECK, jd.plugins.hoster.NdrDe.defaultFAST_LINKCHECK);
        br.setFollowRedirects(true);
        this.br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_player_html = this.br.getRegex("\"(/fernsehen/[^<>\"]*?\\-player_[^<>\"]*?\\.html)\"").getMatch(0);
        final String url_iframe = br.getRegex("src=\"(/[^<>\"]+)_theme\\-[A-Za-z0-9\\-_]+\\.html\"\\s*?></iframe>").getMatch(0);
        final String url_json;
        if (url_player_html != null) {
            url_json = url_player_html.replace("-player_", "-ppjson_").replace(".html", ".json");
        } else if (url_iframe != null) {
            /* 2017-12-07: New */
            url_json = "http://www." + this.getHost() + url_iframe.replace("-ardplayer", "-ardjson") + ".json";
        } else {
            /* Fallback - this might sometimes work too */
            url_json = "http://www." + this.getHost() + "/epg/" + url_id + ".json";
        }
        // http://www.ndr.de/fernsehen/sendungen/die_nordstory/dienordstory264-ardjson_image-19fb5bd4-d7d6-4c2f-8af1-a2a201daf8a7.json
        /* API - well, basically returns same html code as if we simply access the normal link... */
        br.getPage(url_json);
        /* Offline | livestream | no stream */
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("cid:[\t\n\r ]*?\"livestream")) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String date = this.br.getRegex("itemprop=\"(?:startDate|datePublished)\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (date == null) {
            date = PluginJSonUtils.getJsonValue(br, "publicationDate");
        }
        String title = br.getRegex("name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = PluginJSonUtils.getJsonValue(br, "trackTitle");
        }
        if (title == null || date == null) {
            logger.info("Failed to find any downloadable content");
            return decryptedLinks;
        }
        final String date_formatted = formatDate(date);
        final String availablequalitiestext = br.getRegex("\\.,([a-z,]+),\\.mp4\\.csmil/master\\.m3u8").getMatch(0);
        final Regex afnreg = br.getRegex("afn: \"TV-(\\d{4})(\\d{4})([0-9\\-]+)\"");
        // http://hls.ndr.de/i/ndr/2015/0806/TV-20150806-1349-2442.,lo,hi,hq,hd,.mp4.csmil/master.m3u8
        final Regex afneg_replacement = br.getRegex("hls\\.ndr\\.de/i/ndr/(\\d+)/(\\d+)/TV\\-\\d+([^<>\"(]*?)\\.,");
        String v_year = afnreg.getMatch(0);
        if (v_year == null) {
            v_year = afneg_replacement.getMatch(0);
        }
        String v_id = afnreg.getMatch(1);
        if (v_id == null) {
            v_id = afneg_replacement.getMatch(1);
        }
        String v_rest = afnreg.getMatch(2);
        if (v_rest == null) {
            v_rest = afneg_replacement.getMatch(2);
        }
        if (availablequalitiestext == null || v_id == null || v_year == null || v_id == null || v_rest == null) {
            logger.info("Found no downloadable content: " + parameter);
            return decryptedLinks;
        }
        String subtitle_url = PluginJSonUtils.getJson(this.br, "_subtitleUrl");
        if (subtitle_url != null) {
            subtitle_url = "https://www.ndr.de" + subtitle_url.replace(".html", ".xml");
        }
        final String linkdupeid = v_rest + "_%S_%S";
        final String[] availablequalities = availablequalitiestext.split(",");
        for (final String quality : availablequalities) {
            final String finalfilename = date_formatted + "_ndr_" + title + "_" + getNiceQuality(quality) + ".mp4";
            final String directlink = "http://media.ndr.de/progressive/" + v_year + "/" + v_id + "/TV-" + v_year + v_id + v_rest + "." + quality + ".mp4";
            final DownloadLink dl = createDownloadlink("http://ndrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            dl.setContentUrl(parameter);
            dl.setProperty("directlink", directlink);
            dl.setProperty("quality", quality);
            dl.setProperty("streamingType", "video");
            dl.setProperty("decryptedfilename", finalfilename);
            dl.setProperty("mainlink", parameter);
            dl.setLinkID(String.format(linkdupeid, "video", quality));
            dl.setFinalFileName(finalfilename);
            if (fastlinkcheck) {
                dl.setAvailable(true);
            }
            foundqualities.put(quality, dl);
        }
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        if (cfg.getBooleanProperty("Q_BEST", false)) {
            for (final String quality : qualities) {
                if (foundqualities.get(quality) != null) {
                    selectedQualities.add(quality);
                    break;
                }
            }
        } else {
            /** User selected nothing -> Decrypt everything */
            boolean qhd = cfg.getBooleanProperty("Q_HD", true);
            boolean qveryhigh = cfg.getBooleanProperty("Q_VERYHIGH", true);
            boolean qhigh = cfg.getBooleanProperty("Q_HIGH", true);
            boolean qlow = cfg.getBooleanProperty("Q_LOW", true);
            if (qveryhigh == false && qhigh == false && qlow == false) {
                qveryhigh = true;
                qhigh = true;
                qlow = true;
            }
            if (qhd) {
                selectedQualities.add("hd");
            }
            if (qveryhigh) {
                selectedQualities.add("hq");
            }
            if (qhigh) {
                selectedQualities.add("hi");
            }
            if (qlow) {
                selectedQualities.add("lo");
            }
        }
        /* Add chosen downloadlinks, add subtitle if desired. */
        for (final String selectedQualityValue : selectedQualities) {
            final DownloadLink dl = foundqualities.get(selectedQualityValue);
            if (dl != null) {
                decryptedLinks.add(dl);
                if (subtitle_url != null && qsubtitles) {
                    /* Add subtitle */
                    final String quality = dl.getStringProperty("quality", null);
                    final String finalfilename = date_formatted + "_ndr_" + title + "_" + getNiceQuality(quality) + ".xml";
                    final DownloadLink dlsubtitle = createDownloadlink("http://ndrdecrypted.de/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    dlsubtitle.setContentUrl(parameter);
                    dlsubtitle.setProperty("directlink", subtitle_url);
                    dlsubtitle.setProperty("quality", quality);
                    dlsubtitle.setProperty("streamingType", "subtitle");
                    dlsubtitle.setProperty("decryptedfilename", finalfilename);
                    dlsubtitle.setProperty("mainlink", parameter);
                    dl.setLinkID(String.format(linkdupeid, "subtitle", quality));
                    dlsubtitle.setFinalFileName(finalfilename);
                    dlsubtitle.setAvailable(true);
                    decryptedLinks.add(dlsubtitle);
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.info(domain + ": None of the selected qualities were found, decrypting done...");
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(date_formatted + "_ndr_" + Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getNiceQuality(final String qual) {
        String nicequal;
        if (qual.equalsIgnoreCase("hd")) {
            nicequal = "HD";
        } else if (qual.equalsIgnoreCase("hq")) {
            nicequal = "VERYHIGH";
        } else if (qual.equalsIgnoreCase("hi")) {
            nicequal = "HIGH";
        } else {
            nicequal = "LOW";
        }
        return nicequal;
    }

    private String formatDate(String input) {
        /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
        if (input == null || input.equals("")) {
            return "-";
        }
        final long date;
        if (input.matches("\\d+T\\d{2}\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "yyyyMMdd'T'HHmm", Locale.GERMAN);
        } else {
            if (input.contains(":")) {
                input = input.substring(0, input.lastIndexOf(":")) + "00";
            }
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.GERMAN);
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
}
