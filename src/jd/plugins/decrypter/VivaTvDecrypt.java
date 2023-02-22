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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;
import jd.plugins.hoster.VivaTv;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mtv.de", "mtviggy.com", "southpark.de", "southpark.cc.com", "mtv.com.au", "mtv.co.uk", "mtv.com", "cc.com", "funnyclips.cc", "comedycentral.tv", "nick.de", "tvland.com", "spike.com", "thedailyshow.cc.com", "tosh.cc.com", "mtvu.com" }, urls = { "https?://(?:www\\.)?mtv\\.de/.+", "https?://(?:www\\.)?(?:mtviggy|mtvdesi|mtvk)\\.com/.+", "https?://(?:www\\.)?southpark\\.de/.+", "https?://southpark\\.cc\\.com/.+", "https?://(?:www\\.)?mtv\\.com\\.au/.+", "https?://(?:www\\.)?mtv\\.co\\.uk/.+", "https?://(?:www\\.)?mtv\\.com/.+", "https?://(?:www\\.)?cc\\.com/.+", "https?://de\\.funnyclips\\.cc/.+", "https?://(?:www\\.)?comedycentral\\.tv/.+", "https?://(?:www\\.)?nick\\.de/.+", "https?://(?:www\\.)?tvland\\.com/.+", "https?://(?:www\\.)?spike\\.com/.+", "https?://thedailyshow\\.cc\\.com/.+",
        "https?://tosh\\.cc\\.com/.+", "https?://(?:www\\.)?mtvu\\.com/.+" })
