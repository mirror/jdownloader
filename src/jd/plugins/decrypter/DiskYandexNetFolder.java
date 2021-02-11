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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DiskYandexNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "disk.yandex.net", "docviewer.yandex.com" }, urls = { "https?://(?:www\\.)?(((((mail|disk)\\.)?yandex\\.(?:net|com|com\\.tr|ru|ua)|yadi\\.sk)/(disk/)?public/?(\\?hash=.+|#.+))|(?:yadi\\.sk|yadisk\\.cc)/(?:d|i)/[A-Za-z0-9\\-_]+(/[^/]+){0,}|yadi\\.sk/mail/\\?hash=.+)|https?://yadi\\.sk/a/[A-Za-z0-9\\-_]+", "https?://docviewer\\.yandex\\.(?:net|com|com\\.tr|ru|ua)/\\?url=ya\\-disk\\-public%3A%2F%2F.+" })
public class DiskYandexNetFolder extends PluginForDecrypt {
    public DiskYandexNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_docviewer     = "https?://docviewer\\.yandex\\.[^/]+/\\?url=ya\\-disk\\-public%3A%2F%2F([^/\"\\&]+).*?";
    private final String        type_primaryURLs   = ".+?public/?(\\?hash=.+|#.+)";
    private final String        type_shortURLs_d   = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+((/[^/]+){0,})";
    private final String        type_shortURLs_i   = "https?://(?:www\\.)?(yadi\\.sk|yadisk\\.cc)/i/[A-Za-z0-9\\-_]+";
    private final String        type_yadi_sk_mail  = "https?://(www\\.)?yadi\\.sk/mail/\\?hash=.+";
    private final String        type_yadi_sk_album = "https?://(www\\.)?yadi\\.sk/a/[A-Za-z0-9\\-_]+";
    private final String        DOWNLOAD_ZIP       = "DOWNLOAD_ZIP_2";
    private static final String JSON_TYPE_DIR      = "dir";

