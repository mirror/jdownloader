package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
import jd.plugins.Account;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PremiumizeBrowseNode;
import jd.plugins.hoster.PremiumizeMe;
import jd.plugins.hoster.ZeveraCom;
import jd.plugins.hoster.ZeveraCore;

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
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            /* Account required to add such URLs */
            throw new AccountRequiredException();
        }
        this.setBrowserExclusive();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<PremiumizeBrowseNode> nodes = getNodes(br, account, parameter.getCryptedUrl());
        final Map<String, Object> data = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final String status = (String) data.get("status");
        final Object errorO = data.get("error");
        if ("error".equals(status) || errorO != null) {
            throw new AccountRequiredException();
        } else if (nodes == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Use path from previous craw process if available --> Saves http requests */
        String folderPath = this.getAdoptedCloudFolderStructure();
        if (folderPath == null) {
            /* Try to find complete path by going back until we reach the root folder. */
            folderPath = this.findFullFolderPath(account, "", data, new HashSet<String>());
        }
        /*
         * Allow loose files from root folder to go into package named "root" but remove "root" from path for all items that are below the
         * root folder.
         */
        if (folderPath.contains("/") && folderPath.startsWith("root")) {
            folderPath = folderPath.substring(folderPath.indexOf("/") + 1);
        }
        ret.addAll(convert(parameter.getCryptedUrl(), nodes, folderPath));
        return ret;
    }

    /**
     * Recursive function that finds full path to a folder by going up until the root folder. </br>
     * This is needed because the API we use does not contain absolute paths.
     *
     * @throws Exception
     */
    private String findFullFolderPath(final Account account, String path, final Map<String, Object> data, final HashSet<String> dupes) throws Exception {
        final String parent_id = (String) data.get("parent_id");
        final String currentFolderName = (String) data.get("name");
        if (StringUtils.isEmpty(parent_id) || StringUtils.isEmpty(currentFolderName)) {
            /**
             * This should never happen but can and will: e.g. {"status":"error","message":"Could not decode folder id"} </br>
             * The root folder also has a 'parent_id' but we can't access this one and also we can't just stop when we've reached a folder
             * named 'root' because users could also download files with normal folders named 'root'...
             */
            return path;
        } else if (dupes.contains(parent_id)) {
            /* Fail safe was triggered */
            return path;
        } else if (dupes.size() >= 200) {
            /* Fail safe 2: This should never happen */
            logger.warning("Fail safe triggered: Path is too long");
            return path;
        } else {
            dupes.add(parent_id);
            if (path.length() == 0) {
                path = currentFolderName;
            } else {
                path = currentFolderName + "/" + path;
            }
            final String response = accessCloudItem(br, account, createFolderURL(this.getHost(), parent_id));
            final Map<String, Object> dataNew = JavaScriptEngineFactory.jsonToJavaMap(response);
            return findFullFolderPath(account, path, dataNew, dupes);
        }
    }

    public List<DownloadLink> convert(final String url_source, ArrayList<PremiumizeBrowseNode> premiumizeNodes, final String folderPath) {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (premiumizeNodes == null || premiumizeNodes.size() == 0) {
            return ret;
        }
        final String host = Browser.getHost(url_source);
        final String folderPathForFiles;
        if (folderPath.equals("root")) {
            folderPathForFiles = this.getHost() + "_root";
        } else {
            folderPathForFiles = folderPath;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderPathForFiles);
        for (final PremiumizeBrowseNode node : premiumizeNodes) {
            final String itemName = node.getName();
            final String nodeCloudID = node.getID();
            if (node._isDirectory()) {
                /* Folder */
                final String folderPathForNextCrawlLevel = folderPath + "/" + itemName;
                final String folderURL = createFolderURL(host, nodeCloudID);
                final DownloadLink folder = new DownloadLink(null, null, host, folderURL, true);
                folder.setRelativeDownloadFolderPath(folderPathForNextCrawlLevel);
                ret.add(folder);
            } else {
                /* File */
                final String url_for_hostplugin = "https://" + host + "/file?id=" + nodeCloudID;
                final DownloadLink link = new DownloadLink(null, null, host, url_for_hostplugin, true);
                setPremiumizeBrowserNodeInfoOnDownloadlink(link, node);
                link.setRelativeDownloadFolderPath(folderPathForFiles);
                link._setFilePackage(fp);
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

    public static ArrayList<PremiumizeBrowseNode> getNodes(final Browser br, final Account account, final String url) throws IOException, AccountInvalidException {
        final String response = accessCloudItem(br, account, url);
        final Map<String, Object> responseMap = JSonStorage.restoreFromString(response, TypeRef.HASHMAP);
        final String status = (String) responseMap.get("status");
        final ArrayList<PremiumizeBrowseNode> browseNodes = new ArrayList<PremiumizeBrowseNode>();
        if (StringUtils.equals("success", status)) {
            /* Folder */
            final List<Object> folderContents = (List<Object>) responseMap.get("content");
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

    protected static String accessCloudItem(final Browser br, final Account account, final String url_source) throws IOException, AccountInvalidException {
        final boolean pairingLogin = ZeveraCore.setAuthHeader(br, account);
        final String itemID = PremiumizeMe.getCloudID(url_source);
        final String client_id;
        if (account.getHoster().equals("premiumize.me")) {
            client_id = PremiumizeMe.getClientIDExt();
        } else {
            client_id = ZeveraCom.getClientIDExt();
        }
        final UrlQuery query = new UrlQuery();
        query.add("id", Encoding.urlEncode(itemID));
        query.add("client_id", Encoding.urlEncode(client_id));
        if (!pairingLogin) {
            query.add("apikey", Encoding.urlEncode(ZeveraCore.getAPIKey(account)));
        }
        if (StringUtils.containsIgnoreCase(url_source, "folder_id")) {
            /* Folder */
            br.getPage("https://www." + account.getHoster() + "/api/folder/list?" + query.toString());
        } else {
            /* Single file */
            br.getPage("https://www." + account.getHoster() + "/api/item/details?" + query.toString());
        }
        return br.getRequest().getHtmlCode();
    }
}
