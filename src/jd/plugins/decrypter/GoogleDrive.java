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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drive.google.com" }, urls = { "https?://(?:www\\.)?drive\\.google\\.com/open\\?id=[a-zA-Z0-9\\-_]+|https?://(?:www\\.)?docs\\.google\\.com/folder/d/[a-zA-Z0-9\\-_]+|https?://(?:www\\.)?(?:docs|drive)\\.google\\.com/(?:embedded)?folderview\\?[a-z0-9\\-_=\\&]+|https?://(?:www\\.)?drive\\.google\\.com/drive/(?:[\\w\\-]+/)*folders/[a-zA-Z0-9\\-_=\\&]+(\\?resourcekey=[A-Za-z0-9_\\-]+)?" })
public class GoogleDrive extends PluginForDecrypt {
    /**
     * @author raztoki
     */
    public GoogleDrive(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES
    // https://docs.google.com/folder/d/0B4lNqBSBfg_dbEdISXAyNlBpLUk/edit?pli=1 :: folder view of dir and files, can't seem to view dir
    // unless edit present.
    // https://docs.google.com/folder/d/0B4lNqBSBfg_dOEVERmQzcU9LaWc/edit?pli=1&docId=0B4lNqBSBfg_deEpXNjJrZy1MSGM :: above sub dir of docs
    // they don't provide data constistantly.
    // - with /edit?pli=1 they provide via javascript section partly escaped
    // - with /list?rm=whitebox&hl=en_GB&forcehl=1&pref=2&pli=1"; - not used and commented out, supported except for scanLinks
    // language determined by the accept-language
    private static final String TYPE_FOLDER_NORMAL               = "https?://(?:www\\.)?docs\\.google\\.com/folder/d/([a-zA-Z0-9\\-_]+)";
    private static final String TYPE_FOLDER_OLD                  = "https?://(?:www\\.)?docs\\.google\\.com/(?:embedded)?folderview\\?(pli=1\\&id=[A-Za-z0-9_]+(\\&tid=[A-Za-z0-9]+)?|id=[A-Za-z0-9_]+\\&usp=sharing)";
    private static final String TYPE_FOLDER_CURRENT              = "https?://[^/]+/drive/(?:[\\w\\-]+/)*folders/([^/?]+)(\\?resourcekey=[A-Za-z0-9_\\-]+)?";
    /* 2021-02-26: Theoretically, "leaf?" does the same but for now we'll only handle "open=" as TYPE_REDIRECT */
    private static final String TYPE_REDIRECT                    = "https?://[^/]+/open\\?id=([a-zA-Z0-9\\-_]+)";
    private final String        PROPERTY_SPECIAL_SHORTCUT_FOLDER = "special_shortcut_folder";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("(?i)http://", "https://"));
        if (param.getCryptedUrl().matches(TYPE_REDIRECT)) {
            /**
             * Special case: This could either be a file or a folder. Other theoretically possible special cases which we will ignore here:
             * folderID in file URL --> Un-Downloadable item and can only be handled by API </br>
             *
             */
            logger.info("Checking if we have a file or folder");
            br.setFollowRedirects(false);
            int redirectCounter = 0;
            do {
                br.getPage(param.getCryptedUrl());
                if (br.getRedirectLocation() != null) {
                    if (this.isSingleFileURL(br.getRedirectLocation())) {
                        logger.info("Found single fileURL");
                        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                        ret.add(this.createDownloadlink(br.getRedirectLocation()));
                        return ret;
                    } else if (!br.getRedirectLocation().matches(TYPE_REDIRECT)) {
                        /* Must be folder */
                        break;
                    } else {
                        /* E.g. possible http -> https redirect */
                        br.followRedirect();
                    }
                } else {
                    break;
                }
            } while (br.getRedirectLocation() != null && redirectCounter <= 3);
            if (br.getRedirectLocation() != null && redirectCounter >= 3) {
                logger.warning("Possible redirectloop");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* Additional offline check */
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                ret.add(this.createOfflinelink(param.getCryptedUrl()));
                return ret;
            }
        }
        if (jd.plugins.hoster.GoogleDrive.canUseAPI()) {
            return this.crawlAPI(param);
        } else {
            return this.crawlWebsite(param);
        }
    }

    private String getFolderID(final String url) {
        if (url.matches(TYPE_FOLDER_NORMAL)) {
            return new Regex(url, TYPE_FOLDER_NORMAL).getMatch(0);
        } else if (url.matches(TYPE_FOLDER_CURRENT)) {
            return new Regex(url, TYPE_FOLDER_CURRENT).getMatch(0);
        } else {
            return new Regex(url, "id=([^\\&=]+)").getMatch(0);
        }
    }

    private String getFolderResourceKey(final String url) {
        try {
            return UrlQuery.parse(url).get("resourcekey");
        } catch (final Throwable ignore) {
            return null;
        }
    }

    private ArrayList<DownloadLink> crawlAPI(final CryptedLink param) throws Exception {
        String folderID = getFolderID(param.getCryptedUrl());
        final String folderResourceKey = this.getFolderResourceKey(param.getCryptedUrl());
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser websiteBR = new Browser();
        websiteBR.setFollowRedirects(true);
        if (param.getDownloadLink() != null && param.getDownloadLink().hasProperty(PROPERTY_SPECIAL_SHORTCUT_FOLDER)) {
            /**
             * 2021-05-31: Workaround for special folder shortcuts --> FolderID will change and we cannot use the given folderID via API!
             * </br>
             * Very rare case!!
             */
            websiteBR.getPage(param.getCryptedUrl());
            final String newFolderID = this.getFolderID(websiteBR.getURL());
            if (newFolderID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (!newFolderID.equals(folderID)) {
                logger.info("Folder redirected to new folder: Old: " + folderID + " | New: " + newFolderID);
                folderID = newFolderID;
            } else {
                /*
                 * This should never happen and chances are high that this object is either offline or API crawler will fail to process it.
                 */
                logger.warning("Expected to find new folderID but failed to do so");
            }
        }
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        final String subfolderPath = this.getAdoptedCloudFolderStructure();
        String nameOfCurrentFolder = null;
        final UrlQuery queryFolder = new UrlQuery();
        queryFolder.appendEncoded("q", "'" + folderID + "' in parents");
        queryFolder.add("supportsAllDrives", "true");
        queryFolder.add("includeItemsFromAllDrives", "true");
        /**
         * Returns up to 1000 items per request (default = 100). </br>
         * 2021-02-25: Appearently the GDrive API decides randomly how many items it wants to return but it doesn't matter as we got
         * pagination. It worked fine in my tests in their API explorer but in reality the max number of items I got was 30.
         */
        queryFolder.add("pageSize", "200");
        queryFolder.appendEncoded("fields", "kind,nextPageToken,incompleteSearch,files(" + jd.plugins.hoster.GoogleDrive.getFieldsAPI() + ")");
        /* API key for testing */
        queryFolder.appendEncoded("key", jd.plugins.hoster.GoogleDrive.getAPIKey());
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> dupes = new ArrayList<String>();
        if (folderResourceKey != null) {
            setResourceKeyHeader(br, folderID, folderResourceKey);
        }
        int page = 0;
        do {
            logger.info("Working on pagination page " + (page + 1));
            br.getPage(jd.plugins.hoster.GoogleDrive.API_BASE + "/files?" + queryFolder.toString());
            ((jd.plugins.hoster.GoogleDrive) hostPlugin).handleErrorsAPI(br, null, account);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            /* 2020-12-10: Will return empty array for private items too! */
            if (!entries.containsKey("files") || ((List<Object>) entries.get("files")).size() == 0) {
                final String offlineFolderName;
                if (!StringUtils.isEmpty(subfolderPath)) {
                    offlineFolderName = subfolderPath + " " + folderID;
                } else {
                    offlineFolderName = folderID;
                }
                /*
                 * 2021-05-31: TODO: Try to find a good(!) way to handle "folder shortcuts" via API completely without website (rare case)!
                 */
                final String offlineFilename = "EMPTY_OR_PRIVATE_OR_SHORTCUT_FOLDER " + offlineFolderName;
                final DownloadLink offlineFolder = this.createOfflinelink(param.getCryptedUrl());
                offlineFolder.setFinalFileName(offlineFilename);
                offlineFolder.setComment(offlineFilename + "\r\nIn case this folder is a folder shortcut: Disable API and re-add this URL.");
                decryptedLinks.add(offlineFolder);
                return decryptedLinks;
            }
            if (page == 0 && subfolderPath == null) {
                /* Leave this in the loop! It doesn't really belong here but it's only a workaround and only executed once! */
                logger.info("Trying to find title of current folder");
                try {
                    /**
                     * 2020-12-09: psp: This is a workaround because API doesn't return name of the current folder or I'm just too stupid
                     * ... </br>
                     * Basically for big folder structures we really only need to do this once and after that we'll use the API only!
                     */
                    /*
                     * TODO Login when API once API login is possible -> Then we'd be able to crawl private folders which are restricted to
                     * specified accounts.
                     */
                    // if (account != null) {
                    // login(websiteBR, account);
                    // } else {
                    // /* Respect users' plugin settings (e.g. custom User-Agent) */
                    // ((jd.plugins.hoster.GoogleDrive) hostPlugin).prepBrowser(websiteBR);
                    // }
                    if (websiteBR.getRequest() == null) {
                        /* Only access URL if it hasn't been accessed before */
                        websiteBR.getPage(param.getCryptedUrl());
                    }
                    nameOfCurrentFolder = getCurrentFolderTitleWebsite(websiteBR);
                    if (!StringUtils.isEmpty(nameOfCurrentFolder)) {
                        logger.info("Successfully found title of current folder: " + nameOfCurrentFolder);
                    } else {
                        logger.warning("Failed to find title of current folder");
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.info("Folder title workaround failed due to Exception");
                }
            }
            // if (page == 0) {
            // String parentFolderID = null;
            // try {
            // final List<Object> items = (List<Object>) entries.get("files");
            // for (final Object item : items) {
            // final Map<String, Object> itemInfo = (Map<String, Object>) item;
            // parentFolderID = (String) JavaScriptEngineFactory.walkJson(itemInfo, "parents/{0}");
            // break;
            // }
            // if (!StringUtils.isEmpty(parentFolderID)) {
            // final UrlQuery queryParentDir = queryFolder;
            // queryParentDir.remove("q");
            // queryParentDir.appendEncoded("q", "'" + parentFolderID + "' in parents");
            // final Browser brc = this.br.cloneBrowser();
            // brc.getPage(jd.plugins.hoster.GoogleDrive.API_BASE + "/files?" + queryFolder.toString());
            // final Map<String, Object> parentFolderInfo = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            // final List<Object> parentFolderItems = (List<Object>) parentFolderInfo.get("files");
            // for (final Object item : parentFolderItems) {
            // final Map<String, Object> itemInfo = (Map<String, Object>) item;
            // final String parentFolderItemID = (String) itemInfo.get("id");
            // if (StringUtils.equals(parentFolderItemID, parentFolderID)) {
            // nameOfCurrentFolder = (String) itemInfo.get("name");
            // break;
            // }
            // }
            // }
            // } catch (final Throwable e) {
            // logger.log(e);
            // logger.info("Failed to find name of current folder");
            // }
            // }
            final boolean incompleteSearch = ((Boolean) entries.get("incompleteSearch")).booleanValue();
            if (incompleteSearch) {
                /* This should never happen */
                logger.warning("WTF incompleteSearch == true");
            }
            final int dupesListOldSize = dupes.size();
            this.parseFolderJsonAPI(decryptedLinks, dupes, entries, this.getAdoptedCloudFolderStructure(), nameOfCurrentFolder);
            if (page == 0 && decryptedLinks.size() == 0) {
                /* Empty folder - 2nd check which usually won't be required as we're checking for this at the beginning of this function. */
                final String offlineFolderName;
                if (!StringUtils.isEmpty(subfolderPath)) {
                    offlineFolderName = subfolderPath + " " + folderID;
                } else {
                    offlineFolderName = folderID;
                }
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER " + offlineFolderName, "EMPTY_FOLDER " + offlineFolderName));
                return decryptedLinks;
            }
            final String nextPageToken = (String) entries.get("nextPageToken");
            if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because: nextPageToken is null");
                break;
            } else if (dupes.size() <= dupesListOldSize) {
                /* Fail safe */
                logger.info("Stopping because: Failed to find any new item on current page");
                break;
            } else {
                /* TODO: Check if this is needed or if add() replaces the previous value */
                // queryFolder.remove("pageToken");
                queryFolder.appendEncoded("pageToken", nextPageToken);
                page++;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlWebsite(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String folderID = this.getFolderID(param.getCryptedUrl());
        final String folderResourceKey = this.getFolderResourceKey(param.getCryptedUrl());
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        /*
         * 2020-11-17: Crawling doesn't work anymore when user is logged in at this stage AND crawling of private folders is broken anyways:
         * https://svn.jdownloader.org/issues/88600
         */
        boolean loggedin = false;
        final boolean allowLogin = false;
        if (account != null && allowLogin) {
            login(this.br, account);
        } else {
            /* Respect users' plugin settings (e.g. custom User-Agent) */
            ((jd.plugins.hoster.GoogleDrive) hostPlugin).prepBrowser(this.br);
        }
        logger.info("LoggedIn:" + loggedin);
        br.setFollowRedirects(true);
        /* 2021-05-31: Folders can redirect to other folderIDs. Most likely we got a "Shortcut" then --> Very rare case */
        final UrlQuery folderInitQuery = new UrlQuery();
        if (folderResourceKey != null) {
            folderInitQuery.add("resourcekey", folderResourceKey);
        }
        final String folderURL = "https://drive.google.com/drive/folders/" + folderID + "?" + folderInitQuery.toString();
        br.getPage(folderURL);
        final String newFolderID = this.getFolderID(br.getURL());
        if (newFolderID == null) {
            /*
             * Indication that something is wrong. Maybe offline or private folder - errorhandling down bewlow is supposed to find that out!
             */
            logger.warning("Failed to find folderID in current URL --> Possible crawler failure");
        } else if (!newFolderID.equals(folderID)) {
            logger.info("Folder redirected to new folder: Old: " + folderID + " | New: " + newFolderID);
            folderID = newFolderID;
        }
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        final String offlineOrEmptyFolderName;
        if (!StringUtils.isEmpty(subfolderPath)) {
            offlineOrEmptyFolderName = subfolderPath + " " + folderID;
        } else {
            offlineOrEmptyFolderName = folderID;
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<p class=\"errorMessage\" style=\"padding-top: 50px\">Sorry, the file you have requested does not exist\\.</p>")) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "OFFLINE_FOLDER " + offlineOrEmptyFolderName, "OFFLINE_FOLDER " + offlineOrEmptyFolderName));
            return decryptedLinks;
        }
        // login required!
        if (br.getURL().contains("//accounts.google.com/ServiceLogin?")) {
            // we are logged in but the account doesn't have permission
            if (loggedin) {
                throw new AccountRequiredException("This account doesn't have the required permission to access this folder");
            } else {
                throw new AccountRequiredException("You need an account to access URL");
            }
        }
        String currentFolderTitle = getCurrentFolderTitleWebsite(this.br);
        if (currentFolderTitle == null) {
            /* Final fallback */
            currentFolderTitle = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            if (!StringUtils.isEmpty(currentFolderTitle)) {
                currentFolderTitle = Encoding.htmlDecode(currentFolderTitle.trim()).trim();
            }
        }
        String[] results = null;
        // old type
        String json_src = br.getRegex("window\\['_DRIVE_ivd'\\]\\s*=\\s*'\\[(.*?)';").getMatch(0);
        if (json_src != null) {
            // json_src = JSonUtils.unescape(json_src);
            results = json_src.split("\\\\n,\\[\\\\x22");
        }
        // new type 20170709-raz
        if (json_src == null) {
            json_src = br.getRegex("window\\['_DRIVE_ivd'\\]\\s*=\\s*'(.*?)';").getMatch(0);
            // hex encoded
            if (json_src != null) {
                json_src = Encoding.unicodeDecode(json_src);
                results = json_src.split("\\][\r\n]+,\\[");
            }
        }
        /* Handle the json way. */
        String key = null;
        final String keys[] = br.getRegex("\"([A-Za-z0-9\\-_]{6})([A-Za-z0-9\\-_]+)\"\\s*,\\s*\"\\1[A-Za-z0-9\\-_]+\"\\s*,\\s*null").getRow(0);
        logger.info("Keys:" + Arrays.asList(keys));
        if (keys != null && keys.length == 2) {
            key = keys[0] + keys[1];
        }
        if (StringUtils.isEmpty(key)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Using key: " + key);
        // final String eof = br.getRegex("\\|eof\\|([^<>\"]*)\\\\x22").getMatch(0);
        final String teamDriveID = new Regex(json_src, ",null,\\d{10,},\\d+,\"([A-Za-z0-9_]{10,30})\",null,null").getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("openDrive", "false");
        query.add("reason", "102");
        query.add("syncType", "0");
        query.add("errorRecovery", "false");
        query.add("q", URLEncode.encodeURIComponent("trashed = false and '" + folderID + "' in parents"));
        query.add("fields", URLEncode.encodeURIComponent("kind,nextPageToken,items(kind,modifiedDate,modifiedByMeDate,lastViewedByMeDate,fileSize,owners(kind,permissionId,displayName,picture),lastModifyingUser(kind,permissionId,displayName,picture),hasThumbnail,thumbnailVersion,title,id,shared,sharedWithMeDate,userPermission(role),explicitlyTrashed,mimeType,quotaBytesUsed,copyable,fileExtension,sharingUser(kind,permissionId,displayName,picture),spaces,version,teamDriveId,hasAugmentedPermissions,createdDate,trashingUser(kind,permissionId,displayName,picture),trashedDate,parents(id),shortcutDetails(targetId,targetMimeType,targetLookupStatus),capabilities(canCopy,canDownload,canEdit,canAddChildren,canDelete,canRemoveChildren,canShare,canTrash,canRename,canReadTeamDrive,canMoveTeamDriveItem),labels(starred,trashed,restricted,viewed)),incompleteSearch"));
        query.add("appDataFilter", "NO_APP_DATA");
        query.add("spaces", "drive");
        query.add("maxResults", "50");
        query.add("orderBy", "folder%2Ctitle_natural%20asc");
        // query.add("retryCount", "0");
        query.add("key", key);
        if (folderResourceKey != null) {
            query.add("resourcekey", folderResourceKey);
        }
        /* Optional params (team drives only) */
        if (teamDriveID != null) {
            query.add("supportsTeamDrives", "true");
            query.add("includeTeamDriveItems", "true");
            query.add("teamDriveId", teamDriveID);
            query.add("corpora", "teamDrive");
        }
        String nextPageToken = null;
        int page = 0;
        final Browser brc = br.cloneBrowser();
        brc.addAllowedResponseCodes(new int[] { 400 });
        if (loggedin) {
            /* TODO: This doesn't work! */
            final String sapisid = br.getCookie(br.getHost(), "SAPISID");
            final String auth = "SAPISIDHASH " + System.currentTimeMillis() * 1000 + "_" + JDHash.getSHA1(sapisid) + "_u";
            brc.getHeaders().put("Authorization", auth);
        }
        if (folderResourceKey != null) {
            setResourceKeyHeader(brc, folderID, folderResourceKey);
        }
        do {
            page++;
            logger.info("Crawling page: " + page);
            sleep(500, param);
            /* Most common reason of failure: teamDriveID was not found thus the request is wrong! */
            brc.getPage("https://clients6.google.com/drive/v2beta/files?" + query.toString());
            Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            final List<Object> items = (List<Object>) entries.get("items");
            if (items == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (items.isEmpty()) {
                if (decryptedLinks.isEmpty()) {
                    decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER " + offlineOrEmptyFolderName, "EMPTY_FOLDER " + offlineOrEmptyFolderName));
                } else {
                    break;
                }
            }
            nextPageToken = (String) entries.get("nextPageToken");
            parseFolderJsonWebsite(decryptedLinks, entries, subfolderPath, currentFolderTitle);
            logger.info("Items current page: " + items.size() + " | Total: " + decryptedLinks.size());
            if (StringUtils.isEmpty(nextPageToken)) {
                /* Either we found everything or plugin failure ... */
                logger.info("Stopping because: Failed to find nextPageToken");
                break;
            } else if (items.isEmpty()) {
                logger.info("Stopping because: Failed to find any items on current page");
                break;
            } else {
                query.addAndReplace("pageToken", Encoding.urlEncode(nextPageToken));
            }
        } while (key != null && !isAbort());
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + param.getCryptedUrl());
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    public static void setResourceKeyHeader(final Browser br, final String resourceID, final String resourceKey) {
        /* https://developers.google.com/drive/api/v3/resource-keys */
        br.getHeaders().put("X-Goog-Drive-Resource-Keys", resourceID + "/" + resourceKey);
    }

    private static String getCurrentFolderTitleWebsite(final Browser br) {
        String currentFolderTitle = br.getRegex("\"title\":\"([^\"]+)\",\"urlPrefix\"").getMatch(0);
        if (currentFolderTitle == null) {
            currentFolderTitle = br.getRegex("<title>([^<>\"]*?) - Google Drive</title>").getMatch(0);
            if (currentFolderTitle == null) {
                currentFolderTitle = br.getRegex("<title>([^<>\"]*?) – Google Drive</title>").getMatch(0);
            }
        }
        if (!StringUtils.isEmpty(currentFolderTitle)) {
            currentFolderTitle = Encoding.htmlDecode(currentFolderTitle.trim()).trim();
            return currentFolderTitle;
        } else {
            return null;
        }
    }

    /**
     * There are differences between website- and API json e.g. we cannot request all fields we can get via API from website and the
     * filesize field is "fileSize" via website and "size" via API.
     *
     * @throws PluginException
     */
    private void parseFolderJsonWebsite(final ArrayList<DownloadLink> decryptedLinks, Map<String, Object> entries, String subfolder, final String currentFolderTitle) throws PluginException {
        if (!StringUtils.isEmpty(currentFolderTitle) && StringUtils.isEmpty(subfolder)) {
            /* Begin subfolder structure if not given already */
            subfolder = currentFolderTitle;
        }
        FilePackage fp = null;
        if (subfolder != null) {
            fp = FilePackage.getInstance();
            fp.setName(subfolder);
        } else if (currentFolderTitle != null) {
            fp = FilePackage.getInstance();
            fp.setName(currentFolderTitle);
        }
        final List<Object> items = (List<Object>) entries.get("items");
        for (final Object item : items) {
            entries = (Map<String, Object>) item;
            // kind within entries, returns false positives 20170709-raz
            final String mimeType = (String) entries.get("mimeType");
            final String kind = mimeType != null && mimeType.contains(".folder") ? "folder" : (String) entries.get("kind");
            final String title = (String) entries.get("title");
            final String id = (String) entries.get("id");
            if (kind == null || title == null || id == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean canDownload = true;
            try {
                final Map<String, Object> capabilities = (Map<String, Object>) entries.get("capabilities");
                canDownload = ((Boolean) capabilities.get("canDownload")).booleanValue();
            } catch (final Throwable e) {
            }
            /* 2021-05-31: application/vnd.google-apps.shortcut is (or can be??) a folder shortcut */
            final boolean isShortcutFolder = StringUtils.equalsIgnoreCase(mimeType, "application/vnd.google-apps.shortcut") && !canDownload;
            final boolean isFile = kind.equals("drive#file") && !isShortcutFolder;
            final DownloadLink dl;
            String folderPath = null;
            if (isFile) {
                /* Single file */
                final long fileSize = JavaScriptEngineFactory.toLong(entries.get("fileSize"), 0);
                /* Single file */
                dl = createDownloadlink("https://drive.google.com/file/d/" + id);
                final String googleDriveDocumentType = new Regex(mimeType, "application/vnd\\.google-apps\\.(.+)").getMatch(0);
                if (googleDriveDocumentType != null) {
                    jd.plugins.hoster.GoogleDrive.parseGoogleDocumentProperties(dl, title, googleDriveDocumentType, null);
                } else {
                    dl.setName(title);
                }
                if (fileSize > 0) {
                    dl.setDownloadSize(fileSize);
                    dl.setVerifiedFileSize(fileSize);
                }
                dl.setAvailable(true);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                if (subfolder != null) {
                    folderPath = subfolder;
                }
                if (folderPath != null) {
                    /*
                     * Packagizer property so user can e.g. merge all files of a folder and subfolders in a package named after the name of
                     * the root dir.
                     */
                    final String root_dir_name = new Regex(folderPath, "^/?([^/]+)").getMatch(0);
                    if (root_dir_name != null) {
                        dl.setProperty(jd.plugins.hoster.GoogleDrive.PROPERTY_ROOT_DIR, root_dir_name);
                    }
                }
                distribute(dl);
            } else {
                /* Folder */
                if (subfolder != null) {
                    folderPath = subfolder + "/" + title;
                } else {
                    folderPath = "/" + title;
                }
                dl = createDownloadlink("https://drive.google.com/drive/folders/" + id);
            }
            if (folderPath != null) {
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folderPath);
            }
            if (isShortcutFolder) {
                dl.setProperty(PROPERTY_SPECIAL_SHORTCUT_FOLDER, true);
            }
            decryptedLinks.add(dl);
        }
    }

    private String generateFileURL(final String fileID, final String resourceKey) {
        String url = "https://drive.google.com/file/d/" + fileID;
        if (resourceKey != null) {
            url += "?resourcekey=" + resourceKey;
        }
        return url;
    }

    private String generateFolderURL(final String folderID, final String folderResourceKey) {
        String url = "https://drive.google.com/drive/folders/" + folderID;
        if (folderResourceKey != null) {
            url += "?resourcekey=" + folderResourceKey;
        }
        return url;
    }

    private void parseFolderJsonAPI(final ArrayList<DownloadLink> decryptedLinks, final ArrayList<String> dupes, Map<String, Object> entries, String subfolder, final String currentFolderTitle) throws PluginException {
        if (!StringUtils.isEmpty(currentFolderTitle) && StringUtils.isEmpty(subfolder)) {
            /* Begin subfolder structure if not given already */
            subfolder = currentFolderTitle;
        }
        FilePackage fp = null;
        /* 2020-12-07: Workaround: Use path as packagename as long as we're unable to get the name of the folder we're currently in! */
        if (subfolder != null) {
            fp = FilePackage.getInstance();
            fp.setName(subfolder);
        } else if (currentFolderTitle != null) {
            fp = FilePackage.getInstance();
            fp.setName(currentFolderTitle);
        }
        final List<Object> items = (List<Object>) entries.get("files");
        for (final Object item : items) {
            entries = (Map<String, Object>) item;
            final String mimeType = (String) entries.get("mimeType");
            final String kind = mimeType != null && mimeType.contains(".folder") ? "folder" : (String) entries.get("kind");
            final String title = (String) entries.get("name");
            final String id = (String) entries.get("id");
            final String resourceKey = (String) entries.get("resourceKey");
            if (StringUtils.isEmpty(kind) || StringUtils.isEmpty(title) || StringUtils.isEmpty(id)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dupes.contains(id)) {
                continue;
            }
            dupes.add(id);
            boolean canDownload = true;
            try {
                final Map<String, Object> capabilities = (Map<String, Object>) entries.get("capabilities");
                canDownload = ((Boolean) capabilities.get("canDownload")).booleanValue();
            } catch (final Throwable e) {
            }
            /* 2021-05-31: application/vnd.google-apps.shortcut is (or can be??) a folder shortcut */
            final boolean isShortcutFolder = StringUtils.equalsIgnoreCase(mimeType, "application/vnd.google-apps.shortcut") && !canDownload;
            final boolean isFile = kind.equals("drive#file") && !isShortcutFolder;
            final DownloadLink dl;
            String folderPath = null;
            if (isFile) {
                /* Single file */
                dl = createDownloadlink(generateFileURL(id, resourceKey));
                jd.plugins.hoster.GoogleDrive.parseFileInfoAPI(dl, entries);
                if (subfolder != null) {
                    folderPath = subfolder;
                    /*
                     * Packagizer property so user can e.g. merge all files of a folder and subfolders in a package named after the name of
                     * the root dir.
                     */
                    final String root_dir_name = new Regex(folderPath, "^/?([^/]+)").getMatch(0);
                    if (root_dir_name != null) {
                        dl.setProperty(jd.plugins.hoster.GoogleDrive.PROPERTY_ROOT_DIR, root_dir_name);
                    }
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folderPath);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                }
            } else {
                /* Folder */
                if (subfolder != null) {
                    folderPath = subfolder + "/" + title;
                } else {
                    folderPath = "/" + title;
                }
                dl = createDownloadlink(generateFolderURL(id, resourceKey));
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folderPath);
                if (isShortcutFolder) {
                    dl.setProperty(PROPERTY_SPECIAL_SHORTCUT_FOLDER, true);
                }
            }
            decryptedLinks.add(dl);
            this.distribute(dl);
        }
    }

    private boolean isSingleFileURL(final String url) throws PluginException {
        final PluginForHost hostPlg = this.getNewPluginForHostInstance(this.getHost());
        return hostPlg.canHandle(url);
    }

    public void login(final Browser br, final Account account) throws Exception {
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        plg.setBrowser(br);
        ((jd.plugins.hoster.GoogleDrive) plg).login(this.br, account, false);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}