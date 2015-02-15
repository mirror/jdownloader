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
import java.util.LinkedHashMap;
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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vevo.com" }, urls = { "http://www\\.vevo\\.com/watch/([A-Za-z0-9\\-_]+/[^/]+/[A-Z0-9]+|[A-Z0-9]+)|http://vevo\\.ly/[A-Za-z0-9]+|http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+" }, flags = { 32 })
public class VevoComDecrypter extends PluginForDecrypt {

    public VevoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String                 DOMAIN           = "vevo.com";

    private LinkedHashMap<String, DownloadLink> FOUNDQUALITIES   = new LinkedHashMap<String, DownloadLink>();
    /** Settings stuff */
    private static final String                 ALLOW_HTTP_56    = "version_4_type_2_56";
    private static final String                 ALLOW_HTTP_500   = "version_4_type_2_500";
    private static final String                 ALLOW_HTTP_2000  = "version_4_type_2_2000";
    private static final String                 ALLOW_RTMP_500   = "version_4_type_1_500";
    private static final String                 ALLOW_RTMP_800   = "version_4_type_1_800";
    private static final String                 ALLOW_RTMP_1200  = "version_4_type_1_1200";
    private static final String                 ALLOW_RTMP_1600  = "version_4_type_1_1600";
    private static final String                 ALLOW_HLS_64     = "version_4_type_4_64";
    private static final String                 ALLOW_HLS_200    = "version_4_type_4_200";
    private static final String                 ALLOW_HLS_400    = "version_4_type_4_400";
    private static final String                 ALLOW_HLS_500    = "version_4_type_4_500";
    private static final String                 ALLOW_HLS_800    = "version_4_type_4_800";
    private static final String                 ALLOW_HLS_1200   = "version_4_type_4_1200";
    private static final String                 ALLOW_HLS_2400   = "version_4_type_4_2400";
    private static final String                 ALLOW_HLS_3200   = "version_4_type_4_3200";
    private static final String                 ALLOW_HLS_4200   = "version_4_type_4_4200";
    private static final String                 ALLOW_HLS_5200   = "version_4_type_4_5200";

    final String[]                              formats          = { ALLOW_HTTP_56, ALLOW_HTTP_500, ALLOW_HTTP_2000, ALLOW_RTMP_500, ALLOW_RTMP_800, ALLOW_RTMP_1200, ALLOW_RTMP_1600, ALLOW_HLS_64, ALLOW_HLS_200, ALLOW_HLS_400, ALLOW_HLS_500, ALLOW_HLS_800, ALLOW_HLS_1200, ALLOW_HLS_2400, ALLOW_HLS_3200, ALLOW_HLS_4200, ALLOW_HLS_5200 };

