package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.PremiumizeBrowseNode;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PremiumizeMeZeveraFolder extends PluginForDecrypt {
    public PremiumizeMeZeveraFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "premiumize.me" });
        ret.add(new String[] { "zevera.com" });
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
            ret.add("https?://(?:[a-z0-9\\.\\-]+)?" + buildHostsPatternPart(domains) + "/files\\?folder_id=([a-zA-Z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (accs == null || accs.size() == 0) {
            logger.info("Cannot add cloud URLs without account");
            throw new AccountRequiredException();
        }
        setBrowserExclusive();
        final Account account = accs.get(0);
        final ArrayList<PremiumizeBrowseNode> nodes = getNodes(br, account, parameter.getCryptedUrl());
        if (nodes == null) {
            final String status = PluginJSonUtils.getJson(br, "status");
            if ("error".equals(status)) {
                /* E.g. "{"status":"error","message":"customer_id and pin param missing or not logged in "}" */
                logger.info("Either invalid logindata, wrong account (you can only download your own cloud files) OR offline content");
                final String url_params = new Regex(parameter.getCryptedUrl(), "\\?(.+)").getMatch(0);
                final DownloadLink offline = this.createOfflinelink(parameter.getCryptedUrl());
                if (url_params != null) {
                    offline.setName(url_params);
                }
                ret.add(offline);
                return ret;
            }
            return null;
        }
        /* Find path from previous craw process if available. */
        final String folderPath = this.getAdoptedCloudFolderStructure();
        ret.addAll(convert(parameter.getCryptedUrl(), nodes, folderPath));
        return ret;
    }

    private static FilePackage getFilePackage(Map<String, FilePackage> filePackages, PremiumizeBrowseNode node) {
        FilePackage ret = filePackages.get(node._getParentID());
        if (ret == null && StringUtils.isNotEmpty(node._getParentName())) {
            ret = FilePackage.getInstance();
            ret.setName(node._getParentName());
            filePackages.put(node._getParentID(), ret);
        }
        return ret;
    }

    public static List<DownloadLink> convert(final String url_source, ArrayList<PremiumizeBrowseNode> premiumizeNodes, String currentPath) {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (premiumizeNodes == null || premiumizeNodes.size() == 0) {
            return ret;
        }
        if (currentPath == null) {
            currentPath = "";
        }
        final boolean addPath = StringUtils.isNotEmpty(currentPath);
        final Map<String, FilePackage> filePackages = new HashMap<String, FilePackage>();
        final String host = Browser.getHost(url_source);
        for (final PremiumizeBrowseNode node : premiumizeNodes) {
            final String itemName = node.getName();
            final String parentName = node._getParentName();
            final String nodeCloudID = node.getID();
            if (node._isDirectory()) {
                /* Folder */
                final String path_for_next_crawl_level;
                if (StringUtils.isEmpty(currentPath)) {
                    if (!StringUtils.isEmpty(parentName)) {
                        path_for_next_crawl_level = parentName + "/" + itemName;
                    } else {
                        path_for_next_crawl_level = itemName;
                    }
                } else {
                    path_for_next_crawl_level = currentPath + "/" + itemName;
                }
                final String folderURL = createFolderURL(host, nodeCloudID);
                final DownloadLink folder = new DownloadLink(null, null, host, folderURL, true);
                folder.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path_for_next_crawl_level);
                ret.add(folder);
            } else {
                /* File */
                final String url_for_hostplugin = "https://" + host + "/file?id=" + nodeCloudID;
                final DownloadLink link = new DownloadLink(null, null, host, url_for_hostplugin, true);
                setPremiumizeBrowserNodeInfoOnDownloadlink(link, node);
                final FilePackage filePackage = getFilePackage(filePackages, node);
                if (filePackage != null) {
                    filePackage.add(link);
                }
                if (addPath && StringUtils.isNotEmpty(currentPath)) {
                    link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, currentPath);
                }
                ret.add(link);
            }
        }
        return ret;
    }

    private static String createFolderURL(final String host, final String cloudID) {
        return String.format("https://www." + host + "/files?folder_id=%s", cloudID);
    }

    /* Sets info from PremiumizeBrowseNode --> On DownloadLink */
    public static void setPremiumizeBrowserNodeInfoOnDownloadlink(final DownloadLink link, final PremiumizeBrowseNode node) {
        if (node.getSize() >= 0) {
            link.setVerifiedFileSize(node.getSize());
        }
        link.setFinalFileName(node.getName());
        link.setAvailable(true);
        link.setLinkID(link.getHost() + "://" + node.getID());
    }

    public static ArrayList<PremiumizeBrowseNode> getNodes(final Browser br, final Account account, final String url) throws IOException {
        String cloudID = jd.plugins.hoster.PremiumizeMe.getCloudID(url);
        if (cloudID == null) {
            /*
             * 2018-02-24: No cloudID found? Fallback to root folder. This may happen if only a file_id is given --> It must be located in
             * the root dir.
             */
            cloudID = "0";
        }
        final String response = accessCloudItem(br, account, url);
        final Map<String, Object> responseMap = JSonStorage.restoreFromString(response, TypeRef.HASHMAP);
        final String status = (String) responseMap.get("status");
        final ArrayList<PremiumizeBrowseNode> browseNodes = new ArrayList<PremiumizeBrowseNode>();
        if (StringUtils.equals("success", status)) {
            /* Folder */
            final List<Object> folderContents = (ArrayList<Object>) responseMap.get("content");
            final String folderName = (String) responseMap.get("name");
            final String parentID = (String) responseMap.get("parent_id");
            for (final Object jsonObject : folderContents) {
                final Map<String, Object> folderObject = (Map<String, Object>) jsonObject;
                final PremiumizeBrowseNode node = JSonStorage.restoreFromString(JSonStorage.toString(folderObject), new TypeRef<PremiumizeBrowseNode>() {
                });
                if (node != null) {
                    node._setParentName(folderName);
                    node._setParentID(parentID);
                    browseNodes.add(node);
                }
            }
            return browseNodes;
        } else if (status == null || (status != null && !status.equals("error"))) {
            /* Single file */
            final PremiumizeBrowseNode node = JSonStorage.restoreFromString(JSonStorage.toString(responseMap), new TypeRef<PremiumizeBrowseNode>() {
            });
            if (node != null) {
                browseNodes.add(node);
                return browseNodes;
            }
        }
        /* Error e.g. {"status":"error","message":"Nicht dein Ordner"} */
        return null;
    }

    public static String accessCloudItem(final Browser br, final Account account, final String url_source) throws IOException {
        final boolean pairingLogin = jd.plugins.hoster.ZeveraCore.setAuthHeader(br, account);
        final String itemID = jd.plugins.hoster.PremiumizeMe.getCloudID(url_source);
        final String client_id;
        if (account.getHoster().equals("premiumize.me")) {
            client_id = jd.plugins.hoster.PremiumizeMe.getClientIDExt();
        } else {
            client_id = jd.plugins.hoster.ZeveraCom.getClientIDExt();
        }
        String getData = "?id=" + itemID + "&client_id=" + client_id;
        if (!pairingLogin) {
            getData += "&pin=" + Encoding.urlEncode(jd.plugins.hoster.ZeveraCore.getAPIKey(account));
        }
        if (StringUtils.containsIgnoreCase(url_source, "folder_id")) {
            /* Folder */
            br.getPage("https://www." + account.getHoster() + "/api/folder/list" + getData);
        } else {
            /* Single file */
            br.getPage("https://www." + account.getHoster() + "/api/item/details" + getData);
        }
        return br.toString();
    }
}
