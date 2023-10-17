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
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
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
import jd.plugins.decrypter.FlickrComCrawler;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "https?://(?:www\\.)?flickr\\.com/photos/([^<>\"/]+)/(\\d+)(?:/in/album-\\d+|/in/gallery-\\d+@N\\d+-\\d+)?" })
public class FlickrCom extends PluginForHost {
    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
        /**
         * Accounts are required to be able to download some mature content and/or content that is private (can only be viewed by the user
         * who has added the account) and some "moderated" content.
         */
        this.enablePremium("https://edit.yahoo.com/registration?.src=flickrsignup");
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
    }

    @Override
    public String getAGBLink() {
        return "https://www.flickr.com/help/terms";
    }

    /* Settings */
    private static final String SETTING_FAST_LINKCHECK                  = "FAST_LINKCHECK";
    private static final String SETTING_SELECTED_PHOTO_QUALITY          = "SELECTED_PHOTO_QUALITY";
    private static final String SETTING_SELECTED_VIDEO_QUALITY          = "SELECTED_VIDEO_QUALITY";
    public static final String  CUSTOM_DATE                             = "CUSTOM_DATE";
    private static final String CUSTOM_FILENAME                         = "CUSTOM_FILENAME";
    public static final String  CUSTOM_PACKAGENAME_SET_GALLERY          = "CUSTOM_PACKAGENAME_SET_GALLERY";
    public static final String  CUSTOM_PACKAGENAME_OTHERS               = "CUSTOM_PACKAGENAME_OTHERS";
    private static final String CUSTOM_EMPTY_TAG_STRING                 = "CUSTOM_EMPTY_TAG_STRING";
    public static final String  PROPERTY_EXT                            = "ext";
    public static final String  PROPERTY_USERNAME_INTERNAL              = "username_internal";
    public static final String  PROPERTY_USERNAME                       = "username";
    public static final String  PROPERTY_USERNAME_FULL                  = "username_full";
    public static final String  PROPERTY_USERNAME_URL                   = "username_url";
    public static final String  PROPERTY_REAL_NAME                      = "real_name";
    public static final String  PROPERTY_CONTENT_ID                     = "content_id";
    public static final String  PROPERTY_SET_ID                         = "set_id";                                                                    // set/album
    public static final String  PROPERTY_GALLERY_ID                     = "gallery_id";                                                                // gallery
    // id
    public static final String  PROPERTY_DATE                           = "dateadded";                                                                 // timestamp
    /* pre-formatted string */
    public static final String  PROPERTY_DATE_TAKEN                     = "date_taken";
    public static final String  PROPERTY_TITLE                          = "title";
    public static final String  PROPERTY_ORDER_ID                       = "order_id";
    public static final String  PROPERTY_MEDIA_TYPE                     = "media";
    private static final String PROPERTY_SETTING_PREFER_SERVER_FILENAME = "prefer_server_filename";
    public static final String  PROPERTY_QUALITY                        = "quality";
    /* required e.g. to download video streams */
    public static final String  PROPERTY_SECRET                         = "secret";
    public static final String  PROPERTY_DIRECTURL                      = "directurl_%s";
    public static final String  PROPERTY_ACCOUNT_CSRF                   = "csrf";
    public static final String  PROPERTY_ACCOUNT_USERNAME_INTERNAL      = "username_internal";
    private static final String TYPE_PHOTO                              = "https?://[^/]+/photos/([^<>\"/]+)/(\\d+)$";
    private static final String TYPE_PHOTO_AS_PART_OF_SET               = "https?://[^/]+/photos/([^<>\"/]+)/(\\d+)/in/album-(\\d+)/?$";
    private static final String TYPE_PHOTO_AS_PART_OF_GALLERY           = "https?://[^/]+/photos/([^<>\"/]+)/(\\d+)/in/gallery-(\\d+@N\\d+)-(\\d+)/?$";

    /** Max 2000 requests per hour. */
    @Override
    public void init() {
        try {
            Browser.setBurstRequestIntervalLimitGlobal(this.getHost(), 3000, 20, 1900);
        } catch (final Throwable t) {
            Browser.setRequestIntervalLimitGlobal(this.getHost(), 1800);
        }
    }

    public static final Browser prepBrowser(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    private String getUsername(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getPhotoURLWithoutAlbumOrGalleryInfo(final DownloadLink link) throws PluginException {
        final String ret = new Regex(link.getPluginPatternMatcher(), "(?i)(https?://[^/]+/photos/[^<>\"/]+/\\d+)").getMatch(0);
        if (ret == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getPublicAPIKey(final Browser br) throws IOException {
        return FlickrComCrawler.getPublicAPIKey(this, br);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, aa, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        prepBrowser(this.br);
        correctDownloadLink(link);
        if (!link.isNameSet()) {
            /* Set fallback name */
            if (isVideo(link)) {
                link.setName(this.getFID(link) + ".mp4");
            } else {
                link.setName(this.getFID(link));
            }
        }
        /* Set some properties needed for custom filenames/Packagizer! */
        final String usernameFromURL = this.getUsername(link);
        /* Determine which type of username is inside the URL. */
        if (FlickrComCrawler.looksLikeInternalUsername(usernameFromURL)) {
            link.setProperty(PROPERTY_USERNAME_INTERNAL, usernameFromURL);
        } else {
            link.setProperty(PROPERTY_USERNAME, usernameFromURL);
        }
        link.setProperty(PROPERTY_USERNAME_URL, usernameFromURL);
        final String setID = new Regex(link.getPluginPatternMatcher(), TYPE_PHOTO_AS_PART_OF_SET).getMatch(2);
        if (setID != null) {
            link.setProperty(PROPERTY_SET_ID, setID);
        }
        final String galleryID = new Regex(link.getPluginPatternMatcher(), TYPE_PHOTO_AS_PART_OF_GALLERY).getMatch(3);
        if (galleryID != null) {
            link.setProperty(PROPERTY_GALLERY_ID, galleryID);
        }
        /* Picture direct-URLs are static --> Rely on them. */
        final String storedDirecturl = getStoredDirecturl(link);
        if (storedDirecturl != null) {
            logger.info("Doing linkcheck via directurl");
            if (checkDirecturl(link, storedDirecturl)) {
                logger.info("Linkcheck via directurl successful");
                return AvailableStatus.TRUE;
            } else {
                logger.info("Linkcheck via directurl failed --> Full linkcheck needed to get fresh directurl");
            }
        }
        if (account != null) {
            login(account, false);
        }
        /* 2021-09-17: Prefer API over website. */
        final boolean useAPI = true;
        if (useAPI) {
            availablecheckAPI(link, account);
        } else {
            availablecheckWebsite(link, account);
        }
        final String directurl;
        if (isVideo(link) && (isDownload || !this.getPluginConfig().getBooleanProperty(SETTING_FAST_LINKCHECK, default_SETTING_FAST_LINKCHECK))) {
            directurl = getVideoDownloadurlAPI(link, account);
        } else {
            directurl = getStoredDirecturl(link);
        }
        setFilename(link);
        if (!StringUtils.isEmpty(directurl) && !isDownload && allowDirecturlCheckForFilesize(link)) {
            checkDirecturl(link, directurl);
        }
        return AvailableStatus.TRUE;
    }

    /** Sets filename according to user preferences. */
    public static void setFilename(final DownloadLink link) throws ParseException {
        final String directurl = getStoredDirecturl(link);
        String filenameURL = null;
        if (directurl != null && !isVideo(link)) {
            filenameURL = new Regex(directurl, "(?i)https?://live\\.staticflickr\\.com/\\d+/([^/]+)").getMatch(0);
        }
        if (userPrefersServerFilenames() && filenameURL != null) {
            link.setFinalFileName(filenameURL);
        } else {
            link.setFinalFileName(getFormattedFilename(link));
        }
    }

    /** Checks single video/photo via website and sets required DownloadLink properties. */
    @Deprecated
    private void availablecheckWebsite(final DownloadLink link, final Account account) throws Exception {
        /* 2021-09-13: Don't do this anymore as it may not always work for videos! */
        // br.getPage(https://www.flickr.com/photos/<pathAlias>/<photoID> + "/in/photostream");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("div class=\"Four04Case\">") || br.containsHTML("(?i)>\\s*This member is no longer active on Flickr") || br.containsHTML("class=\"Problem\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /* This can also happen when user is logged in --> This then probably means that that is private content. */
            throw new AccountRequiredException();
        } else if (br.getURL().contains("login.yahoo.com/config")) {
            throw new AccountRequiredException();
        }
        /* Collect metadata (needed for custom filenames) */
        final boolean collectMetadataFromHTML = false;
        if (collectMetadataFromHTML) {
            String title = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("<title>(.*?) \\| Flickr \\- Photo Sharing\\!</title>").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("<meta name=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("<title>([^<>]+)\\| Flickr</title").getMatch(0);
            }
            /* Username used inside URL for this item */
            final String usernameFromHTML = br.getRegex("id=\"canonicalurl\"[^>]*href=\"https?://[^/]+/photos/([^/]+)/").getMatch(0);
            if (usernameFromHTML != null) {
                /* Overwrite property! */
                if (FlickrComCrawler.looksLikeInternalUsername(usernameFromHTML)) {
                    link.setProperty(PROPERTY_USERNAME_INTERNAL, usernameFromHTML);
                } else {
                    link.setProperty(PROPERTY_USERNAME, usernameFromHTML);
                }
            }
            final String usernameFull = br.getRegex("class=\"owner-name truncate\"[^>]*data-track=\"attributionNameClick\">([^<]+)</a>").getMatch(0);
            setStringProperty(this, link, PROPERTY_USERNAME_FULL, usernameFull, false);
            /* Do not overwrite property as crawler is getting this information more reliably as it is using their API! */
            setStringProperty(this, link, PROPERTY_TITLE, title, false);
            final String uploadedDate = PluginJSonUtils.getJsonValue(br, "datePosted");
            if (uploadedDate != null && uploadedDate.matches("\\d+")) {
                link.setProperty(PROPERTY_DATE, Long.parseLong(uploadedDate) * 1000);
            }
        }
        link.setProperty(PROPERTY_CONTENT_ID, getFID(link));
        if (!link.hasProperty(PROPERTY_MEDIA_TYPE)) {
            /* Fallback */
            if (br.containsHTML("class=\"videoplayer main\\-photo\"")) {
                link.setProperty(PROPERTY_MEDIA_TYPE, "video");
            } else {
                link.setProperty(PROPERTY_MEDIA_TYPE, "photo");
            }
        }
        final PhotoQuality preferredPhotoQuality = getPreferredPhotoQuality(link);
        final String json = br.getRegex("main\":(\\{\"photo-models\".*?),\\s+auth: auth,").getMatch(0);
        if (json != null) {
            /* json handling */
            Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            final List<Object> photoModels = (List) root.get("photo-models");
            final Map<String, Object> photoData = (Map<String, Object>) photoModels.get(0);
            final Map<String, Object> owner = (Map<String, Object>) photoData.get("owner");
            setStringProperty(this, link, PROPERTY_USERNAME, (String) owner.get("pathAlias"), false);
            /*
             * This might be confusing but their fields are different in API/website! E.g. API.ownername == Website.realname --> Both really
             * is the full username (not to be mistaken with the real name of the uploader!!)
             */
            setStringProperty(this, link, PROPERTY_USERNAME_FULL, (String) owner.get("realname"), false);
            if (!link.hasProperty(PROPERTY_USERNAME_INTERNAL)) {
                link.setProperty(PROPERTY_USERNAME_INTERNAL, owner.get("id"));
            }
            setStringProperty(this, link, PROPERTY_REAL_NAME, (String) owner.get("username"), false);
            final String secret = (String) photoData.get("secret");
            if (!StringUtils.isEmpty(secret)) {
                setStringProperty(this, link, PROPERTY_SECRET, secret, false);
            }
            setStringProperty(this, link, PROPERTY_TITLE, (String) photoData.get("title"), false);
            String description = (String) photoData.get("description");
            if (description != null) {
                setStringProperty(this, link, DownloadLink.PROPERTY_COMMENT, description, false);
            }
            /* 2021-09-23: Seems like this is only given for video items(?) */
            final Object mediaTypeO = photoData.get("mediaType");
            if (mediaTypeO != null) {
                setStringProperty(this, link, PROPERTY_MEDIA_TYPE, mediaTypeO.toString(), false);
            }
            {
                /* This block solely exists to find the uploaded-timestamp. */
                final List<Object> photoStatsModels = (List) root.get("photo-stats-models");
                final Map<String, Object> photoStatsData = (Map<String, Object>) photoStatsModels.get(0);
                final long datePosted = JavaScriptEngineFactory.toLong(photoStatsData.get("datePosted"), 0);
                if (datePosted > 0) {
                    link.setProperty(PROPERTY_DATE, datePosted * 1000);
                }
                /*
                 * If isDateTakenUnknown == true they will simply but the date when this item was uploaded into "isDateTakenUnknown" so
                 * effectively there is not "date taken" available!
                 */
                final boolean isDateTakenUnknown = ((Boolean) photoStatsData.get("isDateTakenUnknown")).booleanValue();
                if (!isDateTakenUnknown) {
                    final String dateTaken = (String) photoStatsData.get("dateTaken");
                    setDateTakenProperty(link, dateTaken);
                }
            }
            /* Get metadata: This way is safer than via html and it will return more information! */
            final Map<String, Object> photoSizes = (Map<String, Object>) photoData.get("sizes");
            final Iterator<Entry<String, Object>> iterator = photoSizes.entrySet().iterator();
            long maxWidth = -1;
            String maxQualityName = null;
            String maxQualityDownloadurl = null;
            String userPreferredQualityName = null;
            String userPreferredDownloadurl = null;
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                root = (Map<String, Object>) entry.getValue();
                String url = (String) root.get("url");
                final String qualityName = entry.getKey();
                final long width = JavaScriptEngineFactory.toLong(root.get("width"), 0);
                if (StringUtils.isEmpty(url)) {
                    /* Skip invalid items */
                    continue;
                }
                /* Fix URL */
                if (!url.startsWith("http")) {
                    url = "https:" + url;
                }
                if (this.stringToPhotoQuality(qualityName) == preferredPhotoQuality) {
                    logger.info("Found user preferred quality: " + qualityName);
                    userPreferredQualityName = qualityName;
                    userPreferredDownloadurl = url;
                    break;
                } else if (width > maxWidth) {
                    maxQualityName = qualityName;
                    maxWidth = width;
                    maxQualityDownloadurl = url;
                }
            }
            if (userPreferredDownloadurl != null) {
                logger.info("Using user preferred quality: " + userPreferredQualityName);
                link.setProperty(PROPERTY_QUALITY, userPreferredQualityName);
                link.setProperty(String.format(PROPERTY_DIRECTURL, link.getStringProperty(userPreferredQualityName)), userPreferredDownloadurl);
            } else if (maxQualityDownloadurl != null) {
                logger.info("Using best quality: " + maxQualityName + " | width: " + maxWidth);
                link.setProperty(PROPERTY_QUALITY, maxQualityName);
                link.setProperty(String.format(PROPERTY_DIRECTURL, maxQualityName), maxQualityDownloadurl);
            }
        } else {
            logger.warning("Failed to find json in html");
            if (!isVideo(link)) {
                /* Old website handling */
                /*
                 * Fast way to get finallink via site as we always try to access the "o" (original) quality. Page might be redirected!
                 */
                br.getPage("/photos/" + getUsername(link) + "/" + getFID(link) + "/sizes/o");
                /* Special case: Check if user prefers to download original quality */
                String directurl = null;
                if (preferredPhotoQuality == PhotoQuality.QO) {
                    if (br.getURL().contains("sizes/o")) { // Not redirected
                        directurl = br.getRegex("<a href=\"([^<>\"]+)\">\\s*(Dieses Foto im Originalformat|Download the Original)").getMatch(0);
                    }
                }
                if (directurl != null) {
                    link.setProperty(PROPERTY_QUALITY, "o");
                    link.setProperty(String.format(PROPERTY_DIRECTURL, "o"), directurl);
                } else { // Redirected if download original is not allowed
                    /*
                     * If it is redirected, get the highest available quality
                     */
                    final String[] qualities = getPhotoQualityStringsDescending();
                    final String html = br.getRegex("<ol class=\"sizes-list\">(.*?)<div id=\"allsizes-photo\">").getMatch(0);
                    String maxQualityName = null;
                    String foundUserPreferredQualityName = null;
                    for (final String qualityName : qualities) {
                        final String sizeAvailable = new Regex(html, "(?i)\"(/photos/[^/]+/\\d+/sizes/" + qualityName + "/)\"").getMatch(0);
                        if (sizeAvailable != null) {
                            /* First found = best */
                            if (maxQualityName == null) {
                                maxQualityName = qualityName;
                            }
                            if (this.stringToPhotoQuality(qualityName) == preferredPhotoQuality) {
                                foundUserPreferredQualityName = qualityName;
                                break;
                            }
                        }
                    }
                    if (maxQualityName != null || foundUserPreferredQualityName != null) {
                        final String selectedQualityName;
                        if (foundUserPreferredQualityName != null) {
                            logger.info("Fond user preferred quality: " + foundUserPreferredQualityName);
                            selectedQualityName = foundUserPreferredQualityName;
                        } else {
                            logger.info("Using best quality: " + maxQualityName);
                            selectedQualityName = maxQualityName;
                        }
                        br.getPage(this.getPhotoURLWithoutAlbumOrGalleryInfo(link) + "/sites/" + selectedQualityName + "/");
                        directurl = br.getRegex("id=\"allsizes-photo\">[^~]*?<img src=\"(http[^<>\"]*?)\"").getMatch(0);
                        if (directurl != null) {
                            link.setProperty(PROPERTY_QUALITY, selectedQualityName);
                            link.setProperty(String.format(PROPERTY_DIRECTURL, selectedQualityName), directurl);
                        } else {
                            /* This should never happen */
                            logger.warning("Website quality picker appears to be broken");
                        }
                    } else {
                        /* This should never happen */
                        logger.warning("Failed to find any photo quality");
                    }
                }
            }
        }
    }

    /** Checks single video/photo via API and sets required DownloadLink properties. */
    private void availablecheckAPI(final DownloadLink link, final Account account) throws Exception {
        final UrlQuery query = new UrlQuery();
        query.add("api_key", this.getPublicAPIKey(br));
        query.add("extras", getApiParamExtras());
        query.add("format", "json");
        query.add("hermes", "1");
        query.add("hermesClient", "1");
        query.add("nojsoncallback", "1");
        if (account != null) {
            query.add("csrf", Encoding.urlEncode(account.getStringProperty(PROPERTY_ACCOUNT_CSRF)));
        } else {
            query.add("csrf", "");
        }
        query.add("method", "flickr.photos.getInfo");
        query.add("photo_id", this.getFID(link));
        br.getPage(FlickrComCrawler.API_BASE + "services/rest?" + query.toString());
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        /*
         * Compared to the website, this API will return offline status for private files too while website would return error 403. We don't
         * care about it as it#s an edge case anyways!
         */
        /* E.g. {"stat":"fail","code":1,"message":"Photo \"<content_id>\" not found (invalid ID)"} */
        if (StringUtils.equalsIgnoreCase(entries.get("stat").toString(), "fail")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> photo = (Map<String, Object>) entries.get("photo");
        parseInfoAPI(this, link, photo);
    }

    private String getVideoDownloadurlAPI(final DownloadLink link, final Account account) throws PluginException, IOException {
        /* Video */
        final String secret = link.getStringProperty(PROPERTY_SECRET);
        if (StringUtils.isEmpty(secret)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser apibr = br.cloneBrowser();
        final UrlQuery query = new UrlQuery();
        query.add("photo_id", getFID(link));
        query.add("secret", secret);
        query.add("method", "flickr.video.getStreamInfo");
        if (account != null) {
            query.add("csrf", Encoding.urlEncode(account.getStringProperty(PROPERTY_ACCOUNT_CSRF)));
        } else {
            query.add("csrf", "");
        }
        query.add("api_key", getPublicAPIKey(this.br));
        query.add("format", "json");
        query.add("hermes", "1");
        query.add("hermesClient", "1");
        query.add("nojsoncallback", "1");
        apibr.getPage(FlickrComCrawler.API_BASE + "services/rest?" + query.toString());
        Map<String, Object> entries = restoreFromString(apibr.toString(), TypeRef.MAP);
        /*
         * 2021-09-09: Found 2 video types so far: "700" and "iphone_wifi" --> Both are equal in filesize. If more are available,
         * implementing a quality selection for videos could make sense.
         */
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "streams/stream");
        if (streams.isEmpty()) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "No streams available");
        }
        /* Find user preferred quality. */
        String bestQualityURL = null;
        String bestQualityName = null;
        String userPreferredQualityURL = null;
        String userPreferredQualityName = null;
        final VideoQuality userPreferredVideoQuality = getPreferredVideoQuality();
        for (final Map<String, Object> stream : streams) {
            /* type can sometimes be represented as an Integer. */
            final String qualityName = stream.get("type").toString();
            final String url = (String) stream.get("_content");
            // if (qualityName.equals("700") || qualityName.equalsIgnoreCase("iphone_wifi")) {
            if (qualityName.equalsIgnoreCase("iphone_wifi")) {
                continue;
            } else if (StringUtils.isEmpty(url)) {
                continue;
            }
            if (bestQualityURL == null) {
                /* List is sorted from best to worst -> Set best first */
                bestQualityURL = url;
                bestQualityName = qualityName;
            }
            if (stringToVideoQuality(qualityName) == userPreferredVideoQuality) {
                userPreferredQualityURL = url;
                userPreferredQualityName = qualityName;
                break;
            }
        }
        if (userPreferredQualityURL != null) {
            logger.info("Found user preferred quality: " + userPreferredQualityName);
            link.setProperty(PROPERTY_QUALITY, userPreferredQualityName);
            link.setProperty(String.format(PROPERTY_DIRECTURL, userPreferredQualityName), userPreferredQualityURL);
            return userPreferredQualityURL;
        } else if (bestQualityURL != null) {
            logger.info("Failed to find user preferred quality " + userPreferredVideoQuality.getLabel() + " - using this one instead: " + bestQualityName);
            link.setProperty(PROPERTY_QUALITY, bestQualityName);
            link.setProperty(String.format(PROPERTY_DIRECTURL, bestQualityName), bestQualityURL);
            return bestQualityURL;
        } else {
            /* This should either never happen or be a very rare case. */
            logger.warning("Failed to find any usable video stream --> Only broken streams available?");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?");
        }
    }

    /** Returns API parameters "extras" containing the needed extra properties for images/videos. */
    public static final String getApiParamExtras() {
        /**
         * needs_interstitial = show 18+ content </br>
         * media = include media-type (video/photo) </br>
         * datecreate = get create-date of groups </br>
         * date_taken = date_taken of photos </br>
         * date_activity = timestamp of the last activity of a group (given via "dateactivity/_content") </br>
         */
        String extras = "date_activity%2Cdatecreate%2Cdate_taken%2Cdate_upload%2Cdescription%2Cowner_name%2Cpath_alias%2Crealname%2Cneeds_interstitial%2Cmedia";
        final String[] allPhotoQualities = getPhotoQualityStringsDescending();
        for (final String qualityStr : allPhotoQualities) {
            extras += "%2Curl_" + qualityStr;
        }
        return extras;
    }

    /** Finds and sets all required information for single photo/video returned by previously done flickr API request. */
    public static void parseInfoAPI(final Plugin plg, final DownloadLink link, final Map<String, Object> photo) {
        /* This map is now always given. Information can alsoi be in main ("photo") map. */
        final Map<String, Object> dates = (Map<String, Object>) photo.get("dates");
        final String userPreferredPhotoQualityStr = photoQualityToQualityString(jd.plugins.hoster.FlickrCom.getPreferredPhotoQuality());
        final String thisUsernameSlug = (String) photo.get("pathalias");
        /*
         * This can be a map containing more information about the owner when json is obtained via API method=flickr.photos.getInfo, else it
         * is a string.
         */
        final Object ownerO = photo.get("owner");
        String thisUsernameInternal = null;
        if (ownerO instanceof String) {
            thisUsernameInternal = (String) ownerO;
        } else if (ownerO instanceof Map) {
            final Map<String, Object> owner = (Map<String, Object>) ownerO;
            thisUsernameInternal = (String) owner.get("nsid");
        }
        final String thisUsernameFull = (String) photo.get("realname");
        final String realName = (String) photo.get("ownername");
        final String photoID = photo.get("id").toString();
        final Object titleO = photo.get("title");
        String title = null;
        if (titleO instanceof String) {
            title = (String) titleO;
        } else {
            title = (String) JavaScriptEngineFactory.walkJson(titleO, "_content");
        }
        final String dateUploaded = photo.get("dateupload").toString();
        final String description = (String) JavaScriptEngineFactory.walkJson(photo, "description/_content");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(Encoding.htmlDecode(description));
        }
        /*
         * 2021-09-14: There is also a field media_status=ready --> Maybe indicates if an item is down or needs processing (e.g. videos)?
         */
        final String media = (String) photo.get("media");
        // final String originalformat = (String) photo.get("originalformat");
        final String extension;
        if (media.equalsIgnoreCase("video")) {
            extension = ".mp4";
            final String secret = (String) photo.get("secret");
            if (!StringUtils.isEmpty(secret)) {
                link.setProperty(PROPERTY_SECRET, secret);
            }
        } else {
            extension = ".jpg";
            /* Try to find photo directurl right away */
            String maxQualityName = null;
            String maxQualityDownloadurl = null;
            String userPreferredQualityDownloadurl = null;
            final String[] allPhotoQualities = getPhotoQualityStringsDescending();
            for (final String qualityStr : allPhotoQualities) {
                final String url = (String) photo.get("url_" + qualityStr);
                if (url == null) {
                    continue;
                }
                /* First found = best */
                if (maxQualityDownloadurl == null) {
                    maxQualityDownloadurl = url;
                    maxQualityName = qualityStr;
                }
                if (qualityStr.equalsIgnoreCase(userPreferredPhotoQualityStr)) {
                    /* Quit loop as this is the quality our user wants to have. */
                    userPreferredQualityDownloadurl = url;
                    break;
                }
            }
            /* Check if we found anything and set to re-use later. */
            if (!StringUtils.isEmpty(maxQualityDownloadurl) || !StringUtils.isEmpty(userPreferredQualityDownloadurl)) {
                final String url;
                final String chosenQualityStr;
                if (userPreferredQualityDownloadurl != null) {
                    url = userPreferredQualityDownloadurl;
                    chosenQualityStr = userPreferredPhotoQualityStr;
                } else {
                    url = maxQualityDownloadurl;
                    chosenQualityStr = maxQualityName;
                }
                link.setProperty(String.format(jd.plugins.hoster.FlickrCom.PROPERTY_DIRECTURL, chosenQualityStr), url);
                link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_QUALITY, chosenQualityStr);
            }
        }
        link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_CONTENT_ID, photoID);
        link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_MEDIA_TYPE, media);
        {
            /* Overwrite previously set properties if our "photo" object has them too as we can trust those ones 100%. */
            setStringProperty(plg, link, jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME, thisUsernameSlug, true);
            setStringProperty(plg, link, jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_FULL, thisUsernameFull, true);
            setStringProperty(plg, link, jd.plugins.hoster.FlickrCom.PROPERTY_REAL_NAME, realName, true);
            link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_USERNAME_INTERNAL, thisUsernameInternal);
        }
        link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_DATE, Long.parseLong(dateUploaded) * 1000);
        final int takenunknown;
        if (dates != null) {
            takenunknown = Integer.parseInt(dates.get("takenunknown").toString());
        } else {
            takenunknown = Integer.parseInt(photo.get("datetakenunknown").toString());
        }
        /*
         * 1 = no "date taken" is known and they will simply put the date when the item was uploaded into "datetaken" --> We do not set it
         * at all in this case.
         */
        if (takenunknown == 0) {
            setDateTakenProperty(link, (String) photo.get("datetaken"));
        }
        setStringProperty(plg, link, jd.plugins.hoster.FlickrCom.PROPERTY_TITLE, title, false);
        link.setProperty(jd.plugins.hoster.FlickrCom.PROPERTY_EXT, extension);
    }

    public static void setDateTakenProperty(final DownloadLink link, String dateTakenStr) {
        if (dateTakenStr == null) {
            return;
        }
        if (dateTakenStr.contains("%")) {
            dateTakenStr = Encoding.htmlDecode(dateTakenStr);
        }
        if (dateTakenStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            /* Convert to milliseconds */
            link.setProperty(PROPERTY_DATE_TAKEN, TimeFormatter.getMilliSeconds(dateTakenStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else {
            /* Set raw String as fallback -> This should never happen */
            link.setProperty(PROPERTY_DATE_TAKEN, dateTakenStr);
        }
    }

    public static boolean setStringProperty(final Plugin plugin, final DownloadLink link, final String property, String value, final boolean overwrite) {
        if ((overwrite || !link.hasProperty(property)) && !StringUtils.isEmpty(value)) {
            final String decodedValue = decodeEncoding(plugin, property, value);
            if (!StringUtils.isEmpty(decodedValue)) {
                link.setProperty(property, decodedValue);
                return true;
            }
        }
        return false;
    }

    public static String decodeEncoding(final Plugin plugin, final String property, final String value) {
        if (value != null) {
            String decodedValue = Encoding.unicodeDecode(value);
            try {
                decodedValue = URLEncode.decodeURIComponent(decodedValue, new URLEncode.Decoder() {
                    @Override
                    public String decode(String value) throws UnsupportedEncodingException {
                        try {
                            String ret = URLDecoder.decode(value, "UTF-8");
                            if (ret.contains("\ufffd")) {
                                // REPLACEMENT CHARACTER
                                ret = URLDecoder.decode(value, "ISO-8859-1");
                            }
                            return ret;
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(e);
                            return value;
                        }
                    }
                });
            } catch (UnsupportedEncodingException e) {
                plugin.getLogger().log(e);
            }
            decodedValue = Encoding.htmlOnlyDecode(decodedValue);
            return decodedValue.trim();
        } else {
            return null;
        }
    }

    private boolean checkDirecturl(final DownloadLink link, final String directurl) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            con = brc.openHeadConnection(directurl);
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return true;
            } else {
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        if (StringUtils.equals(link.getStringProperty(PROPERTY_MEDIA_TYPE), "video")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean allowDirecturlCheckForFilesize(final DownloadLink link) {
        if (isVideo(link)) {
            return true;
        } else {
            /* 2021-09-17: Content-Length header not always given for images. */
            return false;
        }
    }

    public static String getStoredDirecturl(final DownloadLink link) {
        return link.getStringProperty(getDirecturlProperty(link));
    }

    public static String getDirecturlProperty(final DownloadLink link) {
        return String.format(PROPERTY_DIRECTURL, link.getStringProperty(PROPERTY_QUALITY, getPreferredQualityStr(link)));
    }

    /** Returns preferred video/photo quality as string. */
    private static String getPreferredQualityStr(final DownloadLink link) {
        if (isVideo(link)) {
            return videoQualityToQualityString(getPreferredVideoQuality());
        } else {
            return photoQualityToQualityString(getPreferredPhotoQuality());
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, account, true);
            final String directurl = getStoredDirecturl(link);
            if (StringUtils.isEmpty(directurl)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* chunked transfer */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, isResumable(link), getMaxChunks(link));
            connectionErrorhandling(dl.getConnection());
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dl.startDownload()) {
            /*
             * 2016-08-19: Detect "Temporarily unavailable" message inside downloaded picture via md5 hash of the file:
             * https://board.jdownloader.org/showthread.php?t=70487
             */
            boolean isTempUnavailable = false;
            try {
                isTempUnavailable = "e60b98765d26e34bfbb797c1a5f378f2".equalsIgnoreCase(JDHash.getMD5(new File(link.getFileOutput())));
            } catch (final Throwable ignore) {
            }
            if (isTempUnavailable) {
                /* Reset progress */
                link.setDownloadCurrent(0);
                /* Size unknown */
                link.setDownloadSize(0);
                errorBrokenImage();
            }
        }
    }

    private static void connectionErrorhandling(final URLConnectionAdapter con) throws PluginException {
        if (StringUtils.containsIgnoreCase(con.getURL().toString(), "/photo_unavailable.gif")) {
            con.disconnect();
            errorBrokenImage();
        }
    }

    private static void errorBrokenImage() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken image?");
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = getStoredDirecturl(link);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        final Browser brc = br.cloneBrowser();
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumable(link), this.getMaxChunks(link));
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
        connectionErrorhandling(dl.getConnection());
        if (this.looksLikeDownloadableContent(dl.getConnection())) {
            return true;
        } else {
            /* Delete stored URL so it won't be tried again. */
            link.removeProperty(getDirecturlProperty(link));
            brc.followConnection(true);
            throw new IOException();
        }
    }

    private boolean isResumable(final DownloadLink link) {
        // if (isVideo(link)) {
        // return true;
        // } else {
        // return true;
        // }
        /*
         * 2021-09-22: Videos are always resumable. For photos it varies but upper handling correctly auto-detects it so let's allow resume
         * in general.
         */
        return true;
    }

    private int getMaxChunks(final DownloadLink link) {
        if (isVideo(link)) {
            /* Unlimited */
            return 0;
        } else {
            /*
             * Max 1 as chunkload is not possible for all images and most times these are small times so chunkload won't help that much to
             * get better download speeds.
             */
            return 1;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies == null) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            br.setCookies(userCookies);
            if (!force) {
                /* Trust cookies without check */
                return;
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/");
            final String loginjson = br.getRegex("root\\.auth\\s*=\\s*(\\{.*?\\});").getMatch(0);
            final Map<String, Object> rootAuth = JavaScriptEngineFactory.jsonToJavaMap(loginjson);
            final Map<String, Object> user = (Map<String, Object>) rootAuth.get("user");
            if (!(Boolean) rootAuth.get("signedIn")) {
                if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                }
            }
            final String csrf = (String) rootAuth.get("csrf");
            final String usernameInternal = (String) user.get("nsid");
            if (StringUtils.isEmpty(csrf) || StringUtils.isEmpty(usernameInternal)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (((Number) user.get("ispro")).intValue() == 1) {
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            /*
             * User can put anything into the "username" field when doing cookie login but we want unique usernames so let's set his
             * internal username as username.
             */
            account.setUser(usernameInternal);
            /* Save cookies and special tokens */
            account.setProperty(PROPERTY_ACCOUNT_CSRF, csrf);
            account.setProperty(PROPERTY_ACCOUNT_USERNAME_INTERNAL, usernameInternal);
        }
    }

    /** Returns formatted filename according to user preferences. */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("flickr.com");
        final String customStringForEmptyTags = getCustomStringForEmptyTags();
        final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
        final String formattedDate = formatToUserDefinedDate(link.getLongProperty(PROPERTY_DATE, 0), userDefinedDateFormat, customStringForEmptyTags);
        final String formattedDateTaken = formatToUserDefinedDate(link.getLongProperty(PROPERTY_DATE_TAKEN, 0), userDefinedDateFormat, customStringForEmptyTags);
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (StringUtils.isEmpty(formattedFilename)) {
            formattedFilename = defaultCustomFilename;
        } else {
            if (!formattedFilename.endsWith("*extension*")) {
                /* Fallback: Ensure that the user entered a somewhat 'valid' custom filename pattern - if not, use the default name */
                formattedFilename = defaultCustomFilename;
            }
        }
        formattedFilename = formattedFilename.replace("*content_id*", link.getStringProperty(PROPERTY_CONTENT_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*set_id*", link.getStringProperty(PROPERTY_SET_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*gallery_id*", link.getStringProperty(PROPERTY_GALLERY_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*order_id*", link.getStringProperty(PROPERTY_ORDER_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*quality*", link.getStringProperty(PROPERTY_QUALITY, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*date*", formattedDate);
        formattedFilename = formattedFilename.replace("*date_taken*", formattedDateTaken);
        formattedFilename = formattedFilename.replace("*media*", link.getStringProperty(PROPERTY_MEDIA_TYPE, customStringForEmptyTags));
        final String url = getStoredDirecturl(link);
        String extension = url != null ? getFileNameExtensionFromURL(url) : null;
        if (StringUtils.isEmpty(extension)) {
            extension = link.getStringProperty(PROPERTY_EXT, defaultPhotoExt);
        }
        formattedFilename = formattedFilename.replace("*extension*", extension);
        formattedFilename = formattedFilename.replace("*username*", link.getStringProperty(PROPERTY_USERNAME, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_url*", link.getStringProperty(PROPERTY_USERNAME_URL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_full*", link.getStringProperty(PROPERTY_USERNAME_FULL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_internal*", link.getStringProperty(PROPERTY_USERNAME_INTERNAL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*real_name*", link.getStringProperty(PROPERTY_REAL_NAME, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*title*", link.getStringProperty(PROPERTY_TITLE, customStringForEmptyTags));
        return formattedFilename;
    }

    public static String formatToUserDefinedDate(final long timestamp, final String targetFormat, final String fallbackStr) {
        if (timestamp > 0 && targetFormat != null) {
            Date theDate = new Date(timestamp);
            try {
                final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
                return formatter.format(theDate);
            } catch (final Exception ignore) {
                /* prevent user error killing the custom filename function. */
                return fallbackStr;
            }
        } else {
            return fallbackStr;
        }
    }

    public static boolean userPrefersServerFilenames() {
        return JDUtilities.getPluginForHost("flickr.com").getPluginConfig().getBooleanProperty(PROPERTY_SETTING_PREFER_SERVER_FILENAME, defaultPreferServerFilename);
    }

    private PhotoQuality getPreferredPhotoQuality(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_QUALITY)) {
            /* Return last saved quality. */
            return stringToPhotoQuality(link.getStringProperty(PROPERTY_QUALITY));
        } else {
            /* Return quality currently selected by user. */
            return getPreferredPhotoQuality();
        }
    }

    /** Returns quality currently selected by user. */
    public static PhotoQuality getPreferredPhotoQuality() {
        final int arrayPos = JDUtilities.getPluginForHost("flickr.com").getPluginConfig().getIntegerProperty(SETTING_SELECTED_PHOTO_QUALITY, defaultArrayPosSelectedPhotoQuality);
        if (arrayPos < PhotoQuality.values().length) {
            return PhotoQuality.values()[arrayPos];
        } else {
            return PhotoQuality.values()[defaultArrayPosSelectedPhotoQuality];
        }
    }

    private VideoQuality getPreferredVideoQuality(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_QUALITY)) {
            /* Return last saved quality. */
            return stringToVideoQuality(link.getStringProperty(PROPERTY_QUALITY));
        } else {
            /* Return quality currently selected by user. */
            return getPreferredVideoQuality();
        }
    }

    /** Returns quality currently selected by user. */
    public static VideoQuality getPreferredVideoQuality() {
        final int arrayPos = JDUtilities.getPluginForHost("flickr.com").getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_QUALITY, defaultArrayPosSelectedVideoQuality);
        if (arrayPos < VideoQuality.values().length) {
            return VideoQuality.values()[arrayPos];
        } else {
            return VideoQuality.values()[defaultArrayPosSelectedPhotoQuality];
        }
    }

    public static enum PhotoQuality implements LabelInterface {
        QO {
            @Override
            public String getLabel() {
                return "Original";
            }
        },
        Q6K {
            @Override
            public String getLabel() {
                return "X-Large 6K";
            }
        },
        Q5K {
            @Override
            public String getLabel() {
                return "X-Large 5K";
            }
        },
        Q4K {
            @Override
            public String getLabel() {
                return "X-Large 4K";
            }
        },
        Q3K {
            @Override
            public String getLabel() {
                return "X-Large 3K";
            }
        },
        QK {
            @Override
            public String getLabel() {
                return "Large 2048";
            }
        },
        QH {
            @Override
            public String getLabel() {
                return "Large 1600";
            }
        },
        QL {
            @Override
            public String getLabel() {
                return "Large 1024";
            }
        },
        QC {
            @Override
            public String getLabel() {
                return "Medium 800";
            }
        },
        QZ {
            @Override
            public String getLabel() {
                return "Medium 640";
            }
        },
        QM {
            @Override
            public String getLabel() {
                return "Medium 500";
            }
        },
        QW {
            @Override
            public String getLabel() {
                return "Small 400";
            }
        },
        QN {
            @Override
            public String getLabel() {
                return "Small 320";
            }
        },
        QS {
            @Override
            public String getLabel() {
                return "Small 240";
            }
        },
        QT {
            @Override
            public String getLabel() {
                return "Thumbnail";
            }
        },
        QQ {
            @Override
            public String getLabel() {
                return "Square 150";
            }
        },
        QSQ {
            @Override
            public String getLabel() {
                return "Square 75";
            }
        };
    }

    public static String[] getPhotoQualityStringsDescending() {
        final String[] ret = new String[PhotoQuality.values().length];
        for (int i = 0; i < PhotoQuality.values().length; i++) {
            ret[i] = photoQualityToQualityString(PhotoQuality.values()[i]);
        }
        return ret;
    }

    public static String photoQualityToQualityString(final PhotoQuality photoQuality) {
        return photoQuality.name().substring(1).toLowerCase(Locale.ENGLISH);
    }

    private PhotoQuality stringToPhotoQuality(final String str) {
        if (str == null) {
            return null;
        } else {
            for (final PhotoQuality quality : PhotoQuality.values()) {
                final String qualStr = photoQualityToQualityString(quality);
                if (qualStr.equalsIgnoreCase(str)) {
                    return quality;
                }
            }
            return null;
        }
    }

    private String[] getPhotoQualityLabels() {
        final PhotoQuality[] qualityValues = PhotoQuality.values();
        final String[] ret = new String[qualityValues.length];
        for (int i = 0; i < qualityValues.length; i++) {
            ret[i] = qualityValues[i].getLabel();
        }
        return ret;
    }

    public static enum VideoQuality implements LabelInterface {
        Q1080p {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q720p {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q360p {
            @Override
            public String getLabel() {
                return "360p";
            }
        };
    }

    public static String[] getVideoQualityStringsDescending() {
        final String[] ret = new String[VideoQuality.values().length];
        for (int i = 0; i < VideoQuality.values().length; i++) {
            ret[i] = videoQualityToQualityString(VideoQuality.values()[i]);
        }
        return ret;
    }

    public static String videoQualityToQualityString(final VideoQuality videoQuality) {
        return videoQuality.name().substring(1).toLowerCase(Locale.ENGLISH);
    }

    private VideoQuality stringToVideoQuality(final String str) {
        if (str == null) {
            return null;
        } else {
            for (final VideoQuality quality : VideoQuality.values()) {
                final String qualStr = videoQualityToQualityString(quality);
                if (qualStr.equalsIgnoreCase(str)) {
                    return quality;
                }
            }
            return null;
        }
    }

    private String[] getVideoQualityLabels() {
        final VideoQuality[] qualityValues = VideoQuality.values();
        final String[] ret = new String[qualityValues.length];
        for (int i = 0; i < qualityValues.length; i++) {
            ret[i] = qualityValues[i].getLabel();
        }
        return ret;
    }

    public static String getCustomStringForEmptyTags() {
        final SubConfiguration cfg = SubConfiguration.getConfig("flickr.com");
        String emptytag = cfg.getStringProperty(CUSTOM_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
        if (emptytag.equals("")) {
            emptytag = defaultCustomStringForEmptyTags;
        }
        return emptytag;
    }

    private static final boolean default_SETTING_FAST_LINKCHECK      = true;
    private static final int     defaultArrayPosSelectedPhotoQuality = 0;
    private static final int     defaultArrayPosSelectedVideoQuality = 0;
    private static final boolean defaultPreferServerFilename         = false;
    public static final String   defaultCustomDate                   = "MM-dd-yyyy";
    private static final String  defaultCustomFilename               = "*username_url*_*content_id*_*title**extension*";
    public static final String   defaultCustomPackagenameSetGallery  = "*type* - *username_url*_*set_or_gallery_id* - *title*";
    public static final String   defaultCustomPackagenameOthers      = "*type* - *username_url*";
    public final static String   defaultCustomStringForEmptyTags     = "-";
    public final static String   defaultPhotoExt                     = ".jpg";

    @Override
    public String getDescription() {
        return "JDownloader's flickr.com Plugin helps downloading media from flickr. Here you can define custom filenames.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_FAST_LINKCHECK, "Enable fast linkcheck for videos?\r\nFilesize won't be displayed until download is started.").setDefaultValue(default_SETTING_FAST_LINKCHECK));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_PHOTO_QUALITY, getPhotoQualityLabels(), "Select preferred photo quality. If that is not available, best will be used instead.").setDefaultValue(defaultArrayPosSelectedPhotoQuality));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_VIDEO_QUALITY, getVideoQualityLabels(), "Select preferred video quality. If that is not available, best will be used instead.").setDefaultValue(defaultArrayPosSelectedVideoQuality));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Define how dates inside filenames/packagenames should look like:").setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Packagename settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME_SET_GALLERY, "Custom packagename for sets/galleries:").setDefaultValue(defaultCustomPackagenameSetGallery));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME_OTHERS, "Custom packagename for users/user-favorites/groups:").setDefaultValue(defaultCustomPackagenameOthers));
        final StringBuilder tagsPackage = new StringBuilder();
        tagsPackage.append("Explanation of the available packagename tags:\r\n");
        tagsPackage.append("*type* = Type of url: One of the following: Set, Gallery, Groups, User, Favorites\r\n");
        tagsPackage.append("*total_number_of_items* = Total number of items\r\n");
        tagsPackage.append("*set_id* = ID of album\r\n");
        tagsPackage.append("*gallery_id* = ID of gallery\r\n");
        tagsPackage.append("*set_or_gallery_id* = ID of gallery or set\r\n");
        tagsPackage.append("*date* = Create date of gallery, set or group - custom date format will be used here\r\n");
        tagsPackage.append("*date_update* = Last updated date of gallery, set or group - custom date format will be used here\r\n");
        tagsPackage.append("*title* = Title of gallery or set\r\n");
        tagsPackage.append("*description* = Description of the gallery/set\r\n");
        tagsPackage.append("*username* = Short username e.g. 'exampleusername'\r\n");
        tagsPackage.append("*username_internal* = Internal username e.g. '12345678@N04'\r\n");
        tagsPackage.append("*username_full* = Full username e.g. 'Example Username'\r\n");
        tagsPackage.append("*username_url* = Username from inside URL - usually either the same value as 'username' or 'username_internal'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, tagsPackage.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings */
        final ConfigEntry preferServerFilenames = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_SETTING_PREFER_SERVER_FILENAME, "Prefer server filenames instead of formatted filenames (photos only) e.g. '11112222_574508fa345a_6k.jpg'?").setDefaultValue(defaultPreferServerFilename);
        getConfig().addEntry(preferServerFilenames);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Custom filename:").setDefaultValue(defaultCustomFilename).setEnabledCondidtion(preferServerFilenames, false));
        final StringBuilder tagsFile = new StringBuilder();
        tagsFile.append("Explanation of the available filename tags:\r\n");
        tagsFile.append("*content_id* = ID of the photo/video\r\n");
        tagsFile.append("*set_id* = ID of album if the photo/video is part of a crawled album or single photo/video URL contains album ID\r\n");
        tagsFile.append("*gallery_id* = ID of gallery if the photo/video is part of a crawled gallery or single photo/video URL contains gallery ID\r\n");
        tagsFile.append("*date* = date when the photo was uploaded - custom date format will be used here\r\n");
        tagsFile.append("*date_taken* = date when the photo was taken - custom date format will be used here\r\n");
        tagsFile.append("*extension* = Extension of the photo - usually '.jpg'\r\n");
        tagsFile.append("*media* = Media type ('video' or 'photo')\r\n");
        tagsFile.append("*order_id* = Position of image/video if it was part of a crawled gallery/user-profile\r\n");
        tagsFile.append("*quality* = Quality of the photo/video e.g. 'm' or '1080p'\r\n");
        tagsFile.append("*real_name* = Real name of the user (name and surname) e.g. 'Marcus Mueller'\r\n");
        tagsFile.append("*title* = Title of the photo\r\n");
        tagsFile.append("*username* = Short username e.g. 'exampleusername'\r\n");
        tagsFile.append("*username_internal* = Internal username e.g. '12345678@N04'\r\n");
        tagsFile.append("*username_full* = Full username e.g. 'Example Username'\r\n");
        tagsFile.append("*username_url* = Username from inside URL - usually either the same value as 'username' or 'username_internal'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, tagsFile.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_EMPTY_TAG_STRING, "Char which will be used for empty tags (e.g. missing data):").setDefaultValue(defaultCustomStringForEmptyTags).setEnabledCondidtion(preferServerFilenames, false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* Allow upper code to change to a different preferred quality whenever user resets DownloadLink. */
        link.removeProperty(PROPERTY_QUALITY);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }
}