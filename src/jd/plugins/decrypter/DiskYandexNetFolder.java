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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DiskYandexNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DiskYandexNetFolder extends PluginForDecrypt {
    public DiskYandexNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    // public static List<String> getPluginSubDomains() {
    // final ArrayList<String> subdomains = new ArrayList<String>();
    // subdomains.add("disk");
    // subdomains.add("mail");
    // subdomains.add("docviewer");
    // return subdomains;
    // }
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "disk.yandex.net", "yandex.net", "yandex.com", "yandex.com.tr", "yandex.ru", "yandex.ua", "yadi.sk", "yadisk.cc", "yandex.kz" });
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
            // final String annotationName = domains[0];
            String pattern = "https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/((?:disk/)?public/?(\\?hash=.+|#.+)|";
            pattern += "(?:d|i)/[A-Za-z0-9\\-_]+(/[^/]+){0,}|";
            pattern += "mail/\\?hash=.+|";
            pattern += "\\?url=ya\\-disk\\-public%3A%2F%2F.+";
            pattern += ")";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    /** Usually docviewer.yandex.xy but we're supporting so many domains and subdomains that a generic RegEx works better. */
    private static final Pattern type_docviewer    = Pattern.compile("https?://[^/]+/\\?url=ya\\-disk\\-public%3A%2F%2F([^/\"\\&]+).*?", Pattern.CASE_INSENSITIVE);
    private final String         type_primaryURLs  = "(?i).+?public/?(\\?hash=.+|#.+)";
    private final String         type_shortURLs_d  = "(?i)https?://[^/]+/d/[A-Za-z0-9\\-_]+((/[^/]+){0,})";
    private final String         type_shortURLs_i  = "(?i)https?://[^/]+/i/[A-Za-z0-9\\-_]+";
    private final String         type_yadi_sk_mail = "(?i)https?://[^/]+/mail/\\?hash=.+";
    private static final String  JSON_TYPE_DIR     = "dir";
    private DiskYandexNet        hosterplugin      = null;

    /** Using API: https://tech.yandex.ru/disk/api/reference/public-docpage/ */
    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return decryptIt(param, account, true);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final Account account, final boolean allowPagination) throws Exception {
        br.setFollowRedirects(true);
        String contenturl = param.getCryptedUrl();
        /* Do some URL corrections */
        final Regex docviewer = new Regex(contenturl, type_docviewer);
        if (docviewer.patternFind()) {
            /* Documents in web view mode --> Change them to normal file-URLs! */
            final UrlQuery query = UrlQuery.parse(contenturl);
            final String param_url = query.get("url");
            final String hash = param_url.replace("ya-disk-public%3A%2F%2F", "");
            final String hashRoot = URLDecoder.decode(hash, "UTF-8");
            contenturl = generateContentURL(hashRoot);
        }
        /* Load hosterplugin to use same browser headers/settings. */
        hosterplugin = (DiskYandexNet) this.getNewPluginForHostInstance(this.getHost());
        /* Login whenever possible. Helps us get around anti bot captchas. */
        if (account != null) {
            hosterplugin.login(account, false);
        }
        /**
         * 2021-02-09: New: Prefer website if we don't know whether we got a file or a folder! API will fail in case it is a single file &&
         * is currently quota-limited!
         */
        /*
         * Set password from previous CryptedLink e.g. password has been obtained when root folder was added so now we know it for all
         * subfolders.
         */
        String passToken = null;
        final DownloadLink parent = param.getDownloadLink();
        if (parent != null) {
            /*
             * 2024-08-13: psp: Password protected links cannot be processed via API in JDownloader [yet]. Reason: I was unable to find out
             * which parameter the password needs to be sent as.
             */
            passToken = parent.getStringProperty(DiskYandexNet.PROPERTY_PASSWORD_TOKEN);
        }
        if (StringUtils.isEmpty(this.getAdoptedCloudFolderStructure()) || StringUtils.isEmpty(getHashFromURL(contenturl)) || passToken != null) {
            logger.info("Using website crawler because we cannot know whether we got a single file- or a folder");
            return this.crawlFilesFoldersWebsite(param, contenturl, allowPagination);
        } else {
            logger.info("Using API crawler");
            return this.crawlFilesFoldersAPI(param, contenturl, allowPagination);
        }
    }

    private ArrayList<DownloadLink> crawlFilesFoldersWebsite(final CryptedLink param, final String contenturl, final boolean allowPagination) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        /*
         * Set password from previous CryptedLink e.g. password has been obtained when root folder was added so now we know it for all
         * subfolders.
         */
        String passCode = null;
        String passToken = null;
        final DownloadLink parent = param.getDownloadLink();
        if (parent != null) {
            passToken = parent.getStringProperty(DiskYandexNet.PROPERTY_PASSWORD_TOKEN);
            passCode = parent.getDownloadPassword();
            if (passToken != null) {
                /*
                 * If that token is still valid, it will grant us access to password protected folders without the need to send the password
                 * again for each subfolder crawl process.
                 */
                setFolderPasswordTokenCookie(br, Browser.getHost(contenturl), passToken);
            }
        }
        /**
         * 2021-02-12: If a user adds an URL leading to only one file but this file is part of a folder, this crawler will crawl everything
         * from the root folder on. This is because the file could e.g. be at the end of a paginated folder -> Browser will do pagination
         * until file is found but this takes too much time and effort for us so we'll just add everything. The user can then sort/find that
         * file in the LinkGrabber.
         */
        Map<String, Object> json_root = null;
        Map<String, Object> resources_map = null;
        String sk = null;
        boolean passwordSuccess = false;
        folderPasswordVerificationLoop: do {
            br.getPage(contenturl);
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.hosterplugin.checkErrorsWebsite(br, parent, null);
            sk = DiskYandexNet.getSK(this.br);
            final String json = br.getRegex("<script type=\"application/json\"[^>]*id=\"store-prefetch\"[^>]*>(.*?)</script>").getMatch(0);
            if (json == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            json_root = restoreFromString(json, TypeRef.MAP);
            resources_map = (Map<String, Object>) json_root.get("resources");
            final Map<String, Object> password_protected_map = (Map<String, Object>) resources_map.get("password-protected");
            if (password_protected_map == null) {
                /* No password needed or password has already been entered. */
                break folderPasswordVerificationLoop;
            } else if (passwordSuccess) {
                /* Correct password has already been entered but is needed again here -> Something went really wrong */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Password protected folder */
            Map<String, Object> pwresp = null;
            pwloop: for (int i = 0; i <= 5; i++) {
                if (StringUtils.isEmpty(passCode) || i > 0) {
                    /* Ask user for password */
                    passCode = getUserInput("Password?", param);
                }
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("hash", password_protected_map.get("hash"));
                postData.put("password", passCode);
                postData.put("sk", sk);
                final PostRequest request = br.createJSonPostRequest("/public/api/check-password", postData);
                prepareJsonRequest(request, br);
                br.getPage(request);
                pwresp = hosterplugin.checkErrorsWebAPI(br, parent, null);
                final Object errorO = pwresp.get("error");
                if (errorO == null || Boolean.FALSE.equals(errorO)) {
                    logger.info("Pwloop: " + i + " | Correct password: " + passCode);
                    passwordSuccess = true;
                    break pwloop;
                } else {
                    /* {"error":true,"statusCode":403,"code":"HTTP_403","data":{"code":309,"title":"Symlink invalid password"}} */
                    logger.info("Pwloop: " + i + " | Incorrect password: " + passCode);
                    continue pwloop;
                }
            }
            if (!passwordSuccess) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            passToken = pwresp.get("token").toString();
            setFolderPasswordTokenCookie(br, br.getHost(), passToken);
            /* Continue so page gets reloaded so we can get the "json without folder-password-prompt". */
            logger.info("Performing page reload after successful password handling");
            continue;
        } while (true);
        final String authSk = PluginJSonUtils.getJson(this.br, "authSk");
        // final String rootResourceId = (String) map.get("rootResourceId");
        final String currentResourceId = (String) json_root.get("currentResourceId");
        /*
         * First find the base folder name: If there are multiple items as part of a folder, the first item is kind of a dummy item
         * containing the name of the root folder.
         */
        String hashMain = null;
        for (final String key : resources_map.keySet()) {
            final Map<String, Object> ressource = (Map<String, Object>) resources_map.get(key);
            final String type = (String) ressource.get("type");
            if ("dir".equals(type) && ressource.get("parent") == null) {
                hashMain = (String) ressource.get("hash");
                break;
            }
        }
        String baseFolderName = null;
        for (final String key : resources_map.keySet()) {
            final Map<String, Object> ressource = (Map<String, Object>) resources_map.get(key);
            final String type = (String) ressource.get("type");
            final String id = (String) ressource.get("id");
            if ("dir".equals(type) && StringUtils.equals(id, currentResourceId)) {
                baseFolderName = (String) ressource.get("name");
                break;
            }
        }
        if (StringUtils.isEmpty(relativeDownloadPath) && baseFolderName != null) {
            /* First time crawl of a possible folder structure -> Define root dir name */
            relativeDownloadPath = baseFolderName;
        }
        FilePackage fp = null;
        if (relativeDownloadPath != null) {
            fp = FilePackage.getInstance();
            fp.setName(relativeDownloadPath);
        }
        List<Object> ressources_list = new ArrayList<Object>();
        /* First collect map items in list because API will return list later. */
        for (final String key : resources_map.keySet()) {
            ressources_list.add(resources_map.get(key));
        }
        final int maxItemsPerPage = 40;
        int page = 1;
        int offset = 0;
        pagination: do {
            logger.info("Crawling page: " + page);
            boolean completed = false;
            for (final Object ressourceO : ressources_list) {
                final Map<String, Object> ressource = (Map<String, Object>) ressourceO;
                final String type = (String) ressource.get("type");
                final String name = (String) ressource.get("name");
                String hash = (String) ressource.get("hash");
                final String path = (String) ressource.get("path");
                final DownloadLink link;
                if (type.equalsIgnoreCase("dir")) {
                    /* Folder */
                    final List<Object> children = (List<Object>) ressource.get("children");
                    if ((children != null && !children.isEmpty()) || path == null || StringUtils.equals(path, hashMain + ":")) {
                        /* Skip dummy entries - also do not increase our offset value! */
                        continue;
                    }
                    /* Subfolders go back into our crawler! Path contains "<hash_long_decoded>:/path" */
                    final String folderlink = this.generateContentURL(path);
                    link = createDownloadlink(folderlink);
                    if (relativeDownloadPath != null) {
                        link.setRelativeDownloadFolderPath(relativeDownloadPath + "/" + name);
                    }
                } else {
                    /* File */
                    // final boolean antiFileSharing = ((Boolean) ressourceMeta.get("antiFileSharing")).booleanValue();
                    final String resource_id = (String) ressource.get("id");
                    if (StringUtils.isEmpty(hash) && !StringUtils.isEmpty(path) && path.contains(":/")) {
                        /* Small workaround */
                        hash = path.split(":/")[0];
                    }
                    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(hash) || StringUtils.isEmpty(resource_id)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    link = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    parseFilePropertiesWebsite(link, ressource);
                    /*
                     * We want the user to have an URL which he can open via browser and it does not only open up the root of the folder but
                     * the exact file he wants to have!
                     */
                    final String urlContent;
                    if (!StringUtils.isEmpty(path)) {
                        /* Path contains hash and path! */
                        urlContent = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(path);
                    } else {
                        urlContent = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(hash);
                    }
                    link.setProperty("mainlink", urlContent);
                    link.setContentUrl(urlContent);
                    if (fp != null) {
                        link._setFilePackage(fp);
                    }
                    if (relativeDownloadPath != null) {
                        link.setRelativeDownloadFolderPath(relativeDownloadPath);
                    }
                }
                if (!StringUtils.isEmpty(path)) {
                    /* Path contains hash + path */
                    link.setProperty(DiskYandexNet.PROPERTY_HASH, path);
                } else {
                    /* Hash only */
                    link.setProperty(DiskYandexNet.PROPERTY_HASH, hash);
                }
                if (passCode != null && passToken != null) {
                    link.setPasswordProtected(true);
                    link.setDownloadPassword(passCode);
                    link.setProperty(DiskYandexNet.PROPERTY_PASSWORD_TOKEN, passToken);
                }
                if (authSk != null) {
                    link.setProperty(DiskYandexNet.PROPERTY_LAST_AUTH_SK, authSk);
                }
                ret.add(link);
                distribute(link);
                offset += 1;
            }
            /* Determine if we've reached the end / crawled all items. */
            if (page > 1) {
                completed = Boolean.TRUE.equals(resources_map.get("completed"));
            } else {
                if (!completed) {
                    completed = ressources_list.size() < maxItemsPerPage;
                }
            }
            logger.info("Crawled page: " + page + " | Found items so far: " + ret.size());
            if (completed) {
                logger.info("Stopping because: Reached last page");
                break pagination;
            } else if (StringUtils.isEmpty(sk)) {
                /* This should never happen */
                logger.warning("Pagination failure: sk missing");
                break pagination;
            } else if (StringUtils.isEmpty(hashMain)) {
                /* This should never happen */
                logger.warning("Pagination failure: hashMain missing");
                break pagination;
            } else if (!allowPagination) {
                logger.info("Stopping because: Pagination is not allowed");
                break pagination;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break pagination;
            }
            /* Continue to next page */
            final PostRequest request = br.createPostRequest("/public/api/fetch-list", (UrlQuery) null, null);
            prepareJsonRequest(request, br);
            request.setPostDataString("%7B%22hash%22%3A%22" + Encoding.urlEncode(hashMain) + "%3A%22%2C%22offset%22%3A" + offset + "%2C%22withSizes%22%3Atrue%2C%22sk%22%3A%22" + sk + "%22%2C%22options%22%3A%7B%22hasExperimentVideoWithoutPreview%22%3Atrue%7D%7D");
            br.getPage(request);
            resources_map = hosterplugin.checkErrorsWebAPI(br, parent, null);
            ressources_list = (List<Object>) resources_map.get("resources");
            if (ressources_list == null) {
                /* This should never happen */
                logger.warning("Pagination failure: ressources missing");
                break pagination;
            }
            page += 1;
            continue pagination;
        } while (!this.isAbort());
        return ret;
    }

    public static void setFolderPasswordTokenCookie(final Browser br, final String domain, final String token) {
        br.setCookie(domain, DiskYandexNet.COOKIE_KEY_PASSWORD_TOKEN, token);
    }

    private void prepareJsonRequest(final PostRequest request, final Browser originbr) {
        request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // request.getHeaders().put("X-Retpath-Y", param.getCryptedUrl());
        request.getHeaders().put("Accept", "*/*");
        request.getHeaders().put("Origin", "https://" + originbr._getURL().getHost());
        request.setContentType("text/plain");
    }

    private ArrayList<DownloadLink> crawlFilesFoldersAPI(final CryptedLink param, final String contenturl, final boolean allowPagination) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        String hashWithPath;
        if (contenturl.matches(type_yadi_sk_mail)) {
            hashWithPath = getHashFromURL(contenturl);
        } else if (contenturl.matches(type_shortURLs_d) || contenturl.matches(type_shortURLs_i)) {
            br.getPage(contenturl);
            this.hosterplugin.checkErrorsWebsite(br, null, null);
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            hashWithPath = PluginJSonUtils.getJsonValue(br, "hash");
            if (hashWithPath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            hashWithPath = getHashFromURL(contenturl);
        }
        if (hashWithPath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String hashWithoutPath = getHashWithoutPath(hashWithPath);
        String internalPath = null;
        if (contenturl.matches(type_shortURLs_d)) {
            internalPath = new Regex(contenturl, type_shortURLs_d).getMatch(1);
            internalPath = URLDecoder.decode(internalPath, "UTF-8");
            /* Remove parameter(s) */
            if (internalPath.contains("?")) {
                internalPath = internalPath.substring(0, internalPath.indexOf("?"));
            }
            /* No path given from previous crawler actions -> That's the best we can get. */
            if (StringUtils.isEmpty(relativeDownloadPath)) {
                relativeDownloadPath = internalPath;
            }
        } else {
            internalPath = getPathFromHash(hashWithPath);
        }
        if (internalPath == null) {
            /* No path given? Crawl everything starting from root. */
            internalPath = "/";
        }
        this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int offset = 0;
        final short entries_per_request = 200;
        int totalNumberofEntries = 0;
        final FilePackage fp = FilePackage.getInstance();
        final UrlQuery query = new UrlQuery();
        query.add("limit", Integer.toString(entries_per_request));
        query.add("public_key", URLEncode.encodeURIComponent(hashWithoutPath));
        query.add("path", URLEncode.encodeURIComponent(internalPath));
        pagination: do {
            query.addAndReplace("offset", Integer.toString(offset));
            br.getPage(DiskYandexNet.APIV1_BASE + "/disk/public/resources?" + query.toString());
            Map<String, Object> entries = this.hosterplugin.checkErrorsWebAPI(br, null, null);
            /*
             * 2021-01-19:
             * {"message":"Не удалось найти запрошенный ресурс.","description":"Resource not found.","error":"DiskNotFoundError"}
             */
            if (entries.containsKey("error")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String type_main = (String) entries.get("type");
            if (!type_main.equals(JSON_TYPE_DIR)) {
                /* We only have a single file --> Add to downloadliste / host plugin */
                final DownloadLink link = parseSingleFileAPI(entries);
                if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                    link.setRelativeDownloadFolderPath(relativeDownloadPath);
                }
                link._setFilePackage(fp);
                ret.add(link);
                return ret;
            }
            final String walk_string = "_embedded/items";
            final List<Map<String, Object>> resource_data_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, walk_string);
            if (offset == 0) {
                /* Set total number of entries on first loop. */
                final Map<String, Object> itemInfo = (Map<String, Object>) entries.get("_embedded");
                String baseFolderName = (String) entries.get("name");
                if (StringUtils.isEmpty(baseFolderName)) {
                    /* Fallback */
                    baseFolderName = hashWithPath;
                }
                totalNumberofEntries = ((Number) itemInfo.get("total")).intValue();
                if (StringUtils.isEmpty(relativeDownloadPath)) {
                    /* First time crawl of a possible folder structure -> Define root dir name */
                    relativeDownloadPath = baseFolderName;
                }
                fp.setName(relativeDownloadPath);
                if (totalNumberofEntries == 0) {
                    if (!StringUtils.isEmpty(relativeDownloadPath)) {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, hashWithoutPath + "_" + relativeDownloadPath);
                    } else {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, hashWithoutPath);
                    }
                }
            }
            for (final Map<String, Object> resource_data : resource_data_list) {
                final String type = (String) resource_data.get("type");
                final String hash = (String) resource_data.get("public_key");
                final String path = (String) resource_data.get("path");
                final String name = (String) resource_data.get("name");
                if (StringUtils.isEmpty(type_main) || StringUtils.isEmpty(path) || StringUtils.isEmpty(name)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink link;
                if (type.equals(JSON_TYPE_DIR)) {
                    /* Folder */
                    final String folderlink = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(hash + ":" + path);
                    link = createDownloadlink(folderlink);
                    if (!StringUtils.isEmpty(relativeDownloadPath)) {
                        link.setRelativeDownloadFolderPath(relativeDownloadPath + "/" + name);
                    }
                } else {
                    /* File */
                    link = parseSingleFileAPI(resource_data);
                    if (!StringUtils.isEmpty(relativeDownloadPath)) {
                        link.setRelativeDownloadFolderPath(relativeDownloadPath);
                    }
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
                offset++;
            }
            if (resource_data_list.size() < entries_per_request) {
                /* Fail safe */
                logger.info("Stopping because current page contains less items than max items per page --> Should be the last page");
                break pagination;
            } else if (offset >= totalNumberofEntries) {
                logger.info("Stopping because: Reached end");
                break pagination;
            } else if (!allowPagination) {
                logger.info("Stopping because: Pagination is not allowed");
                break pagination;
            }
        } while (!this.isAbort());
        return ret;
    }

    private DownloadLink parseSingleFileAPI(final Map<String, Object> entries) throws Exception {
        final String hash = (String) entries.get("public_key");
        final String path = (String) entries.get("path");
        final String name = (String) entries.get("name");
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(path) || StringUtils.isEmpty(hash)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink link = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
        parseFilePropertiesAPI(link, entries);
        final String hashFull = hash + ":" + path;
        final String urlContent = generateContentURL(hashFull);
        /**
         * 2021-02-12: Keep it simple: Do not use different contentURLs for documents so they can be viewed in context of the folder they're
         * in via browser!
         */
        // String urlUser;
        // if ("document".equalsIgnoreCase((String) entries.get("media_type"))) {
        // /*
        // * Set contentURL which links to a comfortable web-view of documents whenever it makes sense.
        // */
        // urlUser = "https://docviewer.yandex.com/?url=ya-disk-public%3A%2F%2F" + URLEncode.encodeURIComponent(hashFull);
        // } else {
        // /*
        // * No fancy content URL available - set main URL.
        // */
        // urlUser = urlContent;
        // }
        link.setProperty("mainlink", generateContentURL(hashFull));
        link.setContentUrl(urlContent);
        return link;
    }

    private String generateContentURL(final String hashWithPath) {
        return "https://disk.yandex.com/public/?hash=" + URLEncode.encodeURIComponent(hashWithPath);
    }

    public static String getPathFromHash(final String hash) {
        if (hash.matches(".+:/.+")) {
            return hash.substring(hash.indexOf(":/") + 1, hash.length());
        } else {
            return "/";
        }
    }

    public static String getHashWithoutPath(final String hash) {
        if (hash.matches(".+:/.*")) {
            return hash.substring(0, hash.indexOf(":/"));
        } else {
            return hash;
        }
    }

    public static String regExJSON(final Browser br) {
        return br.getRegex("<script id=\"models\\-client\" type=\"application/json\">(.*?)</script>").getMatch(0);
    }

    public static Browser prepBrAlbum(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        return br;
    }

    public static boolean isOfflineWebsite(final Browser br) {
        if (br.containsHTML("class=\"not\\-found\\-public__caption\"|class=\"error__icon error__icon_blocked\"|_file\\-blocked\"|A complaint was received regarding this file|>\\s*File blocked\\s*<")) {
            return true;
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            return true;
        } else {
            return false;
        }
    }

    private String getHashFromURL(final String url) throws UnsupportedEncodingException, MalformedURLException {
        final String ret = UrlQuery.parse(url).get("hash");
        if (ret != null) {
            return URLDecoder.decode(ret, "UTF-8");
        } else {
            return null;
        }
    }

    private void parseFilePropertiesAPI(final DownloadLink link, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = DiskYandexNet.parseInformationAPIAvailablecheckFiles(this, link, null, entries);
        link.setAvailableStatus(status);
    }

    private void parseFilePropertiesWebsite(final DownloadLink link, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = DiskYandexNet.parseInformationWebsiteAvailablecheckFiles(this, link, entries);
        link.setAvailableStatus(status);
    }
}
