package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Locale;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sexygirlspics.com" }, urls = { "https?://(?:www\\.)?sexygirlspics\\.com/pics/[^/]+" })
public class SexyGirlsPicsCom extends PluginForDecrypt {
    public SexyGirlsPicsCom(PluginWrapper wrapper) {
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
        final String title = getFilePackageName(url);
        populateDecryptedLinks(decryptedLinks, url, title);
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, final String url, final String title) throws PluginException {
        final String[] links = br.getRegex("href\\s*=\\s*'([^']+\\.jpg)'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int padLength = (int) Math.log10(links.length) + 1;
        int index = 1;
        for (String link : links) {
            final DownloadLink dl = createDownloadlink(link);
            dl.setAvailable(true);
            String fileName = String.format(Locale.US, "%0" + padLength + "d", index) + ".jpg";
            if (title != null) {
                fileName = title + "_" + fileName;
            }
            dl.setFinalFileName(fileName);
            decryptedLinks.add(dl);
            index++;
        }
    }

    private String getFilePackageName(String url) {
        String title = br.getRegex("<title>\\s*([^<>\"]*?)\\s*</title>").getMatch(0);
        if (title == null) {
            title = new Regex(url, "pics/(.+)").getMatch(0);
        }
        return title != null ? Encoding.htmlDecode(title.trim()) : null;
    }
}
