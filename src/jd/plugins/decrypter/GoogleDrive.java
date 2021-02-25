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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drive.google.com" }, urls = { "https?://(?:www\\.)?drive\\.google\\.com/open\\?id=[a-zA-Z0-9\\-_]+|https?://(?:www\\.)?docs\\.google\\.com/folder/d/[a-zA-Z0-9\\-_]+|https?://(?:www\\.)?(?:docs|drive)\\.google\\.com/folderview\\?[a-z0-9\\-_=\\&]+|https?://(?:www\\.)?drive\\.google\\.com/drive/(?:[\\w\\-]+/)*folders/[a-zA-Z0-9\\-_=\\&]+" })
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
    private static final String TYPE_FOLDER_NORMAL  = "https?://(?:www\\.)?docs\\.google\\.com/folder/d/([a-zA-Z0-9\\-_]+)";
    private static final String TYPE_FOLDER_OLD     = "https?://(?:www\\.)?docs\\.google\\.com/folderview\\?(pli=1\\&id=[A-Za-z0-9_]+(\\&tid=[A-Za-z0-9]+)?|id=[A-Za-z0-9_]+\\&usp=sharing)";
    private static final String TYPE_FOLDER_CURRENT = "https?://[^/]+/drive/(?:[\\w\\-]+/)*folders/[^/]+";
    /* TODO: Add better handling for these */
    private static final String TYPE_REDIRECT       = "https?://[^/]+/open\\?id=([a-zA-Z0-9\\-_]+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (jd.plugins.hoster.GoogleDrive.canUseAPI()) {
            return this.crawlAPI(param);
        } else {
            return this.crawlWebsite(param);
        }
    }

    private String getFolderID(final String url) {
        if (url.matches(TYPE_FOLDER_NORMAL) || url.matches(TYPE_FOLDER_CURRENT)) {
            return new Regex(url, "([^/]+)$").getMatch(0);
        } else {
            return new Regex(url, "id=([^\\&=]+)").getMatch(0);
        }
    }

    private ArrayList<DownloadLink> crawlAPI(final CryptedLink param) throws Exception {
        final String folderID = getFolderID(param.getCryptedUrl());
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        int page = 0;
        do {
            logger.info("Working on pagination page " + (page + 1));
            br.getPage(jd.plugins.hoster.GoogleDrive.API_BASE + "/files?" + queryFolder.toString());
            ((jd.plugins.hoster.GoogleDrive) hostPlugin).handleErrorsAPI(br, null, account);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            /* 2020-12-10: Will return empty array for private items too! */
            if (!entries.containsKey("files") || ((List<Object>) entries.get("files")).size() == 0) {
                /* Check if we maybe got a single file instead of a folder */
                if (checkForSingleFile(this.br, param.getCryptedUrl())) {
                    decryptedLinks.add(this.createDownloadlink(br.getURL()));
                } else {
                    final String offlineFolderName;
                    if (!StringUtils.isEmpty(subfolderPath)) {
                        offlineFolderName = subfolderPath + " " + folderID;
                    } else {
                        offlineFolderName = folderID;
                    }
                    decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "EMPTY_OR_PRIVATE_FOLDER " + offlineFolderName, "EMPTY_OR_PRIVATE_FOLDER " + offlineFolderName));
                }
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
                    final Browser websiteBR = new Browser();
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
                    websiteBR.getPage(param.getCryptedUrl());
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
        String parameter = param.toString().replaceFirst("http:", "https:");
        final String folderID = this.getFolderID(param.getCryptedUrl());
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        /*
         * 2020-11-17: Crawling doesn't work anymore when user is logged in at this stage AND crawling of private folders was broken
         * anyways: https://svn.jdownloader.org/issues/88600
         */
        /* TODO: Allow login if account is given and API login is active */
        final boolean allowLogin = false;
        boolean loggedin = false;
        if (aa != null && allowLogin) {
            login(this.br, aa);
        } else {
            /* Respect users' plugin settings (e.g. custom User-Agent) */
            ((jd.plugins.hoster.GoogleDrive) hostPlugin).prepBrowser(this.br);
        }
        logger.info("LoggedIn:" + loggedin);
        if (folderID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (checkForSingleFile(this.br, parameter)) {
            decryptedLinks.add(this.createDownloadlink(br.getURL()));
            return decryptedLinks;
        }
        parameter = "https://drive.google.com/drive/folders/" + folderID;
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (parameter.matches(TYPE_FOLDER_NORMAL)) {
            br.getPage(parameter + "/edit?pli=1");
        } else {
            br.getPage(parameter);
        }
        //
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<p class=\"errorMessage\" style=\"padding-top: 50px\">Sorry, the file you have requested does not exist\\.</p>")) {
            final String offlineFolderName;
            if (!StringUtils.isEmpty(subfolderPath)) {
                offlineFolderName = subfolderPath + " " + folderID;
            } else {
                offlineFolderName = folderID;
            }
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "OFFLINE_FOLDER " + offlineFolderName, "OFFLINE_FOLDER " + offlineFolderName));
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
        if (results != null && results.length != 0) {
            /* Handle the json way. */
            final String key;
            final String key_json = br.getRegex("\\]([^\\[\\]]+),\"https://client\\-channel\\.google\\.com/client\\-channel/client\"").getMatch(0);
            if (key_json != null) {
                /* 2017-05-10: New - but this is still a bad solution! */
                final String[] keys = new Regex(key_json, "\"([A-Za-z0-9\\-_]{10,})\"").getColumn(0);
                key = keys[0];
                logger.info("Keys:" + Arrays.toString(keys));
                logger.info("Key:" + key);
            } else {
                /* Old fallback */
                key = br.getRegex("\"([A-Za-z0-9\\-_]+)\",{1,}1000,1,\"https?://client\\-channel\\.google\\.com/client\\-channel/client").getMatch(0);
                logger.info("KeyOld:" + key);
            }
            if (StringUtils.isEmpty(key)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // final String eof = br.getRegex("\\|eof\\|([^<>\"]*)\\\\x22").getMatch(0);
            final String teamDriveID = new Regex(json_src, ",null,\\d{10,},\\d+,\"([A-Za-z0-9]{10,30})\",null,null").getMatch(0);
            String nextPageToken = null;
            boolean firstRequest = true;
            int addedlinks;
            final int maxItemsPerPage = 50;
            do {
                addedlinks = 0;
                if (decryptedLinks.size() >= 50 || firstRequest) {
                    final Browser brc = br.cloneBrowser();
                    brc.addAllowedResponseCodes(400);
                    try {
                        sleep(500, param);
                        if (firstRequest) {
                            /* 2017-05-10: Updated these requests / URLs! */
                            /* Required to get the first "nextPageToken". */
                            if (teamDriveID != null) {
                                /* 2020-10-08 */
                                brc.getPage("https://clients6.google.com/drive/v2beta/files?openDrive=false&reason=102&syncType=0&errorRecovery=false&q=trashed%20%3D%20false%20and%20'" + folderID
                                        + "'%20in%20parents&fields=kind%2CnextPageToken%2Citems(kind%2CmodifiedDate%2CmodifiedByMeDate%2ClastViewedByMeDate%2CfileSize%2Cowners(kind%2CpermissionId%2CdisplayName%2Cpicture)%2ClastModifyingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2ChasThumbnail%2CthumbnailVersion%2Ctitle%2Cid%2Cshared%2CsharedWithMeDate%2CuserPermission(role)%2CexplicitlyTrashed%2CmimeType%2CquotaBytesUsed%2Ccopyable%2CfileExtension%2CsharingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2Cspaces%2Cversion%2CteamDriveId%2ChasAugmentedPermissions%2CcreatedDate%2CtrashingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2CtrashedDate%2Cparents(id)%2CshortcutDetails(targetId%2CtargetMimeType%2CtargetLookupStatus)%2Ccapabilities(canCopy%2CcanDownload%2CcanEdit%2CcanAddChildren%2CcanDelete%2CcanRemoveChildren%2CcanShare%2CcanTrash%2CcanRename%2CcanReadTeamDrive%2CcanMoveTeamDriveItem)%2Clabels(starred%2Ctrashed%2Crestricted%2Cviewed))%2CincompleteSearch&appDataFilter=NO_APP_DATA&spaces=drive&maxResults=50&supportsTeamDrives=true&includeTeamDriveItems=true&teamDriveId="
                                        + teamDriveID + "&corpora=teamDrive&orderBy=folder%2Ctitle_natural%20asc&retryCount=0&key=" + key);
                            } else {
                                brc.getPage("https://clients6.google.com/drive/v2beta/files?openDrive=true&reason=102&syncType=0&errorRecovery=false&q=trashed%20%3D%20false%20and%20'" + folderID
                                        + "'%20in%20parents&fields=kind%2CnextPageToken%2Citems(kind%2Ctitle%2CmimeType%2CcreatedDate%2CmodifiedDate%2CmodifiedByMeDate%2ClastViewedByMeDate%2CfileSize%2ClastModifyingUser(kind%2C%20displayName%2C%20picture%2C%20permissionId%2C%20emailAddress)%2ChasThumbnail%2CthumbnailVersion%2CiconLink%2Cid%2Cshared%2CsharedWithMeDate%2CuserPermission(role)%2CexplicitlyTrashed%2CquotaBytesUsed%2Cshareable%2Ccopyable%2CfileExtension%2CsharingUser(kind%2CdisplayName%2Cpicture%2CpermissionId%2CemailAddress)%2Cspaces%2Ceditable%2Cversion%2CteamDriveId%2ChasAugmentedPermissions%2CtrashingUser(kind%2CdisplayName%2Cpicture%2CpermissionId%2CemailAddress)%2CtrashedDate%2Cparents(id)%2Clabels(starred%2Chidden%2Ctrashed%2Crestricted%2Cviewed)%2Cowners(permissionId%2CdisplayName%2Cpicture%2Ckind)%2Ccapabilities(canCopy%2CcanDownload%2CcanEdit%2CcanAddChildren%2CcanDelete%2CcanRemoveChildren%2CcanShare%2CcanTrash%2CcanRename%2CcanReadTeamDrive%2CcanMoveTeamDriveItem))%2CincompleteSearch&appDataFilter=NO_APP_DATA&spaces=DRIVE&maxResults=50&orderBy=folder%2Ctitle%20asc&key="
                                        + key);
                            }
                            firstRequest = false;
                        } else {
                            if (teamDriveID != null) {
                                /* 2020-10-08 */
                                brc.getPage("https://clients6.google.com/drive/v2beta/files?openDrive=false&reason=102&syncType=0&errorRecovery=false&q=trashed%20%3D%20false%20and%20'" + folderID
                                        + "'%20in%20parents&fields=kind%2CnextPageToken%2Citems(kind%2CmodifiedDate%2CmodifiedByMeDate%2ClastViewedByMeDate%2CfileSize%2Cowners(kind%2CpermissionId%2CdisplayName%2Cpicture)%2ClastModifyingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2ChasThumbnail%2CthumbnailVersion%2Ctitle%2Cid%2Cshared%2CsharedWithMeDate%2CuserPermission(role)%2CexplicitlyTrashed%2CmimeType%2CquotaBytesUsed%2Ccopyable%2CfileExtension%2CsharingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2Cspaces%2Cversion%2CteamDriveId%2ChasAugmentedPermissions%2CcreatedDate%2CtrashingUser(kind%2CpermissionId%2CdisplayName%2Cpicture)%2CtrashedDate%2Cparents(id)%2CshortcutDetails(targetId%2CtargetMimeType%2CtargetLookupStatus)%2Ccapabilities(canCopy%2CcanDownload%2CcanEdit%2CcanAddChildren%2CcanDelete%2CcanRemoveChildren%2CcanShare%2CcanTrash%2CcanRename%2CcanReadTeamDrive%2CcanMoveTeamDriveItem)%2Clabels(starred%2Ctrashed%2Crestricted%2Cviewed))%2CincompleteSearch&appDataFilter=NO_APP_DATA&spaces=drive&maxResults=50&supportsTeamDrives=true&includeTeamDriveItems=true&teamDriveId="
                                        + teamDriveID + "&corpora=teamDrive&orderBy=folder%2Ctitle_natural%20asc&retryCount=0&key=" + key + "&pageToken=" + nextPageToken);
                            } else {
                                brc.getPage("https://clients6.google.com/drive/v2beta/files?openDrive=true&reason=102&syncType=0&errorRecovery=false&q=trashed%20%3D%20false%20and%20'" + folderID
                                        + "'%20in%20parents&fields=kind%2CnextPageToken%2Citems(kind%2Ctitle%2CmimeType%2CcreatedDate%2CmodifiedDate%2CmodifiedByMeDate%2ClastViewedByMeDate%2CfileSize%2ClastModifyingUser(kind%2C%20displayName%2C%20picture%2C%20permissionId%2C%20emailAddress)%2ChasThumbnail%2CthumbnailVersion%2CiconLink%2Cid%2Cshared%2CsharedWithMeDate%2CuserPermission(role)%2CexplicitlyTrashed%2CquotaBytesUsed%2Cshareable%2Ccopyable%2CfileExtension%2CsharingUser(kind%2CdisplayName%2Cpicture%2CpermissionId%2CemailAddress)%2Cspaces%2Ceditable%2Cversion%2CteamDriveId%2ChasAugmentedPermissions%2CtrashingUser(kind%2CdisplayName%2Cpicture%2CpermissionId%2CemailAddress)%2CtrashedDate%2Cparents(id)%2Clabels(starred%2Chidden%2Ctrashed%2Crestricted%2Cviewed)%2Cowners(permissionId%2CdisplayName%2Cpicture%2Ckind)%2Ccapabilities(canCopy%2CcanDownload%2CcanEdit%2CcanAddChildren%2CcanDelete%2CcanRemoveChildren%2CcanShare%2CcanTrash%2CcanRename%2CcanReadTeamDrive%2CcanMoveTeamDriveItem))%2CincompleteSearch&appDataFilter=NO_APP_DATA&spaces=DRIVE&maxResults=50&orderBy=folder%2Ctitle%20asc&key="
                                        + key + "&pageToken=" + nextPageToken);
                            }
                        }
                    } catch (final IOException e) {
                        logger.log(e);
                        break;
                    }
                    Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                    final ArrayList<Object> items = (ArrayList<Object>) entries.get("items");
                    if (items == null || items.isEmpty()) {
                        logger.info("break1");
                        break;
                    }
                    addedlinks = items.size();
                    /*
                     * TODO: Add parser for this json which will work via API and website (json is the same.) Also adjust "fields" value to
                     * return more information such as MD5 hash.
                     */
                    nextPageToken = (String) entries.get("nextPageToken");
                    parseFolderJsonWebsite(decryptedLinks, entries, subfolderPath, currentFolderTitle);
                    logger.info("added:" + addedlinks);
                    if (StringUtils.isEmpty(nextPageToken)) {
                        logger.info("break2");
                        /* Either we found everything or plugin failure ... */
                        break;
                    }
                }
                if (addedlinks < maxItemsPerPage) {
                    logger.info("Stopping because current page contains less than " + maxItemsPerPage + " elements");
                    break;
                }
            } while (key != null && !isAbort());
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Found nothing to download: " + parameter);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    /** Checks if a single URL leads to a single file- or a folder. */
    private boolean checkForSingleFile(final Browser br, final String url) throws PluginException, IOException {
        if (url.contains("open?id")) {
            br.setFollowRedirects(true);
            br.getPage(url);
            if (this.isSingleFileURL(br.getURL())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
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
            final DownloadLink dl;
            String folder_path = null;
            if (kind.contains("#file")) {
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
                    folder_path = subfolder;
                }
                if (folder_path != null) {
                    /*
                     * Packagizer property so user can e.g. merge all files of a folder and subfolders in a package named after the name of
                     * the root dir.
                     */
                    final String root_dir_name = new Regex(folder_path, "^/?([^/]+)").getMatch(0);
                    if (root_dir_name != null) {
                        dl.setProperty(jd.plugins.hoster.GoogleDrive.PROPERTY_ROOT_DIR, root_dir_name);
                    }
                }
            } else {
                /* Folder */
                if (subfolder != null) {
                    folder_path = subfolder + "/" + title;
                } else {
                    folder_path = "/" + title;
                }
                dl = createDownloadlink("https://drive.google.com/drive/folders/" + id);
            }
            if (folder_path != null) {
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folder_path);
            }
            decryptedLinks.add(dl);
        }
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
        /* TODO: Add FilePackage handling */
        final List<Object> items = (List<Object>) entries.get("files");
        for (final Object item : items) {
            entries = (Map<String, Object>) item;
            // kind within entries, returns false positives 20170709-raz
            final String kind = entries.get("mimeType") != null && ((String) entries.get("mimeType")).contains(".folder") ? "folder" : (String) entries.get("kind");
            final String title = (String) entries.get("name");
            final String id = (String) entries.get("id");
            if (StringUtils.isEmpty(kind) || StringUtils.isEmpty(title) || StringUtils.isEmpty(id)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dupes.contains(id)) {
                continue;
            }
            dupes.add(id);
            final DownloadLink dl;
            String folder_path = null;
            if (kind.contains("#file")) {
                /* Single file */
                dl = createDownloadlink("https://drive.google.com/file/d/" + id);
                jd.plugins.hoster.GoogleDrive.parseFileInfoAPI(dl, entries);
                if (subfolder != null) {
                    folder_path = subfolder;
                    /*
                     * Packagizer property so user can e.g. merge all files of a folder and subfolders in a package named after the name of
                     * the root dir.
                     */
                    final String root_dir_name = new Regex(folder_path, "^/?([^/]+)").getMatch(0);
                    if (root_dir_name != null) {
                        dl.setProperty(jd.plugins.hoster.GoogleDrive.PROPERTY_ROOT_DIR, root_dir_name);
                    }
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folder_path);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                }
            } else {
                /* Folder */
                if (subfolder != null) {
                    folder_path = subfolder + "/" + title;
                } else {
                    folder_path = "/" + title;
                }
                dl = createDownloadlink("https://drive.google.com/drive/folders/" + id);
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folder_path);
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