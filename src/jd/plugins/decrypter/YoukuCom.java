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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.YoukuCom.YoukuComConfigInterface;

/** See also youtube-dl: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youku.py */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youku.com", "video.tudou.com", "tudou.com" }, urls = { "https?://v\\.youku\\.com/v_show/id_[A-Za-z0-9=]+", "https?://video\\.tudou\\.com/v/[A-Za-z0-9=]+", "https?://(?:www\\.)?tudou\\.com/programs/view/[A-Za-z0-9\\-_]+" })
public class YoukuCom extends PluginForDecrypt {
    public YoukuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String> all_known_qualities = Arrays.asList("http_mp4_1080", "hls_mp4_1080", "http_mp4_720", "hls_mp4_720", "http_mp4_540", "hls_mp4_540", "http_mp4_360", "hls_mp4_360");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        List<String> all_selected_qualities = new ArrayList<String>();
        String parameter = param.toString();
        final YoukuComConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.YoukuCom.YoukuComConfigInterface.class);
        final boolean fastLinkcheck = cfg.isFastLinkcheckEnabled();
        final boolean grabBest = cfg.isGrabBESTEnabled();
        final boolean grabBestWithinUserSelection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
        final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
        final boolean grabhttp1080p = cfg.isGrabHTTPMp4_1080pEnabled();
        final boolean grabhttp720p = cfg.isGrabHTTPMp4_720pEnabled();
        final boolean grabhttp540p = cfg.isGrabHTTPMp4_540pEnabled();
        final boolean grabhttp360p = cfg.isGrabHTTPMp4_360pEnabled();
        final boolean grabhls1080p = cfg.isGrabHLSMp4_1080pEnabled();
        final boolean grabhls720p = cfg.isGrabHLSMp4_720pEnabled();
        final boolean grabhls540p = cfg.isGrabHLSMp4_540pEnabled();
        final boolean grabhls360p = cfg.isGrabHLSMp4_360pEnabled();
        if (grabhttp1080p) {
            all_selected_qualities.add("http_mp4_1080");
        }
        if (grabhttp720p) {
            all_selected_qualities.add("http_mp4_720");
        }
        if (grabhttp540p) {
            all_selected_qualities.add("http_mp4_540");
        }
        if (grabhttp360p) {
            all_selected_qualities.add("http_mp4_360");
        }
        if (grabhls1080p) {
            all_selected_qualities.add("hls_mp4_1080");
        }
        if (grabhls720p) {
            all_selected_qualities.add("hls_mp4_720");
        }
        if (grabhls540p) {
            all_selected_qualities.add("hls_mp4_540");
        }
        if (grabhls360p) {
            all_selected_qualities.add("hls_mp4_360");
        }
        if (all_selected_qualities.isEmpty()) {
            all_selected_qualities = all_known_qualities;
        }
        if (parameter.matches(".+tudou\\.com/programs/view/.+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            /* Old tudou.com URLs with old videoIDs --> Usually redirect to new URLs with new IDs. */
            final String newURL = this.br.getRedirectLocation();
            if (newURL == null || !newURL.matches(".+/v/[A-Za-z0-9=]+.*?")) {
                logger.info("Failed to find new tudou URL --> Offline");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.info("Found new tudou URL: " + newURL);
            parameter = newURL;
        }
        final String videoid = jd.plugins.hoster.YoukuCom.getURLName(parameter);
        /* Important: Use fresh Browser here!! */
        this.br = prepBR(new Browser());
        accessVideoJson(this.br, parameter);
        if (isOffline(br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        /* TODO: Add better errorhandling here */
        final Object erroro = entries.get("error");
        final LinkedHashMap<String, Object> errormap = erroro != null ? (LinkedHashMap<String, Object>) erroro : null;
        if (errormap != null) {
            /* -4001 == blocked because of copyright [can also happen when using wrong ccode in API request] */
            final String note = (String) errormap.get("note");
            final long errorcode = JavaScriptEngineFactory.toLong(errormap.get("code"), 0);
            logger.info(String.format("Error: Code: %d Message: %s", errorcode, note));
        }
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("stream");
        ArrayList<Object> segment_list;
        entries = (LinkedHashMap<String, Object>) entries.get("video");
        String title = (String) entries.get("title");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = videoid;
        }
        final HashMap<String, DownloadLink[]> all_found_downloadlinks = new HashMap<String, DownloadLink[]>();
        final String filename_format = "%s_%s_%s_%s.mp4";
        final String quality_key_format = "%s_mp4_%s";
        String protocol;
        for (final Object streamo : ressourcelist) {
            protocol = "hls";
            entries = (LinkedHashMap<String, Object>) streamo;
            final String url_hls = (String) entries.get("m3u8_url");
            segment_list = (ArrayList<Object>) entries.get("segs");
            final String width = Long.toString(JavaScriptEngineFactory.toLong(entries.get("width"), 0));
            final String height = Long.toString(JavaScriptEngineFactory.toLong(entries.get("height"), 0));
            long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            if (width.equals("0") || height.equals("0")) {
                /* WTF */
                continue;
            }
            final String resolution = width + "x" + height;
            int segment_counter = 1;
            final int padLength = getPadLength(segment_list.size());
            /* Add HLS URL */
            DownloadLink dl = this.createDownloadlink(url_hls);
            String segment_counter_formatted = String.format(Locale.US, "%0" + padLength + "d", segment_counter);
            String quality_key = String.format(quality_key_format, protocol, height);
            dl.setFinalFileName(String.format(filename_format, title, protocol, resolution, segment_counter_formatted));
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
                dl.setAvailable(true);
            } else if (fastLinkcheck) {
                dl.setAvailable(true);
            }
            dl.setLinkID(videoid + quality_key + segment_counter);
            dl.setProperty("quality_height", height);
            dl.setProperty("mainlink", parameter);
            all_found_downloadlinks.put(quality_key, new DownloadLink[] { dl });
            /* Add http URLs */
            protocol = "http";
            quality_key = String.format(quality_key_format, protocol, height);
            final DownloadLink[] httpSegments = new DownloadLink[segment_list.size()];
            for (final Object segmento : segment_list) {
                entries = (LinkedHashMap<String, Object>) segmento;
                final String url_http = (String) entries.get("cdn_url");
                filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
                if (StringUtils.isEmpty(url_http)) {
                    continue;
                }
                segment_counter_formatted = String.format(Locale.US, "%0" + padLength + "d", segment_counter);
                /* Re-use that variable */
                dl = this.createDownloadlink(url_http);
                dl.setFinalFileName(String.format(filename_format, title, protocol, resolution, segment_counter_formatted));
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                    dl.setAvailable(true);
                } else if (fastLinkcheck) {
                    dl.setAvailable(true);
                }
                dl.setLinkID(videoid + quality_key + segment_counter);
                dl.setProperty("quality_height", height);
                dl.setProperty("segment_position", (long) segment_counter - 1);
                dl.setProperty("mainlink", parameter);
                httpSegments[segment_counter - 1] = dl;
                segment_counter++;
            }
            all_found_downloadlinks.put(quality_key, httpSegments);
        }
        final HashMap<String, DownloadLink> all_selected_downloadlinks = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, grabBest, grabBestWithinUserSelection, grabUnknownQualities);
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it_2 = all_selected_downloadlinks.entrySet().iterator();
        while (it_2.hasNext()) {
            final Entry<String, DownloadLink> entry = it_2.next();
            final DownloadLink keep = entry.getValue();
            decryptedLinks.add(keep);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;
        }
    }

    private HashMap<String, DownloadLink> handleQualitySelection(final HashMap<String, DownloadLink[]> all_found_downloadlinks, final List<String> all_selected_qualities, final boolean grab_best, final boolean grab_best_out_of_user_selection, final boolean grab_unknown) {
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        final Iterator<Entry<String, DownloadLink[]>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
        if (grab_best) {
            for (final String possibleQuality : this.all_known_qualities) {
                if (all_found_downloadlinks.containsKey(possibleQuality)) {
                    all_selected_downloadlinks = addSegmentsToHashMap(all_selected_downloadlinks, possibleQuality, all_found_downloadlinks.get(possibleQuality));
                    break;
                }
            }
            if (all_selected_downloadlinks.isEmpty()) {
                logger.info("Possible issue: Best selection found nothing --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink[]> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks = addSegmentsToHashMap(all_selected_downloadlinks, dl_entry.getKey(), dl_entry.getValue());
                }
            }
        } else {
            boolean atLeastOneSelectedItemExists = false;
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink[]> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks = addSegmentsToHashMap(all_selected_downloadlinks, dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grab_unknown) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grab_unknown) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks = addSegmentsToHashMap(all_selected_downloadlinks, dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink[]> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks = addSegmentsToHashMap(all_selected_downloadlinks, dl_entry.getKey(), dl_entry.getValue());
                }
            } else {
                if (grab_best_out_of_user_selection) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
            }
        }
        return all_selected_downloadlinks;
    }

    private HashMap<String, DownloadLink> addSegmentsToHashMap(final HashMap<String, DownloadLink> inputmap, final String main_quality_key, final DownloadLink[] newSegments) {
        int counter = 0;
        for (final DownloadLink dl : newSegments) {
            inputmap.put(main_quality_key + counter, dl);
            counter++;
        }
        return inputmap;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_downloadlinks) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_downloadlinks.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_downloadlinks.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_downloadlinks;
        }
        return newMap;
    }

    public static void accessVideoJson(final Browser br, final String source_url) throws IOException, InterruptedException {
        if (br == null || source_url == null) {
            return;
        }
        final String videoid = jd.plugins.hoster.YoukuCom.getURLName(source_url);
        final String host = Browser.getHost(source_url);
        br.setCookie(host, "xreferrer", "http://www.youku.com");
        br.getPage("https://log.mmstat.com/eg.js");
        final String cna_value = br.getRegex("goldlog\\.Etag=\"([^<>\"]+)\"").getMatch(0);
        final String ccode;
        if (host.equals("youku.com")) {
            ccode = "0401";
        } else {
            /* tudou.com */
            ccode = "0402";
        }
        if (cna_value == null) {
            return;
        }
        br.setCookie(host, "cna", cna_value);
        /* This waittime is important! */
        Thread.sleep(3000l);
        br.getPage("https://ups.youku.com/ups/get.json?vid=" + Encoding.urlEncode(videoid) + "&ccode=" + ccode + "&client_ip=192.168.1.1&utid=" + Encoding.urlEncode(cna_value) + "&client_ts=" + System.currentTimeMillis());
    }

    public static String getCnaValue(final Browser br) {
        return br.getCookie(br.getHost(), "cna");
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    public static boolean isOffline(final Browser br) {
        final String apiError = PluginJSonUtils.getJson(br, "code");
        final boolean offlineAPI = apiError != null && apiError.equals("-6001");
        final boolean offlineWebsite = br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/index/");
        return offlineAPI || offlineWebsite;
    }
}
