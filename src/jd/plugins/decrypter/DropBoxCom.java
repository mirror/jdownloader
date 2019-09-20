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
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dropbox.com" }, urls = { "https?://(?:www\\.)?dropbox\\.com/(?:(?:sh|sc|s)/[^<>\"]+|l/[A-Za-z0-9]+).*|https?://(www\\.)?db\\.tt/[A-Za-z0-9]+|https?://dl\\.dropboxusercontent\\.com/s/.+" })
public class DropBoxCom extends PluginForDecrypt {
    public DropBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DropBoxConfig.class;
    }

    private static final String TYPE_NORMAL         = "https?://(www\\.)?dropbox\\.com/(sh|sc)/.+";
    private static final String TYPE_S_AND_SH       = "https?://[^/]+/((?:s|sh)/.+)";
    /** 2019-09-20: TODO: Find out what 'sc' means - seems like photo albums */
    private static final String TYPE_SC             = "https?://[^/]+/sc/.+";
    private static final String TYPE_REDIRECT       = "https?://(www\\.)?dropbox\\.com/l/[A-Za-z0-9]+";
    private static final String TYPE_SHORT          = "https://(www\\.)?db\\.tt/[A-Za-z0-9]+";
    /* Unsupported linktypes which can occur during the decrypt process */
    /* 2019-09-20: Some time ago, these were direct-URLs. Now not anymore. */
    private static final String TYPE_DIRECTLINK_OLD = "https?://dl\\.dropboxusercontent.com/s/(.+)";
    private static final String TYPE_REFERRAL       = "https?://(www\\.)?dropbox\\.com/referrals/.+";
    private String              subFolder           = "";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        subFolder = getAdoptedCloudFolderStructure();
        if (subFolder == null) {
            subFolder = "";
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = correctAddedURL(param.toString());
        final Account account = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        final boolean canUseAPI = jd.plugins.hoster.DropboxCom.setAPILoginHeaders(this.br, account);
        if (canUseAPI && jd.plugins.hoster.DropboxCom.useAPI()) {
            /**
             * 2019-09-19: TODO: Check if there is a way to use this part of their API without logging in e.g. general authorization header
             * provided by our official Dropbox developer account! Then make sure we do not run into some kind of rate-limit!
             */
            jd.plugins.hoster.DropboxCom.prepBrAPI(this.br);
            jd.plugins.hoster.DropboxCom.setAPILoginHeaders(this.br, account);
            decryptedLinks.addAll(crawlViaAPI(parameter));
        } else {
            decryptedLinks.addAll(crawlViaWebsite(parameter));
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + parameter);
            final DownloadLink dl = this.createOfflinelink(parameter);
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

    private ArrayList<DownloadLink> crawlViaAPI(final String parameter) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /**
         * TODO: Get and set name of current folder, check pagination, check subfolder-decryption, check subfolder-paths!
         */
        /*
         * We cannot use the following request because we do not have the folderID at this stage:
         * https://www.dropbox.com/developers/documentation/http/documentation#sharing-get_folder_metadata
         */
        /** https://www.dropbox.com/developers/documentation/http/documentation#sharing-get_shared_link_metadata */
        /** TODO: Add path and link_pasword fields */
        /* To access crawled subfolders, we need the same URL as before but a different 'path' value! */
        final String last_path = getAdoptedCloudFolderStructure();
        /* Just a 2nd variable to make it clear where we started! */
        final boolean is_root;
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
        br.postPageRaw(jd.plugins.hoster.DropboxCom.API_BASE + "/sharing/get_shared_link_metadata", "{\"url\":\"" + parameter + "\",\"path\":" + path + "}");
        ArrayList<Object> ressourcelist = new ArrayList<Object>();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String error_summary = jd.plugins.hoster.DropboxCom.getErrorSummaryField(this.br);
        final String object_type = (String) entries.get(".tag");
        if (!StringUtils.isEmpty(error_summary)) {
            /* 2019-09-198: Typically response 409 with error_summary 'shared_link_access_denied/..' */
            logger.info("Decryption failed because: " + error_summary);
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
            /* Single file - rare case! */
            ressourcelist.add(entries);
        } else {
            /* Assume we have a folder */
            /* path_lower = FULL path including current folder - lowercase */
            // subFolder = (String) entries.get("path_lower");
            if (path.equals("/") || path.equals("\"/\"")) {
                /*
                 * 2019-09-20: For this call, "/" is not accepted to get the root folder although their API 'knows what we want'. This is
                 * really strange but okay. Error when used with wrong value: "Error in call to API function "files/
                 * list_folder": request body: path: Specify the root folder as an empty string rather than as "/"."
                 */
                path = "\"\"";
            }
            br.postPageRaw(jd.plugins.hoster.DropboxCom.API_BASE + "/files/list_folder", "{\"path\":" + path + ",\"shared_link\": {\"url\":\"" + parameter + "\"}}");
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
                /**
                 * TODO: Website uses something called 'secureHash' for their public URLs - I was not able to find this anywhere in their
                 * API!
                 */
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
                        /* E.g. /<rootFolder>/subfolder1/subfolder2/filename.ext --> We need /subfolder1/subfolder2/filename.ext */
                        serverside_path_to_file_relative = new Regex(serverside_path_full, "(?:/[^/]+)?(.+)$").getMatch(0);
                    }
                    if (StringUtils.isEmpty(serverside_path_to_file_relative)) {
                        /* Fallback - This should never happen! */
                        serverside_path_to_file_relative = serverside_path_full;
                    }
                    if (!StringUtils.isEmpty(serverside_path_to_file_relative) && !is_single_file) {
                        dl.setProperty("serverside_path_to_file_relative", serverside_path_to_file_relative);
                    }
                    dl.setProperty("mainlink", parameter);
                    // dl.setProperty("serverside_path_full", serverside_path_full);
                    dl.setLinkID(this.getHost() + "://" + id);
                    /**
                     * 2019-09-20: TODO: Find out if it is possible to generate URLs which lead to the exact files. At the moment this is a
                     * huge issue when using their API - it contains other fileIDs so we cannot get to the public contentURLs although they
                     * do exist!
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
        } while (has_more && !StringUtils.isEmpty(cursor));
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlViaWebsite(String link) throws Exception {
        br.setFollowRedirects(false);
        br.setCookie("https://dropbox.com", "locale", "en");
        br.setLoadLimit(br.getLoadLimit() * 4);
        final String crawl_subfolder_string = new Regex(link, "(\\&crawl_subfolders=(?:true|false))").getMatch(0);
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
        link = link.replaceAll("\\?dl=\\d", "");
        if (crawl_subfolder_string != null) {
            link = link.replace(crawl_subfolder_string, "");
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link);
            if (con.getResponseCode() == 429) {
                try {
                    con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                logger.info("URL's downloads are disabled due to it generating too much traffic");
                return decryptedLinks;
            } else if (con.getResponseCode() == 460) {
                try {
                    con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                logger.info("Restricted Content: This file is no longer available. For additional information contact Dropbox Support.");
                return decryptedLinks;
            } else if (con.getResponseCode() == 509) {
                try {
                    con.setAllowedResponseCodes(new int[] { con.getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                /* Temporarily unavailable links */
                final DownloadLink dl = createDownloadlink(link.replace("dropbox.com/", "dropboxdecrypted.com/"));
                dl.setProperty("decrypted", true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (con.getResponseCode() == 302 && (link.matches(TYPE_REDIRECT) || link.matches(TYPE_SHORT))) {
                link = br.getRedirectLocation();
                if (link.matches(TYPE_DIRECTLINK_OLD)) {
                    final DownloadLink direct = createDownloadlink("directhttp://" + link);
                    decryptedLinks.add(direct);
                    return decryptedLinks;
                } else if (link.matches(TYPE_S_AND_SH)) {
                    decryptedLinks.add(createSingleDownloadLink(link));
                    return decryptedLinks;
                } else if (link.matches(TYPE_REFERRAL)) {
                    final DownloadLink dl = this.createOfflinelink(link);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                } else if (!link.matches(TYPE_NORMAL)) {
                    logger.warning("Decrypter broken or unsupported redirect-url: " + link);
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
                final DownloadLink dl = this.createOfflinelink(link);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Throwable e) {
            }
        }
        /* Decrypt file- and folderlinks */
        String fpName = br.getRegex("content=\"([^<>/]*?)\" property=\"og:title\"").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
        }
        if (fpName != null) {
            if (fpName.contains("\\")) {
                fpName = Encoding.unicodeDecode(fpName);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            currentPackage.set(fp);
            if (StringUtils.isEmpty(subFolder) && PluginJsonConfig.get(DropBoxConfig.class).isIncludeRootSubfolder()) {
                subFolder = Encoding.htmlDecode(fpName.trim());
            }
        }
        // /*
        // * 2017-01-27: This does not work anymore - also their .zip downloads often fail so rather not do this!Decrypt "Download as zip"
        // * link if available and wished by the user
        // */
        // if (br.containsHTML(">Download as \\.zip<") && PluginJsonConfig.get(DropboxConfig.class).isZipFolderDownloadEnabled()) {
        // final DownloadLink dl = createDownloadlink(link.replace("dropbox.com/", "dropboxdecrypted.com/"));
        // dl.setName(fpName + ".zip");
        // dl.setProperty("decrypted", true);
        // dl.setProperty("type", "zip");
        // dl.setProperty("directlink", link.replaceAll("\\?dl=\\d", "") + "?dl=1");
        // dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
        // decryptedLinks.add(dl);
        // }
        boolean hasMore = false;
        boolean isShared = false;
        boolean askedUserIfHeWantsSubfolders = false;
        final int page_start = 1;
        int page = page_start;
        String json_source = null;
        do {
            if (page == page_start) {
                json_source = getSharedJsonSource(br);
                if (json_source != null) {
                    isShared = true;
                } else {
                    isShared = false;
                    json_source = getJsonSource(this.br);
                }
            } else {
                /** TODO: Fix this */
                if (true) {
                    break;
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                br.getHeaders().put("Origin", "https://www.dropbox.com");
                br.postPage("https://www.dropbox.com/list_shared_link_folder_entries", "");
                json_source = br.toString();
            }
            if (json_source == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2017-01-27 new */
            boolean decryptSubfolders = crawl_subfolder_string != null && crawl_subfolder_string.contains("crawl_subfolders=true");
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            final List<Object> ressourcelist_folders = getFoldersList(entries, isShared);
            final List<Object> ressourcelist_files = getFilesList(entries, isShared);
            final boolean isSingleFile = ressourcelist_files != null && ressourcelist_files.size() == 1;
            if (ressourcelist_folders != null && ressourcelist_folders.size() > 0 && !decryptSubfolders && !askedUserIfHeWantsSubfolders) {
                /* Only ask user if we actually have subfolders that can be decrypted! */
                final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, link, "For this URL JDownloader can crawl the files inside the current folder or crawl subfolders as well. What would you like to do?", null, "Add files of current folder AND subfolders?", "Add only files of current folder?") {
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
                for (final Object o : ressourcelist_files) {
                    entries = (LinkedHashMap<String, Object>) o;
                    String url = (String) entries.get("href");
                    if (url == null && isSingleFile) {
                        url = link;
                    }
                    final String filename = (String) entries.get("filename");
                    final long filesize = JavaScriptEngineFactory.toLong(entries.get("bytes"), 0);
                    if (url == null || url.equals("") || filename == null || filename.equals("")) {
                        return null;
                    }
                    final DownloadLink dl = createSingleDownloadLink(url);
                    if (filesize > 0) {
                        dl.setDownloadSize(filesize);
                    }
                    dl.setName(filename);
                    dl.setAvailable(true);
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
                    decryptedLinks.add(dl);
                }
            }
            if (decryptSubfolders) {
                final String subFolderBase = subFolder;
                for (final Object o : ressourcelist_folders) {
                    entries = (LinkedHashMap<String, Object>) o;
                    final boolean is_dir = ((Boolean) entries.get("is_dir")).booleanValue();
                    String url = (String) entries.get("href");
                    if (!is_dir || url == null || url.equals("")) {
                        continue;
                    }
                    url += "&crawl_subfolders=true";
                    final String name = (String) entries.get("filename");
                    if (StringUtils.isNotEmpty(name)) {
                        subFolder = subFolderBase + "/" + name;
                    } else {
                        subFolder = subFolderBase;
                    }
                    final DownloadLink subFolderDownloadLink = this.createDownloadlink(url);
                    decryptedLinks.add(subFolderDownloadLink);
                }
            }
            page++;
        } while (hasMore);
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
        String json_source = br.getRegex("InitReact\\.mountComponent\\(mod,\\s*?(\\{.*?\\})\\)").getMatch(0);
        if (json_source == null) {
            json_source = br.getRegex("mod\\.initialize_module\\((\\{\"components\".*?)\\);\\s+").getMatch(0);
            if (json_source == null) {
                json_source = br.getRegex("mod\\.initialize_module\\((\\{.*?)\\);\\s+").getMatch(0);
            }
        }
        return json_source;
    }

    private DownloadLink createSingleDownloadLink(final String parameter) {
        final String urlpart = new Regex(parameter, TYPE_S_AND_SH).getMatch(0);
        final DownloadLink dl = createDownloadlink(String.format("https://dropboxdecrypted.com/%s", urlpart));
        dl.setProperty("decrypted", true);
        if (!StringUtils.isEmpty(subFolder)) {
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
        }
        return dl;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}