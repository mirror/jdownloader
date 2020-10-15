package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fapcat.com" }, urls = { "https?://(?:www\\.)?fapcat\\.com/albums/.+" })
public class FapCatComGallery extends PluginForDecrypt {
    public FapCatComGallery(PluginWrapper wrapper) {
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

    private void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String url) throws PluginException, IOException {
        final String[] links = br.getRegex("href=\"([^\"]+\\.jpg/)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String link : links) {
            if (!isAbort()) {
                // request to "link" is responded with 302 and the actual URL of the picture (in location header)
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(link);
                String location = brc.getRedirectLocation();
                if (StringUtils.isEmpty(location)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final DownloadLink dl = createDownloadlink(location);
                    dl.setName(new Regex(link, "/([^/]+\\.jpg)/$").getMatch(0));
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
        }
    }

    private String getFilePackageName(String url) {
        String title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (title == null) {
            title = new Regex(url, "albums/[^/]+/(.+)").getMatch(0);
            if (title.endsWith("/")) {
                title = title.substring(0, title.length() - 1);
            }
        }
        return title.trim();
    }
}
