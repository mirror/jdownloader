package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "premiumize.me" }, urls = { "https?://(?:(?:www|beta)\\.)?premiumize\\.me/files\\?folder_id=[A-Z0-9\\-_]+(?:\\&folderpath=[a-zA-Z0-9_/\\+\\=\\-%]+)?" })
public class PremiumizeMe extends PluginForDecrypt {
    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (accs.size() > 0) {
            setBrowserExclusive();
            final String cloudID = jd.plugins.hoster.PremiumizeMe.getCloudID(parameter.getCryptedUrl());
            final Account account = accs.get(0);
            final ArrayList<PremiumizeBrowseNode> nodes = getNodes(br, account, cloudID);
            /* Find path from previous craw process if available. */
            String folderPath = new Regex(parameter.getCryptedUrl(), "folderpath=(.+)").getMatch(0);
            if (folderPath != null) {
                folderPath = Encoding.Base64Decode(folderPath);
            } else {
                folderPath = "0";
            }
            ret.addAll(convert(nodes, folderPath));
        }
        return ret;
    }

    public static List<DownloadLink> convert(ArrayList<PremiumizeBrowseNode> premiumizeNodes, String currentPath) {
        final List<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (premiumizeNodes == null || premiumizeNodes.size() == 0) {
            return ret;
        }
        if (currentPath == null) {
            currentPath = "";
        }
        final boolean addPath = StringUtils.isNotEmpty(currentPath);
        final FilePackage filePackage;
        if (addPath) {
            filePackage = FilePackage.getInstance();
            filePackage.setName(currentPath);
        } else {
            filePackage = null;
        }
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
                if (filePackage != null) {
                    filePackage.add(link);
                }
                if (addPath) {
                    link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, currentPath);
                }
                ret.add(link);
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
            final ArrayList<PremiumizeBrowseNode> browseNodes = new ArrayList<PremiumizeBrowseNode>();
            for (final Object jsonObject : folderContents) {
                final Map<String, Object> folderObject = (Map<String, Object>) jsonObject;
                browseNodes.add(JSonStorage.restoreFromString(JSonStorage.toString(folderObject), new TypeRef<PremiumizeBrowseNode>() {
                }, null));
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
