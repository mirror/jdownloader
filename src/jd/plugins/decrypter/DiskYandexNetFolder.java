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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
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
        ret.add(new String[] { "yandex.net", "yandex.com", "yandex.com.tr", "yandex.ru", "yandex.ua", "yadi.sk", "yadisk.cc", "yandex.kz" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return new String[] { "disk.yandex.net" };
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
    private static final String type_docviewer    = "https?://[^/]+/\\?url=ya\\-disk\\-public%3A%2F%2F([^/\"\\&]+).*?";
    private final String        type_primaryURLs  = ".+?public/?(\\?hash=.+|#.+)";
    private final String        type_shortURLs_d  = "https?://[^/]+/d/[A-Za-z0-9\\-_]+((/[^/]+){0,})";
    private final String        type_shortURLs_i  = "https?://[^/]+/i/[A-Za-z0-9\\-_]+";
    private final String        type_yadi_sk_mail = "https?://[^/]+/mail/\\?hash=.+";
    private static final String JSON_TYPE_DIR     = "dir";

    /** Using API: https://tech.yandex.ru/disk/api/reference/public-docpage/ */
    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        /* Load hosterplugin to use same browser headers/settings. */
        final DiskYandexNet hosterplugin = (DiskYandexNet) this.getNewPluginForHostInstance(this.getHost());
        final String parameter = param.getCryptedUrl();
        /* Do some URL corrections */
        if (param.getCryptedUrl().matches(type_docviewer)) {
            /* Documents in web view mode --> File-URLs! */
            /* First lets fix broken URLs by removing unneeded parameters ... */
            String tmp = param.getCryptedUrl();
            final String remove = new Regex(tmp, "(\\&[a-z0-9]+=.+)").getMatch(0);
            if (remove != null) {
                tmp = tmp.replace(remove, "");
            }
            String hash = new Regex(tmp, type_docviewer).getMatch(0);
            if (StringUtils.isEmpty(hash)) {
                hash = new Regex(tmp, "url=ya\\-disk\\-public%3A%2F%2F(.+)").getMatch(0);
                if (StringUtils.isEmpty(hash)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final String hashRoot = URLDecoder.decode(hash, "UTF-8");
            param.setCryptedUrl(generateContentURL(hashRoot));
        }
        /**
         * 2021-02-09: New: Prefer website if we do now know whether we got a file or a folder! API will fail in case it is a single file &&
         * is currently quota-limited!
         */
        if (StringUtils.isEmpty(this.getAdoptedCloudFolderStructure()) || StringUtils.isEmpty(getHashFromURL(parameter))) {
            logger.info("Using website crawler because we cannot know whether we got a single file- or a folder");
            return this.crawlFilesFoldersWebsite(param);
        } else {
            logger.info("Using API crawler");
            return this.crawlFilesFoldersAPI(param);
        }
    }

    private ArrayList<DownloadLink> crawlFilesFoldersWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        /**
         * 2021-02-12: If a user adds an URL leading to only one file but this file is part of a folder, this crawler will crawl everything
         * from the root folder on. This is because the fiule could e.g. be at the end of a paginated folder -> Browser will do pagination
         * until file is found but this takes too much time and effort for us so we'll just add everything. The user can then sort/find that
         * file in the LinkGrabber.
         */
        br.getPage(param.getCryptedUrl());
        if (isOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sk = DiskYandexNet.getSK(this.br);
        final String json = br.getRegex("<script type=\"application/json\"[^>]*id=\"store-prefetch\"[^>]*>(.*?)</script>").getMatch(0);
        Map<String, Object> map = restoreFromString(json, TypeRef.MAP);
        // final String rootResourceId = (String) map.get("rootResourceId");
        final String currentResourceId = (String) map.get("currentResourceId");
        Map<String, Object> entries = (Map<String, Object>) map.get("resources");
        /*
         * First find the base folder name: If there are multiple items as part of a folder, the first item is kind of a dummy item
         * containing the name of the root folder.
         */
        String hashMain = null;
        for (final String key : entries.keySet()) {
            final Map<String, Object> ressource = (Map<String, Object>) entries.get(key);
            final String type = (String) ressource.get("type");
            if ("dir".equals(type) && ressource.get("parent") == null) {
                hashMain = (String) ressource.get("hash");
                break;
            }
        }
        String baseFolderName = null;
        for (final String key : entries.keySet()) {
            final Map<String, Object> ressource = (Map<String, Object>) entries.get(key);
            final String type = (String) ressource.get("type");
            final String id = (String) ressource.get("id");
            if ("dir".equals(type) && StringUtils.equals(id, currentResourceId)) {
                baseFolderName = (String) ressource.get("name");
                break;
            }
        }
        if (StringUtils.isEmpty(baseFolderName)) {
            /* Fallback */
            baseFolderName = "unknown";
        }
        if (StringUtils.isEmpty(relativeDownloadPath)) {
            /* First time crawl of a possible folder structure -> Define root dir name */
            relativeDownloadPath = baseFolderName;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(baseFolderName);
        List<Object> ressources = new ArrayList<Object>();
        /* First collect map items in list because API will return list later! */
        for (final String key : entries.keySet()) {
            ressources.add(entries.get(key));
        }
        final int maxItemsPerPage = 40;
        int page = 0;
        int offset = 0;
        do {
            logger.info("Crawling page: " + (page + 1));
            boolean completed = false;
            for (final Object ressourceO : ressources) {
                final Map<String, Object> ressource = (Map<String, Object>) ressourceO;
                final String type = (String) ressource.get("type");
                final String name = (String) ressource.get("name");
                String hash = (String) ressource.get("hash");
                final String path = (String) ressource.get("path");
                if (type.equalsIgnoreCase("dir")) {
                    final List<Object> children = (List<Object>) ressource.get("children");
                    if ((children != null && !children.isEmpty()) || path == null || StringUtils.equals(path, hashMain + ":")) {
                        /* Skip dummy entries - also do not increase out offset value! */
                        continue;
                    }
                    offset += 1;
                    /* Subfolders go back into our decrypter! Path contains "<hash_long_decoded>:/path" */
                    final String folderlink = this.generateContentURL(path);
                    final DownloadLink dl = createDownloadlink(folderlink);
                    dl.setRelativeDownloadFolderPath(relativeDownloadPath + "/" + name);
                    decryptedLinks.add(dl);
                    distribute(dl);
                } else {
                    offset += 1;
                    // final Map<String, Object> ressourceMeta = (Map<String, Object>) ressource.get("meta");
                    // final boolean antiFileSharing = ((Boolean) ressourceMeta.get("antiFileSharing")).booleanValue();
                    final String resource_id = (String) ressource.get("id");
                    if (StringUtils.isEmpty(hash) && !StringUtils.isEmpty(path) && path.contains(":/")) {
                        /* Small workaround */
                        hash = path.split(":/")[0];
                    }
                    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(hash) || StringUtils.isEmpty(resource_id)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    parseFilePropertiesWebsite(dl, ressource);
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
                    if (!StringUtils.isEmpty(path)) {
                        /* Path contains hash + path */
                        dl.setProperty(DiskYandexNet.PROPERTY_HASH, path);
                    } else {
                        /* Hash only */
                        dl.setProperty(DiskYandexNet.PROPERTY_HASH, hash);
                    }
                    dl.setProperty("mainlink", urlContent);
                    dl.setContentUrl(urlContent);
                    dl.setProperty(DiskYandexNet.PROPERTY_INTERNAL_FUID, resource_id);
                    if (ressources.size() > 1) {
                        if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                            dl.setRelativeDownloadFolderPath(relativeDownloadPath);
                        }
                        dl._setFilePackage(fp);
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
            if (page > 0) {
                completed = Boolean.TRUE.equals(entries.get("completed"));
            } else {
                if (!completed) {
                    completed = ressources.size() < maxItemsPerPage;
                }
            }
            if (completed) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (StringUtils.isEmpty(sk)) {
                /* This should never happen */
                logger.warning("Pagination failure: sk missing");
                break;
            } else if (StringUtils.isEmpty(hashMain)) {
                /* This should never happen */
                logger.warning("Pagination failure: hashMain missing");
                break;
            }
            // final Map<String, Object> paginationMap = new HashMap<String, Object>();
            // final Map<String, Object> paginationOptions = new HashMap<String, Object>();
            // paginationOptions.put("hasExperimentVideoWithoutPreview", true);
            // paginationMap.put("hash", hash_long_decoded);
            // paginationMap.put("offset", offset);
            // paginationMap.put("withSizes", true);
            // paginationMap.put("sk", sk);
            // paginationMap.put("options", paginationOptions);
            PostRequest request = br.createPostRequest("/public/api/fetch-list", (UrlQuery) null, null);
            request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            // request.getHeaders().put("X-Retpath-Y", param.getCryptedUrl());
            request.getHeaders().put("Accept", "*/*");
            request.getHeaders().put("Origin", "https://" + br._getURL().getHost());
            request.setContentType("text/plain");
            request.setPostDataString("%7B%22hash%22%3A%22" + Encoding.urlEncode(hashMain) + "%3A%22%2C%22offset%22%3A" + offset + "%2C%22withSizes%22%3Atrue%2C%22sk%22%3A%22" + sk + "%22%2C%22options%22%3A%7B%22hasExperimentVideoWithoutPreview%22%3Atrue%7D%7D");
            br.getPage(request);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            ressources = (List<Object>) entries.get("resources");
            if (ressources == null) {
                /* This should never happen */
                logger.warning("Pagination failure: ressources missing");
                break;
            }
            page += 1;
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlFilesFoldersAPI(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        String hashWithPath;
        if (param.getCryptedUrl().matches(type_yadi_sk_mail)) {
            hashWithPath = getHashFromURL(param.getCryptedUrl());
        } else if (param.getCryptedUrl().matches(type_shortURLs_d) || param.getCryptedUrl().matches(type_shortURLs_i)) {
            getPage(param.getCryptedUrl());
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            hashWithPath = PluginJSonUtils.getJsonValue(br, "hash");
            if (hashWithPath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            hashWithPath = getHashFromURL(param.getCryptedUrl());
        }
        if (hashWithPath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String hashWithoutPath = getHashWithoutPath(hashWithPath);
        String internalPath = null;
        if (param.getCryptedUrl().matches(type_shortURLs_d)) {
            internalPath = new Regex(param.getCryptedUrl(), type_shortURLs_d).getMatch(1);
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
        short offset = 0;
        final short entries_per_request = 200;
        int totalNumberofEntries = 0;
        final FilePackage fp = FilePackage.getInstance();
        do {
            getPage("https://cloud-api.yandex.net/v1/disk/public/resources?limit=" + entries_per_request + "&offset=" + offset + "&public_key=" + URLEncode.encodeURIComponent(hashWithoutPath) + "&path=" + URLEncode.encodeURIComponent(internalPath));
            Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
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
                final DownloadLink dl = parseSingleFileAPI(entries);
                if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                    dl.setRelativeDownloadFolderPath(relativeDownloadPath);
                }
                dl._setFilePackage(fp);
                ret.add(dl);
                return ret;
            }
            final String walk_string = "_embedded/items";
            final List<Object> resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, walk_string);
            if (offset == 0) {
                /* Set total number of entries on first loop. */
                final Map<String, Object> itemInfo = (Map<String, Object>) entries.get("_embedded");
                String baseFolderName = (String) entries.get("name");
                if (StringUtils.isEmpty(baseFolderName)) {
                    /* Fallback */
                    baseFolderName = hashWithPath;
                }
                totalNumberofEntries = ((Number) itemInfo.get("total")).intValue();
                fp.setName(baseFolderName);
                if (StringUtils.isEmpty(relativeDownloadPath)) {
                    /* First time crawl of a possible folder structure -> Define root dir name */
                    relativeDownloadPath = baseFolderName;
                }
                if (totalNumberofEntries == 0) {
                    if (!StringUtils.isEmpty(relativeDownloadPath)) {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, hashWithoutPath + "_" + relativeDownloadPath);
                    } else {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, hashWithoutPath);
                    }
                }
            }
            for (final Object list_object : resource_data_list) {
                entries = (Map<String, Object>) list_object;
                final String type = (String) entries.get("type");
                final String hash = (String) entries.get("public_key");
                final String path = (String) entries.get("path");
                final String name = (String) entries.get("name");
                if (StringUtils.isEmpty(type_main) || StringUtils.isEmpty(path) || StringUtils.isEmpty(name)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (type.equals(JSON_TYPE_DIR)) {
                    /* Subfolders go back into our decrypter! */
                    final String folderlink = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(hash + ":" + path);
                    final DownloadLink dl = createDownloadlink(folderlink);
                    dl.setRelativeDownloadFolderPath(relativeDownloadPath + "/" + name);
                    ret.add(dl);
                } else {
                    final DownloadLink dl = parseSingleFileAPI(entries);
                    if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                        dl.setRelativeDownloadFolderPath(relativeDownloadPath);
                    }
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                }
                offset++;
            }
            if (resource_data_list.size() < entries_per_request) {
                /* Fail safe */
                logger.info("Stopping because current page contains less items than max. items allowed --> Should be the last page");
                break;
            } else if (offset >= totalNumberofEntries) {
                logger.info("Stopping because: Reached end");
                break;
            }
        } while (!this.isAbort());
        return ret;
    }

    private DownloadLink parseSingleFileAPI(final Map<String, Object> entries) throws Exception {
        final String hash = (String) entries.get("public_key");
        final String path = (String) entries.get("path");
        final String name = (String) entries.get("name");
        final String resource_id = (String) entries.get("resource_id");
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(path) || StringUtils.isEmpty(hash) || StringUtils.isEmpty(resource_id)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
        parseFilePropertiesAPI(dl, entries);
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
        dl.setProperty("mainlink", generateContentURL(hashFull));
        dl.setContentUrl(urlContent);
        dl.setProperty(DiskYandexNet.PROPERTY_INTERNAL_FUID, resource_id);
        return dl;
    }

    private String generateContentURL(final String hash) {
        return "https://disk.yandex.com/public/?hash=" + URLEncode.encodeURIComponent(hash);
    }

    public static String getPathFromHash(final String hash) {
        if (hash.matches(".+:/.+")) {
            return hash.substring(hash.indexOf(":/") + 1, hash.length());
        } else {
            return "/";
        }
    }

    public static String getHashWithoutPath(final String hash) {
        if (hash.matches(".+:/.+")) {
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

    public static Map<String, Object> findModel(final List<Object> modelObjects, final String targetModelName) {
        Map<String, Object> entries = null;
        boolean foundResourceModel = false;
        for (final Object modelo : modelObjects) {
            entries = (Map<String, Object>) modelo;
            final String model = (String) entries.get("model");
            if (targetModelName.equalsIgnoreCase(model)) {
                foundResourceModel = true;
                break;
            }
        }
        if (!foundResourceModel) {
            return null;
        }
        return entries;
    }

    public static boolean isOfflineWebsite(final Browser br) {
        return br.containsHTML("class=\"not\\-found\\-public__caption\"|class=\"error__icon error__icon_blocked\"|_file\\-blocked\"|A complaint was received regarding this file|>File blocked<") || br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500;
    }

    private String getHashFromURL(final String url) throws UnsupportedEncodingException, MalformedURLException {
        final String ret = UrlQuery.parse(url).get("hash");
        if (ret != null) {
            return URLDecoder.decode(ret, "UTF-8");
        } else {
            return null;
        }
    }

    private void parseFilePropertiesAPI(final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = DiskYandexNet.parseInformationAPIAvailablecheckFiles(this, dl, entries);
        dl.setAvailableStatus(status);
    }

    private void parseFilePropertiesWebsite(final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = DiskYandexNet.parseInformationWebsiteAvailablecheckFiles(this, dl, entries);
        dl.setAvailableStatus(status);
    }

    private void getPage(final String url) throws IOException {
        DiskYandexNet.getPage(this.br, url);
    }
}
