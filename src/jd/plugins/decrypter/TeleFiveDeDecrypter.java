//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.Inflater;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.TeleFiveDe.Tele5DeConfigInterface;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tele5.de" }, urls = { "https?://(?:www\\.)?tele5\\.de/.+" })
public class TeleFiveDeDecrypter extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public TeleFiveDeDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String> all_known_qualities = Arrays.asList("http_2400", "http_1650", "http_1250", "http_620", "http_225");

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(this.getHost());
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Videoids with releasedate */
        final HashMap<String, String> videoidsToDecrypt = new HashMap<String, String>();
        final String parameter = param.toString();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final Tele5DeConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.TeleFiveDe.Tele5DeConfigInterface.class);
        final boolean grabBestWithinUserSelection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
        final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
        final boolean fastlinkcheck = cfg.isFastLinkcheckEnabled();
        boolean grab_225 = cfg.isGrabHTTP225kVideoEnabled();
        boolean grab_620 = cfg.isGrabHTTP620kVideoEnabled();
        boolean grab_1250 = cfg.isGrabHTTP1250kVideoEnabled();
        boolean grab_1650 = cfg.isGrabHTTP1650kVideoEnabled();
        boolean grab_2400 = cfg.isGrabHTTP2400kVideoEnabled();
        boolean grab_best = cfg.isGrabBESTEnabled();
        List<String> all_selected_qualities = new ArrayList<String>();
        if (grab_225) {
            all_selected_qualities.add("http_225");
        }
        if (grab_620) {
            all_selected_qualities.add("http_620");
        }
        if (grab_1250) {
            all_selected_qualities.add("http_1250");
        }
        if (grab_1650) {
            all_selected_qualities.add("http_1650");
        }
        if (grab_2400) {
            all_selected_qualities.add("http_2400");
        }
        final boolean user_selected_nothing = all_selected_qualities.size() == 0;
        if (user_selected_nothing) {
            logger.info("User selected no quality at all --> Adding ALL qualities instead");
            all_selected_qualities = all_known_qualities;
        }
        final String videoid_inside_url = new Regex(parameter, "v=(\\d+)").getMatch(0);
        LinkedHashMap<String, Object> entries = null;
        String json_source;
        FilePackage fp = null;
        br.setFollowRedirects(true);
        if (videoid_inside_url != null) {
            /* Single video (date unknown) */
            videoidsToDecrypt.put(videoid_inside_url, null);
        } else {
            /* Multiple videos */
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String player_id = this.br.getRegex("pid=\"(vplayer_\\d+)\"").getMatch(0);
            final String cid = this.br.getRegex("cid=\"(\\d+)\"").getMatch(0);
            String lid = this.br.getRegex("lid=\"([A-Za-z0-9\\-_]+)\"").getMatch(0);
            if ((player_id == null || lid == null) && cid == null) {
                /* Probably not a json/player/video page --> Offline */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if ((player_id == null || lid == null) && !"0".equals(cid)) {
                /* Single video */
                videoidsToDecrypt.put(cid, null);
            } else {
                /* Category/Season */
                /* Access series main url and get name of the series. */
                this.br.getPage("http://tele5.flowcenter.de/gg/play/l/" + lid + ":pid=" + player_id + "&tt=1&se=1&rpl=1&ssd=1&ssp=1&sst=1&lbt=1&");
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                json_source = this.br.getRegex("\\d+\\s*?:\\s*?(\\{.*?\\}),\\s*?\\}\\);").getMatch(0);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
                final String name_category_or_series = (String) entries.get("title");
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("entries");
                /* Use FilePackage */
                if (name_category_or_series != null) {
                    fp = FilePackage.getInstance();
                    fp.setName(name_category_or_series);
                }
                /* Find videoids (episodes). */
                for (final Object episodeo : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) episodeo;
                    final String videoid = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
                    final String date = (String) entries.get("vodate");
                    if (videoid.equals("0") || date == null || date.equals("")) {
                        continue;
                    }
                    videoidsToDecrypt.put(videoid, date);
                }
            }
        }
        /* Crawl videos. */
        final DecimalFormat df = new DecimalFormat("00");
        final Iterator<Entry<String, String>> it = videoidsToDecrypt.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, String> videoidEntry = it.next();
            final String videoid = videoidEntry.getKey();
            final String date = videoidEntry.getValue();
            if (this.isAbort()) {
                return decryptedLinks;
            }
            this.br.getPage("http://tele5.flowcenter.de/gg/play/p/" + videoid + ":mobile=0&");
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* Skip offline content */
                continue;
            }
            json_source = this.br.getRegex("setup:(\\{.*?\\}\\})\\s*?},").getMatch(0);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            /* Find dash master url. */
            final Object dash_master_url_o = JavaScriptEngineFactory.walkJson(entries, "playlist/{0}/file");
            if (!(dash_master_url_o instanceof String)) {
                /* Very rare offline case ... */
                continue;
            }
            final String dash_master_url = (String) dash_master_url_o;
            final String akamaized_videoid = new Regex(dash_master_url, "kamaized\\.net/([^/]+)/").getMatch(0);
            /* Find information about that video. */
            entries = (LinkedHashMap<String, Object>) entries.get("fw");
            final String name_episode = (String) entries.get("title");
            final Object seasono = entries.get("season");
            final Object episodeo = entries.get("episode");
            final long season;
            final long episode;
            if (seasono instanceof Boolean || episodeo instanceof Boolean) {
                /* Not part of a series. */
                season = 0;
                episode = 0;
            } else {
                /* Episode of a series. */
                season = JavaScriptEngineFactory.toLong(entries.get("season"), 0);
                episode = JavaScriptEngineFactory.toLong(entries.get("episode"), 0);
            }
            if (dash_master_url == null || akamaized_videoid == null || !dash_master_url.startsWith("http")) {
                continue;
            }
            String date_formatted = null;
            if (date != null) {
                date_formatted = new Regex(date, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
            }
            String bestQualityQualityKey = null;
            DownloadLink bestQuality = null;
            int bitrateMax = 0;
            int bitrateTemp = 0;
            String filename_part = date_formatted != null ? date_formatted + "_" : "";
            filename_part += "tele5_" + name_episode + "_";
            if (season > 0 && episode > 0) {
                filename_part += "S" + df.format(season) + "E" + df.format(episode) + "_";
            }
            this.br.getPage(dash_master_url);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* Skip offline content - some content seems to be online from the main page but actually it is offline. */
                /* E.g. response: "File not found."" */
                final DownloadLink offline = this.createOfflinelink(dash_master_url);
                offline.setFinalFileName(filename_part);
                decryptedLinks.add(offline);
                /* No distribute() here! */
                continue;
            }
            final String[] quality_entries = this.br.getRegex("<BaseURL>([^<>\"]+)</BaseURL>").getColumn(0);
            for (final String quality_entry : quality_entries) {
                final String bitrate_str = new Regex(quality_entry, "_(\\d+)k").getMatch(0);
                if (bitrate_str == null) {
                    continue;
                }
                bitrateTemp = Integer.parseInt(bitrate_str);
                final String quality_key = "http_" + bitrate_str;
                final DownloadLink dl = this.createDownloadlink("tele5decrypted://tele5.akamaized.net/" + akamaized_videoid + "/" + quality_entry);
                final String filename_final = filename_part + quality_key + ".mp4";
                dl.setFinalFileName(filename_final);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                if (bitrateTemp > bitrateMax) {
                    /* Save highest quality DownloadLink. */
                    bitrateMax = bitrateTemp;
                    bestQuality = dl;
                    bestQualityQualityKey = quality_key;
                }
                all_found_downloadlinks.put(quality_key, dl);
            }
            final HashMap<String, DownloadLink> all_selected_downloadlinks = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, grab_best, grabBestWithinUserSelection, grabUnknownQualities);
            /* Finally add selected URLs */
            final Iterator<Entry<String, DownloadLink>> it_2 = all_selected_downloadlinks.entrySet().iterator();
            while (it_2.hasNext()) {
                final Entry<String, DownloadLink> entry = it_2.next();
                final DownloadLink keep = entry.getValue();
                decryptedLinks.add(keep);
                distribute(keep);
            }
        }
        return decryptedLinks;
    }

    private HashMap<String, DownloadLink> handleQualitySelection(final HashMap<String, DownloadLink> all_found_downloadlinks, final List<String> all_selected_qualities, final boolean grab_best, final boolean grab_best_out_of_user_selection, final boolean grab_unknown) {
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
        if (grab_best) {
            for (final String possibleQuality : this.all_known_qualities) {
                if (all_found_downloadlinks.containsKey(possibleQuality)) {
                    all_selected_downloadlinks.put(possibleQuality, all_found_downloadlinks.get(possibleQuality));
                    break;
                }
            }
            if (all_selected_downloadlinks.isEmpty()) {
                logger.info("Possible issue: Best selection found nothing --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            }
        } else {
            boolean atLeastOneSelectedItemExists = false;
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grab_unknown) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grab_unknown) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            } else {
                if (grab_best_out_of_user_selection) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
            }
        }
        return all_selected_downloadlinks;
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

    /**
     * Can extract directurls from api.medianac.com.
     *
     * @param br
     *            Browser to use
     * @param decryptedhost
     *            (String) Name of the host to use in created DownloadLink's
     * @param video_source
     *            (String)html/json source that contains medianac.com API parameters
     * @param formats
     *            (Stringarray) containing information about the supported videoformats
     *
     * @throws IOException
     *
     *             2016-04-04: This code uses swf decompression. In the future this will not be needed anymore as there are enough ways
     *             around. Currently still used by the "weltderwunder.de" plugin but not by the "tele5.de" plugin anymore.
     */
    public HashMap<String, DownloadLink> getURLsFromMedianac(final Browser br, final String decryptedhost, final String video_source, final HashMap<String, String[]> formats) throws IOException {
        final String service_name = new Regex(br.getURL(), "https?://(?:www\\.)([A-Za-z0-9\\-\\.]+)\\.[A-Za-z]+/").getMatch(0);
        /* parse flash url */
        HashMap<String, DownloadLink> foundLinks = new HashMap<String, DownloadLink>();
        String entry_id = PluginJSonUtils.getJsonValue(video_source, "entry_id");
        String wid = PluginJSonUtils.getJsonValue(video_source, "wid");
        String uiconf_id = PluginJSonUtils.getJsonValue(video_source, "uiconf_id");
        String cache_st = PluginJSonUtils.getJsonValue(video_source, "cache_st");
        String streamerType = PluginJSonUtils.getJsonValue(video_source, "streamerType");
        /* Check if values are missing. Most likely this is needed to support embedded medianac videos. */
        if (wid == null) {
            wid = new Regex(video_source, "api\\.medianac\\.com/p/(\\d+)/").getMatch(0);
            if (wid != null) {
                wid = "_" + wid;
            }
        }
        if (uiconf_id == null) {
            uiconf_id = new Regex(video_source, "/uiconf_id/(\\d+)/").getMatch(0);
        }
        if (entry_id == null) {
            entry_id = new Regex(video_source, "(?:entry_id/|\\&entry_id=)([^/\\&]+)").getMatch(0);
        }
        /* Force http */
        if (streamerType == null || true) {
            streamerType = "http";
        }
        if (entry_id == null || wid == null || uiconf_id == null) {
            return null;
        }
        String flashUrl = "http://api.medianac.com/index.php/kwidget/wid/" + wid + "/uiconf_id/" + uiconf_id + "/entry_id/" + entry_id;
        if (cache_st != null) {
            flashUrl += "/cache_st/" + cache_st;
        }
        /* decompress flash */
        String result = getUrlValues(flashUrl);
        if (result == null) {
            return null;
        }
        String kdpUrl = new Regex(result, "kdpUrl=([^#]+)#").getMatch(0);
        if (kdpUrl == null) {
            return null;
        }
        kdpUrl = Encoding.htmlDecode(kdpUrl);
        /* put url vars into map */
        Map<String, String> v = new HashMap<String, String>();
        for (String s : kdpUrl.split("&")) {
            if (!s.contains("=")) {
                s += "=placeholder";
            }
            if ("referer=".equals(s)) {
                s += br.getURL();
            }
            v.put(s.split("=")[0], s.split("=")[1]);
        }
        v.put("streamerType", streamerType);
        boolean isRtmp = "rtmp".equals(streamerType);
        Browser br2 = br.cloneBrowser();
        /* create final request */
        String getData = "?service=multirequest&action=null&";
        getData += "2:entryId=" + v.get("entryId");
        getData += "&2:service=flavorasset";
        getData += "&clientTag=kdp:3.4.10.3," + v.get("clientTag");
        getData += "&2:action=getWebPlayableByEntryId";
        getData += "&3:entryId=" + v.get("entryId");
        getData += "&1:version=-1";
        getData += "&3:service=baseentry";
        getData += "&ks=" + v.get("ks");
        getData += "&1:service=baseentry";
        getData += "&3:action=getContextData";
        getData += "&3:contextDataParams:objectType=KalturaEntryContextDataParams";
        getData += "&1:entryId=" + v.get("entryId");
        getData += "&3:contextDataParams:referrer=" + v.get("referer");
        getData += "&1:action=get";
        getData += "&ignoreNull=1";
        final String getpage = "http://api.medianac.com//api_v3/index.php" + getData;
        br2.getPage(getpage);
        if (br2.containsHTML("<isCountryRestricted>1</isCountryRestricted>")) {
            /* Country blocked */
            return foundLinks;
        }
        Document doc = JDUtilities.parseXmlString(br2.toString(), false);
        /* xml data --> HashMap */
        // /xml/result/item --> name, ext etc.
        // /xml/result/item/item --> streaminfo entryId bitraten auflösung usw.
        final Node root = doc.getChildNodes().item(0);
        NodeList nl = root.getFirstChild().getChildNodes();
        HashMap<String, HashMap<String, String>> KalturaMediaEntry = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> KalturaFlavorAsset = null;
        HashMap<String, String> fileInfo = new HashMap<String, String>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childNode = nl.item(i);
            NodeList t = childNode.getChildNodes();
            for (int j = 0; j < t.getLength(); j++) {
                Node g = t.item(j);
                if ("item".equals(g.getNodeName())) {
                    KalturaFlavorAsset = new HashMap<String, String>();
                    NodeList item = g.getChildNodes();
                    for (int k = 0; k < item.getLength(); k++) {
                        Node kk = item.item(k);
                        KalturaFlavorAsset.put(kk.getNodeName(), kk.getTextContent());
                    }
                    KalturaMediaEntry.put(String.valueOf(j), KalturaFlavorAsset);
                    // if (isRtmp) break;
                    continue;
                }
                fileInfo.put(g.getNodeName(), g.getTextContent());
            }
        }
        if (fileInfo.size() == 0) {
            return null;
        }
        if (!isEmpty(fileInfo.get("categories"))) {
            fileInfo.put("categories", new String(fileInfo.get("categories").getBytes("ISO-8859-1"), "UTF-8"));
        }
        if (!isEmpty(fileInfo.get("name"))) {
            fileInfo.put("name", new String(fileInfo.get("name").getBytes("ISO-8859-1"), "UTF-8"));
        }
        boolean r = true;
        boolean gotPage = false;
        /* creating downloadLinks */
        for (Entry<String, HashMap<String, String>> next : KalturaMediaEntry.entrySet()) {
            KalturaFlavorAsset = new HashMap<String, String>(next.getValue());
            final String date = KalturaFlavorAsset.get("createdAt");
            final String bitrate = KalturaFlavorAsset.get("bitrate");
            String fName = fileInfo.get("categories");
            if (isEmpty(fName)) {
                fName = br.getRegex("<div id=\"headline\" class=\"grid_\\d+\"><h1>(.*?)</h1><").getMatch(0);
                fileInfo.put("categories", fName);
            }
            String formatString = "";
            final String videoBitrate = KalturaFlavorAsset.get("bitrate");
            final String videoWidth = KalturaFlavorAsset.get("width");
            final String videoHeight = KalturaFlavorAsset.get("height");
            final String videoResolution = videoWidth + "x" + videoHeight;
            final String qualityInfo = videoBitrate.substring(0, 1) + "_" + videoWidth.substring(0, 1) + "_" + videoHeight.substring(0, 1);
            /* Dirty "workaround": Force unknown formats */
            String[] formatinfo = null;
            String audioCodec = null;
            String audioBitrate = null;
            String videoCodec = null;
            if (formats.containsKey(qualityInfo)) {
                formatinfo = formats.get(qualityInfo);
                audioCodec = formatinfo[3];
                audioBitrate = formatinfo[4];
                videoCodec = formatinfo[0];
            }
            if (videoCodec != null) {
                formatString += videoCodec + "_";
            }
            if (videoResolution != null) {
                formatString += videoResolution + "_";
            }
            if (videoBitrate != null) {
                formatString += videoBitrate + "_";
            }
            if (audioCodec != null) {
                formatString += audioCodec + "_";
            }
            if (audioBitrate != null) {
                formatString += audioBitrate;
            }
            if (formatString.endsWith("_")) {
                formatString = formatString.substring(0, formatString.lastIndexOf("_"));
            }
            final String date_formatted = formatDate(date);
            final String filename = date_formatted + "_" + service_name + "_" + fileInfo.get("categories") + "_" + fileInfo.get("name").replaceAll("\\|", "-") + "_" + formatString + "." + KalturaFlavorAsset.get("fileExt");
            /* Always access rtmp urls as this way we get all qualities/formats --> Then build http urls out of them --> :) */
            String vidlink = "http://api.medianac.com/p/" + v.get("partnerId") + "/sp/" + v.get("subpId") + "/playManifest/entryId/" + v.get("entryId") + "/format/rtmp/protocol/rtmp/cdnHost/api.medianac.com";
            vidlink += (v.containsKey("storageId") ? "/storageId/" + v.get("storageId") : "");
            vidlink += (v.containsKey("ks") ? "/ks/" + v.get("ks") : "") + "/referrer/" + Encoding.Base64Encode(br.getURL()) + (v.containsKey("token") ? "/token/" + v.get("token") : "") + "/a/a.f4m";
            vidlink += "?referrer=" + Encoding.Base64Encode(br.getURL());
            if (vidlink.contains("null")) {
                /* WTF */
                return null;
            }
            if (r && !gotPage) {
                /* We only need to access this page once, at least if we use rtmp as streaming protocol. */
                gotPage = true;
                br2.getPage(vidlink);
            }
            final String rtmp_base_server = br2.getRegex("<baseURL>rtmp://rtmp\\.(mnac\\-p\\-\\d+)\\.c\\.nmdn\\.net/.+</baseURL>").getMatch(0);
            /* make dllink */
            vidlink = br2.getRegex("<media url=\"mp4:([^<>\"]*?)\" bitrate=\"" + bitrate + "\"").getMatch(0);
            if (vidlink != null) {
                /* rtmp --> http */
                if (rtmp_base_server == null) {
                    return null;
                }
                vidlink = "http://dl." + rtmp_base_server + ".c.nmdn.net/" + rtmp_base_server + "/" + vidlink + ".mp4";
            } else {
                vidlink = br2.getRegex("<media url=\"(http[^\"]+" + KalturaFlavorAsset.get("id") + ".*?)\"").getMatch(0);
            }
            if (vidlink == null) {
                return null;
            }
            final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            dl.setLinkID(filename);
            dl.setContentUrl(br.getURL());
            dl.setDownloadSize(SizeFormatter.getSize(KalturaFlavorAsset.get("size") + "kb"));
            dl.setProperty("streamerType", v.get("streamerType"));
            dl.setProperty("directURL", vidlink);
            if (isRtmp) {
                r = false;
                String baseUrl = br2.getRegex("<baseURL>(rtmp.*?)</baseURL").getMatch(0);
                if (baseUrl == null) {
                    /* Ignore rtmp */
                    continue;
                }
                dl.setProperty("directRTMPURL", baseUrl + "@" + vidlink);
            }
            foundLinks.put(qualityInfo, dl);
        }
        return foundLinks;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, DownloadLink> getUrlsWithoutFlash(final Browser br, final String source, final HashMap<String, String[]> formats, final String decryptedhost, final String entryid_fallback, final String partnerid_fallback, final String sp_fallback, final String title) throws Exception {
        final Browser br2 = br.cloneBrowser();
        HashMap<String, DownloadLink> foundLinks = new HashMap<String, DownloadLink>();
        /* Always access rtmp urls as this way we get all qualities/formats --> Then build http urls out of them --> :) */
        // boolean isRtmp = false;
        String entryid = null;
        String partnerid = null;
        String sp = null;
        if (source.contains("{")) {
            /* Parse json */
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(source);
            entryid = (String) entries.get("entry_id");
            partnerid = (String) entries.get("wid");
            sp = (String) entries.get("sp");
        }
        if (entryid == null) {
            entryid = entryid_fallback;
        }
        if (partnerid == null) {
            partnerid = partnerid_fallback;
        }
        if (sp == null) {
            sp = sp_fallback;
        }
        if (entryid == null || partnerid == null || sp == null) {
            return null;
        }
        partnerid = partnerid.replace("_", "");
        final String vidlink = "http://api.medianac.com/p/" + partnerid + "/sp/" + sp + "/playManifest/entryId/" + entryid + "/format/rtmp/protocol/rtmp/cdnHost/api.medianac.com";
        br2.getPage(vidlink);
        final String rtmp_base_server = br2.getRegex("<baseURL>rtmp://rtmp\\.(mnac\\-p\\-\\d+)\\.c\\.nmdn\\.net/.+</baseURL>").getMatch(0);
        /* Needed for rtmp --> http */
        if (rtmp_base_server == null) {
            return null;
        }
        final String[] videolinks = br2.getRegex("<media(.*?)/>").getColumn(0);
        for (final String videoinfo : videolinks) {
            final String linkpart = new Regex(videoinfo, "mp4:([^\"]+)").getMatch(0);
            int videoBitrateInt = 0;
            String videoBitrate = new Regex(videoinfo, "bitrate=\"(\\d+)\"").getMatch(0);
            final String videoWidth = new Regex(videoinfo, "width=\"(\\d+)\"").getMatch(0);
            final String videoHeight = new Regex(videoinfo, "height=\"(\\d+)\"").getMatch(0);
            if (linkpart == null || videoBitrate == null || videoWidth == null || videoHeight == null) {
                continue;
            }
            videoBitrateInt = Integer.parseInt(videoBitrate);
            if (videoBitrateInt < 500) {
                videoBitrate = "400";
            } else if (videoBitrateInt >= 500 && videoBitrateInt < 800) {
                videoBitrate = "600";
            } else if (videoBitrateInt >= 800 && videoBitrateInt < 1000) {
                videoBitrate = "900";
            } else {
                /* Do not correct videoBitrate */
            }
            String formatString = "";
            final String videoResolution = videoWidth + "x" + videoHeight;
            final String qualityInfo = videoBitrate.substring(0, 1) + "_" + videoWidth.substring(0, 1) + "_" + videoHeight.substring(0, 1);
            String[] formatinfo = null;
            String audioCodec = null;
            String audioBitrate = null;
            String videoCodec = null;
            /* Dirty "workaround": Force unknown formats */
            if (formats != null && formats.containsKey(qualityInfo)) {
                formatinfo = formats.get(qualityInfo);
                audioCodec = formatinfo[3];
                audioBitrate = formatinfo[4];
                videoCodec = formatinfo[0];
            }
            if (videoCodec != null) {
                formatString += videoCodec + "_";
            }
            if (videoResolution != null) {
                formatString += videoResolution + "_";
            }
            if (videoBitrate != null) {
                formatString += videoBitrate + "_";
            }
            if (audioCodec != null) {
                formatString += audioCodec + "_";
            }
            if (audioBitrate != null) {
                formatString += audioBitrate;
            }
            if (formatString.endsWith("_")) {
                formatString = formatString.substring(0, formatString.lastIndexOf("_"));
            }
            final String service_name = new Regex(br.getURL(), "https?://(?:www\\.)?([A-Za-z0-9]+)\\.").getMatch(0);
            final String filename;
            if (title != null) {
                filename = service_name + "_" + title + "_" + formatString + ".mp4";
            } else {
                filename = service_name + "_" + partnerid + "_" + entryid + "_" + formatString + ".mp4";
            }
            /* make dllink */
            final String dllink = "http://dl." + rtmp_base_server + ".c.nmdn.net/" + rtmp_base_server + "/" + linkpart + ".mp4";
            final DownloadLink dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
            dl.setAvailable(true);
            dl.setFinalFileName(filename);
            dl.setLinkID(filename);
            dl.setContentUrl(br.getURL());
            // dl.setProperty("streamerType", v.get("streamerType"));
            dl.setProperty("directURL", dllink);
            foundLinks.put(qualityInfo, dl);
        }
        return foundLinks;
    }

    public String formatDate(final String input) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        final Date theDate = new Date(Long.parseLong(input) * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    public String getUrlValues(String s) throws UnsupportedEncodingException {
        SWFDecompressor swfd = new SWFDecompressor();
        byte[] swfdec = swfd.decompress(s);
        if (swfdec == null || swfdec.length == 0) {
            return null;
        }
        for (int i = 0; i < swfdec.length; i++) {
            if (swfdec[i] < 33 || swfdec[i] > 127) {
                swfdec[i] = 35; // #
            }
        }
        return new String(swfdec, "UTF8");
    }

    public class SWFDecompressor {
        public SWFDecompressor() {
            super();
        }

        public byte[] decompress(String s) { // ~2000ms
            byte[] enc = null;
            URLConnectionAdapter con = null;
            try {
                con = new Browser().openGetConnection(s);
                final InputStream input = con.getInputStream();
                final ByteArrayOutputStream result = new ByteArrayOutputStream();
                try {
                    final byte[] buffer = new byte[512];
                    int amount;
                    while ((amount = input.read(buffer)) != -1) { // ~1500ms
                        result.write(buffer, 0, amount);
                    }
                } finally {
                    try {
                        result.close();
                    } catch (Throwable e2) {
                    }
                    enc = result.toByteArray();
                }
            } catch (Throwable e3) {
                e3.getStackTrace();
                return null;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            return uncompress(enc);
        }

        /**
         * Strips the uncompressed header bytes from a swf file byte array
         *
         * @param bytes
         *            of the swf
         * @return bytes array minus the uncompressed header bytes
         */
        private byte[] strip(byte[] bytes) {
            byte[] compressable = new byte[bytes.length - 8];
            System.arraycopy(bytes, 8, compressable, 0, bytes.length - 8);
            return compressable;
        }

        private byte[] uncompress(byte[] b) {
            Inflater decompressor = new Inflater();
            decompressor.setInput(strip(b));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length - 8);
            byte[] buffer = new byte[1024];
            try {
                while (true) {
                    int count = decompressor.inflate(buffer);
                    if (count == 0 && decompressor.finished()) {
                        break;
                    } else if (count == 0) {
                        return null;
                    } else {
                        bos.write(buffer, 0, count);
                    }
                }
            } catch (Throwable t) {
            } finally {
                decompressor.end();
            }
            byte[] swf = new byte[8 + bos.size()];
            System.arraycopy(b, 0, swf, 0, 8);
            System.arraycopy(bos.toByteArray(), 0, swf, 8, bos.size());
            swf[0] = 70; // F
            return swf;
        }
    }

    private static boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}