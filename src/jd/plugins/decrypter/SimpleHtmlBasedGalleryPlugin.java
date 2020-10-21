package jd.plugins.decrypter;

import java.io.IOException;
import java.net.URL;
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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

/**
 * A plugin for downloading JPGs via href links from plain HTML. Those links can be absolute or relative to the host.
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class SimpleHtmlBasedGalleryPlugin extends PluginForDecrypt {
    public SimpleHtmlBasedGalleryPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getSupportedSites() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "babesource.com", "https?://(?:www\\.)?babesource\\.com/galleries/[^/]+-\\d+\\.html" });
        ret.add(new String[] { "coedcherry.com", "https?://(?:www\\.)?coedcherry\\.com/.*pics/[^/]+" });
        ret.add(new String[] { "elitebabes.com", "https?://(?:www\\.)?elitebabes\\.com/.+" });
        ret.add(new String[] { "erocurves.com", "https?://(?:www\\.)?erocurves\\.com/.+" });
        ret.add(new String[] { "pornpics.com", "https?://(?:www\\.)?pornpics\\.com/galleries/.+" });
        ret.add(new String[] { "sexygirlspics.com", "https?://(?:www\\.)?sexygirlspics\\.com/pics/.+" });
        ret.add(new String[] { "pichunter.com", "https?://(?:www\\.)?pichunter\\.com/gallery/.+" });
        ret.add(new String[] { "nastypornpics.com", "http?://(?:www\\.)?nastypornpics\\.com/pics/.+" });
        ret.add(new String[] { "prettynubiles.com", "http?://(?:www\\.)?prettynubiles\\.com/galleries/[^\\.]+\\.html" });
        ret.add(new String[] { "viewgals.com", "http?://(?:www\\.)?viewgals\\.com/pics/.+" });
        ret.add(new String[] { "sexhd.pics", "https?://(?:www\\.)?sexhd\\.pics/gallery/[^/]+/[^/]+/.+" });
        ret.add(new String[] { "xxxporn.pics", "https?://(?:www\\.)?xxxporn\\.pics/sex/(?!\\d+).+" });
        ret.add(new String[] { "fapcat.com", "https?://(?:www\\.)?fapcat\\.com/albums/\\d+/.+" });
        ret.add(new String[] { "hqsluts.com", "https?://(?:www\\.)?hqsluts\\.com/[^/]+-\\d+" });
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

    protected void populateDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, String url) throws PluginException, IOException {
        final String[] links = determineLinks();
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final int padLength = (int) Math.log10(links.length) + 1;
            int index = 1;
            for (String link : links) {
                decryptedLinks.add(buildDownloadLink(padLength, index++, link));
            }
        }
    }

    protected String[] determineLinks() throws PluginException {
        final String[] links = getRawLinks();
        if (links == null || links.length == 0) {
            return links;
        } else {
            // in case the link is relative to the host, make it absolute
            final List<String> ret = new ArrayList<String>();
            for (int i = 0; i < links.length; i++) {
                try {
                    ret.add(br.getURL(links[i]).toString());
                } catch (IOException e) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
            return ret.toArray(new String[0]);
        }
    }

    protected String[] getRawLinks() {
        return br.getRegex("href\\s*=\\s*(?:\"|')([^\"']+\\.jpg/?)(?:\"|')").getColumn(0);
    }

    protected DownloadLink buildDownloadLink(int padLength, int index, String link) throws IOException {
        final URL url = new URL(link);
        final DownloadLink dl;
        if (url.getPath().matches(".*\\.(jpg)$")) {
            dl = createDownloadlink(link);
        } else {
            dl = createDownloadlink("directhttp://" + link);
        }
        dl.setAvailable(true);
        dl.setFinalFileName(buildFileName(padLength, index));
        return dl;
    }

    private String buildFileName(int padLength, int index) {
        return "image_" + String.format(Locale.US, "%0" + padLength + "d", index) + ".jpg";
    }

    protected String getFilePackageName(String url) {
        String title = br.getRegex("<title>\\s*([^<>]+?)\\s*</title>").getMatch(0);
        if (StringUtils.isNotEmpty(title)) {
            String id = new Regex(url, "(\\d+)").getMatch(0);
            if (StringUtils.isNotEmpty(id) && !title.contains(id)) {
                title = title + " " + id;
            }
        }
        if (title == null) {
            // title = new Regex(url, getMatcher().pattern()).getMatch(1);
        }
        return title != null ? Encoding.htmlDecode(title.trim()) : null;
    }
}
