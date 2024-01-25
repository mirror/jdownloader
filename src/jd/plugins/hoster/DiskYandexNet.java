//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.DiskYandexNetFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net" }, urls = { "http://yandexdecrypted\\.net/\\d+" })
public class DiskYandexNet extends PluginForHost {
    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://passport.yandex.ru/passport?mode=register&from=cloud&retpath=https%3A%2F%2Fdisk.yandex.ru%2F%3Fauth%3D1&origin=face.en");
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.setCookie(getCurrentDomain(), "ys", "");
        br.setAllowedResponseCodes(new int[] { 429, 500 });
        br.setFollowRedirects(true);
        return br;
    }

    /* Settings values */
    private final String         MOVE_FILES_TO_ACCOUNT                      = "MOVE_FILES_TO_ACCOUNT";
    private final String         DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD         = "EMPTY_TRASH_AFTER_DOWNLOAD";
    private final boolean        MOVE_FILES_TO_ACCOUNT_default              = false;
    private final boolean        DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD_default = false;
    private static final String  NORESUME                                   = "NORESUME";
    /* Some constants which they used in browser */
    public static final String   CLIENT_ID                                  = "12139679121706110849432";
    /* Domains & other login stuff */
    private final String[]       cookie_domains                             = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/", "https://disk.yandex.com.tr/", "https://disk.yandex.kz/" };
    public static final String[] sk_domains                                 = new String[] { "disk.yandex.com", "disk.yandex.ru", "disk.yandex.com.tr", "disk.yandex.ua", "disk.yandex.az", "disk.yandex.com.am", "disk.yandex.com.ge", "disk.yandex.co.il", "disk.yandex.kg", "disk.yandex.lt", "disk.yandex.lv", "disk.yandex.md", "disk.yandex.tj", "disk.yandex.tm", "disk.yandex.uz", "disk.yandex.fr", "disk.yandex.ee", "disk.yandex.kz", "disk.yandex.by" };
    /* Properties */
    public static final String   PROPERTY_HASH                              = "hash_main";
    public static final String   PROPERTY_INTERNAL_FUID                     = "INTERNAL_FUID";
    public static final String   PROPERTY_QUOTA_REACHED                     = "quoty_reached";
    public static final String   PROPERTY_CRAWLED_FILENAME                  = "plain_filename";
    public static final String   PROPERTY_PATH_INTERNAL                     = "path_internal";
    public static final String   PROPERTY_LAST_AUTH_SK                      = "last_auth_sk";
    public static final String   PROPERTY_LAST_URL                          = "last_url";
    public static final String   PROPERTY_MEDIA_TYPE                        = "media_type";
    public static final String   PROPERTY_PREVIEW_URL_ORIGINAL              = "preview_url_original";
    public static final String   PROPERTY_PREVIEW_URL_DEFAULT               = "preview_url_default";
    public static final String   PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN      = "enforce_cookie_login";
    private final String         PROPERTY_ACCOUNT_USERID                    = "account_userid";
    public static final String   APIV1_BASE                                 = "https://cloud-api.yandex.net/v1";
    private static final String  ERRORTEXT_FILE_DOWNLOAD_DISABLED           = "File owner has disabled downloads for this file";
    /*
     * https://tech.yandex.com/disk/api/reference/public-docpage/ 2018-08-09: API(s) seem to work fine again - in case of failure, please
     * disable use_api_file_free_availablecheck ONLY!!
     */
    private static final boolean use_api_file_free_availablecheck           = true;
    private static final boolean use_api_file_free_download                 = true;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (link.getBooleanProperty(DiskYandexNet.NORESUME, false)) {
            return false;
        } else {
            return true;
        }
    }

    private int getMaxChunks(final Account account) {
        return 0;
    }

    /* Make sure we always use our main domain */
    private String getMainLink(final DownloadLink link) throws Exception {
        String mainlink = link.getStringProperty("mainlink");
        if (mainlink == null && getRawHash(link) != null) {
            mainlink = String.format("https://yadi.sk/public/?hash=%s", URLEncode.encodeURIComponent(getRawHash(link)));
        }
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.contains(mainlink, "yadi")) {
            mainlink = "https://disk.yandex.com/" + new Regex(mainlink, "(?i)yandex\\.[^/]+/(.+)").getMatch(0);
        }
        return mainlink;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fuid = link.getStringProperty(PROPERTY_INTERNAL_FUID);
        if (fuid != null) {
            return this.getHost() + "://" + fuid;
        } else {
            return super.getLinkID(link);
        }
    }

    /** Returns currently used domain */
    public static String getCurrentDomain() {
        return "disk.yandex.com";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (getRawHash(link) == null || this.getPath(link) == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (use_api_file_free_availablecheck) {
            return requestFileInformationAPI(link, account);
        } else {
            return requestFileInformationWebsite(link, account);
        }
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        final UrlQuery query = new UrlQuery();
        query.add("public_key", URLEncode.encodeURIComponent(this.getHashWithoutPath(link)));
        query.add("path", URLEncode.encodeURIComponent(this.getPath(link)));
        br.getPage(APIV1_BASE + "/disk/public/resources?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = this.handleErrorsAPI(br, link, account);
        /* No error --> No quota reached issue! */
        link.setProperty(PROPERTY_QUOTA_REACHED, false);
        return parseInformationAPIAvailablecheckFiles(this, link, account, resp);
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        br.getPage(getMainLink(link));
        if (DiskYandexNetFolder.isOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        long filesize_long = -1;
        final String crawledFilename = link.getStringProperty(PROPERTY_CRAWLED_FILENAME);
        String filename = this.br.getRegex("class=\"file-name\" data-reactid=\"[^\"]*?\">([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* Very unsafe method! */
            final String json = br.getRegex("<script type=\"application/json\"[^>]*id=\"store\\-prefetch\"[^>]*>(.*?)<").getMatch(0);
            if (json != null) {
                filename = PluginJSonUtils.getJson(json, "name");
            }
        }
        String filesize_str = br.getRegex("class=\"item-details__name\">Size:</span> ([^<>\"]+)</div>").getMatch(0);
        if (filesize_str == null) {
            /* Language independent */
            filesize_str = this.br.getRegex("class=\"item-details__name\">[^<>\"]+</span> ([\\d\\.]+ (?:B|KB|MB|GB))</div>").getMatch(0);
        }
        if (filesize_str != null) {
            filesize_str = filesize_str.replace(",", ".");
            filesize_long = SizeFormatter.getSize(filesize_str);
        }
        /* Important for account download handling */
        if (br.containsHTML("class=\"[^\"]+antifile-sharing\"")) {
            link.setProperty(PROPERTY_QUOTA_REACHED, true);
        } else {
            link.removeProperty(PROPERTY_QUOTA_REACHED);
        }
        if (crawledFilename == null && filename != null) {
            link.setFinalFileName(filename);
        } else if (crawledFilename != null) {
            link.setFinalFileName(crawledFilename);
        }
        if (filesize_long > -1) {
            link.setDownloadSize(filesize_long);
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationAPIAvailablecheckFiles(final Plugin plugin, final DownloadLink link, final Account account, final Map<String, Object> entries) throws Exception {
        final Number filesize = (Number) entries.get("size");
        final String error = (String) entries.get("error");
        final String hash = (String) entries.get("public_key");
        final String filename = (String) entries.get("name");
        final String path = (String) entries.get("path");
        final String md5 = (String) entries.get("md5");
        final String sha256 = (String) entries.get("sha256");
        if (error != null) {
            return AvailableStatus.FALSE;
        }
        if (sha256 != null) {
            // only one hash is stored
            link.setSha256Hash(sha256);
        }
        if (link.getHashInfo() == null && md5 != null) {
            // sha256 might be invalid
            link.setMD5Hash(md5);
        }
        link.setProperty(PROPERTY_HASH, hash + ":" + path);
        link.setFinalFileName(filename);
        link.setProperty(PROPERTY_CRAWLED_FILENAME, filename);
        if (filesize != null) {
            link.setVerifiedFileSize(filesize.longValue());
        }
        /**
         * Array of direct-URLs -> Find best quality version / original and store that URL. </br>
         * We expect this array to be sorted from best to worst.
         */
        final String directurlOfficialDownload = (String) entries.get("file");
        if (!StringUtils.isEmpty(directurlOfficialDownload)) {
            link.setProperty(getDirecturlProperty(account), directurlOfficialDownload);
        }
        final List<Map<String, Object>> sizes = (List<Map<String, Object>>) entries.get("sizes");
        if (sizes != null && sizes.size() > 0) {
            /* Find best */
            String directurlPreviewOriginal = null;
            String directurlPreviewDefault = null;
            for (final Map<String, Object> size : sizes) {
                final String sizeurl = (String) size.get("url");
                final String sizename = (String) size.get("name");
                if (!StringUtils.isEmpty(sizeurl) && StringUtils.equalsIgnoreCase(sizename, "ORIGINAL")) {
                    if (StringUtils.isEmpty(directurlOfficialDownload)) {
                        directurlPreviewOriginal = sizeurl;
                    }
                } else if (!StringUtils.isEmpty(sizeurl) && StringUtils.equalsIgnoreCase(sizename, "DEFAULT")) {
                    directurlPreviewDefault = sizeurl;
                }
            }
            /* Store for later usage. */
            if (!StringUtils.isEmpty(directurlPreviewOriginal)) {
                link.setProperty(PROPERTY_PREVIEW_URL_ORIGINAL, directurlPreviewOriginal);
            }
            if (directurlPreviewDefault != null) {
                link.setProperty(PROPERTY_PREVIEW_URL_DEFAULT, directurlPreviewDefault);
            }
        }
        link.setProperty(PROPERTY_MEDIA_TYPE, entries.get("media_type"));
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationWebsiteAvailablecheckFiles(final Plugin plugin, final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final Map<String, Object> meta = (Map<String, Object>) entries.get("meta");
        final long filesize = JavaScriptEngineFactory.toLong(meta.get("size"), -1);
        final String filename = (String) entries.get("name");
        // final String path = (String) entries.get("path");
        if (StringUtils.isEmpty(filename) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        dl.setFinalFileName(filename);
        dl.setProperty(PROPERTY_CRAWLED_FILENAME, filename);
        if (filesize > 0) {
            dl.setVerifiedFileSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private static String getDirecturlProperty(final Account account) {
        String str = "directurl";
        if (account != null) {
            str += "_" + account.getType();
        }
        return str;
    }

    private boolean isVideo(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_MEDIA_TYPE), "VIDEO")) {
            return true;
        } else {
            return false;
        }
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        Map<String, Object> directurlresultmap = getDirecturlResult(link, account);
        PluginException exceptionDuringAPILinkcheck = null;
        if (directurlresultmap == null && use_api_file_free_availablecheck) {
            /* Item has never been link-checked via API -> Do this now */
            logger.info("Checking availablestatus via API in hope to find directurl");
            try {
                requestFileInformationAPI(link, account);
                directurlresultmap = getDirecturlResult(link, account);
            } catch (final PluginException exc) {
                if (exc.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    throw exc;
                } else {
                    /* E.g. download quota reached --> Evaluate that later. */
                    logger.info("Exception happened during availablecheck!");
                    exceptionDuringAPILinkcheck = exc;
                }
            }
        }
        final String directurlproperty = getDirecturlProperty(account);
        final String storedPreviouslyGeneratedDirecturl = link.getStringProperty(directurlproperty);
        boolean freshDirecturlHasJustBeenGenerated = false;
        String dllink = null;
        if (directurlresultmap != null) {
            dllink = directurlresultmap.get("url").toString();
            logger.info("Re-using stored url: " + dllink);
            if ((Boolean) directurlresultmap.get("original") == Boolean.FALSE) {
                /* Remove HashInfo as we might be downloading a non-original file now. */
                link.setHashInfo(null);
            }
        } else {
            /* Hard part: We need to generate a fresh direct-url */
            if (account != null) {
                dllink = generateFreshDirecturlAccountMode(link, account);
            } else {
                /* Download without account */
                if (exceptionDuringAPILinkcheck != null) {
                    throw exceptionDuringAPILinkcheck;
                }
                if (use_api_file_free_download) {
                    /**
                     * Download API:
                     *
                     * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=public_key&path=/
                     */
                    /* Free API download. */
                    br.getPage(APIV1_BASE + "/disk/public/resources/download?public_key=" + URLEncode.encodeURIComponent(getHashWithoutPath(link)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(link)));
                    final Map<String, Object> entries = this.handleErrorsAPI(br, link, account);
                    dllink = (String) entries.get("href");
                } else {
                    /* Free website download */
                    if (use_api_file_free_availablecheck) {
                        this.requestFileInformationWebsite(link, account);
                    }
                    final String sk = getSK(this.br);
                    if (sk == null) {
                        logger.warning("sk in website download handling is null");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getHeaders().put("Accept", "*/*");
                    br.getHeaders().put("Content-Type", "text/plain");
                    br.postPageRaw("/public-api-desktop/download-url", String.format("{\"hash\":\"%s\",\"sk\":\"%s\"}", getRawHash(link), sk));
                    handleErrorsFree(br);
                    // TODO: 2023-11-03: Use json parser
                    dllink = PluginJSonUtils.getJsonValue(br, "url");
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to find final downloadurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* Don't do htmldecode because the link will become invalid then */
                    /* sure json will return url with htmlentities? */
                    dllink = HTMLEntities.unhtmlentities(dllink);
                }
                if (dllink != null && dllink.equals("")) {
                    if (isVideo(link)) {
                        dllink = getStreamDownloadurl(br.cloneBrowser(), link, account);
                    } else {
                        /**
                         * Dead end </br>
                         * Some of such files can be viewed in browser e.g. documents and .txt files but all in all we can't really do much
                         * if the owner has disabled download button.
                         */
                        throw new PluginException(LinkStatus.ERROR_FATAL, ERRORTEXT_FILE_DOWNLOAD_DISABLED);
                    }
                }
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                freshDirecturlHasJustBeenGenerated = true;
            }
        }
        if (isHLS(dllink)) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
        } else {
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    handleServerErrors(link);
                    /* This will most likely happen for 0 byte filesize files / serverside broken files. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
                }
            } catch (final Exception e) {
                if (storedPreviouslyGeneratedDirecturl != null) {
                    link.removeProperty(directurlproperty);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
                } else {
                    throw e;
                }
            }
            if (freshDirecturlHasJustBeenGenerated) {
                link.setProperty(directurlproperty, dl.getConnection().getURL().toExternalForm());
            }
            dl.setFilenameFix(true);
        }
        dl.startDownload();
    }

    private Map<String, Object> getDirecturlResult(final DownloadLink link, final Account account) {
        final String directurlproperty = getDirecturlProperty(account);
        String storedPreviouslyGeneratedDirecturl = link.getStringProperty(directurlproperty);
        String directurl = storedPreviouslyGeneratedDirecturl;
        boolean isOriginalFile = true;
        if (directurl == null && StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_MEDIA_TYPE), "image")) {
            /* Look for "preview" image downloadurls [for non-officially-downloadable image files]. */
            directurl = link.getStringProperty(PROPERTY_PREVIEW_URL_ORIGINAL);
            if (directurl != null) {
                logger.info("Downloading original image preview: " + directurl);
            } else {
                directurl = link.getStringProperty(PROPERTY_PREVIEW_URL_DEFAULT);
                if (directurl != null) {
                    logger.info("Downloading default image preview");
                    /* Remove HashInfo as we might be downloading a non-original file now. */
                    link.setHashInfo(null);
                    isOriginalFile = false;
                }
            }
        }
        if (directurl != null) {
            final Map<String, Object> resultmap = new HashMap<String, Object>();
            resultmap.put("url", directurl);
            resultmap.put("original", isOriginalFile);
            return resultmap;
        }
        return null;
    }

    private String generateFreshDirecturlAccountMode(final DownloadLink link, final Account account) throws Exception {
        this.login(account, false);
        requestFileInformationWebsite(link, account);
        final String userID = getUserID(account);
        final String authSk = PluginJSonUtils.getJson(this.br, "authSk");
        if (StringUtils.isEmpty(authSk) || userID == null) {
            /* This should never happen */
            logger.warning("authSk or userID is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_LAST_AUTH_SK, authSk);
        link.setProperty(PROPERTY_LAST_URL, br.getURL());
        /*
         * Move files into account and download them "from there" although user might not have selected this? --> Forced handling, only
         * required if not possible via different way
         */
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Accept", "*/*");
        brc.getHeaders().put("Content-Type", "text/plain");
        final boolean fileIsDownloadQuotaLimitReached = isFileDownloadQuotaReached(link);
        String dllink = null;
        if (!fileIsDownloadQuotaLimitReached) {
            logger.info("Performing normal account download handling");
            final String postdata = String.format("{\"hash\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\",\"options\":{\"hasExperimentVideoWithoutPreview\":true}}", this.getRawHash(link), authSk, userID);
            brc.postPageRaw("/public/api/download-url", URLEncode.encodeURIComponent(postdata));
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            dllink = (String) data.get("url");
            if (!StringUtils.isEmpty(dllink)) {
                return dllink;
            }
            /**
             * Example response when file can only be viewed but not downloaded: </br>
             * {"error":false,"statusCode":200,"code":"","data":{"read_only":true}}
             */
            if (Boolean.TRUE.equals(data.get("read_only"))) {
                logger.warning("Failed to find official downloadurl because file owner has disabled download button");
                if (this.isVideo(link)) {
                    /* For video streams we can try to download the video stream */
                    dllink = getStreamDownloadurl(br.cloneBrowser(), link, account);
                }
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, ERRORTEXT_FILE_DOWNLOAD_DISABLED);
                }
                /* Return streaming downloadlink */
                return dllink;
            } else {
                logger.warning("Failed to find official downloadurl for unknown reasons");
            }
        }
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2024-01-25: Unfinished code down below */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("/client/disk/Downloads");
        final String longSK = br.getRegex("\"sk\":\"([^\"]+)").getMatch(0);
        if (longSK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (getPluginConfig().getBooleanProperty(MOVE_FILES_TO_ACCOUNT, MOVE_FILES_TO_ACCOUNT_default) && fileIsDownloadQuotaLimitReached) {
            /* Move file into users' account as it is quote limited and without doing this it's impossible to download this file. */
            String internal_file_path = getInternalFilePath(link, account);
            logger.info("MoveFileIntoAccount: MoveFileIntoAccount handling is active");
            if (internal_file_path != null) {
                // TODO: Check if file still exists under the path we have; if not we will need to re-import our target-file
                brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                brc.getHeaders().put("Origin", "https://disk.yandex.com");
                brc.getHeaders().put("Referer", "https://disk.yandex.com/client/disk/Download");
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final UrlQuery query = new UrlQuery();
                query.add("idClient", Encoding.urlEncode("12139679121706174438510")); // TODO: Check this
                query.add("sk", Encoding.urlEncode(longSK));
                query.add("_model.0", "resources");
                query.add("sort.0", "mtime");
                query.add("order.0", "0");
                query.add("idContext.0", Encoding.urlEncode("/disk/Downloads"));
                query.add("amount.0", "40");
                query.add("offset.0", "0");
                query.add("withParent.0", "1");
                brc.postPage("/models/?_m=resources", query);
                final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "modesl/{0}/data/resources");
                boolean foundInternalPath = false;
                for (final Map<String, Object> fileObject : resourcelist) {
                    if (fileObject.get("path").toString().equals(internal_file_path)) {
                        foundInternalPath = true;
                        break;
                    }
                }
                if (!foundInternalPath) {
                    /* Remove stored path so we will not try again with this one. */
                    logger.info("Failed to find stored internal filepath: " + internal_file_path);
                    this.saveInternalFilePath(link, account, null);
                    internal_file_path = null;
                }
            }
            if (internal_file_path == null) {
                logger.info("MoveFileIntoAccount: No internal filepath available --> Trying to move file into account");
                /**
                 * 2021-02-10: Possible values for "source": public_web_copy, public_web_copy_limit </br>
                 * public_web_copy_limit is usually used if the files is currently quota limited and cannot be downloaded at all at this
                 * moment. </br>
                 * Both will work but we'll try to choose the one which would also be used via browser.
                 */
                final String copySource;
                if (this.isFileDownloadQuotaReached(link)) {
                    copySource = "public_web_copy_limit";
                } else {
                    copySource = "public_web_copy";
                }
                final String hash = this.getRawHash(link);
                final Map<String, Object> postmap = new HashMap<String, Object>();
                postmap.put("hash", hash);
                postmap.put("name", link.getName());
                postmap.put("lang", "en");
                postmap.put("source", copySource);
                postmap.put("isAlbum", false);
                postmap.put("itemId", null);
                postmap.put("sk", authSk);
                postmap.put("uid", userID);
                brc.postPageRaw("/public/api/save", URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postmap)));
                final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                internal_file_path = (String) JavaScriptEngineFactory.walkJson(entries, "data/path");
                final String oid = (String) JavaScriptEngineFactory.walkJson(entries, "data/oid");
                if (br.containsHTML("\"code\":85")) {
                    logger.info("MoveFileIntoAccount: failed to move file to account: No free space available");
                    // throw new PluginException(LinkStatus.ERROR_FATAL, "No free space available, failed to move file to account");
                    throw new AccountUnavailableException("No free space available, failed to move file to account", 5 * 60 * 1000l);
                } else if (StringUtils.isEmpty(internal_file_path)) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to move file into Yandex account");
                } else if (StringUtils.isEmpty(oid)) {
                    /* Should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to move file into Yandex account: Failed to find oid");
                }
                /* Sometimes the process of 'moving' a file into a cloud account can take some seconds. */
                logger.info("transfer-status: checking");
                boolean fileWasImportedSuccessfully = false;
                Map<String, Object> fileImportResp = null;
                Map<String, Object> fileImportRespData = null;
                for (int i = 1; i < 5; i++) {
                    brc.postPageRaw("https://" + getCurrentDomain() + "/public/api/get-operation-status", URLEncode.encodeURIComponent(String.format("{\"oid\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\"}", oid, authSk, userID)));
                    fileImportResp = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                    fileImportRespData = (Map<String, Object>) fileImportResp.get("data");
                    final String copyState = fileImportRespData.get("state").toString();
                    logger.info("Copy state: " + copyState);
                    if (copyState.equalsIgnoreCase("COMPLETED")) {
                        fileWasImportedSuccessfully = true;
                        break;
                    } else if (copyState.equalsIgnoreCase("FAILED")) {
                        logger.info("Possibly failed to copy file to account");
                        break;
                    } else {
                        sleep(i * 1000l, link);
                    }
                }
                if (!fileWasImportedSuccessfully) {
                    /* Should never happen */
                    String errormessage = "Error while importing file into account";
                    final Map<String, Object> errormap = (Map<String, Object>) fileImportRespData.get("error");
                    if (errormap != null) {
                        errormessage += " | Reason: " + errormap.get("message"); // E.g. "NoFreeSpaceCopyToDisk"
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
                }
                /* Save internal path for later. */
                this.saveInternalFilePath(link, account, internal_file_path);
            } else {
                /* File has already been copied to this account before */
                logger.info("given/stored internal filepath: " + internal_file_path);
            }
            final String thisAuthSK = authSk + ":" + System.currentTimeMillis() / 1000;
            final UrlQuery query = new UrlQuery();
            query.add("idClient", Encoding.urlEncode(CLIENT_ID));
            // TODO: sk is wrong - fix this!
            query.add("sk", Encoding.urlEncode(thisAuthSK));
            query.add("_model.0", "do-get-resource-url");
            query.add("idResource.0", Encoding.urlEncode(internal_file_path));
            brc.postPage("https://" + getCurrentDomain() + "/models/?_m=do-get-resource-url", query);
            dllink = PluginJSonUtils.getJsonValue(brc, "file");
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.startsWith("//")) {
                /* 2018-04-18 */
                dllink = "https:" + dllink;
            }
            if (getPluginConfig().getBooleanProperty(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD_default)) {
                /*
                 * We have a direct downloadable link to that file which will work even after deleting it so let's move it into the trash
                 * right now [before even downloading].
                 */
                moveFileToTrash(brc, link, account, authSk);
                emptyTrash(brc, authSk);
            }
        }
        if (StringUtils.isEmpty(dllink) && fileIsDownloadQuotaLimitReached) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit", 1 * 60 * 60 * 1000l);
        }
        return dllink;
    }

    /**
     * Only call this if you believe that streaming download of this file is possible. </br>
     * This is pretty much only working when user is logged in. In most of all other cases, their bot protection kicks in and blocks this
     * handling.
     */
    private String getStreamDownloadurl(final Browser br, final DownloadLink link, final Account account) throws Exception {
        /* 2023-07-15: For video files which can officially only be streamed and not downloaded. */
        final boolean developerBooleanAllowAttemptStreamingDownload = true;
        if (!developerBooleanAllowAttemptStreamingDownload) {
            throw new PluginException(LinkStatus.ERROR_FATAL, ERRORTEXT_FILE_DOWNLOAD_DISABLED);
        }
        logger.info("Trying to find stream downloadlink");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "text/plain");
        br.getHeaders().put("Origin", "https://" + getCurrentDomain());
        br.getHeaders().put("Referer", this.getMainLink(link));
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Retpath-Y", this.getMainLink(link));
        br.setAllowedResponseCodes(400);
        String sk = link.getStringProperty(PROPERTY_LAST_AUTH_SK);
        String errortextStreamingDownloadFailed = "Streaming download failed.";
        if (account == null) {
            errortextStreamingDownloadFailed += " Add account and try again.";
        }
        if (sk == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortextStreamingDownloadFailed);
        }
        Map<String, Object> entries = null;
        String newSK = null;
        do {
            final String poststring = "%7B%22hash%22%3A%22" + URLEncode.encodeURIComponent(this.getRawHash(link)) + "%22%2C%22sk%22%3A%22" + sk + "%22%7D";
            br.postPageRaw("https://" + getCurrentDomain() + "/public/api/get-video-streams", poststring);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (newSK != null) {
                logger.info("Breaking loop because: This was the retry");
                break;
            }
            newSK = (String) entries.get("newSk");
            if (newSK != null) {
                /* This is sometimes needed. */
                logger.info("Retrying because: Found newSk value");
                sk = newSK;
                continue;
            } else {
                logger.info("Breaking loop because: Failed to find newSk");
                break;
            }
        } while (true);
        if (Boolean.TRUE.equals(entries.get("error"))) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortextStreamingDownloadFailed);
        }
        final Map<String, Object> captchamap = (Map<String, Object>) entries.get("captcha");
        if (captchamap != null) {
            /* Dead end: Yandex SmartCaptcha which we cannot solve. https://cloud.yandex.com/en/services/smartcaptcha */
            // final Browser brcaptcha = br.cloneBrowser();
            // brcaptcha.getPage(captchamap.get("page").toString());
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                link.setComment(captchamap.get("page").toString());
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Streaming download impossible because of unsupported captcha type Yandex SmartCaptcha");
        }
        /* Find highest video quality */
        int bestHeight = -1;
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final List<Map<String, Object>> videos = (List<Map<String, Object>>) data.get("videos");
        String dllink = null;
        for (final Map<String, Object> video : videos) {
            final String dimension = video.get("dimension").toString();
            if (!dimension.matches("\\d+p")) {
                /* Skip unsupported values such as "adaptive". */
                continue;
            }
            final int heightTmp = Integer.parseInt(dimension.replace("p", ""));
            if (dllink == null || heightTmp > bestHeight) {
                bestHeight = heightTmp;
                dllink = video.get("url").toString();
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Picked stream: " + bestHeight + "p | Link: " + dllink);
        return dllink;
    }

    private boolean isHLS(final String url) {
        if (StringUtils.containsIgnoreCase(url, ".m3u8") || StringUtils.containsIgnoreCase(url, "/hls/")) {
            return true;
        } else {
            return false;
        }
    }

    private Map<String, Object> handleErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String error = (String) entries.get("error");
        if (error != null) {
            final String description = (String) entries.get("description");
            if (error.equalsIgnoreCase("DiskNotFoundError")) {
                /* {"message":"Не удалось найти запрошенный ресурс.","description":"Resource not found.","error":"DiskNotFoundError"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (error.equalsIgnoreCase("DiskResourceDownloadLimitExceededError")) {
                if (link != null) {
                    link.setProperty(PROPERTY_QUOTA_REACHED, true);
                }
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit: Wait or add account and retry", 1 * 60 * 60 * 1000l);
                } else {
                    /* This should never happen */
                    logger.warning("Single file quota reached although account is given");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, description, 5 * 60 * 1000l);
                }
            } else {
                /* Handle unknown errors */
                if (link == null) {
                    throw new AccountUnavailableException("Unknown error: " + description, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error: " + description);
                }
            }
        }
        return entries;
    }

    private void handleErrorsFree(final Browser br) throws PluginException {
        if (br.containsHTML("\"title\":\"invalid ckey\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid ckey'", 5 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":21")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 21 'bad formed path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":69")) {
            /* Usually this does not happen. Happens also if you actually try to download a "premiumonly" link via this method. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"code\":71")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 71 'Wrong path'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":77")) {
            /* Happens when we send a very wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 77 'resource not found'", 1 * 60 * 60 * 1000l);
        } else if (br.containsHTML("\"code\":88")) {
            /* Happens when we send a wrong hash - usually shouldn't happen! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 88 'Decryption error'", 10 * 60 * 1000l);
        }
    }

    /** Returns full hash which usually comes in this format: <hash>:/<path> */
    private String getRawHash(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_HASH);
    }

    private String getHashWithoutPath(final DownloadLink link) {
        return DiskYandexNetFolder.getHashWithoutPath(this.getRawHash(link));
    }

    /** Returns relative path of the file in relation to {@link #getHashWithoutPath(DownloadLink)}. */
    private String getPath(final DownloadLink link) {
        return DiskYandexNetFolder.getPathFromHash(this.getRawHash(link));
    }

    private String getUserID(final Account acc) {
        return acc.getStringProperty(PROPERTY_ACCOUNT_USERID);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            /* Always try to re-use cookies to avoid login captchas! */
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = account.loadUserCookies();
            /* 2021-02-15: Implemented cookie login for testing purposes */
            if (userCookies != null) {
                logger.info("Attempting user cookie login...");
                this.setCookies(br, userCookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                if (this.checkCookies(br, account)) {
                    logger.info("User cookie login successful");
                    /*
                     * Set username by cookie in an attempt to get a unique username because in theory user can enter whatever he wants when
                     * doing cookie-login!
                     */
                    final String usernameByCookie = br.getCookie(br.getURL(), "yandex_login", Cookies.NOTDELETEDPATTERN);
                    if (!StringUtils.isEmpty(usernameByCookie)) {
                        account.setUser(usernameByCookie);
                    }
                    return;
                } else {
                    logger.info("Cookie login failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            }
            if (cookies != null) {
                this.setCookies(br, cookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                if (this.checkCookies(br, account)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            /* Check if previous login attempt failed and cookie login is enforced. */
            if (account.getBooleanProperty(PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN, false)) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            try {
                logger.info("Performing full login");
                boolean isLoggedIN = false;
                boolean requiresCaptcha;
                final Browser ajaxBR = br.cloneBrowser();
                ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                ajaxBR.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getPage("https://passport.yandex.com/auth?from=cloud&origin=disk_landing_web_signin_ru&retpath=https%3A%2F%2Fdisk.yandex.com%2F%3Fsource%3Dlanding_web_signin&backpath=https%3A%2F%2Fdisk.yandex.com");
                for (int i = 0; i <= 4; i++) {
                    final Form[] forms = br.getForms();
                    if (forms.length == 0) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Form loginform = forms[0];
                    loginform.remove("twoweeks");
                    loginform.put("source", "password");
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("passwd", Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("\\&quot;captchaRequired\\&quot;:true")) {
                        /** TODO: 2018-08-10: Fix captcha support */
                        /* 2018-04-18: Only required after 10 bad login attempts or bad IP */
                        requiresCaptcha = true;
                        final String csrf_token = loginform.hasInputFieldByName("csrf_token") ? loginform.getInputField("csrf_token").getValue() : null;
                        final String idkey = loginform.hasInputFieldByName("idkey") ? loginform.getInputField("idkey").getValue() : null;
                        if (csrf_token == null || idkey == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        ajaxBR.postPage("/registration-validations/textcaptcha", "csrf_token=" + csrf_token + "&track_id=" + idkey);
                        final String url_captcha = PluginJSonUtils.getJson(ajaxBR, "image_url");
                        final String id = PluginJSonUtils.getJson(ajaxBR, "id");
                        if (StringUtils.isEmpty(url_captcha) || StringUtils.isEmpty(id)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "https://" + account.getHoster(), true);
                        final String c = getCaptchaCode(url_captcha, dummyLink);
                        loginform.put("captcha_answer", c);
                        // loginform.put("idkey", id);
                    } else {
                        requiresCaptcha = false;
                    }
                    br.submitForm(loginform);
                    isLoggedIN = br.getCookie(br.getURL(), "yandex_login", Cookies.NOTDELETEDPATTERN) != null;
                    if (!isLoggedIN) {
                        /* 2021-02-11: Small workaround/test */
                        isLoggedIN = br.getCookie("yandex.com", "yandex_login", Cookies.NOTDELETEDPATTERN) != null;
                    }
                    if (!requiresCaptcha) {
                        /* No captcha -> Only try login once! */
                        break;
                    } else if (requiresCaptcha && i > 0) {
                        /* Probably wrong password and we only allow one captcha attempt. */
                        break;
                    } else if (isLoggedIN) {
                        break;
                    }
                }
                if (!isLoggedIN) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final Throwable e) {
                logger.info("Normal login failed -> Enforcing cookie login");
                account.setProperty(PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN, true);
                /* Don't display dialog e.g. during linkcheck/download-attempt. */
                if (!account.hasEverBeenValid()) {
                    showCookieLoginInfo();
                }
                throw new AccountInvalidException("Normal login failed - try cookie login");
            }
        }
    }

    private void setCookies(final Browser br, final Cookies cookies) {
        for (final String domain : cookie_domains) {
            br.setCookies(domain, cookies);
        }
        br.setCookies("passport.yandex.com", cookies);
    }

    private boolean checkCookies(final Browser br, final Account account) throws IOException {
        br.getPage("https://id.yandex.com/");
        if (br.getURL().contains("passport.yandex.com") || br.getURL().contains("/auth")) {
            /* Redirect to login-page */
            return false;
        }
        logger.info("First check looks good, performing 2nd login check");
        try {
            br.getPage("https://mail.yandex.com/api/v2/serp/counters?silent");
            if (br.getRequest().getHtmlCode().startsWith("{")) {
                /* json response -> Looks like we're logged in */
                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* userid is needed for some account related http requests later on. */
        final String sessionIDCookie = br.getCookie(br.getHost(), "Session_id");
        String userID = null;
        if (sessionIDCookie != null) {
            /* 2023-01-24: I've abandoned the idea to extract that ID from the cookies. */
            // final String[] sessionIDCookieSeparatedStrings = sessionIDCookie.split("\\|");
            // if(sessionIDCookieSeparatedStrings != null && sessionIDCookieSeparatedStrings.length > 0) {
            // final String maybeUserID =
            // }
        }
        if (userID == null) {
            br.getPage("https://" + getCurrentDomain() + "/client/disk/");
            userID = br.getRegex("\"uid\":\"(\\d+)").getMatch(0);
        }
        logger.info("userID = " + userID);
        if (StringUtils.isEmpty(userID) || !userID.matches("\\d+")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty(PROPERTY_ACCOUNT_USERID, userID);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private boolean isFileDownloadQuotaReached(final DownloadLink dl) {
        return dl.getBooleanProperty(PROPERTY_QUOTA_REACHED, false);
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink link) throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(DiskYandexNet.NORESUME, true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private void moveFileToTrash(final Browser br, final DownloadLink link, final Account account, final String authSk) {
        final String filepath = getInternalFilePath(link, account);
        if (!StringUtils.isEmpty(filepath) && !StringUtils.isEmpty(authSk) && br != null && br.getRequest() != null) {
            logger.info("Trying to move file to trash: " + filepath);
            try {
                br.postPage("/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=" + Encoding.urlEncode(filepath) + "&idClient=" + CLIENT_ID + "&sk=" + authSk);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if (entries.containsKey("error")) {
                    logger.info("Possible failure on moving file into trash");
                } else {
                    logger.info("Successfully moved file into trash");
                    link.removeProperty(PROPERTY_PATH_INTERNAL);
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.warning("Failed to move file to trash - Exception!");
            }
        } else {
            logger.info("Cannot move any file to trash as there is no stored internal path or authSk available");
        }
    }

    private void saveInternalFilePath(final DownloadLink link, final Account account, final String filepath) {
        link.setProperty(PROPERTY_PATH_INTERNAL + ":" + JDHash.getMD5(account.getUser()), filepath);
    }

    /**
     * Returns URL-encoded internal path for download/move/file POST-requests. This is the path which a specific file has inside a users'
     * account e.g. after importing a public file into an account.
     */
    private String getInternalFilePath(final DownloadLink link, final Account account) {
        return link.getStringProperty(PROPERTY_PATH_INTERNAL + ":" + JDHash.getMD5(account.getUser()));
    }

    /** Deletes all items inside users' Yandex trash folder. */
    private void emptyTrash(final Browser br2, final String authSk) {
        if (!StringUtils.isEmpty(authSk) && br2 != null && br2.getRequest() != null) {
            try {
                logger.info("Trying to empty trash");
                br2.postPage("/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + authSk);
                logger.info("Successfully emptied trash");
            } catch (final Throwable e) {
                logger.warning("Failed to empty trash");
            }
        } else {
            logger.info("Cannot empty trash as authSk is null or no browser-request is available");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    @Override
    public String getDescription() {
        return "JDownloader's disk.yandex.net Plugin helps downloading files from disk.yandex.net. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_FILES_TO_ACCOUNT, "1. Move files to account before downloading them to get higher download speeds?").setDefaultValue(MOVE_FILES_TO_ACCOUNT_default);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD, "2. Delete moved files & empty trash after downloadlink-generation?").setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(DELETE_FROM_ACCOUNT_AFTER_DOWNLOAD_default));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}