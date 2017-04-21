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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.RtbfBe.RtbfBeConfigInterface;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rtbf.be" }, urls = { "https?://(?:www\\.)?rtbf\\.be/(?:video|auvio)/.+\\?id=\\d+" })
public class RtbfBeDecrypter extends PluginForDecrypt {

    public RtbfBeDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    private static final String FAST_LINKCHECK      = "FAST_LINKCHECK";

    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>  all_known_qualities = Arrays.asList("hls_mp4_1080", "hls_mp4_720", "http_mp4_720", "hls_mp4_570", "hls_mp4_480", "http_mp4_480", "hls_mp4_200", "http_mp4_200", "hls_mp4_360", "http_webm_high", "hls_mp4_270", "http_mp4_high", "http_webm_low", "hls_mp4_170", "http_mp4_low", "hls_aac_0");

    private boolean             fastLinkcheck       = false;

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final RtbfBeConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.RtbfBe.RtbfBeConfigInterface.class);
        final String parameter = param.toString();
        final String id_url = new Regex(parameter, "(\\d+)$").getMatch(0);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String decryptedhost = "http://" + this.getHost() + "decrypted/";
        String date_formatted = null;

        fastLinkcheck = cfg.isFastLinkcheckEnabled();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String video_id = this.br.getRegex("/embed/media\\?id=(\\d+)").getMatch(0);
        if (video_id == null) {
            /*
             * Fallback - that is a bad fallback as this must not be the ID we're looking for but it could sometimes work fine as a
             * fallback!
             */
            video_id = id_url;
        }

        String title = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\s+(?::|-)\\s+RTBF\\s+(?:Vidéo|Auvio)\"").getMatch(0);
        if (title == null) {
            /* Fallback 1 - title with date --> Grab title without date */
            title = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\- \\d{2}/\\d{2}/\\d{4}").getMatch(0);
        }
        /* 2017-03-01: Removed subtitle for now as we got faulty names before. Title should actually contain everything we need! */
        final String subtitle = this.br.getRegex("<h2 class=\"rtbf\\-media\\-item__subtitle\">([^<>]+)</h2>").getMatch(0);
        final String uploadDate = PluginJSonUtils.getJsonValue(this.br, "uploadDate");
        if (uploadDate != null) {
            date_formatted = new Regex(uploadDate, "^(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        }
        br.getPage("/auvio/embed/media?id=" + video_id + "&autoplay=1");
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // final String video_json_metadata = this.br.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
        String video_json = br.getRegex("<div class=\"js\\-player\\-embed.*?\" data\\-video=\"(.*?)\">").getMatch(0);
        if (video_json == null) {
            video_json = this.br.getRegex("data\\-video=\"(.*?)\"").getMatch(0);
        }
        if (video_json == null) {
            video_json = this.br.getRegex("data\\-media=\"(.*?)\"></div>").getMatch(0);
        }
        if (video_json == null) {
            return null;
        }
        // this is json encoded with htmlentities.
        video_json = HTMLEntities.unhtmlentities(video_json);
        video_json = Encoding.htmlDecode(video_json);
        video_json = PluginJSonUtils.unescape(video_json);
        // we can get filename here also.
        if (title == null) {
            title = PluginJSonUtils.getJsonValue(video_json, "title");
        }
        if (title == null || title.equalsIgnoreCase("")) {
            /* Fallback */
            title = video_id;
        }
        title = Encoding.htmlDecode(title).trim();
        title = "rtbf_" + title;
        if (date_formatted != null) {
            title = date_formatted + "_" + title;
        }
        if (subtitle != null && !subtitle.equalsIgnoreCase("{{subtitle}}")) {
            title = title + " - " + subtitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        /* Quality selection */
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        List<String> all_selected_qualities = new ArrayList<String>();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final boolean grabBEST = cfg.isGrabBESTEnabled();
        final boolean grabBESTWithinSelected = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        final boolean grabHls200 = cfg.isGrabHLS200pVideoEnabled();
        final boolean grabHls480 = cfg.isGrabHLS480pVideoEnabled();
        final boolean grabHls570 = cfg.isGrabHLS570pVideoEnabled();
        final boolean grabHls720 = cfg.isGrabHLS720pVideoEnabled();
        final boolean grabHls1080 = cfg.isGrabHLS1080pVideoEnabled();

        if (grabHls200) {
            all_selected_qualities.add("hls_mp4_200");
        }
        if (grabHls480) {
            all_selected_qualities.add("hls_mp4_480");
        }
        if (grabHls570) {
            all_selected_qualities.add("hls_mp4_570");
        }
        if (grabHls720) {
            all_selected_qualities.add("hls_mp4_720");
        }
        if (grabHls1080) {
            all_selected_qualities.add("hls_mp4_1080");
        }

        final boolean grabHttp200 = cfg.isGrabHTTP200pVideoEnabled();
        final boolean grabHttp480 = cfg.isGrabHTTP480VideoEnabled();
        final boolean grabHttp720 = cfg.isGrabHTTP720VideoEnabled();

        if (grabHttp200) {
            all_selected_qualities.add("http_mp4_200");
        }
        if (grabHttp480) {
            all_selected_qualities.add("http_mp4_480");
        }
        if (grabHttp720) {
            all_selected_qualities.add("http_mp4_720");
        }

        final boolean grab_hls = grabBEST || grabHls200 || grabHls480 || grabHls570 || grabHls720 || grabHls1080;

        /* "url" = usually highest == 720p --> Leave that out as we have it already */
        final String[] qualities = { "mobile", "web", "high" };
        String protocol = "http";
        final String linkid_format = "%s_%s_%s";
        final String format_filename = "%s_%s_%sp.mp4";
        DownloadLink dl = null;
        /* Add http qualities */
        for (final String quality_json_string : qualities) {
            final String finallink = PluginJSonUtils.getJsonValue(video_json, quality_json_string);
            if (!StringUtils.isEmpty(finallink)) {
                final String height = new Regex(finallink, "(\\d+)p\\.mp4").getMatch(0);
                if (height == null) {
                    /* Skip invalid findings. */
                    continue;
                }
                final String height_for_quality_selection = getHeightForQualitySelection(Integer.parseInt(height));
                dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String filename = String.format(format_filename, title, protocol, height);
                final String linkid = String.format(linkid_format, video_id, protocol, height_for_quality_selection);

                setDownloadlinkProperties(dl, parameter, date_formatted, filename, linkid, finallink);
                all_found_downloadlinks.put("http_mp4_" + height_for_quality_selection, dl);
            }
        }

        /* Add hls qualities if wanted. */
        protocol = "hls";
        final String hls_master = PluginJSonUtils.getJsonValue(video_json, "urlHls");
        DownloadLink best_hls_quality = null;
        long highestHlsBandwidth = 0;
        if (!StringUtils.isEmpty(hls_master) && grab_hls) {
            this.br.getPage(hls_master);
            final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(this.br);
            for (final HlsContainer hlscontainer : allHlsContainers) {
                final String height_for_quality_selection = getHeightForQualitySelection(hlscontainer.getHeight());
                final String finallink = hlscontainer.getDownloadurl();
                final String linkid = String.format(linkid_format, video_id, protocol, height_for_quality_selection);
                final String filename = String.format(format_filename, title, protocol, Integer.toString(hlscontainer.getHeight()));
                dl = createDownloadlink(decryptedhost + System.currentTimeMillis() + new Random().nextInt(1000000000));
                if (hlscontainer.getBandwidth() > highestHlsBandwidth) {
                    /*
                     * While adding the URLs, let's find the BEST quality url. In case we need it later we will already know which one is
                     * the BEST.
                     */
                    highestHlsBandwidth = hlscontainer.getBandwidth();
                    best_hls_quality = dl;
                }
                setDownloadlinkProperties(dl, parameter, date_formatted, filename, linkid, finallink);
                all_found_downloadlinks.put("hls_mp4_" + height_for_quality_selection, dl);
            }
        }

        if (grabBEST) {
            decryptedLinks.add(best_hls_quality);
        } else {
            for (final String selected_quality : all_selected_qualities) {
                final DownloadLink selected_downloadlink = all_found_downloadlinks.get(selected_quality);
                if (selected_downloadlink != null) {
                    all_selected_downloadlinks.put(selected_quality, selected_downloadlink);
                }
            }

            if (grabBESTWithinSelected) {
                all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
            }

            if (all_selected_downloadlinks.isEmpty()) {
                /* Errorhandling */
                all_selected_downloadlinks = all_found_downloadlinks;
            }
            /* Finally add selected URLs */
            final Iterator<Entry<String, DownloadLink>> it = all_selected_downloadlinks.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, DownloadLink> entry = it.next();
                decryptedLinks.add(entry.getValue());
            }
        }

        if (decryptedLinks.size() == 0) {
            logger.info("No formats were found, decrypting done...");
            return decryptedLinks;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private String getHeightForQualitySelection(final int height) {
        final String heightselect;
        if (height > 0 && height <= 300) {
            heightselect = "200";
        } else if (height > 300 && height <= 400) {
            heightselect = "360";
        } else if (height > 400 && height <= 500) {
            heightselect = "480";
        } else if (height > 500 && height <= 600) {
            heightselect = "570";
        } else if (height > 600 && height <= 800) {
            heightselect = "720";
        } else if (height > 800 && height <= 1200) {
            heightselect = "1080";
        } else {
            /* Either unknown quality or audio (0x0) */
            heightselect = Integer.toString(height);
        }
        return heightselect;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> bestMap) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (bestMap.size() > 0) {
            for (final String quality : all_known_qualities) {
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

    private void setDownloadlinkProperties(final DownloadLink dl, final String main_url, final String date_formatted, final String final_filename, final String linkid, final String finallink) {
        dl.setFinalFileName(final_filename);
        dl.setLinkID(linkid);
        dl.setProperty("date", date_formatted);
        dl.setProperty("directfilename", final_filename);
        dl.setProperty("directlink", finallink);
        dl.setContentUrl(main_url);
        if (fastLinkcheck) {
            dl.setAvailable(true);
        }
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}