//  jDownloader - Downloadmanager
//  Copyright (C) 2013  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.plugins.components.config.GoogleConfig;
import org.jdownloader.plugins.components.config.GoogleConfig.APIDownloadMode;
import org.jdownloader.plugins.components.config.GoogleConfig.PreferredVideoQuality;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.settings.GeneralSettings;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.GoogleDriveCrawler;
import jd.plugins.decrypter.GoogleDriveCrawler.JsonSchemeType;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GoogleDrive extends PluginForHost {
    public GoogleDrive(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.google.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://support.google.com/drive/answer/2450387?hl=en-GB";
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "drive.google.com", "docs.google.com", "googledrive" };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "drive.google.com", "docs.google.com", "drive.usercontent.google.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.FAVICON, FEATURE.COOKIE_LOGIN_ONLY };
    }

    @Override
    public Object getFavIcon(String host) throws IOException {
        if ("drive.google.com".equals(host) || "docs.google.com".equals(host)) {
            /**
             * Required because websites redirect to login page for anonymous users which would not let auto handling fetch correct favicon.
             */
            if ("docs.google.com".equals(host)) {
                return "https://ssl.gstatic.com/docs/documents/images/kix-favicon7.ico";
            } else {
                return "https://drive.google.com/favicon.ico";
            }
        } else {
            return null;
        }
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String regex = "https?://" + buildHostsPatternPart(domains) + "/(?:";
            regex += "(?:leaf|open)\\?([^<>\"/]+)?id=[A-Za-z0-9\\-_]+.*";
            regex += "|(?:u/\\d+/)?uc(?:\\?|.*?&)id=[A-Za-z0-9\\-_]+.*";
            regex += "|download\\?id=[A-Za-z0-9\\-_]+.*";
            regex += "|(?:a/[a-zA-z0-9\\.]+/)?(?:file|document)/d/[A-Za-z0-9\\-_]+.*";
            regex += ")";
            /*
             * Special case: Embedded video URLs with subdomain that is not given in our list of domains because it only supports this
             * pattern!
             */
            regex += "|https?://video\\.google\\.com/get_player\\?docid=[A-Za-z0-9\\-_]+";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isSpeedLimited(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fileID = getFID(link);
        if (fileID != null) {
            return getHost().concat("://".concat(fileID));
        } else {
            return super.getLinkID(link);
        }
    }

    private static Object         LOCK                                           = new Object();
    private String                websiteWebapiKey                               = null;
    /* Constants */
    public static final String    API_BASE                                       = "https://www.googleapis.com/drive/v3";
    public static final String    WEBAPI_BASE                                    = "https://content.googleapis.com/drive";
    public static final String    WEBAPI_BASE_2                                  = "https://clients6.google.com/drive";
    private static final String   PATTERN_GDOC                                   = "(?i)https?://.*/document/d/([a-zA-Z0-9\\-_]+).*";
    private final String          PATTERN_FILE                                   = "(?i)https?://.*/file/d/([a-zA-Z0-9\\-_]+).*";
    private final String          PATTERN_FILE_OLD                               = "(?i)https?://[^/]+/(?:leaf|open)\\?([^<>\"/]+)?id=([A-Za-z0-9\\-_]+).*";
    private final String          PATTERN_FILE_DOWNLOAD_PAGE                     = "(?i)https?://[^/]+/(?:u/\\d+/)?uc(?:\\?|.*?&)id=([A-Za-z0-9\\-_]+).*";
    private final String          PATTERN_DRIVE_USERCONTENT_DOWNLOAD             = "(?i)https?://[^/]+/download\\?id=([A-Za-z0-9\\-_]+).*";
    private final String          PATTERN_VIDEO_STREAM                           = "(?i)https?://video\\.google\\.com/get_player\\?docid=([A-Za-z0-9\\-_]+)";
    /* Developer: Do not touch this!! You need to know what you're doing!! */
    private final boolean         canHandleGoogleSpecialCaptcha                  = false;
    /* Don't touch this! */
    private final boolean         attemptQuickLinkcheck                          = true;
    /** DownloadLink properties */
    /**
     * Contains the quality modifier of the last chosen quality. This property gets reset on reset DownloadLink to ensure that a user cannot
     * change the quality and then resume the started download with another URL.
     */
    private final String          PROPERTY_USED_QUALITY                          = "USED_QUALITY";
    private static final String   PROPERTY_GOOGLE_DOCUMENT                       = "IS_GOOGLE_DOCUMENT";
    private static final String   PROPERTY_GOOGLE_DOCUMENT_FILE_EXTENSION        = "GOOGLE_DOCUMENT_FILE_EXTENSION";
    private static final String   PROPERTY_FORCED_FINAL_DOWNLOADURL              = "FORCED_FINAL_DOWNLOADURL";
    private static final String   PROPERTY_CAN_DOWNLOAD                          = "CAN_DOWNLOAD";
    private final String          PROPERTY_CAN_STREAM                            = "CAN_STREAM";
    private final String          PROPERTY_IS_INFECTED                           = "is_infected";
    private final String          PROPERTY_LAST_IS_PRIVATE_FILE_TIMESTAMP        = "LAST_IS_PRIVATE_FILE_TIMESTAMP";
    private final String          PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS     = "TIMESTAMP_QUOTA_REACHED_ANONYMOUS";
    private final String          PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT       = "TIMESTAMP_QUOTA_REACHED_ACCOUNT";
    private final String          PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED        = "TIMESTAMP_STREAM_QUOTA_REACHED";
    private final String          PROPERTY_DIRECTURL                             = "directurl";
    /* Used in very rare cases e.g. if original link goes to a google drive shortcut. */
    private final String          PROPERTY_REAL_FILE_ID                          = "real_file_id";
    /* Directurl for files which are not officially downloadable view viewable - typically images. */
    private final String          PROPERTY_DIRECTURL_DRIVE_VIEWER                = "directurl_drive_viewer";
    private final static String   PROPERTY_CACHED_FILENAME                       = "cached_filename";
    private final static String   PROPERTY_CACHED_LAST_DISPOSITION_STATUS        = "cached_last_disposition_status";
    private final String          PROPERTY_TIMESTAMP_STREAM_DOWNLOAD_FAILED      = "timestamp_stream_download_failed";
    private final String          PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION     = "tmp_allow_obtain_more_information";
    private final String          PROPERTY_TMP_STREAM_DOWNLOAD_ACTIVE            = "tmp_stream_download_active";
    /* Misc */
    private final String          DISPOSITION_STATUS_QUOTA_EXCEEDED              = "QUOTA_EXCEEDED";
    /* Pre defined exceptions */
    private final PluginException exceptionRateLimitedSingle                     = new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Rate limited", getRateLimitWaittime());
    /**
     * 2022-02-20: We store this property but we're not using it at this moment. It is required to access some folders though so it's good
     * to have it set on each DownloadLink if it exists.
     */
    public static final String    PROPERTY_TEAM_DRIVE_ID                         = "TEAM_DRIVE_ID";
    public static final String    PROPERTY_ROOT_DIR                              = "root_dir";
    /* Account properties */
    private final String          PROPERTY_ACCOUNT_ACCESS_TOKEN                  = "ACCESS_TOKEN";
    private final String          PROPERTY_ACCOUNT_REFRESH_TOKEN                 = "REFRESH_TOKEN";
    private final String          PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP = "ACCESS_TOKEN_EXPIRE_TIMESTAMP";

    private String getFID(final DownloadLink link) {
        if (link == null) {
            return null;
        } else if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            final String storedFileID = link.getStringProperty(PROPERTY_REAL_FILE_ID);
            if (storedFileID != null) {
                return storedFileID;
            } else {
                return findFileID(link.getPluginPatternMatcher());
            }
        }
    }

    private String findFileID(final String url) {
        if (url.matches(PATTERN_GDOC)) {
            return new Regex(url, PATTERN_GDOC).getMatch(0);
        } else if (url.matches(PATTERN_FILE)) {
            return new Regex(url, PATTERN_FILE).getMatch(0);
        } else if (url.matches(PATTERN_VIDEO_STREAM)) {
            return new Regex(url, PATTERN_VIDEO_STREAM).getMatch(0);
        } else if (url.matches(PATTERN_FILE_OLD)) {
            return new Regex(url, PATTERN_FILE_OLD).getMatch(0);
        } else if (url.matches(PATTERN_FILE_DOWNLOAD_PAGE)) {
            return new Regex(url, PATTERN_FILE_DOWNLOAD_PAGE).getMatch(0);
        } else if (url.matches(PATTERN_DRIVE_USERCONTENT_DOWNLOAD)) {
            return new Regex(url, PATTERN_DRIVE_USERCONTENT_DOWNLOAD).getMatch(0);
        } else {
            return null;
        }
    }

    public static Browser prepBrowser(final Browser pbr) {
        pbr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        pbr.setCustomCharset("utf-8");
        pbr.setFollowRedirects(true);
        pbr.setAllowedResponseCodes(new int[] { 429 });
        return pbr;
    }

    public static Browser prepBrowserWebAPI(final Browser br, final Account account) throws PluginException {
        final String domainWithProtocol = "https://drive.google.com";
        br.getHeaders().put("X-Referer", domainWithProtocol);
        br.getHeaders().put("X-Origin", domainWithProtocol);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("x-javascript-user-agent", "google-api-javascript-client/1.1.0");
        if (account != null) {
            /* For logged in users: */
            final String sapisidhash = GoogleHelper.getSAPISidHash(br, domainWithProtocol);
            if (sapisidhash != null) {
                br.getHeaders().put("Authorization", "SAPISIDHASH " + sapisidhash);
            }
            br.getHeaders().put("X-Goog-Authuser", "0");
        }
        return br;
    }

    public static Browser prepBrowserAPI(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    private static boolean isGoogleDocument(final DownloadLink link) {
        final Boolean googleDocumentCachedStatus = (Boolean) link.getProperty(PROPERTY_GOOGLE_DOCUMENT);
        if (googleDocumentCachedStatus != null) {
            /* Return stored property (= do not care about the URL). */
            return googleDocumentCachedStatus.booleanValue();
        } else if (link.getPluginPatternMatcher().matches(PATTERN_GDOC)) {
            /* URL looks like GDoc */
            return true;
        } else {
            /* Assume it's not a google document! */
            return false;
        }
    }

    /**
     * Google has added this parameter to some long time shared URLs as of October 2021 to make those safer. </br>
     * https://support.google.com/a/answer/10685032?p=update_drives&visit_id=637698313083783702-233025620&rd=1
     */
    private String getFileResourceKey(final DownloadLink link) {
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("resourcekey");
        } catch (final MalformedURLException ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

    /** Returns true if this link has the worst "Quota reached" status: It is currently not even downloadable via account. */
    private boolean isDownloadQuotaReachedAccount(final DownloadLink link) {
        if (System.currentTimeMillis() - link.getLongProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT, 0) < 10 * 60 * 1000l) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if this link has the worst "Streaming Quota reached" status: It is currently not even downloadable via account. */
    private boolean isStreamQuotaReached(final DownloadLink link) {
        if (System.currentTimeMillis() - link.getLongProperty(PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED, 0) < 10 * 60 * 1000l) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this link has the most common "Quota reached" status: It is currently only downloadable via account (or not
     * downloadable at all).
     */
    private boolean isDownloadQuotaReachedAnonymous(final DownloadLink link, final Account account) {
        if (System.currentTimeMillis() - link.getLongProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, 0) < 10 * 60 * 1000l) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDownloadQuotaReached(final DownloadLink link, final Account account) {
        if (account != null) {
            return isDownloadQuotaReachedAccount(link);
        } else if (System.currentTimeMillis() - link.getLongProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, 0) < 10 * 60 * 1000l) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns false if official download has been disabled for this file. */
    private boolean canDownloadOfficially(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_CAN_DOWNLOAD, true);
    }

    /** Files marked as infected by Google can only be downloaded by the original uploader. */
    private boolean isInfected(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_IS_INFECTED, false);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, AccountController.getInstance().getValidAccount(this.getHost()), false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (this.useAPIForLinkcheck()) {
            return this.requestFileInformationAPI(link, isDownload);
        } else {
            return this.requestFileInformationWebsite(link, account, isDownload);
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final boolean isDownload) throws Exception {
        final String fileID_so_far = this.getFID(link);
        String fileID = fileID_so_far;
        if (fileID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            /* Set fallback name */
            setFilename(this, link, null, fileID, false);
        }
        final String fileResourceKey = getFileResourceKey(link);
        prepBrowserAPI(this.br);
        if (fileResourceKey != null) {
            GoogleDriveCrawler.setResourceKeyHeaderAPI(br, fileID, fileResourceKey);
        }
        Map<String, Object> entries = null;
        int retries = 0;
        do {
            final UrlQuery queryFile = new UrlQuery();
            queryFile.appendEncoded("fileId", fileID);
            queryFile.add("supportsAllDrives", "true");
            queryFile.appendEncoded("fields", getSingleFilesFieldsAPI());
            queryFile.appendEncoded("key", getAPIKey());
            br.getPage(GoogleDrive.API_BASE + "/files/" + fileID + "?" + queryFile.toString());
            this.handleErrorsAPI(this.br, link, null);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> shortcutDetails = (Map<String, Object>) entries.get("shortcutDetails");
            if (shortcutDetails == null) {
                /* Normal single file -> No retry needed. */
                break;
            } else {
                /**
                 * Rare case: Single file link as shortcut which has been added to hostplugin. </br>
                 * A shortcut can go to another file/folder which has a different ID than the one of this object. We will skip this redirect
                 * by making use of this ID right here. </br>
                 * We need to do this request again with the real fileID in order to obtain the information of the final file.
                 */
                fileID = shortcutDetails.get("targetId").toString();
                logger.info("Item is shortcut item -> Retry with itemID: " + fileID);
                retries++;
                continue;
            }
        } while (retries <= 1);
        parseFileInfoAPIAndWebsiteWebAPI(this, JsonSchemeType.API, link, true, true, entries);
        /* Store "real" fileID if item was a shortcut to another ID. */
        if (!StringUtils.equals(fileID, fileID_so_far)) {
            link.setProperty(PROPERTY_REAL_FILE_ID, fileID);
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Contains all fields we need for file/folder API requests. </br>
     * Use the star symbol "*" to get all fields which exist. </br>
     * This is very similar to the Google Drive WebAPI but some fields are named differently in the website version. </br>
     * Additional notes: </br>
     * Fields for file hashes: md5Checksum, sha1Checksum, sha256Checksum </br>
     */
    public static final String getSingleFilesFieldsAPI() {
        return "kind,mimeType,id,name,size,description,sha256Checksum,exportLinks,capabilities(canDownload),resourceKey,modifiedTime,shortcutDetails(targetId,targetMimeType)";
        // return "*";
    }

    public static final String getSingleFilesFieldsWebsite() {
        return "kind,mimeType,id,title,fileSize,description,sha256Checksum,exportLinks,capabilities(canDownload),resourceKey,modifiedDate,shortcutDetails(targetId,targetMimeType)";
    }

    private boolean useAPIForLinkcheck() {
        return canUseAPI();
    }

    /** Multiple factors decide whether we want to use the API for downloading or use the website. */
    private boolean useAPIForDownloading(final DownloadLink link, final Account account) {
        if (!canUseAPI()) {
            /* No API download possible */
            return false;
        }
        /*
         * Download via API is generally allowed. Now check for cases where we'd like to prefer website download.
         */
        if (account != null && PluginJsonConfig.get(GoogleConfig.class).getAPIDownloadMode() == APIDownloadMode.WEBSITE_IF_ACCOUNT_AVAILABLE) {
            /* Always prefer download via website with account to avoid "quota reached" errors. */
            return false;
        } else if (account != null && !this.isDownloadQuotaReachedAccount(link) && PluginJsonConfig.get(GoogleConfig.class).getAPIDownloadMode() == APIDownloadMode.WEBSITE_IF_ACCOUNT_AVAILABLE_AND_FILE_IS_QUOTA_LIMITED) {
            /*
             * Prefer download via website (avoid API) with account to avoid "quota reached" errors for specific links which we know are
             * quota limited.
             */
            return false;
        } else {
            /* Prefer API download for all other cases. */
            return true;
        }
    }

    public static void parseFileInfoAPIAndWebsiteWebAPI(final Plugin plugin, final JsonSchemeType schemetype, final DownloadLink link, final boolean setFinalFilename, final boolean setVerifiedFilesize, final Map<String, Object> entries) {
        final String mimeType = (String) entries.get("mimeType");
        final String checksumSha256 = (String) entries.get("sha256Checksum");
        final String sha1Checksum = (String) entries.get("sha1Checksum");
        final String checksumMd5 = (String) entries.get("md5Checksum");
        /* Filesize is returned as String so we need to parse it to long. */
        final String filename;
        final long filesize;
        final String modifiedDate;
        if (schemetype == JsonSchemeType.WEBSITE) {
            filename = entries.get("title").toString();
            filesize = JavaScriptEngineFactory.toLong(entries.get("fileSize"), -1);
            modifiedDate = (String) entries.get("modifiedDate");
        } else {
            filename = entries.get("name").toString();
            filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
            modifiedDate = (String) entries.get("modifiedTime");
        }
        final String description = (String) entries.get("description");
        /* E.g. application/vnd.google-apps.document | application/vnd.google-apps.spreadsheet */
        final String googleDriveDocumentType = new Regex(mimeType, "(?i)application/vnd\\.google-apps\\.(.+)").getMatch(0);
        if (googleDriveDocumentType != null) {
            /* Google Document */
            final Map<String, Object> exportLinks = (Map<String, Object>) entries.get("exportLinks");
            parseGoogleDocumentPropertiesAPIAndSetFilename(plugin, link, filename, googleDriveDocumentType, exportLinks);
        } else {
            setFilename(plugin, link, Boolean.FALSE, filename, setFinalFilename);
        }
        link.setProperty(PROPERTY_CACHED_FILENAME, filename);
        if (filesize > -1) {
            if (setVerifiedFilesize) {
                link.setVerifiedFileSize(filesize);
            } else {
                link.setDownloadSize(filesize);
            }
        }
        /* Set hash for CRC check */
        if (!StringUtils.isEmpty(checksumSha256)) {
            link.setSha256Hash(checksumSha256);
        } else if (!StringUtils.isEmpty(sha1Checksum)) {
            link.setSha1Hash(sha1Checksum);
        } else if (!StringUtils.isEmpty(checksumMd5)) {
            link.setMD5Hash(checksumMd5);
        }
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        if (modifiedDate != null) {
            final long lastModifiedDate = TimeFormatter.getMilliSeconds(modifiedDate, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
            link.setLastModifiedTimestamp(lastModifiedDate);
        }
        link.setProperty(GoogleDrive.PROPERTY_TEAM_DRIVE_ID, entries.get("teamDriveId"));
        link.setProperty(PROPERTY_CAN_DOWNLOAD, JavaScriptEngineFactory.walkJson(entries, "capabilities/canDownload"));
        link.setAvailable(true);
    }

    /** Sets filename- and required parameters for GDocs files. */
    public static void parseGoogleDocumentPropertiesAPIAndSetFilename(final Plugin plg, final DownloadLink link, final String filename, final String googleDriveDocumentType, final Map<String, Object> exportFormatDownloadurls) {
        /**
         * Google Drive documents: Either created directly on Google Drive or user added a "real" document-file to GDrive and converted it
         * into a GDoc later. </br>
         * In this case, the "filename" is more like a title no matter whether or not it contains a file-extension.</br>
         * If it contains a file-extension we will try to find download the output format accordingly. </br>
         * For GDocs usually there is no filesize given because there is no "original" file anymore. The filesize depends on the format we
         * chose to download the file in.
         */
        link.setProperty(PROPERTY_GOOGLE_DOCUMENT, true);
        /* Assume that a filename/title has to be given. */
        if (StringUtils.isEmpty(filename)) {
            /* This should never happen */
            return;
        }
        String docDownloadURL = null;
        String preGivenFileExtensionLowercase = Plugin.getFileNameExtensionFromString(filename);
        String finalFileExtensionWithoutDot = null;
        if (exportFormatDownloadurls != null) {
            if (preGivenFileExtensionLowercase != null) {
                preGivenFileExtensionLowercase = preGivenFileExtensionLowercase.toLowerCase(Locale.ENGLISH).replace(".", "");
            }
            final Map<String, String> extToDownloadlinkMap = new HashMap<String, String>();
            final Iterator<Entry<String, Object>> iterator = exportFormatDownloadurls.entrySet().iterator();
            while (iterator.hasNext()) {
                final String docDownloadURLCandidate = (String) iterator.next().getValue();
                try {
                    final String extFromDownloadurlCandidate = UrlQuery.parse(docDownloadURLCandidate).get("exportFormat");
                    if (extFromDownloadurlCandidate != null) {
                        extToDownloadlinkMap.put(extFromDownloadurlCandidate.toLowerCase(Locale.ENGLISH), docDownloadURLCandidate);
                    }
                } catch (final MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            if (preGivenFileExtensionLowercase != null && extToDownloadlinkMap.containsKey(preGivenFileExtensionLowercase)) {
                docDownloadURL = extToDownloadlinkMap.get(preGivenFileExtensionLowercase);
                finalFileExtensionWithoutDot = preGivenFileExtensionLowercase;
            }
            if (docDownloadURL == null) {
                /* Fallback 1 */
                final Map<String, String> documentTypeToPreferredExtMap = new HashMap<String, String>();
                documentTypeToPreferredExtMap.put("document", "odt");
                documentTypeToPreferredExtMap.put("presentation", "pdf");
                documentTypeToPreferredExtMap.put("spreadsheet", "ods");
                final String preferredExtByDocumentType = documentTypeToPreferredExtMap.get(googleDriveDocumentType);
                if (preferredExtByDocumentType != null && extToDownloadlinkMap.containsKey(preferredExtByDocumentType)) {
                    docDownloadURL = extToDownloadlinkMap.get(preferredExtByDocumentType);
                    finalFileExtensionWithoutDot = preferredExtByDocumentType;
                }
                if (docDownloadURL == null) {
                    /* Fallback 2 */
                    final String[] fileExtFallbackPriorityList = new String[] { "pdf", "zip", "odt", "ods", "txt" };
                    for (final String fileExtFallback : fileExtFallbackPriorityList) {
                        docDownloadURL = extToDownloadlinkMap.get(fileExtFallback);
                        if (docDownloadURL != null) {
                            finalFileExtensionWithoutDot = fileExtFallback;
                            break;
                        }
                    }
                }
            }
            /* If docDownloadURL is still null here this means that this is a Google Document with a file-type we do not know. */
        }
        if (!StringUtils.isEmpty(docDownloadURL)) {
            /* We found an export format suiting our filename-extension --> Prefer that */
            link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, docDownloadURL);
            link.setProperty(PROPERTY_GOOGLE_DOCUMENT_FILE_EXTENSION, finalFileExtensionWithoutDot);
        }
        setFilename(plg, link, Boolean.TRUE, filename, true);
        link.setProperty(PROPERTY_CACHED_FILENAME, link.getFinalFileName());
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String fid = getFID(link);
        if (fid == null) {
            /** This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            /* Set fallback name */
            setFilename(this, link, null, fid, false);
        }
        /* Make sure the value in this property is always fresh. */
        link.removeProperty(PROPERTY_DIRECTURL);
        /* Login whenever possible */
        if (account != null) {
            this.loginDuringLinkcheckOrDownload(br, account);
        }
        final GoogleConfig cfg = PluginJsonConfig.get(GoogleConfig.class);
        prepBrowser(this.br);
        try {
            boolean performDeeperOfflineCheck = false;
            if (attemptQuickLinkcheck) {
                try {
                    final AvailableStatus status = this.handleLinkcheckQuick(br, link, account);
                    final boolean itemIsEligableForObtainingMoreInformation = link.hasProperty(PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION);
                    if (status == AvailableStatus.TRUE) {
                        /* File is online. Now decide whether or not a deeper check is needed. */
                        final boolean deeperCheckHasAlreadyBeenPerformed = link.getFinalFileName() != null || isGoogleDocument(link) || link.getView().getBytesTotal() > 0;
                        if (looksLikeImageFile(link.getName()) && !canDownloadOfficially(link)) {
                            /* Allow deeper linkcheck to find alternative downloadlink for officially non-downloadable images. */
                            logger.info("File is online but we'll be looking for more information about this un-downloadable image");
                        } else if (deeperCheckHasAlreadyBeenPerformed || !itemIsEligableForObtainingMoreInformation) {
                            return status;
                        } else {
                            logger.info("File is online but we'll be looking for more information about this one");
                        }
                    } else {
                        logger.info("Do not trust quick linkcheck as it returned status != AVAILABLE");
                    }
                } catch (final PluginException exc) {
                    if (exc.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        if (cfg.isDebugWebsiteTrustQuickLinkcheckOfflineStatus()) {
                            throw exc;
                        } else {
                            logger.info("Looks like that file is offline -> Double-checking as it could also be a private file!");
                            performDeeperOfflineCheck = true;
                        }
                    } else {
                        throw exc;
                    }
                }
            }
            logger.info("Checking availablestatus via file overview");
            this.handleLinkcheckFileOverview(br, link, account, isDownload, performDeeperOfflineCheck);
            if (isGoogleDocument(link) && !this.hasObtainedInformationFromAPIOrWebAPI(link)) {
                /* Important: Without this, some google documents will not be downloadable! */
                logger.info("Handling extra linkcheck as preparation for google document download");
                try {
                    crawlAdditionalFileInformationFromWebsite(br, link, account, false, true);
                } catch (final PluginException docCheckFailure) {
                    if (isDownload || docCheckFailure.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                        throw docCheckFailure;
                    } else {
                        /* We know that the file is online. Do not throw exception during linkcheck. */
                        logger.log(docCheckFailure);
                        logger.info("Google Document extended linkcheck failed");
                    }
                }
            }
        } catch (final AccountRequiredException ae) {
            if (isDownload) {
                throw ae;
            } else {
                return AvailableStatus.TRUE;
            }
        } finally {
            /* Remove dummy property which only has a meaning in this small block of code. */
            link.removeProperty(PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION);
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus handleLinkcheckQuick(final Browser br, final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        logger.info("Attempting quick linkcheck");
        /* Remove some flags that can always change and will be re-assigned here. */
        link.removeProperty(PROPERTY_DIRECTURL);
        link.removeProperty(PROPERTY_CAN_DOWNLOAD);
        link.removeProperty(PROPERTY_IS_INFECTED);
        link.removeProperty(PROPERTY_CACHED_LAST_DISPOSITION_STATUS);
        removeQuotaReachedFlags(link, account, false);
        link.setProperty(PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION, true);
        br.getHeaders().put("X-Drive-First-Party", "DriveViewer");
        final UrlQuery query = new UrlQuery();
        query.add("id", this.getFID(link));
        final String fileResourceKey = this.getFileResourceKey(link);
        if (fileResourceKey != null) {
            query.add("resourcekey", Encoding.urlEncode(fileResourceKey));
        }
        /* 2020-12-01: authuser=0 also for logged-in users! */
        query.add("authuser", "0");
        query.add("export", "download");
        /* POST request with no data */
        br.postPage("https://drive.google.com/uc?" + query.toString(), "");
        if (br.getHttpConnection().getResponseCode() == 403) {
            this.errorAccountRequiredOrPrivateFile(br, link, account);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * 2023-02-23: Looks like this is sometimes returning status 404 for files where an account is required so we can't trust this
             * 100%.
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex(".*?(\\{.+\\})$").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        final String filename = (String) entries.get("fileName");
        final Number filesizeO = (Number) entries.get("sizeBytes");
        if (filesizeO != null) {
            /* Filesize field will be 0 for Google Documents and given downloadUrl will be broken. */
            final long filesize = filesizeO.longValue();
            if (filesize == 0) {
                /**
                 * Do not trust this filename as it is most likely missing a file-extension. </br>
                 * Assume that it is a google document while not trusting it and set default file extension for google doc downloads.
                 */
                /* Do NOT set google document flag as we can't be 100% sure that this is a google doc. */
                // link.setProperty(PROPERTY_GOOGLE_DOCUMENT, true);
                if (!StringUtils.isEmpty(filename)) {
                    setFilename(this, link, Boolean.TRUE, filename, false);
                }
                return AvailableStatus.TRUE;
            } else {
                link.setVerifiedFileSize(filesize);
            }
        }
        if (!StringUtils.isEmpty(filename)) {
            setFilename(this, link, null, filename, true);
            link.setProperty(PROPERTY_CACHED_FILENAME, filename);
        } else {
            /**
             * Try to use cached name if we are unable to find one at this moment. </br>
             * This can happen if e.g. download quota has been reached for this file.
             */
            final String cachedFilename = link.getStringProperty(PROPERTY_CACHED_FILENAME);
            if (cachedFilename != null) {
                link.setFinalFileName(null);
                setFilename(this, link, null, cachedFilename, false);
            }
        }
        final String directurl = (String) entries.get("downloadUrl");
        final String scanResult = (String) entries.get("scanResult");
        if (scanResult != null && scanResult.equalsIgnoreCase("ERROR")) {
            /* Assume that this has happened: {"disposition":"QUOTA_EXCEEDED","scanResult":"ERROR"} */
            link.setProperty(PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION, true);
            final String disposition = entries.get("disposition").toString();
            link.setProperty(PROPERTY_CACHED_LAST_DISPOSITION_STATUS, disposition);
            if (disposition.equalsIgnoreCase("FILE_INFECTED_NOT_OWNER")) {
                link.setProperty(PROPERTY_IS_INFECTED, true);
                return AvailableStatus.TRUE;
            } else if (disposition.equalsIgnoreCase("DOWNLOAD_RESTRICTED")) {
                /* Official download impossible -> Stream download may still be possible */
                link.setProperty(PROPERTY_CAN_DOWNLOAD, false);
                return AvailableStatus.TRUE;
            } else if (disposition.equalsIgnoreCase(this.DISPOSITION_STATUS_QUOTA_EXCEEDED)) {
                this.setQuotaReachedFlags(link, account, false);
                return AvailableStatus.TRUE;
            } else {
                /* Unknown error state */
                throw new PluginException(LinkStatus.ERROR_FATAL, disposition);
            }
        } else {
            /* Typically with scanResult == "CLEAN_FILE" or "SCAN_CLEAN" */
            link.setProperty(PROPERTY_DIRECTURL, directurl);
            link.removeProperty(PROPERTY_TMP_ALLOW_OBTAIN_MORE_INFORMATION);
            logger.info("Experimental linkcheck successful and file looks to be downloadable");
            return AvailableStatus.TRUE;
        }
    }

    /** Check availablestatus via https://drive.google.com/file/d/<fuid> */
    private AvailableStatus handleLinkcheckFileOverview(final Browser br, final DownloadLink link, final Account account, final boolean isDownload, final boolean specialOfflineCheck) throws PluginException, IOException, InterruptedException {
        br.getPage(getFileViewURL(link));
        this.websiteWebapiKey = this.findWebsiteWebAPIKey(br);
        /** More errorhandling / offline check */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)<p class=\"error\\-caption\"[^>]*>\\s*Sorry, we are unable to retrieve this document")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("\"docs-dm\":\\s*\"video/")) {
            link.setProperty(PROPERTY_CAN_STREAM, true);
        }
        if (this.websiteWebapiKey == null) {
            logger.warning("'Failed to find websiteWebapiKey");
        }
        final String fid = this.getFID(link);
        final String fidFromCurrentURL = this.findFileID(br.getURL());
        final boolean looksLikeFileIsOffline = br.getHttpConnection().getResponseCode() == 403 && !br.containsHTML(Pattern.quote(fid));
        if (specialOfflineCheck && looksLikeFileIsOffline) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (fid != null && fidFromCurrentURL != null && !fid.equals(fidFromCurrentURL)) {
            /* Shortcut-link which redirected from one ID to another. */
            logger.info("File link redirected to new fileID: Old: " + fid + " | New: " + fidFromCurrentURL);
            link.setProperty(PROPERTY_REAL_FILE_ID, fidFromCurrentURL);
        }
        this.handleErrorsWebsite(this.br, link, account);
        /** Only look for/set filename/filesize if it hasn't been done before! */
        String filename = br.getRegex("'id'\\s*:\\s*'" + Pattern.quote(fid) + "'\\s*,\\s*'title'\\s*:\\s*'(.*?)'").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("'title'\\s*:\\s*'([^<>\"\\']+)'").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(?i)<title>([^<]+) - Google Drive\\s*</title>").getMatch(0);
            }
            if (filename == null) {
                filename = HTMLSearch.searchMetaTag(br, "og:title");
            }
        }
        final boolean isGoogleDocument;
        if (br.containsHTML("\"docs-dm\":\\s*\"application/vnd\\.google-apps")) {
            isGoogleDocument = true;
            link.setProperty(PROPERTY_GOOGLE_DOCUMENT, true);
        } else {
            isGoogleDocument = false;
            /* Do not remove this property! */
            // link.removeProperty(PROPERTY_GOOGLE_DOCUMENT);
        }
        if (filename != null) {
            filename = PluginJSonUtils.unescape(filename);
            filename = Encoding.unicodeDecode(filename).trim();
            setFilename(this, link, Boolean.TRUE, filename, true);
            link.setProperty(PROPERTY_CACHED_FILENAME, filename);
        } else {
            logger.warning("Failed to find filename");
        }
        /* Try to find precise filesize */
        String filesizeBytesStr = br.getRegex(Pattern.quote(fid) + "/view\"[^\\]]*\\s*,\\s*\"(\\d+)\"\\s*\\]").getMatch(0);
        if (filesizeBytesStr == null) {
            filesizeBytesStr = br.getRegex("\"sizeInBytes\"\\s*:\\s*(\\d+),").getMatch(0);
        }
        if (filesizeBytesStr != null) {
            /* Size of original file but the to be downloaded file could be a re-encoded stream with different file size */
            final long filesize = Long.parseLong(filesizeBytesStr);
            if (filesize > 0) {
                link.setDownloadSize(Long.parseLong(filesizeBytesStr));
            }
        } else {
            /* Log missing filesize */
            /* Usually file size is not given for google documents so in that case, don't log. */
            if (!isGoogleDocument) {
                logger.warning("Failed to find filesize");
            }
        }
        /* For non-downloadable images: Grab URL to the image which is displayed on the website so we can later download that one. */
        if (looksLikeImageFile(link.getName())) {
            String driveViewerURL = br.getRegex("\"(https?://[^/\"]+/([^\"]*/)?drive-viewer/[^\"]+)").getMatch(0);
            if (driveViewerURL != null) {
                driveViewerURL = PluginJSonUtils.unescape(driveViewerURL);
                /*
                 * 2024-06-11: Typically ends with "=s<number>" while in browser it ends with user's screen resolution e.g.: "=w1720-h1264".
                 *
                 * remove those as they may cause different image format/resolution
                 */
                driveViewerURL = driveViewerURL.replaceFirst("=s\\d+[^&\\?]+$", "");
                driveViewerURL = driveViewerURL.replaceFirst("=w\\d+-h\\d+[^&\\?]+$", "");
                link.setProperty(PROPERTY_DIRECTURL_DRIVE_VIEWER, driveViewerURL);
            } else {
                logger.warning("Failed to generate valid driveViewerURL");
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * A function that parses filename/size/last-modified date from single files via "Details View" of Google Drive website.
     *
     * @throws InterruptedException
     */
    private void crawlAdditionalFileInformationFromWebsite(final Browser br, final DownloadLink link, final Account account, final boolean accessFileViewPageIfNotAlreadyDone, final boolean trustAndSetFileInfo) throws PluginException, IOException, InterruptedException {
        if (accessFileViewPageIfNotAlreadyDone && !br.getURL().matches(PATTERN_FILE) && this.websiteWebapiKey == null) {
            this.handleLinkcheckFileOverview(br, link, account, false, false);
        }
        if (this.websiteWebapiKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBrowserWebAPI(br, account);
        final UrlQuery query = new UrlQuery();
        query.add("fields", URLEncode.encodeURIComponent(getSingleFilesFieldsWebsite()));
        query.add("supportsTeamDrives", "true");
        // query.add("includeBadgedLabels", "true");
        query.add("enforceSingleParent", "true");
        query.add("key", URLEncode.encodeURIComponent(this.websiteWebapiKey));
        if (account != null) {
            /* For logged in users */
            br.getPage(WEBAPI_BASE_2 + "/v2internal/files/" + this.getFID(link) + "?" + query.toString());
        } else {
            br.getPage(WEBAPI_BASE + "/v2beta/files/" + this.getFID(link) + "?" + query.toString());
        }
        /* Use same errorhandling than used for official API usage. */
        this.handleErrorsAPI(br, link, account);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        parseFileInfoAPIAndWebsiteWebAPI(this, JsonSchemeType.WEBSITE, link, trustAndSetFileInfo, trustAndSetFileInfo, entries);
    }

    private static void setFilename(final Plugin plg, final DownloadLink link, final Boolean looksLikeGoogleDocument, String filename, final boolean setFinalFilename) {
        if (isGoogleDocument(link) || Boolean.TRUE.equals(looksLikeGoogleDocument)) {
            String googleDocumentExt = link.getStringProperty(PROPERTY_GOOGLE_DOCUMENT_FILE_EXTENSION, ".zip");
            if (!googleDocumentExt.startsWith(".")) {
                googleDocumentExt = "." + googleDocumentExt;
            }
            filename = plg.correctOrApplyFileNameExtension(filename, googleDocumentExt);
        }
        if (setFinalFilename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
    }

    /** Returns directurl for original file download items and google document items. */
    private String getDirecturl(final DownloadLink link, final Account account) throws PluginException {
        if (isGoogleDocument(link)) {
            /* Google document download */
            return getGoogleDocumentDirecturl(link);
        } else {
            /* File download */
            final String lastStoredDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
            if (lastStoredDirecturl != null) {
                return lastStoredDirecturl;
            } else {
                return constructFileDirectDownloadUrl(link, account);
            }
        }
    }

    private String getGoogleDocumentDirecturl(final DownloadLink link) {
        final String forcedDocumentFinalDownloadlink = link.getStringProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL);
        if (forcedDocumentFinalDownloadlink != null) {
            return forcedDocumentFinalDownloadlink;
        } else {
            /**
             * Default: Download google document as .zip file.
             */
            return "https://docs.google.com/feeds/download/documents/export/Export?id=" + this.getFID(link) + "&exportFormat=zip";
        }
    }

    /** Returns URL which should redirect to file download in website mode. */
    private String constructFileDirectDownloadUrl(final DownloadLink link, final Account account) throws PluginException {
        final String fid = getFID(link);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /**
         * E.g. older alternative URL for documents: https://docs.google.com/document/export?format=pdf&id=<fid>&includes_info_params=true
         * </br>
         * Last rev. with this handling: 42866
         */
        String url = "https://drive.google.com";
        /* Minor difference when user is logged in. They don't really check that but let's mimic browser behavior. */
        if (account != null) {
            url += "/u/0/uc";
        } else {
            url += "/uc";
        }
        url += "?id=" + getFID(link) + "&export=download";
        final String fileResourceKey = this.getFileResourceKey(link);
        if (fileResourceKey != null) {
            url += "&resourcekey=" + fileResourceKey;
        }
        return url;
    }

    private String findWebsiteWebAPIKey(final Browser br) {
        String key = br.getRegex("\"([^\"]+)\",null,\"/drive/v2beta\"").getMatch(0);
        if (key == null) {
            key = br.getRegex("\"([^\"]+)\",null,\"/drive/v2beta\"").getMatch(0);
        }
        if (key == null) {
            key = br.getRegex("\"/drive/v2internal\",\"([^\"]+)\"").getMatch(0);
        }
        return key;
    }

    private String getFileViewURL(final DownloadLink link) {
        final String fileResourceKey = this.getFileResourceKey(link);
        String url = "https://drive.google.com/file/d/" + getFID(link) + "/view";
        if (fileResourceKey != null) {
            url += "?resourcekey=" + fileResourceKey;
        }
        return url;
    }

    private String regexConfirmDownloadurl(final Browser br) throws MalformedURLException {
        String ret = br.getRegex("\"([^\"]*?/uc[^\"]+export=download[^<>\"]*?confirm=[^<>\"]+)\"").getMatch(0);
        if (ret == null) {
            /**
             * We're looking for such an URL (parameter positions may vary and 'resourcekey' parameter is not always given): </br>
             * https://drive.google.com/uc?id=<fileID>&export=download&resourcekey=<key>&confirm=t
             */
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                try {
                    final UrlQuery query = UrlQuery.parse(url);
                    if (query.containsKey("export") && query.containsKey("confirm")) {
                        ret = url;
                        break;
                    }
                } catch (final IOException e) {
                    logger.log(e);
                }
            }
        }
        if (ret == null) {
            /* Fallback */
            final Form dlform = br.getFormbyProperty("id", "downloadForm");
            if (dlform != null) {
                ret = dlform.getAction();
            }
        }
        if (ret != null) {
            ret = HTMLEntities.unhtmlentities(ret);
        }
        return ret;
    }

    /**
     * @return: true: Prefer stream download. </br>
     *          false: Do not prefer stream download -> Download original version of file
     */
    private boolean isStreamDownloadPreferred(final DownloadLink link) {
        final int lastUsedQuality = link.getIntegerProperty(PROPERTY_USED_QUALITY, -1);
        if (lastUsedQuality != -1) {
            /* Stream download was used last time for this item -> Force-use it again */
            return true;
        } else if (userPrefersStreamDownload()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean userPrefersStreamDownload() {
        if (PluginJsonConfig.get(GoogleConfig.class).getPreferredVideoQuality() != PreferredVideoQuality.ORIGINAL) {
            return true;
        } else {
            return false;
        }
    }

    private boolean videoStreamShouldBeAvailable(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_CAN_STREAM)) {
            /* We know that this file is streamable. */
            return true;
        } else if (isGoogleDocument(link)) {
            /* Google documents can theoretically have video-like filenames but they can never be streamed! */
            return false;
        } else {
            /* Assume streamable status by filename-extension. */
            if (looksLikeVideoFile(link.getName())) {
                /* Assume that file is streamable. */
                return true;
            } else {
                return false;
            }
        }
    }

    private String getVideoStreamDownloadurl(final Browser br, final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        final boolean isFallback = this.useStreamDownloadAsFallback(link, account);
        try {
            final GoogleConfig cfg = PluginJsonConfig.get(GoogleConfig.class);
            final PreferredVideoQuality qual;
            if (cfg.getPreferredVideoQuality() == PreferredVideoQuality.ORIGINAL) {
                /*
                 * User probably prefers original quality file but stream download handling expects a preferred stream quality ->
                 * Force-Prefer BEST stream quality.
                 */
                qual = PreferredVideoQuality.STREAM_BEST;
            } else {
                qual = cfg.getPreferredVideoQuality();
            }
            int preferredQualityHeight = link.getIntegerProperty(PROPERTY_USED_QUALITY, -1);
            final boolean userHasDownloadedStreamBefore;
            if (preferredQualityHeight != -1) {
                /* Prefer quality that was used for last download attempt. */
                userHasDownloadedStreamBefore = true;
                logger.info("User has downloaded stream before, trying to obtain same quality as before: " + preferredQualityHeight + "p");
            } else {
                userHasDownloadedStreamBefore = false;
                preferredQualityHeight = getPreferredQualityHeight(qual);
            }
            final boolean looksLikeVideoFile = videoStreamShouldBeAvailable(link);
            if (!looksLikeVideoFile) {
                /* Possible developer mistake but let's carry on anyways. */
                logger.warning("Looks like stream download might not be available for this item - prepare for failure!");
            }
            logger.info("Obtaining stream information");
            synchronized (LOCK) {
                final UrlQuery query = new UrlQuery();
                query.add("docid", Encoding.urlEncode(this.getFID(link)));
                query.add("drive_originator_app", "303");
                final String fileResourceKey = this.getFileResourceKey(link);
                if (fileResourceKey != null) {
                    query.add("resourcekey", Encoding.urlEncode(fileResourceKey));
                }
                final String userstring;
                if (account != null) {
                    /* Uses a slightly different request than when not logged in but answer is the same. */
                    /*
                     * E.g. also possible (reduces number of available video qualities):
                     * https://docs.google.com/get_video_info?formats=android&docid=<fuid>
                     */
                    userstring = "u/0";
                } else {
                    userstring = "";
                }
                br.getPage("https://drive.google.com/" + userstring + "/get_video_info?" + query.toString());
                this.handleErrorsWebsite(br, link, account);
            }
            final UrlQuery query = UrlQuery.parse(br.getRequest().getHtmlCode());
            /* Attempt final fallback/edge-case: Check for download of "un-downloadable" streams. */
            final String errorcodeStr = query.get("errorcode");
            if (errorcodeStr != null) {
                link.setProperty(PROPERTY_TIMESTAMP_STREAM_DOWNLOAD_FAILED, System.currentTimeMillis());
                /**
                 * This reason varies depending on the language of the google account in use / header preferred language. </br>
                 * It would be a huge effort to differ between different errormessages so at this moment we will only rely on the
                 * error-code.
                 */
                String errorMessage = query.get("reason");
                if (errorMessage != null) {
                    errorMessage = Encoding.htmlDecode(errorMessage);
                }
                logger.info("Stream download impossible because: " + errorcodeStr + " | " + errorMessage);
                final int errorCode = Integer.parseInt(errorcodeStr);
                if (errorCode == 100) {
                    /* This should never happen but if it does, we know for sure that the file is offline! */
                    /* 2020-11-29: E.g. &errorcode=100&reason=Dieses+Video+ist+nicht+vorhanden.& */
                    if (looksLikeVideoFile) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        /* This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL: Attempted stream download of non streamable file!");
                    }
                } else if (errorCode == 150) {
                    /**
                     * Similar to file-download mode: File is definitely not streamable at this moment! </br>
                     * Reasons for that may vary and there can be different reasons given for the same error-code. </br>
                     * The original file could still be downloadable via account but maybe not at this moment.
                     */
                    /** Similar handling to { @link #errorDownloadQuotaReachedWebsite } */
                    link.setProperty(PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED, System.currentTimeMillis());
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream download failed because: " + errorMessage, getQuotaReachedWaittime());
                } else {
                    /* Unknown error happened */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Stream download failed because: " + errorMessage);
                }
            }
            /* Update limit properties */
            removeQuotaReachedFlags(link, account, true);
            /* Usually same as the title we already have but always with .mp4 ending(?) */
            // final String streamFilename = query.get("title");
            // final String fmt_stream_map = query.get("fmt_stream_map");
            final String url_encoded_fmt_stream_map = query.get("url_encoded_fmt_stream_map");
            if (url_encoded_fmt_stream_map == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final YoutubeHelper dummy = new YoutubeHelper(br, this.getLogger());
            final List<YoutubeStreamData> qualities = new ArrayList<YoutubeStreamData>();
            final String[] qualityInfos = Encoding.urlDecode(url_encoded_fmt_stream_map, false).split(",");
            for (final String qualityInfo : qualityInfos) {
                final UrlQuery qualityQuery = UrlQuery.parse(qualityInfo);
                final YoutubeStreamData yts = dummy.convert(qualityQuery, br.getURL());
                qualities.add(yts);
            }
            if (qualities.isEmpty()) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid state: Expected streaming download but none is available");
            }
            link.setProperty(PROPERTY_TMP_STREAM_DOWNLOAD_ACTIVE, true);
            logger.info("Found " + qualities.size() + " stream qualities");
            String bestQualityDownloadlink = null;
            int bestQualityHeight = 0;
            String selectedQualityDownloadlink = null;
            for (final YoutubeStreamData quality : qualities) {
                if (quality.getItag().getVideoResolution().getHeight() > bestQualityHeight || bestQualityDownloadlink == null) {
                    bestQualityHeight = quality.getItag().getVideoResolution().getHeight();
                    bestQualityDownloadlink = quality.getUrl();
                }
                if (quality.getItag().getVideoResolution().getHeight() == preferredQualityHeight) {
                    selectedQualityDownloadlink = quality.getUrl();
                    break;
                }
            }
            final String chosenQualityDownloadlink;
            final int chosenQuality;
            if (selectedQualityDownloadlink != null) {
                logger.info("Using user preferred quality: " + preferredQualityHeight + "p");
                chosenQuality = preferredQualityHeight;
                chosenQualityDownloadlink = selectedQualityDownloadlink;
            } else {
                /* This should never happen! */
                if (preferredQualityHeight == 0) {
                    logger.info("Using best stream quality: " + bestQualityHeight + "p (BEST)");
                } else {
                    logger.info("Using best stream quality: " + bestQualityHeight + "p (BEST as fallback)");
                }
                chosenQuality = bestQualityHeight;
                chosenQualityDownloadlink = bestQualityDownloadlink;
            }
            this.prepareNonOriginalFileDownload(link);
            if (!userHasDownloadedStreamBefore && link.getView().getBytesLoaded() > 0) {
                /*
                 * User could have started download of original file before: Clear download-progress and potentially partially downloaded
                 * file.
                 */
                logger.info("Resetting progress because user has downloaded parts of original file before but prefers stream download now");
                link.setChunksProgress(null);
            }
            /* Save the quality we've decided to download in case user stops- and resumes download later. */
            link.setProperty(PROPERTY_USED_QUALITY, chosenQuality);
            String filenameOld = link.getName();
            if (StringUtils.isEmpty(filenameOld)) {
                /* This fallback should never be needed! */
                filenameOld = "video";
            }
            /* Update file-extension in filename to .mp4 and add quality identifier to filename if chosen by user. */
            final String videoExt = ".mp4";
            String filenameNew = correctOrApplyFileNameExtension(filenameOld, videoExt);
            if (PluginJsonConfig.get(GoogleConfig.class).isAddStreamQualityIdentifierToFilename()) {
                final String newFilenameEnding = "_" + chosenQuality + "p" + videoExt;
                if (!filenameNew.toLowerCase(Locale.ENGLISH).endsWith(newFilenameEnding)) {
                    filenameNew = filenameNew.replaceFirst("(?i)\\.mp4$", newFilenameEnding);
                }
            }
            if (!filenameNew.equals(filenameOld)) {
                logger.info("Setting new filename for stream download | Old: " + filenameOld + " | New: " + filenameNew);
                setFilename(this, link, Boolean.FALSE, filenameNew, true);
            }
            /* Stream handling was successful so we can delete the 'failed timestamp'. */
            link.removeProperty(PROPERTY_TIMESTAMP_STREAM_DOWNLOAD_FAILED);
            return chosenQualityDownloadlink;
        } catch (final PluginException pe) {
            if (isFallback) {
                /* Expect this to throw an exception */
                logger.log(pe);
                logger.info("Stream download failed due to exception -> Trying to find root cause of that to display more meaningful errormessage to user");
                this.checkUndownloadableConditions(link, account, true);
                /* This should never happen. */
                logger.info("Failed to find more meaningful error -> Throwing initial exception");
            }
            throw pe;
        }
    }

    /**
     * Returns result according to file-extensions listed here:
     * https://support.google.com/drive/answer/2423694/?co=GENIE.Platform%3DiOS&hl=de </br>
     * Last updated: 2020-11-29
     */
    private static boolean looksLikeVideoFile(final String filename) {
        /*
         * 2020-11-30: .ogg is also supported but audio streams seem to be the original files --> Do not allow stream download for .ogg
         * files.
         */
        if (filename == null) {
            return false;
        } else if (new Regex(filename, Pattern.compile(".*\\.(webm|3gp|mov|wmv|mp4|mpeg|mkv|avi|flv|mts|m2ts)$", Pattern.CASE_INSENSITIVE)).matches()) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean looksLikeImageFile(final String filename) {
        if (filename == null) {
            return false;
        }
        final ExtensionsFilterInterface efi = CompiledFiletypeFilter.getExtensionsFilterInterface(Plugin.getFileNameExtensionFromString(filename));
        if (CompiledFiletypeFilter.ImageExtensions.PNG.isSameExtensionGroup(efi)) {
            return true;
        } else {
            return false;
        }
    }

    private int getPreferredQualityHeight(final PreferredVideoQuality quality) {
        switch (quality) {
        case STREAM_360P:
            return 360;
        case STREAM_480P:
            return 480;
        case STREAM_720P:
            return 720;
        case STREAM_1080P:
            return 1080;
        case STREAM_BEST:
            return 0;
        default:
            /* Original quality (no stream download) */
            return -1;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final boolean resume = true;
        final int maxchunks = 0;
        if (useAPIForLinkcheck()) {
            /* Always use API for linkchecking if possible, even if in the end, website is used for downloading! */
            requestFileInformationAPI(link, true);
        }
        /* Account is not always used even if it is available. */
        boolean usedAccount = false;
        String directurl = null;
        String streamDownloadlink = null;
        boolean isNonOriginalFileDownload = false;
        if (useAPIForDownloading(link, account)) {
            /* API download */
            logger.info("Download in API mode");
            this.checkUndownloadableConditions(link, account, false);
            if (isGoogleDocument(link)) {
                /* Expect stored directurl to be available. */
                directurl = this.getGoogleDocumentDirecturl(link);
                if (StringUtils.isEmpty(directurl)) {
                    /* This should never happen */
                    throw getErrorFailedToFindFinalDownloadurl(link);
                }
                isNonOriginalFileDownload = true;
            } else {
                /* Check if user prefers stream download which is only possible via website. */
                if (this.allowVideoStreamDownloadAttempt(link, account)) {
                    if (this.isStreamDownloadPreferred(link)) {
                        logger.info("Attempting stream download in API mode");
                    } else {
                        logger.info("Attempting stream download FALLBACK in API mode");
                    }
                    if (account != null) {
                        usedAccount = true;
                        this.loginDuringLinkcheckOrDownload(br, account);
                    }
                    streamDownloadlink = this.getVideoStreamDownloadurl(br.cloneBrowser(), link, account);
                    if (!StringUtils.isEmpty(streamDownloadlink)) {
                        /* Use found stream downloadlink. */
                        directurl = streamDownloadlink;
                        isNonOriginalFileDownload = true;
                    }
                }
                if (StringUtils.isEmpty(directurl)) {
                    /* API */
                    final UrlQuery queryFile = new UrlQuery();
                    queryFile.appendEncoded("fileId", this.getFID(link));
                    queryFile.add("supportsAllDrives", "true");
                    // queryFile.appendEncoded("fields", getFieldsAPI());
                    queryFile.appendEncoded("key", getAPIKey());
                    queryFile.appendEncoded("alt", "media");
                    directurl = GoogleDrive.API_BASE + "/files/" + this.getFID(link) + "?" + queryFile.toString();
                }
            }
            if (streamDownloadlink != null) {
                logger.info("Downloading stream");
                isNonOriginalFileDownload = true;
            } else {
                logger.info("Downloading original file");
            }
            if (isNonOriginalFileDownload) {
                this.prepareNonOriginalFileDownload(link);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, resume, maxchunks);
        } else {
            /* Website download */
            logger.info("Download in website mode");
            /* Check availablestatus again via website as we're downloading via website. */
            requestFileInformationWebsite(link, account, true);
            if (!this.canDownloadOfficially(link) && looksLikeImageFile(link.getName()) && link.hasProperty(PROPERTY_DIRECTURL_DRIVE_VIEWER)) {
                directurl = link.getStringProperty(PROPERTY_DIRECTURL_DRIVE_VIEWER);
                isNonOriginalFileDownload = true;
            } else if (isGoogleDocument(link)) {
                directurl = this.getGoogleDocumentDirecturl(link);
                isNonOriginalFileDownload = true;
            }
            if (directurl == null) {
                /* No downloadurl found yet -> Check for reasons why that is the case. */
                this.checkUndownloadableConditions(link, account, false);
            }
            if (this.allowVideoStreamDownloadAttempt(link, account)) {
                if (this.isStreamDownloadPreferred(link)) {
                    /* Stream download because user prefers stream download. */
                    logger.info("Attempting stream download in website mode");
                } else {
                    logger.info("Attempting stream download FALLBACK in website mode");
                }
                /**
                 * Sidenote: Files can be blocked for downloading but streaming may still be possible(rare case). </br>
                 * If downloads are blocked because of "too high traffic", streaming can be blocked too!
                 */
                streamDownloadlink = this.getVideoStreamDownloadurl(br.cloneBrowser(), link, account);
                if (!StringUtils.isEmpty(streamDownloadlink)) {
                    directurl = streamDownloadlink;
                    isNonOriginalFileDownload = true;
                }
            }
            if (StringUtils.isEmpty(directurl)) {
                /* Attempt to download original file */
                directurl = getDirecturl(link, account);
            }
            if (StringUtils.isEmpty(directurl)) {
                /* Dead end */
                throw getErrorFailedToFindFinalDownloadurl(link);
            }
            if (streamDownloadlink != null) {
                logger.info("Downloading stream");
                isNonOriginalFileDownload = true;
            } else if (isGoogleDocument(link)) {
                logger.info("Downloading Google Document");
                isNonOriginalFileDownload = true;
            } else {
                logger.info("Downloading file");
            }
            if (isNonOriginalFileDownload) {
                this.prepareNonOriginalFileDownload(link);
            }
            this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.info("Download attempt failed -> Direct download not possible -> One step more might be required or special handling for stream download or google document download");
                br.followConnection(true);
                /**
                 * 2021-02-02: Interesting behavior of offline content: </br>
                 * Returns 403 when accessed via: https://drive.google.com/file/d/<fuid> </br>
                 * Returns 404 when accessed via: https://docs.google.com/uc?id=<fuid>&export=download
                 */
                /* E.g. "This file is too big for Google to virus-scan it - download anyway?" */
                directurl = regexConfirmDownloadurl(br);
                if (directurl != null) {
                    /* We know that the file is online and downloadable. */
                    logger.info("Attempting download if file that is too big for Google v_rus scan");
                    this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, resume, maxchunks);
                } else {
                    this.legacyFallbackHandling(link, account, streamDownloadlink, resume, maxchunks);
                }
            }
            if (account != null) {
                /* Website mode will always use account if available. */
                usedAccount = true;
            }
        }
        if (!this.looksLikeDownloadableContent(this.dl.getConnection())) {
            br.followConnection(true);
            this.downloadFailedLastResortErrorhandling(link, account, streamDownloadlink != null);
        }
        if (JsonConfig.create(GeneralSettings.class).isUseOriginalLastModified() && !hasObtainedInformationFromAPIOrWebAPI(link)) {
            try {
                /*
                 * Do not set final filename/setVerifiedFileSize here as we could be downloading the video stream which has a different
                 * filesize/file-hash.
                 */
                crawlAdditionalFileInformationFromWebsite(br.cloneBrowser(), link, account, true, false);
            } catch (final Exception ignore) {
                logger.log(ignore);
                logger.info("Failed to crawl additional file information for correct \"Last Modified\" date due to Exception");
            }
        }
        /* Update quota properties */
        if (account != null && usedAccount) {
            this.removeQuotaReachedFlags(link, account, streamDownloadlink != null);
        } else {
            this.removeQuotaReachedFlags(link, null, streamDownloadlink != null);
        }
        /** Set final filename here in case previous handling failed to find a good final filename. */
        final String headerFilename = getFileNameFromHeader(this.dl.getConnection());
        if (link.getFinalFileName() == null && !StringUtils.isEmpty(headerFilename)) {
            link.setFinalFileName(headerFilename);
            link.setProperty(PROPERTY_CACHED_FILENAME, headerFilename);
        }
        this.dl.startDownload();
    }

    private void prepareNonOriginalFileDownload(final DownloadLink link) {
        /*
         * The file we are about to download might not be the original anymore -> Delete hash to prevent hashcheck from failing!
         */
        final long fileSize = link.getVerifiedFileSize();
        if (fileSize != -1) {
            // filesize might be from original but download can be different file
            link.setVerifiedFileSize(-1);
            link.setDownloadSize(fileSize);
        }
        link.setHashInfo(null);
    }

    @Deprecated
    private void legacyFallbackHandling(final DownloadLink link, final Account account, final String streamDownloadlink, final boolean resume, final int maxchunks) throws Exception {
        this.dl = null;
        if (!attemptQuickLinkcheck) {
            String directurl = null;
            if (br.getHttpConnection().getResponseCode() == 500 && !isGoogleDocument(link)) {
                crawlAdditionalFileInformationFromWebsite(br.cloneBrowser(), link, account, true, true);
                if (!isGoogleDocument(link)) {
                    /* This should never happen */
                    logger.warning("Not confirmed: No Google Document document -> Something went wrong or item is offline");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Item is not downloadable for unknown reasons");
                }
                logger.info("Confirmed: This is a Google Document");
                /* Working directurl might be available now. */
                directurl = this.getDirecturl(link, account);
                if (StringUtils.isEmpty(directurl)) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Attempting google doc download as fallback");
            } else {
                final boolean isFileDownloadQuotaReached = this.isQuotaReachedWebsiteFile(br, link);
                if (isFileDownloadQuotaReached && this.videoStreamShouldBeAvailable(link) && streamDownloadlink == null && PluginJsonConfig.get(GoogleConfig.class).isAllowStreamDownloadAsFallbackIfFileDownloadQuotaIsReached()) {
                    /* Download quota limit reached -> Try stream download as last resort fallback */
                    logger.info("Attempting forced stream download in an attempt to get around quota limit");
                    /* Very important! Set quota reached flags so stream download handling knows that it was called as a fallback! */
                    this.setQuotaReachedFlags(link, account, false);
                    directurl = this.getVideoStreamDownloadurl(br.cloneBrowser(), link, account);
                    if (StringUtils.isEmpty(directurl)) {
                        /* This should never happen. */
                        logger.info("Stream download fallback failed -> There is nothing we can do to avoid this limit");
                        errorDownloadQuotaReachedWebsite(link, account);
                    } else {
                        logger.info("Attempting stream download as fallback");
                    }
                } else {
                    /* Dead end */
                    this.handleErrorsWebsite(this.br, link, account);
                    this.downloadFailedLastResortErrorhandling(link, account, streamDownloadlink != null);
                }
            }
            this.dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, resume, maxchunks);
        }
        if (this.dl == null) {
            this.handleErrorsWebsite(this.br, link, account);
            this.downloadFailedLastResortErrorhandling(link, account, streamDownloadlink != null);
        }
    }

    private PluginException getErrorFailedToFindFinalDownloadurl(final DownloadLink link) {
        if (isGoogleDocument(link)) {
            return new PluginException(LinkStatus.ERROR_FATAL, "This google document is not downloadable(?)");
        } else {
            return new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /** Returns true if this DownloadLink looks like it contains details that can only be fetched via API/Web-API. */
    private boolean hasObtainedInformationFromAPIOrWebAPI(final DownloadLink link) {
        if (link.getMD5Hash() != null || link.getSha256Hash() != null || link.getSha1Hash() != null || link.getLastModifiedTimestamp() != -1 || link.hasProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL)) {
            return true;
        } else {
            return false;
        }
    }

    /** Checks for conditions which make a file un-downloadable and throws exception if any exist. */
    private void checkUndownloadableConditions(final DownloadLink link, final Account account, final boolean streamDownloadFallbackAttempted) throws PluginException {
        if (this.isInfected(link)) {
            this.errorFileInfected(link);
        } else if (!this.canDownloadOfficially(link) && (streamDownloadFallbackAttempted || !allowVideoStreamDownloadAttempt(link, account))) {
            this.errorCannotDownload(link, streamDownloadFallbackAttempted);
        } else if (isDownloadQuotaReached(link, account) && (streamDownloadFallbackAttempted || !allowVideoStreamDownloadAttempt(link, account))) {
            this.errorDownloadQuotaReachedWebsite(link, account);
        }
    }

    /** Returns true if stream download would theoretically be possible. */
    private boolean allowVideoStreamDownloadAttempt(final DownloadLink link, final Account account) {
        if (!this.videoStreamShouldBeAvailable(link)) {
            /* Stream download impossible. */
            return false;
        } else if (this.isStreamQuotaReached(link)) {
            /* Stream download temporarily impossible */
            return false;
        }
        /* File should be streamable -> Check if user prefers stream download or allows stream download as fallback. */
        if (this.isStreamDownloadPreferred(link) || useStreamDownloadAsFallback(link, account)) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns true, if video stream download is preferred for given DownloadLink. */
    private boolean useStreamDownloadAsFallback(final DownloadLink link, final Account account) {
        final GoogleConfig cfg = PluginJsonConfig.get(GoogleConfig.class);
        if (!this.videoStreamShouldBeAvailable(link)) {
            /* Stream download impossible (not a video file). */
            return false;
        } else if (this.isStreamQuotaReached(link)) {
            /* Stream download temporarily impossible */
            return false;
        } else if (cfg.getPreferredVideoQuality() == PreferredVideoQuality.ORIGINAL) {
            /* User prefers stream download anyways -> Not a fallback situation. */
            return false;
        }
        if (this.canDownloadOfficially(link) && this.isDownloadQuotaReached(link, account) && cfg.isAllowStreamDownloadAsFallbackIfFileDownloadQuotaIsReached()) {
            /*
             * Fallback case 1: File is officially downloadable but download quota is reached and user allows stream download as fallback.
             */
            return true;
        } else if (!this.canDownloadOfficially(link) && cfg.isAllowStreamDownloadAsFallbackIfOfficialDownloadIsDisabled()) {
            /* Fallback case 2: File owner disabled official downloads for this file and user allows stream download in this case. */
            return true;
        } else {
            return false;
        }
    }

    /** Call this when download was attempted and is not possible at all. */
    private void downloadFailedLastResortErrorhandling(final DownloadLink link, final Account account, final boolean isStreamDownload) throws PluginException, InterruptedException, IOException {
        if (br.getHttpConnection().getContentType().contains("application/json")) {
            /* Looks like API response -> Check errors accordingly */
            this.handleErrorsAPI(this.br, link, account);
        }
        this.handleErrorsWebsite(this.br, link, account);
        if (this.dl != null && this.dl.getConnection().getResponseCode() == 416) {
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
        } else {
            if (isGoogleDocument(link)) {
                throw this.getErrorGoogleDocumentDownloadImpossible();
            } else {
                if (!this.canDownloadOfficially(link)) {
                    errorCannotDownload(link, isStreamDownload);
                } else if (isStreamDownload) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: Stream download failed");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: File download failed");
                }
            }
        }
    }

    private void loginDuringLinkcheckOrDownload(final Browser br, final Account account) throws Exception {
        if (PluginJsonConfig.get(GoogleConfig.class).isDebugForceValidateLoginAlways()) {
            this.login(br, account, true);
        } else {
            this.login(br, account, false);
        }
    }

    private void checkErrorBlockedByGoogle(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 403 && br.containsHTML("(?i)but your computer or network may be sending automated queries")) {
            /* 2022-02-24 */
            if (account != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Blocked by Google", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Blocked by Google", 5 * 60 * 1000l);
            }
        }
    }

    /**
     * Checks for errors that can happen at "any time". Preferably call this inside synchronized block especially if an account is available
     * in an attempt to avoid having to solve multiple captchas!
     */
    private void handleErrorsWebsite(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException, IOException {
        checkHandleRateLimit(br, link, account);
        /* Check for other errors */
        checkErrorBlockedByGoogle(br, link, account);
        if (br.containsHTML("(?i)>\\s*Sorry, this file is infected with a virus")) {
            link.setProperty(PROPERTY_IS_INFECTED, true);
            this.errorFileInfected(link);
        } else if (isQuotaReachedWebsiteFile(br, link)) {
            errorDownloadQuotaReachedWebsite(link, account);
        } else if (isAccountRequired(br)) {
            errorAccountRequiredOrPrivateFile(br, link, account);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /**
             * Most likely quota error or "Missing permissions" error. </br>
             * 2021-05-19: Important: This can also happen if e.g. this is a private file and permissions are missing! It is hard to detect
             * the exact reason for error as errormessages differ depending on the user set Google website language! </br>
             * 2022-11-17: Treat this as "Private file" for now
             */
            final boolean usePrivateFileHandlingHere = true;
            if (usePrivateFileHandlingHere) {
                errorAccountRequiredOrPrivateFile(br, link, account);
            } else {
                if (account != null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Insufficient permissions: Private file or quota limit reached", 30 * 60 * 1000l);
                } else {
                    errorDownloadQuotaReachedWebsite(link, account);
                }
            }
        }
    }

    private void errorAccountRequiredOrPrivateFile(final Browser br, final DownloadLink link, final Account account) throws IOException, InterruptedException, PluginException {
        errorAccountRequiredOrPrivateFile(br, link, account, true);
    }

    private void errorAccountRequiredOrPrivateFile(final Browser br, final DownloadLink link, final Account account, final boolean verifyLoginStatus) throws IOException, InterruptedException, PluginException {
        synchronized (LOCK) {
            if (link == null) {
                /* Problem happened during account-check -> Account must be invalid */
                throw new AccountInvalidException();
            } else if (account == null) {
                logger.info("Looks like a private file and no account given -> Ask user to add one");
                throw new AccountRequiredException();
            } else {
                /*
                 * Typically Google will redirect us to accounts.google.com for private files but this can also happen when login session is
                 * expired -> Extra check is needed
                 */
                final String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
                if (account != null && directurl != null) {
                    /**
                     * 2023-01-25: TODO: Temporary errorhandling for "Insufficient permissions" error happening for public files. </br>
                     * 2023-03-01: We should be able to remove this now as login handling has been refactored.
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Try again later or delete your google account and re-add it");
                } else {
                    logger.info("Checking if we got a private file or a login session failure");
                    final Browser brc = br.cloneBrowser();
                    final GoogleHelper helper = new GoogleHelper(brc, this.getLogger());
                    helper.validateCookies(account);
                    /* No exception -> Login session is okay -> We can be sure that this is a private file! */
                    link.setProperty(PROPERTY_LAST_IS_PRIVATE_FILE_TIMESTAMP, System.currentTimeMillis());
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Insufficient permissions: Private file");
                }
            }
        }
    }

    private boolean isQuotaReachedWebsiteFile(final Browser br, final DownloadLink link) {
        if (br.containsHTML("(?i)error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
            return true;
        } else if (br.containsHTML("class=\"uc\\-error\\-caption\"") && StringUtils.equals(DISPOSITION_STATUS_QUOTA_EXCEEDED, link.getStringProperty(PROPERTY_CACHED_LAST_DISPOSITION_STATUS))) {
            return true;
        } else {
            return false;
        }
    }

    /** If this returns true we can be relaively sure that the file we want to download is a private file. */
    private boolean isAccountRequired(final Browser br) {
        if (br.getHost(true).equals("accounts.google.com")) {
            /* User is not logged in but file is private. */
            return true;
        } else if (br.getHttpConnection().getResponseCode() == 401) {
            return true;
        } else if (br.getHttpConnection().getResponseCode() == 403 && br.containsHTML("accounts\\.google\\.com/AccountChooser")) {
            /* User is logged in and file is private but user is lacking permissions to view file. */
            return true;
        } else {
            return false;
        }
    }

    public void handleErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        /*
         * E.g. {"error":{"errors":[{"domain":"global","reason":"downloadQuotaExceeded",
         * "message":"The download quota for this file has been exceeded."}],"code":403,
         * "message":"The download quota for this file has been exceeded."}}
         */
        /*
         * {"error":{"errors":[{"domain":"global","reason":"notFound","message":"File not found: <fileID>."
         * ,"locationType":"parameter","location":"fileId"}],"code":404,"message":"File not found: <fileID>."}}
         */
        /*
         * {"error":{"errors":[{"domain":"usageLimits","reason":"keyInvalid","message":"Bad Request"}],"code":400,"message":"Bad Request"}}
         */
        List<Map<String, Object>> errors = null;
        try {
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            errors = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "error/errors");
        } catch (final Exception ignore) {
            /* Did not get the expected json response */
            logger.warning("Got unexpected API response");
            return;
        }
        if (errors == null || errors.isEmpty()) {
            return;
        }
        /* Most of all times there will be only one errort */
        logger.info("Number of detected errors: " + errors.size());
        int index = 0;
        for (final Map<String, Object> errormap : errors) {
            final boolean isLastItem = index == errors.size() - 1;
            final String reason = (String) errormap.get("reason");
            final String message = (String) errormap.get("message");
            /* First check for known issues */
            if (reason.equalsIgnoreCase("notFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (reason.equalsIgnoreCase("downloadQuotaExceeded")) {
                this.errorQuotaReachedInAPIMode(link, account);
            } else if (reason.equalsIgnoreCase("keyInvalid")) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API key invalid", 2 * 60 * 60 * 1000l);
            } else if (reason.equalsIgnoreCase("cannotDownloadFile")) {
                this.errorCannotDownload(link, false);
            } else if (reason.equalsIgnoreCase("rateLimitExceeded")) {
                throw exceptionRateLimitedSingle;
            }
            /* Now either continue to the next error or handle it as unknown error if it's the last one in our Array of errors */
            logger.info("Unknown error detected: " + message);
            if (isLastItem) {
                if (link == null) {
                    /* Assume it's an account related error */
                    throw new AccountUnavailableException(message, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, message);
                }
            } else {
                index++;
            }
        }
    }

    private boolean isRateLimitedWebsite(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 429) {
            return true;
        } else {
            return false;
        }
    }

    private void checkHandleRateLimit(final Browser br, final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        if (!isRateLimitedWebsite(br)) {
            /* Do nothing */
            return;
        }
        final boolean captchaRequired = CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(br);
        logger.info("Google rate-limit detected | captchaRequired =" + captchaRequired);
        if (link == null) {
            /* Rate-limit has happened during account-check */
            /* 2020-11-29: This captcha should never happen during account-check! It should only happen when requesting files. */
            if (captchaRequired) {
                throw new AccountUnavailableException("Rate limited and captcha blocked", getRateLimitWaittime());
            } else {
                throw new AccountUnavailableException("Rate limited", getRateLimitWaittime());
            }
        } else {
            /* Rate-limit during download-attempt. */
            if (!captchaRequired) {
                throw exceptionRateLimitedSingle;
            } else if (!canHandleGoogleSpecialCaptcha) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Rate limited - captcha required but not implemented yet", getRateLimitWaittime());
            }
            final Form captchaForm = br.getForm(0);
            if (captchaForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            /* This should now redirect back to where we initially wanted to got to! */
            // br.getHeaders().put("X-Client-Data", "0");
            br.submitForm(captchaForm);
            /* Double-check to make sure access was granted */
            if (this.isRateLimitedWebsite(br)) {
                logger.info("Captcha failed and/or rate-limit is still there");
                /*
                 * Do not invalidate captcha result because most likely that was correct but our plugin somehow failed -> Try again later
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate limited and captcha failed", getRateLimitWaittime());
            } else {
                logger.info("Captcha success");
                if (account != null) {
                    /*
                     * Cookies have changed! Store new cookies so captcha won't happen again immediately. This is stored on the current
                     * session and not just IP!
                     */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                } else {
                    /*
                     * TODO: Consider to save- and restore session cookies - this captcha only has to be solved once per session per X time!
                     */
                }
            }
        }
    }

    /**
     * Use this for response 403 or messages like 'file can not be downloaded at this moment'. Such files will usually be downloadable via
     * account. </br>
     * Only use this for failed website download attempts!
     */
    private void errorDownloadQuotaReachedWebsite(final DownloadLink link, final Account account) throws PluginException {
        if (account != null) {
            if (this.isDownloadQuotaReachedAccount(link)) {
                errorQuotaReachedInAllModes(link);
            } else {
                link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT, System.currentTimeMillis());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download quota reached: " + getDownloadQuotaReachedHint1(), getQuotaReachedWaittime());
            }
        } else {
            if (this.isDownloadQuotaReachedAccount(link)) {
                errorQuotaReachedInAllModes(link);
            } else {
                link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, System.currentTimeMillis());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download quota reached: Try later or add Google account and retry", getQuotaReachedWaittime());
            }
        }
    }

    /** Use this for "Quota reached" errors during API download attempts. */
    private void errorQuotaReachedInAPIMode(final DownloadLink link, final Account account) throws PluginException {
        if (PluginJsonConfig.get(GoogleConfig.class).getAPIDownloadMode() == APIDownloadMode.WEBSITE_IF_ACCOUNT_AVAILABLE_AND_FILE_IS_QUOTA_LIMITED && account != null && !this.isDownloadQuotaReachedAccount(link)) {
            link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, System.currentTimeMillis());
            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry with account in website mode to avoid 'Quota reached'");
        } else {
            link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, System.currentTimeMillis());
            if (account != null) {
                if (this.isDownloadQuotaReachedAccount(link)) {
                    errorQuotaReachedInAllModes(link);
                } else {
                    /* We haven't yet attempted to download this link via account. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download quota reached: Try later or adjust API download mode in plugin settings", getQuotaReachedWaittime());
                }
            } else {
                if (this.isDownloadQuotaReachedAccount(link)) {
                    errorQuotaReachedInAllModes(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download quota reached: Try later or add Google account and retry", getQuotaReachedWaittime());
                }
            }
        }
    }

    /**
     * Use this if a link has been attempted to be downloaded with account and still wasn't downloadable.
     *
     * @throws PluginException
     */
    private void errorQuotaReachedInAllModes(final DownloadLink link) throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download quota reached: " + getDownloadQuotaReachedHint1(), getQuotaReachedWaittime());
    }

    private void errorFileInfected(final DownloadLink link) throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "File is v" + "irus infected. Only file owner can download this file.");
    }

    private static String getDownloadQuotaReachedHint1() {
        return "Try later or import the file into your account and download it from there";
    }

    private static long getQuotaReachedWaittime() {
        return PluginJsonConfig.get(GoogleConfig.class).getWaitOnQuotaReachedMinutes() * 60 * 1000;
    }

    private static long getRateLimitWaittime() {
        return 5 * 60 * 1000;
    }

    /**
     * Use this for files which are not downloadable at all (rare case). </br>
     * This mostly gets called if a file is not downloadable according to the Google Drive API.
     */
    private void errorCannotDownload(final DownloadLink link, final boolean isAfterStreamDownloadAttempt) throws PluginException {
        String errorMsg = "Download disabled by file owner!";
        if (!isAfterStreamDownloadAttempt && this.videoStreamShouldBeAvailable(link) && !PluginJsonConfig.get(GoogleConfig.class).isAllowStreamDownloadAsFallbackIfOfficialDownloadIsDisabled()) {
            errorMsg += " Stream download might be possible: Enable stream download as fallback for disabled downloads in plugin settings.";
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, errorMsg);
    }

    private PluginException getErrorGoogleDocumentDownloadImpossible() {
        return new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This Google Document is not downloadable or not available in desired format");
    }

    public void login(final Browser br, final Account account, final boolean forceLoginValidation) throws Exception {
        final boolean loginAPI = false;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && loginAPI) {
            loginAPI(br, account);
        } else {
            /* Website login */
            loginWebsite(br, account, forceLoginValidation);
        }
    }

    public void loginWebsite(final Browser br, final Account account, final boolean forceLoginValidation) throws Exception {
        prepBrowser(br);
        final GoogleHelper helper = new GoogleHelper(br, this.getLogger());
        try {
            helper.login(account, forceLoginValidation);
        } catch (final PluginException e) {
            /* Look for reasons of failure other than invalid login-cookies. */
            this.checkErrorBlockedByGoogle(br, null, account);
            this.checkHandleRateLimit(br, null, account);
            throw e;
        }
        final Cookies userCookies = account.loadUserCookies();
        if (userCookies != null && PluginJsonConfig.get(GoogleConfig.class).isDebugAccountLogin()) {
            /* Old debug check */
            final String cookieOSID = br.getCookie("google.com", "OSID");
            if (cookieOSID == null || cookieOSID.equals("")) {
                logger.warning("OSID cookie has empty value -> This should never happen");
                final Cookie realOSID = userCookies.get("OSID");
                if (realOSID != null && realOSID.getValue().length() > 0) {
                    logger.warning("OSID cookie value is: " + realOSID.getValue() + " | This should never_never happen!!");
                    // br.setCookies(userCookies);
                }
                throw new AccountInvalidException("OSID cookie login failure");
            } else {
                logger.info("cookieOSID cookie looks good");
            }
        }
    }

    /**
     * 2021-02-02: Unfinished work!
     */
    @Deprecated
    private void loginAPI(final Browser br, final Account account) throws IOException, InterruptedException, PluginException {
        /* https://developers.google.com/identity/protocols/oauth2/limited-input-device */
        br.setAllowedResponseCodes(new int[] { 428 });
        String access_token = account.getStringProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN);
        int auth_expires_in = 0;
        String refresh_token = account.getStringProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN);
        final long tokenTimeLeft = account.getLongProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP, 0) - System.currentTimeMillis();
        Map<String, Object> entries = null;
        if (account.hasProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP) && tokenTimeLeft <= 2 * 60 * 1000l) {
            logger.info("Token refresh required");
            final UrlQuery refreshTokenQuery = new UrlQuery();
            refreshTokenQuery.appendEncoded("client_id", getClientID());
            refreshTokenQuery.appendEncoded("client_secret", getClientSecret());
            refreshTokenQuery.appendEncoded("grant_type", refresh_token);
            refreshTokenQuery.appendEncoded("refresh_token", refresh_token);
            br.postPage("https://oauth2.googleapis.com/token", refreshTokenQuery);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            access_token = (String) entries.get("access_token");
            auth_expires_in = ((Number) entries.get("expires_in")).intValue();
            if (StringUtils.isEmpty(access_token)) {
                /* Permanently disable account */
                throw new AccountInvalidException("Token refresh failed");
            }
            logger.info("Successfully obtained new access_token");
            account.setProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN, refresh_token);
            account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP, System.currentTimeMillis() + auth_expires_in * 1000l);
            br.getHeaders().put("Authorization", "Bearer " + access_token);
            return;
        } else if (access_token != null) {
            logger.info("Trust existing token without check");
            br.getHeaders().put("Authorization", "Bearer " + access_token);
            return;
        }
        logger.info("Performing full API login");
        final UrlQuery deviceCodeQuery = new UrlQuery();
        deviceCodeQuery.appendEncoded("client_id", getClientID());
        /*
         * We're using a recommended scope - we don't want to get permissions which we don't make use of:
         * https://developers.google.com/drive/api/v2/about-auth
         */
        deviceCodeQuery.appendEncoded("scope", "https://www.googleapis.com/auth/drive.file");
        br.postPage("https://oauth2.googleapis.com/device/code", deviceCodeQuery);
        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String device_code = (String) entries.get("device_code");
        final String user_code = (String) entries.get("user_code");
        final int user_code_expires_in = ((Number) entries.get("expires_in")).intValue();
        final int interval = ((Number) entries.get("interval")).intValue();
        final String verification_url = (String) entries.get("verification_url");
        int waitedSeconds = 0;
        /* 2020-12-15: Google allows the user to react within 30 minutes - we only allow 5. */
        int maxTotalSeconds = 5 * 60;
        if (user_code_expires_in < maxTotalSeconds) {
            maxTotalSeconds = user_code_expires_in;
        }
        final Thread dialog = showPINLoginInformation(verification_url, user_code);
        try {
            /* Polling */
            final UrlQuery pollingQuery = new UrlQuery();
            pollingQuery.appendEncoded("client_id", getClientID());
            pollingQuery.appendEncoded("client_secret", getClientSecret());
            pollingQuery.appendEncoded("device_code", device_code);
            pollingQuery.appendEncoded("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            do {
                Thread.sleep(interval * 1000l);
                br.postPage("https://oauth2.googleapis.com/token", pollingQuery);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if (entries.containsKey("error")) {
                    logger.info("User hasn't yet confirmed auth");
                    continue;
                } else {
                    access_token = (String) entries.get("access_token");
                    refresh_token = (String) entries.get("refresh_token");
                    auth_expires_in = ((Number) entries.get("expires_in")).intValue();
                    break;
                }
            } while (waitedSeconds < maxTotalSeconds);
        } finally {
            dialog.interrupt();
        }
        if (StringUtils.isEmpty(access_token)) {
            throw new AccountInvalidException("Authorization failed");
        }
        br.getHeaders().put("Authorization", "Bearer " + access_token);
        account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN, access_token);
        account.setProperty(PROPERTY_ACCOUNT_REFRESH_TOKEN, refresh_token);
        account.setProperty(PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP, System.currentTimeMillis() + auth_expires_in * 1000l);
    }

    private Thread showPINLoginInformation(final String pairingURL, final String confirmCode) {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Google Drive - Login";
                        message += "Hallo liebe(r) Google Drive NutzerIn\r\n";
                        message += "Um deinen Google Drive Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "1. Öffne diesen Link im Browser falls das nicht automatisch passiert:\r\n\t'" + pairingURL + "'\t\r\n";
                        message += "2. Gib folgenden Code im Browser ein: " + confirmCode + "\r\n";
                        message += "Dein Account sollte nach einigen Sekunden von JDownloader akzeptiert werden.\r\n";
                    } else {
                        title = "Google Drive - Login";
                        message += "Hello dear Google Drive user\r\n";
                        message += "In order to use your Google Drive account in JDownloader, you need to follow these steps:\r\n";
                        message += "1. Open this URL in your browser if it is not opened automatically:\r\n\t'" + pairingURL + "'\t\r\n";
                        message += "2. Enter this confirmation code in your browser: " + confirmCode + "\r\n";
                        message += "Your account should be accepted in JDownloader within a few seconds.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(5 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(pairingURL);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static final boolean canUseAPI() {
        if (StringUtils.isEmpty(getAPIKey())) {
            return false;
        } else {
            return true;
        }
    }

    public static final String getAPIKey() {
        return PluginJsonConfig.get(GoogleConfig.class).getGoogleDriveAPIKey();
    }

    public static final String getClientID() {
        return null;
        // return "blah";
    }

    public static final String getClientSecret() {
        return null;
        // return "blah";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private void removeQuotaReachedFlags(final DownloadLink link, final Account account, final boolean isStream) {
        if (account != null) {
            link.removeProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT);
            return;
        }
        /* No account = Remove all quota_reached properties. */
        link.removeProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS);
        link.removeProperty(PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED);
    }

    private void setQuotaReachedFlags(final DownloadLink link, final Account account, final boolean isStream) {
        link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS, System.currentTimeMillis());
        if (isStream) {
            link.setProperty(PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED, System.currentTimeMillis());
        }
        if (account != null) {
            link.setProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT, System.currentTimeMillis());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.setProperty(DirectHTTP.PROPERTY_ServerComaptibleForByteRangeRequest, true);
            link.removeProperty(PROPERTY_USED_QUALITY);
            link.removeProperty(PROPERTY_CAN_DOWNLOAD);
            link.removeProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ACCOUNT);
            link.removeProperty(PROPERTY_TIMESTAMP_QUOTA_REACHED_ANONYMOUS);
            link.removeProperty(PROPERTY_TIMESTAMP_STREAM_QUOTA_REACHED);
            link.removeProperty(PROPERTY_TMP_STREAM_DOWNLOAD_ACTIVE);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GoogleConfig.class;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}