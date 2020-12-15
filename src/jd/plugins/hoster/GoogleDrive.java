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
import org.jdownloader.plugins.components.config.GoogleConfig.PreferredQuality;
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
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drive.google.com" }, urls = { "https?://(?:www\\.)?(?:docs|drive)\\.google\\.com/(?:(?:leaf|open|uc)\\?([^<>\"/]+)?id=[A-Za-z0-9\\-_]+|(?:a/[a-zA-z0-9\\.]+/)?(?:file|document)/d/[A-Za-z0-9\\-_]+)|https?://video\\.google\\.com/get_player\\?docid=[A-Za-z0-9\\-_]+" })
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

    @Override
    public String rewriteHost(final String host) {
        if (host == null || host.equalsIgnoreCase("docs.google.com")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String id = getFID(link);
        if (id != null) {
            return getHost().concat("://".concat(id));
        } else {
            return super.getLinkID(link);
        }
    }

    private static final String NOCHUNKS                      = "NOCHUNKS";
    private boolean             privatefile                   = false;
    private boolean             fileHasReachedServersideQuota = false;
    private boolean             specialError403               = false;
    /* Connection stuff */
    // private static final boolean FREE_RESUME = true;
    // private static final int FREE_MAXCHUNKS = 0;
    private static final int    FREE_MAXDOWNLOADS             = 20;
    private static Object       CAPTCHA_LOCK                  = new Object();
    public static final String  API_BASE                      = "https://www.googleapis.com/drive/v3";
    private static final String PATTERN_GDOC                  = "https?://[^/]+/document/d/([a-zA-Z0-9\\-_]+)";

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink link) {
        // known url formats
        // https://docs.google.com/file/d/0B4AYQ5odYn-pVnJ0Z2V4d1E5UWc/preview?pli=1
        // can't dl these particular links, same with document/doc, presentation/present and view
        // https://docs.google.com/uc?id=0B4AYQ5odYn-pVnJ0Z2V4d1E5UWc&export=download
        // https://docs.google.com/leaf?id=0B_QJaGmmPrqeZjJkZDFmYzEtMTYzMS00N2Y2LWI2NDUtMjQ1ZjhlZDhmYmY3
        // https://docs.google.com/open?id=0B9Z2XD2XD2iQNmxzWjd1UTdDdnc
        // https://video.google.com/get_player?docid=0B2vAVBc_577958658756vEo2eUk
        if (link == null) {
            return null;
        } else if (link.getDownloadURL() == null) {
            return null;
        } else {
            if (link.getDownloadURL().matches(PATTERN_GDOC)) {
                return new Regex(link.getPluginPatternMatcher(), PATTERN_GDOC).getMatch(0);
            } else {
                String id = new Regex(link.getDownloadURL(), "/file/d/([a-zA-Z0-9\\-_]+)").getMatch(0);
                if (id == null) {
                    id = new Regex(link.getDownloadURL(), "video\\.google\\.com/get_player\\?docid=([A-Za-z0-9\\-_]+)").getMatch(0);
                    if (id == null) {
                        id = new Regex(link.getDownloadURL(), "(?!rev)id=([a-zA-Z0-9\\-_]+)").getMatch(0);
                    }
                }
                return id;
            }
        }
    }

    /**
     * Contains the quality modifier of the last chosen quality. This property gets reset on reset DownloadLink to ensure that a user cannot
     * change the quality and then resume the started download with another URL.
     */
    private static final String PROPERTY_USED_QUALITY                          = "USED_QUALITY";
    private static final String PROPERTY_GOOGLE_DOCUMENT                       = "IS_GOOGLE_DOCUMENT";
    private static final String PROPERTY_FORCED_FINAL_DOWNLOADURL              = "FORCED_FINAL_DOWNLOADURL";
    private static final String PROPERTY_CAN_DOWNLOAD                          = "CAN_DOWNLOAD";
    /* Packagizer property */
    public static final String  PROPERTY_ROOT_DIR                              = "root_dir";
    /* Account properties */
    private static final String PROPERTY_ACCOUNT_ACCESS_TOKEN                  = "ACCESS_TOKEN";
    private static final String PROPERTY_ACCOUNT_REFRESH_TOKEN                 = "REFRESH_TOKEN";
    private static final String PROPERTY_ACCOUNT_ACCESS_TOKEN_EXPIRE_TIMESTAMP = "ACCESS_TOKEN_EXPIRE_TIMESTAMP";
    public String               agent                                          = null;
    private boolean             isStreamable                                   = false;
    private String              dllink                                         = null;

    /** Only call this if the user is not logged in! */
    public Browser prepBrowser(Browser pbr) {
        // used within the decrypter also, leave public
        // language determined by the accept-language
        // user-agent required to use new ones otherwise blocks with javascript notice.
        if (pbr == null) {
            pbr = new Browser();
        }
        if (agent == null) {
            agent = UserAgents.stringUserAgent();
        }
        pbr.getHeaders().put("User-Agent", agent);
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
        /* Use result by property if existant */
        if (link.hasProperty(PROPERTY_GOOGLE_DOCUMENT)) {
            return link.getBooleanProperty(PROPERTY_GOOGLE_DOCUMENT, false);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_GDOC)) {
            /* URL looks like GDoc */
            return true;
        } else {
            return false;
        }
    }

    private boolean canDownload(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_CAN_DOWNLOAD, true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        /* TODO: Decide whether to use website- or API here. */
        if (canUseAPI()) {
            return this.requestFileInformationAPI(link, isDownload);
        } else {
            return this.requestFileInformationWebsite(link, null, isDownload);
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final boolean isDownload) throws Exception {
        final String fid = this.getFID(link);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBrowserAPI(this.br);
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
        return "kind,mimeType,id,name,size,description,md5Checksum,exportLinks,capabilities(canDownload)";
    }

    /** Multiple factors decide whether or not we want to prefer using the API for downloading. */
    private boolean useAPIForDownloading(final DownloadLink link, final Account account) {
        if (!canUseAPI()) {
            return false;
        } else if (this.isGoogleDocument(link)) {
            /* 2020-12-10: Website mode cannot handle gdocs downloads (yet). */
            return true;
        } else if (account != null) {
            /* For all other downloads: Prefer download via website with account to avoid "quota reached" errors. */
            return false;
        } else {
            return true;
        }
    }

    public static void parseFileInfoAPI(final DownloadLink link, final Map<String, Object> entries) {
        final String mimeType = (String) entries.get("mimeType");
        final String filename = (String) entries.get("name");
        final String md5Checksum = (String) entries.get("md5Checksum");
        final long fileSize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
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
        if (fileSize > 0) {
            link.setDownloadSize(fileSize);
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
        if (filename != null) {
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
                /* Unknown document type: Fallback - try to download .zip */
                /* TODO: Check if .zip is always given and/or add selection for preferred format */
                if (exportFormatDownloadurls != null && exportFormatDownloadurls.containsKey("application/zip")) {
                    link.setProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, exportFormatDownloadurls.get("application/zip"));
                }
                link.setFinalFileName(filename + ".zip");
            }
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, Account account, final boolean isDownload) throws Exception {
        this.br = new Browser();
        privatefile = false;
        fileHasReachedServersideQuota = false;
        /* Prefer given account vs random account */
        if (account == null) {
            account = AccountController.getInstance().getValidAccount(this.getHost());
        }
        if (account != null) {
            login(br, account, false);
        } else {
            prepBrowser(br);
        }
        if (getFID(link) == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = null;
        String filesizeStr = null;
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
                fileHasReachedServersideQuota = true;
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
        {
            /*
             * 2020-09-14: Check for possible direct download first. This will also get around Googles "IP/ISP captcha-blocks" (see code
             * below).
             */
            URLConnectionAdapter con = null;
            try {
                if (this.isGoogleDocument(link)) {
                    if (link.hasProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL)) {
                        this.dllink = link.getStringProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL);
                    } else {
                        this.dllink = "https://docs.google.com/feeds/download/documents/export/Export?id=" + this.getFID(link) + "&exportFormat=zip";
                    }
                    con = br.openGetConnection(this.dllink);
                } else {
                    con = br.openGetConnection(constructDownloadUrl(link));
                }
                if (this.looksLikeDownloadableContent(con)) {
                    logger.info("Direct download active");
                    final String fileName = getFileNameFromHeader(con);
                    if (!StringUtils.isEmpty(fileName)) {
                        link.setFinalFileName(fileName);
                    }
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    dllink = con.getURL().toString();
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                    if (this.isGoogleDocument(link)) {
                        this.handleErrors(this.br, link, account);
                        /* Document which is not direct-downloadable --> Must be offline */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (con.getResponseCode() == 403) {
                        /*
                         * 2020-09-14: E.g. "Sorry[...] but your computer or network may be sending automated queries"[2020-09-14: Retry
                         * with active Google account can 'fix' this.] or rights-issue ...
                         */
                        specialError403 = true;
                    } else if (con.getResponseCode() == 404) {
                        /* 2020-09-14: File should be offline */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        filename = br.getRegex("class=\"uc-name-size\"><a href=\"[^\"]+\">([^<>\"]+)<").getMatch(0);
                        if (filename != null) {
                            link.setName(Encoding.htmlDecode(filename).trim());
                        }
                        filesizeStr = br.getRegex("\\((\\d+(?:[,\\.]\\d)?\\s*[KMGT])\\)</span>").getMatch(0);
                        if (filesizeStr != null) {
                            link.setDownloadSize(SizeFormatter.getSize(filesizeStr + "B"));
                        }
                    }
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
                /*
                 * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged in
                 * but not necessarily!
                 */
                logger.info("Official download is impossible because quota has been reached");
                fileHasReachedServersideQuota = true;
                if (isDownload) {
                    originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
                } else {
                    /* Continue so other handling can find filename and/or filesize! */
                }
            } else if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
                /*
                 * 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant!
                 */
                /*
                 * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged in
                 * but not necessarily!
                 */
                logger.info("Official download is impossible because quota has been reached2");
                fileHasReachedServersideQuota = true;
                if (isDownload) {
                    originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
                } else {
                    /* Continue so other handling can find filename and/or filesize! */
                }
            } else {
                /* E.g. "This file is too big for Google to virus-scan it - download anyway?" */
                dllink = br.getRegex("\"([^\"]*?/uc\\?export=download[^<>\"]+)\"").getMatch(0);
                if (dllink != null) {
                    dllink = HTMLEntities.unhtmlentities(dllink);
                    logger.info("File is too big for Google v_rus scan but looks like it is downloadable");
                    return AvailableStatus.TRUE;
                } else {
                    logger.info("Direct download inactive --> Download Overview");
                }
            }
        }
        /* In case we were not able to find a download-URL until now, we'll have to try the more complicated way ... */
        logger.info("Trying to find file information via 'download overview' page");
        if (isDownload) {
            synchronized (CAPTCHA_LOCK) {
                br.getPage("https://drive.google.com/file/d/" + getFID(link) + "/view");
                this.handleErrors(this.br, link, account);
            }
        } else {
            br.getPage("https://drive.google.com/file/d/" + getFID(link) + "/view");
        }
        /* 2020-11-29: If anyone knows why we're doing this, please add comment! */
        // String jsredirect = br.getRegex("var url = \\'(http[^<>\"]*?)\\'").getMatch(0);
        // if (jsredirect != null) {
        // final String url_gdrive = "https://drive.google.com/file/d/" + getFID(link) + "/view?ddrp=1";
        // br.getPage(url_gdrive);
        // }
        isStreamable = br.containsHTML("video\\.google\\.com/get_player\\?docid=" + Encoding.urlEncode(this.getFID(link)));
        if (br.containsHTML("<p class=\"error\\-caption\">Sorry, we are unable to retrieve this document\\.</p>") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("accounts.google.com/")) {
            link.getLinkStatus().setStatusText("You are missing the rights to download this file");
            privatefile = true;
            return AvailableStatus.TRUE;
        } else if (this.requiresSpecialCaptcha(br)) {
            logger.info("Don't handle captcha in availablecheck");
            return AvailableStatus.UNCHECKABLE;
        }
        /* Only look for/set filename/filesize if it hasn't been done before! */
        if (filename == null) {
            filename = br.getRegex("'title'\\s*:\\s*'([^<>\"]*?)'").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"filename\"\\s*:\\s*\"([^\"]+)\",").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>([^\"]+) - Google Drive</title>").getMatch(0);
            }
            if (filename == null) {
                /*
                 * Chances are high that we have a non-officially-downloadable-document (pdf). PDF is displayed in browser via images (1
                 * image per page) - we would need a decrypter for this.
                 */
                /* 2020-09-14: Handling for this edge case has been removed. Provide example URLs if it happens again! */
                filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
            }
            if (filename == null && !link.isNameSet()) {
                /* Fallback */
                link.setName(this.getFID(link));
            } else if (filename != null) {
                filename = Encoding.unicodeDecode(filename.trim());
                link.setName(filename);
            }
        }
        if (filesizeStr == null) {
            filesizeStr = br.getRegex("\"sizeInBytes\"\\s*:\\s*(\\d+),").getMatch(0);
            if (filesizeStr == null) {
                // value is within html or a subquent ajax request to fetch json..
                // devnote: to fix, look for the json request to https://clients\d+\.google\.com/drive/v2internal/files/ + fuid and find the
                // filesize, then search for the number within the base page. It's normally there. just not referenced as such.
                filesizeStr = br.getRegex("\\[null,\"" + (filename != null ? Pattern.quote(filename) : "[^\"]") + "\"[^\r\n]+\\[null,\\d+,\"(\\d+)\"\\]").getMatch(0);
            }
            if (filesizeStr != null) {
                link.setVerifiedFileSize(Long.parseLong(filesizeStr));
                link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * @return: true: Allow stream download attempt </br>
     *          false: Do not allow stream download -> Download original version of file
     */
    private boolean attemptStreamDownload(final DownloadLink link) {
        final PreferredQuality qual = PluginJsonConfig.get(GoogleConfig.class).getPreferredQuality();
        final boolean userHasDownloadedStreamBefore = link.hasProperty(PROPERTY_USED_QUALITY);
        final boolean userWantsStreamDownload = qual != PreferredQuality.ORIGINAL;
        final boolean streamShouldBeAvailable = streamShouldBeAvailable(link);
        if (userHasDownloadedStreamBefore) {
            return true;
        } else if (userWantsStreamDownload && streamShouldBeAvailable) {
            return true;
        } else {
            return false;
        }
    }

    private boolean streamShouldBeAvailable(final DownloadLink link) {
        if (this.isGoogleDocument(link)) {
            return false;
        } else {
            String filename = link.getFinalFileName();
            if (filename == null) {
                filename = link.getName();
            }
            return this.isStreamable || isVideoFile(filename);
        }
    }

    /** Returns user preferred stream quality if user prefers stream download else returns null. */
    private String handleStreamQualitySelection(final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        final PreferredQuality qual = PluginJsonConfig.get(GoogleConfig.class).getPreferredQuality();
        final int preferredQualityHeight;
        final boolean userHasDownloadedStreamBefore = link.hasProperty(PROPERTY_USED_QUALITY);
        if (userHasDownloadedStreamBefore) {
            preferredQualityHeight = (int) link.getLongProperty(PROPERTY_USED_QUALITY, 0);
            logger.info("Using last used quality: " + preferredQualityHeight);
        } else {
            preferredQualityHeight = getPreferredQualityHeight(qual);
            logger.info("Using currently selected quality: " + preferredQualityHeight);
        }
        String filename = link.getFinalFileName();
        if (filename == null) {
            filename = link.getName();
        }
        final boolean streamShouldBeAvailable = streamShouldBeAvailable(link);
        if (preferredQualityHeight <= -1 || !streamShouldBeAvailable) {
            logger.info("Downloading original file");
            return null;
        }
        logger.info("Looking for stream download");
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
            this.handleErrors(this.br, link, account);
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
                /* Same as in file-download mode: File is definitely not downloadable at this moment! */
                /* TODO: Add recognition for non-available stream downloads --> To at least have this case logged! */
                // if (isDownload) {
                // downloadTempUnavailableAndOrOnlyViaAccount(account);
                // } else {
                // return AvailableStatus.TRUE;
                // }
                streamDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else {
                logger.info("Streaming download impossible because: " + errorcodeStr + " | " + errorReason);
                return null;
            }
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
        /* TODO: Collect qualities, then do quality selection */
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
                logger.info("Found user preferred quality: " + preferredQualityHeight + "p");
                selectedQualityDownloadlink = quality.getUrl();
                break;
            } else if (quality.getItag().getVideoResolution().getHeight() > bestQualityHeight) {
                bestQualityHeight = quality.getItag().getVideoResolution().getHeight();
                bestQualityDownloadlink = quality.getUrl();
            }
        }
        final int usedQuality;
        if (selectedQualityDownloadlink == null && bestQualityDownloadlink != null) {
            logger.info("Using best stream quality: " + bestQualityHeight + "p");
            selectedQualityDownloadlink = bestQualityDownloadlink;
            usedQuality = bestQualityHeight;
        } else if (selectedQualityDownloadlink != null) {
            usedQuality = preferredQualityHeight;
        } else {
            /* This should never happen! */
            logger.warning("Failed to find any quality");
            return null;
        }
        if (filename != null) {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* Put quality in filename */
                link.setFinalFileName(correctOrApplyFileNameExtension(filename, "_" + usedQuality + "p.mp4"));
            } else {
                link.setFinalFileName(correctOrApplyFileNameExtension(filename, ".mp4"));
            }
        }
        /* TODO: Leave this one in after public release and remove this comment! */
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            if (userHasDownloadedStreamBefore) {
                link.setComment("Using FORCED preferred quality: " + preferredQualityHeight + "p | Used quality: " + usedQuality + "p");
            } else {
                link.setComment("Using preferred quality: " + preferredQualityHeight + "p | Used quality: " + usedQuality + "p");
            }
        }
        /* Reset this because md5hash could possibly have been set during availablecheck before! */
        link.setMD5Hash(null);
        if (!userHasDownloadedStreamBefore) {
            /* User could have started download of original file before: Clear progress! */
            link.setChunksProgress(null);
            link.setVerifiedFileSize(-1);
            /* Save the quality we've decided to download in case user stops and resumes download later. */
            link.setProperty(PROPERTY_USED_QUALITY, usedQuality);
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

    private int getPreferredQualityHeight(final PreferredQuality quality) {
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

    private String constructDownloadUrl(final DownloadLink link) throws PluginException {
        final String fid = getFID(link);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /**
         * E.g. older alternative URL for documents: https://docs.google.com/document/export?format=pdf&id=<fid>&includes_info_params=true
         * </br>
         * Last rev. with this handling: 42866
         */
        return "https://docs.google.com/uc?id=" + getFID(link) + "&export=download";
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        boolean resume = true;
        int maxChunks = 0;
        if (link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) || !resume) {
            maxChunks = 1;
        }
        String streamDownloadlink = null;
        /*
         * TODO: Add another setting: API keys cannot just download an unlimited amount - they're still quota-limited like an anonymous (not
         * logged-in) user!
         */
        boolean checkForAPIErrors = false;
        if (useAPIForDownloading(link, account)) {
            /* Additionally check via API if allowed */
            this.requestFileInformationAPI(link, true);
            if (!this.canDownload(link)) {
                cannotDownload();
            }
            if (this.isGoogleDocument(link)) {
                /* Yeah it's silly but we keep using this variable as it is required for website mode download. */
                this.dllink = link.getStringProperty(PROPERTY_FORCED_FINAL_DOWNLOADURL, null);
                if (StringUtils.isEmpty(this.dllink)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This GDoc is not downloadable or not in format .zip");
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, resume, maxChunks);
            } else {
                streamDownloadlink = this.handleStreamQualitySelection(link, account);
                if (streamDownloadlink != null) {
                    /* Yeah it's silly but we keep using this variable as it is required for website mode download. */
                    this.dllink = streamDownloadlink;
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, resume, maxChunks);
                } else {
                    /* Only check for API errors if */
                    checkForAPIErrors = true;
                    final UrlQuery queryFile = new UrlQuery();
                    queryFile.appendEncoded("fileId", this.getFID(link));
                    queryFile.add("supportsAllDrives", "true");
                    // queryFile.appendEncoded("fields", getFieldsAPI());
                    queryFile.appendEncoded("key", getAPIKey());
                    queryFile.appendEncoded("alt", "media");
                    /* Yeah it's silly but we keep using this variable as it is required for website mode download. */
                    this.dllink = jd.plugins.hoster.GoogleDrive.API_BASE + "/files/" + this.getFID(link) + "?" + queryFile.toString();
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, resume, maxChunks);
                }
            }
        } else {
            /* Additionally use API for availablecheck if possible. */
            if (canUseAPI()) {
                this.requestFileInformationAPI(link, true);
                if (!this.canDownload(link)) {
                    cannotDownload();
                } else if (this.isGoogleDocument(link)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "GDocs are only downloadable when API key is provided");
                }
            }
            requestFileInformationWebsite(link, account, true);
            if (privatefile) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else if (fileHasReachedServersideQuota) {
                originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else if (StringUtils.isEmpty(this.dllink)) {
                /* Last chance errorhandling */
                if (specialError403) {
                    if (account != null) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403: Add Google account or try again later");
                    }
                } else {
                    this.handleErrors(this.br, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /*
             * TODO: Files can be blocked for downloading but streaming may still be possible(rare case). Usually if downloads are blocked
             * because of "too high traffic", streaming is blocked too!
             */
            /**
             * 2020-11-29: Do NOT try to move this into availablecheck! Availablecheck can get around Google's "sorry" captcha for
             * downloading original files but this does not work for streaming! If a captcha is required and the user wants to download a
             * stream there is no way around it! The user has to solve it!
             */
            streamDownloadlink = this.handleStreamQualitySelection(link, account);
            if (streamDownloadlink != null) {
                this.dllink = streamDownloadlink;
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
        }
        /* 2020-03-18: Streams do not have content-disposition but often 206 partial content. */
        // if ((!dl.getConnection().isContentDisposition() && dl.getConnection().getResponseCode() != 206) ||
        // (dl.getConnection().getResponseCode() != 200 && dl.getConnection().getResponseCode() != 206)) {
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                br.followConnection();
            } catch (IOException e) {
                logger.log(e);
            }
            if (checkForAPIErrors) {
                this.handleErrorsAPI(this.br, link, account);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                /* Most likely quota error or "Missing permissions" error. */
                originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else if (dl.getConnection().getResponseCode() == 416) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
            }
            if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
                // so its not possible to download at this time.
                originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
                /*
                 * 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant!
                 */
                originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        try {
            if (link.getFinalFileName() == null) {
                String fileName = getFileNameFromHeader(dl.getConnection());
                if (fileName != null) {
                    link.setFinalFileName(fileName);
                }
            }
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                    link.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                link.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    /**
     * Checks for errors that can happen at "any time". Preferably call this inside synchronized block especially if an account is available
     * in an attempt to avoid having to solve multiple captchas!
     */
    private void handleErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException, IOException {
        if (requiresSpecialCaptcha(br)) {
            handleSpecialCaptcha(link, account);
        } else if (br.getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 too many requests");
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
        } catch (final Throwable e) {
            /* Did not get the expected json response */
            return;
        }
        /* Most of all times there will be only one error -> Handle that */
        if (errorsO == null || errorsO.size() == 0) {
            return;
        }
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
                originalFileDownloadTempUnavailableAndOrOnlyViaAccount(account);
            } else if (reason.equalsIgnoreCase("keyInvalid")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API key invalid", 3 * 60 * 60 * 1000l);
            }
            /* Now either continue to the next error or handle it as unknown error if it's the last one in our Array of errors */
            logger.info("Unknown error detected: " + message);
            if (isLastItem) {
                if (link == null) {
                    /* Assume it's an account related error */
                    throw new AccountUnavailableException(message, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message);
                }
            } else {
                index++;
            }
        }
    }

    private boolean requiresSpecialCaptcha(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 429 && br.getURL().contains("/sorry/index");
    }

    private void handleSpecialCaptcha(final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
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
             * 2020-09-14: TODO: This handling doesn't work so we'll at least display a meaningful errormessage. The captcha should never
             * occur anyways as upper handling will try to avoid it!
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
                 * Do not invalidate captcha result because most likely that was correct but our plugin somehow failed -> Try again later
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

    /** Similar to {@link #originalFileDownloadTempUnavailableAndOrOnlyViaAccount(Account)} but in stream download handling! */
    private void streamDownloadTempUnavailableAndOrOnlyViaAccount(final Account account) throws PluginException {
        if (account != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream-Download Quota reached: Retry later or import the file into your account and dl it from there or disable stream-download or try again with a different account", 2 * 60 * 60 * 1000);
        } else {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream-Download Quota reached: Add Google account and retry or retry later or disable stream-download", 2 * 60 * 60 * 1000);
        }
    }

    /**
     * Use this for response 403 or messages like 'file can not be downloaded at this moment'. Such files will usually be downloadable via
     * account.
     */
    private void originalFileDownloadTempUnavailableAndOrOnlyViaAccount(final Account account) throws PluginException {
        if (account != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: Retry later or import the file into your account and dl it from there or try again with a different account", 2 * 60 * 60 * 1000);
        } else {
            /* 2020-03-10: No warranties that a download will work via account but most times it will! */
            /*
             * 2020-08-10: Updated Exception - rather wait and try again later because such file may be downloadable without account again
             * after some time!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download Quota reached: Retry later or add Google account and retry", 2 * 60 * 60 * 1000);
            // throw new AccountRequiredException();
        }
    }

    private void cannotDownload() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Download not allowed");
    }

    public void login(final Browser br, final Account account, final boolean forceLoginValidation) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            loginAPI(br, account);
        } else {
            final GoogleHelper helper = new GoogleHelper(br);
            helper.setLogger(this.getLogger());
            final boolean loggedIN = helper.login(account, forceLoginValidation);
            if (!loggedIN) {
                throw new AccountUnavailableException("Login failed", 2 * 60 * 60 * 1000l);
            }
        }
    }

    /** TODO: Add settings for apiID and apiSecret */
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
        return !StringUtils.isEmpty(getAPIKey());
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
        /* Free accounts cannot have captchas */
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(20);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.setProperty("ServerComaptibleForByteRangeRequest", true);
            link.removeProperty(GoogleDrive.NOCHUNKS);
            link.removeProperty(GoogleDrive.PROPERTY_USED_QUALITY);
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GoogleConfig.class;
    }
}