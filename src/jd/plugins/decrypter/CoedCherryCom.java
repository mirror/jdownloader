package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "coedcherry.com" }, urls = { "https?://(?:www\\.)?coedcherry\\.com/.*pics/.+" })
public class CoedCherryCom extends PluginForDecrypt {
    public CoedCherryCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = cryptedLink.toString();
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        }
        String filePackageName = getFilePackageName(url);
        populateDecryptedLinks(decryptedLinks, url);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(filePackageName));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String url) throws PluginException {
        final String[] links = br.getRegex("href=\"([^\"]+\\.jpg)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String link : links) {
            final DownloadLink dl = createDownloadlink(link);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
    }

    private String getFilePackageName(String url) {
        String title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (title == null) {
            title = new Regex(url, "pics/(.+)").getMatch(0);
            if (title.endsWith("/")) {
                title = title.substring(0, title.length() - 1);
            }
        }
        return title.trim();
    }
}