public class VivaTvDecrypt extends PluginForDecrypt {
    public VivaTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }
    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */

    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */
    /* Additional information/methods can be found in the VivaTv host plugin */
    /** TODO: mtvplay.tv */
    private static final String     type_viva                    = "https?://(?:www\\.)?viva\\.tv/.+";
    private static final String     type_mtv_de                  = "https?://(?:www\\.)?mtv\\.de/.+";
    private static final String     type_southpark_cc_episode    = "http://southpark\\.cc\\.com/full\\-episodes/.+";
    private static final String     type_mtv_com                 = "https?://(?:www\\.)?mtv\\.com/.+";
    private static final String     hosterplugin_url_viacom_mgid = "http://viacommgid/";
    private static final String     PATTERN_MGID                 = "mgid:[A-Za-z]+:[A-Za-z0-9_\\-]+:[A-Za-z0-9\\.\\-]+:[A-Za-z0-9_\\-]+";
    private ArrayList<DownloadLink> decryptedLinks               = new ArrayList<DownloadLink>();
    private String                  default_ext                  = null;
    private String                  mgid                         = null;
    private String                  fpName                       = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("viva.tv");
        default_ext = VivaTv.default_ext;
        VivaTv.prepBR(this.br);
        if (this.getHost().equals("nick.de")) {
            return crawlNickDe(param);
        } else if (param.getCryptedUrl().matches(type_southpark_cc_episode)) {
            crawlSouthparkCc(param);
        } else if (param.getCryptedUrl().matches(type_mtv_com)) {
            crawlMtvCom(param);
        } else {
            /* Universal viacom crawler */
            this.br.getPage(param.getCryptedUrl());
            vivaUniversalCrawler(param);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlNickDe(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        return this.crawlWebsiteJson(param, new ArrayList<DownloadLink>());
    }

    private ArrayList<DownloadLink> crawlWebsiteJson(final CryptedLink param, final ArrayList<DownloadLink> ret) throws Exception {
        final String jsonRoot = br.getRegex("window\\.__DATA__\\s*=\\s*(\\{.*?\\});\\s+").getMatch(0);
        if (jsonRoot == null) {
            /* Assume that there is no downloadable content available. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Try to find all video objects */
        final ArrayList<Map<String, Object>> websiteVideoObjects = new ArrayList<Map<String, Object>>();
        final Map<String, Object> root = JSonStorage.restoreFromString(jsonRoot, TypeRef.HASHMAP);
        findVideoMaps(websiteVideoObjects, root);
        if (websiteVideoObjects.isEmpty()) {
            logger.info("Failed to find any video items inside json");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // String episodeNumber = null;
        // if (websiteVideoObjects.size() == 1) {
        // /* We can only do this if we only got one item */
        // episodeNumber = new Regex(param.getCryptedUrl(), "-ep-(\\d+)").getMatch(0);
        // }
        final DecimalFormat sf = new DecimalFormat("00");
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Accept", "application/json");
        int index = 0;
        for (final Map<String, Object> websiteVideoPlayerRoot : websiteVideoObjects) {
            long seasonNumber = -1;
            long episodeNumber = -1;
            String tvShow = null;
            final Map<String, Object> props = (Map<String, Object>) websiteVideoPlayerRoot.get("props");
            final String schemaText = (String) props.get("schema");
            if (schemaText != null) {
                final Map<String, Object> schema = JSonStorage.restoreFromString(schemaText, TypeRef.HASHMAP);
                final String mediaType = (String) schema.get("@type");
                if (mediaType.equalsIgnoreCase("TVEpisode")) {
                    final Map<String, Object> partOfSeries = (Map<String, Object>) schema.get("partOfSeries");
                    seasonNumber = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(schema, "partOfSeason/seasonNumber"), -1);
                    episodeNumber = JavaScriptEngineFactory.toLong(schema.get("episodeNumber"), -1);
                    tvShow = partOfSeries.get("name").toString();
                } else {
                    /* Type "VideoObject" */
                }
            }
            final Map<String, Object> videoPlayer = (Map<String, Object>) JavaScriptEngineFactory.walkJson(props, "media/video");
            // final String entityType = (String) video.get("entityType");
            final Map<String, Object> config = (Map<String, Object>) videoPlayer.get("config");
            final String uri = (String) config.get("uri");
            logger.info("Crawling uri " + (index + 1) + "/" + websiteVideoObjects.size() + " --> " + uri);
            final UrlQuery query = new UrlQuery();
            query.add("uri", Encoding.urlEncode(uri));
            query.add("configtype", "edge");
            query.add("ref", Encoding.urlEncode(param.getCryptedUrl()));
            brc.getPage("https://media.mtvnservices.com/pmt/e1/access/index.html?" + query.toString());
            final Map<String, Object> uriRoot = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            final Map<String, Object> feed = (Map<String, Object>) uriRoot.get("feed");
            final String title = (String) feed.get("title");
            final String description = (String) feed.get("description");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
            final Object imageO = feed.get("image");
            if (imageO != null) {
                final Map<String, Object> thumbnailMap = (Map<String, Object>) imageO;
                // final String thumbnailFilename = (String) thumbnailMap.get("title");
                final String thumbnailurl = (String) thumbnailMap.get("url");
                if (!StringUtils.isEmpty(thumbnailurl)) {
                    final DownloadLink thumbnail = this.createDownloadlink("directhttp://" + thumbnailurl);
                    thumbnail.setFinalFileName(title + "_thumbnail.jpg");
                    // thumbnail.setFinalFileName(thumbnailFilename);
                    thumbnail._setFilePackage(fp);
                    distribute(thumbnail);
                    ret.add(thumbnail);
                }
            }
            /* Some videos are split into multiple parts */
            final List<Map<String, Object>> parts = (List<Map<String, Object>>) feed.get("items");
            final DecimalFormat pf = new DecimalFormat("00");
            int partNumber = 0;
            for (final Map<String, Object> part : parts) {
                partNumber++;
                logger.info("Crawling video part: " + partNumber + "/" + parts.size());
                final Map<String, Object> group = (Map<String, Object>) part.get("group");
                final Map<String, Object> metadata = (Map<String, Object>) group.get("category");
                // final String seasonNumber = (String) metadata.get("seasonN");
                final String mediagenURL = (String) group.get("content");
                final UrlQuery queryMediagen = UrlQuery.parse(mediagenURL);
                /* We don't want rtmp(e) */
                queryMediagen.addAndReplace("acceptMethods", "hls");
                queryMediagen.addAndReplace("tveprovider", "null");
                /* We don't want XML */
                queryMediagen.addAndReplace("format", "json");
                /* Default = "device={device}" --> If left in, we'll get rtmp(e) streams */
                queryMediagen.remove("device");
                final String mediagenURLWithoutParams = mediagenURL.substring(0, mediagenURL.lastIndexOf("?"));
                final Browser brMedia = br.cloneBrowser();
                brMedia.getHeaders().put("Accept", "application/json");
                brMedia.getPage(mediagenURLWithoutParams + "?" + queryMediagen.toString());
                final Map<String, Object> video = JSonStorage.restoreFromString(brMedia.toString(), TypeRef.HASHMAP);
                final List<Map<String, Object>> videoStreams = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(video, "package/video/item");
                if (videoStreams.size() > 1) {
                    logger.warning("Multiple video streams available: " + videoStreams.size());
                }
                String filenameBase;
                if (seasonNumber > -1 && episodeNumber > -1) {
                    final String seasonEpisodeNumberInfoFormatted = "S" + sf.format(seasonNumber) + "E" + sf.format(episodeNumber);
                    filenameBase = title;
                    if (!StringUtils.isEmpty(tvShow)) {
                        filenameBase = tvShow + "_" + seasonEpisodeNumberInfoFormatted + " - " + title;
                    } else {
                        filenameBase = title + "_" + seasonEpisodeNumberInfoFormatted;
                    }
                } else {
                    filenameBase = title;
                }
                if (parts.size() > 1) {
                    filenameBase += "_pt_" + pf.format(partNumber);
                }
                final Browser hlsBR = new Browser();
                for (final Map<String, Object> videoStream : videoStreams) {
                    // final String origination_date = (String) videoStream.get("origination_date");
                    final List<Map<String, Object>> videoRenditions = (List<Map<String, Object>>) videoStream.get("rendition");
                    for (final Map<String, Object> videoRendition : videoRenditions) {
                        final String method = (String) videoRendition.get("method");
                        if (!method.equalsIgnoreCase("hls")) {
                            throw new DecrypterException("Unsupported streaming method: " + method);
                        }
                        final String hlsMaster = (String) videoRendition.get("src");
                        hlsBR.getPage(hlsMaster);
                        /* 2021-08-05: Only pick best quality for now */
                        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(hlsBR));
                        final DownloadLink dl = this.createDownloadlink(GenericM3u8.createURLForThisPlugin(hlsbest.getDownloadurl()));
                        dl.setAvailable(true);
                        dl._setFilePackage(fp);
                        dl.setFinalFileName(filenameBase + ".mp4");
                        distribute(dl);
                        ret.add(dl);
                        /* 2021-08-05: Only process one videoStream at this moment. */
                        break;
                    }
                    /* Find- and add all compatible subtitle formats (2021-08-05: .vtt only). */
                    final List<Map<String, Object>> transcripts = (List<Map<String, Object>>) videoStream.get("transcript");
                    if (transcripts != null) {
                        for (final Map<String, Object> transcript : transcripts) {
                            final String kind = (String) transcript.get("kind");
                            if (!kind.equalsIgnoreCase("captions")) {
                                continue;
                            }
                            // final String srclang = (String) transcript.get("srclang");
                            final List<Map<String, Object>> subtitleFormats = (List<Map<String, Object>>) transcript.get("typographic");
                            for (final Map<String, Object> subtitleFormat : subtitleFormats) {
                                final String format = (String) subtitleFormat.get("format");
                                final String url = (String) subtitleFormat.get("src");
                                if (format.equalsIgnoreCase("vtt") && !StringUtils.isEmpty(url)) {
                                    final DownloadLink subtitle = this.createDownloadlink("directhttp://" + url);
                                    subtitle.setFinalFileName(filenameBase + ".vtt");
                                    subtitle.setAvailable(true);
                                    subtitle._setFilePackage(fp);
                                    distribute(subtitle);
                                    ret.add(subtitle);
                                }
                            }
                        }
                    }
                }
                if (this.isAbort()) {
                    return ret;
                }
            }
            index++;
            if (this.isAbort()) {
                return ret;
            }
        }
        return ret;
    }

    /** Recursive function to find photoMap inside json. */
    private void findVideoMaps(final ArrayList<Map<String, Object>> maps, final Object o) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            final Object typeO = entrymap.get("type");
            if (typeO != null && typeO instanceof String && typeO.toString().equals("VideoPlayer")) {
                maps.add(entrymap);
            } else {
                for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                    // if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                    // findVideoMaps(maps, entry.getValue());
                    // }
                    findVideoMaps(maps, entry.getValue());
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> objects = (List) o;
            for (final Object arrayo : objects) {
                findVideoMaps(maps, arrayo);
                // if (arrayo instanceof Map || arrayo instanceof List) {
                // findVideoMaps(maps, arrayo);
                // }
            }
            return;
        } else {
            /* No map/list */
            return;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void crawlMtvGermanyPlaylists(final CryptedLink param) {
        fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback to url-packagename */
            fpName = new Regex(param.getCryptedUrl(), "https?://[^/]+/(.+)").getMatch(0);
        }
        List<Object> ressourcelist = null;
        Map<String, Object> entries = null;
        try {
            final String json = this.br.getRegex("window\\.pagePlaylist = (\\[\\{.*?\\}\\])").getMatch(0);
            ressourcelist = (List) JavaScriptEngineFactory.jsonToJavaObject(json);
            for (final Object object : ressourcelist) {
                entries = (Map<String, Object>) object;
                final String path = (String) entries.get("path");
                final String url_mrss = (String) entries.get("mrss");
                final String title = (String) entries.get("title");
                final String subtitle = (String) entries.get("subtitle");
                final String video_token = (String) entries.get("video_token");
                final String mgid = VivaTv.getMGIDOutOfURL(url_mrss);
                if (url_mrss == null || title == null || video_token == null || mgid == null) {
                    throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
                }
                final String contenturl;
                if (path != null) {
                    contenturl = "http://" + this.br.getHost() + path;
                } else {
                    contenturl = param.getCryptedUrl();
                }
                String temp_filename = title;
                if (subtitle != null) {
                    temp_filename += " - " + subtitle;
                }
                temp_filename += VivaTv.default_ext;
                final DownloadLink dl = mgidSingleVideoGetDownloadLink(mgid);
                dl.setLinkID(video_token);
                dl.setName(temp_filename);
                dl.setAvailable(true);
                dl.setContentUrl(contenturl);
                this.decryptedLinks.add(dl);
            }
        } catch (final Throwable e) {
            return;
        }
    }

    private void crawlMtvComPlaylists(final CryptedLink param) {
        fpName = this.br.getRegex("<h3 class=\"h\\-sub3 h\\-feed\">[\t\n\r ]*?<span>([^<>\"]*?)</span>[\t\n\r ]*?</h3>").getMatch(0);
        final String[] entries = this.br.getRegex("(id=\"vid\\d+\">.*?<p class=\"usage\"/>)").getColumn(0);
        if (entries != null && entries.length != 0) {
            for (final String entry : entries) {
                String title = new Regex(entry, "<span class=\"title_container trim_container\">[\t\n\r ]+<span>([^<>]*?)</span>").getMatch(0);
                if (title == null) {
                    title = new Regex(entry, "class=\"song\">([^<>\"]*?)</span>").getMatch(0);
                }
                String url_content = new Regex(entry, "itemprop=\"url\"[\t\n\r ]*?href=\"(/[^<>\"]*?)\"").getMatch(0);
                final String mgid = new Regex(entry, "/uri/(mgid:[^<>\"\\?]+)").getMatch(0);
                if (title == null || mgid == null) {
                    /* This shouldn't happen ... */
                    continue;
                }
                if (url_content != null) {
                    url_content = param.getCryptedUrl();
                } else {
                    url_content = "http://" + this.br.getHost() + url_content;
                }
                final String url_final = getViacomHostUrl(mgid);
                final DownloadLink dl = this.createDownloadlink(url_final);
                if (title != null) {
                    /* Should cover 99% of all cases */
                    title = this.doFilenameEncoding(title);
                    dl.setName(title + default_ext);
                    dl.setAvailable(true);
                }
                dl.setContentUrl(url_content);
                dl.setLinkID(mgid);
                this.decryptedLinks.add(dl);
            }
        }
    }

    private void crawlSouthparkCc(final CryptedLink param) throws IOException, DecrypterException {
        br.getPage(param.getCryptedUrl());
        this.mgid = br.getRegex("data\\-mgid=\"(mgid[^<>\"]*?)\"").getMatch(0);
        if (this.mgid == null) {
            throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
        }
        final String feedURL = String.format(getFEEDURL("southpark.cc.com"), this.mgid);
        br.getPage(feedURL);
        fpName = getFEEDtitle(br.toString());
        if (fpName == null) {
            this.decryptedLinks = null;
            return;
        }
        fpName = new Regex(param.getCryptedUrl(), "episodes/(s\\d{2}e\\d{2})").getMatch(0) + " - " + fpName;
        fpName = Encoding.htmlDecode(fpName.trim());
        crawlFeed(this.br, param);
    }

    private void crawlMtvCom(final CryptedLink param) throws Exception {
        // final String feedURL_plain = this.getFEEDURL("mtv.com");
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            this.decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return;
        }
        if (crawlVevo()) {
            return;
        }
        logger.info("Current link is NO VEVO link");
        String playlist_id = new Regex(param.getCryptedUrl(), "^.+#id=(\\d+)$").getMatch(0);
        if (playlist_id == null) {
            playlist_id = this.br.getRegex("\\.jhtml#id=(\\d+)\"").getMatch(0);
        }
        if (playlist_id != null) {
            /* Playlist */
            this.br.getPage("http://www.mtv.com/global/music/videos/ajax/playlist.jhtml?id=" + playlist_id);
            crawlMtvComPlaylists(param);
        } else {
            vivaUniversalCrawler(param);
        }
    }

    /**
     * This function is able to crawl content of most viacom/mtv websites.
     *
     * @throws Exception
     */
    private void vivaUniversalCrawler(final CryptedLink param) throws Exception {
        if (crawlVevo()) {
            return;
        }
        this.crawlWebsiteJson(param, this.decryptedLinks);
        if (this.decryptedLinks.isEmpty()) {
            crawlDrupal(param);
            crawlMgids(param);
            crawlTriforceManifestFeed();
            crawlMtvGermanyPlaylists(param);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fpName = Encoding.htmlDecode(fpName.trim());
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
    }

    /** In case a video is not hosted by mtv but by vimeo, this function should find- and return that vimeo url. */
    private boolean crawlVevo() {
        final String vevo_ID = br.getRegex("MTVN\\.Player\\.vevoVideoId = \"([A-Za-z0-9]+)\";").getMatch(0);
        if (vevo_ID != null) {
            logger.info("Current link is a VEVO link");
            decryptedLinks.add(createDownloadlink("http://www.vevo.com/watch/" + vevo_ID));
            return true;
        }
        return false;
    }

    private void crawlMgids(final CryptedLink param) throws IOException, DecrypterException {
        final String[] mgids = this.br.getRegex("(" + PATTERN_MGID + ")").getColumn(0);
        if (mgids != null) {
            for (final String mgid : mgids) {
                addMgid(param, mgid);
            }
        }
    }

    /** Finds mgids inside drupal json */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void crawlDrupal(final CryptedLink param) {
        final String playlist_id = br.getRegex("name=\"vimn:entity_uuid\" content=\"([a-z0-9\\-:]*?)\"").getMatch(0);
        final String js = br.getRegex("jQuery\\.extend\\(Drupal\\.settings, (\\{.*?)\\);.*?</script>").getMatch(0);
        if (js == null || playlist_id == null) {
            return;
        }
        try {
            Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(js);
            final Map<String, Object> vimn_video = (Map<String, Object>) entries.get("vimn_video");
            List<Object> resources = null;
            final Object playlist_o = vimn_video.get("playlists");
            final Object embedded_videos_o = vimn_video.get("embedded_videos");
            if (!(playlist_o instanceof List)) {
                /* Playlist */
                entries = (Map<String, Object>) playlist_o;
                entries = (Map<String, Object>) entries.get(playlist_id);
                resources = (List) entries.get("items");
                final Object title_object = entries.get("title");
                if (title_object instanceof String) {
                    fpName = (String) entries.get("title");
                }
                for (final Object pt : resources) {
                    final Map<String, Object> playlistentry = (Map<String, Object>) pt;
                    final String mgid = (String) playlistentry.get("guid");
                    final String partname = (String) playlistentry.get("title");
                    if (mgid == null || partname == null) {
                        continue;
                    }
                    if (fpName == null) {
                        fpName = doFilenameEncoding(partname);
                    }
                    final String final_filename = this.doFilenameEncoding(partname) + this.default_ext;
                    final DownloadLink fina = mgidSingleVideoGetDownloadLink(mgid);
                    fina.setFinalFileName(final_filename);
                    fina.setProperty("decryptedfilename", final_filename);
                    fina.setAvailable(true);
                    fina.setContentUrl(param.getCryptedUrl());
                    decryptedLinks.add(fina);
                }
            }
            if (embedded_videos_o instanceof List) {
                resources = (List) embedded_videos_o;
                for (final Object pt : resources) {
                    final Map<String, Object> playlistentry = (Map<String, Object>) pt;
                    final String mgid = (String) playlistentry.get("video_id");
                    final String partname = (String) playlistentry.get("video_title");
                    final String url = (String) playlistentry.get("video_url");
                    if (mgid == null || partname == null || url == null) {
                        continue;
                    }
                    final String final_filename = this.doFilenameEncoding(partname) + this.default_ext;
                    final DownloadLink fina = mgidSingleVideoGetDownloadLink(mgid);
                    fina.setFinalFileName(final_filename);
                    fina.setProperty("decryptedfilename", final_filename);
                    fina.setAvailable(true);
                    fina.setContentUrl("http://" + this.br.getHost() + url);
                    decryptedLinks.add(fina);
                }
            }
        } catch (final Throwable e) {
        }
    }

    /* 2017-02-22: New - finds- and adds feed URLs --> These will then go back into the decrypter! */
    private void crawlTriforceManifestFeed() {
        try {
            final String json_source = this.br.getRegex("var triforceManifestFeed\\s*?=\\s*?(\\{.*?\\})\\s+").getMatch(0);
            Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json_source);
            Map<String, Object> entries2 = null;
            entries = (Map<String, Object>) entries.get("manifest");
            entries = (Map<String, Object>) entries.get("zones");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                entries2 = (Map<String, Object>) entry.getValue();
                final String url_feed = (String) entries2.get("feed");
                if (url_feed != null) {
                    this.decryptedLinks.add(this.createDownloadlink(url_feed));
                }
            }
        } catch (final Throwable e) {
        }
    }

    /** Validates- and adds mgids. */
    private void addMgid(final CryptedLink param, String mgid) throws IOException, DecrypterException {
        if (mgid == null) {
            return;
        }
        mgid = cleanMgid(mgid);
        /* Skip image-mgids - we don't need them! */
        if (!isValidMgid(mgid)) {
            return;
        }
        if (mgidIsPlaylist(mgid)) {
            /* Episode (maybe with multiple segments) */
            final String feed_url = VivaTv.mgidGetFeedurlForMgid(mgid, this.getHost());
            if (feed_url == null) {
                return;
            }
            final Browser brc = br.cloneBrowser();
            this.br.getPage(feed_url);
            crawlFeed(brc, param);
        } else {
            final DownloadLink dl = mgidSingleVideoGetDownloadLink(mgid);
            if (dl != null) {
                dl.setContentUrl(param.getCryptedUrl());
                this.decryptedLinks.add(dl);
            }
        }
    }

    /** Used to make downloadlink objects out of mgids of which we know they are single videos. */
    private DownloadLink mgidSingleVideoGetDownloadLink(String mgid) {
        /* Additional errorhandling - make sure we only accept valid video content! */
        if (!isValidMgid(mgid) || !mgidIsSingleVideo(mgid)) {
            return null;
        }
        mgid = cleanMgid(mgid);
        final String url_hosterplugin = getViacomHostUrl(mgid);
        final DownloadLink dl = this.createDownloadlink(url_hosterplugin);
        return dl;
    }

    private boolean mgidIsSingleVideo(final String mgid) {
        final String type = VivaTv.mgidGetType(mgid);
        final boolean isVideo = type.equals("video");
        return isVideo;
    }

    private String cleanMgid(String mgid) {
        if (mgid == null) {
            return null;
        }
        mgid = Encoding.htmlDecode(mgid);
        mgid = mgid.replace(" ", "");
        return mgid;
    }

    /** Validates mgids */
    private boolean isValidMgid(final String mgid) {
        if (mgid == null) {
            return false;
        }
        boolean isValidMgid = mgid.matches(PATTERN_MGID);
        if (isValidMgid && mgid.contains(":image:") || mgid.contains(":drupal:")) {
            isValidMgid = false;
        }
        return isValidMgid;
    }

    private boolean mgidIsPlaylist(final String mgid) {
        if (mgid.contains("episode")) {
            return true;
        }
        return false;
    }

    /** General function to decrypt viacom RSS feeds, especially with multiple segments of a single video no matter what their source is. */
    private void crawlFeed(final Browser br, final CryptedLink param) throws DecrypterException {
        final String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
        if (fpName == null) {
            fpName = getMainFEEDTitle(br);
        }
        if (items == null || items.length == 0 || fpName == null) {
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        int counter = -1;
        for (final String item : items) {
            counter++;
            String title = getFEEDtitle(item);
            String item_mgid = new Regex(item, "uri=(mgid:[A-Za-z0-9:\\-\\.]+)").getMatch(0);
            if (item_mgid == null) {
                item_mgid = new Regex(item, "(mgid:[A-Za-z0-9:\\-\\.]+)").getMatch(0);
            }
            if (title == null || item_mgid == null) {
                throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
            }
            /* We don't need the intro - it's always the same! */
            if (counter == 0 && items.length > 1 && title.contains("Intro")) {
                continue;
            }
            title = doFilenameEncoding(title);
            title = title + this.default_ext;
            // final DownloadLink dl = mgidSingleVideoGetDownloadLink(item_mgid);
            final String url_hosterplugin = getViacomHostUrl(item_mgid);
            final DownloadLink dl = this.createDownloadlink(url_hosterplugin);
            dl.setProperty("decryptedfilename", title);
            dl.setProperty("mainlink", param.getCryptedUrl());
            dl.setProperty(VivaTv.PROPERTY_original_host, this.getHost());
            dl.setName(title);
            dl.setAvailable(true);
            dl.setContentUrl(param.getCryptedUrl());
            decryptedLinks.add(dl);
        }
    }

    private String getMainFEEDTitle(final Browser br) {
        String title = getXML("title");
        if (title != null) {
            title = doEncoding(title);
        }
        return title;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "[^<]*?><\\!\\[CDATA\\[([^<>]*?)\\]\\]><").getMatch(0);
        }
        return result;
    }

    private String getFEEDURL(final String domain) {
        return VivaTv.feedURLs.get(domain);
    }

    private String getEMBEDURL(final String domain) {
        return VivaTv.embedURLs.get(domain);
    }

    private String getFEEDtitle(final String source) {
        return VivaTv.feedGetTitle(source);
    }

    private String doEncoding(final String data) {
        return VivaTv.doEncoding(data);
    }

    private String doFilenameEncoding(final String filename) {
        return VivaTv.doFilenameEncoding(this, filename);
    }

    private String getViacomHostUrl(final String mgid) {
        return hosterplugin_url_viacom_mgid + mgid;
    }
}
