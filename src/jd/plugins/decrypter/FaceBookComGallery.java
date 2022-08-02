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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.FaceBookComVideos;

@SuppressWarnings("deprecation")
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FaceBookComGallery extends PluginForDecrypt {
    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 2;
    }

    public static String[] getAnnotationNames() {
        return new String[] { "facebook.com_old" };
    }

    public static String[] getAnnotationUrls() {
        // return new String[] { "https?://(?:www\\.)?facebook\\.com/.+" };
        return new String[] { "https?://(?:www\\.)?facebook_plugin_unfinished\\.com/.+" };
    }

    private final String COMPONENT_USERNAME              = "(?:[\\%a-zA-Z0-9\\-]+)";
    private final String TYPE_FBSHORTLINK                = "https?://(?:www\\.)?on\\.fb\\.me/[A-Za-z0-9]+\\+?";
    private final String TYPE_FB_REDIRECT_TO_EXTERN_SITE = "https?://l\\.facebook\\.com/(?:l/[^/]+/.+|l\\.php\\?u=.+)";
    // private final String TYPE_SINGLE_PHOTO = "https?://(?:www\\.)?facebook\\.com/photo\\.php\\?fbid=\\d+.*?";
    private final String TYPE_SET_LINK_PHOTO             = "https?://(?:www\\.)?facebook\\.com/(media/set/\\?set=|media_set\\?set=)o?a[0-9\\.]+(&type=\\d+)?";
    private final String TYPE_SET_LINK_VIDEO             = "https?://(?:www\\.)?facebook\\.com/(media/set/\\?set=|media_set\\?set=)vb\\.\\d+.*?";
    private final String TYPE_PHOTOS_ALBUMS_LINK         = "https?://(?:www\\.)?facebook\\.com/.+photos_albums";
    private final String TYPE_PHOTOS_OF_LINK             = "https?://(?:www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_of.*";
    private final String TYPE_PHOTOS_ALL_LINK            = "https?://(?:www\\.)?facebook\\.com/[A-Za-z0-9\\.]+/photos_all.*";
    private final String TYPE_PHOTOS_STREAM_LINK         = "https?://(?:www\\.)?facebook\\.com/[^/]+/photos_stream.*";
    private final String TYPE_PHOTOS_STREAM_LINK_2       = "https?://(?:www\\.)?facebook\\.com/pages/[^/]+/\\d+\\?sk=photos_stream&tab=.*";
    // private final String TYPE_PHOTOS_LINK = "https?://(?:www\\.)?facebook\\.com/" + COMPONENT_USERNAME + "/photos.*";
    private final String TYPE_PHOTOS_LINK_2              = "https?://(?:www\\.)?facebook\\.com/pg/" + COMPONENT_USERNAME + "/photos.*";
    private final String TYPE_GROUPS_PHOTOS              = "https?://(?:www\\.)?facebook\\.com/groups/\\d+/photos/";
    private final String TYPE_GROUPS_FILES               = "https?://(?:www\\.)?facebook\\.com/groups/\\d+/files/";
    private final String TYPE_PROFILE_PHOTOS             = "^https?://(?:www\\.)?facebook\\.com/profile\\.php\\?id=\\d+&sk=photos&collection_token=\\d+(?:%3A|:)\\d+(?:%3A|:)5$";
    private final String TYPE_PROFILE_ALBUMS             = "^https?://(?:www\\.)?facebook\\.com/profile\\.php\\?id=\\d+&sk=photos&collection_token=\\d+(?:%3A|:)\\d+(?:%3A|:)6$";
    private final String TYPE_NOTES                      = "https?://(?:www\\.)?facebook\\.com/(notes/|note\\.php\\?note_id=).+";
    private final String TYPE_MESSAGE                    = "https?://(?:www\\.)?facebook\\.com/messages/.+";
    private boolean      debug                           = false;

    @Deprecated
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // for debugging
        if (debug) {
            disableLogger();
        }
        if (param.getCryptedUrl().matches(TYPE_FBSHORTLINK)) {
            return handleRedirectToExternalSite(param.getCryptedUrl());
        } else {
            return crawl(param);
        }
    }

    private ArrayList<DownloadLink> crawl(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final FaceBookComVideos hosterPlugin = (FaceBookComVideos) this.getNewPluginForHostInstance(this.getHost());
            hosterPlugin.login(account, false);
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> videoMaps3 = new HashMap<String, Object>();
        /* Different sources to parse their json. */
        final List<String> jsonRegExes = new ArrayList<String>();
        /* 2021-03-19: E.g. when user is loggedIN */
        jsonRegExes.add(Pattern.quote("(new ServerJS()).handleWithCustomApplyEach(ScheduledApplyEach,") + "(\\{.*?\\})" + Pattern.quote(");});});</script>"));
        /* Same as previous RegEx but lazier. */
        jsonRegExes.add(Pattern.quote("(new ServerJS()).handleWithCustomApplyEach(ScheduledApplyEach,") + "(\\{.*?\\})" + Pattern.quote(");"));
        /* 2022-08-01: Lazier attempt: On RegEx which is simply supposed to find all jsons on the current page. */
        jsonRegExes.add("<script type=\"application/json\" data-content-len=\"\\d+\" data-sjs>(\\{.*?)</script>");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final HashSet<String> processedJsons = new HashSet<String>();
        int numberofJsonsFound = 0;
        for (final String jsonRegEx : jsonRegExes) {
            final String[] jsons = br.getRegex(jsonRegEx).getColumn(0);
            for (final String json : jsons) {
                /* Do not process/parse same json multiple times. */
                if (!processedJsons.add(json)) {
                    continue;
                }
                numberofJsonsFound++;
                try {
                    final Object jsonO = JavaScriptEngineFactory.jsonToJavaMap(json);
                    /* 2021-03-23: Use JavaScriptEngineFactory as they can also have json without quotes around the keys. */
                    // final Object jsonO = JSonStorage.restoreFromString(json, TypeRef.OBJECT);
                    final ArrayList<DownloadLink> videos = new ArrayList<DownloadLink>();
                    this.crawlVideos(jsonO, videos);
                    ret.addAll(videos);
                    final ArrayList<DownloadLink> photos = new ArrayList<DownloadLink>();
                    this.crawlPhotos(jsonO, photos);
                    ret.addAll(photos);
                    this.websiteFindVideoMaps3(jsonO, videoMaps3);
                } catch (final Throwable ignore) {
                    // logger.log(ignore);
                }
            }
        }
        if (ret.isEmpty() && skippedLivestreams > 0) {
            logger.info("Livestreams are not supported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            final boolean debugWriteFoundJsonsToFile = false;
            if (debugWriteFoundJsonsToFile) {
                try {
                    // IO.writeToFile(new File("fbdebug_videoMaps2.txt"), JSonStorage.serializeToJsonByteArray(videoMaps2),
                    // IO.SYNC.META_AND_DATA);
                    IO.writeToFile(new File("fbdebug_videoMaps3.txt"), JSonStorage.serializeToJsonByteArray(videoMaps3), IO.SYNC.META_AND_DATA);
                } catch (final Throwable e) {
                }
            }
        }
        if (numberofJsonsFound == 0) {
            logger.info("Failed to find any jsons --> Probably unsupported URL");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return ret;
    }

    private ArrayList<DownloadLink> handleRedirectToExternalSite(final String url) throws DecrypterException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String external_url = new Regex(url, "/l\\.php\\?u=([^&]+)").getMatch(0);
        if (StringUtils.isNotEmpty(external_url)) {
            external_url = Encoding.urlDecode(external_url, false);
            ret.add(this.createDownloadlink(external_url));
            return ret;
        }
        external_url = new Regex(url, "facebook\\.com/l/[^/]+/(.+)").getMatch(0);
        if (StringUtils.isNotEmpty(external_url)) {
            ret.add(this.createDownloadlink("https://" + external_url));
            return ret;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private int skippedLivestreams = 0;

    private void crawlVideos(final Object o, final List<DownloadLink> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                final String valueStr = value instanceof String ? value.toString() : null;
                final String __typename = (String) map.get("__typename");
                if (key.equals("id") && map.containsKey("is_live_streaming") && StringUtils.equals(__typename, "Video") && map.containsKey("dash_manifest")) {
                    final boolean isLivestream = ((Boolean) map.get("is_live_streaming")).booleanValue();
                    final String videoID = valueStr;
                    if (isLivestream) {
                        /* Livestreams are not supported */
                        logger.info("Skipping livestream: " + videoID);
                        skippedLivestreams++;
                        continue;
                    }
                    final String url = (String) map.get("permalink_url");
                    final DownloadLink thumbnail = this.createDownloadlink(JavaScriptEngineFactory.walkJson(map, "preferred_thumbnail/image/uri").toString());
                    thumbnail.setProperty(FaceBookComVideos.PROPERTY_TYPE, "thumbnail");
                    final DownloadLink link = this.createDownloadlink(url);
                    final Object playable_duration_in_ms = map.get("playable_duration_in_ms");
                    if (playable_duration_in_ms instanceof Number) {
                        /* Set this as a possible Packagizer property. */
                        link.setProperty(FaceBookComVideos.PROPERTY_RUNTIME_MILLISECONDS, ((Number) playable_duration_in_ms).longValue());
                    }
                    final String title = (String) map.get("name");
                    final String uploader = (String) JavaScriptEngineFactory.walkJson(map, "owner/name");
                    String publishDateFormatted = null;
                    final Object publish_timeO = map.get("publish_time");
                    if (publish_timeO instanceof Number) {
                        final long publish_time = ((Number) publish_timeO).longValue();
                        final Date date = new Date(publish_time * 1000);
                        publishDateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(date);
                    }
                    final String description = (String) JavaScriptEngineFactory.walkJson(map, "savable_description/text");
                    final String uploaderURL = FaceBookComVideos.getUploaderNameFromVideoURL(url);
                    final String urlLow = (String) map.get("playable_url");
                    final String urlHigh = (String) map.get("playable_url_quality_hd");
                    if (!StringUtils.isEmpty(urlHigh)) {
                        link.setProperty(FaceBookComVideos.PROPERTY_DIRECTURL_HD, urlHigh);
                    }
                    if (!StringUtils.isEmpty(urlLow)) {
                        link.setProperty(FaceBookComVideos.PROPERTY_DIRECTURL_LOW, urlLow);
                    }
                    link.setProperty(FaceBookComVideos.PROPERTY_TYPE, "video");
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(videoID);
                    if (!StringUtils.isEmpty(description)) {
                        fp.setComment(description);
                    }
                    final ArrayList<DownloadLink> thisResults = new ArrayList<DownloadLink>();
                    thisResults.add(link);
                    thisResults.add(thumbnail);
                    for (final DownloadLink thisResult : thisResults) {
                        thisResult.setProperty(FaceBookComVideos.PROPERTY_CONTENT_ID, videoID);
                        if (uploaderURL != null) {
                            thisResult.setProperty(FaceBookComVideos.PROPERTY_UPLOADER_URL, uploaderURL);
                        }
                        if (!StringUtils.isEmpty(title)) {
                            thisResult.setProperty(FaceBookComVideos.PROPERTY_TITLE, title);
                        }
                        if (!StringUtils.isEmpty(uploader)) {
                            thisResult.setProperty(FaceBookComVideos.PROPERTY_UPLOADER, uploader);
                        }
                        if (publishDateFormatted != null) {
                            link.setProperty(FaceBookComVideos.PROPERTY_DATE_FORMATTED, publishDateFormatted);
                        }
                        thisResult._setFilePackage(fp);
                        thisResult.setAvailable(true);
                    }
                    FaceBookComVideos.setFilename(link);
                    results.addAll(thisResults);
                    break;
                } else if (value instanceof List || value instanceof Map) {
                    crawlVideos(value, results);
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    crawlVideos(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    /** Returns Mao of maps containing information about video + old http streams. */
    private void websiteFindVideoMaps2(final Object o, final Map<String, Object> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                final String valueStr = value instanceof String ? value.toString() : null;
                final String __typename = (String) map.get("__typename");
                if (key.equals("id") && map.containsKey("is_live_streaming") && StringUtils.equals(__typename, "Video")) {
                    results.put(valueStr, map);
                    break;
                } else if (value instanceof List || value instanceof Map) {
                    websiteFindVideoMaps2(value, results);
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    websiteFindVideoMaps2(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    /** Returns Mao of maps containing information about video + new DASH streams. */
    private void websiteFindVideoMaps3(final Object o, final Map<String, Object> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("data") && map.containsKey("path") && map.containsKey("extensions") && value instanceof Map) {
                    final Map<String, Object> dataMap = (Map<String, Object>) value;
                    final String thisIDStr = dataMap.get("id").toString();
                    if (thisIDStr.matches("\\d+") && dataMap.containsKey("live_status")) {
                        results.put(thisIDStr, map);
                    }
                } else if (value instanceof List || value instanceof Map) {
                    websiteFindVideoMaps3(value, results);
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    websiteFindVideoMaps3(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    private void crawlPhotos(final Object o, final ArrayList<DownloadLink> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("id") && value instanceof String) {
                    if (map.containsKey("__isMedia") && map.containsKey("image")) {
                        final DownloadLink link = this.createDownloadlink(JavaScriptEngineFactory.walkJson(map, "image/uri").toString());
                        link.setProperty(FaceBookComVideos.PROPERTY_TYPE, "photo");
                        link.setAvailable(true);
                        results.add(link);
                        break;
                    } else {
                        continue;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    crawlPhotos(value, results);
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    crawlPhotos(arrayo, results);
                }
            }
            return;
        } else {
            return;
        }
    }

    private String getProfileID() {
        String profileid = br.getRegex("data-gt=\"\\&#123;\\&quot;profile_owner\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        if (profileid == null) {
            profileid = br.getRegex("PageHeaderPageletController_(\\d+)\"").getMatch(0);
            if (profileid == null) {
                profileid = br.getRegex("data-profileid=\"(\\d+)\"").getMatch(0);
                if (profileid == null) {
                    profileid = br.getRegex("\\\\\"profile_id\\\\\":(\\d+)").getMatch(0);
                }
            }
        }
        return profileid;
    }

    private String getajaxpipeToken() {
        final String ajaxpipe = br.getRegex("\"ajaxpipe_token\":\"([^<>\"]*?)\"").getMatch(0);
        return ajaxpipe;
    }

    private String getPageTitle() {
        String pageTitle = br.getRegex("id=\"pageTitle\">([^<>\"]*?)</title>").getMatch(0);
        if (pageTitle != null) {
            pageTitle = HTMLEntities.unhtmlentities(pageTitle);
            pageTitle = pageTitle.trim().replaceFirst("\\s*\\|\\s*Facebook$", "");
        }
        return pageTitle;
    }

    private String getPageAlbumTitle() {
        String albumTitle = br.getRegex("<h1 class=\"fbPhotoAlbumTitle\">([^<>]*?)<").getMatch(0);
        if (albumTitle != null) {
            albumTitle = HTMLEntities.unhtmlentities(albumTitle);
            albumTitle = albumTitle.trim();
        }
        return albumTitle;
    }

    public static String getDyn() {
        return "7xeXxmdwgp8fqwOyax68xfLFwgoqwgEoyUnwgU6C7QdwPwDyUG4UeUuwh8eUny8lwIwHwJwr9U";
    }

    public static String get_fb_dtsg(final Browser br) {
        final String fb_dtsg = br.getRegex("name=\\\\\"fb_dtsg\\\\\" value=\\\\\"([^<>\"]*?)\\\\\"").getMatch(0);
        return fb_dtsg;
    }

    private String getLastFBID() {
        String currentLastFbid = br.getRegex("\"last_fbid\\\\\":\\\\\"(\\d+)\\\\\\\"").getMatch(0);
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\\\\\":(\\d+)").getMatch(0);
        }
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\":(\\d+)").getMatch(0);
        }
        if (currentLastFbid == null) {
            currentLastFbid = br.getRegex("\"last_fbid\":\"(\\d+)\"").getMatch(0);
        }
        return currentLastFbid;
    }

    public static String getUser(final Browser br) {
        String user = br.getRegex("\"user\":\"(\\d+)\"").getMatch(0);
        if (user == null) {
            user = br.getRegex("detect_broken_proxy_cache\\(\"(\\d+)\", \"c_user\"\\)").getMatch(0);
        }
        // regex verified: 10.2.2014
        if (user == null) {
            user = br.getRegex("\\[(\\d+)\\,\"c_user\"").getMatch(0);
        }
        return user;
    }

    public static final String get_ttstamp() {
        return Long.toString(System.currentTimeMillis());
    }

    @Override
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /**
     * Code below = prevents Eclipse from freezing as it removes the log output of this thread!
     **/
    private void disableLogger() {
        final LogInterface logger = new LogInterface() {
            @Override
            public void warning(String msg) {
            }

            @Override
            public void severe(String msg) {
            }

            @Override
            public void log(Throwable e) {
            }

            @Override
            public void info(String msg) {
            }

            @Override
            public void finest(String msg) {
            }

            @Override
            public void finer(String msg) {
            }

            @Override
            public void exception(String msg, Throwable e) {
            }

            @Override
            public void fine(String msg) {
            }
        };
        this.setLogger(logger);
        ((LinkCrawlerThread) Thread.currentThread()).setLogger(logger);
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}