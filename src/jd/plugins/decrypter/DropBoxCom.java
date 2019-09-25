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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DropboxCom;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.plugins.components.config.DropBoxConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?dropbox\\.com/(?:(?:sh|s)/[^<>\"]+|l/[A-Za-z0-9]+).*|https?://(www\\.)?db\\.tt/[A-Za-z0-9]+|https?://dl\\.dropboxusercontent\\.com/s/.+" })
public class DropBoxCom extends PluginForDecrypt {
    public DropBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DropBoxConfig.class;
    }

    private static final String TYPE_NORMAL                = "https?://(www\\.)?dropbox\\.com/(sh|sc)/.+";
    private static final String TYPE_REDIRECT              = "https?://(www\\.)?dropbox\\.com/l/[A-Za-z0-9]+";
    private static final String TYPE_SHORT                 = "https://(www\\.)?db\\.tt/[A-Za-z0-9]+";
    /* Unsupported linktypes which can occur during the decrypt process */
    /* 2019-09-20: Some time ago, these were direct-URLs. Now not anymore. */
    private static final String TYPE_DIRECTLINK_OLD        = "https?://dl\\.dropboxusercontent.com/s/(.+)";
    private static final String TYPE_REFERRAL              = "https?://[^/]+/referrals/.+";
    private String              subFolder                  = "";
    private final int           website_max_items_per_page = 30;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        subFolder = getAdoptedCloudFolderStructure();
        if (subFolder == null) {
            subFolder = "";
        }
        final ArrayList<DownloadLink> decryptedLinks;
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        /*
         * Do not set API headers on main browser object because if we use website crawler for some reason and have API login headers set
         * we'll run into problems for sure!
         */
        final Browser dummy_login_browser = new Browser();
        final boolean canLoginViaAPI = jd.plugins.hoster.DropboxCom.setAPILoginHeaders(dummy_login_browser, account);
        final boolean urlCanBeCrawledViaAPI = !param.toString().contains("disallow_crawl_via_api=true");
        final boolean canUseAPI = canLoginViaAPI && urlCanBeCrawledViaAPI;
        if (canUseAPI && jd.plugins.hoster.DropboxCom.useAPI()) {
            br = dummy_login_browser;
            /**
             * 2019-09-19: TODO: Check if there is a way to use this part of their API without logging in e.g. general authorization header
             * provided by our official Dropbox developer account! Then make sure we do not run into some kind of rate-limit!
             */
            jd.plugins.hoster.DropboxCom.prepBrAPI(this.br);
            jd.plugins.hoster.DropboxCom.setAPILoginHeaders(this.br, account);
            decryptedLinks = crawlViaAPI(param);
        } else {
            decryptedLinks = crawlViaWebsite(param);
            if (decryptedLinks == null || (decryptedLinks != null && decryptedLinks.size() == website_max_items_per_page)) {
                /*
                 * Possible website-crawler failure - remind user to add an account so API can be used and crawler can work more reliable!
                 */
                recommendAPIUsage();
            }
        }
        if (decryptedLinks.size() == 0) {
            final DownloadLink dl = this.createOfflinelink(param.toString());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private String correctAddedURL(final String url_original) {
        String url_new;
        final String args = new Regex(url_original, "(\\?.+)").getMatch(0);
        if (args != null) {
            /* Remove that from our initial URL. */
            url_new = url_original.replace(args, "");
        } else {
            url_new = url_original;
        }
        if (url_new.matches(TYPE_DIRECTLINK_OLD)) {
            url_new = "https://www." + this.getHost() + "/s/" + new Regex(url_new, TYPE_DIRECTLINK_OLD).getMatch(0);
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
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
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
        if (jd.plugins.hoster.DropboxCom.isSingleFile(parameter)) {
            /* This is crucial!! */
            path = "null";
            is_root = true;
            is_single_file = true;
        } else {
            if (last_path != null) {
                /*
                 * Important! For the API to accept this we only need the path relative to our last folder so we'll have to filter this out
                 * of the full path!
                 */
                final String path_relative_to_parent_folder = new Regex(last_path, "(/[^/]*)$").getMatch(0);
                path = path_relative_to_parent_folder;
                is_root = false;
            } else {
                /* Root */
                path = "/";
                is_root = true;
            }
            /* Fix json value */
            path = "\"" + path + "\"";
        }
        String passCode = param.getDecrypterPassword();
        String error_summary = null;
        boolean url_is_password_protected = !StringUtils.isEmpty(passCode);
        boolean continue_reason = false;
        /*
         * 2019-09-24: For single files, we have to set path to "null" but in some cases, we cannot detect single files simply by looking at
         * the URL. This workaround exists to solve this issue!
         */
        final boolean enable_file_folder_workaround = true;
        /**
         * 2019-09-24: TODO: 'link_password' does not yet work for this request! Also "recursive":true will not work for the 'list_folder'
         * request although this would be useful here! Waiting for answer of their support ...
         */
        final boolean enable_password_protected_workaround = true;
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
                br.postPageRaw(jd.plugins.hoster.DropboxCom.API_BASE + "/sharing/get_shared_link_metadata", "{\"url\":\"" + parameter + "\",\"path\":" + path + ",\"link_password\":\"" + passCode + "\"}");
                error_summary = jd.plugins.hoster.DropboxCom.getErrorSummaryField(this.br);
                if (error_summary != null) {
                    if (error_summary.contains("shared_link_access_denied")) {
                        logger.info("URL appears to be password protected or your account is lacking the rights to view it");
                        url_is_password_protected = true;
                        /* Reset just in case we had a given password and that was wrong. Ask the user for the password now! */
                        passCode = null;
                        continue;
                    } else if (enable_file_folder_workaround) {
                        logger.info("Trying file_folder_workaround");
                        path = "null";
                        is_root = true;
                        is_single_file = true;
                        continue_reason = true;
                        continue;
                    }
                } else {
                    continue_reason = false;
                }
                break;
            } finally {
                counter++;
            }
        } while ((url_is_password_protected || continue_reason) && counter <= 3);
        ArrayList<Object> ressourcelist = new ArrayList<Object>();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String object_type = (String) entries.get(".tag");
        if (!StringUtils.isEmpty(error_summary)) {
            /* 2019-09-198: Typically response 409 with error_summary 'shared_link_access_denied/..' */
            if (url_is_password_protected) {
                logger.info("Decryption failed because of wrong password");
            } else {
                logger.info("Decryption failed because: " + error_summary);
            }
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String cursor = null;
        boolean has_more = false;
        final String internal_folder_id = (String) entries.get("id");
        /* Important! Only fill this in if we have a folder as this may be used later as RELATIVE_DOWNLOAD_FOLDER_PATH! */
        final String folderName = !is_single_file ? (String) entries.get("name") : null;
        final String packageName;
        if (!StringUtils.isEmpty(folderName)) {
            packageName = folderName;
        } else {
            packageName = internal_folder_id;
        }
        FilePackage fp = null;
        /* Do not set packagenames if we only have a single file!! */
        if (!StringUtils.isEmpty(packageName) && !is_single_file) {
            fp = FilePackage.getInstance();
            fp.setName(packageName);
        }
        if ("file".equalsIgnoreCase(object_type)) {
            /* Single file */
            ressourcelist.add(entries);
        } else {
            /* Folder */
            if (path.equals("/") || path.equals("\"/\"")) {
                /*
                 * 2019-09-20: For this call, "/" is not accepted to get the root folder although their API 'knows what we want'. This is
                 * really strange but okay. Error when used with wrong value: "Error in call to API function "files/
                 * list_folder": request body: path: Specify the root folder as an empty string rather than as "/"."
                 */
                path = "\"\"";
            }
            if (url_is_password_protected && enable_password_protected_workaround) {
                logger.info("Adding URL again to be crawled via website handling because API has issues with password protected URLs");
                final DownloadLink dl = super.createDownloadlink(parameter + "?disallow_crawl_via_api=true");
                /* We already know the correct password! Store it so we do not have to ask the user again :) */
                dl.setDownloadPassword(passCode);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            br.postPageRaw(jd.plugins.hoster.DropboxCom.API_BASE + "/files/list_folder", "{\"path\":" + path + ",\"shared_link\": {\"url\":\"" + parameter + "\"},\"recursive\":false}");
        }
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            cursor = (String) entries.get("cursor");
            final Object has_moreO = entries.get("has_more");
            if (has_moreO == null) {
                has_more = false;
            } else {
                has_more = ((Boolean) has_moreO).booleanValue();
            }
            final Object entriesO = entries.get("entries");
            if (entriesO != null) {
                ressourcelist = (ArrayList<Object>) entries.get("entries");
            }
            for (final Object folderO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) folderO;
                final String type = (String) entries.get(".tag");
                final String name = (String) entries.get("name");
                final String serverside_path_full = (String) entries.get("path_display");
                final String id = (String) entries.get("id");
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
                    }
                    final long size = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
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
                        dl.setProperty(DropboxCom.PROPERTY_IS_PASSWORD_PROTECTED, true);
                    }
                    if (is_single_file) {
                        dl.setProperty(DropboxCom.PROPERTY_IS_SINGLE_FILE, true);
                    }
                    dl.setProperty(DropboxCom.PROPERTY_MAINPAGE, parameter);
                    // dl.setProperty("serverside_path_full", serverside_path_full);
                    dl.setLinkID(this.getHost() + "://" + id);
                    /**
                     * 2019-09-20: TODO: Find out if it is possible to generate URLs which lead to the exact files when opening them via
                     * browser. </br> At the moment this is a huge issue when using their API - it contains other(internal) fileIDs so we
                     * cannot get to the corresponding public contentURLs although they do exist!
                     */
                    dl.setContentUrl(parameter);
                    /*
                     * 2019-09-20: It can happen that single files inside a folder are offline although according to this API they are
                     * available and downloadable. This is hopefully a rare case. Via browser, these files are simply missing when the
                     * folder is loaded and will not get displayed at all!
                     */
                    dl.setAvailable(true);
                } else {
                    /*
                     * Essentially we're adding the same URL to get crawled again but with a different 'path' value so let's modify the URL
                     * so that it goes back into this crawler!
                     */
                    dl = this.createDownloadlink(parameter + "?subfolder_path=" + serverside_path_full, serverside_path_full);
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }
            if (has_more && !StringUtils.isEmpty(cursor)) {
                /*
                 * They do not use 'classic' pagination but work with tokens so you cannot specify what to grab - you have to go through all
                 * 'pages' to find everything!
                 */
                /*
                 * 2019-09-20: I was not able to test this - tested with an example URL which contained over 1000 items but they all showed
                 * up on the first page!
                 */
                br.postPageRaw(jd.plugins.hoster.DropboxCom.API_BASE + "/files/list_folder/continue", "{\"cursor\":\"" + cursor + "\"}");
            }
        } while (has_more && !StringUtils.isEmpty(cursor) && !this.isAbort());
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlViaWebsite(final CryptedLink param) throws Exception {
        br.setFollowRedirects(false);
        jd.plugins.hoster.DropboxCom.prepBrWebsite(br);
        /* Website may return hige amounts of json/html */
        br.setLoadLimit(br.getLoadLimit() * 4);
        final String crawl_subfolder_string = new Regex(param.toString(), "(\\&crawl_subfolders=(?:true|false))").getMatch(0);
        final AtomicReference<FilePackage> currentPackage = new AtomicReference<FilePackage>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                final FilePackage fp = currentPackage.get();
                if (fp != null) {
                    fp.add(e);
                }
                distribute(e);
                return super.add(e);
            }
        };
        String parameter = correctAddedURL(param.toString());
        /*
         * 2019-09-24: isSingleFile may sometimes be wrongt but if our URL contains 'crawl_subfolders=' we know it has been added via
         * crawler and it is definitely a folder and not a file!
         */
        if (DropboxCom.isSingleFile(parameter) && crawl_subfolder_string == null) {
            decryptedLinks.add(createSingleDownloadLink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 429) {
            logger.info("URL's downloads are disabled due to it generating too much traffic");
            final DownloadLink dl = createDownloadlink(parameter.replace("dropbox.com/", "dropboxdecrypted.com/"));
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 460) {
            logger.info("Restricted Content: This file is no longer available. For additional information contact Dropbox Support.");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 509) {
            /* Temporarily unavailable link */
            final DownloadLink dl = createDownloadlink(parameter.replace("dropbox.com/", "dropboxdecrypted.com/"));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null && (parameter.matches(TYPE_REDIRECT) || parameter.matches(TYPE_SHORT))) {
            parameter = br.getRedirectLocation();
            if (parameter.matches(TYPE_REFERRAL)) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else if (!parameter.matches(TYPE_NORMAL)) {
                logger.warning("Decrypter broken or unsupported redirect-url: " + parameter);
                return null;
            }
        }
        br.setFollowRedirects(true);
        br.followConnection();
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("sharing/error_shmodel|class=\"not-found\">")) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String passCode = param.getDecrypterPassword();
        String password_cookie = null;
        if (DropboxCom.isPasswordProtectedWebsite(br)) {
            final String content_id = new Regex(br.getURL(), "content_id=([^\\&]+)").getMatch(0);
            if (content_id == null) {
                logger.warning("Failed to find content_id");
                return null;
            }
            boolean wrongPass = true;
            int counter = 0;
            do {
                if (passCode == null) {
                    passCode = getUserInput("Password?", param);
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                String post_data = "is_xhr=true&content_id=" + content_id + "&password=" + Encoding.urlEncode(passCode);
                final String cookie_t = br.getCookie(getHost(), "t");
                if (cookie_t != null) {
                    post_data += "&t=" + cookie_t;
                }
                br.postPage("/sm/auth", post_data);
                final String status = PluginJSonUtils.getJson(br, "status");
                if (!"error".equalsIgnoreCase(status)) {
                    wrongPass = false;
                    break;
                }
                /* Reset just in case we had a given password and that was wrong. Ask the user for the password now! */
                passCode = null;
                counter++;
            } while (wrongPass && counter <= 2);
            password_cookie = br.getCookie(br.getHost(), "sm_auth");
            br.getPage(parameter);
        }
        /* Decrypt file- and folderlinks */
        boolean hasMore = false;
        boolean isShared = false;
        boolean askedUserIfHeWantsSubfolders = false;
        final int page_start = 1;
        int page = page_start;
        String json_source = null;
        /* Contains information about current folder but not about subfolders and/or files! */
        final String current_folder_json_source = br.getRegex("InitReact\\.mountComponent\\(mod,\\s*(\\{\"module_name\":\\s*\"modules/clean/react/shared_link_folder.*?\\})\\);").getMatch(0);
        LinkedHashMap<String, Object> entries = null;
        String currentRootFolderName = null;
        if (current_folder_json_source != null) {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(current_folder_json_source);
            currentRootFolderName = (String) JavaScriptEngineFactory.walkJson(entries, "props/folderSharedLinkInfo/displayName");
            if (!StringUtils.isEmpty(currentRootFolderName)) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(currentRootFolderName);
                currentPackage.set(fp);
                if (StringUtils.isEmpty(subFolder)) {
                    subFolder = currentRootFolderName;
                }
            }
        }
        int current_numberof_items;
        int page_num = 0;
        do {
            page_num++;
            current_numberof_items = 0;
            logger.info("Crawling page: " + page_num);
            if (page == page_start) {
                json_source = getSharedJsonSource(br);
                if (json_source != null) {
                    isShared = true;
                } else {
                    isShared = false;
                    json_source = getJsonSource(this.br);
                }
            } else {
                final Regex urlinfo = new Regex(parameter, "https?://[^/]+/sh/([^/]+)/([^/]+)");
                final String link_key = urlinfo.getMatch(0);
                final String secure_hash = urlinfo.getMatch(1);
                String next_request_voucher = PluginJSonUtils.getJson(br, "next_request_voucher");
                if (StringUtils.isEmpty(next_request_voucher)) {
                    next_request_voucher = br.getRegex("next_request_voucher..\\s*:\\s*..(\\{.*?).\"\\}\"\\)\\}\\);").getMatch(0);
                }
                final String cookie_t = br.getCookie(getHost(), "t");
                if (StringUtils.isEmpty(next_request_voucher) || cookie_t == null || link_key == null || secure_hash == null) {
                    logger.warning("Failed to find more content than the first page");
                    break;
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.getHeaders().put("Origin", "https://www.dropbox.com");
                final Form pagination_form = new Form();
                pagination_form.setMethod(MethodType.POST);
                pagination_form.setAction("https://www.dropbox.com/list_shared_link_folder_entries");
                pagination_form.put("is_xhr", "true");
                pagination_form.put("link_key", link_key);
                pagination_form.put("link_type", "s");
                pagination_form.put("secure_hash", secure_hash);
                pagination_form.put("sub_path", "");
                next_request_voucher = PluginJSonUtils.unescape(next_request_voucher);
                next_request_voucher = PluginJSonUtils.unescape(next_request_voucher);
                // next_request_voucher = next_request_voucher.replaceAll("", "");
                pagination_form.put("voucher", Encoding.urlEncode(next_request_voucher));
                pagination_form.put("t", cookie_t);
                br.submitForm(pagination_form);
                json_source = br.toString();
            }
            if (json_source == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2017-01-27 new */
            boolean decryptSubfolders = crawl_subfolder_string != null && crawl_subfolder_string.contains("crawl_subfolders=true");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            final List<Object> ressourcelist_folders = getFoldersList(entries, isShared);
            final List<Object> ressourcelist_files = getFilesList(entries, isShared);
            final boolean isSingleFileInsideFolder = ressourcelist_files != null && ressourcelist_files.size() == 1 && (ressourcelist_folders == null || ressourcelist_folders.size() == 0);
            if (ressourcelist_folders != null && ressourcelist_folders.size() > 0 && !decryptSubfolders && !askedUserIfHeWantsSubfolders) {
                /*
                 * Only ask user if we actually have subfolders that can be decrypted AND if we have not asked him already for this folder
                 * AND if subfolders exist in this folder!
                 */
                final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, parameter, "For this URL JDownloader can crawl the files inside the current folder or crawl subfolders as well. What would you like to do?", null, "Add files of current folder AND subfolders?", "Add only files of current folder?") {
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
                    decryptSubfolders = true;
                } catch (DialogCanceledException e) {
                    decryptSubfolders = false;
                } catch (DialogClosedException e) {
                    decryptSubfolders = false;
                }
                askedUserIfHeWantsSubfolders = true;
            }
            if (ressourcelist_files != null) {
                current_numberof_items += ressourcelist_files.size();
                for (final Object o : ressourcelist_files) {
                    entries = (LinkedHashMap<String, Object>) o;
                    String url = (String) entries.get("href");
                    if (StringUtils.isEmpty(url) && isSingleFileInsideFolder) {
                        /* Fallback - this should never be required! */
                        url = parameter;
                    }
                    final String filename = (String) entries.get("filename");
                    final long filesize = JavaScriptEngineFactory.toLong(entries.get("bytes"), 0);
                    if (StringUtils.isEmpty(url) || StringUtils.isEmpty(filename)) {
                        return null;
                    }
                    final DownloadLink dl = createSingleDownloadLink(url);
                    if (dl == null) {
                        return null;
                    }
                    if (filesize > 0) {
                        dl.setDownloadSize(filesize);
                    }
                    if (!StringUtils.isEmpty(passCode)) {
                        dl.setDownloadPassword(passCode);
                        dl.setProperty(DropboxCom.PROPERTY_IS_PASSWORD_PROTECTED, true);
                        if (!StringUtils.isEmpty(password_cookie)) {
                            dl.setProperty(DropboxCom.PROPERTY_PASSWORD_COOKIE, password_cookie);
                        }
                    }
                    /*
                     * 2019-09-24: All URLs crawled via website crawler count as single files later on if we try to download them via API!
                     */
                    dl.setProperty(DropboxCom.PROPERTY_IS_SINGLE_FILE, true);
                    dl.setName(filename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
            if (ressourcelist_folders != null) {
                current_numberof_items += ressourcelist_folders.size();
                if (decryptSubfolders) {
                    for (final Object o : ressourcelist_folders) {
                        entries = (LinkedHashMap<String, Object>) o;
                        final boolean is_dir = ((Boolean) entries.get("is_dir")).booleanValue();
                        String url = (String) entries.get("href");
                        if (!is_dir || StringUtils.isEmpty(url)) {
                            continue;
                        }
                        url += "&crawl_subfolders=true";
                        final String name = (String) entries.get("filename");
                        final String currentPath;
                        if (StringUtils.isNotEmpty(name)) {
                            currentPath = subFolder + "/" + name;
                        } else {
                            currentPath = subFolder;
                        }
                        final DownloadLink subFolderDownloadLink = this.createDownloadlink(url, currentPath);
                        decryptedLinks.add(subFolderDownloadLink);
                    }
                }
            }
            hasMore = current_numberof_items >= website_max_items_per_page;
            page++;
        } while (hasMore && !this.isAbort());
        return decryptedLinks;
    }

    public static List<Object> getFoldersList(Map<String, Object> map, boolean isShared) {
        if (isShared) {
            final List<Object> entries = (List<Object>) JavaScriptEngineFactory.walkJson(map, "entries");
            final ArrayList<Object> ret = new ArrayList<Object>();
            for (final Object entry : entries) {
                if (Boolean.TRUE.equals(((Map<String, Object>) entry).get("is_dir"))) {
                    ret.add(entry);
                }
            }
            return ret;
        } else {
            if (!map.containsKey("props")) {
                map = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "components/{0}");
            }
            final List<Object> ret = (List<Object>) JavaScriptEngineFactory.walkJson(map, "props/contents/folders");
            return ret;
        }
    }

    public static List<Object> getFilesList(Map<String, Object> map, boolean isShared) {
        if (isShared) {
            final List<Object> entries = (List<Object>) JavaScriptEngineFactory.walkJson(map, "entries");
            final ArrayList<Object> ret = new ArrayList<Object>();
            for (final Object entry : entries) {
                if (!Boolean.TRUE.equals(((Map<String, Object>) entry).get("is_dir"))) {
                    ret.add(entry);
                }
            }
            return ret;
        } else {
            if (!map.containsKey("props")) {
                map = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "components/{0}");
            }
            List<Object> filesList = (List<Object>) JavaScriptEngineFactory.walkJson(map, "props/contents/files");
            /* Null? Then we probably have a single file */
            if (filesList == null) {
                filesList = (List<Object>) JavaScriptEngineFactory.walkJson(map, "props/files");
            }
            if (filesList == null) {
                // single file
                final Object file = JavaScriptEngineFactory.walkJson(map, "props/file");
                if (file != null) {
                    final ArrayList<Object> ret = new ArrayList<Object>();
                    ret.add(file);
                    return ret;
                }
            }
            return filesList;
        }
    }

    @Override
    protected DownloadLink createDownloadlink(final String link) {
        return createDownloadlink(link, this.subFolder);
    }

    protected DownloadLink createDownloadlink(final String link, final String subFolder) {
        final DownloadLink ret = super.createDownloadlink(link);
        if (!StringUtils.isEmpty(subFolder)) {
            ret.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
        }
        return ret;
    }

    public static String getSharedJsonSource(Browser br) {
        String json_source = br.getRegex("(\\s*\\{\\s*\\\\\"shared_link_infos.*?\\})\\s*\\)?\\s*;").getMatch(0);
        if (json_source != null) {
            json_source = json_source.replaceAll("\\\\\"", "\"");
            json_source = json_source.replaceAll("\\\\\\\"", "\"");// inner next_request_voucher
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

    private DownloadLink createSingleDownloadLink(final String parameter) {
        final String urlpart = new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0);
        if (urlpart == null) {
            return null;
        }
        final DownloadLink dl = createDownloadlink(String.format("https://dropboxdecrypted.com/%s", urlpart));
        if (!StringUtils.isEmpty(subFolder)) {
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
        }
        return dl;
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