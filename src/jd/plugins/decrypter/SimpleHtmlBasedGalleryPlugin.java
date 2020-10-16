package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SimpleHtmlBasedGalleryPlugin extends PluginForDecrypt {
    public SimpleHtmlBasedGalleryPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getSupportedSites() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "babesource.com", "https?://(?:www\\.)?babesource\\.com/galleries/([^/]+)-\\d+\\.html" });
        ret.add(new String[] { "coedcherry.com", "https?://(?:www\\.)?coedcherry\\.com/.*pics/([^/]+)" });
        ret.add(new String[] { "elitebabes.com", "https?://(?:www\\.)?elitebabes\\.com/([^/]+)-\\d+" });
        ret.add(new String[] { "erocurves.com", "https?://(?:www\\.)?erocurves\\.com/.+" });
        ret.add(new String[] { "pornpics.com", "https?://(?:www\\.)?pornpics\\.com/galleries/.+" });
        ret.add(new String[] { "sexygirlspics.com", "https?://(?:www\\.)?sexygirlspics\\.com/pics/.+" });
        ret.add(new String[] { "pichunter.com", "https?://(?:www\\.)?pichunter\\.com/gallery/.+" });
        ret.add(new String[] { "nastypornpics.com", "http?://(?:www\\.)?nastypornpics\\.com/pics/.+" });
        ret.add(new String[] { "prettynubiles.com", "http?://(?:www\\.)?prettynubiles\\.com/galleries/([^\\.]+)\\.html" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] supportedSite : getSupportedSites()) {
            ret.add(supportedSite[0]);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { getHost() };
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] supportedSite : getSupportedSites()) {
            ret.add(supportedSite[1]);
        }
        return ret.toArray(new String[0]);
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
        final String title = getFilePackageName(url);
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
        } else {
            return links;
        }
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

    private String getFilePackageName(String url) {
        String title = br.getRegex("<title>\\s*([^<>]+?)\\s*</title>").getMatch(0);
        if (title == null) {
            // title = new Regex(url, getMatcher().pattern()).getMatch(1);
        }
        return title != null ? Encoding.htmlDecode(title.trim()) : null;
    }
}
