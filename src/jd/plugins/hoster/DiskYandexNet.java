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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.text.TextDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
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
import jd.plugins.CryptedLink;
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
        this.enablePremium("https://passport.yandex.ru/registration?mode=register&from=cloud");
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "https://" + getCurrentDomain() + "/";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    private void prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.setCookie(getCurrentDomain(), "ys", "");
        br.setAllowedResponseCodes(new int[] { 429, 500 });
        br.setFollowRedirects(true);
    }

    /** Returns currently used domain. */
    public static String getCurrentDomain() {
        return "disk.yandex.com";
    }

    /* Settings values */
    private final String         MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT                                 = "move_quota_limited_files_to_account";
    private final String         DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD         = "delete_moved_file_from_account_after_quota_limited_download";
    private final String         EMPTY_TRASH_AFTER_QUOTA_LIMITED_DOWNLOAD                            = "empty_trash_after_quota_limited_download";
    private final boolean        MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT_default                         = true;
    private final boolean        DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD_default = true;
    private final boolean        EMPTY_TRASH_AFTER_DOWNLOAD_default                                  = false;
    /* Some constants which they used in browser */
    public static final String   CLIENT_ID                                                           = "12139679121706110849432";
    /* Domains & other login stuff */
    private final String[]       cookie_domains                                                      = new String[] { "https://yandex.ru", "https://yandex.com", "https://disk.yandex.ru/", "https://disk.yandex.com/", "https://disk.yandex.net/", "https://disk.yandex.com.tr/", "https://disk.yandex.kz/" };
    public static final String[] sk_domains                                                          = new String[] { "disk.yandex.com", "disk.yandex.ru", "disk.yandex.com.tr", "disk.yandex.ua", "disk.yandex.az", "disk.yandex.com.am", "disk.yandex.com.ge", "disk.yandex.co.il", "disk.yandex.kg", "disk.yandex.lt", "disk.yandex.lv", "disk.yandex.md", "disk.yandex.tj", "disk.yandex.tm", "disk.yandex.uz", "disk.yandex.fr", "disk.yandex.ee", "disk.yandex.kz", "disk.yandex.by" };
    /* Properties */
    public static final String   PROPERTY_HASH                                                       = "hash_main";
    public static final String   PROPERTY_QUOTA_REACHED                                              = "quoty_reached";
    public static final String   PROPERTY_CRAWLED_FILENAME                                           = "plain_filename";
    public static final String   PROPERTY_PATH_INTERNAL                                              = "path_internal";
    public static final String   PROPERTY_LAST_AUTH_SK                                               = "last_auth_sk";
    public static final String   PROPERTY_MEDIA_TYPE                                                 = "media_type";
    public static final String   PROPERTY_MIME_TYPE                                                  = "mine_type";
    public static final String   PROPERTY_PREVIEW_URL_ORIGINAL                                       = "preview_url_original";
    public static final String   PROPERTY_PREVIEW_URL_DEFAULT                                        = "preview_url_default";
    public static final String   PROPERTY_META_READ_ONLY                                             = "meta_read_only";
    public static final String   PROPERTY_PASSWORD_TOKEN                                             = "password_token";
    public static final String   PROPERTY_ACCOUNT_ENFORCE_COOKIE_LOGIN                               = "enforce_cookie_login";
    private final String         PROPERTY_ACCOUNT_USERID                                             = "account_userid";
    private static final String  PROPERTY_NORESUME                                                   = "NORESUME";
    public static final String   APIV1_BASE                                                          = "https://cloud-api.yandex.com/v1";
    /*
     * https://tech.yandex.com/disk/api/reference/public-docpage/ 2018-08-09: API(s) seem to work fine again - in case of failure, please
     * disable use_api_file_free_availablecheck ONLY!!
     */
    private static final boolean allow_use_api_file_free_availablecheck                              = true;
    private static final boolean allow_use_api_file_free_download                                    = true;

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (link.getBooleanProperty(DiskYandexNet.PROPERTY_NORESUME, false)) {
            return false;
        } else {
            return true;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
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
        final String fuid = link.getStringProperty(PROPERTY_HASH);
        if (fuid != null) {
            return this.getHost() + "://" + fuid;
        } else {
            return super.getLinkID(link);
        }
    }

    private boolean canUseAPIFreeAvailablecheck(final DownloadLink link) {
        if (!allow_use_api_file_free_availablecheck) {
            return false;
        } else if (link.hasProperty(PROPERTY_PASSWORD_TOKEN)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean canUseAPIFreeDownload(final DownloadLink link) {
        if (!allow_use_api_file_free_download) {
            return false;
        } else if (link.hasProperty(PROPERTY_PASSWORD_TOKEN)) {
            return false;
        } else {
            return true;
        }
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
        if (canUseAPIFreeAvailablecheck(link)) {
            return requestFileInformationAPI(link, account);
        } else {
            return requestFileInformationWebsite(link, account);
        }
    }

    /** Check availablestatus via public API. */
    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        final UrlQuery query = new UrlQuery();
        query.add("public_key", URLEncode.encodeURIComponent(this.getHashWithoutPath(link)));
        query.add("path", URLEncode.encodeURIComponent(this.getPath(link)));
        br.getPage(APIV1_BASE + "/disk/public/resources?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = this.checkErrorsWebAPI(br, link, account);
        /* No Exception --> No quota reached issue! */
        link.removeProperty(PROPERTY_QUOTA_REACHED);
        return parseInformationAPIAvailablecheckFiles(this, link, account, resp);
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        /* Remove that value to ensure that we will have a fresh one down below. */
        link.removeProperty(PROPERTY_LAST_AUTH_SK);
        final String contenturl = getMainLink(link);
        /**
         * Why do we need to use the crawler plugin here? </br>
         * - If the item is password protected and the 'passToken' cookie has expired, it will be refreshed </br>
         * - If the item was not password protected but it is password protected now, that will be handled correctly </br>
         * - If any single-file properties which only our crawler finds change, we will get them
         */
        final CryptedLink cryptedlink = new CryptedLink(contenturl, link);
        final DiskYandexNetFolder crawler = (DiskYandexNetFolder) this.getNewPluginForDecryptInstance(this.getHost());
        final ArrayList<DownloadLink> results = crawler.decryptIt(cryptedlink, account, false);
        DownloadLink fresh = null;
        for (final DownloadLink result : results) {
            if (StringUtils.equals(this.getLinkID(result), this.getLinkID(link))) {
                fresh = result;
                break;
            }
        }
        if (fresh == null) {
            /* Dead end */
            /* Check for other errors */
            checkErrorsWebsite(br, link, account);
            /* Dead end */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set fresh properties */
        link.setProperties(fresh.getProperties());
        final String crawledFilename = link.getStringProperty(PROPERTY_CRAWLED_FILENAME);
        String filename = br.getRegex("class=\"file-name\" data-reactid=\"[^\"]*?\">([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
        }
        String filesize_str = br.getRegex("class=\"item-details__name\">\\s*Size:\\s*</span> ([^<>\"]+)</div>").getMatch(0);
        if (filesize_str == null) {
            /* Language independent */
            filesize_str = br.getRegex("class=\"item-details__name\"[^>]*>[^<>\"]+</span> ([\\d\\.]+ (?:B|KB|MB|GB))</div>").getMatch(0);
        }
        /* Important for account download handling */
        if (br.containsHTML("class=\"[^\"]+antifile-sharing\"")) {
            link.setProperty(PROPERTY_QUOTA_REACHED, true);
        } else {
            link.removeProperty(PROPERTY_QUOTA_REACHED);
        }
        if (crawledFilename != null) {
            /* Fallback: Use cached value */
            link.setFinalFileName(crawledFilename);
        }
        if (filesize_str != null) {
            filesize_str = filesize_str.replace(",", ".");
            link.setDownloadSize(SizeFormatter.getSize(filesize_str));
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationAPIAvailablecheckFiles(final Plugin plugin, final DownloadLink link, final Account account, final Map<String, Object> entries) throws Exception {
        final String error = (String) entries.get("error");
        if (error != null) {
            return AvailableStatus.FALSE;
        }
        final String resource_id = (String) entries.get("resource_id");
        final Number filesize = (Number) entries.get("size");
        final String hash = (String) entries.get("public_key");
        final String filename = (String) entries.get("name");
        final String path = (String) entries.get("path");
        final String md5 = (String) entries.get("md5");
        final String sha256 = (String) entries.get("sha256");
        if (sha256 != null) {
            // only one hash is stored
            link.setSha256Hash(sha256);
        } else if (link.getHashInfo() == null && md5 != null) {
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
        link.setProperty(PROPERTY_MIME_TYPE, entries.get("mime_type"));
        final String lastModifiedDateStr = (String) entries.get("modified");
        if (lastModifiedDateStr != null) {
            final long lastModifiedTimestampMillis = TimeFormatter.getMilliSeconds(lastModifiedDateStr, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);
            link.setLastModifiedTimestamp(lastModifiedTimestampMillis);
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus parseInformationWebsiteAvailablecheckFiles(final Plugin plugin, final DownloadLink link, final Map<String, Object> entries) throws Exception {
        // final String resource_id = (String) entries.get("id");
        final Map<String, Object> meta = (Map<String, Object>) entries.get("meta");
        final long filesize = JavaScriptEngineFactory.toLong(meta.get("size"), -1);
        final String filename = (String) entries.get("name");
        // final String path = (String) entries.get("path");
        if (StringUtils.isEmpty(filename) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        link.setFinalFileName(filename);
        link.setProperty(PROPERTY_CRAWLED_FILENAME, filename);
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        if (meta != null) {
            if (Boolean.TRUE.equals(meta.get("read_only"))) {
                link.setProperty(PROPERTY_META_READ_ONLY, true);
            } else {
                link.removeProperty(PROPERTY_META_READ_ONLY);
            }
            link.setProperty(PROPERTY_MIME_TYPE, meta.get("mimetype"));
            /* 2024-08-15: Internal fileID is not always the files' sha256 hash so do not set this here!! */
            /* Internal fileID == sha256 hash */
            // final String file_id = (String) meta.get("file_id");
            // if (file_id != null && file_id.matches("[a-f0-9]{64}")) {
            // link.setSha256Hash(file_id);
            // }
        }
        /* 2024-08-14: Yes, they got a typo in this API field lol */
        Boolean videoPlayerAvailability = (Boolean) entries.get("isAvialableForVideoPlayer");
        if (videoPlayerAvailability == null) {
            videoPlayerAvailability = (Boolean) entries.get("isAvailableForVideoPlayer");
        }
        if (Boolean.TRUE.equals(videoPlayerAvailability) && !link.hasProperty(PROPERTY_MEDIA_TYPE)) {
            /* We know that this file is a video item. */
            link.setProperty(PROPERTY_MEDIA_TYPE, "VIDEO");
        }
        final Number lastModifiedTimestamp = (Number) entries.get("modified");
        if (lastModifiedTimestamp != null) {
            link.setLastModifiedTimestamp(lastModifiedTimestamp.longValue() * 1000l);
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

    private boolean isReadOnlyFile(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_META_READ_ONLY)) {
            return true;
        } else {
            return false;
        }
    }

    /** Typically .txt/.pdf */
    private boolean isDocument(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_MEDIA_TYPE), "document")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isImage(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_MEDIA_TYPE), "image")) {
            return true;
        } else {
            return false;
        }
    }

    /** Typically .mp4 */
    private boolean isVideo(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_MEDIA_TYPE), "VIDEO")) {
            return true;
        } else {
            return false;
        }
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (account != null) {
            this.login(account, false);
        }
        final String passToken = link.getStringProperty(PROPERTY_PASSWORD_TOKEN);
        if (passToken != null) {
            DiskYandexNetFolder.setFolderPasswordTokenCookie(br, Browser.getHost(getMainLink(link)), passToken);
        }
        Map<String, Object> directurlresultmap = getDirecturlResult(link, account);
        PluginException exceptionDuringAPILinkcheck = null;
        if (directurlresultmap == null && canUseAPIFreeAvailablecheck(link)) {
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
            /* Re-use cached directurl */
            dllink = directurlresultmap.get("url").toString();
            logger.info("Re-using stored url: " + dllink);
            if ((Boolean) directurlresultmap.get("original") == Boolean.FALSE) {
                /* Remove HashInfo as we might be downloading a non-original file now. */
                link.setHashInfo(null);
                link.setVerifiedFileSize(-1);
            }
        } else {
            /* More complicated else path: We need to generate a fresh directurl */
            this.requestFileInformationWebsite(link, account);
            if (this.isReadOnlyFile(link)) {
                this.downloadReadonlyFile(br.cloneBrowser(), link, account);
                return;
            } else if (account != null) {
                dllink = generateFreshDirecturlAccountMode(link, account);
            } else {
                /* Download without account or at least without "Move file into account" handling. */
                if (exceptionDuringAPILinkcheck != null) {
                    throw exceptionDuringAPILinkcheck;
                }
                if (this.canUseAPIFreeDownload(link)) {
                    /**
                     * Download API:
                     *
                     * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=public_key&path=/
                     */
                    /* Free API download. */
                    br.getPage(APIV1_BASE + "/disk/public/resources/download?public_key=" + URLEncode.encodeURIComponent(getHashWithoutPath(link)) + "&path=" + URLEncode.encodeURIComponent(this.getPath(link)));
                    final Map<String, Object> entries = this.checkErrorsWebAPI(br, link, account);
                    dllink = (String) entries.get("href");
                } else {
                    /* Website download */
                    final String sk = link.getStringProperty(PROPERTY_LAST_AUTH_SK);
                    if (StringUtils.isEmpty(sk)) {
                        logger.warning("sk in website download handling is null");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    this.prepWebapiBrowser(br, link);
                    br.postPageRaw("/public-api-desktop/download-url", String.format("{\"hash\":\"%s\",\"sk\":\"%s\"}", getRawHash(link), sk));
                    checkErrorsWebsite(br, link, account);
                    // TODO: 2023-11-03: Update this and use json parser
                    dllink = PluginJSonUtils.getJsonValue(br, "url");
                    if (StringUtils.isEmpty(dllink)) {
                        logger.warning("Failed to find final downloadurl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    /* Don't do htmldecode because the link will become invalid then */
                    /* sure json will return url with htmlentities? */
                    dllink = HTMLEntities.unhtmlentities(dllink);
                }
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            freshDirecturlHasJustBeenGenerated = true;
        }
        if (isHLS(dllink)) {
            // TODO: Remove this, it shouldn't be needed here anymore
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
        } else {
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 416) {
                        logger.info("Resume impossible, disabling it for the next try");
                        link.setChunksProgress(null);
                        link.setProperty(DiskYandexNet.PROPERTY_NORESUME, true);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else {
                        /* This will most likely happen for 0 byte filesize files / serverside broken files. */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
                    }
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
        }
        dl.startDownload();
    }

    /** Ugly helper function which returns map of current cached direct downloadlink information. */
    private Map<String, Object> getDirecturlResult(final DownloadLink link, final Account account) {
        final String directurlproperty = getDirecturlProperty(account);
        String storedPreviouslyGeneratedDirecturl = link.getStringProperty(directurlproperty);
        String directurl = storedPreviouslyGeneratedDirecturl;
        boolean isOriginalFile = true;
        if (directurl == null && this.isImage(link)) {
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

    /** Use this before doing requests on "/client/...". */
    private Browser prepClientapiBrowser(final Browser sourceBrowser) throws Exception {
        final Browser br2 = sourceBrowser.cloneBrowser();
        br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br2.getHeaders().put("Origin", "https://" + br.getHost(true));
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return br2;
    }

    /** Use this before doing requests on "/public/api/...". */
    private void prepWebapiBrowser(final Browser br3, final DownloadLink link) throws Exception {
        final String host;
        if (br3.getRequest() != null) {
            host = br3.getHost(true);
        } else {
            /* Fallback */
            host = getCurrentDomain();
        }
        br3.getHeaders().put("Accept", "*/*");
        br3.getHeaders().put("Content-Type", "text/plain"); // Important header else we will get error 400
        br3.getHeaders().put("Origin", "https://" + host);
        br3.getHeaders().put("Referer", this.getMainLink(link));
        br3.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br3.getHeaders().put("X-Retpath-Y", this.getMainLink(link));
    }

    private String generateFreshDirecturlAccountMode(final DownloadLink link, final Account account) throws Exception {
        this.login(account, false);
        final String userID = getUserID(account);
        final String authSk = PluginJSonUtils.getJson(this.br, "authSk");
        if (StringUtils.isEmpty(authSk) || userID == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_LAST_AUTH_SK, authSk);
        String dllink = null;
        final boolean fileIsDownloadQuotaLimitReached = isFileDownloadQuotaReached(link);
        /* For requests to "/public/api" */
        final Browser br3 = br.cloneBrowser();
        prepWebapiBrowser(br3, link);
        if (fileIsDownloadQuotaLimitReached) {
            /* File is quota limited -> We can only download it straight away by moving it into users' account. */
            if (!getPluginConfig().getBooleanProperty(MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT, MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT_default)) {
                /* User does not allow us to move file to account in order to get around this limit */
                if (account == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit: Add account to be able to download it or try again later", 1 * 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit: Enable auto move to account or wait and retry later", 1 * 60 * 60 * 1000l);
                }
            }
            /* Move file into users' account as it is quote limited and without doing this it's impossible to download this file. */
            /* Obtain special token */
            br.getPage("/client/disk/Downloads");
            checkErrorsWebsite(br, link, account);
            final String longSK = br.getRegex("\"sk\":\"([^\"]+)").getMatch(0);
            if (longSK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser br2 = prepClientapiBrowser(br);
            String internal_file_path = getInternalFilePath(link, account);
            boolean foundInternalPath = false;
            if (internal_file_path != null) {
                logger.info("Checking if stored internal path/file still exists on account | file-path: " + internal_file_path);
                try {
                    final UrlQuery query = new UrlQuery();
                    query.add("idClient", Encoding.urlEncode(CLIENT_ID));
                    query.add("sk", Encoding.urlEncode(longSK));
                    query.add("_model.0", "resources");
                    query.add("sort.0", "mtime");
                    query.add("order.0", "0");
                    query.add("idContext.0", Encoding.urlEncode("/disk/Downloads"));
                    query.add("amount.0", "40");
                    query.add("offset.0", "0");
                    query.add("withParent.0", "1");
                    br2.postPage("/models/?_m=resources", query);
                    checkErrorsWebsite(br2, link, account);
                    final Map<String, Object> entries = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                    final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "models/{0}/data/resources");
                    for (final Map<String, Object> fileObject : resourcelist) {
                        if (fileObject.get("path").toString().equals(internal_file_path)) {
                            foundInternalPath = true;
                            break;
                        }
                    }
                    if (foundInternalPath) {
                        logger.info("Re-using stored internal file-path: " + internal_file_path);
                    } else {
                        /* Remove stored path so we will not try again with this one. */
                        logger.info("Failed to find stored internal filepath: " + internal_file_path);
                        this.saveInternalFilePath(link, account, null);
                        internal_file_path = null;
                    }
                } catch (final Exception e) {
                    logger.log(e);
                    logger.warning("Failed to find existing file via given internal file path - Exception!");
                }
            }
            if (!foundInternalPath) {
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
                final Map<String, Object> postmap = new HashMap<String, Object>();
                postmap.put("hash", this.getRawHash(link));
                postmap.put("name", link.getName());
                postmap.put("lang", "en");
                postmap.put("source", copySource);
                postmap.put("isAlbum", false);
                postmap.put("itemId", null);
                postmap.put("sk", authSk);
                postmap.put("uid", userID);
                prepWebapiBrowser(br3, link);
                br3.postPageRaw("/public/api/save", URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postmap)));
                checkErrorsWebsite(br3, link, account);
                final Map<String, Object> entries = this.checkErrorsWebAPI(br3, link, account);
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                internal_file_path = data.get("path").toString();
                final String oid = data.get("oid").toString();
                if (StringUtils.isEmpty(internal_file_path)) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to move file into Yandex account");
                } else if (StringUtils.isEmpty(oid)) {
                    /* Should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to move file into Yandex account: Failed to find oid");
                }
                /* Save internal path for later. */
                this.saveInternalFilePath(link, account, internal_file_path);
                /* Sometimes the process of 'moving' a file into a cloud account can take some seconds. */
                logger.info("transfer-status: checking");
                boolean fileWasImportedSuccessfully = false;
                Map<String, Object> fileImportResp = null;
                Map<String, Object> fileImportRespData = null;
                final Map<String, Object> postMap = new HashMap<String, Object>();
                postMap.put("oid", oid);
                postMap.put("lang", "en");
                postMap.put("sk", authSk);
                postMap.put("uid", userID);
                for (int i = 1; i < 10; i++) {
                    prepWebapiBrowser(br3, link);
                    br3.postPageRaw("/public/api/get-save-operation-status", URLEncode.encodeURIComponent(JSonStorage.serializeToJson(postMap)));
                    checkErrorsWebsite(br3, link, account);
                    fileImportResp = this.checkErrorsWebAPI(br3, link, account);
                    fileImportRespData = (Map<String, Object>) fileImportResp.get("data");
                    final String copyState = fileImportRespData.get("state").toString();
                    logger.info("Copy state loop: " + i + " | Serverside copy state: " + copyState);
                    if (copyState.equalsIgnoreCase("COMPLETED")) {
                        fileWasImportedSuccessfully = true;
                        break;
                    } else if (copyState.equalsIgnoreCase("FAILED")) {
                        logger.info("Possibly failed to copy file to account");
                        break;
                    } else if (this.isAbort()) {
                        /* Aborted by user */
                        throw new InterruptedException();
                    } else {
                        sleep(i * 1000l, link);
                    }
                }
                if (!fileWasImportedSuccessfully) {
                    /* This can happen e.g. if users' account does not have any free space left. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to move file to account " + account.getUser());
                }
            } else {
                /* File has already been copied to this account before */
                logger.info("given/stored internal filepath: " + internal_file_path);
            }
            /* Generate downloadurl to copy of file which is now owned by the account we are using at this moment. */
            final UrlQuery query = new UrlQuery();
            query.add("idClient", Encoding.urlEncode(CLIENT_ID));
            query.add("sk", Encoding.urlEncode(longSK));
            query.add("_model.0", "do-get-resource-url");
            query.add("idResource.0", Encoding.urlEncode(internal_file_path));
            br2.postPage("/models/?_m=do-get-resource-url", query);
            checkErrorsWebsite(br2, link, account);
            final Map<String, Object> entries = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> downloadMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "models/{0}/data");
            dllink = downloadMap.get("file").toString();
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Downloadlink creation was successful -> Now check for other tasks */
            if (getPluginConfig().getBooleanProperty(DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD, DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD_default)) {
                /*
                 * We have a direct downloadable link to that file which will work even after deleting it so let's move it into the trash
                 * right now [before even downloading].
                 */
                logger.info("Trying to move previously copied file to trash: " + internal_file_path);
                try {
                    br2.postPage("/models/?_m=do-resource-delete", "_model.0=do-resource-delete&id.0=" + Encoding.urlEncode(internal_file_path) + "&idClient=" + CLIENT_ID + "&sk=" + authSk);
                    final Map<String, Object> resp = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                    final Object errorO = resp.get("error");
                    if (errorO != null) {
                        logger.info("Possible failure on moving file into trash: " + errorO);
                    } else {
                        try {
                            /* Access trash bin and look for the file we've just deleted */
                            int waitedSeconds = 0;
                            final int maxWaitSeconds = 10;
                            String pathToFileInTrash = null;
                            do {
                                /* Wait some seconds until file was moved to trash */
                                this.sleep(1000, link);
                                waitedSeconds++;
                                logger.info("Successfully moved file into trash --> Looking for deleted file in trash | Seconds waited: " + waitedSeconds + "/" + maxWaitSeconds);
                                final Pattern patternPathToFileInTrash = Pattern.compile("/trash/" + Pattern.quote(link.getName()) + "_[a-f0-9]{40}");
                                final UrlQuery queryTrash = new UrlQuery();
                                queryTrash.add("idClient", Encoding.urlEncode(CLIENT_ID));
                                queryTrash.add("sk", Encoding.urlEncode(longSK));
                                queryTrash.add("_model.0", "resources");
                                queryTrash.add("sort.0", "append_time");
                                queryTrash.add("order.0", "0");
                                queryTrash.add("idContext.0", Encoding.urlEncode("/trash"));
                                queryTrash.add("amount.0", "40");
                                queryTrash.add("offset.0", "0");
                                queryTrash.add("withParent.0", "1");
                                br2.postPage("/models/?_m=resources", queryTrash);
                                final Map<String, Object> entriesTrash = restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                                final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entriesTrash, "models/{0}/data/resources");
                                for (final Map<String, Object> fileObject : resourcelist) {
                                    final String thisPath = fileObject.get("path").toString();
                                    if (new Regex(thisPath, patternPathToFileInTrash).patternFind()) {
                                        pathToFileInTrash = thisPath;
                                        break;
                                    }
                                }
                                if (pathToFileInTrash != null) {
                                    break;
                                } else if (waitedSeconds >= maxWaitSeconds) {
                                    logger.info("Giving up - failed to find target-file in trash after " + maxWaitSeconds + " seconds");
                                    break;
                                } else {
                                    /* Continue */
                                    logger.info("Failed to find file in trash");
                                }
                            } while (true);
                            if (pathToFileInTrash != null) {
                                /* Permanently delete file from trash which we've moved to trash before */
                                logger.info("Found file in trash -> Permanently deleting file from trash: " + pathToFileInTrash);
                                try {
                                    final String postData = "{\"sk\":\"" + longSK + "\",\"connection_id\":\"" + CLIENT_ID + "\",\"apiMethod\":\"mpfs/bulk-async-delete\",\"requestParams\":{\"operations\":[{\"src\":\"" + pathToFileInTrash + "\"}]}}";
                                    br2.postPageRaw("/models-v2?m=mpfs/bulk-async-delete", postData);
                                    logger.info("Successfully deleted file from trash");
                                } catch (final Exception e) {
                                    logger.log(e);
                                    logger.warning("Failed to permanently delete file from trash - Exception!");
                                }
                            } else {
                                logger.warning("Failed to find file in trash. Either it hasn't been moved to trash yet or it has already been deleted from trash.");
                            }
                        } catch (final Exception e) {
                            logger.log(e);
                            logger.warning("Failed to find deleted file in trash - Exception!");
                        }
                    }
                } catch (final Exception e) {
                    logger.log(e);
                    logger.warning("Failed to move file to trash - Exception!");
                }
                /*
                 * Remove internal path property so next time the user is downloading this file we will not unnecessarily check if that file
                 * still exists.
                 */
                link.removeProperty(PROPERTY_PATH_INTERNAL);
                if (getPluginConfig().getBooleanProperty(EMPTY_TRASH_AFTER_QUOTA_LIMITED_DOWNLOAD, EMPTY_TRASH_AFTER_DOWNLOAD_default)) {
                    /**
                     * Empty trash is user wants this. </br>
                     * This is not a necessary step but can be useful when downloading a lot of quota limited files so that files which we
                     * failed to delete from trash will be deleted as well.
                     */
                    try {
                        logger.info("Trying to empty trash");
                        br2.postPage("/models/?_m=do-clean-trash", "_model.0=do-clean-trash&idClient=" + CLIENT_ID + "&sk=" + authSk);
                        logger.info("Successfully emptied trash");
                    } catch (final Throwable e) {
                        logger.warning("Failed to empty trash");
                    }
                }
            }
            return dllink;
        } else {
            /* Normal download */
            logger.info("Performing normal account download handling");
            final String postdata = String.format("{\"hash\":\"%s\",\"sk\":\"%s\",\"uid\":\"%s\",\"options\":{\"hasExperimentVideoWithoutPreview\":true}}", this.getRawHash(link), authSk, userID);
            prepWebapiBrowser(br3, link);
            br3.postPageRaw("/public/api/download-url", URLEncode.encodeURIComponent(postdata));
            checkErrorsWebsite(br3, link, account);
            final Map<String, Object> entries = this.checkErrorsWebAPI(br3, link, account);
            final Object errorO = entries.get("error");
            if (errorO == null || Boolean.TRUE.equals(errorO)) {
                return null;
            }
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
                link.setProperty(PROPERTY_META_READ_ONLY, true);
            }
        }
        return null;
    }

    /**
     * Downloads items which are officially not downloadable. </br>
     * Only call this if you believe that official download of this file is possible. </br>
     * This is pretty much only working when user is logged in. In most of all other cases, their bot protection kicks in and blocks this
     * handling.
     */
    private void downloadReadonlyFile(final Browser br, final DownloadLink link, final Account account) throws Exception {
        logger.info("Attempting to download read-only file");
        if (!this.isReadOnlyFile(link)) {
            logger.warning("!DEV! Only call this function for read-only files!!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2023-07-15: For video files which can officially only be streamed and not downloaded. */
        if (isVideo(link)) {
            /* Video stream download */
            logger.info("Trying to find video stream downloadlink");
            final String passToken = link.getStringProperty(PROPERTY_PASSWORD_TOKEN);
            this.prepWebapiBrowser(br, link);
            /* For "WrongSK" handling */
            br.setAllowedResponseCodes(400);
            String errortextStreamingDownloadFailed = "Streaming download failed.";
            if (account == null) {
                errortextStreamingDownloadFailed += " Add account and try again.";
            }
            String sk = link.getStringProperty(PROPERTY_LAST_AUTH_SK);
            if (sk == null) {
                /* This is not nice but it will work. */
                sk = generateRandomSK();
            }
            Map<String, Object> entries = null;
            int attempts = 0;
            do {
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("hash", this.getRawHash(link));
                postData.put("sk", sk);
                if (passToken != null) {
                    postData.put("passToken", passToken);
                }
                final PostRequest request = br.createJSonPostRequest("https://" + getCurrentDomain() + "/public/api/get-video-streams", postData);
                request.setContentType("text/plain");
                br.getPage(request);
                checkErrorsWebsite(br, link, account);
                entries = this.checkErrorsWebAPI(br, link, account);
                final String skNew = (String) entries.get("newSk");
                if (StringUtils.isEmpty(skNew)) {
                    logger.info("Breaking loop because: Failed to find newSk");
                    break;
                } else if (attempts > 0) {
                    logger.info("Breaking loop because: This was the 2nd try");
                    break;
                } else if (this.isAbort()) {
                    /* Aborted by user */
                    throw new InterruptedException();
                }
                /* This is sometimes needed. */
                logger.info("Retrying because: Found newSk value");
                sk = skNew;
                attempts++;
                continue;
            } while (true);
            /* Find highest video quality */
            int bestHeight = -1;
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) data.get("videos");
            if (videos == null || videos.isEmpty()) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FATAL, errortextStreamingDownloadFailed);
            }
            logger.info("Picked stream: " + bestHeight + "p | Link: " + dllink);
            /* We will download a non-original file so the hash and/or verified filesize we know can be incorrect. */
            link.setHashInfo(null);
            link.setVerifiedFileSize(-1);
            /* sk value may have changed */
            link.setProperty(PROPERTY_LAST_AUTH_SK, sk);
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            /* Document download */
            /*
             * 2024-08-15: For now we only allow download of plaintext documents since the content of those can be obtained via one request.
             */
            final HashSet<String> allowedContentTypes = new HashSet<String>();
            allowedContentTypes.add("text/plain");
            final String cachedContentType = link.getStringProperty(PROPERTY_MIME_TYPE);
            if (cachedContentType != null && !allowedContentTypes.contains(cachedContentType)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download read-only documents of content-type: " + cachedContentType);
            }
            final String rawHash = this.getRawHash(link);
            final UrlQuery query = new UrlQuery();
            query.add("url", "ya-disk-public%3A%2F%2F" + URLEncode.encodeURIComponent(rawHash));
            query.add("name", URLEncode.encodeURIComponent(link.getName()));
            final String docViewURL = "https://docviewer.yandex.com/?" + query.toString();
            br.getPage(docViewURL);
            this.checkErrorsWebsite(br, link, account);
            /* If the user is logged in, the numbers inside that URL == userID of currently authorized account. */
            final String redirect = br.getRegex("(/view/\\d+/[^'\"]+)").getMatch(0);
            if (redirect != null) {
                br.getPage(redirect);
                this.checkErrorsWebsite(br, link, account);
            }
            final String json = br.getRegex("<script type=application/json id=store-prefetch>(.*?)</script>").getMatch(0);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> doc = (Map<String, Object>) entries.get("doc");
            final String doc_contentType = doc.get("contentType").toString();
            if (StringUtils.isEmpty(doc_contentType)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download read-only documents of unknown content-type");
            } else if (!allowedContentTypes.contains(doc_contentType)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download read-only documents of content-type: " + doc_contentType);
            }
            final List<Map<String, Object>> pages = (List<Map<String, Object>>) doc.get("pages");
            if (pages == null || pages.isEmpty()) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Read-only document has zero pages?");
            }
            final StringBuilder sb = new StringBuilder();
            for (final Map<String, Object> page : pages) {
                /* This index starts from 1 */
                final int index = ((Number) page.get("index")).intValue();
                final String state = page.get("state").toString();
                if (!state.equalsIgnoreCase("READY")) {
                    /* This should never happen. */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download read-only document because page " + index + " is not ready");
                }
                final String html = page.get("html").toString();
                if (sb.length() > 0) {
                    /* Add line break */
                    sb.append("\r\n");
                }
                /* Try to extract plaintext from HTML */
                final String[] plaintexts = new Regex(html, "<div class=\"b1\"><p class=\"mg\\d+\">([^<]+)</p></div>").getColumn(0);
                if (plaintexts != null && plaintexts.length > 0) {
                    for (final String plaintext : plaintexts) {
                        if (sb.length() > 0) {
                            /* Add line break */
                            sb.append("\r\n");
                        }
                        sb.append(plaintext);
                    }
                } else {
                    sb.append(html);
                }
            }
            /* We will download a non-original file so the hash and/or verified filesize we know can be incorrect. */
            link.setHashInfo(null);
            link.setVerifiedFileSize(-1);
            /* Write text to file */
            dl = new TextDownloader(this, link, sb.toString());
            dl.startDownload();
        }
    }

    private static String generateRandomSK() {
        return "u" + generateRandomString("0123456789abcdef", 32);
    }

    public static String generateRandomString(final String chars, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean isHLS(final String url) {
        if (StringUtils.containsIgnoreCase(url, ".m3u8") || StringUtils.containsIgnoreCase(url, "/hls/")) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Object> checkErrorsWebAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException e) {
            /* Check for html based errors */
            this.checkErrorsWebsite(br, link, account);
            /* Dead end */
            final String errortext = "Invalid API response";
            if (link == null) {
                throw new AccountUnavailableException(errortext, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext);
            }
        }
        return checkErrorsWebAPI(entries, link, account);
    }

    public Map<String, Object> checkErrorsWebAPI(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException {
        final Map<String, Object> captchamap = (Map<String, Object>) entries.get("captcha");
        if (captchamap != null) {
            /* Dead end: Yandex SmartCaptcha which we cannot solve. https://cloud.yandex.com/en/services/smartcaptcha */
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && link != null) {
                final String captchaPage = captchamap.get("captcha-page").toString();
                if (captchaPage != null) {
                    link.setComment(captchaPage);
                }
            }
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Download impossible because of unsupported captcha type 'Yandex SmartCaptcha'");
        }
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final Object errorO;
        if (data != null) {
            /* No error */
            errorO = data.get("error");
        } else {
            errorO = entries.get("error");
        }
        if (errorO == null) {
            /* No error */
            return entries;
        }
        String errormessage = null;
        if (errorO instanceof Map) {
            final Map<String, Object> errormap = (Map<String, Object>) errorO;
            errormessage = errormap.get("errormessage").toString();
        } else if (errorO instanceof String) {
            errormessage = errorO.toString();
        }
        if (errormessage == null) {
            /* No error */
            return entries;
        }
        /**
         * Description of fields: </br>
         * message: error message in currently selected language </br>
         * description: Description of error in English </br>
         * error: Unique error string e.g. "DiskNotFoundError"
         */
        if (errormessage.equalsIgnoreCase("DiskNotFoundError")) {
            /* {"message":"Не удалось найти запрошенный ресурс.","description":"Resource not found.","error":"DiskNotFoundError"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errormessage.equalsIgnoreCase("DiskResourceDownloadLimitExceededError")) {
            if (link != null) {
                link.setProperty(PROPERTY_QUOTA_REACHED, true);
            }
            if (account == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit! Try again later or add account.", 1 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File has reached quota limit! Try again later.", 5 * 60 * 1000l);
            }
        } else if (errormessage.equalsIgnoreCase("NoFreeSpaceCopyToDisk")) {
            throw new AccountUnavailableException("Not enough free space available to move quota limited file " + link.getName() + " to account", 5 * 60 * 1000l);
        } else if (errormessage.equalsIgnoreCase("DiskSymlinkTokenExpiredError")) {
            /* Password token (cookie) expired. */
            link.removeProperty(PROPERTY_PASSWORD_TOKEN);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password token (cookie) expired", 5 * 60 * 1000l);
        } else {
            if (link == null) {
                throw new AccountUnavailableException("Unknown error: " + errormessage, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown error: " + errormessage);
            }
        }
    }

    public void checkErrorsWebsite(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        // TODO: Refactor this (use json parser)
        if (StringUtils.containsIgnoreCase(br.getURL(), "/showcaptcha")) {
            final String msg = "Rate limit reached";
            final long waitMillis = 5 * 60 * 1000;
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, msg, waitMillis);
            } else {
                throw new AccountUnavailableException(msg, waitMillis);
            }
        } else if (br.containsHTML("\"title\":\"invalid ckey\"")) {
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

    private String getUserID(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_USERID);
    }

    public void login(final Account account, final boolean force) throws Exception {
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
                    final String usernameFromCookie = br.getCookie(br.getURL(), "yandex_login", Cookies.NOTDELETEDPATTERN);
                    if (!StringUtils.isEmpty(usernameFromCookie)) {
                        account.setUser(usernameFromCookie);
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
            this.prepBR(br);
            logger.info("Performing full login");
            try {
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
            } catch (final Exception e) {
                logger.info("Normal login failed -> Enforcing cookie login for next login attempt for this account");
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
        final HashSet<String> blacklistedCookies = new HashSet<String>();
        blacklistedCookies.add("passToken");
        /**
         * Explanation for blacklisted cookie values: </br>
         * passToken: Token which authorizes user to access password protected folder. While providing it via cookie login will technically
         * work, the information which folder it is for, is not given. Setting it may cause disruption in folder crawler handling thus we
         * ignore it here.
         */
        for (final Cookie cookie : cookies.getCookies()) {
            if (cookie.getValue() == null) {
                continue;
            } else if (blacklistedCookies.contains(cookie.getKey())) {
                logger.info("Skipping blacklisted cookie: " + cookie.getKey());
                continue;
            }
            /* Set cookie on all known domains. */
            for (final String domain : cookie_domains) {
                br.setCookie(domain, cookie.getKey(), cookie.getValue());
            }
            br.setCookie("passport.yandex.com", cookie.getKey(), cookie.getValue());
        }
    }

    /** Returns true if given cookies are valid. */
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
        } catch (final Exception ignore) {
            logger.log(ignore);
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        /* userid is needed for some account related http requests later on. */
        br.getPage("https://" + getCurrentDomain() + "/client/disk/");
        this.checkErrorsWebsite(br, getDownloadLink(), account);
        final String userID = br.getRegex("\"uid\":\"(\\d+)").getMatch(0);
        logger.info("userID = " + userID);
        if (userID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        account.setProperty(PROPERTY_ACCOUNT_USERID, userID);
        account.setType(AccountType.FREE);
        final AccountInfo ai = new AccountInfo();
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private boolean isFileDownloadQuotaReached(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_QUOTA_REACHED, false);
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    @Override
    public String getDescription() {
        return "JDownloader's disk.yandex.com Plugin helps downloading files from disk.yandex.com. It provides some settings for downloads via account.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Account settings:"));
        final ConfigEntry moveFilesToAcc = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT, "<html>Account mode: Move </b>quota limited</b> files to account before downloading them to get higher download speeds?</html>").setDefaultValue(MOVE_QUOTA_LIMITED_FILES_TO_ACCOUNT_default);
        getConfig().addEntry(moveFilesToAcc);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD, "Account mode: Delete moved files after downloadlink-generation?").setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(DELETE_MOVED_FILE_FROM_ACCOUNT_AFTER_QUOTA_LIMITED_DOWNLOAD_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EMPTY_TRASH_AFTER_QUOTA_LIMITED_DOWNLOAD, "Account mode: Empty trash after each quota limited download?").setEnabledCondidtion(moveFilesToAcc, true).setDefaultValue(EMPTY_TRASH_AFTER_DOWNLOAD_default));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link == null) {
            return;
        }
        link.removeProperty(DiskYandexNet.PROPERTY_NORESUME);
    }
}