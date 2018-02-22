package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PremiumizeBrowseNode;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "premiumize.me" }, urls = { "https?://(?:(?:www|beta)\\.)?premiumize\\.me/files\\?folder_id=[a-zA-Z0-9\\-_]+(?:\\&file_id=[a-zA-Z0-9\\-_]+)?(?:\\&folderpath=[a-zA-Z0-9_/\\+\\=\\-%]+)?" })
public class PremiumizeMe extends PluginForDecrypt {
    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (accs.size() == 0) {
            logger.info("Cannot add cloud URLs without account");
            return ret;
        }
        setBrowserExclusive();
        final String cloudID = jd.plugins.hoster.PremiumizeMe.getCloudID(parameter.getCryptedUrl());
        final Account account = accs.get(0);
        final ArrayList<PremiumizeBrowseNode> nodes = getNodes(br, account, cloudID);
        /* Find path from previous craw process if available. */
        String folderPath = new Regex(parameter.getCryptedUrl(), "folderpath=(.+)").getMatch(0);
        if (folderPath != null) {
            folderPath = Encoding.Base64Decode(folderPath);
        } else {
            folderPath = "";
        }
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
        /* If given, user only wants to have one specific file. */
        final String targetFileID = new Regex(url_source, "file_id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        final boolean addPath = StringUtils.isNotEmpty(currentPath);
        final Map<String, FilePackage> filePackages = new HashMap<String, FilePackage>();
        for (final PremiumizeBrowseNode node : premiumizeNodes) {
            final String itemName = node.getName();
            final String nodeCloudID = node.getID();
            if (node._isDirectory()) {
                /* Folder */
                final String path_for_next_crawl_level;
                if (StringUtils.isEmpty(currentPath)) {
                    path_for_next_crawl_level = itemName;
                } else {
                    path_for_next_crawl_level = currentPath + "/" + itemName;
                }
                final String folderURL = createFolderURL(nodeCloudID) + "&folderpath=" + Encoding.Base64Encode(path_for_next_crawl_level);
                final DownloadLink folder = new DownloadLink(null, null, "premiumize.me", folderURL, true);
                ret.add(folder);
            } else {
                /* File */
                /** TODO: Maybe do not add .nzb and .torrent files (see comment in host class near PremiumizeMeConfigInterface)! */
                final DownloadLink link = new DownloadLink(null, null, "premiumize.me", node.getUrl(), true);
                setPremiumizeBrowserNodeInfoOnDownloadlink(link, node);
                final FilePackage filePackage = getFilePackage(filePackages, node);
                if (filePackage != null) {
                    filePackage.add(link);
                }
                if (addPath && StringUtils.isNotEmpty(currentPath)) {
                    link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, currentPath);
                }
                if (targetFileID != null && nodeCloudID.equals(targetFileID)) {
                    /* User only wants to add specific file --> Delete everything else and only return this single URL/file. */
                    ret.clear();
                    ret.add(link);
                    break;
                } else {
                    ret.add(link);
                }
            }
        }
        return ret;
    }

    private static String createFolderURL(final String cloudID) {
        return String.format("https://www.premiumize.me/files?folder_id=%s", cloudID);
    }

    /* Sets info from PremiumizeBrowseNode --> On DownloadLink */
    public static void setPremiumizeBrowserNodeInfoOnDownloadlink(final DownloadLink link, final PremiumizeBrowseNode node) {
        if (node.getSize() >= 0) {
            link.setVerifiedFileSize(node.getSize());
        }
        link.setFinalFileName(node.getName());
        link.setAvailable(true);
        link.setLinkID("premiumizecloud://" + node.getID());
    }

    public static ArrayList<PremiumizeBrowseNode> getNodes(Browser br, Account account, final String cloudID) throws IOException {
        accessCloudItem(br, account, cloudID);
        final Map<String, Object> responseMap = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP, null);
        final String status = (String) responseMap.get("status");
        if (StringUtils.equals("success", status)) {
            final ArrayList<Object> folderContents = (ArrayList<Object>) responseMap.get("content");
            final String folderName = (String) responseMap.get("name");
            final String parentID = (String) responseMap.get("parent_id");
            final ArrayList<PremiumizeBrowseNode> browseNodes = new ArrayList<PremiumizeBrowseNode>();
            for (final Object jsonObject : folderContents) {
                final Map<String, Object> folderObject = (Map<String, Object>) jsonObject;
                PremiumizeBrowseNode node = JSonStorage.restoreFromString(JSonStorage.toString(folderObject), new TypeRef<PremiumizeBrowseNode>() {
                }, null);
                if (node != null) {
                    node._setParentName(folderName);
                    node._setParentID(parentID);
                    browseNodes.add(node);
                }
            }
            return browseNodes;
        }
        return null;
    }

    public static String accessCloudItem(final Browser br, final Account account, final String itemID) throws IOException {
        br.getPage("https://www.premiumize.me/api/folder/list?customer_id=" + Encoding.urlEncode(account.getUser()) + "&pin=" + Encoding.urlEncode(account.getPass()) + "&id=" + itemID);
        return br.toString();
    }
}