    /* Linktypes */
    private static final String                 type_short       = "http://vevo\\.ly/[A-Za-z0-9]+";
    private static final String                 type_watch       = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9\\-_]+/[^/]+/[A-Z0-9]+";
    private static final String                 type_watch_short = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9]+";
    private static final String                 type_embedded    = "http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+";
    private static final String                 player           = "http://cache.vevo.com/a/swf/versions/3/player.swf";

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        boolean geoblock_1 = false;
        boolean geoblock_2 = false;
        String title = null;
        String type_string = null;
        String dupeid = null;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        boolean rtmpAvailable = false;
        JDUtilities.getPluginForHost("vevo.com");

        String parameter = param.toString();
        if (parameter.matches(type_embedded)) {
            parameter = "http://www.vevo.com/watch/" + parameter.substring(parameter.lastIndexOf("=") + 1);
        }
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final DownloadLink offline = createDloadlink();
        offline.setProperty("mainlink", parameter);
        offline.setProperty("offline", true);
        offline.setAvailable(false);
        offline.setContentUrl(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches(type_short) && !br.getURL().matches(type_watch)) {
            logger.info("Short url either redirected to unknown url format or is offline");
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (!br.getURL().matches(type_watch)) {
            /* User probably added invalid url */
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches(type_short)) {
            /* Set- and re-use new (correct) URL in case we had a short URL before */
            parameter = br.getURL();
        }
        final String fid = getFID(parameter);
        /* Handling especially for e.g. users from Portugal */
        if (br.containsHTML("THIS PAGE IS CURRENTLY UNAVAILABLE IN YOUR REGION")) {
            geoblock_1 = true;
        } else {
            final String apijson = br.getRegex("apiResults:[\t\n\r ]*?(\\{.*?\\});").getMatch(0);
            if (apijson == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(apijson);
            final ArrayList<Object> videos = (ArrayList) entries.get("videos");
            if (videos == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final LinkedHashMap<String, Object> videoinfo = (LinkedHashMap<String, Object>) videos.get(0);
            if (videoinfo == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String year = Integer.toString(((Number) videoinfo.get("year")).intValue());
            final String artistsInfo = (String) videoinfo.get("artistsInfo");
            title = (String) videoinfo.get("title");
            if (title == null || artistsInfo == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            title = year + "_" + artistsInfo + " - " + title;
            title = Encoding.htmlDecode(title);
            title = title.trim();
            title = encodeUnicode(title);
        }

        /** Decrypt qualities START */
        if (!geoblock_1) {
            final String videoid = br.getRegex("\"isrc\":\"([^<>\"]*?)\"").getMatch(0);
            if (videoid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Referer", player);
            br.getPage("http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=" + videoid);
            final String statusCode = getJson("statusCode");
            if (statusCode.equals("304") || statusCode.equals("909")) {
                decryptedLinks.add(offline);
                return decryptedLinks;
            } else if (statusCode.equals("501")) {
                geoblock_2 = true;
            }
        }

        if (!geoblock_1 && !geoblock_2) {
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> video = (LinkedHashMap<String, Object>) entries.get("video");
            final ArrayList<Object> ressourcelist = (ArrayList) video.get("videoVersions");
            /* Explanation of sourceType: 0=undefined, 1=?RTMP?, 2=HTTP, 3=HLS iOS,4=HLS, 5=HDS, 10=SmoothStreaming, 13=RTMPAkamai */
            /*
             * Explanation of version: Seems to be different vevo data servers as it has no influence on the videoquality: 0==, 1=?,
             * 2=aka.vevo.com, 3=lvl3.vevo.com, 4=aws.vevo.com --> version 2 never worked for me
             */
            /* Last checked: 08.01.2015 */
            final int preferredVersion = 4;
            LinkedHashMap<String, Object> tempmap = null;
            String html_data = null;
            for (final Object currentStreamset : ressourcelist) {
                tempmap = (LinkedHashMap<String, Object>) currentStreamset;
                final int version = ((Number) tempmap.get("version")).intValue();
                final int sourceType = ((Number) tempmap.get("sourceType")).intValue();
                html_data = (String) tempmap.get("data");
                type_string = "version_" + version + "_type_" + sourceType;
                if (sourceType == 2 && version == preferredVersion) {
                    final String[] renditions = new Regex(html_data, "<rendition (.*?) />").getColumn(0);
                    for (final String linkinfo : renditions) {
                        final String url = new Regex(linkinfo, "url=\"(http[^<>\"]*?)\"").getMatch(0);
                        final String totalBitrate = new Regex(linkinfo, "totalBitrate=\"([^<>\"]*?)\"").getMatch(0);
                        final String videoCodec = new Regex(linkinfo, "videoCodec=\"([^<>\"]*?)\"").getMatch(0);
                        final String audioCodec = new Regex(linkinfo, "audioCodec=\"([^<>\"]*?)\"").getMatch(0);
                        final String frameWidth = new Regex(linkinfo, "frameWidth=\"([^<>\"]*?)\"").getMatch(0);
                        final String frameheight = new Regex(linkinfo, "frameheight=\"([^<>\"]*?)\"").getMatch(0);
                        final String videoBitrate = new Regex(linkinfo, "videoBitrate=\"([^<>\"]*?)\"").getMatch(0);
                        final String audioBitrate = new Regex(linkinfo, "audioBitrate=\"([^<>\"]*?)\"").getMatch(0);
                        int videobitrateint = Integer.parseInt(videoBitrate);
                        final String final_filename = title + "_" + totalBitrate + "k_" + frameWidth + "x" + frameheight + "_" + videoCodec + "_" + videoBitrate + "_" + audioCodec + "_" + audioBitrate + ".mp4";
                        final DownloadLink fina = createDloadlink();
                        fina.setAvailable(true);
                        fina.setFinalFileName(final_filename);
                        fina.setProperty("directlink", url);
                        fina.setProperty("mainlink", parameter);
                        fina.setProperty("plain_filename", final_filename);
                        fina.setProperty("sourcetype", sourceType);
                        /* Bitrates may vary so let's make sure we know what we're adding so the selection works later. */
                        if (videobitrateint <= 200) {
                            videobitrateint = 56;
                        } else if (videobitrateint > 200 && videobitrateint <= 1000) {
                            videobitrateint = 500;
                        } else {
                            videobitrateint = 2000;
                        }
                        dupeid = fid + "_" + type_string + videoBitrate;
                        try {
                            fina.setContentUrl(parameter);
                            fina.setLinkID(dupeid);
                        } catch (final Throwable e) {
                            /* Not available in 0.9.581 Stable */
                            fina.setBrowserUrl(parameter);
                        }
                        final String format = type_string + "_" + Integer.toString(videobitrateint);
                        FOUNDQUALITIES.put(format, fina);
                    }
                } else if (sourceType == 4 && version == preferredVersion) {
                    final String url = new Regex(html_data, "url=\"(http[^<>\"]*?)\"").getMatch(0);
                    br.getPage(url);
                    // http://hls-lvl3.vevo.com/v3/hls/2014/09/GB89B1000240/5c5ed477-0a30-49e7-8e15-34b6df8d3b9d/5200/gb89b1000240_5200k_1920x1080_h264_5200_aac_128.m3u8
                    final String[] medias = br.getRegex("#EXT-X-STREAM-INF:(.*?\\.m3u8)").getColumn(-1);
                    if (medias == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String hls_base = url.replace("/index.m3u8", "");
                    for (final String media : medias) {
                        final String m3u8_part = new Regex(media, "(\\d+/[A-Za-z0-9]+_\\d+k_\\d+x\\d+_[^<>\"]*?\\.m3u8)").getMatch(0);
                        final String hls_filename_part = new Regex(m3u8_part, "(_[0-9]{2,5}k_\\d+x\\d+_[a-z0-9]+_(\\d+)_.+)\\.m3u8$").getMatch(0);
                        final String videoBitrate = new Regex(hls_filename_part, "_\\d+k_\\d+x\\d+_[a-z0-9]+_(\\d+)_").getMatch(0);
                        final String directlink = hls_base + "/" + m3u8_part;
                        final String final_filename = title + hls_filename_part + ".mp4";
                        final DownloadLink fina = createDloadlink();
                        fina.setAvailable(true);
                        fina.setFinalFileName(final_filename);
                        fina.setProperty("hlsbase", hls_base);
                        fina.setProperty("directlink", directlink);
                        fina.setProperty("mainlink", parameter);
                        fina.setProperty("plain_filename", final_filename);
                        fina.setProperty("sourcetype", sourceType);
                        dupeid = fid + "_" + type_string + videoBitrate;
                        int videobitrateint = Integer.parseInt(videoBitrate);
                        dupeid = fid + "_" + type_string + videoBitrate;
                        try {
                            fina.setContentUrl(parameter);
                            fina.setLinkID(dupeid);
                        } catch (final Throwable e) {
                            /* Not available in 0.9.581 Stable */
                            fina.setBrowserUrl(parameter);
                        }
                        final String format = type_string + "_" + Integer.toString(videobitrateint);
                        FOUNDQUALITIES.put(format, fina);
                    }
                    FOUNDQUALITIES.put(type_string, null);
                } else if (sourceType == 13) {
                    rtmpAvailable = true;
                }
            }
        }
        final boolean user_wants_RTMP = (cfg.getBooleanProperty(ALLOW_RTMP_500, false) || cfg.getBooleanProperty(ALLOW_RTMP_800, false) || cfg.getBooleanProperty(ALLOW_RTMP_1200, false) || cfg.getBooleanProperty(ALLOW_RTMP_1600, false));
        /* Now get RTMP qualities if either we're geo blocked or user wants it. */
        if ((geoblock_1 || geoblock_2) || (user_wants_RTMP && rtmpAvailable)) {
            this.br = new Browser();
            this.accessSmi_url(this.br, parameter);
            final int responsecode = br.getHttpConnection().getResponseCode();
            /* 404 + geoblock = probably offline or simply geoblocked successfully */
            if (responsecode == 404 && (geoblock_1 || geoblock_2)) {
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            /* Sometimes, rtmp versions are just not available --> Skip this */
            if (responsecode == 404) {
                logger.info("rtmp versions are not available for link: " + parameter);
            } else {
                logger.info("rtmp versions are available for link: " + parameter);
                final String base = br.getRegex("base=\"(rtmp[^<>\"]*?)\"").getMatch(0);
                final String[] playpaths = br.getRegex("<video src=\"(mp4:[^<>\"]*?\\.mp4)\"").getColumn(0);
                if (playpaths == null || playpaths.length == 0 || base == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                type_string = "version_4_type_1";
                for (final String playpath : playpaths) {
                    final String rtmp_filename_part = new Regex(playpath, "(_[0-9]{2,5}k_\\d+x\\d+_[a-z0-9]+_(\\d+)_.+\\.mp4)$").getMatch(0);
                    if (rtmp_filename_part == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final String videoBitrate = new Regex(rtmp_filename_part, "_\\d+k_\\d+x\\d+_[a-z0-9]+_(\\d+)_").getMatch(0);
                    String final_filename;
                    /* geoblock_1 = no filename available at all --> Use whatever we have in our URL that comes close to the real name. */
                    if (geoblock_1) {
                        final_filename = getURLfilename(parameter) + "_" + rtmp_filename_part;
                    } else {
                        final_filename = title + rtmp_filename_part;
                    }
                    final DownloadLink fina = createDloadlink();
                    fina.setAvailable(true);
                    fina.setFinalFileName(final_filename);
                    fina.setProperty("rtmpbase", base);
                    fina.setProperty("directlink", playpath);
                    fina.setProperty("mainlink", parameter);
                    fina.setProperty("plain_filename", final_filename);
                    fina.setProperty("sourcetype", "1");
                    dupeid = fid + "_" + type_string + videoBitrate;
                    /* Bitrates may vary so let's make sure we know what we're adding so the selection works later. */
                    int videobitrateint = Integer.parseInt(videoBitrate);
                    if (videobitrateint <= 600) {
                        videobitrateint = 500;
                    } else if (videobitrateint > 600 && videobitrateint <= 1000) {
                        videobitrateint = 800;
                    } else if (videobitrateint > 1000 && videobitrateint <= 1400) {
                        videobitrateint = 1200;
                    } else {
                        videobitrateint = 1600;
                    }
                    dupeid = fid + "_" + type_string + videoBitrate;
                    try {
                        fina.setContentUrl(parameter);
                        fina.setLinkID(dupeid);
                    } catch (final Throwable e) {
                        /* Not available in 0.9.581 Stable */
                        fina.setBrowserUrl(parameter);
                    }
                    final String format = type_string + "_" + Integer.toString(videobitrateint);
                    FOUNDQUALITIES.put(format, fina);
                    /* Force adding rtmp links to the linkgrabber in case the user is geo-blocked! */
                    if (geoblock_1 || geoblock_2) {
                        decryptedLinks.add(fina);
                    }
                }
            }
        }

        if (FOUNDQUALITIES == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities END */
        /** Decrypt qualities, selected by the user */
        for (final String possibleFormat : formats) {
            final boolean user_selected = cfg.getBooleanProperty(possibleFormat, false);
            final DownloadLink theLink = FOUNDQUALITIES.get(possibleFormat);
            if (user_selected && theLink != null) {
                decryptedLinks.add(theLink);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);
        if (decryptedLinks.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found, decrypting done...");
        }
        return decryptedLinks;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private DownloadLink createDloadlink() {
        return createDownloadlink("http://vevodecrypted/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private String getFID(final String parameter) {
        return new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
    }

    private void accessSmi_url(final Browser br, final String parameter) throws IOException, PluginException {
        jd.plugins.hoster.VevoCom.accessSmi_url(br, this.getFID(parameter));
    }

    private String getURLfilename(final String parameter) {
        return jd.plugins.hoster.VevoCom.getURLfilename(parameter);
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