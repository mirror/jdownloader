//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BrDe.BrDeConfigInterface;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "br.de" }, urls = { "http://(www\\.)?br\\.de/mediathek/video/[^<>\"]+\\.html" })
public class BrDeDecrypter extends PluginForDecrypt {
    private static final String   TYPE_INVALID           = "http://(www\\.)?br\\.de/mediathek/video/index\\.html";
    /* only keep best quality , do not change the ORDER */
    private static final String[] all_possible_qualities = { "X", "C", "E", "B", "A", "0" };

    public BrDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        boolean offline = false;
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        if (parameter.contains("/livestream/")) {
            /* Invalid URLs. */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (offline || this.br.getHttpConnection().getResponseCode() == 404 || parameter.matches(TYPE_INVALID)) {
            /* Add offline link so user can see it */
            final DownloadLink dl = this.createOfflinelink(parameter);
            String offline_name = new Regex(parameter, "br\\.de/(.+)\\.html$").getMatch(0);
            if (offline_name != null) {
                dl.setFinalFileName(offline_name);
            }
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> best_map = new HashMap<String, DownloadLink>();
        HashMap<String, DownloadLink> tmpBestMap = new HashMap<String, DownloadLink>();
        final BrDeConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.BrDe.BrDeConfigInterface.class);
        final boolean grab_subtitle = cfg.isGrabSubtitleEnabled();
        final boolean grabBEST = cfg.isGrabBESTEnabled();
        final String player_link = br.getRegex("\\{dataURL:\\'(/mediathek/video/[^<>\"]*?)\\'\\}").getMatch(0);
        String date = br.getRegex(">(\\d{2}\\.\\d{2}\\.\\d{4}), \\d{2}:\\d{2} Uhr,?</time>").getMatch(0);
        if (player_link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String playerLinkID = JDHash.getMD5(player_link);
        br.getPage("http://www.br.de" + player_link);
        if (date == null) {
            date = getXML("broadcastDate");
        }
        String show = getXML("broadcast");
        String plain_name = this.getXML("shareTitle");
        final String[] qualities = br.getRegex("<asset type=(.*?)</asset>").getColumn(0);
        if (qualities == null || qualities.length == 0 || plain_name == null || date == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String date_formatted = formatDate(date);
        if (show == null) {
            /* Show is not always given */
            show = "-";
        }
        plain_name = encodeUnicode(Encoding.htmlDecode(plain_name).trim()).replace("\n", "");
        show = encodeUnicode(Encoding.htmlDecode(show).trim());
        String subtitle_url = br.getRegex("<dataTimedText url=\"(/mediathek/video/untertitel[^<>\"/]+\\.xml)\"").getMatch(0);
        if (subtitle_url != null) {
            subtitle_url = "http://www.br.de" + Encoding.htmlDecode(subtitle_url);
        }
        for (final String qinfo : qualities) {
            final String final_url = this.getXML(qinfo, "downloadUrl");
            /* Avoid HDS */
            if (final_url == null) {
                continue;
            }
            final String q_string = new Regex(final_url, "_(0|A|B|C|D|E|X)\\.mp4").getMatch(0);
            final String width = this.getXML(qinfo, "frameWidth");
            final String height = this.getXML(qinfo, "frameHeight");
            final String fsize = this.getXML(qinfo, "size");
            final String resolution = width + "x" + height;
            final String final_video_name = date_formatted + "_br_" + show + " - " + plain_name + "_" + resolution + ".mp4";
            final DownloadLink dl_video = createDownloadlink("http://brdecrypted-online.de/?format=mp4&quality=" + resolution + "&hash=" + playerLinkID);
            dl_video.setLinkID(getHost() + "://" + playerLinkID + "/" + q_string + "/" + resolution);
            dl_video.setProperty("mainlink", parameter);
            dl_video.setProperty("direct_link", final_url);
            dl_video.setProperty("plain_filename", final_video_name);
            dl_video.setProperty("plain_resolution", resolution);
            dl_video.setContentUrl(parameter);
            dl_video.setFinalFileName(final_video_name);
            dl_video.setDownloadSize(Long.parseLong(fsize));
            dl_video.setAvailable(true);
            best_map.put(q_string, dl_video);
            newRet.add(dl_video);
        }
        if (newRet.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        boolean atLeastOneSelectedQualityExists = false;
        ArrayList<String> selected_qualities = new ArrayList<String>();
        if (newRet.size() > 1 && grabBEST) {
            tmpBestMap = findBESTInsideGivenMap(best_map);
            if (!tmpBestMap.isEmpty()) {
                atLeastOneSelectedQualityExists = true;
                best_map = tmpBestMap;
            }
        } else {
            boolean grab_0 = cfg.isGrabHTTPMp4XSVideoEnabled();
            boolean grab_A = cfg.isGrabHTTPMp4SVideoEnabled();
            boolean grab_B = cfg.isGrabHTTPMp4MVideoEnabled();
            boolean grab_C = cfg.isGrabHTTPMp4LVideoEnabled();
            boolean grab_E = cfg.isGrabHTTPMp4XLVideoEnabled();
            boolean grab_X = cfg.isGrabHTTPMp4XXLVideoEnabled();
            /* User deselected all --> Add all */
            if (!grab_0 && !grab_A && !grab_B && !grab_C && !grab_E && !grab_X) {
                grab_0 = true;
                grab_A = true;
                grab_B = true;
                grab_C = true;
                grab_E = true;
                grab_X = true;
            }
            if (grab_X) {
                selected_qualities.add("X");
            }
            if (grab_E) {
                selected_qualities.add("E");
            }
            if (grab_C) {
                selected_qualities.add("C");
            }
            if (grab_B) {
                selected_qualities.add("B");
            }
            if (grab_A) {
                selected_qualities.add("A");
            }
            if (grab_0) {
                selected_qualities.add("0");
            }
            for (final String selected_quality : selected_qualities) {
                if (best_map.containsKey(selected_quality)) {
                    if (!atLeastOneSelectedQualityExists) {
                        atLeastOneSelectedQualityExists = true;
                    }
                    tmpBestMap.put(selected_quality, best_map.get(selected_quality));
                }
            }
            if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled() && atLeastOneSelectedQualityExists) {
                /* Select highest quality inside user selected qualities. */
                best_map = findBESTInsideGivenMap(tmpBestMap);
            } else {
                best_map = tmpBestMap;
            }
        }
        if (!atLeastOneSelectedQualityExists) {
            selected_qualities = (ArrayList<String>) Arrays.asList(all_possible_qualities);
        }
        final Iterator<Entry<String, DownloadLink>> it = best_map.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink keep = entry.getValue();
            if (keep != null) {
                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && subtitle_url != null) {
                    final String subtitle_filename = date_formatted + "_br_" + show + " - " + plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://brdecrypted-online.de/?format=xml&quality=" + resolution + "&hash=" + playerLinkID);
                    final String linkID = keep.getSetLinkID();
                    if (linkID != null) {
                        dl_subtitle.setLinkID(linkID + "/subtitle");
                    }
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setContentUrl(parameter);
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    decryptedLinks.add(dl_subtitle);
                }
                decryptedLinks.add(keep);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(date_formatted + "_br_" + show + " - " + plain_name);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> bestMap) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (bestMap.size() > 0) {
            for (final String quality : all_possible_qualities) {
                keep = bestMap.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = bestMap;
        }
        return newMap;
    }

    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>\"]*?)\\]\\]></" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        }
        return result;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
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