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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.StringUtils;
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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "https?://(?:www\\.)?flickr\\.com/photos/(?!tags/)([^<>\"/]+)/(\\d+)(?:/in/album-\\d+)?" })
public class FlickrCom extends PluginForHost {
    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://edit.yahoo.com/registration?.src=flickrsignup");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://flickr.com";
    }

    /* Settings */
    private static final String            SETTING_SELECTED_PHOTO_QUALITY          = "SELECTED_PHOTO_QUALITY";
    private static final String            SETTING_SELECTED_VIDEO_QUALITY          = "SELECTED_VIDEO_QUALITY";
    private static final String            CUSTOM_DATE                             = "CUSTOM_DATE";
    private static final String            CUSTOM_FILENAME                         = "CUSTOM_FILENAME";
    private static final String            CUSTOM_FILENAME_EMPTY_TAG_STRING        = "CUSTOM_FILENAME_EMPTY_TAG_STRING";
    public static final String             PROPERTY_EXT                            = "ext";
    public static final String             PROPERTY_USERNAME_INTERNAL              = "username_internal";
    public static final String             PROPERTY_USERNAME                       = "username";
    public static final String             PROPERTY_USERNAME_FULL                  = "username_full";
    public static final String             PROPERTY_USERNAME_URL                   = "username_url";
    public static final String             PROPERTY_REAL_NAME                      = "real_name";
    public static final String             PROPERTY_CONTENT_ID                     = "content_id";
    public static final String             PROPERTY_SET_ID                         = "set_id";
    public static final String             PROPERTY_DATE                           = "dateadded";                       // timestamp
    /* pre-formatted string */
    public static final String             PROPERTY_DATE_TAKEN                     = "date_taken";
    public static final String             PROPERTY_TITLE                          = "title";
    public static final String             PROPERTY_ORDER_ID                       = "order_id";
    public static final String             PROPERTY_MEDIA_TYPE                     = "media";
    private static final String            PROPERTY_SETTING_PREFER_SERVER_FILENAME = "prefer_server_filename";
    public static final String             PROPERTY_QUALITY                        = "quality";
    public static final String             PROPERTY_DIRECTURL                      = "directurl_%s";
    private String                         dllink                                  = null;
    private static HashMap<String, Object> api                                     = new HashMap<String, Object>();

    /** Max 2000 requests per hour. */
    @Override
    public void init() {
        try {
            Browser.setBurstRequestIntervalLimitGlobal(this.getHost(), 3000, 20, 1900);
        } catch (final Throwable t) {
            Browser.setRequestIntervalLimitGlobal(this.getHost(), 1800);
        }
        /** Backward compatibility: TODO: Remove this in 01-2022 */
        final String userCustomFilenameMask = this.getPluginConfig().getStringProperty(CUSTOM_FILENAME);
        if (userCustomFilenameMask != null) {
            if (userCustomFilenameMask.contains("*owner*") || userCustomFilenameMask.contains("*photo_id*")) {
                String correctedUserCustomFilenameMask = userCustomFilenameMask.replace("*owner*", "*username_internal*");
                if (correctedUserCustomFilenameMask.contains("*photo_id*")) {
                    correctedUserCustomFilenameMask = correctedUserCustomFilenameMask.replace("*photo_id*", "*content_id*");
                } else if (!correctedUserCustomFilenameMask.contains("*content_id*") && correctedUserCustomFilenameMask.contains("*content_id")) {
                    /* Fix for mistage in rev 44961 */
                    correctedUserCustomFilenameMask = correctedUserCustomFilenameMask.replace("*content_id", "*content_id*");
                }
                getPluginConfig().setProperty(CUSTOM_FILENAME, correctedUserCustomFilenameMask);
            } else if (userCustomFilenameMask.equalsIgnoreCase("*username*_*content_id*_*title**extension*")) {
                /**
                 * 2021-09-14: Correct defaults just in case user has entered the field so the property has been saved. See new default in:
                 * defaultCustomFilename </br>
                 * username_url = always given </br>
                 * username = not always given but previously the same as new "username_url" and default.
                 */
                final String correctedUserCustomFilenameMask = userCustomFilenameMask.replace("*username*", "*username_url*");
                getPluginConfig().setProperty(CUSTOM_FILENAME, correctedUserCustomFilenameMask);
            }
        }
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

    private String getPhotoURLWithoutAlbumInfo(final DownloadLink link) throws PluginException {
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
        synchronized (api) {
            if (!api.containsKey("apikey") || !api.containsKey("timestamp") || System.currentTimeMillis() - ((Number) api.get("timestamp")).longValue() > 1 * 60 * 60 * 1000) {
                logger.info("apikey refresh required");
                final String apikey = jd.plugins.decrypter.FlickrCom.findPublicApikey(br);
                api.put("apikey", apikey);
                api.put("timestamp", System.currentTimeMillis());
            }
            return api.get("apikey").toString();
        }
    }

    /**
     * Keep in mind that there is this nice oauth API which might be useful in the future: https://www.flickr.com/services/oembed?url=
     *
     * Other calls of the normal API which might be useful in the future: https://www.flickr.com/services/api/flickr.photos.getInfo.html
     * https://www.flickr.com/services/api/flickr.photos.getSizes.html TODO API: Get correct csrf values so we can make requests as a
     * logged-in user
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, aa, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        correctDownloadLink(link);
        if (!link.isNameSet()) {
            /* Set fallback name */
            if (isVideo(link)) {
                link.setName(this.getFID(link) + ".mp4");
            } else {
                link.setName(this.getFID(link));
            }
        }
        if (link.hasProperty("owner")) {
            /** Backward compatibility: TODO: Remove this in 01-2022 */
            link.setProperty(PROPERTY_USERNAME_INTERNAL, link.getStringProperty("owner"));
            link.removeProperty("owner");
        }
        /* Needed for custom filenames! */
        final String usernameFromURL = this.getUsername(link);
        /* Determine which type of username is inside the URL. */
        if (jd.plugins.decrypter.FlickrCom.looksLikeInternalUsername(usernameFromURL)) {
            link.setProperty(PROPERTY_USERNAME_INTERNAL, usernameFromURL);
        } else {
            link.setProperty(PROPERTY_USERNAME, usernameFromURL);
        }
        link.setProperty(PROPERTY_USERNAME_URL, usernameFromURL);
        br.setFollowRedirects(true);
        /* Picture direct-URLs are static --> Rely on them. */
        final String storedDirecturl = getStoredDirecturl(link);
        if (storedDirecturl != null) {
            logger.info("Doing linkcheck via directurl");
            if (checkDirecturl(link, storedDirecturl)) {
                logger.info("Linkcheck via directurl successful");
                return AvailableStatus.TRUE;
            } else {
                logger.info("Linkcheck via directurl failed --> Full linkcheck needed");
            }
        }
        if (account != null) {
            login(account, false);
        }
        /* 2021-09-13: Don't do this anymore as it may not always work for videos! */
        // br.getPage(getPhotoURLWithoutAlbumInfo(link) + "/in/photostream");
        br.getPage(getPhotoURLWithoutAlbumInfo(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("div class=\"Four04Case\">") || br.containsHTML("(?i)>\\s*This member is no longer active on Flickr") || br.containsHTML("class=\"Problem\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /* 2020-04-27 */
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
                if (jd.plugins.decrypter.FlickrCom.looksLikeInternalUsername(usernameFromHTML)) {
                    link.setProperty(PROPERTY_USERNAME_INTERNAL, usernameFromHTML);
                } else {
                    link.setProperty(PROPERTY_USERNAME, usernameFromHTML);
                }
            }
            final String usernameFull = br.getRegex("class=\"owner-name truncate\"[^>]*data-track=\"attributionNameClick\">([^<]+)</a>").getMatch(0);
            setStringProperty(link, PROPERTY_USERNAME_FULL, usernameFull, false);
            /* Do not overwrite property as crawler is getting this information more reliably as it is using their API! */
            setStringProperty(link, PROPERTY_TITLE, title, false);
            final String uploadedDate = PluginJSonUtils.getJsonValue(br, "datePosted");
            if (uploadedDate != null && uploadedDate.matches("\\d+")) {
                link.setProperty(PROPERTY_DATE, Long.parseLong(uploadedDate) * 1000);
            }
        }
        link.setProperty(PROPERTY_CONTENT_ID, getFID(link));
        boolean isVideo = br.containsHTML("class=\"videoplayer main\\-photo\"") || isVideo(link);
        final PhotoQuality preferredPhotoQuality = getPreferredPhotoQuality(link);
        final String json = br.getRegex("main\":(\\{\"photo-models\".*?),\\s+auth: auth,").getMatch(0);
        String secret = null;
        if (json != null) {
            /* json handling */
            Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            final List<Object> photoModels = (List) root.get("photo-models");
            final Map<String, Object> photoData = (Map<String, Object>) photoModels.get(0);
            /* Required to obtain videostreams */
            secret = (String) photoData.get("secret");
            final Map<String, Object> owner = (Map<String, Object>) photoData.get("owner");
            setStringProperty(link, PROPERTY_USERNAME, (String) owner.get("pathAlias"), false);
            /*
             * This might be confusing but their fields are different in API/website! E.g. API.ownername == Website.realname --> Both really
             * is the full username (not to be mistaken with the real name of the uploader!!)
             */
            setStringProperty(link, PROPERTY_USERNAME_FULL, (String) owner.get("realname"), false);
            if (!link.hasProperty(PROPERTY_USERNAME_INTERNAL)) {
                link.setProperty(PROPERTY_USERNAME_INTERNAL, owner.get("id"));
            }
            setStringProperty(link, PROPERTY_REAL_NAME, (String) owner.get("username"), false);
            setStringProperty(link, PROPERTY_TITLE, (String) photoData.get("title"), false);
            String description = (String) photoData.get("description");
            if (description != null) {
                description = decodeEncoding(null, description);
                if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                    link.setComment(description);
                }
            }
            final String mediaType = (String) photoData.get("mediaType");
            if (setStringProperty(link, PROPERTY_MEDIA_TYPE, mediaType, false)) {
                /* Assign this again just to be sure. */
                isVideo = isVideo(link);
            }
            {
                /* This block solely exists to find the uploaded-timestamp. */
                final List<Object> photoStatsModels = (List) root.get("photo-stats-models");
                final Map<String, Object> photoStatsData = (Map<String, Object>) photoStatsModels.get(0);
                final long datePosted = JavaScriptEngineFactory.toLong(photoStatsData.get("datePosted"), 0);
                if (datePosted > 0) {
                    link.setProperty(PROPERTY_DATE, datePosted * 1000);
                }
                final String dateTaken = (String) photoStatsData.get("dateTaken");
                setStringProperty(link, PROPERTY_DATE_TAKEN, dateTaken, false);
            }
            /* Get metadata: This way is safer than via html and it will return more information! */
            final Map<String, Object> photoSizes = (Map<String, Object>) photoData.get("sizes");
            final Iterator<Entry<String, Object>> iterator = photoSizes.entrySet().iterator();
            long maxWidth = -1;
            String maxQualityName = null;
            String maxQualityDownloadurl = null;
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
                    link.setProperty(PROPERTY_QUALITY, qualityName);
                    dllink = url;
                    break;
                } else if (width > maxWidth) {
                    maxQualityName = qualityName;
                    maxWidth = width;
                    maxQualityDownloadurl = url;
                }
            }
            if (dllink == null && maxQualityDownloadurl != null) {
                logger.info("Using best quality: " + maxQualityName + " | width: " + maxWidth);
                link.setProperty(PROPERTY_QUALITY, maxQualityName);
                dllink = maxQualityDownloadurl;
            }
        } else if (!isVideo) {
            /* Old website handling */
            /*
             * Fast way to get finallink via site as we always try to access the "o" (original) quality. Page might be redirected!
             */
            br.getPage("/photos/" + getUsername(link) + "/" + getFID(link) + "/sizes/o");
            /* Special case: Check if user prefers to download original quality */
            if (preferredPhotoQuality == PhotoQuality.QO) {
                if (br.getURL().contains("sizes/o")) { // Not redirected
                    dllink = br.getRegex("<a href=\"([^<>\"]+)\">\\s*(Dieses Foto im Originalformat|Download the Original)").getMatch(0);
                }
            }
            if (dllink == null) { // Redirected if download original is not allowed
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
                    br.getPage(this.getPhotoURLWithoutAlbumInfo(link) + "/sites/" + selectedQualityName + "/");
                    dllink = br.getRegex("id=\"allsizes-photo\">[^~]*?<img src=\"(http[^<>\"]*?)\"").getMatch(0);
                    if (dllink != null) {
                        link.setProperty(PROPERTY_QUALITY, selectedQualityName);
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
        final boolean allowCheckDirecturlForFilesize;
        String filenameURL = null; // 2021-09-09: Only allow this for photos atm.
        if (isVideo) {
            /* Video */
            /*
             * TODO: Add correct API csrf cookie handling so we can use this while being logged in to download videos and do not have to
             * remove the cookies here - that's just a workaround!
             */
            final Browser apibr = br.cloneBrowser();
            final String apikey = getPublicAPIKey(this.br);
            if (StringUtils.isEmpty(apikey) || StringUtils.isEmpty(secret)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            apibr.getPage(jd.plugins.decrypter.FlickrCom.API_BASE + "services/rest?photo_id=" + getFID(link) + "&secret=" + secret + "&method=flickr.video.getStreamInfo&csrf=&api_key=" + apikey + "&format=json&hermes=1&hermesClient=1&reqId=&nojsoncallback=1");
            Map<String, Object> entries = JSonStorage.restoreFromString(apibr.toString(), TypeRef.HASHMAP);
            /*
             * 2021-09-09: Found 2 video types so far: "700" and "iphone_wifi" --> Both are equal in filesize. If more are available,
             * implementing a quality selection for videos could make sense.
             */
            final List<Map<String, Object>> streams = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "streams/stream");
            if (streams.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                this.dllink = userPreferredQualityURL;
            } else if (bestQualityURL != null) {
                logger.info("Failed to find user preferred quality " + userPreferredVideoQuality.getLabel() + " - using this one instead: " + bestQualityName);
                link.setProperty(PROPERTY_QUALITY, bestQualityName);
                this.dllink = bestQualityURL;
            } else {
                /* This should either never happen or be a very rare case. */
                logger.warning("Failed to find any usable video stream --> Only broken streams available?");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?");
            }
            String videoExt;
            if (dllink.contains("mp4")) {
                videoExt = ".mp4";
            } else {
                videoExt = ".flv";
            }
            /* Needed for custom filenames! */
            link.setProperty(PROPERTY_EXT, videoExt);
            allowCheckDirecturlForFilesize = true;
        } else {
            /* Photo */
            String ext;
            if (!StringUtils.isEmpty(dllink)) {
                ext = dllink.substring(dllink.lastIndexOf("."));
                if (ext == null || ext.length() > 5) {
                    ext = defaultPhotoExt;
                }
                filenameURL = getFilenameFromDirecturl(this.dllink);
            } else {
                ext = defaultPhotoExt;
            }
            /* Needed for custom filenames! */
            link.setProperty(PROPERTY_EXT, ext);
            /* 2021-09-09: Filesize is not provided for photo directURLs */
            allowCheckDirecturlForFilesize = false;
        }
        if (userPrefersServerFilenames()) {
            link.setFinalFileName(filenameURL);
        } else {
            link.setFinalFileName(getFormattedFilename(link));
        }
        this.br.setFollowRedirects(true);
        if (!StringUtils.isEmpty(dllink) && !isDownload && allowCheckDirecturlForFilesize) {
            checkDirecturl(link, this.dllink);
        }
        /* Save directurl for later usage. */
        if (!StringUtils.isEmpty(dllink)) {
            link.setProperty(String.format(PROPERTY_DIRECTURL, link.getStringProperty(PROPERTY_QUALITY)), this.dllink);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean setStringProperty(final DownloadLink link, final String property, String value, final boolean overwrite) {
        if ((overwrite || !link.hasProperty(property)) && !StringUtils.isEmpty(value)) {
            final String decodedValue = decodeEncoding(property, value);
            if (!StringUtils.isEmpty(decodedValue)) {
                link.setProperty(property, decodedValue);
                return true;
            }
        }
        return false;
    }

    public static String decodeEncoding(final String property, final String value) {
        if (value != null) {
            String decodedValue = Encoding.unicodeDecode(value);
            decodedValue = Encoding.htmlDecode(decodedValue);
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

    private boolean isVideo(final DownloadLink link) {
        if (StringUtils.equals(link.getStringProperty(PROPERTY_MEDIA_TYPE), "video")) {
            return true;
        } else {
            return false;
        }
    }

    private String getStoredDirecturl(final DownloadLink link) {
        return link.getStringProperty(getDirecturlProperty(link));
    }

    private String getDirecturlProperty(final DownloadLink link) {
        return String.format(PROPERTY_DIRECTURL, link.getStringProperty(PROPERTY_QUALITY, getPreferredQualityStr(link)));
    }

    private String getPreferredQualityStr(final DownloadLink link) {
        if (this.isVideo(link)) {
            return photoQualityEnumNameToString(getPreferredVideoQuality().name());
        } else {
            return photoQualityEnumNameToString(getPreferredPhotoQuality().name());
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, account, true);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* chunked transfer */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumable(link), getMaxChunks(link));
            connectionErrorhandling(dl.getConnection());
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
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
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken image?");
            }
        }
    }

    private void connectionErrorhandling(final URLConnectionAdapter con) throws PluginException {
        if (dl.getConnection().getURL().toString().contains("/photo_unavailable.gif")) {
            dl.getConnection().disconnect();
            /* Same as check below */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
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
        if (this.isVideo(link)) {
            return true;
        } else {
            return false;
        }
    }

    private int getMaxChunks(final DownloadLink link) {
        if (this.isVideo(link)) {
            /* Unlimited */
            return 0;
        } else {
            /* Max 1 */
            return 1;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, false);
        account.setType(AccountType.FREE);
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
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    if (isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/signin/");
                Form login = br.getFormByRegex("login-username-form");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(account.getUser()));
                br.submitForm(login);
                if (br.containsHTML("messages\\.ERROR_INVALID_USERNAME")) {
                    final String message = br.getRegex("messages\\.ERROR_INVALID_USERNAME\">\\s*(.*?)\\s*<").getMatch(0);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                login = br.getFormByRegex("name\\s*=\\s*\"displayName\"");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("password", Encoding.urlEncode(account.getPass()));
                login.remove("skip");
                br.submitForm(login);
                if (br.containsHTML("messages\\.ERROR_INVALID_PASSWORD")) {
                    final String message = br.getRegex("messages\\.ERROR_INVALID_PASSWORD\">\\s*(.*?)\\s*<").getMatch(0);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) throws IOException {
        br.getPage("https://www.flickr.com/");
        if (br.containsHTML("gnSignin")) {
            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("unused")
    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        String formattedFilename = null;
        final SubConfiguration cfg = SubConfiguration.getConfig("flickr.com");
        final String customStringForEmptyTags = getCustomStringForEmptyTags();
        String formattedDate = defaultCustomStringForEmptyTags;
        if (link.hasProperty(PROPERTY_DATE)) {
            final long date = link.getLongProperty(PROPERTY_DATE, 0);
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            Date theDate = new Date(date);
            if (userDefinedDateFormat != null) {
                try {
                    final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (final Exception ignore) {
                    /* prevent user error killing the custom filename function. */
                    formattedDate = defaultCustomStringForEmptyTags;
                }
            }
        }
        formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        /* Make sure that the user entered a VALID custom filename - if not, use the default name */
        if (!formattedFilename.contains("*extension*") || (!formattedFilename.contains("*content_id*") && !formattedFilename.contains("*date*") && !formattedFilename.contains("*username*") && !formattedFilename.contains("*username_internal*"))) {
            formattedFilename = defaultCustomFilename;
        }
        formattedFilename = formattedFilename.replace("*content_id*", link.getStringProperty(PROPERTY_CONTENT_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*order_id*", link.getStringProperty(PROPERTY_ORDER_ID, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*quality*", link.getStringProperty(PROPERTY_QUALITY, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*date*", formattedDate);
        formattedFilename = formattedFilename.replace("*date_taken*", link.getStringProperty(PROPERTY_DATE_TAKEN, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*media*", link.getStringProperty(PROPERTY_MEDIA_TYPE, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*extension*", link.getStringProperty(PROPERTY_EXT, defaultPhotoExt));
        formattedFilename = formattedFilename.replace("*username*", link.getStringProperty(PROPERTY_USERNAME, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_url*", link.getStringProperty(PROPERTY_USERNAME_URL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_full*", link.getStringProperty(PROPERTY_USERNAME_FULL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*username_internal*", link.getStringProperty(PROPERTY_USERNAME_INTERNAL, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*real_name*", link.getStringProperty(PROPERTY_REAL_NAME, customStringForEmptyTags));
        formattedFilename = formattedFilename.replace("*title*", link.getStringProperty(PROPERTY_TITLE, customStringForEmptyTags));
        return formattedFilename;
    }

    public static final String getFilenameFromDirecturl(final String url) {
        return new Regex(url, "(?i)https?://live\\.staticflickr\\.com/\\d+/([^/]+)").getMatch(0);
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
            ret[i] = photoQualityEnumNameToString(PhotoQuality.values()[i].name());
        }
        return ret;
    }

    public static String photoQualityEnumNameToString(final String label) {
        return label.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private PhotoQuality stringToPhotoQuality(final String str) {
        if (str == null) {
            return null;
        } else {
            for (final PhotoQuality quality : PhotoQuality.values()) {
                final String qualStr = photoQualityEnumNameToString(quality.name());
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
            ret[i] = videoQualityEnumNameToString(VideoQuality.values()[i].name());
        }
        return ret;
    }

    public static String videoQualityEnumNameToString(final String label) {
        return label.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private VideoQuality stringToVideoQuality(final String str) {
        if (str == null) {
            return null;
        } else {
            for (final VideoQuality quality : VideoQuality.values()) {
                final String qualStr = videoQualityEnumNameToString(quality.name());
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
        String emptytag = cfg.getStringProperty(CUSTOM_FILENAME_EMPTY_TAG_STRING, defaultCustomStringForEmptyTags);
        if (emptytag.equals("")) {
            emptytag = defaultCustomStringForEmptyTags;
        }
        return emptytag;
    }

    private static final int     defaultArrayPosSelectedPhotoQuality = 0;
    private static final int     defaultArrayPosSelectedVideoQuality = 0;
    private static final boolean defaultPreferServerFilename         = false;
    private static final String  defaultCustomDate                   = "MM-dd-yyyy";
    private static final String  defaultCustomFilename               = "*username_url*_*content_id*_*title**extension*";
    public final static String   defaultCustomStringForEmptyTags     = "-";
    public final static String   defaultPhotoExt                     = ".jpg";

    @Override
    public String getDescription() {
        return "JDownloader's flickr.com Plugin helps downloading media from flickr. Here you can define custom filenames.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_PHOTO_QUALITY, getPhotoQualityLabels(), "Select preferred photo quality. If that is not available, best will be used instead.").setDefaultValue(defaultArrayPosSelectedPhotoQuality));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_VIDEO_QUALITY, getVideoQualityLabels(), "Select preferred video quality. If that is not available, best will be used instead.").setDefaultValue(defaultArrayPosSelectedVideoQuality));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* Filename settings */
        final ConfigEntry preferServerFilenames = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_SETTING_PREFER_SERVER_FILENAME, "Prefer server filenames instead of formatted filenames (photos only) e.g. '11112222_574508fa345a_6k.jpg'?").setDefaultValue(defaultPreferServerFilename);
        getConfig().addEntry(preferServerFilenames);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Define how dates inside filenames should look like:").setDefaultValue(defaultCustomDate).setEnabledCondidtion(preferServerFilenames, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Custom filename:").setDefaultValue(defaultCustomFilename).setEnabledCondidtion(preferServerFilenames, false));
        final StringBuilder sbtags = new StringBuilder();
        sbtags.append("Explanation of the available tags:\r\n");
        sbtags.append("*content_id* = ID of the photo/video\r\n");
        sbtags.append("*date* = date when the photo was uploaded - custom date format will be used here\r\n");
        sbtags.append("*date_taken* = date when the photo was taken - pre-formatted string (yyyy-MM-dd HH:mm:ss)\r\n");
        sbtags.append("*extension* = Extension of the photo - usually '.jpg'\r\n");
        sbtags.append("*media* = Media type ('video' or 'photo')\r\n");
        sbtags.append("*order_id* = Position of image/video if it was part of a crawled gallery/user-profile\r\n");
        sbtags.append("*quality* = Quality of the photo/video e.g. 'm' or '1080p'\r\n");
        sbtags.append("*real_name* = Real name of the user (name and surname) e.g. 'Marcus Mueller'\r\n");
        sbtags.append("*title* = Title of the photo\r\n");
        sbtags.append("*username* = Short username e.g. 'exampleusername'\r\n");
        sbtags.append("*username_internal* = Internal username e.g. '12345678@N04'\r\n");
        sbtags.append("*username_full* = Full username e.g. 'Example Username'\r\n");
        sbtags.append("*username_url* = Username from inside URL - usually either the same value as 'username' or 'username_internal'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbtags.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_EMPTY_TAG_STRING, "Char which will be used for empty tags (e.g. missing data):").setDefaultValue(defaultCustomStringForEmptyTags).setEnabledCondidtion(preferServerFilenames, false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_QUALITY);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }
}