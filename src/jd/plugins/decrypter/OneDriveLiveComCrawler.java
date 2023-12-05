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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.plugins.PluginForHost;
import jd.plugins.hoster.OneDriveLiveCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onedrive.live.com" }, urls = { "https?://([a-zA-Z0-9\\-]+\\.)?(onedrive\\.live\\.com/.+|skydrive\\.live\\.com/.+|(sdrv|1drv)\\.ms/[A-Za-z0-9&!=#\\.,-_]+)" })
public class OneDriveLiveComCrawler extends PluginForDecrypt {
    public OneDriveLiveComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    private String        cid         = null;
    private String        resource_id = null;
    private String        authkey     = null;
    private PluginForHost hostPlugin  = null;

    private void ensureInitHosterplugin() throws PluginException {
        if (this.hostPlugin == null) {
            this.hostPlugin = getNewPluginForHostInstance("onedrive.live.com");
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        setGlobalVars(contenturl);
        if (cid == null || resource_id == null) {
            /* Possibly a short-URL which redirects to another URL which should contain the parameters we are looking for. */
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String newURL = br.getURL();
            if (newURL.equalsIgnoreCase(contenturl)) {
                /* URL hasn't changed */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!this.canHandle(newURL)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            setGlobalVars(newURL);
            contenturl = newURL;
        }
        if (cid == null || resource_id == null) {
            /* Invalid URL */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (authkey != null) {
            authkey = Encoding.htmlDecode(authkey);
        }
        cid = cid.toUpperCase(Locale.ENGLISH);
        contenturl = this.generateFolderLink(cid, resource_id, authkey);
        boolean useNewAPI = DebugMode.TRUE_IN_IDE_ELSE_FALSE;
        // useNewAPI = false;
        if (useNewAPI) {
            final UrlQuery rootquerypost = new UrlQuery();
            rootquerypost.add("%24select", "*%2CsharepointIds%2CwebDavUrl%2CcontainingDrivePolicyScenarioViewpoint");
            rootquerypost.add("24expand", "thumbnails");
            rootquerypost.add("ump", "1");
            if (authkey != null) {
                rootquerypost.add("authKey", Encoding.urlEncode(authkey));
            }
            br.getHeaders().put("Origin", "https://onedrive.live.com");
            br.getHeaders().put("Referer", "https://onedrive.live.com/");
            /* Browser is using POST request, we are using GET to keep things simple. */
            br.getPage("https://api.onedrive.com/v1.0/drives/" + cid.toLowerCase(Locale.ENGLISH) + "/items/" + resource_id + "?" + rootquerypost);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> resourceinfo = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> foldermap = (Map<String, Object>) resourceinfo.get("folder");
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (foldermap != null) {
                /* Crawl all items of a folder */
                final long size = ((Number) resourceinfo.get("size")).longValue();
                if (size == 0) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                }
                String path = resourceinfo.get("pathFromRoot").toString();
                path = Encoding.htmlDecode(path).trim();
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(path);
                fp.setPackageKey("onedrive://folder/" + cid);
                int page = 1;
                final HashSet<String> dupes = new HashSet<String>();
                final int maxItemsPerPage = 100;
                final UrlQuery querypagination = new UrlQuery();
                querypagination.add("%24top", Integer.toString(maxItemsPerPage));
                querypagination.add("%24expand", "");
                querypagination.add("select", "*%2Cocr%2CwebDavUrl%2CsharepointIds%2CisRestricted%2CcommentSettings%2CspecialFolder%2CcontainingDrivePolicyScenarioViewpoint");
                querypagination.add("ump", "1");
                if (authkey != null) {
                    querypagination.add("authKey", Encoding.urlEncode(authkey));
                }
                String nextpageLink = "/v1.0/drives/" + cid.toLowerCase(Locale.ENGLISH) + "/items/" + resource_id + "/children?" + querypagination;
                do {
                    br.getPage(nextpageLink);
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("value");
                    int numberofNewItemsThisPage = 0;
                    for (final Map<String, Object> item : items) {
                        final String itemID = item.get("id").toString();
                        if (!dupes.add(itemID)) {
                            /* Skip duplicates */
                            continue;
                        }
                        numberofNewItemsThisPage++;
                        final Map<String, Object> file = (Map<String, Object>) item.get("file");
                        if (file != null) {
                            /* Single file */
                            final DownloadLink dlfile = crawlProcessSingleFile(item);
                            dlfile._setFilePackage(fp);
                            /* Set additional important properties */
                            ret.add(dlfile);
                            distribute(dlfile);
                        } else {
                            /* Subfolder */
                            /* Do not use the URL from field "webUrl" because this will need one extra http request later. */
                            final String folderlink = this.generateFolderLink(cid, itemID, this.authkey);
                            // final Map<String, Object> folder = (Map<String, Object>) item.get("folder");
                            final DownloadLink dlfolder = this.createDownloadlink(folderlink);
                            ret.add(dlfolder);
                            distribute(dlfolder);
                        }
                    }
                    final int totalNumberofItems = ((Number) entries.get("@odata.count")).intValue();
                    nextpageLink = (String) entries.get("@odata.nextLink");
                    logger.info("Crawled page " + page + " | Found items on this page: " + items.size() + "/" + maxItemsPerPage + " | Found items so far: " + ret.size() + "/" + totalNumberofItems + " | nextpageLink = " + nextpageLink);
                    if (this.isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else if (numberofNewItemsThisPage == 0) {
                        logger.info("Stopping because: Failed to find any new items on current page");
                        break;
                    } else if (StringUtils.isEmpty(nextpageLink)) {
                        logger.info("Stopping because: Reached last page");
                        break;
                    } else {
                        /* Continue to next page */
                        page++;
                    }
                } while (!this.isAbort());
            } else {
                /* Crawl single file */
                ret.add(crawlProcessSingleFile(resourceinfo));
            }
            return ret;
        } else {
            return crawlLegacy(contenturl);
        }
    }

    private void setGlobalVars(final String url) throws MalformedURLException {
        final UrlQuery query = UrlQuery.parse(url);
        resource_id = query.getDecoded("resid");
        cid = query.getDecoded("cid");
        authkey = query.getDecoded("authkey");
        if (resource_id == null) {
            resource_id = getLastID(url);
        }
    }

    private DownloadLink crawlProcessSingleFile(final Map<String, Object> item) throws PluginException {
        ensureInitHosterplugin();
        final String url = item.get("webUrl").toString();
        final String filename = item.get("name").toString();
        final DownloadLink dlfile = this.createDownloadlink(url);
        dlfile.setFinalFileName(filename);
        dlfile.setVerifiedFileSize(((Number) item.get("size")).longValue());
        dlfile.setDefaultPlugin(this.hostPlugin);
        dlfile.setAvailable(true);
        {
            /* Set file-hash */
            final Map<String, Object> file = (Map<String, Object>) item.get("file");
            final Map<String, Object> hashes = (Map<String, Object>) file.get("hashes");
            final String sha256Hash = (String) hashes.get("sha256Hash");
            final String sha1Hash = (String) hashes.get("sha1Hash");
            if (!StringUtils.isEmpty(sha256Hash)) {
                dlfile.setSha256Hash(sha256Hash);
            } else if (!StringUtils.isEmpty(sha1Hash)) {
                dlfile.setSha1Hash(sha1Hash);
            }
        }
        String pathFromRoot = item.get("pathFromRoot").toString();
        pathFromRoot = Encoding.htmlDecode(pathFromRoot).trim();
        final String pathToFile = pathFromRoot.replaceFirst("/" + Pattern.quote(filename) + "$", "");
        dlfile.setRelativeDownloadFolderPath(pathToFile);
        /* Set additional important properties */
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_FILE_ID, item.get("id"));
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_CID, cid);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_FOLDER_ID, resource_id);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_AUTHKEY, authkey);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_VIEW_IN_BROWSER_URL, url);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_DIRECTURL, item.get("@content.downloadUrl"));
        {
            final Map<String, Object> parentReference = (Map<String, Object>) item.get("parentReference");
            if (parentReference != null) {
                dlfile.setProperty(OneDriveLiveCom.PROPERTY_PARENT_FOLDER_ID, parentReference.get("id"));
            }
        }
        return dlfile;
    }

    @Deprecated
    public static final int MAX_ENTRIES_PER_REQUEST_LEGACY = 200;

    @Deprecated
    private ArrayList<DownloadLink> crawlLegacy(final String contenturl) throws Exception {
        /* TODO: Delete this in 2024-05 */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        prepBrAPILegacy(this.br);
        String subFolderBase = null;
        final String additional_data;
        if (authkey != null) {
            additional_data = "&authkey=" + Encoding.urlEncode(authkey);
        } else {
            additional_data = "";
        }
        int page = 1;
        int startIndex = 0;
        int nextStartIndex = 0;
        final Set<String> dups = new HashSet<String>();
        final long ITEM_TYPE_FILE = 1;
        final long ITEM_TYPE_PICTURE = 3;
        final long ITEM_TYPE_VIDEO = 5;
        final int ITEM_TYPE_FOLDER = 32;
        do {
            startIndex = nextStartIndex;
            accessItems_API(this.br, contenturl, cid, resource_id, additional_data, startIndex, MAX_ENTRIES_PER_REQUEST_LEGACY);
            nextStartIndex = startIndex + MAX_ENTRIES_PER_REQUEST_LEGACY;
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Object error = entries.get("error");
            if (error != null) {
                /*
                 * E.g. "{"error":{"code":3000,"debugMessage":"Item not found or access denied.","isExpected":true,"message":"This item
                 * might have been deleted, expired, or you might not have permission to access it. Contact the owner of this item for more
                 * information."....
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            List<Map<String, Object>> items = (List) entries.get("items");
            if (items.size() > 1 && StringUtils.isEmpty(subFolderBase)) {
                /* Try to build absolute path to current folder/file */
                subFolderBase = "";
                for (final Map<String, Object> folder : items) {
                    final String folderID = (String) folder.get("id");
                    if (folderID.equalsIgnoreCase("root")) {
                        /* Reached end */
                        break;
                    }
                    final String name = (String) folder.get("name");
                    if (StringUtils.isEmpty(name)) {
                        /* This should never happen! */
                        logger.warning("Folder without name??");
                    }
                    subFolderBase = name + "/" + subFolderBase;
                }
                logger.info("Found absolute path: " + subFolderBase);
            }
            FilePackage fp = null;
            if (!StringUtils.isEmpty(subFolderBase)) {
                fp = FilePackage.getInstance();
                fp.setName(subFolderBase);
            }
            Map<String, Object> firstItem = items.get(0);
            final long totalItemType = JavaScriptEngineFactory.toLong(firstItem.get("itemType"), -1);
            if (totalItemType == ITEM_TYPE_FILE || totalItemType == ITEM_TYPE_PICTURE || totalItemType == ITEM_TYPE_VIDEO) {
                /* Single file */
                final DownloadLink link = parseFileLegacy(contenturl, firstItem, startIndex, MAX_ENTRIES_PER_REQUEST_LEGACY);
                if (dups.add(link.getLinkID())) {
                    if (fp != null) {
                        fp.add(link);
                    }
                    if (!StringUtils.isEmpty(subFolderBase)) {
                        link.setRelativeDownloadFolderPath(subFolderBase);
                    }
                    ret.add(link);
                    distribute(link);
                }
                logger.info("Stopping because: Folder contains/is single file only");
                break;
            } else {
                if (firstItem.containsKey("folder")) {
                    firstItem = (Map<String, Object>) firstItem.get("folder");
                    items = (List<Map<String, Object>>) firstItem.get("children");
                }
                if (fp == null) {
                    /* This should NEVER happen */
                    fp = FilePackage.getInstance();
                    fp.setName("onedrive.live.com content of user " + cid + " - folder - " + resource_id);
                }
                /* Folder, maybe with subfolders */
                final long totalCount;
                if (firstItem.containsKey("totalCount")) {
                    totalCount = JavaScriptEngineFactory.toLong(firstItem.get("totalCount"), 0);
                } else {
                    totalCount = items.size();
                }
                final int childCount = ((Number) firstItem.get("childCount")).intValue();
                if (br.containsHTML("\"code\":154")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (totalCount == 0 && childCount == 0) {
                    /* Empty folder */
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                }
                final int lastSize = ret.size();
                for (final Map<String, Object> entry : items) {
                    final boolean isPlaceholder = entry.containsKey("isPlaceholder") ? ((Boolean) entry.get("isPlaceholder")).booleanValue() : false;
                    final long type = ((Number) entry.get("itemType")).longValue();
                    final String item_id = (String) entry.get("id");
                    final String creatorCid = (String) entry.get("creatorCid");
                    if (isPlaceholder) {
                        /* Skip 'dummy' items */
                        continue;
                    }
                    if (type == ITEM_TYPE_FOLDER) {
                        /* Folder --> Goes back into crawler */
                        if (item_id == null || creatorCid == null) {
                            /* Fatal failure */
                            return null;
                        }
                        final String folderlink = generateFolderLink(creatorCid, item_id, this.authkey);
                        if (dups.add(folderlink)) {
                            final DownloadLink dlfolder = createDownloadlink(folderlink);
                            ret.add(dlfolder);
                        }
                    } else {
                        /* File --> Grab information & return to crawler. */
                        final DownloadLink link = parseFileLegacy(contenturl, entry, startIndex, MAX_ENTRIES_PER_REQUEST_LEGACY);
                        if (dups.add(link.getLinkID())) {
                            if (fp != null) {
                                fp.add(link);
                            }
                            if (!StringUtils.isEmpty(subFolderBase)) {
                                link.setRelativeDownloadFolderPath(subFolderBase);
                            }
                            ret.add(link);
                            distribute(link);
                        }
                    }
                }
                if (ret.size() == lastSize) {
                    logger.info("Stopping because: Failed to find more items on current page");
                    break;
                }
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            page++;
        } while (true);
        return ret;
    }

    private String generateFolderLink(final String cid, final String id, final String authkey) {
        String folderlink = "https://onedrive.live.com/?cid=" + cid + "&id=" + id;
        if (authkey != null) {
            /* Don't forget to add authKey if needed */
            folderlink += "&authkey=" + authkey;
        }
        return folderlink;
    }

    private String getLastID(final String url) {
        /* Get last ID */
        int pos = url.lastIndexOf("&id=") + 4;
        final String parameter_part = url.substring(pos, url.length());
        final String ret = new Regex(parameter_part, "([A-Z0-9]+(\\!|%21)\\d+)").getMatch(0);
        if (ret != null) {
            return ret.replace("%21", "!");
        } else {
            return ret;
        }
    }

    @Deprecated
    private DownloadLink parseFileLegacy(final String contenturl, final Map<String, Object> entry, final int startIndex, final int maxItems) throws DecrypterException, PluginException {
        /* File --> Grab information & return to decrypter. All found links are usually ONLINE and downloadable! */
        ensureInitHosterplugin();
        final Map<String, Object> urls = (Map<String, Object>) entry.get("urls");
        final String name = (String) entry.get("name");
        final String iconType = (String) entry.get("iconType");
        final String extension = (String) entry.get("extension");
        /* For single pictures, get the highest quality pic */
        if ("Photo".equals(iconType) && extension == null) {
            /* Download and view of the original picture only possible via account */
            // br.getPage("https://onedrive.live.com/download.aspx?cid=" + cid + "&resid=" + Encoding.urlEncode(id) +
            // "&canary=");
            // download_url = br.getRedirectLocation();
            // final String photoLinks[] = new Regex(singleinfo, "\"streamVersion\":\\d+,\"url\":\"([^<>\"]*?)\"").getColumn(0);
            // if (photoLinks != null && photoLinks.length != 0) {
            // download_url = "https://dm" + photoLinks[photoLinks.length - 1];
            // }
            /* TODO: */
            throw new DecrypterException("Decrypter broken");
        }
        if (StringUtils.isEmpty(name)) {
            throw new DecrypterException("Decrypter broken");
        }
        final DownloadLink dlfile = createDownloadlink(contenturl);
        dlfile.setDefaultPlugin(this.hostPlugin);
        /* Files without extension == possible */
        final String filename;
        if (extension != null) {
            filename = name + extension;
        } else {
            filename = name;
        }
        final long size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
        if (size >= 0) {
            dlfile.setVerifiedFileSize(size);
        }
        dlfile.setFinalFileName(filename);
        dlfile.setProperty("mainlink", contenturl);
        dlfile.setProperty("plain_name", filename);
        dlfile.setProperty("plain_filesize", size);
        final String download_url = (String) urls.get("download");
        if (download_url != null) {
            dlfile.setProperty("plain_download_url", download_url);
        } else {
            dlfile.setProperty(OneDriveLiveCom.PROPERTY_ACCOUNT_ONLY, true);
        }
        final String itemId = (String) entry.get("id");
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_FILE_ID, itemId);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_CID, cid);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_FOLDER_ID, resource_id);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_AUTHKEY, authkey);
        dlfile.setProperty(OneDriveLiveCom.PROPERTY_VIEW_IN_BROWSER_URL, urls.get("viewInBrowser"));
        dlfile.setProperty("plain_item_si", startIndex);
        dlfile.setAvailable(true);
        return dlfile;
    }

    @Deprecated
    public static String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    @Deprecated
    public static String getLinktextLegacy(final Browser br) {
        String linktext = br.getRegex("\"children\":\\[(\\{.*?\\})\\],\"covers\":").getMatch(0);
        if (linktext == null) {
            linktext = br.getRegex("\"children\":\\[(\\{.*?\\})\\],\"defaultSort\":\\d+").getMatch(0);
        }
        // Check for single pictures: https://onedrive.live.com/?cid=E0615573A3471F93&id=E0615573A3471F93!1567
        if (linktext == null) {
            linktext = br.getRegex("\"items\":\\[(\\{.*?\\})\\]").getMatch(0);
        }
        if (linktext == null) {
            linktext = br.getRegex("\"children\":\\[(.*?)\\],\"defaultSort\":").getMatch(0);
        }
        return linktext;
    }

    @Deprecated
    public static void prepBrAPILegacy(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("X-ForceCache", "1");
        br.getHeaders().put("X-SkyApiOriginId", "0.9554840477898046");
        br.getHeaders().put("Referer", "https://skyapi.onedrive.live.com/api/proxy?v=3");
        br.getHeaders().put("AppId", "1141147648");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        br.setAllowedResponseCodes(500);
    }

    @Deprecated
    public static void accessItems_API(final Browser br, final String contenturl, final String cid, final String id, final String additional, final int startIndex, final int maxItems) throws IOException {
        final String v = "0.10707631620552516";
        String data = null;
        final String fromTo = "&si=" + startIndex + "&ps=" + maxItems;
        if (contenturl.contains("ithint=") && id != null) {
            data = "&cid=" + Encoding.urlEncode(cid) + additional;
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?id=" + id + "&group=0&qt=&ft=&sb=1&sd=1&gb=0%2C1%2C2&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
        } else if (id == null && contenturl.matches("(?i)https?://onedrive\\.live\\.com/\\?cid=[a-z0-9]+")) {
            /* Access root-dir */
            data = "&cid=" + Encoding.urlEncode(cid);
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?id=root&group=0&qt=&ft=&sb=0&sd=0&gb=0%2C1%2C2&rif=0&d=1&iabch=1&caller=unauth&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
        } else {
            data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(id) + additional;
            boolean failed = false;
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=unauth&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
            /* Maybe the folder is empty but we can move one up and get its contents... */
            if (br.getRequest().getHttpConnection().getResponseCode() == 500 || getLinktextLegacy(br) == null) {
                br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0%2C1%2C2&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
                final String parentID = getJson("parentId", br.toString());
                if (parentID != null) {
                    /* Error 500 will happen on invalid API requests */
                    data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(parentID) + "&sid=" + Encoding.urlEncode(id) + additional;
                    br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
                }
            }
        }
    }
}
