package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * A plugin for downloading all galleries in the current HTML page, which are on this host. Does not support paging right now.
 * 
 * For downloading the single galleries, it uses functionality from SimpleHtmlBasedGalleryPlugin, so make sure, that the galleries-page is
 * supported there.
 */
//@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SimpleHtmlBasedGalleriesPlugin extends SimpleHtmlBasedGalleryPlugin {

    private static final Map<String, String> HOST_2_GALLERY_URL_IDENTIFIER = new HashMap<String, String>();
    static {
        HOST_2_GALLERY_URL_IDENTIFIER.put("sexygirlspics.com", "[^\"']+sexygirlspics\\.com/pics[^\"']+");
        HOST_2_GALLERY_URL_IDENTIFIER.put("nastypornpics.com", "[^\"']+nastypornpics\\.com/pics[^\"']+");
        HOST_2_GALLERY_URL_IDENTIFIER.put("viewgals.com", "[^\"']+viewgals\\.com/pics[^\"']+");
        HOST_2_GALLERY_URL_IDENTIFIER.put("hqsluts.com", "/[^/\"']+-\\d+/");
        HOST_2_GALLERY_URL_IDENTIFIER.put("babesource.com", "[^\"']+babesource\\.com/galleries[^\"']+");
        HOST_2_GALLERY_URL_IDENTIFIER.put("pichunter.com", "/gallery/[^\"']+");
        // HOST_2_GALLERY_URL_IDENTIFIER.put("sexhd.pics", "/gallery/[^/\"']+/[^/\"']+/[^/\"']+/");
        // HOST_2_GALLERY_URL_IDENTIFIER.put("xxxporn.pics", "/sex/[^\"']+");
    }

    public SimpleHtmlBasedGalleriesPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getSupportedSites() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "sexygirlspics.com", "https?://(?:www\\.)?sexygirlspics\\.com/\\?q=[^&]+&log-model=1" });
        ret.add(new String[] { "nastypornpics.com", "http?://(?:www\\.)?nastypornpics\\.com/\\?q=[^&]+&log-model=1" });
        ret.add(new String[] { "viewgals.com", "http?://(?:www\\.)?viewgals\\.com/\\?q=[^&]+&log-model=1" });
        ret.add(new String[] { "hqsluts.com", "https?://(?:www\\.)?hqsluts\\.com/sluts/.+" });
        ret.add(new String[] { "babesource.com", "https?://(?:www\\.)?babesource\\.com/pornstars/.+" });
        ret.add(new String[] { "pichunter.com", "https?://(?:www\\.)?pichunter\\.com/models/.+" });
        // TODO distinguish from single gallery
        // ret.add(new String[] { "sexhd.pics", "https?://(?:www\\.)?sexhd\\.pics/gallery/[^/]+/" });
        // ret.add(new String[] { "xxxporn.pics", "https?://(?:www\\.)?xxxporn\\.pics/sex/[^/]+" });
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
        String[] galleryUrls = getGalleryUrls();
        for (final String galleryUrl : galleryUrls) {
            if (!isAbort()) {
                decryptedLinks.addAll(super.decryptIt(new CryptedLink(galleryUrl), progress));
            }
        }
        return decryptedLinks;
    }

    protected String[] getGalleryUrls() throws PluginException {
        String galleryUrlIdentifier = HOST_2_GALLERY_URL_IDENTIFIER.get(br.getHost());
        if (StringUtils.isEmpty(galleryUrlIdentifier)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no gallery url identifier configured for " + br.getHost());
        }
        String[][] matches = br.getRegex("href\\s*=\\s*(?:\"|')(" + galleryUrlIdentifier + ")(?:\"|')").getMatches();
        String[] galleryUrls = new String[matches.length];
        for (int i = 0; i < matches.length; i++) {
            try {
                String match = matches[i][0];
                if (StringUtils.isEmpty(match)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no gallery match found");
                }
                galleryUrls[i] = br.getURL(match).toString();
            } catch (IOException e) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            }
        }
        return galleryUrls;
    }
}
