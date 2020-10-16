package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "babesource.com", "coedcherry.com", "elitebabes.com", "erocurves.com", "pornpics.com", "sexygirlspics.com", "pichunter.com", "nastypornpics.com" }, urls = { "https?://(?:www\\.)?babesource\\.com/galleries/.+", "https?://(?:www\\.)?coedcherry\\.com/.*pics/.+", "https?://(?:www\\.)?elitebabes\\.com/(?!.*?model).+", "https?://(?:www\\.)?erocurves\\.com/.+", "https?://(?:www\\.)?pornpics\\.com/galleries/.+", "https?://(?:www\\.)?sexygirlspics\\.com/pics/.+", "https?://(?:www\\.)?pichunter\\.com/gallery/.+", "http?://(?:www\\.)?nastypornpics\\.com/pics/.+", })
public class SimpleHtmlBasedGalleryPlugin extends PluginForDecrypt {

    public SimpleHtmlBasedGalleryPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = cryptedLink.toString();
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        }
        populateDecryptedLinks(decryptedLinks, url);
        final String title = getFilePackageName();
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    protected void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String url) throws PluginException {
        final String[] links = determineLinks(url);
        final int padLength = (int) Math.log10(links.length) + 1;
        int index = 1;
        for (String link : links) {
            decryptedLinks.add(buildDownloadLink(padLength, index, link));
            index++;
        }
    }

    protected String[] determineLinks(String url) throws PluginException {
        final String[] links = br.getRegex("href\\s*=\\s*(?:\"|')([^\"']+\\.jpg)(?:\"|')").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return links;
    }

    protected DownloadLink buildDownloadLink(int padLength, int index, String link) {
        final DownloadLink dl = createDownloadlink(link);
        dl.setAvailable(true);
        dl.setFinalFileName(buildFileName(padLength, index));
        return dl;
    }

    private String buildFileName(int padLength, int index) {
        return "image_" + String.format(Locale.US, "%0" + padLength + "d", index) + ".jpg";
    }

    private String getFilePackageName() {
        String title = br.getRegex("<title>\\s*([^<>\"]*?)\\s*</title>").getMatch(0);
        return title != null ? Encoding.htmlDecode(title.trim()) : null;
    }
}
