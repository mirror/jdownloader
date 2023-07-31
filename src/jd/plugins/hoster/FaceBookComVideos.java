//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.FacebookConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FaceBookComVideos extends PluginForHost {
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    private static final String PATTERN_PHOTO                          = "(?i)https?://[^/]+/(?:photo\\.php|photo/)\\?fbid=(\\d+)";
    private static final String PATTERN_GROUP_PERMALINK                = "(?i)https://[^/]+/groups/[^/]+/permalink/\\d+";
    private static final String PATTERN_POSTS                          = "(?i)https://[^/]+/([^/]+)/posts/[^/]+.*";
    private static final String PATTERN_PHOTO_PART_OF_ALBUM            = "(?i)https?://[^/]+/[^/]+/photos/a\\.\\d+/(\\d+)";
    /* Allow parameter 'v' to be anywhere in that URL. */
    private static final String PATTERN_VIDEO_WATCH                    = "(?i)https?://[^/]+/watch/(?:live/)?\\?.*v=(\\d+)";
    private static final String PATTERN_VIDEO_WITH_UPLOADER_NAME       = "(?i)https://[^/]+/([^/]+)/videos/(\\d+).*";
    // private static final String TYPE_SINGLE_VIDEO_ALL = "https?://(www\\.)?facebook\\.com/video\\.php\\?v=\\d+";
    public static final String  PROPERTY_DATE_FORMATTED                = "date_formatted";
    public static final String  PROPERTY_TITLE                         = "title";
    /* Real uploader name */
    public static final String  PROPERTY_UPLOADER                      = "uploader";
    /* Uploader name inside URL (slug, shortened variant of uploaders' name.) */
    public static final String  PROPERTY_UPLOADER_URL                  = "uploader_url";
    public static final String  PROPERTY_CONTENT_ID                    = "content_id";
    @Deprecated
    public static final String  PROPERTY_DIRECTURL_OLD                 = "directurl";
    public static final String  PROPERTY_DIRECTURL_LAST                = "directurl_last";
    public static final String  PROPERTY_DIRECTURL_LOW                 = "directurl_low";
    public static final String  PROPERTY_DIRECTURL_HD                  = "directurl_hd";
    private static final String PROPERTY_IS_CHECKABLE_VIA_PLUGIN_EMBED = "is_checkable_via_plugin_embed";
    public static final String  PROPERTY_ACCOUNT_REQUIRED              = "account_required";
    public static final String  PROPERTY_RUNTIME_MILLISECONDS          = "runtime_milliseconds";
    public static final String  PROPERTY_DESCRIPTION                   = "description";
    public static final String  PROPERTY_TYPE                          = "type";
    public static final String  TYPE_VIDEO                             = "video";
    public static final String  TYPE_PHOTO                             = "photo";
    public static final String  TYPE_THUMBNAIL                         = "thumbnail";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "facebook.com" });
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
            String regex = "https?://(?:www\\.|m\\.)?" + buildHostsPatternPart(domains) + "/(?:";
            regex += ".*?video\\.php\\?v=\\d+|";
            regex += "video/embed\\?video_id=\\d+|";
            regex += ".*?/videos/(?:[^/]+/)?\\d+|";
            regex += ".*?/posts/[^/]+|";
            regex += "groups/[^/]+/permalink/\\d+|";
            regex += "watch/\\?.*v=\\d+|";
            regex += "watch/live/\\?.*v=\\d+";
            /* Photo RegExes */
            regex += "photo\\.php\\?fbid=\\d+|";
            regex += "photo/\\?fbid=\\d+|";
            regex += "[^/]+/photos/a\\.\\d+/\\d+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.facebook.com/r.php");
        /*
         * to prevent all downloads starting and finishing together (quite common with small image downloads), login, http request and json
         * task all happen at same time and cause small hangups and slower download speeds. raztoki20160309
         */
        setStartIntervall(200l);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        /* 2021-03-22: E.g. remove mobile page subdomain. */
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("(?i)https://[^/]+/", "https://www." + this.getHost() + "/"));
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            /* Older URLs */
            return this.getHost() + "://" + fid + "/" + getType(link);
        } else {
            return super.getLinkID(link);
        }
    }

    public static String getFID(final DownloadLink link) {
        final String storedContentID = link.getStringProperty(PROPERTY_CONTENT_ID);
        if (storedContentID != null) {
            return storedContentID;
        } else {
            return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        }
    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.setCookie(this.getHost(), "locale", "en_GB");
        br.setFollowRedirects(true);
        return br;
    }

    private String getAndCheckDownloadURL(final DownloadLink link, final boolean throwExceptionIfNoDirecturlIsAvailable) throws Exception {
        final String urlLast = link.getStringProperty(PROPERTY_DIRECTURL_LAST);
        final String urlVideoLow = link.getStringProperty(PROPERTY_DIRECTURL_LOW);
        final String urlVideoHD = link.getStringProperty(PROPERTY_DIRECTURL_HD);
        final String urlOld = link.getStringProperty(PROPERTY_DIRECTURL_OLD);
        if (urlLast == null && urlVideoLow == null && urlVideoHD == null && urlOld == null) {
            if (throwExceptionIfNoDirecturlIsAvailable) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return null;
            }
        }
        String ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_LAST);
        if (ret == null && (PluginJsonConfig.get(this.getConfigInterface()).isPreferHD() || urlVideoLow == null)) {
            ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_HD);
        }
        if (ret == null) {
            ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_LOW);
            if (ret == null) {
                ret = checkDirecturlFromPropertyAndSetFilesize(link, PROPERTY_DIRECTURL_OLD);
            }
        }
        return ret;
    }

    /** Returns directurl without checking it. */
    private static String getDirecturl(final DownloadLink link) {
        String ret = link.getStringProperty(PROPERTY_DIRECTURL_LAST);
        final String urlVideoLow = link.getStringProperty(PROPERTY_DIRECTURL_LOW);
        if (ret == null && (PluginJsonConfig.get(FacebookConfig.class).isPreferHD() || urlVideoLow == null)) {
            ret = link.getStringProperty(PROPERTY_DIRECTURL_HD);
        }
        if (ret == null) {
            ret = urlVideoLow;
            if (ret == null) {
                ret = link.getStringProperty(PROPERTY_DIRECTURL_OLD);
            }
        }
        return ret;
    }

    private String downloadURL = null;

    private int getMaxChunks(final DownloadLink link) {
        if (isVideo(link)) {
            return 0;
        } else {
            return 1;
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(PATTERN_PHOTO) || link.getPluginPatternMatcher().matches(PATTERN_PHOTO_PART_OF_ALBUM)) {
            /* Legacy handling */
            return false;
        } else {
            final String type = link.getStringProperty(PROPERTY_TYPE);
            if (type == null) {
                /* Old video URL (legacy handling) */
                return true;
            } else {
                if (StringUtils.equalsIgnoreCase(type, TYPE_VIDEO)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static boolean isPhoto(final DownloadLink link) {
        final String type = getType(link);
        if (StringUtils.equals(type, TYPE_PHOTO)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getType(final DownloadLink link) {
        final String type = link.getStringProperty(PROPERTY_TYPE);
        if (type != null) {
            return type;
        } else if (isVideo(link)) {
            return TYPE_VIDEO;
        } else {
            /* Legacy handling */
            return TYPE_PHOTO;
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        prepBR(this.br);
        downloadURL = getAndCheckDownloadURL(link, false);
        if (downloadURL != null) {
            // New handling dec 2022
            logger.info("Availablecheck only via directurl done:" + downloadURL);
            return AvailableStatus.TRUE;
        }
        boolean loggedIn = false;
        final boolean fastLinkcheck = PluginJsonConfig.get(this.getConfigInterface()).isEnableFastLinkcheck();
        if (link.getPluginPatternMatcher().matches(PATTERN_GROUP_PERMALINK) || link.getPluginPatternMatcher().matches(PATTERN_POSTS)) {
            /* Hm old code */
            br.getPage(link.getPluginPatternMatcher());
            String video = null;
            boolean search = true;
            while (search) {
                search = false;
                video = br.getRegex("video_url\\s*:\\s*\"(/[^/]+/videos/\\d+/?)\"").getMatch(0);
                if (video != null) {
                    br.getPage(video);
                    search = true;
                    continue;
                }
                if (video == null) {
                    video = br.getRegex("href\\s*=\\s*(?:\"|')(https://www.facebook.com/[^/]+/videos/(?:[^/]+/)?\\d+)").getMatch(0);
                    if (video == null) {
                        video = br.getRegex("\"url\"\\s*:\\s*\"(https?:\\\\/\\\\/www.facebook.com\\\\/[^/]+\\\\/videos\\\\/(?:[^/]+/)?\\d+)").getMatch(0);
                        if (video == null) {
                            video = br.getRegex("url\"\\s*:\\s*\"(https?:\\\\/\\\\/www.facebook.com\\\\/[^/]+\\\\/videos\\\\/(?:[^/]+/)?\\d+)").getMatch(0);
                        }
                        if (video != null) {
                            video = JSonStorage.restoreFromString("\"" + video + "\"", TypeRef.STRING);
                        }
                    }
                }
                if (video == null && (br.containsHTML("(?i)When this happens, it('|&#039;)s usually because the owner only shared it") || br.containsHTML("\"Private group\"") || br.containsHTML("(?i)>\\s*You must log in to continue.\\s*<"))) {
                    if (loggedIn || account == null) {
                        /* Account needed or given account is lacking permissions to view that content. */
                        throw new AccountRequiredException();
                    } else {
                        /* Account available --> Login and try again */
                        login(account, false);
                        loggedIn = true;
                        br.getPage(link.getPluginPatternMatcher());
                        search = true;
                    }
                }
            }
            if (video != null) {
                logger.info("Rewriting:" + link.getPluginPatternMatcher() + "->" + video);
                link.setPluginPatternMatcher(video);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (link.getPluginPatternMatcher().matches(PATTERN_PHOTO) || link.getPluginPatternMatcher().matches(PATTERN_PHOTO_PART_OF_ALBUM)) {
            return this.requestFileInformationPhoto(link, isDownload);
        } else {
            if (!link.isNameSet()) {
                link.setName(getFID(link) + ".mp4");
            }
            if (account != null && !loggedIn) {
                login(account, false);
            }
            /*
             * First round = do this as this is the best way to find all required filename information especially the upload-date!
             */
            AvailableStatus websiteCheckResult = AvailableStatus.UNCHECKABLE;
            try {
                websiteCheckResult = requestFileInformationVideoWebsite(link, isDownload);
                if (websiteCheckResult == AvailableStatus.FALSE) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (account == null) {
                    link.removeProperty(PROPERTY_ACCOUNT_REQUIRED);
                }
            } catch (final AccountRequiredException aq) {
                if (account != null) {
                    /*
                     * We're already logged in -> Item must be offline (or can only be accessed via another account with has the required
                     * permissions which we can't know as Facebook gives little information in their errormessages).
                     */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw aq;
                }
            }
            setFilename(link);
            final boolean findAndCheckDownloadurl = isDownload || fastLinkcheck == false;
            if (findAndCheckDownloadurl) {
                downloadURL = getAndCheckDownloadURL(link, true);
                if (downloadURL == null) {
                    /* E.g. final downloadurl doesn't lead to video-file. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
                }
            }
            return AvailableStatus.TRUE;
        }
    }

    public static String getUploaderURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO_WITH_UPLOADER_NAME)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO_WITH_UPLOADER_NAME).getMatch(0);
        } else {
            return link.getStringProperty(PROPERTY_UPLOADER_URL);
        }
    }

    public static String getUploaderNameAny(final DownloadLink link) {
        final String uploader = link.getStringProperty(PROPERTY_UPLOADER);
        final String uploaderURL = getUploaderURL(link);
        if (uploader != null) {
            return uploader;
        } else {
            return uploaderURL;
        }
    }

    /** Sets filename based on previously set DownloadLink properties. */
    public static void setFilename(final DownloadLink link) {
        /* Some filename corrections */
        String filename = "";
        final String title = link.getStringProperty(PROPERTY_TITLE);
        final String dateFormatted = link.getStringProperty(PROPERTY_DATE_FORMATTED);
        if (dateFormatted != null) {
            filename += dateFormatted + "_";
        }
        final String uploaderNameForFilename = getUploaderNameAny(link);
        if (!StringUtils.isEmpty(uploaderNameForFilename)) {
            filename += uploaderNameForFilename + "_";
        }
        if (!StringUtils.isEmpty(title)) {
            filename += title.replaceAll("\\s*\\| Facebook\\s*$", "");
            if (!filename.contains(getFID(link))) {
                filename = filename + "_" + getFID(link);
            }
        } else {
            /* No title given at all -> use fuid only */
            filename += getFID(link);
        }
        String ext = null;
        if (isVideo(link)) {
            ext = ".mp4";
        } else {
            final String directurl = getDirecturl(link);
            if (directurl != null) {
                ext = Plugin.getFileNameExtensionFromURL(directurl);
            }
        }
        if (ext != null) {
            filename += ext;
        }
        link.setFinalFileName(filename);
    }

    @Deprecated
    private Object findSchemaJsonVideoObject() throws Exception {
        final String[] jsons = br.getRegex("<script[^>]*?type=\"application/ld\\+json\"[^>]*>(.*?)</script>").getColumn(0);
        for (final String json : jsons) {
            if (json.contains("VideoObject")) {
                return JavaScriptEngineFactory.jsonToJavaObject(json);
            }
        }
        return null;
    }

    @Deprecated
    private Object websiteFindAndParseJson() {
        final String json = br.getRegex(Pattern.quote("<script>requireLazy([\"TimeSliceImpl\",\"ServerJS\"],function(TimeSlice,ServerJS){var s=(new ServerJS());s.handle(") + "(\\{.*?\\})\\);requireLazy\\(").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.OBJECT);
    }

    /**
     * Parses specific json object from website and sets all useful information as properties on given DownloadLink.
     *
     * @throws PluginException
     */
    @Deprecated
    private void websitehandleVideoJson(final DownloadLink link, final Object jsonO) throws PluginException {
        final Object videoO = this.websiteFindVideoMap1(jsonO, getFID(link));
        final Object htmlO = pluginEmbedFindHTMLInJson(jsonO, getFID(link));
        if (htmlO != null) {
            String specialHTML = (String) htmlO;
            specialHTML = PluginJSonUtils.unescape(specialHTML);
            final String uploader = new Regex(specialHTML, "alt=\"\" aria-label=\"([^\"]+)\"").getMatch(0);
            if (uploader != null) {
                link.setProperty(PROPERTY_UPLOADER, Encoding.htmlDecode(uploader).trim());
            }
        }
        Map<String, Object> entries = (Map<String, Object>) videoO;
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "videoData/{0}");
        final boolean isLivestream = ((Boolean) entries.get("is_live_stream")).booleanValue();
        if (isLivestream) {
            logger.info("Livestreams are not supported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] sourcesInOrder;
        if (PluginJsonConfig.get(this.getConfigInterface()).isPreferHD()) {
            sourcesInOrder = new String[] { "hd_src_no_ratelimit", "hd_src", "sd_src_no_ratelimit", "sd_src" };
        } else {
            /* Other order if user doesn't prefer highest quality. */
            sourcesInOrder = new String[] { "sd_src_no_ratelimit", "sd_src", "hd_src_no_ratelimit", "hd_src" };
        }
        for (final String videosrc : sourcesInOrder) {
            final String dllink = (String) entries.get(videosrc);
            if (!StringUtils.isEmpty(dllink)) {
                logger.info("Found directurl using videosource: " + videosrc);
                if (StringUtils.contains(videosrc, "hd_")) {
                    link.setProperty(PROPERTY_DIRECTURL_HD, dllink);
                } else {
                    link.setProperty(PROPERTY_DIRECTURL_LOW, dllink);
                }
            }
        }
        /* Try to get name of the uploader */
        if (entries.containsKey("video_url")) {
            String videourl = (String) entries.get("video_url");
            if (!StringUtils.isEmpty(videourl)) {
                videourl = PluginJSonUtils.unescape(videourl);
                final String uploaderURL = getUploaderNameFromVideoURL(videourl);
                if (uploaderURL != null) {
                    link.setProperty(PROPERTY_UPLOADER_URL, uploaderURL);
                }
            }
        }
    }

    /**
     * Normal linkcheck via website. </br>
     * Only suited for URLs matching TYPE_VIDEO_WATCH! <br>
     * For other URL-types, website can be quite different depending on the video the user wants to download - it can redirect to random
     * other URLs! </br>
     * The Facebook website is hard to parse thus our attempt is to find different json sources to obtain video information from --> Set
     * crawled data on our DownloadLink and use that later to build filenames- and for downloading.
     *
     * @throws PluginException
     */
    @Deprecated
    private AvailableStatus requestFileInformationVideoWebsite(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        br.getPage(link.getPluginPatternMatcher());
        this.checkErrors(link);
        Object jsonO1 = null, jsonO2 = null, jsonO3 = null;
        Object errorO = null;
        /* Different sources to parse their json. */
        final List<String> jsonRegExes = new ArrayList<String>();
        /* 2021-03-19: E.g. when user is loggedIN */
        jsonRegExes.add(Pattern.quote("(new ServerJS()).handleWithCustomApplyEach(ScheduledApplyEach,") + "(\\{.*?\\})" + Pattern.quote(");});});</script>"));
        /* Same as previous RegEx but lazier. */
        jsonRegExes.add(Pattern.quote("(new ServerJS()).handleWithCustomApplyEach(ScheduledApplyEach,") + "(\\{.*?\\})" + Pattern.quote(");"));
        /* 2022-08-01: Lazier attempt: On RegEx which is simply supposed to find all jsons on the current page. */
        jsonRegExes.add("<script type=\"application/json\" data-content-len=\"\\d+\" data-sjs>(\\{.*?)</script>");
        final String videoID = getFID(link);
        int numberofJsonsFound = 0;
        for (final String jsonRegEx : jsonRegExes) {
            final String[] jsons = br.getRegex(jsonRegEx).getColumn(0);
            for (final String json : jsons) {
                numberofJsonsFound++;
                try {
                    final Object jsonO = JavaScriptEngineFactory.jsonToJavaMap(json);
                    /* 2021-03-23: Use JavaScriptEngineFactory as they can also have json without quotes around the keys. */
                    // final Object jsonO = JSonStorage.restoreFromString(json, TypeRef.OBJECT);
                    if (jsonO1 == null) {
                        jsonO1 = this.websiteFindVideoMap1(jsonO, videoID);
                    }
                    if (jsonO2 == null) {
                        jsonO2 = this.websiteFindVideoMap2(jsonO, videoID);
                    }
                    if (jsonO3 == null) {
                        jsonO3 = this.websiteFindVideoMap3(jsonO, videoID);
                    }
                    if (errorO == null) {
                        errorO = this.websiteFindErrorMap(jsonO, videoID);
                    }
                } catch (final Throwable ignore) {
                }
            }
        }
        if (numberofJsonsFound == 0) {
            /* Content may still be offline or only for logged-in users! */
            logger.warning("Possible fatal failure: Didn't find any json source");
        }
        /**
         * Try to find extra data for nicer filenames. </br>
         * Do not trust this source 100% so only set properties which haven't been set before! </br>
         * This source (video schema object) is not always given!! </br>
         * Example: https://www.facebook.com/socialchefs/videos/326580738743639
         */
        try {
            logger.info("Trying to set filename info via findSchemaJsonVideoObject");
            final Map<String, Object> entries = (Map<String, Object>) findSchemaJsonVideoObject();
            final String title = (String) entries.get("name");
            final String uploadDate = (String) entries.get("uploadDate");
            final String uploader = (String) JavaScriptEngineFactory.walkJson(entries, "author/name");
            if (!StringUtils.isEmpty(uploadDate)) {
                final String dateFormatted = new Regex(uploadDate, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
                if (dateFormatted != null && !link.hasProperty(PROPERTY_DATE_FORMATTED)) {
                    link.setProperty(PROPERTY_DATE_FORMATTED, dateFormatted);
                }
            }
            if (!StringUtils.isEmpty(title) && !link.hasProperty(PROPERTY_TITLE)) {
                link.setProperty(PROPERTY_TITLE, title);
            }
            if (!StringUtils.isEmpty(uploader) && !link.hasProperty(PROPERTY_UPLOADER)) {
                link.setProperty(PROPERTY_UPLOADER, uploader);
            }
            /* 2021-03-23: Don't use this. Normal website video json provides higher quality! */
            // fallback_downloadurl = (String) entries.get("contentUrl");
        } catch (final Throwable ignore) {
            logger.info("Failed to find findSchemaJsonVideoObject");
        }
        if (jsonO3 != null) {
            logger.info("Set filename info via jsonO3");
            final Map<String, Object> entries = (Map<String, Object>) jsonO3;
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final String uploader = (String) JavaScriptEngineFactory.walkJson(data, "owner/owner_as_page/name");
            final String videoDescription = (String) JavaScriptEngineFactory.walkJson(data, "creation_story/message/text");
            final Map<String, Object> titleInfo = (Map<String, Object>) data.get("title");
            final String title = (String) titleInfo.get("text");
            if (!StringUtils.isEmpty(title) && !link.hasProperty(PROPERTY_TITLE)) {
                link.setProperty(PROPERTY_TITLE, title);
            }
            if (!StringUtils.isEmpty(uploader) && !link.hasProperty(PROPERTY_UPLOADER)) {
                link.setProperty(PROPERTY_UPLOADER, uploader);
            }
            if (link.getComment() == null && !StringUtils.isEmpty(videoDescription)) {
                link.setComment(videoDescription);
            }
        }
        if (jsonO1 != null) {
            // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            // System.out.print(JSonStorage.serializeToJson(jsonO1));
            // }
            logger.info("Found videosource: json1");
            this.websitehandleVideoJson(link, jsonO1);
            // if (!link.hasProperty(PROPERTY_UPLOADER) && link.hasProperty(PROPERTY_UPLOADER_URL)) {
            // final String uploaderURL = link.getStringProperty(PROPERTY_UPLOADER_URL);
            // final String uploader = br.getRegex("<a href=\"/watch/" + Pattern.quote(uploaderURL) +
            // "/?\"[^>]*id=\"[^\"]+\"[^>]*>([^<>\"]+)</a>").getMatch(0);
            // if (uploader != null) {
            // link.setProperty(PROPERTY_UPLOADER, uploader);
            // }
            // }
            // if (!link.hasProperty(PROPERTY_TITLE)) {
            // final String title = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\" />").getMatch(0);
            // if (title != null) {
            // link.setProperty(PROPERTY_TITLE, title);
            // }
            // }
            return AvailableStatus.TRUE;
        } else if (jsonO2 != null) {
            logger.info("Found videosource: json2");
            Map<String, Object> entries = (Map<String, Object>) jsonO2;
            final boolean isLivestream = ((Boolean) entries.get("is_live_streaming")).booleanValue();
            if (isLivestream) {
                logger.info("Livestreams are not supported");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (entries.containsKey("playable_duration_in_ms")) {
                /* Set this as a possible packagizer property. */
                link.setProperty(PROPERTY_RUNTIME_MILLISECONDS, ((Number) entries.get("playable_duration_in_ms")).longValue());
            }
            final String title = (String) entries.get("name");
            final String uploader = (String) JavaScriptEngineFactory.walkJson(entries, "owner/name");
            if (entries.containsKey("publish_time")) {
                final long publish_time = ((Number) entries.get("publish_time")).longValue();
                final Date date = new Date(publish_time * 1000);
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                link.setProperty(PROPERTY_DATE_FORMATTED, formatter.format(date));
            }
            try {
                /* Find- and set description if possible */
                final String description = (String) JavaScriptEngineFactory.walkJson(entries, "savable_description/text");
                if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
                    link.setComment(description);
                }
            } catch (final Throwable ignore) {
            }
            try {
                final String permalink_url = (String) entries.get("permalink_url");
                final String uploaderURL = getUploaderNameFromVideoURL(permalink_url);
                if (uploaderURL != null) {
                    link.setProperty(PROPERTY_UPLOADER_URL, uploaderURL);
                }
            } catch (final Throwable ignore) {
            }
            final String urlLow = (String) entries.get("playable_url");
            final String urlHigh = (String) entries.get("playable_url_quality_hd");
            if (!StringUtils.isEmpty(urlHigh)) {
                link.setProperty(PROPERTY_DIRECTURL_HD, urlHigh);
            }
            if (!StringUtils.isEmpty(urlLow)) {
                link.setProperty(PROPERTY_DIRECTURL_LOW, urlLow);
            }
            if (!StringUtils.isEmpty(title)) {
                link.setProperty(PROPERTY_TITLE, title);
            }
            if (!StringUtils.isEmpty(uploader)) {
                link.setProperty(PROPERTY_UPLOADER, uploader);
            }
            return AvailableStatus.TRUE;
        } else if (errorO != null) {
            /*
             * Video offline or we don't have access to it -> Website doesn't return a clear errormessage either -> Let's handle it as
             * offline!
             */
            return AvailableStatus.FALSE;
        } else if (br.getFormbyProperty("id", "login_form") != null) {
            throw new AccountRequiredException();
        } else {
            logger.warning("Failed to find any video json");
            return AvailableStatus.UNCHECKABLE;
        }
    }

    private Object websiteFindErrorMap(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("rootView") && value instanceof Map && entrymap.containsKey("tracePolicy")) {
                    final String tracePolicy = (String) entrymap.get("tracePolicy");
                    final String videoidTmp = (String) JavaScriptEngineFactory.walkJson(entrymap, "params/video_id");
                    if (StringUtils.equals(tracePolicy, "comet.error") && StringUtils.equals(videoidTmp, videoid)) {
                        return o;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = websiteFindErrorMap(value, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object ret = websiteFindErrorMap(arrayo, videoid);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Deprecated
    private Object websiteFindVideoMap1(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("video_id") && value instanceof String && entrymap.containsKey("videoData")) {
                    final String entry_id = (String) value;
                    if (entry_id.equals(videoid)) {
                        return o;
                    } else {
                        continue;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = websiteFindVideoMap1(value, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = websiteFindVideoMap1(arrayo, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Deprecated
    private Object websiteFindVideoMap2(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("id") && value instanceof String && entrymap.containsKey("is_live_streaming")) {
                    final String entry_id = (String) value;
                    if (entry_id.equals(videoid)) {
                        return o;
                    } else {
                        continue;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = websiteFindVideoMap2(value, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = websiteFindVideoMap2(arrayo, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * 2021-11-22: This result will only contain DASH streams --> Only use it to grab video title information, not for downloading as we
     * cannot handle split video/audio for now.
     */
    @Deprecated
    private Object websiteFindVideoMap3(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("data") && entrymap.containsKey("path") && entrymap.containsKey("extensions")) {
                    final Map<String, Object> dataMap = (Map<String, Object>) value;
                    final String thisID = dataMap.get("id").toString();
                    if (thisID.equals(videoid) && dataMap.containsKey("live_status")) {
                        return o;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = websiteFindVideoMap3(value, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = websiteFindVideoMap3(arrayo, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private String checkDirecturlFromPropertyAndSetFilesize(final DownloadLink link, final String propertyName) throws IOException, PluginException {
        final String url = link.getStringProperty(propertyName);
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(url);
            if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return url;
            } else {
                try {
                    br.followConnection(true);
                } catch (IOException ignore) {
                    logger.log(ignore);
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        link.removeProperty(propertyName);
        return null;
    }

    @Deprecated
    private Object pluginEmbedFindHTMLInJson(final Object o, final String videoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("__html") && value instanceof String && ((String) value).contains(videoid)) {
                    return value;
                } else if (value instanceof List || value instanceof Map) {
                    final Object target = pluginEmbedFindHTMLInJson(value, videoid);
                    if (target != null) {
                        return target;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = pluginEmbedFindHTMLInJson(arrayo, videoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public AvailableStatus requestFileInformationPhoto(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            /* 2021-03-24: Images are usually .jpg or .png */
            link.setName(getFID(link) + ".png");
        }
        this.prepBR(this.br);
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa != null) {
            login(aa, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        String dllink = null;
        final String[] jsons = br.getRegex("<script type=\"application/json\" data-content-len=\"\\d+\" data-sjs>(\\{.*?)</script>").getColumn(0);
        for (final String json : jsons) {
            final Object jsonO = JSonStorage.restoreFromString(json, TypeRef.OBJECT);
            final Map<String, Object> photoMap = (Map<String, Object>) findPhotoMap(jsonO, getFID(link));
            if (photoMap != null) {
                dllink = (String) JavaScriptEngineFactory.walkJson(photoMap, "image/uri");
                break;
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            final String filename = Plugin.getFileNameFromURL(new URL(dllink));
            if (filename != null) {
                if (filename.contains(".")) {
                    /* Set custom filename with given extension */
                    link.setFinalFileName(getFID(link) + filename.substring(filename.lastIndexOf(".")));
                } else {
                    link.setFinalFileName(filename);
                }
            }
            link.setProperty(PROPERTY_DIRECTURL_LAST, dllink);
            this.checkDirecturlFromPropertyAndSetFilesize(link, dllink);
        }
        return AvailableStatus.TRUE;
    }

    /** Recursive function to find photoMap inside json. */
    @Deprecated
    private Object findPhotoMap(final Object o, final String photoid) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (key.equals("id") && value instanceof String) {
                    final String entry_id = (String) value;
                    if (entry_id.equals(photoid) && entrymap.containsKey("__isMedia") && entrymap.containsKey("image")) {
                        return o;
                    } else {
                        continue;
                    }
                } else if (value instanceof List || value instanceof Map) {
                    final Object pico = findPhotoMap(value, photoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object pico = findPhotoMap(arrayo, photoid);
                    if (pico != null) {
                        return pico;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public static String getUploaderNameFromVideoURL(final String videourl) {
        if (videourl == null) {
            return null;
        } else {
            return new Regex(videourl, "(?i)https?://[^/]+/([^/]+)/videos/.*").getMatch(0);
        }
    }

    private void checkErrors(final DownloadLink link) throws PluginException {
        if (br.getURL().contains("/login.php") || br.getURL().contains("/login/?next=")) {
            /*
             * 2021-03-01: Login required: There are videos which are only available via account but additionally it seems like FB randomly
             * enforces the need of an account for other videos also e.g. by country/IP.
             */
            throw new AccountRequiredException();
        } else if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO_WATCH) && !br.getURL().contains(getFID(link))) {
            /*
             * Specific type of URL will redirect to other URL/mainpage on offline --> Check for that E.g.
             * https://www.facebook.com/watch/?v=2739449049644930
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "https://www.facebook.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, account, true);
            if (downloadURL == null) {
                if (isAccountRequired(link)) {
                    /*
                     * If this happens while an account is active this means that the user is either missing the rights to access that item
                     * or the item is offline.
                     */
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, this.getMaxChunks(link));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file?");
            } else {
                link.setProperty(PROPERTY_DIRECTURL_LAST, downloadURL);
            }
        }
        dl.startDownload();
    }

    private boolean isAccountRequired(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_ACCOUNT_REQUIRED)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL_LAST);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, true, this.getMaxChunks(link));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                /* Remove that so we don't waste time checking this again. */
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(PROPERTY_DIRECTURL_LAST);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                prepBR(br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                /* 2020-10-9: Experimental login/test */
                final Cookies userCookies = account.loadUserCookies();
                final boolean enforceCookieLogin = true;
                if (enforceCookieLogin && userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    logger.info("Trying to login via user-cookies");
                    br.setCookies(userCookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    if (verifyCookies(account, userCookies, br)) {
                        /* Save cookies to save new valid cookie timestamp */
                        logger.info("User-cookie login successful");
                        /*
                         * Try to make sure that username in JD is unique because via cookie login, user can enter whatever he wants into
                         * username field! 2020-11-16: Username can be "" (empty) for some users [rare case].
                         */
                        final String username = PluginJSonUtils.getJson(br, "username");
                        if (!StringUtils.isEmpty(username)) {
                            logger.info("Found username in json: " + username);
                            account.setUser(username);
                        } else {
                            logger.info("Failed to find username in json (rarec case)");
                        }
                        return;
                    } else {
                        logger.info("User-Cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!validateCookies) {
                        /* Do not validate cookies */
                        return;
                    }
                    if (verifyCookies(account, cookies, br)) {
                        /* Save cookies to save new valid cookie timestamp */
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        /* Get rid of old cookies / headers */
                        br.clearAll();
                        prepBR(br);
                    }
                }
                logger.info("Full login required");
                br.setFollowRedirects(true);
                final boolean prefer_mobile_login = true;
                // better use the website login. else the error handling below might be broken.
                if (prefer_mobile_login) {
                    /* Mobile login = no crypto crap */
                    br.getPage("https://m.facebook.com/");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginForm.remove(null);
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                    br.getPage("https://www.facebook.com/");
                } else {
                    br.getPage("https://www.facebook.com/login.php");
                    final String lang = System.getProperty("user.language");
                    final Form loginForm = br.getForm(0);
                    if (loginForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginForm.remove("persistent");
                    loginForm.put("persistent", "1");
                    loginForm.remove(null);
                    loginForm.remove("login");
                    loginForm.remove("trynum");
                    loginForm.remove("profile_selector_ids");
                    loginForm.remove("legacy_return");
                    loginForm.remove("enable_profile_selector");
                    loginForm.remove("display");
                    String _js_datr = br.getRegex("\"_js_datr\"\\s*,\\s*\"([^\"]+)").getMatch(0);
                    br.setCookie("https://facebook.com", "_js_datr", _js_datr);
                    br.setCookie("https://facebook.com", "_js_reg_fb_ref", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    br.setCookie("https://facebook.com", "_js_reg_fb_gate", Encoding.urlEncode("https://www.facebook.com/login.php"));
                    loginForm.put("email", Encoding.urlEncode(account.getUser()));
                    loginForm.put("pass", Encoding.urlEncode(account.getPass()));
                    br.submitForm(loginForm);
                }
                /**
                 * Facebook thinks we're an unknown device, now we prove we're not ;)
                 */
                if (br.containsHTML(">Your account is temporarily locked")) {
                    final String nh = br.getRegex("name=\"nh\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String dstc = br.getRegex("name=\"fb_dtsg\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (nh == null || dstc == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&submit%5BContinue%5D=Continue");
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "facebook.com", "http://facebook.com", true);
                    String achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                    final String captchaPersistData = br.getRegex("name=\"captcha_persist_data\" value=\"([^<>\"]*?)\"").getMatch(0);
                    if (captchaPersistData == null || achal == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    // Normal captcha handling
                    for (int i = 1; i <= 3; i++) {
                        String captchaLink = br.getRegex("\"(https?://(www\\.)?facebook\\.com/captcha/tfbimage\\.php\\?captcha_challenge_code=[^<>\"]*?)\"").getMatch(0);
                        if (captchaLink == null) {
                            break;
                        }
                        captchaLink = Encoding.htmlDecode(captchaLink);
                        String code;
                        try {
                            code = getCaptchaCode(captchaLink, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_response=" + Encoding.urlEncode(code) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                    }
                    // reCaptcha handling
                    for (int i = 1; i <= 3; i++) {
                        final String rcID = br.getRegex("\"recaptchaPublicKey\":\"([^<>\"]*?)\"").getMatch(0);
                        if (rcID == null) {
                            break;
                        }
                        final String extraChallengeParams = br.getRegex("name=\"extra_challenge_params\" value=\"([^<>\"]*?)\"").getMatch(0);
                        final String captchaSession = br.getRegex("name=\"captcha_session\" value=\"([^<>\"]*?)\"").getMatch(0);
                        if (extraChallengeParams == null || captchaSession == null) {
                            break;
                        }
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId(rcID);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c;
                        try {
                            c = getCaptchaCode("recaptcha", cf, dummyLink);
                        } catch (final Exception e) {
                            continue;
                        }
                        br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&captcha_session=" + Encoding.urlEncode(captchaSession) + "&extra_challenge_params=" + Encoding.urlEncode(extraChallengeParams) + "&recaptcha_type=password&recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha_response=" + Encoding.urlEncode(c) + "&achal=1&submit%5BSubmit%5D=Submit");
                    }
                    for (int i = 1; i <= 3; i++) {
                        if (br.containsHTML(">To confirm your identity, please enter your birthday")) {
                            achal = br.getRegex("name=\"achal\" value=\"([a-z0-9]+)\"").getMatch(0);
                            if (achal == null) {
                                break;
                            }
                            String birthdayVerificationAnswer;
                            try {
                                birthdayVerificationAnswer = getUserInput("Enter your birthday (dd:MM:yyyy)", dummyLink);
                            } catch (final Exception e) {
                                continue;
                            }
                            final String[] bdSplit = birthdayVerificationAnswer.split(":");
                            if (bdSplit == null || bdSplit.length != 3) {
                                continue;
                            }
                            int bdDay = 0, bdMonth = 0, bdYear = 0;
                            try {
                                bdDay = Integer.parseInt(bdSplit[0]);
                                bdMonth = Integer.parseInt(bdSplit[1]);
                                bdYear = Integer.parseInt(bdSplit[2]);
                            } catch (final Exception e) {
                                continue;
                            }
                            br.postPage(br.getURL(), "fb_dtsg=" + Encoding.urlEncode(dstc) + "&nh=" + nh + "&geo=true&birthday_captcha_month=" + bdMonth + "&birthday_captcha_day=" + bdDay + "&birthday_captcha_year=" + bdYear + "&captcha_persist_data=" + Encoding.urlEncode(captchaPersistData) + "&achal=" + achal + "&submit%5BSubmit%5D=Submit");
                        } else {
                            break;
                        }
                    }
                    if (br.containsHTML("/captcha/friend_name_image\\.php\\?")) {
                        // unsupported captcha challange.
                        logger.warning("Unsupported captcha challenge.");
                    }
                } else if (br.containsHTML("/checkpoint/")) {
                    br.getPage("https://www.facebook.com/checkpoint/");
                    final String postFormID = br.getRegex("name=\"post_form_id\" value=\"(.*?)\"").getMatch(0);
                    final String nh = br.getRegex("name=\"nh\" value=\"(.*?)\"").getMatch(0);
                    if (nh == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BContinue%5D=Weiter&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&submit%5BThis+is+Okay%5D=Das+ist+OK&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                    br.postPage("https://www.facebook.com/checkpoint/", "post_form_id=" + postFormID + "&lsd=GT_Up&machine_name=&submit%5BDon%27t+Save%5D=Nicht+speichern&nh=" + nh);
                } else if (br.getURL().contains("/login/save-device")) {
                    /* 2020-10-29: Challenge kinda like "Trust this device" */
                    final Form continueForm = br.getFormbyActionRegex(".*/login/device-based/.*");
                    if (continueForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.submitForm(continueForm);
                    br.getPage("https://" + this.getHost() + "/");
                    br.followRedirect();
                }
                if (!isLoggedinHTML(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Save cookies */
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.removeProperty("");
                }
                throw e;
            }
        }
    }

    protected boolean verifyCookies(final Account account, final Cookies cookies, final Browser br) throws Exception {
        br.setCookies(this.getHost(), cookies);
        final boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.getPage("https://" + this.getHost() + "/");
        } finally {
            br.setFollowRedirects(follow);
        }
        if (this.isLoggedinHTML(br)) {
            logger.info("Successfully logged in via cookies");
            return true;
        } else {
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        final boolean brContainsSecondaryLoggedinHint = br.containsHTML("settings_dropdown_profile_picture");
        final String logout_hash = PluginJSonUtils.getJson(br, "logout_hash");
        logger.info("logout_hash = " + logout_hash);
        logger.info("brContainsSecondaryLoggedinHint = " + brContainsSecondaryLoggedinHint);
        return !StringUtils.isEmpty(logout_hash) && brContainsSecondaryLoggedinHint;
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* No not allow multihost plugins to handle Facebook URLs! */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public Class<? extends FacebookConfig> getConfigInterface() {
        return FacebookConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.removeProperty(PROPERTY_DIRECTURL_LAST);
            link.removeProperty(PROPERTY_DIRECTURL_OLD);
            link.removeProperty(PROPERTY_DIRECTURL_HD);
            link.removeProperty(PROPERTY_DIRECTURL_LOW);
            link.removeProperty(PROPERTY_IS_CHECKABLE_VIA_PLUGIN_EMBED);
            link.removeProperty(PROPERTY_ACCOUNT_REQUIRED);
            link.removeProperty(PROPERTY_TITLE);
            link.removeProperty(PROPERTY_UPLOADER);
            link.removeProperty(PROPERTY_UPLOADER_URL);
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* Only login captcha sometimes */
        return false;
    }
}