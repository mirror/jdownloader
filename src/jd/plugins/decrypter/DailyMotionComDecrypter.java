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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

//Decrypts embedded videos from dailymotion
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "https?://(?:www\\.)?dailymotion\\.com/.+" })
public class DailyMotionComDecrypter extends PluginForDecrypt {
    public DailyMotionComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String                          videoSource       = null;
    /**
     * @ 1hd1080URL or stream_h264_hd1080_url [1920x1080]
     *
     * @ 2 hd720URL or stream_h264_hd_url [1280x720]
     *
     * @ 3 hqURL or stream_h264_hq_url [848x480]
     *
     * @ 4 sdURL or stream_h264_url [512x384]
     *
     * @ 5 ldURL or video_url or stream_h264_ld_url [320x240]
     *
     * @ 6 video_url or rtmp
     *
     * @ 7 hds
     *
     * @String[] = {"Direct download url", "filename, if available before quality selection"}
     */
    private LinkedHashMap<String, String[]> foundQualities    = new LinkedHashMap<String, String[]>();
    private String                          filename          = null;
    private String                          parameter         = null;
    private static final String             ALLOW_BEST        = "ALLOW_BEST";
    private static final String             ALLOW_OTHERS      = "ALLOW_OTHERS";
    public static final String              ALLOW_AUDIO       = "ALLOW_AUDIO";
    private static final String             TYPE_PLAYLIST     = "https?://(?:www\\.)?dailymotion\\.com/playlist/[A-Za-z0-9\\-_]+(?:/\\d+)?.*?";
    private static final String             TYPE_USER         = "https?://(?:www\\.)?dailymotion\\.com/user/[A-Za-z0-9_\\-]+/\\d+";
    private static final String             TYPE_USER_SEARCH  = "https?://(?:www\\.)?dailymotion\\.com/.*?/user/[^/]+/search/[^/]+/\\d+";
    private static final String             TYPE_VIDEO        = "https?://(?:www\\.)?dailymotion\\.com/((?:embed/)?video/[^/]+|swf(?:/video)?/[^/]+)";
    /** API limits for: https://developer.dailymotion.com/api#graph-api */
    private static final short              api_limit_items   = 100;
    private static final short              api_limit_pages   = 100;
    public final static boolean             defaultAllowAudio = true;
    private ArrayList<DownloadLink>         decryptedLinks    = new ArrayList<DownloadLink>();
    private boolean                         acc_in_use        = false;
    private static Object                   ctrlLock          = new Object();

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        parameter = param.toString().replace("embed/video/", "video/").replaceAll("\\.com/swf(/video)?/", ".com/video/").replace("http://", "https://");
        br.setFollowRedirects(true);
        jd.plugins.hoster.DailyMotionCom.prepBrowser(this.br);
        synchronized (ctrlLock) {
            /* Login if account available */
            final PluginForHost dailymotionHosterplugin = JDUtilities.getPluginForHost("dailymotion.com");
            Account aa = AccountController.getInstance().getValidAccount(dailymotionHosterplugin);
            if (aa != null) {
                try {
                    ((jd.plugins.hoster.DailyMotionCom) dailymotionHosterplugin).login(aa, this.br);
                    acc_in_use = true;
                } catch (final PluginException e) {
                    logger.info("Account seems to be invalid -> Continuing without account!");
                }
            }
            /* Login end... */
            br.getPage(parameter);
            /* 404 */
            if (br.containsHTML("(<title>Dailymotion \\– 404 Not Found</title>|url\\(/images/404_background\\.jpg)") || this.br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* 403 */
            if (br.containsHTML("class=\"forbidden\">Access forbidden</h3>|>You don\\'t have permission to access the requested URL") || this.br.getHttpConnection().getResponseCode() == 403) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* 410 */
            if (br.getHttpConnection().getResponseCode() == 410) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            /* video == 'video_item', user == 'user_home' */
            final String route_name = PluginJSonUtils.getJson(this.br, "route_name");
            if (parameter.matches(TYPE_PLAYLIST)) {
                decryptPlaylist();
            } else if (parameter.matches(TYPE_USER) || "user_home".equalsIgnoreCase(route_name)) {
                decryptUser();
            } else if (parameter.matches(TYPE_VIDEO)) {
                decryptSingleVideo(decryptedLinks);
            } else if (parameter.matches(TYPE_USER_SEARCH)) {
                decryptUserSearch();
            } else {
                logger.info("Unsupported linktype: " + parameter);
                return decryptedLinks;
            }
        }
        if (decryptedLinks == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /**
     * Crawls all videos of a user. In some cases it is not possible to crawl all videos due to website- AND API limitations (both have the
     * same limits).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void decryptUser() throws Exception {
        logger.info("Decrypting user: " + parameter);
        String username = new Regex(parameter, "dailymotion\\.com/user/([A-Za-z0-9\\-_]+)").getMatch(0);
        if (username == null) {
            username = new Regex(parameter, "dailymotion\\.com/([A-Za-z0-9_\\-]+)").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        boolean has_more = false;
        int page = 0;
        do {
            page++;
            if (this.isAbort()) {
                logger.info("Decrypt process aborted by user on page " + page);
                return;
            }
            final String json = this.br.cloneBrowser().getPage("https://api.dailymotion.com/user/" + username + "/videos?limit=" + api_limit_items + "&page=" + page);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            has_more = ((Boolean) entries.get("has_more")).booleanValue();
            final ArrayList<Object> list = (ArrayList) entries.get("list");
            for (final Object video_o : list) {
                entries = (LinkedHashMap<String, Object>) video_o;
                final String videoid = (String) entries.get("id");
                if (videoid == null) {
                    logger.warning("Decrypter failed: " + parameter);
                    decryptedLinks = null;
                    return;
                }
                final DownloadLink dl = this.createDownloadlink(br.getURL("//www.dailymotion.com/video/" + videoid).toString());
                dl._setFilePackage(fp);
                this.decryptedLinks.add(dl);
                distribute(dl);
            }
        } while (has_more && page <= api_limit_pages);
        if (decryptedLinks == null) {
            logger.warning("Decrypter failed: " + parameter);
            decryptedLinks = null;
            return;
        }
    }

    private void decryptPlaylist() throws Exception {
        final ArrayList<String> dupelist = new ArrayList<String>();
        logger.info("Decrypting playlist: " + parameter);
        final Regex info = br.getRegex("class=\"name\">([^<>\"]*?)</a> \\| (\\d+(,\\d+)?) Videos?");
        String username = info.getMatch(0);
        if (username == null) {
            username = br.getRegex("<meta name=\"author\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (username == null) {
            username = PluginJSonUtils.getJsonValue(this.br, "playlist_owner_login");
        }
        String fpName = br.getRegex("<div id=\"playlist_name\">([^<>\"]*?)</div>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<div class=\"page\\-title mrg\\-btm\\-sm\">([^<>\"]*?)</div>").getMatch(0);
        }
        if (fpName == null) {
            fpName = br.getRegex("\"playlist_title\":\"([^<>\"]*?)\"").getMatch(0);
        }
        if (fpName == null) {
            fpName = new Regex(parameter, "dailymotion.com/playlist/([^/]+)").getMatch(0);
        }
        fpName = Encoding.htmlDecode(fpName.trim());
        String videosNum = info.getMatch(1);
        final String videosnum_text = br.getRegex("class=\"link\\-on\\-hvr\"(.*?)<span>").getMatch(0);
        if (videosNum == null && videosnum_text != null) {
            videosNum = new Regex(videosnum_text, "(\\d+(,\\d+)?) Videos?").getMatch(0);
        }
        if (videosNum == null) {
            /* Empty playlist site */
            if (!br.containsHTML("\"watchlaterAdd\"")) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                dl.setFinalFileName(fpName);
                decryptedLinks.add(dl);
                return;
            }
            logger.warning("Decrypter failed: " + parameter);
            decryptedLinks = null;
            return;
        }
        final int videoCount = Integer.parseInt(videosNum.replace(",", ""));
        if (videoCount == 0) {
            /* User has 0 videos */
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(username);
            decryptedLinks.add(dl);
            return;
        }
        String desiredPage = new Regex(parameter, "playlist/[A-Za-z0-9]+_[A-Za-z0-9\\-_]+/(\\d+)").getMatch(0);
        if (desiredPage == null) {
            desiredPage = "1";
        }
        boolean parsePageOnly = false;
        if (Integer.parseInt(desiredPage) != 1) {
            parsePageOnly = true;
        }
        final BigDecimal bd = new BigDecimal((double) videoCount / 18);
        final int pagesNum = bd.setScale(0, BigDecimal.ROUND_UP).intValue();
        int currentPage = Integer.parseInt(desiredPage);
        final String base_link = "https://www.dailymotion.com/playlist/" + new Regex(parameter, "/playlist/([^/]+)").getMatch(0);
        do {
            if (this.isAbort()) {
                logger.info("Decrypt process aborted by user on page " + currentPage + " of " + pagesNum);
                return;
            }
            final String nextpage = base_link + "/" + currentPage;
            logger.info("Decrypting page: " + nextpage);
            br.getPage(nextpage);
            final String[] videos = br.getRegex("href=\"(/video/[^/]+)").getColumn(0);
            if (videos == null || videos.length == 0) {
                logger.info("Found no videos on page " + currentPage + " -> Stopping decryption");
                break;
            }
            for (final String videolink : videos) {
                if (!dupelist.contains(videolink)) {
                    final DownloadLink fina = createDownloadlink(br.getURL(videolink).toString());
                    distribute(fina);
                    decryptedLinks.add(fina);
                }
                logger.info("Decrypted page " + currentPage + " of " + pagesNum);
                logger.info("Found " + videos.length + " links on current page");
                logger.info("Found " + decryptedLinks.size() + " of total " + videoCount + " links already...");
                dupelist.add(videolink);
                currentPage++;
            }
        } while (decryptedLinks.size() < videoCount && !parsePageOnly);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter failed: " + parameter);
            decryptedLinks = null;
            return;
        }
    }

    private void decryptUserSearch() throws Exception {
        int pagesNum = 1;
        final String[] page_strs = this.br.getRegex("class=\"foreground2 inverted-link-on-hvr\"> ?(\\d+)</a>").getColumn(0);
        if (page_strs != null) {
            for (final String page_str : page_strs) {
                final int page_int = Integer.parseInt(page_str);
                if (page_int > pagesNum) {
                    pagesNum = page_int;
                }
            }
        }
        final String main_search_url = new Regex(parameter, "(.+/)\\d+$").getMatch(0);
        final String username = new Regex(parameter, "/user/([^/]+)/").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        String desiredPage = new Regex(parameter, "(\\d+)$").getMatch(0);
        if (desiredPage == null) {
            desiredPage = "1";
        }
        boolean parsePageOnly = false;
        if (Integer.parseInt(desiredPage) != 1) {
            parsePageOnly = true;
        }
        int currentPage = Integer.parseInt(desiredPage);
        do {
            if (this.isAbort()) {
                logger.info("Decrypt process aborted by user on page " + currentPage + " of " + pagesNum);
                return;
            }
            logger.info("Decrypting page " + currentPage + " / " + pagesNum);
            br.getPage(main_search_url + currentPage);
            final String[] videos = br.getRegex("<a href=\"(/video/[^<>\"]*?)\" class=\"link\"").getColumn(0);
            if (videos == null || videos.length == 0) {
                logger.info("Found no videos on page " + currentPage + " -> Stopping decryption");
                break;
            }
            for (final String videolink : videos) {
                final DownloadLink fina = createDownloadlink(br.getURL(videolink).toString());
                fp.add(fina);
                distribute(fina);
                decryptedLinks.add(fina);
            }
            logger.info("Decrypted page " + currentPage + " of " + pagesNum);
            logger.info("Found " + videos.length + " links on current page");
            currentPage++;
        } while (currentPage <= pagesNum && !parsePageOnly);
        if (this.decryptedLinks.size() == 0) {
            logger.info("Found nothing - user probably entered invalid search term(s)");
        }
    }

    private String videoId     = null;
    private String channelName = null;
    private long   date        = 0;

    @SuppressWarnings("deprecation")
    protected void decryptSingleVideo(ArrayList<DownloadLink> decryptedLinks) throws Exception {
        logger.info("Decrypting single video: " + parameter);
        // We can't download live streams
        if (br.containsHTML("DMSTREAMMODE=live")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("dailymotion.com/", "dailymotiondecrypted.com/"));
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return;
        }
        /** Decrypt start */
        /** Decrypt external links START */
        String externID = br.getRegex("player\\.hulu\\.com/express/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.hulu.com/watch/" + externID));
            return;
        }
        externID = br.getRegex("name=\"movie\" value=\"(https?://(www\\.)?embed\\.5min\\.com/\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(https?://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        /** Decrypt external links END */
        /** Find videolinks START */
        videoId = new Regex(parameter, "dailymotion\\.com/video/([a-z0-9]+)").getMatch(0);
        channelName = br.getRegex("\"owner\":\"([^<>\"]*?)\"").getMatch(0);
        String strdate = br.getRegex("property=\"video:release_date\" content=\"([^<>\"]*?)\"").getMatch(0);
        filename = br.getRegex("<meta itemprop=\"name\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        }
        videoSource = getVideosource(this.br);
        LinkedHashMap<String, Object> json = null;
        // channel might not be present above, but is within videoSource
        if (videoSource != null && channelName == null) {
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(videoSource);
            channelName = (String) JavaScriptEngineFactory.walkJson(json, "metadata/owner/username");
        }
        if (videoSource == null || filename == null || videoId == null || channelName == null || strdate == null) {
            logger.warning("Decrypter failed: " + parameter);
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(new Regex(parameter, "dailymotion\\.com/(.+)").getMatch(0));
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return;
        }
        /* Fix date */
        strdate = strdate.replace("T", "").replace("+", "GMT");
        date = TimeFormatter.getMilliSeconds(strdate, "yyyy-MM-ddHH:mm:ssz", Locale.ENGLISH);
        filename = Encoding.htmlDecode(filename.trim()).replace(":", " - ").replaceAll("/|<|>", "");
        if (new Regex(videoSource, "(Dein Land nicht abrufbar|this content is not available for your country|This video has not been made available in your country by the owner|\"Video not available due to geo\\-restriction)").matches()) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("Geo restricted video - " + filename + ".mp4");
            dl.setProperty("countryblock", true);
            decryptedLinks.add(dl);
            return;
        } else if (new Regex(videoSource, "\"title\":\"Video geo\\-restricted by the owner").matches()) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("Geo-Restricted by owner - " + filename + ".mp4");
            decryptedLinks.add(dl);
        } else if (new Regex(videoSource, "(his content as suitable for mature audiences only|You must be logged in, over 18 years old, and set your family filter OFF, in order to watch it)").matches() && !acc_in_use) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(filename + ".mp4");
            dl.setProperty("registeredonly", true);
            decryptedLinks.add(dl);
            return;
        } else if (new Regex(videoSource, "\"message\":\"Publication of this video is in progress").matches()) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("Publication of this video is in progress - " + filename + ".mp4");
            decryptedLinks.add(dl);
            return;
        } else if (new Regex(videoSource, "\"encodingMessage\":\"Encoding in progress\\.\\.\\.\"").matches()) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("Encoding in progress - " + filename + ".mp4");
            decryptedLinks.add(dl);
            return;
        } else if (new Regex(videoSource, "\"title\":\"Channel offline\\.\"").matches()) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName("Channel offline - " + filename + ".mp4");
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        /** Decrypt subtitles if available */
        String subsource = new Regex(videoSource, "\"recorded\",(.*?\\}\\})").getMatch(0);
        if (subsource != null) {
            subsource = subsource.replace("\\/", "/");
            final String[] subtitles = new Regex(subsource, "\"(https?://static\\d+(-ssl)?\\.dmcdn\\.net/static/video/\\d+/\\d+/\\d+:subtitle_[a-z]{1,4}\\.srt(?:\\?\\d+)?)\"").getColumn(0);
            if (subtitles != null && subtitles.length != 0) {
                final FilePackage fpSub = FilePackage.getInstance();
                fpSub.setName(filename + "_Subtitles");
                for (final String subtitle : subtitles) {
                    final DownloadLink dl = createDownloadlink(br.getURL("//dailymotiondecrypted.com/video/" + videoId).toString());
                    dl.setContentUrl(parameter);
                    final String language = new Regex(subtitle, ".*?\\d+:subtitle_(.{1,4}).srt.*?").getMatch(0);
                    String qualityname = "subtitle";
                    if (language != null) {
                        qualityname += "_" + language;
                    }
                    dl.setProperty("directlink", subtitle);
                    dl.setProperty("type_subtitle", true);
                    dl.setProperty("qualityname", qualityname);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("plain_videoname", filename);
                    dl.setProperty("plain_ext", ".srt");
                    dl.setProperty("plain_videoid", videoId);
                    dl.setProperty("plain_channel", channelName);
                    dl.setProperty("plain_date", Long.toString(date));
                    dl.setLinkID("dailymotioncom" + videoId + "_" + qualityname);
                    final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(dl);
                    dl.setName(formattedFilename);
                    fpSub.add(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        foundQualities = findVideoQualities(this.br, parameter, videoSource);
        if (foundQualities.isEmpty() && decryptedLinks.size() == 0) {
            logger.warning("Found no quality for link: " + parameter);
            decryptedLinks = null;
            return;
        }
        /** Find videolinks END */
        /** Pick qualities, selected by the user START */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig("dailymotion.com");
        final boolean best = cfg.getBooleanProperty(ALLOW_BEST, false);
        boolean noneSelected = true;
        for (final String quality : new String[] { "7", "6", "5", "4", "3", "2", "1" }) {
            if (cfg.getBooleanProperty("ALLOW_" + quality, true)) {
                noneSelected = false;
                break;
            }
        }
        for (final String quality : new String[] { "7", "6", "5", "4", "3", "2", "1" }) {
            if (foundQualities.containsKey(quality) && (best || noneSelected || cfg.getBooleanProperty("ALLOW_" + quality, true))) {
                selectedQualities.add(quality);
                if (best) {
                    break;
                }
            }
        }
        for (final String selectedQuality : selectedQualities) {
            final DownloadLink dl = setVideoDownloadlink(this.br, foundQualities, selectedQuality);
            if (dl == null) {
                continue;
            }
            dl.setContentUrl(parameter);
            fp.add(dl);
            decryptedLinks.add(dl); // Needed only for the "if" below.
        }
        /** Pick qualities, selected by the user END */
        if (decryptedLinks.size() == 0) {
            logger.info("None of the selected qualities were found, decrypting done...");
            return;
        }
    }

    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String[]> findVideoQualities(final Browser br, final String parameter, String videosource) throws Exception {
        final LinkedHashMap<String, String[]> QUALITIES = new LinkedHashMap<String, String[]>();
        final String[][] qualities = { { "hd1080URL", "5" }, { "hd720URL", "4" }, { "hqURL", "3" }, { "sdURL", "2" }, { "ldURL", "1" }, { "video_url", "6" } };
        for (final String quality[] : qualities) {
            final String qualityName = quality[0];
            final String qualityNumber = quality[1];
            final String currentQualityUrl = PluginJSonUtils.getJsonValue(videosource, qualityName);
            if (currentQualityUrl != null) {
                final String[] dlinfo = new String[4];
                dlinfo[0] = currentQualityUrl;
                dlinfo[1] = null;
                dlinfo[2] = qualityName;
                dlinfo[3] = qualityNumber;
                QUALITIES.put(qualityNumber, dlinfo);
            }
        }
        if (QUALITIES.isEmpty() && videosource.startsWith("{\"context\"")) {
            /* "New" player July 2015 */
            try {
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(videosource);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "metadata/qualities");
                /* TODO: Maybe add HLS support in case it gives us more/other formats/qualities */
                final String[][] qualities_2 = { { "2160", "7" }, { "1440", "6" }, { "1080", "5" }, { "720", "4" }, { "480", "3" }, { "380", "2" }, { "240", "1" } };
                for (final String quality[] : qualities_2) {
                    final String qualityName = quality[0];
                    final String qualityNumber = quality[1];
                    final Object jsono = entries.get(qualityName);
                    final String currentQualityUrl = (String) JavaScriptEngineFactory.walkJson(jsono, "{0}/url");
                    if (currentQualityUrl != null) {
                        final String[] dlinfo = new String[4];
                        dlinfo[0] = currentQualityUrl;
                        dlinfo[1] = null;
                        dlinfo[2] = qualityName;
                        dlinfo[3] = qualityNumber;
                        QUALITIES.put(qualityNumber, dlinfo);
                    }
                }
            } catch (final Throwable e) {
            }
        }
        // List empty or only 1 link found -> Check for (more) links
        if (QUALITIES.isEmpty() || QUALITIES.size() == 1) {
            final String manifestURL = PluginJSonUtils.getJsonValue(videosource, "autoURL");
            if (manifestURL != null) {
                /** HDS */
                final String[] dlinfo = new String[4];
                dlinfo[0] = manifestURL;
                dlinfo[1] = "hds";
                dlinfo[2] = "autoURL";
                dlinfo[3] = "8";
                QUALITIES.put("8", dlinfo);
            }
            // Try to avoid HDS
            br.getPage("https://www.dailymotion.com/embed/video/" + new Regex(parameter, "([A-Za-z0-9\\-_]+)$").getMatch(0));
            // 19.09.2014
            videosource = br.getRegex("(\"stream_.*)\"swf_url\":").getMatch(0);
            if (videosource == null) {
                // old version. did not work for me today (19.09.2014)
                videosource = br.getRegex("var info = \\{(.*?)\\},").getMatch(0);
            }
            if (videosource != null) {
                videosource = Encoding.htmlDecode(videosource).replace("\\", "");
                final String[][] embedQualities = { { "stream_h264_ld_url", "5" }, { "stream_h264_url", "4" }, { "stream_h264_hq_url", "3" }, { "stream_h264_hd_url", "2" }, { "stream_h264_hd1080_url", "1" } };
                for (final String quality[] : embedQualities) {
                    final String qualityName = quality[0];
                    final String qualityNumber = quality[1];
                    final String currentQualityUrl = PluginJSonUtils.getJsonValue(videosource, qualityName);
                    if (currentQualityUrl != null) {
                        final String[] dlinfo = new String[4];
                        dlinfo[0] = currentQualityUrl;
                        dlinfo[1] = null;
                        dlinfo[2] = qualityName;
                        dlinfo[3] = qualityNumber;
                        QUALITIES.put(qualityNumber, dlinfo);
                    }
                }
            }
        }
        return QUALITIES;
    }

    /* Sync the following functions in hoster- and decrypterplugin */
    public static String getVideosource(final Browser br) {
        String videosource = br.getRegex("\"sequence\":\"([^<>\"]*?)\"").getMatch(0);
        if (videosource == null) {
            videosource = br.getRegex("%2Fsequence%2F(.*?)</object>").getMatch(0);
            if (videosource != null) {
                videosource = Encoding.urlDecode(videosource, false);
            }
        }
        if (videosource == null) {
            videosource = br.getRegex("name=\"flashvars\" value=\"(.*?)\"/></object>").getMatch(0);
        }
        if (videosource == null) {
            /*
             * This source is unsupported however we only need to have it here so the handling later will eventually fail and jump into
             * embed-fallback mode. See here (some users seem to get another/new videoplayer):
             * https://board.jdownloader.org/showthread.php?t=64943&page=2
             */
            videosource = br.getRegex("window\\.playerV5 = dmp\\.create\\(document\\.getElementById\\(\\'player\\'\\), (\\{.*?\\}\\})\\);").getMatch(0);
        }
        if (videosource == null) {
            videosource = br.getRegex("(\\{\"context\":.*?[\\}]{2,3});").getMatch(0);
        }
        return videosource;
    }

    private DownloadLink setVideoDownloadlink(final Browser br, final LinkedHashMap<String, String[]> foundqualities, final String qualityValue) throws ParseException {
        String directlinkinfo[] = foundqualities.get(qualityValue);
        if (directlinkinfo != null) {
            final String directlink = Encoding.htmlDecode(directlinkinfo[0]);
            final DownloadLink dl = createDownloadlink("https://dailymotiondecrypted.com/video/" + videoId);
            String qualityName = directlinkinfo[1]; // qualityName is dlinfo[2]
            if (qualityName == null) {
                /* For hls urls */
                if (directlink.matches(".+/manifest/.+\\.m3u8.+include=\\d+")) {
                    qualityName = new Regex(directlink, "include=(\\d+)").getMatch(0);
                    if (qualityName.equals("240")) {
                        qualityName = "320x240";
                    } else if (qualityName.equals("380")) {
                        qualityName = "640X380";
                    } else if (qualityName.equals("480")) {
                        qualityName = "640X480";
                    } else {
                        /* TODO / leave that untouched */
                    }
                } else {
                    /* For http urls mostly */
                    // for example H264-320x240
                    qualityName = new Regex(directlink, "cdn/([^<>\"]*?)/video").getMatch(0);
                    /* 2016-10-18: Added "manifest" handling for hls urls. */
                    if (qualityName == null) {
                        // statically set it... better than nothing.
                        if ("1".equalsIgnoreCase(qualityValue)) {
                            qualityName = "H264-1920x1080";
                        } else if ("2".equalsIgnoreCase(qualityValue)) {
                            qualityName = "H264-1280x720";
                        } else if ("3".equalsIgnoreCase(qualityValue)) {
                            qualityName = "H264-848x480";
                        } else if ("4".equalsIgnoreCase(qualityValue)) {
                            qualityName = "H264-512x384";
                        } else if ("5".equalsIgnoreCase(qualityValue)) {
                            qualityName = "H264-320x240";
                        }
                    }
                }
            }
            final String originalQualityName = directlinkinfo[2];
            final String qualityNumber = directlinkinfo[3];
            dl.setProperty("directlink", directlink);
            dl.setProperty("qualityvalue", qualityValue);
            dl.setProperty("qualityname", qualityName);
            dl.setProperty("originalqualityname", originalQualityName);
            dl.setProperty("qualitynumber", qualityNumber);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_videoname", filename);
            dl.setProperty("plain_ext", ".mp4");
            dl.setProperty("plain_videoid", videoId);
            dl.setProperty("plain_channel", channelName);
            dl.setProperty("plain_date", Long.toString(date));
            dl.setLinkID("dailymotioncom" + videoId + "_" + qualityName);
            final String formattedFilename = jd.plugins.hoster.DailyMotionCom.getFormattedFilename(dl);
            dl.setName(formattedFilename);
            dl.setContentUrl(parameter);
            logger.info("Creating: " + directlinkinfo[2] + "/" + qualityName + " link");
            logger.info(directlink);
            decryptedLinks.add(dl); // This is it, not the other one.
            return dl;
        } else {
            return null;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}