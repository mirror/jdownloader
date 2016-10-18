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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "atv.at" }, urls = { "http://(?:www\\.)?atv\\.at/[a-z0-9\\-_]+/[a-z0-9\\-_]+/(?:d|v)\\d+/" })
public class AtvAt extends PluginForDecrypt {

    public AtvAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Important note: Via browser the videos are streamed via RTSP.
     *
     * Old URL: http://atv.at/binaries/asset/tvnext_clip/496790/video
     *
     *
     * --> http://b2b.atv.at/binaries/asset/tvnext_clip/496790/video
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex linkinfo = new Regex(parameter, "atv\\.at/([a-z0-9\\-_]+)/([a-z0-9\\-_]+)/((?:d|v)\\d+)/$");
        String url_seriesname = linkinfo.getMatch(0);
        String url_episodename = linkinfo.getMatch(1);
        boolean geo_blocked = false;
        final String fid = linkinfo.getMatch(2);
        final String url_seriesname_remove = new Regex(url_seriesname, "((?:\\-)?staffel\\-\\d+)").getMatch(0);
        final String url_episodename_remove = new Regex(url_episodename, "((?:\\-)?folge\\-\\d+)").getMatch(0);
        String seriesname = null;
        String episodename = null;
        short seasonnumber = -1;
        short episodenumber = -1;
        String seasonnumber_str = null;
        String episodenumber_str = null;
        final DecimalFormat df = new DecimalFormat("00");

        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline (404 error): " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(fid);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (!br.containsHTML("class=\"jsb_ jsb_video/FlashPlayer\"")) {
            logger.info("There is no downloadable content: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(fid);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("is_geo_ip_blocked\\&quot;:true")) {
            /*
             * We can get the direct links of geo blocked videos anyways - also, this variable only tells if a video is geo blocked at all -
             * this does not necessarily mean that it is blocked in the users'country!
             */
            logger.info("Video might not be available in your country [workaround might be possible though]: " + parameter);
            geo_blocked = true;
        }
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));

        String json_source = br.getRegex("<div class=\"jsb_ jsb_video/FlashPlayer\" data\\-jsb=\"([^\"<>]+)\">").getMatch(0);
        json_source = Encoding.htmlDecode(json_source);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "config/initial_video");
        final ArrayList<Object> parts = (ArrayList<Object>) entries.get("parts");
        ArrayList<Object> sources = null;
        /* Get filename information */
        seriesname = this.br.getRegex("\">zurück zu ([^<>\"]*?)<span class=\"ico ico_close\"").getMatch(0);
        if (seriesname == null) {
            /* Fallback to URL information */
            seriesname = url_seriesname.replace("-", " ");
        }

        episodename = this.br.getRegex("property=\"og:title\" content=\"Folge \\d+ \\- ([^<>\"]*?)\"").getMatch(0);
        if (episodename == null) {
            /* Fallback to URL information */
            episodename = url_episodename.replace("-", " ");
        }
        if (url_seriesname_remove != null) {
            seasonnumber_str = new Regex(url_seriesname_remove, "(\\d+)$").getMatch(0);
            /* Clean url_seriesname */
            url_seriesname = url_seriesname.replace(url_seriesname_remove, "");
        }
        if (url_episodename_remove != null) {
            episodenumber_str = new Regex(url_episodename_remove, "(\\d+)$").getMatch(0);
            /* Clean url_episodename! */
            url_episodename = url_episodename.replace(url_episodename_remove, "");
        }
        if (episodenumber_str == null) {
            episodenumber_str = br.getRegex("class=\"headline\">Folge (\\d+)</h4>").getMatch(0);
        }
        if (seasonnumber_str != null && episodenumber_str != null) {
            seasonnumber = Short.parseShort(seasonnumber_str);
            episodenumber = Short.parseShort(episodenumber_str);
        }
        String hybrid_name = seriesname + "_";
        if (seasonnumber > 0 && episodenumber > 0) {
            /* That should be given in most of all cases! */
            hybrid_name += "S" + df.format(seasonnumber) + "E" + df.format(episodenumber) + "_";
        }
        hybrid_name += episodename;
        hybrid_name = Encoding.htmlDecode(hybrid_name);
        hybrid_name = decodeUnicode(hybrid_name);

        final String[] possibleQualities = { "1080", "720", "576", "540", "360", "226" };
        final int possibleQualitiesNumber = possibleQualities.length;
        final FFmpeg ffmpeg = new FFmpeg();
        int part_counter = 1;
        for (final Object parto : parts) {
            entries = (LinkedHashMap<String, Object>) parto;
            sources = (ArrayList<Object>) entries.get("sources");
            for (final Object source : sources) {
                entries = (LinkedHashMap<String, Object>) source;
                final String protocol = (String) entries.get("protocol");
                final String delivery = (String) entries.get("delivery");
                // final String type = (String) entries.get("type");
                String src = (String) entries.get("src");
                if (!"http".equalsIgnoreCase(protocol) || src == null || !src.startsWith("http")) {
                    /* Skip unknown/unsupported streaming protocols */
                    continue;
                }
                /* Get around GEO-block */
                src = src.replaceAll("http?://blocked.", "http://");
                /* Some variables we need in that loop below. */
                DownloadLink link = null;
                String quality = null;
                String finalname = null;
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(hybrid_name);
                final String part_formatted = df.format(part_counter);
                if ("streaming".equalsIgnoreCase(delivery) || src.contains(".m3u8")) {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user");
                        return decryptedLinks;
                    }
                    if (!ffmpeg.isAvailable()) {
                        logger.info("Ffmpeg is not installed --> Skipping hls urls");
                        continue;
                    }
                    /* Find all hls qualities */
                    /* 2016-10-18: It is possible to change hls urls to http urls! */
                    this.br.getPage(src);
                    final String[] qualities = this.br.getRegex("BANDWIDTH=").getColumn(-1);
                    final int qualitiesNum = qualities.length;
                    if (this.br.containsHTML("#EXT-X-STREAM-INF")) {
                        int qualityIndex = 0;
                        for (String line : Regex.getLines(br.toString())) {
                            if (!line.startsWith("#")) {
                                if (this.isAbort()) {
                                    logger.info("Decryption aborted by user");
                                    return decryptedLinks;
                                }
                                long estimatedSize = 0;
                                /* Reset quality value */
                                quality = null;
                                link = createDownloadlink(br.getBaseURL() + line);
                                link.setContainerUrl(parameter);

                                try {
                                    /* try to get the video quality */
                                    final HLSDownloader downloader = new HLSDownloader(link, br, link.getDownloadURL()) {
                                        @Override
                                        public LogInterface initLogger(DownloadLink link) {
                                            return getLogger();
                                        }
                                    };
                                    final M3U8Playlist m3u8PlayList = downloader.getM3U8Playlist();
                                    estimatedSize = m3u8PlayList.getEstimatedSize();
                                    final StreamInfo streamInfo = downloader.getProbe();
                                    for (Stream s : streamInfo.getStreams()) {
                                        if ("video".equals(s.getCodec_type())) {
                                            quality = s.getHeight() + "p";
                                            break;
                                        }
                                    }

                                } catch (Throwable e) {
                                    getLogger().log(e);
                                    estimatedSize = 0;
                                }
                                finalname = hybrid_name + "_";
                                finalname += "hls_part_";
                                if (quality == null && qualitiesNum <= possibleQualitiesNumber) {
                                    /*
                                     * Workaround for possible Ffmpeg issue. We know that the bitrates go from highest to lowest so we can
                                     * assume which quality we have here in case the Ffmpeg method fails!
                                     */
                                    quality = possibleQualities[possibleQualitiesNumber - qualitiesNum + qualityIndex] + "p";
                                }
                                if (quality != null) {
                                    finalname += quality + "_";
                                }
                                finalname += part_formatted + ".mp4";

                                link.setFinalFileName(finalname);
                                if (estimatedSize > 0) {
                                    link.setAvailable(true);
                                    link.setDownloadSize(estimatedSize);
                                }
                                link.setContentUrl(parameter);
                                link._setFilePackage(fp);
                                decryptedLinks.add(link);
                                distribute(link);
                                qualityIndex++;
                            }

                        }
                    }
                } else {
                    /* delivery:progressive --> http url */
                    /* 2016-10-18: Seems like their http urls always have the same quality */
                    quality = "576p";
                    link = this.createDownloadlink("directhttp://" + src);
                    finalname = hybrid_name + "_";
                    finalname += "http_part_";
                    finalname += quality + "_";
                    finalname += part_formatted + ".mp4";
                    link.setFinalFileName(finalname);
                    // link.setAvailable(true);
                    link.setContentUrl(parameter);
                    link._setFilePackage(fp);
                    decryptedLinks.add(link);
                    distribute(link);
                }

            }
            part_counter++;
        }

        if (decryptedLinks.size() == 0 && geo_blocked) {
            logger.info("GEO-blocked");
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName("GEO_blocked_" + fid);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}