    /** Using API: https://tech.yandex.ru/disk/api/reference/public-docpage/ */
    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        jd.plugins.hoster.DiskYandexNet.prepBR(this.br);
        final String parameter = param.toString();
        if (parameter.matches(type_yadi_sk_album)) {
            /* Crawl albums */
            return crawlPhotoAlbum(parameter);
        } else {
            /**
             * 2021-02-09: New: Prefer website if we do now know whether we got a file or a folder! API will fail in case it is a single
             * file && is currently quota-limited!
             */
            if (StringUtils.isEmpty(this.getAdoptedCloudFolderStructure()) || StringUtils.isEmpty(getHashFromURL(parameter))) {
                logger.info("Using website crawler because we cannot know whether we got a single file- or a folder");
                return this.crawlFilesFoldersWebsite(param);
            } else {
                logger.info("Using API crawler");
                return this.crawlFilesFoldersAPI(param);
            }
        }
    }

    private ArrayList<DownloadLink> crawlFilesFoldersWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        String internalPath = new Regex(param.toString(), type_shortURLs_d).getMatch(1);
        if (internalPath != null) {
            internalPath = URLDecoder.decode(internalPath, "UTF-8");
            /* Remove parameter(s) */
            if (internalPath.contains("?")) {
                internalPath = internalPath.substring(0, internalPath.indexOf("?"));
            }
            /* No path given from previous crawler actions -> That's the best we can get. */
            if (StringUtils.isEmpty(relativeDownloadPath)) {
                relativeDownloadPath = internalPath;
            }
        }
        boolean is_part_of_a_folder = false;
        if (internalPath == null) {
            /* No path given? Crawl everything starting from root. */
            internalPath = "/";
        }
        String hashMain = getHashMain(param.getCryptedUrl());
        if (hashMain.contains(":/")) {
            /* Hash with path --> Separate that */
            final Regex hashregex = new Regex(hashMain, "(.*?):(/.+)");
            if (StringUtils.isEmpty(hashregex.getMatch(0))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Set correct value */
            hashMain = hashregex.getMatch(0);
            internalPath = hashregex.getMatch(1);
            is_part_of_a_folder = true;
        } else if (hashMain.contains(":")) {
            /* Small workaround: Hash contains remains of path -> Clean that */
            hashMain = hashMain.replace(":", "");
        }
        final String addedLink = "https://disk.yandex.com/public/?hash=" + URLEncode.encodeURIComponent(hashMain);
        /* Access URL if it hasn't been accessed before */
        if (br.getRequest() == null) {
            br.getPage(addedLink);
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String sk = jd.plugins.hoster.DiskYandexNet.getSK(this.br);
        final String json = br.getRegex("<script type=\"application/json\"[^>]*id=\"store-prefetch\"[^>]*>(.*?)</script>").getMatch(0);
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("resources");
        /*
         * First find the base folder name: If there are multiple items as part of a folder, the first item is kind of a dummy item
         * containing the name of the root folder.
         */
        String baseFolderName = null;
        for (final String key : entries.keySet()) {
            final Map<String, Object> ressource = (Map<String, Object>) entries.get(key);
            final String type = (String) ressource.get("type");
            if (type.equals("dir")) {
                baseFolderName = (String) ressource.get("name");
            }
            break;
        }
        if (StringUtils.isEmpty(baseFolderName)) {
            /* Fallback */
            baseFolderName = hashMain;
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
                    final String folderlink = "https://disk.yandex.com/public/?hash=" + URLEncode.encodeURIComponent(path);
                    final DownloadLink dl = createDownloadlink(folderlink);
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relativeDownloadPath + "/" + name);
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
                        jd.plugins.hoster.DiskYandexNet.setRawHash(dl, path);
                    } else {
                        /* Hash only */
                        jd.plugins.hoster.DiskYandexNet.setRawHash(dl, hash);
                    }
                    dl.setProperty("mainlink", urlContent);
                    dl.setContentUrl(urlContent);
                    dl.setProperty(DiskYandexNet.PROPERTY_INTERNAL_FUID, resource_id);
                    if (ressources.size() > 1) {
                        if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relativeDownloadPath);
                        }
                        dl._setFilePackage(fp);
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
            if (page > 0) {
                completed = ((Boolean) entries.get("completed")).booleanValue();
            } else {
                if (!completed) {
                    completed = ressources.size() < maxItemsPerPage;
                }
            }
            if (completed) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (StringUtils.isEmpty(sk)) {
                logger.warning("Pagination failure: sk missing");
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
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Content-Type", "text/plain");
            br.getHeaders().put("Origin", "https://disk.yandex.com");
            br.postPageRaw("/public/api/fetch-list", "%7B%22hash%22%3A%22" + Encoding.urlEncode(hashMain) + "%3A%22%2C%22offset%22%3A" + offset + "%2C%22withSizes%22%3Atrue%2C%22sk%22%3A%22" + sk + "%22%2C%22options%22%3A%7B%22hasExperimentVideoWithoutPreview%22%3Atrue%7D%7D");
            entries = JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP);
            ressources = (List<Object>) entries.get("resources");
            page += 1;
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private String getHashMain(final String url) throws PluginException, IOException {
        String hashMain;
        if (url.matches(type_docviewer)) {
            /* Documents in web view mode --> File-URLs! */
            /* First lets fix broken URLs by removing unneeded parameters ... */
            String tmp = url;
            final String remove = new Regex(tmp, "(\\&[a-z0-9]+=.+)").getMatch(0);
            if (remove != null) {
                tmp = tmp.replace(remove, "");
            }
            String hash = new Regex(tmp, type_docviewer).getMatch(0);
            if (StringUtils.isEmpty(hash)) {
                hash = new Regex(tmp, "url=ya\\-disk\\-public%3A%2F%2F(.+)").getMatch(0);
            }
            if (StringUtils.isEmpty(hash)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            hashMain = URLDecoder.decode(hash, "UTF-8");
        } else if (url.matches(type_yadi_sk_mail)) {
            hashMain = getHashFromURL(url);
        } else if (url.matches(type_shortURLs_d) || url.matches(type_shortURLs_i)) {
            getPage(url);
            if (isOfflineWebsite(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            hashMain = PluginJSonUtils.getJsonValue(br, "hash");
            if (hashMain == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            hashMain = getHashFromURL(url);
        }
        return hashMain;
    }

    private ArrayList<DownloadLink> crawlFilesFoldersAPI(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String relativeDownloadPath = this.getAdoptedCloudFolderStructure();
        if (relativeDownloadPath == null) {
            relativeDownloadPath = "";
        }
        String internalPath = new Regex(param.getCryptedUrl(), type_shortURLs_d).getMatch(1);
        if (internalPath != null) {
            internalPath = URLDecoder.decode(internalPath, "UTF-8");
            /* Remove parameter(s) */
            if (internalPath.contains("?")) {
                internalPath = internalPath.substring(0, internalPath.indexOf("?"));
            }
            /* No path given from previous crawler actions -> That's the best we can get. */
            if (StringUtils.isEmpty(relativeDownloadPath)) {
                relativeDownloadPath = internalPath;
            }
        }
        boolean is_part_of_a_folder = false;
        if (internalPath == null) {
            /* No path given? Crawl everything starting from root. */
            internalPath = "/";
        }
        String hashMain = getHashMain(param.getCryptedUrl());
        if (hashMain == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (hashMain.contains(":/")) {
            /* Hash with path --> Separate that */
            final Regex hashregex = new Regex(hashMain, "(.*?):(/.+)");
            if (StringUtils.isEmpty(hashregex.getMatch(0))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Set correct value */
            hashMain = hashregex.getMatch(0);
            internalPath = hashregex.getMatch(1);
            is_part_of_a_folder = true;
        } else if (hashMain.contains(":")) {
            /* Small workaround: Hash contains remains of path -> Clean that */
            hashMain = hashMain.replace(":", "");
        }
        final String addedLink = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(hashMain);
        this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        short offset = 0;
        final short entries_per_request = 200;
        long totalNumberofEntries = 0;
        final FilePackage fp = FilePackage.getInstance();
        do {
            getPage("https://cloud-api.yandex.net/v1/disk/public/resources?limit=" + entries_per_request + "&offset=" + offset + "&public_key=" + URLEncode.encodeURIComponent(hashMain) + "&path=" + URLEncode.encodeURIComponent(internalPath));
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            /*
             * 2021-01-19:
             * {"message":"Не удалось найти запрошенный ресурс.","description":"Resource not found.","error":"DiskNotFoundError"}
             */
            if (entries.containsKey("error")) {
                decryptedLinks.add(this.createOfflinelink(addedLink));
                return decryptedLinks;
            }
            final String type_main = (String) entries.get("type");
            if (!type_main.equals(JSON_TYPE_DIR)) {
                /* We only have a single file --> Add to downloadliste / host plugin */
                final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                if (jd.plugins.hoster.DiskYandexNet.apiAvailablecheckIsOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(addedLink));
                    return decryptedLinks;
                }
                parseFilePropertiesAPI(dl, entries);
                if (is_part_of_a_folder) {
                    /* TODO: Check this! */
                    /* 2017-04-07: Overwrite previously set path value with correct value. */
                    dl.setProperty("path", internalPath);
                }
                dl.setProperty("mainlink", addedLink);
                dl.setLinkID(hashMain + internalPath);
                /* Required by hoster plugin to get filepath (filename) */
                dl.setProperty(DiskYandexNet.PROPERTY_CRAWLED_FILENAME, PluginJSonUtils.getJsonValue(br, "name"));
                if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relativeDownloadPath);
                }
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            final String walk_string = "_embedded/items";
            final List<Object> resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, walk_string);
            if (offset == 0) {
                /* Set total number of entries on first loop. */
                final Map<String, Object> itemInfo = (Map<String, Object>) entries.get("_embedded");
                totalNumberofEntries = JavaScriptEngineFactory.toLong(itemInfo.get("total"), 0);
                if (totalNumberofEntries == 0) {
                    logger.info("Empty folder");
                    return decryptedLinks;
                }
                String baseFolderName = (String) entries.get("name");
                if (StringUtils.isEmpty(baseFolderName)) {
                    /* Fallback */
                    baseFolderName = hashMain;
                }
                fp.setName(baseFolderName);
                if (StringUtils.isEmpty(relativeDownloadPath)) {
                    /* First time crawl of a possible folder structure -> Define root dir name */
                    relativeDownloadPath = baseFolderName;
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
                final String hashFull = hash + ":" + path;
                if (type.equals(JSON_TYPE_DIR)) {
                    /* Subfolders go back into our decrypter! */
                    final String folderlink = "https://disk.yandex.com/public?hash=" + URLEncode.encodeURIComponent(hashFull);
                    final DownloadLink dl = createDownloadlink(folderlink);
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relativeDownloadPath + "/" + name);
                    decryptedLinks.add(dl);
                } else {
                    final String media_type = (String) entries.get("media_type");
                    final String resource_id = (String) entries.get("resource_id");
                    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(hash) || StringUtils.isEmpty(resource_id)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final DownloadLink dl = createDownloadlink("http://yandexdecrypted.net/" + System.currentTimeMillis() + new Random().nextInt(10000000));
                    parseFilePropertiesAPI(dl, entries);
                    final String urlContent = "https://disk.yandex.com/public/?hash=" + URLEncode.encodeURIComponent(hashFull);
                    String urlUser;
                    if ("document".equalsIgnoreCase(media_type)) {
                        /*
                         * Set contentURL which links to a comfortable web-view of documents whenever it makes sense.
                         */
                        urlUser = "https://docviewer.yandex.com/?url=ya-disk-public%3A%2F%2F" + URLEncode.encodeURIComponent(hashFull);
                    } else {
                        /*
                         * No fancy content URL available - set main URL.
                         */
                        urlUser = urlContent;
                    }
                    jd.plugins.hoster.DiskYandexNet.setRawHash(dl, hashFull);
                    dl.setProperty("mainlink", urlContent);
                    if (StringUtils.isNotEmpty(relativeDownloadPath)) {
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, relativeDownloadPath);
                    }
                    dl.setContentUrl(urlUser);
                    dl.setProperty(DiskYandexNet.PROPERTY_INTERNAL_FUID, resource_id);
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
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
        if (decryptedLinks.size() == 0) {
            /* Should never happen! */
            logger.info("Probably empty folder");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    /** For e.g. https://yadi.sk/a/blabla */
    private ArrayList<DownloadLink> crawlPhotoAlbum(String addedLink) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> dupeList = new ArrayList<String>();
        final String domain = "disk.yandex.com";
        getPage(addedLink);
        if (isOfflineWebsite(this.br)) {
            decryptedLinks.add(this.createOfflinelink(addedLink));
            return decryptedLinks;
        }
        String sk = jd.plugins.hoster.DiskYandexNet.getSK(this.br);
        String fpName = null;
        final String clientID = jd.plugins.hoster.YandexAlbum.albumGetIdClient();
        final String hash_short = new Regex(addedLink, "/a/(.+)").getMatch(0);
        final String rawHash = jd.plugins.hoster.DiskYandexNet.getHashLongFromHTML(this.br);
        final String json_of_first_page = regExJSON(this.br);
        if (StringUtils.isEmpty(rawHash)) {
            /* Value is required! */
            decryptedLinks.add(this.createOfflinelink(addedLink));
            return decryptedLinks;
        }
        if (StringUtils.isEmpty(sk)) {
            /** TODO: Maybe keep SK throughout sessions to save that one request ... */
            logger.info("Getting new SK value ...");
            sk = jd.plugins.hoster.DiskYandexNet.getNewSK(this.br, domain, addedLink);
            if (StringUtils.isEmpty(sk)) {
                logger.warning("Failed to get SK value");
                throw new DecrypterException();
            }
        }
        prepBrAlbum(this.br);
        final int maxItemsPerPage = 40;
        int addedItemsTemp = 0;
        int offset = 0;
        String idItemLast = null;
        final FilePackage fp = FilePackage.getInstance();
        do {
            addedItemsTemp = 0;
            Map<String, Object> entries = null;
            final List<Object> modelObjects;
            if (offset == 0) {
                /* First loop */
                modelObjects = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(json_of_first_page);
                entries = findModel(modelObjects, "album");
                entries = (Map<String, Object>) entries.get("data");
                fpName = (String) entries.get("title");
                if (StringUtils.isEmpty(fpName)) {
                    fpName = hash_short;
                }
                fp.setName(fpName);
            } else {
                br.postPage("https://" + domain + "/album-models/?_m=resources", "_model.0=resources&idContext.0=%2Falbum%2F" + URLEncode.encodeURIComponent(rawHash) + "&order.0=1&sort.0=order_index&offset.0=" + offset + "&amount.0=" + maxItemsPerPage + "&idItemLast.0=" + URLEncode.encodeURIComponent(idItemLast) + "&idClient=" + clientID + "&version=" + jd.plugins.hoster.YandexAlbum.VERSION_YANDEX_PHOTO_ALBUMS + "&sk=" + sk);
                entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                modelObjects = (List<Object>) entries.get("models");
            }
            entries = findModel(modelObjects, "resources");
            if (entries == null) {
                logger.warning("Failed to find resource model");
                throw new DecrypterException();
            }
            final List<Object> mediaObjects = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/resources");
            for (final Object mediao : mediaObjects) {
                entries = (Map<String, Object>) mediao;
                /* Unique id e.g. '/album/<public_key>:<item_id>' */
                final String id = (String) entries.get("id");
                final String item_id = (String) entries.get("item_id");
                if (StringUtils.isEmpty(id) || StringUtils.isEmpty(item_id)) {
                    /* his should never happen */
                    continue;
                }
                if (dupeList.contains(id)) {
                    logger.info("Stopping to avoid an endless loop because of duplicates / wrong 'idItemLast.0' value");
                    return decryptedLinks;
                }
                final String url = String.format("https://yadi.sk/a/%s/%s", URLEncode.encodeURIComponent(hash_short), URLEncode.encodeURIComponent(item_id));
                final DownloadLink dl = this.createDownloadlink(url);
                dl.setLinkID(hash_short + "/" + item_id);
                jd.plugins.hoster.YandexAlbum.parseInformationAPIAvailablecheckAlbum(this, dl, entries);
                jd.plugins.hoster.DiskYandexNet.setRawHash(dl, rawHash);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                offset++;
                addedItemsTemp++;
                if (addedItemsTemp == mediaObjects.size()) {
                    /* Important for ajax request - id of our last object */
                    idItemLast = id;
                }
                dupeList.add(id);
            }
        } while (!this.isAbort() && addedItemsTemp >= maxItemsPerPage && idItemLast != null);
        if (offset == 0) {
            logger.warning("Failed to find items");
            throw new DecrypterException();
        }
        return decryptedLinks;
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

    private String getHashFromURL(final String url) throws UnsupportedEncodingException {
        final String ret = new Regex(url, "hash=([^&#]+)").getMatch(0);
        if (ret != null) {
            return URLDecoder.decode(ret, "UTF-8");
        } else {
            return null;
        }
    }

    private void parseFilePropertiesAPI(final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = jd.plugins.hoster.DiskYandexNet.parseInformationAPIAvailablecheckFiles(this, dl, entries);
        dl.setAvailableStatus(status);
    }

    private void parseFilePropertiesWebsite(final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        final AvailableStatus status = jd.plugins.hoster.DiskYandexNet.parseInformationWebsiteAvailablecheckFiles(this, dl, entries);
        dl.setAvailableStatus(status);
    }

    private void getPage(final String url) throws IOException {
        jd.plugins.hoster.DiskYandexNet.getPage(this.br, url);
    }
}
