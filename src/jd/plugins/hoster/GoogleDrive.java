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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.GoogleConfig;
import org.jdownloader.plugins.components.config.GoogleConfig.APIDownloadMode;
import org.jdownloader.plugins.components.config.GoogleConfig.PreferredVideoQuality;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.components.youtube.YoutubeHelper;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

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
        ret.add(new String[] { "drive.google.com", "docs.google.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            String regex = "https?://" + buildHostsPatternPart(domains) + "/(?:";
            regex += "(?:leaf|open)\\?([^<>\"/]+)?id=[A-Za-z0-9\\-_]+.*";
            regex += "|(?:u/\\d+/)?uc(?:\\?|.*?&)id=[A-Za-z0-9\\-_]+.*";
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
        return -1;
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

    private static Object      CAPTCHA_LOCK               = new Object();
    public static final String API_BASE                   = "https://www.googleapis.com/drive/v3";
    private final String       PATTERN_GDOC               = "https?://.*/document/d/([a-zA-Z0-9\\-_]+).*";
    private final String       PATTERN_FILE               = "https?://.*/file/d/([a-zA-Z0-9\\-_]+).*";
    private final String       PATTERN_FILE_OLD           = "https?://[^/]+/(?:leaf|open)\\?([^<>\"/]+)?id=([A-Za-z0-9\\-_]+).*";
    private final String       PATTERN_FILE_DOWNLOAD_PAGE = "https?://[^/]+/(?:u/\\d+/)?uc(?:\\?|.*?&)id=([A-Za-z0-9\\-_]+).*";
    private final String       PATTERN_VIDEO_STREAM       = "https?://video\\.google\\.com/get_player\\?docid=([A-Za-z0-9\\-_]+)";

    private String getFID(final DownloadLink link) {
        if (link == null) {
            return null;
        } else if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            if (link.getPluginPatternMatcher().matches(PATTERN_GDOC)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_GDOC).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(PATTERN_FILE)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_FILE).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(PATTERN_VIDEO_STREAM)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO_STREAM).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(PATTERN_FILE_OLD)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_FILE_OLD).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(PATTERN_FILE_DOWNLOAD_PAGE)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_FILE_DOWNLOAD_PAGE).getMatch(0);
            } else {
                logger.warning("Developer mistake!! URL with unknown pattern:" + link.getPluginPatternMatcher());
                return null;
            }
        }
    }

    /**
     * Google has added this parameter to some long time shared URLs as of October 2021 to make those safer. </br>
     * https://support.google.com/a/answer/10685032?p=update_drives&visit_id=637698313083783702-233025620&rd=1
     */
    private String getFileResourceKey(final DownloadLink link) {
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("resourcekey");
        } catch (final Throwable ignore) {
            return null;
        }
    }

    /** DownloadLink properties */
    /**
     * Contains the quality modifier of the last chosen quality. This property gets reset on reset DownloadLink to ensure that a user cannot
     * change the quality and then resume the started download with another URL.
     */
    private final String        PROPERTY_USED_QUALITY                          = "USED_QUALITY";
    private static final String PROPERTY_GOOGLE_DOCUMENT                       = "IS_GOOGLE_DOCUMENT";
    private static final String PROPERTY_FORCED_FINAL_DOWNLOADURL              = "FORCED_FINAL_DOWNLOADURL";
    private static final String PROPERTY_CAN_DOWNLOAD                          = "CAN_DOWNLOAD";
    private final String        PROPERTY_CAN_STREAM                            = "CAN_STREAM";
    private final String        PROPERTY_IS_QUOTA_REACHED_ANONYMOUS            = "IS_QUOTA_REACHED_ANONYMOUS";
    private final String        PROPERTY_IS_QUOTA_REACHED_ACCOUNT              = "IS_QUOTA_REACHED_ACCOUNT";
    private final String        PROPERTY_IS_STREAM_QUOTA_REACHED_ANONYMOUS     = "IS_STREAM_QUOTA_REACHED_ANONYMOUS";
    private final String        PROPERTY_IS_STREAM_QUOTA_REACHED_ACCOUNT       = "IS_STREAM_QUOTA_REACHED_ACCOUNT";
    /**
     * 2022-02-20: We store this property but we're not using it at this moment. It is required to access some folders though so it's good
     * to have it set on each DownloadLink if it exists.
     */
    public static final String  PROPERTY_TEAM_DRIVE_ID                         = "TEAM_DRIVE_ID";
    /* Packagizer property */
    public static final String  PROPERTY_ROOT_DIR                              = "root_dir";
    /* Account properties */
    private final String        PROPERTY_ACCOUNT_ACCESS_TOKEN                  = "ACCESS_TOKEN";
    private final String        PROPERTY_ACCOUNT_REFRESH_TOKEN                 = "REFRESH_TOKEN";
    private final String        PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP = "ACCESS_TOKEN_EXPIRE_TIMESTAMP";
    private String              dllink                                         = null;
    private boolean             quotaReachedForceStreamDownloadAsWorkaround    = false;

    public Browser prepBrowser(final Browser pbr) {
        pbr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        pbr.setCustomCharset("utf-8");
        pbr.setFollowRedirects(true);
        pbr.setAllowedResponseCodes(new int[] { 429 });
        return pbr;
    }

    public static Browser prepBrowserAPI(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    private boolean isGoogleDocument(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_GOOGLE_DOCUMENT)) {
            /* Return stored property (= do not care about the URL). */
            return link.getBooleanProperty(PROPERTY_GOOGLE_DOCUMENT, false);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_GDOC)) {
            /* URL looks like GDoc */
            return true;
        } else {
            /* Assume it's not a google document! */
            return false;
        }
    }

    /** Returns true if this link has the worst "Quota reached" status: It is currently not even downloadable via account. */
    private boolean isDownloadQuotaReachedAccount(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT)) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if this link has the worst "Streaming Quota reached" status: It is currently not even downloadable via account. */
    private boolean isStreamQuotaReachedAccount(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ACCOUNT)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this link has the most common "Quota reached" status: It is currently only downloadable via account (or not
     * downloadable at all).
     */
    private boolean isDownloadQuotaReachedAnonymous(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS)) {
            return true;
        } else if (isDownloadQuotaReachedAccount(link)) {
            /* If a file is quota limited in account mode, it is quota limited in anonymous download mode too. */
            return true;
        } else {
            return false;
        }
    }

    /** Returns state of flag set during API availablecheck. */
    private boolean canDownload(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_CAN_DOWNLOAD, true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback name */
            link.setName(this.getFID(link));
        }
        if (canUseAPI()) {
            return this.requestFileInformationAPI(link, isDownload);
        } else {
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            return this.requestFileInformationWebsite(link, account, isDownload);
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final boolean isDownload) throws Exception {
        final String fid = this.getFID(link);
        final String fileResourceKey = getFileResourceKey(link);
        if (fid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBrowserAPI(this.br);
        if (fileResourceKey != null) {
            jd.plugins.decrypter.GoogleDrive.setResourceKeyHeader(br, fid, fileResourceKey);
        }
        final UrlQuery queryFile = new UrlQuery();
        queryFile.appendEncoded("fileId", fid);
        queryFile.add("supportsAllDrives", "true");
        queryFile.appendEncoded("fields", getFieldsAPI());
        queryFile.appendEncoded("key", getAPIKey());
        br.getPage(jd.plugins.hoster.GoogleDrive.API_BASE + "/files/" + fid + "?" + queryFile.toString());
        this.handleErrorsAPI(this.br, link, null);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        parseFileInfoAPI(link, entries);
        return AvailableStatus.TRUE;
    }

    /** Contains all fields we need for file/folder API requests. */
    public static final String getFieldsAPI() {
        return "kind,mimeType,id,name,size,description,md5Checksum,exportLinks,capabilities(canDownload),resourceKey";
    }

    /** Multiple factors decide whether we want to use the API for downloading or use the website. */
    private boolean useAPIForDownloading(final DownloadLink link, final Account account) {
        if (!canUseAPI()) {
            /* No API download possible */
            return false;
        }
        if (this.isGoogleDocument(link)) {
            /* Prefer API download for Google Documents. */
            return true;
        } else if (account != null && PluginJsonConfig.get(GoogleConfig.class).getAPIDownloadMode() == APIDownloadMode.WEBSITE_IF_ACCOUNT_AVAILABLE) {
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

    public static void parseFileInfoAPI(final DownloadLink link, final Map<String, Object> entries) {
        final String mimeType = (String) entries.get("mimeType");
        final String filename = (String) entries.get("name");
        final String md5Checksum = (String) entries.get("md5Checksum");
        final long fileSize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String description = (String) entries.get("description");
        final boolean canDownload = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "capabilities/canDownload")).booleanValue();
        /* E.g. application/vnd.google-apps.document | application/vnd.google-apps.spreadsheet */
        final String googleDriveDocumentType = new Regex(mimeType, "application/vnd\\.google-apps\\.(.+)").getMatch(0);
        if (googleDriveDocumentType != null) {
            final Map<String, Object> exportFormatDownloadurls = entries.containsKey("exportLinks") ? (Map<String, Object>) entries.get("exportLinks") : null;
            parseGoogleDocumentProperties(link, filename, googleDriveDocumentType, exportFormatDownloadurls);
        } else if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (fileSize > -1) {
            link.setVerifiedFileSize(fileSize);
        }
        link.setAvailable(true);
        if (!StringUtils.isEmpty("md5Checksum")) {
            link.setMD5Hash(md5Checksum);
        }
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        link.setProperty(PROPERTY_CAN_DOWNLOAD, canDownload);
    }

    /** Sets filename- and required parameters for GDocs files. */
    public static void parseGoogleDocumentProperties(final DownloadLink link, final String filename, final String googleDriveDocumentType, final Map<String, Object> exportFormatDownloadurls) {
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
        if (!StringUtils.isEmpty(filename)) {
            String docDownloadURL = null;
            String fileExtension = Plugin.getFileNameExtensionFromString(filename);
            if (fileExtension != null && exportFormatDownloadurls != null) {
                fileExtension = fileExtension.toLowerCase(Locale.ENGLISH).replace(".", "");
                final Iterator<Entry<String, Object>> iterator = exportFormatDownloadurls.entrySet().iterator();
                while (iterator.hasNext()) {
                    final String docDownloadURLCandidate = (String) iterator.next().getValue();
                    if (docDownloadURLCandidate.toLowerCase(Locale.ENGLISH).contains("exportformat=" + fileExtension)) {
                        docDownloadURL = docDownloadURLCandidate;
                        break;
                    }
                }
            }
            if (!StringUtils.isEmpty(docDownloadURL)) {
                /* We found an export format suiting our filename-extension --> Prefer that */
                link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, docDownloadURL);
                link.setFinalFileName(filename);
            } else if (googleDriveDocumentType.equalsIgnoreCase("document")) {
                /* Download in OpenDocument format. */
                link.setFinalFileName(Plugin.applyFilenameExtension(filename, ".odt"));
                if (exportFormatDownloadurls != null && exportFormatDownloadurls.containsKey("application/vnd.oasis.opendocument.text")) {
                    link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, exportFormatDownloadurls.get("application/vnd.oasis.opendocument.text"));
                }
            } else if (googleDriveDocumentType.equalsIgnoreCase("spreadsheet")) {
                /* Download in OpenDocument format. */
                link.setFinalFileName(Plugin.applyFilenameExtension(filename, ".ods"));
                if (exportFormatDownloadurls != null && exportFormatDownloadurls.containsKey("application/x-vnd.oasis.opendocument.spreadsheet")) {
                    link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, exportFormatDownloadurls.get("application/x-vnd.oasis.opendocument.spreadsheet"));
                }
            } else {
                /* Unknown document type: Fallback - try to download document as .zip archive. */
                if (exportFormatDownloadurls != null && exportFormatDownloadurls.containsKey("application/zip")) {
                    link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, exportFormatDownloadurls.get("application/zip"));
                }
                link.setFinalFileName(filename + ".zip");
            }
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        /* Login whenever possible */
        if (account != null) {
            login(this.br, account, false);
        }
        prepBrowser(this.br);
        if (this.getFID(link) == null) {
            /** This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = null;
        /* 2020-12-01: Only for testing! */
        final boolean allowExperimentalLinkcheck = false;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && allowExperimentalLinkcheck) {
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Drive-First-Party", "DriveViewer");
            /* 2020-12-01: authuser=0 also for logged-in users! */
            br2.postPage("https://drive.google.com/uc?id=" + this.getFID(link) + "&authuser=0&export=download", "");
            if (br2.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String json = br2.getRegex(".*(\\{.+\\})$").getMatch(0);
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            /* 2020-12-01: E.g. "SCAN_CLEAN" or "TOO_LARGE" or "QUOTA_EXCEEDED" */
            // final String disposition = (String) entries.get("disposition");
            /* 2020-12-01: E.g. "OK" or "WARNING" or "ERROR" */
            final String scanResult = (String) entries.get("scanResult");
            filename = (String) entries.get("fileName");
            final Object filesizeO = entries.get("sizeBytes");
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
            if (filesizeO != null && filesizeO instanceof Number) {
                final long filesize = ((Number) filesizeO).longValue();
                if (filesize > 0) {
                    link.setDownloadSize(filesize);
                    link.setVerifiedFileSize(filesize);
                }
            }
            if (scanResult.equalsIgnoreCase("error")) {
                /* Assume that this has happened: {"disposition":"QUOTA_EXCEEDED","scanResult":"ERROR"} */
                if (isDownload) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "TODO");
                }
                if (link.isNameSet()) {
                    return AvailableStatus.TRUE;
                } else {
                    logger.info("Continue to try to find filename");
                }
            } else {
                this.dllink = (String) entries.get("downloadUrl");
                return AvailableStatus.TRUE;
            }
        }
        /*
         * 2020-09-14: Check for possible direct download first. This will also get around Googles "IP/ISP captcha-blocks" (see code below).
         */
        URLConnectionAdapter con = null;
        if (this.isGoogleDocument(link)) {
            if (link.hasProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL)) {
                con = br.openGetConnection(link.getStringProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL));
            } else {
                con = br.openGetConnection("https://docs.google.com/feeds/download/documents/export/Export?id=" + this.getFID(link) + "&exportFormat=zip");
            }
        } else {
            /* File download */
            con = br.openGetConnection(constructDownloadUrl(link, account));
        }
        /* We hope for the file to be direct-downloadable. */
        if (this.looksLikeDownloadableContent(con)) {
            logger.info("Direct download active");
            final String fileNameFromHeader = getFileNameFromHeader(con);
            if (!StringUtils.isEmpty(fileNameFromHeader)) {
                link.setFinalFileName(fileNameFromHeader);
            }
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            /** There could have been redirects in the meantime -> Be sure to assign the final URL! */
            dllink = con.getURL().toString();
            con.disconnect();
            return AvailableStatus.TRUE;
        }
        logger.info("Direct download not possible -> Continuing linkcheck");
        br.followConnection();
        /**
         * 2021-02-02: Interesting behavior of offline content: </br>
         * Returns 403 when accessed via: https://drive.google.com/file/d/<fuid> </br>
         * Returns 404 when accessed via: https://docs.google.com/uc?id=<fuid>&export=download
         */
        /* Check for offline */
        if (this.isGoogleDocument(link)) {
            /*
             * Google Documents do not provide an additional download page. They're either direct downloadable or there is some kind of
             * error.
             */
            this.handleErrorsWebsite(this.br, link, account);
            /* Document which is not direct-downloadable --> Must be offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getResponseCode() == 404) {
            /* 2020-09-14: Item should be offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /** Check for errors: Some errors prohibit us from continuing linkcheck, others still allow us to find filename/size! */
        this.checkErrorBlockedByGoogle(this.br, link, account);
        if (this.isQuotaReachedWebsiteFile(br) && this.videoStreamShouldBeAvailable(link) && (PluginJsonConfig.get(GoogleConfig.class).isAllowStreamDownloadAsFallbackOnQuotaLimitReached() || this.userPrefersStreamDownload())) {
            /* Quota limit reached -> Download handling should try stream download as last resort fallback */
            logger.info("Quota limit reached -> Attempting stream download as it looks like that might be possible");
            this.quotaReachedForceStreamDownloadAsWorkaround = true;
            return AvailableStatus.TRUE;
        }
        /* Check for all errors if in download mode */
        if (isDownload) {
            this.handleErrorsWebsite(this.br, link, account);
        }
        /** Try to set filename- and size. This will even work for quota-blocked files! */
        filename = br.getRegex("class=\"uc-name-size\"><a href=\"[^\"]+\">([^<>\"]+)<").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        final String filesizeUnsafe = br.getRegex("\\((\\d+(?:[,\\.]\\d)?\\s*[KMGT])\\)</span>").getMatch(0);
        if (filesizeUnsafe != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeUnsafe + "B"));
        }
        /* E.g. "This file is too big for Google to virus-scan it - download anyway?" */
        dllink = regexConfirmDownloadurl(br);
        if (dllink != null) {
            logger.info("File is too big for Google v_rus scan but should be downloadable");
            return AvailableStatus.TRUE;
        }
        /** In case we were not able to find a final download-URL until now, we'll have to try the more complicated way ... */
        logger.info("Direct download inactive --> Accessing download Overview");
        if (isDownload) {
            synchronized (CAPTCHA_LOCK) {
                br.getPage(getFileViewURL(link));
                this.handleErrorsWebsite(this.br, link, account);
            }
        } else {
            br.getPage(getFileViewURL(link));
        }
        /** More errorhandling / offline check */
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)<p class=\"error\\-caption\">\\s*Sorry, we are unable to retrieve this document\\.\\s*</p>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isAccountRequired(br)) {
            link.getLinkStatus().setStatusText("You are missing the rights to download this file");
            throw new AccountRequiredException();
        }
        this.handleSpecialCaptcha(br, link, account);
        if (br.containsHTML("video\\.google\\.com/get_player\\?docid=" + Encoding.urlEncode(this.getFID(link)))) {
            link.setProperty(PROPERTY_CAN_STREAM, true);
        } else {
            link.removeProperty(PROPERTY_CAN_STREAM);
        }
        /** Only look for/set filename/filesize if it hasn't been done before! */
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("'title'\\s*:\\s*'([^<>\"\\']*?)'").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"filename\"\\s*:\\s*\"([^\"]+)\",").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("(?i)<title>([^\"]+) - Google Drive\\s*</title>").getMatch(0);
            }
            if (filename == null) {
                /*
                 * Chances are high that we have a non-officially-downloadable-document (pdf). PDF is displayed in browser via images (1
                 * image per page) - we would need a decrypter for this.
                 */
                /* 2020-09-14: Handling for this edge case has been removed. Provide example URLs if it happens again! */
                filename = HTMLSearch.searchMetaTag(br, "og:title");
            }
            if (filename != null) {
                filename = Encoding.unicodeDecode(filename.trim());
                link.setName(filename);
            }
        }
        /* Try to find precise filesize */
        final String filesizeBytes = br.getRegex("\"sizeInBytes\"\\s*:\\s*(\\d+),").getMatch(0);
        if (filesizeBytes != null) {
            link.setVerifiedFileSize(Long.parseLong(filesizeBytes));
        }
        return AvailableStatus.TRUE;
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
            ret = br.getRegex("form id=\"downloadForm\"[^<]*action=\"(https?://[^\"]+)\"").getMatch(0);
        }
        if (ret != null) {
            ret = HTMLEntities.unhtmlentities(ret);
        }
        return ret;
    }

    /**
     * @return: true: Allow stream download attempt </br>
     *          false: Do not allow stream download -> Download original version of file
     */
    private boolean isStreamDownloadPreferredAndAllowed(final DownloadLink link) {
        if (userPrefersStreamDownload() && videoStreamShouldBeAvailable(link)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean userPrefersStreamDownload() {
        if (PluginJsonConfig.get(GoogleConfig.class).getPreferredVideoQuality() == PreferredVideoQuality.ORIGINAL) {
            return false;
        } else {
            return true;
        }
    }

    private boolean videoStreamShouldBeAvailable(final DownloadLink link) {
        if (this.isGoogleDocument(link)) {
            return false;
        } else if (link.hasProperty(PROPERTY_CAN_STREAM)) {
            /* We know that file is streamable. */
            return true;
        } else {
            if (isVideoFile(link.getName())) {
                /* Assume that file is streamable. */
                return true;
            } else {
                return false;
            }
        }
    }

    /** Returns user defined preferred video stream quality. Returns null if user prefers download of original file. */
    private String handleStreamQualitySelection(final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        final GoogleConfig cfg = PluginJsonConfig.get(GoogleConfig.class);
        final PreferredVideoQuality qual = cfg.getPreferredVideoQuality();
        return handleStreamQualitySelection(link, account, qual);
    }

    private String handleStreamQualitySelection(final DownloadLink link, final Account account, final PreferredVideoQuality qual) throws PluginException, IOException, InterruptedException {
        int preferredQualityHeight = link.getIntegerProperty(PROPERTY_USED_QUALITY, -1);
        final boolean userHasDownloadedStreamBefore;
        if (preferredQualityHeight != -1) {
            /* Prefer quality that was used for last download attempt. */
            userHasDownloadedStreamBefore = true;
        } else {
            userHasDownloadedStreamBefore = false;
            preferredQualityHeight = getPreferredQualityHeight(qual);
        }
        /* Some guard clauses: Conditions in which this function should have never been called. */
        if (preferredQualityHeight <= -1) {
            logger.info("Not attempting stream download because: User prefers original file");
            return null;
        } else if (!videoStreamShouldBeAvailable(link)) {
            logger.info("Not attempting stream download because: File does not seem to be streamable (no video file)");
            return null;
        }
        logger.info("Attempting stream download");
        synchronized (CAPTCHA_LOCK) {
            if (account != null) {
                /* Uses a slightly different request than when not logged in but answer is the same. */
                /*
                 * E.g. also possible (reduces number of available video qualities):
                 * https://docs.google.com/get_video_info?formats=android&docid=<fuid>
                 */
                br.getPage("https://drive.google.com/u/0/get_video_info?docid=" + this.getFID(link));
            } else {
                br.getPage("https://drive.google.com/get_video_info?docid=" + this.getFID(link));
            }
            this.handleErrorsWebsite(this.br, link, account);
        }
        final UrlQuery query = UrlQuery.parse(br.toString());
        /* Attempt final fallback/edge-case: Check for download of "un-downloadable" streams. */
        final String errorcodeStr = query.get("errorcode");
        final String errorReason = query.get("reason");
        if (errorcodeStr != null && errorcodeStr.matches("\\d+")) {
            final int errorCode = Integer.parseInt(errorcodeStr);
            if (errorCode == 100) {
                /* This should never happen but if it does, we know for sure that the file is offline! */
                /* 2020-11-29: E.g. &errorcode=100&reason=Dieses+Video+ist+nicht+vorhanden.& */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (errorCode == 150) {
                /**
                 * Same as in file-download mode: File is definitely not streamable at this moment! </br>
                 * The original file could still be downloadable via account.
                 */
                /** Similar handling to { @link #errorDownloadQuotaReachedWebsite } */
                if (account != null) {
                    if (this.isDownloadQuotaReachedAccount(link)) {
                        /* This link has already been tried in all download modes and is not downloadable at all at this moment. */
                        errorQuotaReachedInAllModes(link);
                    } else {
                        /* User has never tried non-stream download with account --> This could still work for him. */
                        link.setProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ACCOUNT, true);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream Quota reached: Try later or disable stream download in plugin settings and try again", getQuotaReachedWaittime());
                    }
                } else {
                    if (this.isDownloadQuotaReachedAccount(link)) {
                        /* This link has already been tried in all download modes and is not downloadable at all at this moment. */
                        errorQuotaReachedInAllModes(link);
                    } else {
                        link.setProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ANONYMOUS, true);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream Quota reached: Try later or add Google account and retry", getQuotaReachedWaittime());
                    }
                }
            } else {
                /* Unknown error happened */
                logger.info("Streaming download impossible because: " + errorcodeStr + " | " + errorReason);
                return null;
            }
        }
        /* Update limit properties */
        if (account != null) {
            link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT);
            link.removeProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ACCOUNT);
        } else {
            removeAllQuotaReachedFlags(link);
        }
        /* Usually same as the title we already have but always with .mp4 ending(?) */
        // final String streamFilename = query.get("title");
        // final String fmt_stream_map = query.get("fmt_stream_map");
        String url_encoded_fmt_stream_map = query.get("url_encoded_fmt_stream_map");
        url_encoded_fmt_stream_map = Encoding.urlDecode(url_encoded_fmt_stream_map, false);
        if (url_encoded_fmt_stream_map == null) {
            logger.info("Stream download impossible for unknown reasons");
            return null;
        }
        final YoutubeHelper dummy = new YoutubeHelper(this.br, this.getLogger());
        final List<YoutubeStreamData> qualities = new ArrayList<YoutubeStreamData>();
        final String[] qualityInfos = url_encoded_fmt_stream_map.split(",");
        for (final String qualityInfo : qualityInfos) {
            final UrlQuery qualityQuery = UrlQuery.parse(qualityInfo);
            final YoutubeStreamData yts = dummy.convert(qualityQuery, this.br.getURL());
            qualities.add(yts);
        }
        if (qualities.isEmpty()) {
            logger.warning("Failed to find any stream qualities");
            return null;
        }
        logger.info("Found " + qualities.size() + " qualities");
        String bestQualityDownloadlink = null;
        int bestQualityHeight = 0;
        String selectedQualityDownloadlink = null;
        for (final YoutubeStreamData quality : qualities) {
            if (quality.getItag().getVideoResolution().getHeight() == preferredQualityHeight) {
                selectedQualityDownloadlink = quality.getUrl();
                break;
            } else if (quality.getItag().getVideoResolution().getHeight() > bestQualityHeight) {
                bestQualityHeight = quality.getItag().getVideoResolution().getHeight();
                bestQualityDownloadlink = quality.getUrl();
            }
        }
        final int usedQuality;
        if (selectedQualityDownloadlink != null) {
            logger.info("Using user preferred quality: " + preferredQualityHeight + "p");
            usedQuality = preferredQualityHeight;
        } else if (bestQualityDownloadlink != null) {
            logger.info("Using best stream quality: " + bestQualityHeight + "p");
            selectedQualityDownloadlink = bestQualityDownloadlink;
            usedQuality = bestQualityHeight;
        } else {
            /* This should never happen! */
            logger.warning("Failed to find any quality");
            return null;
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            if (userHasDownloadedStreamBefore) {
                link.setComment("Using FORCED preferred quality: " + preferredQualityHeight + "p | Used quality: " + usedQuality + "p");
            } else {
                link.setComment("Using preferred quality: " + preferredQualityHeight + "p | Used quality: " + usedQuality + "p");
            }
        }
        /** Reset this because hash could possibly have been set before and is only valid for the original file! */
        link.setHashInfo(null);
        if (!userHasDownloadedStreamBefore && link.getView().getBytesLoaded() > 0) {
            /*
             * User could have started download of original file before: Clear download-progress and potentially partially downloaded file.
             */
            logger.info("Resetting progress because user has downloaded parts of original file before but prefers stream download now");
            link.setChunksProgress(null);
            link.setVerifiedFileSize(-1);
            /* Save the quality we've decided to download in case user stops- and resumes download later. */
            link.setProperty(PROPERTY_USED_QUALITY, usedQuality);
        }
        final String filename = link.getName();
        if (filename != null) {
            /* Update extension in filename to .mp4 and add quality identifier to filename if chosen by user. */
            if (PluginJsonConfig.get(GoogleConfig.class).isAddStreamQualityIdentifierToFilename()) {
                link.setFinalFileName(correctOrApplyFileNameExtension(filename, "_" + usedQuality + "p.mp4"));
            } else {
                link.setFinalFileName(correctOrApplyFileNameExtension(filename, ".mp4"));
            }
        }
        return selectedQualityDownloadlink;
    }

    /**
     * Returns result according to file-extensions listed here:
     * https://support.google.com/drive/answer/2423694/?co=GENIE.Platform%3DiOS&hl=de </br>
     * Last updated: 2020-11-29
     */
    private static boolean isVideoFile(final String filename) {
        /*
         * 2020-11-30: .ogg is also supported but audio streams seem to be the original files --> Do not allow streaming download for .ogg
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

    private int getPreferredQualityHeight(final PreferredVideoQuality quality) {
        switch (quality) {
        case STREAM_BEST:
            return 0;
        case STREAM_360P:
            return 360;
        case STREAM_480P:
            return 480;
        case STREAM_720P:
            return 720;
        case STREAM_1080P:
            return 1080;
        default:
            /* Original quality (no stream download) */
            return -1;
        }
    }

    /** Returns URL which should redirect to file download in website mode. */
    private String constructDownloadUrl(final DownloadLink link, final Account account) throws PluginException {
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
        /* Minor difference when user is logged in. They don#t really check that but let's mimic browser behavior. */
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

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        boolean resume = true;
        int maxChunks = 0;
        if (!resume) {
            maxChunks = 1;
        }
        /* Always use API for linkchecking, even if in the end, website is used for downloading! */
        if (canUseAPI()) {
            /* Additionally use API for availablecheck if possible. */
            this.requestFileInformationAPI(link, true);
            if (!this.canDownload(link)) {
                /* File is not downloadable according to API. */
                errorCannotDownload(link);
            }
        }
        final GoogleConfig cfg = PluginJsonConfig.get(GoogleConfig.class);
        boolean usedAccount = false;
        if (useAPIForDownloading(link, account)) {
            /* API download */
            if (!this.canDownload(link)) {
                /*
                 * 2022-10-13: psp: Yes I know duplicated code but please don't touch it because I got some ideas and I don't want to forget
                 * this check if I remove it from the other place in the future.
                 */
                errorCannotDownload(link);
            }
            if (this.isGoogleDocument(link)) {
                /* Expect stored directurl to be available. */
                this.dllink = link.getStringProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL);
                if (StringUtils.isEmpty(this.dllink)) {
                    this.errorGoogleDocumentDownloadImpossible();
                }
            } else {
                /* Check if user prefers stream download which is only possible via website. */
                if (this.isStreamDownloadPreferredAndAllowed(link) && cfg.isPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible()) {
                    if (account != null) {
                        usedAccount = true;
                        this.login(br, account, usedAccount);
                    }
                    final String streamDownloadlink = this.handleStreamQualitySelection(link, account);
                    if (!StringUtils.isEmpty(streamDownloadlink)) {
                        /* Use found stream downloadlink. */
                        this.dllink = streamDownloadlink;
                    }
                }
                if (this.dllink == null) {
                    /* Use API */
                    final UrlQuery queryFile = new UrlQuery();
                    queryFile.appendEncoded("fileId", this.getFID(link));
                    queryFile.add("supportsAllDrives", "true");
                    // queryFile.appendEncoded("fields", getFieldsAPI());
                    queryFile.appendEncoded("key", getAPIKey());
                    queryFile.appendEncoded("alt", "media");
                    this.dllink = jd.plugins.hoster.GoogleDrive.API_BASE + "/files/" + this.getFID(link) + "?" + queryFile.toString();
                }
            }
        } else {
            /* Website download */
            /* Check availablestatus again via website as we're downloading via website. */
            requestFileInformationWebsite(link, account, true);
            if (account != null) {
                usedAccount = true;
            }
            if (StringUtils.isEmpty(this.dllink) && this.isGoogleDocument(link)) {
                this.errorGoogleDocumentDownloadImpossible();
            }
            if (this.quotaReachedForceStreamDownloadAsWorkaround) {
                logger.info("Attempting forced stream download in an attempt to get around quota limit");
                try {
                    final PreferredVideoQuality preferredVideoQuality = cfg.getPreferredVideoQuality();
                    if (preferredVideoQuality == PreferredVideoQuality.ORIGINAL) {
                        /*
                         * User prefers original quality file but stream download handling will download a stream quality -> Prefer BEST
                         * stream quality instead.
                         */
                        this.dllink = this.handleStreamQualitySelection(link, account, PreferredVideoQuality.STREAM_BEST);
                    } else {
                        this.dllink = this.handleStreamQualitySelection(link, account, preferredVideoQuality);
                    }
                } catch (final PluginException ignore) {
                    logger.exception("Stream download fallback failed", ignore);
                }
                if (StringUtils.isEmpty(this.dllink)) {
                    logger.info("Stream download fallback failed -> There is nothing we can do to avoid this limit");
                    errorDownloadQuotaReachedWebsite(link, account);
                }
            } else {
                if (StringUtils.isEmpty(this.dllink)) {
                    /* Last chance errorhandling */
                    this.handleErrorsWebsite(this.br, link, account);
                    /* Give up */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /*
                 * Sidenote: Files can be blocked for downloading but streaming may still be possible(rare case). Usually if downloads are
                 * blocked because of "too high traffic", streaming is blocked too!
                 */
                /**
                 * 2020-11-29: Do NOT try to move this into availablecheck!</br>
                 * Availablecheck can get around Google's "sorry" captcha for downloading original files but this does not work for
                 * streaming! </br>
                 * If a captcha is required and the user wants to download a stream there is no way around it! The user has to solve it!
                 */
                /** Check if stream download is preferred by the user. */
                if (this.isStreamDownloadPreferredAndAllowed(link)) {
                    final String streamDownloadlink = this.handleStreamQualitySelection(link, account);
                    if (!StringUtils.isEmpty(streamDownloadlink)) {
                        this.dllink = streamDownloadlink;
                    }
                }
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (br.getHttpConnection().getContentType().contains("application/json")) {
                /* Looks like API response -> Check errors accordingly */
                this.handleErrorsAPI(this.br, link, account);
            }
            this.handleErrorsWebsite(this.br, link, account);
            if (dl.getConnection().getResponseCode() == 416) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        /* Update quota properties */
        if (account != null && usedAccount) {
            link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT);
        } else {
            link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT);
            link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS);
        }
        /** Set final filename here in case previous handling failed to find a good final filename. */
        final String headerFilename = getFileNameFromHeader(dl.getConnection());
        if (link.getFinalFileName() == null && !StringUtils.isEmpty(headerFilename)) {
            link.setFinalFileName(headerFilename);
        }
        /* 2021-07-23: TODO: Set CRC32 filehash if possible */
        // final String googleHash = this.br.getRequest().getResponseHeader("X-Goog-Hash");
        // if (googleHash != null) {
        // try {
        // /* Hashes are base64 encoded. Multiple hashes can be given. */
        // /* https://cloud.google.com/storage/docs/xml-api/reference-headers#xgooghash */
        // final String crc32 = new Regex(googleHash, "crc32c=([^,]+)").getMatch(0);
        // final String md5 = new Regex(googleHash, "md5=([^,]+)").getMatch(0);
        // if (crc32 != null) {
        // link.setHashInfo(HashInfo.newInstanceSafe(crc32, HashInfo.TYPE.CRC32));
        // }
        // } catch (final Throwable ignore) {
        // }
        // }
        this.dl.startDownload();
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
        handleSpecialCaptcha(br, link, account);
        if (br.getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 too many requests");
        }
        /* Check for other errors */
        checkErrorBlockedByGoogle(br, link, account);
        if (br.containsHTML("(?i)error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
            errorDownloadQuotaReachedWebsite(link, account);
        } else if (isQuotaReachedWebsiteFile(br)) {
            errorDownloadQuotaReachedWebsite(link, account);
        } else if (isAccountRequired(br)) {
            if (link == null) {
                /* Looks like failed login -> Should never happen */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failure", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                if (account != null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Insufficient permissions (private file)?", 30 * 60 * 1000l);
                } else {
                    throw new AccountRequiredException();
                }
            }
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /**
             * Most likely quota error or "Missing permissions" error. </br>
             * 2021-05-19: Important: This can also happen if e.g. this is a private file and permissions are missing! It is hard to detect
             * the exact reason for error as errormessages differ depending on the user set Google website language!
             */
            if (account != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Insufficient permissions (private file) or quota limit reached", 30 * 60 * 1000l);
            } else {
                errorDownloadQuotaReachedWebsite(link, account);
            }
        }
    }

    private boolean isQuotaReachedWebsiteFile(final Browser br) {
        if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequired(final Browser br) {
        if (br.getHost(true).equals("accounts.google.com")) {
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
        Map<String, Object> errormap = null;
        List<Object> errorsO = null;
        try {
            errormap = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            errormap = (Map<String, Object>) errormap.get("error");
            errorsO = (List<Object>) errormap.get("errors");
        } catch (final Throwable ignore) {
            /* Did not get the expected json response */
            logger.warning("Got unexpected API response");
            return;
        }
        if (errorsO == null || errorsO.size() == 0) {
            return;
        }
        /* Most of all times there will be only one errort */
        logger.info("Number of detected errors: " + errorsO.size());
        int index = 0;
        for (final Object errorO : errorsO) {
            final boolean isLastItem = index == errorsO.size() - 1;
            errormap = (Map<String, Object>) errorO;
            final String reason = (String) errormap.get("reason");
            final String message = (String) errormap.get("message");
            /* First check for known issues */
            if (reason.equalsIgnoreCase("notFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (reason.equalsIgnoreCase("downloadQuotaExceeded")) {
                this.errorQuotaReachedInAPIMode(link, account);
            } else if (reason.equalsIgnoreCase("keyInvalid")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API key invalid", 3 * 60 * 60 * 1000l);
            } else if (reason.equalsIgnoreCase("cannotDownloadFile")) {
                this.errorCannotDownload(link);
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

    private boolean requiresSpecialCaptcha(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 429 && br.getURL().contains("/sorry/index")) {
            return true;
        } else {
            return false;
        }
    }

    private void handleSpecialCaptcha(final Browser br, final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        if (requiresSpecialCaptcha(br)) {
            if (link == null) {
                /* 2020-11-29: This captcha should never happen during account-check! It should only happen when requesting files. */
                throw new AccountUnavailableException("Captcha blocked", 5 * 60 * 1000l);
            } else {
                /*
                 * 2020-09-09: Google is sometimes blocking users/whole ISP IP subnets so they need to go through this step in order to e.g.
                 * continue downloading.
                 */
                logger.info("Google 'ISP/IP block captcha' detected");
                /*
                 * 2020-09-14: TODO: This handling doesn't work so we'll at least display a meaningful errormessage. The captcha should
                 * never occur anyways as upper handling will try to avoid it!
                 */
                final boolean canSolveCaptcha = false;
                if (!canSolveCaptcha) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Google blocked your IP - captcha required but not implemented yet");
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
                if (br.getHttpConnection().getResponseCode() == 429) {
                    logger.info("Captcha failed");
                    /*
                     * Do not invalidate captcha result because most likely that was correct but our plugin somehow failed -> Try again
                     * later
                     */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 too many requests: Captcha failed");
                } else {
                    logger.info("Captcha success");
                    if (account != null) {
                        /*
                         * Cookies have changed! Store new cookies so captcha won't happen again immediately. This is stored on the current
                         * session and not just IP!
                         */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                    } else {
                        /* TODO: Save- and restore session cookies - this captcha only has to be solved once per session per X time! */
                    }
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
                link.setProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT, true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: " + getDownloadQuotaReachedHint1(), getQuotaReachedWaittime());
            }
        } else {
            if (this.isDownloadQuotaReachedAccount(link)) {
                errorQuotaReachedInAllModes(link);
            } else {
                link.setProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS, true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: Try later or add Google account and retry", getQuotaReachedWaittime());
            }
        }
    }

    /** Use this for "Quota reached" errors during API download attempts. */
    private void errorQuotaReachedInAPIMode(final DownloadLink link, final Account account) throws PluginException {
        if (PluginJsonConfig.get(GoogleConfig.class).getAPIDownloadMode() == APIDownloadMode.WEBSITE_IF_ACCOUNT_AVAILABLE_AND_FILE_IS_QUOTA_LIMITED && account != null && !this.isDownloadQuotaReachedAccount(link)) {
            link.setProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS, true);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry with account in website mode to avoid 'Quota reached'");
        } else {
            link.setProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS, true);
            if (account != null) {
                if (this.isDownloadQuotaReachedAccount(link)) {
                    errorQuotaReachedInAllModes(link);
                } else {
                    /* We haven't yet attempted to download this link via account. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: Try later or adjust API download mode in plugin settings", getQuotaReachedWaittime());
                }
            } else {
                if (this.isDownloadQuotaReachedAccount(link)) {
                    errorQuotaReachedInAllModes(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: Try later or add Google account and retry", getQuotaReachedWaittime());
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
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: " + getDownloadQuotaReachedHint1(), getQuotaReachedWaittime());
    }

    private static String getDownloadQuotaReachedHint1() {
        return "Try later or import the file into your account and download it from there";
    }

    private static long getQuotaReachedWaittime() {
        return 2 * 60 * 60 * 1000;
    }

    /**
     * Use this for files which are not downloadable at all (rare case). </br>
     * This mostly gets called if a file is not downloadable according to the Google Drive API.
     */
    private void errorCannotDownload(final DownloadLink link) throws PluginException {
        String errorMsg = "Download not allowed!";
        if (this.videoStreamShouldBeAvailable(link)) {
            errorMsg += " Video stream download should be possible: Remove your API key, reset this file and try again.";
        } else {
            errorMsg += " If video streaming is available for this file, remove your Google Drive API key, reset this file and try again.";
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, errorMsg);
    }

    private void errorGoogleDocumentDownloadImpossible() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This Google Document is not downloadable or not available in desired format");
    }

    public void login(final Browser br, final Account account, final boolean forceLoginValidation) throws Exception {
        final boolean loginAPI = false;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && loginAPI) {
            loginAPI(br, account);
        } else {
            /* Website login */
            final GoogleHelper helper = new GoogleHelper(br);
            helper.setLogger(this.getLogger());
            final boolean loggedIN = helper.login(account, forceLoginValidation);
            if (!loggedIN) {
                throw new AccountUnavailableException("Login failed", 2 * 60 * 60 * 1000l);
            }
        }
    }

    /**
     * TODO: Add settings for apiID and apiSecret </br>
     * 2021-02-02: Unfinished work! ...
     */
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
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            access_token = (String) entries.get("access_token");
            auth_expires_in = ((Number) entries.get("expires_in")).intValue();
            if (StringUtils.isEmpty(access_token)) {
                /* Permanently disable account */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Token refresh failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
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
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
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
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Authorization failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setMaxSimultanDownloads(20);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private void removeAllQuotaReachedFlags(final DownloadLink link) {
        link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ACCOUNT);
        link.removeProperty(PROPERTY_IS_QUOTA_REACHED_ANONYMOUS);
        link.removeProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ACCOUNT);
        link.removeProperty(PROPERTY_IS_STREAM_QUOTA_REACHED_ANONYMOUS);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.setProperty("ServerComaptibleForByteRangeRequest", true);
            link.removeProperty(PROPERTY_USED_QUALITY);
            link.removeProperty(PROPERTY_CAN_DOWNLOAD);
            removeAllQuotaReachedFlags(link);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GoogleConfig.class;
    }
}