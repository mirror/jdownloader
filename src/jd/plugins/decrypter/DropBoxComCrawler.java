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
package jd.plugins.decrypter;

import java.awt.Dialog.ModalityType;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.components.config.DropBoxConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DropboxCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?dropbox\\.com/(?:(?:sh|s|sc|scl)/[^<>\"]+|l/[A-Za-z0-9]+).*|https?://(www\\.)?db\\.tt/[A-Za-z0-9]+|https?://dl\\.dropboxusercontent\\.com/s/.+" })
public class DropBoxComCrawler extends PluginForDecrypt {
    public DropBoxComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DropBoxConfig.class;
    }

    private static final String TYPES_NORMAL              = "https?://(?:www\\.)?dropbox\\.com/(sh|s|sc)/.+";
    private static final String TYPE_REDIRECT             = "https?://(?:www\\.)?dropbox\\.com/l/[A-Za-z0-9]+";
    private static final String TYPE_SHORT                = "https://(?:www\\.)?db\\.tt/[A-Za-z0-9]+";
    /* Unsupported linktypes which can occur during the decrypt process */
    /* 2019-09-20: Some time ago, these were direct-URLs. Now not anymore. */
    private static final String TYPE_DIRECTLINK_OLD       = "https?://dl\\.dropboxusercontent.com/s/(.+)";
    private static final String PROPERTY_CRAWL_SUBFOLDERS = "crawl_subfolders";

    private String getFilepathFromURL(final String url) {
        final String path = new Regex(url, "https?://[^/]+/(?:sh|s|sc)/[^/]+/[^/]+/([^\\?#]+)").getMatch(0);
        if (path != null) {
            return Encoding.htmlDecode(path);
        } else {
            return null;
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        /*
         * Do not set API headers on main browser object because if we use website crawler for some reason and have API login headers set
         * we'll run into problems for sure!
         */
        final Browser dummy_login_browser = new Browser();
        final boolean canLoginViaAPI = DropboxCom.setAPILoginHeaders(dummy_login_browser, account);
        final boolean urlCanBeCrawledViaAPI = !param.toString().contains("disallow_crawl_via_api=true") && !param.toString().matches(DropboxCom.TYPE_SC_GALLERY);
        final boolean canUseAPI = canLoginViaAPI && urlCanBeCrawledViaAPI;
        if (canUseAPI && DropboxCom.useAPI()) {
            br = dummy_login_browser;
            /**
             * 2019-09-19: TODO: Check if there is a way to use this part of their API without logging in e.g. general authorization header
             * provided by our official Dropbox developer account! Then make sure we do not run into some kind of rate-limit!
             */
            DropboxCom.prepBrAPI(this.br);
            DropboxCom.setAPILoginHeaders(this.br, account);
            return crawlViaAPI(param);
        } else {
            return crawlViaWebsite(param);
        }
    }

    private String correctAddedURL(final String url_original) {
        String url_new = url_original;
        if (url_original.matches(TYPE_DIRECTLINK_OLD)) {
            url_new = "https://www." + this.getHost() + "/s/" + new Regex(url_original, TYPE_DIRECTLINK_OLD).getMatch(0);
        }
        if (!url_new.equals(url_original)) {
            logger.info("Added URL has been changed: " + url_original + " --> " + url_new);
        } else {
            logger.info("Added URL was not changed");
        }
        return url_new;
    }

    private ArrayList<DownloadLink> crawlViaAPI(final CryptedLink param) throws Exception {
        final String parameter = correctAddedURL(param.toString());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /*
         * We cannot use the following request because we do not have the folderID at this stage:
         * https://www.dropbox.com/developers/documentation/http/documentation#sharing-get_folder_metadata
         */
        /** https://www.dropbox.com/developers/documentation/http/documentation#sharing-get_shared_link_metadata */
        /* To access crawled subfolders, we need the same URL as before but a different 'path' value! */
        final String last_path = getAdoptedCloudFolderStructure();
        /* Just a 2nd variable to make it clear where we started! */
        boolean is_root;
        boolean is_single_file = false;
        String path;
        if (DropboxCom.looksLikeSingleFile(parameter)) {
            /* This is crucial to access single files!! */
            path = null;
            is_root = true;
            is_single_file = true;
        } else {
            if (last_path != null) {
                /* Folder */
                /*
                 * Important! For the API to accept this we only need the path relative to our last folder so we'll have to filter this out
                 * of the full path!
                 */
                final String path_relative_to_parent_folder = new Regex(last_path, "(/[^/]*)$").getMatch(0);
                path = path_relative_to_parent_folder;
                is_root = false;
            } else {
                /* Folder-root or single file in folder */
                path = null;
                is_root = true;
            }
        }
        String passCode = param.getDecrypterPassword();
        String error_summary = null;
        boolean url_is_password_protected = !StringUtils.isEmpty(passCode);
        int counter = 0;
        do {
            try {
                if (url_is_password_protected && StringUtils.isEmpty(passCode)) {
                    passCode = getUserInput("Password?", param);
                } else if (passCode == null) {
                    /* Set to "" so that we do not send 'null' to the API. */
                    passCode = "";
                }
                /*
                 * 2019-09-24: In theory we could leave out this API request if we know that we have a folder and not only a single file BUT
                 * when accessing items of a folder it is not possible to get the name of the current folder and we want that - so we'll
                 * always do this request!
                 */
                final Map<String, Object> postData = new HashMap<String, Object>();
                postData.put("url", parameter);
                postData.put("path", path);
                postData.put("link_password", passCode);
                br.postPageRaw(DropboxCom.API_BASE + "/sharing/get_shared_link_metadata", JSonStorage.serializeToJson(postData));
                error_summary = DropboxCom.getErrorSummaryField(this.br);
                if (error_summary != null) {
                    if (error_summary.contains("shared_link_access_denied")) {
                        logger.info("URL appears to be password protected or your account is lacking the rights to view it");
                        url_is_password_protected = true;
                        /* Reset just in case we had a given password and that was wrong. Ask the user for the password now! */
                        passCode = null;
                        continue;
                    }
                }
                break;
            } finally {
                counter++;
            }
        } while (url_is_password_protected && counter <= 3);
        List<Object> ressourcelist = new ArrayList<Object>();
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final String object_type = (String) entries.get(".tag");
        if (!StringUtils.isEmpty(error_summary)) {
            /* 2019-09-198: Typically response 409 with error_summary 'shared_link_access_denied/..' */
            if (url_is_password_protected) {
                logger.info("Decryption failed because: Wrong password");
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else {
                logger.info("Decryption failed because: " + error_summary);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String cursor = null;
        Boolean has_more = null;
        // final String internal_folder_id = (String) entries.get("id");
        /* Important! Only fill this in if we have a folder as this may be used later as RELATIVE_DOWNLOAD_FOLDER_PATH! */
        final String folderName = !is_single_file ? (String) entries.get("name") : null;
        FilePackage fp = null;
        if ("file".equalsIgnoreCase(object_type)) {
            /* Single file */
            ressourcelist.add(entries);
        } else {
            /* Folder */
            String postdata_shared_link = "\"shared_link\": {\"url\":\"" + parameter + "\"";
            if (url_is_password_protected) {
                postdata_shared_link += ",\"password\":\"" + passCode + "\"";
            }
            postdata_shared_link += "}";
            /* 2019-09-25: Requested 'recursive' to work for shared URLs as well (currently only working for 'local files'). */
            /* Default API values: recursive=false, include_deleted=false */
            String postdata_list_folder = "{" + postdata_shared_link + ",\"recursive\":false,\"include_deleted\":false";
            if (path == null) {
                /* "" = root of a folder */
                postdata_list_folder += ",\"path\":\"\"";
            } else {
                /* Request specified path */
                postdata_list_folder += ",\"path\":\"" + path + "\"";
            }
            postdata_list_folder += "}";
            br.postPageRaw(DropboxCom.API_BASE + "/files/list_folder", postdata_list_folder);
        }
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            final Map<String, Object> foldermap = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            cursor = (String) foldermap.get("cursor");
            has_more = (Boolean) foldermap.get("has_more");
            final Object entriesO = foldermap.get("entries");
            if (entriesO != null) {
                ressourcelist = (List<Object>) foldermap.get("entries");
            }
            String subFolder = getAdoptedCloudFolderStructure();
            if (subFolder == null) {
                subFolder = "";
            }
            for (final Object folderO : ressourcelist) {
                final Map<String, Object> folderitemMap = (Map<String, Object>) folderO;
                final String type = (String) folderitemMap.get(".tag");
                final String name = (String) folderitemMap.get("name");
                final String serverside_path_full = (String) folderitemMap.get("path_display");
                final String id = (String) folderitemMap.get("id");
                /** TODO: Check if files with 'is_downloadable' == false are really not downloadable at all! */
                // final boolean is_downloadable = ((Boolean)entries.get("is_downloadable")).booleanValue();
                if (StringUtils.isEmpty(id)) {
                    continue;
                }
                final DownloadLink dl;
                if ("file".equalsIgnoreCase(type)) {
                    if (StringUtils.isEmpty(subFolder)) {
                        /*
                         * This may be the case if we jump into a nested folder straight away. We could use the 'path_lower' from the
                         * 'get_shared_link_metadata' API call but it is lowercase - we want the original path! So let's grab the path by
                         * filtering out of the full path of the first file-item in our list!
                         */
                        subFolder = new Regex(serverside_path_full, "(/[^/]+/.+)/[^/]+$").getMatch(0);
                        if (StringUtils.isEmpty(subFolder) && !StringUtils.isEmpty(folderName)) {
                            /* Last chance fallback */
                            subFolder = "/" + folderName;
                        }
                        fp = FilePackage.getInstance();
                        fp.setName(subFolder);
                    }
                    final long size = JavaScriptEngineFactory.toLong(folderitemMap.get("size"), 0);
                    dl = this.createDownloadlink("https://dropboxdecrypted.com/" + id);
                    /*
                     * 2019-09-20: In my tests I was not able to make use of this hash - here is some information about it:
                     * https://www.dropbox.com/developers/reference/content-hash
                     */
                    // final String content_hash = (String) entries.get("content_hash");
                    if (size > 0) {
                        dl.setDownloadSize(size);
                    }
                    if (!StringUtils.isEmpty(name)) {
                        dl.setFinalFileName(name);
                    } else {
                        /* Fallback - this should never be required! */
                        dl.setName(id);
                    }
                    /*
                     * This is the path we later need to download the file. It always has to be relative to our first added 'root' folder!
                     */
                    String serverside_path_to_file_relative;
                    if (is_root) {
                        /* Easy - file can be found on /<filename> */
                        serverside_path_to_file_relative = "/" + name;
                    } else {
                        /*
                         * E.g. /<rootFolder[current folder/folder which user has added!]>/subfolder1/subfolder2/filename.ext --> We need
                         * /subfolder1/subfolder2/filename.ext
                         */
                        serverside_path_to_file_relative = new Regex(serverside_path_full, "(?:/[^/]+)?(.+)$").getMatch(0);
                    }
                    if (StringUtils.isEmpty(serverside_path_to_file_relative)) {
                        /* Fallback - This should never happen! */
                        serverside_path_to_file_relative = serverside_path_full;
                    }
                    if (!StringUtils.isEmpty(serverside_path_to_file_relative) && !is_single_file) {
                        dl.setProperty(DropboxCom.PROPERTY_INTERNAL_PATH, serverside_path_to_file_relative);
                    }
                    if (!StringUtils.isEmpty(passCode)) {
                        dl.setDownloadPassword(passCode);
                        dl.setPasswordProtected(true);
                    }
                    if (is_single_file) {
                        dl.setProperty(DropboxCom.PROPERTY_IS_SINGLE_FILE, true);
                    }
                    dl.setProperty(DropboxCom.PROPERTY_MAINPAGE, parameter);
                    dl.setContainerUrl(parameter);
                    // dl.setProperty("serverside_path_full", serverside_path_full);
                    dl.setLinkID(this.getHost() + "://" + id);
                    dl.setContentUrl(parameter);
                    /*
                     * 2019-09-20: It can happen that single files inside a folder are offline although according to this API they are
                     * available and downloadable. This is hopefully a rare case. Via browser, these files are simply missing when the
                     * folder is loaded and will not get displayed at all!
                     */
                    dl.setAvailable(true);
                } else {
                    /* Subfolder */
                    /*
                     * Essentially we're adding the same URL to get crawled again but with a different 'path' value so let's modify the URL
                     * so that it goes back into this crawler!
                     */
                    dl = this.createDownloadlink(parameter + "?subfolder_path=" + serverside_path_full);
                }
                dl.setRelativeDownloadFolderPath(subFolder);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
                distribute(dl);
            }
            if (Boolean.TRUE.equals(has_more) && !StringUtils.isEmpty(cursor)) {
                /*
                 * They do not use 'classic' pagination but work with tokens so you cannot specify what to grab - you have to go through all
                 * 'pages' to find everything!
                 */
                /*
                 * 2019-09-20: I was not able to test this - tested with an example URL which contained over 1000 items but they all showed
                 * up on the first page!
                 */
                br.postPageRaw(DropboxCom.API_BASE + "/files/list_folder/continue", "{\"cursor\":\"" + cursor + "\"}");
            }
        } while (Boolean.TRUE.equals(has_more) && !StringUtils.isEmpty(cursor) && !this.isAbort());
        return ret;
    }

    private ArrayList<DownloadLink> crawlViaWebsite(final CryptedLink param) throws Exception {
        DropboxCom.prepBrWebsite(br);
        /* Website may return huge amounts of json/html */
        br.setLoadLimit(br.getLoadLimit() * 4);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        // TODO: 2023-05-08: Remove "correctAddedURL".
        final String correctedURL = correctAddedURL(param.getCryptedUrl());
        if (!correctedURL.equals(param.getCryptedUrl())) {
            logger.info("Changed added URL! Old: " + param.getCryptedUrl() + " | New: " + correctedURL);
            param.setCryptedUrl(correctedURL);
        }
        if (param.getCryptedUrl().matches(DropboxCom.TYPE_SC_GALLERY)) {
            /*
             * 2019-09-25: Gallerys are rarely used by Dropbox Users. Basically these are folders but we cannot access them like folders and
             * they cannot be accessed via API(?). Also downloading single objects from galleries works a bit different than files from
             * folders.
             */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String currentGalleryName = null;
            try {
                final String gallery_json = br.getRegex("InitReact\\.mountComponent\\(mod,\\s*(\\{.*?\"modules/clean/react/shared_link_collection/app\".*?\\})\\);").getMatch(0);
                Map<String, Object> galleryInfo = JavaScriptEngineFactory.jsonToJavaMap(gallery_json);
                galleryInfo = (Map<String, Object>) galleryInfo.get("props");
                currentGalleryName = (String) JavaScriptEngineFactory.walkJson(galleryInfo, "collection/name");
                FilePackage fp = null;
                if (!StringUtils.isEmpty(currentGalleryName)) {
                    fp = FilePackage.getInstance();
                    fp.setName(currentGalleryName);
                }
                final List<Map<String, Object>> galleryElements = (List<Map<String, Object>>) galleryInfo.get("collectionFiles");
                for (final Map<String, Object> galleryO : galleryElements) {
                    final DownloadLink dl = this.crawlFolderItem(galleryO);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    ret.add(dl);
                }
            } catch (final Exception e) {
                /* Fallback - add .zip containing all elements of that gallery! This should never happen! */
                final DownloadLink dl = this.createSingleFileDownloadLink(param.getCryptedUrl());
                if (currentGalleryName != null) {
                    dl.setFinalFileName("Gallery - " + currentGalleryName + ".zip");
                } else {
                    dl.setFinalFileName("Gallery - " + new Regex(param.getCryptedUrl(), "https?://[^/]+/(.+)").getMatch(0) + ".zip");
                }
                ret.add(dl);
            }
            return ret;
        }
        /*
         * 2019-09-24: isSingleFile may sometimes be wrong but if our URL contains 'crawl_subfolders=' we know it has been added via crawler
         * and it is definitely a folder and not a file!
         */
        final DownloadLink previousDownloadlink = param.getDownloadLink();
        final boolean enforceCrawlSubfoldersByProperty = previousDownloadlink != null && previousDownloadlink.hasProperty(PROPERTY_CRAWL_SUBFOLDERS);
        final boolean askIfSubfoldersShouldbeCrawled = PluginJsonConfig.get(DropBoxConfig.class).isAskIfSubfoldersShouldBeCrawled();
        final String storedPasswordCookieValue = previousDownloadlink != null ? previousDownloadlink.getStringProperty(DropboxCom.PROPERTY_PASSWORD_COOKIE) : null;
        if (storedPasswordCookieValue != null) {
            /**
             * If this is given, the folder is most likely password protected and the user has entered the correct password when a parent
             * folder was crawled. </br>
             * Re-using that cookie speeds up the crawl process as we do not have to send the password again for each subfolder we want to
             * crawl.
             */
            DropBoxComCrawler.setPasswordCookie(br, storedPasswordCookieValue);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 429) {
            /* 2023-05-08: I'm wonderung about this code snippet: Single file download? Or rate-limited? */
            logger.info("URL's downloads are disabled due to it generating too much traffic");
            final DownloadLink dl = createDownloadlink(param.getCryptedUrl().replace("dropbox.com/", "dropboxdecrypted.com/"));
            ret.add(dl);
            return ret;
        } else if (br.getHttpConnection().getResponseCode() == 460) {
            logger.info("Restricted Content: This file is no longer available. For additional information contact Dropbox Support.");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 509) {
            /* Temporarily unavailable link --> Rare case */
            final DownloadLink dl = createDownloadlink(param.getCryptedUrl().replace("dropbox.com/", "dropboxdecrypted.com/"));
            ret.add(dl);
            return ret;
        } else if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("sharing/error_shmodel|class=\"not-found\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String passCode = param.getDecrypterPassword();
        String passwordCookieValue = null;
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        brc.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        brc.getHeaders().put("Origin", "https://www." + this.getHost());
        if (DropboxCom.isPasswordProtectedWebsite(br)) {
            logger.info("Item is password protected | Password of previous folder-item: " + passCode);
            String content_id = new Regex(br.getURL(), "content_id=([^\\&;]+)").getMatch(0);
            if (content_id == null) {
                content_id = new Regex(br.getRedirectLocation(), "content_id=([^\\&;]+)").getMatch(0);
            }
            if (content_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean success = false;
            int counter = 0;
            do {
                if (passCode == null || counter > 0) {
                    passCode = getUserInput("Password?", param);
                }
                final UrlQuery query = new UrlQuery();
                query.add("is_xhr", "true");
                final String cookie_t = br.getCookie(getHost(), "t", Cookies.NOTDELETEDPATTERN);
                if (cookie_t != null) {
                    query.add("t", Encoding.urlEncode(cookie_t));
                }
                query.add("content_id", Encoding.urlEncode(content_id));
                query.add("password", Encoding.urlEncode(passCode));
                query.add("url", Encoding.urlEncode(new URL(param.getCryptedUrl()).getPath()));
                brc.postPage("/sm/auth", query);
                final String status = PluginJSonUtils.getJson(brc, "status");
                if (!"error".equalsIgnoreCase(status)) {
                    success = true;
                    break;
                } else {
                    /* Reset just in case we had a given password and that was wrong. Ask the user for the password now! */
                    passCode = null;
                    counter++;
                }
            } while (!success && counter <= 2);
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            passwordCookieValue = brc.getCookie(brc.getHost(), "sm_auth");
            /* 2023-05-03: It is very important to wait some seconds here or the auth token might not be accepted yet serverside! */
            final int waitSeconds = 5;
            logger.info("User entered correct password \"" + passCode + "\" | Waiting seconds before continuing: " + waitSeconds);
            this.sleep(waitSeconds * 1000, param);
            br.getPage(param.getCryptedUrl());
        }
        if (!br.getURL().matches(TYPES_NORMAL)) {
            logger.warning("Possible redirect to unsupported URL: " + br.getURL());
        }
        /* Decrypt file- and folderlinks */
        String subFolderPath = getAdoptedCloudFolderStructure();
        if (subFolderPath == null) {
            subFolderPath = "";
        }
        boolean askedUserIfHeWantsSubfolders = false;
        final int page_start = 1;
        int page = page_start;
        /* Contains information about current folder but not about subfolders and/or files! */
        String currentRootFolderName = null;
        String link_key = null;
        String secure_hash = null;
        String link_type = null;
        String rlkey = null;
        final String current_folder_json_source = br.getRegex("InitReact\\.mountComponent\\(mod,[ ]*(\\{[^\\n\\r]*?folderSharedLinkInfo[^\\n\\r]*?\\})\\);\\s+").getMatch(0);
        if (current_folder_json_source != null) {
            final Map<String, Object> folderInfo = JavaScriptEngineFactory.jsonToJavaMap(current_folder_json_source);
            final Map<String, Object> props = (Map<String, Object>) folderInfo.get("props");
            final Map<String, Object> folderShareToken = (Map<String, Object>) props.get("folderShareToken");
            link_key = (String) folderShareToken.get("linkKey");
            secure_hash = (String) folderShareToken.get("secureHash");
            link_type = (String) folderShareToken.get("linkType");
            rlkey = (String) folderShareToken.get("rlkey");
            currentRootFolderName = (String) JavaScriptEngineFactory.walkJson(props, "folderSharedLinkInfo/displayName");
            if (!StringUtils.isEmpty(currentRootFolderName)) {
                if (StringUtils.isEmpty(subFolderPath)) {
                    subFolderPath = currentRootFolderName;
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(subFolderPath);
        int current_page_numberof_items;
        int page_num = 0;
        String next_request_voucher = null;
        int website_max_items_per_page = 30;
        if (StringUtils.isEmpty(rlkey)) {
            rlkey = UrlQuery.parse(param.getCryptedUrl()).get("rlkey");
        }
        final Regex urlinfoTypeC = new Regex(param.getCryptedUrl(), "https://[^/]+/scl/([^/]+)/([^/]+)/([^/]+)");
        if (urlinfoTypeC.matches()) {
            if (StringUtils.isEmpty(link_type)) {
                link_type = "c";
            }
            if (StringUtils.isEmpty(link_key)) {
                link_key = urlinfoTypeC.getMatch(1);
            }
            if (StringUtils.isEmpty(secure_hash)) {
                secure_hash = urlinfoTypeC.getMatch(2);
            }
        } else {
            /* Typically dropbox.com/sh/bla/bla(?params...)? */
            link_type = "s";
            final Regex urlinfo = new Regex(param.getCryptedUrl(), "https?://[^/]+/([^/]+)/([^/]+)/([\\w\\-]+).*");
            if (StringUtils.isEmpty(link_key)) {
                link_key = urlinfo.getMatch(1);
            }
            if (StringUtils.isEmpty(secure_hash)) {
                secure_hash = urlinfo.getMatch(2);
            }
        }
        final String folderidString = link_type + "_" + link_key + "_" + secure_hash;
        final String dummyFilenameForErrors;
        if (!StringUtils.isEmpty(currentRootFolderName) && !StringUtils.isEmpty(subFolderPath)) {
            dummyFilenameForErrors = folderidString + "_" + currentRootFolderName;
        } else {
            dummyFilenameForErrors = folderidString;
        }
        final String cookie_t = br.getCookie(getHost(), "t", Cookies.NOTDELETEDPATTERN);
        do {
            page_num++;
            current_page_numberof_items = 0;
            logger.info("Crawling page: " + page_num);
            String json_source = null;
            final boolean isFirstPage = page == page_start;
            if (isFirstPage) {
                /* First page: Get prefetch-json from html */
                json_source = br.getRegex("REGISTER_SHARED_LINK_FOLDER_PRELOAD_HANDLER\"\\]\\.responseReceived\\(\"(\\{.*?\\})\"\\)\\}\\);").getMatch(0);
                if (json_source != null) {
                    json_source = PluginJSonUtils.unescape(json_source);
                } else {
                    logger.info("Failed to find json source for folder content on first page --> Plugin might be broken or this is a single file and not a folder");
                }
            }
            if (json_source == null) {
                if (isFirstPage) {
                    logger.info("First page didn't contain prefetch json -> Trying to obtain that via Ajax request");
                } else {
                    logger.info("Trying to load next page");
                }
                if (link_type == null || cookie_t == null || link_key == null || secure_hash == null) {
                    logger.warning("Stopping because: Pagination is not possible");
                    break;
                } else if (StringUtils.isEmpty(next_request_voucher) && !isFirstPage) {
                    /* next_request_voucher is required for pagination */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String sub_path = getFilepathFromURL(param.getCryptedUrl());
                if (sub_path == null) {
                    /* We're crawling a root directory. */
                    sub_path = "";
                }
                final Form pagination_form = new Form();
                pagination_form.setMethod(MethodType.POST);
                pagination_form.setAction("/list_shared_link_folder_entries");
                pagination_form.put("is_xhr", "true");
                pagination_form.put("link_key", link_key);
                pagination_form.put("link_type", link_type);
                pagination_form.put("secure_hash", secure_hash);
                pagination_form.put("sub_path", Encoding.urlEncode(sub_path));
                if (rlkey != null) {
                    pagination_form.put("rlkey", Encoding.urlEncode(rlkey));
                }
                if (next_request_voucher != null) {
                    pagination_form.put("voucher", Encoding.urlEncode(next_request_voucher));
                }
                pagination_form.put("t", cookie_t);
                brc.submitForm(pagination_form);
                if (brc.getHttpConnection().getResponseCode() == 404) {
                    /* 2023-05-03: Looks ugly but I haven't been able to come up with a better check so far... */
                    // logger.info("Looks like this is not a folder but a single file");
                    // ret.add(createSingleFileDownloadLink(param.getCryptedUrl()));
                    // return ret;
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    json_source = brc.toString();
                }
            }
            /* 2017-01-27 new */
            boolean crawlSubfolders = false;
            final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(json_source);
            if (response.isEmpty()) {
                if (ret.isEmpty()) {
                    /* Nothing has been found before -> Assume that we got a single file. */
                    final DownloadLink singleFile = createSingleFileDownloadLink(param.getCryptedUrl());
                    if (!StringUtils.isEmpty(passCode)) {
                        singleFile.setDownloadPassword(passCode);
                        singleFile.setPasswordProtected(true);
                        if (!StringUtils.isEmpty(passwordCookieValue)) {
                            singleFile.setProperty(DropboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookieValue);
                        }
                    }
                    ret.add(singleFile);
                    return ret;
                } else {
                    /* This should never happen. */
                    logger.info("Stopping because: Current page contains an empty map");
                    break;
                }
            }
            next_request_voucher = (String) response.get("next_request_voucher");
            if (next_request_voucher != null) {
                /* Only escape if it does not fit already */
                if (next_request_voucher.contains("\\\\")) {
                    next_request_voucher = PluginJSonUtils.unescape(next_request_voucher);
                }
                /* Dev-only! */
                final boolean enterRequestVoucherDebugCode = false;
                if (enterRequestVoucherDebugCode) {
                    final Map<String, Object> next_request_voucherMap = JSonStorage.restoreFromString(next_request_voucher, TypeRef.HASHMAP);
                    final String next_request_voucherMapJsonProg = (String) next_request_voucherMap.get("prog");
                    final Map<String, Object> next_request_voucherProg = JSonStorage.restoreFromString(next_request_voucherMapJsonProg, TypeRef.HASHMAP);
                    /*
                     * 2022-03-28: Absolute path might sometimes be available here but only sometimes -> Do not use this! It will lead to
                     * wrong paths for folder items without pagination!
                     */
                    final boolean useFullPathFromPagination = false;
                    final String ns_path = (String) next_request_voucherProg.get("ns_path");
                    if (!StringUtils.isEmpty(ns_path) && useFullPathFromPagination) {
                        fp.setName(ns_path);
                        subFolderPath = ns_path;
                    }
                }
            }
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) response.get("entries");
            final List<Map<String, Object>> ressourcelist_folders = new ArrayList<Map<String, Object>>();
            final List<Map<String, Object>> ressourcelist_files = new ArrayList<Map<String, Object>>();
            /* Separate files/folders */
            for (final Map<String, Object> folderRessource : ressourcelist) {
                if ((Boolean) folderRessource.get("is_dir") == Boolean.TRUE) {
                    ressourcelist_folders.add(folderRessource);
                } else {
                    ressourcelist_files.add(folderRessource);
                }
            }
            if (ressourcelist_files.isEmpty() && ressourcelist_folders.isEmpty()) {
                if (ret.isEmpty()) {
                    /* Looks like folder is empty */
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, dummyFilenameForErrors);
                } else {
                    logger.info("Stopping because: Current page does not contain any items");
                    break;
                }
            }
            if (enforceCrawlSubfoldersByProperty) {
                crawlSubfolders = true;
            } else if (!askIfSubfoldersShouldbeCrawled) {
                crawlSubfolders = true;
            } else if (askIfSubfoldersShouldbeCrawled && ressourcelist_folders.size() > 0 && !askedUserIfHeWantsSubfolders) {
                /*
                 * Only ask user if there are actually subfolders that can be crawled AND if we haven't asked him already for this folder
                 * AND if subfolders exist in this folder!
                 */
                final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, param.getCryptedUrl(), "For this URL JDownloader can crawl only the files inside the current folder or crawl subfolders as well. What would you like to do?", null, "Add files of current folder AND subfolders?", "Add only files of current folder?") {
                    @Override
                    public ModalityType getModalityType() {
                        return ModalityType.MODELESS;
                    }

                    @Override
                    public boolean isRemoteAPIEnabled() {
                        return true;
                    }
                };
                try {
                    UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
                    crawlSubfolders = true;
                } catch (DialogCanceledException e) {
                    crawlSubfolders = false;
                } catch (DialogClosedException e) {
                    crawlSubfolders = false;
                }
                askedUserIfHeWantsSubfolders = true;
                if (!crawlSubfolders && ressourcelist_files.isEmpty()) {
                    logger.info("User doesn't want subfolders but only subfolders are available!");
                    throw new DecrypterRetryException(RetryReason.PLUGIN_SETTINGS, "SUBFOLDER_CRAWL_DESELECTED_BUT_ONLY_SUBFOLDERS_AVAILABLE_" + dummyFilenameForErrors, "You deselected subfolder crawling but this folder contains only subfolders and no single files!");
                }
            }
            if (!ressourcelist_files.isEmpty()) {
                current_page_numberof_items += ressourcelist_files.size();
                for (final Map<String, Object> file : ressourcelist_files) {
                    final DownloadLink dl = this.crawlFolderItem(file);
                    if (!StringUtils.isEmpty(passCode)) {
                        dl.setDownloadPassword(passCode);
                        dl.setPasswordProtected(true);
                        if (!StringUtils.isEmpty(passwordCookieValue)) {
                            dl.setProperty(DropboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookieValue);
                        }
                    }
                    /*
                     * 2019-09-24: All URLs crawled via website crawler count as single files later on if we try to download them via API!
                     */
                    dl.setProperty(DropboxCom.PROPERTY_IS_SINGLE_FILE, true);
                    dl.setRelativeDownloadFolderPath(subFolderPath);
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                }
            }
            if (!ressourcelist_folders.isEmpty()) {
                current_page_numberof_items += ressourcelist_folders.size();
                if (crawlSubfolders) {
                    for (final Map<String, Object> folder : ressourcelist_folders) {
                        final DownloadLink dl = this.crawlFolderItem(folder);
                        /*
                         * Make sure that for password protected folders, user will not be asked for password again in next round/next
                         * subfolder-level.
                         */
                        if (!StringUtils.isEmpty(passCode)) {
                            dl.setDownloadPassword(passCode);
                            if (!StringUtils.isEmpty(passwordCookieValue)) {
                                dl.setProperty(DropboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookieValue);
                            }
                        }
                        final String foldername = folder.get("filename").toString();
                        /* Store next path as property so we can keep track of the full path. */
                        final String currentPath = subFolderPath + "/" + foldername;
                        dl.setRelativeDownloadFolderPath(currentPath);
                        dl.setProperty(PROPERTY_CRAWL_SUBFOLDERS, true);
                        ret.add(dl);
                        distribute(dl);
                    }
                }
            }
            if (current_page_numberof_items < website_max_items_per_page) {
                logger.info("Stopping because: Current page contains less items than " + website_max_items_per_page);
                break;
            } else if (StringUtils.isEmpty(next_request_voucher)) {
                logger.info("Stopping because: Failed to find next_request_voucher");
                break;
            }
            /* Continue to next page */
            page++;
        } while (!this.isAbort());
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    public static final void setPasswordCookie(final Browser br, final String value) {
        final String host;
        if (br.getHost() != null) {
            host = br.getHost();
        } else {
            host = "dropbox.com";
        }
        br.setCookie(host, "sm_auth", value);
    }

    private DownloadLink crawlFolderItem(final Map<String, Object> filefolderinfo) {
        final String url = filefolderinfo.get("href").toString();
        final Boolean is_dir = (Boolean) filefolderinfo.get("is_dir");
        final DownloadLink dl;
        if (is_dir) {
            /* Folder --> Will go back into crawler */
            dl = this.createDownloadlink(url);
        } else {
            dl = createSingleFileDownloadLink(url);
            parseMiscFileInfo(dl, filefolderinfo);
        }
        return dl;
    }

    public static void parseMiscFileInfo(final DownloadLink dl, final Map<String, Object> fileinfo) {
        /* Try to grab special downloadurls needed for items without official download button. */
        final String filename = (String) fileinfo.get("filename");
        final Number filesize = (Number) fileinfo.get("bytes");
        final String videoStreamURL = (String) JavaScriptEngineFactory.walkJson(fileinfo, "preview/content/transcode_url");
        final String photoStreamURL = (String) JavaScriptEngineFactory.walkJson(fileinfo, "preview/content/full_size_src");
        if (filesize != null) {
            dl.setVerifiedFileSize(filesize.longValue());
        }
        dl.setFinalFileName(filename);
        dl.setAvailable(true);
        if (!StringUtils.isEmpty(videoStreamURL)) {
            dl.setProperty(DropboxCom.PROPERTY_PREVIEW_DOWNLOADLINK, videoStreamURL);
        } else if (!StringUtils.isEmpty(photoStreamURL)) {
            dl.setProperty(DropboxCom.PROPERTY_PREVIEW_DOWNLOADLINK, photoStreamURL);
        }
        dl.setProperty(DropboxCom.PROPERTY_ORIGINAL_FILENAME, filename);
    }

    public static String getSharedJsonSource(final Browser br) {
        String json_source = br.getRegex("(\\s*\\{\\s*\\\\\"shared_link_infos.*?\\})\\s*\\)?\\s*;").getMatch(0);
        if (json_source != null) {
            json_source = JSonStorage.restoreFromString("\"" + json_source + "\"", TypeRef.STRING);
        }
        return json_source;
    }

    public static String getJsonSource(final Browser br) {
        String json_source = br.getRegex("InitReact\\.mountComponent\\(mod,\\s*(\\{.*?\\})\\)").getMatch(0);
        if (json_source == null) {
            json_source = br.getRegex("mod\\.initialize_module\\((\\{\"components\".*?)\\);\\s+").getMatch(0);
            if (json_source == null) {
                json_source = br.getRegex("mod\\.initialize_module\\((\\{.*?)\\);\\s+").getMatch(0);
            }
        }
        return json_source;
    }

    private DownloadLink createSingleFileDownloadLink(final String url) {
        final String urlpart = new Regex(url, "https?://[^/]+/(.+)").getMatch(0);
        if (urlpart == null) {
            return null;
        }
        final String urlForHosterplugin = "https://dropboxdecrypted.com/" + urlpart;
        return createDownloadlink(urlForHosterplugin);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    private Thread recommendAPIUsage() {
        final long display_dialog_every_x = 1 * 60 * 60 * 1000l;
        final long timestamp_last_time_displayed = this.getPluginConfig().getLongProperty("timestamp_last_time_displayed", 0);
        final long timestamp_display_dialog_next_time = timestamp_last_time_displayed + display_dialog_every_x;
        final long waittime_until_next_dialog_display = timestamp_display_dialog_next_time - System.currentTimeMillis();
        if (waittime_until_next_dialog_display > 0) {
            /* Do not display dialog this time - we do not want to annoy our users. */
            logger.info("Not displaying dialog now - waittime until next display: " + waittime_until_next_dialog_display);
            return null;
        }
        this.getPluginConfig().setProperty("timestamp_last_time_displayed", System.currentTimeMillis());
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Dropbox - bitte die API verwenden";
                        message += "Hallo liebe(r) Dropbox NutzerIn\r\n";
                        message += "Unser Dropbox Plugin verwendet die Dropbox Webseite sofern du keinen Dropbox Account eingetragen hast.\r\n";
                        message += "Leider ist dieser Weg manchmal unzuverlässig.\r\n";
                        message += "Falls soeben nicht alle Dateien und (Unter-)Ordner gefunden wurden, trage einen kostenlosen Dropbox Account in JDownloader ein und füge die Links erneut hinzu.\r\n";
                        message += "Dies ist keine Werbung! Leider können wir die zuverlässigere Dropbox Schnittstelle nur über registrierte Nutzeraccounts ansprechen.\r\n";
                        message += "Dropbox Accounts sind kostenlos. Es werden weder ein Abonnement- noch Zahlungsdfaten benötigt!\r\n";
                        message += "Falls du trotz eingetragenem Dropbox Account Probleme hast, kontaktiere bitte unseren Support!\r\n";
                    } else {
                        title = "Dropbox - recommendation to use API";
                        message += "Hello dear Dropbox user\r\n";
                        message += "Our Dropbox plugin is using the Dropbox website to find files- and (sub-)folders as long as no (Free) Account is added to JDownloader.\r\n";
                        message += "The Website handling may be unreliable sometimes!\r\n";
                        message += "If our plugin was unable to find all files- and (sub-)folders, add your free Dropbox account to JDownloader and re-add your URLs afterwards.\r\n";
                        message += "This is NOT and advertisement! Sadly the more reliable Dropbox API can only be used by registered users!\r\n";
                        message += "In case you are still experiencing issues even after adding a Dropbox account, please contact our support!\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
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
}