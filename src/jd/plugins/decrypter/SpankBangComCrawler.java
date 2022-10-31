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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.SpankBangCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SpankBangComCrawler extends PluginForDecrypt {
    public SpankBangComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "spankbang.com", "spankbang.party" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:([a-z]{2}|www)\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z0-9]+/video/\\?quality=[\\w\\d]+|[a-z0-9]+/(?:video|embed)/([^/]+)?|[a-z0-9\\-]+/playlist/\\w+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void init() {
        super.init();
        /** 2021-07-27: Important else we'll run into Cloudflare Rate-Limit prohibition after about 250 requests! */
        Browser.setRequestIntervalLimitGlobal(getHost(), 3000);
    }

    private static final String DOMAIN           = "spankbang.com";
    /** Settings stuff */
    private static final String FASTLINKCHECK    = "FASTLINKCHECK";
    private static final String ALLOW_BEST       = "ALLOW_BEST";
    private static final String ALLOW_240p       = "ALLOW_240p";
    private static final String ALLOW_320p       = "ALLOW_320p";
    private static final String ALLOW_480p       = "ALLOW_480p";
    private static final String ALLOW_720p       = "ALLOW_720p";
    private static final String ALLOW_1080p      = "ALLOW_1080p";
    private static final String ALLOW_4k         = "ALLOW_4k";
    private SpankBangCom        plugin           = null;
    private final String        PATTERN_PLAYLIST = "https?://[^/]+/([a-z0-9]+)/playlist/(\\w+)";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        /* www = English language */
        br.setCookie("spankbang.com", "language", "www");
        br.getHeaders().put("Accept-Language", "en");
        return br;
    }

    private void getPage(final String page) throws Exception {
        plugin.getPage(page);
    }

    private void prepCrawlerplugin() throws PluginException {
        if (plugin == null) {
            plugin = (SpankBangCom) getNewPluginForHostInstance(this.getHost());
            if (plugin == null) {
                throw new IllegalStateException("Plugin not found!");
            }
        }
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        prepBR(br);
        prepCrawlerplugin();
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            /* Login whenever account is available so we can e.g. handle private videos too. */
            plugin.login(account, false);
        }
        if (param.getCryptedUrl().matches(PATTERN_PLAYLIST)) {
            return this.crawlPlaylist(param);
        } else {
            return crawlSingleVideo(param);
        }
    }

    private ArrayList<DownloadLink> crawlPlaylist(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        this.checkErrors(br);
        final Regex playlistInfo = new Regex(param.getCryptedUrl(), PATTERN_PLAYLIST);
        final String playlistID = playlistInfo.getMatch(0);
        final String playlistSlug = playlistInfo.getMatch(1);
        if (playlistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(playlistSlug);
        final String totalNumberofItemsStr = br.getRegex("(?i)records in total\\s*<b>(\\d+)").getMatch(0);
        if (totalNumberofItemsStr == null) {
            logger.warning("Failed to find totalNumberofItems");
        }
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        do {
            final String[] urls = br.getRegex("\"(/" + playlistID + "-[a-z0-9]+/playlist/[^\"]+)\"").getColumn(0);
            int numberofNewItems = 0;
            for (String url : urls) {
                if (!dupes.add(url)) {
                    continue;
                }
                numberofNewItems++;
                url = br.getURL(url).toString();
                final DownloadLink video = this.createDownloadlink(url);
                video._setFilePackage(fp);
                ret.add(video);
                distribute(video);
            }
            logger.info("Crawled page " + 1 + " | Found items so far: " + ret.size() + " of " + totalNumberofItemsStr);
            final String nextPageURL = br.getRegex("<a href=\"(/[^\"]+)\">" + (page + 1) + "</a>").getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (numberofNewItems == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (nextPageURL == null) {
                logger.info("Stopping because: Reached last page");
                break;
            } else {
                page++;
                br.getPage(nextPageURL);
            }
        } while (true);
        return ret;
    }

    /** Crawls single videos and single videos that are parts of a playlist. */
    private ArrayList<DownloadLink> crawlSingleVideo(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean fastcheck = cfg.getBooleanProperty(FASTLINKCHECK, true);
        final String addedurl = param.getCryptedUrl().replace("/embed/", "/video/");
        br.setAllowedResponseCodes(new int[] { 503 });
        getPage(addedurl);
        checkErrors(br);
        if (isPrivate(this.br)) {
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            logger.info("Server error 503: Cannot crawl new URLs at the moment");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        /* Decrypt qualities START */
        /* 2020-05-11: Prefer filenames from inside URL as they are always 'good'. */
        String title = br.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta\\s*name\\s*=\\s*\"twitter:description\"\\s*content\\s*=\\s*\"(?:\\s*Watch\\s*)?([^<>\"]+)\\s+on SpankBang now").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h1\\s*title\\s*=\\s*\"(.*?)\"").getMatch(0);
                if (title == null) {
                    title = new Regex(addedurl, "/video/(.+)").getMatch(0);
                    title = Encoding.urlDecode(title, false);
                }
            }
        }
        String videoID = br.getRegex("\"embedUrl\"\\s*:\\s*\"https?://[^/]+/([a-z0-9]+)/embed").getMatch(0);
        if (videoID == null) {
            videoID = getFid(addedurl);
        }
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final LinkedHashMap<String, String> foundQualities = findQualities(this.br, addedurl);
        if (foundQualities == null || foundQualities.size() == 0 || title == null) {
            throw new DecrypterException("Decrypter broken for link: " + addedurl);
        }
        title = Encoding.htmlDecode(title.trim());
        fp.setName(title);
        /* Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        boolean q240p = cfg.getBooleanProperty(ALLOW_240p, true);
        boolean q320p = cfg.getBooleanProperty(ALLOW_320p, true);
        boolean q480p = cfg.getBooleanProperty(ALLOW_480p, true);
        boolean q720p = cfg.getBooleanProperty(ALLOW_720p, true);
        boolean q1080p = cfg.getBooleanProperty(ALLOW_1080p, true);
        boolean q4k = cfg.getBooleanProperty(ALLOW_4k, true);
        if (!q240p && !q320p && !q480p && !q720p && !q1080p) {
            // user has made error and disabled them all, so we will treat as all enabled.
            q240p = true;
            q320p = true;
            q480p = true;
            q720p = true;
            q1080p = true;
            q4k = true;
        }
        final boolean best = cfg.getBooleanProperty(ALLOW_BEST, true);
        // needs to be in reverse order
        if (q4k) {
            selectedQualities.add("4k");
        }
        if (q1080p) {
            selectedQualities.add("1080p");
        }
        if (q720p) {
            selectedQualities.add("720p");
        }
        if (q480p) {
            selectedQualities.add("480p");
        }
        if (q320p) {
            selectedQualities.add("320p");
        }
        if (q240p) {
            selectedQualities.add("240p");
        }
        final String predefinedVariant = UrlQuery.parse(param.getCryptedUrl()).get("quality");
        for (final String selectedQualityValue : selectedQualities) {
            // if quality marker is in the url. skip all others
            if (predefinedVariant != null && !predefinedVariant.equalsIgnoreCase(selectedQualityValue)) {
                continue;
            }
            final String directlink = foundQualities.get(selectedQualityValue);
            if (directlink != null) {
                final String finalname = title + "_" + selectedQualityValue + ".mp4";
                final DownloadLink video = createDownloadlink("http://spankbangdecrypted.com/" + UniqueAlltimeID.create());
                video.setFinalFileName(finalname);
                // dl.setContentUrl(br.getURL());
                if (fastcheck) {
                    video.setAvailable(true);
                }
                video.setLinkID("spankbangcom_" + videoID + "_" + selectedQualityValue);
                video.setProperty("plain_filename", finalname);
                video.setProperty("plain_directlink", directlink);
                video.setProperty("mainlink", addedurl);
                video.setProperty("quality", selectedQualityValue);
                fp.add(video);
                ret.add(video);
                if (best) {
                    break;
                }
            }
        }
        if (ret.size() == 0) {
            logger.info(DOMAIN + ": None of the selected qualities were found");
        }
        return ret;
    }

    private void checkErrors(final Browser br) throws PluginException {
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static LinkedHashMap<String, String> findQualities(final Browser br, final String source_url) throws DecrypterException, PluginException, IOException {
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        final String[] knownQualities = new String[] { "4k", "1080p", "720p", "480p", "320p", "240p" };
        // final String fid = getFid(source_url);
        /* 2021-06-10: API no longer required/available */
        final boolean useAPI = false;
        if (useAPI) {
            final String dataStreamKey = br.getRegex("data-streamkey\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (dataStreamKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String x_csrftoken = br.getCookie(br.getURL(), "sb_csrf_session");
            Request request = br.createPostRequest("/api/videos/stream", "data=0&id=" + dataStreamKey + "&sb_csrf_session=" + x_csrftoken);
            request.getHeaders().put("accept", "application/json, text/javascript, */*; q=0.01");
            request.getHeaders().put("x-requested-with", "XMLHttpRequest");
            if (x_csrftoken != null) {
                request.getHeaders().put("x-csrftoken", x_csrftoken);
            }
            final Browser brc = br.cloneBrowser();
            final String page = brc.getPage(request);
            if (page.matches("(?s)^\\s*\\{.*") && page.matches("(?s).*\\}\\s*$")) {
                final Map<String, Object> map = JSonStorage.restoreFromString(page, TypeRef.HASHMAP);
                final String stream_url_m3u8 = String.valueOf(map.get("m3u8"));
                for (final String quality : knownQualities) {
                    final String qualityID = getQuality(quality);
                    final Object entry = map.get(quality);
                    String value = null;
                    if (entry instanceof String) {
                        value = (String) entry;
                    } else if (entry instanceof List && ((List) entry).size() != 0) {
                        value = ((List<String>) entry).get(0);
                    }
                    System.out.println("value: " + value);
                    if (StringUtils.isEmpty(value) && StringUtils.isNotEmpty(stream_url_m3u8)) {
                        final String one_stream_url_m3u8 = new Regex(stream_url_m3u8, "(http.*?d=\\d)").getMatch(0);
                        // System.out.println("one_stream_url_m3u8: " + one_stream_url_m3u8);
                        final String page_m3u8 = br.getPage(one_stream_url_m3u8);
                        final String[] single_url_m3u8s = new Regex(page_m3u8, "(http.*?d=\\d)\\s").getColumn(0);
                        // System.out.println("single_url_m3u8s.length: " + single_url_m3u8s.length);
                        for (final String single_url_m3u8 : single_url_m3u8s) {
                            // System.out.println("single_url_m3u8: " + single_url_m3u8);
                            if (single_url_m3u8.contains(quality)) {
                                value = single_url_m3u8;
                                System.out.println("value: " + value);
                            }
                        }
                    } else {
                        // continue;
                    }
                    if (StringUtils.isNotEmpty(value)) {
                        foundQualities.put(qualityID, value);
                    }
                }
            } else {
                // final String streamkey = br.getRegex("var stream_key = \\'([^<>\"]*?)\\'").getMatch(0);
                for (final String q : knownQualities) {
                    final String quality = getQuality(q);
                    // final String directlink = "http://spankbang.com/_" + fid + "/" + streamkey + "/title/" + quality + "__mp4";
                    final String directlink = PluginJSonUtils.getJson(br, "stream_url_" + q);
                    if (StringUtils.isEmpty(directlink)) {
                        continue;
                    }
                    foundQualities.put(quality, directlink);
                }
            }
        } else {
            /* 2021-06-10 */
            final String js = br.getRegex("var stream_data = (\\{.*?\\})").getMatch(0);
            try {
                final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(js);
                for (final String q : knownQualities) {
                    final Object qO = entries.get(q);
                    if (qO == null) {
                        continue;
                    }
                    final List<Object> temp = (List<Object>) qO;
                    if (temp.isEmpty()) {
                        continue;
                    }
                    final String directlink = (String) temp.get(0);
                    if (StringUtils.isEmpty(directlink)) {
                        continue;
                    }
                    foundQualities.put(q, directlink);
                }
            } catch (final Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return foundQualities;
    }

    private static String parseSingleQuality(String source) {
        if (source == null) {
            return null;
        }
        /* 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript */
        if (source.contains("240p")) {
            return "low";
        } else if (source.contains("480p")) {
            return "medium";
        } else if (source.contains("hi")) {
            return "medium";
        } else if (source.contains("720p")) {
            return "high";
        } else if (source.contains("1080p")) {
            return "super";
        }
        return null;
    }

    public static String getFid(final String source_url) {
        return new Regex(source_url, "spankbang\\.com/([a-z0-9]+)/video/").getMatch(0);
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*this video is (no longer available|private|under review)|>\\s*este vídeo já não está disponível|video_removed_page");
    }

    public static boolean isPrivate(final Browser br) {
        return br.containsHTML("(?i)>\\s*(this video is private\\.?|Dieses Video ist privat|Este vídeo es privado)");
    }

    /**
     * 'super = 1080p', 'high = 720p', 'medium = 480p', 'low = 240p' they do this in javascript
     *
     * @param q
     * @return
     * @throws DecrypterException
     */
    public static String getQuality(final String q) throws PluginException {
        if ("4k".equalsIgnoreCase(q)) {
            return "4k";
        } else if ("super".equalsIgnoreCase(q) || "1080p".equalsIgnoreCase(q)) {
            return "1080p";
        } else if ("high".equalsIgnoreCase(q) || "720p".equalsIgnoreCase(q)) {
            return "720p";
        } else if ("medium".equalsIgnoreCase(q) || "480p".equalsIgnoreCase(q)) {
            return "480p";
        } else if ("320p".equalsIgnoreCase(q)) {
            return "320p";
        } else if ("low".equalsIgnoreCase(q) || "240p".equalsIgnoreCase(q)) {
            return "240p";
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /**
     * JD2 CODE: DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}