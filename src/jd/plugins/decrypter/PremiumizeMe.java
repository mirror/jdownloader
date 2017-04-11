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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PremiumizeBrowseNode;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "premiumize.me" }, urls = { "https?://(www\\.)?premiumize\\.me/browsetorrent\\?hash=[a-f0-9]+" }) public class PremiumizeMe extends PluginForDecrypt {

    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (accs.size() > 0) {
            setBrowserExclusive();
            final String hash = new Regex(parameter.getCryptedUrl(), "hash=([a-f0-9]+)").getMatch(0);
            final Account account = accs.get(0);
            final Map<String, PremiumizeBrowseNode> nodes = getNodes(br, account, hash);
            ret.addAll(convert(nodes, ""));
        }
        return ret;
    }

    public static List<DownloadLink> convert(Map<String, PremiumizeBrowseNode> premiumizeNodes, String currentPath) {
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
        for (final PremiumizeBrowseNode node : premiumizeNodes.values()) {
            if (node._isFile()) {
                final DownloadLink link = new DownloadLink(null, null, "premiumize.me", node.getUrl(), true);
                if (node.getSize() >= 0) {
                    link.setVerifiedFileSize(node.getSize());
                }
                link.setFinalFileName(node.getName());
                if (filePackage != null) {
                    filePackage.add(link);
                }
                link.setAvailable(true);
                link.setLinkID("premiumizetorrent://" + Hash.getSHA256(currentPath + node.getName()) + node.getSize());
                if (addPath) {
                    link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, currentPath);
                }
                ret.add(link);
            } else if (node._isDirectory() && node.getChildren() != null) {
                if (addPath) {
                    ret.addAll(convert(node.getChildren(), currentPath + "/" + CrossSystem.alleviatePathParts(node.getName())));
                } else {
                    ret.addAll(convert(node.getChildren(), CrossSystem.alleviatePathParts(node.getName())));
                }
            }
        }
        return ret;
    }

    public static Map<String, PremiumizeBrowseNode> getNodes(Browser br, Account account, final String hash) throws IOException {
        final String responseString = br.postPage("https://www.premiumize.me/api/torrent/browse", "customer_id=" + Encoding.urlEncode(account.getUser()) + "&pin=" + Encoding.urlEncode(account.getPass()) + "&hash=" + hash);
        final Map<String, Object> responseMap = JSonStorage.restoreFromString(responseString, TypeRef.HASHMAP, null);
        final String status = (String) responseMap.get("status");
        if (StringUtils.equals("success", status)) {
            final Map<String, Object> contentValue = (Map<String, Object>) responseMap.get("content");
            final HashMap<String, PremiumizeBrowseNode> premiumizeNodes = JSonStorage.restoreFromString(JSonStorage.toString(contentValue), new TypeRef<HashMap<String, PremiumizeBrowseNode>>() {
            }, null);
            return premiumizeNodes;
        }
        return null;
    }

}
